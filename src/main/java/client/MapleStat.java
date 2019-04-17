package client;

public enum MapleStat {

    皮肤(0x1), // short
    脸型(0x2), // int
    发型(0x4), // int
    等级(0x10), // byte
    职业(0x20), // int
    力量(0x40), // short
    敏捷(0x80), // short
    智力(0x100), // short
    运气(0x200), // short
    HP(0x400), // int
    MAXHP(0x800), // int
    MP(0x1000), // int
    MAXMP(0x2000), // int
    AVAILABLEAP(0x4000), // short
    AVAILABLESP(0x8000), // short (depends)
    经验(0x10000), // V.110修改为 long
    人气(0x20000), // int
    金币(0x40000), // V.110修改为 long
    宠物(0x180008), // Pets: 0x8 + 0x80000 + 0x100000  [3 longs]
    GACHAPONEXP(0x80000), // int
    疲劳(0x80000), //疲劳
    领袖(0x100000), //领袖
    洞察(0x200000), //洞察
    意志(0x400000), //意志
    手技(0x800000), //手技
    感性(0x1000000), //感性
    魅力(0x2000000), //魅力
    TODAYS_TRAITS(0x4000000), //今日获得
    TRAIT_LIMIT(0x8000000),
    BATTLE_EXP(0x10000000),
    BATTLE_RANK(0x20000000),
    BATTLE_POINTS(0x40000000),
    ICE_GAGE(0x80000000L),
    VIRTUE(0x100000000L),
    性别(0x200000000L);
    private final long i;

    MapleStat(long i) {
        this.i = i;
    }

    public static MapleStat getByValue(long value) {
        for (final MapleStat stat : MapleStat.values()) {
            if (stat.i == value) {
                return stat;
            }
        }
        return null;
    }

    public long getValue() {
        return i;
    }

    public enum Temp {

        力量(0x1),
        敏捷(0x2),
        智力(0x4),
        运气(0x8),
        物攻(0x10),
        魔攻(0x20),
        物防(0x40),
        魔防(0x80),
        命中(0x100),
        回避(0x200),
        速度(0x400),
        跳跃(0x800);
        private final int i;

        Temp(int i) {
            this.i = i;
        }

        public int getValue() {
            return i;
        }
    }
}
