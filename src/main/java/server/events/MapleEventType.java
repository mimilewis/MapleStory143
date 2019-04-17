package server.events;

public enum MapleEventType {

    Coconut("椰子比赛", new int[]{109080000}), //冒险岛活动 - 椰子比赛
    CokePlay("CokePlay", new int[]{109080010}), //?????? - ?-???? ??
    Fitness("向高地", new int[]{109040000, 109040001, 109040002, 109040003, 109040004}), //冒险岛活动 - 向高地&lt;待机室>
    OlaOla("上楼~上楼", new int[]{109030001, 109030002, 109030003}), //冒险岛活动 - 上楼~上楼~&lt;第1阶段>
    OxQuiz("OX问答", new int[]{109020001}), //冒险岛活动 - OX问答
    Survival("", new int[]{809040000, 809040100}),
    Snowball("雪球赛", new int[]{109060000}); //冒险岛活动 - 雪球赛
    public final String desc; //活动描述介绍
    public final int[] mapids; //活动举行的地图

    MapleEventType(String desc, int[] mapids) {
        this.desc = desc;
        this.mapids = mapids;
    }

    public static MapleEventType getByString(String splitted) {
        for (MapleEventType t : MapleEventType.values()) {
            if (t.name().equalsIgnoreCase(splitted)) {
                return t;
            }
        }
        return null;
    }
}
