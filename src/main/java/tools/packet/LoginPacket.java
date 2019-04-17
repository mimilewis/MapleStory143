package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleEnumClass;
import client.MaplePartTimeJob;
import configs.ServerConfig;
import constants.JobConstants;
import constants.ServerConstants;
import handling.ServerType;
import handling.login.LoginServer;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.*;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginPacket {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(LoginPacket.class);

    /**
     * 发送一个hello packet
     *
     * @参数 mapleversion为客户端版本
     * @参数 sendiv 发送
     * @参数 recviv 接收
     * @参数 testServer
     * @完毕
     */
    public static byte[] getHello(short mapleVersion, byte[] sendIv, byte[] recvIv, ServerType type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(15 + ServerConfig.LOGIN_MAPLE_PATCH.length() - (type != ServerType.登录服务器 ? 1 : 0));
        mplew.writeShort(mapleVersion + (type == ServerType.聊天服务器 ? 159 : 0));
        mplew.writeMapleAsciiString(ServerConfig.LOGIN_MAPLE_PATCH);
        mplew.write(recvIv);
        mplew.write(sendIv);
        if (type != ServerType.登录服务器) {
            mplew.write(ServerConfig.LOGIN_MAPLE_TYPE);
        } else {
            mplew.writeShort(ServerConfig.LOGIN_MAPLE_TYPE);
        }
        mplew.write(1);

        return mplew.getPacket();
    }

    /**
     * 发送ping 包.
     *
     * @return 数据包
     */
    public static byte[] getPing() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(2);

        mplew.writeShort(SendPacketOpcode.PING.getValue());

        return mplew.getPacket();
    }

    /**
     * 发送一个认证服务器的封包
     *
     * @返回 数据包
     */
    public static byte[] getLoginAUTH() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 1D 00 - 包头
         * 09 00 4D 61 70 4C 6F 67 69 6E 31 - MapLogin1
         * 24 FF EC 77 -2012020516
         */
        mplew.writeShort(SendPacketOpcode.LOGIN_AUTH.getValue());
        int rand = Randomizer.rand(4, 7);
        mplew.writeMapleAsciiString("MapLogin" + (rand == 0 ? "" : rand));
        mplew.writeInt(DateUtil.getTime()); //当前系统时间的年月日时

        return mplew.getPacket();
    }

    public static byte[] addConnection() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREATING_CONNECTION.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 发送同意许可协议的封包
     * @返回 同意的封包
     */
    public static byte[] licenseResult() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LICENSE_RESULT.getValue());
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 发送选择性别的封包
     * @返回 封包
     */
    public static byte[] genderNeeded(MapleClient c) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.CHOOSE_GENDER.getValue());
        mplew.writeMapleAsciiString(c.getAccountName());

        return mplew.getPacket();
    }

    /*
     * 发送选择性别成功封包
     * @返回 封包
     */
    public static byte[] genderChanged(MapleClient c) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        mplew.writeShort(SendPacketOpcode.GENDER_SET.getValue());
        mplew.write(0);
        mplew.writeMapleAsciiString(c.getAccountName());
        mplew.writeMapleAsciiString(String.valueOf(c.getAccID()));

        return mplew.getPacket();
    }

    /**
     * 发送一个登录失败的数据包。
     *
     * @param reason 登陆结果
     * @return 登陆反馈数据包
     * @参数 reason 测井在失败的原因。
     * @返回 登录失败的数据包。
     */
    public static byte[] getLoginFailed(MapleEnumClass.AuthReply reason) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(reason.getCode());
        if (reason.getCode() == 84) {
            mplew.writeLong(PacketHelper.getTime(-2));
        } else if (reason.is(MapleEnumClass.AuthReply.GAME_CONNECTING_ACCOUNT)) { //prolly this
            mplew.writeZeroBytes(5);
        }
        return mplew.getPacket();
    }

    public static byte[] getPermBan(byte reason) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(16);

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(0x02); // 账号已经被禁止
        mplew.write(0);
        mplew.writeShort(reason);
        mplew.write(HexTool.getByteArrayFromHexString("01 01 01 01 00"));

        return mplew.getPacket();
    }

    public static byte[] getTempBan(long timestampTill, byte reason) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(17);
        /*
         * 0x01 = 您的账号违反用户协议已经被永久封停
         */
        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.writeShort(0x02);
        mplew.writeInt(0); // Account is banned
        mplew.write(reason);
        mplew.writeLong(timestampTill); // Tempban 日期处理 -- 64位长, 100 ns的间隔从 1/1/1601.

        return mplew.getPacket();
    }

    /**
     * 发送成功验证和请求数据包
     *
     * @param client 客户端
     * @return 密码请求数据包
     */
    public static byte[] getAuthSuccessRequest(MapleClient client) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_STATUS.getValue());
        mplew.write(0); //0 = 打开全部职业,1 = 关闭全部职业
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.writeInt(client.getAccID());
        mplew.write(client.getGender());
        mplew.writeBool(ServerConfig.LOGIN_DELETECHARACTER);//显示删除角色
        mplew.write(0x80); // Admin byte
        mplew.writeInt(0);
        mplew.write(0);
        mplew.write(0); // 限制游戏行为
        mplew.write(0); // 限制聊天
        mplew.writeShort(3);
        //mplew.writeLong(130171593600000000L);
        mplew.writeLong(DateUtil.getFileTimestamp(System.currentTimeMillis()));//大约是聊天限制时间吧? mplew.writeLong(c.getSessionId());
        mplew.writeZeroBytes(11);
        mplew.write(0);  // 删除角色 封号
        mplew.write(0);
        mplew.writeInt(-1);
        mplew.writeBool(true);//0 = 打开盛大的登陆提示,1 = 关闭登陆提示
        mplew.writeShort(0); // mplew.writeMapleAsciiString(String.valueOf(client.getAccID()));
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.write(1);//0 = 打开未成年提示,1=关闭未成年提示
        mplew.writeShort(1);

        return mplew.getPacket();
    }

    /**
     * 切换角色自动登录
     *
     * @param client
     * @param showWhat
     * @return
     */
    public static byte[] getAuthSuccessRequestX(MapleClient client, boolean showWhat) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.AUTO_LOGIN_STATUS.getValue());
        mplew.write(0); // 未知
        mplew.writeInt(client.getAccID()); // 账号ID
        mplew.write(client.getGender()); // 性别
        mplew.write(0); //显示删除角色
        mplew.write(0x80); // Admin byte - Commands //client.isGm() ? 1 : 0
        mplew.writeZeroBytes(7);
        mplew.writeShort(3);
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis())); //really create date
        mplew.writeZeroBytes(9);
        mplew.writeMapleAsciiString(client.getAccountName());
        mplew.writeShort(0);
        mplew.write(1);
        mplew.write(1);
        // 职业开放状态
        for (int i = 0; i < ServerConstants.JOB_NAMELIST.length; i++) {
            mplew.write(ServerConfig.WORLD_CLOSEJOBS.contains(ServerConstants.JOB_NAMELIST[i]) ? 0 : 1);
            mplew.writeShort(1);
        }
        mplew.write(0);
        mplew.writeInt(-1);
        mplew.writeBool(showWhat);

        return mplew.getPacket();
    }

    public static byte[] deleteCharResponse(int chrId, int state) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DELETE_CHAR_RESPONSE.getValue());
        mplew.writeInt(chrId);
        mplew.write(state);

        return mplew.getPacket();
    }

    public static byte[] secondPwError(byte mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter(3);

        /*
         * 14 - Invalid password
         * 15 - Second password is incorrect
         */
        mplew.writeShort(SendPacketOpcode.SECONDPW_ERROR.getValue());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] enableRecommended(List<Triple<String, Integer, Boolean>> backgrounds) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENABLE_RECOMMENDED.getValue());
        mplew.write(backgrounds.size()); //背景图层数量
        for (Triple<String, Integer, Boolean> background : backgrounds) {
            mplew.writeMapleAsciiString(background.getLeft());
            mplew.write(background.getRight() ? Randomizer.nextInt(2) : background.getMid());
        }

        return mplew.getPacket();
    }

    public static byte[] sendRecommended(int world, String message) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SEND_RECOMMENDED.getValue());
        mplew.write(message != null ? 1 : 0); //amount of messages
        if (message != null) {
            mplew.writeInt(world);
            mplew.writeMapleAsciiString(message);
        }

        return mplew.getPacket();
    }

    /**
     * 发送数据包的详细介绍了服务器和在线人数
     *
     * @参数 serverId - 服务器ID
     * @参数 channelLoad 负荷的频道-1200似乎是最大
     * @返回 服务器信息包。
     */
    public static byte[] getServerList(int serverId, Map<Integer, Integer> channelLoad) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * Recv SERVERLIST [0009] (201)
         * 09 00 - 包头
         * 78 00 - 服务器ID
         * 06 00 57 6F 72 6C 64 30 - 服务器名字
         * 00 - 服务器状态
         * 00 00 - 服务器公告牌信息
         * 64 00
         * 64 00
         * 0A - 频道总数
         * 90 01 00 00 - 未知
         * ----------------------------------------------------------------
         * 08 00 57 6F 72 6C 64 30 2D 31 - 频道名字 = 服务器名字 - 频道编号
         * 6F 01 00 00 - 频道连接人数
         * 78 - 服务器ID
         * 00 00 - 频道编号
         * 08 00 57 6F 72 6C 64 30 2D 32 9F 00 00 00 78 01 00 - World0-2
         * 08 00 57 6F 72 6C 64 30 2D 33 9E 00 00 00 78 02 00 - World0-3
         * 08 00 57 6F 72 6C 64 30 2D 34 A2 00 00 00 78 03 00 - World0-4
         * 08 00 57 6F 72 6C 64 30 2D 35 9A 00 00 00 78 04 00 - World0-5
         * 08 00 57 6F 72 6C 64 30 2D 36 98 00 00 00 78 05 00 - World0-6
         * 08 00 57 6F 72 6C 64 30 2D 37 A9 00 00 00 78 06 00 - World0-7
         * 08 00 57 6F 72 6C 64 30 2D 38 9F 00 00 00 78 07 00 - World0-8
         * 08 00 57 6F 72 6C 64 30 2D 39 A4 00 00 00 78 08 00 - World0-9
         * 09 00 57 6F 72 6C 64 30 2D 31 30 9C 00 00 00 78 09 00 - World0-10
         * ------------------------------------------------------------------
         * 00 00 00 00 00 00
         * ..x...World0...d.d..?....World0-1o...x....World0-2?..x....World0-3?..x....World0-4?..x....World0-5?..x....World0-6?..x....World0-7?..x....World0-8?..x....World0-9?..x....World0-10?..x........
         */
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.writeShort(ServerConstants.MapleServerName.getValueByOrdinal(ServerConfig.LOGIN_SERVERFLAG)); // 0 = Aquilla, 1 = bootes, 2 = cass, 3 = delphinus
        String worldName = LoginServer.getTrueServerName(); //remove the SEA
        mplew.writeMapleAsciiString(worldName); // 服务器名字
        mplew.write(ServerConfig.LOGIN_SERVERSTATUS); // 服务器状态  0 无 1活动 2新的 3hot
        mplew.writeMapleAsciiString(LoginServer.getEventMessage()); // 服务器公告牌信息
        mplew.writeShort(100);
        mplew.writeShort(100);
        int lastChannel = 1;
        Set<Integer> channels = channelLoad.keySet();
        for (int i = 30; i > 0; i--) {
            if (channels.contains(i)) {
                lastChannel = i;
                break;
            }
        }
        mplew.write(lastChannel); // 频道总数
        mplew.writeInt(500); // [90 01 00 00]
        int load;
        for (int i = 1; i <= lastChannel; i++) {
            if (channels.contains(i)) {
                load = channelLoad.get(i);
            } else {
                load = ServerConfig.LOGIN_USERLIMIT;
            }
            mplew.writeMapleAsciiString(worldName + "-" + i); // 频道名字 = 服务器名字 - 频道编号
            mplew.writeInt(load + ServerConfig.LOGIN_DEFAULTUSERLIMIT); // 频道连接人数
            mplew.write(serverId); // 服务器ID
            mplew.writeShort(i - 1); // 频道编号
        }
        mplew.writeShort(0); //size: (short x, short y, string msg)
//        mplew.writeShort(LoginServer.getBalloons().size());
//        for (MapleBalloon balloon : LoginServer.getBalloons()) {
//            mplew.writeShort(balloon.nX);
//            mplew.writeShort(balloon.nY);
//            mplew.writeMapleAsciiString(balloon.sMessage);
//        }
        mplew.writeInt(0);
        mplew.write(0);

        return mplew.getPacket();
    }

    /**
     * 发送数据包说服务器列表结束
     *
     * @return 结束服务器列表的数据包
     */
    public static byte[] getEndOfServerList() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * Recv SERVERLIST [0009] (4)
         * 09 00
         * FF FF
         * ..?
         */
        mplew.writeShort(SendPacketOpcode.SERVERLIST.getValue());
        mplew.writeShort(-1);
        mplew.writeLong(System.currentTimeMillis());
        mplew.writeShort(0); //V.103新增

        return mplew.getPacket();
    }

    public static byte[] getLoginWelcome() {
        log.trace("调用");
        return MaplePacketCreator.spawnFlags(null);
    }

    /**
     * 发送数据包详细介绍了服务器的状态信息。
     *
     * @param status 服务器状态。
     * @return 服务器状态数据包。
     */
    public static byte[] getServerStatus(int status) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        /*
         * 可能的值 status:
         * 0 - 没有消息
         * 1 - 当前世界连接数量较多，这可能会导致登录游戏时有些困难。
         * 2 - 当前世界上的连接已到达最高限制。请选择别的服务器进行游戏或稍后再试。
         */
        mplew.writeShort(SendPacketOpcode.SERVERSTATUS.getValue());
        mplew.write(status);

        return mplew.getPacket();
    }

    public static byte[] EventCheck() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.EVENT_CHECK.getValue());
        mplew.write(HexTool.getByteArrayFromHexString("00 05 00 00 10 40 00 46 E5 58 00 57 F5 98 00 04 00 00 00 5F F5 98 00 04 00 00 00 6C F5 98 00 94 CA 07 00 D0 C3 A0 00 1C 16 01 00"));

        return mplew.getPacket();

    }

    public static byte[] getChannelSelected() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHANNEL_SELECTED.getValue());
        mplew.writeInt(3);

        return mplew.getPacket();
    }

    /**
     * Gets a packet with a list of characters.
     *
     * @param secondpw
     * @param chars
     * @param charslots
     * @return The character list packet.
     */
    public static byte[] getCharList(String secondpw, List<MapleCharacter> chars, int charslots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHARLIST.getValue());
        mplew.write(0); //未知
        mplew.writeMapleAsciiString("normal");
        mplew.writeInt(0);
        mplew.write(0);
        int n3 = 0;
        for (int i = 0; i <= n3; i++) {
            mplew.writeInt(n3);
            mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis()));
        }
        mplew.write(0);
        mplew.writeInt(chars.size()); //目前角色个数
        chars.forEach(chr -> mplew.writeInt(chr.getId()));
        mplew.write(chars.size());
        chars.forEach(chr -> addCharEntry(mplew, chr, false)); //!chr.isGM() && chr.getLevel() >= 30);
        //mplew.write(secondpw != null && secondpw.length() > 0 ? 1 : (secondpw != null && secondpw.length() <= 0 && GameConstants.GMS ? 2 : 0)); // second pw request
        mplew.writeShort(0);
        mplew.writeLong(charslots); //帐号当前可创建角色的总数
        mplew.writeInt(-1); //V.106新增
        mplew.writeReversedLong(PacketHelper.getTime(System.currentTimeMillis()));
        mplew.write(0);     // 预约改名
        mplew.write(0);     // = -1 收集PC运行数据
        mplew.write(0);
        mplew.writeInt(0);
        mplew.writeInt(121022127);
        mplew.write(1);
        mplew.write(1);
        // 职业开放状态
        for (int i = 0; i < ServerConstants.JOB_NAMELIST.length; i++) {
            mplew.write(ServerConfig.WORLD_CLOSEJOBS.contains(ServerConstants.JOB_NAMELIST[i]) ? 0 : 1);
            mplew.writeShort(1);
        }

        return mplew.getPacket();
    }

    public static byte[] getCreatCharAuth(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CREAT_CHAR_AUTH.getValue());
        mplew.write(0x04);

        return mplew.getPacket();
    }

    public static byte[] addNewCharEntry(MapleCharacter chr, boolean worked) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ADD_NEW_CHAR_ENTRY.getValue());
        mplew.write(worked ? 0 : 1);
        addCharEntry(mplew, chr, false);

        return mplew.getPacket();
    }

    public static byte[] charNameResponse(String charname, boolean nameUsed) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(nameUsed ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] charNameResponse(String charname, byte type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CHAR_NAME_RESPONSE.getValue());
        mplew.writeMapleAsciiString(charname);
        mplew.write(type); //0 = 可创建 1 = 已被使用 2 = 名字无法使用 3 = 因未知原因失败

        return mplew.getPacket();
    }

    private static void addCharEntry(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, boolean ranking) {
        PacketHelper.addCharStats(mplew, chr);
        mplew.writeInt(1);
        PacketHelper.addCharLook(mplew, chr, true, chr.isZeroSecondLook());
        if (JobConstants.is神之子(chr.getJob())) {
            PacketHelper.addCharLook(mplew, chr, true, !chr.isZeroSecondLook());
        }
        mplew.write(ranking ? 1 : 0);
        if (ranking) {
            mplew.writeInt(chr.getRank());
            mplew.writeInt(chr.getRankMove());
            mplew.writeInt(chr.getJobRank());
            mplew.writeInt(chr.getJobRankMove());
        }
    }

    public static byte[] updatePartTimeJob(MaplePartTimeJob partTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PART_TIME.getValue());
        mplew.writeInt(partTime.getCharacterId());
        mplew.write(0);
        PacketHelper.addPartTimeJob(mplew, partTime);
        return mplew.getPacket();
    }

    /*
     * 显示角色卡的数量上限
     * 默认是3 最高为6
     */
    public static byte[] showCharCards(int cards) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHAR_CARDS.getValue());
        mplew.writeInt(cards);

        return mplew.getPacket();
    }

    /*
     * 显示角色点卷信息
     */
    public static byte[] ShowAccCash(int ACash, int mPoints) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ACC_CASH.getValue());
        mplew.writeInt(ACash);
        mplew.writeInt(mPoints);

        return mplew.getPacket();
    }

    public static byte[] changePlayerKey(String key) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOGIN_AUTHKEY.getValue());
        mplew.writeMapleAsciiString(key);

        return mplew.getPacket();
    }

}
