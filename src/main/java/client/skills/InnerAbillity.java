/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.skills;

import constants.ItemConstants;
import tools.Randomizer;

/**
 * @author PlayDK
 */
public class InnerAbillity {

    private static InnerAbillity instance = null;

    public static InnerAbillity getInstance() {
        if (instance == null) {
            instance = new InnerAbillity();
        }
        return instance;
    }

    public InnerSkillEntry renewSkill(int rank, int position) {
        return renewSkill(rank, position, false);
    }

    public InnerSkillEntry renewSkill(int rank, int position, boolean ultimateCirculatorPos) {
        if (ultimateCirculatorPos) {
            int randomSkill = ItemConstants.getInnerSkillbyRank(3)[(int) Math.floor(Math.random() * ItemConstants.getInnerSkillbyRank(rank).length)];
            Skill skill = SkillFactory.getSkill(randomSkill);
            if (skill == null) {
                return null;
            }
            int skillLevel;
            int random = Randomizer.nextInt(100);
            if (random < 38) {
                skillLevel = Randomizer.rand(skill.getMaxLevel() / 2, skill.getMaxLevel());
            } else if (random < 70) {
                skillLevel = Randomizer.rand(skill.getMaxLevel() / 3, skill.getMaxLevel() / 2);
            } else {
                skillLevel = Randomizer.rand(skill.getMaxLevel() / 4, skill.getMaxLevel() / 3);
            }
            if (skillLevel > skill.getMaxLevel()) {
                skillLevel = skill.getMaxLevel();
            }
            return new InnerSkillEntry(randomSkill, skillLevel, (byte) position, (byte) 3);
        }
        int circulatorRate = 10;
        if (ultimateCirculatorPos) { //现在普通的就这2个能用
            circulatorRate = 20;
        }
        if (Randomizer.isSuccess(3 + circulatorRate)) {
            rank = 1;
        } else if (Randomizer.isSuccess(2 + circulatorRate / 5)) {
            rank = 2;
        } else if (Randomizer.isSuccess(1 + circulatorRate / 10)) {
            rank = 3;
        } else {
            rank = 0;
        }
        /*
         * 产物最高等级：S级#
         */
        int randomSkill = ItemConstants.getInnerSkillbyRank(rank)[(int) Math.floor(Math.random() * ItemConstants.getInnerSkillbyRank(rank).length)];
        Skill skill = SkillFactory.getSkill(randomSkill);
        if (skill == null) {
            return null;
        }
        int skillLevel;
        int random = Randomizer.nextInt(100);
        if (random < 3 + circulatorRate / 2) {
            skillLevel = Randomizer.rand(skill.getMaxLevel() / 2, skill.getMaxLevel());
        } else if (random < circulatorRate) {
            skillLevel = Randomizer.rand(skill.getMaxLevel() / 3, skill.getMaxLevel() / 2);
        } else {
            skillLevel = Randomizer.rand(skill.getMaxLevel() / 4, skill.getMaxLevel() / 3);
        }
        if (skillLevel > skill.getMaxLevel()) {
            skillLevel = skill.getMaxLevel();
        }
        return new InnerSkillEntry(randomSkill, skillLevel, (byte) position, (byte) rank);
    }

    /*
     * 现在只有这3个能用
     * 2701000 - 特别还原器 - 拥有神秘力量，可以重置全部“内在能力”的还原器。使用本还原器重置时，最深层的内在能力必定为S级。
     * 2702000 - 内在能力还原器 - #c双击#可以对角色的内在能力进行重新设置。内在能力等级可能提高，也可能降低。\n#c无法用于SS级内在能力\n产物最高等级：S级#
     * 2702001 - 内在能力还原器 - #c双击#可以对角色的内在能力进行重新设置。内在能力等级可能提高，也可能降低。\n#c无法用于SS级内在能力\n产物最高等级：S级#
     */
    public int getCirculatorRank(int itemId) {
        return ((itemId % 1000) / 100) + 1;
    }

    public boolean isSuccess(int rate) {
        return rate > Randomizer.nextInt(100);
    }
}
