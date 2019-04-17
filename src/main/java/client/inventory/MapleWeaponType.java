package client.inventory;

public enum MapleWeaponType {

    没有武器(1.43f, 20, 0),
    双头杖(1.2f, 25, 21),
    灵魂手铳(1.7f, 25, 22),
    亡命剑(1.3f, 25, 23),
    能量剑(1.5f, 25, 24),
    驯兽魔法棒(1.34f, 25, 25),
    单手剑(1.2f, 20, 30),
    单手斧(1.2f, 20, 31),
    单手钝器(1.2f, 20, 32),
    短刀(1.3f, 20, 33),
    双刀副手(1.3f, 20, 34),
    特殊副手(2.0f, 15, 35),
    手杖(1.3f, 15, 36),
    短杖(1.2f, 25, 37),
    长杖(1.2f, 25, 38),
    双手斧(1.34f, 20, 40),
    双手剑(1.34f, 20, 41),
    双手钝器(1.34f, 20, 42),
    枪(1.49f, 20, 43),
    矛(1.49f, 20, 44),
    弓(1.3f, 15, 45),
    弩(1.35f, 15, 46),
    拳套(1.75f, 15, 47),
    指节(1.7f, 20, 48),
    短枪(1.5f, 15, 49),
    双弩枪(1.3f, 15, 52),
    手持火炮(1.35f, 15, 53),
    武士刀(1.3f, 15, 54),
    折扇(1.1f, 25, 55),
    大剑(1.49f, 15, 56),
    太刀(1.34f, 15, 57),
    左轮(1.7f, 20, 58);
    private final float damageMultiplier;
    private final int baseMastery;
    private final int weaponType;

    MapleWeaponType(float maxDamageMultiplier, int baseMastery, int weaponType) {
        this.damageMultiplier = maxDamageMultiplier;
        this.baseMastery = baseMastery;
        this.weaponType = weaponType;
    }

    public float getMaxDamageMultiplier() {
        return damageMultiplier;
    }

    public int getBaseMastery() {
        return baseMastery;
    }

    public int getWeaponType() {
        return weaponType;
    }
}
