package client.status;

import client.MapleDisease;
import constants.skills.尖兵;
import handling.Buffstat;

import java.io.Serializable;

/**
 * @author setosan
 */
public enum MonsterStatus implements Serializable, Buffstat {

    MOB_STAT_PAD(0),
    MOB_STAT_PDR(1),
    MOB_STAT_MAD(2),
    MOB_STAT_MDR(3),
    MOB_STAT_ACC(4),
    MOB_STAT_EVA(5),
    MOB_STAT_Speed(6),
    MOB_STAT_Stun(7),
    MOB_STAT_Freeze(8),
    MOB_STAT_Poison(9),
    MOB_STAT_Seal(10),
    MOB_STAT_Darkness(11),
    MOB_STAT_PowerUp(12),
    MOB_STAT_MagicUp(13),
    MOB_STAT_PGuardUp(14),
    MOB_STAT_MGuardUp(15),
    MOB_STAT_PImmune(16),
    MOB_STAT_MImmune(17),
    MOB_STAT_Web(18),
    MOB_STAT_HardSkin(19),
    MOB_STAT_Ambush(20),
    MOB_STAT_Venom(21),
    MOB_STAT_Venom_2(21),
    MOB_STAT_Blind(22),
    心灵控制(23),
    MOB_STAT_SealSkill(23),
    MOB_STAT_Dazzle(24),
    MOB_STAT_PCounter(25),
    MOB_STAT_MCounter(26),
    MOB_STAT_RiseByToss(27),
    MOB_STAT_BodyPressure(28),
    MOB_STAT_Weakness(29),
    MOB_STAT_Showdown(30),
    MOB_STAT_MagicCrash(31),
    MOB_STAT_DamagedElemAttr(32),
    MOB_STAT_Dark(33, true),
    MOB_STAT_Mystery(34, true),
    MOB_STAT_AddDamParty(35),
    MOB_STAT_HitCriDamR(36),
    MOB_STAT_Fatality(37),
    MOB_STAT_Lifting(38),
    MOB_STAT_DeadlyCharge(39),
    MOB_STAT_Smite(40),
    MOB_STAT_AddDamSkil(41),
    MOB_STAT_Incizing(42),
    MOB_STAT_DodgeBodyAttack(43),
    MOB_STAT_DebuffHealing(44),
    MOB_STAT_AddDamSkill2(45),
    MOB_STAT_BodyAttack(46),
    MOB_STAT_TempMoveAbility(47),
    MOB_STAT_FixDamRBuff(48),
    MOB_STAT_ElementDarkness(49),
    MOB_STAT_AreaInstallByHit(50),
    MOB_STAT_BMageDebuff(51),
    MOB_STAT_JaguarProvoke(52),
    MOB_STAT_JaguarBleeding(53),
    MOB_STAT_DarkLightning(54),
    MOB_STAT_PinkbeanFlowerPot(55),
    MOB_STAT_BattlePvP_Helena_Mark(56),
    MOB_STAT_PsychicLock(57),
    MOB_STAT_PsychicLockCoolTime(58),
    MOB_STAT_PsychicGroundMark(59),
    MOB_STAT_PowerImmune(60),
    MOB_STAT_PsychicForce(61),
    MOB_STAT_MultiPMDR(62),
    MOB_STAT_ElementResetBySummon(63),
    MOB_STAT_BahamutLightElemAddDam(64),
    MOB_STAT_BossPropPlus(65),
    MOB_STAT_MultiDamSkill(66),
    MOB_STAT_RWLiftPress(67),
    MOB_STAT_RWChoppingHammer(68),
    MOB_STAT_TimeBomb(70),
    MOB_STAT_Treasure(71, true),
    MOB_STAT_AddEffect(72),
    MOB_STAT_Invincible(73, true),
    MOB_STAT_Explosion(74),
    MOB_STAT_HangOver(75),
    MOB_STAT_Burned(76, true),
    MOB_STAT_BalogDisable(77, true),
    MOB_STAT_ExchangeAttack(78, true),
    MOB_STAT_AddBuffStat(79, true),
    MOB_STAT_LinkTeam(80, true),
    MOB_STAT_SoulExplosion(81, true),
    MOB_STAT_SeperateSoulP(82, true),
    MOB_STAT_SeperateSoulC(83, true),
    MOB_STAT_Ember(84, true),
    MOB_STAT_TrueSight(85, true),
    MOB_STAT_Laser(86, true),
    MOB_STAT_StatResetSkill(87, true),
    MOB_STAT_COUNT(88, true),
    MOB_STAT_NONE(-1);

    static final long serialVersionUID = 0L;
    private final int i;
    private final int position;
    private final boolean end;
    private int value;

    MonsterStatus(int value) {
        this.i = 1 << 31 - value % 32;
        this.position = (int) Math.floor(value / 32);
        this.end = false;
        this.value = value;
    }

    MonsterStatus(int value, boolean end) {
        this.i = 1 << 31 - value % 32;
        this.position = (int) Math.floor(value / 32);
        this.end = end;
        this.value = value;
    }

    public static MonsterStatus getBySkill_Pokemon(int skill) {
        switch (skill) {
            case 120:
                return MOB_STAT_Seal;
            case 121:
                return MOB_STAT_Blind;
            case 123:
                return MOB_STAT_Stun;
            case 125:
                return MOB_STAT_Poison;
            case 126:
                return MOB_STAT_Speed;
            case 137:
                return MOB_STAT_Freeze;
        }
        return null;
    }

    public static MapleDisease getLinkedDisease(MonsterStatus stat) {
        switch (stat) {
            case MOB_STAT_Stun:
            case MOB_STAT_Web:
                return MapleDisease.昏迷;
            case MOB_STAT_Poison:
            case 心灵控制:
                return MapleDisease.中毒;
            case MOB_STAT_Seal:
            case MOB_STAT_MagicCrash:
                return MapleDisease.封印;
            case MOB_STAT_Freeze:
                return MapleDisease.FREEZE;
            case MOB_STAT_PCounter:
                return MapleDisease.黑暗;
            case MOB_STAT_Speed:
                return MapleDisease.缓慢;
        }
        return null;
    }

    public static int genericSkill(MonsterStatus stat) {
        switch (stat) {
            case MOB_STAT_Stun: {
                return 90001001;
            }
            case MOB_STAT_Speed: {
                return 90001002;
            }
            case MOB_STAT_Poison: {
                return 90001003;
            }
            case MOB_STAT_PCounter: {
                return 90001004;
            }
            case MOB_STAT_Seal: {
                return 90001005;
            }
            case MOB_STAT_Freeze: {
                return 90001006;
            }
            case MOB_STAT_MagicCrash: {
                return 1111007;
            }
            case MOB_STAT_Darkness: {
                return 4121003;
            }
            case MOB_STAT_Weakness: {
                return 22161002;
            }
            case MOB_STAT_Web: {
                return 4111003;
            }
            case MOB_STAT_Venom: {
                return 5211004;
            }
            case MOB_STAT_Venom_2: {
                return 2311005;
            }
            case MOB_STAT_Ambush: {
                return 4121004;
            }
            case MOB_STAT_Explosion: {
                return 尖兵.三角进攻;
            }
        }
        return 0;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public boolean isEmpty() {
        return end;
    }

    @Override
    public int getValue() {
        return i;
    }

    public int getOrValue() {
        return value;
    }

    public Integer getOrder() {
        return position;
    }

    public MonsterStatus getMonsterStatusById(int id) {
        for (MonsterStatus monsterStatus : MonsterStatus.values()) {
            if (monsterStatus.getValue() == id) {
                return monsterStatus;
            }
        }
        return null;
    }
}
