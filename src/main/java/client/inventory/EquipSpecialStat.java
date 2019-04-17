/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 * @author PlayDK
 */
public enum EquipSpecialStat {

    总伤害(0x1),
    全属性(0x2),
    剪刀次数(0x4),
    漩涡经验(0x8), //long
    星级(0x10); //int
    private final int value;

    EquipSpecialStat(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
