package handling.channel;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

public class MapleDojoRanking {

    private static final MapleDojoRanking instance = new MapleDojoRanking();
    private final List<DojoRankingInfo> ranks = new LinkedList<>();

    public static MapleDojoRanking getInstance() {
        return instance;
    }

    public List<DojoRankingInfo> getRank() {
        return ranks;
    }

    public void load(boolean reload) {
        if (reload) {
            ranks.clear();
        }
        if (!ranks.isEmpty()) {
            return;
        }
        PreparedStatement ps;
        ResultSet rs;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            ps = con.prepareStatement("SELECT * FROM dojorankings ORDER BY `rank` DESC LIMIT 50");
            rs = ps.executeQuery();
            while (rs.next()) {
                final DojoRankingInfo rank = new DojoRankingInfo(rs.getShort("rank"), rs.getString("name"), rs.getLong("time"));
                ranks.add(rank);
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error handling dojo rankings: " + e);
        }
    }

    public static class DojoRankingInfo {

        private final String name;
        private final short rank;
        private final long time;

        public DojoRankingInfo(short rank, String name, long time) {
            this.rank = rank;
            this.name = name;
            this.time = time;
        }

        public short getRank() {
            return rank;
        }

        public String getName() {
            return name;
        }

        public long getTime() {
            return time;
        }
    }
}
