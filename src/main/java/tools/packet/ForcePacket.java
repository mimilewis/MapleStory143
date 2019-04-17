package tools.packet;

import client.MapleForce;
import client.MapleForceType;
import constants.skills.尖兵;
import handling.opcode.SendPacketOpcode;
import server.life.MapleMonster;
import tools.DateUtil;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class ForcePacket {

    public static byte[] showForce(MapleForce force) {
        return showForce(force, null);
    }

    public static byte[] showForce(MapleForce force, Point point) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());

        mplew.writeBool(force.getFromMobOID() > 0);
        mplew.writeInt(force.getOwnerID());
        if (force.getFromMobOID() > 0) {
            mplew.writeInt(force.getFromMobOID());
        }
        mplew.writeInt(force.getType().ordinal());
        if (force.getType() != MapleForceType.恶魔精气) {
            mplew.write(force.getSubType());
            if (force.getType().isMultiMob()) {
                mplew.writeInt(force.getToMobOID().size());
            }
            for (Integer mobid : force.getToMobOID()) {
                mplew.writeInt(mobid);
            }
            mplew.writeInt(force.getSkillID());
        }

        for (Pair<Integer, Byte> forceid : force.getForceOID()) {
            mplew.write(1);
            mplew.writeInt(forceid.getLeft());
            mplew.writeInt(forceid.getRight());
            switch (force.getType()) {
                case 恶魔精气:
                    mplew.writeInt(Randomizer.rand(0x23, 0x31));
                    mplew.writeInt(Randomizer.rand(5, 6));
                    mplew.writeInt(Randomizer.rand(0x1E, 0x44));
                    mplew.writeInt(0);
                    break;
                case 幻影卡牌:
                    mplew.writeInt(Randomizer.rand(15, 32));
                    mplew.writeInt(Randomizer.rand(7, 10));
                    mplew.writeInt(Randomizer.rand(9, 25));
                    mplew.writeInt(0);
                    break;
                case 追击盾:
                    mplew.writeInt(Randomizer.rand(0x10, 0x12));
                    mplew.writeInt(Randomizer.rand(0x14, 0x1C));
                    mplew.writeInt(Randomizer.rand(0x22, 0x42));
                    mplew.writeInt(0x3C);
                    break;
                case 尖兵火箭:
                    mplew.writeInt(Randomizer.rand(15, 20));
                    mplew.writeInt(Randomizer.rand(20, 30));
                    mplew.writeInt(Randomizer.rand(120, 150));
                    mplew.writeInt(Randomizer.rand(300, 900));
                    break;
                case 宙斯盾系统:
                    mplew.writeInt(0x23);
                    mplew.writeInt(0x05);
                    mplew.writeInt(Randomizer.rand(80, 90));
                    mplew.writeInt(Randomizer.rand(100, 500));
                    break;
                case 三彩箭矢:
                    mplew.writeInt(Randomizer.rand(0x0A, 0x0E));
                    mplew.writeInt(Randomizer.rand(8, 9));
                    mplew.writeInt(Randomizer.rand(0xD3, 0x13A));
                    mplew.writeInt(Randomizer.rand(0x1E, 0x3B));
                    break;
                case 狂风肆虐:
                    mplew.writeInt(Randomizer.rand(40, 50));
                    mplew.writeInt(Randomizer.rand(5, 9));
                    mplew.writeInt(Randomizer.rand(150, 180));
                    mplew.writeInt(Randomizer.rand(30, 40));
                    break;
                case 灵狐:
                    mplew.writeInt(Randomizer.rand(0x10, 0x12));
                    mplew.writeInt(0x15);
                    mplew.writeInt(0x2E);
                    mplew.writeInt(0x276);
                    break;
                case 辅助导弹:
                    mplew.writeInt(50);
                    mplew.writeInt(Randomizer.rand(11, 13));
                    mplew.writeInt(Randomizer.rand(0x1A, 0x27));
                    mplew.writeInt(500);
                    break;
                case 制裁火球:
                    mplew.writeInt(Randomizer.rand(36, 39));
                    mplew.writeInt(Randomizer.rand(5, 6));
                    mplew.writeInt(Randomizer.rand(33, 64));
                    mplew.writeInt(Randomizer.rand(512, 544));
                    break;
                case 心雷合一:
                    mplew.writeInt(Randomizer.rand(10, 50));
                    mplew.writeInt(Randomizer.rand(0, 16));
                    mplew.writeInt(Randomizer.rand(30, 50));
                    mplew.writeInt(Randomizer.rand(0, 0));
                    break;
                case 爱星能量:
                    mplew.writeInt(Randomizer.rand(36, 39));
                    mplew.writeInt(Randomizer.rand(5, 6));
                    mplew.writeInt(Randomizer.rand(33, 64));
                    mplew.writeInt(Randomizer.rand(512, 544));
                    break;
                default:
                    mplew.writeZeroBytes(16);
                    break;
            }
            if (point != null) {
                mplew.writeInt(Randomizer.rand(-600 + point.x, 600 + point.x));
                mplew.writeInt(Randomizer.rand(-300 + point.y, 50 + point.y));
            } else {
                mplew.writeInt(0);
                switch (force.getType()) {
                    case 幻影卡牌:
                        mplew.writeInt(Randomizer.rand(0, 10));
                        break;
                    default:
                        mplew.writeInt(0);
                        break;
                }
            }
            mplew.writeInt(DateUtil.getSpecialNowiTime());
            mplew.writeInt(0);
            switch (force.getType()) {
                case 幻影卡牌:
                    mplew.writeInt(Randomizer.rand(0, 10));
                    break;
                default:
                    mplew.writeInt(0);
                    break;
            }
        }
        mplew.write(0);
        switch (force.getType()) {
            case 心雷合一:
            case 制裁火球:
                mplew.writeInt(-120);
                mplew.writeInt(100);
                mplew.writeInt(120);
                mplew.writeInt(100);
                mplew.writeInt(0);
                break;
            case 爱星能量:
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] UserExplosionAttack(MapleMonster monster) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USER_EXPLOSION_ATTACK.getValue());
        mplew.writeInt(尖兵.三角进攻);
        mplew.writePos(monster.getTruePosition());
        mplew.writeInt(2);
        mplew.writeInt(monster.getObjectId());
        mplew.writeInt(0);

        return mplew.getPacket();
    }
}
