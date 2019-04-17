package constants;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.PlayerStats;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.skills.*;

public class SkillConstants {

    public static boolean isBuffSkill(int skillid, boolean isbuff) {
        switch (skillid) {
            case 主教.群体治愈:
            case 火毒.致命毒雾:
            case 唤灵斗师.避难所:
            case 火毒.末日烈焰:
                //case 火毒.创世之破:
                //case 冰雷.创世之破:
            case 主教.创世之破:
            case 1076: //奥兹的火牢术屏障
            case 双刀.龙卷风:
            case 神之子.暴风步:
                isbuff = false;
                break;
            case 1004: //骑兽技能
            case 10001004: //骑兽技能
            case 20001004: //骑兽技能
            case 20011004: //骑兽技能
            case 80001000: //骑兽技能
            case 1026: //飞翔
            case 10001026: //飞翔
            case 20001026: //飞翔
            case 20011026: //飞翔
            case 20021026: //飞翔
            case 30001026: //飞翔
            case 30011026: //飞翔
            case 93: //潜力解放(冒险家)
            case 10000093: //潜力解放(骑士团)
            case 20000093: //潜力解放(战神)
            case 20010093: //潜力解放(龙神)
            case 20020093: //潜力解放（英雄）
            case 30000093: //潜力解放(反抗者)
            case 30010093: //潜力解放（反抗者）
            case 9101004: // hide is a buff -.- atleast for us o.o"
            case 80001140: //光之守护 - [排名技能]受到光之骑士米哈尔的庇护，在一定时间内，即使受到敌人攻击也不会被击退。
            case 80001242:
            case 英雄.斗气集中:
            case 神射手.无形箭:
            case 神射手.极限射箭:
            case 神射手.箭雨:
            case 箭神.无形箭:
            case 箭神.极限射箭:
            case 箭神.伤害置换:
            case 侠盗.暴击蓄能:
            case 侠盗.敛财术:
            case 侠盗.侠盗本能:
            case 侠盗.暗杀:
            case 风灵使者.狂风肆虐Ⅰ:
            case 风灵使者.狂风肆虐Ⅱ:
            case 风灵使者.狂风肆虐Ⅲ:
            case 风灵使者.重振精神:
            case 风灵使者.呼啸暴风:
            case 战神.抗压:
            case 战神.激素引擎:
            case 海盗.疾驰:
            case 奇袭者.疾风:
            case 冲锋队长.能量获得:
            case 冲锋队长.极速领域:
            case 龙神.自然力重置:
            case 龙神.魔法狂暴:
            case 龙神.抗魔领域:
            case 龙神.冒险岛勇士:
            case 龙神.勇士的意志:
            case 龙神.玛瑙的祝福:
            case 龙神.玛瑙的意志:
            case 龙神.龙神:
                //case 龙神.冰点寒气:
                //case 龙神.火焰喷射:
            case 黑骑士.灵魂祝福:
            case 黑骑士.灵魂助力震惊:
            case 机械师.终极机甲:
            case 机械师.金属机甲_人类:
            case 机械师.金属机甲_战车:
            case 管理员.隐藏术:
            case 双刀.终极斩:
            case 唤灵斗师.黑暗灵气:
            case 唤灵斗师.蓝色灵气:
            case 唤灵斗师.黄色灵气:
            case 唤灵斗师.减益灵气:
            case 唤灵斗师.吸收灵气:
            case 唤灵斗师.暴怒对战:
            case 唤灵斗师.黑暗闪电:
            case 唤灵斗师.死亡:
            case 机械师.完美机甲:
            case 英雄.魔击无效:
            case 圣骑士.魔击无效:
            case 黑骑士.魔击无效:
            case 冲锋队长.幸运骰子:
            case 冲锋队长.双幸运骰子:
            case 船长.幸运骰子:
            case 船长.双幸运骰子:
            case 船长.指挥船员:
            case 神炮王.幸运骰子:
            case 神炮王.双幸运骰子:
            case 机械师.幸运骰子:
            case 机械师.双幸运骰子:
            case 冲锋队长.反制攻击:
            case 船长.反制攻击:
            case 圣骑士.祝福护甲:
            case 豹弩游侠.呼啸:
            case 豹弩游侠.辅助打猎单元:
            case 豹弩游侠.集束箭:
            case 豹弩游侠.撤步退身:
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
            case 豹弩游侠.利爪狂风:
            case 豹弩游侠.激怒:
            case 豹弩游侠.十字攻击:
            case 豹弩游侠.毁灭音爆:
            case 豹弩游侠.美洲豹灵魂:
            case 机械师.战争机器_泰坦:
            case 机械师.机器人工厂_RM1:
            case 机械师.机器人发射器_RM7:
            case 机械师.支援波动器_H_EX:
            case 机械师.磁场:
            case 机械师.传送门_GX9:
            case 火毒.燎原之火:
            case 火毒.神秘瞄准术:
            case 火毒.火焰灵气:
            case 火毒.制裁火球:
            case 冰雷.神秘瞄准术:
            case 冰雷.寒冰灵气:
            case 冰雷.极冻吐息:
            case 主教.魔力精通:
            case 主教.神秘瞄准术:
            case 神炮王.磁性船锚:
            case 神炮王.双胞胎猴子支援:
            case 80001089: //飞翔·
            case 恶魔猎手.黑暗变形:
            case 幻影.神秘的运气:
            case 幻影.卡牌审判:
            case 幻影.卡牌审判_高级:
            case 双刀.影子闪避:
            case 船长.无尽追击:
            case 夜光.黑暗祝福:
            case 夜光.生命潮汐:
            case 夜光.记录:
            case 尖兵.精准火箭:
            case 尖兵.急速支援:
            case 尖兵.宙斯盾系统:
            case 神射手.三彩箭矢:
            case 狂龙战士.石化:
            case 狂龙战士.石化_变身:
            case 狂龙战士.防御模式:
            case 狂龙战士.攻击模式:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
            case 狂龙战士.剑刃之壁_变身:
            case 狂龙战士.进阶剑刃之壁_变身:
            case 恶魔复仇者.血之契约:
            case 恶魔复仇者.超越:
            case 恶魔复仇者.超越十字斩:
            case 恶魔复仇者.超越恶魔突袭:
            case 恶魔复仇者.超越月光斩:
            case 恶魔复仇者.超越处决:
            case 圣骑士.元素冲击:
            case 圣骑士.连环环破:
            case 炎术士.元素_火焰:
            case 炎术士.元素_火焰II:
            case 炎术士.元素_火焰III:
            case 炎术士.元素_火焰IV:
            case 炎术士.大漩涡:
            case 炎术士.希纳斯的骑士:
            case 夜行者.黑暗领地:
            case 冰雷.寒冰步:
            case 双刀.阿修罗:
            case 80001427:
            case 80001428:
            case 80001430:
            case 80001432:
            case 林之灵.伊卡飞翔:
            case 林之灵.阿尔之好伙伴:
            case 林之灵.阿尔之窃取:
            case 林之灵.阿尔之爪:
            case 林之灵.阿尔之魅力_强化:
            case 林之灵.阿尔之弱点把握:
            case 林之灵.阿尔之饱腹感:
            case 林之灵.红色卡片:
            case 林之灵.蓝色卡片:
            case 林之灵.绿色卡片:
            case 林之灵.金色卡片:
            case 林之灵.火焰屁:
            case 林之灵.嗨_兄弟:
            case 神之子.提速时刻_侦查:
            case 神之子.提速时刻_战斗:
            case 隐月.灵狐:
            case 隐月.招魂结界:
            case 剑豪.基本姿势加成:
            case 剑豪.拔刀术加成:
            case 剑豪.拔刀姿势:
            case 剑豪.秘剑_隼:
            case 剑豪.武神招来:
            case 剑豪.刚健:
            case 剑豪.晓之勇者:
            case 剑豪.晓之樱:
            case 剑豪.厚积薄发:
            case 剑豪.避柳:
            case 剑豪.迅速:
            case 阴阳师.紫扇白狐:
            case 阴阳师.影朋_小白:
            case 超能力者.心魂漫步:
            case 爆莉萌天使.终极契约:
                isbuff = true;
                break;
        }
        return isbuff;
    }

    public static int isFinisher(int skillid) {
        switch (skillid) {
            case 英雄.狂澜之力:
                return 1;
            case 英雄.恐慌:
                return 2;
            case 英雄.烈焰冲斩:
                return 4;
        }
        return 0;
    }

    public static boolean isMagicChargeSkill(int skillid) {
        switch (skillid) {
            case 2321001:
            case 龙神.雷电俯冲_攻击:
            case 阴阳师.破邪连击符:
                return true;
            default:
                Skill skill = SkillFactory.getSkill(skillid);
                return skill != null && skill.isChargingSkill();
        }
    }

    public static boolean isRecoveryIncSkill(int skillId) {
        switch (skillId) {
            case 英雄.自我恢复:
            case 狂龙战士.自我恢复:
            case 米哈尔.自我恢复:
                return true;
        }
        return false;
    }

    public static boolean isLinkedAttackSkill(int skillId) {
        return getLinkedAttackSkill(skillId) != skillId;
    }

    public static int getLinkedAttackSkill(int skillId) {
        switch (skillId) {
            case 黑骑士.重生契约_状态:
                return 黑骑士.重生契约;
            case 英雄.终极打击_爆击:
                return 英雄.终极打击;
            case 隐士.刺客标记_飞镖:
                return 隐士.刺客标记;
            case 隐士.隐士标记_飞镖:
                return 隐士.隐士标记;
            case 侠盗.暗杀_1:
                return 侠盗.暗杀;
            case 侠盗.金钱炸弹_攻击:
                return 侠盗.金钱炸弹;
            case 弓箭手.二阶跳_2:
            case 弓箭手.二阶跳_3:
            case 弓箭手.二阶跳_4:
                return 弓箭手.二阶跳;
            case 神射手.箭矢炮盘_攻击:
                return 神射手.箭矢炮盘;
            case 神射手.暴风箭雨_四转:
                return 神射手.进阶终极攻击; //test
            case 神射手.三彩箭矢_魔法:
                return 神射手.三彩箭矢;
            case 神射手.进阶箭筒_魔法:
                return 神射手.进阶箭筒;
            case 海盗.双弹射击1:
                return 海盗.双弹射击;
            case 冲锋队长.能量旋风:
                return 冲锋队长.龙卷风拳;
            case 冲锋队长.爆能破袭:
                return 冲锋队长.能量爆破;
            case 冲锋队长.碎石乱击_1:
                return 冲锋队长.碎石乱击;
            case 冲锋队长.潜龙出渊_1:
                return 冲锋队长.潜龙出渊;
            case 冲锋队长.暴怒拳:
                return 冲锋队长.激怒拳;
            case 冲锋队长.双重爆炸:
                return 冲锋队长.能量爆炸;
            case 船长.轻羽鞋_下落:
                return 船长.轻羽鞋;
            case 船长.召唤船员2:
            case 船长.召唤船员3:
                return 船长.召唤船员;
            case 船长.集合船员2:
            case 船长.集合船员3:
            case 船长.集合船员4:
                return 船长.集合船员;
            case 船长.战船轰炸机:
            case 船长.战舰炮轰_1:
            case 船长.战舰炮轰_2:
            case 船长.战舰炮轰_3:
                return 船长.指挥船员;
            case 神炮王.猴子爆弹_爆炸:
                return 神炮王.猴子爆弹;
            case 战神.粉碎重击_2:
                return 战神.粉碎重击;
            case 战神.粉碎浪潮_2:
                return 战神.粉碎浪潮;
            case 战神.战神突进_2:
                return 战神.战神突进;
            case 战神.终极投掷_2:
                return 战神.终极投掷;
            case 战神.旋风_2:
                return 战神.旋风;
            case 战神.高空重击_2:
            case 战神.高空重击_3:
                return 战神.高空重击;
            case 战神.摩诃审判_2:
            case 战神.摩诃审判_3:
            case 战神.摩诃审判_4:
                return 战神.摩诃审判;
            case 战神.聚集捕手_2:
                return 战神.聚集捕手;
            case 战神.加速终端_恐惧风暴_2:
            case 战神.加速终端_恐惧风暴_3:
                return 战神.加速终端_恐惧风暴;
            case 战神.加速终端_瞄准猎物_2:
                return 战神.加速终端_瞄准猎物;
            case 战神.巨熊咆哮_2:
            case 战神.巨熊咆哮_3:
            case 战神.巨熊咆哮_4:
            case 战神.巨熊咆哮_5:
                return 战神.巨熊咆哮;
            case 战神.比昂德_2击:
            case 战神.比昂德_3击:
                return 战神.比昂德;
            case 唤灵斗师.如意棒_第2击:
                return 唤灵斗师.如意棒;
            case 神炮王.猴子炸药桶_爆炸:
                return 神炮王.猴子炸药桶;
            case 神炮王.双胞胎猴子支援_1:
                return 神炮王.双胞胎猴子支援;
            case 双弩.冲锋拳1:
                return 双弩.冲锋拳;
            case 双弩.精灵骑士1:
            case 双弩.精灵骑士2:
                return 双弩.精灵骑士;
            case 恶魔猎手.恶魔之翼1:
                return 恶魔猎手.恶魔之翼;
            case 恶魔猎手.恶魔跳跃1:
            case 恶魔猎手.恶魔跳跃2:
            case 恶魔猎手.恶魔跳跃3:
                return 恶魔猎手.恶魔跳跃;
            case 恶魔猎手.恶魔血月斩1:
            case 恶魔猎手.恶魔血月斩2:
            case 恶魔猎手.恶魔血月斩3:
                return 恶魔猎手.恶魔血月斩;
            case 恶魔猎手.恶魔爆炸1:
                return 恶魔猎手.恶魔爆炸;
            case 幻影.幻影突击1:
                return 幻影.幻影突击;
            case 幻影.暮光祝福1:
                return 幻影.暮光祝福;
            case 龙神.龙飞行:
                return 80001000;
            case 夜光.晨星坠落_爆炸:
                return 夜光.晨星坠落;
            case 狂龙战士.飞龙斩_1:
            case 狂龙战士.飞龙斩_2:
            case 狂龙战士.飞龙斩_变身_3转:
            case 狂龙战士.飞龙斩_变身_4转:
                return 狂龙战士.飞龙斩;
            case 狂龙战士.扇击_变身:
            case 狂龙战士.扇击_变身_2:
                return 狂龙战士.扇击;
            case 狂龙战士.石化_变身:
                return 狂龙战士.石化;
            case 狂龙战士.重拾力量_额外攻击:
                return 狂龙战士.重拾力量;
            case 狂龙战士.剑气突袭_变身:
            case 狂龙战士.剑气突袭_爆发:
            case 狂龙战士.剑气突袭_爆发_变身:
                return 狂龙战士.剑气突袭;
            case 狂龙战士.终极变形_4转:
                return 狂龙战士.终极变形_3转;
            case 狂龙战士.天空剑影_变身:
                return 狂龙战士.天空剑影;
            case 狂龙战士.怒雷屠龙斩_变身:
                return 狂龙战士.怒雷屠龙斩;
            case 狂龙战士.剑刃之壁_变身:
                return 狂龙战士.剑刃之壁;
            case 狂龙战士.进阶剑刃之壁_变身:
                return 狂龙战士.进阶剑刃之壁;
            case 狂龙战士.恶魔之息_变身:
                return 狂龙战士.恶魔之息;
            case 爆莉萌天使.三位一体_2击:
            case 爆莉萌天使.三位一体_3击:
                return 爆莉萌天使.三位一体;
            case 恶魔复仇者.超越十字斩_1:
            case 恶魔复仇者.超越十字斩_2:
            case 恶魔复仇者.超越十字斩_3:
            case 恶魔复仇者.超越十字斩_4:
                return 恶魔复仇者.超越十字斩;
            case 恶魔复仇者.超越恶魔突袭_1:
            case 恶魔复仇者.超越恶魔突袭_2:
            case 恶魔复仇者.超越恶魔突袭_3:
            case 恶魔复仇者.超越恶魔突袭_4:
                return 恶魔复仇者.超越恶魔突袭;
            case 恶魔复仇者.超越月光斩_1:
            case 恶魔复仇者.超越月光斩_2:
            case 恶魔复仇者.超越月光斩_3:
            case 恶魔复仇者.超越月光斩_4:
                return 恶魔复仇者.超越月光斩;
            case 恶魔复仇者.持盾突击_1:
                return 恶魔复仇者.持盾突击;
            case 恶魔复仇者.超越处决_1:
            case 恶魔复仇者.超越处决_2:
            case 恶魔复仇者.超越处决_3:
            case 恶魔复仇者.超越处决_4:
                return 恶魔复仇者.超越处决;
            case 恶魔复仇者.追击盾_攻击:
                return 恶魔复仇者.追击盾;
            case 尖兵.银色快剑_集中:
            case 尖兵.银色快剑_跳跃:
                return 尖兵.银色快剑_闪光;
            case 尖兵.战斗切换_击落:
            case 尖兵.战斗切换_分裂:
                return 尖兵.战斗切换_爆炸;
            case 尖兵.全息力场_力场:
            case 尖兵.全息力场_支援:
                return 尖兵.全息力场_穿透;
            case 尖兵.聚能脉冲炮_炮击:
            case 尖兵.聚能脉冲炮_暴击:
                return 尖兵.聚能脉冲炮_狙击;
            case 尖兵.宙斯盾系统_攻击:
                return 尖兵.宙斯盾系统;
            case 魂骑士.猛袭:
                return 魂骑士.瞬步;
            case 魂骑士.灼影之焰:
                return 魂骑士.摄魂斩;
            case 魂骑士.月影斩:
                return 魂骑士.光速突击;
            case 魂骑士.月光十字斩:
                return 魂骑士.日光十字斩;
            case 魂骑士.人剑合一_旭日:
                return 魂骑士.人剑合一;
            case 魂骑士.日月轮转_月光洒落:
            case 魂骑士.日月轮转_旭日:
                return 魂骑士.日月轮转;
            case 魂骑士.极速霞光:
            case 魂骑士.极速霞光_空中:
            case 魂骑士.月光之舞_空中:
                return 魂骑士.月光之舞;
            case 魂骑士.新月斩:
                return 魂骑士.烈日之刺;
            case 魂骑士.落魂剑_傀儡:
                return 魂骑士.落魂剑;
            case 魂骑士.冥河破_爆破:
                return 魂骑士.冥河破;
            case 炎术士.轨道烈焰_LINK:
                return 炎术士.轨道烈焰;
            case 炎术士.轨道烈焰II_LINK:
                return 炎术士.轨道烈焰II;
            case 炎术士.轨道烈焰III_LINK:
                return 炎术士.轨道烈焰III;
            case 炎术士.轨道烈焰IV_LINK:
                return 炎术士.轨道烈焰IV;
            case 炎术士.灭绝之焰_LINK:
                return 炎术士.灭绝之焰;
            case 炎术士.火焰屏障_LINK:
                return 炎术士.火焰屏障;
            case 炎术士.火焰化身_狮子:
            case 炎术士.火焰化身_狐狸:
                return 炎术士.火焰化身;
            case 风灵使者.狂风肆虐Ⅰ_攻击:
                return 风灵使者.狂风肆虐Ⅰ;
            case 风灵使者.狂风肆虐Ⅱ_攻击:
                return 风灵使者.狂风肆虐Ⅱ;
            case 风灵使者.狂风肆虐Ⅲ_攻击:
                return 风灵使者.狂风肆虐Ⅲ;
            case 风灵使者.旋风箭_溅射:
                return 风灵使者.旋风箭;
            case 夜行者.暗影大风车_爆炸:
                return 夜行者.暗影大风车;
            case 夜行者.三连环光击破_最后一击:
                return 夜行者.三连环光击破;
            case 夜行者.四连镖_最后一击:
                return 夜行者.四连镖;
            case 夜行者.五倍投掷_最后一击:
                return 夜行者.五倍投掷;
            case 夜行者.影子蝙蝠_攻击:
            case 夜行者.影子蝙蝠_反弹:
                return 夜行者.影子蝙蝠;
            case 爆莉萌天使.灵魂吸取_攻击:
                return 爆莉萌天使.灵魂吸取;
            case 神之子.暴风闪:
                return 神之子.暴风步;
            case 神之子.进阶狂蛮撞击_冲击波:
                return 神之子.进阶狂蛮撞击;
            case 神之子.影子突击_剑气:
                return 神之子.影子突击;
            case 神之子.进阶旋卷切割_剑气:
                return 神之子.进阶旋卷切割;
            case 神之子.进阶圆月旋风_吸收:
                return 神之子.进阶圆月旋风;
            case 神之子.进阶狂转回旋_剑气:
                return 神之子.进阶狂转回旋;
            case 神之子.进阶旋跃斩_剑气:
                return 神之子.进阶旋跃斩;
            case 神之子.跳跃坠袭_冲击波:
                return 神之子.跳跃坠袭;
            case 神之子.地裂山崩_冲击波:
                return 神之子.地裂山崩;
            case 神之子.进阶地裂山崩_冲击波:
            case 神之子.进阶地裂山崩_雷电区域:
                return 神之子.进阶地裂山崩;
            case 神之子.极速切割_漩涡:
                return 神之子.极速切割;
            case 神之子.暴风制动_旋风:
                return 神之子.暴风制动;
            case 神之子.进阶暴风旋涡_旋涡:
            case 神之子.进阶暴风旋涡_雷电区域:
                return 神之子.进阶暴风旋涡;
            case 林之灵.前爪挥击2:
            case 林之灵.前爪挥击3:
            case 林之灵.巨熊35击:
                return 林之灵.前爪挥击;
            case 林之灵.雪豹_未知:
                return 林之灵.雪豹重斩;
            case 林之灵.伙伴发射2:
            case 林之灵.伙伴发射3:
            case 林之灵.伙伴发射4:
                return 林之灵.伙伴发射;
            case 隐月.冲击拳_2:
                return 隐月.冲击拳;
            case 隐月.狐灵_1:
                return 隐月.狐灵;
            case 隐月.幻灵招魂_1:
                return 隐月.幻灵招魂;
            case 隐月.火狐灵_1:
                return 隐月.火狐灵;
            case 隐月.爆流拳_2:
            case 隐月.爆流拳_3:
            case 隐月.爆流拳_4:
                return 隐月.爆流拳;
            case 隐月.通背拳_2:
            case 隐月.通背拳_3:
            case 隐月.通背拳_冲击波:
                return 隐月.通背拳;
            case 隐月.闪拳_2:
                return 隐月.闪拳;
            case 隐月.破力拳_冲击波:
            case 隐月.破力拳_2:
                return 隐月.破力拳;
            case 机械师.集中射击_IRON:
                return 机械师.集中射击_SPLASH;
            case 机械师.集中射击_IRON_B:
                return 机械师.集中射击_SPLASH_F;
            case 豹弩游侠.二连射_美洲豹:
                return 豹弩游侠.二连射;
            case 豹弩游侠.连环三箭_美洲豹:
                return 豹弩游侠.连环三箭;
            case 豹弩游侠.狂野射击_美洲豹:
                return 豹弩游侠.狂野射击;
            case 豹弩游侠.奥义箭乱舞_美洲豹:
                return 豹弩游侠.奥义箭乱舞;
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
                return 豹弩游侠.召唤美洲豹_灰;
            case 豹弩游侠.十字攻击_骑士:
                return 豹弩游侠.十字攻击;
            case 神之子.提速时刻_侦查:
            case 神之子.提速时刻_战斗:
                return 神之子.提速时刻;
            case 剑豪.三连斩_疾_2:
            case 剑豪.三连斩_疾_3:
            case 剑豪.拔刀击_疾:
                return 剑豪.三连斩_疾;
            case 剑豪.三连斩_风_2:
            case 剑豪.三连斩_风_3:
            case 剑豪.拔刀击_风:
                return 剑豪.三连斩_风;
            case 剑豪.三连斩_迅_2:
            case 剑豪.三连斩_迅_3:
            case 剑豪.拔刀击_迅:
                return 剑豪.三连斩_迅;
            case 剑豪.三连斩_雷_2:
            case 剑豪.三连斩_雷_3:
            case 剑豪.拔刀击_雷:
                return 剑豪.三连斩_雷;
            case 剑豪.连刃斩_2:
            case 剑豪.连刃斩_3:
            case 剑豪.连刃斩_4:
                return 剑豪.连刃斩;
            case 阴阳师.花炎结界:
            case 阴阳师.小白的恢复:
            case 阴阳师.幽玄气息:
                return 阴阳师.影朋_小白;
            case 阴阳师.花炎结界_4转:
            case 阴阳师.小白的恢复_4转:
            case 阴阳师.幽玄气息_4转:
                return 阴阳师.幻醒_小白;
            case 超能力者.心魂粉碎:
            case 超能力者.心魂粉碎_最后一击:
                return 超能力者.心魂之手;
            case 超能力者.心魂粉碎2:
            case 超能力者.心魂粉碎2_最后一击:
            case 超能力者.终极_心魂弹:
                return 超能力者.心魂之手2;
            case 超能力者.心魂释放_攻击:
                return 超能力者.心魂释放;
            case 超能力者.心魂之力2_引力:
                return 超能力者.心魂之力2;
            case 超能力者.心魂之力3_引力:
                return 超能力者.心魂之力3;
            case 龙神.巨龙迅捷_2:
            case 龙神.巨龙迅捷_3:
            case 龙神.风之迅捷:
            case 龙神.风之迅捷_攻击:
            case 龙神.迅捷_回来:
                return 龙神.巨龙迅捷;
            case 龙神.巨龙俯冲_攻击:
            case 龙神.雷电俯冲:
            case 龙神.雷电俯冲_攻击:
            case 龙神.雷之迅捷:
            case 龙神.雷之迅捷_攻击:
                return 龙神.巨龙俯冲;
            case 龙神.大地吐息_攻击:
            case 龙神.大地俯冲:
            case 龙神.风之吐息:
                return 龙神.巨龙吐息;
            case 龙神.龙神_2:
                return 龙神.龙神;
            case 火毒.审判之焰_人偶:
                return 火毒.审判之焰;
            case 400011002: {
                return 英雄.燃灵之剑;
            }
            case 5721055: {
                return 5721052;
            }
            case 400021020:
            case 400021021:
            case 400021022:
            case 400021023:
            case 400021024:
            case 400021025:
            case 400021026:
            case 400021027:
            case 400021028:
            case 400021029:
            case 400021030:
            case 400021031:
            case 400021032:
            case 400021033:
            case 400021034:
            case 400021035:
            case 400021036:
            case 400021037:
            case 400021038:
            case 400021039: {
                return 400021019;
            }
            case 400020002: {
                return 400021002;
            }
            case 恶魔猎手.恶魔觉醒_1:
            case 恶魔猎手.恶魔觉醒_2:
            case 恶魔猎手.恶魔觉醒_3:
            case 恶魔猎手.恶魔觉醒_4: {
                return 恶魔猎手.恶魔觉醒;
            }
            case 隐士.多向飞镖_双飞斩:
            case 隐士.多向飞镖_三连环光击破:
            case 隐士.多向飞镖_四连镖: {
                return 隐士.多向飞镖;
            }
            case 冲锋队长.超人变形_1:
            case 冲锋队长.超人变形_2:
            case 冲锋队长.超人变形_3: {
                return 冲锋队长.超人变形;
            }
            case 战神.装备摩诃_1: {
                return 战神.装备摩诃;
            }
            case 奇袭者.心雷合一_1: {
                return 奇袭者.心雷合一;
            }
            case 恶魔复仇者.恶魔狂怒_2: {
                return 恶魔复仇者.恶魔狂怒;
            }
            case 400010000: {
                return 400011000;
            }
            case 魂骑士.天人之舞_1:
            case 魂骑士.天人之舞_2: {
                return 魂骑士.天人之舞;
            }
            case 唤灵斗师.结合灵气_攻击: {
                return 唤灵斗师.结合灵气;
            }
            case 超能力者.心魂龙卷风_1:
            case 超能力者.心魂龙卷风_2:
            case 超能力者.心魂龙卷风_3:
            case 超能力者.心魂龙卷风_4:
            case 超能力者.心魂龙卷风_5:
            case 超能力者.心魂龙卷风_6: {
                return 超能力者.心魂龙卷风;
            }
            case 神射手.箭雨_1: {
                return 神射手.箭雨;
            }
            case 箭神.真一击要害箭: {
                return 箭神.真狙击;
            }
            case 风灵使者.呼啸暴风_1: {
                return 风灵使者.呼啸暴风;
            }
            case 双刀.利剑风暴: {
                return 双刀.利刃风暴;
            }
            case 侠盗.暗影抨击_1:
            case 侠盗.暗影抨击_2:
            case 侠盗.暗影抨击_3: {
                return 侠盗.暗影抨击;
            }
            case 双弩.元素幽灵_3: {
                return 双弩.精灵元素;
            }
            case 幻影.小丑_1:
            case 幻影.小丑_2:
            case 幻影.小丑_3:
            case 幻影.小丑_4:
            case 幻影.小丑_5:
            case 幻影.小丑_6: {
                return 幻影.王牌;
            }
            case 双弩.精神逃脱_1: {
                return 双弩.精神逃脱;
            }
            case 爆破手.神圣狂暴打击_1:
            case 爆破手.神圣狂暴打击_2:
            case 爆破手.神圣狂暴打击_3:
            case 爆破手.神圣狂暴打击_4:
            case 爆破手.神圣狂暴打击_5: {
                return 爆破手.神圣狂暴打击;
            }
            case 爆破手.碎骨巨叉_1:
            case 爆破手.碎骨巨叉_2:
            case 爆破手.碎骨巨叉_3:
            case 爆破手.碎骨巨叉_4: {
                return 爆破手.碎骨巨叉;
            }
            case 爆破手.急速闪避_1:
                return 爆破手.急速闪避;
            case 22170061:
                return 22170060;
            case 21120024:
                return 21120022;
            case 爆破手.摇摆不定_1: {
                return 爆破手.摇摆不定;
            }
            case 爆破手.重锤出击_1:
            case 爆破手.重锤出击_2: {
                return 爆破手.重锤出击;
            }
            case 爆破手.旋转弹_1:
            case 爆破手.旋转弹_2:
            case 爆破手.旋转弹_3:
            case 爆破手.旋转弹_4:
            case 爆破手.旋转弹_5:
            case 爆破手.旋转弹_6: {
                return 爆破手.旋转弹;
            }
            case 爆破手.飓风粉碎_1:
            case 爆破手.飓风粉碎_2: {
                return 爆破手.飓风粉碎;
            }
            case 爆破手.双重爆炸_转管炮: {
                return 爆破手.双重爆炸;
            }
            case 爆破手.狂暴打击_转管炮: {
                return 爆破手.狂暴打击;
            }
            case 爆破手.浮空冲压_1: {
                return 爆破手.浮空冲压;
            }
            case 爆破手.爆炸闪动_爆炸: {
                return 爆破手.爆炸闪动;
            }
            case 1100012: {
                return 1101012;
            }
            case 1111014: {
                return 1111008;
            }
            case 33120018: {
                return 33121017;
            }
            case 51111011:
            case 51111012: {
                return 51110009;
            }
            case 51001006:
            case 51001007:
            case 51001008:
            case 51001009:
            case 51001010:
            case 51001011:
            case 51001012:
            case 51001013: {
                return 51001005;
            }
            case 142100008: {
                return 142101003;
            }
            case 142000006: {
                return 142001004;
            }
            case 33000036: {
                return 33001010;
            }
            case 131001001:
            case 131001002:
            case 131001003:
            case 131001101:
            case 131001102:
            case 131001103:
            case 131002000: {
                return 131001000;
            }
            case 131002014: {
                return 131000014;
            }
            case 131002016: {
                return 131000016;
            }
            case 131002015: {
                return 131001015;
            }
            case 131001113:
            case 131001213:
            case 131001313: {
                return 131001013;
            }
            case 131002012: {
                return 131001012;
            }
            case 131001011:
            case 131002010: {
                return 131001010;
            }
            case 131001108:
            case 131001208: {
                return 131001008;
            }
            case 131001107:
            case 131001207:
            case 131001307: {
                return 131001007;
            }
            case 131001104:
            case 131002004: {
                return 131001004;
            }
            case 131001106:
            case 131001206:
            case 131001306:
            case 131001406:
            case 131001506: {
                return 131001006;
            }
            case 41111016: {
                return 41111018;
            }
            case 2100010: {
                return 2101010;
            }
            case 61121220: {
                return 61121015;
            }
            case 5121055: {
                return 5121052;
            }
            case 夜行者.影子蝙蝠_召唤兽: {
                return 14001027;
            }
            case 14121055:
            case 14121056: {
                return 14121054;
            }
            case 5701012: {
                return 5701011;
            }
            case 61111216: {
                return 61101100;
            }
            case 爆莉萌天使.灵魂吸取专家_1: {
                return 65121011;
            }
            case 42110010:
            case 42110011:
            case 42120024: {
                return 42101002;
            }
            case 42111011: {
                return 42111000;
            }
            case 41121021: {
                return 41121018;
            }
            case 41111017: {
                return 41111001;
            }
            case 41121020: {
                return 41121017;
            }
            case 41111018: {
                return 41111015;
            }
            case 41101015: {
                return 41101013;
            }
            case 41101014: {
                return 41101004;
            }
            case 41001015: {
                return 41001013;
            }
            case 41001014: {
                return 41001002;
            }
            case 41001012: {
                return 41001011;
            }
            case 40011290: {
                return 40011289;
            }
            case 42101022: {
                return 42101002;
            }
            case 42121022: {
                return 42120011;
            }
            case 42001007: {
                return 42001002;
            }
            case 42100010: {
                return 42101001;
            }
            case 42001005:
            case 42001006: {
                return 42001000;
            }
            case 12111029: {
                return 12111023;
            }
            case 12001027: {
                return 12000023;
            }
            case 32110020:
            case 32111003: {
                return 32111016;
            }
            case 33001202: {
                return 33001102;
            }
            case 33121255: {
                return 33121155;
            }
            case 33000037: {
                return 33001016;
            }
            case 33100016: {
                return 33101115;
            }
            case 33110016: {
                return 33111015;
            }
            case 2321055: {
                return 2321052;
            }
            case 61111217: {
                return 61101101;
            }
            case 61111219: {
                return 61111101;
            }
            case 61111215: {
                return 61001101;
            }
            case 61120018: {
                return 61121105;
            }
            case 初心者.元素跃动_超高跳: {
                return 初心者.元素跃动;
            }
            case 5710023:
            case 5710024:
            case 5710025:
            case 5710026: {
                return 5710020;
            }
            case 2120013: {
                return 2121007;
            }
            case 2220014: {
                return 2221007;
            }
            case 12001028: {
                return 12000023;
            }
            case 12121055: {
                return 12121054;
            }
            case 21110003: {
                return 21111013;
            }
            case 21110006: {
                return 21111014;
            }
            case 21120005: {
                return 21121013;
            }
            case 21121055:
            case 21121056: {
                return 21120052;
            }
            case 24120055: {
                return 24121052;
            }
            case 21000007: {
                return 21001010;
            }
            case 21110007:
            case 21110008:
            case 21110015: {
                return 21110002;
            }
            case 21120009:
            case 21120010:
            case 21120015: {
                return 21120002;
            }
            case 33101008: {
                return 33101004;
            }
            case 35101009:
            case 35101010: {
                return 35100008;
            }
            case 35111009:
            case 35111010: {
                return 35111001;
            }
            case 35121013: {
                return 35111004;
            }
            case 35121011: {
                return 35121009;
            }
            case 5710012: {
                return 5711002;
            }
            case 61111113: {
                return 61111100;
            }
//            case 112000000: {
//                return 110001501;
//            }
//            case 112100000: {
//                return 110001502;
//            }
//            case 112111003: {
//                return 110001503;
//            }
//            case 112120000: {
//                return 110001504;
//            }

        }
        return skillId;
    }

    public static boolean isForceIncrease(int skillId) {
        switch (skillId) {
            case 恶魔猎手.恶魔血月斩:
            case 恶魔猎手.恶魔血月斩1:
            case 恶魔猎手.恶魔血月斩2:
            case 恶魔猎手.恶魔血月斩3:
            case 恶魔猎手.死亡诅咒:

            case 30010166:
            case 30011167:
            case 30011168:
            case 30011169:
            case 30011170:
                return true;
        }
        return false;
    }

    public static boolean is超越攻击(int skillId) {
        switch (skillId) {
            case 恶魔复仇者.超越十字斩:
            case 恶魔复仇者.超越十字斩_1:
            case 恶魔复仇者.超越十字斩_2:
            case 恶魔复仇者.超越十字斩_3:
            case 恶魔复仇者.超越十字斩_4:
            case 恶魔复仇者.超越恶魔突袭:
            case 恶魔复仇者.超越恶魔突袭_1:
            case 恶魔复仇者.超越恶魔突袭_2:
            case 恶魔复仇者.超越恶魔突袭_3:
            case 恶魔复仇者.超越恶魔突袭_4:
            case 恶魔复仇者.超越月光斩:
            case 恶魔复仇者.超越月光斩_1:
            case 恶魔复仇者.超越月光斩_2:
            case 恶魔复仇者.超越月光斩_3:
            case 恶魔复仇者.超越月光斩_4:
            case 恶魔复仇者.超越处决:
            case 恶魔复仇者.超越处决_1:
            case 恶魔复仇者.超越处决_2:
            case 恶魔复仇者.超越处决_3:
            case 恶魔复仇者.超越处决_4:
                return true;
        }
        return false;
    }

    public static boolean isElementAmp_Skill(int skillId) {
        switch (skillId) {
            case 火毒.魔力激化:
            case 冰雷.魔力激化:
            case 龙神.魔力激化:
                return true;
        }
        return false;
    }

    public static int getMPEaterForJob(int job) {
        switch (job) {
            case 210:
            case 211:
            case 212:
                return 火毒.魔力吸收;
            case 220:
            case 221:
            case 222:
                return 冰雷.魔力吸收;
            case 230:
            case 231:
            case 232:
                return 主教.魔力吸收;
        }
        return 火毒.魔力吸收; //魔力吸收 Default, in case GM
    }

    public static int getJobShortValue(int job) {
        if (job >= 1000) {
            job -= (job / 1000) * 1000;
        }
        job /= 100;
        if (job == 4) { // For some reason dagger/ claw is 8.. IDK
            job *= 2;
        } else if (job == 3) {
            job += 1;
        } else if (job == 5) {
            job += 11; // 16
        }
        return job;
    }

    public static boolean isPyramidSkill(int skill) {
        return JobConstants.is新手职业(skill / 10000) && skill % 10000 == 1020;
    }

    public static boolean isInflationSkill(int skill) {
        return JobConstants.is新手职业(skill / 10000) && (skill % 10000 >= 1092 && skill % 10000 <= 1095);
    }

    public static boolean isMulungSkill(int skill) {
        return JobConstants.is新手职业(skill / 10000) && (skill % 10000 == 1009 || skill % 10000 == 1010 || skill % 10000 == 1011);
    }

    public static boolean isIceKnightSkill(int skill) {
        return JobConstants.is新手职业(skill / 10000) && (skill % 10000 == 1098 || skill % 10000 == 99 || skill % 10000 == 100 || skill % 10000 == 103 || skill % 10000 == 104 || skill % 10000 == 1105);
    }

    public static boolean is骑兽技能(int skill) {
        return JobConstants.is新手职业(skill / 10000) && skill % 10000 == 1004;
    }

    public static boolean is品克缤技能(int skill) {
        return skill / 10000 == 13000 || skill / 10000 == 13100;
    }

    public static int getAttackDelay(int skillId, Skill skill) {
        switch (skillId) {
            case 火毒.致命毒雾:
            case 林之灵.火焰屁:
            case 超能力者.心灵传动:
                return 0;
            case 神射手.暴风箭雨_四转:
            case 神射手.箭矢炮盘:
            case 双弩.伊师塔之环:
            case 豹弩游侠.奥义箭乱舞:
            case 恶魔复仇者.暗影蝙蝠:
            case 黑骑士.拉曼查之枪:
            case 船长.战船轰炸机:
            case 船长.战舰炮轰_1:
            case 船长.战舰炮轰_2:
            case 船长.战舰炮轰_3:
            case 炎术士.轨道烈焰:
            case 炎术士.轨道烈焰II:
            case 炎术士.轨道烈焰III:
            case 炎术士.轨道烈焰IV:
            case 超能力者.终极_BPM:
            case 超能力者.心魂吸收:
                return 40;
            case 隐士.三连环光击破:
                return 99;
            case 风灵使者.风霜雪舞:
            case 风灵使者.天空之歌:
                return 120;
            case 幻影.卡片雪舞:
            case 幻影.黑色秘卡:
            case 幻影.蓝光连击:
            case 幻影.卡片风暴:
            case 夜光.晨星坠落:
            case 尖兵.精准火箭:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
            case 狂龙战士.剑刃之壁_变身:
            case 狂龙战士.进阶剑刃之壁_变身:
            case 隐士.刺客标记_飞镖:
            case 隐士.隐士标记_飞镖:
                return 30;
            case 冰雷.寒霜爆晶:
                return 180;
            case 夜光.晨星坠落_爆炸:
            case 狂龙战士.扇击:
            case 狂龙战士.扇击_1:
                return 210;
            case 恶魔猎手.恶魔血月斩1:
            case 恶魔猎手.恶魔血月斩2:
            case 恶魔猎手.恶魔血月斩3:
                return 270;
            case 恶魔猎手.黑暗变形:
                return 510;
            case 狂龙战士.飞龙斩:
            case 狂龙战士.飞龙斩_1:
            case 狂龙战士.飞龙斩_2:
                return 240;
            case 爆莉萌天使.释世书:
            case 爆莉萌天使.灵魂共鸣:
                return 180;
            case 尖兵.刀锋之舞:
                return 120;
            case 0: // Normal Attack, TODO delay for each weapon type
                return 330;
        }
        if (skill != null && skill.getSkillType() == 3) {
            return 0; //final attack
        }
        if (skill != null && skill.getDelay() > 0 && !isNoDelaySkill(skillId)) {
            return skill.getDelay();
        }
        return 330; // Default usually
    }

    /*
         * 管理员技能
         */
    public static boolean isAdminSkill(int skillId) {
        int jobId = skillId / 10000;
        return jobId == 800 || jobId == 900;
    }

    /*
         * 特殊技能
         */
    public static boolean isSpecialSkill(int skillId) {
        int jobId = skillId / 10000;
        return jobId == 7000 || jobId == 7100 || jobId == 8000 || jobId == 9000 || jobId == 9100 || jobId == 9200 || jobId == 9201 || jobId == 9202 || jobId == 9203 || jobId == 9204;
    }

    public static boolean isApplicableSkill(int skillId) {
        return ((skillId < 80000000 || skillId >= 100000000) && (skillId % 10000 < 8000 || skillId % 10000 > 8006) && !is天使祝福戒指(skillId)) || skillId >= 92000000 || (skillId >= 80000000 && skillId < 80020000); //no additional/decent skills
    }

    public static boolean isApplicableSkill_(int skillId) { //not applicable to saving but is more of temporary
        for (int i : PlayerStats.pvpSkills) {
            if (skillId == i) {
                return true;
            }
        }
        return (skillId >= 90000000 && skillId < 92000000) || (skillId % 10000 >= 8000 && skillId % 10000 <= 8003) || is天使祝福戒指(skillId);
    }

    public static int getMasterySkill(int job) {
        if (job >= 1410 && job <= 1412) {
            return 夜行者.投掷精通;
        } else if (job >= 410 && job <= 412) {
            return 隐士.精准暗器;
        }
        return 0;
    }

    public static boolean is不检测范围(int skillId) {
        switch (skillId) {
            case 神射手.爆炸箭:
            case 冲锋队长.龙卷风拳:
            case 神炮王.猴子炸药桶:
            case 神炮王.猴子炸药桶_爆炸:
            case 冰雷.冰河锁链:
            case 冰雷.链环闪电:
            case 冰雷.寒霜爆晶:
            case 恶魔猎手.恶魔追踪:
            case 夜光.晨星坠落:
            case 夜光.晨星坠落_爆炸:
            case 夜光.闪电反击:
            case 狂龙战士.扇击:
            case 狂龙战士.扇击_1:
            case 隐士.刺客标记_飞镖:
            case 隐士.隐士标记_飞镖:
                return true;
        }
        return false;
    }

    public static boolean isNoCheckAttackSkill(int skillId) {
        switch (skillId) {
            case 侠盗.金钱炸弹:
            case 侠盗.金钱炸弹_攻击:
            case 箭神.一击要害箭:
            case 双弩.闪电刀刃:
            case 幻影.卡片雪舞:
            case 幻影.黑色秘卡:
            case 隐士.隐士标记_飞镖:
            case 隐士.刺客标记_飞镖:
            case 爆莉萌天使.灵魂吸取_攻击:
            case 机械师.金属机甲_人类:
            case 机械师.金属机甲_战车:
            case 1201011:
            case 1201012:
            case 1211008:
            case 1221004:
            case 1221009:
            case 1321052:
            case 3120017:
            case 27111100:
            case 36110004:
            case 超能力者.心灵传动:
            case 400010000:
            case 幻影.小丑_1: {
                return true;
            }
        }
        return false;
    }

    public static boolean isNoDelaySkill(int skillId) {
        switch (skillId) {
            case 冲锋队长.能量获得:
            case 战神.抗压:
            case 火毒.快速移动精通:
            case 冰雷.快速移动精通:
            case 主教.快速移动精通:
            case 机械师.金属机甲_人类:
            case 机械师.金属机甲_战车:
            case 机械师.战争机器_泰坦:
            case 龙神.巨龙迅捷:
            case 龙神.飞龙闪:
            case 龙神.巨龙迅捷_2:
            case 龙神.巨龙迅捷_3:
            case 龙神.风之迅捷_攻击:
            case 龙神.玛瑙的意志:
            case 双刀.阿修罗:
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
            case 唤灵斗师.死亡:
            case 唤灵斗师.死亡契约:
            case 唤灵斗师.死亡契约2:
            case 唤灵斗师.死亡契约3:
            case 唤灵斗师.黑暗闪电:
            case 隐月.灵狐:
            case 超能力者.心魂释放:
            case 林之灵.旋风飞行:
            case 冰雷.寒霜爆晶:
            case 侠盗.潜影杀机:
            case 恶魔猎手.黑暗变形:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
            case 超能力者.心魂吸收:
            case 超能力者.终极_BPM:
            case 豹弩游侠.辅助打猎单元:
            case 豹弩游侠.集束箭:
                return true;
        }
        return false;
    }

    public static boolean isNoApplyTo(int skillId) {
        switch (skillId) {
            case 恶魔猎手.黑暗变形:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
            case 狂龙战士.剑刃之壁_变身:
            case 狂龙战士.进阶剑刃之壁_变身:
            case 尖兵.宙斯盾系统:
            case 冰雷.寒冰步:
            case 双刀.阿修罗:
            case 神射手.箭矢炮盘_攻击:
            case 夜光.晨星坠落:
            case 爆莉萌天使.灵魂吸取_攻击:
            case 恶魔复仇者.追击盾_攻击:
            case 风灵使者.狂风肆虐Ⅰ:
            case 风灵使者.狂风肆虐Ⅱ:
            case 风灵使者.狂风肆虐Ⅲ:
            case 风灵使者.暴风灭世:
            case 风灵使者.呼啸暴风:
            case 风灵使者.呼啸暴风_1:
            case 双刀.终极斩:
            case 冲锋队长.能量旋风:
            case 船长.战船轰炸机:
            case 船长.战舰炮轰_1:
            case 船长.战舰炮轰_2:
            case 船长.战舰炮轰_3:
            case 船长.子弹盛宴:
            case 林之灵.火焰屁:
            case 火毒.制裁火球:
            case 隐士.多向飞镖_双飞斩:
            case 隐士.多向飞镖_三连环光击破:
            case 隐士.多向飞镖_四连镖:
            case 尖兵.超能光束炮:
            case 神炮王.宇宙无敌火炮弹:
                return true;
        }
        return false;
    }

    public static boolean is召唤兽戒指(int skillId) {
        switch (skillId) {
            case 80000052:
            case 80000053:
            case 80000054:
            case 80000086:
            case 80001154:
            case 80001262:
            case 80001518:
            case 80001519:
            case 80001520:
            case 80001521:
            case 80001522:
            case 80001523:
            case 80001524:
            case 80001525:
            case 80001526:
            case 80001527:
            case 80001528:
            case 80001529:
            case 80001530:
            case 80001531:
            case 80010067:
            case 80010068:
            case 80010069:
            case 80010070:
            case 80010071:
            case 80010072:
            case 80010075:
            case 80010076:
            case 80010077:
            case 80010078:
            case 80010079:
            case 80010080:
            case 80011103:
            case 80011104:
            case 80011105:
            case 80011106:
            case 80011107:
            case 80011108: {
                return true;
            }
        }
        return is天使祝福戒指(skillId);
    }

    public static boolean is天使祝福戒指(int skillId) {
        return JobConstants.is新手职业(skillId / 10000) && (skillId % 10000 == 1085 || skillId % 10000 == 1087 || skillId % 10000 == 1090 || skillId % 10000 == 1179);
    }

    public static boolean is气象戒指(int skillId) {
        return JobConstants.is新手职业(skillId / 10000) && (skillId / 10000 == 8001) && (skillId % 10000 >= 67 && skillId % 10000 <= 80);
    }

    /*
         * 角色卡系统
         * 1 = B
         * 2 = A
         * 3 = S
         * 4 = SS
         */
    public static int getCardSkillLevel(int level) {
        if (level >= 60 && level < 100) {
            return 2;
        } else if (level >= 100 && level < 200) {
            return 3;
        } else if (level >= 200) {
            return 4;
        }
        return 1;
    }

    public static boolean is致命毒液(int skillId) {
        switch (skillId) {
            case 侠盗.致命毒液:
            case 隐士.致命毒液:
                return true;
        }
        return false;
    }

    /*
         * 技能的模式
         */
    public static int getLuminousSkillMode(int skillId) {
        switch (skillId) {
            case 夜光.耀眼光球:
            case 夜光.仙女发射:
            case 夜光.闪爆光柱:
            case 夜光.超级光谱:
            case 夜光.闪耀救赎:
            case 夜光.闪电反击:
                return 夜光.太阳火焰; //光明技能 20040216 - 太阳火焰 - 使用充满光明的光之魔法后，造成额外伤害。每次施展魔法时，恢复一定比例的体力，MP使用量减少50%。
            case 夜光.黑暗降临:
            case 夜光.虚空重压:
            case 夜光.暗锁冲击:
            case 夜光.晨星坠落:
            case 夜光.启示录:
            case 夜光.晨星坠落_爆炸:
                return 夜光.月蚀; //黑暗技能 20040217 - 月蚀 - 使用充满黑暗的暗之魔法后，造成额外伤害。每次施展魔法时，恢复一定比例的体力，MP使用量减少50%。
            case 夜光.死亡之刃:
            case 夜光.绝对死亡:
                return 夜光.平衡_光明; //平衡技能 20040219 - 平衡 - 使用光明和黑暗完美平衡的稳如泰山，并使所有伤害减至1。使用光明、黑暗，混合魔法时产生额外伤害。无冷却时间，施展光明攻击魔法时，恢复一定比例的体力；施展黑暗攻击魔法时，不消耗MP。
        }
        return -1;
    }

    /**
     * 技能无视时间限制
     *
     * @param skillid
     * @return
     */
    public static boolean isSkillTiem(int skillid) {
        switch (skillid) {
            case 林之灵.旋风飞行:
            case 冰雷.寒霜爆晶:
            case 侠盗.潜影杀机:
            case 恶魔猎手.黑暗变形:
            case 狂龙战士.剑刃之壁:
            case 狂龙战士.进阶剑刃之壁:
                return true;
        }
        return false;
    }

    public static boolean isKSTelekinesisSkill(int skillid) {
        switch (skillid) {
            case 超能力者.心灵传动:
            case 超能力者.心魂之力:
            case 超能力者.心魂之力2:
            case 超能力者.心魂之力2_引力:
            case 超能力者.心魂之力3:
            case 超能力者.心魂之力3_引力:
            case 超能力者.心魂吸收:
            case 超能力者.心魂吸收_攻击:
            case 超能力者.终极_物质:
            case 超能力者.终极_深层冲击:
            case 超能力者.终极_火车:
            case 超能力者.终极_BPM:
            case 超能力者.终极_心魂弹:
                return false;
            default:
                return true;
        }
    }

    public static boolean isSkip4Skill(int skillid) {
        switch (skillid) {
            case 夜行者.双飞斩:
            case 夜行者.四连镖:
            case 夜行者.四连镖_最后一击:
            case 夜行者.暗影大风车:
            case 夜行者.暗影大风车_爆炸:
            case 夜行者.三连环光击破:
            case 夜行者.三连环光击破_最后一击:
            case 夜行者.五倍投掷_爆击率:
            case 夜行者.五倍投掷:
            case 夜行者.五倍投掷_最后一击:
            case 双弩.终结箭:
            case 双弩.爆裂飞腿:
            case 双弩.进阶急袭双杀:
                return true;
            default:
                if (skillid / 1000 == 23001 || skillid / 1000 == 23101 || skillid / 1000 == 23111 || skillid / 1000 == 23121 || skillid / 1000 == 131001 && skillid != 131001207 && skillid != 131001107) {
                    return skillid != 双弩.艾琳之怒;
                }
        }
        return getSoulMasterAttackMode(skillid) > 0;
    }

    public static boolean isSkip4CloseAttack(int skillid) {
        switch (skillid) {
            case 神之子.极速切割_漩涡:
            case 神之子.暴风制动_旋风:
            case 神之子.进阶暴风旋涡_旋涡:
            case 狂龙战士.扇击:
            case 狂龙战士.扇击_1:
            case 狂龙战士.扇击_变身_2:
            case 恶魔复仇者.暗影蝙蝠:
            case 夜行者.暗影大风车:
            case 龙神.雷电俯冲:
            case 龙神.雷电俯冲_攻击:
            case 炎术士.灭绝之焰:
            case 神炮王.猴子炸药桶:
            case 冲锋队长.能量旋风:
            case 冲锋队长.龙卷风拳:
                return true;
        }
        return false;
    }

    public static int getSoulMasterAttackMode(int skillid) {
        switch (skillid) {
            case 魂骑士.瞬步:
            case 魂骑士.摄魂斩:
            case 魂骑士.月影斩:
            case 魂骑士.月光十字斩:
            case 魂骑士.月光之舞:
            case 魂骑士.月光之舞_空中:
            case 魂骑士.新月斩: {
                return 1;
            }
            case 魂骑士.猛袭:
            case 魂骑士.灼影之焰:
            case 魂骑士.光速突击:
            case 魂骑士.日光十字斩:
            case 魂骑士.极速霞光:
            case 魂骑士.极速霞光_空中:
            case 魂骑士.烈日之刺: {
                return 2;
            }
        }
        return -1;
    }

    public static boolean ge(int skillid) {
        switch (skillid) {
            case 火毒.燎原之火_MIST:
            case 火毒.快速移动精通:
            case 冰雷.寒冰步:
            case 冰雷.快速移动精通:
            case 主教.快速移动精通:
            case 双刀.阿修罗:
            case 冲锋队长.能量获得:
            case 炎术士.快速移动精通:
            case 战神.抗压:
            case 幻影.玫瑰卡片终结_1:
            case 唤灵斗师.黑暗闪电:
            case 机械师.战争机器_泰坦:
            case 狂龙战士.恶魔之息_暴怒:
            case 神之子.暴风制动_旋风: {
                return true;
            }
        }
        return false;
    }

    public static boolean gw(int skillId) {
        switch (skillId) {
            case 冰雷.寒霜爆晶:
            case 冲锋队长.龙卷风拳:
            case 冲锋队长.能量旋风:
            case 神炮王.猴子炸药桶:
            case 炎术士.灭绝之焰:
            case 夜行者.暗影大风车:
            case 龙神.雷电俯冲:
            case 龙神.雷电俯冲_攻击:
            case 夜光.晨星坠落:
            case 恶魔复仇者.暗影蝙蝠:
            case 狂龙战士.扇击:
            case 狂龙战士.扇击_1:
            case 狂龙战士.扇击_变身_2:
            case 神之子.极速切割_漩涡:
            case 神之子.暴风制动_旋风:
            case 神之子.进阶暴风旋涡_旋涡:
                return true;
        }
        return false;
    }


    public static boolean isShowForgenBuff(MapleBuffStat buff) {
        switch (buff) {
            case 变形值:
            case 激素狂飙:
            case 月光转换:
            case 日月轮转:
            case 晓月流基本技能:
            case 重生契约:
            case 精神连接:
            case 灵魂凝视:
            case 伤害置换:
            case 返回原位置:
            case 影分身:
            case 三彩箭矢:
            case SECONDARY_STAT_ImmuneBarrier:
            case SECONDARY_STAT_NaviFlying:
            case 地雷:
            case SECONDARY_STAT_MagnetArea:
            case SECONDARY_STAT_RideVehicleExpire:
            case 骑兽技能:
            case 超能光束炮:
            case 灵气大融合:
            case SECONDARY_STAT_PinkbeanRollingGrade:
            case SECONDARY_STAT_Magnet:
            case 昏迷:
            case 虚弱:
            case 缓慢:
            case 变身效果:
            case GHOST_MORPH:
            case 隐身术:
            case 金属机甲:
            case 疾驰速度:
            case 疾驰跳跃:
            case 飞行骑乘:
            case 飞翔:
            case FREEZE:
            case SECONDARY_STAT_Shock:
            case 能量获得:
            case SECONDARY_STAT_KeyDownMoving:
            case 火焰灵气:
            case 精灵的帽子:
            case 花炎结界:
            case 击杀点数:
            case 极速领域:
            case 斗气集中:
            case 属性攻击:
            case 元素冲击:
            case 黑暗:
            case 封印:
            case SECONDARY_STAT_WeaknessMdamage:
            case 诅咒:
            case SECONDARY_STAT_PvPRaceEffect:
            case SECONDARY_STAT_IceKnight:
            case SECONDARY_STAT_DisOrder:
            case SECONDARY_STAT_Explosion:
            case SECONDARY_STAT_Thread:
            case SECONDARY_STAT_Team:
            case 中毒:
            case 增加伤害最大值:
            case 无形箭弩:
            case 诱惑:
            case 暗器伤人:
            case 不死化:
            case ARIANT_COSS_IMU:
            case 闪光击:
            case 混乱:
            case SECONDARY_STAT_RespectPImmune:
            case SECONDARY_STAT_RespectMImmune:
            case ILLUSION:
            case 狂暴战魂:
            case 金刚霸体:
            case 天使状态:
            case 无法使用药水:
            case SHADOW:
            case 致盲:
            case 魔法屏障:
            case SECONDARY_STAT_Frozen2:
            case 龙卷风:
            case 撤步退身:
            case 终极斩:
            case 呼啸_爆击概率:
            case 时间胶囊:
            case 巨人药水:
            case 风影漫步:
            case 黑暗变形:
            case 神圣拯救者的祝福:
            case 死亡束缚:
            case SECONDARY_STAT_PyramidEffect:
            case 百分比无视防御:
            case 幻影屏障:
            case 卡牌审判:
            case SECONDARY_STAT_KeyDownAreaMoving:
            case 黑暗高潮:
            case 黑暗祝福:
            case 光暗转换:
            case 模式转换:
            case 剑刃之壁:
            case 天使亲和:
            case 灵魂鼓舞:
            case SECONDARY_STAT_MobZoneState:
            case 连击无限制:
            case 至圣领域:
            case SECONDARY_STAT_IgnorePImmune:
            case 最终审判:
            case 抗震防御:
            case 寒冰灵气:
            case 天堂之门:
            case 伤害吸收:
            case 神圣保护:
            case 无敌状态:
            case 流血剧毒:
            case 隐形剑:
            case 不倦神酒:
            case 阿修罗:
            case 恶魔超越:
            case 尖兵电力:
            case 永动引擎:
            case 元素属性:
            case 开天辟地:
            case 信天翁:
            case 神之子透明:
            case 灵魂武器:
            case 灵魂技能:
            case 灵魂助力:
            case 防甲穿透:
            case 圣洁之力:
            case 神圣迅捷:
            case SECONDARY_STAT_FullSoulMP:
            case 招魂结界:
            case 召唤美洲豹:
            case 战法灵气:
            case 黑暗闪电:
            case 战斗大师:
            case 守护模式变更:
            case 舞力全开:
            case 拔刀姿势:
            case 拔刀术加成:
            case 增加HP百分比:
            case 增加MP百分比:
            case 增加攻击力:
            case 一闪:
            case 稳如泰山:
            case 结界破魔:
            case 厚积薄发:
            case 结合灵气:
            case SECONDARY_STAT_BattlePvP_LangE_Protection:
            case 导航辅助:
            case SECONDARY_STAT_Undead:
            case SECONDARY_STAT_COUNT_PLUS1:
            case 潜入状态:
            case 隐藏碎片:
            case 祝福护甲:
            case 神圣魔法盾:
            case 天使复仇:
            case 心魂本能:
            case 灵魂链接:
            case 光之守护:
            case 忍耐之盾:
            case 水枪阵营:
            case 水枪军阶:
            case 水枪效果:
            case 装备摩诃:
            case 超人变形:
            case 爱星能量:
            case 心雷合一:
            case 子弹盛宴:
            case SECONDARY_STAT_UNK466:
            case 变换攻击:
            case 祈祷:
            case SECONDARY_STAT_UNK476:
            case 向导之箭:
            case 精灵元素:
            case SECONDARY_STAT_UNK497: {
                return true;
            }
        }
        return false;
    }

    public static boolean isWriteBuffByteData(MapleBuffStat buffStat) {
        switch (buffStat) {
            case 变形值:
            case SECONDARY_STAT_NaviFlying:
            case 骑兽技能:
            case 移动速度:
            case 跳跃力:
            case 昏迷:
            case 虚弱:
            case 缓慢:
            case 变身效果:
            case GHOST_MORPH:
            case 冒险岛勇士:
            case 隐身术:
            case 金属机甲:
            case 疾驰速度:
            case 疾驰跳跃:
            case 飞翔:
            case 飞行骑乘:
            case FREEZE:
            case SECONDARY_STAT_Shock:
            case 增加跳跃力:
            case 增加移动速度:
            case 能量获得:
            case SECONDARY_STAT_KeyDownMoving:
            case 火焰灵气:
            case 精灵的帽子:
                return true;
        }
        return false;
    }

    public static boolean isWriteBuffIntValue(MapleBuffStat buffStat) {
        switch (buffStat) {
            case SECONDARY_STAT_CarnivalDefence:
            case 精神连接:
            case SECONDARY_STAT_DojangLuckyBonus:
            case 灵魂凝视:
            case 伤害置换:
            case 返回原位置:
            case 影分身:
            case SECONDARY_STAT_BossShield:
            case IDA_SPECIAL_BUFF5:
            case SECONDARY_STAT_SetBaseDamage:
            case 三彩箭矢:
            case SECONDARY_STAT_ImmuneBarrier:
            case SECONDARY_STAT_NaviFlying:
            case 地雷:
            case SECONDARY_STAT_SetBaseDamageByBuff:
            case SECONDARY_STAT_DotHealHPPerSecond:
            case SECONDARY_STAT_MagnetArea:
            case SECONDARY_STAT_RideVehicleExpire:
            case 骑兽技能:
            case 重击研究:
            case 超能光束炮:
            case 灵气大融合:
            case 神圣归一:
            case SECONDARY_STAT_BattlePvP_Helena_WindSpirit:
            case SECONDARY_STAT_BattlePvP_LeeMalNyun_ScaleUp:
            case SECONDARY_STAT_PinkbeanRollingGrade:
            case UNK_MBS_512:
            case SECONDARY_STAT_VampDeath:
            case SECONDARY_STAT_Magnet: {
                return true;
            }
        }
        return false;
    }

    public static boolean isWriteNeedPointValue(MapleBuffStat buffStat) {
        switch (buffStat) {
            case SECONDARY_STAT_RideVehicleExpire:
            case 骑兽技能:
            case 击杀点数:
            case 极速领域: {
                return true;
            }
        }
        return false;
    }

    public static boolean f(MapleBuffStat buffStat) {
        switch (buffStat) {
            case 花炎结界:
            case SECONDARY_STAT_ChangeFoxMan: {
                return true;
            }
        }
        return false;
    }

    public static boolean c(MapleBuffStat stat) {
        switch (stat) {
            case 激素狂飙:
            case 月光转换:
            case 日月轮转:
            case 霰弹炮:
            case 晓月流基本技能:
            case 皇家守护:
            case 重生契约: {
                return true;
            }
        }
        return false;
    }

    public static boolean is美洲豹(int skillId) {
        switch (skillId) {
            case 豹弩游侠.召唤美洲豹_灰:
            case 豹弩游侠.召唤美洲豹_黄:
            case 豹弩游侠.召唤美洲豹_红:
            case 豹弩游侠.召唤美洲豹_紫:
            case 豹弩游侠.召唤美洲豹_蓝:
            case 豹弩游侠.召唤美洲豹_剑:
            case 豹弩游侠.召唤美洲豹_雪:
            case 豹弩游侠.召唤美洲豹_玛瑙:
            case 豹弩游侠.召唤美洲豹_铠甲:
                return true;
        }
        return false;
    }

    public static boolean isRuneSkill(int skillid) {
        switch (getLinkedAttackSkill(skillid)) {
            case 80001427:
            case 80001428:
            case 80001430:
            case 80001432:
            case 80001752:
            case 80001753:
            case 80001754:
            case 80001755:
            case 80001757:
            case 80001762: {
                return true;
            }
        }
        return false;
    }

    public static boolean isGeneralSkill(int skillid) {
        switch (skillid) {
            case 80001242:
            case 80001429:
            case 80001431:
            case 80001761:
            case 80001762: {
                return true;
            }
        }
        return skillid % 10000 == 1095 || skillid % 10000 == 1094;
    }

    public static boolean isExtraSkill(int skillid) {
        int group = skillid % 10000;
        switch (group) {
            case 8000:
            case 8001:
            case 8002:
            case 8003:
            case 8004:
            case 8005:
            case 8006: {
                return true;
            }
        }
        return false;
    }

    public static byte getLinkSkillslevel(int maxLevel, Skill skill, int n3, int defchrlevel) {
        int chrlevel;
        if (n3 > 0 && skill.isLinkSkills()) {
            chrlevel = MapleCharacter.getLevelbyid(n3);
        } else if (skill.isTeachSkills()) {
            chrlevel = defchrlevel;
        } else {
            return 0;
        }
        switch (maxLevel) {
            case 1: {
                return 1;
            }
            case 2: {
                if (chrlevel >= 120) {
                    return 2;
                }
                return 1;
            }
            case 3: {
                if (chrlevel >= 210) {
                    return 3;
                }
                if (chrlevel >= 120 && chrlevel < 210) {
                    return 2;
                }
                return 1;
            }
            case 5: {
                if (chrlevel >= 200) {
                    return 5;
                }
                if (chrlevel >= 175 && chrlevel < 200) {
                    return 4;
                }
                if (chrlevel >= 150 && chrlevel < 175) {
                    return 3;
                }
                if (chrlevel >= 125 && chrlevel < 150) {
                    return 2;
                }
                return 1;
            }
        }
        return 2;
    }

    /*
         * 种族特性本能技能
         */
    public static boolean isTeachSkills(int id) {
        switch (id) {
            case 110: //0000110 - 海盗祝福 - [种族特性技能]强化火炮手特有的坚韧，永久提高各种属性。
            case 1214: //0001214 - 宝盒的庇佑 - 获得含有侠义精神的宝盒的庇佑。\n#c双击#技能可打开宝盒界面，查看目前属性及#c剩余时间#，倒计时结束后当前属性会重置。可以通过特定道具变更属性、走向、剩余时间。
            case 20021110: //20021110 - 精灵的祝福 - [种族特性技能]借助古代精灵的祝福，可以回到埃欧雷，永久性地提高经验值获得量。
            case 20030204: //20030204 - 致命本能 - 拥有通过卓越的洞察力，找到敌人致命弱点的本能。
            case 20040218: //20040218 - 穿透 - 用穿透一切阻碍的光之力量，无视敌人的部分防御力。
            case 30010112: //30010112 - 恶魔之怒 - 对象是BOSS怪时，唤醒内在的愤怒，造成更强的伤害，吸收更多的精气。
            case 30010241: //30010241 - 野性狂怒 - 由于愤怒，伤害增加。
            case 30020233: //30020233 - 混合逻辑 - 采用混合逻辑设计，所有能力值永久提高。
            case 40020002: //40020002 - 紫扇传授 - 利用五行之力，增加对敌人造成的伤害。
            case 50001214: //50001214 - 光之守护 - 受到光之守护，在一定时间内即使受到敌人攻击，也不会被击退。
            case 60000222: //60000222 - 钢铁之墙 - 具备钢铁意志的狂龙战士获得额外体力。
            case 60011219: //60011219 - 灵魂契约 - 通过和爱丝卡达的契约，攻击力瞬间到达最大。
            case 110000800: //110000800 - 精灵集中 - 攻击BOSS怪时,精灵之力会更强。
            case 100000271: //100000271 - 伦娜的祝福 - 受到伦娜女神的祝福，减少自己受到的伤害。攻击敌人时，可无视部分防御。
            case 10000255: //10000255 - 希纳斯护佑（战士） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 10000256: //10000256 - 希纳斯护佑（魔法师） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 10000257: //10000257 - 希纳斯护佑（弓箭手） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 10000258: //10000258 - 希纳斯护佑（飞侠） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 10000259: //10000259 - 希纳斯护佑（海盗） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 20000297:
            case 20010294:
            case 20050286:
            case 30000074:
            case 30000075:
            case 30000076:
            case 30000077:
            case 40010001:
            case 140000292:
                return true;
        }
        return false;
    }

    /*
     * 链接技能技能
     */
    public static boolean isLinkSkills(int id) {
        switch (id) {
            case 80000000: //80000000 - 海盗祝福 - [链接技能]学习火炮手特有的强韧，永久性地提高各种属性。
            case 80000001: //80000001 - 恶魔之怒 - [链接技能]对象是BOSS怪时，唤醒内心的愤怒，造成更强的伤害。
            case 80000002: //80000002 - 致命本能 - 拥有通过卓越的洞察力，找到敌人致命弱点的本能。
            case 80000003: //80000003 - 疾风传授 - 剑斗无所不能！！但不知道他有没有什么不足的地方呢？
            case 80000004: //80000004 - 紫扇传授 - 利用五行之力，增加对敌人造成的伤害。
            case 80000005: //80000005 - 穿透 - 用穿透一切阻碍的光之力量，无视敌人的部分防御力。
            case 80000006: //80000006 - 钢铁之墙 - 具有比狂龙战士更出色的体力。
            case 80000047: //80000047 - 混合逻辑 - 采用混合逻辑设计，所有能力值永久提高。
            case 80000050: //80000050 - 野性狂怒 - 由于愤怒，伤害增加。
            case 80000055: //80000055 - 希纳斯护佑 - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000066: //80000066 - 希纳斯护佑（战士） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000067: //80000067 - 希纳斯护佑（魔法师） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000068: //80000068 - 希纳斯护佑（弓箭手） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000069: //80000069 - 希纳斯护佑（飞侠） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000070: //80000070 - 希纳斯护佑（海盗） - 觉醒的女皇的护佑充盈体内，使自己避免受到敌人的威胁。
            case 80000110: //80000110 - 伦娜的祝福 - [链接技能]获得伦娜女神的祝福，受到的伤害减少。进行攻击时，无视一部分防御。\n#c每次获得被激活的女神之泪时，技能效果得到强化。
//            case 80000169: //80000169 - 九死一生 - [链接技能]受到会导致死亡的攻击时, 有一定概率不死。
            case 80001040: //80001040 - 精灵的祝福 - [链接技能]获得古代精灵的祝福，可以回到埃欧雷去，经验值获得量永久提高。
            case 80001140: //80001140 - 光之守护 - [排名技能]受到光之骑士米哈尔的庇护，在一定时间内，即使受到敌人攻击也不会被击退。
            case 80001151: //80001151 - 宝盒的庇佑 - 获得包含了侠义精神的宝盒的庇佑。从龙的传人那里获得的属性链接，会因为自身和传授者的等级产生差异。
            case 80001155: //80001155 - 灵魂契约 - 通过与爱丝卡达的契约，瞬间令攻击力极大化。
            case 80010006: //80010006 - 精灵集中 - 攻击BOSS怪时,精灵之力会更强。
            case 80000169:
            case 80000188:
            case 80000329:
            case 80000333:
            case 80000334:
            case 80000335:
            case 80000369:
            case 80000378:
                return true;
        }
        return false;
    }

    public static int[] gm(int skillid) {
        switch (skillid) {
            case 80000055: {
                return new int[]{80000066, 80000067, 80000068, 80000069, 80000070};
            }
            case 80000329: {
                return new int[]{80000333, 80000334, 80000335, 80000378};
            }
        }
        return null;
    }

    public static int gn(int skillid) {
        switch (skillid) {
            case 80000066:
            case 80000067:
            case 80000068:
            case 80000069:
            case 80000070: {
                return 80000055;
            }
            case 80000333:
            case 80000334:
            case 80000335:
            case 80000378: {
                return 80000329;
            }
            case 80000055:
            case 80000329: {
                return 1;
            }
        }
        return 0;
    }

    public static int go(int skillid) {
        switch (skillid) {
            case 80000055: {
                return 80000055;
            }
            case 80000329: {
                return 80000329;
            }
            case 80000000: {
                return 110;
            }
            case 80001151: {
                return 1214;
            }
            case 80000370: {
                return 20000297;
            }
            case 80000369: {
                return 20010294;
            }
            case 80001040: {
                return 20021110;
            }
            case 80000002: {
                return 20030204;
            }
            case 80000005: {
                return 20040218;
            }
            case 80000169: {
                return 20050286;
            }
            case 80000001: {
                return 30010112;
            }
            case 80000050: {
                return 30010241;
            }
            case 80000047: {
                return 30020233;
            }
            case 80000004: {
                return 40020002;
            }
            case 80000003: {
                return 40010001;
            }
            case 80001140: {
                return 50001214;
            }
            case 80000006: {
                return 60000222;
            }
            case 80001155: {
                return 60011219;
            }
            case 80010006: {
                return 110000800;
            }
            case 80000066: {
                return 10000255;
            }
            case 80000067: {
                return 10000256;
            }
            case 80000068: {
                return 10000257;
            }
            case 80000069: {
                return 10000258;
            }
            case 80000070: {
                return 10000259;
            }
            case 80000110: {
                return 100000271;
            }
            case 80000188: {
                return 140000292;
            }
            case 80000333: {
                return 30000074;
            }
            case 80000334: {
                return 30000075;
            }
            case 80000335: {
                return 30000076;
            }
            case 80000378: {
                return 30000077;
            }
        }
        return -1;
    }

    public static int getLinkedSkillId(int skillid) {
        switch (skillid) {
            case 110: {
                return 80000000;
            }
            case 1214: {
                return 80001151;
            }
            case 20000297: {
                return 80000370;
            }
            case 20010294: {
                return 80000369;
            }
            case 20021110: {
                return 80001040;
            }
            case 20030204: {
                return 80000002;
            }
            case 20040218: {
                return 80000005;
            }
            case 20050286: {
                return 80000169;
            }
            case 30010112: {
                return 80000001;
            }
            case 30010241: {
                return 80000050;
            }
            case 30020233: {
                return 80000047;
            }
            case 40020002: {
                return 80000004;
            }
            case 40010001: {
                return 80000003;
            }
            case 50001214: {
                return 80001140;
            }
            case 60000222: {
                return 80000006;
            }
            case 60011219: {
                return 80001155;
            }
            case 110000800: {
                return 80010006;
            }
            case 10000255: {
                return 80000066;
            }
            case 10000256: {
                return 80000067;
            }
            case 10000257: {
                return 80000068;
            }
            case 10000258: {
                return 80000069;
            }
            case 10000259: {
                return 80000070;
            }
            case 100000271: {
                return 80000110;
            }
            case 140000292: {
                return 80000188;
            }
            case 30000074: {
                return 80000333;
            }
            case 30000075: {
                return 80000334;
            }
            case 30000076: {
                return 80000335;
            }
            case 30000077: {
                return 80000378;
            }
        }
        return -1;
    }
}
