/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.inventory.Item;
import client.inventory.MapleAndroid;
import client.inventory.MapleInventoryType;
import constants.ServerConstants;
import handling.opcode.SendPacketOpcode;
import server.movement.LifeMovementFragment;
import tools.DateUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.List;

/**
 * @author PlayDK
 */
public class AndroidPacket {

    /*
     * 召唤安卓
     */
    public static byte[] spawnAndroid(MapleCharacter chr, MapleAndroid android) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_SPAWN.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(android.getType());
        mplew.writePos(android.getPos() == null ? chr.getTruePosition() : android.getPos());
        mplew.write(android.getStance());
        mplew.writeShort(android.getFh());
        mplew.writeShort(android.getSkin() >= 2000 ? android.getSkin() - 2000 : android.getSkin());
        mplew.writeShort(android.getHair() - 30000);
        mplew.writeShort(android.getFace() - 20000);
        mplew.writeMapleAsciiString(android.getName());
        for (short i = -1200; i > -1207; i--) {
            Item item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(i);
            mplew.writeInt(item != null ? item.getItemId() : 0);
        }

        return mplew.getPacket();
    }

    /*
     * 安卓移动
     */
    public static byte[] moveAndroid(int chrId, Point pos, List<LifeMovementFragment> res) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_MOVE.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(0); //V.112新增
        mplew.writePos(pos);
        mplew.writeInt(DateUtil.getTime(System.currentTimeMillis())); //time left in milliseconds? this appears to go down...slowly 1377440900
        PacketHelper.serializeMovementList(mplew, res);

        return mplew.getPacket();
    }

    /*
     * 显示安卓表情
     */
    public static byte[] showAndroidEmotion(int chrId, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_EMOTION.getValue());
        mplew.writeInt(chrId);
        mplew.write(0);
        mplew.write(animation); //1234567 = default smiles, 8 = throwing up, 11 = kiss, 14 = googly eyes, 17 = wink...

        return mplew.getPacket();
    }

    /*
     * 更新安卓外观
     */
    public static byte[] updateAndroidLook(int chrId, int size, int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_UPDATE.getValue());
        mplew.writeInt(chrId);
        switch (size) {
            case -1200: //帽子
                mplew.write(0x01);
                break;
            case -1201: //披风
                mplew.write(0x02);
                break;
            case -1202: //脸饰
                mplew.write(0x04);
                break;
            case -1203: //上衣
                mplew.write(0x08);
                break;
            case -1204: //裤子
                mplew.write(0x10);
                break;
            case -1205: //鞋子
                mplew.write(0x20);
                break;
            case -1206: //手套
                mplew.write(0x40);
                break;
        }
        mplew.writeInt(itemId);
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 玩家停用安卓
     */
    public static byte[] deactivateAndroid(int chrId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ANDROID_DEACTIVATED.getValue());
        mplew.writeInt(chrId);

        return mplew.getPacket();
    }

    /**
     * 移除机器人心脏
     */
    public static byte[] removeAndroidHeart() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        /*
         * 0x15 智能机器人没有动力。请装备机械心脏。
         * 0x16 休息后恢复了疲劳度。
         */
        mplew.write(ServerConstants.MapleStatusInfo.移除机器人心脏.getType());

        return mplew.getPacket();
    }
}
