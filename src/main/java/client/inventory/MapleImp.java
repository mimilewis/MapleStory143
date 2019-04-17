package client.inventory;

import java.io.Serializable;

public class MapleImp implements Serializable {

    private static final long serialVersionUID = 91795493413738569L;
    private int itemid;
    private short fullness = 0, closeness = 0;
    private byte state = 1, level = 1;

    public MapleImp() {

    }

    public MapleImp(int itemid) {
        this.itemid = itemid;
    }

    public int getItemId() {
        return itemid;
    }

    public byte getState() {
        return state;
    }

    public void setState(int state) {
        this.state = (byte) state;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = (byte) level;
    }

    public short getCloseness() {
        return closeness;
    }

    public void setCloseness(int closeness) {
        this.closeness = (short) Math.min(100, closeness);
    }

    public short getFullness() {
        return fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = (short) Math.min(1000, fullness);
    }
}
