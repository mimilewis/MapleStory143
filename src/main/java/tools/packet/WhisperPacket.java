/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author PlayDK
 */
public class WhisperPacket {

    private static final Logger log = LogManager.getLogger(WhisperPacket.class);

    public static byte[] getWhisper(String sender, int channel, String text) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x12);
        mplew.writeMapleAsciiString(sender);
        mplew.writeShort(channel - 1);
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    public static byte[] getWhisperReply(String target, byte reply) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(0x0A); // whisper?
        mplew.writeMapleAsciiString(target);
        mplew.write(reply);//  0x0 = cannot find char, 0x1 = success

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithMap(String target, int mapid, boolean buddy) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 0x48 : 0x09);
        mplew.writeMapleAsciiString(target);
        mplew.write(1);
        mplew.writeInt(mapid);

        return mplew.getPacket();
    }

    public static byte[] getFindReply(String target, int channel, boolean buddy) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 0x48 : 0x09);
        mplew.writeMapleAsciiString(target);
        mplew.write(3);
        mplew.writeInt(channel - 1);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithCS(String target, boolean buddy) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 0x48 : 0x09);
        mplew.writeMapleAsciiString(target);
        mplew.write(2);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }

    public static byte[] getFindReplyWithMTS(String target, boolean buddy) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WHISPER.getValue());
        mplew.write(buddy ? 0x48 : 0x09);
        mplew.writeMapleAsciiString(target);
        mplew.write(0);
        mplew.writeInt(-1);

        return mplew.getPacket();
    }
}
