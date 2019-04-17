package handling.login;

import client.MapleClient;
import client.MapleEnumClass;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.handler.ServerlistRequestHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer.PingTimer;
import tools.MaplePacketCreator;
import tools.packet.LoginPacket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Map.Entry;

public class LoginWorker {

    private static final Logger log = LogManager.getLogger(LoginWorker.class.getName());
    private static long lastUpdate = 0;

    public static void registerClient(final MapleClient c, boolean useKey) {
        if (ServerConfig.WORLD_ONLYADMIN && !c.isGm() && !c.isLocalhost()) {
            c.announce(MaplePacketCreator.serverNotice(1, "当前服务器设置只能管理员进入游戏.\r\n我们目前在修复几个问题.\r\n请稍后再试."));
            c.announce(LoginPacket.getLoginFailed(MapleEnumClass.AuthReply.GAME_DEFINITION_INFO));
            return;
        }
        if (System.currentTimeMillis() - lastUpdate > 600000) { // Update once every 10 minutes
            lastUpdate = System.currentTimeMillis();
            Map<Integer, Integer> load = ChannelServer.getChannelLoad();
            int usersOn = 0;
            if (load == null || load.size() <= 0) { // In an unfortunate event that client logged in before load
                lastUpdate = 0;
                c.announce(LoginPacket.getLoginFailed(MapleEnumClass.AuthReply.GAME_CONNECTING_ACCOUNT));
                return;
            }
            double loadFactor = ServerConfig.LOGIN_USERLIMIT / ((double) LoginServer.getUserLimit() / load.size() / 100);
            for (Entry<Integer, Integer> entry : load.entrySet()) {
                usersOn += entry.getValue();
                load.put(entry.getKey(), Math.min(ServerConfig.LOGIN_USERLIMIT, (int) (entry.getValue() * loadFactor)));
            }
            LoginServer.setLoad(load, usersOn);
            lastUpdate = System.currentTimeMillis();
        }
        if (c.finishLogin() == 0) {
            if (!useKey) {
                c.announce(LoginPacket.getAuthSuccessRequest(c));
            } else {
                c.announce(LoginPacket.getAuthSuccessRequestX(c, false));
            }

            ServerlistRequestHandler.handlePacket(c, false);
            c.setIdleTask(PingTimer.getInstance().schedule(c.getSession()::close, 10 * 60 * 10000));
        } else {
            c.announce(LoginPacket.getLoginFailed(MapleEnumClass.AuthReply.GAME_CONNECTING_ACCOUNT));
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO accounts_log (accid, accname, ip, macs) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, c.getAccID());
                ps.setString(2, c.getAccountName());
                ps.setString(3, c.getSessionIPAddress());
                ps.setString(4, c.getMac());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error(e);
        }
    }
}
