package server.shop;

import client.inventory.Item;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MapleShopItem {

    private short buyable; //可购买的数量
    private int itemId; //物品ID
    private int price; //物品价格
    private int reqItem; //自定义购买物品需要的道具ID
    private int reqItemQ; //购买需要的道具数量
    private int period; //该道具购买后的天数
    private int state; //道具状态 1=未鉴定
    private int category; //商店道具分类
    private int minLevel; //购买道具最小等级
    @JsonIgnore
    private Item rebuy; //回购的道具信息
    private int position; //道具位置
    private boolean isRechargeableItem; //是否为飞镖、子弹
    private int pointtype;

    public MapleShopItem() {

    }

    /*
     * 回购信息相关
     * 道具信息
     * 道具价格
     * 道具数量
     */
    public MapleShopItem(Item rebuy, int price, short buyable) {
        this.buyable = buyable;
        this.itemId = rebuy.getItemId();
        this.price = price;
        this.reqItem = 0;
        this.reqItemQ = 0;
        this.period = 0;
        this.state = 0;
        this.category = 0;
        this.minLevel = 0;
        this.isRechargeableItem = false;
        this.rebuy = rebuy;
        this.pointtype = 0;
    }

    /*
     * 加载商店信息相关
     */
    public MapleShopItem(short buyable, int itemId, int price, int reqItem, int reqItemQ, int pointtype, int period, int state, int category, int minLevel, int position, boolean isRechargeableItem) {
        this.buyable = buyable;
        this.itemId = itemId;
        this.price = price;
        this.reqItem = reqItem;
        this.reqItemQ = reqItemQ;
        this.period = period;
        this.state = state;
        this.category = category;
        this.minLevel = minLevel;
        this.position = position;
        this.isRechargeableItem = isRechargeableItem;
        this.rebuy = null;
        this.pointtype = pointtype;
    }

    public short getBuyable() {
        return buyable;
    }

    public int getItemId() {
        return itemId;
    }

    public int getPrice() {
        return price;
    }

    public int getReqItem() {
        return reqItem;
    }

    public int getReqItemQ() {
        return reqItemQ;
    }

    public int getCategory() {
        return category;
    }

    public int getPeriod() {
        return period;
    }

    public int getState() {
        return state;
    }

    public Item getRebuy() {
        return rebuy;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getPosition() {
        return position;
    }

    public boolean isRechargeableItem() {
        return isRechargeableItem;
    }

    public int getPointtype() {
        return pointtype;
    }
}
