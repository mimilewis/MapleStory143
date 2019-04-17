/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 * @author PlayDK
 */
public enum EquipStats {

    可升级次数(0x1, 1, 1),
    已升级次数(0x2, 1, 1),
    力量(0x4, 2, 1),
    敏捷(0x8, 2, 1),
    智力(0x10, 2, 1),
    运气(0x20, 2, 1),
    Hp(0x40, 2, 1),
    Mp(0x80, 2, 1),
    物攻(0x100, 2, 1),
    魔攻(0x200, 2, 1),
    物防(0x400, 2, 1),
    魔防(0x800, 2, 1),
    命中(0x1000, 2, 1),
    回避(0x2000, 2, 1),
    手技(0x4000, 2, 1),
    速度(0x8000, 2, 1),
    跳跃(0x10000, 2, 1),
    状态(0x20000, 4, 1),
    技能(0x40000, 2, 1),
    道具等级(0x80000, 2, 1),
    道具经验(0x100000, 8, 1),
    耐久度(0x200000, 4, 1),
    金锤子(0x400000, 4, 1),
    大乱斗攻击力(0x800000, 2, 1),
    DOWNLEVEL(0x1000000, 1, 1),
    ENHANCT_BUFF(0x2000000, 2, 1),
    DURABILITY_SPECIAL(0x4000000, 4, 1),
    REQUIRED_LEVEL(0x8000000, 1, 1),
    YGGDRASIL_WISDOM(0x10000000, 1, 1),
    FINAL_STRIKE(0x20000000, 1, 1), //最终一击卷轴成功
    BOSS伤害(0x40000000, 1, 1),
    无视防御(0x80000000, 1, 1);
    private final int value, datatype, first;

    EquipStats(int value, int datatype, int first) {
        this.value = value;
        this.datatype = datatype;
        this.first = first;
    }

    public int getValue() {
        return value;
    }

    public int getDatatype() {
        return datatype;
    }

    public int getPosition() {
        return first;
    }

    public boolean check(int flag) {
        return (flag & value) != 0;
    }

    public enum EnhanctBuff {

        UPGRADE_TIER(0x1),
        NO_DESTROY(0x2),
        SCROLL_SUCCESS(0x4);
        private final int value;

        EnhanctBuff(int value) {
            this.value = value;
        }

        public byte getValue() {
            return (byte) value;
        }

        public boolean check(int flag) {
            return (flag & value) != 0;
        }
    }
}
