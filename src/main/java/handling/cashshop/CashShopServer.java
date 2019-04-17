package handling.cashshop;

import configs.ServerConfig;
import handling.ServerType;
import handling.channel.PlayerStorage;
import handling.netty.ServerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CashShopServer {

    private static final Logger log = LogManager.getLogger(CashShopServer.class);
    private static String ip;
    private static ServerConnection init;
    private static PlayerStorage players;
    private static boolean finishedShutdown = false;
    private static short port;

    public static void run_startup_configurations() {
        port = ServerConfig.CASH_PORT;
        ip = ServerConfig.WORLD_INTERFACE;
        players = new PlayerStorage(-10);
        try {
            init = new ServerConnection(port, 0, -10, ServerType.商城服务器);
            init.run();
        } catch (final Exception e) {
            throw new RuntimeException("商城服务器绑定端口 " + port + " 失败", e);
        }
    }

    public static String getIP() {
        return ip;
    }

    public static short getPort() {
        return port;
    }

    public static PlayerStorage getPlayerStorage() {
        return players;
    }

    public static int getConnectedClients() {
        return getPlayerStorage().getConnectedClients();
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        log.info("正在关闭商城服务器...");
        players.disconnectAll();
        log.info("商城服务器解除端口绑定...");
        init.close();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }

    public static String getCashBlockedMsg(int itemId) {
        switch (itemId) {
            case 5050000: //洗能力点卷轴
            case 5072000: //高质地喇叭
            case 5073000: //心脏高级喇叭
            case 5074000: //白骨高级喇叭
            case 5076000: //道具喇叭
            case 5077000: //缤纷喇叭
            case 5079001: //蛋糕高级喇叭
            case 5079002: //馅饼高级喇叭
            case 5390000: //炽热情景喇叭
            case 5390001: //绚烂情景喇叭
            case 5390002: //爱心情景喇叭
            case 5390003: //新年庆祝喇叭1
            case 5390004: //新年庆祝喇叭2
            case 5390005: //小老虎情景喇叭
            case 5390006: //咆哮老虎情景喇叭
            case 5390007: //球进了!情景喇叭
            case 5390008: //世界杯情景喇叭
            case 5390010: //鬼出没情景喇叭
            case 5060003: //花生机
            case 5360000: //双倍爆率卡一天权
            case 5360014: //双倍爆率卡三小时权
            case 5360015: //双倍爆率卡一天权
            case 5360016: //双倍爆率卡一周权
                return "该道具只能通过NPC购买.";
        }
        return "该道具禁止购买.";
    }
}
