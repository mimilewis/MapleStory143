package server;

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillFactory;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.*;
import constants.skills.*;
import handling.channel.ChannelServer;
import handling.opcode.EffectOpcode;
import handling.world.party.MaplePartyCharacter;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import server.Timer.BuffTimer;
import server.life.MapleMonster;
import server.maps.*;
import server.skill.MapleForeignBuffSkill;
import server.skill.MapleForeignBuffStat;
import tools.*;
import tools.packet.BuffPacket;
import tools.packet.EffectPacket;
import tools.packet.SummonPacket;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

import static server.MapleStatInfo.z;

@Data
@Log4j2
public class MapleStatEffect implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private Map<MapleStatInfo, Integer> info;
    private Map<MapleTraitType, Integer> traits;
    private boolean overTime, skill, partyBuff = true;
    private boolean notRemoved; //不能取消的BUFF
    private boolean repeatEffect; //自动重复使用的BUFF
    private boolean refreshstyle = false;// 是否需要刷新外观到其他玩家
    private MapleForeignBuffSkill foreign;
    private EnumMap<MapleBuffStat, Integer> statups;
    private List<Pair<Integer, Integer>> availableMap;
    private Map<MonsterStatus, Integer> monsterStatus;
    private Point lt, rb;
    private byte level;
    //private List<Pair<Integer, Integer>> randomMorph;
    private List<MapleDisease> cureDebuffs;
    private List<Integer> petsCanConsume, familiars, randomPickup;
    private List<Triple<Integer, Integer, Integer>> rewardItem;
    private byte slotCount, slotPerLine; //矿(药)背包道具需要
    private byte expR, recipeUseCount, recipeValidDay, reqSkillLevel, effectedOnAlly, effectedOnEnemy, type, preventslip, immortal, bs;
    private short ignoreMob, mesoR, thaw, lifeId, imhp, immp, inflation, useLevel, indiePdd, indieMdd, mobSkill, mobSkillLevel; // incPVPdamage,
    private double hpR, mpR;
    private int sourceid, recipe, moveTo, moneyCon, morphId = 0, expinc, exp, consumeOnPickup, charColor, interval, rewardMeso, totalprob, cosmetic;
    private int expBuff, itemup, mesoup, cashup, berserk, illusion, booster, berserk2;
    private boolean ruleOn;
    private boolean bxi = false;

    public MapleStatEffect() {

    }

    public static void apply祈祷众生(MapleCharacter player) {
        int skillLevel = player.getSkillLevel(主教.祈祷众生);
        if (skillLevel > 0) {
            Skill skill;
            int n3 = 1;
            if (player.getParty() != null) {
                n3 = player.getParty().getPartyBuffs(player.getId());
            }
            if ((skill = SkillFactory.getSkill(主教.祈祷众生)) == null) {
                return;
            }
            MapleStatEffect effect = player.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
            int n4 = effect.getX();
            skillLevel = player.getSkillLevel(主教.祈祷和音);
            if (skillLevel > 0 && (skill = SkillFactory.getSkill(主教.祈祷和音)) != null) {
                MapleStatEffect effect1 = player.inPVP() ? skill.getPVPEffect(skillLevel) : skill.getEffect(skillLevel);
                n4 = effect1.getX();
            }
            effect.getStatups().clear();
            effect.getStatups().put(MapleBuffStat.祈祷众生, n4 * n3);
            if (player.isAdmin() && ServerConstants.isShowPacket()) {
                player.dropSpouseMessage(10, "发送主教特性增益技能, 增益基础：" + n4 + " 人数：" + n3 + " 总增益：" + n4 * n3);
            }
//            effect.applyBuffEffect(player, 2100000000, true);
        }
    }

    /**
     * 添加被动效果
     *
     * @param applyto 角色
     * @param obj     对象
     */
    public void applyPassive(MapleCharacter applyto, MapleMapObject obj) {
        /*判断技能是否有概率获得特定的增益效果*/
        if (makeChanceResult()) {
            /*sourceid 技能ID，此处单独引用 sourceid 无需判断是否为 skill*/
            switch (sourceid) {
                case 火毒.魔力吸收:
                case 冰雷.魔力吸收:
                case 主教.魔力吸收:
                    if (obj == null || obj.getType() != MapleMapObjectType.MONSTER) {
                        /*如果对象为空或者对象的类型不是怪物就直接返回*/
                        return;
                    }
                    MapleMonster mob = (MapleMonster) obj;
                    /*取当前怪物的状态信息，判断是否为BOSS*/
                    if (!mob.getStats().isBoss()) {
                        /* absorbMp 吸收MP的计算方法：技能X值除以100乘以怪物的最大MP，得到的结果如果小于或等于怪物当前的MP，就赋值给 absorbMp 反之 将怪物当前的mp赋值给 absorbMp*/
                        int absorbMp = Math.min((int) (mob.getMobMaxMp() * (getX() / 100.0)), mob.getMp());
                        /* 判断吸收MP的结果值是否大于0*/
                        if (absorbMp > 0) {
                            /*设置怪物当前的MP：怪物当前的MP减去被吸收的MP。*/
                            mob.setMp(mob.getMp() - absorbMp);
                            /*设置角色当前的MP：角色当前的MP加上吸收到的MP*/
                            applyto.getStat().setMp(applyto.getStat().getMp() + absorbMp, applyto);
                            /*发送给角色吸收MP的效果包*/
                            applyto.getClient().announce(EffectPacket.showOwnBuffEffect(sourceid, 1, applyto.getLevel(), level));
                            /*发送给角色当前所在地图其他玩家的效果广播包*/
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto, sourceid, 1, applyto.getLevel(), level), false);
                        }
                    }
                    break;
            }
        }
    }

    /**
     * 给角色BUFF
     *
     * @param chr - 角色
     * @return
     */
    public boolean applyTo(MapleCharacter chr) {
        return applyTo(chr, chr, true, null, getDuration(chr), false);
    }

    /**
     * 给角色BUFF
     *
     * @param chr     角色
     * @param passive 是否被动使用
     * @return
     */
    public boolean applyTo(MapleCharacter chr, boolean passive) {
        return applyTo(chr, chr, true, null, getDuration(chr), passive);
    }

    /**
     * 给角色BUFF
     *
     * @param chr - 角色
     * @param pos - 坐标范围
     * @return
     */
    public boolean applyTo(MapleCharacter chr, Point pos) {
        return applyTo(chr, chr, true, pos, getDuration(chr), false);
    }

    /**
     * 给角色BUFF
     *
     * @param chr     - 角色
     * @param pos     - 坐标范围
     * @param passive - 是否被动使用
     * @return
     */
    public boolean applyTo(MapleCharacter chr, Point pos, boolean passive) {
        return applyTo(chr, chr, true, pos, getDuration(chr), passive);
    }

    public boolean applyTo(MapleCharacter chr, Point pos, int subValue) {
        return applyTo(chr, chr, true, pos, getDuration(chr), false, subValue);
    }

    /**
     * 给角色BUFF
     *
     * @param applyfrom
     * @param applyto
     * @param primary
     * @param pos         - 坐标范围
     * @param newDuration - 持续时间
     * @return
     */
    public boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos, int newDuration) {
        return applyTo(applyfrom, applyto, primary, pos, newDuration, false);
    }

    public boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos, int newDuration, boolean passive) {
        return applyTo(applyfrom, applyto, primary, pos, newDuration, passive, 0);
    }

    /**
     * 给角色BUFF
     *
     * @param applyfrom
     * @param applyto
     * @param primary
     * @param pos         - 坐标范围
     * @param newDuration - 持续时间
     * @param passive     - 是否被动使用
     * @return
     */
    public boolean applyTo(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, Point pos, int newDuration, boolean passive, int subValue) {
        if (applyfrom.isSkillCooling(sourceid)) {
            if (!skill) {
                return false;
            }
        }
        if (is群体治愈() && (applyfrom.getMapId() == 749040100 || applyto.getMapId() == 749040100 || applyfrom.getMap().isPvpMaps())) { //隐藏地图 - 纯净雪人栖息地
            applyfrom.getClient().announce(MaplePacketCreator.enableActions());
            return false;
        } else if ((isSoaring_Mount() && applyfrom.getBuffedValue(MapleBuffStat.骑兽技能) == null) || (isSoaring_Normal() && !applyfrom.getMap().canSoar())) {
            applyfrom.getClient().announce(MaplePacketCreator.enableActions());
            return false;
        } else if (sourceid == 双刀.傀儡召唤 && applyfrom.getBuffedValue(MapleBuffStat.影分身) == null) {
            applyfrom.getClient().announce(MaplePacketCreator.enableActions());
            return false;
        } else if (JobConstants.is隐士(applyfrom.getJob())) {
            Skill expert = SkillFactory.getSkill(隐士.娴熟飞镖术);
            if (applyfrom.getTotalSkillLevel(expert) > 0) {
                MapleStatEffect eff = expert.getEffect(applyfrom.getTotalSkillLevel(expert));
                if (eff.makeChanceResult()) {
                    applyfrom.getClient().announce(EffectPacket.showExpertEffect());
                } else {
                    passive = false;
                }
            }
        }

        if (primary) {
            if (info.get(MapleStatInfo.itemConNo) != 0 && !applyto.inPVP()) {
                if (!applyto.haveItem(info.get(MapleStatInfo.itemCon), info.get(MapleStatInfo.itemConNo), false, true)) {
                    applyto.getClient().announce(MaplePacketCreator.enableActions());
                    return false;
                }
                MapleInventoryManipulator.removeById(applyto.getClient(), ItemConstants.getInventoryType(info.get(MapleStatInfo.itemCon)), info.get(MapleStatInfo.itemCon), info.get(MapleStatInfo.itemConNo), false, true);
            }
        }

        /*
         * 处理主动施放技能消耗的HPMP等,一些攻击的就不需要再次扣除HPMP了
         */
        if (!passive) {
            int hpchange = calcHPChange(applyfrom, primary);
            int mpchange = calcMPChange(applyfrom, primary);
            PlayerStats stat = applyto.getStat();
            if (!primary && is复活术()) {
                hpchange = stat.getMaxHp();
                applyto.setStance(0); //TODO fix death bug, player doesnt spawn on other screen
            } else if (is负荷释放()) {
                if (applyto.get超越数值() < 20) {
                    applyfrom.dropMessage(5, "该技能处于未激活状态。");
                    applyto.getClient().announce(MaplePacketCreator.enableActions());
                    return false;
                }
                applyto.cancelEffectFromBuffStat(MapleBuffStat.恶魔超越);
                hpchange = (stat.getMaxHp() / 100) * getX();
            } else if (is额外供给() || is永动引擎()) {
                applyto.addPowerCount(is额外供给() ? info.get(MapleStatInfo.x) : applyto.getPowerCountByJob());
            }
            List<Pair<MapleStat, Long>> hpmpupdate = new ArrayList<>(2);
            if (hpchange != 0) {
                if (hpchange < 0 && (-hpchange) > stat.getHp() && !applyto.hasDisease(MapleDisease.不死化)) {
                    applyto.getClient().announce(MaplePacketCreator.enableActions());
                    return false;
                }
                stat.setHp(stat.getHp() + hpchange, applyto);
                hpmpupdate.add(new Pair<>(MapleStat.HP, (long) stat.getHp()));
            }
            if (mpchange != 0) {
                if (JobConstants.is超能力者(applyto.getJob())) {
                    applyto.gainPP(mpchange);
                } else {
                    if (mpchange < 0 && (-mpchange) > stat.getMp()) {
                        applyto.getClient().announce(MaplePacketCreator.enableActions());
                        return false;
                    }
                    stat.setMp(stat.getMp() + mpchange, applyto);
                    hpmpupdate.add(new Pair<>(MapleStat.MP, (long) stat.getMp()));
                }
            }
            applyto.getClient().announce(MaplePacketCreator.updatePlayerStats(hpmpupdate, true, applyto));
        }
        //尖兵使用技能需要的能量点数
        int powerchange = calcPowerChange(applyfrom);
        if (powerchange != 0) {
            if (powerchange < 0 && (-powerchange) > applyfrom.getSpecialStat().getPowerCount()) {
                applyfrom.dropMessage(5, "施展技能所需的支援能量不足。");
                return false;
            }
            applyto.addPowerCount(powerchange);
        }
        if (expinc != 0) {
            applyto.gainExp(expinc, true, true, false);
//            applyto.getClient().announce(EffectPacket.showSpecialEffect(0x17));
        } else if (isReturnScroll()) { //回城卷处理
            applyReturnScroll(applyto);
        } else if (useLevel > 0 && !skill) {
            applyto.setExtractor(new MapleExtractor(applyto, sourceid, useLevel * 50, 1440)); //no clue about time left
            applyto.getMap().spawnExtractor(applyto.getExtractor());
        } else if (is净化() && makeChanceResult() || is勇士的意志()) {
            applyto.dispelDebuffs();
        } else if (cureDebuffs.size() > 0) {
            for (MapleDisease debuff : cureDebuffs) {
                applyfrom.dispelDebuff(debuff);
            }
        } else if (is龙之献祭()) {
            applyto.dispelSkill(黑骑士.灵魂助力);
            if (applyto.skillisCooling(黑骑士.神枪降临)) {
                applyto.removeCooldown(黑骑士.神枪降临);
                applyto.getClient().announce(MaplePacketCreator.skillCooldown(黑骑士.神枪降临, 0));
            }
        } else if (is迷雾爆发()) {
            int i = info.get(MapleStatInfo.y);
            for (MapleMist mist : applyto.getMap().getAllMistsThreadsafe()) {
                if (mist.getOwnerId() == applyto.getId() && mist.getSourceSkill().getId() == 火毒.致命毒雾) {
                    if (mist.getSchedule() != null) {
                        mist.getSchedule().cancel(false);
                        mist.setSchedule(null);
                    }
                    if (mist.getPoisonSchedule() != null) {
                        mist.getPoisonSchedule().cancel(false);
                        mist.setPoisonSchedule(null);
                    }
                    applyto.getMap().broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId(), true));
                    applyto.getMap().removeMapObject(mist);
                    i--;
                    if (i <= 0) {
                        break;
                    }
                }
            }
        } else if (cosmetic > 0) {
            if (cosmetic >= 30000) {
                applyto.setHair(cosmetic);
                applyto.updateSingleStat(MapleStat.发型, cosmetic);
            } else if (cosmetic >= 20000) {
                applyto.setFace(cosmetic);
                applyto.updateSingleStat(MapleStat.脸型, cosmetic);
            } else if (cosmetic < 100) {
                applyto.setSkinColor((byte) cosmetic);
                applyto.updateSingleStat(MapleStat.皮肤, cosmetic);
            }
            applyto.equipChanged();
        } else if (recipe > 0) {
            if (applyto.getSkillLevel(recipe) > 0 || applyto.getProfessionLevel((recipe / 10000) * 10000) < reqSkillLevel) {
                return false;
            }
            applyto.changeSingleSkillLevel(SkillFactory.getCraft(recipe), Integer.MAX_VALUE, recipeUseCount, recipeValidDay > 0 ? (System.currentTimeMillis() + recipeValidDay * 24L * 60 * 60 * 1000) : -1L);
        } else if (is卡牌审判()) {
            if (applyto.getCardStack() < applyto.getCarteByJob()) {
                applyfrom.dropMessage(5, "必须等卡片值充满后，才能使用技能。");
                return false;
            }
            applyto.setCardStack(0);
        } else if (is狂龙变形()) {
            if (applyto.getMorphCount() < 700) {
                applyfrom.dropMessage(5, "变形值不足，无法使用该技能。");
                return false;
            }
            applyto.setMorphCount(0);
        } else if (is暗器伤人()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            boolean itemz = false;
            int bulletConsume = info.get(MapleStatInfo.bulletConsume);
            for (int i = 0; i < use.getSlotLimit(); i++) { // impose order...
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (ItemConstants.is飞镖道具(item.getItemId()) && item.getQuantity() >= bulletConsume) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i, (short) bulletConsume, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }
        } else if (is无限子弹()) {
            MapleInventory use = applyto.getInventory(MapleInventoryType.USE);
            boolean itemz = false;
            int bulletConsume = info.get(MapleStatInfo.bulletConsume);
            for (int i = 0; i < use.getSlotLimit(); i++) {
                Item item = use.getItem((byte) i);
                if (item != null) {
                    if (ItemConstants.is子弹道具(item.getItemId()) && item.getQuantity() >= bulletConsume) {
                        MapleInventoryManipulator.removeFromSlot(applyto.getClient(), MapleInventoryType.USE, (short) i, (short) bulletConsume, false, true);
                        itemz = true;
                        break;
                    }
                }
            }
            if (!itemz) {
                return false;
            }
        } else if ((effectedOnEnemy > 0 || effectedOnAlly > 0) && primary && applyto.inPVP()) {
            int types = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
            if (types > 0 || effectedOnEnemy > 0) {
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (effectedOnAlly > 0 ? (chr.getTeam() == applyto.getTeam()) : (chr.getTeam() != applyto.getTeam() || types == 0))) {
                        applyTo(applyto, chr, false, pos, newDuration);
                    }
                }
            }
        } else if (mobSkill > 0 && mobSkillLevel > 0 && primary && applyto.inPVP()) {
            if (effectedOnEnemy > 0) {
                int types = Integer.parseInt(applyto.getEventInstance().getProperty("type"));
                for (MapleCharacter chr : applyto.getMap().getCharactersThreadsafe()) {
                    if (chr.getId() != applyto.getId() && (chr.getTeam() != applyto.getTeam() || types == 0)) {
                        chr.disease(mobSkill, mobSkillLevel);
                    }
                }
            } else {
                if (sourceid == 2910000 || sourceid == 2910001) { //red flag
                    applyto.getClient().announce(EffectPacket.showOwnBuffEffect(sourceid, EffectOpcode.UserEffect_JobChanged.getValue(), applyto.getLevel(), level));
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto, sourceid, EffectOpcode.UserEffect_JobChanged.getValue(), applyto.getLevel(), level), false);
                    applyto.getClient().announce(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Effect", 0, 0));
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Effect", 0, 0), false);
                    if (applyto.getTeam() == (sourceid - 2910000)) { //restore duh flag
                        if (sourceid == 2910000) {
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been restored.");
                        } else {
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been restored.");
                        }
                        applyto.getMap().spawnAutoDrop(sourceid, applyto.getMap().getGuardians().get(sourceid - 2910000).left);
                    } else {
                        applyto.disease(mobSkill, mobSkillLevel);
                        if (sourceid == 2910000) {
                            applyto.getEventInstance().setProperty("redflag", String.valueOf(applyto.getId()));
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Red Team's flag has been captured!");
                            applyto.getClient().announce(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Tail/Red", 600000, 0));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Tail/Red", 600000, 0), false);
                        } else {
                            applyto.getEventInstance().setProperty("blueflag", String.valueOf(applyto.getId()));
                            applyto.getEventInstance().broadcastPlayerMsg(-7, "The Blue Team's flag has been captured!");
                            applyto.getClient().announce(EffectPacket.showOwnCraftingEffect("UI/UIWindow2.img/CTF/Tail/Blue", 600000, 0));
                            applyto.getMap().broadcastMessage(applyto, EffectPacket.showCraftingEffect(applyto.getId(), "UI/UIWindow2.img/CTF/Tail/Blue", 600000, 0), false);
                        }
                    }
                } else {
                    applyto.disease(mobSkill, mobSkillLevel);
                }
            }
        } else if (randomPickup != null && randomPickup.size() > 0) {
            MapleItemInformationProvider.getInstance().getItemEffect(randomPickup.get(Randomizer.nextInt(randomPickup.size()))).applyTo(applyto);
        }
        for (Entry<MapleTraitType, Integer> traitType : traits.entrySet()) {
            applyto.getTrait(traitType.getKey()).addExp(traitType.getValue(), applyto);
        }
        if (is机械传送门()) {
            int newId = 0;
            boolean applyBuff = false;
            if (applyto.getMechDoors().size() >= 2) {
                MechDoor remove = applyto.getMechDoors().remove(0);
                newId = remove.getId();
                applyto.getMap().broadcastMessage(MaplePacketCreator.removeMechDoor(remove, true));
                applyto.getMap().removeMapObject(remove);
            } else {
                for (MechDoor d : applyto.getMechDoors()) {
                    if (d.getId() == newId) {
                        applyBuff = true;
                        newId = 1;
                        break;
                    }
                }
            }
            MechDoor door = new MechDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), newId);
            applyto.getMap().spawnMechDoor(door);
            applyto.addMechDoor(door);
            applyto.getClient().announce(MaplePacketCreator.mechPortal(door.getTruePosition()));
            if (!applyBuff) {
                return true; //do not apply buff until 2 doors spawned
            }
        }
        if (primary && availableMap != null) {
            for (Pair<Integer, Integer> e : availableMap) {
                if (applyto.getMapId() < e.left || applyto.getMapId() > e.right) {
                    applyto.getClient().announce(MaplePacketCreator.enableActions());
                    return true;
                }
            }
        }

        // 防止眼花
        if ((overTime || statups.size() > 0) && !is能量获得() && !is超越攻击()) {
            if (getSummonMovementType() != null && !is嗨兄弟()) {
                boolean applyBuff = false;
                if (is全息力场支援() || is元素火焰()) {
                    applyBuff = applyfrom.hasSummonBySkill(sourceid);
                }
                if (applyBuff) {
                    if (applyfrom.isShowPacket()) {
                        applyfrom.dropDebugMessage(1, "[BUFF信息] BUFFID: " + sourceid + " 持续时间: " + newDuration + " 群体: " + isPartyBuff() + " 被动: " + passive);
                    }
                    applyBuffEffect(applyfrom, applyto, primary, newDuration, passive);
                } else {
                    applySummonEffect(applyfrom, primary, pos, newDuration, subValue);
                }
            } else if (is三彩箭矢()) {
                applyArrowsBuff(applyfrom, false);
            } else {
                if (applyfrom.isShowPacket()) {
                    applyfrom.dropDebugMessage(1, "[BUFF信息] " + SkillFactory.getSkillName(sourceid) + "(" + sourceid + ") 持续时间: " + newDuration + " 群体: " + isPartyBuff() + " 被动: " + passive);
                }
                applyBuffEffect(applyfrom, applyto, primary, newDuration, passive);
            }
        }

        if (skill) {
            removeMonsterBuff(applyfrom);
        }
        if (primary) {
            if ((overTime || is群体治愈()) && !is能量获得()) {
                applyPartyBuff(applyfrom, newDuration);
            }
            if (isMonsterBuff()) {
                applyMonsterBuff(applyfrom);
            }
        }
        if (is时空门()) {
            MapleDoor door = new MapleDoor(applyto, new Point(pos == null ? applyto.getTruePosition() : pos), sourceid); // 在当前地图中创建一个门
            if (door.getTownPortal() != null) {
                applyto.getMap().spawnDoor(door);
                applyto.addDoor(door);
                MapleDoor townDoor = new MapleDoor(door); // 创建临镇的门
                applyto.addDoor(townDoor);
                door.getTown().spawnDoor(townDoor);
                if (applyto.getParty() != null) { // 更新临镇的门
                    applyto.silentPartyUpdate();
                }
            } else {
                applyto.dropMessage(5, "村庄里已经没有可开启时空门的位置。");
            }
        } else if (isMist()) {
            Rectangle bounds = calculateBoundingBox(pos != null ? pos : applyfrom.getPosition(), applyfrom.isFacingLeft());
            MapleMist mist = new MapleMist(bounds, applyfrom, this, applyfrom.getPosition());
            applyfrom.getMap().spawnMist(mist, getDuration(), false);
        } else if (is伺机待发()) {
            for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                if (i.skillId != 冲锋队长.伺机待发) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().announce(MaplePacketCreator.skillCooldown(i.skillId, 0));
                }
            }
        } else if (is幸运钱()) {
            applyto.switchLuckyMoney(false);
        } else if (is狂龙战士的威严()) {
            for (MapleCoolDownValueHolder i : applyto.getCooldowns()) {
                if (i.skillId != 狂龙战士.狂龙战士的威严 && i.skillId != 狂龙战士.日珥 && i.skillId != 狂龙战士.终极变形_超级) {
                    applyto.removeCooldown(i.skillId);
                    applyto.getClient().announce(MaplePacketCreator.skillCooldown(i.skillId, 0));
                }
            }
        } else if (is金钱炸弹()) {
            applyto.handleMesoExplosion();
        } else if (is能量激发()) {
            applyfrom.setEnergyCount(800);
        } else if (is美洲豹技能()) {
            applyfrom.getClient().announce(MaplePacketCreator.美洲豹攻击效果(sourceid));
        }
        /*
         * 随机获得金币
         */
        if (rewardMeso != 0) {
            applyto.gainMeso(rewardMeso, false);
        }
        /*
         * 随机获得道具
         */
        if (rewardItem != null && totalprob > 0) {
            for (Triple<Integer, Integer, Integer> reward : rewardItem) {
                if (MapleInventoryManipulator.checkSpace(applyto.getClient(), reward.left, reward.mid, "") && reward.right > 0 && Randomizer.nextInt(totalprob) < reward.right) { // Total prob
                    if (ItemConstants.getInventoryType(reward.left) == MapleInventoryType.EQUIP) {
                        Item item = MapleItemInformationProvider.getInstance().getEquipById(reward.left);
                        item.setGMLog("Reward item (effect): " + sourceid + " on " + DateUtil.getCurrentDate());
                        MapleInventoryManipulator.addbyItem(applyto.getClient(), item);
                    } else {
                        MapleInventoryManipulator.addById(applyto.getClient(), reward.left, reward.mid.shortValue(), "Reward item (effect): " + sourceid + " on " + DateUtil.getCurrentDate());
                    }
                }
            }
        }
        if (skill && info.get(MapleStatInfo.onActive) > 0) {
            //停止技能激活
            applyto.getClient().announce(MaplePacketCreator.skillNotActive(sourceid));
            //激活技能的使用
            applyto.getClient().announce(MaplePacketCreator.skillActive());
            //技能激活的效果
            if (JobConstants.is爆莉萌天使(applyto.getJob())) {
                applyto.getClient().announce(EffectPacket.showSpecialEffect(EffectOpcode.UserEffect_ResetOnStateForOnOffSkill.getValue()));
            }
        }
        //如果角色是夜光3转或4转就自动BUFF生命潮汐
        if (applyfrom.getJob() == 2711 || applyfrom.getJob() == 2712) {
            applyfrom.check生命潮汐();
        }
        return true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void applySummonEffect(MapleCharacter player, long time, boolean bl2) {
        int cooldown = this.getCooldown(player);
        if (this.sourceid == 英雄.燃灵之剑) {
            cooldown = this.getX();
        }
        if (cooldown > 0) {
            int n3 = SkillConstants.getLinkedAttackSkill(sourceid);
            if (sourceid == 主教.神圣保护 || sourceid == 夜行者.黑暗重生 || sourceid == 幻影.神秘的运气) {
                return;
            }
            if (sourceid == 机械师.磁场) {
                ArrayList<Integer> arrayList;
                arrayList = new ArrayList<>();
                List<MapleSummon> list = player.getSummonsReadLock();
                try {
                    for (MapleSummon ah2 : list) {
                        if (ah2.getSkillId() != sourceid) continue;
                        arrayList.add(ah2.getObjectId());
                    }
                } finally {
                    player.unlockSummonsReadLock();
                }
                if (arrayList.size() < 3) {
                    return;
                }
                player.getMap().broadcastMessage(MaplePacketCreator.teslaTriangle(player.getId(), arrayList.get(0), arrayList.get(1), arrayList.get(2)));
            }
            if (!player.skillisCooling(n3 == 黑骑士.重生契约 ? sourceid : n3)) {
                player.addCooldown(n3 == 黑骑士.重生契约 ? sourceid : n3, time, cooldown * 1000);
            }
        }
    }

    public boolean applySummonEffect(MapleCharacter applyto, boolean primary, Point pos, int newDuration, int subValue) {
        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType == null) {
            return false;
        }
        byte[] buff = null;
        int summonSkillId = sourceid;
        MapleStatEffect localeffect = this;
        Map<MapleBuffStat, Integer> localstatups = new EnumMap<>(statups);
        if (applyto.isShowPacket()) {
            applyto.dropDebugMessage(1, "[召唤兽] 开始召唤 - 召唤兽技能ID: " + summonSkillId + " 持续时间: " + newDuration);
        }
        //取消BUFF 不用发送封包
        if (sourceid != 机械师.磁场 && sourceid != 夜行者.影子蝙蝠_召唤兽 && sourceid != 林之灵.嗨_兄弟 && !SkillConstants.is美洲豹(sourceid)) {
            applyto.cancelEffect(this, true, -1, localstatups);
        }

        if (this.sourceid == 双弩.精灵骑士) {
            summonSkillId += Randomizer.nextInt(3);
        }
        //设置和刷出召唤兽
        if (subValue > 0) {
            MapleMonster monster = applyto.getMap().getMonsterByOid(subValue);
            if (monster == null) {
                return false;
            } else {
                subValue = monster.getId();
            }
        }
        MapleSummon tosummon = new MapleSummon(applyto, summonSkillId, getLevel(), new Point(pos == null ? applyto.getTruePosition() : pos), summonMovementType, newDuration, subValue);

        //元素火焰刷新比较频繁，判断一下比较好
        applyto.getMap().spawnSummon(tosummon); //刷出召唤兽
        applyto.addSummon(tosummon); //在角色的召唤兽列表中添加 召唤兽信息

        if (is集合船员()) {
            int skilllevel = applyto.getTotalSkillLevel(船长.指挥船员);
            if (skilllevel > 0) {
                SkillFactory.getSkill(船长.指挥船员).getEffect(skilllevel).applyBuffEffect(applyto, applyto, primary, newDuration);
            }
        } else if (info.get(MapleStatInfo.hcSummonHp) > 0) { //默认设置的召唤兽的HP为 1
            tosummon.setSummonHp(info.get(MapleStatInfo.hcSummonHp)); //设置召唤兽的血量 也就是替身术的血量 多少后消失
        } else if (sourceid == 箭神.神箭幻影) { //这个技能召唤兽的血为 x
            tosummon.setSummonHp(info.get(MapleStatInfo.x));
        } else if (sourceid == 风灵使者.钻石星尘) {
            applyto.dispelSkill(风灵使者.绿水晶花);
        } else if (sourceid == 双刀.傀儡召唤) {
            applyto.cancelEffectFromBuffStat(MapleBuffStat.影分身);
        } else if (is灵魂助力()) {
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.灵魂助力, 1);
            buff = BuffPacket.giveBuff(sourceid, newDuration, stat, this, applyto);
        } else if (is元素火焰()) {
            localstatups = Collections.singletonMap(MapleBuffStat.召唤兽, 1);
//            int skilllevel = applyto.getTotalSkillLevel(sourceid);
//            if (skilllevel > 0) {
//                SkillFactory.getSkill(sourceid).getEffect(skilllevel).applyBuffEffect(applyto, applyto, primary, newDuration);
//            }
        } else if (sourceid == 炎术士.火焰化身_狮子 || sourceid == 炎术士.火焰化身_狐狸) {
            applyto.dispelSkill(sourceid == 炎术士.火焰化身_狮子 ? 炎术士.火焰化身_狐狸 : 炎术士.火焰化身_狮子);
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.百分比无视防御, applyto.getTotalSkillLevel(炎术士.火焰化身));
            buff = BuffPacket.giveBuff(sourceid, newDuration, stat, this, applyto);
        } else if (is全息力场支援()) {
            localstatups = Collections.singletonMap(MapleBuffStat.召唤兽, 1);
        } else if (is影子侍从()) {
            localstatups.put(MapleBuffStat.影子侍从, 1);
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.影子侍从, 1);
            buff = BuffPacket.giveBuff(sourceid, newDuration, stat, this, applyto);
        } else if (is召唤美洲豹()) {
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.召唤美洲豹, info.get(MapleStatInfo.asrR) << 8 + info.get(MapleStatInfo.criticaldamageMin));
            localeffect = SkillFactory.getSkill(豹弩游侠.召唤美洲豹_灰).getEffect(1);
            buff = BuffPacket.giveBuff(localeffect.getSourceid(), newDuration, stat, localeffect, applyto);
            applyto.getClient().announce(MaplePacketCreator.openPantherAttack(true));
        } else if (is死亡契约()) {
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.死亡契约, 0);
            buff = BuffPacket.giveBuff(sourceid, newDuration, stat, this, applyto);
        } else if (is嗨兄弟()) {
            BuffTimer.getInstance().schedule(() -> {
                applyto.getMap().broadcastMessage(SummonPacket.removeSummon(tosummon, true));
                applyto.getMap().removeMapObject(tosummon);
                applyto.removeVisibleMapObject(tosummon);
                applyto.removeSummon(tosummon);
            }, newDuration * 1000);
            return true;
        }
        //设置BUFF技能的消失时间 和 注册角色的BUFF状态信息
        long startTime = System.currentTimeMillis();
        if (newDuration > 0) {
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, localeffect, startTime, localstatups);
            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, newDuration);
            applyto.registerEffect(localeffect, startTime, schedule, localstatups, false, newDuration, applyto.getId());
        } else {
            applyto.registerEffect(localeffect, startTime, null, localstatups, false, newDuration, applyto.getId());
        }
        //设置BUFF技能的冷却时间
        int cooldown = getCooldown(applyto);
        if (cooldown > 0) {
            if (sourceid == 机械师.磁场) {
                List<Integer> count = new ArrayList<>();
                List<MapleSummon> summons = applyto.getSummonsReadLock();
                try {
                    for (MapleSummon summon : summons) {
                        if (summon.getSkillId() == sourceid) {
                            count.add(summon.getObjectId());
                        }
                    }
                } finally {
                    applyto.unlockSummonsReadLock();
                }
                if (count.size() == 3) {
                    applyto.getClient().announce(MaplePacketCreator.skillCooldown(sourceid, cooldown));
                    applyto.addCooldown(sourceid, startTime, cooldown * 1000);
                    applyto.getMap().broadcastMessage(MaplePacketCreator.teslaTriangle(applyto.getId(), count.get(0), count.get(1), count.get(2)));
                }
            } else {
                if (applyto.skillisCooling(sourceid)) {
                    applyto.dropMessage(5, "技能由于冷却时间限制，暂时无法使用。");
                    applyto.getClient().announce(MaplePacketCreator.enableActions());
                    return false;
                } else {
                    if (applyto.isAdmin()) {
                        applyto.dropDebugMessage(2, "[技能冷却] 为GM消除技能冷却时间, 原技能冷却时间:" + cooldown + "秒.");
                    } else {
                        applyto.getClient().announce(MaplePacketCreator.skillCooldown(sourceid, cooldown));
                        applyto.addCooldown(sourceid, startTime, cooldown * 1000);
                    }
                }
            }
        }
        if (buff != null) {
            applyto.getClient().announce(buff);
        }
        return true;
    }

    /*
     * 回城卷处理
     */
    public boolean applyReturnScroll(MapleCharacter applyto) {
        if (moveTo != -1) {
            //applyto.getMap().getReturnMapId() != applyto.getMapId() || 暂时不要这个检测
            //特别课程邀请信 骑士卷轴 这个貌似还是检测不到 不管了
            MapleMap target = null;
            boolean nearest = false;
            if (moveTo == 999999999) {
                nearest = true;
                if (applyto.getMap().getReturnMapId() != 999999999) {
                    target = applyto.getMap().getReturnMap();
                }
            } else {
                target = ChannelServer.getInstance(applyto.getClient().getChannel()).getMapFactory().getMap(moveTo);
                if (target.getId() == 931050500 && target != applyto.getMap()) {
                    applyto.changeMap(target, target.getPortal(0));
                    return true;
                }
                int targetMapId = target.getId() / 10000000;
                int charMapId = applyto.getMapId() / 10000000;
                if (targetMapId != 60 && charMapId != 61) {
                    if (targetMapId != 21 && charMapId != 20) {
                        if (targetMapId != 12 && charMapId != 10) {
                            if (targetMapId != 10 && charMapId != 12) {
                                if (targetMapId != charMapId) {
                                    log.info("玩家 " + applyto.getName() + " 尝试回到一个非法的位置 (" + applyto.getMapId() + "->" + target.getId() + ")");
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
            if (target == applyto.getMap() || nearest && applyto.getMap().isTown()) {
                return false;
            }
            if (target != null) {
                applyto.changeMap(target, target.getPortal(0));
                return true;
            }
        }
        return false;
    }

//    private void applyBuff(MapleCharacter applyFrom, int duration) {
//        ArrayList<MapleCharacter> players = new ArrayList<>();
//        if (is灵魂之石()) {
//            if (applyFrom.getParty() != null) {
//                int n3 = 0;
//                for (MapleCharacter player : applyFrom.getMap().getCharactersThreadsafe()) {
//                    if (player.getParty() != null && player.getParty().getPartyId() == applyFrom.getParty().getPartyId() && player.isAlive()) {
//                        ++n3;
//                    }
//                }
//                while (players.size() < Math.min(n3, getY())) {
//                    for (MapleCharacter player : applyFrom.getMap().getCharactersThreadsafe()) {
//                        if (player != null && player.isAlive() && player.getParty() != null && player.getParty().getPartyId() == applyFrom.getParty().getPartyId() && !players.contains(player) && Randomizer.nextInt(getY()) == 0) {
//                            players.add(player);
//                        }
//                    }
//                }
//            }
//        } else if (this.jR() && (applyFrom.getParty() != null || isGmBuff() || applyFrom.inPVP())) {
//            Rectangle rectangle = calculateBoundingBox(applyFrom.getTruePosition(), applyFrom.isFacingLeft());
//            List<MapleMapObject> mapObjectsInRect = applyFrom.getMap().getMapObjectsInRect(rectangle, Collections.singletonList(MapleMapObjectType.PLAYER));
//            for (MapleMapObject mapObject : mapObjectsInRect) {
//                MapleCharacter player = (MapleCharacter) mapObject;
//                if ((sourceid % 10000 != 1053 || applyFrom.getJob() / 1000 == player.getJob() / 1000) && player.getId() != applyFrom.getId() && (isGmBuff() || (applyFrom.inPVP() && player.getTeam() == applyFrom.getTeam() && Integer.parseInt(applyFrom.getEventInstance().getProperty("type")) != 0) || (applyFrom.getParty() != null && player.getParty() != null && applyFrom.getParty().getPartyId() == player.getParty().getPartyId()))) {
//                    players.add(player);
//                }
//            }
//        }
//        for (MapleCharacter player : players) {
//            if (this.jR() && player.getParty() != null && !is群体治愈()) {
//                player.getParty().givePartyBuff(sourceid, applyFrom.getId(), player.getId());
//            }
//            if (is复活术() && !player.isAlive() || !is复活术() && player.isAlive()) {
//                applyBuffEffect(applyFrom, player, false, null, duration, true);
//                player.getClient().announce(EffectPacket.showOwnBuffEffect(sourceid, EffectOpcode.UserEffect_SkillAffected.getValue(), this.ln));
//                player.getMap().broadcastMessage(player, i.b.d.a(player, sourceid, (int)d.g.f.aZw.ex(), (int)applyFrom.getLevel(), (int)this.ln), false);
//            }
//            if (is伺机待发()) {
//                player.resetAllCooldowns(false);
//            }
//        }
//        if (is净化()) {
//            if (applyFrom.getParty() == null) {
//                return;
//            }
//            int n4 = applyFrom.getParty().getPartyBuffs(applyFrom.getId()) * getDuration();
//            if (n4 > 0) {
//                applyFrom.reduceCooldown(2311012, n4);
//            }
//            for (Object object : applyFrom.getParty().getMembers()) {
//                applyFrom.getParty().cancelPartyBuff(sourceid, object.getId());
//            }
//        } else if (this.jR() && !this.jS() && !this.jT()) {
//            if (applyFrom.getParty() != null) {
//                applyFrom.getParty().givePartyBuff(sourceid, applyFrom.getId(), applyFrom.getId());
//            }
//            apply祈祷众生(applyFrom);
//        }
//    }

    /*
     * 开始处理组队BUFF效果
     */
    private void applyPartyBuff(MapleCharacter applyfrom, int newDuration) {
        if (isPartyPassiveBuff() && applyfrom.getParty() != null) { //没有范围 检测全图地图的成员是否为组队成员
            for (MaplePartyCharacter partyMember : applyfrom.getParty().getMembers()) {
                MapleCharacter member = applyfrom.getMap().getCharacterById(partyMember.getId());
                if (member != null && member.getId() != applyfrom.getId() && member.isAlive()) {
                    applyTo(applyfrom, member, false, null, newDuration);
                }
            }
        } else if (isPartyBuff() && (applyfrom.getParty() != null || isGmBuff() || applyfrom.inPVP())) {
            Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
            List<MapleMapObject> affecteds = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.PLAYER));
            for (MapleMapObject affectedmo : affecteds) {
                MapleCharacter affected = (MapleCharacter) affectedmo;
                if (affected.getId() != applyfrom.getId() && (isGmBuff() || (applyfrom.inPVP() && affected.getTeam() == applyfrom.getTeam() && Integer.parseInt(applyfrom.getEventInstance().getProperty("type")) != 0) || (applyfrom.getParty() != null && affected.getParty() != null && applyfrom.getParty().getPartyId() == affected.getParty().getPartyId()))) {
                    boolean applyBuff = false;
                    if (is复活术() && !affected.isAlive()) {
                        applyBuff = true;
                    }
                    if (!is复活术() && affected.isAlive()) {
                        applyBuff = true;
                        if (is传说冒险家()) {
                            applyBuff = affected.getJob() >= 0 && affected.getJob() < 1000;
                        } else if (is守护者之荣誉()) {
                            applyBuff = affected.getJob() >= 1000 && affected.getJob() < 2000;
                        } else if (is英雄奥斯()) {
                            applyBuff = affected.getJob() >= 2000 && affected.getJob() < 3000;
                        } else if (is自由之墙()) {
                            applyBuff = affected.getJob() >= 3000 && affected.getJob() < 4000;
                        }
                    }
                    if (applyBuff) {
                        applyTo(applyfrom, affected, false, null, newDuration);
                        affected.getClient().announce(EffectPacket.showOwnBuffEffect(sourceid, EffectOpcode.UserEffect_SkillAffected.getValue(), applyfrom.getLevel(), level));
                        affected.getMap().broadcastMessage(affected, EffectPacket.showBuffeffect(affected, sourceid, EffectOpcode.UserEffect_SkillAffected.getValue(), applyfrom.getLevel(), level), false);
                    }
                    if (is伺机待发()) {
                        for (MapleCoolDownValueHolder i : affected.getCooldowns()) {
                            if (i.skillId != 冲锋队长.伺机待发) {
                                affected.removeCooldown(i.skillId);
                                affected.getClient().announce(MaplePacketCreator.skillCooldown(i.skillId, 0));
                            }
                        }
                    }
                }
            }
        }
    }

    public void h(MapleCharacter applyFrom, boolean bl2) {
        if (!bl2) {
            for (MapleMapObject object : applyFrom.getMap().getMapObjectsInRect(calculateBoundingBox(applyFrom.getTruePosition(), applyFrom.isFacingLeft()), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                MapleCharacter applyTo = (MapleCharacter) object;
                if ((applyTo.getParty() != null && applyTo.getParty().getMemberById(applyFrom.getId()) != null && applyTo.getBuffStats(this, -1).isEmpty()) || (applyTo.getId() == applyFrom.getId() && applyTo.getBuffStats(this, -1).isEmpty())) {
                    applyPartyBuff(applyFrom, getDuration());
                }
            }
            if (applyFrom.getMap().getCharacterById_InMap(applyFrom.getId()) != null && applyFrom.getMap().getCharacterById_InMap(applyFrom.getId()).getParty() != null) {
                for (MaplePartyCharacter member : applyFrom.getMap().getCharacterById_InMap(applyFrom.getId()).getParty().getMembers()) {
                    List list = applyFrom.getMap().getCharactersIntersect(calculateBoundingBox(applyFrom.getTruePosition(), applyFrom.isFacingLeft()));
                    MapleCharacter p3 = applyFrom.getMap().getCharacterById_InMap(member.getId());
                    if (list != null && p3 != null && !list.contains(p3) && p3.getBuffStats(this, -1) != null) {
                        applyFrom.getMap().getCharacterById_InMap(member.getId()).cancelEffect(this, false, -1);
                    }
                }
            }
        } else {
            if (getStatups().size() > 0) {
                for (MapleMapObject object : applyFrom.getMap().getMapObjectsInRect(calculateBoundingBox(applyFrom.getTruePosition(), applyFrom.isFacingLeft()), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                    ((MapleMonster) object).applyStatus(applyFrom, getStatups(), false, (long) getDuration(applyFrom), true, this);
                }
            }
        }
    }

    public boolean is灵魂之石() {
        return skill && sourceid == 22181003;
    }

    public void w(boolean bl2) {
        this.bxi = bl2;
    }

    public boolean jR() {
        if (lt == null || rb == null || !bxi) {
            return is灵魂之石();
        }
        return bxi;
    }

    private void removeMonsterBuff(MapleCharacter applyfrom) {
        List<MonsterStatus> cancel = new ArrayList<>();
        switch (sourceid) {
            case 英雄.魔击无效:
            case 圣骑士.魔击无效:
            case 黑骑士.魔击无效:
            case 米哈尔.魔击无效:
                cancel.add(MonsterStatus.MOB_STAT_PGuardUp);
                cancel.add(MonsterStatus.MOB_STAT_MGuardUp);
                cancel.add(MonsterStatus.MOB_STAT_PowerUp);
                cancel.add(MonsterStatus.MOB_STAT_MagicUp);
                break;
            default:
                return;
        }
        Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        List<MapleMapObject> affected = applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
        int i = 0;
        for (MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (MonsterStatus stat : cancel) {
                    ((MapleMonster) mo).cancelStatus(stat);
                }
            }
            i++;
            if (i >= info.get(MapleStatInfo.mobCount)) {
                break;
            }
        }
    }

    public void applyMonsterBuff(MapleCharacter applyfrom) {
        Rectangle bounds = calculateBoundingBox(applyfrom.getTruePosition(), applyfrom.isFacingLeft());
        boolean pvp = applyfrom.inPVP();
        MapleMapObjectType types = pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER;
        List<MapleMapObject> affected = sourceid == 机械师.支援波动器_H_EX ? applyfrom.getMap().getMapObjectsInRange(applyfrom.getTruePosition(), Double.POSITIVE_INFINITY, Collections.singletonList(types)) : applyfrom.getMap().getMapObjectsInRect(bounds, Collections.singletonList(types));
        int i = 0;
        for (MapleMapObject mo : affected) {
            if (makeChanceResult()) {
                for (Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) {
                    if (pvp) {
                        MapleCharacter chr = (MapleCharacter) mo;
                        MapleDisease d = MonsterStatus.getLinkedDisease(stat.getKey());
                        if (d != null) {
                            chr.giveDebuff(d, stat.getValue(), getDuration(), d.getDisease(), 1);
                        }
                    } else {
                        MapleMonster mons = (MapleMonster) mo;
                        if (sourceid == 机械师.支援波动器_H_EX && mons.getStats().isBoss()) {
                            break;
                        }
                        mons.applyStatus(applyfrom, new MonsterStatusEffect(stat.getKey(), stat.getValue(), sourceid, null, false), isPoison(), isSubTime(sourceid) ? getSubTime() : getDuration(), true, this);
                    }
                }

                //将全部怪物Buff修改为叠加模式
//                List<MonsterStatusEffect> pMonsterList = new ArrayList<>();
//                for (Entry<MonsterStatus, Integer> stat : getMonsterStati().entrySet()) {
//                    pMonsterList.add(new MonsterStatusEffect(stat.getKey(), stat.getValue(), sourceid, null, false, this.getDOTStack()));
//                }
//                ((MapleMonster) mo).applyStatus(applyfrom, pMonsterList, isPoison(), isSubTime(sourceid) ? getSubTime() : getDuration(), true, this);

                if (pvp && skill) {
                    MapleCharacter chr = (MapleCharacter) mo;
                    handleExtraPVP(applyfrom, chr);
                }
            }
            i++;
            if (i >= info.get(MapleStatInfo.mobCount) && sourceid != 机械师.支援波动器_H_EX) { //加速器：EX-7
                break;
            }
        }
    }

    public boolean isSubTime(int source) {
        switch (source) {
            case 圣骑士.压制术:
            case 双弩.精灵骑士:
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
            case 恶魔猎手.黑暗复仇:
            case 恶魔猎手.鬼泣:
            case 恶魔猎手.黑暗变形:
            case 狂龙战士.石化:
            case 狂龙战士.石化_变身:
            case 魂骑士.灵魂之眼:
                return true;
        }
        return false;
    }

    public void handleExtraPVP(MapleCharacter applyfrom, MapleCharacter chr) {
        if (sourceid == 圣骑士.压制术 || (JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 104)) { //doom, threaten, snatch
            long starttime = System.currentTimeMillis();
            int localsourceid = sourceid;
            Map<MapleBuffStat, Integer> localstatups;
            if (sourceid == 圣骑士.压制术) {
                localstatups = Collections.singletonMap(MapleBuffStat.压制术, (int) level);
            } else {
                localstatups = Collections.singletonMap(MapleBuffStat.变身效果, info.get(MapleStatInfo.x));
            }
            chr.send(BuffPacket.giveBuff(localsourceid, getDuration(), localstatups, this, chr));
            chr.registerEffect(this, starttime, BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, localstatups), isSubTime(sourceid) ? getSubTime() : getDuration()), localstatups, false, getDuration(), applyfrom.getId());
        }
    }

    public Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        return MapleStatEffectFactory.calculateBoundingBox(posFrom, facingLeft, lt, rb, info.get(MapleStatInfo.range));
    }

    public Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft, int addedRange) {
        return MapleStatEffectFactory.calculateBoundingBox(posFrom, facingLeft, lt, rb, info.get(MapleStatInfo.range) + addedRange);
    }

    public double getMaxDistanceSq() { //lt = infront of you, rb = behind you; not gonna distanceSq the two points since this is in relative to player position which is (0,0) and not both directions, just one
        int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        return (maxX * maxX) + (maxY * maxY);
    }

    /*
     * 切换频道或者进入商城出来后 给角色BUFF 不需要发送封包
     */
    public void silentApplyBuff(MapleCharacter chr, long starttime, int localDuration, Map<MapleBuffStat, Integer> statup, int chrId) {
        int maskedDuration = 0;
        int newDuration = (int) ((starttime + localDuration) - System.currentTimeMillis());
        if (is终极无限()) {
            maskedDuration = alchemistModifyVal(chr, 4000, false);
        }
        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(new CancelEffectAction(chr, this, starttime, statup), maskedDuration > 0 ? maskedDuration : newDuration);
        chr.registerEffect(this, starttime, schedule, statup, true, localDuration, chrId);
        SummonMovementType summonMovementType = getSummonMovementType();
        if (summonMovementType != null && !summonMovementType.equals(SummonMovementType.自由移动)) {
            MapleSummon summon = new MapleSummon(chr, this, chr.getTruePosition(), summonMovementType, newDuration, 0);
            if (!summon.is替身术()) {
                chr.getMap().spawnSummon(summon);
                chr.addSummon(summon);
                summon.addSummonHp(info.get(MapleStatInfo.x).shortValue());
                if (is灵魂助力()) {
                    summon.addSummonHp((short) 1);
                }
            }
        }
    }

    /*
     * 注册能量获得BUFF效果
     */
    public void applyEnergyBuff(MapleCharacter applyto) {
        applyEnergyBuff(applyto, info.get(MapleStatInfo.x));
    }

    public void applyEnergyBuff(MapleCharacter applyto, int senergy) {
        long startTime = System.currentTimeMillis();
        int localDuration = info.get(MapleStatInfo.time); //当前设置的为 0秒
        Map<MapleBuffStat, Integer> localstatups = Collections.singletonMap(MapleBuffStat.能量获得, senergy);
        applyto.cancelEffect(this, true, -1, localstatups);
        CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, startTime, localstatups);
        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, localDuration);
        applyto.registerEffect(this, startTime, schedule, localstatups, false, localDuration, applyto.getId());
        //发送显示BUFF效果
        applyto.getClient().announce(EffectPacket.showOwnBuffEffect(sourceid, EffectOpcode.UserEffect_SkillAffected.getValue(), applyto.getLevel(), level));
        applyto.getClient().announce(BuffPacket.giveEnergyCharge(senergy, sourceid, false, false));
        //发送显示给其他玩家看的效果
        applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto, sourceid, EffectOpcode.UserEffect_SkillAffected.getValue(), applyto.getLevel(), level), false);
        applyto.getMap().broadcastMessage(applyto, BuffPacket.showEnergyCharge(applyto.getId(), senergy, sourceid, false, false), false);
    }

    /*
     * 给角色超越攻击的BUFF效果
     */
    public void applyTranscendBuff(MapleCharacter applyto) {
        long startTime = System.currentTimeMillis();
        int localDuration = info.get(MapleStatInfo.time); //当前设置的为 15秒 也就是15000毫秒
        int skillId = SkillConstants.getLinkedAttackSkill(sourceid); //取当前技能的连接技能ID
        int buffSourceId = applyto.getBuffSource(MapleBuffStat.超越攻击); //取当前BUFF的技能ID没有返回 -1
        if (buffSourceId > -1 && buffSourceId != skillId) {
            applyto.cancelEffectFromBuffStat(MapleBuffStat.超越攻击);
        }
        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(applyto.getTotalSkillLevel(skillId));
        int combos = applyto.getBuffedIntValue(MapleBuffStat.超越攻击);
        if (combos == 0) {
            combos = 1;
        } else {
            combos++;
        }
        if (combos > 4) {
            combos = 4;
        }
        //设置BUFF的状态和数值
        Map<MapleBuffStat, Integer> localstatups = Collections.singletonMap(MapleBuffStat.超越攻击, combos);
        //取消以前的BUFF效果
        applyto.cancelEffect(effect, true, -1, localstatups);
        //设置新BUFF的线程处理
        CancelEffectAction cancelAction = new CancelEffectAction(applyto, effect, startTime, localstatups);
        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, localDuration);
        //注册BUFF效果
        applyto.registerEffect(effect, startTime, schedule, localstatups, false, localDuration, applyto.getId());
        //发送显示BUFF效果
        applyto.getClient().announce(BuffPacket.giveBuff(skillId, localDuration, localstatups, effect, applyto));
    }

    /*
     * 给角色添加 三彩箭矢 BUFF效果
     */
    public void applyArrowsBuff(MapleCharacter applyto, boolean reset) {
        long startTime = System.currentTimeMillis();
        int localDuration = info.get(MapleStatInfo.time); //当前设置的为 0秒
        int mode = applyto.getSpecialStat().getArrowsMode(); //默认的模式为 -1
        mode++;
        if (mode > 2 || mode == -1) {
            mode = 0;
        }
        int arrows = applyto.getBuffedIntValue(MapleBuffStat.三彩箭矢); //当前的箭矢数量
        if (arrows == 0 || reset) {
            mode = 0;
            arrows = 101010;
            Skill skil = SkillFactory.getSkill(神射手.进阶箭筒);
            if (applyto.getSkillLevel(skil) > 0) {
                MapleStatEffect effect = skil.getEffect(applyto.getSkillLevel(skil));
                if (effect != null) {
                    arrows = 10000 * effect.getY() + 100 * effect.getY() + effect.getZ();
                }
            }
        }
        applyto.getSpecialStat().setArrowsMode(mode);
        Map<MapleBuffStat, Integer> localstatups = Collections.singletonMap(MapleBuffStat.三彩箭矢, arrows);
        applyto.cancelEffect(this, true, -1, localstatups);
        CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, startTime, localstatups);
        ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, localDuration);
        applyto.registerEffect(this, startTime, schedule, localstatups, false, localDuration, applyto.getId());
        //发送显示BUFF效果
        applyto.getClient().announce(BuffPacket.giveBuff(sourceid, localDuration, localstatups, this, applyto));
        //初始化箭矢数量
        int newArrows = 0x0A;
        if (mode == 0) {
            newArrows = arrows / 10000;
        } else if (mode == 1) {
            newArrows = arrows % 10000 / 100;
        } else if (mode == 2) {
            newArrows = arrows % 100;
        }
        applyto.getClient().announce(EffectPacket.showArrowsEffect(sourceid, applyto.getSpecialStat().getArrowsMode(), newArrows)); //冲入 10只箭矢
    }


    public static void apply双重防御(MapleCharacter player) {
        int skillLevel = player.getSkillLevel(尖兵.双重防御);
        Skill skill = SkillFactory.getSkill(尖兵.双重防御);
        MapleStatEffect effect = skill.getEffect(skillLevel);
        if (!JobConstants.is尖兵(player.getJob()) || player.getBuffStatValueHolder(MapleBuffStat.黑暗高潮) == null) {
            return;
        }
        effect.applyBuffEffect(player, 2100000000, true);
    }

    public void applyBuffEffect(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, int newDuration) {
        applyBuffEffect(applyfrom, applyto, primary, newDuration, false);
    }

    public void applyBuffEffect(MapleCharacter applyfrom, int newDuration, boolean primary) {
        applyBuffEffect(applyfrom, applyfrom, false, newDuration, primary);
    }

    private int getHpMpChange(MapleCharacter applyfrom, boolean hpchange) {
        int change = 0;
        if (hpR != 0 || mpR != 0) {
            double healHpRate = hpR;
            if (is元气恢复()) {
                healHpRate -= applyfrom.getBuffedIntValue(MapleBuffStat.元气恢复) / 100.0;
                if (healHpRate <= 0) {
                    return 0;
                }
            }
            if (applyfrom.isShowPacket()) {
                applyfrom.dropMessage(-5, "HpMpChange => 默认: " + hpR + " - " + healHpRate);
            }
            int maxChange = (hpchange ? healHpRate : mpR) < 1 ? Math.min(49999, (int) Math.floor(99999 * (hpchange ? healHpRate : mpR))) : 99999;
            int current = hpchange ? applyfrom.getStat().getCurrentMaxHp() : applyfrom.getStat().getCurrentMaxMp(applyfrom.getJob());
            change = Math.abs((int) (current * (hpchange ? healHpRate : mpR))) > Math.abs(maxChange) ? maxChange : (int) (current * (hpchange ? healHpRate : mpR));
        }
        return change;
    }

    private int calcHPChange(MapleCharacter applyfrom, boolean primary) {
        int hpchange = 0;
        if (info.get(MapleStatInfo.hp) != 0) {
            if (!skill) {
                if (primary) {
                    hpchange += alchemistModifyVal(applyfrom, info.get(MapleStatInfo.hp), true);
                } else {
                    hpchange += info.get(MapleStatInfo.hp);
                }
                if (applyfrom.hasDisease(MapleDisease.不死化)) {
                    hpchange /= 2;
                }
            } else { // assumption: this is heal
                hpchange += MapleStatEffectFactory.makeHealHP(info.get(MapleStatInfo.hp) / 100.0, applyfrom.getStat().getTotalMagic(), 3, 5);
                if (applyfrom.hasDisease(MapleDisease.不死化)) {
                    hpchange = -hpchange;
                }
            }
        }
        if (hpR != 0) {
            hpchange += getHpMpChange(applyfrom, true) / (applyfrom.hasDisease(MapleDisease.不死化) ? 2 : 1);
        }
        // actually receivers probably never get any hp when it's not heal but whatever
        if (primary) {
            if (info.get(MapleStatInfo.hpCon) != 0) {
                hpchange -= info.get(MapleStatInfo.hpCon);
            }
        }
        return hpchange;
    }

    private int calcMPChange(MapleCharacter applyfrom, boolean primary) {
        int mpchange = 0;
        if (info.get(MapleStatInfo.mp) != 0) {
            if (primary) {
                mpchange += alchemistModifyVal(applyfrom, info.get(MapleStatInfo.mp), true);
            } else {
                mpchange += info.get(MapleStatInfo.mp);
            }
        }
        if (mpR != 0) {
            mpchange += getHpMpChange(applyfrom, false);
        }
        if (JobConstants.is恶魔猎手(applyfrom.getJob()) || JobConstants.is超能力者(applyfrom.getJob())) {
            mpchange = 0;
        }
        if (primary) {
            if (info.get(MapleStatInfo.mpCon) != 0 && !JobConstants.is恶魔猎手(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.终极无限) != null) {
                    mpchange = 0;
                } else {
                    int mpconMaxPercent = getDamage() > 0 && !isSummonSkill() ? applyfrom.getStat().mpconMaxPercent * applyfrom.getStat().getCurrentMaxMp(applyfrom.getJob()) / 100 : 0;
                    mpchange -= (info.get(MapleStatInfo.mpCon) - (info.get(MapleStatInfo.mpCon) * applyfrom.getStat().mpconReduce / 100)) * (applyfrom.getStat().mpconPercent / 100.0) + mpconMaxPercent;
                }
            } else if (info.get(MapleStatInfo.forceCon) != 0 && JobConstants.is恶魔猎手(applyfrom.getJob())) {
                if (applyfrom.getBuffedValue(MapleBuffStat.无限精气) != null) {
                    mpchange = 0;
                } else {
                    boolean superskill = false;
                    if (is恶魔呼吸() && applyfrom.getSkillLevel(恶魔猎手.恶魔呼吸_减少精气) > 0 || is黑暗变形() && applyfrom.getSkillLevel(恶魔猎手.黑暗变形_减少精气) > 0 || is恶魔冲击波() && applyfrom.getSkillLevel(恶魔猎手.恶魔冲击波_减少精气) > 0) {
                        superskill = true;
                    }
                    mpchange -= info.get(MapleStatInfo.forceCon) / (superskill ? 2 : 1);
                }
            } else if (JobConstants.is超能力者(applyfrom.getJob()) && !is终极物质() && !is终极火车() && !is终极心魂弹()) {
                if (info.get(MapleStatInfo.ppRecovery) != 0) {
                    mpchange += info.get(MapleStatInfo.ppRecovery);
                } else if (info.get(MapleStatInfo.ppCon) != 0) {
                    mpchange -= info.get(MapleStatInfo.ppCon);
                }
            }
        }
        return mpchange;
    }

    public int alchemistModifyVal(MapleCharacter chr, int val, boolean withX) {
        if (!skill) {
            return (val * (withX ? chr.getStat().RecoveryUP : chr.getStat().BuffUP) / 100);
        }
        return val * (withX ? chr.getStat().RecoveryUP : (chr.getStat().BuffUP_Skill + (getSummonMovementType() == null ? 0 : chr.getStat().BuffUP_Summon))) / 100;
    }

    private int calcPowerChange(MapleCharacter applyfrom) {
        int powerchange = 0;
        if (!JobConstants.is尖兵(applyfrom.getJob())) {
            return powerchange;
        } else if (applyfrom.getBuffedValue(MapleBuffStat.永动引擎) != null) {
            return powerchange;
        }
        if (info.get(MapleStatInfo.powerCon) != 0) {
            powerchange -= info.get(MapleStatInfo.powerCon);
        }
        return powerchange;
    }

    public boolean isRefreshstyle() {
        return refreshstyle;
    }

    public void initForeign(MapleForeignBuffStat... stat) {
        if (!refreshstyle) {
            refreshstyle = true;
            foreign = new MapleForeignBuffSkill(this);
            foreign.getStats().addAll(Arrays.asList(stat));
        }
    }

    public void setLt(Point Lt) {
        lt = Lt;
    }

    public void setRb(Point Rb) {
        rb = Rb;
    }

    public Skill getSkill() {
        return SkillFactory.getSkill(sourceid);
    }

    public boolean isGmBuff() {
        switch (sourceid) {
            case 10001075: //Empress Prayer
            case 9001000: // GM dispel
            case 9001001: // GM haste
            case 9001002: // GM Holy Symbol
            case 9001003: // GM Bless
            case 9001005: // GM resurrection
            case 9001008: // GM Hyper body

            case 9101000:
            case 9101001:
            case 9101002:
            case 9101003:
            case 9101005:
            case 9101008:
                return true;
            default:
                return JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1005;
        }
    }

    public boolean isInflation() {
        return inflation > 0;
    }

    public int getInflation() {
        return inflation;
    }

    public boolean is能量获得() {
        return skill && (sourceid == 冲锋队长.能量获得 || sourceid == 冲锋队长.超级冲击 || sourceid == 冲锋队长.终极冲击);
    }

    public boolean isMonsterBuff() {
        switch (sourceid) {
            case 圣骑士.压制术:
            case 神炮王.紧急后撤:
            case 隐士.影网术:
            case 双刀.闪光弹:
            case 90001002:
            case 90001003:
            case 90001004:
            case 90001005:
            case 90001006:
            case 英雄.魔击无效:
            case 圣骑士.魔击无效:
            case 黑骑士.魔击无效:
            case 米哈尔.魔击无效:
            case 机械师.支援波动器_H_EX:
            case 隐月.破力拳_冲击波:
            case 魂骑士.灵魂之眼:
            case 豹弩游侠.激怒:
                return skill;
        }
        return false;
    }

    /*
     * 是否组队BUFF效果
     */
    private boolean isPartyBuff() {
        if (lt == null || rb == null || !partyBuff) {
            return false;
        }
        switch (sourceid) {
            case 圣骑士.火焰冲击:
            case 圣骑士.寒冰冲击:
            case 圣骑士.雷鸣冲击:
            case 圣骑士.神圣冲击:
            case 双刀.终极斩:
            case 狂龙战士.强健护甲:
            case 林之灵.模式解除:
            case 林之灵.巨熊模式:
            case 林之灵.雪豹模式:
            case 林之灵.猛鹰模式:
            case 林之灵.猫咪模式:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
                return false;
        }
        return !(SkillConstants.isNoDelaySkill(sourceid) || is超越攻击());
    }

    /*
     * 是否为组队被动BUFF状态
     */
    public boolean isPartyPassiveBuff() {
        return skill && (sourceid == 林之灵.阿尔之好伙伴 || sourceid == 林之灵.阿尔之窃取 || sourceid == 林之灵.阿尔之爪 || sourceid == 林之灵.阿尔之魅力_强化 || sourceid == 林之灵.阿尔之弱点把握 || sourceid == 林之灵.阿尔之饱腹感 || sourceid == 林之灵.喵喵卡片 || sourceid == 林之灵.喵喵治愈);
    }

    public boolean is狂龙战士的威严() {
        return skill && sourceid == 狂龙战士.狂龙战士的威严;
    }

    public boolean is剑刃之壁() {
        return skill && (sourceid == 狂龙战士.剑刃之壁 || sourceid == 狂龙战士.进阶剑刃之壁 || sourceid == 狂龙战士.剑刃之壁_变身 || sourceid == 狂龙战士.进阶剑刃之壁_变身);
    }

    public int get剑刃之壁类型() {
        switch (sourceid) {
            case 狂龙战士.剑刃之壁:
                return 1;
            case 狂龙战士.进阶剑刃之壁:
                return 2;
            case 狂龙战士.剑刃之壁_变身:
                return 3;
            case 狂龙战士.进阶剑刃之壁_变身:
                return 4;
        }
        return 1;
    }

    public boolean is神圣之火() {
        return skill && sourceid == 黑骑士.神圣之火;
    }

    public boolean is黑暗领地() {
        return skill && sourceid == 夜行者.黑暗领地;
    }

    public boolean is黑暗幻影() {
        return skill && sourceid == 夜行者.黑暗幻影;
    }

    public boolean is三彩箭矢() {
        return skill && sourceid == 神射手.三彩箭矢;
    }

    public boolean is永动引擎() {
        return skill && sourceid == 尖兵.永动引擎;
    }

    public boolean is神秘瞄准术() {
        return skill && (sourceid == 主教.神秘瞄准术 || sourceid == 冰雷.神秘瞄准术 || sourceid == 火毒.神秘瞄准术);
    }

    public boolean is群体治愈() {
        return skill && (sourceid == 主教.群体治愈 || sourceid == 9101000 || sourceid == 管理员.完美治愈 || sourceid == 林之灵.喵喵治愈);
    }

    public boolean is黑暗灵气() {
        return skill && sourceid == 唤灵斗师.黑暗灵气;
    }

    public boolean is黄色灵气() {
        return skill && sourceid == 唤灵斗师.黄色灵气;
    }

    public boolean is蓝色灵气() {
        return skill && sourceid == 唤灵斗师.蓝色灵气;
    }

    public boolean is吸收灵气() {
        return skill && sourceid == 唤灵斗师.吸收灵气;
    }

    public boolean is减益灵气() {
        return skill && sourceid == 唤灵斗师.减益灵气;
    }

    public boolean is黑暗闪电() {
        return skill && sourceid == 唤灵斗师.黑暗闪电;
    }

    public boolean is暴怒对战() {
        return skill && sourceid == 唤灵斗师.暴怒对战;
    }

    public boolean is复活术() {
        return skill && (sourceid == 管理员.普天复活 || sourceid == 9101005 || sourceid == 主教.复活术 || sourceid == 林之灵.喵喵复活);
    }

    public boolean is伺机待发() {
        return skill && (sourceid == 冲锋队长.伺机待发);
    }

    public boolean is尖兵支援() {
        return skill && sourceid == 尖兵.急速支援;
    }

    public boolean is月光转换() {
        return skill && (sourceid == 魂骑士.月光洒落 || sourceid == 魂骑士.旭日 || sourceid == 魂骑士.日月轮转_月光洒落 || sourceid == 魂骑士.日月轮转_旭日);
    }

    public boolean is信天翁新() {
        return skill && (sourceid == 风灵使者.信天翁_新 || sourceid == 风灵使者.极限信天翁);
    }

    public boolean is圣洁之力() {
        return skill && sourceid == 神之子.圣洁之力;
    }

    public boolean is神圣迅捷() {
        return skill && sourceid == 神之子.神圣迅捷;
    }

    public boolean is进阶祝福() {
        return skill && sourceid == 主教.进阶祝福;
    }

    public boolean is神圣魔法盾() {
        return skill && sourceid == 主教.神圣魔法盾;
    }

    public boolean is幸运钱() {
        return skill && sourceid == 侠盗.幸运钱;
    }

    public boolean is终极斩() {
        return skill && sourceid == 双刀.终极斩;
    }

    public int getHp() {
        return info.get(MapleStatInfo.hp);
    }

    public int getMp() {
        return info.get(MapleStatInfo.mp);
    }

    public int getMpCon() {
        return info.get(MapleStatInfo.mpCon);
    }

    /**
     * *
     * 持续秒数(每X秒造成伤害X)
     *
     * @return
     */
    public int getDotInterval() {
        return info.get(MapleStatInfo.dotInterval);
    }

    /**
     * *
     * 持续伤害重叠次数
     *
     * @return
     */
    public int getDOTStack() {
        return info.get(MapleStatInfo.dotSuperpos);
    }

    public double getHpR() {
        return hpR;
    }

    public double getMpR() {
        return mpR;
    }

    public int getMastery() {
        return info.get(MapleStatInfo.mastery);
    }

    public int getWatk() {
        return info.get(MapleStatInfo.pad);
    }

    public int getPadR() {
        return info.get(MapleStatInfo.padR);
    }

    public int getMatk() {
        return info.get(MapleStatInfo.mad);
    }

    public int getWdef() {
        return info.get(MapleStatInfo.pdd);
    }

    public int getWdef2Dam() {
        return info.get(MapleStatInfo.pdd2dam);
    }

    public int getMdef() {
        return info.get(MapleStatInfo.mdd);
    }

    /**
     * 增加命中力
     *
     * @return
     */
    public int getAcc() {
        return info.get(MapleStatInfo.acc);
    }

    public int getAcc2Dam() {
        return info.get(MapleStatInfo.acc2dam);
    }

    /**
     * 增加回避值
     *
     * @return
     */
    public int getAvoid() {
        return info.get(MapleStatInfo.eva);
    }

    /**
     * 移动速度
     *
     * @return
     */
    public int getSpeed() {
        return info.get(MapleStatInfo.speed);
    }

    public int getJump() {
        return info.get(MapleStatInfo.jump);
    }

    /**
     * 最大移动速度提高
     *
     * @return
     */
    public int getSpeedMax() {
        return info.get(MapleStatInfo.speedMax);
    }

    /**
     * *
     * 移动速度提高或增加
     *
     * @return
     */
    public int getPassiveSpeed() {
        return info.get(MapleStatInfo.psdSpeed);
    }

    /**
     * 跳跃力提高或者增加
     *
     * @return
     */
    public int getPassiveJump() {
        return info.get(MapleStatInfo.psdJump);
    }

    /**
     * BUFF的持续时间
     */
    public int getDuration() {
        return info.get(MapleStatInfo.time);
    }

    public void setDuration(int d) {
        this.info.put(MapleStatInfo.time, d);
    }

    public int getDuration(MapleCharacter applyfrom) {
        int time = skill ? applyfrom.getStat().getDuration(sourceid) : 0;
        return info.get(MapleStatInfo.time) + time;
    }

    /**
     * 对怪物BUFF的持续时间
     */
    public int getSubTime() {
        return info.get(MapleStatInfo.subTime);
    }

    /**
     * 是否BUFF状态技能
     */
    public boolean isOverTime() {
        return overTime;
    }

    /**
     * 不会被取消的BUFF
     */
    public boolean isNotRemoved() {
        return notRemoved;
    }

    /**
     * 是否自动重复使用的BUFF
     */
    public boolean isRepeatEffect() {
        return repeatEffect;
    }

    public EnumMap<MapleBuffStat, Integer> getStatups() {
        return statups;
    }

    /**
     * BUFF状态是否是同1个技能里面的
     */
    public boolean sameSource(MapleStatEffect effect) {
        return effect != null && (SkillConstants.getLinkedAttackSkill(effect.sourceid) == sourceid || sourceid == effect.sourceid) && skill == effect.skill;
    }

    public int getQ() {
        return info.get(MapleStatInfo.q);
    }

    public int getQ2() {
        return info.get(MapleStatInfo.q2);
    }

    public int getS() {
        return info.get(MapleStatInfo.s);
    }

    public int getT() {
        return info.get(MapleStatInfo.t);
    }

    public int getU() {
        return info.get(MapleStatInfo.u);
    }

    public int getV() {
        return info.get(MapleStatInfo.v);
    }

    public int getW() {
        return info.get(MapleStatInfo.w);
    }

    public int getX() {
        return info.get(MapleStatInfo.x);
    }

    public int getY() {
        return info.get(MapleStatInfo.y);
    }

    public int getZ() {
        return info.get(z);
    }

    public int getDamage() {
        if (sourceid == 豹弩游侠.美洲豹灵魂) {
            return info.get(MapleStatInfo.y);
        }
        return info.get(MapleStatInfo.damage);
    }

    public int getMagicDamage() {
        return info.get(MapleStatInfo.madR);
    }

    public int getPVPDamage() {
        return info.get(MapleStatInfo.PVPdamage);
    }

    /*
     * 获取技能攻击次数
     */
    public int getAttackCount() {
        return info.get(MapleStatInfo.attackCount);
    }

    /*
     * 获取技能攻击次数 + 额外增加次数
     */
    public int getAttackCount(MapleCharacter applyfrom) {
        int addcount = applyfrom.getSkillLevel(箭神.天赐神箭) > 0 && getAttackCount() >= 2 ? 1 : 0;
        return info.get(MapleStatInfo.attackCount) + applyfrom.getStat().getAttackCount(sourceid) + addcount;
    }

    /**
     * *
     * 攻击次数
     *
     * @return
     */
    public int getBulletCount() {
        return info.get(MapleStatInfo.bulletCount);
    }

    public int getBulletCount(MapleCharacter applyfrom) {
        int addcount = applyfrom.getSkillLevel(箭神.天赐神箭) > 0 && getBulletCount() >= 2 ? 1 : 0;
        return info.get(MapleStatInfo.bulletCount) + applyfrom.getStat().getAttackCount(sourceid) + addcount;
    }

    /*
     * 使用技能消耗子弹/飞镖多少发
     */
    public int getBulletConsume() {
        return info.get(MapleStatInfo.bulletConsume);
    }

    /**
     * 攻击怪物个数
     *
     * @return
     */
    public int getMobCount() {
        return info.get(MapleStatInfo.mobCount);
    }

    /*
     * 获取技能攻击怪物的数量 + 额外增加数量
     */
    public int getMobCount(MapleCharacter applyfrom) {
        return info.get(MapleStatInfo.mobCount) + applyfrom.getStat().getMobCount(sourceid);
    }

    public int getMoneyCon() {
        return moneyCon;
    }

    /**
     * 冷却时间(减少)
     *
     * @return
     */
    public int getCooltimeReduceR() {
        return info.get(MapleStatInfo.coolTimeR);
    }

    /*
     * 金币获得量增加x%
     */
    public int getMesoAcquisition() {
        return info.get(MapleStatInfo.mesoR);
    }

    /*
     * 获取技能的冷却时间
     */
    public int getCooldown(MapleCharacter applyfrom) {
        if (is神枪降临() && applyfrom.hasBuffSkill(黑骑士.龙之献祭)) {
            return 0;
        } else if (is一击要害箭() && applyfrom.getSkillLevel(箭神.一击要害箭_缩短冷却时间) > 0) {
            return 0;
        } else if (is平衡技能() && applyfrom.getBuffedIntValue(MapleBuffStat.光暗转换) == 2) { //平衡状态下无冷却时间
            return 0;
        }
        if (info.get(MapleStatInfo.cooltime) > 5) {
            int cooldownX = (int) (info.get(MapleStatInfo.cooltime) * (applyfrom.getStat().getCoolTimeR() / 100.0));
            int coolTimeR = (int) (info.get(MapleStatInfo.cooltime) * (applyfrom.getStat().getReduceCooltimeRate(sourceid) / 100.0));
            if (applyfrom.isShowPacket()) {
                applyfrom.dropMessage(-5, "技能冷却时间 => 默认: " + info.get(MapleStatInfo.cooltime) + " [减少百分比: " + applyfrom.getStat().getCoolTimeR() + "% - " + cooldownX + "] [减少时间: " + applyfrom.getStat().getReduceCooltime() + "] [超级技能减少百分比: " + applyfrom.getStat().getReduceCooltimeRate(sourceid) + "% 减少时间: " + coolTimeR + "]");
            }
            return Math.max(0, (info.get(MapleStatInfo.cooltime) - applyfrom.getStat().getReduceCooltime() - (cooldownX > 5 ? 5 : cooldownX) - coolTimeR)); //返回最大的数
        }
        return info.get(MapleStatInfo.cooltime);
    }

    public Map<MonsterStatus, Integer> getMonsterStati() {
        return monsterStatus;
    }

    public int getBerserk() {
        return berserk;
    }

    public boolean is神枪降临() {
        return skill && sourceid == 黑骑士.神枪降临;
    }

    public boolean is一击要害箭() {
        return skill && sourceid == 箭神.一击要害箭;
    }

    public boolean is船长爆头() {
        return skill && sourceid == 船长.爆头;
    }

    public boolean is平衡技能() {
        return skill && (sourceid == 夜光.死亡之刃 || sourceid == 夜光.绝对死亡);
    }

    public boolean is隐藏术() {
        return skill && (sourceid == 管理员.隐藏术 || sourceid == 9101004);
    }

    public boolean is隐身术() {
        return skill && (sourceid == 管理员.隐藏术 || sourceid == 9101004 || sourceid == 飞侠.隐身术 || sourceid == 夜行者.隐身术 || sourceid == 双刀.进阶隐身术);
    }

    public boolean is龙之力() {
        return skill && sourceid == 1311008; //黑骑士.龙之力
    }

    public boolean is龙之献祭() {
        return skill && sourceid == 黑骑士.龙之献祭;
    }

    public boolean is元气恢复() {
        return skill && sourceid == 圣骑士.元气恢复;
    }

    public boolean is日月轮转() {
        return skill && sourceid == 魂骑士.日月轮转;
    }

    public boolean is团队治疗() {
        return skill && (sourceid == 1001 || sourceid == 10001001 || sourceid == 20001001 || sourceid == 20011001 || sourceid == 35121005);
    }

    public boolean is潜入() {
        return skill && (sourceid == 20021001 || sourceid == 20031001 || sourceid == 30001001 || sourceid == 30011001 || sourceid == 30021001 || sourceid == 60001001 || sourceid == 60011001);
    }

    public boolean is重生契约状态() {
        return skill && (sourceid == 黑骑士.重生契约_状态);
    }

    public boolean is重生契约() {
        return skill && (sourceid == 黑骑士.重生契约);
    }

    public boolean is灵魂助力() {
        return skill && sourceid == 黑骑士.灵魂助力;
    }

    public boolean is灵魂助力统治() {
        return skill && sourceid == 黑骑士.灵魂助力统治;
    }

    public boolean is灵魂助力震惊() {
        return skill && sourceid == 黑骑士.灵魂助力震惊;
    }

    public boolean is刀飞炼狱() {
        return skill && sourceid == 侠盗.炼狱;
    }

    public boolean is极限射箭() {
        return skill && (sourceid == 神射手.极限射箭 || sourceid == 箭神.极限射箭);
    }

    public boolean is终极无限() {
        return skill && (sourceid == 火毒.终极无限 || sourceid == 冰雷.终极无限 || sourceid == 主教.终极无限);
    }

    public boolean is猫咪模式() {
        return skill && sourceid == 林之灵.猫咪模式;
    }

    public boolean is模式变更() {
        return skill && (sourceid == 林之灵.巨熊模式 || sourceid == 林之灵.雪豹模式 || sourceid == 林之灵.猛鹰模式 || sourceid == 林之灵.猫咪模式 || sourceid == 林之灵.模式解除);
    }

    public boolean is喵喵卡片() {
        return skill && (sourceid == 林之灵.喵喵卡片 || sourceid == 林之灵.红色卡片 || sourceid == 林之灵.蓝色卡片 || sourceid == 林之灵.绿色卡片 || sourceid == 林之灵.金色卡片 || sourceid == 林之灵.喵喵金卡);
    }

    public boolean is舞力全开() {
        return skill && sourceid == 林之灵.舞力全开;
    }

    public boolean is嗨兄弟() {
        return skill && sourceid == 林之灵.嗨_兄弟;
    }

    public boolean is骑兽技能_() {
        return skill && (SkillConstants.is骑兽技能(sourceid) || sourceid == 80001000);
    }

    public boolean is骑兽技能() {
        return skill && (is骑兽技能_() || GameConstants.getMountItem(sourceid, null) != 0);
    }

    public boolean is时空门() {
        return skill && (sourceid == 主教.时空门 || sourceid % 10000 == 8001 || sourceid == 林之灵.设置伊卡驿站);
    }

    public boolean is金钱护盾() {
        return skill && sourceid == 侠盗.金钱护盾;
    }

    public boolean is愤怒之火() {
        return skill && sourceid == 英雄.愤怒之火;
    }

    public boolean is葵花宝典() {
        return skill && sourceid == 英雄.葵花宝典;
    }

    public boolean is弓手火眼晶晶() {
        return skill && sourceid == 神射手.火眼晶晶;
    }

    public boolean is符文状态() {
        return skill && (sourceid == 80001427 || sourceid == 80001428 || sourceid == 80001430 || sourceid == 80001432);
    }

    public boolean is金钱炸弹() {
        return skill && sourceid == 侠盗.金钱炸弹;
    }

    public boolean is机械传送门() {
        return skill && sourceid == 机械师.传送门_GX9;
    }

    public boolean is火焰咆哮() {
        return skill && sourceid == 双弩.火焰咆哮;
    }

    public boolean is影子闪避() {
        return skill && sourceid == 双刀.影子闪避;
    }

    public boolean is卡牌审判() {
        return skill && (sourceid == 幻影.卡牌审判 || sourceid == 幻影.卡牌审判_高级);
    }

    public boolean is黑暗祝福() {
        return skill && sourceid == 夜光.黑暗祝福;
    }

    public boolean is黑暗高潮() {
        return skill && sourceid == 夜光.黑暗高潮;
    }

    public boolean is夜光平衡() {
        return skill && (sourceid == 夜光.平衡_光明 || sourceid == 夜光.平衡_黑暗);
    }

    public boolean is绝对死亡() {
        return skill && sourceid == 夜光.绝对死亡;
    }

    public boolean is恶魔呼吸() {
        return skill && sourceid == 恶魔猎手.恶魔呼吸;
    }

    public boolean is黑暗变形() {
        return skill && sourceid == 恶魔猎手.黑暗变形;
    }

    public boolean is恶魔冲击波() {
        return skill && sourceid == 恶魔猎手.恶魔冲击波;
    }

    public boolean is激素引擎() {
        return skill && sourceid == 战神.激素引擎;
    }

    public boolean isCharge() {
        switch (sourceid) {
            case 圣骑士.雷鸣冲击:
            case 战神.冰雪矛:
                return skill;
        }
        return false;
    }

    public boolean isPoison() {
        return info.get(MapleStatInfo.dot) > 0 && info.get(MapleStatInfo.dotTime) > 0;
    }

    /*
     * 是否为烟雾效果
     */
    private boolean isMist() {
        switch (sourceid) {
            case 1076: //0001076 - 奥兹的火牢术屏障 - 召唤的奥兹在一定时间内在自身周围形成火幕。火幕内的怪物有一定概率处于着火状态，持续受到伤害。特定等级提升时，技能等级可以提升1。
            case 火毒.致命毒雾:
            case 主教.神圣源泉:
            case 隐士.模糊领域:
            case 侠盗.烟幕弹:
            case 唤灵斗师.避难所:
            case 隐月.束缚术:
            case 炎术士.燃烧领域:
            case 机械师.支援波动器_H_EX:
            case 机械师.支援波动器强化:
            case 豹弩游侠.辅助打猎单元:
            case 豹弩游侠.集束箭:
            case 林之灵.火焰屁:
            case 阴阳师.召唤妖云:
            case 阴阳师.结界_樱:
            case 阴阳师.结界_桔梗:
            case 阴阳师.结界_破魔:
            case 战神.摩诃领域_MIST:
            case 林之灵.喵喵空间:
                return true;
        }
        return false;
    }

    private boolean is暗器伤人() {
        return skill && (sourceid == 隐士.暗器伤人 || sourceid == 夜行者.魔法飞镖);
    }

    private boolean is无限子弹() {
        return skill && (sourceid == 船长.无限子弹);
    }

    private boolean is神速衔接() {
        return skill && sourceid == 船长.神速衔接;
    }

    private boolean is净化() {
        return skill && (sourceid == 主教.净化 || sourceid == 管理员.完美治愈 || sourceid == 9101000);
    }

    private boolean is勇士的意志() {
        switch (sourceid) {
            case 英雄.勇士的意志:
            case 圣骑士.勇士的意志:
            case 黑骑士.勇士的意志:
            case 火毒.勇士的意志:
            case 冰雷.勇士的意志:
            case 主教.勇士的意志:
            case 神射手.勇士的意志:
            case 箭神.勇士的意志:
            case 隐士.勇士的意志:
            case 侠盗.勇士的意志:
            case 冲锋队长.勇士的意志:
            case 船长.勇士的意志:
            case 战神.勇士的意志:
            case 龙神.勇士的意志:
            case 双刀.勇士的意志:
            case 唤灵斗师.勇士的意志:
            case 豹弩游侠.勇士的意志:
            case 机械师.勇士的意志:
            case 爆破手.勇士的意志:
            case 神炮王.勇士的意志:
            case 双弩.勇士的意志:
            case 幻影.勇士的意志:
            case 夜光.勇士的意志:
            case 尖兵.勇士的意志:
            case 狂龙战士.诺巴勇士的意志:
            case 爆莉萌天使.诺巴勇士的意志:
            case 林之灵.林之灵之意志:
            case 剑豪.晓之樱:
            case 阴阳师.晓之樱:
            case 超能力者.精神净化:
                return skill;
        }
        return false;
    }

    public boolean is矛连击强化() {
        return skill && sourceid == 战神.矛连击强化;
    }

    public boolean is侠盗本能() {
        return skill && sourceid == 侠盗.侠盗本能;
    }

    public boolean is提速时刻() {
        return skill && (sourceid == 神之子.提速时刻_侦查 || sourceid == 神之子.提速时刻_战斗);
    }

    public boolean is斗气集中() {
        switch (sourceid) {
            case 英雄.斗气集中:
                return skill;
        }
        return false;
    }

    public boolean isMorph() {
        return morphId > 0;
    }

    public int getMorph() {
        return morphId;
    }

    public boolean is元素冲击() {
        return skill && (sourceid == 圣骑士.元素冲击 || sourceid == 圣骑士.万佛归一破);
    }

    public boolean is狂龙变形() {
        return skill && (sourceid == 狂龙战士.终极变形_3转 || sourceid == 狂龙战士.终极变形_4转);
    }

    public boolean is狂龙超级变形() {
        return skill && sourceid == 狂龙战士.终极变形_超级;
    }

    public boolean is负荷释放() {
        return skill && sourceid == 恶魔复仇者.负荷释放;
    }

    public boolean is恶魔恢复() {
        return skill && sourceid == 恶魔复仇者.恶魔恢复;
    }

    public boolean is血之契约() {
        return skill && sourceid == 恶魔复仇者.血之契约;
    }

    public boolean is超越攻击() {
        return skill && SkillConstants.is超越攻击(sourceid);
    }

    public boolean is超越攻击状态() {
        switch (sourceid) {
            case 恶魔复仇者.超越十字斩:
            case 恶魔复仇者.超越恶魔突袭:
            case 恶魔复仇者.超越月光斩:
            case 恶魔复仇者.超越处决:
                return skill;
        }
        return false;
    }

    public boolean is额外供给() {
        return skill && sourceid == 尖兵.额外供给;
    }

    public boolean is金刚霸体() {
        return skill && JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1010;
    }

    public boolean is祝福护甲() {
        switch (sourceid) {
            case 圣骑士.祝福护甲:
                return skill;
        }
        return false;
    }

    public boolean is狂暴战魂() {
        return skill && JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1011;
    }

    public boolean is美洲豹骑士() {
        return skill && sourceid == 豹弩游侠.美洲豹骑士;
    }

    public int getMorph(MapleCharacter chr) {
        int morph = getMorph();
        switch (morph) {
            case 1000:
            case 1001:
            case 1003:
                return morph + (chr.getGender() == 1 ? 100 : 0);
        }
        return morph;
    }

    public byte getLevel() {
        return level;
    }

    public boolean isSummonSkill() {
        Skill summon = SkillFactory.getSkill(sourceid);
        return !(!skill || summon == null) && summon.isSummonSkill();
    }

    public SummonMovementType getSummonMovementType() {
        if (!skill) {
            return null;
        }
        if (is戒指技能()) {
            return SummonMovementType.飞行跟随; //1
        }
        switch (sourceid) {
            case 箭神.神箭幻影:
            case 风灵使者.绿水晶花: //新的风灵使者召唤兽技能
            case 风灵使者.钻石星尘: //绿水晶花的进阶技能
            case 船长.八轮重机枪:
            case 双刀.傀儡召唤:
            case 机械师.磁场:
            case 机械师.机器人发射器_RM7:
            case 机械师.支援波动器_H_EX:
            case 机械师.支援波动器强化:
            case 机械师.机器人工厂_RM1:
            case 机械师.战争机器_泰坦:
            case 隐士.黑暗杂耍:
            case 侠盗.黑暗杂耍:
            case 船长.战船轰炸机:
            case 神炮王.磁性船锚:
            case 神炮王.双胞胎猴子支援: //5321004
            case 神炮王.双胞胎猴子支援_1: //5320011
            case 神炮王.旋转彩虹炮:
            case 狂龙战士.石化:
            case 狂龙战士.石化_变身:
            case 龙的传人.破城炮:
            case 尖兵.全息力场_穿透:
            case 尖兵.全息力场_力场:
            case 尖兵.全息力场_支援:
            case 龙神.召唤玛瑙龙:
            case 夜行者.黑暗预兆:
            case 林之灵.小波波:
            case 炎术士.大漩涡:
            case 14111010:
            case 22171052:
            case 33101008:
            case 33111003:
            case 35111005:
            case 35121010:
            case 42100010:
            case 42111003:
            case 80011261:
            case 131001019:
            case 131001307:
            case 400011002:
            case 400021005:
            case 爆莉萌天使.爱星能量:
            case 龙的传人.猛龙暴风:
                return SummonMovementType.不会移动; //0
            case 黑骑士.灵魂助力:
            case 箭神.冰凤凰:
            case 神射手.火凤凰:
            case 双弩.精灵骑士:
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
            case 夜行者.影子蝙蝠_召唤兽:
            case 33101011:
            case 112110005:
            case 131002015:
                return SummonMovementType.跟随并且随机移动打怪; //3
//            case 机械师.机器人工厂_机器人:
            case 冰雷.闪电风暴:
            case 林之灵.嗨_兄弟:
            case 2111010:
            case 32111006:
            case 35121011:
                return SummonMovementType.自由移动; //2
            case 炎术士.元素_火焰:
            case 炎术士.元素_火焰II:
            case 炎术士.元素_火焰III:
            case 炎术士.元素_火焰IV:
            case 唤灵斗师.死亡:
            case 唤灵斗师.死亡契约:
            case 唤灵斗师.死亡契约2:
            case 唤灵斗师.死亡契约3:
            case 火毒.火魔兽:
            case 冰雷.冰破魔兽:
            case 主教.强化圣龙:
            case 30011090:
            case 英雄.燃灵之剑:
            case 12001004:
            case 12111004:
            case 14001005:
            case 35111001:
            case 35111009:
            case 35111010:
            case 400021018:
            case 机械师.多重属性_M_FL:
                return SummonMovementType.飞行跟随; //1
            case 船长.集合船员:
            case 船长.集合船员2:
            case 船长.集合船员3:
            case 船长.集合船员4:
            case 炎术士.火焰化身_狮子:
            case 炎术士.火焰化身_狐狸:
                return SummonMovementType.左右跟随;
            case 夜行者.影子侍从:
            case 夜行者.黑暗幻影_影子40:
            case 夜行者.黑暗幻影_影子20:
                return SummonMovementType.侍从;
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
                return SummonMovementType.坐骑跟随;
            case 101100100:
            case 101100101:
            case 400011012:
            case 400011013:
            case 400011014:
                return SummonMovementType.移动一定距离;
        }
        return null;
    }

    public boolean is集合船员() {
        switch (sourceid) {
            case 船长.集合船员:
            case 船长.集合船员2:
            case 船长.集合船员3:
            case 船长.集合船员4:
                return skill;
        }
        return false;
    }

    public boolean is元素火焰() {
        switch (sourceid) {
            case 炎术士.元素_火焰:
            case 炎术士.元素_火焰II:
            case 炎术士.元素_火焰III:
            case 炎术士.元素_火焰IV:
                return skill;
        }
        return false;
    }

    public boolean is船员统帅() {
        return skill && sourceid == 船长.指挥船员;
    }

    public boolean is燃烧领域() {
        return skill && sourceid == 炎术士.燃烧领域;
    }

    public boolean is全息力场支援() {
        return skill && sourceid == 尖兵.全息力场_支援;
    }

    public boolean is戒指技能() {
        return SkillConstants.is召唤兽戒指(sourceid);
    }

    public boolean isSkill() {
        return skill;
    }

    public boolean is冰骑士() {
        return skill && JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1105;
    }

    public boolean isSoaring() {
        return isSoaring_Normal() || isSoaring_Mount();
    }

    public boolean isSoaring_Normal() {
        //飞翔
        return skill && JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1026;
    }

    public boolean isSoaring_Mount() {
        //飞翔·
        return skill && ((JobConstants.is新手职业(sourceid / 10000) && sourceid % 10000 == 1142) || sourceid == 80001089);
    }

    /*
     * 80001242 - 飞行骑乘 - 可以在一定时间内自由飞行。但只能在村庄中飞行。
     */
    public boolean is高空飞行() {
        return skill && (sourceid == 80001242 || sourceid == 尖兵.自由飞行 || sourceid == 林之灵.伊卡飞翔 || sourceid == 超能力者.心魂漫步 || sourceid == 龙神.龙神);
    }

    public boolean is迷雾爆发() {
        return skill && sourceid == 火毒.迷雾爆发;
    }

    public boolean is能量激发() {
        return skill && sourceid == 冲锋队长.能量激发;
    }

    public boolean is寒冰灵气() {
        return skill && sourceid == 冰雷.寒冰灵气;
    }

    public boolean is影分身() {
        switch (sourceid) {
            case 隐士.影分身:
            case 侠盗.影分身:
            case 夜行者.影子侍从:
            case 双刀.镜像分身:
            case 尖兵.全息投影:
                return skill;
        }
        return false;
    }

    public double makeRate(int rate) {
        return rate / 100.0;
    }

    /*
     * 影分身的分身伤害倍数
     */
    public int getShadowDamage() {
        switch (sourceid) {
            case 隐士.影分身:
            case 侠盗.影分身:
            case 双刀.镜像分身:
                return info.get(MapleStatInfo.x);
            case 尖兵.全息投影:
                return info.get(MapleStatInfo.y);
        }
        return info.get(MapleStatInfo.x);
    }

    private boolean is全息投影() {
        return skill && sourceid == 尖兵.全息投影;
    }

    private boolean is影子侍从() {
        return skill && sourceid == 夜行者.影子侍从;
    }

    private boolean is伤害置换() {
        return skill && sourceid == 箭神.伤害置换;
    }

    public boolean is天使物品() {
        return !skill && (sourceid == 2022746 || sourceid == 2022747 || sourceid == 2022823);
    }

    public boolean is天使戒指() {
        return skill && (sourceid == 1087 || sourceid == 1179 || sourceid == 1085 || sourceid == 80001154);
    }

    private boolean is极速领域() {
        return skill && (sourceid == 冲锋队长.极速领域 || sourceid == 奇袭者.极速领域_新 || sourceid % 10000 == 8006);
    }

    private boolean is疾驰() {
        return skill && (sourceid == 海盗.疾驰);
    }

    private boolean is属性攻击() {
        switch (sourceid) {
            case 战神.冰雪矛:
            case 圣骑士.火焰冲击:
            case 圣骑士.寒冰冲击:
            case 圣骑士.雷鸣冲击:
            case 圣骑士.神圣冲击:
                return true;
        }
        return false;
    }

    private boolean is导航辅助() {
        switch (sourceid) {
            case 船长.无尽追击:
                return true;
        }
        return false;
    }

    public boolean is传说冒险家() {
        switch (sourceid) {
            case 冰雷.传说冒险家:
            case 侠盗.传说冒险家:
            case 双刀.传说冒险家:
            case 圣骑士.传说冒险家:
            case 神射手.传说冒险家:
            case 箭神.传说冒险家:
            case 冲锋队长.传说冒险家:
            case 船长.传说冒险家:
            case 隐士.传说冒险家:
            case 火毒.传说冒险家:
            case 神炮王.传说冒险家:
            case 主教.传说冒险家:
            case 英雄.传说冒险家:
            case 黑骑士.传说冒险家:
                return true;
        }
        return false;
    }

    public boolean is守护者之荣誉() {
        switch (sourceid) {
            case 魂骑士.守护者之荣誉:
            case 奇袭者.守护者之荣誉:
            case 炎术士.守护者之荣誉:
            case 夜行者.守护者之荣誉:
            case 风灵使者.守护者之荣誉:
                return true;
        }
        return false;
    }

    public boolean is英雄奥斯() {
        switch (sourceid) {
            case 战神.英雄奥斯:
            case 龙神.英雄奥斯:
            case 双弩.英雄奥斯:
            case 幻影.英雄奥斯:
            case 夜光.英雄奥斯:
            case 隐月.英雄奥斯:
                return true;
        }
        return false;
    }

    public boolean is自由之墙() {
        switch (sourceid) {
            case 恶魔猎手.自由之墙:
            case 唤灵斗师.自由之墙:
            case 豹弩游侠.自由之墙:
            case 机械师.自由之墙:
            case 恶魔复仇者.自由之墙:
            case 爆破手.自由之墙:
                return true;
        }
        return false;
    }

    public boolean is姬儿的加持() {
        switch (sourceid) {
            case 剑豪.姬儿的加持:
                return true;
        }
        return false;
    }

    public boolean is明日祝福() {
        switch (sourceid) {
            case 米哈尔.明日祝福:
                return true;
        }
        return false;
    }

    public boolean is金属机甲() {
        return skill && (sourceid == 机械师.金属机甲_人类 || sourceid == 机械师.金属机甲_战车);
    }

    public boolean is召唤美洲豹() {
        return skill && SkillConstants.is美洲豹(sourceid);
    }

    private boolean is美洲豹技能() {
        switch (sourceid) {
            case 豹弩游侠.利爪狂风:
            case 豹弩游侠.激怒:
            case 豹弩游侠.十字攻击:
            case 豹弩游侠.毁灭音爆:
            case 豹弩游侠.美洲豹灵魂:
            case 豹弩游侠.狂暴合一:
                return true;
            default:
                return false;
        }
    }

    private boolean is死亡契约() {
        switch (sourceid) {
            case 唤灵斗师.死亡:
            case 唤灵斗师.死亡契约:
            case 唤灵斗师.死亡契约2:
            case 唤灵斗师.死亡契约3:
                return true;
        }
        return false;
    }

    public boolean is战法灵气() {
        switch (sourceid) {
            case 唤灵斗师.黄色灵气:
            case 唤灵斗师.吸收灵气:
            case 唤灵斗师.蓝色灵气:
            case 唤灵斗师.黑暗灵气:
            case 唤灵斗师.减益灵气:
                return true;
        }
        return false;
    }

    public boolean is狂风肆虐() {
        switch (sourceid) {
            case 风灵使者.狂风肆虐Ⅰ:
            case 风灵使者.狂风肆虐Ⅱ:
            case 风灵使者.狂风肆虐Ⅲ:
                return true;
        }
        return false;
    }

    public boolean is影朋小白() {
        return skill && sourceid == 阴阳师.影朋_小白;
    }

    public boolean is拔刀姿势() {
        return skill && sourceid == 剑豪.拔刀姿势;
    }

    public boolean is避柳() {
        return skill && sourceid == 剑豪.避柳;
    }

    public boolean is灵狐() {
        return skill && sourceid == 隐月.灵狐;
    }

    public boolean is终极物质() {
        return skill && sourceid == 超能力者.终极_物质;
    }

    public boolean is终极火车() {
        return skill && sourceid == 超能力者.终极_火车;
    }

    public boolean is终极心魂弹() {
        return skill && sourceid == 超能力者.终极_心魂弹;
    }

    public boolean is终极BPM() {
        return skill && sourceid == 超能力者.终极_BPM;
    }

    public boolean is心魂本能() {
        return skill && sourceid == 超能力者.心魂本能;
    }

    public boolean is心魂吸收() {
        return skill && sourceid == 超能力者.心魂吸收;
    }

    public boolean is心魂充能() {
        return skill && sourceid == 超能力者.心魂充能;
    }

    public boolean is刺客标记() {
        return skill && (sourceid == 隐士.刺客标记);
    }

    /**
     * 机率计算结果，根据随机数据对比得到结果
     *
     * @return true ? 执行 : 不执行
     */
    public boolean makeChanceResult() {
        return info.get(MapleStatInfo.prop) >= 100 || Randomizer.nextInt(100) < info.get(MapleStatInfo.prop);
    }

    /**
     * 机率值
     *
     * @return 值
     */
    public int getProp() {
        return info.get(MapleStatInfo.prop);
    }

    /**
     * 额外机率
     *
     * @return
     */
    public int getSubProp() {
        return info.get(MapleStatInfo.subProp);
    }

    /*
     * 无视怪物防御
     */
    public short getIgnoreMob() {
        return ignoreMob;
    }

    /*
     * 增加Hp
     */
    public int getEnhancedHP() {
        return info.get(MapleStatInfo.emhp);
    }

    /*
     * 增加Mp
     */
    public int getEnhancedMP() {
        return info.get(MapleStatInfo.emmp);
    }

    /*
     * 增加物理攻击
     */
    public int getEnhancedWatk() {
        return info.get(MapleStatInfo.epad);
    }

    /*
     * 增加魔法攻击
     */
    public int getEnhancedMatk() {
        return info.get(MapleStatInfo.emad);
    }

    /*
     * 增加物理防御
     */
    public int getEnhancedWdef() {
        return info.get(MapleStatInfo.pdd);
    }

    /*
     * 增加魔法防御
     */
    public int getEnhancedMdef() {
        return info.get(MapleStatInfo.emdd);
    }

    /**
     * *
     * 持续伤害%比
     *
     * @return
     */
    public int getDOT() {
        return info.get(MapleStatInfo.dot);
    }

    /**
     * ***
     * 持续总时间
     *
     * @return
     */
    public int getDOTTime() {
        return info.get(MapleStatInfo.dotTime);
    }

    /*
     * 爆击概率
     */
    public int getCritical() {
        return info.get(MapleStatInfo.cr);
    }

    /*
     * 爆击最大伤害
     */
    public int getCriticalMax() {
        return info.get(MapleStatInfo.criticaldamageMax);
    }

    /*
     * 爆击最小伤害
     */
    public int getCriticalMin() {
        return info.get(MapleStatInfo.criticaldamageMin);
    }

    /*
     * 命中增加 x%
     */
    public int getArRate() {
        return info.get(MapleStatInfo.ar);
    }

    public int getASRRate() {
        return info.get(MapleStatInfo.asrR);
    }

    public int getTERRate() {
        return info.get(MapleStatInfo.terR);
    }

    /*
     * 攻击伤害提高 百分比
     */
    public int getDAMRate() {
        return info.get(MapleStatInfo.damR);
    }

    /*
     * 魔攻伤害提高 百分比
     */
    public int getMdRate() {
        return info.get(MapleStatInfo.mdR);
    }

    /*
     * 攻击伤害提高 百分比
     */
    public int getPercentDamageRate() {
        return info.get(MapleStatInfo.pdR);
    }

    /*
     * 金币获得量增加x%
     */
    public short getMesoRate() {
        return mesoR;
    }

    public int getEXP() {
        return exp;
    }

    /*
     * 物理防御力的x%追加到魔法防御力
     */
    public int getWdefToMdef() {
        return info.get(MapleStatInfo.pdd2mdd);
    }

    /*
     * 魔法防御力的x%追加到物理防御力
     */
    public int getMdefToWdef() {
        return info.get(MapleStatInfo.mdd2pdd);
    }

    /*
     * 回避值提升HP上限 - HP上限增加回避值的x%
     */
    public int getAvoidToHp() {
        return info.get(MapleStatInfo.eva2hp);
    }

    /*
     * 命中值提升MP上限 - MP上限增加命中值的x%
     */
    public int getAccToMp() {
        return info.get(MapleStatInfo.acc2mp);
    }

    /*
     * 力量提升敏捷 - 投资了AP力量的x%追加到敏捷
     */
    public int getStrToDex() {
        return info.get(MapleStatInfo.str2dex);
    }

    /*
     * 敏捷提升力量 - 投资了AP敏捷的x%追加到力量
     */
    public int getDexToStr() {
        return info.get(MapleStatInfo.dex2str);
    }

    /*
     * 智力提升运气 - 投资了AP智力的x%追加到运气
     */
    public int getIntToLuk() {
        return info.get(MapleStatInfo.int2luk);
    }

    /*
     * 运气提升敏捷 - 投资了AP运气的x%追加到敏捷
     */
    public int getLukToDex() {
        return info.get(MapleStatInfo.luk2dex);
    }

    /*
     * Hp增加攻击伤害
     */
    public int getHpToDamageX() {
        return info.get(MapleStatInfo.mhp2damX);
    }

    /*
     * Mp增加攻击伤害
     */
    public int getMpToDamageX() {
        return info.get(MapleStatInfo.mmp2damX);
    }

    /*
     * 升级增加最大HP上限
     */
    public int getLevelToMaxHp() {
        return info.get(MapleStatInfo.lv2mhp);
    }

    /*
     * 升级增加最大MP上限
     */
    public int getLevelToMaxMp() {
        return info.get(MapleStatInfo.lv2mmp);
    }

    /*
     * 升级增加增加攻击伤害
     */
    public int getLevelToDamageX() {
        return info.get(MapleStatInfo.lv2damX);
    }

    /*
     * 升级增加物理攻击力 - 每x级攻击力增加1
     */
    public int getLevelToWatk() {
        return info.get(MapleStatInfo.lv2pad);
    }

    /*
     * 升级增加魔法攻击力 - 每x级魔法攻击力增加1
     */
    public int getLevelToMatk() {
        return info.get(MapleStatInfo.lv2mad);
    }

    /*
     * 升级增加物理攻击力 - 每5级攻击力增加1
     */
    public int getLevelToWatkX() {
        return info.get(MapleStatInfo.lv2pdX);
    }

    /*
     * 升级增加魔法攻击力 - 每5级魔法攻击力增加1
     */
    public int getLevelToMatkX() {
        return info.get(MapleStatInfo.lv2mdX);
    }

    /**
     * 死亡时经验减少 X%
     *
     * @return
     */
    public int getEXPLossRate() {
        return info.get(MapleStatInfo.expLossReduceR);
    }

    /**
     * 增加增益效果时间 X%
     *
     * @return
     */
    public int getBuffTimeRate() {
        return info.get(MapleStatInfo.bufftimeR);
    }

    public int getSuddenDeathR() {
        return info.get(MapleStatInfo.suddenDeathR);
    }

    /**
     * 增加召唤兽时间 X%
     *
     * @return
     */
    public int getSummonTimeInc() {
        return info.get(MapleStatInfo.summonTimeR);
    }

    /**
     * 增加MP药物效果 X%
     *
     * @return
     */
    public int getMPConsumeEff() {
        return info.get(MapleStatInfo.mpConEff);
    }

    /*
     * 增加物理攻击力
     */
    public int getAttackX() {
        return info.get(MapleStatInfo.padX);
    }

    /*
     * 增加魔法攻击力
     */
    public int getMagicX() {
        return info.get(MapleStatInfo.madX);
    }

    /*
     * 最大Hp增加 按百分比
     */
    public int getPercentHP() {
        return info.get(MapleStatInfo.mhpR);
    }

    /*
     * 最大Mp增加 按百分比
     */
    public int getPercentMP() {
        return info.get(MapleStatInfo.mmpR);
    }

    /*
     * 受到怪物攻击的伤害减少x%
     */
    public int getIgnoreMobDamR() {
        return info.get(MapleStatInfo.ignoreMobDamR);
    }

    /*
     * 防御率无视x%
     */
    public int getIndieIgnoreMobpdpR() {
        return info.get(MapleStatInfo.indieIgnoreMobpdpR);
    }

    /*
     * 受到伤害减少x%
     */
    public int getDamAbsorbShieldR() {
        return info.get(MapleStatInfo.damAbsorbShieldR);
    }

    public int getConsume() {
        return consumeOnPickup;
    }

    /**
     * 自爆伤害
     *
     * @return
     */
    public int getSelfDestruction() {
        return info.get(MapleStatInfo.selfDestruction);
    }

    public int getGauge() {
        return info.get(MapleStatInfo.gauge);
    }

    public int getCharColor() {
        return charColor;
    }

    public List<Integer> getPetsCanConsume() {
        return petsCanConsume;
    }

    public boolean isReturnScroll() {
        return skill && (sourceid == 幻影.幻影回归 //幻影回归 - 返回到幻影的专用飞艇水晶花园中。
                || sourceid == 80001040 //精灵的祝福
                || sourceid == 20021110 //精灵的祝福
        );
    }

    public int getRange() {
        return info.get(MapleStatInfo.range);
    }

    /*
     * 回避率增加 x%
     */
    public int getER() {
        return info.get(MapleStatInfo.er);
    }

    public int getPrice() {
        return info.get(MapleStatInfo.price);
    }

    public int getExtendPrice() {
        return info.get(MapleStatInfo.extendPrice);
    }

    public int getPeriod() {
        return info.get(MapleStatInfo.period);
    }

    public int getReqGuildLevel() {
        return info.get(MapleStatInfo.reqGuildLevel);
    }

    public byte getEXPRate() {
        return expR;
    }

    public short getLifeID() {
        return lifeId;
    }

    public short getUseLevel() {
        return useLevel;
    }

    /*
     * 矿(药)背包道具需要
     */
    public byte getSlotCount() {
        return slotCount;
    }

    public byte getSlotPerLine() {
        return slotPerLine;
    }

    /*
     * 增加力量
     */
    public int getStr() {
        return info.get(MapleStatInfo.str);
    }

    public int getStrX() {
        return info.get(MapleStatInfo.strX);
    }

    public int getStrFX() {
        return info.get(MapleStatInfo.strFX);
    }

    public int getStrRate() {
        return info.get(MapleStatInfo.strR);
    }

    /*
     * 增加敏捷
     */
    public int getDex() {
        return info.get(MapleStatInfo.dex);
    }

    public int getDexX() {
        return info.get(MapleStatInfo.dexX);
    }

    public int getDexFX() {
        return info.get(MapleStatInfo.dexFX);
    }

    public int getDexRate() {
        return info.get(MapleStatInfo.dexR);
    }

    /*
     * 增加智力
     */
    public int getInt() {
        return info.get(MapleStatInfo.int_);
    }

    public int getIntX() {
        return info.get(MapleStatInfo.intX);
    }

    public int getIntFX() {
        return info.get(MapleStatInfo.intFX);
    }

    public int getIntRate() {
        return info.get(MapleStatInfo.intR);
    }

    /*
     * 增加运气
     */
    public int getLuk() {
        return info.get(MapleStatInfo.luk);
    }

    public int getLukX() {
        return info.get(MapleStatInfo.lukX);
    }

    public int getLukFX() {
        return info.get(MapleStatInfo.lukFX);
    }

    public int getLukRate() {
        return info.get(MapleStatInfo.lukR);
    }

    /*
     * 最大HP增加
     */
    public int getMaxHpX() {
        return info.get(MapleStatInfo.mhpX);
    }

    /*
     * 最大MP增加
     */
    public int getMaxMpX() {
        return info.get(MapleStatInfo.mmpX);
    }

    /*
     * 命中值增加
     */
    public int getAccX() {
        return info.get(MapleStatInfo.accX);
    }

    /*
     * 命中值增加 x%
     */
    public int getPercentAcc() {
        return info.get(MapleStatInfo.accR);
    }

    /*
     * 回避值增加
     */
    public int getAvoidX() {
        return info.get(MapleStatInfo.evaX);
    }

    /*
     * 回避值增加 x%
     */
    public int getPercentAvoid() {
        return info.get(MapleStatInfo.evaR);
    }

    /*
     * 物理防御力增加
     */
    public int getWdefX() {
        return info.get(MapleStatInfo.pddX);
    }

    /*
     * 魔法防御力增加
     */
    public int getMdefX() {
        return info.get(MapleStatInfo.mddX);
    }

    /*
     * Hp增加
     */
    public int getIndieMHp() {
        return info.get(MapleStatInfo.indieMhp);
    }

    /*
     * Mp增加
     */
    public int getIndieMMp() {
        return info.get(MapleStatInfo.indieMmp);
    }

    /*
     * 百分比MaxHp增加
     */
    public int getIndieMhpR() {
        return info.get(MapleStatInfo.indieMhpR);
    }

    /*
     * 百分比MaxMp增加
     */
    public int getIndieMmpR() {
        return info.get(MapleStatInfo.indieMmpR);
    }

    /*
     * 所有属性增加
     */
    public int getIndieAllStat() {
        return info.get(MapleStatInfo.indieAllStat);
    }

    /*
     * 爆击概率增加 %
     */
    public int getIndieCr() {
        return info.get(MapleStatInfo.indieCr);
    }

    /**
     * *
     * 增加攻击力
     *
     * @return
     */
    public int getEpdd() {
        return info.get(MapleStatInfo.epad);
    }

    public short getIndiePdd() {
        return indiePdd;
    }

    public short getIndieMdd() {
        return indieMdd;
    }

    /*
     * 攻击力提高 %
     */
    public int getIndieDamR() {
        return info.get(MapleStatInfo.indieDamR);
    }

    /*
     * 提高攻击速度
     */
    public int getIndieBooster() {
        return info.get(MapleStatInfo.indieBooster);
    }

    public byte getType() {
        return type;
    }

    /*
     * 攻击BOSS时，伤害增加x%
     */
    public int getBossDamage() {
        return info.get(MapleStatInfo.bdR);
    }

    /*
     * 攻击时怪物数量少于技能的数量伤害提高
     */
    public int getMobCountDamage() {
        return info.get(MapleStatInfo.mobCountDamR);
    }

    public int getInterval() {
        return interval;
    }

    public List<Pair<Integer, Integer>> getAvailableMaps() {
        return availableMap;
    }

    /*
     * 增加物防 按百分比
     */
    public int getWDEFRate() {
        return info.get(MapleStatInfo.pddR);
    }

    /*
     * 增加魔防 按百分比
     */
    public int getMDEFRate() {
        return info.get(MapleStatInfo.mddR);
    }

    /*
     * 新增变量
     */
    public int getKillSpree() {
        return info.get(MapleStatInfo.kp);
    }

    /*
     * 技能伤害最大值
     */
    public int getMaxDamageOver() {
        return info.get(MapleStatInfo.MDamageOver);
    }

    /*
     * 技能伤害最大值
     */
    public int getIndieMaxDamageOver() {
        return info.get(MapleStatInfo.indieMaxDamageOver);
    }

    /*
     * 消耗更多的 Mp 来增加技能的伤害
     */
    public int getCostMpRate() {
        return info.get(MapleStatInfo.costmpR);
    }

    /*
     * 技能Mp消耗减少 %
     */
    public int getMPConReduce() {
        return info.get(MapleStatInfo.mpConReduce);
    }

    /*
     * 恶魔的最大DF增加 也就是恶魔精气
     */
    public int getIndieMaxDF() {
        return info.get(MapleStatInfo.MDF);
    }

    /*
     * 格外增加攻击怪物的数量
     */
    public int getTargetPlus() {
        return info.get(MapleStatInfo.targetPlus);
    }

    /*
     * 使用技能消耗
     */
    public int getForceCon() {
        return info.get(MapleStatInfo.forceCon);
    }

    /*
     * 使用灵魂技能
     */
    public int getSoulMpCon() {
        return info.get(MapleStatInfo.soulmpCon);
    }

    /*
     * 使用pp技能消耗
     */
    public int getPPCon() {
        return info.get(MapleStatInfo.ppCon);
    }

    public boolean isOnRule() {
        return ruleOn;
    }

    public boolean is疾风() {
        return skill && sourceid == 奇袭者.台风 || sourceid == 奇袭者.疾风;
    }

    public int a(MapleCharacter player, MonsterStatus stat) {
        int n2 = 0;
        n2 = player.getMap().getMapObjectsInRect(calculateBoundingBox(player.getTruePosition(), player.isFacingLeft()), Collections.singletonList(MapleMapObjectType.MONSTER)).stream()
                .filter(y2 -> ((MapleMonster) y2).isMyPoisons(stat, player.getId()))
                .map(y2 -> 1)
                .reduce(n2, Integer::sum);
        return n2;
    }

    public void applyBuffEffect(MapleCharacter applyfrom, MapleCharacter applyto, boolean primary, int newDuration, boolean passive) {
        int localDuration = newDuration;
        if (primary) {
            localDuration = Math.max(newDuration, alchemistModifyVal(applyfrom, localDuration, false));
        }
        if (!primary && isRepeatEffect()) {
            localDuration = 180000;
        }
        Map<MapleBuffStat, Integer> localstatups = statups, maskedStatups = null;
        boolean normal = true; //是否为正常没有修改的BUFF
        boolean showEffect = primary; //是否显示BUFF状态效果
        int maskedDuration = 0; //这个是设置1个自动BUFF的意思 也就是注册的消失的时间 但是BUFF的持续时间是另外1个 也就是1个间隔的意思
        byte[] buff = null;
        byte[] foreignbuff = null;
        int direction = 1;

//        MapleSkillEffectApp.effectEventData effectEventData = new MapleSkillEffectApp.effectEventData(localstatups, localDuration);
//        MapleSkillEffectApp.applyBuffEffect(this, applyfrom, applyto, primary, effectEventData);
        switch (sourceid) {
            case 冲锋队长.幸运骰子:
            case 船长.幸运骰子:
            case 神炮王.幸运骰子:
            case 机械师.幸运骰子: {
                int dice = Randomizer.nextInt(6) + 1;
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, dice, -1, level), false);
                applyto.getClient().announce(EffectPacket.showOwnDiceEffect(sourceid, dice, -1, level));
                if (dice <= 1) {
                    applyto.dropMessage(-10, "幸运骰子技能失败。");
                    return;
                }
                localstatups = Collections.singletonMap(MapleBuffStat.幸运骰子, dice);
                applyto.dropMessage(-10, "幸运骰子技能发动了[" + dice + "]号效果。");
                applyto.getClient().announce(BuffPacket.giveDice(dice, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 冲锋队长.双幸运骰子:
            case 船长.双幸运骰子:
            case 神炮王.双幸运骰子:
            case 机械师.双幸运骰子: {
                int dice1 = Randomizer.nextInt(6) + 1;
                int dice2 = makeChanceResult() ? (Randomizer.nextInt(6) + 1) : 0;
                applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, dice1, dice2 > 0 ? -1 : 0, level), false);
                applyto.getClient().announce(EffectPacket.showOwnDiceEffect(sourceid, dice1, dice2 > 0 ? -1 : 0, level));
                if (dice1 <= 1 && dice2 <= 1) {
                    applyto.dropMessage(-10, "双幸运骰子技能失败。");
                    return;
                }
                int buffid = dice1 == dice2 ? (dice1 * 100) : (dice1 <= 1 ? dice2 : (dice2 <= 1 ? dice1 : (dice1 * 10 + dice2)));
                if (buffid >= 100) {
                    applyto.dropMessage(-10, "双幸运骰子技能发动了[" + (buffid / 100) + "]号效果。");
                } else if (buffid >= 10) {
                    applyto.dropMessage(-10, "双幸运骰子技能发动了[" + (buffid / 10) + "]号效果。");
                    applyto.dropMessage(-10, "双幸运骰子技能发动了[" + (buffid % 10) + "]号效果。");
                } else {
                    applyto.dropMessage(-10, "双幸运骰子技能发动了[" + buffid + "]号效果。");
                }
                localstatups = Collections.singletonMap(MapleBuffStat.幸运骰子, buffid);
                applyto.getClient().announce(BuffPacket.giveDice(buffid, sourceid, localDuration, localstatups));
                normal = false;
                showEffect = false;
                break;
            }
            case 400011003: {
                if (applyfrom.getParty() != null) {
                    if (applyto != applyfrom) {
                        localstatups.put(MapleBuffStat.神圣归一, applyfrom.getId());
                        break;
                    }
                    int var49_80 = 0;
                    for (MaplePartyCharacter member : applyfrom.getParty().getMembers()) {
                        MapleCharacter player = applyfrom.getMap().getCharacterById(member.getId());
                        if (member.isOnline() && member.getMapid() == applyfrom.getMapId() && member.getId() != applyfrom.getId() && calculateBoundingBox(applyfrom.getPosition(), applyfrom.isFacingLeft()).intersects(player.getBounds())) {
                            this.applyBuffEffect(applyfrom, player, primary, newDuration);
                            localstatups.put(MapleBuffStat.神圣归一, member.getId());
                            var49_80 = 1;
                            break;
                        }
                    }
                    if (var49_80 != 0) break;
                }
                localstatups.put(MapleBuffStat.SECONDARY_STAT_IndiePMdR, getZ());
                break;
            }
            case 双弩.精灵元素: {
                SkillFactory.getSkill(双弩.元素幽灵_1).getEffect(level).applyTo(applyto);
                SkillFactory.getSkill(双弩.元素幽灵_2).getEffect(level).applyTo(applyto);
                break;
            }
            case 双弩.元素幽灵_1:
            case 双弩.元素幽灵_2:
            case 双弩.元素幽灵_3: {
                statups.remove(MapleBuffStat.SECONDARY_STAT_IndieQrPointTerm);
                break;
            }
            case 英雄.燃灵之剑: {
                if (applyto.getStatForBuff(MapleBuffStat.变换攻击) == null || applyto.getBuffStatValueHolder(MapleBuffStat.变换攻击) == null)
                    break;
                applyto.updateSummonBySkillID(英雄.燃灵之剑, 400011002, false);
                return;
            }
            case 夜光.真理之门: {
//                applyto.setenattacke(false);
                applyto.updateBuffEffect(applyto.getStatForBuff(MapleBuffStat.光暗转换), Collections.singletonMap(MapleBuffStat.光暗转换, applyto.getBuffStatValueHolder(MapleBuffStat.光暗转换).value));
                break;
            }
            case 侠盗.暗影抨击:
            case 侠盗.暗影抨击_1:
            case 侠盗.暗影抨击_2:
            case 侠盗.暗影抨击_3: {
                if (applyto.getStatForBuff(MapleBuffStat.暗影抨击) != null) {
                    int n56 = applyto.getBuffedIntValue(MapleBuffStat.暗影抨击) - 1;
                    n56 = Math.max(n56, 0);
                    statups.put(MapleBuffStat.暗影抨击, n56);
                    if (n56 == 0) {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.暗影抨击);
                    }
                    return;
                }
                break;
            }
            case 尖兵.超能光束炮: {
                localDuration = 2100000000;
                break;
            }
            case 幻影.王牌: {
                showEffect = false;
                break;
            }
            case 幻影.小丑_2:
            case 幻影.小丑_3:
            case 幻影.小丑_4:
            case 幻影.小丑_5:
            case 幻影.小丑_6: {
                direction = 1;
                applyto.send(EffectPacket.showOwnBuffEffect(幻影.王牌, 1, applyto.getLevel(), this.level, (byte) 0));
                showEffect = true;
                break;
            }
            case 爆破手.装填弹药: {
                applyto.hadnleBlasterClip(8);
                break;
            }
            case 爆破手.碎骨巨叉_2:
            case 爆破手.碎骨巨叉_3:
            case 爆破手.碎骨巨叉_4: {
                applyto.hadnleCylinder(-8);
                break;
            }
            case 爆破手.狂暴打击_转管炮:
            case 爆破手.双重爆炸_转管炮:
            case 爆破手.旋转弹_1: {
                applyto.hadnleCylinder(1);
            }
            case 爆破手.爆炸闪动: {
                applyto.hadnleBlasterClip(-1);
                break;
            }
            case 爆破手.忍耐之盾: {
                localstatups.put(MapleBuffStat.忍耐之盾, Math.max(applyto.getHurtHP() * getX(), 100) / 100);
                break;
            }
            case 风灵使者.呼啸暴风: {
                int min = Math.min(applyto.getBuffedIntValue(MapleBuffStat.呼啸暴风) + (passive ? 1 : -1), 2);
                if (min < 0) {
                    return;
                }
                if (passive && applyto.getBuffStatValueHolder(MapleBuffStat.呼啸暴风) != null && System.currentTimeMillis() < applyto.getBuffStatValueHolder(MapleBuffStat.呼啸暴风).startTime + 1000) {
                    return;
                }
                localstatups.put(MapleBuffStat.呼啸暴风, min);
                break;
            }
            case 豹弩游侠.美洲豹风暴: {
                final ArrayList<Integer> list = new ArrayList<>();
                final int[] array3 = {豹弩游侠.召唤美洲豹_黄, 豹弩游侠.召唤美洲豹_红, 豹弩游侠.召唤美洲豹_紫, 豹弩游侠.召唤美洲豹_蓝, 豹弩游侠.召唤美洲豹_剑, 豹弩游侠.召唤美洲豹_雪, 豹弩游侠.召唤美洲豹_玛瑙, 豹弩游侠.召唤美洲豹_铠甲};
                while (true) {
                    final int skillid = array3[Randomizer.nextInt(array3.length)];
                    final int mountid = applyfrom.getIntNoRecord(GameConstants.JAGUAR) / 10 + 豹弩游侠.美洲豹管理;
                    if (!list.contains(skillid) && mountid != skillid) {
                        list.add(skillid);
                        if (list.size() >= getY()) {
                            break;
                        }
                    }
                }
                for (final int intValue2 : list) {
                    Point randomPos;
                    do {
                        randomPos = applyto.getMap().getRandomPos();
                    }
                    while (!calculateBoundingBox(applyto.getTruePosition(), applyto.isFacingLeft()).contains(randomPos));
                    SkillFactory.getSkill(intValue2).getEffect(1).applyTo(applyfrom, applyto, false, randomPos, localDuration, false);
                }
                break;
            }
            case 神炮王.宇宙无敌火炮弹: {
                int min = Math.min(applyto.getBuffedIntValue(MapleBuffStat.宇宙无敌火炮弹) + (passive ? 1 : -1), getY());
                if (min < 0) {
                    return;
                }
                if (passive && applyto.getBuffStatValueHolder(MapleBuffStat.宇宙无敌火炮弹) != null && System.currentTimeMillis() < applyto.getBuffStatValueHolder(MapleBuffStat.宇宙无敌火炮弹).startTime + (long) (1000 * getQ())) {
                    return;
                }
                localstatups.put(MapleBuffStat.宇宙无敌火炮弹, min);
                break;
            }
            case 尖兵.双重防御: {
                if (!passive) {
                    applyfrom.setBuffValue(0);
                }
                localDuration = 2100000000;
                int x = getX() - applyfrom.getBuffValue();
                int z = (11 - x) * getZ();
                localstatups.clear();
                localstatups.put(MapleBuffStat.黑暗高潮, getProp() - applyfrom.getBuffValue() * getY());
                localstatups.put(MapleBuffStat.伤害吸收, z);
                if (x <= 0) {
                    applyfrom.setBuffValue(0);
                    applyfrom.cancelEffect(this, false, -1);
                    return;
                }
                applyfrom.setBuffValue(applyfrom.getBuffValue() + 1);
                break;
            }
            default: {
                if (is卡牌审判()) {
                    int dice = Randomizer.nextInt(sourceid == 幻影.卡牌审判 ? 2 : 4) + 1;
                    int theStat = info.get(MapleStatInfo.v);
                    switch (dice) {
                        case 1: //爆击概率增加 %
                            theStat = info.get(MapleStatInfo.v);
                            break;
                        case 2: //物品掉落率增加 %
                            theStat = info.get(MapleStatInfo.w);
                            break;
                        case 3: //状态异常抗性/属性抗性分别增加 %
                            theStat = info.get(MapleStatInfo.x) * 100 + info.get(MapleStatInfo.y);
                            break;
                        case 4: //防御力增加 100%
                            theStat = info.get(MapleStatInfo.s);
                            break;
                        case 5: //攻击时，将伤害的x%转换为HP
                            theStat = info.get(z);
                            break;
                    }
                    applyto.getMap().broadcastMessage(applyto, EffectPacket.showDiceEffect(applyto.getId(), sourceid, dice, -1, level), false);
                    applyto.getClient().announce(EffectPacket.showOwnDiceEffect(sourceid, dice, -1, level));
                    localstatups = Collections.singletonMap(MapleBuffStat.卡牌审判, dice);
                    applyto.getClient().announce(BuffPacket.give卡牌审判(sourceid, localDuration, localstatups, theStat));
                    normal = false;
                    showEffect = false;
                } else if (is极速领域()) {
                    buff = BuffPacket.givePirateBuff(statups, localDuration / 1000, sourceid);
                } else if (is喵喵卡片()) {
                    int dice = Randomizer.rand(1, sourceid == 林之灵.喵喵金卡 ? 4 : 3);
                    int buffid = sourceid == 林之灵.喵喵金卡 ? 林之灵.金色卡片 : 林之灵.红色卡片;
                    switch (dice) {
                        case 1:
                            buffid = 林之灵.红色卡片;
                            applyto.dropMessage(-10, "喵喵卡片抽取到 红色卡片 效果。");
                            break;
                        case 2:
                            buffid = 林之灵.蓝色卡片;
                            applyto.dropMessage(-10, "喵喵卡片抽取到 蓝色卡片 效果。");
                            break;
                        case 3:
                            buffid = 林之灵.绿色卡片;
                            applyto.dropMessage(-10, "喵喵卡片抽取到 绿色卡片 效果。");
                            break;
                        case 4:
                            buffid = 林之灵.金色卡片;
                            applyto.dropMessage(-10, "喵喵卡片抽取到 金色卡片 效果。");
                            break;
                    }
                    int skillLevel = buffid == 林之灵.金色卡片 ? applyto.getTotalSkillLevel(林之灵.喵喵金卡) : applyto.getTotalSkillLevel(林之灵.喵喵卡片);
                    if (skillLevel > 0) {
                        SkillFactory.getSkill(buffid).getEffect(skillLevel).applyTo(applyto);
                    }
                    int cooldown = getCooldown(applyto);
                    if (cooldown > 0 && !applyto.skillisCooling(sourceid)) {
                        applyto.getClient().announce(MaplePacketCreator.skillCooldown(sourceid, cooldown));
                        applyto.addCooldown(sourceid, System.currentTimeMillis(), cooldown * 1000);
                    }
                    return;
                } else if (is愤怒之火()) {
                    if (!primary) { //队员没有愤怒之火反射伤害效果
                        localstatups = Collections.singletonMap(MapleBuffStat.增加物理攻击力, info.get(MapleStatInfo.indiePad));
                    }
                } else if (is舞力全开()) {
                    if (!primary) { //队员没有无敌状态
                        localstatups = Collections.singletonMap(MapleBuffStat.提升伤害百分比, info.get(MapleStatInfo.indieDamR));
                    }
                } else if (is弓手火眼晶晶()) {
                    if (applyfrom.getTotalSkillLevel(神射手.火眼晶晶_神圣暴击) > 0) {
                        localstatups = Collections.singletonMap(MapleBuffStat.火眼晶晶, ((info.get(MapleStatInfo.x) + 5) << 8) + info.get(MapleStatInfo.y));
                    }
                } else if (is极限射箭()) {
                    if (applyto.getBuffedValue(MapleBuffStat.极限射箭) != null) {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.极限射箭);
                        return;
                    }
                } else if (is疾驰()) {
                    buff = BuffPacket.givePirateBuff(statups, localDuration / 1000, sourceid);
                    foreignbuff = BuffPacket.giveForeignDash(statups, localDuration / 1000, applyto.getId(), sourceid);
                } else if (is导航辅助()) {
                    if (applyto.getFirstLinkMid() > 0) {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.导航辅助);
                        buff = BuffPacket.give导航辅助(sourceid, applyto.getFirstLinkMid(), 1);
                    } else {
                        return;
                    }
                } else if (is神秘瞄准术()) {
                    if (applyto.getFirstLinkMid() > 0 && !applyto.getAllLinkMid().isEmpty()) {
                        buff = BuffPacket.give神秘瞄准术(applyto.getAllLinkMid().size() * info.get(MapleStatInfo.x), sourceid, localDuration);
                    } else {
                        return;
                    }
                } else if (is神圣之火()) {
                    localstatups = new EnumMap<>(MapleBuffStat.class);
                    int addHp = applyfrom.getTotalSkillLevel(黑骑士.神圣之火_额外体力点数) > 0 ? 20 : 0;
                    int addMp = applyfrom.getTotalSkillLevel(黑骑士.神圣之火_额外魔法点数) > 0 ? 20 : 0;
                    localstatups.put(MapleBuffStat.MAXHP, info.get(MapleStatInfo.x) + addHp);
                    localstatups.put(MapleBuffStat.MAXMP, info.get(MapleStatInfo.x) + addMp);
                } else if (is潜入()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.潜入状态, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is隐身术()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.隐身术, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is神圣魔法盾()) {
                    if (applyfrom.getTotalSkillLevel(主教.神圣魔法盾_额外格挡) > 0) { //格外增加2次概率
                        localstatups = Collections.singletonMap(MapleBuffStat.神圣魔法盾, info.get(MapleStatInfo.x) + 2);
                    }
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.神圣魔法盾, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (sourceid == 双弩.水盾) {
                    int add_X = applyfrom.getTotalSkillLevel(双弩.水盾_强化) > 0 ? 10 : 0;
                    int add_terR = applyfrom.getTotalSkillLevel(双弩.水盾_抗性提升1) > 0 ? 10 : 0;
                    int add_asrR = applyfrom.getTotalSkillLevel(双弩.水盾_抗性提升2) > 0 ? 10 : 0;
                    if (add_terR > 0 || add_asrR > 0 || add_X > 0) {
                        localstatups = new EnumMap<>(MapleBuffStat.class);
                        localstatups.put(MapleBuffStat.异常抗性, info.get(MapleStatInfo.terR) + add_terR);
                        localstatups.put(MapleBuffStat.属性抗性, info.get(MapleStatInfo.asrR) + add_asrR);
                        localstatups.put(MapleBuffStat.伤害吸收, info.get(MapleStatInfo.x) + add_X);
                    }
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.伤害吸收, info.get(MapleStatInfo.x));
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (sourceid == 圣骑士.雷鸣冲击) {
                    if (applyto.getBuffedValue(MapleBuffStat.属性攻击) != null && applyto.getBuffSource(MapleBuffStat.属性攻击) != sourceid) {
                        localstatups = Collections.singletonMap(MapleBuffStat.雷鸣冲击, 1);
                    }
                    buff = BuffPacket.giveBuff(sourceid, localDuration, localstatups, this, applyto);
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.属性攻击, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is元气恢复()) {
                    int healRate = Math.min(50, applyto.getBuffedIntValue(MapleBuffStat.元气恢复) + 10);
                    localstatups = Collections.singletonMap(MapleBuffStat.元气恢复, healRate);
                } else if (sourceid == 圣骑士.祝福护甲) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.祝福护甲, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is斗气集中()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.斗气集中, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (sourceid == 神射手.无形箭 || sourceid == 箭神.无形箭) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.无形箭弩, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is进阶祝福()) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.牧师祝福);
                    int add_indiePad = applyfrom.getTotalSkillLevel(主教.进阶祝福_额外伤害) > 0 ? 20 : 0;
                    int add_indieMhp = applyfrom.getTotalSkillLevel(主教.进阶祝福_额外点数) > 0 ? 1000 : 0;
                    int add_bossDamage = applyfrom.getTotalSkillLevel(主教.进阶祝福_BOSS杀手) > 0 ? 10 : 0;
                    localstatups = new EnumMap<>(MapleBuffStat.class);
                    localstatups.put(MapleBuffStat.进阶祝福, (int) level);
                    if (add_indiePad > 0) {
                        localstatups.put(MapleBuffStat.增加物理攻击力, add_indiePad);
                        localstatups.put(MapleBuffStat.增加魔法攻击力, add_indiePad);
                    }
                    localstatups.put(MapleBuffStat.增加最大HP, info.get(MapleStatInfo.indieMhp) + add_indieMhp);
                    localstatups.put(MapleBuffStat.增加最大MP, info.get(MapleStatInfo.indieMmp) + add_indieMhp);
                    if (add_bossDamage > 0) {
                        localstatups.put(MapleBuffStat.SECONDARY_STAT_IndieBDR, add_bossDamage);
                    }
                } else if (is影分身()) {
                    if (is全息投影()) {
                        localstatups = Collections.singletonMap(MapleBuffStat.影分身, applyto.getStat().getCurrentMaxHp() * info.get(MapleStatInfo.x));
                    }
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.影分身, info.get(MapleStatInfo.x));
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is侠盗本能()) {
                    int killSpree = Math.min(applyto.getBuffedIntValue(MapleBuffStat.击杀点数), 5);
                    if (passive) { //如果是被动使用
                        if (killSpree >= 5) { //最多只有5点击杀点数
                            return;
                        }
                        killSpree++;
                        if (applyto.isShowPacket()) {
                            applyto.dropSpouseMessage(0x0A, "当前击杀点数: " + killSpree);
                        }
                        localstatups = Collections.singletonMap(MapleBuffStat.击杀点数, killSpree);
                        foreignbuff = BuffPacket.giveForeignBuff(applyto, localstatups, this);
                    } else {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.击杀点数);
                        localstatups = Collections.singletonMap(MapleBuffStat.增加物理攻击力, (info.get(MapleStatInfo.x) + (info.get(MapleStatInfo.kp)) * killSpree));
                    }
                } else if (is提速时刻()) {
                    MapleBuffStat status_ = applyto.isZeroSecondLook() ? MapleBuffStat.提速时刻_战斗 : MapleBuffStat.提速时刻_侦查;
                    int buffvalue = applyto.getBuffedIntValue(status_);
                    buffvalue++;
                    buffvalue = Math.min(buffvalue, 10);
                    //applyto.cancelEffectFromBuffStat(status_);
                    localstatups = Collections.singletonMap(status_, buffvalue);
                } else if (is黑暗幻影()) {
                    applyto.dispelSkill(夜行者.影子侍从);
                    if (applyto.getTotalSkillLevel(夜行者.影子侍从) > 0) {
                        SkillFactory.getSkill(夜行者.影子侍从).getEffect(applyto.getTotalSkillLevel(夜行者.影子侍从)).applyTo(applyto);
                    }
                    SkillFactory.getSkill(夜行者.黑暗幻影_影子40).getEffect(1).applyTo(applyto);
                    SkillFactory.getSkill(夜行者.黑暗幻影_影子20).getEffect(1).applyTo(applyto);
                } else if (sourceid == 奇袭者.元素_闪电) {
                    if (passive) { //如果是被动使用
                        localDuration = 30 * 1000; //被动的BUFF持续时间为 30 秒
                        int raidenCount = applyto.getStat().raidenCount; //获取角色雷电累计的最大上限次数
                        int count = Math.min(applyto.getBuffedIntValue(MapleBuffStat.百分比无视防御) / 5, raidenCount); //最大只有 5*5%
                        if (count < raidenCount && raidenCount > 0) {
                            count++;
                        }
                        localstatups = Collections.singletonMap(MapleBuffStat.百分比无视防御, count * 5);
                    } else {
                        localstatups = Collections.singletonMap(MapleBuffStat.元素属性, 1);
                    }
                } else if (sourceid == 圣骑士.连环环破) {
                    int count = Math.min(applyto.getBuffedIntValue(MapleBuffStat.元素冲击) / 5, 5);
                    if (count < 5) {
                        return;
                    }
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.元素冲击);
                } else if (is疾风()) {
                    int count = Math.min(applyto.getBuffedIntValue(MapleBuffStat.百分比无视防御) / 5, applyto.getStat().raidenCount);
                    int skillLevel = applyto.getTotalSkillLevel(奇袭者.台风);
                    if (count < (skillLevel > 0 ? 2 : 3)) {
                        applyto.dropMessage(5, "雷电增益不足，无法使用技能。");
                        return;
                    }
                    int value = info.get(MapleStatInfo.y);
                    if (skillLevel > 0) {
                        MapleStatEffect effect = SkillFactory.getSkill(奇袭者.台风).getEffect(skillLevel);
                        value = effect.getY();
                        localDuration = effect.getDuration(applyto);
                    }
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.百分比无视防御);
                    localstatups = Collections.singletonMap(MapleBuffStat.提升伤害百分比, value * count);
                } else if (is属性攻击()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.属性攻击, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is终极无限()) {
                    maskedDuration = alchemistModifyVal(applyfrom, 4000, false);
                } else if (is尖兵支援()) {
                    maskedDuration = 4000; //设置尖兵支援的间隔时间为 4 秒
                } else if (is黑暗高潮()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.黑暗高潮, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is黑暗祝福()) {
                    localDuration = 0;
                    applyto.getClient().announce(EffectPacket.showBlessOfDarkness(sourceid));
                } else if (is圣洁之力()) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.圣洁之力);
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.圣洁之力, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is神圣迅捷()) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.神圣迅捷);
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.神圣迅捷, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is葵花宝典()) {
                    applyto.handleOrbconsume(1);
                } else if (is剑刃之壁()) {
                    if (sourceid == 狂龙战士.剑刃之壁 && applyfrom.getTotalSkillLevel(狂龙战士.进阶剑刃之壁) > 0) {
                        SkillFactory.getSkill(狂龙战士.进阶剑刃之壁).getEffect(applyfrom.getTotalSkillLevel(狂龙战士.进阶剑刃之壁)).applyBuffEffect(applyfrom, applyto, primary, newDuration);
                        return;
                    }
                    Item weapon = applyto.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                    if (weapon != null) {
                        Equip skin = (Equip) weapon;
                        int itemId = skin.getItemSkin() % 10000 > 0 ? skin.getItemSkin() : weapon.getItemId();
                        Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.剑刃之壁, (int) level);
                        buff = BuffPacket.give剑刃之壁(sourceid, localDuration, localstatups, itemId, get剑刃之壁类型());
                        foreignbuff = BuffPacket.show剑刃之壁(applyto.getId(), sourceid, stat, itemId, get剑刃之壁类型());

                        // 剑刃之壁的冷却时间单独处理
                        int cooldown = getCooldown(applyto);
                        if (cooldown > 0 && !applyto.skillisCooling(狂龙战士.剑刃之壁)) {
                            applyto.getClient().announce(MaplePacketCreator.skillCooldown(狂龙战士.剑刃之壁, cooldown));
                            applyto.addCooldown(狂龙战士.剑刃之壁, System.currentTimeMillis(), cooldown * 1000);
                        }
                    } else {
                        applyto.dropMessage(5, "佩戴的武器无法使用此技能。");
                        return;
                    }
//        } else if (is金属机甲()) {
//            if (sourceid == 机械师.金属机甲_人类 && applyfrom.getTotalSkillLevel(机械师.终极机甲) > 0) {
//                localstatups = new ArrayList<>(SkillFactory.getSkill(机械师.终极机甲).getEffect(applyfrom.getTotalSkillLevel(机械师.终极机甲)).statups);
//                localstatups.add(new Pair<>(MapleBuffStat.增加最大HP百分比, applyfrom.getTotalSkillLevel(机械师.终极机甲)));
//            }
//            List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.骑兽技能, 0));
//            foreignbuff = BuffPacket.showMonsterRiding(applyto.getId(), stat, 机械师.金属机甲_人类, 1932016);
//            List<Pair<MapleBuffStat, Integer>> localstatups_ = new ArrayList<>(SkillFactory.getSkill(预备兵.隐藏碎片).getEffect(1).statups);
//            applyfrom.getClient().announce(BuffPacket.giveBuff(sourceid, newDuration, localstatups_, this, applyto));
                } else if (isMorph()) {
                    if (is冰骑士()) {
                        Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.SECONDARY_STAT_IceKnight, 2);
                        buff = BuffPacket.giveBuff(0, localDuration, stat, this, applyto);
                    }
//                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.变身效果, getMorph(applyto));
//                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (isInflation()) { //如果是巨人药水
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.巨人药水, (int) inflation);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (charColor > 0) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.FAMILIAR_SHADOW, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is骑兽技能()) {
                    if (sourceid != 龙神.龙神) {
                        localDuration = 0;
                    }
                    localstatups = new EnumMap<>(statups);
                    int mountid = MapleStatEffectFactory.parseMountInfo(applyto, sourceid);
                    int mountid2 = MapleStatEffectFactory.parseMountInfo_Pure(applyto, sourceid);
                    if (mountid != 0 && mountid2 != 0) {
                        localstatups.put(MapleBuffStat.骑兽技能, mountid2);
                        if (is金属机甲()) {
                            int skilllevel_ = applyfrom.getTotalSkillLevel(机械师.终极机甲);
                            if (skilllevel_ > 0) {
                                localstatups.put(MapleBuffStat.增加最大HP百分比, skilllevel_);
                            }
                        }
                        List<Pair<MapleBuffStat, Integer>> stat = Collections.singletonList(new Pair<>(MapleBuffStat.骑兽技能, 0));
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.战神抗压);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.伤害反击);
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.召唤美洲豹);
                        foreignbuff = BuffPacket.showMonsterRiding(applyto.getId(), stat, mountid, sourceid);
                    } else {
                        if (applyto.isAdmin()) {
                            applyto.dropSpouseMessage(0x0A, "骑宠BUFF " + sourceid + " 错误，未找到这个骑宠的外形ID。");
                        }
                        return;
                    }
                } else if (isSoaring()) { //飞翔
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.飞翔, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is高空飞行()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.飞行骑乘, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (berserk > 0) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.天使状态, 0);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is狂暴战魂() || berserk2 > 0) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.狂暴战魂, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is金刚霸体()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.金刚霸体, 1);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is天使物品()) {
                    if (applyto.getStat().equippedSummon <= 0) {
                        localstatups = Collections.singletonMap(MapleBuffStat.天使状态, 1);
                    }
                } else if (sourceid == 魂骑士.元素_灵魂) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.元素灵魂, (int) level);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is月光转换()) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.月光转换);
                    int skillLevel = applyto.getTotalSkillLevel(魂骑士.人剑合一); //判断角色人剑合一技能是否大于0
                    if (skillLevel > 0) {
                        MapleStatEffect effect;
                        if (sourceid == 魂骑士.月光洒落 || sourceid == 魂骑士.日月轮转_月光洒落) {
                            effect = SkillFactory.getSkill(魂骑士.人剑合一).getEffect(skillLevel);
                            if (effect != null) {
                                localstatups = new EnumMap<>(MapleBuffStat.class);
                                localstatups.put(MapleBuffStat.霰弹炮, (int) level);
                                localstatups.put(MapleBuffStat.月光转换, 1); //1 = 月光 2 = 旭日
                                localstatups.put(MapleBuffStat.暴击概率, effect.getIndieCr());
                            }
                        } else if (sourceid == 魂骑士.旭日 || sourceid == 魂骑士.日月轮转_旭日) {
                            effect = SkillFactory.getSkill(魂骑士.人剑合一_旭日).getEffect(skillLevel);
                            if (effect != null) {
                                localstatups = new EnumMap<>(MapleBuffStat.class);
                                localstatups.put(MapleBuffStat.月光转换, 2); //1 = 月光 2 = 旭日
                                localstatups.put(MapleBuffStat.提高攻击速度, effect.getIndieBooster());
                                localstatups.put(MapleBuffStat.提升伤害百分比, effect.getIndieDamR());
                            }
                        }
                    }
                    //其他玩家看的技能效果
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.月光转换, sourceid == 魂骑士.月光洒落 ? 1 : 2);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is信天翁新()) {
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.元素灵魂, (int) level);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (isSummonSkill()) { //如果是召唤兽技能
                    localstatups = new EnumMap<>(statups);
                    localstatups.remove(MapleBuffStat.召唤兽);
                    normal = localstatups.size() > 0;
                } else if (is符文状态()) {
                    applyto.dispelSkill(80001427);
                    applyto.dispelSkill(80001428);
                    applyto.dispelSkill(80001430);
                    applyto.dispelSkill(80001432);
                } else if (is模式变更()) {
                    applyto.cancelEffectFromBuffStat(MapleBuffStat.守护模式变更);
                    if (sourceid != 林之灵.巨熊模式) {
                        applyto.dispelBuffByJobId(11200);
                    }
                    if (sourceid != 林之灵.雪豹模式) {
                        applyto.dispelBuffByJobId(11210);
                    }
                    if (sourceid != 林之灵.猛鹰模式) {
                        applyto.dispelBuffByJobId(11211);
                    }
                    if (sourceid != 林之灵.猫咪模式) {
                        applyto.dispelBuffByJobId(11212);
                    }
                } else if (is伤害置换()) {
                    if (!passive) {
                        return;
                    }
                    int xishou = (int) (applyto.getTotDamageToMob() * makeRate(info.get(MapleStatInfo.y)));
                    if (applyto.isAdmin()) {
                        applyto.dropSpouseMessage(0x14, "[伤害置换] 当前打怪总伤害: " + applyto.getTotDamageToMob() + " 吸收转换数值: " + xishou);
                    }
                    localstatups = Collections.singletonMap(MapleBuffStat.伤害置换, xishou);
                } else if (is幸运钱()) {
                    int luck = Math.min(applyto.getBuffedIntValue(MapleBuffStat.幸运钱), 5);
                    if (luck > 0) {
                        if (luck < 5) { //最多叠加5次 幸运钱
                            luck++;
                        }
                        if (applyto.isShowPacket()) {
                            applyto.dropSpouseMessage(0x0A, "当前幸运钱次数: " + luck);
                        }
                        localstatups = new EnumMap<>(MapleBuffStat.class);
                        localstatups.put(MapleBuffStat.幸运钱, luck);
                        localstatups.put(MapleBuffStat.提升伤害百分比, info.get(MapleStatInfo.indieDamR) * luck);
                        localstatups.put(MapleBuffStat.增加伤害最大值, info.get(MapleStatInfo.indieMaxDamageOver) * luck);
                        localstatups.put(MapleBuffStat.暴击概率, info.get(MapleStatInfo.x) * luck);
                    }
                    applyto.switchLuckyMoney(false);
                } else if (is拔刀姿势()) {
                    applyto.dispelSkill(剑豪.基本姿势加成);
                    SkillFactory.getSkill(剑豪.拔刀术加成).getEffect(1).applyTo(applyto);
                } else if (is避柳()) {
                    int counts = Math.min(applyto.getBuffedIntValue(MapleBuffStat.避柳) / info.get(MapleStatInfo.damR) + 1, 5);
                    localstatups = Collections.singletonMap(MapleBuffStat.避柳, info.get(MapleStatInfo.damR) * counts);
                    //applyto.setBuffedValue(MapleBuffStat.避柳, counts);
                } else if (is灵狐()) {
                    if (applyto.getBuffedValue(MapleBuffStat.灵狐) != null) {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.灵狐);
                        return;
                    }
                } else if (is心魂本能()) {
                    if (applyto.getBuffedValue(MapleBuffStat.心魂本能) != null) {
                        applyto.cancelEffectFromBuffStat(MapleBuffStat.心魂本能);
                        return;
                    }
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.心魂本能, (int) level);
                    foreignbuff = BuffPacket.giveForeignBuff(applyto, stat, this);
                } else if (is心魂充能()) {
                    applyto.gainPP((30 - applyto.getSpecialStat().getPP()) / 2);
                } else if (is灵魂助力统治()) {
                    ruleOn = !ruleOn;
                    localDuration = applyto.getBuffedValue(MapleBuffStat.灵魂助力) != null ? (int) (applyto.getBuffStatValueHolder(MapleBuffStat.灵魂助力).localDuration + applyto.getBuffedStartTime(MapleBuffStat.灵魂助力) - System.currentTimeMillis()) : localDuration;
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.灵魂助力, 1);
                    buff = BuffPacket.giveBuff(sourceid, localDuration, stat, this, applyto);
                } else if (is激素引擎()) {
                    if (applyto.getSkillLevel(战神.激素狂飙) > 0) {
                        Skill skill1 = SkillFactory.getSkill(战神.激素狂飙);
                        MapleStatEffect effect1 = skill1.getEffect(applyto.getTotalSkillLevel(战神.激素狂飙));
                        effect1.applyTo(applyto);
                        applyto.setAranCombo(500, true);
                        return;
                    }
                } else if (is神速衔接()) {
                    if (applyto.getBuffedValue(MapleBuffStat.神速衔接) != null) {
                        localstatups = Collections.singletonMap(MapleBuffStat.神速衔接, info.get(MapleStatInfo.damR));
                    }
                }
            }
        }


        //取消一些技能BUFF的效果，以免重复
        if (!is骑兽技能() && !is机械传送门()) {
            applyto.cancelEffect(this, true, -1, localstatups);
        }
        //设置BUFF技能的消失时间 和 注册角色的BUFF状态信息
        long startTime = System.currentTimeMillis();
        if (localDuration > 0) {
            CancelEffectAction cancelAction = new CancelEffectAction(applyto, this, startTime, localstatups);
            ScheduledFuture<?> schedule = BuffTimer.getInstance().schedule(cancelAction, maskedDuration > 0 ? maskedDuration : localDuration);
            applyto.registerEffect(this, startTime, schedule, localstatups, false, localDuration, applyfrom.getId());
        } else {
            applyto.registerEffect(this, startTime, null, localstatups, false, localDuration, applyto.getId());
        }
        //设置BUFF技能的冷却时间
        int cooldown = getCooldown(applyto);
        if (cooldown > 0 && !applyto.skillisCooling(sourceid)) {
            if (applyto.isAdmin()) {
                applyto.dropDebugMessage(2, "[技能冷却] 为GM消除技能冷却时间, 原技能冷却时间:" + cooldown + "秒.");
            } else if (!passive) {
                applyto.getClient().announce(MaplePacketCreator.skillCooldown(sourceid, cooldown));
                applyto.addCooldown(sourceid, startTime, cooldown * 1000);
            }
        }
        //开始加状态BUFF
        if (buff != null) {
            applyto.getClient().announce(buff);
        } else if (normal && localstatups.size() > 0) {
            applyto.getClient().announce(BuffPacket.giveBuff((skill ? sourceid : -sourceid), localDuration, localstatups, this, applyto));
        }
        if (foreignbuff != null && !applyto.isHidden()) {
            applyto.getMap().broadcastMessage(foreignbuff);
        }
        //是否发送给其他玩家显示角色获得BUFF的效果
        if (showEffect && !applyto.isHidden()) {
            applyto.getMap().broadcastMessage(applyto, EffectPacket.showBuffeffect(applyto, sourceid, 1, applyto.getLevel(), level), false);
        }
    }

    /*
     * 取消BUFF的线程操作
     */
    public static class CancelEffectAction implements Runnable {

        private final MapleStatEffect effect;
        private final WeakReference<MapleCharacter> target;
        private final long startTime;
        private final Map<MapleBuffStat, Integer> statup;

        public CancelEffectAction(MapleCharacter target, MapleStatEffect effect, long startTime, Map<MapleBuffStat, Integer> statup) {
            this.effect = effect;
            this.target = new WeakReference<>(target);
            this.startTime = startTime;
            this.statup = statup;
        }

        @Override
        public void run() {
            MapleCharacter realTarget = target.get();
            if (realTarget != null) {
                realTarget.cancelEffect(effect, false, startTime, statup);
            }
        }
    }
}
