package server;

import client.*;
import client.inventory.*;
import client.skills.SkillEntry;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConstants;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.cashshop.CashItemFactory;
import server.cashshop.CashItemInfo;
import server.maps.AramiaFireWorks;
import server.quest.MapleQuest;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.packet.AndroidPacket;
import tools.packet.InventoryPacket;
import tools.packet.MTSCSPacket;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapleInventoryManipulator {

    private static final Logger log = LogManager.getLogger("ItemLog");

    public static void addRing(MapleCharacter chr, int itemId, int ringId, int sn) {
        CashItemInfo csi = CashItemFactory.getInstance().getItem(sn);
        if (csi == null) {
            return;
        }
        Item ring = chr.getCashInventory().toItem(csi, ringId);
        if (ring == null || ring.getUniqueId() != ringId || ring.getUniqueId() <= 0 || ring.getItemId() != itemId) {
            return;
        }
        chr.getCashInventory().addToInventory(ring);
        chr.send(MTSCSPacket.INSTANCE.购买商城道具(ring, sn, chr.getClient().getAccID()));
    }

    public static boolean addbyItem(MapleClient c, Item item) {
        return addbyItem(c, item, false) >= 0;
    }

    public static short addbyItem(MapleClient c, Item item, boolean fromcs) {
        MapleInventoryType type = ItemConstants.getInventoryType(item.getItemId());
        short newSlot = c.getPlayer().getInventory(type).addItem(item);
        if (newSlot == -1) {
            if (!fromcs) {
                c.announce(InventoryPacket.getInventoryFull());
                c.announce(InventoryPacket.getShowInventoryFull());
            }
            return newSlot;
        }
        if (ItemConstants.isHarvesting(item.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        if (item.hasSetOnlyId()) {
            item.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
        }
        c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, item))));
        c.getPlayer().havePartyQuest(item.getItemId());
        if (!fromcs && type.equals(MapleInventoryType.EQUIP)) {
            c.getPlayer().checkCopyItems();
        }
        return newSlot;
    }

    public static int getUniqueId(int itemId, MaplePet pet) {
        int uniqueid = -1;
        if (ItemConstants.isPet(itemId)) {
            if (pet != null) {
                uniqueid = pet.getUniqueId();
            } else {
                uniqueid = MapleInventoryIdentifier.getInstance();
            }
        } else if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.CASH || MapleItemInformationProvider.getInstance().isCash(itemId)) { //less work to do
            uniqueid = MapleInventoryIdentifier.getInstance(); //shouldnt be generated yet, so put it here
        }
        return uniqueid;
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, int state, String gmLog) {
        return addById(c, itemId, quantity, null, null, 0, state, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, long period, String gmLog) {
        return addById(c, itemId, quantity, null, null, period, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, long period, int state, String gmLog) {
        return addById(c, itemId, quantity, null, null, period, state, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addById(c, itemId, quantity, owner, null, 0, 0, gmLog);
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, String gmLog) {
        return addId(c, itemId, quantity, owner, null, 0, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, 0, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, String gmLog) {
        return addById(c, itemId, quantity, owner, pet, period, 0, gmLog);
    }

    public static boolean addById(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, int state, String gmLog) {
        return addId(c, itemId, quantity, owner, pet, period, state, gmLog) >= 0;
    }

    public static byte addId(MapleClient c, int itemId, short quantity, String owner, MaplePet pet, long period, int state, String gmLog) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.announce(InventoryPacket.getInventoryFull());
            c.announce(InventoryPacket.showItemUnavailable());
            return -1;
        }
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        int uniqueid = getUniqueId(itemId, pet);
        short newSlot = -1;
        if (!type.equals(MapleInventoryType.EQUIP)) { //如果不是装备道具
            short slotMax = ii.getSlotMax(itemId);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ItemConstants.isRechargable(itemId)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && (eItem.getOwner().equals(owner) || owner == null) && eItem.getExpiration() == -1) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, eItem))));
                                newSlot = eItem.getPosition();
                            }
                        } else {
                            break;
                        }
                    }
                }
                Item nItem;
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0, uniqueid, (short) 0);
                        newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1) {
                            c.announce(InventoryPacket.getInventoryFull());
                            c.announce(InventoryPacket.getShowInventoryFull());
                            return -1;
                        }
                        if (gmLog != null) {
                            nItem.setGMLog(gmLog);
                        }
                        if (owner != null) {
                            nItem.setOwner(owner);
                        }
                        if (period > 0) {
                            if (period < 1000) {
                                nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                            } else {
                                nItem.setExpiration(System.currentTimeMillis() + period);
                            }
                        }
                        if (pet != null) {
                            nItem.setPet(pet);
                            pet.setInventoryPosition(newSlot);
                        }
                        c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                        if (ItemConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        c.getPlayer().havePartyQuest(itemId);
                        c.announce(MaplePacketCreator.enableActions());
                        return (byte) newSlot;
                    }
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0, uniqueid, (short) 0);
                newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(InventoryPacket.getInventoryFull());
                    c.announce(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                if (period > 0) {
                    if (period < 1000) {
                        nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                    } else {
                        nItem.setExpiration(System.currentTimeMillis() + period);
                    }
                }
                if (gmLog != null) {
                    nItem.setGMLog(gmLog);
                }
                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
            }
        } else {
            //这个是装备道具
            if (quantity == 1) {
                Item nEquip = ii.getEquipById(itemId, uniqueid);
                if (owner != null) { //设置装备所有者
                    nEquip.setOwner(owner);
                }
                if (gmLog != null) { //设置装备获得日志
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) { //设置到期时间
                    if (period < 1000) {
                        nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                    } else {
                        nEquip.setExpiration(System.currentTimeMillis() + period);
                    }
                }
                if (state > 0) { //设置装备潜能
                    ii.setPotentialState((Equip) nEquip, state);
                }
                if (nEquip.hasSetOnlyId()) {
                    nEquip.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                }
                newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    c.announce(InventoryPacket.getInventoryFull());
                    c.announce(InventoryPacket.getShowInventoryFull());
                    return -1;
                }
                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip))));
                if (ItemConstants.isHarvesting(itemId)) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
                c.getPlayer().checkCopyItems();
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        c.getPlayer().havePartyQuest(itemId);
        return (byte) newSlot;
    }

    public static Item addbyId_Gachapon(MapleClient c, int itemId, short quantity) {
        return addbyId_Gachapon(c, itemId, quantity, null, 0);
    }

    public static Item addbyId_Gachapon(MapleClient c, int itemId, short quantity, String gmLog) {
        return addbyId_Gachapon(c, itemId, quantity, null, 0);
    }

    public static Item addbyId_Gachapon(MapleClient c, int itemId, short quantity, String gmLog, long period) {
        if (c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.USE).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.ETC).getNextFreeSlot() == -1 || c.getPlayer().getInventory(MapleInventoryType.SETUP).getNextFreeSlot() == -1) {
            return null;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if ((ii.isPickupRestricted(itemId) && c.getPlayer().haveItem(itemId, 1, true, false)) || (!ii.itemExists(itemId))) {
            c.announce(InventoryPacket.getInventoryFull());
            c.announce(InventoryPacket.showItemUnavailable());
            return null;
        }
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemId);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemId);
            if (!ItemConstants.isRechargable(itemId)) {
                Item nItem = null;
                boolean recieved = false;
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            nItem = i.next();
                            short oldQ = nItem.getQuantity();
                            if (oldQ < slotMax) {
                                recieved = true;
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                nItem.setQuantity(newQ);
                                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, nItem))));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    if (newQ != 0) {
                        quantity -= newQ;
                        nItem = new Item(itemId, (byte) 0, newQ, (byte) 0);
                        short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                        if (newSlot == -1 && recieved) {
                            return nItem;
                        } else if (newSlot == -1) {
                            return null;
                        }
                        recieved = true;
                        if (gmLog != null) { //设置装备获得日志
                            nItem.setGMLog(gmLog);
                        }
                        if (period > 0) { //设置到期时间
                            if (period < 1000) {
                                nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                            } else {
                                nItem.setExpiration(System.currentTimeMillis() + period);
                            }
                        }
                        c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                        if (ItemConstants.isRechargable(itemId) && quantity == 0) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (recieved && nItem != null) {
                    c.getPlayer().havePartyQuest(nItem.getItemId());
                    return nItem;
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                Item nItem = new Item(itemId, (byte) 0, quantity, (byte) 0);
                short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    return null;
                }
                if (gmLog != null) { //设置装备获得日志
                    nItem.setGMLog(gmLog);
                }
                if (period > 0) { //设置到期时间
                    if (period < 1000) {
                        nItem.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                    } else {
                        nItem.setExpiration(System.currentTimeMillis() + period);
                    }
                }
                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nItem))));
                c.getPlayer().havePartyQuest(nItem.getItemId());
                return nItem;
            }
        } else {
            //这个是装备道具  装备道具只能数量为 1
            if (quantity == 1) {
                Item nEquip = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                short newSlot = c.getPlayer().getInventory(type).addItem(nEquip);
                if (newSlot == -1) {
                    return null;
                }
                if (gmLog != null) { //设置装备获得日志
                    nEquip.setGMLog(gmLog);
                }
                if (period > 0) { //设置到期时间
                    if (period < 1000) {
                        nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                    } else {
                        nEquip.setExpiration(System.currentTimeMillis() + period);
                    }
                }
                if (nEquip.hasSetOnlyId()) {
                    nEquip.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                }
                c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip))));
                c.getPlayer().havePartyQuest(nEquip.getItemId());
                return nEquip;
            } else {
                throw new InventoryException("Trying to create equip with non-one quantity");
            }
        }
        return null;
    }

    public static boolean addFromDrop(MapleClient c, Item item, boolean show) {
        return addFromDrop(c, item, show, false);
    }

    public static boolean addFromDrop(MapleClient c, Item item, boolean show, boolean enhance) {
        return addFromDrop(c, item, show, enhance, true);
    }

    public static boolean addFromDrop(MapleClient c, Item item, boolean show, boolean enhance, boolean updateTick) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getPlayer() == null || (ii.isPickupRestricted(item.getItemId()) && c.getPlayer().haveItem(item.getItemId(), 1, true, false)) || (!ii.itemExists(item.getItemId()))) {
            c.announce(InventoryPacket.getInventoryFull());
            c.announce(InventoryPacket.showItemUnavailable());
            return false;
        }
        int before = c.getPlayer().itemQuantity(item.getItemId());
        short quantity = item.getQuantity();
        MapleInventoryType type = ItemConstants.getInventoryType(item.getItemId());
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(item.getItemId());
            List<Item> existing = c.getPlayer().getInventory(type).listById(item.getItemId());
            if (!ItemConstants.isRechargable(item.getItemId())) {
                if (quantity <= 0) { //wth
                    c.announce(InventoryPacket.getInventoryFull());
                    c.announce(InventoryPacket.showItemUnavailable());
                    return false;
                }
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    Iterator<Item> i = existing.iterator();
                    while (quantity > 0) {
                        if (i.hasNext()) {
                            Item eItem = i.next();
                            short oldQ = eItem.getQuantity();
                            if (oldQ < slotMax && item.getOwner().equals(eItem.getOwner()) && item.getExpiration() == eItem.getExpiration() && item.getFamiliarCard() == null) {
                                short newQ = (short) Math.min(oldQ + quantity, slotMax);
                                quantity -= (newQ - oldQ);
                                eItem.setQuantity(newQ);
                                eItem.setUniqueId(item.getUniqueId());
                                c.announce(InventoryPacket.modifyInventory(updateTick, Collections.singletonList(new ModifyInventory(1, eItem))));
                            }
                        } else {
                            break;
                        }
                    }
                }
                // add new slots if there is still something left
                while (quantity > 0) {
                    short newQ = (short) Math.min(quantity, slotMax);
                    quantity -= newQ;
                    Item nItem = new Item(item.getItemId(), (byte) 0, newQ, item.getFlag());
                    nItem.setExpiration(item.getExpiration());
                    nItem.setOwner(item.getOwner());
                    nItem.setPet(item.getPet());
                    nItem.setGMLog(item.getGMLog());
                    nItem.setFamiliarCard(item.getFamiliarCard());
                    nItem.setFamiliarid(item.getFamiliarid());
                    if (item.getUniqueId() != -1) {
                        nItem.setUniqueId(item.getUniqueId());
                    }
                    short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                    if (newSlot == -1) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        item.setQuantity((short) (quantity + newQ));
                        return false;
                    }
                    c.announce(InventoryPacket.modifyInventory(updateTick, Collections.singletonList(new ModifyInventory(0, nItem))));
                }
            } else {
                // Throwing Stars and Bullets - Add all into one slot regardless of quantity.
                Item nItem = new Item(item.getItemId(), (byte) 0, quantity, item.getFlag());
                nItem.setExpiration(item.getExpiration());
                nItem.setOwner(item.getOwner());
                nItem.setPet(item.getPet());
                nItem.setGMLog(item.getGMLog());
                nItem.setFamiliarCard(item.getFamiliarCard());
                nItem.setFamiliarid(item.getFamiliarid());
                if (item.getUniqueId() != -1) {
                    nItem.setUniqueId(item.getUniqueId());
                }
                short newSlot = c.getPlayer().getInventory(type).addItem(nItem);
                if (newSlot == -1) {
                    c.announce(InventoryPacket.getInventoryFull());
                    c.announce(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.announce(InventoryPacket.modifyInventory(updateTick, Collections.singletonList(new ModifyInventory(0, nItem))));
                c.announce(MaplePacketCreator.enableActions());
            }
        } else {
            //装备道具的数量只能为 1
            if (quantity == 1) {
                if (enhance) { //是否需要重置潜能 也就是角色刚从地上捡取怪物掉落的装备
                    item = checkEnhanced(item, c.getPlayer());
                }
                if (item.hasSetOnlyId()) {
                    item.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
                }
                short newSlot = c.getPlayer().getInventory(type).addItem(item);
                if (newSlot == -1) {
                    c.announce(InventoryPacket.getInventoryFull());
                    c.announce(InventoryPacket.getShowInventoryFull());
                    return false;
                }
                c.announce(InventoryPacket.modifyInventory(updateTick, Collections.singletonList(new ModifyInventory(0, item))));
                if (ItemConstants.isHarvesting(item.getItemId())) {
                    c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
                }
                c.getPlayer().checkCopyItems();
            } else {
                throw new RuntimeException("玩家[" + c.getPlayer().getName() + "] 获得装备但装备的数量不为1 装备ID: " + item.getItemId());
            }
        }
        if (item.getQuantity() >= 50 && item.getItemId() == 2340000) {
            c.setMonitored(true);
        }
        if (before == 0) {
            switch (item.getItemId()) {
                case AramiaFireWorks.KEG_ID:
                    //c.getPlayer().dropMessage(5, "You have gained a Powder Keg, you can give this in to Aramia of Henesys.");
                    break;
                case AramiaFireWorks.SUN_ID:
                    //c.getPlayer().dropMessage(5, "You have gained a Warm Sun, you can give this in to Maple Tree Hill through @joyce.");
                    break;
                case AramiaFireWorks.DEC_ID:
                    //c.getPlayer().dropMessage(5, "You have gained a Tree Decoration, you can give this in to White Christmas Hill through @joyce.");
                    break;
            }
        }
        c.getPlayer().havePartyQuest(item.getItemId());
        if (show) {
            c.announce(MaplePacketCreator.getShowItemGain(item.getItemId(), item.getQuantity()));
        }
        return true;
    }

    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot) {
        return addItemAndEquip(c, itemId, slot, 0);
    }

    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot, boolean removeItem) {
        return addItemAndEquip(c, itemId, slot, 0, removeItem);
    }

    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot, int state) {
        return addItemAndEquip(c, itemId, slot, state, true);
    }

    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot, int state, boolean removeItem) {
        return addItemAndEquip(c, itemId, slot, null, 0, state, "系统赠送 时间: " + DateUtil.getCurrentDate(), removeItem);
    }

    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot, int state, String gmLog) {
        return addItemAndEquip(c, itemId, slot, null, 0, state, gmLog, true);
    }

    /*
     * 给玩家道具并且自动穿戴
     * c 客户端
     * itemId 道具ID
     * slot 穿戴道具的位置
     * owner 道具的所有者日志
     * period 道具的时间
     * state 道具的未鉴定的状态
     * gmLog 道具的来源日志或者操作日志
     * removeItem 是否删除穿戴位置也有的道具信息
     */
    public static boolean addItemAndEquip(MapleClient c, int itemId, short slot, String owner, long period, int state, String gmLog, boolean removeItem) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        if (!ii.itemExists(itemId) || slot > 0 || !type.equals(MapleInventoryType.EQUIP)) {
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        Item toRemove = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(slot);
        if (toRemove != null) {
            if (removeItem) {
                removeFromSlot(c, MapleInventoryType.EQUIPPED, toRemove.getPosition(), toRemove.getQuantity(), false);
            } else {
                short nextSlot = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                if (nextSlot > -1) {
                    MapleInventoryManipulator.unequip(c, toRemove.getPosition(), nextSlot);
                }
            }
        }
        Item nEquip = ii.getEquipById(itemId);
        if (owner != null) { //设置装备所有者
            nEquip.setOwner(owner);
        }
        if (gmLog != null) { //设置装备获得日志
            nEquip.setGMLog(gmLog);
        }
        if (period > 0) { //设置到期时间
            if (period < 1000) {
                nEquip.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
            } else {
                nEquip.setExpiration(System.currentTimeMillis() + period);
            }
        }
        if (state > 0) { //设置装备潜能
            ii.setPotentialState((Equip) nEquip, state);
        }
        if (nEquip.hasSetOnlyId()) {
            nEquip.setEquipOnlyId(MapleEquipOnlyId.getInstance().getNextEquipOnlyId());
        }
        nEquip.setPosition(slot); //设置装备的位置
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(nEquip);
        c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(0, nEquip))));
        return true;
    }

    private static Item checkEnhanced(Item before, MapleCharacter chr) {
        if (before instanceof Equip) {
            Equip eq = (Equip) before;
            if (eq.getState(false) == 0 && (eq.getUpgradeSlots() >= 1 || eq.getLevel() >= 1) && ItemConstants.canScroll(eq.getItemId()) && Randomizer.nextInt(100) >= 90) { //20% chance of pot?
                eq.renewPotential(false);
                //chr.dropMessage(5, "You have obtained an item with hidden Potential.");
            }
        }
        return before;
    }

    public static boolean checkSpace(MapleClient c, int itemid, int quantity, String owner) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (c.getPlayer() == null || (ii.isPickupRestricted(itemid) && c.getPlayer().haveItem(itemid, 1, true, false)) || (!ii.itemExists(itemid))) {
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        if (quantity <= 0 && !ItemConstants.isRechargable(itemid)) {
            return false;
        }
        MapleInventoryType type = ItemConstants.getInventoryType(itemid);
        if (c == null || c.getPlayer() == null || c.getPlayer().getInventory(type) == null) { //wtf is causing this?
            return false;
        }
        if (!type.equals(MapleInventoryType.EQUIP)) {
            short slotMax = ii.getSlotMax(itemid);
            List<Item> existing = c.getPlayer().getInventory(type).listById(itemid);
            if (!ItemConstants.isRechargable(itemid)) {
                if (existing.size() > 0) { // first update all existing slots to slotMax
                    for (Item eItem : existing) {
                        short oldQ = eItem.getQuantity();
                        if (oldQ < slotMax && owner != null && owner.equals(eItem.getOwner())) {
                            short newQ = (short) Math.min(oldQ + quantity, slotMax);
                            quantity -= (newQ - oldQ);
                        }
                        if (quantity <= 0) {
                            break;
                        }
                    }
                }
            }
            // add new slots if there is still something left
            int numSlotsNeeded;
            if (slotMax > 0 && !ItemConstants.isRechargable(itemid)) {
                numSlotsNeeded = (int) (Math.ceil(((double) quantity) / slotMax));
            } else {
                numSlotsNeeded = 1;
            }
            return !c.getPlayer().getInventory(type).isFull(numSlotsNeeded - 1);
        } else {
            return !c.getPlayer().getInventory(type).isFull();
        }
    }

    public static boolean removeFromSlot(MapleClient c, MapleInventoryType type, short slot, short quantity, boolean fromDrop) {
        return removeFromSlot(c, type, slot, quantity, fromDrop, false);
    }

    public static boolean removeFromSlot(MapleClient c, MapleInventoryType type, short slot, short quantity, boolean fromDrop, boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            /*
             * 5370000 - 黑板（7天权） - 把输入的内容显示在黑板上。可以在自由市场入口使用，但不能在#c市场#中使用。
             * 5370001 - 黑板（1天权） - 把输入的内容显示在黑板上。可以在自由市场入口使用，但不能在#c市场#中使用。
             */
            if ((item.getItemId() == 5370000 || item.getItemId() == 5370001) && c.getPlayer().getChalkboard() != null) {
                c.getPlayer().setChalkboard(null);
            }
            boolean allowZero = consume && ItemConstants.isRechargable(item.getItemId());
            c.getPlayer().getInventory(type).removeItem(slot, quantity, allowZero);
            if (ItemConstants.isHarvesting(item.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            if (item.getQuantity() == 0 && !allowZero) {
                c.announce(InventoryPacket.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(3, item))));
            } else {
                c.announce(InventoryPacket.modifyInventory(fromDrop, Collections.singletonList(new ModifyInventory(1, item))));
            }
            return true;
        }
        return false;
    }

    public static boolean removeById(MapleClient c, MapleInventoryType type, int itemId, int quantity, boolean fromDrop, boolean consume) {
        int remremove = quantity;
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            int theQ = item.getQuantity();
            if (remremove <= theQ && removeFromSlot(c, type, item.getPosition(), (short) remremove, fromDrop, consume)) {
                remremove = 0;
                break;
            } else if (remremove > theQ && removeFromSlot(c, type, item.getPosition(), item.getQuantity(), fromDrop, consume)) {
                remremove -= theQ;
            }
        }
        return remremove <= 0;
    }

    public static boolean removeFromSlot_Lock(MapleClient c, MapleInventoryType type, short slot, short quantity, boolean fromDrop, boolean consume) {
        if (c.getPlayer() == null || c.getPlayer().getInventory(type) == null) {
            return false;
        }
        Item item = c.getPlayer().getInventory(type).getItem(slot);
        if (item != null) {
            return !(ItemFlag.封印.check(item.getFlag()) || ItemFlag.不可交易.check(item.getFlag())) && removeFromSlot(c, type, slot, quantity, fromDrop, consume);
        }
        return false;
    }

    public static boolean removeById_Lock(MapleClient c, MapleInventoryType type, int itemId) {
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (removeFromSlot_Lock(c, type, item.getPosition(), (short) 1, false, false)) {
                return true;
            }
        }
        return false;
    }

    public static void removeAllById(MapleClient c, int itemId, boolean checkEquipped) {
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        for (Item item : c.getPlayer().getInventory(type).listById(itemId)) {
            if (item != null) {
                removeFromSlot(c, type, item.getPosition(), item.getQuantity(), true, false);
            }
        }
        if (checkEquipped) {
            Item ii = c.getPlayer().getInventory(type).findById(itemId);
            if (ii != null) {
                c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeItem(ii.getPosition());
                c.getPlayer().equipChanged();
            }
        }
    }

    public static void removeAll(MapleClient c, MapleInventoryType type) {
        List<ModifyInventory> mods = new ArrayList<>();
        for (Item item : c.getPlayer().getInventory(type).list()) {
            if (item != null) {
                mods.add(new ModifyInventory(3, item));
            }
        }
        if (!mods.isEmpty()) {
            c.announce(InventoryPacket.modifyInventory(false, mods));
        }
        c.getPlayer().getInventory(type).removeAll();
    }

    public static void removeAllByEquipOnlyId(MapleClient c, int equipOnlyId) {
        if (c.getPlayer() == null) {
            return;
        }
        boolean locked = false;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        //背包装备中的复制信息
        List<Item> copyEquipItems = c.getPlayer().getInventory(MapleInventoryType.EQUIP).listByEquipOnlyId(equipOnlyId);
        for (Item item : copyEquipItems) {
            if (item != null) {
                if (!locked) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.封印.getValue();
                    flag |= ItemFlag.不可交易.getValue();
                    flag |= ItemFlag.CRAFTED.getValue();
                    item.setFlag(flag);
                    item.setOwner("复制装备");
                    c.getPlayer().forceUpdateItem(item);
                    c.getPlayer().dropMessage(-11, "在背包中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其锁定。");
                    String msgtext = "玩家 " + c.getPlayer().getName() + " ID: " + c.getPlayer().getId() + " (等级 " + c.getPlayer().getLevel() + ") 地图: " + c.getPlayer().getMapId() + " 在玩家背包中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其锁定。";
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + msgtext));
                    log.warn(msgtext + " 道具唯一ID: " + item.getEquipOnlyId());
                    locked = true;
                } else {
                    removeFromSlot(c, MapleInventoryType.EQUIP, item.getPosition(), item.getQuantity(), true, false);
                    c.getPlayer().dropMessage(-11, "在背包中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其删除。");
                }
            }
        }
        //身上装备中的复制信息
        List<Item> copyEquipedItems = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).listByEquipOnlyId(equipOnlyId);
        for (Item item : copyEquipedItems) {
            if (item != null) {
                if (!locked) {
                    short flag = item.getFlag();
                    flag |= ItemFlag.封印.getValue();
                    flag |= ItemFlag.不可交易.getValue();
                    flag |= ItemFlag.CRAFTED.getValue();
                    item.setFlag(flag);
                    item.setOwner("复制装备");
                    c.getPlayer().forceUpdateItem(item);
                    c.getPlayer().dropMessage(-11, "在穿戴中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其锁定。");
                    String msgtext = "玩家 " + c.getPlayer().getName() + " ID: " + c.getPlayer().getId() + " (等级 " + c.getPlayer().getLevel() + ") 地图: " + c.getPlayer().getMapId() + " 在玩家穿戴中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其锁定。";
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + msgtext));
                    log.warn(msgtext + " 道具唯一ID: " + item.getEquipOnlyId());
                    locked = true;
                } else {
                    removeFromSlot(c, MapleInventoryType.EQUIPPED, item.getPosition(), item.getQuantity(), true, false);
                    c.getPlayer().dropMessage(-11, "在穿戴中发现复制装备[" + ii.getName(item.getItemId()) + "]已经将其删除。");
                    c.getPlayer().equipChanged();
                }
            }
        }
    }

    public static void move(MapleClient c, MapleInventoryType type, short src, short dst) {
        if (src < 0 || dst < 0 || src == dst || type == MapleInventoryType.EQUIPPED) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item source = c.getPlayer().getInventory(type).getItem(src);
        Item initialTarget = c.getPlayer().getInventory(type).getItem(dst);
        if (source == null) {
            c.getPlayer().dropMessage(1, "移动道具失败，找不到移动道具的信息。");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        boolean bag = false, switchSrcDst = false, bothBag = false;
        short eqIndicator = -1;
        List<ModifyInventory> mods = new ArrayList<>();
        if (dst > c.getPlayer().getInventory(type).getSlotLimit()) {
            if ((type == MapleInventoryType.ETC || type == MapleInventoryType.SETUP || type == MapleInventoryType.USE) && dst > 10000 && dst % 10000 != 0) {
                int eSlot = c.getPlayer().getExtendedSlot(MapleInventoryType.ETC.getType(), (dst / 10000) - 1);
                if (eSlot > 0) {
                    MapleStatEffect itemEffect = ii.getItemEffect(eSlot);
                    if (dst % 100 > itemEffect.getSlotCount() || itemEffect.getType() <= 0 || itemEffect.getType() != ii.getBagType(source.getItemId()) && source.getItemId() / 10000 != 301 && source.getItemId() / 10000 != 251) {
                        c.getPlayer().dropMessage(1, "无法将该道具移动到小背包.");
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    } else {
                        eqIndicator = 0;
                        bag = true;
                    }
                } else {
                    c.getPlayer().dropMessage(1, "无法将该道具移动到小背包.");
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
            } else {
                c.getPlayer().dropMessage(1, "无法进行此操作.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (src > c.getPlayer().getInventory(type).getSlotLimit() && (type == MapleInventoryType.ETC || type == MapleInventoryType.SETUP || type == MapleInventoryType.USE) && src > 10000 && src % 10000 != 0) {
            //source should be not null so not much checks are needed
            if (!bag) {
                switchSrcDst = true;
                eqIndicator = 0;
                bag = true;
            } else {
                bothBag = true;
            }
        }
        short olddstQ = -1;
        if (initialTarget != null) {
            olddstQ = initialTarget.getQuantity();
        }
        short oldsrcQ = source.getQuantity();
        short slotMax = ii.getSlotMax(source.getItemId());
        c.getPlayer().getInventory(type).move(src, dst, slotMax);
        if (ItemConstants.isHarvesting(source.getItemId())) {
            c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
        }
        if (!type.equals(MapleInventoryType.EQUIP) && initialTarget != null && initialTarget.getItemId() == source.getItemId() && initialTarget.getOwner().equals(source.getOwner()) && initialTarget.getExpiration() == source.getExpiration() && !ItemConstants.isRechargable(source.getItemId()) && !type.equals(MapleInventoryType.CASH)) {
            if ((olddstQ + oldsrcQ) > slotMax) {
                mods.add(new ModifyInventory(bag && (switchSrcDst || bothBag) ? 6 : 1, source));
                mods.add(new ModifyInventory(bag && (switchSrcDst || bothBag) ? 6 : 1, initialTarget));
            } else {
                mods.add(new ModifyInventory(bag && (switchSrcDst || bothBag) ? 7 : 3, source));
                mods.add(new ModifyInventory(bag && (!switchSrcDst || bothBag) ? 6 : 1, initialTarget));
            }
        } else {
            mods.add(new ModifyInventory(bag ? (bothBag ? 8 : 5) : 2, source, src, eqIndicator, switchSrcDst));
        }
        c.announce(InventoryPacket.modifyInventory(true, mods));
    }

    public static void equip(MapleClient c, short src, short dst) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        PlayerStats statst = chr.getStat();
        Equip source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src);
        if (source == null || source.getDurability() == 0 || ItemConstants.isHarvesting(source.getItemId())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.isAdmin() && ServerConstants.isShowPacket()) {
            chr.dropMessage(5, "穿戴装备  " + source.getItemId() + " src: " + src + " dst: " + dst);
        }
        /*
         * 1002140 - 维泽特帽
         * 1003142 - 霸气·W·能力
         * 1042003 - 维泽特西装
         * 1062007 - 维泽特西裤
         * 1322013 - 维泽特特殊提包
         * 1003824 - 9999帽子
         */
        if (source.getItemId() == 1003142 || source.getItemId() == 1002140 || source.getItemId() == 1042003 || source.getItemId() == 1062007 || source.getItemId() == 1322013 || source.getItemId() == 1003824) {
            if (!chr.isIntern()) {
                chr.dropMessage(1, "无法佩带此物品");
                log.info("[作弊] 非管理员玩家: " + chr.getName() + " 非法穿戴GM装备 " + source.getItemId());
                removeById(c, MapleInventoryType.EQUIP, source.getItemId(), 1, true, false);
                AutobanManager.getInstance().autoban(chr.getClient(), "无理由.");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (!ii.itemExists(source.getItemId())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst > -1200 && dst < -999 && !ItemConstants.is龙龙装备(source.getItemId()) && !ItemConstants.is机甲装备(source.getItemId())) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "穿戴装备 - 1 " + source.getItemId());
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if ((dst > -6000 && dst < -5003 || (dst >= -999 && dst < -99)) && !ii.isCash(source.getItemId()) && dst != -5200) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "穿戴装备 - 2 " + source.getItemId() + " dst: " + dst + " 检测1: " + (dst <= -1200) + " 检测2: " + (dst >= -999 && dst < -99) + " 检测3: " + !ii.isCash(source.getItemId()));
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if ((dst <= -1200 && dst > -1300) && chr.getAndroid() == null) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "穿戴装备 - 3 " + source.getItemId() + " dst: " + dst + " 检测1: " + (dst <= -1200 && dst > -1300) + " 检测2: " + (chr.getAndroid() == null));
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        } else if ((dst <= -1300 && dst > -1306) && !JobConstants.is爆莉萌天使(chr.getJob())) {
            if (chr.isAdmin()) {
                chr.dropMessage(5, "穿戴装备 - 4 " + source.getItemId() + " dst: " + dst + " 检测1: " + (dst <= -1300 && dst > -1306) + " 检测2: " + !JobConstants.is爆莉萌天使(chr.getJob()));
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (!ii.canEquip(source.getItemId(), chr.getLevel(), chr.getJob(), chr.getFame(), statst.getTotalStr(), statst.getTotalDex(), statst.getTotalLuk(), statst.getTotalInt(), chr.getStat().levelBonus)) {
            if (ServerConfig.WORLD_EQUIPCHECKFAME && chr.getFame() < 0) {
                chr.dropMessage(1, "人气度小于0，无法穿戴装备。");
            }
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (ItemConstants.isWeapon(source.getItemId()) && dst != -10 && dst != -11) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -23 && !GameConstants.isMountItemAvailable(source.getItemId(), chr.getJob())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -118 && source.getItemId() / 10000 != 190) { //商城骑宠
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -119 && source.getItemId() / 10000 != 191) { //商城骑宠鞍子
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if ((dst <= -5000 && dst > -5003) && source.getItemId() / 10000 != 120) { //图腾道具
            chr.dropMessage(1, "无法将此装备佩戴这个地方，该位置只能装备图腾道具");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -33 && source.getItemId() / 10000 != 116) { //口袋物品道具
            chr.dropMessage(1, "无法将此装备佩戴这个地方，该位置只能装备口袋物品道具");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -36 && source.getItemId() / 10000 != 118) { //徽章道具
            chr.dropMessage(1, "无法将此装备佩戴这个地方，该位置只能装备徽章道具");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (dst == -38) { //项链扩充的栏位 T072修改 以前为 -37
            MapleQuestStatus stat = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
            if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < System.currentTimeMillis()) {
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (ItemConstants.is双刀副手(source.getItemId()) || source.getItemId() / 10000 == 135) {
            dst = (byte) -10; //盾牌的位置
        }
        if (ItemConstants.is龙龙装备(source.getItemId()) && JobConstants.is龙神(chr.getJob())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (ItemConstants.is机甲装备(source.getItemId()) && JobConstants.is机械师(chr.getJob())) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (ii.isExclusiveEquip(source.getItemId())) { //检测只能佩戴一种的戒指和项链道具
            StructExclusiveEquip exclusive = ii.getExclusiveEquipInfo(source.getItemId());
            if (exclusive != null) {
                List<Integer> theList = chr.getInventory(MapleInventoryType.EQUIPPED).listIds();
                for (Integer i : exclusive.itemIDs) {
                    if (theList.contains(i)) {
                        chr.dropMessage(1, exclusive.msg);
                        c.announce(MaplePacketCreator.enableActions());
                        return;
                    }
                }
            }
        }
        switch (dst) {
            case -6: { // 上衣
                Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                if (top != null && ItemConstants.isOverall(top.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -5: {
                Item top = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -5);
                Item bottom = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -6);
                if (top != null && ItemConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull(bottom != null && ItemConstants.isOverall(source.getItemId()) ? 1 : 0)) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -5, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                if (bottom != null && ItemConstants.isOverall(source.getItemId())) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -6, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -10: { // 盾牌
                Item weapon = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
                if (ItemConstants.is双刀副手(source.getItemId())) {
                    if ((chr.getJob() != 900 && (chr.getJob() < 430 || chr.getJob() > 434)) || weapon == null || !ItemConstants.is双刀主手(weapon.getItemId())) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                } else if (weapon != null && ItemConstants.isTwoHanded(weapon.getItemId(), chr.getJob()) && source.getItemId() / 10000 != 135) { //如果是双手武器 也就是不能佩戴副手装备的
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -11, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
            case -11: { // 武器
                Item shield = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -10);
                if (shield != null && ItemConstants.isTwoHanded(source.getItemId(), chr.getJob()) && shield.getItemId() / 10000 != 135) {
                    if (chr.getInventory(MapleInventoryType.EQUIP).isFull()) {
                        c.announce(InventoryPacket.getInventoryFull());
                        c.announce(InventoryPacket.getShowInventoryFull());
                        return;
                    }
                    unequip(c, (byte) -10, chr.getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                }
                break;
            }
        }
        source = (Equip) chr.getInventory(MapleInventoryType.EQUIP).getItem(src); // Equip
        Equip target = (Equip) chr.getInventory(MapleInventoryType.EQUIPPED).getItem(dst); // Currently equipping
        if (source == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        short flag = source.getFlag();
        boolean itemChanged = false;
        if (ii.isEquipTradeBlock(source.getItemId()) || source.getItemId() / 10000 == 167) { //禁止交易的装备和智能机器人心脏
            if (!ItemFlag.不可交易.check(flag)) {
                flag |= ItemFlag.不可交易.getValue();
                source.setFlag(flag);
                itemChanged = true;
            }
        }
        if (ii.isCash(source.getItemId()) && ItemFlag.可以交换1次.check(flag) && !ItemFlag.不可交易.check(flag)) {
            flag = (short) ((short) (flag | ItemFlag.不可交易.getValue()) - ItemFlag.可以交换1次.getValue());
            source.setFlag(flag);
            itemChanged = true;
        }
        if (source.getItemId() / 10000 == 166) { //智能机器人
            if (source.getAndroid() == null) {
                int uid = MapleInventoryIdentifier.getInstance();
                source.setUniqueId(uid);
                source.setAndroid(MapleAndroid.create(source.getItemId(), uid));
                flag |= ItemFlag.封印.getValue();
                flag |= ItemFlag.不可交易.getValue();
                flag |= ItemFlag.ANDROID_ACTIVATED.getValue();
                source.setFlag(flag);
                itemChanged = true;
            }
            chr.removeAndroid();
            chr.setAndroid(source.getAndroid());
        } else if (chr.getAndroid() != null) {
            if (dst <= -1300) {
                chr.setAndroid(chr.getAndroid()); //respawn it
            } else if (dst <= -1200) { // equip android
                chr.updateAndroid(dst, source.getItemId());
            }
        }
        if (source.getCharmEXP() > 0 && !ItemFlag.装备时获得魅力.check(flag)) {
            chr.getTrait(MapleTraitType.charm).addExp(source.getCharmEXP(), chr);
            source.setCharmEXP((short) 0);
            flag |= ItemFlag.装备时获得魅力.getValue();
            source.setFlag(flag);
            itemChanged = true;
        }
        chr.getInventory(MapleInventoryType.EQUIP).removeSlot(src);
        if (target != null) {
            chr.getInventory(MapleInventoryType.EQUIPPED).removeSlot(dst);
        }
        //装备如果有更新信息 必须在设置新位置之前就加入列表
        List<ModifyInventory> mods = new ArrayList<>();
        if (itemChanged) {
            mods.add(new ModifyInventory(3, source)); //删除道具
            mods.add(new ModifyInventory(0, source)); //获得道具
        }
        source.setPosition(dst);
        chr.getInventory(MapleInventoryType.EQUIPPED).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            chr.getInventory(MapleInventoryType.EQUIP).addFromDB(target);
        }
        if (ItemConstants.isWeapon(source.getItemId())) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.攻击加速);
            chr.cancelEffectFromBuffStat(MapleBuffStat.暗器伤人);
            chr.cancelEffectFromBuffStat(MapleBuffStat.无形箭弩);
            chr.cancelEffectFromBuffStat(MapleBuffStat.属性攻击);
            chr.cancelEffectFromBuffStat(MapleBuffStat.雷鸣冲击);
            chr.cancelEffectFromBuffStat(MapleBuffStat.剑刃之壁);
        }
        if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) { //骑宠道具
            chr.cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
            chr.cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
        } else if (GameConstants.isReverseItem(source.getItemId())) { //穿戴重生装备
            chr.finishAchievement(9);
        } else if (GameConstants.isTimelessItem(source.getItemId())) { //穿戴永恒装备
            chr.finishAchievement(10);
        } else if (ii.getReqLevel(source.getItemId()) >= 140) {
            chr.finishAchievement(41);
        } else if (ii.getReqLevel(source.getItemId()) >= 130) {
            chr.finishAchievement(40);
        } else if (source.getItemId() == 1122017) { //精灵吊坠
            chr.startFairySchedule(true, true);
        }
        if (source.getState(false) >= 17) {
            Map<Integer, SkillEntry> skills = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getPotential4(), source.getPotential5(), source.getPotential6()};
            for (int i : potentials) {
                if (i > 0) {
                    int itemReqLevel = ii.getReqLevel(source.getItemId());
                    List<StructItemOption> potentialInfo = ii.getPotentialInfo(i);
                    StructItemOption pot = potentialInfo.get(Math.min(potentialInfo.size() - 1, (itemReqLevel - 1) / 10));
                    if (pot != null && pot.get("skillID") > 0) {
                        skills.put(PlayerStats.getSkillByJob(pot.get("skillID"), chr.getJob()), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            chr.changeSkillLevel_Skip(skills, true);
        }
        if (source.getSocketState() >= 0x13) {
            Map<Integer, SkillEntry> skills = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        skills.put(PlayerStats.getSkillByJob(soc.get("skillID"), chr.getJob()), new SkillEntry((byte) 1, (byte) 0, -1));
                    }
                }
            }
            chr.changeSkillLevel_Skip(skills, true);
        }
        mods.add(new ModifyInventory(2, source, src)); //移动道具
        c.announce(InventoryPacket.modifyInventory(true, mods));
        if (target != null && chr.isSoulWeapon(target)) {
            chr.unequipSoulWeapon(target);
        }
        if (chr.isSoulWeapon(source)) {
            chr.equipSoulWeapon(source);
        }
        chr.equipChanged();
    }

    public static void unequip(MapleClient c, short src, short dst) {
        if (c.getPlayer() == null) {
            return;
        }
        Equip source = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem(src);
        Equip target = (Equip) c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(dst);
        if (dst < 0 || source == null) {
            return;
        }
        if (target != null && src <= 0) {
            c.announce(InventoryPacket.getInventoryFull());
            return;
        }
        c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).removeSlot(src);
        if (target != null) {
            c.getPlayer().getInventory(MapleInventoryType.EQUIP).removeSlot(dst);
        }
        source.setPosition(dst);
        c.getPlayer().getInventory(MapleInventoryType.EQUIP).addFromDB(source);
        if (target != null) {
            target.setPosition(src);
            c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).addFromDB(target);
        }
        if (ItemConstants.isWeapon(source.getItemId())) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.攻击加速);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.暗器伤人);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.无形箭弩);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.属性攻击);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.雷鸣冲击);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.剑刃之壁);
        } else if (source.getItemId() / 10000 == 190 || source.getItemId() / 10000 == 191) {
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
            c.getPlayer().cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
        } else if (source.getItemId() / 10000 == 166) { //取消安卓
            c.getPlayer().removeAndroid();
        } else if (source.getItemId() / 10000 == 167 && c.getPlayer().getAndroid() != null) { //取消心脏当安卓不为空
            c.announce(AndroidPacket.removeAndroidHeart());
            c.getPlayer().removeAndroid();
        } else if (c.getPlayer().getAndroid() != null) {
            if (src <= -1300) {
                c.getPlayer().setAndroid(c.getPlayer().getAndroid());
            } else if (src <= -1200) {
                c.getPlayer().updateAndroid(src, 0);
            }
        } else if (source.getItemId() == 1122017) {
            c.getPlayer().cancelFairySchedule(true);
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (source.getState(false) >= 17) {
            Map<Integer, SkillEntry> skills = new HashMap<>();
            int[] potentials = {source.getPotential1(), source.getPotential2(), source.getPotential3(), source.getPotential4(), source.getPotential5(), source.getPotential6()};
            for (int i : potentials) {
                if (i > 0) {
                    int itemReqLevel = ii.getReqLevel(source.getItemId());
                    List<StructItemOption> potentialInfo = ii.getPotentialInfo(i);
                    StructItemOption pot = potentialInfo.get(Math.min(potentialInfo.size() - 1, (itemReqLevel - 1) / 10));
                    if (pot != null && pot.get("skillID") > 0) {
                        skills.put(PlayerStats.getSkillByJob(pot.get("skillID"), c.getPlayer().getJob()), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(skills, true);
        }
        if (source.getSocketState() >= 0x13) {
            Map<Integer, SkillEntry> skills = new HashMap<>();
            int[] sockets = {source.getSocket1(), source.getSocket2(), source.getSocket3()};
            for (int i : sockets) {
                if (i > 0) {
                    StructItemOption soc = ii.getSocketInfo(i);
                    if (soc != null && soc.get("skillID") > 0) {
                        skills.put(PlayerStats.getSkillByJob(soc.get("skillID"), c.getPlayer().getJob()), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
            c.getPlayer().changeSkillLevel_Skip(skills, true);
        }
        c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(2, source, src))));
        if (c.getPlayer().isSoulWeapon(source)) {
            c.getPlayer().unequipSoulWeapon(source);
        }
        c.getPlayer().equipChanged();
    }

    public static boolean drop(MapleClient c, MapleInventoryType type, short src, short quantity) {
        return drop(c, type, src, quantity, false);
    }

    public static boolean drop(MapleClient c, MapleInventoryType type, short src, short quantity, boolean npcInduced) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (src < 0) {
            type = MapleInventoryType.EQUIPPED;
        }
        if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
            return false;
        }
        Item source = c.getPlayer().getInventory(type).getItem(src);
        if (quantity < 0 || source == null || (!npcInduced && ItemConstants.isPet(source.getItemId())) || (quantity == 0 && !ItemConstants.isRechargable(source.getItemId())) || c.getPlayer().inPVP()) {
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        /*
         * 设置国庆纪念币无法进行丢弃
         */
        if (!npcInduced && source.getItemId() == 4000463) {
            c.getPlayer().dropMessage(1, "该道具无法丢弃.");
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }

        short flag = source.getFlag();
        if (quantity > source.getQuantity() && !ItemConstants.isRechargable(source.getItemId())) {
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        if (ItemFlag.封印.check(flag) || (quantity != 1 && type == MapleInventoryType.EQUIP)) { // hack
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        Point dropPos = new Point(c.getPlayer().getPosition());
        c.getPlayer().getCheatTracker().checkDrop();
        if (quantity < source.getQuantity() && !ItemConstants.isRechargable(source.getItemId())) {
            Item target = source.copy();
            target.setQuantity(quantity);
            source.setQuantity((short) (source.getQuantity() - quantity));
            c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(1, source)))); //发送更新道具数量的封包
            //log.info("[物品] " + c.getPlayer().getName() + " 丢弃道具: " + target.getItem() + " x " + target.getQuantity() + " - " + ii.getName(target.getItem()) + " 地图: " + c.getPlayer().getMapId());
            if (ii.isDropRestricted(target.getItemId()) || ii.isAccountShared(target.getItemId())) {
                if (ItemFlag.可以交换1次.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.可以交换1次.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else if (ItemFlag.宿命剪刀.check(flag)) {
                    target.setFlag((byte) (flag - ItemFlag.宿命剪刀.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                }
            } else {
                if (ItemConstants.isPet(source.getItemId()) || ItemFlag.不可交易.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), target, dropPos, true, true);
                }
            }
        } else {
            c.getPlayer().getInventory(type).removeSlot(src);
            if (ItemConstants.isHarvesting(source.getItemId())) {
                c.getPlayer().getStat().handleProfessionTool(c.getPlayer());
            }
            c.announce(InventoryPacket.modifyInventory(true, Collections.singletonList(new ModifyInventory(3, source)))); //发送删除道具的封包
            if (src < 0) {
                c.getPlayer().equipChanged();
            }
            //log.info("[物品] " + c.getPlayer().getName() + " 丢弃道具: " + source.getItem() + " x " + source.getQuantity() + " - " + ii.getName(source.getItem()) + " 地图: " + c.getPlayer().getMapId());
            if (ii.isDropRestricted(source.getItemId()) || ii.isAccountShared(source.getItemId())) {
                if (ItemFlag.可以交换1次.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.可以交换1次.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else if (ItemFlag.宿命剪刀.check(flag)) {
                    source.setFlag((byte) (flag - ItemFlag.宿命剪刀.getValue()));
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                } else {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                }
            } else {
                if (ItemConstants.isPet(source.getItemId()) || ItemFlag.不可交易.check(flag)) {
                    c.getPlayer().getMap().disappearingItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos);
                } else {
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), source, dropPos, true, true);
                }
            }
        }
        return true;
    }

//    public static void drop(MapleClient c, MapleInventoryType type, short src, short quantity, boolean npcInduced, ) {
}
