package server;

import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.ItemConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import tools.Pair;
import tools.packet.MTSCSPacket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MTSStorage {

    private static MTSStorage instance;
    private final Map<Integer, MTSCart> idToCart;
    private final AtomicInteger packageId;
    private final Map<Integer, MTSItemInfo> buyNow; //packageid to mtsiteminfo
    private final ReentrantReadWriteLock mutex;
    private final ReentrantReadWriteLock cart_mutex;
    private long lastUpdate = System.currentTimeMillis();
    private boolean end = false;
    //mts_cart is just characterid, itemid
    //mts_items is id/packageid, tab(byte), price, characterid, seller, expiration

    public MTSStorage() {
        idToCart = new LinkedHashMap<>();
        buyNow = new LinkedHashMap<>();
        packageId = new AtomicInteger(1);
        mutex = new ReentrantReadWriteLock();
        cart_mutex = new ReentrantReadWriteLock();
    }

    public static MTSStorage getInstance() {
        return instance;
    }

    public static void load() {
        if (instance == null) {
            instance = new MTSStorage();
            instance.loadBuyNow();
        }
    }

    public boolean check(int packageid) {
        return getSingleItem(packageid) != null;
    }

    public boolean checkCart(int packageid, int charID) {
        MTSItemInfo item = getSingleItem(packageid);
        return item != null && item.getCharacterId() != charID;
    }

    public MTSItemInfo getSingleItem(int packageid) {
        mutex.readLock().lock();
        try {
            return buyNow.get(packageid);
        } finally {
            mutex.readLock().unlock();
        }
    }

    public void addToBuyNow(MTSCart cart, Item item, int price, int cid, String seller, long expiration) {
        int id;
        mutex.writeLock().lock();
        try {
            id = packageId.incrementAndGet();
            buyNow.put(id, new MTSItemInfo(price, item, seller, id, cid, expiration));
        } finally {
            mutex.writeLock().unlock();
        }
        cart.addToNotYetSold(id);
    }

    public boolean removeFromBuyNow(int id, int cidBought, boolean check) {
        Item item = null;
        mutex.writeLock().lock();
        try {
            if (buyNow.containsKey(id)) {
                MTSItemInfo r = buyNow.get(id);
                if (!check || r.getCharacterId() == cidBought) {
                    item = r.getItem();
                    buyNow.remove(id);
                }
            }
        } finally {
            mutex.writeLock().unlock();
        }
        if (item != null) {
            cart_mutex.readLock().lock();
            try {
                for (Entry<Integer, MTSCart> c : idToCart.entrySet()) {
                    c.getValue().removeFromCart(id);
                    c.getValue().removeFromNotYetSold(id);
                    if (c.getKey() == cidBought) {
                        c.getValue().addToInventory(item);
                    }
                }
            } finally {
                cart_mutex.readLock().unlock();
            }
        }
        return item != null;
    }

    private void loadBuyNow() {
        int lastPackage = 0;
        int charId;
        Map<Long, Pair<Item, MapleInventoryType>> items;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM mts_items WHERE tab = 1");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lastPackage = rs.getInt("id");
                charId = rs.getInt("characterid");
                if (!idToCart.containsKey(charId)) {
                    idToCart.put(charId, new MTSCart(charId));
                }
                items = ItemLoader.拍卖道具.loadItems(false, lastPackage);
                if (items != null && items.size() > 0) {
                    for (Pair<Item, MapleInventoryType> i : items.values()) {
                        buyNow.put(lastPackage, new MTSItemInfo(rs.getInt("price"), i.getLeft(), rs.getString("seller"), lastPackage, charId, rs.getLong("expiration")));
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        packageId.set(lastPackage);
    }

    public void saveBuyNow(boolean isShutDown) {
        if (this.end) {
            return;
        }
        this.end = isShutDown;
        if (isShutDown) {
            System.out.println("正在保存 MTS...");
        }
        Map<Integer, ArrayList<Item>> expire = new HashMap<>();
        List<Integer> toRemove = new ArrayList<>();
        long now = System.currentTimeMillis();
        Map<Integer, ArrayList<Pair<Item, MapleInventoryType>>> items = new HashMap<>();
        mutex.writeLock().lock(); //lock wL so rL will also be locked
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM mts_items WHERE tab = 1");
            ps.execute();
            ps.close();
            ps = con.prepareStatement("INSERT INTO mts_items VALUES (?, ?, ?, ?, ?, ?)");
            for (MTSItemInfo m : buyNow.values()) {
                if (now > m.getEndingDate()) {
                    if (!expire.containsKey(m.getCharacterId())) {
                        expire.put(m.getCharacterId(), new ArrayList<>());
                    }
                    expire.get(m.getCharacterId()).add(m.getItem());
                    toRemove.add(m.getId());
                    items.put(m.getId(), null); //destroy from the mtsitems.
                } else {
                    ps.setInt(1, m.getId());
                    ps.setByte(2, (byte) 1);
                    ps.setInt(3, m.getPrice());
                    ps.setInt(4, m.getCharacterId());
                    ps.setString(5, m.getSeller());
                    ps.setLong(6, m.getEndingDate());
                    ps.executeUpdate();
                    if (!items.containsKey(m.getId())) {
                        items.put(m.getId(), new ArrayList<>());
                    }
                    items.get(m.getId()).add(new Pair<>(m.getItem(), ItemConstants.getInventoryType(m.getItem().getItemId())));
                }
            }
            for (int i : toRemove) {
                buyNow.remove(i);
            }
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            mutex.writeLock().unlock();
        }
        if (isShutDown) {
            System.out.println("正在保存 MTS 道具信息...");
        }
        try {
            for (Entry<Integer, ArrayList<Pair<Item, MapleInventoryType>>> ite : items.entrySet()) {
                ItemLoader.拍卖道具.saveItems(null, ite.getValue(), ite.getKey());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (isShutDown) {
            System.out.println("正在保存 MTS carts...");
        }
        cart_mutex.writeLock().lock();
        try {
            for (Entry<Integer, MTSCart> c : idToCart.entrySet()) {
                for (int i : toRemove) {
                    c.getValue().removeFromCart(i);
                    c.getValue().removeFromNotYetSold(i);
                }
                if (expire.containsKey(c.getKey())) {
                    for (Item item : expire.get(c.getKey())) {
                        c.getValue().addToInventory(item);
                    }
                }
                c.getValue().save();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            cart_mutex.writeLock().unlock();
        }
        lastUpdate = System.currentTimeMillis();
    }

    public void checkExpirations() {
        if ((System.currentTimeMillis() - lastUpdate) > 3600000) { //every hour
            saveBuyNow(false);
        }
    }

    public MTSCart getCart(int characterId) {
        MTSCart ret;
        cart_mutex.readLock().lock();
        try {
            ret = idToCart.get(characterId);
        } finally {
            cart_mutex.readLock().unlock();
        }
        if (ret == null) {
            cart_mutex.writeLock().lock();
            try {
                ret = new MTSCart(characterId);
                idToCart.put(characterId, ret);
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                cart_mutex.writeLock().unlock();
            }
        }
        return ret;
    }

    public byte[] getCurrentMTS(MTSCart cart) {
        mutex.readLock().lock();
        try {
            return MTSCSPacket.INSTANCE.sendMTS(getMultiItems(cart.getCurrentView(), cart.getPage()), cart.getTab(), cart.getType(), cart.getPage(), cart.getCurrentView().size());
        } finally {
            mutex.readLock().unlock();
        }
    }

    public byte[] getCurrentNotYetSold(MTSCart cart) {
        mutex.readLock().lock();
        try {
            List<MTSItemInfo> nys = new ArrayList<>();
            MTSItemInfo r;
            List<Integer> nyss = new ArrayList<>(cart.getNotYetSold());
            for (int i : nyss) {
                r = buyNow.get(i);
                if (r == null) {
                    cart.removeFromNotYetSold(i);
                } else {
                    nys.add(r);
                }
            }
            return MTSCSPacket.INSTANCE.getNotYetSoldInv(nys);
        } finally {
            mutex.readLock().unlock();
        }
    }

    public byte[] getCurrentTransfer(MTSCart cart, boolean changed) {
        return MTSCSPacket.INSTANCE.getTransferInventory(cart.getInventory(), changed);
    }

    public List<MTSItemInfo> getMultiItems(List<Integer> items, int pageNumber) {
        List<MTSItemInfo> ret = new ArrayList<>();
        MTSItemInfo r;
        List<Integer> cartt = new ArrayList<>(items);
        if (pageNumber > cartt.size() / 16 + (cartt.size() % 16 > 0 ? 1 : 0)) {
            pageNumber = 0;
        }
        int maxSize = Math.min(cartt.size(), pageNumber * 16 + 16);
        int minSize = Math.min(cartt.size(), pageNumber * 16);
        for (int i = minSize; i < maxSize; i++) { //by packageid
            if (cartt.size() > i) {
                r = buyNow.get(cartt.get(i));
                if (r == null) {
                    items.remove(i);
                    cartt.remove(i);
                } else {
                    ret.add(r);
                }
            } else {
                break;
            }
        }
        return ret;
    }

    public List<Integer> getBuyNow(int type) {
        mutex.readLock().lock();
        try {
            if (type == 0) {
                return new ArrayList<>(buyNow.keySet());
            }
            //page * 16 = FIRST item thats displayed

            List<MTSItemInfo> ret = new ArrayList<>(buyNow.values());
            List<Integer> rett = new ArrayList<>();
            MTSItemInfo r;
            for (MTSItemInfo aRet : ret) {
                r = aRet; //by index
                if (r != null && ItemConstants.getInventoryType(r.getItem().getItemId()).getType() == type) {
                    rett.add(r.getId());
                }
            }
            return rett;
        } finally {
            mutex.readLock().unlock();
        }
    }

    public List<Integer> getSearch(boolean item, String name, int type, int tab) {
        mutex.readLock().lock();
        try {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (tab != 1 || name.length() <= 0) {
                return new ArrayList<>();
            }
            name = name.toLowerCase();
            List<MTSItemInfo> ret = new ArrayList<>(buyNow.values());
            List<Integer> rett = new ArrayList<>();
            MTSItemInfo r;
            for (MTSItemInfo aRet : ret) {
                r = aRet; //by index
                if (r != null && (type == 0 || ItemConstants.getInventoryType(r.getItem().getItemId()).getType() == type)) {
                    String thename = item ? ii.getName(r.getItem().getItemId()) : r.getSeller();
                    if (thename != null && thename.toLowerCase().contains(name)) {
                        rett.add(r.getId());
                    }
                }
            }
            return rett;
        } finally {
            mutex.readLock().unlock();
        }
    }

    public List<MTSItemInfo> getCartItems(MTSCart cart) {
        return getMultiItems(cart.getCart(), cart.getPage());
    }

    public static class MTSItemInfo {

        private final int price;
        private final Item item;
        private final String seller;
        private final int id; //packageid
        private final int cid;
        private final long date;

        public MTSItemInfo(int price, Item item, String seller, int id, int cid, long date) {
            this.item = item;
            this.price = price;
            this.seller = seller;
            this.id = id;
            this.cid = cid;
            this.date = date;
        }

        public Item getItem() {
            return item;
        }

        public int getPrice() {
            return price;
        }

        public int getRealPrice() {
            return price + getTaxes();
        }

        public int getTaxes() {
            return ServerConstants.MTS_BASE + price * ServerConstants.MTS_TAX / 100;
        }

        public int getId() {
            return id;
        }

        public int getCharacterId() {
            return cid;
        }

        public long getEndingDate() {
            return date;
        }

        public String getSeller() {
            return seller;
        }
    }
}
