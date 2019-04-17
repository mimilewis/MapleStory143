package server.cashshop

enum class CashShopType(val subtype: Array<String>) {
    首页(arrayOf<String>()),
    活动(arrayOf("限量出售", "积分商城")),
    强化(arrayOf("强化", "卷轴")),
    游戏(arrayOf("便捷", "个人商店", "通讯物品", "喜庆商品", "钓鱼", "背包扩充")),
    搭配(arrayOf("武器", "帽子", "披风", "长袍", "上衣", "裙裤", "鞋子", "手套", "饰品", "眼饰", "效果")),
    美容(arrayOf("发型", "整容", "其他", "表情")),
    宠物(arrayOf("宠物", "宠物装备", "宠物食品", "宠物技能")),
    礼包(arrayOf("游戏", "装扮类礼包", "宠物礼包")),
    转蛋机(arrayOf<String>()),
    搜索结果(arrayOf<String>());
}