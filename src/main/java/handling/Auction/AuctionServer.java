/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.Auction;

import configs.ServerConfig;
import handling.ServerType;
import handling.channel.PlayerStorage;
import handling.netty.ServerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MTSStorage;

/**
 * 拍卖服务器
 *
 * @author PlayDK
 */
public class AuctionServer {

    private static final Logger log = LogManager.getLogger(AuctionServer.class);
    private static ServerConnection init;
    private static String ip;
    private static PlayerStorage players;
    private static boolean finishedShutdown = false;
    private static short port;

    public static void run_startup_configurations() {
        try {
            port = ServerConfig.AUCTION_PORT;
            ip = ServerConfig.WORLD_INTERFACE;
            players = new PlayerStorage(-20);
            init = new ServerConnection(port, 0, -20, ServerType.拍卖服务器);
            init.run();
            log.info("拍卖服务器绑定端口: " + port + ".");
        } catch (final Exception e) {
            throw new RuntimeException("拍卖服务器绑定端口 " + port + " 失败", e);
        }
    }

    public static String getIP() {
        return ip;
    }

    public static PlayerStorage getPlayerStorage() {
        return players;
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        log.info("正在关闭拍卖服务器...");
        players.disconnectAll();
        MTSStorage.getInstance().saveBuyNow(true);
        log.info("拍卖服务器解除端口绑定...");
        init.close();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }
}
