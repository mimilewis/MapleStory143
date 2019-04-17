package handling.channel.handler;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.PlayerSpecialStats;
import client.PlayerStats;
import client.anticheat.CheatTracker;
import client.anticheat.CheatingOffense;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.JobConstants;
import constants.ServerConstants;
import constants.SkillConstants;
import constants.skills.*;
import handling.world.WorldBroadcastService;
import handling.world.party.MaplePartyCharacter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.AutobanManager;
import server.MapleStatEffect;
import server.MapleStatEffectFactory;
import server.life.Element;
import server.life.MapleMonster;
import server.life.MapleMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMist;
import server.maps.pvp.MaplePvp;
import tools.*;
import tools.data.input.LittleEndianAccessor;
import tools.packet.ForcePacket;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * 解析玩家所有的攻击行为.
 *
 * @author dongjak
 */
public class DamageParse {

    private static final Logger log = LogManager.getLogger(DamageParse.class.getName());

    /**
     * @param attack              攻击信息
     * @param theSkill            技能信息
     * @param player              角色信息
     * @param attackCount         攻击次数
     * @param effect              技能效果
     * @param maxDamagePerMonster 每个怪物的最大伤害
     * @param visProjectile       可见的子弹、箭矢、飞镖等...
     */
    public static void applyAttack(AttackInfo attack, Skill theSkill, MapleCharacter player, int attackCount, double maxDamagePerMonster, MapleStatEffect effect, int visProjectile) {
        if (!player.isAlive() || player.isBanned()) { //如果玩家死亡
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD, "操作者已死亡.");
            return;
        }
        if (attack.real && SkillConstants.getAttackDelay(attack.skillId, theSkill) >= 50) { //大于100的就检测攻击时间
            player.getCheatTracker().checkAttack(attack.skillId, attack.lastAttackTickCount);
        }
        if (attack.skillId != 0) { //当攻击技能不等于空
            if (effect == null) {
                player.getClient().announce(MaplePacketCreator.enableActions());
                return;
            }
            if (SkillConstants.isMulungSkill(attack.skillId)) {
                if (player.getMapId() / 10000 != 92502) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Mu Lung dojo skill out of dojo maps.");
                    return;
                } else {
                    if (player.getMulungEnergy() < 10000) {
                        return;
                    }
                    player.mulung_EnergyModify(false);
                }
            } else if (SkillConstants.isPyramidSkill(attack.skillId)) {
                if (player.getMapId() / 1000000 != 926) {
                    //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                    return;
                } else {
                    if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                        return;
                    }
                }
            } else if (SkillConstants.isInflationSkill(attack.skillId)) {
                if (player.getBuffedValue(MapleBuffStat.巨人药水) == null) {
                    return;
                }
            } else if (JobConstants.is魂骑士(player.getJob()) && player.getBuffedValue(MapleBuffStat.日月轮转) != null) {
                int b = player.getSpecialStat().getMoonCycle();
                int skillid = b == 0 ? 魂骑士.月光洒落 : 魂骑士.旭日;
                SkillFactory.getSkill(skillid).getEffect(player.getSkillLevel(skillid)).applyTo(player);
            } else if (JobConstants.is神之子(player.getJob())) {
                player.handle提速时刻();
            }

            int mobCount = effect.getMobCount();
            if (player.getStatForBuff(MapleBuffStat.激素狂飙) != null) {
                mobCount += 5;
            }
            if (player.getTotalSkillLevel(圣骑士.万佛归一破) > 0) {
                mobCount += SkillFactory.getSkill(圣骑士.万佛归一破).getEffect(player.getTotalSkillLevel(圣骑士.万佛归一破)).getTargetPlus();
            }
            if (attack.numAttacked > mobCount && attack.skillId != 圣骑士.万佛归一破) { // Must be done here, since NPE with normal atk
                player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT, "异常的攻击次数.");
                if (player.isShowPacket()) {
                    player.dropMessage(-5, "物理怪物数量检测 => 封包解析次数: " + attack.numAttacked + " 服务端设置次数: " + effect.getMobCount(player));
                }
                return;
            }
            player.setLastAttackSkillId(attack.skillId);
        }

        if (player.isShowPacket()) {
            player.dropMessage(-1, "攻击动作: " + Integer.toHexString(attack.display));
        }

        /* 根据角色当前拥有的状态增加攻击次数 */
        if (player.getStatForBuff(MapleBuffStat.光暗转换) != null || player.getStatForBuff(MapleBuffStat.影分身) != null || (player.getStatForBuff(MapleBuffStat.骑兽技能) != null && player.getStatForBuff(MapleBuffStat.骑兽技能).is美洲豹骑士())) {
            attackCount *= 2;
        } else if (player.hasBuffSkill(神炮王.霰弹炮)) {
            attackCount *= 3;
        } else if (player.getStatForBuff(MapleBuffStat.战斗大师) != null || player.getStatForBuff(MapleBuffStat.激素狂飙) != null) {
            attackCount += 2;
        } else if (attack.skillId == 奇袭者.毁灭) {
            attackCount += Math.min(player.getBuffedIntValue(MapleBuffStat.百分比无视防御) / 5, player.getStat().raidenCount); //最大是5次 取最小值
        }


        /* 检测技能是否为正常的攻击次数 ： 在此处定义的技能将不会被检测 */
        boolean useAttackCount = !SkillConstants.isNoCheckAttackSkill(attack.skillId);

        /* 检测伤害次数是否大于攻击次数 */
        if (attack.numDamage > attackCount) {
            if (useAttackCount) {
                if (player.isShowPacket()) {
                    player.dropDebugMessage(3, "[攻击信息] 物理攻击次数出错 技能ID:" + attack.skillId + " 封包解析次数:" + attack.numDamage + " 实际次数:" + attackCount);
                }
                player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT, "异常的攻击次数.");
                log.info("[作弊] " + player.getName() + " 物理攻击次数异常。 attack.hits " + attack.numDamage + " attackCount " + attackCount + " 技能ID " + attack.skillId);
                if (ServerConstants.isShowGMMessage()) {
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[管理员信息] " + player.getName() + " ID: " + player.getId() + " (等级 " + player.getLevel() + ") 物理攻击次数异常。 attack.hits " + attack.numDamage + " attackCount " + attackCount + " 技能ID " + attack.skillId));
                }
                return;
            }
        }

        /* 如果当前攻击的伤害数量大于0且攻击次数大于0 */
        if (attack.numDamage > 0 && attack.numAttacked > 0) {
            // 检查当前角色武器的耐久度
            if (!player.getStat().checkEquipDurabilitys(player, -1)) {
                player.dropMessage(5, "武器耐久度不足，无法进行攻击！");
                return;
            }
        }

        /* 检测角色主武器是否为灵魂武器，且当前的攻击技能是否为灵魂技能。*/
        if (player.checkSoulWeapon() && attack.skillId == player.getEquippedSoulSkill()) {
            player.checkSoulState(true);
        }

        /* PVP 设置*/
        if (player.getMap().isPvpMap()) {
            MaplePvp.doPvP(player, player.getMap(), attack, effect);
        } else if (player.getMap().isPartyPvpMap()) {
            MaplePvp.doPartyPvP(player, player.getMap(), attack, effect);
        } else if (player.getMap().isGuildPvpMap()) {
            MaplePvp.doGuildPvP(player, player.getMap(), attack, effect);
        }

        /* 定义伤害数据信息 */
        int fixeddmg;                                                   // 固定伤害
        long totalDamage = 0;                                           // 总伤害
        long totDamageToOneMonster = 0;                                 // 总伤害到一个怪物
        long monsterMaxHp = 0;                                          // 怪物最大HP
        long maxDamagePerHit = player.getMaxDamageOver(attack.skillId); // 每次命中怪物时最大的伤害等于角色当前攻击技能的最大伤害值
        MapleMonster monster;                                           // 怪物
        MapleMonsterStats monsterstats;                                 // 怪物状态信息
        int lastKillMob = 0;                                            // 最后一个杀死的怪物
        int lastKillMobExp = 0;                                         // 最后一个杀死的怪物的经验
        Map<Integer, Integer> killMobList = new LinkedHashMap<>();      // 创建一个杀死怪物的列表
        boolean isUseSkillEffect = true;

        /* 是否为暴击伤害 */
        boolean isCritDamage = false;

        /* 开始解析伤害信息 */
        for (AttackPair oned : attack.allDamage) {
            monster = player.getMap().getMonsterByOid(oned.objectid);
            if (monster != null && monster.getLinkCID() <= 0) {
                totDamageToOneMonster = 0;
                monsterMaxHp = monster.getMobMaxHp();
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                long eachd;
                for (Pair<Long, Boolean> eachde : oned.attack) {
                    if (eachde.right) {
                        isCritDamage = true;
                    }
                    eachd = eachde.left;
                    if (player.isShowPacket() && eachd > 0) {
                        player.dropMessage(-1, "物理攻击打怪伤害 : " + eachd + " 服务端预计伤害 : " + maxDamagePerHit + " 是否超过 : " + (eachd > maxDamagePerHit) + " 是否爆击: " + eachde.right);
                    }
                    if (fixeddmg != -1) {
                        if (monsterstats.getOnlyNoramlAttack()) { //如果怪物是只容许普通攻击的类型
                            eachd = attack.skillId != 0 ? 0 : fixeddmg;
                        } else {
                            eachd = fixeddmg;
                        }
                    } else {
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = attack.skillId != 0 ? 0 : Math.min(eachd, maxDamagePerHit);  // 转换为服务端来计算伤害数据
                        } else if (!player.isGM()) {
                            if (eachd > maxDamagePerHit && maxDamagePerHit > 2) {
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE, "[伤害: " + eachd + ", 预计伤害: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]");
                                if (attack.real) {
                                    player.getCheatTracker().checkSameDamage(eachd, maxDamagePerHit);
                                }
                                if (eachd > maxDamagePerHit * 2 && attack.skillId != 飞侠.双飞斩) {
                                    String banReason = player.getName() + " 被系统封号.[异常攻击伤害值: " + eachd + ", 预计伤害: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]";
                                    if (player.getLevel() < 10 && eachd >= 10000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 20 && eachd >= 20000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 30 && eachd >= 40000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 50 && eachd >= 60000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_2, "[伤害: " + eachd + ", 预计伤害: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]");
                                }
                            } else {
                                if (eachd > maxDamagePerHit) {
                                    eachd = (int) (maxDamagePerHit);
                                }
                            }
                        }
                    }
                    totDamageToOneMonster += eachd;
                    //force the miss even if they dont miss. popular wz edit
                    if ((eachd == 0 || monster.getId() == 9700021) && player.getPyramidSubway() != null) { //miss
                        player.getPyramidSubway().onMiss(player);
                    }
                }

                /* 总伤害等于总伤害加上对每个怪物的总伤害！*/
                totalDamage += totDamageToOneMonster;

                /* 检测当前怪物是否为自动攻击， 控制器为空 */
                player.checkMonsterAggro(monster);

                /* 处理技能 */

                /* 对怪物的伤害大于0时处理的数据 */
                if (totDamageToOneMonster > 0 || attack.skillId == 圣骑士.圣域) {

                    /* 角色是刺客时标记攻击怪物效果*/
                    if (player.isBuffFrom(MapleBuffStat.刺客标记, SkillFactory.getSkill(隐士.刺客标记))) {

                        Skill mskill = null;
                        int mskillid = 0;
                        int mskillevel = 0;
                        MapleStatEffect effectSkill = null;

                        if (player.getSkillLevel(SkillFactory.getSkill(隐士.刺客标记)) > 0) {
                            mskillid = 隐士.刺客标记;
                            mskill = SkillFactory.getSkill(隐士.刺客标记);
                            mskillevel = player.getSkillLevel(SkillFactory.getSkill(隐士.隐士标记));
                        }

                        if (player.getSkillLevel(SkillFactory.getSkill(隐士.隐士标记)) > 0) {
                            mskillid = 隐士.隐士标记;
                            mskill = SkillFactory.getSkill(隐士.隐士标记);
                            mskillevel = player.getSkillLevel(SkillFactory.getSkill(隐士.隐士标记));
                        }
                        for (MonsterStatusEffect s : monster.getAllBuffs()) {
                            //检测该怪物是否存在刺客标记
                            if (s.getSkill() == mskillid) {
                                if (JobConstants.is隐士(player.getJob())) {
                                    if (attack.skillId != 0 && attack.skillId != 隐士.刺客标记_飞镖 && attack.skillId != 隐士.隐士标记_飞镖 && visProjectile > 0) {
                                        isUseSkillEffect = false;
                                        player.handleAssassinStack(monster, visProjectile);
                                        //对其怪物取消标记
                                        monster.cancelSingleStatus(s);
                                        //monster.cancelSingleStatus(new MonsterStatusEffect(MonsterStatus.中毒, effectSkill.getDOT(), mskillid, null, false));
                                    }
                                }
                            }
                        }

                        if (isUseSkillEffect) {
                            if (mskill != null) {
                                effectSkill = mskill.getEffect(mskillevel);
                            }
                            if (effectSkill != null) {
                                if (attack.skillId != 0 && attack.skillId != 隐士.刺客标记_飞镖 && attack.skillId != 隐士.隐士标记_飞镖 && visProjectile > 0) {
                                    if (effectSkill.makeChanceResult()) {
                                        monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, effectSkill.getDOT(), mskillid, null, false, effectSkill.getDOTStack()), true, effectSkill.getDuration(), true, effectSkill);
                                    }
                                }
                            }
                        }
                    }
                    /* 处理技能伤害 */
                    for (Triple<Integer, String, Integer> psdSkill : player.getStat().getPsdSkills()) {
                        if (psdSkill.left == attack.skillId && attack.skillId != 0 && !psdSkill.mid.isEmpty()) {
                            totDamageToOneMonster *= (1.0 + (MapleStatEffectFactory.parseEval(psdSkill.mid, player.getSkillLevel(psdSkill.right)) / 100.0));
                        }
                    }

                    /* 处理技能圣域 */
                    boolean killmob;

                    /* 用巨大的锤子击打地面，同时攻击15名以下的多个敌人。受到攻击的对象体力减至1，给BOSS造成致命伤。 */
                    if (attack.skillId != 圣骑士.圣域) {
                        killmob = monster.damage(player, totDamageToOneMonster, true, attack.skillId);
                    } else {
                        killmob = monster.damage(player, (monster.getStats().isBoss() ? 500000 : (monster.getHp() - 1)), true, attack.skillId);
                    }
                    if (killmob) {
                        killMobList.put(monster.getObjectId(), monster.getMobExpFromChannel());
                        lastKillMob = monster.getObjectId();
                        lastKillMobExp = monster.getMobExpFromChannel();
                        afterKillMonster(player, monster);

                    }
                    //怪物反射伤害 宙斯盾系统是无视反射的
                    boolean reflectDamage = true;
                    if (attack.skillId == 尖兵.宙斯盾系统 || player.getBuffedValue(MapleBuffStat.至圣领域) != null || player.hasBuffSkill(狂龙战士.终极变形_超级)) {
                        reflectDamage = false;
                    }
//                    if (player.isShowPacket()) {
//                        player.dropDebugMessage(1, "[技能信息] 怪物是否反射伤害: " + reflectDamage);
//                    }
                    if (reflectDamage && monster.isBuffed(MonsterStatus.MOB_STAT_PCounter)) {
                        player.addHP(-(7000 + Randomizer.nextInt(8000)));
                    }

                    onAttack(player, monster, attack.skillId, totalDamage, oned);


                    /* 当前攻击出的伤害对怪物伤害大于0的话，就根据角色穿戴的武器、技能状态、BUFF状态、给予怪物状态 */
                    if (totDamageToOneMonster > 0) {
                        /* 主武器 */
                        Item weapon_ = player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                        if (weapon_ != null) {
                            MonsterStatus stat = GameConstants.getStatFromWeapon(weapon_.getItemId()); //根据主武器的不同，分两种状态：1恐慌 2速度
                            if (stat != null && Randomizer.nextInt(100) < GameConstants.getStatChance()) {
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(stat, GameConstants.getXForStat(stat), GameConstants.getSkillForStat(stat), null, false, 0);
                                monster.applyStatus(player, monsterStatusEffect, false, 10000, false, null);
                            }
                        }

                        if (player.getBuffedValue(MapleBuffStat.炎术引燃) != null) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.炎术引燃);
                            if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.MOB_STAT_Poison)) {
                                //List<MonsterStatusEffect> monsterList = new ArrayList<>();
                                //monsterList.add(new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, 1, eff.getSourceid(), null, false, 0));
                                //monsterList.add(new MonsterStatusEffect(MonsterStatus.引燃, eff.getX(), eff.getSourceid(), null, false, 0));
                                monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, 1, eff.getSourceid(), null, false), true, eff.getDOTTime(), true, eff);
                                //monster.applyStatus(player, monsterList, true, eff.getDOTTime(), true, eff);
                            }
                        } else if (player.getBuffedValue(MapleBuffStat.额外回避) != null) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.额外回避);
                            if ((eff != null) && (eff.makeChanceResult())) {
                                monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Speed, eff.getX(), 3121007, null, false, 0), false, eff.getY() * 1000, true, eff);
                            }
                        } else if (player.getJob() == 121 || player.getJob() == 122) {
                            Skill skill = SkillFactory.getSkill(圣骑士.寒冰冲击);
                            if (player.isBuffFrom(MapleBuffStat.属性攻击, skill)) {
                                MapleStatEffect eff = skill.getEffect(player.getTotalSkillLevel(skill));
                                MonsterStatusEffect monsterStatusEffect = new MonsterStatusEffect(MonsterStatus.MOB_STAT_Freeze, 1, skill.getId(), null, false, 0);
                                monster.applyStatus(player, monsterStatusEffect, false, eff.getY() * 2000, true, eff);
                            }
                        } else if (JobConstants.is战神(player.getJob())) {
                            if (player.getBuffedValue(MapleBuffStat.属性攻击) != null && !monster.getStats().isBoss()) {
                                MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.属性攻击);
                                if (eff != null) {
                                    monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Speed, eff.getX(), eff.getSourceid(), null, false, 0), false, eff.getY() * 1000, true, eff);
                                }
                            }
                        }
                    }

                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false, 0), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                }
            }
        }
        //攻击怪物时的一些特殊处理
        if (monsterMaxHp > 0 && totDamageToOneMonster > 0) {
            afterAttack(player, attack.numAttacked, attack.numDamage, attack.skillId, isCritDamage);
            if (!killMobList.isEmpty() && lastKillMob > 0) {
                handleKillMobs(player, killMobList.size(), lastKillMob, lastKillMobExp);
            }
        }

        //特殊技能效果处理
        if (effect != null && attack.skillId != 0 && attack.numAttacked > 0 && !SkillConstants.isNoDelaySkill(attack.skillId) && !SkillConstants.isNoApplyTo(attack.skillId)) {
            boolean isApplyTo = true;
            if (effect.is超越攻击() && totDamageToOneMonster <= 0) {
                isApplyTo = false;
            }
            if (isApplyTo) {
                if (player.isShowPacket()) {
                    player.dropDebugMessage(1, "[攻击BUFF] 技能: " + SkillFactory.getSkillName(attack.skillId) + "(" + attack.skillId + ")");
                }
                effect.applyTo(player, attack.position, false); //这个地方是处理减少角色使用技能的HP或者MP
            }
        }
        //检测角色是否使用无敌
        if (totalDamage > 1 && SkillConstants.getAttackDelay(attack.skillId, theSkill) >= 100) {
            CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);
            if (tracker.getAttacksWithoutHit() >= 50) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, "无敌自动封号.");
            }
        }
    }

    public static void applyAttackMagic(AttackInfo attack, Skill theSkill, MapleCharacter player, MapleStatEffect effect, double maxDamagePerMonster) {
        if (!player.isAlive()) {
            player.getCheatTracker().registerOffense(CheatingOffense.ATTACKING_WHILE_DEAD);
            return;
        }
        if (attack.real && SkillConstants.getAttackDelay(attack.skillId, theSkill) >= 50) {  //大于100的就检测攻击时间
            player.getCheatTracker().checkAttack(attack.skillId, attack.lastAttackTickCount);
        }
        player.setLastAttackSkillId(attack.skillId);
        int mobCount = effect.getMobCount(player); //攻击怪物数量
        int attackCount = effect.getAttackCount(player); //攻击怪物次数
        if (player.getStatForBuff(MapleBuffStat.光暗转换) != null) {
            attackCount *= 2;
        }
        if (player.getStatForBuff(MapleBuffStat.天使复仇) != null && attack.skillId == 主教.光芒飞箭) {
            attackCount += 5;
        }
        if (attack.numDamage > attackCount || (attack.numAttacked > mobCount && attack.skillId != 夜光.绝对死亡)) {
            if (player.isShowPacket()) {
                player.dropDebugMessage(3, "[攻击信息] 魔法攻击次数出错 解析攻击次数:" + attack.numDamage + " 实际次数:" + attackCount + " 解析怪物数量:" + attack.numAttacked + " 实际怪物数量:" + mobCount);
            }
            player.getCheatTracker().registerOffense(CheatingOffense.MISMATCHING_BULLETCOUNT, "异常的攻击次数.");
            log.info("[作弊] " + player.getName() + " 魔法攻击次数异常  解析攻击次数:" + attack.numDamage + " 实际次数:" + attackCount + " 解析怪物数量:" + attack.numAttacked + " 实际怪物数量:" + mobCount);
            if (ServerConstants.isShowGMMessage()) {
                WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + player.getName() + " ID: " + player.getId() + " (等级 " + player.getLevel() + ") 魔法攻击次数异常。attack.hits " + attack.numDamage + " attackCount " + attackCount + " attack.targets " + attack.numAttacked + " MobCount " + mobCount + " 技能ID " + attack.skillId));
            }
            return;
        }
        if (attack.numDamage > 0 && attack.numAttacked > 0) {
            if (!player.getStat().checkEquipDurabilitys(player, -1)) { //i guess this is how it works ?
                player.dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
                return;
            }
        }
        if (SkillConstants.isMulungSkill(attack.skillId)) {
            if (player.getMapId() / 10000 != 92502) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Mu Lung dojo skill out of dojo maps.");
                return;
            } else {
                if (player.getMulungEnergy() < 10000) {
                    return;
                }
                player.mulung_EnergyModify(false);
            }
        } else if (SkillConstants.isPyramidSkill(attack.skillId)) {
            if (player.getMapId() / 1000000 != 926) {
                //AutobanManager.getInstance().autoban(player.getClient(), "Using Pyramid skill outside of pyramid maps.");
                return;
            } else {
                if (player.getPyramidSubway() == null || !player.getPyramidSubway().onSkillUse(player)) {
                    return;
                }
            }
        } else if (SkillConstants.isInflationSkill(attack.skillId)) {
            if (player.getBuffedValue(MapleBuffStat.巨人药水) == null) {
                return;
            }
        }
        if (player.isAdmin()) {
            player.dropMessage(-1, "攻击动作: " + Integer.toHexString(attack.display));
        }

        long maxDamagePerHit = player.getMaxDamageOver(attack.skillId);
        long totDamageToOneMonster, fixeddmg;
        long totDamage = 0;
        MapleMonsterStats monsterstats;
        Skill eaterSkill = SkillFactory.getSkill(SkillConstants.getMPEaterForJob(player.getJob()));
        int eaterLevel = player.getTotalSkillLevel(eaterSkill);
        int lastKillMob = 0;
        int lastKillMobExp = 0;
        Map<Integer, Integer> killMobList = new LinkedHashMap<>();

        MapleMap map = player.getMap();
        //PVP设置
        if (attack.skillId != 2301002) {
            if (map.isPvpMap()) {
                MaplePvp.doPvP(player, map, attack, effect);
            } else if (map.isPartyPvpMap()) {
                MaplePvp.doPartyPvP(player, map, attack, effect);
            } else if (map.isGuildPvpMap()) {
                MaplePvp.doGuildPvP(player, map, attack, effect);
            }
        }
        //攻击怪物伤害处理
        for (AttackPair oned : attack.allDamage) {
            MapleMonster monster = map.getMonsterByOid(oned.objectid);
            if (monster != null && monster.getLinkCID() <= 0) {
                totDamageToOneMonster = 0;
                monsterstats = monster.getStats();
                fixeddmg = monsterstats.getFixedDamage();
                long eachd;
                for (Pair<Long, Boolean> eachde : oned.attack) {
                    eachd = eachde.left;
                    if (fixeddmg != -1) {
                        eachd = monsterstats.getOnlyNoramlAttack() ? 0 : fixeddmg; // Magic is always not a normal attack
                    } else {
                        if (player.isShowPacket() && eachd > 0) {
                            player.dropMessage(-1, "魔法攻击打怪伤害 : " + eachd + " 服务端预计伤害 : " + maxDamagePerHit + " 是否超过 : " + (eachd > maxDamagePerHit));
                        }
                        if (monsterstats.getOnlyNoramlAttack()) {
                            eachd = 0; // Magic is always not a normal attack
                        } else if (!player.isGM()) {
                            if (eachd > maxDamagePerHit) {
                                player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC, "[伤害: " + eachd + ", 预期: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]");
                                if (attack.real) { //检测是否为相同的伤害
                                    player.getCheatTracker().checkSameDamage(eachd, maxDamagePerHit);
                                }
                                if (eachd > maxDamagePerHit * 2 && attack.skillId != 夜光.绝对死亡) {
                                    String banReason = player.getName() + " 被系统封号.[异常攻击伤害值: " + eachd + ", 预计伤害: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]";
                                    if (player.getLevel() < 10 && eachd >= 10000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 20 && eachd >= 20000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 30 && eachd >= 40000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 50 && eachd >= 60000) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 70 && eachd >= 399999) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (player.getLevel() < 150 && eachd >= 599999) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    if (eachd > maxDamagePerHit * 3) {
                                        AutobanManager.getInstance().autoban(player.getClient(), banReason);
                                        return;
                                    }
                                    player.getCheatTracker().registerOffense(CheatingOffense.HIGH_DAMAGE_MAGIC_2, "[伤害: " + eachd + ", 预期: " + maxDamagePerHit + ", 怪物ID: " + monster.getId() + "] [职业: " + player.getJob() + ", 等级: " + player.getLevel() + ", 技能: " + attack.skillId + "]");
                                }
                            } else {
                                if (eachd > maxDamagePerHit) {
                                    eachd = (int) (maxDamagePerHit);
                                }
                            }
                        }
                    }
                    totDamageToOneMonster += eachd;
                }
                totDamage += totDamageToOneMonster;
                player.checkMonsterAggro(monster);
                if (SkillConstants.getAttackDelay(attack.skillId, theSkill) >= 50 && !SkillConstants.isNoDelaySkill(attack.skillId) && !SkillConstants.is不检测范围(attack.skillId) && !monster.getStats().isBoss() && player.getTruePosition().distanceSq(monster.getTruePosition()) > GameConstants.getAttackRange(effect, player.getStat().defRange)) {
                    if (player.getMapId() != 703002000) {
                        if (ServerConstants.isShowGMMessage()) {
                            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + player.getName() + " ID: " + player.getId() + " (等级 " + player.getLevel() + ") 攻击范围异常。职业: " + player.getJob() + " 技能: " + attack.skillId + " [范围: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + " 预期: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + "]"));
                        }
                        player.getCheatTracker().registerOffense(CheatingOffense.ATTACK_FARAWAY_MONSTER, "[范围: " + player.getTruePosition().distanceSq(monster.getTruePosition()) + ", 预期范围: " + GameConstants.getAttackRange(effect, player.getStat().defRange) + " ] [职业: " + player.getJob() + " 技能: " + attack.skillId + " ]"); // , Double.toString(Math.sqrt(distance))
                    }
                }
                if (attack.skillId == 主教.群体治愈 && !monsterstats.getUndead()) {
                    player.getCheatTracker().registerOffense(CheatingOffense.HEAL_ATTACKING_UNDEAD);
                    return;
                }
                if (totDamageToOneMonster > 0) {
                    boolean killmob = monster.damage(player, totDamageToOneMonster, true, attack.skillId);
                    if (killmob) {
                        killMobList.put(monster.getObjectId(), monster.getMobExpFromChannel());
                        lastKillMob = monster.getObjectId();
                        lastKillMobExp = monster.getMobExpFromChannel();
                        afterKillMonster(player, monster);
                    }
                    if (monster.isBuffed(MonsterStatus.MOB_STAT_MCounter)) {
                        player.addHP(-(7000 + Randomizer.nextInt(8000)));
                    }
                    if (player.getBuffedValue(MapleBuffStat.缓速术) != null) {
                        MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.缓速术);
                        if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.MOB_STAT_Speed)) {
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Speed, eff.getX(), eff.getSourceid(), null, false, 0), false, eff.getY() * 1000, true, eff);
                        }
                    }
                    if (player.getBuffedValue(MapleBuffStat.炎术引燃) != null) {
                        MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.炎术引燃);
                        if (eff != null && eff.makeChanceResult() && !monster.isBuffed(MonsterStatus.MOB_STAT_Poison)) {
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, 1, eff.getSourceid(), null, false, 0), true, eff.getDOTTime(), true, eff);
                        }
                    }
                    onAttack(player, monster, attack.skillId, totDamage, oned);

                    //给怪物BUFF状态效果
                    if (effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false, 0), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                    //魔力吸收效果处理
                    if (eaterLevel > 0) {
                        eaterSkill.getEffect(eaterLevel).applyPassive(player, monster);
                    }
                } else {
                    if (attack.skillId == 夜光.闪爆光柱 && effect != null && effect.getMonsterStati().size() > 0) {
                        if (effect.makeChanceResult()) {
                            for (Entry<MonsterStatus, Integer> z : effect.getMonsterStati().entrySet()) {
                                monster.applyStatus(player, new MonsterStatusEffect(z.getKey(), z.getValue(), theSkill.getId(), null, false, 0), effect.isPoison(), effect.getDuration(), true, effect);
                            }
                        }
                    }
                }
            }
        }
        if (attack.skillId != 主教.群体治愈) {
            effect.applyTo(player);
        }
        //攻击怪物时的一些特殊处理
        if (totDamage > 1) {
            afterAttack(player, attack.numAttacked, attack.numDamage, attack.skillId, false);
        }
        if (!killMobList.isEmpty() && lastKillMob > 0) {
            handleKillMobs(player, killMobList.size(), lastKillMob, lastKillMobExp);
        }
        if (totDamage > 1 && SkillConstants.getAttackDelay(attack.skillId, theSkill) >= 100) {
            CheatTracker tracker = player.getCheatTracker();
            tracker.setAttacksWithoutHit(true);
            if (tracker.getAttacksWithoutHit() >= 50) {
                tracker.registerOffense(CheatingOffense.ATTACK_WITHOUT_GETTING_HIT, "无敌自动封号.");
            }
        }

        if (attack.skillId == 主教.光芒飞箭 && player.getParty() != null) {
            for (MaplePartyCharacter pc : player.getParty().getMembers()) {
                if (pc != null && pc.getMapid() == player.getMapId() && pc.getChannel() == player.getClient().getChannel()) {
                    MapleCharacter other = player.getClient().getChannelServer().getPlayerStorage().getCharacterByName(pc.getName());
                    if (other != null) {
                        other.addHP((int) (player.getStat().getCurrentMaxHp() * SkillFactory.getSkill(主教.光芒飞箭).getEffect(player.getSkillLevel(主教.光芒飞箭)).getX() / 100.0D * 10));
                    }
                }
            }
        }
        if (attack.skillId == 主教.天堂之门) {
            MapleStatEffect tmpEffect = SkillFactory.getSkill(主教.天堂之门).getEffect(player.getSkillLevel(主教.天堂之门));
            tmpEffect.applyTo(player);
        }
    }

    /*
     * 爆击伤害
     */
    public static AttackInfo Modify_AttackCrit(AttackInfo attack, MapleCharacter chr, int type, MapleStatEffect effect) {
        if (attack.skillId == 侠盗.金钱炸弹) {
            return attack;
        }

        int criticalRate = chr.getStat().passive_sharpeye_rate() + (effect == null ? 0 : effect.getCritical());
        int critStorage = chr.getBuffedIntValue(MapleBuffStat.暴击蓄能);
        if (critStorage > 0) {
            criticalRate += critStorage;
            if (!attack.allDamage.isEmpty()) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.暴击蓄能);
            }
        }
        boolean shadow = chr.getBuffedValue(MapleBuffStat.影分身) != null && (type == 1 || type == 2);
        List<Long> damages = new ArrayList<Long>(), damage = new ArrayList<Long>();
        int hit, toCrit, mid_att;
        for (AttackPair pair : attack.allDamage) {
            if (pair.attack != null) {
                hit = 0;
                mid_att = shadow ? (pair.attack.size() / 2) : pair.attack.size();
                toCrit = attack.skillId == 侠盗.暗杀_1 || attack.skillId == 箭神.一击要害箭 || attack.skillId == 双弩.闪电刀刃 || attack.skillId == 双刀.暗影飞跃斩 || attack.skillId == 双刀.地狱锁链 || attack.skillId == 战神.巨熊咆哮 ? mid_att : 0;
                if (toCrit == 0) {
                    for (Pair<Long, Boolean> eachd : pair.attack) {
                        if (!eachd.right && hit < mid_att) {
                            if (eachd.left > 999999 || Randomizer.nextInt(100) < criticalRate) {
                                toCrit++;
                            }
                            damage.add(eachd.left);
                        }
                        hit++;
                    }
                    if (toCrit == 0) {
                        damage.clear();
                        continue;
                    }
                    Collections.sort(damage);
                    for (int i = damage.size(); i > damage.size() - toCrit; i--) {
                        damages.add(damage.get(i - 1));
                    }
                    damage.clear();
                }
                hit = 0;
                for (Pair<Long, Boolean> eachd : pair.attack) {
                    if (!eachd.right) {
                        if (attack.skillId == 侠盗.暗杀) {
                            eachd.right = hit == 3;
                        } else if (attack.skillId == 箭神.一击要害箭 || attack.skillId == 双弩.闪电刀刃 || attack.skillId == 战神.巨熊咆哮 || attack.skillId == 双刀.暗影飞跃斩 || attack.skillId == 双刀.地狱锁链 || eachd.left > 999999) { //snipe always crit
                            eachd.right = true;
                        } else if (hit >= mid_att) {
                            eachd.right = pair.attack.get(hit - mid_att).right;
                        } else {
                            eachd.right = damages.contains(eachd.left);
                        }
                    }
                    hit++;
                }
                damages.clear();
            }
        }
        return attack;
    }

    /*
     * 解析魔法攻击
     */
    public static AttackInfo parseMagicDamage(LittleEndianAccessor lea, MapleCharacter chr) {
        AttackInfo ai = new AttackInfo();
        ai.isMagicAttack = true; //设置该攻击为魔法攻击
        lea.skip(1);
        ai.numAttackedAndDamage = lea.readByte();
        ai.numAttacked = (byte) ((ai.numAttackedAndDamage >>> 4) & 0xF);
        ai.numDamage = (byte) (ai.numAttackedAndDamage & 0xF);
        ai.skillId = lea.readInt(); //技能ID
        ai.skllv = lea.readByte();
        lea.skip(16);
        ai.charge = SkillConstants.isMagicChargeSkill(ai.skillId) ? lea.readInt() : -1;
        lea.skip(1);
        switch (ai.skillId) {
            case 龙神.雷电俯冲:
            case 龙神.雷电俯冲_攻击: {
                lea.skip(4);
            }
        }
        ai.unk = lea.readByte();
        ai.display = lea.readByte(); //动作
        ai.direction = lea.readByte(); //方向
        lea.skip(4); // big bang
        lea.skip(1); // Weapon class
        switch (ai.skillId) {
            case 炎术士.轨道烈焰_LINK:
            case 炎术士.轨道烈焰II_LINK:
            case 炎术士.轨道烈焰III_LINK:
            case 炎术士.轨道烈焰IV_LINK:
            case 龙神.狂风之环:
            case 龙神.巨龙迅捷:
            case 龙神.巨龙迅捷_2:
            case 龙神.巨龙迅捷_3:
            case 龙神.雷电之环:
            case 龙神.巨龙俯冲:
            case 龙神.巨龙俯冲_攻击:
            case 龙神.巨龙吐息:
            case 龙神.大地吐息:
            case 龙神.大地之环:
            case 龙神.元素爆破:
                lea.skip(1);
                break;
        }
        ai.speed = lea.readByte(); //攻击速度
        ai.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //0
        switch (ai.skillId) {
            case 炎术士.轨道烈焰_LINK:
            case 炎术士.轨道烈焰II_LINK:
            case 炎术士.轨道烈焰III_LINK:
            case 炎术士.轨道烈焰IV_LINK:
            case 80001762: {
                lea.skip(4);
            }
        }
        int oid;
        long damage;
        List<Pair<Long, Boolean>> allDamageNumbers;
        ai.allDamage = new ArrayList<>();
        boolean isOutput = false;
        long maxDamagePerHit = chr.getMaxDamageOver(ai.skillId);
        for (int i = 0; i < ai.numAttacked; i++) {
            oid = lea.readInt(); //怪物ID
            ai.ef = lea.readByte();
            byte by4 = lea.readByte();
            byte by5 = lea.readByte();
            short s2 = lea.readShort();
            lea.skip(4);
            lea.skip(1);
            lea.skip(4);
            lea.skip(4);
            if (ai.skillId == 阴阳师.朱玉的咒印) {
                lea.skip(1);
                ai.numDamage = lea.readByte();
            } else {
                if (lea.readByte() > 0) {
                    lea.skip(1);
                }
                lea.skip(1);
            }
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ai.numDamage; j++) {
                damage = lea.readLong(); //打怪伤害
                if (chr.isShowPacket()) {
                    chr.dropMessage(-5, "魔法攻击 - 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage);
                }
                if (damage > maxDamagePerHit * 1.5 || damage < 0 || oid <= 0) {
                    if (chr.isAdmin()) {
                        chr.dropMessage(-5, "魔法攻击出错次数: 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 默认上限: " + maxDamagePerHit);
                    }
                    if (ServerConstants.isShowGMMessage()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chr.getName() + " ID: " + chr.getId() + " (等级 " + chr.getLevel() + ") 魔法攻击伤害异常。打怪伤害: " + damage + " 地图ID: " + chr.getMapId()));
                    }
                    if (!isOutput) {
                        isOutput = true;
                        log.error("魔法攻击出错封包:  打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 技能ID: " + ai.skillId + " 默认上限: " + maxDamagePerHit + lea.toString(true));
                    }
                }
                allDamageNumbers.add(new Pair<>(damage, false));
            }
            int ksPsychicObjectId = -1;
            if (ai.skillId == 超能力者.心魂粉碎 || ai.skillId == 超能力者.心魂粉碎2 || ai.skillId == 超能力者.终极_心魂弹) {
                ksPsychicObjectId = lea.readInt();
                lea.skip(9);
            }
            if (ksPsychicObjectId >= 0) {
                ai.allDamage.add(new AttackPair(oid, ksPsychicObjectId, allDamageNumbers));
            } else {
                ai.allDamage.add(new AttackPair(oid, allDamageNumbers));
            }
            lea.skip(18);

        }
        ai.position = lea.readPos();
        return ai;
    }

    /**
     * 解析近距离攻击封包
     *
     * @param lea
     * @param chr
     * @param energy
     * @return
     */
    public static AttackInfo parseCloseRangeAttack(LittleEndianAccessor lea, MapleCharacter chr, boolean energy) {
        AttackInfo ai = new AttackInfo();
        ai.isCloseRangeAttack = true; //设置该攻击为近距离攻击
        lea.skip(1); //00
        ai.numAttackedAndDamage = lea.readByte();
        ai.numAttacked = (byte) ((ai.numAttackedAndDamage >>> 4) & 0xF); //攻击怪物数
        ai.numDamage = (byte) (ai.numAttackedAndDamage & 0xF); //攻击次数
        ai.skillId = lea.readInt(); //技能ID
        Skill skill = SkillFactory.getSkill(ai.skillId);
        ai.skllv = lea.readByte();
//        if (ai.getSkillId() == 0) {
//            parseNormalAttack(lea, ai, chr);
//        } else {
//            parseMeleeAttack(lea, ai, chr, energy);
//        }
//        if (!SkillConstants.ge(ai.skillId) && (!energy || ai.skillId == 神之子.进阶圆月旋风_吸收)) {
//            lea.skip(1);
//        }
//        lea.skip(4);
//        lea.skip(1);
//        ai.cashSlot = lea.readShort();
//        lea.skip(4);
//        ai.charge = ai.skillId == 侠盗.潜影杀机 || ai.skillId == 魂骑士.冥河破_爆破 || skill != null && skill.isChargeSkill() && ai.skillId != 双刀.阿修罗 && ai.skillId != 尖兵.超能光束炮 && ai.skillId != 400051006 ? lea.readInt() : 0;

        switch (ai.skillId) {
            case 战神.抗压:
            case 唤灵斗师.黑暗闪电:
            case 火毒.快速移动精通:
            case 冰雷.快速移动精通:
            case 主教.快速移动精通:
            case 双刀.阿修罗:
            case 冰雷.寒冰步:
            case 品克缤.品克缤之品格:
                lea.skip(4);
                break;
            default:
                if (SkillConstants.isInflationSkill(ai.skillId) || energy) {
                    lea.skip(4);
                } else {
                    lea.skip(5);
                }
                break;
        }
        lea.skip(1);
        ai.cashSlot = lea.readShort();
        lea.skip(4);
        switch (ai.skillId) {
            case 冰雷.闪电矛:
            case 黑骑士.拉曼查之枪:
            case 魂骑士.冥河破:
            case 魂骑士.冥河破_爆破:
            case 侠盗.潜影杀机:
            case 双刀.终极斩:
            case 神炮王.猴子炸药桶:
            case 神炮王.猴子炸药桶_爆炸:
            case 恶魔猎手.恶魔镰刀:
            case 恶魔猎手.灵魂吞噬:
            case 恶魔猎手.恶魔呼吸:
            case 幻影.蓝光连击:
            case 幻影.卡片风暴:
            case 夜光.超级光谱:
            case 夜光.虚空重压:
            case 夜光.晨星坠落:
//            case 狂龙战士.扇击:
//            case 狂龙战士.扇击_1:
//            case 狂龙战士.扇击_变身:
//            case 狂龙战士.扇击_变身_2:
            case 爆莉萌天使.超级诺巴:
            case 爆莉萌天使.灵魂共鸣:
            case 恶魔复仇者.暗影蝙蝠:
            case 恶魔复仇者.活力吞噬:
            case 尖兵.原子推进器:
            case 尖兵.刀锋之舞:
            case 神之子.圆月旋风:
            case 神之子.进阶圆月旋风:
            case 神之子.极速切割_漩涡:
            case 神之子.暴风制动_旋风:
            case 神之子.进阶暴风旋涡_旋涡:
            case 林之灵.生鲜龙卷风:
            case 林之灵.旋风飞行:
            case 隐月.招魂之幕:
            case 隐月.精灵化身:
            case 龙的传人.飞龙在天:
            case 龙的传人.龙魂流星拳:
            case 机械师.集中射击_SPLASH_F:
            case 剑豪.神速无双:
            case 阴阳师.猩猩火酒:
            case 品克缤.骨碌骨碌:
            case 品克缤.飞天跳跳杆:
            case 炎术士.龙奴:
            case 战神.加速终端_恐惧风暴:
            case 战神.加速终端_恐惧风暴_2:
            case 战神.加速终端_恐惧风暴_3:
            case 战神.加速终端_瞄准猎物:
            case 战神.加速终端_瞄准猎物_2:
            case 夜行者.影缝之术:
            case 龙神.龙神_2:
                //case 炎术士.龙奴_最后一击:
                ai.charge = lea.readInt();
                break;
            default:
                ai.charge = !energy && skill != null && skill.isChargeSkill() && ai.skillId != 双刀.阿修罗 && ai.skillId != 尖兵.超能光束炮 && ai.skillId != 400051006 ? lea.readInt() : 0;
                break;
        }
        if (SkillConstants.isSkip4CloseAttack(ai.skillId) || ai.skillId == 5300007 || ai.skillId == 27120211 || ai.skillId == 14111023 || ai.skillId == 400031010 || ai.skillId == 风灵使者.呼啸暴风 || ai.skillId == 风灵使者.呼啸暴风_1) {
            lea.skip(4);
        }
        //神之子这个地方需要多1个
        if (JobConstants.is神之子(chr.getJob()) && ai.skillId >= 100000000) {
            ai.zeroUnk = lea.readByte();
        }
        if (SkillConstants.isSkip4Skill(ai.skillId) || ai.skillId == 400031010) {
            lea.skip(4);
        }
        lea.skip(6);
        ai.unk = lea.readByte();
        ai.display = lea.readByte(); //动作
        ai.direction = lea.readByte(); //方向
//        if (chr.getCygnusBless()) { //精灵的祝福
//            lea.skip(12); //3个相同的Int 
//        }
        lea.skip(4); // big bang
        lea.skip(1); // Weapon class
        ai.speed = lea.readByte(); //攻击速度
        ai.lastAttackTickCount = lea.readInt(); // Ticks
        if (ai.skillId == 豹弩游侠.辅助打猎单元 || ai.skillId == 豹弩游侠.集束箭) {
            lea.skip(4);
        }
        lea.skip(4); //四个00
        if (!energy && lea.readInt() > 0) {
            lea.skip(1);
        }
        if (ai.skillId == 5111009) {
            lea.skip(1);
        }
        switch (ai.skillId) {
            case 夜行者.黑暗预兆:
                lea.skip(4);

        }
        if (ai.skillId == 隐月.招魂之幕) {
            lea.skip(4);
        }
        ai.allDamage = new ArrayList<>();
        int oid;
        long damage;
        List<Pair<Long, Boolean>> allDamageNumbers;
        long maxDamagePerHit = chr.getMaxDamageOver(ai.skillId);
        boolean isOutput = false;
        for (int i = 0; i < ai.numAttacked; i++) {
            oid = lea.readInt(); // 怪物编号
            ai.ef = lea.readByte();
            lea.skip(19); //V.112修改 以前19
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ai.numDamage; j++) {
                damage = lea.readLong(); //打怪的伤害
                if (damage > maxDamagePerHit * 1.5 || damage < 0 || oid <= 0) {
                    if (chr.isShowPacket()) {
                        chr.dropDebugMessage(2, "[近距离攻击] 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 默认上限: " + maxDamagePerHit);
                    }
                    if (ServerConstants.isShowGMMessage()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chr.getName() + " ID: " + chr.getId() + " (等级 " + chr.getLevel() + ") 近距离攻击伤害异常。打怪伤害: " + damage + " 地图ID: " + chr.getMapId()));
                    }
                    if (!isOutput) {
                        isOutput = true;
                        log.error("近距离攻击出错封包: 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 技能ID: " + ai.skillId + " 默认上限: " + maxDamagePerHit + lea.toString(true));
                    }
                }
                allDamageNumbers.add(new Pair<>(damage, false));
            }
//            chr.dropMessage(6, "暴击次数：" + critNum);
            lea.skip(18);
            ai.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        if (lea.available() >= 4) {
            ai.position = lea.readPos();
        }
        return ai;
    }

//    private static void parseNormalAttack(LittleEndianAccessor lea, AttackInfo ai, MapleCharacter chr) {
//        lea.skip(1); // bAddAttackProc
//        lea.skip(4); // crc
//
//        lea.skip(1);
//
//        lea.skip(1);
//        ai.display = lea.readShort();
//        lea.skip(4);
//        lea.skip(1);
//        ai.speed = lea.readByte();
//
//        ai.lastAttackTickCount = lea.readInt();
//
//        lea.skip(4);
//        lea.skip(4); // final attack
//        lea.skip(2); // slot
//        lea.skip(2); // csstar
//    }
//
//    private static void parseMeleeAttack(LittleEndianAccessor lea, AttackInfo ai, MapleCharacter chr, boolean energy) {
//
//    }

    /*
     * 解析远距离攻击
     */
    public static AttackInfo parseRangedAttack(LittleEndianAccessor lea, MapleCharacter chr) {
        //00 01 03 22 0A 51 00 07 00 B8 13 46 93 00 00 00 EB 02 39 2D B5 8C 0C 08 5A 52 B2 04 00 00 00 00 00 00 00 00 00 E0 00 4F 01
        AttackInfo ai = new AttackInfo();
        ai.isRangedAttack = true; //设置该攻击为远距离攻击
        boolean b = lea.readByte() == 1;
        lea.skip(1);
        ai.numAttackedAndDamage = lea.readByte();
        ai.numAttacked = (byte) ((ai.numAttackedAndDamage >>> 4) & 0xF); //攻击怪物数
        ai.numDamage = (byte) (ai.numAttackedAndDamage & 0xF); //攻击次数
        ai.skillId = lea.readInt(); //技能ID
        Skill skill = SkillFactory.getSkill(ai.skillId);
        ai.skllv = lea.readByte();
        lea.skip(6);
        lea.readShort();
        lea.skip(4);
        ai.charge = skill != null && skill.isChargeSkill() ? lea.readInt() : 0;
        //神之子这个地方需要多1个
        if (JobConstants.is神之子(chr.getJob()) && ai.skillId >= 100000000) {
            ai.zeroUnk = lea.readByte();
        }
        if (b) {
            lea.skip(4);
        }
        if (SkillConstants.isSkip4Skill(ai.skillId)) {
            lea.skip(4);
        }
        lea.skip(11);
        ai.unk = lea.readByte();
        ai.display = lea.readByte(); //动作
        ai.direction = lea.readByte(); //方向
        if (b) {
            lea.skip(4);
        }
        lea.skip(4); // big bang
        lea.skip(1); // Weapon class
        switch (ai.skillId) {
            case 双弩.飞叶龙卷风:
            case 尖兵.战斗切换_分裂:
            case 80001915:
                lea.skip(12);
                break;
        }
//        if (chr.getCygnusBless()) { //精灵的祝福
//            lea.skip(12); //3个相同的Int 
//        }
        ai.speed = lea.readByte(); // 攻击速度
        ai.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(4); //0
        ai.starSlot = lea.readShort(); //消耗飞镖 子弹等等在消耗栏的位置
//        ai.cashSlot = lea.readShort(); //飞镖 子弹等等的商城外形
        ai.AOE = lea.readByte(); // is AOE or not, TT/ Avenger = 41, Showdown = 0
        lea.skip(8);

        long damage;
        int oid;
        List<Pair<Long, Boolean>> allDamageNumbers;
        ai.allDamage = new ArrayList<>();
        boolean isOutput = false;
        long maxDamagePerHit = chr.getMaxDamageOver(ai.skillId);
        for (int i = 0; i < ai.numAttacked; i++) {
            oid = lea.readInt();
            ai.ef = lea.readByte();
            lea.skip(19); //V.112修改 以前19
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ai.numDamage; j++) {
                damage = lea.readLong();
                if (damage > maxDamagePerHit * 1.5 || damage < 0 || oid <= 0) {
                    if (chr.isShowPacket()) {
                        chr.dropDebugMessage(2, "[远距离攻击] 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 默认上限: " + maxDamagePerHit);
                    }
                    if (ServerConstants.isShowGMMessage()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chr.getName() + " ID: " + chr.getId() + " (等级 " + chr.getLevel() + ") 远距离攻击伤害异常。打怪伤害: " + damage + " 地图ID: " + chr.getMapId()));
                    }
                    if (!isOutput) {
                        isOutput = true;
                        log.error("远距离攻击出错封包: 打怪数量: " + ai.numAttacked + " 打怪次数: " + ai.numDamage + " 怪物ID " + oid + " 伤害: " + damage + " 技能ID: " + ai.skillId + " 默认上限: " + maxDamagePerHit + lea.toString(true));
                    }
                }
                allDamageNumbers.add(new Pair<>(damage, false));
            }
            lea.skip(18);
            ai.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        ai.position = lea.readPos(); //角色坐标
        if (lea.available() >= 4) {
            ai.skillposition = lea.readPos(); //技能坐标
        }
        return ai;
    }

    /*
     * 解析金钱炸弹攻击
     */
    public static AttackInfo parseMesoExplosion(LittleEndianAccessor lea, AttackInfo ret, MapleCharacter chr) {
        //System.out.println(lea.toString(true));
        byte bullets;
        if (ret.numDamage == 0) { //金钱炸弹攻击怪物为0
            lea.skip(4); //角色坐标
            bullets = lea.readByte();
            for (int j = 0; j < bullets; j++) {
                int mesoid = lea.readInt();
                lea.skip(2); //00 00
                if (chr.isShowPacket()) {
                    chr.dropDebugMessage(1, "[技能信息] 金钱炸弹攻击怪物: 无怪 " + ret.numDamage + " 金币ID: " + mesoid);
                }
                ret.allDamage.add(new AttackPair(mesoid, null));
            }
            lea.skip(2); // 63 02 [00] 0个怪物
            return ret;
        }
        int oid;
        List<Pair<Long, Boolean>> allDamageNumbers;
        for (int i = 0; i < ret.numAttacked; i++) { //金钱炸弹攻击怪物大于0
            oid = lea.readInt();
            lea.skip(19); //以前是16
            bullets = lea.readByte();
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < bullets; j++) {
                long damage = lea.readInt();
                if (chr.isShowPacket()) {
                    chr.dropDebugMessage(1, "[技能信息] 金钱炸弹攻击怪物: " + ret.numAttacked + " 攻击次数: " + bullets + " 打怪伤害: " + damage);
                }
                allDamageNumbers.add(new Pair<>(damage, false)); //m.e. never crits
            }
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
            lea.skip(8);
        }
        lea.skip(4); //角色坐标
        bullets = lea.readByte();
        for (int j = 0; j < bullets; j++) {
            int mesoid = lea.readInt();
            lea.skip(2); //01 00 
            if (chr.isShowPacket()) {
                chr.dropDebugMessage(1, "[技能信息] 金钱炸弹攻击怪物: 有怪 " + bullets + " 金币ID: " + mesoid);
            }
            ret.allDamage.add(new AttackPair(mesoid, null));
        }
        //lea.skip(2);  // 8F 02/ 63 02
        return ret;
    }

    /*
     * 解析炎术士魔法攻击
     */
    public static AttackInfo parseWarLockMagicDamage(LittleEndianAccessor lea, MapleCharacter chr) {
        AttackInfo ret = new AttackInfo();
        ret.isMagicAttack = true; //设置该攻击为魔法攻击
        lea.skip(13);
        ret.numAttackedAndDamage = lea.readByte();
        ret.numAttacked = (byte) ((ret.numAttackedAndDamage >>> 4) & 0xF);
        ret.numDamage = (byte) (ret.numAttackedAndDamage & 0xF);
        ret.skillId = lea.readInt(); //技能ID
        lea.skip(13);
        if (SkillConstants.isMagicChargeSkill(ret.skillId)) {
            ret.charge = lea.readInt();
        } else {
            ret.charge = -1;
        }
        lea.skip(6); // T071新增 未知
        ret.unk = lea.readByte();
        ret.display = lea.readByte(); //动作
        ret.direction = lea.readByte(); //方向
        lea.skip(4); // big bang
        lea.skip(1); // Weapon class
//        if (chr.getCygnusBless()) { //精灵的祝福
//            lea.skip(12); //3个相同的Int 
//        }
        ret.speed = lea.readByte(); //攻击速度
        ret.lastAttackTickCount = lea.readInt(); // Ticks
        lea.skip(8); //0
        int oid;
        long damage;
        List<Pair<Long, Boolean>> allDamageNumbers;
        ret.allDamage = new ArrayList<>();
        for (int i = 0; i < ret.numAttacked; i++) {
            oid = lea.readInt(); //怪物ID
            lea.skip(20); //V.112修改 以前19
            allDamageNumbers = new ArrayList<>();
            for (int j = 0; j < ret.numDamage; j++) {
                damage = lea.readLong(); //打怪伤害
                allDamageNumbers.add(new Pair<>(damage, false));
            }
            lea.skip(10);
            ret.allDamage.add(new AttackPair(oid, allDamageNumbers));
        }
        ret.position = lea.readPos();
        return ret;
    }


    public static boolean applyAttackCooldown(MapleStatEffect effect, MapleCharacter chr, int skillid, boolean isChargeSkill, boolean isBuff, boolean energy) {
        int cooldownTime = effect.getCooldown(chr);
        if (cooldownTime > 0) {
            if (chr.skillisCooling(skillid) && !isChargeSkill && !isBuff && !SkillConstants.isNoDelaySkill(skillid)) {
                chr.dropMessage(5, "技能由于冷却时间限制，暂时无法使用。");
                chr.send(MaplePacketCreator.enableActions());
                return false;
            } else {
                if (chr.isAdmin() || energy) {
                    if (isBuff) {
                        chr.dropDebugMessage(2, "[技能冷却] 为GM消除技能冷却时间, 原技能冷却时间:" + cooldownTime + "秒");
                    }
                } else {
                    chr.addCooldown(skillid, System.currentTimeMillis(), cooldownTime * 1000);
                    chr.send(MaplePacketCreator.skillCooldown(SkillConstants.getLinkedAttackSkill(skillid), cooldownTime));
                }
            }
        }
        return true;
    }


    /**
     * 角色攻击怪物时的HP,MP属性处理，如吸收HP
     *
     * @param player    玩家对象
     * @param monster   怪物对象
     * @param skillid   技能ID
     * @param totDamage 受到的伤害
     */
    public static void onAttack(final MapleCharacter player, final MapleMonster monster, int skillid, long totDamage, AttackPair oned) {
        final PlayerStats stats = player.getStat();
        final PlayerSpecialStats specialStats = player.getSpecialStat();
        int job = player.getJob();
        int moboid = monster.getObjectId();
        long maxhp = monster.getMobMaxHp();
        Point point = monster.getPosition();
        /*参数传递到当前怪物的伤害*/
        player.setTotDamageToMob(totDamage);

        //处理技能方面
        Integer value = player.getBuffedValue(MapleBuffStat.生命吸收);
        if (value != null && totDamage > 0) {
            /*使用将给敌人造成的伤害的一部分转化为HP的增益。最多恢复角色最大HP的20%，只有在#c斗气点数在30以上#时才能使用。*/
            /*消耗斗气点数#comboConAran，在#time秒内将伤害的#x%转化为HP*/
            MapleStatEffect effect = player.getStatForBuff(MapleBuffStat.生命吸收);
            double maxhp_per = 50;
            switch (player.getBuffSource(MapleBuffStat.生命吸收)) {
                case 恶魔猎手.吸血鬼之触:
                    int currentdate = Integer.valueOf(DateUtil.getCurrentDate("ddHHmmss"));
                    if (value + effect.getY() > currentdate) {
                        break;
                    } else {
                        maxhp_per = effect.getW();
                        player.getBuffStatValueHolder(MapleBuffStat.生命吸收).value = currentdate;
                    }
                default:
                    player.addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), ((double) stats.getMaxHp() / 100.0) * maxhp_per))));
                    break;
            }
        }

        /*攻击时，在短时间内按照一定比率，把给敌人造成的伤害吸收，并生成保护膜。不可叠加，且每次攻击时重新生成保护膜。*/
        /*攻击时，在#time秒内，把对敌人造成的伤害的#y%转换为保护自己的保护膜。保护膜可以吸收所受伤害的#x%，可吸收的最大伤害量为最大体力的#z%*/
        if (job == 321 || job == 322) {
            Optional.ofNullable(SkillFactory.getSkill(箭神.伤害置换)).ifPresent(skill -> {
                if (player.getTotalSkillLevel(skill) > 0) {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));
                    effect.applyTo(player, true);
                }
            });
        } else if (job == 212 || job == 222 || job == 232) {
            int[] skillIds = {火毒.神秘瞄准术, 冰雷.神秘瞄准术, 主教.神秘瞄准术};
            for (int i : skillIds) {
                Skill skill = SkillFactory.getSkill(i);
                if (player.getTotalSkillLevel(skill) > 0) {
                    MapleStatEffect venomEffect = skill.getEffect(player.getTotalSkillLevel(skill));
                    if (venomEffect.makeChanceResult() && player.getAllLinkMid().size() < venomEffect.getY()) {
                        player.setLinkMid(moboid, venomEffect.getX());
                        venomEffect.applyTo(player);
                    }
                    break;
                }
            }
            if (skillid == 400021002) {
                Optional.ofNullable(SkillFactory.getSkill(400020002)).ifPresent(skill -> {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));
                    monster.getMap().spawnMist(new MapleMist(effect.calculateBoundingBox(monster.getTruePosition(), player.isFacingLeft()), player, effect, monster.getPosition()), effect.getDuration(), false);
                });
            }
        } else if (player.getBuffSource(MapleBuffStat.守护模式变更) == 林之灵.巨熊模式) {
            Optional.ofNullable(SkillFactory.getSkill(林之灵.拉伊之皮_强化)).ifPresent(skill -> {
                if (player.getTotalSkillLevel(skill) > 0) {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));
                    if (effect != null && effect.makeChanceResult()) {
                        if (totDamage > 0) {
                            player.addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 10))));
                        }
                    }
                }
            });
        } else if (JobConstants.is侠盗(job)) {
            int s = player.getTotalSkillLevel(侠盗.名流爆击) > 0 ? 侠盗.名流爆击 : 侠盗.暴击蓄能;
            Optional.ofNullable(SkillFactory.getSkill(s)).ifPresent(skill -> {
                final MapleStatEffect eff = skill.getEffect(player.getTotalSkillLevel(skill));
                if (player.getTotalSkillLevel(skill) > 0) {
                    int critical = Math.min(100, (eff.getX() + player.getCriticalGrowth()));
                    player.setCriticalGrowth((critical > 0 && critical <= 100) ? critical : 0);
                    eff.applyTo(player);
                }
            });

                /* 在一定时间内攻击敌人时掉落金币。技能等级越高，掉落的金币越多。使用一次会激活，再使用一次就会关闭效果的#cON/OFF技能 */
            if (player.getBuffedValue(MapleBuffStat.敛财术) != null) {
                    /* 以下技能在攻击时，将会掉落金币 */
                switch (skillid) {
                    case 0:
                    case 飞侠.二连击:
                    case 侠盗.回旋斩:
                    case 侠盗.炼狱:
                    case 侠盗.刀刃之舞:
                    case 侠盗.突然袭击:
                    case 侠盗.一出双击:
                        int maxmeso = player.getBuffedValue(MapleBuffStat.敛财术);
                        Optional.ofNullable(SkillFactory.getSkill(侠盗.敛财术)).ifPresent(skill1 -> {
                            MapleStatEffect effect = skill1.getEffect(player.getTotalSkillLevel(侠盗.敛财术));
                            for (Pair<Long, Boolean> eachde : oned.attack) {
                                long num = eachde.left;
                                if (player.getStat().pickRate >= 100 || Randomizer.nextInt(99) < player.getStat().pickRate) {
                                    player.getMap().spawnMesoDrop(Math.min((int) Math.max(((double) num / (double) 20000) * maxmeso, 1), maxmeso), new Point((int) (monster.getTruePosition().getX() + Randomizer.nextInt(100) - 50), (int) (monster.getTruePosition().getY())), monster, player, false, (byte) 0);
                                    player.setBuffedValue(MapleBuffStat.敛财术, Math.min(effect.getY(), player.getBuffedIntValue(MapleBuffStat.敛财术) + 1));
                                    effect.applyTo(player, true);
                                }
                            }
                        });
                        break;
                }
            }

            player.handleKillSpreeGain();

        } else if (JobConstants.is恶魔猎手(job)) {
            player.handleForceGain(moboid, skillid);
        } else if (JobConstants.is夜行者(job)) {
            //存在影子蝙蝠技能Buff时才触法该效果
            if (player.getBuffedValue(MapleBuffStat.影子蝙蝠) != null) {
                if (skillid == 夜行者.双飞斩 || skillid == 夜行者.三连环光击破 || skillid == 夜行者.四连镖 || skillid == 夜行者.五倍投掷) {
                    player.handle影子蝙蝠(moboid);
                    //这里处理攻击蝙蝠分裂目标!!!
                    player.handle影子分裂(moboid, point);
                }
            }
        } else if (JobConstants.is幻影(job)) {
            if (skillid != 幻影.黑色秘卡 && skillid != 幻影.卡片雪舞) {
                player.handleCarteGain(moboid, false);
            }
        } else if (JobConstants.is隐月(job)) {
            Optional.ofNullable(SkillFactory.getSkill(隐月.精灵凝聚第1招)).ifPresent(skill -> {
                if (player.getTotalSkillLevel(skill) > 0) {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(skill));
                    if (totDamage > 0) {
                        player.addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                    }
                }
            });
        } else if (JobConstants.is尖兵(job)) {
            player.addPowerCount(1); //打怪必须是开启后才能获得能量
            if (monster.isAlive() && monster.getTriangulation() >= 3 && monster.isBuffed(MonsterStatus.MOB_STAT_Explosion)) {
                monster.setTriangulation(0);
                monster.cancelStatus(MonsterStatus.MOB_STAT_Explosion);
                monster.cancelStatus(MonsterStatus.MOB_STAT_EVA);
                monster.cancelStatus(MonsterStatus.MOB_STAT_Blind);
                player.send(ForcePacket.UserExplosionAttack(monster));
                return;
            }
            if (player.getSkillLevel(尖兵.三角进攻) > 0 && monster.isAlive()) {
                Optional.ofNullable(SkillFactory.getSkill(尖兵.三角进攻)).ifPresent(skill -> {
                    MapleStatEffect effect = skill.getEffect(player.getSkillLevel(skill));
                    if (player.getLastComboTime() + (long) (effect.getY() * 1000) < System.currentTimeMillis()) {
                        monster.setTriangulation(0);
                    }
                    if (effect.makeChanceResult()) {
                        player.setLastComboTime(System.currentTimeMillis());
                        if (monster.getTriangulation() < 3) {
                            monster.setTriangulation(monster.getTriangulation() + 1);
                            List<MonsterStatusEffect> arrayList = new ArrayList<>();
                            arrayList.add(new MonsterStatusEffect(MonsterStatus.MOB_STAT_EVA, (-monster.getTriangulation()) * effect.getX(), effect.getSourceid(), null, false));
                            arrayList.add(new MonsterStatusEffect(MonsterStatus.MOB_STAT_Blind, monster.getTriangulation() * effect.getX(), effect.getSourceid(), null, false));
                            arrayList.add(new MonsterStatusEffect(MonsterStatus.MOB_STAT_Explosion, monster.getTriangulation(), effect.getSourceid(), null, false));
                            monster.applyStatus(player, arrayList, false, (long) (effect.getY() * 1000), true, effect);
                        }
                    }
                });
            }
        } else if (JobConstants.is恶魔复仇者(job)) {
            if (totDamage > 0) {
                Optional.ofNullable(SkillFactory.getSkill(恶魔复仇者.生命吸收)).ifPresent(skill -> {
                    int skillLevel = player.getTotalSkillLevel(skill);
                    if (skillLevel > 0) {
                        MapleStatEffect effect = skill.getEffect(skillLevel);
                        if (effect != null && effect.makeChanceResult()) {
                            int hpheal = (int) (stats.getCurrentMaxHp() * (effect.getX() / 100.0));
                            if (player.isShowPacket()) {
                                player.dropDebugMessage(1, "[恶魔复仇者] 攻击恢复Hp " + hpheal);
                            }
                            player.addHP(hpheal);
                        }
                    }
                });
            }
        } else if (JobConstants.is唤灵斗师(job)) {
            player.handleDeathPact(moboid, true);
        } else if (JobConstants.is神射手(job)) {
            if (skillid != 95001000) {
                //50% 的概率吸血 20%
                if (player.getBuffedValue(MapleBuffStat.三彩箭矢) != null && specialStats.getArrowsMode() == 0 && Randomizer.nextInt(100) <= 50) {
                    if (totDamage > 0) {
                        player.addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) 20 / 100.0)), stats.getMaxHp() / 2))));
                    }
                }
                player.handleArrowsCharge(moboid);
            }
        } else if (JobConstants.is超能力者(player.getJob())) {
            if (SkillConstants.isKSTelekinesisSkill(skillid)) {
                player.handlerKSTelekinesis(moboid);
            }
        } else if (JobConstants.is奇袭者(player.getJob())) {
            player.handle元素雷电(skillid);
//            player.handleHeartMineUnity();
        } else if (JobConstants.is圣骑士(player.getJob())) {
            player.handle元素冲击(skillid);
        } else if (JobConstants.is风灵使者(player.getJob())) {
            if (skillid != 风灵使者.狂风肆虐Ⅰ && skillid != 风灵使者.狂风肆虐Ⅱ && skillid != 风灵使者.狂风肆虐Ⅲ) {
                //player.handle狂风肆虐();
                player.handle暴风灭世(monster.getObjectId());
            }
        } else if (skillid == 爆莉萌天使.灵魂吸取_攻击) {
            player.handle灵魂吸取(monster.getObjectId());
        }
        if (skillid > 0) {
            Optional.ofNullable(SkillFactory.getSkill(skillid)).ifPresent(skil -> {
                MapleStatEffect effect = skil.getEffect(player.getTotalSkillLevel(skil));
                switch (skillid) {
                    case 1078: //伊卡尔特的吸血 - 伊卡尔特召唤吸血鬼后，对多个敌人造成4连击，并吸收部分伤害恢复HP。一次不可吸收超过角色最大HP1/2以上，也不可超过怪物的最大HP。特定等级提升时，技能等级可以提升1。
                    case 恶魔猎手.血腥渡鸦:
                    case 豹弩游侠.利爪狂风: {
                        if (totDamage > 0) {
                            player.addHP(((int) Math.min(maxhp, Math.min(((int) ((double) totDamage * (double) effect.getX() / 100.0)), stats.getMaxHp() / 2))));
                        }
                        break;
                    }
                    case 船长.无尽追击: {
                        player.setLinkMid(moboid, effect.getX());
                        break;
                    }
                    case 双刀.流云斩:
                    case 双刀.双刀风暴:
                    case 双刀.血雨腥风:
                    case 双刀.悬浮地刺:
                    case 双刀.暗影飞跃斩:
                    case 双刀.终极斩:
                    case 双刀.暴怒刀阵:
                    case 双刀.地狱锁链:
                    case 双刀.幽灵一击:
                    case 飞侠.二连击:
                    case 飞侠.双飞斩:
                    case 侠盗.回旋斩:
                    case 侠盗.炼狱:
                    case 侠盗.刀刃之舞:
                    case 侠盗.暗杀:
                    case 侠盗.一出双击:
                    case 侠盗.突然袭击:
                    case 隐士.爆裂飞镖:
                    case 隐士.风之护符:
                    case 隐士.三连环光击破:
                    case 隐士.影子分裂:
                    case 隐士.四连镖:
                    case 夜行者.武器用毒液:
                    case 夜行者.多重飞镖:
                    case 夜行者.四连射: {
                        int[] skills = {侠盗.武器用毒液, 隐士.武器用毒液, 双刀.武器用毒液};
                        int[] skillss = {侠盗.致命毒液, 隐士.致命毒液, 双刀.致命毒液};
                        int skillS = 0;
                        for (int i : skillss) {
                            if (player.getTotalSkillLevel(i) > 0) {
                                skillS = i;
                                break;
                            }
                        }
                        for (int i : skills) {
                            int finalSkillS = skillS;
                            Skill skill = SkillFactory.getSkill(i);
                            if (player.getTotalSkillLevel(skill) > 0) {
                                MapleStatEffect venomEffect = skill.getEffect(player.getTotalSkillLevel(skill));
                                if (player.getTotalSkillLevel(finalSkillS) > 0) {//致命毒液
                                    Optional.ofNullable(SkillFactory.getSkill(finalSkillS)).ifPresent(skill1 -> {
                                        MapleStatEffect effect1 = skill1.getEffect(player.getTotalSkillLevel(skill1));
                                        int szie = 0;
                                        for (MonsterStatusEffect s : monster.getAllBuffs()) {
                                            if (s.getSkill() == finalSkillS) {
                                                szie++;
                                            }
                                        }

                                        if (effect1.makeChanceResult()) {
                                            if (szie < effect1.getDOTStack() + 1) {
                                                monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, effect1.getDOT(), finalSkillS, null, false), true, effect1.getDuration(), true, effect1);
                                            }
                                        }
                                    });
                                } else {
                                    if (venomEffect.makeChanceResult()) {
                                        monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, 1, i, null, false), true, venomEffect.getDuration(), true, venomEffect);
                                    }
                                }
                                break;
                            }
                        }
                        break;
                    }
                    case 侠盗.神通术: { // 神通术
                        monster.handleSteal(player);
                        break;
                    }
                    case 夜行者.双飞斩:
                    case 夜行者.三连环光击破:
                    case 夜行者.三连环光击破_最后一击:
                    case 夜行者.四连镖:
                    case 夜行者.四连镖_最后一击:
                    case 夜行者.五倍投掷:
                    case 夜行者.五倍投掷_最后一击: {
                        if (player.getBuffedValue(MapleBuffStat.元素黑暗) != null) {
                            MapleStatEffect eff = player.getStatForBuff(MapleBuffStat.元素黑暗);
                            monster.applyStatus(player, new MonsterStatusEffect(MonsterStatus.MOB_STAT_ElementDarkness, 1, 夜行者.元素_黑暗, null, false, 0), true, eff.getDuration(), true, eff);
                        }
                        break;
                    }
                    case 冰雷.冰河锁链:
                        monster.setTempEffectiveness(Element.冰, effect.getDuration());
                        break;
                    case 火毒.迷雾爆发:
                        monster.setTempEffectiveness(Element.火, effect.getDuration());
                        break;
                }
            });
        }
    }

    /**
     * 处理攻击怪物触发
     */
    public static void afterAttack(MapleCharacter player, int mobCount, int attackCount, int skillid, boolean isCritDamage) {
        final PlayerStats stats = player.getStat();
        final PlayerSpecialStats specialStats = player.getSpecialStat();
        int job = player.getJob();
        /*HP恢复机率*/
        if (stats.hpRecoverProp > 0 && Randomizer.nextInt(100) <= stats.hpRecoverProp) {
            if (stats.hpRecover > 0) {
                player.healHP(stats.hpRecover);
            }
            /*HP恢复百分比*/
            if (stats.hpRecover_Percent > 0) {
                /*角色增加HP*/
                player.addHP((int) ((double) stats.getCurrentMaxHp() * (double) stats.hpRecover_Percent / 100.0) * mobCount);
            }
        }
        /*MP恢复机率*/
        if (stats.mpRecoverProp > 0 && !JobConstants.isNotMpJob(job) && Randomizer.nextInt(100) <= stats.mpRecoverProp) {
            if (stats.mpRecover > 0) {
                player.healMP(stats.mpRecover);
            }
        }

        switch (job) {
            case 422: {
                //处理激活刀飞幸运钱的再次使用
                if (isCritDamage && player.getSkillLevel(侠盗.幸运钱) > 0) {
                    player.switchLuckyMoney(true);
                }
            }
            case 510:
            case 511:
            case 512:
                if (!specialStats.isEnergyFull()) {
                    player.handleEnergyCharge(mobCount * 2);
                } else {
                    player.handleEnergyConsume(mobCount, skillid);
                }
                break;
            case 110:
            case 111:
            case 112:
            case 2411: //添加幻影职业 幻影复制技能后有这个
            case 2412:
                if (skillid != 英雄.虎咆哮 & player.getBuffedValue(MapleBuffStat.斗气集中) != null) {
                    player.handleOrbgain(false);
                }
                break;
            case 6100:
            case 6110:
            case 6111:
            case 6112:
                int amon = 0;
                switch (skillid) {
                    case 狂龙战士.扇击:
                    case 狂龙战士.扇击_变身:
                        amon = 1;
                        break;
                    case 狂龙战士.飞龙斩_2:
                    case 狂龙战士.烈火箭:
                        amon = 2;
                        break;
                    case 狂龙战士.冲击波:
                    case 狂龙战士.穿刺冲击:
                        amon = 3;
                        break;
                    case 狂龙战士.牵引锁链:
                    case 狂龙战士.怒雷屠龙斩:
                    case 狂龙战士.天空剑影:
                    case 狂龙战士.剑气突袭:
                        amon = 5;
                        break;
                    case 狂龙战士.剑刃之壁:
                    case 狂龙战士.进阶剑刃之壁:
                        amon = 20;
                        break;
                    case 狂龙战士.恶魔之息:
                        amon = 40;
                        break;
                }
                if (amon > 0) {
                    player.handleMorphCharge(amon);
                }
                break;
            case 312: {
                if (player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) == 神射手.箭雨) {
                    Optional.ofNullable(SkillFactory.getSkill(神射手.箭雨)).ifPresent(skill -> {
                        int totalSkillLevel = player.getTotalSkillLevel(神射手.箭雨);
                        MapleStatEffect effect = skill.getEffect(totalSkillLevel);
                        if (totalSkillLevel > 0 && effect != null && JobConstants.is神射手(skillid / 10000) && System.currentTimeMillis() >= player.getLastUseVSkillTime()) {
                            player.setLastUseVSkillTime(System.currentTimeMillis() + (long) (effect.getX() * 1000));
                            player.send(MaplePacketCreator.userBonusAttackRequest(神射手.箭雨_1, 0, Collections.emptyList()));
                        }
                    });
                }
                break;
            }
            case 1112: {
                if (player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) == 魂骑士.天人之舞) {
                    Optional.ofNullable(SkillFactory.getSkill(魂骑士.天人之舞)).ifPresent(skill -> {
                        int totalSkillLevel = player.getTotalSkillLevel(魂骑士.天人之舞);
                        MapleStatEffect effect = skill.getEffect(totalSkillLevel);
                        int value = player.getBuffedIntValue(MapleBuffStat.月光转换);
                        if (totalSkillLevel > 0 && effect != null && JobConstants.is魂骑士(skillid / 10000) && System.currentTimeMillis() >= player.getLastUseVSkillTime()) {
                            player.setLastUseVSkillTime(System.currentTimeMillis() + (long) (effect.getX() * 1000));
                            player.send(MaplePacketCreator.userBonusAttackRequest(value == 1 ? 魂骑士.天人之舞_1 : 魂骑士.天人之舞_2, 0, Collections.emptyList()));
                        }
                    });
                }
                break;
            }
            case 2112: {
                if (player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) == 战神.装备摩诃) {
                    Optional.ofNullable(SkillFactory.getSkill(战神.装备摩诃)).ifPresent(skill -> {
                        int totalSkillLevel = player.getTotalSkillLevel(战神.装备摩诃);
                        MapleStatEffect effect = skill.getEffect(totalSkillLevel);
                        if (totalSkillLevel > 0 && effect != null && JobConstants.is战神(skillid / 10000) && System.currentTimeMillis() >= player.getLastUseVSkillTime()) {
                            player.setLastUseVSkillTime(System.currentTimeMillis() + (long) (effect.getX() * 1000));
                            player.send(MaplePacketCreator.userBonusAttackRequest(战神.装备摩诃_1, 0, Collections.emptyList()));
                        }
                    });
                }
                break;
            }
            case 2312: {
                if (player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) == 双弩.精灵元素) {
                    Optional.ofNullable(SkillFactory.getSkill(双弩.元素幽灵_3)).ifPresent(skill -> {
                        int totalSkillLevel = player.getTotalSkillLevel(双弩.精灵元素);
                        MapleStatEffect effect = skill.getEffect(totalSkillLevel);
                        if (totalSkillLevel > 0 && effect != null && JobConstants.is双弩精灵(skillid / 10000) && System.currentTimeMillis() >= player.getLastUseVSkillTime()) {
                            player.setLastUseVSkillTime(System.currentTimeMillis() + (long) (effect.getX() * 1000));
                            player.send(MaplePacketCreator.userBonusAttackRequest(双弩.元素幽灵_3, 0, Collections.emptyList()));
                        }
                    });
                }
                break;
            }
            case 6500:
            case 6510:
            case 6511:
            case 6512: {
//                ArrayList<Integer> monsterids = new ArrayList<>();
//                Skill skill = SkillFactory.getSkill(skillid);
//                MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(SkillConstants.getLinkedAttackSkill(skillid)));
//                if (skill != null && effect != null && skillid != 爆莉萌天使.灵魂吸取_攻击 && skillid != 爆莉萌天使.灵魂吸取专家_1 && l3 - player.getstifftime() > 1500 && player.getStatForBuff(MapleBuffStat.灵魂吸取专家) != null && (effect = player.getStatForBuff(MapleBuffStat.灵魂吸取专家)).lH()) {
//                    player.setstifftime(l3);
//                    List<MapleMapObject> monstersInRange = player.getMap().getMonstersInRange(player.getTruePosition(), 100000.0);
//                    if (!monstersInRange.isEmpty()) {
//                        monstersInRange.forEach(mapleMapObject -> {
//                            if (mapleMapObject != null && !((MapleMonster) mapleMapObject).getStats().isFriendly()) {
//                                monsterids.add(mapleMapObject.getObjectId());
//                            }
//                        });
//                        new MapleForce(player.getId(), skillid, 0, MapleForceType.)
//                        player.send_other(i.b.x.a(player, 0, monsterids, 1, effect.kA(), 爆莉萌天使.灵魂吸取专家_1, 300, false), true);
//                        player.sendEnableActions();
//                    }
//                }
//                MapleStatEffect statForBuff = player.getStatForBuff(MapleBuffStat.伤害置换);
//                if (statForBuff != null) {
//                    long l4 = (long) player.getBuffedValue(MapleBuffStat.伤害置换) + l2 * (long)statForBuff.getY() / 100;
//                    player.setBuffedValue(MapleBuffStat.伤害置换, (int)Math.min(Math.max(0, l4), (long)Math.min(99999, player.getStat().getMaxHp())));
//                    player.updateBuffEffect(statForBuff, Collections.singletonMap(MapleBuffStat.伤害置换, (int)l4));
//                }
                if (skillid == 爆莉萌天使.爱星能量) {
                    Skill skill;
                    MapleStatEffect effect;
                    if ((skill = SkillFactory.getSkill(爆莉萌天使.爱星能量)) != null
                            && (effect = skill.getEffect(player.getTotalSkillLevel(skill))) != null) {
                        player.cancelEffectFromBuffStat(MapleBuffStat.爱星能量);
                        player.handle无敌(effect, effect.getZ());
                    }
                }
                break;
            }
            case 522:
                Integer value = player.getBuffedValue(MapleBuffStat.神速衔接);
                if (value == null) {
                    int skilllevel = player.getTotalSkillLevel(船长.神速衔接);
                    Optional.ofNullable(SkillFactory.getSkill(船长.神速衔接)).ifPresent(skill -> {
                        MapleStatEffect effect = skill.getEffect(skilllevel);
                        if (Randomizer.nextInt(100) < effect.getProp()) {
                            effect.applyTo(player);
                        }
                    });
                } else if (value > 1) {
                    player.cancelBuffStats(MapleBuffStat.神速衔接);
                }
                break;
            default:
                if (!player.isIntern()) {
                    player.cancelEffectFromBuffStat(MapleBuffStat.潜入状态);
                    MapleStatEffect ds = player.getStatForBuff(MapleBuffStat.隐身术);
                    if (ds != null) {
                        if (ds.getSourceid() != 双刀.进阶隐身术 || !ds.makeChanceResult()) {
                            player.cancelEffectFromBuffStat(MapleBuffStat.隐身术);
                        }
                    }
                }
                if (JobConstants.is尖兵(job)) {
                    if (player.getBuffedValue(MapleBuffStat.精准火箭) != null) {
                        player.handleCardStack(skillid);
                    }
                } else if (JobConstants.is风灵使者(job)) {
                    if (player.getBuffedValue(MapleBuffStat.狂风肆虐) != null) {
                        player.handle狂风肆虐(skillid);
                    }
                } else if (JobConstants.is夜光(job)) {
                    player.handleLuminous(skillid); //夜光光暗点数处理
                    if (player.getJob() == 2712) {
                        player.handleDarkCrescendo(); //夜光黑暗高潮处理
                    }
                    player.handleBlackBless(); //夜光黑暗祝福处理
                } else if (JobConstants.is恶魔复仇者(job)) {
                    player.handle超越状态(skillid);
                }
        }

        if (JobConstants.is战士(player.getJob()) && player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) > 0 && player.getBuffSource(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm) == 战士.灵气武器) {
            Optional.ofNullable(SkillFactory.getSkill(战士.灵气武器)).ifPresent(skill -> {
                int totalSkillLevel = player.getTotalSkillLevel(战士.灵气武器);
                MapleStatEffect effect = skill.getEffect(totalSkillLevel);
                if (totalSkillLevel > 0 && effect != null && System.currentTimeMillis() >= player.getLastUseVSkillTime()) {
                    player.setLastUseVSkillTime(System.currentTimeMillis() + (long) (effect.getZ() * 1000));
                    player.send(MaplePacketCreator.userBonusAttackRequest(战士.灵气武器_1, 0, Collections.emptyList()));
                }
            });
        }
        if (skillid == 箭神.真一击要害箭 && JobConstants.is箭神(player.getJob()) && player.getBuffSource(MapleBuffStat.SECONDARY_STAT_CursorSniping) == 箭神.真狙击) {
            int value = Math.max(0, player.getBuffedIntValue(MapleBuffStat.SECONDARY_STAT_CursorSniping) - 1);
            if (value > 0) {
                player.setBuffedValue(MapleBuffStat.SECONDARY_STAT_CursorSniping, value);
                player.updateBuffEffect(player.getStatForBuff(MapleBuffStat.SECONDARY_STAT_CursorSniping), Collections.singletonMap(MapleBuffStat.SECONDARY_STAT_CursorSniping, value));
            } else {
                Optional.ofNullable(SkillFactory.getSkill(箭神.真狙击)).ifPresent(skill -> {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(箭神.真狙击));
                    player.cancelEffectFromBuffStat(MapleBuffStat.SECONDARY_STAT_CursorSniping);
                    player.handle无敌(effect, 2000);
                });
            }
        }
    }

    /**
     * 处理角色连续击杀怪物
     */
    public static void handleKillMobs(MapleCharacter player, int kill, int moboid, int mobexp) {
        int mobKills = player.getMobKills();
        long lastMobKillTime = player.getLastMobKillTime();
        //初始化连续击杀
        if (lastMobKillTime <= 0 || lastMobKillTime + 10000 < System.currentTimeMillis()) {
            mobKills = 0;
        }
        //开始处理连续击杀
        int totalexp;
        if (kill >= 3) {
            totalexp = (int) ((mobexp / 100.0) * kill);
            totalexp = totalexp < 1 ? 1 : totalexp;
            player.gainExp(totalexp, false, false, false);
            player.getClient().announce(MaplePacketCreator.showContinuityKill(true, totalexp, kill, moboid));
        }
        mobKills += kill;
        if (mobKills > 1) {
            totalexp = (int) ((mobexp / 1000.0) * (mobKills >= 300 ? 300 : mobKills));
            totalexp = totalexp < 1 ? 1 : totalexp;
            player.gainExp(totalexp, false, false, false);
            player.getClient().announce(MaplePacketCreator.showContinuityKill(false, totalexp, mobKills, moboid));
        }
        lastMobKillTime = System.currentTimeMillis();
        player.setMobKills(mobKills);
        player.setLastMobKillTime(lastMobKillTime);
    }

    public static void afterKillMonster(MapleCharacter player, MapleMonster monster) {
        if (JobConstants.is剑豪(player.getJob())) {
            player.getSpecialStat().gainJianQi(player.hasBuffSkill(剑豪.拔刀姿势) ? 2 : 1);
            player.getClient().announce(MaplePacketCreator.updateJianQi(player.getSpecialStat().getJianQi()));
        } else if (JobConstants.is林之灵(player.getJob())) {
            if (player.hasBuffSkill(林之灵.嗨_兄弟)) {
                Optional.ofNullable(SkillFactory.getSkill(林之灵.嗨_兄弟)).ifPresent(skill -> {
                    MapleStatEffect effect = skill.getEffect(player.getTotalSkillLevel(林之灵.嗨_兄弟));
                    if (effect.makeChanceResult()) {
                        effect.applySummonEffect(player, false, monster.getTruePosition(), effect.getX(), 0);
                    }
                });
            }
        } else if (JobConstants.is火毒(player.getJob())) {
//            if (player.hasBuffSkill(火毒.燎原之火)) {
//                MapleStatEffect effect = SkillFactory.getSkill(火毒.燎原之火_MIST).getEffect(player.getTotalSkillLevel(火毒.燎原之火));
//                Point point = monster.getPosition();
//                point.y += 50;
//                Rectangle bounds = effect.calculateBoundingBox(point, false);
//                player.getClient().announce(BuffPacket.showFirewall(player.getMap().getSpawnedMistOnMap(), 火毒.燎原之火_MIST, bounds, 0x16));
//            }
        }
    }
}
