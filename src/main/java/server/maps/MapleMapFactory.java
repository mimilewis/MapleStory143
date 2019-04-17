package server.maps;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import configs.ServerConfig;
import constants.BattleConstants;
import constants.GameConstants;
import database.DatabaseConnection;
import provider.*;
import redis.clients.jedis.Jedis;
import server.MaplePortal;
import server.life.*;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import tools.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MapleMapFactory {

    private static final MapleDataProvider source = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Map.wz"));
    private static final MapleData nameData = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/String.wz")).getData("Map.img");
    private final HashMap<Integer, MapleMap> maps = new HashMap<>();
    private final HashMap<Integer, MapleMap> instanceMap = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private int channel;
    private Map<Integer, List<Integer>> linknpcs = new HashMap<>();

    public MapleMapFactory(int channel) {
        this.channel = channel;
    }

    public static void loadAllMapName() {
        Jedis jedis = RedisUtil.getJedis();
        try {
            if (!jedis.exists(RedisUtil.KEYNAMES.MAP_NAME.getKeyName())) {
                for (MapleData mapleData : nameData) {
                    for (MapleData data : mapleData) {
                        for (MapleData subdata : data) {
                            if (subdata.getName().equalsIgnoreCase("mapName")) {
                                jedis.hset(RedisUtil.KEYNAMES.MAP_NAME.getKeyName(), data.getName(), subdata.getData().toString());
                            }
                        }
                    }
                }
            }
        } finally {
            RedisUtil.returnResource(jedis);
        }
    }

    public static String getMapName(int mapid) {
        return RedisUtil.hget(RedisUtil.KEYNAMES.MAP_NAME.getKeyName(), String.valueOf(mapid));
    }

    public static void loadAllLinkNpc() {
        Jedis jedis = RedisUtil.getJedis();
        if (!jedis.exists(RedisUtil.KEYNAMES.MAP_LINKNPC.getKeyName())) {

            for (MapleDataDirectoryEntry directoryEntry : source.getRoot().getSubdirectories()) {
                if (directoryEntry.getName().equals("Map")) {
                    for (MapleDataDirectoryEntry directoryEntry1 : directoryEntry.getSubdirectories()) {
                        if (directoryEntry1.getName().startsWith("Map")) {
                            for (MapleDataFileEntry fileEntry : directoryEntry1.getFiles()) {
                                for (MapleData life : source.getData(directoryEntry.getName() + "/" + directoryEntry1.getName() + "/" + fileEntry.getName())) {
                                    if (life.getName().equals("life")) {
                                        List<Integer> npcids = new ArrayList<>();
                                        for (MapleData mapleData : life) {
                                            MapleData type1 = mapleData.getChildByPath("type");
                                            if (type1 == null) {
                                                continue;
                                            }
                                            String type = MapleDataTool.getString(type1);
                                            if (type != null && type.equals("n")) {
                                                npcids.add(MapleDataTool.getIntConvert(mapleData.getChildByPath("id"), 0));
                                            }
                                        }
                                        if (!npcids.isEmpty()) {
                                            try {
                                                jedis.hset(RedisUtil.KEYNAMES.MAP_LINKNPC.getKeyName(), Integer.valueOf(fileEntry.getName().substring(0, 9)).toString(), JsonUtil.getMapperInstance().writeValueAsString(npcids));
                                            } catch (JsonProcessingException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        RedisUtil.returnResource(jedis);
    }

    public static Map<Integer, List<Integer>> getAllLinkNpc() {
        Map<Integer, List<Integer>> ret = new HashMap<>();
        Map<String, String> tempdata = RedisUtil.hgetAll(RedisUtil.KEYNAMES.MAP_LINKNPC.getKeyName());
        for (Map.Entry<String, String> entry : tempdata.entrySet()) {
            try {
                ret.put(Integer.valueOf(entry.getKey()), JsonUtil.getMapperInstance().readValue(entry.getValue(), new TypeReference<List<Integer>>() {
                }));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /*
     * 获取地图的xml文件的名字
     */
    private static String getMapXMLName(int mapid) {
        String mapName = StringUtil.getLeftPaddedStr(Integer.toString(mapid), '0', 9);
        String builder = "Map/Map" + mapid / 100000000 +
                "/" +
                mapName +
                ".img";
        mapName = builder;
        return mapName;
    }

    public MapleMap getMap(int mapid) {
        return getMap(mapid, true, true, true);
    }

    //backwards-compatible
    public MapleMap getMap(int mapid, boolean respawns, boolean npcs) {
        return getMap(mapid, respawns, npcs, true);
    }

    public MapleMap getMap(int mapid, boolean respawns, boolean npcs, boolean reactors) {
        Integer omapid = mapid;
        MapleMap map = maps.get(omapid);
        if (map == null) {
            lock.lock();
            try {
                map = maps.get(omapid);
                if (map != null) {
                    return map;
                }
                MapleData mapData;
                try {
                    mapData = source.getData(getMapXMLName(mapid));
                } catch (Exception e) {
                    return null;
                }
                if (mapData == null) {
                    return null;
                }
                int linkMapId = -1;
                MapleData link = mapData.getChildByPath("info/link");
                if (link != null) {
                    linkMapId = MapleDataTool.getIntConvert("info/link", mapData);
                    mapData = source.getData(getMapXMLName(linkMapId));
                }

                float monsterRate = 0;
                if (respawns) {
                    MapleData mobRate = mapData.getChildByPath("info/mobRate");
                    if (mobRate != null) {
                        monsterRate = (Float) mobRate.getData();
                    }
                }

                // v118 自由市场房间xml数据不完整，因此填空为本图内
                int freeMarket = mapid % 910000000;
                if (freeMarket >= 1 && freeMarket <= 22) {
                    map = new MapleMap(GameConstants.getOverrideChangeToMap(mapid), channel, mapid, monsterRate);
                } else {
                    map = new MapleMap(mapid, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);
                }

                loadPortals(map, mapData.getChildByPath("portal"));
                map.setTop(MapleDataTool.getInt(mapData.getChildByPath("info/VRTop"), 0));
                map.setLeft(MapleDataTool.getInt(mapData.getChildByPath("info/VRLeft"), 0));
                map.setBottom(MapleDataTool.getInt(mapData.getChildByPath("info/VRBottom"), 0));
                map.setRight(MapleDataTool.getInt(mapData.getChildByPath("info/VRRight"), 0));
                List<MapleFoothold> allFootholds = new LinkedList<>();
                Point lBound = new Point();
                Point uBound = new Point();
                MapleFoothold fh;

                for (MapleData footRoot : mapData.getChildByPath("foothold")) {
                    for (MapleData footCat : footRoot) {
                        for (MapleData footHold : footCat) {
                            fh = new MapleFoothold(new Point(
                                    MapleDataTool.getInt(footHold.getChildByPath("x1"), 0),
                                    MapleDataTool.getInt(footHold.getChildByPath("y1"), 0)),
                                    new Point(
                                            MapleDataTool.getInt(footHold.getChildByPath("x2"), 0),
                                            MapleDataTool.getInt(footHold.getChildByPath("y2"), 0)),
                                    Integer.parseInt(footHold.getName()));
                            fh.setPrev((short) MapleDataTool.getInt(footHold.getChildByPath("prev"), 0));
                            fh.setNext((short) MapleDataTool.getInt(footHold.getChildByPath("next"), 0));

                            if (fh.getX1() < lBound.x) {
                                lBound.x = fh.getX1();
                            }
                            if (fh.getX2() > uBound.x) {
                                uBound.x = fh.getX2();
                            }
                            if (fh.getY1() < lBound.y) {
                                lBound.y = fh.getY1();
                            }
                            if (fh.getY2() > uBound.y) {
                                uBound.y = fh.getY2();
                            }
                            allFootholds.add(fh);
                        }
                    }
                }
                MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
                for (MapleFoothold foothold : allFootholds) {
                    fTree.insert(foothold);
                }
                map.setFootholds(fTree);
                if (map.getTop() == 0) {
                    map.setTop(lBound.y);
                }
                if (map.getBottom() == 0) {
                    map.setBottom(uBound.y);
                }
                if (map.getLeft() == 0) {
                    map.setLeft(lBound.x);
                }
                if (map.getRight() == 0) {
                    map.setRight(uBound.x);
                }
                int bossid = -1;
                String msg = null;
                if (mapData.getChildByPath("info/timeMob") != null) {
                    bossid = MapleDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
                    msg = MapleDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
                }

                try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                    PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?");
                    ps.setInt(1, omapid);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        int sqlid = rs.getInt("idd");
                        int sqlf = rs.getInt("f");
                        boolean sqlhide = false;
                        String sqltype = rs.getString("type");
                        int sqlfh = rs.getInt("fh");
                        int sqlcy = rs.getInt("cy");
                        int sqlrx0 = rs.getInt("rx0");
                        int sqlrx1 = rs.getInt("rx1");
                        int sqlx = rs.getInt("x");
                        int sqly = rs.getInt("y");
                        int sqlmobTime = rs.getInt("mobtime");
                        AbstractLoadedMapleLife sqlmyLife = loadLife(sqlid, sqlf, sqlhide, sqlfh, sqlcy, sqlrx0, sqlrx1, sqlx, sqly, sqltype, mapid);
                        switch (sqltype) {
                            case "n":
                                map.addMapObject(sqlmyLife);
                                break;
                            case "m":
                                MapleMonster monster = (MapleMonster) sqlmyLife;
                                map.addMonsterSpawn(monster, sqlmobTime, (byte) -1, null);
                                break;
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("读取SQL刷Npc和刷新怪物出错.");
                }
                /*
                 * load life data (npc, monsters)
                 * 加载地图刷新的NPC和怪物信息
                 */
                List<Point> herbRocks = new ArrayList<>();
                int lowestLevel = 200, highestLevel = 0;
                String type, limited;
                AbstractLoadedMapleLife myLife;

                for (MapleData life : mapData.getChildByPath("life")) {
                    String id = MapleDataTool.getString(life.getChildByPath("id"));
                    type = MapleDataTool.getString(life.getChildByPath("type"));
                    limited = MapleDataTool.getString("limitedname", life, "");
                    if ((npcs || !type.equals("n")) && !limited.equals("Stage0")) { //alien pq stuff
                        myLife = loadLife(life, id, type, mapid);

                        if (myLife instanceof MapleMonster && !BattleConstants.isBattleMap(mapid) && !GameConstants.isNoSpawn(mapid)) {
                            MapleMonster mob = (MapleMonster) myLife;

                            herbRocks.add(map.addMonsterSpawn(mob, MapleDataTool.getInt("mobTime", life, 0), (byte) MapleDataTool.getInt("team", life, -1), mob.getId() == bossid ? msg : null).getPosition());
                            if (mob.getStats().getLevel() > highestLevel && !mob.getStats().isBoss()) {
                                highestLevel = mob.getStats().getLevel();
                            }
                            if (mob.getStats().getLevel() < lowestLevel && !mob.getStats().isBoss()) {
                                lowestLevel = mob.getStats().getLevel();
                            }
                        } else if (myLife instanceof MapleNPC) {
                            for (MapleHideNpc h : MapleHideNpc.values()) {
                                if (Integer.parseInt(id) == h.getNpcId()) {
                                    map.addHideNpc(h);
                                }
                            }
                            map.addMapObject(myLife);
                        }
                    }
                }
                addAreaBossSpawn(map);
                map.setCreateMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), ServerConfig.CHANNEL_MONSTER_REFRESH * 1000));
                map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
                map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
                map.loadMonsterRate(true);
                map.setNodes(loadNodes(mapid, mapData));
                map.setSpawnPoints(herbRocks);
                /*
                 * 加载地图刷新的反应堆信息
                 */
                String id;
                if (reactors && mapData.getChildByPath("reactor") != null && !BattleConstants.isBattleMap(mapid)) {
                    for (MapleData reactor : mapData.getChildByPath("reactor")) {
                        id = MapleDataTool.getString(reactor.getChildByPath("id"));
                        if (id != null) {
                            map.spawnReactor(loadReactor(reactor, id, (byte) MapleDataTool.getInt(reactor.getChildByPath("f"), 0)));
                        }
                    }
                }
                map.setFirstUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
                map.setUserEnter(mapid == GameConstants.JAIL ? "jail" : MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
                if (reactors && herbRocks.size() > 0 && highestLevel >= 30 && map.getFirstUserEnter().equals("") && map.getUserEnter().equals("")) {
                    List<Integer> allowedSpawn = new ArrayList<>(24);
                    allowedSpawn.add(100011); //金色药草堆
                    allowedSpawn.add(200011); //怦然心跳
                    if (highestLevel >= 100) {
                        for (int i = 0; i < 10; i++) {
                            for (int x = 0; x < 4; x++) { //to make heartstones rare
                                allowedSpawn.add(100000 + i);
                                allowedSpawn.add(200000 + i);
                            }
                        }
                    } else {
                        for (int i = (lowestLevel % 10 > highestLevel % 10 ? 0 : (lowestLevel % 10)); i < (highestLevel % 10); i++) {
                            for (int x = 0; x < 4; x++) { //to make heartstones rare
                                allowedSpawn.add(100000 + i);
                                allowedSpawn.add(200000 + i);
                            }
                        }
                    }
                    int numSpawn = Randomizer.nextInt(allowedSpawn.size()) / 6; //0-7
                    for (int i = 0; i < numSpawn && !herbRocks.isEmpty(); i++) {
                        int idd = allowedSpawn.get(Randomizer.nextInt(allowedSpawn.size()));
                        int theSpawn = Randomizer.nextInt(herbRocks.size());
                        MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(idd), idd);
                        myReactor.setPosition(herbRocks.get(theSpawn));
                        myReactor.setDelay(idd % 100 == 11 ? 60000 : 5000); //in the reactor's wz
                        map.spawnReactor(myReactor);
                        herbRocks.remove(theSpawn);
                    }
                }
                /*
                 * 设置地图名称
                 */
                try {
                    map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(linkMapId > 0 ? linkMapId : omapid)), ""));
                    map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(linkMapId > 0 ? linkMapId : omapid)), ""));
                } catch (Exception e) {
                    map.setMapName("");
                    map.setStreetName("");
                }
                /*
                 * 设置地图一些状态
                 */
                map.setClock(mapData.getChildByPath("clock") != null); //clock was changed in wz to have x,y,width,height
                map.setEverlast(MapleDataTool.getInt(mapData.getChildByPath("info/everlast"), 0) > 0);
                map.setTown(MapleDataTool.getInt(mapData.getChildByPath("info/town"), 0) > 0);
                map.setSoaring(MapleDataTool.getInt(mapData.getChildByPath("info/needSkillForFly"), 0) > 0);
                map.setPersonalShop(MapleDataTool.getInt(mapData.getChildByPath("info/personalShop"), 0) > 0);
                map.setEntrustedFishing(MapleDataTool.getInt(mapData.getChildByPath("info/entrustedFishing"), 0) > 0);
                map.setForceMove(MapleDataTool.getInt(mapData.getChildByPath("info/lvForceMove"), 0));
                map.setHPDec(MapleDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
                map.setHPDecInterval(MapleDataTool.getInt(mapData.getChildByPath("info/decHPInterval"), 10000));
                map.setHPDecProtect(MapleDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
                map.setForcedReturnMap(mapid == 0 ? 999999999 : MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
                map.setTimeLimit(MapleDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
                map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
                map.setMiniMapOnOff(MapleDataTool.getInt(mapData.getChildByPath("info/miniMapOnOff"), 0) > 0);
                map.setRecoveryRate(MapleDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
                map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
                map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
                map.setConsumeItemCoolTime(MapleDataTool.getInt(mapData.getChildByPath("info/consumeItemCoolTime"), 0));
                maps.put(omapid, map);
            } finally {
                lock.unlock();
            }
        }
        return map;
    }

    public MapleMap getInstanceMap(int instanceid) {
        return instanceMap.get(instanceid);
    }

    public void removeInstanceMap(int instanceid) {
        lock.lock();
        try {
            if (isInstanceMapLoaded(instanceid)) {
                getInstanceMap(instanceid).checkStates("");
                instanceMap.remove(instanceid);
            }
        } finally {
            lock.unlock();
        }
    }

    public void removeMap(int instanceid) {
        lock.lock();
        try {
            if (isMapLoaded(instanceid)) {
                getMap(instanceid).checkStates("");
                maps.remove(instanceid);
            }
        } finally {
            lock.unlock();
        }
    }

    public MapleMap CreateInstanceMap(int mapid, boolean respawns, boolean npcs, boolean reactors, int instanceid) {
        lock.lock();
        try {
            if (isInstanceMapLoaded(instanceid)) {
                return getInstanceMap(instanceid);
            }
        } finally {
            lock.unlock();
        }
        MapleData mapData = null;
        try {
            mapData = source.getData(getMapXMLName(mapid));
        } catch (Exception e) {
            return null;
        }
        if (mapData == null) {
            return null;
        }
        MapleData link = mapData.getChildByPath("info/link");
        if (link != null) {
            mapData = source.getData(getMapXMLName(MapleDataTool.getIntConvert("info/link", mapData)));
        }

        float monsterRate = 0;
        if (respawns) {
            MapleData mobRate = mapData.getChildByPath("info/mobRate");
            if (mobRate != null) {
                monsterRate = (Float) mobRate.getData();
            }
        }
        MapleMap map = new MapleMap(mapid, channel, MapleDataTool.getInt("info/returnMap", mapData), monsterRate);
        loadPortals(map, mapData.getChildByPath("portal"));
        map.setTop(MapleDataTool.getInt(mapData.getChildByPath("info/VRTop"), 0));
        map.setLeft(MapleDataTool.getInt(mapData.getChildByPath("info/VRLeft"), 0));
        map.setBottom(MapleDataTool.getInt(mapData.getChildByPath("info/VRBottom"), 0));
        map.setRight(MapleDataTool.getInt(mapData.getChildByPath("info/VRRight"), 0));
        map.setFieldType(MapleDataTool.getInt(mapData.getChildByPath("info/fieldType"), 0));
        List<MapleFoothold> allFootholds = new LinkedList<>();
        Point lBound = new Point();
        Point uBound = new Point();
        for (MapleData footRoot : mapData.getChildByPath("foothold")) {
            for (MapleData footCat : footRoot) {
                for (MapleData footHold : footCat) {
                    MapleFoothold fh = new MapleFoothold(new Point(
                            MapleDataTool.getInt(footHold.getChildByPath("x1")),
                            MapleDataTool.getInt(footHold.getChildByPath("y1"))),
                            new Point(
                                    MapleDataTool.getInt(footHold.getChildByPath("x2")),
                                    MapleDataTool.getInt(footHold.getChildByPath("y2"))),
                            Integer.parseInt(footHold.getName()));
                    fh.setPrev((short) MapleDataTool.getInt(footHold.getChildByPath("prev")));
                    fh.setNext((short) MapleDataTool.getInt(footHold.getChildByPath("next")));

                    if (fh.getX1() < lBound.x) {
                        lBound.x = fh.getX1();
                    }
                    if (fh.getX2() > uBound.x) {
                        uBound.x = fh.getX2();
                    }
                    if (fh.getY1() < lBound.y) {
                        lBound.y = fh.getY1();
                    }
                    if (fh.getY2() > uBound.y) {
                        uBound.y = fh.getY2();
                    }
                    allFootholds.add(fh);
                }
            }
        }
        MapleFootholdTree fTree = new MapleFootholdTree(lBound, uBound);
        for (MapleFoothold fh : allFootholds) {
            fTree.insert(fh);
        }
        map.setFootholds(fTree);
        if (map.getTop() == 0) {
            map.setTop(lBound.y);
        }
        if (map.getBottom() == 0) {
            map.setBottom(uBound.y);
        }
        if (map.getLeft() == 0) {
            map.setLeft(lBound.x);
        }
        if (map.getRight() == 0) {
            map.setRight(uBound.x);
        }
        int bossid = -1;
        String msg = null;
        if (mapData.getChildByPath("info/timeMob") != null) {
            bossid = MapleDataTool.getInt(mapData.getChildByPath("info/timeMob/id"), 0);
            msg = MapleDataTool.getString(mapData.getChildByPath("info/timeMob/message"), null);
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM spawns WHERE mid = ?");
            ps.setInt(1, mapid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int sqlid = rs.getInt("idd");
                int sqlf = rs.getInt("f");
                boolean sqlhide = false;
                String sqltype = rs.getString("type");
                int sqlfh = rs.getInt("fh");
                int sqlcy = rs.getInt("cy");
                int sqlrx0 = rs.getInt("rx0");
                int sqlrx1 = rs.getInt("rx1");
                int sqlx = rs.getInt("x");
                int sqly = rs.getInt("y");
                int sqlmobTime = rs.getInt("mobtime");
                AbstractLoadedMapleLife sqlmyLife = loadLife(sqlid, sqlf, sqlhide, sqlfh, sqlcy, sqlrx0, sqlrx1, sqlx, sqly, sqltype, mapid);
                if (sqltype.equals("n")) {
                    map.addMapObject(sqlmyLife);
                } else if (sqltype.equals("m")) {
                    MapleMonster monster = (MapleMonster) sqlmyLife;
                    map.addMonsterSpawn(monster, sqlmobTime, (byte) -1, null);
                }
            }
        } catch (SQLException e) {
            System.out.println("读取SQL刷Npc和刷新怪物出错.");
        }
        // load life data (npc, monsters)
        String type, limited;
        AbstractLoadedMapleLife myLife;

        for (MapleData life : mapData.getChildByPath("life")) {
            type = MapleDataTool.getString(life.getChildByPath("type"));
            limited = MapleDataTool.getString("limitedname", life, "");
            if ((npcs || !type.equals("n")) && limited.equals("")) {
                myLife = loadLife(life, MapleDataTool.getString(life.getChildByPath("id")), type, mapid);
                if (myLife instanceof MapleMonster && !BattleConstants.isBattleMap(mapid) && !GameConstants.isNoSpawn(mapid)) {
                    MapleMonster mob = (MapleMonster) myLife;
                    map.addMonsterSpawn(mob, MapleDataTool.getInt("mobTime", life, 0), (byte) MapleDataTool.getInt("team", life, -1), mob.getId() == bossid ? msg : null);
                } else if (myLife instanceof MapleNPC) {
                    map.addMapObject(myLife);
                }
            }
        }
        addAreaBossSpawn(map);
        map.setCreateMobInterval((short) MapleDataTool.getInt(mapData.getChildByPath("info/createMobInterval"), ServerConfig.CHANNEL_MONSTER_REFRESH * 1000));
        map.setFixedMob(MapleDataTool.getInt(mapData.getChildByPath("info/fixedMobCapacity"), 0));
        map.setPartyBonusRate(GameConstants.getPartyPlay(mapid, MapleDataTool.getInt(mapData.getChildByPath("info/partyBonusR"), 0)));
        map.loadMonsterRate(true);
        map.setNodes(loadNodes(mapid, mapData));

        //load reactor data
        String id;
        if (reactors && mapData.getChildByPath("reactor") != null && !BattleConstants.isBattleMap(mapid)) {
            for (MapleData reactor : mapData.getChildByPath("reactor")) {
                id = MapleDataTool.getString(reactor.getChildByPath("id"));
                if (id != null) {
                    map.spawnReactor(loadReactor(reactor, id, (byte) MapleDataTool.getInt(reactor.getChildByPath("f"), 0)));
                }
            }
        }
        try {
            map.setMapName(MapleDataTool.getString("mapName", nameData.getChildByPath(getMapStringName(mapid)), ""));
            map.setStreetName(MapleDataTool.getString("streetName", nameData.getChildByPath(getMapStringName(mapid)), ""));
        } catch (Exception e) {
            map.setMapName("");
            map.setStreetName("");
        }
        map.setClock(MapleDataTool.getInt(mapData.getChildByPath("info/clock"), 0) > 0);
        map.setEverlast(MapleDataTool.getInt(mapData.getChildByPath("info/everlast"), 0) > 0);
        map.setTown(MapleDataTool.getInt(mapData.getChildByPath("info/town"), 0) > 0);
        map.setSoaring(MapleDataTool.getInt(mapData.getChildByPath("info/needSkillForFly"), 0) > 0);
        map.setForceMove(MapleDataTool.getInt(mapData.getChildByPath("info/lvForceMove"), 0));
        map.setHPDec(MapleDataTool.getInt(mapData.getChildByPath("info/decHP"), 0));
        map.setHPDecInterval(MapleDataTool.getInt(mapData.getChildByPath("info/decHPInterval"), 10000));
        map.setHPDecProtect(MapleDataTool.getInt(mapData.getChildByPath("info/protectItem"), 0));
        map.setForcedReturnMap(MapleDataTool.getInt(mapData.getChildByPath("info/forcedReturn"), 999999999));
        map.setTimeLimit(MapleDataTool.getInt(mapData.getChildByPath("info/timeLimit"), -1));
        map.setFieldLimit(MapleDataTool.getInt(mapData.getChildByPath("info/fieldLimit"), 0));
        map.setFirstUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onFirstUserEnter"), ""));
        map.setUserEnter(MapleDataTool.getString(mapData.getChildByPath("info/onUserEnter"), ""));
        map.setMiniMapOnOff(MapleDataTool.getInt(mapData.getChildByPath("info/miniMapOnOff"), 0) > 0);
        map.setRecoveryRate(MapleDataTool.getFloat(mapData.getChildByPath("info/recovery"), 1));
        map.setConsumeItemCoolTime(MapleDataTool.getInt(mapData.getChildByPath("info/consumeItemCoolTime"), 0));
        map.setInstanceId(instanceid);
        lock.lock();
        try {
            instanceMap.put(instanceid, map);
        } finally {
            lock.unlock();
        }
        return map;
    }

    public int getLoadedMaps() {
        return maps.size();
    }

    public boolean isMapLoaded(int mapId) {
        return maps.containsKey(mapId);
    }

    public boolean isInstanceMapLoaded(int instanceid) {
        return instanceMap.containsKey(instanceid);
    }

    public void clearLoadedMap() {
        lock.lock();
        try {
            maps.clear();
        } finally {
            lock.unlock();
        }
    }

    public List<MapleMap> getAllLoadedMaps() {
        List<MapleMap> ret = new ArrayList<>();
        lock.lock();
        try {
            ret.addAll(maps.values());
            ret.addAll(instanceMap.values());
        } finally {
            lock.unlock();
        }
        return ret;
    }

    public Collection<MapleMap> getAllMaps() {
        return maps.values();
    }

    private AbstractLoadedMapleLife loadLife(MapleData life, String id, String type, int mapid) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(Integer.parseInt(id), type, mapid);
        if (myLife == null) {
            return null;
        }
        myLife.setCy(MapleDataTool.getInt(life.getChildByPath("cy")));
        MapleData dF = life.getChildByPath("f");
        if (dF != null) {
            myLife.setF(MapleDataTool.getInt(dF));
        }
        myLife.setFh(MapleDataTool.getInt(life.getChildByPath("fh")));
        myLife.setRx0(MapleDataTool.getInt(life.getChildByPath("rx0")));
        myLife.setRx1(MapleDataTool.getInt(life.getChildByPath("rx1")));
        myLife.setPosition(new Point(MapleDataTool.getIntConvert(life.getChildByPath("x")), MapleDataTool.getIntConvert(life.getChildByPath("y"))));

        if (MapleDataTool.getInt("hide", life, 0) == 1 && myLife instanceof MapleNPC) {
            myLife.setHide(true);
        }
        return myLife;
    }

    private AbstractLoadedMapleLife loadLife(int id, int f, boolean hide, int fh, int cy, int rx0, int rx1, int x, int y, String type, int mapid) {
        AbstractLoadedMapleLife myLife = MapleLifeFactory.getLife(id, type, mapid);
        if (myLife == null) {
            System.out.println("加载npc错误，id：" + id + "  type：" + type);
            return null;
        }
        myLife.setCy(cy);
        myLife.setF(f);
        myLife.setFh(fh);
        myLife.setRx0(rx0);
        myLife.setRx1(rx1);
        myLife.setPosition(new Point(x, y));
        myLife.setHide(hide);
        return myLife;
    }

    private MapleReactor loadReactor(MapleData reactor, String id, byte FacingDirection) {
        MapleReactor myReactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(id)), Integer.parseInt(id));
        myReactor.setFacingDirection(FacingDirection);
        myReactor.setPosition(new Point(MapleDataTool.getInt(reactor.getChildByPath("x")), MapleDataTool.getInt(reactor.getChildByPath("y"))));
        myReactor.setDelay(MapleDataTool.getInt(reactor.getChildByPath("reactorTime")) * 1000);
        myReactor.setName(MapleDataTool.getString(reactor.getChildByPath("name"), ""));
        return myReactor;
    }

    /*
     * 获取地图名字在Map.img.xml中的位置信息
     */
    private String getMapStringName(int mapid) {
        StringBuilder builder = new StringBuilder();
        if (mapid < 100000000) { //OK
            builder.append("maple");
        } else if (mapid >= 100000000 && mapid < 200000000) { //OK
            builder.append("victoria");
        } else if (mapid >= 200000000 && mapid < 300000000) { //OK
            builder.append("ossyria");
        } else if (mapid >= 300000000 && mapid < 400000000) { //OK
            builder.append("3rd");
        } else if (mapid >= 400000000 && mapid < 410000000 || mapid >= 940000000 && mapid < 941000000) { //ok
            builder.append("grandis");
        } else if (mapid >= 500000000 && mapid < 510000000) { //ok
            builder.append("thai");
        } else if (mapid >= 510000000 && mapid < 520000000) { //ok 吸血鬼城堡
            builder.append("EU");
        } else if (mapid >= 540000000 && mapid <= 555000000) { //OK
            builder.append("SG");
        } else if (mapid >= 600000000 && mapid <= 600020600) { //OK
            builder.append("MasteriaGL");
        } else if (mapid >= 677000000 && mapid < 678000000) { //OK
            builder.append("Episode1GL");
        } else if (mapid >= 680000000 && mapid < 681000000) { //OK
            builder.append("global");
        } else if (mapid >= 680100000 && mapid < 681100000 || mapid >= 682000000 && mapid < 683000000) { //OK
            builder.append("HalloweenGL");
        } else if (mapid >= 686000000 && mapid < 687000000) { //OK
            builder.append("event_6th");
        } else if (mapid >= 689010000 && mapid < 690000000) { //OK
            builder.append("Pink ZakumGL");
        } else if (mapid == 689000000 || mapid == 689000010) { //OK
            builder.append("CTF_GL");
        } else if (mapid >= 690010000 && mapid < 700000000 || mapid >= 746000000 && mapid < 746100000) { //OK
            builder.append("boost");
        } else if (mapid >= 700000000 && mapid < 742000000) { //OK
            builder.append("chinese");
        } else if (mapid >= 743000000 && mapid < 744000000) { //OK
            builder.append("taiwan");
        } else if (mapid >= 744000000 && mapid < 800000000) { //ok
            builder.append("chinese");
        } else if (mapid >= 860000000 && mapid < 861000000) { //ok 蔚蓝道路
            builder.append("aquaroad");
        } else if (mapid >= 863000000 && mapid < 864000000) { //ok V.113新增地图绯红
            builder.append("masteria");
        } else if (mapid >= 800000000 && mapid < 900000000) { //OK
            builder.append("jp");
        } else {
            builder.append("etc");
        }
        builder.append("/");
        builder.append(mapid);
        return builder.toString();
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    /*
     * 地图设置刷新特殊怪物
     */
    private void addAreaBossSpawn(MapleMap map) {
        int monsterid = -1; //怪物的ID
        int mobtime = -1; //重新刷出怪物的时间 按秒计算
        String msg = null; //怪物刷出来的提示
        boolean shouldSpawn = true;
        boolean sendWorldMsg = false; //怪物出现时是全服公告提示
        Point pos1 = null, pos2 = null, pos3 = null; //怪物刷出的坐标

        switch (map.getId()) {
            case 104010200: // 金银岛 - 小道2
                mobtime = 1200;
                monsterid = 2220000; // 红蜗牛王
                msg = "天气凉快了就会出现红蜗牛王。";
                pos1 = new Point(189, 2);
                pos2 = new Point(478, 250);
                pos3 = new Point(611, 489);
                break;
            case 102020500: // 北部岩山 - 旋风地带
                mobtime = 1200;
                monsterid = 3220000;
                msg = "树妖王出现了。";
                pos1 = new Point(1121, 2130);
                pos2 = new Point(483, 2171);
                pos3 = new Point(1474, 1706);
                break;
            case 100020101: // 蘑菇之歌森林 - 蘑菇王小道
                mobtime = 1200;
                monsterid = 6130101;
                msg = "什么地方出现了巨大的蘑菇。";
                pos1 = new Point(-311, 201);
                pos2 = new Point(-903, 197);
                pos3 = new Point(-568, 196);
                break;
            case 100020301: // 蘑菇之歌森林 - 蓝蘑菇王森林
                mobtime = 1200;
                monsterid = 8220007;
                msg = "什么地方出现了巨大的蓝色蘑菇。";
                pos1 = new Point(-188, -657);
                pos2 = new Point(625, -660);
                pos3 = new Point(508, -648);
                break;
            case 105010301: // 蘑菇之歌森林 - 僵尸蘑菇王山丘
                mobtime = 1200;
                monsterid = 6300005;
                msg = "什么地方出现了笼罩着阴暗气息的巨大蘑菇。";
                pos1 = new Point(-130, -773);
                pos2 = new Point(504, -760);
                pos3 = new Point(608, -641);
                break;
            case 120030500: // 黄金海滩 - 温暖的沙地
                mobtime = 1200;
                monsterid = 5220001;
                msg = "从沙滩里慢慢的走出了一只巨居蟹。";
                pos1 = new Point(-355, 179);
                pos2 = new Point(-1283, -113);
                pos3 = new Point(-571, -593);
                break;
            case 250010304: // 武陵 - 流浪熊的地盘
                mobtime = 2100;
                monsterid = 7220000;
                msg = "随着微弱的口哨声，肯德熊出现了。";
                pos1 = new Point(-210, 33);
                pos2 = new Point(-234, 393);
                pos3 = new Point(-654, 33);
                break;
            case 200010300: // 神秘岛 - 天空楼梯Ⅱ
                mobtime = 1200;
                monsterid = 8220000;
                msg = "艾利杰出现了。";
                pos1 = new Point(665, 83);
                pos2 = new Point(672, -217);
                pos3 = new Point(-123, -217);
                break;
            case 250010503: // 武陵 - 妖怪森林
                mobtime = 1800;
                monsterid = 7220002;
                msg = "周边的妖气慢慢浓厚，可以听到诡异的猫叫声。";
                pos1 = new Point(-303, 543);
                pos2 = new Point(227, 543);
                pos3 = new Point(719, 543);
                break;
            case 222010310: // 隐藏地图 - 月岭
                mobtime = 2700;
                monsterid = 7220001;
                msg = "在阴暗的月光中随着九尾狐的哭声，可以感受到它阴气。";
                pos1 = new Point(-169, -147);
                pos2 = new Point(-517, 93);
                pos3 = new Point(247, 93);
                break;
            case 103030400: // 沼泽地带 - 深泥坑
                mobtime = 1800;
                monsterid = 6220000;
                msg = "从沼泽出现了巨大的多尔。";
                pos1 = new Point(-831, 109);
                pos2 = new Point(1525, -75);
                pos3 = new Point(-511, 107);
                break;
            case 101040300: // 诅咒森林 - 污染的树
                mobtime = 1800;
                monsterid = 5220002;
                msg = "蓝雾慢慢散去，浮士德慢慢的显现了出来。";
                pos1 = new Point(600, -600);
                pos2 = new Point(600, -800);
                pos3 = new Point(600, -300);
                break;
            case 220050200: // 玩具城 - 丢失的时间<2>
                mobtime = 1500;
                monsterid = 5220003;
                msg = "嘀嗒嘀嗒! 随着规则的指针声出现了提莫。";
                pos1 = new Point(-467, 1032);
                pos2 = new Point(532, 1032);
                pos3 = new Point(-47, 1032);
                break;
            case 221040301: // 地球防御本部 - 哥雷草原
                mobtime = 2400;
                monsterid = 6220001;
                msg = "厚重的机器运作声，朱诺出现了!";
                pos1 = new Point(-4134, 416);
                pos2 = new Point(-4283, 776);
                pos3 = new Point(-3292, 776);
                break;
            case 240040401: // 隐藏地图 - 大海兽 峡谷
                mobtime = 7200;
                monsterid = 8220003;
                msg = "大海兽出现了。";
                pos1 = new Point(-15, 2481);
                pos2 = new Point(127, 1634);
                pos3 = new Point(159, 1142);
                break;
            case 260010201: // 隐藏地图 - 仙人掌爸爸沙漠
                mobtime = 3600;
                monsterid = 3220001;
                msg = "从沙尘中可以看到大宇的身影。";
                pos1 = new Point(-215, 275);
                pos2 = new Point(298, 275);
                pos3 = new Point(592, 275);
                break;
            case 251010102: // 百草堂 - 八十年药草地
                mobtime = 3600;
                monsterid = 5220004;
                msg = "大王蜈蚣出现了。";
                pos1 = new Point(-41, 124);
                pos2 = new Point(-173, 126);
                pos3 = new Point(79, 118);
                break;
            case 261030000: // 隐藏地图 - 研究所地下秘密通道
                mobtime = 2700;
                monsterid = 8220002;
                msg = "吉米拉出现了。";
                pos1 = new Point(-1094, -405);
                pos2 = new Point(-772, -116);
                pos3 = new Point(-108, 181);
                break;
            case 230020100: // 水下世界 - 海草之塔
                mobtime = 2700;
                monsterid = 4220000;
                msg = "在海草中间，出现了奇怪的蛤蚌。";
                pos1 = new Point(-291, -20);
                pos2 = new Point(-272, -500);
                pos3 = new Point(-462, 640);
                break;
            case 103020320: // 废都地铁 - 1号线第3区间
                mobtime = 1800;
                monsterid = 5090000;
                msg = "在地铁的阴影中出现了什么东西。";
                pos1 = new Point(79, 174);
                pos2 = new Point(-223, 296);
                pos3 = new Point(80, 275);
                break;
            case 103020420: // 废都地铁 - 2号线第3区间
                mobtime = 1800;
                monsterid = 5090000;
                msg = "在地铁的阴影中出现了什么东西。";
                pos1 = new Point(2241, 301);
                pos2 = new Point(1990, 301);
                pos3 = new Point(1684, 307);
                break;
            case 261020300: // 卡帕莱特研究所 - 研究所C-1 地区
                mobtime = 2700;
                monsterid = 7090000;
                msg = "自动警备系统出现了。";
                pos1 = new Point(312, 157);
                pos2 = new Point(539, 136);
                pos3 = new Point(760, 141);
                break;
            case 261020401: // 卡帕莱特研究所 - 禁止出入(除相关者外)
                mobtime = 2700;
                monsterid = 8090000;
                msg = "迪特和罗伊出现了。";
                pos1 = new Point(-263, 155);
                pos2 = new Point(-436, 122);
                pos3 = new Point(22, 144);
                break;
            case 250020300: // 武陵 - 上级修炼场
                mobtime = 2700;
                monsterid = 5090001;
                msg = "仙人玩偶出现了。";
                pos1 = new Point(1208, 27);
                pos2 = new Point(1654, 40);
                pos3 = new Point(927, -502);
                break;
            case 211050000: // 神秘岛 - 寒冰平原
                mobtime = 2700;
                monsterid = 6090001;
                msg = "被束缚在冰里的魔女睁开了眼睛。";
                pos1 = new Point(-233, -431);
                pos2 = new Point(-370, -426);
                pos3 = new Point(-526, -420);
                break;
            case 261010003: // 蒙特鸠研究所 - 研究所103号
                mobtime = 2700;
                monsterid = 6090004;
                msg = "陆陆猫出现了。";
                pos1 = new Point(-861, 301);
                pos2 = new Point(-703, 301);
                pos3 = new Point(-426, 287);
                break;
            case 222010300: // 童话村 - 狐狸山坡
                mobtime = 2700;
                monsterid = 6090003;
                msg = "书生鬼出现了。";
                pos1 = new Point(1300, -400);
                pos2 = new Point(1100, -100);
                pos3 = new Point(1100, 100);
                break;
            case 251010101: // 百草堂 - 六十年药草地
                mobtime = 2700;
                monsterid = 6090002;
                msg = "竹林里出现了一个来历不明的青竹武士，只要打碎小竹片，就可让青竹武士大发雷霆而葬失自制力，并将他打倒。";
                pos1 = new Point(-15, -449);
                pos2 = new Point(-114, -442);
                pos3 = new Point(-255, -446);
                break;
            case 211041400: // 神秘岛 - 死亡之林Ⅳ
                mobtime = 2700;
                monsterid = 6090000;
                msg = "黑山老妖出现了！";
                pos1 = new Point(1672, 82);
                pos2 = new Point(2071, 10);
                pos3 = new Point(1417, 57);
                break;
            case 105030500: // 被诅咒的寺院 - 禁忌祭坛
                mobtime = 2700;
                monsterid = 8130100;
                msg = "蝙蝠怪出现了。";
                pos1 = new Point(1275, -399);
                pos2 = new Point(1254, -412);
                pos3 = new Point(1058, -427);
                break;
            case 105020400: // 龙族洞穴 - 洞穴出口
                mobtime = 2700;
                monsterid = 8220008;
                msg = "出现了一个奇怪的商店。";
                pos1 = new Point(-163, 82);
                pos2 = new Point(958, 107);
                pos3 = new Point(706, -206);
                break;
            case 211040101: // 神秘岛 - 雪人谷
                mobtime = 3600;
                monsterid = 8220001;
                msg = "驮狼雪人出现了。";
                pos1 = new Point(485, 244);
                pos2 = new Point(-60, 249);
                pos3 = new Point(208, 255);
                break;
            case 209000000: // 隐藏地图 - 幸福村
                mobtime = 300;
                monsterid = 9500317;
                msg = "小雪人出现了。";
                pos1 = new Point(-115, 154);
                pos2 = new Point(-115, 154);
                pos3 = new Point(-115, 154);
                break;
            case 677000001: // 迷你地图 - 牛魔王藏身之地
                mobtime = 60;
                monsterid = 9400612;
                msg = "牛魔王出现了。";
                pos1 = new Point(99, 60);
                pos2 = new Point(99, 60);
                pos3 = new Point(99, 60);
                break;
            case 677000003: // 迷你地图 - 黑暗独角兽藏身之地
                mobtime = 60;
                monsterid = 9400610;
                msg = "黑暗独角兽出现了。";
                pos1 = new Point(6, 35);
                pos2 = new Point(6, 35);
                pos3 = new Point(6, 35);
                break;
            case 677000005: // 迷你地图 - 印第安老斑鸠藏身之地
                mobtime = 60;
                monsterid = 9400609;
                msg = "印第安老斑鸠出现了。";
                pos1 = new Point(-277, 78);
                pos2 = new Point(547, 86);
                pos3 = new Point(-347, 80);
                break;
            case 677000007: // 迷你地图 - 雪之猫女藏身之地
                mobtime = 60;
                monsterid = 9400611;
                msg = "雪之猫女出现了。";
                pos1 = new Point(117, 73);
                pos2 = new Point(117, 73);
                pos3 = new Point(117, 73);
                break;
            case 677000009: // 迷你地图 - 沃勒福藏身之地
                mobtime = 60;
                monsterid = 9400613;
                msg = "沃勒福出现了。";
                pos1 = new Point(85, 66);
                pos2 = new Point(85, 66);
                pos3 = new Point(85, 66);
                break;
            case 931000500: // 秘密地图 - 美洲豹栖息地
            case 931000502: // 秘密地图 - 美洲豹栖息地
                mobtime = 3600; //2小时刷新
                monsterid = 9304005;
                msg = "美洲豹栖息地出现 剑齿豹 ，喜欢此坐骑的弩豹游侠职业可以前往抓捕。";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                sendWorldMsg = true;
                break;
            case 931000501: // 秘密地图 - 美洲豹栖息地
            case 931000503: // 秘密地图 - 美洲豹栖息地
                mobtime = 2 * 3600; //2小时刷新
                monsterid = 9304006;
                msg = "美洲豹栖息地出现 雪豹 ，喜欢此坐骑的弩豹游侠职业可以前往抓捕。";
                pos1 = new Point(-872, -332);
                pos2 = new Point(409, -572);
                pos3 = new Point(-131, 0);
                shouldSpawn = false;
                sendWorldMsg = true;
                break;
        }
        if (monsterid > 0) {
            map.addAreaMonsterSpawn(MapleLifeFactory.getMonster(monsterid), pos1, pos2, pos3, mobtime, msg, shouldSpawn, sendWorldMsg);
        }
    }

    private void loadPortals(MapleMap map, MapleData port) {
        if (port == null) {
            return;
        }
        int nextDoorPortal = 0x80;
        for (MapleData portal : port.getChildren()) {
            MaplePortal myPortal = new MaplePortal(MapleDataTool.getInt(portal.getChildByPath("pt")));
            myPortal.setName(MapleDataTool.getString(portal.getChildByPath("pn")));
            myPortal.setTarget(MapleDataTool.getString(portal.getChildByPath("tn")));
            myPortal.setTargetMapId(MapleDataTool.getInt(portal.getChildByPath("tm")));
            myPortal.setPosition(new Point(MapleDataTool.getInt(portal.getChildByPath("x")), MapleDataTool.getInt(portal.getChildByPath("y"))));
            String script = MapleDataTool.getString("script", portal, null);
            if (script != null && script.equals("")) {
                script = null;
            }
            myPortal.setScriptName(script);

            if (myPortal.getType() == MaplePortal.DOOR_PORTAL) {
                myPortal.setId(nextDoorPortal);
                nextDoorPortal++;
            } else {
                myPortal.setId(Integer.parseInt(portal.getName()));
            }
            map.addPortal(myPortal);
        }
    }

    private MapleNodes loadNodes(int mapid, MapleData mapData) {
        MapleNodes nodeInfo = new MapleNodes(mapid);
        if (mapData.getChildByPath("nodeInfo") != null) {
            for (MapleData node : mapData.getChildByPath("nodeInfo")) {
                try {
                    if (node.getName().equals("start")) {
                        nodeInfo.setNodeStart(MapleDataTool.getInt(node, 0));
                        continue;
                    }
                    List<Integer> edges = new ArrayList<>();
                    if (node.getChildByPath("edge") != null) {
                        for (MapleData edge : node.getChildByPath("edge")) {
                            edges.add(MapleDataTool.getInt(edge, -1));
                        }
                    }
                    MapleNodeInfo mni = new MapleNodeInfo(
                            Integer.parseInt(node.getName()),
                            MapleDataTool.getIntConvert("key", node, 0),
                            MapleDataTool.getIntConvert("x", node, 0),
                            MapleDataTool.getIntConvert("y", node, 0),
                            MapleDataTool.getIntConvert("attr", node, 0), edges);
                    nodeInfo.addNode(mni);
                } catch (NumberFormatException ignored) {
                } //start, end, edgeInfo = we dont need it
            }
            nodeInfo.sortNodes();
        }
        for (int i = 1; i <= 7; i++) {
            if (mapData.getChildByPath(String.valueOf(i)) != null && mapData.getChildByPath(i + "/obj") != null) {
                for (MapleData node : mapData.getChildByPath(i + "/obj")) {
                    if (node.getChildByPath("SN_count") != null && node.getChildByPath("speed") != null) {
                        int sn_count = MapleDataTool.getIntConvert("SN_count", node, 0);
                        String name = MapleDataTool.getString("name", node, "");
                        int speed = MapleDataTool.getIntConvert("speed", node, 0);
                        if (sn_count <= 0 || speed <= 0 || name.equals("")) {
                            continue;
                        }
                        List<Integer> SN = new ArrayList<>();
                        for (int x = 0; x < sn_count; x++) {
                            SN.add(MapleDataTool.getIntConvert("SN" + x, node, 0));
                        }
                        MaplePlatform mni = new MaplePlatform(
                                name, MapleDataTool.getIntConvert("start", node, 2), speed,
                                MapleDataTool.getIntConvert("x1", node, 0),
                                MapleDataTool.getIntConvert("y1", node, 0),
                                MapleDataTool.getIntConvert("x2", node, 0),
                                MapleDataTool.getIntConvert("y2", node, 0),
                                MapleDataTool.getIntConvert("r", node, 0), SN);
                        nodeInfo.addPlatform(mni);
                    } else if (node.getChildByPath("tags") != null) {
                        String name = MapleDataTool.getString("tags", node, "");
                        nodeInfo.addFlag(new Pair<>(name, name.endsWith("3") ? 1 : 0)); //idk, no indication in wz
                    }
                }
            }
        }
        // load areas (EG PQ platforms)
        if (mapData.getChildByPath("area") != null) {
            int x1, y1, x2, y2;
            Rectangle mapArea;
            for (MapleData area : mapData.getChildByPath("area")) {
                x1 = MapleDataTool.getInt(area.getChildByPath("x1"));
                y1 = MapleDataTool.getInt(area.getChildByPath("y1"));
                x2 = MapleDataTool.getInt(area.getChildByPath("x2"));
                y2 = MapleDataTool.getInt(area.getChildByPath("y2"));
                mapArea = new Rectangle(x1, y1, (x2 - x1), (y2 - y1));
                nodeInfo.addMapleArea(mapArea);
            }
        }
        if (mapData.getChildByPath("CaptureTheFlag") != null) {
            MapleData mc = mapData.getChildByPath("CaptureTheFlag");
            for (MapleData area : mc) {
                nodeInfo.addGuardianSpawn(new Point(MapleDataTool.getInt(area.getChildByPath("FlagPositionX")), MapleDataTool.getInt(area.getChildByPath("FlagPositionY"))), area.getName().startsWith("Red") ? 0 : 1);
            }
        }
        if (mapData.getChildByPath("directionInfo") != null) {
            MapleData mc = mapData.getChildByPath("directionInfo");
            for (MapleData area : mc) {
                DirectionInfo di = new DirectionInfo(Integer.parseInt(area.getName()), MapleDataTool.getInt("x", area, 0), MapleDataTool.getInt("y", area, 0), MapleDataTool.getInt("forcedInput", area, 0) > 0);
                if (area.getChildByPath("EventQ") != null) {
                    for (MapleData event : area.getChildByPath("EventQ")) {
                        di.eventQ.add(MapleDataTool.getString(event));
                    }
                } else {
                    System.out.println("[loadNodes] 地图: " + mapid + " 没有找到EventQ.");
                }
                nodeInfo.addDirection(Integer.parseInt(area.getName()), di);
            }
        }
        if (mapData.getChildByPath("monsterCarnival") != null) {
            MapleData mc = mapData.getChildByPath("monsterCarnival");
            if (mc.getChildByPath("mobGenPos") != null) {
                for (MapleData area : mc.getChildByPath("mobGenPos")) {
                    nodeInfo.addMonsterPoint(MapleDataTool.getInt(area.getChildByPath("x")),
                            MapleDataTool.getInt(area.getChildByPath("y")),
                            MapleDataTool.getInt(area.getChildByPath("fh")),
                            MapleDataTool.getInt(area.getChildByPath("cy")),
                            MapleDataTool.getInt("team", area, -1));
                }
            }
            if (mc.getChildByPath("mob") != null) {
                for (MapleData area : mc.getChildByPath("mob")) {
                    nodeInfo.addMobSpawn(MapleDataTool.getInt(area.getChildByPath("id")), MapleDataTool.getInt(area.getChildByPath("spendCP")));
                }
            }
            if (mc.getChildByPath("guardianGenPos") != null) {
                for (MapleData area : mc.getChildByPath("guardianGenPos")) {
                    nodeInfo.addGuardianSpawn(new Point(MapleDataTool.getInt(area.getChildByPath("x")), MapleDataTool.getInt(area.getChildByPath("y"))), MapleDataTool.getInt("team", area, -1));
                }
            }
            if (mc.getChildByPath("skill") != null) {
                for (MapleData area : mc.getChildByPath("skill")) {
                    nodeInfo.addSkillId(MapleDataTool.getInt(area));
                }
            }
        }
        return nodeInfo;
    }
}
