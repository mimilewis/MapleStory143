/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps.pvp;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.skills.夜光;
import handling.channel.handler.AttackInfo;
import handling.world.WorldBroadcastService;
import handling.world.WorldGuildService;
import server.MapleStatEffect;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

/**
 * @author PlayDK
 */
public class MaplePvp {

    /*
     * 解析Pvp角色的伤害
     */
    private static PvpAttackInfo parsePvpAttack(AttackInfo attack, MapleCharacter player, MapleStatEffect effect) {
        PvpAttackInfo ret = new PvpAttackInfo();
        double maxdamage = player.getLevel() + 100.0;
        int skillId = attack.skillId;
        ret.skillId = skillId;
        ret.critRate = 5; //爆击概率
        ret.ignoreDef = 0; //无视防御
        ret.skillDamage = 100; //技能攻击
        ret.mobCount = 1; //攻击角色的数量
        ret.attackCount = 1; //攻击角色的次数
        int pvpRange = attack.isCloseRangeAttack ? 35 : 70; //攻击的距离
        ret.facingLeft = attack.direction < 0;
        if (skillId != 0 && effect != null) {
            ret.critRate += effect.getCritical();
            ret.ignoreDef += effect.getIgnoreMob();
            ret.skillDamage = (effect.getDamage() + player.getStat().getDamageIncrease(skillId));
            ret.mobCount = Math.max(1, effect.getMobCount(player));
            ret.attackCount = Math.max(effect.getBulletCount(player), effect.getAttackCount(player));
            ret.box = effect.calculateBoundingBox(player.getTruePosition(), ret.facingLeft, pvpRange);
        } else {
            ret.box = calculateBoundingBox(player.getTruePosition(), ret.facingLeft, pvpRange);
        }
        boolean mirror = player.getBuffedValue(MapleBuffStat.影分身) != null || player.getBuffedIntValue(MapleBuffStat.月光转换) == 1;
        ret.attackCount *= (mirror ? 2 : 1);
        maxdamage *= ret.skillDamage / 100.0;
        ret.maxDamage = maxdamage * ret.attackCount;
        if (player.isShowPacket()) {
            player.dropSpouseMessage(0x0A, "Pvp伤害解析 - 最大攻击: " + maxdamage + " 数量: " + ret.mobCount + " 次数: " + ret.attackCount + " 爆击: " + ret.critRate + " 无视: " + ret.ignoreDef + " 技能伤害: " + ret.skillDamage);
        }
        return ret;
    }

    private static Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft, int range) {
        Point lt = new Point(-70, -30);
        Point rb = new Point(-10, 0);
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    public static boolean inArea(MapleCharacter chr) {
        for (Rectangle rect : chr.getMap().getAreas()) {
            if (rect.contains(chr.getTruePosition())) {
                return true;
            }
        }
        return false;
    }

    private static void monsterBomb(MapleCharacter player, MapleCharacter attacked, MapleMap map, PvpAttackInfo attack) {
        if (player == null || attacked == null || map == null) {
            return;
        }
        double maxDamage = attack.maxDamage;
        boolean isCritDamage = false;
        //等级压制处理
        if (player.getLevel() > attacked.getLevel() + 10) {
            maxDamage *= 1.05;
        } else if (player.getLevel() < attacked.getLevel() - 10) {
            maxDamage /= 1.05;
        } else if (player.getLevel() > attacked.getLevel() + 20) {
            maxDamage *= 1.10;
        } else if (player.getLevel() < attacked.getLevel() - 20) {
            maxDamage /= 1.10;
        } else if (player.getLevel() > attacked.getLevel() + 30) {
            maxDamage *= 1.15;
        } else if (player.getLevel() < attacked.getLevel() - 30) {
            maxDamage /= 1.15;
        }
        //爆击概率处理
        if (Randomizer.nextInt(100) < attack.critRate) {
            maxDamage *= 1.50;
            isCritDamage = true;
        }
        int attackedDamage = (int) (Math.floor(Math.random() * ((int) maxDamage * 0.35) + (int) maxDamage * 0.65));
        int MAX_PVP_DAMAGE = (int) (player.getStat().getLimitBreak(player) / 100.0); //最大Pvp伤害为角色武器突破极限的攻击除以100 也就是默认为: 9999
        int MIN_PVP_DAMAGE = 100; //最小Pvp伤害
        if (attackedDamage > MAX_PVP_DAMAGE) {
            attackedDamage = MAX_PVP_DAMAGE;
        }
        if (attackedDamage < MIN_PVP_DAMAGE) {
            attackedDamage = MIN_PVP_DAMAGE;
        }
        int hploss = attackedDamage, mploss = 0;
        if (attackedDamage > 0) {
            if (attacked.getBuffedValue(MapleBuffStat.魔法盾) != null) {
                mploss = (int) (attackedDamage * (attacked.getBuffedValue(MapleBuffStat.魔法盾).doubleValue() / 100.0));
                hploss -= mploss;
                if (attacked.getBuffedValue(MapleBuffStat.终极无限) != null) {
                    mploss = 0;
                } else if (mploss > attacked.getStat().getMp()) {
                    mploss = attacked.getStat().getMp();
                    hploss -= mploss;
                }
                attacked.addHPMP(-hploss, -mploss);
            } else if (attacked.getTotalSkillLevel(夜光.普通魔法防护) > 0) {
                Skill skill = SkillFactory.getSkill(夜光.普通魔法防护);
                MapleStatEffect effect = skill.getEffect(attacked.getTotalSkillLevel(夜光.普通魔法防护));
                mploss = (int) (attackedDamage * (effect.getX() / 100.0));
                hploss -= mploss;
                if (mploss > attacked.getStat().getMp()) {
                    mploss = attacked.getStat().getMp();
                    hploss -= mploss;
                }
                attacked.addHPMP(-hploss, -mploss);
            } else if (attacked.getStat().mesoGuardMeso > 0) {
                hploss = (int) Math.ceil(attackedDamage * attacked.getStat().mesoGuard / 100.0);
                int mesoloss = (int) (attackedDamage * (attacked.getStat().mesoGuardMeso / 100.0));
                if (attacked.getMeso() < mesoloss) {
                    attacked.gainMeso(-attacked.getMeso(), false);
                    attacked.cancelBuffStats(MapleBuffStat.金钱护盾);
                } else {
                    attacked.gainMeso(-mesoloss, false);
                }
                attacked.addHP(-hploss);
            } else {
                attacked.addHP(-hploss);
            }
        }
        MapleMonster pvpMob = MapleLifeFactory.getMonster(9400711);
        map.spawnMonsterOnGroundBelow(pvpMob, attacked.getPosition());
        map.broadcastMessage(MaplePacketCreator.damagePlayer(attacked.getId(), 2, pvpMob.getId(), hploss));
        if (isCritDamage) {
            player.dropMessage(6, "你对玩家 " + attacked.getName() + " 造成了 " + hploss + " 点爆击伤害! 对方血量: " + attacked.getStat().getHp() + "/" + attacked.getStat().getCurrentMaxHp());
            attacked.dropMessage(6, "玩家 " + player.getName() + " 对你造成了 " + hploss + " 点爆击伤害!");
        } else {
            player.dropTopMsg("你对玩家 " + attacked.getName() + " 造成了 " + hploss + " 点伤害! 对方血量: " + attacked.getStat().getHp() + "/" + attacked.getStat().getCurrentMaxHp());
            attacked.dropTopMsg("玩家 " + player.getName() + " 对你造成了 " + hploss + " 点伤害!");
        }
        map.killMonster(pvpMob, player, false, false, (byte) 1);
        //最终奖励处理
        if (attacked.getStat().getHp() <= 0 && !attacked.isAlive()) {
            int expReward = attacked.getLevel() * 10 * (attacked.getLevel() / player.getLevel());
            int gpReward = (int) (Math.floor(Math.random() * 10 + 10));
            if (player.getPvpKills() * .25 >= player.getPvpDeaths()) {
                expReward *= 2;
            }
            player.gainExp(expReward, true, false, true);
            if (player.getGuildId() > 0 && player.getGuildId() != attacked.getGuildId()) {
                WorldGuildService.getInstance().gainGP(player.getGuildId(), gpReward);
            }
            player.gainPvpKill();
            player.dropMessage(6, "你击败了玩家 " + attacked.getName() + "!! ");
            int pvpVictory = attacked.getPvpVictory();
            attacked.gainPvpDeath();
            attacked.dropMessage(6, player.getName() + " 将你击败!");
            byte[] packet = MaplePacketCreator.spouseMessage(0x0A, "[Pvp] 玩家 " + player.getName() + " 终结了 " + attacked.getName() + " 的 " + pvpVictory + " 连斩。");
            if (pvpVictory >= 5 && pvpVictory < 10) {
                map.broadcastMessage(packet);
            } else if (pvpVictory >= 10 && pvpVictory < 20) {
                player.getClient().getChannelServer().broadcastMessage(packet);
            } else if (pvpVictory >= 20) {
                WorldBroadcastService.getInstance().broadcastMessage(packet);
            }
        }
    }

    public synchronized static void doPvP(MapleCharacter player, MapleMap map, AttackInfo attack, MapleStatEffect effect) {
        PvpAttackInfo pvpAttack = parsePvpAttack(attack, player, effect);
        int mobCount = 0;
        for (MapleCharacter attacked : player.getMap().getCharactersIntersect(pvpAttack.box)) {
            if (attacked.getId() != player.getId() && attacked.isAlive() && !attacked.isHidden() && mobCount < pvpAttack.mobCount) {
                mobCount++;
                monsterBomb(player, attacked, map, pvpAttack);
            }
        }
    }

    public synchronized static void doPartyPvP(MapleCharacter player, MapleMap map, AttackInfo attack, MapleStatEffect effect) {
        PvpAttackInfo pvpAttack = parsePvpAttack(attack, player, effect);
        int mobCount = 0;
        for (MapleCharacter attacked : player.getMap().getCharactersIntersect(pvpAttack.box)) {
            if (attacked.getId() != player.getId() && attacked.isAlive() && !attacked.isHidden() && (player.getParty() == null || player.getParty() != attacked.getParty()) && mobCount < pvpAttack.mobCount) {
                mobCount++;
                monsterBomb(player, attacked, map, pvpAttack);
            }
        }
    }

    public synchronized static void doGuildPvP(MapleCharacter player, MapleMap map, AttackInfo attack, MapleStatEffect effect) {
        PvpAttackInfo pvpAttack = parsePvpAttack(attack, player, effect);
        int mobCount = 0;
        for (MapleCharacter attacked : player.getMap().getCharactersIntersect(pvpAttack.box)) {
            if (attacked.getId() != player.getId() && attacked.isAlive() && !attacked.isHidden() && (player.getGuildId() == 0 || player.getGuildId() != attacked.getGuildId()) && mobCount < pvpAttack.mobCount) {
                mobCount++;
                monsterBomb(player, attacked, map, pvpAttack);
            }
        }
    }
}
