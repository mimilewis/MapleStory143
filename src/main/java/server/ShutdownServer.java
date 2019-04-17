package server;

import database.DatabaseConnection;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.chat.ChatServer;
import handling.login.LoginServer;
import handling.world.WorldAllianceService;
import handling.world.WorldBroadcastService;
import handling.world.WorldFamilyService;
import handling.world.WorldGuildService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer.*;
import tools.MaplePacketCreator;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;

public class ShutdownServer implements ShutdownServerMBean {

    private static final Logger log = LogManager.getLogger(ShutdownServer.class);
    public static ShutdownServer instance;
    public static boolean running = false;
    private int time = 0;
    private boolean first = true;

    public static void registerMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            instance = new ShutdownServer();
            mBeanServer.registerMBean(instance, new ObjectName("server:type=ShutdownServer"));
        } catch (Exception e) {
            log.error("Error registering Shutdown MBean", e);
        }
    }

    public static ShutdownServer getInstance() {
        return instance;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public void shutdown() {//can execute twice
        run();
    }

    @Override
    public void run() {
        synchronized (this) {
            if (running) { //Run once!
                return;
            }
            running = true;
        }

        if (time != 0) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(0, " 游戏服务器将在" + getTime() + "分钟后关闭维护，请玩家安全下线..."));
            if (first) {
                first = false;
                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    cs.setShutdown();
                    cs.setServerMessage("游戏服务器将关闭维护，请玩家安全下线...");
                    cs.closeAllMerchants();
                    cs.closeAllFisher();
                }
            }
            running = false;
            return;
        }
        WorldGuildService.getInstance().save();
        WorldAllianceService.getInstance().save();
        WorldFamilyService.getInstance().save();
        Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);
        for (int i : chs) {
            try {
                ChannelServer cs = ChannelServer.getInstance(i);
                synchronized (this) {
                    cs.shutdown();
                }
            } catch (Exception e) {
                log.error("关闭服务端错误" + e);
            }
        }
        LoginServer.shutdown();
        CashShopServer.shutdown();
        AuctionServer.shutdown();
        ChatServer.shutdown();
        System.out.println("正在关闭时钟线程...");
        WorldTimer.getInstance().stop();
        MapTimer.getInstance().stop();
        BuffTimer.getInstance().stop();
        CloneTimer.getInstance().stop();
        EventTimer.getInstance().stop();
        EtcTimer.getInstance().stop();
        PingTimer.getInstance().stop();
        System.out.println("正在关闭数据库连接...");
        try {
            DatabaseConnection.closeAll();
        } catch (SQLException e) {
            log.error("关闭数据库连接错误" + e);
        }
        System.out.println("服务端关闭完成...");
    }
}
