/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author PlayDK
 */
public class MaplePvpStats implements Serializable {

    private static final long serialVersionUID = -639523813413728519L;
    private int watk; //物理攻击力
    private int matk; //魔法攻击力
    private int wdef; //物理防御
    private int mdef; //魔法防御
    private int acc; //命中率
    private int avoid; //回避率
    private int wdef_rate; //物理防御增加x%
    private int mdef_rate; //魔法防御增加x%
    private int ignore_def; //无视x%防御
    private int damage_rate; //伤害增加x%
    private int ignore_damage; //伤害减少x%

    public MaplePvpStats() {
    }

    public MaplePvpStats(int watk, int matk, int wdef, int mdef, int acc, int avoid, int wdef_rate, int mdef_rate, int ignore_def, int damage_rate, int ignore_damage) {
        this.watk = watk;
        this.matk = matk;
        this.wdef = wdef;
        this.mdef = mdef;
        this.acc = acc;
        this.avoid = avoid;
        this.wdef_rate = wdef_rate;
        this.mdef_rate = mdef_rate;
        this.ignore_def = ignore_def;
        this.damage_rate = damage_rate;
        this.ignore_damage = ignore_damage;
    }

    public static MaplePvpStats loadOrCreateFromDB(int accountId) {
        MaplePvpStats ret = null;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM pvpstats WHERE accountid = ?");
            ps.setInt(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ret = new MaplePvpStats(rs.getInt("watk"), rs.getInt("matk"), rs.getInt("wdef"), rs.getInt("mdef"), rs.getInt("acc"), rs.getInt("avoid"), rs.getInt("wdef_rate"), rs.getInt("mdef_rate"), rs.getInt("ignore_def"), rs.getInt("damage_rate"), rs.getInt("ignore_damage"));
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO pvpstats (accountid, watk, matk, wdef, mdef, acc, avoid, wdef_rate, mdef_rate, ignore_def, damage_rate, ignore_damage) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                psu.setInt(1, accountId);
                psu.setInt(2, 0); //watk 物理攻击力
                psu.setInt(3, 0); //matk 魔法攻击力
                psu.setInt(4, 0); //wdef 物理防御
                psu.setInt(5, 0); //mdef 魔法防御
                psu.setInt(6, 100); //acc 命中率
                psu.setInt(7, 0); //avoid 回避率
                psu.setInt(8, 0); //wdef_rate 物理防御增加x%
                psu.setInt(9, 0); //mdef_rate 魔法防御增加x%
                psu.setInt(10, 0); //ignore_def 无视x%防御
                psu.setInt(11, 0); //damage_rate 伤害增加x%
                psu.setInt(12, 0); //ignore_damage 伤害减少x%
                psu.executeUpdate();
                psu.close();
                ret = new MaplePvpStats(0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0);
            }
            rs.close();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("加载角色 Pvp 属性出现错误." + ex);
        }
        return ret;
    }

    public void saveToDb(DruidPooledConnection con, int accountId) {
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE pvpstats SET watk = ?, matk = ?, wdef = ?, mdef = ?, acc = ?, avoid = ?, wdef_rate = ?, mdef_rate = ?, ignore_def = ?, damage_rate = ?, ignore_damage = ? WHERE accountId = ?");
            ps.setInt(1, accountId);
            ps.setInt(2, watk); //物理攻击力
            ps.setInt(3, matk); //魔法攻击力
            ps.setInt(4, wdef); //物理防御
            ps.setInt(5, mdef); //魔法防御
            ps.setInt(6, acc); //命中率
            ps.setInt(7, avoid); //回避率
            ps.setInt(8, wdef_rate); //物理防御增加x%
            ps.setInt(9, mdef_rate); //魔法防御增加x%
            ps.setInt(10, ignore_def); //无视x%防御
            ps.setInt(11, damage_rate); //伤害增加x%
            ps.setInt(12, ignore_damage); //伤害减少x%
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("保存角色 Pvp 属性出现错误." + ex);
        }
    }

    /*
     * 物理攻击力
     */
    public int getWatk() {
        return watk;
    }

    public void setWatk(int gain) {
        this.watk = gain;
    }

    public void gainWatk(int gain) {
        this.watk += gain;
    }

    /*
     * 魔法攻击力
     */
    public int getMatk() {
        return matk;
    }

    public void setMatk(int gain) {
        this.matk = gain;
    }

    public void gainMatk(int gain) {
        this.matk += gain;
    }

    /*
     * 物理防御
     */
    public int getWdef() {
        return wdef;
    }

    public void setWdef(int gain) {
        this.wdef = gain;
    }

    public void gainWdef(int gain) {
        this.wdef += gain;
    }

    /*
     * 魔法防御
     */
    public int getMdef() {
        return mdef;
    }

    public void setMdef(int gain) {
        this.mdef = gain;
    }

    public void gainMdef(int gain) {
        this.mdef += gain;
    }

    /*
     * 命中率
     */
    public int getAcc() {
        return acc;
    }

    public void setAcc(int gain) {
        this.acc = gain;
    }

    public void gainAcc(int gain) {
        this.acc += gain;
    }

    /*
     * 回避率
     */
    public int getAvoid() {
        return avoid;
    }

    public void setAvoid(int gain) {
        this.avoid = gain;
    }

    public void gainAvoid(int gain) {
        this.avoid += gain;
    }

    /*
     * 物理防御增加x%
     */
    public int getWdefRate() {
        return wdef_rate;
    }

    public void setWdefRate(int gain) {
        this.wdef_rate = gain;
    }

    public void gainWdefRate(int gain) {
        this.wdef_rate += gain;
    }

    /*
     * 魔法防御增加x%
     */
    public int getMdefRate() {
        return mdef_rate;
    }

    public void setMdefRate(int gain) {
        this.mdef_rate = gain;
    }

    public void gainMdefRate(int gain) {
        this.mdef_rate += gain;
    }

    /*
     * 无视x%防御
     */
    public int getIgnoreDef() {
        return ignore_def;
    }

    public void setIgnoreDef(int gain) {
        this.ignore_def = gain;
    }

    public void gainIgnoreDef(int gain) {
        this.ignore_def += gain;
    }

    /*
     * 伤害增加x%
     */
    public int getDamageRate() {
        return damage_rate;
    }

    public void setDamageRate(int gain) {
        this.damage_rate = gain;
    }

    public void gainDamageRate(int gain) {
        this.damage_rate += gain;
    }

    /*
     * 伤害增加x%
     */
    public int getIgnoreDamage() {
        return ignore_damage;
    }

    public void setIgnoreDamage(int gain) {
        this.ignore_damage = gain;
    }

    public void gainIgnoreDamage(int gain) {
        this.ignore_damage += gain;
    }
}
