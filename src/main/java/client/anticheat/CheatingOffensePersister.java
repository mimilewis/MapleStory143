package client.anticheat;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import server.Timer.CheatTimer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CheatingOffensePersister {

    private final static CheatingOffensePersister instance = new CheatingOffensePersister();
    private final Set<CheatingOffenseEntry> toPersist = new LinkedHashSet<>();
    private final Lock mutex = new ReentrantLock();

    private CheatingOffensePersister() {
        CheatTimer.getInstance().register(new PersistingTask(), 61000);
    }

    public static CheatingOffensePersister getInstance() {
        return instance;
    }

    public void persistEntry(CheatingOffenseEntry coe) {
        mutex.lock();
        try {
            toPersist.remove(coe); //equal/hashCode h4x
            toPersist.add(coe);
        } finally {
            mutex.unlock();
        }
    }

    public class PersistingTask implements Runnable {

        @Override
        public void run() {
            CheatingOffenseEntry[] offenses;

            mutex.lock();
            try {
                offenses = toPersist.toArray(new CheatingOffenseEntry[toPersist.size()]);
                toPersist.clear();
            } finally {
                mutex.unlock();
            }

            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                PreparedStatement insertps = con.prepareStatement("INSERT INTO cheatlog (characterid, offense, count, lastoffensetime, param) VALUES (?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
                PreparedStatement updateps = con.prepareStatement("UPDATE cheatlog SET count = ?, lastoffensetime = ?, param = ? WHERE id = ?");
                for (CheatingOffenseEntry offense : offenses) {
                    String parm = offense.getParam() == null ? "" : offense.getParam();
                    if (offense.getDbId() == -1) {
                        insertps.setInt(1, offense.getChrfor());
                        insertps.setString(2, offense.getOffense().name());
                        insertps.setInt(3, offense.getCount());
                        insertps.setTimestamp(4, new Timestamp(offense.getLastOffenseTime()));
                        insertps.setString(5, parm);
                        insertps.executeUpdate();
                        ResultSet rs = insertps.getGeneratedKeys();
                        if (rs.next()) {
                            offense.setDbId(rs.getInt(1));
                        }
                        rs.close();
                    } else {
                        updateps.setInt(1, offense.getCount());
                        updateps.setTimestamp(2, new Timestamp(offense.getLastOffenseTime()));
                        updateps.setString(3, parm);
                        updateps.setInt(4, offense.getDbId());
                        updateps.executeUpdate();
                    }
                }
                insertps.close();
                updateps.close();
            } catch (SQLException e) {
                System.err.println("error persisting cheatlog" + e);
            }
        }
    }
}
