package handling.opcode

import handling.WritableIntValueHolder

enum class NPCTalkOP(private var type: Short) : WritableIntValueHolder {

    NEXT_PREV(0),
    UNK_1(1),
    UNK_2(2),
    YES_NO(3),
    TEXT(4),
    NUMBER(5),
    SELECTION(6),
    UNK_7(7),
    UNK_8(8),
    AVATAR(9),
    HAIR(10),
    PET_REVIVE(11),
    UNK_12(12),
    UNK_13(13),
    UNK_14(14),
    ACCEPT_DECLINE(15),
    SLIDE_MENU(16),
    DIRECTION1(18),
    DIRECTION2(19),
    UNK_20(20),
    bnh(21),
    bni(22),
    UNK_24(24),
    CHOICE_ANGLE(25),
    bnl(26),
    bnm(35),
    bnn(36),
    bno(37),
    bnp(38),
    bnq(39),
    MIX_HAIR(40),
    bns(41),
    MIXED_HAIR(42),
    bnu(43),
    MIX_HAIR_NEW(44),
    bnw(45),
    bnx(46),
    bny(47),
    bnz(48),
    bnA(50),
    bnB(51),
    bnC(52);

    override fun getValue(): Short {
        return type
    }

    override fun setValue(newval: Short) {
        type = newval
    }

    companion object {
        fun getNPCTalkOP(by2: Byte): NPCTalkOP? {
            return NPCTalkOP.values().firstOrNull { it.type.toByte() == by2 }
        }
    }
}