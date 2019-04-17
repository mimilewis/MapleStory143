/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 * 使用终极魔方对道具进行潜能
 *
 * @author admin
 */
public class ModifyItemPotential {

    private final Item newItem; //鉴定后的道具属性信息
    private final int csItemId; //使用的商城道具ID

    public ModifyItemPotential(Item item, int csItemId) {
        this.newItem = item.copy();
        this.csItemId = csItemId;
    }

    /*
     * 装备道具的位置
     */
    public short getPosition() {
        return newItem.getPosition();
    }

    /*
     * 装备道具的唯一ID
     */
    public int getEquipOnlyId() {
        return newItem.getEquipOnlyId();
    }

    /*
     * 商城道具的ID
     */
    public int getCSItemId() {
        return csItemId;
    }

    /*
     * 现在的道具ID
     */
    public Item getNewItem() {
        return newItem;
    }
}
