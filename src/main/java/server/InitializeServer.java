package server;

import client.MapleCharacter;
import client.skills.SkillFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.world.CharacterTransfer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleOverrideData;
import redis.clients.jedis.Jedis;
import server.cashshop.CashItemFactory;
import server.console.Start;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MobSkillFactory;
import server.maps.MapleMapFactory;
import server.shop.MapleShopFactory;
import tools.JsonUtil;
import tools.Pair;
import tools.RedisUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 初始化服务器配置，比如更新数据库格式等
 */
public class InitializeServer {

    private static final Logger log = LogManager.getLogger(InitializeServer.class.getName());

    /**
     * 服务端初始化，应用数据库更改等
     *
     * @return
     */
    public static boolean Initial() {
        initializeSetting();
        return initializeUpdateLog(); //  && initializeMySQL()
    }

    /**
     * 初始化设置
     * 1、账号状态
     */
    private static void initializeSetting() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE `accounts` SET `loggedin` = 0, `check` = 0")) {
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }
    }

    /**
     * 创建更新日志的记录表
     *
     * @return
     */
    private static boolean initializeUpdateLog() {
        if (!checkTableisExist()) {
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("CREATE TABLE `systemupdatelog` (`id`  INT(11) NOT NULL AUTO_INCREMENT,`patchid`  TINYINT(1) NOT NULL ,`lasttime`  TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP ,PRIMARY KEY (`id`))")) {
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return checkTableisExist();
    }

    /**
     * 查看更新日志记录表是否存在
     *
     * @return
     */
    private static boolean checkTableisExist() {
        boolean exist = false;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SHOW TABLES LIKE 'systemupdatelog'"); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    exist = true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exist;
    }

    /**
     * 应用MySQL补丁
     *
     * @return
     */
    private static boolean initializeMySQL() {
        // 检查并应用MySQL补丁
        for (UPDATE_PATCH patch : UPDATE_PATCH.values()) {
            if (!checkIsAppliedSQLPatch(patch.ordinal())) {
                if (!applySQLPatch(patch.getSQL()) || !insertUpdateLog(patch.ordinal())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查是否已应用补丁
     *
     * @param id
     * @return
     */
    private static boolean checkIsAppliedSQLPatch(int id) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT id FROM systemupdatelog WHERE patchid = ?")) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 插入补丁的更新记录
     *
     * @param patchid 补丁id
     * @return
     */
    private static boolean insertUpdateLog(int patchid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO systemupdatelog(id, patchid, lasttime) VALUES (DEFAULT, ?, CURRENT_TIMESTAMP)")) {
                ps.setInt(1, patchid);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    private static boolean applySQLPatch(String sql) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate column name")) {
                return true;
            }
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 关闭c3p0日志输出
     */
//    public static void closeC3P0Log() {
//        Properties p = new Properties(System.getProperties());
//        p.put("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
//        p.put("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF"); // or any other
//        System.setProperties(p);
//    }
    public static boolean initializeRedis(boolean reload, Start.ProgressBarObservable observable) {
        Jedis jedis = RedisUtil.getJedis();
        if (RedisUtil.KEYNAMES.DELETECACHE || reload) {
            jedis.flushAll();
        }
        RedisUtil.returnResource(jedis);

        try {
            List<String> indexs = new ArrayList<>(Arrays.asList(
                    "正在加载技能冷却时间...",
                    "正在加载技能数据...",
                    "正在加载复制技能...",
                    "正在加载道具数据...",
                    "正在加载套装数据...",
                    "正在加载潜能数据...",
                    "正在加载星岩数据...",
                    "正在加载地图信息...",
                    "正在加载NPC名称与任务数据...",
                    "正在加载怪物信息...",
                    "正在加载怪物技能信息...",
                    "正在加载商店数据..."
            ));

            int index = -1;
            int currPro = observable.getProgress();
            int singlePro = (100 - currPro) / (indexs.size() + 1);

            // 加载技能冷却时间
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            SkillFactory.loadDelays();
            // 加载技能数据
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleOverrideData.getInstance().init();
            SkillFactory.loadSkillData();
            // 加载复制技能
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            SkillFactory.loadMemorySkills();
            // 加载道具数据
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleItemInformationProvider.getInstance().runItems();
            // 加载套装数据
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleItemInformationProvider.getInstance().loadSetItemData();
            // 加载潜能数据
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleItemInformationProvider.getInstance().loadPotentialData();
            // 加载星岩数据
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleItemInformationProvider.getInstance().loadSocketData();
            // 加载地图信息
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleMapFactory.loadAllLinkNpc();
            // 加载NPC名称与任务数量
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleLifeFactory.loadQuestCounts();
            // 加载怪物爆率
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MapleMonsterInformationProvider.getInstance().load();
            // 加载怪物名称
            MapleMapFactory.loadAllMapName();
            // 加载怪物技能信息
            observable.setProgress(new Pair<>(indexs.get(++index), currPro + singlePro * (index + 1)));
            MobSkillFactory.initialize();
            // 加载商店数据
            observable.setProgress(new Pair<>(indexs.get(++index), 90));
            MapleShopFactory.getInstance().loadShopData();
            CashItemFactory.getInstance().initialize();
            // 将数据库内所有角色数据保存到Redis
            clearAllPlayerCache();
            observable.setProgress(new Pair<>("服务端初始化完成，正在启动主界面...", 100));
        } catch (Exception e) {
            log.error("服务端初始化失败", e);
            Start.showMessage("服务端初始化失败", "错误", 0);
//            RedisUtil.flushall();
            System.exit(0);
        }

        return true;
    }

    private static void clearAllPlayerCache() {
        Jedis jedis = RedisUtil.getJedis();
        if (jedis.exists(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName())) {
            try {
                Map<String, String> datas = jedis.hgetAll(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName());
                for (Map.Entry<String, String> entry : datas.entrySet()) {
                    CharacterTransfer ct = JsonUtil.getMapperInstance().readValue(entry.getValue(), CharacterTransfer.class);
                    MapleCharacter.ReconstructChr(ct, null, false).saveToDB(true, false);
                }
            } catch (Exception e) {
                log.error("初始化玩家缓存出错", e);
                Start.showMessage("初始化玩家缓存出错,请联系我们进行解决", "警告", 0);
                System.exit(0);
            } finally {
                jedis.del(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName());
            }
        }
        RedisUtil.returnResource(jedis);
    }

    /**
     * 补丁列表
     */
    enum UPDATE_PATCH {

        更新表格式_questinfo("ALTER TABLE `questinfo` ADD COLUMN `accountid`  int(11) NOT NULL DEFAULT 0 AFTER `questinfoid`"),
        更新表格式_data_signin_reward("ALTER TABLE `data_signin_reward` CHANGE COLUMN `level` `iscash`  tinyint(1) NULL DEFAULT 0 AFTER `expiredate`"),
        删除旧版签到记录表("DROP TABLE IF EXISTS `data_signin_log`"),;

        private final String sql;

        UPDATE_PATCH(String sql) {
            this.sql = sql;
        }

        public String getSQL() {
            return sql;
        }
    }
}
