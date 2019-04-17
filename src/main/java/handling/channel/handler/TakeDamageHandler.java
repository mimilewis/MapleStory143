/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.PlayerStats;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.ItemConstants;
import constants.JobConstants;
import constants.skills.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleInventoryManipulator;
import server.MapleStatEffect;
import server.life.MapleLifeFactory.loseItem;
import server.life.MapleMonster;
import server.life.MobAttackInfo;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.packet.BuffPacket;

import java.awt.*;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author admin
 */
public class TakeDamageHandler {

    private static final Logger log = LogManager.getLogger(TakeDamageHandler.class.getName());

    public static void TakeDamage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        /*
         * 如果角色为空 或者 角色地图为空 或者角色是隐身的 或者角色是GM且角色处于无敌状态 就返回
         */
        if (chr == null || chr.getMap() == null || chr.isHidden() || (chr.isGM() && chr.isInvincible())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        slea.readInt();
        chr.updateTick(slea.readInt());
        byte type = slea.readByte(); //-4 为烟雾造成, -3 和 -2 为地图造成的伤害.
        slea.skip(1); // Element - 0x00 = elementless, 0x01 = ice, 0x02 = fire, 0x03 = lightning
        int damage = slea.readInt(); //受伤数值
        slea.skip(6);
        boolean isDeadlyAttack = false;
        boolean pPhysical = false;
        int oid = 0;
        int monsteridfrom = 0;
        int fake = 0;
        int mpattack = 0;
        byte direction = 0;
        int skillId = 0;
        int pOid = 0;
        int pDamage = 0;
        byte pType = 0;
        byte defType = 0;
        Point pPos = new Point(0, 0);
        MapleMonster attacker = null;
        PlayerStats stats = chr.getStat();
        if (chr.isShowPacket()) {
            chr.dropDebugMessage(1, "[玩家受伤] 受伤类型: " + type + " 受伤数值: " + damage);
        }
        switch (type) {
            case -8:
            case -5:
            case -4:
            case -3:
            case -2: {
                break;
            }
            default: {
                monsteridfrom = slea.readInt(); //怪物ID
                oid = slea.readInt(); //怪物工作ID
                attacker = chr.getMap().getMonsterByOid(oid);
                direction = slea.readByte(); // Knock direction
                List<loseItem> loseItems;

                if (attacker == null || attacker.getId() != monsteridfrom || attacker.getLinkCID() > 0 || attacker.isFake() || attacker.getStats().isFriendly()) {
                    return;
                }
                if (type != -1 && damage > 0) { // Bump damage
                    loseItems = chr.getMap().getMonsterById(monsteridfrom).getStats().loseItem();
                    if (loseItems != null) {
                        MapleInventoryType InvType;
                        final int playerpos = chr.getPosition().x;
                        byte d = 1;
                        Point pos = new Point(0, chr.getPosition().y);
                        for (loseItem loseItem : loseItems) {
                            InvType = ItemConstants.getInventoryType(loseItem.getId());
                            for (byte b = 0; b < loseItem.getX(); b++) {
                                if (Randomizer.nextInt(101) >= loseItem.getChance()) {
                                    if (chr.haveItem(loseItem.getId())) {
                                        pos.x = playerpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2)));
                                        MapleInventoryManipulator.removeById(c, InvType, loseItem.getId(), 1, false, false);
                                        chr.getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), new Item(loseItem.getId(), (byte) 0, (short) 1), chr.getMap().calcDropPos(pos, chr.getPosition()), true, true);
                                        d++;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                        chr.getMap().removeMapObject(attacker);
                    }
                    MobAttackInfo attackInfo = attacker.getStats().getMobAttack(type);
                    if (attackInfo != null) {
                        if ((attackInfo.isElement) && (stats.TER > 0) && (Randomizer.nextInt(100) < stats.TER)) {
                            System.out.println("Avoided ER from mob id: " + monsteridfrom);
                            return;
                        }
                        if (attackInfo.isDeadlyAttack()) {
                            isDeadlyAttack = true;
                            mpattack = stats.getMp() - 1;
                        } else {
                            mpattack += attackInfo.getMpBurn();
                        }
                        MobSkill skill = MobSkillFactory.getMobSkill(attackInfo.getDiseaseSkill(), attackInfo.getDiseaseLevel());
                        if (skill != null && (damage == -1 || damage > 0) && skill.getDuration() != 0) {
                            skill.applyEffect(chr, attacker, (short) 0, false);
                        }
                        attacker.setMp(attacker.getMp() - attackInfo.getMpCon());
                    }
                }
            }

            skillId = slea.readInt();
            pDamage = slea.readInt(); // 对怪物反射造成的伤害
            defType = slea.readByte();
            slea.skip(1); //[00] 未知
            if (chr.isShowPacket()) {
                chr.dropDebugMessage(1, "[玩家受伤] 受到伤害: " + damage + " 技能ID: " + skillId + " 反射伤害: " + pDamage + " defType: " + defType);
            }
            if (skillId != 0 && pDamage > 0) {
                pPhysical = slea.readByte() > 0;
                pOid = slea.readInt(); //反射伤害给怪物的工作ID
                pType = slea.readByte();
                slea.skip(4); // Mob position garbage
                pPos = slea.readPos();
            }
        }

        if (defType == 1) { // 精气防护
            Skill bx = SkillFactory.getSkill(恶魔猎手.精气防护);
            int bof = chr.getTotalSkillLevel(bx);
            if (bof > 0) {
                MapleStatEffect eff = bx.getEffect(bof);
                chr.addDemonMp(eff.getZ());
                chr.addHP((chr.getStat().getCurrentMaxHp() / 100) * eff.getY());
            }
        }
        if (damage == -1) {
            fake = 4020002 + ((chr.getJob() / 10 - 40) * 100000);
            if (fake != 隐士.假动作 && fake != 侠盗.假动作) {
                fake = 隐士.假动作;
            }
            if (type == -1) {
                if (chr.getJob() == 122 && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10) != null) {
                    if (chr.getTotalSkillLevel(圣骑士.守护之神) > 0) {
                        MapleStatEffect eff = SkillFactory.getSkill(圣骑士.守护之神).getEffect(chr.getTotalSkillLevel(圣骑士.守护之神));
                        attacker.applyStatus(chr, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Stun, 1, 圣骑士.守护之神, null, false, 0), false, eff.getDuration(), true, eff);
                        fake = 圣骑士.守护之神;
                    }
                } else if (chr.getJob() == 3611 || chr.getJob() == 3612) {
                    MapleStatEffect effect = chr.getStatForBuff(MapleBuffStat.黑暗高潮);
                    if (effect != null && effect.getSourceid() == 尖兵.双重防御) {
                        int prop = chr.getBuffedValue(MapleBuffStat.黑暗高潮); //回避的概率
                        if (prop > 0) {
                            prop -= effect.getY();
                        }
                        int z = chr.getBuffedValue(MapleBuffStat.伤害吸收); //吸收伤害多少
                        if (z > 0) {
                            z += effect.getZ();
                        }
                        int x = effect.getX();
                        x -= (effect.getProp() - prop) / effect.getY();
                        if (x > 0 && prop > 0) {
                            Map<MapleBuffStat, Integer> buffstats = new EnumMap<>(MapleBuffStat.class);
                            buffstats.put(MapleBuffStat.黑暗高潮, prop);
                            buffstats.put(MapleBuffStat.伤害吸收, z);
                            chr.setBuffedValue(MapleBuffStat.黑暗高潮, prop);
                            chr.setBuffedValue(MapleBuffStat.伤害吸收, z);
                            int duration = effect.getDuration();
                            chr.send(BuffPacket.giveBuff(effect.getSourceid(), duration, buffstats, effect, chr));
                        } else {
                            chr.dispelSkill(尖兵.双重防御);
                        }
                        fake = 尖兵.双重防御;
                    }
                } else if (JobConstants.is剑豪(chr.getJob())) {
                    int skilllevel = chr.getSkillLevel(剑豪.避柳);
                    int prop;
                    if (skilllevel > 0) {
                        MapleStatEffect eff = SkillFactory.getSkill(剑豪.避柳).getEffect(skilllevel);
                        prop = eff.getProp();
                        if (Randomizer.nextInt(100) < prop) {
                            eff.applyTo(chr);
                        }
                    }
                    skilllevel = chr.getSkillLevel(剑豪.迅速);
                    if (skilllevel > 0) {
                        MapleStatEffect eff = SkillFactory.getSkill(剑豪.迅速).getEffect(skilllevel);
                        prop = eff.getProp();
                        if (Randomizer.nextInt(100) < prop) {
                            eff.applyTo(chr);
                        }
                    }
                } else if (slea.available() == 9) {
                    slea.skip(1);
                    fake = slea.readInt();
                }
            }
            if (chr.getTotalSkillLevel(fake) <= 0) {
                if (chr.isShowPacket()) {
                    chr.dropDebugMessage(3, "[玩家受伤] 受到伤害: " + damage + " 技能ID: " + skillId + " 反射伤害: " + pDamage + " defType: " + defType);
                }
                return;
            }
        } else if (damage < -1) {
            //AutobanManager.getInstance().addPoints(c, 1000, 60000, "Taking abnormal amounts of damge from " + monsteridfrom + ": " + damage);
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
//        if (chr.getStat().dodgeChance > 0 && Randomizer.nextInt(100) < chr.getStat().dodgeChance) { //删除这个 暂时不加
//            c.announce(MaplePacketCreator.showSpecialEffect(0x16));
//            return;
//        }
        if (attacker != null && pPhysical && pDamage > 0 && chr.getTotalSkillLevel(skillId) > 0) {
            switch (skillId) {
                case 英雄.愤怒之火:
                    Skill skill = SkillFactory.getSkill(skillId);
                    MapleStatEffect eff = skill.getEffect(chr.getTotalSkillLevel(skillId));
                    long bounceDamage = Math.min((int) (damage * (eff.getY() / 100.0)), (attacker.getMobMaxHp() / 2)); //获取能够吸收的伤害 不超过怪物的最大HP的一半
                    if (chr.isShowPacket()) {
                        chr.dropDebugMessage(1, "[玩家受伤] 受到伤害 - 封包解析: " + damage + " 技能吸收倍率: " + eff.getX() / 100.0 + " 被动减少倍率: " + stats.reduceDamageRate / 100.0);
                    }
                    damage -= (damage * (eff.getX() / 100.0 + stats.reduceDamageRate / 100.0)); //最终玩家受到的伤害
                    if (damage < 1) {
                        damage = 1;
                    }
                    if (chr.isShowPacket()) {
                        chr.dropDebugMessage(1, "[玩家受伤] 受到伤害 - 减少后的伤害: " + damage + " 处理反射伤害: " + bounceDamage + " 解析反射伤害: " + pDamage + " 技能ID: " + skillId + " - " + skill.getName());
                    }
                    if (bounceDamage > pDamage) {
                        bounceDamage = pDamage;
                    }
                    attacker.damage(chr, bounceDamage, true, skillId);
                    break;
                default:
                    attacker.damage(chr, pDamage, true, skillId);
                    break;
            }
        } else if (stats.reduceDamageRate > 0) {
            damage -= damage * (stats.reduceDamageRate / 100.0);
            if (damage < 1) {
                damage = 1;
            }
        }
        MapleStatEffect.apply双重防御(chr);
        chr.getCheatTracker().checkTakeDamage(damage);
        Pair<Double, Boolean> modify = chr.modifyDamageTaken(damage, attacker);
        damage = modify.left.intValue();
        if (chr.isShowPacket()) {
            chr.dropDebugMessage(1, "[玩家伤害] 最终受到伤害 " + damage);
        }
        if (damage > 0) {
            chr.getCheatTracker().setAttacksWithoutHit(false);
            if (chr.getBuffedValue(MapleBuffStat.变身效果) != null) {
                chr.cancelMorphs();
            }
            boolean mpAttack = chr.getBuffedValue(MapleBuffStat.金属机甲) != null && chr.getBuffSource(MapleBuffStat.金属机甲) != 机械师.金属机甲_战车;
            if (chr.getBuffedValue(MapleBuffStat.魔法盾) != null) {
                int hploss = 0, mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    if (chr.getBuffedValue(MapleBuffStat.终极无限) != null) {
                        mploss = 0;
                    }
                    chr.addHPMP(-hploss, -mploss);
                } else {
                    mploss = (int) (damage * (chr.getBuffedValue(MapleBuffStat.魔法盾).doubleValue() / 100.0)) + mpattack;
                    hploss = damage - mploss;
                    if (chr.getBuffedValue(MapleBuffStat.终极无限) != null) {
                        mploss = 0;
                    } else if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    chr.addHPMP(-hploss, -mploss);
                }
            } else if (chr.getTotalSkillLevel(夜光.普通魔法防护) > 0 || chr.getTotalSkillLevel(炎术士.火焰斥力) > 0) {
                int hploss = 0, mploss = 0;
                if (isDeadlyAttack) {
                    if (stats.getHp() > 1) {
                        hploss = stats.getHp() - 1;
                    }
                    if (stats.getMp() > 1) {
                        mploss = stats.getMp() - 1;
                    }
                    chr.addHPMP(-hploss, -mploss);
                } else if (chr.getTotalSkillLevel(夜光.普通魔法防护) > 0) {
                    Skill skill = SkillFactory.getSkill(夜光.普通魔法防护);
                    MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(夜光.普通魔法防护));
                    mploss = (int) (damage * (effect.getX() / 100.0)) + mpattack;
                    hploss = damage - mploss;
                    if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    if (chr.isShowPacket()) {
                        chr.dropMessage(5, "[普通魔法防护] 受到伤害: " + damage + " 减少Hp: " + hploss + " 减少Mp: " + mploss + " 技能减少: " + (effect.getX() / 100.0));
                    }
                    chr.addHPMP(-hploss, -mploss);
                } else if (chr.getTotalSkillLevel(炎术士.火焰斥力) > 0) {
                    Skill skill = SkillFactory.getSkill(炎术士.火焰斥力);
                    MapleStatEffect effect = skill.getEffect(chr.getTotalSkillLevel(炎术士.火焰斥力));
                    mploss = (int) (damage * (effect.getX() / 100.0)) + mpattack;
                    hploss = damage - mploss;
                    if (mploss > stats.getMp()) {
                        mploss = stats.getMp();
                        hploss = damage - mploss + mpattack;
                    }
                    if (chr.isShowPacket()) {
                        chr.dropDebugMessage(1, "[火焰斥力] 受到伤害: " + damage + " 减少Hp: " + hploss + " 减少Mp: " + mploss + " 技能减少: " + (effect.getX() / 100.0));
                    }
                    chr.addHPMP(-hploss, -mploss);
                }
            } else if (JobConstants.is神之子(chr.getJob())) {
                int skilllevel = chr.getTotalSkillLevel(神之子.防御之盾);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(神之子.防御之盾).getEffect(skilllevel);
                    if (effect.makeChanceResult()) {
                        effect.applyTo(chr);
                    }
                }
            } else if (chr.getStat().mesoGuardMeso > 0) {
                if (chr.isShowPacket()) {
                    chr.dropDebugMessage(1, "[玩家受伤] 受到伤害: " + damage);
                }
                damage = (int) Math.ceil(damage * chr.getStat().mesoGuard / 100.0);
                int mesoloss = (int) (damage * (chr.getStat().mesoGuardMeso / 100.0));
                if (chr.isShowPacket()) {
                    chr.dropDebugMessage(1, "[玩家受伤] 金钱护盾 - 最终伤害: " + damage + " 减少金币: " + mesoloss);
                }
                if (chr.getMeso() < mesoloss) {
                    chr.gainMeso(-chr.getMeso(), false);
                    chr.cancelBuffStats(MapleBuffStat.金钱护盾);
                } else {
                    chr.gainMeso(-mesoloss, false);
                }
                if (isDeadlyAttack && stats.getMp() > 1) {
                    mpattack = stats.getMp() - 1;
                }
                chr.addHPMP(-damage, -mpattack);
            } else if (isDeadlyAttack) {
                chr.addHPMP(stats.getHp() > 1 ? -(stats.getHp() - 1) : 0, stats.getMp() > 1 && !mpAttack ? -(stats.getMp() - 1) : 0);
            } else {
                chr.addHPMP(-damage, mpAttack ? 0 : -mpattack);
            }
            if (chr.inPVP() && chr.getStat().getHPPercent() <= 20) {
                SkillFactory.getSkill(PlayerStats.getSkillByJob(93, chr.getJob())).getEffect(1).applyTo(chr);
            }
            if (chr.getBuffedValue(MapleBuffStat.心魂本能) != null && chr.getSpecialStat().getPP() > 0) {
                chr.gainPP(-1);
            }
        }
        byte offset = 0;
        int offset_d = 0;
        if (slea.available() == 1) {
            offset = slea.readByte();
            if (offset == 1 && slea.available() >= 4) {
                offset_d = slea.readInt();
            }
            if (offset < 0 || offset > 2) {
                offset = 0;
            }
        }
        if (chr.isShowPacket()) {
            chr.dropDebugMessage(1, "[玩家受伤] 类型: " + type + " 怪物ID: " + monsteridfrom + " 伤害: " + damage + " fake: " + fake + " direction: " + direction + " oid: " + oid + " offset: " + offset);
        }
        chr.hadnle忍耐之盾(damage);
        if (damage < -1 || damage > 200000) {
            log.error("掉血错误", "玩家[" + chr.getName() + " 职业: " + chr.getJob() + "]掉血错误 - 类型: " + type + " 怪物ID: " + monsteridfrom + " 伤害: " + damage + " fake: " + fake + " direction: " + direction + " oid: " + oid + " offset: " + offset + " 封包:" + slea.toString(true));
            return;
        }
//        c.announce(MaplePacketCreator.enableActions());
        chr.getMap().broadcastMessage(chr, MaplePacketCreator.damagePlayer(chr.getId(), type, damage, monsteridfrom, direction, skillId, pDamage, pPhysical, pOid, pType, pPos, offset, offset_d, fake), false);
    }
}
