package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.anticheat.CheatingOffense;
import client.skills.Skill;
import client.skills.SkillFactory;
import client.skills.SummonSkillEntry;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.SkillConstants;
import constants.skills.*;
import handling.opcode.EffectOpcode;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.life.MapleMonster;
import server.maps.*;
import server.movement.LifeMovementFragment;
import tools.AttackPair;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.packet.EffectPacket;
import tools.packet.MobPacket;
import tools.packet.SummonPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class SummonHandler {

    private static final Logger log = LogManager.getLogger(MovementParse.class.getName());

    public static void MoveDragon(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.skip(4); //[00 00 00 00]
        slea.skip(4); //开始的坐标
        slea.skip(4); //摆动的坐标
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 5);
        if (chr != null && chr.getDragon() != null && res.size() > 0) {
//            if (slea.available() != 8) {
//                System.out.println("slea.available() != 8 (龙龙移动错误) 剩余封包长度: " + slea.available());
//                FileoutputUtil.log(FileoutputUtil.Movement_Log, "slea.available() != 8 (龙龙移动错误) 封包: " + slea.toString(true));
//                return;
//            }
            Point pos = chr.getDragon().getPosition();
            MovementParse.updatePosition(res, chr.getDragon(), 0);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, SummonPacket.moveDragon(chr.getDragon(), pos, res), chr.getTruePosition());
            }
        }
    }

    /*
     * 龙飞行
     */
    public static void DragonFly(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getDragon() == null) {
            return;
        }
        /*
         * 1902040 - 第1阶段龙 - (无描述)
         * 1902041 - 第2阶段龙 - (无描述)
         * 1902042 - 第3阶段龙 - (无描述)
         * 1912033 - 第1阶段龙鞍 - (无描述)
         * 1912034 - 第2阶段龙鞍 - (无描述)
         * 1912035 - 第3阶段龙鞍 - (无描述)
         */
        int type = slea.readInt();
        int mountId = type == 0 ? slea.readInt() : 0;
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.showDragonFly(chr.getId(), type, mountId), chr.getTruePosition());
    }

    public static void MoveSummon(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        int objid = slea.readInt();
        MapleMapObject obj = chr.getMap().getMapObject(objid, MapleMapObjectType.SUMMON);
        if (obj == null) {
            return;
        }
        if (obj instanceof MapleDragon) {
            MoveDragon(slea, chr);
            return;
        }
        MapleSummon sum = (MapleSummon) obj;
        if (sum.getOwnerId() != chr.getId() || sum.getSkillLevel() <= 0 || sum.getMovementType() == SummonMovementType.不会移动) {
            return;
        }
        slea.skip(4); //[00 00 00 00]
        slea.skip(4); //开始的坐标
        slea.skip(4); //摆动的坐标
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 4);
        Point pos = sum.getPosition();
        MovementParse.updatePosition(res, sum, 0);
        if (res.size() > 0) {
//            if (slea.available() != 8) {
////                System.out.println("slea.available() != 8 (召唤兽移动错误) 剩余封包长度: " + slea.available());
//                FileoutputUtil.log(FileoutputUtil.Movement_Log, "slea.available() != 8 (召唤兽移动错误) 封包: " + slea.toString(true));
//                return;
//            }
            chr.getMap().broadcastMessage(chr, SummonPacket.moveSummon(chr.getId(), sum.getObjectId(), pos, res), sum.getTruePosition());
        }
    }

    public static void DamageSummon(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null) {
            return;
        }
        int sumoid = slea.readInt(); //召唤兽的工作ID
        MapleSummon summon = chr.getMap().getSummonByOid(sumoid);
        if (summon == null || summon.getOwnerId() != chr.getId()) {
            return;
        }
        int type = slea.readByte(); //受到伤害的类型
        int damage = slea.readInt(); //受到伤害的数字
        int monsterIdFrom = slea.readInt(); //怪物的ID
        slea.skip(1); //未知 00
        int moboid = slea.available() >= 4 ? slea.readInt() : 0;
        MapleMonster monster = chr.getMap().getMonsterByOid(moboid);
        if (monster == null) {
            return;
        }
        boolean remove = false;
        if (summon.is替身术() && damage > 0) {
            summon.addSummonHp(-damage);
            if (summon.getSummonHp() <= 0) {
                remove = true;
            } else if (summon.is神箭幻影()) {
                List<Pair<Long, Boolean>> allDamageNumbers = new ArrayList<>();
                List<AttackPair> allDamage = new ArrayList<>();
                long theDmg = (long) (SkillFactory.getSkill(summon.getSkillId()).getEffect(summon.getSkillLevel()).getY() * damage / 100.0);
                allDamageNumbers.add(new Pair<>(theDmg, false));
                allDamage.add(new AttackPair(monster.getObjectId(), allDamageNumbers));
                chr.getMap().broadcastMessage(SummonPacket.summonAttack(summon, (byte) 0x84, (byte) 0x11, allDamage, chr.getLevel(), true));
                monster.damage(chr, theDmg, true);
                chr.checkMonsterAggro(monster);
                if (!monster.isAlive()) {
                    chr.send(MobPacket.killMonster(monster.getObjectId(), 1));
                }
            }
            chr.getMap().broadcastMessage(chr, SummonPacket.damageSummon(chr.getId(), summon.getSkillId(), damage, type, monsterIdFrom), summon.getTruePosition());
        }
        if (remove) {
            chr.dispelSkill(summon.getSkillId());
        }
    }

    /**
     * 解析客户"召唤兽攻击"封包并响应客户端
     *
     * @param slea
     * @param c
     * @param chr
     */
    public static void SummonAttack(final LittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (chr == null || !chr.isAlive() || chr.getMap() == null) {
            return;
        }
        MapleMap map = chr.getMap();
        int objid = slea.readInt();
        MapleMapObject obj = map.getMapObject(objid, MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            chr.dropMessage(5, "召唤兽已经消失。");
            return;
        }
        MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != chr.getId() || summon.getSkillLevel() <= 0) {
            chr.dropMessage(5, "出现错误.");
            return;
        }
        int skillid = summon.getSkillId();
        SummonSkillEntry sse = SkillFactory.getSummonData(skillid);
        if (skillid / 1000000 != 35 && sse == null) {
            chr.dropMessage(5, "召唤兽攻击处理出错。");
            return;
        }
        int tick = slea.readInt();
        int linkskill = slea.readInt();
        int linkskill2 = slea.readInt();
        boolean uselink = false;
        switch (summon.getSkillId()) {
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
            case 豹弩游侠.辅助打猎单元: {
                uselink = true;
            }
        }
        byte animation = slea.readByte();
        byte numAttackedAndDamage = slea.readByte();
        byte numAttacked = (byte) ((numAttackedAndDamage >>> 4) & 0xF); //召唤兽攻击怪物的数量
        byte numDamage = (byte) (numAttackedAndDamage & 0xF); //召唤兽攻击怪物的次数
//        if (sse != null) {
//            if (sse.delay > 0) {
//                chr.updateTick(tick);
//                summon.CheckSummonAttackFrequency(chr, tick);
//            }
//            int mobCount = sse.mobCount, numAttackCount = sse.attackCount;
//            if (uselink && (linkskill != 0 || linkskill2 != 0)) {
//                Skill skill = SkillFactory.getSkill(linkskill2 != 0 ? linkskill2 : linkskill);
//                if (skill != null) {
//                    MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(linkskill2 != 0 ? linkskill2 : linkskill));
//                    if (effect != null) {
//                        mobCount = effect.getMobCount();
//                        numAttackCount = effect.getAttackCount();
//                    }
//                }
//            }
//            if (numAttacked > mobCount) {
//                if (chr.isShowPacket()) {
//                    chr.dropMessage(-5, "召唤兽攻击次数错误 (Skillid : " + skillid + " 怪物数量 : " + numAttacked + " 默认数量: " + sse.mobCount + ")");
//                }
//                chr.dropMessage(5, "[警告] 请不要使用非法程序。召唤兽攻击怪物数量错误.");
//                chr.getCheatTracker().registerOffense(CheatingOffense.SUMMON_HACK_MOBS);
//                return;
//            }
//            if (numDamage > numAttackCount) {
//                if (chr.isShowPacket()) {
//                    chr.dropMessage(-5, "召唤兽攻击次数错误 (Skillid : " + skillid + " 打怪次数 : " + numDamage + " 默认次数: " + sse.attackCount + " 超级技能增加次数: " + chr.getStat().getAttackCount(skillid) + ")");
//                }
//                chr.dropMessage(5, "[警告] 请不要使用非法程序。召唤兽攻击怪物次数错误.");
//                chr.getCheatTracker().registerOffense(CheatingOffense.SUMMON_HACK_MOBS);
//                return;
//            }
//        }
        slea.skip(skillid == 机械师.磁场 ? 39 : 27); //怪物的坐标和召唤兽的坐标 后面还有4个00 (mob x,y and summon x,y)
        List<Pair<Long, Boolean>> allDamageNumbers;
        List<AttackPair> allDamage = new ArrayList<>();
        for (int i = 0; i < numAttacked; i++) {
            int moboid = slea.readInt();
            slea.skip(24); //怪物在WZ中的ID
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < numDamage; j++) {
                long damge = slea.readLong();
                if (chr.isAdmin()) {
                    chr.dropMessage(-5, "召唤兽攻击 打怪数量: " + numAttacked + " 打怪次数: " + numDamage + " 打怪伤害: " + damge + " 怪物ID: " + moboid);
                }
                if (damge > chr.getMaxDamageOver(0) && !chr.isGM()) {
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chr.getName() + " ID: " + chr.getId() + " (等级 " + chr.getLevel() + ") 召唤兽攻击伤害异常。打怪伤害: " + damge + " 地图ID: " + chr.getMapId()));
                }
                allDamageNumbers.add(new Pair<>(damge, false)); //[伤害数值] [是否暴击]
            }
            slea.skip(14); //未知 [00 00 00 00]
            allDamage.add(new AttackPair(moboid, allDamageNumbers)); //[怪物ID] [伤害信息]
        }
        Skill summonSkill = SkillFactory.getSkill(skillid);
        MapleStatEffect summonEffect = summonSkill.getEffect(summon.getSkillLevel());
        MapleStatEffect summonAttackEffect = null;
        if (uselink && linkskill2 > 0) {
            summonAttackEffect = SkillFactory.getSkill(linkskill2).getEffect(summon.getSkillLevel());
        }
        if (summonEffect == null) {
            chr.dropDebugMessage(1, "[召唤兽攻击] 召唤兽攻击出现错误 => 攻击效果为空.");
            return;
        }
        //发送召唤兽攻击怪物的封包
        if (allDamage.isEmpty()) { //如果攻击封包为空就返回
            return;
        }
        map.broadcastMessage(chr, SummonPacket.summonAttack(summon, animation, numAttackedAndDamage, allDamage, chr.getLevel(), false), summon.getTruePosition());
        for (AttackPair attackEntry : allDamage) {
            MapleMonster targetMob = map.getMonsterByOid(attackEntry.objectid);
            if (targetMob == null) {
                continue;
            }
            int totDamageToOneMonster = 0;
            for (Pair<Long, Boolean> eachde : attackEntry.attack) {
                long toDamage = eachde.left;
                if (toDamage < (chr.getStat().getCurrentMaxBaseDamage() * 5.0 * (summonEffect.getSelfDestruction() + summonEffect.getDamage() + (uselink && summonAttackEffect != null ? summonAttackEffect.getDamage() : 0) + chr.getStat().getDamageIncrease(summonEffect.getSourceid())) / 100.0)) { //10 x dmg.. eh
                    totDamageToOneMonster += toDamage;
                } else {
                    chr.dropDebugMessage(2, "[召唤兽攻击] 召唤兽攻击怪物伤害过高.");
                    if (!chr.isGM()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chr.getName() + " ID: " + chr.getId() + " (等级 " + chr.getLevel() + ") 召唤兽攻击伤害异常。打怪伤害: " + toDamage + " 地图ID: " + chr.getMapId()));
                        break;
                    }
                }
            }
            if (sse != null && sse.delay > 0 && summon.getMovementType() != SummonMovementType.不会移动 && summon.getMovementType() != SummonMovementType.CIRCLE_STATIONARY && summon.getMovementType() != SummonMovementType.自由移动 && chr.getTruePosition().distanceSq(targetMob.getTruePosition()) > 400000.0) {
                if (!chr.getMap().isBossMap()) {
                    chr.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER_SUMMON);
                }
            }
            if (totDamageToOneMonster > 0) {
                if (chr.isShowPacket()) {
                    chr.dropMessage(5, "召唤兽打怪最终伤害 : " + totDamageToOneMonster);
                }
                targetMob.damage(chr, totDamageToOneMonster, true);
                chr.checkMonsterAggro(targetMob);
                if (!targetMob.isAlive()) {
                    chr.send(MobPacket.killMonster(targetMob.getObjectId(), 1));
                } else {
                    if (summonEffect.getMonsterStati().size() > 0) {
                        if (summonEffect.makeChanceResult()) {
                            for (Entry<MonsterStatus, Integer> z : summonEffect.getMonsterStati().entrySet()) {
                                targetMob.applyStatus(chr, new MonsterStatusEffect(z.getKey(), z.getValue(), summonSkill.getId(), null, false, 0), summonEffect.isPoison(), 4000, true, summonEffect);
                            }
                        }
                    }
                }
            }
        }

        if (summonEffect.getSkill().isNonExpireSummon() && System.currentTimeMillis() > summon.getCreateTime() + summonEffect.getDuration()) {
            chr.getMap().broadcastMessage(SummonPacket.removeSummon(summon, true));
            chr.getMap().removeMapObject(summon);
            chr.removeVisibleMapObject(summon);
            chr.removeSummon(summon);
        }
    }

    public static void RemoveSummon(LittleEndianAccessor slea, MapleClient c) {
        int objid = slea.readInt();
        MapleMapObject obj = c.getPlayer().getMap().getMapObject(objid, MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        MapleSummon summon = (MapleSummon) obj;
        if (summon.getOwnerId() != c.getPlayer().getId() || summon.getSkillLevel() <= 0) {
            c.getPlayer().dropMessage(5, "移除召唤兽出现错误.");
            return;
        }
        if (c.getPlayer().isShowPacket()) {
            c.getPlayer().dropSpouseMessage(0x0A, "收到移除召唤兽信息 - 召唤兽技能ID: " + summon.getSkillId() + " 技能名字 " + SkillFactory.getSkillName(summon.getSkillId()));
        }
        if (summon.getSkillId() == 机械师.磁场) {
            return;
        }
        c.getPlayer().getMap().broadcastMessage(SummonPacket.removeSummon(summon, false));
        c.getPlayer().getMap().removeMapObject(summon);
        c.getPlayer().removeVisibleMapObject(summon);
        c.getPlayer().removeSummon(summon);
        c.getPlayer().dispelSkill(summon.getSkillId());
        if (summon.is天使召唤兽()) {
            int buffId = summon.getSkillId() % 10000 == 1087 ? 2022747 : summon.getSkillId() % 10000 == 1179 ? 2022823 : 2022746;
            c.getPlayer().dispelBuff(buffId); //取消天使加的BUFF效果
        }
        if (summon.getSkillId() == 爆莉萌天使.爱星能量) {
            c.getPlayer().send(MaplePacketCreator.userBonusAttackRequest(爆莉萌天使.爱星能量, 0, Collections.emptyList()));
        }
    }

    public static void SubSummon(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMapObject obj = chr.getMap().getMapObject(slea.readInt(), MapleMapObjectType.SUMMON);
        if (obj == null || !(obj instanceof MapleSummon)) {
            return;
        }
        MapleSummon sum = (MapleSummon) obj;
        if (sum.getOwnerId() != chr.getId() || sum.getSkillLevel() <= 0 || !chr.isAlive()) {
            return;
        }
        switch (sum.getSkillId()) {
            case 机械师.机器人工厂_RM1:
                if (!chr.canSummon(2000)) {
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    MapleStatEffect effect = SkillFactory.getSkill(机械师.机器人工厂_机器人).getEffect(sum.getSkillLevel());
                    MapleSummon tosummon = new MapleSummon(chr, effect, new Point(sum.getTruePosition().x, sum.getTruePosition().y - 5), SummonMovementType.自由移动, effect.getDuration(), 0);
                    chr.getMap().spawnSummon(tosummon);
                    chr.addSummon(tosummon);
                }
                break;
            case 机械师.支援波动器_H_EX:
            case 机械师.支援波动器强化:
                if (!chr.canSummon(1000)) {
                    return;
                }
                chr.addHP((int) (chr.getStat().getCurrentMaxHp() * SkillFactory.getSkill(sum.getSkillId()).getEffect(sum.getSkillLevel()).getHp() / 100.0));
                chr.send(EffectPacket.showOwnBuffEffect(sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), sum.getSkillLevel()));
                chr.getMap().broadcastMessage(chr, EffectPacket.showBuffeffect(chr, sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), sum.getSkillLevel()), false);
                break;
            case 黑骑士.灵魂助力:
                Skill bHealing = SkillFactory.getSkill(slea.readInt());
                int bHealingLvl = chr.getTotalSkillLevel(bHealing);
                if (bHealingLvl <= 0 || bHealing == null) {
                    return;
                }
                MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
                if (bHealing.getId() == 黑骑士.灵魂祝福) {
                    healEffect.applyTo(chr);
                } else if (bHealing.getId() == 黑骑士.灵魂助力) { //黑骑士.灵魂治愈
                    if (!chr.canSummon(healEffect.getX() * 1000)) {
                        return;
                    }
                    int healHp = Math.min(1000, healEffect.getHp() * chr.getLevel());
                    chr.addHP(healHp);
                }
                chr.send(EffectPacket.showOwnBuffEffect(sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), bHealingLvl));
                chr.getMap().broadcastMessage(SummonPacket.summonSkill(chr.getId(), sum.getSkillId(), bHealing.getId() == 黑骑士.灵魂助力 ? 5 : (Randomizer.nextInt(3) + 6)));
                chr.getMap().broadcastMessage(chr, EffectPacket.showBuffeffect(chr, sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), chr.getLevel(), bHealingLvl), false);
                break;
            case 尖兵.全息力场_支援:
                SkillFactory.getSkill(sum.getSkillId()).getEffect(sum.getSkillLevel()).applyTo(chr);
                break;
        }
        if (SkillConstants.is天使祝福戒指(sum.getSkillId())) {
            if (sum.getSkillId() % 10000 == 1087) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022747).applyTo(chr); //黑天使的祝福
            } else if (sum.getSkillId() % 10000 == 1179) {
                MapleItemInformationProvider.getInstance().getItemEffect(2022823).applyTo(chr); //白天使的祝福
            } else {
                MapleItemInformationProvider.getInstance().getItemEffect(2022746).applyTo(chr); //天使的祝福
            }
            chr.send(EffectPacket.showOwnBuffEffect(sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), 2, 1));
            chr.getMap().broadcastMessage(chr, EffectPacket.showBuffeffect(chr, sum.getSkillId(), EffectOpcode.UserEffect_SkillAffected.getValue(), 2, 1), false);
        }
    }

    public static void MoveLittleWhite(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.skip(17);
        List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 7);

        if (res != null && chr != null && chr.getMap() != null && chr.getLittleWhite() != null && res.size() > 0) {
            if (slea.available() != 1) {
                //System.out.println("slea.available() != 8 (小白移动错误) 剩余封包长度: " + slea.available());

                log.error("slea.available() != 1 (小白移动错误) 封包: " + slea.toString(true));
                return;
            }
            Point pos = new Point(chr.getLittleWhite().getPosition());
            chr.getLittleWhite().updatePosition(res);
            if (!chr.isHidden()) {
                chr.getMap().broadcastMessage(chr, SummonPacket.moveLittleWhite(chr.getId(), chr.getLittleWhite().getObjectId(), pos, chr.getLittleWhite().getStance(), res), false);
            }
        }
    }

    public static void SubLittleWhite(LittleEndianAccessor slea, MapleCharacter chr) {
        slea.skip(4);
        int skillType = slea.readInt();
        Skill bHealing = SkillFactory.getSkill(阴阳师.幻醒_小白);
        int bHealingLvl = chr.getTotalSkillLevel(bHealing);
        boolean forth = true;
        if (bHealingLvl <= 0 || bHealing == null) {
            bHealing = SkillFactory.getSkill(阴阳师.影朋_小白);
            bHealingLvl = chr.getTotalSkillLevel(bHealing);
            forth = false;
        }
        if (bHealingLvl <= 0 || bHealing == null) {
            return;
        }
        int effectid = 0;
        switch (skillType) {
            case 3:
                effectid = 阴阳师.花炎结界;
                break;
            case 5:
                effectid = 阴阳师.幽玄气息;
                break;
        }
        effectid += forth ? 20000 : 0;
        bHealing = SkillFactory.getSkill(effectid);
        bHealingLvl = chr.getTotalSkillLevel(bHealing);
        MapleStatEffect healEffect = bHealing.getEffect(bHealingLvl);
        if (healEffect != null) {
            healEffect.applyTo(chr);
        }
//        chr.send(EffectPacket.show影朋小白效果(skillType));
        chr.getMap().broadcastMessage(EffectPacket.show影朋小白效果(skillType));
        chr.getMap().broadcastMessage(chr, EffectPacket.show影朋小白效果(skillType), false);
    }
}
