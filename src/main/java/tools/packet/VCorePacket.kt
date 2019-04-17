package tools.packet

import client.MapleCharacter
import handling.opcode.SendPacketOpcode
import tools.data.output.MaplePacketLittleEndianWriter

object VCorePacket {

    fun updateVCoreList(player: MapleCharacter, b: Boolean, n: Int, n2: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.VCORE_LIST_UPDATE.value)
        writeVCoreSkillData(mplew, player)
        mplew.writeInt(if (b) 1 else 0)
        if (b) {
            mplew.writeInt(n)
            mplew.writeInt(n2)
        }
        return mplew.packet
    }

    fun showVCoreSkillExpResult(n1: Int, expEnforce: Int, currLevel: Int, newLevel: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.VCORE_SKILLEXP_RESULT.value)
        mplew.writeInt(n1)
        mplew.writeInt(expEnforce)
        mplew.writeInt(currLevel)
        mplew.writeInt(newLevel)

        return mplew.packet
    }

    fun addVCorePieceResult(piece: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.VCORE_ADD_PIECE_RESULT.value)
        mplew.writeInt(piece)

        return mplew.packet
    }

    fun addVCoreSkillResult(vcoreid: Int, n2: Int, skill1: Int, skill2: Int, skill3: Int): ByteArray {
        val mplew = MaplePacketLittleEndianWriter()

        mplew.writeShort(SendPacketOpcode.VCORE_ADD_SKILL_RESULT.value)
        mplew.writeInt(vcoreid)
        mplew.writeInt(n2)
        mplew.writeInt(skill1)
        mplew.writeInt(skill2)
        mplew.writeInt(skill3)

        return mplew.packet
    }

    fun writeVCoreSkillData(mplew: MaplePacketLittleEndianWriter, player: MapleCharacter) {
        mplew.writeInt(player.vCoreSkill.size)
        player.vCoreSkill.entries.forEach {
            mplew.writeInt(it.key)
            mplew.writeInt(201327833)
            mplew.writeInt(it.value.vcoreid)
            mplew.writeInt(it.value.level)
            mplew.writeInt(it.value.exp)
            mplew.writeInt(it.value.slot)
            mplew.writeInt(it.value.skill1)
            mplew.writeInt(it.value.skill2)
            mplew.writeInt(it.value.skill3)
            mplew.writeLong(PacketHelper.getTime(-2))
        }
    }
}