/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world.messenger;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer.WorldTimer;
import tools.StringUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author PlayDK
 */
public class MessengerRankingWorker {

    private static final Logger log = LogManager.getLogger(MessengerRankingWorker.class.getName());
    private final MapleCharacter[] rankingPlayer;
    private final int[] rankingLove;
    private final long[] lastUpdateTime;

    private MessengerRankingWorker() {
        log.info("正在启动[MessengerRankingWorker]");
        rankingPlayer = new MapleCharacter[2];
        for (int i = 0; i < rankingPlayer.length; i++) {
            rankingPlayer[i] = null;
        }
        rankingLove = new int[2];
        for (int i = 0; i < rankingLove.length; i++) {
            rankingLove[i] = 0;
        }
        lastUpdateTime = new long[2];
        for (int i = 0; i < lastUpdateTime.length; i++) {
            lastUpdateTime[i] = 0;
        }
        WorldTimer.getInstance().register(this::updateRankFromDB, 1000 * 60 * 60 * 3, 0); //暂时设置为3小时刷新1次
    }

    public static MessengerRankingWorker getInstance() {
        return SingletonHolder.instance;
    }

    public void updateRankFromDB() {
        String malesql = "SELECT chr.id, chr.gender, chr.love FROM characters AS chr LEFT JOIN accounts AS acc ON chr.accountid = acc.id WHERE chr.gm = 0 AND chr.gender = 0 AND acc.banned = 0 AND chr.love > 0 ORDER BY chr.love LIMIT 1";
        String femalesql = "SELECT chr.id, chr.gender, chr.love FROM characters AS chr LEFT JOIN accounts AS acc ON chr.accountid = acc.id WHERE chr.gm = 0 AND chr.gender = 1 AND acc.banned = 0 AND chr.love > 0 ORDER BY chr.love LIMIT 1";
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            /*
             * 加载男角色
             */
            PreparedStatement ps = con.prepareStatement(malesql);
            ResultSet rs = ps.executeQuery();
            int maleId = 0, malelove = 0;
            if (rs.next()) {
                maleId = rs.getInt("id");
                malelove = rs.getInt("love");
            }
            rs.close();
            ps.close();
            MapleCharacter malechr = null;
            if (maleId > 0) {
                malechr = WorldFindService.getInstance().findCharacterById(maleId);
                if (malechr == null) {
                    malechr = MapleCharacter.loadCharFromDB(maleId, null, false, null);
                }
            }
            rankingPlayer[0] = malechr;
            rankingLove[0] = malelove;
            lastUpdateTime[0] = System.currentTimeMillis();
            if (maleId > 0) {
                log.info("更新聊天招待人气排行榜 男角色 - Id: " + StringUtil.getRightPaddedStr(String.valueOf(maleId), ' ', 6) + " 好感度: " + StringUtil.getRightPaddedStr(String.valueOf(malelove), ' ', 4) + " 名字: " + (malechr != null ? malechr.getName() : "????"));
            } else {
                log.info("更新聊天招待人气排行榜 男角色 - 暂无信息... ");
            }
            /*
             * 加载女角色
             */
            ps = con.prepareStatement(femalesql);
            rs = ps.executeQuery();
            int femaleId = 0, femalelove = 0;
            if (rs.next()) {
                femaleId = rs.getInt("id");
                femalelove = rs.getInt("love");
            }
            rs.close();
            ps.close();
            MapleCharacter femalechr = null;
            if (femaleId > 0) {
                femalechr = WorldFindService.getInstance().findCharacterById(femaleId);
                if (femalechr == null) {
                    femalechr = MapleCharacter.loadCharFromDB(femaleId, null, false, null);
                }
            }
            rankingPlayer[1] = femalechr;
            rankingLove[1] = femalelove;
            lastUpdateTime[1] = System.currentTimeMillis();
            if (femaleId > 0) {
                log.info("更新聊天招待人气排行榜 女角色 - Id: " + StringUtil.getRightPaddedStr(String.valueOf(femaleId), ' ', 6) + " 好感度: " + StringUtil.getRightPaddedStr(String.valueOf(femalelove), ' ', 4) + " 名字: " + (femalechr != null ? femalechr.getName() : "????"));
            } else {
                log.info("更新聊天招待人气排行榜 女角色 - 暂无信息... ");
            }
        } catch (SQLException se) {
            log.error("更新聊天招待人气排行榜失败..", se);
        }
    }

    public void updateRankFromPlayer(MapleCharacter chr) {
        if (chr == null || chr.isGM()) {
            return;
        }
        int num = chr.getGender(); //0 = 男角色 1 = 女角色
        if (chr.getLove() > rankingLove[num]) {
            rankingPlayer[num] = chr;
            rankingLove[num] = chr.getLove();
            lastUpdateTime[num] = System.currentTimeMillis();
        }
    }

    public MapleCharacter getRankingPlayer(int num) {
        return rankingPlayer[num];
    }

    public int getRankingLove(int num) {
        return rankingLove[num];
    }

    public long getLastUpdateTime(int num) {
        return lastUpdateTime[num];
    }

    public void resetLastUpdateTime(int num) {
        lastUpdateTime[num] = System.currentTimeMillis();
    }

    private static class SingletonHolder {

        protected static final MessengerRankingWorker instance = new MessengerRankingWorker();
    }
}
