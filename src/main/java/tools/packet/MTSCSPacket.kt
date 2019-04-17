package tools.packet

import client.MapleCharacter
import client.MapleClient
import client.MapleStat
import client.inventory.Item
import client.inventory.MapleInventoryType
import client.inventory.MaplePotionPot
import configs.CSInfoConfig
import constants.ItemConstants
import constants.ServerConstants
import handling.opcode.CashShopOpcode
import handling.opcode.EffectOpcode
import handling.opcode.SendPacketOpcode
import server.MTSStorage
import server.cashshop.CashItemFactory
import server.cashshop.CashItemInfo
import tools.Pair
import tools.data.output.MaplePacketLittleEndianWriter
import java.sql.ResultSet
import java.sql.SQLException
import java.text.SimpleDateFormat
import java.util.*


object MTSCSPacket {

    fun warpchartoCS(c: MapleClient): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_CHAR.value)
        PacketHelper.addCharacterInfo(mplew, c.player)

        return mplew.packet
    }

    fun warpCS(custom: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPEN.value)
        if (!custom) {
            mplew.writeHexString(CSInfoConfig.CASH_CASHSHOPPACK)
        } else {
            val blockitems = emptyArray<Int>()
            val customPackages = emptyMap<Int, CashItemInfo.CashModInfo>()

            mplew.writeInt(blockitems.size)
            blockitems.forEach { mplew.writeInt(it) }

            mplew.writeShort(CashItemFactory.getInstance().allModInfo.size)
            CashItemFactory.getInstance().allModInfo.forEach { it -> writeModItemData(mplew, it.value) }

            mplew.writeShort(0)

            mplew.writeInt(CashItemFactory.getInstance().randomItemInfo.size)
            CashItemFactory.getInstance().randomItemInfo.forEach { it ->
                run {
                    mplew.writeInt(it.key)
                    mplew.writeInt(it.value.size)
                    it.value.forEach { itt -> mplew.writeInt(itt.left) }
                }
            }

            // 首页道具
            mplew.writeInt(CashItemFactory.getInstance().allBaseNewInfo.size)
            CashItemFactory.getInstance().allBaseNewInfo.forEach { it ->
                run {
                    mplew.write(it.value)
                    mplew.writeInt(it.key)
                }
            }

            mplew.writeZeroBytes(1090)

            //自定义礼包?
            mplew.writeShort(customPackages.size)
            customPackages.forEach { it ->
                run {
                    mplew.writeInt(it.key)
                    mplew.writeInt(it.value.sn)
                    mplew.writeZeroBytes(36)
                    mplew.writeInt(2)
                    mplew.writeLong(-1)
                    mplew.writeInt(0x0E)
                    mplew.writeInt(it.value.termStart)
                    mplew.writeInt(it.value.termEnd)
                    mplew.writeInt(0x13)
                    mplew.writeInt(0x16)
                    (0..6).forEach { mplew.writeInt(1) }
                    mplew.writeLong(0)
                    mplew.writeInt(0x3C)
                }
            }
            mplew.writeLong(1)
            mplew.writeInt(0)
            mplew.writeLong(System.currentTimeMillis())
            mplew.writeInt(0)
        }

        return mplew.packet
    }

    fun writeModItemData(mplew: MaplePacketLittleEndianWriter, cmi: CashItemInfo.CashModInfo) {
        val flags = cmi.flags
        mplew.writeInt(cmi.sn)
        mplew.writeLong(cmi.flags.toLong())
        if (flags and 0x01 != 0) {
            mplew.writeInt(cmi.itemid)
        }
        if (flags and 0x02 != 0) {
            mplew.writeShort(cmi.count)
        }
        if (flags and 0x10 != 0) {
            mplew.write(cmi.priority)
        }
        if (flags and 0x04 != 0) {
            mplew.writeInt(cmi.discountPrice)
        }
        if (flags and 0x8 != 0) {
            mplew.write(cmi.csClass)
        }
        if (flags and 0x20 != 0) {
            mplew.writeShort(cmi.period)
        }
        if (flags and 0x20000 != 0) {
            mplew.writeShort(cmi.fameLimit)
        }
        if (flags and 0x40000 != 0) {
            mplew.writeShort(cmi.levelLimit)
        }
        //0x40 = ?
        if (flags and 0x80 != 0) {
            mplew.writeInt(cmi.meso)
        }
        if (flags and 0x200 != 0) {
            mplew.write(cmi.gender)
        }
        if (flags and 0x400 != 0) {
            mplew.writeBool(cmi.isShowUp)
        }
        if (flags and 0x800 != 0) {
            mplew.write(cmi.mark)
        }
        //0x2000, 0x4000, 0x8000, 0x10000, 0x20000, 0x100000, 0x80000 - ?
        if (flags and 0x80000 != 0) {
            mplew.writeInt(cmi.termStart)
        }
        if (flags and 0x100000 != 0) {
            mplew.writeInt(cmi.termEnd)
        }
        if (flags and 0x800000 != 0) {
            mplew.writeShort(cmi.categories)
        }
    }

    fun playCashSong(itemid: Int, name: String): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CASH_SONG.value)
        mplew.writeInt(itemid)
        mplew.writeMapleAsciiString(name)

        return mplew.packet
    }

    /*
     * 添加玩家使用音乐盒效果
     */
    fun addCharBox(c: MapleCharacter, itemId: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.value)
        mplew.writeInt(c.id)
        mplew.writeInt(itemId)

        return mplew.packet
    }

    /*
     * 取消玩家使用音乐盒效果
     */
    fun removeCharBox(c: MapleCharacter): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.value)
        mplew.writeInt(c.id)
        mplew.writeInt(0)

        return mplew.packet
    }

    fun useCharm(charmsleft: Byte, daysleft: Byte): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.value)
        mplew.write(0x0A)
        mplew.write(1)
        mplew.write(charmsleft)
        mplew.write(daysleft)

        return mplew.packet
    }

    fun useWheel(charmsleft: Byte): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.value)
        mplew.write(EffectOpcode.UserEffect_UpgradeTombItemUse.value.toInt())
        mplew.writeLong(charmsleft.toLong())

        return mplew.packet
    }

    fun sendGoldHammerResult(n: Int, n2: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.GOLD_HAMMER_RESULT.value)
        mplew.write(n)
        mplew.writeInt(n2)
        mplew.writeInt(0)

        return mplew.packet
    }

    /*
     * 金锤子效果
     */
    fun sendCashHammerResult(start: Boolean, hammered: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_HAMMER_RESPONSE.value)
        mplew.write(if (start) if (hammered) 0xB7 else 0xB6 else if (hammered) 0xBC else 0xBB)
        mplew.writeInt(0)
        if (start) {
            mplew.writeInt(1)
        }
        return mplew.packet
    }

    fun changePetFlag(uniqueId: Int, added: Boolean, flagAdded: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.PET_FLAG_CHANGE.value)

        mplew.writeLong(uniqueId.toLong())
        mplew.write(if (added) 1 else 0)
        mplew.writeShort(flagAdded)

        return mplew.packet
    }

    fun changePetName(chr: MapleCharacter, newname: String, slot: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.value)

        mplew.writeInt(chr.id)
        mplew.write(0) //notsure
        mplew.writeMapleAsciiString(newname)
        mplew.writeInt(slot)

        return mplew.packet
    }

    @Throws(SQLException::class)
    fun showNotes(notes: ResultSet, count: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        /*
         * Recv SHOW_NOTES [002E] (40) 2E 00 03 01 77 47 0A 00 0B 00 4D 61 70 6C
         * 65 D8 BC D0 A1 B1 EA 08 00 D0 BB D0 BB B4 F3 BA C5 F0 E8 23 E3 B5 F0
         * CC 01 01 ....wG....Maple丶小标..谢谢大号痂#愕鹛..
         */
        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.value)
        mplew.write(0x03)
        mplew.write(count)
        for (i in 0..count - 1) {
            mplew.writeInt(notes.getInt("id"))
            mplew.writeMapleAsciiString(notes.getString("from"))
            mplew.writeMapleAsciiString(notes.getString("message"))
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")))
            mplew.write(notes.getInt("gift"))
            notes.next()
        }

        return mplew.packet
    }

    /*
     * 小黑板
     */
    fun useChalkboard(charid: Int, msg: String?): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.value)

        mplew.writeInt(charid)
        if (msg == null || msg.isEmpty()) {
            mplew.write(0)
        } else {
            mplew.write(1)
            mplew.writeMapleAsciiString(msg)
        }

        return mplew.packet
    }

    /*
     * 使用瞬移之石
     */
    fun getTrockRefresh(chr: MapleCharacter, vip: Byte, delete: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        /*
         * 高级瞬移之石
         * 2F 00
         * 03 - 添加
         * 02
         * C0 A7 23 06
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * FF C9 9A 3B
         * /...困#.??????????????????
         */
        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.value)
        mplew.write(if (delete) 2 else 3)
        mplew.write(vip)
        if (vip.toInt() == 0x01) {
            val map = chr.regRocks
            for (i in 0..4) {
                mplew.writeInt(map[i])
            }
        } else if (vip.toInt() == 0x02) {
            val map = chr.rocks
            for (i in 0..9) {
                mplew.writeInt(map[i])
            }
        } else if (vip.toInt() == 0x03) {
            val map = chr.hyperRocks
            for (i in 0..12) {
                mplew.writeInt(map[i])
            }
        }
        return mplew.packet
    }

    /*
     * 使用时空或者超时空卷错误提示
     */
    fun getTrockMessage(op: Byte): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.value)
        /*
         * 0x05 因未知原因无法移动 0x0B 因某种原因，不能去那里
         */
        mplew.writeShort(op.toShort())

        return mplew.packet
    }

    /*
     * 加载完商城道具就是这个包
     */
    fun enableCSUse(type: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_USE.value)
        /*
         * 0x10 显示Vip服务界面
         */
        mplew.write(type)
        mplew.writeInt(0)

        return mplew.packet
    }

    /*
     * 商城购买药剂罐更新药剂罐信息
     */
    fun updataPotionPot(potionPot: MaplePotionPot): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_POTION_POT_UPDATE.value)
        PacketHelper.addPotionPotInfo(mplew, potionPot)

        return mplew.packet
    }

    /**
     * 显示点卷和抵用卷
     */
    fun updateCouponsInfo(chr: MapleCharacter): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_UPDATE.value)
        mplew.writeInt(chr.getCSPoints(1)) // 点券
        mplew.writeInt(chr.getCSPoints(2)) // 抵用券
        mplew.writeInt(0)

        return mplew.packet
    }

    /**
     * 刷新角色在商城中的金币信息
     */
    fun updataMeso(chr: MapleCharacter): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_UPDATE_MESO.value)
        mplew.writeLong(MapleStat.金币.value)
        mplew.writeLong(chr.meso)

        return mplew.packet
    }

    /*
     * 显示商城道具栏物品
     * getCSInventory
     */
    fun 商城道具栏信息(c: MapleClient): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.加载道具栏.value.toInt())
        mplew.write(0) //V.109 新增
        val mci = c.player.cashInventory
        var size = 0
        mplew.writeShort(mci.itemsSize)
        for (itemz in mci.inventory) {
            addCashItemInfo(mplew, itemz, c.accID, 0)
            if (ItemConstants.isPet(itemz.itemId)) {
                size++
            }
        }
        if (mci.itemsSize > 0) {
            mplew.writeInt(size)
            if (mci.inventory.size > 0) {
                mci.inventory
                        .filter { ItemConstants.isPet(it.itemId) }
                        .forEach {
                            //好像现在只有宠物才写这个封包
                            PacketHelper.addItemInfo(mplew, it)
                        }
            }
        }
        mplew.writeShort(c.player.storage.slots) // 仓库数量
        mplew.writeShort(c.accCharSlots) // 可以创建的角色数量
        mplew.writeShort(0)
        mplew.writeShort(c.loadCharactersSize(c.world)) // 已创建的角色数量

        return mplew.packet
    }

    /*
     * 显示商城的礼物
     * getCSGifts
     */
    fun 商城礼物信息(gifts: List<Pair<Item, String>>): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.加载礼物.value.toInt())
        mplew.writeShort(gifts.size)
        for (gift in gifts) {
            mplew.writeLong(gift.getLeft().uniqueId.toLong())
            mplew.writeInt(gift.getLeft().itemId)
            mplew.writeAsciiString(gift.getLeft().giftFrom, 13)
            mplew.writeAsciiString(gift.getRight(), 73)
        }

        return mplew.packet
    }

    /*
     * 商城购物车
     * sendWishList
     */
    fun 商城购物车(chr: MapleCharacter, update: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write((if (update) CashShopOpcode.更新购物车.value else CashShopOpcode.加载购物车.value).toInt())
        val list = chr.wishlist
        for (i in 0..11) {
            mplew.writeInt(if (list[i] != -1) list[i] else 0)
        }
        return mplew.packet
    }

    /*
     * 购买商城物品
     * showBoughtCSItem
     */
    fun 购买商城道具(item: Item, sn: Int, accid: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.购买道具.value.toInt())
        addCashItemInfo(mplew, item, accid, sn)
        mplew.writeZeroBytes(9) //V.114修改 以前为4

        return mplew.packet
    }

    /*
     * 商城送礼物
     */
    fun 商城送礼(itemid: Int, quantity: Int, receiver: String): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.商城送礼.value.toInt())
        mplew.writeMapleAsciiString(receiver)
        mplew.writeInt(itemid)
        mplew.writeShort(quantity)

        return mplew.packet

    }

    fun 扩充道具栏(inv: Int, slots: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.扩充道具栏.value.toInt())
        mplew.write(inv)
        mplew.writeShort(slots)
        mplew.writeInt(0) //V.114新增 未知

        return mplew.packet
    }

    fun 扩充仓库(slots: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.扩充仓库.value.toInt())
        mplew.writeShort(slots)
        mplew.writeInt(0)

        return mplew.packet
    }

    fun 购买角色卡(slots: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.购买角色卡.value.toInt())
        mplew.writeShort(slots)
        mplew.writeInt(0)

        return mplew.packet
    }

    fun 扩充项链(days: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.扩充项链.value.toInt())
        mplew.writeShort(0x00)
        mplew.writeShort(days)
        mplew.writeInt(0)

        return mplew.packet
    }

    /*
     * 商城-->背包
     */
    fun moveItemToInvFormCs(item: Item): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.商城到背包.value.toInt())
        mplew.write(item.quantity.toInt())
        mplew.writeShort(item.position)
        PacketHelper.addItemInfo(mplew, item)
        mplew.writeZeroBytes(5)

        return mplew.packet
    }

    /*
     * 背包-->商城
     */
    fun moveItemToCsFromInv(item: Item, accId: Int, sn: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.背包到商城.value.toInt())
        addCashItemInfo(mplew, item, accId, sn)

        return mplew.packet
    }

    /*
     * 商城删除道具
     */
    fun 商城删除道具(uniqueid: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.删除道具.value.toInt())
        mplew.writeLong(uniqueid.toLong())

        return mplew.packet
    }

    /*
     * 商城道具到期
     */
    fun cashItemExpired(uniqueid: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.道具到期.value.toInt())
        mplew.writeLong(uniqueid.toLong())

        return mplew.packet
    }

    /*
     * 商城换购道具
     */
    fun 商城换购道具(uniqueId: Int, Money: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.换购道具.value.toInt())
        mplew.writeLong(uniqueId.toLong())
        mplew.writeLong(Money.toLong())

        return mplew.packet
    }

    /*
     * 商城购买礼包
     */
    fun 商城购买礼包(packageItems: Map<Int, Item>, accId: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.购买礼包.value.toInt())
        mplew.write(packageItems.size)
        var size = 0
        for ((key, value) in packageItems) {
            addCashItemInfo(mplew, value, accId, key)
            if (ItemConstants.isPet(value.itemId) || ItemConstants.getInventoryType(value.itemId) == MapleInventoryType.EQUIP) {
                size++
            }
        }
        mplew.writeInt(size)
        if (packageItems.isNotEmpty()) {
            packageItems.values
                    .filter { ItemConstants.isPet(it.itemId) || ItemConstants.getInventoryType(it.itemId) == MapleInventoryType.EQUIP }
                    .forEach { PacketHelper.addItemInfo(mplew, it) }
        }
        mplew.writeZeroBytes(3)

        return mplew.packet
    }

    /*
     * 商城赠送礼包
     */
    fun 商城送礼包(itemId: Int, quantity: Int, receiver: String): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.商城送礼包.value.toInt())
        mplew.writeMapleAsciiString(receiver)
        mplew.writeInt(itemId)
        mplew.writeInt(quantity)

        return mplew.packet
    }

    /*
     * 商城购买任务物品
     */
    fun 商城购买任务道具(price: Int, quantity: Short, position: Byte, itemid: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.购买任务道具.value.toInt())
        mplew.writeInt(price)
        mplew.writeShort(quantity)
        mplew.writeShort(position.toShort())
        mplew.writeInt(itemid)

        return mplew.packet
    }

    fun 抵用券兑换道具(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.抵用券兑换道具.value.toInt())

        return mplew.packet
    }

    fun addCashItemInfo(mplew: MaplePacketLittleEndianWriter, item: Item, accId: Int, sn: Int) {
        val cashinfo = CashItemFactory.getInstance()
        mplew.writeLong((if (item.uniqueId > 0) item.uniqueId else 0).toLong())
        mplew.writeLong(accId.toLong())
        mplew.writeInt(item.itemId)
        mplew.writeInt(if (sn > 0) sn else cashinfo.getSnFromId(cashinfo.getLinkItemId(item.itemId)))
        mplew.writeShort(item.quantity)
        mplew.writeAsciiString(item.giftFrom, 13)
        PacketHelper.addExpirationTime(mplew, item.expiration) //00 80 05 BB 46 E6 17 02
        mplew.writeLong((if (item.expiration == -1L) 30 else 0).toLong())
        mplew.writeZeroBytes(22) //V.113修改以前为 30
        PacketHelper.addExpirationTime(mplew, -2) //00 40 E0 FD 3B 37 4F 01
        mplew.writeZeroBytes(17) //V.101新增
    }

    /*
     * 00 = 因发生未知错误。不能进入到冒险岛商城。
     * 01 = 超出工作时间。请稍后在试。
     * 02 = 因发生未知错误。不能进入到冒险岛商城。
     * 03 = 点卷余额不足
     * 04 = 未满14岁的用户不能赠送现金道具。
     * 05 = 超出了可以送礼物的限界额
     * 06 = 无法向本人的账号赠送礼物。请用该角色登陆，然后购买。
     * 07 = 请确认角色名是否错误。
     * 08 = 此道具对性别有限制。请确认接收人的性别。
     * 09 = 接收礼物的人的保管箱已满，无法发送礼物。
     * 0A = 请确认是否超过可以保有的现金道具数量。
     * 0B = 请确认角色名字是否正确或者性别有限制。
     * 0C = 因发生未知错误。不能进入到冒险岛商城。
     * 0D = 因发生未知错误。不能进入到冒险岛商城。
     * 0E = 请再确认领奖卡号码是否正确。
     * 0F = 已经过期的领奖号
     * 10 = 已经使用过的领奖号
     * 11 = 未知是乱码显示
     * 12 = 未知是乱码显示
     * 13 = 未知是乱码显示
     * 14 = 这是NexonCashCoupon号码！请上Nexon.com(www.nexon.com)的MyPage>NexonCash>Menu中登录Copon号码。
     * 15 = 你的性别不适合这种领奖卡
     * 16 = 这种领奖卡是专用道具。所有你不能赠送给别人。
     * 17 = 此卷是冒险岛专用抵用卷无法送给他人。
     * 18 = 请确认是不是你的背包空间不够。
     * 19 = 这种道具只能在优秀网吧会员买的到。
     * 1A = 恋人道具只能赠送给相同频道的不同性别的角色。请确认是否你要送礼物的角色在同一频道或者性别不同。
     * 1B = 请你正确输入要送礼物的角色名。
     * 1C = 无 直接掉线38
     * 1D = 无 直接掉线38
     * 1E = 超过了点卷购买限制额。
     * 1F = 金币不足
     * 20 = 请确认身份证号后再试。
     * 21 = 此会员卡只限于新购买现金道具用户使用。
     * 22 = 已经报名
     * 23 = 因发生未知错误。不能进入到冒险岛商城。
     * 24 = 因发生未知错误。不能进入到冒险岛商城。
     * 25 = 因发生未知错误。不能进入到冒险岛商城。
     * 26 = 因发生未知错误。不能进入到冒险岛商城。
     * 27 = 因发生未知错误。不能进入到冒险岛商城。
     * 28 = 因发生未知错误。不能进入到冒险岛商城。
     * 29 = 超过了该道具的每日购买限额，无法购买。
     * 2A = 因发生未知错误。不能进入到冒险岛商城。
     * 2B = 因发生未知错误。不能进入到冒险岛商城。
     * 2C = 已超过每个盛大账号可以使用该优惠卷的限制次数。详细内容请参考优惠卷说明。
     * 2D = 因发生未知错误。不能进入到冒险岛商城。
     * 2E = 未满7岁的人无法购买该物品
     * 2F = 未满7对的人无法接受该礼物
     * 30 = 超过了该道具每日购买限额，无法购买。
     * 31 = 设置为不能使用点卷。请到盛大主页我的信息中的点卷安全设置菜单中更改设置。
     * 32 = 因发生未知错误。不能进入到冒险岛商城。
     * 33 = 因发生未知错误。不能进入到冒险岛商城。
     * 34 = 因发生未知错误。不能进入到冒险岛商城。
     * 35 = 该道具目前不在出售
     * 36 = 目前无法取消订单
     * 37 = 购买后超过7天的道具无法取消订单。
     * 38 = 无法取消订单的道具
     * 39 = 礼包中的部分道具已领取，无法取消订单。
     * 3A = 超过该道具的购买限度，无法购买。
     * 3B = 该道具只有[30]级以上角色才可以购买.
     * 3C = 该道具只有[70]级以上角色才可以购买.
     * 3D = 该道具只有[50]级以上角色才可以购买.
     * 3E = 该道具只有[100]级以上角色才可以购买.
     * 3F = 无法购买或赠送更多每日特价物品。
     * 40 = 该道具无法用抵用卷购买。
     * 41 = 该道具无法用抵用卷购买。
     * 42 = 因发生未知错误。不能进入到冒险岛商城。
     * 43 = 因发生未知错误。不能进入到冒险岛商城。
     * 44 = 该道具无法用抵用卷购买。
     * 45 = 70级以上无法购买
     * 46 = 因发生未知错误。不能进入到冒险岛商城。
     * 47 = 因发生未知错误。不能进入到冒险岛商城。
     * 48 = 因发生未知错误。不能进入到冒险岛商城。
     * 49 = 因发生未知错误。不能进入到冒险岛商城。
     * 4A = 因发生未知错误。不能进入到冒险岛商城。
     * 4B = 因发生未知错误。不能进入到冒险岛商城。
     * 4C = 因发生未知错误。不能进入到冒险岛商城。
     */
    fun 商城错误提示(err: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.错误提示.value.toInt()) //V.111 0x70
        mplew.write(err)

        return mplew.packet
    }

    /*
     * 商城领奖卡提示
     */
    fun showCouponRedeemedItem(itemid: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.writeShort(CashShopOpcode.领奖卡提示.value)
        mplew.writeInt(0)
        mplew.writeInt(1)
        mplew.writeShort(1)
        mplew.writeShort(0x1A)
        mplew.writeInt(itemid)
        mplew.writeInt(0)

        return mplew.packet
    }

    /*
     * 商城领奖卡提示
     */
    fun showCouponRedeemedItem(items: Map<Int, Item>, mesos: Int, maplePoints: Int, c: MapleClient): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.领奖卡提示.value.toInt()) //use to be 4c
        mplew.write(items.size)
        for ((key, value) in items) {
            addCashItemInfo(mplew, value, c.accID, key)
        }
        mplew.writeInt(maplePoints)
        mplew.writeInt(0) // Normal items size
        mplew.writeInt(mesos)

        return mplew.packet
    }

    /*
     * 不发这个好像买不了商城道具
     */
    fun redeemResponse(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        /*
         * T075 0xB9
         * V111 0xBC
         */
        mplew.write(CashShopOpcode.注册商城.value.toInt())
        mplew.writeInt(0)
        mplew.writeInt(1)

        return mplew.packet
    }

    /*
     * 商城中打开箱子
     */
    fun 商城打开箱子(item: Item, uniqueId: Long?): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.value)
        mplew.write(CashShopOpcode.打开箱子.value.toInt())
        mplew.writeLong(uniqueId!!)
        mplew.writeInt(0)
        PacketHelper.addItemInfo(mplew, item)
        mplew.writeInt(item.position.toInt()) //道具在背包中的位置
        mplew.writeZeroBytes(3)

        return mplew.packet
    }

    fun getTime(): Int {
        val time = SimpleDateFormat("yyyy-MM-dd").format(Date()).replace("-", "")
        return Integer.valueOf(time)!!
    }

    /*
     * 使用金币包失败
     */
    fun sendMesobagFailed(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.MESOBAG_FAILURE.value)
        return mplew.packet
    }

    /*
     * 使用金币包成功
     */
    fun sendMesobagSuccess(mesos: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.MESOBAG_SUCCESS.value)
        mplew.writeInt(mesos)
        return mplew.packet
    }

    //======================================MTS===========================================
    fun startMTS(chr: MapleCharacter): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()
        mplew.writeShort(SendPacketOpcode.MTS_OPEN.value)

        PacketHelper.addCharacterInfo(mplew, chr)
        mplew.writeMapleAsciiString(chr.client.accountName)
        mplew.writeInt(ServerConstants.MTS_MESO) //2500 [C4 09 00 00]
        mplew.writeInt(ServerConstants.MTS_TAX) //5 [05 00 00 00]
        mplew.writeInt(ServerConstants.MTS_BASE) //150 [96 00 00 00]
        mplew.writeInt(24) //[18 00 00 00]
        mplew.writeInt(168) //[A8 00 00 00]
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()))

        return mplew.packet
    }

    fun sendMTS(items: List<MTSStorage.MTSItemInfo>, tab: Int, type: Int, page: Int, pages: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        /*
         * T065 0x14
         */
        mplew.write(0x15) //operation
        mplew.writeInt(pages) //total items
        mplew.writeInt(items.size) //number of items on this page
        mplew.writeInt(tab)
        mplew.writeInt(type)
        mplew.writeInt(page)
        mplew.write(1)
        mplew.write(1)

        for (item in items) {
            addMTSItemInfo(mplew, item)
        }
        mplew.write(0) //0 or 1?


        return mplew.packet
    }

    /*
     * 在拍卖中显示角色抵用卷
     */
    fun showMTSCash(chr: MapleCharacter): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.GET_MTS_TOKENS.value)
        mplew.writeInt(chr.getCSPoints(2))

        return mplew.packet
    }

    fun getMTSWantedListingOver(nx: Int, items: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x3D)
        mplew.writeInt(nx)
        mplew.writeInt(items)

        return mplew.packet
    }

    fun getMTSConfirmSell(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x1D)

        return mplew.packet
    }

    fun getMTSFailSell(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x1E)
        mplew.write(0x42)

        return mplew.packet
    }

    fun getMTSConfirmBuy(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x33)

        return mplew.packet
    }

    fun getMTSFailBuy(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x34)
        mplew.write(0x42)

        return mplew.packet
    }

    fun getMTSConfirmCancel(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x25)

        return mplew.packet
    }

    fun getMTSFailCancel(): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x26)
        mplew.write(0x42)

        return mplew.packet
    }

    fun getMTSConfirmTransfer(quantity: Int, pos: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x27)
        mplew.writeInt(quantity)
        mplew.writeInt(pos)

        return mplew.packet
    }

    private fun addMTSItemInfo(mplew: MaplePacketLittleEndianWriter, item: MTSStorage.MTSItemInfo) {
        PacketHelper.addItemInfo(mplew, item.item)
        mplew.writeInt(item.id) //id
        mplew.writeInt(item.taxes) //this + below = price
        mplew.writeInt(item.price) //price
        mplew.writeZeroBytes(8)
        mplew.writeLong(PacketHelper.getTime(item.endingDate))
        mplew.writeMapleAsciiString(item.seller) //account name (what was nexon thinking?)
        mplew.writeMapleAsciiString(item.seller) //char name
        mplew.writeZeroBytes(28)
    }

    fun getNotYetSoldInv(items: List<MTSStorage.MTSItemInfo>): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x23)

        mplew.writeInt(items.size)

        for (item in items) {
            addMTSItemInfo(mplew, item)
        }

        return mplew.packet
    }

    fun getTransferInventory(items: List<Item>, changed: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        mplew.write(0x21)

        mplew.writeInt(items.size)
        var i = 0
        for (item in items) {
            PacketHelper.addItemInfo(mplew, item)
            mplew.writeInt(Integer.MAX_VALUE - i) //fake ID
            mplew.writeZeroBytes(56) //really just addMTSItemInfo
            i++
        }
        mplew.writeInt(-47 + i - 1)
        mplew.write(if (changed) 1 else 0)

        return mplew.packet
    }

    fun addToCartMessage(fail: Boolean, remove: Boolean): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.MTS_OPERATION.value)
        if (remove) {
            if (fail) {
                mplew.write(0x2C)
                mplew.writeInt(-1)
            } else {
                mplew.write(0x2B) //T065 0x28
            }
        } else {
            if (fail) {
                mplew.write(0x2A)
                mplew.writeInt(-1)
            } else {
                mplew.write(0x29) //T065 0x26
            }
        }

        return mplew.packet
    }
}
