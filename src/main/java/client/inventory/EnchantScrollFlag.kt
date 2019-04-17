package client.inventory

enum class EnchantScrollFlag constructor(val value: Int) {
    NULL(0),
    物攻(1),
    魔攻(2),
    力量(4),
    敏捷(8),
    智力(0x10),
    运气(0x20),
    物防(0x40),
    魔防(0x80),
    Hp(0x100),
    Mp(0x200),
    命中(0x400),
    回避(0x800),
    跳跃(0x1000),
    速度(0x2000);

    fun check(mask: Int): Boolean {
        return mask and value != 0x0
    }
}