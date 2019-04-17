/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleTraitType;
import client.PlayerStats;
import client.inventory.*;
import client.skills.SkillFactory;
import configs.ServerConfig;
import configs.StarForceConfig;
import constants.GameConstants;
import constants.ItemConstants;
import handling.world.WorldBroadcastService;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.StructItemOption;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.data.input.LittleEndianAccessor;
import tools.packet.InventoryPacket;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author PlayDK
 */
public class ItemScrollHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr, boolean cash) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        chr.updateTick(slea.readInt());
        short slot = slea.readShort();
        short dst = slea.readShort();
        short ws = 0; //是否使用祝福卷轴
        if (slea.available() >= 3) {
            ws = slea.readShort();
        }
        //boolean ScrollResult = UseUpgradeScroll(slot, dst, ws, c, chr, 0, cash);
        //c.announce(MaplePacketCreator.showScrollTip(ScrollResult));
        UseUpgradeScroll(slot, dst, ws, c, chr, 0, cash);
    }

    public static boolean UseUpgradeScroll(short slot, short dst, short ws, MapleClient c, MapleCharacter chr, int vegas, boolean cash) {
        boolean whiteScroll = false; //是否使用祝福卷轴
        boolean legendarySpirit = false; //是否使用技能砸卷 V.110后修改无需技能就可以砸卷
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        chr.setScrolledPosition((short) 0);
        if ((ws & 2) == 2) {
            whiteScroll = true;
        }
        Equip toScroll;
        if (dst < 0) {
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst);
        } else {
            legendarySpirit = true;
            toScroll = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dst);
        }
        if (toScroll == null || c.getPlayer().hasBlockedInventory()) {
            return false;
        }
        byte oldLevel = toScroll.getLevel();
        byte oldEnhance = toScroll.getEnhance();
        byte oldState = toScroll.getState(false);
        byte oldAddState = toScroll.getState(true);
        short oldFlag = toScroll.getFlag();
        byte oldSlots = toScroll.getUpgradeSlots();
        int oldLimitBreak = toScroll.getLimitBreak();
        byte oldSealedLevel = toScroll.getSealedLevel();

        Item scroll;
        if (cash) {
            scroll = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        } else {
            scroll = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        }
        if (scroll == null) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 卷轴道具为空");
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if (chr.isAdmin()) {
            chr.dropSpouseMessage(0x0A, "砸卷信息: 卷轴ID " + scroll.getItemId() + " 卷轴名字 " + ii.getName(scroll.getItemId()));
        }
        if (!ItemConstants.isSpecialScroll(scroll.getItemId())
                && !ItemConstants.isCleanSlate(scroll.getItemId())
                && !ItemConstants.isEquipScroll(scroll.getItemId())
                && !ItemConstants.isPotentialScroll(scroll.getItemId())
                && !ItemConstants.isPotentialAddScroll(scroll.getItemId())
                && !ItemConstants.isLimitBreakScroll(scroll.getItemId())
                && !ItemConstants.isResetScroll(scroll.getItemId())
                && !ItemConstants.isSealedScroll(scroll.getItemId())
                && !ItemConstants.isUpgradeItemEx(scroll.getItemId())) {
            int scrollSlots = ItemConstants.isAzwanScroll(scroll.getItemId()) ? ii.getSlots(scroll.getItemId()) : 1;
            if (toScroll.getUpgradeSlots() < scrollSlots) {
                chr.dropMessage(1, "当前装备可升级次数为: " + toScroll.getUpgradeSlots() + " 成功或失败需要减少: " + scrollSlots + " 的升级次数，请检查该装备是否符合升级条件.");
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isEquipScroll(scroll.getItemId())) {
            if (toScroll.getUpgradeSlots() >= 1 || toScroll.getEnhance() >= 100 || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 强化卷轴检测 装备是否有升级次数: " + (toScroll.getUpgradeSlots() >= 1) + " 装备星级是否大于100星: " + (toScroll.getEnhance() >= 100) + " - " + (vegas > 0) + " 装备是是否为点装: " + (ii.isCash(toScroll.getItemId())));
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
            //检测直接强化到x星的卷轴 必须装备当前为0星才能砸卷
            int forceUpgrade = ii.getForceUpgrade(scroll.getItemId());
            if (forceUpgrade > 1 && toScroll.getEnhance() > 0) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 强化卷轴检测 forceUpgrade: " + forceUpgrade + " 装备星级: " + toScroll.getEnhance());
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isPotentialScroll(scroll.getItemId())) {
            boolean isSpecialScrollA = scroll.getItemId() / 100 == 20497;
            boolean isSpecialEquip = toScroll.getItemId() / 10000 == 135 || toScroll.getItemId() / 1000 == 1098 || toScroll.getItemId() / 1000 == 1099 || toScroll.getItemId() / 1000 == 1190;
            if ((!isSpecialScrollA && toScroll.getState(false) >= 1) || (isSpecialScrollA && toScroll.getState(false) >= 18) || (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && !isSpecialScrollA && !isSpecialEquip) || vegas > 0 || ii.isCash(toScroll.getItemId())) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: isPotentialScroll " + (toScroll.getState(false) >= 1) + " " + (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && !isSpecialScrollA && !isSpecialEquip) + " " + (vegas > 0) + " " + (ii.isCash(toScroll.getItemId())));
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isPotentialAddScroll(scroll.getItemId())) {
            boolean isA级潜能卷轴 = scroll.getItemId() / 100 == 20497;
            boolean is特殊装备 = toScroll.getItemId() / 10000 == 135 || toScroll.getItemId() / 1000 == 1098 || toScroll.getItemId() / 1000 == 1099 || toScroll.getItemId() / 1000 == 1190;
            if (vegas > 0 || ii.isCash(toScroll.getItemId()) || toScroll.getState(true) > 0 && !isA级潜能卷轴 && !is特殊装备) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: isPotentialAddScroll " + (toScroll.getState(true) >= 1) + " " + (toScroll.getLevel() == 0 && toScroll.getUpgradeSlots() == 0 && !isA级潜能卷轴 && !is特殊装备) + " " + (vegas > 0) + " " + (ii.isCash(toScroll.getItemId())));
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isUpgradeItemEx(scroll.getItemId())) {
            if (toScroll.getReqLevel() > ii.getReqEquipLevelMax(scroll.getItemId())) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 涅槃火焰 装备等级超过限定等级");
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isSpecialScroll(scroll.getItemId())) {
            //要砸的卷的道具是商城道具或者道具的星级大于等于12星
            int maxEnhance = scroll.getItemId() == 5064003 ? 7 : 12;
            if (ii.isCash(toScroll.getItemId()) || toScroll.getEnhance() >= maxEnhance) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 特殊卷轴 isCash - " + (ii.isCash(toScroll.getItemId())) + " getEnhance - " + (toScroll.getEnhance() >= maxEnhance) + " 保护星级: " + maxEnhance);
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isLimitBreakScroll(scroll.getItemId())) {
            if (!ItemConstants.isWeapon(toScroll.getItemId()) || (ii.getScrollLimitBreak(scroll.getItemId()) + oldLimitBreak) > 5000000) {
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        } else if (ItemConstants.isSealedScroll(scroll.getItemId())) {
            if (!GameConstants.canSealedLevelUp(toScroll.getItemId(), toScroll.getSealedLevel(), toScroll.getSealedExp())) {
                chr.dropMessage(-9, "砸卷错误: 封印解除经验不足或已经达到最高级，无法解除封印。");
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        }
        if (!ItemConstants.canScroll(toScroll.getItemId()) && !ItemConstants.isChaosScroll(toScroll.getItemId())) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 卷轴是否能对装备进行砸卷 " + !ItemConstants.canScroll(toScroll.getItemId()) + " 是否混沌卷轴 " + !ItemConstants.isChaosScroll(toScroll.getItemId()));
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if ((ItemConstants.isCleanSlate(scroll.getItemId()) || ItemConstants.isTablet(scroll.getItemId()) || ItemConstants.isGeneralScroll(scroll.getItemId()) || ItemConstants.isChaosScroll(scroll.getItemId())) && (vegas > 0 || ii.isCash(toScroll.getItemId()))) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 卷轴是否白衣卷轴 " + ItemConstants.isCleanSlate(scroll.getItemId()) + " isTablet " + ItemConstants.isTablet(scroll.getItemId()));
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if (ItemConstants.isTablet(scroll.getItemId()) && !ItemConstants.is武器攻击力卷轴(scroll.getItemId()) && toScroll.getDurability() < 0) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: isTablet " + ItemConstants.isTablet(scroll.getItemId()) + " getDurability " + (toScroll.getDurability() < 0));
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        } else if ((!ItemConstants.isTablet(scroll.getItemId()) && !ItemConstants.isPotentialScroll(scroll.getItemId()) && !ItemConstants.isEquipScroll(scroll.getItemId()) && !ItemConstants.isCleanSlate(scroll.getItemId()) && !ItemConstants.isSpecialScroll(scroll.getItemId()) && !ItemConstants.isPotentialAddScroll(scroll.getItemId()) && !ItemConstants.isChaosScroll(scroll.getItemId())) && toScroll.getDurability() >= 0 && !ItemConstants.is随机攻击卷轴(scroll.getItemId())) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: !isTablet ----- 1");
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        } else if (scroll.getItemId() == 2049405 && !ItemConstants.is真觉醒冒险之心(toScroll.getItemId())) { //2049405 - 真·觉醒冒险之心专用潜能力卷轴 - 不会扣除使用卷轴的次数，会赋予真·觉醒冒险之心项链专用潜在能力。\n#c只有真·冒险之心项链可以使用。#\n成功率 100%
            chr.dropMessage(1, "该卷轴只能对真·觉醒冒险之心使用。");
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        Item wscroll = null;
        // 骗子卷轴什么的 有些道具只能砸特定的卷轴
        Map<Integer, Integer> scrollReqs = ii.getScrollReqs(scroll.getItemId());
        if (scrollReqs != null && scrollReqs.size() > 0 && !scrollReqs.containsValue(toScroll.getItemId())) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 特定卷轴只能对指定的卷轴进行砸卷.");
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if (whiteScroll) {
            wscroll = chr.getInventory(MapleInventoryType.USE).findById(2340000); //祝福卷轴
            if (wscroll == null) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 使用祝福卷轴 但祝福卷轴信息为空.");
                }
                c.announce(InventoryPacket.getInventoryFull());
                whiteScroll = false;
            }
        }
        if (ItemConstants.isTablet(scroll.getItemId()) || ItemConstants.isGeneralScroll(scroll.getItemId())) {
            switch (scroll.getItemId() % 1000 / 100) {
                case 0: //1h
                    if (ItemConstants.isTwoHanded(toScroll.getItemId()) || !ItemConstants.isWeapon(toScroll.getItemId())) {
                        if (chr.isAdmin()) {
                            chr.dropMessage(-9, "砸卷错误: 最后检测 --- 0");
                        }
                        c.announce(InventoryPacket.getInventoryFull());
                        return false;
                    }
                    break;
                case 1: //2h
                    if (!ItemConstants.isTwoHanded(toScroll.getItemId()) || !ItemConstants.isWeapon(toScroll.getItemId())) {
                        if (chr.isAdmin()) {
                            chr.dropMessage(-9, "砸卷错误: 最后检测 --- 1");
                        }
                        c.announce(InventoryPacket.getInventoryFull());
                        return false;
                    }
                    break;
                case 2: //armor
                    if (ItemConstants.isAccessory(toScroll.getItemId()) || ItemConstants.isWeapon(toScroll.getItemId())) {
                        if (chr.isAdmin()) {
                            chr.dropMessage(-9, "砸卷错误: 最后检测 --- 2");
                        }
                        c.announce(InventoryPacket.getInventoryFull());
                        return false;
                    }
                    break;
                case 3: //accessory
                    if (!ItemConstants.isAccessory(toScroll.getItemId()) || ItemConstants.isWeapon(toScroll.getItemId())) {
                        if (chr.isAdmin()) {
                            chr.dropMessage(-9, "砸卷错误: 最后检测 --- 3");
                        }
                        c.announce(InventoryPacket.getInventoryFull());
                        return false;
                    }
                    break;
            }
        } else if (!ItemConstants.isAccessoryScroll(scroll.getItemId())
                && !ItemConstants.isChaosScroll(scroll.getItemId())
                && !ItemConstants.isCleanSlate(scroll.getItemId())
                && !ItemConstants.isEquipScroll(scroll.getItemId())
                && !ItemConstants.isPotentialScroll(scroll.getItemId())
                && !ItemConstants.isPotentialAddScroll(scroll.getItemId())
                && !ItemConstants.isSpecialScroll(scroll.getItemId())
                && !ItemConstants.isLimitBreakScroll(scroll.getItemId())
                && !ItemConstants.isResetScroll(scroll.getItemId())
                && !ItemConstants.is随机攻击卷轴(scroll.getItemId())
                && !ItemConstants.isSealedScroll(scroll.getItemId())
                && !ItemConstants.isUpgradeItemEx(scroll.getItemId())) {
            if (!ii.canScroll(scroll.getItemId(), toScroll.getItemId())) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 砸卷的卷轴无法对装备进行砸卷");
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        }
        if (ItemConstants.isAccessoryScroll(scroll.getItemId()) && !ItemConstants.isAccessory(toScroll.getItemId())) {
            if (chr.isAdmin()) {
                chr.dropMessage(-9, "砸卷错误: 卷轴为配置卷轴 但砸卷的装备不是配饰");
            }
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if (scroll.getQuantity() <= 0) {
            chr.dropSpouseMessage(0x0B, "砸卷错误，背包卷轴[" + ii.getName(scroll.getItemId()) + "]数量为 0 .");
            c.announce(InventoryPacket.getInventoryFull());
            return false;
        }
        if (legendarySpirit && vegas == 0) {
            if (chr.getSkillLevel(SkillFactory.getSkill(PlayerStats.getSkillByJob(1003, chr.getJob()))) <= 0 && ServerConfig.LOGIN_MAPLE_VERSION < 110) {
                if (chr.isAdmin()) {
                    chr.dropMessage(-9, "砸卷错误: 检测是否技能砸卷 角色没有拥有技能");
                }
                c.announce(InventoryPacket.getInventoryFull());
                return false;
            }
        }
        Equip scrolled = (Equip) ii.scrollEquipWithId(toScroll, scroll, whiteScroll, chr, vegas);
        Equip.ScrollResult scrollSuccess;
        if (scrolled == null) { //如果返回的砸卷后的道具为空
            if (ItemFlag.装备防爆.check(oldFlag)) { //检测未砸卷前是否有防爆效果
                scrolled = toScroll;
                scrollSuccess = Equip.ScrollResult.失败;
                scrolled.removeFlag((short) ItemFlag.装备防爆.getValue());
                chr.dropSpouseMessage(0x0B, "由于卷轴的效果，物品没有损坏。");
            } else if (ItemConstants.isAdvancedEquipScroll(scroll.getItemId())) {
                scrollSuccess = Equip.ScrollResult.失败;
            } else {
                scrollSuccess = Equip.ScrollResult.消失;
            }
        } else {
            if ((scroll.getItemId() / 100 == 20497 && scrolled.getState(false) == 1) || scrolled.getLevel() > oldLevel || scrolled.getEnhance() > oldEnhance || scrolled.getState(false) > oldState || scrolled.getFlag() > oldFlag || scrolled.getState(true) > oldAddState || scrolled.getLimitBreak() > oldLimitBreak || scrolled.getSealedLevel() > oldSealedLevel) {
                scrollSuccess = Equip.ScrollResult.成功;
            } else if (ItemConstants.isCleanSlate(scroll.getItemId()) && scrolled.getUpgradeSlots() > oldSlots) {
                scrollSuccess = Equip.ScrollResult.成功;
            } else if (ItemConstants.isResetScroll(scroll.getItemId()) && scrolled != toScroll) {
                scrollSuccess = Equip.ScrollResult.成功;
            } else if (ItemConstants.isUpgradeItemEx(scroll.getItemId())) {
                scrollSuccess = Equip.ScrollResult.成功;
            } else {
                scrollSuccess = Equip.ScrollResult.失败;
            }
            //如果砸卷后道具不为空 就清除防爆卷轴状态 且道具不为白衣和特殊卷轴
            if (ItemFlag.装备防爆.check(oldFlag) && !ItemConstants.isCleanSlate(scroll.getItemId()) && !ItemConstants.isSpecialScroll(scroll.getItemId())) {
                scrolled.removeFlag((short) ItemFlag.装备防爆.getValue());
            }
            //如果是管理员
            if (chr.isIntern()) {
                scrolled.addFlag((short) ItemFlag.CRAFTED.getValue());
                scrolled.setOwner(chr.getName());
            }
        }
        //装备带有保护卷轴不消失的效果
        if (ItemFlag.卷轴防护.check(oldFlag)) {
            if (scrolled != null) {
                scrolled.removeFlag((short) ItemFlag.卷轴防护.getValue());
            }
            if (scrollSuccess == Equip.ScrollResult.成功) {
                chr.getInventory(ItemConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false); //删除卷轴信息
            } else {
                chr.dropSpouseMessage(0x0B, "由于卷轴的效果，卷轴" + ii.getName(scroll.getItemId()) + "没有消失。");
            }
        } else {
            chr.getInventory(ItemConstants.getInventoryType(scroll.getItemId())).removeItem(scroll.getPosition(), (short) 1, false); //删除卷轴信息
        }
        if (whiteScroll) { //祝福卷轴
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, wscroll.getPosition(), (short) 1, false, false);
        } else if (scrollSuccess == Equip.ScrollResult.失败 && scrolled.getUpgradeSlots() < oldSlots && chr.getInventory(MapleInventoryType.CASH).findById(5640000) != null) {
            chr.setScrolledPosition(scrolled.getPosition());
            if (vegas == 0) {
                c.announce(MaplePacketCreator.pamSongUI());
            }
        }
        List<ModifyInventory> mods = new ArrayList<>();
        mods.add(new ModifyInventory(scroll.getQuantity() > 0 ? 1 : 3, scroll)); //更新卷轴信息 [1 = 更新卷轴数量 3 = 删除卷轴]
        if (scrollSuccess == Equip.ScrollResult.消失) {
            mods.add(new ModifyInventory(3, toScroll)); //删除装备
            if (dst < 0) {
                chr.getInventory(MapleInventoryType.EQUIPPED).removeItem(toScroll.getPosition());
            } else {
                chr.getInventory(MapleInventoryType.EQUIP).removeItem(toScroll.getPosition());
            }
        } else if (vegas == 0) {
            mods.add(new ModifyInventory(3, scrolled)); //删除装备
            mods.add(new ModifyInventory(0, scrolled)); //获得装备
        }
        c.announce(InventoryPacket.modifyInventory(true, mods, chr));
        chr.getMap().broadcastMessage(chr, InventoryPacket.getScrollEffect(chr.getId(), scrollSuccess, legendarySpirit, whiteScroll, scroll.getItemId(), toScroll.getItemId()), vegas == 0);
        if (dst < 0 && (scrollSuccess == Equip.ScrollResult.成功 || scrollSuccess == Equip.ScrollResult.消失) && vegas == 0) {
            chr.equipChanged();
        }
        chr.finishActivity(120102);
        if (scrolled != null) {
            chr.forceReAddItem_NoUpdate(scrolled, scrolled.getPosition() >= 0 ? MapleInventoryType.EQUIP : MapleInventoryType.EQUIPPED);
            if (scrolled.getEnhance() > oldEnhance && scrolled.getEnhance() > 5) {
                chr.getClient().getChannelServer().startMapEffect(chr.getName() + "成功将" + ii.getName(scrolled.getItemId()) + "强化至 " + scrolled.getEnhance() + "星！", 5120037);
            }
        }
        return scrollSuccess == Equip.ScrollResult.成功;
    }

    public static void UseEquipEnchanting(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int mode = slea.readByte();
        switch (mode) {
            case 0: {
                chr.updateTick(slea.readInt());
                short slot = slea.readShort();
                short s3 = slea.readShort();
                short s4 = slea.readShort();
                Item toScroll = chr.getInventory(slot < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem(slot);
                if (toScroll == null) {
                    chr.send(InventoryPacket.enchantingOperation(104, 0, null, null, null));
                    c.sendEnableActions();
                    return;
                }
                Equip nEquip = (Equip) toScroll;
                Item e3 = toScroll.copy();
                ArrayList<EnchantScrollEntry> scrollList = EnchantHandler.Companion.getScrollList(nEquip);
                if (scrollList.isEmpty() || scrollList.size() < s3 - 1 || nEquip.getUpgradeSlots() < 1 || !c.getPlayer().haveItem(4001832, scrollList.get(s3).getNeed())) {
                    c.sendEnableActions();
                    if (c.getPlayer().isAdmin()) {
                        c.getPlayer().dropMessage(-9, "[咒语痕迹] 检测：找不到卷轴：" + scrollList.isEmpty() + " 收到的卷轴不在范围：" + (scrollList.size() < s3 - 1) + " 沒有剩余次数：" + (nEquip.getUpgradeSlots() < 1) + " 痕迹不足：" + !c.getPlayer().haveItem(4001832, scrollList.get(s3).getNeed()));
                    }
                    if (nEquip.getUpgradeSlots() < 1) {
                        c.getPlayer().dropMessage(-1, "当前道具已经不能在进行强化！");
                    }
                    chr.send(InventoryPacket.enchantingOperation(104, 0, null, null, null));
                    return;
                }
                EnchantScrollEntry enchantScrollEntry = scrollList.get(s3);
                if (Randomizer.isSuccess(enchantScrollEntry.getSucess())) {
                    s4 = 1;
                }
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(-9, "[咒语痕迹] 强化道具：" + toScroll + " 选中卷轴:" + enchantScrollEntry.getName() + " 消耗痕迹：" + enchantScrollEntry.getNeed() * getNeedRate(nEquip.getReqLevel()) + " 成功率：" + enchantScrollEntry.getSucess() + "%  强化結果：" + (s4 == 1));
                }
                enchantScrollEquip(enchantScrollEntry, nEquip, s4, c, false);
                chr.forceUpdateItem(nEquip, true);
                chr.send(InventoryPacket.enchantingOperation(100, s4, e3, toScroll, null, enchantScrollEntry.getName()));
                chr.send(InventoryPacket.enchantingOperation(50, 0, null, nEquip, EnchantHandler.Companion.getScrollList(nEquip)));
                break;
            }
            case 1: {
                boolean bl3;
                chr.updateTick(slea.readInt());
                slea.readShort();
                bl3 = slea.readByte() == 1;
                if (bl3) {
                    slea.readInt();
                }
                slea.readInt();
                slea.readInt();
                boolean bl5 = slea.readByte() == 1;
                if (enchantEnhance(chr, (short) chr.getEnchant(), bl3, bl5)) break;
                chr.send(InventoryPacket.enchantingOperation(104, 0, null, null, null));
                break;
            }
            case 2: {
                chr.updateTick(slea.readInt());
                short src = slea.readShort();
                short dec = slea.readShort();
                inheritEquip(chr, src, dec);
                chr.sendEnableActions();
                break;
            }
            case 0x32: {// 请求装备附魔的卷轴列表
                int slot = slea.readInt();
                Equip equip = (Equip) chr.getInventory(slot < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) slot);
                chr.send(InventoryPacket.enchantingOperation(mode, 0, null, equip, EnchantHandler.Companion.getScrollList(equip)));
                break;
//                int slot = slea.readInt();
//                Item toScroll = chr.getInventory(slot < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) slot);
//                c.announce(InventoryPacket.getEnchantingScrolls(((Equip) toScroll).getUpgradeSlots() <= 0 ? Collections.emptyList() : MapleItemInformationProvider.getInstance().getEnchantingScrolls(ItemConstants.getEnchantingEquipType(toScroll.getItemId())), mode));
//                break;
            }
            case 0x34: {// 请求装备附魔的星级属性
                int n2 = slea.readInt();
                chr.setEnchant(n2);
                Equip toEnhance = n2 < 0 ? (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) n2) : (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem((short) n2);
                NirvanaFlame nirvanaFlame = new NirvanaFlame(toEnhance.getNirvanaFlame());
                toEnhance.getNirvanaFlame().reset();
                chr.send(InventoryPacket.enchantingOperation(mode, 0, toEnhance, null, null));
                toEnhance.setNirvanaFlame(nirvanaFlame);
                break;
//                int slot = slea.readInt();
//                Item toScroll = chr.getInventory(slot < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem((short) slot);
//                EnchantingScroll es = MapleItemInformationProvider.getInstance().getEnchantingEnhanceInfo(ItemConstants.getEnchantingEquipType(toScroll.getItemId()), ((Equip) toScroll).getEnhance() + 1);
//                if (es == null) {
//                    c.dropMessage("获取装备附魔的星级属性错误。");
//                    c.announce(MaplePacketCreator.enableActions());
//                    return;
//                }
//                c.announce(InventoryPacket.getEnchantingEnhance(es, mode));
//                break;
            }
            case 0x33:
            case 0x35: {// 星级附魔的强化开始 {
                c.announce(InventoryPacket.enchantingOperation(mode, 0, null, null, null));
                break;
            }
        }
    }

    public static void inheritEquip(MapleCharacter chr, short src, short dec) {
        ArrayList<ModifyInventory> mods = new ArrayList<>();
        Equip srcEquip = (Equip) chr.getInventory(src < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP).getItem(src);
        Equip decEquip = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(dec);
        srcEquip.inherit(decEquip, srcEquip);
        if (EnhanceResultType.EQUIP_MARK.check(decEquip.getEnhanctBuff())) {
            srcEquip.setEnhanctBuff((short) (decEquip.getEnhanctBuff() - EnhanceResultType.EQUIP_MARK.getValue()));
            srcEquip.addFlag((short) ItemFlag.不可交易.getValue());
            srcEquip.setOwner(chr.getName());
            mods.add(new ModifyInventory(3, srcEquip));
            mods.add(new ModifyInventory(0, srcEquip));
            mods.add(new ModifyInventory(3, decEquip));
            chr.getInventory(MapleInventoryType.EQUIP).removeItem(decEquip.getPosition());
            chr.send(InventoryPacket.enchantingOperation(103, 0, decEquip, srcEquip, null));
            chr.send(InventoryPacket.modifyInventory(true, mods, chr));
            chr.equipChanged();
        } else {
            chr.send(InventoryPacket.enchantingOperation(104, 0, null, null, null));
        }
    }

    public static boolean enchantEnhance(MapleCharacter chr, short slot, boolean bl2, boolean bl3) {
        chr.setScrolledPosition((short) 0);
        Equip item = slot < 0 ? (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(slot) : (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(slot);
        if (item == null || chr.hasBlockedInventory()) {
            return false;
        }
        if (item.getEnhance() >= ItemConstants.getMaxEnhance(item.getItemId())) {
            chr.send(InventoryPacket.enchantingOperation(33, 0, null, null, null));
            return false;
        }
        int n2 = 2 * (item.getEnhance() * item.getEnhance() + 2) * (bl3 ? 2 : 1);
        if (chr.getItemQuantity(4001839) < n2) {
            return false;
        }
        chr.removeItem(4001839, n2);
        byte by2 = item.getEnhance();
        Item toEnhance = item.copy();
        int n3 = bl2 ? 8 : 0;
        NirvanaFlame nirvanaFlame = new NirvanaFlame(item.getNirvanaFlame());
        Equip nEquip = toStarForce(item, chr, n3, bl3);
        nEquip.setNirvanaFlame(nirvanaFlame);
        ArrayList<ModifyInventory> arrayList = new ArrayList<>();
        if (EnhanceResultType.EQUIP_MARK.check(nEquip.getEnhanctBuff())) {
            chr.send(InventoryPacket.enchantingOperation(101, 2, toEnhance, nEquip, null));
            if (slot < 0) {
                MapleInventoryManipulator.unequip(chr.getClient(), slot, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
            }
            arrayList.add(new ModifyInventory(3, nEquip));
            arrayList.add(new ModifyInventory(0, nEquip));
        } else if (nEquip.getEnhance() > by2) {
            if (slot < 0) {
                chr.equipChanged();
            }
            nEquip.setBonus(0);
            arrayList.add(new ModifyInventory(3, nEquip));
            arrayList.add(new ModifyInventory(0, nEquip));
            chr.send(InventoryPacket.enchantingOperation(101, 1, toEnhance, nEquip, null));
        } else if (nEquip.getEnhance() < by2) {
            nEquip.setBonus(nEquip.getBonus() + 1);
            arrayList.add(new ModifyInventory(3, nEquip));
            arrayList.add(new ModifyInventory(0, nEquip));
            chr.send(InventoryPacket.enchantingOperation(101, 0, toEnhance, nEquip, null));
        } else {
            nEquip.setBonus(nEquip.getBonus() + 1);
            chr.send(InventoryPacket.enchantingOperation(101, 3, toEnhance, toEnhance, null));
        }
        if (slot < 0) {
            chr.equipChanged();
        }
        chr.forceUpdateItem(nEquip, true);
        chr.send(InventoryPacket.modifyInventory(true, arrayList, chr));
        return true;
    }

    public static Equip toStarForce(Item item, MapleCharacter chr, int n2, boolean bl2) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip nEquip = (Equip) item;
        byte by2 = nEquip.getEnhance();
        boolean bl3 = ii.isSuperiorEquip(nEquip.getItemId());
        boolean bl4 = nEquip.getStartCurse();
        boolean bl5 = false;
        int n3 = nEquip.getNeedStar() * 10;
        int n4 = chr.getTrait(MapleTraitType.craft).getLevel() / 10;
        int rate = n3 + n3 * n4 / 10 + n2 * 10;
        int n6 = bl4 && (!bl2 || bl3) ? (1000 - n3) * (bl3 ? by2 - 4 : by2 - 14) / 18 : 0;
        int n7 = nEquip.getStartDown() ? 1000 - n3 : 0;
        rate = chr.isAdmin() ? 1000 : rate;
        if (chr.isAdmin()) {
            chr.dropSpouseMessage(11, "[星之力强化] 默认几率: " + n3 / 10 + "% 倾向加成: " + n4 + "% 最终几率: " + rate / 10 + "% 失败下降几率: " + n7 / 10 + "% 失败消失几率: " + n6 / 10 + "%");
        }
        if (by2 >= ItemConstants.getMaxEnhance(nEquip.getItemId())) {
            return nEquip;
        }
        nEquip.getNirvanaFlame().reset();
        int rand = Randomizer.nextInt(1000);
        int curseReduceCount = StarForceConfig.CURSE_REDUCE_COUNT;
        curseReduceCount = curseReduceCount >= 0 ? curseReduceCount : (int) nEquip.getEnhance();
        if (rate < rand && !nEquip.getBonusTime()) {
            if (rate < rand && rand < rate + n6) {
                for (int i2 = 0; i2 < curseReduceCount; ++i2) {
                    EnchantHandler.Companion.toEnchantScrollHandler(nEquip, true);
                }
                nEquip.setEnhanctBuff((short) (nEquip.getEnhanctBuff() | EnhanceResultType.EQUIP_MARK.getValue()));
                return nEquip;
            }
            if (rate + n6 < rand && rand < n3 + n6 + n7 && by2 > 5 && by2 != 10 && by2 != 15 && by2 != 20) {
                bl5 = true;
            } else {
                return nEquip;
            }
        }
        EnchantHandler.Companion.toEnchantScrollHandler(nEquip, bl5);
        return nEquip;
    }

    public static void enchantScrollEquip(EnchantScrollEntry scrollEntry, Equip equip, int mode, MapleClient c, boolean update) {
        int mask = scrollEntry.getMask();
        NirvanaFlame nirvanaFlame = new NirvanaFlame(equip.getNirvanaFlame());
        equip.getNirvanaFlame().reset();
        if (scrollEntry.getViewType() < 4) {
            if (mode == 1) {
                for (int value : scrollEntry.getValues()) {
                    if (EnchantScrollFlag.物攻.check(mask)) {
                        mask -= EnchantScrollFlag.物攻.getValue();
                        equip.setWatk((short) (equip.getWatk() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.魔攻.check(mask)) {
                        mask -= EnchantScrollFlag.魔攻.getValue();
                        equip.setMatk((short) (equip.getMatk() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.力量.check(mask)) {
                        mask -= EnchantScrollFlag.力量.getValue();
                        equip.setStr((short) (equip.getStr() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.敏捷.check(mask)) {
                        mask -= EnchantScrollFlag.敏捷.getValue();
                        equip.setDex((short) (equip.getDex() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.智力.check(mask)) {
                        mask -= EnchantScrollFlag.智力.getValue();
                        equip.setInt((short) (equip.getInt() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.运气.check(mask)) {
                        mask -= EnchantScrollFlag.运气.getValue();
                        equip.setLuk((short) (equip.getLuk() + value));
                        continue;
                    }
                    if (EnchantScrollFlag.Hp.check(mask)) {
                        mask -= EnchantScrollFlag.Hp.getValue();
                        equip.setHp((short) (equip.getHp() + value));
                    }
                }
                equip.setLevel((byte) (equip.getLevel() + 1));
            }
            equip.setUpgradeSlots((byte) (equip.getUpgradeSlots() - 1));
        }
        equip.setNirvanaFlame(nirvanaFlame);
        if (!update) {
            MapleInventoryManipulator.removeById(c, ItemConstants.getInventoryType(4001832), 4001832, scrollEntry.getNeed() * getNeedRate(equip.getReqLevel()), true, false);
            c.getPlayer().forceReAddItem(equip, equip.getPosition() >= 0 ? MapleInventoryType.EQUIP : MapleInventoryType.EQUIPPED);
        }
    }

    public static int getNeedRate(int reqLevel) {
        switch (reqLevel / 20) {
            case 0:
            case 1: {
                return 1;
            }
            case 2:
            case 3: {
                return 2;
            }
            case 4: {
                return 5;
            }
            case 5: {
                return 6;
            }
            case 6: {
                return 7;
            }
            case 7: {
                return 8;
            }
        }
        return 5;
    }

    public static void ChangeWeaponPotential(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.skip(4);
        int mode = slea.readByte();
        if (mode == 1) {
            Item lazuliItem = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
            Item lapisItem = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
            int level = (int) (Math.floor(chr.getLevel() / 10) * 10 + 10);
            c.announce(InventoryPacket.getZeroWeaponInfo(lazuliItem.getItemId() % 10, level, Math.min(1562007, lazuliItem.getItemId() + 1), Math.min(1572007, lapisItem.getItemId() + 1)));
            c.announce(InventoryPacket.getZeroWeaponChangePotential(100000, 600));
        }
    }

    public static void ChangeWeaponPotential_WP(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr.getWeaponPoint() >= 600 && chr.getMeso() >= 100000L) {
            final Equip lazuliequip = ItemPotentialAndMagnify((Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) (-10)), chr, false);
            final Equip lapisequip = ((Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem((short) (-11))).copyPotential(lazuliequip);
            chr.forceUpdateItem(lazuliequip, true);
            chr.forceUpdateItem(lapisequip, true);
            chr.equipChanged();
            chr.gainWeaponPoint(-600);
            chr.gainMeso(-100000L, false);
            chr.dropSpouseMessage(0x0C, "潜能被变更了。");
        } else {
            chr.dropMessage(-1, "金币或WP不足，无法更改潜能。");
            c.sendEnableActions();
        }
    }

    public static Equip ItemPotentialAndMagnify(Equip toScroll, MapleCharacter player, boolean bl2) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        final int n = ii.getReqLevel(toScroll.getItemId()) / 10;
        final int n2 = (n >= 20) ? 19 : n;
        final List<List<StructItemOption>> list = new LinkedList<>(ii.getAllPotentialInfo().values());
        if (bl2) {
            toScroll.c(ServerConfig.CHANNEL_RATE_POTENTIALLEVEL, ItemConstants.PotentialConstants.getDefaultPotentialFlag(10000), 0);
        }
        final boolean b3 = toScroll.getState(false) >= 17 || toScroll.getState(false) <= 0;
        int n3 = 0;
        final int n4 = Math.abs(toScroll.getPotential(1, b3)) % 1000000;
        int n5;
        if (n4 >= 100000) {
            n3 = n4 / 100000;
            n5 = n4 % 100000;
        } else {
            n5 = 0;
        }
        final int n6 = (toScroll.getPotential(2, b3) != 0) ? 3 : 2;
        int n7 = toScroll.getState(b3) + 16;
        if (n7 > 20 || n7 < 17) {
            n7 = 17;
        }
        final int abs = Math.abs(toScroll.getPotential(3, b3));
        toScroll.setPotential(0, 3, b3);
        final boolean check = ItemConstants.PotentialConstants.PotentialFlag.前两条相同.check(abs);
        for (int i = 1; i <= n6; ++i) {
            if (i == n3) {
                toScroll.setPotential(n5, n3, b3);
            } else {
                StructItemOption itemOption;
                do {
                    itemOption = list.get(Randomizer.nextInt(list.size())).get(n2);
                } while (itemOption == null
                        || itemOption.reqLevel / 10 > n2
                        || !GameConstants.optionTypeFits(itemOption.optionType, toScroll.getItemId())
                        || !GameConstants.isBlockedPotential(toScroll, itemOption.opID, b3, ItemConstants.PotentialConstants.PotentialFlag.点券光环.check(abs))
                        || !GameConstants.potentialIDFits(itemOption.opID, n7, ItemConstants.PotentialConstants.PotentialFlag.对等.check(abs) ? 1 : i)
                        || (ItemConstants.PotentialConstants.PotentialFlag.去掉无用潜能.check(abs) && (!ItemConstants.PotentialConstants.PotentialFlag.去掉无用潜能.check(abs)
                        || ItemConstants.PotentialConstants.checkProperties(itemOption))));
                if (i == 1 && check) {
                    toScroll.setPotential(itemOption.opID, 2, b3);
                }
                if (i != 2 || !check) {
                    toScroll.setPotential(itemOption.opID, i, b3);
                }
            }
        }
        toScroll.initAllState();
        if (!b3 && toScroll.getState(false) >= 18 && toScroll.getStateMsg() < 3) {
            if (toScroll.getState(false) == 18 && toScroll.getStateMsg() == 0) {
                toScroll.setStateMsg(1);
                player.finishAchievement(52);
                if (!player.isAdmin()) {
                    WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(player.getMedalText() + player.getName() + " : 鉴定出 A 级装备，大家祝贺他(她)吧！", true, player.getClient().getChannel(), toScroll));
                }
            } else if (toScroll.getState(false) == 19 && toScroll.getStateMsg() <= 1) {
                toScroll.setStateMsg(2);
                player.finishAchievement(53);
                if (!player.isAdmin()) {
                    WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(player.getMedalText() + player.getName() + " : 鉴定出 S 级装备，大家祝贺他(她)吧！", true, player.getClient().getChannel(), toScroll));
                }
            } else if (toScroll.getState(false) == 20 && toScroll.getStateMsg() <= 2) {
                toScroll.setStateMsg(3);
                player.finishAchievement(54);
                if (!player.isAdmin()) {
                    WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(player.getMedalText() + player.getName() + " : 鉴定出 SS 级装备，大家祝贺他(她)吧！", true, player.getClient().getChannel(), toScroll));
                }
            }
        }
        return toScroll;
    }
}
