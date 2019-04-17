package tools;

import com.alee.laf.optionpane.WebOptionPane;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.console.Start;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class DeleteUserData {

    private static final Logger log = LogManager.getLogger(DeleteUserData.class.getName());
    private static final List<String> databasenames = Arrays.asList(
            "accounts",
            "accounts_event",
            "accounts_info",
            "accounts_log",
            "achievements",
            "alliances",
            "androids",
            "bank",
            "battlelog",
            "bbs_replies",
            "bbs_threads",
            "bosslog",
            "buddies",
            "cashshop_categories",
            "cashshop_items",
            "cashshop_limit_sell",
            "cashshop_log",
            "cashshop_menuitems",
            "cashshop_modified_items",
            "character_cards",
            "character_coreauras",
            "character_credit",
            "character_keyvalue",
            "character_potionpots",
            "character_slots",
            "character_work",
            "characters",
            "cheatlog",
            "compensationlog_confirmed",
            "dojorankings",
            "donation",
            "donorlog",
            "dueypackages",
            "effectswitch",
            "eventforday",
            "eventtimes",
            "extendedslots",
            "famelog",
            "familiars",
            "families",
            "gifts",
            "gmlog",
            "guilds",
            "guildskills",
            "hacker",
            "hiredmerch",
            "imps",
            "innerskills",
            "internlog",
            "inventoryequipment",
            "inventoryitems",
            "inventorylog",
            "inventoryslot",
            "ipbans",
            "ipvotelog",
            "keymap",
            "lovelog",
            "macbans",
            "macfilters",
            "missionlist",
            "missionstatus",
            "monsterbook",
            "mountdata",
            "mts_cart",
            "mts_items",
            "notes",
            "nxcode",
            "parttime",
            "paylog",
            "pets",
            "pokemon",
            "pqlog",
            "pvpstats",
            "pwreset",
            "questinfo",
            "queststatus",
            "queststatusmobs",
            "quickslot",
            "rankingtop",
            "reports",
            "rings",
            "savedlocations",
            "scroll_log",
            "sidekicks",
            "skillmacros",
            "skills",
            "skills_cooldowns",
            "speedruns",
            "storages",
            "tournamentlog",
            "trocklocations",
            "wishlist"
    );

    public synchronized static void run() {
        setKeyChecks(0);
        for (String name : databasenames) {
            System.err.println("正在清空" + name + "表...");
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("TRUNCATE TABLE " + name)) {
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                log.error("清空" + name + "表失败", e);
            }
        }
        setKeyChecks(1);
        WebOptionPane.showMessageDialog(Start.getInstance(), "玩家数据清空完成！");
    }

    private synchronized static void setKeyChecks(int mode) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SET FOREIGN_KEY_CHECKS = " + mode)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("操作外键失败", e);
        }
    }
}
