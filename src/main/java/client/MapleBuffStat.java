package client;

import handling.Buffstat;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum MapleBuffStat implements Serializable, Buffstat {

    NO_SLIP(999),
    FAMILIAR_SHADOW(999),
    子弹数量(999),
    取消天使(999, true),
    ICE_SKILL(999),
    FROZEN(999),
    INVINCIBILITY(999),
    精灵弱化(999),
    潜力解放(999),
    召唤兽(-1),
    时空门(-2),

    /*
     * ----------------------------
     */
    增加物理攻击力(0, true),
    增加魔法攻击力(1, true),
    提升物理防御力(2, true),
    增加最大HP(3, true),
    增加最大HP百分比(4, true),
    增加最大MP(5, true),
    增加最大MP百分比(6, true),
    增加命中值(7, true),
    增加回避值(8, true),
    增加跳跃力(9, true),
    增加移动速度(10, true),
    增加所有属性(11, true),
    增加经验值(13, true),
    提高攻击速度(14, true),
    提升伤害百分比(25, true),
    增加伤害最大值(27, true),
    增加状态异常抗性(28, true),
    增加所有属性抗性(29, true),
    /*
     * ----------------------------
     */
    暴击概率(30, true),
    防御概率(31, true),
    SECONDARY_STAT_IndieCrMax(32, true),
    SECONDARY_STAT_IndieBDR(33, true),
    终极契约(35, true),
    暴击最大伤害(36, true),
    增加物理攻击力百分比(38, true),
    增加魔法攻击力百分比(39, true),
    SECONDARY_STAT_IndiePMdR(42, true),
    SECONDARY_STAT_IndieQrPointTerm(46, true),
    SECONDARY_STAT_IndieShadowPartner(47, true),
    SECONDARY_STAT_IndieInvincible(48, true),
    SECONDARY_STAT_UNK49(49),
    黑暗姿势(999, true),
    增加伤害最大值百分比(999, true),
    经验获得(49, true), //T096 阿尔之好伙伴中的经验增加
    暴击最小伤害(50, true), //T096 阿尔之爪中的暴击最小伤害
    爆率增加(51, true), //T096 阿尔之窃取中的道具获得概率
    物理攻击(55),
    物理防御(56),
    魔法攻击(57),
    命中率(58),
    回避率(59),
    手技(60),
    /*
     * ----------------------------
     */
    移动速度(61),
    跳跃力(62),
    魔法盾(63),
    隐身术(64, false, true),
    攻击加速(65),
    伤害反击(66),
    MAXHP(67),
    MAXMP(68),
    无形箭弩(70),
    昏迷(71),
    中毒(72),
    封印(73),
    黑暗(74),
    斗气集中(75),
    龙之力(77),
    属性攻击(77),
    神圣祈祷(78),
    聚财术(79),
    //    召唤兽(0x2001, 3),
    影分身(80),
    敛财术(81, false, true),
    金钱护盾(82),
    HP_LOSS_GUARD(83),
    蓝血(84),
    虚弱(84),
    诅咒(85),
    缓慢(86),
    变身效果(87),
    恢复效果(88),
    冒险岛勇士(89),
    稳如泰山(90),
    火眼晶晶(91, false, true),
    /*
     * ----------------------------
     */
    魔法反击(92),
    诱惑(93),
    暗器伤人(94),
    终极无限(95),
    进阶祝福(96, false, true),
    额外回避(97),
    不死化(100),
    英雄回声(101),
    MESO_RATE(102, 1),
    GHOST_MORPH(103),
    ARIANT_COSS_IMU(104),
    混乱(105),
    SECONDARY_STAT_ItemUpByItem(106),
    SECONDARY_STAT_RespectPImmune(107),
    SECONDARY_STAT_RespectMImmune(108),
    SECONDARY_STAT_DefenseAtt(109),
    ILLUSION(110, 1),
    狂暴战魂(111),
    金刚霸体(112),
    闪光击(113),
    隐形剑(115),
    自然力重置(116),
    风影漫步(117),
    矛连击强化(119),
    连环吸血(120),
    战神抗压(122),
    天使状态(123),
    EXPRATE(124),
    无法使用药水(125),
    SHADOW(126),
    致盲(127),
    /*
     * ----------------------------
     */
    隐藏碎片(128, false, true),
    魔法屏障(129),
    抗魔领域(130),
    灵魂之石(131),
    飞翔(132),
    FREEZE(133),
    雷鸣冲击(134),
    葵花宝典(135),
    死亡猫头鹰(136),
    撤步退身(136),
    无敌状态(137),
    终极斩(138),
    DAMAGE_BUFF(139),
    ATTACK_BUFF(140),
    地雷(141),
    增强_MAXHP(142),
    增强_MAXMP(143),
    增强_物理攻击(144),
    增强_魔法攻击(145),
    增强_物理防御(146),
    //    增强_魔法防御(145),
    完美机甲(147),
    呼啸_爆击概率(149),
    呼啸_MaxMp增加(150),
    呼啸_伤害减少(151),
    呼啸_回避概率(152),
    嗨兄弟(155),
    /*
     * ----------------------------
     */
    潜入状态(156),
    金属机甲(157),
    幸运骰子(159),
    祝福护甲(160),
    反制攻击(161),
    移动精通(162),
    战斗命令(163),
    灵魂助力(164),
    DISABLE_POTENTIAL(165),
    巨人药水(166),
    SECONDARY_STAT_OnixDivineProtection(167),
    龙卷风(168),
    牧师祝福(169),
    SECONDARY_STAT_DisOrder(170),
    SECONDARY_STAT_Thread(171),
    SECONDARY_STAT_Team(172),
    SECONDARY_STAT_Explosion(173),
    SECONDARY_STAT_BuffLimit(174),
    增加力量(176),
    增加智力(177),
    增加敏捷(178),
    增加运气(179),
    SECONDARY_STAT_DarkTornado(181),
    增加_物理攻击(182),
    /*
     * ----------------------------
     */
    SECONDARY_STAT_PvPRaceEffect(185),
    SECONDARY_STAT_WeaknessMdamage(186),
    SECONDARY_STAT_Frozen2(187),
    SECONDARY_STAT_AmplifyDamage(188),
    SECONDARY_STAT_IceKnight(189),
    SECONDARY_STAT_Shock(190),
    无限精气(191),
    SECONDARY_STAT_IncMaxHP(192),
    SECONDARY_STAT_IncMaxMP(193),
    神圣魔法盾(194),
    神秘瞄准术(196),
    异常抗性(198),
    属性抗性(199),
    伤害吸收(200),
    黑暗变形(201),
    随机橡木桶(202),
    神圣拯救者的祝福(205),
    精神连接(203),
    爆击提升(206),
    DROP_RATE(207),
    SECONDARY_STAT_Event2(213),
    吸血鬼之触(214),
    防御力百分比(215),
    SECONDARY_STAT_DeathMark(218),
    /*
     * ----------------------------
     */
    死亡束缚(220),
    SECONDARY_STAT_VenomSnake(221),
    SECONDARY_STAT_CarnivalDefence(223),
    SECONDARY_STAT_PyramidEffect(226),
    击杀点数(227, true),
    SECONDARY_STAT_HollowPointBullet(228),
    SECONDARY_STAT_KeyDownMoving(229),
    百分比无视防御(230, false, true),
    召唤玩家A(231),
    神秘运气(231),
    幻影屏障(232),
    爆击概率增加(233),
    最小爆击伤害(234),
    卡牌审判(235),
    SECONDARY_STAT_DojangLuckyBonus(236),
    贯穿箭(236),
    SECONDARY_STAT_PainMark(237),
    SECONDARY_STAT_Magnet(238),
    SECONDARY_STAT_MagnetArea(239),
    幻灵招魂攻击状态(238),
    向导之箭(240),
    精灵元素(241, false, true),
    SECONDARY_STAT_VampDeath(242),
    SECONDARY_STAT_BlessingArmorIncPAD(243),
    SECONDARY_STAT_KeyDownAreaMoving(244),
    光暗转换_2,
    光暗转换(245),
    黑暗高潮(246),
    黑暗祝福(247),
    神圣保护(248),
    祝福护甲_增加物理攻击(248),
    生命潮汐(249),
    /*
     * ----------------------------
     */
    变形值(251),
    强健护甲(252),
    模式转换(253),
    SECONDARY_STAT_SpecialAction(254),
    SECONDARY_STAT_VampDeathSummon(255),
    剑刃之壁(256),
    灵魂凝视(257),
    伤害置换(259, false, true),
    天使亲和(260),
    三位一体(261),
    SECONDARY_STAT_BossShield(262),
    IDA_UNK_BUFF_3(263),
    SECONDARY_STAT_MobZoneState(264),
    SECONDARY_STAT_GiveMeHeal(265),
    SECONDARY_STAT_TouchMe(266),
    SECONDARY_STAT_Contagion(267),
    连击无限制(268),
    灵魂鼓舞(269),
    至圣领域(270),
    SECONDARY_STAT_IgnoreAllCounter(271),
    SECONDARY_STAT_IgnorePImmune(272),
    SECONDARY_STAT_IgnoreAllImmune(273),
    最终审判(275),
    PVP_ATTACK(276),
    PVP_DAMAGE(277),
    寒冰灵气(276),
    火焰灵气(277),
    天使复仇(278),
    天堂之门(279),
    战斗准备(280),
    /*
     * ----------------------------
     */
    流血剧毒(284),
    不倦神酒(285),
    阿修罗(286),
    超能光束炮(287),
    能量激发(288),
    混元归一(289),
    返回原位置(291),
    压制术(292),
    幸运钱(292),
    受到伤害减少百分比(293),
    BOSS伤害(294),
    精灵的帽子(295),
    超越攻击(296),
    恶魔恢复(297),
    恶魔超越(300),
    霰弹炮(301),
    终极攻击(302),
    SECONDARY_STAT_FireBomb(302),
    SECONDARY_STAT_HalfstatByDebuff(303),
    尖兵电力(304),
    SECONDARY_STAT_SetBaseDamage(305),
    全息力场(306),
    飞行骑乘(307),
    永动引擎(308),
    时间胶囊(309),
    元素属性(310),
    开天辟地(311),
    SECONDARY_STAT_EventPointAbsorb(312),
    ECONDARY_STAT_EventAssemble(313),
    暴风灭世(314),
    命中增加(315),
    回避增加(316),
    /*
     * ----------------------------
     */
    敏捷增加(999),
    元素灵魂(317),
    信天翁(317),
    神之子透明(318),
    月光转换(319),
    元素灵魂_僵直(321),
    日月轮转(322),
    灵魂武器(325),
    灵魂技能(326),
    SECONDARY_STAT_FullSoulMP(326),
    元素冲击(328),
    元气恢复(329),
    重生契约(332),
    交叉锁链(330, false, true),
    连环环破(331),  // 原: 地雷
    抗震防御(333),
    寒冰步(334),
    元素爆破(335),
    祈祷众生(336),
    SECONDARY_STAT_ComboCostInc(337),
    极限射箭(338),
    SECONDARY_STAT_NaviFlying(339),
    三彩箭矢(340),
    进阶箭筒(341),
    灵气大融合(342),
    SECONDARY_STAT_ImmuneBarrier(343),
    防甲穿透(344),
    缓速术(344),
    圣洁之力(345),
    神圣迅捷(346),
    暴击蓄能(347),
    /*
     * ----------------------------
     */
    神速衔接(348),
    集中精力(349),
    提速时刻_侦查(350),
    提速时刻_战斗(351),
    SECONDARY_STAT_CursorSniping(354),
    SECONDARY_STAT_DebuffTolerance(356),
    SECONDARY_STAT_DotHealHPPerSecond(357),
    招魂结界(358),
    九死一生(359),
    ACASH_RATE(999, 1),
    SECONDARY_STAT_SetBaseDamageByBuff(360),
    SECONDARY_STAT_LimitMP(361),
    SECONDARY_STAT_ReflectDamR(362),
    SECONDARY_STAT_ComboTempest(363),
    SECONDARY_STAT_MHPCutR(364),
    SECONDARY_STAT_MMPCutR(365),
    SECONDARY_STAT_SelfWeakness(366),
    元素黑暗(367),
    炎术引燃(369),
    黑暗领地(370),
    火焰庇佑(371),
    火焰屏障(374),
    影子侍从(375),
    黑暗幻影(376),
    SECONDARY_STAT_KnockBack(377),
    火焰咆哮(378),
    SECONDARY_STAT_ComplusionSlant(379),
    /*
     * ----------------------------
     */
    召唤美洲豹(380),
    SECONDARY_STAT_SSFShootingAttack(382),
    战法灵气(385, false, true),
    黑暗闪电(386),
    战斗大师(387),
    死亡契约(388),
    宙斯盾系统(393),
    灵魂吸取专家(394),
    灵狐(395),
    影子蝙蝠(396),
    刺客标记(397),
    燎原之火(398),
    花炎结界(399),
    SECONDARY_STAT_ChangeFoxMan(400),
    神圣归一(401),
    SECONDARY_STAT_UNK403(403),
    SECONDARY_STAT_UNK404(404),
    结合灵气(406),
    SECONDARY_STAT_BattlePvP_Helena_WindSpirit(407),
    SECONDARY_STAT_BattlePvP_LangE_Protection(408),
    SECONDARY_STAT_BattlePvP_LeeMalNyun_ScaleUp(409),
    SECONDARY_STAT_PinkbeanAttackBuff(411),
    影朋小白(412),
    SECONDARY_STAT_PinkbeanRollingGrade(413),
    SECONDARY_STAT_PinkbeanYoYoStack(414),
    SECONDARY_STAT_RandAreaAttack(415),
    皇家守护(421),
    皇家守护防御(422),
    重生符文(423),
    灵魂链接(423),
    光之守护(424),
    狂风肆虐(425),
    PP(427),
    心魂附体(428),
    心魂之盾(429),
    临界(430),
    心魂本能(431),
    不消耗HP(432),
    限制HP恢复(436),
    UNK_MBS_437(437),
    激素狂飙(443),
    重击研究(444),
    生命吸收(445),
    瞄准猎物(446),
    爆破弹夹(448),
    组合训练(449),
    避柳(449),
    UNK_MBS_450(450),
    SECONDARY_STAT_UNK451(451),
    忍耐之盾(452),
    极限火炮(454),
    气缸过热(455),
    UNK_MBS_456(456),
    急速闪避(457),
    戴米安刻印(458),
    装备摩诃(459),
    激素引擎(461),
    精准火箭(462),
    超人变形(464),
    爱星能量(465),
    心雷合一(466),
    子弹盛宴(467),
    SECONDARY_STAT_UNK466(468),
    变换攻击(469),
    祈祷(470),
    灵气武器(473),
    超压魔力(474),
    迅速(474),
    神奇圆环(475),
    SECONDARY_STAT_UNK476(476),
    多向飞镖(477),
    呼啸暴风(478),
    宇宙无敌火炮弹(479),
    暗影抨击(480),
    恶魔狂怒(485),
    SECONDARY_STAT_UNK497(497),
    结界破魔(501),
    拔刀姿势(502, false, true),
    拔刀术加成(503),
    厚积薄发(513),
    增加攻击力(506),
    增加HP百分比(507),
    增加MP百分比(508),
    一闪(510),
    HAKU_BLESS(511),
    UNK_MBS_512(512),
    晓月流基本技能(514),
    UNK_MBS_472(499),
    /*
     * ----------------------------
     */
    守护模式变更(491),
    舞力全开(492),
    IDA_SPECIAL_BUFF5(498),
    SECONDARY_STAT_UNK513(513),
    SECONDARY_STAT_UNK514(514),
    SECONDARY_STAT_UNK515(515),
    水枪阵营(516),
    水枪军阶(517),
    水枪连击(518),
    水枪效果(519),
    SECONDARY_STAT_UNK520(520),
    SECONDARY_STAT_UNK521(521),
    能量获得(525, true),
    疾驰速度(526, true),
    疾驰跳跃(527, true),
    骑兽技能(528, true),
    极速领域(529, true),
    导航辅助(530, true),
    SECONDARY_STAT_Undead(531, true),
    SECONDARY_STAT_RideVehicleExpire(532, true),
    SECONDARY_STAT_COUNT_PLUS1(533, true),;


    private final static HashMap<MapleBuffStat, Integer> spawnStatsList = new HashMap<>();
    private static final long serialVersionUID = 0L;

    static {
        spawnStatsList.put(SECONDARY_STAT_UNK49, 0);
        spawnStatsList.put(SECONDARY_STAT_PyramidEffect, 0);
        spawnStatsList.put(击杀点数, 0);
        spawnStatsList.put(圣洁之力, 0);
        spawnStatsList.put(神圣迅捷, 0);
        spawnStatsList.put(战法灵气, 0);
        spawnStatsList.put(结合灵气, 0);
        spawnStatsList.put(SECONDARY_STAT_BattlePvP_LangE_Protection, 0);
        spawnStatsList.put(SECONDARY_STAT_PinkbeanRollingGrade, 0);
        spawnStatsList.put(激素狂飙, 0);
        spawnStatsList.put(忍耐之盾, 0);
        spawnStatsList.put(SECONDARY_STAT_UNK476, 0);
        spawnStatsList.put(能量获得, 0);
        spawnStatsList.put(疾驰速度, 0);
        spawnStatsList.put(疾驰跳跃, 0);
        spawnStatsList.put(骑兽技能, 0);
        spawnStatsList.put(极速领域, 0);
        spawnStatsList.put(导航辅助, 0);
        spawnStatsList.put(SECONDARY_STAT_Undead, 0);
        spawnStatsList.put(SECONDARY_STAT_RideVehicleExpire, 0);
    }

    private int value;
    private int pos;
    private boolean stacked = false;
    private boolean special = false;
    private int isShow = 0;
    private SerializeSpawn serializeSpawn = null;

    MapleBuffStat() {
    }

    MapleBuffStat(int value) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
    }

    MapleBuffStat(int value, int isShow) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
        this.isShow = isShow;
    }

    MapleBuffStat(int value, boolean stacked) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
        this.stacked = stacked;
    }

    MapleBuffStat(int value, boolean stacked, int isShow) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
        this.stacked = stacked;
        this.isShow = isShow;
    }

    MapleBuffStat(int value, boolean stacked, boolean special) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
        this.stacked = stacked;
        this.special = special;
    }

    MapleBuffStat(int value, boolean stacked, boolean special, int isShow) {
        this.value = 1 << 31 - value % 32;
        this.pos = (int) Math.floor(value / 32);
        this.stacked = stacked;
        this.special = special;
        this.isShow = isShow;
    }

    public static Map<MapleBuffStat, Integer> getSpawnList() {
        return spawnStatsList;
    }

    public static List<MapleBuffStat> getSpawnList(MapleCharacter chr) {
        return chr.getBuffManager().getSpawnList(getSpawnList());
    }

    @Override
    public int getPosition() {
        return pos;
    }

    public int getPosition(boolean fromZero) {
        if (!fromZero) {
            return pos;
        }
        if (pos > 0) {
            return pos - 1;
        }
        return 0;
    }

    @Override
    public int getValue() {
        return value;
    }

    public int getValue(boolean foreign, boolean give) {
        int value = 1 << pos;
        if (!foreign) {// 加给自己的
            if (give) {// 加

            } else {// 消除

            }
        } else {// 加给别人的。
            if (give) {// 加
                switch (this) {
                    case 跳跃力:
                        value = 0;
                        break;
                }
            } else {// 消除
            }
        }
        return value;
    }

    public boolean canStack() {
        return stacked;
    }

    public boolean isSpecial() {
        return special;
    }

    public boolean isShow() {
        return isShow > 0;
    }

    public SerializeSpawn getSerializeSpawn() {
        if (serializeSpawn == null) {
            maskserializeSpawn();
        }
        return serializeSpawn;
    }

    private void maskserializeSpawn() {
        switch (this) {
            case 隐身术: {
                serializeSpawn = (m, chr) -> m.write(chr.getBuffedValue(MapleBuffStat.隐身术));
                break;
            }
            case 无形箭弩: {
                serializeSpawn = (m, chr) -> m.write(chr.getBuffedValue(MapleBuffStat.无形箭弩));
                break;
            }
            case 斗气集中: {
                serializeSpawn = (m, chr) -> m.write(chr.getBuffedValue(MapleBuffStat.斗气集中));
                break;
            }
            case 属性攻击: {
                serializeSpawn = (m, chr) -> {
                    m.writeShort(chr.getBuffedValue(MapleBuffStat.属性攻击));
                    m.writeInt(-chr.getBuffSource(MapleBuffStat.属性攻击));
                };
                break;
            }
            case 移动速度: {
                serializeSpawn = (m, chr) -> m.write(chr.getBuffedValue(MapleBuffStat.移动速度));
                break;
            }
            case 变身效果: {
                serializeSpawn = (m, chr) -> {
                    m.writeShort(chr.getBuffedValue(MapleBuffStat.变身效果));
                    m.writeInt(chr.getBuffSource(MapleBuffStat.变身效果));
                };
                break;
            }
            case 巨人药水: {
                serializeSpawn = (m, chr) -> {
                    m.writeShort(chr.getBuffedValue(MapleBuffStat.巨人药水));
                    m.writeInt(-chr.getBuffSource(MapleBuffStat.巨人药水));
                };
                break;
            }
            default:
                break;
        }
    }

    public interface SerializeSpawn {

        void Serialize(MaplePacketLittleEndianWriter mplew, MapleCharacter chr);
    }
}
