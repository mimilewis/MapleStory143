package handling.opcode;

import handling.WritableIntValueHolder;

public enum MessageOpcode implements WritableIntValueHolder {


    MS_DropPickUpMessage(0),
    MS_QuestRecordMessage(1),
    MS_QuestRecordMessageAddValidCheck(2),
    MS_CashItemExpireMessage(3),
    MS_IncEXPMessage(4),
    MS_IncSPMessage(5),
    MS_IncPOPMessage(6),
    MS_IncMoneyMessage(7),
    MS_IncGPMessage(8),
    MS_IncCommitmentMessage(9),
    MS_GiveBuffMessage(10),
    MS_GeneralItemExpireMessage(11),
    MS_SystemMessage(12),
    MS_QuestRecordExMessage(13),
    MS_WorldShareRecordMessage(14),
    MS_ItemProtectExpireMessage(15),
    MS_ItemExpireReplaceMessage(16),
    MS_ItemAbilityTimeLimitedExpireMessage(17),
    MS_SkillExpireMessage(18),
    MS_IncNonCombatStatEXPMessage(19),
    MS_LimitNonCombatStatEXPMessage(20),
    MS_RecipeExpireMessage(21),
    MS_AndroidMachineHeartAlertMessage(22),
    MS_IncFatigueByRestMessage(23),
    MS_IncPvPPointMessage(24),
    MS_PvPItemUseMessage(25),
    MS_WeddingPortalError(26),
    MS_PvPHardCoreExpMessage(27),
    MS_NoticeAutoLineChanged(28),
    MS_EntryRecordMessage(29),
    MS_EvolvingSystemMessage(30),
    MS_EvolvingSystemMessageWithName(31),
    MS_CoreInvenOperationMessage(32),
    MS_NxRecordMessage(33),
    MS_BlockedBehaviorTypeMessage(34),
    MS_IncWPMessage(35),
    MS_MaxWPMessage(36),
    MS_StylishKillMessage(37);

    private short code;

    MessageOpcode(int code) {
        this.code = (short) code;
    }


    @Override
    public short getValue() {
        return code;
    }

    @Override
    public void setValue(short newval) {
        this.code = newval;
    }
}
