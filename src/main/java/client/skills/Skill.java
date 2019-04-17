package client.skills;

import constants.JobConstants;
import constants.SkillConstants;
import constants.skills.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataTool;
import server.MapleStatEffect;
import server.MapleStatEffectFactory;
import server.life.Element;
import tools.Pair;
import tools.Randomizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Skill {

    public static final Logger log = LogManager.getLogger();

    private final List<MapleStatEffect> effects = new ArrayList<>();
    private final List<Pair<String, Byte>> requiredSkill = new ArrayList<>();
    private final String name = "";
    private final boolean isSwitch = false;
    private final Map<Integer, Integer> bonusExpInfo = new HashMap<>(); //[技能等级] [升级需要的经验]
    private int id;
    private String psdDamR = "";
    private String targetPlus = "";
    private String minionAttack = "";
    private String minionAbility = "";
    private Element element = Element.NEUTRAL;
    private List<MapleStatEffect> pvpEffects = null;
    private List<Integer> animation = null;
    private int hyper = 0, hyperStat = 0, reqLev = 0, animationTime = 0, masterLevel = 0, maxLevel = 0, delay = 0, trueMax = 0, eventTamingMob = 0, skillType = 0,
            fixLevel, disableNextLevelInfo, psd = 0, psdSkill = 0, setItemReason, setItemPartsCount, maxDamageOver = 999999, ppRecovery = 0;
    private boolean invisible = false;
    private boolean chargeSkill = false;
    private boolean timeLimited = false;
    private boolean combatOrders = false;
    private boolean pvpDisabled = false;
    private boolean magic = false;
    private boolean casterMove = false;
    private boolean chargingSkill;
    private boolean passiveSkill;
    private boolean selfDestructMinion;
    private boolean rapidAttack;
    private boolean pushTarget = false;
    private boolean pullTarget = false;
    private boolean buffSkill = false;
    private boolean isSummon = false;
    private boolean notRemoved = false;
    private boolean disable = false;
    private boolean hasMasterLevelProperty = false;
    private boolean petPassive = false;
    private boolean finalAttack = false;
    private boolean soulSkill = false;

    public Skill() {

    }

    public Skill(int id) {
        super();
        this.id = id;
    }

    public static Skill loadFromData(int id, MapleData data, MapleData delayData) {
        boolean showSkill = false;
        if (showSkill) {
            System.out.println("正在解析技能id: " + id + " 名字: " + SkillFactory.getSkillName(id));
            log.trace("正在解析技能id: " + id + " 名字: " + SkillFactory.getSkillName(id), true);
        }
        Skill ret = new Skill(id);

        boolean isBuff;
        int skillType = MapleDataTool.getInt("skillType", data, -1);
        String elem = MapleDataTool.getString("elemAttr", data, null);
        ret.element = elem != null ? Element.getFromChar(elem.charAt(0)) : Element.NEUTRAL;
        ret.skillType = skillType;
        ret.invisible = MapleDataTool.getInt("invisible", data, 0) > 0;
        MapleData effect = data.getChildByPath("effect");
        MapleData common = data.getChildByPath("common");
        MapleData inf = data.getChildByPath("info");
        MapleData hit = data.getChildByPath("hit");
        MapleData ball = data.getChildByPath("ball");
        ret.isSummon = data.getChildByPath("summon") != null;
        ret.masterLevel = MapleDataTool.getInt("masterLevel", data, 0);
        if (ret.masterLevel > 0) {
            ret.hasMasterLevelProperty = true;
        }
        ret.psd = MapleDataTool.getInt("psd", data, 0);
        if (ret.psd == 1) {
            final MapleData psdskill = data.getChildByPath("psdSkill");
            if (psdskill != null) {
                ret.psdSkill = Integer.parseInt(data.getChildByPath("psdSkill").getChildren().get(0).getName());
            }
        }
        ret.notRemoved = MapleDataTool.getInt("notRemoved", data, 0) > 0;
        ret.timeLimited = MapleDataTool.getInt("timeLimited", data, 0) > 0;
        ret.combatOrders = MapleDataTool.getInt("combatOrders", data, 0) > 0;
        ret.fixLevel = MapleDataTool.getInt("fixLevel", data, 0);
        ret.disable = MapleDataTool.getInt("disable", data, 0) > 0;
        ret.disableNextLevelInfo = MapleDataTool.getInt("disableNextLevelInfo", data, 0);
        ret.eventTamingMob = MapleDataTool.getInt("eventTamingMob", data, 0);
        ret.hyper = MapleDataTool.getInt("hyper", data, 0); //超级技能栏位设置 P A
        ret.hyperStat = MapleDataTool.getInt("hyperStat", data, 0); //超级属性点
        ret.reqLev = MapleDataTool.getInt("reqLev", data, 0); //超级技能需要的等级
        ret.petPassive = MapleDataTool.getInt("petPassive", data, 0) > 0; //是否宠物被动触发技能
        ret.setItemReason = MapleDataTool.getInt("setItemReason", data, 0); //触发技能的套装ID
        ret.setItemPartsCount = MapleDataTool.getInt("setItemPartsCount", data, 0); //触发技能需要的数量
        ret.ppRecovery = MapleDataTool.getInt("ppRecovery", data, 0); //超能力者pp恢复量
        if (inf != null) {
            ret.pvpDisabled = MapleDataTool.getInt("pvp", inf, 1) <= 0;
            ret.magic = MapleDataTool.getInt("magicDamage", inf, 0) > 0;
            ret.casterMove = MapleDataTool.getInt("casterMove", inf, 0) > 0;
            ret.pushTarget = MapleDataTool.getInt("pushTarget", inf, 0) > 0;
            ret.pullTarget = MapleDataTool.getInt("pullTarget", inf, 0) > 0;
            ret.rapidAttack = MapleDataTool.getInt("rapidAttack", inf, 0) > 0;
            ret.minionAttack = MapleDataTool.getString("minionAttack", inf, "");
            ret.minionAbility = MapleDataTool.getString("minionAbility", inf, "");
            ret.selfDestructMinion = MapleDataTool.getInt("selfDestructMinion", inf, 0) > 0;
            ret.chargingSkill = MapleDataTool.getInt("chargingSkill", inf, 0) > 0 || MapleDataTool.getInt("keydownThrowing", inf, 0) > 0 || id == 冰雷.极冻吐息;
        }
        if (skillType == 2) {
            isBuff = true;
        } else if (skillType == 3) { //final attack
            ret.animation = new ArrayList<>();
            ret.animation.add(0);
            isBuff = effect != null;
            switch (id) {
                case 夜光.太阳火焰:
                case 夜光.月蚀:
                case 夜光.平衡_光明:
                case 夜光.光明黑暗模式转换:
                case 夜光.平衡_黑暗:
                    isBuff = true;
            }
        } else {
            MapleData action_ = data.getChildByPath("action");
            boolean action = false;
            if (action_ == null && data.getChildByPath("prepare/action") != null) {
                action_ = data.getChildByPath("prepare/action");
                action = true;
            }
            isBuff = effect != null && hit == null && ball == null;
            if (action_ != null) {
                String d;
                if (action) { //prepare
                    d = MapleDataTool.getString(action_, null);
                } else {
                    d = MapleDataTool.getString("0", action_, null);
                }
                if (d != null) {
                    isBuff |= d.equals("alert2");
                    MapleData dd = delayData.getChildByPath(d);
                    if (dd != null) {
                        for (MapleData del : dd) {
                            ret.delay += Math.abs(MapleDataTool.getInt("delay", del, 0));
                        }
                        if (ret.delay > 30) { //then, faster(2) = (10+2)/16 which is basically 3/4
                            ret.delay = (int) Math.round(ret.delay * 11.0 / 16.0); //fastest(1) lolol
                            ret.delay -= ret.delay % 30; //round to 30ms
                        }
                    }
                    if (SkillFactory.getDelay(d) != null) { //this should return true always
                        ret.animation = new ArrayList<>();
                        ret.animation.add(SkillFactory.getDelay(d));
                        if (!action) {
                            for (MapleData ddc : action_) {
                                if (!MapleDataTool.getString(ddc, d).equals(d) && !ddc.getName().contentEquals("delay")) {
                                    String c = MapleDataTool.getString(ddc);
                                    if (SkillFactory.getDelay(c) != null) {
                                        ret.animation.add(SkillFactory.getDelay(c));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            isBuff = SkillConstants.isBuffSkill(id, isBuff);
        }
        ret.chargeSkill = data.getChildByPath("keydown") != null;
        if (inf != null) {
            inf.getChildren().forEach(mapleData -> {
                if (mapleData.getName().equals("finalAttack") && ((Integer) mapleData.getData()) == 1 && !SkillFactory.getFinalAttackSkills().contains(id)) {
                    ret.finalAttack = true;
                    SkillFactory.getFinalAttackSkills().add(id);
                }
            });
        }
        if (ret.chargeSkill) {
            //FileoutputUtil.log("chargeSkill.txt", "技能id: " + id + " 名字: " + SkillFactory.getSkillName(id), true);
        } else {
            switch (id) {
                case 冰雷.寒霜爆晶:
                case 林之灵.旋风飞行:
                case 夜光.晨星坠落:
                    ret.chargeSkill = true;
            }
        }
        //有些技能是老的XML模式
        if (common != null) {
            ret.soulSkill = common.getChildByPath("soulmpCon") != null;
            ret.maxLevel = MapleDataTool.getInt("maxLevel", common, 1); //10 just a failsafe, shouldn't actually happens
            ret.psdDamR = MapleDataTool.getString("damR", common, "");
            ret.targetPlus = MapleDataTool.getString("targetPlus", common, "");
            ret.trueMax = ret.maxLevel + (ret.combatOrders ? 2 : 0);
            for (int i = 1; i <= ret.trueMax; i++) {
                ret.effects.add(MapleStatEffectFactory.loadSkillEffectFromData(common, id, isBuff, i, "x", ret.notRemoved));
            }
            ret.maxDamageOver = MapleDataTool.getInt("MDamageOver", common, 999999);
        } else {
            for (MapleData leve : data.getChildByPath("level")) {
                ret.effects.add(MapleStatEffectFactory.loadSkillEffectFromData(leve, id, isBuff, Byte.parseByte(leve.getName()), null, ret.notRemoved));
            }
            ret.maxLevel = ret.effects.size();
            ret.trueMax = ret.effects.size();
        }
        boolean loadPvpSkill = false;
        if (loadPvpSkill) {
            MapleData level2 = data.getChildByPath("PVPcommon");
            if (level2 != null) {
                ret.pvpEffects = new ArrayList<>();
                for (int i = 1; i <= ret.trueMax; i++) {
                    ret.pvpEffects.add(MapleStatEffectFactory.loadSkillEffectFromData(level2, id, isBuff, i, "x", ret.notRemoved));
                }
            }
        }
        MapleData reqDataRoot = data.getChildByPath("req");
        if (reqDataRoot != null) {
            for (MapleData reqData : reqDataRoot.getChildren()) {
                ret.requiredSkill.add(new Pair<>(reqData.getName(), (byte) MapleDataTool.getInt(reqData, 1)));
            }
        }
        ret.animationTime = 0;
        if (effect != null) {
            for (MapleData effectEntry : effect) {
                ret.animationTime += MapleDataTool.getIntConvert("delay", effectEntry, 0);
            }
        }
        ret.buffSkill = isBuff;
        switch (id) {
            case 夜光.耀眼光球:
            case 夜光.黑暗降临:
            case 夜光.光明黑暗魔法强化:
                ret.masterLevel = ret.maxLevel;
                break;
        }

        MapleData growthInfo = data.getChildByPath("growthInfo/level");
        if (growthInfo != null) {
            for (MapleData expData : growthInfo.getChildren()) {
                ret.bonusExpInfo.put(Integer.parseInt(expData.getName()), MapleDataTool.getInt("maxExp", expData, 100000000));
            }
        }
        return ret;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public MapleStatEffect getEffect(int level) {
        return getEffect(false, level);
    }

    public MapleStatEffect getPVPEffect(int level) {
        return getEffect(true, level - 1);
    }

    private MapleStatEffect getEffect(boolean ispvp, int level) {
        List<MapleStatEffect> effects = ispvp ? pvpEffects : this.effects;
        if (effects.size() < level) {
            if (effects.size() > 0) { //incAllskill
                return effects.get(effects.size() - 1);
            }
            return null;
        } else if (level <= 0) {
            return effects.get(0);
        }
        return effects.get(level - 1);
    }

    public int getSkillType() {
        return skillType;
    }

    public List<Integer> getAllAnimation() {
        return animation;
    }

    public int getAnimation() {
        if (animation == null) {
            return -1;
        }
        return animation.get(Randomizer.nextInt(animation.size()));
    }

    public void setAnimation(List<Integer> animation) {
        this.animation = animation;
    }

    public int getPsdSkill() {
        return psdSkill;
    }

    public int getPsd() {
        return psd;
    }

    public String getPsdDamR() {
        return psdDamR;
    }

    public String getTargetPlus() {
        return targetPlus;
    }

    public boolean isPVPDisabled() {
        return pvpDisabled;
    }

    public boolean isChargeSkill() {
        return chargeSkill;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public boolean isNotRemoved() {
        return notRemoved;
    }

    public boolean isRapidAttack() {
        return rapidAttack;
    }

    public boolean isPassiveSkill() {
        return passiveSkill;
    }

    public boolean isChargingSkill() {
        return chargingSkill;
    }

    public boolean hasRequiredSkill() {
        return requiredSkill.size() > 0;
    }

    public List<Pair<String, Byte>> getRequiredSkills() {
        return requiredSkill;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getTrueMax() {
        return trueMax;
    }

    public boolean combatOrders() {
        return combatOrders;
    }

    public boolean canBeLearnedBy(int job) {
        int skillForJob = id / 10000;
        if (skillForJob == 2001) {
            return JobConstants.is龙神(job);
        } else if (skillForJob == 0) {
            return JobConstants.is冒险家(job);
        } else if (skillForJob == 500) {
            return JobConstants.is拳手(job) || JobConstants.is火枪手(job);
        } else if (skillForJob == 501) {
            return JobConstants.is火炮手(job);
        } else if (skillForJob == 508) {
            return JobConstants.is龙的传人(job);
        } else if (skillForJob == 509) {
            return JobConstants.is拳手新(job) || JobConstants.is火枪手新(job);
        } else if (skillForJob == 1000) {
            return JobConstants.is骑士团(job);
        } else if (skillForJob == 2000) {
            return JobConstants.is战神(job);
        } else if (skillForJob == 2002) {
            return JobConstants.is双弩精灵(job);
        } else if (skillForJob == 2003) {
            return JobConstants.is幻影(job);
        } else if (skillForJob == 2004) {
            return JobConstants.is夜光(job);
        } else if (skillForJob == 2500) {
            return JobConstants.is隐月(job);
        } else if (skillForJob == 3000) {
            return JobConstants.is反抗者(job);
        } else if (skillForJob == 3001) {
            return JobConstants.is恶魔猎手(job);
        } else if (skillForJob == 3002) {
            return JobConstants.is尖兵(job);
        } else if (skillForJob == 5000) {
            return JobConstants.is米哈尔(job);
        } else if (skillForJob == 6000) {
            return JobConstants.is狂龙战士(job);
        } else if (skillForJob == 6001) {
            return JobConstants.is爆莉萌天使(job);
        } else if (skillForJob == 10000) {
            return JobConstants.is神之子(job);
        } else if (skillForJob == 11000) {
            return JobConstants.is林之灵(job);
        } else if (job / 100 != skillForJob / 100) { // wrong job
            return false;
        } else if (job / 1000 != skillForJob / 1000) { // wrong job
            return false;
        } else if (JobConstants.is林之灵(skillForJob) && !JobConstants.is林之灵(job)) {
            return false;
        } else if (JobConstants.is神之子(skillForJob) && !JobConstants.is神之子(job)) {
            return false;
        } else if (JobConstants.is爆莉萌天使(skillForJob) && !JobConstants.is爆莉萌天使(job)) {
            return false;
        } else if (JobConstants.is狂龙战士(skillForJob) && !JobConstants.is狂龙战士(job)) {
            return false;
        } else if (JobConstants.is米哈尔(skillForJob) && !JobConstants.is米哈尔(job)) {
            return false;
        } else if (JobConstants.is尖兵(skillForJob) && !JobConstants.is尖兵(job)) {
            return false;
        } else if (JobConstants.is夜光(skillForJob) && !JobConstants.is夜光(job)) {
            return false;
        } else if (JobConstants.is隐月(skillForJob) && !JobConstants.is隐月(job)) {
            return false;
        } else if (JobConstants.is幻影(skillForJob) && !JobConstants.is幻影(job)) {
            return false;
        } else if (JobConstants.is龙的传人(skillForJob) && !JobConstants.is龙的传人(job)) {
            return false;
        } else if (JobConstants.is火炮手(skillForJob) && !JobConstants.is火炮手(job)) {
            return false;
        } else if (JobConstants.is拳手(skillForJob) && !JobConstants.is拳手(job)) {
            return false;
        } else if (JobConstants.is火枪手(skillForJob) && !JobConstants.is火枪手(job)) {
            return false;
        } else if (JobConstants.is拳手新(skillForJob) && !JobConstants.is拳手新(job)) {
            return false;
        } else if (JobConstants.is火枪手新(skillForJob) && !JobConstants.is火枪手新(job)) {
            return false;
        } else if (JobConstants.is恶魔复仇者(skillForJob) && !JobConstants.is恶魔复仇者(job)) {
            return false;
        } else if (JobConstants.is恶魔猎手(skillForJob) && !JobConstants.is恶魔猎手(job)) {
            return false;
        } else if (JobConstants.is冒险家(skillForJob) && !JobConstants.is冒险家(job)) {
            return false;
        } else if (JobConstants.is骑士团(skillForJob) && !JobConstants.is骑士团(job)) {
            return false;
        } else if (JobConstants.is战神(skillForJob) && !JobConstants.is战神(job)) {
            return false;
        } else if (JobConstants.is龙神(skillForJob) && !JobConstants.is龙神(job)) {
            return false;
        } else if (JobConstants.is双弩精灵(skillForJob) && !JobConstants.is双弩精灵(job)) {
            return false;
        } else if (JobConstants.is反抗者(skillForJob) && !JobConstants.is反抗者(job)) {
            return false;
        } else if (JobConstants.is超能力者(skillForJob) && !JobConstants.is超能力者(job)) {
            return false;
        } else if ((job / 10) % 10 == 0 && (skillForJob / 10) % 10 > (job / 10) % 10) { // wrong 2nd job
            return false;
        } else if ((skillForJob / 10) % 10 != 0 && (skillForJob / 10) % 10 != (job / 10) % 10) { //wrong 2nd job
            return false;
        } else if (skillForJob % 10 > job % 10) { // wrong 3rd/4th job
            return false;
        }
        return true;
    }

    public boolean isTimeLimited() {
        return timeLimited;
    }

    public boolean isFourthJob() {
        if (id / 10000 == 11212) {
            return false;
        }
        if (isHyperSkill()) {
            return true;
        }
        switch (id) {
            case 英雄.战斗精通:
            case 黑骑士.灵魂复仇:
            case 冲锋队长.反制攻击:
            case 冲锋队长.双幸运骰子:
            case 船长.反制攻击:
            case 船长.双幸运骰子:
            case 双弩.旋转月瀑坠击:
            case 双弩.进阶急袭双杀:
            case 双弩.勇士的意志:
            case 双刀.武器用毒液:
            case 双刀.锋利:
            case 双刀.致命毒液:
            case 神射手.射术精修:
            case 豹弩游侠.野性本能:
            case 神炮王.勇士的意志:
            case 战神.迅捷移动:
            case 战神.精准动态Ⅱ:
            case 战神.重击研究Ⅱ:
            case 战神.勇士的意志:
            case 龙神.勇士的意志:
            case 龙神.玛瑙的意志:
            case 米哈尔.战斗精通:
                return false;
        }
        switch (id / 10000) {
            case 2312:
            case 2412:
            case 2712:
            case 3122:
            case 6112:
            case 6512:
            case 14212:
                return true;
            case 10100:
                return id == 神之子.进阶狂蛮撞击;
            case 10110:
                return id == 神之子.进阶神剑陨落 || id == 神之子.进阶旋卷切割;
            case 10111:
                return id == 神之子.进阶圆月旋风 || id == 神之子.进阶狂转回旋 || id == 神之子.进阶旋跃斩;
            case 10112:
                return id == 神之子.进阶地裂山崩 || id == 神之子.进阶暴风旋涡;
        }
        if ((getMaxLevel() <= 15 && !invisible && getMasterLevel() <= 0)) {
            return false;
        }
        if (JobConstants.is龙神(id / 10000) && id / 10000 < 3000) { //龙神技能
            return ((id / 10000) % 10) >= 7;
        }
        if (id / 10000 >= 430 && id / 10000 <= 434) { //暗影双刀技能
            return ((id / 10000) % 10) == 4 || getMasterLevel() > 0;
        }
        return ((id / 10000) % 10) == 2 && id < 90000000 && !isBeginnerSkill();
    }

    public Element getElement() {
        return element;
    }

    public int getAnimationTime() {
        return animationTime;
    }

    public boolean getDisable() {
        return disable;
    }

    public int getFixLevel() {
        return this.fixLevel;
    }

    public int getMasterLevel() {
        return masterLevel;
    }

    public int getDisableNextLevelInfo() {
        return this.disableNextLevelInfo;
    }

    public int getDelay() {
        return delay;
    }

    public int getTamingMob() {
        return eventTamingMob;
    }

    public int getHyper() {
        return hyper;
    }

    public int getReqLevel() {
        return reqLev;
    }

    public int getMaxDamageOver() {
        return maxDamageOver;
    }

    public int getBonusExpInfo(int level) {
        if (bonusExpInfo.isEmpty()) {
            return -1;
        }
        if (bonusExpInfo.containsKey(level)) {
            return bonusExpInfo.get(level);
        }
        return -1;
    }

    public Map<Integer, Integer> getBonusExpInfo() {
        return bonusExpInfo;
    }

    public boolean isMagic() {
        return magic;
    }

    public boolean isMovement() {
        return casterMove;
    }

    public boolean isPush() {
        return pushTarget;
    }

    public boolean isPull() {
        return pullTarget;
    }

    public boolean isBuffSkill() {
        return buffSkill;
    }

    public boolean isSummonSkill() {
        return isSummon;
    }

    public boolean isNonAttackSummon() {
        return isSummon && minionAttack.isEmpty() && (minionAbility.isEmpty() || minionAbility.equals("taunt"));
    }

    public boolean isNonExpireSummon() {
        return selfDestructMinion;
    }

    public boolean isHyperSkill() {
        return hyper > 0 && reqLev > 0;
    }

    public boolean isHyperStat() {
        return hyperStat > 0;
    }

    /**
     * @return 家族技能
     */
    public boolean isGuildSkill() {
        int jobId = id / 10000;
        return jobId == 9100;
    }

    /**
     * 新手技能
     */
    public boolean isBeginnerSkill() {
        int jobId = id / 10000;
        return JobConstants.is新手职业(jobId);
    }

    /**
     * 管理员技能
     */
    public boolean isAdminSkill() {
        int jobId = id / 10000;
        return jobId == 800 || jobId == 900;
    }

    /**
     * 内在能力技能
     */
    public boolean isInnerSkill() {
        int jobId = id / 10000;
        return jobId == 7000;
    }

    /**
     * 特殊技能
     */
    public boolean isSpecialSkill() {
        int jobId = id / 10000;
        return jobId == 7000 || jobId == 7100 || jobId == 8000 || jobId == 9000 || jobId == 9100 || jobId == 9200 || jobId == 9201 || jobId == 9202 || jobId == 9203 || jobId == 9204;
    }

    public int getSkillByJobBook() {
        return getSkillByJobBook(id);
    }

    public int getSkillByJobBook(int skillid) {
        switch (skillid / 10000) {
            case 112:
            case 122:
            case 132:
            case 212:
            case 222:
            case 232:
            case 312:
            case 322:
            case 412:
            case 422:
            case 512:
            case 522:
                return 4;
            case 111:
            case 121:
            case 131:
            case 211:
            case 221:
            case 231:
            case 311:
            case 321:
            case 411:
            case 421:
            case 511:
            case 521:
                return 3;
            case 110:
            case 120:
            case 130:
            case 210:
            case 220:
            case 230:
            case 310:
            case 320:
            case 410:
            case 420:
            case 510:
            case 520:
                return 2;
            case 100:
            case 200:
            case 300:
            case 400:
            case 500:
                return 1;
        }
        return -1;
    }

    /**
     * 是否宠物被动触发技能
     */
    public boolean isPetPassive() {
        return petPassive;
    }

    /**
     * 触发技能的套装ID
     */
    public int getSetItemReason() {
        return setItemReason;
    }

    /**
     * 触发技能的套装需要的件数
     */
    public int geSetItemPartsCount() {
        return setItemPartsCount;
    }

    /**
     * 是否是开关技能
     *
     * @return
     */
    public boolean isSwitch() {
        return isSwitch;
    }

    /*
         * 种族特性本能技能
         */
    public boolean isTeachSkills() {
        return SkillConstants.isTeachSkills(id);
    }

    /*
     * 链接技能技能
     */
    public boolean isLinkSkills() {
        return SkillConstants.isLinkSkills(id);
    }

    public boolean is老技能() {
        switch (id) {
            //--------魂骑士1转---------
            case 11000005: //HP增加
            case 11000006: //守护者之甲
            case 11001001: //圣甲术
            case 11001002: //强力攻击
            case 11001003: //群体攻击
            case 11001004: //魂精灵
                //--------魂骑士2转---------
            case 11100000: //精准剑
            case 11100007: //物理训练
            case 11101001: //快速剑
            case 11101002: //终极剑
            case 11101003: //愤怒之火
            case 11101004: //灵魂之刃
            case 11101005: //灵魂迅移
            case 11101006: //伤害反射
            case 11101008: //轻舞飞扬
                //--------魂骑士3转---------
            case 11110000: //自我恢复
            case 11110005: //进阶斗气
            case 11111001: //斗气集中
            case 11111002: //恐慌
            case 11111003: //昏迷
            case 11111004: //勇猛劈砍
            case 11111006: //灵魂突刺
            case 11111007: //闪耀冲击
            case 11111008: //魔法碰撞
                //--------风灵使者1转---------
            case 13000000: //强力箭
            case 13000001: //弓箭手精通
            case 13000005: //自然吸收
            case 13001003: //二连射
            case 13001004: //风精灵
                //--------风灵使者2转---------
            case 13100000: //精准弓
            case 13100008: //物理训练
            case 13101001: //快速箭
            case 13101002: //终极弓
            case 13101003: //无形箭
            case 13101004: //二阶跳
            case 13101005: //尖刺风暴
            case 13101006: //风影漫步
            case 13101007: //箭扫射
                //--------风灵使者3转---------
            case 13110003: //神箭手
            case 13110008: //闪避
            case 13110009: //致命一击
            case 13111000: //箭雨
            case 13111001: //集中精力
            case 13111002: //暴风箭雨
            case 13111004: //替身术
            case 13111005: //信天翁
            case 13111006: //风灵穿越
            case 13111007: //疾风扫射
                //--------奇袭者1转---------
            case 15000000: //快动作
            case 15000005: //幸运一击
            case 15000006: //致命咆哮
            case 15000008: //HP增加
            case 15001002: //半月踢
            case 15001003: //疾驰
            case 15001004: //雷精灵
            case 15001007: //未知技能
                //--------奇袭者2转---------
            case 15100001: //精准拳
            case 15100004: //能量获得
            case 15100009: //物理训练
            case 15101002: //急速拳
            case 15101003: //贯骨击
            case 15101005: //能量爆破
            case 15101008: //静心
            case 15101010: //龙卷风拳
                //--------奇袭者3转---------
            case 15110009: //致命狂热
            case 15110010: //迷惑攻击
            case 15111004: //激怒拳
            case 15111005: //极速领域
            case 15111006: //闪光击
            case 15111007: //鲨鱼波
            case 15111008: //能量爆炸
            case 15111011: //幸运骰子
            case 15111012: //碎石乱击
                return true;
        }
        return false;
    }

    public boolean isAngelSkill() {
        return SkillConstants.is天使祝福戒指(id);
    }

    public boolean isLinkedAttackSkill() {
        return SkillConstants.isLinkedAttackSkill(id);
    }

    public boolean isDefaultSkill() {
        return getFixLevel() > 0;
    }

    public int getPPRecovery() {
        return ppRecovery;
    }

    public boolean isSoulSkill() {
        return soulSkill;
    }

    public void setSoulSkill(boolean soulSkill) {
        this.soulSkill = soulSkill;
    }

    private enum SkillType {

        BUFF_ICO(10),
        PASSIVE(30, 31, 50, 51),//不同類型的被動技能
        PASSIVE_TRUE(50),//唯一类型的被动技能
        MONSTER_DEBUFF(32),//怪物異常效果技能
        SPAWN_OBJECT(33),//基本上全是召喚類技能
        MONSTER_DEBUFF_OR_CANCEL(34),//用於取消怪物特定效果的技能
        SINGLE_EFFECT(35),//非攻擊技能 楓葉淨化
        PROTECTIVE_MIST(36),//在地圖中召喚中特定的技能效果 如（煙幕彈）
        RESURRECT(38),//復活玩家技能
        MOVEMENT(40),//移動相關技能
        MOVEMENT_RANDOM(42),//龍之氣息 隨便移動到地圖上某個地方
        KEY_COMBO_ATTACK(52),//連擊技能
        COVER_SKILL(98),//雙重攻擊 終極攻擊 超級體 重裝武器精通 猴子衝擊
        ;//效果分類是特別奇怪的··但基本上都是 >= 10 (不包含上述聲明)
        final int[] vals;

        SkillType(int... vals) {
            this.vals = vals;
        }
    }
}
