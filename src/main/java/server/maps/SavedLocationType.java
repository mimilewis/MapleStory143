package server.maps;

public enum SavedLocationType {

    FREE_MARKET(0), //自由市场
    MULUNG_TC(1), //次元门
    WORLDTOUR(2), //旅游中心
    FLORINA(3),
    FISHING(4), //钓鱼
    RICHIE(5),
    DONGDONGCHIANG(6),
    EVENT(7), //任务地图
    AMORIA(8), //结婚地图
    CHRISTMAS(9), //圣诞地图?
    ARDENTMILL(10), //匠人街
    TURNEGG(11), //转蛋机
    PVP(12), //大乱战斗地图
    GUILD(13), //家族地图
    FAMILY(14), //枫之高校
    MonsterPark(15), //怪物公园
    ROOT(16), //鲁塔比斯
    BPReturn(17),
    STAR_PLANET(18),
    CRYSTALGARDEN(19),
    PQ_OUT1(20);
    private final int index;

    SavedLocationType(int index) {
        this.index = index;
    }

    public static SavedLocationType fromString(String Str) {
        return valueOf(Str);
    }

    public int getValue() {
        return index;
    }
}
