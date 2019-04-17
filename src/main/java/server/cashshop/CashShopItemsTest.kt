package server.cashshop

import org.apache.logging.log4j.LogManager
import provider.MapleDataProviderFactory
import provider.MapleDataTool
import java.io.File
import java.util.*
import kotlin.Comparator


val bestItems = intArrayOf(30200045, 50000080, 30200066, 50400016, 30100092)
val data = MapleDataProviderFactory.getDataProvider(File(System.getProperty("wzpath") + "/Etc.wz"))!!
val commodities = data.getData("Commodity.img")!!
val itemStats: MutableMap<Int, CashItemInfo> = HashMap() //商城道具状态
val log = LogManager.getLogger()!!

class CashShopItemsTest {

    companion object load {

        fun load() {
            var onSaleSize = 0
            val prioritys = ArrayList<Int>()
            val classs = ArrayList<Int>()
            commodities.children.forEach { field ->
                val SN = MapleDataTool.getIntConvert("SN", field, 0)
                val itemId = MapleDataTool.getIntConvert("ItemId", field, 0)
                val count = MapleDataTool.getIntConvert("Count", field, 1)
                val price = MapleDataTool.getIntConvert("Price", field, 0)
                val originalPrice = MapleDataTool.getIntConvert("originalPrice", field, 0)
                val period = MapleDataTool.getIntConvert("Period", field, 0)
                val gender = MapleDataTool.getIntConvert("Gender", field, 2)
                val onSale = MapleDataTool.getIntConvert("OnSale", field, 0) > 0 || isOnSalePackage(SN) //道具是否出售
                val bonus = MapleDataTool.getIntConvert("Bonus", field, 0) >= 0 //是否有奖金红利？
                val refundable = MapleDataTool.getIntConvert("Refundable", field, 0) == 0 //道具是否可以回购
                val discount = MapleDataTool.getIntConvert("discount", field, 0) >= 0 //是否打折出售
                val priority = MapleDataTool.getIntConvert("Priority", field, 0)
                val Class = MapleDataTool.getIntConvert("Class", field, 0)
                val meso = MapleDataTool.getIntConvert("Meso", field, 0)
                val termStart = MapleDataTool.getIntConvert("termStart", field, 0)
                val termEnd = MapleDataTool.getIntConvert("termEnd", field, 0)
                if (onSale) {
                    onSaleSize++
                    if (priority !in prioritys) {
                        prioritys.add(priority)
                    }
                    if (Class !in classs) {
                        classs.add(Class)
                    }
                    println("SN: $SN ItemID: $itemId priority: $priority Class: $Class")
                }
                //int itemId, int count, int price, int sn, int expire, int gender, boolean sale
                val stats = CashItemInfo(itemId, count, price, originalPrice, meso, SN, period, gender, Class.toByte(), priority.toByte(), termStart, termEnd, onSale, bonus, refundable, discount)
                if (SN > 0) {
                    itemStats.put(SN, stats)
                }
            }
            prioritys.sortWith(Comparator<Int>(Int::compareTo))
            classs.sortWith(Comparator<Int>(Int::compareTo))
            println(prioritys)
            println(classs)
            log.info("共加载 " + itemStats.size + " 个商城道具 有 " + onSaleSize + " 个道具处于出售状态...")
        }

        fun isOnSalePackage(snId: Int): Boolean {
            return snId in 170200002..170200013
        }
    }
}


fun main(args: Array<String>) {
    CashShopItemsTest.load()
    getAllItemSort().forEach { println("SN:${it.key} 是否正在销售:${it.value.onSale()}") }

}

fun getAllItemSort(): ArrayList<Map.Entry<Int, CashItemInfo>> {
    val maps = ArrayList<Map.Entry<Int, CashItemInfo>>(itemStats.entries)
    maps.sortWith(Comparator<Map.Entry<Int, CashItemInfo>> { o1, o2 ->
        if (o1.value.onSale() != o2.value.onSale()) {
            return@Comparator o2.value.onSale().compareTo(o1.value.onSale())
        }
        return@Comparator o1.key.compareTo(o2.key)
    })
    return maps
}