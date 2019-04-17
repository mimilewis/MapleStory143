/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import handling.opcode.SendPacketOpcode;
import handling.world.WorldAllianceService;
import handling.world.WorldGuildService;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import handling.world.messenger.MessengerRankingWorker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.DateUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

/**
 * @author PlayDK
 */
public class MessengerPacket {

    private static final Logger log = LogManager.getLogger(MessengerPacket.class);

    /*
     * 增加聊天招待的角色
     */
    public static byte[] addMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x00);
        mplew.write(position);
        PacketHelper.addCharLook(mplew, chr, true, chr.isZeroSecondLook());
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.write(position); //难道是位置？
        chr.writeJobData(mplew); //职业ID

        return mplew.getPacket();
    }

    /*
     * 同意加入聊天招待
     * 这个是发送在聊天招待里面的位置
     */
    public static byte[] joinMessenger(int position) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x01);
        mplew.write(position);

        return mplew.getPacket();
    }

    /*
     * 聊天招待
     * 玩家退出
     */
    public static byte[] removeMessengerPlayer(int position) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x02);
        mplew.write(position);

        return mplew.getPacket();
    }

    /*
     * 收到玩家的聊天邀请
     */
    public static byte[] messengerInvite(String from, int messengerId, int channel) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x03);
        mplew.writeMapleAsciiString(from);
        mplew.write(channel);
        mplew.writeInt(messengerId);
        mplew.write(0x00);

        return mplew.getPacket();
    }

    /*
     * 聊天招待说话
     */
    public static byte[] messengerChat(String text, String postxt) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x06);
        mplew.writeMapleAsciiString(text);
        if (postxt.length() > 0) {
            mplew.writeMapleAsciiString(postxt);
        }

        return mplew.getPacket();
    }

    public static byte[] updateMessengerPlayer(String from, MapleCharacter chr, int position, int channel) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x07);
        mplew.write(position);
        PacketHelper.addCharLook(mplew, chr, true, chr.isZeroSecondLook());
        //mplew.writeMapleAsciiString(from);
        //mplew.write(0x00); //是否写职业ID 0x01 为需要写
        //mplew.writeInt(0x00); //职业ID 上面为1时就要写职业ID

        return mplew.getPacket();
    }

    /*
     * 聊天招待中给玩家加好感度的返回
     */
    public static byte[] giveLoveResponse(int mode, String charname, String targetname) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x0A);
        /*
         * 0x00 'xxxx'成功提升了'xxxx'的好感度。
         * 0x01 由于未知原因，提升好感度失败。
         * 0x02 今天之内无法再次提升'xxxx'的好感度。
         */
        mplew.write(mode);
        mplew.writeMapleAsciiString(charname);
        mplew.writeMapleAsciiString(targetname);

        return mplew.getPacket();
    }

    /*
     * 在聊天招待中查看玩家的信息
     */
    public static byte[] messengerPlayerInfo(MapleCharacter chr) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x0B);
        mplew.writeMapleAsciiString(chr.getName()); //角色名字
        mplew.write(chr.getLevel()); //等级
        chr.writeJobData(mplew); //职业
        mplew.writeInt(chr.getFame()); //人气
        mplew.writeInt(chr.getLove()); //好感度
        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("-");
            mplew.writeMapleAsciiString("");
        } else {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(chr.getGuildId());
            if (guild != null) {
                mplew.writeMapleAsciiString(guild.getName());
                if (guild.getAllianceId() > 0) {
                    MapleGuildAlliance alliance = WorldAllianceService.getInstance().getAlliance(guild.getAllianceId());
                    if (alliance != null) {
                        mplew.writeMapleAsciiString(alliance.getName());
                    } else {
                        mplew.writeMapleAsciiString("");
                    }
                } else {
                    mplew.writeMapleAsciiString("");
                }
            } else {
                mplew.writeMapleAsciiString("-");
                mplew.writeMapleAsciiString("");
            }
        }
        mplew.write(0x00); //未知

        return mplew.getPacket();
    }

    /*
     * 聊天招待中私聊
     */
    public static byte[] messengerWhisper(String namefrom, String chatText) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(0x0E);
        mplew.writeMapleAsciiString(namefrom);
        mplew.writeMapleAsciiString(chatText);

        return mplew.getPacket();
    }

    public static byte[] messengerNote(String text, int mode, int mode2) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MESSENGER.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(text);
        mplew.write(mode2);

        return mplew.getPacket();
    }

    public static byte[] updateLove(int love) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOVE_OPERATION.getValue());
        mplew.write(0);
        mplew.writeInt(love); //好感度
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));
        mplew.writeInt(0x03);

        return mplew.getPacket();
    }

    public static byte[] showLoveRank(int mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOVE_OPERATION.getValue());
        mplew.write(mode);
        MessengerRankingWorker rank = MessengerRankingWorker.getInstance();
        for (int i = 0; i < 2; i++) {
            MapleCharacter player = rank.getRankingPlayer(i);
            mplew.write(player != null ? 1 : 0);
            if (player != null) {
                mplew.writeInt(player.getId());
                mplew.writeInt(player.getLove());
                mplew.writeLong(DateUtil.getFileTimestamp(rank.getLastUpdateTime(i)));
                mplew.writeMapleAsciiString(player.getName());
                PacketHelper.addCharLook(mplew, player, false, false);
            }
        }

        return mplew.getPacket();
    }
}
