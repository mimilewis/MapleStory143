package handling.world.family;

import client.MapleBuffStat;
import client.MapleCharacter;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer.BuffTimer;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public enum MapleFamilyBuff {

    瞬移("瞬移", "[对象] 我自己\n[效果] 可以马上瞬移到自己想去见的学院成员所在的身边.", 0, 0, 0, 300, 190000),
    召唤("召唤", "[对象] 学院成员1名\n[效果] 可以召唤自己想召唤的学院成员到自己的身边.", 1, 0, 0, 500, 190001),
    爆率15分钟("我的爆率 1.2倍(15分钟)", "[对象] 我自己\n[持续时间] 15分钟\n[效果] 打猎怪物时提升怪物爆率 #c1.2倍#.\n※ 与爆率活动重叠时效果被无视.", 2, 15, 120, 700, 190002),
    经验15分钟("我的经验值 1.2倍(15分钟)", "[对象] 我自己\n[持续时间] 15分钟\n[效果] 打猎怪物时提升怪物经验值#c1.2倍#.\n※ 与经验值活动重叠时效果被无视.", 3, 15, 120, 900, 190003),
    爆率30分钟("我的爆率 1.2倍(30分钟)", "[对象] 我自己\n[持续时间] 30分钟\n[效果] 打猎怪物时提升怪物爆率#c1.2倍#.\n※ 与爆率活动重叠时效果被无视.", 2, 30, 120, 1500, 190004),
    经验30分钟("我的经验值 1.2倍(30分钟)", "[对象] 我自己\n[持续时间] 30分钟\n[效果] 打猎怪物时提升怪物经验值 #c1.2倍#.\n※ 与经验值活动重叠时效果被无视.", 3, 30, 120, 2000, 190005),
    //Drop_15_15("My Drop Rate 1.5x (15min)", "[Target] Me\n[Time] 15 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 15, 150, 1500, 190009),
    //Drop_15_30("My Drop Rate 1.5x (30min)", "[Target] Me\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 30, 150, 2000, 190010),
    团结("团结(30分钟)", "[发动条件]学院关系图中下端上学院成员有6名以上在线时\n[持续时间] 30分钟\n[效果] 提升爆率,经验值#c1.5倍#. ※ 与爆率经验值活动重叠时,效果被无视.", 4, 30, 150, 3000, 190006);
    //Drop_Party_12("My Party Drop Rate 1.2x (30min)", "[Target] Party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c1.2x#.\n*  If the event is in progress, this will be nullified.", 2, 30, 120, 4000, 190007),
    //EXP_Party("My Party EXP Rate 1.2x (30min)", "[Target] Party\n[Time] 30 min.\n[Effect] Monster EXP rate will be increased #c1.2x#.\n*  If the event is in progress, this will be nullified.", 3, 30, 120, 5000, 190008),
    //Drop_Party_15("My Party Drop Rate 1.5x (30min)", "[Target] Party\n[Time] 30 min.\n[Effect] Monster drop rate will be increased #c1.5x#.\n*  If the event is in progress, this will be nullified.", 2, 30, 150, 7000, 190011);
    // 0=tele, 1=summ, 2=drop, 3=exp, 4=both
    public final String name;
    public final String desc;
    public final int rep;
    public final int type;
    public final int questID;
    public final int duration;
    public final int effect;
    public Map<MapleBuffStat, Integer> effects;

    MapleFamilyBuff(String name, String desc, int type, int duration, int effect, int rep, int questID) {
        this.name = name;
        this.desc = desc;
        this.rep = rep;
        this.type = type;
        this.questID = questID;
        this.duration = duration;
        this.effect = effect;
        setEffects();
    }

    public int getEffectId() {
        switch (type) {
            case 2: //drop
                return 2022694; //暗影双刀-掉落率2倍！ - 30分钟内掉落率提高为2倍。
            case 3: //exp
                return 2450018; //暗影双刀-经验值2倍！ - 30分钟内获得的经验值增加2倍。
        }
        return 2022332; //custom 工作人员O的激励 - 工作人员O给我的激励提示。30分钟内跳跃力提高20。
    }

    public void setEffects() {
        //custom
        this.effects = new EnumMap<>(MapleBuffStat.class);
        switch (type) {
            case 2: //drop
                effects.put(MapleBuffStat.DROP_RATE, effect);
                effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
            case 3: //exp
                effects.put(MapleBuffStat.EXPRATE, effect);
                break;
            case 4: //both
                effects.put(MapleBuffStat.EXPRATE, effect);
                effects.put(MapleBuffStat.DROP_RATE, effect);
                effects.put(MapleBuffStat.MESO_RATE, effect);
                break;
        }
    }

    public void applyTo(MapleCharacter chr) {
//        chr.send(BuffPacket.giveBuff(-getEffectId(), duration * 60000, effects, null, chr));
        MapleStatEffect eff = MapleItemInformationProvider.getInstance().getItemEffect(getEffectId());
        chr.cancelEffect(eff, true, -1, effects);
        long starttime = System.currentTimeMillis();
        CancelEffectAction cancelAction = new CancelEffectAction(chr, eff, starttime, effects);
        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, duration * 60000);
        chr.registerEffect(eff, starttime, schedule, effects, false, duration, chr.getId());
    }
}
