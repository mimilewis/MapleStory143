package client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import handling.Buffstat;
import tools.Randomizer;

import java.io.Serializable;

public enum MapleDisease implements Serializable, Buffstat {

    虚弱(MapleBuffStat.虚弱, 122),
    封印(MapleBuffStat.封印, 120),
    黑暗(MapleBuffStat.黑暗, 121),
    昏迷(MapleBuffStat.昏迷, 123),
    诅咒(MapleBuffStat.诅咒, 124),
    中毒(MapleBuffStat.中毒, 125),
    缓慢(MapleBuffStat.缓慢, 126),
    诱惑(MapleBuffStat.诱惑, 128),
    混乱(MapleBuffStat.混乱, 132),
    不死化(MapleBuffStat.不死化, 133),
    无法使用药水(MapleBuffStat.无法使用药水, 134),
    SHADOW(MapleBuffStat.SHADOW, 135),
    致盲(MapleBuffStat.致盲, 136),
    FREEZE(MapleBuffStat.FREEZE, 137),
    DISABLE_POTENTIAL(MapleBuffStat.DISABLE_POTENTIAL, 138),
    变身(MapleBuffStat.变身效果, 172),
    龙卷风(MapleBuffStat.龙卷风, 173),
    死亡束缚(MapleBuffStat.死亡束缚, 174),
    返回原位置(MapleBuffStat.返回原位置, 184),
    诱惑之境(MapleBuffStat.诱惑, 188),
    精灵帽子(MapleBuffStat.精灵的帽子, 189),
    精灵帽子2(MapleBuffStat.精灵的帽子, 190),
    禁止跳跃(MapleBuffStat.增加跳跃力, 229);
//    变身(0x4, 2, 172), //变身
//    诱惑(0x1, 3, 128), //诱惑          --OK
//    僵尸(0x2000000, 4, 133), //失去控制 不死化
//    药水停止(0x100, 3, 134), //禁止使用药水 药水停止                //0x200
//    缓慢(0x80, 3, 126), //缓慢        --OK
//    SHADOW(0x80000000, 5, 135), //从不停止 ?receiving damage/moving  //0x400
//    诅咒(0x100, 3, 124), //诅咒
//    致盲(0x20000, 3, 136), //致盲                                 //0x800
//    虚弱(0x200, 3, 122), //虚弱[不能跳]    --OK
//    黑暗(0x40000, 3, 121), //黑暗        --OK
//    封印(0x80000, 3, 120), //封印         --ok
//    中毒(0x100000, 3, 125), //中毒         -测试
//    眩晕(0x200000, 3, 123), //眩晕         --ok
//    结冰(0x20000, 3, 137), //中毒
//    混乱(0x100000, 4, 132),
//    潜能无效(0x1000000, 4, 138), //潜在能力无效
//    TORNADO(0x10000000, 4, 173),
//    FLAG(0x80000000, 5, 799),;

    // 0x100 is disable skill except buff
    private final MapleBuffStat buffStat;
    private final int disease;

    @JsonCreator
    MapleDisease(@JsonProperty("buffStat") MapleBuffStat buffStat, @JsonProperty("JsonCreator") int disease) {
        this.buffStat = buffStat;
        this.disease = disease;
    }

    public static MapleDisease getRandom() {
        while (true) {
            for (MapleDisease dis : MapleDisease.values()) {
                if (Randomizer.nextInt(MapleDisease.values().length) == 0) {
                    return dis;
                }
            }
        }
    }

    public static MapleDisease getBySkill(int skill) {
        for (MapleDisease d : MapleDisease.values()) {
            if (d.getDisease() == skill) {
                return d;
            }
        }
        return null;
    }

    public MapleBuffStat getBuffStat() {
        return buffStat;
    }

    @Override
    public int getPosition() {
        return buffStat.getPosition();
    }

    @Override
    public int getValue() {
        return buffStat.getValue();
    }

    public int getDisease() {
        return disease;
    }
}
