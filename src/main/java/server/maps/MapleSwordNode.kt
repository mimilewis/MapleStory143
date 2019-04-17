package server.maps

import tools.Randomizer
import java.awt.Point
import java.util.*

class MapleSwordNode(val monsterId: Int, val bKO: Int, var point: Point) {
    var bKM = 0
    var count = 0
    val swordNodeInfos = ArrayList<MapleSwordNodeInfo>()
    var bKQ: Point? = null

    init {
        swordNodeInfos.add(MapleSwordNodeInfo(1, bKO, 0, 0, 0, 0, 0, 0, 0, point))
    }

    fun gainCount() {
        ++count
    }

    fun a(top: Int, bottom: Int, left: Int, right: Int, bl2: Boolean) {
        if (bl2) {
            this.swordNodeInfos.clear()
        }
        if (this.swordNodeInfos.size != 14) {
            val n6 = 14
            for (i2 in this.swordNodeInfos.size..n6 - 1) {
                val n7 = 1
                val n8 = this.bKO
                val n9 = i2
                val n10 = if (i2 == 12) 60 else 35
                val n11 = if (i2 == 12) 500 else 0
                val n12 = if (i2 == 13) 11000 else 0
                val n13 = 0
                val n14 = if (i2 == 13) 1 else 0
                val n15 = 0
                var point = Point(Randomizer.rand(left, right), Randomizer.rand(top / 2, bottom - 20))
                if (i2 == 12) {
                    this.bKQ = Point(Randomizer.rand(left, right), 15)
                    point = bKQ as Point
                } else if (n14 > 0) {
                    point = bKQ as Point
                }
                this.swordNodeInfos.add(MapleSwordNodeInfo(n7, n8, n9, n10, n11, n12, n13, n14, n15, point))
            }
        }
    }

    inner class MapleSwordNodeInfo(val nodeType: Int, val bKS: Int, val nodeIndex: Int, val bKU: Int, val bKV: Int, val bKW: Int, val bKX: Int, val bKY: Int, val bKZ: Int, val pos: Point)

}


