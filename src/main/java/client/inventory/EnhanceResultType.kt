package client.inventory

enum class EnhanceResultType constructor(val value: Short) {
    NO_DESTROY(0x1),
    UPGRADE_TIER(0x2),
    SCROLL_SUCCESS(0x4),
    EQUIP_MARK(0x80);

    fun check(n: Int): Boolean {
        return n and value.toInt() != 0x0
    }
}