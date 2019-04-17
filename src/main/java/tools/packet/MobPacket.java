package tools.packet;

import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import client.status.NewMonsterStatusEffect;
import handling.Buffstat;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.maps.MapleMap;
import server.maps.MapleNodes;
import server.maps.MapleSwordNode;
import server.movement.LifeMovementFragment;
import tools.DateUtil;
import tools.HexTool;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;


/***
 * 负责生成怪物Buff相关的数据包
 * @author dongjak
 *
 */
public class MobPacket {

    private static final Logger log = LogManager.getLogger(MobPacket.class);

    /**
     * 怪物伤害数字显示
     */
    public static byte[] damageMonster(int oid, long damage) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        if (damage > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) damage);
        }

        return mplew.getPacket();
    }

    /**
     * 友好的怪物伤害数字显示
     */
    public static byte[] damageFriendlyMob(MapleMonster mob, long damage, boolean display) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.write(display ? 1 : 2); //false for when shammos changes map!
        if (damage > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) damage);
        }
        if (mob.getHp() > Integer.MAX_VALUE) {
            mplew.writeInt((int) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        } else {
            mplew.writeInt((int) mob.getHp());
        }
        if (mob.getMobMaxHp() > Integer.MAX_VALUE) {
            mplew.writeInt(Integer.MAX_VALUE);
        } else {
            mplew.writeInt((int) mob.getMobMaxHp());
        }

        return mplew.getPacket();
    }

    /**
     * 杀死怪物
     */
    public static byte[] killMonster(int oid, int animation) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(animation); // 0 = dissapear, 1 = fade out, 2+ = special
        if (animation == 4) {
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    /**
     * 吞噬怪物?
     */
    public static byte[] suckMonster(int oid, int chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KILL_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(4);
        mplew.writeInt(chr);

        return mplew.getPacket();
    }

    /**
     * 怪物加血
     */
    public static byte[] healMonster(int oid, int heal) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.write(0);
        mplew.writeInt(-heal);

        return mplew.getPacket();
    }

    /**
     * 显示怪物血量
     */
    public static byte[] showMonsterHP(int oid, int remhppercentage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MONSTER_HP.getValue());
        mplew.writeInt(oid);
        mplew.write(remhppercentage);

        return mplew.getPacket();
    }

    /**
     * 显示BOSS血条
     */
    public static byte[] showBossHP(MapleMonster mob) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x06); //V.107修改 以前0x05
        mplew.writeInt(mob.getId() == 9400589 ? 9300184 : mob.getId());
        mplew.writeLong((long) (((double) mob.getHp() / mob.getMobMaxHp()) * Integer.MAX_VALUE));
        mplew.writeLong(Integer.MAX_VALUE);
        mplew.write(mob.getStats().getTagColor());
        mplew.write(mob.getStats().getTagBgColor());

        return mplew.getPacket();
    }

    /**
     * 显示BOSS血条
     * 怪物ID
     * 怪物当前血量
     * 怪物的总血量
     */
    public static byte[] showBossHP(int monsterId, long currentHp, long maxHp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(0x06); //V.107修改 以前0x05
        mplew.writeInt(monsterId); //has no image
        mplew.writeLong((long) (((double) currentHp / maxHp) * Integer.MAX_VALUE));
        mplew.writeLong(Integer.MAX_VALUE);
        mplew.write(6);
        mplew.write(5);

        //colour legend: (applies to both colours)
        //1 = red, 2 = dark blue, 3 = light green, 4 = dark green, 5 = black, 6 = light blue, 7 = purple
        return mplew.getPacket();
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid  The ObjectID of the monster being moved.
     * @param moveid    The movement ID.
     * @param currentMp The current MP of the monster.
     * @param useSkills Can the monster use skills?
     * @return The move response packet.
     */
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills) {
        return moveMonsterResponse(objectid, moveid, currentMp, useSkills, 0, 0);
    }

    /**
     * Gets a response to a move monster packet.
     *
     * @param objectid   The ObjectID of the monster being moved.
     * @param moveid     The movement ID.
     * @param currentMp  The current MP of the monster.
     * @param useSkills  Can the monster use skills?
     * @param skillId    The skill ID for the monster to use.
     * @param skillLevel The level of the skill to use.
     * @return The move response packet.
     */
    public static byte[] moveMonsterResponse(int objectid, short moveid, int currentMp, boolean useSkills, int skillId, int skillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(19);

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER_RESPONSE.getValue());
        mplew.writeInt(objectid);
        mplew.writeShort(moveid);
        mplew.writeBool(useSkills);
        mplew.writeInt(currentMp);
        mplew.writeInt(skillId);
        mplew.write(skillLevel);
        mplew.writeInt(0); //Randomizer.rand(0, 5)

        return mplew.getPacket();
    }

    public static byte[] moveMonster(boolean useskill, int mode, int skillid, int skilllevel, short effectAfter, int oid, Point startPos, List<LifeMovementFragment> moves, List<Pair<Short, Short>> list1, List<Short> list2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_MONSTER.getValue());
        mplew.writeInt(oid);
        mplew.writeBool(useskill);
        mplew.write(mode);
        mplew.write(skillid);
        mplew.write(skilllevel);
        mplew.writeShort(effectAfter);
        mplew.write(list1 == null ? 0 : list1.size());
        if (list1 != null) {
            list1.forEach(pair -> {
                mplew.writeShort(pair.getLeft());
                mplew.writeShort(pair.getRight());
            });
        }
        mplew.write(list2 == null ? 0 : list2.size());
        if (list2 != null) {
            list2.forEach(mplew::writeShort);
        }
        mplew.writeInt(0);
        mplew.writePos(startPos);
        mplew.writeInt(DateUtil.getTime(System.currentTimeMillis()));
        PacketHelper.serializeMovementList(mplew, moves);
        mplew.write(0);
        return mplew.getPacket();
    }

    /**
     * 刷出怪物
     */
    public static byte[] spawnMonster(MapleMonster life, int spawnType, int link) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER.getValue());
        mplew.write(0);
        mplew.writeInt(life.getObjectId());
        mplew.write(1); // 1 = Control normal, 5 = Control none
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        writeMonsterEndData(mplew, life, true, spawnType, link);

        return mplew.getPacket();
    }

    /**
     * 怪物召唤控制
     */
    public static byte[] controlMonster(MapleMonster life, boolean newSpawn, boolean aggro, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(aggro ? 2 : 1); // 1 = Control normal, 5 = Control none
        mplew.writeInt(life.getObjectId());
        mplew.write(type);
        mplew.writeInt(life.getId());
        addMonsterStatus(mplew, life);
        writeMonsterEndData(mplew, life, newSpawn, life.isFake() ? 1 : 0, life.getLinkOid());

        return mplew.getPacket();
    }

    public static void writeMonsterEndData(MaplePacketLittleEndianWriter mplew, MapleMonster life, boolean newSpawn, int spawnType, int link) {
        mplew.writePos(life.getTruePosition());
        mplew.write(life.getStance()); // Bitfield
        if (life.getStats().getSmartPhase() > 0) {
            mplew.write(0);
        }
        int fh = newSpawn ? life.getFh() : life.getMobFH();
        mplew.writeShort(fh); // FH life.getFh()
        mplew.writeShort(life.getFh()); // Origin FH
        spawnType = newSpawn ? spawnType : life.isFake() ? -4 : -1;
        mplew.writeShort(spawnType); //(-2 新刷出的怪物 -1 已刷出的怪物)
        if (spawnType == -3 || spawnType >= 0) {
            mplew.writeInt(link);
        }
        mplew.write(life.getCarnivalTeam());
        mplew.writeLong(life.getHp() > Integer.MAX_VALUE ? Integer.MAX_VALUE : life.getMobMaxHp());
        mplew.writeZeroBytes(20);
        if (life.getId() / 10000 == 961) {
            mplew.writeShort(0);
        }
        mplew.writeInt(-1);
        mplew.writeInt(newSpawn ? -1 : 0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(100); //  + life.getStats().getScale()
        mplew.writeInt(-1);
//        if (life.is精英怪()) {
//            final int n4 = 1;
//            mplew.writeInt(n4);
//            for (int j = 0; j < n4; ++j) {
//                mplew.writeInt(life.is精英怪类型());
//                mplew.writeInt(0);
//            }
//            mplew.writeInt(1);
//        }
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(0);
        if (life.getId() == 8880102) {
            mplew.writeInt(life.getFollowChrID());
        }
        mplew.write(0);
//        if (life.getStats().isSkeleton()) {
//            mplew.write(life.getStats().getHitParts().size());
//            for (final Entry<String, Integer> entry : life.getStats().getHitParts().entrySet()) {
//                mplew.writeMapleAsciiString(entry.getKey());
//                mplew.write(0);
//                mplew.writeInt(entry.getValue());
//            }
//        }
    }

    /**
     * 怪物自定义属性
     *
     * @param mplew
     * @param life
     */
    public static void addMonsterStatus(MaplePacketLittleEndianWriter mplew, MapleMonster life) {
        if (life.getStati().size() <= 1) {
            life.addEmpty();
        }
        boolean writeChangedStats = life.getStats().isChangeable() && life.getChangedStats() != null;
        mplew.write(writeChangedStats ? 1 : 0);
        if (writeChangedStats) {
            // 13组int
            mplew.writeInt(life.getChangedStats().hp > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) life.getChangedStats().hp);
            mplew.writeInt(life.getChangedStats().mp);
            mplew.writeInt(life.getChangedStats().exp);
            mplew.writeInt(life.getChangedStats().watk);
            mplew.writeInt(life.getChangedStats().matk);
            mplew.writeInt(life.getChangedStats().PDRate);
            mplew.writeInt(life.getChangedStats().MDRate);
            mplew.writeInt(life.getChangedStats().acc);
            mplew.writeInt(life.getChangedStats().eva);
            mplew.writeInt(life.getChangedStats().pushed);
            mplew.writeInt(life.getChangedStats().speed); //V.109.1新增 未知
            mplew.writeInt(life.getChangedStats().level);
            mplew.writeInt(0);
        }
        mplew.write(HexTool.getByteArrayFromHexString("00 00 00 00 00 00 00 60 80 FF 4F 01"));

        int random = Randomizer.nextInt();
        for (int i = 1; i <= 4; i++) {
            mplew.writeLong(0);
            mplew.writeShort(random);
        }
        mplew.writeZeroBytes(119); //V.120.1修改
    }

    /**
     * 停止怪物召唤控制
     *
     * @param oid 怪物oid
     * @return
     */
    public static byte[] stopControllingMonster(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_MONSTER_CONTROL.getValue());
        mplew.write(0);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    /**
     * 怪物移动回应
     */
    public static byte[] makeMonsterReal(MapleMonster life) {
        return spawnMonster(life, -1, 0);
    }

    public static byte[] makeMonsterFake(MapleMonster life) {
        return spawnMonster(life, -4, 0);
    }

    public static byte[] makeMonsterEffect(MapleMonster life, int effect) {
        return spawnMonster(life, effect, 0);
    }

    /**
     * 写入单个状态的MASK
     *
     * @param mplew
     * @param statup
     * @param <E>
     */
    public static <E extends Buffstat> void writeSingleMask(MaplePacketLittleEndianWriter mplew, E statup) {
        for (int i = 1; i <= 3; i++) {
            mplew.writeInt(i == statup.getPosition() ? statup.getValue() : 0);
        }
    }

    /**
     * 写入所有状态的MASK
     *
     * @param mplew
     * @param statups
     * @param <E>
     */
    public static <E extends Buffstat> void writeNewMask(MaplePacketLittleEndianWriter mplew, Collection<E> statups) {
        int[] mask = new int[3];
        for (E statup : statups) {
            mask[statup.getPosition()] |= statup.getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
    }

    /**
     * 写入所有状态的MASK
     *
     * @param mplew
     * @param statups
     */
    public static void writeMask(MaplePacketLittleEndianWriter mplew, Collection<MonsterStatusEffect> statups) {
        int[] mask = new int[3];
        for (MonsterStatusEffect statup : statups) {
            mask[statup.getStati().getPosition()] |= statup.getStati().getValue();
        }
        for (int i = 0; i < mask.length; i++) {
            mplew.writeInt(mask[i]);
        }
    }

    /**
     * 怪物自己添加BUFF状态
     */
//    public static byte[] applyMonsterStatus(int oid, MonsterStatus mse, int x, MobSkill skil, short effectAfter) {
//        log.trace("调用");
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
//        mplew.writeInt(oid);
//        writeSingleMask(mplew, mse);
//        mplew.writeInt(x);
//        mplew.writeShort(skil.getSkillId());
//        mplew.writeShort(skil.getSkillLevel());
//        mplew.writeShort(0); // 好像这个地方是 [28 00] 具体不清楚是什么意思 might actually be the buffTime but it's not displayed anywhere 以前 mse.isEmpty() ? 1 : 0
//        mplew.writeShort(effectAfter); //技能延时显示时间 客户端发来的
//        mplew.write(1);
//
//        return mplew.getPacket();
//    }

    /**
     * 玩家对怪物施放的 DEBUFF
     */
    public static byte[] applyMonsterStatus(MapleMonster mons, MonsterStatusEffect ms) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        writeSingleMonsterStatus(mplew, ms);

        return mplew.getPacket();
    }

    public static byte[] applyMonsterStatus(MapleMonster mons, List<MonsterStatusEffect> mss) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(mons.getObjectId());
        writeMonsterStatusEffectData(mplew, mss);

        return mplew.getPacket();
    }

    public static void writeSingleMonsterStatus(MaplePacketLittleEndianWriter mplew, MonsterStatusEffect ms) {
        List<MonsterStatusEffect> hashSet = new ArrayList<>();
        hashSet.add(ms);
        writeMonsterStatusEffectData(mplew, hashSet);
    }

    public static void writeMonsterStatusEffectData(MaplePacketLittleEndianWriter mplew, List<MonsterStatusEffect> collection) {
        writeStatusEffectData(mplew, collection);
        int count = 1;
        for (MonsterStatusEffect mse : collection) {
            if (mse.getCount() <= 1) continue;
            count = mse.getCount();
            break;
        }
        mplew.writeShort(0);
        mplew.write(count + 1);
        mplew.write(count);
    }

    public static void writeStatusEffectData(MaplePacketLittleEndianWriter mplew, List<MonsterStatusEffect> collection) {
        MonsterStatusEffect ms;
        HashMap<MonsterStatus, MonsterStatusEffect> hashMap = new HashMap<>();
        writeMask(mplew, collection);
        for (MonsterStatusEffect mse : collection) {
            hashMap.put(mse.getStati(), mse);
            if (mse.getStati().getOrValue() < MonsterStatus.MOB_STAT_Burned.getOrValue()) {
                mplew.writeInt(mse.getCount());
                if (mse.getMobSkill() != null) {
                    mplew.writeShort(mse.getMobSkill().getSkillId());
                    mplew.writeShort(mse.getMobSkill().getSkillLevel());
                } else {
                    mplew.writeInt(mse.getSkill() > 0 ? mse.getSkill() : 0);
                }
                mplew.writeShort((short) ((mse.getCancelTask() - System.currentTimeMillis()) / 500));
            }
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_PDR)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_MDR)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_PCounter)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_MCounter)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_PCounter) || hashMap.containsKey(MonsterStatus.MOB_STAT_MCounter)) {
            mplew.writeInt(500);
            mplew.write(1);
            mplew.writeInt(500);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Fatality)) {
            mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_Fatality)).getFromID());
            mplew.writeInt(1000000 + 20000 * ((hashMap.get(MonsterStatus.MOB_STAT_Fatality)).getCount() / 3));
            mplew.writeInt(0);
            mplew.writeInt(2 * ((hashMap.get(MonsterStatus.MOB_STAT_Fatality)).getCount() / 3));
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Explosion)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_AddBuffStat)) {
            int n2 = 0;
            mplew.write(n2);
            if (n2 > 0) {
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
            }
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_DeadlyCharge)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Incizing)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Speed)) {
            mplew.write((hashMap.get(MonsterStatus.MOB_STAT_Speed)).getCount());
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_BMageDebuff)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_DarkLightning)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_BattlePvP_Helena_Mark)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_MultiPMDR)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Freeze)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Burned)) {
            int n2 = (hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCount() > 0 ? 1 : 0;
            int n3 = 1;
            mplew.write(n2);
            if (n2 > 0) {
                do {
                    mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCount());
                    mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getSkill());
                    mplew.writeLong((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCount());
                    mplew.writeInt(1000);
                    mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCount());
                    mplew.writeInt((int) ((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCancelTask() - System.currentTimeMillis()));
                    mplew.writeInt(5);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(999999);
                    mplew.writeInt(0);
                    mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_Burned)).getCount());
                    mplew.writeInt(0);
                } while (++n3 < n2);
            }
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_BalogDisable)) {
            mplew.write(0);
            mplew.write(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_ExchangeAttack)) {
            mplew.write(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_AddDamParty)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_LinkTeam)) {
            mplew.writeMapleAsciiString("");
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_SoulExplosion)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_SeperateSoulP)) {
            ms = hashMap.get(MonsterStatus.MOB_STAT_SeperateSoulP);
            mplew.writeInt(ms != null ? ms.getCount() : 0);
            mplew.writeInt(ms != null ? ms.getMoboid() : 0);
            mplew.writeShort(0);
            mplew.writeInt(ms != null ? ms.getCount() : 0);
            mplew.writeInt(ms != null ? ms.getSkill() : 0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_SeperateSoulC)) {
            ms = hashMap.get(MonsterStatus.MOB_STAT_SeperateSoulC);
            mplew.writeInt(ms != null ? ms.getCount() : 0);
            mplew.writeInt(ms != null ? ms.getMoboid() : 0);
            mplew.writeShort(0);
            mplew.writeInt(ms != null ? ms.getCount() : 0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Ember)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_TrueSight)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_TrueSight)).getCount());
            mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_TrueSight)).getSkill());
            mplew.writeInt((hashMap.get(MonsterStatus.MOB_STAT_TrueSight)).getSkill() > 0 ? (int) System.currentTimeMillis() : 0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_MultiDamSkill)) {
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_Laser)) {
            final boolean b = hashMap.get(MonsterStatus.MOB_STAT_Laser).getCount() != 0;
            final int n4 = (int) ((System.currentTimeMillis() - hashMap.get(MonsterStatus.MOB_STAT_Laser).getStartTime()) / 1100.0 * 10.0 % 360.0);
            mplew.writeInt(b ? (hashMap.get(MonsterStatus.MOB_STAT_Laser).getCount()) : 0);
            mplew.writeShort(b ? 223 : 0);
            mplew.writeShort(b ? hashMap.get(MonsterStatus.MOB_STAT_Laser).getMobSkill().getSkillLevel() : 0);
            mplew.writeInt(b ? DateUtil.getTime() : 0);
            mplew.writeInt((b && !hashMap.get(MonsterStatus.MOB_STAT_Laser).isFirstUse()) ? 1 : 0);
            mplew.writeInt(b ? n4 : 0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_ElementResetBySummon)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        if (hashMap.containsKey(MonsterStatus.MOB_STAT_BahamutLightElemAddDam)) {
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
    }

    public static byte[] applyNewMonsterStatus(MapleMonster monster, List<Pair<MonsterStatus, NewMonsterStatusEffect>> statups) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
        mplew.writeInt(monster.getObjectId());
        writeNewMask(mplew, statups.stream().map(Pair::getLeft).collect(Collectors.toList()));
        for (Pair<MonsterStatus, NewMonsterStatusEffect> statup : statups) {
            mplew.writeInt(statup.getRight().getX());
            mplew.writeInt(statup.getRight().getSkillid());
            mplew.writeShort(0);
        }
        mplew.writeShort(0);
        mplew.writeZeroBytes(statups.size() * 2);
        mplew.writeZeroBytes(5);
        mplew.write(0);
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(MapleMonster monster, MonsterStatusEffect mse) {
        List<MonsterStatusEffect> arrayList = new ArrayList<>();
        arrayList.add(mse);
        return cancelMonsterStatus(monster, arrayList);
    }

    public static byte[] cancelMonsterStatus(MapleMonster monster, List<MonsterStatusEffect> statups) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(monster.getObjectId());
        writeMask(mplew, statups);
        for (MonsterStatusEffect mse : statups) {
            if (mse.getStati() == MonsterStatus.MOB_STAT_Burned) {
                mplew.writeInt(0);
                int n2 = 1;
                mplew.writeInt(n2);
                if (n2 > 0) {
                    do {
                        mplew.writeInt(0);
                        mplew.writeInt(0);
                    } while (--n2 == 0);
                }
            }
        }
        mplew.write(5);
        mplew.write(2);

        return mplew.getPacket();
    }

    public static byte[] cancelMonsterStatus(int oid, List<MonsterStatus> statups) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_STATUS.getValue());
        mplew.writeInt(oid);
        writeNewMask(mplew, statups);
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    /**
     * 玩家给怪物添加中毒BUFF状态
     */
//    public static byte[] applyMonsterPoisonStatus(MapleMonster mons, List<MonsterStatusEffect> mse) {
//        log.trace("调用");
//        if (mse.size() <= 0 || mse.get(0) == null) {
//            return MaplePacketCreator.enableActions();
//        }
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
//        mplew.writeInt(mons.getObjectId());
//        MonsterStatusEffect ms = mse.get(0);
//        if (ms.getStati() == MonsterStatus.MOB_STAT_Poison) { //stack ftw
//            writeSingleMask(mplew, MonsterStatus.MOB_STAT_Poison);
//            mplew.write(1); //mse.size()
//            for (MonsterStatusEffect m : mse) {
//                mplew.writeInt(m.getFromID()); //character ID
//                if (m.isMonsterSkill()) {
//                    mplew.writeShort(m.getMobSkill().getSkillId());
//                    mplew.writeShort(m.getMobSkill().getSkillLevel());
//                } else if (m.getSkill() > 0) {
//                    mplew.writeInt(m.getSkill());
//                    //System.out.println("怪物中毒 - 技能: " + m.getSkill() + " 掉血伤害: " + m.getX() + " 持续时间: " + m.getDotTime() + " 毫秒 转为: " + m.getDotTime() / 1000 + " 秒");
//                }
//                mplew.writeInt(m.getX()); //掉血的伤害
//                mplew.writeInt(1000); //掉血的时间间隔
//                mplew.writeInt(0); // tick count
//                mplew.writeInt(10000); //V.112新增 未知
//                mplew.writeInt((int) (m.getDotTime() / 1000)); //中毒持续时间 单位为秒计算
//                mplew.writeInt(0); //是第次叠加 第1次为 0 第2次为 1 第3次为 2
//                mplew.write(new byte[12]);
//                mplew.writeInt(99999); //最大伤害
//                mplew.writeInt(0);
//                mplew.writeInt(m.getX());
//            }
//            //mplew.writeShort(1000); // delay in ms
//            mplew.writeShort(0);
//            mplew.write(1); // size
//        } else {
//            writeSingleMask(mplew, ms.getStati());
//            mplew.writeInt(ms.getX());
//            if (ms.isMonsterSkill()) {
//                mplew.writeShort(ms.getMobSkill().getSkillId());
//                mplew.writeShort(ms.getMobSkill().getSkillLevel());
//            } else if (ms.getSkill() > 0) {
//                mplew.writeInt(ms.getSkill());
//            }
//            mplew.writeShort(0); // might actually be the buffTime but it's not displayed anywhere
//            mplew.writeShort(0); // delay in ms
//            mplew.writeShort(0);
//            mplew.write(0);
//            mplew.write(1); // size
//            mplew.write(1); // ? v97
//        }
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] applyMonsterStatus(int oid, MonsterStatus mse, int x, MobSkill skil) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
//        mplew.writeInt(oid);
//        writeSingleMask(mplew, mse);
//
//        mplew.writeInt(x);
//        mplew.writeShort(skil.getSkillId());
//        mplew.writeShort(skil.getSkillLevel());
//        mplew.writeShort(mse.isEmpty() ? 1 : 0);
//
//        mplew.writeShort(0);
//        mplew.write(2);// was 1
//        mplew.writeZeroBytes(30);
//
//        return mplew.getPacket();
//    }

    /**
     * 怪物自己添加BUFF状态，物理.魔法反射什么的状态的效果
     */
//    public static byte[] applyMonsterStatus(int oid, Map<MonsterStatus, Integer> stati, List<Integer> reflection, MobSkill skil) {
//        log.trace("调用");
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
//        mplew.writeInt(oid);
//        writeMask(mplew, stati.keySet());
//
//        for (Entry<MonsterStatus, Integer> mse : stati.entrySet()) {
//            mplew.writeInt(mse.getValue());
//            mplew.writeShort(skil.getSkillId());
//            mplew.writeShort(skil.getSkillLevel());
//            mplew.writeShort(0); // might actually be the buffTime but it's not displayed anywhere
//        }
//        for (Integer ref : reflection) {
//            mplew.writeInt(ref);
//        }
//        mplew.writeLong(0);
//        mplew.writeShort(0); // delay in ms
//
//        int size = stati.size(); // size
//        if (reflection.size() > 0) {
//            size /= 2; // This gives 2 buffs per reflection but it's really one buff
//        }
//        mplew.write(size); // size
//
//        return mplew.getPacket();
//    }

//    public static byte[] displayNode(MapleMonster monster, MapleCharacter player) {
//        log.trace("调用");
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.APPLY_MONSTER_STATUS.getValue());
//        mplew.writeInt(monster.getObjectId());
//
//        return mplew.getPacket();
//    }
//
//    public static void a(d d2, o o2) {
//        HashSet<o> hashSet = new HashSet<o>();
//        hashSet.add(o2);
//        n.c(d2, hashSet);
//    }
//
//    public static void c(d d2, Collection collection) {
//        n.b(d2, collection);
//        int n2 = 1;
//        for (o o2 : collection) {
//            if (o2.eL() <= 1) continue;
//            n2 = o2.eL();
//            break;
//        }
//        d2.writeShort(0);
//        d2.write(n2 + 1);
//        d2.write(n2);
//    }

    /**
     * 显示操作结果
     */
    public static byte[] showResults(int mobid, boolean success) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_RESULTS.getValue());
        mplew.writeInt(mobid);
        mplew.writeBool(success);
        mplew.write(1);

        return mplew.getPacket();
    }

    /**
     * 扑捉怪物
     */
    public static byte[] catchMonster(int mobid, int itemid, byte success) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CATCH_MONSTER.getValue());
        mplew.writeInt(mobid);
        mplew.writeInt(itemid);
        mplew.write(success);

        return mplew.getPacket();
    }

    public static byte[] unknown(int moboid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(0x3C9);
        mplew.writeLong(moboid);

        return mplew.getPacket();
    }

    public static byte[] showMobSkillDelay(int moboid, MobSkill mobSkill, int effectAfter, List<Rectangle> rectangles) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_SKILL_DELAY.getValue());
        mplew.writeInt(moboid);
        mplew.writeInt(effectAfter);
        mplew.writeInt(mobSkill.getSkillId());
        mplew.writeInt(mobSkill.getSkillLevel());
        mplew.writeInt(mobSkill.getAreaSequenceDelay());
        mplew.writeInt(rectangles.size());
        rectangles.forEach(mplew::writeRect);

        return mplew.getPacket();
    }
//
//    public static byte[] unknown1(int moboid) {
//        if (ServerConstants.isShowPacket()) {
//            log.trace("调用");
//        }
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(0x1CC);
//        mplew.writeInt(moboid);
//        mplew.writeInt(1);
//        mplew.write(0);
//
//        return mplew.getPacket();
//    }

    public static byte[] showMonsterSpecialSkill(int moboid, int type) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_SPECIAL_SKILL.getValue());
        mplew.writeInt(moboid);
        mplew.writeInt(type);

        return mplew.getPacket();
    }

    public static byte[] mobEscortReturnBefore(MapleMonster mob, MapleMap map) {
        if (mob.getNodePacket() != null) {
            return mob.getNodePacket();
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ESCORT_RETURN_BEFORE.getValue());
        mplew.writeInt(mob.getObjectId());
        mplew.writeInt(map.getNodes().size());
        mplew.writeInt(mob.getPosition().x);
        mplew.writeInt(mob.getPosition().y);
        for (MapleNodes.MapleNodeInfo nodeInfo : map.getNodes()) {
            mplew.writeInt(nodeInfo.x);
            mplew.writeInt(nodeInfo.y);
            mplew.writeInt(nodeInfo.attr);
            if (nodeInfo.attr != 2) continue;
            mplew.writeInt(500);
        }
        mplew.writeZeroBytes(6);
        mob.setNodePacket(mplew.getPacket());
        return mob.getNodePacket();
    }

    public static byte[] mobEscortStopEndPermmision(int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ESCORT_RETURN_BEFORE.getValue());
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] getBossHatred(final Map<String, Integer> aggroRank) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_HATRED.getValue());
        final ArrayList<Entry<String, Integer>> list = new ArrayList<>(aggroRank.entrySet());
        list.sort((entry, entry2) -> entry2.getValue() - entry.getValue());
        mplew.writeInt(list.size());
        list.forEach(entry3 -> mplew.writeMapleAsciiString(entry3.getKey()));

        return mplew.getPacket();
    }

    public static byte[] showMonsterNotice(final int chrid, final int type, final String message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_NOTICE_MESSAGE.getValue());
        mplew.writeInt(Randomizer.nextInt(3));
        mplew.writeInt(chrid);
        mplew.writeInt(type);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(message);

        return mplew.getPacket();
    }

    public static byte[] controlLaser(final int moboid, final int angle, final int x, final boolean isFirstUse) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_LASER_CONTROL.getValue());
        mplew.writeInt(moboid);
        mplew.writeInt(angle);
        mplew.writeInt(x);
        mplew.writeBool(isFirstUse);

        return mplew.getPacket();
    }

    public static byte[] showMonsterPhaseChange(final int moid, final int reduceDamageType) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_PHASE_CHANGE.getValue());
        mplew.writeInt(moid);
        mplew.writeInt(reduceDamageType);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] changeMonsterZone(final int moid, final int reduceDamageType) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_ZONE_CHANGE.getValue());
        mplew.writeInt(moid);
        mplew.writeInt(reduceDamageType);

        return mplew.getPacket();
    }

    public static byte[] a(final int moboid, final int skilllevel, final Map<Integer, Point> pointMap, final boolean isFacingLeft) {
        return a(moboid, 42, skilllevel, 0, null, pointMap, isFacingLeft);
    }

    public static byte[] a(final int moboid, final int skilllevel, final int n3, final int n4, final Point point, final boolean isFacingLeft) {
        return a(moboid, skilllevel, n3, n4, point, null, isFacingLeft);
    }

    public static byte[] a(final int moboid, final int skilllevel, final int n3, final int n4, final Point point, final Map<Integer, Point> pointMap, final boolean isFacingLeft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_DEMIAN_DELAYED_ATTACK_CREATE.getValue());
        mplew.writeInt(moboid);
        mplew.writeInt(170);
        mplew.writeInt(skilllevel);
        if (skilllevel == 42) {
            mplew.writeBool(isFacingLeft);
            mplew.writeInt(n3);
            mplew.writeInt(pointMap.size());
            for (final Entry<Integer, Point> entry : pointMap.entrySet()) {
                a(mplew, entry.getKey(), entry.getValue());
                mplew.writeInt(Randomizer.rand(73, 95));
            }
        } else if (skilllevel > 44 && skilllevel <= 47) {
            mplew.writeBool(isFacingLeft);
            mplew.writeInt(n3);
            a(mplew, n4, point);
        }

        return mplew.getPacket();
    }

    public static void a(MaplePacketLittleEndianWriter mplew, final int n, final Point point) {
        mplew.writeInt(n);
        mplew.writeInt(point.x);
        mplew.writeInt(point.y);
    }

    public static byte[] teleportMonster(final int moboid, final boolean b, final int n2, final Point point, final int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MONSTER_TELEPORT.getValue());
        mplew.writeInt(moboid);
        mplew.writeBool(b);
        if (!b) {
            mplew.writeInt(n2);
            switch (n2) {
                case 3:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10: {
                    mplew.writeInt(point.x);
                    mplew.writeInt(point.y);
                    break;
                }
                case 4: {
                    mplew.writeInt(n3);
                    break;
                }
            }
        }

        return mplew.getPacket();
    }

    public static byte[] cancelMonsterAction(final MapleMonster monster, final int n) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_MONSTER_ACTION.getValue());
        mplew.writeInt(monster.getObjectId());
        mplew.write(n);

        return mplew.getPacket();
    }

    public static byte[] CreateDemianFlyingSword(boolean bl2, int monsterOid, int nodeid, Point point) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DEMIAN_FLYING_SWORD_CREATE.getValue());
        mplew.writeBool(bl2);
        if (bl2) {
            mplew.writeInt(monsterOid);
            mplew.write(0);
            mplew.write(4);
            mplew.writeInt(nodeid);
            mplew.writeInt(point.x);
            mplew.writeInt(point.y);
        } else {
            mplew.writeInt(monsterOid);
        }

        return mplew.getPacket();
    }

    public static byte[] NodeDemianFlyingSword(int n2, boolean bl2, MapleSwordNode node) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DEMIAN_FLYING_SWORD_NODE.getValue());
        mplew.writeInt(n2);
        mplew.writeInt(node.getBKM());
        mplew.writeBool(bl2);
        mplew.writeInt(node.getSwordNodeInfos().size());
        for (MapleSwordNode.MapleSwordNodeInfo a2 : node.getSwordNodeInfos()) {
            mplew.write(a2.getNodeType());
            mplew.writeShort(a2.getBKS());
            mplew.writeShort(a2.getNodeIndex());
            mplew.writeShort(a2.getBKU());
            mplew.writeInt(a2.getBKV());
            mplew.writeInt(a2.getBKW());
            mplew.writeInt(a2.getBKX());
            mplew.write(a2.getBKZ());
            mplew.write(a2.getBKY());
            mplew.writeInt(a2.getPos().x);
            mplew.writeInt(a2.getPos().y);
        }

        return mplew.getPacket();
    }

    public static byte[] TargetDemianFlyingSword(int n2, int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DEMIAN_FLYING_SWORD_TARGET.getValue());
        mplew.writeInt(n2);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }
}
