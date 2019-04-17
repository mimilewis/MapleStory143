/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.chat;

import configs.ServerConfig;
import handling.ServerType;
import handling.netty.ServerConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChatServer {

    private static final Logger log = LogManager.getLogger(ChatServer.class);
    private static String ip;
    private static ServerConnection init;
    private static boolean finishedShutdown = false;
    private static short port;

    public static void run_startup_configurations() {

        port = ServerConfig.CHAT_PORT;
        ip = ServerConfig.WORLD_INTERFACE;
        try {
            init = new ServerConnection(port, -1, -1, ServerType.聊天服务器);
            init.run();
            log.info("聊天服务器绑定端口: " + port + ".");
        } catch (final Exception e) {
            throw new RuntimeException("聊天服务器绑定端口 " + port + " 失败", e);
        }
    }

    public static String getIP() {
        return ip;
    }

    public static int getPort() {
        return port;
    }

    public static void shutdown() {
        if (finishedShutdown) {
            return;
        }
        log.info("正在关闭商城服务器...");
        log.info("商城服务器解除端口绑定...");
        init.close();
        finishedShutdown = true;
    }

    public static boolean isShutdown() {
        return finishedShutdown;
    }
}
