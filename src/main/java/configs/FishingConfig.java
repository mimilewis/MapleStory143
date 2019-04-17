/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package configs;


import tools.config.Property;

/**
 * @author admin
 */
public class FishingConfig {

    /**
     * 是否启用钓鱼系统
     * 默认: True
     */
    @Property(key = "Fishing.Enable", defaultValue = "true")
    public static boolean FISHING_ENABLE;
    /**
     * 钓鱼地图
     * 默认：749050500,749050501,749050502,970020000,970020005
     */
    @Property(key = "Fishing.Check.MAP", defaultValue = "749050500,749050501,749050502,970020000,970020005")
    public static String FISHING_MAP;
    /**
     * 是否使用指定道具的椅子进行钓鱼
     * 默认: True
     */
    @Property(key = "Fishing.Check.Chair", defaultValue = "true")
    public static boolean FISHING_CHECK_CHAIR;
    /**
     * 钓鱼时做的椅子
     * 默认: 3011000
     */
    @Property(key = "Fishing.Chair", defaultValue = "3011000")
    public static int FISHING_CHAIR;
    /**
     * 普通钓鱼竿钓鱼的时间间隔
     * 默认: 60000 (毫秒)
     */
    @Property(key = "Fishing.Time", defaultValue = "60000")
    public static int FISHING_TIME;
    /**
     * 高级钓鱼竿钓鱼的时间间隔
     * 默认: 30000 (毫秒)
     */
    @Property(key = "Fishing.Time.Vip", defaultValue = "30000")
    public static int FISHING_TIME_VIP;
    /**
     * GM钓鱼的时间间隔
     * 默认: 10000 (毫秒)
     */
    @Property(key = "Fishing.Time.GM", defaultValue = "10000")
    public static int FISHING_TIME_GM;
    /**
     * 普通钓鱼竿成功的概率
     * 默认: 70%
     */
    @Property(key = "Fishing.Chance", defaultValue = "70")
    public static int FISHING_CHANCE;
    /**
     * 高级钓鱼竿成功的概率
     * 默认: 90%
     */
    @Property(key = "Fishing.Chance.Vip", defaultValue = "90")
    public static int FISHING_CHANCE_VIP;
    /**
     * GM钓鱼成功的概率
     * 默认: 100%
     */
    @Property(key = "Fishing.Chance.GM", defaultValue = "100")
    public static int FISHING_CHANCE_GM;
}
