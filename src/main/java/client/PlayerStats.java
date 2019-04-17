package client;

import client.inventory.*;
import client.skills.InnerSkillEntry;
import client.skills.Skill;
import client.skills.SkillEntry;
import client.skills.SkillFactory;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.skills.*;
import handling.world.WorldGuildService;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildSkill;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.*;
import server.life.Element;
import tools.Pair;
import tools.Triple;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.packet.EffectPacket;
import tools.packet.InventoryPacket;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

public class PlayerStats implements Serializable {


    public final static int[] pvpSkills = {1000007, 2000007, 3000006, 4000010, 5000006, 5010004, 11000006, 12000006, 13000005, 14000006, 15000005, 21000005, 22000002, 23000004, 31000005, 32000012, 33000004, 35000005};
    private static final Logger log = LogManager.getLogger();
    private static final long serialVersionUID = -679541993413738569L;
    private final static int[] allJobs = {0, 10000000, 20000000, 20010000, 20020000, 20030000, 20040000, 20050000, 30000000, 30010000, 30020000, 50000000, 60000000, 60010000, 100000000, 110000000};
    private final Map<Integer, Integer> setHandling = new HashMap<>();
    private final Map<Integer, Integer> skillsIncrement = new HashMap<>();
    private final Map<Integer, Integer> damageIncrease = new HashMap<>();
    private final EnumMap<Element, Integer> elemBoosts = new EnumMap<>(Element.class);
    private final List<Triple<Integer, String, Integer>> psdSkills = new ArrayList<>();
    private final List<Equip> durabilityHandling = new ArrayList<>();
    private final List<Equip> equipLevelHandling = new ArrayList<>();
    private final List<Equip> sealedEquipHandling = new ArrayList<>();
    /*
     * 超级技能进阶信息的处理
     */
    private final Map<Integer, Integer> add_skill_duration = new HashMap<>(); //增加持续时间
    private final Map<Integer, Integer> add_skill_attackCount = new HashMap<>(); //增加攻击怪物的次数
    private final Map<Integer, Integer> add_skill_targetPlus = new HashMap<>();  //增加攻击怪物的数量
    private final Map<Integer, Integer> add_skill_bossDamageRate = new HashMap<>();  //增加攻击BOSS的伤害
    private final Map<Integer, Integer> add_skill_dotTime = new HashMap<>();  //对怪物持续伤害BUFF
    private final Map<Integer, Integer> add_skill_prop = new HashMap<>();  //概率
    private final Map<Integer, Integer> add_skill_coolTimeR = new HashMap<>();  //减少技能的冷却时间
    private final Map<Integer, Integer> add_skill_ignoreMobpdpR = new HashMap<>();  //增加技能攻击怪物无视防御
    public short str, dex, luk, int_;
    public int hp, maxhp, mp, maxmp;
    public transient boolean equippedWelcomeBackRing, hasClone, hasPartyBonus, Berserk, canFish, canFishVIP;
    public transient double expBuff, dropBuff, mesoBuff, cashBuff, mesoGuard, mesoGuardMeso, expMod, pickupRange, incRewardProp;
    //same with incMesoProp/incRewardProp for now
    public transient int recoverHP, recoverMP, mpconReduce, mpconPercent, incMesoProp, reduceCooltime, coolTimeR, suddenDeathR, expLossReduceR, DAMreflect, DAMreflect_rate,
            ignoreDAMr, ignoreDAMr_rate, ignoreDAM, ignoreDAM_rate, mpRestore,
            hpRecover, hpRecoverProp, hpRecover_Percent, mpRecover, mpRecoverProp, RecoveryUP, BuffUP, RecoveryUP_Skill, BuffUP_Skill,
            incAllskill, combatOrders, BuffUP_Summon, dodgeChance,
            equipmentBonusExp, dropMod, cashMod, ASR, TER, pickRate, decreaseDebuff, equippedFairy, equippedSummon,
            pvpDamage, hpRecoverTime = 0, mpRecoverTime = 0, dot, dotTime, pvpRank, pvpExp, trueMastery, damX, incMaxDamage, incMaxDF,
            gauge_x, mpconMaxPercent;
    public transient int wdef; //物理防御力
    public transient int mdef; //魔法防御力
    public transient int levelBonus; //减少装备的穿戴等级
    public transient int questBonus; //任务经验奖励倍数
    public transient int harvestingTool; //采集工具在背包的坐标位置
    public transient int defRange; //默认攻击距离
    public transient int speed; //移动速度
    public transient int speedMax; //最大移动速度
    public transient int jump; //跳跃力
    public transient short passive_sharpeye_rate; //爆击概率
    public transient short passive_sharpeye_max_percent; //爆击最大伤害倍率
    public transient short passive_sharpeye_min_percent; //爆击最小伤害倍率
    public transient int stanceProp; //稳如泰山概率
    public transient int percent_wdef; //物理防御增加x%
    public transient int percent_mdef; //魔法防御增加x%
    public transient int percent_hp; //Hp增加x%
    public transient int percent_mp; //Mp增加x%
    public transient int percent_str; //力量增加x%
    public transient int percent_dex; //敏捷增加x%
    public transient int percent_int; //智力增加x%
    public transient int percent_luk; //运气增加x%
    public transient int percent_acc; //命中增加x%
    public transient int percent_atk; //物理攻击力增加x%
    public transient int percent_matk; //魔法攻击力增加x%
    public transient int percent_ignore_mob_def_rate; //无视怪x%防御
    public transient double percent_damage; //攻击增加 也就是角色面板的攻击数字增加x%
    public transient double percent_damage_rate; //伤害增加x%
    public transient int percent_boss_damage_rate; //BOSS伤害增加x%
    public transient int ignore_mob_damage_rate; //被怪物攻击受到的伤害减少x%
    public transient int reduceDamageRate; //被动减少受到的伤害的x%
    // Elemental properties
    public transient int def, element_ice, element_fire, element_light, element_psn;
    public transient int raidenCount, raidenPorp; //奇袭者雷电能够获得数量和概率
    private transient float shouldHealHP, shouldHealMP;
    private transient byte passive_mastery; //武器熟练度
    private transient int localstr, localdex, localluk, localint_, localmaxhp, localmaxmp, addmaxhp, addmaxmp;
    private transient int indieStrFX, indieDexFX, indieLukFX, indieIntFX; //内在技能增加属性 不计算潜能的百分比增加属性
    private transient int magic, watk, accuracy;
    private transient float localmaxbasedamage, localmaxbasepvpdamage, localmaxbasepvpdamageL;
    private transient int efftype;

    public static int getSkillByJob(int skillId, int job) {
        if (JobConstants.is骑士团(job)) {
            return skillId + 10000000;
        } else if (JobConstants.is战神(job)) {
            return skillId + 20000000;
        } else if (JobConstants.is龙神(job)) {
            return skillId + 20010000;
        } else if (JobConstants.is双弩精灵(job)) {
            return skillId + 20020000;
        } else if (JobConstants.is幻影(job)) {
            return skillId + 20030000;
        } else if (JobConstants.is夜光(job)) {
            return skillId + 20040000;
        } else if (JobConstants.is隐月(job)) {
            return skillId + 20050000;
        } else if (JobConstants.is反抗者(job)) {
            return skillId + 30000000;
        } else if (JobConstants.is恶魔猎手(job) || JobConstants.is恶魔复仇者(job)) {
            return skillId + 30010000;
        } else if (JobConstants.is尖兵(job)) {
            return skillId + 30020000;
        } else if (JobConstants.is米哈尔(job)) {
            return skillId + 50000000;
        } else if (JobConstants.is狂龙战士(job)) {
            return skillId + 60000000;
        } else if (JobConstants.is爆莉萌天使(job)) {
            return skillId + 60010000;
        } else if (JobConstants.is神之子(job)) {
            return skillId + 100000000;
        } else if (JobConstants.is林之灵(job)) {
            return skillId + 110000000;
        } else if (JobConstants.is剑豪(job)) {
            return skillId + 40010000;
        } else if (JobConstants.is阴阳师(job)) {
            return skillId + 40020000;
        }
        return skillId;
    }

    public void recalcLocalStats(MapleCharacter chra) {
        recalcLocalStats(false, chra);
    }

    private void resetLocalStats(int job) {
        accuracy = 0;
        wdef = 0;
        mdef = 0;
        damX = 0;
        addmaxhp = 0;
        addmaxmp = 0;
        localdex = getDex();
        localint_ = getInt();
        localstr = getStr();
        localluk = getLuk();
        indieDexFX = 0;
        indieIntFX = 0;
        indieStrFX = 0;
        indieLukFX = 0;
        speed = 100;
        jump = 100;
        pickupRange = 0.0;
        decreaseDebuff = 0;
        ASR = 0;
        TER = 0;
        dot = 0;
        questBonus = 1;
        dotTime = 0;
        trueMastery = 0;
        stanceProp = 0;
        percent_wdef = 0;
        percent_mdef = 0;
        percent_hp = 0;
        percent_mp = 0;
        percent_str = 0;
        percent_dex = 0;
        percent_int = 0;
        percent_luk = 0;
        percent_acc = 0;
        percent_atk = 0;
        percent_matk = 0;
        percent_ignore_mob_def_rate = 0;
        passive_sharpeye_rate = 5;
        passive_sharpeye_min_percent = 20;
        passive_sharpeye_max_percent = 50;
        percent_damage_rate = 100.0;
        percent_boss_damage_rate = 100;
        magic = 0;
        watk = 0;
        dodgeChance = 0;
        pvpDamage = 0;
        mesoGuard = 50.0;
        mesoGuardMeso = 0.0;
        percent_damage = 0.0;
        expBuff = 100.0;
        cashBuff = 100.0;
        dropBuff = 100.0;
        mesoBuff = 100.0;
        recoverHP = 0;
        recoverMP = 0;
        mpconReduce = 0;
        mpconPercent = 100;
        incMesoProp = 0;
        reduceCooltime = 0;
        coolTimeR = 0;
        suddenDeathR = 0;
        expLossReduceR = 0;
        incRewardProp = 0.0; //潜能道具所加的装备掉落几率
        DAMreflect = 0;
        DAMreflect_rate = 0;
        ignoreDAMr = 0;
        ignoreDAMr_rate = 0;
        ignoreDAM = 0;
        ignoreDAM_rate = 0;
        hpRecover = 0;
        hpRecoverProp = 0;
        hpRecover_Percent = 0;
        mpRecover = 0;
        mpRecoverProp = 0;
        mpRestore = 0;
        pickRate = 0;
        incMaxDamage = 0;
        equippedWelcomeBackRing = false;
        equippedFairy = 0;
        equippedSummon = 0;
        hasPartyBonus = false;
        hasClone = false;
        Berserk = false;
        canFish = false;
        canFishVIP = false;
        equipmentBonusExp = 0;
        RecoveryUP = 100;
        BuffUP = 100;
        RecoveryUP_Skill = 100;
        BuffUP_Skill = 100;
        BuffUP_Summon = 100;
        dropMod = 1;
        expMod = 1.0;
        cashMod = 1;
        levelBonus = 0;
        incMaxDF = 0;
        incAllskill = 0;
        combatOrders = 0;
        defRange = isRangedJob(job) ? 200 : 0;
        durabilityHandling.clear();
        equipLevelHandling.clear();
        sealedEquipHandling.clear();
        skillsIncrement.clear();
        damageIncrease.clear();
        setHandling.clear();
        add_skill_duration.clear(); //超级技能格外增加持续时间
        add_skill_attackCount.clear(); //超级技能格外增加攻击次数
        add_skill_targetPlus.clear();
        add_skill_dotTime.clear();
        add_skill_prop.clear();
        add_skill_coolTimeR.clear();
        add_skill_ignoreMobpdpR.clear();
        harvestingTool = 0;
        element_fire = 100;
        element_ice = 100;
        element_light = 100;
        element_psn = 100;
        def = 100;
        raidenCount = 0;
        raidenPorp = 0;
        ignore_mob_damage_rate = 0;
        reduceDamageRate = 0;
        mpconMaxPercent = 0;
    }

    public void recalcLocalStats(boolean first_login, MapleCharacter chra) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int oldmaxhp = localmaxhp;
        int localmaxhp_ = getMaxHp();
        int localmaxmp_ = getMaxMp();
        resetLocalStats(chra.getJob());
        //refreshGamesStatsForSkill(chra);
        for (MapleTraitType t : MapleTraitType.values()) {
            chra.getTrait(t).clearLocalExp();
        }
        StructItemOption soc;
        Map<Integer, SkillEntry> sData = new HashMap<>();
        for (Item item1 : chra.getInventory(MapleInventoryType.EQUIPPED).newList()) {
            Equip equip = (Equip) item1;
            if (equip.getPosition() == -11) {
                if (ItemConstants.isMagicWeapon(equip.getItemId())) {
                    Map<String, Integer> eqstat = ii.getItemBaseInfo(equip.getItemId());
                    if (eqstat != null) { //slow, poison, darkness, seal, freeze
                        if (eqstat.containsKey("incRMAF")) {
                            element_fire = eqstat.get("incRMAF");
                        }
                        if (eqstat.containsKey("incRMAI")) {
                            element_ice = eqstat.get("incRMAI");
                        }
                        if (eqstat.containsKey("incRMAL")) {
                            element_light = eqstat.get("incRMAL");
                        }
                        if (eqstat.containsKey("incRMAS")) {
                            element_psn = eqstat.get("incRMAS");
                        }
                        if (eqstat.containsKey("elemDefault")) {
                            def = eqstat.get("elemDefault");
                        }
                    }
                }
            }
            if (equip.getItemId() / 10000 == 166 && equip.getAndroid() != null && chra.getAndroid() == null) {
                chra.setAndroid(equip.getAndroid());
            }
            chra.getTrait(MapleTraitType.craft).addLocalExp(equip.getHands());
            accuracy += equip.getAcc();
            localmaxhp_ += equip.getHp();
            localmaxmp_ += equip.getMp();
            localdex += equip.getDex();
            localint_ += equip.getInt();
            localstr += equip.getStr();
            localluk += equip.getLuk();
            magic += equip.getMatk();
            watk += equip.getWatk();
            wdef += equip.getWdef();
            mdef += equip.getMdef();
            speed += equip.getSpeed();
            jump += equip.getJump();
            pvpDamage += equip.getPVPDamage();
            percent_boss_damage_rate += equip.getBossDamage();
            percent_ignore_mob_def_rate += equip.getIgnorePDR();
            percent_damage_rate *= ((double) equip.getTotalDamage() + 100.0) / 100.0;
            percent_str += equip.getAllStat();
            percent_dex += equip.getAllStat();
            percent_int += equip.getAllStat();
            percent_luk += equip.getAllStat();
            switch (equip.getItemId()) {
                case 1112918: //以前是1112127 盛大修改 回归戒指 - 热烈欢迎玩家回归的特别戒指，附带特殊福利。佩戴本戒指时，在组队状态下，#c全队队员可享受额外80%的召回经验奖励#。归来的朋友，快去和其他玩家组队一起战斗吧！
                    equippedWelcomeBackRing = true;
                    break;
                case 1122017: //精灵吊坠
                    equippedFairy = 10;
                    break;
                case 1122158:
                    equippedFairy = 5;
                    break;
                case 1112585: //天使的祝福 - 装备戒指后，可以召唤#c大天使#。
                    equippedSummon = 1085;
                    break;
                case 1112586: //黑天使的祝福 - 装备戒指后，可以召唤#c黑天使#。
                    equippedSummon = 1087;
                    break;
                case 1112663: //白天使的祝福 - 装备戒指，可以召唤#c大天使#。
                    equippedSummon = 1179;
                    break;
                default:
                    for (int eb_bonus : GameConstants.Equipments_Bonus) {
                        if (equip.getItemId() == eb_bonus) {
                            equipmentBonusExp += GameConstants.Equipment_Bonus_EXP(eb_bonus);
                            break;
                        }
                    }
                    break;
            }
            //恶魔猎手的盾牌加的Mp单独计算
            if (equip.getItemId() / 1000 == 1099) {
                incMaxDF += equip.getMp();
            }
            percent_hp += ii.getItemIncMHPr(equip.getItemId()); //增加百分比的HP
            percent_mp += ii.getItemIncMMPr(equip.getItemId()); //增加百分比的MP
            percent_boss_damage_rate += equip.getBossDamage(); //增加百分比的BOSS伤害
            percent_ignore_mob_def_rate += equip.getIgnorePDR(); //增加百分比的无视怪物防御
            percent_damage_rate += equip.getTotalDamage(); //增加百分比的伤害
            //套装属性
            Integer setId = ii.getSetItemID(equip.getItemId());
            if (setId != null && setId > 0) {
                int value = 1;
                if (setHandling.containsKey(setId)) {
                    value += setHandling.get(setId);
                }
                setHandling.put(setId, value); //套装ID 套装携带的数量
                //处理套装属性
                Pair<Integer, Integer> setix = handleEquipSetStats(ii, chra, first_login, sData, setId, value);
                if (setix != null) {
                    localmaxhp_ += setix.getLeft();
                    localmaxmp_ += setix.getRight();
                }
            }
            if (equip.getIncSkill() > 0 && ii.getEquipSkills(equip.getItemId()) != null) {
                for (int skillId : ii.getEquipSkills(equip.getItemId())) {
                    Skill skil = SkillFactory.getSkill(skillId);
                    if (skil != null && skil.canBeLearnedBy(chra.getJob())) { //dont go over masterlevel :D
                        int value = 1;
                        if (skillsIncrement.get(skil.getId()) != null) {
                            value += skillsIncrement.get(skil.getId());
                        }
                        skillsIncrement.put(skil.getId(), value);
                    }
                }
            }
            Pair<Integer, Integer> ix = handleEquipAdditions(ii, chra, first_login, sData, equip.getItemId());
            if (ix != null) {
                localmaxhp_ += ix.getLeft();
                localmaxmp_ += ix.getRight();
            }
            if (equip.getState(false) >= 17) {
                int[] potentials = {equip.getPotential1(), equip.getPotential2(), equip.getPotential3(), equip.getPotential4(), equip.getPotential5(), equip.getPotential6()};
                for (int i : potentials) {
                    if (i > 0) {
                        int itemReqLevel = ii.getReqLevel(equip.getItemId());
                        //System.err.println("潜能ID: " + i + " 装备等级: " + itemReqLevel + " 潜能等级: " + (itemReqLevel - 1) / 10);
                        List<StructItemOption> potentialInfo = ii.getPotentialInfo(i);
                        soc = potentialInfo.get(Math.min(potentialInfo.size() - 1, (itemReqLevel - 1) / 10));
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }
            if (equip.getSocketState() >= 0x13) {
                int[] sockets = {equip.getSocket1(), equip.getSocket2(), equip.getSocket3()};
                for (int i : sockets) {
                    if (i > 0) {
                        soc = ii.getSocketInfo(i);
                        if (soc != null) {
                            localmaxhp_ += soc.get("incMHP");
                            localmaxmp_ += soc.get("incMMP");
                            handleItemOption(soc, chra, first_login, sData);
                        }
                    }
                }
            }
            if (equip.getDurability() > 0) {
                durabilityHandling.add(equip);
            }
            if (GameConstants.getMaxLevel(equip.getItemId()) > 0 && (GameConstants.getStatFromWeapon(equip.getItemId()) == null ? (equip.getEquipLevel() <= GameConstants.getMaxLevel(equip.getItemId())) : (equip.getEquipLevel() < GameConstants.getMaxLevel(equip.getItemId())))) {
                equipLevelHandling.add(equip);
            }
            if (equip.isSealedEquip()) {
                sealedEquipHandling.add(equip);
            }
        }

        if (chra.getSummonedFamiliar() != null) {
            final MonsterFamiliar summonedFamiliar = chra.getSummonedFamiliar();
            for (int i = 0; i < 3; ++i) {
                final int option = summonedFamiliar.getOption(i);
                if (option > 0) {
                    soc = ii.getFamiliar_option().get(option).get(Math.max(summonedFamiliar.getGrade(), 0));
                    if (soc != null) {
                        localmaxhp_ += soc.get("incMHP");
                        localmaxmp_ += soc.get("incMMP");
                        handleItemOption(soc, chra, first_login, sData);
                    }
                }
            }
        }

//        ii.getSetItemInfoEffs().stream().filter(integer -> ).forEach(integer -> {
//            if () {
//
//            }
//        });
        handleProfessionTool(chra);
        for (Item item : chra.getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getItemId() / 100000 == 52) {
                if (this.expMod < 3.0 && item.getItemId() == 5211060) {
                    this.expMod = 3.0;
                    continue;
                }
                if (this.expMod < 2.0 && (item.getItemId() == 5210000 || item.getItemId() == 5210001 || item.getItemId() == 5210002 || item.getItemId() == 5210003 || item.getItemId() == 5210004 || item.getItemId() == 5210005 || item.getItemId() == 5210006 || item.getItemId() == 5211047)) {
                    this.expMod = 2.0;
                    continue;
                }
                if (this.expMod < 1.5 && (item.getItemId() == 5211063 || item.getItemId() == 5211064 || item.getItemId() == 5211065 || item.getItemId() == 5211066 || item.getItemId() == 5211069 || item.getItemId() == 5211070)) {
                    this.expMod = 1.5;
                    continue;
                }
                if (this.expMod < 1.2 && (item.getItemId() == 5211071 || item.getItemId() == 5211072 || item.getItemId() == 5211073 || item.getItemId() == 5211074 || item.getItemId() == 5211075 || item.getItemId() == 5211076 || item.getItemId() == 5211067)) {
                    this.expMod = 1.2;
                }

//                double rate = ii.getExpCardRate(item.getItemId());
//                if (item.getItemId() != 5210009 && rate > 1) {
//                    if (!ii.isExpOrDropCardTime(item.getItemId()) || chra.getLevel() > ii.getExpCardMaxLevel(item.getItemId()) || (item.getExpiration() == -1)) {
//                        if (item.getExpiration() == -1) {
//                            chra.dropMessage(5, ii.getName(item.getItemId()) + "属性错误，经验加成无效！");
//                        }
//                        continue;
//                    }
//                    if (expMod < rate) {
//                        expMod = rate;
//                    }
//                }
            } else if (dropMod == 1 && item.getItemId() / 10000 == 536) {
                if (item.getItemId() == 5360000 || item.getItemId() == 5360014 || item.getItemId() == 5360015 || item.getItemId() == 5360016) {
                    dropMod = 2;
                }

//                if (item.getItemId() >= 5360000 && item.getItemId() < 5360100) {
//                    if (!ii.isExpOrDropCardTime(item.getItemId()) || (item.getExpiration() == -1)) {
//                        if (item.getExpiration() == -1) {
//                            chra.dropMessage(5, ii.getName(item.getItemId()) + "属性错误，经验加成无效！");
//                        }
//                        continue;
//                    }
//                    dropMod = 2;
//                }
            } else if (item.getItemId() == 5650000) {
                hasPartyBonus = true;
            } else if (item.getItemId() == 5590001) { //高级装备特许证 - 拥有装备特许证后可以装备比自己等级高#c10级#的装备
                levelBonus = 10;
            } else if (levelBonus == 0 && item.getItemId() == 5590000) { //装备特许证 - 拥有装备特许证后可以装备比自己等级高#c5级#的装备
                levelBonus = 5;
            } else if (item.getItemId() == 5710000) {
                questBonus = 2;
            } else if (item.getItemId() == 5340000) { //钓竿 - 用来捕鱼的重要装备
                canFish = true;
            } else if (item.getItemId() == 5340001) { //高级鱼竿 - 用更坚韧的材料制成的钓鱼竿，可以加快钓鱼的速度
                canFish = true;
                canFishVIP = true;
            }
        }
        for (Item item : chra.getInventory(MapleInventoryType.ETC).list()) { //omfg;
            switch (item.getItemId()) { //暂时不开放以下功能
                case 4030003: //俄罗斯方块
                    //pickupRange = Double.POSITIVE_INFINITY;
                    break;
                case 4030004: //俄罗斯方块
                    //hasClone = true;
                    break;
                case 4030005: //俄罗斯方块
                    //cashMod = 2;
                    break;
            }
        }
        if (first_login && chra.getLevel() >= 30) {
            //大天使,黑天使,白色天使
            int[] skills = {1085, 1087, 1179};
            for (int skillId : skills) {
                if (chra.isGM()) { //!job lol
                    for (int allJob : allJobs) {
                        sData.put(skillId + allJob, new SkillEntry((byte) -1, (byte) 0, -1));
                    }
                } else {
                    sData.put(getSkillByJob(skillId, chra.getJob()), new SkillEntry((byte) -1, (byte) 0, -1));
                }
            }
        }
        if (equippedSummon > 0) {
            equippedSummon = getSkillByJob(equippedSummon, chra.getJob());
        }
        /*
         * 宝盒属性加成
         */
        if (chra.getCoreAura() != null) {
            watk += chra.getCoreAura().getWatk();
            magic += chra.getCoreAura().getMagic();
            localstr += chra.getCoreAura().getStr();
            localdex += chra.getCoreAura().getDex();
            localint_ += chra.getCoreAura().getInt();
            localluk += chra.getCoreAura().getLuk();
        }
        /*
         * 技能加属性
         */
        handlePassiveSkills(chra);
        /*
         * add to localmaxhp_ if percentage plays a role in it, else add_hp
         * 添加BUFF加属性效果
         */
        handleBuffStats(chra);
        Integer buff = chra.getBuffedValue(MapleBuffStat.增强_MAXHP);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增强_MAXMP);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加最大HP);
        if (buff != null) {
            localmaxhp_ += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加最大MP);
        if (buff != null) {
            localmaxmp_ += buff;
        }
        MapleStatEffect eff = chra.getStatForBuff(MapleBuffStat.进阶祝福);
        if (eff != null) {
            watk += eff.getX();
            magic += eff.getY();
            accuracy += eff.getV();
            mpconReduce += eff.getMPConReduce();
        }
        /*
         * 特殊的技能加成
         */
        Skill bx;
        int bof;
        bx = SkillFactory.getSkill(船长.坚忍不拔); //5210012 - 坚忍不拔 - 凭借大副特有的韧劲，永久增加物理、魔法防御力、最大HP和MP。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            percent_wdef += eff.getWDEFRate();
            percent_mdef += eff.getMDEFRate();
            localmaxhp_ += eff.getMaxHpX();
            localmaxmp_ += eff.getMaxMpX();
        }
        bx = SkillFactory.getSkill(船长.指挥船员); //5210012 - 坚忍不拔 - 凭借大副特有的韧劲，永久增加物理、魔法防御力、最大HP和MP。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localmaxhp_ += eff.getMaxHpX();
        }
//        bx = SkillFactory.getSkill(尖兵.高效输能); //36101003 - 高效输能 - 在一定时间内提高能量流的利用效率，增加最大HP和最大MP。同时可以获得增加最大HP和最大MP的被动效果。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null) {
//            eff = bx.getEffect(bof);
//            localmaxhp_ += eff.getMaxHpX();
//            localmaxmp_ += eff.getMaxMpX();
//        }
        bx = SkillFactory.getSkill(爆莉萌天使.内心平和); //65110005 - 内心平和 - 内心的平和，使得自身不会受到外界冲击的干扰。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            wdef += eff.getWdefX();
            mdef += eff.getMdefX();
            localmaxhp_ += eff.getMaxHpX();
        }
        bx = SkillFactory.getSkill(恶魔复仇者.心灵之力);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localmaxhp_ += eff.getMaxHpX();
        }
        bx = SkillFactory.getSkill(魂骑士.灵魂守卫);
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localmaxhp_ += eff.getMaxHpX();
        }
        /*
         * 家族技能加属性
         */
        if (chra.getGuildId() > 0) {
            MapleGuild g = WorldGuildService.getInstance().getGuild(chra.getGuildId());
            if (g != null && g.getSkills().size() > 0) {
                long now = System.currentTimeMillis();
                for (MapleGuildSkill gs : g.getSkills()) {
                    Skill skill = SkillFactory.getSkill(gs.skillID);
                    if (skill != null && gs.timestamp > now && gs.activator.length() > 0) {
                        MapleStatEffect e = skill.getEffect(gs.level);
                        passive_sharpeye_rate += e.getCritical();
                        watk += e.getAttackX();
                        magic += e.getMagicX();
                        expBuff *= (e.getEXPRate() + 100.0) / 100.0;
                        dodgeChance += e.getER();
                        percent_wdef += e.getWDEFRate();
                        percent_mdef += e.getMDEFRate();
                    }
                }
            }
        }
        /*
         * 角色卡系统加属性
         */
        List<Integer> cardSkills = new LinkedList<>();
        for (Pair<Integer, Integer> ix : chra.getCharacterCard().getCardEffects()) {
            Skill skill = SkillFactory.getSkill(ix.getLeft());
            if (skill == null) {
                continue;
            }
            MapleStatEffect cardEff = skill.getEffect(ix.getRight());
            if (cardEff == null || (cardSkills.contains(ix.getLeft()) && ix.getLeft() < 71001100)) {
                continue;
            }
            cardSkills.add(ix.getLeft());
            percent_wdef += cardEff.getWDEFRate();
            damX += (cardEff.getLevelToWatkX() * chra.getLevel() * 0.5);
            damX += (cardEff.getLevelToMatkX() * chra.getLevel() * 0.5);
            percent_hp += cardEff.getPercentHP();
            percent_mp += cardEff.getPercentMP();
            RecoveryUP += cardEff.getMPConsumeEff();
            percent_acc += cardEff.getPercentAcc();
            passive_sharpeye_rate += cardEff.getCritical();
            jump += cardEff.getPassiveJump();
            speed += cardEff.getPassiveSpeed();
            dodgeChance += cardEff.getPercentAvoid();
            damX += (cardEff.getLevelToDamageX() * chra.getLevel());
            BuffUP_Summon += cardEff.getSummonTimeInc();
            expLossReduceR += cardEff.getEXPLossRate();
            ASR += cardEff.getASRRate();
            //ignoreMobDamR
            suddenDeathR += (cardEff.getSuddenDeathR() * 0.5);
            BuffUP_Skill += cardEff.getBuffTimeRate();
            //onHitHpRecoveryR
            //onHitMpRecoveryR
            coolTimeR += cardEff.getCooltimeReduceR();
            incMesoProp += cardEff.getMesoAcquisition();
            damX += Math.floor((cardEff.getHpToDamageX() * oldmaxhp) / 100.0f);
            damX += Math.floor((cardEff.getMpToDamageX() * oldmaxhp) / 100.0f);
            //finalAttackDamR
            passive_sharpeye_max_percent += cardEff.getCriticalMax();
            percent_ignore_mob_def_rate += cardEff.getIgnoreMob();
            localstr += cardEff.getStrX();
            localdex += cardEff.getDexX();
            localint_ += cardEff.getIntX();
            localluk += cardEff.getLukX();
            indieStrFX += cardEff.getStrFX();
            indieDexFX += cardEff.getDexFX();
            indieIntFX += cardEff.getIntFX();
            indieLukFX += cardEff.getLukFX();
            localmaxhp_ += cardEff.getMaxHpX();
            localmaxmp_ += cardEff.getMaxMpX();
            watk += cardEff.getAttackX();
            magic += cardEff.getMagicX();
            percent_boss_damage_rate += cardEff.getBossDamage();
        }
        cardSkills.clear();
        /*
         * 内在能力技能加属性
         */
        for (int i = 0; i < 3; i++) {
            InnerSkillEntry innerSkill = chra.getInnerSkills()[i];
            if (innerSkill == null) {
                continue;
            }
            MapleStatEffect InnerEffect = SkillFactory.getSkill(innerSkill.getSkillId()).getEffect(innerSkill.getSkillLevel());
            if (InnerEffect == null) {
                continue;
            }
            wdef += InnerEffect.getWdefX();
            mdef += InnerEffect.getWdefX();
            percent_wdef += InnerEffect.getWDEFRate();
            percent_mdef += InnerEffect.getMDEFRate();
            percent_hp += InnerEffect.getPercentHP();
            percent_mp += InnerEffect.getPercentMP();
            accuracy += InnerEffect.getAccX(); //命中值
            dodgeChance += InnerEffect.getPercentAvoid();
            passive_sharpeye_rate += InnerEffect.getCritical();
            jump += InnerEffect.getPassiveJump();
            speed += InnerEffect.getPassiveSpeed();
            indieStrFX += InnerEffect.getStrFX();
            indieDexFX += InnerEffect.getDexFX();
            indieIntFX += InnerEffect.getIntFX();
            indieLukFX += InnerEffect.getLukFX();
            localmaxhp_ += InnerEffect.getMaxHpX();
            localmaxmp_ += InnerEffect.getMaxMpX();
            watk += InnerEffect.getAttackX();
            magic += InnerEffect.getMagicX();
            BuffUP_Skill += InnerEffect.getBuffTimeRate();
            if (InnerEffect.getDexToStr() > 0) {
                indieStrFX += Math.floor((getDex() * InnerEffect.getDexToStr()) / 100.0f);
            }
            if (InnerEffect.getStrToDex() > 0) {
                indieDexFX += Math.floor((getStr() * InnerEffect.getStrToDex()) / 100.0f);
            }
            if (InnerEffect.getIntToLuk() > 0) {
                indieLukFX += Math.floor((getInt() * InnerEffect.getIntToLuk()) / 100.0f);
            }
            if (InnerEffect.getLukToDex() > 0) {
                indieDexFX += Math.floor((getLuk() * InnerEffect.getLukToDex()) / 100.0f);
            }
            if (InnerEffect.getLevelToWatk() > 0) {
                watk += Math.floor(chra.getLevel() / InnerEffect.getLevelToWatk());
            }
            if (InnerEffect.getLevelToMatk() > 0) {
                magic += Math.floor(chra.getLevel() / InnerEffect.getLevelToMatk());
            }
            percent_boss_damage_rate += InnerEffect.getBossDamage();
        }
        if (JobConstants.is尖兵(chra.getJob())) {
            double d = chra.getSpecialStat().getPowerCount() / 100.0;
            localstr += d * str;
            localdex += d * dex;
            localluk += d * luk;
            localint_ += d * int_;
            int[] skillIds = {
                    尖兵.多线程Ⅰ, //30020234 - 多线程Ⅰ - 直接用AP提高的能力值达到一定数值以上时，获得各个能力值对应的特定奖励。
                    尖兵.多线程Ⅱ, //36000004 - 多线程Ⅱ - 直接用AP提高的能力值达到一定数值以上时，获得各个能力值对应的特定奖励。
                    尖兵.多线程Ⅲ, //36100007 - 多线程Ⅲ - 直接用AP提高的能力值达到一定数值以上时，获得各个能力值对应的奖励。\n[需要技能]：#c多线程II1级以上#
                    尖兵.多线程Ⅳ, //36110007 - 多线程Ⅳ - 直接用AP提高的能力值达到一定数值以上时，获得各个能力值对应的奖励。\n[需要技能]：#c多线程III1级以上#
                    尖兵.多线程Ⅴ //36120010 - 多线程Ⅴ - 直接用AP提高的能力值达到一定数值以上时，获得各个能力值对应的奖励。\n[需要技能]：#c多线程IV1级以上#
            };
            for (int i : skillIds) {
                bx = SkillFactory.getSkill(i);
                if (bx != null) {
                    bof = chra.getSkillLevel(bx);
                    if (bof > 0) {
                        eff = bx.getEffect(bof);
                        if (localdex >= eff.getX()) {
                            ASR += eff.getZ();
                            TER += eff.getZ();
                        }
                        if (localluk >= eff.getX()) {
                            dodgeChance += eff.getZ();
                        }
                        if (localstr >= eff.getX() && localdex >= eff.getX() && localluk >= eff.getX()) {
                            percent_damage_rate += eff.getW();
                            percent_boss_damage_rate += eff.getW();
                        }
                        percent_hp += eff.getS();
                        percent_mp += eff.getS();
                    }
                }
            }
        }
        localstr += Math.floor((localstr * percent_str) / 100.0f) + indieStrFX;
        localdex += Math.floor((localdex * percent_dex) / 100.0f) + indieDexFX;
        localint_ += Math.floor((localint_ * percent_int) / 100.0f) + indieIntFX;
        localluk += Math.floor((localluk * percent_luk) / 100.0f) + indieLukFX;
        if (localint_ > localdex) {
            accuracy += Math.floor((localint_ * 1.6) + (localluk * 0.8) + (localdex * 0.4));
        } else {
            accuracy += Math.floor((localdex * 1.6) + (localluk * 0.8) + (localstr * 0.4));
        }
        watk += Math.floor((watk * percent_atk) / 100.0f);
        magic += Math.floor((magic * percent_matk) / 100.0f);
        localint_ += Math.floor((localint_ * percent_matk) / 100.0f);
        //计算物理防御和魔法防御
        wdef += Math.floor((localstr * 1.5) + ((localdex + localluk) * 0.4));
        mdef += Math.floor((localint_ * 1.5) + ((localdex + localluk) * 0.4));
        wdef += chra.getTrait(MapleTraitType.will).getLevel();
        mdef += chra.getTrait(MapleTraitType.will).getLevel();
        wdef += Math.min(9999, Math.floor((wdef * percent_wdef) / 100.0f));
        mdef += Math.min(9999, Math.floor((mdef * percent_mdef) / 100.0f));

        calculateFame(chra);
        percent_ignore_mob_def_rate += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        pvpDamage += chra.getTrait(MapleTraitType.charisma).getLevel() / 10;
        ASR += chra.getTrait(MapleTraitType.will).getLevel() / 5;
        //计算命中值
        accuracy += chra.getTrait(MapleTraitType.insight).getLevel() * 15 / 10;
        accuracy += Math.min(9999, Math.floor((accuracy * percent_acc) / 100.0f));

        //计算最大Hp先算倾向系统 在算技能增加上限 最后被动技能增加
        localmaxhp_ += chra.getTrait(MapleTraitType.will).getLevel() / 5 * 100;
        localmaxhp_ += addmaxhp;
        localmaxhp_ += Math.floor((percent_hp * localmaxhp_) / 100.0f);
        localmaxhp = Math.min(chra.getMaxHpForSever(), Math.abs(Math.max(-chra.getMaxHpForSever(), localmaxhp_)));

        //计算最大Mp先算倾向系统 在算被动技能增加 最后计算技能增加上限
        localmaxmp_ += chra.getTrait(MapleTraitType.sense).getLevel() / 5 * 100;
        localmaxmp_ += Math.floor((percent_mp * localmaxmp_) / 100.0f);
        localmaxmp_ += addmaxmp;
        localmaxmp = Math.min(chra.getMaxMpForSever(), Math.abs(Math.max(-chra.getMaxMpForSever(), localmaxmp_)));

        if (chra.getEventInstance() != null && chra.getEventInstance().getName().startsWith("PVP")) {
            localmaxhp = Math.min(40000, localmaxhp * 3); //approximate.
            localmaxmp = Math.min(20000, localmaxmp * 2);
            //not sure on 20000 cap
            for (int i : pvpSkills) {
                Skill skil = SkillFactory.getSkill(i);
                if (skil != null && skil.canBeLearnedBy(chra.getJob())) {
                    sData.put(i, new SkillEntry((byte) 1, (byte) 0, -1));
                    eff = skil.getEffect(1);
                    switch ((i / 1000000) % 10) {
                        case 1:
                            if (eff.getX() > 0) {
                                pvpDamage += (wdef / eff.getX());
                            }
                            break;
                        case 3:
                            hpRecoverProp += eff.getProp();
                            hpRecover += eff.getX();
                            mpRecoverProp += eff.getProp();
                            mpRecover += eff.getX();
                            break;
                        case 5:
                            passive_sharpeye_rate += eff.getProp();
                            passive_sharpeye_max_percent = 100;
                            break;
                    }
                    break;
                }
            }
            eff = chra.getStatForBuff(MapleBuffStat.变身效果);
            if (eff != null && eff.getSourceid() % 10000 == 1105) { //ice knight
                localmaxhp = 500000;
                localmaxmp = 500000;
            }
        }
        /*
         * 增加或改变技能等级
         */
        chra.changeSkillLevel_Skip(sData, false);
        /*
         * 恶魔职业的Mp设置
         */
        if (JobConstants.is恶魔猎手(chra.getJob())) {
            localmaxmp = GameConstants.getMPByJob(chra.getJob());
            localmaxmp += incMaxDF;
        } else if (JobConstants.is神之子(chra.getJob())) {
            localmaxmp = 100;
        } else if (JobConstants.isNotMpJob(chra.getJob())) {
            localmaxmp = 10;
        }
        CalcPassive_SharpEye(chra);
        CalcPassive_Mastery(chra);
        recalcPVPRank(chra);
        if (first_login) {
            chra.silentEnforceMaxHpMp();
            relocHeal(chra);
        } else {
            chra.enforceMaxHpMp();
        }
        calculateMaxBaseDamage(Math.max(magic, watk), damX, pvpDamage, chra);
        trueMastery = Math.min(100, trueMastery);
        passive_sharpeye_min_percent = (short) Math.min(passive_sharpeye_min_percent, passive_sharpeye_max_percent);
        if (oldmaxhp != 0 && oldmaxhp != localmaxhp) {
            chra.updatePartyMemberHP();
            chra.checkBloodContract();
        }
        //System.out.println("装备潜能道具掉落几率 :" + incRewardProp + " 最后爆率: " + getDropBuff());
    }

    public double getDropBuff() {
        if (incRewardProp > 100.0) {
            incRewardProp = 100.0;
        }
        return dropBuff + incRewardProp;
    }

    public List<Triple<Integer, String, Integer>> getPsdSkills() {
        return psdSkills;
    }

    private void handlePassiveSkills(MapleCharacter chra) {
        Skill bx;
        int bof;
        MapleStatEffect eff;
        if (JobConstants.is骑士团(chra.getJob())) {
            bx = SkillFactory.getSkill(10000074); //女皇的呼唤 - 学习技能后，由于女皇的呼唤，最大HP和最大MP永久增加。
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                eff = bx.getEffect(bof);
                percent_hp += eff.getX();
                percent_mp += eff.getX();
            }
            bx = SkillFactory.getSkill(初心者.元素和声_力量);
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                localstr += chra.getLevel() / 2;
            }
            bx = SkillFactory.getSkill(初心者.元素和声_敏捷);
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                localdex += chra.getLevel() / 2;
            }
            bx = SkillFactory.getSkill(初心者.元素和声_智力);
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                localint_ += chra.getLevel() / 2;
            }
            bx = SkillFactory.getSkill(初心者.元素和声_运气);
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                localluk += chra.getLevel() / 2;
            }
        }
        psdSkills.clear();
        for (Integer sk : chra.getSkills().keySet()) {
            Skill skill = SkillFactory.getSkill(sk);
            if (skill != null && skill.getPsd() == 1) {
                Triple<Integer, String, Integer> psdSkill = new Triple<>(0, "", 0);
                psdSkill.left = skill.getPsdSkill();
                psdSkill.mid = skill.getPsdDamR();
                psdSkill.mid = skill.getTargetPlus();
                psdSkill.right = skill.getId();
                psdSkills.add(psdSkill);
            }
        }
        switch (chra.getJob()) {
            case 100:
            case 110:
            case 111:
            case 112:
            case 120:
            case 121:
            case 122:
            case 130:
            case 131:
            case 132: {
                bx = SkillFactory.getSkill(战士.圣甲术); //1001003 - 圣甲术 - 在一定时间内增加自身的物理防御力，永久增加自身一定比率的最大体力，受到一定范围内的攻击时，减少所受的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(战士.战士精通); //1000009 - 战士精通 - 提高战士的基本素养增加移动速度和跳跃力、最大体力、最大移动速度。受到敌人攻击时，有一定的几率不会被击退。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addmaxhp += eff.getLevelToMaxHp() * chra.getLevel(); //等级乘以倍率
                    jump += eff.getPassiveJump(); //移动速度上限
                    speedMax += eff.getSpeedMax(); //跳跃力
                }
                break;
            }
            case 200:
            case 210:
            case 211:
            case 212:
            case 220:
            case 221:
            case 222:
            case 230:
            case 231:
            case 232: {
                bx = SkillFactory.getSkill(魔法师.MP增加); //2000006 - MP增加 - 永久增加最大MP，根据等级MP也会额外的增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                    addmaxmp += eff.getLevelToMaxMp() * chra.getLevel(); //等级乘以倍率
                }
                break;
            }
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322: {
                bx = SkillFactory.getSkill(弓箭手.弓箭手精通); //弓箭手精通 - 熟习弓箭手的基本技能。提高命中值、回避值、射程、移动速度、最大移动速度上限。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    defRange += eff.getRange();
                    accuracy += eff.getAcc();
                }
                break;
            }
            case 400:
            case 410:
            case 411:
            case 412:
            case 420:
            case 421:
            case 422:
            case 431:
            case 432:
            case 433:
            case 434: {
                bx = SkillFactory.getSkill(飞侠.增益偷取); //4000010 - 增益偷取 - (无描述)
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                }
                bx = SkillFactory.getSkill(飞侠.轻功); //4001005 - 轻功 - 在一定时间内增加全体队员的移动速度和跳跃力，移动速度永久提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    speed += eff.getSpeed();
                    speedMax += eff.getSpeedMax();
                }
                bx = SkillFactory.getSkill(飞侠.侧移); //4000012 - 侧移 - 永久提高回避率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                break;
            }
            case 500:
            case 510:
            case 511:
            case 512:
            case 520:
            case 521:
            case 522: {
                bx = SkillFactory.getSkill(海盗.快动作); //5000000 - 快动作 - 永久增加命中值、移动速度上限、跳跃力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX(); //命中值
                    jump += eff.getPassiveJump(); //移动速度上限
                    speedMax += eff.getSpeedMax(); //跳跃力
                }
                break;
            }
        }
        /*
         * ---------------------------------
         * 新手技能和特殊技能
         * ---------------------------------
         */
        bx = SkillFactory.getSkill(80000000); //海盗祝福 - [链接技能]学习火炮手特有的强韧，永久性地提高各种属性。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getPercentHP();
            percent_mp += eff.getPercentMP();
        }
        bx = SkillFactory.getSkill(80000001); //恶魔之怒 - [链接技能]对象是BOSS怪时，唤醒内心的愤怒，造成更强的伤害。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
        }
        bx = SkillFactory.getSkill(80000002); //致命本能 - 拥有通过卓越的洞察力，找到敌人致命弱点的本能。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            passive_sharpeye_rate += bx.getEffect(bof).getCritical();
        }
        bx = SkillFactory.getSkill(80000005); //穿透 - 用穿透一切阻碍的光之力量，无视敌人的部分防御力。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
        }
        bx = SkillFactory.getSkill(80000006); //钢铁之墙 - 具有比狂龙战士更出色的体力。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            percent_hp += bx.getEffect(bof).getPercentHP();
        }
        int[] skillIds = {
                80000007, //80000007 - Lv.1 魔族的高贵祝福 - 用魔族的气息提升自己的物理攻击力和魔法攻击力各3点。
                80000008, //80000008 - Lv.2 魔族的高贵祝福 - 用魔族的气息提升自己的物理攻击力和魔法攻击力各5点。
                80000009, //80000009 - Lv.3 魔族的高贵祝福 - 用魔族的气息提升自己的物理攻击力和魔法攻击力各7点。
                80000013, //80000013 - Lv.1 鲨鱼的气息 - 通过鲨鱼的气息，将自身的物理攻击力和魔法攻击力各提升3
                80000014, //80000014 - Lv.2 鲨鱼的气息 - 通过鲨鱼的气息，将自身的物理攻击力和魔法攻击力各提升5
                80000015, //80000015 - Lv.3 鲨鱼的气息 - 通过鲨鱼的气息，将自身的物理攻击力和魔法攻击力各提升7
                80000017, //80000017 - Lv.1 品克缤狂人 - 通过品克缤的可爱，将自身的物理攻击力和魔法攻击力各提升3
                80000018, //80000018 - Lv.2 品克缤狂人 - 通过品克缤的可爱，将自身的物理攻击力和魔法攻击力各提升5
                80000019, //80000019 - Lv.3 品克缤狂人 - 通过品克缤的可爱，将自身的物理攻击力和魔法攻击力各提升7
                80000020, //80000020 - Lv.1 龙的惊人祝福 - 通过龙的气息，将自身的物理攻击力和魔法攻击力各提升3
                80000021, //80000021 - Lv.2 龙的惊人祝福 - 通过龙的气息，将自身的物理攻击力和魔法攻击力各提升5
                80000022, //80000022 - Lv.3 龙的惊人祝福 - 通过龙的气息，将自身的物理攻击力和魔法攻击力各提升7
                80000026, //80000026 - Lv.1 因为熊 - 因为熊～因为熊～增益是因为熊！使自己的物理攻击力和魔法攻击力各提高3点。
                80000027, //80000027 - Lv.2 因为熊 - 因为熊～因为熊～增益是因为熊！使自己的物理攻击力和魔法攻击力各提高5点。
                80000028, //80000028 - Lv.3 因为熊 - 因为熊～因为熊～增益是因为熊！使自己的物理攻击力和魔法攻击力各提高7点。
                80000029, //80000029 - Lv.1 南瓜魔法 - 惊人的南瓜魔法！使自己的物理攻击力和魔法攻击力各提高3点。
                80000030, //80000030 - Lv.2 南瓜魔法 - 惊人的南瓜魔法！使自己的物理攻击力和魔法攻击力各提高5点。
                80000031, //80000031 - Lv.3 南瓜魔法 - 惊人的南瓜魔法！使自己的物理攻击力和魔法攻击力各提高7点。
                80000056, //80000056 - Lv.1 考拉的声援 - 可爱的考拉的声援！使自己的物理攻击力和魔法攻击力各增加3。
                80000057, //80000057 - Lv.2 考拉的声援 - 可爱的考拉的声援！使自己的物理攻击力和魔法攻击力各增加5。
                80000058, //80000058 - Lv.3 考拉的声援 - 可爱的考拉的声援！使自己的物理攻击力和魔法攻击力各增加7。
                80000063, //80000063 - Lv.1 松鼠变强 - 和可爱的松鼠一起变强吧！使自己的物理攻击力和魔法攻击力各增加4。
                80000064, //80000064 - Lv.2松鼠变强 - 和可爱的松鼠一起变强吧！使自己的物理攻击力和魔法攻击力各增加6。
                80000065, //80000065 - Lv.3松鼠变强 - 和可爱的松鼠一起变强吧！使自己的物理攻击力和魔法攻击力各增加8。
                80000072, //80000072 - Lv.1 New魔族的高贵祝福 - 由于魔族的气息，自己的物理攻击力和魔法攻击力各增加3。
                80000073, //80000073 - Lv.2 New魔族的高贵祝福 - 由于魔族的气息，自己的物理攻击力和魔法攻击力各增加5。
                80000074, //80000074 - Lv.3 New魔族的高贵祝福 - 由于魔族的气息，自己的物理攻击力和魔法攻击力各增加7。
                80000077, //80000077 - Lv.1 天使的神圣祝福 - 身体被天使神圣的气息所包围。自己的物理攻击力和魔法攻击力各提高3。
                80000078, //80000078 - Lv.2 天使的神圣祝福 - 身体被天使的神圣气息所包围。自己的物理攻击力和魔法攻击力各提高5。
                80000079, //80000079 - Lv.3 天使的神圣祝福 - 身体被天使的神圣气息所包围。自己的物理攻击力和魔法攻击力各提高7。
                80000081, //80000081 - Lv.1 鲁提鲁提 - 让人无法抗拒的鲁提的可爱俏皮。自己的物理攻击力和魔法攻击力各提高3。
                80000082, //80000082 - Lv.2 鲁提鲁提 - 让人无法抗拒的鲁提的可爱俏皮。自己的物理攻击力和魔法攻击力各提高5。
                80000083, //80000083 - Lv.3 鲁提鲁提 - 让人无法抗拒的鲁提的可爱俏皮。自己的物理攻击力和魔法攻击力各提高7。
                80000098, //80000098 - Lv.1 邪恶气息 - 身体被恶魔的邪恶气息所包围。自己的物理攻击力和魔法攻击力各提高3。
                80000099, //80000099 - Lv.2 邪恶气息 - 身体被恶魔的邪恶气息所包围。自己的物理攻击力和魔法攻击力各提高5。
                80000100, //80000100 - Lv.3 邪恶气息 - 身体被恶魔的邪恶气息所包围。自己的物理攻击力和魔法攻击力各提高7。
                80000101, //80000101 - Lv.1 难以置信！ - 鲁塔比斯的巨大气息袭来。自己的物理攻击力和魔法攻击力各提高3。
                80000102, //80000102 - Lv.2 难以置信！ - 鲁塔比斯的巨大气息袭来。自己的物理攻击力和魔法攻击力各提高5。
                80000103, //80000103 - Lv.3 难以置信！ - 鲁塔比斯的巨大气息袭来。自己的物理攻击力和魔法攻击力各提高7。
                80000111, //80000111 - Lv.1 身强力壮的铠鼠 - 身强力壮的铠鼠今天又来了。自己的物理攻击力和魔法攻击力各提高3。
                80000112, //80000112 - Lv.2 身强力壮的铠鼠 - 身强力壮的铠鼠今天又来了。自己的物理攻击力和魔法攻击力各提高5。
                80000113, //80000113 - Lv.3 身强力壮的铠鼠 - 身强力壮的铠鼠今天又来了。自己的物理攻击力和魔法攻击力各提高7。
                80000120, //80000120 - Lv.1 布迪军团长宠物 - 见识一下布迪军团长的力量。自己的物理攻击力和魔法攻击力各提高3。
                80000121, //80000121 - Lv.2 布迪军团长宠物 - 见识一下布迪军团长的力量。自己的物理攻击力和魔法攻击力各提高5。
                80000122, //80000122 - Lv.3 布迪军团长宠物 - 见识一下布迪军团长的力量。自己的物理攻击力和魔法攻击力各提高7。
        };
        for (int i : skillIds) {
            bx = SkillFactory.getSkill(i);
            if (bx != null) {
                bof = chra.getSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    magic += eff.getMagicX();
                }
            }
        }
        bx = SkillFactory.getSkill(80000025); //国庆闪光肩饰 - 可以激发国庆节特殊属性。
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
            jump += eff.getPassiveJump();
            speed += eff.getPassiveSpeed();
        }
        bx = SkillFactory.getSkill(80000050); //80000050 - 野性狂怒 - 由于愤怒，伤害增加。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            percent_damage += eff.getDAMRate();
        }
        bx = SkillFactory.getSkill(80001040); //精灵的祝福 - [链接技能]获得古代精灵的祝福，可以回到埃欧雷去，经验值获得量永久提高
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) { //去掉这个
            //expBuff *= (bx.getEffect(bof).getEXPRate() + 100.0) / 100.0;
        }
        bx = SkillFactory.getSkill(80000047); //80000047 - 混合逻辑 - 采用混合逻辑设计，所有能力值永久提高。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localstr += (eff.getStrRate() / 100.0) * str;
            localdex += (eff.getDexRate() / 100.0) * dex;
            localluk += (eff.getLukRate() / 100.0) * luk;
            localint_ += (eff.getIntRate() / 100.0) * int_;
        }
        bx = SkillFactory.getSkill(80010006); //80010006 - 精灵集中 - 攻击BOSS怪时,精灵之力会更强。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            percent_hp += eff.getPercentHP();
            percent_mp += eff.getPercentMP();
            passive_sharpeye_rate += eff.getCritical();
            percent_boss_damage_rate += eff.getBossDamage();
        }
        if (JobConstants.is冒险家(chra.getJob())) {
            bx = SkillFactory.getSkill(74); //女皇的强化 - 可以装备比自己等级高的装备
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                levelBonus += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(80); //女皇的强化 - 可以装备比自己等级高的装备
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                levelBonus += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(10074); //未知 - 可以装备比自己等级高的装备
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                levelBonus += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(10080); //未知 - 可以装备比自己等级高的装备
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                levelBonus += bx.getEffect(bof).getX();
            }
            bx = SkillFactory.getSkill(110); //海盗祝福 - [种族特性技能]强化火炮手特有的坚韧，永久提高各种属性
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getPercentHP();
                percent_mp += eff.getPercentMP();
            }
            bx = SkillFactory.getSkill(10110); //未知 - 永久提高各种属性
            bof = chra.getSkillLevel(bx);
            if (bof > 0 && bx != null) {
                eff = bx.getEffect(bof);
                localstr += eff.getStrX();
                localdex += eff.getDexX();
                localint_ += eff.getIntX();
                localluk += eff.getLukX();
                percent_hp += eff.getHpR();
                percent_mp += eff.getMpR();
            }
        }
        /*
         * 精灵的祝福 和 女皇的祝福 属性加成
         * 精灵的祝福和女皇的祝福只发动等级更高的效果。
         */
        Skill skillBof = SkillFactory.getSkill(JobConstants.getBOF_ForJob(chra.getJob()));
        Skill skillEmpress = SkillFactory.getSkill(JobConstants.getEmpress_ForJob(chra.getJob()));
        if (chra.getSkillLevel(skillBof) > 0 || chra.getSkillLevel(skillEmpress) > 0) {
            if (chra.getSkillLevel(skillBof) > chra.getSkillLevel(skillEmpress)) { //启用 精灵的祝福属性加成
                eff = skillBof.getEffect(chra.getSkillLevel(skillBof));
                watk += eff.getX();
                magic += eff.getY();
                accuracy += eff.getX();
            } else {  //启用 女皇的祝福属性加成
                eff = skillEmpress.getEffect(chra.getSkillLevel(skillEmpress));
                watk += eff.getX();
                magic += eff.getY();
                accuracy += eff.getX();
            }
        }
        switch (chra.getJob()) {
            case 110:
            case 111:
            case 112: {
                bx = SkillFactory.getSkill(英雄.物理训练); //1100009 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(英雄.抵抗力); //1110011 - 抵抗力 - 强化自身的状态异常抵抗和对所有属性的抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                bx = SkillFactory.getSkill(英雄.乘胜追击); //1110009 - 乘胜追击 - 攻击昏迷、暗黑、结冰状态的敌人时，可以造成更大的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getDamage();
                    percent_boss_damage_rate += eff.getDamage();
                }
                bx = SkillFactory.getSkill(英雄.战斗精通); //1120012 - 战斗精通 - 攻击时一定程度无视怪物的物理防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(英雄.进阶终极攻击); //1120013 - 进阶终极攻击 - 永久性地增加攻击力和命中率，终极武器技能的发动概率和伤害大幅上升。\n需要技能：#c终极剑斧20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                /*
                 * 英雄超级技能
                 */
                bx = SkillFactory.getSkill(英雄.进阶斗气_强化); //1120043 - 进阶斗气-强化 - 增加每1个斗气点数的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    MapleStatEffect 斗气效果 = chra.getStatForBuff(MapleBuffStat.斗气集中);
                    Integer 斗气状态 = chra.getBuffedValue(MapleBuffStat.斗气集中);
                    if (斗气效果 != null && 斗气状态 != null) {
                        percent_damage_rate += eff.getDAMRate() * (斗气状态 - 1);
                        percent_boss_damage_rate += eff.getDAMRate() * (斗气状态 - 1);
                    }
                }
                bx = SkillFactory.getSkill(英雄.进阶斗气_额外机会); //1120044 - 进阶斗气-额外机会 - 提高一次积累2个斗气点数的概率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addSkillProp(英雄.斗气集中, bx.getEffect(bof).getProp());
                    addSkillProp(英雄.进阶斗气, bx.getEffect(bof).getProp());
                }
                bx = SkillFactory.getSkill(英雄.进阶斗气_BOSS杀手); //1120045 - 进阶斗气-BOSS杀手 - 增加每1个斗气点数的BOSS攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    MapleStatEffect 斗气效果 = chra.getStatForBuff(MapleBuffStat.斗气集中);
                    Integer 斗气状态 = chra.getBuffedValue(MapleBuffStat.斗气集中);
                    if (斗气效果 != null && 斗气状态 != null) {
                        percent_boss_damage_rate += eff.getW() * (斗气状态 - 1);
                    }
                }
                bx = SkillFactory.getSkill(英雄.进阶终极攻击_命中); //1120046 - 进阶终极攻击-命中 - 增加进阶终极攻击提高的命中率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_acc += bx.getEffect(bof).getArRate();
                }
                bx = SkillFactory.getSkill(英雄.进阶终极攻击_额外伤害); //1120047 - 进阶终极攻击-额外伤害 - 增加进阶终极攻击提高的物理攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(英雄.终极打击_强化); //1120049 - 终极打击-强化 - 增加终极打击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(英雄.终极打击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(英雄.终极打击_爆击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(英雄.终极打击_额外目标); //1120050 - 终极打击-额外目标 - 增加终极打击攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(英雄.终极打击, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(英雄.终极打击_爆击, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(英雄.终极打击_额外攻击); //1120051 - 终极打击-额外攻击 - 增加终极打击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(英雄.终极打击, bx.getEffect(bof).getAttackCount());
                    addAttackCount(英雄.终极打击_爆击, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 120:
            case 121:
            case 122: {
                bx = SkillFactory.getSkill(圣骑士.物理训练); //1200009 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(圣骑士.盾防精通); //1210001 - 盾防精通 - 佩戴盾牌时增加额外的物理防御力和魔法防御力。佩戴盾牌时有一定概率阻挡敌人的攻击。
                bof = chra.getTotalSkillLevel(bx);
                Item shield = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10); //必须装备盾牌
                if (bof > 0 && bx != null && shield != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(圣骑士.阿基里斯); //1220005 - 阿基里斯 - 永久性地强化身体，减少从敌人那里受到的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    reduceDamageRate += bx.getEffect(bof).getT();
                }
                bx = SkillFactory.getSkill(圣骑士.守护之神); //1220006 - 守护之神 - 强化自己的所有属性抗性和状态异常抗性。此外，#c装备盾牌时#，有一定几率抵挡住敌人的攻击。抵挡住近身攻击时，使攻击者在一定时间内昏迷
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    ASR += bx.getEffect(bof).getASRRate();
                }
                bx = SkillFactory.getSkill(圣骑士.万佛归一破); //1220010 - 万佛归一破 - 增加属性攻击的伤害和最大目标数量，增加攻击次数，提高聚气状态下的武器熟练度。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addAttackCount(圣骑士.火焰冲击, eff.getAttackCount());
                    addAttackCount(圣骑士.寒冰冲击, eff.getAttackCount());
                    addAttackCount(圣骑士.雷鸣冲击, eff.getAttackCount());
                    addAttackCount(圣骑士.神圣冲击, eff.getAttackCount());
                    addAttackCount(圣骑士.连环环破, eff.getAttackCount());

                    addTargetPlus(圣骑士.火焰冲击, eff.getTargetPlus());
                    addTargetPlus(圣骑士.寒冰冲击, eff.getTargetPlus());
                    addTargetPlus(圣骑士.雷鸣冲击, eff.getTargetPlus());
                    addTargetPlus(圣骑士.神圣冲击, eff.getTargetPlus());
                    addTargetPlus(圣骑士.连环环破, eff.getTargetPlus());
                    Integer buff = chra.getBuffedValue(MapleBuffStat.元素冲击);
                    if (buff != null) {
                        watk += buff / 5 * 12;
                        percent_damage += buff;
                    }
                }
                /*
                 * 圣骑士超级技能
                 */
                bx = SkillFactory.getSkill(圣骑士.压制术_坚持); //1220043 - 压制术-坚持 - 增加压制术的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(圣骑士.压制术, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(圣骑士.压制术_额外机会); //1220044 - 压制术-额外机会 - 增加压制术的成功率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addSkillProp(圣骑士.压制术, bx.getEffect(bof).getProp());
                }
                bx = SkillFactory.getSkill(圣骑士.连环环破_强化); //1220046 - 连环环破-强化 - 增加连环环破的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(圣骑士.连环环破, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(圣骑士.连环环破_额外攻击); //1220048 - 连环环破-额外攻击 - 增加连环环破的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(圣骑士.连环环破, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(圣骑士.圣域_强化); //1220049 - 圣域-强化 - 增加圣域的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(圣骑士.圣域, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(圣骑士.圣域_额外攻击); //1220050 - 圣域-额外攻击 - 增加圣域的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(圣骑士.圣域, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(圣骑士.圣域_缩短冷却时间); //1220051 - 圣域-缩短冷却时间 - 减少圣域的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(圣骑士.圣域, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 130:
            case 131:
            case 132: {
                bx = SkillFactory.getSkill(黑骑士.物理训练); //1300009 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(黑骑士.抵抗力); //1310010 - 抵抗力 - 强化自己对异常状态的抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗至尊); //1310009 - 黑暗至尊 - 增加暴击率、暴击最小伤害，有一定几率将伤害的一部分转换成体力。但是，#c不能超过最大HP的一半。#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCritical();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                    hpRecoverProp += eff.getProp();
                    hpRecover_Percent += eff.getX();
                }
                bx = SkillFactory.getSkill(黑骑士.龙之献祭); //1321015 - 龙之献祭 - 吸收灵魂助力可以恢复体力，在一定时间内可以获得防御无视效果和BOSS攻击时伤害增加的效果，神枪降临技能不会冷却。另外会增加#c永久防御无视效果#。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                /*
                 * 黑骑士超级技能
                 */
                bx = SkillFactory.getSkill(黑骑士.神圣之火_坚持); //1320043 - 神圣之火-坚持 - 增加神圣之火的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(黑骑士.神圣之火, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗力量_强化); //1320046 - 黑暗力量-强化 - 增加黑暗力量提高的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getDamage();
                    percent_boss_damage_rate += eff.getDamage();
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗力量_爆击伤害); //1320047 - 黑暗力量-爆击伤害 - 增加黑暗力量提高的最小爆击伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_min_percent += eff.getCriticalMin(); //爆击最小伤害
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗力量_爆击率); //1320048 - 黑暗力量-爆击率 - 增加黑暗力量提高的爆击率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCritical(); //爆击概率
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗穿刺_强化); //1320049 - 黑暗穿刺-强化 - 增加黑暗穿刺的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(黑骑士.黑暗穿刺, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗穿刺_无视防御); //1320050 - 黑暗穿刺-无视防御 - 增加黑暗穿刺无视怪物防御力的数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(黑骑士.黑暗穿刺, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(黑骑士.黑暗穿刺_额外攻击); //1320051 - 黑暗穿刺-额外攻击 - 增加黑暗穿刺的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(黑骑士.黑暗穿刺, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 210:
            case 211:
            case 212: {
                bx = SkillFactory.getSkill(火毒.智慧激发); //2100007 - 智慧激发 - 通过精神修养，永久性增加智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(火毒.极限魔力); //2110000 - 极限魔力（火，毒） - 增加自己的所有持续伤害技能的持续时间，攻击受到持续伤害或处于昏迷、冻结、暗黑、麻痹状态的敌人，可以增加伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    dotTime += eff.getX();
                    dot += eff.getZ();
                }
                bx = SkillFactory.getSkill(火毒.魔力激化); //2110001 - 魔力激化 - 消耗更多的MP，提高所有攻击魔法的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getCostMpRate();
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(火毒.迷雾爆发); //迷雾爆发 - 可以永久性地增加致命毒雾的持续伤害，使用技能时，设置在周围的致命毒雾爆炸，给敌人造成致命伤害。对象所中的持续伤害效果越多，造成的伤害越大。无法引爆其他人设置的毒雾。根据持续伤害效果的数量，伤害的增加幅度不超过5次。\n需要技能：#c致命毒雾20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(火毒.致命毒雾, eff.getX()); //致命毒雾 - 一定时间内在自己周围生成致命的毒雾，使所有敌人中毒。
                }
                bx = SkillFactory.getSkill(火毒.魔力精通); //2120012 - 魔力精通 - 永久性增加魔力，增加对自己使用的所有增益的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getBuffTimeRate();
                }
                bx = SkillFactory.getSkill(火毒.神秘瞄准术); //2120010 - 神秘瞄准术 - 攻击时可以无视怪物的部分防御力，持续攻击时提升所有攻击的伤害。有一定概率发动伤害提升效果，最多可以累积5次。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getX() * eff.getY();
                    percent_boss_damage_rate += eff.getX() * eff.getY();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                /*
                 * 火毒超级技能
                 */
                bx = SkillFactory.getSkill(火毒.美杜莎之眼_强化); //2120046 - 美杜莎之眼-强化 - 增加美杜莎之眼的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(火毒.美杜莎之眼, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(火毒.美杜莎之眼_额外攻击); //2120048 - 美杜莎之眼-额外攻击  - 增加美杜莎之眼的攻击次数
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(火毒.美杜莎之眼, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(火毒.迷雾爆发_额外攻击); //2120049 - 迷雾爆发-额外攻击 - 增加迷雾爆发的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(火毒.迷雾爆发, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(火毒.迷雾爆发_无视防御); //2120050 - 迷雾爆发-无视防御 - 提高迷雾爆发的无视怪物防御力效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(火毒.迷雾爆发, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(火毒.迷雾爆发_缩短冷却时间); //2120051 - 迷雾爆发-缩短冷却时间 - 减少迷雾爆发的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(火毒.迷雾爆发, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 220:
            case 221:
            case 222: {
                bx = SkillFactory.getSkill(冰雷.智慧激发); //2200007 - 智慧激发 - 通过精神修养，永久性增加智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(冰雷.极限魔力); //2210000 - 极限魔力（冰，雷） - HP较低的怪物有一定概率一击必杀，攻击受到持续伤害或处于昏迷、冻结、暗黑、麻痹状态的敌人，可以增加伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dot += bx.getEffect(bof).getZ();
                }
                bx = SkillFactory.getSkill(冰雷.魔力激化); //2210001 - 魔力激化 - 消耗更多的MP，提高所有攻击魔法的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getCostMpRate();
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(冰雷.魔力精通); //2220013 - 魔力精通 - 永久性增加魔力，增加对自己使用的所有增益的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getBuffTimeRate();
                }
                bx = SkillFactory.getSkill(冰雷.神秘瞄准术); //2220010 - 神秘瞄准术 - 攻击时可以无视怪物的部分防御力，持续攻击时提升所有攻击的伤害。有一定概率发动伤害提升效果，最多可以累积5次。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getX() * eff.getY();
                    percent_boss_damage_rate += eff.getX() * eff.getY();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                /*
                 * 冰雷超级技能
                 */
                bx = SkillFactory.getSkill(冰雷.快速移动精通_强化); //2220043 - 快速移动精通-强化 - 增加快速移动精通的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(冰雷.快速移动精通, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(冰雷.快速移动精通_额外目标); //2220044 - 快速移动精通-额外目标 - 增加快速移动精通攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(冰雷.快速移动精通, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(冰雷.链环闪电_强化); //2220046 - 链环闪电-强化 - 增加链环闪电的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(冰雷.链环闪电, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(冰雷.链环闪电_额外目标); //2220047 - 链环闪电-额外目标 - 增加链环闪电攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(冰雷.链环闪电, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(冰雷.链环闪电_额外攻击); //2220048 - 链环闪电-额外攻击 - 增加链环闪电的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(冰雷.链环闪电, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(冰雷.冰河锁链_额外目标); //2220049 - 冰河锁链-额外目标 - 增加冰河锁链攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(冰雷.冰河锁链, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(冰雷.冰河锁链_额外攻击); //2220050 - 冰河锁链-额外攻击 - 增加冰河锁链的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(冰雷.冰河锁链, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(冰雷.冰河锁链_缩短冷却时间); //2220051 - 冰河锁链-缩短冷却时间 - 减少冰河锁链的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(冰雷.冰河锁链, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 230:
            case 231:
            case 232: {
                bx = SkillFactory.getSkill(主教.智慧激发); //2300007 - 智慧激发 - 通过精神修养，永久性增加智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(主教.魔力精通); //2320012 - 魔力精通 - 永久性增加魔力，增加对自己使用的所有增益的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    BuffUP_Skill += eff.getBuffTimeRate();
                }
                bx = SkillFactory.getSkill(主教.神秘瞄准术); //2320011 - 神秘瞄准术 - 攻击时可以无视怪物的部分防御力，持续攻击时提升所有攻击的伤害。有一定概率发动伤害提升效果，最多可以累积5次。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getX() * eff.getY();
                    percent_boss_damage_rate += eff.getX() * eff.getY();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                /*
                 * 主教超级技能
                 */
                bx = SkillFactory.getSkill(主教.神圣魔法盾_坚持); //2320044 - 神圣魔法盾-坚持 - 增加神圣魔法盾的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(主教.神圣魔法盾, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(主教.神圣魔法盾_缩短冷却时间); //2320045 - 神圣魔法盾-缩短冷却时间 - 减少神圣魔法盾的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(主教.神圣魔法盾, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 310:
            case 311:
            case 312: {
                bx = SkillFactory.getSkill(神射手.物理训练); //3100006 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(神射手.精神集中); //3110012 - 精神集中 - 持续攻击时，集中力逐渐提高，异常状态抗性持续增加。拥有相应增益的情况下抵抗异常状态后，应用冷却时间。此外，永久增加异常状态抗性及所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(神射手.射术精修); //3110014 - 射术精修 - 攻击时有一定概率在一定程度上无视怪物的防御力，永久性地增加命中率及总伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    percent_acc += eff.getArRate(); //命中概率
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(神射手.火凤凰); //3111005 - 火凤凰 - 召唤带有火属性的凤凰。凤凰最多同时攻击4个敌人，有一定概率造成昏迷。另外，永久增加物理/魔法防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate(); //增加物防
                    percent_mdef += eff.getMDEFRate(); //增加魔防
                }
                bx = SkillFactory.getSkill(神射手.寒冰爪钩); //3111010 - 寒冰爪钩 - 给范围之内最远处的敌人发射钩子造成伤害，并移动到敌人的背后。移动的时候不会碰到其他怪物，而且还会增加一定比率的体力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(神射手.闪避); //3110007 - 闪避 - 有一定概率可以回避敌人的攻击，回避成功后1秒内的攻击必定是爆击。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(神射手.进阶终极攻击); //3120008 - 进阶终极攻击 - 永久性地增加攻击力和命中率，大幅提高终极武器的发动概率和伤害。\n需要技能：#c终极弓20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    addDamageIncrease(神射手.终极弓, eff.getDamage());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(神射手.火眼晶晶_坚持); //3120043 - 火眼晶晶-坚持 - 火眼晶晶持续时间增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(神射手.火眼晶晶, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(神射手.火眼晶晶_无视防御); //3120044 - 火眼晶晶-无视防御 - 火眼晶晶效果上增加防御无视效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {

                }
                bx = SkillFactory.getSkill(神射手.火眼晶晶_神圣暴击); //3120045 - 火眼晶晶-神圣暴击 - 火眼晶晶效果中增加暴击率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {

                }
                bx = SkillFactory.getSkill(神射手.骤雨箭矢_强化); //3120046 - 骤雨箭矢-强化 - 增加骤雨箭矢的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(神射手.骤雨箭矢, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(神射手.骤雨箭矢_额外目标); //3120047 - 骤雨箭矢-额外目标 - 使用骤雨箭矢攻击的怪物数量增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(神射手.骤雨箭矢, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(神射手.骤雨箭矢_额外攻击); //3120048 - 骤雨箭矢-额外攻击 - 增加骤雨箭矢的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(神射手.骤雨箭矢, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(神射手.暴风箭雨_强化); //3120049 - 暴风箭雨-强化 - 增加暴风箭雨的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(神射手.暴风箭雨, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(神射手.暴风箭雨_BOSS杀手); //3120050 - 暴风箭雨-BOSS杀手 - 增加暴风箭雨对BOSS的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBossDamageRate(神射手.暴风箭雨, bx.getEffect(bof).getBossDamage());
                }
                bx = SkillFactory.getSkill(神射手.暴风箭雨_灵魂攻击); //3120051 - 暴风箭雨-灵魂攻击 - 暴风箭雨的伤害变为原来的#x%，但攻击次数增多。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(神射手.暴风箭雨, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 320:
            case 321:
            case 322: {
                bx = SkillFactory.getSkill(箭神.物理训练); //3200006 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(箭神.闪避); //3210007 - 闪避 - 有一定概率可以回避敌人的攻击，回避成功后1秒内的攻击必定是爆击。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(箭神.射术精修); //3210015 - 射术精修 - 攻击时有一定概率在一定程度上无视怪物的防御力，永久性地增加命中率及总伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    percent_acc += eff.getArRate(); //命中概率
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(箭神.冰凤凰); //3211005 - 冰凤凰 - 在一定时间内召唤带有冰属性的冰凤凰。冰凤凰最多同时攻击4个敌人。另外，会永久增加物理/魔法防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate(); //增加物防
                    percent_mdef += eff.getMDEFRate(); //增加魔防
                }
                bx = SkillFactory.getSkill(箭神.寒冰爪钩); //3211010 - 寒冰爪钩 - 给范围之内最远处的敌人发射钩子造成伤害，并移动到敌人的背后。移动的时候不会碰到其他怪物，而且还会增加一定比率的体力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(箭神.治愈长杖); //3211011 - 治愈长杖 - 在冒险中，吃下所准备的药草后，立即摆脱异常状态。此外，所有属性抗性及异常状态抵抗永久增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(箭神.火眼晶晶_坚持); //3220043 - 火眼晶晶-坚持 - 火眼晶晶持续时间增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(箭神.火眼晶晶, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(箭神.火眼晶晶_无视防御); //3220044 - 火眼晶晶-无视防御 - 火眼晶晶效果上增加防御无视效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {

                }
                bx = SkillFactory.getSkill(箭神.火眼晶晶_神圣暴击); //3220045 - 火眼晶晶-神圣暴击 - 火眼晶晶效果中增加暴击率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {

                }
                bx = SkillFactory.getSkill(箭神.穿透箭_强化); //3220046 - 穿透箭-强化 - 增加穿透箭的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(箭神.穿透箭_四转, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(箭神.穿透箭_额外目标); //3220047 - 穿透箭-额外目标 - 增加穿透箭攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(箭神.穿透箭_四转, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(箭神.穿透箭_额外攻击); //3220048 - 穿透箭-额外攻击 - 增加穿透箭的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(箭神.穿透箭_四转, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(箭神.一击要害箭_强化); //3220049 - 一击要害箭-强化 - 增加一击要害箭的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(箭神.一击要害箭, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(箭神.一击要害箭_最大值提高); //3220050 - 一击要害箭-最大值提高 - 增加一击要害箭的伤害最大值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {

                }
                bx = SkillFactory.getSkill(箭神.一击要害箭_缩短冷却时间); //3220051 - 一击要害箭-缩短冷却时间 - 减少一击要害箭的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(箭神.一击要害箭, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 410:
            case 411:
            case 412: {
                bx = SkillFactory.getSkill(隐士.物理训练); //4100007 - 物理训练 - 通过锻炼身体，永久提高运气和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(隐士.永恒黑暗); //4110008 - 永恒黑暗 - 和黑暗融为一体，永久增加最大HP、增加状态异常抗性、增加所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(隐士.娴熟飞镖术); //4110012 - 娴熟飞镖术 - 提高双飞斩、爆裂飞镖、风之护符、三连环光击破的#c总伤害#。\n此外，攻击时有一定几率不消耗飞镖，使当前持有的#c飞镖增加1个#。\n(但飞镖数量无法超出最大个数)\n此外娴熟飞镖术效果发动时，#c下一次攻击百分之百造成爆击#。(在暗器伤人状态下也可以造成爆击，但标枪数量不增加。)
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage += eff.getPercentDamageRate();
                }
                bx = SkillFactory.getSkill(隐士.药品吸收); //4110014 - 药品吸收 - 提高药水等恢复道具的效果。但对超级药水之类按百分比恢复的道具无效。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    RecoveryUP += eff.getX() - 100;
                    BuffUP += eff.getY() - 100;
                }
                bx = SkillFactory.getSkill(隐士.黑暗祝福); //4121014 - 黑暗祝福 - 获得黑暗的祝福，可以对敌人发动更加致命的攻击。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(隐士.决战之巅_强化); //4120043 - 决战之巅-强化 - 增加决战之巅的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(隐士.决战之巅, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(隐士.决战之巅_额外目标); //4120044 - 决战之巅-额外目标 - 增加攻击决战之巅的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(隐士.决战之巅, bx.getEffect(bof).getTargetPlus());
                }
//                bx = SkillFactory.getSkill(标飞.决战之巅_增强); //4120045 - 决战之巅-增强 - 增加攻击决战之巅怪物的经验值和掉宝率。
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    addAttackCount(标飞.决战之巅, bx.getEffect(bof).getAttackCount());
//                }
                bx = SkillFactory.getSkill(隐士.模糊领域_缩短冷却时间); //4120048 - 模糊领域-缩短冷却时间 - 减少模糊领域的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(隐士.模糊领域, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(隐士.四连镖_强化); //4120049 - 四连镖-强化  - 增加四连射的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(隐士.四连镖, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(隐士.四连镖_BOSS杀手); //4120050 - 四连镖-BOSS杀手  - 增加四连射的BOSS攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
                }
                bx = SkillFactory.getSkill(隐士.四连镖_额外攻击); //4120051 - 四连镖-额外攻击  - 增加四连射的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(隐士.四连镖, bx.getEffect(bof).getBulletCount());
                }
                break;
            }
            case 420:
            case 421:
            case 422: {
                bx = SkillFactory.getSkill(侠盗.物理训练); //4200007 - 物理训练 - 通过锻炼身体，永久提高运气和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(侠盗.永恒黑暗); //4210013 - 永恒黑暗 - 和黑暗融为一体，永久增加最大HP、增加状态异常抗性、增加所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(侠盗.盾防精通); //4200010 - 盾防精通 - 装备盾牌时，物理防御力和魔法防御力增加，回避率增加。
                bof = chra.getTotalSkillLevel(bx);
                Item shield = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10); //必须装备盾牌
                if (bof > 0 && bx != null && shield != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getX();
                    percent_mdef += eff.getX();
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(侠盗.贪婪); //4210012 - 贪婪 - 修炼飞侠的秘籍，增加金币获得量。此外略微提高使用金币的所有技能的效率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    mesoBuff *= (eff.getMesoRate() + 100.0) / 100.0;
                    pickRate += eff.getU();
                    mesoGuard -= eff.getV();
                    mesoGuardMeso -= eff.getW();
                    addDamageIncrease(侠盗.金钱炸弹, eff.getX()); //金钱炸弹 - 使周围掉落的金币爆炸，给敌人造成伤害。但无法引爆其他人有优先权的金币，普通怪物最多可以重叠10个的伤害，BOSS怪物最多可以重叠15个的伤害。
                }
                bx = SkillFactory.getSkill(侠盗.侠盗本能); //4221013 - 侠盗本能 - 通过暗杀，唤醒侠盗的本能，增加攻击力，获得穿透敌人防御的能力。被动效果是攻击敌人时可以积累击杀点数，消耗积累的点数，可以增强攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(侠盗.一出双击); //4221007 - 一出双击 - 使用技能时，快速两次攻击多个敌人，有一定几率使敌人昏迷。此外，强化可以和一出双击相连接的所有3转以下的攻击技能。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(侠盗.回旋斩, eff.getDAMRate());
                    addDamageIncrease(侠盗.神通术, eff.getDAMRate());
                    addDamageIncrease(侠盗.炼狱, eff.getDAMRate());
                    addDamageIncrease(侠盗.刀刃之舞, eff.getDAMRate());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(侠盗.金钱炸弹_强化); //4220043 - 金钱炸弹-强化 - 增加金钱炸弹的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(侠盗.金钱炸弹, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(侠盗.一出双击_强化); //4220046 - 一出双击-强化 - 增加一出双击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(侠盗.一出双击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(侠盗.一出双击_额外目标); //4220047 - 一出双击-额外目标  - 增加一出双击攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(侠盗.一出双击, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(侠盗.一出双击_额外攻击); //4220048 - 一出双击-额外攻击 - 增加一出双击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(侠盗.一出双击, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(侠盗.暗杀_强化); //4220049 - 暗杀-强化 - 增加暗杀的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(侠盗.暗杀, bx.getEffect(bof).getDAMRate());
                }
                break;
            }
            case 431:
            case 432:
            case 433:
            case 434: {
                bx = SkillFactory.getSkill(双刀.物理训练); //4310006 - 物理训练 - 通过锻炼身体，永久提高运气和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(双刀.生命偷取); //4330007 - 生命偷取 - 攻击时有一定概率恢复HP。但一次恢复的HP不能超过最大值的20%。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    hpRecoverProp += eff.getProp();
                    hpRecover_Percent += eff.getX();
                }
                bx = SkillFactory.getSkill(双刀.永恒黑暗); //4330008 - 永恒黑暗 - 和黑暗融为一体，永久增加最大HP、增加状态异常抗性、增加所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                }
                bx = SkillFactory.getSkill(双刀.影子闪避); //4330009 - 影子闪避 - 有一定概率可以回避敌人的攻击，成功回避后的1秒之内提升攻击力，并且接下来的攻击一定会以爆击形式击中。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(双刀.傀儡召唤); //4341006 - 傀儡召唤 - 永久增加暗影双刀的防御力和回避概率，并且使用技能时，将召唤为镜像分身的分身分离出来，做成土堆。土堆可以吸引敌人的攻击，并吸收部分伤害，以保护自己不受敌人伤害。只能在镜像分身技能有效期间内使用。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWDEFRate();
                    percent_mdef += eff.getMDEFRate();
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(双刀.终极斩); //4341002 - 终极斩 - 消耗大量HP，给多个敌人加以非常强力的攻击。有较低概率一击必杀，在一定时间内，部分攻击技能的伤害增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(双刀.流云斩, eff.getDAMRate());
                    addDamageIncrease(双刀.双刀风暴, eff.getDAMRate());
                    addDamageIncrease(双刀.龙卷风, eff.getDAMRate());
                    addDamageIncrease(双刀.血雨腥风, eff.getDAMRate());
                    addDamageIncrease(双刀.悬浮地刺, eff.getDAMRate());
                    addDamageIncrease(双刀.暗影飞跃斩, eff.getDAMRate());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(双刀.血雨腥风_强化); //4340043 - 血雨腥风-强化 - 增加血雨腥风的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双刀.血雨腥风, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双刀.血雨腥风_额外目标); //4340044 - 血雨腥风-额外目标 - 增加血雨腥风攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(双刀.血雨腥风, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(双刀.血雨腥风_额外攻击); //4340045 - 血雨腥风-额外攻击 - 增加血雨腥风的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(双刀.血雨腥风, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(双刀.幽灵一击_强化); //4340046 - 幽灵一击-强化 - 增加幽灵一击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双刀.幽灵一击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双刀.幽灵一击_无视防御); //4340047 - 幽灵一击-无视防御 - 增加幽灵一击的无视怪物防御力数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(双刀.幽灵一击, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(双刀.幽灵一击_额外攻击); //4340048 - 幽灵一击-额外攻击 - 增加幽灵一击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(双刀.幽灵一击, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(双刀.暴怒刀阵_强化); //4340055 - 暴怒刀阵 - 强化 - 增加暴怒刀阵的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双刀.暴怒刀阵, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双刀.暴怒刀阵_无视防御); //4340056 - 暴怒刀阵 - 无视防御 - 增加暴怒刀阵的防御力无视效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(双刀.暴怒刀阵, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(双刀.暴怒刀阵_额外目标); //4340057 - 暴怒刀阵 -额外目标 - 增加利用暴怒刀阵攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(双刀.暴怒刀阵, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 510:
            case 511:
            case 512: {
                bx = SkillFactory.getSkill(冲锋队长.HP增加); //5100009 - HP增加 - 通过锻炼身体永久性地增加最大HP。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(冲锋队长.物理训练); //5100010 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX(); //提高力量
                    localdex += eff.getDexX(); //提高敏捷
                }
                bx = SkillFactory.getSkill(冲锋队长.蛇拳); //5121015 - 蛇拳 - 在一定时间内召唤出遗忘的毒蛇之魂，增加攻击力。被动效果是提高状态异常和所有属性抗性，提高拳甲熟练度。\n需要技能：#c精准拳20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate(); //提高状态异常
                }
                bx = SkillFactory.getSkill(冲锋队长.重装碾压); //5120014 - 重装碾压 - 攻击时有一定概率100%无视敌人的防御力。对BOSS怪同样有效。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getX();
                }
                double energyrate = chra.getBuffedValue(MapleBuffStat.能量获得) != null && chra.getSpecialStat().isEnergyFull() ? 1.0 : 0.5;
                if (chra.getTotalSkillLevel(冲锋队长.终极冲击) > 0) {
                    bx = SkillFactory.getSkill(冲锋队长.终极冲击);
                    bof = chra.getTotalSkillLevel(bx);
                    eff = bx.getEffect(bof);
                    watk += (eff.getWatk() * energyrate);
                    wdef += (eff.getEnhancedWdef() * energyrate);
                    mdef += (eff.getEnhancedMdef() * energyrate);
                    speed += (eff.getSpeed() * energyrate);
                    accuracy += (eff.getAcc() * energyrate);
                } else if (chra.getTotalSkillLevel(冲锋队长.超级冲击) > 0) {
                    bx = SkillFactory.getSkill(冲锋队长.超级冲击);
                    bof = chra.getTotalSkillLevel(bx);
                    eff = bx.getEffect(bof);
                    watk += (eff.getWatk() * energyrate);
                    wdef += (eff.getEnhancedWdef() * energyrate);
                    mdef += (eff.getEnhancedMdef() * energyrate);
                    speed += (eff.getSpeed() * energyrate);
                    accuracy += (eff.getAcc() * energyrate);
                } else if (chra.getTotalSkillLevel(冲锋队长.能量获得) > 0) {
                    bx = SkillFactory.getSkill(冲锋队长.能量获得);
                    bof = chra.getTotalSkillLevel(bx);
                    eff = bx.getEffect(bof);
                    wdef += (eff.getEnhancedWdef() * energyrate);
                    mdef += (eff.getEnhancedMdef() * energyrate);
                    speed += (eff.getSpeed() * energyrate);
                    accuracy += (eff.getAcc() * energyrate);
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(冲锋队长.激怒拳_强化); //5120046 - 激怒拳-强化 - 增加激怒拳的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(冲锋队长.激怒拳, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(冲锋队长.暴怒拳, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(冲锋队长.激怒拳_BOSS杀手); //5120047 - 激怒拳-BOSS杀手 - 增加激怒拳的BOSS攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBossDamageRate(冲锋队长.激怒拳, bx.getEffect(bof).getBossDamage());
                    addBossDamageRate(冲锋队长.暴怒拳, bx.getEffect(bof).getBossDamage());
                }
                bx = SkillFactory.getSkill(冲锋队长.激怒拳_额外攻击); //5120048 - 激怒拳-额外攻击 - 增加激怒拳的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(冲锋队长.激怒拳, bx.getEffect(bof).getAttackCount());
                    addAttackCount(冲锋队长.暴怒拳, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(冲锋队长.能量爆炸_强化); //5120049 - 能量爆炸-强化 - 增加能量爆炸的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(冲锋队长.能量爆炸, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(冲锋队长.双重爆炸, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(冲锋队长.能量爆炸_额外目标); //5120050 - 能量爆炸-额外目标 - 增加能量爆炸攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(冲锋队长.能量爆炸, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(冲锋队长.双重爆炸, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(冲锋队长.能量爆炸_额外攻击); //5120051 - 能量爆炸-额外攻击 - 增加能量爆炸的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(冲锋队长.能量爆炸, bx.getEffect(bof).getAttackCount());
                    addAttackCount(冲锋队长.双重爆炸, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 520:
            case 521:
            case 522: {
                bx = SkillFactory.getSkill(船长.物理训练); //5200009 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX(); //提高力量
                    localdex += eff.getDexX(); //提高敏捷
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(船长.金属风暴_强化); //5220049 - 金属风暴-强化 - 增加金属风暴的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(船长.金属风暴, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(船长.金属风暴_BOSS杀手); //5220051 - 金属风暴-BOSS杀手 - 增加金属风暴对BOSS的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
                }
                break;
            }
            case 501:
            case 530:
            case 531:
            case 532: {
                bx = SkillFactory.getSkill(神炮王.升级火炮); //升级火炮 - 对大炮进行改良，永久性地提高攻击力和防御力
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(神炮王.海盗训练); //海盗训练 - 通过海盗的秘密修炼，提高力量和敏捷
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(神炮王.猴子超级炸弹); //猴子超级炸弹
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(神炮王.猴子炸药桶, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(神炮王.生命强化); //生命强化 - 永久性地强化体力、防御力、增加状态异常抗性
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP(); //以前是这个 好像有点问题加不满血getHpR()
                    ASR += eff.getASRRate();
                    percent_wdef += eff.getWDEFRate();
                }
                bx = SkillFactory.getSkill(神炮王.火炮强化); //火炮强化 - 永久性地强化大炮，提高攻击力和攻击速度
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(神炮王.极限燃烧弹); //极限燃烧弹 - 将大炮的性能提高到极限，永久性地增加伤害。此外，攻击时有一定概率在一定程度上无视怪物的防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getDAMRate();
                    percent_boss_damage_rate += eff.getDAMRate();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(神炮王.双胞胎猴子支援_增加); //5320043 - 双胞胎猴子支援-增加 - 降低双胞胎猴子支援的伤害，增加攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(神炮王.双胞胎猴子支援, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(神炮王.双胞胎猴子支援_坚持); //5320044 - 双胞胎猴子支援-坚持 - 增加双胞胎猴子支援的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(神炮王.双胞胎猴子支援, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(神炮王.加农火箭炮_强化); //5320046 - 加农火箭炮-强化 - 增加加农火箭炮的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(神炮王.加农火箭炮, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(神炮王.加农火箭炮_额外目标); //5320047 - 加农火箭炮-额外目标 - 增加加农火箭炮攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(神炮王.加农火箭炮, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(神炮王.加农火箭炮_额外攻击); //5320048 - 加农火箭炮-额外攻击 - 增加加农火箭炮的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(神炮王.加农火箭炮, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(神炮王.集中炮击_强化); //5320049 - 集中炮击-强化 - 增加集中炮击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(神炮王.集中炮击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(神炮王.集中炮击_额外攻击); //5320051 - 集中炮击-额外攻击 - 增加集中炮击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(神炮王.集中炮击, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 508:
            case 570:
            case 571:
            case 572: {
                bx = SkillFactory.getSkill(龙的传人.侠客之道); //5080022 - 侠客之道 - 熟练侠客的一切基本技巧.提升命中值,回避值,射程,移动速度,最高移动速度上限.\r\n
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    defRange += eff.getRange();
                    accuracy += eff.getAcc();
                    speed += eff.getPassiveSpeed();
                    speedMax += eff.getSpeedMax();
                }
                bx = SkillFactory.getSkill(龙的传人.侠客秘诀); //5700011 - 侠客秘诀 - 透过特殊锻炼方式永久提升力量,敏捷与体力.另外,有几率发动格挡效果.\r\n
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    percent_hp += eff.getPercentHP(); //最大Hp增加
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                }
                bx = SkillFactory.getSkill(龙的传人.宏武典籍); //5710022 - 宏武典籍 - 强化使用短枪与指节的技巧,大幅提升短枪与指节的爆击几率和物理攻击力.
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage_rate += eff.getDAMRate();
                    percent_boss_damage_rate += eff.getDAMRate();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                bx = SkillFactory.getSkill(龙的传人.金刚不坏); //5720061 - 金刚不坏 - 爆发所有真气强化身体,永久提升和防御有关的所有能力.大幅强化耐力并使攻击力剧增.
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP(); //最大Hp增加
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(龙的传人.龙魂流星拳_增加目标); //5720044 - 龙魂流星拳-增加目标 - 增加龙魂流星拳攻击怪物的数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(龙的传人.龙魂流星拳, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(龙的传人.龙魂流星拳_次数强化); //5720045 - 龙魂流星拳-次数强化 - 增加龙魂流星拳的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(龙的传人.龙魂流星拳, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(龙的传人.真气爆炸_次数强化); //5720048 - 真气爆炸-次数强化 - 增加真气爆炸的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(龙的传人.真气爆炸, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 1100:
            case 1110:
            case 1111:
            case 1112: {
                /*
                 * 新的技能
                 */
                bx = SkillFactory.getSkill(魂骑士.灵魂鸣响);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_wdef += eff.getWdefX();
                    percent_mdef += eff.getMdefX();
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                    speedMax += eff.getSpeedMax();
                }
                bx = SkillFactory.getSkill(魂骑士.元素_灵魂); //11001022 - 元素：灵魂 - 召唤出灵魂元素，获得其的力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(魂骑士.人神一体); //11100026 - 人神一体 - 同时修炼身心，打好坚实的基本功。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(魂骑士.钢铁之轮); //11110025 - 钢铁之轮 - 凭借超越常人的坚强意志，摆脱逆境。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(魂骑士.心灵呐喊); //11110026 - 心灵呐喊 - 集中精神，给全身注入活力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    localstr += eff.getStrX();
                }
                bx = SkillFactory.getSkill(魂骑士.幻千之刃); //11120008 - 幻千之刃 - 以幻千之刃的攻击削弱敌人的防御。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                /*
                 * 超级技能处理
                 */
                bx = SkillFactory.getSkill(魂骑士.斩刺_强化); //11120046 - 斩刺-强化 - 提升新月斩和烈日之刺的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(魂骑士.新月斩, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(魂骑士.烈日之刺, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(魂骑士.斩刺_额外目标); //11120047 - 斩刺-额外目标 - 增加使用新月斩和烈日之刺攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(魂骑士.新月斩, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(魂骑士.烈日之刺, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(魂骑士.斩刺_额外攻击);  //11120048 - 斩刺-额外攻击 - 增加新月斩和烈日之刺的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(魂骑士.新月斩, bx.getEffect(bof).getAttackCount());
                    addAttackCount(魂骑士.烈日之刺, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(魂骑士.极速之舞_强化); //11120049 - 极速之舞-强化 - 增加月光之舞和极速霞光的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(魂骑士.月光之舞, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(魂骑士.月光之舞_空中, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(魂骑士.极速霞光, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(魂骑士.极速霞光_空中, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(魂骑士.极速之舞_无视防御); //11120050 - 极速之舞-无视防御 - 增加月光之舞和极速霞光的无视怪物防御数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(魂骑士.月光之舞, bx.getEffect(bof).getIgnoreMob());
                    addIgnoreMobpdpRate(魂骑士.月光之舞_空中, bx.getEffect(bof).getIgnoreMob());
                    addIgnoreMobpdpRate(魂骑士.极速霞光, bx.getEffect(bof).getIgnoreMob());
                    addIgnoreMobpdpRate(魂骑士.极速霞光_空中, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(魂骑士.极速之舞_BOSS杀手); //11120051 - 极速之舞-BOSS杀手 - 增加月光之舞和极速霞光的BOSS攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
                }
                break;
            }
            case 1200:
            case 1210:
            case 1211:
            case 1212: {
                bx = SkillFactory.getSkill(炎术士.MP增加);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                bx = SkillFactory.getSkill(炎术士.魔法爆击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    TER += bx.getEffect(bof).getX();
                }
                bx = SkillFactory.getSkill(炎术士.卓越才能); //12000025 - 卓越才能 - 利用受到祝福的才能增加最大魔力，等级越高，魔力增加量越高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                    addmaxmp += eff.getLevelToMaxMp() * chra.getLevel(); //等级乘以倍率
                }
                bx = SkillFactory.getSkill(炎术士.弱点分析); //12110026 - 弱点分析 - 通过敏锐的分析看透敌人的弱点，提高咒语造成爆击的概率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCritical(); //爆击概率
                    passive_sharpeye_min_percent += eff.getCriticalMin(); //爆击最小伤害
                }
                bx = SkillFactory.getSkill(炎术士.顿悟); //12110027 - 顿悟 - 咒语达到更高的境界，快速成长。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                /*
                 * 处理超级技能
                 */
                bx = SkillFactory.getSkill(炎术士.轨道烈焰_灵魂攻击);  //12120045 - 轨道烈焰-灵魂攻击 - 轨道烈焰IV的伤害降为280%，但攻击次数增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(炎术士.轨道烈焰_LINK, bx.getEffect(bof).getAttackCount());
                    addAttackCount(炎术士.轨道烈焰II_LINK, bx.getEffect(bof).getAttackCount());
                    addAttackCount(炎术士.轨道烈焰III_LINK, bx.getEffect(bof).getAttackCount());
                    addAttackCount(炎术士.轨道烈焰IV_LINK, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(炎术士.灭绝之焰_额外攻击);  //12120046 - 灭绝之焰-额外攻击 - 增加灭绝之焰的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(炎术士.灭绝之焰_LINK, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(炎术士.灭绝之焰_额外目标);  //12120048 - 灭绝之焰-额外目标 - 增加灭绝之焰攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(炎术士.灭绝之焰_LINK, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 1300:
            case 1310:
            case 1311:
            case 1312: {
                /*
                 * 新的技能模版
                 */
                bx = SkillFactory.getSkill(风灵使者.风影漫步); //13001021 - 风影漫步 - 借助风之力量，快速向前方突进。突进时无视怪物的攻击。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(风灵使者.风之私语); //13000023 - 风之私语 - 听取风之私语，习得古代知识，拥有轻捷的身手。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    accuracy += eff.getAccX();
                    defRange += eff.getRange();
                }
                bx = SkillFactory.getSkill(风灵使者.物理训练); //13100026 - 物理训练 - 通过锻炼身体，永久提升力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(风灵使者.轻如鸿毛); //13110025 - 轻如鸿毛 - 身体变得像羽毛一样轻盈，可以灵巧地避开敌人的攻击。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(风灵使者.重振精神); //13110026 - 重振精神 - 借助风之力量，摆脱无法回避的情况，获得反击的机会。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWdefX();
                    percent_mdef += eff.getMdefX();
                    dodgeChance += eff.getER();
                }
                /*
                 * 超级技能处理
                 */
                bx = SkillFactory.getSkill(风灵使者.狂风肆虐_强化); //13120043 - 狂风肆虐-强化 - 增加狂风肆虐的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(风灵使者.狂风肆虐Ⅰ, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.狂风肆虐Ⅰ_攻击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.狂风肆虐Ⅱ, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.狂风肆虐Ⅱ_攻击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.狂风肆虐Ⅲ, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.狂风肆虐Ⅲ_攻击, bx.getEffect(bof).getDAMRate());
                }
                //已经在触发的时候处理 13120044 - 狂风肆虐-增强 - 提升狂风肆虐的触发概率。
                bx = SkillFactory.getSkill(风灵使者.狂风肆虐_二次机会); //13120045 - 狂风肆虐-二次机会 - 可以使狂风肆虐再次命中同一个对象。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(风灵使者.狂风肆虐Ⅰ, 2);
                    addAttackCount(风灵使者.狂风肆虐Ⅰ_攻击, 2);
                    addAttackCount(风灵使者.狂风肆虐Ⅱ, 2);
                    addAttackCount(风灵使者.狂风肆虐Ⅱ_攻击, 2);
                    addAttackCount(风灵使者.狂风肆虐Ⅲ, 2);
                    addAttackCount(风灵使者.狂风肆虐Ⅲ_攻击, 2);
                }
                bx = SkillFactory.getSkill(风灵使者.旋风箭_强化); //13120046 - 旋风箭-强化 - 增加旋风箭的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(风灵使者.旋风箭, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(风灵使者.旋风箭_溅射, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(风灵使者.旋风箭_额外目标); //13120047 - 旋风箭-额外目标 - 增加旋风箭攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(风灵使者.旋风箭, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(风灵使者.旋风箭_溅射, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(风灵使者.旋风箭_额外攻击);  //13120048 - 旋风箭-额外攻击 - 增加旋风箭的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(风灵使者.旋风箭, bx.getEffect(bof).getAttackCount());
                    addAttackCount(风灵使者.旋风箭_溅射, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(风灵使者.天空之歌_强化); //13120049 - 天空之歌-强化 - 增加天空之歌的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(风灵使者.天空之歌, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(风灵使者.天空之歌_无视防御); //13120050 - 天空之歌-无视防御 - 提升天空之歌的无视怪物防御数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(风灵使者.天空之歌, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(风灵使者.天空之歌_BOSS杀手); //13120051 - 天空之歌-BOSS杀手 - 提升天空之歌的BOSS攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
                }
                break;
            }
            case 1400:
            case 1410:
            case 1411:
            case 1412: {
                bx = SkillFactory.getSkill(夜行者.投掷精通); //14100023 - 投掷精通 - 增加伤害，提高拳套系列武器的熟练度和命中值，增加飞镖的最大持有数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(夜行者.物理训练); //14100025 - 物理训练 - 通过锻炼身体永久性地提高幸运。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(夜行者.永恒黑暗); //永恒黑暗 - 和黑暗融为一体，永久增加最大HP、增加状态异常抗性、增加所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    ASR += eff.getASRRate();
                }
                bx = SkillFactory.getSkill(夜行者.黑暗祝福); //14120006 - 黑暗祝福 - 获得黑暗的祝福，能力强化。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                }
                bx = SkillFactory.getSkill(夜行者.黑暗预兆_缩短冷却时间); //14120046 - 黑暗预兆-缩短冷却时间 - 减少黑暗预兆的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(夜行者.黑暗预兆, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(夜行者.黑暗预兆_额外目标); //14120047 - 黑暗预兆-额外目标 - 增加黑暗预兆攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(夜行者.黑暗预兆, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 1500:
            case 1510:
            case 1511:
            case 1512: {
                /*
                 * 新的技能模版
                 */
                bx = SkillFactory.getSkill(奇袭者.光速); //15000023 - 光速 - 通过操控一股闪电力量，使身体变得更加敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                    raidenCount += eff.getV();
                    raidenPorp += eff.getProp();
                }
                bx = SkillFactory.getSkill(奇袭者.元素_闪电); //15001022 - 元素：闪电 - 召唤出青白色的闪电元素，获得其的力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    raidenCount += eff.getV();
                    raidenPorp += eff.getProp();
                }
                bx = SkillFactory.getSkill(奇袭者.体力锻炼); //15100024 - 体力锻炼 - 通过坚持不懈的修炼，永久提升力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                }
                bx = SkillFactory.getSkill(奇袭者.雷魄); //15100025 - 雷魄 - 更加熟练地使用闪电力量，威力提升一个阶段。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    raidenCount += eff.getV();
                    raidenPorp += eff.getProp();
                }
                bx = SkillFactory.getSkill(奇袭者.雷帝); //15110026 - 雷帝 - 将闪电力量置于自己的支配之下，更积极地操控这种力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    raidenCount += eff.getV();
                    raidenPorp += eff.getProp();
                }
                bx = SkillFactory.getSkill(奇袭者.刺激); //15120007 - 刺激 - 利用电气刺激全身的神经，形成比任何人都更强韧的肉体。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    //toto 回避率增加 x% er
                    percent_hp += eff.getPercentHP();
                    ignore_mob_damage_rate += eff.getIgnoreMobDamR(); //受到伤害减少x%
                }
                bx = SkillFactory.getSkill(奇袭者.雷神); //15120008 - 雷神 - 精通闪电力量，领悟其中蕴含的精髓。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    raidenCount += eff.getV();
                    raidenPorp += eff.getProp();
                    passive_sharpeye_rate += eff.getCritical(); //爆击概率
                    passive_sharpeye_min_percent += eff.getCriticalMin(); //爆击最小伤害
                }
                /*
                 * 超级技能处理
                 */
                bx = SkillFactory.getSkill(奇袭者.疾风_强化); //15120043 - 疾风-强化 - 提升疾风技能的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(奇袭者.疾风, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(奇袭者.台风, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(奇袭者.疾风_额外目标); //15120044 - 疾风-额外目标 - 增加使用疾风技能可攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(奇袭者.疾风, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(奇袭者.台风, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(奇袭者.疾风_额外攻击);  //15120045 - 疾风-额外攻击 - 增加疾风技能的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(奇袭者.疾风, bx.getEffect(bof).getAttackCount());
                    addAttackCount(奇袭者.台风, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(奇袭者.霹雳_强化); //15120046 - 霹雳-强化 - 提升霹雳技能的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(奇袭者.霹雳, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(奇袭者.霹雳_额外目标); //15120047 - 霹雳-额外目标 - 增加使用霹雳技能可攻击的怪物数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(奇袭者.霹雳, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(奇袭者.霹雳_额外攻击);  //15120048 - 霹雳-额外攻击 - 增加霹雳技能的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(奇袭者.霹雳, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(奇袭者.毁灭_强化); //15120049 - 毁灭-强化 - 提升毁灭技能的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(奇袭者.毁灭, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(奇袭者.毁灭_无视防御); //15120050 - 毁灭-无视防御 - 提升毁灭技能的怪物防御率无视数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(奇袭者.毁灭, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(奇袭者.毁灭_BOSS杀手); //15120051 - 毁灭-BOSS杀手 - 提升毁灭技能对BOSS的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_boss_damage_rate += bx.getEffect(bof).getBossDamage();
                }
                break;
            }
            case 2003: {
                bx = SkillFactory.getSkill(幻影.灵敏身手); //20030206 - 灵敏身手 - 幻影拥有卓越的洞察力和使用各种武器的能力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    dodgeChance += eff.getER();
                    chra.getTrait(MapleTraitType.insight).setLevel(20, chra);
                    chra.getTrait(MapleTraitType.craft).setLevel(20, chra);
                }
                break;
            }
            case 2110:
            case 2111:
            case 2112: {
                bx = SkillFactory.getSkill(战神.物理训练); //21100008 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(战神.冰雪矛); //21101006 - 冰雪矛 - 永久性地增加伤害，使用技能时有一定概率为矛附加冰属性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_damage += bx.getEffect(bof).getDAMRate();
                }
                bx = SkillFactory.getSkill(战神.进阶矛连击强化); //21110000 - 进阶矛连击强化 - 状态异常抗性概率、稳如泰山概率、爆击概率、爆击最大伤害提高。此外，斗气点数每累积10点，状态异常抗性概率、稳如泰山概率、爆击概率攻击力额外提高。\n需要技能：#c矛连击强化10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    passive_sharpeye_rate += eff.getCritical(); //爆击概率
                    passive_sharpeye_min_percent += eff.getCriticalMax(); //爆击最大伤害
                    ASR += eff.getASRRate(); //状态异常抗性概率
                }
                bx = SkillFactory.getSkill(战神.分裂攻击); //21110010 - 分裂攻击 - 一定程度上无视怪物的防御力，打猎BOSS怪时，可以造成额外伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob(); //无视防御
                    percent_boss_damage_rate += eff.getBossDamage(); //BOSS伤害
                }
                bx = SkillFactory.getSkill(战神.防守策略); //21120004 - 防守策略 - 学会更高层次的防御技术，永久性地减少从敌人那里受到的伤害，提高体力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(战神.进阶终极攻击); //21120012 - 进阶终极攻击 - 永久性地增加攻击力和命中率，终极矛技能的发动概率和伤害大幅上升。\n需要技能：#c终极矛20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    addDamageIncrease(战神.终极矛, eff.getDamage());
                }
                bx = SkillFactory.getSkill(战神.迅捷移动); //21120011 - 迅捷移动 - 战神突进、终极投掷的伤害进一步提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(战神.战神突进, eff.getDAMRate());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(战神.恐惧风暴_附加目标);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addTargetPlus(战神.加速终端_恐惧风暴, eff.getTargetPlus());
                    addTargetPlus(战神.加速终端_恐惧风暴_2, eff.getTargetPlus());
                    addTargetPlus(战神.加速终端_恐惧风暴_3, eff.getTargetPlus());
                }
                bx = SkillFactory.getSkill(战神.激素狂飙_坚持);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addBuffDuration(战神.激素狂飙, eff.getDuration());
                }
                bx = SkillFactory.getSkill(战神.比昂德_额外攻击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addAttackCount(战神.比昂德, eff.getAttackCount());
                    addAttackCount(战神.比昂德_2击, eff.getAttackCount());
                    addAttackCount(战神.比昂德_3击, eff.getAttackCount());
                }
                break;
            }
            case 2200:
            case 2211:
            case 2214:
            case 2217: {
                bx = SkillFactory.getSkill(龙神.链接魔力); //2000006 - MP增加 - 永久增加最大MP，根据等级MP也会额外的增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                    addmaxmp += eff.getLevelToMaxMp() * chra.getLevel(); //等级乘以倍率
                }

                magic += chra.getTotalSkillLevel(SkillFactory.getSkill(龙神.龙魂)); //龙魂 - 通过和龙交感增加魔力
                bx = SkillFactory.getSkill(龙神.智慧激发); //22120001 - 智慧激发 - 通过精神修养，永久性地提高智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(龙神.魔力激化); //22150000 - 魔力激化 - 消耗更多的MP，提高所有攻击魔法的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    mpconPercent += eff.getX() - 100;
                    percent_damage_rate += eff.getY();
                    percent_boss_damage_rate += eff.getY();
                }
                bx = SkillFactory.getSkill(龙神.龙的愤怒); //22160000 - 龙的愤怒 - 自己的MP维持在一定范围内时，增强龙的集中力，提高魔力。MP超出有效范围时，效果消失。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_damage += bx.getEffect(bof).getDamage();
                }
                bx = SkillFactory.getSkill(龙神.魔法精通); //22170001 - 魔法精通 - 提高魔法熟练度和魔力，提高爆击最小伤害。\n需要技能：#c咒语精通10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    magic += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                /*
                 * 超级技能
                 */
            }
            case 2001:
            case 2300:
            case 2310:
            case 2311:
            case 2312: {
                bx = SkillFactory.getSkill(双弩.精灵的祝福); //20021110 - [种族特性技能]借助古代精灵的祝福，可以回到埃欧雷，永久性地提高经验值获得量
                bof = chra.getSkillLevel(bx);
                if (bof > 0 && bx != null) { //去掉这个
                    //expBuff *= (bx.getEffect(bof).getEXPRate() + 100.0) / 100.0;
                }
                bx = SkillFactory.getSkill(双弩.王者资格); //20020112 - 王者资格 - 精灵的国王一出生就拥有快速的动作和移动速度，以及致命的魅力
                bof = chra.getSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    chra.getTrait(MapleTraitType.charm).setLevel(20, chra);
                }
                bx = SkillFactory.getSkill(双弩.潜力激发); //23000001 - 潜力激发 - 永久性地激活体内潜藏的力量。移动速度和最大移动速度上限增加，有一定概率回避敌人的攻击
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getER();
                }
                bx = SkillFactory.getSkill(双弩.物理训练); //23100008 - 物理训练 - 通过身体锻炼，永久性地提高力量和敏捷
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(双弩.终结箭); //23100004 - 终结箭 - 用箭矢扫射被冲锋拳打到空中的敌人。只能在冲锋拳之后使用。\n需要技能：#c冲锋拳1级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getProp();
                }
                bx = SkillFactory.getSkill(双弩.爆裂飞腿); //23110006 - 爆裂飞腿- 用箭矢扫射被冲锋拳打到空中的敌人。只能在冲锋拳之后使用。需要技能：冲锋拳1级以上
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.冲锋拳, bx.getEffect(bof).getDAMRate()); //冲锋拳 - 快速击退前方的多个敌人，并将其打到空中。攻击打到空中的敌人，可以造成额外伤害。
                }
                bx = SkillFactory.getSkill(双弩.伊师塔之环); //23121000 - 伊师塔之环 - 借助传说中的武器伊师塔的力量，快速向前方的敌人发射箭矢。按住技能键时，可以持续发射箭矢。此外还拥有永久性增加急袭双杀伤害的被动效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.急袭双杀, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.传说之矛); //23121002 - 传说之矛 - 在跳跃的同时投下传说之枪，攻击前方的多个敌人。可以使对象的防御力一定程度减少，100%判定为爆击。此外还拥有永久性地增加飞叶龙卷风伤害的被动效果。\n需要技能：#c飞叶龙卷风10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.飞叶龙卷风, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.古老意志); //23121004 - 古老意志 - 在一定时间内获得古代精灵的祝福，伤害和HP增加，永久性地提高火焰咆哮的回避率。\n需要技能：#c火焰咆哮5级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    dodgeChance += bx.getEffect(bof).getProp();
                }
                bx = SkillFactory.getSkill(双弩.双弩枪专家); //23120009 - 双弩枪专家 - 增加双弩枪系列武器的熟练度、物理攻击力和爆击最小伤害。\n需要技能：#c精准双弩枪20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(双弩.防御突破); //23120010 - 防御突破 - 攻击时有一定概率100%无视敌人的防御力。对BOSS怪同样有效。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getX();
                }
                bx = SkillFactory.getSkill(双弩.旋转月瀑坠击); //23120011 - 旋转月瀑坠击 - 用冲锋拳将敌人打到空中后使用的连续技能。快速旋转，向打到空中的敌人发动连续攻击。拥有永久性增加冲锋拳伤害的被动效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.冲锋拳, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.进阶终极攻击); //23120012 - 进阶终极攻击 - 永久性地增加攻击力和命中率，终极：双弩枪技能的发动概率和伤害大幅上升。\n需要技能：#c终极：双弩枪20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    addDamageIncrease(双弩.精准双弩枪, eff.getDamage());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(双弩.伊师塔之环_强化); //23120043 - 伊师塔之环-强化 - 提升伊师塔之环的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.伊师塔之环, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.伊师塔之环_链接强化); //23120045 - 伊师塔之环-链接强化 - 提升伊师塔之环伤害增加量。\n前置技能：#c伊师塔之环1级以上
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.急袭双杀, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.传说之矛_强化); //23120049 - 传说之矛-强化 - 提升传说之矛的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.传说之矛, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(双弩.传说之矛_链接强化); //23120051 - 传说之矛-链接强化 - 提升飞叶龙卷风的伤害增加量。\n前置技能：#c传说之矛1级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(双弩.飞叶龙卷风, bx.getEffect(bof).getDAMRate());
                }
                break;
            }
            case 2400:
            case 2410:
            case 2411:
            case 2412: {
                bx = SkillFactory.getSkill(幻影.迅捷幻影); //24001002 - 迅捷幻影 - 永久性提高移动速度、最大移动速度、跳跃力，在跳跃中使用时，可以向前方飞行很远距离。技能等级越高，移动距离越远。可以用作跳跃键。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    speed += eff.getPassiveSpeed();
                    jump += eff.getPassiveJump();
                }
                bx = SkillFactory.getSkill(幻影.快速逃避); //24000003 - 快速逃避 - 永久提高回避率。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getX();
                }
                bx = SkillFactory.getSkill(幻影.超级幸运星); //24100006 - 超级幸运星 - 永久提高运气。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(幻影.神秘的运气); //24111002 - 神秘的运气 - 最幸运的幻影可以永久性地提高运气。使用技能时，进入可以避免一次死亡并恢复体力的幸运状态。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(幻影.幻影突击); //24111006 - 幻影突击 - 用卡片组成巨大的枪，和枪一起突击，发动连续半月斩。\n可以在使用和风卡浪后作为连续技使用，此时和风卡浪的延迟时间减少。永久增加和风卡浪的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(幻影.和风卡浪, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(幻影.黑色秘卡); //24120002 - 黑色秘卡 - 幻影的攻击造成爆击时，有一定概率从幻影身上飞出卡片，自动攻击周围的敌人。#c此时卡片值增加1。#\n此外回避概率永久性增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getX();
                }
                bx = SkillFactory.getSkill(幻影.暮光祝福); //24121003 - 暮光祝福 - 向后跳跃，用卡片向前方的敌人发动猛烈攻击。\n可以在使用幻影突击后作为连续技使用，此时幻影突击的延迟时间减少。此外永久提高幻影突击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(幻影.幻影突击, eff.getDAMRate());
                    addDamageIncrease(幻影.幻影突击1, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(幻影.手杖专家); //24120006 - 手杖专家 - 增加手杖系列武器的熟练度、物理攻击力、爆击最小伤害。\n需要技能：#c精准手杖20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX(); //增加攻击
                    trueMastery += eff.getMastery(); //熟练度
                    passive_sharpeye_min_percent += eff.getCriticalMin(); //爆击最小伤害
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(幻影.卡片风暴_强化); //24120043 - 卡片风暴-强化 - 提升卡片风暴的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(幻影.卡片风暴, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(幻影.卡片风暴_缩短冷却时间); //24120044 - 卡片风暴-缩短冷却时间 - 卡片风暴的冷却时间缩短。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(幻影.卡片风暴, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(幻影.卡片风暴_额外目标); //24120045 - 卡片风暴-额外目标 - 提升卡片风暴攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(幻影.卡片风暴, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(幻影.蓝光连击_强化); //24120046 - 蓝光连击-强化 - 提升蓝光连击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(幻影.蓝光连击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(幻影.蓝光连击_额外目标); //24120047 - 蓝光连击-额外目标 - 提升蓝光连击攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(幻影.蓝光连击, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 2005: //隐月
            case 2500:
            case 2510:
            case 2511:
            case 2512: {
                bx = SkillFactory.getSkill(隐月.乾坤一体); //25000105 - 乾坤一体 - 吸收天地之息，调节身体，增加物理防御力/魔法防御力和最大HP/MP。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP(); //最大Hp增加
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                    wdef += eff.getWdefX(); //物理防御力
                    mdef += eff.getWdefX(); //魔法防御力
                }
                bx = SkillFactory.getSkill(隐月.后方移动); //25101205 - 后方移动 - 向后方滑动，永久增加回避率。\n#c在使用技能过程中可以移动。#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    dodgeChance += eff.getX();
                }
                bx = SkillFactory.getSkill(隐月.力量锻炼); //25100108 - 力量锻炼 - 通过锻炼身体, 永久性增加力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                }
                bx = SkillFactory.getSkill(隐月.精灵凝聚第3招); //25110107 - 精灵凝聚第3招 - 强化与精灵的团结，永久增加攻击力和伤害。\n[需要技能]：#c精灵凝聚第2招10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    percent_damage += eff.getDAMRate();
                }
                bx = SkillFactory.getSkill(隐月.招魂式); //25110108 - 招魂式 - 将精灵的力量与身体融合，物理防御力、魔法防御力、增加状态异常抗性、所有属性抗性增加。[需要技能]：#c乾坤一体20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    wdef += eff.getWdefX(); //物理防御力
                    mdef += eff.getWdefX(); //魔法防御力
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(隐月.精灵凝聚第4招); //25120112 - 精灵凝聚第4招 - 强化与精灵的团结，永久性地在攻击时无视敌人的部分防御力，BOSS攻击力增加。[需要技能]：#c精灵凝聚第3招20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob(); //无视防御
                    percent_boss_damage_rate += eff.getBossDamage(); //BOSS伤害
                }
                /*
                 * 处理超级技能
                 */
                bx = SkillFactory.getSkill(隐月.鬼斩_额外攻击); //25120148 - 鬼斩-额外攻击 - 增加鬼斩的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(隐月.鬼斩, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(隐月.爆流拳_额外目标); //25120150 - 爆流拳-额外目标 - 增加爆流拳攻击的敌人数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(隐月.爆流拳, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(隐月.爆流拳_2, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(隐月.爆流拳_3, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(隐月.爆流拳_4, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 2004: //夜光
            case 2700:
            case 2710:
            case 2711:
            case 2712: {
                bx = SkillFactory.getSkill(夜光.魔力延伸); //20040218 - 穿透 - 用穿透一切阻碍的光之力量，无视敌人的部分防御力
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
                bx = SkillFactory.getSkill(夜光.穿透); //20040218 - 穿透 - 用穿透一切阻碍的光之力量，无视敌人的部分防御力
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(夜光.光之力量); //20040221 - 光之力量 - 与命运对抗的意志和魔法师特有的洞察力。智力值很高，受光之力量的保护而不受暗黑效果侵袭。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localint_ += eff.getIntX();
                    chra.getTrait(MapleTraitType.will).setLevel(20, chra);
                    chra.getTrait(MapleTraitType.insight).setLevel(20, chra);
                }
                bx = SkillFactory.getSkill(夜光.普通魔法防护); //27000003 - 普通魔法防护 - 受到的伤害由一定比例的MP代替。额外的永久增加物理、魔法防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    wdef += eff.getWdefX();
                    mdef += eff.getMdefX();
                }
                bx = SkillFactory.getSkill(夜光.光束瞬移); //27001002 - 光束瞬移 - 变成光束移动。按住方向键施展技能就能瞬移到目标方向。瞬移后的短暂时间内会变成透明状态。作为被动效果，永久增加移动速度和跳跃力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(夜光.光明黑暗魔法强化); //27000207 - 光明/黑暗魔法强化 - 永久提升光明/黑暗系列魔法的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(夜光.耀眼光球, eff.getMdRate());
                    addDamageIncrease(夜光.仙女发射, eff.getMdRate());
                    addDamageIncrease(夜光.闪爆光柱, eff.getMdRate());
                    addDamageIncrease(夜光.超级光谱, eff.getMdRate());
                    addDamageIncrease(夜光.闪耀救赎, eff.getMdRate());
                    addDamageIncrease(夜光.死亡之刃, eff.getMdRate());
                    addDamageIncrease(夜光.闪电反击, eff.getMdRate());
                    addDamageIncrease(夜光.晨星坠落_爆炸, eff.getMdRate());
                    addDamageIncrease(夜光.黑暗降临, eff.getMdRate());
                    addDamageIncrease(夜光.虚空重压, eff.getMdRate());
                    addDamageIncrease(夜光.暗锁冲击, eff.getMdRate());
                    addDamageIncrease(夜光.死亡之刃, eff.getMdRate());
                    addDamageIncrease(夜光.启示录, eff.getMdRate());
                    addDamageIncrease(夜光.绝对死亡, eff.getMdRate());
                }
                bx = SkillFactory.getSkill(夜光.智慧激发); //27100006 - 智慧激发 - 永久提升智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(夜光.抵抗之魔法盾); //27111004 - 抵抗之魔法盾 - 使用可以无视状态异常的保护罩。使用无视状态异常的效果，且使用次数超过指定次数以上时，进入冷却时间。作为被动效果，增加所有属性抗性和状态异常抵抗力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    ASR += bx.getEffect(bof).getASRRate();
                    TER += bx.getEffect(bof).getTERRate();
                }
                bx = SkillFactory.getSkill(夜光.暗光精通); //27120008 - 暗光精通 - 增加平衡增益的持续时间并加快使用各系列技能时光和黑暗增加的速度。额外增加黑色死亡的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(夜光.死亡之刃, eff.getDAMRate());
                    gauge_x = eff.getX();
                    addBuffDuration(夜光.平衡_光明, eff.getDuration());
                    addBuffDuration(夜光.平衡_黑暗, eff.getDuration());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(夜光.闪电反击_强化); //27120043 - 闪电反击-强化 - 提升闪电反击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(夜光.闪电反击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(夜光.启示录_强化); //27120046 - 启示录-强化 - 提升启示录的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(夜光.启示录, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(夜光.启示录_额外目标); //27120048 - 启示录-额外目标 - 提升启示录攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(夜光.启示录, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(夜光.绝对死亡_强化); //27120049 - 绝对死亡-强化 - 提升绝对死亡的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(夜光.绝对死亡, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(夜光.绝对死亡_额外目标); //27120050 - 绝对死亡-额外目标 - 提升绝对死亡攻击的怪物数
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(夜光.绝对死亡, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 3001:
            case 3100:
            case 3110:
            case 3111:
            case 3112: //恶魔猎手
            case 3101:
            case 3120:
            case 3121:
            case 3122: { //恶魔复仇者
//                mpRecoverProp = 100;
//                bx = SkillFactory.getSkill(恶魔.恶魔之怒); //恶魔之怒 - 对象是BOSS怪时，唤醒内在的愤怒，造成更强的伤害，吸收更多的精气
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    eff = bx.getEffect(bof);
//                    percent_boss_damage_rate += eff.getBossDamage();
//                    mpRecover += eff.getX();
//                    mpRecoverProp += eff.getBossDamage();
//                }
                bx = SkillFactory.getSkill(恶魔猎手.恶魔之血); //恶魔之血 - 魔族天生就拥有先天的强大意志和压倒性的领导力
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    chra.getTrait(MapleTraitType.will).setLevel(20, chra);
                    chra.getTrait(MapleTraitType.charisma).setLevel(20, chra);
                }
                bx = SkillFactory.getSkill(恶魔复仇者.野性狂怒); //30010241 - 野性狂怒 - 由于愤怒，伤害增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage += eff.getDAMRate();
                }
                //恶魔复仇者
                if (JobConstants.is恶魔复仇者(chra.getJob())) {
                    bx = SkillFactory.getSkill(恶魔复仇者.恶魔之力); //31010003 - 恶魔之力 - 永久增加移动速度和最高移动速度、跳跃力、爆击率。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        jump += eff.getPassiveJump();
                        speed += eff.getPassiveSpeed();
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.铜墙铁壁); //31200004 - 铜墙铁壁 - 凭借坚强的意志，使物理防御力和魔法防御力大幅提高。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_wdef += eff.getWDEFRate(); //增加物防
                        percent_mdef += eff.getMDEFRate(); //增加魔防
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.亡命剑精通); //31200005 - 亡命剑精通 - 永久增加亡命剑的熟练度和命中值。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        accuracy += eff.getAccX();
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.心灵之力); //31200006 - 心灵之力 - 永久增加力量和防御力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        localstr += eff.getStrX();
                        wdef += eff.getWdefX();
                        mdef += eff.getMdefX();
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.冒险岛勇士); //31200006 - 心灵之力 - 永久增加力量和防御力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_hp += eff.getPercentHP();
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.负荷缓解); //31210005 - 负荷缓解 - 永久缓解由于超越负荷而减少的生命吸收的HP吸收量。额外强化超越技能的攻击力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(恶魔复仇者.超越十字斩, eff.getDAMRate()); //超越十字斩
                        addDamageIncrease(恶魔复仇者.超越十字斩_1, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越十字斩_2, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越十字斩_3, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越恶魔突袭, eff.getDAMRate()); //超越恶魔突袭
                        addDamageIncrease(恶魔复仇者.超越恶魔突袭_1, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越恶魔突袭_2, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越恶魔突袭_3, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越恶魔突袭_4, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越月光斩, eff.getDAMRate()); //超越月光斩
                        addDamageIncrease(恶魔复仇者.超越月光斩_1, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越月光斩_2, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越月光斩_3, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越月光斩_4, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越处决, eff.getDAMRate()); //超越处决
                        addDamageIncrease(恶魔复仇者.超越处决_1, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越处决_2, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越处决_3, eff.getDAMRate());
                        addDamageIncrease(恶魔复仇者.超越处决_4, eff.getDAMRate());
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.进阶生命吸收); //31210006 - 进阶生命吸收 - 永久增加通过生命吸收吸收的HP数值。\r\n需要技能：#c生命吸收10级以上#
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        hpRecover_Percent += eff.getX();
                    }
                    bx = SkillFactory.getSkill(恶魔复仇者.防御专精); //31220005 - 防御专精 - 永久提高使用盾牌的能力，有一定概率无视敌人的防御力，提高盾牌技能#c持盾突击#和#c追击盾#的攻击力。盾牌技能的防御力无视比例增加2倍。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                        addDamageIncrease(恶魔复仇者.持盾突击, eff.getX()); //持盾突击
                        addDamageIncrease(恶魔复仇者.持盾突击_1, eff.getX());
                        addDamageIncrease(恶魔复仇者.追击盾, eff.getX()); //追击盾
                        addDamageIncrease(恶魔复仇者.追击盾_攻击, eff.getX());
                    }
                    /*
                     * 超级技能
                     */
                    bx = SkillFactory.getSkill(恶魔复仇者.追击盾_额外目标); //31220050 - 追击盾-额外目标 - 永久增加追击盾攻击的怪物数量。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addAttackCount(恶魔复仇者.追击盾, bx.getEffect(bof).getZ());
                        addAttackCount(恶魔复仇者.追击盾_攻击, bx.getEffect(bof).getZ());
                    }
                } else {
                    bx = SkillFactory.getSkill(恶魔猎手.HP增加); //HP增加 - 最大HP永久增加
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        percent_hp += bx.getEffect(bof).getPercentHP();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔血月斩1次强化); //恶魔血月斩1次强化 - 永久性地增加恶魔血月斩的伤害
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(恶魔猎手.恶魔血月斩, eff.getDAMRate()); //恶魔血月斩
                        addDamageIncrease(恶魔猎手.恶魔血月斩1, eff.getDAMRate());
                        addDamageIncrease(恶魔猎手.恶魔血月斩2, eff.getDAMRate());
                        addDamageIncrease(恶魔猎手.恶魔血月斩3, eff.getDAMRate());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.物理训练); //物理训练 - 永久性地增加力量和敏捷
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        localstr += eff.getStrX();
                        localdex += eff.getDexX();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔血月斩2次强化); //恶魔血月斩2次强化 - 提高恶魔血月斩的伤害。\n需要技能：#c恶魔血月斩1次强化1级#
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(恶魔猎手.恶魔血月斩, eff.getX()); //恶魔血月斩
                        addDamageIncrease(恶魔猎手.恶魔血月斩1, eff.getX());
                        addDamageIncrease(恶魔猎手.恶魔血月斩2, eff.getX());
                        addDamageIncrease(恶魔猎手.恶魔血月斩3, eff.getX());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.邪恶拷问); //邪恶拷问 - 攻击状态异常的敌人时，伤害和爆击概率增加
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_damage_rate += eff.getX();
                        percent_boss_damage_rate += eff.getX();
                        passive_sharpeye_rate += eff.getY();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.黑暗束缚); //黑暗束缚 - 有一定概率使周围的多个敌人陷入无法行动的状态，造成持续伤害。拥有一定概率无视怪物防御力的被动效果，黑暗束缚的效果对BOSS同样有效
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.精神集中); //31110007 - 精神集中 - 通过集中精神，永久性地增加伤害，使攻击速度提高1个阶段
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        percent_damage += bx.getEffect(bof).getDAMRate();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔血月斩最终强化); //恶魔血月斩最终强化 - 最终对恶魔血月斩的伤害进行强化。\n需要技能：#c恶魔血月斩2次强化1级#
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(恶魔猎手.恶魔血月斩, eff.getX());
                        addDamageIncrease(恶魔猎手.恶魔血月斩1, eff.getX());
                        addDamageIncrease(恶魔猎手.恶魔血月斩2, eff.getX());
                        addDamageIncrease(恶魔猎手.恶魔血月斩3, eff.getX());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.进阶精准武器); //进阶精准武器 - 将单手钝器、单手斧系列武器的熟练度提高到极限，增加爆击最小伤害和物理攻击力。\n需要技能：#c精准武器20级以上#
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        watk += eff.getAttackX();
                        trueMastery += eff.getMastery();
                        passive_sharpeye_min_percent += eff.getCriticalMin();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.皮肤硬化); //未知 难道是皮肤硬化 - 永久性地强化身体，减少敌人造成的伤害
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        percent_wdef += bx.getEffect(bof).getT();
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.死亡诅咒); //死亡诅咒 - 攻击时有一定概率使敌人一击必杀，一击必杀效果发动时，恢复自己的HP。攻击对象死亡时，有一定概率吸收一定量的精气。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        hpRecover_Percent += eff.getX();
                        hpRecoverProp += eff.getProp();
                    }
                    /*
                     * 超级技能
                     */
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔呼吸_强化); //31120043 - 恶魔呼吸-强化 - 增加恶魔呼吸的伤害。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addDamageIncrease(恶魔猎手.恶魔呼吸, bx.getEffect(bof).getDAMRate());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔呼吸_额外攻击); //31120044 - 恶魔呼吸-额外攻击 - 增加恶魔呼吸的攻击次数。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addAttackCount(恶魔猎手.恶魔呼吸, bx.getEffect(bof).getAttackCount());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.黑暗变形_强化); //31120047 - 黑暗变形-强化 - 增加黑暗变形的伤害。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addDamageIncrease(恶魔猎手.黑暗变形, bx.getEffect(bof).getDAMRate());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔冲击波_强化); //31120049 - 恶魔冲击波-强化 - 增加恶魔冲击波的伤害。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addDamageIncrease(恶魔猎手.恶魔冲击波, bx.getEffect(bof).getDAMRate());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.恶魔冲击波_额外攻击); //31120050 - 恶魔冲击波-额外攻击 - 增加恶魔冲击波的攻击次数。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        addAttackCount(恶魔猎手.恶魔冲击波, bx.getEffect(bof).getAttackCount());
                    }
                    bx = SkillFactory.getSkill(恶魔猎手.蓝血); //31121054 - 蓝血 - 唤醒自己血液中的贵族血统，让力量觉醒。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        incMaxDamage += bx.getEffect(bof).getMaxDamageOver();
                    }
                }
                break;
            }
            case 3200: //幻灵
            case 3210:
            case 3211:
            case 3212: {
                bx = SkillFactory.getSkill(唤灵斗师.长杖艺术); //32000015 - 长杖艺术 - 锻炼使用长杖的方法，把它变成一种技能。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    magic += eff.getMagicX();
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(唤灵斗师.智慧激发); //32100007 - 智慧激发 - 通过精神修养，永久性地提高智力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localint_ += bx.getEffect(bof).getIntX();
                }
                bx = SkillFactory.getSkill(唤灵斗师.普通转化); //32100008 - 普通转化 - 大幅增加自己的最大HP。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(唤灵斗师.战斗精通); //32110001 - 战斗精通 - 学习更高深的战斗技术，永久性增加伤害和爆击最小伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage += eff.getDAMRate();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(唤灵斗师.斗战突击); //32111015 - 斗战突击 - 向前突进，击退多个敌人。施展后，可与#c致命冲击进行衔接使用#。此外，永久增加超级黑暗锁链的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(唤灵斗师.黑暗锁链, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(唤灵斗师.黑暗创世); //32121004 - 黑暗创世 - 用黑暗之光攻击最多15个敌人，使其在一定时间内昏迷。如学习了黑暗闪电技能，可以永久性增加黑暗闪电的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(唤灵斗师.黑暗闪电, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(唤灵斗师.暴怒对战); //32121010 - 暴怒对战 一定时间内进入可以集中精神攻击一个敌人的状态，大幅提高伤害值。同时永久激活身体，永久增加自己的最大HP和MP、防御力，可以无视怪物的部分防御力。即使受到敌人的攻击也不会解除增益。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP(); //最大Hp增加
                    percent_mp += eff.getPercentMP(); //最大Mp增加
                    percent_wdef += eff.getWDEFRate(); //增加物防
                    percent_mdef += eff.getMDEFRate(); //增加魔防
                    percent_ignore_mob_def_rate += eff.getIgnoreMob(); //无视怪物防御
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(唤灵斗师.黑暗创世_缩短冷却时间); //32120057 - 黑暗创世-缩短冷却时间 - 减少黑暗创世的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(唤灵斗师.黑暗创世, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(唤灵斗师.黑暗创世_强化); //32120058 - 黑暗创世-强化 - 增加黑暗创世的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(唤灵斗师.黑暗创世, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(唤灵斗师.避难所_缩短冷却时间); //32120063 - 避难所-缩短冷却时间 - 减少避难所的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(唤灵斗师.避难所, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(唤灵斗师.避难所_坚持); //32120064 - 避难所-坚持 - 提升避难所的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(唤灵斗师.避难所, bx.getEffect(bof).getDuration());
                }
                break;
            }
            case 3300:
            case 3310:
            case 3311:
            case 3312: {
                bx = SkillFactory.getSkill(豹弩游侠.召唤美洲豹_灰);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_atk += Math.min(180, chra.getLevel());
                    addDamageIncrease(豹弩游侠.召唤美洲豹_灰, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_黄, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_红, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_紫, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_蓝, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_剑, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_雪, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_玛瑙, Math.min(180, chra.getLevel()));
                    addDamageIncrease(豹弩游侠.召唤美洲豹_铠甲, Math.min(180, chra.getLevel()));
                }
                eff = SkillFactory.getSkill(豹弩游侠.利爪狂风_EX).getEffect(1);
                if (eff != null) {
                    addDamageIncrease(豹弩游侠.利爪狂风, eff.getDamage() + Math.max(180, chra.getLevel()));
                }
                eff = SkillFactory.getSkill(豹弩游侠.十字攻击_EX).getEffect(1);
                if (eff != null) {
                    addDamageIncrease(豹弩游侠.十字攻击, eff.getDamage() + Math.max(180, chra.getLevel()) * 2);
                }

                bx = SkillFactory.getSkill(豹弩游侠.自动射击装置); //33000005 - 自动射击装置 - 将反抗者的技术和弩弓结合，可以更快、更简单地发射箭矢。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX(); //提升攻击力
                    accuracy += eff.getAccX(); //提升命中
                    percent_acc += eff.getPercentAcc(); //提升x%的命中
                    defRange += eff.getRange(); //提升攻击距离
                }
                bx = SkillFactory.getSkill(豹弩游侠.物理训练); //33100010 - 物理训练 - 通过锻炼身体，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(豹弩游侠.美洲豹精通); //33120000 - 神弩手 - 弩系列武器的熟练度和物理攻击力、爆击最小伤害。\n前置技能：#c精准弩10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(豹弩游侠.美洲豹传动);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getWatk() * eff.getX();
                    passive_sharpeye_rate += eff.getY() * eff.getX();
                    passive_sharpeye_min_percent += 2 * eff.getX();
                }
                bx = SkillFactory.getSkill(豹弩游侠.暴走形态); //33120000 - 神弩手 - 弩系列武器的熟练度和物理攻击力、爆击最小伤害。\n前置技能：#c精准弩10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(豹弩游侠.神弩手); //33120000 - 神弩手 - 弩系列武器的熟练度和物理攻击力、爆击最小伤害。\n前置技能：#c精准弩10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getX();
                    trueMastery += eff.getMastery();
                    passive_sharpeye_min_percent += eff.getCriticalMin();
                }
                bx = SkillFactory.getSkill(豹弩游侠.野性本能); //33120010 - 野性本能 - 攻击时无视怪物的部分物理防御力，有一定概率回避敌人的攻击。永久增加所有属性抗性。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    dodgeChance += eff.getER();
                }
                bx = SkillFactory.getSkill(豹弩游侠.进阶终极攻击); //33120011 - 进阶终极攻击 - 永久性地增加攻击力和命中率，终极弓弩技能的发动概率和伤害大幅上升。\n需要技能：#c终极弓弩20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    addDamageIncrease(豹弩游侠.终极弓弩, eff.getDamage());
                }
                /*
                 * 超级技能
                 */
//                bx = SkillFactory.getSkill(豹弩游侠.音速震波_强化); //33120046 - 音速震波-强化 - 增加音速震波的伤害。
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    addDamageIncrease(豹弩游侠.音速震波, bx.getEffect(bof).getDAMRate());
//                }
//                bx = SkillFactory.getSkill(豹弩游侠.音速震波_额外目标); //33120047 - 音速震波-额外目标 - 增加音速震波攻击的怪物数。
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    addTargetPlus(豹弩游侠.音速震波, bx.getEffect(bof).getTargetPlus());
//                }
//                bx = SkillFactory.getSkill(豹弩游侠.音速震波_额外攻击); //33120048 - 音速震波-额外攻击 - 增加音速震波的攻击次数。
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    addAttackCount(豹弩游侠.音速震波, bx.getEffect(bof).getAttackCount());
//                }
                bx = SkillFactory.getSkill(豹弩游侠.奥义箭乱舞_强化); //33120049 - 奥义箭乱舞-强化 - 增加奥义箭乱舞的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(豹弩游侠.奥义箭乱舞, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(豹弩游侠.奥义箭乱舞_无视防御); //33120051 - 奥义箭乱舞-无视防御 - 增加奥义箭乱舞的无视防御力效果。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(豹弩游侠.奥义箭乱舞, bx.getEffect(bof).getIgnoreMob());
                }
                break;
            }
            case 3510:
            case 3511:
            case 3512: {
                bx = SkillFactory.getSkill(机械师.物理训练); //物理训练 - 通过身体锻炼，永久性地提高力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(机械师.机械精通); //机械精通
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(机械师.机械防御系统); //机械精通
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                    percent_mp += bx.getEffect(bof).getPercentMP();
                }
//                bx = SkillFactory.getSkill(机械师.终极机甲); //终极机甲
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    trueMastery += bx.getEffect(bof).getMastery();
//                    percent_hp += bx.getEffect(bof).getPercentHP();
//                }
                bx = SkillFactory.getSkill(机械师.机器人精通); //机器人精通 - 提高所有召唤机器人的攻击力、自爆伤害和持续时间
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(机械师.机器人发射器_RM7, eff.getX());
                    addDamageIncrease(机械师.机器人工厂_RM1, eff.getX());
                    BuffUP_Summon += eff.getY();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(机械师.磁场_强化); //35120043 - 磁场-强化 - 增加磁场的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(机械师.磁场, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(机械师.磁场_坚持); //35120044 - 磁场-坚持 - 增加磁场的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(机械师.磁场, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(机械师.磁场_缩短冷却时间); //35120045 - 磁场-缩短冷却时间 - 减少磁场的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(机械师.磁场, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(机械师.支援波动器_H_EX_强化); //35120046 - 支援波动器：H-EX-强化 - 增加支援波动器：H-EX的爆炸伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(机械师.支援波动器_H_EX, bx.getEffect(bof).getDAMRate());
                }
//                bx = SkillFactory.getSkill(机械师.支援波动器_H_EX_组队强化); //35120047 - 支援波动器：H-EX-组队强化 - 增加支援波动器：H-EX强化的队员伤害。
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0 && bx != null) {
//                    addBuffDuration(机械师.磁场, bx.getEffect(bof).getDuration());
//                }
                bx = SkillFactory.getSkill(机械师.支援波动器_H_EX_坚持); //35120048 - 支援波动器：H-EX-坚持 - 增加支援波动器：H-EX的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(机械师.支援波动器_H_EX, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(机械师.集中射击_强化); //35120049 - 集中射击-强化 - 增加集中射击<SPLASH-F>和<IRON-B>的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(机械师.集中射击_SPLASH_F, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(机械师.集中射击_IRON_B, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(机械师.集中射击_SPLASH_F_额外目标); //35120050 - 集中射击：SPLASH-F-额外目标 - 增加集中射击：SPLASH-F攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(机械师.集中射击_SPLASH_F, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(机械师.集中射击_IRON_B_额外攻击); //35120051 - 集中射击：IRON-B-额外攻击 - 增加集中射击：IRON-B的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(机械师.集中射击_IRON_B, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 3002: //尖兵
            case 3600:
            case 3610:
            case 3611:
            case 3612: {
                bx = SkillFactory.getSkill(尖兵.混合逻辑); //30020233 - 混合逻辑 - 采用混合逻辑设计，所有能力值永久提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += (eff.getStrRate() / 100.0) * str;
                    localdex += (eff.getDexRate() / 100.0) * dex;
                    localluk += (eff.getLukRate() / 100.0) * luk;
                    localint_ += (eff.getIntRate() / 100.0) * int_;
                }
                bx = SkillFactory.getSkill(尖兵.神经系统改造); //36000003 - 神经系统改造 - 强化脊柱的神经系统，提高和运动有关的所有数值。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getPercentAcc();
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(尖兵.超能力量); //36001002 - 超能力量 - 提高能量，增加伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(尖兵.精英支援); //36100005 - 精英支援 - 永久增加所有能力值
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(尖兵.尖兵精通); //36100006 - 尖兵精通 - 熟练度和物理攻击力提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(尖兵.精准火箭1次强化); //36100010 - 精准火箭1次强化 - 强化精准火箭的伤害。\n[需要技能]：#c精准火箭4级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(尖兵.精准火箭, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(尖兵.直线透视); //36101002 - 直线透视 - 在一定时间内激活视觉，看穿敌人的弱点，提高爆击率。消耗备用能量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                }
                bx = SkillFactory.getSkill(尖兵.精准火箭2次强化); //36110012 - 精准火箭2次强化 - 增强精准火箭的伤害。\n[需要技能]：#c精准火箭1次强化4级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    if (damageIncrease.containsKey(尖兵.精准火箭)) {
                        addDamageIncrease(尖兵.精准火箭, damageIncrease.get(尖兵.精准火箭) + eff.getDAMRate());
                    }
                }
                bx = SkillFactory.getSkill(尖兵.双重防御); //36111003 - 双重防御 - 额外回避率100%，之后每次回避/受到攻击时，回避率和受到的伤害减少。此外，防御力永久增加。\n[需要技能]：#c直线透视5级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                    wdef += eff.getWdefX();
                    mdef += eff.getMdefX();
                }
                bx = SkillFactory.getSkill(尖兵.精准火箭最终强化); //36120015 - 精准火箭最终强化 - 使精准火箭的伤害最大化。\n[需要技能]：#c精准火箭2次强化4级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    if (damageIncrease.containsKey(尖兵.精准火箭)) {
                        addDamageIncrease(尖兵.精准火箭, damageIncrease.get(尖兵.精准火箭) + eff.getDAMRate());
                    }
                }
                bx = SkillFactory.getSkill(尖兵.神秘代码); //36121003 - 神秘代码 - 在一定时间内发挥出当前世界上不可能出现的强大力量，增加总伤害和攻击BOSS时的伤害，所有能力值永久增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(尖兵.刀锋之舞_强化); //36120044 - 刀锋之舞 - 强化 - 增加刀锋之舞的攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(尖兵.刀锋之舞, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(尖兵.刀锋之舞_额外目标); //36120045 - 刀锋之舞 - 额外目标 - 增加刀锋之舞的目标对象数量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(尖兵.刀锋之舞, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(尖兵.聚能脉冲炮_强化); //36120046 - 聚能脉冲炮 - 强化 - 增加聚能脉冲炮的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(尖兵.聚能脉冲炮_狙击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(尖兵.聚能脉冲炮_炮击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(尖兵.聚能脉冲炮_暴击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(尖兵.聚能脉冲炮_无视防御); //36120047 - 聚能脉冲炮 - 无视防御 - 增加聚能脉冲炮的无视防御比例
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addIgnoreMobpdpRate(尖兵.聚能脉冲炮_狙击, bx.getEffect(bof).getIgnoreMob());
                    addIgnoreMobpdpRate(尖兵.聚能脉冲炮_炮击, bx.getEffect(bof).getIgnoreMob());
                    addIgnoreMobpdpRate(尖兵.聚能脉冲炮_暴击, bx.getEffect(bof).getIgnoreMob());
                }
                bx = SkillFactory.getSkill(尖兵.聚能脉冲炮_额外目标); //36120048 - 聚能脉冲炮 - 额外目标 - 增加聚能脉冲炮的目标对象数量。但狙击单个敌人时不增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(尖兵.聚能脉冲炮_炮击, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(尖兵.聚能脉冲炮_暴击, bx.getEffect(bof).getTargetPlus());
                }
                break;
            }
            case 3700:
            case 3710:
            case 3711:
            case 3712: {
                bx = SkillFactory.getSkill(37001001);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    chra.getSpecialStat().setBullet(3);
                }
                bx = SkillFactory.getSkill(37100007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    chra.getSpecialStat().setBullet(4);
                }
                bx = SkillFactory.getSkill(37110007);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    chra.getSpecialStat().setBullet(5);
                }
                bx = SkillFactory.getSkill(37120008);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    chra.getSpecialStat().setBullet(6);
                }
                bx = SkillFactory.getSkill(37100004);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    watk += bx.getEffect(bof).getAttackX();
                }
//                bx = SkillFactory.getSkill(37100005);
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
//                    this.mh += bx.getEffect(bof).getWdef2Dam();
//                    this.mi += bx.getEffect(bof).getAcc2Dam();
//                }
                bx = SkillFactory.getSkill(37000006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
//                bx = SkillFactory.getSkill(37110008);
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
//                    this.nB += bx.getEffect(bof).getASRRate();
//                    this.nC += bx.getEffect(bof).getTERRate();
//                }
//                bx = SkillFactory.getSkill(37110009);
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
//                    this.of += bx.getEffect(bof).getPadR();
//                }
//                bx = SkillFactory.getSkill(37120009);
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
//                    this.nB += bx.getEffect(bof).getASRRate();
//                    this.nC += bx.getEffect(bof).getTERRate();
//                }
//                bx = SkillFactory.getSkill(37120012);
//                bof = chra.getTotalSkillLevel(bx);
//                if (bof > 0) {
//                    this.of += bx.getEffect(bof).getPadR();
//                }
                bx = SkillFactory.getSkill(37100006);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_damage += (double) bx.getEffect(bof).getDAMRate();
                }
                bx = SkillFactory.getSkill(37120011);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0) {
                    percent_ignore_mob_def_rate += (1.0 - percent_ignore_mob_def_rate) * (double) bx.getEffect(bof).getIgnoreMob() / 100.0;
//                    this.ol += bx.getEffect(bof).getDamAbsorbShieldR();
                }
                break;
            }
            case 4100: //剑豪
            case 4110:
            case 4111:
            case 4112: {
                if (chra.getJob() >= 4111) {
                    bx = SkillFactory.getSkill(剑豪.迅速);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        dodgeChance += eff.getPercentAvoid();
                    }
                }
                if (chra.getJob() >= 4110) {
                    bx = SkillFactory.getSkill(剑豪.秘剑_斑鸠);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        accuracy += eff.getX();
                        passive_sharpeye_max_percent += eff.getCriticalMax();
                        passive_sharpeye_min_percent += eff.getCriticalMin();
                    }
                }
                if (chra.getJob() >= 4100) {
                    bx = SkillFactory.getSkill(剑豪.剑豪道);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        localstr += eff.getStrX();
                        localdex += eff.getDexX();
                    }
                }
                bx = SkillFactory.getSkill(剑豪.神速无双_连击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addAttackCount(剑豪.神速无双, eff.getAttackCount());
                }
                bx = SkillFactory.getSkill(剑豪.瞬杀_连击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addAttackCount(剑豪.瞬杀, eff.getAttackCount());
                }
                bx = SkillFactory.getSkill(剑豪.瞬杀_散击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addTargetPlus(剑豪.瞬杀, eff.getTargetPlus());
                }
                bx = SkillFactory.getSkill(剑豪.一闪_连击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addAttackCount(剑豪.一闪, eff.getAttackCount());
                }
                bx = SkillFactory.getSkill(剑豪.一闪_即击);
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(剑豪.一闪, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 5100: //米哈尔
            case 5110:
            case 5111:
            case 5112: {
                bx = SkillFactory.getSkill(米哈尔.灵魂盾); //51000001 - 灵魂盾 - 提升灵魂盾的力量，提高防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWdefX();
                    percent_mdef += eff.getMdefX();
                }
                bx = SkillFactory.getSkill(米哈尔.灵魂敏捷); //51000002 - 灵魂敏捷 - 永久提升命中值和移动速度、跳跃力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    accuracy += eff.getAccX();
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(米哈尔.增加HP); //51000000 - 增加HP - 永久增加最大HP。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(米哈尔.物理训练); //51100000 - 物理训练 - 通过锻炼身体，永久提升力量和敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    localdex += eff.getDexX();
                }
                bx = SkillFactory.getSkill(米哈尔.专注); //51110001 - 专注 - 集中精神，永久增加力量，攻击力提升1阶段。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                }
                bx = SkillFactory.getSkill(米哈尔.战斗精通); //51120000 - 战斗精通 - 攻击时无视一定程度的怪物防御力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                bx = SkillFactory.getSkill(米哈尔.进阶终结攻击); //51120002 - 进阶终结攻击 - 永久增加攻击力和命中率，终结攻击的发动概率和伤害值大幅提升。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    watk += eff.getAttackX();
                    addDamageIncrease(米哈尔.终结攻击, eff.getDamage());
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(米哈尔.闪耀爆炸_强化); //51120046 - 闪耀爆炸-强化 - 增加闪耀爆炸的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(米哈尔.闪耀爆炸, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(米哈尔.闪耀爆炸_额外目标); //51120047 - 闪耀爆炸-额外目标 - 增加闪耀爆炸攻击的怪物数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(米哈尔.闪耀爆炸, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(米哈尔.闪耀爆炸_额外攻击); //51120048 - 闪耀爆炸-额外攻击 - 增加闪耀爆炸的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(米哈尔.闪耀爆炸, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(米哈尔.灵魂抨击_强化); //51120049 - 灵魂抨击-强化 - 增加灵魂抨击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(米哈尔.灵魂抨击, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(米哈尔.灵魂抨击_额外攻击); //51120051 - 灵魂抨击-额外攻击 - 增加灵魂抨击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(米哈尔.灵魂抨击, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 6000: //狂龙战士
            case 6100:
            case 6110:
            case 6111:
            case 6112: {
                bx = SkillFactory.getSkill(狂龙战士.钢铁之墙); //60000222 - 钢铁之墙 - 具备钢铁意志的狂龙战士获得额外体力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_hp += bx.getEffect(bof).getPercentHP();
                }
                bx = SkillFactory.getSkill(狂龙战士.皮肤保护); //61000003 - 皮肤保护 - 强化皮肤，永久提升防御力，有一定概率进入不被击退的状态。和变身的稳如泰山效果重叠。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_wdef += eff.getWdefX();
                    percent_mdef += eff.getMdefX();
                }
                bx = SkillFactory.getSkill(狂龙战士.双重跳跃); //61001002 - 双重跳跃 - 跳跃中再次跳跃一次后，移动至远处。额外的永久增加移动速度和最大移动速度。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    speed += bx.getEffect(bof).getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(狂龙战士.飞龙斩1次强化); //61100009 - 飞龙斩1次强化 - 强化飞龙斩的攻击力。\n前置技能：#c飞龙斩20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(狂龙战士.飞龙斩, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_1, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_2, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.内心火焰); //61100007 - 内心火焰 - 永久提升力量和HP。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(狂龙战士.飞龙斩2次强化); //61110015 - 飞龙斩 2次强化 - 强化飞龙斩的攻击力。\n前置技能：#c飞龙斩1次强化 2级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(狂龙战士.飞龙斩, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_1, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_2, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.进阶内心火焰); //61110007 - 进阶内心火焰 - 永久提升力量和HP。\n前置技能：#c内心火焰10级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localstr += eff.getStrX();
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(狂龙战士.进阶剑刃之壁); //61120007 - 进阶剑刃之壁 - 强化剑刃之壁技能。召唤3把使用中的剑。召唤出的剑画出一道轨迹，寻找并攻击怪物。额外的永久提升攻击力。\n前置技能：#c剑刃之壁20级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(狂龙战士.飞龙斩3次强化); //61120020 - 飞龙斩 3次强化 - 强化飞龙斩的攻击力。\n前置技能：#c飞龙斩2次强化2级以上#
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    addDamageIncrease(狂龙战士.飞龙斩, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_1, eff.getDAMRate());
                    addDamageIncrease(狂龙战士.飞龙斩_2, eff.getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.无敌之勇); //61120011 - 无敌之勇 - 无视怪物的防御。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    percent_ignore_mob_def_rate += bx.getEffect(bof).getIgnoreMob();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(狂龙战士.怒雷屠龙斩_强化); //61120043 - 怒雷屠龙斩-强化 - 提升怒雷屠龙斩的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(狂龙战士.怒雷屠龙斩, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(狂龙战士.怒雷屠龙斩_变身, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.怒雷屠龙斩_坚持); //61120044 - 怒雷屠龙斩-坚持 - 提升怒雷屠龙斩的减速持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(狂龙战士.怒雷屠龙斩, bx.getEffect(bof).getDuration());
                    addBuffDuration(狂龙战士.怒雷屠龙斩_变身, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(狂龙战士.怒雷屠龙斩_额外攻击); //61120045 - 怒雷屠龙斩-额外攻击 - 提升怒雷屠龙斩的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(狂龙战士.怒雷屠龙斩, bx.getEffect(bof).getAttackCount());
                    addAttackCount(狂龙战士.怒雷屠龙斩_变身, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(狂龙战士.恶魔之息_强化); //61120046 - 恶魔之息-强化 - 提升恶魔之息的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(狂龙战士.恶魔之息, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.恶魔之息_暴怒坚持); //61120047 - 恶魔之息-暴怒坚持 - 提升恶魔之息产生的火焰持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(狂龙战士.恶魔之息_暴怒, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(狂龙战士.恶魔之息_暴怒强化); //61120048 - 恶魔之息-暴怒强化 - 提升恶魔之息产生的火焰的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(狂龙战士.恶魔之息_暴怒, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.扇击_强化); //61120049 - 扇击-强化 - 提升扇击的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(狂龙战士.扇击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(狂龙战士.扇击_1, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(狂龙战士.扇击_变身, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(狂龙战士.扇击_坚持); //61120050 - 扇击-坚持 - 提升扇击的持续时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addBuffDuration(狂龙战士.扇击, bx.getEffect(bof).getDuration());
                    addBuffDuration(狂龙战士.扇击_1, bx.getEffect(bof).getDuration());
                    addBuffDuration(狂龙战士.扇击_变身, bx.getEffect(bof).getDuration());
                }
                bx = SkillFactory.getSkill(狂龙战士.扇击_额外攻击); //61120051 - 扇击-额外攻击 - 提升扇击的攻击次数。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(狂龙战士.扇击, bx.getEffect(bof).getAttackCount());
                    addAttackCount(狂龙战士.扇击_1, bx.getEffect(bof).getAttackCount());
                    addAttackCount(狂龙战士.扇击_变身, bx.getEffect(bof).getAttackCount());
                }
                break;
            }
            case 6001: //爆莉萌天使
            case 6500:
            case 6510:
            case 6511:
            case 6512: {
                bx = SkillFactory.getSkill(爆莉萌天使.亲和Ⅰ); //65000003 - 亲和Ⅰ - 提升与爱丝卡达的亲和力，身体感到轻盈。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    jump += eff.getPassiveJump();
                    speed += eff.getPassiveSpeed();
                }
                bx = SkillFactory.getSkill(爆莉萌天使.精准灵魂手铳); //65100003 - 精准灵魂手铳 - 提升灵魂手铳的熟练度和命中值、物理攻击力。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    watk += bx.getEffect(bof).getAttackX();
                }
                bx = SkillFactory.getSkill(爆莉萌天使.内心之火); //65100004 - 内心之火 - 永久提升敏捷。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    localdex += bx.getEffect(bof).getDexX();
                }
                bx = SkillFactory.getSkill(爆莉萌天使.亲和Ⅱ); //65100005 - 亲和Ⅱ - 提升与爱丝卡达的亲和力，接受到战斗的经验，抵抗力提升。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                bx = SkillFactory.getSkill(爆莉萌天使.亲和Ⅲ); //65110006 - 亲和Ⅲ - 提升与爱丝卡达的亲和力，提升敏捷，熟读秘传。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localdex += eff.getDexX();
                    percent_damage += eff.getDAMRate();
                }
                /*
                 * 超级技能
                 */
                bx = SkillFactory.getSkill(爆莉萌天使.灵魂吸取_强化); //65120043 - 灵魂吸取-强化 - 提升灵魂吸取的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(爆莉萌天使.灵魂吸取_攻击, bx.getEffect(bof).getDAMRate());
                    addDamageIncrease(爆莉萌天使.灵魂吸取, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(爆莉萌天使.大地冲击波_强化); //65120046 - 大地冲击波-强化 - 提升大地冲击波的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(爆莉萌天使.大地冲击波, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(爆莉萌天使.大地冲击波_缩短冷却时间); //65120048 - 大地冲击波-缩短冷却时间 - 缩短大地冲击波的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(爆莉萌天使.大地冲击波, bx.getEffect(bof).getCooltimeReduceR());
                }
                bx = SkillFactory.getSkill(爆莉萌天使.灵魂共鸣_强化); //65120049 - 灵魂共鸣-强化 - 提升灵魂共鸣的伤害。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addDamageIncrease(爆莉萌天使.灵魂共鸣, bx.getEffect(bof).getDAMRate());
                }
                bx = SkillFactory.getSkill(爆莉萌天使.灵魂共鸣_缩短冷却时间); //65120050 - 灵魂共鸣-缩短冷却时间 - 缩短灵魂共鸣的冷却时间。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addCoolTimeReduce(爆莉萌天使.灵魂共鸣, bx.getEffect(bof).getCooltimeReduceR());
                }
                break;
            }
            case 10000: //神之子
            case 10100:
            case 10110:
            case 10111:
            case 10112: {
                if (chra.isZeroSecondLook()) {
                    bx = SkillFactory.getSkill(神之子.精准大剑); //101000103 - 精准大剑 - 提高大剑系列武器的熟练度、攻击力和攻击速度。此外，施展大剑系列技能时，受到攻击的敌人数量越少，伤害越强。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        watk += eff.getAttackX();
                        percent_damage_rate += eff.getMobCountDamage();
                        percent_boss_damage_rate += eff.getBossDamage();
                    }
                    bx = SkillFactory.getSkill(神之子.固态身体); //101100102 - 固态身体 - 强化贝塔的身体，可以增加物理防御力、魔法防御力、增加状态异常抗性、增加所有属性抗性，同时会提高稳如泰山的几率。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        wdef += eff.getWdefX();
                        mdef += eff.getMdefX();
                        ASR += eff.getASRRate();
                        TER += eff.getTERRate();
                    }
                } else {
                    bx = SkillFactory.getSkill(神之子.精准太刀); //101000203 - 精准太刀 - 提高太刀系列武器的熟练度和攻击力。使用太刀时增加伤害、攻击速度、移动速度和跳跃力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        watk += eff.getAttackX();
                        jump += eff.getPassiveJump();
                        speed += eff.getPassiveSpeed();
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    }
                    bx = SkillFactory.getSkill(神之子.强化之躯); //101100203 - 强化之躯 - 强化阿尔法的身体，增加最大HP、最大时间之力和暴击率。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_hp += eff.getPercentHP();
                        passive_sharpeye_rate += eff.getCritical(); //爆击概率
                    }
                    bx = SkillFactory.getSkill(神之子.圣光照耀); //101120207 - 圣光照耀 - 增加阿尔法的最大和最小暴击伤害，同时有一定的几率给对象造成出血状态，并恢复自己的HP。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        passive_sharpeye_max_percent += eff.getCriticalMax();
                    }
                }
                bx = SkillFactory.getSkill(神之子.决意时刻); //100000279 - 决意时刻 - 继承并获得伦娜女神的强大力量。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_damage += eff.getDAMRate();
                    localstr += bx.getEffect(bof).getStrX();
                    percent_hp += eff.getPercentHP();
                    speedMax += eff.getSpeedMax();
                }
                break;
            }
            case 11000: //林之灵
            case 11200:
            case 11210:
            case 11211:
            case 11212: {
                bx = SkillFactory.getSkill(林之灵.精灵集中); //110000800 - 精灵集中 - 攻击BOSS怪时,精灵之力会更强。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                    passive_sharpeye_rate += eff.getCritical();
                    percent_boss_damage_rate += eff.getBossDamage();
                }
                bx = SkillFactory.getSkill(林之灵.林之灵之修养); //110000513 - 林之灵之修养 - 林之灵每次获得经验值时,潜力上升,自动得到成长。\r\n从60级开始可以学习。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    localint_ += eff.getIntX();
                    localluk += eff.getLukX();
                    passive_sharpeye_rate += eff.getCritical();
                    percent_hp += eff.getPercentHP();
                    percent_mp += eff.getPercentMP();
                    percent_boss_damage_rate += eff.getBossDamage();
                    percent_damage += eff.getMagicDamage();
                    wdef += eff.getWdefX();
                    mdef += eff.getMdefX();
                    ASR += eff.getASRRate();
                    TER += eff.getTERRate();
                }
                /*
                 * 超级技能处理
                 */
                bx = SkillFactory.getSkill(林之灵.致命三角_额外目标); //112120047 - 致命三角 -额外目标 - 利用致命三角攻击的怪物数量增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(林之灵.致命三角, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(林之灵.致命三角_额外攻击); //112120048 - 致命三角 - 额外攻击 - 致命三角的攻击次数增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addAttackCount(林之灵.致命三角, bx.getEffect(bof).getAttackCount());
                }
                bx = SkillFactory.getSkill(林之灵.编队攻击_额外目标); //112120050 - 编队攻击  - 额外目标 - 利用编队攻击攻击的怪物数量增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(林之灵.编队攻击, bx.getEffect(bof).getTargetPlus());
                }
                bx = SkillFactory.getSkill(林之灵.伙伴发射_额外目标); //112120053 - 伙伴发射 - 额外目标 - 利用伙伴发射攻击的怪物数量增加。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    addTargetPlus(林之灵.伙伴发射, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(林之灵.伙伴发射2, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(林之灵.伙伴发射3, bx.getEffect(bof).getTargetPlus());
                    addTargetPlus(林之灵.伙伴发射4, bx.getEffect(bof).getTargetPlus());
                }
                /*
                 * 模式技能处理
                 */
                int buffSourceId = chra.getBuffSource(MapleBuffStat.守护模式变更);
                if (buffSourceId == 林之灵.巨熊模式) {
                    bx = SkillFactory.getSkill(林之灵.波波之粮食储备); //112000011 - 波波之粮食储备 - 巨熊状态下最大HP和智力提高一定量。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_hp += eff.getPercentHP();
                        localint_ += eff.getIntX();
                    }
                    bx = SkillFactory.getSkill(林之灵.波波之坚韧); //112000012 - 波波之坚韧 - 巨熊状态下，可以无视怪物一定量的防御力，并且攻击速度提高。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    }
                    bx = SkillFactory.getSkill(林之灵.怒之乱击); //112000003 - 怒之乱击 - 连续按下#前爪挥击#技能时，该技能会在第4击时发动。每次按下技能键会给前方的敌人造成连续攻击。\n[发动命令]: #c前爪挥击#3击以后，连续按下按键时
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(林之灵.前爪挥击, eff.getDAMRate());
                        addDamageIncrease(林之灵.前爪挥击2, eff.getDAMRate());
                        addDamageIncrease(林之灵.前爪挥击3, eff.getDAMRate());
                    }
                    bx = SkillFactory.getSkill(林之灵.波波之致命一击); //112000014 - 波波之致命一击 - 巨熊状态下爆击率、最小伤害、最大伤害提高，魔法攻击力永久增加。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        magic += eff.getMagicX();
                        passive_sharpeye_rate += eff.getCritical();
                        passive_sharpeye_max_percent += eff.getCriticalMax();
                        passive_sharpeye_min_percent += eff.getCriticalMin();
                    }
                    bx = SkillFactory.getSkill(林之灵.波波之勇猛); //112000013 - 波波之勇猛 - 巨熊状态下，增加一定%的魔法攻击力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_damage += eff.getMagicDamage();
                    }
                    bx = SkillFactory.getSkill(林之灵.集中打击); //112001009 - 集中打击 - 一定时间内，变成集中攻击‘一个敌人’的状态，伤害大幅提升。此外，巨熊的身体得到强化。最大HP和MP，以及防御力增加。并可以无视怪物一定量的防御力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_hp += eff.getPercentHP();
                        percent_mp += eff.getPercentMP();
                        percent_wdef += eff.getWDEFRate();
                        percent_mdef += eff.getMDEFRate();
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    }
                    bx = SkillFactory.getSkill(林之灵.火焰屁_强化); //112000020 - 火焰屁 强化 - 强化魔法攻击力，将火焰屁的火焰强化成更加强大的地狱火。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        magic += eff.getMagicX();
                        addDamageIncrease(林之灵.火焰屁, eff.getDAMRate());
                    }
                } else if (buffSourceId == 林之灵.雪豹模式) { //雪豹模式
                    bx = SkillFactory.getSkill(林之灵.拉伊之力_强化); //112100013 - 拉伊之力 强化 - 强化雪豹的力量，提高魔法攻击力、智力、移动速度和跳跃力。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        localint_ += eff.getIntX();
                        magic += eff.getMagicX();
                        jump += eff.getPassiveJump();
                        speed += eff.getPassiveSpeed();
                    }
                    bx = SkillFactory.getSkill(林之灵.雪豹咆哮); //112100003 - 雪豹咆哮 - 在#c雪豹强袭#之后，向前方咆哮，造成额外伤害。此外，#c雪豹重斩#和#c雪豹强袭#的伤害及可攻击的怪物数量得到提高。\n[发动命令]：施展#c雪豹强袭#后，再次按下#c[攻击]#键时
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(林之灵.雪豹重斩, eff.getDAMRate());
                        addDamageIncrease(林之灵.雪豹强袭, eff.getDAMRate());
                        addDamageIncrease(林之灵.雪豹_未知, eff.getDAMRate());
                        addTargetPlus(林之灵.雪豹重斩, eff.getTargetPlus());
                        addTargetPlus(林之灵.雪豹强袭, eff.getTargetPlus());
                        addTargetPlus(林之灵.雪豹_未知, eff.getTargetPlus());
                    }
                    bx = SkillFactory.getSkill(林之灵.男子汉姿态); //112100006 - 男子汉姿态 - 在#c男子汉步伐#后，展现雪豹的帅气姿态，并对地面施以巨大冲击，造成额外的范围伤害，受到伤害的敌人陷入眩晕。此外，#c男子汉之舞，男子汉步伐#攻击的怪物数量增加。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        addDamageIncrease(林之灵.男子汉之舞, eff.getDAMRate());
                        addDamageIncrease(林之灵.男子汉步伐, eff.getDAMRate());
                        addTargetPlus(林之灵.男子汉之舞, eff.getTargetPlus());
                        addTargetPlus(林之灵.男子汉步伐, eff.getTargetPlus());
                    }
                    bx = SkillFactory.getSkill(林之灵.拉伊之牙_强化); //112100010 - 拉伊之牙 强化 - 有一定概率秒杀对方。对BOSS怪无效。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        magic += eff.getMagicX();
                    }
                    bx = SkillFactory.getSkill(林之灵.拉伊之爪_强化); //112100014 - 拉伊之爪 强化 - 雪豹状态下使用的特定主动技能的攻击伤害和攻击次数增加。\r\n#c相关技能 : 雪豹重斩，雪豹强袭, 雪豹咆哮, 男子汉之舞, 男子汉步伐, 迅雷冲刺#
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                        addDamageIncrease(林之灵.雪豹重斩, eff.getDAMRate());
                        addDamageIncrease(林之灵.雪豹强袭, eff.getDAMRate());
                        addDamageIncrease(林之灵.雪豹咆哮, eff.getDAMRate());
                        addDamageIncrease(林之灵.雪豹_未知, eff.getDAMRate());
                        addDamageIncrease(林之灵.男子汉之舞, eff.getDAMRate());
                        addDamageIncrease(林之灵.男子汉步伐, eff.getDAMRate());
                        addDamageIncrease(林之灵.迅雷冲刺, eff.getDAMRate());
                        addDamageIncrease(林之灵.进阶迅雷冲刺, eff.getDAMRate());
                        addTargetPlus(林之灵.雪豹重斩, eff.getTargetPlus());
                        addTargetPlus(林之灵.雪豹强袭, eff.getTargetPlus());
                        addTargetPlus(林之灵.雪豹咆哮, eff.getTargetPlus());
                        addTargetPlus(林之灵.雪豹_未知, eff.getTargetPlus());
                        addTargetPlus(林之灵.男子汉之舞, eff.getTargetPlus());
                        addTargetPlus(林之灵.男子汉步伐, eff.getTargetPlus());
                        addTargetPlus(林之灵.迅雷冲刺, eff.getTargetPlus());
                        addTargetPlus(林之灵.进阶迅雷冲刺, eff.getTargetPlus());
                    }
                    bx = SkillFactory.getSkill(林之灵.拉伊之心_强化); //112100015 - 拉伊之心 强化 - 转换成雪豹状态后，攻击速度及魔法攻击力提高。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        eff = bx.getEffect(bof);
                        accuracy += eff.getAccX();
                        percent_damage += eff.getMagicDamage();
                        percent_ignore_mob_def_rate += eff.getIgnoreMob();
                    }
                } else if (buffSourceId == 林之灵.猛鹰模式) { //猛鹰模式

                } else if (buffSourceId == 林之灵.猫咪模式) { //猫咪模式
                    bx = SkillFactory.getSkill(林之灵.阿尔之萌); //112120015 - 阿尔之萌 - 猫咪状态下,智力永久增加。
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        localint_ += bx.getEffect(bof).getIntX();
                    }
                }
                break;
            }
            case 14200:
            case 14210:
            case 14211:
            case 14212: {
                bx = SkillFactory.getSkill(超能力者.内在1); //112100015 - 拉伊之心 强化 - 转换成雪豹状态后，攻击速度及魔法攻击力提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(超能力者.内在2); //112100015 - 拉伊之心 强化 - 转换成雪豹状态后，攻击速度及魔法攻击力提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    percent_hp += eff.getPercentHP();
                }
                bx = SkillFactory.getSkill(超能力者.精神集中_维持); //112100015 - 拉伊之心 强化 - 转换成雪豹状态后，攻击速度及魔法攻击力提高。
                bof = chra.getTotalSkillLevel(bx);
                if (bof > 0 && bx != null) {
                    eff = bx.getEffect(bof);
                    BuffUP_Skill += eff.getBuffTimeRate();
                }
                break;
            }
        }
        bx = SkillFactory.getSkill(初心者.海盗祝福);
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            localstr += eff.getStrX();
            localdex += eff.getDexX();
            localint_ += eff.getIntX();
            localluk += eff.getLukX();
            percent_hp += eff.getHpR();
            percent_mp += eff.getMpR();
        }
        bx = SkillFactory.getSkill(初心者.恶魔之怒);
        bof = chra.getSkillLevel(bx);
        if (bof > 0 && bx != null) {
            eff = bx.getEffect(bof);
            percent_boss_damage_rate += eff.getBossDamage();
        }
        if (JobConstants.is反抗者(chra.getJob())) {
            bx = SkillFactory.getSkill(预备兵.效率提升); //效率提升 - 为了解决物资缺乏问题，学习高效使用药水的方法。
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                RecoveryUP += bx.getEffect(bof).getX() - 100;
            }
        }
        /*
         * 超级技能加属性
         */
        bx = SkillFactory.getSkill(80000400); //STR - 提高力量（STR）。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            localstr += bx.getEffect(bof).getStrX();
        }
        bx = SkillFactory.getSkill(80000401); //DEX - 提高敏捷（DEX）。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            localdex += bx.getEffect(bof).getDexX();
        }
        bx = SkillFactory.getSkill(80000402); //INT - 提高智力（INT）。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            localint_ += bx.getEffect(bof).getIntX();
        }
        bx = SkillFactory.getSkill(80000403); //LUK - 提高运气（LUK）。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            localluk += bx.getEffect(bof).getLukX();
        }
        bx = SkillFactory.getSkill(80000404); //HP - 提高最大HP。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            percent_hp += bx.getEffect(bof).getPercentHP();
        }
        bx = SkillFactory.getSkill(80000405); //MP - 提高最大MP。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            if (!JobConstants.is恶魔猎手(chra.getJob())) {
                percent_mp += bx.getEffect(bof).getPercentMP();
            }
        }
        bx = SkillFactory.getSkill(80000406); //DF - 提高最大恶魔精气。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            incMaxDF += bx.getEffect(bof).getIndieMaxDF();
        }
        bx = SkillFactory.getSkill(80000407); //移动速度 - 提高移动速度。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            speed += bx.getEffect(bof).getPassiveSpeed();
        }
        bx = SkillFactory.getSkill(80000408); //跳跃力 - 提高跳跃力。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            jump += bx.getEffect(bof).getPassiveJump();
        }
        bx = SkillFactory.getSkill(80000409); //爆击发动 - 提高爆击率。
        bof = chra.getTotalSkillLevel(bx);
        if (bof > 0 && bx != null && bx.isHyperStat()) {
            passive_sharpeye_rate += bx.getEffect(bof).getCritical();
        }
//        bx = SkillFactory.getSkill(80000410); //爆击最小 - 提高爆击最小伤害。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000411); //爆击最大 - 提高爆击最大伤害。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000412); //无视防御力 - 提高无视怪物防御力比例。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000413); //伤害 - 提高伤害。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000414); //攻击BOSS怪物时的伤害增加 - 提高攻击BOSS怪物时的伤害。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000415); //所有属性抗性 - 对怪物的所有属性攻击拥有抗性。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000416); //状态异常抗性 - 对怪物的所有状态异常攻击拥有抗性。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
//        bx = SkillFactory.getSkill(80000417); //稳如泰山 - 提高稳如泰山概率。
//        bof = chra.getTotalSkillLevel(bx);
//        if (bof > 0 && bx != null && bx.isHyperSkill()) {
//            //
//        }
    }

    private void handleBuffStats(MapleCharacter chra) {
        MapleStatEffect effect = chra.getStatForBuff(MapleBuffStat.骑兽技能);
        if (effect != null && effect.getSourceid() == 豹弩游侠.美洲豹骑士) {
            passive_sharpeye_rate += effect.getW();
            percent_hp += effect.getZ();
        }
        Integer buff = chra.getBuffedValue(MapleBuffStat.物理攻击);
        if (buff != null) {
            watk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.物理防御);
        if (buff != null) {
            wdef += buff;
        }
//        buff = chra.getBuffedValue(MapleBuffStat.魔法防御);
        if (buff != null) {
            mdef += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.魔法攻击);
        if (buff != null) {
            magic += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.命中率);
        if (buff != null) {
            accuracy += buff;
        }
        //to 回避率 手技
        buff = chra.getBuffedSkill_Y(MapleBuffStat.隐身术);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.移动速度);
        if (buff != null) {
            speed += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.跳跃力);
        if (buff != null) {
            jump += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MAXHP);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.MAXMP);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.斗气集中);
        if (buff != null) {
            Skill combos = SkillFactory.getSkill(英雄.斗气协合);
            int comboslevel = chra.getTotalSkillLevel(combos);
            if (comboslevel > 0) {
                effect = combos.getEffect(comboslevel);
                percent_damage_rate += buff * effect.getX();
                percent_boss_damage_rate += buff * effect.getX();
            }
        }
        effect = chra.getStatForBuff(MapleBuffStat.召唤兽);
        if (effect != null) {
            if (effect.getSourceid() == 机械师.支援波动器_H_EX) {
                percent_damage_rate += effect.getX();
                percent_boss_damage_rate += effect.getX();
            }
        }
        effect = chra.getStatForBuff(MapleBuffStat.属性攻击);
        if (effect != null) {
            percent_damage_rate += effect.getDamage();
            percent_boss_damage_rate += effect.getDamage();
        }
        effect = chra.getStatForBuff(MapleBuffStat.敛财术);
        if (effect != null) {
            pickRate = effect.getProp();
        }
        buff = chra.getBuffedValue(MapleBuffStat.金钱护盾);
        if (buff != null) {
            mesoGuardMeso += buff.doubleValue();
        }
        //--------------------------------2--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.冒险岛勇士);
        if (buff != null) {
            double d = buff.doubleValue() / 100.0;
            localstr += d * str;
            localdex += d * dex;
            localluk += d * luk;
            localint_ += d * int_;
        }
        effect = chra.getStatForBuff(MapleBuffStat.火眼晶晶);
        if (effect != null) {
            passive_sharpeye_rate += effect.getX();
            passive_sharpeye_max_percent += effect.getCriticalMax();
        }
        buff = chra.getBuffedValue(MapleBuffStat.终极无限);
        if (buff != null) {
            percent_matk += buff - 1;
        }
//        buff = chra.getBuffedSkill_X(MapleBuffStat.集中精力);
//        if (buff != null) {
//            mpconReduce += buff.intValue();
//        }
        buff = chra.getBuffedValue(MapleBuffStat.英雄回声);
        if (buff != null) {
            double d = buff.doubleValue() / 100.0;
            watk += (int) (watk * d);
            magic += (int) (magic * d);
        }
        buff = chra.getBuffedValue(MapleBuffStat.MESO_RATE);
        if (buff != null) {
            mesoBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DROP_RATE);
        if (buff != null) {
            dropBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.EXPRATE);
        if (buff != null) {
            expBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ACASH_RATE);
        if (buff != null) {
            cashBuff *= buff.doubleValue() / 100.0;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.狂暴战魂);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        //--------------------------------3--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.矛连击强化);
        if (buff != null) {
            int level = Math.max(1, chra.getAranCombo() / 10);
            if (chra.getTotalSkillLevel(战神.进阶矛连击强化) > 0) {
                effect = SkillFactory.getSkill(战神.进阶矛连击强化).getEffect(chra.getTotalSkillLevel(战神.进阶矛连击强化));
                int num = Math.min(effect.getX(), level);
                watk += num * effect.getW();
                ASR += num * effect.getW();
                passive_sharpeye_rate += num * effect.getY();
            } else if (chra.getTotalSkillLevel(战神.矛连击强化) > 0) {
                effect = SkillFactory.getSkill(战神.矛连击强化).getEffect(chra.getTotalSkillLevel(战神.矛连击强化));
                int num = Math.min(effect.getX(), level);
                watk += num * effect.getY();
            }
        }
        effect = chra.getStatForBuff(MapleBuffStat.抗魔领域);
        if (effect != null) {
            ASR += effect.getX();
        }
        effect = chra.getStatForBuff(MapleBuffStat.雷鸣冲击);
        if (effect != null) {
            percent_damage_rate += effect.getDamage();
            percent_boss_damage_rate += effect.getDamage();
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.葵花宝典);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedSkill_Y(MapleBuffStat.终极斩);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.DAMAGE_BUFF); //提高队员攻击力 按百分百算 攻击力增加X%
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.ATTACK_BUFF); //提高自身攻击力 按百分百算 攻击力增加X%
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增强_物理攻击);
        if (buff != null) {
            watk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增强_魔法攻击);
        if (buff != null) {
            magic += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增强_物理防御);
        if (buff != null) {
            wdef += buff;
        }
//        buff = chra.getBuffedValue(MapleBuffStat.增强_魔法防御);
//        if (buff != null) {
//            mdef += buff;
//        }
        //--------------------------------4--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.呼啸_爆击概率);
        if (buff != null) {
            passive_sharpeye_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.呼啸_MaxMp增加);
        if (buff != null) {
            percent_mp += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.金属机甲);
        if (effect != null) {
            passive_sharpeye_rate += effect.getCritical();
        }
//        effect = chra.getStatForBuff(MapleBuffStat.黑暗灵气);
//        if (effect != null) {
//            percent_damage_rate += effect.getX();
//            percent_boss_damage_rate += effect.getX();
//        }
//        effect = chra.getStatForBuff(MapleBuffStat.蓝色灵气);
//        if (effect != null) {
//            percent_wdef += effect.getZ() + effect.getY();
//            percent_mdef += effect.getZ() + effect.getY();
//        }
        buff = chra.getBuffedValue(MapleBuffStat.ATTACK_BUFF);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.幸运钱);
        if (buff != null) {
            incMaxDamage += buff * 10000000;
        }
        buff = chra.getBuffedValue(MapleBuffStat.幸运骰子);
        if (buff != null) {
            percent_wdef += GameConstants.getDiceStat(buff, 2);
            percent_mdef += GameConstants.getDiceStat(buff, 2);
            percent_hp += GameConstants.getDiceStat(buff, 3);
            percent_mp += GameConstants.getDiceStat(buff, 3);
            passive_sharpeye_rate += GameConstants.getDiceStat(buff, 4);
            percent_damage_rate += GameConstants.getDiceStat(buff, 5);
            percent_boss_damage_rate += GameConstants.getDiceStat(buff, 5);
            expBuff *= (GameConstants.getDiceStat(buff, 6) + 100.0) / 100.0;
        }
        effect = chra.getStatForBuff(MapleBuffStat.祝福护甲);
        if (effect != null) {
            watk += effect.getEnhancedWatk();
        }
        effect = chra.getStatForBuff(MapleBuffStat.反制攻击);
        if (effect != null) {
            switch (effect.getSourceid()) {
                case 冲锋队长.反制攻击:
                case 船长.反制攻击:
                    percent_damage_rate += effect.getIndieDamR();
                    percent_boss_damage_rate += effect.getIndieDamR();
                    break;
                case 冲锋队长.蛇拳:
                    percent_damage_rate += effect.getX();
                    percent_boss_damage_rate += effect.getX();
                    break;
                case 恶魔猎手.黑暗变形:
                    percent_damage += effect.getDAMRate();
                    break;
                default:
                    percent_damage_rate += effect.getDAMRate();
                    percent_boss_damage_rate += effect.getDAMRate();
                    break;
            }
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.战斗命令);
        if (buff != null) {
            combatOrders += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.灵魂助力);
        if (effect != null) {
            trueMastery += effect.getMastery();
        }
        effect = chra.getStatForBuff(MapleBuffStat.牧师祝福);
        if (effect != null) {
            watk += effect.getX();
            magic += effect.getY();
            accuracy += effect.getV();
        }
        //--------------------------------5--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.增加物理攻击力);
        if (buff != null) {
            watk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加魔法攻击力);
        if (buff != null) {
            magic += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加命中值);
        if (buff != null) {
            accuracy += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加所有属性);
        if (buff != null) {
            localstr += buff;
            localdex += buff;
            localint_ += buff;
            localluk += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVP_DAMAGE);
        if (buff != null) {
            pvpDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.PVP_ATTACK);
        if (buff != null) {
            pvpDamage += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.潜力解放);
        if (effect != null) {
            passive_sharpeye_rate = 100;
            ASR = 100;
            wdef += effect.getX();
            mdef += effect.getX();
            watk += effect.getX();
            magic += effect.getX();
        }
        //--------------------------------6--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.伤害吸收);
        if (buff != null) {
            reduceDamageRate += buff;
        }
        buff = chra.getBuffedSkill_X(MapleBuffStat.属性抗性);
        if (buff != null) {
            TER += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.提升物理防御力);
        if (buff != null) {
            wdef += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.精神连接);
        if (effect != null) {
            if (effect.is船员统帅()) {
                watk += effect.getWatk();
                passive_sharpeye_rate += effect.getCritical();
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.爆击提升);
        if (buff != null) {
            passive_sharpeye_rate += buff;
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        //--------------------------------7--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.增加最大HP百分比);
        if (buff != null) {
            percent_hp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加最大MP百分比);
        if (buff != null) {
            percent_mp += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.百分比无视防御);
        if (buff != null) {
            percent_ignore_mob_def_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.爆击概率增加);
        if (buff != null) {
            passive_sharpeye_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.最小爆击伤害);
        if (buff != null) {
            passive_sharpeye_max_percent += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.卡牌审判);
        buff = chra.getBuffedValue(MapleBuffStat.卡牌审判);
        if (effect != null && buff != null) {
            switch (buff) {
                case 1:
                    passive_sharpeye_rate += effect.getV();
                    break;
                case 2:
                    dropBuff *= (effect.getW() + 100.0) / 100.0;
                    break;
                case 3:
                    ASR += effect.getX();
                    TER += effect.getY();
                    break;
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.提升伤害百分比);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        //--------------------------------8--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.黑暗高潮);
        if (buff != null) {
            percent_damage += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.生命潮汐);
        if (effect != null) {
            passive_sharpeye_rate += effect.getProp();
        }
        Skill bx;
        int bof;
        MapleStatEffect skilleff;
        effect = chra.getStatForBuff(MapleBuffStat.黑暗祝福);
        if (effect != null) {
            buff = chra.getBuffedValue(MapleBuffStat.黑暗祝福);
            bx = SkillFactory.getSkill(夜光.黑暗祝福);
            bof = chra.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                skilleff = bx.getEffect(bof);
                if (buff == 1) {
                    magic += skilleff.getU();
                } else if (buff == 2) {
                    magic += skilleff.getV();
                } else if (buff == 3) {
                    magic += skilleff.getY();
                }
            }
        }
        effect = chra.getStatForBuff(MapleBuffStat.模式转换);
        if (effect != null) {
            if (effect.getSourceid() == 狂龙战士.防御模式) {
                percent_wdef += effect.getWdefX();
                percent_mdef += effect.getMdefX();
                accuracy += effect.getAccX();
                percent_hp += effect.getPercentHP();
                int[] skills = {狂龙战士.防御模式1次强化, 狂龙战士.防御模式2次强化, 狂龙战士.防御模式3次强化};
                for (int i : skills) {
                    bx = SkillFactory.getSkill(i);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        skilleff = bx.getEffect(bof);
                        percent_wdef += skilleff.getWdefX();
                        percent_mdef += skilleff.getMdefX();
                        accuracy += skilleff.getAccX();
                        percent_hp += skilleff.getPercentHP();
                    }
                }
            } else if (effect.getSourceid() == 狂龙战士.攻击模式) {
                watk += effect.getAttackX();
                percent_boss_damage_rate += effect.getBossDamage();
                passive_sharpeye_rate += effect.getCritical();
                int[] skills = {狂龙战士.攻击模式1次强化, 狂龙战士.攻击模式2次强化, 狂龙战士.攻击模式3次强化};
                for (int i : skills) {
                    bx = SkillFactory.getSkill(i);
                    bof = chra.getTotalSkillLevel(bx);
                    if (bof > 0 && bx != null) {
                        skilleff = bx.getEffect(bof);
                        watk += skilleff.getAttackX();
                        percent_boss_damage_rate += skilleff.getBossDamage();
                        passive_sharpeye_rate += skilleff.getCritical();
                    }
                }
            }
        }
        //tod 强健护甲
        buff = chra.getBuffedValue(MapleBuffStat.灵魂凝视);
        if (buff != null) {
            percent_damage_rate += buff;
            percent_boss_damage_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.增加伤害最大值);
        if (buff != null) {
            incMaxDamage += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.提升伤害百分比);
        if (buff != null) {
            percent_damage += buff;
        }
        //--------------------------------9--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.暴击概率);
        if (buff != null) {
            passive_sharpeye_rate += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.恶魔超越);
        if (buff != null) {
            hpRecover_Percent -= buff / 2;
            if (hpRecover_Percent < 0) {
                hpRecover_Percent = 0;
            }
        }
        //--------------------------------10--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.交叉锁链);
        if (buff != null) {
            percent_damage += buff;
        }
        effect = chra.getStatForBuff(MapleBuffStat.元素爆破);
        if (effect != null && effect.getSourceid() == 圣骑士.连环环破) {
            percent_damage += effect.getDAMRate();
            passive_sharpeye_rate += effect.getCritical();
            percent_ignore_mob_def_rate += effect.getIgnoreMob();
        }
        //--------------------------------11--------------------------------------
        effect = chra.getStatForBuff(MapleBuffStat.极限射箭);
        if (effect != null) {
            switch (effect.getSourceid()) {
                case 神射手.极限射箭:
                    watk += effect.getAttackX();
                    break;
                case 箭神.极限射箭:
                    //爆击最大伤害
                    break;
            }
        }
        //--------------------------------12--------------------------------------
        buff = chra.getBuffedValue(MapleBuffStat.疾驰速度);
        if (buff != null) {
            speed += buff;
        }
        buff = chra.getBuffedValue(MapleBuffStat.疾驰跳跃);
        if (buff != null) {
            jump += buff;
        }
        //--------------------------------------------------------------------
        if (speed > 140) {
            speed = 140;
        }
        if (jump > 123) {
            jump = 123;
        }
        buff = chra.getBuffedValue(MapleBuffStat.骑兽技能);
        if (buff != null) {
            jump = 120;
            switch (buff) {
                case 1:
                    speed = 150;
                    break;
                case 2:
                    speed = 170;
                    break;
                case 3:
                    speed = 180;
                    break;
                default:
                    speed = 200;
                    break;
            }
        }
        buff = chra.getBuffedValue(MapleBuffStat.超压魔力);
        if (buff != null) {
            mpconMaxPercent += buff;
        }
    }

    public boolean checkEquipLevels(MapleCharacter chr, long gain) {
        boolean changed = false;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        List<Equip> all = new ArrayList<>(equipLevelHandling);
        for (Equip eq : all) {
            int lvlz = eq.getEquipLevel();
            eq.setItemEXP(Math.min(eq.getItemEXP() + gain, Long.MAX_VALUE));

            if (eq.getEquipLevel() > lvlz) { //lvlup
                for (int i = eq.getEquipLevel() - lvlz; i > 0; i--) {
                    //升级装备属性
                    final Map<String, Map<String, Integer>> inc = ii.getEquipIncrements(eq.getItemId());
                    int extra = eq.getYggdrasilWisdom();
                    if (extra == 1) {
                        inc.get(String.valueOf(lvlz + i - 1)).put("incSTRMin", 1);
                        inc.get(String.valueOf(lvlz + i - 1)).put("incSTRMax", 3);
                    } else if (extra == 2) {
                        inc.get(String.valueOf(lvlz + i - 1)).put("incDEXMin", 1);
                        inc.get(String.valueOf(lvlz + i - 1)).put("incDEXMax", 3);
                    } else if (extra == 3) {
                        inc.get(String.valueOf(lvlz + i - 1)).put("incINTMin", 1);
                        inc.get(String.valueOf(lvlz + i - 1)).put("incINTMax", 3);
                    } else if (extra == 4) {
                        inc.get(String.valueOf(lvlz + i - 1)).put("incLUKMin", 1);
                        inc.get(String.valueOf(lvlz + i - 1)).put("incLUKMax", 3);
                    }
                    if (inc != null && inc.containsKey(String.valueOf(lvlz + i - 1))) {
                        eq = ii.levelUpEquip(eq, inc.get(String.valueOf(lvlz + i - 1)));
                    }
                    //检测装备是否可以获得技能
                    if (GameConstants.getStatFromWeapon(eq.getItemId()) == null && GameConstants.getMaxLevel(eq.getItemId()) < (lvlz + i) && Math.random() < 0.1 && eq.getIncSkill() <= 0 && ii.getEquipSkills(eq.getItemId()) != null) {
                        for (int zzz : ii.getEquipSkills(eq.getItemId())) {
                            final Skill skil = SkillFactory.getSkill(zzz);
                            if (skil != null && skil.canBeLearnedBy(chr.getJob())) { //dont go over masterlevel :D
                                eq.setIncSkill(skil.getId());
                                chr.dropMessage(5, "武器：" + skil.getName() + " 已获得新的等级提升！");
                            }
                        }
                    }
                }
                changed = true;
            }
            chr.forceUpdateItem(eq.copy());
        }
        if (changed) {
            chr.equipChanged();
            chr.send(EffectPacket.showItemLevelupEffect());
            chr.getMap().broadcastMessage(chr, EffectPacket.showForeignItemLevelupEffect(chr.getId()), false);
        }
        return changed;
    }

    public boolean checkEquipDurabilitys(MapleCharacter chr, int gain) {
        return checkEquipDurabilitys(chr, gain, false);
    }

    public boolean checkEquipDurabilitys(MapleCharacter chr, int gain, boolean aboveZero) {
        if (chr.inPVP()) {
            return true;
        }
        List<Equip> all = new ArrayList<>(durabilityHandling);
        for (Equip item : all) {
            if (item != null && ((item.getPosition() >= 0) == aboveZero)) {
                item.setDurability(item.getDurability() + gain);
                if (item.getDurability() < 0) { //shouldnt be less than 0
                    item.setDurability(0);
                }
            }
        }
        for (Equip eqq : all) {
            if (eqq != null && eqq.getDurability() == 0 && eqq.getPosition() < 0) { //> 0 went to negative
                if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                    chr.send(InventoryPacket.getInventoryFull());
                    chr.send(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                durabilityHandling.remove(eqq);
                short pos = chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                MapleInventoryManipulator.unequip(chr.getClient(), eqq.getPosition(), pos);
            } else if (eqq != null) {
                chr.forceUpdateItem(eqq.copy());
            }
        }
        return true;
    }

    public void checkEquipSealed(MapleCharacter chr, long gain) {
        List<Equip> all = new ArrayList<>(sealedEquipHandling);
        List<ModifyInventory> mods = new ArrayList<>();
        for (Equip eqq : all) {
            if (eqq != null && !GameConstants.canSealedLevelUp(eqq.getItemId(), eqq.getSealedLevel(), eqq.getSealedExp())) {
                eqq.gainSealedExp(gain);
                mods.add(new ModifyInventory(4, eqq)); //删除装备
            }
        }
        chr.send(InventoryPacket.modifyInventory(true, mods, chr));
    }

    public void handleProfessionTool(MapleCharacter chra) {
        if (chra.getProfessionLevel(92000000) > 0 || chra.getProfessionLevel(92010000) > 0) {
            for (Item item : chra.getInventory(MapleInventoryType.EQUIP).newList()) { //goes to first harvesting tool and stops
                Equip equip = (Equip) item;
                if (equip.getDurability() != 0 && (equip.getItemId() / 10000 == 150 && chra.getProfessionLevel(92000000) > 0) || (equip.getItemId() / 10000 == 151 && chra.getProfessionLevel(92010000) > 0)) {
                    if (equip.getDurability() > 0) {
                        durabilityHandling.add(equip);
                    }
                    harvestingTool = equip.getPosition();
                    break;
                }
            }
        }
    }

    private void CalcPassive_Mastery(MapleCharacter player) {
        if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11) == null) {
            passive_mastery = 0;
            return;
        }
        int skil;
        MapleWeaponType weaponType = ItemConstants.getWeaponType(player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11).getItemId());
        boolean acc = true;
        //System.out.println("武器类型: " + weaponType);
        switch (weaponType) {
            case 弓:
                skil = JobConstants.is风灵使者(player.getJob()) ? 风灵使者.精准弓 : 神射手.精准弓;
                break;
            case 拳套:
                skil = player.getJob() >= 410 && player.getJob() <= 412 ? 隐士.精准暗器 : 夜行者.投掷精通;
                break;
            case 手杖:
                skil = player.getTotalSkillLevel(幻影.手杖专家) > 0 ? 幻影.手杖专家 : 幻影.精准手杖;
                break;
            case 手持火炮:
                skil = 神炮王.精准炮;
                break;
            case 双刀副手:
            case 短刀:
                skil = player.getJob() >= 430 && player.getJob() <= 434 ? 双刀.精准双刀 : 侠盗.精准短刀;
                break;
            case 弩:
                skil = JobConstants.is反抗者(player.getJob()) ? 豹弩游侠.精准弩 : 箭神.精准弩;
                break;
            case 单手斧:
            case 单手钝器:
                skil = JobConstants.is恶魔猎手(player.getJob()) ? 恶魔猎手.精准武器 : (JobConstants.is魂骑士(player.getJob()) ? 魂骑士.精准剑_新 : (JobConstants.is米哈尔(player.getJob()) ? 米哈尔.精准剑 : (player.getJob() >= 110 && player.getJob() <= 112 ? 英雄.精准武器 : 圣骑士.精准武器)));
                break;
            case 双手斧:
            case 单手剑:
            case 双手剑:
            case 双手钝器:
                skil = JobConstants.is狂龙战士(player.getJob()) ? 狂龙战士.精准剑 : JobConstants.is魂骑士(player.getJob()) ? 魂骑士.精准剑_新 : (JobConstants.is米哈尔(player.getJob()) ? 米哈尔.精准剑 : (player.getJob() >= 110 && player.getJob() <= 112 ? 英雄.精准武器 : 圣骑士.精准武器));
                break;
            case 矛:
                skil = JobConstants.is战神(player.getJob()) ? 战神.精准矛 : 黑骑士.精准武器;
                break;
            case 枪:
                skil = 黑骑士.精准武器;
                break;
            case 指节:
                skil = JobConstants.is奇袭者(player.getJob()) ? 奇袭者.精准拳_新 : JobConstants.is隐月(player.getJob()) ? 隐月.拳甲修炼 : 冲锋队长.精准拳;
                break;
            case 短枪:
                skil = JobConstants.is反抗者(player.getJob()) ? 机械师.机械精通 : 船长.精准枪;
                break;
            case 双弩枪:
                skil = 双弩.精准双弩枪;
                break;
            case 短杖:
            case 长杖:
                acc = false;
                skil = JobConstants.is反抗者(player.getJob()) ? 唤灵斗师.精准长杖 : (player.getJob() <= 212 ? 火毒.咒语精通 : (player.getJob() <= 222 ? 冰雷.咒语精通 : (player.getJob() <= 232 ? 主教.咒语精通 : (player.getJob() <= 2000 ? 炎术士.咒语修炼 : 龙神.咒语精通))));
                break;
            case 双头杖:
                acc = false;
                skil = 夜光.咒语精通;
                break;
            case 灵魂手铳:
                skil = 爆莉萌天使.精准灵魂手铳;
                break;
            case 亡命剑:
                skil = 恶魔复仇者.亡命剑精通;
                break;
            case 能量剑:
                skil = 尖兵.尖兵精通;
                break;
            case 大剑:
                skil = 神之子.精准大剑;
                break;
            case 太刀:
                skil = 神之子.精准太刀;
                break;
            case 驯兽魔法棒:
                acc = false;
                skil = 林之灵.驯兽魔法棒练习;
                break;
            default:
                passive_mastery = 0;
                return;
        }
        if (player.getSkillLevel(skil) <= 0) {
            passive_mastery = 0;
            return;
        }
        // TODO: add job id check above skill, etc
        MapleStatEffect eff = SkillFactory.getSkill(skil).getEffect(player.getTotalSkillLevel(skil));
        if (acc) {
            accuracy += eff.getX();
        } else {
            magic += eff.getX();
        }
        passive_sharpeye_rate += eff.getCritical();
        passive_mastery = (byte) eff.getMastery();
        trueMastery += eff.getMastery() + weaponType.getBaseMastery();
        if (player.getJob() == 132) { //1320018 - 进阶精准武器 - 将枪、矛系列的武器熟练度提高至极限，增加暴击最小伤害和物理攻击力。\n前置技能 : #c精准武器10级以上#
            Skill bx = SkillFactory.getSkill(黑骑士.进阶精准武器);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                watk += eff2.getAttackX(); //增加物理攻击
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
                passive_sharpeye_min_percent += eff.getCriticalMin();
            }
        } else if (player.getJob() == 231 || player.getJob() == 232) { //2310008 - 神圣集中 - 通过和圣灵的感应，永久提升所有攻击魔法和治疗技能的爆击率、命中率、魔法熟练度。\n前置技能：#c咒语精通10级以上#
            Skill bx = SkillFactory.getSkill(主教.神圣集中);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
                passive_sharpeye_rate += eff2.getCritical(); //爆击概率
                percent_acc += eff2.getArRate(); //命中概率
            }
        } else if (player.getJob() == 312) { //3120005 - 神箭手 - 提升弓系列武器的熟练度和物理攻击力、爆击最小伤害。\n前置技能：#c精准弓10级以上#
            Skill bx = SkillFactory.getSkill(神射手.神箭手);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                watk += eff2.getX();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
                passive_sharpeye_min_percent += eff.getCriticalMin();
            }
        } else if (player.getJob() == 322) { //3220004 - 神弩手 - 提升弩系列武器的熟练度和物理攻击力、爆击最小伤害。\n前置技能：#c精准弩10级以上#
            Skill bx = SkillFactory.getSkill(箭神.神弩手);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                watk += eff2.getX();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
                passive_sharpeye_min_percent += eff.getCriticalMin();
            }
        } else if (player.getJob() == 412) { //4120012 - 暗器专家 - 增加拳套系列武器的熟练度、物理攻击力、命中值、回避值。
            Skill bx = SkillFactory.getSkill(隐士.暗器专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                accuracy += eff2.getPercentAcc();
                dodgeChance += eff2.getPercentAvoid();
                watk += eff2.getX();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
            }
        } else if (player.getJob() == 422) { //4220012 - 短刀专家 - 增加短剑系列武器的熟练度、物理攻击力、命中值和回避值。
            Skill bx = SkillFactory.getSkill(侠盗.短刀专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                accuracy += eff2.getPercentAcc();
                dodgeChance += eff2.getPercentAvoid();
                watk += eff2.getX();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
            }
        } else if (player.getJob() == 434) { //4340013 - 双刀专家 - 增加短剑和双刀系列武器的熟练度和物理攻击力, 命中值, 回避值。
            Skill bx = SkillFactory.getSkill(双刀.双刀专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                accuracy += eff2.getPercentAcc();
                dodgeChance += eff2.getPercentAvoid();
                watk += eff2.getX();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
            }
        } else if (player.getJob() == 512) { //5121015 - 蛇拳 - 在一定时间内召唤出遗忘的毒蛇之魂，增加攻击力。被动效果是提高状态异常和所有属性抗性，提高拳甲熟练度。
            Skill bx = SkillFactory.getSkill(冲锋队长.蛇拳);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
            }
        } else if (player.getJob() == 522) { //5220020 - 船长的威严 - 凭借船长的威严，提高枪械类武器的熟练度，使用命中目标时会引起爆炸的子弹，造成额外伤害。
            Skill bx = SkillFactory.getSkill(船长.船长的威严);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
            }
        } else if (player.getJob() == 1112) { //11120007 - 长剑专家 - 提升单手剑和双手剑的熟练度、物理攻击力和爆击最小伤害。\n需要技能：#c精准剑10级以上#
            Skill bx = SkillFactory.getSkill(魂骑士.长剑专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 1212) { //12120009 - 魔法真理 - 领悟魔法真理，释放出体内隐藏的力量。\n需要技能：#c咒语修炼 10级#
            Skill bx = SkillFactory.getSkill(炎术士.魔法真理);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                magic += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 1312) { //13120006 - 神箭手 - 提升弓系列武器的熟练度、物理攻击力和爆击最小伤害。\n前置技能：#c精准弓10级以上#
            Skill bx = SkillFactory.getSkill(风灵使者.神箭手);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 1412) { //14120005 - 投掷专家 - 提高拳套系列武器的熟练度、物理攻击力和最大暴击伤害。\n需要技能：#c投掷精通10级以上#
            Skill bx = SkillFactory.getSkill(夜行者.投掷专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 1512) { //15120006 - 拳甲专家 - 提升拳甲系列武器的熟练度以及物理攻击力和最小爆击伤害。\n前置技能：#c精准拳甲10级以上#
            Skill bx = SkillFactory.getSkill(奇袭者.拳甲专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 2112) { //21120001 - 攻击策略 - 将矛系列武器的熟练度提升至极限。提升爆击最小伤害和物理攻击力。\n前置技能：#c精准矛10级以上#
            Skill bx = SkillFactory.getSkill(战神.攻击策略);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 2512) { //25120113 - 高级拳甲修炼 - 利用精灵的力量，将拳甲系列武器的熟练度提高到极限，增加暴击最小伤害和最大伤害。\n[需要技能]：#c拳甲修炼10级以上#
            Skill bx = SkillFactory.getSkill(隐月.高级拳甲修炼);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 2712) { //27120007 - 魔法精通 - 增加魔法熟练度及魔力。
            Skill bx = SkillFactory.getSkill(夜光.魔法精通);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); // 减去老的
                trueMastery += eff2.getMastery(); // 添加新的
                magic += eff2.getX(); //魔法攻击力
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 3122) { //31220006 - 进阶亡命剑精通 - 永久提高亡命剑的熟练度、攻击力和爆击最小伤害。\r\n需要技能：#c亡命剑精通10级以上#
            Skill bx = SkillFactory.getSkill(恶魔复仇者.进阶亡命剑精通);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getAttackX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 3612) { //36120006 - 尖兵专家 - 提高熟练度，增加攻击力和爆击最小伤害。\n[需要技能]：#c尖兵精通10级以上#
            Skill bx = SkillFactory.getSkill(尖兵.尖兵专家);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getAttackX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 5112) { //51120001 - 进阶精准剑 - 单手剑系列武器熟练度提高到极限，增加最低爆击伤害和物理攻击力。
            Skill bx = SkillFactory.getSkill(米哈尔.进阶精准剑);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                watk += eff2.getAttackX();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
            }
        } else if (player.getJob() == 6112) { //61120012 - 进阶精准剑 - 将双手剑的熟练度提升至极限。提升爆击最小伤害和物理攻击力。\n前置技能：#c精准剑10级以上#
            Skill bx = SkillFactory.getSkill(狂龙战士.进阶精准剑);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getAttackX();
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        } else if (player.getJob() == 6512) { //65120005 - 灵魂手铳手 - 提升灵魂手铳的熟练度及攻击力。\n前置技能：#c精准灵魂手铳10级以上#
            Skill bx = SkillFactory.getSkill(爆莉萌天使.灵魂手铳手);
            int bof = player.getTotalSkillLevel(bx);
            if (bof > 0 && bx != null) {
                MapleStatEffect eff2 = bx.getEffect(bof);
                passive_mastery = (byte) eff2.getMastery();
                trueMastery -= eff.getMastery(); //减去老的
                trueMastery += eff2.getMastery(); //添加新的
                watk += eff2.getAttackX();
                passive_sharpeye_rate += eff2.getCritical(); //爆击概率
                passive_sharpeye_min_percent += eff2.getCriticalMin(); //爆击最小伤害
            }
        }
    }

    private void calculateFame(MapleCharacter player) {
        player.getTrait(MapleTraitType.charm).addLocalExp(player.getFame());
        for (MapleTraitType t : MapleTraitType.values()) {
            player.getTrait(t).recalcLevel();
        }
    }

    private void CalcPassive_SharpEye(MapleCharacter player) {
        Skill critSkill;
        int critlevel;
        if (JobConstants.is反抗者(player.getJob())) {
            critSkill = SkillFactory.getSkill(30000022); //30000022 - 爆击 - [最高等级：20]\n可以增加箭的攻击力。
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                passive_sharpeye_rate += critSkill.getEffect(critlevel).getProp();
                this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
            }
            critSkill = SkillFactory.getSkill(30010022); //30010022 - 爆击 - [最高等级：20]\n可以增加箭的攻击力。
            critlevel = player.getTotalSkillLevel(critSkill);
            if (critlevel > 0) {
                passive_sharpeye_rate += critSkill.getEffect(critlevel).getProp();
                this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
            }
        }
        switch (player.getJob()) { // Apply passive Critical bonus
            case 410:
            case 411:
            case 412: { // Assasin/ Hermit / NL
                critSkill = SkillFactory.getSkill(隐士.强力投掷); //4100001 - 强力投掷 - 增加爆击概率和爆击最小伤害。\n需要技能：#c精准暗器3级以上#
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProp());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 1410:
            case 1411:
            case 1412: { // Night Walker
                critSkill = SkillFactory.getSkill(夜行者.强力投掷); //14100001 - 强力投掷 - 增加爆击概率和爆击最小伤害。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProp());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 3100:
            case 3110:
            case 3111:
            case 3112: {
                critSkill = SkillFactory.getSkill(恶魔猎手.愤怒); //31100006 - 愤怒 - 永久性地增加攻击力和爆击概率。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.watk += critSkill.getEffect(critlevel).getAttackX();
                }
                break;
            }
            case 2300:
            case 2310:
            case 2311:
            case 2312: {
                critSkill = SkillFactory.getSkill(双弩.敏锐瞄准); //23000003 - 敏锐瞄准 - 永久性地提高爆击概率。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 3210:
            case 3211:
            case 3212: {
                critSkill = SkillFactory.getSkill(唤灵斗师.精准长杖); //32100006 - 精准长杖 - 提高长杖系列武器的熟练度、魔力和爆击概率。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 434: {
                critSkill = SkillFactory.getSkill(双刀.锋利); //4340010 - 锋利 - 提高爆击概率和爆击最小伤害。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProp());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 211:
            case 212: {
                critSkill = SkillFactory.getSkill(火毒.魔法爆击); //2110009 - 魔法爆击 - 永久增加爆击概率和爆击最小伤害值。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 221:
            case 222: {
                critSkill = SkillFactory.getSkill(冰雷.魔法爆击); //2210009 - 魔法爆击 - 永久增加爆击概率和爆击最小伤害值。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 231:
            case 232: {
                critSkill = SkillFactory.getSkill(主教.魔法爆击); //2310010 - 魔法爆击 - 永久增加爆击概率和爆击最小伤害值。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 1211:
            case 1212: {
                break;
            }
            case 530:
            case 531:
            case 532: {
                critSkill = SkillFactory.getSkill(神炮王.致命炮火); //5300004 - 致命炮火 - 永久性地增加爆击概率和爆击最小伤害。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 300:
            case 310:
            case 311:
            case 312:
            case 320:
            case 321:
            case 322: { // Bowman
                critSkill = SkillFactory.getSkill(弓箭手.强力箭); //3000001 - 强力箭 - 爆击概率增加。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProp());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 2214:
            case 2217: {
                critSkill = SkillFactory.getSkill(龙神.魔法暴击); //22140000 - 魔法爆击 - 增加爆击攻击概率和爆击最小伤害。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getProp());
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                }
                break;
            }
            case 570:
            case 571:
            case 572: {
                break;
            }
            case 2003:
            case 2410:
            case 2411:
            case 2412: {
                critSkill = SkillFactory.getSkill(幻影.敏锐直觉); //24110007 - 敏锐直觉 - 永久性地提高爆击概率。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                }
                critSkill = SkillFactory.getSkill(幻影.致命本能); //20030204 - 致命本能 - 拥有通过卓越的洞察力，找到敌人致命弱点的本能。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                }
                break;
            }
            case 3101:
            case 3120:
            case 3121:
            case 3122: {
                critSkill = SkillFactory.getSkill(恶魔复仇者.恶魔之力); //31010003 - 恶魔之力 - 永久增加移动速度和最高移动速度、跳跃力、爆击率。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical());
                }
                break;
            }
            case 500:
            case 510:
            case 511:
            case 512:
            case 520:
            case 521:
            case 522: {
                critSkill = SkillFactory.getSkill(海盗.致命咆哮); //5000007 - 致命咆哮 - 增加爆击概率和爆击最小伤害。
                critlevel = player.getTotalSkillLevel(critSkill);
                if (critlevel > 0 && critSkill != null) {
                    this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical()); //爆击概率
                    this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin(); //爆击最小伤害
                }
                if (player.getJob() == 511 || player.getJob() == 512) {
                    critSkill = SkillFactory.getSkill(冲锋队长.致命狂热); //5110011 - 致命狂热 - 增加爆击概率和爆击伤害，对BOSS的爆击率额外提高。
                    critlevel = player.getTotalSkillLevel(critSkill);
                    if (critlevel > 0 && critSkill != null) {
                        this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical()); //爆击概率
                        this.passive_sharpeye_max_percent += critSkill.getEffect(critlevel).getCriticalMax(); //爆击伤害
                        this.percent_boss_damage_rate += critSkill.getEffect(critlevel).getProp(); //BOSS的爆击率额外提高
                    }
                    critSkill = SkillFactory.getSkill(冲锋队长.迷惑攻击); //5110000 - 迷惑攻击 - 攻击昏迷状态的敌人时，爆击概率增加。
                    critlevel = player.getTotalSkillLevel(critSkill);
                    if (critlevel > 0 && critSkill != null) {
                        this.passive_sharpeye_rate += (short) critSkill.getEffect(critlevel).getProp();
                        this.passive_sharpeye_min_percent += critSkill.getEffect(critlevel).getCriticalMin();
                    }
                }
                if (player.getJob() == 521 || player.getJob() == 522) {
                    critSkill = SkillFactory.getSkill(船长.合金子弹); //5210013 - 合金盔甲 - 学习在子弹上加装铁甲的技能，增加爆击概率，无视怪物的部分防御力。
                    critlevel = player.getTotalSkillLevel(critSkill);
                    if (critlevel > 0 && critSkill != null) {
                        this.passive_sharpeye_rate += (short) (critSkill.getEffect(critlevel).getCritical()); //爆击概率
                        this.percent_ignore_mob_def_rate += critSkill.getEffect(critlevel).getIgnoreMob(); //无视怪物的部分防御力
                    }
                }
                break;
            }
        }
    }

    public short passive_sharpeye_rate() {
        return passive_sharpeye_rate;
    }

    public short passive_sharpeye_min_percent() {
        return passive_sharpeye_min_percent;
    }

    public short passive_sharpeye_percent() {
        return passive_sharpeye_max_percent;
    }

    public byte passive_mastery() {
        return passive_mastery; //* 5 + 10 for mastery %
    }

    /*
     * 飞镖
     * 弓箭
     * 弩矢
     * 子弹
     * 等攻击伤害加成
     */
    public double calculateMaxProjDamage(int projectileWatk, MapleCharacter chra) {
        if (projectileWatk < 0) {
            return 0;
        } else {
            Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.没有武器 : ItemConstants.getWeaponType(weapon_item.getItemId());
            int mainstat, secondarystat;
            switch (weapon) {
                case 弓: //弓
                case 弩: //弩
                case 短枪: //短枪
                    mainstat = localdex; //敏捷
                    secondarystat = localstr; //力量
                    break;
                case 拳套: //拳套
                    mainstat = localluk; //运气
                    secondarystat = localdex; //敏捷
                    break;
                default:
                    mainstat = 0;
                    secondarystat = 0;
                    break;
            }
            float maxProjDamage = weapon.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (projectileWatk / 100.0f);
            maxProjDamage += maxProjDamage * (percent_damage / 100.0f);
            return maxProjDamage;
        }
    }

    public void calculateMaxBaseDamage(int watk, int lv2damX, int pvpDamage, MapleCharacter chra) {
        if (watk <= 0) {
            localmaxbasedamage = 1;
            localmaxbasepvpdamage = 1;
        } else {
            Item weapon_item = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
            Item weapon_item2 = chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            int job = chra.getJob();
            MapleWeaponType weapon = weapon_item == null ? MapleWeaponType.没有武器 : ItemConstants.getWeaponType(weapon_item.getItemId());
            MapleWeaponType weapon2 = weapon_item2 == null ? MapleWeaponType.没有武器 : ItemConstants.getWeaponType(weapon_item2.getItemId());
            int mainstat, secondarystat, thirdstat = 0, mainstatpvp, secondarystatpvp; // thirdstatpvp
            boolean mage = (job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || JobConstants.is龙神(job) || (job >= 2700 && job <= 2712) || (job >= 3200 && job <= 3212) || (job >= 11200 && job <= 11212);
            switch (weapon) {
                case 弓:
                case 弩:
                case 短枪:
                case 双弩枪:
                case 灵魂手铳:
                    mainstat = localdex;
                    secondarystat = localstr;
                    mainstatpvp = dex;
                    secondarystatpvp = str;
                    break;
                case 短刀:
                case 双刀副手:
                    mainstat = localluk;
                    secondarystat = localdex + localstr;
                    mainstatpvp = luk;
                    secondarystatpvp = dex + str;
                    break;
                case 手杖:
                case 拳套:
                    mainstat = localluk; //运气
                    secondarystat = localdex; //敏捷
                    mainstatpvp = luk;
                    secondarystatpvp = dex;
                    break;
                case 亡命剑:
                    mainstat = localstr;
                    secondarystat = localmaxhp;
                    mainstatpvp = localstr;
                    secondarystatpvp = localmaxhp;
                    break;
                case 能量剑:
                    mainstat = localstr; //力量
                    secondarystat = localluk; //运气
                    thirdstat = localdex; //敏捷
                    mainstatpvp = str;
                    secondarystatpvp = luk;
//                    thirdstatpvp = dex;
                    break;
                default:
                    if (mage) {
                        mainstat = localint_;
                        secondarystat = localluk;
                        mainstatpvp = int_;
                        secondarystatpvp = luk;
                    } else {
                        mainstat = localstr;
                        secondarystat = localdex;
                        mainstatpvp = str;
                        secondarystatpvp = dex;
                    }
                    break;
            }
            if (JobConstants.is新手职业(job)) {
                mainstat = localstr;
                secondarystat = localdex;
                mainstatpvp = str;
                secondarystatpvp = dex;
            }
            float weaponDamageMultiplier = weapon.getMaxDamageMultiplier();
            localmaxbasepvpdamage = weaponDamageMultiplier * (4 * mainstatpvp + secondarystatpvp) * (100.0f + (pvpDamage / 100.0f)) + lv2damX;
            localmaxbasepvpdamageL = weaponDamageMultiplier * (4 * mainstat + secondarystat) * (100.0f + (pvpDamage / 100.0f)) + lv2damX;
            if (weapon2 != MapleWeaponType.没有武器 && weapon_item != null && weapon_item2 != null && JobConstants.is恶魔复仇者(job)) {
//                Equip we1 = (Equip) weapon_item;
                Equip we2 = (Equip) weapon_item2;
                int watk2 = mage ? we2.getMatk() : we2.getWatk();
                localmaxbasedamage = weaponDamageMultiplier * (4 * mainstat + secondarystat) * ((watk - watk2) / 100.0f) + lv2damX;
                if (watk2 > 0) {
                    localmaxbasedamage += weapon2.getMaxDamageMultiplier() * (4 * mainstat + secondarystat) * (watk2 / 100.0f);
                }
            } else if (JobConstants.is恶魔复仇者(job)) {
                localmaxbasedamage = (weaponDamageMultiplier * mainstat + secondarystat * ((8.0f / 9.0f) - 0.73f)) * (watk / 100.0f) + lv2damX;
            } else if (JobConstants.is尖兵(job)) {
                localmaxbasedamage = weaponDamageMultiplier * (4 * (mainstat + secondarystat + thirdstat)) * (watk / 100.0f) + lv2damX;
            } else {
                if (job == 110 || job == 111 || job == 112) {
                    weaponDamageMultiplier += 0.1;
                }
                localmaxbasedamage = weaponDamageMultiplier * (4 * mainstat + secondarystat) * (watk / 100.0f) + lv2damX;
            }
            localmaxbasedamage += localmaxbasedamage * (percent_damage / 100.0f);
        }
    }

    public float getHealHP() {
        return shouldHealHP;
    }

    public float getHealMP() {
        return shouldHealMP;
    }

    /*
     * 自动回复血和蓝
     */
    public void relocHeal(MapleCharacter chra) {
        int playerjob = chra.getJob();
        //重置恢复数据信息和时间
        shouldHealHP = 10 + recoverHP;
        shouldHealMP = JobConstants.isNotMpJob(chra.getJob()) ? 0 : (3 + mpRestore + recoverMP + (localint_ / 10));
        mpRecoverTime = 0;
        hpRecoverTime = 0;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (playerjob == 111 || playerjob == 112) {
            Skill skill = SkillFactory.getSkill(英雄.自我恢复); //1110000 - 自我恢复 - 每4秒恢复HP和MP。在战斗过程中也可以恢复。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += effect.getHp();
                hpRecoverTime = 4000;
                shouldHealMP += effect.getMp();
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 1111 || playerjob == 1112) {
            Skill skill = SkillFactory.getSkill(魂骑士.钢铁之轮); //11110025 - 钢铁之轮 - 凭借超越常人的坚强意志，摆脱逆境。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += (effect.getY() * localmaxhp) / 100.0f;
                hpRecoverTime = effect.getW() * 1000;
            }
        } else if (playerjob == 510 || playerjob == 511 || playerjob == 512) {
            Skill skill = SkillFactory.getSkill(冲锋队长.忍耐); //5100013 - 忍耐 - 每隔一定时间，恢复HP和MP。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += (effect.getX() * localmaxhp) / 100.0f;
                hpRecoverTime = effect.getY(); //或者 eff.getW() * 1000
                shouldHealMP += (effect.getX() * localmaxmp) / 100.0f;
                mpRecoverTime = effect.getY(); //或者 eff.getW() * 1000
            }
        } else if (playerjob == 570 || playerjob == 571 || playerjob == 572) {
            Skill skill = SkillFactory.getSkill(5700005); //5700005 - 侠士的忍耐 - 每隔一定时间，恢复HP和MP。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += (effect.getX() * localmaxhp) / 100.0f;
                hpRecoverTime = effect.getY() * 1000;
                shouldHealMP += (effect.getX() * localmaxmp) / 100.0f;
                mpRecoverTime = effect.getY() * 1000;
            }
        } else if (JobConstants.is双弩精灵(playerjob)) {
            Skill skill = SkillFactory.getSkill(双弩.精灵恢复); //20020109 - 精灵恢复 - 利用自然中净化的能量，持续恢复HP和MP。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += (effect.getX() * localmaxhp) / 100.0f;
                hpRecoverTime = 4000;
                shouldHealMP += (effect.getX() * localmaxmp) / 100.0f;
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 3111 || playerjob == 3112) {
            Skill skill = SkillFactory.getSkill(恶魔猎手.极限精气吸收); //31110009 - 极限精气吸收 - 使用恶魔血月斩时，有一定概率获得额外的精气，每4秒自动恢复一定量的精气。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                shouldHealMP += skill.getEffect(lvl).getY();
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 5111 || playerjob == 5112) {
            Skill skill = SkillFactory.getSkill(米哈尔.自我恢复); //51110000 - 自我恢复 - 每4秒回复HP和MP，战斗中也能恢复。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += effect.getHp();
                hpRecoverTime = 4000;
                shouldHealMP += effect.getMp();
                mpRecoverTime = 4000;
            }
        } else if (playerjob == 6111 || playerjob == 6112) {
            Skill skill = SkillFactory.getSkill(狂龙战士.自我恢复); //61110006 - 自我恢复 - 持续补充体力和MP。
            int lvl = chra.getSkillLevel(skill);
            if (lvl > 0 && skill != null) {
                MapleStatEffect effect = skill.getEffect(lvl);
                shouldHealHP += (effect.getX() * localmaxhp) / 100.0f;
                hpRecoverTime = effect.getY(); //或者 eff.getW() * 1000
                shouldHealMP += (effect.getX() * localmaxmp) / 100.0f;
                mpRecoverTime = effect.getY(); //或者 eff.getW() * 1000
            }
        }
        if (chra.getChair() != 0) { // 椅子恢复的血量 暂时默认这个数字
            Pair<Integer, Integer> ret = ii.getChairRecovery(chra.getChair());
            if (ret != null) {
                shouldHealHP += ret.getLeft();
                if (hpRecoverTime == 0) {
                    hpRecoverTime = 4000;
                }
                shouldHealMP += JobConstants.isNotMpJob(chra.getJob()) ? 0 : ret.getRight();
                if (mpRecoverTime == 0 && !JobConstants.isNotMpJob(chra.getJob())) {
                    hpRecoverTime = 4000;
                }
            }
        } else if (chra.getMap() != null) { // 地图自动恢复的倍率
            float recvRate = chra.getMap().getRecoveryRate();
            if (recvRate > 0) {
                shouldHealHP *= recvRate;
                shouldHealMP *= recvRate;
            }
        }
    }

    public void connectData(MaplePacketLittleEndianWriter mplew) {
        mplew.writeShort(str); // str
        mplew.writeShort(dex); // dex
        mplew.writeShort(int_); // int
        mplew.writeShort(luk); // luk

        mplew.writeInt(hp); // hp
        mplew.writeInt(maxhp); // maxhp
        mplew.writeInt(mp); // mp
        mplew.writeInt(maxmp); // maxmp
    }

    public void zeroData(MaplePacketLittleEndianWriter mplew, MapleCharacter chr) {
        mplew.write(0xFF);
        mplew.write(0xFF);
        mplew.write(chr.isZeroSecondLook() ? 0x01 : 0x00); //0x00 为男 0x01为女
        mplew.writeInt(maxhp);
        mplew.writeInt(maxmp);
        mplew.write(0x00);
        mplew.writeInt(chr.getSecondHair());
        mplew.writeInt(chr.getSecondFace());
        mplew.writeInt(maxhp);
        mplew.writeInt(maxmp);
        mplew.writeInt(0x00);
        mplew.writeInt(-1);
        mplew.writeZeroBytes(8);
    }

    public int getSkillIncrement(int skillID) {
        if (skillsIncrement.containsKey(skillID)) {
            return skillsIncrement.get(skillID);
        }
        return 0;
    }

    public int getElementBoost(Element key) {
        if (elemBoosts.containsKey(key)) {
            return elemBoosts.get(key);
        }
        return 0;
    }

    public int getDamageIncrease(int key) {
        if (damageIncrease.containsKey(key)) {
            return damageIncrease.get(key);
        }
        return 0;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void heal_noUpdate(MapleCharacter chra) {
        setHp(getCurrentMaxHp(), chra);
        setMp(getCurrentMaxMp(chra.getJob()), chra);
    }

    public void heal(MapleCharacter chra) {
        heal_noUpdate(chra);
        chra.updateSingleStat(MapleStat.HP, getCurrentMaxHp());
        chra.updateSingleStat(MapleStat.MP, getCurrentMaxMp(chra.getJob()));
    }

    public Pair<Integer, Integer> handleEquipAdditions(MapleItemInformationProvider ii, MapleCharacter chra, boolean first_login, Map<Integer, SkillEntry> sData, int itemId) {
        Map<String, ?> additions = ii.getEquipAdditions(itemId);
        if (additions == null) {
            return null;
        }
        int localmaxhp_x = 0, localmaxmp_x = 0;
        int skillid = 0, skilllevel = 0;
        for (Entry<String, ?> add : additions.entrySet()) {
//            int right = Integer.parseInt(ii.getEquipAdditionInfo(itemId, "elemboost/elemVol"));
            switch (add.getKey()) {
                case "elemboost": {
                    String craft = null, elemVol = null;
                    try {
                        craft = (String) ((Map<?, ?>) ((Map<?, ?>) add.getValue()).get("con")).get("craft");
                        elemVol = (String) ((Map<?, ?>) add.getValue()).get("elemVol");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (elemVol != null && (craft == null || chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
                        int value = Integer.parseInt(elemVol.substring(1, elemVol.length()));
                        Element key = Element.getFromChar(elemVol.charAt(0));
                        if (elemBoosts.get(key) != null) {
                            value += elemBoosts.get(key);
                        }
                        elemBoosts.put(key, value);
                    }
                    break;
                }
                case "mobcategory": {
                    Integer damage = (Integer) ((Map<?, ?>) add.getValue()).get("damage");
                    if (damage != null) {
                        percent_damage_rate += damage;
                        percent_boss_damage_rate += damage;
                    }
                    break;
                }
                case "critical":
                    boolean canJob = true,
                            canLevel = true;
                    int prob = 0, damage = 0;
                    if (add.getValue() instanceof Map<?, ?>) {
                        for (Entry<String, ?> entry : ((Map<String, ?>) add.getValue()).entrySet()) {
                            switch (entry.getKey()) {
                                case "con":
                                    Map<?, ?> subentry = (Map<?, ?>) entry.getValue();
                                    if (subentry.containsKey("job")) {
                                        canJob = subentry.values().contains(chra.getJob());
                                    } else if (subentry.containsKey("lv")) {
                                        canLevel = chra.getLevel() >= (Integer) subentry.get("lv");
                                    }
                                    break;
                                case "prob":
                                    prob = (Integer) entry.getValue();
                                    break;
                                case "damage":
                                    try {
                                        damage = Integer.parseInt(entry.getValue().toString());
                                    } catch (ClassCastException e) {
                                        log.error("读取damage错误, Itemid: " + itemId, e);
                                    }
                            }
                        }
                    }
                    if (canJob && canLevel) {
                        passive_sharpeye_rate += prob;
                        passive_sharpeye_min_percent += damage;
                        passive_sharpeye_max_percent += damage;
                    }
                    break;
                case "boss":
                    // ignore prob, just add
//                    String craft = ii.getEquipAdditionInfo(itemId, add.getLeft(), "craft");
//                    if (add.getMid().equals("damage") && (craft == null || chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
//                        percent_boss_damage_rate += right;
//                    }
//                    break;
                case "mobdie":
                    // lv, hpIncRatioOnMobDie, hpRatioProp, mpIncRatioOnMobDie, mpRatioProp, modify =D, don't need mob to die
//                    craft = ii.getEquipAdditionInfo(itemId, add.getLeft(), "craft");
//                    if ((craft == null || chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
//                        switch (add.getMid()) {
//                            case "hpIncOnMobDie":
//                                hpRecover += right;
//                                hpRecoverProp += 5;
//                                break;
//                            case "mpIncOnMobDie":
//                                mpRecover += right;
//                                mpRecoverProp += 5;
//                                break;
//                        }
//                    }
//                    break;
                case "skill":
                    // all these are additional skills
//                    if (first_login) {
//                        craft = ii.getEquipAdditionInfo(itemId, add.getLeft(), "craft");
//                        if ((craft == null || chra.getTrait(MapleTraitType.craft).getLocalTotalExp() >= Integer.parseInt(craft))) {
//                            switch (add.getMid()) {
//                                case "id":
//                                    skillid = right;
//                                    break;
//                                case "level":
//                                    skilllevel = right;
//                                    break;
//                            }
//                        }
//                    }
//                    break;
                case "hpmpchange":
//                    switch (add.getMid()) {
//                        case "hpChangerPerTime":
//                            recoverHP += right;
//                            break;
//                        case "mpChangerPerTime":
//                            recoverMP += right;
//                            break;
//                    }
//                    break;
                case "statinc":
//                    boolean canJobx = false,
//                            canLevelx = false;
//                    job = ii.getEquipAdditionInfo(itemId, add.getLeft(), "job");
//                    if (job != null) {
//                        if (job.contains(",")) {
//                            String[] jobs = job.split(",");
//                            for (String x : jobs) {
//                                if (chra.getJob() == Integer.parseInt(x)) {
//                                    canJobx = true;
//                                }
//                            }
//                        } else if (chra.getJob() == Integer.parseInt(job)) {
//                            canJobx = true;
//                        }
//                    }
//                    level = ii.getEquipAdditionInfo(itemId, add.getLeft(), "level");
//                    if (level != null && chra.getLevel() >= Integer.parseInt(level)) {
//                        canLevelx = true;
//                    }
//                    if ((!canJobx && job != null) || (!canLevelx && level != null)) {
//                        continue;
//                    }
//                    if (itemId == 1142367) { //1142367 - 巧克力棒周末特别勋章 - (无描述)
//                        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
//                        if (day != 1 && day != 7) {
//                            continue;
//                        }
//                    }
//                    switch (add.getMid()) {
//                        case "incPAD":
//                            watk += right;
//                            break;
//                        case "incMAD":
//                            magic += right;
//                            break;
//                        case "incSTR":
//                            localstr += right;
//                            break;
//                        case "incDEX":
//                            localdex += right;
//                            break;
//                        case "incINT":
//                            localint_ += right;
//                            break;
//                        case "incLUK":
//                            localluk += right;
//                            break;
//                        case "incJump":
//                            jump += right;
//                            break;
//                        case "incMHP":
//                            localmaxhp_x += right;
//                            break;
//                        case "incMMP":
//                            localmaxmp_x += right;
//                            break;
//                        case "incPDD":
//                            wdef += right;
//                            break;
//                        case "incMDD":
//                            mdef += right;
//                            break;
//                        case "incACC":
//                            accuracy += right;
//                            break;
//                        case "incEVA":
//                            break;
//                        case "incSpeed":
//                            speed += right;
//                            break;
//                        case "incMMPr":
//                            percent_mp += right;
//                            break;
//                    }
                    break;
            }
        }
        if (skillid != 0 && skilllevel != 0) {
            sData.put(skillid, new SkillEntry((byte) skilllevel, (byte) 0, -1));
        }
        return new Pair<>(localmaxhp_x, localmaxmp_x);
    }

    /*
     * 处理角色套装属性
     */
    public Pair<Integer, Integer> handleEquipSetStats(MapleItemInformationProvider ii, MapleCharacter chra, boolean first_login, Map<Integer, SkillEntry> sData, int setId, int setCount) {
        //获取套装ID的信息
        StructSetItem setItem = ii.getSetItem(setId);
        if (setItem == null) {
            return null;
        }
        //套装属性信息 [激活需要的数量 套装属性信息]
        Map<Integer, StructSetItemStat> setItemStats = setItem.getSetItemStats();
        //System.err.println("套装编号: " + setItem.setItemID + " 名称: " + setItem.setItemName + " 当前佩戴数量: " + setCount);
        int localmaxhp_x = 0, localmaxmp_x = 0;
        StructItemOption soc;
        for (Entry<Integer, StructSetItemStat> ent : setItemStats.entrySet()) {
            //System.err.println("套装编号: " + setItem.setItemID + " 名称: " + setItem.setItemName + " 激活套装属性ID: " + ent.getKey() + " 能否激活: " + (ent.getKey() == setCount));
            if (ent.getKey() == setCount) { //必须等于才处理
                //System.err.println("开始处理激活的套装属性信息....");
                StructSetItemStat setItemStat = ent.getValue();
                localstr += setItemStat.incSTR + setItemStat.incAllStat;
                localdex += setItemStat.incDEX + setItemStat.incAllStat;
                localint_ += setItemStat.incINT + setItemStat.incAllStat;
                localluk += setItemStat.incLUK + setItemStat.incAllStat;
                watk += setItemStat.incPAD;
                magic += setItemStat.incMAD;
                speed += setItemStat.incSpeed;
                accuracy += setItemStat.incACC;
                localmaxhp_x += setItemStat.incMHP;
                localmaxmp_x += setItemStat.incMMP;
                percent_hp += setItemStat.incMHPr;
                percent_mp += setItemStat.incMMPr;
                wdef += setItemStat.incPDD;
                mdef += setItemStat.incMDD;
                if (setItemStat.option1 > 0 && setItemStat.option1Level > 0) {
                    soc = ii.getPotentialInfo(setItemStat.option1).get(setItemStat.option1Level);
                    if (soc != null) {
                        localmaxhp_x += soc.get("incMHP");
                        localmaxmp_x += soc.get("incMMP");
                        handleItemOption(soc, chra, first_login, sData);
                    }
                }
                if (setItemStat.option2 > 0 && setItemStat.option2Level > 0) {
                    soc = ii.getPotentialInfo(setItemStat.option2).get(setItemStat.option2Level);
                    if (soc != null) {
                        localmaxhp_x += soc.get("incMHP");
                        localmaxmp_x += soc.get("incMMP");
                        handleItemOption(soc, chra, first_login, sData);
                    }
                }
            }
        }
        return new Pair<>(localmaxhp_x, localmaxmp_x);
    }

    public void handleItemOption(StructItemOption soc, MapleCharacter chra, boolean first_login, Map<Integer, SkillEntry> sData) {
        //System.out.println("incSTR: " + soc.get("incSTR") + " incDEX: " + soc.get("incDEX") + " incINT: " + soc.get("incINT") + " incLUK: " + soc.get("incLUK"));
        //System.out.println("incSTRr: " + soc.get("incSTRr") + " incDEXr: " + soc.get("incDEXr") + " incINTr: " + soc.get("incINTr") + " incLUKr: " + soc.get("incLUKr"));
        localstr += soc.get("incSTR");
        localdex += soc.get("incDEX");
        localint_ += soc.get("incINT");
        localluk += soc.get("incLUK");
        if (soc.get("incSTRlv") > 0) {
            localstr += (chra.getLevel() / 10) * soc.get("incSTRlv");
        }
        if (soc.get("incDEXlv") > 0) {
            localdex += (chra.getLevel() / 10) * soc.get("incDEXlv");
        }
        if (soc.get("incINTlv") > 0) {
            localint_ += (chra.getLevel() / 10) * soc.get("incINTlv");
        }
        if (soc.get("incLUKlv") > 0) {
            localluk += (chra.getLevel() / 10) * soc.get("incLUKlv");
        }
        accuracy += soc.get("incACC");
        // incEVA -> increase dodge
        speed += soc.get("incSpeed");
        jump += soc.get("incJump");
        watk += soc.get("incPAD");
        if (soc.get("incPADlv") > 0) {
            watk += (chra.getLevel() / 10) * soc.get("incPADlv");
        }
        magic += soc.get("incMAD");
        if (soc.get("incMADlv") > 0) {
            magic += (chra.getLevel() / 10) * soc.get("incMADlv");
        }
        wdef += soc.get("incPDD");
        mdef += soc.get("incMDD");
        percent_str += soc.get("incSTRr");
        percent_dex += soc.get("incDEXr");
        percent_int += soc.get("incINTr");
        percent_luk += soc.get("incLUKr");
        percent_hp += soc.get("incMHPr");
        percent_mp += soc.get("incMMPr");
        percent_acc += soc.get("incACCr");
        dodgeChance += soc.get("incEVAr");
        percent_atk += soc.get("incPADr");
        percent_matk += soc.get("incMADr");
        percent_wdef += soc.get("incPDDr");
        percent_mdef += soc.get("incMDDr");
        passive_sharpeye_rate += soc.get("incCr");
        percent_boss_damage_rate += soc.get("incDAMr");
        if (soc.get("boss") <= 0) {
            percent_damage_rate += soc.get("incDAMr");
        }
        recoverHP += soc.get("RecoveryHP"); // This shouldn't be here, set 4 seconds.
        recoverMP += soc.get("RecoveryMP"); // This shouldn't be here, set 4 seconds.
        if (soc.get("HP") > 0) { // Should be heal upon attacking
            hpRecover += soc.get("HP");
            hpRecoverProp += soc.get("prop");
        }
        if (soc.get("MP") > 0 && !JobConstants.isNotMpJob(chra.getJob())) {
            mpRecover += soc.get("MP");
            mpRecoverProp += soc.get("prop");
        }
        percent_ignore_mob_def_rate += soc.get("ignoreTargetDEF");
        if (soc.get("ignoreDAM") > 0) {
            ignoreDAM += soc.get("ignoreDAM");
            ignoreDAM_rate += soc.get("prop");
        }
        incAllskill += soc.get("incAllskill");
        if (soc.get("ignoreDAMr") > 0) {
            ignoreDAMr += soc.get("ignoreDAMr");
            ignoreDAMr_rate += soc.get("prop");
        }
        if (soc.get("incMaxDamage") > 0) {
            incMaxDamage += soc.get("incMaxDamage");
        }
        RecoveryUP += soc.get("RecoveryUP"); // only for hp items and skills
        passive_sharpeye_min_percent += soc.get("incCriticaldamageMin");
        passive_sharpeye_max_percent += soc.get("incCriticaldamageMax");
        TER += soc.get("incTerR"); // elemental resistance = avoid element damage from monster
        ASR += soc.get("incAsrR"); // abnormal status = disease
        if (soc.get("DAMreflect") > 0) {
            DAMreflect += soc.get("DAMreflect");
            DAMreflect_rate += soc.get("prop");
        }
        mpconReduce += soc.get("mpconReduce");
        reduceCooltime += soc.get("reduceCooltime"); // in seconds
        incMesoProp += soc.get("incMesoProp"); // 金币掉率 + %
        incRewardProp += soc.get("incRewardProp"); // 道具掉落 + %先不计算百分百的掉落几率 最后计算
        BuffUP_Skill += soc.get("bufftimeR");
        if (first_login && soc.get("skillID") > 0) {
            sData.put(getSkillByJob(soc.get("skillID"), chra.getJob()), new SkillEntry((byte) 1, (byte) 0, -1));
        }
    }

    public void recalcPVPRank(MapleCharacter chra) {
        this.pvpRank = 10;
        this.pvpExp = chra.getTotalBattleExp();
        for (int i = 0; i < 10; i++) {
            if (pvpExp > GameConstants.getPVPExpNeededForLevel(i + 1)) {
                pvpRank--;
                pvpExp -= GameConstants.getPVPExpNeededForLevel(i + 1);
            }
        }
    }

    public int getHPPercent() {
        return (int) Math.ceil((hp * 100.0) / localmaxhp);
    }

    public void init(MapleCharacter chra) {
        recalcLocalStats(chra);
    }

    public short getStr() {
        return str;
    }

    public short getDex() {
        return dex;
    }

    public short getLuk() {
        return luk;
    }

    public short getInt() {
        return int_;
    }

    public void setStr(short str, MapleCharacter chra) {
        this.str = str;
        recalcLocalStats(chra);
    }

    public void setDex(short dex, MapleCharacter chra) {
        this.dex = dex;
        recalcLocalStats(chra);
    }

    public void setLuk(short luk, MapleCharacter chra) {
        this.luk = luk;
        recalcLocalStats(chra);
    }

    public void setInt(short int_, MapleCharacter chra) {
        this.int_ = int_;
        recalcLocalStats(chra);
    }

    public int getHealHp() {
        return Math.max(localmaxhp - hp, 0);
    }

    public int getHealMp(int job) {
        if (JobConstants.isNotMpJob(job)) {
            return 0;
        }
        return Math.max(localmaxmp - mp, 0);
    }

    public boolean setHp(int newhp, MapleCharacter chra) {
        return setHp(newhp, false, chra);
    }

    public boolean setHp(int newhp, boolean silent, MapleCharacter chra) {
        int oldHp = hp;
        int thp = newhp;
        if (thp < 0) {
            thp = 0;
        }
        if (thp > localmaxhp) {
            thp = localmaxhp;
        }
        this.hp = thp;
        if (chra != null) {
            if (!silent) {
                chra.checkBerserk();
                chra.updatePartyMemberHP();
            }
        }
        return hp != oldHp;
    }

    public boolean setMp(int newmp, MapleCharacter chra) {
        int oldMp = mp;
        int tmp = newmp;
        if (tmp < 0) {
            tmp = 0;
        }
        if (tmp > localmaxmp) {
            tmp = localmaxmp;
        }
        this.mp = tmp;
        return mp != oldMp;
    }

    public void setInfo(int maxhp, int maxmp, int hp, int mp) {
        this.maxhp = maxhp;
        this.maxmp = maxmp;
        this.hp = hp;
        this.mp = mp;
    }

    public void setMaxHp(int hp, MapleCharacter chra) {
        this.maxhp = hp;
        recalcLocalStats(chra);
    }

    public void setMaxMp(int mp, MapleCharacter chra) {
        this.maxmp = mp;
        recalcLocalStats(chra);
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxhp;
    }

    public int getMp() {
        return mp;
    }

    public int getMaxMp() {
        return maxmp;
    }

    public int getTotalDex() {
        return localdex;
    }

    public int getTotalInt() {
        return localint_;
    }

    public int getTotalStr() {
        return localstr;
    }

    public int getTotalLuk() {
        return localluk;
    }

    public int getTotalMagic() {
        return magic;
    }

    public int getSpeed() {
        return speed;
    }

    public int getJump() {
        return jump;
    }

    public int getTotalWatk() {
        return watk;
    }

    public int getCurrentMaxHp() {
        return localmaxhp;
    }

    public int getCurrentMaxMp(int job) {
        if (JobConstants.isNotMpJob(job) && GameConstants.getMPByJob(job) <= 0) {
            return 50;
        }
        return localmaxmp;
    }

    public float getCurrentMaxBaseDamage() {
        return localmaxbasedamage;
    }

    public float getCurrentMaxBasePVPDamage() {
        return localmaxbasepvpdamage;
    }

    public float getCurrentMaxBasePVPDamageL() {
        return localmaxbasepvpdamageL;
    }

    public boolean isRangedJob(int job) {
        return JobConstants.is龙的传人(job) || JobConstants.is双弩精灵(job) || JobConstants.is火炮手(job) || job == 400 || (job / 10 == 52) || (job / 10 == 59) || (job / 100 == 3) || (job / 100 == 13) || (job / 100 == 14) || (job / 100 == 33) || (job / 100 == 35) || (job / 10 == 41);
    }

    /*
     * 所有技能的冷却时间 : -#reduceCooltime秒(最多减少5秒)
     */
    public int getCoolTimeR() {
        if (coolTimeR > 5) {
            return 5;
        }
        return coolTimeR;
    }

    public int getReduceCooltime() {
        if (reduceCooltime > 5) {
            return 5;
        }
        return reduceCooltime;
    }

    /*
     * 武器突破极限的攻击上限
     */
    public int getLimitBreak(MapleCharacter chra) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        //主手武器突破极限的攻击上限
        int limitBreak = 999999;
        Equip weapon = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon != null) {
            limitBreak = ii.getLimitBreak(weapon.getItemId()) + weapon.getLimitBreak();
            //副手手武器突破极限的攻击上限 必须佩戴主手才检测副手且副手装备为武器
            Equip subweapon = (Equip) chra.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
            if (subweapon != null && ItemConstants.isWeapon(subweapon.getItemId())) {
                int subWeaponLB = ii.getLimitBreak(subweapon.getItemId()) + subweapon.getLimitBreak();
                if (subWeaponLB > limitBreak) {
                    limitBreak = subWeaponLB;
                }
            }
        }
        return limitBreak;
    }

    /*
     * 额外增加攻击次数
     */
    public int getAttackCount(int skillId) {
        if (add_skill_attackCount.containsKey(skillId)) {
            return add_skill_attackCount.get(skillId);
        }
        return 0;
    }

    /*
     * 额外增加攻击怪物数量
     */
    public int getMobCount(int skillId) {
        if (add_skill_targetPlus.containsKey(skillId)) {
            return add_skill_targetPlus.get(skillId);
        }
        return 0;
    }

    /*
     * 技能冷却时间减少
     */
    public int getReduceCooltimeRate(int skillId) {
        if (add_skill_coolTimeR.containsKey(skillId)) {
            return add_skill_coolTimeR.get(skillId);
        }
        return 0;
    }

    /*
     * 无视怪物防御
     */
    public int getIgnoreMobpdpR(int skillId) {
        if (add_skill_ignoreMobpdpR.containsKey(skillId)) {
            return add_skill_ignoreMobpdpR.get(skillId) + percent_ignore_mob_def_rate;
        }
        return percent_ignore_mob_def_rate;
    }

    /*
     * 伤害%
     */
    public double getDamageRate() {
        return percent_damage_rate;
    }

    /*
     * BOSS伤害%
     */
    public int getBossDamageRate() {
        return percent_boss_damage_rate;
    }

    /*
     * 角色对BOSS的攻击加成
     */
    public int getBossDamageRate(int skillId) {
        if (add_skill_bossDamageRate.containsKey(skillId)) {
            return add_skill_bossDamageRate.get(skillId) + percent_boss_damage_rate;
        }
        return percent_boss_damage_rate;
    }

    /*
     * 格外增加技能的持续时间
     */
    public int getDuration(int skillId) {
        if (add_skill_duration.containsKey(skillId)) {
            return add_skill_duration.get(skillId);
        }
        return 0;
    }

    /*
     * 增加技能的伤害
     */
    public void addDamageIncrease(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (damageIncrease.containsKey(skillId)) {
            int oldval = damageIncrease.get(skillId);
            damageIncrease.put(skillId, oldval + val);
        } else {
            damageIncrease.put(skillId, val);
        }
    }

    /*
     * 增加技能攻击怪物的数量
     */
    public void addTargetPlus(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_targetPlus.containsKey(skillId)) {
            int oldval = add_skill_targetPlus.get(skillId);
            add_skill_targetPlus.put(skillId, oldval + val);
        } else {
            add_skill_targetPlus.put(skillId, val);
        }
    }

    /*
     * 增加技能攻击怪物的次数
     */
    public void addAttackCount(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_attackCount.containsKey(skillId)) {
            int oldval = add_skill_attackCount.get(skillId);
            add_skill_attackCount.put(skillId, oldval + val);
        } else {
            add_skill_attackCount.put(skillId, val);
        }
    }

    /*
     * 增加技能对BOSS的伤害
     */
    public void addBossDamageRate(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_bossDamageRate.containsKey(skillId)) {
            int oldval = add_skill_bossDamageRate.get(skillId);
            add_skill_bossDamageRate.put(skillId, oldval + val);
        } else {
            add_skill_bossDamageRate.put(skillId, val);
        }
    }

    /*
     * 增加技能攻击怪物的无视概率
     */
    public void addIgnoreMobpdpRate(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_ignoreMobpdpR.containsKey(skillId)) {
            int oldval = add_skill_ignoreMobpdpR.get(skillId);
            add_skill_ignoreMobpdpR.put(skillId, oldval + val);
        } else {
            add_skill_ignoreMobpdpR.put(skillId, val);
        }
    }

    /*
     * 增加技能BUFF的持续时间
     */
    public void addBuffDuration(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_duration.containsKey(skillId)) {
            int oldval = add_skill_duration.get(skillId);
            add_skill_duration.put(skillId, oldval + val);
        } else {
            add_skill_duration.put(skillId, val);
        }
    }

    /*
     * 增加技能攻击怪物的中毒效果的持续时间
     */
    public void addDotTime(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_dotTime.containsKey(skillId)) {
            int oldval = add_skill_dotTime.get(skillId);
            add_skill_dotTime.put(skillId, oldval + val);
        } else {
            add_skill_dotTime.put(skillId, val);
        }
    }

    /*
     * 技能冷去时间减少
     */
    public void addCoolTimeReduce(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_coolTimeR.containsKey(skillId)) {
            int oldval = add_skill_coolTimeR.get(skillId);
            add_skill_coolTimeR.put(skillId, oldval + val);
        } else {
            add_skill_coolTimeR.put(skillId, val);
        }
    }

    /*
     * 增加技能触发的概率
     */
    public void addSkillProp(int skillId, int val) {
        if (skillId < 0 || val <= 0) {
            return;
        }
        if (add_skill_prop.containsKey(skillId)) {
            int oldval = add_skill_prop.get(skillId);
            add_skill_prop.put(skillId, oldval + val);
        } else {
            add_skill_prop.put(skillId, val);
        }
    }

    public int getGauge_x() {
        return gauge_x;
    }

    public int getEfftype() {
        return efftype;
    }
}
