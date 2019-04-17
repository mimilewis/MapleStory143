package server;

import client.inventory.Item;

import java.util.ArrayList;
import java.util.List;

public class MerchItemPackage {

    private long exp;
    private long mesos = 0;
    private List<Item> items = new ArrayList<>();

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public long getMesos() {
        return mesos;
    }

    public void setMesos(long set) {
        mesos = set;
    }
}
