package server.shops;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import server.MapleInventoryManipulator;
import tools.packet.PlayerShopPacket;

import java.util.ArrayList;
import java.util.List;

public class MaplePlayerShop extends AbstractPlayerStore {

    private final List<String> bannedList = new ArrayList<>();
    private int boughtnumber = 0;

    public MaplePlayerShop(MapleCharacter owner, int itemId, String desc) {
        super(owner, itemId, desc, "", 6); //以前是3个人 V.100改为6个
    }

    @Override
    public void buy(MapleClient c, int item, short quantity) {
        MaplePlayerShopItem pItem = items.get(item);
        if (pItem.bundles > 0) {
            Item newItem = pItem.item.copy();
            newItem.setQuantity((short) (quantity * newItem.getQuantity()));
            short flag = newItem.getFlag();

            if (ItemFlag.可以交换1次.check(flag)) {
                newItem.setFlag((short) (flag - ItemFlag.可以交换1次.getValue()));
            } else if (ItemFlag.宿命剪刀.check(flag)) {
                newItem.setFlag((short) (flag - ItemFlag.宿命剪刀.getValue()));
            }
            long gainmeso = pItem.price * quantity;
            if (c.getPlayer().getMeso() >= gainmeso) {
                if (getMCOwner().getMeso() + gainmeso > 0 && MapleInventoryManipulator.checkSpace(c, newItem.getItemId(), newItem.getQuantity(), newItem.getOwner()) && MapleInventoryManipulator.addFromDrop(c, newItem, false)) {
                    pItem.bundles -= quantity;
                    bought.add(new BoughtItem(newItem.getItemId(), quantity, gainmeso, c.getPlayer().getName()));
                    c.getPlayer().gainMeso(-gainmeso, false);
                    getMCOwner().gainMeso(gainmeso, false);
                    if (pItem.bundles <= 0) {
                        boughtnumber++;
                        if (boughtnumber == items.size()) {
                            closeShop(true, true);
                            return;
                        }
                    }
                } else {
                    c.getPlayer().dropMessage(1, "Your inventory is full.");
                }
            } else {
                c.getPlayer().dropMessage(1, "You do not have enough mesos.");
            }
            getMCOwner().getClient().announce(PlayerShopPacket.shopItemUpdate(this));
        }
    }

    @Override
    public byte getShopType() {
        return IMaplePlayerShop.PLAYER_SHOP;
    }

    @Override
    public void closeShop(boolean sellout, boolean remove) {
        byte error = (byte) (sellout ? 0x11 : 0x03);
        MapleCharacter owner = getMCOwner();
        removeAllVisitors(error, 1);
        getMap().removeMapObject(this);

        for (MaplePlayerShopItem items : getItems()) {
            if (items.bundles > 0) {
                Item newItem = items.item.copy();
                newItem.setQuantity((short) (items.bundles * newItem.getQuantity()));
                if (MapleInventoryManipulator.addFromDrop(owner.getClient(), newItem, false)) {
                    items.bundles = 0;
                } else {
                    saveItems(); //O_o
                    break;
                }
            }
        }
        owner.setPlayerShop(null);
        update();
        getMCOwner().getClient().announce(PlayerShopPacket.shopErrorMessage(error, 0));
    }

    public void banPlayer(String name) {
        if (!bannedList.contains(name)) {
            bannedList.add(name);
        }
        int slot = -1;
        for (int i = 0; i < getMaxSize(); i++) {
            MapleCharacter chr = getVisitor(i);
            if (chr != null && chr.getName().equals(name)) {
                slot = i + 1;
                break;
            }
        }

        if (slot != -1) {
            for (int i = 0; i < getMaxSize(); i++) {
                MapleCharacter chr = getVisitor(i);
                if (chr != null && chr.getName().equals(name)) {
                    chr.send(PlayerShopPacket.shopErrorMessage(5, slot));
                    chr.setPlayerShop(null);
                    removeVisitor(chr);
                }
            }
        }
    }

    public boolean isBanned(String name) {
        return bannedList.contains(name);
    }
}
