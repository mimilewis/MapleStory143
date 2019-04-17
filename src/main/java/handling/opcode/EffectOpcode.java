package handling.opcode;

import handling.WritableIntValueHolder;

public enum EffectOpcode implements WritableIntValueHolder {

    UserEffect_LevelUp(0x0),
    UserEffect_SkillUse(0x1),
    UserEffect_SkillUseBySummoned(0x2),
    UserEffect_SkillAffected(0x3),
    UserEffect_SkillAffected_Ex(0x5),
    UserEffect_SkillAffected_Select(0x6),
    UserEffect_SkillSpecialAffected(0x7),
    UserEffect_Quest(0x8),
    UserEffect_Pet(0x9),
    UserEffect_SkillSpecial(0x0A),
    UserEffect_Resist(0x0B),
    UserEffect_ProtectOnDieItemUse(0x0C),
    UserEffect_PlayPortalSE(0x0D),
    UserEffect_JobChanged(0x0E),
    UserEffect_QuestComplete(0x0F),
    UserEffect_IncDecHPEffect(0x10),
    UserEffect_BuffItemEffect(0x11),
    UserEffect_SquibEffect(0x12),
    UserEffect_MonsterBookCardGet(0x13),
    UserEffect_LotteryUse(0x14),
    UserEffect_ItemLevelUp(0x15),
    UserEffect_ItemMaker(0x16),
    UserEffect_ExpItemConsumed(0x18),
    UserEffect_FieldExpItemConsumed(0x19),
    UserEffect_ReservedEffect(0x1A),
    UserEffect_UpgradeTombItemUse(0x1B),
    UserEffect_BattlefieldItemUse(0x1C),
    UserEffect_AvatarOriented(0x1D),
    UserEffect_AvatarOrientedRepeat(0x1E),
    UserEffect_AvatarOrientedMultipleRepeat(0x1F),
    UserEffect_IncubatorUse(0x20),
    UserEffect_PlaySoundWithMuteBGM(0x21),
    UserEffect_PlayExclSoundWithDownBGM(0x22),
    UserEffect_SoulStoneUse(0x23),
    UserEffect_IncDecHPEffect_EX(0x24),
    UserEffect_IncDecHPRegenEffect(0x25),
    UserEffect_EffectUOL(0x26),
    UserEffect_PvPChampion(0x28),
    UserEffect_PvPGradeUp(0x29),
    UserEffect_PvPRevive(0x2A),
    UserEffect_JobEffect(0x2B),
    UserEffect_FadeInOut(0x2C),
    UserEffect_MobSkillHit(0x2D),
    UserEffect_AswanSiegeAttack(0x2E),
    UserEffect_BlindEffect(0x2F),
    UserEffect_BossShieldCount(0x30),
    UserEffect_ResetOnStateForOnOffSkill(0x30),
    UserEffect_JewelCraft(0x31),
    UserEffect_ConsumeEffect(0x32),
    UserEffect_PetBuff(0x33),
    UserEffect_LotteryUIResult(0x34),
    UserEffect_LeftMonsterNumber(0x35),
    UserEffect_ReservedEffectRepeat(0x36),
    UserEffect_RobbinsBomb(0x37),
    UserEffect_SkillMode(0x38),
    UserEffect_ActQuestComplete(0x39),
    UserEffect_Point(0x3A),
    UserEffect_SpeechBalloon(0x3B),
    UserEffect_TextEffect(0x3C),
    UserEffect_SkillPreLoopEnd(0x3D),
    UserEffect_Aiming(0x3E),
    UserEffect_PickUpItem(0x3F),
    UserEffect_BattlePvP_IncDecHp(0x40),
    UserEffect_BiteAttack_ReceiveSuccess(0x41),
    UserEffect_BiteAttack_ReceiveFail(0x42),
    BatteryON(0x4B),
    BatteryOFF(0x4C),
    HakuSkillEffect(0x4E);

    private int code;

    EffectOpcode(int code) {
        this.code = code;
    }

    @Override
    public short getValue() {
        return (short) code;
    }

    @Override
    public void setValue(short newval) {
        code = newval;
    }
}
