package scripting;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.MapleTraitType;
import client.inventory.*;
import client.skills.Skill;
import client.skills.SkillFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.channel.handler.InterServerHandler;
import handling.opcode.EffectOpcode;
import handling.world.WorldBroadcastService;
import handling.world.WorldGuildService;
import handling.world.WorldPartyService;
import handling.world.guild.MapleGuild;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import scripting.event.EventInstanceManager;
import scripting.event.EventManager;
import scripting.event.EventScriptManager;
import scripting.npc.NPCScriptManager;
import server.*;
import server.RankingTop.CharNameAndId;
import server.Timer;
import server.events.MapleDojoAgent;
import server.events.MapleEvent;
import server.events.MapleEventType;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import tools.*;
import tools.packet.*;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.util.*;
import java.util.List;

public abstract class AbstractPlayerInteraction {

    private static final Logger log = LogManager.getLogger(AbstractPlayerInteraction.class);
    private static final Map<Pair, MapleNPC> npcs = new WeakHashMap<>();
    protected final WeakReference c;
    protected final int id;
    protected final String id2;

    public AbstractPlayerInteraction(MapleClient c) {
        this.c = new WeakReference<>(c);
        this.id = 0;
        this.id2 = "";
    }

    public AbstractPlayerInteraction(MapleClient c, int id, String id2) {
        this.c = new WeakReference<>(c);
        this.id = id;
        this.id2 = id2;
    }

    /*
     * 获取连接
     */
    public MapleClient getClient() {
        return (MapleClient) c.get();
    }

    public MapleClient getC() {
        return (MapleClient) c.get();
    }

    /*
     * 获取角色
     */
    public MapleCharacter getChar() {
        return getClient().getPlayer();
    }

    public MapleCharacter getPlayer() {
        return getClient().getPlayer();
    }

    public int getPosX() {
        return getClient().getPlayer().getTruePosition().x;
    }

    public int getPosY() {
        return getClient().getPlayer().getTruePosition().y;
    }

    /*
     * 获取频道
     */
    public ChannelServer getChannelServer() {
        return getClient().getChannelServer();
    }

    public EventManager getEventManager(String event) {
        return getClient().getChannelServer().getEventSM().getEventManager(event);
    }

    public EventInstanceManager getEventInstance() {
        return getClient().getPlayer().getEventInstance();
    }

    /*
     * 角色地图传送
     */
    public void warp(int mapId) {
        MapleMap mapz = getWarpMap(mapId);
        if (mapz == null) {
            playerMessage(1, "不存在的地图ID:" + mapId + "\r\n请联系管理员删除该地图传送");
            return;
        }
        try {
            getClient().getPlayer().changeMap(mapz, mapz.getPortalSP().get(nextInt(mapz.getPortalSP().size())));
        } catch (Exception e) {
            getClient().getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public void warp_Instanced(int mapId) {
        MapleMap mapz = getMap_Instanced(mapId);
        try {
            getClient().getPlayer().changeMap(mapz, mapz.getPortalSP().get(nextInt(mapz.getPortalSP().size())));
        } catch (Exception e) {
            getClient().getPlayer().changeMap(mapz, mapz.getPortal(0));
        }
    }

    public void warp(int mapId, int portal) {
        MapleMap mapz = getWarpMap(mapId);
        if (portal != 0 && mapId == getClient().getPlayer().getMapId()) { //test
            Point portalPos = new Point(getClient().getPlayer().getMap().getPortal(portal).getPosition());
            if (portalPos.distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                instantMapWarp((byte) portal); //until we get packet for far movement, this will do
                getPlayer().checkFollow();
                getMap().movePlayer(getClient().getPlayer(), portalPos);
            } else {
                getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void warpS(int mapId, int portal) {
        MapleMap mapz = getWarpMap(mapId);
        getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public void warp(int mapId, String portal) {
        MapleMap mapz = getWarpMap(mapId);
        if (mapId == 109060000 || mapId == 109060002 || mapId == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        if (mapId == getClient().getPlayer().getMapId()) { //test
            MaplePortal portalPos = getClient().getPlayer().getMap().getPortal(portal);
            if (portalPos != null && portalPos.getPosition().distanceSq(getPlayer().getTruePosition()) < 90000.0) { //estimation
                getClient().getPlayer().checkFollow();
                instantMapWarp((byte) portalPos.getId());
                getClient().getPlayer().getMap().movePlayer(getClient().getPlayer(), portalPos.getPosition());
            } else {
                getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
            }
        } else {
            getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
        }
    }

    public void warpS(int mapId, String portal) {
        MapleMap mapz = getWarpMap(mapId);
        if (mapId == 109060000 || mapId == 109060002 || mapId == 109060004) {
            portal = mapz.getSnowballPortal();
        }
        getClient().getPlayer().changeMap(mapz, mapz.getPortal(portal));
    }

    public void warpMap(int mapId, int portal) {
        MapleMap map = getMap(mapId);
        for (MapleCharacter chr : getClient().getPlayer().getMap().getCharactersThreadsafe()) {
            chr.changeMap(map, map.getPortal(portal));
        }
    }

    public void playPortalSE() {
        getClient().announce(EffectPacket.showOwnBuffEffect(0, EffectOpcode.UserEffect_PlayPortalSE.getValue(), 1, 1));
    }

    private MapleMap getWarpMap(int mapId) {
        return ChannelServer.getInstance(getClient().getChannel()).getMapFactory().getMap(mapId);
    }

    public MapleMap getMap() {
        return getClient().getPlayer().getMap();
    }

    public MapleMap getMap(int mapId) {
        return getWarpMap(mapId);
    }

    public MapleMap getMap_Instanced(int mapId) {
        return getClient().getPlayer().getEventInstance() == null ? getMap(mapId) : getClient().getPlayer().getEventInstance().getMapInstance(mapId);
    }

    /*
     * 刷出怪物
     * 通过修改怪物的等级来刷怪
     */
    public void spawnMobLevel(int mobId, int level) {
        spawnMobLevel(mobId, 1, level, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobLevel(int mobId, int quantity, int level) {
        spawnMobLevel(mobId, quantity, level, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobLevel(int mobId, int quantity, int level, int x, int y) {
        spawnMobLevel(mobId, quantity, level, new Point(x, y));
    }

    public void spawnMobLevel(int mobId, int quantity, int level, Point pos) {
        for (int i = 0; i < quantity; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(mobId);
            if (mob == null || !mob.getStats().isChangeable()) {
                if (getClient().getPlayer().isAdmin()) {
                    getClient().getPlayer().dropMessage(-11, "[系统提示] spawnMobLevel召唤怪物出错，ID为: " + mobId + " 怪物不存在或者该怪物无法使用这个函数来改变怪物的属性！");
                }
                continue;
            }
            mob.changeLevel(level, false);
            getClient().getPlayer().getMap().spawnMonsterOnGroundBelow(mob, pos);
        }
    }

    /*
     * 自定义改变怪物的血和经验
     */
    public void spawnMobStats(int mobId, long newhp, int newExp) {
        spawnMobStats(mobId, 1, newhp, newExp, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobStats(int mobId, int quantity, long newhp, int newExp) {
        spawnMobStats(mobId, quantity, newhp, newExp, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobStats(int mobId, int quantity, long newhp, int newExp, int x, int y) {
        spawnMobStats(mobId, quantity, newhp, newExp, new Point(x, y));
    }

    public void spawnMobStats(int mobId, int quantity, long newhp, int newExp, Point pos) {
        for (int i = 0; i < quantity; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(mobId);
            if (mob == null) {
                if (getClient().getPlayer().isAdmin()) {
                    getClient().getPlayer().dropMessage(-11, "[系统提示] spawnMobStats召唤怪物出错，ID为: " + mobId + " 怪物不存在！");
                }
                continue;
            }
            OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, mob.getMobMaxMp(), newExp <= 0 ? mob.getMobExp() : newExp, false);
            mob.setOverrideStats(overrideStats);
            getClient().getPlayer().getMap().spawnMonsterOnGroundBelow(mob, pos);
        }
    }

    /*
     * 按倍数计算刷怪
     */
    public void spawnMobMultipler(int mobId, int multipler) {
        spawnMobMultipler(mobId, 1, multipler, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobMultipler(int mobId, int quantity, int multipler) {
        spawnMobMultipler(mobId, quantity, multipler, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobMultipler(int mobId, int quantity, int multipler, int x, int y) {
        spawnMobMultipler(mobId, quantity, multipler, new Point(x, y));
    }

    public void spawnMobMultipler(int mobId, int quantity, int multipler, Point pos) {
        for (int i = 0; i < quantity; i++) {
            MapleMonster mob = MapleLifeFactory.getMonster(mobId);
            if (mob == null) {
                if (getClient().getPlayer().isAdmin()) {
                    getClient().getPlayer().dropMessage(-11, "[系统提示] spawnMobMultipler召唤怪物出错，ID为: " + mobId + " 怪物不存在！");
                }
                continue;
            }
            OverrideMonsterStats overrideStats = new OverrideMonsterStats(mob.getMobMaxHp() * multipler, mob.getMobMaxMp() * multipler, mob.getMobExp() + (multipler * 100), false);
            mob.setOverrideStats(overrideStats);
            getClient().getPlayer().getMap().spawnMonsterOnGroundBelow(mob, pos);
        }
    }

    /*
     * 普通刷怪
     */
    public void spawnMonster(int mobId, int quantity) {
        spawnMob(mobId, quantity, getClient().getPlayer().getTruePosition());
    }

    public void spawnMobOnMap(int mobId, int quantity, int x, int y, int map) {
        for (int i = 0; i < quantity; i++) {
            getMap(map).spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(x, y));
        }
    }

    public void spawnMob(int mobId, int quantity, int x, int y) {
        spawnMob(mobId, quantity, new Point(x, y));
    }

    public void spawnMob(int mobId, int x, int y) {
        spawnMob(mobId, 1, new Point(x, y));
    }

    private void spawnMob(int mobId, int quantity, Point pos) {
        for (int i = 0; i < quantity; i++) {
            getClient().getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), pos);
        }
    }

    /*
     * 通过怪物ID杀死地图怪物
     */
    public void killMob(int mobId) {
        getClient().getPlayer().getMap().killMonster(mobId);
    }

    /*
     * 杀死地图里面所有怪物
     */
    public void killAllMob() {
        getClient().getPlayer().getMap().killAllMonsters(true);
    }

    /*
     * 改变角色血量
     */
    public void addHP(int delta) {
        getClient().getPlayer().addHP(delta);
    }

    public int getPlayerStat(String type) {
        switch (type) {
            case "LVL":
                return getClient().getPlayer().getLevel();
            case "STR":
                return getClient().getPlayer().getStat().getStr();
            case "DEX":
                return getClient().getPlayer().getStat().getDex();
            case "INT":
                return getClient().getPlayer().getStat().getInt();
            case "LUK":
                return getClient().getPlayer().getStat().getLuk();
            case "HP":
                return getClient().getPlayer().getStat().getHp();
            case "MP":
                return getClient().getPlayer().getStat().getMp();
            case "MAXHP":
                return getClient().getPlayer().getStat().getMaxHp();
            case "MAXMP":
                return getClient().getPlayer().getStat().getMaxMp();
            case "RAP":
                return getClient().getPlayer().getRemainingAp();
            case "RSP":
                return getClient().getPlayer().getRemainingSp();
            case "GID":
                return getClient().getPlayer().getGuildId();
            case "GRANK":
                return getClient().getPlayer().getGuildRank();
            case "ARANK":
                return getClient().getPlayer().getAllianceRank();
            case "GM":
                return getClient().getPlayer().isGM() ? 1 : 0;
            case "ADMIN":
                return getClient().getPlayer().isAdmin() ? 1 : 0;
            case "GENDER":
                return getClient().getPlayer().getGender();
            case "FACE":
                return getClient().getPlayer().getFace();
            case "HAIR":
                return getClient().getPlayer().getHair();
        }
        return -1;
    }

    /*
     * 获取安卓的属性
     */
    public int getAndroidStat(String type) {
        switch (type) {
            case "HAIR":
                return getClient().getPlayer().getAndroid().getHair();
            case "FACE":
                return getClient().getPlayer().getAndroid().getFace();
            case "SKIN":
                return getClient().getPlayer().getAndroid().getSkin();
            case "GENDER":
                return getClient().getPlayer().getAndroid().getGender();
        }
        return -1;
    }

    /*
     * 获取角色名字
     */
    public String getName() {
        return getClient().getPlayer().getName();
    }

    /*
     * 获取服务器的名称
     */
    public String getServerName() {
        return getClient().getPlayer().getClient().getChannelServer().getServerName();
    }

    /*
     * 获取服务器的名称 只显示前2个字的名字
     */
    public String getTrueServerName() {
        return getClient().getPlayer().getClient().getChannelServer().getTrueServerName();
    }

    /*
     * 检测是否拥有道具
     */
    public boolean haveItem(int itemId) {
        return haveItem(itemId, 1);
    }

    public boolean haveItem(int itemId, int quantity) {
        return haveItem(itemId, quantity, false, true);
    }

    public boolean haveItem(int itemId, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        return getClient().getPlayer().haveItem(itemId, quantity, checkEquipped, greaterOrEquals);
    }

    /*
     * 新增变量获取玩家背包道具的数量
     */
    public int getItemQuantity(int itemId) {
        return getClient().getPlayer().getItemQuantity(itemId);
    }

    public boolean canHold() {
        return getClient().getPlayer().canHold();
    }

    public boolean canHoldSlots(int slot) {
        return getClient().getPlayer().canHoldSlots(slot);
    }

    public boolean canHold(int itemId) {
        return getClient().getPlayer().canHold(itemId);
    }

    public boolean canHold(int itemId, int quantity) {
        return MapleInventoryManipulator.checkSpace(getClient(), itemId, quantity, "");
    }

    /*
     * 任务相关
     */
    public MapleQuestStatus getQuestRecord(int questId) {
        return getClient().getPlayer().getQuestNAdd(MapleQuest.getInstance(questId));
    }

    public MapleQuestStatus getQuestNoRecord(int questId) {
        return getClient().getPlayer().getQuestNoAdd(MapleQuest.getInstance(questId));
    }

    public byte getQuestStatus(int questId) {
        return getClient().getPlayer().getQuestStatus(questId);
    }

    public boolean isQuestActive(int questId) {
        return getQuestStatus(questId) == 1;
    }

    public boolean isQuestFinished(int questId) {
        return getQuestStatus(questId) == 2;
    }

    public void showQuestMsg(String msg) {
        getClient().announce(MaplePacketCreator.showQuestMsg(msg));
    }

    public void forceStartQuest(int questId, String data) {
        MapleQuest.getInstance(questId).forceStart(getClient().getPlayer(), 0, data);
    }

    public void forceStartQuest(int questId, int data, boolean filler) {
        MapleQuest.getInstance(questId).forceStart(getClient().getPlayer(), 0, filler ? String.valueOf(data) : null);
    }

    public void forceStartQuest(int questId) {
        MapleQuest.getInstance(questId).forceStart(getClient().getPlayer(), 0, null);
    }

    public void forceCompleteQuest(int questId) {
        MapleQuest.getInstance(questId).forceComplete(getPlayer(), 0);
    }

    /*
     * 刷出NPC
     */
    public void spawnNpc(int npcId) {
        getClient().getPlayer().getMap().spawnNpc(npcId, getClient().getPlayer().getPosition());
    }

    public void spawnNpc(int npcId, int x, int y) {
        getClient().getPlayer().getMap().spawnNpc(npcId, new Point(x, y));
    }

    public void spawnNpc(int npcId, Point pos) {
        getClient().getPlayer().getMap().spawnNpc(npcId, pos);
    }

    /*
     * 移除NPC
     */
    public void removeNpc(int mapid, int npcId) {
        getClient().getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId);
    }

    public void removeNpcforQ(int mapid, int npcId) {
        getChannelServer().getMapFactory().getMap(mapid).removeNpc(npcId, getChar().getId());
    }

    public void removeNpc(int npcId) {
        getClient().getPlayer().getMap().removeNpc(npcId);
    }

    public void forceStartReactor(int mapId, int reactorId) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapId);
        MapleReactor react;

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == reactorId) {
                react.forceStartReactor(getClient());
                break;
            }
        }
    }

    public void destroyReactor(int mapId, int reactorId) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapId);
        MapleReactor react;

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == reactorId) {
                react.hitReactor(getClient());
                break;
            }
        }
    }

    public void hitReactor(int mapId, int reactorId) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapId);
        MapleReactor react;

        for (MapleMapObject remo : map.getAllReactorsThreadsafe()) {
            react = (MapleReactor) remo;
            if (react.getReactorId() == reactorId) {
                react.hitReactor(getClient());
                break;
            }
        }
    }

    public void forceTrigger(int mapId, int reactorId) {
        MapleMap map = getClient().getChannelServer().getMapFactory().getMap(mapId);
        for (MapleReactor reactor : map.getAllReactorsThreadsafe()) {
            if (reactor.getReactorId() == reactorId) {
                reactor.setState((byte) 1);
                reactor.forceTrigger();
                break;
            }
        }
    }

    /*
     * 获取角色的职业ID
     */
    public int getJob() {
        return getClient().getPlayer().getJob();
    }

    /*
     * 获取角色的职业ID
     */
    public int getJobId() {
        return getJob();
    }

    public int getBeginner() {
        return JobConstants.getBeginner((short) getJob());
    }

    public int getTrueJobGrade() {
        return JobConstants.getTrueJobGrade(getJob());
    }

    /*
     * 通过职业ID获取职业名字
     */
    public String getJobName(int jobId) {
        return JobConstants.getJobNameById(jobId);
    }

    /*
     * 检测角色是否是新手职业
     */
    public boolean isBeginnerJob() {
        return JobConstants.is新手职业(getJob()) && getLevel() < 11;
    }

    /*
     * 检测角色是否是骑士团职业
     */
    public boolean is骑士团() {
        return JobConstants.is骑士团(getJob());
    }

    /*
     * 获取角色当前的等级
     */
    public int getLevel() {
        return getClient().getPlayer().getLevel();
    }

    public void getLevelup() {
        getPlayer().levelUp();
    }

    /*
     * 获取角色当前的人气点数
     */
    public int getFame() {
        return getClient().getPlayer().getFame();
    }

    /*
     * 加减角色人气和更新角色人气
     */
    public void gainFame(int famechange) {
        gainFame(famechange, false);
    }

    public void gainFame(int famechange, boolean show) {
        getClient().getPlayer().gainFame(famechange, show);
    }

    /*
     * 玩家点卷和抵用卷函数
     */
    public int getNX(int type) {
        return getClient().getPlayer().getCSPoints(type);
    }

    public boolean gainNX(int amount) {
        return getClient().getPlayer().modifyCSPoints(1, amount, true);
    }

    public boolean gainNX(int type, int amount) {
        if (type <= 0 || type > 2) {
            type = 2;
        }
        return getClient().getPlayer().modifyCSPoints(type, amount, true);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param period   时间小于 1000 按天算 大于使用给的时候计算的默认时间
     */
    public void gainItemPeriod(int itemId, short quantity, long period) {
        gainItem(itemId, quantity, false, period, -1, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param period   时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param owner    道具上面带角色名字
     */
    public void gainItemPeriod(int itemId, short quantity, long period, String owner) {
        gainItem(itemId, quantity, false, period, -1, owner, 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     */
    public void gainItem(int itemId, short quantity) {
        gainItem(itemId, quantity, false, 0, -1, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param state    是否有潜能
     */
    public void gainItem(int itemId, short quantity, int state) {
        gainItem(itemId, quantity, false, 0, -1, "", state);
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     */
    public void gainItem(int itemId, short quantity, boolean randomStats) {
        gainItem(itemId, quantity, randomStats, 0, -1, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param owner    道具上面带角色名字
     */
    public void gainItem(int itemId, short quantity, String owner) {
        gainItem(itemId, quantity, false, 0, -1, owner, 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param owner    道具上面带角色名字
     */
    public void gainItem(int itemId, short quantity, String owner, int state) {
        gainItem(itemId, quantity, false, 0, -1, owner, state);
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     * @param slots       设置道具的可升级次数
     */
    public void gainItem(int itemId, short quantity, boolean randomStats, int slots) {
        gainItem(itemId, quantity, randomStats, 0, slots, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param period   时间小于 1000 按天算 大于使用给的时候计算的默认时间
     */
    public void gainItem(int itemId, short quantity, long period) {
        gainItem(itemId, quantity, false, period, -1, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param state    潜能状态
     */
    public void gainItemByState(int itemId, short quantity, int state) {
        gainItem(itemId, quantity, false, 0, -1, "", state);
    }

    /**
     * 给玩家道具
     *
     * @param itemId   道具ID
     * @param quantity 道具数量
     * @param period   时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param state    是否有潜能
     */
    public void gainItem(int itemId, short quantity, long period, int state) {
        gainItem(itemId, quantity, false, period, -1, "", state);
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     * @param period      时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param slots       设置道具的可升级次数
     */
    public void gainItem(int itemId, short quantity, boolean randomStats, long period, int slots) {
        gainItem(itemId, quantity, randomStats, period, slots, "", 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     * @param period      时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param slots       设置道具的可升级次数
     * @param owner       道具上面带角色名字
     */
    public void gainItem(int itemId, short quantity, boolean randomStats, long period, int slots, String owner) {
        gainItem(itemId, quantity, randomStats, period, slots, owner, 0);
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     * @param period      时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param slots       设置道具的可升级次数
     * @param owner       道具上面带角色名字
     * @param state       是否带潜能
     */
    public void gainItem(int itemId, short quantity, boolean randomStats, long period, int slots, String owner, int state) {
        gainItem(itemId, quantity, randomStats, period, slots, owner, state, getClient());
    }

    /**
     * 给玩家道具
     *
     * @param itemId      道具ID
     * @param quantity    道具数量
     * @param randomStats 是否随机属性
     * @param period      时间小于 1000 按天算 大于使用给的时候计算的默认时间
     * @param slots       设置道具的可升级次数
     * @param owner       道具上面带角色名字
     * @param state       是否带潜能
     * @param cg          当前角色的连接(MapleClient对象)
     */
    public void gainItem(int itemId, short quantity, boolean randomStats, long period, int slots, String owner, int state, MapleClient cg) {
        if (ItemConstants.isLogItem(itemId)) {
            String itemText = "玩家 " + StringUtil.getRightPaddedStr(cg.getPlayer().getName(), ' ', 13) + (quantity >= 0 ? " 获得道具: " : " 失去道具: ") + itemId + " 数量: " + StringUtil.getRightPaddedStr(String.valueOf(Math.abs(quantity)), ' ', 5) + " 道具名字: " + getItemName(itemId);
            log.info("[物品] " + itemText);
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + itemText));
        }
        if (quantity >= 0) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            MapleInventoryType type = ItemConstants.getInventoryType(itemId);
            if (!MapleInventoryManipulator.checkSpace(cg, itemId, quantity, "")) {
                return;
            }
            if (type.equals(MapleInventoryType.EQUIP) && !ItemConstants.is飞镖道具(itemId) && !ItemConstants.is子弹道具(itemId)) {
                Equip item = (Equip) (randomStats ? ii.randomizeStats((Equip) ii.getEquipById(itemId)) : ii.getEquipById(itemId));
                if (period > 0) {
                    if (period < 1000) {
                        item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
                    } else {
                        item.setExpiration(System.currentTimeMillis() + period);
                    }
                }
                if (slots > 0) {
                    item.setUpgradeSlots((byte) (item.getUpgradeSlots() + slots));
                }
                if (state > 0) {
                    int newstate = 16 + state;
                    if (newstate > 20 || newstate < 17) {
                        newstate = 17;
                    }
                    item.setPotential1(-newstate);
                }
                if (owner != null) {
                    item.setOwner(owner);
                }
                item.setGMLog("脚本获得 " + this.id + " (" + this.id2 + ") 地图: " + cg.getPlayer().getMapId() + " 时间: " + DateUtil.getNowTime());
                String name = ii.getName(itemId);
                if (itemId / 10000 == 114 && name != null && name.length() > 0) { //获得勋章道具
                    String msg = "恭喜您获得勋章 <" + name + ">";
                    cg.getPlayer().dropMessage(-1, msg);
                    cg.getPlayer().dropMessage(5, msg);
                }
                MapleInventoryManipulator.addbyItem(cg, item.copy());
            } else {
                MapleInventoryManipulator.addById(cg, itemId, quantity, owner == null ? "" : owner, null, period, "脚本获得 " + this.id + " (" + this.id2 + ") 地图: " + cg.getPlayer().getMapId() + " 时间: " + DateUtil.getNowTime());
            }
        } else {
            MapleInventoryManipulator.removeById(cg, ItemConstants.getInventoryType(itemId), itemId, -quantity, true, false);
        }
        cg.announce(MaplePacketCreator.getShowItemGain(itemId, quantity, true));
    }

    /**
     * 移除角色道具 数量为: 1
     *
     * @param itemId 道具ID
     */
    public boolean removeItem(int itemId) { //quantity 1
        if (MapleInventoryManipulator.removeById_Lock(getClient(), ItemConstants.getInventoryType(itemId), itemId)) {
            getClient().announce(MaplePacketCreator.getShowItemGain(itemId, (short) -1, true));
            return true;
        }
        return false;
    }

    public void removeAllItem(int type) {
        MapleInventoryManipulator.removeAll(getClient(), this.getInvType(type));
    }

    /*
     * 给玩家道具并且装备上该道具
     */
    public void gainItemAndEquip(int itemId, short slot) {
        MapleInventoryManipulator.addItemAndEquip(getClient(), itemId, slot);
    }

    public void gainLockItem(int itemId, short quantity, boolean lock, long period) {
        gainLockItem(itemId, quantity, lock, period, "");
    }

    public void gainLockItem(int itemId, short quantity, boolean lock, long period, String from) {
        gainLockItem(itemId, quantity, lock, period, from, true);
    }

    public void gainLockItem(int itemId, short quantity, boolean lock, long period, String from, boolean broad) {
        if (quantity <= 0) {
            if (getClient().getPlayer().isAdmin()) {
                getClient().getPlayer().dropMessage(5, "输入的数量错误，数量必须大于0.如果是装备道具不管设置多少都只给1个.");
            }
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!ii.itemExists(itemId)) {
            if (getClient().getPlayer().isAdmin()) {
                getClient().getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
            }
            return;
        }
        if (!MapleInventoryManipulator.checkSpace(getClient(), itemId, quantity, "")) {
            if (getClient().getPlayer().isAdmin()) {
                getClient().getPlayer().dropMessage(5, "背包空间不足.");
            }
            return;
        }
        Item item;
        if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
            item = ii.getEquipById(itemId);
        } else {
            item = new Item(itemId, (byte) 0, quantity, (byte) 0);
        }
        if (lock) {
            item.addFlag((short) ItemFlag.封印.getValue());
        }
        if (period > 0) {
            if (period < 1000) {
                item.setExpiration(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
            } else {
                item.setExpiration(System.currentTimeMillis() + period);
            }
        }
        if (!from.equals("")) {
            item.setGMLog("从" + from + "中获得 时间: " + DateUtil.getNowTime());
        }
        MapleInventoryManipulator.addbyItem(getClient(), item);
        getClient().announce(MaplePacketCreator.getShowItemGain(itemId, quantity, true));
        if (!from.equals("") && broad) {
            if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(getClient().getPlayer().getName() + " : 从" + from + "中获得" + ii.getName(itemId) + "！大家一起恭喜他（她）吧！！！！", false, getClient().getChannel(), item));
            } else {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getClient().getPlayer().getName(), " : 从" + from + "中获得{" + ii.getName(item.getItemId()) + "}！大家一起恭喜他（她）吧！！！！", item, (byte) 3, getClient().getChannel()));
            }
        }
    }

    public void worldMessageItem(String message, Item item) {
        WorldBroadcastService.getInstance().broadcastSmega(MaplePacketCreator.itemMegaphone(message, false, getClient().getChannel(), item));
    }

    public final void worldMessageYellow(final String message) {
        WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(9, getClient().getChannel(), message, true));
    }

    public final void worldMessageEffect(final String message, final int type, final int time) {
        WorldBroadcastService.getInstance().broadcastMessage(UIPacket.getMapEffectMsg(message, type, time));
    }

    public final void worldBrodcastEffect(final int itemid, final String message) {
        if (itemid > 0) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.startMapEffect(message, itemid, true));
        } else {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.environmentChange(message, 4));
        }

        Timer.WorldTimer.getInstance().schedule(() -> WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.removeMapEffect()), 20000);  //20秒
    }

    /*
     * 改变角色当前地图的音乐
     */
    public void changeMusic(String songName) {
        getMap().broadcastMessage(MaplePacketCreator.musicChange(songName));
    }

    /*
     * 发送全频道公告
     */
    public void channelMessage(int type, String message) {
        getClient().getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(type, getClient().getChannel(), message));
    }

    /*
     * 发送全服公告
     */
    public void worldMessage(String message) {
        worldMessage(6, message);
    }

    public void laba(int type, String message) {
        worldMessage(type, message);
    }

    public void worldMessage(int type, String message) {
        WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    public void worldSpouseMessage(int type, String message) {
        if (type == 0x00 || type == 0x01 || (type >= 0x06 && type <= 0x2A)) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.spouseMessage(type, message));
        } else {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, message));
        }
    }

    /*
     * 发送给角色信息 默认为类型 5
     */
    public void playerMessage(String message) {
        playerMessage(5, message);
    }

    /*
     * 发送给地图信息 默认为类型 5
     */
    public void mapMessage(String message) {
        mapMessage(5, message);
    }

    /*
     * 发送给家族信息 默认为类型 5
     */
    public void guildMessage(String message) {
        guildMessage(5, message);
    }

    /**
     * 给玩家发送消息
     *
     * @param type    消息类型
     * @param message 消息内容
     */
    public void playerMessage(int type, String message) {
        getClient().getPlayer().dropMessage(type, message);
    }

    /**
     * 给玩家发送消息
     *
     * @param type    消息类型
     * @param message 消息内容
     */
    public void dropMessage(int type, String message) {
        getClient().getPlayer().dropMessage(type, message);
    }

    /**
     * 给当前地图的所有玩家发送信息
     *
     * @param type    消息类型
     * @param message 消息内容
     */
    public void mapMessage(int type, String message) {
        getClient().getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(type, message));
    }

    /**
     * 发送给家族信息
     *
     * @param type    消息类型
     * @param message 消息内容
     */
    public void guildMessage(int type, String message) {
        if (getPlayer().getGuildId() > 0) {
            WorldGuildService.getInstance().guildPacket(getPlayer().getGuildId(), MaplePacketCreator.serverNotice(type, message));
        }
    }

    /**
     * 顶部公告
     *
     * @param message 消息内容
     */
    public void topMessage(String message) {
        getClient().announce(UIPacket.getTopMsg(message));
    }

    public void topMsg(String string) {
        topMessage(string);
    }

    /**
     * 获取角色家族对象
     *
     * @return
     */
    public MapleGuild getGuild() {
        return getGuild(getPlayer().getGuildId());
    }

    /*
     * 通过家族ID获取家族
     */
    public MapleGuild getGuild(int guildid) {
        return WorldGuildService.getInstance().getGuild(guildid);
    }

    /*
     * 获得所有家族
    */
    public List<Pair<Integer, MapleGuild>> getGuildList() {
        return WorldGuildService.getInstance().getGuildList();
    }

    /*
     * 获取角色组队
     */
    public MapleParty getParty() {
        return getClient().getPlayer().getParty();
    }

    public int getPartySize() {
        return getParty() != null ? getParty().getMembers().size() : -1;
    }

    public int getPartyID() {
        return getParty() != null ? getParty().getPartyId() : -1;
    }

    public boolean isChrInParty(int cid) {
        return getParty() != null && getParty().getMemberById(cid) != null;
    }

    public int getCurrentPartyId(int mapId) {
        return getMap(mapId).getCurrentPartyId();
    }

    public int getPartyAverageLevel() {
        return getParty().getAverageLevel();
    }

    /*
     * 检测角色是否是队长
     */
    public boolean isLeader() {
        return getPlayer().getParty() != null && getParty().getLeader().getId() == getClient().getPlayer().getId();
    }

    public void partyMessage(int partyId, String string) {
        WorldPartyService.getInstance().partyMessage(partyId, string);
    }

    public boolean isAllPartyMembersAllowedJob(int jobId) {
        if (getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter mem : getClient().getPlayer().getParty().getMembers()) {
            if (mem.getJobId() / 100 != jobId) {
                return false;
            }
        }
        return true;
    }

    public final boolean isAllPartyMembersAllowedLevel(int min, int max) {
        if (getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter d2 : getParty().getMembers()) {
            if (d2.getLevel() >= min && d2.getLevel() <= max) continue;
            return false;
        }
        return true;
    }

    public final boolean isAllPartyMembersNotCoolDown(int questID, int coolDownTime) {
        return getParty() != null && this.getIsInCoolDownMember(questID, coolDownTime) == null;
    }

    public final String getIsInCoolDownMemberName(int questID, int coolDownTime) {
        MaplePartyCharacter d2 = this.getIsInCoolDownMember(questID, coolDownTime);
        return d2 != null ? d2.getName() : null;
    }

    public final MaplePartyCharacter getIsInCoolDownMember(int questID, int coolDownTime) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
                if (player == null) {
                    return partyCharacter;
                }
                MapleQuestStatus status = player.getQuestNAdd(MapleQuest.getInstance(questID));
                if (status == null || status.getCustomData() == null || Long.valueOf(status.getCustomData()) + (long) coolDownTime <= System.currentTimeMillis())
                    continue;
                return partyCharacter;
            }
        }
        return null;
    }

    public String getCustomData(int questid) {
        return getPlayer().getQuestNAdd(MapleQuest.getInstance(questid)).getCustomData();
    }

    public void setCustomData(int questid, String customdata) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(questid)).setCustomData(customdata);
    }

    public final boolean isAllPartyMembersHaveItem(int itemId, int quantity) {
        if (getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
            MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
            if (player != null && player.getItemQuantity(itemId) >= quantity) continue;
            return false;
        }
        return true;
    }

    public final String getNotHaveItemMemberName(int itemId, int quantity) {
        MaplePartyCharacter partyCharacter = this.getNotHaveItemMember(itemId, quantity);
        return partyCharacter != null ? partyCharacter.getName() : null;
    }

    public final MaplePartyCharacter getNotHaveItemMember(int itemId, int quantity) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
                if (player != null && player.getItemQuantity(itemId) >= quantity) continue;
                return partyCharacter;
            }
        }
        return null;
    }

    public final boolean isAllPartyMembersAllowedPQ(String pqName, int times) {
        return this.isAllPartyMembersAllowedPQ(pqName, times, 1);
    }

    public final boolean isAllPartyMembersAllowedPQ(String pqName, int times, int day) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
                if (player != null && getDaysPQLog(pqName, day) < times) continue;
                return false;
            }
        }
        return true;
    }

    public final MaplePartyCharacter getNotAllowedPQMember(String pqName, int times, int day) {
        if (getParty() == null) {
            return null;
        }
        for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
            MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
            if (player != null && getDaysPQLog(pqName, day) < times) continue;
            return partyCharacter;
        }
        return null;
    }

    public final String getNotAllowedPQMemberName(String pqName, int times) {
        return this.getNotAllowedPQMemberName(pqName, times, 1);
    }

    public final String getNotAllowedPQMemberName(String string, int times, int day) {
        if (this.getNotAllowedPQMember(string, times, day) != null) {
            return this.getNotAllowedPQMember(string, times, day).getName();
        }
        return null;
    }

    public final void gainMembersPQ(String pqName, int num) {
        if (getParty() == null) {
            return;
        }
        for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
            MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
            if (player == null) continue;
            player.setPQLog(pqName, 0, num);
        }
    }

    public int getDaysPQLog(String pqName, int days) {
        return getPlayer().getDaysPQLog(pqName, 0, days);
    }

    public int getPQLog(String pqName) {
        return getPlayer().getPQLog(pqName);
    }

    public int getPQLog(String pqName, int type) {
        return getPlayer().getPQLog(pqName, type);
    }

    public int getPQLog(String pqName, int type, int days) {
        return getPlayer().getDaysPQLog(pqName, type, days);
    }

    public void setPQLog(String pqName) {
        getPlayer().setPQLog(pqName);
    }

    public void setPQLog(String pqName, int type) {
        getPlayer().setPQLog(pqName, type);
    }

    public void setPQLog(String pqName, int type, int count) {
        getPlayer().setPQLog(pqName, type, count);
    }

    public void resetPQLog(String pqName) {
        getPlayer().resetPQLog(pqName);
    }

    public void resetPQLog(String pqName, int type) {
        getPlayer().resetPQLog(pqName, type);
    }

    public void setPartyPQLog(String pqName) {
        this.setPartyPQLog(pqName, 0);
    }

    public void setPartyPQLog(String pqName, int type) {
        this.setPartyPQLog(pqName, type, 1);
    }

    public void setPartyPQLog(String pqName, int type, int count) {
        if (this.getPlayer().getParty() == null || this.getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().setPQLog(pqName, type, count);
            return;
        }
        int n4 = this.getPlayer().getMapId();
        for (MaplePartyCharacter partyCharacter : this.getPlayer().getParty().getMembers()) {
            MapleCharacter player = this.getPlayer().getMap().getCharacterById(partyCharacter.getId());
            if (player == null || player.getMapId() != n4) continue;
            player.setPQLog(pqName, type, count);
        }
    }

    /*
     * 检测组队成员是否都在同一地图
     */
    public boolean allMembersHere() {
        if (getClient().getPlayer().getParty() == null) {
            return false;
        }
        for (MaplePartyCharacter mem : getClient().getPlayer().getParty().getMembers()) {
            MapleCharacter chr = getClient().getPlayer().getMap().getCharacterById(mem.getId());
            if (chr == null) {
                return false;
            }
        }
        return true;
    }

    /*
     * 组队地图传送
     */
    public void warpParty(int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp(mapId, 0);
            return;
        }
        MapleMap target = getMap(mapId);
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    /*
     * 组队地图传送和传送点
     */
    public void warpParty(int mapId, int portal) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            if (portal < 0) {
                warp(mapId);
            } else {
                warp(mapId, portal);
            }
            return;
        }
        boolean rand = portal < 0;
        MapleMap target = getMap(mapId);
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                if (rand) {
                    try {
                        curChar.changeMap(target, target.getPortal(nextInt(target.getPortals().size())));
                    } catch (Exception e) {
                        curChar.changeMap(target, target.getPortal(0));
                    }
                } else {
                    curChar.changeMap(target, target.getPortal(portal));
                }
            }
        }
    }

    /*
     * 组队地图传送
     */
    public void warpParty_Instanced(int mapId) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            warp_Instanced(mapId);
            return;
        }
        MapleMap target = getMap_Instanced(mapId);

        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.changeMap(target, target.getPortal(0));
            }
        }
    }

    /*
     * 改变角色金币
     */
    public void gainMeso(long gain) {
        getClient().getPlayer().gainMeso(gain, true, true);
    }

    /*
     * 改变角色经验
     */
    public void gainExp(int gain) {
        getClient().getPlayer().gainExp(gain, true, true, true);
    }

    /*
     * 改变角色经验是否按频道经验倍数
     */
    public void gainExpR(int gain) {
        getClient().getPlayer().gainExp(gain * getClient().getChannelServer().getExpRate(), true, true, true);
    }

    /*
     * 给组队中所有角色道具
     */
    public void givePartyItems(int itemId, short quantity, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            if (quantity >= 0) {
                MapleInventoryManipulator.addById(chr.getClient(), itemId, quantity, "Received from party interaction " + itemId + " (" + this.id + ")");
            } else {
                MapleInventoryManipulator.removeById(chr.getClient(), ItemConstants.getInventoryType(itemId), itemId, -quantity, true, false);
            }
            chr.send(MaplePacketCreator.getShowItemGain(itemId, quantity, true));
        }
    }

    /*
     * 给组队中所有角色倾向系统的经验
     */
    public void addPartyTrait(String t, int e, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.getTrait(MapleTraitType.valueOf(t)).addExp(e, chr);
        }
    }

    /*
     * 给组队中所有角色倾向系统的经验
     */
    public void addPartyTrait(String t, int e) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            addTrait(t, e);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.getTrait(MapleTraitType.valueOf(t)).addExp(e, curChar);
            }
        }
    }

    /*
     * 给角色倾向系统的经验
     */
    public void addTrait(String t, int e) {
        getPlayer().getTrait(MapleTraitType.valueOf(t)).addExp(e, getPlayer());
    }

    /*
     * 给组队中所有角色道具
     */
    public void givePartyItems(int itemId, short quantity) {
        givePartyItems(itemId, quantity, false);
    }

    /*
     * 给组队中所有角色道具 是否删除道具
     */
    public void givePartyItems(int itemId, short quantity, boolean removeAll) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainItem(itemId, (short) (removeAll ? -getPlayer().itemQuantity(itemId) : quantity));
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                gainItem(itemId, (short) (removeAll ? -curChar.itemQuantity(itemId) : quantity), false, 0, 0, "", 0, curChar.getClient());
            }
        }
    }

    public void givePartyExp_PQ(int maxLevel, double mod, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            int amount = (int) Math.round(GameConstants.getExpNeededForLevel(chr.getLevel() > maxLevel ? (maxLevel + ((maxLevel - chr.getLevel()) / 10)) : chr.getLevel()) / (Math.min(chr.getLevel(), maxLevel) / 5.0) / (mod * 2.0));
            chr.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
        }
    }

    public void gainExp_PQ(int maxLevel, double mod) {
        int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
        gainExp(amount * getClient().getChannelServer().getExpRate());
    }

    public void givePartyExp_PQ(int maxLevel, double mod) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            int amount = (int) Math.round(GameConstants.getExpNeededForLevel(getPlayer().getLevel() > maxLevel ? (maxLevel + (getPlayer().getLevel() / 10)) : getPlayer().getLevel()) / (Math.min(getPlayer().getLevel(), maxLevel) / 10.0) / mod);
            gainExp(amount * getClient().getChannelServer().getExpRate());
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                int amount = (int) Math.round(GameConstants.getExpNeededForLevel(curChar.getLevel() > maxLevel ? (maxLevel + (curChar.getLevel() / 10)) : curChar.getLevel()) / (Math.min(curChar.getLevel(), maxLevel) / 10.0) / mod);
                curChar.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public void givePartyExp(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
        }
    }

    public void givePartyExp(int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainExp(amount * getClient().getChannelServer().getExpRate());
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.gainExp(amount * getClient().getChannelServer().getExpRate(), true, true, true);
            }
        }
    }

    public void givePartyNX(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.modifyCSPoints(1, amount, true);
        }
    }

    public void givePartyNX(int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            gainNX(amount);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.modifyCSPoints(1, amount, true);
            }
        }
    }

    public void endPartyQuest(int amount, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            chr.endPartyQuest(amount);
        }
    }

    public void endPartyQuest(int amount) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getPlayer().endPartyQuest(amount);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.endPartyQuest(amount);
            }
        }
    }

    public void removeFromParty(int itemId, List<MapleCharacter> party) {
        for (MapleCharacter chr : party) {
            int possesed = chr.getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId);
            if (possesed > 0) {
                MapleInventoryManipulator.removeById(getClient(), ItemConstants.getInventoryType(itemId), itemId, possesed, true, false);
                chr.send(MaplePacketCreator.getShowItemGain(itemId, (short) -possesed, true));
            }
        }
    }

    public void removeFromParty(int itemId) {
        givePartyItems(itemId, (short) 0, true);
    }

    public void useSkill(int skillId, int skillLevel) {
        if (skillLevel <= 0) {
            return;
        }
        Skill skill = SkillFactory.getSkill(skillId);
        if (skill != null) {
            skill.getEffect(skillLevel).applyTo(getClient().getPlayer());
        }
    }

    public void useItem(int itemId) {
        MapleItemInformationProvider.getInstance().getItemEffect(itemId).applyTo(getClient().getPlayer());
        getClient().announce(UIPacket.getStatusMsg(itemId));
    }

    public void cancelItem(int itemId) {
        getClient().getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(itemId), false, -1);
    }

    public int getMorphState() {
        return getClient().getPlayer().getMorphState();
    }

    public void removeAll(int itemId) {
        getClient().getPlayer().removeAll(itemId);
    }

    public void gainCloseness(int closeness, int index) {
        MaplePet pet = getPlayer().getSpawnPet(index);
        if (pet != null) {
            pet.setCloseness(pet.getCloseness() + (closeness * ServerConfig.CHANNEL_RATE_TRAIT));
            getPlayer().petUpdateStats(pet, true);
        }
    }

    public void gainClosenessAll(int closeness) {
        MaplePet[] pets = getPlayer().getSpawnPets();
        for (int i = 0; i < 3; i++) {
            if (pets[i] != null && pets[i].getSummoned()) {
                pets[i].setCloseness(pets[i].getCloseness() + closeness);
                getPlayer().petUpdateStats(pets[i], true);
            }
        }
    }

    /*
     * 重置地图
     */
    public void resetMap(int mapId) {
        getMap(mapId).resetFully();
    }

    /*
     * 打开NPC
     */
    public void openNpc(int npcId) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), npcId);
    }

    /*
     * 打开NPC 是另外的连接
     */
    public void openNpc(MapleClient cg, int npcId) {
        cg.removeClickedNPC();
        NPCScriptManager.getInstance().start(cg, npcId);
    }

    /*
     * 打开NPC和NPC模式
     */
    public void openNpc(int npcId, String npcMode) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().start(getClient(), npcId, npcMode);
    }

    /*
     * 获取地图ID
     */
    public int getMapId() {
        return getClient().getPlayer().getMap().getId();
    }

    /*
     * 检测地图是否有指定的怪物ID
     */
    public boolean haveMonster(int mobId) {
        for (MapleMapObject obj : getClient().getPlayer().getMap().getAllMonstersThreadsafe()) {
            MapleMonster mob = (MapleMonster) obj;
            if (mob.getId() == mobId) {
                return true;
            }
        }
        return false;
    }

    /*
     * 获取频道
     */
    public int getChannelNumber() {
        return getClient().getChannel();
    }

    public boolean getMonsterByID(int mobid) {
        return getMap().getMonsterById(mobid) != null;
    }

    /**
     * 获取当前中指定地图的怪物数量
     *
     * @param mapId 地图ID
     * @return
     */
    public int getMonsterCount(int mapId) {
        return getMonsterCount(mapId, -1);
    }

    /**
     * 获取当前中指定地图的怪物数量
     *
     * @param mapId 地图ID
     * @param mobid 怪物ID
     * @return
     */
    public int getMonsterCount(int mapId, int mobid) {
        return getClient().getChannelServer().getMapFactory().getMap(mapId).getNumMonsters();
    }

    /*
     * 改变技能等级 参数 技能ID 技能等级 技能最大等级
     */
    public void teachSkill(int skillId, int skilllevel, byte masterlevel) {
        getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(skillId), skilllevel, masterlevel);
    }

    /*
     * 改变技能等级 参数 技能ID 技能等级
     */
    public void teachSkill(int skillId, int skilllevel) {
        Skill skil = SkillFactory.getSkill(skillId);
        if (skil == null) {
            return;
        }
        if (getPlayer().getSkillLevel(skil) > skilllevel) {
            skilllevel = getPlayer().getSkillLevel(skil);
        }
        getPlayer().changeSingleSkillLevel(skil, skilllevel, (byte) skil.getMaxLevel());
    }

    public int getSkillLevel(int skillId) {
        return getPlayer().getSkillLevel(skillId);
    }

    /*
     * 获取当前中指定地图的角色数量
     */
    public int getPlayerCount(int mapId) {
        return getClient().getChannelServer().getMapFactory().getMap(mapId).getCharactersSize();
    }

    public void dojo_getUp() {
        getClient().announce(MaplePacketCreator.updateInfoQuest(1207, "pt=1;min=4;belt=1;tuto=1")); //todo
        getClient().announce(EffectPacket.Mulung_DojoUp2());
        instantMapWarp((byte) 6);
    }

    public void instantMapWarp(byte by2) {
        getClient().announce(MaplePacketCreator.instantMapWarp(by2));
    }

    public void curentMapWarp(int type, Point pos) {
        getClient().announce(MaplePacketCreator.instantMapWarp(type, pos));
    }

    public boolean dojoAgent_NextMap(boolean dojo, boolean fromresting) {
        if (dojo) {
            return MapleDojoAgent.warpNextMap(getPlayer(), fromresting, getPlayer().getMap());
        }
        return MapleDojoAgent.warpNextMap_Agent(getPlayer(), fromresting);
    }

    public boolean dojoAgent_NextMap(boolean dojo, boolean fromresting, int mapid) {
        if (dojo) {
            return MapleDojoAgent.warpNextMap(getPlayer(), fromresting, this.getMap(mapid));
        }
        return MapleDojoAgent.warpNextMap_Agent(getPlayer(), fromresting);
    }


    public int dojo_getPts() {
        return getClient().getPlayer().getIntNoRecord(GameConstants.DOJO);
    }

    public MapleEvent getEvent(String string) {
        return getChannelServer().getEvent(MapleEventType.getByString(string));
    }

    public boolean isEventStart(String string) {
        return getChannelServer().getEvent() == getChannelServer().getEvent(MapleEventType.getByString(string)).getMap(0).getId();
    }

    public int getSavedLocation(String loc) {
        Integer ret = getClient().getPlayer().getSavedLocation(SavedLocationType.fromString(loc));
        if (ret == -1) {
            return 100000000;
        }
        return ret;
    }

    public void saveLocation(String loc) {
        getClient().getPlayer().saveLocation(SavedLocationType.fromString(loc));
    }

    public void saveReturnLocation(String loc) {
        getClient().getPlayer().saveLocation(SavedLocationType.fromString(loc), getClient().getPlayer().getMap().getReturnMap().getId());
    }

    public void clearSavedLocation(String loc) {
        getClient().getPlayer().clearSavedLocation(SavedLocationType.fromString(loc));
    }

    public void summonMsg(String msg) {
        if (!getClient().getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        getClient().announce(UIPacket.summonMessage(msg));
    }

    public void summonMsg(int type) {
        if (!getClient().getPlayer().hasSummon()) {
            playerSummonHint(true);
        }
        getClient().announce(UIPacket.summonMessage(type));
    }

    public void showInstruction(String msg, int width, int height) {
        getClient().announce(MaplePacketCreator.sendHint(msg, width, height));
    }

    public void playerSummonHint(boolean summon) {
        getClient().getPlayer().setHasSummon(summon);
        getClient().announce(UIPacket.summonHelper(summon));
    }

    public String getInfoQuest(int questId) {
        return getClient().getPlayer().getInfoQuest(questId);
    }

    public void updateInfoQuest(int questId, String data) {
        getClient().getPlayer().updateInfoQuest(questId, data);
    }

    public boolean getEvanIntroState(String data) {
        return getInfoQuest(22013).equals(data);
    }

    public void updateEvanIntroState(String data) {
        updateInfoQuest(22013, data);
    }

    public void Aran_Start() {
        getClient().announce(UIPacket.Aran_Start());
    }

    public void evanTutorial(String data, int v1) {
        getClient().announce(NPCPacket.getEvanTutorial(data));
    }

    public void AranTutInstructionalBubble(String data) {
        getClient().announce(EffectPacket.AranTutInstructionalBalloon(data));
    }

    public void TutInstructionalBalloon(String data) {
        AranTutInstructionalBubble(data);
    }

    public void showWZEffect(String data) {
        getClient().announce(EffectPacket.ShowWZEffect(data));
    }

    public void showEffect(String effect) {
        getClient().announce(MaplePacketCreator.showEffect(effect));
    }

    public void playSound(String sound) {
        playSound(false, sound);
    }

    public void playSound(boolean broadcast, String sound) {
        if (broadcast) {
            getMap().broadcastMessage(MaplePacketCreator.playSound(sound));
        } else {
            getClient().announce(MaplePacketCreator.playSound(sound));
        }
    }

    public void startMapEffect(String msg, int itemId) {
        getClient().getPlayer().getMap().startMapEffect(msg, itemId);
    }

    public void showMapEffect(String path) {
        getClient().announce(UIPacket.showMapEffect(path));
    }

    public void EnableUI(short i) {
        getClient().announce(UIPacket.IntroEnableUI(i));
    }

    public void EnableUI(short i, boolean block) {
        getClient().announce(UIPacket.IntroEnableUI(i, block));
    }

    public void DisableUI(boolean enabled) {
        getClient().announce(UIPacket.IntroDisableUI(enabled));
    }

    public void MovieClipIntroUI(boolean enabled) {
        getClient().announce(UIPacket.IntroDisableUI(enabled));
        getClient().announce(UIPacket.IntroLock(enabled));
    }

    public void lockUI() {
        getClient().announce(UIPacket.IntroDisableUI(true));
        getClient().announce(UIPacket.IntroLock(true));
    }

    public void unlockUI() {
        getClient().announce(UIPacket.IntroDisableUI(false));
        getClient().announce(UIPacket.IntroLock(false));
    }

    public MapleInventoryType getInvType(int i) {
        return MapleInventoryType.getByType((byte) i);
    }

    public String getItemName(int itemId) {
        return MapleItemInformationProvider.getInstance().getName(itemId);
    }

    public void gainPetItem(int itemid) {
        gainPetItem(itemid, (short) 1);
    }

    public void gainPetItem(int itemid, short quantity) {
        MapleInventoryManipulator.addById(getClient(), itemid, quantity, "", MaplePet.createPet(itemid, MapleInventoryIdentifier.getInstance()), 45, getName() + "脚本获得");
    }

    public void gainPet(int itemId, String name, int level, int closeness, int fullness, long period, short flags) {
        if (itemId / 10000 != 500) {
            itemId = 5000000;
        }
        if (level > 30) {
            level = 30;
        }
        if (closeness > 30000) {
            closeness = 30000;
        }
        if (fullness > 100) {
            fullness = 100;
        }
        try {
            MapleInventoryManipulator.addById(getClient(), itemId, (short) 1, "", MaplePet.createPet(itemId, name, level, closeness, fullness, MapleInventoryIdentifier.getInstance(), itemId == 5000054 ? (int) period : 0, flags, 0, -1), 45, "Pet from interaction " + itemId + " (" + this.id + ")" + " on " + DateUtil.getCurrentDate());
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
    }

    public void removeSlot(int invType, byte slot, short quantity) {
        MapleInventoryManipulator.removeFromSlot(getClient(), getInvType(invType), slot, quantity, true);
    }

    public void gainGP(int gp) {
        if (getPlayer().getGuildId() <= 0) {
            return;
        }
        WorldGuildService.getInstance().gainGP(getPlayer().getGuildId(), gp); //1 for
    }

    public int getGP() {
        if (getPlayer().getGuildId() <= 0) {
            return 0;
        }
        return WorldGuildService.getInstance().getGP(getPlayer().getGuildId()); //1 for
    }

    public int itemQuantity(int itemId) {
        return getPlayer().itemQuantity(itemId);
    }

    public EventInstanceManager getDisconnected(String event) {
        EventManager em = getEventManager(event);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.isDisconnected(getClient().getPlayer()) && eim.getPlayerCount() > 0) {
                return eim;
            }
        }
        return null;
    }

    public EventInstanceManager getEIMbyEvenName(String string) {
        EventManager em = this.getEventManager(string);
        if (em == null) {
            return null;
        }
        for (EventInstanceManager eim : em.getInstances()) {
            if (eim.getPlayerCount() <= 0) continue;
            return eim;
        }
        return null;
    }

    public boolean isAllReactorState(int reactorId, int state) {
        boolean ret = false;
        for (MapleReactor r : getMap().getAllReactorsThreadsafe()) {
            if (r.getReactorId() == reactorId) {
                ret = r.getState() == state;
            }
        }
        return ret;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public void spawnMonster(int mobId) {
        spawnMonster(mobId, 1, getPlayer().getTruePosition());
    }

    // summon one monster, remote location
    public void spawnMonster(int mobId, int x, int y) {
        spawnMonster(mobId, 1, new Point(x, y));
    }

    // multiple monsters, remote location
    public void spawnMonster(int mobId, int quantity, int x, int y) {
        spawnMonster(mobId, quantity, new Point(x, y));
    }

    // handler for all spawnMonster
    public void spawnMonster(int mobId, int quantity, Point pos) {
        for (int i = 0; i < quantity; i++) {
            getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), pos);
        }
    }

    public void sendNPCText(String text, int npcId) {
        getMap().broadcastMessage(NPCPacket.getNPCTalk(npcId, (byte) 0, text, "00 00 00 00 00 00", (byte) 0));
    }

    public boolean getTempFlag(int flag) {
        return (getClient().getChannelServer().getTempFlag() & flag) == flag;
    }

    public void logPQ(String text) {
//	FileoutputUtil.log(FileoutputUtil.PQ_Log, text);
    }

    public void trembleEffect(int type, int delay) {
        getClient().announce(MaplePacketCreator.trembleEffect(type, delay));
    }

    public int nextInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public MapleQuest getQuest(int arg0) {
        return MapleQuest.getInstance(arg0);
    }

    public void achievement(int a) {
        getClient().getPlayer().getMap().broadcastMessage(MaplePacketCreator.achievementRatio(a));
    }

    public MapleInventory getInventory(int type) {
        return getClient().getPlayer().getInventory(MapleInventoryType.getByType((byte) type));
    }

    public int randInt(int arg0) {
        return Randomizer.nextInt(arg0);
    }

    public void sendDirectionStatus(int key, int value) {
        sendDirectionStatus(key, value, true);
    }

    public void sendDirectionStatus(int key, int value, boolean direction) {
        getClient().announce(UIPacket.getDirectionInfo(key, value));
        getClient().announce(UIPacket.getDirectionStatus(direction));
    }

    public void getDirectionStatus(boolean enable) {
        getClient().announce(UIPacket.inGameCurNodeEventEnd(enable));
    }

    public void sendDirectionInfo(String data) {
        getClient().announce(UIPacket.getDirectionInfo(data, 2000, 0, -100, 0));
        getClient().announce(UIPacket.getDirectionInfo(1, 2000));
    }

    public void getDirectionInfo(byte type, int value) {
        getClient().announce(UIPacket.getDirectionInfo(type, value));
    }

    public void getDirectionInfo(int value, int x, int y) {
        getClient().announce(UIPacket.getDirectionInfo(value, x, y));
    }

    public void getDirectionInfo(String data, int value, int x, int y, int a, int b) {
        getClient().announce(UIPacket.getDirectionInfo(data, value, x, y, a, b));
    }

    public void sendDirectionFacialExpression(int expression, int duration) {
        getClient().announce(UIPacket.getDirectionFacialExpression(expression, duration));
    }

    public void getDirectionInfoNew(byte type, int value) {
        getClient().announce(UIPacket.getDirectionInfoNew(type, value));
    }

    public void getDirectionInfoNew(byte type, int x, int y, int z) {
        getClient().announce(UIPacket.getDirectionInfoNew(type, x, y, z));
    }

    public void getDIRECTION_INFO(int value, int s, String data) {
        getClient().announce(UIPacket.getDIRECTION_INFO(data, value, s));
    }

    public void getDirectionEffect(String data, int value, int x, int y) {
        getClient().announce(UIPacket.getDirectionEffect(data, value, x, y));
    }

    public void getDirectionEffect(int mod, String data, int value, int value2, int value3, int a1, int a2, int a3, int a4, int npc) {
        getClient().announce(UIPacket.getDirectionEffect(mod, data, value, value2, value3, a1, a2, a3, a4, npc));
    }

    /*
     * 获取角色专业技能学习的个数
     */
    public int getProfessions() {
        int ii = 0;
        int skillId;
        for (int i = 0; i < 5; i++) {
            skillId = 92000000 + i * 10000;
            if (getClient().getPlayer().getProfessionLevel(skillId) > 0) {
                ii++;
            }
        }
        return ii;
    }

    public void setVip(int vip, long period) {
        getClient().getPlayer().setVip(vip);
        if (period > 0) {
            getClient().getPlayer().setViptime(period);
        }
    }

    public int getVip() {
        return getClient().getPlayer().getVip();
    }

    public boolean isVip() {
        return getClient().getPlayer().getVip() > 0;
    }

    /*
     * Vip 会员变量参数
     */
    public void setVip(int vip) {
        setVip(vip, 7);
    }

    public void setViptime(long period) {
        if (period != 0) {
            getClient().getPlayer().setViptime(period);
        }
    }

    /*
     * 获取挑战BOSS次数
     */
    public int getBossLog(String bossid) {
        return getClient().getPlayer().getBossLog(bossid);
    }

    public int getBossLog(String bossid, int type) {
        return getClient().getPlayer().getBossLog(bossid, type);
    }

    /*
     * 设置或增加挑战BOSS次数
     */
    public void setBossLog(String bossid) {
        getClient().getPlayer().setBossLog(bossid);
    }

    public void setBossLog(String bossid, int type) {
        getClient().getPlayer().setBossLog(bossid, type);
    }

    public void setBossLog(String bossid, int type, int count) {
        getClient().getPlayer().setBossLog(bossid, type, count);
    }

    /*
     * 重置挑战BOSS次数
     */
    public void resetBossLog(String bossid) {
        getClient().getPlayer().resetBossLog(bossid);
    }

    public void resetBossLog(String bossid, int type) {
        getClient().getPlayer().resetBossLog(bossid, type);
    }

    /*
     * 设置或增加组队成员的挑战BOSS次数
     * bossid 挑战BOSS任务的名称
     * type 0 = 0点重置 大于0不重置
     * count 设置的次数
     * checkMap 是否检测在同一地图
     */
    public void setPartyBossLog(String bossid) {
        setPartyBossLog(bossid, 0);
    }

    public void setPartyBossLog(String bossid, int type) {
        setPartyBossLog(bossid, type, 1);
    }

    public void setPartyBossLog(String bossid, int type, int count) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getClient().getPlayer().setBossLog(bossid, type, count);
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getPlayer().getMap().getCharacterById(chr.getId());
            if (curChar != null && curChar.getMapId() == cMap) {
                curChar.setBossLog(bossid, type, count);
            }
        }
    }

    public int getBossLogAcc(String bossid) {
        return getClient().getPlayer().getBossLogAcc(bossid);
    }

    public void setBossLogAcc(String bossid) {
        getClient().getPlayer().setBossLogAcc(bossid);
    }

    public void setBossLogAcc(String bossid, int bosscount) {
        getClient().getPlayer().setBossLogAcc(bossid, bosscount);
    }

    public int getEventLogForDay(String eventId) {
        return getClient().getPlayer().getEventLogForDay(eventId);
    }

    public void setEventLogForDay(String eventId) {
        getClient().getPlayer().setEventLogForDay(eventId);
    }

    public void setEventLogForDay(String eventId, int eventCount) {
        getClient().getPlayer().setEventLogForDay(eventId, eventCount);
    }

    public void resetEventLogForDay(String eventId) {
        getClient().getPlayer().resetEventLogForDay(eventId);
    }

    /*
     * 新的在线泡点
     */
    public int getGamePoints() {
        return getClient().getPlayer().getGamePoints();
    }

    public void gainGamePoints(int amount) {
        getClient().getPlayer().gainGamePoints(amount);
    }

    public void resetGamePoints() {
        getClient().getPlayer().resetGamePoints();
    }

    /*
     * 打开时钟
     */
    public void getClock(int time) {
        getClient().announce(MaplePacketCreator.getClock(time));
    }

    /*
     * 打开1个网页
     */
    public void openWeb(String web) {
        getClient().announce(MaplePacketCreator.openWeb(web));
    }

    /*
     * 是否开启Pvp大乱战斗地图
     */
    public boolean isCanPvp() {
        return ServerConfig.CHANNEL_OPENPVP;
    }

    /*
     * 显示武林道场排名
     */
    public void showDoJangRank() {
        getClient().announce(MaplePacketCreator.getMulungRanking((byte) 1));
    }

    /*
     * 结婚脚本检测
     */
    public int MarrageChecking() {
        if (getPlayer().getParty() == null) { //没有组队 - -1
            return -1;
        } else if (getPlayer().getMarriageId() > 0) { //检测角色是否结婚 - 0
            return 0;
        } else if (getPlayer().getParty().getMembers().size() != 2) { //组队成员不等于2 - 1
            return 1;
        } else if (getPlayer().getGender() == 0 && !(getPlayer().haveItem(1050121) || getPlayer().haveItem(1050122) || getPlayer().haveItem(1050113))) {
            return 5;
        } else if (getPlayer().getGender() == 1 && !(getPlayer().haveItem(1051129) || getPlayer().haveItem(1051130) || getPlayer().haveItem(1051114))) {
            return 5;
        } else if (!getPlayer().haveItem(1112001)) { //没有恋人戒指
            return 6;
        }
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            if (chr.getId() == getPlayer().getId()) {
                continue;
            }
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar == null) { //找不到组队成员
                return 2;
            } else if (curChar.getMarriageId() > 0) { //组队成员中有人结婚
                return 3;
            } else if (curChar.getGender() == getPlayer().getGender()) { //性别相同
                return 4;
            } else if (curChar.getGender() == 0 && !(curChar.haveItem(1050121) || curChar.haveItem(1050122) || curChar.haveItem(1050113))) {
                return 5;
            } else if (curChar.getGender() == 1 && !(curChar.haveItem(1051129) || curChar.haveItem(1051130) || curChar.haveItem(1051114))) {
                return 5;
            } else if (!curChar.haveItem(1112001)) { //没有恋人戒指
                return 6;
            }
        }
        return 9;
    }

    /*
     * 结婚脚本获取组队成员中另外1个玩家的ID
     */
    public int getPartyFormID() {
        int curCharID = -1;
        if (getPlayer().getParty() == null) { //没有组队 - -1
            curCharID = -1;
        } else if (getPlayer().getMarriageId() > 0) { //检测角色是否结婚 - 0
            curCharID = -2;
        } else if (getPlayer().getParty().getMembers().size() != 2) { //组队成员不等于2 - 1
            curCharID = -3;
        }
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            if (chr.getId() == getPlayer().getId()) {
                continue;
            }
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar == null) {
                curCharID = -4;
            } else {
                curCharID = chr.getId();
            }
        }
        return curCharID;
    }

    /*
     * 获取角色的GM等级
     */
    public int getGMLevel() {
        return getClient().getPlayer().getGMLevel();
    }

    /*
     * 使用脚本对角色使用测谎仪
     */
    public void startLieDetector(boolean isItem) {
        getClient().getPlayer().startLieDetector(isItem);
    }

    /*
     * 转身系统
     */
    public int getReborns() {
        return getClient().getPlayer().getReborns();
    }

    public int getReborns1() {
        return getClient().getPlayer().getReborns1();
    }

    public int getReborns2() {
        return getClient().getPlayer().getReborns2();
    }

    public int getReborns3() {
        return getClient().getPlayer().getReborns3();
    }

    public void doReborn(int type) {
        getClient().getPlayer().doReborn(type);
    }

    public void doReborn(int type, int ap) {
        getClient().getPlayer().doReborn(type, ap);
    }

    /*
     * 多颜色聊天代码
     */
    public void spouseMessage(int op, String msg) {
        getClient().announce(MaplePacketCreator.spouseMessage(op, msg));
    }

    /*
     * GM警告信息
     */
    public void sendPolice(String text, boolean dc) {
        if (dc) {
            getClient().getPlayer().sendPolice(text);
        } else {
            getClient().announce(MaplePacketCreator.sendPolice(text));
        }
    }

    /*
     * 给组队中所有角色名声值
     */
    public void givePartyHonorExp(int gain, boolean show) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getClient().getPlayer().gainHonor(gain);
            if (show) {
                getClient().announce(MaplePacketCreator.spouseMessage(1, "扎比埃尔的悄悄话：阿斯旺剩下的希拉残党全部消灭掉啦？刚来了一些新东西，你可以来看看。说不定你能用得上～"));
            }
            return;
        }
        int cMap = getPlayer().getMapId();
        for (MaplePartyCharacter chr : getPlayer().getParty().getMembers()) {
            MapleCharacter curChar = getChannelServer().getPlayerStorage().getCharacterById(chr.getId());
            if (curChar != null && (curChar.getMapId() == cMap || curChar.getEventInstance() == getPlayer().getEventInstance())) {
                curChar.gainHonor(gain);
                if (show) {
                    curChar.getClient().announce(MaplePacketCreator.spouseMessage(1, "扎比埃尔的悄悄话：阿斯旺剩下的希拉残党全部消灭掉啦？刚来了一些新东西，你可以来看看。说不定你能用得上～"));
                }
            }
        }
    }

    /*
     * 取系统时间 格式为: yyyy年MM月dd日HH时mm分ss秒
     */
    public String getTime() {
        return DateUtil.getNowTime();
    }

    public boolean checkPartyEvent(int minLevel, int maxLevel, int minPartySize, int maxPartySize, int itemId) {
        MapleParty party = getClient().getPlayer().getParty();
        if (party == null || party.getMembers().size() < minPartySize || party.getLeader().getId() != getClient().getPlayer().getId()) {
            return false;
        }
        int inMap = 0;
        boolean next = true;
        int checkMapId = getPlayer().getMapId(); //需要检测的地图ID
        for (MaplePartyCharacter cPlayer : party.getMembers()) {
            MapleCharacter ccPlayer = getPlayer().getMap().getCharacterById(cPlayer.getId());
            if (ccPlayer != null && ccPlayer.getLevel() >= minLevel && ccPlayer.getLevel() <= maxLevel && ccPlayer.getMapId() == checkMapId && ccPlayer.haveItem(itemId)) {
                inMap += 1;
            } else {
                return false;
            }
        }
        if (party.getMembers().size() > maxPartySize || inMap < minPartySize) {
            next = false;
        }
        return next;
    }

    /*
     * 新增变量
     */
    public int getPlayerPoints() {
        return getClient().getPlayer().getPlayerPoints();
    }

    public void setPlayerPoints(int gain) {
        getClient().getPlayer().setPlayerPoints(gain);
    }

    public void gainPlayerPoints(int gain) {
        getClient().getPlayer().gainPlayerPoints(gain);
    }

    public int getPlayerEnergy() {
        return getClient().getPlayer().getPlayerEnergy();
    }

    public void setPlayerEnergy(int gain) {
        getClient().getPlayer().setPlayerEnergy(gain);
    }

    public void gainPlayerEnergy(int gain) {
        getClient().getPlayer().gainPlayerEnergy(gain);
    }

    /*
     * 新增函数
     */
    public int getEventCount(String eventId) {
        return getClient().getPlayer().getEventCount(eventId);
    }

    public int getEventCount(String eventId, int type) {
        return getClient().getPlayer().getEventCount(eventId, type);
    }

    public void setEventCount(String eventId) {
        getClient().getPlayer().setEventCount(eventId);
    }

    public void setEventCount(String eventId, int type) {
        getClient().getPlayer().setEventCount(eventId, type);
    }

    public void setEventCount(String eventId, int type, int count) {
        getClient().getPlayer().setEventCount(eventId, type, count);
    }

    public void resetEventCount(String eventId) {
        getClient().getPlayer().resetEventCount(eventId);
    }

    public void resetEventCount(String eventId, int type) {
        getClient().getPlayer().resetEventCount(eventId, type);
    }

    public void setPartyEventCount(String eventId) {
        setPartyEventCount(eventId, 0);
    }

    public void setPartyEventCount(String eventId, int type) {
        setPartyEventCount(eventId, type, 1);
    }

    public void setPartyEventCount(String eventId, int type, int count) {
        if (getPlayer().getParty() == null || getPlayer().getParty().getMembers().size() == 1) {
            getClient().getPlayer().setEventCount(eventId, type, count);
            return;
        }
        int checkMap = getPlayer().getMapId();
        for (MaplePartyCharacter partyPlayer : getPlayer().getParty().getMembers()) {
            MapleCharacter chr = getPlayer().getMap().getCharacterById(partyPlayer.getId());
            if (chr != null && chr.getMapId() == checkMap) {
                chr.setEventCount(eventId, type, count);
            }
        }
    }

    public boolean checkPartyEventCount(String eventId) {
        return checkPartyEventCount(eventId, 1);
    }

    public boolean checkPartyEventCount(String eventId, int checkcount) {
        MapleParty party = getClient().getPlayer().getParty();
        int count;
        if (party == null || party.getMembers().size() == 1) {
            count = getEventCount(eventId);
            return count >= 0 && count < checkcount;
        }
        int check = 0;
        int partySize = party.getMembers().size();
        for (MaplePartyCharacter partyPlayer : party.getMembers()) {
            MapleCharacter chr = getPlayer().getMap().getCharacterById(partyPlayer.getId());
            if (chr != null) {
                count = chr.getEventCount(eventId);
                if (count >= 0 && count < checkcount) {
                    check++;
                }
            }
        }
        return partySize == check;
    }

    public MapleItemInformationProvider getItemInfo() {
        return MapleItemInformationProvider.getInstance();
    }

    public Equip getNewEquip(int equipid) {
        Equip equip = (Equip) getItemInfo().getEquipById(equipid);
        return getItemInfo().randomizeStats(equip);
    }

    public Equip getEquipBySlot(short slot) {
        return (Equip) getClient().getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
    }

    /**
     * 修改角色武器的攻击上限
     * 如果角色武器为空 或者不是武器 返回 假
     * 如果角色武器新的攻击上限属性小于0 或者大于1亿也返回 假
     */
    public boolean changeLimitBreak(int amount) {
        //获取角色的武器信息 检查是否为空或者是否为武器
        Equip equip = (Equip) getClient().getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (equip == null || !ItemConstants.isWeapon(equip.getItemId())) {
            return false;
        }
        //检查新属性是否小于0或者超过设置的上限
        int newLimitBreak = equip.getLimitBreak() + amount;
        if (newLimitBreak < 0 || newLimitBreak > 2100000000) {
            return false;
        }
        //设置道具的新攻击上限
        equip.setLimitBreak(newLimitBreak);
        //更新道具状态信息
        getClient().getPlayer().forceUpdateItem(equip);
        return true;
    }

    /**
     * 获取角色武器的攻击突破上限
     */
    public int getLimitBreak() {
        int limitBreak = 999999; //默认的上限
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip weapon = (Equip) getClient().getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -11);
        if (weapon != null) {
            //武器自带的突破上限数字 + 武器附加的上限数字
            limitBreak = ii.getLimitBreak(weapon.getItemId()) + weapon.getLimitBreak();
        }
        return limitBreak;
    }

    public boolean WeaponLimitBreak(int limitBreak) {
        Item item = this.getInventory(-1).getItem((short) -11);
        if (item != null) {
            Equip equip = (Equip) item;
            if (!ItemConstants.isWeapon(equip.getItemId()) || (limitBreak += equip.getLimitBreak()) > ItemConstants.getLimitBreakSurplus(equip.getItemId())) {
                return false;
            }
            equip.setLimitBreak(limitBreak);
            ArrayList<ModifyInventory> arrayList = new ArrayList<>();
            arrayList.add(new ModifyInventory(3, equip));
            arrayList.add(new ModifyInventory(0, equip));
            getClient().announce(InventoryPacket.modifyInventory(true, arrayList, getPlayer()));
            return true;
        }
        return false;
    }

    public int getRandomPotential(short slot, int potId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip equip = (Equip) getClient().getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
        if (equip == null || ii.isCash(equip.getItemId()) || ii.getPotentialInfo(potId) == null) {
            return -1;
        }
        List<List<StructItemOption>> pots = new LinkedList<>(ii.getPotentialInfos(40000).values());
        int reqLevel = ii.getReqLevel(equip.getItemId()) / 10;
        int count = 0;
        boolean rewarded = false;
        while (!rewarded) {
            count++;
            StructItemOption pot = pots.get(nextInt(pots.size())).get(reqLevel);
            if (pot != null && pot.reqLevel / 10 <= reqLevel && pot.opID == potId) {
                rewarded = true;
            } else if (count > 3000) {
                rewarded = true;
            }
        }
        return count;
    }

    public boolean changePotential(byte slot, int potline, int potId, boolean show) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Equip equip = (Equip) getClient().getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(slot);
        if (equip == null || ii.isCash(equip.getItemId()) || ii.getPotentialInfo(potId) == null) {
            return false;
        }
        if (potline >= 1 && potline <= 6) {
            equip.setPotential(potId, potline > 3 ? potline - 3 : potline, potline > 3);
            getClient().getPlayer().forceUpdateItem(equip);
            if (show) {
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getClient().getPlayer().getName(), " : 使用 一键潜能功能 将装备{" + ii.getName(equip.getItemId()) + "}第" + potline + "条潜能修改.大家一起恭喜他（她）吧！！！！", equip, 3, getClient().getChannel()));
            }
            return true;
        } else {
            return false;
        }
    }

    public byte getSubcategory() {
        return getClient().getPlayer().getSubcategory();
    }

    public final int getActivity() {
        return MapleActivity.getActivity(getPlayer());
    }

    public final int getMaxActivity() {
        return MapleActivity.getMaxActivity();
    }

    public final int getDiffActivity() {
        return MapleActivity.getDiffActivity(getPlayer());
    }

    public final int getNextStage() {
        return MapleActivity.getNextStage(getPlayer());
    }

    public final void finishActivity(int questid) {
        MapleActivity.finish(getPlayer(), questid);
    }

    public final int getAQActivity(final int questid) {
        return MapleActivity.QuestActivity.getActivityById(questid);
    }

    public final int getAQMaxTimes(final int questid) {
        return MapleActivity.QuestActivity.getMaxTimesById(questid);
    }

    public final int getAQNextStageNeed() {
        return MapleActivity.getNextStageNeed(getPlayer());
    }

    public final int getRecevieReward() {
        return MapleActivity.getRecevieReward(getPlayer());
    }

    public List<Integer> getSevenDayPayLog(int day) {
        List<Integer> ret = new ArrayList<>();
        for (int i = 0; i < day; i++) {
            ret.add(0);
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM paylog WHERE account = ?");
            ps.setString(1, getAccountName());
            ResultSet rs = ps.executeQuery();

            Timestamp currtime = new Timestamp(System.currentTimeMillis());
            while (rs.next()) {
                int rmb = rs.getInt("rmb");
                Timestamp time = rs.getTimestamp("paytime");
                int diffday = (int) ((currtime.getTime() - time.getTime()) / (1000 * 60 * 60 * 24));
                if (diffday < day) {
                    ret.set(diffday, (ret.get(diffday)) + rmb);
                }
            }
            ps.close();
            rs.close();
        } catch (SQLException e) {
            System.err.println("获取充值记录失败" + e);
        }
        return ret;
    }

//    public List<CharNameAndId> getPayRankingTop() {
//        List<CharNameAndId> ret = new LinkedList<>();
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DATE, -(cal.get(Calendar.DAY_OF_WEEK) - 1));
//
//        try {
//            Connection con = DatabaseConnection.getConnection();
//            PreparedStatement ps = con.prepareStatement("SELECT SUM(rmb) FROM paylog WHERE ORDER BY rmb DESC LIMIT 10");
//            ps.setString(1, getClient().getAccountName());
//            ResultSet rs = ps.executeQuery();
//
//            Timestamp currtime = new Timestamp(System.currentTimeMillis());
//            while (rs.next()) {
//                int rmb = rs.getInt("rmb");
//                Timestamp time = rs.getTimestamp("paytime");
//                int diffday = (int) ((currtime.getTime() - time.getTime()) / (1000 * 60 * 60 * 24));
//                if (diffday < day) {
//                    ret.set(diffday, (int) (ret.get(diffday)) + rmb);
//                }
//            }
//            ps.close();
//            rs.close();
//        } catch (SQLException e) {
//            System.err.println("获取充值记录失败" + e);
//        }
//        return ret;
//    }

    /**
     * 高级任务系统 - 检查基础条件是否符合所有任务前置条件
     *
     * @param missionid
     * @return
     */
    public final boolean MissionCanMake(final int missionid) {
        return getPlayer().MissionCanMake(missionid);
    }

    /**
     * 高级任务系统 - 检查基础条件是否符合指定任务前置条件
     *
     * @param missionid
     * @param checktype
     * @return
     */
    public final boolean MissionCanMake(final int missionid, final int checktype) {
        return getPlayer().MissionCanMake(missionid, checktype);
    }

    /**
     * 高级任务函数 - 得到任务的等级数据
     *
     * @param missionid
     * @param checktype
     * @return
     */
    public final int MissionGetIntData(final int missionid, final int checktype) {
        return getPlayer().MissionGetIntData(missionid, checktype);
    }

    /**
     * 高级任务函数 - 得到任务的的字符串型数据
     *
     * @param missionid
     * @param checktype
     * @return
     */
    public final String MissionGetStrData(final int missionid, final int checktype) {
        return getPlayer().MissionGetStrData(missionid, checktype);
    }

    /**
     * 高级任务函数 - 直接输出需要的职业列表串
     *
     * @param joblist
     * @return
     */
    public final String MissionGetJoblist(final String joblist) {
        return getPlayer().MissionGetJoblist(joblist);
    }

    /**
     * 高级任务系统 - 任务创建
     *
     * @param charid
     * @param missionid
     * @param repeat
     * @param repeattime
     * @param lockmap
     * @param mobid
     */
    public final void MissionMake(final int charid, final int missionid, final int repeat, final int repeattime, final int lockmap, final int mobid) {
        getPlayer().MissionMake(charid, missionid, repeat, repeattime, lockmap, mobid);
    }

    /**
     * 高级任务系统 - 重新做同一个任务
     *
     * @param charid     角色ID
     * @param missionid  任务ID
     * @param repeat     重复次数
     * @param repeattime 重复间隔时间
     * @param lockmap    锁定地图ID
     */
    public final void MissionReMake(final int charid, final int missionid, final int repeat, final int repeattime, final int lockmap) {
        getPlayer().MissionReMake(charid, missionid, repeat, repeattime, lockmap);
    }

    /**
     * 高级任务系统 - 任务完成
     *
     * @param charid
     * @param missionid
     */
    public final void MissionFinish(final int charid, final int missionid) {
        getPlayer().MissionFinish(charid, missionid);
    }

    /**
     * 高级任务系统 - 放弃任务
     *
     * @param charid
     * @param missionid
     */
    public final void MissionDelete(final int charid, final int missionid) {
        getPlayer().MissionDelete(charid, missionid);
    }

    /**
     * 添加最小任务
     *
     * @param charid
     * @param missionid
     * @param num
     */
    public final void MissionSetMinNum(final int charid, final int missionid, final int num) {
        getPlayer().MissionSetMinNum(charid, missionid, num);
    }

    /**
     * 获取最小任务
     *
     * @param charid
     * @param missionid
     * @param mobid
     * @return
     */
    public final int MissionGetMinNum(final int charid, final int missionid, final int mobid) {
        return getPlayer().MissionGetMinNum(charid, missionid, mobid);
    }

    public final int MissionGetMaxNum(final int charid, final int missionid, final int mobid) {
        return getPlayer().MissionGetMaxNum(charid, missionid, mobid);
    }

    public final int MissionGetMobId(final int charid, final int missionid) {
        return getPlayer().MissionGetMobId(charid, missionid);
    }

    public final void MissionSetMobId(final int charid, final int missionid, final int mobid) {
        getPlayer().MissionSetMobId(charid, missionid, mobid);
    }

    public final int MissionGetFinish(final int charid, final int missionid) {
        return getPlayer().MissionGetFinish(charid, missionid);
    }

    /**
     * 高级任务系统 - 指定任务的需要最大打怪数量
     *
     * @param missionid
     * @param maxnum
     */
    public final void MissionMaxNum(final int missionid, final int maxnum) {
        getPlayer().MissionMaxNum(missionid, maxnum);
    }

    /**
     * 高级任务系统 - 放弃所有未完成任务
     *
     * @param charid
     */
    public final void MissionDeleteNotFinish(final int charid) {
        getPlayer().MissionDeleteNotFinish(charid);
    }

    /**
     * 高级任务系统 - 获得任务是否可以做
     *
     * @param charid
     * @param missionid
     * @param maxtimes
     * @param checktype
     * @return
     */
    public final boolean MissionStatus(final int charid, final int missionid, final int maxtimes, final int checktype) {
        return getPlayer().MissionStatus(charid, missionid, maxtimes, checktype);
    }

    public final long MissionGetRepeattime(final int charid, final int missionid) {
        return getPlayer().MissionGetRepeattime(charid, missionid);
    }

    public final void MissionAddMinNum(final int charid, final int missionid, final int num) {
        getPlayer().MissionAddMinNum(charid, missionid, num);
    }

    public int getRMB() {
        return getPlayer().getRMB();
    }

    public void setRMB(int rmb) {
        getPlayer().setRMB(rmb);
    }

    public void gainRMB(int rmb) {
        getPlayer().gainRMB(rmb);
    }

    public int getTotalRMB() {
        return getPlayer().getTotalRMB();
    }

    public List<Pair<String, Integer>> getTotalRMBRanking(int limit) {
        return getPlayer().getTotalRMBRanking(limit);
    }

    public int getMapleEquipOnlyId() {
        return MapleEquipOnlyId.getInstance().getNextEquipOnlyId();
    }

    public void addFromDrop(MapleClient c, Item item, boolean show) {
        MapleInventoryManipulator.addFromDrop(getClient(), item, show);
    }

    public Connection getConnection() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public final Item newItem(final int id, final byte position, final short quantity) {
        return new Item(id, position, quantity);
    }

    public final Item newItem(final int id, final byte position, final short quantity, final short flag) {
        return new Item(id, position, quantity, flag);
    }

    public final void addByItem(final Item item) {
        MapleInventoryManipulator.addbyItem(getClient(), item);
    }

    public void changeDamageSkin(int id) {
        getPlayer().changeDamageSkin(id);
    }

    public void updateSubmitBug(int id, int status) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE buglog SET status = ? WHERE id = ?")) {
                ps.setInt(1, status);
                ps.setInt(2, id);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public void submitBug(String title, String content) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO buglog VALUES (DEFAULT, ?, ?, ?, DEFAULT)")) {
                ps.setInt(1, getPlayer().getId());
                ps.setString(2, title);
                ps.setString(3, content);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error(e.getMessage());
        }
    }

    public List<Quadruple> getSubmitBug() {
        List<Quadruple> buglist = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM buglog WHERE charid = ?")) {
                ps.setInt(1, getPlayer().getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        buglist.add(new Quadruple<>(rs.getInt("id"), rs.getString("title"), rs.getString("content"), rs.getInt("status")));
                    }
                }
            }
        } catch (SQLException e) {
            log.error(e.toString());
        }
        return buglist;
    }

    public RankingTop getRankingTopInstance() {
        return RankingTop.getInstance();
    }

    public final MapleMonster getMonster(final int mobid) {
        return MapleLifeFactory.getMonster(mobid);
    }

    /**
     * 获取怪物状态
     *
     * @param hp  最大HP
     * @param mp  最大MP
     * @param exp 经验值
     * @return 重载后的怪物状态对象
     */
    public final OverrideMonsterStats getOverrideMonsterStats(final long hp, final int mp, final int exp) {
        return new OverrideMonsterStats(hp, mp, exp);
    }

    public final long getNextDayDiff(int day) {
        return DateUtil.getNextDayDiff(day);
    }

    public final void getShowItemGain(int itemId, short quantity, boolean inChat) {
        getClient().announce(MaplePacketCreator.getShowItemGain(itemId, quantity, inChat));
    }

    public final void insertRanking(String rankingname, int value) {
        RankingTop.getInstance().insertRanking(getPlayer(), rankingname, value);
    }

    public final List<CharNameAndId> getRanking(String rankingname) {
        return RankingTop.getInstance().getRanking(rankingname);
    }

    public final List<CharNameAndId> getRanking(String rankingname, int previous) {
        return RankingTop.getInstance().getRanking(rankingname, previous);
    }

    public final List<CharNameAndId> getRanking(String rankingname, int previous, boolean repeatable) {
        return RankingTop.getInstance().getRanking(rankingname, previous, repeatable);
    }

    /**
     * 发送NPC右下角对话，单位：分钟
     *
     * @param npcid 显示的NPCID, 为0的话 显示的是玩家自己
     * @param text  文本内容
     * @param time  持续时间
     */
    public void getNpcNotice(int npcid, String text, int time) {
        getClient().getPlayer().getNpcNotice(npcid, text, time);
    }

    public void setCredit(String name, int value) {
        getClient().getPlayer().setCredit(name, value);
    }

    public void gainCredit(String name, int value) {
        getClient().getPlayer().gainCredit(name, value);
    }

    public int getCredit(String name) {
        return getClient().getPlayer().getCredit(name);
    }

    public int getWp() {
        return getClient().getPlayer().getWeaponPoint();
    }

    public void setWp(int wp) {
        getClient().getPlayer().setWeaponPoint(wp);
    }

    public void gainWp(int wp) {
        getClient().getPlayer().gainWeaponPoint(wp);
    }

    public MapleMonsterInformationProvider getMonsterInfo() {
        return MapleMonsterInformationProvider.getInstance();
    }

    public void enterCS() {
        InterServerHandler.enterCS(getClient(), getClient().getPlayer(), true);
    }

    public void playMovie(String data, boolean show) {
        getClient().announce(UIPacket.playMovie(data, show));
    }

    public void openUI(int id) {
        getClient().announce(UIPacket.sendOpenWindow(id));
    }

    public void putGeneralData(String key, String value) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT general_data (`key`, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = ?")) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.setString(3, value);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("添加通用数据失败", e);
        }
    }

    public void removeGeneralData(String key) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM general_data WHERE `key` = ?")) {
                ps.setString(1, key);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("删除通用数据失败", e);
        }
    }

    public JSONObject getGeneralData(String key) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT `value` FROM general_data WHERE `key` = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new JSONObject(rs.getString("value"));
                    } else {
                        return new JSONObject();
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获得通用数据失败", e);
            return new JSONObject();
        }
    }

    public ResultSet select(String SQL) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(SQL)) {
                return ps.executeQuery();
            }
        } catch (SQLException e) {
            log.error("查询数据失败", e);
            return null;
        }
    }

    public void update(String SQL) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(SQL)) {
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("插入数据失败", e);
        }
    }

    public void updatePet(Object object) {
//        ArrayList<Item> arrayList = new ArrayList<>();
//        if (((Item)object).getPet() != null) {
//            arrayList.add(new a.b.t(0, this.getPlayer().getInventory(m.AI).getItem(((a.b.e)object).getPosition())));
//            getClient().announce(i.a(true, arrayList, this.getClient().getPlayer()));
//        }
    }

    public boolean changePetColor(int index) {
        MaplePet pet = getPlayer().getSpawnPet(index);
        if (pet != null) {
            pet.setColor(this.nextInt(153));
            this.getPlayer().getMap().broadcastMessage(PetPacket.changePetColor(getPlayer(), pet));
            return true;
        }
        return false;
    }

    public List<MaplePet> getSummonedPet() {
        return this.getPlayer().getSummonedPets();
    }

    public final boolean canSpawn(int mapid, Point point) {
        return this.getMap(mapid).getFootholds().findBelow(point, false) != null;
    }


    public void checkMedalQuest() {
        scripting.map.MapScriptMethods.explorationPoint(getClient());
    }

    public boolean checkPartyMemberNearby(Point point) {
        return this.getPlayer().getParty() != null && this.getPlayer().getParty().getMembers().parallelStream().map(partyCharacter -> this.getPlayer().getMap().getCharacterById(partyCharacter.getId())).allMatch(player -> player == null || player.getTruePosition().distanceSq(point) <= 800.0);
    }

    public int deleteCharacter(int cid) {
        return getClient().deleteCharacter(cid);
    }

    public void dispelBuff(int skillid) {
        getPlayer().dispelBuff(skillid);
    }

    public void displayNode(MapleMonster mob) {
        if (mob != null) {
            mob.switchController(this.getPlayer(), false);
            this.getClient().announce(MobPacket.mobEscortReturnBefore(mob, this.getMap()));
        }
    }

    public void EventGainNX() {
        gainNX((int) (getPlayer().getParty().getAverageLevel() / 250.0 * 3000.0));
    }

    public final void fieldGravefall(int count, int type1, int type2) {
        getMap().broadcastMessage(getPlayer(), MaplePacketCreator.createObtacleAtom(count, type1, type2, getMap()), true);
    }

    public final void gainPQPoint() {
        if (getPlayer() == null) {
            return;
        }
        long l2 = (int) ((double) (Randomizer.rand(25, 50) * getLevel()) * 1.5);
        getPlayer().gainPQPoint(l2);
        getPlayer().dropMessage(-9, "获得了" + l2 + "组队点数!");
    }

    public boolean gainSailBouns() {
        int n2 = this.getSailCoins();
        if (getPlayer().canHold(4310100)) {
            getPlayer().gainItem(4310100, n2, "航海奖励领取");
            getPlayer().updateInfoQuest(17011, "");
            return true;
        }
        return false;
    }

    public int getSailStat() {
        String string = getPlayer().getOneInfo(17011, "S");
        if (string != null) {
            return Integer.valueOf(string);
        }
        return -1;
    }

    public int getSailCoins() {
        String string = getPlayer().getOneInfo(17011, "C");
        if (string != null) {
            return Integer.valueOf(string);
        }
        return 0;
    }

    public void setCanSail() {
        getPlayer().doneSailQuestion();
    }

    public void GainSpecial(int type) {
        switch (type) {
            case 1: {
                if (nextInt(100) >= 15) break;
                this.gainItem(ItemConstants.fa()[nextInt(ItemConstants.fa().length)], (short) 1);
                break;
            }
            case 2: {
                if (nextInt(100) >= 15) break;
                this.gainItem(ItemConstants.fb()[nextInt(ItemConstants.fb().length)], (short) 1);
                break;
            }
            case 3: {
                if (nextInt(100) >= 15) break;
                this.gainItem(ItemConstants.fc()[nextInt(ItemConstants.fc().length)], (short) 5);
            }
        }
    }

    public final String getAccountName() {
        return getClient().getAccountName();
    }

    public List getCashItemlist() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleInventory items = getPlayer().getInventory(MapleInventoryType.EQUIP);
        ArrayList<Item> ret = new ArrayList<>();
        for (Item item : items) {
            if (ii.isCash(item.getItemId())) {
                ret.add(item);
            }
        }
        return ret;
    }

    public boolean Singin() {
        int n2 = this.getSinginCount();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT `count` , `lastdate` FROM singin WHERE characterid = ?")) {
                ps.setInt(1, this.getPlayer().getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean bl2;
                        Timestamp timestamp2 = rs.getTimestamp("lastdate");
                        boolean bl3 = this.getPlayer().getDaybyDay(0).getTimeInMillis() - timestamp2.getTime() > 0;
                        long l2 = timestamp2.getTime() - this.getPlayer().getDaybyDay(-1).getTimeInMillis();
                        boolean bl4 = bl2 = l2 <= 86400000 && l2 >= 0;
                        if (bl3) {
                            try (PreparedStatement preparedStatement = con.prepareStatement("UPDATE singin SET lastdate = ?, count = ? WHERE characterid = ?")) {
                                preparedStatement.setTimestamp(1, timestamp);
                                preparedStatement.setInt(2, bl2 && n2 < 31 ? rs.getInt("count") + 1 : 1);
                                preparedStatement.setInt(3, this.getPlayer().getId());
                                preparedStatement.executeUpdate();
                            }
                            return true;
                        }
                        return false;
                    }
                }
            }
            try (PreparedStatement preparedStatement = con.prepareStatement("INSERT INTO singin (characterid, lastdate, count) VALUES (?, ?, ?)")) {
                preparedStatement.setInt(1, this.getPlayer().getId());
                preparedStatement.setTimestamp(2, timestamp);
                preparedStatement.setInt(3, 1);
                preparedStatement.execute();
            }
            return true;
        } catch (SQLException e) {
            log.error("签到出现错误", e);
            return false;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public int getSinginCount() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT `count` FROM singin WHERE characterid = ?")) {
                ps.setInt(1, this.getPlayer().getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return 0;
                    {
                        return rs.getInt("count");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("签到出现错误", e);
        }
        return 0;
    }

    public void getChoiceText(byte type, byte mode, int npc) {
        getClient().announce(NPCPacket.getNPCTalkText(type, mode, npc));
    }

    public List<MapleCharacter> getchrlist() {
        return getClient().loadCharacters(getPlayer().getWorld());
    }

    public int getOnlineTime() {
        return getPlayer().getOnlineTime() / 60000;
    }

    public void setOnlineTime() {
        getPlayer().setOnlineTime(System.currentTimeMillis());
    }

    public int getTodayOnlineTime() {
        return getPlayer().getTodayOnlineTime();
    }

    public void initTodayOnlineTime() {
        getPlayer().initTodayOnlineTime();
    }

    public final void spawnNpcForPlayer(int mapid, int id, Point pos) {
        this.getMap(mapid).spawnNpcForPlayer(getClient(), id, pos);
    }

    public final Point getSpawnPoint(int mapid) {
        ArrayList<Point> arrayList = new ArrayList<>();
        MapleMap map = this.getMap(mapid);
        map.getFootholds().getAllRelevants().forEach(p2 -> arrayList.add(new Point(Randomizer.rand(map.getLeft(), map.getRight()), p2.getY2()))
        );
        return arrayList.get(nextInt(arrayList.size()));
    }

    public void onUserEnter(String onUserEnter) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = this.getPlayer().getMap().getCharacterById(partyCharacter.getId());
                if (player == null) continue;
                scripting.map.MapScriptMethods.startScript_FirstUser(player.getClient(), onUserEnter);
            }
        }
    }

    public void openMapScript(String onUserEnter) {
        getClient().removeClickedNPC();
        scripting.map.MapScriptMethods.startScript_FirstUser(this.getClient(), onUserEnter);
    }

    public boolean giveWeaponByLevel(int level) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        MapleItemInformationProvider ii = getItemInfo();
        ii.getAllItemNames().entrySet().parallelStream().forEach(stringStringEntry -> {
            int itemId = Integer.valueOf(stringStringEntry.getKey());
            String name = stringStringEntry.getValue();
            if (!name.contains("枫叶")
                    && !name.contains("游泳")
                    && !name.contains("国旗")
                    && !name.contains("周年")
                    && itemId % 100 >= 0
                    && ii.getReqLevel(itemId) == level
                    && getPlayer().isMainWeapon(getJob(), itemId)
                    && ii.isEquipTradeBlock(itemId)) {
                arrayList.add(itemId);
            }
        });
        if (arrayList.size() > 0) {
            this.gainItem(arrayList.get(nextInt(arrayList.size())), (short) 1);
            return true;
        }
        return false;
    }

    public boolean isAdmin() {
        return getPlayer().isAdmin();
    }

    public boolean isGMS() {
        return ServerConstants.GMS;
    }

    public boolean isMarried() {
        return getPlayer().getMarriageId() > 0;
    }

    public void sendMarriedBefore() {
        EventInstanceManager eim = this.getEventInstance();
        if (eim != null) {
            int ringid1 = Integer.valueOf(eim.getProperty("male"));
            int ringid2 = Integer.valueOf(eim.getProperty("female"));
            this.getPlayer().getMap().broadcastMessage(MaplePacketCreator.sendMarriedBefore(ringid1, ringid2));
        }
    }

    public void nextNodeAction(int mobid, int time) {
        getMap().nextNodeAction(mobid, time);
    }

    public void outputFileError(Throwable throwable) {
        log.error("脚本异常", throwable);
    }

    public void sendRemoveNPC(int npcid) {
        getClient().announce(NPCPacket.removeNPC(npcid));
    }

    public void sendPyramidEnergy(String object, String amount) {
        getPlayer().writeEnergy(object, amount);
    }

    public void spawnPortal() {
        getClient().announce(MaplePacketCreator.spawnPortal(999999999, 999999999, 0, null));
    }

    public void spawnNPCRequestController(int npcid, int x, int y) {
        this.spawnNPCRequestController(npcid, x, y, 0);
    }

    public void spawnNPCRequestController(int npcid, int x, int y, int f) {
        MapleNPC npc;
        if (npcs.containsKey(new Pair<>(npcid, c))) {
            npcs.remove(new Pair<>(npcid, c));
        }
        if ((npc = MapleLifeFactory.getNPC(npcid, getMapId())) == null) {
            return;
        }
        npc.setPosition(new Point(x, y));
        npc.setCy(y);
        npc.setRx0(x - 50);
        npc.setRx1(x + 50);
        npc.setF(f);
        npc.setFh(getMap().getFootholds().findBelow(new Point(x, y), false).getId());
        npc.setCustom(true);
        npc.setObjectId(npcid);
        npcs.put(new Pair<>(npcid, c), npc);
        getClient().announce(NPCPacket.spawnNPCRequestController(npc, true));
    }

    public void setNPCSpecialAction(int npcid, String action) {
        if (!npcs.containsKey(new Pair<>(npcid, this.c))) {
            return;
        }
        MapleNPC npc = npcs.get(new Pair<>(npcid, c));
        getClient().announce(NPCPacket.setNPCSpecialAction(npc.getObjectId(), action));
    }

    public void updateNPCSpecialAction(int npcid, int value, int x, int y) {
        if (!npcs.containsKey(new Pair<>(npcid, this.c))) {
            return;
        }
        MapleNPC npc = npcs.get(new Pair<>(npcid, c));
        getClient().announce(NPCPacket.updateNPCSpecialAction(npc.getObjectId(), value, x, y));
    }

    public void getNPCDirectionEffect(int npcid, String data, int value, int x, int y) {
        if (!npcs.containsKey(new Pair<>(npcid, this.c))) {
            return;
        }
        MapleNPC npc = npcs.get(new Pair<>(npcid, c));
        getClient().announce(UIPacket.getDirectionEffect(data, value, x, y, npc.getObjectId()));
    }

    public void removeNPCRequestController(int npcid) {
        if (!npcs.containsKey(new Pair<>(npcid, this.c))) {
            return;
        }
        MapleNPC npc = npcs.get(new Pair<>(npcid, c));
        getClient().announce(NPCPacket.removeNPCController(npc.getObjectId(), false));
        sendRemoveNPC(npc.getObjectId());
        npcs.remove(new Pair<>(npcid, this.c));
    }

    public void enableActions() {
        getClient().announce(MaplePacketCreator.enableActions());
    }

    public void spawnReactorOnGroundBelow(int id, int x, int y) {
        getMap().spawnReactorOnGroundBelow(new MapleReactor(MapleReactorFactory.getReactor(id), id), new Point(x, y));
    }

    public void removeNPCController(int npcid) {
        if (!npcs.containsKey(new Pair<>(npcid, this.c))) {
            return;
        }
        MapleNPC npc = npcs.get(new Pair<>(npcid, c));
        getClient().announce(NPCPacket.removeNPCController(npc.getObjectId(), false));
    }

    public void sendESLab() {
        openUI(100);
    }

    public void sendSceneUI() {
        getClient().announce(UIPacket.sendSceneUI());
    }

    public void sendUIWindow(int op, int npc) {
        getClient().announce(UIPacket.sendUIWindow(op, npc));
    }

    public void setDirection(int z) {
        setDirection(z);
    }

    public void showVisitorResult(int type) {
        getClient().announce(MaplePacketCreator.showVisitorResult(type));
    }

    public void showVisitoKillResult(int total) {
        getPlayer().updateVisitorKills(0, total);
    }

    public void showEventMesssage(int type, int dally, String text) {
        getClient().announce(UIPacket.getMapEffectMsg(text, type, dally));
    }

    public void showPQEffect(int type, String str, String count) {
        getMap().broadcastMessage(getPlayer(), UIPacket.showPQEffect(type, str, count), true);
    }

    public void showScreenShaking(int mapID, boolean stop) {
        getClient().announce(UIPacket.screenShake(mapID, stop));
    }

    public void showSetAction(String str, String act) {
        getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showFieldValue(str, act), true);
    }

    public void updatePartyOneInfo(int questid, String key, String value) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
                if (player == null) continue;
                player.updateOneInfo(questid, key, value);
            }
        }
    }

    public void updatePartyInfoQuest(int questid, String data) {
        this.updatePartyInfoQuest(questid, data, true);
    }

    public void updatePartyInfoQuest(int questid, String data, boolean check) {
        if (getParty() != null) {
            for (MaplePartyCharacter partyCharacter : getParty().getMembers()) {
                MapleCharacter player = getChannelServer().getPlayerStorage().getCharacterById(partyCharacter.getId());
                if (player == null || !player.getInfoQuest(30200).isEmpty() && check) continue;
                player.updateInfoQuest(questid, data);
            }
        }
    }

    /**
     * 充值函数 - 获取
     *
     * @param type 1 = 当前充值金额 2 = 已经消费金额 3 = 总计消费金额 4 = 充值奖励
     * @return
     */
    public int getHyPay(int type) {
        return getPlayer().getHyPay(type);
    }

    /**
     * 充值函数 - 消费, 如需增加请加负号, 如: -100
     *
     * @param pay 1 = 当前充值金额 2 = 已经消费金额 3 = 总计消费金额 4 = 充值奖励
     * @return
     */
    public int addHyPay(int pay) {
        return getPlayer().addHyPay(pay);
    }

    /**
     * 充值函数 - 加减消费奖励
     *
     * @param pay
     * @return
     */
    public int delPayReward(int pay) {
        return getPlayer().delPayReward(pay);
    }

    /**
     * 制作道具
     *
     * @param id     道具ID
     * @param str    力量
     * @param dex    敏捷
     * @param ints   智力
     * @param luk    运气
     * @param watk   物攻
     * @param matk   魔攻
     * @param period 时间
     */
    public void makeitem(int id, short str, short dex, short ints, short luk, short watk, short matk, long period) {
        makeitem(id, str, dex, ints, luk, watk, matk, period, 0);
    }

    /**
     * 制作道具
     *
     * @param id     道具ID
     * @param str    力量
     * @param dex    敏捷
     * @param ints   智力
     * @param luk    运气
     * @param watk   物攻
     * @param matk   魔攻
     * @param period 时间
     * @param state  潜能状态
     */
    public void makeitem(int id, short str, short dex, short ints, short luk, short watk, short matk, long period, int state) {
        makeitem(id, str, dex, ints, luk, watk, matk, period, state, "");
    }

    /**
     * 制作道具
     *
     * @param id     道具ID
     * @param str    力量
     * @param dex    敏捷
     * @param ints   智力
     * @param luk    运气
     * @param watk   物攻
     * @param matk   魔攻
     * @param period 时间
     * @param state  潜能状态
     * @param owner  道具签名
     */
    public void makeitem(int id, short str, short dex, short ints, short luk, short watk, short matk, long period, int state, String owner) {
        if (!canHold(id) || !ItemConstants.isEquip(id)) {
            playerMessage("装备栏空间不足或添加的道具不是装备");
            return;
        }

        Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(id);
        equip.setStr(str);
        equip.setDex(dex);
        equip.setInt(ints);
        equip.setLuk(luk);
        equip.setWatk(watk);
        equip.setMatk(matk);
        equip.setExpiration(period);
        equip.setState((byte) state, false);
        if (!owner.isEmpty()) {
            equip.setOwner(owner);
        }
        equip.setGMLog("脚本获得 " + this.id + " (" + this.id2 + ") 地图: " + getPlayer().getMapId() + " 时间: " + DateUtil.getNowTime());
        MapleInventoryManipulator.addbyItem(getC(), equip.copy());
    }

    public void getLobbyTime(int n2) {
        String string = getPlayer().getOneInfo(42011, "tTime");
        String string2 = getPlayer().getOneInfo(42011, "time");
        if (string == null) {
            string = "600000";
            string2 = "600000";
        }
        int n3 = Integer.valueOf(string);
        int n4 = Integer.valueOf(string2);
        getPlayer().send(MaplePacketCreator.LobbyTimeAction(n2, n4, n3));
    }

    public int isSocketDone(short s2) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Item item = getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(s2);
        if (item != null) {
            Equip equip = (Equip) item;
            if (ii.isCash(equip.getItemId()) || ItemConstants.is饰品(equip.getItemId())) {
                return 2;
            }
            if (equip.getSocket1() >= 0 || (equip.getSocketState() & SocketFlag.已打孔01.getValue()) != 0) {
                return 0;
            }
            equip.setSocket1(0);
            getPlayer().forceUpdateItem(equip);
            return 1;
        }
        return -1;
    }

    public void startQuestTimeLimitTask(int n2, int n3) {
        getPlayer().startQuestTimeLimitTask(n2, n3);
    }

    public void setForcedAction(int n2, int n3) {
        getC().announce(UIPacket.getDirectionEffect(0, null, n2, n3, 0, 0, 0, 0, 0, 0));
    }

    public void setDelay(int n2) {
        getC().announce(UIPacket.getDirectionEffect(1, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setEffectPlay(String string, int n2, int n3, int n4, int n5, int n6, int n7, int n8, int n9) {
        getC().announce(UIPacket.getDirectionEffect(2, string, n2, n3, n4, n5, n6, n7, n8, n9));
    }

    public void setForcedInput(int n2) {
        getC().announce(UIPacket.getDirectionEffect(3, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setPatternInputRequest(String string, int n2, int n3, int n4) {
        getC().announce(UIPacket.getDirectionEffect(4, string, n2, n3, n4, 0, 0, 0, 0, 0));
    }

    public void setCameraMove(int n2, int n3, int n4, int n5) {
        getC().announce(UIPacket.getDirectionEffect(5, null, n2, n3, n4, n5, 0, 0, 0, 0));
    }

    public void setCameraOnCharacter(int n2) {
        getC().announce(UIPacket.getDirectionEffect(6, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setCameraZoom(int n2, int n3, int n4, int n5, int n6) {
        getC().announce(UIPacket.getDirectionEffect(7, null, n2, n3, n4, n5, n6, 0, 0, 0));
    }

    public void setCameraReleaseFromUserPoint() {
        getC().announce(UIPacket.getDirectionEffect(8, null, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setVansheeMode(int n2) {
        getC().announce(UIPacket.getDirectionEffect(9, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setFaceOff(int n2) {
        getC().announce(UIPacket.getDirectionEffect(10, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setMonologue(String string, int n2) {
        getC().announce(UIPacket.getDirectionEffect(11, string, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setMonologueScroll(String string, int n2, int n3, int n4, int n5) {
        getC().announce(UIPacket.getDirectionEffect(12, null, n2, n3, n4, n5, 0, 0, 0, 0));
    }

    public void setAvatarLookSet(short s2) {
    }

    public void setRemoveAdditionalEffect() {
        getC().announce(UIPacket.getDirectionEffect(14, null, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setForcedMove(int n2, int n3) {
        getC().announce(UIPacket.getDirectionEffect(15, null, n2, n3, 0, 0, 0, 0, 0, 0));
    }

    public void setForcedFlip(int n2) {
        getC().announce(UIPacket.getDirectionEffect(16, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void setInputUI(int n2) {
        getC().announce(UIPacket.getDirectionEffect(17, null, n2, 0, 0, 0, 0, 0, 0, 0));
    }

    public void show5thJobEffect() {
        getC().announce(MaplePacketCreator.environmentChange("Effect/5skill.img/screen", 11));
        getC().announce(MaplePacketCreator.environmentChange("Sound/SoundEff.img/5thJobd", 5));
        this.TutInstructionalBalloon("Effect/5skill.img/character_delayed");
    }

    public void setLTime() {
        getPlayer().setLTime();
    }

    public void gainVCraftCore(int n2) {
        getPlayer().gainVCraftCore(n2);
    }

    /**
     * 斗燃消息提示框
     *
     * @param message 消息内容
     * @param second  持续时间, 单位: 秒
     */
    public void showCombustionMessage(String message, int second) {
        getClient().announce(EffectPacket.showCombustionMessage(message, second));
    }

    public int getNewInstanceMapId() {
        return EventScriptManager.getNewInstanceMapId();
    }
}
