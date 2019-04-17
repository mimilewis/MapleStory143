package client.inventory;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.StructAndroid;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import tools.Randomizer;

import java.awt.*;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class MapleAndroid implements Serializable {

    private static final Logger log = LogManager.getLogger(MapleAndroid.class);
    private static final long serialVersionUID = 9179541993413738569L;
    private final int uniqueid;
    private final int itemid;
    private int Fh = 0;
    private int stance = 0;
    private int skin;
    private int hair;
    private int face;
    private int gender;
    private int type;
    private String name;
    private Point pos = new Point(0, 0);
    private boolean changed = false;

    @JsonCreator
    private MapleAndroid(@JsonProperty("itemid") int itemid, @JsonProperty("uniqueid") int uniqueid) {
        this.itemid = itemid;
        this.uniqueid = uniqueid;
    }

    /*
     * 加载机器人信息
     */
    public static MapleAndroid loadFromDb(int itemid, int uniqueid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            MapleAndroid ret = new MapleAndroid(itemid, uniqueid);
            PreparedStatement ps = con.prepareStatement("SELECT * FROM androids WHERE uniqueid = ?");
            ps.setInt(1, uniqueid);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            int type = rs.getInt("type");
            int gender = rs.getInt("gender");
            boolean fix = false;
            if (type < 1) { //修复以前错误的设置 机器人的外形ID默认是从1开始
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                type = ii.getAndroidType(itemid);
                StructAndroid aInfo = ii.getAndroidInfo(type);
                if (aInfo == null) { //如果没有这个类型就返回空
                    return null;
                }
                gender = aInfo.gender;
                fix = true;
            }
            ret.setType(type);
            ret.setGender(gender);
            ret.setSkin(rs.getInt("skin"));
            ret.setHair(rs.getInt("hair"));
            ret.setFace(rs.getInt("face"));
            ret.setName(rs.getString("name"));
            ret.changed = fix;
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            log.error("加载安卓信息出错", ex);
            return null;
        }
    }

    /*
     * 创建1个机器人信息
     * 也就是使用1个新机器人的道具
     */
    public static MapleAndroid create(int itemid, int uniqueid) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        int type = ii.getAndroidType(itemid);
        StructAndroid aInfo = ii.getAndroidInfo(type);
        if (aInfo == null) {
            return null;
        }
        int gender = aInfo.gender;
        int skin = aInfo.skin.get(Randomizer.nextInt(aInfo.skin.size()));
        int hair = aInfo.hair.get(Randomizer.nextInt(aInfo.hair.size()));
        int face = aInfo.face.get(Randomizer.nextInt(aInfo.face.size()));
        if (uniqueid <= -1) { //修复唯一ID小于-1的情况
            uniqueid = MapleInventoryIdentifier.getInstance();
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement pse = con.prepareStatement("INSERT INTO androids (uniqueid, type, gender, skin, hair, face, name) VALUES (?, ?, ?, ?, ?, ?, ?)");
            pse.setInt(1, uniqueid);
            pse.setInt(2, type);
            pse.setInt(3, gender);
            pse.setInt(4, skin);
            pse.setInt(5, hair);
            pse.setInt(6, face);
            pse.setString(7, "智能机器人");
            pse.executeUpdate();
            pse.close();
        } catch (SQLException ex) {
            log.error("创建安卓信息出错", ex);
            return null;
        }
        MapleAndroid and = new MapleAndroid(itemid, uniqueid);
        and.setType(type);
        and.setGender(gender);
        and.setSkin(skin);
        and.setHair(hair);
        and.setFace(face);
        and.setName("智能机器人");
        return and;
    }

    /**
     * 保存机器人信息到SQL
     */
    public void saveToDb() {
        saveToDb(null);
    }

    /**
     * 保存机器人信息到SQL
     */
    public void saveToDb(DruidPooledConnection con) {
        if (!changed) {
            return;
        }
        boolean needclose = false;
        try {
            if (con == null) {
                needclose = true;
                con = DatabaseConnection.getInstance().getConnection();
            }
            PreparedStatement ps = con.prepareStatement("UPDATE androids SET type = ?, gender = ?, skin = ?, hair = ?, face = ?, name = ? WHERE uniqueid = ?");
            ps.setInt(1, type); //外形ID
            ps.setInt(2, gender); //性别
            ps.setInt(3, skin); //皮肤
            ps.setInt(4, hair); //发型
            ps.setInt(5, face); //脸型
            ps.setString(6, name); //名字
            ps.setInt(7, uniqueid); //唯一的ID
            ps.executeUpdate();
            ps.close();
            changed = false;
        } catch (SQLException ex) {
            log.error("保存安卓信息出错", ex);
        } finally {
            if (needclose) {
                try {
                    con.close();
                } catch (SQLException e) {
                    log.error("保存安卓信息出错", e);
                }
            }
        }
    }

    public int getItemId() {
        return itemid;
    }

    public int getUniqueId() {
        return uniqueid;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
        this.changed = true;
    }

    public int getType() {
        return type;
    }

    public void setType(int t) {
        this.type = t;
        this.changed = true;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int g) {
        this.gender = g;
        this.changed = true;
    }

    public int getSkin() {
        return skin;
    }

    public void setSkin(int s) {
        this.skin = s;
        this.changed = true;
    }

    public int getHair() {
        return hair;
    }

    public void setHair(int h) {
        this.hair = h;
        this.changed = true;
    }

    public int getFace() {
        return face;
    }

    public void setFace(int f) {
        this.face = f;
        this.changed = true;
    }

    /*
     * 移动相关信息
     */
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

    public int getStance() {
        return stance;
    }

    public void setStance(int stance) {
        this.stance = stance;
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
}
