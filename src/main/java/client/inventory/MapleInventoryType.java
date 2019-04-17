package client.inventory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Matze
 */
public enum MapleInventoryType {

    UNDEFINED(0), //2
    EQUIP(1), //4
    USE(2), //8
    SETUP(3), //10
    ETC(4), //20
    CASH(5), //40
    CASH_PACKAGE(9),
    ELAB(36),
    EQUIPPED(-1);
    final byte type;

    @JsonCreator
    MapleInventoryType(@JsonProperty("type") int type) {
        this.type = (byte) type;
    }

    public static MapleInventoryType getByType(byte type) {
        for (MapleInventoryType l : MapleInventoryType.values()) {
            if (l.getType() == type) {
                return l;
            }
        }
        return null;
    }

    public static MapleInventoryType getByWZName(String name) {
        switch (name) {
            case "Install":
                return SETUP;
            case "Consume":
                return USE;
            case "Etc":
                return ETC;
            case "Eqp":
                return EQUIP;
            case "Cash":
                return CASH;
            case "Pet":
                return CASH;
        }
        return UNDEFINED;
    }

    public byte getType() {
        return type;
    }

    public short getBitfieldEncoding() {
        return (short) (2 << type);
    }
}
