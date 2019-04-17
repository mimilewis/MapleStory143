/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author PlayDK
 */
public class MapleQuickSlot implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final List<Pair<Integer, Integer>> quickslot;
    private boolean changed = false;

    public MapleQuickSlot() {
        quickslot = new ArrayList<>();
    }

    public MapleQuickSlot(List<Pair<Integer, Integer>> quickslots) {
        quickslot = quickslots;
    }

    public List<Pair<Integer, Integer>> Layout() {
        changed = true;
        return quickslot;
    }

    public void unchanged() {
        changed = false;
    }

    public void resetQuickSlot() {
        changed = true;
        quickslot.clear();
    }

    public void addQuickSlot(int index, int key) {
        changed = true;
        quickslot.add(new Pair<>(index, key));
    }

    public int getKeyByIndex(int index) {
        for (Pair<Integer, Integer> p : quickslot) {
            if (p.getLeft() == index) {
                return p.getRight();
            }
        }
        return -1;
    }

    public void writeData(MaplePacketLittleEndianWriter mplew) {
        mplew.write(quickslot.isEmpty() ? 0 : 1);
        if (quickslot.isEmpty()) {
            return;
        }
        quickslot.sort(Comparator.comparing(Pair::getLeft));
        for (Pair<Integer, Integer> qs : quickslot) {
            mplew.writeInt(qs.getRight());
        }
    }

    public void saveQuickSlots(DruidPooledConnection con, int charid) throws SQLException {
        if (!changed) {
            return;
        }
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM quickslot WHERE characterid = ?")) {
            ps.setInt(1, charid);
            ps.execute();
            if (quickslot.isEmpty()) {
                return;
            }
            boolean first = true;
            StringBuilder query = new StringBuilder();
            for (Pair<Integer, Integer> q : quickslot) {
                if (first) {
                    first = false;
                    query.append("INSERT INTO quickslot VALUES (");
                } else {
                    query.append(",(");
                }
                query.append("DEFAULT,");
                query.append(charid).append(",");
                query.append(q.getLeft().intValue()).append(",");
                query.append(q.getRight().intValue()).append(")");
            }
            try (PreparedStatement pse = con.prepareStatement(query.toString())) {
                pse.execute();
            }
        }
    }
}
