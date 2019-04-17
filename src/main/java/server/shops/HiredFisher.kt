package server.shops

import client.MapleCharacter
import client.MapleClient
import client.inventory.Item
import client.inventory.ItemLoader
import client.inventory.MapleInventoryType
import com.alibaba.druid.pool.DruidPooledConnection
import constants.GameConstants
import constants.ItemConstants
import database.DatabaseConnection
import handling.cashshop.CashShopServer
import handling.channel.ChannelServer
import handling.world.WorldFindService
import org.apache.logging.log4j.LogManager
import server.MapleInventoryManipulator
import server.MapleItemInformationProvider
import server.RandomRewards
import server.Timer
import server.life.MapleMonsterInformationProvider
import server.maps.MapleMapObjectType
import tools.Randomizer
import tools.packet.PlayerShopPacket
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.*
import java.util.concurrent.ScheduledFuture


/**
 * 免责声明：本模拟器源代码下载自ragezone.com，仅用于技术研究学习，无任何商业行为。
 */
class HiredFisher(owner: MapleCharacter?, itemId: Int, time: Long) : AbstractPlayerStore(owner, itemId, "", "", 6) {

    private val log = LogManager.getLogger()
    var schedule: ScheduledFuture<*>? = null
    var repeat: ScheduledFuture<*>? = null
    var startTime = 0L
    var stopTime = 0L
    var level = 0
    var fh = 0
    var id = 0
    var exp = 0
        set(value) {
            field += value
        }

    init {
        val lastTime = time * 60 * 60 * 1000
        startTime = System.currentTimeMillis()
        stopTime = System.currentTimeMillis() + lastTime
        level = owner?.id!!
        fh = owner.map.footholds.findBelow(owner.truePosition, false)?.id ?: 0
        if (time > 0) {
            schedule = Timer.EtcTimer.getInstance().schedule({
                if (mcOwner != null && mcOwner.hiredFisher == this) {
                    mcOwner.hiredFisher = null
                }
                removeAllVisitors(-1, -1)
                closeShop(true, true)
            }, lastTime)
        }

        repeat = Timer.EtcTimer.getInstance().register({
            doFish()
        }, 60000)

    }

    fun isHiredFisherItemId(): Boolean {
        return getItemId() in 5601000..5601002
    }

    fun doFish() {
        var canFish = false
        var rate = 1.0
        var player: MapleCharacter? = null
        var baitId = 0
        if (!isHiredFisherItemId()) {
            val channel = WorldFindService.getInstance().findChannel(ownerId)
            if (channel > 0) {
                player = ChannelServer.getInstance(channel).playerStorage.getCharacterById(ownerId)
            } else if (channel == -10) {
                player = CashShopServer.getPlayerStorage().getCharacterById(ownerId)
            }
            if (player != null) {
                if (player.getItemQuantity(2300003) > 0) {
                    baitId = 2300003
                    rate = 1.9
                } else if (player.getItemQuantity(2300002) > 0) {
                    baitId = 2300002
                }
                canFish = true
            }
        }
        if (!(isHiredFisherItemId() || baitId != 0 && canFish)) {
            closeShop(true, true)
            if (player != null && baitId == 0) {
                player.dropMessage(1, "鱼饵已经使用完,雇佣钓手已经关闭.")
            }
            return
        }
        if (player != null && !isHiredFisherItemId() && canFish) {
            player.removeItem(baitId, 1)
        }
        val rewardDropEntry = MapleMonsterInformationProvider.getInstance().getReward(mapId, itemId)
        if (Randomizer.nextInt(1000) < 400.0 * rate && rewardDropEntry != null) {
            val useDatabase = true
            val itemid = if (useDatabase) rewardDropEntry.itemId else RandomRewards.getFishingReward()
            val quantity = if (useDatabase) rewardDropEntry.quantity else 1
            when (itemid) {
                0 -> {
                    if (Randomizer.isSuccess(50)) {
                        val mesoreward = Randomizer.rand(15, 75000)
                        setMeso(mesoreward.toLong() + this.getMeso())
                    } else {
                        val total = GameConstants.getExpNeededForLevel(this.level)
                        val expreward = Math.min(Randomizer.nextInt(Math.abs(total / 200).toInt() + 1), 500000)
                        exp = expreward
                    }
                }
                else -> {
                    if (MapleItemInformationProvider.getInstance().itemExists(itemid)) {
                        var add = false
                        for (shopItem in getItems()) {
                            if (ItemConstants.getInventoryType(itemid) != MapleInventoryType.EQUIP && itemid == shopItem.item.itemId) {
                                shopItem.item.quantity = (shopItem.item.quantity + 1).toShort()
                                add = true
                                break
                            }
                        }
                        if (!add) {
                            val item = if (ItemConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) MapleItemInformationProvider.getInstance().getEquipById(itemid) else Item(itemid, 0, quantity.toShort())
                            addItem(MaplePlayerShopItem(item, 0, 0))
                        }
                    }
                }
            }
        }
    }

    fun isFull(player: MapleCharacter): Boolean {
        val hashMap = HashMap<MapleInventoryType, ArrayList<Item>>()
        hashMap.put(MapleInventoryType.EQUIP, ArrayList<Item>())
        hashMap.put(MapleInventoryType.USE, ArrayList<Item>())
        hashMap.put(MapleInventoryType.SETUP, ArrayList<Item>())
        hashMap.put(MapleInventoryType.ETC, ArrayList<Item>())
        hashMap.put(MapleInventoryType.CASH, ArrayList<Item>())
        for (shopitem in getItems()) {
            hashMap.getValue(ItemConstants.getInventoryType(shopitem.item.itemId)).add(shopitem.item)
        }
        hashMap.entries
                .filter { player.getSpace(it.key.type.toInt()) < (it.value as List<*>).size }
                .forEach { return false }
        for (shopitem in getItems()) {
            MapleInventoryManipulator.addFromDrop(player.client, shopitem.item, true)
        }
        getItems().clear()
        return true
    }

    override fun sendSpawnData(client: MapleClient?) {
        client?.announce(PlayerShopPacket.spawnHiredMerchant(this))
    }

    override fun sendDestroyData(client: MapleClient?) {
        client?.announce(PlayerShopPacket.destroyHiredMerchant(ownerId))
    }

    override fun getType(): MapleMapObjectType {
        return MapleMapObjectType.HIRED_FISHER
    }

    override fun getShopType(): Byte {
        return IMaplePlayerShop.HIRED_FISHER
    }

    override fun buy(c: MapleClient?, item: Int, quantity: Short) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun closeShop(saveItems: Boolean, remove: Boolean) {
        try {
            if (saveItems) {
                saveItems()
                items.clear()
            }
            if (remove) {
                ChannelServer.getInstance(channel).removeFisher(this)
                getMap().broadcastMessage(PlayerShopPacket.destroyHiredMerchant(getOwnerId()))
            }
            getMap().removeMapObject(this)
        } finally {
            schedule?.cancel(true)
            repeat?.cancel(true)
        }
    }

    override fun saveItems(): Boolean {
        if (shopType != IMaplePlayerShop.HIRED_FISHER) {
            return false
        }

//        var con: DruidPooledConnection?
//        var ps: PreparedStatement?
//        var rs: ResultSet?
//        try {
//            con = DatabaseConnection.getInstance().connection
//            ps = con.prepareStatement("DELETE FROM hiredfisher WHERE characterid = ?")
//            ps.setInt(1, ownerId)
//            ps.executeUpdate()
//            ps.close()
//
//            ps = con.prepareStatement("INSERT INTO hiredfisher (characterid, Mesos, exp) VALUES (?, ?, ?)")
//            ps.setInt(1, ownerId)
//            ps.setLong(2, meso.get())
//            ps.setInt(3, exp)
//            ps.executeUpdate()
//            rs = ps.generatedKeys
//            if (!rs.next()) {
//                println("[SaveItems] 保存雇佣钓手信息出错 - 1")
//                throw RuntimeException("保存雇佣钓手信息出错.")
//            }
//
//            val ret = java.util.ArrayList<tools.Pair<Item, MapleInventoryType>>()
//            ret.filter { it.left != null && (it.left.quantity > 0 || ItemConstants.isRechargable(it.left.itemId)) }.forEach {
//                val copy = it.left.copy()
//                ret.add(tools.Pair(copy, ItemConstants.getInventoryType(copy.itemId)))
//            }
//            ItemLoader.钓鱼道具.saveItems(con, ret, ownerId)
//            return true
//        } catch (e: SQLException) {
//            log.error("[SaveItems] 保存钓鱼道具信息出错 ", e)
//            return false
//        }

        try {
            DatabaseConnection.getInstance().connection.use { conn ->
                conn.prepareStatement("DELETE FROM hiredfisher WHERE characterid = ?").use { ps ->
                    ps.setInt(1, ownerId)
                    ps.executeUpdate()
                }

                conn.prepareStatement("INSERT INTO hiredfisher (characterid, mesos, exp) VALUES (?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS).use { ps ->
                    ps.setInt(1, ownerId)
                    ps.setLong(2, meso.get())
                    ps.setInt(3, exp)
                    ps.executeUpdate()
                    ps.generatedKeys.use { rs ->
                        if (!rs.next()) {
                            println("[SaveItems] 保存雇佣钓手信息出错 - 1")
                            throw RuntimeException("保存雇佣钓手信息出错.")
                        }
                    }
                }

                val ret = java.util.ArrayList<tools.Pair<Item, MapleInventoryType>>()
                items.filter { it.item != null && (it.item.quantity > 0 || ItemConstants.isRechargable(it.item.itemId)) }.forEach {
                    val copy = it.item.copy()
                    ret.add(tools.Pair(copy, ItemConstants.getInventoryType(copy.itemId)))
                }
                ItemLoader.钓鱼道具.saveItems(null, ret, ownerId)
                return true
            }
        } catch (e: Exception) {
            log.error("保存钓鱼道具信息出错", e)
            return false
        }
    }

    fun isDone(): Boolean {
        return System.currentTimeMillis() >= stopTime
    }
}


private inline fun <R> DruidPooledConnection.use(block: (DruidPooledConnection) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

private inline fun <R> PreparedStatement.use(block: (PreparedStatement) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

private inline fun <R> ResultSet.use(block: (ResultSet) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}