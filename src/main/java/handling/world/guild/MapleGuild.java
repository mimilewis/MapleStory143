package handling.world.guild;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.skills.SkillFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.world.WorldAllianceService;
import handling.world.WorldBroadcastService;
import handling.world.WorldGuildService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleStatEffect;
import tools.MaplePacketCreator;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.packet.ChatPacket;
import tools.packet.GuildPacket;
import tools.packet.PacketHelper;
import tools.packet.UIPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapleGuild implements Serializable {

    public static final long serialVersionUID = 6322150443228168192L;
    private static final Logger log = LogManager.getLogger(MapleGuild.class);
    private final List<MapleGuildCharacter> members = new CopyOnWriteArrayList<>(); //家族成员的信息
    private final List<MapleGuildCharacter> applyMembers = new ArrayList<>(); //等待加入的家族成员信息
    private final Map<Integer, MapleGuildSkill> guildSkills = new HashMap<>();
    private final String rankTitles[] = new String[5]; // 1 = master, 2 = jr, 5 = lowest member
    private final Map<Integer, MapleBBSThread> bbs = new HashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final int[] guildExp = {0, 15000, 60000, 135000, 240000, 375000, 540000, 735000, 960000, 1215000, 1500000, 1815000, 2160000, 2535000, 2940000, 3375000, 3840000, 4335000, 4860000, 5415000, 6000000, 6615000, 7260000, 7935000, 8640000};
    private String name, notice;
    private int id, gp, contribution, logo, logoColor, leader, capacity, logoBG, logoBGColor, signature, level;
    private boolean bDirty = true, proper = true;
    private int allianceid = 0, invitedid = 0;
    private boolean init = false, changed = false, changed_skills = false;

    public MapleGuild(int guildid) {
        this(guildid, null);
    }

    public MapleGuild(int guildid, Map<Integer, Map<Integer, MapleBBSReply>> replies) {
        super();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM guilds WHERE guildid = ?");
            ps.setInt(1, guildid);
            ResultSet rs = ps.executeQuery();
            if (!rs.first()) {
                rs.close();
                ps.close();
                id = -1;
                return;
            }
            id = guildid;
            name = rs.getString("name");
            gp = rs.getInt("GP");
            logo = rs.getInt("logo");
            logoColor = rs.getInt("logoColor");
            logoBG = rs.getInt("logoBG");
            logoBGColor = rs.getInt("logoBGColor");
            capacity = rs.getInt("capacity");
            rankTitles[0] = rs.getString("rank1title");
            rankTitles[1] = rs.getString("rank2title");
            rankTitles[2] = rs.getString("rank3title");
            rankTitles[3] = rs.getString("rank4title");
            rankTitles[4] = rs.getString("rank5title");
            leader = rs.getInt("leader");
            notice = rs.getString("notice");
            signature = rs.getInt("signature");
            allianceid = rs.getInt("alliance");
            rs.close();
            ps.close();

            MapleGuildAlliance alliance = WorldAllianceService.getInstance().getAlliance(allianceid);
            if (alliance == null) {
                allianceid = 0;
            }

            ps = con.prepareStatement("SELECT id, name, level, job, guildrank, guildContribution, alliancerank FROM characters WHERE guildid = ? ORDER BY guildrank ASC, name ASC", ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            if (!rs.first()) {
                System.err.println("家族ID: " + id + " 没用成员，系统自动解散该家族。");
                rs.close();
                ps.close();
                writeToDB(true);
                proper = false;
                return;
            }
            boolean leaderCheck = false;
            byte gFix = 0, aFix = 0;
            do {
                int chrId = rs.getInt("id");
                byte gRank = rs.getByte("guildrank"), aRank = rs.getByte("alliancerank");

                if (chrId == leader) {
                    leaderCheck = true;
                    if (gRank != 1) { //needs updating to 1
                        gRank = 1;
                        gFix = 1;
                    }
                    if (alliance != null) {
                        if (alliance.getLeaderId() == chrId && aRank != 1) {
                            aRank = 1;
                            aFix = 1;
                        } else if (alliance.getLeaderId() != chrId && aRank != 2) {
                            aRank = 2;
                            aFix = 2;
                        }
                    }
                } else {
                    if (gRank == 1) {
                        gRank = 2;
                        gFix = 2;
                    }
                    if (aRank < 3) {
                        aRank = 3;
                        aFix = 3;
                    }
                }
                members.add(new MapleGuildCharacter(chrId, rs.getShort("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), gRank, rs.getInt("guildContribution"), aRank, guildid, false));
            } while (rs.next());
            rs.close();
            ps.close();

            if (!leaderCheck) {
                System.err.println("族长[ " + leader + " ]没有在家族ID为 " + id + " 的家族中，系统自动解散这个家族。");
                writeToDB(true);
                proper = false;
                return;
            }

            if (gFix > 0) {
                ps = con.prepareStatement("UPDATE characters SET guildrank = ? WHERE id = ?");
                ps.setByte(1, gFix);
                ps.setInt(2, leader);
                ps.executeUpdate();
                ps.close();
            }

            if (aFix > 0) {
                ps = con.prepareStatement("UPDATE characters SET alliancerank = ? WHERE id = ?");
                ps.setByte(1, aFix);
                ps.setInt(2, leader);
                ps.executeUpdate();
                ps.close();
            }

            ps = con.prepareStatement("SELECT * FROM bbs_threads WHERE guildid = ? ORDER BY localthreadid DESC");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            while (rs.next()) {
                int tID = rs.getInt("localthreadid");
                MapleBBSThread thread = new MapleBBSThread(tID, rs.getString("name"), rs.getString("startpost"), rs.getLong("timestamp"), guildid, rs.getInt("postercid"), rs.getInt("icon"));
                if (replies != null && replies.containsKey(rs.getInt("threadid"))) {
                    thread.replies.putAll(replies.get(rs.getInt("threadid")));
                }
                bbs.put(tID, thread);
            }
            rs.close();
            ps.close();

            ps = con.prepareStatement("SELECT * FROM guildskills WHERE guildid = ?");
            ps.setInt(1, guildid);
            rs = ps.executeQuery();
            while (rs.next()) {
                int skillId = rs.getInt("skillid");
                if (skillId < 91000000) { //hack
                    rs.close();
                    ps.close();
                    System.err.println("非家族技能ID: " + skillId + " 在家族ID为 " + id + " 的家族中，系统自动解散该家族。");
                    writeToDB(true);
                    proper = false;
                    return;
                }
                guildSkills.put(skillId, new MapleGuildSkill(skillId, rs.getInt("level"), rs.getLong("timestamp"), rs.getString("purchaser"), "")); //activators not saved
            }
            rs.close();
            ps.close();
            level = calculateLevel();
        } catch (SQLException se) {
            log.error("[MapleGuild] 从数据库中加载家族信息出错." + se);
        }
    }

    public static void loadAll() {
        Map<Integer, Map<Integer, MapleBBSReply>> replies = new LinkedHashMap<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_replies");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int tID = rs.getInt("threadid");
                Map<Integer, MapleBBSReply> reply = replies.computeIfAbsent(tID, k -> new HashMap<>());
                reply.put(reply.size(), new MapleBBSReply(reply.size(), rs.getInt("postercid"), rs.getString("content"), rs.getLong("timestamp")));
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("SELECT guildid FROM guilds");
            rs = ps.executeQuery();
            while (rs.next()) {
                WorldGuildService.getInstance().addLoadedGuild(new MapleGuild(rs.getInt("guildid"), replies));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleGuild] 从数据库中加载家族信息出错." + se);
        }
    }

    public static void loadAll(Object toNotify) {
        Map<Integer, Map<Integer, MapleBBSReply>> replies = new LinkedHashMap<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM bbs_replies");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int tID = rs.getInt("threadid");
                Map<Integer, MapleBBSReply> reply = replies.computeIfAbsent(tID, k -> new HashMap<>());
                reply.put(reply.size(), new MapleBBSReply(reply.size(), rs.getInt("postercid"), rs.getString("content"), rs.getLong("timestamp")));
            }
            rs.close();
            ps.close();
            boolean cont = false;
            ps = con.prepareStatement("SELECT guildid FROM guilds");
            rs = ps.executeQuery();
            while (rs.next()) {
                GuildLoad.QueueGuildForLoad(rs.getInt("guildid"), replies);
                cont = true;
            }
            rs.close();
            ps.close();
            if (!cont) {
                return;
            }
        } catch (SQLException se) {
            log.error("[MapleGuild] 从数据库中加载家族信息出错." + se);
        }
        AtomicInteger FinishedThreads = new AtomicInteger(0);
        GuildLoad.Execute(toNotify);
        synchronized (toNotify) {
            try {
                toNotify.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        while (FinishedThreads.incrementAndGet() != GuildLoad.NumSavingThreads) {
            synchronized (toNotify) {
                try {
                    toNotify.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /*
     * 创建1个新的家族信息
     */
    public static int createGuild(int leaderId, String name) {
        if (name.length() > 12) {
            return 0;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT guildid FROM guilds WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();

            if (rs.first()) { // 家族名字重复
                rs.close();
                ps.close();
                return 0;
            }
            ps.close();
            rs.close();

            ps = con.prepareStatement("INSERT INTO guilds (`leader`, `name`, `signature`, `alliance`) VALUES (?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, leaderId);
            ps.setString(2, name);
            ps.setInt(3, (int) (System.currentTimeMillis() / 1000));
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            int ret = 0;
            if (rs.next()) {
                ret = rs.getInt(1);
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException se) {
            log.error("[MapleGuild] 创建家族信息出错." + se);
            return 0;
        }
    }

    /*
     * 家族邀请
     */
    public static MapleGuildResponse sendInvite(MapleClient c, String targetName) {
        MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targetName);
        if (target == null) {
            return MapleGuildResponse.找不到角色;
        }
        if (target.getGuildId() > 0) {
            return MapleGuildResponse.已经有家族;
        }
        if (c.getPlayer().getGuild().getMembers().size() >= c.getPlayer().getGuild().getCapacity()) {
            return MapleGuildResponse.家族人数已满;
        }
        c.announce(MaplePacketCreator.showRedNotice("已邀请'" + targetName + "'加入公会。"));
        target.getClient().announce(GuildPacket.guildInvite(c.getPlayer().getGuildId(), c.getPlayer().getName(), c.getPlayer().getLevel(), c.getPlayer().getJob()));
        return null;
    }

    public static void setOfflineGuildStatus(int guildId, byte guildrank, int contribution, byte alliancerank, int chrId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET guildid = ?, guildrank = ?, guildContribution = ?, alliancerank = ? WHERE id = ?");
            ps.setInt(1, guildId);
            ps.setInt(2, guildrank);
            ps.setInt(3, contribution);
            ps.setInt(4, alliancerank);
            ps.setInt(5, chrId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
        }
    }

    public boolean isProper() {
        return proper;
    }

    public final void writeToDB(boolean bDisband) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            if (!bDisband) {
                StringBuilder buf = new StringBuilder("UPDATE guilds SET GP = ?, logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ?, ");
                for (int i = 1; i < 6; i++) {
                    buf.append("rank").append(i).append("title = ?, ");
                }
                buf.append("capacity = ?, notice = ?, alliance = ?, leader = ? WHERE guildid = ?");

                PreparedStatement ps = con.prepareStatement(buf.toString());
                ps.setInt(1, gp);
                ps.setInt(2, logo);
                ps.setInt(3, logoColor);
                ps.setInt(4, logoBG);
                ps.setInt(5, logoBGColor);
                ps.setString(6, rankTitles[0]);
                ps.setString(7, rankTitles[1]);
                ps.setString(8, rankTitles[2]);
                ps.setString(9, rankTitles[3]);
                ps.setString(10, rankTitles[4]);
                ps.setInt(11, capacity);
                ps.setString(12, notice);
                ps.setInt(13, allianceid);
                ps.setInt(14, leader);
                ps.setInt(15, id);
                ps.executeUpdate();
                ps.close();

                if (changed) {
                    ps = con.prepareStatement("DELETE FROM bbs_threads WHERE guildid = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    ps.close();

                    ps = con.prepareStatement("DELETE FROM bbs_replies WHERE guildid = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    ps.close();

                    PreparedStatement pse = con.prepareStatement("INSERT INTO bbs_replies (`threadid`, `postercid`, `timestamp`, `content`, `guildid`) VALUES (?, ?, ?, ?, ?)");
                    ps = con.prepareStatement("INSERT INTO bbs_threads(`postercid`, `name`, `timestamp`, `icon`, `startpost`, `guildid`, `localthreadid`) VALUES(?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
                    ps.setInt(6, id);
                    for (MapleBBSThread bb : bbs.values()) {
                        ps.setInt(1, bb.ownerID);
                        ps.setString(2, bb.name);
                        ps.setLong(3, bb.timestamp);
                        ps.setInt(4, bb.icon);
                        ps.setString(5, bb.text);
                        ps.setInt(7, bb.localthreadID);
                        ps.execute();
                        ResultSet rs = ps.getGeneratedKeys();
                        if (!rs.next()) {
                            rs.close();
                            continue;
                        }
                        int ourId = rs.getInt(1);
                        rs.close();
                        pse.setInt(5, id);
                        for (MapleBBSReply r : bb.replies.values()) {
                            pse.setInt(1, ourId);
                            pse.setInt(2, r.ownerID);
                            pse.setLong(3, r.timestamp);
                            pse.setString(4, r.content);
                            pse.addBatch();
                        }
                    }
                    pse.executeBatch();
                    pse.close();
                    ps.close();
                }
                if (changed_skills) {
                    ps = con.prepareStatement("DELETE FROM guildskills WHERE guildid = ?");
                    ps.setInt(1, id);
                    ps.execute();
                    ps.close();

                    ps = con.prepareStatement("INSERT INTO guildskills(`guildid`, `skillid`, `level`, `timestamp`, `purchaser`) VALUES(?, ?, ?, ?, ?)");
                    ps.setInt(1, id);
                    for (MapleGuildSkill i : guildSkills.values()) {
                        ps.setInt(2, i.skillID);
                        ps.setByte(3, (byte) i.level);
                        ps.setLong(4, i.timestamp);
                        ps.setString(5, i.purchaser);
                        ps.execute();
                    }
                    ps.close();
                }
                changed_skills = false;
                changed = false;
            } else {
                PreparedStatement ps = con.prepareStatement("DELETE FROM bbs_threads WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();


                ps = con.prepareStatement("DELETE FROM bbs_replies WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();

                ps = con.prepareStatement("DELETE FROM guildskills WHERE guildid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();

                ps = con.prepareStatement("DELETE FROM guilds WHERE guildid = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
                ps.close();


                if (allianceid > 0) {
                    MapleGuildAlliance alliance = WorldAllianceService.getInstance().getAlliance(allianceid);
                    if (alliance != null) {
                        alliance.removeGuild(id, false);
                    }
                }

                broadcast(GuildPacket.guildDisband(id));
            }
        } catch (SQLException se) {
            log.error("[MapleGuild] 保存家族信息出错." + se);
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leader;
    }

    public MapleCharacter getLeader(MapleClient c) {
        return c.getChannelServer().getPlayerStorage().getCharacterById(leader);
    }

    /*
     * 家族的GP点数
     */
    public int getGP() {
        return gp;
    }

    /*
     * 家族的贡献点数
     */
    public int getGontribution() {
        return contribution;
    }

    public int getLogo() {
        return logo;
    }

    public void setLogo(int l) {
        logo = l;
    }

    public int getLogoColor() {
        return logoColor;
    }

    public void setLogoColor(int c) {
        logoColor = c;
    }

    public int getLogoBG() {
        return logoBG;
    }

    public void setLogoBG(int bg) {
        logoBG = bg;
    }

    public int getLogoBGColor() {
        return logoBGColor;
    }

    public void setLogoBGColor(int c) {
        logoBGColor = c;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getSignature() {
        return signature;
    }

    public void broadcast(byte[] packet) {
        broadcast(packet, -1, BCOp.NONE);
    }

    public void broadcast(byte[] packet, int exception) {
        broadcast(packet, exception, BCOp.NONE);
    }

    public void broadcast(byte[] packet, int exceptionId, BCOp bcop) {
        lock.writeLock().lock();
        try {
            buildNotifications();
        } finally {
            lock.writeLock().unlock();
        }

        lock.readLock().lock();
        try {
            for (MapleGuildCharacter mgc : members) {
                if (bcop == BCOp.DISBAND) {
                    if (mgc.isOnline()) {
                        WorldGuildService.getInstance().setGuildAndRank(mgc.getId(), 0, 5, 0, 5);
                    } else {
                        setOfflineGuildStatus(0, (byte) 5, 0, (byte) 5, mgc.getId());
                    }
                } else if (mgc.isOnline() && mgc.getId() != exceptionId || mgc.isOnline() && bcop == BCOp.CHAT) {
                    if (bcop == BCOp.EMBELMCHANGE) {
                        WorldGuildService.getInstance().changeEmblem(id, mgc.getId(), this);
                    } else {
                        WorldBroadcastService.getInstance().sendGuildPacket(mgc.getId(), packet, exceptionId, id, bcop == BCOp.CHAT);
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void buildNotifications() {
        if (!bDirty) {
            return;
        }
        List<Integer> mem = new LinkedList<>();
        for (MapleGuildCharacter mgc : members) {
            if (!mgc.isOnline()) {
                continue;
            }
            if (mem.contains(mgc.getId()) || mgc.getGuildId() != id) {
                members.remove(mgc);
                continue;
            }
            mem.add(mgc.getId());
        }
        bDirty = false;
    }

    public void setOnline(int chrId, boolean online, int channel) {
        boolean isBroadcast = true;
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getGuildId() == id && mgc.getId() == chrId) {
                if (mgc.isOnline() == online) {
                    isBroadcast = false;
                }
                mgc.setOnline(online);
                mgc.setChannel((byte) channel);
                break;
            }
        }
        if (isBroadcast) {
            broadcast(GuildPacket.guildMemberOnline(id, chrId, online), chrId);
//            if (allianceid > 0) {
//                WorldAllianceService.getInstance().sendGuild(GuildPacket.allianceMemberOnline(allianceid, id, chrId, online), id, allianceid);
//            }
        }
        bDirty = true; // member formation has changed, update notifications
        init = true;
    }

    /*
     * 家族聊天
     */
    public void guildChat(int accId, int guildId, int chrId, String msg) {
        broadcast(ChatPacket.guildChat(accId, guildId, chrId, msg), chrId, BCOp.CHAT);
    }

    /*
     * 联盟聊天
     */
    public void allianceChat(String name, int chrId, String msg) {
        broadcast(MaplePacketCreator.multiChat(name, msg, 3), chrId);
    }

    public String getRankTitle(int rank) {
        return rankTitles[rank - 1];
    }

    public int getAllianceId() {
        return this.allianceid;
    }

    public void setAllianceId(int a) {
        this.allianceid = a;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET alliance = ? WHERE guildid = ?");
            ps.setInt(1, a);
            ps.setInt(2, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            log.error("[MapleGuild] 保存家族联盟信息出错." + e);
        }
    }

    public int getInvitedId() {
        return this.invitedid;
    }

    public void setInvitedId(int iid) {
        this.invitedid = iid;
    }

    /*
     * 添加1个新的家族成员
     */
    public int addGuildMember(MapleGuildCharacter newMember) {
        lock.writeLock().lock();
        try {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(newMember.getName()) < 0) {
                    members.add(i + 1, newMember);
                    bDirty = true;
                    break;
                }
            }
            //删除临时列表
            for (MapleGuildCharacter mgcc : applyMembers) {
                if (mgcc.getId() == newMember.getId()) {
                    applyMembers.remove(mgcc);
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        broadcast(GuildPacket.newGuildMember(newMember, false)); //发送新的玩家加入家族的信息
        gainGP(500, true, newMember.getId());
        if (allianceid > 0) {
            WorldAllianceService.getInstance().sendGuild(allianceid);
        }
        return 1;
    }

    public int addGuildMember(int chrId) {
        MapleGuildCharacter newMember = getApplyMGC(chrId);
        if (newMember == null) {
            return 0;
        }
        applyMembers.remove(newMember);
        //开始加入家族
        lock.writeLock().lock();
        try {
            if (members.size() >= capacity) {
                return 0;
            }
            for (int i = members.size() - 1; i >= 0; i--) {
                if (members.get(i).getGuildRank() < 5 || members.get(i).getName().compareTo(newMember.getName()) < 0) {
                    members.add(i + 1, newMember);
                    bDirty = true;
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        gainGP(500, true, newMember.getId());
        broadcast(GuildPacket.newGuildMember(newMember, false)); //发送新的玩家加入家族的信息
        if (allianceid > 0) {
            WorldAllianceService.getInstance().sendGuild(allianceid);
        }
        return 1;
    }

    /*
     * 添加1个成员的申请列表
     */
    public int addGuildApplyMember(MapleGuildCharacter applyMember) {
        if (getApplyMGC(applyMember.getId()) != null) {
            return 0;
        }
        applyMembers.add(applyMember);
        broadcast(GuildPacket.newGuildMember(applyMember, true)); //发送新的玩家加入家族的信息
        return 1;
    }

    /*
     * 拒绝玩家的家族申请
     * denyGuildApplyMember
     */
    public int denyGuildApplyMember(int fromId) {
        MapleGuildCharacter applyMember = getApplyMGC(fromId);
        if (applyMember == null) {
            return 0;
        }
        applyMembers.remove(applyMember);
        return 1;
    }

    /*
     * 玩家自己离开家族
     */
    public void leaveGuild(MapleGuildCharacter mgc) {
        lock.writeLock().lock();
        try {
            for (MapleGuildCharacter mgcc : members) {
                if (mgcc.getId() == mgc.getId()) {
                    broadcast(GuildPacket.memberLeft(mgcc, false));
                    bDirty = true;
                    gainGP(mgcc.getGuildContribution() > 0 ? -mgcc.getGuildContribution() : -500);
                    members.remove(mgcc);
                    if (mgc.isOnline()) {
                        WorldGuildService.getInstance().setGuildAndRank(mgcc.getId(), 0, 5, 0, 5);
                    } else {
                        setOfflineGuildStatus(0, (byte) 5, 0, (byte) 5, mgcc.getId());
                    }
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (bDirty && allianceid > 0) {
            WorldAllianceService.getInstance().sendGuild(allianceid);
        }
    }

    /*
     * 家族驱除玩家
     */
    public void expelMember(MapleGuildCharacter initiator, String name, int chrId) {
        lock.writeLock().lock();
        try {
            for (MapleGuildCharacter mgc : members) {
                if (mgc.getId() == chrId && initiator.getGuildRank() < mgc.getGuildRank()) {
                    broadcast(GuildPacket.memberLeft(mgc, true));
                    bDirty = true;
                    gainGP(mgc.getGuildContribution() > 0 ? -mgc.getGuildContribution() : -500);
                    if (mgc.isOnline()) {
                        WorldGuildService.getInstance().setGuildAndRank(chrId, 0, 5, 0, 5);
                    } else {
                        MapleCharacterUtil.sendNote(mgc.getName(), initiator.getName(), "被家族除名了。", 0);
                        setOfflineGuildStatus(0, (byte) 5, 0, (byte) 5, chrId);
                    }
                    members.remove(mgc);
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
        if (bDirty && allianceid > 0) {
            WorldAllianceService.getInstance().sendGuild(allianceid);
        }
    }

    public void changeARank() {
        changeARank(false);
    }

    public void changeARank(boolean leader) {
        if (allianceid <= 0) {
            return;
        }
        for (MapleGuildCharacter mgc : members) {
            byte newRank = 3;
            if (this.leader == mgc.getId()) {
                newRank = (byte) (leader ? 1 : 2);
            }
            if (mgc.isOnline()) {
                WorldGuildService.getInstance().setGuildAndRank(mgc.getId(), this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
            } else {
                setOfflineGuildStatus(this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank, mgc.getId());
            }
            mgc.setAllianceRank(newRank);
        }
        WorldAllianceService.getInstance().sendGuild(allianceid);
    }

    public void changeARank(int newRank) {
        if (allianceid <= 0) {
            return;
        }
        for (MapleGuildCharacter mgc : members) {
            if (mgc.isOnline()) {
                WorldGuildService.getInstance().setGuildAndRank(mgc.getId(), this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
            } else {
                setOfflineGuildStatus(this.id, mgc.getGuildRank(), mgc.getGuildContribution(), (byte) newRank, mgc.getId());
            }
            mgc.setAllianceRank((byte) newRank);
        }
        WorldAllianceService.getInstance().sendGuild(allianceid);
    }

    public boolean changeARank(int chrId, int newRank) {
        if (allianceid <= 0) {
            return false;
        }
        for (MapleGuildCharacter mgc : members) {
            if (chrId == mgc.getId()) {
                if (mgc.isOnline()) {
                    WorldGuildService.getInstance().setGuildAndRank(chrId, this.id, mgc.getGuildRank(), mgc.getGuildContribution(), newRank);
                } else {
                    setOfflineGuildStatus(this.id, mgc.getGuildRank(), mgc.getGuildContribution(), (byte) newRank, chrId);
                }
                mgc.setAllianceRank((byte) newRank);
                WorldAllianceService.getInstance().sendGuild(allianceid);
                return true;
            }
        }
        return false;
    }

    /*
     * 改变家族族长
     */
    public void changeGuildLeader(int newLeader) {
        if (changeRank(newLeader, 1) && changeRank(leader, 2)) {
            if (allianceid > 0) {
                int aRank = getMGC(leader).getAllianceRank();
                if (aRank == 1) {
                    WorldAllianceService.getInstance().changeAllianceLeader(allianceid, newLeader, true);
                } else {
                    changeARank(newLeader, aRank);
                }
                changeARank(leader, 3);
            }
            broadcast(GuildPacket.guildLeaderChanged(id, leader, newLeader, allianceid));
            this.leader = newLeader;
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                PreparedStatement ps = con.prepareStatement("UPDATE guilds SET leader = ? WHERE guildid = ?");
                ps.setInt(1, newLeader);
                ps.setInt(2, id);
                ps.execute();
                ps.close();
            } catch (SQLException e) {
                log.error("[MapleGuild] Saving leaderid ERROR." + e);
            }
        }
    }

    /*
     * 改变玩家的头衔
     */
    public boolean changeRank(int chrId, int newRank) {
        for (MapleGuildCharacter mgc : members) {
            if (chrId == mgc.getId()) {
                if (mgc.isOnline()) {
                    WorldGuildService.getInstance().setGuildAndRank(chrId, this.id, newRank, mgc.getGuildContribution(), mgc.getAllianceRank());
                } else {
                    setOfflineGuildStatus(this.id, (byte) newRank, mgc.getGuildContribution(), mgc.getAllianceRank(), chrId);
                }
                mgc.setGuildRank((byte) newRank);
                broadcast(GuildPacket.changeRank(mgc));
                return true;
            }
        }
        return false;
    }

    /*
     * 设置家族公告
     */
    public void setGuildNotice(String notice) {
        this.notice = notice;
        broadcast(GuildPacket.guildNotice(id, notice));
    }

    public void memberLevelJobUpdate(MapleGuildCharacter mgc) {
        for (MapleGuildCharacter member : members) {
            if (member.getId() == mgc.getId()) {
                int old_level = member.getLevel();
                int old_job = member.getJobId();
                member.setJobId(mgc.getJobId());
                member.setLevel((short) mgc.getLevel());
                if (mgc.getLevel() > old_level) {
                    gainGP((mgc.getLevel() - old_level) * mgc.getLevel(), false, mgc.getId());
                    //aftershock: formula changes (below 100 = 40, above 100 = 80) (12000 max) but i prefer level (21100 max), add guildContribution, do setGuildAndRank or just get the MapleCharacter object
                }
                if (old_level != mgc.getLevel()) {
                    broadcast(MaplePacketCreator.sendLevelup(false, mgc.getLevel(), mgc.getName()), mgc.getId());
                }
                if (old_job != mgc.getJobId()) {
                    broadcast(MaplePacketCreator.sendJobup(false, mgc.getJobId(), mgc.getName()), mgc.getId());
                }
                broadcast(GuildPacket.guildMemberLevelJobUpdate(mgc));
                if (allianceid > 0) {
                    WorldAllianceService.getInstance().sendGuild(GuildPacket.updateAlliance(mgc, allianceid), id, allianceid);
                }
                break;
            }
        }
    }

    /*
     * 修改家族称号的名称
     */
    public void changeRankTitle(String[] ranks) {
        System.arraycopy(ranks, 0, rankTitles, 0, 5);
        broadcast(GuildPacket.rankTitleChange(id, ranks));
    }

    /*
     * 解散家族
     */
    public void disbandGuild() {
        writeToDB(true);
        broadcast(null, -1, BCOp.DISBAND);
    }

    /*
     * 修改家族的标识
     */
    public void setGuildEmblem(short bg, byte bgcolor, short logo, byte logocolor) {
        this.logoBG = bg;
        this.logoBGColor = bgcolor;
        this.logo = logo;
        this.logoColor = logocolor;
        broadcast(null, -1, BCOp.EMBELMCHANGE);

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET logo = ?, logoColor = ?, logoBG = ?, logoBGColor = ? WHERE guildid = ?");
            ps.setInt(1, logo);
            ps.setInt(2, logoColor);
            ps.setInt(3, logoBG);
            ps.setInt(4, logoBGColor);
            ps.setInt(5, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            log.error("[MapleGuild] Saving guild logo / BG colo ERROR." + e);
        }
    }

    /*
     * 获得家族成员的信息
     */
    public MapleGuildCharacter getMGC(int chrId) {
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == chrId) {
                return mgc;
            }
        }
        return null;
    }

    /*
     * 获取临时申请的成员信息
     */
    public MapleGuildCharacter getApplyMGC(int chrId) {
        for (MapleGuildCharacter mgc : applyMembers) {
            if (mgc.getId() == chrId) {
                return mgc;
            }
        }
        return null;
    }

    /*
     * 增加家族人数上限
     */
    public boolean increaseCapacity(boolean trueMax) {
        if (capacity >= (trueMax ? 200 : 100) || ((capacity + 5) > (trueMax ? 200 : 100))) {
            return false;
        }
        if (trueMax && gp < 25000) {
            return false;
        }
        if (trueMax && gp - 25000 < getGuildExpNeededForLevel(getLevel() - 1)) {
            return false;
        }
        capacity += 5;
        broadcast(GuildPacket.guildCapacityChange(this.id, this.capacity));

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE guilds SET capacity = ? WHERE guildid = ?");
            ps.setInt(1, this.capacity);
            ps.setInt(2, this.id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            log.error("[MapleGuild] Saving guild capacity ERROR." + e);
        }
        return true;
    }

    /*
     * 获得家族GP点数
     */
    public void gainGP(int amount) {
        gainGP(amount, true, -1);
    }

    public void gainGP(int amount, boolean broadcast) {
        gainGP(amount, broadcast, -1);
    }

    public void gainGP(int amount, boolean broadcast, int chrId) {
        if (amount == 0) {
            return;
        }
        if (amount + gp < 0) {
            amount = -gp;
        }
        gp += amount;
        level = calculateLevel();
        broadcast(GuildPacket.updateGuildInfo(id, gp, level)); //更新家族的总贡献和GP信息
        if (chrId > 0 && amount > 0) {
            MapleGuildCharacter mgc = getMGC(chrId);
            if (mgc != null) {
                mgc.setGuildContribution(mgc.getGuildContribution() + amount);
                if (mgc.isOnline()) {
                    WorldGuildService.getInstance().setGuildAndRank(chrId, this.id, mgc.getGuildRank(), mgc.getGuildContribution(), mgc.getAllianceRank());
                } else {
                    setOfflineGuildStatus(this.id, mgc.getGuildRank(), mgc.getGuildContribution(), mgc.getAllianceRank(), chrId);
                }
                broadcast(GuildPacket.updatePlayerContribution(id, chrId, mgc.getGuildContribution())); //发送更新玩家的贡献和IGP信息
            }
        }
        if (broadcast) {
            broadcast(UIPacket.getGPMsg(amount));
        }
    }

    public Collection<MapleGuildSkill> getSkills() {
        return guildSkills.values();
    }

    public int getSkillLevel(int skillId) {
        if (!guildSkills.containsKey(skillId)) {
            return 0;
        }
        return guildSkills.get(skillId).level;
    }

    /*
     * 激活家族技能
     */
    public boolean activateSkill(int skillId, String name) {
        if (!guildSkills.containsKey(skillId)) {
            return false;
        }
        MapleGuildSkill ourSkill = guildSkills.get(skillId);
        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(ourSkill.level);
        if (ourSkill.timestamp > System.currentTimeMillis() || effect.getPeriod() <= 0) {
            return false;
        }
        ourSkill.timestamp = System.currentTimeMillis() + (effect.getPeriod() * 60000L);
        ourSkill.activator = name;
        broadcast(GuildPacket.guildSkillPurchased(id, skillId, ourSkill.level, ourSkill.timestamp, ourSkill.purchaser, name));
        return true;
    }

    /*
     * 购买家族技能
     */
    public boolean purchaseSkill(int skillId, String name, int chrId) {
        MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(getSkillLevel(skillId) + 1);
        if (effect.getReqGuildLevel() > getLevel() || effect.getLevel() <= getSkillLevel(skillId)) {
            return false;
        }
        MapleGuildSkill ourSkill = guildSkills.get(skillId);
        if (ourSkill == null) {
            ourSkill = new MapleGuildSkill(skillId, effect.getLevel(), 0, name, name);
            guildSkills.put(skillId, ourSkill);
        } else {
            ourSkill.level = effect.getLevel();
            ourSkill.purchaser = name;
            ourSkill.activator = name;
        }
        if (effect.getPeriod() <= 0) {
            ourSkill.timestamp = -1L;
        } else {
            ourSkill.timestamp = System.currentTimeMillis() + (effect.getPeriod() * 60000L);
        }
        changed_skills = true;
        gainGP(1000, true, chrId);
        broadcast(GuildPacket.guildSkillPurchased(id, skillId, ourSkill.level, ourSkill.timestamp, name, name));
        return true;
    }

    public int getLevel() {
        return level;
    }

    public final int calculateLevel() {
        for (int i = 1; i < 10; i++) {
            if (gp < getGuildExpNeededForLevel(i)) {
                return i;
            }
        }
        return 10;
    }

    public int[] getGuildExp() {
        return guildExp;
    }

    public int getGuildExpNeededForLevel(int levelx) {
        if (levelx < 0 || levelx >= guildExp.length) {
            return Integer.MAX_VALUE;
        }
        return guildExp[levelx];
    }

    public void addMemberData(MaplePacketLittleEndianWriter mplew) {
        List<MapleGuildCharacter> players = new ArrayList<>();
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() == leader) {
                players.add(mgc);
            }
        }
        for (MapleGuildCharacter mgc : members) {
            if (mgc.getId() != leader) {
                players.add(mgc);
            }
        }
        if (players.size() != members.size()) {
            System.out.println("家族成员信息加载错误 - 实际加载: " + players.size() + " 应当加载: " + members.size());
        }
        //家族成员信息
        mplew.writeShort(players.size());
        for (MapleGuildCharacter mgc : players) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : players) {
            mplew.writeAsciiString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(mgc.getGuildRank());
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(mgc.getAllianceRank());
            mplew.writeInt(mgc.getGuildContribution());
            mplew.writeInt(0); //未知 V.117.1 新增
            mplew.writeInt(0); //未知 V.117.1 新增
            mplew.writeLong(PacketHelper.getTime(-2)); //00 40 E0 FD 3B 37 4F 01
        }
        //等待家族加入家族的角色信息
        mplew.writeShort(applyMembers.size());
        for (MapleGuildCharacter mgc : applyMembers) {
            mplew.writeInt(mgc.getId());
        }
        for (MapleGuildCharacter mgc : applyMembers) {
            mplew.writeAsciiString(mgc.getName(), 13);
            mplew.writeInt(mgc.getJobId());
            mplew.writeInt(mgc.getLevel());
            mplew.writeInt(0);
            mplew.writeInt(mgc.isOnline() ? 1 : 0);
            mplew.writeInt(3);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeInt(0);
            mplew.writeLong(PacketHelper.getTime(-2)); //00 40 E0 FD 3B 37 4F 01
        }
    }

    public Collection<MapleGuildCharacter> getMembers() {
        return Collections.unmodifiableCollection(members);
    }

    public boolean isInit() {
        return init;
    }

    /*
     * 家族BBS
     */
    public List<MapleBBSThread> getBBS() {
        List<MapleBBSThread> ret = new ArrayList<>(bbs.values());
        ret.sort(new MapleBBSThread.ThreadComparator());
        return ret;
    }

    /*
     * 添加1个新的BBS信息
     */
    public int addBBSThread(String title, String text, int icon, boolean bNotice, int posterID) {
        int add = bbs.get(0) == null ? 1 : 0; //add 1 if no notice
        changed = true;
        int ret = bNotice ? 0 : Math.max(1, bbs.size() + add);
        bbs.put(ret, new MapleBBSThread(ret, title, text, System.currentTimeMillis(), this.id, posterID, icon));
        return ret;
    }

    /*
     * 编辑BBS信息
     */
    public void editBBSThread(int localthreadid, String title, String text, int icon, int posterID, int guildRank) {
        MapleBBSThread thread = bbs.get(localthreadid);
        if (thread != null && (thread.ownerID == posterID || guildRank <= 2)) {
            changed = true;
            thread.setTitle(title);
            thread.setText(text);
            thread.setIcon(icon);
            thread.setTimestamp(System.currentTimeMillis());
            //bbs.put(localthreadid, new MapleBBSThread(localthreadid, title, text, System.currentTimeMillis(), this.id, thread.ownerID, icon));
        }
    }

    /*
     * 删除BBS信息
     */
    public void deleteBBSThread(int localthreadid, int posterID, int guildRank) {
        MapleBBSThread thread = bbs.get(localthreadid);
        if (thread != null && (thread.ownerID == posterID || guildRank <= 2)) {
            changed = true;
            bbs.remove(localthreadid);
        }
    }

    /*
     * 添加1个BBS的留言信息
     */
    public void addBBSReply(int localthreadid, String text, int posterID) {
        MapleBBSThread thread = bbs.get(localthreadid);
        if (thread != null) {
            changed = true;
            thread.replies.put(thread.replies.size(), new MapleBBSReply(thread.replies.size(), posterID, text, System.currentTimeMillis()));
        }
    }

    /*
     * 删除1个BBS的留言信息
     */
    public void deleteBBSReply(int localthreadid, int replyid, int posterID, int guildRank) {
        MapleBBSThread thread = bbs.get(localthreadid);
        if (thread != null) {
            MapleBBSReply reply = thread.replies.get(replyid);
            if (reply != null && (reply.ownerID == posterID || guildRank <= 2)) {
                changed = true;
                thread.replies.remove(replyid);
            }
        }
    }

    public boolean hasSkill(int id) {
        return guildSkills.containsKey(id);
    }

    private enum BCOp {

        NONE, DISBAND, EMBELMCHANGE, CHAT
    }
}
