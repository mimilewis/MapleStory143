package handling.login.handler;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.data.input.LittleEndianAccessor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UpdatePlayerSlots {

    private static final Logger log = LogManager.getLogger();

    public static void handlePacket(LittleEndianAccessor lea) {
        /*
         * 02 00 00 00 01 05 00 00 00 06 00 00 00 05 00 00 00 28 00 00 00 2C 00 00 00 2D 00 00 00
         * 02 00 00 00 01 05 00 00 00 05 00 00 00 28 00 00 00 06 00 00 00 2C 00 00 00 2D 00 00 00
         */
        lea.readInt(); // accid
        lea.skip(1);
        int size = lea.readInt();
        List<Integer> playerID = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            playerID.add(lea.readInt());
        }

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE characters SET position = ? WHERE id = ?")) {
                for (int i = 0; i < playerID.size(); i++) {
                    ps.setInt(1, i);
                    ps.setInt(2, playerID.get(i));
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error("变更角色位置出错", e);
        }
    }
}
