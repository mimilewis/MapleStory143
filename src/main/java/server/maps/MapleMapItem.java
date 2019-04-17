package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import tools.packet.InventoryPacket;

import java.awt.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMapItem extends MapleMapObject {

    protected final int character_ownerid;
    protected final boolean playerDrop;
    private final ReentrantLock lock = new ReentrantLock();
    protected Item item;
    protected MapleMapObject dropper;
    protected int meso = 0;
    protected int questid = -1;
    protected byte type;
    protected boolean pickedUp = false;
    protected boolean randDrop = false;
    protected long nextExpiry = 0, nextFFA = 0;

    public MapleMapItem(Item item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public MapleMapItem(Item item, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop, int questid) {
        setPosition(position);
        this.item = item;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.type = type;
        this.playerDrop = playerDrop;
        this.questid = questid;
    }

    public MapleMapItem(int meso, Point position, MapleMapObject dropper, MapleCharacter owner, byte type, boolean playerDrop) {
        setPosition(position);
        this.item = null;
        this.dropper = dropper;
        this.character_ownerid = owner.getId();
        this.meso = meso;
        this.type = type;
        this.playerDrop = playerDrop;
    }

    public MapleMapItem(Point position, Item item) {
        setPosition(position);
        this.item = item;
        this.character_ownerid = 0;
        this.type = 2;
        this.playerDrop = false;
        this.randDrop = true;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item z) {
        this.item = z;
    }

    public int getQuest() {
        return questid;
    }

    public int getItemId() {
        if (getMeso() > 0) {
            return meso;
        }
        return item.getItemId();
    }

    public MapleMapObject getDropper() {
        return dropper;
    }

    public int getOwner() {
        return character_ownerid;
    }

    public int getMeso() {
        return meso;
    }

    public boolean isPlayerDrop() {
        return playerDrop;
    }

    public boolean isPickedUp() {
        return pickedUp;
    }

    public void setPickedUp(boolean pickedUp) {
        this.pickedUp = pickedUp;
    }

    public byte getDropType() {
        return type;
    }

    public void setDropType(byte z) {
        this.type = z;
    }

    public boolean isRandDrop() {
        return randDrop;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.ITEM;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (questid <= 0 || (client.getPlayer().getQuestStatus(questid) == 1 && client.getPlayer().needQuestItem(questid, item.getItemId()))) {
            client.announce(InventoryPacket.dropItemFromMapObject(this, this.getPosition(), getTruePosition(), (byte) 2));
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(InventoryPacket.removeItemFromMap(getObjectId(), 1, 0));
    }

    public Lock getLock() {
        return lock;
    }

    public void registerExpire(long time) {
        nextExpiry = System.currentTimeMillis() + time;
    }

    public void registerFFA(long time) {
        nextFFA = System.currentTimeMillis() + time;
    }

    public boolean shouldExpire(long now) {
        return !pickedUp && nextExpiry > 0 && nextExpiry < now;
    }

    public boolean shouldFFA(long now) {
        return !pickedUp && type < 2 && nextFFA > 0 && nextFFA < now;
    }

    public boolean hasFFA() {
        return nextFFA > 0;
    }

    public void expire(MapleMap map) {
        pickedUp = true;
        map.broadcastMessage(InventoryPacket.removeItemFromMap(getObjectId(), 0, 0));
        map.removeMapObject(this);
    }

    public int getState() {
        if (this.getMeso() > 0) {
            return 0;
        }
        if (ItemConstants.getInventoryType(item.getItemId()) != MapleInventoryType.EQUIP) {
            return 0;
        }
        Equip equip = (Equip) item;
        int state = equip.getState(false);
        int addstate = equip.getState(true);
        if (state <= 0 || state >= 17) {
            state = (state -= 16) < 0 ? 0 : state;
        }
        if (addstate <= 0 || addstate >= 17) {
            addstate = (addstate -= 16) < 0 ? 0 : addstate;
        }
        return state > addstate ? state : addstate;
    }
}
