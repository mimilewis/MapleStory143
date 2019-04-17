/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

import constants.ItemConstants;

/**
 * @author PlayDK
 */
public class ModifyInventory {

    private final int mode;
    private Item item;
    private short oldPos;
    private short indicator;
    private boolean switchSrcDst = false;

    /*
     * 0 = 获得道具
     * 1 = 更新道具数量
     * 2 = 移动道具
     * 3 = 删除道具
     * 4 = 刷新装备经验
     * 5 = 移动道具小背包到背包
     * 6 = 小背包更新道具
     * 7 = 小背包删除道具
     * 8 = 移动位置小背包里面的道具
     * 9 = 小背包获得道具
     */
    public ModifyInventory(int mode, Item item) {
        this.mode = mode;
        this.item = item.copy();
    }

    public ModifyInventory(int mode, Item item, short oldPos) {
        this.mode = mode;
        this.item = item.copy();
        this.oldPos = oldPos;
    }

    public ModifyInventory(int mode, Item item, short oldPos, short indicator, boolean switchSrcDst) {
        this.mode = mode;
        this.item = item.copy();
        this.oldPos = oldPos;
        this.indicator = indicator;
        this.switchSrcDst = switchSrcDst;
    }

    public int getMode() {
        if ((getInventoryType() == 2 || getInventoryType() == 3 || getInventoryType() == 4) && item.getPosition() > 10000) { //其他栏目的道具
            switch (mode) {
                case 0:
                    return 9;
                case 1:
                    return 6;
                case 2:
                    return 5;
                case 3:
                    return 7;
            }
        }
        return mode;
    }

    public int getInventoryType() {
        return ItemConstants.getInventoryType(item.getItemId()).getType();
    }

    public short getPosition() {
        return item.getPosition();
    }

    public short getOldPosition() {
        return oldPos;
    }

    public short getIndicator() {
        return indicator;
    }

    public boolean switchSrcDst() {
        return switchSrcDst;
    }

    public short getQuantity() {
        return item.getQuantity();
    }

    public Item getItem() {
        return item;
    }

    public void clear() {
        this.item = null;
    }
}
