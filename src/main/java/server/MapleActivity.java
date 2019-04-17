/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import client.MapleCharacter;
import client.MapleClient;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MapleActivity {

    private final static Logger log = LogManager.getLogger(MapleActivity.class.getName());
    private final static int activity_max = 150;
    private final static int stage[] = {20, 40, 80, 120, 150};

    public static void initAllActivity() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM missionstatus WHERE missionid >= 120101 AND missionid <= 120114")) {
                ps.executeUpdate();
                ps.close();
            }
        } catch (SQLException e) {
            log.error("Error MissionDelete:", e);
        }
    }

    public static void loginTip(MapleClient c) {
        // 发送角色头顶信息
        int activity = MapleActivity.getDiffActivity(c.getPlayer());
        String message;
        if (activity == 0) {
            message = "#e【今日登陆提示】#n\r\n\r\n已完成所有活跃度任务，可以去试试挑战新BOSS~";
        } else {
            message = "#e【今日登陆提示】#n\r\n\r\n您今天还需 " + getNextStageNeed(c.getPlayer()) + " 活跃度即可领取第 " + getNextStage(c.getPlayer()) + " 阶段奖励。\r\n点击拍卖查看如何获得活跃度。";
        }
        c.announce(MaplePacketCreator.sendHint(message, 150, 60));
    }

    public static int getActivity(final MapleCharacter player) {
        int ret = 0;
        for (QuestActivity q : QuestActivity.values()) {
            int times = player.MissionGetFinish(player.getId(), q.id);
            ret += times * q.activity;
        }
        return ret;
    }

    public static void finish(final MapleCharacter player, int questid) {
        if (!player.MissionStatus(player.getId(), questid, 0, 4)) {
            player.MissionMake(player.getId(), questid, 0, 0, 0, 0);
        } else {
            if (player.MissionGetFinish(player.getId(), questid) >= QuestActivity.getMaxTimesById(questid)) {
                return;
            }
        }
        player.MissionFinish(player.getId(), questid);
        player.dropSpouseMessage(0x0A, "[系统提示] 任务完成，活跃度增加 " + QuestActivity.getActivityById(questid));
    }

    public static int getMaxActivity() {
        return activity_max;
    }

    public static int getDiffActivity(final MapleCharacter player) {
        return activity_max - getActivity(player);
    }

    public static int getNextStage(final MapleCharacter player) {
        int stage_ = 0;
        final int activity = getActivity(player);
        for (int i = 0; i < stage.length; i++) {
            if (activity < stage[i]) {
                stage_ = i + 1;
                break;
            }
        }
        return stage_;
    }

    public static int getNextStageNeed(final MapleCharacter player) {
        return stage[Math.min(stage.length - 1, getNextStage(player) - 1)] - getActivity(player);
    }

    public static int getRecevieReward(final MapleCharacter player) {
        final int activity = getActivity(player);
        for (int i = 1; i <= stage.length; i++) {
            if (activity >= stage[i - 1] && player.getBossLog("活跃度礼包" + i) == 0) {
                return i;
            }
        }
        return -1;
    }

    public enum QuestActivity {
        每日签到(120101, 5, 1),
        装备砸卷(120102, 2, 5),
        使用魔方(120103, 2, 5),
        废弃任务(120104, 5, 2),
        挑战扎昆(120105, 2, 5),
        挑战品克缤(120106, 10, 1),
        击杀任意BOSS(120107, 10, 1),
        在线300分钟(120108, 5, 1),
        在线800分钟(120109, 10, 1),
        环任务(120110, 1, 20),
        每日红包(120111, 10, 1),
        兑换中介币(120112, 10, 1),
        兑换点券(120113, 10, 1),
        闯关副本(120114, 20, 1);

        private final int id, activity, maxtimes;

        QuestActivity(int id, int activity, int maxtimes) {
            this.id = id;
            this.activity = activity;
            this.maxtimes = maxtimes;
        }

        public static int getActivityById(int id) {
            for (QuestActivity q : QuestActivity.values()) {
                if (q.id == id) {
                    return q.activity;
                }
            }
            return 0;
        }

        public static int getMaxTimesById(int id) {
            for (QuestActivity q : QuestActivity.values()) {
                if (q.id == id) {
                    return q.maxtimes;
                }
            }
            return 0;
        }
    }
}
