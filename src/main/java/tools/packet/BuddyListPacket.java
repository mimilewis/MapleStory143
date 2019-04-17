/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.BuddylistEntry;
import handling.opcode.BuddyOpcode;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Collection;

/**
 * @author PlayDK
 */
public class BuddyListPacket {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(BuddyListPacket.class);

    /*
     * 返回好友操作信息
     * 0x0B 好友目录已满了。
     * 0x0C 对方的好友目录已满了。
     * 0x0D 已经是好友。
     * 0x0E 不能把管理员加为好友。
     * 0x0F 没登录的角色。
     * 0x1E 还在对方的好友目录中
     */
    public static byte[] buddylistMessage(int message) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(message);

        return mplew.getPacket();
    }

    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist) {
        return updateBuddylist(buddylist, BuddyOpcode.FriendRes_LoadAccountIDOfCharacterFriend_Done.getValue());//更新
    }

    /*
     * 更新好友信息
     */
    public static byte[] updateBuddylist(Collection<BuddylistEntry> buddylist, int mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(mode);
        if (mode != 0x27) {
            mplew.writeInt(buddylist.size());
        }
        for (BuddylistEntry buddy : buddylist) {
            mplew.writeInt(buddy.getCharacterId());
            mplew.writeAsciiString(buddy.getName(), 13);
            mplew.write(buddy.isVisible() ? 0 : 1);//0普通好友不在线 2普通好友在线 4开启帐号转换,5离线账号好友,7账号好友在线
            mplew.writeInt(buddy.getChannel() == -1 ? -1 : (buddy.getChannel() - 1));
            mplew.writeAsciiString(buddy.getGroup(), 18); //V.116.修改以前 17位
            mplew.writeInt(0); // buddy.getAccountId()
//            mplew.writeAsciiString(buddy.getName(), 13);//别名
            mplew.writeHexString("00 AE 1A 0B 64 FC 34 11 10 0D AB 0F 68");
            mplew.writeAsciiString("", 13);//备注
            mplew.writeZeroBytes(247);
        }
//        mplew.writeHexString("69 00 15 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 07 FF FF FF FF CE B4 D6 B8 B6 A8 C8 BA D7 E9 00 00 00 00 00 00 00 00 E9 C9 91 02 CB D1 CB F7 00 00 00 00 00 00 00 00 00 EC AA EC AA 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 05 FF FF FF FF CE B4 D6 B8 B6 A8 C8 BA D7 E9 00 00 00 00 00 00 00 00 79 0F D5 02 CA C7 00 00 00 00 00 00 00 00 00 00 00 B5 C4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
        return mplew.getPacket();
    }

    /*
    * 更新好友完毕
    */
    public static byte[] updateBuddylistEnd() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(BuddyOpcode.FriendRes_SetMessengerMode.getValue());
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 申请加好友
     */
    public static byte[] requestBuddylistAdd(int chrIdFrom, String nameFrom, int channel, int levelFrom, int jobFrom) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(BuddyOpcode.FriendRes_Invite.getValue());
        mplew.writeBool(false);
        mplew.writeInt(chrIdFrom);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(nameFrom);
        mplew.writeInt(levelFrom);
        mplew.writeInt(jobFrom);
        mplew.writeInt(0); //V.104新增 貌似是把职业的 Int 改为 Long ?
        mplew.writeInt(chrIdFrom);
        mplew.writeAsciiString(nameFrom, 13);
        mplew.write(1);
        mplew.writeInt(channel); //频道
        mplew.writeAsciiString("未指定群组", 18);
        mplew.writeInt(0);
        mplew.writeAsciiString(nameFrom, 13);
        for (int i = 0; i < 65; i++) {
            mplew.writeInt(0);
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    /*
     * 更新好友频道信息
     */
    public static byte[] updateBuddyChannel(int chrId, int channel, String name) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(BuddyOpcode.FriendRes_Notify.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(0);
        mplew.write(0); //isVisible() 角色在商城和拍卖的时候为1
        mplew.writeInt(channel);
        mplew.write(0);
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 更新好友数量
     */
    public static byte[] updateBuddyCapacity(int capacity) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(BuddyOpcode.FriendRes_IncMaxCount_Done.getValue());
        mplew.write(capacity);

        return mplew.getPacket();
    }

    /*
     * 更新好友别名
     */
    public static byte[] updateBuddyNamer(BuddylistEntry buddylist, int mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(mode);
        mplew.writeInt(buddylist.getCharacterId());
        mplew.writeInt(buddylist.getCharacterId());
        mplew.writeInt(buddylist.getCharacterId());
        mplew.writeAsciiString(buddylist.getName(), 13);
        mplew.write(buddylist.isVisible() ? 7 : 5);//4开启帐号转换,5离线好友,7好友在线
        mplew.writeInt(buddylist.getChannel() == -1 ? -1 : (buddylist.getChannel() - 1));
        mplew.writeAsciiString(buddylist.getGroup(), 18); //V.116.修改以前 17位
        mplew.writeInt(buddylist.getCharacterId());
        mplew.writeAsciiString(buddylist.getName(), 13);//别名
        mplew.writeAsciiString("", 13);//备注        
        for (int i = 0; i < 64; i++) {
            mplew.writeInt(0);
        }
        mplew.writeZeroBytes(3);
        return mplew.getPacket();
    }

    /*
    * 拒绝好友
    */
    public static byte[] NoBuddy(int buddyid, int mode, boolean linkaccount) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(mode);
        mplew.writeBool(linkaccount);
        mplew.writeInt(buddyid);
        return mplew.getPacket();
    }

    /*
     * 好友信息
     */
    public static byte[] BuddyMess(int mode, String name) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUDDYLIST.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }
}
