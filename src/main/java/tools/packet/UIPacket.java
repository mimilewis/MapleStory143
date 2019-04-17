package tools.packet;

import configs.ServerConfig;
import constants.ServerConstants;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;

public class UIPacket {

    private static final Logger log = LogManager.getLogger(UIPacket.class);

    public static byte[] getSPMsg(byte sp, short job) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ServerConstants.MapleStatusInfo.获得SP.getType());
        mplew.writeShort(job);
        mplew.write(sp);

        return mplew.getPacket();
    }

    public static byte[] getGPMsg(int itemid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // Temporary transformed as a dragon, even with the skill ......
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ServerConstants.MapleStatusInfo.获得家族点.getType());
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] getBPMsg(int amount) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // Temporary transformed as a dragon, even with the skill ......
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x17);
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] getGPContribution(int itemid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // Temporary transformed as a dragon, even with the skill ......
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ServerConstants.MapleStatusInfo.获得贡献度.getType());
        mplew.writeInt(itemid);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getTopMsg(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TOP_MSG.getValue());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getMidMsg(String msg, boolean keep, int index) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MID_MSG.getValue());
        mplew.write(index); //where the message should appear on the screen
        mplew.writeMapleAsciiString(msg);
        mplew.write(keep ? 0 : 1);

        return mplew.getPacket();
    }

    public static byte[] clearMidMsg() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLEAR_MID_MSG.getValue());

        return mplew.getPacket();
    }

    /*
     * TYPE 公告类型汇总
     * 1 = 礼物箱
     * 2 = 雪花
     * 3 = 巧克力
     * 4 = 一束花
     * 5 = 棒棒糖
     * 6 = 枫叶
     * 7 = 烟花
     * 8 = 赛跑的姨妈巾
     * 9 = 红蓝配
     * A = 足球
     * B = 人参汤
     * C = 彩色土豆拼盘
     * D = 糖果拼盘
     * E = 夜空中最亮的星
     * F = 圣诞礼物
     * 10 = 气球
     * 11 = 可乐
     * 12 = 透明背景的人参汤
     * 13 = 玫瑰花
     * 14 = 金鱼
     * 15 = 雪人
     * 16 = 福到
     * 17 = 爱心巧克力
     * 18 = Happy熊
     * 19 = 老虎
     * 1A = 克拉拉
     * 1B = 警告牌
     * 1C = 云朵
     */
    public static byte[] getMapEffectMsg(String msg, int type, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAP_EFFECT_MSG.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.writeInt(type);
        mplew.write(0xA0);
        mplew.writeInt(time * 3);

        return mplew.getPacket();
    }

    /*
     * 特殊的顶部公告
     * unk = 0宋体 3黑体 7雅园 8小黄
     * 字体大小最大128
     */
    public static byte[] getSpecialTopMsg(String msg, int unk, int fontsize, int color) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPECIAL_TOP_MSG.getValue());
        mplew.writeInt(unk); //字体
        mplew.writeInt(fontsize); //字体大小
        mplew.writeInt(color); //颜色代码
        mplew.writeInt(0); //未知
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getStatusMsg(int itemid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // Temporary transformed as a dragon, even with the skill ......
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ServerConstants.MapleStatusInfo.显示消耗品描述.getType());
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showMapEffect(String path) {
        return MaplePacketCreator.environmentChange(path, 4); //T072修改为 4 以前为 3
    }

    public static byte[] MapNameDisplay(int mapid) {
        return MaplePacketCreator.environmentChange("maplemap/enter/" + mapid, 4); //T072修改为 4 以前为 3
    }

    public static byte[] Aran_Start() {
        return MaplePacketCreator.environmentChange("Aran/balloon", 5); //T072修改为 5 以前为 4
    }

    public static byte[] playMovie(String data, boolean show) {
        if (show) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAY_MOVIE.getValue());
        mplew.writeMapleAsciiString(data);
        mplew.write(show ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] summonHelper(boolean summon) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SUMMON_HINT.getValue());
        mplew.write(summon ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] summonMessage(int type) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
        mplew.write(1);
        mplew.writeInt(type);
        mplew.writeInt(7000); // probably the delay

        return mplew.getPacket();
    }

    public static byte[] summonMessage(String message) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SUMMON_HINT_MSG.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(message);
        mplew.writeInt(200); // IDK
        mplew.writeInt(10000); // Probably delay

        return mplew.getPacket();
    }

    public static byte[] IntroLock(boolean enable) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INTRO_LOCK.getValue());
        mplew.write(enable ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getDirectionStatus(boolean enable) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_STATUS.getValue());
        mplew.write(enable ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfo(int type, int value) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(type);
        if (type == 9) {
            mplew.write(value);
        } else {
            mplew.writeInt(value);
        }

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfo(int value, int x, int y) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(value);
        mplew.writeInt(x);
        mplew.writeInt(y);

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfo(String data, int value, int x, int y, int pro) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(2);
        mplew.writeMapleAsciiString(data);
        mplew.writeInt(value);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeShort(pro);
        mplew.writeInt(0); //only if pro is > 0

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfo(String data, int value, int x, int y, int a, int b) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(2);
        mplew.writeMapleAsciiString(data);
        mplew.writeInt(value);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.write(a);
        if (a > 0) {
            mplew.writeInt(0);
        }
        mplew.write(b);
        if (b > 1) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfoNew(byte type, int value) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(5);
        mplew.write(type);
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] getDirectionInfoNew(byte type, int x, int y, int z) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(5);
        mplew.write(type);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.writeInt(z);

        return mplew.getPacket();
    }

    public static byte[] getDIRECTION_INFO(String data, int value, int s) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(6);
        mplew.writeMapleAsciiString(data);
        mplew.writeInt(value);
        mplew.writeInt(0);
        mplew.writeInt(s);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] getDirectionEffect(String data, int value, int x, int y) {
        return getDirectionEffect(data, value, x, y, 0);
    }

    public static byte[] getDirectionEffect(String data, int value, int x, int y, int z) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(2);
        mplew.writeMapleAsciiString(data);
        mplew.writeInt(value);
        mplew.writeInt(x);
        mplew.writeInt(y);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeInt(z);
        mplew.write(z == 0 ? 1 : 0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getDirectionEffect(int mod, String data, int value, int value2, int value3, int a1, int a2, int a3, int a4, int npc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DIRECTION_INFO.getValue());
        mplew.write(mod);
        switch (mod) {
            case 0: {
                mplew.writeInt(value);
                if (value > 1195) break;
                mplew.writeInt(value2);
                break;
            }
            case 1: {
                mplew.writeInt(value);
                break;
            }
            case 2: {
                mplew.writeMapleAsciiString(data);
                mplew.writeInt(value);
                mplew.writeInt(value2);
                mplew.writeInt(value3);
                mplew.write(a1);
                if (a1 > 0) {
                    mplew.writeInt(a3);
                }
                mplew.write(a2);
                if (a2 <= 0) break;
                mplew.writeInt(npc);
                mplew.write(npc > 0 ? 0 : 1);
                mplew.write(a4);
                break;
            }
            case 3: {
                mplew.writeInt(value);
                break;
            }
            case 4: {
                mplew.writeMapleAsciiString(data);
                mplew.writeInt(value);
                mplew.writeInt(value2);
                mplew.writeInt(value3);
                break;
            }
            case 5: {
                mplew.write(value);
                mplew.writeInt(value2);
                if (value2 <= 0 || value != 0) break;
                mplew.writeInt(value3);
                mplew.writeInt(a1);
                break;
            }
            case 6: {
                mplew.writeInt(value);
                break;
            }
            case 7: {
                mplew.writeInt(value);
                mplew.writeInt(value2);
                mplew.writeInt(value3);
                mplew.writeInt(a1);
                mplew.writeInt(a2);
                break;
            }
            case 8: {
                break;
            }
            case 9: {
                mplew.write(value);
                break;
            }
            case 10: {
                mplew.writeInt(value);
                break;
            }
            case 11: {
                mplew.writeMapleAsciiString(data);
                mplew.write(value);
                break;
            }
            case 12: {
                mplew.writeMapleAsciiString(data);
                mplew.write(value);
                mplew.writeShort(value2);
                mplew.writeInt(value3);
                mplew.writeInt(a1);
                break;
            }
            case 13: {
                mplew.write(value);
                for (int i2 = 0; i2 <= value; ++i2) {
                    mplew.writeInt(value2);
                }
                break;
            }
            case 14: {
                break;
            }
            case 15: {
                mplew.writeInt(value);
                mplew.writeInt(value2);
                break;
            }
            case 16: {
                mplew.writeInt(value);
                break;
            }
            case 17: {
                mplew.write(value);
                break;
            }
            default: {
                System.out.println("getDirectionInfo() is Unknow mod :: [" + mod + "]");
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getDirectionFacialExpression(int expression, int duration) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USER_EMOTION_LOCAL.getValue());
        mplew.writeInt(expression);
        mplew.writeInt(duration);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] IntroEnableUI(int wtf) {
        return IntroEnableUI(wtf, true);
    }

    public static byte[] IntroEnableUI(int wtf, boolean block) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INTRO_LOCK_MOVIE.getValue());
        mplew.writeBool(wtf > 0);
        if (wtf > 0) {
            mplew.writeShort(block ? 1 : 0);
            mplew.write(0);
        } else {
            mplew.writeBool(wtf < 0);
        }

        return mplew.getPacket();
    }

    public static byte[] IntroDisableUI(boolean enable) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CYGNUS_INTRO_DISABLE_UI.getValue());
        mplew.write(enable ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getNpcNotice(int npcid, String text, int time) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.NPC_NOTICE.getValue());
        mplew.writeInt(npcid);
        mplew.writeInt(Math.max(1000, time));
        mplew.writeMapleAsciiString(text);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    /**
     * 显示自由市场小地图，该数据包只对自由市场生效。
     *
     * @参数 show true ? 隐藏 : 显示
     */
    public static byte[] showFreeMarketMiniMap(boolean show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FM_HIDE_MINIMAP.getValue());
        mplew.writeReversedBool(show);

        return mplew.getPacket();
    }

    /**
     * 让客户端打开指定窗口
     *
     * @param id 类似于子类型
     * @return
     */
    public static byte[] sendOpenWindow(int id) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_WINDOW.getValue());
        mplew.writeInt(id);

        return mplew.getPacket();
    }

    /**
     * 打开新的聊天界面
     *
     * @param npc
     * @return
     */
    public static byte[] sendPVPWindow(int npc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_WINDOW.getValue());
        mplew.writeInt(0x32);
        if (npc > 0) {
            mplew.writeInt(npc);
        }

        return mplew.getPacket();
    }

    /**
     * 打开活动列表界面
     *
     * @param npc
     * @return
     */
    public static byte[] sendEventWindow(int npc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_WINDOW.getValue());
        mplew.writeInt(0x37);
        if (npc > 0) {
            mplew.writeInt(npc);
        }

        return mplew.getPacket();
    }

    public static byte[] inGameCurNodeEventEnd(boolean enable) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.IN_GAME_CUR_NODE_EVENT_END.getValue());
        mplew.writeBool(enable);

        return mplew.getPacket();
    }

    public static byte[] sendSceneUI() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SCENE_UI.getValue());
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
         * 打开1个游戏窗口界面
         */
    public static byte[] sendUIWindow(int op, int npc) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REPAIR_WINDOW.getValue());
        /*
         * 0x03 传授技能后显示的窗口
         * 0x15 组队搜索窗口
         * 0x21 道具修理窗口
         * 0x2A 专业技术窗口
         */
        mplew.writeInt(op);
        mplew.writeInt(npc);
        mplew.writeInt(0); //V.114新增 未知

        return mplew.getPacket();
    }

    public static byte[] showPQEffect(int n2, String string, String string2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PQ_EFFECT.getValue());
        mplew.write(n2);
        switch (n2) {
            case 1:
            case 3: {
                mplew.writeMapleAsciiString(string);
                mplew.writeMapleAsciiString(string2);
                break;
            }
            case 2:
            case 4: {
                mplew.writeMapleAsciiString(string);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] screenShake(int n2, boolean bl2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SCREEN_SHAKE.getValue());
        mplew.writeInt(n2);
        mplew.writeInt(bl2 ? 0 : 20);
        mplew.writeInt(bl2 ? 0 : 50);
        mplew.writeInt(bl2 ? 0 : 20);

        return mplew.getPacket();
    }
}
