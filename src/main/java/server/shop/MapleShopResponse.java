/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.shop;

/**
 * @author PlayDK
 */
public enum MapleShopResponse {

    购买道具完成(0x00),
    背包空间不够(0x07),
    卖出道具完成(0x08),
    充值飞镖完成(0x0E),
    充值金币不够(0x10),
    购买回购出错(0x1D);
    private final int value;

    MapleShopResponse(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
