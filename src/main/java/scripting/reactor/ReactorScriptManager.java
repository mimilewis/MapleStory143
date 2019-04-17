package scripting.reactor;

import client.MapleClient;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractScriptManager;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ReactorScriptManager extends AbstractScriptManager {

    private static final Logger log = LogManager.getLogger(ReactorScriptManager.class.getName());
    private static final ReactorScriptManager instance = new ReactorScriptManager();
    private final Map<Integer, List<ReactorDropEntry>> drops = new HashMap<>();

    public synchronized static ReactorScriptManager getInstance() {
        return instance;
    }

    public void act(MapleClient c, MapleReactor reactor) {
        try {
            Invocable iv = getInvocable("reactor/" + reactor.getReactorId() + ".js", c);
            if (iv == null) {
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(5, "未找到 Reactor 文件中的 " + reactor.getReactorId() + ".js 文件.");
                }
                log.info("未找到 Reactor 文件中的 " + reactor.getReactorId() + ".js 文件.");
                return;
            }
            ScriptEngine scriptengine = (ScriptEngine) iv;
            ReactorActionManager rm = new ReactorActionManager(c, reactor);

            scriptengine.put("rm", rm);
            iv.invokeFunction("act");
        } catch (Exception e) {
            log.error("执行Reactor文件出错 ReactorID: " + reactor.getReactorId() + ", ReactorName: " + reactor.getName(), e);
        }
    }

    public List<ReactorDropEntry> getDrops(int reactorId) {
        List<ReactorDropEntry> ret = drops.get(reactorId);
        if (ret != null) {
            return ret;
        }
        ret = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM reactordrops WHERE reactorid = ?")) {
                ps.setInt(1, reactorId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret.add(new ReactorDropEntry(rs.getInt("itemid"), rs.getInt("chance"), rs.getInt("questid")));
                    }
                }
            }
        } catch (SQLException e) {
            log.error("从SQL中读取箱子的爆率出错.箱子的ID: " + reactorId, e);
        }
        drops.put(reactorId, ret);
        return ret;
    }

    public void clearDrops() {
        drops.clear();
    }
}
