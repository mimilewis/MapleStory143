package server.console.groups.datamanage

import org.junit.Test
import server.MapleItemInformationProvider

class CashShopPaneTest {

    @Test
    fun test(): Unit {
//        MapleItemInformationProvider.getInstance().allItemNames.keys.forEach {
//            try {
//                print("$it -> ")
//                println(MapleItemInformationProvider.getInstance().getInLinkID(it.toInt()))
//            } catch (e: Exception) {
//                println("$it 进入死循环了")
//            }
//        }
        println(MapleItemInformationProvider.getInstance().getInLinkID(1702109))
    }
}