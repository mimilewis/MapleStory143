package server.cashshop

/**
 * @param code mask值
 */
enum class CashIconName(val code: Int) {
    新品(0),
    折扣(1),
    人气(2),
    活动(3),
    限量(4),
    数字限量(0x0b),
    枫叶图标(0x0f)
}
