package handling.world.guild;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.world.WorldAllianceService;
import handling.world.WorldGuildService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.GuildPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;

public class MapleGuildAlliance implements Serializable {

    public static final long serialVersionUID = 24081985245L;
    public static final int CHANGE_CAPACITY_COST = 10000000;
    private static final Logger log = LogManager.getLogger(MapleGuildAlliance.class);
    private final int[] guilds = new int[5];
    private int allianceid, leaderid, capacity; //make SQL for this auto-increment
    private String name, notice;
    private String ranks[] = new String[5];

    public MapleGuildAlliance(int id) {
        super();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM alliances WHERE id = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                allianceid = -1;
                return;
            }
            allianceid = id;
            name = rs.getString("name");
            capacity = rs.getInt("capacity");
            for (int i = 1; i < 6; i++) {
                guilds[i - 1] = rs.getInt("guild" + i);
                ranks[i - 1] = rs.getString("rank" + i);
            }
            leaderid = rs.getInt("leaderid");
            notice = rs.getString("notice");
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuildAlliance] 从数据库中加载家族联盟信息出错." + se);
        }
    }

    public static Collection<MapleGuildAlliance> loadAll() {
        Collection<MapleGuildAlliance> ret = new ArrayList<>();
        MapleGuildAlliance g;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM alliances");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                g = new MapleGuildAlliance(rs.getInt("id"));
                if (g.getId() > 0) {
                    ret.add(g);
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuildAlliance] 从数据库中加载家族联盟信息出错." + se);
        }
        return ret;
    }

    public static int createToDb(int leaderId, String name, int guild1, int guild2) {
        int ret = -1;
        if (name.length() > 12) {
            return ret;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM alliances WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.first()) {// name taken
                rs.close();
                ps.close();
                return ret;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO alliances (name, guild1, guild2, leaderid) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setInt(2, guild1);
            ps.setInt(3, guild2);
            ps.setInt(4, leaderId);
            ps.execute();
            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuildAlliance] 创建家族联盟出错." + se);
        }
        return ret;
    }

    public int getNoGuilds() {
        int ret = 0;
        for (int i = 0; i < capacity; i++) {
            if (guilds[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public boolean deleteAlliance() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps;
            for (int i = 0; i < getNoGuilds(); i++) {
                ps = con.prepareStatement("UPDATE characters SET alliancerank = 5 WHERE guildid = ?");
                ps.setInt(1, guilds[i]);
                ps.execute();
                ps.close();
            }

            ps = con.prepareStatement("DELETE FROM alliances WHERE id = ?");
            ps.setInt(1, allianceid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuildAlliance] 解散家族联盟出错." + se);
            return false;
        }
        return true;
    }

    public void broadcast(byte[] packet) {
        broadcast(packet, -1, GAOp.NONE, false);
    }

    public void broadcast(byte[] packet, int exception) {
        broadcast(packet, exception, GAOp.NONE, false);
    }

    public void broadcast(byte[] packet, int exceptionId, GAOp op, boolean expelled) {
        if (op == GAOp.DISBAND) {
            WorldAllianceService.getInstance().setOldAlliance(exceptionId, expelled, allianceid); //-1 = alliance gone, exceptionId = guild left/expelled
        } else if (op == GAOp.NEWGUILD) {
            WorldAllianceService.getInstance().setNewAlliance(exceptionId, allianceid); //exceptionId = guild that just joined
        } else {
            WorldAllianceService.getInstance().sendGuild(packet, exceptionId, allianceid); //exceptionId = guild to broadcast to only
        }
    }

    public boolean disband() {
        boolean ret = deleteAlliance();
        if (ret) {
            broadcast(null, -1, GAOp.DISBAND, false);
        }
        return ret;
    }

    public void saveToDb() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE alliances SET guild1 = ?, guild2 = ?, guild3 = ?, guild4 = ?, guild5 = ?, rank1 = ?, rank2 = ?, rank3 = ?, rank4 = ?, rank5 = ?, capacity = ?, leaderid = ?, notice = ? WHERE id = ?");
            for (int i = 0; i < 5; i++) {
                ps.setInt(i + 1, guilds[i] < 0 ? 0 : guilds[i]);
                ps.setString(i + 6, ranks[i]);
            }
            ps.setInt(11, capacity);
            ps.setInt(12, leaderid);
            ps.setString(13, notice);
            ps.setInt(14, allianceid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuildAlliance] 保存家族联盟出错." + se);
        }
    }

    public void setRank(String[] ranks) {
        this.ranks = ranks;
        broadcast(GuildPacket.getAllianceUpdate(this));
        saveToDb();
    }

    public String getRank(int rank) {
        return ranks[rank - 1];
    }

    public String[] getRanks() {
        return ranks;
    }

    public String getNotice() {
        return notice;
    }

    public void setNotice(String newNotice) {
        this.notice = newNotice;
        broadcast(GuildPacket.getAllianceUpdate(this));
        broadcast(MaplePacketCreator.serverNotice(5, "* 联盟公告 : " + newNotice));
        saveToDb();
    }

    public int getGuildId(int i) {
        return guilds[i];
    }

    public int getId() {
        return allianceid;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public boolean setCapacity() {
        if (capacity >= 5) {
            return false;
        }
        this.capacity += 1;
        broadcast(GuildPacket.getAllianceUpdate(this));
        saveToDb();
        return true;
    }

    public boolean addGuild(int guildid) {
        if (getNoGuilds() >= getCapacity()) {
            return false;
        }
        guilds[getNoGuilds()] = guildid;
        saveToDb();
        broadcast(null, guildid, GAOp.NEWGUILD, false);
        return true;
    }

    public boolean removeGuild(int guildid, boolean expelled) {
        return removeGuild(guildid, expelled, false);
    }

    public boolean removeGuild(int guildid, boolean expelled, boolean isNull) {
        for (int i = 0; i < getNoGuilds(); i++) {
            if (guilds[i] == guildid) {
                if (!isNull) {
                    broadcast(null, guildid, GAOp.DISBAND, expelled);
                }
                if (i > 0 && i != getNoGuilds() - 1) { // if guild isnt the last guild.. damnit
                    for (int x = i + 1; x < getNoGuilds(); x++) {
                        if (guilds[x] > 0) {
                            guilds[x - 1] = guilds[x];
                            if (x == getNoGuilds() - 1) {
                                guilds[x] = -1;
                            }
                        }
                    }
                } else {
                    guilds[i] = -1;
                }
                if (i == 0) { //leader guild.. FUCK THIS ALLIANCE! xD
                    return disband();
                } else {
                    broadcast(GuildPacket.getAllianceUpdate(this));
                    broadcast(GuildPacket.getGuildAlliance(this));
                    saveToDb();
                    return true;
                }
            }
        }
        return false;
    }

    public int getLeaderId() {
        return leaderid;
    }

    public boolean setLeaderId(int c) {
        return setLeaderId(c, false);
    }

    public boolean setLeaderId(int c, boolean sameGuild) {
        if (leaderid == c) {
            return false;
        }
        //re-arrange the guilds so guild1 is always the leader guild
        int g = -1; //this shall be leader
        String leaderName = null;
        for (int i = 0; i < getNoGuilds(); i++) {
            MapleGuild g_ = WorldGuildService.getInstance().getGuild(guilds[i]);
            if (g_ != null) {
                MapleGuildCharacter newLead = g_.getMGC(c);
                MapleGuildCharacter oldLead = g_.getMGC(leaderid);
                if (newLead != null && oldLead != null && !sameGuild) { //same guild
                    return false;
                }
                if (newLead != null && newLead.getGuildRank() == 1 && newLead.getAllianceRank() == 2) { //guild1 should always be leader so no worries about g being -1
                    g_.changeARank(c, 1);
                    g = i;
                    leaderName = newLead.getName();
                }
                if (oldLead != null && oldLead.getGuildRank() == 1 && oldLead.getAllianceRank() == 1) {
                    g_.changeARank(leaderid, 2);
                }
            }
        }
        if (g == -1) {
            return false; //nothing was done
        }
        int oldGuild = guilds[g];
        guilds[g] = guilds[0];
        guilds[0] = oldGuild;
        if (leaderName != null) {
            broadcast(MaplePacketCreator.serverNotice(5, "* 联盟公告 : " + leaderName + " 成为了家族联盟族长."));
        }
        broadcast(GuildPacket.changeAllianceLeader(allianceid, leaderid, c));
        broadcast(GuildPacket.updateAllianceLeader(allianceid, leaderid, c));
        broadcast(GuildPacket.getAllianceUpdate(this));
        broadcast(GuildPacket.getGuildAlliance(this));
        this.leaderid = c;
        saveToDb();
        return true;
    }

    public boolean changeAllianceRank(int chrId, int change) {
        if (leaderid == chrId || change < 0 || change > 1) {
            return false;
        }
        for (int i = 0; i < getNoGuilds(); i++) {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(guilds[i]);
            if (guild != null) {
                MapleGuildCharacter chr = guild.getMGC(chrId);
                if (chr != null && chr.getAllianceRank() > 2) {
                    if ((change == 0 && chr.getAllianceRank() >= 5) || (change == 1 && chr.getAllianceRank() <= 3)) {
                        return false;
                    }
                    guild.changeARank(chrId, chr.getAllianceRank() + (change == 0 ? 1 : -1));
                    return true;
                }
            }
        }
        return false;
    }

    private enum GAOp {

        NONE, DISBAND, NEWGUILD
    }
}
