/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 * @author PlayDK
 */
public enum PetFlag {

    PET_PICKUP_ITEM(0x01, 5190000, 5191000), //捡道具技能
    PET_LONG_RANGE(0x02, 5190002, 5191002), //扩大移动范围技能
    PET_DROP_SWEEP(0x04, 5190003, 5191003), //范围自动捡起功能
    PET_IGNORE_PICKUP(0x08, 5190005, -1), //不拣取特定道具技能
    PET_PICKUP_ALL(0x10, 5190004, 5191004), //捡起无所有权道具&金币技能
    PET_CONSUME_HP(0x20, 5190001, 5191001), //自动服用HP药水技能
    PET_CONSUME_MP(0x40, 5190006, -1), //自动服用MP药水技能
    PET_RECALL(0x80, 5190007, -1), //宠物召唤技能
    PET_AUTO_SPEAKING(0x100, 5190008, -1), //宠物自言自语技能
    PET_AUTO_BUFF(0x200, 5190010, -1), //宠物自动加BUFF技能
    PET_SMART(0x800, 5190011, -1); //宠物训练技能 - 训练宠物，让宠物变成智能宠物。可智能移动到主人身边捡取道具、金币，并可以使用自动喂食功能。\n#c可用于所有宠物，只限于宠物食品# 
    private final int i;
    private final int item;
    private final int remove;

    PetFlag(int i, int item, int remove) {
        this.i = i;
        this.item = item;
        this.remove = remove;
    }

    public static PetFlag getByAddId(int itemId) {
        for (PetFlag flag : PetFlag.values()) {
            if (flag.item == itemId) {
                return flag;
            }
        }
        return null;
    }

    public static PetFlag getByDelId(int itemId) {
        for (PetFlag flag : PetFlag.values()) {
            if (flag.remove == itemId) {
                return flag;
            }
        }
        return null;
    }

    public int getValue() {
        return i;
    }

    public boolean check(int flag) {
        return (flag & i) == i;
    }
}
