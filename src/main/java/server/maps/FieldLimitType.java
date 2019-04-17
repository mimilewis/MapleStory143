package server.maps;

public enum FieldLimitType {

    Jump(0x1),
    MovementSkills(0x2),
    SummoningBag(0x04),
    MysticDoor(0x08),
    ChannelSwitch(0x10),
    RegularExpLoss(0x20),
    VipRock(0x40),
    Minigames(0x80),
    Mount(0x200),
    PotionUse(0x1000), //or 0x40000
    Event(0x2000),
    Pet(0x8000), //needs confirmation
    Event2(0x10000),
    DropDown(0x20000),;
    private final int i;

    FieldLimitType(int i) {
        this.i = i;
    }

    public int getValue() {
        return i;
    }

    public boolean check(int fieldlimit) {
        return (fieldlimit & i) == i;
    }
}
