package tools;

import client.*;
import client.inventory.*;
import client.skills.SkillEntry;
import client.skills.SkillMacro;
import configs.ServerConfig;
import constants.*;
import constants.ServerConstants.MapleStatusInfo;
import constants.skills.*;
import handling.channel.DojoRankingsData;
import handling.channel.handler.AttackInfo;
import handling.channel.handler.InventoryHandler;
import handling.opcode.SendPacketOpcode;
import handling.world.WorldAllianceService;
import handling.world.WorldGuildService;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleDueyActions;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import server.RankingWorker;
import server.events.MapleSnowball;
import server.maps.*;
import server.maps.MapleNodes.MaplePlatform;
import server.movement.LifeMovementFragment;
import server.quest.MapleQuest;
import server.shops.HiredFisher;
import server.shops.HiredMerchant;
import server.shops.MaplePlayerShopItem;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.packet.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

public class MaplePacketCreator {

    public final static List<Pair<MapleStat, Long>> EMPTY_STATUPDATE = Collections.emptyList();
    private static final Logger log = LogManager.getLogger(MaplePacketCreator.class);

    public static byte[] getWzCheck(String WzCheckPack) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.WZ_CHECK.getValue());
        mplew.write(HexTool.getByteArrayFromHexString(WzCheckPack));
        return mplew.getPacket();
    }

    /**
     * 客户端验证
     *
     * @param fileValue
     * @return 返回客户端检查结果
     */
    public static byte[] getClientAuthentication(int fileValue) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CLIENT_AUTH.getValue());
        mplew.writeInt(fileValue);
        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the channel server.
     *
     * @param c
     * @param port   The port the channel is on.
     * @param charId
     * @return The server IP packet.
     */
    public static byte[] getServerIP(MapleClient c, int port, int charId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVER_IP.getValue());
        mplew.writeShort(0);
        mplew.write(ServerConstants.NEXON_IP);
        mplew.writeShort(port);
//        mplew.write(ServerConstants.NEXON_IP);
//        mplew.writeShort(ChatServer.getPort());
        mplew.writeInt(0);
        mplew.writeShort(0);
        mplew.writeInt(0);
        mplew.writeInt(charId);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    /**
     * Gets a packet telling the client the IP of the new channel.
     *
     * @param c    The InetAddress of the requested channel server.
     * @param port The port the channel is on.
     * @return The server IP packet.
     */
    public static byte[] getChannelChange(MapleClient c, int port) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_CHANNEL.getValue());
        mplew.write(1);
        mplew.write(ServerConstants.NEXON_IP);
        mplew.writeShort(port);
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 隐藏头顶称号
     * V.112.1新增
     */
    public static byte[] cancelTitleEffect() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_TITLE_EFFECT.getValue());
        for (int i = 0; i < 5; i++) {
            mplew.writeShort(0);
            mplew.write(-1);
        }

        return mplew.getPacket();
    }

    public static byte[] getWarpToMap(MapleCharacter player, boolean firstLoggedIn) {
        return getWarpToMap(player, true, null, 0, firstLoggedIn);
    }

    public static byte[] getWarpToMap(MapleCharacter player, MapleMap to, int spawnPoint) {
        return getWarpToMap(player, false, to, spawnPoint, false);
    }

    /**
     * Gets character info for a character.
     *
     * @param player The character to get info about.
     * @return The character info packet.
     */
    public static byte[] getWarpToMap(MapleCharacter player, boolean load, MapleMap to, int spawnPoint, boolean firstLoggedIn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WARP_TO_MAP.getValue());
        mplew.writeShort(1); // 未知
        mplew.writeLong(1); // 未知
        mplew.writeInt(player.getClient().getChannel() - 1); // 频道
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(Math.min(player.getCMapCount(), 255));
        mplew.writeInt(!load ? to.getFieldType() : 0);
        mplew.writeInt(player.getMap().getBottom() - player.getMap().getTop());
        mplew.writeInt(player.getMap().getRight() - player.getMap().getLeft());
        mplew.writeBool(load);
        mplew.writeShort(0);
        if (load) {
            for (int i = 0; i < 3; i++) {
                mplew.writeInt(Randomizer.nextInt()); // 3个随机数字 Int
            }
            PacketHelper.addCharacterInfo(mplew, player);
            mplew.writeZeroBytes(20);
        } else {
            mplew.write(0);
            mplew.writeInt(to.getId()); //地图ID
            mplew.write(spawnPoint);
            mplew.writeInt(player.getStat().getHp()); // 角色HP
        }
        mplew.writeShort(0); //渐变画面
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.writeInt(0x64);

        // bl3 start 如果为真则加入数据
        mplew.write(0); // bl3
//        mplew.writeInt(0);
//        mplew.writeMapleAsciiString("");
//        mplew.writeInt(player.getMapId()); //  + 10000 后可更改地图背景
        // bl3 end

        mplew.write(0);
        mplew.writeBool(JobConstants.canUseFamiliar(player.getJob()));
        byte[] arrby = FamiliarPacket.writeWarpToMap(player, firstLoggedIn);
        mplew.writeInt(arrby.length);
        mplew.write(arrby);
        mplew.writeBool(false);
        mplew.writeBool(false);
        mplew.writeBool(false);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        if (to != null && to.getFieldType() == 0x3f) {
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    /**
     * Gets an empty stat update.
     *
     * @return The empy stat update packet.
     */
    public static byte[] enableActions() {
        return updatePlayerStats(MaplePacketCreator.EMPTY_STATUPDATE, true, null);
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats The stats to update.
     * @param chr
     * @return The stat update packet.
     */
    public static byte[] updatePlayerStats(List<Pair<MapleStat, Long>> stats, MapleCharacter chr) {
        return updatePlayerStats(stats, false, chr);
    }

    public static byte[] updatePlayerStats(List<Pair<MapleStat, Long>> stats, boolean itemReaction, MapleCharacter chr) {
        return updatePlayerStats(stats, itemReaction, chr, false);
    }

    /**
     * Gets an update for specified stats.
     *
     * @param stats        The list of stats to update.
     * @param itemReaction Result of an item reaction(?)
     * @param chr
     * @param isWarlock
     * @return The stat update packet.
     */
    public static byte[] updatePlayerStats(List<Pair<MapleStat, Long>> stats, boolean itemReaction, MapleCharacter chr, boolean isWarlock) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_STATS.getValue());
        mplew.write(itemReaction ? 1 : 0);
        long updateMask = 0;
        for (Pair<MapleStat, Long> statupdate : stats) {
            updateMask |= statupdate.getLeft().getValue();
        }
        if (stats.size() > 1) {
            stats.sort((o1, o2) -> {
                long val1 = o1.getLeft().getValue();
                long val2 = o2.getLeft().getValue();
                return (val1 < val2 ? -1 : (val1 == val2 ? 0 : 1));
            });
        }
        mplew.writeLong(updateMask);
        for (Pair<MapleStat, Long> statupdate : stats) {
            switch (statupdate.getLeft()) {
                case 皮肤: //0x01
                case 等级: //0x10
                case 疲劳: //0x80000
                    mplew.write(statupdate.right.byteValue());
                    break;
                case ICE_GAGE: //
                case 力量: //0x40
                case 敏捷: //0x80
                case 智力: //0x100
                case 运气: //0x200
                case AVAILABLEAP: //0x4000
                    mplew.writeShort(statupdate.getRight().shortValue());
                    break;
                case 职业: //0x20
                    mplew.writeShort(statupdate.getRight().shortValue());
                    mplew.writeShort(chr.getSubcategory());
                    break;
//                case 脸型: //0x02
//                case 发型: //0x04
//                case HP: //0x400
//                case MAXHP: //0x800
//                case MP: //0x1000
//                case MAXMP: //0x2000
//                case 人气: //0x20000
//                case 领袖: //0x100000
//                case 洞察: //0x200000
//                case 意志: //0x400000
//                case 手技: //0x800000
//                case 感性: //0x1000000
//                case 魅力: //0x2000000
//                    mplew.writeInt(statupdate.getRight().intValue());
//                    break;
                case AVAILABLESP: //0x8000
                    PacketHelper.addCharSP(mplew, chr);
                    break;
                case 经验: //0x10000
                case 金币: //0x40000
                    mplew.writeLong(statupdate.getRight());
                    break;
                case TODAYS_TRAITS:
                    mplew.writeZeroBytes(21);
                    break;
                case TRAIT_LIMIT:
                    mplew.write(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.writeInt(0);
                    mplew.write(0);
                    break;
                case BATTLE_EXP:
                    chr.getCharacterCard().connectData(mplew);
                    break;
                case BATTLE_RANK:
                    mplew.writeInt(chr.getStat().pvpExp);
                    mplew.write(chr.getStat().pvpRank);
                    mplew.writeInt(chr.getBattlePoints());
                    break;
                case BATTLE_POINTS:
                    mplew.write(5);
                    mplew.write(6);
                    break;
                default:
                    mplew.writeInt(statupdate.getRight().intValue());
                    break;
            }
        }
        mplew.write(chr != null ? chr.getHairBaseColor() : -1);
        mplew.write(chr != null ? chr.getHairMixedColor() : 0);
        mplew.write(chr != null ? chr.getHairProbColor() : 0);
        mplew.writeBool(updateMask == 0 || !itemReaction);
        if (updateMask == 0 || !itemReaction) {
            mplew.write(0);
        }
        mplew.writeBool(false);
        return mplew.getPacket();
    }

    /*
     * 武陵道场移动
     */
    public static byte[] instantMapWarp(byte portal) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        mplew.writeShort(0);
        mplew.writeInt(portal); // 6

        return mplew.getPacket();
    }

    /*
     * 火焰传动痕迹
     */
    public static byte[] flameMark() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FLAME_MARK.getValue());
        mplew.write(0x01);

        return mplew.getPacket();
    }

    /*
     * 角色移动到地图的另外1个坐标地点
     */
    public static byte[] instantMapWarp(int charId, Point pos) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CURRENT_MAP_WARP.getValue());
        mplew.write(0x00);
        mplew.write(0x02);
        mplew.writeInt(charId); //角色ID
        mplew.writePos(pos); //移动到的坐标

        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a portal.
     *
     * @param townId   The ID of the town the portal goes to.
     * @param targetId The ID of the target.
     * @param skillId
     * @param pos      Where to put the portal.
     * @return The portal spawn packet.
     */
    public static byte[] spawnPortal(int townId, int targetId, int skillId, Point pos) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_PORTAL.getValue());
        mplew.writeInt(townId);
        mplew.writeInt(targetId);
        if (townId != 999999999 && targetId != 999999999) {
            mplew.writeInt(skillId);
            mplew.writePos(pos);
        }

        return mplew.getPacket();
    }

    /**
     * Gets a packet to spawn a door.
     *
     * @param ownerId   传送门所有者的角色ID.
     * @param skillId   传送门的技能ID
     * @param pos       传送门的位置.
     * @param animation
     * @return The spawn door packet.
     */
    public static byte[] spawnDoor(int ownerId, int skillId, Point pos, boolean animation) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_DOOR.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(ownerId);
        mplew.writeInt(skillId);
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    /**
     * Gets a packet to remove a door.
     *
     * @param ownerId   The door's ID.
     * @param animation
     * @return The remove door packet.
     */
    public static byte[] removeDoor(int ownerId, boolean animation) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_DOOR.getValue());
        mplew.write(animation ? 0 : 1);
        mplew.writeInt(ownerId);

        return mplew.getPacket();
    }

    /*
     * 重置屏幕
     */
    public static byte[] resetScreen() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RESET_SCREEN.getValue());

        return mplew.getPacket();
    }

    /**
     * 发送地图错误信息到客户端 数据包的值大概如下: 0x01: 现在关闭了缩地门 0x02: 因某种原因，不能去那里 0x03:
     * 对不起，正在准备冒险岛ONLINE商城 - 弹出窗口 0x04: 因为有地气阻挡，无法接近。 0x05：无法进行瞬间移动的地区。 - 弹出窗口
     * 0x06：无法进行瞬间移动的地区。 0x07: 队员的等级差异太大，无法入场。 0x08: 只有组队成员才能入场的地图 0x09:
     * 只有队长可以申请入场。 0x0A: 请在队员全部聚齐后申请入场。 0x0B:
     * 你因不当行为，而遭游戏管理员禁止攻击，禁止获取经验值和金币，禁止交易，禁止丢弃道具，禁止开启个人商店与精灵商人，禁止组队，禁止使用拍卖系统，因此无法使用改功能。
     * 0x0C: 只有远征队员可以进入该地图。 0x0D: 所有副本人数已满。请使用其他频道。 0x0E: 远征队入场时间已结束，无法进入。
     *
     * @参数 type
     * @返回数据包后通知.
     */
    public static byte[] mapBlocked(int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAP_BLOCKED.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    /**
     * 发送错误信息到客户端 数据包的值大概如下: 0x01: 日前无法进入该频道，请稍后在尝试。 0x02: 现在无法进入冒险岛商城。请稍后在尝试。
     * 0x03: 只有在PVE服务器中可以使用。 0x04: 根据非活跃帐号保护政策，限制使用商城。必须登录官方网站进行身份认证后，才能正常使用。
     * 0x05: 现在无法操作。请稍后再试。- 对话框提示 0x06：日前无法进入，请玩家稍后在试.(电击象服务器目前不开放拍卖平台)
     * 0x07：日前拍卖系统拥塞中，请稍后再试！ 0x08:
     * 你因不当行为，而遭游戏管理员禁止攻击，禁止获取经验值和金币，禁止交易，禁止丢弃道具，禁止开启个人商店与精灵商人，禁止组队，禁止使用拍卖系统，因此无法使用改功能。
     *
     * @参数 type
     * @返回数据包后通知.
     */
    public static byte[] serverBlocked(int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVER_BLOCKED.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    /**
     * 发送错误信息到客户端 数据包的值大概如下: 0x01: 现在无法登录大乱斗服务器。请稍后重新尝试。 0x02: 从频道中获取队员信息失败。
     * 0x03: 只有队长可以进行。 0x04: 存在未复活的队员。 0x05：有队员在其他地方。 0x06：只有在大乱斗服务器中可以使用。 0x07:
     * 无 0x08: 不符合频道入场条件的队员无法移动。请重新确认。 - 对话框提示 0x09: 组队状态下无法入场的模式。请退出组队后重新尝试。 -
     * 对话框提示
     *
     * @参数 type
     * @返回数据包后通知.
     */
    public static byte[] partyBlocked(int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PARTY_BLOCKED.getValue());
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] serverMessage(String message) {
        return serverMessage(4, 0, message, false);
    }

    public static byte[] serverNotice(int type, String message) {
        return serverMessage(type, 0, message, false);
    }

    public static byte[] serverNotice(int type, int channel, String message) {
        return serverMessage(type, channel, message, false);
    }

    public static byte[] serverNotice(int type, int channel, String message, boolean smegaEar) {
        return serverMessage(type, channel, message, smegaEar);
    }

    private static byte[] serverMessage(int type, int channel, String message, boolean megaEar) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 0x00: [公告事项]
         * 0x01: 提示框
         * 0x02: 小喇叭
         * 0x03: 高品质喇叭
         * 0x04: 置顶公告
         * 0x05: 红色字
         * 0x06: 蓝色字
         * 0x08: 道具喇叭
         * 0x09: 带耳朵的道具喇叭
         * 0x0A: 特效喇叭
         * 0x0B: 黄字抽奖喇叭后面message发完后跟着的是物品ID
         * 0x0D: 5E 00 0D [5D DB] [60 EA 00 00] 任务头顶提示 T071修改 以前0x0C
         * 0x0E: 5E 00 0E [5D DB] 停止任务提示 T071修改 以前0x0D
         * 0x0E: 白色喇叭
         * 0x18: 蛋糕高级喇叭 V.117.1修改 以前0x16
         * 0x19: 馅饼高级喇叭 V.117.1修改 以前0x17
         * 0x1A: 心脏高级喇叭 V.117.1修改 以前0x18
         * 0x1B: 白骨高级喇叭 V.117.1修改 以前0x19
         */
        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        if (type == 0x18) {
            type = 0x28;
        } else if (type == 0x19) {
            type = 0x32;
        }
        mplew.write(type);
        if (type == 0x04) {
            mplew.write(1);
        }
        mplew.writeMapleAsciiString(message);
        switch (type) {
            case 0x03: //高品质喇叭
            case 0x09: //道具喇叭
            case 0x28: //蛋糕高级喇叭 +0x10
            case 0x32: //馅饼高级喇叭 +0x19
            case 0x1A: //心脏高级喇叭
            case 0x1B: //白骨高级喇叭
                mplew.write(channel - 1); // channel
                mplew.write(megaEar ? 1 : 0);
                break;
            case 0x10:
                mplew.writeInt(channel - 1);
                break;
            case 0x06:
                mplew.writeInt(channel >= 1000000 && channel < 6000000 ? channel : 0); //cash itemID, displayed in yellow by the {name}
                break;
        }
        return mplew.getPacket();
    }

    /*
     * 抽奖喇叭
     */
    public static byte[] getGachaponMega(String name, String message, Item item, int rareness, int channel) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x22); //V.117.1修改 以前0x20
        mplew.writeMapleAsciiString(name + message);
        mplew.writeInt(item.getItemId()); //道具ID
        mplew.writeInt(channel > 0 ? channel - 1 : -1); //频道 如果是在商城打开箱子  这个频道为 -1
        mplew.writeInt(rareness == 1 ? 0x00 : rareness == 3 ? 0x03 : 0x02); //颜色代码 0 为绿色 1 2 为红色 3为黄色
        mplew.write(0x01);
        PacketHelper.addItemInfo(mplew, item);

        return mplew.getPacket();
    }

    public static byte[] getAniMsg(int questID, int time) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0D);
        mplew.writeShort(questID);
        mplew.writeInt(time);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 缤纷喇叭
     */
    public static byte[] tripleSmega(List<String> message, boolean ear, int channel) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x0A);
        if (message.get(0) != null) {
            mplew.writeMapleAsciiString(message.get(0));
        }
        mplew.write(message.size());
        for (int i = 1; i < message.size(); i++) {
            if (message.get(i) != null) {
                mplew.writeMapleAsciiString(message.get(i));
            }
        }
        mplew.write(channel - 1);
        mplew.write(ear ? 1 : 0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /**
     * 情景喇叭
     */
    public static byte[] getAvatarMega(MapleCharacter chr, int channel, int itemId, List<String> message, boolean ear) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AVATAR_MEGA.getValue());
        mplew.writeInt(itemId);
        mplew.writeMapleAsciiString(chr.getName());
        for (int i = 0; i < 4; i++) {
            mplew.writeMapleAsciiString(message.get(i));
        }
        mplew.writeInt(channel - 1); // channel
        mplew.write(ear ? 1 : 0);
        PacketHelper.addCharLook(mplew, chr, true, chr.isZeroSecondLook());
        mplew.write(0);
        return mplew.getPacket();
    }

    /*
     * 道具喇叭
     */
    public static byte[] itemMegaphone(String msg, boolean whisper, int channel, Item item) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SERVERMESSAGE.getValue());
        mplew.write(0x08);
        mplew.writeMapleAsciiString(msg);
        mplew.write(channel - 1);
        mplew.write(whisper ? 1 : 0);
        PacketHelper.addItemPosition(mplew, item, true, false);
        if (item != null) {
            PacketHelper.addItemInfo(mplew, item);
        }
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getChatText(int cidfrom, String text, boolean whiteBG, int show) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHATTEXT.getValue());
        mplew.writeInt(cidfrom);
        mplew.write(whiteBG ? 1 : 0);
        mplew.writeMapleAsciiString(text);
        mplew.writeShort(show);
        mplew.write(0xFF);

        return mplew.getPacket();
    }

    public static byte[] GameMaster_Func(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GM_EFFECT.getValue());
        mplew.write(value);
        mplew.writeZeroBytes(17);

        return mplew.getPacket();
    }

    public static byte[] ShowAranCombo(int combo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ARAN_COMBO.getValue());
        mplew.writeInt(combo);

        return mplew.getPacket();
    }

    public static byte[] rechargeCombo(int value) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ARAN_COMBO_RECHARGE.getValue());
        mplew.writeInt(value);

        return mplew.getPacket();
    }

    public static byte[] getPacketFromHexString(String hex) {
        return HexTool.getByteArrayFromHexString(hex);
    }

    public static byte[] showGainExpFromMonster(int gain, boolean white, Map<MapleExpStat, Integer> expStats) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得经验.getType()); // 3 = 经验, 4 = SP, 5 = 人气, 7 = 家族GP ,8 = 贡献值 , 11 = 豆豆 12 = 精灵小助手
        mplew.write(white ? 1 : 0); // 1 = 白色 2 = 黄色
        mplew.writeInt(gain); // 多少经验
        mplew.write(0); // 不在聊天框显示
        long expMask = 0;
        for (MapleExpStat statupdate : expStats.keySet()) {
            expMask |= statupdate.getValue();
        }
        mplew.writeLong(expMask);
        Long value;
        for (Entry<MapleExpStat, Integer> statupdate : expStats.entrySet()) {
            value = statupdate.getKey().getValue();
            if (value >= 1) {
                if (value == MapleExpStat.活动组队经验.getValue()) {
                    mplew.write(statupdate.getValue().byteValue());
                } else {
                    mplew.writeInt(statupdate.getValue());
                }
            }
        }
        // 如果是 燃烧场地经验, 则需要附加一个int表示百分比
        //mplew.writeInt(0x64);

        return mplew.getPacket();
    }

    public static byte[] GainEXP_Monster(int gain, boolean white, int 组队经验, int 精灵祝福经验, int 道具佩戴经验, int 召回戒指经验, int Sidekick_Bonus_EXP, int 网吧特别经验, int 结婚奖励经验) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得经验.getType()); // 3 = 经验, 4 = SP, 5 = 人气, 7 = 家族GP ,8 = 贡献值 , 11 = 豆豆 12 = 精灵小助手
        mplew.write(white ? 1 : 0); // 1 = 白色 2 = 黄色
        mplew.writeInt(gain); // 多少经验
        mplew.write(0); // 不在聊天框显示

        mplew.writeInt(0); //活动奖励经验
        mplew.writeShort(0);
        mplew.writeInt(结婚奖励经验); //结婚奖励经验
        mplew.writeInt(召回戒指经验); //召回戒指组队经验
        mplew.write(0); //活动组队经验奖励倍数(i x 100)最大3倍
        mplew.writeInt(组队经验); //组队经验
        mplew.writeInt(道具佩戴经验); //道具佩戴奖励经验
        mplew.writeInt(网吧特别经验); //网吧特别经验
        mplew.writeInt(0); //彩虹周奖励经验
        mplew.writeInt(0); //欢乐奖励经验
        mplew.writeInt(0); //飞跃奖励经验
        mplew.writeInt(精灵祝福经验); //精灵祝福经验 (NULL)奖励经验
        mplew.writeInt(0); //增益奖励经验 V.100新增
        mplew.writeInt(0); //休息奖励经验 V.100新增
        mplew.writeInt(0); //物品奖励经验 V.100新增
        mplew.writeInt(0); //Cake vs Pie Bonus 经验
        mplew.writeInt(0); //Pvp Bonus 经验
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] GainEXP_Others(long gain, boolean inChat, boolean white) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得经验.getType()); // 3 = exp, 4 = fame, 5 = mesos, 6 = guildpoints
        mplew.write(white ? 1 : 0);
        mplew.writeInt((int) gain);
        mplew.write(inChat ? 1 : 0);
        mplew.writeZeroBytes(32); //V099是51 V100多了12个
        if (inChat) {
            mplew.write(0);
        }

        return mplew.getPacket();
    }

    /**
     * 发送封包到客户端 显示角色获得人气的信息
     *
     * @param gain 增加或者减少多少人气.
     * @return 封包数据.
     */
    public static byte[] getShowFameGain(int gain) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得人气.getType());
        mplew.writeInt(gain);

        return mplew.getPacket();
    }

    /**
     * 发送封包到客户端 显示角色获得金币的信息
     *
     * @param gain   增加或者减少多少金币.
     * @param inChat 是否在聊天框中显示
     * @return 封包数据.
     */
    public static byte[] showMesoGain(long gain, boolean inChat) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        if (!inChat) {
            mplew.write(0);
            mplew.write(1);
            mplew.write(0);
            mplew.writeLong(gain);
        } else {
            mplew.write(MapleStatusInfo.获得金币.getType());
            mplew.writeInt((int) gain);
            mplew.writeInt(-1);
        }

        return mplew.getPacket();
    }

    /**
     * 发送封包到客户端 显示角色获得装备的信息
     *
     * @param itemId   道具的ID.
     * @param quantity 增加或者减少多少.
     * @return 封包数据.
     */
    public static byte[] getShowItemGain(int itemId, short quantity) {
        return getShowItemGain(itemId, quantity, false);
    }

    public static byte[] getShowItemGain(int itemId, short quantity, boolean inChat) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        if (inChat) {
            return EffectPacket.getShowItemGain(Collections.singletonList(new Pair<>(itemId, (int) quantity)));
        }

        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.writeShort(MapleStatusInfo.获得道具.getType());
        mplew.writeInt(itemId);
        mplew.writeInt(quantity);

        return mplew.getPacket();
    }

    /**
     * 非商城道具到期
     *
     * @param itemId
     * @return
     */
    public static byte[] showItemExpired(int itemId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.非商城道具到期.getType());
        mplew.write(1); //有多少道具
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    /**
     * 技能到期提示
     *
     * @param update
     * @return
     */
    public static byte[] showSkillExpired(Map<Integer, SkillEntry> update) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.技能到期.getType());
        mplew.write(update.size()); //有多少技能
        for (Entry<Integer, SkillEntry> skills : update.entrySet()) {
            mplew.writeInt(skills.getKey());
        }

        return mplew.getPacket();
    }

    /**
     * 商城道具到期
     *
     * @param itemId
     * @return
     */
    public static byte[] showCashItemExpired(int itemId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.商城道具到期.getType());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    private static void addPlayerStats(final MapleCharacter chr, MaplePacketLittleEndianWriter mplew) {
        List<MapleBuffStat> buffStats = MapleBuffStat.getSpawnList(chr);
        for (int i = GameConstants.MAX_BUFFSTAT; i > 0; i--) {
            int value = 0;

            for (MapleBuffStat buffstat : buffStats) {
                if (buffstat.getPosition() == i) {
                    value += (buffstat.getValue(true, true));
                }
            }
            mplew.writeInt(value);
        }

        for (int i = 0; i < GameConstants.MAX_BUFFSTAT; i++) {
            for (MapleBuffStat buffstat : buffStats) {
                if (buffstat.getPosition() == i) {
                    buffstat.getSerializeSpawn().Serialize(mplew, chr);
                }
            }
        }
        mplew.writeInt(-1);
    }

    public static byte[] spawnPlayerMapobject(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_PLAYER.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(chr.getLevel());
        mplew.writeMapleAsciiString(chr.getName());
        MapleQuestStatus ultExplorer = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.ULT_EXPLORER));
        if (ultExplorer != null && ultExplorer.getCustomData() != null) {
            mplew.writeMapleAsciiString(ultExplorer.getCustomData());
        } else {
            mplew.writeMapleAsciiString("");
        }
        if (chr.getGuildId() <= 0) {
            mplew.writeLong(0);
        } else {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(chr.getGuildId());
            if (guild != null) {
                mplew.writeMapleAsciiString(guild.getName());
                mplew.writeShort(guild.getLogoBG());
                mplew.write(guild.getLogoBGColor());
                mplew.writeShort(guild.getLogo());
                mplew.write(guild.getLogoColor());
            } else {
                mplew.writeLong(0);
            }
        }
        mplew.write(chr.getAccountGender());
        mplew.write(chr.getGender());
        //称号
        mplew.writeInt(-1);//40
        mplew.writeInt(-1);
        mplew.writeInt(-1); // chr.getVip()

        Map<MapleBuffStat, Integer> statups = new HashMap<>(MapleBuffStat.getSpawnList());

        chr.getAllEffects().stream().filter(pair -> SkillConstants.isShowForgenBuff(pair.getLeft())).forEach(pair -> statups.put(pair.getLeft(), pair.getRight().value));

        BuffPacket.writeForeignBuff(mplew, chr, statups, true);
        //-----------------------结束---------------------------//
        chr.writeJobData(mplew);
        mplew.writeInt(0);
        mplew.writeInt(0);
        PacketHelper.addCharLook(mplew, chr, true, chr.isZeroSecondLook());
        if (JobConstants.is神之子(chr.getJob())) {
            PacketHelper.addCharLook(mplew, chr, true, !chr.isZeroSecondLook());
        }
        mplew.writeHexString("00 00 00 00 FF 00 00 00 00 FF");
        //开始加载玩家特殊效果信息
        mplew.writeLong(0); //未知
        int buffSrc = chr.getBuffSource(MapleBuffStat.骑兽技能);
        if (chr.getBuffedValue(MapleBuffStat.飞翔) != null && buffSrc > 0) {
            addMountId(mplew, chr, buffSrc);
            mplew.writeInt(chr.getId());
        } else {
            mplew.writeLong(0);
        }
        mplew.writeInt(0); //未知
        mplew.writeInt(Math.min(250, chr.getInventory(MapleInventoryType.CASH).countById(5110000))); //红心巧克力 max is like 100. but w/e
        mplew.writeInt(chr.getItemEffect()); //眼睛之类的特殊效果 5010073 - 人气美女 的特效
        mplew.writeInt(chr.getItemEffectType()); //幻影残像之类的特殊效果
        mplew.writeInt(chr.getTitleEffect()); //头顶上面的称号 3700135
        mplew.writeInt(chr.getDamageSkin()); //伤害皮肤效果
        mplew.writeInt(0);
        mplew.writeInt(chr.getItemEffect());
        mplew.writeLong(0);
        mplew.writeInt(chr.getStat().getEfftype());
        mplew.writeMapleAsciiString("");
        mplew.writeMapleAsciiString("");
        mplew.writeInt(-1); //加载玩家特殊效果信息结束
        mplew.writeShort(-1);//96
        mplew.writeInt(ItemConstants.getInventoryType(chr.getChair()) == MapleInventoryType.SETUP ? chr.getChair() : 0); // 椅子
        mplew.writeInt(chr.getChairType() & 15);
        mplew.writeInt(chr.getChairMeso());
        mplew.writeInt(chr.getChairMsg().isEmpty() ? 0 : chr.getChairType() >>> 4 & 15);
        if (!chr.getChairMsg().isEmpty()) {
            mplew.writeMapleAsciiString(chr.getChairMsg());
        }
        writeChairData(mplew, chr);
        mplew.write(0);
        mplew.writePos(chr.getTruePosition()); // 坐标
        mplew.write(chr.getStance()); // 姿势
        mplew.writeShort(chr.getFH()); //FH
        mplew.write(0);
        //开始加载玩家的宠物信息
        MaplePet[] pet = chr.getSpawnPets();
        for (int i = 0; i < 3; i++) {
            if (pet[i] != null && pet[i].getSummoned()) {
                PetPacket.addPetInfo(mplew, chr, pet[i], true);
            }
        }
        mplew.write(0); //加载玩家宠物结束
        mplew.write(0); //暂时注释,包数据错误导致第二玩家会看到第二只小白·官方没有只发送该包·不单独发送召唤包 PacketHelper.addLittleWhite(mplew, chr);
        //开始玩家坐骑信息
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getLevel() : 1); // 坐骑等级
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getExp() : 0); // 坐骑经验
        mplew.writeInt(chr.getMount() != null ? chr.getMount().getFatigue() : 0); // 坐骑疲劳
        //开始PlayerShop和MiniGame
        PacketHelper.addAnnounceBox(mplew, chr); //没有占1个 00
        //开始玩家小黑板信息
        mplew.write(chr.getChalkboard() != null && chr.getChalkboard().length() > 0 ? 1 : 0);
        if (chr.getChalkboard() != null && chr.getChalkboard().length() > 0) {
            mplew.writeMapleAsciiString(chr.getChalkboard());
        }
        //开始玩家戒指信息
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(true);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);
        mplew.write(0);
        int berserk = chr.getStat().Berserk ? 1 : 0;
        mplew.write(berserk);
        if ((berserk & 8) != 0) {
            mplew.writeInt(0);
        }
        if ((berserk & 16) != 0) {
            mplew.writeInt(0);
        }
        if ((berserk & 32) != 0) {
            mplew.writeInt(0);
        }

        mplew.writeInt(chr.getMount().getItemId());
        if (JobConstants.is狂龙战士(chr.getJob())) {
            String string2 = chr.getOneInfo(12860, "extern");
            mplew.writeInt(string2 == null ? 0 : Integer.parseInt(string2));
            string2 = chr.getOneInfo(12860, "inner");
            mplew.writeInt(string2 == null ? 0 : Integer.parseInt(string2));
            string2 = chr.getOneInfo(12860, "premium");
            mplew.write(string2 == null ? 0 : Integer.parseInt(string2));
        }
        mplew.writeInt(0);
        //不发送这个5个 FF 就会看到人物头上的称号一大堆
        mplew.writeInt(-1);
        mplew.write(-1);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0); // false
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(JobConstants.is超能力者(chr.getJob()) && chr.getSpecialStat().getPP() > 0 && chr.getBuffedValue(MapleBuffStat.心魂本能) != null ? 1 : 0); // 心魂本能特效
        mplew.write(1);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(chr.getSkillSkin().size());
        chr.getSkillSkin().forEach((key, value) -> {
            mplew.writeInt(key);
            mplew.writeInt(value);
        });
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(chr.getSummonedFamiliar() != null ? 1 : 0);
        if (chr.getSummonedFamiliar() != null) {
            MaplePacketLittleEndianWriter mplew2 = new MaplePacketLittleEndianWriter();
            mplew.writeHexString("24 31 7B 25");
            mplew.writeInt(1946157058);
            chr.getSummonedFamiliar().writePacket(mplew2, true);
            mplew.writeInt(10 + mplew2.getPacket().length);
            mplew.write(mplew2.getPacket());
            mplew.write(0);
            mplew.write(2);
            mplew.writeInt(2000);
            mplew.writeInt(2000);
        }
        if (chr.getMapId() / 100000 == 9600) {
            mplew.write(chr.inPVP() ? Integer.valueOf(chr.getEventInstance().getProperty("type")) : 0);
            if (chr.inPVP() && Integer.valueOf(chr.getEventInstance().getProperty("type")) > 0) {
                mplew.write(chr.getTeam() + 1);
            }
        } else if (chr.getMapId() / 100000 == 9800 || chr.getMapId() == 109080000) {
            mplew.write(chr.getCarnivalParty() != null ? chr.getCarnivalParty().getTeam() : 0);
        }
        return mplew.getPacket();
    }

    public static void addMountId(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int buffSrc) {
        Item c_mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -123);
        Item mount = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
        int mountId = GameConstants.getMountItem(buffSrc, chr);
        if (mountId == 0 && c_mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -124) != null) {
            mplew.writeInt(c_mount.getItemId());
        } else if (mountId == 0 && mount != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -19) != null) {
            mplew.writeInt(mount.getItemId());
        } else {
            mplew.writeInt(mountId);
        }
    }

    public static byte[] removePlayerFromMap(int chrId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_PLAYER_FROM_MAP.getValue());
        mplew.writeInt(chrId);

        return mplew.getPacket();
    }

    public static byte[] facialExpression(MapleCharacter from, int expression) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FACIAL_EXPRESSION.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(expression);
        mplew.writeInt(-1); //itemid of expression use
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] movePlayer(int chrId, List<LifeMovementFragment> moves, Point startPos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_PLAYER.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(0); //V.112新增
        mplew.writePos(startPos);
        mplew.writeInt(0); //未知随机数字 每个角色还不同
        PacketHelper.serializeMovementList(mplew, moves);

        return mplew.getPacket();
    }

    public static byte[] closeRangeAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo, boolean energy, boolean hasMoonBuff) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(energy ? SendPacketOpcode.ENERGY_ATTACK.getValue() : SendPacketOpcode.CLOSE_RANGE_ATTACK.getValue());
        addAttackBody(mplew, chr, skilllevel, itemId, attackInfo, hasMoonBuff, false);

        return mplew.getPacket();
    }

    public static byte[] rangedAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RANGED_ATTACK.getValue());
        addAttackBody(mplew, chr, skilllevel, itemId, attackInfo, false, true);
        if (JobConstants.is神之子(chr.getJob()) && attackInfo.skillId >= 100000000) {
            mplew.writeInt(attackInfo.position.x);
            mplew.writeInt(attackInfo.position.y);
        } else if (attackInfo.skillposition != null) {
            if (attackInfo.skillId == 风灵使者.季候风) {    // 季候风为全屏技能，不需要坐标信息
                mplew.writeLong(0);
            } else {
                mplew.writePos(attackInfo.skillposition); // 有些技能要发送技能的坐标信息
            }
        } else if (attackInfo.skillId == 林之灵.编队攻击) {
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] magicAttack(MapleCharacter chr, int skilllevel, int itemId, AttackInfo attackInfo) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAGIC_ATTACK.getValue());
        addAttackBody(mplew, chr, skilllevel, itemId, attackInfo, false, false);

        return mplew.getPacket();
    }

    public static void addAttackBody(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, int skilllevel, int itemId, AttackInfo ai, boolean hasMoonBuff, boolean b3) {
        int skillId = ai.skillId;
        mplew.writeInt(chr.getId()); //角色的ID
        mplew.write(0); //V.116.1新增 未知 好像箭矢炮盘 这个地方是1
        mplew.write(ai.numAttackedAndDamage);
        mplew.write(chr.getLevel()); //角色的等级
        mplew.write(skilllevel > 0 && skillId > 0 ? skilllevel : 0);
        if (skilllevel > 0 && skillId > 0) {
            mplew.writeInt(skillId);
        }
        if (JobConstants.is神之子(chr.getJob()) && skillId >= 100000000) {
            mplew.write(0); //这个地方未知 当为1的时候 后面还写个什么
        }
        if (b3 && nw(skillId)) {
            mplew.write(0);
        }
        if (skillId == 80001850 || skillId == 42001000 || skillId > 42001004 && skillId <= 42001006) {
            mplew.write(skillId);
        }
        if (hasMoonBuff) {
            mplew.write(0);
            mplew.write(0x02); //攻击怪物的次数加倍
            mplew.writeInt(11101022); //月光洒落 技能ID
            mplew.writeShort(20); //技能等级
        }
        mplew.write(b3 ? 8 : ny(skillId));
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(ai.display); //攻击的动作效果
        mplew.write(ai.direction); //攻击的方向
        mplew.write(-1); //攻击的速度
        mplew.writeShort(0);
        mplew.writeShort(0);
        mplew.write(0);
        mplew.write(0);
        mplew.write(ai.speed);
        mplew.write(chr.getStat().passive_mastery());
        mplew.writeInt(itemId);
        for (AttackPair oned : ai.allDamage) {
            if (oned.attack != null) {
                mplew.writeInt(oned.objectid);
                mplew.write(ai.ef);
                mplew.writeInt(0); //这个地方好像是颜色相关 但神之子切换状态的时候 这个地方为1 打出的攻击显示颜色和普通的不同
                if (skillId == 侠盗.金钱炸弹 || skillId == 阴阳师.朱玉的咒印 || skillId == 80011050) {
                    mplew.write(oned.attack.size());
                }
                for (Pair<Long, Boolean> eachd : oned.attack) {
                    if (eachd.right) {
                        mplew.writeLong(eachd.left + Long.MIN_VALUE);
                    } else {
                        mplew.writeLong(eachd.left);
                    }
                }
                if (oned.ksPsychicObjectId >= 0) {
                    mplew.writeInt(oned.ksPsychicObjectId);
                }
                if (skillId == 爆破手.浮空冲压) {
                    mplew.write(0);
                }
            }
        }

        switch (skillId) {
            case 2321001:
            case 2221052:
            case 11121052:
            case 12121054:
                mplew.writeInt(ai.charge);
        }

        switch (skillId) {
            case 2321001:
            case 2221052:
            case 11121052:
            case 12121054:
                mplew.writeInt(ai.charge);
                break;
            case 400041019:
            case 80002212:
            case 80001762:
            case 1011000102:
            case 101000202:
            case 4221052:
            case 爆莉萌天使.超级诺巴:
            case 80001431:
            case 100001283:
            case 战神.摩诃领域:
            case 13121052:
            case 14121052:
            case 15121052:
                mplew.writeInt(chr.getOldPosition().x);
                mplew.writeInt(chr.getOldPosition().y);
                break;
            case 13111020:
            case 112111016:
                mplew.writeShort(0);
                mplew.writeShort(0);
                break;
            case 400020009:
            case 400020010:
            case 400020011:
                mplew.writeShort(0);
                mplew.writeShort(0);
                break;
            case 51121009:
                mplew.write(0);
                break;
            case 112110003:
                mplew.writeInt(0);
                break;
            case 42100007:
                mplew.writeShort(0);
                mplew.write(0);
                break;
            case 21120019:
            case 爆破手.神圣狂暴打击:
            case 11121014:
            case 侠盗.暗影抨击:
            case 侠盗.暗影抨击_1:
            case 侠盗.暗影抨击_2:
            case 侠盗.暗影抨击_3:
                mplew.writeInt(0);
                mplew.writeInt(0);
                break;
            default:
                if (nv(skillId)) {
                    mplew.writeInt(0);
                    mplew.write(0);
                }
        }
    }

    public static boolean nv(int skillId) {
        switch (skillId) {
            case 400011004:
            case 400021009:
            case 400021010:
            case 400021011:
            case 400041017:
            case 400041018:
            case 冲锋队长.超人变形_1:
            case 神炮王.宇宙无敌火炮弹:
                return true;
        }
        return false;
    }

    public static boolean nw(int n2) {
        switch (n2) {
            case 1120017:
            case 1121008:
            case 1221009:
            case 1221011:
            case 2121006:
            case 2221006:
            case 3121015:
            case 3121020:
            case 3221017:
            case 4121013:
            case 4221007:
            case 4331000:
            case 4341009:
            case 5121007:
            case 5121016:
            case 5121017:
            case 5121020:
            case 5221016:
            case 5321000:
            case 5321012:
            case 5721007:
            case 5721064:
            case 11121103:
            case 11121203:
            case 12100028:
            case 12110028:
            case 12120010:
            case 12120011:
            case 13121002:
            case 14121002:
            case 15111022:
            case 15120003:
            case 15121002:
            case 21110020:
            case 21111021:
            case 21120006:
            case 21120022:
            case 22140023:
            case 25121005:
            case 31111005:
            case 31121001:
            case 32111003:
            case 35121016:
            case 37110002:
            case 爆破手.震波打击_1:
            case 41121001:
            case 41121018:
            case 41121021:
            case 42121000:
            case 51121007:
            case 51121008:
            case 112101009:
            case 112111004:
            case 112120000:
            case 112120001:
            case 112120002:
            case 112120003:
            case 112120050: {
                return true;
            }
        }
        return false;
    }

    public static int ny(int n2) {
        switch (n2) {
            case 2121054:
            case 31121005:
            case 42121054:
            case 爆莉萌天使.超级诺巴: {
                return 4;
            }
        }
        return 0;
    }

    /*
     * 特殊攻击效果显示
     */
    public static byte[] showSpecialAttack(int chrId, int tickCount, int pot_x, int pot_y, int display, int skillId, int skilllevel, boolean isLeft, int speed) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SPECIAL_ATTACK.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(tickCount);
        mplew.writeInt(pot_x);
        mplew.writeInt(pot_y);
        mplew.writeInt(display);
        mplew.writeInt(skillId);
        mplew.writeInt(0);//119
        mplew.writeInt(skilllevel);
        mplew.write(isLeft ? 1 : 0);
        mplew.writeInt(speed);

        return mplew.getPacket();
    }

    /*
     * 更新玩家外观
     */
    public static byte[] updateCharLook(MapleCharacter chr) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_LOOK.getValue());
        mplew.writeInt(chr.getId());
        mplew.write(1);
        PacketHelper.addCharLook(mplew, chr, false, chr.isZeroSecondLook());
        mplew.writeHexString("00 00 00 00 FF 00 00 00 00 FF");
        Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> rings = chr.getRings(false);
        addRingInfo(mplew, rings.getLeft());
        addRingInfo(mplew, rings.getMid());
        addMRingInfo(mplew, rings.getRight(), chr);
        mplew.writeInt(0);
        mplew.writeLong(0);

        return mplew.getPacket();
    }

    /*
     * 更新神之子切换后的外观
     */
    public static byte[] updateZeroLook(MapleCharacter chr) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_ZERO_LOOK.getValue());
        mplew.writeInt(chr.getId());
        PacketHelper.addCharLook(mplew, chr, false, chr.isZeroSecondLook());
        mplew.writeHexString("00 00 00 00 FF 00 00 00 00 FF");

        return mplew.getPacket();
    }

    public static byte[] removeZeroFromMap(int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ZERO_FROM_MAP.getValue());
        mplew.writeInt(chrId);

        return mplew.getPacket();
    }

    public static void addRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeInt(1);
            mplew.writeLong(ring.getRingId()); //自己的戒指ID
            mplew.writeLong(ring.getPartnerRingId()); //对方的戒指ID
            mplew.writeInt(ring.getItemId()); //戒指的道具ID
        }
    }

    /*
     * 结婚戒指
     */
    public static void addMRingInfo(MaplePacketLittleEndianWriter mplew, List<MapleRing> rings, MapleCharacter chr) {
        mplew.write(rings.size());
        for (MapleRing ring : rings) {
            mplew.writeInt(chr.getId());
            mplew.writeInt(ring.getPartnerChrId());
            mplew.writeInt(ring.getItemId());
        }
    }

    public static byte[] damagePlayer(int chrId, int type, int damage, int monsteridfrom, byte direction, int skillid, int pDMG, boolean pPhysical, int pID, byte pType, Point pPos, byte offset, int offset_d, int fake) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(chrId);
        mplew.write(type);
        mplew.writeInt(damage);
        mplew.writeShort(0);
        if (type >= -1) {
            mplew.writeInt(monsteridfrom);
            mplew.write(direction);
            mplew.writeInt(pID);
            mplew.writeInt(skillid);
            mplew.writeInt(pDMG);
            mplew.write(0); // ?
            if (pDMG > 0) {
                mplew.write(pPhysical ? 1 : 0);
                mplew.writeInt(pID);
                mplew.write(pType);
                mplew.writePos(pPos);
            }
        } else if (type == -8) {
            mplew.writeInt(1);
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(offset);
        if (offset == 1) {
            mplew.writeInt(offset_d);
        }
        mplew.writeInt(damage);
        if (damage <= 0 || fake > 0) { // supposed to be -1
            mplew.writeInt(fake);
        }
        return mplew.getPacket();
    }

    public static byte[] damagePlayer(int chrId, int type, int monsteridfrom, int damage) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAMAGE_PLAYER.getValue());
        mplew.writeInt(chrId);
        mplew.write(type);
        mplew.writeInt(damage);
        mplew.write(0);
        mplew.writeInt(monsteridfrom);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0);
        mplew.writeInt(damage);

        return mplew.getPacket();
    }

    /**
     * 更新任务
     *
     * @param quest
     * @return
     */
    public static byte[] updateQuest(MapleQuestStatus quest) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[2D 00] [01] [46 2D] [01] [03 00 30 35 38]
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.更新任务状态.getType());
        mplew.writeInt(quest.getQuest().getId());
        mplew.write(quest.getStatus());
        switch (quest.getStatus()) {
            case 0: //新任务？
                mplew.writeZeroBytes(10);
                break;
            case 1: //更新任务
                mplew.writeMapleAsciiString(quest.getCustomData() != null ? quest.getCustomData() : "");
                break;
            case 2: //完成任务
                mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
                break;
        }

        return mplew.getPacket();
    }

    /*
     * 更新任务信息
     */
    public static byte[] updateInfoQuest(int quest, String data) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(quest == 7 ? MapleStatusInfo.更新任务信息2.getType() : MapleStatusInfo.更新任务信息.getType());
        mplew.writeInt(quest);
        mplew.writeMapleAsciiString(data);

        return mplew.getPacket();
    }

    /**
     * 更新任务信息
     *
     * @param quest     任务ID
     * @param npc       任务NPC
     * @param nextquest 下一项任务
     * @param updata    是否更新数据
     * @return 返回任务更新数据结果数据包
     */
    public static byte[] updateQuestInfo(int quest, int npc, int nextquest, boolean updata) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x0B);
        mplew.writeInt(quest);
        mplew.writeInt(npc);
        mplew.writeInt(nextquest);
        mplew.writeBool(updata);

        return mplew.getPacket();
    }

    public static byte[] startQuestTimeLimit(int n2, int n3) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(7);
        mplew.writeShort(1);
        mplew.writeInt(n2);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }

    public static byte[] stopQuestTimeLimit(int n2) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_QUEST_INFO.getValue());
        mplew.write(0x13);
        mplew.writeInt(n2);

        return mplew.getPacket();
    }

    /*
     * 更新重新获取勋章任务信息
     */
    public static byte[] updateMedalQuestInfo(byte op, int itemId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REISSUE_MEDAL.getValue());
        /*
         * 0x00 = 领取成功
         * 0x03 = 已经有这个勋章
         */
        mplew.write(op);
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    public static byte[] charInfo(MapleCharacter chr, boolean isSelf) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_INFO.getValue());
        mplew.writeInt(chr.getId()); //角色ID
        mplew.write(0);
        mplew.write(chr.getLevel()); //等级
        chr.writeJobData(mplew);
        mplew.write(chr.getStat().pvpRank); //PK等级
        mplew.writeInt(chr.getFame()); //人气
        MapleRing mRing = chr.getMarriageRing(); //结婚戒指
        mplew.write(mRing != null ? 1 : 0);
        if (mRing != null) {
            mplew.writeInt(mRing.getRingId());
            mplew.writeInt(chr.getId());
            mplew.writeInt(mRing.getPartnerChrId());
            mplew.writeShort(0x03);
            mplew.writeInt(mRing.getItemId());
            mplew.writeInt(mRing.getItemId());
            mplew.writeAsciiString(chr.getName(), 13);
            mplew.writeAsciiString(mRing.getPartnerName(), 13);
        }
        //专业技能
        List<Integer> prof = chr.getProfessions();
        mplew.write(prof.size());
        for (int i : prof) {
            mplew.writeShort(i);
        }
        //家族和家族联盟
        if (chr.getGuildId() <= 0) {
            mplew.writeMapleAsciiString("-");
            mplew.writeMapleAsciiString("");
        } else {
            MapleGuild gs = WorldGuildService.getInstance().getGuild(chr.getGuildId());
            if (gs != null) {
                mplew.writeMapleAsciiString(gs.getName());
                if (gs.getAllianceId() > 0) {
                    MapleGuildAlliance allianceName = WorldAllianceService.getInstance().getAlliance(gs.getAllianceId());
                    if (allianceName != null) {
                        mplew.writeMapleAsciiString(allianceName.getName());
                    } else {
                        mplew.writeMapleAsciiString("");
                    }
                } else {
                    mplew.writeMapleAsciiString("");
                }
            } else {
                mplew.writeMapleAsciiString("-");
                mplew.writeMapleAsciiString("");
            }
        }
        mplew.write(0);
        mplew.write(isSelf ? 1 : 0); //是否显示宠物信息
        //宠物信息
        MaplePet[] pets = chr.getSpawnPets();
        mplew.write(chr.getSpawnPet(0) != null ? 1 : 0);
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null && pets[i].getSummoned()) { //已召唤的宠物
                mplew.write(1);
                mplew.writeInt(i);
                mplew.writeInt(pets[i].getPetItemId()); //宠物的道具ID
                mplew.writeMapleAsciiString(pets[i].getName()); //宠物名
                mplew.write(pets[i].getLevel()); //宠物等级
                mplew.writeShort(pets[i].getCloseness()); //宠物亲密度
                mplew.write(pets[i].getFullness()); //宠物饥饿度
                mplew.writeShort(pets[i].getFlags()); //宠物的状态
                Item inv = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (i == 0 ? -114 : (i == 1 ? -122 : -124)));
                mplew.writeInt(inv == null ? 0 : inv.getItemId());
                mplew.writeInt(-1); //T071新增
            }
        }
        mplew.write(0); // End of pet
        //坐骑信息
        /*
         * if (chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-18)) != null && chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) (-19)) != null) {
         * int itemid = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18).getItem();
         * MapleMount mount = chr.getMount();
         * boolean canwear = MapleItemInformationProvider.getInstance().getReqLevel(itemid) <= chr.getLevel();
         * mplew.write(canwear ? 1 : 0);
         * if (canwear) {
         * mplew.writeInt(mount.getLevel()); //等级
         * mplew.writeInt(mount.getExp()); //经验
         * mplew.writeInt(mount.getFatigue()); //疲劳度
         * }
         * } else {
         * mplew.write(0); //没有坐骑
         * }
         */
        //购物车信息
        int wishlistSize = chr.getWishlistSize();
        mplew.write(wishlistSize);
        if (wishlistSize > 0) {
            int[] wishlist = chr.getWishlist();
            for (int x = 0; x < wishlistSize; x++) {
                mplew.writeInt(wishlist[x]);
            }
        }
        //当前佩戴的勋章
        Item medal = chr.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -26);
        mplew.writeInt(medal == null ? 0 : medal.getItemId());
        //任务获得勋章列表
        List<Pair<Integer, Long>> medalQuests = chr.getCompletedMedals();
        mplew.writeShort(medalQuests.size());
        for (Pair<Integer, Long> x : medalQuests) {
            mplew.writeInt(x.left);
            mplew.writeLong(x.right);
        }

        mplew.write(1); // 显示当前伤害皮肤的信息
        mplew.writeInt(chr.getDamageSkin());
        mplew.writeInt(2431965);
        mplew.write(0);
        mplew.writeMapleAsciiString("");
        mplew.writeHexString("FF FF FF FF 00 00 00 00 01 00 00 00 00 00 00");

        //倾向系统信息
        for (MapleTraitType t : MapleTraitType.values()) {
            mplew.write(chr.getTrait(t).getLevel());
        }
        mplew.writeInt(0x00); //T071新增
        mplew.writeInt(0x00); //T071新增
        //椅子列表
        List<Integer> chairs = new ArrayList<>();
        for (Item i : chr.getInventory(MapleInventoryType.SETUP).newList()) {
            if (i.getItemId() / 10000 == 301 && !chairs.contains(i.getItemId())) {
                chairs.add(i.getItemId());
            }
        }
        mplew.writeInt(chairs.size());
        for (int i : chairs) {
            mplew.writeInt(i);
        }
        //勋章列表
        List<Integer> medals = new ArrayList<>();
        for (Item i : chr.getInventory(MapleInventoryType.EQUIP).list()) {
            if (i.getItemId() >= 1142000 && i.getItemId() < 1152000) {
                medals.add(i.getItemId());
            }
        }
        mplew.writeInt(medals.size());
        for (int i : medals) {
            mplew.writeInt(i);
        }

        //伤害皮肤
        writeDamageSkinData(mplew, chr);
        return mplew.getPacket();
    }

    public static byte[] updateMount(MapleCharacter chr, boolean levelup) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getMount().getLevel());
        mplew.writeInt(chr.getMount().getExp());
        mplew.writeInt(chr.getMount().getFatigue());
        mplew.write(levelup ? 1 : 0);

        return mplew.getPacket();
    }

//    public static byte[] mountInfo(MapleCharacter chr) {
//        if (ServerConstants.isShowPacket()) {
//            log.trace("调用");
//        }
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.UPDATE_MOUNT.getValue());
//        mplew.writeInt(chr.getId());
//        mplew.write(1);
//        mplew.writeInt(chr.getMount().getLevel());
//        mplew.writeInt(chr.getMount().getExp());
//        mplew.writeInt(chr.getMount().getFatigue());
//
//        return mplew.getPacket();
//    }

    public static byte[] updateSkill(int skillid, int level, int masterlevel, long expiration) {
        boolean isProfession = skillid == 92000000 || skillid == 92010000 || skillid == 92020000 || skillid == 92030000 || skillid == 92040000;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(isProfession ? 0 : 1);
        mplew.write(0x00);
        mplew.write(0x00); //未知 V.114 新增
        mplew.writeShort(1); //有多少个技能
        mplew.writeInt(skillid);
        mplew.writeInt(level);
        mplew.writeInt(masterlevel);
        PacketHelper.addExpirationTime(mplew, expiration);
        mplew.write(isProfession ? 4 : 3);

        return mplew.getPacket();
    }

    public static byte[] updateSkills(Map<Integer, SkillEntry> update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(0x01); //删除技能为 0x00 获得技能为 0x01
        mplew.write(0x00);
        mplew.write(0x00); //未知 V.114 新增
        mplew.writeShort(update.size());
        for (Entry<Integer, SkillEntry> skills : update.entrySet()) {
            mplew.writeInt(skills.getKey());
            mplew.writeInt(skills.getValue().skillevel);
            mplew.writeInt(skills.getValue().masterlevel);
            PacketHelper.addExpirationTime(mplew, skills.getValue().expiration);
        }
        mplew.write(4);

        return mplew.getPacket();
    }

    public static byte[] updatePetSkill(int skillid, int level, int masterlevel, long expiration) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILLS.getValue());
        mplew.write(0x00);
        mplew.write(0x01); //宠物是0x01
        mplew.write(0x00); //未知 V.114 新增
        mplew.writeShort(0x01); //技能的数量
        mplew.writeInt(skillid);
        mplew.writeInt(level == 0 ? -1 : level);
        mplew.writeInt(masterlevel);
        PacketHelper.addExpirationTime(mplew, expiration);
        mplew.write(0x04);

        return mplew.getPacket();
    }

    public static byte[] updateQuestMobKills(MapleQuestStatus status) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.更新任务状态.getType());
        mplew.writeShort(status.getQuest().getId());
        mplew.write(1);
        StringBuilder sb = new StringBuilder();
        for (int kills : status.getMobKills().values()) {
            sb.append(StringUtil.getLeftPaddedStr(String.valueOf(kills), '0', 3));
        }
        mplew.writeMapleAsciiString(sb.toString());

        return mplew.getPacket();
    }

    public static byte[] getShowQuestCompletion(int id) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_QUEST_COMPLETION.getValue());
        mplew.writeShort(id);

        return mplew.getPacket();
    }

    /*
     * 发送角色的键盘设置
     */
    public static byte[] getKeymap(MapleCharacter chr) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.KEYMAP.getValue());
        MapleKeyLayout keymap = chr.getKeyLayout();
        keymap.writeData(mplew, JobConstants.is林之灵(chr.getJob()) ? 5 : 1);

        return mplew.getPacket();
    }

    /*
     * 宠物自动加血
     */
    public static byte[] petAutoHP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_HP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    /*
     * 宠物自动加蓝
     */
    public static byte[] petAutoMP(int itemId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_MP.getValue());
        mplew.writeInt(itemId);

        return mplew.getPacket();
    }

    /*
     * 宠物自动加BUFF状态
     */
    public static byte[] petAutoBuff(int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_AUTO_BUFF.getValue());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    /*
     * 打开钓鱼记录NPC
     */
    public static byte[] openFishingStorage(int type, HiredFisher hf, MerchItemPackage pack, int playrId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * AF 00
         * 21
         * FF FF FF FF FF FF FF FF
         * 00
         * 00 00 00 00 00 00 00 00
         * 9E 4E 08 00
         */
        mplew.writeShort(SendPacketOpcode.FISHING_STORE.getValue());
        mplew.write(type);
        switch (type) {
            case 33: {
                mplew.writeInt(-1);
                break;
            }
            case 35: {
                mplew.writeInt(pack != null ? (int) pack.getMesos() : 0);
                mplew.writeLong(pack != null ? (long) ((int) pack.getExp()) : 0);
                writeHiredFisher(mplew, hf, pack, playrId);
                break;
            }
            case 28:
            case 30: {
                mplew.writeInt(hf.getObjectId());
                writeHiredFisher(mplew, hf, pack, playrId);
                break;
            }
            case 15: {
                mplew.writeInt(0);
                mplew.write(0);
                break;
            }
            case 22: {
                mplew.writeInt(hf.getOwnerId());
                mplew.write(1);
                break;
            }
            case 23: {
                mplew.writeInt(hf.getOwnerId());
                break;
            }
            case 43:
            case 45: {
                mplew.writeLong(DateUtil.getKoreanTimestamp(hf.getStartTime()));
                mplew.writeLong(DateUtil.getKoreanTimestamp(hf.getStopTime()));
            }
        }

        return mplew.getPacket();
    }

    public static void writeHiredFisher(MaplePacketLittleEndianWriter mplew, HiredFisher hf, MerchItemPackage itemPackage, int playrId) {
        long l2 = -1;
        mplew.writeLong(l2);
        mplew.writeInt(0);
        EnumMap<MapleInventoryType, ArrayList<Item>> items = new EnumMap<>(MapleInventoryType.class);
        items.put(MapleInventoryType.EQUIP, new ArrayList<>());
        items.put(MapleInventoryType.USE, new ArrayList<>());
        items.put(MapleInventoryType.SETUP, new ArrayList<>());
        items.put(MapleInventoryType.ETC, new ArrayList<>());
        items.put(MapleInventoryType.CASH, new ArrayList<>());
        if (hf != null) {
            hf.getItems().forEach(item -> items.get(ItemConstants.getInventoryType(item.getItem().getItemId())).add(item.getItem()));
        } else if (itemPackage != null) {
            itemPackage.getItems().forEach(item -> items.get(ItemConstants.getInventoryType(item.getItemId())).add(item));
        }
        items.forEach((key, value) -> {
            mplew.write(value.size());
            value.forEach(item -> PacketHelper.addItemInfo(mplew, item));
        });
        items.clear();
        mplew.writeInt(hf != null ? hf.getOwnerId() : playrId);
    }

    public static byte[] fairyPendantMessage(int position, int percent) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 6E 00
         * 0C 00 00 00 - 装备的位置
         * 00 00 00 00
         * 02 00 00 00 - 百分比经验
         */
        mplew.writeShort(SendPacketOpcode.FAIRY_PEND_MSG.getValue());
        mplew.writeInt(position); // 道具的位置
        mplew.writeInt(0); // 未知
        mplew.writeInt(percent); // 百分比经验提示

        return mplew.getPacket();
    }

    public static byte[] giveFameResponse(int mode, String charname, int newfame) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(charname);
        mplew.write(mode);
        mplew.writeInt(newfame);

        return mplew.getPacket();
    }

    public static byte[] giveFameErrorResponse(int status) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*
         * * 0: ok, use giveFameResponse<br> 1: the username is incorrectly
         * entered<br> 2: users under level 15 are unable to toggle with
         * fame.<br> 3: can't raise or drop fame anymore today.<br> 4: can't
         * raise or drop fame for this character for this month anymore.<br> 5:
         * received fame, use receiveFame()<br> 6: level of fame neither has
         * been raised nor dropped due to an unexpected error
         */
        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    public static byte[] receiveFame(int mode, String charnameFrom) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FAME_RESPONSE.getValue());
        mplew.write(5);
        mplew.writeMapleAsciiString(charnameFrom);
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] multiChat(String name, String chattext, int mode) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MULTICHAT.getValue());
        mplew.write(mode); // 0 好友聊天; 1 组队聊天; 2 家族聊天; 4 远征聊天
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(chattext);

        return mplew.getPacket();
    }

    public static byte[] getClock(int time) { // time in seconds
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(2); // clock type. if you send 3 here you have to send another byte (which does not matter at all) before the timestamp
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] getClockTime(int hour, int min, int sec) { // Current Time
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * Recv CLOCK [00BE] (6)
         * [BE 00] [01] [0F] [0B] [0F]
         * ?....
         */
        mplew.writeShort(SendPacketOpcode.CLOCK.getValue());
        mplew.write(1); //Clock-Type
        mplew.write(hour);
        mplew.write(min);
        mplew.write(sec);

        return mplew.getPacket();
    }

    /*
     * 终于试出来了 STOP_CLOCK = CLOCK + 6
     */
    public static byte[] stopClock() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.STOP_CLOCK.getValue());

        return mplew.getPacket();
    }

    /**
     * 召唤烟雾效果
     *
     * @param mist
     * @return
     */
    public static byte[] spawnMist(MapleMist mist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * Recv SPAWN_MIST [01A6] (45)
         * A6 01
         * 01 00 00 00 - ObjectId
         * 02 00 00 00
         * 9E 4E 08 00 - getOwnerId
         * 4E 68 40 00
         * 05 - SkillLevel
         * 03 00 - SkillDelay
         * C2 FE FF FF -318
         * 67 FD FF FF -665
         * B6 00 00 00 182
         * 93 FE FF FF -365
         * 00 00 00 00
         * BC FF -68
         * FD FD -515
         * ?........濶..Nh@....漫?g??...據?....?
         */
        mplew.writeShort(SendPacketOpcode.SPAWN_MIST.getValue());
        mplew.writeInt(mist.getObjectId());
        mplew.writeInt(mist.getMistType()); //2 = invincible, so put 1 for recovery aura
        mplew.writeInt(mist.getOwnerId());
        int skillid;
        if (mist.getMobSkill() == null) {
            skillid = mist.getSourceSkill().getId();
        } else {
            skillid = mist.getMobSkill().getSkillId();
        }
        mplew.writeInt(skillid);
        mplew.write(mist.getSkillLevel());
        mplew.writeShort(mist.getSkillDelay());
        mplew.writeRect(mist.getBox());
        mplew.writeInt(mist.getSubtype());
        mplew.writePos(mist.getOwnerPosition() != null ? mist.getOwnerPosition() : mist.getPosition());
        mplew.writeShort((skillid == 227) ? mist.getPosition().x : 0);
        mplew.writeShort(mist.getForce());
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        switch (skillid) {
            case 隐士.模糊领域:
            case 机械师.扭曲空间:
            case 豹弩游侠.集束箭:
            case 豹弩游侠.辅助打猎单元:
            case 豹弩游侠.辅助打猎单元_1:
            case 131001207:
            case 131001107:
            case 51120057:
            case 400021039:
                mplew.writeBool(!mist.isFacingLeft());
                break;
        }
        if (mist.getMobSkill() == null) {
            mplew.writeInt(mist.getSourceSkill().getDelay());
        } else {
            mplew.writeInt((int) mist.getMobSkill().getDuration());
        }
        return mplew.getPacket();
    }

    /**
     * 移除烟雾效果
     *
     * @param oid
     * @param eruption
     * @return
     */
    public static byte[] removeMist(int oid, boolean eruption) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.REMOVE_MIST.getValue());
        mplew.writeInt(oid);
        mplew.write(eruption ? 1 : 0);
        return mplew.getPacket();
    }

    public static byte[] spawnLove(int oid, int itemid, String name, String msg, Point pos, int ft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_LOVE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(msg);
        mplew.writeMapleAsciiString(name);
        mplew.writeShort(pos.x);
        mplew.writeShort(pos.y + ft);

        return mplew.getPacket();
    }

    public static byte[] removeLove(int oid, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_LOVE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] itemEffect(int chrId, int itemid, int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(itemid);
        mplew.writeInt(type);

        return mplew.getPacket();
    }

    public static byte[] showTitleEffect(int chrId, int itemid) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_TITLE_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] showUnkEffect(int chrId, int itemid) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_UNK_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    /*
     * 显示角色椅子
     */
    public static byte[] showChair(MapleCharacter player, int itemId, String text) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(player.getId());
        mplew.writeInt(itemId);
        mplew.writeInt(text.isEmpty() ? 0 : player.getChairType() >>> 4 & 0xF);
        if (!text.isEmpty()) {
            mplew.writeMapleAsciiString(text);
        }
        writeChairData(mplew, player);
        mplew.write(0);
        mplew.writeInt(player.getChairType() & 0xF);
        mplew.writeLong(0); //新增
        mplew.write(0);

        return mplew.getPacket();
    }

    public static void writeChairData(MaplePacketLittleEndianWriter mplew, MapleCharacter player) {
        String string;
        ArrayList<Integer> arrayList = new ArrayList<>();
        player.getInfoQuest(7266);
        for (int i2 = 0; i2 < 6 && (string = player.getOneInfo(7266, String.valueOf(i2))) != null && Integer.valueOf(string) > 0; ++i2) {
            arrayList.add(Integer.valueOf(string));
        }
        mplew.writeInt(arrayList.size());
        arrayList.forEach(mplew::writeInt);
    }

    public static byte[] addChairMeso(int cid, int value) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHAIR.getValue());
        mplew.writeInt(cid);
        mplew.writeInt(value);
        mplew.writeInt(1);

        return mplew.getPacket();
    }

    public static byte[] useTowerChairSetting() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USE_TOWERCHAIR_SETTING_RESULT.getValue());
        return mplew.getPacket();
    }

    /*
     * 取消椅子
     */
    public static byte[] cancelChair(int id) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_CHAIR.getValue());
        mplew.writeInt(id);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] spawnReactor(MapleReactor reactor) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        /*
         * Recv REACTOR_SPAWN [01B1] (24)
         * B1 01
         * FB 2A 00 00
         * 68 77 89 00
         * 00
         * 60 13 1D 00
         * 00
         * 06 00 44 47 54 65 73 74
         * ??..hw?.`......DGTest
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_SPAWN.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.writeInt(reactor.getReactorId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.write(reactor.getFacingDirection()); // stance
        mplew.writeMapleAsciiString(reactor.getName());

        return mplew.getPacket();
    }

    public static byte[] triggerReactor(MapleReactor reactor, int stance) {
        return triggerReactor(reactor, stance, 0, 0, 0);
    }

    public static byte[] triggerReactor(MapleReactor reactor, int stance, int n2, int cid, int n4) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_HIT.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getTruePosition());
        mplew.writeShort(stance);
        mplew.write(n4);
        mplew.write(n2);
        mplew.writeInt(cid);

        return mplew.getPacket();
    }

    public static byte[] destroyReactor(MapleReactor reactor) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REACTOR_DESTROY.getValue());
        mplew.writeInt(reactor.getObjectId());
        mplew.write(reactor.getState());
        mplew.writePos(reactor.getPosition());

        return mplew.getPacket();
    }

    public static byte[] musicChange(String song) {
        return environmentChange(song, 0x07);
    }

    public static byte[] showEffect(String effect) {
        return environmentChange(effect, 0x0C);
    }

    public static byte[] playSound(String sound) {
        return environmentChange(sound, 0x05);
    }

    public static byte[] environmentChange(String env, int mode) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(mode);
        mplew.writeMapleAsciiString(env);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] startMapEffect(String msg, int itemid, boolean active) {
        return startMapEffect(msg, 0, -1, active);
    }

    public static byte[] startMapEffect(String msg, int itemid, int effectType, boolean active) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * B9 00
         * 00
         * 18 20 4E 00
         * 2C 00 CC F4 D5 BD CE E4 C1 EA B5 C0 B3 A1 B5 C4 BC D2 BB EF A3 AC CE D2 D2 BB B6 A8 BB E1 C8 C3 CB FB 28 CB FD 29 BA F3 BB DA A3 A1 A3 A1
         */
        mplew.writeShort(SendPacketOpcode.MAP_EFFECT.getValue());
        mplew.write(active ? 0 : 1);
        mplew.writeInt(itemid);
        if (effectType > 0) {
            mplew.writeInt(effectType);
        }
        if (active) {
            mplew.writeMapleAsciiString(msg);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] removeMapEffect() {
        return startMapEffect(null, 0, -1, false);
    }

    /*
     * 显示占卜结果
     */
    public static byte[] showPredictCard(String name, String otherName, int love, int cardId, int commentId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_PREDICT_CARD.getValue());
        mplew.writeMapleAsciiString(name);
        mplew.writeMapleAsciiString(otherName);
        mplew.writeInt(love);
        mplew.writeInt(cardId);
        mplew.writeInt(commentId);

        return mplew.getPacket();
    }

    public static byte[] skillEffect(int fromId, int skillId, byte level, byte display, byte direction, byte speed, Point position) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_EFFECT.getValue());
        mplew.writeInt(fromId); //角色ID
        mplew.writeInt(skillId); //技能ID
        mplew.write(level); //技能等级
        mplew.write(display); //技能效果
        mplew.write(direction); //攻击方向
        mplew.write(speed); //速度
        if (position != null) {
            mplew.writePos(position); //有些技能这个地方要写个坐标信息
        }

        return mplew.getPacket();
    }

    public static byte[] skillCancel(MapleCharacter from, int skillId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANCEL_SKILL_EFFECT.getValue());
        mplew.writeInt(from.getId());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    public static byte[] sendHint(String hint, int width, int time) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        if (width < 1) {
            width = hint.length() * 10;
            if (width < 40) {
                width = 40;
            }
        }
        if (time < 5) {
            time = 5;
        }
        mplew.writeShort(SendPacketOpcode.PLAYER_HINT.getValue());
        mplew.writeMapleAsciiString(hint);
        mplew.writeShort(width);
        mplew.writeShort(time);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());

        return mplew.getPacket();
    }

    public static byte[] showEquipEffect(int team) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_EQUIP_EFFECT.getValue());
        mplew.writeShort(team);

        return mplew.getPacket();
    }

    public static byte[] skillCooldown(int skillId, int time) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.COOLDOWN.getValue());
        mplew.writeInt(1);
        mplew.writeInt(skillId);
        mplew.writeInt(time * 1000);

        return mplew.getPacket();
    }

    public static byte[] useSkillBook(MapleCharacter chr, int skillid, int maxlevel, boolean canuse, boolean success) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USE_SKILL_BOOK.getValue());
        mplew.write(0); //?
        mplew.writeInt(chr.getId());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.writeInt(maxlevel);
        mplew.write(canuse ? 1 : 0);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getMacros(SkillMacro[] macros) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_MACRO.getValue());
        int count = 0;
        for (int i = 0; i < 5; i++) {
            if (macros[i] != null) {
                count++;
            }
        }
        mplew.write(count); // number of macros
        for (int i = 0; i < 5; i++) {
            SkillMacro macro = macros[i];
            if (macro != null) {
                mplew.writeMapleAsciiString(macro.getName());
                mplew.write(macro.getShout());
                mplew.writeInt(macro.getSkill1());
                mplew.writeInt(macro.getSkill2());
                mplew.writeInt(macro.getSkill3());
            }
        }

        return mplew.getPacket();
    }

    public static byte[] boatPacket(int effect) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        mplew.writeShort(SendPacketOpcode.BOAT_EFFECT.getValue());
        mplew.writeShort(effect); // 0A 04 balrog
        //this packet had 3: boat leaves

        return mplew.getPacket();
    }

    public static byte[] boatEffect(int effect) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // 1034: balrog boat comes, 1548: boat comes, 3: boat leaves
        mplew.writeShort(SendPacketOpcode.BOAT_EFF.getValue());
        mplew.writeShort(effect); // 0A 04 balrog
        //this packet had the other ones o.o

        return mplew.getPacket();
    }

    public static byte[] removeItemFromDuey(boolean remove, int Package) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(0x18);
        mplew.writeInt(Package);
        mplew.write(remove ? 3 : 4);

        return mplew.getPacket();
    }

    public static byte[] sendDuey(byte operation, List<MapleDueyActions> packages) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DUEY.getValue());
        mplew.write(operation);
        switch (operation) {
            case 0x09: { // Request 13 Digit AS
                mplew.write(1);
                // 0xFF = error
                break;
            }
            case 0x0A: { // 打开送货员
                mplew.write(0);
                mplew.write(packages.size());
                for (MapleDueyActions dp : packages) {
                    mplew.writeInt(dp.getPackageId());
                    mplew.writeAsciiString(dp.getSender(), 13);
                    mplew.writeInt(dp.getMesos());
                    mplew.writeLong(PacketHelper.getTime(dp.getSentTime()));
                    mplew.writeZeroBytes(202);
                    if (dp.getItem() != null) {
                        mplew.write(1);
                        PacketHelper.addItemInfo(mplew, dp.getItem());
                    } else {
                        mplew.write(0);
                    }
                }
                mplew.write(0);
                break;
            }
        }
        return mplew.getPacket();
    }

    public static byte[] enableTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENABLE_TV.getValue());
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] removeTV() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_TV.getValue());

        return mplew.getPacket();
    }

    public static byte[] sendTV(MapleCharacter chr, List<String> messages, int type, MapleCharacter partner, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.START_TV.getValue());
        mplew.write(partner != null ? 2 : 1);
        mplew.write(type); // type   Heart = 2  Star = 1  Normal = 0
        PacketHelper.addCharLook(mplew, chr, false, chr.isZeroSecondLook());
        mplew.writeMapleAsciiString(chr.getName());

        if (partner != null) {
            mplew.writeMapleAsciiString(partner.getName());
        } else {
            mplew.writeShort(0);
        }
        for (int i = 0; i < messages.size(); i++) {
            if (i == 4 && messages.get(4).length() > 15) {
                mplew.writeMapleAsciiString(messages.get(4).substring(0, 15)); // hmm ?
            } else {
                mplew.writeMapleAsciiString(messages.get(i));
            }
        }
        mplew.writeInt(delay); // time limit shit lol 'Your thing still start in blah blah seconds'
        if (partner != null) {
            PacketHelper.addCharLook(mplew, partner, false, partner.isZeroSecondLook());
        }

        return mplew.getPacket();
    }

    public static byte[] showQuestMsg(final String msg) {
        return serverNotice(5, msg);
    }

    public static byte[] Mulung_Pts(int recv, int total) {
        return showQuestMsg("获得了 " + recv + " 点修炼点数。总修炼点数为 " + total + " 点。");
    }

    public static byte[] showOXQuiz(int questionSet, int questionId, boolean askQuestion) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OX_QUIZ.getValue());
        mplew.write(askQuestion ? 1 : 0);
        mplew.write(questionSet);
        mplew.writeShort(questionId);

        return mplew.getPacket();
    }

    public static byte[] leftKnockBack() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LEFT_KNOCK_BACK.getValue());

        return mplew.getPacket();
    }

    public static byte[] rollSnowball(int type, MapleSnowball.MapleSnowballs ball1, MapleSnowball.MapleSnowballs ball2) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ROLL_SNOWBALL.getValue());
        mplew.write(type); // 0 = normal, 1 = rolls from start to end, 2 = down disappear, 3 = up disappear, 4 = move
        mplew.writeInt(ball1 == null ? 0 : (ball1.getSnowmanHP() / 75));
        mplew.writeInt(ball2 == null ? 0 : (ball2.getSnowmanHP() / 75));
        mplew.writeShort(ball1 == null ? 0 : ball1.getPosition());
        mplew.write(0);
        mplew.writeShort(ball2 == null ? 0 : ball2.getPosition());
        mplew.writeZeroBytes(11);

        return mplew.getPacket();
    }

    public static byte[] enterSnowBall() {
        return rollSnowball(0, null, null);
    }

    public static byte[] hitSnowBall(int team, int damage, int distance, int delay) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HIT_SNOWBALL.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeShort(damage);
        mplew.write(distance);
        mplew.write(delay);

        return mplew.getPacket();
    }

    public static byte[] snowballMessage(int team, int message) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SNOWBALL_MESSAGE.getValue());
        mplew.write(team);// 0 is down, 1 is up
        mplew.writeInt(message);

        return mplew.getPacket();
    }

    public static byte[] finishedSort(int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        /*
         * [41 00] [01] [01]
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FINISH_SORT.getValue());
        mplew.write(1);
        mplew.write(type);

        return mplew.getPacket();
    }

    // 00 01 00 00 00 00
    public static byte[] coconutScore(int[] coconutscore) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.COCONUT_SCORE.getValue());
        mplew.writeShort(coconutscore[0]);
        mplew.writeShort(coconutscore[1]);

        return mplew.getPacket();
    }

    public static byte[] hitCoconut(boolean spawn, int id, int type) {
        // FF 00 00 00 00 00 00
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HIT_COCONUT.getValue());
        if (spawn) {
            mplew.write(0);
            mplew.writeInt(0x80);
        } else {
            mplew.writeInt(id);
            mplew.write(type); // What action to do for the coconut.
        }

        return mplew.getPacket();
    }

    public static byte[] finishedGather(int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        /*
         * [40 00] [01] [01]
         */
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FINISH_GATHER.getValue());
        mplew.write(1);
        mplew.write(type);

        return mplew.getPacket();
    }

    public static byte[] yellowChat(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        //mplew.writeShort(SendPacketOpcode.YELLOW_CHAT.getValue());
        mplew.writeShort(SendPacketOpcode.SPOUSE_MESSAGE.getValue()); //没有找到封包使用就用这个
        mplew.writeShort(0x07);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] getPeanutResult(int itemId, short quantity, int ourItem, int ourSlot, int itemId2, short quantity2) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PIGMI_REWARD.getValue());
        mplew.writeInt(itemId);
        mplew.writeShort(quantity);
        mplew.writeInt(ourItem);
        mplew.writeInt(ourSlot);
        mplew.writeInt(itemId2);
        mplew.writeInt(quantity2);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    /*
     * 发送玩家升级信息和 学院 家族 相关
     */
    public static byte[] sendLevelup(boolean family, int level, String name) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[80 00] [01] [15 00 00 00] [09 00 53 48 5A 42 47 BF CE B7 A8]
        mplew.writeShort(SendPacketOpcode.LEVEL_UPDATE.getValue());
        mplew.write(family ? 1 : 2);
        mplew.writeInt(level);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendMarriage(boolean family, String name) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MARRIAGE_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    public static byte[] sendJobup(boolean family, int jobid, String name) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.JOB_UPDATE.getValue());
        mplew.write(family ? 1 : 0);
        mplew.writeInt(jobid); //or is this a short
        mplew.writeMapleAsciiString(name);

        return mplew.getPacket();
    }

    /*
     * 显示龙飞行效果
     */
    public static byte[] showDragonFly(int chrId, int type, int mountId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_DRAGON_FLY.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(type);
        if (type == 0) {
            mplew.writeInt(mountId);
        }

        return mplew.getPacket();
    }

    public static byte[] temporaryStats_Aran() {
        Map<MapleStat.Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);
        stats.put(MapleStat.Temp.力量, 999);
        stats.put(MapleStat.Temp.敏捷, 999);
        stats.put(MapleStat.Temp.智力, 999);
        stats.put(MapleStat.Temp.运气, 999);
        stats.put(MapleStat.Temp.物攻, 255);
        stats.put(MapleStat.Temp.命中, 999);
        stats.put(MapleStat.Temp.回避, 999);
        stats.put(MapleStat.Temp.速度, 140);
        stats.put(MapleStat.Temp.跳跃, 120);
        return temporaryStats(stats);
    }

    public static byte[] temporaryStats_Balrog(MapleCharacter chr) {
        Map<MapleStat.Temp, Integer> stats = new EnumMap<>(MapleStat.Temp.class);
        int offset = 1 + (chr.getLevel() - 90) / 20;
        //every 20 levels above 90, +1

        stats.put(MapleStat.Temp.力量, chr.getStat().getTotalStr() / offset);
        stats.put(MapleStat.Temp.敏捷, chr.getStat().getTotalDex() / offset);
        stats.put(MapleStat.Temp.智力, chr.getStat().getTotalInt() / offset);
        stats.put(MapleStat.Temp.运气, chr.getStat().getTotalLuk() / offset);
        stats.put(MapleStat.Temp.物攻, chr.getStat().getTotalWatk() / offset);
        stats.put(MapleStat.Temp.物防, chr.getStat().getTotalMagic() / offset);
        return temporaryStats(stats);
    }

    public static byte[] temporaryStats(Map<MapleStat.Temp, Integer> mystats) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TEMP_STATS.getValue());
        //str 0x1, dex 0x2, int 0x4, luk 0x8
        //level 0x10 = 255
        //0x100 = 999
        //0x200 = 999
        //0x400 = 120
        //0x800 = 140
        int updateMask = 0;
        for (MapleStat.Temp statupdate : mystats.keySet()) {
            updateMask |= statupdate.getValue();
        }
        mplew.writeInt(updateMask);
        Integer value;
        for (final Entry<MapleStat.Temp, Integer> statupdate : mystats.entrySet()) {
            value = statupdate.getKey().getValue();
            if (value >= 1) {
                if (value <= 0x200) { //level 0x10 - is this really short or some other? (FF 00)
                    mplew.writeShort(statupdate.getValue().shortValue());
                } else {
                    mplew.write(statupdate.getValue().byteValue());
                }
            }
        }

        return mplew.getPacket();
    }

    public static byte[] temporaryStats_Reset() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TEMP_STATS_RESET.getValue());

        return mplew.getPacket();
    }

    /*
     * 传授技能后显示的窗口
     */
    public static byte[] sendLinkSkillWindow(int skillId) {
        return UIPacket.sendUIWindow(0x03, skillId);
    }

    /*
     * 组队搜索窗口
     */
    public static byte[] sendPartyWindow(int npc) {
        return UIPacket.sendUIWindow(0x15, npc);
    }

    /*
     * 道具修理窗口
     */
    public static byte[] sendRepairWindow(int npc) {
        return UIPacket.sendUIWindow(0x21, npc);
    }

    /*
     * 专业技术窗口
     */
    public static byte[] sendProfessionWindow(int npc) {
        return UIPacket.sendUIWindow(0x2A, npc);
    }

    public static byte[] sendRedLeaf(int points, boolean viewonly) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(10);

        mplew.writeShort(SendPacketOpcode.REPAIR_WINDOW.getValue());
        mplew.writeInt(0x73);
        mplew.writeInt(points);
        mplew.write(viewonly ? 1 : 0); //只是查看，完成按钮被禁用

        return mplew.getPacket();
    }

    public static byte[] sendPVPMaps() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PVP_INFO.getValue());
        mplew.write(1); //max amount of players
        mplew.writeInt(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(1); //how many peoples in each map
        }
        mplew.writeLong(0);
        for (int i = 0; i < 3; i++) {
            mplew.writeInt(1);
        }
        mplew.writeLong(0);
        for (int i = 0; i < 4; i++) {
            mplew.writeInt(1);
        }
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(1);
        }
        mplew.writeInt(0x0E);
        mplew.writeShort(0x64); ////PVP 1.5 EVENT!
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] sendPyramidUpdate(int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PYRAMID_UPDATE.getValue());
        mplew.writeInt(amount); //1-132 ?

        return mplew.getPacket();
    }

    public static byte[] sendPyramidResult(byte rank, int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PYRAMID_RESULT.getValue());
        mplew.write(rank);
        mplew.writeInt(amount); //1-132 ?

        return mplew.getPacket();
    }

    //show_status_info - 01 53 1E 01
    //10/08/14/19/11
    //update_quest_info - 08 53 1E 00 00 00 00 00 00 00 00
    //show_status_info - 01 51 1E 01 01 00 30
    //update_quest_info - 08 51 1E 00 00 00 00 00 00 00 00
    public static byte[] sendPyramidEnergy(String type, String amount) {
        return sendString(1, type, amount);
    }

    public static byte[] sendString(int type, String object, String amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        switch (type) {
            case 1:
                mplew.writeShort(SendPacketOpcode.ENERGY.getValue()); //武林道场会出现
                break;
            case 2:
                mplew.writeShort(SendPacketOpcode.GHOST_POINT.getValue()); //金字塔会出现
                break;
            case 3:
                mplew.writeShort(SendPacketOpcode.GHOST_STATUS.getValue()); //金字塔会出现
                break;
        }
        mplew.writeMapleAsciiString(object); //massacre_hit, massacre_cool, massacre_miss, massacre_party, massacre_laststage, massacre_skill
        mplew.writeMapleAsciiString(amount);

        return mplew.getPacket();
    }

    public static byte[] sendGhostPoint(String type, String amount) {
        return sendString(2, type, amount); //PRaid_Point (0-1500???)
    }

    public static byte[] sendGhostStatus(String type, String amount) {
        return sendString(3, type, amount); //Red_Stage(1-5), Blue_Stage, blueTeamDamage, redTeamDamage
    }

    public static byte[] MulungEnergy(int energy) {
        return sendPyramidEnergy("energy", String.valueOf(energy));
    }

//    public static byte[] getPollQuestion() {
//        if (ServerConfig.DEBUG_MODE) {
//            log.trace("调用");
//        }
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.GAME_POLL_QUESTION.getValue());
//        mplew.writeInt(1);
//        mplew.writeInt(14);
//        mplew.writeMapleAsciiString(ServerConstants.Poll_Question);
//        mplew.writeInt(ServerConstants.Poll_Answers.length); // pollcount
//        for (byte i = 0; i < ServerConstants.Poll_Answers.length; i++) {
//            mplew.writeMapleAsciiString(ServerConstants.Poll_Answers[i]);
//        }
//
//        return mplew.getPacket();
//    }
//
//    public static byte[] getPollReply(String message) {
//        if (ServerConfig.DEBUG_MODE) {
//            log.trace("调用");
//        }
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.GAME_POLL_REPLY.getValue());
//        mplew.writeMapleAsciiString(message);
//
//        return mplew.getPacket();
//    }

    public static byte[] showEventInstructions() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GMEVENT_INSTRUCTIONS.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 打开商店搜索器 -- OK
     */
    public static byte[] getOwlOpen() { //best items! hardcoded
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(0x0A); //V.112修改 以前是0x09
        List<Integer> owlItems = RankingWorker.getItemSearch();
        mplew.write(owlItems.size());
        for (int i : owlItems) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();
    }

    /*
     * 搜索的结果 - OK
     */
    public static byte[] getOwlSearched(int itemSearch, List<HiredMerchant> hms) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OWL_OF_MINERVA.getValue());
        mplew.write(0x09); //V.112修改 以前是0x08
        mplew.writeInt(0);
        mplew.writeShort(0); //V.112新增 未知
        mplew.writeInt(itemSearch); //要搜索的道具ID
        int size = 0;
        for (HiredMerchant hm : hms) {
            size += hm.searchItem(itemSearch).size();
        }
        mplew.writeInt(size);
        for (HiredMerchant hm : hms) {
            List<MaplePlayerShopItem> items = hm.searchItem(itemSearch);
            for (MaplePlayerShopItem item : items) {
                mplew.writeMapleAsciiString(hm.getOwnerName());
                mplew.writeInt(hm.getMap().getId());
                mplew.writeMapleAsciiString(hm.getDescription());
                mplew.writeInt(item.item.getQuantity()); //道具数量
                mplew.writeInt(item.bundles); //道具份数
                mplew.writeLong(item.price); //道具价格
                switch (InventoryHandler.OWL_ID) {
                    case 0:
                        mplew.writeInt(hm.getOwnerId()); //拥有者ID
                        break;
                    case 1:
                        mplew.writeInt(hm.getStoreId()); //保管的ID?
                        break;
                    default:
                        mplew.writeInt(hm.getObjectId()); //雇佣商人工具ID？
                        break;
                }
                mplew.write(hm.getChannel() - 1); //雇佣商店在几频道
                mplew.write(ItemConstants.getInventoryType(itemSearch).getType());
                if (ItemConstants.getInventoryType(itemSearch) == MapleInventoryType.EQUIP) {
                    PacketHelper.addItemInfo(mplew, item.item);
                }
            }
        }
        return mplew.getPacket();
    }

    public static byte[] getRPSMode(byte mode, int mesos, int selection, int answer) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RPS_GAME.getValue());
        mplew.write(mode);
        switch (mode) {
            case 6: { //not enough mesos
                if (mesos != -1) {
                    mplew.writeInt(mesos);
                }
                break;
            }
            case 8: { //open (npc)
                mplew.writeInt(9000019);
                break;
            }
            case 11: { //selection vs answer
                mplew.write(selection);
                mplew.write(answer); // FF = lose, or if selection = answer then lose ???
                break;
            }
        }
        return mplew.getPacket();
    }

    /*
     * 玩家请求跟随
     */
    public static byte[] followRequest(int chrid) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_REQUEST.getValue());
        mplew.writeInt(chrid);

        return mplew.getPacket();
    }

    /*
     * 跟随状态
     */
    public static byte[] followEffect(int initiator, int replier, Point toMap) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_EFFECT.getValue());
        mplew.writeInt(initiator);
        mplew.writeInt(replier);
        if (replier == 0) { //cancel
            mplew.write(toMap == null ? 0 : 1); //1 -> x (int) y (int) to change map
            if (toMap != null) {
                mplew.writeInt(toMap.x);
                mplew.writeInt(toMap.y);
            }
        }
        return mplew.getPacket();
    }

    /*
     * 返回跟随的信息
     */
    public static byte[] getFollowMsg(int opcode) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_MSG.getValue());
        /*
         * 0x01 = 当前位置无法接受跟随请求
         * 0x05 = 拒绝跟随请求
         */
        mplew.writeLong(opcode);

        return mplew.getPacket();
    }

    /*
     * 跟随移动
     */
    public static byte[] moveFollow(Point otherStart, Point myStart, Point otherEnd, List<LifeMovementFragment> moves) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FOLLOW_MOVE.getValue());
        mplew.writeInt(0); //V.112新增
        mplew.writePos(otherStart);
        mplew.writePos(myStart);
        PacketHelper.serializeMovementList(mplew, moves);
        mplew.write(0x11); //what? could relate to movePlayer
        for (int i = 0; i < 8; i++) {
            mplew.write(0); //?? sometimes 0x44 sometimes 0x88 sometimes 0x4.. etc.. buffstat or what
        }
        mplew.write(0); //?
        mplew.writePos(otherEnd);
        mplew.writePos(otherStart);

        return mplew.getPacket();
    }

    /*
     * 跟随断开的信息
     */
    public static byte[] getFollowMessage(String msg) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        mplew.writeShort(0x0B); //?
        mplew.writeMapleAsciiString(msg); //white in gms, but msea just makes it pink.. waste

        return mplew.getPacket();
    }

    public static byte[] getMovingPlatforms(MapleMap map) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MOVE_PLATFORM.getValue());
        mplew.writeInt(map.getPlatforms().size());
        for (MaplePlatform mp : map.getPlatforms()) {
            mplew.writeMapleAsciiString(mp.name);
            mplew.writeInt(mp.start);
            mplew.writeInt(mp.SN.size());
            for (int x = 0; x < mp.SN.size(); x++) {
                mplew.writeInt(mp.SN.get(x));
            }
            mplew.writeInt(mp.speed);
            mplew.writeInt(mp.x1);
            mplew.writeInt(mp.x2);
            mplew.writeInt(mp.y1);
            mplew.writeInt(mp.y2);
            mplew.writeInt(mp.x1);//?
            mplew.writeInt(mp.y1);
            mplew.writeShort(mp.r);
        }
        return mplew.getPacket();
    }

    /**
     * @param type  - (0:Light&Long 1:Heavy&Short)
     * @param delay - seconds
     * @return
     */
    public static byte[] trembleEffect(int type, int delay) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BOSS_ENV.getValue());
        mplew.write(1);
        mplew.write(type);
        mplew.writeInt(delay);

        return mplew.getPacket();
    }

    public static byte[] sendEngagementRequest(String name, int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENGAGE_REQUEST.getValue());
        mplew.write(0); //mode, 0 = engage, 1 = cancel, 2 = answer.. etc
        mplew.writeMapleAsciiString(name); // name
        mplew.writeInt(chrId); // playerid

        return mplew.getPacket();
    }

    /*
     * 0x0D = 恭喜你订婚成功.
     * 0x0E = 结婚成功.
     * 0x0F = 订婚失败.
     * 0x10 = 离婚成功.
     * 0x12 = 结婚典礼预约已经成功接受.
     * 0x15 = 该道具不能用于神之子  新增
     * 0x16 = 当前频道、地图找不到该角色或角色名错误.   以前0x15
     * 0x17 = 对方不在同一地图. 以前0x16
     * 0x18 = 道具栏已满.请整理其他窗口.    以前0x17
     * 0x19 = 对方的道具栏已满. 以前0x18
     * 0x1A = 同性不能结婚. 以前0x19
     * 0x1B = 您已经是订婚的状态.   以前0x1A
     * 0x1C = 对方已经是订婚的状态. 以前0x1B
     * 0x1D = 您已经是结婚的状态.   以前0x1C
     * 0x1E = 对方已经是结婚的状态. 以前0x1D
     * 0x1F = 您处于不能求婚的状态. 以前0x1E
     * 0x20 = 对方处于无法接受求婚的状态.   以前0x1F
     * 0x21 = 很遗憾对方取消了您的求婚请求. 以前0x20
     * 0x22 = 对方郑重地拒绝了您的求婚. 以前0x21
     * 0x23 = 已成功取消预约.请以后再试.    以前0x22
     * 0x24 = 预约后无法取消结婚典礼.   以前0x23
     * 0x24 = 无
     * 0x26 = 此请帖无效.   以前0x25
     */
    public static byte[] sendEngagement(byte msg, int item, MapleCharacter male, MapleCharacter female) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENGAGE_RESULT.getValue());
        mplew.write(msg); // 结婚任务 1103
        switch (msg) {
            case 0x0D:
            case 0x0E:
            case 0x11: {
                mplew.writeInt(0); // ringid or uniqueid
                mplew.writeInt(male.getId());
                mplew.writeInt(female.getId());
                mplew.writeShort(msg == 0x0E ? 0x03 : 0x01);
                mplew.writeInt(item);
                mplew.writeInt(item);
                mplew.writeAsciiString(male.getName(), 13);
                mplew.writeAsciiString(female.getName(), 13);
                break;
            }
        }

        return mplew.getPacket();
    }

    /*
     * 美洲豹更新
     */
    public static byte[] updateJaguar(MapleCharacter from) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_JAGUAR.getValue());
        PacketHelper.addJaguarInfo(mplew, from);

        return mplew.getPacket();
    }

    public static byte[] teslaTriangle(int chrId, int sum1, int sum2, int sum3) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TESLA_TRIANGLE.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(sum1);
        mplew.writeInt(sum2);
        mplew.writeInt(sum3);

        return mplew.getPacket();
    }

    public static byte[] mechPortal(Point pos) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MECH_PORTAL.getValue());
        mplew.writePos(pos);

        return mplew.getPacket();
    }

    public static byte[] spawnMechDoor(MechDoor md, boolean animated) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MECH_DOOR_SPAWN.getValue());
        mplew.write(animated ? 0 : 1);
        mplew.writeInt(md.getOwnerId());
        mplew.writePos(md.getTruePosition());
        mplew.write(md.getId());
        mplew.writeInt(md.getPartyId());

        return mplew.getPacket();
    }

    public static byte[] removeMechDoor(MechDoor md, boolean animated) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MECH_DOOR_REMOVE.getValue());
        mplew.write(animated ? 0 : 1);
        mplew.writeInt(md.getOwnerId());
        mplew.write(md.getId());

        return mplew.getPacket();
    }

    public static byte[] useSPReset(int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SP_RESET.getValue());
        mplew.write(1);
        mplew.writeInt(chrId);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] useAPReset(int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AP_RESET.getValue());
        mplew.write(1);
        mplew.writeInt(chrId);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] report(int err) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REPORT_RESULT.getValue());
        mplew.write(err); //0 = success
        if (err == 2) {
            mplew.write(0);
            mplew.writeInt(1);
        }
        return mplew.getPacket();
    }

    /*
     * 测谎仪
     */
    public static byte[] sendLieDetector(byte[] image, int attempt) {
        if (ServerConstants.isShowPacket()) {
            log.info("调用: " + new Throwable().getStackTrace()[0] + " 测谎仪图片大小: " + image.length + " 换图次数: " + (attempt - 1));
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LIE_DETECTOR.getValue());
        mplew.write(0x09); // 1 = not attacking, 2 = tested, 3 = going through
        mplew.write(0x01); // 2 give invalid pointer (suppose to be admin macro)
        mplew.write(0x01); // the time >0 is always 1 minute
        mplew.write(attempt - 1); // 更换图片次数
        if (image == null) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        mplew.writeInt(image.length);
        mplew.write(image);

        return mplew.getPacket();
    }

    public static byte[] LieDetectorResponse(byte msg) {
        return LieDetectorResponse(msg, (byte) 0);
    }

    public static byte[] LieDetectorResponse(byte msg, byte msg2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LIE_DETECTOR.getValue());
        /*
         * 0x01 = 对不进行攻击的角色不能使用
         * 0x02 = 已经被使用过测谎仪的角色
         * 0x03 = 测谎仪正在查询的角色
         */
        mplew.write(msg);
        /*
         * msg = 0x0C
         * msg2:
         * 0x00 = 成功通过测谎仪的测试，谢谢配合。祝你冒险愉快！！
         * 0x01 = 谢谢你的帮助。你没有使用非法程序。我们已给予你5000金币的奖励。
         * 0x02 = 谢谢你协助运营人员进行核查。
         * 0x03 = 成功通过测谎仪的测试，谢谢配合。祝你冒险愉快！！
         */
        mplew.write(msg2);

        return mplew.getPacket();
    }

    /*
     * 开启举报系统
     */
    public static byte[] enableReport() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.ENABLE_REPORT.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 举报系统消息
     */
    public static byte[] reportResponse(byte mode, int remainingReports) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REPORT_RESPONSE.getValue());
        mplew.writeShort(mode);
        if (mode == 2) {
            mplew.write(1);
            mplew.writeInt(remainingReports);
        }

        return mplew.getPacket();
    }

    /*
     * 终极冒险家窗口
     */
    public static byte[] ultimateExplorer() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ULTIMATE_EXPLORER.getValue());

        return mplew.getPacket();
    }

    public static byte[] pamSongUI() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PAM_SONG.getValue());
        //mplew.writeInt(0); //no clue
        return mplew.getPacket();
    }

    public static byte[] dragonBlink(int portalId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DRAGON_BLINK.getValue());
        mplew.write(portalId);

        return mplew.getPacket();
    }

    public static byte[] showTraitGain(MapleTraitType trait, int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得倾向熟练度.getType());
        mplew.writeLong(trait.getStat().getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] showTraitMaxed(MapleTraitType trait) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.超过今天可获得倾向熟练度.getType());
        mplew.writeLong(trait.getStat().getValue());

        return mplew.getPacket();
    }

    /*
     * 采集的信息
     * 0x09 还无法采集。
     * 0x0B 开始采集
     */
    public static byte[] harvestMessage(int oid, MapleEnumClass.HarvestMsg msg) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HARVEST_MESSAGE.getValue());
        mplew.writeInt(oid);
        mplew.writeInt(msg.getCode());

        return mplew.getPacket();
    }

    public static byte[] showHarvesting(int chrId, int tool) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_HARVEST.getValue());
        mplew.writeInt(chrId);
        mplew.write(tool > 0 ? 1 : 0);
        if (tool > 0) {
            mplew.writeInt(tool);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] harvestResult(int chrId, boolean success) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HARVESTED.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] makeExtractor(int chrId, String cname, Point pos, int timeLeft, int itemId, int fee) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_EXTRACTOR.getValue());
        mplew.writeInt(chrId);
        mplew.writeMapleAsciiString(cname);
        mplew.writeInt(pos.x);
        mplew.writeInt(pos.y);
        mplew.writeShort(timeLeft); //fh or time left, dunno
        mplew.writeInt(itemId); //3049000, 3049001...
        mplew.writeInt(fee);

        return mplew.getPacket();
    }

    public static byte[] removeExtractor(int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_EXTRACTOR.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(1); //probably 1 = animation, 2 = make something?

        return mplew.getPacket();
    }

    public static byte[] spouseMessage(String msg, boolean white) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        mplew.writeShort(white ? 10 : 6);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] spouseMessage(int op, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPOUSE_MESSAGE.getValue());
        /*
         * 多颜色信息提示
         * 0x00 = 白色
         * 0x01 = 绿色
         * 0x06 = 灰色
         * 0x07 = 黄色
         * 0x08 = 浅黄
         * 0x09 = 蓝色
         * 0x0A = 白底黑字
         * 0x0B = 普通提示颜色
         * 0x0C = 白底蓝字
         * 0x0D = 普通喇叭颜色
         * 0x0E = 普通喇叭颜色
         * 0x0F = 道具喇叭颜色
         * 0x10 = 紫色
         * 0x11 = 绿色抽奖喇叭颜色
         * 0x12 = 灰色
         * 0x13 = 黄色
         * 0x14 = 浅蓝色
         * 0x15 = 道具喇叭颜色
         * 0x16 = 黄色标题蓝色内容(忽略标题"[]") 蓝黄蓝3次
         * 0x17 = 浅黄
         * 0x18 = 黄色标题蓝色内容(标题加粗)
         * 0x19 = 粉色
         * 0x1A = 黄色标题紫色内容
         * 0x1B = 浅黄色
         * 0x1C = 紫色黑体
         * 0x1F = 黄底黑字
         * 0x20 = 白底粉字
         * 0x21 = 红底黑字
         * 0x23 = 红底黄字
         * 0x24 = 粉底黑字
         * 0x25 = 浅黄底黑字
         * 0x26 = 红底浅黄字
         * 0x27 = 绿底黑字
         * 0x28 = 红底黑字
         * 0x29 = 浅黄底黑字
         * 0x2A = 深蓝色
         */
        mplew.writeShort(op);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    /*
     * 打开矿物背包
     */
    public static byte[] openBag(int index, int itemId, boolean firstTime) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[53 01] [00 00 00 00] [19 12 42 00] 00 00
        mplew.writeShort(SendPacketOpcode.OPEN_BAG.getValue());
        mplew.writeInt(index);
        mplew.writeInt(itemId);
        mplew.writeShort(firstTime ? 1 : 0); //this might actually be 2 bytes

        return mplew.getPacket();
    }

    /*
     * 道具制造开始
     */
    public static byte[] craftMake(int chrId, int something, int time) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[EC 00] [9E 4E 08 00] [7C 01 00 00] [A0 0F 00 00]
        mplew.writeShort(SendPacketOpcode.CRAFT_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(something);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    /*
     * 道具制作成功
     */
    public static byte[] craftFinished(int chrId, int craftID, int ranking, int itemId, int quantity, int exp) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CRAFT_COMPLETE.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(craftID);
        mplew.writeInt(ranking);
        /*
         * 0x18	SOSO
         * 0x19	GOOD
         * 0x1A	COOL
         * 0x1B	FAIL	由于未知原因 制作道具失败
         * 0x1C	FAIL	物品制作失败.
         * 0x1D	FAIL	分解机已撤除，分解取消。
         * 0x1E	FAIL	分解机的主任无法继续获得手续费。
         */
        if (ranking == 0x18 || ranking == 0x19 || ranking == 0x1A) { //只有制作成功才发送 制作出来的道具和数量
            mplew.writeInt(itemId);
            mplew.writeInt(quantity);
        }
        mplew.writeInt(exp);

        return mplew.getPacket();
    }

    /*
     * 道具制作熟练度已满的提示
     */
    public static byte[] craftMessage(String msg) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CRAFT_MESSAGE.getValue());
        mplew.writeMapleAsciiString(msg);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] showEnchanterEffect(int cid, boolean result) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ENCHANTER_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.writeBool(result);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] showSoulScrollEffect(int cid, boolean result, boolean destroyed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SOULSCROLL_EFFECT.getValue());
        mplew.writeInt(cid);
        mplew.writeBool(result);
        mplew.writeBool(destroyed);

        return mplew.getPacket();
    }

    public static byte[] shopDiscount(int percent) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOP_DISCOUNT.getValue());
        mplew.write(percent);

        return mplew.getPacket();
    }

    public static byte[] pendantSlot(boolean p) { //slot -59
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PENDANT_SLOT.getValue());
        mplew.write(p ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getBuffBar(long millis) { //You can use the buff again _ seconds later. + bar above head
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BUFF_BAR.getValue());
        mplew.writeLong(millis);

        return mplew.getPacket();
    }

    public static byte[] showMidMsg(String s, int l) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MID_MSG.getValue());
        mplew.write(l); //i think this is the line.. or soemthing like that. 1 = lower than 0
        mplew.writeMapleAsciiString(s);
        mplew.write(s.length() > 0 ? 0 : 1); //remove?

        return mplew.getPacket();
    }

    public static byte[] updateGender(MapleCharacter chr) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_GENDER.getValue());
        mplew.write(chr.getGender());

        return mplew.getPacket();
    }

    /*
     * 显示副本进度
     */
    public static byte[] achievementRatio(int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ACHIEVEMENT_RATIO.getValue()); //not sure
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    /*
     * 创建终极冒险岛家检测提示
     */
    public static byte[] createUltimate(int amount) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 0x00 创建成功
         * 0x01 已存在同名的角色
         * 0x02 角色栏已满。
         * 0x03 无法使用该名字。
         */
        mplew.writeShort(SendPacketOpcode.CREATE_ULTIMATE.getValue());
        mplew.writeInt(amount);

        return mplew.getPacket();
    }

    public static byte[] updateSpecialStat(String stat, int array, int mode, boolean unk, int chance) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * Recv PROFESSION_INFO [008E] (25)
         * 8E 00
         * 08 00 39 32 30 33 30 30 30 30
         * 05 00 00 00
         * 07 00 00 00
         * 01
         * 64 00 00 00
         * ?..92030000.........d...
         *
         */
        mplew.writeShort(SendPacketOpcode.PROFESSION_INFO.getValue());
        mplew.writeMapleAsciiString(stat);
        mplew.writeInt(array);
        mplew.writeInt(mode);
        mplew.write(unk ? 1 : 0);
        mplew.writeInt(chance);

        return mplew.getPacket();
    }

    public static byte[] getQuickSlot(MapleQuickSlot quickslot) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.QUICK_SLOT.getValue());
        quickslot.writeData(mplew);

        return mplew.getPacket();
    }

    public static byte[] updateImp(MapleImp imp, int mask, int index, boolean login) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ITEM_POT.getValue());
        mplew.write(login ? 0 : 1); //0 = unchanged, 1 = changed
        mplew.writeInt(index + 1);
        mplew.writeInt(mask);
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0) {
            Pair<Integer, Integer> i = MapleItemInformationProvider.getInstance().getPot(imp.getItemId());
            if (i == null) {
                return enableActions();
            }
            mplew.writeInt(i.left);
            mplew.write(imp.getLevel()); //probably type
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.STATE.getValue()) != 0) {
            mplew.write(imp.getState());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.FULLNESS.getValue()) != 0) {
            mplew.writeInt(imp.getFullness());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CLOSENESS.getValue()) != 0) {
            mplew.writeInt(imp.getCloseness());
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CLOSENESS_LEFT.getValue()) != 0) {
            mplew.writeInt(1); //how much closeness is available to get right now
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MINUTES_LEFT.getValue()) != 0) {
            mplew.writeInt(0); //how much mins till next closeness
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.LEVEL.getValue()) != 0) {
            mplew.write(1); //k idk
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.FULLNESS_2.getValue()) != 0) {
            mplew.writeInt(imp.getFullness()); //idk
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.UPDATE_TIME.getValue()) != 0) {
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.CREATE_TIME.getValue()) != 0) {
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.AWAKE_TIME.getValue()) != 0) {
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.SLEEP_TIME.getValue()) != 0) {
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_CLOSENESS.getValue()) != 0) {
            mplew.writeInt(100); //max closeness available to be gotten
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_DELAY.getValue()) != 0) {
            mplew.writeInt(1000); //idk, 1260?
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_FULLNESS.getValue()) != 0) {
            mplew.writeInt(1000);
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_ALIVE.getValue()) != 0) {
            mplew.writeInt(1); //k ive no idea
        }
        if ((mask & ImpFlag.SUMMONED.getValue()) != 0 || (mask & ImpFlag.MAX_MINUTES.getValue()) != 0) {
            mplew.writeInt(10); //max minutes?
        }
        mplew.write(0); //or 1 then lifeID of affected pot, OR IS THIS 0x80000?

        return mplew.getPacket();
    }

    public static byte[] spawnFlags(List<Pair<String, Integer>> flags) { //Flag_R_1 to 0, etc
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_WELCOME.getValue());
        mplew.write(flags == null ? 0 : flags.size());
        if (flags != null) {
            for (Pair<String, Integer> f : flags) {
                mplew.writeMapleAsciiString(f.left);
                mplew.write(f.right);
            }
        }

        return mplew.getPacket();
    }

    public static byte[] showStatusMessage(String info, String data) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.系统灰字公告.getType());
        mplew.writeMapleAsciiString(info); //name got Shield.
        mplew.writeMapleAsciiString(data); //Shield applied to name.

        return mplew.getPacket();
    }

    public static byte[] changeTeam(int cid, int type) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOAD_TEAM.getValue());
        mplew.writeInt(cid);
        mplew.write(type); //2?

        return mplew.getPacket();
    }

    /*
     * 显示快速移动
     */
    public static byte[] showQuickMove(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.QUICK_MOVE.getValue());
        List<MapleQuickMove> quickMove = MapleQuickMove.getQuickMove(chr.getMapId());
        mplew.write(quickMove.size());
        int i = 0;
        for (MapleQuickMove map : quickMove) {
            mplew.writeShort(i);
            mplew.writeMapleAsciiString(map.name); //NPC名字介绍
            mplew.writeInt(map.npcid); //NPCid
            mplew.writeInt(map.type); //NPC编号
            mplew.writeInt(map.level); //传送需要的等级
            mplew.writeMapleAsciiString(map.desc); //NPC功能介绍
            mplew.writeLong(PacketHelper.getTime(-2)); //00 40 E0 FD 3B 37 4F 01
            mplew.writeLong(PacketHelper.getTime(-1)); //00 80 05 BB 46 E6 17 02
            i++;
        }

        return mplew.getPacket();
    }

    /*
     * 显示恶魔精气获得效果
     */
    public static byte[] showForce(MapleCharacter chr, int moboid, int forceCount, int forceColor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(1);
        mplew.writeInt(chr.getId());
        mplew.writeInt(moboid);
        mplew.writeInt(0);
        mplew.write(1);
        mplew.writeInt(forceCount);
        /*
         * 0x0A 黑色 打鬼怪出现
         */
        mplew.writeInt(forceColor);
        mplew.writeInt(0x2F);
        mplew.writeInt(0x06);
        mplew.writeInt(0x2E);
        mplew.writeZeroBytes(25); //V.114修改 以前是7

        return mplew.getPacket();
    }

    public static byte[] updateCardStack(int total) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CARTE.getValue());
        mplew.write(total);

        return mplew.getPacket();
    }

    public static byte[] 幻影卡片效果(int chrId, int oid, int skillId, int forceCount, int color, int times) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(1); //攻击效果
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        //开始
        for (int i = 0; i < times; i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i);
            mplew.writeInt(color);
            mplew.writeInt(0x12);
            mplew.writeInt(0x07);
            mplew.writeInt(0x05);
            mplew.writeInt(0x00);
            mplew.writeZeroBytes(8);
            mplew.writeZeroBytes(8);
            mplew.writeInt(0);
        }
        //结束
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 刺客标记效果(int chrId, int oid, int forceCount, boolean isAssassin, List<Integer> moboids, int visProjectile, Point posFrom) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(1);
        mplew.writeInt(chrId); //角色ID
        mplew.writeInt(oid); //怪物的工作ID
        mplew.writeInt(0x0B); //刺客标记是0x0B
        mplew.write(1);
        mplew.writeInt(moboids.size()); //攻击怪物的数量
        for (int moboid : moboids) {
            mplew.writeInt(moboid);
        }
        mplew.writeInt(isAssassin ? 4100012 : 4120019); //技能ID 4120019 4100012
        for (int i = 0; i < moboids.size(); i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i);
            mplew.writeInt(isAssassin ? 1 : 2);
            mplew.writeInt(Randomizer.rand(0x20, 0x30));
            mplew.writeInt(Randomizer.rand(3, 4));
            mplew.writeInt(Randomizer.rand(100, 200));
            mplew.writeInt(200);
            mplew.writeZeroBytes(8);
            mplew.writeZeroBytes(8);
            mplew.writeInt(0);
        }
        mplew.write(0);
        mplew.writeInt(posFrom.x - 120);
        mplew.writeInt(posFrom.y - 100);
        mplew.writeInt(posFrom.x + 120);
        mplew.writeInt(posFrom.y + 100);
        mplew.writeInt(visProjectile); //飞镖的外形道具

        return mplew.getPacket();
    }

    /*
     * 显示金钱炸弹攻击效果
     */
    public static byte[] 金钱炸弹效果(int chrId, int skillId, int forceCount, List<Integer> moboids, List<Point> posFroms) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId); //角色ID
        mplew.writeInt(0x0C); //刺客标记是0x0B
        mplew.write(1);
        mplew.writeInt(moboids.size()); //攻击怪物的数量
        for (int moboid : moboids) {
            mplew.writeInt(moboid);
        }
        mplew.writeInt(skillId); //技能ID
        //处理攻击的次数
        for (int i = 0; i < posFroms.size(); i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i);
            mplew.writeInt(0x01);
            mplew.writeInt(Randomizer.rand(0x28, 0x2C));
            mplew.writeInt(Randomizer.rand(3, 4));
            mplew.writeInt(Randomizer.rand(50 + i * 10, 100 + i * 10));
            mplew.writeInt(700);
            Point posFrom = posFroms.get(i);
            mplew.writeInt(posFrom != null ? posFrom.x : 0);
            mplew.writeInt(posFrom != null ? posFrom.y : 0);
            mplew.writeZeroBytes(8); //未知
            mplew.writeInt(0);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 显示狂龙剑刃之壁攻击效果
     */
    public static byte[] showTempestBladesAttack(int chrId, int skillId, int forceCount, List<Integer> moboids, int attackCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId); //角色ID
        mplew.writeInt(0x02); //剑刃之壁攻击效果
        mplew.write(1);
        mplew.writeInt(moboids.size()); //攻击怪物的数量
        for (int moboid : moboids) {
            mplew.writeInt(moboid);
        }
        mplew.writeInt(skillId); //技能ID
        int type = 1;
        switch (skillId) {
            case 狂龙战士.剑刃之壁:
                type = 1;
                break;
            case 狂龙战士.进阶剑刃之壁:
                type = 2;
                break;
            case 狂龙战士.剑刃之壁_变身:
                type = 3;
                break;
            case 狂龙战士.进阶剑刃之壁_变身:
                type = 4;
                break;
        }
        //这个地方不管怪物数量多少 都需要循环当前技能攻击怪物的最大数量
        for (int i = 0; i < attackCount; i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i + 1); //好像默认从2开始
            mplew.writeInt(type);
            mplew.writeInt(Randomizer.rand(15, 20));
            mplew.writeInt(Randomizer.rand(20, 30));
            mplew.writeInt(0);
            mplew.writeInt(Randomizer.rand(1000, 1500));
            mplew.writeZeroBytes(8);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 暴风灭世效果(int chrId, int oid, int skillId, int forceCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(0x08); //攻击效果
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        //开始
        mplew.write(1);
        mplew.writeInt(forceCount);
        mplew.writeInt(0x01);
        mplew.writeInt(0x01);
        mplew.writeInt(0x06);
        mplew.writeInt(270);
        mplew.writeInt(0x4B);
        mplew.writeInt(0x23);
        mplew.writeInt(0x2A);
        mplew.writeZeroBytes(8);
        mplew.writeInt(0);
        //结束
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 灵魂吸取精髓(int chrId, int skillId, int forceCount, List<Integer> moboids, int attackCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId); //角色ID
        mplew.writeInt(0x03); //首次使用灵魂吸取的效果
        mplew.write(1);
        mplew.writeInt(moboids.size()); //攻击怪物的数量
        for (int moboid : moboids) {
            mplew.writeInt(moboid);
        }
        mplew.writeInt(skillId); //技能ID
        for (int i = 0; i < attackCount; i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i + 1); //好像默认从2开始
            mplew.writeInt(0x01);
            mplew.writeInt(Randomizer.rand(0x10, 0x12));
            mplew.writeInt(Randomizer.rand(0x14, 0x1C));
            mplew.writeInt(Randomizer.rand(0x22, 0x42));
            mplew.writeInt(540); //[1C 02 00 00]
            mplew.writeZeroBytes(8);
            mplew.writeZeroBytes(8);
            mplew.writeInt(0);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 灵魂吸取攻击(int chrId, int skillId, int forceCount, int moboid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0x01);
        mplew.writeInt(chrId);
        mplew.writeInt(moboid);
        mplew.writeInt(0x04); //灵魂吸取重新生成精髓的效果
        mplew.write(1);
        mplew.writeInt(moboid);
        mplew.writeInt(skillId);
        //开始
        mplew.write(1);
        mplew.writeInt(forceCount);
        mplew.writeInt(0x01);
        mplew.writeInt(0x2A);
        mplew.writeInt(0x04);
        mplew.writeInt(0x2A);
        mplew.writeInt(0x00);
        mplew.writeZeroBytes(8);
        mplew.writeZeroBytes(8);
        mplew.writeInt(0);
        //结束
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 三彩箭矢效果(int chrId, int oid, int skillId, int forceCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(0x0A); //攻击效果
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        //开始
        mplew.write(1);
        mplew.writeInt(forceCount);
        mplew.writeInt(0x00);
        mplew.writeInt(0x0D);
        mplew.writeInt(0x09);
        mplew.writeInt(0xD3);
        mplew.writeInt(0x23);
        mplew.writeZeroBytes(8);
        mplew.writeZeroBytes(8);
        mplew.writeInt(0);
        //结束
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 炎术士轨道烈焰
     */
    public static byte[] showTrackFlames(int chrid, int skillid, byte skilllevel, int forceCount, int mobid, short direction) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrid);
        mplew.writeInt(0x11);
        mplew.write(1);
        mplew.writeInt(mobid);
        mplew.writeInt(skillid);

        mplew.write(1);
        mplew.writeInt(forceCount);
        mplew.writeInt(0x04);
        mplew.writeInt(skilllevel);
        mplew.writeInt(skilllevel);
        mplew.writeInt(0x5A);
        mplew.writeZeroBytes(12);
        mplew.writeHexString("D0 B3 68 D7");
        mplew.writeInt(4);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(direction);
        mplew.writeInt(500);

        return mplew.getPacket();
    }

    public static byte[] ShieldChacing(int cid, List<Integer> moblist, int skillid, int szie, int delay) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(cid);
        mplew.writeInt(3);
        mplew.write(1);
        mplew.writeInt(moblist.size());
        for (Integer aMoblist : moblist) {
            mplew.writeInt(aMoblist);
        }
        mplew.writeInt(skillid);
        for (int i = 1; i <= szie; i++) {
            mplew.write(1);
            mplew.writeInt(1 + i);
            mplew.writeInt(3);
            mplew.writeInt(Randomizer.rand(1, 20));
            mplew.writeInt(Randomizer.rand(20, 50));
            mplew.writeInt(Randomizer.rand(50, 200));
            mplew.writeInt(delay);
            mplew.writeLong(0);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] ShieldChacing(int chrId, int skillId, int forceCount, List<Integer> mobids, int szieLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(skillId == 恶魔复仇者.追击盾 ? 3 : 20);
        mplew.write(1);
        mplew.writeInt(mobids.size()); //攻击怪物的数量
        for (Integer mobid : mobids) {
            mplew.writeInt(mobid);
        }
        mplew.writeInt(skillId); //技能ID
        for (int i = 0; i <= szieLevel; i++) {
            mplew.write(1);
            mplew.writeInt(skillId == 恶魔复仇者.追击盾 ? 1 + i : forceCount + i);
            mplew.writeInt(skillId == 恶魔复仇者.追击盾 ? 3 : 2);
            mplew.writeInt(Randomizer.rand(1, 20));
            mplew.writeInt(Randomizer.rand(20, 50));
            mplew.writeInt(Randomizer.rand(50, 200));
            mplew.writeInt(660);
            mplew.writeLong(0);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] ShieldChacingRe(int cid, int unkwoun, int oid, int cn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(1);
        mplew.writeInt(cid);
        mplew.writeInt(unkwoun);
        mplew.writeInt(4);
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.writeInt(31221014);

        mplew.write(1);
        mplew.writeInt(cn + 1);
        mplew.writeInt(3);
        mplew.writeInt(Randomizer.rand(40, 44));
        mplew.writeInt(3);
        mplew.writeInt(Randomizer.rand(36, 205));
        mplew.writeInt(0);
        mplew.writeLong(0);
        mplew.writeInt(Randomizer.nextInt());
        mplew.writeInt(0);
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 心灵传动攻击效果(int chrId, int oid, int skillId, int forceCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(0x16); //攻击效果
        mplew.write(1);
        mplew.writeInt(oid);
        mplew.writeInt(skillId);
        //开始
        mplew.write(1);
        mplew.writeInt(forceCount);
        mplew.writeInt(0x00);
        mplew.writeInt(0x18);
        mplew.writeInt(0x09);
        mplew.writeInt(0x74);
        mplew.writeInt(0x3C0);
        mplew.writeZeroBytes(8);
        mplew.writeZeroBytes(8);
        mplew.writeInt(0);
        //结束
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] 美洲豹攻击效果(int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PANTHER_ATTACK.getValue());
        mplew.writeInt(skillid);

        return mplew.getPacket();
    }

    public static byte[] openPantherAttack(boolean on) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_PANTHER_ATTACK.getValue());
        mplew.writeBool(on);

        return mplew.getPacket();

    }

    public static byte[] showRedNotice(String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.系统红字公告.getType());
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    public static byte[] sendloginSuccess() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_SUCC.getValue());

        return mplew.getPacket();
    }

    public static byte[] showCharCash(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_CASH.getValue());
        mplew.writeInt(chr.getId());
        mplew.writeInt(chr.getCSPoints(2));

        return mplew.getPacket();
    }

    public static byte[] showPlayerCash(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_PLAYER_CASH.getValue());
        mplew.writeInt(chr.getCSPoints(1));
        mplew.writeInt(chr.getCSPoints(2));

        return mplew.getPacket();
    }

    public static byte[] playerCashUpdate(int mode, int toCharge, MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_CASH_UPDATE.getValue());
        mplew.writeInt(mode);
        mplew.writeInt(toCharge == 1 ? chr.getCSPoints(1) : 0);
        mplew.writeInt(chr.getCSPoints(2));
        mplew.write(toCharge);
        mplew.writeShort(0); //未知

        return mplew.getPacket();
    }

    public static byte[] playerSoltUpdate(int itemid, int acash, int mpoints) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHARSOLE.getValue());

        mplew.writeInt(itemid);
        mplew.writeInt(acash);
        mplew.writeInt(mpoints);
        mplew.write(1);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] sendTestPacket(String test) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.write(HexTool.getByteArrayFromHexString(test));
        return mplew.getPacket();
    }

    public static byte[] GainEXP_Monster(String testmsg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.获得经验.getType()); // 3 = 经验, 4 = SP, 5 = 人气, 7 = 家族GP ,8 = 贡献值 , 11 = 豆豆 12 = 精灵小助手
        mplew.write(1); // 1 = 白色 2 = 黄色
        mplew.writeInt(1000); // 多少经验
        mplew.write(0); // 不在聊天框显示

        mplew.writeInt(0); //活动奖励经验
        mplew.writeShort(0);
        mplew.writeInt(0); //结婚奖励经验
        mplew.writeInt(0); //召回戒指组队经验
        mplew.write(0); //活动组队经验奖励倍数(i x 100)最大3倍
        mplew.writeInt(0); //组队经验
        mplew.writeInt(0); //道具佩戴奖励经验
        mplew.writeInt(0); //网吧特别经验
        mplew.writeInt(0); //彩虹周奖励经验
        mplew.writeInt(0); //欢乐奖励经验
        mplew.writeInt(0); //飞跃奖励经验
        mplew.writeInt(0); //精灵祝福经验 (NULL)奖励经验
        mplew.writeInt(0); //增益奖励经验
        mplew.writeInt(0); //休息奖励经验
        mplew.writeInt(0); //物品奖励经验
        mplew.writeInt(0); //Cake vs Pie Bonus 经验
        mplew.writeInt(0); //Pvp Bonus 经验
        //mplew.write(HexTool.getByteArrayFromHexString(testmsg));

        return mplew.getPacket();
    }

    /*
     * 传授技能的提示
     */
    public static byte[] UpdateLinkSkillResult(int skillId, int mode) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_LINKSKILL_RESULT.getValue());
        mplew.writeInt(skillId); //技能ID
        mplew.writeInt(mode);

        return mplew.getPacket();
    }

    public static final byte[] DeleteLinkSkillResult(Map<Integer, Integer> map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DELETE_LINKSKILL_RESULT.getValue());
        mplew.writeInt(map.size());
        for (Entry<Integer, Integer> entry : map.entrySet()) {
            mplew.writeInt(entry.getKey());
            mplew.writeInt(entry.getValue());
        }
        return mplew.getPacket();
    }

    public static final byte[] SetLinkSkillResult(int skillId, Pair<Integer, SkillEntry> skillinfo, int linkSkillId, int linkSkillLevel) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SET_LINKSKILL_RESULT.getValue());
        PacketHelper.writeSonOfLinkedSkill(mplew, skillId, skillinfo);
        mplew.writeInt(linkSkillId);
        if (linkSkillId > 0) {
            mplew.writeInt(linkSkillLevel);
        }
        return mplew.getPacket();
    }

    /*
     * 显示武林道场时间排名 1:全体排名 3:职业排名
     */
    public static byte[] getMulungRanking(byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MULUNG_DOJO_RANKING.getValue());
        DojoRankingsData data = DojoRankingsData.loadLeaderboard();
        mplew.write(type);
        mplew.writeInt(data.totalCharacters);
        for (int i = 0; i < data.totalCharacters; i++) {
            mplew.writeShort(data.ranks[i]);
            mplew.writeMapleAsciiString(data.names[i]);
            mplew.writeLong(data.times[i]);
        }
        return mplew.getPacket();
    }

    /*
     * 显示武林道场消息
     */
    public static byte[] getMulungMessage(boolean dc, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MULUNG_MESSAGE.getValue());
        mplew.write(dc ? 1 : 0);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    //    public static byte[] showSilentCrusadeMsg(byte type, short chapter) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//
//        mplew.writeShort(SendPacketOpcode.SILENT_CRUSADE_MSG.getValue());
//        mplew.write(type);
//        mplew.writeShort(chapter - 1);
//
//        return mplew.getPacket();
//    }
    /*
     * 确认十字商店交易
     */
    public static byte[] confirmCrossHunter(byte code) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CONFIRM_CROSS_HUNTER.getValue());
        /*
         * 0x00 物品购买完成。
         * 0x01 道具不够.
         * 0x02 背包空间不足。
         * 0x03 无法拥有更多物品。
         * 0x04 现在无法购买物品。
         */
        mplew.write(code);

        return mplew.getPacket();
    }

    /*
     * 打开1个网页地址
     */
    public static byte[] openWeb(String web) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.OPEN_WEB.getValue());
        mplew.writeMapleAsciiString(web);

        return mplew.getPacket();
    }

    /*
     * 更新角色内在能力技能
     * 参数 角色
     * 参数 是否升级
     */
    public static byte[] updateInnerSkill(int skillId, int skillevel, byte position, byte rank) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_INNER_SKILL.getValue());
        mplew.write(0x01);
        mplew.write(0x01);
        mplew.writeShort(position);
        mplew.writeInt(skillId);
        mplew.writeShort(skillevel);
        mplew.writeShort(rank);
        mplew.write(0x01);

        return mplew.getPacket();
    }

    /*
     * 更新角色内在能力
     * 参数 角色
     * 参数 是否升级
     */
    public static byte[] updateInnerStats(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_INNER_STATS.getValue());
        mplew.writeInt(chr.getHonor()); //声望点数

        return mplew.getPacket();
    }

    /*
     * 系统警告
     * 冒险岛运营员NPC自定义对话
     */
    public static byte[] sendPolice(String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MAPLE_ADMIN.getValue());
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    /*
     * 显示每日免费超级时空卷可以移动次数
     */
    public static byte[] showChronosphere(int mf, int cs) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DAY_OF_CHRONOSPHERE.getValue());
        mplew.writeInt(30 - mf);
        mplew.writeInt(cs);

        return mplew.getPacket();
    }

    /*
     * 超级时空卷错误出现
     * 0x02 超时空卷不够
     */
    public static byte[] errorChronosphere() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ERROR_CHRONOSPHERE.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] testPacket(String testmsg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.write(HexTool.getByteArrayFromHexString(testmsg));

        return mplew.getPacket();
    }

    public static byte[] testPacket(byte[] testmsg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.write(testmsg);

        return mplew.getPacket();
    }

    public static byte[] testPacket(String op, String text) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.write(HexTool.getByteArrayFromHexString(op));
        mplew.writeMapleAsciiString(text);

        return mplew.getPacket();
    }

    /*
     * 幻影封印之瞳
     */
    public static byte[] 封印之瞳(MapleCharacter chr, List<Integer> memorySkills) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_MEMORY.getValue());
        mplew.write(0x01);
        mplew.writeInt(chr.getId());
        mplew.writeInt(0x04);
        chr.writeJobData(mplew);
        mplew.writeInt(memorySkills.size());
        for (int i : memorySkills) {
            mplew.writeInt(i);
        }

        return mplew.getPacket();

    }

    /*
     * Recv SKILL_MEMORY [002E] (12)
     * 2E 00
     * 01
     * 03
     * 01 00 00 00 - 技能在第个栏
     * 01 00 00 00 - 技能在当前栏的位置
     * ............
     */
    public static byte[] 幻影删除技能(int position) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILL_TICK.getValue());
        mplew.write(1);
        mplew.write(3);
        if (position < 4) {
            mplew.writeInt(1);
            mplew.writeInt(position);
        } else if (position >= 4 && position < 8) {
            mplew.writeInt(2);
            mplew.writeInt(position - 4);
        } else if (position >= 8 && position < 11) {
            mplew.writeInt(3);
            mplew.writeInt(position - 8);
        } else if (position >= 11 && position < 13) {
            mplew.writeInt(4);
            mplew.writeInt(position - 11);
        }

        return mplew.getPacket();
    }

    public static byte[] 修改幻影装备技能(int skillId, int teachId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.EQUIPPED_SKILL.getValue());
        mplew.write(1);
        mplew.write(1);
        mplew.writeInt(skillId);
        mplew.writeInt(teachId);

        return mplew.getPacket();
    }

    public static byte[] 幻影复制错误() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILL_TICK.getValue());
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] 幻影复制技能(int position, int skillId, int level) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_SKILL_TICK.getValue());
        mplew.write(1);
        mplew.write(0);
        if (position < 4) {
            mplew.writeInt(1);
            mplew.writeInt(position);
        } else if (position >= 4 && position < 8) {
            mplew.writeInt(2);
            mplew.writeInt(position - 4);
        } else if (position >= 8 && position < 11) {
            mplew.writeInt(3);
            mplew.writeInt(position - 8);
        } else if (position >= 11 && position < 13) {
            mplew.writeInt(4);
            mplew.writeInt(position - 11);
        }
        mplew.writeInt(skillId);
        mplew.writeInt(level);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 未知封包 右键点击玩家出现的返回封包
     * 好像不发送申请交易的一方就无法交易中放道具
     */
    public static byte[] sendUnkPacket1FC() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_REQUEST.getValue());
        mplew.write(0x01);

        return mplew.getPacket();
    }

    public static byte[] SystemProcess() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SYSTEM_PROCESS_LIST.getValue());
        mplew.write(0x01);

        return mplew.getPacket();
    }

    /*
     * 显示连续击杀怪物的效果
     */
    public static byte[] showContinuityKill(boolean top, int exp, int kills, int moboid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(MapleStatusInfo.连续击杀.getType());
        mplew.write(top ? 0 : 1); //这个地方如果要顶部提示就是 0
        if (top) {
            mplew.writeLong(exp); //获得多少经验
        }
        mplew.writeInt(kills); //已经连续击杀多少次 如果是顶部这个地方就是1下杀死的怪物数量
        if (!top) {
            mplew.writeInt(moboid);
        }
        mplew.writeInt(kills);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] showGainVictoryEffect(long VictoryExp, int VictoryNum, int Victoryoid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(0x22);
        mplew.write(1);
        mplew.writeLong(VictoryExp);
        mplew.writeInt(VictoryNum);
        mplew.writeInt(Victoryoid);

        return mplew.getPacket();
    }

    public static byte[] showVictoryEffect(int Victorynum, long VictoryExp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.writeShort(0x22);
        mplew.writeLong(VictoryExp);
        mplew.writeInt(Victorynum);
        return mplew.getPacket();
    }

    public static byte[] showGainWeaponPoint(int gainwp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.writeShort(MapleStatusInfo.获得WP.getType());
        mplew.writeInt(gainwp);

        return mplew.getPacket();
    }

    public static byte[] updateWeaponPoint(int wp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATA_WP.getValue());
        mplew.writeInt(wp);

        return mplew.getPacket();
    }

    /**
     * 别人看到加血效果
     *
     * @param chrId
     * @param skillid
     * @param effectid
     * @return
     */
    public static byte[] showHPEffect(int chrId, int skillid, int effectid) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_FOREIGN_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(effectid);
        mplew.writeInt(skillid);
        mplew.write(10); //player level 好像不用写角色等级
        mplew.write(11); //skill level
        mplew.write(2);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    //额外攻击
    public static byte[] ExtraAttack(int skillid, int cskillid, int weapon, int type, int mobid) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.EXTRA_ATTACK.getValue());
        mplew.writeInt(skillid);//攻击技能
        mplew.writeInt(cskillid);//被动技能
        mplew.writeInt(weapon);//武器类型
        mplew.writeInt(type);
        mplew.writeInt(mobid);//怪物id
        mplew.writeInt(0);
        if (cskillid == 101000102) {
            mplew.write(0);
            mplew.writeShort(0);
            mplew.writeShort(0);
        }
        return mplew.getPacket();
    }

    public static byte[] SummonEnergy1(short type, int id, int mapid, int oid, int Type, Point Spos, Point toPos, int Tiem, int skillid, int skilllevel, int s2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANNON_PLATE.getValue());
        mplew.writeShort(type);
        mplew.writeInt(id);
        mplew.writeInt(mapid);
        mplew.writeShort(1);
        mplew.writeInt(oid);
        mplew.writeShort(Type);
        mplew.writePos(Spos);
        if (Type == 5) {
            mplew.writePos(toPos);
        }
        mplew.writeShort(s2);
        mplew.writeInt(Tiem);
        mplew.writeShort(s2);
        mplew.writeInt(skillid);
        mplew.writeShort(skilllevel);
        mplew.write(0);
        return mplew.getPacket();
    }

    public static byte[] SummonEnergy2(int type, int cid, int mapid, int s1, int s2, int skillid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CANNON_PLATE.getValue());
        mplew.writeShort(type);
        mplew.writeInt(cid);
        mplew.writeInt(mapid);
        mplew.writeInt(cid);
        mplew.writeInt(skillid);
        mplew.writeInt(s1);
        mplew.writeInt(s2);

        return mplew.getPacket();
    }

    public static byte[] openWorldMap() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VIEW_WORLDMAP.getValue());
        mplew.writeInt(0);
        return mplew.getPacket();
    }

    /*
     * 技能重生
     *
     */
    public static byte[] skillActive() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_ACTIVE.getValue());

        return mplew.getPacket();
    }

    public static byte[] skillNotActive(int skillId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_NOT_ACTIVE.getValue());
        mplew.writeInt(skillId);

        return mplew.getPacket();
    }

    public static byte[] updateJianQi(int jianqi) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.JIANQI_POINTS.getValue());
        mplew.writeShort(jianqi);

        return mplew.getPacket();
    }

    public static byte[] sendCritAttack() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CRIT_STATUS.getValue());

        return mplew.getPacket();
    }

    public static byte[] updateSoulEffect(int chrid, boolean open) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SOUL_MODE.getValue());

        mplew.writeInt(chrid);
        mplew.writeBool(open);

        return mplew.getPacket();
    }

    public static byte[] spawnRune(MapleRune rune, boolean respawn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(respawn ? SendPacketOpcode.RESPAWN_RUNE.getValue() : SendPacketOpcode.SPAWN_RUNE.getValue());
        mplew.writeInt(respawn ? 0 : 1);
        mplew.writeInt(0);
        mplew.writeInt(rune.getRuneType());
        mplew.writeInt(rune.getPosition().x);
        mplew.writeInt(rune.getPosition().y);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] removeRune(MapleRune rune) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_RUNE.getValue());
        mplew.writeInt(0);
        mplew.writeInt(rune.getObjectId());
        mplew.writeInt(200);

        return mplew.getPacket();
    }

    public static byte[] RuneAction(int type, int time) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RUNE_ACTION.getValue());
        mplew.writeInt(type);
        mplew.writeInt(time);

        return mplew.getPacket();
    }

    public static byte[] showRuneEffect(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.RUNE_EFFECT.getValue());
        mplew.writeInt(type);

        return mplew.getPacket();
    }

    /**
     * **
     * 第一次分裂攻击
     *
     * @param chrId
     * @param skillId
     * @param forceCount
     * @param mobids
     * @param szieLevel
     * @return
     */
    public static byte[] Show影子蝙蝠锁定(int chrId, int skillId, int mobids, int szieLevel, Point posFrom, int forceCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(0);
        mplew.writeInt(chrId);
        mplew.writeInt(15);
        mplew.write(1);
        mplew.writeInt(mobids);
        mplew.writeInt(skillId); //技能ID
        for (int i = 0; i < szieLevel; i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + 1);
            mplew.writeInt(3);
            mplew.writeInt(Randomizer.rand(1, 20));
            mplew.writeInt(Randomizer.rand(20, 50));
            mplew.writeInt(Randomizer.rand(50, 200));
            mplew.writeInt(660);
            mplew.writeLong(0);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(0);
        mplew.writeInt(posFrom.x - 120);
        mplew.writeInt(posFrom.y - 100);
        mplew.writeInt(posFrom.x + 120);
        mplew.writeInt(posFrom.y + 100);
        return mplew.getPacket();
    }

    /**
     * *
     * 蝙蝠分裂效果
     *
     * @param chrId
     * @param mobids
     * @param posFrom
     * @return
     */
    public static byte[] Show影子蝙蝠_分裂(int chrId, int mobids, Point posFrom, int toMobid, int forceCount) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GAIN_FORCE.getValue());
        mplew.write(1);
        mplew.writeInt(chrId);
        mplew.writeInt(mobids);
        mplew.writeInt(MapleForceType.影子蝙蝠_反射.ordinal());
        mplew.write(1);
        mplew.writeInt(toMobid);
        mplew.writeInt(夜行者.影子蝙蝠_反弹); //技能ID
        for (int i = 0; i < 1; i++) {
            mplew.write(1);
            mplew.writeInt(forceCount + i);
            mplew.writeInt(1);
            mplew.writeInt(5);
            mplew.writeInt(5);
            mplew.writeInt(5);
            mplew.writeInt(43);
            mplew.writeLong(1);
            mplew.writeInt(Randomizer.nextInt());
            mplew.writeInt(0);
            mplew.writeInt(0);
        }
        mplew.write(0);
        mplew.writeInt(posFrom.x - 120);
        mplew.writeInt(posFrom.y - 100);
        mplew.writeInt(posFrom.x + 120);
        mplew.writeInt(posFrom.y + 100);
        return mplew.getPacket();
    }

    public static byte[] pamsSongEffect(int chrId) {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PAMS_SONG.getValue());
        mplew.writeInt(chrId);

        return mplew.getPacket();
    }

    public static byte[] pamsSongUI() {
        if (ServerConfig.DEBUG_MODE) {
            log.trace("调用");
        }
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PAMS_SONG.getValue());
        mplew.writeShort(0); //doesn't seem to change it

        return mplew.getPacket();
    }

    public static byte[] startBattleStatistics() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BATTLE_STATISTICS.getValue());
        mplew.write(1); //doesn't seem to change it

        return mplew.getPacket();
    }

    public static byte[] updateDamageSkin(MapleCharacter player, boolean save) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(save ? SendPacketOpcode.SAVE_DAMSKIN.getValue() : SendPacketOpcode.DELETE_DAMSKIN.getValue());
        writeDamageSkinData(mplew, player);

        return mplew.getPacket();
    }

    public static void writeDamageSkinData(final MaplePacketLittleEndianWriter mplew, final MapleCharacter player) {
        final String customData = player.getQuestNAdd(MapleQuest.getInstance(7291)).getCustomData();
        mplew.writeInt(customData == null ? 0 : Integer.valueOf(customData));
        mplew.writeInt(Integer.valueOf(player.getKeyValue("DAMAGE_SKIN_SLOT")));
        mplew.writeInt(player.getDamSkinList().size());
        player.getDamSkinList().forEach(mplew::writeInt);
    }

    public static byte[] changeHour(int n1, int n2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.HOURCHANGED.getValue());
        mplew.writeShort(n1);
        mplew.writeShort(n2);

        return mplew.getPacket();
    }

    public static byte[] createObtacleAtom(int count, int type1, int type2, MapleMap map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREATE_OBTACLEATOM.getValue());
        mplew.writeInt(0);
        mplew.writeInt(count);
        mplew.write(0);
        int n5 = Randomizer.nextInt(200000);
        for (int i2 = 0; i2 < count; ++i2) {
            MapleFoothold foothold = map.getFootholds().getAllRelevants().get(Randomizer.nextInt(map.getFootholds().getAllRelevants().size()));
            int n6 = foothold.getY2();
            int n7 = Randomizer.rand(map.getLeft(), map.getRight());
            Point point = map.calcPointBelow(new Point(n7, n6));
            if (point == null) {
                point = new Point(n7, n6);
            }
            mplew.write(1);
            mplew.writeInt(Randomizer.rand(type1, type2));
            mplew.writeInt(n5 + i2);
            mplew.writeInt((int) point.getX());
            mplew.writeInt(map.getTop());
            mplew.writeInt((int) point.getX());
            mplew.writeInt(Math.abs(map.getTop() - (int) point.getY()));
            mplew.writeInt(Randomizer.rand(25, 37));
            mplew.writeInt(Randomizer.rand(10, 15));
            mplew.writeInt(0);
            mplew.writeInt(Randomizer.rand(500, 1300));
            mplew.writeInt(0);
            mplew.writeInt(Randomizer.rand(10, 173));
            mplew.writeInt(Randomizer.rand(1, 4));
            mplew.writeInt(Math.abs(map.getTop() - (int) point.getY()));
            mplew.writeInt(0);
        }
        return mplew.getPacket();
    }

    public static byte[] sendMarriedBefore(int n2, int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_PROGRESS.getValue());
        mplew.writeInt(n2);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }

    public static byte[] sendMarriedDone() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.WEDDING_CREMONY_END.getValue());

        return mplew.getPacket();
    }

    public static byte[] showVisitorResult(int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_VISITOR_RESULT.getValue());
        mplew.writeShort(type);
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] updateVisitorKills(int n2, int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_VISITOR_KILL.getValue());
        mplew.writeShort(n2);
        mplew.writeShort(n3);

        return mplew.getPacket();
    }

    public static byte[] showFieldValue(String str, String act) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.FIELD_VALUE.getValue());
        mplew.writeMapleAsciiString(str);
        mplew.writeMapleAsciiString(act);

        return mplew.getPacket();
    }

    public static byte[] DressUpInfoModified(MapleCharacter player) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DRESS_UP_INFO_MODIFIED.getValue());
        PacketHelper.writeDressUpInfo(mplew, player);

        return mplew.getPacket();

    }

    public static byte[] UserRequestChangeMobZoneState(String string, List<Point> list) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANGE_MOBZONESTATE_REQUEST.getValue());
        mplew.writeMapleAsciiString(string);
        mplew.writeInt(0);
        mplew.writeInt(list.size());
        list.stream().filter(Objects::nonNull).forEach(point -> {
            mplew.writeInt(point.x);
            mplew.writeInt(point.y);
        });

        return mplew.getPacket();
    }

    public static final byte[] LobbyTimeAction(final int n, final int n2, final int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOBBY_TIME_ACTION.getValue());
        mplew.writeInt(1);
        mplew.writeInt(n);
        mplew.writeInt(n2);
        mplew.writeInt(0);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }

    public static byte[] SendGiantBossMap(Map<String, String> map) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GIANT_BOSS_MAP.getValue());
        mplew.writeInt(map.size());
        for (Entry<String, String> entry : map.entrySet()) {
            mplew.writeMapleAsciiString(entry.getKey());
            mplew.writeMapleAsciiString(entry.getValue());
        }

        return mplew.getPacket();
    }

    public static byte[] ShowPortal(String string, int n2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_PORTAL.getValue());
        mplew.writeMapleAsciiString(string);
        mplew.writeInt(n2);

        return mplew.getPacket();
    }

    public static byte[] IndividualDeathCountInfo(int n2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.INDIVIDUAL_DEATH_COUNT_INFO.getValue());
        mplew.writeInt(n2);

        return mplew.getPacket();
    }

    public static byte[] cannonSkillResult(int skillid, List<Integer> list) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CANNON_SKILL_RESULT.getValue());
        mplew.write(1);
        mplew.writeInt(skillid);
        mplew.write(1);
        mplew.writeInt(list.size());
        for (Integer n3 : list) {
            mplew.writeInt(n3);
        }

        return mplew.getPacket();
    }

    public static byte[] userBonusAttackRequest(int skillid, int value, List<Integer> list) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.USER_BONUS_ATTACK_REQUEST.getValue());
        mplew.writeInt(skillid);
        mplew.writeInt(list.size());
        mplew.write(list.size() > 0 ? 0 : 1);
        mplew.writeInt(value);
        for (Integer n4 : list) {
            mplew.writeInt(n4);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] SkillFeed() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SKILL_FEED.getValue());
        mplew.writeInt(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] RegisterExtraSkill(MapleCharacter chr, int n2, int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REGISTER_EXTRA_SKILL.getValue());
        mplew.writeInt(chr.getTruePosition().x);
        mplew.writeInt(chr.getTruePosition().y);
        mplew.writeShort(-1);
        mplew.writeInt(n2);
        mplew.writeShort(1);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }

}
