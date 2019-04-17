/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleDisease;
import client.skills.SpecialBuffInfo;
import constants.GameConstants;
import constants.JobConstants;
import constants.SkillConstants;
import constants.skills.*;
import handling.Buffstat;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleStatEffect;
import server.maps.MapleArrowsTurret;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author PlayDK
 */
public class BuffPacket {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(BuffPacket.class);

    /**
     * 幸运骰子BUFF
     *
     * @param buffid
     * @param skillid
     * @param duration
     * @param statups
     * @return
     */
    public static byte[] giveDice(int buffid, int skillid, int duration, Map<MapleBuffStat, Integer> statups) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        //Math.max(buffid / 100, Math.max(buffid / 10, buffid % 10))
        int dice = buffid >= 100 ? buffid / 100 : buffid;
        mplew.writeShort(dice); // 普通为 1-6 双幸运为 10 - 100

        mplew.writeInt(skillid); // 技能ID
        mplew.writeInt(duration);// 持续时间
        mplew.writeZeroBytes(5); // T071修改 以前为3

        mplew.writeInt(GameConstants.getDiceStat(dice, 3)); //0x14
        mplew.writeInt(GameConstants.getDiceStat(dice, 3)); //0x14
        mplew.writeInt(GameConstants.getDiceStat(dice, 4)); //0x0F
        mplew.writeZeroBytes(20); //idk
        mplew.writeInt(GameConstants.getDiceStat(dice, 2)); //0x1E
        mplew.writeZeroBytes(12); //idk
        mplew.writeInt(GameConstants.getDiceStat(dice, 5)); //0x14
        mplew.writeZeroBytes(16); //idk
        mplew.writeInt(GameConstants.getDiceStat(dice, 6)); //0x1E
        mplew.writeZeroBytes(16);
        mplew.writeZeroBytes(4); //V.114新增

        mplew.writeInt(1000);
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /**
     * 其他玩家看到角色获得骑宠BUFF
     *
     * @param chrId
     * @param statups
     * @param itemId
     * @param skillId
     * @return
     */
    public static byte[] showMonsterRiding(int chrId, List<Pair<MapleBuffStat, Integer>> statups, int itemId, int skillId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeBuffMask(mplew, statups);
        mplew.writeZeroBytes(23);
        mplew.writeInt(itemId);
        mplew.writeInt(skillId);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.write(4);
        mplew.write(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    /**
     * 角色获得 疾驰 或者 极速领域 BUFF
     *
     * @param statups
     * @param duration
     * @param skillid
     * @return
     */
    public static byte[] givePirateBuff(Map<MapleBuffStat, Integer> statups, int duration, int skillid) {
        boolean infusion = skillid == 冲锋队长.极速领域 || skillid == 奇袭者.极速领域_新 || skillid % 10000 == 8006;
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        mplew.writeZeroBytes(5 + 4); //V.114修改
        statups.forEach((left, right) -> {
            mplew.writeInt(right);
            mplew.writeLong(skillid);
            mplew.writeZeroBytes(infusion ? 6 : 1);
            mplew.writeShort(duration);
        });
        mplew.writeInt(infusion ? 600 : 0);
        mplew.write(1);
        if (!infusion) {
            mplew.write(4);
        }
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 其他玩家看到角色获得 疾驰
     */
    public static byte[] giveForeignDash(Map<MapleBuffStat, Integer> statups, int duration, int chrId, int skillid) {
        boolean infusion = skillid == 冲锋队长.极速领域 || skillid == 奇袭者.极速领域_新 || skillid % 10000 == 8006;
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeBuffMask(mplew, statups);
        if (!infusion) {
            mplew.writeZeroBytes(16);
        }
        mplew.writeZeroBytes(7); //V.114修改
        statups.forEach((left, right) -> {
            mplew.writeInt(right);
            mplew.writeLong(skillid);
            mplew.writeZeroBytes(infusion ? 6 : 1);
            mplew.writeShort(duration);
        });
        mplew.writeShort(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 角色给怪物上 导航辅助 BUFF
     */
    public static byte[] give导航辅助(int skillid, int mobid, int x) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.导航辅助);
        mplew.writeZeroBytes(9); //V.114修改
        mplew.writeInt(x);
        mplew.writeInt(skillid);
        mplew.writeZeroBytes(5);
        mplew.writeInt(mobid);
        mplew.writeInt(0);
        mplew.writeInt(720); //[D0 02 00 00]
        mplew.writeZeroBytes(5);

        return mplew.getPacket();
    }

    /*
     * 神秘瞄准术 BUFF
     */
    public static byte[] give神秘瞄准术(int x, int skillId, int duration) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.神秘瞄准术);
        mplew.writeShort(x);
        mplew.writeInt(skillId);
        mplew.writeInt(duration); //默认为5秒
        mplew.writeZeroBytes(18); //V.114修改 以前 14

        return mplew.getPacket();
    }

    /*
     * 角色自己看到能量获得BUFF
     */
    public static byte[] giveEnergyCharge(int bar, int buffId, boolean fullbar, boolean consume) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.能量获得);
        mplew.writeZeroBytes(5);
        mplew.writeInt(fullbar || (consume && bar > 0) ? buffId : 0); //满能量和消耗能量 写技能ID
        mplew.writeInt(Math.min(bar, 10000)); // 0 = 没有能量, 10000 = 满能量
        mplew.writeInt(0);
        mplew.writeInt(0); //[01 01 00 00] 当技能为3转且满能量 这个地方是这个
        mplew.writeZeroBytes(6);
        mplew.write(0x01); //这个地方是随机数字
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 其他玩家看到角色自己看到能量获得BUFF
     */
    public static byte[] showEnergyCharge(int chrId, int bar, int buffId, boolean fullbar, boolean consume) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeSingleMask(mplew, MapleBuffStat.能量获得);
        mplew.writeZeroBytes(19);
        mplew.writeInt(fullbar || (consume && bar > 0) ? buffId : 0); //满能量和消耗能量 写技能ID
        mplew.writeInt(Math.min(bar, 10000)); // 0 = 没有能量, 10000 = 满能量
        mplew.writeZeroBytes(11); //V.114.1 修改 没有持续时间
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 更新夜光当前界面的光暗点数
     */
    public static byte[] updateLuminousGauge(int points, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LUMINOUS_COMBO.getValue());
        mplew.writeInt(points);
        mplew.write(type);

        return mplew.getPacket();
    }

    /*
     * 是否开启尖兵能量
     */
    public static byte[] startPower() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.尖兵电力);
        mplew.writeShort(1);
        mplew.writeInt(1);
        mplew.writeZeroBytes(9);
        mplew.writeInt(3);
        mplew.writeZeroBytes(12);

        return mplew.getPacket();
    }

    /*
     * 尖兵能量
     */
    public static byte[] updatePowerCount(int skillId, int count) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.尖兵电力);
        mplew.writeShort(count);
        mplew.writeInt(skillId);
        mplew.writeInt(0);
        mplew.writeZeroBytes(18); //V.114修改 以前 14

        return mplew.getPacket();
    }

    /*
     * 幻影 - 卡牌审判
     */
    public static byte[] give卡牌审判(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, int theStat) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        statups.forEach((left, right) -> {
            mplew.writeShort(right);
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        });
        mplew.writeZeroBytes(5);
        mplew.writeInt(theStat);
        mplew.writeZeroBytes(8);
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] give狂龙变形值(int bar) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.变形值);
        mplew.writeInt(Math.min(bar, 700)); // 0 = no bar, 1000 = full bar
        mplew.writeShort(0);
//        mplew.write(HexTool.getByteArrayFromHexString("78 90 2A EC"));
        mplew.writeInt((int) PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeZeroBytes(5); //V.114修改
        mplew.writeInt(bar >= 700 ? 3 : bar >= 300 ? 2 : bar >= 100 ? 1 : 0);
        mplew.writeZeroBytes(13); //V.114修改 以前 9

        return mplew.getPacket();
    }

    public static byte[] show狂龙变形值(int chrId, int bar) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeSingleMask(mplew, MapleBuffStat.变形值);
        mplew.writeInt(Math.min(bar, 1000));
        mplew.writeZeroBytes(27);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showPP(int pp, int job) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.PP);
        mplew.writeShort(pp);
        mplew.writeInt(job);
        mplew.writeZeroBytes(22);

        return mplew.getPacket();
    }

    /*
     * 狂龙战士 - 剑刃之壁
     */
    public static byte[] give剑刃之壁(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, int SECONDARY_STAT_Event2emId, int type) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        statups.forEach((left, right) -> {
            mplew.writeShort(right);
            mplew.writeInt(buffid);
            mplew.writeInt(bufflength);
        });
        boolean isNormal = buffid == 狂龙战士.剑刃之壁 || buffid == 狂龙战士.剑刃之壁_变身;
        mplew.writeZeroBytes(5);
        mplew.writeInt(type); //x ?
        mplew.writeInt(isNormal ? 3 : 5); //bulletCount ?
        mplew.writeInt(SECONDARY_STAT_Event2emId);
        mplew.writeInt(isNormal ? 3 : 5); //mobCount ?
        mplew.writeZeroBytes(isNormal ? 16 : 24);
        mplew.writeInt(0);
        mplew.write(0x01);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] show剑刃之壁(int chrId, int buffid, Map<MapleBuffStat, Integer> statups, int SECONDARY_STAT_Event2emId, int type) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeBuffMask(mplew, statups);
        statups.forEach((left, right) -> {
            mplew.writeShort(right);
            mplew.writeInt(buffid);
        });
        boolean isNormal = buffid == 狂龙战士.剑刃之壁 || buffid == 狂龙战士.剑刃之壁_变身;
        mplew.writeZeroBytes(3);
        mplew.writeInt(type);
        mplew.writeInt(isNormal ? 3 : 5);
        mplew.writeInt(SECONDARY_STAT_Event2emId);
        mplew.writeInt(isNormal ? 3 : 5);
        mplew.writeZeroBytes(isNormal ? 22 : 26); //进阶的多4个00
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] show隐藏碎片(int buffid, int skilllevel, List<Pair<MapleBuffStat, Integer>> statups) {

        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        mplew.writeShort(0xB41);
        mplew.writeInt(预备兵.隐藏碎片);
        mplew.writeZeroBytes(13);
        mplew.writeInt(0x02);
        mplew.writeInt(预备兵.隐藏碎片);
        mplew.writeInt(0x0A);
        long time = DateUtil.getTime(System.currentTimeMillis());
        mplew.writeInt(DateUtil.getSpecialNowiTime());
        mplew.writeInt(DateUtil.getSpecialNowiTime());
        mplew.writeLong(0);

        mplew.writeInt(buffid);
        mplew.writeInt(skilllevel);
        mplew.writeLong(time);
        mplew.writeInt(0);

        for (int i = 0; i < 2; i++) {
            mplew.writeHexString("01 00 00 00 63 C4 C9 01 0A 00 00 00");
            mplew.writeLong(time);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.writeHexString("00 00 00 00 01 00 00 00 00");

        return mplew.getPacket();
    }

    public static byte[] giveSoulGauge(int count, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.灵魂武器);
        mplew.writeShort(count);
        mplew.writeInt(skillid);//skill
        mplew.writeInt(0);
        mplew.writeInt(1000);
        mplew.writeInt(skillid);//soulskill
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeLong(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] cancelSoulGauge() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.灵魂武器);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] giveSoulEffect(int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.灵魂技能);
        mplew.writeShort(0);
        mplew.writeInt(skillid);
        mplew.writeInt(640000);
        mplew.writeLong(0);
        mplew.writeShort(8);
        mplew.writeLong(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] giveForeignSoulEffect(int cid, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeSingleMask(mplew, MapleBuffStat.灵魂技能);
        mplew.writeInt(skillid);
        mplew.writeLong(0x60000000000L);
        mplew.writeLong(0);
        mplew.writeLong(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] cancelForeignSoulEffect(int cid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(cid);
        writeSingleMask(mplew, MapleBuffStat.灵魂技能);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] show灵魂武器(int buffid, int point) {

        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());

        List<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();
        statups.add(new Pair<>(MapleBuffStat.灵魂武器, 0));
        if (buffid > 0) {
            statups.add(new Pair<>(MapleBuffStat.增加魔法攻击力, 0));
            statups.add(new Pair<>(MapleBuffStat.增加物理攻击力, 0));
        }
        writeBuffMask(mplew, statups);
        mplew.writeShort(point);
        mplew.writeLong(buffid);
        mplew.writeShort(1000);
        mplew.writeShort(0);
        mplew.writeLong(buffid);
        mplew.writeShort(0);
        mplew.writeZeroBytes(3);

        if (buffid > 0) {
            for (int i = 0; i < 2; i++) {
                mplew.writeInt(1);
                mplew.writeInt(2590000);
                mplew.writeZeroBytes(13);
                mplew.writeHexString("75 2B 7D");
            }
        }

        mplew.writeHexString("00 00 00 00 01 00 00 00 00");

        return mplew.getPacket();
    }

    public static byte[] show灵魂技能(int buffid) {

        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());

        List<Pair<MapleBuffStat, Integer>> statups = new ArrayList<>();
        writeSingleMask(mplew, MapleBuffStat.灵魂技能);
        mplew.writeShort(0);
        mplew.writeInt(buffid);
        mplew.write(0);
        mplew.writeShort(2500);
        mplew.writeZeroBytes(23);

        return mplew.getPacket();
    }

    public static byte[] giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        boolean isWriteIntValue = false;
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            if (SkillConstants.isWriteBuffIntValue(entry.getKey())) {
                isWriteIntValue = true;
            }
        }
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            SpecialBuffInfo stackBuffInfo = chr.getStackBuffInfo(entry.getKey(), buffid);
            if (!entry.getKey().canStack()) {
                if (isWriteIntValue) {
                    mplew.writeInt(stackBuffInfo != null ? stackBuffInfo.value : entry.getValue());
                } else {
                    mplew.writeShort(stackBuffInfo != null ? stackBuffInfo.value : (entry.getKey() == MapleBuffStat.SECONDARY_STAT_MobZoneState ? 1 : entry.getValue()));
                }
                mplew.writeInt(stackBuffInfo != null ? stackBuffInfo.buffid : buffid);
                int n4 = stackBuffInfo != null ? stackBuffInfo.bufflength : bufflength;
                mplew.writeInt(n4 == 2100000000 ? -1 : n4);
            }
        }
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            if (SkillConstants.f(entry.getKey()) && !entry.getKey().canStack()) {
                if (isWriteIntValue) {
                    mplew.writeInt(entry.getValue());
                } else {
                    mplew.writeShort(entry.getValue());
                }
                mplew.writeInt(buffid);
                mplew.writeInt(bufflength);
            }
        }
        // 没有特殊数据的话跳过 9byte
        writeBuffData(mplew, statups, effect, chr);
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            if (entry.getKey().canStack() && SkillConstants.isWriteBuffIntValue(entry.getKey())) {
                mplew.writeInt(entry.getValue());
                mplew.writeInt(buffid);
                mplew.write(1);
                mplew.writeInt(2);
                if (entry.getKey() == MapleBuffStat.极速领域) {
                    mplew.write(1);
                    mplew.writeInt(2);
                    mplew.writeShort(104);
                    continue;
                }
                if (entry.getKey() == MapleBuffStat.SECONDARY_STAT_RideVehicleExpire) {
                    mplew.writeShort(10);
                }
            }
        }
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            if (entry.getKey().canStack() && !SkillConstants.isWriteBuffIntValue(entry.getKey())) {
                List<SpecialBuffInfo> specialBuffInfo = chr.getSpecialBuffInfo(entry.getKey());
                mplew.writeInt(specialBuffInfo.size());
                specialBuffInfo.forEach(specialBuffInfo1 -> {
                    mplew.writeInt(specialBuffInfo1.buffid);
                    mplew.writeInt(specialBuffInfo1.value);
                    mplew.writeInt((int) specialBuffInfo1.time);
                    mplew.writeInt(0);
                    mplew.writeInt(specialBuffInfo1.bufflength);
                    int n6 = 0;
                    mplew.writeInt(n6);
                    for (int i2 = 0; i2 < n6; ++i2) {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    }
                });
            }
        }
        if (statups.containsKey(MapleBuffStat.隐身术)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_456)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.飞行骑乘)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK513)) {
            int n7;
            for (n7 = 0; n7 <= 0; ++n7) {
                mplew.write(n7);
            }
            for (n7 = 0; n7 <= 0; ++n7) {
                mplew.write(n7);
            }
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK514)) {
            mplew.write(0);
            for (int i3 = 0; i3 <= 0; ++i3) {
                mplew.write(i3);
            }
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK515)) {
            mplew.write(0);
            for (int i4 = 0; i4 <= 0; ++i4) {
                mplew.write(i4);
            }
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK521)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        for (Map.Entry<MapleBuffStat, Integer> entry : statups.entrySet()) {
            if (SkillConstants.isWriteBuffByteData(entry.getKey())) {
                mplew.write(0);
            }
        }
        mplew.writeInt(0);
        return mplew.getPacket();
    }

//    public static byte[] giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect, MapleCharacter chr) {
//        log.trace("调用");
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
//        writeBuffMask(mplew, statups);
//        boolean special = false;
//        boolean isMountBuff = false;
//        boolean isZeroUnknown = buffid == 神之子.圣洁之力 || buffid == 神之子.神圣迅捷;
//        boolean isWriteIntSkill = buffid == 双弩.精神注入 || buffid == 爆莉萌天使.力量转移 || buffid == 船长.指挥船员;
//        boolean darklight = false;
//        int count = 0; //一些特殊技能的处理 侠盗的击杀点数 ，奇袭者闪电的无视防御次数 ， 夜光黑暗高潮的次数
//        List<MapleBuffStat> buffStat = new ArrayList<>(); //将BUFF在的BuffStat放到列表 用来判断BUFF中是否包含
//        Map<MapleBuffStat, Integer> ordinaryStatups = new EnumMap<>(MapleBuffStat.class); //普通的BUFF属性
//        Map<MapleBuffStat, Integer> speciaStatups = new EnumMap<>(MapleBuffStat.class); //特殊的BUFF属性
//        for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
//            if (stat.getKey() == MapleBuffStat.骑兽技能) {
//                isMountBuff = true;
//            } else if (stat.getKey() == MapleBuffStat.月光转换 || stat.getKey() == MapleBuffStat.神圣保护) {
//                isZeroUnknown = true;
//            } else if (stat.getKey() == MapleBuffStat.战法灵气 || stat.getKey() == MapleBuffStat.寒冰灵气 || stat.getKey() == MapleBuffStat.月光转换 || stat.getKey() == MapleBuffStat.抗震防御) {
//                count = 1;
//                isZeroUnknown = true;
//            } else if (stat.getKey() == MapleBuffStat.百分比无视防御 && buffid == 奇袭者.元素_闪电) {
//                count = Math.min(stat.getValue() / 5, 5);
//            } else if (stat.getKey() == MapleBuffStat.黑暗高潮) {
//                if (buffid == 夜光.黑暗高潮) {
//                    count = stat.getValue();
//                } else if (buffid == 尖兵.双重防御) { //回避多少次后消失 默认为10次 但这个BUFF的值 初始为 5
//                    count = effect.getX(); //设置为默认次数
//                    if (effect.getProp() > stat.getValue()) {
//                        int prop = effect.getProp() - stat.getValue();
//                        count -= prop / effect.getY();
//                        if (count < 0) {
//                            count = 0;
//                        }
//                    }
//                }
//                isZeroUnknown = true;
//            } else if (stat.getKey() == MapleBuffStat.元素冲击) {
//                count = Math.min(stat.getValue() / 5, 5);
//            } else if (stat.getKey() == MapleBuffStat.光暗转换_2) {
//                darklight = true;
//                continue;
//            } else if (stat.getKey().equals(MapleBuffStat.敛财术)) {
//                count = chr.getBuffedIntValue(MapleBuffStat.敛财术);
//            }
//            buffStat.add(stat.getKey());
//            if (stat.getKey().canStack()) {
//                speciaStatups.put(stat.getKey(), stat.getValue());
//            } else {
//                ordinaryStatups.put(stat.getKey(), stat.getValue());
//            }
//        }
//        //开始处理普通的BUFF属性
//        for (Map.Entry<MapleBuffStat, Integer> stat : ordinaryStatups.entrySet()) {
//            if (stat.getKey() == MapleBuffStat.击杀点数 && buffid == 侠盗.侠盗本能) {
//                count = stat.getValue();
//                isZeroUnknown = true;
//                break; //跳出和结束这个循环
//            }
//            //貌似有些要写Int
//            if (isMountBuff || isWriteIntSkill || stat.getKey() == MapleBuffStat.影分身 || stat.getKey() == MapleBuffStat.伤害置换 || stat.getKey() == MapleBuffStat.重生符文 || stat.getKey() == MapleBuffStat.三彩箭矢) {
//                mplew.writeInt(stat.getValue());
//            } else if (buffid == 爆莉萌天使.灵魂凝视) {
//                mplew.writeShort(stat.getValue() / 2); //这个地方我用是 x 而 封包必须这个地方是 y 下面 x
//                mplew.writeShort(stat.getValue());
////            } else if (stat.getKey() == MapleBuffStat.鹰眼) { //好像这个有点特殊 是2个 0x14 也就是1个暴击概率1个暴击最大伤害
////                mplew.write(stat.getValue().byteValue());
////                mplew.write(stat.getValue().byteValue());
//            } else {
//                mplew.writeShort(stat.getValue());
//            }
//
//            switch (buffid) {
//                case 恶魔复仇者.血之契约:
//                    mplew.writeInt(buffid == 恶魔复仇者.血之契约 ? 0 : buffid); //好像血之契约这个地方的BUFFID写的 0
//                    break;
//                case 黑骑士.灵魂助力统治:
//                case 黑骑士.灵魂助力震惊:
//                    mplew.writeInt(黑骑士.灵魂助力);
//                    break;
//                default:
//                    mplew.writeInt(buffid);
//            }
//
////            if (!buffStat.contains(MapleBuffStat.极限射箭)) {
////                mplew.writeInt(bufflength);
////            }
//            mplew.writeInt(bufflength);
//            if (stat.getKey().isSpecial()) { //未知 有些特殊的BUFF 这个地方要多[00 00 00 00]
//                special = true;
//            }
//            if (ServerConstants.isShowPacket()) {
//                log.info("技能ID: " + buffid + " ShortStat: " + stat.getValue() + " 持续时间: " + bufflength + " 转换: " + bufflength / 1000 + "秒");
//            }
//        }
//        //发送中间的字节
//        mplew.writeShort(0);
//        switch (buffid) {
//            case 剑豪.拔刀姿势:
//                mplew.writeHexString("E8 79 9D FD 00 00 00");
//                break;
//            default:
//                mplew.writeZeroBytes(3);
//                if (special) {
//                    if (buffStat.contains(MapleBuffStat.百分比无视防御) && buffid == 奇袭者.元素_闪电) {
//                        mplew.writeInt(count);
//                    } else {
//                        mplew.writeInt(0);
//                    }
//                }
//                break;
//        }
//
//        if (isZeroUnknown) {
//            mplew.write(count);
//        } else if (buffStat.contains(MapleBuffStat.火眼晶晶)) {
//            mplew.writeInt(buffid == 神射手.火眼晶晶 && chr.getTotalSkillLevel(神射手.火眼晶晶_无视防御) > 0 ? 5 : 0); //这个地方是否带无视怪物防御
//        } else if (buffStat.contains(MapleBuffStat.极限射箭)) {
//            mplew.writeInt(effect.getX()); //减少的百分比防御
//            mplew.writeInt(effect.getZ()); //暴击最小伤害增加
//        } else if (buffStat.contains(MapleBuffStat.交叉锁链)) {
//            mplew.writeInt(0x01);
//        } else if (buffStat.contains(MapleBuffStat.生命潮汐)) {
//            mplew.writeInt(buffid == 夜光.生命潮汐 ? effect.getProp() : buffid == 恶魔复仇者.血之契约 ? chr.getStat().getCurrentMaxHp() : 0);
//        } else if (buffStat.contains(MapleBuffStat.重生符文)) {
//            mplew.writeInt(122); //7A 00 00 00 不知道是怎么处理的
//        } else if (buffStat.contains(MapleBuffStat.三彩箭矢)) {
//            mplew.writeInt(chr.getSpecialStat().getArrowsMode() + 1);
//        } else if (buffStat.contains(MapleBuffStat.灵魂助力)) {
//            mplew.writeInt(effect.isOnRule() ? 黑骑士.灵魂助力统治 : 黑骑士.灵魂助力);
//            mplew.writeInt(0);
//        } else if (buffStat.contains(MapleBuffStat.元素冲击)) {
//            mplew.write(count); //秒杀概率?
//            mplew.writeShort(count * 12); //攻击加成
//            mplew.write(count * 2);
//            mplew.write(count * 2);
//        } else if (buffStat.contains(MapleBuffStat.招魂结界)) {
//            mplew.writeInt(0x01);
//        } else if (buffStat.contains(MapleBuffStat.激素狂飙)) {
//            mplew.write(1);
//        }
//
//        if (darklight) {
//            mplew.writeInt(buffid);
//            mplew.writeInt(DateUtil.getSpecialNowiTime());
//            mplew.writeLong(0);
//            mplew.writeInt(-1);
//            mplew.writeLong(10000);
//        } else {
//            mplew.writeInt(count);
//        }
//
//        //处理9个 00 之后的状态
//        if (buffStat.contains(MapleBuffStat.飞行骑乘) && buffid != 龙神.龙神) {
//            mplew.writeInt(buffid == 林之灵.伊卡飞翔 && chr.getSkillLevel(林之灵.编队掩护飞行) > 0 ? 林之灵.编队掩护飞行 : 0); //必须写个int
//        } else if (buffStat.contains(MapleBuffStat.飞翔)) {
//            mplew.write(0);
//        }
//        //开始处理特殊的BUFF属性
//        speciaStatups.entrySet().stream().filter(stat -> stat.getKey() == MapleBuffStat.骑兽技能).forEach(stat -> {
//            mplew.writeInt(stat.getValue()); //骑宠ID
//            mplew.writeInt(buffid); //技能ID
//            mplew.write(0); //当为机械师的骑宠ID这个地方为 1
//            mplew.writeInt(0); //bufflength 貌似骑宠的为0 当为机械师的骑宠ID这个地方为 1
//            mplew.writeInt(bufflength / 1000);
//            mplew.writeShort(0);
//        });
//        boolean finalIsMountBuff = isMountBuff;
//        speciaStatups.entrySet().stream().filter(stat -> stat.getKey() != MapleBuffStat.骑兽技能).forEach(stat -> {
//            List<SpecialBuffInfo> buffs = chr.getSpecialBuffInfo(stat.getKey(), buffid, stat.getValue(), bufflength);
//            mplew.writeInt(buffs.size()); //这个地方是有多少个重复的特殊BUFF 是1个循环
//            for (SpecialBuffInfo info : buffs) {
//                mplew.writeInt(info.buffid);
//                mplew.writeInt(info.value);
//                mplew.writeInt((int) info.time); //未知 反正是个很大的数字而且是变动的 [08 F1 55 38]
//                mplew.writeInt((int) info.time); //V.114新增
//                mplew.writeInt(finalIsMountBuff ? 0 : info.bufflength); //这个地方如果带有骑宠好像是 0
//                mplew.writeInt(0);
//                if (ServerConstants.isShowPacket()) {
//                    log.info("技能ID: " + info.buffid + " LongStat: " + info.value + " 持续时间: " + info.bufflength + " 转换: " + info.bufflength / 1000 + "秒");
//                }
//            }
//        });
//        //-------------------------------------------------------------------
//        mplew.writeInt(0); //未知 不知道是范围还是其他的 [E8 03 00 00]
//        mplew.writeShort(0);
//        mplew.write(0);
//        mplew.write(0);
//        mplew.write(0); //V.112.1新增 有时为 00
//
//        statups.keySet().forEach(it -> {
//            if (SkillConstants.isWriteBuffByteData(it)) {
//                mplew.write(0);
//            }
//        });
//        mplew.writeInt(0); //V.112.1新增
//
//        return mplew.getPacket();
//    }

    /*
     * 减益buff,怪给角色
     */
    public static byte[] giveDebuff(MapleDisease statups, int x, int skillid, int level, int duration) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        EnumMap<MapleBuffStat, Object> enumMap = new EnumMap<>(MapleBuffStat.class);

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, statups);

//        if (!statups.getBuffStat().canStack()) {
//            if (SkillConstants.isWriteDeBuffIntValue(statups.getBuffStat())) {
//                mplew.writeInt(x);
//            } else {
//                mplew.writeShort(x);
//            }
//            mplew.writeShort(skillid);
//            mplew.writeShort(level);
//            mplew.writeInt(duration);
//        }
//        writeDeBuffData(mplew, enumMap, null, null);
//        if (statups.getBuffStat().canStack() && !SkillConstants.isWriteNeedPointValue(statups.getBuffStat())) {
//            mplew.writeInt(1);
//            mplew.writeShort(skillid);
//            mplew.writeShort(level);
//            mplew.writeInt(-94);
//            mplew.writeInt((int)System.currentTimeMillis());
//            mplew.writeInt(1);
//            mplew.writeInt(duration);
//        }
//        mplew.writeShort(0);
//        mplew.write(0);
//        mplew.write(0);
//        mplew.write(1);
//        mplew.write(0);
//        mplew.writeInt(0);
//        mplew.writeZeroBytes(5);

        mplew.writeShort(x);
        mplew.writeShort(skillid);
        mplew.writeShort(level);
        mplew.writeInt(duration);
        mplew.writeZeroBytes(4); //未知
        if (skillid == MapleDisease.缓慢.getDisease()) { //好像是减速的时候 这个地方多1个
            mplew.write(0); //不知道是等级还是固定的 0x02
        }
        mplew.writeZeroBytes(5);
        mplew.writeShort(0); //[84 03] Delay
        mplew.writeZeroBytes(3);
        if (skillid == MapleDisease.缓慢.getDisease()) {
            mplew.write(3);
        } else {
            mplew.write(4);
        }
        mplew.writeZeroBytes(5); //未知 V.112新增

        return mplew.getPacket();
    }

    public static void writeBuffData(MaplePacketLittleEndianWriter mplew, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect, MapleCharacter player) {
        int n2;
        if (statups.containsKey(MapleBuffStat.灵魂武器)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_FullSoulMP)) {
            mplew.writeInt(0);
        }
        int n3 = 0;
        mplew.writeShort(n3);
        for (n2 = 0; n2 < n3; ++n2) {
            mplew.writeInt(0);
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.拔刀姿势)) {
            mplew.writeInt(-剑豪.拔刀姿势);
        }
        mplew.write(0);
        mplew.write(0);
        mplew.write(0);
        if (statups.containsKey(MapleBuffStat.幸运骰子)) {
            for (n2 = 0; n2 < 22; ++n2) {
                mplew.writeInt(0);
            }
        }
        if (statups.containsKey(MapleBuffStat.击杀点数)) {
            mplew.write(statups.get(MapleBuffStat.击杀点数));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PinkbeanRollingGrade)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.卡牌审判)) {
            switch (statups.get(MapleBuffStat.卡牌审判)) {
                case 1:
                case 2:
                case 4: {
                    mplew.writeInt(effect.getSourceid() == 幻影.卡牌审判 ? 10 : 20);
                    break;
                }
                case 3: {
                    mplew.writeInt(2020);
                    break;
                }
                default: {
                    mplew.writeInt(0);
                }
            }
        }
        if (statups.containsKey(MapleBuffStat.黑暗高潮)) {
            mplew.write(Math.max(statups.get(MapleBuffStat.黑暗高潮), 1));
        }
        if (statups.containsKey(MapleBuffStat.三位一体)) {
            mplew.write(statups.get(MapleBuffStat.三位一体) / 5);
        }
        if (statups.containsKey(MapleBuffStat.元素冲击)) {
            mplew.write(0);
            mplew.writeShort(0);
            mplew.write(0);
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.生命潮汐)) {
            n2 = 0;
            switch (statups.get(MapleBuffStat.生命潮汐)) {
                case 1: {
                    n2 = effect.getX();
                    break;
                }
                case 2: {
                    n2 = effect.getProp();
                    break;
                }
                case 3: {
                    n2 = player.getStat().getHp();
                }
            }
            mplew.writeInt(n2);
        }
        if (statups.containsKey(MapleBuffStat.神圣保护)) {
            n2 = statups.get(MapleBuffStat.神圣保护) == 2 && player.getBuffSource(MapleBuffStat.神圣保护) == 2311012 ? 1 : 0;
            mplew.write(n2);
        }
        if (statups.containsKey(MapleBuffStat.光暗转换)) {
            n2 = statups.get(MapleBuffStat.光暗转换);
            mplew.writeInt(n2 == 1 ? effect.getSourceid() : 夜光.月蚀);
            mplew.writeInt(effect.getDuration());
            mplew.writeInt(n2 == 1 ? 0 : 夜光.太阳火焰);
            mplew.writeInt(n2 == 1 ? 0 : effect.getDuration());
            mplew.writeInt(-1);
            mplew.writeInt(player.getSpecialStat().getDarkLight());
            mplew.writeInt(0); //n2 == 2 && player.getenattacke() ? 1 : 0

//            mplew.writeInt(buffid);
//            mplew.writeInt(DateUtil.getSpecialNowiTime());
//            mplew.writeLong(0);
//            mplew.writeInt(-1);
//            mplew.writeLong(10000);
        }
        if (statups.containsKey(MapleBuffStat.百分比无视防御)) {
            mplew.writeInt(statups.get(MapleBuffStat.百分比无视防御) / 5);
        }
        if (statups.containsKey(MapleBuffStat.剑刃之壁)) {
            PacketHelper.write剑刃之壁(mplew, player, effect.getSourceid());
        }
        if (statups.containsKey(MapleBuffStat.变形值)) {
            n2 = 0;
            if (player.getMorphCount() >= 100) {
                n2 = 1;
            } else if (player.getMorphCount() >= 300) {
                n2 = 2;
            }
            mplew.writeInt(n2);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_MobZoneState)) {
            ArrayList<Object> arrayList = new ArrayList<>();
            arrayList.add(statups.get(MapleBuffStat.SECONDARY_STAT_MobZoneState));
            arrayList.add(-1);
            for (Object anArrayList : arrayList) {
                int n4 = (Integer) anArrayList;
                mplew.writeInt(n4);
            }
        }
        if (statups.containsKey(MapleBuffStat.缓慢)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.寒冰灵气)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.抗震防御)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.受到伤害减少百分比)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.BOSS伤害)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.幸运钱)) {
            mplew.writeInt(0);
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.月光转换)) {
            mplew.write(player.getBuffedIntValue(MapleBuffStat.日月轮转));
        }
        if (statups.containsKey(MapleBuffStat.灵魂助力)) {
            mplew.writeInt(effect.getSourceid());
            mplew.writeInt(effect.getSourceid() == 黑骑士.灵魂助力震惊 ? 黑骑士.灵魂助力震惊 : 0);
        }
        if (statups.containsKey(MapleBuffStat.交叉锁链)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.重生契约)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.重生契约));
        }
        if (statups.containsKey(MapleBuffStat.极限射箭)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.三彩箭矢)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_ImmuneBarrier)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.圣洁之力)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.神圣迅捷)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.防甲穿透)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.火眼晶晶)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.进阶祝福)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DebuffTolerance)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DotHealHPPerSecond)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.招魂结界)) {
            mplew.writeInt((Integer) statups.get(MapleBuffStat.招魂结界));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_KnockBack)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.灵魂吸取专家)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_SSFShootingAttack)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.战法灵气)) {
            mplew.writeInt(738263040);
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.结合灵气)) {
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PinkbeanAttackBuff)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.皇家守护)) {
            mplew.writeInt(statups.get(MapleBuffStat.增加物理攻击力));
            mplew.writeInt(statups.get(MapleBuffStat.皇家守护));
        }
        if (statups.containsKey(MapleBuffStat.灵魂链接)) {
            mplew.writeInt(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId == player.getId() ? statups.get(MapleBuffStat.灵魂链接) : 0);
            mplew.writeBool(player.getParty() == null || player.getParty() != null && player.getParty().getMembers().size() == 1);
            mplew.writeInt(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId);
            mplew.writeInt(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId != player.getId() ? (int) player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).effect.getLevel() : 0);
        }
        if (statups.containsKey(MapleBuffStat.激素狂飙)) {
            mplew.write(effect.getSourceid() == 战神.激素狂飙 ? 1 : 0);
        }
        if (statups.containsKey(MapleBuffStat.爆破弹夹)) {
            mplew.write(0); //player.getBullet()
            mplew.writeShort(0); //player.getCylinder()
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_450)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.呼啸暴风)) {
            mplew.writeShort(0);
            mplew.write(0);
        }
        mplew.writeInt(player != null && player.getSpecialStat().isEnergyFull() ? player.getBuffSource(MapleBuffStat.能量获得) : 0);
        if (statups.containsKey(MapleBuffStat.不消耗HP)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.隐身术)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.戴米安刻印)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.精灵元素)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.暴击蓄能)) {
            mplew.writeInt(effect.getQ());
        }
        if (statups.containsKey(MapleBuffStat.炎术引燃)) {
            mplew.writeInt(1);
        }
        if (statups.containsKey(MapleBuffStat.敛财术)) {
            mplew.writeInt(0); //player != null ? player.getStat().getBuffValue() : 0
        }
        if (statups.containsKey(MapleBuffStat.神圣归一)) {
            mplew.writeShort(effect.getZ());
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK403)) {
            mplew.writeShort(1);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK404)) {
            mplew.writeShort(35);
        }
        if (statups.containsKey(MapleBuffStat.神奇圆环)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_VampDeath)) {
            mplew.writeInt(3);
        }
    }

    /*
     * 其他玩家看到别人获得负面BUFF状态
     */
    public static byte[] giveForeignDebuff(int chrId, MapleDisease statups, int skillid, int level, int x) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeSingleMask(mplew, statups);
        if (skillid == 125) { //中毒
            mplew.writeShort(0);
            mplew.write(0); //todo test
        }
        if (skillid == 184) {
            mplew.writeInt(x);
        } else {
            mplew.writeShort(x);
        }
        mplew.writeShort(skillid);
        mplew.writeShort(level);
        mplew.writeZeroBytes(3);
        mplew.writeZeroBytes(16);
        mplew.writeZeroBytes(4); //V.114新增
        mplew.writeShort(900); //Delay
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 其他玩家看到别人取消负面BUFF状态
     */
    public static byte[] cancelForeignDebuff(int chrId, MapleDisease mask) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeSingleMask(mplew, mask);
        mplew.write(3);
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 其他玩家看到别人取消BUFF状态
     */
    public static byte[] cancelForeignBuff(int chrId, List<MapleBuffStat> statups) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeMask(mplew, statups);
        mplew.write(3);
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 其他玩家看到别人取消BUFF状态
     */
    public static byte[] cancelForeignBuff(int chrId, MapleBuffStat buffstat) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_FOREIGN_BUFF.getValue());
        mplew.writeInt(chrId);
        writeSingleMask(mplew, buffstat);
        mplew.write(3);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] giveForeignBuff(MapleCharacter player, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_FOREIGN_BUFF.getValue());
        mplew.writeInt(player.getId());
        writeForeignBuff(mplew, player, statups, false);
        mplew.writeShort(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static void writeForeignBuff(MaplePacketLittleEndianWriter mplew, MapleCharacter player, Map<MapleBuffStat, Integer> statups, boolean isChrinfo) {
        int sourceid;
        int n3;
        writeBuffMask(mplew, statups);
        if (statups.containsKey(MapleBuffStat.移动速度)) {
            mplew.write(player.getBuffedIntValue(MapleBuffStat.移动速度));
        }
        if (statups.containsKey(MapleBuffStat.斗气集中)) {
            mplew.write(player.getBuffedIntValue(MapleBuffStat.斗气集中));
        }
        if (statups.containsKey(MapleBuffStat.属性攻击)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.属性攻击));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.属性攻击));
        }
        if (statups.containsKey(MapleBuffStat.元素冲击)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.元素冲击));
        }
        if (statups.containsKey(MapleBuffStat.昏迷)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.昏迷).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.昏迷).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Shock)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.黑暗)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.黑暗).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.黑暗).level);
        }
        if (statups.containsKey(MapleBuffStat.封印)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.封印).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.封印).level);
        }
        if (statups.containsKey(MapleBuffStat.虚弱)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.虚弱).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.虚弱).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_WeaknessMdamage)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.诅咒)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.诅咒).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.诅咒).level);
        }
        if (statups.containsKey(MapleBuffStat.缓慢)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.缓慢).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.缓慢).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PvPRaceEffect)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_IceKnight)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_IceKnight));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IceKnight));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DisOrder)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Explosion)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Thread)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Team)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_Team));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_Team));
        }
        if (statups.containsKey(MapleBuffStat.中毒)) {
            mplew.writeShort(1);
        }
        if (statups.containsKey(MapleBuffStat.中毒)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.中毒).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.中毒).level);
        }
        if (statups.containsKey(MapleBuffStat.影分身)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.影分身));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.影分身));
        }
        if (statups.containsKey(MapleBuffStat.隐身术)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.无形箭弩)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.变身效果)) {
            mplew.writeShort(player.getStatForBuff(MapleBuffStat.变身效果).getMorph(player));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.变身效果));
        }
        if (statups.containsKey(MapleBuffStat.GHOST_MORPH)) {
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.诱惑)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.诱惑).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.诱惑).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Magnet)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_MagnetArea)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.暗器伤人)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.暗器伤人));
        }
        if (statups.containsKey(MapleBuffStat.不死化)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.不死化).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.不死化).level);
        }
        if (statups.containsKey(MapleBuffStat.ARIANT_COSS_IMU)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.ARIANT_COSS_IMU));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.ARIANT_COSS_IMU));
        }
        if (statups.containsKey(MapleBuffStat.闪光击)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.闪光击));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.闪光击));
        }
        if (statups.containsKey(MapleBuffStat.混乱)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.混乱).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.混乱).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_RespectPImmune)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_RespectPImmune));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_RespectMImmune)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_RespectMImmune));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DefenseAtt)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.ILLUSION)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.ILLUSION));
        }
        if (statups.containsKey(MapleBuffStat.狂暴战魂)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.狂暴战魂));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.狂暴战魂));
        }
        if (statups.containsKey(MapleBuffStat.金刚霸体)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.天使状态)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.天使状态));
            mplew.writeInt(player.getTrueBuffSource(MapleBuffStat.天使状态));
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_472)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.无法使用药水)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.无法使用药水).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.无法使用药水).level);
        }
        if (statups.containsKey(MapleBuffStat.SHADOW)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.SHADOW).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.SHADOW).level);
        }
        if (statups.containsKey(MapleBuffStat.致盲)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.致盲).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.致盲).level);
        }
        if (statups.containsKey(MapleBuffStat.魔法屏障)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.飞翔)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.FREEZE)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.FREEZE).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.FREEZE).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Frozen2)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.龙卷风)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.龙卷风).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.龙卷风).level);
        }
        if (statups.containsKey(MapleBuffStat.撤步退身)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.撤步退身));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.撤步退身));
        }
        if (statups.containsKey(MapleBuffStat.终极斩)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.终极斩));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.终极斩));
        }
        if (statups.containsKey(MapleBuffStat.呼啸_爆击概率)) {
            mplew.write(player.getBuffedIntValue(MapleBuffStat.呼啸_爆击概率));
        }
        if (statups.containsKey(MapleBuffStat.时间胶囊)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.潜入状态)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.ATTACK_BUFF)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.金属机甲)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.金属机甲));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.金属机甲));
        }
        if (statups.containsKey(MapleBuffStat.祝福护甲)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_BlessingArmorIncPAD)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.巨人药水)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.巨人药水));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.巨人药水));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_BuffLimit)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_BuffLimit));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_BuffLimit));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DarkTornado)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_DarkTornado));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_DarkTornado));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_AmplifyDamage)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_AmplifyDamage));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_AmplifyDamage));
        }
        if (statups.containsKey(MapleBuffStat.风影漫步)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.风影漫步));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.风影漫步));
        }
        if (statups.containsKey(MapleBuffStat.神圣魔法盾)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.黑暗变形)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.黑暗变形));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.黑暗变形));
        }
        if (statups.containsKey(MapleBuffStat.精神连接)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.精神连接));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.精神连接));
        }
        if (statups.containsKey(MapleBuffStat.神圣拯救者的祝福)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.神圣拯救者的祝福));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.神圣拯救者的祝福));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Event2)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_Event2));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_Event2));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_DeathMark)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_DeathMark));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_DeathMark));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PainMark)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_PainMark));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_PainMark));
        }
        if (statups.containsKey(MapleBuffStat.死亡束缚)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.死亡束缚).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.死亡束缚).level);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_VampDeath)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_VampDeath));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_VampDeath));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_VampDeathSummon)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_VampDeathSummon));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_VampDeathSummon));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_VenomSnake)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_VenomSnake));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_VenomSnake));
        }
        // SPAWN 1
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PyramidEffect)) {
            mplew.writeInt(-1);
        }
        // SPAWN 2
        if (statups.containsKey(MapleBuffStat.击杀点数)) {
            mplew.write(Math.min(player.getBuffedIntValue(MapleBuffStat.击杀点数), 5));
        }
        // SPAWN 8
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_PinkbeanRollingGrade)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.百分比无视防御)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.百分比无视防御));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.百分比无视防御));
        }
        if (statups.containsKey(MapleBuffStat.幻影屏障)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.幻影屏障));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.幻影屏障));
        }
        if (statups.containsKey(MapleBuffStat.卡牌审判)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.卡牌审判));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.卡牌审判));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_KeyDownAreaMoving)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_KeyDownAreaMoving));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_KeyDownAreaMoving));
        }
        if (statups.containsKey(MapleBuffStat.黑暗高潮)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.黑暗高潮));
        }
        if (statups.containsKey(MapleBuffStat.黑暗祝福)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.黑暗祝福));
        }
        if (statups.containsKey(MapleBuffStat.光暗转换)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.光暗转换));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.光暗转换));
        }
        if (statups.containsKey(MapleBuffStat.模式转换)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.模式转换));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.模式转换));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_SpecialAction)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_SpecialAction));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_SpecialAction));
        }
        if (statups.containsKey(MapleBuffStat.剑刃之壁)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.剑刃之壁));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.剑刃之壁));
        }
        if (statups.containsKey(MapleBuffStat.灵魂凝视)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.灵魂凝视));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.灵魂凝视));
        }
        if (statups.containsKey(MapleBuffStat.伤害置换)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.伤害置换));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.伤害置换));
        }
        if (statups.containsKey(MapleBuffStat.天使亲和)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.天使亲和));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.天使亲和));
        }
        if (statups.containsKey(MapleBuffStat.灵魂鼓舞)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.灵魂鼓舞));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.灵魂鼓舞));
        }
        if (statups.containsKey(MapleBuffStat.隐藏碎片)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.隐藏碎片));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.隐藏碎片));
        }
        if (statups.containsKey(MapleBuffStat.变形值)) {
            mplew.writeShort(player.getMorphCount());
            mplew.writeInt(player.getBuffSource(MapleBuffStat.变形值));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_MobZoneState)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_MobZoneState));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_MobZoneState));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_GiveMeHeal)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_GiveMeHeal));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_GiveMeHeal));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_TouchMe)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_TouchMe));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_TouchMe));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Contagion)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_Contagion));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_Contagion));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Contagion)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.连击无限制)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.连击无限制));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.连击无限制));
        }
        if (statups.containsKey(MapleBuffStat.至圣领域)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.至圣领域));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.至圣领域));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_IgnoreAllCounter)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_IgnoreAllCounter));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IgnoreAllCounter));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_IgnorePImmune)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_IgnorePImmune));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IgnorePImmune));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_IgnoreAllImmune)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_IgnoreAllImmune));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IgnoreAllImmune));
        }
        if (statups.containsKey(MapleBuffStat.最终审判)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.最终审判));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.最终审判));
        }
        if (statups.containsKey(MapleBuffStat.抗震防御)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.抗震防御));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.抗震防御));
        }
        if (statups.containsKey(MapleBuffStat.寒冰灵气)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.寒冰灵气));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.寒冰灵气));
        }
        if (statups.containsKey(MapleBuffStat.火焰灵气)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.火焰灵气));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.火焰灵气));
        }
        if (statups.containsKey(MapleBuffStat.天使复仇)) {
            // empty if block
        }
        if (statups.containsKey(MapleBuffStat.天堂之门)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.天堂之门));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.天堂之门));
        }
        if (statups.containsKey(MapleBuffStat.伤害吸收)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.伤害吸收));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.伤害吸收));
        }
        if (statups.containsKey(MapleBuffStat.神圣保护)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.神圣保护));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.神圣保护));
        }
        if (statups.containsKey(MapleBuffStat.无敌状态)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.无敌状态));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.无敌状态));
        }
        if (statups.containsKey(MapleBuffStat.流血剧毒)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.流血剧毒));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.流血剧毒));
        }
        if (statups.containsKey(MapleBuffStat.隐形剑)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.隐形剑));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.隐形剑));
        }
        if (statups.containsKey(MapleBuffStat.不倦神酒)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.不倦神酒));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.不倦神酒));
        }
        if (statups.containsKey(MapleBuffStat.阿修罗)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.阿修罗));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.阿修罗));
        }
        if (statups.containsKey(MapleBuffStat.超能光束炮)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.超能光束炮));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.超能光束炮));
        }
        if (statups.containsKey(MapleBuffStat.混元归一)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.混元归一));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.混元归一));
        }
        if (statups.containsKey(MapleBuffStat.受到伤害减少百分比)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.受到伤害减少百分比));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.受到伤害减少百分比));
        }
        if (statups.containsKey(MapleBuffStat.返回原位置)) {
            mplew.write(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.返回原位置).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.返回原位置).level);
        }
        if (statups.containsKey(MapleBuffStat.精灵的帽子)) {
            mplew.writeShort(1);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.精灵帽子).skillid);
            mplew.writeShort(player.getDiseasesValue(MapleDisease.精灵帽子).level);
        }
        if (statups.containsKey(MapleBuffStat.恶魔超越)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.恶魔超越));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.恶魔超越));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_FireBomb)) {
            mplew.write(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_FireBomb));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_FireBomb));
        }
        if (statups.containsKey(MapleBuffStat.尖兵电力)) {
            mplew.write(player.getSpecialStat().getPowerCount());
        }
        if (statups.containsKey(MapleBuffStat.飞行骑乘)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.飞行骑乘));
            mplew.writeInt(player.getTrueBuffSource(MapleBuffStat.飞行骑乘));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_NaviFlying)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.永动引擎)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.永动引擎));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.永动引擎));
        }
        if (statups.containsKey(MapleBuffStat.元素属性)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.元素属性));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.元素属性));
        }
        if (statups.containsKey(MapleBuffStat.开天辟地)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.开天辟地));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.开天辟地));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_EventPointAbsorb)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_EventPointAbsorb));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_EventPointAbsorb));
        }
        if (statups.containsKey(MapleBuffStat.ECONDARY_STAT_EventAssemble)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.ECONDARY_STAT_EventAssemble));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.ECONDARY_STAT_EventAssemble));
        }
        if (statups.containsKey(MapleBuffStat.信天翁)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.属性攻击));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.属性攻击));
        }
        if (statups.containsKey(MapleBuffStat.神之子透明)) {
            mplew.writeShort(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.月光转换)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.月光转换));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.月光转换));
        }
        if (statups.containsKey(MapleBuffStat.灵魂武器)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.灵魂武器));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.灵魂武器));
        }
        if (statups.containsKey(MapleBuffStat.元素灵魂_僵直)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.元素灵魂_僵直));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.元素灵魂_僵直));
        }
        if (statups.containsKey(MapleBuffStat.日月轮转)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.日月轮转));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.日月轮转));
        }
        if (statups.containsKey(MapleBuffStat.重生契约)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.重生契约));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.重生契约));
        }
        if (statups.containsKey(MapleBuffStat.灵魂助力)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.灵魂助力));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.灵魂助力));
        }
        if (statups.containsKey(MapleBuffStat.三彩箭矢)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.三彩箭矢));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.三彩箭矢));
        }
        if (statups.containsKey(MapleBuffStat.防甲穿透)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.防甲穿透));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.防甲穿透));
        }
        if (statups.containsKey(MapleBuffStat.灵气大融合)) {
            // empty if block
        }
        // SPAWN 3
        if (statups.containsKey(MapleBuffStat.圣洁之力)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.圣洁之力));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.圣洁之力));
        }
        // SPAWN 4
        if (statups.containsKey(MapleBuffStat.神圣迅捷)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.神圣迅捷));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.神圣迅捷));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_ImmuneBarrier)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_ImmuneBarrier)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_FullSoulMP)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_FullSoulMP));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_FullSoulMP));
        }
        if (statups.containsKey(MapleBuffStat.神圣保护)) {
            mplew.writeBool(player.getBuffSource(MapleBuffStat.神圣保护) == 主教.神圣保护);
        }
        if (statups.containsKey(MapleBuffStat.地雷)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.招魂结界)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.招魂结界));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.招魂结界));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_ComboTempest)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_ComboTempest));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_ComboTempest));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_HalfstatByDebuff)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_HalfstatByDebuff));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_HalfstatByDebuff));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_ComplusionSlant)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_ComplusionSlant));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_ComplusionSlant));
        }
        if (statups.containsKey(MapleBuffStat.召唤美洲豹)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.召唤美洲豹));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.召唤美洲豹));
        }
        // SPAWN 5
        if (statups.containsKey(MapleBuffStat.战法灵气)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.战法灵气));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.战法灵气));
        }
        if (statups.containsKey(MapleBuffStat.超人变形)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.超人变形));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.超人变形));
        }
        if (statups.containsKey(MapleBuffStat.爱星能量)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.爱星能量));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.爱星能量));
        }
        if (statups.containsKey(MapleBuffStat.心雷合一)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.心雷合一));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.心雷合一));
        }
        if (statups.containsKey(MapleBuffStat.子弹盛宴)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.子弹盛宴));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.子弹盛宴));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK466)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_UNK466));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_UNK466));
        }
        if (statups.containsKey(MapleBuffStat.变换攻击)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.变换攻击));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.变换攻击));
        }
        if (statups.containsKey(MapleBuffStat.祈祷)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.祈祷));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.祈祷));
        }
        if (statups.containsKey(MapleBuffStat.黑暗闪电)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.黑暗闪电));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.黑暗闪电));
        }
        if (statups.containsKey(MapleBuffStat.战斗大师)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.战斗大师));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.战斗大师));
        }
        if (statups.containsKey(MapleBuffStat.花炎结界)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.花炎结界));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.花炎结界));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_KeyDownMoving)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_KeyDownMoving));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_KeyDownMoving));
        }
        if (statups.containsKey(MapleBuffStat.灵魂链接)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.灵魂链接));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.灵魂链接));
        }
        if (statups.containsKey(MapleBuffStat.心魂本能)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.心魂本能));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.心魂本能));
        }
        if (statups.containsKey(MapleBuffStat.不消耗HP)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.不消耗HP));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.不消耗HP));
        }
        if (statups.containsKey(MapleBuffStat.不消耗HP)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.不消耗HP));
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_437)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.UNK_MBS_437));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.UNK_MBS_437));
        }
        // SPAWN 9
        if (statups.containsKey(MapleBuffStat.激素狂飙)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.激素狂飙));
        }
        // SPAWN 10
        if (statups.containsKey(MapleBuffStat.忍耐之盾)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_450)) {
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.向导之箭)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.向导之箭));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.向导之箭));
        }
        if (statups.containsKey(MapleBuffStat.精灵元素)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.精灵元素));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.精灵元素));
        }
        if (statups.containsKey(MapleBuffStat.戴米安刻印)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.戴米安刻印));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.戴米安刻印));
        }
        if (statups.containsKey(MapleBuffStat.神圣归一)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.神圣归一));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.神圣归一));
        }
        if (statups.containsKey(MapleBuffStat.神奇圆环)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.神奇圆环));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.神奇圆环));
        }
        // SPAWN 11
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK476)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_UNK476));
        }
        if (statups.containsKey(MapleBuffStat.装备摩诃)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.装备摩诃));
        }
        if (statups.containsKey(MapleBuffStat.超压魔力)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.超压魔力));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_CursorSniping)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_CursorSniping));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_CursorSniping));
        }
        if (statups.containsKey(MapleBuffStat.恶魔狂怒)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.恶魔狂怒));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.恶魔狂怒));
        }
        if (statups.containsKey(MapleBuffStat.守护模式变更)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.守护模式变更));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.守护模式变更));
        }
        if (statups.containsKey(MapleBuffStat.舞力全开)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.舞力全开));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.舞力全开));
        }
        if (statups.containsKey(MapleBuffStat.IDA_SPECIAL_BUFF5)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.IDA_SPECIAL_BUFF5));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.IDA_SPECIAL_BUFF5));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK497)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_UNK497));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_UNK497));
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_512)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.UNK_MBS_512));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.UNK_MBS_512));
        }
        if (statups.containsKey(MapleBuffStat.拔刀姿势)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.拔刀姿势));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.拔刀姿势));
        }
        if (statups.containsKey(MapleBuffStat.拔刀姿势)) {
            mplew.writeInt(-40011288);
        }
        if (statups.containsKey(MapleBuffStat.HAKU_BLESS)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.HAKU_BLESS));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.HAKU_BLESS));
        }
        if (statups.containsKey(MapleBuffStat.拔刀术加成)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.拔刀术加成));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.拔刀术加成));
        }
        if (statups.containsKey(MapleBuffStat.增加攻击力)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.增加攻击力));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.增加攻击力));
        }
        if (statups.containsKey(MapleBuffStat.增加HP百分比)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.增加HP百分比));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.增加HP百分比));
        }
        if (statups.containsKey(MapleBuffStat.增加MP百分比)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.增加MP百分比));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.增加MP百分比));
        }
        if (statups.containsKey(MapleBuffStat.一闪)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.一闪));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.一闪));
        }
        if (statups.containsKey(MapleBuffStat.花炎结界)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.花炎结界));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.花炎结界));
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_512)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.UNK_MBS_512));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.UNK_MBS_512));
        }
        if (statups.containsKey(MapleBuffStat.稳如泰山)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.稳如泰山));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.稳如泰山));
        }
        if (statups.containsKey(MapleBuffStat.UNK_MBS_437)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.UNK_MBS_437));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.UNK_MBS_437));
        }
        if (statups.containsKey(MapleBuffStat.结界破魔)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.结界破魔));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.结界破魔));
        }
        if (statups.containsKey(MapleBuffStat.厚积薄发)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.厚积薄发));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.厚积薄发));
        }
        if (statups.containsKey(MapleBuffStat.晓月流基本技能)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.晓月流基本技能));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.晓月流基本技能));
        }
        if (statups.containsKey(MapleBuffStat.水枪阵营)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.水枪阵营));
        }
        if (statups.containsKey(MapleBuffStat.水枪军阶)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.水枪军阶));
        }
        if (statups.containsKey(MapleBuffStat.水枪效果)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.水枪效果));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.水枪效果));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK520)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_UNK520));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK521)) {
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_UNK521));
        }
        if (statups.containsKey(MapleBuffStat.月光转换)) {
            mplew.write(player.getBuffedValue(MapleBuffStat.月光转换) != null ? 1 : 0);
        }
        mplew.write(0);
        mplew.write(0);
        mplew.write(JobConstants.is魂骑士(player.getJob()) ? 5 : 0);
        if (statups.containsKey(MapleBuffStat.圣洁之力)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.神圣迅捷)) {
            mplew.write(0);
        }
        if (statups.containsKey(MapleBuffStat.战法灵气)) {
            mplew.write(player.getBuffedValue(MapleBuffStat.战法灵气) != null ? 1 : 0);
        }
        // SPAWN 6
        if (statups.containsKey(MapleBuffStat.结合灵气)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        // SPAWN 7
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_BattlePvP_LangE_Protection)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.灵魂链接)) {
            mplew.writeInt(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId == player.getId() ? statups.get(MapleBuffStat.灵魂链接) : 0);
            mplew.writeBool(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId == player.getId() && statups.get(MapleBuffStat.灵魂链接) <= 1);
            mplew.writeInt(player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).fromChrId);
            mplew.writeInt(statups.get(MapleBuffStat.灵魂链接) > 1 ? (int) player.getBuffStatValueHolder(MapleBuffStat.灵魂链接).effect.getLevel() : 0);
        }
        if (statups.containsKey(MapleBuffStat.激素狂飙)) {
            mplew.write(1);
        }
        if (statups.containsKey(MapleBuffStat.戴米安刻印)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.戴米安刻印));
        }
        if (statups.containsKey(MapleBuffStat.神圣归一)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.神圣归一));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK403)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_UNK403));
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK404)) {
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_UNK404));
        }
        if (statups.containsKey(MapleBuffStat.神奇圆环)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.神奇圆环));
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_VampDeath)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_VampDeath));
        }
        PacketHelper.write剑刃之壁(mplew, player, player.getBuffSource(MapleBuffStat.剑刃之壁));
        // SPAWN 12
        int n4 = player.getBuffSource(MapleBuffStat.能量获得);
        mplew.writeInt(player.getSpecialStat().isEnergyFull() ? n4 : 0);
        n3 = isChrinfo ? Randomizer.nextInt() : 1;
        if (statups.containsKey(MapleBuffStat.能量获得)) {
            mplew.writeLong(Math.min(player.getEnergyCount(), 10000));
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 13
        if (statups.containsKey(MapleBuffStat.疾驰速度)) {
            mplew.writeZeroBytes(8);
            mplew.write(1);
            mplew.writeInt(n3);
            mplew.writeShort(0);
        }
        // SPAWN 14
        if (statups.containsKey(MapleBuffStat.疾驰跳跃)) {
            mplew.writeZeroBytes(8);
            mplew.write(1);
            mplew.writeInt(n3);
            mplew.writeShort(0);
        }
        // SPAWN 15
        if (statups.containsKey(MapleBuffStat.骑兽技能)) {
            sourceid = player.getBuffSource(MapleBuffStat.骑兽技能);
            if (sourceid > 0) {
                MaplePacketCreator.addMountId(mplew, player, sourceid);
                mplew.writeInt(sourceid);
            } else {
                mplew.writeLong(0);
            }
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 16
        if (statups.containsKey(MapleBuffStat.极速领域)) {
            mplew.writeZeroBytes(8);
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 17
        if (statups.containsKey(MapleBuffStat.导航辅助)) {
            mplew.write(1);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeZeroBytes(10);
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 18
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_Undead)) {
            mplew.writeZeroBytes(16);
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 19
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_RideVehicleExpire)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_RideVehicleExpire));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_RideVehicleExpire));
            mplew.writeShort(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_RideVehicleExpire) > 0 ? 10 : 0);
            mplew.write(1);
            mplew.writeInt(n3);
        }
        // SPAWN 20
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_COUNT_PLUS1)) {
            mplew.writeInt(player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_COUNT_PLUS1));
            mplew.writeInt(player.getBuffSource(MapleBuffStat.SECONDARY_STAT_COUNT_PLUS1));
            mplew.writeShort(0);
            mplew.write(1);
            mplew.writeInt(n3);
        }
        if (statups.containsKey(MapleBuffStat.SECONDARY_STAT_UNK49)) {
            mplew.writeInt(0);
            mplew.writeShort(0);
        }
        if (statups.containsKey(MapleBuffStat.飞行骑乘)) {
            sourceid = player.getBuffSource(MapleBuffStat.骑兽技能);
            if (sourceid > 0) {
                MaplePacketCreator.addMountId(mplew, player, sourceid);
            } else {
                mplew.writeInt(0);
            }
        }
    }

    /*
     * 取消BUFF状态
     */
    public static byte[] cancelBuff(List<MapleBuffStat> statups, MapleCharacter chr) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        writeMask(mplew, statups);
        for (MapleBuffStat mask : statups) {
            if (mask.canStack()) {
                List<SpecialBuffInfo> buffs = chr.getSpecialBuffInfo(mask);
                mplew.writeInt(buffs.size()); //这个地方是有多少个重复的特殊BUFF 是1个循环 取消后还剩余几个这个效果 还要继续写剩余效果的属性
                for (SpecialBuffInfo info : buffs) {
                    mplew.writeInt(info.buffid);
                    mplew.writeLong(info.value);
                    mplew.writeInt(0); //V.114新增
                    mplew.writeInt(info.bufflength);
                    mplew.writeInt(0);
                }
            }
        }
        if (statups.contains(MapleBuffStat.变身效果) || statups.contains(MapleBuffStat.移动速度) || statups.contains(MapleBuffStat.跳跃力) || statups.contains(MapleBuffStat.增加跳跃力) || statups.contains(MapleBuffStat.增加移动速度) || statups.contains(MapleBuffStat.冒险岛勇士) || statups.contains(MapleBuffStat.金属机甲) || statups.contains(MapleBuffStat.战法灵气) || statups.contains(MapleBuffStat.变形值) || statups.contains(MapleBuffStat.能量获得) || statups.contains(MapleBuffStat.疾驰速度) || statups.contains(MapleBuffStat.疾驰跳跃) || statups.contains(MapleBuffStat.飞行骑乘)) {
            mplew.write(0x03);
        } else if (statups.contains(MapleBuffStat.骑兽技能)) {
            mplew.write(0x03);
            mplew.write(0x01);
        } else if (statups.contains(MapleBuffStat.月光转换)) {
            mplew.write(0x00);
        } else if (statups.contains(MapleBuffStat.飞翔)) {
            mplew.write(0x06);
        }

        return mplew.getPacket();
    }

    /*
     * 取消BUFF状态
     */
    public static byte[] cancelBuff(MapleBuffStat buffstat) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        writeSingleMask(mplew, buffstat);
        if (buffstat.canStack()) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    /*
     * 取消负面BUFF状态
     */
    public static byte[] cancelDebuff(MapleDisease mask) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_BUFF.getValue());
        writeSingleMask(mplew, mask);
        mplew.write(3);
        mplew.write(0);
        mplew.write(1);

        return mplew.getPacket();
    }

    //召唤炮台
    public static byte[] spawnArrowsTurret(MapleArrowsTurret summon) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_ARROWS_TURRET.getValue());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(1);
        mplew.writeInt(summon.getOwnerId());
        mplew.writeInt(0);
        mplew.writeInt(summon.getPosition().x);
        mplew.writeInt(summon.getPosition().y);
        mplew.write(summon.getSide());

        return mplew.getPacket();
    }

    public static byte[] isArrowsTurretAction(MapleArrowsTurret summon, boolean attack) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(attack ? SendPacketOpcode.ARROWS_TURRET_ACTION.getValue() : SendPacketOpcode.SPAWN_ARROWS_TURRET.getValue());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(attack ? 0 : 1);
        if (!attack) {
            mplew.writeInt(summon.getOwnerId());
            mplew.writeInt(0);
            mplew.writeInt(summon.getPosition().x);
            mplew.writeInt(summon.getPosition().y);
            mplew.write(summon.getSide());
        }

        return mplew.getPacket();
    }

    //炮台开始攻击
    public static byte[] ArrowsTurretAction(MapleArrowsTurret summon) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ARROWS_TURRET_ACTION.getValue());
        mplew.writeInt(summon.getObjectId());
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    //删除炮台
    public static byte[] cancelArrowsTurret(MapleArrowsTurret summon) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_ARROWS_TURRET.getValue());
        mplew.writeInt(1);
        mplew.writeInt(summon.getObjectId());

        return mplew.getPacket();
    }

    //箭矢炮盘效果(炮台状态)
    public static byte[] CannonPlateEffectFort(MapleCharacter from, int skillId, Point pos, int nuk, short dir, int Temporary, int Temporary1, int Temporary2, int DirPos1, int DirPos2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANNON_PLATE.getValue());
        mplew.writeShort(4);
        mplew.writeInt(from.getId());
        mplew.writeInt(from.getMapId());
        mplew.write(1);
        mplew.writeShort(0);
        mplew.writePos(pos);//位置
        mplew.writeInt(nuk);//技能时间?
        mplew.writeShort(Temporary);
        mplew.writeShort(Temporary1);
        mplew.writeShort(Temporary2);//
        mplew.writeInt(skillId);//技能ID
        mplew.write((byte) dir);//炮台的面向
        mplew.writeInt((dir == 1) ? -DirPos1 : DirPos1);//射箭的方向,X
        mplew.writeInt(DirPos2);//射箭的方向,Y

        return mplew.getPacket();
    }

    public static byte[] switchLuckyMoney(boolean on) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SWITCH_LUCKYMONEY.getValue());
        mplew.writeBool(on);
        return mplew.getPacket();
    }

    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = 0; i < GameConstants.MAX_BUFFSTAT; i++) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    public static <E extends Buffstat> void writeMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups) {
            mask[statup.getPosition()] |= statup.getValue();
        }
        for (int aMask : mask) {
            mplew.writeInt(aMask);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Collection<Pair<E, Integer>> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (Pair<E, Integer> statup : statups) {
            mask[statup.left.getPosition()] |= statup.left.getValue();
        }
        for (int aMask : mask) {
            mplew.writeInt(aMask);
        }
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition()] |= statup.getValue();
        }
        for (int aMask : mask) {
            mplew.writeInt(aMask);
        }
    }

    public static byte[] showFirewall(int oid, int skillid, Rectangle rectangle, int unk_value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FIREWALL.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(skillid);
        mplew.writeInt(50 * Randomizer.rand(7, 9));
        mplew.writeInt(rectangle.x);
        mplew.writeInt(-rectangle.y);
        mplew.writeInt(rectangle.x + rectangle.width);
        mplew.writeInt(rectangle.y + rectangle.height - 22);

        return mplew.getPacket();
    }

    public static byte[] giveGodBuff(int sourceid, int bufflength) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeSingleMask(mplew, MapleBuffStat.无敌状态);
        mplew.writeShort(1);
        mplew.writeInt(sourceid);
        mplew.writeInt(bufflength);
        mplew.writeZeroBytes(18);

        return mplew.getPacket();
    }
}
