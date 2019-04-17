package handling.channel.handler;

import client.MapleBuffStat;
import client.skills.Skill;
import client.skills.SpecialBuffInfo;
import constants.GameConstants;
import constants.skills.*;
import handling.Buffstat;
import handling.opcode.SendPacketOpcode;
import org.junit.Before;
import org.junit.Test;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import server.MapleStatEffect;
import tools.DateUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.File;
import java.util.*;

public class PlayerHandlerTest {

    private static MapleData delayData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Character.wz")).getData("00002000.img");
    private static MapleDataProvider datasource = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Skill.wz"));

    public static String giveBuff(int buffid, int bufflength, Map<MapleBuffStat, Integer> statups, MapleStatEffect effect) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIVE_BUFF.getValue());
        writeBuffMask(mplew, statups);
        boolean special = false;
        boolean isMountBuff = false;
        boolean isZeroUnknown = buffid == 神之子.圣洁之力 || buffid == 神之子.神圣迅捷;
        boolean isWriteIntSkill = buffid == 双弩.精神注入 || buffid == 爆莉萌天使.力量转移 || buffid == 船长.指挥船员 || buffid == 龙神.龙神;
        boolean darklight = false;
        int count = 0; //一些特殊技能的处理 侠盗的击杀点数 ，奇袭者闪电的无视防御次数 ， 夜光黑暗高潮的次数
        List<MapleBuffStat> buffStat = new ArrayList<>(); //将BUFF在的BuffStat放到列表 用来判断BUFF中是否包含
        Map<MapleBuffStat, Integer> ordinaryStatups = new EnumMap<>(MapleBuffStat.class); //普通的BUFF属性
        Map<MapleBuffStat, Integer> speciaStatups = new EnumMap<>(MapleBuffStat.class); //特殊的BUFF属性
        for (Map.Entry<MapleBuffStat, Integer> stat : statups.entrySet()) {
            if (stat.getKey() == MapleBuffStat.骑兽技能) {
                isMountBuff = true;
            } else if (stat.getKey() == MapleBuffStat.月光转换 || stat.getKey() == MapleBuffStat.神圣保护) {
                isZeroUnknown = true;
            } else if (stat.getKey() == MapleBuffStat.战法灵气 || stat.getKey() == MapleBuffStat.寒冰灵气 || stat.getKey() == MapleBuffStat.月光转换 || stat.getKey() == MapleBuffStat.抗震防御) {
                count = 1;
                isZeroUnknown = true;
            } else if (stat.getKey() == MapleBuffStat.百分比无视防御 && buffid == 奇袭者.元素_闪电) {
                count = Math.min(stat.getValue() / 5, 5);
            } else if (stat.getKey() == MapleBuffStat.黑暗高潮) {
                if (buffid == 夜光.黑暗高潮) {
                    count = stat.getValue();
                } else if (buffid == 尖兵.双重防御) { //回避多少次后消失 默认为10次 但这个BUFF的值 初始为 5
                    count = effect.getX(); //设置为默认次数
                    if (effect.getProp() > stat.getValue()) {
                        int prop = effect.getProp() - stat.getValue();
                        count -= prop / effect.getY();
                        if (count < 0) {
                            count = 0;
                        }
                    }
                }
                isZeroUnknown = true;
            } else if (stat.getKey() == MapleBuffStat.元素冲击) {
                count = Math.min(stat.getValue() / 5, 5);
            } else if (stat.getKey() == MapleBuffStat.光暗转换_2) {
                darklight = true;
                break;
            }
            buffStat.add(stat.getKey());
            if (stat.getKey().canStack()) {
                speciaStatups.put(stat.getKey(), stat.getValue());
            } else {
                ordinaryStatups.put(stat.getKey(), stat.getValue());
            }
        }
        //开始处理普通的BUFF属性
        for (Map.Entry<MapleBuffStat, Integer> stat : ordinaryStatups.entrySet()) {
            if (stat.getKey() == MapleBuffStat.击杀点数 && buffid == 侠盗.侠盗本能) {
                count = stat.getValue();
                isZeroUnknown = true;
                break; //跳出和结束这个循环
            }
            //貌似有些要写Int
            if (isMountBuff || isWriteIntSkill || stat.getKey() == MapleBuffStat.影分身 || stat.getKey() == MapleBuffStat.伤害置换 || stat.getKey() == MapleBuffStat.重生符文 || stat.getKey() == MapleBuffStat.三彩箭矢) {
                mplew.writeInt(stat.getValue());
            } else if (buffid == 爆莉萌天使.灵魂凝视) {
                mplew.writeShort(stat.getValue() / 2); //这个地方我用是 x 而 封包必须这个地方是 y 下面 x
                mplew.writeShort(stat.getValue());
//            } else if (stat.getKey() == MapleBuffStat.鹰眼) { //好像这个有点特殊 是2个 0x14 也就是1个暴击概率1个暴击最大伤害
//                mplew.write(stat.getValue().byteValue());
//                mplew.write(stat.getValue().byteValue());
            } else {
                mplew.writeShort(stat.getValue());
            }

            switch (buffid) {
                case 恶魔复仇者.血之契约:
                    mplew.writeInt(buffid == 恶魔复仇者.血之契约 ? 0 : buffid); //好像血之契约这个地方的BUFFID写的 0
                    break;
                case 黑骑士.灵魂助力统治:
                case 黑骑士.灵魂助力震惊:
                    mplew.writeInt(黑骑士.灵魂助力);
                    break;
                default:
                    mplew.writeInt(buffid);
            }

//            if (!buffStat.contains(MapleBuffStat.极限射箭)) {
//                mplew.writeInt(bufflength);
//            }
            mplew.writeInt(bufflength);
            if (stat.getKey().isSpecial()) { //未知 有些特殊的BUFF 这个地方要多[00 00 00 00]
                special = true;
            }
        }
        //发送中间的字节
        mplew.writeShort(0);
        switch (buffid) {
            case 剑豪.拔刀姿势:
                mplew.writeHexString("E8 79 9D FD 00 00 00");
                break;
            default:
                mplew.writeZeroBytes(3);
                if (special) {
                    if (buffStat.contains(MapleBuffStat.百分比无视防御) && buffid == 奇袭者.元素_闪电) {
                        mplew.writeInt(count);
                    } else {
                        mplew.writeInt(0);
                    }
                }
                break;
        }

        if (isZeroUnknown) {
            mplew.write(count);
        } else if (buffStat.contains(MapleBuffStat.火眼晶晶)) {
            mplew.writeInt(0); //这个地方是否带无视怪物防御
        } else if (buffStat.contains(MapleBuffStat.极限射箭)) {
            mplew.writeInt(effect.getX()); //减少的百分比防御
            mplew.writeInt(effect.getZ()); //暴击最小伤害增加
        } else if (buffStat.contains(MapleBuffStat.交叉锁链)) {
            mplew.writeInt(0x01);
        } else if (buffStat.contains(MapleBuffStat.生命潮汐)) {
            mplew.writeInt(0);
        } else if (buffStat.contains(MapleBuffStat.重生符文)) {
            mplew.writeInt(122); //7A 00 00 00 不知道是怎么处理的
        } else if (buffStat.contains(MapleBuffStat.三彩箭矢)) {
            mplew.writeInt(1);
        } else if (buffStat.contains(MapleBuffStat.灵魂助力)) {
            mplew.writeInt(effect.isOnRule() ? 黑骑士.灵魂助力统治 : 黑骑士.灵魂助力);
            mplew.writeInt(0);
        } else if (buffStat.contains(MapleBuffStat.元素冲击)) {
            mplew.write(count); //秒杀概率?
            mplew.writeShort(count * 12); //攻击加成
            mplew.write(count * 2);
            mplew.write(count * 2);
        } else if (buffStat.contains(MapleBuffStat.招魂结界)) {
            mplew.writeInt(0x01);
        } else if (buffStat.contains(MapleBuffStat.激素狂飙)) {
            mplew.write(1);
        }

        if (darklight) {
            mplew.writeInt(buffid);
            mplew.writeInt(DateUtil.getSpecialNowiTime());
            mplew.writeLong(0);
            mplew.writeInt(-1);
            mplew.writeLong(10000);
        } else {
            mplew.writeZeroBytes(4);
        }

        //处理9个 00 之后的状态
        if (buffStat.contains(MapleBuffStat.飞行骑乘)) {
            mplew.writeInt(0); //必须写个int
        } else if (buffStat.contains(MapleBuffStat.飞翔)) {
            mplew.write(0);
        }
        //开始处理特殊的BUFF属性
        for (Map.Entry<MapleBuffStat, Integer> stat : speciaStatups.entrySet()) {
            if (stat.getKey() == MapleBuffStat.骑兽技能) {
                int mountId = stat.getValue();
                mplew.writeInt(mountId); //骑宠ID
                mplew.writeInt(buffid); //技能ID
                mplew.write(0); //当为机械师的骑宠ID这个地方为 1
                mplew.writeInt(0); //bufflength 貌似骑宠的为0 当为机械师的骑宠ID这个地方为 1
            } else {
                List<SpecialBuffInfo> buffs = Arrays.asList(new SpecialBuffInfo(buffid, stat.getValue(), bufflength, System.currentTimeMillis()));
                mplew.writeInt(buffs.size()); //这个地方是有多少个重复的特殊BUFF 是1个循环
                for (SpecialBuffInfo info : buffs) {
                    mplew.writeInt(info.buffid);
                    mplew.writeInt(info.value);
                    mplew.writeInt((int) info.time); //未知 反正是个很大的数字而且是变动的 [08 F1 55 38]
                    mplew.writeInt(0); //V.114新增
                    mplew.writeInt(isMountBuff ? 0 : info.bufflength); //这个地方如果带有骑宠好像是 0
                    mplew.writeInt(0);
                }
            }
        }
        //-------------------------------------------------------------------
        mplew.writeInt(0); //未知 不知道是范围还是其他的 [E8 03 00 00]
        mplew.write(1); //V.112.1新增 有时为 00
        /*
         * 移动速度
         * 隐身术
         * 冒险岛勇士
         * 尖兵飞行
         * 黄色灵气
         */
        if (isMountBuff || buffStat.contains(MapleBuffStat.变身效果) || buffStat.contains(MapleBuffStat.移动速度) || buffStat.contains(MapleBuffStat.跳跃力) || buffStat.contains(MapleBuffStat.增加跳跃力) || buffStat.contains(MapleBuffStat.增加移动速度) || buffStat.contains(MapleBuffStat.冒险岛勇士) || buffStat.contains(MapleBuffStat.金属机甲) || buffStat.contains(MapleBuffStat.战法灵气) || buffStat.contains(MapleBuffStat.变形值) || buffStat.contains(MapleBuffStat.能量获得) || buffStat.contains(MapleBuffStat.疾驰速度) || buffStat.contains(MapleBuffStat.疾驰跳跃) || buffStat.contains(MapleBuffStat.飞行骑乘)) {
            mplew.write(0x04);
        }
        mplew.writeInt(0); //V.112.1新增

        return mplew.toString();
    }

    public static <E extends Buffstat> void writeBuffMask(MaplePacketLittleEndianWriter mplew, Map<E, Integer> statups) {
        int[] mask = new int[GameConstants.MAX_BUFFSTAT];
        for (E statup : statups.keySet()) {
            mask[statup.getPosition()] |= statup.getValue();
        }
        for (int aMask : mask) {
            mplew.writeInt(aMask);
        }
    }

    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void testSkill() throws Exception {
        int skillid = 龙神.龙神;
        Skill skill = Skill.loadFromData(skillid, datasource.getData((skillid / 10000) + ".img").getChildByPath("skill/" + skillid), delayData);
        MapleStatEffect effect = skill.getEffect(skill.getMaxLevel());
        System.out.println(effect.isOverTime());
        System.out.println(giveBuff(skillid, -1, effect.getStatups(), effect));
    }
}