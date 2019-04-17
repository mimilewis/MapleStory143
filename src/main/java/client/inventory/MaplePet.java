package client.inventory;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;

import java.awt.*;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MaplePet implements Serializable {

    private static final Logger log = LogManager.getLogger(MaplePet.class);
    private static final long serialVersionUID = 9179541993413738569L;
    private final int[] excluded = new int[10]; //宠物捡取过滤
    private String name;
    private int Fh = 0, stance = 0, uniqueid, petitemid, secondsLeft = 0, skillid, color = -1, addSkill = 0;
    private Point pos;
    private byte fullness = 100, level = 1, summoned = 0;
    private short inventorypos = 0, closeness = 0, flags = 0;
    private boolean changed = false;
    private boolean canPickup = true;

    public MaplePet() {

    }

    private MaplePet(int petitemid, int uniqueid) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
        for (int i = 0; i < this.excluded.length; i++) {
            this.excluded[i] = 0;
        }
    }

    private MaplePet(int petitemid, int uniqueid, short inventorypos) {
        this.petitemid = petitemid;
        this.uniqueid = uniqueid;
        this.inventorypos = inventorypos;
        for (int i = 0; i < this.excluded.length; i++) {
            this.excluded[i] = 0;
        }
    }

    public static MaplePet loadFromDb(int itemid, int petid, short inventorypos) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            MaplePet ret = new MaplePet(itemid, petid, inventorypos);
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM pets WHERE petid = ?")) {
                ps.setInt(1, petid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        rs.close();
                        ps.close();
                        return null;
                    }
                    ret.setName(rs.getString("name"));
                    ret.setCloseness(rs.getShort("closeness"));
                    ret.setLevel(rs.getByte("level"));
                    ret.setFullness(rs.getByte("fullness"));
                    ret.setSecondsLeft(rs.getInt("seconds"));
                    ret.setFlags(rs.getShort("flags"));
                    ret.setBuffSkill(rs.getInt("skillid"));
                    ret.setColor(rs.getByte("color"));
                    ret.setAddSkill(rs.getInt("addSkill"));
                    String[] list = rs.getString("excluded").split(",");
                    for (int i = 0; i < ret.excluded.length; i++) {
                        ret.excluded[i] = Integer.parseInt(list[i]);
                    }
                    ret.changed = false;
                }
            }
            return ret;
        } catch (SQLException ex) {
            log.error("加载宠物信息出错", ex);
            return null;
        }
    }

    /*
     * 暂时默认所有宠物的状态都是 0x7F
     */
    public static MaplePet createPet(int itemid, int uniqueid) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        return createPet(itemid, ii.getName(itemid), 1, 0, 100, uniqueid, itemid == 5000054 ? 18000 : 0, ii.getPetFlagInfo(itemid), 0, -1);
    }

    public static MaplePet createPet(int itemid, String name, int level, int closeness, int fullness, int uniqueid, int secondsLeft, short flag, int skillId, int color) {
        if (uniqueid <= -1) { //wah
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement pse = con.prepareStatement("INSERT INTO pets (petid, name, level, closeness, fullness, seconds, flags, skillid, color) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                pse.setInt(1, uniqueid);
                pse.setString(2, name);
                pse.setByte(3, (byte) level);
                pse.setShort(4, (short) closeness);
                pse.setByte(5, (byte) fullness);
                pse.setInt(6, secondsLeft);
                pse.setShort(7, flag);
                pse.setInt(8, skillId);
                pse.setInt(9, color);
                pse.executeUpdate();
            }
        } catch (SQLException ex) {
            log.error("创建宠物信息出错", ex);
            return null;
        }
        MaplePet pet = new MaplePet(itemid, uniqueid);
        pet.setName(name);
        pet.setLevel(level);
        pet.setFullness(fullness);
        pet.setCloseness(closeness);
        pet.setFlags(flag);
        pet.setSecondsLeft(secondsLeft);
        pet.setBuffSkill(skillId);

        return pet;
    }

    public void saveToDb() {
        if (!changed) {
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE pets SET name = ?, level = ?, closeness = ?, fullness = ?, seconds = ?, flags = ?, skillid = ?, excluded = ?, color = ?, addSkill = ? WHERE petid = ?")) {
                ps.setString(1, name); // 宠物名字
                ps.setByte(2, level); // 宠物等级
                ps.setShort(3, closeness); // Set Closeness
                ps.setByte(4, fullness); // Set Fullness
                ps.setInt(5, secondsLeft);
                ps.setShort(6, flags);
                ps.setInt(7, skillid); //宠物自动加BUFF的技能ID
                StringBuilder list = new StringBuilder();
                for (int anExcluded : excluded) {
                    list.append(anExcluded);
                    list.append(",");
                }
                String newlist = list.toString();
                ps.setString(8, newlist.substring(0, newlist.length() - 1)); //宠物捡取过滤
                ps.setInt(9, color);
                ps.setInt(10, addSkill);
                ps.setInt(11, uniqueid); // Set ID
                ps.executeUpdate(); // Execute statement
            } // 宠物名字
            changed = false;
        } catch (SQLException ex) {
            log.error("保存宠物信息出错", ex);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.changed = true;
    }

    public boolean getSummoned() {
        return summoned > 0;
    }

    public void setSummoned(int summoned) {
        this.summoned = (byte) summoned;
    }

    public byte getSummonedValue() {
        return summoned;
    }

    public short getInventoryPosition() {
        return inventorypos;
    }

    public void setInventoryPosition(short inventorypos) {
        this.inventorypos = inventorypos;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public short getCloseness() {
        return closeness;
    }

    public void setCloseness(int closeness) {
        this.closeness = (short) closeness;
        this.changed = true;
    }

    public byte getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = (byte) level;
        this.changed = true;
    }

    public byte getFullness() {
        return fullness;
    }

    public void setFullness(int fullness) {
        this.fullness = (byte) fullness;
        this.changed = true;
    }

    public short getFlags() {
        return flags;
    }

    public void setFlags(int fffh) {
        this.flags = (short) fffh;
        this.changed = true;
    }

    public int getFh() {
        return Fh;
    }

    public void setFh(int Fh) {
        this.Fh = Fh;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }

    public byte getType() {
        return 3;
    }

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
    }

    public int getPetItemId() {
        return petitemid;
    }

    public boolean canConsume(int itemId) {
        MapleItemInformationProvider mii = MapleItemInformationProvider.getInstance();
        for (int petId : mii.getItemEffect(itemId).getPetsCanConsume()) {
            if (petId == petitemid) {
                return true;
            }
        }
        return false;
    }

    public void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if (move instanceof LifeMovement) {
                if (move instanceof AbsoluteLifeMovement) {
                    setPos(move.getPosition());
                    setFh(((AbsoluteLifeMovement) move).getNewFH());
                }
                setStance(((LifeMovement) move).getNewstate());
            }
        }
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public void setSecondsLeft(int sl) {
        this.secondsLeft = sl;
        this.changed = true;
    }

    public int getBuffSkill() {
        return skillid;
    }

    public void setBuffSkill(int id) {
        this.skillid = id;
        this.changed = true;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
        this.changed = true;
    }

    public int getAddSkill() {
        return addSkill;
    }

    public void setAddSkill(int addSkill) {
        this.addSkill = addSkill;
    }

    /*
             * 宠物捡取过滤功能
             */
    public void clearExcluded() {
        for (int i = 0; i < excluded.length; i++) {
            this.excluded[i] = 0;
        }
        this.changed = true;
    }

    public List<Integer> getExcluded() {
        List<Integer> list = new ArrayList<>();
        for (int anExcluded : excluded) {
            if (anExcluded > 0 && PetFlag.PET_IGNORE_PICKUP.check(flags)) {
                list.add(anExcluded);
            }
        }
        return list;
    }

    public void addExcluded(int i, int itemId) {
        if (i < excluded.length) {
            this.excluded[i] = itemId;
            this.changed = true;
        }
    }

    /*
     * 角色是否开启关闭和激活宠物捡取道具功能
     */
    public boolean isCanPickup() {
        return canPickup;
    }

    public void setCanPickup(boolean can) {
        this.canPickup = can;
    }
}
