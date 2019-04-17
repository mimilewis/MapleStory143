package client.inventory;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.mysql.jdbc.exceptions.MySQLQueryInterruptedException;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleInventoryManipulator;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;

public class MapleRing implements Serializable {

    private static final Logger log = LogManager.getLogger(MapleRing.class);
    private static final long serialVersionUID = 9179541993413738579L;
    private final int ringId; //自己的戒指ID
    private final int partnerRingId; //对方的戒指ID
    private final int partnerId; //对方的角色ID
    private final int itemId; //戒指道具的ID
    private String partnerName; //对方的名字
    private boolean equipped = false;

    private MapleRing(int ringId, int partnerRingId, int partnerId, int itemid, String partnerName) {
        this.ringId = ringId;
        this.partnerRingId = partnerRingId;
        this.partnerId = partnerId;
        this.itemId = itemid;
        this.partnerName = partnerName;
    }

    public static MapleRing loadFromDb(int ringId) {
        return loadFromDb(ringId, false);
    }

    public static MapleRing loadFromDb(int ringId, boolean equipped) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE ringId = ?");
            ps.setInt(1, ringId);
            ResultSet rs = ps.executeQuery();
            MapleRing ret = null;
            if (rs.next()) {
                ret = new MapleRing(ringId, rs.getInt("partnerRingId"), rs.getInt("partnerChrId"), rs.getInt("itemid"), rs.getString("partnerName"));
                ret.setEquipped(equipped);
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            log.error("加载戒指信息出错", ex);
            return null;
        }
    }

    public static void addToDB(int itemid, MapleCharacter player, String partnerName, int partnerId, int[] ringId) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO rings (ringId, itemid, partnerChrId, partnerName, partnerRingId) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, ringId[0]);
            ps.setInt(2, itemid);
            ps.setInt(3, player.getId());
            ps.setString(4, player.getName());
            ps.setInt(5, ringId[1]);
            ps.executeUpdate();
            ps.close();

            ps = con.prepareStatement("INSERT INTO rings (ringId, itemid, partnerChrId, partnerName, partnerRingId) VALUES (?, ?, ?, ?, ?)");
            ps.setInt(1, ringId[1]);
            ps.setInt(2, itemid);
            ps.setInt(3, partnerId);
            ps.setString(4, partnerName);
            ps.setInt(5, ringId[0]);
            ps.executeUpdate();
            ps.close();
        }
    }

    public static int createRing(int itemId, MapleCharacter player, String partnerName, String msg, int partnerId, int itemSn) {
        try {
            if (player == null) {
                return -2;
            } else if (partnerId <= 0) {
                return -1;
            }
            return makeRing(itemId, player, partnerName, partnerId, msg, itemSn);
        } catch (Exception ex) {
            log.error("创建戒指信息出错", ex);
            return 0;
        }
    }

    public static int[] makeRing(int itemId, MapleCharacter player, MapleCharacter partnerPlayer) throws Exception {
        //生成2个戒指的ID [1] = 自己, [0] = 对方
        int[] makeRingId = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
        try {
            addToDB(itemId, player, partnerPlayer.getName(), partnerPlayer.getId(), makeRingId);
        } catch (MySQLQueryInterruptedException mslcve) {
            return makeRingId;
        }
        return makeRingId;
    }

    public static int makeRing(int itemId, MapleCharacter player, String partnerName, int partnerId, String msg, int itemSn) throws Exception {
        //生成2个戒指的ID [1] = 自己, [0] = 对方
        int[] makeRingId = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
        try {
            addToDB(itemId, player, partnerName, partnerId, makeRingId);
        } catch (MySQLQueryInterruptedException mslcve) {
            return 0;
        }
        MapleInventoryManipulator.addRing(player, itemId, makeRingId[1], itemSn);
        player.getCashInventory().gift(partnerId, player.getName(), msg, itemSn, makeRingId[0]);
        return 1;
    }

    public static void removeRingFromDb(int ringId, int partnerRingId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM rings WHERE ringId = ? OR ringId = ?");
            ps.setInt(1, ringId);
            ps.setInt(2, partnerRingId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            log.error("删除戒指信息出错", ex);
        }
    }

    public static void removeRingFromDb(MapleCharacter player) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM rings WHERE partnerChrId = ?");
            ps.setInt(1, player.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                rs.close();
                return;
            }
            int otherId = rs.getInt("partnerRingId");
            int otherotherId = rs.getInt("ringId");
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM rings WHERE ringId = ? OR ringId = ?");
            ps.setInt(1, otherotherId);
            ps.setInt(2, otherId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            log.error("删除戒指信息出错", ex);
        }
    }

    public int getRingId() {
        return ringId;
    }

    public int getPartnerRingId() {
        return partnerRingId;
    }

    public int getPartnerChrId() {
        return partnerId;
    }

    public int getItemId() {
        return itemId;
    }

    public boolean isEquipped() {
        return equipped;
    }

    public void setEquipped(boolean equipped) {
        this.equipped = equipped;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(String partnerName) {
        this.partnerName = partnerName;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MapleRing && ((MapleRing) o).getRingId() == getRingId();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + this.ringId;
        return hash;
    }

    public static class RingComparator implements Comparator<MapleRing>, Serializable {


        private static final long serialVersionUID = -1052568707811904914L;

        @Override
        public int compare(MapleRing o1, MapleRing o2) {
            if (o1.ringId < o2.ringId) {
                return -1;
            } else if (o1.ringId == o2.ringId) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}
