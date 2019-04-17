package scripting.event;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleTraitType;
import client.skills.SkillFactory;
import handling.channel.ChannelServer;
import handling.world.WorldPartyService;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import handling.world.party.PartySearch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.Timer.EventTimer;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import server.squad.MapleSquad;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.packet.UIPacket;

import javax.script.ScriptException;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EventInstanceManager {

    private static final Logger log = LogManager.getLogger(EventInstanceManager.class);
    private final ReentrantReadWriteLock mutex = new ReentrantReadWriteLock();
    private final Lock rL = mutex.readLock(), wL = mutex.writeLock();
    private final Map<String, String> infos = new HashMap<>();
    private final EventManager em;
    private final int channel;
    private final String name;
    private List<MapleCharacter> chars = new LinkedList<>(); //this is messy
    private List<Integer> dced = new LinkedList<>();
    private List<MapleMonster> mobs = new LinkedList<>();
    private Map<Integer, Integer> killCount = new HashMap<>();
    private Properties props = new Properties();
    private long timeStarted = 0;
    private long eventTime = 0;
    private List<Integer> mapIds = new LinkedList<>();
    private List<Boolean> isInstanced = new LinkedList<>();
    private ScheduledFuture<?> eventTimer;
    private boolean disposed = false;

    /**
     * @param em
     * @param name
     * @param channel
     */
    public EventInstanceManager(EventManager em, String name, int channel) {
        this.em = em;
        this.name = name;
        this.channel = channel;
    }

    /**
     * @param chr
     */
    public void registerPlayer(MapleCharacter chr) {
        if (disposed || chr == null) {
            return;
        }
        try {
            wL.lock();
            try {
                chars.add(chr);
            } finally {
                wL.unlock();
            }
            chr.setEventInstance(this);
            em.getIv().invokeFunction("playerEntry", this, chr);
        } catch (ScriptException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : playerEntry:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        }
    }

    /**
     * @param chr
     * @param mapid
     */
    public void changedMap(MapleCharacter chr, int mapid) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("changedMap", this, chr, mapid);
        } catch (ScriptException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : changedMap:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        }
    }

    /**
     * @param delay
     * @param eim
     */
    public void timeOut(final long delay, final EventInstanceManager eim) {
        if (disposed || eim == null) {
            return;
        }
        eventTimer = EventTimer.getInstance().schedule(() -> {
            if (disposed || em == null) {
                return;
            }
            try {
                em.getIv().invokeFunction("scheduledTimeout", eim);
            } catch (ScriptException ex) {
                log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : scheduledTimeout:\r\n" + ex);
            } catch (NoSuchMethodException ex) {
                // Ignore
            }
        }, delay);
    }


    public void stopEventTimer() {
        eventTime = 0;
        timeStarted = 0;
        if (eventTimer != null) {
            eventTimer.cancel(false);
        }
    }

    /**
     * @param time
     */
    public void restartEventTimer(long time) {
        try {
            if (disposed) {
                return;
            }
            timeStarted = System.currentTimeMillis();
            eventTime = time;
            if (eventTimer != null) {
                eventTimer.cancel(false);
            }
            eventTimer = null;
            int timesend = (int) time / 1000;
            for (MapleCharacter chr : getPlayers()) {
                chr.send(MaplePacketCreator.getClock(timesend));
            }
            timeOut(time, this);
        } catch (Exception ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : restartEventTimer:\r\n" + ex);
        }
    }

    /**
     * @param time
     */
    public void startEventTimer(long time) {
        restartEventTimer(time); //just incase
    }

    /**
     * @param time
     */
    public void startEventClock(long time) {
        if (disposed) {
            return;
        }
        int timesend = (int) time / 1000;
        for (MapleCharacter chr : getPlayers()) {
            chr.send(MaplePacketCreator.getClock(timesend));
        }
    }


    public void stopEventClock() {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.send(MaplePacketCreator.stopClock());
        }
    }

    /**
     * @return
     */
    public boolean isTimerStarted() {
        return eventTime > 0 && timeStarted > 0;
    }

    /**
     * @return
     */
    public long getTimeLeft() {
        return eventTime - (System.currentTimeMillis() - timeStarted);
    }

    /**
     * @param party
     * @param map
     */
    public void registerParty(MapleParty party, MapleMap map) {
        registerParty(party, map, 0);
    }

    public void registerParty(MapleParty party, MapleMap map, int questID) {
        if (disposed) {
            return;
        }
        for (MaplePartyCharacter pc : party.getMembers()) {
            MapleCharacter player = map.getCharacterById(pc.getId());
            if (player != null && questID > 0) {
                player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData(String.valueOf(System.currentTimeMillis()));
            }
            registerPlayer(map.getCharacterById(pc.getId()));
        }
        PartySearch ps = WorldPartyService.getInstance().getSearch(party);
        if (ps != null) {
            WorldPartyService.getInstance().removeSearch(ps, "开始组队任务，组队广告已被删除。");
        }
    }

    /*
     * 在活动事件中注销某个玩家
     */

    /**
     * @param chr
     */

    public void unregisterPlayer(MapleCharacter chr) {
        if (disposed) {
            chr.setEventInstance(null);
            return;
        }
        wL.lock();
        try {
            unregisterPlayer_NoLock(chr);
        } finally {
            wL.unlock();
        }
    }

    private boolean unregisterPlayer_NoLock(MapleCharacter chr) {
        if (name.equals("CWKPQ")) { //hard code it because i said so
            MapleSquad squad = ChannelServer.getInstance(channel).getMapleSquad("CWKPQ");//so fkin hacky
            if (squad != null) {
                squad.removeMember(chr.getName());
                if (squad.getLeaderName().equals(chr.getName())) {
                    em.setProperty("leader", "false");
                }
            }
        }
        chr.setEventInstance(null);
        if (disposed) {
            return false;
        }
        if (chars.contains(chr)) {
            chars.remove(chr);
            return true;
        }
        return false;
    }

    /**
     * @param size
     * @param towarp
     * @return
     */
    public boolean disposeIfPlayerBelow(byte size, int towarp) {
        if (disposed) {
            return true;
        }
        MapleMap map = null;
        if (towarp > 0) {
            map = this.getMapFactory().getMap(towarp);
        }

        wL.lock();
        try {
            if (chars != null && chars.size() <= size) {
                List<MapleCharacter> chrs = new LinkedList<>(chars);
                for (MapleCharacter chr : chrs) {
                    if (chr == null) {
                        continue;
                    }
                    unregisterPlayer_NoLock(chr);
                    if (towarp > 0) {
                        chr.changeMap(map, map.getPortal(0));
                    }
                }
                dispose_NoLock();
                return true;
            }
        } catch (Exception ex) {
            log.error("", ex);
        } finally {
            wL.unlock();
        }
        return false;
    }

    /*
     * 保存BOSS任务和给奖励
     */

    /**
     * @param points
     */

    public void saveBossQuest(int points) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            MapleQuestStatus record = chr.getQuestNAdd(MapleQuest.getInstance(150001));
            if (record.getCustomData() != null) {
                record.setCustomData(String.valueOf(points + Integer.parseInt(record.getCustomData())));
            } else {
                record.setCustomData(String.valueOf(points)); // First time
            }
            chr.modifyCSPoints(2, points / 5, true);
            chr.getTrait(MapleTraitType.will).addExp(points / 100, chr);
        }
    }

    /*
     * 给参加任务的角色抵用点卷
     */

    /**
     * @param points
     */

    public void saveNX(int points) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.modifyCSPoints(2, points, true);
        }
    }

    /*
     * 获取参加活动所有角色信息
     */

    /**
     * @return
     */

    public List<MapleCharacter> getPlayers() {
        if (disposed) {
            return Collections.emptyList();
        }
        rL.lock();
        try {
            return new LinkedList<>(chars);
        } finally {
            rL.unlock();
        }
    }

    /**
     * @return
     */
    public List<Integer> getDisconnected() {
        return dced;
    }

    /*
     * 获取参加活动人数
     */

    /**
     * @return
     */

    public int getPlayerCount() {
        if (disposed) {
            return 0;
        }
        return chars.size();
    }

    /*
     * 在活动时间中注册怪物信息
     */

    /**
     * @param mob
     */

    public void registerMonster(MapleMonster mob) {
        if (disposed) {
            return;
        }
        mobs.add(mob);
        mob.setEventInstance(this);
    }

    /*
     * 在活动中取消怪物信息
     * 或者
     * 怪物死亡触发和删除这个怪在活动中的信息
     */

    /**
     * @param mob
     */

    public void unregisterMonster(MapleMonster mob) {
        mob.setEventInstance(null);
        if (disposed) {
            return;
        }
        if (mobs.contains(mob)) {
            mobs.remove(mob);
        }
        if (mobs.isEmpty()) {
            try {
                em.getIv().invokeFunction("allMonstersDead", this);
            } catch (ScriptException ex) {
                log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : allMonstersDead:\r\n" + ex);
            } catch (NoSuchMethodException ex) {
                // Ignore
            }
        }
    }

    /*
     * 在活动角色死亡触发事件
     */

    /**
     * @param chr
     */

    public void playerKilled(MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("playerDead", this, chr);
        } catch (ScriptException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : playerDead:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        }
    }

    /*
     * 在活动中角色复活触发事件
     */

    /**
     * @param chr
     * @return
     */

    public boolean revivePlayer(MapleCharacter chr) {
        if (disposed) {
            return false;
        }
        try {
            Object b = em.getIv().invokeFunction("playerRevive", this, chr);
            if (b instanceof Boolean) {
                return (Boolean) b;
            }
        } catch (ScriptException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : playerRevive:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        }
        return true;
    }

    /*
     * 在活动中角色断开连接触发
     */

    /**
     * @param chr
     * @param idz
     */

    public void playerDisconnected(MapleCharacter chr, int idz) {
        if (disposed) {
            return;
        }
        byte ret;
        try {
            ret = ((Double) em.getIv().invokeFunction("playerDisconnected", this, chr)).byteValue();
        } catch (Exception e) {
            ret = 0;
        }

        wL.lock();
        try {
            if (disposed) {
                return;
            }
            if (chr == null || chr.isAlive()) {
                dced.add(idz);
            }
            if (chr != null) {
                unregisterPlayer_NoLock(chr);
            }
            if (ret == 0) {
                if (getPlayerCount() <= 0) {
                    dispose_NoLock();
                }
            } else if ((ret > 0 && getPlayerCount() < ret) || (ret < 0 && (isLeader(chr) || getPlayerCount() < (ret * -1)))) {
                List<MapleCharacter> chrs = new LinkedList<>(chars);
                for (MapleCharacter player : chrs) {
                    if (player.getId() != idz) {
                        removePlayer(player);
                    }
                }
                dispose_NoLock();
            }
        } catch (Exception ex) {
            log.error("", ex);
        } finally {
            wL.unlock();
        }
    }

//    public final void registerCarnivalParty(final MapleCharacter leader, final MapleMap map, final byte team) {
//        if (disposed) {
//            return;
//        }
//        leader.clearCarnivalRequests();
//        List<MapleCharacter> characters = new LinkedList<>();
//        final MapleParty party = leader.getParty();
//
//        if (party == null) {
//            return;
//        }
//        for (MaplePartyCharacter pc : party.getMembers()) {
//            final MapleCharacter c = map.getCharacterById(pc.getId());
//            if (c != null) {
//                characters.add(c);
//                registerPlayer(c);
//                c.resetCP();
//            }
//        }
//        PartySearch ps = World.Party.getSearch(party);
//        if (ps != null) {
//            World.Party.removeSearch(ps, "The Party Listing has been removed because the Party Quest started.");
//        }
//        final MapleCarnivalParty carnivalParty = new MapleCarnivalParty(leader, characters, team);
//        try {
//            em.getIv().invokeFunction("registerCarnivalParty", this, carnivalParty);
//        } catch (ScriptException ex) {
//            System.out.println("Event name" + em.getName() + ", Instance name : " + name + ", method Name : registerCarnivalParty:\n" + ex);
//        } catch (NoSuchMethodException ex) {
//            //ignore
//        }
//    }

    /**
     * 活动中角色杀死怪物触发事件
     *
     * @param chr
     * @param mob
     */
    public void monsterKilled(MapleCharacter chr, MapleMonster mob) {
        if (disposed) {
            return;
        }
        try {
            int inc = (int) em.getIv().invokeFunction("monsterValue", this, mob.getId());
            if (disposed || chr == null) {
                return;
            }
            Integer kc = killCount.get(chr.getId());
            if (kc == null) {
                kc = inc;
            } else {
                kc += inc;
            }
            killCount.put(chr.getId(), kc);
        } catch (ScriptException ex) {
            log.error("Event name" + (em == null ? "null" : em.getName()) + ", Instance name : " + name + ", method Name : monsterValue:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        } catch (Exception ex) {
            log.error("", ex);
        }
    }

    /*
     * 在活动中怪物攻击触发
     */

    /**
     * @param chr
     * @param mob
     * @param damage
     */

    public void monsterDamaged(MapleCharacter chr, MapleMonster mob, int damage) {
        if (disposed || mob.getId() != 9700037) { //幽灵船船长ghost PQ boss only.
            return;
        }
        try {
            em.getIv().invokeFunction("monsterDamaged", this, chr, mob.getId(), damage);
        } catch (ScriptException ex) {
            log.error("Event name: " + (em == null ? "null" : em.getName() + ".js") + ", Instance name : " + name + ", method Name : monsterValue:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    /**
     * 怪物掉落道具
     *
     * @param chr
     * @param mob
     */
    public void monsterDrop(final MapleCharacter chr, final MapleMonster mob) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("monsterDrop", this, chr, mob);
        } catch (ScriptException ex) {
            log.error("Event name: " + (em == null ? "null" : em.getName() + ".js") + ", Instance name : " + name + ", method Name : monsterDrop:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        }
    }

    /**
     * @param chr
     * @param score
     */
    public void addPVPScore(MapleCharacter chr, int score) {
        if (disposed) { //ghost PQ boss only.
            return;
        }
        try {
            em.getIv().invokeFunction("addPVPScore", this, chr, score);
        } catch (ScriptException ex) {
            log.error("Event name: " + (em == null ? "null" : em.getName() + ".js") + ", Instance name : " + name + ", method Name : monsterValue:\r\n" + ex);
        } catch (NoSuchMethodException ex) {
            // Ignore
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    /*
     * 获取角色在活动中杀怪数量
     */

    /**
     * @param chr
     * @return
     */

    public int getKillCount(MapleCharacter chr) {
        if (disposed) {
            return 0;
        }
        Integer kc = killCount.get(chr.getId());
        if (kc == null) {
            return 0;
        } else {
            return kc;
        }
    }

    /*
     * 清除活动事件
     */


    public void dispose_NoLock() {
        if (disposed || em == null) {
            return;
        }
        String emName = em.getName();
        try {
            disposed = true;
            for (MapleCharacter chr : chars) {
                chr.setEventInstance(null);
            }
            chars.clear();
            chars = null;
            if (mobs.size() >= 1) {
                for (MapleMonster mob : mobs) {
                    if (mob != null) {
                        mob.setEventInstance(null);
                    }
                }
            }
            mobs.clear();
            mobs = null;
            killCount.clear();
            killCount = null;
            infos.clear();
            dced.clear();
            dced = null;
            timeStarted = 0;
            eventTime = 0;
            props.clear();
            props = null;
            for (int i = 0; i < mapIds.size(); i++) {
                if (isInstanced.get(i)) {
                    this.getMapFactory().removeInstanceMap(mapIds.get(i));
                }
            }
            mapIds.clear();
            mapIds = null;
            isInstanced.clear();
            isInstanced = null;
            em.disposeInstance(name);
        } catch (Exception e) {
            log.error("Caused by : " + emName + " instance name: " + name + " method: dispose \r\n" + e);
        }
    }


    public void dispose() {
        wL.lock();
        try {
            dispose_NoLock();
        } finally {
            wL.unlock();
        }
    }

    /**
     * @return
     */
    public ChannelServer getChannelServer() {
        return ChannelServer.getInstance(channel);
    }

    /**
     * @return
     */
    public List<MapleMonster> getMobs() {
        return mobs;
    }

    /*
     * 完成某个成就
     */

    /**
     * @param type
     */

    public void giveAchievement(int type) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.finishAchievement(type);
        }
    }

    /**
     * @param type
     * @param msg
     */
    public void broadcastPlayerMsg(int type, String msg) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.dropMessage(type, msg);
        }
    }

    //PVP

    /**
     * @return
     */
    public List<Pair<Integer, String>> newPair() {
        return new ArrayList<>();
    }

    /**
     * @param e
     * @param e1
     * @param e2
     */
    public void addToPair(List<Pair<Integer, String>> e, int e1, String e2) {
        e.add(new Pair<>(e1, e2));
    }

    /**
     * @return
     */
    public List<Pair<Integer, MapleCharacter>> newPair_chr() {
        return new ArrayList<>();
    }

    /**
     * @param e
     * @param e1
     * @param e2
     */
    public void addToPair_chr(List<Pair<Integer, MapleCharacter>> e, int e1, MapleCharacter e2) {
        e.add(new Pair<>(e1, e2));
    }

    /**
     * @param packet
     */
    public void broadcastPacket(byte[] packet) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.send(packet);
        }
    }

    /**
     * @param packet
     * @param team
     */
    public void broadcastTeamPacket(byte[] packet, int team) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            if (chr.getTeam() == team) {
                chr.send(packet);
            }
        }
    }

    public void environmentChange(final String env, final int mode) {
        if (disposed) {
            return;
        }
        for (MapleCharacter chr : getPlayers()) {
            chr.send(MaplePacketCreator.environmentChange(env, mode));
        }
    }

    public final void addInstanceMap(final int mapid) {
        if (disposed) {
            return;
        }
        mapIds.add(mapid);
        isInstanced.add(true);
    }

    /*
     * 创建1个新的地图模版
     * int mapid, - 地图ID
     * boolean respawns, - 是否刷新怪物
     * boolean npcs, - 是否有NPC
     * boolean reactors, - 是否有反应堆
     * int instanceid - 分配的ID
     */

    /**
     * @param mapid
     * @return
     */

    public MapleMap createInstanceMap(int mapid) {
        if (disposed) {
            return null;
        }
        int assignedid = EventScriptManager.getNewInstanceMapId();
        mapIds.add(assignedid);
        isInstanced.add(true);
        return this.getMapFactory().CreateInstanceMap(mapid, true, true, true, assignedid);
    }

    /*
     * 创建1个新的地图模版
     * int mapid, - 地图ID
     * boolean respawns, - 是否刷新怪物
     * boolean npcs, - 是否有NPC
     * boolean reactors, - 是否有反应堆
     * int instanceid - 分配的ID
     */

    /**
     * @param mapid
     * @return
     */

    public MapleMap createInstanceMapS(int mapid) {
        if (disposed) {
            return null;
        }
        int assignedid = EventScriptManager.getNewInstanceMapId();
        mapIds.add(assignedid);
        isInstanced.add(true);
        return this.getMapFactory().CreateInstanceMap(mapid, false, false, false, assignedid);
    }

    /*
     * gets instance map from the channelserv
     * 从频道中获取地图
     */

    /**
     * @param mapid
     * @return
     */

    public MapleMap setInstanceMap(int mapid) {
        if (disposed) {
            return this.getMapFactory().getMap(mapid);
        }
        mapIds.add(mapid);
        isInstanced.add(false);
        return this.getMapFactory().getMap(mapid);
    }

    /**
     * @return
     */
    public MapleMapFactory getMapFactory() {
        return getChannelServer().getMapFactory();
    }

    public MapleMap getMapFactoryMap(int mapid) {
        return getMapFactory().getMap(mapid);
    }

    /**
     * @param args
     * @return
     */
    public MapleMap getMapInstance(int args) {
        if (disposed) {
            return null;
        }
        try {
            boolean instanced = false;
            int trueMapID;
            if (args >= mapIds.size()) {
                trueMapID = args;
            } else {
                trueMapID = mapIds.get(args);
                instanced = isInstanced.get(args);
            }
            MapleMap map;
            if (!instanced) {
                map = this.getMapFactory().getMap(trueMapID);
                if (map == null) {
                    return null;
                }
                if (map.getCharactersSize() == 0) {
                    if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                        map.shuffleReactors();
                    }
                }
            } else {
                map = this.getMapFactory().getInstanceMap(trueMapID);
                if (map == null) {
                    return null;
                }
                if (map.getCharactersSize() == 0) {
                    if (em.getProperty("shuffleReactors") != null && em.getProperty("shuffleReactors").equals("true")) {
                        map.shuffleReactors();
                    }
                }
            }
            return map;
        } catch (NullPointerException ex) {
            log.error(ex);
            return null;
        }
    }

    /**
     * @param methodName
     * @param delay
     */
    public void schedule(final String methodName, final long delay) {
        if (disposed) {
            return;
        }
        EventTimer.getInstance().schedule(() -> {
            if (disposed || em == null) {
                return;
            }
            try {
                em.getIv().invokeFunction(methodName, EventInstanceManager.this);
            } catch (ScriptException ex) {
                log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : " + methodName + ":\n", ex);
            } catch (NoSuchMethodException ex) {
                // Ignore
            }
        }, delay);
    }

    /**
     * @param methodName
     * @param delay
     * @param player
     */
    public void schedule(final String methodName, final long delay, final MapleCharacter player) {
        if (disposed) {
            return;
        }
        EventTimer.getInstance().schedule(() -> {
            if (disposed || em == null) {
                return;
            }
            try {
                em.getIv().invokeFunction(methodName, EventInstanceManager.this, player);
            } catch (ScriptException ex) {
                log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : " + methodName + ":\n" + ex);
            } catch (NoSuchMethodException ex) {
                // Ignore
            }
        }, delay);
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @param key
     * @param value
     */
    public void setProperty(String key, String value) {
        if (disposed) {
            return;
        }
        props.setProperty(key, value);
    }

    /**
     * @param key
     * @param value
     * @param prev
     * @return
     */
    public Object setProperty(String key, String value, boolean prev) {
        if (disposed) {
            return null;
        }
        return props.setProperty(key, value);
    }

    /**
     * @param key
     * @return
     */
    public String getProperty(String key) {
        if (disposed) {
            return "";
        }
        return props.getProperty(key);
    }

    /**
     * @return
     */
    public Properties getProperties() {
        return props;
    }

    public final void setObjectProperty(final Object obj1, final Object obj2) {
        if (disposed) {
            return;
        }
        props.put(obj1, obj2);
    }

    public final Object getObjectProperty(final Object obj) {
        if (disposed) {
            return null;
        }
        return props.get(obj);
    }

    /*
     * 离开队伍触发
     */

    /**
     * @param chr
     */

    public void leftParty(MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("leftParty", this, chr);
        } catch (Exception ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : leftParty:\n", ex);
        }
    }

    /*
     * 解散队伍触发
     */


    public void disbandParty() {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("disbandParty", this);
        } catch (Exception ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : disbandParty:\n", ex);
        }
    }

    //Separate function to warp players to a "finish" map, if applicable


    public void finishPQ() {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("clearPQ", this);
        } catch (NoSuchMethodException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : clearPQ:\n", ex);
        } catch (ScriptException ex) {
            // Ignore
        }
    }

    /*
     * 角色退出时触发
     */

    /**
     * @param chr
     */

    public void removePlayer(MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("playerExit", this, chr);
        } catch (NoSuchMethodException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : playerExit:\n", ex);
        } catch (ScriptException ex) {
            // Ignore
        }
    }

    /**
     * @param chr
     */
    public void onMapLoad(MapleCharacter chr) {
        if (disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("onMapLoad", this, chr);
        } catch (ScriptException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : onMapLoad:\n", ex);
        } catch (NoSuchMethodException ex) {
            // Ignore, we don't want to update this for all events.
        }
    }

    public void pickUpItem(MapleCharacter player, int itemID) {
        if (this.disposed) {
            return;
        }
        try {
            em.getIv().invokeFunction("pickUpItem", this, player, itemID);
        } catch (NoSuchMethodException ex) {
            log.error("Event name: " + em.getName() + ".js, Instance name : " + name + ", method Name : pickUpItem:\n", ex);
        } catch (ScriptException ex) {
            // Ignore
        }
    }

    /**
     * @param chr
     * @return
     */
    public boolean isLeader(MapleCharacter chr) {
        return (chr != null && chr.getParty() != null && chr.getParty().getLeader().getId() == chr.getId());
    }

    /*
     * 用任务ID来记录是否进行过BOSS远征任务
     */

    /**
     * @param squad
     * @param map
     * @param questID
     */

    public void registerSquad(MapleSquad squad, MapleMap map, int questID) {
        if (disposed) {
            return;
        }
        int mapid = map.getId();
        for (String chr : squad.getMembers()) {
            MapleCharacter player = squad.getChar(chr);
            if (player != null && player.getMapId() == mapid) {
                if (questID > 0) {
                    player.getQuestNAdd(MapleQuest.getInstance(questID)).setCustomData(String.valueOf(System.currentTimeMillis()));
                }
                registerPlayer(player);
                if (player.getParty() != null) {
                    PartySearch ps = WorldPartyService.getInstance().getSearch(player.getParty());
                    if (ps != null) {
                        WorldPartyService.getInstance().removeSearch(ps, "开始组队任务，组队广告已被删除。");
                    }
                }
            }
        }
        squad.setStatus((byte) 2);
        squad.getBeginMap().broadcastMessage(MaplePacketCreator.stopClock());
    }

    /*
     * 用SQL来记录是否进行过BOSS远征任务
     */

    /**
     * @param squad
     * @param map
     * @param bossid
     */

    public void registerSquad(MapleSquad squad, MapleMap map, String bossid) {
        if (disposed) {
            return;
        }
        int mapid = map.getId();
        for (String chr : squad.getMembers()) {
            MapleCharacter player = squad.getChar(chr);
            if (player != null && player.getMapId() == mapid) {
                if (bossid != null) {
                    player.setBossLog(bossid);
                }
                registerPlayer(player);
                if (player.getParty() != null) {
                    PartySearch ps = WorldPartyService.getInstance().getSearch(player.getParty());
                    if (ps != null) {
                        WorldPartyService.getInstance().removeSearch(ps, "开始组队任务，组队广告已被删除。");
                    }
                }
            }
        }
        squad.setStatus((byte) 2); //设置活动开始
        squad.getBeginMap().broadcastMessage(MaplePacketCreator.stopClock());
    }

    /*
     * 检测角色是否在活动中的断开列表中
     */

    /**
     * @param chr
     * @return
     */

    public boolean isDisconnected(MapleCharacter chr) {
        return !disposed && (dced.contains(chr.getId()));
    }

    /*
     * 删除角色在活动中断开列表中的信息
     */

    /**
     * @param id
     */

    public void removeDisconnected(int id) {
        if (disposed) {
            return;
        }
        if (dced.contains(id)) {
            dced.remove(id);
        }
    }

    /**
     * @return
     */
    public EventManager getEventManager() {
        return em;
    }

    /**
     * @param chr
     * @param id
     */
    public void applyBuff(MapleCharacter chr, int id) {
        MapleItemInformationProvider.getInstance().getItemEffect(id).applyTo(chr);
        chr.send(UIPacket.getStatusMsg(id));
    }

    /**
     * @param chr
     * @param id
     */
    public void applySkill(MapleCharacter chr, int id) {
        SkillFactory.getSkill(id).getEffect(1).applyTo(chr);
    }

//    public void displayNode(MapleMonster monster, MapleCharacter player) {
//        if (monster != null) {
//            monster.switchController(player, false);
//            player.getClient().announce(n.a(monster, player.getMap()));
//        }
//    }

    public void EventGainNX() {
        if (this.disposed) {
            return;
        }
        int averlevel = getAverlevel();
        for (MapleCharacter player : this.getPlayers()) {
            player.modifyCSPoints(1, averlevel / 250 * 1000, true);
        }
    }

    public int getAverlevel() {
        int ret = 0;
        for (MapleCharacter player : this.getPlayers()) {
            ret += player.getLevel();
        }
        return ret / this.getPlayers().size();
    }

    public Map<String, String> getInfoStats() {
        return infos;
    }

    public void setInfoStats(Map<String, String> infos) {
        this.infos.clear();
        this.infos.putAll(infos);
    }

    public void setPQLog(String log) {
        getPlayers().parallelStream().forEach(p -> p.setPQLog(log));
    }

    public void setEventCount(String log) {
        getPlayers().parallelStream().forEach(p -> p.setEventCount(log));
    }

    public void sendMarriedDone() {
        this.broadcastPacket(MaplePacketCreator.sendMarriedDone());
    }

    public void showEffect(String effect) {
        getPlayers().parallelStream().forEach(p -> p.getClient().announce(MaplePacketCreator.showEffect(effect)));
    }

    public void updateInfoQuest(int questid, String data) {
        getPlayers().parallelStream().forEach(p -> p.updateInfoQuest(questid, data));
    }

    public void updateOneInfo(int questid, String key, String info) {
        getPlayers().parallelStream().forEach(p -> p.updateOneInfo(questid, key, info));
    }
}
