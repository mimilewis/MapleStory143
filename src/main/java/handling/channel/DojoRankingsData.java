/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DojoRankingsData {

    private static final DojoRankingsData instance = new DojoRankingsData();
    private static final int limit = 25;
    public final String[] names = new String[limit];
    public final long[] times = new long[limit];
    public final int[] ranks = new int[limit];
    public int totalCharacters = 0;

    private DojoRankingsData() {
    }

    public static DojoRankingsData getInstance() {
        return instance;
    }

    public static DojoRankingsData loadLeaderboard() {
        DojoRankingsData ret = new DojoRankingsData();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT `name`, `time` FROM `dojorankings` ORDER BY `time` ASC LIMIT " + limit);
            ResultSet rs = ps.executeQuery();

            int i = 0;
            while (rs.next()) {
                if (rs.getInt("time") != 0) {
                    //long time = (rs.getLong("endtime") - rs.getLong("starttime")) / 1000;
                    ret.ranks[i] = (i + 1);
                    ret.names[i] = rs.getString("name");
                    ret.times[i] = rs.getInt("time");
                    // ret.times[i] = time;
                    ret.totalCharacters++;
                    i++;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return ret;
    }
}
