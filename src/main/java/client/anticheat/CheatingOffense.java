package client.anticheat;

public enum CheatingOffense {

    FAST_SUMMON_ATTACK((byte) 5, 6000, 50, (byte) 2),
    FASTATTACK((byte) 5, 6000, 200, (byte) 2),
    FASTATTACK2((byte) 5, 6000, 500, (byte) 2),
    MOVE_MONSTERS((byte) 5, 30000, 500, (byte) 2),
    FAST_HP_MP_REGEN((byte) 5, 20000, 100, (byte) 2),
    SAME_DAMAGE((byte) 5, 180000),
    ATTACK_WITHOUT_GETTING_HIT((byte) 1, 30000, 1200, (byte) 0),
    HIGH_DAMAGE_MAGIC((byte) 5, 30000),
    HIGH_DAMAGE_MAGIC_2((byte) 10, 180000),
    HIGH_DAMAGE((byte) 5, 30000),
    HIGH_DAMAGE_2((byte) 10, 180000),
    EXCEED_DAMAGE_CAP((byte) 5, 60000, 800, (byte) 0),
    ATTACK_FARAWAY_MONSTER((byte) 5, 180000), // NEEDS A SPECIAL FORMULAR!
    ATTACK_FARAWAY_MONSTER_SUMMON((byte) 5, 180000, 200, (byte) 2),
    REGEN_HIGH_HP((byte) 10, 30000, 1000, (byte) 2),
    REGEN_HIGH_MP((byte) 10, 30000, 1000, (byte) 2),
    ITEMVAC_CLIENT((byte) 3, 10000, 100),
    ITEMVAC_SERVER((byte) 2, 10000, 100, (byte) 2),
    PET_ITEMVAC_CLIENT((byte) 3, 10000, 100),
    PET_ITEMVAC_SERVER((byte) 2, 10000, 100, (byte) 2),
    USING_FARAWAY_PORTAL((byte) 1, 60000, 100, (byte) 0),
    FAST_TAKE_DAMAGE((byte) 1, 60000, 100),
    HIGH_AVOID((byte) 5, 180000, 100),
    //FAST_MOVE((byte) 1, 60000),
    HIGH_JUMP((byte) 1, 60000),
    MISMATCHING_BULLETCOUNT((byte) 1, 300000),
    ETC_EXPLOSION((byte) 1, 300000),
    ATTACKING_WHILE_DEAD((byte) 1, 300000),
    USING_UNAVAILABLE_ITEM((byte) 1, 300000),
    FAMING_SELF((byte) 1, 300000), // purely for marker reasons (appears in the database)
    FAMING_UNDER_15((byte) 1, 300000),
    EXPLODING_NONEXISTANT((byte) 1, 300000),
    SUMMON_HACK((byte) 1, 300000),
    SUMMON_HACK_MOBS((byte) 1, 300000),
    ARAN_COMBO_HACK((byte) 1, 600000, 50, (byte) 2),
    HEAL_ATTACKING_UNDEAD((byte) 20, 30000, 100);
    private final byte points;
    private final long validityDuration;
    private final int autobancount;
    private byte bantype = 0; // 0 = Disabled, 1 = Enabled, 2 = DC

    CheatingOffense(byte points, long validityDuration) {
        this(points, validityDuration, -1, (byte) 2);
    }

    CheatingOffense(byte points, long validityDuration, int autobancount) {
        this(points, validityDuration, autobancount, (byte) 1);
    }

    CheatingOffense(byte points, long validityDuration, int autobancount, byte bantype) {
        this.points = points;
        this.validityDuration = validityDuration;
        this.autobancount = autobancount;
        this.bantype = bantype;
    }

    public byte getPoints() {
        return points;
    }

    public long getValidityDuration() {
        return validityDuration;
    }

    public boolean shouldAutoban(int count) {
        return autobancount >= 0 && count >= autobancount;
    }

    public byte getBanType() {
        return bantype;
    }

    public boolean isEnabled() {
        return bantype >= 1;
    }

    public void setEnabled(boolean enabled) {
        bantype = (byte) (enabled ? 1 : 0);
    }
}
