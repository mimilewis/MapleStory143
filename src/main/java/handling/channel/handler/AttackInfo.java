package handling.channel.handler;

import client.MapleCharacter;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.SkillConstants;
import constants.skills.新手;
import server.MapleStatEffect;
import server.movement.LifeMovementFragment;
import tools.AttackPair;

import java.awt.*;
import java.util.List;

public class AttackInfo {

    public int skillId, charge, lastAttackTickCount;
    public List<AttackPair> allDamage;
    public Point position; //角色的坐标
    public Point skillposition = null; //技能的坐标
    public int display;  //动作
    public int direction; //方向
    public int stance; //姿势
    public int maxDamageOver = 2100000000;  //最大攻击上限 默认: 999999
    public short starSlot, cashSlot; //飞镖子弹在背包的位置 现金道具飞镖子弹在背包的位置
    public byte numDamage, numAttacked, numAttackedAndDamage, speed, AOE, unk, zeroUnk, ef, skllv;
    public List<LifeMovementFragment> movei;
    public boolean real = true, move = false;
    public boolean isCloseRangeAttack = false; //是否近离攻击
    public boolean isRangedAttack = false; //是否远距离攻击
    public boolean isMagicAttack = false; //是否魔法攻击

    public int getSkillId() {
        return skillId;
    }

    public int getCharge() {
        return charge;
    }

    public int getLastAttackTickCount() {
        return lastAttackTickCount;
    }

    public List<AttackPair> getAllDamage() {
        return allDamage;
    }

    public Point getPosition() {
        return position;
    }

    public Point getSkillposition() {
        return skillposition;
    }

    public int getDisplay() {
        return display;
    }

    public int getDirection() {
        return direction;
    }

    public int getStance() {
        return stance;
    }

    public int getMaxDamageOver() {
        return maxDamageOver;
    }

    public short getStarSlot() {
        return starSlot;
    }

    public short getCashSlot() {
        return cashSlot;
    }

    public byte getNumDamage() {
        return numDamage;
    }

    public byte getNumAttacked() {
        return numAttacked;
    }

    public byte getNumAttackedAndDamage() {
        return numAttackedAndDamage;
    }

    public byte getSpeed() {
        return speed;
    }

    public byte getAOE() {
        return AOE;
    }

    public byte getUnk() {
        return unk;
    }

    public byte getZeroUnk() {
        return zeroUnk;
    }

    public byte getEf() {
        return ef;
    }

    public byte getSkllv() {
        return skllv;
    }

    public List<LifeMovementFragment> getMovei() {
        return movei;
    }

    public boolean isReal() {
        return real;
    }

    public boolean isMove() {
        return move;
    }

    public boolean isCloseRangeAttack() {
        return isCloseRangeAttack;
    }

    public boolean isRangedAttack() {
        return isRangedAttack;
    }

    public boolean isMagicAttack() {
        return isMagicAttack;
    }

    public MapleStatEffect getAttackEffect(MapleCharacter chr, int skillLevel, Skill theSkill) {
        if (SkillConstants.isMulungSkill(skillId) || SkillConstants.isPyramidSkill(skillId) || SkillConstants.isInflationSkill(skillId) || skillId == 新手.升级特效 || SkillConstants.is品克缤技能(skillId)) {
            skillLevel = 1;
        } else if (skillLevel <= 0) {
            return null;
        }
        if (SkillConstants.isLinkedAttackSkill(skillId)) {
            Skill skillLink = SkillFactory.getSkill(skillId);
            return skillLink.getEffect(skillLevel);
        }
        return theSkill.getEffect(skillLevel);
    }
}
