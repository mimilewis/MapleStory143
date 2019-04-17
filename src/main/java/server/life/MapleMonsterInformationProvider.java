package server.life;

import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import constants.ItemConstants;
import database.DatabaseConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import server.MapleItemInformationProvider;
import server.reward.RewardDropEntry;
import tools.Randomizer;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

public class MapleMonsterInformationProvider {

    private static final Logger log = LogManager.getLogger();
    private static final MapleMonsterInformationProvider instance = new MapleMonsterInformationProvider();
    private static final MapleDataProvider stringDataWZ = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz"));
    private static final MapleData mobStringData = stringDataWZ.getData("MonsterBook.img");
    private final Map<Integer, List<MonsterDropEntry>> drops = new TreeMap<>();
    private final List<MonsterGlobalDropEntry> globaldrops = new ArrayList<>();
    private final Map<Integer, Map<Integer, List<RewardDropEntry>>> specialdrops = new HashMap<>();

    public static MapleMonsterInformationProvider getInstance() {
        return instance;
    }

    public Map<Integer, List<MonsterDropEntry>> getAllDrop() {
        return drops;
    }

    public List<MonsterGlobalDropEntry> getGlobalDrop() {
        return globaldrops;
    }

    public Map<Integer, Map<Integer, List<RewardDropEntry>>> getFishDrop() {
        return specialdrops;
    }

    public void setDropData(int mobid, List<MonsterDropEntry> dropEntries) throws JsonProcessingException {
        setDropData(String.valueOf(mobid), dropEntries);
    }

    public void setDropData(String mobid, List<MonsterDropEntry> dropEntries) throws JsonProcessingException {
        drops.put(Integer.valueOf(mobid), dropEntries);
        update(Integer.valueOf(mobid), dropEntries);
    }

    public void removeDropData(int mobid) {
        drops.remove(mobid);
        update(mobid, null);
    }

    public void update(int mobid, List<MonsterDropEntry> dropEntries) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM drop_data WHERE dropperid = ?")) {
                ps.setInt(1, mobid);
                ps.execute();
            }
        } catch (SQLException ignored) {
        }

        if (dropEntries != null) {
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO drop_data VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?)")) {
                    dropEntries.forEach(monsterDropEntry -> {
                        try {
                            ps.setInt(1, mobid);
                            ps.setInt(2, monsterDropEntry.itemId);
                            ps.setInt(3, monsterDropEntry.minimum);
                            ps.setInt(4, monsterDropEntry.maximum);
                            ps.setInt(5, monsterDropEntry.questid);
                            ps.setInt(6, monsterDropEntry.chance);
                            ps.setString(7, MapleItemInformationProvider.getInstance().getName(monsterDropEntry.itemId));
                            ps.execute();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (SQLException ignored) {
            }
        }
    }

    public void load() {
        try {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            List<Integer> mobids = new ArrayList<>();
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT dropperid FROM drop_data GROUP BY dropperid")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            mobids.add(rs.getInt("dropperid"));
                        }
                    }
                }
            }

            mobids.forEach(mobid -> {
                List<MonsterDropEntry> list = new ArrayList<>();
                boolean exist = MapleLifeFactory.checkMonsterIsExist(mobid);
                if (!exist) {
                    return;
                }
                MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(mobid);
                if (mons == null) {
                    return;
                }

                try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                    try (PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data WHERE dropperid = ?")) {
                        ps.setInt(1, mobid);
                        try (ResultSet rs = ps.executeQuery()) {
                            boolean doneMesos = false;
                            while (rs.next()) {
                                int itemid = rs.getInt("itemid");
                                int chance = rs.getInt("chance");

                                if (itemid != 0 && !ii.itemExists(itemid)) {
                                    continue;
                                }

                                list = drops.computeIfAbsent(mobid, k -> new ArrayList<>());

                                Integer item = ii.getItemIdByMob(mobid);
                                if (item != null && item > 0) {
                                    list.add(new MonsterDropEntry(item, mons.isBoss() ? 1000000 : 10000, 1, 1, 0));
                                }

                                list.add(new MonsterDropEntry(
                                        itemid,
                                        chance,
                                        rs.getInt("minimum_quantity"),
                                        rs.getInt("maximum_quantity"),
                                        rs.getInt("questid")));


                                if (itemid == 0) {
                                    doneMesos = true;
                                }
                            }

                            if (!doneMesos) {
                                addMeso(mons, drops.get(mobid));
                            }
                        }
                    }

                    drops.put(mobid, list);
                } catch (SQLException e) {
                    log.error("读取怪物爆率出错.", e);
                }
            });


            for (Entry<Integer, List<MonsterDropEntry>> entry : drops.entrySet()) {
                if (entry.getKey() / 10000 == 238) { //去掉怪物卡片
                    continue;
                }
                if (entry.getKey() != 9400408 && mobStringData.getChildByPath(String.valueOf(entry.getKey())) != null) {
                    for (MapleData d : mobStringData.getChildByPath(String.valueOf(entry.getKey()) + "/reward")) {
                        int toAdd = MapleDataTool.getInt(d, 0);
                        if (toAdd > 0 && !contains(entry.getValue(), toAdd) && ii.itemExists(toAdd)) {
                            if (toAdd / 10000 == 238 //掉怪物卡片
                                    || toAdd / 10000 == 243 //盒子
                                    || toAdd / 10000 == 399 //字母
                                    || toAdd == 4001126 //枫叶
                                    || toAdd == 4001128 //火药桶
                                    || toAdd == 4001246 //温暖的阳光
                                    || toAdd == 4001473 //圣诞树装饰
                                    || toAdd == 4001447 //一起来行动
                                    || toAdd == 2022450 //经验值上升(小)
                                    || toAdd == 2022451 //经验值上升(中)
                                    || toAdd == 2022452 //经验值上升(大)
                                    || toAdd == 4032302 //神奇的颜料
                                    || toAdd == 4032303 //神秘的颜料
                                    || toAdd == 4032304 //魔法颜料
                                    ) {
                                continue;
                            }
                            entry.getValue().add(new MonsterDropEntry(toAdd, chanceLogic(toAdd), 1, 1, 0));
                        }
                    }
                }
            }

            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data_global")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            globaldrops.add(new MonsterGlobalDropEntry(
                                    rs.getInt("itemid"),
                                    rs.getInt("chance"),
                                    rs.getInt("continent"),
                                    rs.getByte("dropType"),
                                    rs.getInt("minimum_quantity"),
                                    rs.getInt("maximum_quantity"),
                                    rs.getInt("questid")
                            ));
                        }
                    }
                }
            }

            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM drop_data_special")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int mapid = rs.getInt("mapid"), dropperid = rs.getInt("dropperid");
                            Map<Integer, List<RewardDropEntry>> drop_data_special = specialdrops.computeIfAbsent(mapid, k -> new HashMap<>());
                            List<RewardDropEntry> list = drop_data_special.computeIfAbsent(mapid, k -> new ArrayList<>());

                            list.add(new RewardDropEntry(
                                    rs.getInt("itemid"),
                                    rs.getInt("chance"),
                                    rs.getInt("quantity"),
                                    rs.getInt("msgType"),
                                    rs.getInt("period"),
                                    rs.getInt("state")));
                            drop_data_special.put(dropperid, list);
                            specialdrops.put(mapid, drop_data_special);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("导入怪物爆率错误", e);
        }
    }


    public List<MonsterDropEntry> retrieveDrop(int monsterId) {
        return drops.computeIfAbsent(monsterId, k -> new ArrayList<>());
    }


    public List<RewardDropEntry> retrieveSpecialDrop(int mapid, int dropperId) {
        return specialdrops.computeIfAbsent(mapid, k -> new HashMap<>()).computeIfAbsent(dropperId, k -> new ArrayList<>());
    }

    public RewardDropEntry getReward(int mapid, int dropperId) {
        List<RewardDropEntry> dropEntry = retrieveSpecialDrop(0, dropperId);
        dropEntry.addAll(retrieveSpecialDrop(mapid, dropperId));
        int chance = (int) Math.floor(Math.random() * 1000);
        List<RewardDropEntry> ret = new ArrayList<>();
        for (RewardDropEntry de : dropEntry) {
            if (de.chance >= chance) {
                ret.add(de);
            }
        }
        if (ret.isEmpty()) {
            return null;
        }
        Collections.shuffle(ret);
        return ret.get(Randomizer.nextInt(ret.size()));
    }

    public void addExtra() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Entry<Integer, List<MonsterDropEntry>> e : drops.entrySet()) {
            for (int i = 0; i < e.getValue().size(); i++) {
                if (e.getValue().get(i).itemId != 0 && !ii.itemExists(e.getValue().get(i).itemId)) {
                    e.getValue().remove(i);
                }
            }
            MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(e.getKey());
            Integer item = ii.getItemIdByMob(e.getKey());
            if (item != null && item > 0) {
                if (mons == null || item / 10000 == 238) { //去掉怪物卡片
                    continue;
                }
                e.getValue().add(new MonsterDropEntry(item, mons.isBoss() ? 1000000 : 10000, 1, 1, 0));
            }
        }
        /*
         * 怪物添加爆率
         */
        for (Entry<Integer, List<MonsterDropEntry>> e : drops.entrySet()) {
            if (e.getKey() != 9400408 && mobStringData.getChildByPath(String.valueOf(e.getKey())) != null) {
                for (MapleData d : mobStringData.getChildByPath(e.getKey() + "/reward")) {
                    int toAdd = MapleDataTool.getInt(d, 0);
                    if (toAdd > 0 && !contains(e.getValue(), toAdd) && ii.itemExists(toAdd)) {
                        if (toAdd / 10000 == 238 //掉怪物卡片
                                || toAdd / 10000 == 243 //盒子
                                || toAdd / 10000 == 399 //字母
                                || toAdd == 4001126 //枫叶
                                || toAdd == 4001128 //火药桶
                                || toAdd == 4001246 //温暖的阳光
                                || toAdd == 4001473 //圣诞树装饰
                                || toAdd == 4001447 //一起来行动
                                || toAdd == 2022450 //经验值上升(小)
                                || toAdd == 2022451 //经验值上升(中)
                                || toAdd == 2022452 //经验值上升(大)
                                || toAdd == 4032302 //神奇的颜料
                                || toAdd == 4032303 //神秘的颜料
                                || toAdd == 4032304 //魔法颜料
                                ) {
                            continue;
                        }
                        e.getValue().add(new MonsterDropEntry(toAdd, chanceLogic(toAdd), 1, 1, 0));
                    }
                }
            }
        }
    }

    public void addMeso(MapleMonsterStats mons, List<MonsterDropEntry> ret) {
        double divided = (mons.getLevel() < 100 ? (mons.getLevel() < 10 ? (double) mons.getLevel() : 10.0) : (mons.getLevel() / 10.0));
        int maxMeso = mons.getLevel() * (int) Math.ceil(mons.getLevel() / divided);
        if (mons.isBoss() && !mons.isPartyBonus()) {
            maxMeso *= 3;
        }
        for (int i = 0; i < mons.dropsMesoCount(); i++) {
            if (mons.getId() >= 9600086 && mons.getId() <= 9600098) { //外星人金币爆率
                int meso = (int) Math.floor(Math.random() * 500 + 1000);
                ret.add(new MonsterDropEntry(0, 20000, (int) Math.floor(0.46 * meso), meso, 0));
            } else {
                ret.add(new MonsterDropEntry(0, mons.isBoss() && !mons.isPartyBonus() ? 800000 : (mons.isPartyBonus() ? 600000 : 400000), (int) Math.floor(0.66 * maxMeso), maxMeso, 0));
            }
        }
    }

    public void clearDrops() {
        drops.clear();
        globaldrops.clear();
        specialdrops.clear();
        load();
        addExtra();
    }

    public boolean contains(List<MonsterDropEntry> e, int toAdd) {
        for (MonsterDropEntry f : e) {
            if (f.itemId == toAdd) {
                return true;
            }
        }
        return false;
    }

    public int chanceLogic(int itemId) { //not much logic in here. most of the drops should already be there anyway.
//        switch (itemId) {
//            case 4280000: //永恒的谜之蛋
//            case 4280001: //重生的谜之蛋
//            case 2049301: //装备强化卷轴
//            case 2049401: //潜能附加卷轴
//                return 5000;
//            case 2049300: //高级装备强化卷轴
//            case 2049400: //高级潜能附加卷轴
//            case 1002419: //枫叶帽
//                return 2000;
//            case 1002938: //安全帽（1日）
//                return 50;
//        }
        if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
            return 8000; //with *10
        } else if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.SETUP || ItemConstants.getInventoryType(itemId) == MapleInventoryType.CASH) {
            return 500;
        } else {
            switch (itemId / 10000) {
                case 204: //卷轴
                    return 1800;
                case 207: //飞镖之类
                case 233: //子弹之类
                    return 3000;
                case 229: //技能书
                    return 400;
                case 401: //矿
                case 402: //矿
                    return 5000;
                case 403:
                    return 4000; //lol
            }
            return 8000;
        }
    }
    //MESO DROP: level * (level / 10) = max, min = 0.66 * max
    //explosive Reward = 7 meso drops
    //boss, ffaloot = 2 meso drops
    //boss = level * level = max
    //no mesos if: mobid / 100000 == 97 or 95 or 93 or 91 or 90 or removeAfter > 0 or invincible or onlyNormalAttack or friendly or dropitemperiod > 0 or cp > 0 or point > 0 or fixeddamage > 0 or selfd > 0 or mobType != null and mobType.charat(0) == 7 or PDRate <= 0
}
