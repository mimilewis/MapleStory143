package handling.opcode

import handling.WritableIntValueHolder
import tools.packet.BuddyListPacket

enum class BuddyOpcode(private var type: Short) : WritableIntValueHolder {

    FriendReq_LoadFriend(0),
    FriendReq_SetFriend(1),
    FriendReq_AcceptFriend(2),
    FriendReq_AcceptAccountFriend(3),
    FriendReq_DeleteFriend(4),
    FriendReq_DeleteAccountFriend(5),
    FriendReq_RefuseFriend(6),
    FriendReq_RefuseAccountFriend(7),
    FriendReq_NotifyLogin(8),
    FriendReq_NotifyLogout(9),
    FriendReq_IncMaxCount(10),
    FriendReq_ConvertAccountFriend(11),
    FriendReq_ModifyFriend(12),
    FriendReq_ModifyFriendGroup(13),
    FriendReq_ModifyAccountFriendGroup(14),
    FriendReq_SetOffline(15),
    FriendReq_SetOnline(16),
    FriendReq_SetBlackList(17),
    FriendReq_DeleteBlackList(18),
    FriendRes_LoadFriend_Done(22),
    FriendRes_LoadAccountIDOfCharacterFriend_Done(23),
    FriendRes_NotifyChange_FriendInfo(24),
    FriendRes_Invite(25),
    FriendRes_SetFriend_Done(26),
    FriendRes_SetFriend_FullMe(27),
    FriendRes_SetFriend_FullOther(28),
    FriendRes_SetFriend_AlreadySet(29),
    FriendRes_SetFriend_AlreadyRequested(30),
    FriendRes_SetFriend_Ready(31),
    FriendRes_SetFriend_CantSelf(32),
    FriendRes_SetFriend_Master(33),
    FriendRes_SetFriend_UnknownUser(34),
    FriendRes_SetFriend_Unknown(35),
    FriendRes_SetFriend_RemainCharacterFriend(36),
    FriendRes_SetMessengerMode(37),
    FriendRes_SendSingleFriendInfo(38),
    FriendRes_AcceptFriend_Unknown(39),
    FriendRes_DeleteFriend_Done(40),
    FriendRes_DeleteFriend_Unknown(41),
    FriendRes_Notify(42),
    FriendRes_NotifyNewFriend(43),
    FriendRes_IncMaxCount_Done(44),
    FriendRes_IncMaxCount_Unknown(45),
    FriendRes_RefuseFriend(47);


    override fun getValue(): Short {
        return type
    }

    override fun setValue(newval: Short) {
        type = newval
    }

    companion object {
        fun getByAction(type: Short): BuddyOpcode? {
            return BuddyOpcode.values().firstOrNull { it.type == type }
        }
    }

    fun getPacket(): ByteArray {
        return BuddyListPacket.buddylistMessage(value.toInt())
    }
}