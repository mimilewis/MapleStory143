package handling.channel;

import client.MapleCharacter;
import configs.ServerConfig;
import handling.ServerType;
import handling.channel.handler.HiredFisherStorage;
import handling.login.LoginServer;
import handling.netty.ServerConnection;
import handling.world.CheaterData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.event.EventScriptManager;
import server.console.Start;
import server.events.*;
import server.life.PlayerNPC;
import server.maps.AramiaFireWorks;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.market.MarketEngine;
import server.shops.HiredFisher;
import server.shops.HiredMerchant;
import server.squad.MapleSquad;
import server.squad.MapleSquadType;
import tools.ConcurrentEnumMap;
import tools.MaplePacketCreator;

import java.util.*;

public class ChannelServer {

    private static final Logger log = LogManager.getLogger(ChannelServer.class);
    private static final short port_default = 7575;
    private static final Map<Integer, ChannelServer> instances = new HashMap<>();
    public static long serverStartTime;
    private final MapleMapFactory mapFactory;
    private final Map<MapleEventType, MapleEvent> events = new EnumMap<>(MapleEventType.class);
    private final int channel;
    private final int flags = 0;
    private final MarketEngine me = new MarketEngine();
    private final AramiaFireWorks works = new AramiaFireWorks();
    private final Map<MapleSquadType, MapleSquad> mapleSquads = new ConcurrentEnumMap<>(MapleSquadType.class);
    private final List<PlayerNPC> playerNPCs = new LinkedList<>();
    private ServerConnection init;
    private int doubleExp = 1;
    private String ip;
    private short port;
    private boolean shutdown = false, finishedShutdown = false, MegaphoneMuteState = false;
    private PlayerStorage players;
    private MerchantStorage merchants;
    private HiredFisherStorage fishers;
    private EventScriptManager eventSM;
    private int eventmap = -1;

    private ChannelServer(int channel) {
        this.channel = channel;
        mapFactory = new MapleMapFactory(channel);
    }

    public static Set<Integer> getAllInstance() {
        return new HashSet<>(instances.keySet());
    }

    public static ChannelServer newInstance(int channel) {
        return new ChannelServer(channel);
    }

    public static ChannelServer getInstance(int channel) {
        return instances.get(channel);
    }

    public static ArrayList<ChannelServer> getAllInstances() {
        return new ArrayList<>(instances.values());
    }

    public static void startChannel_Main() {
        serverStartTime = System.currentTimeMillis();
        int ch = ServerConfig.CHANNEL_PORTS + 1;
        if (ch > 10) {
            ch = 10;
        }
        for (int i = 0; i < ch; i++) {
            newInstance(i + 1).run_startup_configurations();
        }
    }

    public static Set<Integer> getChannelServer() {
        return new HashSet<>(instances.keySet());
    }

    public static int getChannelCount() {
        return instances.size();
    }

    public static Map<Integer, Integer> getChannelLoad() {
        Map<Integer, Integer> ret = new HashMap<>();
        for (ChannelServer cs : instances.values()) {
            ret.put(cs.getChannel(), cs.getConnectedClients());
        }
        return ret;
    }

    /**
     * 通过角色ID在所有频道中查找角色 @返回 角色
     */
    public static MapleCharacter getCharacterById(int id) {
        for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterById(id);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * 通过角色名字在所有频道中查找角色 @返回 角色
     */
    public static MapleCharacter getCharacterByName(String name) {
        for (ChannelServer cserv_ : ChannelServer.getAllInstances()) {
            MapleCharacter ret = cserv_.getPlayerStorage().getCharacterByName(name);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static short getPort_default() {
        return port_default;
    }

    public void loadEvents() {
        if (!events.isEmpty()) {
            return;
        }
        events.put(MapleEventType.CokePlay, new MapleCoconut(channel, MapleEventType.CokePlay)); //yep, coconut. same shit
        events.put(MapleEventType.Coconut, new MapleCoconut(channel, MapleEventType.Coconut));
        events.put(MapleEventType.Fitness, new MapleFitness(channel, MapleEventType.Fitness));
        events.put(MapleEventType.OlaOla, new MapleOla(channel, MapleEventType.OlaOla));
        events.put(MapleEventType.OxQuiz, new MapleOxQuiz(channel, MapleEventType.OxQuiz));
        events.put(MapleEventType.Snowball, new MapleSnowball(channel, MapleEventType.Snowball));
        events.put(MapleEventType.Survival, new MapleSurvival(channel, MapleEventType.Survival));
    }

    public MapleEvent getEvent(MapleEventType t) {
        return events.get(t);
    }

    public void run_startup_configurations() {
        setChannel(channel); //instances.put
        try {
            players = new PlayerStorage(channel);
            merchants = new MerchantStorage(channel);
            fishers = new HiredFisherStorage(channel);
            Start.getInstance().setupOnlineStatus(channel);
            eventSM = new EventScriptManager(this, ServerConfig.CHANNEL_EVENTS.split(","));
            port = (short) (port_default + channel - 1);
            ip = ServerConfig.WORLD_INTERFACE + ":" + port;
            init = new ServerConnection(port, 0, channel, ServerType.频道服务器);
            init.run();
            log.info("频道: " + channel + " 监听端口: " + port);
            eventSM.init();
            loadEvents();
        } catch (Exception e) {
            throw new RuntimeException("绑定端口: " + port + " 失败 (ch: " + getChannel() + ")", e);
        }
    }

    public void shutdown() {
        if (finishedShutdown) {
            return;
        }
        broadcastPacket(MaplePacketCreator.serverNotice(0, "服务器即将关闭维护"));

        // dc all clients by hand so we get sessionClosed...
        shutdown = true;
        System.out.println("频道 " + channel + " 正在清理事件脚本");

        eventSM.cancel();

        System.out.println("频道 " + channel + " 正在保存所有角色数据");

        getPlayerStorage().disconnectAll();

        System.out.println("频道 " + channel + " 解除绑定端口");

        init.close();

        instances.remove(channel);
        setFinishShutdown();
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        instances.put(channel, this);
        LoginServer.addChannel(channel);
    }

    public MapleMapFactory getMapFactory() {
        return mapFactory;
    }

    public void addPlayer(MapleCharacter chr) {
        getPlayerStorage().registerPlayer(chr);
    }

    public PlayerStorage getPlayerStorage() {
        if (players == null) {
            players = new PlayerStorage(channel);
        }
        return players;
    }

    public void removePlayer(MapleCharacter chr) {
        getPlayerStorage().deregisterPlayer(chr);
    }

    public void removePlayer(int idz, String namez) {
        getPlayerStorage().deregisterPlayer(idz, namez);
    }

    public String getServerMessage() {
        return ServerConfig.LOGIN_SERVERMESSAGE;
    }

    public void setServerMessage(String newMessage) {
        broadcastPacket(MaplePacketCreator.serverMessage(newMessage));
    }

    public void broadcastPacket(byte[] data) {
        getPlayerStorage().broadcastPacket(data);
    }

    public void broadcastSmegaPacket(byte[] data) {
        getPlayerStorage().broadcastSmegaPacket(data);
    }

    public void broadcastGMPacket(byte[] data) {
        getPlayerStorage().broadcastGMPacket(data);
    }

    public String getIP() {
        return ip;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public int getLoadedMaps() {
        return mapFactory.getLoadedMaps();
    }

    public EventScriptManager getEventSM() {
        return eventSM;
    }

    public void reloadEvents() {
        eventSM.cancel();
        eventSM = new EventScriptManager(this, ServerConfig.CHANNEL_EVENTS.split(","));
        eventSM.init();
    }

    /**
     * 游戏经验倍率
     */
    public int getExpRate() {
        return ServerConfig.CHANNEL_RATE_EXP * getDoubleExp();
    }

    public void setExpRate(int rate) {
        ServerConfig.CHANNEL_RATE_EXP = rate;
    }

    /**
     * 游戏金币爆率
     */
    public int getMesoRate() {
        return ServerConfig.CHANNEL_RATE_MESO * getDoubleExp();
    }

    public void setMesoRate(int rate) {
        ServerConfig.CHANNEL_RATE_MESO = rate;
    }

    /**
     * 游戏装备爆率
     */
    public int getDropRate() {
        return ServerConfig.CHANNEL_RATE_DROP * getDoubleExp();
    }

    public void setDropRate(int rate) {
        ServerConfig.CHANNEL_RATE_DROP = rate;
    }

    /**
     * 特殊数据库道具的爆率
     */
    public int getDropgRate() {
        return ServerConfig.CHANNEL_RATE_GLOBALDROP * getDoubleExp();
    }

    public void setDropgRate(int rate) {
        ServerConfig.CHANNEL_RATE_GLOBALDROP = rate;
    }

    /**
     * 双倍经验活动
     */
    public int getDoubleExp() {
        if (doubleExp < 0 || doubleExp > 2) {
            return 1;
        } else {
            return doubleExp;
        }
    }

    public void setDoubleExp(int doubleExp) {
        if (doubleExp < 0 || doubleExp > 2) {
            this.doubleExp = 1;
        } else {
            this.doubleExp = doubleExp;
        }
    }

    public Map<MapleSquadType, MapleSquad> getAllSquads() {
        return Collections.unmodifiableMap(mapleSquads);
    }

    public MapleSquad getMapleSquad(String type) {
        return getMapleSquad(MapleSquadType.valueOf(type.toLowerCase()));
    }

    public MapleSquad getMapleSquad(MapleSquadType type) {
        return mapleSquads.get(type);
    }

    public boolean addMapleSquad(MapleSquad squad, String type) {
        MapleSquadType types = MapleSquadType.valueOf(type.toLowerCase());
        if (types != null && !mapleSquads.containsKey(types)) {
            mapleSquads.put(types, squad);
            squad.scheduleRemoval();
            return true;
        }
        return false;
    }

    public boolean removeMapleSquad(MapleSquadType types) {
        if (types != null && mapleSquads.containsKey(types)) {
            mapleSquads.remove(types);
            return true;
        }
        return false;
    }

    /**
     * 关闭所有雇佣商店
     */
    public void closeAllMerchants() {
        merchants.closeAllMerchants();
    }

    /**
     * 添加雇佣商店
     */
    public int addMerchant(HiredMerchant hMerchant) {
        return merchants.addMerchant(hMerchant);
    }

    /**
     * 删除雇佣商店
     */
    public void removeMerchant(HiredMerchant hMerchant) {
        merchants.removeMerchant(hMerchant);
    }

    /**
     * 检测账号是否开过雇佣商店
     */
    public boolean containsMerchant(int accId) {
        return merchants.containsMerchant(accId);
    }

    /**
     * 检测账号下的玩家是否开过雇佣商店
     */
    public boolean containsMerchant(int accId, int chrId) {
        return merchants.containsMerchant(accId, chrId);
    }

    public List<HiredMerchant> searchMerchant(int itemSearch) {
        return merchants.searchMerchant(itemSearch);
    }

    /**
     * 获取账号下的玩家的雇佣商店信息 返回 雇佣商店
     */
    public HiredMerchant getHiredMerchants(int accId, int chrId) {
        return merchants.getHiredMerchants(accId, chrId);
    }

    public void closeAllFisher() {
        fishers.closeAllFisher();
    }

    public int addFisher(HiredFisher hiredFisher) {
        return fishers.addFisher(hiredFisher);
    }

    public void removeFisher(HiredFisher hiredFisher) {
        fishers.removeFisher(hiredFisher);
    }

    public boolean containsFisher(int accId, int chrId) {
        return fishers.containsFisher(accId, chrId);
    }

    public HiredFisher getHiredFisher(int accId, int chrId) {
        return fishers.getHiredFisher(accId, chrId);
    }

    public void toggleMegaphoneMuteState() {
        this.MegaphoneMuteState = !this.MegaphoneMuteState;
    }

    public boolean getMegaphoneMuteState() {
        return MegaphoneMuteState;
    }

    public int getEvent() {
        return eventmap;
    }

    public void setEvent(int ze) {
        this.eventmap = ze;
    }

    public Collection<PlayerNPC> getAllPlayerNPC() {
        return playerNPCs;
    }

    public void addPlayerNPC(PlayerNPC npc) {
        if (playerNPCs.contains(npc)) {
            return;
        }
        playerNPCs.add(npc);
        getMapFactory().getMap(npc.getMapId()).addMapObject(npc);
    }

    public void removePlayerNPC(PlayerNPC npc) {
        if (playerNPCs.contains(npc)) {
            playerNPCs.remove(npc);
            getMapFactory().getMap(npc.getMapId()).removeMapObject(npc);
        }
    }

    public String getServerName() {
        return ServerConfig.LOGIN_SERVERNAME;
    }

    public void setServerName(String sn) {
        ServerConfig.LOGIN_SERVERNAME = sn;
    }

    public String getTrueServerName() {
        return ServerConfig.LOGIN_SERVERNAME.substring(0, ServerConfig.LOGIN_SERVERNAME.length() - 3);
    }

    public int getPort() {
        return port;
    }

    public void setShutdown() {
        this.shutdown = true;
        log.info("频道 " + channel + " 正在关闭和保存雇佣商店数据信息...");
    }

    public void setFinishShutdown() {
        this.finishedShutdown = true;
        log.info("频道 " + channel + " 已关闭完成.");
    }

    public boolean hasFinishedShutdown() {
        return finishedShutdown;
    }

    public int getTempFlag() {
        return flags;
    }

    public int getConnectedClients() {
        return getPlayerStorage().getConnectedClients();
    }

    public List<CheaterData> getCheaters() {
        List<CheaterData> cheaters = getPlayerStorage().getCheaters();
        Collections.sort(cheaters);
        return cheaters;
    }

    public List<CheaterData> getReports() {
        List<CheaterData> cheaters = getPlayerStorage().getReports();
        Collections.sort(cheaters);
        return cheaters;
    }

    public void broadcastMessage(byte[] message) {
        broadcastPacket(message);
    }

    public void broadcastSmega(byte[] message) {
        broadcastSmegaPacket(message);
    }

    public void broadcastGMMessage(byte[] message) {
        broadcastGMPacket(message);
    }

    public void startMapEffect(String msg, int itemId) {
        startMapEffect(msg, itemId, 10);
    }

    public void startMapEffect(String msg, int itemId, int time) {
        for (MapleMap load : getMapFactory().getAllMaps()) {
            if (load.getCharactersSize() > 0) {
                load.startMapEffect(msg, itemId, time);
            }
        }
    }

    public AramiaFireWorks getFireWorks() {
        return works;
    }

    public boolean isConnected(String name) {
        return getPlayerStorage().getCharacterByName(name) != null;
    }

    public MarketEngine getMarket() {
        return me;
    }
}
