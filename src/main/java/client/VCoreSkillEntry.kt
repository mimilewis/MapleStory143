package client

data class VCoreSkillEntry(
        var vcoreid: Int = 0,
        var level: Int = 0,
        var exp: Int = 0,
        var skill1: Int = 0,
        var skill2: Int = 0,
        var skill3: Int = 0,
        var slot: Int = 0) {

    fun getType(): Int {
        return vcoreid / 10000000 - 1
    }

    fun gainExp(exp: Int): Unit {
        this.exp += exp
    }

    fun levelUP(): Unit {
        ++this.level
    }

    fun getSkill(slot: Int): Int {
        when (slot) {
            1 -> return skill1
            2 -> return skill2
            3 -> return skill3
            else -> return 0
        }
    }
}