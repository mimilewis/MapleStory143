package server;

import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.JobConstants;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer.WorldTimer;
import tools.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class RankingWorker {

    private final static Logger log = LogManager.getLogger(RankingWorker.class.getName());
    private final static Map<Integer, List<RankingInformation>> rankings = new HashMap<>();
    private final static Map<String, Integer> jobCommands = new HashMap<>();
    private final static List<PokemonInformation> pokemon = new ArrayList<>();
    private final static List<PokedexInformation> pokemon_seen = new ArrayList<>();
    private final static List<PokebattleInformation> pokemon_ratio = new ArrayList<>();
    private final static List<Integer> itemSearch = new ArrayList<>(); //热门搜索道具

    public static Integer getJobCommand(String job) {
        return jobCommands.get(job);
    }

    public static Map<String, Integer> getJobCommands() {
        return jobCommands;
    }

    public static List<RankingInformation> getRankingInfo(int job) {
        return rankings.get(job);
    }

    public static List<PokemonInformation> getPokemonInfo() {
        return pokemon;
    }

    public static List<PokedexInformation> getPokemonCaught() {
        return pokemon_seen;
    }

    public static List<PokebattleInformation> getPokemonRatio() {
        return pokemon_ratio;
    }

    public static List<Integer> getItemSearch() {
        return itemSearch;
    }

    public static void start() {
        log.info("系统自动更新玩家排名功能已启动...");
        log.info("更新间隔时间为: " + ServerConfig.WORLD_REFRESHRANK + " 分钟1次。");
        WorldTimer.getInstance().register(() -> {
            jobCommands.clear();
            rankings.clear();
            pokemon.clear();
            pokemon_seen.clear();
            pokemon_ratio.clear();
            itemSearch.clear();
            updateRank();
        }, 1000 * 60 * ServerConfig.WORLD_REFRESHRANK); //4小时刷新1次排名
    }

    public static void updateRank() {
        log.info("开始更新玩家排名...");
        long startTime = System.currentTimeMillis();
        loadJobCommands();
        DruidPooledConnection con = null;
        try {
            con = DatabaseConnection.getInstance().getConnection();
            con.setAutoCommit(false); //false是关闭自动提交
            updateRanking(con);
            updatePokemon(con);
            updatePokemonRatio(con);
            updatePokemonCaught(con);
            updateItemSearch(con);
            con.commit(); //commit 提交
            con.setAutoCommit(true);
        } catch (Exception ex) {
            try {
                con.rollback();
                con.setAutoCommit(true);
                log.error("更新玩家排名出错", ex);
            } catch (SQLException ex2) {
                log.error("Could not rollback unfinished ranking transaction", ex2);
            }
        }
        log.info("玩家排名更新完成 耗时: " + ((System.currentTimeMillis() - startTime) / 1000) + " 秒..");
    }

    private static void updateRanking(Connection con) throws Exception {
        String sb = "SELECT c.id, c.job, c.exp, c.level, c.name, c.jobRank, c.rank, c.fame" + " FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 AND a.banned = 0 AND c.level >= 160" +
                " ORDER BY c.level DESC , c.exp DESC , c.fame DESC , c.rank ASC";

        PreparedStatement charSelect = con.prepareStatement(sb);
        ResultSet rs = charSelect.executeQuery();
        PreparedStatement ps = con.prepareStatement("UPDATE characters SET jobRank = ?, jobRankMove = ?, rank = ?, rankMove = ? WHERE id = ?");
        int rank = 0; //for "all"
        Map<Integer, Integer> rankMap = new LinkedHashMap<>();
        for (int i : jobCommands.values()) {
            rankMap.put(i, 0); //job to rank
            rankings.put(i, new ArrayList<>());
        }
        while (rs.next()) {
            int job = rs.getInt("job");
            if (!rankMap.containsKey(job / 100)) { //not supported.
                continue;
            }
            int jobRank = rankMap.get(job / 100) + 1;
            rankMap.put(job / 100, jobRank);
            rank++;
            rankings.get(-1).add(new RankingInformation(rs.getString("name"), job, rs.getInt("level"), rs.getLong("exp"), rank, rs.getInt("fame")));
            rankings.get(job / 100).add(new RankingInformation(rs.getString("name"), job, rs.getInt("level"), rs.getLong("exp"), jobRank, rs.getInt("fame")));
            ps.setInt(1, jobRank);
            ps.setInt(2, rs.getInt("jobRank") - jobRank);
            ps.setInt(3, rank);
            ps.setInt(4, rs.getInt("rank") - rank);
            ps.setInt(5, rs.getInt("id"));
            ps.addBatch(); //添加要更新执行的SQL
        }
        ps.executeBatch(); //一次更新上面所有的addBatch() Batch update should be faster.
        rs.close();
        charSelect.close();
        ps.close();
    }

    private static void updatePokemon(Connection con) throws Exception {
        String sb = "SELECT count(distinct m.id) AS mc, c.name, c.totalWins, c.totalLosses " + " FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id" +
                " RIGHT JOIN monsterbook AS m ON m.charid = a.id WHERE c.gm = 0 AND a.banned = 0" +
                " ORDER BY c.totalWins DESC, c.totalLosses DESC, mc DESC LIMIT 50";

        PreparedStatement charSelect = con.prepareStatement(sb);
        ResultSet rs = charSelect.executeQuery();
        int rank = 0; //for "all"
        while (rs.next()) {
            rank++;
            pokemon.add(new PokemonInformation(rs.getString("name"), rs.getInt("totalWins"), rs.getInt("totalLosses"), rs.getInt("mc"), rank));
        }
        rs.close();
        charSelect.close();
    }

    private static void updatePokemonRatio(Connection con) throws Exception {
        String sb = "SELECT (c.totalWins / c.totalLosses) AS mc, c.name, c.totalWins, c.totalLosses " + " FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id" +
                " WHERE c.gm = 0 AND a.banned = 0 AND c.totalWins > 10 AND c.totalLosses > 0" +
                " ORDER BY mc DESC, c.totalWins DESC, c.totalLosses ASC LIMIT 50";

        PreparedStatement charSelect = con.prepareStatement(sb);
        ResultSet rs = charSelect.executeQuery();
        int rank = 0; //for "all"
        while (rs.next()) {
            rank++;
            pokemon_ratio.add(new PokebattleInformation(rs.getString("name"), rs.getInt("totalWins"), rs.getInt("totalLosses"), rs.getDouble("mc"), rank));
        }
        rs.close();
        charSelect.close();
    }

    private static void updatePokemonCaught(Connection con) throws Exception {
        String sb = "SELECT count(DISTINCT m.id) AS mc, c.name " + " FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id" +
                " RIGHT JOIN monsterbook AS m ON m.charid = a.id WHERE c.gm = 0 AND a.banned = 0" +
                " ORDER BY mc DESC LIMIT 50";

        PreparedStatement charSelect = con.prepareStatement(sb);
        ResultSet rs = charSelect.executeQuery();
        int rank = 0; //for "all"
        while (rs.next()) {
            rank++;
            pokemon_seen.add(new PokedexInformation(rs.getString("name"), rs.getInt("mc"), rank));
        }
        rs.close();
        charSelect.close();
    }

    private static void updateItemSearch(Connection con) throws Exception {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        PreparedStatement ps = con.prepareStatement("SELECT itemid, count FROM itemsearch WHERE count > 0 ORDER BY count DESC LIMIT 10");
        ResultSet rs = ps.executeQuery();
        itemSearch.clear(); //清理列表
        while (rs.next()) {
            int itemId = rs.getInt("itemid");
            /*
             * 如果列表中这个道具 或者 道具不存在就跳过
             */
            if (itemSearch.contains(itemId) || !ii.itemExists(itemId)) {
                continue;
            }
            itemSearch.add(itemId); //添加道具
        }
        rs.close();
        ps.close();
    }

    public static void loadJobCommands() {
        jobCommands.put("所有", -1);
        jobCommands.put("新手", 0);
        jobCommands.put("战士", 1);
        jobCommands.put("魔法师", 2);
        jobCommands.put("弓箭手", 3);
        jobCommands.put("飞侠", 4);
        jobCommands.put("海盗", 5);
        jobCommands.put("初心者", 10);
        jobCommands.put("魂骑士", 11);
        jobCommands.put("炎术士", 12);
        jobCommands.put("风灵使者", 13);
        jobCommands.put("夜行者", 14);
        jobCommands.put("奇袭者", 15);
        jobCommands.put("英雄", 20);
        jobCommands.put("战神", 21);
        jobCommands.put("龙神", 22);
        jobCommands.put("双弩精灵", 23);
        jobCommands.put("幻影神偷", 24);
        jobCommands.put("夜光法师", 27);
        jobCommands.put("反抗者", 30);
        jobCommands.put("恶魔猎手", 31);
        jobCommands.put("幻灵斗师", 32);
        jobCommands.put("弩豹游侠", 33);
        jobCommands.put("机械师", 35);
        jobCommands.put("米哈尔", 50);
    }

    public static class RankingInformation {

        public final String toString;
        public final int rank;

        public RankingInformation(String name, int job, int level, long exp, int rank, int fame) {
            this.rank = rank;
            String builder = "排名 " + StringUtil.getRightPaddedStr(String.valueOf(rank), ' ', 3) +
                    " : " +
                    StringUtil.getRightPaddedStr(name, ' ', 13) +
                    " 等级: " +
                    StringUtil.getRightPaddedStr(String.valueOf(level), ' ', 3) +
                    " 职业: " +
                    StringUtil.getRightPaddedStr(JobConstants.getJobNameById(job), ' ', 10) +
                    "\r\n";
            //builder.append(" 经验: ");
            //builder.append(exp);
            //builder.append(" 人气: ");
            //builder.append(fame);
            this.toString = builder; //Rank 1 : KiDALex - Level 200 Blade Master | 0 EXP, 30000 Fame
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public static class PokemonInformation {

        public final String toString;

        public PokemonInformation(String name, int totalWins, int totalLosses, int caught, int rank) {
            String builder = "排名 " + rank +
                    " : #e" +
                    name +
                    "#n - #r胜利: " +
                    totalWins +
                    "#b 失败: " +
                    totalLosses +
                    "#k Caught:" +
                    caught +
                    "\r\n";
            this.toString = builder; //Rank 1 : Phoenix - 200 Wins, 0 Losses, 650 Caught
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public static class PokedexInformation {

        public final String toString;

        public PokedexInformation(String name, int caught, int rank) {
            String builder = "排名 " + rank +
                    " : #e" +
                    name +
                    "#n - #rCaught: " +
                    caught +
                    "\r\n";
            this.toString = builder; //Rank 1 : Phoenix - 650 Caught
        }

        @Override
        public String toString() {
            return toString;
        }
    }

    public static class PokebattleInformation {

        public final String toString;

        public PokebattleInformation(String name, int totalWins, int totalLosses, double caught, int rank) {
            String builder = "Rank " + rank +
                    " : #e" +
                    name +
                    "#n - #rRatio: " +
                    caught +
                    "\r\n";
            this.toString = builder; //Rank 1 : Phoenix - 200 Wins, 0 Losses, 200 Ratio
        }

        @Override
        public String toString() {
            return toString;
        }
    }
}
