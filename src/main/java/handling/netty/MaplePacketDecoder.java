package handling.netty;

import client.MapleClient;
import configs.OpcodeConfig;
import configs.ServerConfig;
import handling.login.handler.LoginPasswordHandler;
import handling.opcode.RecvPacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.StringUtil;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericLittleEndianAccessor;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;

import java.util.List;

import static handling.MapleServerHandler.*;

public class MaplePacketDecoder extends ByteToMessageDecoder {
    @SuppressWarnings("deprecation")
    public static final AttributeKey<DecoderState> DECODER_STATE_KEY = AttributeKey.newInstance(MaplePacketDecoder.class.getName() + ".STATE");
    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger("DebugWindows");

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> message) throws Exception {
        final MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();
        DecoderState decoderState = ctx.channel().attr(DECODER_STATE_KEY).get();
        if (decoderState == null) {
            decoderState = new DecoderState();
            ctx.channel().attr(DECODER_STATE_KEY).set(decoderState);
        }
        if (in.readableBytes() >= 4 && decoderState.packetlength == -1) {
            int packetHeader = in.readInt();
            int packetid = (packetHeader >> 16) & 0xFFFF;
            if (!client.isLoggedIn() && packetid == 0x6969) {
                int packetlength = ((packetHeader & 0xFF) << 8) + (packetHeader >> 8 & 0xFF); // packetHeader & 0xFF
                byte packet[] = new byte[packetlength];
                in.readBytes(packet);
                SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream(packet));
                LoginPasswordHandler.handlePacket(slea, client);
                return;
            }
            if (!client.getReceiveCrypto().checkPacket(packetHeader)) {
                ctx.channel().disconnect();
                return;
            }
            decoderState.packetlength = MapleAESOFB.getPacketLength(packetHeader);
        } else if (in.readableBytes() < 4 && decoderState.packetlength == -1) {
            return;
        }


        if (in.readableBytes() >= decoderState.packetlength) {
            byte decryptedPacket[] = new byte[decoderState.packetlength];
            in.readBytes(decryptedPacket);
            decoderState.packetlength = -1;
            client.getReceiveCrypto().crypt(decryptedPacket);
            //MapleCustomEncryption.decryptData(decryptedPacket);
            message.add(decryptedPacket);

            int packetLen = decryptedPacket.length;
            int pHeader = readFirstShort(decryptedPacket);
            String pHeaderStr = Integer.toHexString(pHeader).toUpperCase();
            pHeaderStr = StringUtil.getLeftPaddedStr(pHeaderStr, '0', 4);
            String op = lookupSend(pHeader);
            StringBuilder recvString = new StringBuilder();
            recvString.append("\r\n").append("Recv ").append(op).append("[").append(pHeaderStr).append("] (").append(packetLen).append(")").append(client.getPlayer() != null ? " From : " + client.getPlayer().getName() : "").append("\r\n");
            recvString.append(HexTool.toString(decryptedPacket)).append("\r\n");
            if (op.equals("CLOSE_RANGE_ATTACK") || op.equals("RANGED_ATTACK") || op.equals("MAGIC_ATTACK") || op.equals("SUMMON_ATTACK")) {
                AttackPakcetLog.info(recvString);
            } else if (op.equals("SPECIAL_MOVE")) {
                BuffPacketLog.info(recvString);
            }
            recvString.append(HexTool.toStringFromAscii(decryptedPacket));
            AllPacketLog.info(recvString);
            if (ServerConfig.DEBUG_MODE && !OpcodeConfig.isblock(op, false)) {
                log.trace(recvString);
            }
        }
    }

    private String lookupSend(int val) {
        for (RecvPacketOpcode op : RecvPacketOpcode.values()) {
            if (op.getValue() == val) {
                return op.name();
            }
        }
        return "UNKNOWN";
    }

    private int readFirstShort(byte[] arr) {
        return new GenericLittleEndianAccessor(new ByteArrayByteStream(arr)).readShort();
    }

    private static class DecoderState {
        public int packetlength = -1;
    }
}