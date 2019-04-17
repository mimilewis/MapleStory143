/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.skills.KSPsychicSkillEntry;
import configs.ServerConfig;
import constants.skills.*;
import handling.opcode.EffectOpcode;
import handling.opcode.MessageOpcode;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.Pair;
import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;

/**
 * @author admin
 */
public class EffectPacket {

    private static final Logger log = LogManager.getLogger(EffectPacket.class);

    public static byte[] showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel) {
        return showOwnBuffEffect(skillid, effectid, playerLevel, skillLevel, (byte) 0x04);
    }

    public static byte[] showOwnBuffEffect(int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        return showBuffeffect(null, skillid, effectid, playerLevel, skillLevel, direction);
    }

    public static byte[] showBuffeffect(MapleCharacter chr, int skillid, int effectid, int playerLevel, int skillLevel) {
        return showBuffeffect(chr, skillid, effectid, playerLevel, skillLevel, (byte) 0x04);
    }

    public static byte[] showBuffeffect(MapleCharacter chr, int skillid, int effectid, int playerLevel, int skillLevel, byte direction) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chr == null) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chr.getId());
        }
        mplew.write(effectid);
        if (effectid == 2) {
            mplew.writeInt(0);
        }
        mplew.writeInt(skillid);
        mplew.write(playerLevel);
        mplew.write(skillLevel);
        if (effectid != 0x03) {
            mplew.write(direction); //角色等级 好像有些需要写角色等级
        }
        switch (skillid) {
            case 爆莉萌天使.超级诺巴:
                if (chr != null) {
                    mplew.writeInt(chr.getTruePosition().x);
                    mplew.writeInt(chr.getTruePosition().y);
                } else {
                    mplew.writeLong(0);
                }
                mplew.write(1);
                break;
            case 黑骑士.重生契约:
            case 龙神.龙的愤怒: {
                mplew.write(0);
                break;
            }
            case 双刀.地狱锁链: {
                mplew.write(0);
                mplew.writeInt(0);
                break;
            }
            case 战神.终极投掷_2:
            case 神射手.寒冰爪钩:
            case 箭神.寒冰爪钩: {
                mplew.write(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
            }
            case 预备兵.捕获: {
                mplew.write(0);
            }
        }
        if (skillid != 奇袭者.疾电 && skillid != 隐月.缩地 && skillid != 侠盗.暗影神行 && skillid != 龙的传人.纵步突打) {
            switch (skillid) {
                case 侠盗.潜影杀机:
                case 爆莉萌天使.超级诺巴: {
                    if (chr != null) {
                        mplew.writeInt(chr.getTruePosition().x);
                        mplew.writeInt(chr.getTruePosition().y);
                    } else {
                        mplew.writeLong(0);
                    }
                    break;
                }
                case 爆破手.装填弹药:
                case 爆破手.转管炮:
                case 爆破手.急速闪避_1:
                case 爆破手.急速闪避:
                case 爆破手.重锤出击:
                case 爆破手.重锤出击_1:
                case 爆破手.摇摆不定:
                case 爆破手.摇摆不定_1: {
                    mplew.writeInt(0);
                    break;
                }
                case 幻影.小丑_2:
                case 幻影.小丑_3:
                case 幻影.小丑_4:
                case 幻影.小丑_5:
                case 幻影.小丑_6: {
                    mplew.writeInt(0);
                }
            }
        }
        if (chr == null && skillid == 预备兵.猎人的召唤) {
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
        }
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 角色自己看到幸运骰子BUFF效果
     */
    public static byte[] showOwnDiceEffect(int skillid, int effectid, int effectid2, int level) {
        return showDiceEffect(-1, skillid, effectid, effectid2, level);
    }

    /*
     * 别人看到的幸运骰子BUFF效果
     */
    public static byte[] showDiceEffect(int chrId, int skillid, int effectid, int effectid2, int level) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_SkillAffected_Ex.getValue());
        mplew.writeInt(effectid);
        mplew.writeInt(effectid2);
        mplew.writeInt(skillid);
        mplew.write(level);
        mplew.write(0); //如果是双幸运骰子发动2个效果 这个地放第2个效果为1

        return mplew.getPacket();
    }

    /*
     * 装备道具等级提升
     */
    public static byte[] showItemLevelupEffect() {
        return showSpecialEffect(EffectOpcode.UserEffect_ItemLevelUp.getValue());
    }

    /*
     * 显示给其他玩家看到道具等级提升效果
     */
    public static byte[] showForeignItemLevelupEffect(int chrId) {
        return showForeignEffect(chrId, EffectOpcode.UserEffect_ItemLevelUp.getValue());
    }

    /*
     * 显示给自己看到的特殊效果
     * 0x0A = 使用护身符1次 [1E 02] [0A] [01 00 00 00 00 00]
     * 0x0D = 背后有个天使效果
     * 0x0E = 完成任务效果
     * 0x0F = 回血效果
     * 0x10 = 身上有个光点
     * 0x16 = 道具等级提升效果
     * 0x15 = 头上有1个毡子 后面为0 = 成功 为 1 = 失败效果
     * 0x16 = 身上有个光点效果
     * 0x18 = 消耗1个原地复活术，在当前地图复活了。（剩余x个） 后面接着是1个 Int
     * 0x1F = 因灵魂石的效果，在当前地图中复活
     * 0x20 = 显示掉血伤害多少? 0 = Miss
     * 0x22 = 显示自己恢复Hp效果
     * 0x2F = 爆莉萌天使灵魂重生
     */
    public static byte[] showSpecialEffect(int effect) {
        return showForeignEffect(-1, effect);
    }

    public static byte[] showForeignEffect(int chrId, int effect) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(effect);

        return mplew.getPacket();
    }

    /*
     * 看到自己恢复Hp效果
     * 好像为 0x0F
     * 下面是恢复12点的例子
     * Recv SHOW_SPECIAL_EFFECT [021E] (4)
     * 1E 02 0F 0C
     * V.119.1 OK
     */
    public static byte[] showOwnHpHealed(int amount) {
        return showHpHealed(-1, amount);
    }

    /*
     * 看到其他角色恢复HP效果
     * V.119.1 OK
     */
    public static byte[] showHpHealed(int chrId, int amount) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_IncDecHPRegenEffect.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    /*
     * 显示自己获得 黑暗祝福 效果
     * V.119.1 OK
     */
    public static byte[] showBlessOfDarkness(int skillId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_SkillSpecial.getValue());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    /*
     * 工艺制作
     * 显示自己道具制作效果
     */
    public static byte[] showOwnCraftingEffect(String effect, int time, int mode) {
        return showCraftingEffect(-1, effect, time, mode);
    }

    /*
     * 工艺制作
     * 显示其他玩家道具制作效果
     */
    public static byte[] showCraftingEffect(int chrId, String effect, int time, int mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_EffectUOL.getValue());
        mplew.writeMapleAsciiString(effect);
        mplew.write(1);
        mplew.writeInt(time);
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    /*
     * 显示使用卡勒塔的许愿珍珠的效果
     */
    public static byte[] showOwnJobChangedElf(String effect, int time, int itemId) {
        return showJobChangedElf(-1, effect, time, itemId);
    }

    /*
     * 显示别人使用卡勒塔的许愿珍珠的效果
     */
    public static byte[] showJobChangedElf(int chrId, String effect, int time, int itemId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_EffectUOL.getValue());
        mplew.writeMapleAsciiString(effect);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(time);
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    /*
     * 显示随机获得道具效果
     * V.119.1 OK
     */
    public static byte[] showRewardItemAnimation(int itemId, String effect) {
        return showRewardItemAnimation(itemId, effect, -1);
    }

    /*
     * 显示其他玩家随机获得道具效果
     */
    public static byte[] showRewardItemAnimation(int itemId, String effect, int chrId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_LotteryUse.getValue()); //V.119.1 = 0x12
        mplew.writeInt(itemId);
        mplew.write(effect != null && effect.length() > 0 ? 1 : 0);
        if (effect != null && effect.length() > 0) {
            mplew.writeMapleAsciiString(effect);
        }

        return mplew.getPacket();
    }

    /*
     * V.119.1 OK
     */
    public static byte[] Mulung_DojoUp2() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_PlayPortalSE.getValue());

        return mplew.getPacket();
    }

    /*
     * 道具制造
     * V.119.1 OK
     */
    public static byte[] ItemMaker_Success() {
        return ItemMaker_Success_3rdParty(-1);
    }

    public static byte[] ItemMaker_Success_3rdParty(int chrId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_ItemMaker.getValue());
        mplew.writeInt(0); //成功 = 0 失败 =1

        return mplew.getPacket();
    }

    /*
     * 显示宠物升级效果
     * V.119.1 OK
     */
    public static byte[] showOwnPetLevelUp(byte index) {
        return showPetLevelUp(-1, index);
    }

    public static byte[] showPetLevelUp(int chrId, byte index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_Pet.getValue());
        mplew.write(0);
        mplew.writeInt(index);

        return mplew.getPacket();
    }

    /*
     * V.119.1 OK
     */
    public static byte[] AranTutInstructionalBalloon(String data) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_AvatarOriented.getValue());
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    /*
     * V.120.1  OK
     */
    public static byte[] ShowWZEffect(String data) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_PlaySoundWithMuteBGM.getValue()); //0x18
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    /*
     * 获取和丢失装备的提示 - 2
     * V.119.1 OK
     */
    public static byte[] getShowItemGain(List<Pair<Integer, Integer>> showItems) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_SkillSpecialAffected.getValue()); //V.119.1修改以前 0x05
        mplew.write(showItems.size());
        for (Pair<Integer, Integer> items : showItems) {
            mplew.writeInt(items.left);
            mplew.writeInt(items.right);
        }
        return mplew.getPacket();
    }

    public static byte[] getShowItemGain(final int itemid, final short amount, final boolean b) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        if (b) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
            mplew.write(EffectOpcode.UserEffect_Quest.getValue());
            mplew.write(1);
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
            mplew.writeShort(MessageOpcode.MS_DropPickUpMessage.getValue());
        }
        mplew.writeInt(itemid);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    /*
     * 显示尖兵获得电池
     */
    public static byte[] showOwnXenonPowerOn(String effect) {
        return showXenonPowerOn(-1, effect);
    }

    public static byte[] showXenonPowerOn(int chrId, String effect) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (chrId == -1) {
            mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        } else {
            mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
            mplew.writeInt(chrId);
        }
        mplew.write(EffectOpcode.UserEffect_UpgradeTombItemUse.getValue());
        mplew.writeMapleAsciiString(effect);

        return mplew.getPacket();
    }

    /*
     * 显示 三彩箭矢 效果
     */
    public static byte[] showArrowsEffect(int skillId, int mode, int arrows) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_ActQuestComplete.getValue());
        mplew.writeInt(skillId); //技能ID
        mplew.writeInt(mode); //当前的模式
        mplew.writeInt(arrows); //箭矢数量

        return mplew.getPacket();
    }

    public static byte[] show影朋小白效果(int skillid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.HakuSkillEffect.getValue());
        mplew.writeShort(0);
        mplew.writeInt(skillid);
        mplew.write(1);
        mplew.writeShort(0x0F);

        return mplew.getPacket();
    }

    public static byte[] playerDeadConfirm(int type, int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_DEAD.getValue());
        mplew.writeInt(type);
        mplew.write(0);
        if ((type & 1) != 0) {
            mplew.writeInt(value);
        }
        if ((type & 2) != 0) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] getEffectSwitch(int cid, List<Integer> items) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.EFFECT_SWITCH.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(items.size());
        for (int i : items) {
            mplew.writeInt(i);
        }
        mplew.writeBool(false);

        return mplew.getPacket();
    }

    // 心魂之手 抓取
    public static byte[] showKSPsychicGrab(int cid, int skillid, short skilllevel, List<KSPsychicSkillEntry> ksse) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_KSPSYCHIC.getValue());

        mplew.writeInt(cid);
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeShort(skilllevel);
        mplew.writeInt(1199); // AF 04 00 00 
        mplew.writeInt(6);

        for (KSPsychicSkillEntry k : ksse) {
            mplew.write(1);
            mplew.write(1);
            mplew.writeInt(k.getOid());
            mplew.writeInt(Math.abs(k.getOid()));
            if (k.getMobOid() != 0) {
                mplew.writeInt(k.getMobOid());
                mplew.writeShort(0);
                mplew.writeLong(150520);
                mplew.writeLong(150520);
            } else {
                mplew.writeInt(0);
                mplew.writeShort(Randomizer.nextInt(19) + 1);
                mplew.writeLong(100);
                mplew.writeLong(100);
            }
            mplew.write(1);
            mplew.writeInt(k.getN1());
            mplew.writeInt(k.getN2());
            mplew.writeInt(k.getN3());
            mplew.writeInt(k.getN4());
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showKSPsychicAttack(int cid, int skillid, short skilllevel, int n1, int n2, byte n3, int n4, int n5, int n6, int n7, int n8, int n9, int n10, int n11, int n12) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.ATTACK_KSPSYCHIC.getValue());

        mplew.writeInt(cid);
        mplew.writeInt(skillid);
        mplew.writeShort(skilllevel);
        mplew.writeInt(n1);
        mplew.writeInt(n2);
        mplew.write(n3);
        mplew.writeInt(n4);
        if (n4 != 0) {
            mplew.writeInt(n5);
            mplew.writeInt(n6);
        }
        mplew.writeInt(n7);
        mplew.writeInt(n8);
        mplew.writeInt(n9);
        mplew.writeInt(n10);
        if (skillid == 超能力者.心魂粉碎2 || skillid == 超能力者.心魂粉碎2_最后一击 || skillid == 超能力者.终极_心魂弹) {
            mplew.writeInt(n11);
            mplew.writeInt(n12);
        }

        return mplew.getPacket();
    }

    public static byte[] showKSPsychicRelease(int cid, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_KSPSYCHIC.getValue());

        mplew.writeInt(cid);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] showGiveKSUltimate(int chrid, int mode, int type, int oid, int skillid, short skilllevel, int n1, byte n2, short n3, short n4, short n5, int n6, int n7) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GIVE_KSULTIMATE.getValue());

        mplew.writeInt(chrid);
        mplew.write(1);
        mplew.writeInt(mode);
        mplew.writeInt(type);
        mplew.writeInt(oid);
        mplew.writeInt(skillid);
        mplew.writeShort(skilllevel);
        mplew.writeInt(Math.abs(oid));
        mplew.writeInt(n1);
        mplew.write(n2);
        mplew.writeShort(n3);
        mplew.writeShort(n4);
        mplew.writeShort(n5);
        mplew.writeInt(n6);
        mplew.writeInt(n7);

        return mplew.getPacket();
    }

    public static byte[] showAttackKSUltimate(int oid, int attackcount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.KSULTIMATE_ATTACK.getValue());

        mplew.writeInt(oid);
        mplew.writeInt(attackcount);

        return mplew.getPacket();
    }

    public static byte[] showCancelKSUltimate(int chrid, int oid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANCEL_KSULTIMATE.getValue());

        mplew.writeInt(chrid);
        mplew.writeInt(oid);

        return mplew.getPacket();
    }

    public static byte[] showExpertEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.EXPERT_EFFECT.getValue());

        return mplew.getPacket();
    }

    public static byte[] showCombustionMessage(String text, int second) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_EFFECT.getValue());
        mplew.write(EffectOpcode.UserEffect_TextEffect.getValue());
        mplew.writeMapleAsciiString("#fnNanum GothicExtraBold##fs26#" + text);
        mplew.writeInt(50);
        mplew.writeInt(second * 1000);
        mplew.writeInt(4);
        mplew.writeInt(0);
        mplew.writeInt(-200);
        mplew.writeInt(1);
        mplew.writeInt(4);
        mplew.writeInt(2);
        mplew.writeLong(0);

        return mplew.getPacket();
    }

    //its likely that durability items use this
    public static byte[] showHpHealed_Other(int chrId, int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(EffectOpcode.UserEffect_IncDecHPRegenEffect.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }
}
