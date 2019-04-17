/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyCharacter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author PlayDK
 */
public class WorldFamilyService {

    private static final Logger log = LogManager.getLogger(WorldFamilyService.class.getName());
    private final Map<Integer, MapleFamily> familieList;
    private final ReentrantReadWriteLock lock;

    private WorldFamilyService() {
        log.info("正在启动[WorldFamilyService]");
        lock = new ReentrantReadWriteLock();
        familieList = new LinkedHashMap<>();
    }

    public static WorldFamilyService getInstance() {
        return SingletonHolder.instance;
    }

    public void addLoadedFamily(MapleFamily family) {
        if (family.isProper()) {
            familieList.put(family.getId(), family);
        }
    }

    public MapleFamily getFamily(int id) {
        MapleFamily ret = null;
        lock.readLock().lock();
        try {
            ret = familieList.get(id);
        } finally {
            lock.readLock().unlock();
        }
        if (ret == null) {
            lock.writeLock().lock();
            try {
                ret = new MapleFamily(id);
                if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                    return null;
                }
                familieList.put(id, ret);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return ret;
    }

    public void memberFamilyUpdate(MapleFamilyCharacter familyMember, MapleCharacter chr) {
        MapleFamily family = getFamily(familyMember.getFamilyId());
        if (family != null) {
            family.memberLevelJobUpdate(chr);
        }
    }

    public void setFamilyMemberOnline(MapleFamilyCharacter familyMember, boolean isOnline, int channel) {
        MapleFamily family = getFamily(familyMember.getFamilyId());
        if (family != null) {
            family.setOnline(familyMember.getId(), isOnline, channel);
        }
    }

    public int setRep(int familyId, int chrId, int addrep, int oldLevel, String oldName) {
        MapleFamily family = getFamily(familyId);
        if (family != null) {
            return family.setRep(chrId, addrep, oldLevel, oldName);
        }
        return 0;
    }

    public void save() {
        System.out.println("正在保存学院数据...");
        lock.writeLock().lock();
        try {
            for (MapleFamily family : familieList.values()) {
                family.writeToDB(false);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void setFamily(int familyId, int seniorId, int junior1, int junior2, int currentrep, int totalrep, int chrId) {
        int ch = WorldFindService.getInstance().findChannel(chrId);
        if (ch == -1) {
            // System.out.println("ERROR: cannot find player in given channel");
            return;
        }
        MapleCharacter player = getStorage(ch).getCharacterById(chrId);
        if (player == null) {
            return;
        }
        boolean isDifferent = player.getFamilyId() != familyId || player.getSeniorId() != seniorId || player.getJunior1() != junior1 || player.getJunior2() != junior2;
        player.setFamily(familyId, seniorId, junior1, junior2);
        player.setCurrentRep(currentrep);
        player.setTotalRep(totalrep);
        if (isDifferent) {
            player.saveFamilyStatus();
        }
    }

    public void familyPacket(int familyId, byte[] message, int chrId) {
        MapleFamily family = getFamily(familyId);
        if (family != null) {
            family.broadcast(message, -1, family.getMFC(chrId).getPedigree());
        }
    }

    public void disbandFamily(int familyId) {
        MapleFamily family = getFamily(familyId);
        if (family != null) {
            lock.writeLock().lock();
            try {
                familieList.remove(familyId);
            } finally {
                lock.writeLock().unlock();
            }
            family.disbandFamily();
        }
    }

    public PlayerStorage getStorage(int channel) {
        if (channel == -20) {
            return AuctionServer.getPlayerStorage();
        } else if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    private static class SingletonHolder {

        protected static final WorldFamilyService instance = new WorldFamilyService();
    }
}
