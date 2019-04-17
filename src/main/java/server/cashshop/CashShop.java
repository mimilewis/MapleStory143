package server.cashshop;

import client.MapleClient;
import client.inventory.*;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.ItemConstants;
import database.DatabaseConnection;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.DateUtil;
import tools.Pair;
import tools.packet.MTSCSPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CashShop implements Serializable {

    private static final long serialVersionUID = 231541893513373579L;
    private final ItemLoader factory = ItemLoader.现金道具;
    private final List<Item> inventory = new ArrayList<>();
    private final List<Integer> uniqueids = new ArrayList<>();
    private int accountId, characterId;

    public CashShop() {
    }

    public CashShop(int accountId, int characterId, int jobType) throws SQLException {
        this.accountId = accountId;
        this.characterId = characterId;
        for (Pair<Item, MapleInventoryType> item : factory.loadItems(false, accountId).values()) {
            inventory.add(item.getLeft());
        }
    }

    public int getItemsSize() {
        return inventory.size();
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public Item findByCashId(int cashId) {
        for (Item item : inventory) {
            if (item.getUniqueId() == cashId) {
                return item;
            }
        }
        return null;
    }

    public void checkExpire(MapleClient c) {
        List<Item> toberemove = new ArrayList<>();
        for (Item item : inventory) {
            if (item != null && !ItemConstants.isPet(item.getItemId()) && item.getExpiration() > 0 && item.getExpiration() < System.currentTimeMillis()) {
                toberemove.add(item);
            }
        }
        if (toberemove.size() > 0) {
            for (Item item : toberemove) {
                removeFromInventory(item);
                c.announce(MTSCSPacket.INSTANCE.cashItemExpired(item.getUniqueId()));
            }
            toberemove.clear();
        }
    }

    public Item toItem(CashItemInfo cItem) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getItemId(), null), "");
    }

    public Item toItem(CashItemInfo cItem, String gift) {
        return toItem(cItem, MapleInventoryManipulator.getUniqueId(cItem.getItemId(), null), gift);
    }

    public Item toItem(CashItemInfo cItem, int uniqueid) {
        return toItem(cItem, uniqueid, "");
    }

    public Item toItem(CashItemInfo cItem, int uniqueid, String gift) {
        if (uniqueid <= 0) {
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        long period = cItem.getPeriod();
        Item ret = null;
        if (ItemConstants.getInventoryType(cItem.getItemId()) == MapleInventoryType.EQUIP) {
            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(cItem.getItemId(), uniqueid);
            if (period > 0) {
                eq.setExpiration(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
            }
            eq.setGMLog("商城购买 " + cItem.getSN() + " 时间 " + DateUtil.getCurrentDate());
            eq.setGiftFrom(gift);
            if (ItemConstants.isEffectRing(cItem.getItemId()) && uniqueid > 0) {
                MapleRing ring = MapleRing.loadFromDb(uniqueid);
                if (ring != null) {
                    eq.setRing(ring);
                }
            }
            ret = eq.copy();
        } else {
            Item item = new Item(cItem.getItemId(), (byte) 0, (short) cItem.getCount(), (byte) 0, uniqueid, (short) 0);
            if (ItemConstants.isPet(cItem.getItemId())) {
                period = 90;
                item.setExpiration(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
            } else if (cItem.getItemId() == 5211047 || cItem.getItemId() == 5360014) { //双倍经验值卡三小时权 双倍爆率卡三小时权
                item.setExpiration(System.currentTimeMillis() + 3 * 60 * 60 * 1000);
            } else if (cItem.getItemId() == 5211060) { //三倍经验卡(2小时)
                item.setExpiration(System.currentTimeMillis() + 2 * 60 * 60 * 1000);
            } else if (period > 0) {
                item.setExpiration(System.currentTimeMillis() + period * 24 * 60 * 60 * 1000);
            } else {
                item.setExpiration(-1);
            }
            item.setGMLog("商城购买 " + cItem.getSN() + " 时间 " + DateUtil.getCurrentDate());
            item.setGiftFrom(gift);
            if (ItemConstants.isPet(cItem.getItemId())) {
                MaplePet pet = MaplePet.createPet(cItem.getItemId(), uniqueid);
                if (pet != null) {
                    item.setPet(pet);
                }
            }
            ret = item.copy();
        }
        return ret;
    }

    public void addToInventory(Item item) {
        inventory.add(item);
    }

    public void removeFromInventory(Item item) {
        inventory.remove(item);
    }

    public void gift(int recipient, String from, String message, int sn) {
        gift(recipient, from, message, sn, 0);
    }

    public void gift(int recipient, String from, String message, int sn, int uniqueid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO `gifts` VALUES (DEFAULT, ?, ?, ?, ?, ?)");
            ps.setInt(1, recipient);
            ps.setString(2, from);
            ps.setString(3, message);
            ps.setInt(4, sn);
            ps.setInt(5, uniqueid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    public List<Pair<Item, String>> loadGifts() {
        List<Pair<Item, String>> gifts = new ArrayList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, characterId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                CashItemInfo cItem = CashItemFactory.getInstance().getItem(rs.getInt("sn"));
                if (cItem == null) {
                    continue;
                }
                Item item = toItem(cItem, rs.getInt("uniqueid"), rs.getString("from"));
                gifts.add(new Pair<>(item, rs.getString("message")));
                uniqueids.add(item.getUniqueId());
                List<Integer> packages = CashItemFactory.getInstance().getPackageItems(cItem.getItemId());
                if (packages != null && packages.size() > 0) {
                    for (int packageItem : packages) {
                        CashItemInfo pack = CashItemFactory.getInstance().getSimpleItem(packageItem);
                        if (pack != null) {
                            addToInventory(toItem(pack, rs.getString("from")));
                        }
                    }
                } else {
                    addToInventory(item);
                }
            }

            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM `gifts` WHERE `recipient` = ?");
            ps.setInt(1, characterId);
            ps.executeUpdate();
            ps.close();
            save(con);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        return gifts;
    }

    public boolean canSendNote(int uniqueid) {
        return uniqueids.contains(uniqueid);
    }

    public void sendedNote(int uniqueid) {
        for (int i = 0; i < uniqueids.size(); i++) {
            if (uniqueids.get(i) == uniqueid) {
                uniqueids.remove(i);
            }
        }
    }

    public void save(DruidPooledConnection con) throws SQLException {
        List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();

        for (Item item : inventory) {
            itemsWithType.add(new Pair<>(item, ItemConstants.getInventoryType(item.getItemId())));
        }

        factory.saveItems(con, itemsWithType, accountId);
    }
}
