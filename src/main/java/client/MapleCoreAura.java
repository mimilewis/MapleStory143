/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import tools.Randomizer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author PlayDK
 *         龙的传人宝盒系统
 */
public class MapleCoreAura {

    private int id; //角色ID 如果是传授的别人的技能 这个地方就是传授者的ID
    private int level; //角色等级 如果是传授的别人的技能 这个地方就是传授者的等级
    private int str; //力量
    private int dex; //敏捷
    private int int_; //智力
    private int luk; //运气
    private int watk; //物理攻击
    private int magic; //魔法攻击
    private long expiration; //宝盒的时间

    public MapleCoreAura() {
    }

    public MapleCoreAura(int chrId) {
        this.id = chrId;
    }

    public MapleCoreAura(int chrId, int chrlevel) {
        this.id = chrId;
        this.level = chrlevel;
    }

    /*
     * 读取宝盒信息
     * 如果等级大于0 就是读取自己的 等级小于0就是读取传授者的
     */
    public static MapleCoreAura loadFromDb(int chrId) {
        return loadFromDb(chrId, -1);
    }

    public static MapleCoreAura loadFromDb(int chrId, int chrlevel) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            MapleCoreAura ret = new MapleCoreAura(chrId);
            PreparedStatement ps = con.prepareStatement("SELECT * FROM character_coreauras WHERE characterid = ?");
            ps.setInt(1, chrId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            ret.setLevel(chrlevel > 0 ? chrlevel : rs.getInt("level"));
            long expire = rs.getLong("expiredate");
            if (System.currentTimeMillis() > expire) {
                ret.resetCoreAura();
                ret.saveToDb(con);
            } else {
                ret.setStr(rs.getInt("str"));
                ret.setDex(rs.getInt("dex"));
                ret.setInt(rs.getInt("int"));
                ret.setLuk(rs.getInt("luk"));
                ret.setWatk(rs.getInt("watk"));
                ret.setMagic(rs.getInt("magic"));
                ret.setExpiration(expire);
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("加载龙的传人宝盒信息出错" + ex);
            return null;
        }
    }

    /*
     * 新建1个宝盒
     */
    public static MapleCoreAura createCoreAura(int chrId, int chrlevel) {
        MapleCoreAura ret = new MapleCoreAura(chrId, chrlevel);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM character_coreauras WHERE characterid = ?");
            ps.setInt(1, chrId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) { //如果有就直接加载
                ps.close();
                rs.close();
                ret = loadFromDb(chrId, chrlevel);
                return ret;
            }
            ps.close();
            rs.close();
            //没有就创建1个新的
            ret.resetCoreAura();
            ps = con.prepareStatement("INSERT INTO `character_coreauras` (`characterid`, `level`, `str`, `dex`, `int`, `luk`, `watk`, `magic`, `expiredate`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chrId);
            ps.setInt(2, chrlevel);
            ps.setInt(3, ret.getStr());
            ps.setInt(4, ret.getDex());
            ps.setInt(5, ret.getInt());
            ps.setInt(6, ret.getLuk());
            ps.setInt(7, ret.getWatk());
            ps.setInt(8, ret.getMagic());
            ps.setLong(9, ret.getExpiration());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("创建龙的传人宝盒信息出错 " + ex);
            return null;
        }
        return ret;
    }

    /*
     * 宝盒时间到期
     * 重置宝盒属性
     */
    public void resetCoreAura() {
        this.str = 5;
        this.dex = 5;
        this.int_ = 5;
        this.luk = 5;
        this.watk = 0;
        this.magic = 0;
        this.expiration = System.currentTimeMillis() + 24 * 60 * 60 * 1000; //重新设置时间为 24小时
    }

    /*
     * 随机宝盒属性
     */
    public void randomCoreAura(int type) {
        int max = Randomizer.nextBoolean() ? (type == 4 ? 32 : type == 3 ? 25 : type == 2 ? 20 : 15) : (type == 4 ? 25 : type == 3 ? 20 : type == 2 ? 15 : 10);
        int min = Randomizer.nextBoolean() ? 5 : 1;
        this.str = Randomizer.rand(min, max);
        this.dex = Randomizer.rand(min, max);
        this.int_ = Randomizer.rand(min, max);
        this.luk = Randomizer.rand(min, max);
        if (Randomizer.nextInt(1000) == 1) {
            this.watk = Randomizer.rand(10, 32);
        } else if (Randomizer.nextInt(500) == 1) {
            this.watk = Randomizer.rand(10, 25);
        } else if (Randomizer.nextInt(200) == 1) {
            this.watk = Randomizer.rand(5, 20);
        } else if (Randomizer.nextInt(100) == 1) {
            this.watk = Randomizer.rand(5, 15);
        } else {
            this.watk = Randomizer.rand(0, 15);
        }
        if (Randomizer.nextInt(1000) == 1) {
            this.magic = Randomizer.rand(10, 32);
        } else if (Randomizer.nextInt(500) == 1) {
            this.magic = Randomizer.rand(10, 25);
        } else if (Randomizer.nextInt(200) == 1) {
            this.magic = Randomizer.rand(5, 20);
        } else if (Randomizer.nextInt(100) == 1) {
            this.magic = Randomizer.rand(5, 15);
        } else {
            this.magic = Randomizer.rand(0, 15);
        }
    }

    /*
     * 保存宝盒信息
     */
    public void saveToDb(DruidPooledConnection con) {
        boolean needclose = false;
        try {
            if (con == null) {
                needclose = true;
                con = DatabaseConnection.getInstance().getConnection();
            }
            PreparedStatement ps = con.prepareStatement("UPDATE `character_coreauras` SET `level` = ?, `str` = ?, `dex` = ?, `int` = ?, `luk` = ?, `watk` = ?, `magic` = ?, `expiredate` = ? WHERE `characterid` = ?");
            ps.setInt(1, level);
            ps.setInt(2, str);
            ps.setInt(3, dex);
            ps.setInt(4, int_);
            ps.setInt(5, luk);
            ps.setInt(6, watk);
            ps.setInt(7, magic);
            ps.setLong(8, expiration);
            ps.setInt(9, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("保存龙的传人宝盒出错" + ex);
        } finally {
            if (con != null && needclose) {
                try {
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * 角色ID
     * 传授技能需要
     */
    public int getId() {
        return id;
    }

    /*
     * 角色等级
     * 传授技能需要
     */
    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    /*
     * 宝盒的等级
     * 5771001 - 宝盒魔方 Lv.1 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c30级~69级#的角色可以使用。
     * 5771002 - 宝盒魔方 Lv.2 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c70级~119级#的角色可以使用。
     * 5771003 - 宝盒魔方 Lv.3 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c120级~159级#的角色可以使用。
     * 5771004 - 宝盒魔方 Lv.4 - 可以重新设定宝盒能力的神秘魔方。只有#c龙的传人#职业#c160级以上#的角色可以使用。
     */
    public int getCoreAuraLevel() {
        if (level >= 30 && level < 70) {
            return 1;
        } else if (level >= 70 && level < 120) {
            return 2;
        } else if (level >= 120 && level < 160) {
            return 3;
        } else if (level >= 160) {
            return 4;
        }
        return 1;
    }

    /*
     * 力量
     */
    public int getStr() {
        return str;
    }

    public void setStr(int str) {
        this.str = str;
    }

    /*
     * 敏捷
     */
    public int getDex() {
        return dex;
    }

    public void setDex(int dex) {
        this.dex = dex;
    }

    /*
     * 智力
     */
    public int getInt() {
        return int_;
    }

    public void setInt(int int_) {
        this.int_ = int_;
    }

    /*
     * 运气
     */
    public int getLuk() {
        return luk;
    }

    public void setLuk(int luk) {
        this.luk = luk;
    }

    /*
     * 物理攻击
     */
    public int getWatk() {
        return watk;
    }

    public void setWatk(int watk) {
        this.watk = watk;
    }

    /*
     * 魔法攻击
     */
    public int getMagic() {
        return magic;
    }

    public void setMagic(int magic) {
        this.magic = magic;
    }

    /*
     * 总点数
     */
    public int getTotal() {
        return str + dex + int_ + luk + watk + magic;
    }

    /*
     * 宝盒的时间
     */
    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expire) {
        this.expiration = expire;
    }
}
