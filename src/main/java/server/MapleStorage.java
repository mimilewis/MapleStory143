package server;

import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import constants.ItemConstants;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.Pair;
import tools.packet.NPCPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleStorage implements Serializable {

    private static final Logger log = LogManager.getLogger(MapleStorage.class.getName());
    private static final long serialVersionUID = 9179541993413738569L;
    private final int storageId; //仓库ID
    private final int accountId; //帐号ID
    private final List<Item> items;
    private final Map<MapleInventoryType, List<Item>> typeItems = new EnumMap<>(MapleInventoryType.class);
    private Long meso;
    private byte slots;
    private int storageNpcId; //仓库的NPCID
    private boolean changed = false; //仓库是否发生改变

    @JsonCreator
    private MapleStorage(@JsonProperty("storageId") int storageId, @JsonProperty("solts") byte slots, @JsonProperty("meso") Long meso, @JsonProperty("accountId") int accountId) {
        this.storageId = storageId;
        this.slots = slots;
        this.meso = meso;
        this.accountId = accountId;
        this.items = new LinkedList<>();
        if (this.slots > 96) {
            this.slots = 96;
            this.changed = true;
        }
    }

    /*
     * 创建1个新的仓库信息
     */
    public static int create(int accountId) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO storages (accountid, slots, meso) VALUES (?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, accountId);
                ps.setInt(2, 4);
                ps.setInt(3, 0);
                ps.executeUpdate();

                int storageid;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        storageid = rs.getInt(1);
                        ps.close();
                        rs.close();
                        return storageid;
                    }
                }
            }
        }
        throw new SQLException("Inserting char failed.");
    }

    /*
     * 从SQL中读取仓库信息 如果没有就创建1个新的仓库信息
     */
    public static MapleStorage loadOrCreateFromDB(int accountId) {
        MapleStorage ret = null;
        int storeId;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM storages WHERE accountid = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                storeId = rs.getInt("storageid");
                ret = new MapleStorage(storeId, rs.getByte("slots"), rs.getLong("meso"), accountId);
                rs.close();
                ps.close();
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.仓库道具.loadItems(false, accountId).values()) {
                    Item item = mit.getLeft();
                    if (item.getItemId() / 1000000 == 1 && ii.isDropRestricted(item.getItemId()) && !ItemFlag.可以交换1次.check(item.getFlag())) {
                        item.addFlag((short) ItemFlag.可以交换1次.getValue());
                    }
                    ret.items.add(item);
                }
            } else {
                storeId = create(accountId);
                ret = new MapleStorage(storeId, (byte) 4, (long) 0, accountId);
                rs.close();
                ps.close();
            }
        } catch (SQLException ex) {
            System.err.println("Error loading storage" + ex);
        }
        return ret;
    }

    /*
     * 保存仓库
     */
    public void saveToDB() {
        saveToDB(null);
    }

    /*
         * 保存仓库
         */
    public void saveToDB(DruidPooledConnection con) {
        if (!changed) {
            return;
        }
        boolean needcolse = false;
        try {
            if (con == null) {
                con = DatabaseConnection.getInstance().getConnection();
            }
            PreparedStatement ps = con.prepareStatement("UPDATE storages SET slots = ?, meso = ? WHERE storageid = ?");
            ps.setInt(1, slots);
            ps.setLong(2, meso);
            ps.setInt(3, storageId);
            ps.executeUpdate();
            ps.close();

            List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();
            for (Item item : items) {
                itemsWithType.add(new Pair<>(item, ItemConstants.getInventoryType(item.getItemId())));
            }
            ItemLoader.仓库道具.saveItems(con, itemsWithType, accountId);
            this.changed = false;
        } catch (SQLException ex) {
            log.error("Error saving storage", ex);
        } finally {
            if (needcolse) {
                try {
                    con.close();
                } catch (SQLException e) {
                    log.error("Error saving storage", e);
                }
            }
        }
    }

    /*
     * 取出道具时获取道具在仓库的信息
     */
    public Item getItem(byte slot) {
        if (slot >= items.size() || slot < 0) {
            return null;
        }
        return items.get(slot);
    }

    /*
     * 取出道具
     */
    public Item takeOut(byte slot) {
        this.changed = true;
        Item ret = items.remove(slot);
        MapleInventoryType type = ItemConstants.getInventoryType(ret.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
        return ret;
    }

    /*
     * 保存仓库道具信息
     */
    public void store(Item item) {
        this.changed = true;
        items.add(item);
        MapleInventoryType type = ItemConstants.getInventoryType(item.getItemId());
        typeItems.put(type, new ArrayList<>(filterItems(type)));
    }

    /*
     * 对仓库道具进行排序
     */
    public void arrange() {
        items.sort((o1, o2) -> {
            if (o1.getItemId() < o2.getItemId()) {
                return -1;
            } else if (o1.getItemId() == o2.getItemId()) {
                return 0;
            } else {
                return 1;
            }
        });
        for (MapleInventoryType type : MapleInventoryType.values()) {
            typeItems.put(type, new ArrayList<>(items));
        }
    }

    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    private List<Item> filterItems(MapleInventoryType type) {
        List<Item> ret = new LinkedList<>();
        for (Item item : items) {
            if (ItemConstants.getInventoryType(item.getItemId()) == type) {
                ret.add(item);
            }
        }
        return ret;
    }

    public byte getSlot(MapleInventoryType type, byte slot) {
        byte ret = 0;
        List<Item> it = typeItems.get(type);
        if (it == null || slot >= it.size() || slot < 0) {
            return -1;
        }
        for (Item item : items) {
            if (item == it.get(slot)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    public void sendStorage(MapleClient c, int npcId) {
        this.storageNpcId = npcId;
        items.sort((o1, o2) -> {
            if (ItemConstants.getInventoryType(o1.getItemId()).getType() < ItemConstants.getInventoryType(o2.getItemId()).getType()) {
                return -1;
            } else if (ItemConstants.getInventoryType(o1.getItemId()) == ItemConstants.getInventoryType(o2.getItemId())) {
                return 0;
            } else {
                return 1;
            }
        });
        for (MapleInventoryType type : MapleInventoryType.values()) {
            typeItems.put(type, new ArrayList<>(items));
        }
        c.announce(NPCPacket.getStorage(npcId, slots, items, meso));
    }

    public void update(MapleClient c) {
        c.announce(NPCPacket.arrangeStorage(slots, items, true));
    }

    public void sendStored(MapleClient c, MapleInventoryType type) {
        c.announce(NPCPacket.storeStorage(slots, type, typeItems.get(type)));
    }

    public void sendTakenOut(MapleClient c, MapleInventoryType type) {
        c.announce(NPCPacket.takeOutStorage(slots, type, typeItems.get(type)));
    }

    public long getMeso() {
        return meso;
    }

    public void setMeso(long meso) {
        if (meso < 0) {
            return;
        }
        this.changed = true;
        this.meso = meso;
    }

    /*
     * 检测仓库道具是否有指定道具ID的重复信息
     */
    public Item findById(int itemId) {
        for (Item item : items) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public void sendMeso(MapleClient c) {
        c.announce(NPCPacket.mesoStorage(slots, meso));
    }

    public boolean isFull() {
        return items.size() >= slots;
    }

    public int getSlots() {
        return slots;
    }

    public void setSlots(byte set) {
        this.changed = true;
        this.slots = set;
    }

    public void increaseSlots(byte gain) {
        this.changed = true;
        this.slots += gain;
    }

    public int getNpcId() {
        return storageNpcId;
    }

    public void close() {
        typeItems.clear();
    }
}
