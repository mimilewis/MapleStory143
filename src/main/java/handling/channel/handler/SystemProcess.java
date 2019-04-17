/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleProcess;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer;
import tools.data.input.LittleEndianAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemProcess {

    public static final Map<String, String> WG_Property = new HashMap<>();
    private static final Logger log = LogManager.getLogger(SystemProcess.class.getName());

    public static void SystemProcess(LittleEndianAccessor slea, MapleClient c, final MapleCharacter chr) {
        int num = slea.readInt();
        slea.skip(1);
        String cMD5;
        String path;
        try {
            for (int i = 0; i < num; i++) {
                path = slea.readMapleAsciiString(); //路徑
                slea.readMapleAsciiString();
                slea.readMapleAsciiString();
                slea.readMapleAsciiString();
                cMD5 = slea.readMapleAsciiString(); //MD5
                MapleProcess process = new MapleProcess(path, cMD5);
                if (SystemProcess.checkWGProcess(process) != null || SystemProcess.checkWGProcess(chr.getProcess()) != null) {
                    final MapleProcess WG;
                    if (SystemProcess.checkWGProcess(process) != null) {
                        WG = SystemProcess.checkWGProcess(process);
                    } else {
                        WG = SystemProcess.checkWGProcess(chr.getProcess());
                    }
                    //c.announce(NPCPacket.getNPCTalk(9010000, (byte) 0, "系統檢測到你使用非法程序，你將被永久封號。", "00 00", (byte) 0, 9010000));
                    chr.dropMessage(1, "检测到非法程序!");
                    Timer.CheatTimer.getInstance().schedule(() -> {
                        //chr.ban("被系統封號：使用外掛 " + WG.getName(), true, true, true);
                    }, 10 * 1000);
                }
                boolean loged = false;
                List<MapleProcess> temp = chr.getProcess();
                for (MapleProcess mp : temp) {
                    if (mp.getPath().equals(path) && mp.getMD5().equals(cMD5)) {
                        loged = true;
                        break;
                    }
                }
                if (!loged) {
                    chr.getProcess().add(process);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        chr.setLastCheckProcess(System.currentTimeMillis());
    }

    public static void loadWGProperty() {
        WG_Property.clear();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM hacker")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        WG_Property.put(rs.getString("name"), rs.getString("md5").toLowerCase());
                    }
                }
                ps.close();
            }
        } catch (SQLException e) {
            System.err.println("獲取外掛特征時出現問題：" + e);
        }
    }

    public static MapleProcess checkWGProcess(List<MapleProcess> processList) {
        if (WG_Property.isEmpty()) {
            loadWGProperty();
        }
        for (MapleProcess process : processList) {
            if (WG_Property.containsValue(process.getMD5().toLowerCase()) || WG_Property.containsKey(process.getName())) {
                return process;
            }
        }
        return null;
    }

    public static MapleProcess checkWGProcess(MapleProcess process) {
        if (WG_Property.isEmpty()) {
            loadWGProperty();
        }
        if (WG_Property.containsValue(process.getMD5().toLowerCase()) || WG_Property.containsKey(process.getName())) {
            return process;
        }
        return null;
    }
}
