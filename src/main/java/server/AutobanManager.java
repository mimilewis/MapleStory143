package server;

import client.MapleClient;
import configs.ServerConfig;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class AutobanManager implements Runnable {

    private static final Logger log = LogManager.getLogger(AutobanManager.class);
    private static final int AUTOBAN_POINTS = 5000;
    private static final AutobanManager instance = new AutobanManager();
    private final Map<Integer, Integer> points = new HashMap<>();
    private final Map<Integer, List<String>> reasons = new HashMap<>();
    private final Set<ExpirationEntry> expirations = new TreeSet<>();
    private final ReentrantLock lock = new ReentrantLock(true);

    public static AutobanManager getInstance() {
        return instance;
    }

    public void autoban(MapleClient c, String reason) {
        if (c.getPlayer() == null) {
            return;
        }
        if (c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(5, "[警告] A/b 触发: " + reason);
        } else if (ServerConfig.WORLD_AUTOBAN) {
            addPoints(c, AUTOBAN_POINTS, 0, reason);
        } else {
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + c.getPlayer().getName() + " ID: " + c.getPlayer().getId() + " (等级 " + c.getPlayer().getLevel() + ") 游戏操作异常. (原因: " + reason + ")"));
        }
    }

    public void addPoints(MapleClient c, int points, long expiration, String reason) {
        lock.lock();
        try {
            List<String> reasonList;
            int acc = c.getPlayer().getAccountID();

            if (this.points.containsKey(acc)) {
                int SavedPoints = this.points.get(acc);
                if (SavedPoints >= AUTOBAN_POINTS) { // Already auto ban'd.
                    return;
                }
                this.points.put(acc, SavedPoints + points); // Add
                reasonList = this.reasons.get(acc);
                reasonList.add(reason);
            } else {
                this.points.put(acc, points); //[账号ID] [points]
                reasonList = new LinkedList<>();
                reasonList.add(reason);
                this.reasons.put(acc, reasonList); //[账号ID] [封号原因]
            }

            if (this.points.get(acc) >= AUTOBAN_POINTS) { // See if it's sufficient to auto ban
                log.info("[作弊] 玩家 " + c.getPlayer().getName() + " A/b 触发 " + reason);
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(5, "[警告] A/b 触发 : " + reason);
                    return;
                }
                StringBuilder sb = new StringBuilder("A/b ");
                sb.append(c.getPlayer().getName());
                sb.append(" (IP ");
                sb.append(c.getSessionIPAddress());
                sb.append("): ");
                for (String s : reasons.get(acc)) {
                    sb.append(s);
                    sb.append(", ");
                }
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(0, " <" + c.getPlayer().getName() + "> 被系统封号 (原因: " + reason + ")"));
                c.getPlayer().ban(sb.toString(), false, true, false);
            } else {
                if (expiration > 0) {
                    expirations.add(new ExpirationEntry(System.currentTimeMillis() + expiration, acc, points));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        for (ExpirationEntry e : expirations) {
            if (e.time <= now) {
                this.points.put(e.acc, this.points.get(e.acc) - e.points);
            } else {
                return;
            }
        }
    }

    private static class ExpirationEntry implements Comparable<ExpirationEntry> {

        public final long time;
        public final int acc;
        public final int points;

        public ExpirationEntry(long time, int acc, int points) {
            this.time = time;
            this.acc = acc;
            this.points = points;
        }

        @Override
        public int compareTo(AutobanManager.ExpirationEntry o) {
            return (int) (time - o.time);
        }

        @Override
        public boolean equals(Object oth) {
            if (!(oth instanceof AutobanManager.ExpirationEntry)) {
                return false;
            }
            AutobanManager.ExpirationEntry ee = (AutobanManager.ExpirationEntry) oth;
            return (time == ee.time && points == ee.points && acc == ee.acc);
        }
    }
}
