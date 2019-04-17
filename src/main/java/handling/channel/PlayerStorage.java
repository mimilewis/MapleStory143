package handling.channel;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import handling.world.CharacterTransfer;
import handling.world.CheaterData;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import server.Timer.PingTimer;
import server.Timer.PlayerTimer;
import server.console.groups.datamanage.PlayerPane;
import tools.JsonUtil;
import tools.RedisUtil;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerStorage {

    private static final Logger log = LogManager.getLogger(PlayerStorage.class.getName());
    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock readLock = mutex.readLock(), writeLock = mutex.writeLock();
    private final ReentrantReadWriteLock mutex2 = new ReentrantReadWriteLock();
    private final Lock connectcheckReadLock = mutex2.readLock(), pendingWriteLock = mutex2.writeLock();
    private final Map<String, MapleCharacter> nameToChar = new LinkedHashMap<>();
    private final Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<>();
    private final Map<Integer, CharacterTransfer> PendingCharacter = new HashMap<>();
    private final PlayerObservable playerObservable = new PlayerObservable();
    private final int channel;

    public PlayerStorage(int channel) {
        this.channel = channel;
        PlayerTimer.getInstance().register(new UpdateCacheTask(), 10 * 1000);
        PingTimer.getInstance().register(new PersistingTask(), 60 * 1000);
        PingTimer.getInstance().register(new ConnectChecker(), 60 * 1000); //60秒检测1次
    }

    public ArrayList<MapleCharacter> getAllCharacters() {
        readLock.lock();
        try {
            return new ArrayList<>(idToChar.values());
        } finally {
            readLock.unlock();
        }
    }

    /*
     * 注册角色到服务器上
     */
    public void registerPlayer(MapleCharacter chr) {
        writeLock.lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
            playerObservable.changed();
            PlayerPane.getInstance(null).registerIDs(chr.getId(), chr.getPlayerObservable());
        } finally {
            writeLock.unlock();
        }
        WorldFindService.getInstance().register(chr.getId(), chr.getName(), channel);
    }

    /*
     * 注册临时角色信息到服务器上
     */
    public void registerPendingPlayer(CharacterTransfer chr, int playerId) {
//        Jedis jedis = RedisUtil.getJedis();
//        try {
//            jedis.hset(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName(), String.valueOf(playerId), JsonUtil.getMapperInstance().writeValueAsString(chr));
//            PendingCharacter.put(playerId, chr.TranferTime);
//        } catch (JsonProcessingException e) {
//            log.error("注册临时角色信息到服务器出错", e);
//        } finally {
//            RedisUtil.returnResource(jedis);
//        }
        writeLock.lock();
        try {
            PendingCharacter.put(playerId, chr);
        } finally {
            writeLock.unlock();
        }
    }

    /*
     * 通过 chr
     * 注销角色登记信息
     */
    public void deregisterPlayer(MapleCharacter chr) {
        removePlayer(chr.getId(), chr.getName());
        WorldFindService.getInstance().forceDeregister(chr.getId(), chr.getName());
    }

    /*
     * 通过 角色ID 和 角色名字
     * 注销角色登记信息
     */
    public void deregisterPlayer(int idz, String namez) {
        removePlayer(idz, namez);
        WorldFindService.getInstance().forceDeregister(idz, namez);
    }

    /*
     * 通过 chr
     * 断开角色登记信息
     */
    public void disconnectPlayer(MapleCharacter chr) {
        removePlayer(chr.getId(), chr.getName());
        WorldFindService.getInstance().forceDeregisterEx(chr.getId(), chr.getName());
    }

    private void removePlayer(int idz, String namez) {
        writeLock.lock();
        try {
            nameToChar.remove(namez.toLowerCase());
            MapleCharacter chr = idToChar.remove(idz);
            if (chr != null) {
                chr.setOnlineTime();
                PlayerPane.getInstance(null).removeIDs(chr.getId(), chr.getPlayerObservable());
            }
            playerObservable.changed();
        } finally {
            writeLock.unlock();
        }
    }

    public CharacterTransfer getPendingCharacter(int playerId) throws IOException {
        writeLock.lock();
        try {
            return PendingCharacter.remove(playerId);
        } finally {
            writeLock.unlock();
        }
    }

    public MapleCharacter getCharacterByName(String name) {
        readLock.lock();
        try {
            return nameToChar.get(name.toLowerCase());
        } finally {
            readLock.unlock();
        }
    }

    public MapleCharacter getCharacterById(int id) {
        readLock.lock();
        try {
            return idToChar.get(id);
        } finally {
            readLock.unlock();
        }
    }

    public int getConnectedClients() {
        return idToChar.size();
    }

    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = new ArrayList<>();
        readLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (chr.getCheatTracker().getPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getCheatTracker().getPoints(), MapleCharacterUtil.makeMapleReadable(chr.getName()) + " ID: " + chr.getId() + " (" + chr.getCheatTracker().getPoints() + ") " + chr.getCheatTracker().getSummary()));
                }
            }
        } finally {
            readLock.unlock();
        }
        return cheaters;
    }

    public List<CheaterData> getReports() {
        List<CheaterData> cheaters = new ArrayList<>();
        readLock.lock();
        try {
            final Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (chr.getReportPoints() > 0) {
                    cheaters.add(new CheaterData(chr.getReportPoints(), MapleCharacterUtil.makeMapleReadable(chr.getName()) + " ID: " + chr.getId() + " (" + chr.getReportPoints() + ") " + chr.getReportSummary()));
                }
            }
        } finally {
            readLock.unlock();
        }
        return cheaters;
    }

    /*
     * 断开所有非GM角色连接
     */
    public void disconnectAll() {
        disconnectAll(false);
    }

    /*
     * 断开所有角色连接
     */
    public void disconnectAll(boolean checkGM) {
        writeLock.lock();
        try {
            Iterator<MapleCharacter> chrit = nameToChar.values().iterator();
            MapleCharacter chr;
            while (chrit.hasNext()) {
                chr = chrit.next();
                if (!chr.isGM() || !checkGM) {
                    chr.getClient().disconnect(false, false, true);
                    if (chr.getClient().getSession().isActive()) {
                        chr.getClient().getSession().close();
                    }
                    WorldFindService.getInstance().forceDeregister(chr.getId(), chr.getName());
                    chrit.remove();
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /*
     * 获取在线角色的名字
     */
    public String getOnlinePlayers(boolean byGM) {
        StringBuilder sb = new StringBuilder();
        if (byGM) {
            readLock.lock();
            try {
                for (MapleCharacter mapleCharacter : nameToChar.values()) {
                    sb.append(MapleCharacterUtil.makeMapleReadable(mapleCharacter.getName()));
                    sb.append(", ");
                }
            } finally {
                readLock.unlock();
            }
        } else {
            readLock.lock();
            try {
                Iterator<MapleCharacter> itr = nameToChar.values().iterator();
                MapleCharacter chr;
                while (itr.hasNext()) {
                    chr = itr.next();
                    if (!chr.isGM()) {
                        sb.append(MapleCharacterUtil.makeMapleReadable(chr.getName()));
                        sb.append(", ");
                    }
                }
            } finally {
                readLock.unlock();
            }
        }
        return sb.toString();
    }

    /*
     * 发送给当前频道在线玩家封包
     */
    public void broadcastPacket(byte[] data) {
        readLock.lock();
        try {
            for (MapleCharacter mapleCharacter : nameToChar.values()) {
                mapleCharacter.getClient().announce(data);
            }
        } finally {
            readLock.unlock();
        }
    }

    /*
     * 发送给当前频道在线玩家喇叭的封包
     */
    public void broadcastSmegaPacket(byte[] data) {
        readLock.lock();
        try {
            Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (chr.getClient().isLoggedIn() && chr.getSmega()) {
                    chr.send(data);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    /*
     * 发送给当前频道在线GM的封包
     */
    public void broadcastGMPacket(byte[] data) {
        readLock.lock();
        try {
            Iterator<MapleCharacter> itr = nameToChar.values().iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (chr.getClient().isLoggedIn() && chr.isIntern()) {
                    chr.send(data);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    public PlayerObservable getPlayerObservable() {
        return playerObservable;
    }

    public class UpdateCacheTask implements Runnable {
        private final Jedis jedis = RedisUtil.getJedis();

        @Override
        public void run() {
            try {
                for (Entry<Integer, MapleCharacter> entry : idToChar.entrySet()) {
                    CharacterTransfer ct = new CharacterTransfer(entry.getValue());
                    jedis.hset(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName(), String.valueOf(entry.getKey()), JsonUtil.getMapperInstance().writeValueAsString(ct));
                }
            } catch (JsonProcessingException e) {
                log.error("更新缓存出错", e);
            }
        }
    }


    public class PersistingTask implements Runnable {

        @Override
        public void run() {
            pendingWriteLock.lock();
            try {
                long currenttime = System.currentTimeMillis();
                // min
                PendingCharacter.entrySet().removeIf(next -> currenttime - next.getValue().TranferTime > 1000 * 60 * 30);
            } finally {
                pendingWriteLock.unlock();
            }
        }
    }

    private class ConnectChecker implements Runnable {

        @Override
        public void run() {
            connectcheckReadLock.lock();
            try {
                Iterator<MapleCharacter> chrit = nameToChar.values().iterator();
                Map<Integer, MapleCharacter> disconnectList = new LinkedHashMap<>();
                MapleCharacter player;
                while (chrit.hasNext()) {
                    player = chrit.next();
                    if (player != null && !player.getClient().getSession().isActive()) {
                        disconnectList.put(player.getId(), player);
                    }
                }
                Iterator<MapleCharacter> dcitr = disconnectList.values().iterator();
                while (dcitr.hasNext()) {
                    player = dcitr.next();
                    if (player != null) {
                        player.getClient().disconnect(false, false);
                        player.getClient().updateLoginState(0);
                        disconnectPlayer(player);
                        dcitr.remove();
                    }
                }
            } finally {
                connectcheckReadLock.unlock();
            }
        }
    }

    public class PlayerObservable extends Observable {
        private int count;

        public int getCount() {
            return count;
        }

        public void changed() {
            this.count = nameToChar.size();
            setChanged();
            notifyObservers();
        }
    }
}
