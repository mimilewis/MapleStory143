package client.inventory;

public enum EquipAdditions {

    elemboost("elemVol", "elemVol", true),
    mobcategory("category", "damage"),
    critical("prob", "damage"),
    boss("prob", "damage"),
    mobdie("hpIncOnMobDie", "mpIncOnMobDie"),
    hpmpchange("hpChangerPerTime", "mpChangerPerTime"),
    skill("id", "level");
    private final String i1, i2;
    private final boolean element;

    EquipAdditions(String i1, String i2) {
        this.i1 = i1;
        this.i2 = i2;
        element = false;
    }

    EquipAdditions(String i1, String i2, boolean element) {
        this.i1 = i1;
        this.i2 = i2;
        this.element = element;
    }

    public static EquipAdditions fromString(String str) {
        for (EquipAdditions s : EquipAdditions.values()) {
            if (s.name().equalsIgnoreCase(str)) {
                return s;
            }
        }
        return null;
    }

    public String getValue1() {
        return i1;
    }

    public String getValue2() {
        return i2;
    }

    public boolean isElement() {
        return element;
    }
}
