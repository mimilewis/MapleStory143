package constants;

public enum ScriptType {
    NPC(-1),
    QUEST_START(0),
    QUEST_END(1),
    ITEM(2),
    ON_FIRST_USER_ENTER(-1),
    ON_USER_ENTER(-1),
    PORTAL(),
    EVENT(),
    REACTOR();

    private final byte value;

    ScriptType() {
        this.value = -2;
    }

    ScriptType(int value) {
        this.value = (byte) value;
    }

    public int getValue() {
        return value;
    }

}
