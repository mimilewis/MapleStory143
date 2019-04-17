/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login;

/**
 * @author PlayDK
 */
public enum JobType {

    反抗者(0, 3000, 931000000, false, false, false),
    冒险家(1, 0, 0, false, false, false),
    骑士团(2, 1000, 913040000, false, true, false),
    战神(3, 2000, 914000000, false, false, true),
    龙神(4, 2001, 900090000, false, false, true),
    双弩精灵(5, 2002, 910150000, false, false, false),
    恶魔猎手(6, 3001, 931050310, true, false, false), //脸饰
    幻影(7, 2003, 910000000, false, true, false), //披风
    暗影双刀(8, 0, 910000000, false, false, false),
    米哈尔(9, 5000, 910000000, false, false, false),
    夜光(10, 2004, 910000000, false, true, false), //披风
    狂龙(11, 6000, 910000000, false, false, false),
    萝莉(12, 6001, 910000000, false, false, false),
    火炮手(13, 0, 0, false, false, false),
    尖兵(14, 3002, 910000000, true, false, false), //脸饰
    神之子(15, 10112, 910000000, false, true, false), //披风
    隐月(16, 2005, 910000000, false, true, true, false),
    品克缤(17, 13000, 10000, false, false, false),
    超能力者(18, 14000, 910000000, false, false, false),
    林之灵(19, 11212, 910000000, true, false, false, false), //脸饰 = true 披风 = true 裤子 = false 帽子 = true 好像创建的时候盛大没给帽子和披风
    龙的传人(20, 0, 910000000, false, false, false), //以前是13 现在修改到 16 V.115.1改到 17    
    剑豪(21, 4001, 10000, false, false, false),
    阴阳师(22, 4002, 10000, false, false, false),

    终极冒险家(-1, 0, 130000000, false, false, false);
    public final int type;
    public final int jobId;
    public final int mapId;
    public final boolean faceMark;
    public final boolean cape;
    public final boolean bottom;
    public final boolean cap;
    private final boolean 自由市场 = true;

    JobType(int type, int id, int map, boolean faceMark, boolean cape, boolean bottom) {
        this.type = type;
        this.jobId = id;
        this.mapId = 自由市场 ? 910000000 : map;
        this.faceMark = faceMark; //脸饰
        this.cape = cape; //披风
        this.bottom = bottom; //裤子
        this.cap = false; //帽子
    }

    JobType(int type, int id, int map, boolean faceMark, boolean cape, boolean bottom, boolean cap) {
        this.type = type;
        this.jobId = id;
        this.mapId = 自由市场 ? 910000000 : map;
        this.faceMark = faceMark; //脸饰
        this.cape = cape; //披风
        this.bottom = bottom; //裤子
        this.cap = cap; //帽子
    }

    public static JobType getByType(int g) {
        for (JobType e : JobType.values()) {
            if (e.type == g) {
                return e;
            }
        }
        return null;
    }

    public static JobType getById(int wzNmae) {
        for (JobType e : JobType.values()) {
            if (e.jobId == wzNmae || (wzNmae == 508 && e.type == 13) || (wzNmae == 1 && e.type == 8)) {
                return e;
            }
        }
        return 冒险家;
    }
}
