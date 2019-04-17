/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 * @author PlayDK
 */
public enum MapleStatInfo {

    MDF(0), //最大DF增加 也就是恶魔精气
    MDamageOver(999999), //技能伤害最大值 默认: 999999
    OnActive(0), //重生成功概率 %
    PVPdamage(0),
    PVPdamageX(0), //Battle Mode ATT Increase
    abnormalDamR(0), //攻击昏迷，黑暗，冻结的状态异常对象时，伤害增加x%
    aranComboCon(0), //战神使用技能消耗连击点数
    acc(0), //命中值增加
    acc2dam(0), //命中值提升伤害 - 根据物理命中值和魔法命中值中较高数值的x%，增加格外伤害
    acc2mp(0), //命中值提升MP上限 - MP上限增加命中值的x%
    accR(0), //命中值增加 %
    accX(0), //命中值增加
    //action(0),
    actionSpeed(0), //攻击速度提升
    ar(0), //命中率增加 %
    asrR(0), //Abnormal Status Resistance % 
    attackCount(1), //攻击怪物的次数
    bdR(0), //攻击BOSS时，伤害增加x%
    bufftimeR(0), //增益持续时间增加 - 增益的持续时间增加x%
    bulletConsume(0), //消耗子弹/飞镖多少发
    bulletCount(1), //攻击怪物的数量 当为远程职业是为 攻击怪物的次数
    coolTimeR(0), //缩短冷却时间 %
    cooltime(0), //冷取时间
    cr(0), //爆击率增加 %
    criticaldamageMax(0), //爆击最大伤害增加
    criticaldamageMin(0), //爆击最小伤害增加
    costmpR(0), //消耗更多的MP来增加伤害
    damR(0), //技能增加伤害 % 或者总伤害增加 %
    damage(100), //技能伤害 默认 100%
    damageToBoss(100), //对BOSS技能伤害 100%
    dex(0), //敏捷增加
    dex2str(0), //敏捷提升力量 - 投资了AP敏捷的x%追加到力量
    dexFX(0), //敏捷增加
    dexX(0), //敏捷永久增加
    dexR(0), //敏捷增加 %
    dot(0), //Damage over time %
    dotInterval(0), //Damage dealt at intervals
    dotSuperpos(1), //Damage over time stack
    dotTime(0), //DOT time length (Lasts how long)
    dropR(0), //爆率增加 %
    damAbsorbShieldR(0), //伤害吸收
    emad(0), //增强魔法攻击力
    emdd(0), //增强魔法防御
    emhp(0), //增加Hp
    emmp(0), //增强Mp
    epad(0), //增强攻击力
    epdd(0), //增强物理防御
    er(0), //回避率增加x%
    eva(0), //Avoidability Increase, avoid
    eva2hp(0), //回避值提升HP上限 - HP上限增加回避值的x%
    evaR(0), //回避值增加 %
    evaX(0), //回避值增加
    expLossReduceR(0), //Reduce EXP loss at death %
    expR(0), //Additional % EXP
    extendPrice(0), //[Guild] Extend price
    finalAttackDamR(0), //Additional damage from Final Attack skills %
    fixdamage(0), //Fixed damage dealt upon using skill
    forceCon(0), //Fury Cost
    gauge(0),
    hcCooltime(0),
    hcHp(0),
    hcProp(0),
    hcReflect(0),
    hcSubProp(0),
    hcSubTime(0),
    hcSummonHp(0), //召唤兽的Hp
    hcTime(0),
    hp(0), //Mp格外恢复
    hpCon(0), //HP Consumed
    iceGageCon(0), //Ice Gauge Cost
    ignoreMobDamR(0), //受到怪物攻击伤害减少x%
    ignoreMobpdpR(0), //Ignore Mob DEF % -> Attack higher
    indieIgnoreMobpdpR(0), //受到怪物攻击的伤害减少x%
    indieAcc(0), //命中值增加
    indieSTR(0), //力量增加
    indieDEX(0), //敏捷增加
    indieINT(0), //智力增加
    indieLUK(0), //运气增加
    indieAllStat(0), //所有属性增加
    indieBooster(0), //攻击速度提升
    indieDamR(0), //攻击力提高 %
    indieEva(0), //回避值增加
    indieJump(0), //跳跃力增加
    indieMDF(0), //DF增加 也就是恶魔精气
    indieMad(0), //魔法攻击力增加
    indieMadR(0),//魔法攻击力增加 %
    indieMaxDamageOver(0), //攻击上限增加
    indieMaxDamageOverR(0), //攻击上限增加 %
    indieMdd(0), //魔法防御力增加
    indieMhp(0), //Hp增加
    indieMhpR(0), //HP Consumed
    indieMmp(0), //Mp增加
    indieMmpR(0), //Mp增加 %
    indiePMdR(0),
    indiePad(0), //攻击力增加
    indiePadR(0),//攻击力增加 %
    indiePdd(0), //物理防御力增加
    indiePddR(0), //物理防御力增加 %
    indieSpeed(0), //移动速度增加
    indieCr(0), //暴击概率增加 %
    indieAsrR(0), //状态异常抗性
    indieTerR(0), //所有属性抗性
    indieBDR(0), //对BOSS伤害增加
    indieExp(0), //经验获得
    indieStance(0), //什么姿势
    int2luk(0), //智力提升运气 - 投资了AP智力的x%追加到运气
    intFX(0), //智力增加
    intX(0), //永久增加智力
    int_(0, true), //永久增加智力
    intR(0), //智力增加 %
    itemCon(0), //Consumes item upon using <itemid>
    itemConNo(0), //amount for above
    itemConsume(0), //Uses certain item to cast that attack, the itemid doesn't need to be in inventory, just the effect.
    jump(0), //跳跃力增加
    kp(0), //Body count attack stuffs
    luk2dex(0), //运气提升敏捷 - 投资了AP运气的x%追加到敏捷
    lukFX(0), //运气增加
    lukX(0), //永久增加运气
    luk(0), //永久增加运气
    lukR(0), //运气增加 %
    lv2mhp(0), //升级增加血量上限
    lv2mmp(0), //升级增加Mp上限
    lv2damX(0), //Additional damage per character level
    lv2mad(0), //升级增加魔法攻击力 - 每10级魔法攻击力增加1
    lv2mdX(0), //Additional magic defense per character level
    lv2pad(0), //升级增加物理攻击力 - 每10级攻击力增加1
    lv2pdX(0), //Additional defense per character level
    mad(0), //永久增加魔法攻击力
    madX(0), //魔法攻击力增加
    mastery(0), //武器熟练度增加 %
    mdR(0), //魔法防御力增加
    mdd(0), //魔法防御力增加
    mdd2dam(0), //魔防提升伤害 - 增加魔法防御力的x%的伤害
    mdd2pdd(0), //魔法防御力的x%追加到物理防御力
    mdd2pdx(0), //受到物功减少伤害 - 受到物理攻击时，无视相当于魔法防御力x%的伤害
    mddR(0), //魔法防御力增加 %
    mddX(0), //魔法防御力增加
    mesoR(0), //金币获得量(怪物掉落)增加 %
    mhp2damX(0), //Max MP added as additional damage
    mhpR(0), //HP上限增加x%
    mhpX(0), //最大Hp 增加 %
    minionDeathProp(0), //攻击解放战补给模式的普通怪物时，有x%的概率造成一击必杀效果
    mmp2damX(0),
    mmpR(0), //MP上限增加x%
    mmpX(0), //最大Mp 增加 %
    mobCount(1), //Max Enemies hit
    mobCountDamR(0), //
    morph(0), //MORPH ID
    mp(0), //Mp格外恢复
    mpCon(0), //使用时消耗的mp数值
    mpConEff(0), //MP Potion effect increase %
    mpConReduce(0), //技能Mp消耗减少 %
    madR(0), //魔法攻击增加 x%
    nbdR(0), //攻击普通怪物时，伤害增加x%
    nocoolProp(0), //有一定概率无冷却时间 - 使用技能后，有x%概率无冷却时间。使用无冷却时间的技能时无效。
    onActive(0),
    onHitHpRecoveryR(0), //Chance to recover HP when attacking.
    onHitMpRecoveryR(0), //Chance to recover MP when attacking.
    pad(0), //攻击力增加
    padX(0), //物理攻击力增加
    padR(0),
    passivePlus(0), //被动技能等级加1 - 被动技能的技能等级增加1级。但对既有主动效果，又有被动效果的技能无效。
    pdd(0), //物理防御力增加
    pdd2dam(0), //物防提升伤害 - 增加物理防御力的x%的伤害
    pdd2mdd(0), //物理防御力的x%追加到魔法防御力
    pdd2mdx(0), //受到魔攻减少伤害 - 受到魔法攻击时，无视相当于物理防御力x%的伤害
    pddR(0), //物理防御力增加 %
    pddX(0), //物理防御力增加
    pdR(0), //攻击面板加成 x%
    period(0), //[Guild/Professions] time taken
    price(0), //[Guild] price to purchase
    priceUnit(0), //[Guild] Price stuffs
    prop(100), //触发的概率 默认100%
    psdJump(0), //跳跃力增加
    psdSpeed(0), //移动速度增加
    powerCon(0), //尖兵使用技能时需要的能量
    ppRecovery(0),
    ppCon(0),
    range(0), //最大射程
    reqGuildLevel(0), //[Guild] guild req level
    selfDestruction(0), //Self Destruct Damage
    speed(0), //移动速度增加
    speedMax(0), //移动速度上限增加
    str(0), //力量增加
    str2dex(0), //力量提升敏捷 - 投资了AP力量的x%追加到敏捷
    strFX(0), //力量增加
    strX(0), //力量永久增加
    strR(0), //力量增加 %
    subProp(0), //Summon Damage Prop
    subTime(-1), //Summon Damage Effect time
    suddenDeathR(0), //Instant kill on enemy %
    summonTimeR(0), //Summon Duration + %
    soulmpCon(0), //灵魂消耗点数
    stanceProp(0),
    targetPlus(0), //增加群功技能对象数 - 群攻技能的攻击对象数量增加1
    tdR(0), //阿斯旺解放战，攻击塔时，伤害增加x%
    terR(0), //所有属性抗性增加 %
    time(-1), //技能BUFF的持续时间 或者 给怪物BUFF的持续时间
    q(0),
    q2(0),
    s(0),
    t(0), //Damage taken reduce
    u(0),
    v(0),
    w(0),
    x(0),
    y(0),
    z(0);
    private final int def;
    private final boolean special;

    MapleStatInfo(int def) {
        this.def = def;
        this.special = false;
    }

    MapleStatInfo(int def, boolean special) {
        this.def = def;
        this.special = special;
    }

    public int getDefault() {
        return def;
    }

    public boolean isSpecial() {
        return special;
    }
}
