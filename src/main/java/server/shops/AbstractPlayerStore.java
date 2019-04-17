package server.shops;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import tools.Pair;
import tools.packet.PlayerShopPacket;

import java.lang.ref.WeakReference;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractPlayerStore extends MapleMapObject implements IMaplePlayerShop {

    private static final Logger log = LogManager.getLogger(AbstractPlayerStore.class);
    protected final String ownerName;
    protected final String pass;
    protected final int ownerId;
    protected final int owneraccount;
    protected final int itemId;
    protected final int channel;
    protected final int map;
    protected final AtomicLong meso = new AtomicLong(0);
    protected final WeakReference<MapleCharacter>[] chrs;
    protected final Map<String, VisitorInfo> visitorsList = new HashMap<>();
    protected final List<BoughtItem> bought = new LinkedList<>();
    protected final List<MaplePlayerShopItem> items = new LinkedList<>();
    private final List<Pair<String, Byte>> messages = new LinkedList<>();
    protected boolean open = false, available = false;
    protected String des;

    public AbstractPlayerStore(MapleCharacter owner, int itemId, String desc, String pass, int slots) {
        this.setPosition(owner.getTruePosition());
        this.ownerName = owner.getName();
        this.ownerId = owner.getId();
        this.owneraccount = owner.getAccountID();
        this.itemId = itemId;
        this.des = desc;
        this.pass = pass;
        this.map = owner.getMapId();
        this.channel = owner.getClient().getChannel();
        chrs = new WeakReference[slots];
        for (int i = 0; i < chrs.length; i++) {
            chrs[i] = new WeakReference<>(null);
        }
        this.visitorsList.clear();
    }

    @Override
    public int getMaxSize() {
        return chrs.length + 1;
    }

    @Override
    public int getSize() {
        return getFreeSlot() == -1 ? getMaxSize() : getFreeSlot();
    }

    @Override
    public void broadcastToVisitors(byte[] packet) {
        broadcastToVisitors(packet, true);
    }

    public void broadcastToVisitors(byte[] packet, boolean owner) {
        for (WeakReference<MapleCharacter> chr : chrs) {
            if (chr != null && chr.get() != null) {
                chr.get().getClient().announce(packet);
            }
        }
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT && owner && getMCOwner() != null) {
            getMCOwner().getClient().announce(packet);
        }
    }

    public void broadcastToVisitors(byte[] packet, int exception) {
        for (WeakReference<MapleCharacter> chr : chrs) {
            if (chr != null && chr.get() != null && getVisitorSlot(chr.get()) != exception) {
                chr.get().getClient().announce(packet);
            }
        }
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT && getMCOwner() != null) { // && exception != ownerId
            getMCOwner().getClient().announce(packet);
        }
    }

    @Override
    public long getMeso() {
        return meso.get();
    }

    @Override
    public void setMeso(long meso) {
        this.meso.set(meso);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void setOpen(boolean open) {
        this.open = open;
    }

    @Override
    public boolean saveItems() {
        if (getShopType() != IMaplePlayerShop.HIRED_MERCHANT) { //hired merch only
            return false;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM hiredmerch WHERE characterid = ?"); //蛋疼就是这个地方写成 accountid = ? OR characterid = ? 导致同一帐号下的角色开店钱不见
            ps.setInt(1, ownerId);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("INSERT INTO hiredmerch (characterid, accountid, Mesos, map, channel, time) VALUES (?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, ownerId);
            ps.setInt(2, owneraccount);
            ps.setLong(3, meso.get());
            ps.setInt(4, map);
            ps.setInt(5, channel);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                rs.close();
                ps.close();
                log.info("[SaveItems] 保存雇佣商店信息出错 - 1");
                throw new RuntimeException("保存雇佣商店信息出错.");
            }
            rs.close();
            ps.close();
            //log.info("[雇佣] 保存雇佣商店 角色ID: " + ownerId + " 金币: " + meso.get() + " 时间: " + DateUtil.getCurrentDate());
            List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();
            Item item;
            for (MaplePlayerShopItem pItems : items) {
                if (pItems.item == null || pItems.bundles <= 0) {
                    continue;
                }
                if (pItems.item.getQuantity() <= 0 && !ItemConstants.isRechargable(pItems.item.getItemId())) {
                    continue;
                }
                item = pItems.item.copy();
                item.setQuantity((short) (item.getQuantity() * pItems.bundles));
                itemsWithType.add(new Pair<>(item, ItemConstants.getInventoryType(item.getItemId())));
            }
            ItemLoader.雇佣道具.saveItems(con, itemsWithType, this.ownerId);
            return true;
        } catch (SQLException se) {
            log.info("[SaveItems] 保存雇佣商店信息出错 - 2 " + se);
        }
        return false;
    }

    public MapleCharacter getVisitor(int num) {
        return chrs[num].get();
    }

    @Override
    public void update() {
        if (isAvailable()) {
            if (getShopType() == IMaplePlayerShop.HIRED_MERCHANT) {
                getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant((HiredMerchant) this));
            } else if (getMCOwner() != null) {
                getMap().broadcastMessage(PlayerShopPacket.sendPlayerBox(getMCOwner()));
            }
        }
    }

    @Override
    public void addVisitor(MapleCharacter visitor) {
        int i = getFreeSlot();
        if (i > 0) {
            if (getShopType() >= 3) {
                broadcastToVisitors(PlayerShopPacket.getMiniGameNewVisitor(visitor, i, (MapleMiniGame) this));
            } else {
                broadcastToVisitors(PlayerShopPacket.playerInterVisitorAdd(visitor, i));
            }
            chrs[i - 1] = new WeakReference<>(visitor);
            updateVisitorsList(visitor, false);
            if (i == 6) { //以前是3人V.100改为6人 好像只是雇佣商店
                update();
            }
        }
    }

    /*
     * 角色是否在访问名单
     */
    public boolean isInVisitorsList(String visitorName) {
        return visitorsList.containsKey(visitorName);
    }

    /*
     * 添加访问名单
     * 访问者为空 且 不是商店的开启者 且 不是GM角色
     */
    public void updateVisitorsList(MapleCharacter visitor, boolean leave) {
        if (visitor != null && !isOwner(visitor) && !visitor.isGM()) {
            if (visitorsList.containsKey(visitor.getName())) {
                if (leave) {
                    visitorsList.get(visitor.getName()).updateInTime();
                } else {
                    visitorsList.get(visitor.getName()).updateStartTime();
                }
            } else {
                visitorsList.put(visitor.getName(), new VisitorInfo());
            }
        }
    }

    /*
     * 删除访问名单
     */
    public void removeVisitorsList(String visitorName) {
        if (visitorsList.containsKey(visitorName)) {
            visitorsList.remove(visitorName);
        }
    }

    @Override
    public void removeVisitor(MapleCharacter visitor) {
        byte slot = getVisitorSlot(visitor);
        boolean shouldUpdate = getFreeSlot() == -1;
        if (slot > 0) {
            broadcastToVisitors(PlayerShopPacket.playerInterVisitorLeave(slot), slot);
            chrs[slot - 1] = new WeakReference<>(null);
            if (shouldUpdate) {
                update();
            }
            updateVisitorsList(visitor, true);
        }
    }

    @Override
    public byte getVisitorSlot(MapleCharacter visitor) {
        for (byte i = 0; i < chrs.length; i++) {
            if (chrs[i] != null && chrs[i].get() != null && chrs[i].get().getId() == visitor.getId()) {
                return (byte) (i + 1);
            }
        }
        if (visitor.getId() == ownerId) { //can visit own store in merch, otherwise not.
            return 0;
        }
        return -1;
    }

    @Override
    public void removeAllVisitors(int error, int type) {
        for (int i = 0; i < chrs.length; i++) {
            MapleCharacter visitor = getVisitor(i);
            if (visitor != null) {
                if (type != -1) {
                    visitor.getClient().announce(PlayerShopPacket.shopErrorMessage(error, i + 1));
                }
                broadcastToVisitors(PlayerShopPacket.playerInterVisitorLeave(getVisitorSlot(visitor)), getVisitorSlot(visitor));
                visitor.setPlayerShop(null);
                chrs[i] = new WeakReference<>(null);
                updateVisitorsList(visitor, true);
            }
        }
        update();
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public int getOwnerId() {
        return ownerId;
    }

    @Override
    public int getOwnerAccId() {
        return owneraccount;
    }

    @Override
    public String getDescription() {
        if (des == null) {
            return "";
        }
        return des;
    }

    @Override
    public void setDescription(String desc) {
        if (this.des.equalsIgnoreCase(desc)) {
            return;
        }
        this.des = desc;
        if (isAvailable() && getShopType() == IMaplePlayerShop.HIRED_MERCHANT) {
            getMap().broadcastMessage(PlayerShopPacket.updateHiredMerchant((HiredMerchant) this, false));
        }
    }

    @Override
    public List<Pair<Byte, MapleCharacter>> getVisitors() {
        List<Pair<Byte, MapleCharacter>> chrz = new LinkedList<>();
        for (byte i = 0; i < chrs.length; i++) { //include owner or no
            if (chrs[i] != null && chrs[i].get() != null) {
                chrz.add(new Pair<>((byte) (i + 1), chrs[i].get()));
            }
        }
        return chrz;
    }

    @Override
    public List<MaplePlayerShopItem> getItems() {
        return items;
    }

    @Override
    public void addItem(MaplePlayerShopItem item) {
        items.add(item);
    }

    @Override
    public boolean removeItem(int item) {
        return false;
    }

    @Override
    public void removeFromSlot(int slot) {
        items.remove(slot);
    }

    @Override
    public byte getFreeSlot() {
        for (byte i = 0; i < chrs.length; i++) {
            if (chrs[i] == null || chrs[i].get() == null) {
                return (byte) (i + 1);
            }
        }
        return -1;
    }

    @Override
    public int getItemId() {
        return itemId;
    }

    @Override
    public boolean isOwner(MapleCharacter chr) {
        return chr.getId() == ownerId && chr.getName().equals(ownerName);
    }

    @Override
    public String getPassword() {
        if (pass == null) {
            return "";
        }
        return pass;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
    }

    @Override
    public void sendSpawnData(MapleClient client) {
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SHOP;
    }

    public MapleCharacter getMCOwnerWorld() {
        int ourChannel = WorldFindService.getInstance().findChannel(ownerId);
        if (ourChannel <= 0) {
            return null;
        }
        return ChannelServer.getInstance(ourChannel).getPlayerStorage().getCharacterById(ownerId);
    }

    public MapleCharacter getMCOwnerChannel() {
        return ChannelServer.getInstance(channel).getPlayerStorage().getCharacterById(ownerId);
    }

    public MapleCharacter getMCOwner() {
        return getMap().getCharacterById(ownerId);
    }

    public MapleMap getMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(map);
    }

    @Override
    public int getGameType() {
        if (getShopType() == IMaplePlayerShop.HIRED_MERCHANT) { //雇佣商店
            return 6;
        } else if (getShopType() == IMaplePlayerShop.PLAYER_SHOP) { //玩家自己贩卖的商店
            return 5;
        } else if (getShopType() == IMaplePlayerShop.OMOK) { //五子棋
            return 1;
        } else if (getShopType() == IMaplePlayerShop.MATCH_CARD) { //记忆大考验
            return 2;
        }
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public void setAvailable(boolean b) {
        this.available = b;
    }

    @Override
    public List<BoughtItem> getBoughtItems() {
        return bought;
    }

    @Override
    public List<Pair<String, Byte>> getMessages() {
        return messages;
    }

    @Override
    public int getMapId() {
        return map;
    }

    @Override
    public int getChannel() {
        return channel;
    }

    public static final class BoughtItem {

        public final int id;
        public final int quantity;
        public final long totalPrice;
        public final String buyer;

        public BoughtItem(int id, int quantity, long totalPrice, String buyer) {
            this.id = id;
            this.quantity = quantity;
            this.totalPrice = totalPrice;
            this.buyer = buyer;
        }
    }

    public static final class VisitorInfo {

        public int inTime; //停留的时间
        public long startTime; //访问时间

        public VisitorInfo() {
            this.inTime = 0;
            this.startTime = System.currentTimeMillis();
        }

        public void updateInTime() {
            int time = (int) (System.currentTimeMillis() - startTime);
            if (time > 0) {
                this.inTime += time;
            }
            //System.out.println("雇佣商店角色访问停留时间  当前: " + time + " 毫秒 总计: " + inTime);
        }

        public int getInTime() {
            return inTime;
        }

        public void updateStartTime() {
            this.startTime = System.currentTimeMillis();
        }
    }
}
