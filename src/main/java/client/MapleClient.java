package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.login.handler.AutoRegister;
import handling.netty.MaplePacketDecoder;
import handling.world.*;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuildCharacter;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.item.ItemActionManager;
import scripting.item.ItemScriptManager;
import scripting.npc.NPCConversationManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestActionManager;
import scripting.quest.QuestScriptManager;
import server.CharacterCardFactory;
import server.Timer.PingTimer;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import server.shops.IMaplePlayerShop;
import tools.*;
import tools.packet.LoginPacket;

import javax.script.ScriptEngine;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MapleClient implements Serializable {

    public static final byte LOGIN_NOTLOGGEDIN = 0, LOGIN_SERVER_TRANSITION = 1, LOGIN_LOGGEDIN = 2, CHANGE_CHANNEL = 3, ENTERING_PIN = 4, PIN_CORRECT = 5, LOGIN_CS_LOGGEDIN = 6;
    public static final AttributeKey<MapleClient> CLIENT_KEY = AttributeKey.newInstance("Client");
    private static final Logger log = LogManager.getLogger(MapleClient.class);
    private static final long serialVersionUID = 9179541993413738569L;
    private final static Lock login_mutex = new ReentrantLock(true);
    private final transient Lock mutex = new ReentrantLock(true);
    private final transient Lock npc_mutex = new ReentrantLock();
    private final transient long lastChatServerPing = 0;
    private final transient List<Integer> allowedChar = new LinkedList<>();
    private final transient Map<String, ScriptEngine> engines = new HashMap<>();
    private final Map<Integer, Pair<Short, Short>> charInfo = new LinkedHashMap<>();
    private final List<String> proesslist = new ArrayList<>();
    public transient short loginAttempt = 0;
    private transient MapleAESOFB send, receive;
    private transient Channel session;
    private MapleCharacter player;
    private int channel = 1, accId = -1, world, birthday;
    private int charslots = ServerConfig.CHANNEL_PLAYER_MAXCHARACTERS; //可创建角色的数量
    private int cardslots = 3; //角色卡的数量
    private boolean loggedIn = false, serverTransition = false;
    private transient Calendar tempban = null;
    private String accountName;
    private transient long lastPong = 0;
    private transient long lastPing = 0;
    private transient long lastChatServerPong = 0;
    private boolean monitored = false, receiving = true;
    private int gm;
    private byte greason = 1, gender = -1;
    private transient String mac = "00-00-00-00-00-00";
    private transient List<String> maclist = new LinkedList<>();
    private transient ScheduledFuture<?> idleTask = null;
    private transient String secondPassword, salt2, tempIP = ""; // To be used only on login
    private long lastNpcClick = 0, sessionId;
    private byte loginattempt = 0;
    private DebugUI debugWindow; //调试封包窗口
    private Triple<String, String, Boolean> tempinfo = null;

    public MapleClient() {

    }

    public MapleClient(MapleAESOFB send, MapleAESOFB receive, Channel session) {
        this.send = send;
        this.receive = receive;
        this.session = session;
    }

    public static byte unban(String charname) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, charname);
                int accid;
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return -1;
                    }
                    accid = rs.getInt(1);
                }

                try (PreparedStatement psu = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE id = ?")) {
                    psu.setInt(1, accid);
                    psu.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Error while unbanning", e);
            return -2;
        }
        return 0;
    }

    public static String getLogMessage(MapleClient cfor, String message) {
        return getLogMessage(cfor, message, new Object[0]);
    }

    public static String getLogMessage(MapleCharacter cfor, String message) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message);
    }

    public static String getLogMessage(MapleCharacter cfor, String message, Object... parms) {
        return getLogMessage(cfor == null ? null : cfor.getClient(), message, parms);
    }

    public static String getLogMessage(MapleClient cfor, String message, Object... parms) {
        StringBuilder builder = new StringBuilder();
        if (cfor != null) {
            if (cfor.getPlayer() != null) {
                builder.append("<");
                builder.append(MapleCharacterUtil.makeMapleReadable(cfor.getPlayer().getName()));
                builder.append(" (角色ID: ");
                builder.append(cfor.getPlayer().getId());
                builder.append(")> ");
            }
            if (cfor.getAccountName() != null) {
                builder.append("(账号: ");
                builder.append(cfor.getAccountName());
                builder.append(") ");
            }
        }
        builder.append(message);
        int start;
        for (Object parm : parms) {
            start = builder.indexOf("{}");
            builder.replace(start, start + 2, parm.toString());
        }
        return builder.toString();
    }

    public static int findAccIdForCharacterName(String charName) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, charName);
                ResultSet rs = ps.executeQuery();
                int ret = -1;
                if (rs.next()) {
                    ret = rs.getInt("accountid");
                }
                return ret;
            }
        } catch (SQLException e) {
            log.error("findAccIdForCharacterName SQL error", e);
        }
        return -1;
    }

    public static byte unbanIPMacs(String charname) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charname);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int accid = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            String sessionIP = rs.getString("sessionIP");
            String macs = rs.getString("macs");
            rs.close();
            ps.close();
            byte ret = 0;
            if (sessionIP != null) {
                PreparedStatement psa = con.prepareStatement("DELETE FROM ipbans WHERE ip LIKE ?");
                psa.setString(1, sessionIP);
                psa.execute();
                psa.close();
                ret++;
            }
            if (macs != null) {
                String[] macz = macs.split(", ");
                for (String mac : macz) {
                    if (!mac.equals("")) {
                        PreparedStatement psa = con.prepareStatement("DELETE FROM macbans WHERE mac = ?");
                        psa.setString(1, mac);
                        psa.execute();
                        psa.close();
                    }
                }
                ret++;
            }
            return ret;
        } catch (SQLException e) {
            log.error("Error while unbanning", e);
            return -2;
        }
    }

    public static byte unHellban(String charname) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            ps.setString(1, charname);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int accid = rs.getInt(1);
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, accid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            String sessionIP = rs.getString("sessionIP");
            String email = rs.getString("email");
            rs.close();
            ps.close();
            ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE email = ?" + (sessionIP == null ? "" : " OR sessionIP = ?"));
            ps.setString(1, email);
            if (sessionIP != null) {
                ps.setString(2, sessionIP);
            }
            ps.execute();
            ps.close();
            return 0;
        } catch (SQLException e) {
            log.error("Error while unbanning", e);
            return -2;
        }
    }

    public static String getAccInfo(String accname, boolean admin) {
        StringBuilder ret = new StringBuilder("帐号 " + accname + " 的信息 -");
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, accname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int banned = rs.getInt("banned");
                ret.append(" 状态: ");
                ret.append(banned > 0 ? "已封" : "正常");
                ret.append(" 封号理由: ");
                ret.append(banned > 0 ? rs.getString("banreason") : "(无描述)");
                if (admin) {
                    ret.append(" 点卷: ");
                    ret.append(rs.getInt("ACash"));
                    ret.append(" 抵用卷: ");
                    ret.append(rs.getInt("mPoints"));
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            log.error("获取玩家封号理由信息出错", ex);
        }
        return ret.toString();
    }

    public static String getAccInfoByName(String charname, boolean admin) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?")) {
                ps.setString(1, charname);
                int accid;
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return null;
                    }
                    accid = rs.getInt(1);
                }
                try (PreparedStatement psu = con.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                    psu.setInt(1, accid);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            return null;
                        }

                        StringBuilder ret = new StringBuilder("玩家 " + charname + " 的帐号信息 -");
                        int banned = rs.getInt("banned");
                        if (admin) {
                            ret.append(" 账号: ");
                            ret.append(rs.getString("name"));
                        }
                        ret.append(" 状态: ");
                        ret.append(banned > 0 ? "已封" : "正常");
                        ret.append(" 封号理由: ");
                        ret.append(banned > 0 ? rs.getString("banreason") : "(无描述)");
                        return ret.toString();
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("获取玩家封号理由信息出错", ex);
            return null;
        }
    }

    public synchronized MapleAESOFB getReceiveCrypto() {
        return receive;
    }

    public synchronized MapleAESOFB getSendCrypto() {
        return send;
    }

    public final void announce(final byte[] array) {
        session.writeAndFlush(array);
    }

    public final void sendEnableActions() {
        session.writeAndFlush(MaplePacketCreator.enableActions());
    }

    public synchronized Channel getSession() {
        return session;
    }

    public long getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public void StartWindow() {
        if (debugWindow != null) {
            debugWindow.setVisible(false);
            debugWindow = null;
        }
        debugWindow = new DebugUI();
        debugWindow.setVisible(true);
        debugWindow.setC(this);
    }

    public Lock getLock() {
        return mutex;
    }

    public Lock getNPCLock() {
        return npc_mutex;
    }

    public MapleCharacter getPlayer() {
        return player;
    }

    public void setPlayer(MapleCharacter player) {
        this.player = player;
    }

    public void createdChar(int id) {
        allowedChar.add(id);
    }

    public boolean login_Auth(int id) {
        return allowedChar.contains(id);
    }

    public List<MapleCharacter> loadCharacters(int serverId) {
        List<MapleCharacter> chars = new LinkedList<>();
        Map<Integer, CardData> cards = CharacterCardFactory.getInstance().loadCharacterCards(accId, serverId);
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            MapleCharacter chr = MapleCharacter.loadCharFromDB(cni.id, this, false, cards);
            chars.add(chr);
            charInfo.put(chr.getId(), new Pair<>(chr.getLevel(), chr.getJob()));
            if (!login_Auth(chr.getId())) {
                allowedChar.add(chr.getId());
            }
        }
        return chars;
    }

    public void updateCharacterCards(Map<Integer, Integer> cids) {
        if (charInfo.isEmpty()) { //没有角色
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM `character_cards` WHERE `accid` = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }

            try (PreparedStatement psu = con.prepareStatement("INSERT INTO `character_cards` (accid, worldid, characterid, position) VALUES (?, ?, ?, ?)")) {
                for (Entry<Integer, Integer> ii : cids.entrySet()) {
                    Pair<Short, Short> info = charInfo.get(ii.getValue());
                    if (info == null || ii.getValue() == 0 || !CharacterCardFactory.getInstance().canHaveCard(info.getLeft(), info.getRight())) {
                        continue;
                    }
                    psu.setInt(1, accId);
                    psu.setInt(2, world);
                    psu.setInt(3, ii.getValue());
                    psu.setInt(4, ii.getKey());
                    psu.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("Failed to update character cards. Reason:", e);
        }
    }

    public int getCharacterJob(int cid) {
        if (charInfo.containsKey(cid)) {
            return charInfo.get(cid).getRight();
        }
        return -1;
    }

    public boolean canMakeCharacter(int serverId) {
        return loadCharactersSize(serverId) < getAccCharSlots();
    }

    public List<String> loadCharacterNames(int serverId) {
        List<String> chars = new LinkedList<>();
        for (CharNameAndId cni : loadCharactersInternal(serverId)) {
            chars.add(cni.name);
        }
        return chars;
    }

    private List<CharNameAndId> loadCharactersInternal(int serverId) {
        List<CharNameAndId> chars = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id, name FROM characters WHERE accountid = ? AND world = ? ORDER BY position, id")) {
                ps.setInt(1, accId);
                ps.setInt(2, serverId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        chars.add(new CharNameAndId(rs.getString("name"), rs.getInt("id")));
                        LoginServer.getLoginAuth(rs.getInt("id"));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("error loading characters internal", e);
        }
        return chars;
    }

    /*
     * 获取游戏帐号下已经创建的角色个数
     */
    public int loadCharactersSize(int serverId) {
        int chars = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT count(*) FROM characters WHERE accountid = ? AND world = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, serverId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        chars = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("error loading characters size", e);
        }
        return chars;
    }

    public boolean isLoggedIn() {
        return loggedIn && accId >= 0;
    }

    private Calendar getTempBanCalendar(ResultSet rs) throws SQLException {
        Calendar lTempban = Calendar.getInstance();
        if (rs.getLong("tempban") == 0) { // basically if timestamp in db is 0000-00-00
            lTempban.setTimeInMillis(0);
            return lTempban;
        }
        Calendar today = Calendar.getInstance();
        lTempban.setTimeInMillis(rs.getTimestamp("tempban").getTime());
        if (today.getTimeInMillis() < lTempban.getTimeInMillis()) {
            return lTempban;
        }
        lTempban.setTimeInMillis(0);
        return lTempban;
    }

    public Calendar getTempBanCalendar() {
        return tempban;
    }

    public byte getBanReason() {
        return greason;
    }

    public boolean hasBannedIP() {
        boolean ret = false;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ipbans WHERE ? LIKE CONCAT(ip, '%')")) {
                ps.setString(1, getSessionIPAddress());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("Error checking ip bans", ex);
        }
        return ret;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        this.mac = macData;
    }

    public boolean hasBannedMac() {
        if (mac.equalsIgnoreCase("00-00-00-00-00-00") || mac.length() != 17) {
            return false;
        }
        boolean ret = false;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM macbans WHERE mac = ?")) {
                ps.setString(1, mac);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        ret = true;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error checking mac bans", e);
        }
        return ret;
    }

    public void banMacs() {
        banMacs(mac);
    }

    public void banMacs(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO macbans (mac) VALUES (?)")) {
                ps.setString(1, macData);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error banning MACs", e);
        }
    }

    public void updateMacs() {
        updateMacs(mac);
    }

    public void updateMacs(String macData) {
        if (macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17) {
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET macs = ? WHERE id = ?")) {
                ps.setString(1, macData);
                ps.setInt(2, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error saving MACs", e);
        }
    }

    /**
     * Returns 0 on success, a state to be used for
     * {@link LoginPacket#getLoginFailed(MapleEnumClass.AuthReply)} otherwise.
     *
     * @return The state of the login.
     */
    public int finishLogin() {
        login_mutex.lock();
        try {
            byte state = getLoginState();
            if (state > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                loggedIn = false;
                return 7;
            }
            updateLoginState(MapleClient.LOGIN_LOGGEDIN, getSessionIPAddress());
        } finally {
            login_mutex.unlock();
        }
        return 0;
    }

    public void clearInformation() {
        accountName = null;
        accId = -1;
        secondPassword = null;
        salt2 = null;
        gm = 0;
        loggedIn = false;
        mac = "00-00-00-00-00-00";
        maclist.clear();
        this.player = null;
    }

    public int changePassword(String oldpwd, String newpwd) {
        int ret = -1;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?");
            ps.setString(1, getAccountName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                boolean updatePassword = false;
                String passhash = rs.getString("password");
                String salt = rs.getString("salt");
                if (passhash == null || passhash.isEmpty()) {
                    ret = -1;
                } else if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(oldpwd, passhash)) {
                    ret = 0;
                    updatePassword = true;
                } else if (oldpwd.equals(passhash)) {
                    ret = 0;
                    updatePassword = true;
                } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, oldpwd)) {
                    ret = 0;
                    updatePassword = true;
                } else if (LoginCrypto.checkSaltedSha512Hash(passhash, oldpwd, salt)) {
                    ret = 0;
                    updatePassword = true;
                } else {
                    ret = -1;
                }
                if (updatePassword) {
                    try (PreparedStatement pss = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?")) {
                        String newSalt = LoginCrypto.makeSalt();
                        pss.setString(1, LoginCrypto.makeSaltedSha512Hash(newpwd, newSalt));
                        pss.setString(2, newSalt);
                        pss.setInt(3, accId);
                        pss.executeUpdate();
                    }
                }
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            log.error("修改游戏帐号密码出现错误.\r\n", e);
        }
        return ret;
    }

    public MapleEnumClass.AuthReply login(String login, String pwd, boolean ipMacBanned, boolean useKey) {
        MapleEnumClass.AuthReply loginok = MapleEnumClass.AuthReply.GAME_ACCOUNT_NOT_LANDED;
        if (!useKey) {
            loginattempt++;
            if (loginattempt > 6) {
                log.info("账号[" + login + "]登录次数达到6次还未登录游戏，服务端断开连接.");
                getSession().close();
            }
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE name = ?")) {
                ps.setString(1, login);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int banned = rs.getInt("banned");
                        String passhash = rs.getString("password");
                        String salt = rs.getString("salt");
                        String oldSession = rs.getString("SessionIP");

                        accountName = login;
                        accId = rs.getInt("id");
                        secondPassword = rs.getString("2ndpassword");
                        salt2 = rs.getString("salt2");
                        gm = rs.getInt("gm");
                        greason = rs.getByte("greason");
                        tempban = getTempBanCalendar(rs);
                        gender = rs.getByte("gender");

                        maclist = new LinkedList<>();
                        String macStrs = rs.getString("maclist");
                        if (macStrs != null) {
                            String[] macData = macStrs.split(",");
                            for (String macData1 : macData) {
                                if (macData1.length() == 17) {
                                    maclist.add(macData1);
                                }
                            }
                        }

                        if (secondPassword != null && salt2 != null) {
                            secondPassword = LoginCrypto.rand_r(secondPassword);
                        }
                        ps.close();

                        if (useKey) {
                            loginok = MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL;
                        } else {
                            if (banned > 0 && gm == 0) {
                                loginok = MapleEnumClass.AuthReply.GAME_ACCOUNT_DELETE;
                            } else {
                                if (banned == -1) {
                                    unban();
                                }
                                byte loginstate = getLoginState();
                                if (loginstate > MapleClient.LOGIN_NOTLOGGEDIN) { // already loggedin
                                    loggedIn = false;
                                    loginok = MapleEnumClass.AuthReply.GAME_CONNECTING_ACCOUNT;
                                } else {
                                    boolean updatePasswordHash = false;
                                    // Check if the passwords are correct here. :B
                                    if (passhash == null || passhash.isEmpty()) {
                                        //match by sessionIP
                                        if (oldSession != null && !oldSession.isEmpty()) {
                                            loggedIn = getSessionIPAddress().equals(oldSession);
                                            loginok = loggedIn ? MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL : MapleEnumClass.AuthReply.GAME_PASSWORD_ERROR;
                                            updatePasswordHash = loggedIn;
                                        } else {
                                            loginok = MapleEnumClass.AuthReply.GAME_PASSWORD_ERROR;
                                            loggedIn = false;
                                        }
                                    } else if (LoginCryptoLegacy.isLegacyPassword(passhash) && LoginCryptoLegacy.checkPassword(pwd, passhash)) {
                                        // Check if a password upgrade is needed.
                                        loginok = MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL;
                                        updatePasswordHash = true;
                                    } else if (pwd.equals(passhash)) {
                                        loginok = MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL;
                                        updatePasswordHash = true;
                                    } else if (salt == null && LoginCrypto.checkSha1Hash(passhash, pwd)) {
                                        loginok = MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL;
                                        updatePasswordHash = true;
                                    } else if (LoginCrypto.checkSaltedSha512Hash(passhash, pwd, salt)) {
                                        loginok = MapleEnumClass.AuthReply.GAME_LOGIN_SUCCESSFUL;
                                    } else {
                                        loggedIn = false;
                                        loginok = MapleEnumClass.AuthReply.GAME_PASSWORD_ERROR;
                                    }
                                    if (updatePasswordHash && ServerConfig.LOGIN_USESHA1HASH) {
                                        try (PreparedStatement pss = con.prepareStatement("UPDATE `accounts` SET `password` = ?, `salt` = ? WHERE id = ?")) {
                                            String newSalt = LoginCrypto.makeSalt();
                                            pss.setString(1, LoginCrypto.makeSaltedSha512Hash(pwd, newSalt));
                                            pss.setString(2, newSalt);
                                            pss.setInt(3, accId);
                                            pss.executeUpdate();
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (AutoRegister.createAccount(login, pwd)) {
                            loginok = login(login, pwd, ipMacBanned, useKey);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("登录游戏帐号出现错误. 账号: " + login + " \r\n", e);
        }
        return loginok;
    }

    public boolean CheckSecondPassword(String in) {
        boolean allow = false;
        boolean updatePasswordHash = false;
        // Check if the passwords are correct here. :B
        if (LoginCryptoLegacy.isLegacyPassword(secondPassword) && LoginCryptoLegacy.checkPassword(in, secondPassword)) {
            // Check if a password upgrade is needed.
            allow = true;
            updatePasswordHash = true;
        } else if (salt2 == null && LoginCrypto.checkSha1Hash(secondPassword, in)) {
            allow = true;
            updatePasswordHash = true;
        } else if (LoginCrypto.checkSaltedSha512Hash(secondPassword, in, salt2)) {
            allow = true;
        }
        if (updatePasswordHash) {
            return updateSecondPassword();
        }
        return allow;
    }

    private void unban() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = 0, banreason = '' WHERE id = ?")) {
                ps.setInt(1, accId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error while unbanning", e);
        }
    }

    public int getAccID() {
        return this.accId;
    }

    public void setAccID(int id) {
        this.accId = id;
    }

    public void updateLoginState(int newstate) {
        updateLoginState(newstate, getSessionIPAddress());
    }

    public void updateLoginState(int newstate, String SessionID) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = ?, SessionIP = ?, lastlogin = CURRENT_TIMESTAMP() WHERE id = ?")) {
                ps.setInt(1, newstate);
                ps.setString(2, SessionID);
                ps.setInt(3, getAccID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("Error updating login state", e);
        } finally {
            if (newstate == MapleClient.LOGIN_NOTLOGGEDIN) {
                loggedIn = false;
                serverTransition = false;
            } else {
                serverTransition = (newstate == MapleClient.LOGIN_SERVER_TRANSITION || newstate == MapleClient.CHANGE_CHANNEL);
                loggedIn = !serverTransition;
            }
        }
    }

    public boolean updateSecondPassword() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `2ndpassword` = ?, `salt2` = ? WHERE id = ?")) {
                String newSalt = LoginCrypto.makeSalt();
                ps.setString(1, LoginCrypto.rand_s(LoginCrypto.makeSaltedSha512Hash(secondPassword, newSalt)));
                ps.setString(2, newSalt);
                ps.setInt(3, accId);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public byte getLoginState() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT loggedin, lastlogin, banned, `birthday` + 0 AS `bday` FROM accounts WHERE id = ?")) {
                ps.setInt(1, getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt("banned") > 0) {
                        session.close();
                        log.error("Account doesn't exist or is banned");
                        return MapleClient.LOGIN_NOTLOGGEDIN;
                    }
                    birthday = rs.getInt("bday");
                    byte state = rs.getByte("loggedin");

                /*
                 * 如果是在更换频道或者登录过渡
                 * 检测 lastlogin 的时间加 20秒 小于当前系统的时间
                 * 就更新登录状态为 0
                 */
                    if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
                        if (rs.getTimestamp("lastlogin").getTime() + 20000 < System.currentTimeMillis()) { // connecting to chanserver timeout
                            state = MapleClient.LOGIN_NOTLOGGEDIN;
                            updateLoginState(state, getSessionIPAddress());
                        }
                    }
                    loggedIn = state == MapleClient.LOGIN_LOGGEDIN;
                    return state;
                }
            }
        } catch (SQLException e) {
            loggedIn = false;
            log.error("error getting login state", e);
            return MapleClient.LOGIN_NOTLOGGEDIN;
        }
    }

    public boolean checkBirthDate(int date) {
        return birthday == date;
    }

    public void removalTask(boolean shutdown) {
        try {
            player.cancelAllBuffs_();
            player.cancelAllDebuffs();
            if (player.getMarriageId() > 0) {
                MapleQuestStatus stat1 = player.getQuestNoAdd(MapleQuest.getInstance(160001));
                MapleQuestStatus stat2 = player.getQuestNoAdd(MapleQuest.getInstance(160002));
                if (stat1 != null && stat1.getCustomData() != null && (stat1.getCustomData().equals("2_") || stat1.getCustomData().equals("2"))) {
                    //dc in process of marriage
                    if (stat2 != null && stat2.getCustomData() != null) {
                        stat2.setCustomData("0");
                    }
                    stat1.setCustomData("3");
                }
            }
            if (player.getMapId() == GameConstants.JAIL && !player.isIntern()) {
                MapleQuestStatus stat1 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME));
                MapleQuestStatus stat2 = player.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST));
                if (stat1.getCustomData() == null) {
                    stat1.setCustomData(String.valueOf(System.currentTimeMillis()));
                } else if (stat2.getCustomData() == null) {
                    stat2.setCustomData("0"); //seconds of jail
                } else { //previous seconds - elapsed seconds
                    int seconds = Integer.parseInt(stat2.getCustomData()) - (int) ((System.currentTimeMillis() - Long.parseLong(stat1.getCustomData())) / 1000);
                    if (seconds < 0) {
                        seconds = 0;
                    }
                    stat2.setCustomData(String.valueOf(seconds));
                }
            }
            player.changeRemoval(true);
            if (player.getEventInstance() != null) {
                player.getEventInstance().playerDisconnected(player, player.getId());
            }
            IMaplePlayerShop shop = player.getPlayerShop();
            if (shop != null) {
                shop.removeVisitor(player);
                if (shop.isOwner(player)) {
                    if (shop.getShopType() == 1 && shop.isAvailable() && !shutdown) {
                        shop.setOpen(true);
                    } else {
                        shop.closeShop(true, !shutdown);
                    }
                }
            }
            player.setMessenger(null);
            if (player.getAntiMacro().inProgress()) {
                player.getAntiMacro().end();
            }
            if (player.getMap() != null) {
                if (shutdown || (getChannelServer() != null && getChannelServer().isShutdown())) {
                    int questID = -1;
                    switch (player.getMapId()) {
                        case 240060200: //生命之穴 - 暗黑龙王洞穴
                            questID = 160100;
                            break;
                        case 240060201: //生命之穴 - 进阶暗黑龙王洞穴
                            questID = 160103;
                            break;
                        case 280030100: //最后的任务 - 扎昆的祭台
                        case 280030000: //神秘岛 - 扎昆的祭台
                            questID = 160101;
                            break;
                        case 280030001: //最后的任务 - 进阶扎昆的祭台
                            questID = 160102;
                            break;
                        case 270050100: //神殿的深处 - 神的黄昏
                            questID = 160104;
                            break;
                        case 105100300: //蝙蝠怪神殿 - 蝙蝠怪的墓地
                        case 105100400: //蝙蝠怪神殿 - 蝙蝠怪的墓地
                            questID = 160106;
                            break;
                        case 211070000: //狮子王之城 - 接见室走廊
                        case 211070100: //狮子王之城 - 接见室
                        case 211070101: //狮子王之城 - 空中监狱
                        case 211070110: //狮子王之城 - 复活塔楼
                            questID = 160107;
                            break;
                        case 551030200: //马来西亚 - 阴森世界
                            questID = 160108;
                            break;
                        case 271040100: //骑士团要塞 - 希纳斯的殿堂
                            questID = 160109;
                            break;
                    }
                    if (questID > 0) {
                        player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData("0"); //reset the time.
                    }
                } else if (player.isAlive()) {
                    switch (player.getMapId()) {
                        case 541010100: //新加坡 - 轮机舱
                        case 541020800: //新加坡 - 千年树精王遗迹Ⅱ
                        case 220080001: //玩具城 - 时间塔的本源
                            player.getMap().addDisconnected(player.getId());
                            break;
                    }
                }
                player.getMap().removePlayer(player);
            }
        } catch (Throwable e) {
            log.error(e);
        }
    }

    public void disconnect(boolean RemoveInChannelServer, boolean fromCS) {
        disconnect(RemoveInChannelServer, fromCS, false);
    }

    public void disconnect(boolean RemoveInChannelServer, boolean fromCS, boolean shutdown) {
        if (debugWindow != null) {
            debugWindow.dispose();
            debugWindow = null;
        }
        if (player != null) {
            MapleMap map = player.getMap();
            MapleParty party = player.getParty();
            String namez = player.getName();
            int idz = player.getId(), messengerId = player.getMessenger() == null ? 0 : player.getMessenger().getId(), gid = player.getGuildId(), fid = player.getFamilyId();
            BuddyList chrBuddy = player.getBuddylist();
            MaplePartyCharacter chrParty = new MaplePartyCharacter(player);
            MapleMessengerCharacter chrMessenger = new MapleMessengerCharacter(player);
            MapleGuildCharacter chrGuild = player.getMGC();
            MapleFamilyCharacter chrFamily = player.getMFC();

            removalTask(shutdown);
            LoginServer.getLoginAuth(player.getId());
            LoginServer.getLoginAuthKey(accountName, true);
            player.clearCache();
            player.saveToDB(true, fromCS);
            if (shutdown) {
                player = null;
                receiving = false;
                return;
            }

            if (!fromCS) {
                ChannelServer ch = ChannelServer.getInstance(map == null ? channel : map.getChannel());
                int chz = WorldFindService.getInstance().findChannel(idz);
                if (chz < -1) {
                    disconnect(RemoveInChannelServer, true);//u lie
                    return;
                }
                try {
                    if (chz == -1 || ch == null || ch.isShutdown()) {
                        player = null;
                        return;//no idea
                    }
                    if (messengerId > 0) {
                        WorldMessengerService.getInstance().leaveMessenger(messengerId, chrMessenger);
                    }
                    if (party != null) {
                        chrParty.setOnline(false);
                        WorldPartyService.getInstance().updateParty(party.getPartyId(), PartyOperation.LOG_ONOFF, chrParty);
                        if (map != null && party.getLeader().getId() == idz) {
                            MaplePartyCharacter lchr = null;
                            for (MaplePartyCharacter pchr : party.getMembers()) {
                                if (pchr != null && map.getCharacterById(pchr.getId()) != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                                    lchr = pchr;
                                }
                            }
                            if (lchr != null) {
                                WorldPartyService.getInstance().updateParty(party.getPartyId(), PartyOperation.CHANGE_LEADER_DC, lchr);
                            }
                        }
                    }
                    if (chrBuddy != null) {
                        if (!serverTransition) {
                            WorldBuddyService.getInstance().loggedOff(namez, idz, channel, chrBuddy.getBuddyIds());
                        } else { // Change channel
                            WorldBuddyService.getInstance().loggedOn(namez, idz, channel, chrBuddy.getBuddyIds());
                        }
                    }
                    if (gid > 0 && chrGuild != null) {
                        WorldGuildService.getInstance().setGuildMemberOnline(chrGuild, false, -1);
                    }
                    if (fid > 0 && chrFamily != null) {
                        WorldFamilyService.getInstance().setFamilyMemberOnline(chrFamily, false, -1);
                    }
                } catch (Exception e) {
                    log.error(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (RemoveInChannelServer && ch != null) {
                        ch.removePlayer(idz, namez); //这个地方是清理角色的信息
                    }
                    player = null;
                }
            } else {
                int ch = WorldFindService.getInstance().findChannel(idz);
                if (ch > 0) {
                    disconnect(RemoveInChannelServer, false); //如果频道大于 0 角色应该是在频道服务器中
                    return;
                }
                try {
                    if (party != null) {
                        chrParty.setOnline(false);
                        WorldPartyService.getInstance().updateParty(party.getPartyId(), PartyOperation.LOG_ONOFF, chrParty);
                    }
                    if (!serverTransition) {
                        WorldBuddyService.getInstance().loggedOff(namez, idz, channel, chrBuddy.getBuddyIds());
                    } else { // Change channel
                        WorldBuddyService.getInstance().loggedOn(namez, idz, channel, chrBuddy.getBuddyIds());
                    }
                    if (gid > 0 && chrGuild != null) {
                        WorldGuildService.getInstance().setGuildMemberOnline(chrGuild, false, -1);
                    }
                    if (fid > 0 && chrFamily != null) {
                        WorldFamilyService.getInstance().setFamilyMemberOnline(chrFamily, false, -1);
                    }
                    if (player != null) {
                        player.setMessenger(null);
                    }
                } catch (Exception e) {
                    log.error(getLogMessage(this, "ERROR") + e);
                } finally {
                    if (RemoveInChannelServer && ch == -10) {
                        CashShopServer.getPlayerStorage().deregisterPlayer(idz, namez);
                    }
                    player = null;
                }
            }
        }
        if (!serverTransition && isLoggedIn()) {
            updateLoginState(MapleClient.LOGIN_NOTLOGGEDIN, getSessionIPAddress());
            session.attr(MapleClient.CLIENT_KEY).set(null);
            session.attr(MaplePacketDecoder.DECODER_STATE_KEY).set(null);
            session.close();
        }
        engines.clear();
    }

    public String getSessionIPAddress() {
        if (session == null || !session.isActive()) {
            return "0.0.0.0";
        }
        return session.remoteAddress().toString().split(":")[0];
    }

    public boolean CheckIPAddress() {
        if (this.accId < 0) {
            return false;
        }
        boolean canlogin = true;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT SessionIP, banned FROM accounts WHERE id = ?");
            ps.setInt(1, this.accId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (rs.getInt("banned") > 0) {
                        canlogin = false; //canlogin false = close client
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Failed in checking IP address for client.", e);
        }
        return canlogin;
    }

    public void DebugMessage(StringBuilder sb) {
        sb.append(getSession().remoteAddress());
        sb.append(" 是否连接: ");
        sb.append(getSession().isActive());
        sb.append(" 是否断开: ");
        sb.append(!getSession().isOpen());
        sb.append(" 密匙状态: ");
        sb.append(getSession().attr(MapleClient.CLIENT_KEY) != null);
        sb.append(" 登录状态: ");
        sb.append(isLoggedIn());
        sb.append(" 是否有角色: ");
        sb.append(getPlayer() != null);
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    public int deleteCharacter(int cid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT guildid, guildrank, familyid, name FROM characters WHERE id = ? AND accountid = ?")) {
                ps.setInt(1, cid);
                ps.setInt(2, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return 1;
                    }
                    if (rs.getInt("guildid") > 0) { // is in a guild when deleted
                        if (rs.getInt("guildrank") == 1) { //cant delete when leader
                            rs.close();
                            ps.close();
                            return 1;
                        }
                        WorldGuildService.getInstance().deleteGuildCharacter(rs.getInt("guildid"), cid);
                    }
                    if (rs.getInt("familyid") > 0 && WorldFamilyService.getInstance().getFamily(rs.getInt("familyid")) != null) {
                        WorldFamilyService.getInstance().getFamily(rs.getInt("familyid")).leaveFamily(cid);
                    }
                }

                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM characters WHERE id = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "UPDATE pokemon SET active = 0 WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM hiredmerch WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mts_cart WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mts_items WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM cheatlog WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryitems WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM famelog WHERE characterid_to = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM dueypackages WHERE RecieverId = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?", cid); //商城购物车道具
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM buddies WHERE buddyid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM keymap WHERE characterid = ?", cid); //键盘设置
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?", cid); //技能信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skills WHERE teachId = ?", cid); //技能信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?", cid);
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM mountdata WHERE characterid = ?", cid); //坐骑信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?", cid); //技能宏信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?", cid); //任务信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?", cid); //背包空间数量信息
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?", cid); //小背包
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM bank WHERE charid = ?", cid); //银行系统
                MapleCharacter.deleteWhereCharacterId(con, "DELETE FROM bosslog WHERE characterid = ?", cid); //BOSS挑战日志
                return 0;
            }
        } catch (SQLException e) {
            log.error("删除角色错误.", e);
        }
        return 1;
    }

    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET gender = ? WHERE id = ?")) {
                ps.setByte(1, gender);
                ps.setInt(2, accId);
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            log.error("保存角色性别出错", e);
        }
    }

    public String getSecondPassword() {
        return secondPassword;
    }

    public void setSecondPassword(String secondPassword) {
        this.secondPassword = secondPassword;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public int getWorld() {
        return world;
    }

    public void setWorld(int world) {
        this.world = world;
    }

    public int getLatency() {
        return (int) (lastPong - lastPing);
    }

    public int getChatServerLatency() {
        return (int) (lastChatServerPong - lastChatServerPing);
    }

    public long getLastPong() {
        return lastPong;
    }

    public long getLastChatServerPong() {
        return lastChatServerPong;
    }

    public long getLastPing() {
        return lastPing;
    }

    public long getLastChatServerPing() {
        return lastChatServerPing;
    }

    public void pongReceived() {
        lastPong = System.currentTimeMillis();
    }

    public void chatServerPongReceived() {
        lastChatServerPong = System.currentTimeMillis();
    }

    public void sendPing() {
        lastPing = System.currentTimeMillis();
        session.writeAndFlush(LoginPacket.getPing());
//        session.writeAndFlush(LoginPacket.getChatServerPing());

        PingTimer.getInstance().schedule(() -> {
            try {
                if (getLatency() < 0) {
                    disconnect(true, false);
                    boolean close = false;
                    if (getSession() != null && getSession().isActive()) {
                        close = true;
                        getSession().close();
                    }
                    log.info(getLogMessage(MapleClient.this, "自动断线 : Ping超时" + (close ? "。" : ".")));
                }
            } catch (NullPointerException e) {
                System.out.println(e.toString());
            }
        }, 60000);
    }

    public boolean isGm() {
        return gm > 0;
    }

    public int getGmLevel() {
        return gm;
    }

    public ScheduledFuture<?> getIdleTask() {
        return idleTask;
    }

    public void setIdleTask(ScheduledFuture<?> idleTask) {
        this.idleTask = idleTask;
    }

    /**
     * 获取帐号可创建角色数量
     */
    public int getAccCharSlots() {
        if (charslots != ServerConfig.CHANNEL_PLAYER_MAXCHARACTERS) {
            return charslots; //save a sql
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM character_slots WHERE accid = ? AND worldid = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        charslots = rs.getInt("charslots");
                    } else {
                        try (PreparedStatement psu = con.prepareStatement("INSERT INTO character_slots (accid, worldid, charslots) VALUES (?, ?, ?)")) {
                            psu.setInt(1, accId);
                            psu.setInt(2, world);
                            psu.setInt(3, charslots);
                            psu.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取帐号可创建角色数量出现错误", e);
        }
        return charslots;
    }

    /**
     * 增加帐号可创建角色数量
     */
    public boolean gainAccCharSlot() {
        if (getAccCharSlots() >= 40) {
            return false;
        }
        charslots++;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE character_slots SET charslots = ? WHERE worldid = ? AND accid = ?")) {
                ps.setInt(1, charslots);
                ps.setInt(2, world);
                ps.setInt(3, accId);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            log.error("增加帐号可创建角色数量出现错误", e);
            return false;
        }
    }

    /**
     * 获取帐号下的角色卡数量
     */
    public int getAccCardSlots() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts_info WHERE accId = ? AND worldId = ?")) {
                ps.setInt(1, accId);
                ps.setInt(2, world);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        cardslots = rs.getInt("cardSlots");
                    } else {
                        try (PreparedStatement psu = con.prepareStatement("INSERT INTO accounts_info (accId, worldId, cardSlots) VALUES (?, ?, ?)")) {
                            psu.setInt(1, accId);
                            psu.setInt(2, world);
                            psu.setInt(3, cardslots);
                            psu.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取帐号下的角色卡数量出现错误", e);
        }
        return cardslots;
    }

    /**
     * 增加角色卡的数量
     */
    public boolean gainAccCardSlot() {
        if (getAccCardSlots() >= 9) {
            return false;
        }
        cardslots++;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts_info SET cardSlots = ? WHERE worldId = ? AND accId = ?")) {
                ps.setInt(1, cardslots);
                ps.setInt(2, world);
                ps.setInt(3, accId);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            log.error("增加角色卡的数量出现错误", e);
            return false;
        }
    }

    public boolean isMonitored() {
        return monitored;
    }

    public void setMonitored(boolean m) {
        this.monitored = m;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public void setReceiving(boolean m) {
        this.receiving = m;
    }

    public String getTempIP() {
        return tempIP;
    }

    public void setTempIP(String s) {
        this.tempIP = s;
    }

    public boolean isLocalhost() {
        return ServerConstants.isIPLocalhost(getSessionIPAddress());
    }

    public boolean hasCheck(int accid) {
        boolean ret = false;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                ps.setInt(1, accid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ret = rs.getInt("check") > 0;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error checking ip Check", e);
        }
        return ret;
    }

    public void setScriptEngine(String name, ScriptEngine e) {
        engines.put(name, e);
    }

    public ScriptEngine getScriptEngine(String name) {
        return engines.get(name);
    }

    public void removeScriptEngine(String name) {
        engines.remove(name);
    }

    public boolean canClickNPC() {
        return lastNpcClick + 500 < System.currentTimeMillis();
    }

    public void setClickedNPC() {
        lastNpcClick = System.currentTimeMillis();
    }

    public void removeClickedNPC() {
        lastNpcClick = 0;
    }

    public NPCConversationManager getCM() {
        return NPCScriptManager.getInstance().getCM(this);
    }

    public QuestActionManager getQM() {
        return QuestScriptManager.getInstance().getQM(this);
    }

    public ItemActionManager getIM() {
        return ItemScriptManager.getInstance().getIM(this);
    }

    public boolean hasCheckMac(String macData) {
        return !(macData.equalsIgnoreCase("00-00-00-00-00-00") || macData.length() != 17 || maclist.isEmpty()) && maclist.contains(macData);
    }

    public void setTempInfo(String login, String pwd, boolean isBanned) {
        tempinfo = new Triple<>(login, pwd, isBanned);
    }

    public Triple<String, String, Boolean> getTempInfo() {
        return tempinfo;
    }

    public void addProcessName(String process) {
        proesslist.add(process);
    }

    public boolean hasProcessName(String process) {
        for (String p : proesslist) {
            if (p.startsWith(process)) {
                return true;
            }
        }
        return proesslist.contains(process);
    }

    public void dropMessage(String message) {
        announce(MaplePacketCreator.serverNotice(1, message));
    }

    public void modifyCSPoints(int type, int quantity) {
        switch (type) {
            case 1:
                if (getACash() + quantity < 0) {
                    return;
                }
                setACash(getACash() + quantity);
                break;
            case 2:
                if (getMaplePoints() + quantity < 0) {
                    return;
                }
                setMaplePoints(getMaplePoints() + quantity);
                break;
            default:
                break;
        }
    }

    public int getCSPoints(int type) {
        switch (type) {
            case 1:
                return getACash();
            case 2:
                return getMaplePoints();
            default:
                return 0;
        }
    }

    public int getACash() {
        int point = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT ACash FROM accounts WHERE name = ?")) {
                ps.setString(1, getAccountName());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        point = rs.getInt("ACash");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取角色点券失败。" + e);
        }
        return point;
    }

    public void setACash(final int point) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET ACash = ? WHERE name = ?")) {
                ps.setInt(1, point);
                ps.setString(2, getAccountName());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("获取角色点券失败。" + e);
        }
    }

    public int getMaplePoints() {
        int point = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT mPoints FROM accounts WHERE id = ?")) {
                ps.setInt(1, getAccID());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        point = rs.getInt("mPoints");
                    }
                }
            }
        } catch (SQLException ex) {
            log.error("获取角色抵用券失败。" + ex);
        }
        return point;
    }

    public void setMaplePoints(final int point) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET mPoints = ? WHERE id = ?")) {
                ps.setInt(1, point);
                ps.setInt(2, getAccID());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("获取角色抵用券失败。" + e);
        }
    }

    public Map<String, String> getAccInfoFromDB() {
        Map<String, String> ret = new HashMap<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?")) {
                ps.setInt(1, accId);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    if (rs.next()) {
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            String result = "";
//                        if (metaData.getColumnTypeName(i).equalsIgnoreCase("DATE") || metaData.getColumnTypeName(i).equalsIgnoreCase("TIMESTAMP")) {
//                            result = rs.getDate(i).toString();
//                        } else {
                            result = rs.getString(metaData.getColumnName(i));
//                        }

                            ret.put(metaData.getColumnName(i), result);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取帐号数据失败", e);
        }
        return ret;
    }

    protected static class CharNameAndId {

        public final String name;
        public final int id;

        public CharNameAndId(String name, int id) {
            super();
            this.name = name;
            this.id = id;
        }
    }
}
