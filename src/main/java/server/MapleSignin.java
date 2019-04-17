/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.GameConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MapleSignin {

    public static final int MINLEVEL = 33;
    private static final MapleSignin instance = new MapleSignin();
    private final List<SiginRewardInfo> siginrewards = new LinkedList<>();

    public static MapleSignin getInstance() {
        return instance;
    }

    /**
     * 获得当前时间
     *
     * @return
     */
    public static String getCurrentTime() {
        return "check1=0;cDate=" + new SimpleDateFormat("yy/MM/dd").format(new Date());
    }

    public List<SiginRewardInfo> getSiginRewards() {
        return siginrewards;
    }

    public void load() {
        if (siginrewards.isEmpty()) {
            reload(false);
        }
    }

    /**
     * 重载签到奖励
     *
     * @param isCommand
     */
    public void reload(boolean isCommand) {
        siginrewards.clear();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM data_signin_reward");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                SiginRewardInfo reward = new SiginRewardInfo(
                        rs.getInt("id"),
                        rs.getInt("itemid"),
                        rs.getInt("quantity"),
                        rs.getInt("expiredate"),
                        rs.getInt("isCash"));

                siginrewards.add(reward);
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.err.println("Error handling siginreward" + e);
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int today = cal.get(Calendar.DAY_OF_MONTH);

        // 如果是每月1号，自动清除所有记录
        if (!isCommand && today == 1) {
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM questinfo WHERE quest = ?")) {
                    ps.setInt(1, GameConstants.每日签到系统_签到记录);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("清除签到记录失败.");
            }

            for (ChannelServer channelServer : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : channelServer.getPlayerStorage().getAllCharacters()) {
                    if (player != null) {
                        player.removeInfoQuest(GameConstants.每日签到系统_签到记录);
                    }
                }
            }
        }
    }

    /**
     * 未知数据
     *
     * @return
     */
    public Map<Integer, Integer> getUnknownMap() {
        Map<Integer, Integer> ret = new LinkedHashMap<>();
        ret.put(100, 40914);
        ret.put(101, 40914);
        return ret;
    }

    /**
     * 签到奖励的实体对象
     */
    public class SiginRewardInfo {

        private final int rank, itemid, quantity, expiredate, isCash;

        SiginRewardInfo(int rank, int itemid, int quantity, int expiredate, int isCash) {
            this.rank = rank;
            this.itemid = itemid;
            this.quantity = quantity;
            this.expiredate = expiredate;
            this.isCash = isCash;
        }

        public int getRank() {
            return rank;
        }

        public int getItemId() {
            return itemid;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getExpiredate() {
            return expiredate;
        }

        public int getIsCash() {
            return isCash;
        }

    }
}
