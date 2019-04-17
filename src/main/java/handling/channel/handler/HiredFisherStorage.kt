package handling.channel.handler

import org.apache.logging.log4j.LogManager
import server.shops.HiredFisher
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * 免责声明：本模拟器源代码下载自ragezone.com，仅用于技术研究学习，无任何商业行为。
 */
class HiredFisherStorage(val channel: Int) {

    private val log = LogManager.getLogger()
    private var running_FisherID = 0
    private val fisherLock = ReentrantReadWriteLock(true) //雇佣商店多线程操作
    private val fReadLock = fisherLock.readLock()  // 读锁
    private val fWriteLock = fisherLock.writeLock() // 写锁
    private val hiredFishers = HashMap<Int, HiredFisher>()

    fun saveAllFisher(): Unit {
        fWriteLock.lock()
        try {
            hiredFishers.forEach { it.value.saveItems() }
        } finally {
            fWriteLock.unlock()
        }
    }

    fun closeAllFisher(): Unit {
        var ret = 0
        val start = System.currentTimeMillis()
        fWriteLock.lock()
        try {
            hiredFishers.forEach {
                it.value.closeShop(true, false)
                ret++
            }
        } catch (e: Exception) {
            log.error("关闭雇佣钓手出现错误...", e)
        } finally {
            fWriteLock.unlock()
        }
        println("频道 " + this.channel + " 共保存雇佣钓手: " + ret + " | 耗时: " + (System.currentTimeMillis() - start) + " 毫秒.")
    }


    fun containsFisher(accId: Int, chrId: Int): Boolean {
        var contains = false
        fReadLock.lock()
        try {
            for (hf in hiredFishers.values) {
                if (hf.ownerAccId == accId && hf.ownerId == chrId) {
                    contains = true
                    break
                }
            }
        } finally {
            fReadLock.unlock()
        }
        return contains
    }

    fun getHiredFisher(accId: Int, chrId: Int): HiredFisher? {
        fReadLock.lock()
        try {
            hiredFishers.values
                    .filter { it.ownerAccId == accId && it.ownerId == chrId }
                    .forEach { return it }
        } finally {
            fReadLock.unlock()
        }
        return null
    }

    fun addFisher(hiredFisher: HiredFisher): Int {
        fWriteLock.lock()
        try {
            running_FisherID++
            hiredFishers.put(running_FisherID, hiredFisher)
            return running_FisherID
        } finally {
            fWriteLock.unlock()
        }
    }

    fun removeFisher(hFisher: HiredFisher) {
        fWriteLock.lock()
        try {
            hiredFishers.remove(hFisher.id)
        } finally {
            fWriteLock.unlock()
        }
    }
}