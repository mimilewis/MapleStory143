package handling.channel.handler

import client.MapleCharacter
import client.MapleClient
import client.inventory.Item
import client.inventory.ItemLoader
import client.inventory.MapleInventoryType
import com.alibaba.druid.pool.DruidPooledConnection
import constants.ItemConstants
import database.DatabaseConnection
import handling.world.World
import org.apache.logging.log4j.LogManager
import server.MapleInventoryManipulator
import server.MapleItemInformationProvider
import server.MerchItemPackage
import server.maps.MapleMapObjectType
import server.shops.HiredFisher
import tools.MaplePacketCreator
import tools.data.input.LittleEndianAccessor
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*


/**
 * 免责声明：本模拟器源代码下载自ragezone.com，仅用于技术研究学习，无任何商业行为。
 */
object HiredFisherHandler {

    private val log = LogManager.getLogger("HiredFisher")

    fun UseHiredFisher(slea: LittleEndianAccessor, player: MapleCharacter) {
        val operation = slea.readByte()
        when (operation.toInt()) {
            0 -> {
                check(player.client, true)
            }
            3 -> {
                slea.readInt()
                val hiredFisher = player.map.getMapObject(slea.readInt(), MapleMapObjectType.HIRED_FISHER) as HiredFisher
                hiredFisher.closeShop(true, true)
            }
            4 -> {
                slea.readInt()
                val hiredFisher = player.map.getMapObject(slea.readInt(), MapleMapObjectType.HIRED_FISHER) as HiredFisher
                if (hiredFisher.isHiredFisherItemId() && player.id == hiredFisher.ownerId) {
                    if (!hiredFisher.isDone()) {
                        player.send(MaplePacketCreator.openFishingStorage(0x2B, hiredFisher, null, 0))
                    }
                    return
                }
                player.send(MaplePacketCreator.openFishingStorage(0x1C, hiredFisher, null, 0))
            }
            6 -> {
                slea.readInt()
                val hiredFisher = player.map.getMapObject(slea.readInt(), MapleMapObjectType.HIRED_FISHER) as HiredFisher
                if (!hiredFisher.isHiredFisherItemId() && player.id == hiredFisher.ownerId) {
                    if (hiredFisher.isFull(player)) {
                        player.send(MaplePacketCreator.openFishingStorage(0x1C, hiredFisher, null, 0))
                        return
                    }
                    player.send(MaplePacketCreator.openFishingStorage(0x21, null, null, 0))
                }
            }
            10 -> {
                val hasFisher = World.hasFisher(player.accountID, player.id)
                if (hasFisher) {
                    player.dropMessage(1, "请关闭现有的雇佣钓手.")
                    player.conversation = 0
                    return
                }
                val pack = loadItemFrom_Database(player)
                if (pack == null) {
                    player.dropMessage(1, "发生了未知错误.")
                    return
                }
                if (!checkRetrieve(player, pack)) {
                    player.send(MaplePacketCreator.openFishingStorage(0x21, null, null, 0))
                    return
                }
                if (deletePackage(player.id)) {
                    if (pack.mesos > 0) {
                        player.gainMeso(pack.mesos, false)
//                        System.out.println("[雇佣] " + player.name + " 雇佣钓手取回获得金币: " + w2.nU() + " 时间: " + h.uf())
                        log.info("${player.name}雇佣钓手取回获得金币: ${pack.mesos}")
                    }
                    if (pack.exp > 0) {
                        player.gainExp(pack.exp, true, false, false)
//                        System.out.println("[雇佣] " + player.name + " 雇佣钓手取回获得经验: " + itemPackage.exp + " 时间: " + h.uf())
                        log.info("${player.name}雇佣钓手取回获得经验: ${pack.exp}")
                    }
                    pack.items.forEach { item ->
                        MapleInventoryManipulator.addbyItem(player.client, item, true)
                        log.info("${player.name}雇佣钓手取回获得道具: ${item.itemId} - ${MapleItemInformationProvider.getInstance().getName(item.itemId)} 数量: ${item.quantity}")
                    }
                    player.send(MaplePacketCreator.openFishingStorage(0x23, null, loadItemFrom_Database(player), player.id))
                    return
                }
                player.dropMessage(1, "发生了未知错误.")
                player.send(MaplePacketCreator.openFishingStorage(0x1D, null, loadItemFrom_Database(player), player.id))
            }
            9 -> {
                player.send(MaplePacketCreator.openFishingStorage(0x23, null, loadItemFrom_Database(player), player.id))
            }
        }
    }

    fun check(c: MapleClient, bl2: Boolean): Boolean {
        val player = c.player
        if (c.channelServer.isShutdown) {
            player.dropMessage(1, "服务器即将关闭维护，暂时无法进行。")
            return false
        }
        if (player.map != null && player.map.allowFishing()) {
            val b2 = World.getFisher(player.accountID, player.id)
            if (b2 != null) {
                player.client.announce(MaplePacketCreator.openFishingStorage(0xF, null, null, 0))
            } else {
                if (loadItemFrom_Database(player) == null) {
                    if (bl2) {
                        player.client.announce(MaplePacketCreator.openFishingStorage(0xE, null, null, 0))
                    }
                    return true
                }
                player.client.announce(MaplePacketCreator.openFishingStorage(0x13, null, null, 0))
            }
        }
        return false
    }

    private fun loadItemFrom_Database(player: MapleCharacter): MerchItemPackage? {
        try {
            val loadItems = ItemLoader.钓鱼道具.loadItems(false, player.id)
            if (loadItems.isEmpty()) {
                return null
            }
            val pack = MerchItemPackage()
            load(player, pack)
            if (!loadItems.isEmpty()) {
                val arrayList = loadItems.values.mapTo(ArrayList<Item>()) { it.left }
                pack.items = arrayList
            }
            log.info("${player.name}钓鱼场保管员 道具数量: ${loadItems.size}")
            return pack
        } catch (e: SQLException) {
            log.error("加载钓鱼场保管员道具信息出错" + e)
            return null
        }

    }

    private fun checkRetrieve(player: MapleCharacter, itemPackage: MerchItemPackage): Boolean {
        if (player.meso + itemPackage.mesos < 0) {
            log.error("${player.name}雇佣钓鱼取回道具金币检测错误")
            return false
        }
        var eq: Short = 0
        var use: Short = 0
        var setup: Short = 0
        var etc: Short = 0
        var cash: Short = 0
        for (e2 in itemPackage.items) {
            val m2 = ItemConstants.getInventoryType(e2.itemId)
            if (null != m2) {
                when (m2) {
                    MapleInventoryType.EQUIP -> {
                        eq++
                    }
                    MapleInventoryType.USE -> {
                        use++
                    }
                    MapleInventoryType.SETUP -> {
                        setup++
                    }
                    MapleInventoryType.ETC -> {
                        etc++
                    }
                    MapleInventoryType.CASH -> {
                        cash++
                    }
                    else -> return false
                }
            }
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(e2.itemId) && player.haveItem(e2.itemId, 1)) {
                log.error("${player.name}钓鱼道具是否可以捡取错误")
                return false
            }
        }
        if (player.getInventory(MapleInventoryType.EQUIP).numFreeSlot < eq || player.getInventory(MapleInventoryType.USE).numFreeSlot < use || player.getInventory(MapleInventoryType.SETUP).numFreeSlot < setup || player.getInventory(MapleInventoryType.ETC).numFreeSlot < etc || player.getInventory(MapleInventoryType.CASH).numFreeSlot < cash) {
//            System.out.println("[雇佣] " + player.name + " 雇佣钓鱼取回道具背包空间不够 时间: " + h.uf())
            log.error("${player.name}雇佣钓鱼取回道具背包空间不够")
            return false
        }
        return true
    }

    private fun deletePackage(cid: Int): Boolean {
        var con: DruidPooledConnection? = null
        var ps: PreparedStatement? = null

        try {
            con = DatabaseConnection.getInstance().connection
            ps = con.prepareStatement("DELETE FROM hiredfisher WHERE characterid = ?")
            ps.setInt(1, cid)
            ps.executeUpdate()
            ItemLoader.钓鱼道具.saveItems(null, null, cid)
            return true
        } catch (e: SQLException) {
            log.error("删除雇佣钓手道具信息出错", e)
        } finally {
            con?.close()
            ps?.close()
        }
        return false
    }

    private fun load(player: MapleCharacter, pack: MerchItemPackage) {
        var con: DruidPooledConnection? = null
        var ps: PreparedStatement? = null
        var rs: ResultSet? = null

        try {
            con = DatabaseConnection.getInstance().connection
            ps = con.prepareStatement("SELECT * FROM hiredfisher WHERE characterid = ?")
            ps.setInt(1, player.id)
            rs = ps.executeQuery()
            if (rs.next()) {
                pack.exp = rs.getLong("exp")
                pack.mesos = rs.getLong("mesos")
            }
        } catch (e: SQLException) {
            log.error("获取金币经验信息出错", e)
        } finally {
            con?.close()
            ps?.close()
            rs?.close()
        }
    }
}