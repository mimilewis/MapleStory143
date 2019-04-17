/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

/**
 * @author PlayDK
 */
public enum MapleExpStat {

    活动奖励经验(0x01),
    特别经验(0x02), //每次打3个怪物时给你特别经验值X%
    活动组队经验(0x04), //活动组队经验奖励倍数(i x 100)最大3倍
    组队经验(0x10),
    结婚奖励经验(0x20), //结婚经验要在组队经验前面
    道具佩戴经验(0x40),
    网吧特别经验(0x80),
    彩虹周奖励经验(0x100),
    欢享奖励经验(0x200),
    飞跃奖励经验(0x400),
    精灵祝福经验(0x800),
    增益奖励经验(0x1000),
    休息经验(0x2000),
    物品奖励经验(0x4000),
    阿斯旺获胜者奖励经验(0x8000),
    使用道具经验(0x10000), //使用道具增加了%经验
    超值礼包奖励经验(0x20000),
    受道具影响而获得的组队任务额外奖励(0x40000),
    格外获得经验(0x80000),
    血盟奖励经验(0x100000),
    家族奖励经验(0x100000),
    冷冻勇士经验(0x200000),
    燃烧场地经验(0x400000),
    HP风险经验(0x800000),
    累计打猎数量奖励经验(0x2000000),
    召唤戒指组队经验(0x4000000),
    PVP_BONUS_EXP(0x8000000),
    训练宠物额外经验(0x10000000);
    private final long i;

    MapleExpStat(long i) {
        this.i = i;
    }

    public long getValue() {
        return i;
    }
}
