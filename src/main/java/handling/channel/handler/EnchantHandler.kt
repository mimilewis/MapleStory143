package handling.channel.handler

import client.inventory.EnchantScrollEntry
import client.inventory.EnchantScrollFlag
import client.inventory.Equip
import client.inventory.Item
import constants.ItemConstants
import server.MapleItemInformationProvider
import java.util.*


class EnchantHandler {

    companion object {
        fun getScrollList(equip: Equip): ArrayList<EnchantScrollEntry> {
            val ret = ArrayList<EnchantScrollEntry>()
            val reqJob = MapleItemInformationProvider.getInstance().getReqJob(equip.itemId)
            if (ItemConstants.isWeapon(equip.itemId)) {
                when (reqJob) {
                    0 -> {
                        ret.add(EnchantScrollEntry.攻击_力量_100)
                        ret.add(EnchantScrollEntry.攻击_力量_70)
                        ret.add(EnchantScrollEntry.攻击_力量_30)
                        ret.add(EnchantScrollEntry.攻击_力量_15)
                        ret.add(EnchantScrollEntry.魔力_智力_100)
                        ret.add(EnchantScrollEntry.魔力_智力_70)
                        ret.add(EnchantScrollEntry.魔力_智力_30)
                        ret.add(EnchantScrollEntry.魔力_智力_15)
                        ret.add(EnchantScrollEntry.攻击_敏捷_100)
                        ret.add(EnchantScrollEntry.攻击_敏捷_70)
                        ret.add(EnchantScrollEntry.攻击_敏捷_30)
                        ret.add(EnchantScrollEntry.攻击_敏捷_15)
                        ret.add(EnchantScrollEntry.攻击_运气_100)
                        ret.add(EnchantScrollEntry.攻击_运气_70)
                        ret.add(EnchantScrollEntry.攻击_运气_30)
                        ret.add(EnchantScrollEntry.攻击_运气_15)
                        ret.add(EnchantScrollEntry.攻击_体力_100)
                        ret.add(EnchantScrollEntry.攻击_体力_70)
                        ret.add(EnchantScrollEntry.攻击_体力_30)
                        ret.add(EnchantScrollEntry.攻击_体力_15)
                    }
                    -1, 1 -> {
                        ret.add(EnchantScrollEntry.攻击_力量_100)
                        ret.add(EnchantScrollEntry.攻击_力量_70)
                        ret.add(EnchantScrollEntry.攻击_力量_30)
                        ret.add(EnchantScrollEntry.攻击_力量_15)
                        ret.add(EnchantScrollEntry.攻击_体力_100)
                        ret.add(EnchantScrollEntry.攻击_体力_70)
                        ret.add(EnchantScrollEntry.攻击_体力_30)
                        ret.add(EnchantScrollEntry.攻击_体力_15)
                    }
                    2 -> {
                        ret.add(EnchantScrollEntry.魔力_智力_100)
                        ret.add(EnchantScrollEntry.魔力_智力_70)
                        ret.add(EnchantScrollEntry.魔力_智力_30)
                        ret.add(EnchantScrollEntry.魔力_智力_15)
                    }
                    4 -> {
                        ret.add(EnchantScrollEntry.攻击_敏捷_100)
                        ret.add(EnchantScrollEntry.攻击_敏捷_70)
                        ret.add(EnchantScrollEntry.攻击_敏捷_30)
                        ret.add(EnchantScrollEntry.攻击_敏捷_15)
                        ret.add(EnchantScrollEntry.攻击_体力_100)
                        ret.add(EnchantScrollEntry.攻击_体力_70)
                        ret.add(EnchantScrollEntry.攻击_体力_30)
                        ret.add(EnchantScrollEntry.攻击_体力_15)
                    }
                    8, 24 -> {
                        ret.add(EnchantScrollEntry.攻击_力量_100)
                        ret.add(EnchantScrollEntry.攻击_力量_70)
                        ret.add(EnchantScrollEntry.攻击_力量_30)
                        ret.add(EnchantScrollEntry.攻击_力量_15)
                        ret.add(EnchantScrollEntry.攻击_敏捷_100)
                        ret.add(EnchantScrollEntry.攻击_敏捷_70)
                        ret.add(EnchantScrollEntry.攻击_敏捷_30)
                        ret.add(EnchantScrollEntry.攻击_敏捷_15)
                        ret.add(EnchantScrollEntry.攻击_运气_100)
                        ret.add(EnchantScrollEntry.攻击_运气_70)
                        ret.add(EnchantScrollEntry.攻击_运气_30)
                        ret.add(EnchantScrollEntry.攻击_运气_15)
                    }
                    16 -> {
                        ret.add(EnchantScrollEntry.攻击_力量_100)
                        ret.add(EnchantScrollEntry.攻击_力量_70)
                        ret.add(EnchantScrollEntry.攻击_力量_30)
                        ret.add(EnchantScrollEntry.攻击_力量_15)
                        ret.add(EnchantScrollEntry.攻击_敏捷_100)
                        ret.add(EnchantScrollEntry.攻击_敏捷_70)
                        ret.add(EnchantScrollEntry.攻击_敏捷_30)
                        ret.add(EnchantScrollEntry.攻击_敏捷_15)
                        ret.add(EnchantScrollEntry.攻击_体力_100)
                        ret.add(EnchantScrollEntry.攻击_体力_70)
                        ret.add(EnchantScrollEntry.攻击_体力_30)
                        ret.add(EnchantScrollEntry.攻击_体力_15)
                    }
                }
            } else if (equip.itemId / 10000 == 108) {
                when (reqJob) {
                    0 -> {
                        ret.add(EnchantScrollEntry.攻击_100)
                        ret.add(EnchantScrollEntry.攻击_70)
                        ret.add(EnchantScrollEntry.攻击_30)
                        ret.add(EnchantScrollEntry.魔力_100)
                        ret.add(EnchantScrollEntry.魔力_70)
                        ret.add(EnchantScrollEntry.魔力_30)
                    }
                    1 -> {
                        ret.add(EnchantScrollEntry.攻击_100)
                        ret.add(EnchantScrollEntry.攻击_70)
                        ret.add(EnchantScrollEntry.攻击_30)
                    }
                    2 -> {
                        ret.add(EnchantScrollEntry.魔力_100)
                        ret.add(EnchantScrollEntry.魔力_70)
                        ret.add(EnchantScrollEntry.魔力_30)
                    }
                    4 -> {
                        ret.add(EnchantScrollEntry.攻击_100)
                        ret.add(EnchantScrollEntry.攻击_70)
                        ret.add(EnchantScrollEntry.攻击_30)
                    }
                    8 -> {
                        ret.add(EnchantScrollEntry.攻击_100)
                        ret.add(EnchantScrollEntry.攻击_70)
                        ret.add(EnchantScrollEntry.攻击_30)
                    }
                    16 -> {
                        ret.add(EnchantScrollEntry.攻击_100)
                        ret.add(EnchantScrollEntry.攻击_70)
                        ret.add(EnchantScrollEntry.攻击_30)
                    }
                }
            } else {
                when (reqJob) {
                    0 -> {
                        ret.add(EnchantScrollEntry.力量_100)
                        ret.add(EnchantScrollEntry.力量_70)
                        ret.add(EnchantScrollEntry.力量_30)
                        ret.add(EnchantScrollEntry.智力_100)
                        ret.add(EnchantScrollEntry.智力_70)
                        ret.add(EnchantScrollEntry.智力_30)
                        ret.add(EnchantScrollEntry.敏捷_100)
                        ret.add(EnchantScrollEntry.敏捷_70)
                        ret.add(EnchantScrollEntry.敏捷_30)
                        ret.add(EnchantScrollEntry.运气_100)
                        ret.add(EnchantScrollEntry.运气_70)
                        ret.add(EnchantScrollEntry.运气_30)
                        ret.add(EnchantScrollEntry.体力_100)
                        ret.add(EnchantScrollEntry.体力_70)
                        ret.add(EnchantScrollEntry.体力_30)
                    }
                    1 -> {
                        ret.add(EnchantScrollEntry.力量_100)
                        ret.add(EnchantScrollEntry.力量_70)
                        ret.add(EnchantScrollEntry.力量_30)
                        ret.add(EnchantScrollEntry.体力_100)
                        ret.add(EnchantScrollEntry.体力_70)
                        ret.add(EnchantScrollEntry.体力_30)
                    }
                    2 -> {
                        ret.add(EnchantScrollEntry.智力_100)
                        ret.add(EnchantScrollEntry.智力_70)
                        ret.add(EnchantScrollEntry.智力_30)
                        ret.add(EnchantScrollEntry.体力_100)
                        ret.add(EnchantScrollEntry.体力_70)
                        ret.add(EnchantScrollEntry.体力_30)
                    }
                    4 -> {
                        ret.add(EnchantScrollEntry.敏捷_100)
                        ret.add(EnchantScrollEntry.敏捷_70)
                        ret.add(EnchantScrollEntry.敏捷_30)
                        ret.add(EnchantScrollEntry.体力_100)
                        ret.add(EnchantScrollEntry.体力_70)
                        ret.add(EnchantScrollEntry.体力_30)
                    }
                    8 -> {
                        ret.add(EnchantScrollEntry.力量_100)
                        ret.add(EnchantScrollEntry.力量_70)
                        ret.add(EnchantScrollEntry.力量_30)
                        ret.add(EnchantScrollEntry.敏捷_100)
                        ret.add(EnchantScrollEntry.敏捷_70)
                        ret.add(EnchantScrollEntry.敏捷_30)
                        ret.add(EnchantScrollEntry.运气_100)
                        ret.add(EnchantScrollEntry.运气_70)
                        ret.add(EnchantScrollEntry.运气_30)
                    }
                    16 -> {
                        ret.add(EnchantScrollEntry.力量_100)
                        ret.add(EnchantScrollEntry.力量_70)
                        ret.add(EnchantScrollEntry.力量_30)
                        ret.add(EnchantScrollEntry.敏捷_100)
                        ret.add(EnchantScrollEntry.敏捷_70)
                        ret.add(EnchantScrollEntry.敏捷_30)
                        ret.add(EnchantScrollEntry.体力_100)
                        ret.add(EnchantScrollEntry.体力_70)
                        ret.add(EnchantScrollEntry.体力_30)
                    }
                }
            }
            if (equip.upgradeSlots <= 0) {
                ret.clear()
            }
            ret.add(EnchantScrollEntry.还原卷轴)
            ret.add(EnchantScrollEntry.纯白卷轴)
            return ret
        }

        fun getEnchantScrollList(item: Item, success: Boolean): Map<EnchantScrollFlag, Int> {
            val ret = HashMap<EnchantScrollFlag, Int>()
            val nEquip = item as Equip
            var enhance = nEquip.enhance.toInt()
            if (success) {
                --enhance
            }
            if (enhance >= 15) {
                when (enhance) {
                    15, 16, 17, 18, 19 -> {
                        ret.put(EnchantScrollFlag.物攻, enhance - 6)
                        ret.put(EnchantScrollFlag.魔攻, enhance - 6)
                    }
                    20 -> {
                        ret.put(EnchantScrollFlag.物攻, 15)
                        ret.put(EnchantScrollFlag.魔攻, 15)
                    }
                    21 -> {
                        ret.put(EnchantScrollFlag.物攻, 17)
                        ret.put(EnchantScrollFlag.魔攻, 17)
                    }
                    22 -> {
                        ret.put(EnchantScrollFlag.物攻, 19)
                        ret.put(EnchantScrollFlag.魔攻, 19)
                    }
                    23 -> {
                        ret.put(EnchantScrollFlag.物攻, 21)
                        ret.put(EnchantScrollFlag.魔攻, 21)
                    }
                    24 -> {
                        ret.put(EnchantScrollFlag.物攻, 23)
                        ret.put(EnchantScrollFlag.魔攻, 23)
                    }
                }
                ret.put(EnchantScrollFlag.力量, 11)
                ret.put(EnchantScrollFlag.敏捷, 11)
                ret.put(EnchantScrollFlag.智力, 11)
                ret.put(EnchantScrollFlag.运气, 11)
            } else if (MapleItemInformationProvider.getInstance().isSuperiorEquip(nEquip.itemId)) {
                when (enhance) {
                    0 -> {
                        if (nEquip.str > 0) {
                            ret.put(EnchantScrollFlag.力量, 19)
                        }
                        if (nEquip.dex > 0) {
                            ret.put(EnchantScrollFlag.敏捷, 19)
                        }
                        if (nEquip.int > 0) {
                            ret.put(EnchantScrollFlag.智力, 19)
                        }
                        if (nEquip.luk > 0) {
                            ret.put(EnchantScrollFlag.运气, 19)
                        }
                    }
                    1 -> {
                        if (nEquip.str > 0) {
                            ret.put(EnchantScrollFlag.力量, 20)
                        }
                        if (nEquip.dex > 0) {
                            ret.put(EnchantScrollFlag.敏捷, 20)
                        }
                        if (nEquip.int > 0) {
                            ret.put(EnchantScrollFlag.智力, 20)
                        }
                        if (nEquip.luk > 0) {
                            ret.put(EnchantScrollFlag.运气, 20)
                        }
                    }
                    2 -> {
                        if (nEquip.str > 0) {
                            ret.put(EnchantScrollFlag.力量, 22)
                        }
                        if (nEquip.dex > 0) {
                            ret.put(EnchantScrollFlag.敏捷, 22)
                        }
                        if (nEquip.int > 0) {
                            ret.put(EnchantScrollFlag.智力, 22)
                        }
                        if (nEquip.luk > 0) {
                            ret.put(EnchantScrollFlag.运气, 22)
                        }
                    }
                    3 -> {
                        if (nEquip.str > 0) {
                            ret.put(EnchantScrollFlag.力量, 25)
                        }
                        if (nEquip.dex > 0) {
                            ret.put(EnchantScrollFlag.敏捷, 25)
                        }
                        if (nEquip.int > 0) {
                            ret.put(EnchantScrollFlag.智力, 25)
                        }
                        if (nEquip.luk > 0) {
                            ret.put(EnchantScrollFlag.运气, 25)
                        }
                    }
                    4 -> {
                        if (nEquip.str > 0) {
                            ret.put(EnchantScrollFlag.力量, 29)
                        }
                        if (nEquip.dex > 0) {
                            ret.put(EnchantScrollFlag.敏捷, 29)
                        }
                        if (nEquip.int > 0) {
                            ret.put(EnchantScrollFlag.智力, 29)
                        }
                        if (nEquip.luk > 0) {
                            ret.put(EnchantScrollFlag.运气, 29)
                        }
                    }
                    5, 6, 7, 8, 9 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, enhance + 4)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, enhance + 4)
                        }
                    }
                    10 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, 15)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, 15)
                        }
                    }
                    11 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, 17)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, 17)
                        }
                    }
                    12 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, 19)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, 19)
                        }
                    }
                    13 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, 21)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, 21)
                        }
                    }
                    14 -> {
                        if (nEquip.watk > 0) {
                            ret.put(EnchantScrollFlag.物攻, 23)
                        }
                        if (nEquip.matk > 0) {
                            ret.put(EnchantScrollFlag.魔攻, 23)
                        }
                    }
                }
            } else {
                val n: Int
                if (enhance in 0..4) {
                    n = 2
                } else {
                    n = 3
                }
                if (nEquip.str > 0) {
                    ret.put(EnchantScrollFlag.力量, n)
                }
                if (nEquip.dex > 0) {
                    ret.put(EnchantScrollFlag.敏捷, n)
                }
                if (nEquip.int > 0) {
                    ret.put(EnchantScrollFlag.智力, n)
                }
                if (nEquip.luk > 0) {
                    ret.put(EnchantScrollFlag.运气, n)
                }
                if (!ItemConstants.isWeapon(nEquip.itemId) && !ItemConstants.is双手刀(nEquip.itemId)) {
                    return ret
                }
                val atkvalue = Math.max(nEquip.watk.toInt(), nEquip.matk.toInt())
                val newAtkvalue: Int
                if (atkvalue in 1..49) {
                    newAtkvalue = 1
                } else if (atkvalue in 50..99) {
                    newAtkvalue = 2
                } else if (atkvalue in 100..149) {
                    newAtkvalue = 3
                } else if (atkvalue in 150..199) {
                    newAtkvalue = 4
                } else if (atkvalue in 200..249) {
                    newAtkvalue = 5
                } else if (atkvalue in 250..299) {
                    newAtkvalue = 6
                } else if (atkvalue in 300..349) {
                    newAtkvalue = 7
                } else if (atkvalue in 350..399) {
                    newAtkvalue = 8
                } else if (atkvalue >= 400) {
                    newAtkvalue = 9
                } else {
                    newAtkvalue = 0
                }
                if (nEquip.watk > 0) {
                    ret.put(EnchantScrollFlag.物攻, newAtkvalue)
                }
                if (nEquip.matk > 0) {
                    ret.put(EnchantScrollFlag.魔攻, newAtkvalue)
                }
            }
            return ret
        }

        fun toEnchantScrollHandler(item: Item, success: Boolean) {
            val enchantMap = getEnchantScrollList(item, success)
            val nEquip = item as Equip
            if (enchantMap.containsKey(EnchantScrollFlag.物攻)) {
                nEquip.watk = (nEquip.watk + if (success) -enchantMap[EnchantScrollFlag.物攻]!! else enchantMap[EnchantScrollFlag.物攻] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.魔攻)) {
                nEquip.matk = (nEquip.matk + if (success) -enchantMap[EnchantScrollFlag.魔攻]!! else enchantMap[EnchantScrollFlag.魔攻] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.力量)) {
                nEquip.str = (nEquip.str + if (success) -enchantMap[EnchantScrollFlag.力量]!! else enchantMap[EnchantScrollFlag.力量] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.敏捷)) {
                nEquip.dex = (nEquip.dex + if (success) -enchantMap[EnchantScrollFlag.敏捷]!! else enchantMap[EnchantScrollFlag.敏捷] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.智力)) {
                nEquip.int = (nEquip.int + if (success) -enchantMap[EnchantScrollFlag.智力]!! else enchantMap[EnchantScrollFlag.智力] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.运气)) {
                nEquip.luk = (nEquip.luk + if (success) -enchantMap[EnchantScrollFlag.运气]!! else enchantMap[EnchantScrollFlag.运气] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.物防)) {
                nEquip.wdef = (nEquip.wdef + if (success) -enchantMap[EnchantScrollFlag.物防]!! else enchantMap[EnchantScrollFlag.物防] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.魔防)) {
                nEquip.mdef = (nEquip.mdef + if (success) -enchantMap[EnchantScrollFlag.魔防]!! else enchantMap[EnchantScrollFlag.魔防] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.Hp)) {
                nEquip.hp = (nEquip.hp + if (success) -enchantMap[EnchantScrollFlag.Hp]!! else enchantMap[EnchantScrollFlag.Hp] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.Mp)) {
                nEquip.hp = (nEquip.hp + if (success) -enchantMap[EnchantScrollFlag.Mp]!! else enchantMap[EnchantScrollFlag.Mp] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.命中)) {
                nEquip.acc = (nEquip.acc + if (success) -enchantMap[EnchantScrollFlag.命中]!! else enchantMap[EnchantScrollFlag.命中] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.回避)) {
                nEquip.avoid = (nEquip.avoid + if (success) -enchantMap[EnchantScrollFlag.回避]!! else enchantMap[EnchantScrollFlag.回避] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.跳跃)) {
                nEquip.jump = (nEquip.jump + if (success) -enchantMap[EnchantScrollFlag.跳跃]!! else enchantMap[EnchantScrollFlag.跳跃] as Int).toShort()
            }
            if (enchantMap.containsKey(EnchantScrollFlag.速度)) {
                nEquip.speed = (nEquip.speed + if (success) -enchantMap[EnchantScrollFlag.速度]!! else enchantMap[EnchantScrollFlag.速度] as Int).toShort()
            }
            if (success) {
                nEquip.enhance = Math.max(0, nEquip.enhance - 1).toByte()
            } else {
                nEquip.enhance = (nEquip.enhance + 1).toByte()
            }
        }
    }
}
