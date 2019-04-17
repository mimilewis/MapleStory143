package server;

import client.inventory.Item;

public class MapleDueyActions {

    private String sender = null;
    private Item item = null;
    private int mesos = 0;
    private int quantity = 1;
    private long sentTime;
    private int packageId = 0;

    public MapleDueyActions(int pId, Item item) {
        this.item = item;
        this.quantity = item.getQuantity();
        packageId = pId;
    }

    public MapleDueyActions(int pId) { // meso only package
        this.packageId = pId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String name) {
        sender = name;
    }

    public Item getItem() {
        return item;
    }

    public int getMesos() {
        return mesos;
    }

    public void setMesos(int set) {
        mesos = set;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getPackageId() {
        return packageId;
    }

    public long getSentTime() {
        return sentTime;
    }

    /*
     * public boolean isExpired() { Calendar cal1 = Calendar.getInstance();
     * cal1.set(year, month - 1, day); long diff = System.currentTimeMillis() -
     * cal1.getTimeInMillis(); int diffDays = (int) Math.abs(diff / (24 * 60 *
     * 60 * 1000)); return diffDays > 30; }
     *
     * public long sentTimeInMilliseconds() { Calendar cal =
     * Calendar.getInstance(); cal.set(year, month, day); return
     * cal.getTimeInMillis();
    }
     */
    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }
}
