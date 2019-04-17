package server;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleDisease;
import client.MapleTraitType;
import client.inventory.MapleInventoryType;
import client.status.MonsterStatus;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.SkillConstants;
import constants.skills.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataTool;
import provider.MapleDataType;
import provider.MapleOverrideData;
import tools.CaltechEval;
import tools.Pair;
import tools.Randomizer;
import tools.Triple;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapleStatEffectFactory {

    static final Logger log = LogManager.getLogger("client.skills");

    /*
         * 加载技能的BUFF状态
         */
    public static MapleStatEffect loadSkillEffectFromData(MapleData source, int skillid, boolean overtime, int level, String variables, boolean notRemoved) {
        return loadFromData(source, skillid, true, overtime, level, variables, notRemoved);
    }

    /*
         * 加载道具的BUFF状态
         */
    public static MapleStatEffect loadItemEffectFromData(MapleData source, int itemid) {
        return loadFromData(source, itemid, false, false, 1, null, false);
    }

    /*
         * 添加一些常用但BUFF的参数不为0的BUFF状态信息
         */
    private static void addBuffStatPairToListIfNotZero(Map<MapleBuffStat, Integer> list, MapleBuffStat buffstat, Integer val, int id, int level) {
        if (val != 0) {
            list.put(buffstat, val);
            //将部分Buff技能获取出来!
            if (buffstat.isShow() && level == 1) {
                log.error("找到技能或者道具ID:" + id + " Val:" + buffstat.name());
            }
        }

    }

    public static int parseEval(String data, int level) {
        String variables = "x";
        String dddd = data.toLowerCase().replace(variables, String.valueOf(level));
        if (dddd.substring(0, 1).equals("-")) { //-30+3*x
            if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { //-u(x/2)
                dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; //n(u(x/2))
            } else {
                dddd = "n" + dddd.substring(1, dddd.length()); //n30+3*x
            }
        } else if (dddd.substring(0, 1).equals("=")) { //lol nexon and their mistakes
            dddd = dddd.substring(1, dddd.length());
        }
        return (int) (new CaltechEval(dddd).evaluate());
    }

    private static int parseEval(String path, MapleData source, int def, String variables, int level) {
        return parseEval(path, source, def, variables, level, "");
    }

    private static int parseEval(String path, MapleData source, int def, String variables, int level, String d) {
        if (variables == null) {
            return MapleDataTool.getIntConvert(path, source, def);
        } else {
            String dddd;
            if (d.isEmpty()) {
                MapleData dd = source.getChildByPath(path);
                if (dd == null) {
                    return def;
                }
                if (dd.getType() != MapleDataType.STRING) {
                    return MapleDataTool.getIntConvert(path, source, def);
                }
                dddd = MapleDataTool.getString(dd).toLowerCase();
            } else {
                dddd = d;
            }
            dddd = dddd.replace(variables, String.valueOf(level));
            if (dddd.substring(0, 1).equals("-")) { //-30+3*x
                if (dddd.substring(1, 2).equals("u") || dddd.substring(1, 2).equals("d")) { //-u(x/2)
                    dddd = "n(" + dddd.substring(1, dddd.length()) + ")"; //n(u(x/2))
                } else {
                    dddd = "n" + dddd.substring(1, dddd.length()); //n30+3*x
                }
            } else if (dddd.substring(0, 1).equals("=")) { //lol nexon and their mistakes
                dddd = dddd.substring(1, dddd.length());
            } else if (dddd.endsWith("y")) {
                dddd = dddd.substring(4, dddd.length()).replace("y", String.valueOf(level));
            } else if (dddd.contains("%")) {
                dddd = dddd.replace("%", "/100");
            }
            return (int) (new CaltechEval(dddd).evaluate());
        }
    }

    private static MapleStatEffect loadFromData(MapleData source, int sourceid, boolean skill, boolean overTime, int level, String variables, boolean notRemoved) {
        MapleStatEffect ret = new MapleStatEffect();
        ret.setSourceid(sourceid);
        ret.setSkill(skill);
        ret.setLevel((byte) level);

        if (source == null) {
            return ret;
        }

        EnumMap<MapleStatInfo, Integer> info = new EnumMap<>(MapleStatInfo.class);
        MapleOverrideData moi = MapleOverrideData.getInstance();
        for (MapleStatInfo i : MapleStatInfo.values()) {
            if (i.isSpecial()) {
                info.put(i, parseEval(i.name().substring(0, i.name().length() - 1), source, i.getDefault(), variables, level, moi.getOverrideValue(sourceid, i.name())));
            } else {
                try {
                    info.put(i, parseEval(i.name(), source, i.getDefault(), variables, level, moi.getOverrideValue(sourceid, i.name())));
                } catch (Exception e) {
                    System.out.println("sourceid:" + sourceid);
                }

            }
        }
        ret.setInfo(info);
        ret.setHpR(parseEval("hpR", source, 0, variables, level, moi.getOverrideValue(sourceid, "hpR")) / 100.0);
        ret.setMpR(parseEval("mpR", source, 0, variables, level, moi.getOverrideValue(sourceid, "mpR")) / 100.0);
        ret.setIgnoreMob((short) parseEval("ignoreMobpdpR", source, 0, variables, level, moi.getOverrideValue(sourceid, "ignoreMobpdpR")));
        ret.setThaw((short) parseEval("thaw", source, 0, variables, level));
        ret.setInterval(parseEval("interval", source, 0, variables, level));
        ret.setExpinc(parseEval("expinc", source, 0, variables, level));
        ret.setExp(parseEval("exp", source, 0, variables, level, moi.getOverrideValue(sourceid, "exp")));
        ret.setMorphId(parseEval("morph", source, 0, variables, level, moi.getOverrideValue(sourceid, "morph")));
        ret.setCosmetic(parseEval("cosmetic", source, 0, variables, level));
        ret.setSlotCount((byte) parseEval("slotCount", source, 0, variables, level)); //矿(药)背包道具需要
        ret.setSlotPerLine((byte) parseEval("slotPerLine", source, 0, variables, level)); //矿(药)背包道具需要
        ret.setPreventslip((byte) parseEval("preventslip", source, 0, variables, level));
        ret.setUseLevel((short) parseEval("useLevel", source, 0, variables, level));
        ret.setImmortal((byte) parseEval("immortal", source, 0, variables, level));
        ret.setType((byte) parseEval("type", source, 0, variables, level));
        ret.setBs((byte) parseEval("bs", source, 0, variables, level));
        ret.setIndiePdd((short) parseEval("indiePdd", source, 0, variables, level, moi.getOverrideValue(sourceid, "indiePdd")));
        ret.setIndieMdd((short) parseEval("indieMdd", source, 0, variables, level, moi.getOverrideValue(sourceid, "indieMdd")));
        ret.setExpBuff(parseEval("expBuff", source, 0, variables, level, moi.getOverrideValue(sourceid, "expBuff")));
        ret.setCashup(parseEval("cashBuff", source, 0, variables, level, moi.getOverrideValue(sourceid, "cashBuff")));
        ret.setItemup(parseEval("itemupbyitem", source, 0, variables, level));
        ret.setMesoup(parseEval("mesoupbyitem", source, 0, variables, level));
        ret.setBerserk(parseEval("berserk", source, 0, variables, level, moi.getOverrideValue(sourceid, "berserk")));
        ret.setBerserk2(parseEval("berserk2", source, 0, variables, level, moi.getOverrideValue(sourceid, "berserk2")));
        ret.setBooster(parseEval("booster", source, 0, variables, level, moi.getOverrideValue(sourceid, "booster")));
        ret.setLifeId((short) parseEval("lifeId", source, 0, variables, level));
        ret.setInflation((short) parseEval("inflation", source, 0, variables, level));
        ret.setImhp((short) parseEval("imhp", source, 0, variables, level, moi.getOverrideValue(sourceid, "imhp")));
        ret.setImmp((short) parseEval("immp", source, 0, variables, level, moi.getOverrideValue(sourceid, "immp")));
        ret.setIllusion(parseEval("illusion", source, 0, variables, level));
        ret.setConsumeOnPickup(parseEval("consumeOnPickup", source, 0, variables, level));
        if (ret.getConsumeOnPickup() == 1) {
            if (parseEval("party", source, 0, variables, level) > 0) {
                ret.setConsumeOnPickup(2);
            }
        }
        ret.setRecipe(parseEval("recipe", source, 0, variables, level));
        ret.setRecipeUseCount((byte) parseEval("recipeUseCount", source, 0, variables, level));
        ret.setRecipeValidDay((byte) parseEval("recipeValidDay", source, 0, variables, level));
        ret.setReqSkillLevel((byte) parseEval("reqSkillLevel", source, 0, variables, level));
        ret.setEffectedOnAlly((byte) parseEval("effectedOnAlly", source, 0, variables, level));
        ret.setEffectedOnEnemy((byte) parseEval("effectedOnEnemy", source, 0, variables, level));
//        ret.incPVPdamage = (short) parseEval("incPVPDamage", source, 0, variables, level);
        ret.setMoneyCon(parseEval("moneyCon", source, 0, variables, level));
        ret.setMoveTo(parseEval("moveTo", source, -1, variables, level));
//        ret.repeatEffect = ret.is战法灵气(); //自动重复使用的BUFF

        int charColor = 0;
        String cColor = MapleDataTool.getString("charColor", source, null);
        if (cColor != null) {
            charColor |= Integer.parseInt("0x" + cColor.substring(0, 2));
            charColor |= Integer.parseInt("0x" + cColor.substring(2, 4) + "00");
            charColor |= Integer.parseInt("0x" + cColor.substring(4, 6) + "0000");
            charColor |= Integer.parseInt("0x" + cColor.substring(6, 8) + "000000");
        }
        ret.setCharColor(charColor);
        EnumMap<MapleTraitType, Integer> traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            int expz = parseEval(t.name() + "EXP", source, 0, variables, level);
            if (expz != 0) {
                traits.put(t, expz);
            }
        }
        ret.setTraits(traits);
        List<MapleDisease> cure = new ArrayList<>(5);
        if (parseEval("poison", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.中毒);
        }
        if (parseEval("seal", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.封印);
        }
        if (parseEval("darkness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.黑暗);
        }
        if (parseEval("weakness", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.虚弱);
        }
        if (parseEval("curse", source, 0, variables, level) > 0) {
            cure.add(MapleDisease.诅咒);
        }
        ret.setCureDebuffs(cure);
        List<Integer> petsCanConsume = new ArrayList<>();
        for (int i = 0; true; i++) {
            int dd = parseEval(String.valueOf(i), source, 0, variables, level);
            if (dd > 0) {
                petsCanConsume.add(dd);
            } else {
                break;
            }
        }
        ret.setPetsCanConsume(petsCanConsume);
        MapleData mdd = source.getChildByPath("0");
        if (mdd != null && mdd.getChildren().size() > 0) {
            ret.setMobSkill((short) parseEval("mobSkill", mdd, 0, variables, level));
            ret.setMobSkillLevel((short) parseEval("level", mdd, 0, variables, level));
        } else {
            ret.setMobSkill((short) 0);
            ret.setMobSkillLevel((short) 0);
        }
        MapleData pd = source.getChildByPath("randomPickup");
        if (pd != null) {
            ArrayList<Integer> randomPickup = new ArrayList<>();
            for (MapleData p : pd) {
                randomPickup.add(MapleDataTool.getInt(p));
            }
            ret.setRandomPickup(randomPickup);
        }
        MapleData ltd = source.getChildByPath("lt");
        if (ltd != null) {
            ret.setLt((Point) ltd.getData());
            ret.setRb((Point) source.getChildByPath("rb").getData());
        }
        MapleData ltc = source.getChildByPath("con");
        if (ltc != null) {
            List<Pair<Integer, Integer>> availableMap = new ArrayList<>();
            for (MapleData ltb : ltc) {
                availableMap.add(new Pair<>(MapleDataTool.getInt("sMap", ltb, 0), MapleDataTool.getInt("eMap", ltb, 999999999)));
            }
            ret.setAvailableMap(availableMap);
        }
        int totalprob = 0;
        MapleData lta = source.getChildByPath("reward");
        if (lta != null) {
            ret.setRewardMeso(parseEval("meso", lta, 0, variables, level));
            MapleData ltz = lta.getChildByPath("case");
            if (ltz != null) {
                ArrayList<Triple<Integer, Integer, Integer>> rewardItem = new ArrayList<>();
                for (MapleData lty : ltz) {
                    rewardItem.add(new Triple<>(MapleDataTool.getInt("id", lty, 0), MapleDataTool.getInt("count", lty, 0), MapleDataTool.getInt("prop", lty, 0)));
                    totalprob += MapleDataTool.getInt("prob", lty, 0);
                }
                ret.setRewardItem(rewardItem);
            }
        } else {
            ret.setRewardMeso(0);
        }
        ret.setTotalprob(totalprob);
        // start of server calculated stuffs
        if (ret.isSkill()) {
            int priceUnit = ret.getInfo().get(MapleStatInfo.priceUnit); // Guild skills
            if (priceUnit > 0) {
                int price = ret.getInfo().get(MapleStatInfo.price);
                int extendPrice = ret.getInfo().get(MapleStatInfo.extendPrice);
                ret.getInfo().put(MapleStatInfo.price, price * priceUnit);
                ret.getInfo().put(MapleStatInfo.extendPrice, extendPrice * priceUnit);
            }
            switch (sourceid) {
                case 英雄.终极剑斧:
                case 圣骑士.终极剑钝器:
                case 黑骑士.终极枪矛:
                case 神射手.终极弓:
                case 箭神.终极弩:
                case 火毒.快速移动精通:
                case 冰雷.快速移动精通:
                case 主教.快速移动精通:
                case 唤灵斗师.黑暗闪电:
                case 豹弩游侠.终极弓弩:
                case 豹弩游侠.进阶终极攻击: //V.100新增
                case 龙神.飞龙闪:
                case 龙神.玛瑙的意志:
                case 英雄.进阶终极攻击:
                case 神射手.进阶终极攻击:
                case 双弩.终极双弩枪:
                case 双弩.进阶终极攻击:
                case 战神.终极矛: //V.100新增
                case 战神.进阶终极攻击: //V.100新增
                case 豹弩游侠.召唤美洲豹_灰:
                case 豹弩游侠.召唤美洲豹_黄:
                case 豹弩游侠.召唤美洲豹_红:
                case 豹弩游侠.召唤美洲豹_紫:
                case 豹弩游侠.召唤美洲豹_蓝:
                case 豹弩游侠.召唤美洲豹_剑:
                case 豹弩游侠.召唤美洲豹_雪:
                case 豹弩游侠.召唤美洲豹_玛瑙:
                case 豹弩游侠.召唤美洲豹_铠甲:
                    ret.getInfo().put(MapleStatInfo.mobCount, 6);
                    break;
                case 机械师.金属机甲_战车:
                case 幻影.卡片雪舞:
                case 幻影.黑色秘卡:
                    ret.getInfo().put(MapleStatInfo.attackCount, 6);
                    ret.getInfo().put(MapleStatInfo.bulletCount, 6);
                    break;
                case 恶魔复仇者.强化超越:
                    ret.getInfo().put(MapleStatInfo.attackCount, 2);
                    break;
                case 夜光.仙女发射:
                    ret.getInfo().put(MapleStatInfo.attackCount, 4);
                    break;
                case 尖兵.精准火箭:
                    ret.getInfo().put(MapleStatInfo.attackCount, 4);
                    break;
                case 狂龙战士.剑刃之壁:
                case 狂龙战士.剑刃之壁_变身:
                    ret.getInfo().put(MapleStatInfo.attackCount, 3);
                    break;
                case 狂龙战士.进阶剑刃之壁:
                case 狂龙战士.进阶剑刃之壁_变身:
                    ret.getInfo().put(MapleStatInfo.attackCount, 5);
                    break;
                case 风灵使者.狂风肆虐Ⅰ:
                case 风灵使者.狂风肆虐Ⅱ:
                case 风灵使者.狂风肆虐Ⅲ:
                case 风灵使者.暴风灭世:
                    ret.getInfo().put(MapleStatInfo.attackCount, 6);
                    break;
            }
//            if (GameConstants.isNoDelaySkill(sourceid)) {
//                ret.getInfo().put(MapleStatInfo.mobCount, 6);
//            }
        }
        if (!ret.isSkill() && ret.getInfo().get(MapleStatInfo.time) > -1) {
            ret.setOverTime(true);
        } else {
            ret.getInfo().put(MapleStatInfo.time, (ret.getInfo().get(MapleStatInfo.time) * 1000)); // items have their times stored in ms, of course
            ret.getInfo().put(MapleStatInfo.subTime, (ret.getInfo().get(MapleStatInfo.subTime) * 1000));
            ret.setOverTime(overTime || ret.isMorph() || ret.is戒指技能() || ret.getSummonMovementType() != null);
            ret.setNotRemoved(notRemoved);
        }
        Map<MonsterStatus, Integer> monsterStatus = new LinkedHashMap<>();
        EnumMap<MapleBuffStat, Integer> statups = new EnumMap<>(MapleBuffStat.class);
        if (ret.isOverTime() && ret.getSummonMovementType() == null && !ret.is能量获得()) {
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.物理攻击, ret.getInfo().get(MapleStatInfo.pad), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.物理防御, ret.getInfo().get(MapleStatInfo.pdd), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.提升物理防御力, ret.getInfo().get(MapleStatInfo.indiePdd), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.魔法攻击, ret.getInfo().get(MapleStatInfo.mad), ret.getSourceid(), ret.getLevel());
//            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.魔法防御, ret.getInfo().get(MapleStatInfo.mdd), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.命中率, ret.getInfo().get(MapleStatInfo.acc), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.回避率, ret.getInfo().get(MapleStatInfo.eva), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.移动速度, sourceid == 唤灵斗师.黄色灵气 ? ret.getInfo().get(MapleStatInfo.x) : ret.getInfo().get(MapleStatInfo.speed), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.跳跃力, ret.getInfo().get(MapleStatInfo.jump), ret.getSourceid(), ret.getLevel());

            if (sourceid != 豹弩游侠.暴走形态) { //龙神的这个技能是被动加的HP上限 所以这个地方就不在加了
                addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MAXHP, ret.getInfo().get(MapleStatInfo.mhpR), ret.getSourceid(), ret.getLevel());
            }
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MAXMP, ret.getInfo().get(MapleStatInfo.mmpR), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.攻击加速, ret.getBooster(), ret.getSourceid(), ret.getLevel());
//            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.HP_LOSS_GUARD, Integer.valueOf(ret.thaw), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.EXPRATE, ret.getExpBuff(), ret.getSourceid(), ret.getLevel()); // 经验
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ACASH_RATE, ret.getCashup(), ret.getSourceid(), ret.getLevel()); // custom
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.DROP_RATE, ItemConstants.getModifier(ret.getSourceid(), ret.getItemup()), ret.getSourceid(), ret.getLevel()); // defaults to 2x
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.MESO_RATE, ItemConstants.getModifier(ret.getSourceid(), ret.getMesoup()), ret.getSourceid(), ret.getLevel()); // defaults to 2x
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.狂暴战魂, ret.getBerserk2(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.ILLUSION, ret.getIllusion(), ret.getSourceid(), ret.getLevel()); //复制克隆BUFF
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.天使状态, ret.getBerserk(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_MAXHP, ret.getInfo().get(MapleStatInfo.emhp), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_MAXMP, ret.getInfo().get(MapleStatInfo.emmp), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_物理攻击, ret.getInfo().get(MapleStatInfo.epad), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_魔法攻击, ret.getInfo().get(MapleStatInfo.emad), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_物理防御, ret.getInfo().get(MapleStatInfo.epdd), ret.getSourceid(), ret.getLevel());
//            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增强_魔法防御, ret.getInfo().get(MapleStatInfo.emdd), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.巨人药水, ret.getInflation(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加力量, ret.getInfo().get(MapleStatInfo.str), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加敏捷, ret.getInfo().get(MapleStatInfo.dex), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加智力, ret.getInfo().get(MapleStatInfo.int_), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加运气, ret.getInfo().get(MapleStatInfo.luk), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加力量, ret.getInfo().get(MapleStatInfo.indieSTR), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加敏捷, ret.getInfo().get(MapleStatInfo.indieDEX), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加智力, ret.getInfo().get(MapleStatInfo.indieINT), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加运气, ret.getInfo().get(MapleStatInfo.indieLUK), ret.getSourceid(), ret.getLevel());

            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加魔法攻击力, ret.getInfo().get(MapleStatInfo.indieMad), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加最大HP, (int) ret.getImhp(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加最大MP, (int) ret.getImmp(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.indieMmp), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加跳跃力, ret.getInfo().get(MapleStatInfo.indieJump), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr), ret.getSourceid(), ret.getLevel());
            if (sourceid != 机械师.金属机甲_人类 && sourceid != 机械师.终极机甲) {
                addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed), ret.getSourceid(), ret.getLevel());
            }

            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加命中值, ret.getInfo().get(MapleStatInfo.indieAcc), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加回避值, ret.getInfo().get(MapleStatInfo.indieEva), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.增加所有属性, ret.getInfo().get(MapleStatInfo.indieAllStat), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.PVP_ATTACK, ret.getInfo().get(MapleStatInfo.PVPdamage), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.INVINCIBILITY, (int) ret.getImmortal(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.NO_SLIP, (int) ret.getPreventslip(), ret.getSourceid(), ret.getLevel());
            addBuffStatPairToListIfNotZero(statups, MapleBuffStat.FAMILIAR_SHADOW, ret.getCharColor() > 0 ? 1 : 0, ret.getSourceid(), ret.getLevel());
        }
        if (ret.isSkill()) {
            switch (sourceid) {
                case 爆莉萌天使.爱星能量: {
                    statups.put(MapleBuffStat.爱星能量, 1);
                    break;
                }
                case 机械师.多重属性_M_FL: {
                    statups.put(MapleBuffStat.呼啸_伤害减少, ret.getInfo().get(MapleStatInfo.z));
                    break;
                }
                case 侠盗.暗影抨击:
                case 侠盗.暗影抨击_1:
                case 侠盗.暗影抨击_2:
                case 侠盗.暗影抨击_3: {
                    ret.getInfo().put(MapleStatInfo.time, 5000);
                    statups.put(MapleBuffStat.暗影抨击, 3);
                    break;
                }
                case 400021017: {
                    statups.put(MapleBuffStat.连环吸血, ret.getInfo().get(MapleStatInfo.y));
                    break;
                }
                case 英雄.燃灵之剑: {
                    statups.put(MapleBuffStat.变换攻击, (int) ret.getLevel());
                    break;
                }
                case 400020002: {
//                    r.bwb.put(a.c.m.Qs, ret.getInfo().get(MapleStatInfo.s)));
                    break;
                }
                case 箭神.真狙击: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_CursorSniping, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 恶魔猎手.恶魔觉醒: {
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndiePMdR, ret.getInfo().get(MapleStatInfo.indiePMdR));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    statups.put(MapleBuffStat.变换攻击, (int) ret.getLevel());
                    break;
                }
                case 冲锋队长.超人变形: {
                    statups.put(MapleBuffStat.超人变形, ret.getInfo().get(MapleStatInfo.w));
                    break;
                }
                case 隐士.多向飞镖: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    statups.put(MapleBuffStat.多向飞镖, 1);
                    break;
                }
                case 400011003: {
                    statups.put(MapleBuffStat.神圣归一, 1);
                    break;
                }
                case 唤灵斗师.结合灵气: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    statups.put(MapleBuffStat.结合灵气, (int) ret.getLevel());
                    statups.put(MapleBuffStat.变换攻击, (int) ret.getLevel());
                    break;
                }
                case 恶魔复仇者.恶魔狂怒: {
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.恶魔狂怒, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 400011016: {
                    statups.put(MapleBuffStat.装备摩诃, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    break;
                }
                case 奇袭者.心雷合一: {
                    statups.put(MapleBuffStat.心雷合一, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 400051006: {
                    statups.put(MapleBuffStat.子弹盛宴, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 主教.祈祷: {
                    statups.put(MapleBuffStat.祈祷, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_DebuffTolerance, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_DotHealHPPerSecond, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndiePMdR, ret.getInfo().get(MapleStatInfo.q));
                    break;
                }
                case 400011000: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    statups.put(MapleBuffStat.灵气武器, (int) ret.getLevel());
                    break;
                }
                case 魔法师.超压魔力: {
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.超压魔力, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 400030002: {
                    ret.getInfo().put(MapleStatInfo.time, 2500);
                    break;
                }
                case 400001001: {
                    statups.put(MapleBuffStat.时空门, 1);
                    break;
                }
                case 400001002: {
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.火眼晶晶, (ret.getInfo().get(MapleStatInfo.x)) << 8 + ret.getInfo().get(MapleStatInfo.y));
                    break;
                }
                case 400001003: {
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.MAXHP, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.MAXMP, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 400001004: {
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.战斗命令, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 400001005: {
                    ret.setOverTime(true);
                    statups.clear();
                    statups.put(MapleBuffStat.进阶祝福, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                    statups.put(MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.indieMmp));
                    break;
                }
                case 400001006: {
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.极速领域, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 尖兵.超能光束炮: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieInvincible, 0);
                    statups.put(MapleBuffStat.超能光束炮, -1);
                    break;
                }
                case 幻影.小丑_2: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_DebuffTolerance, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_DotHealHPPerSecond, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 幻影.小丑_3: {
                    statups.put(MapleBuffStat.呼啸_伤害减少, ret.getInfo().get(MapleStatInfo.z));
                }
                case 幻影.小丑_5: {
                    statups.put(MapleBuffStat.呼啸_伤害减少, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.indieAsrR));
                    break;
                }
                case 幻影.小丑_6: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_DebuffTolerance, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_DotHealHPPerSecond, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.呼啸_伤害减少, ret.getInfo().get(MapleStatInfo.z));
                    break;
                }
                case 幻影.王牌: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_KeyDownMoving, 50);
                    break;
                }
                case 400011011: {
                    statups.put(MapleBuffStat.神奇圆环, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 神炮王.宇宙无敌火炮弹: {
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.宇宙无敌火炮弹, 0);
                    break;
                }
                case 双弩.精灵元素: {
                    statups.put(MapleBuffStat.精灵元素, ret.getInfo().get(MapleStatInfo.x));
                }
                case 魂骑士.天人之舞:
                case 神射手.箭雨: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm, 1);
                    break;
                }
                case 弓箭手.向导之箭: {
                    statups.put(MapleBuffStat.向导之箭, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 风灵使者.呼啸暴风: {
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.呼啸暴风, 0);
                    break;
                }
                case 黑骑士.交叉锁链:
                    statups.put(MapleBuffStat.交叉锁链, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 黑骑士.龙之献祭:
                    ret.setHpR(ret.getInfo().get(MapleStatInfo.y) / 100.0);
                    statups.put(MapleBuffStat.BOSS伤害, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.indieBDR));
                    break;
                case 魔法师.魔法盾:
                case 龙神.魔法盾:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.魔法盾, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 机械师.金属机甲_人类:
                case 机械师.终极机甲:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.金属机甲, (int) ret.getLevel());
                    statups.put(MapleBuffStat.提高攻击速度, 1);
                    statups.put(MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed)); //这个封包要放在骑兽BUFF的后面
                    break;
                case 机械师.金属机甲_战车:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.金属机甲, (int) ret.getLevel());
                    statups.put(MapleBuffStat.爆击提升, ret.getInfo().get(MapleStatInfo.cr));
                    break;
                case 预备兵.隐藏碎片:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.隐藏碎片, 1);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    statups.put(MapleBuffStat.增加最大MP百分比, ret.getInfo().get(MapleStatInfo.indieMmpR));
                    break;
                case 9101004:
                case 管理员.隐藏术:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.隐身术, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 双刀.进阶隐身术:
                    statups.put(MapleBuffStat.隐身术, (int) ret.getLevel());
                    statups.put(MapleBuffStat.移动速度, 1);
                    break;
                case 飞侠.隐身术:
                case 夜行者.隐身术:
                    statups.put(MapleBuffStat.隐身术, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 侠盗.敛财术:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.敛财术, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 侠盗.金钱护盾:
                    statups.put(MapleBuffStat.金钱护盾, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 侠盗.暴击蓄能:
                case 侠盗.名流爆击:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.暴击蓄能, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 隐士.影分身:
                case 侠盗.影分身:
                case 双刀.镜像分身:
                case 尖兵.全息投影:
//                    if (sourceid == 尖兵.全息投影) {
//                        ret.getInfo().put(MapleStatInfo.time, 3 * 60 * 1000); //暂时设置为3分钟
//                    }
                    statups.put(MapleBuffStat.影分身, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 侠盗.侠盗本能:
                    statups.put(MapleBuffStat.击杀点数, 0); //设置默认击杀点数为0
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 侠盗.暗杀:
                    statups.put(MapleBuffStat.超越攻击, 1); //100%稳如泰山
                    break;
                case 侠盗.烟幕弹:
//                    monsterStatus.put(MonsterStatus.攻击无效, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神射手.无形箭: //3101004 - 无形箭 - 一定时间内增加物理攻击力，使用弓的时候不会消耗箭矢。
                case 箭神.无形箭:
                case 主教.时空门:
                case 机械师.传送门_GX9:
                case 船长.无限子弹:
                    statups.put(MapleBuffStat.无形箭弩, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 豹弩游侠.无形箭弩:
                    statups.clear();
                    statups.put(MapleBuffStat.无形箭弩, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad)); //这个要放在后面才有效果
                    break;
                case 火毒.神秘瞄准术:
                case 冰雷.神秘瞄准术:
                case 主教.神秘瞄准术:
                    ret.getInfo().put(MapleStatInfo.time, 5 * 1000);
                    statups.put(MapleBuffStat.神秘瞄准术, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.寒冰冲击:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, -20);
                    statups.put(MapleBuffStat.属性攻击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.火焰冲击:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Poison, ret.getInfo().get(MapleStatInfo.dot));
                    statups.put(MapleBuffStat.属性攻击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.雷鸣冲击:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_Poison, ret.getInfo().get(MapleStatInfo.dot));
                    statups.put(MapleBuffStat.属性攻击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.神圣冲击:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Seal, 1);
                    statups.put(MapleBuffStat.属性攻击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 战神.冰雪矛:
//                    statups.put(MapleBuffStat.冰雪矛, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 火毒.自然力重置:
                case 冰雷.自然力重置:
                case 龙神.自然力重置:
                    statups.put(MapleBuffStat.自然力重置, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神射手.精神集中:
                    statups.put(MapleBuffStat.集中精力, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神射手.极限射箭:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.极限射箭, 1);
                    break;
                case 箭神.极限射箭:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.极限射箭, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 神射手.进阶箭筒:
                    statups.put(MapleBuffStat.进阶箭筒, 0);
                    break;
                case 箭神.伤害置换:
                    statups.put(MapleBuffStat.伤害置换, 0);
                    break;
                case 冲锋队长.能量获得:
                case 冲锋队长.超级冲击:
                case 冲锋队长.终极冲击:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.能量获得, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.祝福护甲:
                    statups.clear();
                    statups.put(MapleBuffStat.祝福护甲, ret.getInfo().get(MapleStatInfo.x) + 1);
                    statups.put(MapleBuffStat.祝福护甲_增加物理攻击, ret.getInfo().get(MapleStatInfo.epad));
                    break;
                case 圣骑士.战斗命令:
                    statups.put(MapleBuffStat.战斗命令, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 圣骑士.元气恢复:
                    ret.setHpR(ret.getInfo().get(MapleStatInfo.x) / 100.0);
                    statups.put(MapleBuffStat.元气恢复, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 圣骑士.抗震防御:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.抗震防御, (int) ret.getLevel());
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.防御概率, ret.getInfo().get(MapleStatInfo.indiePddR));
                    break;
                case 圣骑士.虚空元素:
                    statups.put(MapleBuffStat.自然力重置, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 圣骑士.元素冲击:
                case 圣骑士.万佛归一破:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.元素冲击, 1);
                    break;
                case 圣骑士.连环环破:
                    statups.clear();
                    statups.put(MapleBuffStat.元素爆破, 1);
                    break;
                case 圣骑士.至圣领域:
                    statups.clear();
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.至圣领域, 1);
                    break;
                case 英雄.快速武器:
                case 圣骑士.快速武器:
                case 黑骑士.快速武器:
                case 神射手.快速箭:
                case 箭神.快速弩:
                case 隐士.快速暗器:
                case 侠盗.快速短刀:
                case 双弩.快速双弩枪:
                case 恶魔猎手.恶魔加速:
                case 火毒.魔法狂暴:
                case 冰雷.魔法狂暴:
                case 主教.魔法狂暴:
                case 龙神.魔法狂暴:
                case 炎术士.火焰之书:
                case 冲锋队长.急速拳:
                case 船长.速射:
                case 龙的传人.追影身法:
                case 魂骑士.机警灵活: //新的魂骑士职业技能
                case 风灵使者.快速箭_新: //新的风灵使者技能
                case 夜行者.快速投掷:
                case 奇袭者.急速拳_新: //新的奇袭者技能
                case 双刀.快速双刀:
                case 唤灵斗师.快速长杖:
                case 豹弩游侠.快速弩:
                case 机械师.机械加速:
                case 爆破手.快速金属手套:
                case 神炮王.大炮加速:
                case 幻影.快速手杖: //V.103新增职业
                case 夜光.魔法狂暴: //V.106新增职业
                case 米哈尔.快速剑: //V.104新增职业
                case 恶魔复仇者.恶魔加速: //V.110新增职业
                case 尖兵.尖兵加速:
                case 剑豪.秘剑_隼:
                case 阴阳师.孔雀开屏:
                    statups.put(MapleBuffStat.攻击加速, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 战神.快速矛:
                    statups.put(MapleBuffStat.攻击加速, -ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 机械师.幸运骰子:
                case 机械师.双幸运骰子:
                case 冲锋队长.幸运骰子:
                case 船长.幸运骰子:
                case 冲锋队长.双幸运骰子:
                case 船长.双幸运骰子:
                case 神炮王.幸运骰子:
                case 神炮王.双幸运骰子:
                    statups.put(MapleBuffStat.幸运骰子, 0);
                    break;
                case 神炮王.随机橡木桶:
                    statups.put(MapleBuffStat.随机橡木桶, Randomizer.nextInt(3) + 1);
                    break;
                case 冲锋队长.反制攻击:
                case 船长.反制攻击:
                    statups.put(MapleBuffStat.反制攻击, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 冲锋队长.蛇拳:
                    statups.put(MapleBuffStat.反制攻击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 冲锋队长.极速领域:
                case 奇袭者.极速领域_新:
                    statups.put(MapleBuffStat.极速领域, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 海盗.疾驰:
                    statups.put(MapleBuffStat.疾驰速度, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.疾驰跳跃, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 黑骑士.神圣之火:
                case 管理员.神圣之火:
                case 9101008:
                    statups.put(MapleBuffStat.MAXHP, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.MAXMP, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 英雄.斗气集中:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.斗气集中, 1);
                    break;
                case 英雄.愤怒之火:
                    statups.put(MapleBuffStat.伤害反击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 英雄.战灵附体:
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 双刀.荆棘:
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 双刀.终极斩:
                    ret.getInfo().put(MapleStatInfo.time, 60 * 1000);
                    ret.setHpR(-ret.getInfo().get(MapleStatInfo.x) / 100.0);
                    statups.put(MapleBuffStat.终极斩, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 双刀.终极斩_无敌:
                    ret.getInfo().put(MapleStatInfo.time, 3 * 1000);
                    statups.put(MapleBuffStat.无敌状态, 1);
                    break;
                case 火毒.快速移动精通:
                case 冰雷.快速移动精通:
                case 主教.快速移动精通:
                    ret.getInfo().put(MapleStatInfo.mpCon, ret.getInfo().get(MapleStatInfo.y));
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.移动精通, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    break;
                case 英雄.冒险岛勇士:
                case 圣骑士.冒险岛勇士:
                case 黑骑士.冒险岛勇士:
                case 火毒.冒险岛勇士:
                case 冰雷.冒险岛勇士:
                case 主教.冒险岛勇士:
                case 神射手.冒险岛勇士:
                case 箭神.冒险岛勇士:
                case 隐士.冒险岛勇士:
                case 侠盗.冒险岛勇士:
                case 冲锋队长.冒险岛勇士:
                case 船长.冒险岛勇士:
                case 战神.冒险岛勇士:
                case 龙神.冒险岛勇士:
                case 双刀.冒险岛勇士:
                case 唤灵斗师.冒险岛勇士:
                case 豹弩游侠.冒险岛勇士:
                case 机械师.冒险岛勇士:
                case 爆破手.冒险岛勇士:
                case 神炮王.冒险岛勇士:
                case 双弩.冒险岛勇士:
                case 恶魔猎手.冒险岛勇士:
                case 龙的传人.冒险岛勇士: //V.103新增职业
                case 幻影.冒险岛勇士: //V.103新增职业
                case 夜光.冒险岛勇士: //V.106新增职业
                case 米哈尔.冒险岛勇士: //V.103新增职业
                case 狂龙战士.诺巴的勇士: //T071新增职业
                case 爆莉萌天使.诺巴的勇士: //T071新增职业
                case 恶魔复仇者.冒险岛勇士: //V.110新增职业
                case 尖兵.冒险岛勇士:
                case 魂骑士.希纳斯的骑士:
                case 炎术士.希纳斯的骑士:
                case 风灵使者.希纳斯的骑士:
                case 夜行者.希纳斯的骑士:
                case 奇袭者.希纳斯的骑士:
                case 隐月.冒险岛勇士:
                case 剑豪.晓之勇者:
                case 阴阳师.晓之勇者:
                case 超能力者.异界勇士:
                    statups.put(MapleBuffStat.冒险岛勇士, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神射手.火眼晶晶:
                case 箭神.火眼晶晶:
                case 豹弩游侠.火眼晶晶:
                case 风灵使者.火眼晶晶: //3221002 - 火眼晶晶 - 在一定时间内赋予全体队员寻找敌人的弱点并造成致命伤害的能力。
                    statups.put(MapleBuffStat.火眼晶晶, (ret.getInfo().get(MapleStatInfo.x) << 8) + ret.getInfo().get(MapleStatInfo.y) + ret.getInfo().get(MapleStatInfo.criticaldamageMax));
                    break;
                case 龙神.抗魔领域:
                    statups.put(MapleBuffStat.抗魔领域, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 魔法师.精灵弱化:
                    statups.put(MapleBuffStat.精灵弱化, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 战神.抗压:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战神抗压, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 战神.矛连击强化:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.矛连击强化, 10);
                    break;
                case 战神.激素引擎:
                    statups.put(MapleBuffStat.激素引擎, 1);
                    break;
                case 船长.指挥船员:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 120000);
                    statups.put(MapleBuffStat.精神连接, 10);
                    break;
                case 战神.激素狂飙:
                    statups.put(MapleBuffStat.提高攻击速度, 1);
                    statups.put(MapleBuffStat.激素狂飙, ret.getInfo().get(MapleStatInfo.w));
                    break;
                case 战神.生命吸收:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                case 恶魔猎手.吸血鬼之触:
                case 黑骑士.黑暗饥渴:
                    statups.put(MapleBuffStat.生命吸收, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 双弩.火焰咆哮:
                    statups.put(MapleBuffStat.火焰咆哮, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 双弩.古老意志:
                    statups.put(MapleBuffStat.反制攻击, ret.getInfo().get(MapleStatInfo.damR));
                    statups.put(MapleBuffStat.增加物理攻击力百分比, ret.getInfo().get(MapleStatInfo.indiePadR));
                    break;
                case 英雄.魔击无效:
                case 圣骑士.魔击无效:
                case 黑骑士.魔击无效:
                case 米哈尔.魔击无效:
                    monsterStatus.put(MonsterStatus.MOB_STAT_MagicCrash, 1);
                    break;
                case 双弩.水盾:
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.asrR));
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.terR));
                    break;
                case 圣骑士.压制术:
                    monsterStatus.put(MonsterStatus.MOB_STAT_PAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Blind, ret.getInfo().get(MapleStatInfo.z));
                    break;
                case 魂骑士.灵魂之眼:
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_TrueSight, 0);
                    break;
                case 英雄.虎咆哮:
                case 侠盗.一出双击:
                case 侠盗.神通术:
                case 神射手.爆炸箭:
                case 冲锋队长.能量爆破:
                case 唤灵斗师.黑暗锁链:
                case 唤灵斗师.黑暗创世:
                case 机械师.火箭拳:
                case 双刀.暗影飞跃斩:
                case 恶魔猎手.死亡牵引:
                case 恶魔猎手.恶魔追踪:
                case 冰雷.链环闪电:
                case 主教.圣光:
                case 战神.旋风:
                case 神炮王.猴子炸药桶:
                case 神炮王.猴子超级炸弹:
                case 神炮王.猴子冲击波:
                case 神炮王.猴子冲击波_1:
                case 9001020: //没有这个GM技能
                case 9101020: //没有这个GM技能
                case 米哈尔.闪耀爆破:
                case 米哈尔.闪耀爆炸:
                case 狂龙战士.穿刺冲击:
                case 狂龙战士.牵引锁链:
                case 恶魔复仇者.血腥禁锢:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    break;
                case 英雄.恐慌:
                case 双刀.闪光弹:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Blind, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 隐士.决战之巅:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Showdown, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 隐士.模糊领域:
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 豹弩游侠.激怒:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Showdown, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_Darkness, 1);
                    break;
                case 豹弩游侠.十字攻击:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 2);
                    break;
                case 恶魔猎手.鬼泣:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Showdown, ret.getInfo().get(MapleStatInfo.w));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_ACC, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 双弩.传说之矛: //not sure if negative
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 冰雷.冰冻术: // cold beam
                case 冰雷.冰河锁链:
                case 冰雷.冰咆哮: // ice strike
                case 冰雷.落霜冰破: // Blizzard
                case 火毒.美杜莎之眼: // Paralyze
                case 90001006: //没有这个GM技能
                    monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
                    ret.getInfo().put(MapleStatInfo.time, ret.getInfo().get(MapleStatInfo.time) * 2); // freezing skills are a little strange
                    break;
                case 冰雷.极冻吐息:
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.至圣领域, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, ret.getInfo().get(MapleStatInfo.y));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
//                    ret.getInfo().put(MapleStatInfo.time, ret.getInfo().get(MapleStatInfo.time) * 2); // freezing skills are a little strange
                    break;
                case 90001002: //没有这个GM技能
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神炮王.紧急后撤:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, ret.getInfo().get(MapleStatInfo.z));
                    break;
                case 神炮王.霰弹炮:
                    statups.put(MapleBuffStat.霰弹炮, 1);
                    break;
                case 英雄.葵花宝典: //1121010 - 葵花宝典 - 将积累的斗气激发出来。在一定时间内集中攻击一个敌人，伤害大幅提升。消耗10个斗气。
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.葵花宝典, ret.getInfo().get(MapleStatInfo.x) * 100 + ret.getInfo().get(MapleStatInfo.mobCount));
                    statups.put(MapleBuffStat.爆击概率增加, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.最小爆击伤害, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 米哈尔.灵魂之怒: //51121006 - 灵魂之怒 - 一定时间内可以集中攻击一个敌人，伤害值大幅提升。
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.葵花宝典, ret.getInfo().get(MapleStatInfo.x) * 100 + ret.getInfo().get(MapleStatInfo.mobCount));
                    break;
                case 双弩.独角兽之角:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Weakness, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 90001003: //没有这个GM技能
                    monsterStatus.put(MonsterStatus.MOB_STAT_Poison, 1);
                    break;
                case 冰雷.寒冰灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.寒冰灵气, 1);
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.v));
                    break;
                case 主教.神圣魔法盾:
                    statups.put(MapleBuffStat.神圣魔法盾, ret.getInfo().get(MapleStatInfo.x));
                    ret.getInfo().put(MapleStatInfo.cooltime, ret.getInfo().get(MapleStatInfo.y));
                    ret.setHpR(ret.getInfo().get(MapleStatInfo.z) / 100.0);
                    break;
                case 火毒.元素配合:
                case 冰雷.元素配合:
                case 主教.神圣保护:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.神圣保护, 1);
                    break;
                case 主教.神圣祈祷:
                case 管理员.神圣祈祷:
                case 9101002: //没有这个GM技能
                    statups.put(MapleBuffStat.神圣祈祷, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 80001034: //神圣拯救者的祝福
                case 80001035: //神圣拯救者的祝福
                case 80001036: //神圣拯救者的祝福
                    statups.put(MapleBuffStat.神圣拯救者的祝福, 1);
                    break;
                case 90001005: //没有这个GM技能
                    monsterStatus.put(MonsterStatus.MOB_STAT_Seal, 1);
                    break;
                case 隐士.影网术:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Web, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_Poison, ret.getInfo().get(MapleStatInfo.dot));
                    break;
                case 隐士.暗器伤人:
                case 夜行者.魔法飞镖:
                    statups.put(MapleBuffStat.暗器伤人, 0);
                    break;
                case 火毒.终极无限:
                case 冰雷.终极无限:
                case 主教.终极无限:
                    ret.setHpR(ret.getInfo().get(MapleStatInfo.y) / 100.0);
                    ret.setMpR(ret.getInfo().get(MapleStatInfo.y) / 100.0);
                    statups.put(MapleBuffStat.终极无限, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 龙神.玛瑙的意志: //在一定时间内进入不会被敌人的攻击击退的状态，可以对敌人发动身体冲撞。
//                    statups.put(MapleBuffStat.玛瑙意志, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 龙神.龙神:
                    ret.getInfo().put(MapleStatInfo.time, ret.getInfo().get(MapleStatInfo.y) * 1000);
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.飞行骑乘, 1);
                    statups.put(MapleBuffStat.SECONDARY_STAT_RideVehicleExpire, 1939007);
                    break;
                case 唤灵斗师.稳如泰山:
                case 神炮王.海盗精神:
                case 米哈尔.稳如泰山:
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 2121002: //魔法反击 现在好像没有这3个技能
                case 2221002: //魔法反击
                case 2321002: //魔法反击
                    statups.put(MapleBuffStat.魔法反击, 1);
                    break;
                case 主教.进阶祝福:
                    statups.clear();
                    statups.put(MapleBuffStat.进阶祝福, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                    statups.put(MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.indieMmp));
                    break;
                case 神射手.幻影步:
                case 箭神.幻影步:
                    statups.put(MapleBuffStat.额外回避, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.移动速度, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 豹弩游侠.暴走形态:
                    statups.put(MapleBuffStat.ATTACK_BUFF, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.移动速度, ret.getInfo().get(MapleStatInfo.z));
                    break;
                case 豹弩游侠.呼啸:
                    statups.put(MapleBuffStat.DAMAGE_BUFF, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.呼啸_爆击概率, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.呼啸_MaxMp增加, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.呼啸_伤害减少, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.呼啸_回避概率, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 豹弩游侠.弹仓扩展:
                    statups.clear();
                    statups.put(MapleBuffStat.增加所有属性, ret.getInfo().get(MapleStatInfo.indieAllStat));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 豹弩游侠.召唤美洲豹_灰:
                case 豹弩游侠.召唤美洲豹_黄:
                case 豹弩游侠.召唤美洲豹_红:
                case 豹弩游侠.召唤美洲豹_紫:
                case 豹弩游侠.召唤美洲豹_蓝:
                case 豹弩游侠.召唤美洲豹_剑:
                case 豹弩游侠.召唤美洲豹_雪:
                case 豹弩游侠.召唤美洲豹_玛瑙:
                case 豹弩游侠.召唤美洲豹_铠甲:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.召唤美洲豹, ret.getInfo().get(MapleStatInfo.criticaldamageMin) << 8 + ret.getInfo().get(MapleStatInfo.asrR));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 2);
                    break;
                case 豹弩游侠.沉默之怒:
                    statups.put(MapleBuffStat.终极攻击, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 豹弩游侠.撤步退身:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.撤步退身, 0);
                    break;
                case 主教.祝福:
                case 管理员.运营员的祝福:
                case 9101003: //没有这个GM技能
                    statups.put(MapleBuffStat.牧师祝福, (int) ret.getLevel());
                    break;
                case 唤灵斗师.死亡:
                case 唤灵斗师.死亡契约:
                case 唤灵斗师.死亡契约2:
                case 唤灵斗师.死亡契约3:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.死亡契约, 0);
                    break;
                case 唤灵斗师.黄色灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战法灵气, (int) ret.getLevel());
                    statups.put(MapleBuffStat.提高攻击速度, 1);
                    break;
                case 唤灵斗师.吸收灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战法灵气, (int) ret.getLevel());
                    statups.put(MapleBuffStat.生命吸收, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 唤灵斗师.蓝色灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战法灵气, (int) ret.getLevel());
                    break;
                case 唤灵斗师.黑暗灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战法灵气, (int) ret.getLevel());
                    break;
                case 唤灵斗师.减益灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.战法灵气, (int) ret.getLevel());
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.y));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 唤灵斗师.暴怒对战:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.葵花宝典, ret.getInfo().get(MapleStatInfo.x) * 100 + ret.getInfo().get(MapleStatInfo.mobCount));
                    statups.put(MapleBuffStat.爆击概率增加, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.最小爆击伤害, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 机械师.完美机甲:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.完美机甲, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 恶魔猎手.黑暗复仇:
                    statups.put(MapleBuffStat.伤害反击, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 米哈尔.灵魂恢复术: //51111004 - 灵魂恢复术 - 在一定时间内大幅提高防御力、异常状态耐性、所有属性耐性概率。
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.防御力百分比, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 恶魔猎手.黑暗变形: //黑暗变形 - 在一定时间内解放潜在的力量，提高伤害和HP。在变形状态下，2个黑暗斗气以施展者为中心快速旋转，自动攻击范围内的多个敌人。
                    statups.put(MapleBuffStat.黑暗变形, ret.getInfo().get(MapleStatInfo.damR));
                    statups.put(MapleBuffStat.反制攻击, (int) ret.getLevel());
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    break;
                case 恶魔猎手.无限精气: //无限精气 - 消耗掉所有精气，在一定时间内使用技能时可以不消耗精气。但不能忽视技能冷却时间。
                    statups.put(MapleBuffStat.无限精气, 1);
                    break;
                case 幻影.幻影回归: //幻影回归 - 返回到幻影的专用飞艇水晶花园中。
                case 80001040: //精灵的祝福
                case 双弩.精灵的祝福:
                    ret.setMoveTo(ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 80001089: // 飞翔· Soaring
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.飞翔, 1);
                    break;
                case 10001075: //女皇的祈祷 Cygnus Echo
                    statups.put(MapleBuffStat.英雄回声, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 船长.海盗气魄: //5221018 - 海盗气魄 - 展现不怕死的海盗的气势，攻击力、状态异常和属性抗性增加，有一定概率不被击退。因为不怕死，所以回避值减少。
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.反制攻击, ret.getInfo().get(MapleStatInfo.damR));
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 幻影.卡牌审判:
                case 幻影.卡牌审判_高级:
                    statups.put(MapleBuffStat.卡牌审判, 0);
                    break;
                case 幻影.幻影屏障:
                    statups.put(MapleBuffStat.幻影屏障, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 幻影.幸运保护: //幸运保护 - 为了避免不幸，在身边设置卡片的封印。HP、MP、属性抗性、状态异常抗性增加。
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    statups.put(MapleBuffStat.增加最大MP百分比, ret.getInfo().get(MapleStatInfo.indieMmpR));
                    break;
                case 幻影.圣歌祈祷: //圣歌祈祷 - 受到圣歌的祈祷，攻击力大幅上升，有一定概率无视敌人的防御力。
                    statups.put(MapleBuffStat.反制攻击, ret.getInfo().get(MapleStatInfo.damR));
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.damR));
                    break;
                case 幻影.神秘的运气: //神秘的运气 - 最幸运的幻影可以永久性地提高运气。使用技能时，进入可以避免一次死亡并恢复体力的幸运状态。
                    ret.getInfo().put(MapleStatInfo.time, 900000);
                    statups.put(MapleBuffStat.神秘运气, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 米哈尔.光之守护: //光之守护 - 受到光之守护，在一定时间内即使受到敌人攻击，也不会被击退。
                case 80001140: //光之守护 - [排名技能]受到光之骑士米哈尔的庇护，在一定时间内，即使受到敌人攻击也不会被击退。
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 米哈尔.神圣方块:
                    statups.clear();
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 夜光.太阳火焰:
                case 夜光.月蚀:
                    ret.getInfo().put(MapleStatInfo.time, -1);
                    statups.put(MapleBuffStat.光暗转换, 1);
                    statups.put(MapleBuffStat.光暗转换_2, 0);
                    break;
                case 夜光.平衡_光明:
                case 夜光.平衡_黑暗:
                    statups.put(MapleBuffStat.光暗转换, 2);
                    statups.put(MapleBuffStat.光暗转换_2, 0);
                    break;
                case 夜光.闪爆光柱:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    break;
                case 夜光.黑暗祝福:
                    statups.put(MapleBuffStat.黑暗祝福, 1);
                    break;
                case 夜光.生命潮汐:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.生命潮汐, 2);
                    break;
                case 夜光.抵抗之魔法盾:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.神圣保护, 3);
                    break;
                case 夜光.黑暗巫术:
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.自然力重置, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 夜光.黑暗高潮:
                    statups.put(MapleBuffStat.黑暗高潮, 1);
                    break;
                case 狂龙战士.防御模式:
                case 狂龙战士.攻击模式:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.模式转换, 0);
                    break;
                case 狂龙战士.熊熊烈火升级:
                    statups.clear();
                    statups.put(MapleBuffStat.攻击加速, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    break;
                case 狂龙战士.重拾力量:
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.terR));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.asrR));
                    break;
                case 狂龙战士.强健护甲:
                    statups.put(MapleBuffStat.强健护甲, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 狂龙战士.剑刃之壁:
                case 狂龙战士.进阶剑刃之壁:
                case 狂龙战士.剑刃之壁_变身:
                case 狂龙战士.进阶剑刃之壁_变身:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.剑刃之壁, (int) ret.getLevel());
                    break;
                case 爆莉萌天使.力量转移: //65101002 - 力量转移 - 吸收部分对目标的伤害，形成保护自己的保护膜。
                    statups.put(MapleBuffStat.伤害置换, ret.getInfo().get(MapleStatInfo.y) * 1000);
                    break;
                case 爆莉萌天使.钢铁莲花: //65111004 - 钢铁莲花 - 以钢铁般的意志抵挡敌人的攻击。
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 爆莉萌天使.灵魂凝视: //65121004 - 灵魂凝视 - 借助爱丝卡达的力量，具备了洞穿敌人灵魂的双眼。
                    statups.put(MapleBuffStat.灵魂凝视, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 爆莉萌天使.灵魂吸取专家:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.灵魂吸取专家, 1);
                    break;
                case 爆莉萌天使.终极契约:
                    statups.clear();
                    statups.put(MapleBuffStat.终极契约, ret.getInfo().get(MapleStatInfo.indieStance));
                    statups.put(MapleBuffStat.稳如泰山, 0);
                    statups.put(MapleBuffStat.爆击概率增加, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.terR));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.asrR));
                    break;
                case 恶魔复仇者.超越:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.恶魔超越, 1);
                    break;
                case 恶魔复仇者.血之契约:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.生命潮汐, 3);
                    break;
                case 恶魔复仇者.负荷释放:
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    break;
                case 恶魔复仇者.驱邪: //31211003 - 驱邪 - 在一定时间内获得抵挡邪恶气息的能力。所有属性抗性和状态抗性增加，使受到的伤害减少一定比例。
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 恶魔复仇者.恶魔恢复:
                    statups.put(MapleBuffStat.恶魔恢复, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    break;
                case 尖兵.急速支援: //30020232 - 急速支援 - 每隔一定时间补充1个电力，受到攻击/回避时有一定概率补充1个。根据获得的能量，所有能力值提高。获得的能量可能会在使用技能时消耗。
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.尖兵电力, 1);
                    break;
                case 尖兵.自由飞行: //30021237 - 自由飞行 - 打开推进器，在一定时间内可以自由飞行。在飞行状态下只能使用精准火箭技能和增益技能。
                    ret.getInfo().put(MapleStatInfo.time, 30000);
                    statups.put(MapleBuffStat.飞行骑乘, 1);
                    break;
                case 尖兵.精准火箭:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.精准火箭, 1);
                    break;
                case 尖兵.超能力量: //36001002 - 超能力量 - 提高能量，增加伤害。
                    statups.clear();
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    break;
                case 尖兵.直线透视: //36101002 - 直线透视 - 在一定时间内激活视觉，看穿敌人的弱点，提高爆击率。消耗备用能量。
                    statups.put(MapleBuffStat.爆击提升, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 尖兵.高效输能: //36101003 - 高效输能 - 在一定时间内提高能量流的利用效率，增加最大HP和最大MP。同时可以获得增加最大HP和最大MP的被动效果。
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    statups.put(MapleBuffStat.增加最大MP百分比, ret.getInfo().get(MapleStatInfo.indieMmpR));
                    break;
                case 尖兵.双重防御:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.黑暗高潮, ret.getInfo().get(MapleStatInfo.prop));
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.z));
                    break;
                case 尖兵.宙斯盾系统:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.宙斯盾系统, 1);
                    break;
                case 尖兵.神秘代码:
                    statups.put(MapleBuffStat.BOSS伤害, ret.getInfo().get(MapleStatInfo.indieBDR));
                    break;
                case 尖兵.攻击矩阵: //36121004 - 攻击矩阵 - 激活攻击矩阵，在一定时间内攻击时无视敌人的防御，不会被敌人的攻击击退。
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 尖兵.全息力场_支援:
                    statups.clear();
                    statups.put(MapleBuffStat.回避增加, ret.getInfo().get(MapleStatInfo.evaR));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    break;
                case 尖兵.永动引擎:
                    statups.put(MapleBuffStat.永动引擎, 1);
                    break;
                case 魂骑士.灵魂之剑: //11121054 - 灵魂之剑 - 将灵魂的力量化为一柄剑。
                    statups.clear();
                    statups.put(MapleBuffStat.命中增加, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.增加伤害最大值, ret.getInfo().get(MapleStatInfo.indieMaxDamageOver));
                    break;
                case 魂骑士.元素_灵魂: //11001022 - 元素：灵魂 - 召唤出灵魂元素，获得其的力量。
                    statups.put(MapleBuffStat.元素灵魂_僵直, (int) ret.getLevel());
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    break;
                case 魂骑士.月光洒落: //11101022 - 月光洒落 - 将月光的力量注入到剑中。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.霰弹炮, (int) ret.getLevel());
                    statups.put(MapleBuffStat.月光转换, 1); //1 = 月光 2 = 旭日
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 魂骑士.旭日: //11111022 - 旭日 - 在剑上注入阳光的力量。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.月光转换, 2); //1 = 月光 2 = 旭日
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 魂骑士.日月轮转:
                    statups.clear();
                    statups.put(MapleBuffStat.日月轮转, 1);
                    break;
                case 魂骑士.日月轮转_月光洒落: //11121011 暴击概率x% 所有技能攻击次数增加2倍 伤害减少x%
                case 魂骑士.日月轮转_旭日: //11121012 伤害增加x% 攻击速度提升
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.霰弹炮, 20);
                    statups.put(MapleBuffStat.月光转换, sourceid == 魂骑士.日月轮转_旭日 ? 1 : 2);
                    statups.put(MapleBuffStat.提高攻击速度, -1);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    statups.put(MapleBuffStat.暴击概率, 20);
//                    statups.put(MapleBuffStat.伤害减少, (int) ret.getLevel());
//                    statups.put(MapleBuffStat.月光转换, 1); //1 = 月光 2 = 旭日
//                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 风灵使者.元素_风: //13001022 - 元素：风 - 召唤出绿色的风元素，获得其的力量。
                    statups.clear();
                    statups.put(MapleBuffStat.元素属性, 1);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 风灵使者.风精灵相助: //13101024 - 风精灵相助 - 获得风精灵的帮助，在战斗中抢先占据有利地位。
                    statups.clear();
                    statups.put(MapleBuffStat.无形箭弩, 1);
                    statups.put(MapleBuffStat.爆击提升, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    break;
                case 风灵使者.狂风肆虐Ⅰ:
                case 风灵使者.狂风肆虐Ⅱ:
                case 风灵使者.狂风肆虐Ⅲ:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.狂风肆虐, 1);
                    break;
                case 风灵使者.信天翁_新: //13111023 - 信天翁 - 借助传说的名弓信天翁的力量，将战斗能力提升到极限。
                    statups.clear();
                    statups.put(MapleBuffStat.信天翁, (int) ret.getLevel());
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 风灵使者.极限信天翁: //13120008 - 极限信天翁 - 将传说中的名弓信天翁的力量发挥到极限，和自己化为一体。\n需要技能：#c信天翁20级以上#
                    statups.clear();
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.ignoreMobpdpR));
                    statups.put(MapleBuffStat.信天翁, (int) ret.getLevel());
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.indieAsrR));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.indieTerR));
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 风灵使者.风之祝福: //13121004 - 风之祝福 - 获得风之祝福，学会与风合为一体的方法。
                    statups.clear();
                    statups.put(MapleBuffStat.额外回避, ret.getInfo().get(MapleStatInfo.prop));
                    statups.put(MapleBuffStat.命中增加, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.敏捷增加, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.indieMhpR));
                    break;
                case 风灵使者.暴风灭世: //13121054 - 暴风灭世 - 召唤出暴风之力，获得惩治敌人的力量。
                    statups.put(MapleBuffStat.暴风灭世, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 奇袭者.元素_闪电: //15001022 - 元素：闪电 - 召唤出青白色的闪电元素，获得其的力量。
                    statups.clear();
                    statups.put(MapleBuffStat.元素属性, 1); //主动BUFF是这个
                    statups.put(MapleBuffStat.百分比无视防御, 5); //被动BUFF是这个默认为5点
                    break;
                case 奇袭者.漩涡: //15111023 - 漩涡 - 召唤出保护身体不受任何攻击伤害的漩涡。
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.asrR));
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.terR));
                    break;
                case 奇袭者.极限铠甲: //15111024 - 极限铠甲 - 召唤出锋利坚硬的铠甲，同时发动攻击和防御。
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 奇袭者.聚雷: //15121004 - 聚雷 - 凝聚周围散布的闪电力量，给自己的所有攻击赋予额外伤害。
                    statups.put(MapleBuffStat.影分身, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 奇袭者.台风:
                case 奇袭者.疾风: //15111022 - 疾风 - 将通过连续攻击累积的力量凝聚在一处，在一击中释放。可以与除了雷鸣与自我之外的所有技能连锁。
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.y));
                    break;
                //-------------------------------------------------------------
                case 箭神.冰凤凰:
                case 神射手.火凤凰:
                case 机械师.磁场:
                case 双弩.精灵骑士:
                case 双弩.精灵骑士1:
                case 双弩.精灵骑士2:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                    break;
                case 机械师.支援波动器_H_EX:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 黑骑士.灵魂助力:
                    statups.put(MapleBuffStat.灵魂助力, (int) ret.getLevel());
                    break;
                case 黑骑士.灵魂助力震惊:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 21);
                    break;
                case 狂龙战士.石化:
                case 狂龙战士.石化_变身:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神之子.伦娜之庇护: //100001268 - 伦娜之庇护 - 获得伦娜女神的庇护，在一定时间内将队员的所有属性提升一定百分比。
                    statups.put(MapleBuffStat.冒险岛勇士, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 神之子.圣洁之力: //100001263 - 圣洁之力 - 通过超越者的力量，增加周围队员的攻击力、防御力、抗性。\n#c再使用一次技能键就可以解除，不能和神圣迅捷一同使用。#
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.圣洁之力, 1);
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.增加魔法攻击力, ret.getInfo().get(MapleStatInfo.indieMad));
                    statups.put(MapleBuffStat.提升物理防御力, ret.getInfo().get(MapleStatInfo.indiePdd));
//                    statups.put(MapleBuffStat.提升魔法防御力, ret.getInfo().get(MapleStatInfo.indieMdd));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.indieTerR));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.indieAsrR));
                    break;
                case 神之子.神圣迅捷: //100001264 - 神圣迅捷 - 通过超越者的力量，增加周围队员的攻击速度、移动速度、跳跃力、跳跃力、回避值、命中值。\n#c再使用一次技能键就可以解除，不能和圣洁之力一同使用。#
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.神圣迅捷, 1);
                    statups.put(MapleBuffStat.增加命中值, ret.getInfo().get(MapleStatInfo.indieAcc));
                    statups.put(MapleBuffStat.增加回避值, ret.getInfo().get(MapleStatInfo.indieEva));
                    statups.put(MapleBuffStat.增加跳跃力, ret.getInfo().get(MapleStatInfo.indieJump));
                    statups.put(MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    break;
                case 神之子.提速时刻_侦查:
                    statups.put(MapleBuffStat.提速时刻_侦查, 1);
                    break;
                case 神之子.提速时刻_战斗:
                    statups.put(MapleBuffStat.提速时刻_战斗, 1);
                    break;
                case 神之子.防御之盾:
                    statups.put(MapleBuffStat.伤害吸收, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 主教.天使复仇:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.ignoreMobpdpR));
                    statups.put(MapleBuffStat.天使复仇, ret.getInfo().get(MapleStatInfo.mobCount)); //好像默认为1
                    statups.put(MapleBuffStat.增加魔法攻击力, ret.getInfo().get(MapleStatInfo.indieMad));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.增加伤害最大值, ret.getInfo().get(MapleStatInfo.indieMaxDamageOver));
                    break;
                /*
                 * 林之灵BUFF状态
                 */
                case 林之灵.巨熊模式:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.守护模式变更, 1);
                    break;
                case 林之灵.雪豹模式:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.守护模式变更, 2);
                    break;
                case 林之灵.猛鹰模式:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.守护模式变更, 3);
                    break;
                case 林之灵.猫咪模式:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.守护模式变更, 4);
                    break;
                case 林之灵.冒险岛守护勇士:
                    statups.put(MapleBuffStat.冒险岛勇士, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 林之灵.嗨_兄弟:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.嗨兄弟, ret.getInfo().get(MapleStatInfo.prop));
                    break;
                case 林之灵.伊卡之眼_强化:
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 林之灵.阿尔之好伙伴: //112120016 - 阿尔之好伙伴 - 与猫咪位于同一地图的队员可以获得更多经验值。(和#c神圣祈祷# 技能重复使用时，只适用更高的效果。)
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.经验获得, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 林之灵.阿尔之窃取: //112120017 - 阿尔之窃取 - 与猫咪位于同一地图的队员,其道具获得率提高。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.爆率增加, ret.getInfo().get(MapleStatInfo.v));
                    break;
                case 林之灵.阿尔之爪: //112120018 - 阿尔之爪 - 与猫咪位于同一地图的队员,其爆击率及爆击最小伤害增加。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.暴击最小伤害, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 林之灵.阿尔之魅力_强化: //112120021 - 阿尔之魅力 强化 - 与猫咪位于同一地图的队员,其攻击力或魔法攻击力增加。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    statups.put(MapleBuffStat.增加魔法攻击力, ret.getInfo().get(MapleStatInfo.indieMad));
                    break;
                case 林之灵.阿尔之弱点把握: //112120022 - 阿尔之弱点把握 - 与猫咪处在同一地图上的队员可以无视敌人一定量的防御力。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.x)); //好像X 和Y 都是一样的
                    break;
                case 林之灵.阿尔之饱腹感: //112120023 - 阿尔之饱腹感 - 与猫咪位于同一地图的队员,最大HP及最大MP增加。
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 12000); //12秒的持续时间
                    statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                    statups.put(MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.indieMmp));
                    break;
                case 林之灵.蓝色卡片:
                    statups.clear();
                    statups.put(MapleBuffStat.物理防御, ret.getInfo().get(MapleStatInfo.pdd));
//                    statups.put(MapleBuffStat.魔法防御, ret.getInfo().get(MapleStatInfo.mdd));
                    break;
                case 林之灵.绿色卡片:
                    statups.clear();
                    statups.put(MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    break;
                case 林之灵.喵喵卡片:
                case 林之灵.金色卡片:
                    statups.clear();
                    statups.put(MapleBuffStat.物理防御, ret.getInfo().get(MapleStatInfo.pdd));
//                    statups.put(MapleBuffStat.魔法防御, ret.getInfo().get(MapleStatInfo.mdd));
                    statups.put(MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 林之灵.集中打击:
                    statups.put(MapleBuffStat.葵花宝典, ret.getInfo().get(MapleStatInfo.x) * 100 + ret.getInfo().get(MapleStatInfo.mobCount));
                    statups.put(MapleBuffStat.爆击概率增加, ret.getInfo().get(MapleStatInfo.z));
                    statups.put(MapleBuffStat.最小爆击伤害, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 林之灵.舞力全开:
                    statups.clear();
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 林之灵.喵喵治愈:
                    ret.setHpR(ret.getInfo().get(MapleStatInfo.hp) / 100.0);
                    break;
                case 林之灵.伊卡飞翔:
                case 80001242: //80001242 - 飞行骑乘 - 可以在一定时间内自由飞行。但只能在村庄中飞行。
                case 超能力者.心魂漫步:
                    statups.put(MapleBuffStat.飞行骑乘, 1);
                    break;
                case 80001427: //80001427 - 神速符文解放 - 解放后的符文释放出强大的力量！#c移动速度、跳跃力提高到最大值，攻击速度大幅提高。在一定时间内获得100%额外经验值。
                    statups.clear();
                    statups.put(MapleBuffStat.增加跳跃力, ret.getInfo().get(MapleStatInfo.indieJump));
                    statups.put(MapleBuffStat.增加移动速度, ret.getInfo().get(MapleStatInfo.indieSpeed));
                    statups.put(MapleBuffStat.增加经验值, ret.getInfo().get(MapleStatInfo.indieExp));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    break;
                case 80001428: //80001428 - 重生符文解放 - 解放后的符文释放出强大的力量！#c体力重生力提高到最大值，自己受到的所有伤害减少。在一定时间内获得100%额外经验值。
                    statups.clear();
                    //statups.put(MapleBuffStat.重生符文, 799); //这个地方是角色最大血量的10% 我暂时就这样处理算了
                    statups.put(MapleBuffStat.增加经验值, ret.getInfo().get(MapleStatInfo.indieExp));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.indieAsrR));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.indieTerR));
                    break;
                case 80001430: //80001430 - 崩溃符文解放 - 解放后的符文释放出强大的力量！#c消灭周围的所有怪物，自己的伤害提高50%。在一定时间内获得100%额外经验值。
                    statups.clear();
                    statups.put(MapleBuffStat.增加经验值, ret.getInfo().get(MapleStatInfo.indieExp));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 80001432: //80001432 - 破灭符文解放 - 解放后的符文释放出强大的力量！#c给敌人造成持续伤害，自己的攻击速度和伤害增加50%。在一定时间内获得100%额外经验值。
                    statups.clear();
                    statups.put(MapleBuffStat.增加经验值, ret.getInfo().get(MapleStatInfo.indieExp));
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 80001752:
                case 80001753:
                case 80001754:
                case 80001755:
                case 80001877:
                case 80001878:
                case 80001879: {
                    statups.clear();
                    statups.put(MapleBuffStat.增加经验值, ret.getInfo().get(MapleStatInfo.indieExp));
                    break;
                }
                case 80001876: {
                    statups.put(MapleBuffStat.骑兽技能, 1939006);
                    break;
                }
                case 80001762: {
                    statups.put(MapleBuffStat.SECONDARY_STAT_RandAreaAttack, 3);
                    break;
                }
                case 80001757: {
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.巨人药水, 500);
                    break;
                }
                case 80001312:
                case 80001313:
                case 80001314:
                case 80001315: {
                    statups.put(MapleBuffStat.骑兽技能, 1932187 + (sourceid - 80001312));
                    break;
                }
                case 80001155: {
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                }
                case 神射手.三彩箭矢:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.三彩箭矢, 101010); //默认满箭矢
                    break;
                case 隐月.幻灵招魂:
                case 夜行者.黑暗重生:
                    statups.put(MapleBuffStat.神秘运气, ret.getInfo().get(MapleStatInfo.x));
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    ret.setOverTime(true);
                    break;
                case 隐月.九死一生:
                    ret.setOverTime(true);
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.九死一生, 1);
                    break;
                case 隐月.破力拳_冲击波:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, -ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 隐月.灵狐:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.灵狐, 1);
                    break;
                case 隐月.招魂结界:
                    statups.put(MapleBuffStat.招魂结界, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 冰雷.寒冰步:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.寒冰步, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
                    break;
                case 船长.神速衔接:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.神速衔接, 1);
                    break;
                case 黑骑士.重生契约:
                    statups.put(MapleBuffStat.无敌状态, 1);
                    statups.put(MapleBuffStat.重生契约, 27);
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.cr));
                    break;
                case 火毒.火焰灵气:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.火焰灵气, 1);
                    break;
                case 火毒.燎原之火:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.燎原之火, 1);
                    break;
                case 箭神.鹰眼:
                    statups.put(MapleBuffStat.爆击概率增加, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieCrMax, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.ignoreMobpdpR));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 隐士.流血剧毒:
                    statups.clear();
                    statups.put(MapleBuffStat.流血剧毒, 1);
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    break;
                case 冲锋队长.能量激发:
                    statups.clear();
                    statups.put(MapleBuffStat.能量激发, 1000);
                    statups.put(MapleBuffStat.提升伤害百分比, 1);
                    break;
                case 冲锋队长.混元归一:
                    ret.setOverTime(true);
                    statups.put(MapleBuffStat.混元归一, 1);
                    break;
                case 船长.不倦神酒:
                    statups.put(MapleBuffStat.受到伤害减少百分比, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.v));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.w));
                    break;
                case 爆莉萌天使.灵魂鼓舞:
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.indieIgnoreMobpdpR));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieBDR, ret.getInfo().get(MapleStatInfo.indieBDR));
                    statups.put(MapleBuffStat.灵魂鼓舞, 1);
                    break;
                case 唤灵斗师.黑暗闪电:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.黑暗闪电, 1);
                    break;
                case 唤灵斗师.战斗大师:
                    statups.clear();
                    statups.put(MapleBuffStat.战斗大师, 2);
                    break;
                case 双弩.小精灵祝福:
                    statups.clear();
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加物理攻击力, ret.getInfo().get(MapleStatInfo.indiePad));
                    break;
                case 双刀.阿修罗:
                    statups.put(MapleBuffStat.阿修罗, ret.getInfo().get(MapleStatInfo.time) * 1000);
                    break;
                case 双刀.隐形剑:
                    statups.clear();
                    statups.put(MapleBuffStat.隐形剑, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                    break;
                case 恶魔猎手.蓝血:
                    statups.clear();
                    statups.put(MapleBuffStat.影分身, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 炎术士.引燃:
                    statups.put(MapleBuffStat.炎术引燃, 1);
                    break;
                case 炎术士.魔力回流:
                    ret.setMpR(ret.getInfo().get(MapleStatInfo.x) / 100.0);
                    break;
                case 炎术士.元素_火焰:
                case 炎术士.元素_火焰II:
                case 炎术士.元素_火焰III:
                case 炎术士.元素_火焰IV:
                    statups.put(MapleBuffStat.增加魔法攻击力, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 炎术士.燃烧领域:
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    break;
                case 炎术士.火凤凰:
                    statups.put(MapleBuffStat.火焰庇佑, 1);
                    break;
                case 炎术士.火凤凰_无敌状态:
                    ret.getInfo().put(MapleStatInfo.time, 3 * 1000);
                    statups.put(MapleBuffStat.无敌状态, 1);
                    break;
                case 炎术士.火焰屏障:
                    statups.put(MapleBuffStat.火焰屏障, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 炎术士.火焰化身_狮子:
                case 炎术士.火焰化身_狐狸:
                    statups.put(MapleBuffStat.百分比无视防御, 1);
                    break;
                case 夜行者.元素_黑暗:
                    statups.put(MapleBuffStat.元素黑暗, 1);
                    monsterStatus.put(MonsterStatus.MOB_STAT_ElementDarkness, 1);
                    break;
                case 夜行者.影子侍从:
                    statups.put(MapleBuffStat.影子侍从, 1);
                    break;
                case 夜行者.影缝之术:
                    statups.put(MapleBuffStat.稳如泰山, 100);
                    break;
                case 夜行者.黑暗领地:
                    statups.put(MapleBuffStat.黑暗领地, 1);
                    statups.put(MapleBuffStat.终极契约, info.get(MapleStatInfo.indieStance));
                    break;
                case 夜行者.黑暗幻影:
                    statups.put(MapleBuffStat.黑暗幻影, 1);
                    break;
                case 神射手.战斗准备:
                    statups.put(MapleBuffStat.战斗准备, 1);
                    break;
                case 龙的传人.天地无我:
                    statups.clear();
                    statups.put(MapleBuffStat.回避增加, ret.getInfo().get(MapleStatInfo.prop));
                    statups.put(MapleBuffStat.命中增加, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.增加敏捷, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 龙的传人.千斤坠:
                    statups.clear();
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    statups.put(MapleBuffStat.增加所有属性, ret.getInfo().get(MapleStatInfo.indieAllStat));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieBDR, ret.getInfo().get(MapleStatInfo.bdR));
                    statups.put(MapleBuffStat.暴击概率, ret.getInfo().get(MapleStatInfo.indieCr));
                    break;
                case 龙的传人.醉卧竹林:
                    statups.clear();
                    statups.put(MapleBuffStat.受到伤害减少百分比, ret.getInfo().get(MapleStatInfo.w));
                    statups.put(MapleBuffStat.增加最大HP百分比, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.y));
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.z));
                    break;
                case 剑豪.武神招来:
                    statups.put(MapleBuffStat.增加攻击力, ret.getInfo().get(MapleStatInfo.padX));
                    statups.put(MapleBuffStat.增加HP百分比, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加MP百分比, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 剑豪.刚健:
                    statups.put(MapleBuffStat.增加状态异常抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加所有属性抗性, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 剑豪.无双十刃之型:
                    statups.put(MapleBuffStat.属性抗性, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.异常抗性, ret.getInfo().get(MapleStatInfo.y));
                    break;
                case 剑豪.厚积薄发:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.厚积薄发, 0);
                    break;
                case 剑豪.拔刀姿势:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.拔刀姿势, 1);
                    break;
                case 剑豪.基本姿势加成:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.增加最大MP百分比, 20);
                    statups.put(MapleBuffStat.增加最大HP百分比, 20);
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.indieIgnoreMobpdpR));
                    statups.put(MapleBuffStat.增加物理攻击力百分比, 2);
                    break;
                case 剑豪.拔刀术加成:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.爆击提升, 35);
                    statups.put(MapleBuffStat.拔刀术加成, 2);
                    statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
                    statups.put(MapleBuffStat.SECONDARY_STAT_IndieBDR, 6);
                    break;
                case 剑豪.避柳:
                    statups.put(MapleBuffStat.避柳, ret.getInfo().get(MapleStatInfo.damR));
                    break;
                case 剑豪.迅速:
                    statups.put(MapleBuffStat.迅速, ret.getInfo().get(MapleStatInfo.t));
                    break;
                case 阴阳师.影朋_小白:
                    statups.clear();
                    ret.getInfo().put(MapleStatInfo.time, 600000);
                    statups.put(MapleBuffStat.影朋小白, 1);
                    break;
                case 阴阳师.结界_樱:
                    monsterStatus.put(MonsterStatus.MOB_STAT_PAD, ret.getInfo().get(MapleStatInfo.x));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MAD, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 阴阳师.花炎结界:
                case 阴阳师.花炎结界_4转:
                    statups.put(MapleBuffStat.花炎结界, 6);
                    break;
                case 阴阳师.幽玄气息:
                case 阴阳师.幽玄气息_4转:
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                    statups.put(MapleBuffStat.百分比无视防御, ret.getInfo().get(MapleStatInfo.x));
                    break;
                case 超能力者.心魂本能:
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.心魂本能, 1);
                    break;
                case 超能力者.心魂之盾2_扭曲:
                    statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.stanceProp));
                case 超能力者.心魂之盾:
                    statups.put(MapleBuffStat.心魂之盾, ret.getInfo().get(MapleStatInfo.er));
                    break;
                case 超能力者.精神强化:
                    statups.put(MapleBuffStat.增加魔法攻击力百分比, ret.getInfo().get(MapleStatInfo.indieMadR));
                    break;
                case 超能力者.心魂领域:
                case 超能力者.心魂领域2:
                    monsterStatus.put(MonsterStatus.MOB_STAT_PDR, -ret.getInfo().get(MapleStatInfo.s));
                    monsterStatus.put(MonsterStatus.MOB_STAT_MDR, -ret.getInfo().get(MapleStatInfo.s));
                    monsterStatus.put(MonsterStatus.MOB_STAT_Speed, -ret.getInfo().get(MapleStatInfo.s));
                    monsterStatus.put(MonsterStatus.MOB_STAT_PsychicGroundMark, ret.getInfo().get(MapleStatInfo.s));
                    break;
                case 隐士.刺客标记:
                case 隐士.隐士标记:
                    ret.setOverTime(true);
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.刺客标记, 1);
                    break;
                case 魂骑士.落魂剑:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
                    break;
                case 恶魔猎手.黑暗束缚:
                    monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
                    break;
                case 夜行者.影子蝙蝠:
                    ret.setOverTime(true);
                    ret.getInfo().put(MapleStatInfo.time, 0);
                    statups.put(MapleBuffStat.影子蝙蝠, 1);
                    break;
                case 爆破手.碎骨巨叉_2:
                case 爆破手.碎骨巨叉_3:
                case 爆破手.碎骨巨叉_4: {
                    ret.getInfo().put(MapleStatInfo.time, 7000);
                    statups.put(MapleBuffStat.气缸过热, 1);
                }
                case 爆破手.狂暴打击_转管炮:
                case 爆破手.装填弹药:
                case 爆破手.爆炸闪动:
                case 爆破手.双重爆炸_转管炮:
                case 爆破手.旋转弹_1: {
                    ret.getInfo().put(MapleStatInfo.time, -1);
                    statups.put(MapleBuffStat.爆破弹夹, 1);
                    break;
                }
                case 爆破手.极限火炮: {
                    statups.put(MapleBuffStat.极限火炮, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 爆破手.忍耐之盾: {
                    ret.getInfo().put(MapleStatInfo.time, 5000);
                    statups.put(MapleBuffStat.忍耐之盾, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 爆破手.急速闪避: {
                    statups.put(MapleBuffStat.急速闪避, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                case 爆破手.组合训练:
                case 爆破手.组合训练II: {
                    statups.put(MapleBuffStat.增加伤害最大值, ret.getInfo().get(MapleStatInfo.x));
                    statups.put(MapleBuffStat.组合训练, ret.getInfo().get(MapleStatInfo.x));
                    break;
                }
                default:
                    break;
            }
            if (JobConstants.is新手职业(sourceid / 10000)) { //新手技能BUFF处理
                switch (sourceid % 10000) {
                    //angelic blessing: HACK, we're actually supposed to use the passives for atk/matk buff
                    case 1087: //黑天使
                        //ret.getInfo().put(MapleStatInfo.time, 0);
                        //statups.put(MapleBuffStat.ANGEL_ATK, 10);
                        //statups.put(MapleBuffStat.ANGEL_MATK, 10);
                        break;
                    case 1085: //大天使
                    case 1090: //大天使
                        //ret.getInfo().put(MapleStatInfo.time, 0);
                        //statups.put(MapleBuffStat.ANGEL_ATK, 5);
                        //statups.put(MapleBuffStat.ANGEL_MATK, 5);
                        break;
                    case 1179: //白天使
                        //ret.getInfo().put(MapleStatInfo.time, 0);
                        //statups.put(MapleBuffStat.ANGEL_ATK, 12);
                        //statups.put(MapleBuffStat.ANGEL_MATK, 12);
                        break;
                    case 93: //潜力解放
                        statups.put(MapleBuffStat.潜力解放, 1);
                        break;
                    case 99:  //破冰巨剑
                    case 104: //蜗居诅咒
                        monsterStatus.put(MonsterStatus.MOB_STAT_Freeze, 1);
                        ret.getInfo().put(MapleStatInfo.time, ret.getInfo().get(MapleStatInfo.time) * 2); // freezing skills are a little strange
                        break;
                    case 103: //霸天斧
                        monsterStatus.put(MonsterStatus.MOB_STAT_Stun, 1);
                        break;
                    case 1001: //团队治疗
                        if (ret.is潜入()) { //潜入BUFF
                            statups.put(MapleBuffStat.潜入状态, ret.getInfo().get(MapleStatInfo.x));
                        } else {
                            statups.put(MapleBuffStat.恢复效果, ret.getInfo().get(MapleStatInfo.x));
                        }
                        break;
                    case 1005: //英雄之回声
                        statups.put(MapleBuffStat.英雄回声, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 1010: //金刚霸体
                        ret.getInfo().put(MapleStatInfo.time, 0);
                        statups.put(MapleBuffStat.金刚霸体, 1);
                        statups.put(MapleBuffStat.无敌状态, 1);
                        break;
                    case 1011: //狂暴战魂
                        statups.put(MapleBuffStat.狂暴战魂, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 1105: //冰骑士
                        ret.getInfo().put(MapleStatInfo.time, 0);
                        statups.put(MapleBuffStat.ICE_SKILL, 1);
                        break;
                    case 1026: //飞翔
                    case 1142: //飞翔
                        ret.getInfo().put(MapleStatInfo.time, 0);
                        statups.put(MapleBuffStat.飞翔, 1);
                        break;
                    case 8001: //好用的时空门
                        statups.put(MapleBuffStat.无形箭弩, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 8002: //好用的火眼晶晶
                        statups.put(MapleBuffStat.火眼晶晶, (ret.getInfo().get(MapleStatInfo.x) << 8) + ret.getInfo().get(MapleStatInfo.y) + ret.getInfo().get(MapleStatInfo.criticaldamageMax));
                        break;
                    case 8003: //好用的神圣之火
                        statups.put(MapleBuffStat.MAXHP, ret.getInfo().get(MapleStatInfo.x));
                        statups.put(MapleBuffStat.MAXMP, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 8004: //强化战斗命令
                        statups.put(MapleBuffStat.战斗命令, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 8005: //强化进阶祝福
                        statups.clear();
                        statups.put(MapleBuffStat.进阶祝福, ret.getInfo().get(MapleStatInfo.x));
                        statups.put(MapleBuffStat.增加最大HP, ret.getInfo().get(MapleStatInfo.indieMhp));
                        statups.put(MapleBuffStat.增加最大MP, ret.getInfo().get(MapleStatInfo.indieMmp));
                        break;
                    case 8006: //强化极速领域
                        statups.put(MapleBuffStat.极速领域, ret.getInfo().get(MapleStatInfo.x));
                        break;
                    case 169://九死一生
                        statups.put(MapleBuffStat.九死一生, 1);
                        ret.getInfo().put(MapleStatInfo.time, 0);
                        ret.setOverTime(true);
                        break;
                }
            }
        } else {
            switch (sourceid) {
                case 2022746: //天使的祝福
                case 2022747: //黑天使的祝福
                case 2022823: //白天使的祝福
                    statups.clear(); //no atk/matk
                    statups.put(MapleBuffStat.天使状态, 1);
                    int value = sourceid == 2022746 ? 5 : sourceid == 2022747 ? 10 : 12;
                    statups.put(MapleBuffStat.增加物理攻击力, value);
                    statups.put(MapleBuffStat.增加魔法攻击力, value);
                    break;
            }
        }
        if (ret.isPoison()) {
            monsterStatus.put(MonsterStatus.MOB_STAT_Poison, 1);
        }
        if (ret.getSummonMovementType() != null && !ret.is嗨兄弟()) {
            statups.put(MapleBuffStat.召唤兽, 1);
        }
        if (ret.isMorph()) {
            statups.put(MapleBuffStat.变身效果, ret.getMorph());
            if (ret.is狂龙变形() || ret.is狂龙超级变形()) {
                statups.put(MapleBuffStat.稳如泰山, ret.getInfo().get(MapleStatInfo.prop));
                statups.put(MapleBuffStat.爆击提升, ret.getInfo().get(MapleStatInfo.cr));
                statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
                statups.put(MapleBuffStat.提高攻击速度, ret.getInfo().get(MapleStatInfo.indieBooster));
            }
        }
        if (ret.is传说冒险家() || ret.is守护者之荣誉() || ret.is英雄奥斯() || ret.is自由之墙() || ret.is姬儿的加持() || ret.is明日祝福()) {
            statups.put(MapleBuffStat.提升伤害百分比, ret.getInfo().get(MapleStatInfo.indieDamR));
        }
        if (ret.is超越攻击状态()) {
            statups.clear();
            ret.getInfo().put(MapleStatInfo.time, 15000);
            statups.put(MapleBuffStat.超越攻击, 1);
        }
//        statups.trimToSize(); //去掉多余申请的内存空间
        ret.setStatups(statups);
        ret.setMonsterStatus(monsterStatus);
        return ret;
    }

    /**
     * 获取骑宠的 MountId
     */
    public static int parseMountInfo(MapleCharacter player, int skillid) {
        if (skillid == 80001000 || SkillConstants.is骑兽技能(skillid)) {
            if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -123) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -124) != null) {
                return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -123).getItemId();
            }
            return parseMountInfo_Pure(player, skillid);
        } else {
            return GameConstants.getMountItem(skillid, player);
        }
    }

    static int parseMountInfo_Pure(MapleCharacter player, int skillid) {
        if (skillid == 80001000 || SkillConstants.is骑兽技能(skillid)) {
            if (player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18) != null && player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
                return player.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItemId();
            }
            return 0;
        } else {
            return GameConstants.getMountItem(skillid, player);
        }
    }

    static int makeHealHP(double rate, double stat, double lowerfactor, double upperfactor) {
        return (int) ((Math.random() * ((int) (stat * upperfactor * rate) - (int) (stat * lowerfactor * rate) + 1)) + (int) (stat * lowerfactor * rate));
    }

    public static Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft, Point lt, Point rb, int range) {
        if (lt == null || rb == null) {
            return new Rectangle((facingLeft ? (-200 - range) : 0) + posFrom.x, (-100 - range) + posFrom.y, 200 + range, 100 + range);
        }
        Point mylt;
        Point myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x - range, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x + range, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }
}
