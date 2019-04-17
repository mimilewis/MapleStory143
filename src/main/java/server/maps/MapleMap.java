package server.maps;

import client.*;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.skills.KSPsychicSkillEntry;
import client.skills.Skill;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.opcode.EffectOpcode;
import handling.world.PartyOperation;
import handling.world.WorldBroadcastService;
import handling.world.party.ExpeditionType;
import scripting.event.EventInstanceManager;
import scripting.event.EventManager;
import server.*;
import server.Timer.EtcTimer;
import server.Timer.MapTimer;
import server.life.*;
import server.maps.MapleNodes.DirectionInfo;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import server.quest.MapleQuest;
import server.squad.MapleSquad;
import server.squad.MapleSquadType;
import tools.*;
import tools.packet.*;

import java.awt.*;
import java.sql.PreparedStatement;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public final class MapleMap {

    /*
     * Holds mappings of OID -> MapleMapObject separated by MapleMapObjectType.
     * Please acquire the appropriate lock when reading and writing to the
     * LinkedHashMaps. The MapObjectType Maps themselves do not need to
     * synchronized in any way since they should never be modified.
     */
    private final Map<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> mapobjects;
    private final Map<MapleMapObjectType, ReentrantReadWriteLock> mapobjectlocks;
    private final List<MapleCharacter> characters = new ArrayList<>();
    private final ReentrantReadWriteLock charactersLock = new ReentrantReadWriteLock();
    private final Lock runningOidLock = new ReentrantLock();
    private final List<MapleHideNpc> hideNpc = new ArrayList<>();
    private final List<Spawns> monsterSpawn = new ArrayList<>();
    private final AtomicInteger runningOid = new AtomicInteger(500000);
    private final AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private final AtomicInteger spawnedForcesOnMap = new AtomicInteger(0);
    private final AtomicInteger spawnedMistOnMap = new AtomicInteger(0);
    private final Map<Integer, MaplePortal> portals = new HashMap<>();
    private final Map<Integer, Map<Integer, List<Pair<Integer, Integer>>>> kspsychicObjects = new LinkedHashMap<>();
    private final ReentrantReadWriteLock kspsychicLock = new ReentrantReadWriteLock();
    private final Map<Integer, Integer> ksultimates = new LinkedHashMap<>();
    private final float monsterRate;
    private final int channel;
    private final int mapid;
    private final List<Integer> dced = new ArrayList<>();
    private MapleFootholdTree footholds = null;
    private float recoveryRate;
    private MapleMapEffect mapEffect;
    private short decHP = 0, createMobInterval = 9000, top = 0, bottom = 0, left = 0, right = 0;
    private int consumeItemCoolTime = 0;
    private int protectItem = 0;
    private int decHPInterval = 10000;
    private int returnMapId;
    private int timeLimit;
    private int fieldLimit;
    private int maxRegularSpawn = 0;
    private int fixedMob;
    private int forcedReturnMap = 999999999;
    private int instanceid = -1;
    private int lvForceMove = 0;
    private int lvLimit = 0;
    private int permanentWeather = 0;
    private int partyBonusRate = 0;
    private boolean town, clock, personalShop, miniMapOnOff, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true, checkStates = true;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader = "";
    private List<Point> spawnPoints = new ArrayList<>();
    private ScheduledFuture<?> squadSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0;
    private MapleNodes nodes;
    private MapleSquadType squad;
    private Map<Integer, MapleSwordNode> swordNodes;
    private long spawnRuneTime = 0;
    private int fieldType;
    private boolean entrustedFishing;

    public MapleMap(int mapid, int channel, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.channel = channel;
        this.returnMapId = returnMapId;
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
        if (GameConstants.getPartyPlay(mapid) > 0) {
            this.monsterRate = (monsterRate - 1.0f) * 2.5f + 1.0f;
        } else {
            this.monsterRate = monsterRate;
        }
        EnumMap<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> objsMap = new EnumMap<>(MapleMapObjectType.class);
        EnumMap<MapleMapObjectType, ReentrantReadWriteLock> objlockmap = new EnumMap<>(MapleMapObjectType.class);
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            objsMap.put(type, new LinkedHashMap<>());
            objlockmap.put(type, new ReentrantReadWriteLock());
        }
        mapobjects = Collections.unmodifiableMap(objsMap);
        mapobjectlocks = Collections.unmodifiableMap(objlockmap);
    }

    public int getFieldType() {
        return fieldType;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public boolean getSpawns() {
        return isSpawns;
    }

    public void setSpawns(boolean fm) {
        this.isSpawns = fm;
    }

    public void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public int getForceMove() {
        return lvForceMove;
    }

    public void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public int getLevelLimit() {
        return lvLimit;
    }

    public void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public void setSoaring(boolean b) {
        this.soaring = b;
    }

    public boolean canSoar() {
        return soaring;
    }

    public void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public void setDrops(boolean b) {
        this.dropsDisabled = b;
    }

    public void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public int getId() {
        return mapid;
    }

    public MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(int map) {
        this.forcedReturnMap = map;
    }

    public float getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public void setFieldLimit(int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public void setCreateMobInterval(short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getMapName() {
        return mapName;
    }

    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getFirstUserEnter() {
        return onFirstUserEnter;
    }

    public void setFirstUserEnter(String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public String getUserEnter() {
        return onUserEnter;
    }

    public void setUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setClock(boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean isTown() {
        return town;
    }

    public void setTown(boolean town) {
        this.town = town;
    }

    public boolean allowPersonalShop() {
        return personalShop;
    }

    public void setPersonalShop(boolean personalShop) {
        this.personalShop = personalShop;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public void setEverlast(boolean everlast) {
        this.everlast = everlast;
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(int delta) {
        if (delta > 0 || mapid == 749040100) { //隐藏地图 - 纯净雪人栖息地
            lastHurtTime = System.currentTimeMillis();
        }
        decHP = (short) delta;
    }

    public int getHPDecInterval() {
        return decHPInterval;
    }

    public void setHPDecInterval(int delta) {
        decHPInterval = delta;
    }

    public int getHPDecProtect() {
        return protectItem;
    }

    public void setHPDecProtect(int delta) {
        this.protectItem = delta;
    }

    public void addHideNpc(MapleHideNpc qm) {
        hideNpc.add(qm);
    }

    public boolean isMiniMapOnOff() {
        return miniMapOnOff;
    }

    public void setMiniMapOnOff(boolean on) {
        this.miniMapOnOff = on;
    }

    public List<Point> getSpawnPoints() {
        return spawnPoints;
    }

    public void setSpawnPoints(List<Point> Points) {
        this.spawnPoints = Points;
    }

    public List<MapleMapObject> getCharactersAsMapObjects() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.PLAYER));
    }

    public int getCurrentPartyId() {
        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr.getParty() != null) {
                    return chr.getParty().getPartyId();
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return -1;
    }

    /**
     * 添加对象到地图
     *
     * @param mapobject 地图对象
     */
    public void addMapObject(MapleMapObject mapobject) {
        runningOidLock.lock();
        try {
            if (mapobject.getObjectId() != 0) {
                mapobject.setObjectId(mapobject.getObjectId());
            } else {
                mapobject.setObjectId(runningOid.getAndIncrement());
            }
        } finally {
            runningOidLock.unlock();
        }
        mapobjectlocks.get(mapobject.getType()).writeLock().lock();
        try {
            mapobjects.get(mapobject.getType()).put(mapobject.getObjectId(), mapobject);
        } finally {
            mapobjectlocks.get(mapobject.getType()).writeLock().unlock();
        }
    }

    /**
     * @召唤并添加对象到当前范围地图
     */
    private void spawnAndAddRangedMapObject(MapleMapObject mapobject, DelayedPacketCreation packetbakery) {
        addMapObject(mapobject);

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> itr = characters.iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if ((mapobject.getType() == MapleMapObjectType.MIST || chr.getTruePosition().distanceSq(mapobject.getTruePosition()) <= GameConstants.maxViewRangeSq())) {
                    packetbakery.sendPackets(chr.getClient());
                    chr.addVisibleMapObject(mapobject);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    /**
     * 移除地图对象
     *
     * @param obj 地图对象
     */
    public void removeMapObject(MapleMapObject obj) {
        mapobjectlocks.get(obj.getType()).writeLock().lock();
        try {
            mapobjects.get(obj.getType()).remove(obj.getObjectId());
        } finally {
            mapobjectlocks.get(obj.getType()).writeLock().unlock();
        }
    }

    public Point calcPointBelow(Point initial) {
        MapleFoothold fh = footholds.findBelow(initial, false);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            double s5 = Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2)));
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) s5;
            } else {
                dropY = fh.getY1() + (int) s5;
            }
        }
        return new Point(initial.x, dropY);
    }

    public Point calcDropPos(Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(MapleCharacter chr, MapleMonster mob, boolean instanced) {
        if (mob == null || chr == null || ChannelServer.getInstance(channel) == null || dropsDisabled || mob.dropsDisabled() || chr.getPyramidSubway() != null || ServerConfig.WORLD_BANDROPITEM) { //no drops in pyramid ok? no cash either
            return;
        }
        //当地图道具数量达到 300 自动清理掉落在地图上的道具
        int maxSize = 200;
        if (!instanced && maxSize >= 300 && mapobjects.get(MapleMapObjectType.ITEM).size() >= maxSize) {
            removeDropsDelay();
            if (chr.isAdmin()) {
                chr.dropMessage(6, "[系统提示] 当前地图的道具数量达到 " + maxSize + " 系统已自动清理掉所有地上的物品信息.");
            }
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        int mobpos = mob.getTruePosition().x;
        int mesoServerRate = ChannelServer.getInstance(channel).getMesoRate(); //金币爆率
        int dropServerRate = ChannelServer.getInstance(channel).getDropRate(); //物品爆率
//        int cashServerRate = ChannelServer.getInstance(channel).getCashRate(); //点卷爆率
        int globalServerRate = ChannelServer.getInstance(channel).getDropgRate(); //特殊数据库道具爆率
        Item idrop;
        byte d = 1;
        Point pos = new Point(0, mob.getTruePosition().y);
        double 挑衅加成 = 100.0;
        MonsterStatusEffect mse = mob.getBuff(MonsterStatus.MOB_STAT_Showdown);
        if (mse != null) {
            挑衅加成 += mse.getX();
        }

        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();
        List<MonsterDropEntry> derp = mi.retrieveDrop(mob.getId());
        if (derp.isEmpty()) { //if no drops, no global drops either <3
            return;
        }
        List<MonsterDropEntry> dropEntry = new LinkedList<>(derp);
        Collections.shuffle(dropEntry);

        boolean mesoDropped = false;
        for (MonsterDropEntry de : dropEntry) {
            if (de.itemId == mob.getStolen()) {
                continue;
            }
            if (Randomizer.nextInt(999999) < (int) (de.chance * dropServerRate * chr.getDropMod() * (chr.getStat().getDropBuff() / 100.0) * (挑衅加成 / 100.0))) {
                if (mesoDropped && droptype != 3 && de.itemId == 0) { //not more than 1 sack of meso
                    continue;
                }
                if (de.itemId / 10000 == 238) { // 去掉怪物宝怪物卡 && !mob.getStats().isBoss() && chr.getMonsterBook().getLevelByCard(ii.getCardMobId(de.itemId)) >= 2
                    continue;
                }
                if (de.questid != 0 && chr.getQuestStatus(de.questid) <= 0) {
                    continue;
                }
                if (droptype == 3) {
                    pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                } else {
                    pos.x = (mobpos + ((d % 2 == 0) ? (20 * (d + 1) / 2) : -(20 * (d / 2))));
                }
                if (de.itemId == 0) { // meso
                    int mesos = Randomizer.nextInt(1 + Math.abs(de.maximum - de.minimum)) + de.minimum;
                    if (mesos > 0) {
                        spawnMobMesoDrop((int) (mesos * (chr.getStat().mesoBuff / 100.0) * chr.getDropMod() * mesoServerRate), calcDropPos(pos, mob.getTruePosition()), mob, chr, false, droptype);
                        mesoDropped = true;
                        d++;
                    }
                } else {
                    if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        int range = Math.abs(de.maximum - de.minimum);
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.minimum : 1), (byte) 0);
                    }
                    idrop.setGMLog("怪物掉落: " + mob.getId() + " 地图: " + mapid + " 时间: " + DateUtil.getCurrentDate());
                    if (ItemConstants.isNoticeItem(de.itemId)) {
                        broadcastMessage(MaplePacketCreator.serverNotice(6, "[掉宝提示] 玩家 " + chr.getName() + " 在 " + chr.getMap().getMapName() + " 杀死 " + mob.getStats().getName() + " 掉落道具 " + ii.getName(de.itemId)));
                    }
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, droptype, de.questid);
                    d++;
                }
            }
        }
        List<MonsterGlobalDropEntry> globalEntry = new ArrayList<>(mi.getGlobalDrop());
        Collections.shuffle(globalEntry);
        // Global Drops
        for (MonsterGlobalDropEntry de : globalEntry) {
            if (de.chance == 0) { //如果爆率为0 就直接跳过
                continue;
            }
            if (Randomizer.nextInt(999999) < de.chance * globalServerRate && (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                if (!gDropsDisabled) {
                    if (droptype == 3) {
                        pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                    } else {
                        pos.x = (mobpos + ((d % 2 == 0) ? (20 * (d + 1) / 2) : -(20 * (d / 2))));
                    }
                    if (ItemConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1), (byte) 0);
                    }
                    idrop.setGMLog("怪物掉落: " + mob.getId() + " 地图: " + mapid + " (Global) 时间: " + DateUtil.getCurrentDate());
                    if (ItemConstants.isNoticeItem(de.itemId)) {
                        broadcastMessage(MaplePacketCreator.serverNotice(6, "[掉宝提示] 玩家 " + chr.getName() + " 在 " + chr.getMap().getMapName() + " 杀死 " + mob.getStats().getName() + " 掉落道具 " + ii.getName(de.itemId)));
                    }
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getTruePosition()), mob, chr, de.onlySelf ? 0 : droptype, de.questid);
                    d++;
                }
            }
        }
    }

    public void removeMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
        removeMapObject(monster);
        monster.killed();
    }

    public void killMonster(MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        if (monster.getLinkCID() <= 0) {
            monster.spawnRevives(this);
        }
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD()));
        removeMapObject(monster);
        monster.killed();
    }

    public void killMonster(MapleMonster monster, MapleCharacter chr, boolean withDrops, boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public void killMonster(final MapleMonster monster, final MapleCharacter chr, final boolean withDrops, final boolean second, byte animation, final int lastSkill) {
        /*
         * 8810122 - 进阶暗黑龙王
         * 8810018 - 暗黑龙王的灵魂
         */
        if ((monster.getId() == 8810122 || monster.getId() == 8810018) && !second) {
            MapTimer.getInstance().schedule(() -> {
                killMonster(monster, chr, true, true, (byte) 1);
                killAllMonsters(true);
            }, 3000);
            return;
        }
        if (monster.getId() == 8820014) { //时间的宠儿－品克缤 pb sponge, kills pb(w) first before dying
            killMonster(8820000); //时间的宠儿－品克缤
        } else if (monster.getId() == 8820212) { //混沌品克缤
            killMonster(8820100); //混沌品克缤
        } else if (monster.getId() == 9300166) { //炸弹 ariant pq bomb
            animation = 2; //or is it 3?
        } else if (monster.getId() == 9101083 || monster.getId() == 8880000 || monster.getId() == 8880002) {  //麦格纳斯
            if (chr.getQuestStatus(1463) == 1) {
                chr.dropMessage(-1, "由于麦格纳斯死亡时施放出的能量，不再受到古瓦洛的力量的影响。");
                MapleQuestStatus quest = chr.getQuest(MapleQuest.getInstance(1463));
                quest.setCustomData("001");
                chr.updateQuest(quest);
            }
        }
        spawnedMonstersOnMap.decrementAndGet();
        removeMapObject(monster);
        monster.killed();
        MapleSquad sqd = getSquadByMap();
        boolean instanced = sqd != null || monster.getEventInstance() != null || getEMByMap() != null;
        int dropOwner = monster.killBy(chr, lastSkill);
        if (animation >= 0) {
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));
        }
        if (monster.getBuffToGive() > -1) {
            int buffid = monster.getBuffToGive();
            MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter mc : characters) {
                    if (mc.isAlive()) {
                        buff.applyTo(mc);
                        switch (monster.getId()) {
                            case 8810018: //暗黑龙王的灵魂      buffid = 2022108
                            case 8810122: //进阶暗黑龙王        buffid = 2022108
                            case 8820001: //时间的宠儿－品克缤  buffid = 2022449
                            case 8820212: //混沌品克缤          buffid = 2022449
                                mc.getClient().announce(EffectPacket.showOwnBuffEffect(buffid, EffectOpcode.UserEffect_JobChanged.getValue(), mc.getLevel(), 1)); // HT nine spirit
                                broadcastMessage(mc, EffectPacket.showBuffeffect(mc, buffid, EffectOpcode.UserEffect_JobChanged.getValue(), mc.getLevel(), 1), false); // HT nine spirit
                                break;
                        }
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
        int mobid = monster.getId();
        ExpeditionType type = null;
        if (mobid == 8810018 && mapid == 240060200) { // 暗黑龙王
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "经过无数次的挑战，终于击破了暗黑龙王的远征队！你们才是龙之林的真正英雄~"));
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(16);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Horntail;
            }
            doShrine(true);
        } else if (mobid == 8810122 && mapid == 240060201) { // 进阶暗黑龙王
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "经过无数次的挑战，终于击破了进阶暗黑龙王的远征队！你们才是龙之林的真正英雄~"));
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(24);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.ChaosHT;
            }
            doShrine(true);
        } else if (mobid == 9400266 && mapid == 802000111) { //9400266 - 努克斯
            doShrine(true);
        } else if (mobid == 9400265 && mapid == 802000211) { //9400265 - 贝尔加莫特
            doShrine(true);
        } else if (mobid == 9400270 && mapid == 802000411) { //9400270 - 都纳斯
            doShrine(true);
        } else if (mobid == 9400273 && mapid == 802000611) { //9400273 - 尼贝隆
            doShrine(true);
        } else if (mobid == 9400294 && mapid == 802000711) { //9400294 - 都纳斯
            doShrine(true);
        } else if (mobid == 9400296 && mapid == 802000803) { //9400296 - 布雷兹首脑
            doShrine(true);
        } else if (mobid == 9400289 && mapid == 802000821) { //9400289 - 欧碧拉
            doShrine(true);
        } else if (mobid == 8830000 && mapid == 105100300) { //8830000 - 蝙蝠怪
            if (speedRunStart > 0) {
                type = ExpeditionType.Normal_Balrog;
            }
        } else if ((mobid == 9420544 || mobid == 9420549) && mapid == 551030200 && monster.getEventInstance() != null && monster.getEventInstance().getName().contains(getEMByMap().getName())) {
            doShrine(getAllReactor().isEmpty());
        } else if (mobid == 8820001 && mapid == 270050100) { //时间的宠儿－品克缤
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "凭借永不疲倦的热情打败品克缤的远征队啊！你们是真正的时间的胜者！"));
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(17);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Pink_Bean;
            }
            doShrine(true);
        } else if (mobid == 8820212 && mapid == 270051100) { //混沌品克缤
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "凭借永不疲倦的热情打败混沌品克缤的远征队啊！你们是真正的时间的胜者！"));
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(59);
                    c.finishActivity(120106);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Pink_Bean;
            }
            doShrine(true);
        } else if ((mobid == 8850011 && mapid == 271040200) || (mobid == 8850012 && mapid == 271040100)) { //希纳斯 274040200
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, "被黑魔法师黑化的希纳斯女皇终于被永不言败的远征队打倒! 混沌世界得以净化!"));
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(39);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Cygnus;
            }
            doShrine(true);
        } else if (mobid == 8840000 && mapid == 211070100) { //班·雷昂
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(38);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Von_Leon;
            }
            doShrine(true);
        } else if (mobid == 8800002 && (mapid == 280030000 || mapid == 280030100)) { //扎昆
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(15);
                    c.finishActivity(120105);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            //FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Zakum;
            }
            doShrine(true);
        } else if (mobid == 8800102 && mapid == 280030001) { //进阶扎昆
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(23);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            //FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = ExpeditionType.Chaos_Zakum;
            }
            doShrine(true);
        } else if (mobid == 8870000 && mapid == 262031300) { //希拉 120级 简单模式
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(55);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Hillah;
            }
            doShrine(true);
        } else if (mobid == 8870100 && mapid == 262031300) { //希拉 170级 困难模式
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(56);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Hillah;
            }
            doShrine(true);
        } else if (mobid == 8860000 && mapid == 272030400) { //阿卡伊勒
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter c : characters) {
                    c.finishAchievement(58);
                }
            } finally {
                charactersLock.readLock().unlock();
            }
            if (speedRunStart > 0) {
                type = ExpeditionType.Akyrum;
            }
            doShrine(true);
        } else if (mobid >= 8800003 && mobid <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();
            for (MapleMonster mons : monsters) {
                if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (MapleMapObject object : monsters) {
                    MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 8800000) {
                        Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800103 && mobid <= 8800110) {
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();
            for (MapleMonster mons : monsters) {
                if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (MapleMonster mons : monsters) {
                    if (mons.getId() == 8800100) { //进阶扎昆
                        Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800100), pos); //进阶扎昆
                        break;
                    }
                }
            }
        } else if (mobid >= 8800023 && mobid <= 8800030) {
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();
            for (MapleMonster mons : monsters) {
                if (mons.getId() >= 8800023 && mons.getId() <= 8800030) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (MapleMonster mons : monsters) {
                    if (mons.getId() == 8800020) { //进阶扎昆
                        Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800022), pos); //简单扎昆
                        break;
                    }
                }
            }
        } else if (mobid >= 9400903 && mobid <= 9400910) { //粉色扎昆
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();
            for (MapleMonster mons : monsters) {
                if (mons.getId() >= 9400903 && mons.getId() <= 9400910) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (MapleMonster mons : monsters) {
                    if (mons.getId() == 9400900) { //粉色扎昆
                        Point pos = mons.getTruePosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9400900), pos); //粉色扎昆
                        break;
                    }
                }
            }
        } else if (mobid == 8820008) { // 宝宝BOSS召唤用透明怪物 - 品克缤
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                    //FileoutputUtil.log(FileoutputUtil.Pinkbean_Log, "地图杀死怪物 8820008 : " + mons.getId() + " - " + mons.getStats().getName(), true);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) { // 时间的宠儿－品克缤
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getId() != 8820001 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                    //FileoutputUtil.log(FileoutputUtil.Pinkbean_Log, "地图杀死怪物 品克缤 : " + mons.getId() + " - " + mons.getStats().getName(), true);
                }
            }
        } else if (mobid == 8820108) { // 8820108 - 宝宝BOSS召唤用透明怪物 - 混沌品克缤
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                    //FileoutputUtil.log(FileoutputUtil.Pinkbean_Log, "地图杀死怪物 8820108 : " + mons.getId() + " - " + mons.getStats().getName(), true);
                }
            }
        } else if (mobid >= 8820300 && mobid <= 8820304) { // 混沌品克缤
            for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820100 && mons.getId() != 8820212 && mons.getObjectId() != monster.getObjectId() && mons.isAlive() && mons.getLinkOid() == monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                    //FileoutputUtil.log(FileoutputUtil.Pinkbean_Log, "地图杀死怪物 混沌品克缤 : " + mons.getId() + " - " + mons.getStats().getName(), true);
                }
            }
        } else if (mobid / 100000 == 98 && chr.getMapId() / 10000000 == 95 && getAllMonstersThreadsafe().isEmpty()) {
            switch ((chr.getMapId() % 1000) / 100) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    chr.send(UIPacket.showMapEffect("monsterPark/clear"));
                    break;
                case 5:
                    if (chr.getMapId() / 1000000 == 952) {
                        chr.send(UIPacket.showMapEffect("monsterPark/clearF"));
                    } else {
                        chr.send(UIPacket.showMapEffect("monsterPark/clear"));
                    }
                    break;
                case 6:
                    chr.send(UIPacket.showMapEffect("monsterPark/clearF"));
                    break;
            }
        } else if (mobid / 100000 == 93 && chr.getMapId() / 1000000 == 955 && getAllMonstersThreadsafe().isEmpty()) {
            switch ((chr.getMapId() % 1000) / 100) {
                case 1:
                case 2:
                    chr.send(MaplePacketCreator.showEffect("aswan/clear"));
                    chr.send(MaplePacketCreator.playSound("Party1/Clear"));
                    break;
                case 3:
                    chr.send(MaplePacketCreator.showEffect("aswan/clearF"));
                    chr.send(MaplePacketCreator.playSound("Party1/Clear"));
                    chr.dropMessage(-1, "你已经通过了所有回合。请通过传送口移动到外部。");
                    break;
            }
        }
        eventMobkillCheck(mobid, chr);
        if (type != null) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                String name = "";
                switch (type.name()) {
                    case "Normal_Balrog":
                        name = "蝙蝠怪";
                        break;
                    case "Zakum":
                        name = "扎昆";
                        break;
                    case "Horntail":
                        name = "暗黑龙王";
                        break;
                    case "Pink_Bean":
                        name = "时间的宠儿－品克缤";
                        break;
                    case "Chaos_Pink_Bean":
                        name = "混沌品克缤";
                        break;
                    case "Chaos_Zakum":
                        name = "进阶扎昆";
                        break;
                    case "ChaosHT":
                        name = "进阶暗黑龙王";
                        break;
                    case "Von_Leon":
                        name = "班·雷昂";
                        break;
                    case "Cygnus":
                        name = "希纳斯女皇";
                        break;
                    case "Akyrum":
                        name = "阿卡伊勒";
                        break;
                    case "Hillah":
                        name = "希拉";
                        break;
                }
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                broadcastMessage(MaplePacketCreator.serverNotice(5, speedRunLeader + "带领的远征队，耗时: " + time + " 击败了 " + name + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart), (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }
        }
        if (monster.getStats().isBoss()) {
            chr.finishActivity(120107);
        }
        if (withDrops) {
            MapleCharacter drop;
            if (dropOwner <= 0) {
                drop = chr;
            } else {
                drop = getCharacterById(dropOwner);
                if (drop == null) {
                    drop = chr;
                }
            }
            dropFromMonster(drop, monster, instanced);
        }
    }

    public void eventMobkillCheck(int n2, MapleCharacter player) {
        if (player == null) {
            return;
        }
//        if (player.getSailEvent() != null) {
//            player.getSailEvent().at(player);
//        }
        EventInstanceManager eim = player.getEventInstance();
        if (n2 / 100000 == 98 && player.getMapId() / 10000000 == 95 && this.getAllMonstersThreadsafe().isEmpty()) {
            switch (player.getMapId() % 1000 / 100) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4: {
                    player.send_other(MaplePacketCreator.showEffect("monsterPark/clear"), true);
                    break;
                }
                case 5: {
                    if (player.getMapId() / 1000000 == 952) {
                        player.send_other(MaplePacketCreator.showEffect("monsterPark/clearF"), true);
                        break;
                    }
                    player.send_other(MaplePacketCreator.showEffect("monsterPark/clear"), true);
                    break;
                }
                case 6: {
                    player.send_other(MaplePacketCreator.showEffect("monsterPark/clearF"), true);
                }
            }
        } else if (n2 / 100000 == 93 && player.getMapId() / 1000000 == 955 && this.getAllMonstersThreadsafe().isEmpty()) {
            switch (player.getMapId() % 1000 / 100) {
                case 1:
                case 2: {
                    player.send_other(MaplePacketCreator.showEffect("aswan/clear"), true);
                    player.send_other(MaplePacketCreator.playSound("Party1/Clear"), true);
                    break;
                }
                case 3: {
                    player.send_other(MaplePacketCreator.showEffect("aswan/clearF"), true);
                    player.send_other(MaplePacketCreator.playSound("Party1/Clear"), true);
                    player.dropMessage(-1, "你已经通过了所有回合。请通过传送口移动到外部。");
                }
            }
        } else if (n2 / 100000 == 93 && player.getMapId() / 10000 == 86301 && this.getAllMonster().isEmpty()) {
            if (eim != null) {
                eim.setProperty(String.valueOf(this.getId()), "2");
                player.showPortal("clear", "1");
                eim.broadcastPacket(MaplePacketCreator.sendGhostPoint(String.valueOf(this.getId()), "2"));
                player.send_other(MaplePacketCreator.showEffect("aswan/clear"), true);
                player.send_other(MaplePacketCreator.playSound("Party1/Clear"), true);
            }
        } else if (n2 / 100000 == 93 && (player.getMapId() == 921160200 || player.getMapId() == 921160400) && this.getAllMonster().isEmpty()) {
            this.startMapEffect("请快点移动到下一张地图。", 5120053);
        } else if (n2 / 100000 == 93 && player.getMapId() / 10000 == 24008 && this.getAllMonster().isEmpty() && eim != null) {
            player.send_other(MaplePacketCreator.showEffect("quest/party/clear"), true);
            player.send_other(MaplePacketCreator.playSound("Party1/Clear"), true);
        }
        boolean bl2 = false;
        switch (player.getMapId()) {
            case 811000100: {
                if (eim == null || !this.getAllMonster().isEmpty()) break;
                if (eim.getProperty("stage1").equals("1")) {
                    bl2 = true;
                    eim.setProperty("stage1", "clear");
                    break;
                }
                eim.schedule("stage1Check", 100);
                break;
            }
            case 811000200: {
                if (eim == null || !this.getAllMonster().isEmpty()) break;
                if (eim.getProperty("stage2").equals("5")) {
                    bl2 = true;
                    eim.setProperty("stage2", "clear");
                    break;
                }
                eim.schedule("stage2Check", 100);
                break;
            }
            case 811000300: {
                if (eim == null || !this.getAllMonster().isEmpty()) break;
                if (eim.getProperty("stage3").equals("2") && n2 == 9450014) {
                    bl2 = true;
                    eim.setProperty("stage3", "clear");
                    break;
                }
                if (!eim.getProperty("stage3").equals("0")) break;
                eim.schedule("stage3Check", 100);
                break;
            }
            case 811000400: {
                if (eim == null || !this.getAllMonster().isEmpty()) break;
                if (eim.getProperty("stage4").equals("1")) {
                    bl2 = true;
                    eim.setProperty("stage4", "clear");
                    break;
                }
                eim.schedule("stage4Check", 100);
            }
        }
        if (bl2) {
            player.send_other(MaplePacketCreator.showEffect("aswan/clear"), true);
            player.send_other(MaplePacketCreator.playSound("Party1/Clear"), true);
            player.showPortal("clear2", "1");
            player.showPortal("clear1", "1");
            this.changeEnvironment("gate", 2);
        }
        if (n2 == 9390611 || n2 == 9390610) {
            player.showPortal("phase3", "1");
            player.showPortal("clear", "1");
        } else if (n2 == 9390600) {
            player.showPortal("phase2-1", "1");
            player.showPortal("phase2-2", "1");
        } else if (n2 == 9390601) {
            player.showPortal("phase3", "1");
        } else if (n2 == 9390612) {
            player.showPortal("clear2", "1");
            player.showPortal("clear1", "1");
        }
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret;
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.REACTOR).values().parallelStream().map(mmo -> (MapleReactor) mmo).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleRune> getAllRune() {
        return getAllRuneThreadsafe();
    }

    public List<MapleRune> getAllRuneThreadsafe() {
        ArrayList<MapleRune> ret;
        mapobjectlocks.get(MapleMapObjectType.RUNE).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.RUNE).values().parallelStream().map(mmo -> (MapleRune) mmo).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.RUNE).readLock().unlock();
        }
        return ret;
    }

    public List<MapleSummon> getAllSummonsThreadsafe() {
        ArrayList<MapleSummon> ret;
        mapobjectlocks.get(MapleMapObjectType.SUMMON).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.SUMMON).values().parallelStream().filter(mmo -> mmo instanceof MapleSummon).map(mmo -> (MapleSummon) mmo).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.SUMMON).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret;
        mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.DOOR).values().parallelStream().filter(mmo -> mmo instanceof MapleDoor).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllMechDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret;
        mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.DOOR).values().parallelStream().filter(mmo -> mmo instanceof MechDoor).collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().lock();
        try {
            ret.addAll(mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values());
        } finally {
            mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe(false);
    }
    public List<MapleMonster> getAllMonstersThreadsafe() {
        return getAllMonstersThreadsafe(true);
    }

    public List<MapleMonster> getAllMonstersThreadsafe(boolean filter) {
        final ArrayList<MapleMonster> ret;
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            ret = mapobjects.get(MapleMapObjectType.MONSTER).values().parallelStream()
                    .filter(mmo -> filter || !((MapleMonster) mmo).getStats().getName().contains("dummy") && !((MapleMonster) mmo).getStats().isFriendly())
                    .map(mmo -> (MapleMonster) mmo)
                    .collect(Collectors.toCollection(ArrayList::new));
//            ret = mapobjects.get(MapleMapObjectType.MONSTER).values().parallelStream()
//                    .filter(mmo -> !(filter || ((MapleMonster) mmo).getStats().getName().contains("dummy") || ((MapleMonster) mmo).getStats().isFriendly()) || filter)
//                    .map(mmo -> (MapleMonster) mmo)
//                    .collect(Collectors.toCollection(ArrayList::new));
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList<Integer> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                int theId = ((MapleMonster) mmo).getId();
                if (!ret.contains(theId)) {
                    ret.add(theId);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public void killAllMonsters(boolean animate) {
        for (MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
            monster.killed();
        }
    }

    public void killMonster(int monsId) {
        for (MapleMapObject mmo : getAllMonstersThreadsafe()) {
            if (((MapleMonster) mmo).getId() == monsId) {
                spawnedMonstersOnMap.decrementAndGet();
                removeMapObject(mmo);
                broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
                ((MapleMonster) mmo).killed();
                break;
            }
        }
    }

    public String MapDebug_Log() {
        StringBuilder sb = new StringBuilder("Defeat time : ");
        sb.append(DateUtil.getNowTime());
        sb.append(" | Mapid : ").append(this.mapid);
        charactersLock.readLock().lock();
        try {
            sb.append(" Users [").append(characters.size()).append("] | ");
            for (MapleCharacter mc : characters) {
                sb.append(mc.getName()).append(", ");
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return sb.toString();
    }

    public void limitReactor(int rid, int num) {
        List<MapleReactor> toDestroy = new ArrayList<>();
        Map<Integer, Integer> contained = new LinkedHashMap<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (contained.containsKey(mr.getReactorId())) {
                    if (contained.get(mr.getReactorId()) >= num) {
                        toDestroy.add(mr);
                    } else {
                        contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                    }
                } else {
                    contained.put(mr.getReactorId(), 1);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactors(int first, int last) {
        List<MapleReactor> toDestroy = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    toDestroy.add(mr);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactor(int oid) {
        final MapleReactor reactor = getReactorByOid(oid);
        if (reactor == null) {
            return;
        }
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(() -> respawnReactor(reactor), reactor.getDelay());
        }
    }

    public void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor reactor = (MapleReactor) obj;
                broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
                reactor.setAlive(false);
                reactor.setTimerActive(false);
                toSpawn.add(reactor);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            if (!r.isCustom()) { //guardians cpq
                respawnReactor(r);
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use -
     * not tested (broken reactors get removed from mapobjects when destroyed)
     * Should create instances for multiple copies of non-respawning reactors...
     */
    public void resetReactors() {
        setReactorState((byte) 0);
    }

    public void setReactorState() {
        setReactorState((byte) 1);
    }

    public void setReactorState(byte state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).forceHitReactor(state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public void setReactorDelay(int state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).setDelay(state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes
     * (such as ZPQ/LMPQ)
     */
    public void shuffleReactors() {
        shuffleReactors(0, 9999999); //all
    }

    public void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    points.add(mr.getPosition());
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        Collections.shuffle(points);
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    mr.setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * 更新怪物控制器，怪物自动从新的角色对象数据中寻找新的控制权
     *
     * @param monster 怪物
     */
    public void updateMonsterController(MapleMonster monster) {
        if (monster == null || !monster.isAlive() || monster.getLinkCID() > 0 || monster.getStats().isEscort()) {
            return;
        }
        if (monster.getController() != null) {
            if (monster.getController().getMap() != this || monster.getController().getTruePosition().distanceSq(monster.getTruePosition()) > monster.getRange()) {
                monster.getController().stopControllingMonster(monster);
            }
            return;
        }
        int mincontrolled = -1;
        MapleCharacter newController = null;

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isHidden() && (chr.getControlledSize() < mincontrolled || mincontrolled == -1)) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                }
            }
            if (newController != null) {
                monster.setLastMove();
                monster.switchController(newController, false);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
//        if (newController != null) {
//            if (monster.isFirstAttack()) {
//                newController.controlMonster(monster, true);
//                monster.setControllerHasAggro(true);
//                monster.setControllerKnowsAboutAggro(true);
//            } else {
//                newController.controlMonster(monster, false);
//            }
//        }
    }

    public MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        mapobjectlocks.get(type).readLock().lock();
        try {
            return mapobjects.get(type).get(oid);
        } finally {
            mapobjectlocks.get(type).readLock().unlock();
        }
    }

    public boolean containsNPC(int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.NPC).values()) {
                MapleNPC n = (MapleNPC) mapleMapObject;
                if (n.getId() == npcid) {
                    return true;
                }
            }
            return false;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleNPC getNPCById(int id) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.NPC).values()) {
                MapleNPC n = (MapleNPC) mapleMapObject;
                if (n.getId() == id) {
                    return n;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleMonster getMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            MapleMonster ret = null;
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                MapleMonster n = (MapleMonster) mapleMapObject;
                if (n.getId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public int countMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            int ret = 0;
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                MapleMonster n = (MapleMonster) mapleMapObject;
                if (n.getId() == id) {
                    ret++;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public MapleReactor getReactorById(int id) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            MapleReactor ret = null;
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor n = (MapleReactor) mapleMapObject;
                if (n.getReactorId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public MapleMonster getMonsterByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public MapleSummon getSummonByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.SUMMON);
        if (mmo == null) {
            return null;
        }
        return (MapleSummon) mmo;
    }

    public MapleNPC getNPCByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public MapleReactor getReactorByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public MonsterFamiliar getFamiliarByOid(int oid) {
        MapleMapObject mmo = this.getMapObject(oid, MapleMapObjectType.FAMILIAR);
        if (mmo == null) {
            return null;
        }
        return (MonsterFamiliar) mmo;
    }

    public MapleMist getMistByOid(int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MIST);
        if (mmo == null) {
            return null;
        }
        return (MapleMist) mmo;
    }

    public MapleReactor getReactorByName(String name) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = ((MapleReactor) obj);
                if (mr.getName().equalsIgnoreCase(name)) {
                    return mr;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public void spawnNpc(int id, Point pos) {
        MapleNPC npc = MapleLifeFactory.getNPC(id, this.mapid);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos, false).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(NPCPacket.spawnNPC(npc));
    }

    public void spawnNpcForPlayer(MapleClient c, int id, Point pos) {
        final MapleNPC npc = MapleLifeFactory.getNPC(id, this.mapid);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setOwnerid(c.getPlayer().getId());
        npc.setFh(getFootholds().findBelow(pos, false).getId());
        npc.setCustom(true);
        addMapObject(npc);
        c.announce(NPCPacket.spawnNPC(npc));
    }

    public void removeNpc(int npcid) {
        removeNpc(npcid, 0);
    }

    public void removeNpc(int npcid, int ownerid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (!npc.isCustom() || npcid != -1 && npc.getId() != npcid || npc.getId() != 0 && npc.getOwnerid() != ownerid)
                    continue;
                if (!npc.isHidden() && !GameConstants.isHideNpc(mapid, getId())) {
                    broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId(), false));
                }
                broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
                itr.remove();
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().unlock();
        }
    }

    public void hideNpc(int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.NPC).values()) {
                MapleNPC npc = (MapleNPC) mapleMapObject;
                if (npcid == -1 || npc.getId() == npcid) {
                    broadcastMessage(NPCPacket.removeNPCController(npc.getObjectId(), false));
                    broadcastMessage(NPCPacket.removeNPC(npc.getObjectId()));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public void hideNpc(MapleClient c, int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mapleMapObject : mapobjects.get(MapleMapObjectType.NPC).values()) {
                MapleNPC npc = (MapleNPC) mapleMapObject;
                if (npcid == -1 || npc.getId() == npcid) {
                    c.announce(NPCPacket.removeNPCController(npc.getObjectId(), false));
                    c.announce(NPCPacket.removeNPC(npc.getObjectId()));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public void spawnReactorOnGroundBelow(MapleReactor mob, Point pos) {
        mob.setPosition(pos); //reactors dont need FH lol
        mob.setCustom(true);
        spawnReactor(mob);
    }

    public void spawnMonster_sSack(MapleMonster mob, Point pos, int spawnType) {
        mob.setPosition(calcPointBelow(new Point(pos.x, pos.y - 1)));
        spawnMonster(mob, spawnType);
    }

    public void spawnMonster_Pokemon(MapleMonster mob, Point pos, int spawnType) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mob.setPosition(spos);
        spawnMonster(mob, spawnType, true);
    }

    public void spawnMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        spawnMonster_sSack(mob, pos, -2);
    }

    public int spawnMonsterWithEffectBelow(MapleMonster mob, Point pos, int effect) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos);
    }

    /**
     * 刷出扎昆
     */
    public void spawnZakum(int x, int y, long maxhp) {
        Point pos = new Point(x, y);
        MapleMonster mainb = MapleLifeFactory.getMonster(8800000);
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);
        mainb.getStats().setChange(true);
        mainb.changeLevel(120);
        mainb.getChangedStats().setOHp(maxhp);
        mainb.setHp(maxhp);
        spawnFakeMonster(mainb);
        // Might be possible to use the map object for reference in future.
        int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007, 8800008, 8800009, 8800010};
        for (int i : zakpart) {
            MapleMonster part = MapleLifeFactory.getMonster(i);
            part.changeLevel(120);
            part.getStats().setChange(true);
            part.setPosition(spos);
            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    /**
     * 刷出进阶扎昆
     */
    public void spawnChaosZakum(int x, int y, long maxhp) {
        Point pos = new Point(-10, -215);
        MapleMonster mainb = MapleLifeFactory.getMonster(8800100);
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);
        mainb.getStats().setChange(true);
        mainb.changeLevel(160);
        if (maxhp > 0) {
            mainb.getChangedStats().setOHp(maxhp);
            mainb.setHp(maxhp);
        }
        spawnFakeMonster(mainb);
        // Might be possible to use the map object for reference in future.
        int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107, 8800108, 8800109, 8800110};
        for (int i : zakpart) {
            MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);
            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public final void spawnSimpleZakum(final int x, final int y, long maxhp) {
        final Point point = new Point(-10, -215);
        final MapleMonster mainb = MapleLifeFactory.getMonster(8800020);
        final Point calcPointBelow = this.calcPointBelow(new Point(point.x, point.y - 1));
        mainb.setPosition(calcPointBelow);
        mainb.setFake(true);
        if (maxhp > 0) {
            mainb.changeLevel(55);
            mainb.getStats().setChange(true);
            mainb.getChangedStats().setOHp(maxhp);
            mainb.setHp(maxhp);
        }
        spawnFakeMonster(mainb);
        final int[] zakpart = {8800023, 8800024, 8800025, 8800026, 8800027, 8800028, 8800029, 8800030};
        for (int length = zakpart.length, i = 0; i < length; ++i) {
            final MapleMonster part = MapleLifeFactory.getMonster(zakpart[i]);
            part.setPosition(calcPointBelow);
            this.spawnMonster(part, -2);
        }
        if (this.squadSchedule != null) {
            this.cancelSquadSchedule(false);
        }
    }

    /**
     * 刷出粉色扎昆
     */
    public void spawnPinkZakum(int x, int y) {
        Point pos = new Point(x, y);
        MapleMonster mainb = MapleLifeFactory.getMonster(9400900);
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);
        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);
        int[] zakpart = {9400903, 9400904, 9400905, 9400906, 9400907, 9400908, 9400909, 9400910};
        for (int i : zakpart) {
            MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);
            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule(false);
        }
    }

    public void spawnFakeMonsterOnGroundBelow(MapleMonster mob, Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    private void checkRemoveAfter(MapleMonster monster) {
        int ra = monster.getStats().getRemoveAfter();
        if (ra > 0 && monster.getLinkCID() <= 0) {
            monster.registerKill(ra * 1000);
        }
    }

    public void spawnRevives(final MapleMonster monster, final int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        if (monster.getId() == 9300166) { //炸弹
            MapTimer.getInstance().schedule(() -> broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 2)), 3000);
        }
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, c -> {
            c.announce(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 ? -3 : monster.getStats().getSummonType(), oid)); // TODO effect
        });
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonster(MapleMonster monster, int spawnType) {
        spawnMonster(monster, spawnType, false);
    }

    public void spawnMonster(final MapleMonster monster, final int spawnType, final boolean overwrite) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        if (monster.getId() == 9300166) { //炸弹
            MapTimer.getInstance().schedule(() -> broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 2)), 3000);
        }
        spawnAndAddRangedMapObject(monster, c -> {
            c.announce(MobPacket.spawnMonster(monster, monster.getStats().getSummonType() <= 1 || monster.getStats().getSummonType() == 27 || overwrite ? spawnType : monster.getStats().getSummonType(), 0));
//                c.announce(MobPacket.spawnMonster(monster, spawnType, 0));
        });
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public int spawnMonsterWithEffect(final MapleMonster monster, final int effect, Point pos) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);
            spawnAndAddRangedMapObject(monster, c -> c.announce(MobPacket.spawnMonster(monster, effect, 0)));
            updateMonsterController(monster);
            spawnedMonstersOnMap.incrementAndGet();
            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);
        spawnAndAddRangedMapObject(monster, c -> c.announce(MobPacket.spawnMonster(monster, -4, 0)));
        updateMonsterController(monster);
        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);
        spawnAndAddRangedMapObject(reactor, c -> c.announce(MaplePacketCreator.spawnReactor(reactor)));
    }

    private void respawnReactor(MapleReactor reactor) {
        if (!isSecretMap() && reactor.getReactorId() >= 100000 && reactor.getReactorId() <= 200011) {
            int newRid = (reactor.getReactorId() < 200000 ? 100000 : 200000) + Randomizer.nextInt(11);
            int prop = reactor.getReactorId() % 100;
            if (Randomizer.nextInt(22) <= prop && newRid % 100 < 10) {
                newRid++;
            }
            if (Randomizer.nextInt(110) <= prop && newRid % 100 < 11) {
                newRid++;
            }
            List<Point> toSpawnPos = new ArrayList<>(spawnPoints);
            for (MapleMapObject reactor1l : getAllReactorsThreadsafe()) {
                MapleReactor reactor2l = (MapleReactor) reactor1l;
                if (!toSpawnPos.isEmpty() && toSpawnPos.contains(reactor2l.getPosition())) {
                    toSpawnPos.remove(reactor2l.getPosition());
                    //System.err.println("重置反应堆 - 跳过相同的坐标.");
                }
            }
            //System.err.println("重置反应堆 - toSpawnPos: " + toSpawnPos.size() + " herbRocks: " + spawnPoints.size());
            MapleReactor newReactor = new MapleReactor(MapleReactorFactory.getReactor(newRid), newRid);
            newReactor.setPosition(toSpawnPos.isEmpty() ? reactor.getPosition() : toSpawnPos.get(Randomizer.nextInt(toSpawnPos.size())));
            newReactor.setDelay(newRid % 100 == 11 ? 60000 : 5000);
            spawnReactor(newReactor);
            //System.err.println("重置反应堆 - oldId: " + reactor.getReactorId() + " newId: " + newReactor.getReactorId() + " 是否相同: " + (reactor.getReactorId() == newReactor.getReactorId()));
        } else {
            reactor.setState((byte) 0);
            reactor.setAlive(true);
            spawnReactor(reactor);
        }
    }

    public void spawnRune(final MapleRune rune) {
        rune.setMap(this);

        spawnAndAddRangedMapObject(rune, c -> {
            rune.sendSpawnData(c);
            c.announce(MaplePacketCreator.enableActions());
        });
    }

    public void respawnRune() {
        if (this.getCharactersSize() > 0) {
            if (System.currentTimeMillis() - this.spawnRuneTime > 300000) {
                List<Point> spawnPos = new ArrayList<>(spawnPoints);
                MapleRune ag2 = new MapleRune((int) (Math.random() * 9.0));
                for (MapleReactor y2 : this.getAllReactorsThreadsafe()) {
                    if (spawnPos.isEmpty() || !spawnPos.contains(y2.getPosition())) continue;
                    spawnPos.remove(y2.getPosition());
                }
                if (!spawnPos.isEmpty()) {
                    ag2.setPosition(spawnPos.get(Randomizer.nextInt(spawnPos.size())));
                    this.spawnRune(ag2);
                    this.setRuneTime();
                }
            }
        } else {
            this.setRuneTime();
        }
    }

    public void setRuneTime() {
        this.spawnRuneTime = System.currentTimeMillis();
    }

    public void removeRune(MapleRune rune) {
        this.removeMapObject(rune);
        this.broadcastMessage(MaplePacketCreator.removeRune(rune));
    }

    public boolean isSecretMap() {
        switch (mapid) {
            case 910001001: //隐藏地图 - 斯塔切的药草田
            case 910001002: //隐藏地图 - 诺布的矿山
            case 910001003: //隐藏地图 - 新手秘密农场
            case 910001004: //隐藏地图 - 中级者秘密农场
            case 910001005: //隐藏地图 - 新手秘密矿山
            case 910001006: //隐藏地图 - 中级者秘密矿山
            case 910001007: //隐藏地图 - 高手秘密农场
            case 910001008: //隐藏地图 - 高手秘密广场
            case 910001009: //隐藏地图 - 专家秘密农场
            case 910001010: //隐藏地图 - 专家秘密广场
                return true;
            default:
                return false;
        }
    }

    public void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, c -> {
            door.sendSpawnData(c);
            c.announce(MaplePacketCreator.enableActions());
        });
    }

    public void spawnMechDoor(final MechDoor door) {
        spawnAndAddRangedMapObject(door, c -> {
            c.announce(MaplePacketCreator.spawnMechDoor(door, true));
            c.announce(MaplePacketCreator.enableActions());
        });
    }

    /**
     * 召唤召唤兽
     *
     * @param summon
     */
    public void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, c -> {
            if (!summon.isChangedMap() || summon.getOwnerId() == c.getPlayer().getId()) {
                summon.sendSpawnData(c);
            }
        });
    }

    public void spawnFamiliar(MonsterFamiliar familiar) {
        spawnAndAddRangedMapObject(familiar, c -> {
            if (familiar != null && c.getPlayer() != null) {
                familiar.sendSpawnData(c);
            }
        });
    }

    public void spawnExtractor(final MapleExtractor ex) {
        spawnAndAddRangedMapObject(ex, ex::sendSpawnData);
    }

    public void spawnLove(final MapleLove love) {
        spawnAndAddRangedMapObject(love, love::sendSpawnData);

        MapTimer tMan = MapTimer.getInstance();
        tMan.schedule(() -> {
            broadcastMessage(MaplePacketCreator.removeLove(love.getObjectId(), love.getItemId()));
            removeMapObject(love);
        }, 1000 * 60 * 60);
    }

    public void spawnMist(final MapleMist mist, final int duration, boolean fake) {
        spawnAndAddRangedMapObject(mist, mist::sendSpawnData);

        final MapTimer tMan = MapTimer.getInstance();
        ScheduledFuture<?> poisonSchedule = null;
        if (mist.isPoisonMist() && !mist.isMobMist()) { //中毒类型的烟雾处理 不处理怪物的烟雾效果
            final MapleCharacter owner = getCharacterById(mist.getOwnerId());
            final boolean pvp = owner != null && owner.inPVP();
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(pvp ? MapleMapObjectType.PLAYER : MapleMapObjectType.MONSTER))) {
                    if (!pvp && mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.MOB_STAT_Poison)) {
                        if (owner != null) {
                            ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Poison, 1, mist.getSourceSkill().getId(), null, false, mist.getSource().getDOTStack()), true, duration, true, mist.getSource());
                        }
                    }
                }
            }, 2000, 2500);
        } else if (mist.isRecoverMist()) {  //恢复类型的烟雾处理
            poisonSchedule = tMan.register(() -> {
                for (MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                    if (mist.makeChanceResult()) {
                        MapleCharacter chr = ((MapleCharacter) mo);
                        chr.addHPMP((int) (mist.getSource().getX() * (chr.getStat().getMaxHp() / 100.0)), (int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
                    }
                }
            }, 2000, 2500);
        } else if (!mist.isMobMist()) {
            final Skill skill = mist.getSourceSkill();
            if (skill != null) {
                final MapleStatEffect effect = skill.getEffect(mist.getSkillLevel());
                if (effect != null) {
                    poisonSchedule = tMan.register(() -> {
                        for (MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                            if (mist.makeChanceResult()) {
                                if (!effect.getMonsterStati().isEmpty()) {
                                    ((MapleMonster) mo).applyNewStatus(effect);
                                }
                            }
                        }
                    }, 2000, 0);
                }
            }
        }
        //设置烟雾消失的时间
        final ScheduledFuture<?> finalPoisonSchedule = poisonSchedule;
        mist.setPoisonSchedule(finalPoisonSchedule);
        mist.setSchedule(tMan.schedule(() -> {
            if (finalPoisonSchedule != null) {
                finalPoisonSchedule.cancel(false);
            }
            removeMapObject(mist);
            broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId(), false));
        }, duration));
    }

    public void disappearingItemDrop(MapleMapObject dropper, MapleCharacter owner, Item item, Point pos) {
        Point droppos = calcDropPos(pos, pos);
        MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(InventoryPacket.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 3), drop.getTruePosition());
    }

    public void spawnMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(position, position);
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, c -> c.announce(InventoryPacket.dropItemFromMapObject(mdrop, dropper.getTruePosition(), droppos, (byte) 1)));
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    /*
     * 刀飞炼狱推动金币
     */
    public void spawnMesoDropEx(final int meso, final Point dropfrom, final Point dropto, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final Point droppos = calcDropPos(dropto, dropto);
        if (Randomizer.nextBoolean()) {
            droppos.x -= Randomizer.rand(0, 20);
        } else {
            droppos.x += Randomizer.rand(0, 20);
        }
        final MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, c -> c.announce(InventoryPacket.dropItemFromMapObject(mdrop, dropfrom, droppos, (byte) 1)));

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public void spawnMobMesoDrop(final int meso, final Point position, final MapleMapObject dropper, final MapleCharacter owner, final boolean playerDrop, final byte droptype) {
        final MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, c -> c.announce(InventoryPacket.dropItemFromMapObject(mdrop, dropper.getTruePosition(), position, (byte) 1)));

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public void spawnMobDrop(final Item idrop, final Point dropPos, final MapleMonster mob, final MapleCharacter chr, final byte droptype, final int questid) {
        final MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        spawnAndAddRangedMapObject(mdrop, c -> {
            if (c != null && c.getPlayer() != null && (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1) && (idrop.getItemId() / 10000 != 238 || c.getPlayer().getMonsterBook().getLevelByCard(idrop.getItemId()) >= 2) && mob != null && dropPos != null) {
                c.announce(InventoryPacket.dropItemFromMapObject(mdrop, mob.getTruePosition(), dropPos, (byte) 1));
            }
        });
        if (chr.checkSoulWeapon()) {
            chr.writeSoulPacket();
            chr.checkSoulState(false);
        }

        if (mob != null && mob.getStats().getWeaponPoint() > 0 && JobConstants.is神之子(chr.getJob())) {
            chr.gainWeaponPoint(mob.getStats().getWeaponPoint());
        }

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; //fm, ch1
        }

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (((MapleMapItem) o).isRandDrop()) {
                    return;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        MapTimer.getInstance().schedule(() -> {
            Point pos = new Point(Randomizer.nextInt(800) + 531, -806);
            int theItem = Randomizer.nextInt(1000);
            int itemid;
            if (theItem < 950) { //0-949 = normal, 950-989 = rare, 990-999 = super
                itemid = GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
            } else if (theItem < 990) {
                itemid = GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
            } else {
                itemid = GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
            }
            spawnAutoDrop(itemid, pos);
        }, 20000);
    }

    public void spawnAutoDrop(final int itemid, final Point pos) {
        Item idrop;
        final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (ItemConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0);
        }
        idrop.setGMLog("自动掉落 " + itemid + " 地图 " + mapid);
        final MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, c -> c.announce(InventoryPacket.dropItemFromMapObject(mdrop, pos, pos, (byte) 1)));
        broadcastMessage(InventoryPacket.dropItemFromMapObject(mdrop, pos, pos, (byte) 0));
        if (itemid / 10000 != 291) {
            mdrop.registerExpire(120000);
        }
    }

    public void spawnItemDrop(final MapleMapObject dropper, final MapleCharacter owner, final Item item, Point pos, final boolean ffaDrop, final boolean playerDrop) {
        final Point droppos = calcDropPos(pos, pos);
        final MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop);

        spawnAndAddRangedMapObject(drop, c -> c.announce(InventoryPacket.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 1)));
        broadcastMessage(InventoryPacket.dropItemFromMapObject(drop, dropper.getTruePosition(), droppos, (byte) 0));

        if (!everlast) {
            drop.registerExpire(120000);
            activateItemReactors(drop, owner.getClient());
        }
    }

    private void activateItemReactors(final MapleMapItem drop, final MapleClient c) {
        final Item item = drop.getItem();

        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                final MapleReactor react = (MapleReactor) o;

                if (react.getReactorType() == 100) {
                    if (item.getItemId() == GameConstants.getCustomReactItem(react.getReactorId(), react.getReactItem().getLeft()) && react.getReactItem().getRight() == item.getQuantity()) {
                        if (react.getArea().contains(drop.getTruePosition())) {
                            if (!react.isTimerActive()) {
                                MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                                react.setTimerActive(true);
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public int getExtractorSize() {
        return mapobjects.get(MapleMapObjectType.EXTRACTOR).size();
    }

    public int getMobsSize() {
        return mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    public int getRunesSize() {
        return mapobjects.get(MapleMapObjectType.RUNE).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe();
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                ret.add((MapleMapItem) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public Point getPointOfItem(int itemid) {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                MapleMapItem mm = ((MapleMapItem) mmo);
                if (mm.getItem() != null && mm.getItem().getItemId() == itemid) {
                    return mm.getPosition();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return null;
    }

    public List<MapleMist> getAllMistsThreadsafe() {
        ArrayList<MapleMist> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.MIST).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MIST).values()) {
                ret.add((MapleMist) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MIST).readLock().unlock();
        }
        return ret;
    }

    public void returnEverLastItem(MapleCharacter chr) {
        for (MapleMapObject o : getAllItemsThreadsafe()) {
            MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(InventoryPacket.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getTruePosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
                }
                removeMapObject(item);
            }
        }
    }

    public void nextNodeAction(int mobid, int time) {
        MapTimer.getInstance().schedule(() -> {
            if (this.getMonsterById(mobid) != null) {
                this.broadcastMessage(MobPacket.mobEscortStopEndPermmision(this.getMonsterById(mobid).getObjectId()));
            }
        }, time);
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, false);
    }

    public void startMapEffect(String msg, int itemId, boolean jukebox) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(() -> {
            if (mapEffect != null) {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, jukebox ? 300000 : 15000);
    }

    public void startPredictCardMapEffect(String msg, int itemId, int effectType) {
        startMapEffect(msg, itemId, 30, effectType);
    }

    public void startMapEffect(String msg, int itemId, int time) {
        startMapEffect(msg, itemId, time, -1);
    }

    public void startMapEffect(String msg, int itemId, int time, int effectType) {
        if (mapEffect != null) {
            return;
        }
        if (time <= 0) {
            time = 5;
        }
        mapEffect = new MapleMapEffect(msg, itemId, effectType);
        mapEffect.setJukebox(false);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(() -> {
            if (mapEffect != null) {
                broadcastMessage(mapEffect.makeDestroyData());
                mapEffect = null;
            }
        }, time * 1000);
    }

    public void startExtendedMapEffect(final String msg, final int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(() -> {
            broadcastMessage(MaplePacketCreator.removeMapEffect());
            broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, false));
        }, 60000);
    }

    public void startSimpleMapEffect(String msg, int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
    }

    public void startJukebox(String msg, int itemId) {
        startMapEffect(msg, itemId, true);
    }

    public void addPlayer(MapleCharacter chr) {
        mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().lock();
        try {
            mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);
        } finally {
            mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().unlock();
        }
        charactersLock.writeLock().lock();
        try {
            characters.add(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        chr.setChangeTime(true);
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.setTeam(getAndSwitchTeam() ? 0 : 1);
        }
        byte[] packet = MaplePacketCreator.spawnPlayerMapobject(chr);
        if (!chr.isHidden()) {
            broadcastMessage(chr, packet, false);
            if (chr.isIntern() && speedRunStart > 0) {
                endSpeedRun();
                broadcastMessage(MaplePacketCreator.serverNotice(5, "The speed run has ended."));
                broadcastMessage(chr, EffectPacket.getEffectSwitch(chr.getId(), chr.getEffectSwitch()), true);
            }
        } else {
            broadcastGMMessage(chr, packet, false);
            broadcastGMMessage(chr, EffectPacket.getEffectSwitch(chr.getId(), chr.getEffectSwitch()), true);
        }
//        if (isMiniMapOnOff()) {
//            chr.send(UIPacket.showFreeMarketMiniMap(isMiniMapOnOff()));
//        }
        chr.send(UIPacket.showFreeMarketMiniMap(false));
        //发送给地图上其他玩家显示角色的封包
        sendObjectPlacement(chr);
        //发送刷出角色的封包
        //chr.send(packet);
        //地图触发脚本事件
        if (!onUserEnter.equals("")) {
            scripting.map.MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
        }
        if (!onFirstUserEnter.equals("")) {
            if (getCharactersSize() == 1) {
                scripting.map.MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
            }
        }
        GameConstants.achievementRatio(chr.getClient());
        //chr.send(MaplePacketCreator.spawnFlags(nodes.getFlags()));
        if (GameConstants.isTeamMap(mapid) && !chr.inPVP()) {
            chr.send(MaplePacketCreator.showEquipEffect(chr.getTeam()));
        }
        switch (mapid) {
            case 809000101: //昭和村 - 澡堂(男)
            case 809000201: //昭和村 - 澡堂(女)
                chr.send(MaplePacketCreator.showEquipEffect());
                break;
        }
        MaplePet[] pets = chr.getSpawnPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null && pets[i].getSummoned()) {
                pets[i].setPos(chr.getTruePosition());
                chr.petUpdateStats(pets[i], true);
                chr.send(PetPacket.showPet(chr, pets[i], false, false, true));
                chr.send(PetPacket.loadExceptionList(chr, pets[i]));
            }
        }
        if (chr.getAndroid() != null) {
            chr.getAndroid().setPos(chr.getTruePosition());
            broadcastMessage(AndroidPacket.spawnAndroid(chr, chr.getAndroid()));
        }
        if (chr.getParty() != null) {
            chr.silentPartyUpdate();
            chr.send(PartyPacket.updateParty(chr.getClient().getChannel(), chr.getParty(), PartyOperation.更新队伍, null));
            chr.updatePartyMemberHP();
            chr.receivePartyMemberHP();
        }
        if (!chr.isInBlockedMap() && chr.getLevel() > 10) {
            chr.send(MaplePacketCreator.showQuickMove(chr));
        }
        chr.send(NPCPacket.sendNpcHide(hideNpc));

        List<MapleSummon> ss = chr.getSummonsReadLock();
        try {
            for (MapleSummon summon : ss) {
                summon.setPosition(chr.getTruePosition());
                chr.addVisibleMapObject(summon);
                this.spawnSummon(summon);
            }
        } finally {
            chr.unlockSummonsReadLock();
        }
        if (mapEffect != null) {
            mapEffect.sendStartData(chr.getClient());
        }
        if (timeLimit > 0 && getForcedReturnMap() != null) {
            chr.startMapTimeLimitTask(timeLimit, getForcedReturnMap());
        }
        if (chr.getBuffedValue(MapleBuffStat.骑兽技能) != null && !JobConstants.is反抗者(chr.getJob())) {
            if (FieldLimitType.Mount.check(fieldLimit)) {
                chr.cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
            }
        }
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted()) {
            chr.send(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.send((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
        }
        if (chr.getEventInstance() != null) {
            chr.getEventInstance().onMapLoad(chr);
        }
        if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
            chr.send(MaplePacketCreator.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
        }
        if (mapid / 1000 != 105100 && mapid / 100 != 8020003 && mapid / 100 != 8020008 && mapid != 271040100) { //no boss_balrog/2095/coreblaze/auf/cygnus. but coreblaze/auf/cygnus does AFTER
            final MapleSquad sqd = getSquadByMap(); //for all squads
            final EventManager em = getEMByMap();
            if (!squadTimer && sqd != null && chr.getName().equals(sqd.getLeaderName()) && em != null && em.getProperty("leader") != null && em.getProperty("leader").equals("true") && checkStates) {
                //leader? display
                doShrine(false);
                squadTimer = true;
            }
        }
        if (getNumMonsters() > 0 && (mapid == 280030001 || mapid == 240060201 || mapid == 280030000 || mapid == 280030100 || mapid == 240060200 || mapid == 220080001 || mapid == 541020800 || mapid == 541010100)) {
            String music = "Bgm09/TimeAttack";
            switch (mapid) {
                case 240060200: //生命之穴 - 暗黑龙王洞穴
                case 240060201: //生命之穴 - 进阶暗黑龙王洞穴
                    music = "Bgm14/HonTale";
                    break;
                case 280030100: //最后的任务 - 扎昆的祭台 V.110.1修改
                case 280030000: //神秘岛 - 扎昆的祭台
                case 280030001: //最后的任务 - 进阶扎昆的祭台
                    music = "Bgm06/FinalFight";
                    break;
            }
            chr.send(MaplePacketCreator.musicChange(music));
            //maybe timer too for zak/ht
        }
        if (mapid == 914000000 || mapid == 927000000) { //黑暗领主 - 伤病营舍  秘密地图 - 时间神殿回廊1
            chr.send(MaplePacketCreator.temporaryStats_Aran());
        } else if (mapid == 105100300 && chr.getLevel() >= 91) { //蝙蝠怪神殿 - 蝙蝠怪的墓地
            chr.send(MaplePacketCreator.temporaryStats_Balrog(chr));
        }
        chr.send(MaplePacketCreator.temporaryStats_Reset());
        if (JobConstants.is龙神(chr.getJob()) && chr.getJob() >= 2200) {
            if (chr.getDragon() == null) {
                chr.makeDragon();
            } else {
                chr.getDragon().setPosition(chr.getPosition());
            }
            if (chr.getDragon() != null) {
                broadcastMessage(SummonPacket.spawnDragon(chr.getDragon()));
            }
        }
        if (JobConstants.is阴阳师(chr.getJob())) {
            if (chr.getLittleWhite() == null) {
                chr.makeLittleWhite();
            } else {
                chr.getLittleWhite().setPosition(chr.getPosition());
            }
            if (chr.getLittleWhite() != null) {
                broadcastMessage(SummonPacket.spawnLittleWhite(chr.getLittleWhite()));
            }
        }
        if ((mapid == 10000 && chr.getJob() == 0) || (mapid == 130030000 && chr.getJob() == 1000) || (mapid == 914000000 && chr.getJob() == 2000) || (mapid == 900010000 && chr.getJob() == 2001) || (mapid == 931000000 && chr.getJob() == 3000)) {
            chr.send(MaplePacketCreator.startMapEffect("欢迎来到 " + chr.getClient().getChannelServer().getServerName() + "!", 5122000, true));
            chr.dropMessage(5, "使用 @help 可以查看你当前能使用的命令 祝你玩的愉快！");
        }
        if (permanentWeather > 0) {
            chr.send(MaplePacketCreator.startMapEffect("", permanentWeather, false)); //snow, no msg
        }
        if (getPlatforms().size() > 0) {
            chr.send(MaplePacketCreator.getMovingPlatforms(this));
        }
        if (partyBonusRate > 0) {
            //chr.dropMessage(-1, partyBonusRate + "% additional EXP will be applied per each party member here.");
            //chr.dropMessage(-1, "You've entered the party play zone.");
        }
        if (isTown()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.地雷);
        }
        if (!canSoar()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.飞翔);
        }
        if (chr.getJob() < 3200 || chr.getJob() > 3212) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.战法灵气);
        }
        if (chr.getJob() >= 2400 && chr.getJob() <= 2412) { //幻影职业 更换地图要显示角色卡片数量
            chr.send(MaplePacketCreator.updateCardStack(chr.getCardStack()));
        }
        if (isPvpMap()) {
            chr.dropSpouseMessage(0x0A, "[系统提示] 您已进入个人PK地图，请小心。");
        } else if (isPartyPvpMap()) {
            chr.dropSpouseMessage(0x0A, "[系统提示] 您已进入组队PK地图，请小心。");
        } else if (isGuildPvpMap()) {
            chr.dropSpouseMessage(0x0A, "[系统提示] 您已进入家族PK地图，请小心。");
        }
        chr.checkBloodContract();
        chr.send(MaplePacketCreator.showChronosphere(chr.getBossLog("免费超时空券"), (int) Math.ceil(chr.getCSPoints(2) / 200)));
    }

    public int getNumItems() {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.ITEM).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
    }

    public int getNumMonsters() {
        return getNumMonsters(-1);
    }

    public int getNumMonsters(int mobid) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            if (mobid == -1) {
                return mapobjects.get(MapleMapObjectType.MONSTER).size();
            } else {
                return (int) mapobjects.get(MapleMapObjectType.MONSTER).entrySet().stream().filter(entry -> ((MapleMonster) entry.getValue()).getId() == mobid).count();
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public void doShrine(final boolean spawned) { //false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule(true);
        }
        final MapleSquad sqd = getSquadByMap();
        if (sqd == null) {
            return;
        }
        final int mode = ((mapid == 280030000 || mapid == 280030100) ? 1 : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 ? 3 : 0)));
        //chaos_horntail message for horntail too because it looks nicer
        final EventManager em = getEMByMap();
        if (em != null && getCharactersSize() > 0) {
            final String leaderName = sqd.getLeaderName();
            final String state = em.getProperty("state");
            final Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
            if (mode == 1 || mode == 2) { //chaoszakum
                //broadcastMessage(MaplePacketCreator.showChaosZakumShrine(spawned, 5));
            } else if (mode == 3) { //ht/chaosht
                //broadcastMessage(MaplePacketCreator.showChaosHorntailShrine(spawned, 5));
            } else {
                //broadcastMessage(MaplePacketCreator.showHorntailShrine(spawned, 5));
            }
            if (spawned) { //both of these together dont go well
                broadcastMessage(MaplePacketCreator.getClock(300)); //5 min
            }
            final MapleMap returnMapz = returnMapa;
            if (!spawned) { //no monsters yet; inforce timer to spawn it quickly
                final List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                final List<Integer> monsteridz = new ArrayList<>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = () -> {
                    final MapleSquad sqnow = MapleMap.this.getSquadByMap();
                    if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size() && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                        boolean passed = monsterz.isEmpty();
                        for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                            for (int i : monsteridz) {
                                if (m.getObjectId() == i) {
                                    passed = true;
                                    break;
                                }
                            }
                            if (passed) {
                                break;
                            } //even one of the monsters is the same
                        }
                        if (passed) {
                            //are we still the same squad? are monsters still == 0?
//                                byte[] packet;
//                                if (mode == 1 || mode == 2) { //chaoszakum
//                                    packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
//                                } else {
//                                    packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
//                                }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                //chr.send(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }

                };
            } else { //inforce timer to gtfo
                run = () -> {
                    MapleSquad sqnow = MapleMap.this.getSquadByMap();
                    //we dont need to stop clock here because they're getting warped out anyway
                    if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                        //are we still the same squad? monsters however don't count
//                            byte[] packet;
//                            if (mode == 1 || mode == 2) { //chaoszakum
//                                packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
//                            } else {
//                                packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
//                            }
                        for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                            //chr.send(packet);
                            chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                        }
                        checkStates("");
                        resetFully();
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); //5 mins
        }
    }

    public MapleSquad getSquadByMap() {
        MapleSquadType zz;
        switch (mapid) {
            case 105100400: //蝙蝠怪神殿 - 蝙蝠怪的墓地
            case 105100300: //蝙蝠怪神殿 - 蝙蝠怪的墓地
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000: //神秘岛 - 扎昆的祭台
            case 280030100: //最后的任务 - 扎昆的祭台 V.110.1修改
                zz = MapleSquadType.zak;
                break;
            case 280030001: //最后的任务 - 进阶扎昆的祭台
                zz = MapleSquadType.chaoszak;
                break;
            case 240060200: //生命之穴 - 暗黑龙王洞穴
                zz = MapleSquadType.horntail;
                break;
            case 240060201: //生命之穴 - 进阶暗黑龙王洞穴
                zz = MapleSquadType.chaosht;
                break;
            case 270050100: //神殿的深处 - 神的黄昏
                zz = MapleSquadType.pinkbean;
                break;
            case 270051100: //神殿深处 - 众神的黄昏
                zz = MapleSquadType.chaospb;
                break;
            case 802000111: //逆奥之城 - 卡姆那 (远征队)
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211: //逆奥之城 - 防御塔 2100年 (远征队)
                zz = MapleSquadType.vergamot;
                break;
            case 802000311: //逆奥之城 - 公园 2095年 (远征队)
                zz = MapleSquadType.tokyo_2095;
                break;
            case 802000411: //逆奥之城 - 高科区域 2102年 (远征队)
                zz = MapleSquadType.dunas;
                break;
            case 802000611: //逆奥之城 - 天空大战舰甲板 2102年 (远征队)
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711: //逆奥之城 - 核心商业区 2102年（远征队）
                zz = MapleSquadType.dunas2;
                break;
            case 802000801: //逆奥之城 - 商贸中心 2102年(大厅)
            case 802000802: //逆奥之城 - 商贸中心 2102年(升降机井)
            case 802000803: //逆奥之城 - 商贸中心 2102年(入口)
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
            case 802000823: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
                zz = MapleSquadType.aufheben;
                break;
            case 211070100: //狮子王之城 - 接见室
            case 211070101: //狮子王之城 - 空中监狱
            case 211070110: //狮子王之城 - 复活塔楼
                zz = MapleSquadType.vonleon;
                break;
            case 551030200: //马来西亚 - 阴森世界
                zz = MapleSquadType.scartar;
                break;
            case 271040100: //骑士团要塞 - 希纳斯的殿堂
                zz = MapleSquadType.cygnus;
                break;
            case 689013000: //粉色扎昆 - 粉色扎昆 突袭
                zz = MapleSquadType.pinkzak;
                break;
            case 262031300: //希拉之塔 - 希拉之塔
            case 262031310: //希拉之塔 - 灵魂被夺者之屋
                zz = MapleSquadType.hillah;
                break;
            case 272030400: //次元缝隙 - 黑暗祭坛
            case 272030420: //次元缝隙 - 邪恶内心空地
                zz = MapleSquadType.arkarium;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public EventManager getEMByMap() {
        String em;
        switch (mapid) {
            case 105100400: //蝙蝠怪神殿 - 蝙蝠怪的墓地
                em = "BossBalrog_EASY";
                break;
            case 105100300: //蝙蝠怪神殿 - 蝙蝠怪的墓地
                em = "BossBalrog_NORMAL";
                break;
            case 280030100: //最后的任务 - 扎昆的祭台 V.110.1修改
            case 280030000: //神秘岛 - 扎昆的祭台
                em = "ZakumBattle";
                break;
            case 240060200: //生命之穴 - 暗黑龙王洞穴
                em = "HorntailBattle";
                break;
            case 280030001: //最后的任务 - 进阶扎昆的祭台
                em = "ChaosZakum";
                break;
            case 240060201: //生命之穴 - 进阶暗黑龙王洞穴
                em = "ChaosHorntail";
                break;
            case 270050100: //神殿的深处 - 神的黄昏
                em = "PinkBeanBattle";
                break;
            case 270051100: //神殿深处 - 众神的黄昏
                em = "ChaosPinkBean";
                break;
            case 802000111: //逆奥之城 - 卡姆那 (远征队)
                em = "NamelessMagicMonster";
                break;
            case 802000211: //逆奥之城 - 防御塔 2100年 (远征队)
                em = "Vergamot";
                break;
            case 802000311: //逆奥之城 - 公园 2095年 (远征队)
                em = "2095_tokyo";
                break;
            case 802000411: //逆奥之城 - 高科区域 2102年 (远征队)
                em = "Dunas";
                break;
            case 802000611: //逆奥之城 - 天空大战舰甲板 2102年 (远征队)
                em = "Nibergen";
                break;
            case 802000711: //逆奥之城 - 核心商业区 2102年（远征队）
                em = "Dunas2";
                break;
            case 802000801: //逆奥之城 - 商贸中心 2102年(大厅)
            case 802000802: //逆奥之城 - 商贸中心 2102年(升降机井)
            case 802000803: //逆奥之城 - 商贸中心 2102年(入口)
                em = "CoreBlaze";
                break;
            case 802000821: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
            case 802000823: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
                em = "Aufhaven";
                break;
            case 211070100: //狮子王之城 - 接见室
            case 211070101: //狮子王之城 - 空中监狱
            case 211070110: //狮子王之城 - 复活塔楼
                em = "VonLeonBattle";
                break;
            case 551030200: //马来西亚 - 阴森世界
                em = "ScarTarBattle";
                break;
            case 271040100: //骑士团要塞 - 希纳斯的殿堂
                em = "CygnusBattle";
                break;
            case 689013000: //粉色扎昆 - 粉色扎昆 突袭
                em = "PinkZakum";
                break;
            case 262031300: //希拉之塔 - 希拉之塔
            case 262031310: //希拉之塔 - 灵魂被夺者之屋
                em = "Hillah_170";
                break;
            case 272030400: //次元缝隙 - 黑暗祭坛
            case 272030420: //次元缝隙 - 邪恶内心空地
                em = "ArkariumBattle";
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public void removePlayer(MapleCharacter chr) {
        if (everlast) {
            returnEverLastItem(chr);
        }
        charactersLock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        removeMapObject(chr);
        chr.checkFollow();
        chr.removeExtractor();
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));

        removeVisibleSummon(chr);
        checkStates(chr.getName());
        if (mapid == 109020001) { //冒险岛活动 - OX问答
            chr.canTalk(true);
        }
        chr.leaveMap(this);
    }

    /**
     * 角色离开当前地图时移除可见的召唤兽
     *
     * @param chr
     */
    public void removeVisibleSummon(MapleCharacter chr) {
        List<MapleSummon> toCancel = new ArrayList<>();
        List<MapleSummon> listSummons = chr.getSummonsReadLock();
        try {
            listSummons.forEach(summon -> {
                broadcastMessage(SummonPacket.removeSummon(summon, true));
                removeMapObject(summon);
                chr.removeVisibleMapObject(summon);
                if (summon.isChangeMapCanceled()) {
                    toCancel.add(summon);
                } else {
                    summon.setChangedMap(true);
                }
            });
        } finally {
            chr.unlockSummonsReadLock();
        }
        toCancel.forEach(summon -> {
            chr.removeSummon(summon);
            chr.dispelSkill(summon.getSkillId());
        });
    }

    public void broadcastMessage(byte[] packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getTruePosition());
    }

    /*
     * public void broadcastMessage(MapleCharacter source, byte[] packet,
     * boolean repeatToSource, boolean ranged) { broadcastMessage(repeatToSource
     * ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ :
     * Double.POSITIVE_INFINITY, source.getPosition()); }
     */
    public void broadcastMessage(byte[] packet, Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(MapleCharacter source, byte[] packet, Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(MapleCharacter source, byte[] packet, double rangeSq, Point rangedFrom) {
        charactersLock.readLock().lock();
        try {
            characters.stream().filter(chr -> chr != source).forEach(chr -> {
                if (rangeSq < Double.POSITIVE_INFINITY) {
                    if (rangedFrom.distanceSq(chr.getTruePosition()) <= rangeSq) {
                        chr.send(packet);
                    }
                } else {
                    chr.send(packet);
                }
            });
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    /**
     * 发送对象放置
     *
     * @param chr 角色
     */
    private void sendObjectPlacement(MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        getMapObjectsInRange(chr.getTruePosition(), chr.getRange(), GameConstants.rangedMapobjectTypes).forEach(o -> {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    return;
                }
            }
            o.sendSpawnData(chr.getClient());
            chr.addVisibleMapObject(o);
        });
    }

    /**
     * 获取地图节点的距离范围
     *
     * @param from    位置
     * @param rangeSq 距离
     * @return
     */
    public List<MaplePortal> getPortalsInRange(Point from, double rangeSq) {
        return portals.values().stream()
                .filter(type -> from.distanceSq(type.getPosition()) <= rangeSq && type.getTargetMapId() != mapid && type.getTargetMapId() != 999999999)
                .collect(Collectors.toList());
    }

    /**
     * 获取地图上对象的距离范围
     *
     * @param from    位置
     * @param rangeSq 距离
     * @return
     */
    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                mapobjects.get(type).values().stream().filter(mmo -> from.distanceSq(mmo.getTruePosition()) <= rangeSq).forEach(ret::add);
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    /**
     * 获取物品的距离范围
     *
     * @param from    位置
     * @param rangeSq 距离
     * @return
     */
    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Collections.singletonList(MapleMapObjectType.ITEM));
    }

    /**
     * 获取怪物的距离范围
     *
     * @param from    位置
     * @param rangeSq 距离
     * @return
     */
    public List<MapleMapObject> getMonstersInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Collections.singletonList(MapleMapObjectType.MONSTER));
    }

    /**
     * 获取当前地图中对象之前的距离范围
     *
     * @param from            位置
     * @param rangeSq         距离
     * @param MapObject_types 对象
     * @return
     */
    public List<MapleMapObject> getMapObjectsInRange(Point from, double rangeSq, List<MapleMapObjectType> MapObject_types) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                mapobjects.get(type).values().stream().filter(mmo -> from.distanceSq(mmo.getTruePosition()) <= rangeSq).forEach(ret::add);
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRect(Rectangle box) {
        return getMapObjectsInRect(box, Collections.singletonList(MapleMapObjectType.ITEM));
    }

    public List<MapleMapObject> getMonstersInRect(Rectangle box) {
        return getMapObjectsInRect(box, Collections.singletonList(MapleMapObjectType.MONSTER));
    }

    public List<MapleMapObject> getMapObjectsInRect(Rectangle box, List<MapleMapObjectType> MapObject_types) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                mapobjects.get(type).values().stream().filter(mmo -> box.contains(mmo.getTruePosition())).forEach(ret::add);
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleCharacter> getCharactersIntersect(Rectangle box) {
        List<MapleCharacter> ret;
        charactersLock.readLock().lock();
        try {
            ret = characters.stream().filter(chr -> chr.getBounds().intersects(box)).collect(Collectors.toList());
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public List<MapleCharacter> getPlayersInRectAndInList(Rectangle box, List<MapleCharacter> chrList) {
        List<MapleCharacter> character;

        charactersLock.readLock().lock();
        try {
            character = characters.stream().filter(a -> chrList.contains(a) && box.contains(a.getTruePosition())).collect(Collectors.toList());
        } finally {
            charactersLock.readLock().unlock();
        }
        return character;
    }

    public void addPortal(MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(String portalname) {
        return portals.values().stream().filter(port -> port.getName().equals(portalname)).findFirst().orElse(null);
    }

    public MaplePortal getPortal(int portalid) {
        return portals.get(portalid);
    }

    public List<MaplePortal> getPortalSP() {
        return portals.values().stream().filter(port -> port.getName().equals("sp")).collect(Collectors.toCollection(LinkedList::new));
    }

    public void resetPortals() {
        portals.values().forEach(port -> port.setPortalState(true));
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public void setFootholds(MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public int getNumSpawnPoints() {
        return monsterSpawn.size();
    }

    public void loadMonsterRate(boolean first) {
        int spawnSize = monsterSpawn.size();
        if (spawnSize >= 20 || partyBonusRate > 0) {
            maxRegularSpawn = Math.round(spawnSize / monsterRate);
        } else {
            maxRegularSpawn = (int) Math.ceil(spawnSize * monsterRate);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        } else if (maxRegularSpawn <= 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = Math.max(10, spawnSize);
        }

        Collection<Spawns> newSpawn = new LinkedList<>();
        Collection<Spawns> newBossSpawn = new LinkedList<>();
        // Remove carnival spawned mobs
        monsterSpawn.stream().filter(s -> s.getCarnivalTeam() < 2).forEach(s -> {
            if (s.getMonster().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        });
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 15000;
            }
            respawn(false);
        }
    }

    public SpawnPoint addMonsterSpawn(MapleMonster monster, int mobTime, byte carnivalTeam, String msg) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); //at the beginning
        } else {
            monsterSpawn.add(sp);
        }
        return sp;
    }

    public void addAreaMonsterSpawn(MapleMonster monster, Point pos1, Point pos2, Point pos3, int mobTime, String msg, boolean shouldSpawn, boolean sendWorldMsg) {
        pos1 = calcPointBelow(pos1);
        pos2 = calcPointBelow(pos2);
        pos3 = calcPointBelow(pos3);
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");
            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg, shouldSpawn, sendWorldMsg));
    }

    public List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public List<MapleCharacter> getCharactersThreadsafe() {
        List<MapleCharacter> chars = new ArrayList<>();
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                chars.add(mc);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return chars;
    }

    public MapleCharacter getCharacterByName(String id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getName().equalsIgnoreCase(id)) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public MapleCharacter getCharacterById_InMap(int id) {
        return getCharacterById(id);
    }

    public MapleCharacter getCharacterById(int id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getId() == id) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    /**
     * 更新当前地图中的可见对象数据
     *
     * @param chr 角色对象
     * @param mo  地图对象
     */
    public void updateMapObjectVisibility(MapleCharacter chr, MapleMapObject mo) {
        if (chr == null) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { //判断当前地图上的对象是否可见
            if (mo.getType() == MapleMapObjectType.MIST || mo.getType() == MapleMapObjectType.EXTRACTOR || mo.getType() == MapleMapObjectType.SUMMON || mo.getType() == MapleMapObjectType.FAMILIAR || mo instanceof MechDoor || mo.getTruePosition().distanceSq(chr.getTruePosition()) <= mo.getRange()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else { // 当前地图上的对象是可见的
            if (!(mo instanceof MechDoor) && mo.getType() != MapleMapObjectType.MIST && mo.getType() != MapleMapObjectType.EXTRACTOR && mo.getType() != MapleMapObjectType.SUMMON && mo.getType() != MapleMapObjectType.FAMILIAR && mo.getTruePosition().distanceSq(chr.getTruePosition()) > mo.getRange()) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            } else if (mo.getType() == MapleMapObjectType.MONSTER) { //当前地图对象的类型为怪物并且是可见的
                if (chr.getTruePosition().distanceSq(mo.getTruePosition()) <= GameConstants.maxViewRangeSq_Half()) { //判断当前角色所在的位置跟地图中怪物所在的位置是否在游戏窗口内！
                    updateMonsterController((MapleMonster) mo);
                }
            }
        }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                updateMapObjectVisibility(mc, monster);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    /**
     * 玩家移动
     *
     * @param player      玩家
     * @param newPosition 坐标
     */
    public void movePlayer(MapleCharacter player, Point newPosition) {
        player.setPosition(newPosition);
        if (player.getReactor().get() != null) {
            player.getReactor().get().setPosition(newPosition);
        }
        try {
            Collection<MapleMapObject> visibleObjects = player.getAndWriteLockVisibleMapObjects();
            ArrayList<MapleMapObject> copy = new ArrayList<>(visibleObjects);
            for (MapleMapObject mo : copy) {
                if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                    updateMapObjectVisibility(player, mo);
                } else if (mo != null) {
                    visibleObjects.remove(mo);
                }
            }
            for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), GameConstants.maxViewRangeSq())) {
                if (mo != null && !player.isMapObjectVisible(mo)) {
                    mo.sendSpawnData(player.getClient());
                    visibleObjects.add(mo);
                }
            }
        } finally {
            player.unlockWriteVisibleMapObjects();
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal findClosestPortal(Point from) {
        MaplePortal closest = getPortal(0);
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (distance < shortestDistance) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public MaplePortal getRandomSpawnpoint() {
        List<MaplePortal> spawnPoints_ = new ArrayList<>();
        for (MaplePortal portal : portals.values()) {
            if (portal.getType() >= 0 && portal.getType() <= 2) {
                spawnPoints_.add(portal);
            }
        }
        MaplePortal portal = spawnPoints_.get(new Random().nextInt(spawnPoints_.size()));
        return portal != null ? portal : getPortal(0);
    }

    public String spawnDebug() {
        String sb = "Mobs in map : " + this.getMobsSize() +
                " spawnedMonstersOnMap: " +
                spawnedMonstersOnMap +
                " spawnpoints: " +
                monsterSpawn.size() +
                " maxRegularSpawn: " +
                maxRegularSpawn +
                " actual monsters: " +
                getNumMonsters() +
                " monster rate: " +
                monsterRate +
                " fixed: " +
                fixedMob;

        return sb;
    }

    public int getMapObjectSize() {
        return mapobjects.size();
    }

    public int getCharactersSize() {
        int ret = 0;
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter character : characters) {
                ret++;
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public int getSpawnedForcesOnMap() {
        return spawnedForcesOnMap.incrementAndGet();
    }

    public int getSpawnedMistOnMap() {
        return spawnedMistOnMap.incrementAndGet();
    }

    public void respawn(boolean force) {
        respawn(force, System.currentTimeMillis());
    }

    public void respawn(boolean force, long now) {
        lastSpawnTime = now;
        if (force) { //cpq quick hack
            final int numShouldSpawn = monsterSpawn.size() - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;

                for (Spawns spawnPoint : monsterSpawn) {
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else {
            int defaultNum = (GameConstants.isForceRespawn(mapid) ? monsterSpawn.size() : maxRegularSpawn) - spawnedMonstersOnMap.get();
            int numShouldSpawn = ServerConfig.CHANNEL_MONSTER_MAXCOUNT > 0 ? Math.max(defaultNum, ServerConfig.CHANNEL_MONSTER_MAXCOUNT) : defaultNum;
            if (numShouldSpawn > 0) {
                int spawned = 0;

                List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
                Collections.shuffle(randomSpawn);

                for (Spawns spawnPoint : randomSpawn) {
                    if (!isSpawns && spawnPoint.getMobTime() > 0) {
                        continue;
                    }
                    if (spawnPoint.shouldSpawn(lastSpawnTime) || GameConstants.isForceRespawn(mapid) || (monsterSpawn.size() < 10 && maxRegularSpawn > monsterSpawn.size() && partyBonusRate > 0)) {
                        spawnPoint.spawnMonster(this);
                        spawned++;
                    }
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr.getTruePosition().y > -80) {
                    teamss[0]++;
                } else {
                    teamss[1]++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(id);
    }

    public void addDisconnected(int id) {
        dced.add(id);
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        final MapleSquad squads = getSquadByMap();
        if (squads != null) {
            charactersLock.readLock().lock();
            try {
                for (MapleCharacter chr : characters) {
                    if (chr.getName().equals(squads.getLeaderName()) && !chr.isIntern()) {
                        startSpeedRun(chr.getName());
                        return;
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, ExpeditionType type, long timz, Collection<String> squad) {
        try {
            long lastTime = SpeedRunner.getSpeedRunData(type) == null ? 0 : SpeedRunner.getSpeedRunData(type).right;
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)")) {
                    ps.setString(1, type.name());
                    ps.setString(2, leader);
                    ps.setString(3, time);
                    ps.setLong(4, timz);
                    ps.setString(5, z);
                    ps.executeUpdate();
                }
            }

            if (lastTime == 0) { //great, we just add it
                SpeedRunner.addSpeedRunData(type, SpeedRunner.addSpeedRunData(new StringBuilder(SpeedRunner.getPreamble(type)), new HashMap<>(), z, leader, 1, time), timz);
            } else {
                //i wish we had a way to get the rank
                //TODO revamp
                SpeedRunner.removeSpeedRunData(type);
                SpeedRunner.loadSpeedRunData(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isGM()) {
                chr.getClient().disconnect(true, false);
                chr.getClient().getSession().close();
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
                ret.add((MapleNPC) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
        return ret;
    }

    public void resetNPCs() {
        removeNpc(-1);
    }

    public void resetPQ(int level) {
        resetFully();
        for (MapleMonster mons : getAllMonstersThreadsafe()) {
            mons.changeLevel(level, true);
        }
        resetSpawnLevel(level);
    }

    public void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public void resetFully() {
        resetFully(true);
    }

    public void resetFully(boolean respawn) {
        killAllMonsters(false);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule(true);
        resetPortals();
        if (respawn) {
            respawn(true);
        }
    }

    public void cancelSquadSchedule(boolean interrupt) {
        squadTimer = false;
        checkStates = true;
        if (squadSchedule != null) {
            squadSchedule.cancel(interrupt);
            squadSchedule = null;
        }
    }

    public void obtacleFall(final int count, final int type1, final int type2) {
        this.broadcastMessage(MaplePacketCreator.createObtacleAtom(count, type1, type2, this));
    }

    public void removeDrops() {
        List<MapleMapItem> mapItems = this.getAllItemsThreadsafe();
        for (MapleMapItem mapItem : mapItems) {
            mapItem.expire(this);
        }
    }

    public void removeDropsDelay() {
        List<MapleMapItem> mapItems = this.getAllItemsThreadsafe();
        int delay = 0, i = 0;
        for (MapleMapItem mapItem : mapItems) {
            i++;
            if (i < 50) { //先清理掉50个道具 然后在处理后面的
                mapItem.expire(this);
            } else {
                delay++;
                if (mapItem.hasFFA()) {
                    mapItem.registerFFA(delay * 20); //最大持续时间为 30000 毫秒 也就是30秒 清理的时候设置为 delay * 20 毫秒
                } else {
                    mapItem.registerExpire(delay * 30); //最大的持续是ian为 120000 毫秒 也就是 120秒 清理的时候设置为 delay * 30 毫秒
                }
            }
        }
    }

    public void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> AllSpawnPoints = new LinkedList<>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns spawnPoint : AllSpawnPoints) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            newMons.setF(spawnPoint.getF());
            newMons.setFh(spawnPoint.getFh());
            newMons.setPosition(spawnPoint.getPosition());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> AllSpawnPoints = monsterSpawn.iterator();
        while (AllSpawnPoints.hasNext()) {
            if (AllSpawnPoints.next().getCarnivalId() > -1) {
                AllSpawnPoints.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public boolean makeCarnivalSpawn(int team, MapleMonster newMons, int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : nodes.getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team) && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; //this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; //this point is safe for use.
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); //always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); //does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public boolean makeCarnivalReactor(int team, int num) {
        MapleReactor old = getReactorByName(team + "" + num);
        if (old != null && old.getState() < 5) { //already exists
            return false;
        }
        Point guardz = null;
        List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : nodes.getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getTruePosition().x == guard.left.x && r.getTruePosition().y == guard.left.y && r.getState() < 5) {
                        found = true;
                        break; //already used
                    }
                }
                if (!found) {
                    guardz = guard.left; //this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            MapleReactor my = new MapleReactor(MapleReactorFactory.getReactor(9980000 + team), 9980000 + team);
            my.setState((byte) 1);
            my.setName(team + "" + num); //lol
            //with num. -> guardians in factory
            spawnReactorOnGroundBelow(my, guardz);
        }
        return guardz != null;
    }

    public void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;
    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public void checkStates(String chr) {
        if (!checkStates) {
            return;
        }
        MapleSquad sqd = getSquadByMap();
        EventManager em = getEMByMap();
        int size = getCharactersSize();
        if (sqd != null && sqd.getStatus() == 2) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equalsIgnoreCase(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule(!chr.equals(""));
                    sqd.clear();
                    sqd.copy();
                }
            }
        }
        if (em != null && em.getProperty("state") != null && (sqd == null || sqd.getStatus() == 2) && size == 0) {
            em.setProperty("state", "0");
            if (em.getProperty("leader") != null) {
                em.setProperty("leader", "true");
            }
        }
        if (speedRunStart > 0 && size == 0) {
            endSpeedRun();
        }
        //if (squad != null) {
        //    final MapleSquad sqdd = ChannelServer.getInstance(channel).getMapleSquad(squad);
        //    if (sqdd != null && chr != null && chr.length() > 0 && sqdd.getAllNextPlayer().contains(chr)) {
        //	sqdd.getAllNextPlayer().remove(chr);
        //	broadcastMessage(MaplePacketCreator.serverNotice(5, "The queued player " + chr + " has left the map."));
        //    }
        //}
    }

    public void setCheckStates(boolean b) {
        this.checkStates = b;
    }

    public List<MaplePlatform> getPlatforms() {
        return nodes.getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return nodes.getNodes();
    }

    public void setNodes(MapleNodes mn) {
        this.nodes = mn;
    }

    public MapleNodeInfo getNode(int index) {
        return nodes.getNode(index);
    }

    public boolean isLastNode(int index) {
        return nodes.isLastNode(index);
    }

    public List<Rectangle> getAreas() {
        return nodes.getAreas();
    }

    public Rectangle getArea(int index) {
        return nodes.getArea(index);
    }

    public void changeEnvironment(String ms, int type) {
        broadcastMessage(MaplePacketCreator.environmentChange(ms, type));
    }

    public int getNumPlayersInArea(int index) {
        return getNumPlayersInRect(getArea(index));
    }

    public int getNumPlayersInRect(Rectangle rect) {
        int ret = 0;
        charactersLock.readLock().lock();
        try {
            //            MapleCharacter a;
            for (MapleCharacter character : characters) {
                if (rect.contains(character.getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public int getNumPlayersItemsInArea(int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public int getNumPlayersItemsInRect(Rectangle rect) {
        int ret = getNumPlayersInRect(rect);
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (rect.contains(mmo.getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public void broadcastGMMessage(MapleCharacter source, byte[] packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet);
    }

    private void broadcastGMMessage(MapleCharacter source, byte[] packet) {
        charactersLock.readLock().lock();
        try {
            if (source == null) {
                for (MapleCharacter chr : characters) {
                    if (chr.isStaff()) {
                        chr.send(packet);
                    }
                }
            } else {
                for (MapleCharacter chr : characters) {
                    if (chr != source && (chr.getGMLevel() >= source.getGMLevel())) {
                        chr.send(packet);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public List<Pair<Integer, Integer>> getMobsToSpawn() {
        return nodes.getMobsToSpawn();
    }

    public List<Integer> getSkillIds() {
        return nodes.getSkillIds();
    }

    public boolean canSpawn(long now) {
        return lastSpawnTime > 0 && lastSpawnTime + (ServerConfig.CHANNEL_MONSTER_REFRESH > 0 ? ServerConfig.CHANNEL_MONSTER_REFRESH * 1000 : createMobInterval) < now;
    }

    public boolean canHurt(long now) {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < now) {
            lastHurtTime = now;
            return true;
        }
        return false;
    }

    public void resetShammos(final MapleClient c) {
        killAllMonsters(true);
        broadcastMessage(MaplePacketCreator.serverNotice(5, "A player has moved too far from Shammos. Shammos is going back to the start."));
        EtcTimer.getInstance().schedule(() -> {
            if (c.getPlayer() != null) {
                c.getPlayer().changeMap(MapleMap.this, getPortal(0));
                if (getCharactersThreadsafe().size() > 1) {
                    scripting.map.MapScriptMethods.startScript_FirstUser(c, "shammos_Fenter");
                }
            }
        }, 500); //avoid dl
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void setInstanceId(int ii) {
        this.instanceid = ii;
    }

    public int getPartyBonusRate() {
        return partyBonusRate;
    }

    public void setPartyBonusRate(int ii) {
        this.partyBonusRate = ii;
    }

    public short getTop() {
        return top;
    }

    public void setTop(int ii) {
        this.top = (short) ii;
    }

    public short getBottom() {
        return bottom;
    }

    public void setBottom(int ii) {
        this.bottom = (short) ii;
    }

    public short getLeft() {
        return left;
    }

    public void setLeft(int ii) {
        this.left = (short) ii;
    }

    public short getRight() {
        return right;
    }

    public void setRight(int ii) {
        this.right = (short) ii;
    }

    public List<Pair<Point, Integer>> getGuardians() {
        return nodes.getGuardians();
    }

    public DirectionInfo getDirectionInfo(int i) {
        return nodes.getDirection(i);
    }

    /**
     * 计算方法暂时如下.
     *
     * @试运行: 一天24小时 1小时：41点 1小时等于60分钟 60分钟划分为6份 每份10分钟 每10分钟1次 每次7点
     * @按等级计算公式： int givNx = ((jsNx * chr.getLevel()/2) + Nx);
     * @备注信息： 以上计算方法结果如下： 100级玩家/每天最多获得1000点券。
     */
    public void AutoNx(int jsNx, boolean isAutoPoints) {
        if (mapid != 910000000) {
            return;
        }
        for (MapleCharacter chr : characters) {
            if (chr != null) {
//                if (chr.getClient().getLastPing() <= 0) {
//                    chr.getClient().sendPing();
//                }
                if (isAutoPoints) {
                    chr.gainPlayerPoints(jsNx);
                    chr.dropMessage(5, "[系统奖励] 在线时间奖励获得 [" + jsNx + "] 点积分.");
                } else {
                    int givNx = ((chr.getLevel() / 10) + jsNx);
                    chr.modifyCSPoints(2, givNx);
                    chr.dropMessage(5, "[系统奖励] 在线时间奖励获得 [" + givNx + "] 点抵用券.");
                }
            }
        }
    }

    /*
     * 市场泡点
     */
    public void AutoGain(int jsexp, int expRate) {
        if (mapid != 910000000) {
            return;
        }
        for (MapleCharacter chr : characters) {
            if (chr == null || chr.getLevel() >= 250) {
                return;
            }
            int givExp = ((jsexp * chr.getLevel()) + expRate);
            givExp *= 3;
            chr.gainExp(givExp, true, false, true);
            chr.dropMessage(5, "[系统奖励] 在线时间奖励获得 [" + givExp + "] 点经验.");
        }
    }

    /*
     * 是否是自由市场地图
     */
    public boolean isMarketMap() {
        return mapid >= 910000000 && mapid <= 910000017;
    }

    /*
     * 是否PK地图
     */
    public boolean isPvpMaps() {
        return isPvpMap() || isPartyPvpMap() || isGuildPvpMap();
    }

    /*
     * 是否个人PK地图
     */
    public boolean isPvpMap() {
        return ServerConstants.isPvpMap(mapid);
    }

    /*
     * 是否组队PK地图
     */
    public boolean isPartyPvpMap() {
        return mapid == 910000019 || mapid == 910000020;
    }

    /*
     * 是否家族PK地图
     */
    public boolean isGuildPvpMap() {
        return mapid == 910000021 || mapid == 910000022;
    }

    /*
     * 是否是BOSS地图
     */
    public boolean isBossMap() {
        switch (mapid) {
            case 105100400: //蝙蝠怪神殿 - 蝙蝠怪的墓地
            case 105100300: //蝙蝠怪神殿 - 蝙蝠怪的墓地
            case 280030000: //神秘岛 - 扎昆的祭台
            case 280030100: //最后的任务 - 扎昆的祭台
            case 280030001: //最后的任务 - 进阶扎昆的祭台
            case 240040700: //神木村 - 生命之穴入口
            case 240060200: //生命之穴 - 暗黑龙王洞穴
            case 240060201: //生命之穴 - 进阶暗黑龙王洞穴
            case 270050100: //神殿的深处 - 神的黄昏
            case 802000111: //逆奥之城 - 卡姆那 (远征队)
            case 802000211: //逆奥之城 - 防御塔 2100年 (远征队)
            case 802000311: //逆奥之城 - 公园 2095年 (远征队)
            case 802000411: //逆奥之城 - 高科区域 2102年 (远征队)
            case 802000611: //逆奥之城 - 天空大战舰甲板 2102年 (远征队)
            case 802000711: //逆奥之城 - 核心商业区 2102年（远征队）
            case 802000801: //逆奥之城 - 商贸中心 2102年(大厅)
            case 802000802: //逆奥之城 - 商贸中心 2102年(升降机井)
            case 802000803: //逆奥之城 - 商贸中心 2102年(入口)
            case 802000821: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
            case 802000823: //逆奥之城 - 商贸中心顶楼 2102年（远征队）
            case 211070100: //狮子王之城 - 接见室
            case 211070101: //狮子王之城 - 空中监狱
            case 211070110: //狮子王之城 - 复活塔楼
            case 551030200: //马来西亚 - 阴森世界
            case 271040100: //骑士团要塞 - 希纳斯的殿堂
            case 271040200: //骑士团要塞 - 希纳斯的后院
            case 300030310: //艾琳森林 - 女王藏身处
            case 220080001: //玩具城 - 时间塔的本源
            case 262031300: //希拉之塔 - 希拉之塔
            case 262031310: //希拉之塔 - 灵魂被夺者之屋
            case 272030400: //次元缝隙 - 黑暗祭坛
            case 272030420: //次元缝隙 - 邪恶内心空地
                return true;
            default:
                return false;
        }
    }

    /*
     * 检测角色是否吸怪
     */
    public void checkMoveMonster(Point from, boolean fly, MapleCharacter chr) {
        if (maxRegularSpawn <= 2 || monsterSpawn.isEmpty() || monsterRate <= 1.0 || chr == null) {
            return;
        }
        int check = (int) (((fly ? 70 : 60) / 100.0) * maxRegularSpawn);
        //System.err.println("检测数量: " + check + " 怪物数量: " + getMonstersInRange(from, 4000.0).size() + " 最大刷新: " + maxRegularSpawn);
        if (getMonstersInRange(from, 5000.0).size() >= check) {
            //System.err.println("怪物数量超过 杀死所有怪物...");
            for (MapleMapObject obj : getMonstersInRange(from, Double.POSITIVE_INFINITY)) {
                MapleMonster mob = (MapleMonster) obj;
                killMonster(mob, chr, false, false, (byte) 1);
            }
        }
    }

    /*
     * 召唤箭矢炮盘
     */
    public void spawnArrowsTurret(final MapleArrowsTurret aturet) {
        MapTimer tMan = MapTimer.getInstance();
        final ScheduledFuture poisonSchedule;
        spawnAndAddRangedMapObject(aturet, c -> {
            broadcastMessage(BuffPacket.isArrowsTurretAction(aturet, false));
            c.announce(BuffPacket.isArrowsTurretAction(aturet, true));
        });
        poisonSchedule = tMan.register(() -> {
            if (getCharacterById(aturet.getOwnerId()) == null) {
                removeMapObject(aturet);
                broadcastMessage(BuffPacket.cancelArrowsTurret(aturet));
            }
        }, 500, 500);

        aturet.setSchedule(tMan.schedule(() -> {
            poisonSchedule.cancel(false);
            removeMapObject(aturet);
            broadcastMessage(BuffPacket.cancelArrowsTurret(aturet));
        }, ((20 + (long) Math.floor(aturet.getSkillLevel() / 3)) * 1000)));
    }

    /*
     * 获取地图上的所有炮台对象
     */
    public List<MapleArrowsTurret> getAllArrowsTurrets() {
        return getArrowsTurretsThreadsafe();
    }

    public List<MapleArrowsTurret> getArrowsTurretsThreadsafe() {
        List<MapleArrowsTurret> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.ARROWS_TURRET).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ARROWS_TURRET).values()) {
                ret.add((MapleArrowsTurret) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ARROWS_TURRET).readLock().unlock();
        }
        return ret;
    }

    public void addKSPsychicObject(int chrid, int skillid, List<KSPsychicSkillEntry> infos) {
        Map<Integer, List<Pair<Integer, Integer>>> ksobj = new HashMap<>();
        List<Pair<Integer, Integer>> objs = new ArrayList<>();
        for (KSPsychicSkillEntry ksse : infos) {
            objs.add(new Pair<>(ksse.getOid(), ksse.getMobOid() != 0 ? ksse.getMobOid() : ksse.getObjectid()));
        }
        ksobj.put(skillid, objs);
        kspsychicObjects.put(chrid, ksobj);
    }

    public int removeKSPsychicObject(int chrid, int skillid, int moboid) {
        int oid = -1;
        kspsychicLock.writeLock().lock();
        try {
            if (!kspsychicObjects.containsKey(chrid)) {
                return oid;
            } else if (!kspsychicObjects.get(chrid).containsKey(skillid)) {
                return oid;
            }
            Iterator<Pair<Integer, Integer>> it = kspsychicObjects.get(chrid).get(skillid).iterator();
            while (it.hasNext()) {
                Pair<Integer, Integer> ks = it.next();
                if (ks.getRight() == moboid) {
                    oid = ks.getLeft();
                    it.remove();
                }
            }
        } finally {
            kspsychicLock.writeLock().unlock();
        }
        return oid;
    }

    public void addKSUltimateSkill(int chrid, int moboid) {
        ksultimates.put(chrid, moboid);
    }

    public void removeKSUltimateSkill(int chrid) {
        ksultimates.remove(chrid);
    }

    public boolean isKSUltimateSkill(int chrid, int moboid) {
        return ksultimates.containsKey(chrid) && ksultimates.get(chrid) == moboid;
    }

    public void createSwordNode(int n2) {
        this.createSwordNode(n2, null);
    }

    public void createSwordNode(int n2, Point point) {
        int oid;
        if (swordNodes == null) {
            swordNodes = new HashMap<>();
        }
        runningOidLock.lock();
        try {
            oid = runningOid.getAndIncrement();
        } finally {
            runningOidLock.unlock();
        }
        Point point2 = point == null ? new Point(Randomizer.rand(left, right), -200) : point;
        MapleSwordNode swordNode = new MapleSwordNode(n2, oid, point2);
        swordNodes.put(oid, swordNode);
        broadcastMessage(MobPacket.CreateDemianFlyingSword(true, oid, n2, point2));
        broadcastMessage(MobPacket.NodeDemianFlyingSword(oid, false, swordNode));
        int chrid = getCharacters().get(Randomizer.nextInt(getCharactersSize())).getId();
        swordNode.setBKM(chrid);
        broadcastMessage(MobPacket.TargetDemianFlyingSword(oid, chrid));
        swordNodeAck(oid, false);
    }

    public void swordNodeAck(int oid, boolean bl2) {
        MapleSwordNode i2 = swordNodes.get(oid);
        if (i2 == null) {
            return;
        }
        if (i2.getSwordNodeInfos().size() < 14) {
            i2.a(top, bottom, left, right, bl2);
            broadcastMessage(MobPacket.NodeDemianFlyingSword(oid, true, swordNodes.get(oid)));
        } else {
            MobSkill mobSkill;
            i2.gainCount();
            if (i2.getCount() >= 14 && (mobSkill = MobSkillFactory.getMobSkill(131, 28)) != null) {
                Point point = new Point(i2.getPoint());
                MapleMonster monster = getMonsterById(i2.getMonsterId());
                if (monster != null) {
                    MapleMist l2 = new MapleMist(mobSkill.calculateBoundingBox(point, true), monster, mobSkill, point);
                    l2.setSkillDelay(0);
                    l2.setMistType(1);
                    spawnMist(l2, 10000, false);
                }
            }
        }
    }

    public void swordNodeEnd(int oid) {
        MapleSwordNode swordNode = swordNodes.get(oid);
        if (swordNode == null) {
            return;
        }
        if (swordNode.getSwordNodeInfos().size() >= 14) {
            swordNode.getSwordNodeInfos().clear();
            Point point = swordNode.getPoint();
            createSwordNode(swordNode.getMonsterId(), point);
        }
    }

    public Point getRandomPos() {
        Point point;
        ArrayList<Point> arrayList = new ArrayList<>();
        this.getFootholds().getAllRelevants().forEach(p2 -> {
                    int n2 = p2.getX1();
                    int n3 = p2.getX2();
                    int n4 = p2.getY1();
                    int n5 = p2.getY2();
                    int n6 = 0;
                    if (n2 > n4) {
                        n6 = n2;
                        n2 = n4;
                        n4 = n6;
                    }
                    if (n3 > n5) {
                        n6 = n3;
                        n3 = n5;
                        n5 = n6;
                    }
                    arrayList.add(new Point(Randomizer.rand(n2, n4), Randomizer.rand(n3, n5)));
                }
        );
        do {
            point = arrayList.get(Randomizer.nextInt(arrayList.size()));
        } while (this.getFootholds().findBelow(point, false) == null);
        return point;
    }

    public void setEntrustedFishing(boolean entrustedFishing) {
        this.entrustedFishing = entrustedFishing;
    }


    public boolean allowFishing() {
        return this.entrustedFishing;
    }

    private interface DelayedPacketCreation {

        void sendPackets(MapleClient c);
    }

    private class ActivateItemReactor implements Runnable {

        private final MapleMapItem mapitem;
        private final MapleReactor reactor;
        private final MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType()) && !mapitem.isPickedUp()) {
                mapitem.expire(MapleMap.this);
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(() -> reactor.forceHitReactor((byte) 0), reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }
}
