package server.cashshop

enum class CashItemFlag(var mark: Int) {

    商品道具ID(0x01),
    数量(0x02),
    价格(0x04),
    红利(0x08),
    优先级(0x10),
    期限(0x20),
    是否禁止购买(0x100),
    性别限制(0x200),
    是否上架(0x400),
    图标类型(0x800),
    termEnd(0x1000),
    人气限制(0x20000),
    等级限制(0x40000),
    销售开始时间(0x80000),
    销售结束时间(0x100000),
    商品分类(0x800000)
    ;

    fun getValue() = mark
}