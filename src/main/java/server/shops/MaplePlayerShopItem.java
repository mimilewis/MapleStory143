package server.shops;

import client.inventory.Item;

public class MaplePlayerShopItem {

    public final Item item;
    public final long price;
    public short bundles;

    public MaplePlayerShopItem(Item item, short bundles, long price) {
        this.item = item;
        this.bundles = bundles;
        this.price = price;
    }

    public Item getItem() {
        return item;
    }

    public short getBundles() {
        return bundles;
    }

    public long getPrice() {
        return price;
    }
}
