package handling.world.family;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.world.WorldBroadcastService;
import handling.world.WorldFamilyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class MapleFamily implements Serializable {

    public static final long serialVersionUID = 6322150443228168192L;
    private static final Logger log = LogManager.getLogger(MapleFamily.class);
    //does not need to be in order :) CID -> MFC
    private final Map<Integer, MapleFamilyCharacter> members = new ConcurrentHashMap<>();
    private String leadername = null, notice;
    private int id, leaderid;
    private boolean proper = true, bDirty = false, changed = false;

    public MapleFamily(int familyid) {
        super();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM families WHERE familyid = ?");
            ps.setInt(1, familyid);
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                rs.close();
                ps.close();
                id = -1;
                proper = false;
                return;
            }
            id = familyid;
            leaderid = rs.getInt("leaderid");
            notice = rs.getString("notice");
            rs.close();
            ps.close();
            //does not need to be in any order
            ps = con.prepareStatement("SELECT id, name, level, job, seniorid, junior1, junior2, currentrep, totalrep FROM characters WHERE familyid = ?", ResultSet.CONCUR_UPDATABLE);
            ps.setInt(1, familyid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt("id") == leaderid) {
                    leadername = rs.getString("name");
                }
                members.put(rs.getInt("id"), new MapleFamilyCharacter(rs.getInt("id"), rs.getShort("level"), rs.getString("name"), (byte) -1, rs.getInt("job"), familyid, rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"), rs.getInt("currentrep"), rs.getInt("totalrep"), false));
            }
            rs.close();
            ps.close();

            if (leadername == null || members.size() < 2) {
                System.err.println("Leader " + leaderid + " isn't in family " + id + ". Members: " + members.size() + ".  Impossible... family is disbanding.");
                writeToDB(true);
                proper = false;
                return;
            }
            //upon startup, load all the seniorid/junior1/junior2 that aren't in this family
            for (MapleFamilyCharacter mfc : members.values()) { //just in case
                if (mfc.getJunior1() > 0 && (getMFC(mfc.getJunior1()) == null || mfc.getId() == mfc.getJunior1())) {
                    mfc.setJunior1(0);
                }
                if (mfc.getJunior2() > 0 && (getMFC(mfc.getJunior2()) == null || mfc.getId() == mfc.getJunior2() || mfc.getJunior1() == mfc.getJunior2())) {
                    mfc.setJunior2(0);
                }
                if (mfc.getSeniorId() > 0 && (getMFC(mfc.getSeniorId()) == null || mfc.getId() == mfc.getSeniorId())) {
                    mfc.setSeniorId(0);
                }
                if (mfc.getJunior2() > 0 && mfc.getJunior1() <= 0) {
                    mfc.setJunior1(mfc.getJunior2());
                    mfc.setJunior2(0);
                }
                if (mfc.getJunior1() > 0) {
                    MapleFamilyCharacter mfc2 = getMFC(mfc.getJunior1());
                    if (mfc2.getJunior1() == mfc.getId()) {
                        mfc2.setJunior1(0);
                    }
                    if (mfc2.getJunior2() == mfc.getId()) {
                        mfc2.setJunior2(0);
                    }
                    if (mfc2.getSeniorId() != mfc.getId()) {
                        mfc2.setSeniorId(mfc.getId());
                    }
                }
                if (mfc.getJunior2() > 0) {
                    MapleFamilyCharacter mfc2 = getMFC(mfc.getJunior2());
                    if (mfc2.getJunior1() == mfc.getId()) {
                        mfc2.setJunior1(0);
                    }
                    if (mfc2.getJunior2() == mfc.getId()) {
                        mfc2.setJunior2(0);
                    }
                    if (mfc2.getSeniorId() != mfc.getId()) {
                        mfc2.setSeniorId(mfc.getId());
                    }
                }
            }
            resetPedigree();
            resetDescendants(); //set
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
    }

    public static void loadAll() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT familyid FROM families");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                WorldFamilyService.getInstance().addLoadedFamily(new MapleFamily(rs.getInt("familyid")));
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
    }

    public static void loadAll(Object toNotify) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            boolean cont = false;
            PreparedStatement ps = con.prepareStatement("SELECT familyid FROM families");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FamilyLoad.QueueFamilyForLoad(rs.getInt("familyid"));
                cont = true;
            }
            rs.close();
            ps.close();
            if (!cont) {
                return;
            }
        } catch (SQLException se) {
            log.error("[MapleFamily] 从数据库中读取学院信息出错." + se);
        }
        AtomicInteger FinishedThreads = new AtomicInteger(0);
        FamilyLoad.Execute(toNotify);
        synchronized (toNotify) {
            try {
                toNotify.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        while (FinishedThreads.incrementAndGet() != FamilyLoad.NumSavingThreads) {
            synchronized (toNotify) {
                try {
                    toNotify.wait();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void setOfflineFamilyStatus(int familyid, int seniorid, int junior1, int junior2, int currentrep, int totalrep, int chrid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ?, currentrep = ?, totalrep = ? WHERE id = ?");
            ps.setInt(1, familyid);
            ps.setInt(2, seniorid);
            ps.setInt(3, junior1);
            ps.setInt(4, junior2);
            ps.setInt(5, currentrep);
            ps.setInt(6, totalrep);
            ps.setInt(7, chrid);
            ps.execute();
            ps.close();
        } catch (SQLException se) {
            System.out.println("SQLException: " + se.getLocalizedMessage());
        }
    }

    public static int createFamily(int leaderId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO families (`leaderid`) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, leaderId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return 0;
            }
            int ret = rs.getInt(1);
            rs.close();
            ps.close();
            return ret;
        } catch (Exception e) {
            log.error("[MapleFamily] 创建学院信息出错." + e);
            return 0;
        }
    }

    public static void mergeFamily(MapleFamily newfam, MapleFamily oldfam) {
        //happens when someone in newfam juniors LEADER in oldfam
        //update all the members.
        for (MapleFamilyCharacter mgc : oldfam.members.values()) {
            mgc.setFamilyId(newfam.getId());
            if (mgc.isOnline()) {
                WorldFamilyService.getInstance().setFamily(newfam.getId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            } else {
                setOfflineFamilyStatus(newfam.getId(), mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            }
            newfam.members.put(mgc.getId(), mgc); //reset pedigree after
            newfam.setOnline(mgc.getId(), mgc.isOnline(), mgc.getChannel());
        }
        newfam.resetPedigree();
        //do not reset characters, so leadername is fine
        WorldFamilyService.getInstance().disbandFamily(oldfam.getId()); //and remove it
    }

    public void resetPedigree() {
        for (MapleFamilyCharacter mfc : members.values()) {
            mfc.resetPedigree(this);
        }
        bDirty = true;
    }

    public void resetDescendants() { //not stored here, but rather in the MFC
        MapleFamilyCharacter mfc = getMFC(leaderid);
        if (mfc != null) {
            mfc.resetDescendants(this);
        }
        bDirty = true;
    }

    public boolean isProper() {
        return proper;
    }

    public void writeToDB(boolean isDisband) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            if (!isDisband) {
                if (changed) {
                    PreparedStatement ps = con.prepareStatement("UPDATE families SET notice = ? WHERE familyid = ?");
                    ps.setString(1, notice);
                    ps.setInt(2, id);
                    ps.execute();
                    ps.close();
                }
                changed = false;
            } else {
                //members is less than 2, this shall be executed
                if (leadername == null || members.size() < 2) {
                    broadcast(null, -1, FCOp.DISBAND, null);
                }

                PreparedStatement ps = con.prepareStatement("DELETE FROM families WHERE familyid = ?");
                ps.setInt(1, id);
                ps.execute();
                ps.close();
            }
        } catch (SQLException se) {
            log.error("[MapleFamily] 保存学院信息出错." + se);
        }
    }

    public int getId() {
        return id;
    }

    public int getLeaderId() {
        return leaderid;
    }

    public String getNotice() {
        if (notice == null) {
            return "";
        }
        return notice;
    }

    public void setNotice(String notice) {
        this.changed = true;
        this.notice = notice;
    }

    public String getLeaderName() {
        return leadername;
    }

    public void broadcast(byte[] packet, List<Integer> chrIds) {
        broadcast(packet, -1, FCOp.NONE, chrIds);
    }

    public void broadcast(byte[] packet, int exception, List<Integer> chrIds) {
        broadcast(packet, exception, FCOp.NONE, chrIds);
    }

    public void broadcast(byte[] packet, int exceptionId, FCOp bcop, List<Integer> chrIds) {
        //passing null to cids will ensure all
        buildNotifications();
        if (members.size() < 2) {
            bDirty = true;
            return;
        }
        for (MapleFamilyCharacter mgc : members.values()) {
            if (chrIds == null || chrIds.contains(mgc.getId())) {
                if (bcop == FCOp.DISBAND) {
                    if (mgc.isOnline()) {
                        WorldFamilyService.getInstance().setFamily(0, 0, 0, 0, mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                    } else {
                        setOfflineFamilyStatus(0, 0, 0, 0, mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                    }
                } else if (mgc.isOnline() && mgc.getId() != exceptionId) {
                    WorldBroadcastService.getInstance().sendFamilyPacket(mgc.getId(), packet, exceptionId, id);
                }
            }
        }
    }

    private void buildNotifications() {
        if (!bDirty) {
            return;
        }
        Iterator<Entry<Integer, MapleFamilyCharacter>> toRemove = members.entrySet().iterator();
        while (toRemove.hasNext()) {
            MapleFamilyCharacter mfc = toRemove.next().getValue();
            if (mfc.getJunior1() > 0 && getMFC(mfc.getJunior1()) == null) {
                mfc.setJunior1(0);
            }
            if (mfc.getJunior2() > 0 && getMFC(mfc.getJunior2()) == null) {
                mfc.setJunior2(0);
            }
            if (mfc.getSeniorId() > 0 && getMFC(mfc.getSeniorId()) == null) {
                mfc.setSeniorId(0);
            }
            if (mfc.getFamilyId() != id) {
                toRemove.remove();
            }
        }
        if (members.size() < 2 && WorldFamilyService.getInstance().getFamily(id) != null) {
            WorldFamilyService.getInstance().disbandFamily(id); //disband us.
        }
        bDirty = false;
    }

    public void setOnline(int chrId, boolean online, int channel) {
        MapleFamilyCharacter mgc = getMFC(chrId);
        if (mgc != null && mgc.getFamilyId() == id) {
            if (mgc.isOnline() != online) {
                broadcast(FamilyPacket.familyLoggedIn(online, mgc.getName()), chrId, mgc.getId() == leaderid ? null : mgc.getPedigree());
            }
            mgc.setOnline(online);
            mgc.setChannel((byte) channel);
        }
        bDirty = true; // member formation has changed, update notifications
    }

    public int setRep(int chrId, int addrep, int oldLevel, String oldName) {
        MapleFamilyCharacter mgc = getMFC(chrId);
        if (mgc != null && mgc.getFamilyId() == id) {
            if (oldLevel > mgc.getLevel()) {
                addrep /= 2; //:D
            }
            if (mgc.isOnline()) {
                List<Integer> dummy = new ArrayList<>();
                dummy.add(mgc.getId());
                broadcast(FamilyPacket.changeRep(addrep, oldName), -1, dummy);
                WorldFamilyService.getInstance().setFamily(id, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep() + addrep, mgc.getTotalRep() + addrep, mgc.getId());
            } else {
                mgc.setCurrentRep(mgc.getCurrentRep() + addrep);
                mgc.setTotalRep(mgc.getTotalRep() + addrep);
                setOfflineFamilyStatus(id, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
            }
            return mgc.getSeniorId();
        }
        return 0;
    }

    public MapleFamilyCharacter addFamilyMemberInfo(MapleCharacter player, int seniorid, int junior1, int junior2) {
        MapleFamilyCharacter ret = new MapleFamilyCharacter(player, id, seniorid, junior1, junior2);
        members.put(player.getId(), ret);
        ret.resetPedigree(this);
        bDirty = true;
        List<Integer> toRemove = new ArrayList<>();
        for (int i = 0; i < ret.getPedigree().size(); i++) {
            if (ret.getPedigree().get(i) == ret.getId()) {
                continue;
            }
            MapleFamilyCharacter mfc = getMFC(ret.getPedigree().get(i));
            if (mfc == null) {
                toRemove.add(i);
            } else {
                mfc.resetPedigree(this);
            }
        }
        for (int i : toRemove) {
            ret.getPedigree().remove(i);
        }
        return ret;
    }

    public int addFamilyMember(MapleFamilyCharacter mgc) {
        mgc.setFamilyId(id);
        members.put(mgc.getId(), mgc);
        mgc.resetPedigree(this);
        bDirty = true;
        for (int i : mgc.getPedigree()) {
            getMFC(i).resetPedigree(this);
        }
        return 1;
    }

    public void leaveFamily(int chrId) {
        leaveFamily(getMFC(chrId), true);
    }

    public void leaveFamily(MapleFamilyCharacter mgc, boolean skipLeader) {
        bDirty = true;
        if (mgc.getId() == leaderid && !skipLeader) {
            //disband
            leadername = null; //to disband family completely
            WorldFamilyService.getInstance().disbandFamily(id);
        } else {
            //we also have to update anyone below us
            if (mgc.getJunior1() > 0) {
                MapleFamilyCharacter j = getMFC(mgc.getJunior1());
                if (j != null) {
                    j.setSeniorId(0);
                    splitFamily(j.getId(), j); //junior1 makes his own family
                }
            }
            if (mgc.getJunior2() > 0) {
                MapleFamilyCharacter j = getMFC(mgc.getJunior2());
                if (j != null) {
                    j.setSeniorId(0);
                    splitFamily(j.getId(), j); //junior1 makes his own family
                }
            }
            if (mgc.getSeniorId() > 0) {
                MapleFamilyCharacter mfc = getMFC(mgc.getSeniorId());
                if (mfc != null) {
                    if (mfc.getJunior1() == mgc.getId()) {
                        mfc.setJunior1(0);
                    } else {
                        mfc.setJunior2(0);
                    }
                }
            }
            List<Integer> dummy = new ArrayList<>();
            dummy.add(mgc.getId());
            broadcast(null, -1, FCOp.DISBAND, dummy);
            resetPedigree(); //ex but eh
        }
        members.remove(mgc.getId());
        bDirty = true;
    }

    public void memberLevelJobUpdate(MapleCharacter mgc) {
        MapleFamilyCharacter member = getMFC(mgc.getId());
        if (member != null) {
            int old_level = member.getLevel();
            int old_job = member.getJobId();
            member.setJobId(mgc.getJob());
            member.setLevel(mgc.getLevel());
            if (old_level != mgc.getLevel()) {
                this.broadcast(MaplePacketCreator.sendLevelup(true, mgc.getLevel(), mgc.getName()), mgc.getId(), mgc.getId() == leaderid ? null : member.getPedigree());
            }
            if (old_job != mgc.getJob()) {
                this.broadcast(MaplePacketCreator.sendJobup(true, mgc.getJob(), mgc.getName()), mgc.getId(), mgc.getId() == leaderid ? null : member.getPedigree());
            }
        }
    }

    public void disbandFamily() {
        writeToDB(true);
    }

    public MapleFamilyCharacter getMFC(int chrId) {
        return members.get(chrId);
    }

    public int getMemberSize() {
        return members.size();
    }

    //return disbanded or not.
    public boolean splitFamily(int splitId, MapleFamilyCharacter def) {
        //toSplit = initiator who either broke off with their junior/senior, splitId is the ID of the one broken off
        //if it's junior, splitId will be the new leaderID, if its senior it's toSplit thats the new leader
        //happens when someone in fam breaks off with anyone else, either junior/senior
        //update all the members.
        MapleFamilyCharacter leader = getMFC(splitId);
        if (leader == null) {
            leader = def;
            if (leader == null) {
                return false;
            }
        }
        try {
            List<MapleFamilyCharacter> all = leader.getAllJuniors(this); //leader is included in this collection
            if (all.size() <= 1) { //but if leader is the only person, then we're done
                leaveFamily(leader, false);
                return true;
            }
            int newId = createFamily(leader.getId());
            if (newId <= 0) {
                return false;
            }
            for (MapleFamilyCharacter mgc : all) {
                // need it for sql
                mgc.setFamilyId(newId);
                setOfflineFamilyStatus(newId, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                members.remove(mgc.getId()); //clean remove
            }
            MapleFamily newfam = WorldFamilyService.getInstance().getFamily(newId);
            for (MapleFamilyCharacter mgc : all) {
                if (mgc.isOnline()) { //NOW we change the char info
                    WorldFamilyService.getInstance().setFamily(newId, mgc.getSeniorId(), mgc.getJunior1(), mgc.getJunior2(), mgc.getCurrentRep(), mgc.getTotalRep(), mgc.getId());
                }
                newfam.setOnline(mgc.getId(), mgc.isOnline(), mgc.getChannel());
            }
        } finally {
            if (members.size() <= 1) { //only one person is left :|
                WorldFamilyService.getInstance().disbandFamily(id); //disband us.
                return true;
            }
        }
        bDirty = true;
        return false;
    }

    public enum FCOp {

        NONE, DISBAND
    }
}
