package client;

public enum MapleForceType {
    恶魔精气(false),
    幻影卡牌(false),
    剑刃之壁,
    追击盾,
    追击盾_反射(false),
    宙斯盾系统(false),
    尖兵火箭,
    狂风肆虐,
    暴风灭世(false),
    UNK_09,
    三彩箭矢(false),
    刺客标记,
    金钱炸弹,
    灵狐,
    UNK_0E,
    影子蝙蝠,
    影子蝙蝠_反射(false),
    轨道烈焰(false),
    UNK_12,
    UNK_13,
    辅助导弹,
    UNK_15,
    心灵传动(false),
    UNK_17,
    UNK_18,
    UNK_19,
    UNK_1A,
    心雷合一,
    制裁火球,
    爱星能量,
    UNK_1E,
    UNK_1F,
    UNK_20,
    UNK_21,
    UNK_22,
    UNK_23,;


    private final boolean isMultiMob;

    MapleForceType() {
        isMultiMob = true;
    }

    MapleForceType(boolean isMultiMob) {
        this.isMultiMob = isMultiMob;
    }

    public boolean isMultiMob() {
        return isMultiMob;
    }
}
