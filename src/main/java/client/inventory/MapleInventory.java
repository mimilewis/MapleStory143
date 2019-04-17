package client.inventory;

import constants.ItemConstants;

import java.io.Serializable;
import java.util.*;

public class MapleInventory implements Iterable<Item>, Serializable {


    private static final long serialVersionUID = -7238868473236710891L;
    private Map<Short, Item> inventory;
    private MapleInventoryType type;
    private short slotLimit = 0;

    /**
     * Creates a new instance of MapleInventory
     */
    public MapleInventory() {
    }

    /**
     * Creates a new instance of MapleInventory
     */
    public MapleInventory(MapleInventoryType type) {
        this.inventory = new LinkedHashMap<>();
        this.type = type;
    }

    public void addSlot(short slot) {
        this.slotLimit += slot;
        if (slotLimit > 128) {
            slotLimit = 128;
        }
    }

    public short getSlotLimit() {
        return slotLimit;
    }

    public void setSlotLimit(short slot) {
        if (slot > 128) {
            slot = 128;
        }
        slotLimit = slot;
    }

    /**
     * Returns the item with its slot id if it exists within the inventory,
     * otherwise null is returned
     */
    public Item findById(int itemId) {
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public Item findByUniqueId(int itemId) {
        for (Item item : inventory.values()) {
            if (item.getUniqueId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public Item findByInventoryId(long onlyId, int itemId) {
        for (Item item : inventory.values()) {
            if (item.getInventoryId() == onlyId && item.getItemId() == itemId) {
                return item;
            }
        }
        return findById(itemId);
    }

    public Item findByEquipOnlyId(long onlyId, int itemId) {
        for (Item item : inventory.values()) {
            if (item.getEquipOnlyId() == onlyId && item.getItemId() == itemId) {
                return item;
            }
        }
        return null;
    }

    public int countById(int itemId) {
        int possesed = 0;
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                possesed += item.getQuantity();
            }
        }
        return possesed;
    }

    public List<Item> listById(int itemId) {
        List<Item> ret = new ArrayList<>();
        for (Item item : inventory.values()) {
            if (item.getItemId() == itemId) {
                ret.add(item);
            }
        }
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    /*
     * 新增函数
     */
    public List<Item> listByEquipOnlyId(int equipOnlyId) {
        List<Item> ret = new ArrayList<>();
        for (Item item : inventory.values()) {
            if (item.getEquipOnlyId() > 0 && item.getEquipOnlyId() == equipOnlyId) {
                ret.add(item);
            }
        }
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    public Collection<Item> list() {
        return inventory.values();
    }

    public List<Item> newList() {
        if (inventory.size() <= 0) {
            return Collections.emptyList();
        }
        return new LinkedList<>(inventory.values());
    }

    public List<Integer> listIds() {
        List<Integer> ret = new ArrayList<>();
        for (Item item : inventory.values()) {
            if (!ret.contains(item.getItemId())) {
                ret.add(item.getItemId());
            }
        }
        if (ret.size() > 1) {
            Collections.sort(ret);
        }
        return ret;
    }

    /**
     * Adds the item to the inventory and returns the assigned slot id
     */
    public short addItem(Item item) {
        short slotId = getNextFreeSlot();
        if (slotId < 0) {
            return -1;
        }
        inventory.put(slotId, item);
        item.setPosition(slotId);
        return slotId;
    }

    public void addFromDB(Item item) {
        if (item.getPosition() < 0 && !type.equals(MapleInventoryType.EQUIPPED)) {
            // This causes a lot of stuck problem, until we are done with position checking
            return;
        }
        if (item.getPosition() > 0 && type.equals(MapleInventoryType.EQUIPPED)) {
            // This causes a lot of stuck problem, until we are done with position checking
            return;
        }
        inventory.put(item.getPosition(), item);
    }

    public void move(short sSlot, short dSlot, short slotMax) {
        Item source = inventory.get(sSlot);
        Item target = inventory.get(dSlot);
        if (source == null) {
            throw new InventoryException("Trying to move empty slot");
        }
        if (target == null) {
            if (dSlot < 0 && !type.equals(MapleInventoryType.EQUIPPED)) {
                // This causes a lot of stuck problem, until we are done with position checking
                return;
            }
            if (dSlot > 0 && type.equals(MapleInventoryType.EQUIPPED)) {
                // This causes a lot of stuck problem, until we are done with position checking
                return;
            }
            source.setPosition(dSlot);
            inventory.put(dSlot, source);
            inventory.remove(sSlot);
        } else if (target.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId()) && target.getOwner().equals(source.getOwner()) && target.getExpiration() == source.getExpiration()) {
            if (type.getType() == MapleInventoryType.EQUIP.getType() || type.getType() == MapleInventoryType.CASH.getType()) {
                swap(target, source);
            } else if (source.getQuantity() + target.getQuantity() > slotMax) {
                source.setQuantity((short) ((source.getQuantity() + target.getQuantity()) - slotMax));
                target.setQuantity(slotMax);
            } else {
                target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
                inventory.remove(sSlot);
            }
        } else {
            swap(target, source);
        }
    }

    private void swap(Item source, Item target) {
        inventory.remove(source.getPosition());
        inventory.remove(target.getPosition());
        short swapPos = source.getPosition();
        source.setPosition(target.getPosition());
        target.setPosition(swapPos);
        inventory.put(source.getPosition(), source);
        inventory.put(target.getPosition(), target);
    }

    public Item getItem(short slot) {
        return inventory.get(slot);
    }

    public void removeItem(short slot) {
        removeItem(slot, (short) 1, false);
    }

    public void removeItem(short slot, short quantity, boolean allowZero) {
        Item item = inventory.get(slot);
        if (item == null) {
            return;
        }
        item.setQuantity((short) (item.getQuantity() - quantity));
        if (item.getQuantity() < 0) {
            item.setQuantity((short) 0);
        }
        if (item.getQuantity() == 0 && !allowZero) {
            removeSlot(slot);
        }
    }

    public void removeSlot(short slot) {
        inventory.remove(slot);
    }

    public void removeAll() {
        inventory.clear();
    }

    public boolean isFull() {
        return inventory.size() >= slotLimit;
    }

    public boolean isFull(int margin) {
        return inventory.size() + margin >= slotLimit;
    }

    /**
     * Returns the next empty slot id, -1 if the inventory is full
     */
    public short getNextFreeSlot() {
        if (isFull()) {
            return -1;
        }
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.containsKey(i)) {
                return i;
            }
        }
        return -1;
    }

    public short getNumFreeSlot() {
        if (isFull()) {
            return 0;
        }
        byte free = 0;
        for (short i = 1; i <= slotLimit; i++) {
            if (!inventory.containsKey(i)) {
                free++;
            }
        }
        return free;
    }

    /*
     * 获取装备中的技能皮肤道具ID信息
     */
    public List<Integer> listSkillSkinIds() {
        List<Integer> ret = new ArrayList<>();
        for (Item item : inventory.values()) {
            if (item.isSkillSkin() && !ret.contains(item.getItemId())) {
                ret.add(item.getItemId());
            }
        }
        return ret;
    }

    public MapleInventoryType getType() {
        return type;
    }

    @Override
    public Iterator<Item> iterator() {
        return Collections.unmodifiableCollection(inventory.values()).iterator();
    }
}
