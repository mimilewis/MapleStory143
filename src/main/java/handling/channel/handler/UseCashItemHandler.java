/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.*;
import client.inventory.*;
import client.skills.InnerAbillity;
import client.skills.InnerSkillEntry;
import client.skills.Skill;
import client.skills.SkillFactory;
import configs.NebuliteConfig;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConstants;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.*;
import server.PredictCardFactory.PredictCard;
import server.PredictCardFactory.PredictCardComment;
import server.cashshop.CashItemFactory;
import server.life.MapleLifeFactory;
import server.maps.*;
import server.quest.MapleQuest;
import server.shop.MapleShopFactory;
import server.shops.HiredMerchant;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.packet.EffectPacket;
import tools.packet.InventoryPacket;
import tools.packet.MTSCSPacket;
import tools.packet.PetPacket;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author PlayDK
 */
public class UseCashItemHandler {

    private static final Logger log = LogManager.getLogger();

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) throws SQLException {
        if (chr == null || chr.getMap() == null || chr.inPVP()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        chr.updateTick(slea.readInt());
        chr.setScrolledPosition((short) 0);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt(); //物品ID
        int itemType = itemId / 10000; //物品类型,取余
        Item toUse = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getItemId() != itemId || toUse.getQuantity() < 1 || chr.hasBlockedInventory()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isAdmin()) {
            chr.dropMessage(5, "使用商城道具 物品ID: " + itemId + " 物品类型: " + itemType);
        }
        boolean used = false, cc = false;
        switch (itemType) {
            case 504: { //缩地石之类
                if (itemId == 5043000 || itemId == 5043001) {
                    short questid = slea.readShort();
                    int npcid = slea.readInt();
                    MapleQuest quest = MapleQuest.getInstance(questid);
                    if (chr.getQuest(quest).getStatus() == 1 && quest.canComplete(chr, npcid)) {
                        int mapId = MapleLifeFactory.getNPCLocation(npcid);
                        if (mapId != -1) {
                            MapleMap map = c.getChannelServer().getMapFactory().getMap(mapId);
                            if (map.containsNPC(npcid) && !FieldLimitType.VipRock.check(chr.getMap().getFieldLimit()) && !FieldLimitType.VipRock.check(map.getFieldLimit()) && !chr.isInBlockedMap()) {
                                chr.changeMap(map, map.getPortal(0));
                            }
                            used = true;
                        } else {
                            chr.dropMessage(1, "使用道具出现未知的错误.");
                        }
                    }
                } else if (itemId == 5042000) { //豫园高级瞬移之石
                    MapleMap map = c.getChannelServer().getMapFactory().getMap(701000200); //东方神州 - 上海豫园
                    chr.changeMap(map, map.getPortal(0));
                    used = true;
                    break;
                } else if (itemId == 5040005) { //超时空券
                    chr.dropMessage(5, "无法使用这个道具.");
                    break;
                } else if (itemId == 5044011) {
                    slea.skip(1);
                    final MapleMap map = c.getChannelServer().getMapFactory().getMap(slea.readInt());
                    chr.changeMap(map, map.getPortal(0));
                    break;
                } else {
                    used = InventoryHandler.UseTeleRock(slea, c, itemId);
                }
                break;
            }
            case 505: { //洗能力点
                if (itemId == 5050000) {
                    List<Pair<MapleStat, Long>> statupdate = new ArrayList<>(2);
                    int apto = (int) slea.readLong();
                    int apfrom = (int) slea.readLong();
                    int statLimit = ServerConfig.CHANNEL_PLAYER_MAXAP;
                    if (chr.isAdmin()) {
                        chr.dropMessage(5, "洗能力点 apto: " + apto + " apfrom: " + apfrom);
                    }
                    if (apto == apfrom) {
                        break; // Hack
                    }
                    int job = chr.getJob();
                    PlayerStats playerst = chr.getStat();
                    used = true;
                    switch (apto) { // AP to
                        case 64: // 力量
                            if (playerst.getStr() >= statLimit) {
                                used = false;
                            }
                            break;
                        case 128: // 敏捷
                            if (playerst.getDex() >= statLimit) {
                                used = false;
                            }
                            break;
                        case 256: // 智力
                            if (playerst.getInt() >= statLimit) {
                                used = false;
                            }
                            break;
                        case 512: // 运气
                            if (playerst.getLuk() >= statLimit) {
                                used = false;
                            }
                            break;
                        case 2048: // 血
                            if (playerst.getMaxHp() >= chr.getMaxHpForSever()) {
                                used = false;
                            }
                            break;
                        case 8192: // 蓝
                            if (playerst.getMaxMp() >= chr.getMaxMpForSever()) {
                                used = false;
                            }
                            break;
                    }
                    switch (apfrom) { // AP to
                        case 64: // 力量
                            if (playerst.getStr() <= 4) {
                                used = false;
                            }
                            break;
                        case 128: // 敏捷
                            if (playerst.getDex() <= 4) {
                                used = false;
                            }
                            break;
                        case 256: // 智力
                            if (playerst.getInt() <= 4) {
                                used = false;
                            }
                            break;
                        case 512: // 运气
                            if (playerst.getLuk() <= 4) {
                                used = false;
                            }
                            break;
                        case 2048: // 血
                            if (chr.getHpApUsed() <= 0 || chr.getHpApUsed() >= 10000) {
                                used = false;
                            }
                            break;
                        case 8192: // 蓝
                            if (chr.getHpApUsed() <= 0 || chr.getHpApUsed() >= 10000 || JobConstants.isNotMpJob(job)) {
                                used = false;
                            }
                            break;
                    }
                    if (used) {
                        switch (apto) { // AP to
                            case 64: { // 力量
                                long toSet = playerst.getStr() + 1;
                                playerst.setStr((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.力量, toSet));
                                break;
                            }
                            case 128: { // 敏捷
                                long toSet = playerst.getDex() + 1;
                                playerst.setDex((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.敏捷, toSet));
                                break;
                            }
                            case 256: { // 智力
                                long toSet = playerst.getInt() + 1;
                                playerst.setInt((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.智力, toSet));
                                break;
                            }
                            case 512: { // 运气
                                long toSet = playerst.getLuk() + 1;
                                playerst.setLuk((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.运气, toSet));
                                break;
                            }
                            case 2048: // 血
                                int maxhp = playerst.getMaxHp();
                                if (JobConstants.is新手职业(job)) { // 新手
                                    maxhp += Randomizer.rand(4, 8);
                                } else if (JobConstants.is恶魔复仇者(job)) {
                                    maxhp += 30;
                                } else if ((job >= 100 && job <= 132) || (job >= 3200 && job <= 3212) || (job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112) || (job >= 5100 && job <= 5112)) { // 战士
                                    maxhp += Randomizer.rand(36, 42);
                                } else if ((job >= 200 && job <= 232) || (JobConstants.is龙神(job)) || (job >= 1200 && job <= 1212) || (job >= 2700 && job <= 2712)) { // 法师
                                    maxhp += Randomizer.rand(10, 12);
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // Bowman
                                    maxhp += Randomizer.rand(14, 18);
                                } else if ((job >= 510 && job <= 512) || (job >= 580 && job <= 582) || (job >= 1510 && job <= 1512)) {
                                    maxhp += Randomizer.rand(24, 28);
                                } else if ((job >= 500 && job <= 532) || (job >= 590 && job <= 592) || (JobConstants.is龙的传人(job)) || (job >= 3500 && job <= 3512) || job == 1500) { // Pirate
                                    maxhp += Randomizer.rand(16, 20);
                                } else if (job >= 2000 && job <= 2112) { // Aran
                                    maxhp += Randomizer.rand(34, 38);
                                } else { // GameMaster
                                    maxhp += Randomizer.rand(16, 20);
                                }
                                maxhp = Math.min(chr.getMaxHpForSever(), Math.abs(maxhp));
                                chr.setHpApUsed((short) (chr.getHpApUsed() + 1));
                                playerst.setMaxHp(maxhp, chr);
                                statupdate.add(new Pair<>(MapleStat.MAXHP, (long) maxhp));
                                break;
                            case 8192: // 蓝
                                int maxmp = playerst.getMaxMp();
                                if (JobConstants.is新手职业(job)) { // 新手
                                    maxmp += Randomizer.rand(6, 8);
                                } else if (JobConstants.isNotMpJob(job)) {  //恶魔和天使不能洗
                                    return;
                                } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 2000 && job <= 2112) || (job >= 5100 && job <= 5112)) { // 战士
                                    maxmp += Randomizer.rand(4, 9);
                                } else if ((job >= 200 && job <= 232) || (JobConstants.is龙神(job)) || (job >= 3200 && job <= 3212) || (job >= 1200 && job <= 1212) || (job >= 2700 && job <= 2712)) { // 法师
                                    maxmp += Randomizer.rand(32, 36);
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 500 && job <= 592) || (job >= 3200 && job <= 3212) || (job >= 3500 && job <= 3512) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // Bowman
                                    maxmp += Randomizer.rand(8, 10);
                                } else { // GameMaster
                                    maxmp += Randomizer.rand(6, 8);
                                }
                                if (JobConstants.isNotMpJob(job)) {  //恶魔和天使不能洗
                                    chr.dropMessage(1, "该职业无法使用.");
                                } else {
                                    maxmp = Math.min(chr.getMaxMpForSever(), Math.abs(maxmp));
                                    chr.setHpApUsed((short) (chr.getHpApUsed() + 1));
                                    playerst.setMaxMp(maxmp, chr);
                                    statupdate.add(new Pair<>(MapleStat.MAXMP, (long) maxmp));
                                }
                                break;
                        }
                        switch (apfrom) { // AP from
                            case 64: { // 力量
                                long toSet = playerst.getStr() - 1;
                                playerst.setStr((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.力量, toSet));
                                break;
                            }
                            case 128: { // 敏捷
                                long toSet = playerst.getDex() - 1;
                                playerst.setDex((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.敏捷, toSet));
                                break;
                            }
                            case 256: { // 智力
                                long toSet = playerst.getInt() - 1;
                                playerst.setInt((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.智力, toSet));
                                break;
                            }
                            case 512: { // 运气
                                long toSet = playerst.getLuk() - 1;
                                playerst.setLuk((short) toSet, chr);
                                statupdate.add(new Pair<>(MapleStat.运气, toSet));
                                break;
                            }
                            case 2048: // 血
                                int maxhp = playerst.getMaxHp();
                                if (JobConstants.is新手职业(job)) { // 新手
                                    maxhp -= 12;
                                } else if (JobConstants.is恶魔复仇者(job)) {
                                    maxhp -= 30;
                                } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || (job >= 2700 && job <= 2712)) { // 魔法师之类的职业
                                    maxhp -= 10;
                                } else if ((job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // 弓手和飞侠之类的职业
                                    maxhp -= 15;
                                } else if ((job >= 500 && job <= 592) || (job >= 1500 && job <= 1512)) { //海盗职业
                                    maxhp -= 22;
                                } else if (((job >= 100 && job <= 132) || job >= 1100 && job <= 1112) || (job >= 3100 && job <= 3112) || (job >= 5100 && job <= 5112)) { // 战士职业
                                    maxhp -= 32;
                                } else if ((job >= 2000 && job <= 2112) || (job >= 3200 && job <= 3212)) { // 战神和幻灵斗师
                                    maxhp -= 40;
                                } else { // GameMaster
                                    maxhp -= 20;
                                }
                                chr.setHpApUsed((short) (chr.getHpApUsed() - 1));
                                playerst.setMaxHp(maxhp, chr);
                                statupdate.add(new Pair<>(MapleStat.MAXHP, (long) maxhp));
                                break;
                            case 8192: // 蓝
                                int maxmp = playerst.getMaxMp();
                                if (JobConstants.is新手职业(job)) { // 新手
                                    maxmp -= 8;
                                } else if (JobConstants.isNotMpJob(job)) {  //恶魔和天使不能洗
                                    return;
                                } else if ((job >= 100 && job <= 132) || (job >= 1100 && job <= 1112) || (job >= 5100 && job <= 5112)) { // 战士
                                    maxmp -= 4;
                                } else if ((job >= 200 && job <= 232) || (job >= 1200 && job <= 1212) || (job >= 2700 && job <= 2712)) { // 法师
                                    maxmp -= 30;
                                } else if ((job >= 500 && job <= 592) || (job >= 300 && job <= 322) || (job >= 400 && job <= 434) || (job >= 1300 && job <= 1312) || (job >= 1400 && job <= 1412) || (job >= 1500 && job <= 1512) || (job >= 3300 && job <= 3312) || (job >= 3500 && job <= 3512) || (job >= 2300 && job <= 2312) || (job >= 2400 && job <= 2412)) { // Pirate, Bowman. Thief
                                    maxmp -= 10;
                                } else if (job >= 2000 && job <= 2112) { // 战神
                                    maxmp -= 5;
                                } else { // GameMaster
                                    maxmp -= 20;
                                }
                                if (JobConstants.isNotMpJob(job)) {  //恶魔和天使不能洗
                                    chr.dropMessage(1, "该职业无法使用.");
                                } else {
                                    chr.setHpApUsed((short) (chr.getHpApUsed() - 1));
                                    playerst.setMaxMp(maxmp, chr);
                                    statupdate.add(new Pair<>(MapleStat.MAXMP, (long) maxmp));
                                }
                                break;
                        }
                        c.announce(MaplePacketCreator.updatePlayerStats(statupdate, true, chr));
                    }
                } else { //洗技能点卷轴
                    if (itemId >= 5050005 && !JobConstants.is龙神(chr.getJob())) {
                        chr.dropMessage(1, "只有龙神职业才能使用这个道具.");
                        break;
                    } //well i dont really care other than this o.o
                    if (itemId < 5050005 && JobConstants.is龙神(chr.getJob())) {
                        chr.dropMessage(1, "龙神职业无法使用这个道具.");
                        break;
                    } //well i dont really care other than this o.o
                    int skill1 = slea.readInt();
                    int skill2 = slea.readInt();
                    for (int i : GameConstants.blockedSkills) {
                        if (skill1 == i) {
                            chr.dropMessage(1, "该技能未修复，无法增加此技能.");
                            return;
                        }
                    }
                    Skill skillSPTo = SkillFactory.getSkill(skill1);
                    Skill skillSPFrom = SkillFactory.getSkill(skill2);
                    if (skillSPTo.isBeginnerSkill() || skillSPFrom.isBeginnerSkill()) {
                        chr.dropMessage(1, "无法对新手技能使用.");
                        break;
                    }
                    if (JobConstants.getSkillBookBySkill(skill1) != JobConstants.getSkillBookBySkill(skill2)) { //resistance evan
                        chr.dropMessage(1, "You may not add different job skills.");
                        break;
                    }
                    if ((chr.getSkillLevel(skillSPTo) + 1 <= skillSPTo.getMaxLevel()) && chr.getSkillLevel(skillSPFrom) > 0 && skillSPTo.canBeLearnedBy(chr.getJob())) {
                        if (skillSPTo.isFourthJob() && (chr.getSkillLevel(skillSPTo) + 1 > chr.getMasterLevel(skillSPTo))) {
                            chr.dropMessage(1, "You will exceed the master level.");
                            break;
                        }
                        if (itemId >= 5050005) {
                            if (JobConstants.getSkillBookBySkill(skill1) != (itemId - 5050005) * 2 && JobConstants.getSkillBookBySkill(skill1) != (itemId - 5050005) * 2 + 1) {
                                chr.dropMessage(1, "You may not add this job SP using this reset.");
                                break;
                            }
                        } else {
                            int theJob = JobConstants.getJobNumber(skill2 / 10000);
                            switch (skill2 / 10000) {
                                case 430:
                                    theJob = 1;
                                    break;
                                case 432:
                                case 431:
                                    theJob = 2;
                                    break;
                                case 433:
                                    theJob = 3;
                                    break;
                                case 434:
                                    theJob = 4;
                                    break;
                            }
                            if (theJob != itemId - 5050000) { //you may only subtract from the skill if the ID matches Sp reset
                                chr.dropMessage(1, "You may not subtract from this skill. Use the appropriate SP reset.");
                                break;
                            }
                        }
                        chr.changeSingleSkillLevel(skillSPFrom, (byte) (chr.getSkillLevel(skillSPFrom) - 1), chr.getMasterLevel(skillSPFrom));
                        chr.changeSingleSkillLevel(skillSPTo, (byte) (chr.getSkillLevel(skillSPTo) + 1), chr.getMasterLevel(skillSPTo));
                        used = true;
                    }
                }
                break;
            }
            case 506: {
                if (itemId == 5060000) { //装备刻名
                    Item item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(slea.readShort());
                    if (item != null && item.getOwner().equals("")) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (chr.getName().contains(z)) {
                                change = false;
                            }
                        }
                        if (change) {
                            item.setOwner(chr.getName());
                            chr.forceUpdateItem(item);
                            used = true;
                            break;
                        }
                    } else {
                        chr.dropMessage(1, "请将道具直接点在你需要刻名的装备上.");
                        break;
                    }
                } else if (itemId == 5060001 || itemId == 5061000 || itemId == 5061001 || itemId == 5061002 || itemId == 5061003) { //封印之锁
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = chr.getInventory(type).getItem((byte) slea.readInt());
                    if (item != null && item.getExpiration() == -1) {
                        short flag = item.getFlag();
                        flag |= ItemFlag.封印.getValue();
                        item.setFlag(flag);
                        long days = 0;
                        if (itemId == 5061000) { //封印之锁（7天）
                            days = 7;
                        } else if (itemId == 5061001) { //封印之锁：30天
                            days = 30;
                        } else if (itemId == 5061002) { //封印之锁（90天）
                            days = 90;
                        } else if (itemId == 5061003) { //封印之锁（365天）
                            days = 365;
                        }
                        if (chr.isAdmin()) {
                            chr.dropMessage(5, "使用封印之锁 物品ID: " + itemId + " 天数: " + days);
                        }
                        if (days > 0) {
                            item.setExpiration(System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000));
                        }
                        chr.forceUpdateItem(item);
                        used = true;
                        break;
                    } else {
                        chr.dropMessage(1, "使用道具出现错误.");
                        break;
                    }
                } else if (itemId == 5064000 || itemId == 5064003) { //防爆卷轴 - 保护物品的魔法盾。在装备物品上使用，可以在使用卷轴失败时防止装备物品损坏，#c仅限1次#。但是使用卷轴成功时，防御效果也会消失，#c强化12星以上的物品无法使用。# \n可以和#c安全之盾、复原之盾#一起使用。
                    Item item;
                    short dst = slea.readShort();
                    if (dst < 0) {
                        item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    } else {
                        item = null;
                    }
                    if (item != null && item.getType() == 1) {
                        int maxEnhance = itemId == 5064003 ? 7 : 12;
                        if (((Equip) item).getEnhance() >= maxEnhance) {
                            chr.dropMessage(1, "该道具已无法继续使用防爆卷轴效果.");
                            break;
                        }
                        if (!ItemFlag.装备防爆.check(item.getFlag())) {
//                            flag |= ItemFlag.装备防爆.getValue();
//                            item.setFlag(flag);
                            item.addFlag((short) ItemFlag.装备防爆.getValue());
                            chr.forceUpdateItem(item);
                            chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                            used = true;
                        } else {
                            chr.dropMessage(1, "已经获得了相同效果。");
                            break;
                        }
                    } else {
                        chr.dropMessage(1, "请将卷轴点在你需要保护的装备上.");
                        break;
                    }
                } else if (itemId == 5064100) { //保护卷轴 - 保护物品的魔法盾。在装备物品上使用，可以在使用卷轴失败时防止装备物品#c可升级次数#减少，#c仅限1次#。但是使用卷轴成功时，防御效果也会消失。\n可以和#c安全之盾、复原之盾#一起使用。
                    Item item;
                    short dst = slea.readShort();
                    if (dst < 0) {
                        item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    } else {
                        item = null;
                    }
                    if (item != null && item.getType() == 1) {
                        if (!ItemFlag.保护升级次数.check(item.getFlag())) {
//                            flag |= ItemFlag.保护升级次数.getValue();
//                            item.setFlag(flag);
                            item.addFlag((short) ItemFlag.保护升级次数.getValue());
                            chr.forceUpdateItem(item);
                            chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                            used = true;
                        } else {
                            chr.dropMessage(5, "已经获得了相同效果。");
                            break;
                        }
                    } else {
                        chr.dropMessage(5, "请将卷轴点在你需要保护的装备上.");
                        break;
                    }
                } else if (itemId == 5064200) { //完美还原卷轴 - 可以使除潜能之外的所有属性#c初始化#为标准能力值。只能用于装备道具。成长型道具可以恢复到成长之前的状态。\n此外，无法用于使用了#c宿命剪刀、物品共享牌、封印之锁#的道具。附加属性不会初始化。
                    Item item;
                    short dst = slea.readShort();
                    if (dst < 0) {
                        item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    } else {
                        item = null;
                    }
                    if (item != null && item.getType() == 1) {
                        if (!ItemFlag.封印.check(item.getFlag()) && !ItemFlag.宿命剪刀.check(item.getFlag()) && !ItemFlag.转存吊牌.check(item.getFlag())) {
                            Equip nEquip = ii.resetEquipStats((Equip) item);
                            chr.forceUpdateItem(nEquip);
                            chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                            used = true;
                        } else {
                            chr.dropMessage(5, "该道具无法使用完美还原卷。");
                            break;
                        }
                    } else {
                        chr.dropMessage(5, "请将卷轴点在你需要还原的装备上.");
                        break;
                    }
                } else if (itemId == 5064300) { //卷轴防护卷轴 - 卷轴使用失败时，可以保护卷轴不消失的魔法防护卷轴. \n使用在装备道具上时 #c添加一次保护机会#，如果卷轴使用失败时#c使用的卷轴不会消失#。但是,卷轴使用成功时也会消耗保护效果。\n可以和#c保护卷轴,防爆卷轴#一起使用。
                    Item item;
                    short dst = slea.readShort();
                    if (dst < 0) {
                        item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                    } else {
                        item = null;
                    }
                    if (item != null && item.getType() == 1) {
                        if (!ItemFlag.卷轴防护.check(item.getFlag())) {
                            item.addFlag((short) ItemFlag.卷轴防护.getValue());
                            chr.forceUpdateItem(item);
                            chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                            used = true;
                        } else {
                            chr.dropMessage(5, "已经获得了相同效果。");
                            break;
                        }
                    } else {
                        chr.dropMessage(5, "请将卷轴点在你需要保护的装备上.");
                        break;
                    }
                } else if (itemId == 5060003) { //花生机
                    Item item = chr.getInventory(MapleInventoryType.ETC).findById(4170023);
                    if (item == null || item.getQuantity() <= 0) {
                        return;
                    }
                    if (getIncubatedItems(c, itemId, slot)) {
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, item.getPosition(), (short) 1, false);
                        used = true;
                        break;
                    }
                } else if (itemId >= 5062000 && itemId < 5062200 || itemId == 5062502 || itemId == 5062503) { //神奇魔方
                    Equip equip = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                    int lockslot = 0;
                    short lockid = 0;
                    if (itemId == 5062000) {
                        lockslot = slea.readInt();
                        lockid = slea.readShort();
                    }

                    if (equip != null) {
                        used = equip.resetPotential(itemId, chr, lockslot, lockid);
                        if (used) {
                            chr.equipChanged();
                        }
                    }
                    if (equip == null || !used) {
                        chr.getMap().broadcastMessage(InventoryPacket.showPotentialReset(false, chr.getId(), false, itemId));
                    }
                    break;
                } else if (itemId == 5062400 || itemId == 5062402) {
                    //5062400 - 神奇铁砧 - 可合成#c同一种类的游戏装备#，并#c改变装备外观#的道具、装备属性由功能道具决定，可以用相同外观的道具进行合成。合成后，#c外观道具会消失#。\n注意：可使用的装备: 帽子, 眼饰, 脸饰, 上衣, 裤裙, 全身甲, 鞋子, 手套, 披风, 武器, 盾牌以及刀等总共12种。
                    //5062402 - 勋章神秘铁砧 - 可合成#c勋章道具#，并#c改变外观#的铁砧。道具效果将依照功能道具，拥有同样外观的道具也可以合成，请注意：合成后，#c外观道具会消失#。\n可使用的装备：勋章
                    byte skinSlot = (byte) slea.readInt();
                    byte itemSlot = (byte) slea.readInt();
                    Equip toItem = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(itemSlot);
                    Equip skin = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(skinSlot);
                    if (toItem != null && skin != null && chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1) {
                        MapleInventoryManipulator.addById(c, 2028093, (short) 1, "使用神奇铁砧 时间: " + DateUtil.getCurrentDate());
                        MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIP, skinSlot, (short) 1, false, true);
                        toItem.setItemSkin(skin.getItemSkin() > 0 ? skin.getItemSkin() : skin.getItemId()); //如果外形道具有其他的外形 就用其他的外形 如果没有就用外形道具
                        chr.forceUpdateItem(toItem);
                        c.announce(InventoryPacket.showSynthesizingMsg(itemId, 2028093, true));
                        chr.finishAchievement(57); //首次用神奇铁砧完成了形象合成
                        used = true;
                        break;
                    } else {
                        c.announce(InventoryPacket.showSynthesizingMsg(itemId, 2028093, false));
                        break;
                    }
                } else if (itemId == 5062500 || itemId == 5062501) { //5062500 - 潜能变化魔方 - 可对附加潜能进行修改的神秘魔方。对原有的潜能不起影响。\n#c附加潜能仅限对B级以上，SS级以下的道具使用#\n#c创造物最高等级: SS级#
                    Item item = chr.getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                    if (item != null) {
                        Equip eq = (Equip) item;
                        used = eq.resetPotential(itemId, chr, 0, (short) 0);
                        if (used) {
                            chr.equipChanged();
                        }
                    }
                    if (item == null || !used) {
                        chr.getMap().broadcastMessage(InventoryPacket.潜能变化效果(chr.getId(), false, itemId));
                    }
                    break;
                } else if (itemId == 5068100) { //5068100 - 宠物专用保护卷轴 - 可保护道具的魔法盾。对 #c宠物装备#使用后可在使用卷轴失败时不减少装备道具的#c强化次数#,#c只限1次#。 但是使用卷轴成功时，防御效果也会消失。\n可以和#c安全之盾、复原之盾#一起使用。
                    short dst = slea.readShort();
                    if (dst == 0) {
                        chr.dropMessage(5, "请将卷轴点在你需要保护的宠物装备上");
                    } else if (dst == -114 || dst == -122 || dst == -124) {
                        Item item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                        if (item != null) {
                            short flag = item.getFlag();
                            if (!ItemFlag.保护升级次数.check(flag)) {
                                flag |= ItemFlag.保护升级次数.getValue();
                                item.setFlag(flag);
                                //System.out.println("使用保护卷轴: newflag " + flag);
                                chr.forceUpdateItem(item);
                                chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                                used = true;
                            } else {
                                chr.dropMessage(5, "已经获得了相同效果。");
                            }
                        }
                    } else {
                        chr.dropMessage(5, "该卷轴只能用于宠物装备上。");
                    }
                } else if (itemId == 5068200) { //5068200 - 宠物专用卷轴防护卷轴 - 卷轴使用失败时，可以保护卷轴不消失的魔法防护卷轴. \n使用在#c宠物装备道具#上时 #c添加一次保护机会#，如果卷轴使用失败时#c使用的卷轴不会消失#。但是,卷轴使用成功时也会消耗保护效果。\n可以和#c保护卷轴,防爆卷轴#一起使用。
                    short dst = slea.readShort();
                    if (dst == 0) {
                        chr.dropMessage(5, "请将卷轴点在你需要保护的宠物装备上");
                    } else if (dst == -114 || dst == -122 || dst == -124) {
                        Item item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
                        if (item != null) {
                            short flag = item.getFlag();
                            //System.out.println("使用防护卷轴: oldflag " + flag + " check " + ItemFlag.防护卷轴.check(flag));
                            if (!ItemFlag.卷轴防护.check(flag)) {
                                flag |= ItemFlag.卷轴防护.getValue();
                                item.setFlag(flag);
                                //System.out.println("使用防护卷轴: newflag " + flag);
                                chr.forceUpdateItem(item);
                                chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), itemId, item.getItemId()), true);
                                used = true;
                            } else {
                                chr.dropMessage(5, "已经获得了相同效果。");
                            }
                        }
                    } else {
                        chr.dropMessage(5, "该卷轴只能用于宠物装备上。");
                    }
                } else if (itemId == 5060002) { //孵化器 - 为孵化 #c飞天猪的蛋# 的必要装备.双击孵化器可孵化蛋.
                    byte inventory2 = (byte) slea.readInt();
                    byte slot2 = (byte) slea.readInt();
                    Item item2 = chr.getInventory(MapleInventoryType.getByType(inventory2)).getItem(slot2);
                    if (item2 == null) {
                        return;
                    }
                    chr.dropMessage(1, "暂时无法使用这个道具.");
                } else if (itemId == 5062800 || itemId == 5062801) {
                    InnerSkillEntry[] innerskill = chr.getInnerSkills();
                    int rank = -1;
                    for (InnerSkillEntry inner : innerskill) {
                        int temp = inner.getRank();
                        if (temp > rank) {
                            rank = temp;
                        }
                    }
                    int lines = chr.getLevel() >= 70 ? 3 : chr.getLevel() >= 50 ? 2 : chr.getLevel() >= 30 ? 1 : 0;
                    if (lines < chr.getInnerSkillSize()) {
                        lines = chr.getInnerSkillSize() > 3 ? 3 : chr.getInnerSkillSize();
                    }
                    for (int i = 0; i < lines; i++) {
                        boolean rewarded = false;
                        int position = i + 1;
                        while (!rewarded) {
                            InnerSkillEntry newskill = InnerAbillity.getInstance().renewSkill(rank, position, true);
                            if (newskill != null) {
                                chr.changeInnerSkill(newskill);
                                rewarded = true;
                            }
                        }
                    }
                    chr.equipChanged();
                    chr.dropMessage(1, "能力重新设置成功");
                    used = true;
                } else if (itemId == 5060052) {
                    int chairid;
                    do {
                        chairid = Randomizer.nextInt(5000) + 3010000;
                    } while (ii.getName(chairid) == null);

                    if (chr.getSpace(3) >= 1) {
                        used = true;
                        MapleInventoryManipulator.addById(c, chairid, (short) 1, "通过<椅子工具箱> ItemID: " + itemId + " 获得时间：" + DateUtil.getNowTime());
                        chr.dropMessage(1, "恭喜你，\n\r你获得了一个<" + ii.getName(chairid) + ">.");
                    } else {
                        c.getPlayer().dropMessage(5, "背包空间不足。");
                    }
                    break;
                } else if (itemId == 5060049) {
                    final List<Integer> list = ii.getDamageSkinBox().keySet().parallelStream().collect(Collectors.toList());
                    final int intValue = list.get(Randomizer.nextInt(list.size()));
                    if (chr.getSpace(2) >= 1) {
                        used = true;
                        MapleInventoryManipulator.addById(c, intValue, (short) 1, "通过<伤害皮肤卡牌包> ItemID: " + itemId + " 获得时间：" + DateUtil.getNowTime());
                        chr.dropMessage(1, "恭喜你，\n\r你获得了一个<" + ii.getName(intValue) + ">.");
                    } else {
                        c.getPlayer().dropMessage(5, "背包空间不足。");
                    }
                    break;
                    //炮竹声声
//                } else if (itemId == 5065000 || itemId == 5065100) {
//                    MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItem()).applyTo(chr);
//                    used = true;
                } else {
                    chr.dropMessage(1, "暂时无法使用这个道具.");
                }
                break;
            }
            case 507: {
                if (chr.isAdmin()) {
                    chr.dropMessage(5, "使用商场喇叭 道具类型: " + itemId / 1000 % 10);
                }
                if (chr.getLevel() < 10) {
                    chr.dropMessage(5, "需要等级10级才能使用这个道具.");
                    break;
                }
                if (chr.getMapId() == GameConstants.JAIL) {
                    chr.dropMessage(5, "在这个地方无法使用这个道具.");
                    break;
                }
                if (!chr.getCheatTracker().canSmega() && !chr.isGM()) {
                    chr.dropMessage(5, "你需要等待3秒之后才能使用这个道具.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    int msgType = itemId / 1000 % 10;
                    used = true;
                    switch (msgType) {
                        case 0: //蓝喇叭
                            chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(0x02, chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString()));
                            break;
                        case 1: //红喇叭
                            c.getChannelServer().broadcastSmegaPacket(MaplePacketCreator.serverNotice(0x02, chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString()));
                            break;
                        case 2: //高质量喇叭
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.serverNotice(0x03, c.getChannel(), chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString(), slea.readByte() != 0));
                            break;
                        case 3: //心脏高级喇叭
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.serverNotice(0x1A, c.getChannel(), chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString(), slea.readByte() != 0));
                            break;
                        case 4: //白骨高级喇叭
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.serverNotice(0x1B, c.getChannel(), chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString(), slea.readByte() != 0));
                            break;
                        case 5: //冒险岛TV
                            int tvType = itemId % 10;
                            boolean megassenger = false;
                            boolean tvEar = false;
                            MapleCharacter victim = null;
                            if (tvType != 1) {
                                if (tvType >= 3) {
                                    megassenger = true;
                                    if (tvType == 3) {
                                        slea.readByte();
                                    }
                                    tvEar = 1 == slea.readByte();
                                } else if (tvType != 2) {
                                    slea.readByte();
                                }
                                if (tvType != 4) {
                                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(slea.readMapleAsciiString());
                                }
                            }
                            List<String> tvMessages = new LinkedList<>();
                            StringBuilder builder = new StringBuilder();
                            String message = slea.readMapleAsciiString();
                            if (megassenger) {
                                builder.append(" ").append(message);
                            }
                            tvMessages.add(message);
                            //System.out.println("Maple TV: " + tvType + " 说话内容 " + builder.toString());
                            if (!MapleTVEffect.isActive()) {
                                if (megassenger) {
                                    String text = builder.toString();
                                    if (text.length() <= 60) {
                                        WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.serverNotice(0x03, c.getChannel(), chr.getName() + " : " + builder.toString(), tvEar));
                                    }
                                }
                                MapleTVEffect mapleTVEffect = new MapleTVEffect(chr, victim, tvMessages, tvType);
                                mapleTVEffect.stratMapleTV();
                            } else {
                                chr.dropMessage(1, "冒险岛TV正在使用中");
                                used = false;
                            }
                            break;
                        case 6: //道具喇叭
                            String djmsg = chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString();
                            boolean sjEar = slea.readByte() > 0;
                            Item item = null;
                            if (slea.readByte() == 1) { //item
                                byte invType = (byte) slea.readInt();
                                byte pos = (byte) slea.readInt();
                                item = chr.getInventory(MapleInventoryType.getByType(invType)).getItem(pos);
                            }
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(djmsg, sjEar, c.getChannel(), item));
                            break;
                        case 7: //缤纷喇叭
                            byte numLines = slea.readByte();
                            if (numLines < 1 || numLines > 3) {
                                return;
                            }
                            List<String> bfMessages = new LinkedList<>();
                            String bfMsg;
                            for (int i = 0; i < numLines; i++) {
                                bfMsg = slea.readMapleAsciiString();
                                if (bfMsg.length() > 65) {
                                    break;
                                }
                                bfMessages.add(chr.getName() + " : " + bfMsg);
                            }
                            boolean bfEar = slea.readByte() > 0;
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.tripleSmega(bfMessages, bfEar, c.getChannel()));
                            break;
                        case 9: //5079001 - 蛋糕高级喇叭  5079002 - 馅饼高级喇叭
                            WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.serverNotice(itemId == 5079001 ? 0x18 : 0x19, c.getChannel(), chr.getMedalText() + chr.getName() + " : " + slea.readMapleAsciiString(), slea.readByte() != 0));
                            break;
                    }
                } else {
                    chr.dropMessage(5, "当前频道禁止使用道具喇叭.");
                }
                break;
            }
            case 508: { //消息 可以点开那种 悬浮在空中
                MapleLove love = new MapleLove(chr, chr.getPosition(), chr.getMap().getFootholds().findBelow(chr.getPosition(), true).getId(), slea.readMapleAsciiString(), itemId);
                chr.getMap().spawnLove(love);
                used = true;
                break;
            }
            case 509: { //请柬和消息
                String sendTo = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                chr.sendNote(sendTo, msg);
                used = true;
                break;
            }
            case 510: { //音乐盒
                //chr.getMap().broadcastMessage(MaplePacketCreator.musicChange("Jukebox/Congratulation"));
                chr.getMap().startJukebox(chr.getName(), itemId);
                used = true;
                break;
            }
            case 512: { //在角色所在的地图上下东西30秒.可以输入想说的内容
                String msg = ii.getMsg(itemId);
                String ourMsg = slea.readMapleAsciiString();
                if (!msg.contains("%s")) {
                    msg = ourMsg;
                } else {
                    msg = msg.replaceFirst("%s", chr.getName());
                    if (!msg.contains("%s")) {
                        msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                    } else {
                        try {
                            msg = msg.replaceFirst("%s", ourMsg);
                        } catch (Exception e) {
                            msg = ii.getMsg(itemId).replaceFirst("%s", ourMsg);
                        }
                    }
                }
                chr.getMap().startMapEffect(msg, itemId);
                int buff = ii.getStateChangeItem(itemId);
                if (buff != 0) {
                    for (MapleCharacter mChar : chr.getMap().getCharactersThreadsafe()) {
                        ii.getItemEffect(buff).applyTo(mChar);
                    }
                }
                used = true;
                break;
            }
            case 515: {
                /*
                 * 5152049 - 一次性隐形眼镜（银色） - 使用一次可以让眼睛变成#c银色#
                 * 5152100 - 一次性隐形眼镜（黑色） - 使用一次可以让眼睛变成#c黑色#
                 * 5152101 - 一次性隐形眼镜（蓝色） - 使用一次可以让眼睛变成#c蓝色#
                 * 5152102 - 一次性隐形眼镜（红色） - 使用一次可以让眼睛变成#c红色#
                 * 5152103 - 一次性隐形眼镜（绿色） - 使用一次可以让眼睛变成#c 绿色#
                 * 5152104 - 一次性隐形眼镜（棕色） - 使用一次可以让眼睛变成#c棕色#
                 * 5152105 - 一次性隐形眼镜（祖母绿色） - 使用一次可以让眼睛变成#c祖母绿色#
                 * 5152106 - 一次性隐形眼镜（紫色） - 使用一次可以让眼睛变成#c紫色#
                 * 5152107 - 一次性隐形眼镜（紫水晶色） - 使用一次可以让眼睛变成#c紫水晶色#
                 */
                if (itemId >= 5152100 && itemId <= 5152107) { //增加宠物的一些功能
                    int color = (itemId - 5152100) * 100;
                    //System.out.println("使用一次性隐形眼镜 - 道具: " + itemId + " 颜色: " + color);
                    if (color >= 0 && c.getPlayer().changeFace(color)) {
                        used = true;
                    } else {
                        chr.dropMessage(1, "使用一次性隐形眼镜出现错误.");
                    }
                } else if (itemId == 5155000) { //卡勒塔的许愿珍珠 - 卡勒塔炼制的愿望珍珠。\n可以让尖耳朵#c精灵#变成#c人类面孔#，也可以让想体验精灵的#c人类#拥有#c精灵耳朵#。\n想变化新样子时需要对#c新珍珠#重新许愿。\n注：精灵耳朵很帅气，但#c有时过于突出#，需谨慎。
                    chr.changeElfEar();
                    used = true;
                } else if (itemId == 5156000) { //5156000 - 伟大的变性秘药 - 使用后角色性别将改变。使用后将变成基础脸型和基础发型。#c但，已婚角色或只有一个性别的职业无法使用。#
                    if (chr.getMarriageId() > 0 || chr.getMarriageRing() != null) {
                        chr.dropSpouseMessage(0x0B, "已婚人士无法使用。");
                    } else if (JobConstants.is米哈尔(chr.getJob()) || JobConstants.is爆莉萌天使(chr.getJob())) {
                        chr.dropSpouseMessage(0x0B, "该职业群无法使用的物品。");
                    } else {
                        chr.setGender(chr.getGender() == 0 ? (byte) 0x01 : 0x00);
                        Pair<Integer, Integer> ret = JobConstants.getDefaultFaceAndHair(chr.getJob(), chr.getGender());
                        List<Pair<MapleStat, Long>> statupdate = new ArrayList<>(3);
                        chr.setFace(ret.getLeft());
                        chr.setHair(ret.getRight());
                        statupdate.add(new Pair<>(MapleStat.脸型, (long) chr.getFace()));
                        statupdate.add(new Pair<>(MapleStat.发型, (long) chr.getHair()));
                        statupdate.add(new Pair<>(MapleStat.性别, (long) chr.getGender()));
                        c.announce(MaplePacketCreator.updatePlayerStats(statupdate, chr));
                        c.announce(EffectPacket.showOwnCraftingEffect("Effect/BasicEff.img/TransGender", 0, 0));
                        chr.getMap().broadcastMessage(chr, EffectPacket.showCraftingEffect(chr.getId(), "Effect/BasicEff.img/TransGender", 0, 0), false);
                        c.announce(MaplePacketCreator.spawnPlayerMapobject(chr));
                        chr.equipChanged();
                        used = true;
                    }
                } else {
                    chr.dropMessage(1, "暂不支持这个道具的使用.");
                }
                break;
            }
            case 517: { //宠物取名
                int uniqueid = (int) slea.readLong();
                MaplePet pet = null;
                for (MaplePet petx : chr.getPets()) {
                    if (petx != null && petx.getUniqueId() == uniqueid) {
                        pet = petx;
                        break;
                    }
                }
                if (pet == null) {
                    chr.dropMessage(1, "宠物改名错误，找不到宠物的信息.");
                    break;
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (pet.getName().contains(z) || nName.contains(z)) {
                        break;
                    }
                }
                if (MapleCharacterUtil.canChangePetName(nName)) {
                    pet.setName(nName);
                    pet.saveToDb();
                    chr.petUpdateStats(pet, true);
                    c.announce(MaplePacketCreator.enableActions());
                    chr.getMap().broadcastMessage(MTSCSPacket.INSTANCE.changePetName(chr, nName, pet.getInventoryPosition()));
                    used = true;
                }
                break;
            }
            case 519: { //宠物取消增加功能
                if (itemId >= 5190000 && itemId <= 5190011) { //增加宠物的一些功能
                    int uniqueid = (int) slea.readLong();
                    MaplePet pet = null;
                    for (MaplePet petx : chr.getPets()) {
                        if (petx != null && petx.getUniqueId() == uniqueid) {
                            pet = petx;
                            break;
                        }
                    }
                    if (pet == null) {
                        chr.dropMessage(1, "宠物改名错误，找不到宠物的信息.");
                        break;
                    }
                    PetFlag petFlag = PetFlag.getByAddId(itemId);
                    if (petFlag != null && !petFlag.check(pet.getFlags())) {
                        pet.setFlags(pet.getFlags() | petFlag.getValue());
                        pet.saveToDb();
                        chr.petUpdateStats(pet, true);
                        c.announce(MaplePacketCreator.enableActions());
                        c.announce(MTSCSPacket.INSTANCE.changePetFlag(uniqueid, true, petFlag.getValue()));
                        used = true;
                    }
                    break;
                } else if (itemId >= 5191000 && itemId <= 5191004) { //取消宠物的一些功能
                    int uniqueid = (int) slea.readLong();
                    MaplePet pet = null;
                    for (MaplePet petx : chr.getPets()) {
                        if (petx != null && petx.getUniqueId() == uniqueid) {
                            pet = petx;
                            break;
                        }
                    }
                    if (pet == null) {
                        chr.dropMessage(1, "宠物改名错误，找不到宠物的信息.");
                        break;
                    }
                    PetFlag petFlag = PetFlag.getByDelId(itemId);
                    if (petFlag != null && petFlag.check(pet.getFlags())) {
                        pet.setFlags(pet.getFlags() - petFlag.getValue());
                        pet.saveToDb();
                        chr.petUpdateStats(pet, true);
                        c.announce(MaplePacketCreator.enableActions());
                        c.announce(MTSCSPacket.INSTANCE.changePetFlag(uniqueid, false, petFlag.getValue()));
                        used = true;
                    }
                } else if (itemId == 5192000) {
                    MaplePet pet = null;
                    int petuid = (int) slea.readLong();
                    for (MaplePet p : chr.getPets()) {
                        if (p != null && p.getUniqueId() == petuid) {
                            pet = p;
                            break;
                        }
                    }
                    if (pet == null) {
                        chr.dropMessage(1, "宠物训练技能失败,找不到对应宠物信息。");
                        break;
                    }
                    pet.setAddSkill(Randomizer.nextInt(6) + 1);
                    pet.saveToDb();
                    c.announce(InventoryPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem(pet.getInventoryPosition()), pet.getSummoned()));
                    c.announce(InventoryPacket.petAddSkill(chr, pet));
                    c.sendEnableActions();
                    used = true;
                    break;
                }
                break;
            }
            case 520: { //金币包和豆豆箱子
                if (itemId >= 5200000 && itemId <= 5200008) {
                    if (chr.isIntern()) {
                        int mesars = ii.getMeso(itemId);
                        if (mesars > 0 && chr.getMeso() < (Integer.MAX_VALUE - mesars)) {
                            used = true;
                            if (Math.random() > 0.1) {
                                int gainmes = Randomizer.nextInt(mesars);
                                chr.gainMeso(gainmes, false);
                                c.announce(MTSCSPacket.INSTANCE.sendMesobagSuccess(gainmes));
                            } else {
                                c.announce(MTSCSPacket.INSTANCE.sendMesobagFailed());
                            }
                        } else {
                            chr.dropMessage(1, "金币已达到上限无法使用这个道具.");
                        }
                    } else {
                        AutobanManager.getInstance().autoban(chr.getClient(), "使用非法道具.");
                    }
                    break;
                } else {
                    chr.dropMessage(5, "暂时无法使用这个道具.");
                }
                break;
            }
            case 523: { //商店搜索器
                int itemSearch = slea.readInt();
                List<HiredMerchant> hms = c.getChannelServer().searchMerchant(itemSearch);
                if (hms.size() > 0) {
                    c.announce(MaplePacketCreator.getOwlSearched(itemSearch, hms));
                    used = true;
                } else {
                    chr.dropMessage(1, "没有找到这个道具.");
                }
                MapleCharacterUtil.addToItemSearch(itemSearch);
                break;
            }
            case 524: { //宠物食品
                MaplePet pet = null;
                MaplePet[] pets = chr.getSpawnPets();
                for (int i = 0; i < 3; i++) {
                    if (pets[i] != null && (pets[i].canConsume(itemId) || itemId == 5249000)) {
                        pet = pets[i];
                        break;
                    }
                }
                if (pet == null) {
                    chr.dropMessage(1, "没有可以喂食的宠物。\r\n请重新确认。");
                    break;
                }
                byte petIndex = chr.getPetIndex(pet);
                pet.setFullness(100);
                if (pet.getCloseness() < 30000) {
                    pet.setCloseness(Math.min(itemId == 5249000 ? pet.getCloseness() + 100 : pet.getCloseness() + (100 * ServerConfig.CHANNEL_RATE_TRAIT), 30000));
                    while (pet.getCloseness() >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                        pet.setLevel(pet.getLevel() + 1);
                        c.announce(EffectPacket.showOwnPetLevelUp(chr.getPetIndex(pet)));
                        chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, petIndex));
                    }
                }
                chr.petUpdateStats(pet, true);
                chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, petIndex, true, true), true);
                used = true;
                break;
            }
            case 528: { //传说中的臭屁
                Rectangle bounds = new Rectangle((int) chr.getPosition().getX(), (int) chr.getPosition().getY(), 1, 1);
                MapleMist mist = new MapleMist(bounds, chr);
                chr.getMap().spawnMist(mist, 10000, true);
                c.announce(MaplePacketCreator.enableActions());
                used = true;
                break;
            }
            case 532: {
                String name = slea.readMapleAsciiString();
                String otherName = slea.readMapleAsciiString();
                slea.readInt();
                slea.readInt();
                int cardId = slea.readByte();
                PredictCardFactory pcf = PredictCardFactory.getInstance();
                PredictCard Card = pcf.getPredictCard(cardId);
                int commentId = Randomizer.nextInt(pcf.getCardCommentSize());
                PredictCardComment Comment = pcf.getPredictCardComment(commentId);
                if (Card != null && Comment != null) {
                    chr.dropMessage(5, "占卜只是随便写的，占卜结果就当个玩笑看看。");
                    int love = Randomizer.rand(1, Comment.score) + 5;
                    c.announce(MaplePacketCreator.showPredictCard(name, otherName, love, cardId, commentId));
                    used = true;
                }
                break;
            }
            case 537: { //黑板
                chr.setChalkboard(slea.readMapleAsciiString());
                break;
            }
            case 539: { //情景喇叭
                if (chr.getLevel() < 10) {
                    chr.dropMessage(5, "需要等级10级才能使用这个道具.");
                    break;
                }
                if (chr.getMapId() == GameConstants.JAIL) {
                    chr.dropMessage(5, "当前地图无法使用这个道具.");
                    break;
                }
                if (!chr.getCheatTracker().canAvatarSmega()) {
                    chr.dropMessage(5, "你需要等待6秒之后才能使用这个道具.");
                    break;
                }
                if (!c.getChannelServer().getMegaphoneMuteState()) {
                    List<String> messages = new LinkedList<>();
                    for (int i = 0; i < 4; i++) {
                        messages.add(slea.readMapleAsciiString());
                    }
                    boolean ear = slea.readByte() != 0;
                    WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.getAvatarMega(chr, c.getChannel(), itemId, messages, ear));
                    used = true;
                } else {
                    chr.dropMessage(5, "当前频道禁止使用情景喇叭.");
                }
                break;
            }
            case 545: {

                if (ServerConstants.isBlockedMapFM(chr.getMapId())) {
                    chr.dropMessage(5, "当前地图无法使用此道具.");
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                if (chr.getLevel() < 10) {
                    chr.dropMessage(5, "只有等级达到10级才可以使用此道具.");
                } else if (chr.hasBlockedInventory() || chr.getMap().getSquadByMap() != null || chr.getEventInstance() != null || chr.getMap().getEMByMap() != null || chr.getMapId() >= 990000000) {
                    chr.dropMessage(5, "当前地图无法使用此道具.");
                } else if ((chr.getMapId() >= 680000210 && chr.getMapId() <= 680000502) || (chr.getMapId() / 1000 == 980000 && chr.getMapId() != 980000000) || (chr.getMapId() / 100 == 1030008) || (chr.getMapId() / 100 == 922010) || (chr.getMapId() / 10 == 13003000)) {
                    chr.dropMessage(5, "当前地图无法使用此道具.");
                } else /*
                     * 编号:　5450000 名称:　包裹商人妙妙
                     * 编号:　5450001 名称:　金先生直通电话
                     * 编号:　5450003 名称:　新手包裹商人妙妙
                     * 编号:　5450004 名称:　包裹商人妙妙（30天）
                     * 编号:　5450005 名称:　移动仓库（30天）
                     * 编号:　5450006 名称:　[1天]包裹商人妙妙
                     * 编号:　5450007 名称:　[7天]包裹商人妙妙
                     * 编号:　5450008 名称:　[1天]移动仓库王先生
                     * 编号:　5450009 名称:　[7天]移动仓库王先生
                     * 编号:　5450010 名称:　猫咪商人奈落
                 */ if (itemId == 5451001) { //编号:　5451001  名称:　新手转蛋券
                    chr.dropMessage(1, "暂时无法使用这个道具.");
                } else if (itemId == 5450001 || itemId == 5450005 || itemId == 5450008 || itemId == 5450009) { //移动仓库
                    chr.setConversation(4);
                    chr.getStorage().sendStorage(c, itemId == 5450001 ? 1002005 : 1022005);
                } else if (itemId == 5450010) { //猫咪商人奈落 高级杂货商店
                    MapleShopFactory.getInstance().getShop(9090100).sendItemShop(c, itemId);
                    used = true;
                } else {
                    MapleShopFactory.getInstance().getShop(9090000).sendItemShop(c, itemId);
                }
                break;
            }
            case 550: {
                if (itemId == 5500003) { //佳佳变身药水
                    chr.dropMessage(1, "暂时无法使用这个道具.");
                    break;
                } else if (itemId == 5501001 || itemId == 5501002) { //魔法丝线
                    Skill skil = SkillFactory.getSkill(slea.readInt());
                    if (skil == null || skil.getId() / 10000 != 8000 || chr.getSkillLevel(skil) <= 0 || !skil.isTimeLimited() || GameConstants.getMountItem(skil.getId(), chr) <= 0) {
                        break;
                    }
                    long toAdd = (itemId == 5501001 ? 30 : 60) * 24 * 60 * 60 * 1000L;
                    long expire = chr.getSkillExpiry(skil);
                    if (expire < System.currentTimeMillis() || expire + toAdd >= System.currentTimeMillis() + (365 * 24 * 60 * 60 * 1000L)) {
                        break;
                    }
                    chr.changeSingleSkillLevel(skil, chr.getSkillLevel(skil), chr.getMasterLevel(skil), expire + toAdd);
                    used = true;
                    break;
                } else if (itemId >= 5500000 && itemId <= 5500006) { //魔法沙漏
                    Short slots = slea.readShort();
                    if (slots == 0) {
                        chr.dropMessage(1, "请该道具点在你需要延长时间的道具上.");
                        break;
                    }
                    Item item = chr.getInventory(MapleInventoryType.EQUIPPED).getItem(slots);
                    long days = 0;
                    if (itemId == 5500000) { //魔法沙漏
                        days = 1;
                    } else if (itemId == 5500001) { //魔法沙漏（7天）
                        days = 7;
                    } else if (itemId == 5500002) { //魔法沙漏（20天）
                        days = 20;
                    } else if (itemId == 5500004) { //魔法沙漏（30天）
                        days = 30;
                    } else if (itemId == 5500005) { //魔法沙漏（50天）
                        days = 50;
                    } else if (itemId == 5500006) { //魔法沙漏（99天）
                        days = 99;
                    }
                    if (item != null && !ItemConstants.isAccessory(item.getItemId()) && item.getExpiration() > -1 && !ii.isCash(item.getItemId()) && System.currentTimeMillis() + (100 * 24 * 60 * 60 * 1000L) > item.getExpiration() + (days * 24 * 60 * 60 * 1000L)) {
                        boolean change = true;
                        for (String z : GameConstants.RESERVED) {
                            if (chr.getName().contains(z) || item.getOwner().contains(z)) {
                                change = false;
                            }
                        }
                        if (change && days > 0) {
                            item.setExpiration(item.getExpiration() + (days * 24 * 60 * 60 * 1000));
                            chr.forceUpdateItem(item);
                            used = true;
                            break;
                        } else {
                            chr.dropMessage(1, "无法使用在这个道具上.");
                        }
                    } else {
                        chr.dropMessage(1, "使用道具出现错误.");
                    }
                } else {
                    chr.dropMessage(1, "暂时无法使用这个道具.");
                }
                break;
            }
            case 552: {
                if (itemId == 5521000) { //转存吊牌
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = chr.getInventory(type).getItem((byte) slea.readInt());
                    if (item != null && !ItemFlag.封印.check(item.getFlag()) && !ItemFlag.转存吊牌.check(item.getFlag()) && !ItemFlag.宿命剪刀.check(item.getFlag())) {
                        if (ii.isShareTagEnabled(item.getItemId())) {
                            short flag = item.getFlag();
                            flag |= ItemFlag.不可交易.getValue();
                            flag |= ItemFlag.可以交换1次.getValue();

                            if (type == MapleInventoryType.EQUIP) {
                                flag |= ItemFlag.转存吊牌.getValue();
                            }

                            item.setFlag(flag);
                            chr.forceUpdateItem(item);
                            used = true;
                            break;
                        }
                    }
                } else if (itemId == 5520000 || itemId == 5520001) { //宿命剪刀
                    MapleInventoryType type = MapleInventoryType.getByType((byte) slea.readInt());
                    Item item = chr.getInventory(type).getItem((byte) slea.readInt());
                    if (item != null && !ItemFlag.可以交换1次.check(item.getFlag()) && !ItemFlag.宿命剪刀.check(item.getFlag())) {
                        if ((itemId == 5520000 && ii.isTradeAvailable(item.getItemId())) || (itemId == 5520001 && ii.isPKarmaEnabled(item.getItemId())) || ((Equip) item).getKarmaCount() > 0 && ((Equip) item).getSealedExp() > 0) {
                            short flag = item.getFlag();
                            flag |= ItemFlag.不可交易.getValue();
                            flag |= ItemFlag.可以交换1次.getValue();

                            if (type == MapleInventoryType.EQUIP) {
                                flag |= ItemFlag.宿命剪刀.getValue();
                            }
                            item.setFlag(flag);
                            chr.forceUpdateItem(item);
                            used = true;
                            break;
                        }
                    }
                }
                break;
            }
            case 553: {
                if (CashItemFactory.getInstance().hasRandomItem(itemId)) {
                    InventoryHandler.UseRewardBox(slot, itemId, c, chr);
                } else if (itemId == 5537000) { // 怪怪卡牌包
                    if (chr.getSpace(2) < 3) {
//                        chr.send(FamiliarPacket.errorMessage(chr.getId(), (byte) 0));
                        chr.dropMessage(1, "请先空出消耗栏3格，再试一次");
                        break;
                    }
                    InventoryHandler.UseFamiliarCard(chr);
                    used = true;
                } else {
                    if (!InventoryHandler.UseRewardItem(slot, itemId, c, chr)) {
                        chr.dropMessage(1, "该道具无法使用.道具ID: " + itemId);
                    }
                }
                break;
            }
            case 555: {
                if (itemId == 5551000) {
                    final int damskinslot = Integer.valueOf(chr.getKeyValue("DAMAGE_SKIN_SLOT"));
                    if (damskinslot < 10) {
                        chr.setKeyValue("DAMAGE_SKIN_SLOT", String.valueOf(damskinslot + 1));
                        chr.send(MaplePacketCreator.updateDamageSkin(chr, true));
                        chr.dropMessage(1, "伤害皮肤栏位扩充成功，当前栏位：" + (damskinslot + 1) + " 格");
                        used = true;
                    } else {
                        chr.dropMessage(1, "栏位已满无法扩充。");
                    }
                } else {
                    chr.dropMessage(1, "该道具无法使用.道具ID: " + itemId);
                }
                break;
            }
            case 557: { //金锤子
                slea.readInt(); // Inventory type, Hammered eq is always EQ.
                Equip item = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                if (item != null) {
                    if (ItemConstants.canHammer(item.getItemId()) && ii.getSlots(item.getItemId()) > 0 && item.getViciousHammer() < 2) {
                        item.setViciousHammer((byte) (item.getViciousHammer() + 1));
                        item.setUpgradeSlots((byte) (item.getUpgradeSlots() + 1));
                        chr.forceUpdateItem(item);
                        c.announce(MTSCSPacket.INSTANCE.sendCashHammerResult(true, true));
                        used = true;
                    } else {
                        chr.dropMessage(1, "无法使用在这个道具上.");
                        c.announce(MTSCSPacket.INSTANCE.sendCashHammerResult(true, false));
                    }
                }
                break;
            }
            case 562: { //技能书
                if (InventoryHandler.UseSkillBook(slot, itemId, c, chr)) {
                    chr.gainSP(1);
                }
                break;
            }
            case 570: { //智能机器人取名
                slea.skip(8);
                if (chr.getAndroid() == null) {
                    break;
                }
                String nName = slea.readMapleAsciiString();
                for (String z : GameConstants.RESERVED) {
                    if (chr.getAndroid().getName().contains(z) || nName.contains(z)) {
                        break;
                    }
                }
                if (MapleCharacterUtil.canChangePetName(nName)) {
                    chr.getAndroid().setName(nName);
                    chr.getAndroid().saveToDb(); //保存下安卓的数据
                    chr.setAndroid(chr.getAndroid()); //重新召唤安卓
                    used = true;
                }
                break;
            }
            case 575: {
                if (itemId == 5750000 || itemId == 5750002) { //星岩魔方 - 凝聚来自宇宙的能量，重置它所触碰到的#c星岩# 属性和潜能。
                    if (chr.getLevel() < 10) {
                        chr.dropMessage(1, "使用这个道具需要等级达到10级.");
                        break;
                    } else {
                        Item item = chr.getInventory(MapleInventoryType.SETUP).getItem((byte) slea.readInt());
                        if (item != null && chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 1 && chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 1) {
                            int grade = ItemConstants.getNebuliteGrade(item.getItemId());
                            if (grade != -1 && grade < 5) {
                                int prop = 0;
                                switch (grade) {
                                    case 0:
                                        prop = Integer.valueOf(NebuliteConfig.benuliteC);
                                        break;
                                    case 1:
                                        prop = Integer.valueOf(NebuliteConfig.benuliteB);
                                        break;
                                    case 2:
                                        prop = Integer.valueOf(NebuliteConfig.benuliteA);
                                        break;
                                    case 3:
                                        prop = Integer.valueOf(NebuliteConfig.benuliteS);
                                        break;
                                }
//                                int rank = Randomizer.nextInt(100) < 7 ? (Randomizer.nextInt(100) < 2 ? (grade + 1) : (grade != 4 ? (grade + 1) : grade)) : grade;
                                int rank = Randomizer.nextInt(100) < prop ? grade + 1 : grade;
                                List<StructItemOption> pots = new LinkedList<>(ii.getAllSocketInfo(rank).values());
                                int newId = 0;
                                while (newId == 0) {
                                    StructItemOption pot = pots.get(Randomizer.nextInt(pots.size()));
                                    if (pot != null) {
                                        newId = pot.opID;
                                    }
                                }
                                int newGrade = ItemConstants.getNebuliteGrade(newId);
                                if (newGrade != -1 && newGrade > grade && newGrade > 2) {
                                    Item nItem = new Item(newId, (byte) 0, (short) 1, (byte) 0);
                                    WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(chr.getName(), " : 使用星岩魔方升级了星岩获得{" + ii.getName(newId) + "}！大家一起恭喜他（她）吧！！！！", nItem, (byte) 3, c.getChannel()));
                                }
                                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.SETUP, item.getPosition(), (short) 1, false);
                                MapleInventoryManipulator.addById(c, newId, (short) 1, "Upgraded from alien cube on " + DateUtil.getCurrentDate());
                                MapleInventoryManipulator.addById(c, 2430760, (short) 1, "Alien Cube" + " on " + DateUtil.getCurrentDate());
                                c.announce(MaplePacketCreator.getShowItemGain(newId, (short) 1, true));
                                chr.getMap().broadcastMessage(InventoryPacket.showNebuliteEffect(chr.getId(), true, "成功交换了星岩。"));
                                c.announce(MaplePacketCreator.craftMessage("你得到了" + ii.getName(newId)));
                                used = true;
                            } else {
                                chr.dropMessage(5, "重置的道具失败.");
                                break;
                            }
                        } else {
                            chr.dropMessage(5, "您的背包空间不足.");
                            break;
                        }
                    }
                } else if (itemId == 5750001) { //星岩电钻机 - 将星岩及所带属性从装备上#c永久移除#。装备将#c失去对应属性#，被移除的星岩及其属性将#c保留#。（1次只能移除1个星岩）
                    if (chr.getLevel() < 10) {
                        chr.dropMessage(1, "使用这个道具需要等级达到10级.");
                        break;
                    } else {
                        Item item = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem((byte) slea.readInt());
                        if (item != null) {
                            Equip eq = (Equip) item;
                            int sockItem = eq.getSocket1();
                            if (sockItem > 0 && ii.itemExists(sockItem)) { // first slot only.
                                eq.setSocket1(0);
                                chr.forceUpdateItem(item);
                                MapleInventoryManipulator.addById(c, sockItem, (short) 1, "摘取星岩: " + DateUtil.getCurrentDate());
                                MapleInventoryManipulator.addById(c, 2430691, (short) 1, "Alien Cube" + " on " + DateUtil.getCurrentDate());
                                chr.getMap().broadcastMessage(InventoryPacket.showNebuliteEffect(chr.getId(), true, "成功清空了插槽。"));
                                used = true;
                            } else {
                                chr.dropMessage(5, "该道具不具有星岩属性.");
                                break;
                            }
                        } else {
                            chr.dropMessage(5, "This item's nebulite cannot be removed.");
                            break;
                        }
                    }
                }
                break;
            }
            case 577: {
                if (JobConstants.is龙的传人(chr.getJob()) && chr.getCoreAura() != null) {
                    if (itemId == 5770000) { //5770000 - 宝盒封印 - 可以延长八卦宝盒的现有能力，30天内不会自动重设。最长封印时间不能超过365天。#c使用宝盒魔方重新设定宝盒的能力，宝盒的封印时间也不会取消。#只有#c龙的传人#职业可以使用。
                        chr.getCoreAura().setExpiration(chr.getCoreAura().getExpiration() + (30 * 24 * 60 * 60 * 1000));
                        used = true;
                    } else if (itemId == 5771001) { //5771001 - 宝盒魔方 Lv.1 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c30级~69级#的角色可以使用。
                        chr.getCoreAura().randomCoreAura(1);
                        used = true;
                    } else if (itemId == 5771002) { //5771002 - 宝盒魔方 Lv.2 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c70级~119级#的角色可以使用。
                        chr.getCoreAura().randomCoreAura(2);
                        used = true;
                    } else if (itemId == 5771003) { //5771003 - 宝盒魔方 Lv.3 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c120级~159级#的角色可以使用。
                        chr.getCoreAura().randomCoreAura(3);
                        used = true;
                    } else if (itemId == 5771004) { //5771004 - 宝盒魔方 Lv.4 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c160级以上#的角色可以使用。
                        chr.getCoreAura().randomCoreAura(4);
                        used = true;
                    } else {
                        chr.dropMessage(1, "该道具暂时无法使用。");
                    }
                    if (used) {
                        chr.updataCoreAura();
                    }
                } else {
                    chr.dropMessage(1, "只有龙的传人职业可以使用。");
                }
                break;
            }
            case 579: {
                if (itemId == 5790000) {
                    int slots = c.getAccCardSlots();
                    if (c.gainAccCardSlot()) {
                        chr.dropMessage(1, "卡牌扩充成功，当前栏位: " + (slots + 1));
                        used = true;
                    } else {
                        chr.dropMessage(1, "卡牌扩充失败，栏位已超过上限。");
                    }
                } else {
                    chr.dropMessage(1, "该道具无法使用.");
                }
                break;
            }
            case 580: {
                if (JobConstants.is爆莉萌天使(chr.getJob())) {
                    int newLongcoat = 1051294 + itemId % 10;
                    used = true;
                    chr.setKeyValue("Longcoat", String.valueOf(newLongcoat));
                    chr.send(MaplePacketCreator.DressUpInfoModified(chr));
                    break;
                }
                chr.dropMessage(1, "该道具只有爆莉萌天使可以使用。");
                break;
            }
            case 584: {
                int rewardItemId = 0;
                int needItemId = 0;
                int[] needItemIds = new int[]{4310233};
                switch (itemId) {
                    case 5840007: {  // 椅子专用万花筒钥匙
                        int randItemId = Randomizer.nextInt(5000) + 3010000;
                        while (ii.getName(randItemId) == null) {
                            randItemId = Randomizer.nextInt(5000) + 3010000;
                        }
                        needItemId = needItemIds[Randomizer.nextInt(needItemIds.length)];
                        if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() >= 2 && chr.canHold(needItemId)) {
                            used = true;
                            rewardItemId = randItemId;
                            chr.removeItem(4009440, 1);
                            break;
                        }
                        needItemId = 0;
                        c.getPlayer().dropMessage(5, "背包空间不足。");
                        break;
                    }
                    case 5840006: { // 伤害皮肤专用万花筒钥匙
                        List<Integer> arrayList = new ArrayList<>();
                        arrayList.addAll(ii.getDamageSkinBox().keySet());
                        needItemId = needItemIds[Randomizer.nextInt(needItemIds.length)];
                        int randItemId = arrayList.get(Randomizer.nextInt(arrayList.size()));
                        if (chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() >= 2 && chr.canHold(needItemId)) {
                            used = true;
                            rewardItemId = randItemId;
                            chr.removeItem(4009440, 1);
                            break;
                        }
                        needItemId = 0;
                        c.getPlayer().dropMessage(5, "背包空间不足。");
                        break;
                    }
                    case 5840004: { // 怪怪卡牌包钥匙
                        if (chr.getSpace(2) >= 3) {
                            InventoryHandler.UseFamiliarCard(chr);
                            used = true;
                            break;
                        }
                        chr.dropMessage(1, "请确认消耗窗口有3个空栏!");
                    }
                }
                if (rewardItemId > 0) {
                    c.announce(MaplePacketCreator.getPeanutResult(rewardItemId, (short) 1, itemId, slot, needItemId, (short) 1));
                    MapleInventoryManipulator.addById(c, rewardItemId, (short) 1, ii.getName(itemId) + " 在 " + DateUtil.getCurrentDate());
                    if (needItemId > 0) {
                        MapleInventoryManipulator.addById(c, needItemId, (short) 1, ii.getName(itemId) + " 在 " + DateUtil.getCurrentDate());
                    } else {
                        break;
                    }
                }
                break;
            }
            default:
                log.warn("使用未处理的商城道具 : " + itemId + " 封包：" + slea.toString(true));
                break;
        }
        if (itemType == 506) {
            //c.announce(MaplePacketCreator.showScrollTip(used));
        }
        if (used) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false, true);
        }
        c.announce(MaplePacketCreator.enableActions());
        if (cc) {
            if (!chr.isAlive() || chr.getEventInstance() != null || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
                chr.dropMessage(1, "刷新人物数据失败.");
                return;
            }
            chr.dropMessage(5, "正在刷新人数据.请等待...");
            chr.fakeRelog();
            if (chr.getScrolledPosition() != 0) {
                c.announce(MaplePacketCreator.pamSongUI());
            }
        }
    }

    private static boolean getIncubatedItems(MapleClient c, int itemId, byte slot) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.USE).getNumFreeSlot() < 2 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < 2) {
            c.getPlayer().dropMessage(5, "请确保你有足够的背包空间.");
            return false;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int id1 = RandomRewards.getPeanutReward(), id2 = RandomRewards.getPeanutReward();
        while (!ii.itemExists(id1)) {
            id1 = RandomRewards.getPeanutReward();
        }
        while (!ii.itemExists(id2)) {
            id2 = RandomRewards.getPeanutReward();
        }
        c.announce(MaplePacketCreator.getPeanutResult(id1, (short) 1, itemId, slot, id2, (short) 1));
        MapleInventoryManipulator.addById(c, id1, (short) 1, ii.getName(itemId) + " 在 " + DateUtil.getCurrentDate());
        MapleInventoryManipulator.addById(c, id2, (short) 1, ii.getName(itemId) + " 在 " + DateUtil.getCurrentDate());
        return true;
    }

    public static void TapJoyResponse(final LittleEndianAccessor lea, final MapleCharacter player) {
        final int slot = lea.readInt();
        final int itemId = lea.readInt();

        if (player.getItemQuantity(itemId) > 0) {
            player.send(InventoryPacket.showTapJoyInfo(slot, itemId));
        } else {
            player.sendEnableActions();
        }
    }

    public static void TapJoyDone(final LittleEndianAccessor lea, final MapleCharacter player) {
        final int rank = lea.readInt();
        final int slot = lea.readInt();
        final int itemid = lea.readInt();
        final int hammerid = rank < 2 ? 5840000 + rank + 1 : 0;
        final List<Integer> mods = ItemConstants.TapJoyReward.getRewardList(rank);
        Collections.shuffle(mods);
        final int rewardItemId = mods.get(Randomizer.nextInt(mods.size()));
        final int intValue3 = rank < 2 ? itemid + 1 : 0;
        final Item item = player.getInventory(MapleInventoryType.ETC).getItem((short) slot);
        int mode = 3;
        int nextBoxSlot = 0;
        if (!player.haveItem(5840000 + rank, 1) || player.getInventory(MapleInventoryType.EQUIP).isFull() || player.getInventory(MapleInventoryType.USE).isFull() || player.getInventory(MapleInventoryType.SETUP).isFull() || player.getInventory(MapleInventoryType.ETC).isFull() || player.getInventory(MapleInventoryType.CASH).isFull()) {
            mode = 4;
        } else if (item != null) {
            final short amount = 1;
            player.removeItem(itemid, 1);
            player.removeItem(5840000 + rank, 1);
            final ArrayList<ModifyInventory> list = new ArrayList<>();
            if (intValue3 <= 0) {
                mode = 0;
            } else {
                mode = 1;
                nextBoxSlot = MapleInventoryManipulator.addId(player.getClient(), itemid + 1, (short) 1, "", "敲敲乐下一阶段获得！");
            }
            player.gainItem(rewardItemId, amount, "敲敲乐第" + rank + 1 + "阶段获得");
            player.send(InventoryPacket.showTapJoy(rewardItemId));
            player.send(EffectPacket.getShowItemGain(rewardItemId, amount, true));
            player.send(InventoryPacket.modifyInventory(true, list, player));
        }
        player.send(InventoryPacket.showTapJoyDone(mode, itemid, intValue3, nextBoxSlot, hammerid));
    }

    public static void TapJoyNextStage(final LittleEndianAccessor lea, final MapleCharacter player) {
        final int int1 = lea.readInt();
        final int int2 = lea.readInt();
        final int itemid = lea.readInt();
        final byte byte1 = lea.readByte();
        final int intValue = ItemConstants.TapJoyReward.getItemIdAndSN(ItemConstants.TapJoyReward.getSN(itemid) + 1).getLeft();
        int n = 8;
        final Item item = player.getInventory(MapleInventoryType.CASH).getItem((short) int2);
        final ArrayList<ModifyInventory> mods = new ArrayList<>();
        if (item != null && player.modifyCSPoints(byte1, -(int1 + 1) * 350, false)) {
            if (intValue <= 0) {
                player.getInventory(ItemConstants.getInventoryType(item.getItemId())).removeItem(item.getPosition(), (short) 1, false);
                mods.add(new ModifyInventory(1, item));
                mods.add(new ModifyInventory(3, item));
            } else {
                n = 7;
                item.setItemId(intValue);
                mods.add(new ModifyInventory(0, item));
            }
            player.send(InventoryPacket.modifyInventory(true, mods, player));
            player.send(InventoryPacket.showTapJoy(0));
        }
        player.send(InventoryPacket.showTapJoyNextStage(player, n, int2, intValue, byte1));
    }
}
