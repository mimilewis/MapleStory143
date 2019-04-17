package handling.login;

import configs.ServerConfig;
import handling.ServerType;
import handling.login.handler.MapleBalloon;
import handling.netty.ServerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.Pair;
import tools.Quadruple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginServer {

    private static final Logger log = LogManager.getLogger(LoginServer.class);
    private static final List<MapleBalloon> lBalloon = new ArrayList<>();
    private static final HashMap<Integer, Quadruple<String, String, Integer, String>> loginAuth = new HashMap<>();
    private static final HashMap<String, Pair<String, Integer>> loginAuthKey = new HashMap<>();
    private static short port;
    private static ServerConnection init;
    private static Map<Integer, Integer> load = new HashMap<>();
    private static String serverName, eventMessage;
    private static byte flag;
    private static int usersOn = 0;
    private static boolean finishedShutdown = true;

    public static void putLoginAuth(int chrid, String ip, String tempIp, int channel, String mac) {
        loginAuth.put(chrid, new Quadruple<>(ip, tempIp, channel, mac));
    }

    public static Quadruple<String, String, Integer, String> getLoginAuth(int chrid) {
        return loginAuth.remove(chrid);
    }

    public static void pubLoginAuthKey(String key, String account, int channel) {
        loginAuthKey.put(key, new Pair<>(account, channel));
    }

    public static Pair<String, Integer> getLoginAuthKey(String account, boolean remove) {
        if (remove) {
            return loginAuthKey.remove(account);
        } else {
            return loginAuthKey.get(account);
        }
    }

    public static void addChannel(int channel) {
        load.put(channel, 0);
    }

    public static void removeChannel(int channel) {
        load.remove(channel);
    }

    public static void run_startup_configurations() {
        serverName = ServerConfig.LOGIN_SERVERNAME;
        eventMessage = ""; //ServerProperties.getProperty("login.eventMessage");
        flag = ServerConfig.LOGIN_SERVERFLAG;
        port = ServerConfig.LOGIN_PORT;
        try {
            init = new ServerConnection(port, -1, -1, ServerType.登录服务器);
            init.run();
            log.info("登录器服务器绑定端口: " + port + ".");
            log.info("当前设置最大在线: " + ServerConfig.LOGIN_USERLIMIT + " 人 默认角色数: " + ServerConfig.LOGIN_DEFAULTUSERLIMIT + " 人 自动注册: " + ServerConfig.WORLD_ONLYADMIN);
        } catch (Exception e) {
            throw new RuntimeException("登录器服务器绑定端口: " + port + " 失败", e);
        }
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        log.info("正在关闭登录服务器...");
        init.close();
        finishedShutdown = true; //nothing. lol
    }

    public static String getServerName() {
        return serverName;
    }

    public static String getTrueServerName() {
        return serverName.substring(0, serverName.length() - 3);
    }

    public static String getEventMessage() {
        return eventMessage;
    }

    public static void setEventMessage(String newMessage) {
        eventMessage = newMessage;
    }

    public static byte getFlag() {
        return flag;
    }

    public static void setFlag(byte newflag) {
        flag = newflag;
    }

    public static Map<Integer, Integer> getLoad() {
        return load;
    }

    public static void setLoad(Map<Integer, Integer> load_, int usersOn_) {
        load = load_;
        usersOn = usersOn_;
    }

    public static int getUserLimit() {
        return ServerConfig.LOGIN_USERLIMIT;
    }

    public static void setUserLimit(int newLimit) {
        ServerConfig.LOGIN_USERLIMIT = newLimit;
    }

    public static int getUsersOn() {
        return usersOn;
    }

    public static List<MapleBalloon> getBalloons() {
        return lBalloon;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static void setOn() {
        finishedShutdown = false;
    }
}
