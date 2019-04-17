/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildAlliance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.GuildPacket;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author PlayDK
 */
public class WorldAllianceService {

    private static final Logger log = LogManager.getLogger(WorldAllianceService.class.getName());
    private final Map<Integer, MapleGuildAlliance> allianceList;
    private final ReentrantReadWriteLock lock;

    private WorldAllianceService() {
        log.info("正在启动[WorldAllianceService]");
        lock = new ReentrantReadWriteLock();
        allianceList = new LinkedHashMap<>();
        Collection<MapleGuildAlliance> allGuilds = MapleGuildAlliance.loadAll();
        for (MapleGuildAlliance alliance : allGuilds) {
            allianceList.put(alliance.getId(), alliance);
        }
    }

    public static WorldAllianceService getInstance() {
        return SingletonHolder.instance;
    }

    public MapleGuildAlliance getAlliance(int allianceId) {
        MapleGuildAlliance ret = null;
        lock.readLock().lock();
        try {
            ret = allianceList.get(allianceId);
        } finally {
            lock.readLock().unlock();
        }
        if (ret == null) {
            lock.writeLock().lock();
            try {
                ret = new MapleGuildAlliance(allianceId);
                if (ret == null || ret.getId() <= 0) { //failed to load
                    return null;
                }
                allianceList.put(allianceId, ret);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return ret;
    }

    /*
     * 获得家族联盟族长ID
     */
    public int getAllianceLeader(int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            return alliance.getLeaderId();
        }
        return 0;
    }

    /*
     * 更新家族里面称号信息
     */
    public void updateAllianceRanks(int allianceId, String[] ranks) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            alliance.setRank(ranks);
        }
    }

    /*
     * 更新家族联盟公告
     */
    public void updateAllianceNotice(int allianceId, String notice) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            alliance.setNotice(notice);
        }
    }

    /*
     * 检测是否能加入家族联盟
     */
    public boolean canInvite(int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.getCapacity() > alliance.getNoGuilds();
    }

    /*
     * 家族联盟族长修改
     */
    public boolean changeAllianceLeader(int allianceId, int chrId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.setLeaderId(chrId);
    }

    /*
     * 家族联盟族长修改
     */
    public boolean changeAllianceLeader(int allianceId, int chrId, boolean sameGuild) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.setLeaderId(chrId, sameGuild);
    }

    /*
     * 家族联盟地位变更
     */
    public boolean changeAllianceRank(int allianceId, int chrId, int change) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.changeAllianceRank(chrId, change);
    }

    /*
     * 家族联盟上限修改
     */
    public boolean changeAllianceCapacity(int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.setCapacity();
    }

    /*
     * 解散家族联盟
     */
    public boolean disbandAlliance(int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.disband();
    }

    /*
     * 添加1个家族联盟信息
     */
    public boolean addGuildToAlliance(int allianceId, int guildId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.addGuild(guildId);
    }

    /*
     * 移除1个家族联盟的信息
     */
    public boolean removeGuildFromAlliance(int allianceId, int guildId, boolean expelled) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        return alliance != null && alliance.removeGuild(guildId, expelled);
    }

    /*
     * 联盟更新封包
     */
    public void sendGuild(int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            sendGuild(GuildPacket.getAllianceUpdate(alliance), -1, allianceId);
            sendGuild(GuildPacket.getGuildAlliance(alliance), -1, allianceId);
        }
    }

    public void sendGuild(byte[] packet, int exceptionId, int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                int guildId = alliance.getGuildId(i);
                if (guildId > 0 && guildId != exceptionId) {
                    WorldGuildService.getInstance().guildPacket(guildId, packet);
                }
            }
        }
    }

    /*
     * 创建1个新的家族联盟
     */
    public boolean createAlliance(String allianceName, int chrId, int chrId2, int guildId, int guildId2) {
        int allianceId = MapleGuildAlliance.createToDb(chrId, allianceName, guildId, guildId2);
        if (allianceId <= 0) {
            return false;
        }
        MapleGuild leaderGuild = WorldGuildService.getInstance().getGuild(guildId); //联盟族长的家族
        MapleGuild memberGuild = WorldGuildService.getInstance().getGuild(guildId2); //联盟成员的家族
        leaderGuild.setAllianceId(allianceId);
        memberGuild.setAllianceId(allianceId);
        leaderGuild.changeARank(true);
        memberGuild.changeARank(false);

        MapleGuildAlliance alliance = getAlliance(allianceId);

        sendGuild(GuildPacket.createGuildAlliance(alliance), -1, allianceId);
        sendGuild(GuildPacket.getAllianceInfo(alliance), -1, allianceId);
        sendGuild(GuildPacket.getGuildAlliance(alliance), -1, allianceId);
        sendGuild(GuildPacket.changeAlliance(alliance, true), -1, allianceId);
        return true;
    }

    /*
     * 联盟聊天
     */
    public void allianceChat(int guildId, String name, int chrId, String msg) {
        MapleGuild chrGuild = WorldGuildService.getInstance().getGuild(guildId);
        if (chrGuild != null) {
            MapleGuildAlliance alliance = getAlliance(chrGuild.getAllianceId());
            if (alliance != null) {
                for (int i = 0; i < alliance.getNoGuilds(); i++) {
                    MapleGuild guild = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
                    if (guild != null) {
                        guild.allianceChat(name, chrId, msg);
                    }
                }
            }
        }
    }

    public void setNewAlliance(int guildId, int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        MapleGuild guild = WorldGuildService.getInstance().getGuild(guildId);
        if (alliance != null && guild != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                if (guildId == alliance.getGuildId(i)) {
                    guild.setAllianceId(allianceId);
                    guild.broadcast(GuildPacket.getAllianceInfo(alliance));
                    guild.broadcast(GuildPacket.getGuildAlliance(alliance));
                    guild.broadcast(GuildPacket.changeAlliance(alliance, true));
                    guild.changeARank();
                    guild.writeToDB(false);
                } else {
                    MapleGuild guild_ = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
                    if (guild_ != null) {
                        guild_.broadcast(GuildPacket.addGuildToAlliance(alliance, guild));
                        guild_.broadcast(GuildPacket.changeGuildInAlliance(alliance, guild, true));
                    }
                }
            }
        }
    }

    public void setOldAlliance(int guildId, boolean expelled, int allianceId) {
        MapleGuildAlliance alliance = getAlliance(allianceId);
        MapleGuild guild_ = WorldGuildService.getInstance().getGuild(guildId);
        if (alliance != null) {
            for (int i = 0; i < alliance.getNoGuilds(); i++) {
                MapleGuild guild = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
                if (guild == null) {
                    if (guildId != alliance.getGuildId(i)) {
                        alliance.removeGuild(guildId, false, true);
                    }
                    continue; //just skip
                }
                if (guild_ == null || guildId == alliance.getGuildId(i)) {
                    guild.changeARank(5);
                    guild.setAllianceId(0);
                    guild.broadcast(GuildPacket.disbandAlliance(allianceId));
                } else if (guild_ != null) {
                    guild.broadcast(MaplePacketCreator.serverNotice(5, "[" + guild_.getName() + "] 家族退出家族联盟."));
                    guild.broadcast(GuildPacket.changeGuildInAlliance(alliance, guild_, false));
                    guild.broadcast(GuildPacket.removeGuildFromAlliance(alliance, guild_, expelled));
                }
            }
        }
        if (guildId == -1) {
            lock.writeLock().lock();
            try {
                allianceList.remove(allianceId);
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public List<byte[]> getAllianceInfo(int allianceId, boolean start) {
        List<byte[]> ret = new ArrayList<>();
        MapleGuildAlliance alliance = getAlliance(allianceId);
        if (alliance != null) {
            if (start) {
                ret.add(GuildPacket.getAllianceInfo(alliance));
                ret.add(GuildPacket.getGuildAlliance(alliance));
            }
            ret.add(GuildPacket.getAllianceUpdate(alliance));
        }
        return ret;
    }

    public void save() {
        System.out.println("正在保存家族联盟数据...");
        lock.writeLock().lock();
        try {
            for (MapleGuildAlliance alliance : allianceList.values()) {
                alliance.saveToDb();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static class SingletonHolder {

        protected static final WorldAllianceService instance = new WorldAllianceService();
    }
}
