package handling.netty;

import client.MapleClient;
import configs.OpcodeConfig;
import configs.ServerConfig;
import handling.opcode.SendPacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.HexTool;
import tools.MapleAESOFB;
import tools.StringUtil;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericLittleEndianAccessor;

import java.util.concurrent.locks.Lock;

import static handling.MapleServerHandler.AllPacketLog;
import static handling.MapleServerHandler.BuffPacketLog;

public class MaplePacketEncoder extends MessageToByteEncoder<Object> {

    private static final Logger log = LogManager.getLogger("DebugWindows");

    @Override
    protected void encode(ChannelHandlerContext ctx, Object message, ByteBuf buffer) throws Exception {
        final MapleClient client = ctx.channel().attr(MapleClient.CLIENT_KEY).get();

        if (client != null) {
            MapleAESOFB send_crypto = client.getSendCrypto();
            byte[] input = ((byte[]) message);

            int packetLen = input.length;
            int pHeader = readFirstShort(input);
            String pHeaderStr = Integer.toHexString(pHeader).toUpperCase();
            pHeaderStr = StringUtil.getLeftPaddedStr(pHeaderStr, '0', 4);
            String op = lookupRecv(pHeader);
            String Send = "Send " + op + " [" + pHeaderStr + "] (" + packetLen + ")";
            Send += client.getPlayer() != null ? " Recv to : " + client.getPlayer().getName() + "\r\n" : "\r\n";
            String RecvTo = Send + HexTool.toString(input) + "\r\n" + HexTool.toStringFromAscii(input);
            AllPacketLog.info(RecvTo);
            if (ServerConfig.DEBUG_MODE && !OpcodeConfig.isblock(op, true)) {
                log.trace("\r\n" + RecvTo);
            }
            if (op.equals("GIVE_BUFF") || op.equals("CANCEL_BUFF")) {
                BuffPacketLog.trace("\r\n" + RecvTo);
            }

            byte[] unencrypted = new byte[input.length];
            System.arraycopy(input, 0, unencrypted, 0, input.length); // Copy the input > "unencrypted"
            byte[] ret = new byte[unencrypted.length + 4]; // Create new bytes with length = "unencrypted" + 4
            Lock mutex = client.getLock();
            mutex.lock();
            try {
                byte[] header = send_crypto.getPacketHeader(unencrypted.length);
                //MapleCustomEncryption.encryptData(unencrypted); // Encrypting Data
                send_crypto.crypt(unencrypted); // Crypt it with IV
                System.arraycopy(header, 0, ret, 0, 4); // Copy the header > "Ret", first 4 bytes
                System.arraycopy(unencrypted, 0, ret, 4, unencrypted.length); // Copy the unencrypted > "ret"
                buffer.writeBytes(ret);
            } finally {
                mutex.unlock();
            }
        } else { // no client object created yet, send unencrypted (hello) 这里是发送gethello 封包 无需加密
            buffer.writeBytes((byte[]) message);
        }
    }

    private String lookupRecv(int val) {
        for (SendPacketOpcode op : SendPacketOpcode.values()) {
            if (op.getValue() == val) {
                return op.name();
            }
        }
        return "UNKNOWN";
    }

    private int readFirstShort(byte[] arr) {
        return new GenericLittleEndianAccessor(new ByteArrayByteStream(arr)).readShort();
    }
}
