package client;

import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.inventory.*;
import client.skills.*;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.core.JsonProcessingException;
import configs.FishingConfig;
import configs.ServerConfig;
import constants.BattleConstants.PokemonNature;
import constants.BattleConstants.PokemonStat;
import constants.*;
import constants.skills.*;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.login.JobType;
import handling.opcode.EffectOpcode;
import handling.world.*;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.family.MapleFamilyCharacter;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.messenger.MessengerRankingWorker;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import io.netty.channel.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import redis.clients.jedis.Jedis;
import scripting.event.EventInstanceManager;
import scripting.npc.NPCScriptManager;
import server.*;
import server.MapleStatEffect.CancelEffectAction;
import server.Timer;
import server.Timer.BuffTimer;
import server.Timer.MapTimer;
import server.Timer.WorldTimer;
import server.achievement.MapleAchievements;
import server.carnival.MapleCarnivalChallenge;
import server.carnival.MapleCarnivalParty;
import server.cashshop.CashShop;
import server.commands.PlayerGMRank;
import server.life.*;
import server.maps.*;
import server.maps.events.Event_PyramidSubway;
import server.quest.MapleQuest;
import server.reward.RewardDropEntry;
import server.shop.MapleShop;
import server.shop.MapleShopItem;
import server.shops.HiredFisher;
import server.shops.IMaplePlayerShop;
import tools.*;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.packet.*;

import java.awt.*;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapleCharacter extends AnimatedMapleMapObject implements Serializable {

    private static final long serialVersionUID = 845748950829L;
    private static final Logger log = LogManager.getLogger(MapleCharacter.class);
    private final List<MapleProcess> Process = new LinkedList<>();
    private final AtomicInteger cMapCount = new AtomicInteger(1);
    private final AtomicInteger vCoreSkillIndex = new AtomicInteger(0);
    private final AtomicLong exp = new AtomicLong(); //新的经验结果
    private final AtomicLong meso = new AtomicLong();
    private final List<Long> killmonsterExps = new ArrayList<>();
    private final transient PlayerObservable playerObservable = new PlayerObservable(this);
    private String name, chalktext, BlessOfFairy_Origin, BlessOfEmpress_Origin, teleportname, chairMsg = "";
    private long lastfametime, keydown_skill, nextConsume, pqStartTime, lastDragonBloodTime, lastBerserkTime, lastRecoveryTime, lastSummonTime, mapChangeTime, lastFishingTime,
            lastFairyTime, lastmonsterCombo, lastHPTime, lastMPTime, lastExpirationTime, lastBlessOfDarknessTime, lastRecoveryTimeEM, lastCritStorageTime, lastForceTime, lastAegisTime;
    private byte gmLevel, accountGender, gender, initialSpawnPoint, skinColor, guildrank = 5, allianceRank = 5, world, fairyExp, subcategory, hairBaseColor = -1, hairProbColor, hairMixedColor;
    private short level, mulung_energy, fatigue, hpApUsed, job, remainingAp, scrolledPosition, availableCP, totalCP;
    private int accountid, id, hair, face, mapid, fame, pvpExp, pvpPoints, totalWins, totalLosses, hitcountbat, batcount, guildid = 0, fallcounter, /*maplepoints, acash,*/
            chair, chairType, chairMeso, itemEffect, itemEffectType, points, vpoints, criticalgrowth, rank = 1, rankMove = 0, jobRank = 1, jobRankMove = 0, marriageId, marriageItemId, touchedrune, monsterCombo, currentrep, totalrep,
            coconutteam, followid, gachexp, challenge, guildContribution = 0, todayonlinetime, totalonlinetime, weaponPoint = -1;
    private Point old;
    private int[] wishlist, rocks, savedLocations, regrocks, hyperrocks, remainingSp = new int[10];
    private transient AtomicInteger inst, insd;
    private List<Integer> lastmonthfameids, lastmonthbattleids;
    private List<MapleDoor> doors;
    private List<MechDoor> mechDoors;
    private MaplePet[] spawnPets; //召唤中的宠物
    private transient List<MapleShopItem> rebuy; //回购
    private MapleImp[] imps;
    private transient Set<MapleMonster> controlled;
    private transient Set<MapleMapObject> visibleMapObjects;
    private transient ReentrantReadWriteLock visibleMapObjectsLock;
    private transient ReentrantReadWriteLock summonsLock;
    private transient ReentrantReadWriteLock controlledLock;
    private transient ReentrantReadWriteLock itemLock;
    private transient ReentrantReadWriteLock addhpmpLock;
    private transient Lock rLCheck;
    private transient MapleAndroid android;
    private Map<Integer, MapleQuestStatus> quests;
    private Map<Integer, String> questinfo;
    private Map<String, String> keyValue; //新增角色信息
    private Map<Integer, SkillEntry> skills; //角色技能
    private Map<Integer, SkillEntry> linkSkills;
    private Map<Integer, Pair<Integer, SkillEntry>> sonOfLinkedSkills;
    private InnerSkillEntry[] innerSkills; //角色内在能力技能
    private MaplePlayerBuffManager buffManager;
    private transient ArrayList<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effects;
    private transient Map<Integer, MapleCoolDownValueHolder> coolDowns;
    private transient Map<MapleDisease, MapleDiseaseValueHolder> diseases;
    private transient List<MapleSummon> summons;
    private Map<ReportType, Integer> reports;
    private CashShop cs;
    private BuddyList buddylist;
    private MonsterBook monsterbook;
    private transient CheatTracker anticheat; //外挂检测系统
    private transient MapleLieDetector antiMacro; //测谎仪系统
    private transient MapleClient client;
    private transient MapleParty party;
    private PlayerStats stats;
    private MapleCharacterCards characterCard;
    private transient MapleMap map;
    private transient MapleShop shop;
    private transient MapleDragon dragon;
    private transient MapleExtractor extractor;
    private transient RockPaperScissors rps;
    private transient MapleLittleWhite lw;
    private MapleStorage storage;
    private transient MapleTrade trade;
    private MapleMount mount;
    private List<Integer> finishedAchievements;
    private MapleMessenger messenger;
    private byte[] petStore;
    private transient IMaplePlayerShop playerShop;
    private boolean invincible, canTalk, followinitiator, followon, smega, hasSummon;
    private MapleGuildCharacter mgc;
    private MapleFamilyCharacter mfc;
    private transient EventInstanceManager eventInstance;
    private MapleInventory[] inventory;
    private SkillMacro[] skillMacros = new SkillMacro[5];
    private EnumMap<MapleTraitType, MapleTrait> traits;
    private Battler[] battlers = new Battler[6];
    private List<Battler> boxed;
    private MapleKeyLayout keylayout;
    private MapleQuickSlot quickslot;
    private transient ScheduledFuture<?> mapTimeLimitTask;
    private transient ScheduledFuture<?> chalkSchedule; //小黑板处理线程
    private transient ScheduledFuture<?> questTimeLimitTask; //小黑板处理线程
    private transient Event_PyramidSubway pyramidSubway = null;
    private transient List<Integer> pendingExpiration = null;
    private transient Map<Integer, SkillEntry> pendingSkills = null;
    private transient Map<Integer, Integer> linkMobs;
    private transient PokemonBattle battle;
    private boolean changed_wishlist, changed_trocklocations, changed_skillmacros, changed_achievements, changed_savedlocations, changed_pokemon, changed_questinfo, changed_skills, changed_reports, changed_vcores,
            changed_extendedSlots, changed_innerSkills, changed_keyValue;
    private int decorate; //魔族之纹
    private int vip; //会员等级
    private Timestamp viptime; //会员时间
    private int titleEffect; //称号效果
    private boolean isbanned = false; //检测是否封号
    private int beans; //豆豆信息
    private int warning; //使用非法程序警告次数
    private int reborns, reborns1, reborns2, reborns3, apstorage; //转身系统
    private int honor; //荣誉系统
    private Timestamp createDate; //角色创建的时间
    //好感度系统
    private int love; //角色好感度
    private long lastlovetime; //最后加好感度的时间
    private Map<Integer, Long> lastdayloveids; //今天给别人加好感度的列表 角色ID 给角色加好感的时间
    //新增变量
    private int playerPoints; //角色积分
    private int playerEnergy; //角色活力值
    //新增PVP属性
    private transient MaplePvpStats pvpStats;
    private int pvpDeaths; //死亡次数
    private int pvpKills; //杀敌次数
    private int pvpVictory; //连续击杀
    //药剂罐系统
    private MaplePotionPot potionPot;
    //龙的传人宝盒系统
    private MapleCoreAura coreAura;
    //检测是否正在保存角色数据
    private boolean isSaveing;
    //角色一些特殊变量数据
    private PlayerSpecialStats specialStats;
    private Timestamp todayonlinetimestamp;
    //角色连续击杀怪物处理
    private int mobKills; //连续击杀怪物的数量
    private long lastMobKillTime; //判断连续击杀怪物的时间间隔
    //圣骑士攻击的属性
    private transient Element elements = null;
    //自动重复使用的BUFF
    private long lastRepeatEffectTime;
    //火焰传动处理
    private int flameMapId; //设置的地图
    private Point flamePoint = null; //设置需要移动的坐标位置
    //角色1次对怪物的最大伤害
    private long totDamageToMob;
    //角色最后使用符文的时间
    private long lastFuWenTime;
    //尖兵扣除电池时间的检测
//    private long checkXenonBatteryTime; //检测电池在60秒内使用的时间
    //角色使用终极魔方道具的处理
    private transient ModifyItemPotential ItemPotential;
    //最后使用的攻击技能ID
    private int lastAttackSkillId;
    //检测林之灵组队被动BUFF效果
    private long checkPartyPassiveTime;
    //检测夜行者攻击命中次数
    private int attackHit;
    //NPC间隔时间
    private long lasttime = 0;
    private long currenttime = 0;
    private long deadtime = 300L;
    // 名流爆击
    private transient ScheduledFuture<?> celebrityCrit;
    // 幸运钱开关
    private boolean luckymoney = false;
    private transient Channel chatSession;
    // 自定义积分
    private Map<String, Integer> credit;
    // 灵魂武器点数
    private short soulcount = 0;
    // 客户端进程信息
    private long lastCheckProcess;
    private int friendshiptoadd;
    private int[] friendshippoints = new int[5];
    // 每日签到状态
    private MapleSigninStatus siginStatus;
    // 特效
    private List<Integer> effectSwitch;
    // 上次保存时间
    private Long lastSavetime = 0L;
    //怪怪图鉴
    private Map<Integer, MonsterFamiliar> familiars;
    private MonsterFamiliar summonedFamiliar;
    private long logintime;
    private long onlineTime;
    private WeakReference<MapleReactor> reactor = new WeakReference<>(null);
    private transient MapleCarnivalParty carnivalParty;
    private transient Deque<MapleCarnivalChallenge> pendingCarnivalRequests;
    private int reviveCount = -1;
    private boolean attclimit = false;
    private List<Integer> damSkinList;
    private int transWarpTicket;
    private Map<Byte, List<Integer>> extendedSlots;
    private long ltime = 0;
    private Map<Integer, VCoreSkillEntry> vCoreSkills;
    private transient int enchant;
    private transient long lastUseVSkillTime = 0;
    private int buffValue = 0;
    private long lastComboTime = 0;
    private HiredFisher hiredFisher;

    public MapleCharacter() {

    }

    private MapleCharacter(boolean ChannelServer) {
        setStance(0);
        setPosition(new Point(0, 0));
        inventory = new MapleInventory[MapleInventoryType.values().length];
        for (MapleInventoryType type : MapleInventoryType.values()) {
            inventory[type.ordinal()] = new MapleInventory(type);
        }
        keyValue = new LinkedHashMap<>(); //新增角色信息处理
        questinfo = new LinkedHashMap<>();
        quests = new LinkedHashMap<>(); // Stupid erev quest.
        skills = new LinkedHashMap<>(); //角色技能
        linkSkills = new LinkedHashMap<>();
        sonOfLinkedSkills = new LinkedHashMap<>();
        innerSkills = new InnerSkillEntry[3]; //角色内在能力技能 默认只能3个
        stats = new PlayerStats(); //角色属性计算
        characterCard = new MapleCharacterCards(); //角色卡系统
        for (int i = 0; i < remainingSp.length; i++) {
            remainingSp[i] = 0;
        }
        traits = new EnumMap<>(MapleTraitType.class);
        for (MapleTraitType t : MapleTraitType.values()) {
            traits.put(t, new MapleTrait(t));
        }
        spawnPets = new MaplePet[3]; //当前召唤的宠物
        specialStats = new PlayerSpecialStats(); //角色特殊变量处理
        specialStats.resetSpecialStats();
        familiars = new TreeMap<>();
        damSkinList = new ArrayList<>();
        if (ChannelServer) {
            isSaveing = false;
            changed_reports = false;
            changed_skills = false;
            changed_achievements = false;
            changed_wishlist = false;
            changed_trocklocations = false;
            changed_skillmacros = false;
            changed_savedlocations = false;
            changed_pokemon = false;
            changed_extendedSlots = false;
            changed_questinfo = false;
            changed_innerSkills = false;
            changed_keyValue = false;
            changed_vcores = false;
            scrolledPosition = 0;
            criticalgrowth = 0;
            mulung_energy = 0;
            keydown_skill = 0;
            nextConsume = 0;
            pqStartTime = 0;
            fairyExp = 0;
            mapChangeTime = 0;
            lastmonsterCombo = 0;
            monsterCombo = 0;
            lastRecoveryTime = 0;
            lastDragonBloodTime = 0;
            lastBerserkTime = 0;
            lastFishingTime = 0;
            lastFairyTime = 0;
            lastHPTime = 0;
            lastMPTime = 0;
            lastExpirationTime = 0; //装备到期检测
            lastBlessOfDarknessTime = 0; //黑暗祝福检测
            lastRecoveryTimeEM = 0; //恶魔恢复效果
            lastRepeatEffectTime = 0; //自动重复BUFF的检测
            lastCritStorageTime = 0; //暴击蓄能时间检测
            old = new Point(0, 0);
            coconutteam = 0;
            followid = 0;
            marriageItemId = 0;
            fallcounter = 0;
            challenge = 0;
            lastSummonTime = 0;
            hasSummon = false;
            invincible = false;
            canTalk = true;
            followinitiator = false;
            followon = false;
            rebuy = new ArrayList<>(); //商店回购
            linkMobs = new HashMap<>();
            finishedAchievements = new ArrayList<>();
            reports = new EnumMap<>(ReportType.class);
            teleportname = "";
            smega = true;
            petStore = new byte[3];
            for (int i = 0; i < petStore.length; i++) {
                petStore[i] = (byte) -1;
            }
            wishlist = new int[12]; //V.112修改为12个
            rocks = new int[10];
            regrocks = new int[5];
            hyperrocks = new int[13];
            imps = new MapleImp[3];
            friendshippoints = new int[5];
            boxed = new ArrayList<>();
            extendedSlots = new HashMap<>();
            effects = new ArrayList<>();
            coolDowns = new LinkedHashMap<>();
            diseases = new ConcurrentEnumMap<>(MapleDisease.class);
            inst = new AtomicInteger(0);// 1 = NPC/ Quest, 2 = Duey, 3 = Hired Merch store, 4 = Storage
            insd = new AtomicInteger(-1);
            keylayout = new MapleKeyLayout();
            quickslot = new MapleQuickSlot();
            doors = new ArrayList<>();
            mechDoors = new ArrayList<>();
            itemLock = new ReentrantReadWriteLock();
            rLCheck = itemLock.readLock();
            controlled = new LinkedHashSet<>();
            controlledLock = new ReentrantReadWriteLock();
            summons = new LinkedList<>();
            summonsLock = new ReentrantReadWriteLock();
            visibleMapObjects = new LinkedHashSet<>();
            visibleMapObjectsLock = new ReentrantReadWriteLock();
            addhpmpLock = new ReentrantReadWriteLock();

            savedLocations = new int[SavedLocationType.values().length];
            for (int i = 0; i < SavedLocationType.values().length; i++) {
                savedLocations[i] = -1;
            }
            todayonlinetimestamp = new Timestamp(System.currentTimeMillis());
            buffManager = new MaplePlayerBuffManager(getName());
            credit = new LinkedHashMap<>();
            effectSwitch = new ArrayList<>();
            pendingCarnivalRequests = new LinkedList<>();
            vCoreSkills = new LinkedHashMap<>();
        }
    }

    /**
     * 新角色默认的数据
     */
    public static MapleCharacter getDefault(MapleClient client, JobType type) {
        MapleCharacter ret = new MapleCharacter(false);
        ret.client = client;
        ret.map = null;
        ret.exp.set(0);
        ret.gmLevel = 0;
        ret.job = (short) type.jobId;
        ret.meso.set(10000);
        ret.level = 1;
        ret.remainingAp = 0;
        ret.fame = 0; //人气值
        ret.love = 0; //好感度
        ret.accountid = client.getAccID();
        ret.buddylist = new BuddyList((byte) 20);

        ret.stats.str = 12;
        ret.stats.dex = 5;
        ret.stats.int_ = 4;
        ret.stats.luk = 4;
        ret.stats.maxhp = 50;
        ret.stats.hp = 50;
        ret.stats.maxmp = 50;
        ret.stats.mp = 50;
        ret.gachexp = 0;
        ret.friendshiptoadd = 0;
        ret.friendshippoints = new int[]{0, 0, 0, 0, 0};

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
            ps.setInt(1, ret.accountid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                ret.client.setAccountName(rs.getString("name"));
                ret.accountGender = rs.getByte("gender");
                ret.points = rs.getInt("points");
                ret.vpoints = rs.getInt("vpoints");
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error getting character default" + e);
        }
        return ret;
    }

    public static MapleCharacter ReconstructChr(CharacterTransfer ct, MapleClient client, boolean isChannel) {
        MapleCharacter ret = new MapleCharacter(true); // Always true, it's change channel
        ret.client = client;
        if (client != null) {
            client.setAccID(ct.accountid);
            client.setAccountName(ct.accountname);
            client.setTempIP(ct.tempIP);
        }
        if (!isChannel && client != null) {
            ret.client.setChannel(ct.channel);
        }
        ret.id = ct.characterid;
        ret.name = ct.name;
        ret.level = ct.level;
        ret.fame = ct.fame; //人气值
        ret.love = ct.love; //好感度

        ret.stats.str = ct.str;
        ret.stats.dex = ct.dex;
        ret.stats.int_ = ct.int_;
        ret.stats.luk = ct.luk;
        ret.stats.maxhp = ct.maxhp;
        ret.stats.maxmp = ct.maxmp;
        ret.stats.hp = ct.hp;
        ret.stats.mp = ct.mp;

        ret.chalktext = ct.chalkboard;
        ret.gmLevel = ct.gmLevel;
        ret.exp.set(ret.level > ret.getMaxLevelForSever() ? 0 : ct.exp);
        ret.hpApUsed = ct.hpApUsed;
        ret.remainingSp = ct.remainingSp;
        ret.remainingAp = ct.remainingAp;
        ret.meso.set(ct.meso);
        ret.skinColor = ct.skinColor;
        ret.accountGender = ct.accountGender;
        ret.gender = ct.gender;
        ret.job = ct.job;
        ret.hair = ct.hair;
        ret.hairBaseColor = ct.hairBaseColor;
        ret.hairMixedColor = ct.hairMixedColor;
        ret.hairProbColor = ct.hairProbColor;
        ret.face = ct.face;
        ret.accountid = ct.accountid;
        ret.totalWins = ct.totalWins;
        ret.totalLosses = ct.totalLosses;
        ret.mapid = ct.mapid;
        ret.initialSpawnPoint = ct.initialSpawnPoint;
        ret.world = ct.world;
        ret.guildid = ct.guildid;
        ret.guildrank = ct.guildrank;
        ret.guildContribution = ct.guildContribution;
        ret.allianceRank = ct.alliancerank;
        ret.points = ct.points;
        ret.vpoints = ct.vpoints;
        ret.fairyExp = ct.fairyExp;
        ret.marriageId = ct.marriageId;
        ret.currentrep = ct.currentrep;
        ret.totalrep = ct.totalrep;
        ret.gachexp = ct.gachexp;
        ret.pvpExp = ct.pvpExp;
        ret.pvpPoints = ct.pvpPoints;
        ret.decorate = ct.decorate; //魔族之纹
        ret.beans = ct.beans; //豆豆数量
        ret.warning = ct.warning; //警告次数
        //转身系统
        ret.reborns = ct.reborns;
        ret.reborns1 = ct.reborns1;
        ret.reborns2 = ct.reborns2;
        ret.reborns3 = ct.reborns3;
        ret.apstorage = ct.apstorage;
        //荣誉系统
        ret.honor = ct.honor;
        //会员系统
        ret.vip = ct.vip; //会员等级
        ret.viptime = ct.viptime; //会员时间
        ret.playerPoints = ct.playerPoints; //角色积分
        ret.playerEnergy = ct.playerEnergy; //角色活力值
        ret.pvpDeaths = ct.pvpDeaths; //死亡次数
        ret.pvpKills = ct.pvpKills; //杀敌次数
        ret.pvpVictory = ct.pvpVictory; //连续击杀
        ret.makeMFC(ct.familyid, ct.seniorid, ct.junior1, ct.junior2);
        if (ret.guildid > 0) {
            ret.mgc = new MapleGuildCharacter(ret);
        }
        ret.fatigue = ct.fatigue;
        ret.buddylist = new BuddyList(ct.buddysize);
        ret.subcategory = ct.subcategory;
        ret.friendshiptoadd = ct.friendshiptoadd;
        ret.friendshippoints = ct.friendshippoints;
        ret.soulcount = ct.soulcount;
        ret.inventory = ct.inventorys;
        ret.ltime = ct.ltime;

        int messengerid = ct.messengerid;
        if (messengerid > 0) {
            ret.messenger = WorldMessengerService.getInstance().getMessenger(messengerid);
        }
        int partyid = ct.partyid;
        if (partyid >= 0) {
            MapleParty party = WorldPartyService.getInstance().getParty(partyid);
            if (party != null && party.getMemberById(ret.id) != null) {
                ret.party = party;
            }
        }

        MapleQuestStatus queststatus_from;
        for (Entry<Integer, MapleQuestStatus> qs : ct.Quest.entrySet()) {
            queststatus_from = qs.getValue();
            queststatus_from.setQuest(qs.getKey());
            ret.quests.put(qs.getKey(), queststatus_from);
        }
        for (Entry<Integer, SkillEntry> qs : ct.skills.entrySet()) {
            ret.skills.put(qs.getKey(), qs.getValue());
            if (SkillConstants.isLinkSkills(qs.getKey()) && ret.linkSkills.size() < 12) {
                ret.linkSkills.put(qs.getKey(), qs.getValue());
            }
        }
        ret.finishedAchievements.addAll(ct.finishedAchievements);
        for (Battler zz : ct.boxed) {
            zz.setStats();
            ret.boxed.add(zz);
        }
        for (Entry<MapleTraitType, Integer> t : ct.traits.entrySet()) {
            ret.traits.get(t.getKey()).setExp(t.getValue());
        }
        for (Entry<Byte, Integer> qs : ct.reports.entrySet()) {
            ret.reports.put(ReportType.getById(qs.getKey()), qs.getValue());
        }
        ct.sonOfLinedSkills.forEach((key, value) -> ret.sonOfLinkedSkills.put(key, value));
        ct.vcoreskills.forEach(ret.vCoreSkills::put);

        ret.innerSkills = ct.innerSkills;
        ret.monsterbook = new MonsterBook(ct.mbook, ret);
        ret.BlessOfFairy_Origin = ct.BlessOfFairy;
        ret.BlessOfEmpress_Origin = ct.BlessOfEmpress;
        ret.skillMacros = ct.skillmacro;
        ret.battlers = ct.battlers;
        for (Battler b : ret.battlers) {
            if (b != null) {
                b.setStats();
            }
        }
        ret.petStore = ct.petStore;
        ret.keylayout = new MapleKeyLayout(ct.keymap);
        ret.quickslot = new MapleQuickSlot(ct.quickslot);
        ret.keyValue = ct.KeyValue;
        ret.questinfo = ct.InfoQuest;
        ret.savedLocations = ct.savedlocation;
        ret.wishlist = ct.wishlist;
        ret.rocks = ct.rocks;
        ret.regrocks = ct.regrocks;
        ret.hyperrocks = ct.hyperrocks;
        ret.buddylist.loadFromTransfer(ct.buddies);
        // ret.lastfametime
        // ret.lastmonthfameids
        ret.keydown_skill = 0; // Keydown skill can't be brought over
        ret.lastfametime = ct.lastfametime;
        ret.lastmonthfameids = ct.famedcharacters;
        ret.lastmonthbattleids = ct.battledaccs;
        ret.extendedSlots = ct.extendedSlots;
        ret.lastlovetime = ct.lastLoveTime;
        ret.lastdayloveids = ct.loveCharacters;
        ret.storage = ct.storage;
        ret.pvpStats = ct.pvpStats; //Pvp另外计算的属性
        ret.potionPot = ct.potionPot; //药剂罐信息
        ret.coreAura = ct.coreAura; //宝盒信息
        ret.specialStats = ct.SpecialStats; //特殊属性
        ret.cs = ct.cs;
        ret.imps = ct.imps;
        ret.anticheat = ct.anticheat; //外挂检测系统
        ret.anticheat.start(ret.getId());
        ret.antiMacro = ct.antiMacro; //测谎仪系统
        ret.rebuy = ct.rebuy;
        ret.mount = new MapleMount(ret, ct.mount_itemid, PlayerStats.getSkillByJob(1004, ret.job), ct.mount_Fatigue, ct.mount_level, ct.mount_exp);
        ret.todayonlinetime = ct.todayonlinetime;
        ret.totalonlinetime = ct.totalonlinetime;
        ret.weaponPoint = ct.weaponPoint;
        ret.credit = ct.credit;
        ret.effectSwitch = ct.effectSwitch;
        ret.familiars = ct.familiars;

        if (isChannel) {
            MapleMapFactory mapFactory = ChannelServer.getInstance(ct.channel).getMapFactory();
            ret.map = mapFactory.getMap(ret.mapid);
            if (ret.map == null) { //char is on a map that doesn't exist warp it to henesys
                ret.map = mapFactory.getMap(100000000);
            } else if (ret.map.getForcedReturnId() != 999999999 && ret.map.getForcedReturnMap() != null) {
                ret.map = ret.map.getForcedReturnMap();
            }
            MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
            if (portal == null) {
                portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                ret.initialSpawnPoint = 0;
            }
            ret.setPosition(portal.getPosition());
            ret.characterCard.loadCards(client, true);
            ret.stats.recalcLocalStats(true, ret);
        } else {
            ret.messenger = null;
        }


        return ret;
    }

    /**
     * 加载角色信息
     */
    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver) {
        return loadCharFromDB(charid, client, channelserver, null);
    }

    public static MapleCharacter loadCharFromDB(int charid, MapleClient client, boolean channelserver, Map<Integer, CardData> cads) {
        MapleCharacter ret = new MapleCharacter(channelserver);
        ret.client = client;
        ret.id = charid;

        PreparedStatement ps = null;
        PreparedStatement pse;
        ResultSet rs = null;

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                throw new RuntimeException("加载角色失败原因(角色没有找到).");
            }
            ret.name = rs.getString("name");
            ret.level = rs.getShort("level");
            ret.fame = rs.getInt("fame"); //人气值
            ret.love = rs.getInt("love"); //好感度

            ret.stats.str = rs.getShort("str");
            ret.stats.dex = rs.getShort("dex");
            ret.stats.int_ = rs.getShort("int");
            ret.stats.luk = rs.getShort("luk");
            ret.job = rs.getShort("job");
            ret.stats.maxhp = rs.getInt("maxhp");
            ret.stats.maxmp = JobConstants.isNotMpJob(ret.job) ? GameConstants.getMPByJob(ret.job) : rs.getInt("maxmp");
            ret.stats.hp = rs.getInt("hp");
            ret.stats.mp = rs.getInt("mp");
            ret.gmLevel = rs.getByte("gm");
            ret.exp.set(ret.level >= ret.getMaxLevelForSever() ? 0 : rs.getLong("exp"));
            ret.hpApUsed = rs.getShort("hpApUsed");
            String[] sp = rs.getString("sp").split(",");
            for (int i = 0; i < ret.remainingSp.length; i++) {
                ret.remainingSp[i] = Integer.parseInt(sp[i]);
            }
            ret.remainingAp = rs.getShort("ap");
            ret.meso.set(rs.getLong("meso"));
            ret.skinColor = rs.getByte("skincolor");
            ret.gender = rs.getByte("gender");

            // 读取数据前检测一下玩家的发型脸型是否存在，免得客户端出现问题
            int hair = rs.getInt("hair");
            int face = rs.getInt("face");
            if (!MapleItemInformationProvider.getInstance().loadHairFace(hair)) {
                hair = ret.gender == 0 ? 30000 : 31000;
            }
            if (!MapleItemInformationProvider.getInstance().loadHairFace(face)) {
                face = ret.gender == 0 ? 20000 : 21000;
            }
            ret.hair = hair;
            ret.face = face;
            ret.hairBaseColor = rs.getByte("basecolor");
            ret.hairMixedColor = rs.getByte("mixedcolor");
            ret.hairProbColor = rs.getByte("probcolor");

            ret.accountid = rs.getInt("accountid");
            if (client != null) {
                client.setAccID(ret.accountid);
            }
            ret.mapid = rs.getInt("map");
            ret.initialSpawnPoint = rs.getByte("spawnpoint");
            ret.world = rs.getByte("world");
            ret.guildid = rs.getInt("guildid");
            ret.guildrank = rs.getByte("guildrank");
            ret.allianceRank = rs.getByte("allianceRank");
            ret.guildContribution = rs.getInt("guildContribution");
            ret.totalWins = rs.getInt("totalWins");
            ret.totalLosses = rs.getInt("totalLosses");
            ret.currentrep = rs.getInt("currentrep");
            ret.totalrep = rs.getInt("totalrep");
            ret.makeMFC(rs.getInt("familyid"), rs.getInt("seniorid"), rs.getInt("junior1"), rs.getInt("junior2"));
            if (ret.guildid > 0 && client != null) {
                ret.mgc = new MapleGuildCharacter(ret);
            }
            ret.gachexp = rs.getInt("gachexp");
            ret.buddylist = new BuddyList(rs.getByte("buddyCapacity"));
            ret.subcategory = rs.getByte("subcategory");
            ret.mount = new MapleMount(ret, 0, PlayerStats.getSkillByJob(1004, ret.job), (byte) 0, (byte) 1, 0);
            //排名系统
            ret.rank = rs.getInt("rank");
            ret.rankMove = rs.getInt("rankMove");
            ret.jobRank = rs.getInt("jobRank");
            ret.jobRankMove = rs.getInt("jobRankMove");
            //结婚的对象角色ID
            ret.marriageId = rs.getInt("marriageId");
            //疲劳度
            ret.fatigue = rs.getShort("fatigue");
            //PVP系统
            ret.pvpExp = rs.getInt("pvpExp");
            ret.pvpPoints = rs.getInt("pvpPoints");
            //倾向系统
            for (MapleTrait t : ret.traits.values()) {
                t.setExp(rs.getInt(t.getType().name()));
            }
            //魔族之纹
            ret.decorate = rs.getInt("decorate");
            //豆豆数量
            ret.beans = rs.getInt("beans");
            //非法程序使用警告次数
            ret.warning = rs.getInt("warning");
            //转身系统
            ret.reborns = rs.getInt("reborns");
            ret.reborns1 = rs.getInt("reborns1");
            ret.reborns2 = rs.getInt("reborns2");
            ret.reborns3 = rs.getInt("reborns3");
            ret.apstorage = rs.getInt("apstorage");
            //荣誉系统
            ret.honor = rs.getInt("honor");
            //新增变量
            ret.playerPoints = rs.getInt("playerPoints");
            ret.playerEnergy = rs.getInt("playerEnergy");
            //Pvp变量
            ret.pvpDeaths = rs.getInt("pvpDeaths"); //死亡次数
            ret.pvpKills = rs.getInt("pvpKills"); //杀敌次数
            ret.pvpVictory = rs.getInt("pvpVictory"); //连续击杀次数
            //Vip 会员信息
            ret.vip = rs.getInt("vip");
            ret.viptime = rs.getTimestamp("viptime");
            ret.todayonlinetime = rs.getInt("todayonlinetime");
            ret.totalonlinetime = rs.getInt("totalonlinetime");
            ret.weaponPoint = rs.getInt("wp");
            ret.friendshiptoadd = rs.getInt("friendshiptoadd");
            String[] points = rs.getString("friendshippoints").split(",");
            for (int i = 0; i < 5; i++) {
                ret.friendshippoints[i] = Integer.parseInt(points[i]);
            }
            if (channelserver && client != null) {
                //Pvp属性
                ret.pvpStats = MaplePvpStats.loadOrCreateFromDB(ret.accountid);
                //外挂检测系统
                ret.anticheat = new CheatTracker(ret.getId());
                //测谎仪系统
                ret.antiMacro = new MapleLieDetector(ret.getId());
                //加载角色地图信息
                MapleMapFactory mapFactory = ChannelServer.getInstance(client.getChannel()).getMapFactory();
                ret.map = mapFactory.getMap(ret.mapid);
                if (ret.map == null) { //如果地图存在就设置地图为射手
                    ret.map = mapFactory.getMap(100000000);
                }
                //地图的传送点
                MaplePortal portal = ret.map.getPortal(ret.initialSpawnPoint);
                if (portal == null) {
                    portal = ret.map.getPortal(0); // char is on a spawnpoint that doesn't exist - select the first spawnpoint instead
                    ret.initialSpawnPoint = 0;
                }
                ret.setPosition(portal.getPosition());
                //组队信息
                int partyid = rs.getInt("party");
                if (partyid >= 0) {
                    MapleParty party = WorldPartyService.getInstance().getParty(partyid);
                    if (party != null && party.getMemberById(ret.id) != null) {
                        ret.party = party;
                    }
                }
                //宠物信息
                String[] pets = rs.getString("pets").split(",");
                for (int i = 0; i < ret.petStore.length; i++) {
                    ret.petStore[i] = Byte.parseByte(pets[i]);
                }
                rs.close();
                ps.close();
                //成就系统
                ps = con.prepareStatement("SELECT * FROM achievements WHERE accountid = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.finishedAchievements.add(rs.getInt("achievementid"));
                }
                ps.close();
                rs.close();
                //举报信息
                ps = con.prepareStatement("SELECT * FROM reports WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    if (ReportType.getById(rs.getByte("type")) != null) {
                        ret.reports.put(ReportType.getById(rs.getByte("type")), rs.getInt("count"));
                    }
                }
                ret.setLTime();
            }
            rs.close();
            ps.close();
            /*
             * 加载角色卡系统
             */
            if (cads != null) {
                ret.characterCard.setCards(cads);
            } else if (client != null) {
                ret.characterCard.loadCards(client, channelserver);
            }
            //加载角色的一些特殊处理信息
            ps = con.prepareStatement("SELECT * FROM character_keyvalue WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString("key") == null) {
                    continue;
                }
                ret.keyValue.put(rs.getString("key"), rs.getString("value"));
            }
            rs.close();
            ps.close();
            //加载任务完成信息
            ps = con.prepareStatement("SELECT * FROM questinfo WHERE accountid = ? OR characterid = ?");
            ps.setInt(1, ret.accountid);
            ps.setInt(2, charid);
            rs = ps.executeQuery();
            while (rs.next()) {
                ret.questinfo.put(rs.getInt("quest"), rs.getString("customData"));
            }
            rs.close();
            ps.close();
            //加载任务信息
            ps = con.prepareStatement("SELECT * FROM queststatus WHERE characterid = ?");
            ps.setInt(1, charid);
            rs = ps.executeQuery();
            pse = con.prepareStatement("SELECT * FROM queststatusmobs WHERE queststatusid = ?");
            while (rs.next()) {
                int id = rs.getInt("quest");
                MapleQuest q = MapleQuest.getInstance(id);
                byte stat = rs.getByte("status");
                if ((stat == 1 || stat == 2) && channelserver && (q == null || q.isBlocked())) { //bigbang
                    continue;
                }
                if (stat == 1 && channelserver && !q.canStart(ret, null)) { //bigbang
                    continue;
                }
                MapleQuestStatus status = new MapleQuestStatus(q, stat);
                long cTime = rs.getLong("time");
                if (cTime > -1) {
                    status.setCompletionTime(cTime * 1000);
                }
                status.setForfeited(rs.getInt("forfeited"));
                status.setCustomData(rs.getString("customData"));
                ret.quests.put(id, status);
                pse.setInt(1, rs.getInt("queststatusid"));
                try (ResultSet rsMobs = pse.executeQuery()) {
                    while (rsMobs.next()) {
                        status.setMobKills(rsMobs.getInt("mob"), rsMobs.getInt("count"));
                    }
                }
            }
            rs.close();
            ps.close();
            pse.close();
            //====================================================================================
            if (channelserver) {
                //怪物书系统 现在盛大已取消
                ret.monsterbook = MonsterBook.loadCards(ret.accountid, ret);
                //加载角色包裹的最大数量
                ps = con.prepareStatement("SELECT * FROM inventoryslot WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    rs.close();
                    ps.close();
                    throw new RuntimeException("No Inventory slot column found in SQL. [inventoryslot]");
                } else {
                    ret.getInventory(MapleInventoryType.EQUIP).setSlotLimit(rs.getShort("equip"));
                    ret.getInventory(MapleInventoryType.USE).setSlotLimit(rs.getShort("use"));
                    ret.getInventory(MapleInventoryType.SETUP).setSlotLimit(rs.getShort("setup"));
                    ret.getInventory(MapleInventoryType.ETC).setSlotLimit(rs.getShort("etc"));
                    ret.getInventory(MapleInventoryType.CASH).setSlotLimit(rs.getShort("cash"));
                }
                ps.close();
                rs.close();
                //加载角色装备道具
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.装备道具.loadItems(false, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                //加载角色的账号点卷之类的信息
                ps = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.getClient().setAccountName(rs.getString("name"));
                    ret.points = rs.getInt("points");
                    ret.vpoints = rs.getInt("vpoints");
                    /*
                     * 年：calendar.get(Calendar.YEAR)
                     * 月：calendar.get(Calendar.MONTH)+1
                     * 日：calendar.get(Calendar.DAY_OF_MONTH)
                     * 星期：calendar.get(Calendar.DAY_OF_WEEK)-1
                     */
                    if (rs.getTimestamp("lastlogon") != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTimeInMillis(rs.getTimestamp("lastlogon").getTime());
                        if (cal.get(Calendar.DAY_OF_WEEK) + 1 == Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                            //ret.acash += 500; //去掉这个给点卷的
                        }
                    }
                    if (rs.getInt("banned") > 0) {
                        rs.close();
                        ps.close();
                        ret.getClient().getSession().close();
                        throw new RuntimeException("加载的角色为封号状态，服务端断开这个连接...");
                    }
                    rs.close();
                    ps.close();
                    //更新角色账号的登录时间
                    ps = con.prepareStatement("UPDATE accounts SET lastlogon = CURRENT_TIMESTAMP() WHERE id = ?");
                    ps.setInt(1, ret.accountid);
                    ps.executeUpdate();
                } else {
                    rs.close();
                }
                ps.close();

                ps = con.prepareStatement("SELECT * FROM vcoreskill WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.vCoreSkills.put(ret.vCoreSkillIndex.getAndIncrement(), new VCoreSkillEntry(rs.getInt("vcoreid"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getInt("slot")));
                }
                rs.close();
                ps.close();

                //加载角色技能信息
                ps = con.prepareStatement("SELECT * FROM skills WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Skill skil;
                int phantom = 0;
                while (rs.next()) {
                    int skid = rs.getInt("skillid"); //技能ID
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel"); //当前技能等级
                    byte msl = rs.getByte("masterlevel"); //技能最大等级
                    int teachId = rs.getInt("teachId"); //技能传授者ID
                    byte position = rs.getByte("position");  //幻影复制技能位置
                    if (skil != null && SkillConstants.isApplicableSkill(skid)) {
                        if (skil.is老技能()) { //如果是老的技能 直接跳过
                            ret.changed_skills = true;
                            continue;
                        }
                        if (skl > skil.getMaxLevel() && (skid < 92000000 || skid > 99999999)) {
                            if (!skil.isBeginnerSkill() && skil.canBeLearnedBy(ret.job) && !skil.isSpecialSkill() && !skil.isAdminSkill()) {
                                ret.remainingSp[JobConstants.getSkillBookBySkill(skid)] += (skl - skil.getMaxLevel());
                            }
                            skl = (byte) skil.getMaxLevel();
                        }
                        if (msl > skil.getMaxLevel()) {
                            msl = (byte) skil.getMaxLevel();
                        }
                        if ((position >= 0 && position < 15) && phantom < 15) {
                            if (JobConstants.is幻影(ret.job) && skil.getSkillByJobBook() != -1) {
                                msl = skil.isFourthJob() ? (byte) skil.getMasterLevel() : 0;
                                ret.skills.put(skid, new SkillEntry(skl, msl, -1, teachId, position));
                            }
                            phantom++;
                        } else {
                            if (skil.isLinkSkills() && ret.linkSkills.size() < 12) {
                                skl = SkillConstants.getLinkSkillslevel(skil.getMaxLevel(), skil, teachId, ret.level);
                                ret.linkSkills.put(skid, new SkillEntry(skl, msl, rs.getLong("expiration"), teachId));
                            } else if (skil.isTeachSkills()) {
                                skl = SkillConstants.getLinkSkillslevel(skil.getMaxLevel(), skil, teachId, ret.level);
                            } else if (skil.getFixLevel() > 0) {
                                skl = skil.getFixLevel();
                            } else if (skid / 10000 >= 40000 && skid / 10000 <= 40005) {
                                if (ret.getVCoreSkillLevel(skid) <= 0) {
                                    ret.changed_skills = true;
                                    continue;
                                }
                                skl = ret.getVCoreSkillLevel(skid);
                            }
                            ret.skills.put(skid, new SkillEntry(skl, msl, rs.getLong("expiration"), teachId));
                        }
                    } else if (skil == null) { //技能不存在
                        if (!JobConstants.is新手职业(skid / 10000) && !SkillConstants.isSpecialSkill(skid) && !SkillConstants.isAdminSkill(skid)) {
                            ret.remainingSp[JobConstants.getSkillBookBySkill(skid)] += skl;
                        }
                    }
                }
                rs.close();
                ps.close();

                ret.updateShinasBless();
                if (client != null) {
                    ps = con.prepareStatement("SELECT id, level FROM characters WHERE accountid = ? AND world = ?");
                    ps.setInt(1, ret.accountid);
                    ps.setInt(2, ret.world);
                    rs = ps.executeQuery();
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        int level = rs.getInt("level");
                        try (PreparedStatement psee = con.prepareStatement("SELECT skillid, masterlevel, expiration ,teachId FROM skills WHERE characterid = ?")) {
                            psee.setInt(1, id);
                            try (ResultSet rse = psee.executeQuery()) {
                                while (rse.next()) {
                                    int skillid = SkillConstants.getLinkedSkillId(rse.getInt("skillid"));
                                    Skill skill = SkillFactory.getSkill(rse.getInt("skillid"));
                                    if (skill != null && skill.isTeachSkills() && level > 70) {
                                        byte masterlevel = rse.getByte("masterlevel");
                                        int teachId = rse.getInt("teachId");
                                        int linkSkillslevel = SkillConstants.getLinkSkillslevel(skill.getMaxLevel(), skill, teachId, level);
                                        ret.sonOfLinkedSkills.put(skillid, new Pair<>(id, new SkillEntry(linkSkillslevel, masterlevel, rse.getLong("expiration") > 0 && System.currentTimeMillis() < rse.getLong("expiration") + 86400000 ? rse.getLong("expiration") : -2, teachId > 0 ? teachId : id)));
                                    }
                                }
                            }
                        }
                    }
                }
                rs.close();
                ps.close();
                //加载角色能在能力技能
                ps = con.prepareStatement("SELECT skillid, skilllevel, position, rank FROM innerskills WHERE characterid = ? LIMIT 3");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    int skid = rs.getInt("skillid"); //技能ID
                    skil = SkillFactory.getSkill(skid);
                    int skl = rs.getInt("skilllevel"); //当前技能等级
                    byte position = rs.getByte("position");  //内在技能位置
                    byte rank = rs.getByte("rank");  //rank, C, B, A, and S
                    if (skil != null && skil.isInnerSkill() && position >= 1 && position <= 3) {
                        if (skl > skil.getMaxLevel()) {
                            skl = (byte) skil.getMaxLevel();
                        }
                        InnerSkillEntry InnerSkill = new InnerSkillEntry(skid, skl, position, rank);
                        ret.innerSkills[position - 1] = InnerSkill; //这个地方用的数组 数组是从 0 开始计算 所以减去1
                    }
                }
                rs.close();
                ps.close();
                //设置获取精灵祝福和女皇祝福的等级信息
                ps = con.prepareStatement("SELECT * FROM characters WHERE accountid = ? ORDER BY level DESC");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                int maxlevel_ = 0, maxlevel_2 = 0;
                while (rs.next()) {
                    if (rs.getInt("id") != charid) { //去掉角色自己的ID
                        if (JobConstants.is骑士团(rs.getShort("job"))) {
                            int maxlevel = (rs.getShort("level") / 5);
                            if (maxlevel > 24) {
                                maxlevel = 24;
                            }
                            if (maxlevel > maxlevel_2 || maxlevel_2 == 0) {
                                maxlevel_2 = maxlevel;
                                ret.BlessOfEmpress_Origin = rs.getString("name");
                            }
                        }
                        int maxlevel = (rs.getShort("level") / 10);
                        if (maxlevel > 20) {
                            maxlevel = 20;
                        }
                        if (maxlevel > maxlevel_ || maxlevel_ == 0) {
                            maxlevel_ = maxlevel;
                            ret.BlessOfFairy_Origin = rs.getString("name");
                        }
                    }
                }
                if (ret.BlessOfFairy_Origin == null) {
                    ret.BlessOfFairy_Origin = ret.name;
                }
                int skillid = JobConstants.getBOF_ForJob(ret.job);
                ret.skills.put(skillid, new SkillEntry(maxlevel_, (byte) 0, -1, 0));
                if (SkillFactory.getSkill(JobConstants.getEmpress_ForJob(ret.job)) != null) {
                    if (ret.BlessOfEmpress_Origin == null) {
                        ret.BlessOfEmpress_Origin = ret.BlessOfFairy_Origin;
                    }
                    ret.skills.put(JobConstants.getEmpress_ForJob(ret.job), new SkillEntry(maxlevel_2, (byte) 0, -1, 0));
                }
                ps.close();
                rs.close();
                // END
                //加载技能宏信息
                ps = con.prepareStatement("SELECT * FROM skillmacros WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int position;
                while (rs.next()) {
                    position = rs.getInt("position");
                    SkillMacro macro = new SkillMacro(rs.getInt("skill1"), rs.getInt("skill2"), rs.getInt("skill3"), rs.getString("name"), rs.getInt("shout"), position);
                    ret.skillMacros[position] = macro;
                }
                rs.close();
                ps.close();
                // 加载怪怪数据
                ps = con.prepareStatement("SELECT * FROM familiars WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.familiars.put(rs.getInt("id"), new MonsterFamiliar(rs.getInt("id"), rs.getInt("familiar"), charid, rs.getString("name"), rs.getByte("grade"), rs.getByte("level"), rs.getInt("exp"), rs.getShort("skillid"), rs.getInt("option1"), rs.getInt("option2"), rs.getInt("option3")));
                }
                //加载 pokemon
                ps = con.prepareStatement("SELECT * FROM pokemon WHERE characterid = ? OR (accountid = ? AND active = 0)");
                ps.setInt(1, charid);
                ps.setInt(2, ret.accountid);
                rs = ps.executeQuery();
                position = 0;
                while (rs.next()) {
                    Battler b = new Battler(rs.getInt("level"), rs.getInt("exp"), charid, rs.getInt("monsterid"), rs.getString("name"), PokemonNature.values()[rs.getInt("nature")], rs.getInt("itemid"), rs.getByte("gender"), rs.getByte("hpiv"), rs.getByte("atkiv"), rs.getByte("defiv"), rs.getByte("spatkiv"), rs.getByte("spdefiv"), rs.getByte("speediv"), rs.getByte("evaiv"), rs.getByte("acciv"), rs.getByte("ability"));
                    if (b.getFamily() == null) {
                        continue;
                    }
                    if (rs.getInt("active") > 0 && position < 6 && rs.getInt("characterid") == charid) {
                        ret.battlers[position] = b;
                        position++;
                    } else {
                        ret.boxed.add(b);
                    }
                }
                rs.close();
                ps.close();
                //加载角色键盘设置信息
                ps = con.prepareStatement("SELECT `key`,`type`,`action` FROM keymap WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                Map<Integer, Pair<Byte, Integer>> keyb = ret.keylayout.Layout();
                while (rs.next()) {
                    keyb.put(rs.getInt("key"), new Pair<>(rs.getByte("type"), rs.getInt("action")));
                }
                rs.close();
                ps.close();
                ret.keylayout.unchanged();
                //加载quickslot设置 这个设置暂时不知道干什么的
                ps = con.prepareStatement("SELECT `index`, `key` FROM quickslot WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                List<Pair<Integer, Integer>> quickslots = ret.quickslot.Layout();
                while (rs.next()) {
                    quickslots.add(new Pair<>(rs.getInt("index"), rs.getInt("key")));
                }
                rs.close();
                ps.close();
                ret.quickslot.unchanged();
                //加载角色地图信息？
                ps = con.prepareStatement("SELECT `locationtype`,`map` FROM savedlocations WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.savedLocations[rs.getInt("locationtype")] = rs.getInt("map");
                }
                rs.close();
                ps.close();
                //加载角色使用人气的信息
                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM famelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastfametime = 0;
                ret.lastmonthfameids = new ArrayList<>(31);
                while (rs.next()) {
                    ret.lastfametime = Math.max(ret.lastfametime, rs.getTimestamp("when").getTime());
                    ret.lastmonthfameids.add(rs.getInt("characterid_to"));
                }
                rs.close();
                ps.close();
                //加载角色使用好感度的信息
                ps = con.prepareStatement("SELECT `characterid_to`,`when` FROM lovelog WHERE characterid = ? AND DATEDIFF(NOW(),`when`) < 1");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                ret.lastlovetime = 0;
                ret.lastdayloveids = new LinkedHashMap<>();
                while (rs.next()) {
                    ret.lastlovetime = Math.max(ret.lastlovetime, rs.getTimestamp("when").getTime());
                    ret.lastdayloveids.put(rs.getInt("characterid_to"), rs.getTimestamp("when").getTime());
                }
                rs.close();
                ps.close();
                //未知
                ps = con.prepareStatement("SELECT `accid_to`,`when` FROM battlelog WHERE accid = ? AND DATEDIFF(NOW(),`when`) < 30");
                ps.setInt(1, ret.accountid);
                rs = ps.executeQuery();
                ret.lastmonthbattleids = new ArrayList<>();
                while (rs.next()) {
                    ret.lastmonthbattleids.add(rs.getInt("accid_to"));
                }
                rs.close();
                ps.close();
                //加载矿物和药草背包的信息
                ps = con.prepareStatement("SELECT `itemId` FROM extendedSlots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                for (byte i = 2; i <= 4; i++) {
                    ret.extendedSlots.put(i, new ArrayList<>());
                }
                while (rs.next()) {
                    int itemid = rs.getInt("itemId");
                    ret.extendedSlots.get(ItemConstants.getInventoryType(itemid).getType()).add(itemid);
                }
                rs.close();
                ps.close();
                //加载好友信息
                ret.buddylist.loadFromDb(charid);
                //加载仓库信息
                ret.storage = MapleStorage.loadOrCreateFromDB(ret.accountid);
                //加载商城信息
                ret.cs = new CashShop(ret.accountid, charid, ret.getJob());
                //加载礼物信息
                ps = con.prepareStatement("SELECT sn FROM wishlist WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int i = 0;
                while (rs.next()) {
                    ret.wishlist[i] = rs.getInt("sn");
                    i++;
                }
                while (i < 12) {
                    ret.wishlist[i] = 0;
                    i++;
                }
                rs.close();
                ps.close();
                //加载缩地石保存信息
                ps = con.prepareStatement("SELECT mapid,vip FROM trocklocations WHERE characterid = ? LIMIT 28");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                int r = 0;
                int reg = 0;
                int hyper = 0;
                while (rs.next()) {
                    if (rs.getInt("vip") == 0) {
                        ret.regrocks[reg] = rs.getInt("mapid");
                        reg++;
                    } else if (rs.getInt("vip") == 1) {
                        ret.rocks[r] = rs.getInt("mapid");
                        r++;
                    } else if (rs.getInt("vip") == 2) {
                        ret.hyperrocks[hyper] = rs.getInt("mapid");
                        hyper++;
                    }
                }
                while (reg < 5) {
                    ret.regrocks[reg] = 999999999;
                    reg++;
                }
                while (r < 10) {
                    ret.rocks[r] = 999999999;
                    r++;
                }
                while (hyper < 13) {
                    ret.hyperrocks[hyper] = 999999999;
                    hyper++;
                }
                rs.close();
                ps.close();
                //加载道具宝宝信息
                ps = con.prepareStatement("SELECT * FROM imps WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                r = 0;
                while (rs.next()) {
                    ret.imps[r] = new MapleImp(rs.getInt("itemid"));
                    ret.imps[r].setLevel(rs.getByte("level"));
                    ret.imps[r].setState(rs.getByte("state"));
                    ret.imps[r].setCloseness(rs.getShort("closeness"));
                    ret.imps[r].setFullness(rs.getShort("fullness"));
                    r++;
                }
                rs.close();
                ps.close();
                //加载坐骑信息
                ps = con.prepareStatement("SELECT * FROM mountdata WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (!rs.next()) {
                    throw new RuntimeException("在数据库中没有找到角色的坐骑信息...");
                }
                Item mount = ret.getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -18);
                ret.mount = new MapleMount(ret, mount != null ? mount.getItemId() : 0, 80001000, rs.getByte("Fatigue"), rs.getByte("Level"), rs.getInt("Exp"));
                ps.close();
                rs.close();
                //加载药剂罐信息
                ps = con.prepareStatement("SELECT * FROM character_potionpots WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                if (rs.next()) {
                    ret.potionPot = new MaplePotionPot(charid, rs.getInt("itemId"), rs.getInt("hp"), rs.getInt("mp"), rs.getInt("maxValue"), rs.getLong("startDate"), rs.getLong("endDate"));
                }
                ps.close();
                rs.close();
                //加载宝盒信息
                if (ret.getSkillLevel(龙的传人.宝盒的庇佑) > 0 && JobConstants.is龙的传人(ret.job)) { //0001214 - 宝盒的庇佑 - 获得含有侠义精神的宝盒的庇佑。\n#c双击#技能可打开宝盒界面，查看目前属性及#c剩余时间#，倒计时结束后当前属性会重置。可以通过特定道具变更属性、走向、剩余时间。
                    ret.coreAura = MapleCoreAura.loadFromDb(charid, ret.level);
                    if (ret.coreAura == null) { //如果读取为空 就创建1个新的宝盒信息
                        ret.coreAura = MapleCoreAura.createCoreAura(charid, ret.level);
                    }
                } else if (ret.getSkillTeachId(80001151) > 0) { //80001151 - 宝盒的庇佑 - 获得包含了侠义精神的宝盒的庇佑。从龙的传人那里获得的属性链接，会因为自身和传授者的等级产生差异。
                    ret.coreAura = MapleCoreAura.loadFromDb(ret.getSkillTeachId(80001151));
                }
                //加载自定义积分
                ps = con.prepareStatement("SELECT * FROM character_credit WHERE characterid = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.credit.put(rs.getString("name"), rs.getInt("value"));
                }
                ps.close();
                rs.close();
                /*
                  加载效果开关
                 */
                ps = con.prepareStatement("SELECT * FROM effectswitch WHERE `characterid` = ?");
                ps.setInt(1, charid);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ret.effectSwitch.add(rs.getInt("pos"));
                }
                ps.close();
                rs.close();
                // 初始化角色属性
                ret.stats.recalcLocalStats(true, ret);
            } else { // 不是在频道加载
                for (Pair<Item, MapleInventoryType> mit : ItemLoader.装备道具.loadItems(true, charid).values()) {
                    ret.getInventory(mit.getRight()).addFromDB(mit.getLeft());
                }
                ret.stats.recalcPVPRank(ret);
            }
        } catch (SQLException ess) {
            ret.getClient().getSession().close();
            log.error("加载角色数据信息出错...", ess);
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException ignore) {
            }
        }
        return ret;
    }

    /*
     * 保存新角色到数据库
     */
    public static void saveNewCharToDB(MapleCharacter chr, JobType type, short db, boolean oldkey) {
        DruidPooledConnection con = null;

        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;
        try {
            con = DatabaseConnection.getInstance().getConnection();
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("INSERT INTO characters (level, str, dex, luk, `int`, hp, mp, maxhp, maxmp, sp, ap, skincolor, gender, job, hair, face, map, meso, party, buddyCapacity, pets, decorate, subcategory, friendshippoints, accountid, name, world, gm) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            int index = 0;
            ps.setInt(++index, chr.level); // Level
            PlayerStats stat = chr.stats;
            ps.setShort(++index, stat.getStr()); // Str
            ps.setShort(++index, stat.getDex()); // Dex
            ps.setShort(++index, stat.getInt()); // Int
            ps.setShort(++index, stat.getLuk()); // Luk
            ps.setInt(++index, stat.getHp()); // Hp
            ps.setInt(++index, stat.getMp()); // Mp
            ps.setInt(++index, stat.getMaxHp()); // maxHp
            ps.setInt(++index, stat.getMaxMp()); // maxMp
            StringBuilder sps = new StringBuilder();
            for (int i = 0; i < chr.remainingSp.length; i++) {
                sps.append(chr.remainingSp[i]);
                sps.append(",");
            }
            String sp = sps.toString();
            ps.setString(++index, sp.substring(0, sp.length() - 1));
            ps.setShort(++index, chr.remainingAp); // Remaining AP

            ps.setByte(++index, chr.skinColor);
            ps.setByte(++index, chr.gender);
            ps.setShort(++index, chr.job);
            ps.setInt(++index, chr.hair);
            ps.setInt(++index, chr.face);
            if (db < 0 || db > 10) {
                db = 0;
            }
            ps.setInt(++index, ServerConfig.CHANNEL_PLAYER_BEGINNERMAP); //设置出生地图
            ps.setLong(++index, chr.meso.get()); // Meso 金币
            ps.setInt(++index, -1); // Party
            ps.setByte(++index, chr.buddylist.getCapacity()); // Buddylist
            ps.setString(++index, "-1,-1,-1");
            ps.setInt(++index, chr.decorate);
            ps.setInt(++index, db); //for now
            ps.setString(++index, chr.friendshippoints[0] + "," + chr.friendshippoints[1] + "," + chr.friendshippoints[2] + "," + chr.friendshippoints[3] + "," + chr.friendshippoints[4]);
            ps.setInt(++index, chr.getAccountID());
            ps.setString(++index, chr.name);
            ps.setByte(++index, chr.world);
            ps.setInt(++index, chr.getGMLevel());
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                chr.id = rs.getInt(1);
            } else {
                ps.close();
                rs.close();
                log.error("生成新角色到数据库出错...");
            }
            ps.close();
            rs.close();
            /*
             * 保存新角色的任务信息
             */
            ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (MapleQuestStatus q : chr.quests.values()) {
                ps.setInt(2, q.getQuest().getId());
                ps.setInt(3, q.getStatus());
                ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                ps.setInt(5, q.getForfeited());
                ps.setString(6, q.getCustomData());
                ps.execute();
                rs = ps.getGeneratedKeys();
                if (q.hasMobKills()) {
                    rs.next();
                    for (int mob : q.getMobKills().keySet()) {
                        pse.setInt(1, rs.getInt(1));
                        pse.setInt(2, mob);
                        pse.setInt(3, q.getMobKills(mob));
                        pse.execute();
                    }
                }
                rs.close();
            }
            ps.close();
            pse.close();
            /*
             * 保存新角色的其他设置信息
             */
            ps = con.prepareStatement("INSERT INTO character_keyvalue (`characterid`, `key`, `value`) VALUES (?, ?, ?)");
            ps.setInt(1, chr.id);
            for (Entry<String, String> key : chr.keyValue.entrySet()) {
                ps.setString(2, key.getKey());
                ps.setString(3, key.getValue());
                ps.execute();
            }
            ps.close();
            /*
             * 保存角色的技能
             */
            ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, teachId) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            for (Entry<Integer, SkillEntry> skill : chr.skills.entrySet()) {
                if (SkillConstants.isApplicableSkill(skill.getKey())) { //do not save additional skills
                    ps.setInt(2, skill.getKey());
                    ps.setInt(3, skill.getValue().skillevel);
                    ps.setByte(4, skill.getValue().masterlevel);
                    ps.setLong(5, skill.getValue().expiration);
                    ps.setInt(6, skill.getValue().teachId);
                    ps.execute();
                }
            }
            ps.close();
            /*
             * 生成新角色的背包空间数量
             */
            ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setShort(2, (byte) 48); // Eq
            ps.setShort(3, (byte) 48); // Use
            ps.setShort(4, (byte) 48); // Setup
            ps.setShort(5, (byte) 48); // ETC
            ps.setShort(6, (byte) 60); // Cash
            ps.execute();
            ps.close();
            /*
             * 生成新角色的坐骑信息
             */
            ps = con.prepareStatement("INSERT INTO mountdata (characterid, `Level`, `Exp`, `Fatigue`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            ps.setByte(2, (byte) 1);
            ps.setInt(3, 0);
            ps.setByte(4, (byte) 0);
            ps.execute();
            ps.close();
            /*
             * 生成新角色的键盘设置信息
             */
            //以前的模式
            int[] array1 = {1, 2, 3, 4, 5, 6, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 29, 31, 34, 35, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 50, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70};
            int[] array2 = {4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4};
            int[] array3 = {46, 10, 12, 13, 18, 23, 8, 5, 0, 4, 27, 30, 39, 1, 41, 19, 14, 15, 52, 2, 17, 11, 3, 20, 26, 16, 22, 9, 50, 51, 6, 31, 29, 7, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47};
            //新的键盘模式
            int[] new_array1 = {1, 20, 21, 22, 23, 25, 26, 27, 29, 34, 35, 36, 37, 38, 39, 40, 41, 43, 44, 45, 46, 47, 48, 49, 50, 52, 56, 57, 59, 60, 61, 63, 64, 65, 66, 70, 71, 73, 79, 82, 83};
            int[] new_array2 = {4, 4, 4, 4, 4, 4, 4, 4, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 4, 4, 4, 4, 4, 4, 5, 5, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4, 4, 4, 4};
            int[] new_array3 = {46, 27, 30, 0, 1, 19, 14, 15, 52, 17, 11, 8, 3, 20, 26, 16, 22, 9, 50, 51, 2, 31, 29, 5, 7, 4, 53, 54, 100, 101, 102, 103, 104, 105, 106, 47, 12, 13, 23, 10, 18};

            ps = con.prepareStatement("INSERT INTO keymap (characterid, `key`, `type`, `action`) VALUES (?, ?, ?, ?)");
            ps.setInt(1, chr.id);
            int keylength = oldkey ? array1.length : new_array1.length;
            for (int i = 0; i < keylength; i++) {
                ps.setInt(2, oldkey ? array1[i] : new_array1[i]);
                ps.setInt(3, oldkey ? array2[i] : new_array2[i]);
                ps.setInt(4, oldkey ? array3[i] : new_array3[i]);
                ps.execute();
            }
            ps.close();

            List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();
            for (MapleInventory iv : chr.inventory) {
                for (Item item : iv.list()) {
                    itemsWithType.add(new Pair<>(item, iv.getType()));
                }
            }
            ItemLoader.装备道具.saveItems(con, itemsWithType, chr.id);
            con.commit();
        } catch (Exception e) {
            log.error("[charsave] Error saving character data", e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                log.error("[charsave] Error Rolling Back", ex);
            }
        } finally {
            try {
                if (pse != null) {
                    pse.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            } catch (SQLException e) {
                log.error("[charsave] Error going back to autocommit mode", e);
            }
        }
    }

    public static void deleteWhereCharacterId(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    public static void deleteWhereCharacterId_NoLock(Connection con, String sql, int id) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, id);
        ps.execute();
        ps.close();
    }

    public static boolean ban(String id, String reason, boolean accountId, int gmlevel, boolean hellban) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps;
            if (id.matches("/[0-9]{1,3}\\..*")) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, id);
                ps.execute();
                ps.close();
                return true;
            }
            if (accountId) {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE name = ?");
            } else {
                ps = con.prepareStatement("SELECT accountid FROM characters WHERE name = ?");
            }
            boolean ret = false;
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int z = rs.getInt(1);
                PreparedStatement psb = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE id = ? AND gm < ?");
                psb.setString(1, reason);
                psb.setInt(2, z);
                psb.setInt(3, gmlevel);
                psb.execute();
                psb.close();
                if (gmlevel > 100) { //如果是最高管理员封号 就进行下面的处理
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, z);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        String sessionIP = rsa.getString("sessionIP");
                        if (sessionIP != null && sessionIP.matches("/[0-9]{1,3}\\..*")) {
                            PreparedStatement psz = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                            psz.setString(1, sessionIP);
                            psz.execute();
                            psz.close();
                        }
                        String macData = rsa.getString("macs");
                        if (macData != null && !macData.equalsIgnoreCase("00-00-00-00-00-00") && macData.length() >= 17) {
                            PreparedStatement psm = con.prepareStatement("INSERT INTO macbans VALUES (DEFAULT, ?)");
                            psm.setString(1, macData);
                            psm.execute();
                            psm.close();
                        }
                        if (hellban) {
                            PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = 1, banreason = ? WHERE email = ?" + (sessionIP == null ? "" : " OR SessionIP = ?"));
                            pss.setString(1, reason);
                            pss.setString(2, rsa.getString("email"));
                            if (sessionIP != null) {
                                pss.setString(3, sessionIP);
                            }
                            pss.execute();
                            pss.close();
                        }
                    }
                    rsa.close();
                    psa.close();
                }
                ret = true;
            }
            rs.close();
            ps.close();
            return ret;
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
        }
        return false;
    }

    public static byte checkExistance(int accid, int cid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerch WHERE accountid = ? AND characterid = ?")) {
                ps.setInt(1, accid);
                ps.setInt(2, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ps.close();
                        rs.close();
                        return 1;
                    }
                }
            }
            return 0;
        } catch (SQLException se) {
            return -1;
        }
    }

    public static void removePartTime(int cid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM parttime WHERE cid = ?")) {
                ps.setInt(1, cid);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            System.out.println("无法删除打工信息: " + ex);
        }
    }

    public static void addPartTime(MaplePartTimeJob partTime) {
        if (partTime.getCharacterId() < 1) {
            return;
        }
        addPartTime(partTime.getCharacterId(), partTime.getJob(), partTime.getTime(), partTime.getReward());
    }

    public static void addPartTime(int cid, byte job, long time, int reward) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO parttime (cid, job, time, reward) VALUES (?, ?, ?, ?)")) {
                ps.setInt(1, cid);
                ps.setByte(2, job);
                ps.setLong(3, time);
                ps.setInt(4, reward);
                ps.execute();
            }
        } catch (SQLException ex) {
            System.out.println("无法添加打工信息: " + ex);
        }
    }

    public static MaplePartTimeJob getPartTime(int cid) {
        MaplePartTimeJob partTime = new MaplePartTimeJob(cid);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM parttime WHERE cid = ?")) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        partTime.setJob(rs.getByte("job"));
                        partTime.setTime(rs.getLong("time"));
                        partTime.setReward(rs.getInt("reward"));
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("无法查询打工信息: " + ex);
        }
        return partTime;
    }

    public static boolean hasSkill(final Map<Integer, SkillEntry> cskills, final int skillid, int level) {
        if (cskills.get(skillid) != null) {
            return cskills.get(skillid).skillevel == level && cskills.containsKey(skillid);
        }
        return cskills.containsKey(skillid);
    }

    public static MapleCharacter getOnlineCharacterById(int cid) {
        return WorldFindService.getInstance().findCharacterById(cid);
    }

    public static MapleCharacter getCharacterById(int n2) {
        MapleCharacter player = getOnlineCharacterById(n2);
        return player == null ? MapleCharacter.loadCharFromDB(n2, new MapleClient(null, null, null), true) : player;
    }

    public static int getLevelbyid(int cid) {
        int level = -1;
        try (DruidPooledConnection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT `level` FROM characters WHERE id = ?")) {
                ps.setInt(1, cid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        level = rs.getInt("level");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("读取角色等级失败", e);
        }
        return level;
    }

    public synchronized void saveToCache() {
        try {
            CharacterTransfer ct = new CharacterTransfer(this);
            RedisUtil.hset(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName(), String.valueOf(id), JsonUtil.getMapperInstance().writeValueAsString(ct));
        } catch (JsonProcessingException e) {
            log.error("更新缓存出错", e);
        }
    }

    public void clearCache() {
        Jedis jedis = RedisUtil.getJedis();
        jedis.hdel(RedisUtil.KEYNAMES.PLAYER_DATA.getKeyName(), String.valueOf(id));
        RedisUtil.returnResource(jedis);
    }

    /*
     * 保存角色到数据库
     */
    public void saveToDB(boolean dc, boolean fromcs) {
        if (isSaveing) {
            log.info(MapleClient.getLogMessage(getClient(), "正在保存数据，本次操作返回."));
            return;
        }
        DruidPooledConnection con = null;
        PreparedStatement ps = null;
        PreparedStatement pse = null;
        ResultSet rs = null;

        try {
            con = DatabaseConnection.getInstance().getConnection();
            isSaveing = true;
            con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            con.setAutoCommit(false);

            ps = con.prepareStatement("SELECT * FROM characters WHERE id = ?");
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                ps.close();
                rs.close();

                ps = con.prepareStatement("UPDATE characters SET level = ?, fame = ?, str = ?, dex = ?, luk = ?, `int` = ?, exp = ?, hp = ?, mp = ?, maxhp = ?, maxmp = ?, sp = ?, ap = ?, gm = ?, skincolor = ?, gender = ?, job = ?, hair = ?, basecolor = ?, mixedcolor = ?, probcolor = ?, face = ?, map = ?, meso = ?, hpApUsed = ?, spawnpoint = ?, party = ?, buddyCapacity = ?, pets = ?, subcategory = ?, marriageId = ?, currentrep = ?, totalrep = ?, gachexp = ?, fatigue = ?, charm = ?, charisma = ?, craft = ?, insight = ?, sense = ?, will = ?, totalwins = ?, totallosses = ?, pvpExp = ?, pvpPoints = ?, decorate = ?, beans = ?, warning = ?, reborns = ?, reborns1 = ?, reborns2 = ?, reborns3 = ?, apstorage = ?, honor = ?, love = ?, playerPoints = ?, playerEnergy = ?, pvpDeaths = ?, pvpKills = ?, pvpVictory = ?, vip = ?, viptime = ?, todayonlinetime = ?, totalonlinetime = ?, friendshiptoadd = ?, friendshippoints = ?, name = ?, wp = ? WHERE id = ?", DatabaseConnection.RETURN_GENERATED_KEYS);
                int index = 0;
                ps.setInt(++index, level);
                ps.setInt(++index, fame);
                ps.setShort(++index, stats.getStr());
                ps.setShort(++index, stats.getDex());
                ps.setShort(++index, stats.getLuk());
                ps.setShort(++index, stats.getInt());
                ps.setLong(++index, level >= getMaxLevelForSever() ? 0 : Math.abs(exp.get()));
                ps.setInt(++index, stats.getHp() < 1 ? 50 : stats.getHp());
                ps.setInt(++index, stats.getMp());
                ps.setInt(++index, stats.getMaxHp());
                ps.setInt(++index, stats.getMaxMp());
                StringBuilder sps = new StringBuilder();
                for (int aRemainingSp : remainingSp) {
                    sps.append(aRemainingSp);
                    sps.append(",");
                }
                String sp = sps.toString();
                ps.setString(++index, sp.substring(0, sp.length() - 1));
                ps.setShort(++index, remainingAp);
                ps.setByte(++index, gmLevel);
                ps.setByte(++index, skinColor);
                ps.setByte(++index, gender);
                ps.setShort(++index, job);
                ps.setInt(++index, hair);
                ps.setByte(++index, hairBaseColor);
                ps.setByte(++index, hairMixedColor);
                ps.setByte(++index, hairProbColor);
                ps.setInt(++index, face);
                if (!fromcs && map != null) {
                    if (map.getForcedReturnId() != 999999999 && map.getForcedReturnMap() != null) {
                        mapid = map.getForcedReturnId();
                    } else {
                        mapid = stats.getHp() < 1 ? map.getReturnMapId() : map.getId();
                    }
                }
                ps.setInt(++index, GameConstants.getOverrideReturnMap(mapid));
                ps.setLong(++index, meso.get());
                ps.setShort(++index, hpApUsed);
                if (map == null) {
                    ps.setByte(++index, (byte) 0);
                } else {
                    MaplePortal closest = map.findClosestSpawnpoint(getTruePosition());
                    ps.setByte(++index, (byte) (closest != null ? closest.getId() : 0));
                }
                ps.setInt(++index, party == null ? -1 : party.getPartyId());
                ps.setShort(++index, buddylist.getCapacity());
                StringBuilder petz = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    if (spawnPets[i] != null && spawnPets[i].getSummoned()) {
                        spawnPets[i].saveToDb();
                        petz.append(spawnPets[i].getInventoryPosition());
                        petz.append(",");
                    } else {
                        petz.append("-1,");
                    }
                }
                String petstring = petz.toString();
                ps.setString(++index, petstring.substring(0, petstring.length() - 1));
                ps.setByte(++index, subcategory);
                ps.setInt(++index, marriageId);
                ps.setInt(++index, currentrep);
                ps.setInt(++index, totalrep);
                ps.setInt(++index, gachexp);
                ps.setShort(++index, fatigue);
                ps.setInt(++index, traits.get(MapleTraitType.charm).getTotalExp());
                ps.setInt(++index, traits.get(MapleTraitType.charisma).getTotalExp());
                ps.setInt(++index, traits.get(MapleTraitType.craft).getTotalExp());
                ps.setInt(++index, traits.get(MapleTraitType.insight).getTotalExp());
                ps.setInt(++index, traits.get(MapleTraitType.sense).getTotalExp());
                ps.setInt(++index, traits.get(MapleTraitType.will).getTotalExp());
                ps.setInt(++index, totalWins);
                ps.setInt(++index, totalLosses);
                ps.setInt(++index, pvpExp);
                ps.setInt(++index, pvpPoints);
                // 魔族之纹
                ps.setInt(++index, decorate);
                // 豆豆信息
                ps.setInt(++index, beans);
                // 警告次数
                ps.setInt(++index, warning);
                // 转身系统
                ps.setInt(++index, reborns);
                ps.setInt(++index, reborns1);
                ps.setInt(++index, reborns2);
                ps.setInt(++index, reborns3);
                ps.setInt(++index, apstorage);
                // 荣誉系统
                ps.setInt(++index, honor);
                // 好感度
                ps.setInt(++index, love);
                // 新增变量
                ps.setInt(++index, playerPoints);
                ps.setInt(++index, playerEnergy);
                // Pvp变量
                ps.setInt(++index, pvpDeaths);
                ps.setInt(++index, pvpKills);
                ps.setInt(++index, pvpVictory);
                // Vip 会员信息
                ps.setInt(++index, vip);
                ps.setTimestamp(++index, getViptime());
                ps.setInt(++index, todayonlinetime + (int) ((System.currentTimeMillis() - todayonlinetimestamp.getTime()) / 60000));
                ps.setInt(++index, totalonlinetime + (int) ((System.currentTimeMillis() - todayonlinetimestamp.getTime()) / 60000));
                ps.setInt(++index, friendshiptoadd);
                ps.setString(++index, friendshippoints[0] + "," + friendshippoints[1] + "," + friendshippoints[2] + "," + friendshippoints[3] + "," + friendshippoints[4]);
                // 保存角色名字和ID
                ps.setString(++index, name);
                ps.setInt(++index, weaponPoint);
                ps.setInt(++index, id);
                if (ps.executeUpdate() < 1) {
                    ps.close();
                    log.error("Character not in database (" + id + ")");
                }
                ps.close();
                /*
                 * 保存技能宏设置
                 */
                if (changed_skillmacros) {
                    deleteWhereCharacterId(con, "DELETE FROM skillmacros WHERE characterid = ?");
                    for (int i = 0; i < 5; i++) {
                        SkillMacro macro = skillMacros[i];
                        if (macro != null) {
                            ps = con.prepareStatement("INSERT INTO skillmacros (characterid, skill1, skill2, skill3, name, shout, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                            ps.setInt(1, id);
                            ps.setInt(2, macro.getSkill1());
                            ps.setInt(3, macro.getSkill2());
                            ps.setInt(4, macro.getSkill3());
                            ps.setString(5, macro.getName());
                            ps.setInt(6, macro.getShout());
                            ps.setInt(7, i);
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                if (changed_pokemon) {
                    ps = con.prepareStatement("DELETE FROM pokemon WHERE characterid = ? OR (accountid = ? AND active = 0)");
                    ps.setInt(1, id);
                    ps.setInt(2, accountid);
                    ps.execute();
                    ps.close();
                    ps = con.prepareStatement("INSERT INTO pokemon (characterid, level, exp, monsterid, name, nature, active, accountid, itemid, gender, hpiv, atkiv, defiv, spatkiv, spdefiv, speediv, evaiv, acciv, ability) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    for (Battler macro : battlers) {
                        if (macro != null) {
                            ps.setInt(1, id);
                            ps.setInt(2, macro.getLevel());
                            ps.setInt(3, macro.getExp());
                            ps.setInt(4, macro.getMonsterId());
                            ps.setString(5, macro.getName());
                            ps.setInt(6, macro.getNature().ordinal());
                            ps.setInt(7, 1);
                            ps.setInt(8, accountid);
                            ps.setInt(9, macro.getItem() == null ? 0 : macro.getItem().id);
                            ps.setByte(10, macro.getGender());
                            ps.setByte(11, macro.getIV(PokemonStat.HP));
                            ps.setByte(12, macro.getIV(PokemonStat.ATK));
                            ps.setByte(13, macro.getIV(PokemonStat.DEF));
                            ps.setByte(14, macro.getIV(PokemonStat.SPATK));
                            ps.setByte(15, macro.getIV(PokemonStat.SPDEF));
                            ps.setByte(16, macro.getIV(PokemonStat.SPEED));
                            ps.setByte(17, macro.getIV(PokemonStat.EVA));
                            ps.setByte(18, macro.getIV(PokemonStat.ACC));
                            ps.setByte(19, macro.getAbilityIndex());
                            ps.execute();
                        }
                    }
                    for (Battler macro : boxed) {
                        ps.setInt(1, id);
                        ps.setInt(2, macro.getLevel());
                        ps.setInt(3, macro.getExp());
                        ps.setInt(4, macro.getMonsterId());
                        ps.setString(5, macro.getName());
                        ps.setInt(6, macro.getNature().ordinal());
                        ps.setInt(7, 0);
                        ps.setInt(8, accountid);
                        ps.setInt(9, macro.getItem() == null ? 0 : macro.getItem().id);
                        ps.setByte(10, macro.getGender());
                        ps.setByte(11, macro.getIV(PokemonStat.HP));
                        ps.setByte(12, macro.getIV(PokemonStat.ATK));
                        ps.setByte(13, macro.getIV(PokemonStat.DEF));
                        ps.setByte(14, macro.getIV(PokemonStat.SPATK));
                        ps.setByte(15, macro.getIV(PokemonStat.SPDEF));
                        ps.setByte(16, macro.getIV(PokemonStat.SPEED));
                        ps.setByte(17, macro.getIV(PokemonStat.EVA));
                        ps.setByte(18, macro.getIV(PokemonStat.ACC));
                        ps.setByte(19, macro.getAbilityIndex());
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存道具栏的数量信息
                 */
                deleteWhereCharacterId(con, "DELETE FROM inventoryslot WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO inventoryslot (characterid, `equip`, `use`, `setup`, `etc`, `cash`) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                ps.setShort(2, getInventory(MapleInventoryType.EQUIP).getSlotLimit());
                ps.setShort(3, getInventory(MapleInventoryType.USE).getSlotLimit());
                ps.setShort(4, getInventory(MapleInventoryType.SETUP).getSlotLimit());
                ps.setShort(5, getInventory(MapleInventoryType.ETC).getSlotLimit());
                ps.setShort(6, getInventory(MapleInventoryType.CASH).getSlotLimit());
                ps.execute();
                ps.close();
                /*
                 * 保存装备信息
                 */
                List<Pair<Item, MapleInventoryType>> itemsWithType = new ArrayList<>();
                for (MapleInventory iv : inventory) {
                    for (Item item : iv.list()) {
                        itemsWithType.add(new Pair<>(item, iv.getType()));
                    }
                }
                ItemLoader.装备道具.saveItems(con, itemsWithType, id);
                /*
                 * 保存角色的一些特殊操作处理
                 */
                if (changed_keyValue) {
                    deleteWhereCharacterId(con, "DELETE FROM character_keyvalue WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO character_keyvalue (`characterid`, `key`, `value`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (Entry<String, String> key : keyValue.entrySet()) {
                        ps.setString(2, key.getKey());
                        ps.setString(3, key.getValue());
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存任务信息
                 */
                if (changed_questinfo) {
                    deleteWhereCharacterId(con, "DELETE FROM questinfo WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO questinfo (`accountid`, `characterid`, `quest`, `customData`) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, accountid);
                    ps.setInt(2, id);
                    for (Entry<Integer, String> q : questinfo.entrySet()) {
                        ps.setInt(3, q.getKey());
                        ps.setString(4, q.getValue());
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存任务状态信息
                 */
                deleteWhereCharacterId(con, "DELETE FROM queststatus WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO queststatus (`queststatusid`, `characterid`, `quest`, `status`, `time`, `forfeited`, `customData`) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
                pse = con.prepareStatement("INSERT INTO queststatusmobs VALUES (DEFAULT, ?, ?, ?)");
                ps.setInt(1, id);
                for (MapleQuestStatus q : quests.values()) {
                    ps.setInt(2, q.getQuest().getId());
                    ps.setInt(3, q.getStatus());
                    ps.setInt(4, (int) (q.getCompletionTime() / 1000));
                    ps.setInt(5, q.getForfeited());
                    ps.setString(6, q.getCustomData());
                    ps.execute();
                    rs = ps.getGeneratedKeys();
                    if (q.hasMobKills()) {
                        rs.next();
                        for (int mob : q.getMobKills().keySet()) {
                            pse.setInt(1, rs.getInt(1));
                            pse.setInt(2, mob);
                            pse.setInt(3, q.getMobKills(mob));
                            pse.execute();
                        }
                    }
                    rs.close();
                }
                ps.close();
                pse.close();
                /*
                 * 保存技能数据信息
                 */
                if (changed_skills) {
                    deleteWhereCharacterId(con, "DELETE FROM skills WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO skills (characterid, skillid, skilllevel, masterlevel, expiration, teachId, position) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps.setInt(1, id);
                    for (Entry<Integer, SkillEntry> skill : skills.entrySet()) {
                        if (SkillConstants.isApplicableSkill(skill.getKey())) { //do not save additional skills
                            ps.setInt(2, skill.getKey());
                            ps.setInt(3, skill.getValue().skillevel);
                            ps.setByte(4, skill.getValue().masterlevel);
                            ps.setLong(5, skill.getValue().expiration);
                            ps.setInt(6, skill.getValue().teachId);
                            ps.setByte(7, skill.getValue().position);
                            ps.execute();
                        }
                    }
                    ps.close();
                }
                /*
                 * 保存内在技能数据信息
                 */
                if (changed_innerSkills) {
                    deleteWhereCharacterId(con, "DELETE FROM innerskills WHERE characterid = ?");
                    for (int i = 0; i < 3; i++) {
                        InnerSkillEntry InnerSkill = innerSkills[i];
                        if (InnerSkill != null) {
                            ps = con.prepareStatement("INSERT INTO innerskills (characterid, skillid, skilllevel, position, rank) VALUES (?, ?, ?, ?, ?)");
                            ps.setInt(1, id);
                            ps.setInt(2, InnerSkill.getSkillId());
                            ps.setInt(3, InnerSkill.getSkillLevel());
                            ps.setByte(4, InnerSkill.getPosition());
                            ps.setByte(5, InnerSkill.getRank());
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                /*
                 * 保存技能冷取时间信息
                 */
                List<MapleCoolDownValueHolder> coolDownInfo = getCooldowns();
                if (dc && coolDownInfo.size() > 0) {
                    ps = con.prepareStatement("INSERT INTO skills_cooldowns (charid, SkillID, StartTime, length) VALUES (?, ?, ?, ?)");
                    ps.setInt(1, getId());
                    for (MapleCoolDownValueHolder cooling : coolDownInfo) {
                        ps.setInt(2, cooling.skillId);
                        ps.setLong(3, cooling.startTime);
                        ps.setLong(4, cooling.length);
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存传送点位置信息
                 */
                if (changed_savedlocations) {
                    deleteWhereCharacterId(con, "DELETE FROM savedlocations WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO savedlocations (characterid, `locationtype`, `map`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (SavedLocationType savedLocationType : SavedLocationType.values()) {
                        if (savedLocations[savedLocationType.getValue()] != -1) {
                            ps.setInt(2, savedLocationType.getValue());
                            ps.setInt(3, savedLocations[savedLocationType.getValue()]);
                            ps.execute();
                        }
                    }
                    ps.close();
                }
                /*
                 * 保存成就信息
                 */
                if (changed_achievements) {
                    ps = con.prepareStatement("DELETE FROM achievements WHERE accountid = ?");
                    ps.setInt(1, accountid);
                    ps.executeUpdate();
                    ps.close();
                    ps = con.prepareStatement("INSERT INTO achievements(charid, achievementid, accountid) VALUES(?, ?, ?)");
                    for (Integer achid : finishedAchievements) {
                        ps.setInt(1, id);
                        ps.setInt(2, achid);
                        ps.setInt(3, accountid);
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存角色被举报的信息
                 */
                if (changed_reports) {
                    deleteWhereCharacterId(con, "DELETE FROM reports WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO reports VALUES(DEFAULT, ?, ?, ?)");
                    for (Entry<ReportType, Integer> achid : reports.entrySet()) {
                        ps.setInt(1, id);
                        ps.setByte(2, achid.getKey().i);
                        ps.setInt(3, achid.getValue());
                        ps.execute();
                    }
                    ps.close();
                }
                /*
                 * 保存好友信息
                 */
                if (buddylist.changed()) {
                    deleteWhereCharacterId(con, "DELETE FROM buddies WHERE characterid = ?");
                    ps = con.prepareStatement("INSERT INTO buddies (characterid, `buddyid`, `pending`) VALUES (?, ?, ?)");
                    ps.setInt(1, id);
                    for (BuddylistEntry entry : buddylist.getBuddies()) {
                        ps.setInt(2, entry.getCharacterId());
                        ps.setInt(3, entry.isVisible() ? 5 : 7);
                        ps.execute();
                    }
                    ps.close();
                    buddylist.setChanged(false);
                }
                /*
                 * 保存角色点卷信息
                 */
                ps = con.prepareStatement("UPDATE accounts SET `points` = ?, `vpoints` = ? WHERE id = ?");
                ps.setInt(1, points);
                ps.setInt(2, vpoints);
                ps.setInt(3, accountid);
                ps.executeUpdate();
                ps.close();
                /*
                 * 保存仓库信息
                 */
                if (storage != null) {
                    storage.saveToDB(con);
                }
                /*
                 * 保存商城信息
                 */
                if (cs != null) {
                    cs.save(con);
                }
                PlayerNPC.updateByCharId(con, this);
                /*
                 * 保存键盘设置信息
                 */
                keylayout.saveKeys(con, id);
                /*
                 * 保存QuickSlot设置信息
                 */
                quickslot.saveQuickSlots(con, id);
                /*
                 * 保存坐骑信息
                 */
                if (mount != null) {
                    mount.saveMount(con, id);
                }
                /*
                 * 保存安卓信息
                 */
                if (android != null) {
                    android.saveToDb(con);
                }
                /*
                 * 保存怪物卡信息
                 */
                if (monsterbook != null) {
                    monsterbook.saveCards(con, accountid);
                }
                /*
                 * 保存Pvp属性信息
                 */
                if (pvpStats != null) {
                    pvpStats.saveToDb(con, accountid);
                }
                /*
                 * 保存药剂罐信息
                 */
                if (potionPot != null) {
                    potionPot.saveToDb(con);
                }
                /*
                 * 保存宝盒信息
                 */
                if (coreAura != null && coreAura.getId() == id) {
                    coreAura.saveToDb(con);
                }
                /*
                 * 保存怪怪信息
                 */
                deleteWhereCharacterId(con, "DELETE FROM familiars WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO familiars (characterid, familiar, name, level, exp, grade, skillid, option1, option2, option3) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (MonsterFamiliar familiar : familiars.values()) {
                    ps.setInt(2, familiar.getFamiliar());
                    ps.setString(3, familiar.getName());
                    ps.setInt(4, familiar.getLevel());
                    ps.setInt(5, familiar.getExp());
                    ps.setInt(6, familiar.getGrade());
                    ps.setInt(7, familiar.getSkill());
                    ps.setInt(8, familiar.getOption1());
                    ps.setInt(9, familiar.getOption2());
                    ps.setInt(10, familiar.getOption3());
                    ps.executeUpdate();
                }
                ps.close();
                /*
                 * 保存道具宝宝信息
                 */
                deleteWhereCharacterId(con, "DELETE FROM imps WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO imps (characterid, itemid, closeness, fullness, state, level) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, id);
                for (MapleImp imp : imps) {
                    if (imp != null) {
                        ps.setInt(2, imp.getItemId());
                        ps.setShort(3, imp.getCloseness());
                        ps.setShort(4, imp.getFullness());
                        ps.setByte(5, imp.getState());
                        ps.setByte(6, imp.getLevel());
                        ps.executeUpdate();
                    }
                }
                ps.close();
                /*
                 * 保存礼物信息
                 */
                if (changed_wishlist) {
                    deleteWhereCharacterId(con, "DELETE FROM wishlist WHERE characterid = ?");
                    for (int i = 0; i < getWishlistSize(); i++) {
                        ps = con.prepareStatement("INSERT INTO wishlist(characterid, sn) VALUES(?, ?) ");
                        ps.setInt(1, getId());
                        ps.setInt(2, wishlist[i]);
                        ps.executeUpdate();
                        ps.close();
                    }
                }
                /*
                 * 保存缩地石信息
                 */
                if (changed_trocklocations) {
                    /*
                     * rocks = new int[10];
                     * regrocks = new int[5];
                     * hyperrocks = new int[13];
                     */
                    deleteWhereCharacterId(con, "DELETE FROM trocklocations WHERE characterid = ?");
                    for (int regrock : regrocks) {
                        if (regrock != 999999999) {
                            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 0)");
                            ps.setInt(1, getId());
                            ps.setInt(2, regrock);
                            ps.executeUpdate();
                            ps.close();
                        }
                    }
                    for (int rock : rocks) {
                        if (rock != 999999999) {
                            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 1)");
                            ps.setInt(1, getId());
                            ps.setInt(2, rock);
                            ps.executeUpdate();
                            ps.close();
                        }
                    }
                    for (int hyperrock : hyperrocks) {
                        if (hyperrock != 999999999) {
                            ps = con.prepareStatement("INSERT INTO trocklocations(characterid, mapid, vip) VALUES (?, ?, 2)");
                            ps.setInt(1, getId());
                            ps.setInt(2, hyperrock);
                            ps.executeUpdate();
                            ps.close();
                        }
                    }
                }
                /*
                 * 保存矿物背包信息
                 */
                if (changed_extendedSlots) {
                    deleteWhereCharacterId(con, "DELETE FROM extendedSlots WHERE characterid = ?");
                    for (Entry<Byte, List<Integer>> entry : extendedSlots.entrySet()) {
                        for (Integer i : entry.getValue()) {
                            if (getInventory(MapleInventoryType.USE).findById(i) != null
                                    || getInventory(MapleInventoryType.SETUP).findById(i) != null
                                    || getInventory(MapleInventoryType.ETC).findById(i) != null) {

                                ps = con.prepareStatement("INSERT INTO extendedSlots(characterid, itemId) VALUES(?, ?) ");
                                ps.setInt(1, getId());
                                ps.setInt(2, i);
                                ps.executeUpdate();
                                ps.close();
                            }
                        }
                    }
                }
                /*
                 * 保存自定义积分信息
                 */
                deleteWhereCharacterId(con, "DELETE FROM character_credit WHERE characterid = ?");
                ps = con.prepareStatement("INSERT INTO character_credit(characterid, name, value) VALUES(?, ?, ?)");
                for (Entry<String, Integer> i : credit.entrySet()) {
                    ps.setInt(1, getId());
                    ps.setString(2, i.getKey());
                    ps.setInt(3, i.getValue());
                    ps.executeUpdate();
                }
                ps.close();
                /*
                  保存效果开关
                 */
                deleteWhereCharacterId(con, "DELETE FROM effectswitch WHERE `characterid` = ?");
                ps = con.prepareStatement("INSERT INTO effectswitch (characterid, pos) VALUES (?, ?)");
                for (Integer effect : effectSwitch) {
                    ps.setInt(1, getId());
                    ps.setInt(2, effect);
                    ps.executeUpdate();
                }
                ps.close();
                if (changed_vcores) {
                    deleteWhereCharacterId(con, "DELETE FROM vcoreskill WHERE characterid = ?");
                    for (VCoreSkillEntry vCoreSkillEntry : vCoreSkills.values()) {
                        if (vCoreSkillEntry.getSlot() > 0) {
                            ps = con.prepareStatement("INSERT INTO vcoreskill (characterid, vcoreid, level, exp, skill1, skill2, skill3, slot) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                            ps.setInt(1, getId());
                            ps.setInt(2, vCoreSkillEntry.getVcoreid());
                            ps.setInt(3, vCoreSkillEntry.getLevel());
                            ps.setInt(4, vCoreSkillEntry.getExp());
                            ps.setInt(5, vCoreSkillEntry.getSkill1());
                            ps.setInt(6, vCoreSkillEntry.getSkill2());
                            ps.setInt(7, vCoreSkillEntry.getSkill3());
                            ps.setInt(8, vCoreSkillEntry.getSlot());
                            ps.execute();
                            ps.close();
                        }
                    }
                }
                isSaveing = false;
                changed_wishlist = false;
                changed_trocklocations = false;
                changed_skillmacros = false;
                changed_savedlocations = false;
                changed_pokemon = false;
                changed_questinfo = false;
                changed_achievements = false;
                changed_extendedSlots = false;
                changed_skills = false;
                changed_reports = false;
                changed_keyValue = false;
                changed_vcores = false;
                con.commit();
            }
        } catch (SQLException e) {
            log.error("[charsave] 保存角色数据出现错误 .", e);
            try {
                con.rollback();
            } catch (SQLException ex) {
                log.error("[charsave] Error Rolling Back", e);
            }
        } finally {
            try {
                if (ps != null) {
                    ps.close();
                }
                if (pse != null) {
                    pse.close();
                }
                if (rs != null) {
                    rs.close();
                }
                con.setAutoCommit(true);
                con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                if (con != null) {
                    con.close();
                }
            } catch (SQLException e) {
                log.error("[charsave] Error going back to autocommit mode", e);
            }
        }
    }

    private void deleteWhereCharacterId(Connection con, String sql) throws SQLException {
        deleteWhereCharacterId(con, sql, id);
    }

    public final int[] getFriendShipPoints() {
        return friendshippoints;
    }

    public final void setFriendShipPoints(int joejoe, int hermoninny, int littledragon, int ika, int Wooden) {
        this.friendshippoints[0] = joejoe;
        this.friendshippoints[1] = hermoninny;
        this.friendshippoints[2] = littledragon;
        this.friendshippoints[3] = ika;
        this.friendshippoints[4] = Wooden;
    }

    public final int getFriendShipToAdd() {
        return friendshiptoadd;
    }

    public final void setFriendShipToAdd(int points) {
        this.friendshiptoadd = points;
    }

    public final void addFriendShipToAdd(int points) {
        this.friendshiptoadd += points;
    }

    /*
     * 获得角色的属性状态
     */
    public PlayerStats getStat() {
        return stats;
    }

    /*
     * 获得角色的特殊属性状态
     */
    public PlayerSpecialStats getSpecialStat() {
        return specialStats;
    }

    /*
     * 地图时间
     */
    public void cancelMapTimeLimitTask() {
        if (mapTimeLimitTask != null) {
            mapTimeLimitTask.cancel(false);
            mapTimeLimitTask = null;
        }
    }

    public void startQuestTimeLimitTask(int quest, int time) {
        send(MaplePacketCreator.startQuestTimeLimit(quest, time));
        questTimeLimitTask = MapTimer.getInstance().register(() -> send(MaplePacketCreator.stopQuestTimeLimit(quest)), time);
    }

    public void startMapTimeLimitTask(int time, final MapleMap to) {
        if (time <= 0) { //jail
            time = 1;
        }
        cancelMapTimeLimitTask();
        client.announce(MaplePacketCreator.getClock(time));
        final MapleMap ourMap = getMap();
        time *= 1000;
        mapTimeLimitTask = MapTimer.getInstance().register(() -> {
            if (ourMap.getId() == GameConstants.JAIL) {
                getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData("0"); //release them!
            }
            changeMap(to, to.getPortal(0));
        }, time, time);
    }

    public int getTouchedRune() {
        return touchedrune;
    }

    public void setTouchedRune(int type) {
        touchedrune = type;
    }

    public long getRuneTimeStamp() {
        return Long.parseLong(getKeyValue("LastTouchedRune"));
    }

    public void setRuneTimeStamp(long time) {
        setKeyValue("LastTouchedRune", String.valueOf(time));
    }

    /*
     * 新增角色特殊操作处理
     */
    public Map<String, String> getKeyValue_Map() {
        return keyValue;
    }

    public String getKeyValue(String key) {
        if (keyValue.containsKey(key)) {
            return keyValue.get(key);
        }
        return null;
    }

    public void setKeyValue(String key, String values) {
        keyValue.put(key, values);
        changed_keyValue = true;
    }

    /*
     * 任务信息
     */
    public void updateInfoQuest(int questid, String data) {
        updateInfoQuest(questid, data, true);
    }

    public void updateInfoQuest(int questid, String data, boolean show) {
        questinfo.put(questid, data);
        changed_questinfo = true;
        if (show) {
            client.announce(MaplePacketCreator.updateInfoQuest(questid, data));
        }
    }

    public void removeInfoQuest(int questid) {
        if (questinfo.containsKey(questid)) {
            this.updateInfoQuest(questid, "");
            questinfo.remove(questid);
            changed_questinfo = true;
        }
    }

    public String getInfoQuest(int questid) {
        if (questinfo.containsKey(questid)) {
            return questinfo.get(questid);
        }
        return "";
    }

    public String getInfoQuestStatS(int id, String stat) {
        String info = getInfoQuest(id);
        if (info != null && info.length() > 0 && info.contains(stat)) {
            int startIndex = info.indexOf(stat) + stat.length() + 1;
            int until = info.indexOf(";", startIndex);
            return info.substring(startIndex, until != -1 ? until : info.length());
        }
        return "";
    }

    public int getInfoQuestStat(int id, String stat) {
        String statz = getInfoQuestStatS(id, stat);
        return (statz == null || "".equals(statz)) ? 0 : Integer.parseInt(statz);
    }

    public PlayerObservable getPlayerObservable() {
        return playerObservable;
    }

    public void setInfoQuestStat(int id, String stat, int statData) {
        setInfoQuestStat(id, stat, statData);
    }

    public void setInfoQuestStat(int id, String stat, String statData) {
        String info = getInfoQuest(id);
        if (info.length() == 0 || !info.contains(stat)) {
            updateInfoQuest(id, stat + "=" + statData + (info.length() == 0 ? "" : ";") + info);
        } else {
            String newInfo = stat + "=" + statData;
            String beforeStat = info.substring(0, info.indexOf(stat));
            int from = info.indexOf(";", info.indexOf(stat) + stat.length());
            String afterStat = from == -1 ? "" : info.substring(from + 1);
            updateInfoQuest(id, beforeStat + newInfo + (afterStat.length() != 0 ? (";" + afterStat) : ""));
        }
    }

    /*
     * 检测任务ID中完成的信息中是否包含 某个字符
     */
    public boolean containsInfoQuest(int questid, String data) {
        return questinfo.containsKey(questid) && questinfo.get(questid).contains(data);
    }

    public int getNumQuest() {
        int i = 0;
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !(q.isCustom())) {
                i++;
            }
        }
        return i;
    }

    public byte getQuestStatus(int questId) {
        MapleQuest qq = MapleQuest.getInstance(questId);
        if (getQuestNoAdd(qq) == null) {
            return 0;
        }
        return getQuestNoAdd(qq).getStatus();
    }

    public MapleQuestStatus getQuest(MapleQuest quest) {
        if (!quests.containsKey(quest.getId())) {
            return new MapleQuestStatus(quest, (byte) 0);
        }
        return quests.get(quest.getId());
    }

    public boolean needQuestItem(int questId, int itemId) {
        if (questId <= 0) {
            return true;
        }
        MapleQuest quest = MapleQuest.getInstance(questId);
        return getInventory(ItemConstants.getInventoryType(itemId)).countById(itemId) < quest.getAmountofItems(itemId);
    }

    public void setQuestAdd(MapleQuest quest, byte status, String customData) {
        if (!quests.containsKey(quest.getId())) {
            MapleQuestStatus stat = new MapleQuestStatus(quest, status);
            stat.setCustomData(customData);
            quests.put(quest.getId(), stat);
        }
    }

    public MapleQuestStatus getQuestNAdd(MapleQuest quest) {
        if (!quests.containsKey(quest.getId())) {
            MapleQuestStatus status = new MapleQuestStatus(quest, (byte) 0);
            quests.put(quest.getId(), status);
            return status;
        }
        return quests.get(quest.getId());
    }

    public MapleQuestStatus getQuestNoAdd(MapleQuest quest) {
        return quests.get(quest.getId());
    }

    public MapleQuestStatus getQuestRemove(MapleQuest quest) {
        return quests.remove(quest.getId());
    }

    /*
     * 更新任务信息
     */
    public void updateQuest(MapleQuestStatus quest) {
        updateQuest(quest, false);
    }

    public void updateQuest(MapleQuestStatus quest, boolean update) {
        quests.put(quest.getQuest().getId(), quest);
        if (!quest.isCustom()) { //如果任务ID小于 99999 也就是说不是自定义任务
            client.announce(MaplePacketCreator.updateQuest(quest));
            if (quest.getStatus() == 1 && !update) {
                client.announce(MaplePacketCreator.updateQuestInfo(quest.getQuest().getId(), quest.getNpc(), 0, quest.getStatus() == 1));
            }
        }
    }

    public Map<Integer, String> getInfoQuest_Map() {
        return questinfo;
    }

    public Map<Integer, MapleQuestStatus> getQuest_Map() {
        return quests;
    }

    /*
     * 钓鱼设置
     */
    public void startFishingTask() {
        if (FishingConfig.FISHING_ENABLE) {
            cancelFishingTask();
            lastFishingTime = System.currentTimeMillis();
            int fishingTime = isGM() ? FishingConfig.FISHING_TIME_GM : (stats.canFishVIP ? FishingConfig.FISHING_TIME_VIP : FishingConfig.FISHING_TIME);
            this.dropMessage(-1, "开始钓鱼，当前钓鱼间隔时长为：" + (fishingTime / 1000) + "秒。");
            this.dropMessage(-11, "开始钓鱼，当前钓鱼间隔时长为：" + (fishingTime / 1000) + "秒。");
        }
    }

    public boolean canFish(long now) {
        if (!FishingConfig.FISHING_ENABLE) {
            return false;
        }
        int fishingTime = isGM() ? FishingConfig.FISHING_TIME_GM : (stats.canFishVIP ? FishingConfig.FISHING_TIME_VIP : FishingConfig.FISHING_TIME);
        return lastFishingTime > 0 && lastFishingTime + fishingTime < now;
    }

    public void doFish(long now) {
        lastFishingTime = now;
        boolean expMulti = haveItem(2300001, 1, false, true); //高级鱼饵
        if (client == null || client.getPlayer() == null || !client.isReceiving() || !GameConstants.isFishingMap(getMapId()) || !stats.canFish) {
            cancelFishingTask();
            return;
        }
        if (!expMulti && !haveItem(2300000, 1, false, true)) {
            cancelFishingTask();
            dropSpouseMessage(0x19, "[钓鱼系统] 鱼饵不足，已停止钓鱼。");
            return;
        }
        if (chair <= 0) {
            cancelFishingTask();
            dropSpouseMessage(0x19, "[钓鱼系统] 未坐在椅子上，已停止钓鱼。");
            return;
        }
        //先扣除鱼饵
        MapleInventoryManipulator.removeById(client, MapleInventoryType.USE, expMulti ? 2300001 : 2300000, 1, false, false);
        //钓鱼成功的概率
        int chance = isGM() ? FishingConfig.FISHING_CHANCE_GM : (stats.canFishVIP ? FishingConfig.FISHING_CHANCE_VIP : FishingConfig.FISHING_CHANCE);
        if (Randomizer.nextInt(100) > chance) {
            dropSpouseMessage(0x19, "[钓鱼系统] 鱼儿奋力挣扎了一番逃跑了。");
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        RewardDropEntry drop = MapleMonsterInformationProvider.getInstance().getReward(mapid, expMulti ? 2300001 : 2300000);
        if (drop == null || !ii.itemExists(drop.itemId)) {
            boolean expReward = Randomizer.nextBoolean();
            if (expReward) {
                //获得经验
                int experi = Math.min(Randomizer.nextInt(Math.max(1, (int) Math.abs(getExpNeededForLevel() / 250))), 300000);
                gainExp(expMulti ? (experi * 3 / 2) : experi, true, true, true);
            } else {
                //获得金币
                int money = Randomizer.rand(expMulti ? 9000 : 6000, expMulti ? 75000 : 50000);
                gainMeso(money, true);
            }
            return;
        }
        if (!MapleInventoryManipulator.checkSpace(client, drop.itemId, 1, "")) {
            cancelFishingTask();
            dropSpouseMessage(0x19, "[钓鱼系统] 背包空间不足，已停止钓鱼。");
            return;
        }
        Item item;
        if (ItemConstants.getInventoryType(drop.itemId) == MapleInventoryType.EQUIP) {
            item = ii.randomizeStats((Equip) ii.getEquipById(drop.itemId));
            if (drop.state > 0) {
                ii.setPotentialState((Equip) item, drop.state);
            }
            if (drop.period > 0) {
                item.setExpiration(System.currentTimeMillis() + drop.period * 24 * 60 * 60 * 1000);
            }
            item.setGMLog("钓鱼获得 时间 " + DateUtil.getCurrentDate());
            MapleInventoryManipulator.addbyItem(client, item);
        } else {
            item = new Item(drop.itemId, (byte) 0, (short) 1, (byte) 0);
            MapleInventoryManipulator.addById(client, drop.itemId, (short) 1, "钓鱼获得 时间 " + DateUtil.getCurrentDate());
        }
        if (drop.msgType == 1 || drop.msgType == 2 || drop.msgType == 3) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.getGachaponMega(getName(), " : 钓鱼获得{" + ii.getName(item.getItemId()) + "}！大家一起恭喜他（她）吧！！！！", item, drop.msgType, client.getChannel()));
        } else {
            this.dropMessage(5, "[钓鱼系统] 获得 " + ii.getName(item.getItemId()));
        }
        //map.broadcastMessage(UIPacket.fishingCaught(id));
    }

    public void cancelFishingTask() {
        lastFishingTime = 0;
    }

    /*
     * buff状态设置
     */
    public MaplePlayerBuffManager getBuffManager() {
        return buffManager;
    }

    public ArrayList<Pair<MapleBuffStat, MapleBuffStatValueHolder>> getAllEffects() {
        return new ArrayList<>(effects);
    }

    public MapleBuffStatValueHolder getBuffStatValueHolder(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = null;
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            if (buffs == null) {
                continue;
            }
            if (buffs.getLeft() == stat) {
                mbsvh = buffs.getRight();
            }
        }
        return mbsvh;
    }

    public boolean isActiveBuffedValue(int skillid) {
        return buffManager.isActiveBuffedValue(skillid) && !isGM();
    }

    public Integer getBuffedValueNew(MapleBuffStat effect) {
        return buffManager.getBuffedValue(effect);
    }

    public Integer getBuffedValue(MapleBuffStat stat) {
        if (stat.canStack()) {
            int value = 0;
            boolean find = false;
            for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
                if (buffs.getLeft() == stat) {
                    find = true;
                    value += buffs.getRight().value;
                }
            }
            return find ? value : null;
        } else {
            MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
            if (mbsvh == null) {
                return null;
            }
            return mbsvh.value;
        }
    }

    public int getBuffedIntValue(MapleBuffStat stat) {
        if (stat.canStack()) {
            int value = 0;
            for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
                if (buffs.getLeft() == stat) {
                    value += buffs.getRight().value;
                }
            }
            return value;
        } else {
            MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
            if (mbsvh == null) {
                return 0;
            }
            return mbsvh.value;
        }
    }

    public Integer getBuffedSkill_X(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getX();
    }

    public Integer getBuffedSkill_Y(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect.getY();
    }

    public boolean isBuffFromNew(MapleBuffStat stat, Skill skill) {
        return buffManager.isBuffFrom(stat, skill.getId());
    }

    public boolean isBuffFrom(MapleBuffStat stat, Skill skill) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        return mbsvh != null && mbsvh.effect != null && skill != null && mbsvh.effect.isSkill() && mbsvh.effect.getSourceid() == skill.getId();
    }

    public boolean hasBuffSkill(int skillId) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        boolean find = false;
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceid() == skillId) {
                find = true;
                break;
            }
        }
        allBuffs.clear();
        return find;
    }

    public int getBuffSourceNew(MapleBuffStat stat) {
        return buffManager.getBuffSource(stat);
    }

    public int getBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return -1;
        }
        return mbsvh.effect.getSourceid();
    }

    public List<MapleStatEffect> getBuffEffects() {
        return buffManager.getBuffEffects();
    }

    public int getTrueBuffSource(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return -1;
        }
        return (mbsvh.effect.isSkill() ? mbsvh.effect.getSourceid() : -mbsvh.effect.getSourceid());
    }

    public void setBuffedValueNew(MapleBuffStat effect, int value) {
        buffManager.setBuffedValue(effect, value);
    }

    public void setBuffedValue(MapleBuffStat stat, int value) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return;
        }
        mbsvh.value = value;
    }

    public void setSchedule(MapleBuffStat stat, ScheduledFuture<?> sched) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return;
        }
        mbsvh.schedule.cancel(false);
        mbsvh.schedule = sched;
    }

    public Long getBuffedStartTimeNew(MapleBuffStat effect) {
        return buffManager.getBuffedStarttime(effect);
    }

    public Long getBuffedStartTime(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.startTime;
    }

    public MapleStatEffect getStatForBuffNew(MapleBuffStat effect) {
        return buffManager.getStatForBuff(effect);
    }

    public MapleStatEffect getStatForBuff(MapleBuffStat stat) {
        MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
        if (mbsvh == null) {
            return null;
        }
        return mbsvh.effect;
    }

    /*
     * 登记注册角色的BUFF状态效果
     */
    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, List<Pair<MapleBuffStat, Integer>> stat) {
        afterRegister(effect);
        buffManager.registerEffect(effect, starttime, schedule, stat);
        stats.recalcLocalStats(this);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, int from) {
        registerEffect(effect, starttime, schedule, effect.getStatups(), false, effect.getDuration(), from);
    }

    public void registerEffect(MapleStatEffect effect, long starttime, ScheduledFuture<?> schedule, Map<MapleBuffStat, Integer> statups, boolean silent, int localDuration, int from) {
        afterRegister(effect);
        statups.forEach((left, right) -> effects.add(new Pair<>(left, new MapleBuffStatValueHolder(effect, starttime, schedule, right, localDuration, from))));
        if (!silent && !effect.is血之契约()) { //这个不是切换频道注册的角色BUFF 就要重置下角色属性 血之契约需要过滤下，否则会陷入死循环
            stats.recalcLocalStats(this);
        }
        if (isShowPacket()) {
            dropDebugMessage(1, "[BUFF信息] 注册BUFF效果 - 当前BUFF总数: " + effects.size() + " 技能ID: " + effect.getSourceid());
        }
    }

    private void afterRegister(MapleStatEffect effect) {
        if (effect.is隐藏术()) {
            map.broadcastMessage(this, MaplePacketCreator.removePlayerFromMap(getId()), false);
        } else if (effect.is龙之力()) {
            prepareDragonBlood();
        } else if (effect.is团队治疗()) {
            prepareRecovery();
        } else if (effect.is重生契约状态()) {
            checkBerserk();
        } else if (effect.is骑兽技能_()) {
            getMount().startSchedule();
        } else if (effect.is恶魔恢复()) {
            prepareRecoveryEM();
        }
    }

    public List<MapleBuffStat> getBuffStats(MapleStatEffect effect, long startTime) {
        List<MapleBuffStat> ret = new ArrayList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> stateffect : getAllEffects()) {
            MapleBuffStatValueHolder mbsvh = stateffect.getRight();
            if (mbsvh.effect.sameSource(effect) && (startTime == -1 || startTime == mbsvh.startTime)) { //|| stateffect.getLeft().canStack()
                ret.add(stateffect.getLeft());
            }
        }
        return ret;
    }

    public SpecialBuffInfo getStackBuffInfo(MapleBuffStat stat, int n2) {
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> stateffect : this.getAllEffects()) {
            MapleBuffStat buffStat = stateffect.getLeft();
            MapleBuffStatValueHolder mbsvh = stateffect.getRight();
            int sourceid = mbsvh.effect.getSourceid();
            Integer n4 = null;
            if (SkillConstants.c(buffStat)) {
                if (n2 == sourceid && stat == buffStat) {
                    if (sourceid == 11121011 && buffStat == MapleBuffStat.月光转换) {
                        sourceid = 11111022;
                    } else if (sourceid == 11121012 && buffStat == MapleBuffStat.月光转换) {
                        sourceid = 11101022;
                    } else if ((sourceid == 11121012 || sourceid == 11121011) && buffStat == MapleBuffStat.霰弹炮) {
                        sourceid = sourceid == 11121012 ? 11101022 : 11121011;
                    } else if (sourceid == 51001011 || sourceid == 51001012 || sourceid == 51001013 || sourceid == 51111011 || sourceid == 51111012) {
                        sourceid = 51001005;
                        mbsvh.fromChrId = 12000;
                    } else if (sourceid == 1320019) {
                        n4 = 1;
                    }
                    return new SpecialBuffInfo(mbsvh.effect.isSkill() ? sourceid : -sourceid, n4 != null ? n4 : mbsvh.value, mbsvh.fromChrId, (int) mbsvh.startTime);
                }
                switch (n2) {
                    case 21120019:
                    case 21120023: {
                        return new SpecialBuffInfo(mbsvh.effect.isSkill() ? sourceid : -sourceid, n4 != null ? n4 : mbsvh.value, mbsvh.fromChrId, (int) mbsvh.startTime);
                    }
                }
            }
        }
        return null;
    }

    /*
     * 一些特殊的BUFF 需要写几次的那种
     */
    public List<SpecialBuffInfo> getSpecialBuffInfo(MapleBuffStat stat) {
        List<SpecialBuffInfo> ret = new ArrayList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> stateffect : getAllEffects()) {
            MapleBuffStatValueHolder mbsvh = stateffect.getRight();
            int skillId = mbsvh.effect.getSourceid();
            if (stateffect.getLeft() == stat) {
                ret.add(new SpecialBuffInfo(mbsvh.effect.isSkill() ? skillId : -skillId, mbsvh.value, mbsvh.localDuration, mbsvh.startTime));
            }
        }
        return ret;
    }

//    private boolean canRemoveSummon(MapleSummon summon, int summonId, long l2) {
//        return summon.getSkillId() == summonId && !summon.isMutipleSummon() && (summon.getCreateTime() == l2 || l2 == -1)
//                || summon.getSkillId() == summonId && summonId == 14000027 && (summon.getCreateTime() == l2 || l2 == -1)
//                || summon.getSkillId() == summonId && summon.isMutipleSummon() && summon.getCreateTime() == l2
//                || summonId == 35121009 && summon.getSkillId() == 35121011
//                || summon.getSkillId() == 12120013
//                || summon.getSkillId() == 12120014
//                || summon.getSkillId() == summonId + 999
//                || (summonId == 1085
//                || summonId == 1087
//                || summonId == 1090) && summon.getSkillId() == summonId - 999;
//    }

    /*
     * 一些特殊的BUFF 需要写几次的那种
     */
    public List<SpecialBuffInfo> getSpecialBuffInfo(MapleBuffStat stat, int buffid, int value, int bufflength) {
        List<SpecialBuffInfo> ret = new ArrayList<>();
        long time = System.currentTimeMillis();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> stateffect : getAllEffects()) {
            MapleBuffStatValueHolder mbsvh = stateffect.getRight();
            int skillId = mbsvh.effect.getSourceid();
            if (stateffect.getLeft() == stat && skillId != Math.abs(buffid)) { //取绝对值
                ret.add(new SpecialBuffInfo(mbsvh.effect.isSkill() ? skillId : -skillId, mbsvh.value, mbsvh.localDuration, mbsvh.startTime));
            }
        }
        ret.add(new SpecialBuffInfo(buffid, value, bufflength, time)); //添加默认
        return ret;
    }

    /**
     * 取消角色注册的BUFF状态信息
     */
    private void deregisterBuffStats(List<MapleBuffStat> stats, MapleStatEffect effect, boolean overwrite) {
        int effectSize = effects.size();
        ArrayList<Pair<MapleBuffStat, MapleBuffStatValueHolder>> effectsToRemove = new ArrayList<>();
        stats.forEach(stat -> getAllEffects().forEach(buffs -> {
            if (buffs.getLeft() == stat && (effect == null || buffs.getRight().effect.sameSource(effect))) {
                effectsToRemove.add(buffs);
                MapleBuffStatValueHolder mbsvh = buffs.getRight();
                if (stat == MapleBuffStat.召唤兽
                        || stat == MapleBuffStat.灵魂助力
                        || stat == MapleBuffStat.DAMAGE_BUFF
                        || stat == MapleBuffStat.地雷
                        || stat == MapleBuffStat.增加物理攻击力
                        || stat == MapleBuffStat.影子侍从
                        || stat == MapleBuffStat.召唤美洲豹) {
                    int summonId = mbsvh.effect.getSourceid();
                    List<MapleSummon> toRemove = new ArrayList<>();
                    visibleMapObjectsLock.writeLock().lock();
                    summonsLock.writeLock().lock();
                    try {
                        for (MapleSummon summon : summons) {
                            if (summon.getSkillId() == summonId
                                    || ((summonId == 86 || summonId == 88 || summonId == 91) && summon.getSkillId() == summonId + 999)
                                    || ((summonId == 1085 || summonId == 1087 || summonId == 1090) && summon.getSkillId() == summonId - 999)
                                    || SkillConstants.is美洲豹(summon.getSkillId())
                                    || (summonId == 双弩.精灵骑士 || summonId == 双弩.精灵骑士1 || summonId == 双弩.精灵骑士2)) {
                                map.broadcastMessage(SummonPacket.removeSummon(summon, overwrite));
                                map.removeMapObject(summon);
                                visibleMapObjects.remove(summon);
                                toRemove.add(summon);
                            }
                        }
                        for (MapleSummon ss : toRemove) {
                            summons.remove(ss);
                        }
                    } finally {
                        summonsLock.writeLock().unlock();
                        visibleMapObjectsLock.writeLock().unlock();
                    }
                    if (summonId == 神射手.火凤凰 || summonId == 箭神.冰凤凰) {
                        cancelEffectFromBuffStat(MapleBuffStat.精神连接);
                    }
                } else if (stat == MapleBuffStat.龙之力) {
                    lastDragonBloodTime = 0;
                } else if (stat == MapleBuffStat.恢复效果 || mbsvh.effect.getSourceid() == 机械师.金属机甲_战车) {
                    lastRecoveryTime = 0;
                } else if (stat == MapleBuffStat.导航辅助 || stat == MapleBuffStat.神秘瞄准术) {
                    linkMobs.clear();
                } else if (stat == MapleBuffStat.恶魔恢复) {
                    lastRecoveryTimeEM = 0;
                } else if (stat == MapleBuffStat.避柳) {
                    this.setBuffedValue(stat, 0);
                }
            }
        }));
        int toRemoveSize = effectsToRemove.size();
        effectsToRemove.forEach(toRemove -> {
            if (effects.contains(toRemove)) {
                if (toRemove.getRight().schedule != null) {
                    toRemove.getRight().schedule.cancel(false);
                    toRemove.getRight().schedule = null;
                }
                effects.remove(toRemove);
            }
        });
        effectsToRemove.clear();
        boolean ok = (effectSize - effects.size() == toRemoveSize);
        if (isShowPacket()) {
            dropDebugMessage(1, "[BUFF信息] 取消BUFF 以前BUFF总数: " + effectSize + " 现在BUFF总数 " + effects.size() + " 取消的BUFF数量: " + toRemoveSize + " 是否相同: " + ok);
        }
        if (!ok) {
            log.error("取消BUFF错误", this.getName() + " - " + this.getJob() + " 取消BUFF出现错误 技能ID: " + (effect != null ? effect.getSourceid() : "???"), true);
        }
    }

    /**
     * @param effect
     * @param overwrite 是否重新使用BUFF 当为重新使用的时候要取消 状态中的 MapleBuffStat 无需发送取消BUFF的封包
     * @param startTime
     */
    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime) {
        if (effect == null) {
            return;
        }
        cancelEffect(effect, overwrite, startTime, effect.getStatups());
    }

    public void cancelEffect(MapleStatEffect effect, boolean overwrite, long startTime, Map<MapleBuffStat, Integer> statups) {
        if (effect == null) {
            return;
        }
        List<MapleBuffStat> buffstats;
        if (!overwrite) { //不是重新使用BUFF状态
            buffstats = getBuffStats(effect, startTime);
        } else {
            buffstats = new ArrayList<>(statups.size());
            buffstats.addAll(statups.keySet());
        }
        if (ServerConstants.isShowPacket()) {
            log.info("取消技能BUFF: - buffstats.size() " + buffstats.size());
        }
        if (buffstats.size() <= 0) {
            if (effect.is战法灵气()) {
                cancelEffectFromBuffStat(MapleBuffStat.战法灵气);
            } else if (effect.is狂风肆虐()) {
                cancelEffectFromBuffStat(MapleBuffStat.狂风肆虐);
            }
            return;
        }
        if (ServerConstants.isShowPacket()) {
            log.info("开始取消技能BUFF: - 1");
        }
        if (effect.is终极无限() && getBuffedValue(MapleBuffStat.终极无限) != null) {
            int duration = Math.max(effect.getDuration(), effect.alchemistModifyVal(this, effect.getDuration(), false));
            long start = getBuffedStartTime(MapleBuffStat.终极无限);
            duration += (int) ((start - System.currentTimeMillis()));
            if (duration > 0) {
                int neworbcount = getBuffedValue(MapleBuffStat.终极无限) + effect.getDamage();
                Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.终极无限, neworbcount);
                setBuffedValue(MapleBuffStat.终极无限, neworbcount);
                client.announce(BuffPacket.giveBuff(effect.getSourceid(), duration, stat, effect, this));
                addHP((int) (effect.getHpR() * this.stats.getCurrentMaxHp()));
                addMP((int) (effect.getMpR() * this.stats.getCurrentMaxMp(this.getJob())));
                setSchedule(MapleBuffStat.终极无限, BuffTimer.getInstance().schedule(new CancelEffectAction(this, effect, start, stat), effect.alchemistModifyVal(this, 4000, false)));
                return;
            }
        }
        //取消注册的BUFF信息
        deregisterBuffStats(buffstats, effect, overwrite);
        if (effect.is时空门()) {
            if (!getDoors().isEmpty()) {
                removeDoor();
                silentPartyUpdate();
            }
        } else if (effect.is机械传送门()) {
            if (!getMechDoors().isEmpty()) {
                removeMechDoor();
            }
        } else if (effect.is骑兽技能_()) {
            getMount().cancelSchedule();
        } else if (effect.is金属机甲()) {
            cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
        } else if (effect.is拔刀姿势() && !overwrite) {
            cancelEffectFromBuffStat(MapleBuffStat.拔刀术加成);
            SkillFactory.getSkill(剑豪.基本姿势加成).getEffect(1).applyTo(this);
        }
        //取消角色的BUFF状态信息和发送取消BUFF的封包
        cancelPlayerBuffs(buffstats, overwrite);
        if (!overwrite) {
            if (effect.is隐藏术() && client.getChannelServer().getPlayerStorage().getCharacterById(this.getId()) != null) { //Wow this is so fking hacky...
                map.broadcastMessage(this, MaplePacketCreator.spawnPlayerMapobject(this), false);
                map.broadcastMessage(this, EffectPacket.getEffectSwitch(getId(), getEffectSwitch()), true);
            }
        }
//        if (effect.getSourceid() == 机械师.金属机甲_重机枪_4转 && !overwrite) { //when siege 2 deactivates, missile re-activates
//            SkillFactory.getSkill(机械师.金属机甲_原型).getEffect(getTotalSkillLevel(机械师.金属机甲_原型)).applyTo(this);
//        }
        if (effect.is夜光平衡() && !overwrite) {
            int skillId = effect.getSourceid() == 夜光.平衡_光明 ? 夜光.月蚀 : 夜光.太阳火焰;
            SkillFactory.getSkill(skillId).getEffect(1).applyTo(this);
        } else if (effect.is狂龙变形() || effect.is狂龙超级变形()) {
            if (getBuffedValue(MapleBuffStat.剑刃之壁) != null) {
                SkillFactory.getSkill(狂龙战士.剑刃之壁).getEffect(getSkillLevel(狂龙战士.剑刃之壁)).applyTo(this);
            }
        }
        if (isShowPacket()) {
            dropDebugMessage(1, "[BUFF信息] 取消BUFF 当前BUFF总数: " + effects.size() + " 技能: " + effect.getSourceid());
        }
    }

    /**
     * 取消指定的的 MapleBuffStat 集合的BUFF状态
     */
    public void cancelBuffStats(MapleBuffStat... stat) {
        List<MapleBuffStat> buffStatList = Arrays.asList(stat);
        deregisterBuffStats(buffStatList, null, false);
        cancelPlayerBuffs(buffStatList, false);
    }

    /*
     * 取消指定的MapleBuffStat 的BUFF状态
     */
    public void cancelEffectFromBuffStatNew(MapleBuffStat stat) {
        MaplePlayerBuffManager.MapleBuff buff = buffManager.getBuff(stat);
        if (buff != null) {
            cancelEffect(buff.getEffect(), false, -1);
        }
    }

    public void cancelEffectFromBuffStat(MapleBuffStat stat) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            if (buffs.getLeft() == stat) {
                allBuffs.add(buffs.getRight());
            }
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, -1);
        }
        allBuffs.clear();
    }

    /**
     * 取消指定的MapleBuffStat 的BUFF状态
     */
    public void cancelEffectFromBuffStat(MapleBuffStat stat, int from) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            if (buffs.getLeft() == stat && buffs.getRight().fromChrId == from) {
                allBuffs.add(buffs.getRight());
            }
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, -1);
        }
        allBuffs.clear();
    }

    /**
     * 发送取消角色的BUFF状态封包
     */
    private void cancelPlayerBuffs(List<MapleBuffStat> buffstats, boolean overwrite) {
        boolean write = client != null && client.getChannelServer() != null && client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
        if (write) {
            if (buffstats.contains(MapleBuffStat.导航辅助)) {
                client.announce(BuffPacket.cancelBuff(MapleBuffStat.导航辅助));
                return;
            }
            if (buffstats.contains(MapleBuffStat.元素属性)) {
                buffValue = 0;
            }
            if (buffstats.contains(MapleBuffStat.召唤兽)) {
                buffstats.remove(MapleBuffStat.召唤兽);
                if (buffstats.size() <= 0) {
                    return;
                }
            }
            if (overwrite) {
                List<MapleBuffStat> buffStatX = new ArrayList<>();
                for (MapleBuffStat stat : buffstats) {
                    if (stat.canStack()) {
                        buffStatX.add(stat);
                    }
                }
                if (buffStatX.size() <= 0 || buffstats.contains(MapleBuffStat.击杀点数)) {
                    if (isShowPacket()) {
                        dropDebugMessage(1, "[BUFF信息] 取消BUFF - 不发送封包");
                    }
                    return; //无需发送任何封包 不作处理
                } else {
                    buffstats = buffStatX;
                }
            }
            if (isShowPacket()) {
                dropDebugMessage(1, "[BUFF信息] 取消BUFF效果 - 发送封包 - 是否注册BUFF时间: " + overwrite);
            }
            client.announce(BuffPacket.cancelBuff(buffstats, this));
            map.broadcastMessage(this, BuffPacket.cancelForeignBuff(getId(), buffstats), false);
            stats.recalcLocalStats(this);
        }
    }

    /*
     * 清除BUFF状态
     */
    public void dispelNew() {
        buffManager.dispel();
    }

    public void dispel() {
        if (!isHidden()) {
            LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
            for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
                allBuffs.add(buffs.getRight());
            }
            for (MapleBuffStatValueHolder mbsvh : allBuffs) {
                if (mbsvh.effect.isSkill() && mbsvh.schedule != null && !mbsvh.effect.isMorph() && !mbsvh.effect.isGmBuff() && !mbsvh.effect.is骑兽技能() && !mbsvh.effect.is能量获得() && !mbsvh.effect.is矛连击强化() && !mbsvh.effect.is尖兵支援() && !mbsvh.effect.isNotRemoved()) {
                    cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                }
            }
        }
    }

    /*
     * 清除指定技能ID的BUFF状态
     */
    public void dispelSkillNew(int skillid) {
        buffManager.dispelSkill(skillid);
    }

    public void dispelSkill(int skillId) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceid() == skillId) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    /*
     * 清除职业ID所有的BUFF状态
     */
    public void dispelBuffByJobId(int jobId) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isSkill() && mbsvh.effect.getSourceid() / 10000 == jobId) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    /*
     * 清除所有召唤兽
     */
    public void dispelSummons() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
            }
        }
    }

    /*
     * 清除指定技能的召唤兽
     */
    public void dispelSummons(int skillId) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSummonMovementType() != null && mbsvh.effect.getSourceid() == skillId) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    /*
     * 清除指定技能ID的BUFF状态
     */
    public void dispelBuff(int buffId) {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.getSourceid() == buffId) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    /*
     * 取消所有的BUFF状态信息
     */
    public void cancelAllBuffs_() {
        effects.clear();
        cannelAutoCelebrityCrit();
    }

    /*
     * 取消所有的BUFF状态信息
     */
    public void cancelAllBuffsNew() {
        buffManager.cancelAllBuffs();
    }

    public void cancelAllBuffs() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            cancelEffect(mbsvh.effect, false, mbsvh.startTime);
        }
    }

    /*
     * 取消所有变身的BUFF状态信息
     */
    public void cancelMorphsNew() {
        buffManager.cancelMorphs();
    }

    public void cancelMorphs() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            switch (mbsvh.effect.getSourceid()) {
                case 狂龙战士.终极变形_3转:
                case 狂龙战士.终极变形_4转:
                case 狂龙战士.终极变形_超级:
                    return; // Since we can't have more than 1, save up on loops
                default:
                    if (mbsvh.effect.isMorph()) {
                        cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                    }
            }
        }
    }

    /*
     * 获取变身BUFF状态的技能ID
     */
    public int getMorphState() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.isMorph()) {
                return mbsvh.effect.getSourceid();
            }
        }
        return -1;
    }

    /*
     * 准备检测重复使用的BUFF技能
     */
//    private void prepareRepeatEffect() {
//        lastRepeatEffectTime = System.currentTimeMillis();
//    }

    /*
     * 切换频道或者进入商城出来后 给角色BUFF 不需要发送封包
     */
    public void silentGiveBuffs(List<PlayerBuffValueHolder> buffs) {
        if (buffs == null) {
            return;
        }
        for (PlayerBuffValueHolder mbsvh : buffs) {
            mbsvh.effect.silentApplyBuff(this, mbsvh.startTime, mbsvh.localDuration, mbsvh.statup, mbsvh.fromChrId);
        }
    }

    public List<PlayerBuffValueHolder> getAllBuffs() {
        List<PlayerBuffValueHolder> ret = new ArrayList<>();
        Map<Pair<Integer, Byte>, Integer> alreadyDone = new HashMap<>(); //已经完成的
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : getAllEffects()) {
            Pair<Integer, Byte> key = new Pair<>(mbsvh.getRight().effect.getSourceid(), mbsvh.getRight().effect.getLevel());
            if (alreadyDone.containsKey(key)) {
                ret.get(alreadyDone.get(key)).statup.put(mbsvh.getLeft(), mbsvh.getRight().value);
            } else {
                alreadyDone.put(key, ret.size());
                Map<MapleBuffStat, Integer> list = new EnumMap<>(MapleBuffStat.class);
                list.put(mbsvh.getLeft(), mbsvh.getRight().value);
                ret.add(new PlayerBuffValueHolder(mbsvh.getRight().startTime, mbsvh.getRight().effect, list, mbsvh.getRight().localDuration, mbsvh.getRight().fromChrId));
            }
        }
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> mbsvh : getAllEffects()) {
            if (mbsvh.getRight().schedule != null) {
                mbsvh.getRight().schedule.cancel(false);
                mbsvh.getRight().schedule = null;
            }
        }
        return ret;
    }

    /*
     * 清理时空门
     */
    public void cancelMagicDoor() {
        LinkedList<MapleBuffStatValueHolder> allBuffs = new LinkedList<>();
        for (Pair<MapleBuffStat, MapleBuffStatValueHolder> buffs : getAllEffects()) {
            allBuffs.add(buffs.getRight());
        }
        for (MapleBuffStatValueHolder mbsvh : allBuffs) {
            if (mbsvh.effect.is时空门()) {
                cancelEffect(mbsvh.effect, false, mbsvh.startTime);
                break;
            }
        }
    }

    /*
     * 龙之力效果
     */
    public void doDragonBlood() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.龙之力);
        if (bloodEffect == null) {
            lastDragonBloodTime = 0;
            return;
        }
        prepareDragonBlood();
        if (stats.getHp() - bloodEffect.getStr() <= 1) { //以前是getX() 但x解析为0
            cancelBuffStats(MapleBuffStat.龙之力);
        } else {
            addHP(-bloodEffect.getStr()); //以前是getX() 但x解析为0
            client.announce(EffectPacket.showOwnBuffEffect(bloodEffect.getSourceid(), EffectOpcode.UserEffect_Quest.getValue(), getLevel(), bloodEffect.getLevel()));
            map.broadcastMessage(MapleCharacter.this, EffectPacket.showBuffeffect(this, bloodEffect.getSourceid(), EffectOpcode.UserEffect_Quest.getValue(), getLevel(), bloodEffect.getLevel()), false);
        }
    }

    public boolean canBlood(long now) {
        return lastDragonBloodTime > 0 && lastDragonBloodTime + 4000 < now;
    }

    private void prepareDragonBlood() {
        lastDragonBloodTime = System.currentTimeMillis();
    }

    public void doRecovery() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.恢复效果);
        if (bloodEffect == null) {
            bloodEffect = getStatForBuff(MapleBuffStat.金属机甲);
            if (bloodEffect == null) {
                lastRecoveryTime = 0;
            } else if (bloodEffect.getSourceid() == 机械师.金属机甲_战车) {
                prepareRecovery();
                if (stats.getMp() < bloodEffect.getU()) {
                    cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
                    cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
                } else {
                    addMP(-bloodEffect.getU());
                }
            }
        } else {
            prepareRecovery();
            if (stats.getHp() >= stats.getCurrentMaxHp()) {
                cancelEffectFromBuffStat(MapleBuffStat.恢复效果);
            } else {
                healHP(bloodEffect.getX());
            }
        }
    }

    public boolean canRecover(long now) {
        return lastRecoveryTime > 0 && lastRecoveryTime + 5000 < now;
    }

    private void prepareRecovery() {
        lastRecoveryTime = System.currentTimeMillis();
    }

    /*
     * 是否重复使用BUFF技能
     * 10秒1次
     */
    public boolean canRepeatEffect(long now) {
        return lastRepeatEffectTime > 0 && lastRepeatEffectTime + 10000 < now;
    }

    /**
     * 检测检测重复使用的BUFF技能
     */
    public void doRepeatEffect() {
        List<MapleBuffStat> checkStat = new ArrayList<>();
        checkStat.add(MapleBuffStat.寒冰灵气);
        checkStat.add(MapleBuffStat.战法灵气);
        boolean find = false;
        for (MapleBuffStat stat : checkStat) {
            MapleBuffStatValueHolder mbsvh = getBuffStatValueHolder(stat);
            if (mbsvh != null) {
                if (mbsvh.fromChrId == getId()) {
                    mbsvh.effect.applyBuffEffect(this, this, true, 180000, true);
                } else {
                    MapleCharacter from = map.getCharacterById(mbsvh.fromChrId);
                    if (from != null && from.getParty() != null && getParty() != null && from.getParty().getPartyId() == getParty().getPartyId()) {
                        mbsvh.effect.applyBuffEffect(from, this, false, 180000, true);
                    } else {
                        cancelEffect(mbsvh.effect, false, -1);
                    }
                }
                find = true;
            }
        }
        if (!find) {
            lastRepeatEffectTime = 0;
        }
    }

    /*
     * 恶魔复仇者 恶魔恢复
     */
    public void doRecoveryEM() {
        MapleStatEffect bloodEffect = getStatForBuff(MapleBuffStat.恶魔恢复);
        if (bloodEffect == null) {
            lastRecoveryTimeEM = 0;
            return;
        }
        prepareRecoveryEM();
        if (stats.getHp() < stats.getCurrentMaxHp()) {
            healHP(bloodEffect.getX() * (stats.getCurrentMaxHp() / 100));
        }
    }

    public boolean canRecoverEM(long now) {
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.恶魔恢复);
        if (effect == null) {
            lastRecoveryTimeEM = 0;
            return false;
        }
        int time = effect.getY();
        if (time < 4000) {
            time = 4000;
        }
        return lastRecoveryTimeEM > 0 && lastRecoveryTimeEM + time < now;
    }

    private void prepareRecoveryEM() {
        lastRecoveryTimeEM = System.currentTimeMillis();
    }

    /*
     * 尖兵能量恢复
     */
    public int getPowerCountByJob() {
        switch (getJob()) {
            case 3610:
                return 10;
            case 3611:
                return 15;
            case 3612:
                return 20;
        }
        return 5;
    }

    public void addPowerCount(int delta) {
        Skill skill = SkillFactory.getSkill(尖兵.急速支援);
        int skilllevel = getTotalSkillLevel(skill);
        if (!JobConstants.is尖兵(getJob()) || skill == null || skilllevel <= 0) {
            return;
        }
        if (setPowerCount(specialStats.getPowerCount() + delta)) {
            stats.recalcLocalStats(this);
            client.announce(BuffPacket.updatePowerCount(尖兵.急速支援, specialStats.getPowerCount()));
        }
    }

    public boolean setPowerCount(int count) {
        int oldPower = specialStats.getPowerCount();
        int tempPower = count;
        if (tempPower < 0) {
            tempPower = 0;
        }
        if (tempPower > getPowerCountByJob()) {
            tempPower = getPowerCountByJob();
        }
        specialStats.setPowerCount(tempPower);
        return specialStats.getPowerCount() != oldPower;
    }

    public void startPower() {
        client.announce(BuffPacket.startPower());
    }

    public void doRecoveryPower() {
        if (!JobConstants.is尖兵(getJob())) {
            return;
        }
        specialStats.prepareRecoveryPowerTime();
        addPowerCount(1);
    }

    public boolean canRecoverPower(long now) {
        Skill skill = SkillFactory.getSkill(尖兵.急速支援);
        int skilllevel = getTotalSkillLevel(skill);
        return JobConstants.is尖兵(getJob()) && skill != null && skilllevel > 0 && specialStats.getLastRecoveryPowerTime() > 0 && specialStats.getLastRecoveryPowerTime() + 4000 < now;
    }

    /*
     * 能量获得处理
     */
    public int getEnergyCount() {
        if (getBuffedValue(MapleBuffStat.能量获得) == null) {
            return 0;
        }
        return getBuffedValue(MapleBuffStat.能量获得);
    }

    public void setEnergyCount(int count) {
        Skill echskill_2 = SkillFactory.getSkill(冲锋队长.能量获得);
        if (getSkillLevel(echskill_2) <= 0) {
            return;
        }
        MapleStatEffect effect = echskill_2.getEffect(getTotalSkillLevel(echskill_2));
        if (count > 0 && !specialStats.isEnergyFull()) {
            //检测当前是否有能量获得的BUFF效果
            if (getBuffedValue(MapleBuffStat.能量获得) == null) {
                effect.applyEnergyBuff(this, count); // 注册第1次能量获得BUFF效果
                return;
            }
            //开始处理能量获得
            int energy = getBuffedIntValue(MapleBuffStat.能量获得); //获取当前能量的数量
            if (energy < 10000) { //处理未满能量的效果
                energy += count;
                if (energy >= 10000) {
                    energy = 10000;
                    specialStats.changeEnergyfull(true);
                }
                setBuffedValue(MapleBuffStat.能量获得, energy);
                //显示BUFF效果
                client.announce(EffectPacket.showOwnBuffEffect(effect.getSourceid(), EffectOpcode.UserEffect_SkillAffected.getValue(), getLevel(), effect.getLevel()));
                client.announce(BuffPacket.giveEnergyCharge(energy, effect.getSourceid(), specialStats.isEnergyFull(), false));
                //发送给其他玩家看到的效果
                map.broadcastMessage(this, EffectPacket.showBuffeffect(this, effect.getSourceid(), 3, getLevel(), effect.getLevel()), false);
                map.broadcastMessage(this, BuffPacket.showEnergyCharge(id, energy, effect.getSourceid(), specialStats.isEnergyFull(), false), false);
            }
        }
    }

    public void handleEnergyCharge(int targets) {
        handleEnergyCharge(targets, false);
    }

    public void handleEnergyCharge(int targets, boolean is能量激发) {
        Skill echskill_2 = SkillFactory.getSkill(冲锋队长.能量获得);
        Skill echskill_3 = SkillFactory.getSkill(冲锋队长.超级冲击);
        Skill echskill_4 = SkillFactory.getSkill(冲锋队长.终极冲击);
        MapleStatEffect effect;
        if (getSkillLevel(echskill_4) > 0) {
            effect = echskill_4.getEffect(getTotalSkillLevel(echskill_4));
        } else if (getSkillLevel(echskill_3) > 0) {
            effect = echskill_3.getEffect(getTotalSkillLevel(echskill_3));
        } else if (getSkillLevel(echskill_2) > 0) {
            effect = echskill_2.getEffect(getTotalSkillLevel(echskill_2));
        } else {
            return;
        }
        if (targets > 0 && !specialStats.isEnergyFull()) {
            //检测当前是否有能量获得的BUFF效果
            if (getBuffedValue(MapleBuffStat.能量获得) == null) {
                if (is能量激发) {
                    effect.applyEnergyBuff(this, 11000); // 注册第1次能量获得BUFF效果
                } else {
                    effect.applyEnergyBuff(this); // 注册第1次能量获得BUFF效果
                }
                return;
            }
            //开始处理能量获得
            int energy = getBuffedIntValue(MapleBuffStat.能量获得); //获取当前能量的数量
            if (energy < 10000) { //处理未满能量的效果
                energy += (effect.getX() * targets);
                if (energy >= 10000) {
                    energy = 10000;
                    specialStats.changeEnergyfull(true);
                }
                setBuffedValue(MapleBuffStat.能量获得, energy);
                //显示BUFF效果
                client.announce(EffectPacket.showOwnBuffEffect(effect.getSourceid(), EffectOpcode.UserEffect_SkillAffected.getValue(), getLevel(), effect.getLevel()));
                client.announce(BuffPacket.giveEnergyCharge(energy, effect.getSourceid(), specialStats.isEnergyFull(), false));
                //发送给其他玩家看到的效果
                map.broadcastMessage(this, EffectPacket.showBuffeffect(this, effect.getSourceid(), 3, getLevel(), effect.getLevel()), false);
                map.broadcastMessage(this, BuffPacket.showEnergyCharge(id, energy, effect.getSourceid(), specialStats.isEnergyFull(), false), false);
            }
        }
    }

    /*
     * 处理拳手能量减少
     */
    public void handleEnergyConsume(int mobCount, int skillId) {
        //检测当前是否有能量获得BUFF
        MapleStatEffect echeffect = getStatForBuff(MapleBuffStat.能量获得);
        if (skillId == 0 || !specialStats.isEnergyFull() || echeffect == null) {
            return;
        }
        Skill skill = SkillFactory.getSkill(skillId);
        int skillLevel = getTotalSkillLevel(SkillConstants.getLinkedAttackSkill(skillId));
        MapleStatEffect effect = skill.getEffect(skillLevel);
        if (effect == null || effect.getForceCon() <= 0) {
            return;
        }
        //获取当前的能量
        int energy = getBuffedIntValue(MapleBuffStat.能量获得);
        energy -= (effect.getForceCon() * mobCount);
        if (energy <= 0) {
            energy = 0;
            specialStats.changeEnergyfull(false);
        }
        setBuffedValue(MapleBuffStat.能量获得, energy);
        client.announce(BuffPacket.giveEnergyCharge(energy, echeffect.getSourceid(), specialStats.isEnergyFull(), true));
        map.broadcastMessage(this, BuffPacket.showEnergyCharge(id, energy, echeffect.getSourceid(), specialStats.isEnergyFull(), true), false);
    }

    /*
     * 检测林之灵是否可以使用组队被动BUFF效果
     */
    public boolean canPartyPassiveBuff(long now) {
        //是否林之灵 或者  是否开启猫咪模式
        if (getJob() != 11212 || !hasBuffSkill(林之灵.猫咪模式)) {
            return false;
        }
        if (checkPartyPassiveTime <= 0) {
            checkPartyPassiveTime = System.currentTimeMillis();
        }
        return checkPartyPassiveTime > 0 && checkPartyPassiveTime + 8000 < now;
    }

    /*
     * 处理林之灵组队被动BUFF效果
     */
    public void doPartyPassiveBuff() {
        if (getJob() != 11212) {
            return;
        }
        int[] skilllist = {林之灵.阿尔之好伙伴, 林之灵.阿尔之窃取, 林之灵.阿尔之爪, 林之灵.阿尔之魅力_强化, 林之灵.阿尔之弱点把握, 林之灵.阿尔之饱腹感};
        Skill skill;
        MapleStatEffect effect;
        for (int i : skilllist) {
            skill = SkillFactory.getSkill(i);
            if (skill != null && getTotalSkillLevel(skill) > 0) {
                effect = skill.getEffect(getTotalSkillLevel(skill));
                if (effect != null) {
                    effect.applyTo(this);
                }
            }
        }
        checkPartyPassiveTime = System.currentTimeMillis();
    }

    /*
     * 检测是否可以恢复暴击
     */
    public boolean canRecoverCrit(long now) {
        boolean is侠盗 = getJob() == 420 || getJob() == 421 || getJob() == 422;
        if (!is侠盗) {
            lastCritStorageTime = 0;
            return false;
        }
        if (lastCritStorageTime <= 0) {
            lastCritStorageTime = System.currentTimeMillis();
        }
        return lastCritStorageTime > 0 && lastCritStorageTime + 5000 < now;
    }

    /*
     * 刀飞技能
     * 暴击蓄能处理
     */
    public void handleCritStorage() {
        if (getJob() != 420 && getJob() != 421 && getJob() != 422) {
            lastCritStorageTime = 0;
            return;
        }
        //初始化下次检测的时间
        lastCritStorageTime = System.currentTimeMillis();
        //检测角色的技能
        Skill critskill_2 = SkillFactory.getSkill(侠盗.暴击蓄能);
        Skill critskill_4 = SkillFactory.getSkill(侠盗.名流爆击);
        MapleStatEffect skilleffect;
        if (getSkillLevel(critskill_4) > 0) {
            skilleffect = critskill_4.getEffect(getTotalSkillLevel(critskill_4));
        } else if (getSkillLevel(critskill_2) > 0) {
            skilleffect = critskill_2.getEffect(getTotalSkillLevel(critskill_2));
        } else {
            return;
        }
        MapleBuffStat buffStat = MapleBuffStat.暴击蓄能;
        //检测 是否有BUFF效果  如果没有就注册第1次暴击蓄能的BUFF效果
        MapleStatEffect effect = getStatForBuff(buffStat);
        if (effect == null || getBuffedValue(buffStat) == null) {
            skilleffect.applyTo(this);
            return;
        }
        //检测当前的获得暴击点数
        int crit = getBuffedIntValue(buffStat);
        int newcrit = crit;
        //检测当前的暴击点数是否达到100点
        if (newcrit < 100) {
            newcrit += skilleffect.getX();
            if (newcrit > 100) {
                newcrit = 100;
            }
        }
        //检测是否需要发送封包更新BUFF信息
        if (crit != newcrit) {
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(buffStat, newcrit);
            setBuffedValue(buffStat, newcrit);
            int duration = effect.getDuration();
            duration += (int) ((getBuffedStartTime(buffStat) - System.currentTimeMillis()));
            client.announce(BuffPacket.giveBuff(effect.getSourceid(), duration, stat, effect, this));
        }
    }

    /*
     * 获取当前斗气的点数
     */
    public int getOrbCount() {
        if (getBuffedValue(MapleBuffStat.斗气集中) == null) {
            return 0;
        }
        return getBuffedValue(MapleBuffStat.斗气集中);
    }

    /*
     * 斗气集中效果处理
     */
    public void handleOrbgain(boolean passive) {
        //检测当前是否有斗气集中的BUFF效果
        if (getBuffedValue(MapleBuffStat.斗气集中) == null) {
            return;
        }
        //获取当前的斗气点数
        int orbcount = getBuffedValue(MapleBuffStat.斗气集中);
        Skill combo_2 = SkillFactory.getSkill(英雄.斗气集中);
        Skill combo_3 = SkillFactory.getSkill(英雄.斗气协合);
        Skill combo_4 = SkillFactory.getSkill(英雄.进阶斗气);
        if (passive && getTotalSkillLevel(combo_3) <= 0) {
            return;
        }
        MapleStatEffect effect;
        if (getTotalSkillLevel(combo_3) > 0) {
            effect = combo_3.getEffect(getTotalSkillLevel(combo_3));
        } else if (getTotalSkillLevel(combo_2) > 0) {
            effect = combo_2.getEffect(getTotalSkillLevel(combo_2));
        } else {
            return;
        }
        int neworbcount = orbcount;
        if (effect != null && orbcount < effect.getX() + 1) {
            if (!passive && effect.makeChanceResult()) {
                neworbcount++;
            }
            if (passive && Randomizer.nextInt(100) < effect.getSubProp()) {
                neworbcount++;
            }
        }
        //进阶斗气有概率多增加1点斗气
        if (getTotalSkillLevel(combo_4) > 0 && !passive) {
            effect = combo_4.getEffect(getTotalSkillLevel(combo_4));
            if (effect != null && effect.makeChanceResult() && neworbcount < effect.getX() + 1) {
                neworbcount++;
            }
        }
        if (effect != null) {
            if (neworbcount != orbcount) {
                Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.斗气集中, neworbcount);
                setBuffedValue(MapleBuffStat.斗气集中, neworbcount);
                int duration = effect.getDuration();
                duration += (int) ((getBuffedStartTime(MapleBuffStat.斗气集中) - System.currentTimeMillis()));
                client.announce(BuffPacket.giveBuff(combo_2.getId(), duration, stat, effect, this));
                map.broadcastMessage(this, BuffPacket.giveForeignBuff(this, stat, effect), false);
            }
        }
    }

    /*
     * 消耗斗气点数
     */
    public void handleOrbconsume(int howmany) {
        Skill combos = SkillFactory.getSkill(英雄.斗气集中);
        if (getSkillLevel(combos) <= 0) {
            return;
        }
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.斗气集中);
        if (effect == null) {
            return;
        }
        Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.斗气集中, Math.max(1, getBuffedValue(MapleBuffStat.斗气集中) - howmany));
        setBuffedValue(MapleBuffStat.斗气集中, Math.max(1, getBuffedValue(MapleBuffStat.斗气集中) - howmany));
        int duration = effect.getDuration();
        duration += (int) ((getBuffedStartTime(MapleBuffStat.斗气集中) - System.currentTimeMillis()));
        client.announce(BuffPacket.giveBuff(combos.getId(), duration, stat, effect, this));
        map.broadcastMessage(this, BuffPacket.giveForeignBuff(this, stat, effect), false);
    }

    /*
     * 处理 三彩箭矢 的效果
     */
    public void handleArrowsCharge(int oid) {
        Skill skill = SkillFactory.getSkill(神射手.三彩箭矢);
        if (getSkillLevel(skill) <= 0) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(getSkillLevel(skill));
        if (effect == null) {
            return;
        }
        int oldMode = specialStats.getArrowsMode();
        int arrows = getBuffedIntValue(MapleBuffStat.三彩箭矢);
        //如果没有BUFF效果 就注册这个BUFF的效果
        if (getBuffedValue(MapleBuffStat.三彩箭矢) == null || oldMode == -1 || arrows == 0) {
            effect.applyArrowsBuff(this, true);
            return;
        }
        int newMode = oldMode;
        if (oldMode == 0 && arrows / 10000 > 0) { //每次减少 10000
            arrows -= 10000;
            if (arrows / 10000 == 0 && arrows % 10000 / 100 > 0) {
                newMode = 1;
            } else if (arrows / 10000 == 0 && arrows % 100 > 0) {
                newMode = 2;
            }
        } else if (oldMode == 1 && arrows % 10000 / 100 > 0) { //每次减少 100
            arrows -= 100;
            if (arrows % 10000 / 100 == 0 && arrows % 100 > 0) {
                newMode = 2;
            } else if (arrows % 10000 / 100 == 0 && arrows / 10000 > 0) {
                newMode = 0;
            }
        } else if (oldMode == 2 && arrows % 100 > 0) { //每次减少 1
            arrows -= 1;
            handle三彩箭矢(oid);
            if (arrows % 100 == 0 && arrows / 10000 > 0) {
                newMode = 0;
            } else if (arrows % 100 == 0 && arrows % 10000 / 100 > 0) {
                newMode = 1;
            }
        }
        if (arrows <= 0) {
            effect.applyArrowsBuff(this, true);
        } else if (getBuffedValue(MapleBuffStat.进阶箭筒) == null) {
            specialStats.setArrowsMode(newMode);
            Map<MapleBuffStat, Integer> localstatups = Collections.singletonMap(MapleBuffStat.三彩箭矢, arrows);
            setBuffedValue(MapleBuffStat.三彩箭矢, arrows);
            client.announce(BuffPacket.giveBuff(effect.getSourceid(), 0, localstatups, effect, this));
            if (newMode != oldMode) {
                int newArrows = 0x0A;
                if (newMode == 0) {
                    newArrows = arrows / 10000;
                } else if (newMode == 1) {
                    newArrows = arrows % 10000 / 100;
                } else if (newMode == 2) {
                    newArrows = arrows % 100;
                }
                client.announce(EffectPacket.showArrowsEffect(effect.getSourceid(), specialStats.getArrowsMode(), newArrows)); //发送封包
            }
        }
    }

    public int getDarkLight() {
        return specialStats.getDarkLight();
    }

    /*
     * 更新夜光 光暗点数
     */
    public void handleLuminous(int skillId) {
        if (skillId == 夜光.记录) {
            removeCooldown(夜光.死亡之刃, true);
            removeCooldown(夜光.绝对死亡, true);
            SkillFactory.getSkill(夜光.平衡_光明).getEffect(1).applyTo(this);
            return;
        }
        int skillMode = SkillConstants.getLuminousSkillMode(skillId);

        if (skillMode < 0 || skillMode == 夜光.平衡_光明 || getSkillLevel(skillMode) <= 0) {
            return;
        }

        // 平衡状态不改变点数
        Integer buffvalue = getBuffedValue(MapleBuffStat.光暗转换);
        if (buffvalue != null && buffvalue == 2) {
            return;
        }

        int gauge;
        byte gaugetype = (byte) (skillMode == 夜光.太阳火焰 ? 1 : 2);
        int balanceid = -1;

        if (!hasBuffSkill(夜光.太阳火焰) && !hasBuffSkill(夜光.月蚀)) {
            int newskillid = gaugetype == 1 ? 夜光.月蚀 : 夜光.太阳火焰;
            SkillFactory.getSkill(newskillid).getEffect(1).applyTo(this);
            gauge = skillMode == 夜光.太阳火焰 ? 9999 : 1;
        } else {
            boolean isLightSkill = gaugetype == 1;
            boolean isLightMode = getBuffSource(MapleBuffStat.光暗转换) == 夜光.太阳火焰;
            boolean isApply = false;
            gauge = specialStats.getDarkLight();
            MapleStatEffect effect = SkillFactory.getSkill(skillId).getEffect(getTotalSkillLevel(skillId));
            int gauge_val = (int) (effect.getGauge() * (((double) getStat().getGauge_x() / 100) + 1.0));
            if (gaugetype == 1 && isLightMode) {            //月蚀
                gauge = Math.min(9999, gauge + gauge_val);
                isApply = true;
            } else if (gaugetype == 2 && !isLightMode) {    //太阳
                gauge = Math.max(1, gauge - gauge_val);
                isApply = true;
            }

            if (isLightMode) {
                addHP(stats.getMaxHp() / 100); //恢复1%的hp
            }

            if (gauge == 9999) {
                balanceid = 夜光.平衡_光明;
            } else if (gauge == 1) {
                balanceid = 夜光.平衡_黑暗;
            }

            if (isApply && balanceid > 0) {
                cancelBuffStats(MapleBuffStat.光暗转换);
                removeCooldown(夜光.死亡之刃, true);
                removeCooldown(夜光.绝对死亡, true);
                SkillFactory.getSkill(balanceid).getEffect(1).applyTo(this);
            }
        }
        //更新光暗点数
        specialStats.setDarkLight(gauge);
        client.announce(BuffPacket.updateLuminousGauge(gauge, gaugetype));
    }

    /*
     * 夜光黑暗祝福处理
     */
    public void handleBlackBless() {
        if (lastBlessOfDarknessTime == 0) {
            lastBlessOfDarknessTime = System.currentTimeMillis();
        }
        Skill skill = SkillFactory.getSkill(夜光.黑暗祝福); //27100003 - 黑暗祝福 - 战斗中，如果一定时间内不受伤害，就会产生可以保护自己的暗黑球体。球体累积之后可以增加魔力，有球体的情况下受到伤害，则减少伤害值，同时减少1个球体。
        int skilllevel = getTotalSkillLevel(skill);
        if (skilllevel <= 0) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(skilllevel);
        if (getStatForBuff(MapleBuffStat.黑暗祝福) == null) {
            effect.applyTo(this);
            return;
        }
        if (lastBlessOfDarknessTime + effect.getDuration() < System.currentTimeMillis()) {
            int count = getBuffedValue(MapleBuffStat.黑暗祝福);
            if (count < 3) {
                count++;
            }
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.黑暗祝福, count);
            setBuffedValue(MapleBuffStat.黑暗祝福, count);
            client.announce(EffectPacket.showBlessOfDarkness(skill.getId()));
            client.announce(BuffPacket.giveBuff(skill.getId(), 0, stat, effect, this));
            lastBlessOfDarknessTime = System.currentTimeMillis();
        }
    }

    public void handleBlackBlessLost(int howmany) {
        Skill skill = SkillFactory.getSkill(夜光.黑暗祝福);
        if (getSkillLevel(skill) <= 0) {
            cancelEffectFromBuffStat(MapleBuffStat.黑暗祝福);
            return;
        }
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.黑暗祝福);
        if (effect == null) {
            return;
        }
        lastBlessOfDarknessTime = System.currentTimeMillis();
        int count = getBuffedValue(MapleBuffStat.黑暗祝福);
        count = count - howmany;
        if (count <= 0) {
            cancelEffectFromBuffStat(MapleBuffStat.黑暗祝福);
        } else {
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.黑暗祝福, count);
            setBuffedValue(MapleBuffStat.黑暗祝福, count);
            client.announce(EffectPacket.showBlessOfDarkness(skill.getId()));
            client.announce(BuffPacket.giveBuff(skill.getId(), 0, stat, effect, this));
        }
    }

    /*
     * 夜光黑暗高潮处理
     */
    public void handleDarkCrescendo() {
        MapleStatEffect dkeffect = getStatForBuff(MapleBuffStat.黑暗高潮);
        if (dkeffect != null && dkeffect.getSourceid() == 夜光.黑暗高潮) { //27121005 - 黑暗高潮 - 在一定时间内，攻击技能每命中一次敌人，都会有一定概率提升攻击力。
            int orbcount = getBuffedValue(MapleBuffStat.黑暗高潮);
            Skill skill = SkillFactory.getSkill(夜光.黑暗高潮);
            if (getSkillLevel(skill) > 0) {
                MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
                if (orbcount < effect.getX() && effect.makeChanceResult()) {
                    int neworbcount = orbcount + 1;
                    Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.黑暗高潮, neworbcount);
                    setBuffedValue(MapleBuffStat.黑暗高潮, neworbcount);
                    int duration = effect.getDuration();
                    duration += (int) ((getBuffedStartTime(MapleBuffStat.黑暗高潮) - System.currentTimeMillis()));
                    client.announce(BuffPacket.giveBuff(skill.getId(), duration, stat, effect, this));
                }
            }
        }
    }

    /*
     * 狂龙战士变形值处理
     */
    public boolean canMorphLost(long now) {
        return getJob() >= 6100 && getJob() <= 6112 && specialStats.getLastMorphLostTime() > 0 && specialStats.getMorphCount() > 0 && specialStats.getLastMorphLostTime() + (20 * 1000) < now;
    }

    public void morphLostTask() {
        if (getJob() >= 6100 && getJob() <= 6112) {
            if (specialStats.getMorphCount() > 0) {
                specialStats.gainMorphCount(-1);
                client.announce(BuffPacket.give狂龙变形值(specialStats.getMorphCount()));
            }
            specialStats.prepareMorphLostTime();
        }
    }

    public int getMorphCount() {
        return specialStats.getMorphCount();
    }

    public void setMorphCount(int amount) {
        specialStats.setMorphCount(amount);
        if (specialStats.getMorphCount() <= 0) {
            specialStats.setMorphCount(0);
            client.announce(BuffPacket.cancelBuff(MapleBuffStat.变形值));
        }
    }

    public void handleMorphCharge(int targets) {
        Skill mchskill = SkillFactory.getSkill(狂龙战士.变身);
        int skilllevel = getTotalSkillLevel(mchskill);
        if (skilllevel > 0) {
            MapleStatEffect mcheff = mchskill.getEffect(skilllevel);
            if (targets > 0 && mcheff != null) {
                specialStats.prepareMorphLostTime();
                int maxCount = getJob() == 6100 ? mcheff.getS() : getJob() == 6110 ? mcheff.getU() : mcheff.getV();
                if (specialStats.getMorphCount() < maxCount) {
                    specialStats.gainMorphCount(targets);
                    if (specialStats.getMorphCount() >= maxCount) {
                        specialStats.setMorphCount(maxCount);
                    }
                    client.announce(BuffPacket.give狂龙变形值(specialStats.getMorphCount()));
                    if (isAdmin()) {
                        map.broadcastMessage(this, BuffPacket.show狂龙变形值(getId(), specialStats.getMorphCount()), false);
                    }
                }
            }
        }
    }

    public void handleDeathPact(int moboid, boolean attackBoss) {
        if (getBuffStatValueHolder(MapleBuffStat.死亡契约) == null) {
            return;
        }
        int skills_[] = {唤灵斗师.死亡契约3, 唤灵斗师.死亡契约2, 唤灵斗师.死亡契约, 唤灵斗师.死亡};
        MapleStatEffect effect = null;
        for (int skillid_ : skills_) {
            if (this.getSkillLevel(skillid_) > 0) {
                effect = SkillFactory.getSkill(skillid_).getEffect(1);
                break;
            }
        }
        MapleMonster mob = getMap().getMonsterByOid(moboid);
        if (effect == null || mob == null) {
            return;
        }
        MapleSummon summon = getSummonBySkill(effect.getSourceid());
        if (summon == null) {
            return;
        }
        int maxCount = effect.getX() - (hasBuffSkill(唤灵斗师.战斗大师) ? 3 : 0);
        int currentCount = getSpecialStat().getDeathPactCount();
        if (currentCount < maxCount) {
            getSpecialStat().setDeathPactCount(Math.min(currentCount + (attackBoss && mob.isBoss() ? 2 : 1), maxCount));
        } else {
            if (summon.checkLastAttackTime()) {
                getSpecialStat().setDeathPactCount(0);
                getMap().broadcastMessage(SummonPacket.summonSkillLink(id, summon.getObjectId()), getTruePosition());
            }
        }
        Map<MapleBuffStat, Integer> localstatups = Collections.singletonMap(MapleBuffStat.死亡契约, getSpecialStat().getDeathPactCount());
        setBuffedValue(MapleBuffStat.死亡契约, getSpecialStat().getDeathPactCount());
        client.announce(BuffPacket.giveBuff(effect.getSourceid(), 0, localstatups, effect, this));
    }

    //超能力者 心灵传动
    public void handlerKSTelekinesis(int oid) {
        Skill skill = SkillFactory.getSkill(超能力者.心灵传动);
        int skilllevel = getSkillLevel(超能力者.心灵传动);
        if (skilllevel > 0) {
            MapleStatEffect effect = skill.getEffect(skilllevel);
            if (effect != null && Randomizer.nextInt(100) < effect.getProp()) {
                specialStats.gainCardStack();
                getMap().broadcastMessage(MaplePacketCreator.心灵传动攻击效果(getId(), oid, 超能力者.心灵传动, specialStats.getCardStack()), getTruePosition());
            }
        }
    }

    public void silentEnforceMaxHpMp() {
        stats.setMp(stats.getMp(), this);
        stats.setHp(stats.getHp(), true, this);
    }

    public void enforceMaxHpMp() {
        List<Pair<MapleStat, Long>> statup = new ArrayList<>(2);
        if (stats.getMp() > stats.getCurrentMaxMp(this.getJob())) {
            stats.setMp(stats.getMp(), this);
            statup.add(new Pair<>(MapleStat.MP, (long) stats.getMp()));
        }
        if (stats.getHp() > stats.getCurrentMaxHp()) {
            stats.setHp(stats.getHp(), this);
            statup.add(new Pair<>(MapleStat.HP, (long) stats.getHp()));
        }
        if (statup.size() > 0) {
            client.announce(MaplePacketCreator.updatePlayerStats(statup, this));
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(int PmapId) {
        this.mapid = PmapId;
    }

    public void setMap(MapleMap newmap) {
        this.map = newmap;
    }

    public MonsterBook getMonsterBook() {
        return monsterbook;
    }

    public int getMapId() {
        if (map != null) {
            return map.getId();
        }
        return mapid;
    }

    public byte getInitialSpawnpoint() {
        return initialSpawnPoint;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBlessOfFairyOrigin() {
        return this.BlessOfFairy_Origin;
    }

    public String getBlessOfEmpressOrigin() {
        return this.BlessOfEmpress_Origin;
    }

    public short getLevel() {
        return level;
    }

    public void setLevel(short newLevel) {
        if (newLevel <= 0) {
            newLevel = 1;
        }
        this.level = newLevel;
    }

    public int getFame() {
        return fame;
    }

    public void setFame(int fame) {
        this.fame = fame;
    }

    public int getFallCounter() {
        return fallcounter;
    }

    public void setFallCounter(int fallcounter) {
        this.fallcounter = fallcounter;
    }

    public int getCriticalGrowth() {
        return criticalgrowth;
    }

    public void setCriticalGrowth(int critical) {
        this.criticalgrowth = critical;
    }

    public MapleClient getClient() {
        return client;
    }

    public void setClient(MapleClient client) {
        this.client = client;
    }

    public long getExp() {
        return exp.get();
    }

    public void setExp(long exp) {
        this.exp.set(exp);
    }

    public short getRemainingAp() {
        return remainingAp;
    }

    public void setRemainingAp(short remainingAp) {
        this.remainingAp = remainingAp;
    }

    public int getRemainingSp() {
        return remainingSp[JobConstants.getSkillBookByJob(job)];
    }

    public void setRemainingSp(int remainingSp) {
        this.remainingSp[JobConstants.getSkillBookByJob(job)] = remainingSp; //default
    }

    public int getRemainingSp(int skillbook) {
        return remainingSp[skillbook];
    }

    public int[] getRemainingSps() {
        return remainingSp;
    }

    public int getRemainingSpSize() {
        int ret = 0;
        for (int aRemainingSp : remainingSp) {
            if (aRemainingSp > 0) {
                ret++;
            }
        }
        return ret;
    }

    public short getHpApUsed() {
        return hpApUsed;
    }

    public void setHpApUsed(short hpApUsed) {
        this.hpApUsed = hpApUsed;
    }

    /*
     * 是否隐身状态
     */
    public boolean isHidden() {
        return getBuffSource(MapleBuffStat.隐身术) / 1000000 == 9;
    }

    public byte getSkinColor() {
        return skinColor;
    }

    public void setSkinColor(byte skinColor) {
        this.skinColor = skinColor;
    }

    public short getJob() {
        return job;
    }

    public void setJob(int jobId) {
        this.job = (short) jobId;
    }

    public void writeJobData(MaplePacketLittleEndianWriter mplew) {
        boolean sub = getJob() == 400 && getSubcategory() == 1;
        mplew.writeShort(getJob());
        mplew.writeShort(sub ? getSubcategory() : 0);
    }

    public byte getAccountGender() {
        return accountGender;
    }

    /*
         * 角色性别
         */
    public byte getGender() {
        return gender;
    }

    public void setGender(byte gender) {
        this.gender = gender;
    }

    public int getReviveCount() {
        return reviveCount;
    }

    public void setReviveCount(int reviveCount) {
        this.reviveCount = reviveCount;
    }

    public void reduceReviveCount() {
        --this.reviveCount;
    }

    public void restReviveCount() {
        this.reviveCount = -1;
    }

    public void eventRevive() {
        heal();
        this.reduceReviveCount();
    }

    /*
         * 第2角色的性别
         */
    public byte getSecondGender() {
        //如果是神之子 那么第2性别为1
        if (JobConstants.is神之子(job)) {
            return 1;
        }
        return gender;
    }

    public byte getZeroLook() {
        if (!JobConstants.is神之子(job)) {
            return -1;
        }
        if (getKeyValue("Zero_Look") == null) {
            setKeyValue("Zero_Look", "0");
        }
        return Byte.parseByte(getKeyValue("Zero_Look"));
    }

    public void changeZeroLook() {
        if (!JobConstants.is神之子(job)) {
            return;
        }
        setKeyValue("Zero_Look", isZeroSecondLook() ? "0" : "1");
        map.broadcastMessage(this, MaplePacketCreator.updateZeroLook(this), false);
        stats.recalcLocalStats(this);
    }

    public boolean isZeroSecondLook() {
        return getZeroLook() == 1;
    }

    /*
     * 角色发型
     */
    public int getHair() {
        return hair;
    }

    public void setHair(int hair) {
        this.hair = hair;
    }

    /*
     * 第2角色的发型
     * 37623 神之子
     */
    public int getSecondHair() {
        if (JobConstants.is神之子(job)) {
            if (getKeyValue("Second_Hair") == null) {
                setKeyValue("Second_Hair", "37623");
            }
            return Integer.parseInt(getKeyValue("Second_Hair"));
        } else if (JobConstants.is爆莉萌天使(job)) {
            if (getKeyValue("Second_Hair") == null) {
                setKeyValue("Second_Hair", "37141");
            }
            return Integer.parseInt(getKeyValue("Second_Hair"));
        }
        return hair;
    }

    public void setSecondHair(int hair) {
        setKeyValue("Second_Hair", String.valueOf(hair));
    }

    public byte getSecondSkinColor() {
        if (JobConstants.is爆莉萌天使(job)) {
            if (this.getKeyValue("Second_Skin") == null) {
                this.setKeyValue("Second_Skin", "0");
            }
            return Byte.parseByte(this.getKeyValue("Second_Skin"));
        }
        return skinColor;
    }

    public void setSecondSkinColor(final byte b) {
        setKeyValue("Second_Skin", String.valueOf(b));
    }

    /*
     * 角色脸型
     */
    public int getFace() {
        return face;
    }

    public void setFace(int face) {
        this.face = face;
    }

    /*
     * 第2角色的脸型
     * 21290 神之子
     */
    public int getSecondFace() {
        if (JobConstants.is神之子(job)) {
            if (getKeyValue("Second_Face") == null) {
                setKeyValue("Second_Face", "21290");
            }
            return Integer.parseInt(getKeyValue("Second_Face"));
        } else if (JobConstants.is爆莉萌天使(job)) {
            if (getKeyValue("Second_Face") == null) {
                setKeyValue("Second_Face", "21173");
            }
            return Integer.parseInt(getKeyValue("Second_Face"));
        }
        return face;
    }

    public void setSecondFace(int face) {
        setKeyValue("Second_Face", String.valueOf(face));
    }

    public boolean changeFace(int color) {
        int f = 0;
        if (face % 1000 < 100) {
            f = face + color;
        } else if ((face % 1000 >= 100) && (face % 1000 < 200)) {
            f = face - 100 + color;
        } else if ((face % 1000 >= 200) && (face % 1000 < 300)) {
            f = face - 200 + color;
        } else if ((face % 1000 >= 300) && (face % 1000 < 400)) {
            f = face - 300 + color;
        } else if ((face % 1000 >= 400) && (face % 1000 < 500)) {
            f = face - 400 + color;
        } else if ((face % 1000 >= 500) && (face % 1000 < 600)) {
            f = face - 500 + color;
        } else if ((face % 1000 >= 600) && (face % 1000 < 700)) {
            f = face - 600 + color;
        } else if ((face % 1000 >= 700) && (face % 1000 < 800)) {
            f = face - 700 + color;
        }
        if (!MapleItemInformationProvider.getInstance().loadHairFace(f)) {
            return false;
        }
        face = f;
        updateSingleStat(MapleStat.脸型, face);
        equipChanged();
        return true;
    }

    public byte getHairBaseColor() {
        return hairBaseColor;
    }

    public void setHairBaseColor(byte hairBaseColor) {
        this.hairBaseColor = hairBaseColor;
    }

    public byte getHairProbColor() {
        return hairProbColor;
    }

    public void setHairProbColor(byte hairProbColor) {
        this.hairProbColor = hairProbColor;
    }

    public byte getHairMixedColor() {
        return hairMixedColor;
    }

    public void setHairMixedColor(byte hairMixedColor) {
        this.hairMixedColor = hairMixedColor;
    }

    public void handleMixHairColor(byte mixBaseHairColor, byte mixAddHairColor, byte mixHairBaseProb) {
        this.hairBaseColor = mixBaseHairColor;
        this.hairMixedColor = mixAddHairColor;
        this.hairProbColor = mixHairBaseProb;
        int hair = this.hair / 10 * 10 + mixBaseHairColor;
        this.updateHair(hair);
    }

    public Point getOldPosition() {
        return old;
    }

    public void setOldPosition(Point x) {
        this.old = x;
    }

    /*
     * 前1次使用的攻击技能ID
     */
    public int getLastAttackSkillId() {
        return lastAttackSkillId;
    }

    public void setLastAttackSkillId(int skillId) {
        this.lastAttackSkillId = skillId;
    }

    public void setRemainingSp(int remainingSp, int skillbook) {
        this.remainingSp[skillbook] = remainingSp;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invinc) {
        invincible = invinc;
    }

    /*
     * 外挂检测系统
     */
    public CheatTracker getCheatTracker() {
        return anticheat;
    }

    /*
     * 测谎仪系统
     */
    public MapleLieDetector getAntiMacro() {
        return antiMacro;
    }

    /*
     * 好友列表
     */
    public BuddyList getBuddylist() {
        return buddylist;
    }

    /*
     * 加减角色人气
     */
    public void addFame(int famechange) {
        this.fame += famechange;
        getTrait(MapleTraitType.charm).addLocalExp(famechange);
        if (this.fame >= 50) {
            finishAchievement(7);
        }
        insertRanking("人气排行", fame);
    }

    /*
     * 更新角色人气
     */
    public void updateFame() {
        updateSingleStat(MapleStat.人气, this.fame);
    }

    /*
     * 加减角色人气 更新玩家人气 是否在对话框提示
     */
    public void gainFame(int famechange, boolean show) {
        this.fame += famechange;
        updateSingleStat(MapleStat.人气, this.fame);
        if (show && famechange != 0) {
            client.announce(MaplePacketCreator.getShowFameGain(famechange));
        }
    }

    public void updateHair(int hair) {
        setHair(hair);
        updateSingleStat(MapleStat.发型, hair);
        equipChanged();
    }

    public void updateFace(int face) {
        setFace(face);
        updateSingleStat(MapleStat.脸型, face);
        equipChanged();
    }

    /*
     * 切换地图
     */
    public void changeMapBanish(int mapid, String portal, String msg) {
        dropMessage(5, msg);
        MapleMap maps = client.getChannelServer().getMapFactory().getMap(mapid);
        changeMap(maps, maps.getPortal(portal));
    }

    public void changeMap(MapleMap to, Point pos) {
        changeMapInternal(to, pos, MaplePacketCreator.getWarpToMap(this, to, 0x80), null);
    }

    public void changeMap(MapleMap to) {
        changeMapInternal(to, to.getPortal(0).getPosition(), MaplePacketCreator.getWarpToMap(this, to, 0), to.getPortal(0));
    }

    public void changeMap(MapleMap to, MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(this, to, pto.getId()), null);
    }

    public void changeMapPortal(MapleMap to, MaplePortal pto) {
        changeMapInternal(to, pto.getPosition(), MaplePacketCreator.getWarpToMap(this, to, pto.getId()), pto);
    }

    private void changeMapInternal(MapleMap to, Point pos, byte[] warpPacket, MaplePortal pto) {
        if (to == null) {
            dropMessage(5, "changeMapInternal to Null");
            return;
        }
        if (getAntiMacro().inProgress()) {
            dropMessage(5, "被使用测谎仪时无法操作。");
            return;
        }
        int nowmapid = map.getId();
        if (eventInstance != null) {
            eventInstance.changedMap(this, to.getId());
        }
        boolean pyramid = pyramidSubway != null;
        if (map.getId() == nowmapid) {
            client.announce(warpPacket);
            boolean shouldChange = client.getChannelServer().getPlayerStorage().getCharacterById(getId()) != null;
            boolean shouldState = map.getId() == to.getId();
            if (shouldChange && shouldState) {
                to.setCheckStates(false);
            }
            map.removePlayer(this);
            if (shouldChange) {
                map = to;
                setStance(0);
                setPosition(new Point(pos.x, pos.y - 50));
                to.addPlayer(this);
                stats.relocHeal(this);
                if (shouldState) {
                    to.setCheckStates(true);
                }
//                if (this.fj && this.haveItem(5133000, 1, false, true)) {
//                    this.send(i.b.l.x((byte)(this.getInventory(a.b.m.AV).countById(5133000) - 1)));
//                    this.silentGiveBuffs(d.h.e.hl(this.getId()));
//                    this.removeItem(5133000, 1);
//                    this.fj = false;
//                }
            }
        }
        if (pyramid && pyramidSubway != null) { //checks if they had pyramid before AND after changing
            pyramidSubway.onChangeMap(this, to.getId());
        }
        playerObservable.update();
    }

    public void cancelChallenge() {
        if (challenge != 0 && client.getChannelServer() != null) {
            MapleCharacter chr = client.getChannelServer().getPlayerStorage().getCharacterById(challenge);
            if (chr != null) {
                chr.dropMessage(6, getName() + " 拒绝了您的请求.");
                chr.setChallenge(0);
            }
            dropMessage(6, "您的请求被拒绝.");
            challenge = 0;
        }
    }

    /*
     * 离开地图处理设置
     */
    public void leaveMap(MapleMap map) {
        controlledLock.writeLock().lock();
        visibleMapObjectsLock.writeLock().lock();
        try {
            for (MapleMonster mons : controlled) {
                if (mons != null) {
                    mons.setController(null);
                    mons.setControllerHasAggro(false);
                    mons.setControllerKnowsAboutAggro(false);
                    map.updateMonsterController(mons);
                }
            }
            controlled.clear();
            visibleMapObjects.clear();
        } finally {
            controlledLock.writeLock().unlock();
            visibleMapObjectsLock.writeLock().unlock();
        }
        if (chair != 0) {
            chair = 0;
        }
        clearLinkMid();
        cancelFishingTask();
        cancelChallenge();
        resetConversation();
        if (getBattle() != null) {
            getBattle().forfeit(this, true);
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        cancelMapTimeLimitTask();
        if (getTrade() != null) {
            MapleTrade.cancelTrade(getTrade(), client, this);
        }
    }

    /*
     * 改变职业
     */
    public void changeJob(int newJob) {
        try {
            cancelEffectFromBuffStat(MapleBuffStat.影分身);
            int tmpJob = newJob;
            this.job = (short) newJob;
            //双刀
            if (this.getSubcategory() == 1 && newJob == 400) {
                tmpJob = 400 + 65536;
            }
            updateSingleStat(MapleStat.职业, tmpJob);
            if (!JobConstants.is新手职业(newJob) && !JobConstants.is神之子(newJob)) { //非新手职业和神之子职业
                if (JobConstants.isSeparatedSpJob(newJob)) { //是特殊职业
                    int changeSp = 5;
                    switch (JobConstants.getSkillBookByJob(newJob)) {
                        case 2:
                            changeSp = 4;
                            break;
                        case 3:
                            changeSp = 3;
                            break;
                    }
                    remainingSp[JobConstants.getSkillBookByJob(newJob)] += changeSp;
                    client.announce(UIPacket.getSPMsg((byte) changeSp, (short) newJob));
                } else {
                    remainingSp[JobConstants.getSkillBookByJob(newJob)]++;
                    if (newJob % 10 >= 2) {
                        remainingSp[JobConstants.getSkillBookByJob(newJob)] += 2;
                    }
                }
                if (newJob % 10 >= 1 && level >= 70) { //3rd job or higher. lucky for evans who get 80, 100, 120, 160 ap...
                    remainingAp += 5;
                    updateSingleStat(MapleStat.AVAILABLEAP, remainingAp);
                    Skill skil = SkillFactory.getSkill(PlayerStats.getSkillByJob(1007, getJob()));
                    if (skil != null && getSkillLevel(skil) <= 0) {
                        dropMessage(-1, "恭喜你获得锻造技能。");
                        changeSingleSkillLevel(skil, skil.getMaxLevel(), (byte) skil.getMaxLevel());
                    }
                }
                if (getLevel() > 10 && newJob % 100 == 0 && (newJob % 1000) / 100 > 0) { //first job
                    remainingSp[JobConstants.getSkillBookByJob(newJob)] += 3 * (getLevel() - 10);
                }
                if (JobConstants.is龙神(job)) { //龙神
                    if (dragon == null) {
                        dragon = new MapleDragon(this);
                    }
                    dragon.setJobid(job);
                    dragon.sendSpawnData(client);
//                    MapleQuest.getInstance(22100).forceStart(this, 0, null);
//                    MapleQuest.getInstance(22100).forceComplete(this, 0);
//                    client.announce(NPCPacket.getEvanTutorial("UI/tutorial/evan/14/0"));
//                    dropMessage(5, "孵化器里的蛋中孵化出了幼龙，获得了可以提升龙的技能的3点SP，幼龙好像想说话。点击幼龙，和它说话吧！");
                }
                updateSingleStat(MapleStat.AVAILABLESP, 0);
            }

            int maxhp = stats.getMaxHp(), maxmp = stats.getMaxMp();

            switch (job) {
                case 100: // 战士
                case 1100: // 魂骑士1转
                case 2100: // 战神1转
                case 3200: // 幻灵斗师1转
                case 5100: // 米哈尔1转
                case 6100: // 狂龙战士1转
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3100: // 恶魔猎手1转
                    maxhp += Randomizer.rand(200, 250);
                    break;
                case 3110: // 恶魔猎手2转
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 3101: //恶魔复仇者1转
                case 3120: //恶魔复仇者2转
                    maxhp += Randomizer.rand(500, 800);
                    break;
                case 200: // 魔法师
                case 2200: // 龙神1转
                case 2700: // 夜光法师1转
                    maxmp += Randomizer.rand(100, 150);
                    break;
                case 300: // 弓箭手
                case 400: // 飞侠
                case 500: // 海盗
                case 501: // 海盗炮手
                case 509: // 海盗 - 新
                case 2300: // 双弩精灵1转
                case 2400: // 幻影1转
                case 3300: // 弩豹游侠1转
                case 3500: // 机械师1转
                case 3600: // 尖兵1转
                    maxhp += Randomizer.rand(100, 150);
                    maxmp += Randomizer.rand(25, 50);
                    break;
                case 110: // 剑客
                case 120: // 准骑士
                case 130: // 枪战士
                case 1110: // 魂骑士2转
                case 2110: // 战神2转
                case 3210: // 幻灵斗师2转
                case 5110: // 米哈尔2转
                    maxhp += Randomizer.rand(300, 350);
                    break;
                case 6110: // 狂龙战士2转
                    maxhp += Randomizer.rand(350, 400);
                    maxmp += Randomizer.rand(120, 180);
                    break;
                case 210: // 火毒法师
                case 220: // 冰雷法师
                case 230: // 牧师
                case 2710: // 夜光法师2转
                    maxmp += Randomizer.rand(400, 450);
                    break;
                case 310: // 猎人
                case 320: // 弩弓手
                case 410: // 刺客
                case 420: // 侠客
                case 430: // 见习刀客
                case 510: // 拳手
                case 520: // 火枪手
                case 530: // 火炮手
                case 570: // 龙的传人2转
                case 580: // 拳手
                case 590: // 火枪手
                case 2310: // 双弩精灵2转
                case 2410: // 幻影2转
                case 1310: // 风灵使者2转
                case 1410: // 夜行者2转
                case 3310: // 弩豹游侠2转
                case 3510: // 机械师2转
                case 3610: // 尖兵2转
                    maxhp += Randomizer.rand(200, 250);
                    maxhp += Randomizer.rand(150, 200);
                    break;
                case 900: // 管理员
                case 800: // 管理者
                    maxhp += 99999;
                    maxmp += 99999;
                    break;
            }
            if (maxhp >= getMaxHpForSever()) {
                maxhp = getMaxHpForSever();
            }
            if (maxmp >= getMaxMpForSever()) {
                maxmp = getMaxMpForSever();
            }
            if (JobConstants.isNotMpJob(job)) {
                maxmp = 10;
            }
            stats.setInfo(maxhp, maxmp, stats.getCurrentMaxHp(), stats.getCurrentMaxMp(job));
            characterCard.recalcLocalStats(this);
            stats.recalcLocalStats(this);
            List<Pair<MapleStat, Long>> statup = new ArrayList<>(4);
            statup.add(new Pair<>(MapleStat.HP, (long) stats.getCurrentMaxHp()));
            statup.add(new Pair<>(MapleStat.MP, (long) stats.getCurrentMaxMp(job)));
            statup.add(new Pair<>(MapleStat.MAXHP, (long) maxhp));
            statup.add(new Pair<>(MapleStat.MAXMP, (long) maxmp));
            client.announce(MaplePacketCreator.updatePlayerStats(statup, this));
            map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0x0D), false);
            map.broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
            silentPartyUpdate();
            guildUpdate();
            familyUpdate();
            if (dragon != null) {
                map.broadcastMessage(SummonPacket.removeDragon(this.id));
                dragon = null;
            }
            if (lw != null) {
                lw = null;
            }
            baseSkills(); //修复技能
            if (JobConstants.is龙神(newJob)) { //龙神
                if (getBuffedValue(MapleBuffStat.骑兽技能) != null) {
                    cancelBuffStats(MapleBuffStat.骑兽技能);
                }
                makeDragon();
            }
            if (newJob >= 4200 && newJob <= 4212 || newJob == 4002) {
                if (getBuffedValue(MapleBuffStat.骑兽技能) != null) {
                    cancelBuffStats(MapleBuffStat.骑兽技能);
                }
                makeLittleWhite();
            }
            if (newJob == 3300) { //弩豹游侠
                String customData = "1=1;2=1;3=1;4=1;5=1;6=1;7=1;8=1;9=1";
                setQuestAdd(MapleQuest.getInstance(GameConstants.美洲豹管理), (byte) 1, customData);
                client.announce(MaplePacketCreator.updateInfoQuest(GameConstants.美洲豹管理, customData));
                client.announce(MaplePacketCreator.updateJaguar(this));
            }
            playerObservable.update();
        } catch (Exception e) {
            log.error("转职错误", e); //all jobs throw errors :(
        }
    }

    public void checkZeroItem() {
        if (job != 10112 || level < 100) {
            return;
        }
        if (getKeyValue("Zero_Item") == null) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            //删除以前的道具
            int[] toRemovePos = {-9, -5, -7};
            for (int pos : toRemovePos) {
                Item toRemove = getInventory(MapleInventoryType.EQUIPPED).getItem((short) pos);
                if (toRemove != null) {
                    MapleInventoryManipulator.removeFromSlot(client, MapleInventoryType.EQUIPPED, toRemove.getPosition(), toRemove.getQuantity(), false);
                }
            }
            //给神之子装备
            int[][] equips = new int[][]{{1003840, -1}, //帽子
                    {1032202, -4}, //耳环
                    {1052606, -5}, //衣服
                    {1072814, -7}, //鞋子
                    {1082521, -8}, //手套
                    {1102552, -9}, //披风
                    {1113059, -12}, //戒指  无潜能
                    {1113060, -13}, //戒指  无潜能
                    {1113061, -15}, //戒指  无潜能
                    {1113062, -16}, //戒指  无潜能
                    {1122260, -17}, //项链
                    {1132231, -29}, //腰带
                    {1152137, -30}, //护肩  无潜能
            };
            for (int[] i : equips) {
                if (ii.itemExists(i[0])) {
                    Equip equip = (Equip) ii.getEquipById(i[0]);
                    equip.setPosition((byte) i[1]);
                    equip.setQuantity((short) 1);
                    if (i[1] != -12 && i[1] != -13 && i[1] != -15 && i[1] != -16 && i[1] != -30) {
                        equip.renewPotential(false);
                    }
                    equip.setGMLog("系统赠送");
                    forceReAddItem_NoUpdate(equip, MapleInventoryType.EQUIPPED);
                    client.announce(InventoryPacket.modifyInventory(false, Collections.singletonList(new ModifyInventory(0, equip)))); //发送获得道具的封包
                }
            }
            equipChanged();
            MapleInventoryManipulator.addById(client, 1142634, (short) 1, "系统赠送");
            MapleInventoryManipulator.addById(client, 2001530, (short) 100, "系统赠");
            //给角色技能
            Map<Integer, SkillEntry> list = new HashMap<>();
            int[] skillIds = {101000103, 101000203};
            for (int i : skillIds) {
                Skill skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 8, (byte) skil.getMaxLevel(), -1));
                }
            }
            if (!list.isEmpty()) {
                changeSkillsLevel(list);
            }
            setKeyValue("Zero_Item", "True");
        }
    }

    public void checkZeroWeapon() {
        if (level < 100) {
            return;
        }
        int lazuli = getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11).getItemId();
        int lapis = getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10).getItemId();
        if (lazuli == getZeroWeapon(false) && lapis == getZeroWeapon(true)) {
            return;
        }
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (int i = 0; i < 2; i++) {
            int itemId = i == 0 ? getZeroWeapon(false) : getZeroWeapon(true);
            Equip equip = (Equip) ii.getEquipById(itemId);
            equip.setPosition((short) (i == 0 ? -11 : -10));
            equip.setQuantity((short) 1);
            equip.setGMLog("神之子升级赠送, 时间:" + DateUtil.getNowTime());
            equip.renewPotential(false);
            forceReAddItem_NoUpdate(equip, MapleInventoryType.EQUIPPED);
            client.announce(InventoryPacket.modifyInventory(false, Collections.singletonList(new ModifyInventory(0, equip)))); //发送获得道具的封包
        }
        equipChanged();
    }

    public int getZeroWeapon(boolean lapis) {
        if (level < 100) {
            return lapis ? 1562000 : 1572000;
        }
        int weapon = lapis ? 1562001 : 1572001;
        if (level < 160) {
            weapon += (level % 100) / 10;
        } else if (level < 170) {
            weapon += 5;
        } else {
            weapon += 6;
        }
        return weapon;
    }

    /*
     * 修复技能
     */
    public void baseSkills() {
        checkZeroItem();
        checkInnerSkill();
//        checkZeroWeapon();
        checkBeastTamerSkill();
        checkHyperAP();
        Map<Integer, SkillEntry> list = new HashMap<>();
        if (JobConstants.getJobNumber(job) >= 3) { //third job.
            List<Integer> baseSkills = SkillFactory.getSkillsByJob(job);
            if (baseSkills != null) {
                for (int i : baseSkills) {
                    Skill skil = SkillFactory.getSkill(i);
                    //System.err.println("夜光技能: " + !skil.isInvisible() + " - " + skil.getMasterLevel() + " - " + skil.getId() + " - " + skil.getName());
                    if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                        list.put(i, new SkillEntry((byte) 0, (byte) (JobConstants.is超能力者(job) ? skil.getMaxLevel() : skil.getMasterLevel()), SkillFactory.getDefaultSExpiry(skil))); //usually 10 master
                    } else if (skil != null && skil.getName() != null && skil.getName().contains("冒险岛勇士") && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        list.put(i, new SkillEntry((byte) 0, (byte) 10, SkillFactory.getDefaultSExpiry(skil)));
                    } else if (skil != null && skil.getName() != null && (skil.getName().contains("希纳斯的骑士") || skil.getName().contains("希纳斯骑士")) && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        list.put(i, new SkillEntry((byte) 0, (byte) 30, SkillFactory.getDefaultSExpiry(skil)));
                    }
                }
            }
        }
        Skill skil;
        if (job >= 3300 && job <= 3312) {
            int[] ss = {预备兵.捕获, 预备兵.猎人的召唤, 豹弩游侠.美洲豹骑士, 豹弩游侠.召唤美洲豹_灰, 豹弩游侠.利爪狂风, 豹弩游侠.激怒, 豹弩游侠.美洲豹管理};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is机械师(job)) {
            int[] ss = {预备兵.机械冲撞, 预备兵.隐藏碎片};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is火炮手(job) && level >= 10) {
            skil = SkillFactory.getSkill(110);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(110, new SkillEntry((byte) 1, (byte) 1, -1));
            }
        } else if (JobConstants.is骑士团(job)) {
            int[] ss = {初心者.元素斩击, 初心者.回归, 初心者.元素和声_力量, 初心者.希纳斯护佑_战士, 初心者.元素跃动, 初心者.元素专家, 初心者.元素跃动_闪跃};
            if (JobConstants.is炎术士(job)) {
                ss[2] = 初心者.元素和声_智力;
                ss[3] = 初心者.希纳斯护佑_魔法师;
            } else if (JobConstants.is风灵使者(job)) {
                ss[2] = 初心者.元素和声_敏捷;
                ss[3] = 初心者.希纳斯护佑_弓箭手;
            } else if (JobConstants.is夜行者(job)) {
                ss[2] = 初心者.元素和声_运气;
                ss[3] = 初心者.希纳斯护佑_飞侠;
            } else if (JobConstants.is奇袭者(job)) {
                ss[3] = 初心者.希纳斯护佑_海盗;
            }
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
            if (JobConstants.is夜行者(job)) {
                int[] ss2 = {夜行者.影跃, 夜行者.快速闪避};
                for (int i : ss2) {
                    skil = SkillFactory.getSkill(i);
                    if (skil != null && getSkillLevel(skil) <= 0) {
                        list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
        } else if (JobConstants.is恶魔猎手(job) && level >= 10 && job != 3001) {
            int[] ss = {恶魔猎手.恶魔跳跃, 恶魔猎手.死亡诅咒, 恶魔猎手.恶魔之翼1, 恶魔猎手.恶魔之血, 恶魔猎手.恶魔跳跃1, 恶魔猎手.恶魔跳跃2, 恶魔猎手.恶魔跳跃3};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is恶魔复仇者(job) && level >= 10) {
            int[] ss = {恶魔复仇者.恶魔之翼, 恶魔复仇者.恶魔跳跃, 恶魔复仇者.恶魔之血, 恶魔复仇者.野性狂怒, 恶魔复仇者.血之契约, 恶魔复仇者.超越, 恶魔复仇者.高效吸收};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is尖兵(job) && level >= 10) {
            int[] ss = {尖兵.急速支援, 尖兵.多线程Ⅰ, 尖兵.机械战车冲刺, 尖兵.多模式链接, 尖兵.自由飞行, 尖兵.伪装};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is双弩精灵(job) && level >= 10) {
            int[] ss = {双弩.精灵的祝福, 双弩.优雅移动, 双弩.王者资格, 双弩.精灵恢复, 20021160, 20021161};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is龙的传人(job) && level >= 10) {
            skil = SkillFactory.getSkill(龙的传人.身轻如燕);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(龙的传人.身轻如燕, new SkillEntry((byte) 1, (byte) 1, -1));
            }
            skil = SkillFactory.getSkill(龙的传人.宝盒的庇佑);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(龙的传人.宝盒的庇佑, new SkillEntry((byte) 1, (byte) 1, -1));
            }
        } else if (JobConstants.is隐月(job)) {
            int[] ss = {隐月.精灵凝聚第1招, 隐月.缩地};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is幻影(job) && level >= 10) {
            int[] ss = {幻影.幻影回归, 幻影.致命本能, 幻影.幻影屏障, 幻影.灵敏身手, 幻影.印技之瞳, 幻影.印技树, 20031160, 20031161};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
            if (job == 2412) {
                skil = SkillFactory.getSkill(幻影.卡牌审判_高级);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(幻影.卡牌审判_高级, new SkillEntry((byte) 1, (byte) 1, -1));
                }
                skil = SkillFactory.getSkill(幻影.卡牌审判);
                if (skil != null && getSkillLevel(skil) > 0) {
                    list.put(幻影.卡牌审判, new SkillEntry((byte) 0, (byte) 0, -1));
                }
            } else {
                skil = SkillFactory.getSkill(幻影.卡牌审判);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(幻影.卡牌审判, new SkillEntry((byte) 1, (byte) 1, -1));
                }
                skil = SkillFactory.getSkill(幻影.卡牌审判_高级);
                if (skil != null && getSkillLevel(skil) > 0) {
                    list.put(幻影.卡牌审判_高级, new SkillEntry((byte) 0, (byte) 0, -1));
                }
            }
        } else if (JobConstants.is夜光(job) && level >= 10) {
            int[] ss = {夜光.太阳火焰, 夜光.月蚀, 夜光.穿透, 夜光.平衡_光明, 夜光.光明黑暗模式转换, 夜光.光之力量, 夜光.光之传送};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
            int[] fixskills = {夜光.耀眼光球, 夜光.黑暗降临, 夜光.光明黑暗魔法强化};
            for (int f : fixskills) {
                skil = SkillFactory.getSkill(f);
                if (skil != null && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                    list.put(f, new SkillEntry((byte) 1, (byte) skil.getMasterLevel(), SkillFactory.getDefaultSExpiry(skil)));
                }
            }
        } else if (job >= 432 && job <= 434) {
            int[] fixskills = {双刀.双刀风暴, 双刀.暗影飞跃斩, 双刀.影子闪避, 双刀.幽灵一击, 双刀.终极斩};
            for (int i : fixskills) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && !skil.isInvisible() && skil.isFourthJob() && skil.getMasterLevel() > 0) {
                    if (getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0) {
                        list.put(i, new SkillEntry((byte) 0, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
                    } else if (getMasterLevel(skil) <= skil.getMaxLevel()) {
                        list.put(i, new SkillEntry((byte) getSkillLevel(skil), (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil)));
                    }
                }
            }
        } else if (JobConstants.is米哈尔(job) && level >= 10) {
            int[] ss = {米哈尔.光之守护};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is狂龙战士(job) && level >= 10) {
            int[] ss = {狂龙战士.防御模式, 狂龙战士.攻击模式, 狂龙战士.垂直连接, 狂龙战士.变身, 狂龙战士.钢铁之墙};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (JobConstants.is爆莉萌天使(job) && level >= 10) {
            int[] ss = {爆莉萌天使.释世书, 爆莉萌天使.魔法裂隙, 爆莉萌天使.灵魂契约, 爆莉萌天使.协调, 爆莉萌天使.装扮};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                }
            }
        } else if (job == 10112 && level >= 100) {
            List<Integer> baseSkills = SkillFactory.getSkillsByJob(10100);
            baseSkills.addAll(SkillFactory.getSkillsByJob(10110));
            baseSkills.addAll(SkillFactory.getSkillsByJob(10111));
            baseSkills.addAll(SkillFactory.getSkillsByJob(10112));
            for (int i : baseSkills) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && !skil.isInvisible() && skil.isFourthJob() && getSkillLevel(skil) <= 0 && getMasterLevel(skil) <= 0 && skil.getMasterLevel() > 0) {
                    list.put(i, new SkillEntry((byte) 0, (byte) skil.getMaxLevel(), SkillFactory.getDefaultSExpiry(skil))); //usually 10 master
                }
            }
            int[] skillIds = {神之子.重归神殿, 神之子.双重打击, 神之子.圣洁之力, 神之子.神圣迅捷, 神之子.暴风一跃, 神之子.暴风步, 神之子.伦娜之庇护, 神之子.决意时刻, 神之子.提速时刻, 神之子.时光扭曲, 神之子.时间制控, 神之子.时光逆流, 神之子.影子雨印, 神之子.强化时间}; //100001262 重归神殿
            for (int i : skillIds) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && skil.canBeLearnedBy(getJob()) && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), -1));
                }
            }
        } else if (JobConstants.is剑豪(job)) {
            int[] ss = {剑豪.天赋之才, 剑豪.防御是最好的攻击, 剑豪.拔刀姿势, 剑豪.基本姿势加成, 剑豪.拔刀术加成, 剑豪.百刃一闪, 剑豪.晓月流基本技能};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) skil.getMaxLevel(), (byte) 1, -1));
                }
            }
        } else if (JobConstants.is阴阳师(job)) {
            int[] ss = {阴阳师.五行的加持, 阴阳师.无限之灵力, 阴阳师.小白};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) skil.getMaxLevel(), (byte) 1, -1));
                }
            }
        } else if (JobConstants.is超能力者(job)) {
            int[] ss = {超能力者.心魂突袭, 超能力者.心魂本能};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) skil.getMaxLevel(), (byte) 1, -1));
                }
            }
        } else if (JobConstants.is爆破手(job) && level >= 10) {
            int[] ss = {预备兵.秘密广场紧急集结, 爆破手.转管炮, 爆破手.装填弹药};
            for (int i : ss) {
                skil = SkillFactory.getSkill(i);
                if (job >= i / 10000 && skil != null && this.getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry(1, (byte) 1, -1));
                }
            }
        }
        List<Integer> fixSkills = SkillFactory.getSkillsByJob(job);
        if (fixSkills != null) {
            for (int i : fixSkills) {
                Skill sk = SkillFactory.getSkill(i);
                if (sk != null && sk.getFixLevel() > 0) {
                    if (sk.canBeLearnedBy(getJob()) && getSkillLevel(sk) <= 0) {
                        list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
                    }
                }
            }
        }
        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }
    }

    public void makeDragon() {
        dragon = new MapleDragon(this);
    }

    public MapleDragon getDragon() {
        return dragon;
    }

    public void setDragon(MapleDragon d) {
        this.dragon = d;
    }

    public void makeLittleWhite() {
        lw = new MapleLittleWhite(this);
        map.broadcastMessage(SummonPacket.spawnLittleWhite(lw));//showLittleWhite
    }

    public MapleLittleWhite getLittleWhite() {
        return lw;
    }

    public void setLittleWhite(MapleLittleWhite lw) {
        this.lw = lw;
    }

    public void gainAp(short ap) {
        this.remainingAp += ap;
        updateSingleStat(MapleStat.AVAILABLEAP, this.remainingAp);
    }

    public void gainSP(int sp) {
        this.remainingSp[JobConstants.getSkillBookByJob(job)] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        client.announce(UIPacket.getSPMsg((byte) sp, job));
    }

    public void gainSP(int sp, int skillbook) {
        this.remainingSp[skillbook] += sp; //default
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        client.announce(UIPacket.getSPMsg((byte) sp, (short) 0));
    }

    public void resetSP(int sp) {
        for (int i = 0; i < remainingSp.length; i++) {
            this.remainingSp[i] = sp;
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0);
    }

    public void resetAPSP() {
        resetSP(0);
        gainAp((short) -this.remainingAp);
    }

    public int getHitCountBat() {
        return hitcountbat;
    }

    public void setHitCountBat(int hitcount) {
        this.hitcountbat = hitcount;
    }

    public int getBatCount() {
        return batcount;
    }

    public void setBatCount(int count) {
        this.batcount = count;
    }

    /*
     * 专业技能技能设置
     */
    public List<Integer> getProfessions() {
        List<Integer> prof = new ArrayList<>();
        for (int i = 9200; i <= 9204; i++) {
            if (getProfessionLevel(i * 10000) > 0) {
                prof.add(i);
            }
        }
        return prof;
    }

    public byte getProfessionLevel(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (byte) ((ret >>> 24) & 0xFF); //the last byte
    }

    public short getProfessionExp(int id) {
        int ret = getSkillLevel(id);
        if (ret <= 0) {
            return 0;
        }
        return (short) (ret & 0xFFFF); //the first two byte
    }

    public boolean addProfessionExp(int id, int expGain) {
        int ret = getProfessionLevel(id);
        if (ret <= 0 || ret >= 10) {
            return false;
        }
        int newExp = getProfessionExp(id) + expGain;
        if (newExp >= GameConstants.getProfessionEXP(ret)) {
            //gain level
            changeProfessionLevelExp(id, ret + 1, newExp - GameConstants.getProfessionEXP(ret));
            int traitGain = (int) Math.pow(2, ret + 1);
            switch (id) {
                case 92000000: //采药
                    traits.get(MapleTraitType.sense).addExp(traitGain, this);
                    break;
                case 92010000: //采矿
                    traits.get(MapleTraitType.will).addExp(traitGain, this);
                    break;
                case 92020000: //装备制作
                case 92030000: //饰品制作
                case 92040000: //炼金术
                    traits.get(MapleTraitType.craft).addExp(traitGain, this);
                    break;
            }
            return true;
        } else {
            changeProfessionLevelExp(id, ret, newExp);
            return false;
        }
    }

    /*
     * 改变专业技能技能的等级
     */
    public void changeProfessionLevelExp(int id, int level, int exp) {
        changeSingleSkillLevel(SkillFactory.getSkill(id), ((level & 0xFF) << 24) + (exp & 0xFFFF), (byte) 10);
    }

    /*
     * 改变单个技能技能的等级
     */
    public void changeSkillLevel(Skill skill, byte newLevel, byte newMasterlevel) {
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, -1);
    }

    public void changeSingleSkillLevel(Skill skill, int newLevel, byte newMasterlevel) { //1 month
        if (skill == null) {
            return;
        }
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(int skillid, int newLevel, byte newMasterlevel) { //1 month
        Skill skill = SkillFactory.getSkill(skillid);
        changeSingleSkillLevel(skill, newLevel, newMasterlevel, SkillFactory.getDefaultSExpiry(skill));
    }

    public void changeSingleSkillLevel(Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        Map<Integer, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        if (changeSkillData(skill, newLevel, newMasterlevel, expiration)) { // no loop, only 1
            list.put(skill.getId(), new SkillEntry(newLevel, newMasterlevel, expiration, getSkillTeachId(skill), getSkillPosition(skill)));
            if (SkillConstants.isRecoveryIncSkill(skill.getId())) {
                hasRecovery = true;
            }
            if (skill.getId() < 80000000) {
                recalculate = true;
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.announce(MaplePacketCreator.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    public void changeTeachSkillsLevel() {
        getSkills().forEach((skillid, ao2) -> {
                    Skill skill = SkillFactory.getSkill(skillid);
            if (skill != null && skill.isTeachSkills()) {
                        changeSkillData(skill, SkillConstants.getLinkSkillslevel(skill.getMaxLevel(), skill, 0, level), (byte) skill.getMasterLevel(), ao2.expiration);
                    }
                }
        );
    }

    public void changeSkillsLevel(Map<Integer, SkillEntry> skills) {
        if (skills.isEmpty()) {
            return;
        }
        Map<Integer, SkillEntry> list = new HashMap<>();
        boolean hasRecovery = false, recalculate = false;
        for (Entry<Integer, SkillEntry> data : skills.entrySet()) {
            Skill skill = SkillFactory.getSkill(data.getKey());
            if (changeSkillData(skill, data.getValue().skillevel, data.getValue().masterlevel, data.getValue().expiration)) {
                list.put(data.getKey(), data.getValue());
                if (SkillConstants.isRecoveryIncSkill(data.getKey())) {
                    hasRecovery = true;
                }
                if (data.getKey() < 80000000) {
                    recalculate = true;
                }
            }
        }
        if (list.isEmpty()) { // nothing is changed
            return;
        }
        client.announce(MaplePacketCreator.updateSkills(list));
        reUpdateStat(hasRecovery, recalculate);
    }

    /*
     * 角色技能改变后更新角色能力状态
     */
    private void reUpdateStat(boolean hasRecovery, boolean recalculate) {
        changed_skills = true;
        if (hasRecovery) {
            stats.relocHeal(this);
        }
        if (recalculate) {
            stats.recalcLocalStats(this);
        }
    }

    /*
     * 新的改变技能
     */
    public boolean changeSkillData(Skill skill, int newLevel, byte newMasterlevel, long expiration) {
        if (skill == null || !SkillConstants.isApplicableSkill(skill.getId()) && !SkillConstants.isApplicableSkill_(skill.getId())) {
            return false;
        }
        if (newLevel <= 0 && newMasterlevel <= 0) {
            if (skills.containsKey(skill.getId())) {
                skills.remove(skill.getId());
            } else {
                return false; //nothing happen
            }
        } else {
            skills.put(skill.getId(), new SkillEntry(newLevel, newMasterlevel, expiration, getSkillTeachId(skill), getSkillPosition(skill)));
        }
        return true;
    }

    public void changeSkillLevel_Skip(Map<Integer, SkillEntry> skill) {
        changeSkillLevel_Skip(skill, false);
    }

    public void changeSkillLevel_Skip(Map<Integer, SkillEntry> skill, boolean write) {
        if (skill.isEmpty()) {
            return;
        }
        Map<Integer, SkillEntry> newlist = new HashMap<>();
        for (Entry<Integer, SkillEntry> date : skill.entrySet()) {
            if (date.getKey() == null) {
                continue;
            }
            //System.err.println("changeSkillLevel_Skip - " + date.getKey().getId() + " skillevel " + date.getValue().skillevel + " masterlevel " + date.getValue().masterlevel + " - " + date.getKey().getName());
            newlist.put(date.getKey(), date.getValue());
            if (date.getValue().skillevel == 0 && date.getValue().masterlevel == 0) {
                if (skills.containsKey(date.getKey())) {
                    skills.remove(date.getKey());
                }
            } else {
                skills.put(date.getKey(), date.getValue());
            }
        }
        if (write && !newlist.isEmpty()) {
            client.announce(MaplePacketCreator.updateSkills(newlist));
        }
    }

    public void changePetSkillLevel(Map<Integer, SkillEntry> skill) {
        if (skill.isEmpty()) {
            return;
        }
        Map<Integer, SkillEntry> newlist = new HashMap<>();
        for (Entry<Integer, SkillEntry> date : skill.entrySet()) {
            if (date.getKey() == null) {
                continue;
            }
            //System.err.println("changePetSkillLevel - ID: " + date.getKey().getId() + " skillevel: " + date.getValue().skillevel + " masterlevel: " + date.getValue().masterlevel + " - " + date.getKey().getName());
            if (date.getValue().skillevel == 0 && date.getValue().masterlevel == 0) {
                if (skills.containsKey(date.getKey())) {
                    skills.remove(date.getKey());
                    newlist.put(date.getKey(), date.getValue());
                }
            } else if (getSkillLevel(date.getKey()) != date.getValue().skillevel) {
                skills.put(date.getKey(), date.getValue());
                newlist.put(date.getKey(), date.getValue());
            }
        }
        if (!newlist.isEmpty()) {
            for (Entry<Integer, SkillEntry> date : newlist.entrySet()) {
                client.announce(MaplePacketCreator.updatePetSkill(date.getKey(), date.getValue().skillevel, date.getValue().masterlevel, date.getValue().expiration));
            }
            reUpdateStat(false, true);
        }
    }

    public void changeTeachSkill(int skillid, int teachid, int skillevel, boolean delete) {
        Skill skill = SkillFactory.getSkill(skillid);
        if (skill == null) {
            return;
        }
        if (delete) {
            HashMap<Integer, Integer> hashMap = new HashMap<>();
            hashMap.put(skillid, teachid);
            skills.remove(skillid);
            linkSkills.remove(skillid);
            if (skillid == 80000055 || skillid == 80000329) {
                int[] arrn = SkillConstants.gm(skillid);
                if (arrn != null) {
                    for (int id : arrn) {
                        Skill skill1 = SkillFactory.getSkill(id);
                        SkillEntry se = linkSkills.remove(id);
                        skills.remove(id);
                        if (skill1 != null && se != null) {
                            hashMap.put(id, se.teachId);
                            sonOfLinkedSkills.get(id).getRight().teachId = se.teachId;
                            try (DruidPooledConnection conn = DatabaseConnection.getInstance().getConnection()) {
                                try (PreparedStatement ps = conn.prepareStatement("UPDATE skills SET teachId = ? WHERE skillid = ? AND characterid = ?")) {
                                    int n6 = 0;
                                    ps.setInt(++n6, 0);
                                    ps.setInt(++n6, SkillConstants.go(id));
                                    ps.setInt(++n6, se.teachId);
                                    ps.executeUpdate();
                                }
                            } catch (SQLException e) {
                                log.error("传授技能失败 - changeTeachSkill", e);
                            }
                        }
                    }
                }
            } else {
                sonOfLinkedSkills.get(skillid).getRight().teachId = teachid;
            }
            send(MaplePacketCreator.DeleteLinkSkillResult(hashMap));
        } else {
            int gn = SkillConstants.gn(skillid);
            if (gn > 0) {
                Skill skill1 = SkillFactory.getSkill(gn);
                SkillEntry se = skills.get(skillid);
                if (skill1 != null && se == null) {
                    skills.put(skillid, new SkillEntry(skillevel, (byte) skill1.getMasterLevel(), System.currentTimeMillis(), teachid));
                } else {
                    skills.get(skillid).skillevel += skillevel;
                }
            }
            skills.put(skillid, new SkillEntry(skillevel, (byte) skill.getMasterLevel(), System.currentTimeMillis(), teachid));
            linkSkills.put(skillid, new SkillEntry(skillevel, (byte) skill.getMasterLevel(), System.currentTimeMillis(), teachid));
            sonOfLinkedSkills.get(skillid).getRight().teachId = this.id;
            sonOfLinkedSkills.get(skillid).getRight().expiration = System.currentTimeMillis();
        }
        changed_skills = true;
    }

    public void changeTeachSkill(int skillId, int toChrId) {
        SkillEntry ret = getSkillEntry(skillId);
        if (ret != null) {
            ret.teachId = toChrId;
            client.announce(MaplePacketCreator.updateSkill(skillId, toChrId, ret.masterlevel, ret.expiration));
            changed_skills = true;
        }
    }

    public void playerDead() {
        MapleStatEffect statss = getStatForBuff(MapleBuffStat.神秘运气);
        if (statss != null && this.isBuffFrom(MapleBuffStat.神秘运气, SkillFactory.getSkill(幻影.神秘的运气))) {
            dropMessage(5, "由于神秘的运气的效果发动，本次死亡经验您的经验值不会减少。");
            getStat().setHp(((getStat().getMaxHp() / 100) * statss.getX()), this);
            setStance(0);
            cancelEffectFromBuffStat(MapleBuffStat.神秘运气);
            return;
        }
        if (getSkillLevel(黑骑士.重生契约) > 0 && !skillisCooling(黑骑士.重生契约_状态)) {
            getStat().setHp(getStat().getCurrentMaxHp(), this);
            getStat().setMp(getStat().getCurrentMaxMp(getJob()), this);
            updateSingleStat(MapleStat.HP, getStat().getHp());
            updateSingleStat(MapleStat.MP, getStat().getMp());
            setStance(0);
            Skill skill = SkillFactory.getSkill(黑骑士.重生契约_状态);
            skill.getEffect(getSkillLevel(黑骑士.重生契约)).applyTo(this);
            return;
        }
        statss = getStatForBuff(MapleBuffStat.天堂之门);
        if (statss != null) {
            getStat().setHp(getStat().getCurrentMaxHp(), this);
            getStat().setMp(getStat().getCurrentMaxMp(getJob()), this);
            updateSingleStat(MapleStat.HP, getStat().getHp());
            updateSingleStat(MapleStat.MP, getStat().getMp());
            this.cancelEffectFromBuffStat(MapleBuffStat.天堂之门);
            return;
        }
        cancelEffectFromBuffStat(MapleBuffStat.影分身);
        cancelEffectFromBuffStat(MapleBuffStat.变身效果);
        cancelEffectFromBuffStat(MapleBuffStat.飞翔);
        cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
        cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
        cancelEffectFromBuffStat(MapleBuffStat.恢复效果);
        cancelEffectFromBuffStat(MapleBuffStat.增加最大HP);
        cancelEffectFromBuffStat(MapleBuffStat.增加最大MP);
        cancelEffectFromBuffStat(MapleBuffStat.增强_MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.增强_MAXMP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXHP);
        cancelEffectFromBuffStat(MapleBuffStat.MAXMP);
        cancelEffectFromBuffStat(MapleBuffStat.精神连接);
        cancelEffectFromBuffStat(MapleBuffStat.剑刃之壁);
        cancelEffectFromBuffStat(MapleBuffStat.飞行骑乘);
        cancelEffectFromBuffStat(MapleBuffStat.击杀点数);
        dispelSummons();
        checkFollow();
        if (getEventInstance() != null) {
            getEventInstance().playerKilled(this);
        }
        setPowerCount(0);
        specialStats.resetSpecialStats();
        if (!JobConstants.is新手职业(job) && !inPVP()) {
            int charms = getItemQuantity(5130000, false); //护身符 - 戴在身上，死后经验值不会下降。复活时回复MP,HP30%
            if (charms > 0) {
                MapleInventoryManipulator.removeById(client, MapleInventoryType.CASH, 5130000, 1, true, false);
                charms--;
                if (charms > 0xFF) {
                    charms = 0xFF;
                }
                client.announce(MTSCSPacket.INSTANCE.useCharm((byte) charms, (byte) 0));
            } else {
                float diepercentage;
                long expforlevel = getExpNeededForLevel();
                if (map.isTown() || FieldLimitType.RegularExpLoss.check(map.getFieldLimit())) {
                    diepercentage = 0.01f;
                } else {
                    diepercentage = 0.1f - ((traits.get(MapleTraitType.charisma).getLevel() / 20) / 100f);
                }
                long v10 = (exp.get() - (long) ((double) expforlevel * diepercentage));
                if (v10 < 0) {
                    v10 = 0;
                }
                this.exp.set(v10);
            }
            updateSingleStat(MapleStat.经验, this.exp.get());
        }
        if (!stats.checkEquipDurabilitys(this, -100)) { //i guess this is how it works ?
            dropMessage(5, "An item has run out of durability but has no inventory room to go to.");
        } //lol
        playerDeadResponse();
        if (pyramidSubway != null) {
            stats.setHp((short) 50, this);
            pyramidSubway.fail(this);
        }
    }

    public void playerDeadResponse() {
        int type = 1;
        int value = 0;
        if (this.getItemQuantity(5133000) > 0) {
            type |= 2;
        }
        if (this.getEventInstance() != null && this.getReviveCount() >= 0) {
            value = 4;
        } else if (this.getReviveCount() < 0) {
            if (this.getItemQuantity(5510000) > 0) {
                value = 6;
            } else if (this.getItemQuantity(5511001) > 0) {
                value = 12;
            }
        }
        client.announce(EffectPacket.playerDeadConfirm(type, value));
    }

    public void updatePartyMemberHP() {
        if (party != null && client.getChannelServer() != null) {
            int channel = client.getChannel();
            for (MaplePartyCharacter partychar : party.getMembers()) {
                if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                    MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                    if (other != null) {
                        other.getClient().announce(PartyPacket.updatePartyMemberHP(getId(), stats.getHp(), stats.getCurrentMaxHp()));
                    }
                }
            }
        }
    }

    public void receivePartyMemberHP() {
        if (party == null) {
            return;
        }
        int channel = client.getChannel();
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar != null && partychar.getMapid() == getMapId() && partychar.getChannel() == channel) {
                MapleCharacter other = client.getChannelServer().getPlayerStorage().getCharacterByName(partychar.getName());
                if (other != null) {
                    client.announce(PartyPacket.updatePartyMemberHP(other.getId(), other.getStat().getHp(), other.getStat().getCurrentMaxHp()));
                }
            }
        }
    }

    public void heal() {
        this.stats.heal(this);
    }

    /**
     * 恢复HP
     *
     * @param delta 恢复量
     */
    public void healHP(int delta) {
        addHP(delta);
        client.announce(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    /**
     * 恢复HP
     *
     * @param delta 恢复量
     */
    public void healMP(int delta) {
        addMP(delta);
        client.announce(EffectPacket.showOwnHpHealed(delta));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed(getId(), delta), false);
    }

    public void healHPMP(int deltahp, int deltamp) {
        addHPMP(deltahp, deltamp);
        send(EffectPacket.showOwnHpHealed(deltahp != 0 ? deltahp : deltamp));
        getMap().broadcastMessage(this, EffectPacket.showHpHealed_Other(getId(), deltahp != 0 ? deltahp : (deltamp)), false);
    }

    /**
     * Convenience function which adds the supplied parameter to the current hp
     * then directly does a updateSingleStat.
     *
     * @param delta
     */
    public void addHP(int delta) {
        if (stats.setHp(stats.getHp() + delta, this)) {
            updateSingleStat(MapleStat.HP, stats.getHp());
        }
    }

    /**
     * Convenience function which adds the supplied parameter to the current mp
     * then directly does a updateSingleStat.
     *
     * @param delta
     */
    public void addMP(int delta) {
        addMP(delta, false);
    }

    public void addMP(int delta, boolean ignore) {
        if (JobConstants.isNotMpJob(getJob()) && GameConstants.getMPByJob(getJob()) <= 0) {
            return;
        }
        if ((delta < 0 && JobConstants.is恶魔猎手(getJob())) || !JobConstants.is恶魔猎手(getJob()) || ignore) {
            if (stats.setMp(stats.getMp() + delta, this)) {
                updateSingleStat(MapleStat.MP, stats.getMp());
            }
        }
    }

    public void addDemonMp(int delta) {
        if (delta > 0 && (getJob() == 3111 || getJob() == 3112)) {
            if (stats.setMp(stats.getMp() + delta, this)) {
                updateSingleStat(MapleStat.MP, stats.getMp());
            }
        }
    }

    public void addHPMP(int hpDiff, int mpDiff) {
        addhpmpLock.writeLock().lock();
        try {
            List<Pair<MapleStat, Long>> statups = new ArrayList<>();
            int alpha = Math.min(getStat().getCurrentMaxHp(), stats.getHp() + hpDiff);
            int beta = Math.min(getStat().getCurrentMaxMp(getJob()), stats.getMp() + mpDiff);
            if (stats.setHp(alpha, this)) {
                statups.add(new Pair<>(MapleStat.HP, (long) stats.getHp()));
            }
            if ((mpDiff < 0 && JobConstants.is恶魔猎手(getJob())) || !JobConstants.is恶魔猎手(getJob())) {
                if (stats.setMp(beta, this)) {
                    statups.add(new Pair<>(MapleStat.MP, (long) stats.getMp()));
                }
            }
            if (statups.size() > 0) {
                client.announce(MaplePacketCreator.updatePlayerStats(statups, this));
            }
            if (!isAlive()) {
                playerDead();
            }
        } finally {
            addhpmpLock.writeLock().unlock();
        }
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     */
    public void updateSingleStat(MapleStat stat, long newval) {
        updateSingleStat(stat, newval, false);
    }

    /**
     * Updates a single stat of this MapleCharacter for the client. This method
     * only creates and sends an update packet, it does not update the stat
     * stored in this MapleCharacter instance.
     *
     * @param stat
     * @param newval
     * @param itemReaction
     */
    public void updateSingleStat(MapleStat stat, long newval, boolean itemReaction) {
        Pair<MapleStat, Long> statpair = new Pair<>(stat, newval);
        client.announce(MaplePacketCreator.updatePlayerStats(Collections.singletonList(statpair), itemReaction, this));
    }

    public void gainExp(long total, boolean show, boolean inChat, boolean white) {
        if (ServerConfig.WORLD_BANGAINEXP) {
            dropMessage(6, "管理员禁止了经验获取。");
            return;
        }
        try {
            long prevexp = getExp();
            long needed = getExpNeededForLevel();
            if (total > 0) {
                stats.checkEquipLevels(this, total); //gms like
                stats.checkEquipSealed(this, total);
            }
            if (level >= getMaxLevelForSever()) {
                setExp(0);
            } else {
                boolean leveled = false;
                long tot = exp.get() + total;
                if (tot >= needed) {
                    exp.addAndGet(total);
                    levelUp();
                    leveled = true;
                    if (level >= getMaxLevelForSever()) {
                        setExp(0);
                    } else {
                        needed = getExpNeededForLevel();
                        if (exp.get() >= needed) {
                            setExp(needed - 1);
                        }
                    }
                } else {
                    exp.addAndGet(total);
                }
                if (total > 0) {
                    familyRep((int) prevexp, (int) needed, leveled);
                }
            }
            if (total != 0) {
                if (exp.get() < 0) { // After adding, and negative
                    if (total > 0) {
                        setExp(needed);
                    } else {
                        setExp(0);
                    }
                }
                updateSingleStat(MapleStat.经验, getExp());
                if (show) { // still show the expgain even if it's not there
                    client.announce(MaplePacketCreator.GainEXP_Others(total, inChat, white));
                }
            }
        } catch (Exception e) {
            log.error("获得经验错误", e); //all jobs throw errors :(
        }
    }

    public void familyRep(int prevexp, int needed, boolean leveled) {
        if (mfc != null) {
            int onepercent = needed / 100;
            if (onepercent <= 0) {
                return;
            }
            int percentrep = (int) (getExp() / onepercent - prevexp / onepercent);
            if (leveled) {
                percentrep = 100 - percentrep + (level / 2);
            }
            if (percentrep > 0) {
                int sensen = WorldFamilyService.getInstance().setRep(mfc.getFamilyId(), mfc.getSeniorId(), percentrep * 10, level, name);
                if (sensen > 0) {
                    WorldFamilyService.getInstance().setRep(mfc.getFamilyId(), sensen, percentrep * 5, level, name); //and we stop here
                }
            }
        }
    }

    public void monsterMultiKill() {
        if (killmonsterExps.size() > 2) {
            long multiKillExp = 0;
            for (long exps : killmonsterExps) {
                multiKillExp += (long) Math.ceil(exps * (Math.min(monsterCombo, 60) + 1) * 0.5 / 100);
            }
            gainExp((int) multiKillExp, false, false, false);
        }
        killmonsterExps.clear();
    }

//    public void ZeroWeaponChange(Item item, Item item2, int n2) {
//        if (b.f.fS(item2.getItemId())) {
//            if (item != null) {
//                int n3 = n2 == -10 ? -11 : -10;
//                Equip b4 = (Equip) this.getInventory(MapleInventoryType.EQUIPPED).getItem((short)n3);
//                b4.a(item, item2);
//                this.forceReAddItem(b4, b4.getPosition() >= 0 ? a.b.m.AR : a.b.m.AY);
//            } else if (item == null) {
//                this.getInventory(a.b.m.AY).removeItem(item2.getPosition());
//            }
//        }
//    }

    public void gainExpMonster(long gain, boolean show, boolean white, int numExpSharers, boolean partyBonusMob, int partyBonusRate) {
        if (ServerConfig.WORLD_BANGAINEXP) {
            dropMessage(6, "管理员禁止了经验获取。");
            return;
        }
        long totalExp = gain;
        int 网吧经验 = 0;
        if (haveItem(5420008)) {
            网吧经验 = (int) ((gain / 100.0) * 25);
            totalExp += 网吧经验;
        }
        int 精灵祝福 = 0;
        if (get精灵祝福() > 0) {
            精灵祝福 = (int) ((gain / 100.0) * 10);
            totalExp += 精灵祝福;
        }
        int 结婚经验 = 0;
        if (marriageId > 0) {
            MapleCharacter marrChr = map.getCharacterById(marriageId);
            if (marrChr != null) {
                结婚经验 = (int) ((gain / 100.0) * 10);
                totalExp += 结婚经验;
            }
        }
        int 组队经验 = 0;
        if (numExpSharers > 1) {
            double rate = (partyBonusRate > 0 ? (partyBonusRate / 100.0) : (map == null || !partyBonusMob || map.getPartyBonusRate() <= 0 ? 0.05 : (map.getPartyBonusRate() / 100.0)));
            组队经验 = (int) (((float) (gain * rate)) * (numExpSharers + (rate > 0.05 ? -1 : 1)));
            totalExp += 组队经验;
        }

        long combokillExp = 0;
        if (killmonsterExps.isEmpty()) {
            if ((monsterCombo > 0) && (System.currentTimeMillis() - lastmonsterCombo > 7000)) {
                monsterCombo = 0;
            }
            monsterCombo += monsterCombo == 999 ? 0 : 1;
            lastmonsterCombo = System.currentTimeMillis();
        }
        if (monsterCombo > 1) {
            combokillExp += (long) Math.ceil(gain * Math.min(monsterCombo, 60) * 0.5D / 100.0D);
        }
        totalExp += combokillExp;

        int 道具佩戴经验 = 0;
        int 召回戒指经验 = 0;
        if (stats.equippedWelcomeBackRing) {
            召回戒指经验 = (int) ((gain / 100.0) * 80);
            totalExp += 召回戒指经验;
        }
        if (stats.equippedFairy > 0) {
            道具佩戴经验 = (int) ((gain / 100.0) * stats.equippedFairy);
            totalExp += 道具佩戴经验;
        }
        if (gain > 0 && totalExp < gain) {
            totalExp = Integer.MAX_VALUE;
        }
        if (totalExp > 0) {
            stats.checkEquipLevels(this, totalExp);
            gainBeastTamerSkillExp((int) totalExp);
            stats.checkEquipSealed(this, totalExp);
        }
        long prevexp = getExp(); //角色当前的经验
        long needed = getExpNeededForLevel(); //角色升级需要的经验
        if (level >= getMaxLevelForSever()) {
            setExp(0);
        } else {
            boolean leveled = false;
            if (exp.get() + totalExp >= needed || exp.get() >= needed) {
                exp.addAndGet(totalExp);
                levelUp();
                leveled = true;
                if (level >= getMaxLevelForSever()) {
                    setExp(0);
                } else {
                    needed = getExpNeededForLevel();
                    if (exp.get() >= needed) {
                        setExp(needed);
                    }
                }
            } else {
                exp.addAndGet(totalExp);
            }
            if (totalExp > 0) {
                familyRep((int) prevexp, (int) needed, leveled);
            }
        }
        if (gain != 0) {
            if (exp.get() < 0) {
                if (gain > 0) {
                    setExp(getExpNeededForLevel());
                } else if (gain < 0) {
                    setExp(0);
                }
            }
            updateSingleStat(MapleStat.经验, getExp());
            if (show) {
                Map<MapleExpStat, Integer> expStatup = new EnumMap<>(MapleExpStat.class);
                if (组队经验 > 0) {
                    expStatup.put(MapleExpStat.组队经验, 组队经验);
                }
                if (结婚经验 > 0) {
                    expStatup.put(MapleExpStat.结婚奖励经验, 结婚经验);
                }
                if (道具佩戴经验 > 0) {
                    expStatup.put(MapleExpStat.道具佩戴经验, 道具佩戴经验);
                }
                if (网吧经验 > 0) {
                    expStatup.put(MapleExpStat.网吧特别经验, 网吧经验);
                }
                if (精灵祝福 > 0) {
                    expStatup.put(MapleExpStat.精灵祝福经验, 精灵祝福);
                }
                if (召回戒指经验 > 0) {
                    expStatup.put(MapleExpStat.道具佩戴经验, 召回戒指经验);
                }
                client.announce(MaplePacketCreator.showGainExpFromMonster((int) gain, white, expStatup));
            }
        }
        killmonsterExps.add(gain);
    }

    public void forceReAddItem_NoUpdate(Item item, MapleInventoryType type) {
        getInventory(type).removeSlot(item.getPosition());
        getInventory(type).addFromDB(item);
    }

    public void forceReAddItem(Item item, MapleInventoryType type) {
        forceReAddItem_NoUpdate(item, type);
        if (type != MapleInventoryType.UNDEFINED) {
            client.announce(InventoryPacket.modifyInventory(false, Collections.singletonList(new ModifyInventory(0, item)))); //发送获得道具的封包
        }
    }

    public void forceUpdateItem(Item item) {
        forceUpdateItem(item, false);
    }

    public void petUpdateStats(MaplePet pet, boolean active) {
        List<ModifyInventory> mods = new LinkedList<>();
        Item Pet = getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition());
        mods.add(new ModifyInventory(3, Pet));
        mods.add(new ModifyInventory(0, Pet));
        client.announce(InventoryPacket.modifyInventory(false, mods, this, active));
    }

    public void forceUpdateItem(Item item, boolean updateTick) {
        List<ModifyInventory> mods = new LinkedList<>();
        mods.add(new ModifyInventory(3, item)); //删除道具
        mods.add(new ModifyInventory(0, item)); //获得道具
        client.announce(InventoryPacket.modifyInventory(updateTick, mods, this));
    }

    public void silentPartyUpdate() {
        if (party != null) {
            WorldPartyService.getInstance().updateParty(party.getPartyId(), PartyOperation.更新队伍, new MaplePartyCharacter(this));
        }
    }

    public boolean isSuperGM() {
        return gmLevel >= PlayerGMRank.SUPERGM.getLevel();
    }

    public boolean isIntern() {
        return gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isGM() {
        return gmLevel >= PlayerGMRank.GM.getLevel();
    }

    public boolean isAdmin() {
        return gmLevel >= PlayerGMRank.ADMIN.getLevel();
    }

    public int getGMLevel() {
        return gmLevel;
    }

    public boolean hasGmLevel(int level) {
        return gmLevel >= level;
    }

    public void setGmLevel(int level) {
        this.gmLevel = (byte) level;
        playerObservable.update();
    }

    public boolean isShowPacket() {
        return isAdmin() && ServerConstants.isShowPacket();
    }

    public MapleInventory getInventory(MapleInventoryType type) {
        return inventory[type.ordinal()];
    }

    public MapleInventory[] getInventorys() {
        return inventory;
    }

    /*
     * 检测角色道具是否到期的处理
     */
    public boolean canExpiration(long now) {
        return lastExpirationTime > 0 && lastExpirationTime + (60 * 1000) < now; //1分钟检测
    }

    public void expirationTask() {
        /*
         * 检测道具到期
         */
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)); //项链扩充任务
        long expiration;
        List<Integer> ret = new ArrayList<>();
        long currenttimes = System.currentTimeMillis();
        List<Triple<MapleInventoryType, Item, Boolean>> tobeRemoveItem = new ArrayList<>(); // 要删除道具的数组 道具类型 道具 是否删除.
        List<Item> tobeUnlockItem = new ArrayList<>(); // 解除道具锁定的数组.
        rLCheck.lock();
        try {
            for (MapleInventoryType inv : MapleInventoryType.values()) {
                MapleInventory inventory = getInventory(inv);
                for (Item item : inventory) {
                    expiration = item.getExpiration();
                    if ((expiration != -1 && !ItemConstants.isPet(item.getItemId()) && currenttimes > expiration) || ii.isLogoutExpire(item.getItemId())) {
                        if (ItemFlag.封印.check(item.getFlag())) {
                            tobeUnlockItem.add(item); //添加解除锁定的道具信息
                        } else if (currenttimes > expiration) {
                            tobeRemoveItem.add(new Triple<>(inv, item, true)); //添加删除的道具信息
                        }
                    } else if (item.getItemId() == 5000054 && item.getPet() != null && item.getPet().getSecondsLeft() <= 0) { //宠物道具信息
                        tobeRemoveItem.add(new Triple<>(inv, item, false));
                    } else if (item.getPosition() == -38) { //盛大项链扩充 T072修改 以前为 -37
                        if (stat == null || stat.getCustomData() == null || Long.parseLong(stat.getCustomData()) < currenttimes) { //项链扩充检测
                            tobeRemoveItem.add(new Triple<>(inv, item, false));
                        }
                    }
                }
            }
        } finally {
            rLCheck.unlock();
        }
        /*
         * Left = 左边
         * Mid = 中间
         * Right = 右边
         */
        for (Triple<MapleInventoryType, Item, Boolean> itemz : tobeRemoveItem) {
            Item item = itemz.getMid();
            if (item == null) {
                log.error("道具到期", getName() + " 检测道具已经过期，但道具为空，无法继续执行。");
                continue;
            }
            if (itemz.getRight()) { //删除道具
                if (MapleInventoryManipulator.removeFromSlot(client, itemz.getLeft(), item.getPosition(), item.getQuantity(), false)) {
                    ret.add(item.getItemId());
                }
                if (itemz.getLeft() == MapleInventoryType.EQUIPPED) {
                    equipChanged();
                }
            } else if (item.getPosition() == -38) { //盛大项链扩充 T072修改 以前为 -37
                short slot = getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
                if (slot > -1) {
                    MapleInventoryManipulator.unequip(client, item.getPosition(), slot);
                }
            }
        }
        for (Item itemz : tobeUnlockItem) {
            itemz.setExpiration(-1);
            //itemz.setFlag((byte) (itemz.getFlag() - ItemFlag.封印.getValue()));
            forceUpdateItem(itemz);
            //dropMessage(6, "封印道具[" + ii.getName(itemz.getItem()) + "]封印时间已过期。");
        }
        this.pendingExpiration = ret;
        /*
         * 技能到期
         */
        List<Integer> tobeRemoveSkill = new ArrayList<>();
        Map<Integer, SkillEntry> tobeRemoveList = new HashMap<>();
        for (Entry<Integer, SkillEntry> skil : skills.entrySet()) {
            if (skil.getValue().expiration != -1 && currenttimes > skil.getValue().expiration && !SkillConstants.isLinkSkills(skil.getKey()) && !SkillConstants.isTeachSkills(skil.getKey())) {
                tobeRemoveSkill.add(skil.getKey());
            }
        }
        for (Integer skil : tobeRemoveSkill) {
            tobeRemoveList.put(skil, new SkillEntry(0, (byte) 0, -1));
            this.skills.remove(skil);
            changed_skills = true;
        }
        this.pendingSkills = tobeRemoveList;
        if (stat != null && stat.getCustomData() != null && Long.parseLong(stat.getCustomData()) < currenttimes) { //expired bro
            quests.remove(7830);
            quests.remove(GameConstants.PENDANT_SLOT);
            client.announce(MaplePacketCreator.pendantSlot(false)); //发送项链扩充到期
        }
        /*
         * 检测宝盒属性是否到期重置
         */
        if (coreAura != null && currenttimes > coreAura.getExpiration()) {
            coreAura.resetCoreAura();
            coreAura.saveToDb(null);
            updataCoreAura();
            dropMessage(5, "宝盒属性时间到期，属性已经重置。");
        }
        /*
         * 检测项链佩戴位置错误的设置
         */
        Item itemFix = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -37); //以前的项链扩充 T072修改到 -38的位置
        if (itemFix != null && itemFix.getItemId() / 10000 != 119) {
            short slot = getInventory(MapleInventoryType.EQUIP).getNextFreeSlot();
            if (slot > -1) {
                MapleInventoryManipulator.unequip(client, itemFix.getPosition(), slot);
                dropMessage(5, "装备道具[" + ii.getName(itemFix.getItemId()) + "]由于装备的位置错误已自动取下。");
            }
        }
        /*
         * 检测Vip 会员是否到期
         */
        Timestamp currentVipTime = new Timestamp(System.currentTimeMillis());
        if (getVip() != 0) {
            Timestamp expirationVip = getViptime();
            if (expirationVip != null && currentVipTime.after(expirationVip)) {
                setVip(0);
                setViptime(null);
                dropMessage(-11, "您的Vip已经到期，当前Vip等级为 " + getVip());
            } else if (expirationVip == null) {
                setVip(0);
                setViptime(null);
            }
        }
        /*
         * 发送到期封包
         */
        if (!pendingExpiration.isEmpty()) {
            for (Integer itemId : pendingExpiration) { //发送道具到期封包
                if (ii.isCash(itemId)) {
                    client.announce(MaplePacketCreator.showCashItemExpired(itemId));
                } else {
                    client.announce(MaplePacketCreator.showItemExpired(itemId));
                }
            }
        }
        pendingExpiration = null;
        /*
         * 发送技能到期封包
         */
        if (!pendingSkills.isEmpty()) {
            client.announce(MaplePacketCreator.updateSkills(pendingSkills)); //发送删除技能的封包
            client.announce(MaplePacketCreator.showSkillExpired(pendingSkills)); //发送技能到期提示
        }
        pendingSkills = null;
        lastExpirationTime = System.currentTimeMillis();
    }

    public MapleShop getShop() {
        return shop;
    }

    public void setShop(MapleShop shop) {
        this.shop = shop;
    }

    /*
     * 角色传送石处理
     */
    public int[] getSavedLocations() {
        return savedLocations;
    }

    public int getSavedLocation(SavedLocationType type) {
        return savedLocations[type.getValue()];
    }

    public void saveLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = getMapId();
        changed_savedlocations = true;
    }

    public void saveLocation(SavedLocationType type, int mapz) {
        savedLocations[type.getValue()] = mapz;
        changed_savedlocations = true;
    }

    public void clearSavedLocation(SavedLocationType type) {
        savedLocations[type.getValue()] = -1;
        changed_savedlocations = true;
    }

    public int getCMapCount() {
        return cMapCount.getAndIncrement();
    }

    /*
     * V.110修改金币上限
     * 暂时不作修改
     */
    public long getMeso() {
        return meso.get();
    }

    public void gainMeso(long gain, boolean show) {
        gainMeso(gain, show, false);
    }

    public void gainMeso(long gain, boolean show, boolean inChat) {
        if (meso.get() + gain < 0) {
            client.announce(MaplePacketCreator.enableActions());
            return;
        } else if (meso.get() + gain > ServerConfig.CHANNEL_PLAYER_MAXMESO) {
            gain = ServerConfig.CHANNEL_PLAYER_MAXMESO - meso.get();
        }
        meso.addAndGet(gain);
        if (meso.get() >= 1000000) {
            finishAchievement(31);
        }
        if (meso.get() >= 10000000) {
            finishAchievement(32);
        }
        if (meso.get() >= 100000000) {
            finishAchievement(33);
        }
        if (meso.get() >= 1000000000) {
            finishAchievement(34);
        }
        updateSingleStat(MapleStat.金币, meso.get(), false);
        client.announce(MaplePacketCreator.enableActions());
        if (show) {
            client.announce(MaplePacketCreator.showMesoGain(gain, inChat));
        }
        playerObservable.update();
    }

    public int getAccountID() {
        return accountid;
    }

    /*
     * 怪物仇人目标处理
     */
    public void controlMonster(MapleMonster monster, boolean aggro, int type) {
        if (monster == null || !monster.isAlive()) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            monster.setController(this);
            controlled.add(monster);
            client.announce(MobPacket.controlMonster(monster, false, aggro, type));
//            monster.sendStatus(client);
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void stopControllingMonster(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        controlledLock.writeLock().lock();
        try {
            monster.setControllerHasAggro(false);
            monster.setControllerKnowsAboutAggro(false);
            monster.setController(null);
            if (controlled.contains(monster)) {
                controlled.remove(monster);
            }
            if (monster.getMap().getId() == this.getMapId()) {
                this.client.announce(MobPacket.stopControllingMonster(monster.getObjectId()));
            }
        } finally {
            controlledLock.writeLock().unlock();
        }
    }

    public void checkMonsterAggro(MapleMonster monster) {
        if (monster == null) {
            return;
        }
        if (monster.getController() == this) {
            monster.setControllerHasAggro(true);
        } else {
            monster.switchController(this, true);
        }
    }

    public int getControlledSize() {
        return controlled.size();
    }

    /*
     * 任务相关操作
     */
    public List<MapleQuestStatus> getStartedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 1 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public List<MapleQuestStatus> getCompletedQuests() {
        List<MapleQuestStatus> ret = new LinkedList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked()) {
                ret.add(q);
            }
        }
        return ret;
    }

    public List<Pair<Integer, Long>> getCompletedMedals() {
        List<Pair<Integer, Long>> ret = new ArrayList<>();
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() == 2 && !q.isCustom() && !q.getQuest().isBlocked() && q.getQuest().getMedalItem() > 0 && ItemConstants.getInventoryType(q.getQuest().getMedalItem()) == MapleInventoryType.EQUIP) {
                ret.add(new Pair<>(q.getQuest().getId(), q.getCompletionTime()));
            }
        }
        return ret;
    }

    public void mobKilled(int id, int skillID) {
        for (MapleQuestStatus q : quests.values()) {
            if (q.getStatus() != 1 || !q.hasMobKills()) {
                continue;
            }
            if (q.mobKilled(id, skillID)) {
                client.announce(MaplePacketCreator.updateQuestMobKills(q));
                if (q.getQuest().canComplete(this, null)) {
                    client.announce(MaplePacketCreator.getShowQuestCompletion(q.getQuest().getId()));
                }
            }
        }
    }

    /**
     * 技能相关的操作
     */
    public Map<Integer, SkillEntry> getSkills() {
        return Collections.unmodifiableMap(skills);
    }

    public Map<Integer, SkillEntry> getLinkSkills() {
        return Collections.unmodifiableMap(linkSkills);
    }

    public Map<Integer, Pair<Integer, SkillEntry>> getSonOfLinkedSkills() {
        return Collections.unmodifiableMap(sonOfLinkedSkills);
    }

    /*
     * 获取角色所有技能
     * 如果不是返回所有技能
     * 如果是封包发送就返回删除有些需要过滤的技能
     */
    public Map<Integer, SkillEntry> getSkills(boolean packet) {
        if (!packet || JobConstants.is林之灵(job)) {
            return Collections.unmodifiableMap(skills);
        }
        Map<Integer, SkillEntry> oldlist = new LinkedHashMap<>(skills);
        Map<Integer, SkillEntry> newlist = new LinkedHashMap<>();
        for (Entry<Integer, SkillEntry> skill : oldlist.entrySet()) {
            //去掉天使技能和特殊连接技能
            Skill skill1 = SkillFactory.getSkill(skill.getKey());
            if (skill1 != null && !skill1.isAngelSkill() && !skill1.isLinkedAttackSkill() && !skill1.isDefaultSkill()) {
                newlist.put(skill.getKey(), skill.getValue());
            }
        }
        return newlist;
    }

    public int getAllSkillLevels() {
        int rett = 0;
        for (Entry<Integer, SkillEntry> ret : skills.entrySet()) {
            Skill skill = SkillFactory.getSkill(ret.getKey());
            if (!skill.isBeginnerSkill() && !skill.isSpecialSkill() && ret.getValue().skillevel > 0) {
                rett += ret.getValue().skillevel;
            }
        }
        return rett;
    }

    public long getSkillExpiry(Skill skill) {
        if (skill == null) {
            return 0;
        }
        SkillEntry ret = skills.get(skill.getId());
        if (ret == null || ret.skillevel <= 0) {
            return 0;
        }
        return ret.expiration;
    }

    /*
     * 通过技能ID获取技能等级
     */
    public int getSkillLevel(int skillid) {
        return getSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getSkillLevel(Skill skill) {
        if (skill == null) {
            return 0;
        }
        int skillLevel;
        if (getJob() >= skill.getId() / 10000 && getJob() < skill.getId() / 10000 + 3) {
            skillLevel = skill.getFixLevel();
        } else {
            skillLevel = 0;
        }
        SkillEntry ret = this.skills.get(skill.getId());
        if (ret == null || ret.skillevel <= 0) {
            return skillLevel;
        } else {
            skillLevel += ret.skillevel;
        }
        return skillLevel;
    }

    /*
     * 通过技能ID获取技能等级
     */
    public int getTotalSkillLevel(int skillid) {
        return getTotalSkillLevel(SkillFactory.getSkill(skillid));
    }

    public int getTotalSkillLevel(Skill skill) {
        if (skill == null) {
            return 0;
        }
        if (SkillConstants.isGeneralSkill(skill.getId()) || SkillConstants.isExtraSkill(skill.getId()) || skill.isSoulSkill() || SkillConstants.isRuneSkill(skill.getId())) {
            return 1;
        }
        switch (skill.getId()) {
            case 双弩.元素幽灵_1:
            case 双弩.元素幽灵_2: {
                return this.getTotalSkillLevel(双弩.精灵元素);
            }
            case 400011013:
            case 400011014: {
                return this.getTotalSkillLevel(400011012);
            }
        }
        int skillLevel;
        if (getJob() >= skill.getId() / 10000 && getJob() < skill.getId() / 10000 + 3) {
            skillLevel = skill.getFixLevel();
        } else {
            skillLevel = 0;
        }
        SkillEntry ret = this.skills.get(skill.getId());
        if (skill.isLinkSkills()) {
            ret = linkSkills.get(skill.getId());
        }
        if (ret == null || ret.skillevel <= 0) {
            return skillLevel;
        } else {
            skillLevel += ret.skillevel;
        }
        return Math.min(skill.getTrueMax(), skillLevel + (skill.isBeginnerSkill() ? 0 : (stats.combatOrders + (skill.getMaxLevel() > 10 ? stats.incAllskill : 0) + stats.getSkillIncrement(skill.getId()))));
    }

    public byte getMasterLevel(int skillId) {
        return getMasterLevel(SkillFactory.getSkill(skillId));
    }

    public byte getMasterLevel(Skill skill) {
        SkillEntry ret = skills.get(skill.getId());
        if (ret == null) {
            return 0;
        }
        return ret.masterlevel;
    }

    public int getSkillTeachId(int skillId) {
        return getSkillTeachId(SkillFactory.getSkill(skillId));
    }

    public int getSkillTeachId(Skill skill) {
        if (skill == null) {
            return 0;
        }
        SkillEntry ret = skills.get(skill.getId());
        if (ret == null || ret.teachId == 0) {
            return 0;
        }
        return ret.teachId;
    }

    public byte getSkillPosition(int skillId) {
        return getSkillPosition(SkillFactory.getSkill(skillId));
    }

    public byte getSkillPosition(Skill skill) {
        if (skill == null) {
            return -1;
        }
        SkillEntry ret = skills.get(skill.getId());
        if (ret == null || ret.position == -1) {
            return -1;
        }
        return ret.position;
    }

    public SkillEntry getSkillEntry(int skillId) {
        return skills.get(skillId);
    }

    /*
     * 角色升级处理
     */
    public void levelUp() {
        levelUp(false);
    }

    public void levelUp(boolean takeexp) {
//        int vipAp = getVip() > 1 ? getVip() - 1 : 0; //暂时不开放Vip加Ap
        if (!canLevelUp()) {
            return;
        }
        if (JobConstants.is炎术士(job) || JobConstants.is夜行者(job)) {
            if (level <= 70) {
                remainingAp += 6;
            } else {
                remainingAp += 5;
            }
        } else {
            remainingAp += 5;
        }
        int maxhp = stats.getMaxHp();
        int maxmp = stats.getMaxMp();

        if (JobConstants.is新手职业(job)) { // 新手
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(10, 12);
        } else if (JobConstants.is恶魔猎手(job) || JobConstants.is超能力者(job)) { // 恶魔猎手
            maxhp += Randomizer.rand(48, 52);
        } else if (JobConstants.is恶魔复仇者(job) || JobConstants.is阴阳师(job)) { // 恶魔复仇者
            maxhp += Randomizer.rand(30, 40);
        } else if ((job >= 100 && job <= 132) // 战士
                || (job >= 1100 && job <= 1112) // 魂骑士
                || (job >= 5100 && job <= 5112) // 米哈尔
                || JobConstants.is品克缤(job)) { //品克缤
            maxhp += Randomizer.rand(48, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if ((job >= 200 && job <= 232) // 魔法师
                || (job >= 1200 && job <= 1212) // 炎术士
                || (job >= 2700 && job <= 2712)) { //夜光法师
            maxhp += Randomizer.rand(10, 14);
            maxmp += Randomizer.rand(48, 52);
        } else if (job >= 3200 && job <= 3212) { //幻灵斗师
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(42, 44);
        } else if ((job >= 300 && job <= 322) // 弓箭手
                || (job >= 400 && job <= 434) // 飞侠
                || (job >= 1300 && job <= 1312) //风灵使者
                || (job >= 1400 && job <= 1412) //夜行者
                || (job >= 2300 && job <= 2312) //双弩精灵
                || (job >= 2400 && job <= 2412) //幻影
                || (job >= 3300 && job <= 3312) //弩豹游侠
                || (job >= 3600 && job <= 3612)) { //尖兵
            maxhp += Randomizer.rand(20, 24);
            maxmp += Randomizer.rand(14, 16);
        } else if ((job >= 510 && job <= 512) // 拳手
                || (job >= 580 && job <= 582) // 拳手 - 新
                || (job >= 1510 && job <= 1512) //奇袭者
                || (job >= 6500 && job <= 6512)) { //爆莉萌天使
            maxhp += Randomizer.rand(37, 41);
            maxmp += Randomizer.rand(18, 22);
        } else if ((job >= 500 && job <= 532) //火炮手
                || (job >= 570 && job <= 572) || job == 508 //龙的传人
                || (job >= 590 && job <= 592) //火炮手 - 新
                || (job >= 3500 && job <= 3512) //机械师
                || job == 1500) { // 奇袭者
            maxhp += Randomizer.rand(22, 26);
            maxmp += Randomizer.rand(18, 22);
        } else if (job >= 2100 && job <= 2112) { // 战神
            maxhp += Randomizer.rand(50, 52);
            maxmp += Randomizer.rand(4, 6);
        } else if (JobConstants.is龙神(job)) { // 龙神
            maxhp += Randomizer.rand(12, 16);
            maxmp += Randomizer.rand(50, 52);
        } else if (job >= 6100 && job <= 6112) { // 狂龙战士
            maxhp += Randomizer.rand(68, 74);
            maxmp += Randomizer.rand(4, 6);
        } else if (job >= 10100 && job <= 10112) { // 神之子
            maxhp += Randomizer.rand(48, 52);
        } else if (JobConstants.is林之灵(job) || JobConstants.is隐月(job) || JobConstants.is剑豪(job)) {
            maxhp += Randomizer.rand(38, 42);
            maxmp += Randomizer.rand(20, 24);
        } else if (JobConstants.is爆破手(job)) {
            maxhp += Randomizer.rand(48, 75);
            maxmp += Randomizer.rand(8, 18);
        } else { // 默认没有写的职业加血
            maxhp += Randomizer.rand(24, 38);
            maxmp += Randomizer.rand(12, 24);
            if (job != 800 && job != 900 && job != 910) {
                System.err.println("出现未处理的角色升级加血职业: " + job);
            }
        }
        maxmp += JobConstants.isNotMpJob(getJob()) ? 0 : stats.getTotalInt() / 10;
        if (JobConstants.is夜光(job) && getSkillLevel(夜光.光之力量) > 0) {
            maxmp += Randomizer.rand(18, 22);
        }
        if (takeexp) {
            exp.addAndGet(getExpNeededForLevel());
            if (exp.get() < 0) {
                exp.set(0);
            }
        } else {
            setExp(0); // 升级后角色经验设置为0
        }
        level += 1;
        if (level >= getMaxLevelForSever()) {
            setExp(0);
        }
        maxhp = Math.min(getMaxHpForSever(), Math.abs(maxhp));
        maxmp = Math.min(getMaxMpForSever(), Math.abs(maxmp));
        if (JobConstants.is恶魔猎手(job)) {
            maxmp = GameConstants.getMPByJob(job);
        } else if (JobConstants.is神之子(job)) {
            maxmp = 100;
            checkZeroWeapon();
        } else if (JobConstants.isNotMpJob(job)) {
            maxmp = 10;
        }
        if (level == 30) {
            finishAchievement(2);
        }
        if (level == 70) {
            finishAchievement(3);
        }
        if (level == 120) {
            finishAchievement(4);
        }
        if (level == 200) {
            finishAchievement(5);
        }
        if (level == 250 && !isGM()) {
            String sb = "[祝贺] " + getMedalText() + //当前角色勋章的名字
                    getName() + //当前角色的名字
                    "终于达到了250级.大家一起祝贺下吧。";
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, sb));
        }
        List<Pair<MapleStat, Long>> statup = new ArrayList<>();

        if (!JobConstants.is新手职业(job)) {
            if (JobConstants.is神之子(job) && level >= 100) {
                remainingSp[0] += 3;
                remainingSp[1] += 3;
            } else if (!JobConstants.is品克缤(job)) {
                remainingSp[JobConstants.getSkillBookByLevel(this.job, this.level)] += 3;
            }
        } else if (level < 10) {
            stats.str += remainingAp;
            remainingAp = 0;
            statup.add(new Pair<>(MapleStat.力量, (long) stats.getStr()));
        } else if (level == 10) {
            resetStats(4, 4, 4, 4);
        }
        stats.setInfo(maxhp, maxmp, stats.getCurrentMaxHp(), stats.getCurrentMaxMp(job));
        characterCard.recalcLocalStats(this);
        stats.recalcLocalStats(this);
        statup.add(new Pair<>(MapleStat.MAXHP, (long) maxhp));
        statup.add(new Pair<>(MapleStat.MAXMP, (long) maxmp));
        statup.add(new Pair<>(MapleStat.HP, (long) stats.getCurrentMaxHp()));
        statup.add(new Pair<>(MapleStat.MP, (long) stats.getCurrentMaxMp(job)));
        statup.add(new Pair<>(MapleStat.经验, exp.get()));
        statup.add(new Pair<>(MapleStat.等级, (long) level));
        statup.add(new Pair<>(MapleStat.AVAILABLEAP, (long) remainingAp));
        statup.add(new Pair<>(MapleStat.AVAILABLESP, (long) remainingSp[JobConstants.getSkillBookByLevel(job, level)]));
        client.announce(MaplePacketCreator.updatePlayerStats(statup, this));
        map.broadcastMessage(this, EffectPacket.showForeignEffect(getId(), 0), false);
        silentPartyUpdate();
        guildUpdate();
        familyUpdate();
        baseSkills();
        changeTeachSkillsLevel();
        checkBeastTamerSkill();

        if (JobConstants.is龙的传人(getJob()) && getSkillLevel(龙的传人.宝盒的庇佑) > 0) {
            if (coreAura == null) {
                coreAura = MapleCoreAura.createCoreAura(id, level);
            } else {
                coreAura.setLevel(level);
            }
//            updataCoreAura();
        }
        //if (map.getForceMove() > 0 && map.getForceMove() <= getLevel()) {
        //    changeMap(map.getReturnMap(), map.getReturnMap().getPortal(0));
        //    dropMessage(-1, "You have been expelled from the map.");
        //}

        //链接技能
        int tecahskill = JobConstants.getTecahSkillID(job);
        if (tecahskill != -1) {
            if (level >= 70 && level < 120) {
                if (!hasSkill(skills, tecahskill, 1)) {
                    changeSingleSkillLevel(SkillFactory.getSkill(tecahskill), (byte) 1, (byte) 1);
                }
            } else if (level >= 120 && level < 210) {
                if (!hasSkill(skills, tecahskill, 2)) {
                    changeSingleSkillLevel(SkillFactory.getSkill(tecahskill), (byte) 2, (byte) 2);
                }
            } else if (level >= 210) {
                if (!hasSkill(skills, tecahskill, 3)) {
                    changeSingleSkillLevel(SkillFactory.getSkill(tecahskill), (byte) 3, (byte) 3);
                }
            }
        }
        insertRanking("等级排行", level);
        playerObservable.update();
    }

    public boolean canLevelUp() {
        boolean canLevelUp = true;
        String textinfo = "";
        switch (getJob()) {
            case 0:
            case 1000: // 骑士团0转
            case 2000: // 狂狼勇士0转
            case 2001: // 龙魔导师0转
            case 3000: // 末日反抗者0转
            case 2002: // 精灵游侠0转
            case 3001: // 恶魔杀手0转
            case 3002: // 尖兵
            case 6000: // 狂龙战士
            case 6001: // 天使
            case 5000: // 米哈尔
            case 2003:
            case 2004:
            case 2005: // 隐月
            case 11000:// 林之灵
            case 4001:
            case 4002:
            case 14000: //超能力者
                if (getLevel() >= 10) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第一次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            // 冒险家1转
            case 100:
            case 200:
            case 300:
            case 500:
                // 骑士团1转
            case 1100:
            case 1200:
            case 1300:
            case 1400:
            case 1500:
            case 2100: // 狂狼勇士1转
            case 2200: // 龙魔导师1转
            case 2300: // 精灵游侠1转
            case 3200: // 炼狱巫师1转
            case 3300: // 狂豹猎人1转
            case 3500: // 机甲战神1转
            case 3600:
            case 3700:
            case 501:  // 火炮手1转
            case 3100: // 恶魔猎手1转
            case 3101: // 恶魔复仇者1转
            case 6100:
            case 6500:
            case 5100:
            case 508://龙的传人
            case 2700:
            case 2400:
            case 2500: // 隐月1转
            case 4100:
            case 4200:
            case 14200: //超能力者1转
            {
                if (getLevel() >= 30) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第二次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            // 冒险家2转
            case 110:
            case 120:
            case 130:
            case 210:
            case 220:
            case 230:
            case 310:
            case 320:
            case 410:
            case 420:
            case 510:
            case 520:
                // 骑士团2转
            case 1110:
            case 1210:
            case 1310:
            case 1410:
            case 1510:
            case 2110: // 狂狼勇士2转
            case 2211: // 龙魔导师2转
            case 3210: // 炼狱巫师2转
            case 3310: // 狂豹猎人2转
            case 3510: // 机甲战神2转
            case 3610:
            case 3710:
            case 530:  // 火炮手2转
            case 2310: // 精灵游侠2转
            case 3110: // 恶魔杀手2转
            case 3120:
            case 6510:
            case 6110:
            case 5110:
            case 570:
            case 2710:
            case 2410:
            case 2510: // 隐月2转
            case 4110:
            case 4210:
            case 14210: //超能力者2转
            {
                if (getLevel() >= 60) {
                    canLevelUp = false;
                    //textinfo = "您现在可以进行第三次转职了，去冰原雪域的长老公馆找到三转教官进行转职吧。";
                    textinfo = "您现在可以进行第三次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            // 冒险家3转
            case 111:
            case 121:
            case 131:
            case 211:
            case 221:
            case 231:
            case 311:
            case 321:
            case 411:
            case 421:
            case 511:
            case 521:
                // 骑士团3转
            case 1111:
            case 1211:
            case 1311:
            case 1411:
            case 1511:
            case 2111: // 狂狼勇士3转
            case 2214: // 龙魔导师3转
            case 3211: // 炼狱巫师3转
            case 3311: // 狂豹猎人3转
            case 3511: // 机甲战神3转
            case 3711:
            case 531:  // 火炮手3转
            case 2311: // 精灵游侠3转
            case 3111: // 恶魔杀手3转
            case 3121:
            case 6111:
            case 6511:
            case 5111:
            case 3611:
            case 571:
            case 2711:
            case 2411:
            case 2511: // 隐月3转
            case 4111:
            case 4211:
            case 14211: //超能力者3转
            {
                if (getLevel() >= 100) {
                    canLevelUp = false;
                    //textinfo = "您现在可以进行第四次转职了，去神木村祭祀之林找到四转教官进行转职吧。";
                    textinfo = "您现在可以进行第四次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            //双刀
            case 400: {
                //如果又双刀的职业群创建的话
                if (getSubcategory() == 1) {
                    if (getLevel() >= 20) {
                        canLevelUp = false;
                        textinfo = "您现在可以进行第二次转职了，在右下角点击拍卖开始转职吧。";
                    }
                } else if (getLevel() >= 30) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第二次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            case 430: {
                if (getLevel() >= 30) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第三次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            case 431: {
                if (getLevel() >= 45) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第四次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            case 432: {
                if (getLevel() >= 60) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第五次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
            case 433: {
                if (getLevel() >= 100) {
                    canLevelUp = false;
                    textinfo = "您现在可以进行第六次转职了，在右下角点击拍卖开始转职吧。";
                }
                break;
            }
        }
        if (!canLevelUp) {
            dropMessage(5, "[转职提示] " + textinfo); // 请点击拍卖按钮进行转职。
            return false;
        }
        return true;
    }

    /*
     * 键盘设置处理
     */
    public void changeKeybinding(int key, byte type, int action) {
        if (type != 0) {
            keylayout.Layout().put(key, new Pair<>(type, action));
        } else {
            keylayout.Layout().remove(key);
        }
    }

    /*
     * 角色技能宏处理
     */
    public void sendMacros() {
        client.announce(MaplePacketCreator.getMacros(skillMacros));
    }

    public void updateMacros(int position, SkillMacro updateMacro) {
        skillMacros[position] = updateMacro;
        changed_skillmacros = true;
    }

    public SkillMacro[] getMacros() {
        return skillMacros;
    }

    /*
     * 按时间来封号
     */
    public void tempban(String reason, Calendar duration, int greason, boolean IPMac) {
        if (IPMac) {
            client.banMacs();
        }

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps;
            if (IPMac) {
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();
            }
            client.disconnect(true, false);
            client.getSession().close();
            ps = con.prepareStatement("UPDATE accounts SET tempban = ?, banreason = ?, greason = ? WHERE id = ?");
            Timestamp TS = new Timestamp(duration.getTimeInMillis());
            ps.setTimestamp(1, TS);
            ps.setString(2, reason);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.execute();
            ps.close();
        } catch (SQLException ex) {
            System.err.println("Error while tempbanning" + ex);
        }
    }

    /*
     * 封停帐号
     */
    public boolean ban(String reason, boolean IPMac, boolean autoban, boolean hellban) {
        gainWarning(false);

        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE id = ?");
            ps.setInt(1, autoban ? 2 : 1);
            ps.setString(2, reason);
            ps.setInt(3, accountid);
            ps.execute();
            ps.close();
            if (IPMac) {
                client.banMacs();
                ps = con.prepareStatement("INSERT INTO ipbans VALUES (DEFAULT, ?)");
                ps.setString(1, client.getSessionIPAddress());
                ps.execute();
                ps.close();
                if (hellban) {
                    PreparedStatement psa = con.prepareStatement("SELECT * FROM accounts WHERE id = ?");
                    psa.setInt(1, accountid);
                    ResultSet rsa = psa.executeQuery();
                    if (rsa.next()) {
                        PreparedStatement pss = con.prepareStatement("UPDATE accounts SET banned = ?, banreason = ? WHERE email = ? OR SessionIP = ?");
                        pss.setInt(1, autoban ? 2 : 1);
                        pss.setString(2, reason);
                        pss.setString(3, rsa.getString("email"));
                        pss.setString(4, client.getSessionIPAddress());
                        pss.execute();
                        pss.close();
                    }
                    rsa.close();
                    psa.close();
                }
            }
        } catch (SQLException ex) {
            System.err.println("Error while banning" + ex);
            return false;
        }
        client.disconnect(true, false);
        client.getSession().close();
        return true;
    }

    /**
     * 对象ID，角色的对象ID总是等于CID
     *
     * @return 对象ID
     */
    @Override
    public int getObjectId() {
        return getId();
    }

    /**
     * 抛出不支持的操作异常，玩家OID为只读
     *
     * @param id
     */
    @Override
    public void setObjectId(int id) {
        throw new UnsupportedOperationException();
    }

    public MapleStorage getStorage() {
        return storage;
    }

    /**
     * 添加可见的地图对象
     *
     * @param mo
     */
    public void addVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.add(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    /**
     * 移除可见的地图对象
     *
     * @param mo
     */
    public void removeVisibleMapObject(MapleMapObject mo) {
        visibleMapObjectsLock.writeLock().lock();
        try {
            visibleMapObjects.remove(mo);
        } finally {
            visibleMapObjectsLock.writeLock().unlock();
        }
    }

    /**
     * 是否为可见的地图对象
     *
     * @param mo 地图对象
     * @return true = 可见的对象 : false = 不可见的对象
     */
    public boolean isMapObjectVisible(MapleMapObject mo) {
        visibleMapObjectsLock.readLock().lock();
        try {
            return visibleMapObjects.contains(mo);
        } finally {
            visibleMapObjectsLock.readLock().unlock();
        }
    }

    public Collection<MapleMapObject> getAndWriteLockVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().lock();
        return visibleMapObjects;
    }

    public void unlockWriteVisibleMapObjects() {
        visibleMapObjectsLock.writeLock().unlock();
    }

    public boolean isAlive() {
        return stats.getHp() > 0;
    }

    /**
     * 作用：发送角色离开地图的数据包
     *
     * @param client 客户端
     */
    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removePlayerFromMap(this.getObjectId()));
    }

    /**
     * 作用：发送召唤角色相关的数据包
     *
     * @param client 客户端
     */
    @Override
    public void sendSpawnData(MapleClient client) {
        if (client.getPlayer().allowedToTarget(this)) {
            //显示玩家
            client.announce(MaplePacketCreator.spawnPlayerMapobject(this));
            client.announce(EffectPacket.getEffectSwitch(getId(), getEffectSwitch()));
            //刷新队员HP
            if (getParty() != null) {
                updatePartyMemberHP();
                receivePartyMemberHP();
            }
            //显示龙龙
            if (dragon != null) {
                client.announce(SummonPacket.spawnDragon(dragon));
            }
            if (lw != null) {
                client.announce(SummonPacket.spawnLittleWhite(lw));
            }
            //显示安卓
            if (android != null) {
                client.announce(AndroidPacket.spawnAndroid(this, android));
            }
            if (summons != null && summons.size() > 0) {
                summonsLock.readLock().lock();
                try {
                    for (MapleSummon summon : summons) {
                        if (summon.getOwner() != null) {
                            client.announce(SummonPacket.spawnSummon(summon, false));
                        }
                    }
                } finally {
                    summonsLock.readLock().unlock();
                }
            }
            if (followid > 0 && followon) {
                client.announce(MaplePacketCreator.followEffect(followinitiator ? followid : id, followinitiator ? id : followid, null));
            }
        }
    }

    public void equipChanged() {
        if (map == null) {
            return;
        }
        map.broadcastMessage(this, MaplePacketCreator.updateCharLook(this), false);
        stats.recalcLocalStats(this);
        if (getMessenger() != null) {
            WorldMessengerService.getInstance().updateMessenger(getMessenger().getId(), getName(), client.getChannel());
        }
    }

    /*
     * 检测角色背包和身上的复制装备信息
     * 如果发现直接删除装备
     */
    public void checkCopyItems() {
        //检测复制的装备
        List<Integer> equipOnlyIds = new ArrayList<>(); //[道具的唯一ID信息]
        Map<Integer, Integer> checkItems = new HashMap<>(); //[道具唯一ID 道具ID]
        //检测背包的装备信息
        for (Item item : getInventory(MapleInventoryType.EQUIP).list()) {
            int equipOnlyId = item.getEquipOnlyId();
            if (equipOnlyId > 0) {
                if (checkItems.containsKey(equipOnlyId)) { //发现重复的唯一ID装备
                    if (checkItems.get(equipOnlyId) == item.getItemId()) {
                        equipOnlyIds.add(equipOnlyId);
                    }
                } else {
                    checkItems.put(equipOnlyId, item.getItemId());
                }
            }
        }
        //检测身上的装备
        for (Item item : getInventory(MapleInventoryType.EQUIPPED).list()) {
            int equipOnlyId = item.getEquipOnlyId();
            if (equipOnlyId > 0) {
                if (checkItems.containsKey(equipOnlyId)) { //发现重复的唯一ID装备
                    if (checkItems.get(equipOnlyId) == item.getItemId()) {
                        equipOnlyIds.add(equipOnlyId);
                    }
                } else {
                    checkItems.put(equipOnlyId, item.getItemId());
                }
            }
        }
        //如果重复的唯一ID数量大于0
        boolean autoban = false;
        for (Integer equipOnlyId : equipOnlyIds) {
            MapleInventoryManipulator.removeAllByEquipOnlyId(client, equipOnlyId);
            autoban = true;
        }
        if (autoban) {
            AutobanManager.getInstance().autoban(client, "无理由.");
        }
        checkItems.clear();
        equipOnlyIds.clear();
    }

    /*
     * 角色所有宠物的信息
     */
    public List<MaplePet> getPets() {
        List<MaplePet> ret = new ArrayList<>();
        for (Item item : getInventory(MapleInventoryType.CASH).newList()) {
            if (item.getPet() != null) {
                ret.add(item.getPet());
            }
        }
        return ret;
    }

    /*
     * 角色召唤中的宠物的信息
     */
    public MaplePet[] getSpawnPets() {
        return spawnPets;
    }

    public MaplePet getSpawnPet(int index) {
        return spawnPets[index];
    }

    public byte getPetIndex(int petId) {
        for (byte i = 0; i < 3; i++) {
            if (spawnPets[i] != null && spawnPets[i].getUniqueId() == petId) {
                return i;
            }
        }
        return -1;
    }

    public byte getPetIndex(MaplePet pet) {
        for (byte i = 0; i < 3; i++) {
            if (spawnPets[i] != null && spawnPets[i].getUniqueId() == pet.getUniqueId()) {
                return i;
            }
        }
        return -1;
    }

    public byte getPetByItemId(int petItemId) {
        for (byte i = 0; i < 3; i++) {
            if (spawnPets[i] != null && spawnPets[i].getPetItemId() == petItemId) {
                return i;
            }
        }
        return -1;
    }

    public int getNextEmptyPetIndex() {
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] == null) {
                return i;
            }
        }
        return 3;
    }

    public int getNoPets() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public List<MaplePet> getSummonedPets() {
        List<MaplePet> ret = new ArrayList<>();
        for (byte i = 0; i < 3; i++) {
            if (spawnPets[i] != null && spawnPets[i].getSummoned()) {
                ret.add(spawnPets[i]);
            }
        }
        return ret;
    }

    public void addSpawnPet(MaplePet pet) {
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] == null) {
                spawnPets[i] = pet;
                pet.setSummoned((byte) (i + 1));
                return;
            }
        }
    }

    public void removeSpawnPet(MaplePet pet, boolean shiftLeft) {
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] != null) {
                if (spawnPets[i].getUniqueId() == pet.getUniqueId()) {
                    spawnPets[i] = null;
                    break;
                }
            }
        }
    }

    public void unequipAllSpawnPets() {
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] != null) {
                unequipSpawnPet(spawnPets[i], true, false);
            }
        }
    }

    public void spawnPet(byte slot) {
        spawnPet(slot, false, true);
    }

    public void spawnPet(byte slot, boolean lead) {
        spawnPet(slot, lead, true);
    }

    /**
     * 召唤宠物
     *
     * @param slot      宠物在背包中的位置
     * @param lead      主宠物，第一个
     * @param broadcast 地图发送当前数据包
     */
    public void spawnPet(byte slot, boolean lead, boolean broadcast) {
        Item item = getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || !ItemConstants.isPet(item.getItemId())) {
            client.announce(MaplePacketCreator.enableActions());
            return;
        }
        switch (item.getItemId()) {
            case 5000047:   //罗伯
            case 5000028: { //进化龙
                MaplePet pet = MaplePet.createPet(item.getItemId() + 1, MapleInventoryIdentifier.getInstance());
                if (pet != null) {
                    MapleInventoryManipulator.addById(client, item.getItemId() + 1, (short) 1, item.getOwner(), pet, 90, "双击宠物获得: " + item.getItemId() + " 时间: " + DateUtil.getCurrentDate());
                    MapleInventoryManipulator.removeFromSlot(client, MapleInventoryType.CASH, slot, (short) 1, false);
                }
                break;
            }
            default: {
                MaplePet pet = item.getPet();
                if (pet != null && (item.getItemId() != 5000054 || pet.getSecondsLeft() > 0) && (item.getExpiration() == -1 || item.getExpiration() > System.currentTimeMillis())) {
                    if (getPetIndex(pet) != -1) { // Already summoned, let's keep it
                        unequipSpawnPet(pet, true, false);
                    } else {
                        int leadid = 8;
                        if (JobConstants.is骑士团(getJob())) {
                            leadid = 10000018; //群宠
                        } else if (JobConstants.is战神(getJob())) {
                            leadid = 20000024; //群宠
                        } else if (JobConstants.is龙神(getJob())) {
                            leadid = 20011024; //群宠
                        } else if (JobConstants.is剑豪(getJob())) {
                            leadid = 40011024; //群宠
                        } else if (JobConstants.is阴阳师(getJob())) {
                            leadid = 40021024; //群宠
                        }
                        if ((getSkillLevel(SkillFactory.getSkill(leadid)) == 0 || getNoPets() == 3) && getSpawnPet(0) != null) {
                            unequipSpawnPet(getSpawnPet(0), false, false);
                        } else if (lead && getSkillLevel(SkillFactory.getSkill(leadid)) > 0) { // Follow the Lead
                            shiftPetsRight();
                        }
                        Point pos = getPosition();
                        pos.y -= 12;
                        pet.setPos(pos);
                        try {
                            pet.setFh(getMap().getFootholds().findBelow(pet.getPos(), true).getId());
                        } catch (NullPointerException e) {
                            pet.setFh(0); //lol, it can be fixed by movement
                        }
                        pet.setStance(0);
                        if (getSkillLevel(pet.getBuffSkill()) == 0) { //检测宠物自动加BUFF的技能是否大于0
                            pet.setBuffSkill(0);
                        }
                        pet.setCanPickup(getIntRecord(GameConstants.ALLOW_PET_LOOT) > 0);
                        addSpawnPet(pet);
                        if (getMap() != null) {
                            petUpdateStats(pet, true);
                            getMap().broadcastMessage(this, PetPacket.showPet(this, pet, false, false), true);
                            client.announce(PetPacket.loadExceptionList(this, pet));
                            //client.announce(PetPacket.petStatUpdate(this));
                            checkPetSkill();
                        }
                    }
                }
                break;
            }
        }
        client.announce(MaplePacketCreator.enableActions());
    }

    public void unequipSpawnPet(MaplePet pet, boolean shiftLeft, boolean hunger) {
        if (getSpawnPet(getPetIndex(pet)) != null) {
            getSpawnPet(getPetIndex(pet)).setSummoned((byte) 0);
            getSpawnPet(getPetIndex(pet)).saveToDb();
        }
        petUpdateStats(pet, false);
        if (map != null) {
            map.broadcastMessage(this, PetPacket.showPet(this, pet, true, hunger), true);
        }
        removeSpawnPet(pet, shiftLeft);
        checkPetSkill();
        //List<Pair<MapleStat, Integer>> stats = new ArrayList<Pair<MapleStat, Integer>>();
        //stats.put(MapleStat.宠物, Integer.valueOf(0)));
        //showpetupdate isn't done here...
        //client.announce(PetPacket.petStatUpdate(this));
        client.announce(MaplePacketCreator.enableActions());
    }

    public void shiftPetsRight() {
        if (spawnPets[2] == null) {
            spawnPets[2] = spawnPets[1];
            spawnPets[1] = spawnPets[0];
            spawnPets[0] = null;
        }
    }

    public void checkPetSkill() {
        Map<Integer, Integer> setHandling = new HashMap<>(); //宠物技能套装集合
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (int i = 0; i < 3; i++) {
            if (spawnPets[i] != null) {
                int set = ii.getPetSetItemID(spawnPets[i].getPetItemId());
                if (set > 0) {
                    int value = 1;
                    if (setHandling.containsKey(set)) {
                        value += setHandling.get(set);
                    }
                    setHandling.put(set, value);
                }
            }
        }
        if (setHandling.isEmpty()) {
            Map<Integer, SkillEntry> chrSkill = new HashMap<>(getSkills());
            Map<Integer, SkillEntry> petSkill = new HashMap<>();
            for (Entry<Integer, SkillEntry> skill : chrSkill.entrySet()) {
                Skill skill1 = SkillFactory.getSkill(skill.getKey());
                if (skill1 != null && skill1.isPetPassive()) {
                    petSkill.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
                }
            }
            if (!petSkill.isEmpty()) { //如果宠物被动触发技能不为空
                changePetSkillLevel(petSkill);
            }
            return;
        }
        Map<Integer, SkillEntry> petSkillData = new HashMap<>();
        for (Entry<Integer, Integer> entry : setHandling.entrySet()) {
            StructSetItem setItem = ii.getSetItem(entry.getKey());
            if (setItem != null) {
                Map<Integer, StructSetItemStat> setItemStats = setItem.getSetItemStats();
                for (Entry<Integer, StructSetItemStat> ent : setItemStats.entrySet()) {
                    StructSetItemStat setItemStat = ent.getValue();
                    if (ent.getKey() <= entry.getValue()) {
                        if (setItemStat.skillId > 0 && setItemStat.skillLevel > 0 && getSkillLevel(setItemStat.skillId) <= 0) {
                            petSkillData.put(setItemStat.skillId, new SkillEntry((byte) setItemStat.skillLevel, (byte) 0, -1));
                        }
                    } else if (setItemStat.skillId > 0 && setItemStat.skillLevel > 0 && getSkillLevel(setItemStat.skillId) > 0) {
                        petSkillData.put(setItemStat.skillId, new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
        }
        if (!petSkillData.isEmpty()) {
            changePetSkillLevel(petSkillData);
        }
    }

    public long getLastFameTime() {
        return lastfametime;
    }

    public List<Integer> getFamedCharacters() {
        return lastmonthfameids;
    }

    public List<Integer> getBattledCharacters() {
        return lastmonthbattleids;
    }

    public FameStatus canGiveFame(MapleCharacter from) {
        if (lastfametime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
            return FameStatus.NOT_TODAY;
        } else if (from == null || lastmonthfameids == null || lastmonthfameids.contains(from.getId())) {
            return FameStatus.NOT_THIS_MONTH;
        }
        return FameStatus.OK;
    }

    public void hasGivenFame(MapleCharacter to) {
        lastfametime = System.currentTimeMillis();
        lastmonthfameids.add(to.getId());
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO famelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing famelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public boolean canBattle(MapleCharacter to) {
        return !(to == null || lastmonthbattleids == null || lastmonthbattleids.contains(to.getAccountID()));
    }

    public void hasBattled(MapleCharacter to) {
        lastmonthbattleids.add(to.getAccountID());
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO battlelog (accid, accid_to) VALUES (?, ?)");
            ps.setInt(1, getAccountID());
            ps.setInt(2, to.getAccountID());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing battlelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    public MapleKeyLayout getKeyLayout() {
        return this.keylayout;
    }

    public MapleQuickSlot getQuickSlot() {
        return this.quickslot;
    }

    public MapleParty getParty() {
        if (party == null) {
            return null;
        } else if (party.isDisbanded()) {
            party = null;
        }
        return party;
    }

    public void setParty(MapleParty party) {
        this.party = party;
    }

    public byte getWorld() {
        return world;
    }

    public void setWorld(byte world) {
        this.world = world;
    }

    public MapleTrade getTrade() {
        return trade;
    }

    public void setTrade(MapleTrade trade) {
        this.trade = trade;
    }

    public EventInstanceManager getEventInstance() {
        return eventInstance;
    }

    public void setEventInstance(EventInstanceManager eventInstance) {
        this.eventInstance = eventInstance;
    }

    public void addDoor(MapleDoor door) {
        doors.add(door);
    }

    public void clearDoors() {
        doors.clear();
    }

    public List<MapleDoor> getDoors() {
        return new ArrayList<>(doors);
    }

    public void addMechDoor(MechDoor door) {
        mechDoors.add(door);
    }

    public void clearMechDoors() {
        mechDoors.clear();
    }

    public List<MechDoor> getMechDoors() {
        return new ArrayList<>(mechDoors);
    }

    public void setSmega() {
        if (smega) {
            smega = false;
            dropMessage(5, "You have set megaphone to disabled mode");
        } else {
            smega = true;
            dropMessage(5, "You have set megaphone to enabled mode");
        }
    }

    public boolean getSmega() {
        return smega;
    }

    public int getChair() {
        return chair;
    }

    public void setChair(int chair) {
        this.chair = chair;
        stats.relocHeal(this);
    }

    public int getChairType() {
        return chairType;
    }

    public void setChairType(int chairType) {
        this.chairType = chairType;
    }

    public String getChairMsg() {
        return chairMsg;
    }

    public void setChairMsg(String chairMsg) {
        this.chairMsg = chairMsg;
    }

    public int getChairMeso() {
        return chairMeso;
    }

    public void setChairMeso(int chairMeso) {
        this.chairMeso = chairMeso;
    }

    public int getItemEffect() {
        return itemEffect;
    }

    public void setItemEffect(int itemEffect) {
        this.itemEffect = itemEffect;
    }

    public int getItemEffectType() {
        return itemEffectType;
    }

    public void setItemEffectType(int itemEffectType) {
        this.itemEffectType = itemEffectType;
    }

    public int getTitleEffect() {
        return titleEffect;
    }

    public void setTitleEffect(int titleEffect) {
        this.titleEffect = titleEffect;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.PLAYER;
    }

    public int getFamilyId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getFamilyId();
    }

    public int getSeniorId() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getSeniorId();
    }

    public int getJunior1() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior1();
    }

    public int getJunior2() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getJunior2();
    }

    public int getCurrentRep() {
        return currentrep;
    }

    public void setCurrentRep(int newRank) {
        currentrep = newRank;
        if (mfc != null) {
            mfc.setCurrentRep(newRank);
        }
    }

    public int getTotalRep() {
        return totalrep;
    }

    public void setTotalRep(int newRank) {
        totalrep = newRank;
        if (mfc != null) {
            mfc.setTotalRep(newRank);
        }
    }

    public int getTotalWins() {
        return totalWins;
    }

    public int getTotalLosses() {
        return totalLosses;
    }

    public void increaseTotalWins() {
        totalWins++;
    }

    public void increaseTotalLosses() {
        totalLosses++;
    }

    public int getGuildId() {
        return guildid;
    }

    public void setGuildId(int newGuildId) {
        guildid = newGuildId;
        if (guildid > 0) {
            if (mgc == null) {
                mgc = new MapleGuildCharacter(this);
            } else {
                mgc.setGuildId(guildid);
            }
        } else {
            mgc = null;
            guildContribution = 0;
        }
    }

    public byte getGuildRank() {
        return guildrank;
    }

    public void setGuildRank(byte newRank) {
        guildrank = newRank;
        if (mgc != null) {
            mgc.setGuildRank(newRank);
        }
    }

    public int getGuildContribution() {
        return guildContribution;
    }

    public void setGuildContribution(int newContribution) {
        this.guildContribution = newContribution;
        if (mgc != null) {
            mgc.setGuildContribution(newContribution);
        }
    }

    public MapleGuildCharacter getMGC() {
        return mgc;
    }

    public byte getAllianceRank() {
        return allianceRank;
    }

    public void setAllianceRank(byte newRank) {
        allianceRank = newRank;
        if (mgc != null) {
            mgc.setAllianceRank(newRank);
        }
    }

    public MapleGuild getGuild() {
        if (getGuildId() <= 0) {
            return null;
        }
        return WorldGuildService.getInstance().getGuild(getGuildId());
    }

    public void guildUpdate() {
        if (guildid <= 0) {
            return;
        }
        mgc.setLevel(level);
        mgc.setJobId(job);
        WorldGuildService.getInstance().memberLevelJobUpdate(mgc);
    }

    public void saveGuildStatus() {
        MapleGuild.setOfflineGuildStatus(guildid, guildrank, guildContribution, allianceRank, id);
    }

    public void familyUpdate() {
        if (mfc == null) {
            return;
        }
        WorldFamilyService.getInstance().memberFamilyUpdate(mfc, this);
    }

    public void saveFamilyStatus() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET familyid = ?, seniorid = ?, junior1 = ?, junior2 = ? WHERE id = ?");
            if (mfc == null) {
                ps.setInt(1, 0);
                ps.setInt(2, 0);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
            } else {
                ps.setInt(1, mfc.getFamilyId());
                ps.setInt(2, mfc.getSeniorId());
                ps.setInt(3, mfc.getJunior1());
                ps.setInt(4, mfc.getJunior2());
            }
            ps.setInt(5, id);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            log.error(se);
        }
        //MapleFamily.setOfflineFamilyStatus(familyid, seniorid, junior1, junior2, currentrep, totalrep, id);
    }

    public boolean modifyCSPoints(int type, int quantity) {
        return modifyCSPoints(type, quantity, false);
    }

    public boolean modifyCSPoints(int type, int quantity, boolean show) {
        switch (type) {
            case 1:
                if (getACash() + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max cash. No cash will be awarded.");
                    }
//                    ban(getName() + " 点卷数量为负", false, true, false);
                    return false;
                }
                setACash(getACash() + quantity);
                break;
            case 2:
                if (getMaplePoints() + quantity < 0) {
                    if (show) {
                        dropMessage(-1, "You have gained the max maple points. No cash will be awarded.");
                    }
//                    ban(getName() + " 抵用卷数量为负", false, true, false);
                    return false;
                }
                setMaplePoints(getMaplePoints() + quantity);
                break;
            default:
                break;
        }
        if (show && quantity != 0) {
            dropMessage(-1, "您" + (quantity > 0 ? "获得了 " : "消耗了 ") + Math.abs(quantity) + (type == 1 ? " 点券." : " 抵用券."));
            //client.announce(MaplePacketCreator.showSpecialEffect(21));
        }
        client.announce(MaplePacketCreator.showCharCash(this));
        return true;
    }

    public int getCSPoints(int type) {
        //System.out.println("角色信息CSPoints 点卷: " + acash + " 抵用卷: " + maplepoints + " 类型: " + type);
        switch (type) {
            case 1:
                return getACash();
            case 2:
                return getMaplePoints();
            case -1:
                return getACash() + getMaplePoints();
            default:
                return 0;
        }
    }

    public int getTotalRMB() {
        int rmb = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT SUM(rmb) FROM paylog WHERE account = ?");
            ps.setString(1, getClient().getAccountName());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                rmb = rs.getInt(1);
            }
            ps.close();
        } catch (SQLException Ex) {
            log.error("获取账号充值总数失败.", Ex);
        }
        return rmb;
    }

    public List<Pair<String, Integer>> getTotalRMBRanking(int limit) {
        List<Pair<String, Integer>> ret = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            Calendar c = Calendar.getInstance();
            c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), 1, 0, 0, 0);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            PreparedStatement ps = con.prepareStatement("SELECT account, SUM(rmb) FROM paylog WHERE date(`paytime`) >= ? GROUP BY account ORDER BY rmb DESC LIMIT ?");
            ps.setString(1, sdf.format(c.getTime().getTime()));
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(new Pair<>(rs.getString("account"), rs.getInt("rmb")));
            }
            ps.close();
        } catch (SQLException Ex) {
            log.error("获取账号充值总数失败.", Ex);
        }
        return ret;
    }

    public int getRMB() {
        int point = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT rmb FROM accounts WHERE name = ?")) {
                ps.setString(1, getClient().getAccountName());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        point = rs.getInt("rmb");
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取角色rmb失败。", e);
        }
        return point;
    }

    public void setRMB(final int point) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET rmb = ? WHERE name = ?")) {
                ps.setInt(1, point);
                ps.setString(2, getClient().getAccountName());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("角色设置RMB失败。", e);
        }
        playerObservable.update();
    }

    public void gainRMB(final int point) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts SET rmb = rmb + ? WHERE name = ?")) {
                ps.setInt(1, point);
                ps.setString(2, getClient().getAccountName());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            log.error("角色增加RMB失败。", e);
        }
        playerObservable.update();
    }

    public boolean hasEquipped(int itemid) {
        return inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid) >= 1;
    }

    public boolean haveItem(int itemid, int quantity, boolean checkEquipped, boolean greaterOrEquals) {
        MapleInventoryType type = ItemConstants.getInventoryType(itemid);
        int possesed = inventory[type.ordinal()].countById(itemid);
        if (checkEquipped && type == MapleInventoryType.EQUIP) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        if (greaterOrEquals) {
            return possesed >= quantity;
        } else {
            return possesed == quantity;
        }
    }

    public boolean haveItem(int itemid, int quantity) {
        return haveItem(itemid, quantity, true, true);
    }

    public boolean haveItem(int itemid) {
        return haveItem(itemid, 1, true, true);
    }

    /*
     * 查看玩家道具的数量
     */
    public int getItemQuantity(int itemid) {
        MapleInventoryType type = ItemConstants.getInventoryType(itemid);
        return getInventory(type).countById(itemid);
    }

    /*
     * 查看玩家道具的数量是否检测当前穿戴的装备
     */
    public int getItemQuantity(int itemid, boolean checkEquipped) {
        int possesed = inventory[ItemConstants.getInventoryType(itemid).ordinal()].countById(itemid);
        if (checkEquipped) {
            possesed += inventory[MapleInventoryType.EQUIPPED.ordinal()].countById(itemid);
        }
        return possesed;
    }

    public int getEquipId(byte slot) {
        MapleInventory equip = getInventory(MapleInventoryType.EQUIP);
        return equip.getItem(slot).getItemId();
    }

    public int getUseId(byte slot) {
        MapleInventory use = getInventory(MapleInventoryType.USE);
        return use.getItem(slot).getItemId();
    }

    public int getSetupId(byte slot) {
        MapleInventory setup = getInventory(MapleInventoryType.SETUP);
        return setup.getItem(slot).getItemId();
    }

    public int getCashId(byte slot) {
        MapleInventory cash = getInventory(MapleInventoryType.CASH);
        return cash.getItem(slot).getItemId();
    }

    public int getEtcId(byte slot) {
        MapleInventory etc = getInventory(MapleInventoryType.ETC);
        return etc.getItem(slot).getItemId();

    }

    /*
     * 好友
     */
    public byte getBuddyCapacity() {
        return buddylist.getCapacity();
    }

    public void setBuddyCapacity(byte capacity) {
        buddylist.setCapacity(capacity);
        client.announce(BuddyListPacket.updateBuddyCapacity(capacity));
    }

    /*
     * 聊天招待
     */
    public MapleMessenger getMessenger() {
        return messenger;
    }

    public void setMessenger(MapleMessenger messenger) {
        this.messenger = messenger;
    }

    /*
     * 召唤兽处理
     */
    public List<MapleSummon> getSummonsReadLock() {
        summonsLock.readLock().lock();
        return summons;
    }

    public int getSummonsSize() {
        return summons.size();
    }

    public void unlockSummonsReadLock() {
        summonsLock.readLock().unlock();
    }

    public void addSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.add(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    public void removeSummon(MapleSummon s) {
        summonsLock.writeLock().lock();
        try {
            summons.remove(s);
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    /**
     * **
     * 删除指定技能的召唤兽
     *
     * @param Skillid
     */
    public void removeSummon(int Skillid) {
        summonsLock.writeLock().lock();
        MapleSummon delet = null;
        try {
            for (MapleSummon su : summons) {
                if (su.getSkillId() == Skillid) {
                    delet = su;
                }
            }
            if (delet != null) {
                getMap().broadcastMessage(SummonPacket.removeSummon(delet, true));
                getMap().removeMapObject(delet);
                removeVisibleMapObject(delet);
                summons.remove(delet);
            }
        } finally {
            summonsLock.writeLock().unlock();
        }
    }

    /**
     * 当前技能是否已经有召唤兽
     */
    public boolean hasSummonBySkill(int skillId) {
        if (summons == null || summons.isEmpty()) {
            return false;
        }
        summonsLock.readLock().lock();
        try {
            for (MapleSummon summon : summons) {
                if (summon.getSkillId() == skillId) {
                    return true;
                }
            }
        } finally {
            summonsLock.readLock().unlock();
        }
        return false;
    }

    /**
     * 根据技能ID查找怪物对象
     *
     * @param skillId
     * @return
     */
    public MapleSummon getSummonBySkill(int skillId) {
        if (hasSummonBySkill(skillId)) {
            summonsLock.readLock().lock();
            try {
                for (MapleSummon summon : summons) {
                    if (summon.getSkillId() == skillId) {
                        return summon;
                    }
                }
            } finally {
                summonsLock.readLock().unlock();
            }
        }
        return null;
    }

    public void updateSummonBySkillID(int skillId, int newSkillId, boolean animated) {
        summonsLock.readLock().lock();
        try {
            summons.forEach(summon -> {
                if (summon.getSkillId() == skillId) {
                    summon.setSkillId(newSkillId);
                    this.getMap().broadcastMessage(SummonPacket.removeSummon(summon, animated));
                }
            });
        } finally {
            summonsLock.readLock().unlock();
        }
    }

    /*
     * 技能冷却处理
     */
    public void addCooldown(int skillId, long startTime, long length) {
        coolDowns.put(skillId, new MapleCoolDownValueHolder(skillId, startTime, length));
    }

    public boolean isSkillCooling(int skillid) {
        return coolDowns.containsKey(skillid);
    }

    public void removeCooldown(int skillId) {
        if (coolDowns.containsKey(skillId)) {
            coolDowns.remove(skillId);
        }
    }

    public void removeCooldown(int skillId, boolean show) {
        if (coolDowns.containsKey(skillId)) {
            coolDowns.remove(skillId);
            if (show) {
                client.announce(MaplePacketCreator.skillCooldown(skillId, 0));
            }
        }
    }

    public boolean skillisCooling(int skillId) {
        if (coolDowns.containsKey(skillId)) {
            MapleCoolDownValueHolder mapleCoolDownValueHolder = coolDowns.get(skillId);
            return (System.currentTimeMillis() - mapleCoolDownValueHolder.startTime - mapleCoolDownValueHolder.length) > 1000;
        }
        return false;
    }

    public void giveCoolDowns(int skillid, long starttime, long length) {
        addCooldown(skillid, starttime, length);
    }

    public void giveCoolDowns(List<MapleCoolDownValueHolder> cooldowns) {
        if (cooldowns != null) {
            for (MapleCoolDownValueHolder cooldown : cooldowns) {
                coolDowns.put(cooldown.skillId, cooldown);
            }
        } else {
            try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                PreparedStatement ps = con.prepareStatement("SELECT SkillID,StartTime,length FROM skills_cooldowns WHERE charid = ?");
                ps.setInt(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (rs.getLong("length") + rs.getLong("StartTime") - System.currentTimeMillis() <= 0) {
                        continue;
                    }
                    giveCoolDowns(rs.getInt("SkillID"), rs.getLong("StartTime"), rs.getLong("length"));
                }
                ps.close();
                rs.close();
                deleteWhereCharacterId(con, "DELETE FROM skills_cooldowns WHERE charid = ?");
            } catch (SQLException e) {
                System.err.println("Error while retriving cooldown from SQL storage");
            }
        }
    }

    public int getCooldownSize() {
        return coolDowns.size();
    }

    public List<MapleCoolDownValueHolder> getCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mc : coolDowns.values()) {
            if (mc != null) {
                ret.add(mc);
            }
        }
        return ret;
    }

    /*
     * 怪物给角色的BUFF处理
     */
    public List<MapleDiseaseValueHolder> getAllDiseases() {
        return new ArrayList<>(diseases.values());
    }

    public MapleDiseaseValueHolder getDiseasesValue(MapleDisease dis) {
        return diseases.get(dis);
    }

    public boolean hasDisease(MapleDisease dis) {
        return diseases.containsKey(dis);
    }

    public void giveDebuff(MapleDisease disease, MobSkill skill) {
        giveDebuff(disease, skill.getX(), skill.getDuration(), skill.getSkillId(), skill.getSkillLevel());
    }

    public void giveDebuff(MapleDisease disease, int x, long duration, int skillid, int level) {
        if (ServerConfig.CHANNEL_APPLYPLAYERDEBUFF) {
            return;
        }
        if (map != null && !hasDisease(disease)) {
            if (!(disease == MapleDisease.诱惑 || disease == MapleDisease.昏迷)) { // || disease == MapleDisease.FLAG
                if (getBuffedValue(MapleBuffStat.进阶祝福) != null) {
                    return;
                }
            }
            int mC = getBuffSource(MapleBuffStat.金属机甲);
            if (mC > 0 && mC != 机械师.金属机甲_战车) { //missile tank can have debuffs
                return; //flamethrower and siege can't
            }
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.神圣保护);
            if (effect != null) {
                int count = getBuffedValue(MapleBuffStat.神圣保护);
                if (count > 0) {
                    int newcount = count - 1;
                    if (newcount > 0) {
                        setBuffedValue(MapleBuffStat.神圣保护, newcount);
                        Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.神圣保护, newcount);
                        client.announce(BuffPacket.giveBuff(effect.getSourceid(), 0, stat, effect, this));
                    } else {
                        cancelEffectFromBuffStat(MapleBuffStat.神圣保护);
                        if (effect.getSourceid() == 夜光.抵抗之魔法盾) {
                            client.announce(MaplePacketCreator.skillCooldown(effect.getSourceid(), effect.getCooldown(this)));
                            addCooldown(effect.getSourceid(), System.currentTimeMillis(), effect.getCooldown(this) * 1000);
                        }
                    }
                    return;
                } else {
                    cancelEffectFromBuffStat(MapleBuffStat.神圣保护);
                    if (effect.getSourceid() == 夜光.抵抗之魔法盾) {
                        client.announce(MaplePacketCreator.skillCooldown(effect.getSourceid(), effect.getCooldown(this)));
                        addCooldown(effect.getSourceid(), System.currentTimeMillis(), effect.getCooldown(this) * 1000);
                    }
                }
            }
            if (stats.ASR > 0 && Randomizer.nextInt(100) < stats.ASR) {
                return;
            }
            diseases.put(disease, new MapleDiseaseValueHolder(disease, skillid, level, System.currentTimeMillis(), duration - stats.decreaseDebuff));
            client.announce(BuffPacket.giveDebuff(disease, x, skillid, level, (int) duration));
            map.broadcastMessage(this, BuffPacket.giveForeignDebuff(id, disease, skillid, level, x), false);
            if (x > 0 && disease == MapleDisease.中毒) { //poison, subtract all HP
                addHP((int) -(x * ((duration - stats.decreaseDebuff) / 1000)));
            }
        }
    }

    /*
     * 切换频道或者进入商城出来后 给角色负面BUFF 不需要发送封包
     */
    public void giveSilentDebuff(List<MapleDiseaseValueHolder> ld) {
        if (ld != null) {
            for (MapleDiseaseValueHolder disease : ld) {
                diseases.put(disease.disease, disease);
            }
        }
    }

    public void dispelDebuff(MapleDisease debuff) {
        if (hasDisease(debuff)) {
            client.announce(BuffPacket.cancelDebuff(debuff));
            map.broadcastMessage(this, BuffPacket.cancelForeignDebuff(id, debuff), false);
            diseases.remove(debuff);
        }
    }

    public void dispelDebuffs() {
        List<MapleDisease> diseasess = new ArrayList<>(diseases.keySet());
        for (MapleDisease d : diseasess) {
            dispelDebuff(d);
        }
    }

    public void cancelAllDebuffs() {
        diseases.clear();
    }

    public int getDiseaseSize() {
        return diseases.size();
    }

    /*
     * 小字条处理也就是短消息
     */
    public void sendNote(String to, String msg) {
        sendNote(to, msg, 0);
    }

    public void sendNote(String to, String msg, int fame) {
        MapleCharacterUtil.sendNote(to, getName(), msg, fame);
    }

    public void showNote() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM notes WHERE `to`=?", ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            ps.setString(1, getName());
            ResultSet rs = ps.executeQuery();
            rs.last();
            int count = rs.getRow();
            rs.first();
            client.announce(MTSCSPacket.INSTANCE.showNotes(rs, count));
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to show note" + e);
        }
    }

    public void deleteNote(int id, int fame) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT gift FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt("gift") == fame && fame > 0) { //not exploited! hurray
                    addFame(fame);
                    updateSingleStat(MapleStat.人气, getFame());
                    client.announce(MaplePacketCreator.getShowFameGain(fame));
                }
            }
            rs.close();
            ps.close();
            ps = con.prepareStatement("DELETE FROM notes WHERE `id`=?");
            ps.setInt(1, id);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Unable to delete note" + e);
        }
    }

    public int getMulungEnergy() {
        return mulung_energy;
    }

    public void mulung_EnergyModify(boolean inc) {
        if (inc) {
            if (mulung_energy + 100 > 10000) {
                mulung_energy = 10000;
            } else {
                mulung_energy += 100;
            }
        } else {
            mulung_energy = 0;
        }
        client.announce(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeMapEventEffect(final String inc) {
        client.announce(MaplePacketCreator.showEffect(inc));
    }

    public void writeMulungEnergy() {
        client.announce(MaplePacketCreator.MulungEnergy(mulung_energy));
    }

    public void writeEnergy(String type, String inc) {
        client.announce(MaplePacketCreator.sendPyramidEnergy(type, inc));
    }

    public void writeStatus(String type, String inc) {
        client.announce(MaplePacketCreator.sendGhostStatus(type, inc));
    }

    public void writePoint(String type, String inc) {
        client.announce(MaplePacketCreator.sendGhostPoint(type, inc));
    }

    /*
     * 战神连击点数
     */
    public int getAranCombo() {
        return specialStats.getAranCombo();
    }

    public void gainAranCombo(int count, boolean show) {
        setAranCombo(getAranCombo() + count, show);
    }

    public void setAranCombo(int count, boolean show) {
        specialStats.setAranCombo(Math.max(0, Math.min(30000, count)));
        if (show) {
            client.announce(MaplePacketCreator.ShowAranCombo(getAranCombo()));
        }
    }

    public long getKeyDownSkill_Time() {
        return keydown_skill;
    }

    public void setKeyDownSkill_Time(long keydown_skill) {
        this.keydown_skill = keydown_skill;
    }

    /*
     * 黑暗力量效果
     */
    public void checkBerserk() { //berserk is special in that it doesn't use worldtimer :)
        if (job != 132 || lastBerserkTime < 0 || lastBerserkTime + 10000 > System.currentTimeMillis()) {
            return;
        }
        int skillId = 黑骑士.重生契约;
        Skill BerserkX = SkillFactory.getSkill(skillId); //黑骑士.黑暗力量
        int skilllevel = getTotalSkillLevel(BerserkX);
        if (skilllevel >= 1 && map != null) {
            lastBerserkTime = System.currentTimeMillis();
            MapleStatEffect ampStat = BerserkX.getEffect(skilllevel);
            stats.Berserk = stats.getHp() * 100 / stats.getCurrentMaxHp() >= ampStat.getX();
            client.announce(EffectPacket.showOwnBuffEffect(skillId, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)));
            map.broadcastMessage(this, EffectPacket.showBuffeffect(this, skillId, 1, getLevel(), skilllevel, (byte) (stats.Berserk ? 1 : 0)), false);
        } else {
            lastBerserkTime = -1;
        }
    }

    /*
     * 夜光生命潮汐
     */
    public void check生命潮汐() {
        if (job != 2711 && job != 2712) {
            return;
        }
        Skill skill = SkillFactory.getSkill(夜光.生命潮汐);
        int skilllevel = getTotalSkillLevel(skill);
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.生命潮汐);
        if (skilllevel >= 1 && map != null) {
            if (effect == null || effect.getLevel() < skilllevel) {
                skill.getEffect(skilllevel).applyTo(this);
            }
        }
    }

    /*
     * 恶魔复仇者血之契约
     */
    public void checkBloodContract() {
        if (!JobConstants.is恶魔复仇者(job)) {
            return;
        }
        Skill skill = SkillFactory.getSkill(恶魔复仇者.血之契约);
        int skilllevel = getTotalSkillLevel(skill);
        if (skilllevel >= 1 && map != null) {
            skill.getEffect(skilllevel).applyTo(this);
        }
    }

    /*
     * 设置自由市场显示的小黑板信息
     */
    public void setMarketChalkboard(String text) {
        if (map == null) {
            return;
        }
        map.broadcastMessage(MTSCSPacket.INSTANCE.useChalkboard(getId(), text));
        if (chalkSchedule != null) {
            chalkSchedule.cancel(false);
            chalkSchedule = null;
        }
        chalkSchedule = WorldTimer.getInstance().schedule(() -> setChalkboard(null), 4 * 1000);
    }

    public String getChalkboard() {
        return chalktext;
    }

    public void setChalkboard(String text) {
        this.chalktext = text;
        if (map != null) {
            map.broadcastMessage(MTSCSPacket.INSTANCE.useChalkboard(getId(), text));
        }
    }

    public MapleMount getMount() {
        return mount;
    }

    public int[] getWishlist() {
        return wishlist;
    }

    public void setWishlist(int[] wl) {
        this.wishlist = wl;
        changed_wishlist = true;
    }

    public void clearWishlist() {
        for (int i = 0; i < 12; i++) {
            wishlist[i] = 0;
        }
        changed_wishlist = true;
    }

    public int getWishlistSize() {
        int ret = 0;
        for (int i = 0; i < 12; i++) {
            if (wishlist[i] > 0) {
                ret++;
            }
        }
        return ret;
    }

    public int[] getRocks() {
        return rocks;
    }

    public int getRockSize() {
        int ret = 0;
        for (int i = 0; i < 10; i++) {
            if (rocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRocks(int map) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == map) {
                rocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRockMap() {
        if (getRockSize() >= 10) {
            return;
        }
        rocks[getRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRockMap(int id) {
        for (int i = 0; i < 10; i++) {
            if (rocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getRegRocks() {
        return regrocks;
    }

    public int getRegRockSize() {
        int ret = 0;
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromRegRocks(int map) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == map) {
                regrocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addRegRockMap() {
        if (getRegRockSize() >= 5) {
            return;
        }
        regrocks[getRegRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isRegRockMap(int id) {
        for (int i = 0; i < 5; i++) {
            if (regrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    public int[] getHyperRocks() {
        return hyperrocks;
    }

    public int getHyperRockSize() {
        int ret = 0;
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] != 999999999) {
                ret++;
            }
        }
        return ret;
    }

    public void deleteFromHyperRocks(int map) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == map) {
                hyperrocks[i] = 999999999;
                changed_trocklocations = true;
                break;
            }
        }
    }

    public void addHyperRockMap() {
        if (getRegRockSize() >= 13) {
            return;
        }
        hyperrocks[getHyperRockSize()] = getMapId();
        changed_trocklocations = true;
    }

    public boolean isHyperRockMap(int id) {
        for (int i = 0; i < 13; i++) {
            if (hyperrocks[i] == id) {
                return true;
            }
        }
        return false;
    }

    /*
     * 药剂罐系统
     */
    public MaplePotionPot getPotionPot() {
        return potionPot;
    }

    public void setPotionPot(MaplePotionPot p) {
        this.potionPot = p;
    }

    /*
     * 龙的传人宝盒系统
     * 0001214 - 宝盒的庇佑 - 获得含有侠义精神的宝盒的庇佑。\n#c双击#技能可打开宝盒界面，查看目前属性及#c剩余时间#，倒计时结束后当前属性会重置。可以通过特定道具变更属性、走向、剩余时间。
     * 80001151 - 宝盒的庇佑 - 获得包含了侠义精神的宝盒的庇佑。从龙的传人那里获得的属性链接，会因为自身和传授者的等级产生差异。
     */
    public MapleCoreAura getCoreAura() {
        return coreAura;
    }

    public void updataCoreAura() {
        if (coreAura != null) {
            client.announce(InventoryPacket.updataCoreAura(this));
        }
    }

    public void dropMessage(int type, String message) {
        if (type == -1) {
            client.announce(UIPacket.getTopMsg(message));
        } else if (type == -2) {
            client.announce(PlayerShopPacket.playerInterChat(message, 0)); //0 or what
        } else if (type == -3) {
            client.announce(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 0)); //1 = hide
        } else if (type == -4) {
            client.announce(MaplePacketCreator.getChatText(getId(), message, isSuperGM(), 1)); //1 = hide
        } else if (type == -5) {
            client.announce(MaplePacketCreator.spouseMessage(message, false)); //pink - SPOUSE_MESSAGE 0x06
        } else if (type == -6) {
            client.announce(MaplePacketCreator.spouseMessage(message, true)); //white bg - SPOUSE_MESSAGE 0x0A
        } else if (type == -7) {
            client.announce(UIPacket.getMidMsg(message, false, 0));
        } else if (type == -8) {
            client.announce(UIPacket.getMidMsg(message, true, 0));
        } else if (type == -9) {
            client.announce(MaplePacketCreator.showRedNotice(message));//SHOW_STATUS_INFO 0x0B
        } else if (type == -10) {
            client.announce(MaplePacketCreator.getFollowMessage(message));//SPOUSE_MESSAGE 0x0B
        } else if (type == -11) {
            client.announce(MaplePacketCreator.yellowChat(message));//SPOUSE_MESSAGE 0x07
        } else if (type == -12) {
            client.announce(UIPacket.getNpcNotice(1540488, message, 10000));
        } else {
            client.announce(MaplePacketCreator.serverNotice(type, message));
        }
    }

    public void dropSpouseMessage(int type, String message) {
        if (type == 0x00 || type == 0x01 || (type >= 0x06 && type <= 0x2A)) {
            client.announce(MaplePacketCreator.spouseMessage(type, message));
        } else {
            client.announce(MaplePacketCreator.serverNotice(5, message));
        }
    }

    /**
     * 用于在游戏内输出调试信息
     *
     * @param type    1普通 2警告 3错误
     * @param message
     */
    public void dropDebugMessage(int type, String message) {
        dropSpouseMessage(type == 1 ? 0x18 : type == 2 ? 0x1A : 0x23, message);
    }

    public IMaplePlayerShop getPlayerShop() {
        return playerShop;
    }

    public void setPlayerShop(IMaplePlayerShop playerShop) {
        this.playerShop = playerShop;
    }

    public int getConversation() {
        return inst.get();
    }

    public void setConversation(int inst) {
        this.inst.set(inst);
    }

    public void resetConversation() {
        if (this.getConversation() != 0) {
            this.setConversation(0);
        }
    }

    public int getDirection() {
        return insd.get();
    }

    public void setDirection(int inst) {
        this.insd.set(inst);
    }

    public MapleCarnivalParty getCarnivalParty() {
        return carnivalParty;
    }

    public void setCarnivalParty(MapleCarnivalParty party) {
        carnivalParty = party;
    }

    public void addCP(int ammount) {
        totalCP += ammount;
        availableCP += ammount;
    }

    public void useCP(int ammount) {
        availableCP -= ammount;
    }

    public int getAvailableCP() {
        return availableCP;
    }

    public int getTotalCP() {
        return totalCP;
    }

    public void resetCP() {
        totalCP = 0;
        availableCP = 0;
    }

    public void addCarnivalRequest(MapleCarnivalChallenge request) {
        pendingCarnivalRequests.add(request);
    }

    public final MapleCarnivalChallenge getNextCarnivalRequest() {
        return pendingCarnivalRequests.pollLast();
    }

    public void clearCarnivalRequests() {
        pendingCarnivalRequests = new LinkedList<>();
    }

    public void startMonsterCarnival(final int enemyavailable, final int enemytotal) {
        client.announce(MonsterCarnivalPacket.startMonsterCarnival(this, enemyavailable, enemytotal));
    }

    public void setAchievementFinished(int id) {
        if (!finishedAchievements.contains(id)) {
            finishedAchievements.add(id);
            changed_achievements = true;
        }
    }

    public boolean achievementFinished(int achievementid) {
        return finishedAchievements.contains(achievementid);
    }

    public void finishAchievement(int id) {
        if (!achievementFinished(id)) {
            if (isAlive()) {
                MapleAchievements.getInstance().getById(id).finishAchievement(this);
            }
        }
    }

    public List<Integer> getFinishedAchievements() {
        return finishedAchievements;
    }

    public boolean getCanTalk() {
        return this.canTalk;
    }

    public void canTalk(boolean talk) {
        this.canTalk = talk;
    }

    /*
     * 角色的经验倍数 是否有经验卡
     */
    public double getEXPMod() {
        return hasEXPCard(); //stats.expMod
    }

    /*
     * 经验卡
     */
    public double hasEXPCard() {
        int[] expCards = {5210000, //双倍经验值卡一天权 使用时间\n周一至周五 : 10:00 - 22:00\n周六, 周日 : 00:00 - 24:00
                5210001, //双倍经验值卡七天权 使用时间\n周一至周五 : 10:00 - 22:00\n周六, 周日 : 00:00 - 24:00
                5210002, //双倍经验值卡一天权(白) 使用时间\n周一至周五 : 06:00 - 18:00\n周六, 周日 : 00:00 - 24:00
                5210003, //双倍经验值卡七天权(白) 使用时间\n周一至周五 : 06:00 - 18:00\n周六, 周日 : 00:00 - 24:00
                5210004, //双倍经验值卡一天(晚) 使用时间\n周一至周五 : \n当天18:00 - 次日06:00\n周六, 周日 : 00:00 - 24:00
                5210005, //双倍经验值卡七天(晚) 使用时间\n周一至周五 : \n当天18:00 - 次日06:00\n周六, 周日 : 00:00 - 24:00
                5210006, //双倍经验值一七天(特价) 使用时间\n10月7号15:00 - 21:00
                5211047, //双倍经验值卡三小时权

                5211060, //三倍经验卡(2小时)

                5211000, //双倍经验 韩
                5211001, //双倍经验 韩
                5211002, //双倍经验 韩

                5211063, //经验值1.5倍 7天权 (下午券)  使用时间\n12:00 - 18:00
                5211064, //经验值1.5倍 7天权 (夜间券)  使用时间\n18:00 - 24:00
                5211065, //经验值1.5倍 28天权 (下午券) 使用时间\n12:00 - 18:00
                5211066, //经验值1.5倍 28天权 (夜间券) 使用时间\n18:00 - 24:00
                5211069, //经验值1.5倍 1天权 (夜间券)  使用时间\n18:00 - 24:00
                5211070, //经验值1.5倍 1天权 (下午券)  使用时间\n12:00 - 18:00

                //V.104新增
                5211084, //经验值2倍平日券（下午）- 使用时间\n周一至周五 : 12:00 - 18:00
                5211085, //经验值2倍平日券（夜间）- 使用时间\n周一至周五 : 18:00 - 24:00
                5211108, //经验值2倍周末券（下午）- 使用时间\n周六、周日 : 12:00 - 18:00
                5211109, //经验值2倍周末券（夜间）- 使用时间\n周六、周日 : 18:00 - 24:00
        };
        MapleInventory iv = getInventory(MapleInventoryType.CASH);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        double canuse = 1;
        for (Integer ids : expCards) {
            if (iv.countById(ids) > 0) {
                if (ii.isExpOrDropCardTime(ids)) {
                    switch (ids) {
                        case 5210000:
                        case 5210001:
                        case 5210002:
                        case 5210003:
                        case 5210004:
                        case 5210005:
                        case 5210006:
                        case 5211047:
                        case 5211000:
                        case 5211001:
                        case 5211002:
                        case 5211084: //V.104新增
                        case 5211085:
                        case 5211108:
                        case 5211109:
                            canuse = 2;
                            break;
                        case 5211060:
                            canuse = 3;
                            break;
                        case 5211063:
                        case 5211064:
                        case 5211065:
                        case 5211066:
                        case 5211069:
                        case 5211070:
                            canuse = 1.5;
                            break;
                    }
                }
            }
        }
        return canuse;
    }

    /*
     * 角色的爆率倍数 是否有双倍爆率卡
     */
    public int getDropMod() {
        return hasDropCard(); //stats.dropMod
    }

    /*
     * 双倍爆率卡
     */
    public int hasDropCard() {
        int[] dropCards = {5360000, 5360014, 5360015, 5360016};
        MapleInventory iv = getInventory(MapleInventoryType.CASH);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Integer id3 : dropCards) {
            if (iv.countById(id3) > 0) {
                if (ii.isExpOrDropCardTime(id3)) {
                    return 2;
                }
            }
        }
        return 1;
    }

    public int getCashMod() {
        return stats.cashMod;
    }

    public int getACash() {
        return getClient().getACash();
    }

    public void setACash(final int point) {
        getClient().setACash(point);
        playerObservable.update();
    }

    public int getMaplePoints() {
        return getClient().getMaplePoints();
    }

    public void setMaplePoints(final int point) {
        getClient().setMaplePoints(point);
        playerObservable.update();
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int p) {
        this.points = p;
        if (this.points >= 1) {
            finishAchievement(1);
        }
    }

    public int getVPoints() {
        return vpoints;
    }

    public void setVPoints(int p) {
        this.vpoints = p;
    }

    public CashShop getCashInventory() {
        return cs;
    }

    public void removeItem(int id, int quantity) {
        MapleInventoryManipulator.removeById(client, ItemConstants.getInventoryType(id), id, quantity, true, false);
        client.announce(MaplePacketCreator.getShowItemGain(id, (short) -quantity, true));
    }

    public void removeAll(int id) {
        removeAll(id, true, false);
    }

    public void removeAll(int itemId, boolean show, boolean checkEquipped) {
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        int possessed = getInventory(type).countById(itemId);
        if (possessed > 0) {
            MapleInventoryManipulator.removeById(getClient(), type, itemId, possessed, true, false);
            if (show) {
                getClient().announce(MaplePacketCreator.getShowItemGain(itemId, (short) -possessed, true));
            }
        }
        if (checkEquipped && type == MapleInventoryType.EQUIP) { //检测当前穿戴的装备
            type = MapleInventoryType.EQUIPPED;
            possessed = getInventory(type).countById(itemId);
            if (possessed > 0) {
                MapleInventoryManipulator.removeById(getClient(), type, itemId, possessed, true, false);
                if (show) {
                    getClient().announce(MaplePacketCreator.getShowItemGain(itemId, (short) -possessed, true));
                }
                equipChanged();
            }
        }
    }

    public void removeItem(int itemId) {
        removeItem(itemId, false);
    }

    public void removeItem(int itemId, boolean show) {
        MapleInventoryType type = ItemConstants.getInventoryType(itemId);
        if (type == MapleInventoryType.EQUIP) { //check equipped
            type = MapleInventoryType.EQUIPPED;
            int possessed = getInventory(type).countById(itemId);
            if (possessed > 0) {
                MapleInventoryManipulator.removeById(getClient(), type, itemId, possessed, true, false);
                if (show) {
                    getClient().announce(MaplePacketCreator.getShowItemGain(itemId, (short) -possessed, true));
                }
                equipChanged();
            }
        }
    }

    public MapleRing getMarriageRing() {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        MapleRing ring, mrings = null;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (mrings == null && ItemConstants.is结婚戒指(item.getItemId())) {
                    mrings = ring;
                }
            }
        }
        if (mrings == null) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (mrings == null && ItemConstants.is结婚戒指(item.getItemId())) {
                        mrings = ring;
                    }
                }
            }
        }
        return mrings;
    }

    public Triple<List<MapleRing>, List<MapleRing>, List<MapleRing>> getRings(boolean equip) {
        MapleInventory iv = getInventory(MapleInventoryType.EQUIPPED);
        List<Item> equipped = iv.newList();
        Collections.sort(equipped);
        List<MapleRing> crings = new ArrayList<>(), frings = new ArrayList<>(), mrings = new ArrayList<>();
        MapleRing ring;
        for (Item ite : equipped) {
            Equip item = (Equip) ite;
            if (item.getRing() != null && ItemConstants.isEffectRing(item.getItemId())) {
                ring = item.getRing();
                ring.setEquipped(true);
                if (equip) {
                    if (ItemConstants.is恋人戒指(item.getItemId())) {
                        crings.add(ring);
                    } else if (ItemConstants.is好友戒指(item.getItemId())) {
                        frings.add(ring);
                    } else if (ItemConstants.is结婚戒指(item.getItemId())) {
                        mrings.add(ring);
                    }
                } else if (crings.isEmpty() && ItemConstants.is恋人戒指(item.getItemId())) {
                    crings.add(ring);
                } else if (frings.isEmpty() && ItemConstants.is好友戒指(item.getItemId())) {
                    frings.add(ring);
                } else if (mrings.isEmpty() && ItemConstants.is结婚戒指(item.getItemId())) {
                    mrings.add(ring);
                }
            }
        }
        if (equip) {
            iv = getInventory(MapleInventoryType.EQUIP);
            for (Item ite : iv.list()) {
                Equip item = (Equip) ite;
                if (item.getRing() != null && ItemConstants.isEffectRing(item.getItemId())) {
                    ring = item.getRing();
                    ring.setEquipped(false);
                    if (ItemConstants.is恋人戒指(item.getItemId())) {
                        crings.add(ring);
                    } else if (ItemConstants.is好友戒指(item.getItemId())) {
                        frings.add(ring);
                    } else if (ItemConstants.is结婚戒指(item.getItemId())) {
                        mrings.add(ring);
                    }
                }
            }
        }
        frings.sort(new MapleRing.RingComparator());
        crings.sort(new MapleRing.RingComparator());
        mrings.sort(new MapleRing.RingComparator());
        return new Triple<>(crings, frings, mrings);
    }

    public int getFH() {
        MapleFoothold fh = getMap().getFootholds().findBelow(this.getTruePosition(), true);
        if (fh != null) {
            return fh.getId();
        }
        return 0;
    }

    public void startFairySchedule(boolean exp) {
        startFairySchedule(exp, false);
    }

    public void startFairySchedule(boolean exp, boolean equipped) {
        cancelFairySchedule(exp || stats.equippedFairy == 0);
        if (fairyExp <= 0) {
            fairyExp = (byte) stats.equippedFairy;
        }
        if (equipped && fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            dropMessage(5, "您装备了精灵吊坠在1小时后经验获取将增加到 " + (fairyExp + stats.equippedFairy) + " %.");
        }
        lastFairyTime = System.currentTimeMillis();
    }

    public boolean canFairy(long now) {
        return lastFairyTime > 0 && lastFairyTime + (60 * 60 * 1000) < now;
    }

    public boolean canHP(long now) {
        if (lastHPTime + 5000 < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public boolean canMP(long now) {
        if (lastMPTime + 5000 < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public boolean canHPRecover(long now) {
        if (stats.hpRecoverTime > 0 && lastHPTime + stats.hpRecoverTime < now) {
            lastHPTime = now;
            return true;
        }
        return false;
    }

    public boolean canMPRecover(long now) {
        if (stats.mpRecoverTime > 0 && lastMPTime + stats.mpRecoverTime < now) {
            lastMPTime = now;
            return true;
        }
        return false;
    }

    public void cancelFairySchedule(boolean exp) {
        lastFairyTime = 0;
        if (exp) {
            this.fairyExp = 0;
        }
    }

    public void doFairy() {
        if (fairyExp < stats.equippedFairy * 3 && stats.equippedFairy > 0) {
            fairyExp += stats.equippedFairy;
            dropMessage(5, "精灵吊坠经验获取增加到 " + fairyExp + " %.");
        }
        if (getGuildId() > 0) {
            WorldGuildService.getInstance().gainGP(getGuildId(), 20, id);
            client.announce(UIPacket.getGPContribution(20));
        }
        traits.get(MapleTraitType.will).addExp(5, this); //willpower every hour
        startFairySchedule(false, true);
    }

    public byte getFairyExp() {
        return fairyExp;
    }

    public int getTeam() {
        return coconutteam;
    }

    public void setTeam(int v) {
        this.coconutteam = v;
    }

    public void clearLinkMid() {
        linkMobs.clear();
        cancelEffectFromBuffStat(MapleBuffStat.导航辅助);
        cancelEffectFromBuffStat(MapleBuffStat.神秘瞄准术);
    }

    public int getFirstLinkMid() {
        for (Integer lm : linkMobs.keySet()) {
            return lm;
        }
        return 0;
    }

    public Map<Integer, Integer> getAllLinkMid() {
        return linkMobs;
    }

    public void setLinkMid(int lm, int x) {
        linkMobs.put(lm, x);
    }

    public int getDamageIncrease(int lm) {
        if (linkMobs.containsKey(lm)) {
            return linkMobs.get(lm);
        }
        return 0;
    }

    public MapleExtractor getExtractor() {
        return extractor;
    }

    public void setExtractor(MapleExtractor me) {
        removeExtractor();
        this.extractor = me;
    }

    public void removeExtractor() {
        if (extractor != null) {
            map.broadcastMessage(MaplePacketCreator.removeExtractor(this.id));
            map.removeMapObject(extractor);
            extractor = null;
        }
    }

    public void spawnSavedPets() {
        for (byte aPetStore : petStore) {
            if (aPetStore > -1) {
                spawnPet(aPetStore, false, false);
            }
        }
        petStore = new byte[]{-1, -1, -1};
    }

    public byte[] getPetStores() {
        return petStore;
    }

    public Event_PyramidSubway getPyramidSubway() {
        return pyramidSubway;
    }

    public void setPyramidSubway(Event_PyramidSubway ps) {
        this.pyramidSubway = ps;
    }

    /*
     * 0 = 机械师
     * 1 = 冒险家
     * 2 = 骑士团
     * 3 =  战神
     * 4 = 龙神
     * 5 = 双弩精灵
     * 6 = 恶魔猎手  恶魔复仇者
     * 0A = 夜光
     * 0B = 狂龙战士
     * 0C = 爆莉萌天使
     * 0E = 尖兵
     */
    public byte getSubcategory() {
//        if (job >= 430 && job <= 434) {
//            return 1;
//        } else if (GameConstants.is火炮手(job)) {
//            return 2;
//        } else if (GameConstants.is龙的传人(job)) {
//            return 10;
//        } else if (job == 0) {
//            return subcategory;
//        }
//        return 0;
        return subcategory;
    }

    public void setSubcategory(int z) {
        this.subcategory = (byte) z;
    }

    public int itemQuantity(int itemid) {
        return getInventory(ItemConstants.getInventoryType(itemid)).countById(itemid);
    }

    public RockPaperScissors getRPS() {
        return rps;
    }

    public void setRPS(RockPaperScissors rps) {
        this.rps = rps;
    }

    public long getNextConsume() {
        return nextConsume;
    }

    public void setNextConsume(long nc) {
        this.nextConsume = nc;
    }

    public int getRank() {
        return rank;
    }

    public int getRankMove() {
        return rankMove;
    }

    public int getJobRank() {
        return jobRank;
    }

    public int getJobRankMove() {
        return jobRankMove;
    }

    public void changeChannel(int channel) {
        ChannelServer toch = ChannelServer.getInstance(channel);
        if (channel == client.getChannel() || toch == null || toch.isShutdown()) {
            client.announce(MaplePacketCreator.serverBlocked(1));
            return;
        }
        changeRemoval();
        ChannelServer ch = ChannelServer.getInstance(client.getChannel());
        if (getMessenger() != null) {
            WorldMessengerService.getInstance().silentLeaveMessenger(getMessenger().getId(), new MapleMessengerCharacter(this));
        }
        PlayerBuffStorage.addBuffsToStorage(getId(), getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(getId(), getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(getId(), getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(this), getId(), channel);
        ch.removePlayer(this);
        client.updateLoginState(MapleClient.CHANGE_CHANNEL, client.getSessionIPAddress());
        client.announce(MaplePacketCreator.getChannelChange(client, toch.getPort()));
        saveToCache();
        getMap().removePlayer(this);
        client.setPlayer(null);
        client.setReceiving(false);
    }

    public void expandInventory(byte type, int amount) {
        MapleInventory inv = getInventory(MapleInventoryType.getByType(type));
        inv.addSlot((byte) amount);
        client.announce(InventoryPacket.updateInventorySlotLimit(type, (byte) inv.getSlotLimit()));
    }

    /**
     * 作用：用于判断角色是否可以被显示在地图中！ 判断：角色不等于空，角色不是隐身状态，当前角色管理员等级大于或者等于角色管理员等级
     *
     * @param other 参数 角色
     * @return
     */
    public boolean allowedToTarget(MapleCharacter other) {
        return other != null && (!other.isHidden() || getGMLevel() >= other.getGMLevel());
    }

    public int getFollowId() {
        return followid;
    }

    public void setFollowId(int fi) {
        this.followid = fi;
        if (fi == 0) {
            this.followinitiator = false;
            this.followon = false;
        }
    }

    public boolean isFollowOn() {
        return followon;
    }

    public void setFollowOn(boolean fi) {
        this.followon = fi;
    }

    public boolean isFollowInitiator() {
        return followinitiator;
    }

    public void setFollowInitiator(boolean fi) {
        this.followinitiator = fi;
    }

    public void checkFollow() {
        if (followid <= 0) {
            return;
        }
        if (followon) {
            map.broadcastMessage(MaplePacketCreator.followEffect(id, 0, null));
            map.broadcastMessage(MaplePacketCreator.followEffect(followid, 0, null));
        }
        MapleCharacter tt = map.getCharacterById(followid);
        client.announce(MaplePacketCreator.getFollowMessage("已停止跟随。"));
        if (tt != null) {
            tt.setFollowId(0);
            tt.getClient().announce(MaplePacketCreator.getFollowMessage("已停止跟随。"));
        }
        setFollowId(0);
    }

    public int getMarriageId() {
        return marriageId;
    }

    public void setMarriageId(int mi) {
        this.marriageId = mi;
    }

    public int getMarriageItemId() {
        return marriageItemId;
    }

    public void setMarriageItemId(int mi) {
        this.marriageItemId = mi;
    }

    public boolean isStaff() {
        return this.gmLevel >= PlayerGMRank.INTERN.getLevel();
    }

    public boolean isDonator() {
        return this.gmLevel >= PlayerGMRank.DONATOR.getLevel();
    }

    public boolean startPartyQuest(int questid) {
        boolean ret = false;
        MapleQuest q = MapleQuest.getInstance(questid);
        if (q == null || !q.isPartyQuest()) {
            return false;
        }
        if (!quests.containsKey(questid) || !questinfo.containsKey(questid)) {
            MapleQuestStatus status = getQuestNAdd(q);
            status.setStatus((byte) 1);
            updateQuest(status);
            switch (questid) {
                case 1300: //[竞争活动]阿里安特竞技场
                case 1301: //[竞争活动]怪物嘉年华
                case 1302: //[竞争活动]第2届怪物嘉年华
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0;gvup=0;vic=0;lose=0;draw=0");
                    break;
                case 1303: //[竞争活动]雾海幽灵船
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0;vic=0;lose=0");
                    break;
                case 1204: //[组队任务]海盗船组队任务
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;have2=0;have3=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                case 1206: //[组队任务]毒雾森林
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have0=0;have1=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
                default:
                    updateInfoQuest(questid, "min=0;sec=0;date=0000-00-00;have=0;rank=F;try=0;cmp=0;CR=0;VR=0");
                    break;
            }
            ret = true;
        }
        return ret;
    }

    public String getOneInfo(int questid, String key) {
        if (!questinfo.containsKey(questid) || key == null || MapleQuest.getInstance(questid) == null) {
            return null;
        }
        String[] split = questinfo.get(questid).split(";");
        for (String x : split) {
            String[] split2 = x.split("="); //should be only 2
            if (split2.length == 2 && split2[0].equals(key)) {
                return split2[1];
            }
        }
        return null;
    }

    public void updateOneInfo(int questid, String key, String value) {
        if (key == null || value == null || MapleQuest.getInstance(questid) == null) {
            return;
        }
        if (!questinfo.containsKey(questid)) {
            questinfo.put(questid, key + "=" + value);
            updateInfoQuest(questid, key + "=" + value);
            return;
        }
        String[] split = questinfo.get(questid).split(";");
        boolean changed = false;
        StringBuilder newQuest = new StringBuilder();
        for (String x : split) {
            String[] split2 = x.split("="); //should be only 2
            if (split2.length != 2) {
                continue;
            }
            if (split2[0].equals(key)) {
                newQuest.append(key).append("=").append(value);
                changed = true;
            } else {
                newQuest.append(x);
            }
            newQuest.append(";");
        }
        if (!changed) {
            newQuest.append(key).append("=").append(value);
        }
        updateInfoQuest(questid, changed ? newQuest.toString().substring(0, newQuest.toString().length() - 1) : newQuest.toString());
    }

    public void recalcPartyQuestRank(int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        if (!startPartyQuest(questid)) {
            String oldRank = getOneInfo(questid, "rank");
            if (oldRank == null || oldRank.equals("S")) {
                return;
            }
            String newRank;
            switch (oldRank) {
                case "A":
                    newRank = "S";
                    break;
                case "B":
                    newRank = "A";
                    break;
                case "C":
                    newRank = "B";
                    break;
                case "D":
                    newRank = "C";
                    break;
                case "F":
                    newRank = "D";
                    break;
                default:
                    return;
            }
            List<Pair<String, Pair<String, Integer>>> questInfo = MapleQuest.getInstance(questid).getInfoByRank(newRank);
            if (questInfo == null) {
                return;
            }
            for (Pair<String, Pair<String, Integer>> q : questInfo) {
                boolean found = false;
                String val = getOneInfo(questid, q.right.left);
                if (val == null) {
                    return;
                }
                int vall;
                try {
                    vall = Integer.parseInt(val);
                } catch (NumberFormatException e) {
                    return;
                }
                switch (q.left) {
                    case "less":
                        found = vall < q.right.right;
                        break;
                    case "more":
                        found = vall > q.right.right;
                        break;
                    case "equal":
                        found = vall == q.right.right;
                        break;
                }
                if (!found) {
                    return;
                }
            }
            updateOneInfo(questid, "rank", newRank);
        }
    }

    public void tryPartyQuest(int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            pqStartTime = System.currentTimeMillis();
            updateOneInfo(questid, "try", String.valueOf(Integer.parseInt(getOneInfo(questid, "try")) + 1));
        } catch (Exception e) {
            System.out.println("tryPartyQuest error");
        }
    }

    public void endPartyQuest(int questid) {
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        try {
            startPartyQuest(questid);
            if (pqStartTime > 0) {
                long changeTime = System.currentTimeMillis() - pqStartTime;
                int mins = (int) (changeTime / 1000 / 60), secs = (int) (changeTime / 1000 % 60);
                int mins2 = Integer.parseInt(getOneInfo(questid, "min"));
                if (mins2 <= 0 || mins < mins2) {
                    updateOneInfo(questid, "min", String.valueOf(mins));
                    updateOneInfo(questid, "sec", String.valueOf(secs));
                    updateOneInfo(questid, "date", DateUtil.getCurrentDate());
                }
                int newCmp = Integer.parseInt(getOneInfo(questid, "cmp")) + 1;
                updateOneInfo(questid, "cmp", String.valueOf(newCmp));
                updateOneInfo(questid, "CR", String.valueOf((int) Math.ceil((newCmp * 100.0) / Integer.parseInt(getOneInfo(questid, "try")))));
                recalcPartyQuestRank(questid);
                pqStartTime = 0;
            }
        } catch (Exception e) {
            System.out.println("endPartyQuest error");
        }
    }

    public void havePartyQuest(int itemId) {
        int questid, index = -1;
        switch (itemId) {
            case 1002798:
                questid = 1200; //[组队任务]迎月花保护月妙组队任务
                break;
            case 1072369:
                questid = 1201; //[组队任务]组队挑战任务
                break;
            case 1022073:
                questid = 1202; //[组队任务]玩具城组队任务
                break;
            case 1082232:
                questid = 1203; //[组队任务]女神塔组队任务
                break;
            case 1002571:
            case 1002572:
            case 1002573:
            case 1002574:
                questid = 1204; //[组队任务]海盗船组队任务
                index = itemId - 1002571;
                break;
            case 1102226:
                questid = 1303; //[竞争活动]雾海幽灵船
                break;
            case 1102227:
                questid = 1303; //[竞争活动]雾海幽灵船
                index = 0;
                break;
            case 1122010:
                questid = 1205; //[组队任务]拯救罗密欧和朱丽叶
                break;
            case 1032061:
            case 1032060:
                questid = 1206; //[组队任务]毒雾森林
                index = itemId - 1032060;
                break;
            case 3010018:
                questid = 1300; //[竞争活动]阿里安特竞技场
                break;
            case 1122007:
                questid = 1301; //[竞争活动]怪物嘉年华
                break;
            case 1122058:
                questid = 1302; //[竞争活动]第2届怪物嘉年华
                break;
            default:
                return;
        }
        if (MapleQuest.getInstance(questid) == null || !MapleQuest.getInstance(questid).isPartyQuest()) {
            return;
        }
        startPartyQuest(questid);
        updateOneInfo(questid, "have" + (index == -1 ? "" : index), "1");
    }

    public boolean hasSummon() {
        return hasSummon;
    }

    public void setHasSummon(boolean summ) {
        this.hasSummon = summ;
    }

    public void removeDoor() {
        MapleDoor door = getDoors().iterator().next();
        for (MapleCharacter chr : door.getTarget().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (MapleCharacter chr : door.getTown().getCharactersThreadsafe()) {
            door.sendDestroyData(chr.getClient());
        }
        for (MapleDoor destroyDoor : getDoors()) {
            door.getTarget().removeMapObject(destroyDoor);
            door.getTown().removeMapObject(destroyDoor);
        }
        clearDoors();
    }

    public void removeMechDoor() {
        for (MechDoor destroyDoor : getMechDoors()) {
            for (MapleCharacter chr : getMap().getCharactersThreadsafe()) {
                destroyDoor.sendDestroyData(chr.getClient());
            }
            getMap().removeMapObject(destroyDoor);
        }
        clearMechDoors();
    }

    public void changeRemoval() {
        changeRemoval(false);
    }

    public void changeRemoval(boolean dc) {
        if (getCheatTracker() != null && dc) {
            getCheatTracker().dispose();
        }
        dispelSummons();
        if (!dc) {
            cancelEffectFromBuffStat(MapleBuffStat.飞翔);
            cancelEffectFromBuffStat(MapleBuffStat.骑兽技能);
            cancelEffectFromBuffStat(MapleBuffStat.金属机甲);
            cancelEffectFromBuffStat(MapleBuffStat.恢复效果);
            cancelEffectFromBuffStat(MapleBuffStat.精神连接);
            cancelEffectFromBuffStat(MapleBuffStat.飞行骑乘);
        }
        if (getPyramidSubway() != null) {
            getPyramidSubway().dispose(this);
        }
        if (playerShop != null && !dc) {
            playerShop.removeVisitor(this);
            if (playerShop.isOwner(this)) {
                playerShop.setOpen(true);
            }
        }
        if (!getDoors().isEmpty()) {
            removeDoor();
        }
        if (!getMechDoors().isEmpty()) {
            removeMechDoor();
        }
        NPCScriptManager.getInstance().dispose(client);
        cancelFairySchedule(false);
    }

    //------------------------------------------------------------------------------------

    public void updateTick(int newTick) {
        anticheat.updateTick(newTick);
    }

    // 用于防止点击NPC过快掉线-----------------------------------------------------------
    public long getCurrenttime() {
        return this.currenttime;
    }

    public void setCurrenttime(final long currenttime) {
        this.currenttime = currenttime;
    }

    public long getDeadtime() {
        return this.deadtime;
    }

    public void setDeadtime(final long deadtime) {
        this.deadtime = deadtime;
    }

    public long getLasttime() {
        return this.lasttime;
    }

    public void setLasttime(final long lasttime) {
        this.lasttime = lasttime;
    }

    public boolean canUseFamilyBuff(MapleFamilyBuff buff) {
        MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(buff.questID));
        if (stat == null) {
            return true;
        }
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData()) + (24 * 3600000) < System.currentTimeMillis();
    }

    public void useFamilyBuff(MapleFamilyBuff buff) {
        MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(buff.questID));
        stat.setCustomData(String.valueOf(System.currentTimeMillis()));
    }

    public List<Integer> usedBuffs() {
        //assume count = 1
        List<Integer> used = new ArrayList<>();
        MapleFamilyBuff[] z = MapleFamilyBuff.values();
        for (int i = 0; i < z.length; i++) {
            if (!canUseFamilyBuff(z[i])) {
                used.add(i);
            }
        }
        return used;
    }

    public String getTeleportName() {
        return teleportname;
    }

    public void setTeleportName(String tname) {
        teleportname = tname;
    }

    public int getNoJuniors() {
        if (mfc == null) {
            return 0;
        }
        return mfc.getNoJuniors();
    }

    public MapleFamilyCharacter getMFC() {
        return mfc;
    }

    public void makeMFC(int familyid, int seniorid, int junior1, int junior2) {
        if (familyid > 0) {
            MapleFamily f = WorldFamilyService.getInstance().getFamily(familyid);
            if (f == null) {
                mfc = null;
            } else {
                mfc = f.getMFC(id);
                if (mfc == null) {
                    mfc = f.addFamilyMemberInfo(this, seniorid, junior1, junior2);
                }
                if (mfc.getSeniorId() != seniorid) {
                    mfc.setSeniorId(seniorid);
                }
                if (mfc.getJunior1() != junior1) {
                    mfc.setJunior1(junior1);
                }
                if (mfc.getJunior2() != junior2) {
                    mfc.setJunior2(junior2);
                }
            }
        } else {
            mfc = null;
        }
    }

    public void setFamily(int newf, int news, int newj1, int newj2) {
        if (mfc == null || newf != mfc.getFamilyId() || news != mfc.getSeniorId() || newj1 != mfc.getJunior1() || newj2 != mfc.getJunior2()) {
            makeMFC(newf, news, newj1, newj2);
        }
    }

    public int getGachExp() {
        return gachexp;
    }

    public void setGachExp(int ge) {
        this.gachexp = ge;
    }

    public boolean isInBlockedMap() {
        if (!isAlive() || getPyramidSubway() != null || getMap().getSquadByMap() != null || getEventInstance() != null || getMap().getEMByMap() != null) {
            return true;
        }
        if ((getMapId() >= 680000210 && getMapId() <= 680000502) || (getMapId() / 10000 == 92502 && getMapId() >= 925020100) || (getMapId() / 10000 == 92503) || getMapId() == GameConstants.JAIL) {
            return true;
        }
        return ServerConstants.isBlockedMapFM(getMapId());
    }

    public boolean isInTownMap() {
        if (hasBlockedInventory() || !getMap().isTown() || FieldLimitType.VipRock.check(getMap().getFieldLimit()) || getEventInstance() != null) {
            return false;
        }
        return !ServerConstants.isBlockedMapFM(getMapId());
    }

    public boolean hasBlockedInventory() {
        return !isAlive() || getTrade() != null || getConversation() > 0 || getDirection() >= 0 || getPlayerShop() != null || getBattle() != null || map == null;
    }

    public void startPartySearch(List<Integer> jobs, int maxLevel, int minLevel, int membersNeeded) {
        for (MapleCharacter chr : map.getCharacters()) {
            if (chr.getId() != id && chr.getParty() == null && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel && (jobs.isEmpty() || jobs.contains((int) chr.getJob())) && (isGM() || !chr.isGM())) {
                if (party != null && party.getMembers().size() < 6 && party.getMembers().size() < membersNeeded) {
                    chr.setParty(party);
                    WorldPartyService.getInstance().updateParty(party.getPartyId(), PartyOperation.加入队伍, new MaplePartyCharacter(chr));
                    chr.receivePartyMemberHP();
                    chr.updatePartyMemberHP();
                } else {
                    break;
                }
            }
        }
    }

    public Battler getBattler(int pos) {
        return battlers[pos];
    }

    public Battler[] getBattlers() {
        return battlers;
    }

    public List<Battler> getBoxed() {
        return boxed;
    }

    public PokemonBattle getBattle() {
        return battle;
    }

    public void setBattle(PokemonBattle b) {
        this.battle = b;
    }

    public int countBattlers() {
        int ret = 0;
        for (Battler battler : battlers) {
            if (battler != null) {
                ret++;
            }
        }
        return ret;
    }

    public void changedBattler() {
        changed_pokemon = true;
    }

    public void makeBattler(int index, int monsterId) {
        MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(monsterId);
        this.battlers[index] = new Battler(mons);
        this.battlers[index].setCharacterId(id);
        changed_pokemon = true;
        getMonsterBook().monsterCaught(client, monsterId, mons.getName());
    }

    public boolean removeBattler(int ind) {
        if (countBattlers() <= 1) {
            return false;
        }
        if (ind == battlers.length) {
            this.battlers[ind] = null;
        } else {
            for (int i = ind; i < battlers.length; i++) {
                this.battlers[i] = ((i + 1) == battlers.length ? null : this.battlers[i + 1]);
            }
        }
        changed_pokemon = true;
        return true;
    }

    public int getChallenge() {
        return challenge;
    }

    public void setChallenge(int c) {
        this.challenge = c;
    }

    public short getFatigue() {
        return fatigue;
    }

    public void setFatigue(int j) {
        this.fatigue = (short) Math.max(0, j);
        updateSingleStat(MapleStat.疲劳, this.fatigue);
    }

    public void fakeRelog() {
        MapleMap mapp = getMap();
        mapp.setCheckStates(false);
        mapp.removePlayer(this);
        mapp.addPlayer(this);
        mapp.setCheckStates(true);
        client.announce(MaplePacketCreator.getWarpToMap(this, map, 0));
        client.announce(MaplePacketCreator.serverNotice(5, "刷新人数据完成..."));
    }

    /*
     * 检测幻灵重生是否能召唤怪物
     */
    public boolean canSummon() {
        return canSummon(5000);
    }

    public boolean canSummon(int checkTime) {
        if (lastSummonTime <= 0) {
            prepareSummonTime();
        }
        return lastSummonTime + checkTime < System.currentTimeMillis();
    }

    /*
     * 重置幻灵召唤兽时间间隔
     */
    private void prepareSummonTime() {
        lastSummonTime = System.currentTimeMillis();
    }

    public int getIntNoRecord(int questID) {
        MapleQuestStatus stat = getQuestNoAdd(MapleQuest.getInstance(questID));
        if (stat == null || stat.getCustomData() == null) {
            return 0;
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public int getIntRecord(int questID) {
        MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public long getLongRecord(int questID) {
        MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Long.parseLong(stat.getCustomData());
    }

    public void setLongRecord(int questID, long record) {
        MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(questID));
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        stat.setCustomData(String.valueOf(record));
    }

    public void updatePetAuto() {
        client.announce(MaplePacketCreator.petAutoHP(getIntRecord(GameConstants.HP_ITEM)));
        client.announce(MaplePacketCreator.petAutoMP(getIntRecord(GameConstants.MP_ITEM)));
        client.announce(MaplePacketCreator.petAutoBuff(getIntRecord(GameConstants.BUFF_SKILL)));
    }

    public long getChangeTime() {
        return mapChangeTime;
    }

    public void setChangeTime(boolean changeMap) {
        mapChangeTime = System.currentTimeMillis();
        if (changeMap) {
            getCheatTracker().resetInMapIimeCount();
        }
    }

    public Map<ReportType, Integer> getReports() {
        return reports;
    }

    public void addReport(ReportType type) {
        Integer value = reports.get(type);
        reports.put(type, value == null ? 1 : (value + 1));
        changed_reports = true;
    }

    public void clearReports(ReportType type) {
        reports.remove(type);
        changed_reports = true;
    }

    public void clearReports() {
        reports.clear();
        changed_reports = true;
    }

    public int getReportPoints() {
        int ret = 0;
        for (Integer entry : reports.values()) {
            ret += entry;
        }
        return ret;
    }

    public String getReportSummary() {
        StringBuilder ret = new StringBuilder();
        List<Pair<ReportType, Integer>> offenseList = new ArrayList<>();
        for (Entry<ReportType, Integer> entry : reports.entrySet()) {
            offenseList.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        offenseList.sort((o1, o2) -> {
            int thisVal = o1.getRight();
            int anotherVal = o2.getRight();
            return (thisVal < anotherVal ? 1 : (thisVal == anotherVal ? 0 : -1));
        });
        for (Pair<ReportType, Integer> anOffenseList : offenseList) {
            ret.append(StringUtil.makeEnumHumanReadable(anOffenseList.left.name()));
            ret.append(": ");
            ret.append(anOffenseList.right);
            ret.append(" ");
        }
        return ret.toString();
    }

    public short getScrolledPosition() {
        return scrolledPosition;
    }

    public void setScrolledPosition(short s) {
        this.scrolledPosition = s;
    }

    public MapleTrait getTrait(MapleTraitType t) {
        return traits.get(t);
    }

    public void forceCompleteQuest(int id) {
        MapleQuest.getInstance(id).forceComplete(this, 9270035); //NPC乔伊斯
    }

    /*
     * 矿物(药草)背包
     */
    public Map<Byte, List<Integer>> getAllExtendedSlots() {
        return extendedSlots;
    }

    public List<Integer> getExtendedSlots(byte type) {
        return extendedSlots.get(type);
    }

    public int getExtendedSlot(byte type, int index) {
        if (extendedSlots.isEmpty() || extendedSlots.get(type) == null || extendedSlots.get(type).size() <= index || index < 0) {
            return -1;
        }
        return extendedSlots.get(type).get(index);
    }

    public void changedExtended() {
        changed_extendedSlots = true;
    }

    /*
     * 定义安卓
     */
    public MapleAndroid getAndroid() {
        return android;
    }

    /*
     * 刷出安卓
     */
    public void setAndroid(MapleAndroid a) {
        if (checkHearts()) {
            this.android = a;
            if (map != null && a != null) {
                map.broadcastMessage(AndroidPacket.spawnAndroid(this, a));
                map.broadcastMessage(AndroidPacket.showAndroidEmotion(this.getId(), Randomizer.nextInt(17) + 1));
            }
        }
    }

    /*
     * 移除安卓
     */
    public void removeAndroid() {
        if (map != null) {
            map.broadcastMessage(AndroidPacket.deactivateAndroid(this.id));
        }
        if (android != null) {
            android.saveToDb();
        }
        android = null;
    }

    /*
     * 更新安卓外观
     */
    public void updateAndroid(int size, int itemId) {
        if (map != null) {
            map.broadcastMessage(AndroidPacket.updateAndroidLook(this.getId(), size, itemId));
        }
    }

    /*
     * 检测是否有机器人心脏 安卓 -34 机器人心脏 -35
     */
    public boolean checkHearts() {
        return getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -35) != null;
    }

    /*
     * 回购道具
     */
    public List<MapleShopItem> getRebuy() {
        return rebuy;
    }

    public Map<Integer, MonsterFamiliar> getFamiliars() {
        return familiars;
    }

    public void updateFamiliarsIndex() {
        getClient().announce(FamiliarPacket.updateFamiliar(this));
    }

    public MonsterFamiliar getSummonedFamiliar() {
        return summonedFamiliar;
    }

    public void setSummonedFamiliar(MonsterFamiliar summonedFamiliar) {
        this.summonedFamiliar = summonedFamiliar;
    }

    public void removeFamiliar() {
        if (summonedFamiliar != null && map != null) {
            this.removeVisibleFamiliar();
        }
        this.summonedFamiliar = null;
    }

    public void removeVisibleFamiliar() {
        this.getMap().removeMapObject(summonedFamiliar);
        this.removeVisibleMapObject(summonedFamiliar);
        this.getMap().broadcastMessage(FamiliarPacket.removeFamiliar(getId()));
    }

    public void spawnFamiliar(MonsterFamiliar mf) {
        this.summonedFamiliar = mf;
        mf.setStance(0);
        mf.setPosition(this.getPosition());
        mf.setFh(this.getFH());
        this.addVisibleMapObject(mf);
        this.getMap().spawnFamiliar(mf);
    }

    /*
     * 道具宝宝
     */
    public MapleImp[] getImps() {
        return imps;
    }

    public void sendImp() {
        for (int i = 0; i < imps.length; i++) {
            if (imps[i] != null) {
                client.announce(MaplePacketCreator.updateImp(imps[i], ImpFlag.SUMMONED.getValue(), i, true));
            }
        }
    }

    public int getBattlePoints() {
        return pvpPoints;
    }

    public void setBattlePoints(int p) {
        if (p != pvpPoints) {
            client.announce(UIPacket.getBPMsg(p - pvpPoints));
            updateSingleStat(MapleStat.BATTLE_POINTS, p);
        }
        this.pvpPoints = p;
    }

    public int getTotalBattleExp() {
        return pvpExp;
    }

    public void setTotalBattleExp(int p) {
        int previous = pvpExp;
        this.pvpExp = p;
        if (p != previous) {
            stats.recalcPVPRank(this);
            updateSingleStat(MapleStat.BATTLE_EXP, stats.pvpExp);
            updateSingleStat(MapleStat.BATTLE_RANK, stats.pvpRank);
        }
    }

    public void changeTeam(int newTeam) {
        this.coconutteam = newTeam;
        if (inPVP()) {
            //client.announce(MaplePacketCreator.getPVPTransform(newTeam + 1));
            //map.broadcastMessage(MaplePacketCreator.changeTeam(id, newTeam + 1));
        } else {
            client.announce(MaplePacketCreator.showEquipEffect(newTeam));
        }
    }

    public void disease(int type, int level) {
        if (MapleDisease.getBySkill(type) == null) {
            return;
        }
        chair = 0;
        chairMsg = "";
        chairType = 0;
        client.announce(MaplePacketCreator.cancelChair(-1));
        map.broadcastMessage(this, MaplePacketCreator.showChair(this, 0, ""), false);
        giveDebuff(MapleDisease.getBySkill(type), MobSkillFactory.getMobSkill(type, level));
    }

    public boolean inPVP() {
        return eventInstance != null && eventInstance.getName().startsWith("PVP") && ServerConfig.CHANNEL_OPENPVP;
    }

    public long getCooldownLimit(int skillid) {
        for (MapleCoolDownValueHolder mcdvh : getAllCooldowns()) {
            if (mcdvh.skillId == skillid) {
                return System.currentTimeMillis() - mcdvh.startTime;
            }
        }
        return 0;
    }

    public List<MapleCoolDownValueHolder> getAllCooldowns() {
        List<MapleCoolDownValueHolder> ret = new ArrayList<>();
        for (MapleCoolDownValueHolder mcdvh : coolDowns.values()) {
            ret.add(new MapleCoolDownValueHolder(mcdvh.skillId, mcdvh.startTime, mcdvh.length));
        }
        return ret;
    }

    public void clearAllCooldowns() {
        for (MapleCoolDownValueHolder m : getCooldowns()) {
            int skil = m.skillId;
            removeCooldown(skil);
            client.announce(MaplePacketCreator.skillCooldown(skil, 0));
        }
    }

    /*
     * 伤害减少设置处理
     */
    public Pair<Double, Boolean> modifyDamageTaken(double damage, MapleMapObject attacke) {
        Pair<Double, Boolean> ret = new Pair<>(damage, false);
        if (damage < 0) {
            return ret;
        }
        if (stats.ignoreDAMr > 0 && Randomizer.nextInt(100) < stats.ignoreDAMr_rate) { //受到攻击时，有#prop%的概率无视伤害的#ignoreDAMr%
            damage -= Math.floor((stats.ignoreDAMr * damage) / 100.0f);
        }
        if (stats.ignoreDAM > 0 && Randomizer.nextInt(100) < stats.ignoreDAM_rate) { //受到攻击时，有#prop%的概率无视#ignoreDAM的伤害
            damage -= stats.ignoreDAM;
        }
        Integer div = getBuffedValue(MapleBuffStat.祝福护甲);
        Integer div2 = getBuffedValue(MapleBuffStat.神圣魔法盾);
        if (div2 != null) {
            if (div2 <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.神圣魔法盾);
            } else {
                setBuffedValue(MapleBuffStat.神圣魔法盾, div2 - 1);
                damage = 0;
            }
        } else if (div != null) {
            if (div <= 0) {
                cancelEffectFromBuffStat(MapleBuffStat.祝福护甲);
            } else {
                setBuffedValue(MapleBuffStat.祝福护甲, div - 1);
                damage = 0;
            }
        }
        //好像这个BUFF吸收伤害20%
        if (getBuffedValue(MapleBuffStat.寒冰灵气) != null) {
            damage -= damage * (20 / 100.0);
        }
        List<Integer> attack = attacke instanceof MapleMonster || attacke == null ? null : (new ArrayList<>());
        if (damage > 0) {
            if (getJob() == 122 && !skillisCooling(圣骑士.祝福护甲)) { //祝福护甲 - 受到攻击时有一定几率生成个人保护膜，最多吸收5次伤害，提高角色的物理攻击力。保护膜最多维持60秒时间，一次发动后，在一定时间内无法再生成。
                Skill divine = SkillFactory.getSkill(圣骑士.祝福护甲);
                if (getTotalSkillLevel(divine) > 0) {
                    MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.announce(MaplePacketCreator.skillCooldown(圣骑士.祝福护甲, divineShield.getCooldown(this)));
                        addCooldown(圣骑士.祝福护甲, System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                    }
                }
            } else if (getJob() == 2005 || getJob() == 2500 || getJob() == 2510 || getJob() == 2511 || getJob() == 2512) {
                Skill skill = SkillFactory.getSkill(隐月.精灵凝聚第1招);
                if (getTotalSkillLevel(skill) > 0) {
                    MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
                    //System.err.println("隐月 - 当前伤害: " + damage + " 技能减少: " + effect.getDamAbsorbShieldR() + " 百分比: " + (effect.getDamAbsorbShieldR() / 100.0));
                    damage -= damage * (effect.getDamAbsorbShieldR() / 100.0);
                    //System.err.println("隐月 - 最终伤害: " + damage);
                }
            } else if (getJob() == 2112) {
                Skill achilles = SkillFactory.getSkill(战神.防守策略);
                if (getTotalSkillLevel(achilles) > 0) {
                    MapleStatEffect multiplier = achilles.getEffect(getTotalSkillLevel(achilles));
                    damage = ((multiplier.getX() / 1000.0) * damage);
                }
            } else if (getJob() == 111 || getJob() == 112) {
                handleOrbgain(true);
            } else if (getJob() == 321 || getJob() == 322) {
                MapleStatEffect effect = getStatForBuff(MapleBuffStat.伤害置换);
                if (effect != null && getBuffedValue(MapleBuffStat.伤害置换) != null) {
                    int buffHp = getBuffedValue(MapleBuffStat.伤害置换);
                    if (buffHp > 0) {
                        int xishou = (int) (damage * effect.makeRate(effect.getX())); //吸收的伤害
                        int maxxishou = (int) (stats.getCurrentMaxHp() * effect.makeRate(effect.getZ())); //最大
                        if (xishou > maxxishou) {
                            xishou = maxxishou;
                        }
                        damage -= xishou;
                        buffHp -= xishou;
                    }
                    if (buffHp > 0) {
                        setBuffedValue(MapleBuffStat.伤害置换, buffHp);
                    } else {
                        cancelEffectFromBuffStat(MapleBuffStat.伤害置换);
                    }
                }
            } else if (getJob() == 2710 || getJob() == 2711 || getJob() == 2712) {
                lastBlessOfDarknessTime = System.currentTimeMillis();
                Skill skill = SkillFactory.getSkill(夜光.黑暗祝福);
                if (getTotalSkillLevel(skill) > 0 && getBuffedValue(MapleBuffStat.黑暗祝福) != null) {
                    MapleStatEffect effect = skill.getEffect(getTotalSkillLevel(skill));
                    handleBlackBlessLost(1);
                    damage = ((effect.getX() / 100.0) * damage);
                }
            } else if (getJob() == 3112) {
                Skill divine = SkillFactory.getSkill(恶魔猎手.皮肤硬化);
                if (getTotalSkillLevel(divine) > 0) {
                    MapleStatEffect eff = divine.getEffect(getTotalSkillLevel(divine));
                    damage = ((eff.getX() / 1000.0) * damage);
                }
            } else if (getJob() == 5112) {
                Skill divine = SkillFactory.getSkill(米哈尔.进阶灵魂盾);
                if (getTotalSkillLevel(divine) > 0) {
                    MapleStatEffect eff = divine.getEffect(getTotalSkillLevel(divine));
                    damage = ((eff.getX() / 1000.0) * damage);
                }
            } else if (getJob() == 3611 || getJob() == 3612) {
                MapleStatEffect effect = getStatForBuff(MapleBuffStat.影分身);
                if (effect != null && effect.getSourceid() == 尖兵.全息投影) {
                    int 分身血量 = getBuffedValue(MapleBuffStat.影分身);
                    分身血量 -= damage;
                    if (分身血量 > 0) {
                        setBuffedValue(MapleBuffStat.影分身, 分身血量);
                    } else {
                        dispelSkill(尖兵.全息投影);
                    }
                }
            } else if (getJob() == 433 || getJob() == 434) {
                Skill divine = SkillFactory.getSkill(双刀.进阶隐身术);
                if (getTotalSkillLevel(divine) > 0 && getBuffedValue(MapleBuffStat.隐身术) == null && !skillisCooling(divine.getId())) {
                    MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (Randomizer.nextInt(100) < divineShield.getX()) {
                        divineShield.applyTo(this);
                    }
                }
            } else if ((getJob() == 512 || getJob() == 522 || getJob() == 582 || getJob() == 592 || getJob() == 572) && getBuffedValue(MapleBuffStat.反制攻击) == null) {
                Skill divine = SkillFactory.getSkill(getJob() == 512 ? 冲锋队长.反制攻击 : 冲锋队长.反制攻击);
                int skilllevel = getTotalSkillLevel(divine);
                if (skilllevel > 0 && !skillisCooling(divine.getId())) {
                    MapleStatEffect divineShield = divine.getEffect(skilllevel);
                    if (divineShield.makeChanceResult()) {
                        divineShield.applyTo(this);
                        client.announce(EffectPacket.showBuffeffect(this, divine.getId(), EffectOpcode.UserEffect_SkillAffected.getValue(), 0, skilllevel));
                        getMap().broadcastMessage(EffectPacket.showOwnBuffEffect(divine.getId(), EffectOpcode.UserEffect_SkillAffected.getValue(), 0, skilllevel));
                        client.announce(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getX()));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getX() * 1000);
                    }
                }
            } else if ((getJob() == 531 || getJob() == 532) && attacke != null) {
                Skill divine = SkillFactory.getSkill(神炮王.反击炮);
                if (getTotalSkillLevel(divine) > 0) {
                    MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        if (attacke instanceof MapleMonster) {
                            MapleMonster attacker = (MapleMonster) attacke;
                            int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add(divineShield.getDamage());
                        }
                    }
                }
            } else if (getJob() == 132 && attacke != null) {
                Skill divine = SkillFactory.getSkill(黑骑士.灵魂复仇);
                if (getTotalSkillLevel(divine) > 0 && !skillisCooling(divine.getId()) && getBuffSource(MapleBuffStat.灵魂助力) == 黑骑士.灵魂助力) {
                    MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    if (divineShield.makeChanceResult()) {
                        client.announce(MaplePacketCreator.skillCooldown(divine.getId(), divineShield.getCooldown(this)));
                        addCooldown(divine.getId(), System.currentTimeMillis(), divineShield.getCooldown(this) * 1000);
                        if (attacke instanceof MapleMonster) {
                            MapleMonster attacker = (MapleMonster) attacke;
                            int theDmg = (int) (divineShield.getDamage() * getStat().getCurrentMaxBaseDamage() / 100.0);
                            attacker.damage(this, theDmg, true);
                            getMap().broadcastMessage(MobPacket.damageMonster(attacker.getObjectId(), theDmg));
                        } else {
                            MapleCharacter attacker = (MapleCharacter) attacke;
                            attacker.addHP(-divineShield.getDamage());
                            attack.add(divineShield.getDamage());
                        }
                    }
                }
            }
            if (attacke != null) {
                int damr = (Randomizer.nextInt(100) < getStat().DAMreflect_rate ? getStat().DAMreflect : 0) + (getBuffedValue(MapleBuffStat.伤害反击) != null ? getBuffedValue(MapleBuffStat.伤害反击) : 0);
                if (damr > 0) {
                    long bouncedamage = (long) (damage * damr / 100);
                    if (attacke instanceof MapleMonster) {
                        MapleMonster attacker = (MapleMonster) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getMobMaxHp() / 10);
                        attacker.damage(this, bouncedamage, true);
                        getMap().broadcastMessage(this, MobPacket.damageMonster(attacker.getObjectId(), bouncedamage), getTruePosition());
                        if (getBuffSource(MapleBuffStat.伤害反击) == 恶魔猎手.黑暗复仇) {
                            MapleStatEffect eff = this.getStatForBuff(MapleBuffStat.伤害反击);
                            attacker.applyStatus(this, new MonsterStatusEffect(MonsterStatus.MOB_STAT_Stun, 1, eff.getSourceid(), null, false, 0), false, 5000, true, eff); //只有5秒
                        }
                    } else {
                        MapleCharacter attacker = (MapleCharacter) attacke;
                        bouncedamage = Math.min(bouncedamage, attacker.getStat().getCurrentMaxHp() / 10);
                        attacker.addHP(-((int) bouncedamage));
                        //log.info("减少Hp: " + ((int) bouncedamage));
                        attack.add((int) bouncedamage);
                        if (getBuffSource(MapleBuffStat.伤害反击) == 恶魔猎手.黑暗复仇) {
                            attacker.disease(MapleDisease.昏迷.getDisease(), 1);
                        }
                    }
                    ret.right = true;
                }
                if ((getJob() == 411 || getJob() == 412 || getJob() == 421 || getJob() == 422 || getJob() == 1411 || getJob() == 1412) && getBuffedValue(MapleBuffStat.召唤兽) != null) {
                    List<MapleSummon> ss = getSummonsReadLock();
                    try {
                        for (MapleSummon sum : ss) {
                            if (sum.getTruePosition().distanceSq(getTruePosition()) < 400000.0 && sum.is黑暗杂耍()) {
                                if (attacke instanceof MapleMonster) {
                                    List<Pair<Long, Boolean>> allDamageNumbers = new ArrayList<>();
                                    List<AttackPair> allDamage = new ArrayList<>();
                                    MapleMonster attacker = (MapleMonster) attacke;
                                    long theDmg = (long) (SkillFactory.getSkill(sum.getSkillId()).getEffect(sum.getSkillLevel()).getX() * damage / 100.0);
                                    allDamageNumbers.add(new Pair<>(theDmg, false));
                                    allDamage.add(new AttackPair(attacker.getObjectId(), allDamageNumbers));
                                    getMap().broadcastMessage(SummonPacket.summonAttack(sum, (byte) 0x84, (byte) 0x11, allDamage, getLevel(), true));
                                    attacker.damage(this, theDmg, true);
                                    checkMonsterAggro(attacker);
                                    if (!attacker.isAlive()) {
                                        getClient().announce(MobPacket.killMonster(attacker.getObjectId(), 1));
                                    }
                                } else {
                                    MapleCharacter chr = (MapleCharacter) attacke;
                                    int dmg = SkillFactory.getSkill(sum.getSkillId()).getEffect(sum.getSkillLevel()).getX();
                                    chr.addHP(-dmg);
                                    attack.add(dmg);
                                }
                            }
                        }
                    } finally {
                        unlockSummonsReadLock();
                    }
                }
            }
        } else if (damage == 0) {
            if ((getJob() == 433 || getJob() == 434) && attacke != null) {
                Skill divine = SkillFactory.getSkill(双刀.影子闪避);
                if (getTotalSkillLevel(divine) > 0) {
                    MapleStatEffect divineShield = divine.getEffect(getTotalSkillLevel(divine));
                    int prop = getTotalSkillLevel(divine) + 10;
                    if (Randomizer.nextInt(100) < prop) {
                        divineShield.applyTo(this);
                        getCheatTracker().resetTakeDamage();
                        getClient().announce(MaplePacketCreator.sendCritAttack());
                    }
                }
            }
        }
        if ((getJob() == 3611 || getJob() == 3612)) {
            if (this.getBuffStatValueHolder(MapleBuffStat.宙斯盾系统) != null && attacke != null) {
                handleAegisSystem(attacke.getObjectId());
            }
        }
        ret.left = damage;
        return ret;
    }

    /**
     * 怪物死亡前的处理
     */
    public void beforeKillMonster(int moboid, int skillid) {
        /* 角色是恶魔猎手时吸收精气 */
        if (JobConstants.is恶魔猎手(job) && skillid != 新手.升级特效) {
            handleForceGain(moboid, 恶魔猎手.死亡诅咒);
        } else if (JobConstants.is唤灵斗师(job)) {
            handleDeathPact(moboid, false);
        }
    }

    public void applyIceGage(int x) {
        updateSingleStat(MapleStat.ICE_GAGE, x);
    }

    public Rectangle getBounds() {
        return new Rectangle(getTruePosition().x - 25, getTruePosition().y - 75, 50, 75);
    }

    /*
     * 精灵的祝福
     */
    public boolean getCygnusBless() {
        int jobid = getJob();
        return getSkillLevel(12) > 0 && (jobid >= 0 && jobid < 1000) //冒险家
                || getSkillLevel(10000012) > 0 && (jobid >= 1000 && jobid < 2000) //骑士团
                || getSkillLevel(20000012) > 0 && (jobid == 2000 || jobid >= 2100 && jobid <= 2112) //战神
                || getSkillLevel(20010012) > 0 && (jobid == 2001 || JobConstants.is龙神(jobid)) //龙神
                || getSkillLevel(20020012) > 0 && (jobid == 2002 || jobid >= 2300 && jobid <= 2312) //双弩精灵
                || getSkillLevel(20030012) > 0 && (jobid == 2003 || jobid >= 2400 && jobid <= 2412) //幻影神偷
                || getSkillLevel(20040012) > 0 && (jobid == 2004 || jobid >= 2700 && jobid <= 2712) //夜光法师
                || getSkillLevel(30000012) > 0 && (jobid == 3000 || jobid >= 3200 && jobid <= 3512) //反抗职业
                || getSkillLevel(30010012) > 0 && (JobConstants.is恶魔(jobid)) //恶魔职业
                || getSkillLevel(30020012) > 0 && (jobid == 3002 || jobid >= 3600 && jobid <= 3612) //尖兵
                || getSkillLevel(50000012) > 0 && (jobid == 5000 || jobid >= 5100 && jobid <= 5112) //米哈尔
                || getSkillLevel(60000012) > 0 && (jobid == 6000 || jobid >= 6100 && jobid <= 6112) //狂龙战士
                || getSkillLevel(60010012) > 0 && (jobid == 6001 || jobid >= 6500 && jobid <= 6512) //爆莉萌天使
                || getSkillLevel(100000012) > 0 && (jobid == 10000 || jobid >= 10100 && jobid <= 10112) //神之子
                || getSkillLevel(110000012) > 0 && (jobid == 11000 || jobid >= 11200 && jobid <= 11212) //林之灵
                ;
    }

    public byte get精灵祝福() {
        int jobid = getJob();
        if (getSkillLevel(20021110) > 0 && (jobid == 2002 || jobid >= 2300 && jobid <= 2312) || getSkillLevel(80001040) > 0) {
            return 10;
        }
        return 0;
    }

    /*
     * 恶魔猎手吸收精气
     */
    public void handleForceGain(int oid, int skillid) {
        handleForceGain(oid, skillid, 0);
    }

    public void handleForceGain(int moboid, int skillid, int extraForce) {
        if (!SkillConstants.isForceIncrease(skillid) && extraForce <= 0) {
            return;
        }
        int forceGain = Math.max(3, Randomizer.nextInt(5) + 1), forceColor = 3;

        MapleMonster mob = getMap().getMonsterByOid(moboid);
        if (mob != null && mob.getStats().isBoss()) {
            forceGain = 10;
            forceColor = 10;
        } else {
            if (skillid == 恶魔猎手.恶魔血月斩 || skillid == 恶魔猎手.恶魔血月斩1 || skillid == 恶魔猎手.恶魔血月斩2 || skillid == 恶魔猎手.恶魔血月斩3) {
                int skilllevel = getSkillLevel(恶魔猎手.极限精气吸收);
                if (skilllevel > 0) {
                    MapleStatEffect effect = SkillFactory.getSkill(恶魔猎手.极限精气吸收).getEffect(skilllevel);
                    if (Randomizer.nextInt(100) > effect.getProp()) {
                        return;
                    }
                }
            } else if (skillid == 恶魔猎手.死亡诅咒) {
                forceColor = 5;
            }
        }
        forceGain = extraForce > 0 ? extraForce : forceGain;
        specialStats.addForceCounter(forceGain);
        getClient().announce(MaplePacketCreator.showForce(this, moboid, specialStats.getForceCounter(), forceColor));
    }

    /*
     * 幻影卡片系统
     */
    public int getCardStack() {
        return specialStats.getCardStack();
    }

    public void setCardStack(int amount) {
        specialStats.setCardStack(amount);
    }

    /*
     * 幻影卡片数量
     */
    public int getCarteByJob() {
        if (getSkillLevel(幻影.卡牌审判_高级) > 0) {
            return 40;
        } else if (getSkillLevel(幻影.卡牌审判) > 0) {
            return 20;
        }
        return 0;
    }

    public void handleCarteGain(int moid, boolean is5th) {
        if (is5th) {
            Skill skill = SkillFactory.getSkill(幻影.小丑_1);
            if (skill != null && getSkillLevel(幻影.王牌) > 0 && (skill.getEffect(getSkillLevel(幻影.王牌))) != null) {
                MapleForce force = new MapleForce(id, 幻影.小丑_1, 0, MapleForceType.幻影卡牌, (byte) 1, Collections.singletonList(moid), (byte) 2, 10, getMap());
                specialStats.gainCardStack();
                send(MaplePacketCreator.updateCardStack(specialStats.getCardStack()));
                send(ForcePacket.showForce(force));
            }
        } else {
            int[] arrn2 = new int[]{幻影.黑色秘卡, 幻影.卡片雪舞};
            for (int skillid : arrn2) {
                Skill skill = SkillFactory.getSkill(skillid);
                if (skill != null && getSkillLevel(skill) > 0) {
                    MapleStatEffect effect = skill.getEffect(getSkillLevel(skill));
                    if (effect.makeChanceResult() && Randomizer.nextInt(100) <= getStat().passive_sharpeye_rate()) {
                        MapleForce force = new MapleForce(id, 幻影.小丑_1, 0, MapleForceType.幻影卡牌, (byte) 1, Collections.singletonList(moid), (byte) (skillid == 幻影.黑色秘卡 ? 2 : 1), 1, getMap());
                        send(ForcePacket.showForce(force));
                        if (getCardStack() < getCarteByJob()) {
                            specialStats.gainCardStack();
                            send(MaplePacketCreator.updateCardStack(specialStats.getCardStack()));
                            return;
                        }
                    }
                }
            }
        }
    }

    /*
     * 处理尖兵 精准火箭
     */
    public void handleCardStack(int skillId) {
        if (skillId == 0 || skillId == 尖兵.精准火箭 || skillId == 尖兵.宙斯盾系统_攻击) {
            return;
        }
        Skill skill = SkillFactory.getSkill(尖兵.精准火箭);
        if (getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(getSkillLevel(skill));
            // 冷却时间3秒
            if (System.currentTimeMillis() - lastForceTime < 3000) {
                return;
            }
            if (effect != null) {
                List<MapleMapObject> mobs = getMap().getMonstersInRange(getPosition(), 38400);
                List<Integer> mobids = new ArrayList<>();
                if (mobs.isEmpty()) {
                    return;
                }
                for (int i = 0; i < effect.getMobCount(); i++) {
                    mobids.add(mobs.get(Randomizer.nextInt(mobs.size())).getObjectId());
                }
                MapleForce mapleForce = new MapleForce(id, 尖兵.精准火箭, 0, MapleForceType.尖兵火箭, (byte) 1, mobids, (byte) 0, effect.getBulletCount(), getMap());
                client.announce(ForcePacket.showForce(mapleForce));
            }
            lastForceTime = System.currentTimeMillis();
        }
    }

    public void handleAegisSystem(int toMobOid) {
        Skill skill = SkillFactory.getSkill(尖兵.宙斯盾系统);
        if (getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(getSkillLevel(skill));
            if (effect != null) {
                // 冷却时间3秒
                if (System.currentTimeMillis() - lastAegisTime < effect.getY()) {
                    return;
                }
                if (effect.getProp() > Randomizer.nextInt(100)) {
                    MapleForce mapleForce = new MapleForce(id, 尖兵.宙斯盾系统_攻击, 0, MapleForceType.宙斯盾系统, (byte) 1, Collections.singletonList(toMobOid), (byte) 0, effect.getX(), getMap());
                    client.announce(ForcePacket.showForce(mapleForce));
                }
                lastAegisTime = System.currentTimeMillis();
            }
        }
    }

    /*
     * 处理刺客标记攻击怪物效果
     */
    public void handleAssassinStack(MapleMonster mob, int visProjectile) {
        Skill skill_2 = SkillFactory.getSkill(隐士.刺客标记);
        Skill skill_4 = SkillFactory.getSkill(隐士.隐士标记);
        MapleStatEffect effect;
        boolean isAssassin;
        if (getSkillLevel(skill_4) > 0) {
            isAssassin = false;
            effect = skill_4.getEffect(getTotalSkillLevel(skill_4));
        } else if (getSkillLevel(skill_2) > 0) {
            isAssassin = true;
            effect = skill_2.getEffect(getTotalSkillLevel(skill_2));
        } else {
            return;
        }
        if (effect != null && effect.makeChanceResult() && mob != null) {
            int mobCount = effect.getMobCount();
            Rectangle bounds = effect.calculateBoundingBox(mob.getTruePosition(), mob.isFacingLeft());
            List<MapleMapObject> affected = map.getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
            List<Integer> moboids = new ArrayList<>();
            for (MapleMapObject mo : affected) {
                if (moboids.size() < mobCount && mo.getObjectId() != mob.getObjectId()) {
                    moboids.add(mo.getObjectId());
                }
            }
            specialStats.gainForceCounter();
            client.announce(MaplePacketCreator.刺客标记效果(getId(), mob.getObjectId(), specialStats.getForceCounter(), isAssassin, moboids, visProjectile, mob.getTruePosition()));
            specialStats.gainForceCounter(moboids.size());
        }
    }

    /*
     * 金钱炸弹处理
     */
    public void handleMesoExplosion() {
        Skill skill = SkillFactory.getSkill(侠盗.金钱炸弹);
        int skillLevel = getTotalSkillLevel(skill);
        if (skillLevel <= 0) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(skillLevel);
        MapleStatEffect effect_fadong = SkillFactory.getSkill(侠盗.金钱炸弹_攻击).getEffect(skillLevel);
        if (effect != null && effect_fadong != null) {
            //处理攻击怪物的ID
            int mobCount = effect.getMobCount(); //攻击的怪物数量
            Rectangle bounds = effect_fadong.calculateBoundingBox(getTruePosition(), isFacingLeft());
            List<MapleMapObject> affected = map.getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
            List<Integer> moboids = new ArrayList<>();
            for (MapleMapObject mo : affected) {
                if (moboids.size() < mobCount) {
                    moboids.add(mo.getObjectId());
                }
            }
            if (moboids.isEmpty()) {
                return;
            }
            //处理使用金币的数量
            int mesoCount = effect.getBulletCount() + (getTotalSkillLevel(侠盗.金钱炸弹_增强) > 0 ? 5 : 0); //能够使用的金币数量
            affected = map.getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.ITEM));
            List<Point> posFroms = new ArrayList<>();
            for (MapleMapObject mo : affected) {
                if (posFroms.size() < mesoCount) {
                    MapleMapItem mapitem = (MapleMapItem) mo;
                    mapitem.getLock().lock();
                    try {
                        if (mapitem.getMeso() > 0 && !mapitem.isPickedUp() && mapitem.getOwner() == getId()) {
                            Point droppos = new Point(mapitem.getTruePosition());
                            posFroms.add(droppos);
                            //移除以前的金币
                            mapitem.setPickedUp(true);
                            map.broadcastMessage(InventoryPacket.removeItemFromMap(mapitem.getObjectId(), 0, 0), mapitem.getPosition());
                            map.removeMapObject(mapitem);
                        }
                    } finally {
                        mapitem.getLock().unlock();
                    }
                }
            }
            //发送封包
            specialStats.gainForceCounter();
            client.announce(MaplePacketCreator.金钱炸弹效果(getId(), effect_fadong.getSourceid(), specialStats.getForceCounter(), moboids, posFroms));
            specialStats.gainForceCounter(posFroms.size());
        }
    }

    public void handle狂风肆虐(int skillid) {
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.狂风肆虐);
        if (effect != null) {
            if (skillid == effect.getSourceid()) {
                return;
            }
            int prop = effect.getProp() + (getSkillLevel(风灵使者.狂风肆虐_增强) > 0 ? 10 : 0);
            //是否触发下面的效果
            if (Randomizer.nextInt(100) <= prop) {
                int mobCount = effect.getX();
                Rectangle bounds = effect.calculateBoundingBox(getTruePosition(), isFacingLeft());
                List<MapleMapObject> affected = map.getMapObjectsInRect(bounds, Collections.singletonList(MapleMapObjectType.MONSTER));
                List<Integer> moboids = new ArrayList<>();
                if (affected.isEmpty()) {
                    return;
                }
                for (int i = 0; i < effect.getMobCount(); i++) {
                    moboids.add(affected.get(Randomizer.nextInt(affected.size())).getObjectId());
                }
                MapleForce mapleForce = new MapleForce(id, effect.getSourceid(), 0, MapleForceType.狂风肆虐, (byte) 1, moboids, (byte) 1, Randomizer.rand(1, effect.getX()), getMap());
                client.announce(ForcePacket.showForce(mapleForce));
            }
        }
    }

    public void handle暴风灭世(int oid) {
        MapleStatEffect effect = getStatForBuff(MapleBuffStat.暴风灭世);
        if (effect != null && effect.makeChanceResult()) {
            specialStats.gainCardStack();
            client.announce(MaplePacketCreator.暴风灭世效果(getId(), oid, effect.getSourceid(), specialStats.getCardStack()));
            specialStats.gainCardStack();
        }
    }

    public void handle火焰传动() {
        //初始化坐标信息
        if (flameMapId != getMapId() || flamePoint == null) {
            flameMapId = getMapId();
            flamePoint = new Point(getTruePosition());
        }
        //检测坐标信息
        if (!flamePoint.equals(getTruePosition())) {
            client.announce(MaplePacketCreator.instantMapWarp(getId(), flamePoint));
            checkFollow();
            getMap().movePlayer(this, flamePoint);
            flamePoint = null;
        }
    }

    public int get超越数值() {
        if (getBuffedValue(MapleBuffStat.恶魔超越) == null) {
            return 0;
        }
        return getBuffedValue(MapleBuffStat.恶魔超越);
    }

    public void handle超越状态(int skillId) {
        if (!SkillConstants.is超越攻击(skillId)) {
            return;
        }
        //处理超越攻击
        int linkSkillId = SkillConstants.getLinkedAttackSkill(skillId);
        Skill skill = SkillFactory.getSkill(linkSkillId);
        int skilllevel = getSkillLevel(skill);
        if (skilllevel <= 0) {
            return;
        }
        skill.getEffect(skilllevel).applyTranscendBuff(this);
        //处理超越状态
        skill = SkillFactory.getSkill(恶魔复仇者.超越);
        skilllevel = getSkillLevel(skill);
        if (skilllevel <= 0) {
            return;
        }
        MapleStatEffect effect = skill.getEffect(skilllevel);
        MapleBuffStat buffStat = MapleBuffStat.恶魔超越;
        if (getBuffedValue(buffStat) == null) {
            effect.applyTo(this); //注册第1次的BUFF效果
            return;
        }
        int oldCombos = getBuffedIntValue(buffStat);
        if (oldCombos < 20) {
            int newCombos = oldCombos;
            newCombos += 1;
            if (newCombos >= 20) {
                newCombos = 20;
            }
            if (newCombos != oldCombos) {
                setBuffedValue(buffStat, newCombos);
            }
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(buffStat, newCombos);
            client.announce(BuffPacket.giveBuff(effect.getSourceid(), effect.getDuration(), stat, effect, this));
        }
    }

    /*
     * 有概率重新生成1个
     */
    public void handle灵魂吸取(int oid) {
        Skill skill = SkillFactory.getSkill(爆莉萌天使.灵魂吸取_攻击);
        int skilllevel = getSkillLevel(爆莉萌天使.灵魂吸取);
        if (skilllevel > 0) {
            MapleStatEffect effect = skill.getEffect(skilllevel);
            //设置为40%的概率 如果太高了 这个无限攻击
            if (effect != null && Randomizer.nextInt(100) <= 50) {
                specialStats.gainCardStack();
                client.announce(MaplePacketCreator.灵魂吸取攻击(getId(), 爆莉萌天使.灵魂吸取_攻击, specialStats.getCardStack(), oid));
                specialStats.gainCardStack();
            }
        }
    }

    /*
     * 处理三彩箭矢魔法效果
     */
    public void handle三彩箭矢(int oid) {
        if (getBuffedValue(MapleBuffStat.三彩箭矢) == null || specialStats.getArrowsMode() != 2) {
            return;
        }
        int prop = 30; //默认为30%的概率
        int skillId = 神射手.三彩箭矢_魔法;
        Skill skill = SkillFactory.getSkill(神射手.进阶箭筒);
        if (getSkillLevel(skill) > 0) {
            MapleStatEffect effect = skill.getEffect(getSkillLevel(skill));
            if (effect != null) {
                prop = effect.getU();
                skillId = 神射手.进阶箭筒_魔法;
            }
        }
        if (Randomizer.nextInt(100) <= prop) {
            specialStats.gainCardStack();
            client.announce(MaplePacketCreator.三彩箭矢效果(getId(), skillId, specialStats.getCardStack(), oid));
            specialStats.gainCardStack();
        }
    }

    /*
     * 处理夜行者影子蝙蝠效果
     */
    public void handle影子蝙蝠(int oid) {
        //处理召唤蝙蝠
        Skill BuffSkill = SkillFactory.getSkill(夜行者.影子蝙蝠);
        Skill skill_1 = SkillFactory.getSkill(夜行者.影子蝙蝠_召唤兽);
        Skill skill_2 = SkillFactory.getSkill(夜行者.蝙蝠掌控);
        Skill skill_3 = SkillFactory.getSkill(夜行者.蝙蝠掌控Ⅱ);
        Skill skill_4 = SkillFactory.getSkill(夜行者.蝙蝠掌控Ⅲ);
        int skillLevel_1 = getSkillLevel(BuffSkill);
        int skillLevel_2 = getSkillLevel(skill_2);
        int skillLevel_3 = getSkillLevel(skill_3);
        int skillLevel_4 = getSkillLevel(skill_4);
        if (skillLevel_1 < 0) {
            return;
        }
        MapleStatEffect effect = skill_1.getEffect(skillLevel_1);
        int attackCount = effect.getZ(); //攻击多少次可以召唤蝙蝠
        this.attackHit++;
        if (attackHit < attackCount) {
            return; //必须命中3次才召唤怪物
        }
        int maxS = effect.getY();
        if (skillLevel_2 > 0) {
            maxS = maxS + 1;
        }
        if (skillLevel_3 > 0) {
            maxS = maxS + 1;
        }
        if (skillLevel_4 > 0) {
            maxS = maxS + 1;
        }
        int nowSummon = summons != null ? summons.size() : 0; //当前已经召唤的数量
        if (nowSummon < maxS) {
            effect.applyTo(this); //处理召唤兽
        }
        this.attackHit = 0; //重置命中次数
    }

    public int getDecorate() {
        return decorate;
    }

    /*
     * 魔族之纹
     * 恶魔
     * 1012276 - 魔族之纹1 - (无描述)
     * 1012277 - 魔族之纹2 - (无描述)
     * 1012278 - 魔族之纹3 - (无描述)
     * 1012279 - 魔族之纹4 - (无描述)
     * 1012280 - 魔族之纹5 - (无描述)
     * 尖兵
     * 1012361 - 干净的脸 - (无描述)
     * 1012363 - 生成标记 - (无描述)
     * 林之灵
     * 1012455 - 印第安花纹 - (无描述)
     * 1012456 - 眼底花纹 - (无描述)
     * 1012457 - 脸颊心心花纹 - (无描述)
     * 1012458 - 鼻子心心花纹 - (无描述)
     */
    public void setDecorate(int id) {
        if ((id >= 1012276 && id <= 1012280) || id == 1012361 || id == 1012363 || id == 1012455 || id == 1012456 || id == 1012457 || id == 1012458) {
            this.decorate = id;
        } else {
            this.decorate = 0;
        }
    }

    public boolean hasDecorate() {
        return JobConstants.is恶魔(getJob()) || JobConstants.is尖兵(getJob()) || JobConstants.is林之灵(getJob());
    }

    /*
     * 检测林之灵的耳朵和尾巴
     */
    public void checkTailAndEar() {
        if (!JobConstants.is林之灵(getJob())) {
            return;
        }
        if (!questinfo.containsKey(59300)) {
            updateInfoQuest(59300, "bTail=1;bEar=1;TailID=5010119;EarID=5010116", false);
        }
    }

    /*
     * 林之灵隐藏耳朵或者尾巴
     * Tail = 尾巴
     * Ear = 耳朵
     * 59300 = 林之灵的耳朵和尾巴的任务ID
     *
     * 5012000 - 林之灵透明耳朵 - 只有#c林之灵#可以使用的透明耳朵。双击可以遮住林之灵的耳朵或重现显示。
     * 5012001 - 林之灵透明尾巴 - 只有#c林之灵#可以使用的透明尾巴。双击可以遮住林之灵的尾巴或重现显示。
     */
    public void hiddenTailAndEar(int itemId) {
        if (!JobConstants.is林之灵(getJob()) || map == null) {
            return;
        }
        int questId = 59300;
        //检测当前角色是否有这个任务的信息
        if (!questinfo.containsKey(questId)) {
            updateInfoQuest(questId, "bTail=1;bEar=1;TailID=5010119;EarID=5010116", true);
        }
        //检测角色当前是否包含这些字段 如果没有就更新为完整的字段
        if (!containsInfoQuest(questId, "bTail") || !containsInfoQuest(questId, "bEar") || !containsInfoQuest(questId, "TailID") || !containsInfoQuest(questId, "EarID")) {
            updateInfoQuest(questId, "bTail=1;bEar=1;TailID=5010119;EarID=5010116", true);
        }
        if (itemId == 5012000) { //林之灵透明耳朵 - 只有#c林之灵#可以使用的透明耳朵。双击可以遮住林之灵的耳朵或重现显示。
            setInfoQuestStat(questId, "bEar", containsInfoQuest(questId, "bEar=1") ? "0" : "1");
        } else if (itemId == 5012001) { //林之灵透明尾巴 - 只有#c林之灵#可以使用的透明尾巴。双击可以遮住林之灵的尾巴或重现显示。
            setInfoQuestStat(questId, "bTail", containsInfoQuest(questId, "bTail=1") ? "0" : "1");
        }
//        map.broadcastMessage(InventoryPacket.hiddenTailAndEar(getId(), containsInfoQuest(questId, "bTail=0"), containsInfoQuest(questId, "bEar=0")));
    }

    /*
     * 7784 是改变耳朵的任务
     */
    public void changeElfEar() {
        updateInfoQuest(GameConstants.精灵耳朵, containsInfoQuest(GameConstants.精灵耳朵, "sw=") ? containsInfoQuest(GameConstants.精灵耳朵, "sw=1") ? "sw=0" : "sw=1" : "sw=1");
        if (containsInfoQuest(GameConstants.精灵耳朵, JobConstants.is双弩精灵(getJob()) ? "sw=0" : "sw=1")) {
            getClient().announce(EffectPacket.showOwnJobChangedElf("Effect/BasicEff.img/JobChangedElf", 2, 5155000));
            getMap().broadcastMessage(this, EffectPacket.showJobChangedElf(this.getId(), "Effect/BasicEff.img/JobChangedElf", 2, 5155000), false);
        } else {
            getClient().announce(EffectPacket.showOwnJobChangedElf("Effect/BasicEff.img/JobChanged", 2, 5155000));
            getMap().broadcastMessage(this, EffectPacket.showJobChangedElf(this.getId(), "Effect/BasicEff.img/JobChanged", 2, 5155000), false);
        }
        equipChanged();
    }

    public boolean isElfEar() {
        if (containsInfoQuest(GameConstants.精灵耳朵, "sw=")) {
            return containsInfoQuest(GameConstants.精灵耳朵, JobConstants.is双弩精灵(getJob()) ? "sw=0" : "sw=1");
        }
        return JobConstants.is双弩精灵(getJob());
    }

    public int getVip() {
        return vip;
    }

    public boolean isVip() {
        return vip > 0;
    }

    /*
     * Vip 会员信息
     */
    public void setVip(int vip) {
        if (vip >= 5) {
            this.vip = 5;
        } else if (vip < 0) {
            this.vip = 0;
        } else {
            this.vip = vip;
        }
    }

    public Timestamp getViptime() {
        if (getVip() == 0) {
            return null;
        }
        return viptime;
    }

    public void setViptime(Timestamp expire) {
        this.viptime = expire;
    }

    public void setViptime(long period) {
        if (period > 0) {
            Timestamp expiration = new Timestamp(System.currentTimeMillis() + (period * 24 * 60 * 60 * 1000));
            setViptime(expiration);
        } else {
            setViptime(null);
        }
    }

    /*
     * 获取挑战BOSS的次数设置
     */
    public int getBossLog(String boss) {
        return getBossLog(boss, 0);
    }

    public int getBossLog(String boss, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int count = 0;
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM bosslog WHERE characterid = ? AND bossid = ?");
            ps.setInt(1, id);
            ps.setString(2, boss);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                /*
                 * 年：calendar.get(Calendar.YEAR)
                 * 月：calendar.get(Calendar.MONTH)+1
                 * 日：calendar.get(Calendar.DAY_OF_MONTH)
                 * 星期：calendar.get(Calendar.DAY_OF_WEEK)-1
                 */
                count = rs.getInt("count");
                if (count < 0) {
                    return count;
                }
                Timestamp bossTime = rs.getTimestamp("time");
                rs.close();
                ps.close();
                if (type == 0) {
                    Calendar sqlcal = Calendar.getInstance();
                    if (bossTime != null) {
                        sqlcal.setTimeInMillis(bossTime.getTime());
                    }
                    if (sqlcal.get(Calendar.DAY_OF_MONTH) + 1 <= Calendar.getInstance().get(Calendar.DAY_OF_MONTH) || sqlcal.get(Calendar.MONTH) + 1 <= Calendar.getInstance().get(Calendar.MONTH) || sqlcal.get(Calendar.YEAR) + 1 <= Calendar.getInstance().get(Calendar.YEAR)) {
                        count = 0;
                        ps = con.prepareStatement("UPDATE bosslog SET count = 0, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND bossid = ?");
                        ps.setInt(1, id);
                        ps.setString(2, boss);
                        ps.executeUpdate();
                    }
                }
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO bosslog (characterid, bossid, count, type) VALUES (?, ?, ?, ?)");
                psu.setInt(1, id);
                psu.setString(2, boss);
                psu.setInt(3, 0);
                psu.setInt(4, type);
                psu.executeUpdate();
                psu.close();
            }
            rs.close();
            ps.close();
            return count;
        } catch (Exception Ex) {
            log.error("获取BOSS挑战次数.", Ex);
            return -1;
        }
    }

    /*
     * 增加挑战BOSS的次数设置
     */
    public void setBossLog(String boss) {
        setBossLog(boss, 0);
    }

    public void setBossLog(String boss, int type) {
        setBossLog(boss, type, 1);
    }

    public void setBossLog(String boss, int type, int count) {
        int bossCount = getBossLog(boss, type);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE bosslog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND bossid = ?");
            ps.setInt(1, bossCount + count);
            ps.setInt(2, type);
            ps.setInt(3, id);
            ps.setString(4, boss);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
            log.error("Error while set bosslog.", Ex);
        }
    }

    /*
     * 重置挑战BOSS的次数设置
     */
    public void resetBossLog(String boss) {
        resetBossLog(boss, 0);
    }

    public void resetBossLog(String boss, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE bosslog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND bossid = ?");
            ps.setInt(1, 0);
            ps.setInt(2, type);
            ps.setInt(3, id);
            ps.setString(4, boss);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
            log.error("重置BOSS次数失败.", Ex);
        }
    }

    public int getBossLogAcc(String boss) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int count = 0;
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM bosslog WHERE accountid = ? AND bossid = ?");
            ps.setInt(1, accountid);
            ps.setString(2, boss);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                /*
                 * 年：calendar.get(Calendar.YEAR)
                 * 月：calendar.get(Calendar.MONTH)+1
                 * 日：calendar.get(Calendar.DAY_OF_MONTH)
                 * 星期：calendar.get(Calendar.DAY_OF_WEEK)-1
                 */
                count = rs.getInt("count");
                if (count < 0) {
                    return count;
                }
                Timestamp bossTime = rs.getTimestamp("time");
                rs.close();
                ps.close();
                Calendar sqlcal = Calendar.getInstance();
                if (bossTime != null) {
                    sqlcal.setTimeInMillis(bossTime.getTime());
                }
                if (sqlcal.get(Calendar.DAY_OF_MONTH) + 1 <= Calendar.getInstance().get(Calendar.DAY_OF_MONTH) || sqlcal.get(Calendar.MONTH) + 1 <= Calendar.getInstance().get(Calendar.MONTH) || sqlcal.get(Calendar.YEAR) + 1 <= Calendar.getInstance().get(Calendar.YEAR)) {
                    count = 0;
                    ps = con.prepareStatement("UPDATE bosslog SET count = 0, time = CURRENT_TIMESTAMP() WHERE accountid = ? AND bossid = ?");
                    ps.setInt(1, accountid);
                    ps.setString(2, boss);
                    ps.executeUpdate();
                }
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO bosslog (accountid, characterid, bossid, count) VALUES (?, ?, ?, ?)");
                psu.setInt(1, accountid);
                psu.setInt(2, 0);
                psu.setString(3, boss);
                psu.setInt(4, 0);
                psu.executeUpdate();
                psu.close();
            }
            rs.close();
            ps.close();
            return count;
        } catch (Exception Ex) {
            log.error("获取BOSS挑战次数.", Ex);
            return -1;
        }
    }

    public void setBossLogAcc(String bossid) {
        setBossLogAcc(bossid, 0);
    }

    public void setBossLogAcc(String bossid, int bossCount) {
        bossCount += getBossLogAcc(bossid);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE bosslog SET count = ?, characterid = ?, time = CURRENT_TIMESTAMP() WHERE accountid = ? AND bossid = ?");
            ps.setInt(1, bossCount + 1);
            ps.setInt(2, id);
            ps.setInt(3, accountid);
            ps.setString(4, bossid);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
            log.error("Error while set bosslog.", Ex);
        }
    }

    public int getEventLogForDay(String event) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int count = 0;
            PreparedStatement ps;
            ps = con.prepareStatement("SELECT * FROM eventforday WHERE eventid = ?");
            ps.setString(1, event);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt("count");
                    if (count < 0) {
                        return count;
                    }
                    Timestamp eventTime = rs.getTimestamp("time");
                    rs.close();
                    ps.close();
                    Calendar sqlcal = Calendar.getInstance();
                    if (eventTime != null) {
                        sqlcal.setTimeInMillis(eventTime.getTime());
                    }
                    if (sqlcal.get(Calendar.DAY_OF_MONTH) + 1 <= Calendar.getInstance().get(Calendar.DAY_OF_MONTH) || sqlcal.get(Calendar.MONTH) + 1 <= Calendar.getInstance().get(Calendar.MONTH) || sqlcal.get(Calendar.YEAR) + 1 <= Calendar.getInstance().get(Calendar.YEAR)) {
                        count = 0;
                        ps = con.prepareStatement("UPDATE eventforday SET count = 0, time = CURRENT_TIMESTAMP() WHERE eventid = ?");
                        ps.setString(1, event);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement psu = con.prepareStatement("INSERT INTO eventforday (eventid, count) VALUES (?, ?)")) {
                        psu.setString(1, event);
                        psu.setInt(2, 0);
                        psu.executeUpdate();
                    }
                }
            }
            ps.close();
            return count;
        } catch (Exception Ex) {
            log.error("Error while get EventLogForDay.", Ex);
            return -1;
        }
    }

    public void setEventLogForDay(String eventid) {
        setEventLogForDay(eventid, 0);
    }

    public void setEventLogForDay(String eventid, int eventCount) {
        eventCount += getEventLogForDay(eventid);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE eventforday SET count = ?, time = CURRENT_TIMESTAMP() WHERE eventid = ?")) {
                ps.setInt(1, eventCount + 1);
                ps.setString(2, eventid);
                ps.executeUpdate();
            }
        } catch (Exception Ex) {
            log.error("Error while set EventLogForDay.", Ex);
        }
    }

    public void resetEventLogForDay(String eventid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE eventforday SET count = ?, time = CURRENT_TIMESTAMP() WHERE eventid = ?")) {
                ps.setInt(1, 0);
                ps.setString(2, eventid);
                ps.executeUpdate();
            }
        } catch (Exception Ex) {
            log.error("Error while reset EventLogForDay.", Ex);
        }
    }

    /*
     * 更新抵用卷信息
     */
    public void updateCash() {
        client.announce(MaplePacketCreator.showCharCash(this));
    }

    /*
     * 检测玩家背包空间
     */
    public short getSpace(int type) {
        return getInventory(MapleInventoryType.getByType((byte) type)).getNumFreeSlot();
    }

    public boolean haveSpace(int type) {
        short slot = getInventory(MapleInventoryType.getByType((byte) type)).getNextFreeSlot();
        return (slot != -1);
    }

    public boolean haveSpaceForId(int itemid) {
        short slot = getInventory(ItemConstants.getInventoryType(itemid)).getNextFreeSlot();
        return (slot != -1);
    }

    public boolean canHold() {
        for (int i = 1; i <= 5; i++) {
            if (getInventory(MapleInventoryType.getByType((byte) i)).getNextFreeSlot() <= -1) {
                return false;
            }
        }
        return true;
    }

    public boolean canHoldSlots(int slot) {
        for (int i = 1; i <= 5; i++) {
            if (getInventory(MapleInventoryType.getByType((byte) i)).isFull(slot)) {
                return false;
            }
        }
        return true;
    }

    public boolean canHold(int itemid) {
        return getInventory(ItemConstants.getInventoryType(itemid)).getNextFreeSlot() > -1;
    }

    public long getMerchantMeso() {
        long mesos = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM hiredmerch WHERE characterid = ?");
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                mesos = rs.getLong("Mesos");
            }
            rs.close();
            ps.close();
        } catch (SQLException se) {
            log.error("获取雇佣商店金币发生错误", se);
        }
        return mesos;
    }

    public void autoban(String reason, int greason) {
        /*
         * 年：calendar.get(Calendar.YEAR) 月：calendar.get(Calendar.MONTH)+1
         * 日：calendar.get(Calendar.DAY_OF_MONTH)
         * 星期：calendar.get(Calendar.DAY_OF_WEEK)-1
         */
        Calendar cal = Calendar.getInstance();
        cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH) + 3, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        Timestamp TS = new Timestamp(cal.getTimeInMillis());
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET banreason = ?, tempban = ?, greason = ? WHERE id = ?");
            ps.setString(1, reason);
            ps.setTimestamp(2, TS);
            ps.setInt(3, greason);
            ps.setInt(4, accountid);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            log.error("Error while autoban" + e);
        }
    }

    public boolean isBanned() {
        return isbanned;
    }

    public void sendPolice(int greason, String reason, int duration) {
        //announce(LoginPacket.sendPolice(greason, reason, duration));
        this.isbanned = true;
        WorldTimer.getInstance().schedule(() -> client.disconnect(true, false), duration);
    }

    public void sendPolice(String text) {
        client.announce(MaplePacketCreator.sendPolice(text));
        this.isbanned = true;
        WorldTimer.getInstance().schedule(() -> {
            client.disconnect(true, false);
            if (client.getSession().isActive()) {
                client.getSession().close();
            }
        }, 6000);
    }

    /*
     * 获取角色创建的日期
     */
    public Timestamp getChrCreated() {
        if (createDate != null) {
            return createDate;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT createdate FROM characters WHERE id = ?");
            ps.setInt(1, this.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return null;
            }
            createDate = rs.getTimestamp("createdate");
            rs.close();
            ps.close();
            return createDate;
        } catch (SQLException e) {
            log.error("获取角色创建日期出错", e);
            return new Timestamp(-1);
        }
    }

    /*
     * 检测非GM角色是否在监狱地图
     */
    public boolean isInJailMap() {
        return getMapId() == GameConstants.JAIL && getGMLevel() == 0;
    }

    /*
     * 角色警告次数
     */
    public int getWarning() {
        return warning;
    }

    public void setWarning(int warning) {
        this.warning = warning;
    }

    public void gainWarning(boolean warningEnabled) {
        this.warning += 1;
        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 截至目前玩家: " + getName() + " (等级 " + getLevel() + ") 该用户已被警告: " + warning + " 次！"));
        if (warningEnabled) {
            if (warning == 1) { //警告次数1
                dropMessage(5, "这是你的第一次警告！请注意在游戏中勿使用非法程序！");
            } else if (warning == 2) { //警告次数2
                dropMessage(5, "警告现在是第 " + warning + " 次。如果你再得到一次警告就会封号处理！");
            } else if (warning >= 3) { //警告次数3
                ban(getName() + " 由于警告次数超过: " + warning + " 次，系统对其封号处理！", false, true, false);
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(0, "[系统封号] 玩家 " + getName() + " (等级 " + getLevel() + ") 由于警告次数过多，系统对其封号处理！"));
            }
        }
    }

    /*
     * 豆豆信息
     */
    public int getBeans() {
        return beans;
    }

    public void setBeans(int i) {
        this.beans = i;
    }

    public void gainBeans(int i, boolean show) {
        this.beans += i;
        if (show && i != 0) {
            dropMessage(-1, "您" + (i > 0 ? "获得了 " : "消耗了 ") + Math.abs(i) + " 个豆豆.");
        }
    }

    /*
     * 传授技能
     */
    public int teachSkill(int toSkillId, int skillId, int toChrId, boolean notime) {
        if (toSkillId == 80000055 || toSkillId == 80000329) {
            return 1;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM skills WHERE skillid = ? AND teachId = ?")) {
                ps.setInt(1, skillId);
                ps.setInt(2, toChrId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement("UPDATE skills SET " + (notime ? "" : "expiration = ?, ") + "teachId = ? WHERE skillid = ? AND characterid = ?")) {
                int n5 = 0;
                if (!notime) {
                    ps.setLong(++n5, System.currentTimeMillis());
                }
                ps.setInt(++n5, notime ? 0 : this.id);
                ps.setInt(++n5, toSkillId);
                ps.setInt(++n5, toChrId);
                ps.executeUpdate();
            }
            return 1;
        } catch (Exception Ex) {
            log.error("传授技能失败.", Ex);
            return -1;
        }
    }

    /*
     * 对角色使用测谎仪
     */
    public void startLieDetector(boolean isItem) {
        if (!getAntiMacro().inProgress()) {
            getAntiMacro().startLieDetector(getName(), isItem, false);
        }
    }

    /*
     * 转身系统
     */
    public int getReborns() {
        return reborns;
    }

    public int getReborns1() {
        return reborns1;
    }

    public int getReborns2() {
        return reborns2;
    }

    public int getReborns3() {
        return reborns3;
    }

    public void gainReborns(int type) {
        if (type == 0) {
            this.reborns += 1;
        } else if (type == 1) {
            this.reborns1 += 1;
        } else if (type == 2) {
            this.reborns2 += 1;
        } else if (type == 3) {
            this.reborns3 += 1;
        }
    }

    public int getAPS() {
        return apstorage;
    }

    public void gainAPS(int aps) {
        this.apstorage += aps;
    }

    public void doReborn(int type) {
        List<Pair<MapleStat, Long>> stat = new ArrayList<>(12);
        clearSkills(); //清理技能

        gainReborns(type); //添加转身数

        setLevel((short) 1); //设置等级为1级
        setExp(0); //设置经验为0
        setJob(0); //设置职业为 0 新手
        setRemainingAp((short) 0); //设置AP为0
        int str = 12;
        int dex = 5;
        int int_ = 4;
        int luk = 4;
        int total = getReborns() * 5 + getReborns1() * 10 + getReborns2() * 15 + getReborns3() * 30;
        stats.str = (short) str;
        stats.dex = (short) dex;
        stats.int_ = (short) int_;
        stats.luk = (short) luk;

        stats.setInfo(50, 50, 50, 50);
        setRemainingAp((short) total);
        stats.recalcLocalStats(this);

        stat.add(new Pair<>(MapleStat.力量, (long) str));
        stat.add(new Pair<>(MapleStat.敏捷, (long) dex));
        stat.add(new Pair<>(MapleStat.智力, (long) int_));
        stat.add(new Pair<>(MapleStat.运气, (long) luk));
        stat.add(new Pair<>(MapleStat.AVAILABLEAP, (long) total));
        stat.add(new Pair<>(MapleStat.MAXHP, (long) 50));
        stat.add(new Pair<>(MapleStat.MAXMP, (long) 50));
        stat.add(new Pair<>(MapleStat.HP, (long) 50));
        stat.add(new Pair<>(MapleStat.MP, (long) 50));
        stat.add(new Pair<>(MapleStat.等级, (long) 1));
        stat.add(new Pair<>(MapleStat.职业, (long) 0));
        stat.add(new Pair<>(MapleStat.经验, (long) 0));

        client.announce(MaplePacketCreator.updatePlayerStats(stat, false, this));
    }

    public void doReborn(int type, int ap) {
        List<Pair<MapleStat, Long>> stat = new ArrayList<>(12);
        clearSkills(); //清理技能

        gainReborns(type); //添加转身数

        setLevel((short) 1); //设置等级为1级
        setExp(0); //设置经验为0
        setJob(0); //设置职业为 0 新手
        setRemainingAp((short) 0); //设置AP为0
        int str = 12;
        int dex = 5;
        int int_ = 4;
        int luk = 4;
        stats.str = (short) str;
        stats.dex = (short) dex;
        stats.int_ = (short) int_;
        stats.luk = (short) luk;

        stats.setInfo(50, 50, 50, 50);
        setRemainingAp((short) ap);
        stats.recalcLocalStats(this);

        stat.add(new Pair<>(MapleStat.力量, (long) str));
        stat.add(new Pair<>(MapleStat.敏捷, (long) dex));
        stat.add(new Pair<>(MapleStat.智力, (long) int_));
        stat.add(new Pair<>(MapleStat.运气, (long) luk));
        stat.add(new Pair<>(MapleStat.AVAILABLEAP, (long) ap));
        stat.add(new Pair<>(MapleStat.MAXHP, (long) 50));
        stat.add(new Pair<>(MapleStat.MAXMP, (long) 50));
        stat.add(new Pair<>(MapleStat.HP, (long) 50));
        stat.add(new Pair<>(MapleStat.MP, (long) 50));
        stat.add(new Pair<>(MapleStat.等级, (long) 1));
        stat.add(new Pair<>(MapleStat.职业, (long) 0));
        stat.add(new Pair<>(MapleStat.经验, (long) 0));

        client.announce(MaplePacketCreator.updatePlayerStats(stat, false, this));
    }

    public void clearSkills() {
        Map<Integer, SkillEntry> chrSkill = new HashMap<>(getSkills());
        Map<Integer, SkillEntry> newList = new HashMap<>();
        for (Entry<Integer, SkillEntry> skill : chrSkill.entrySet()) {
            newList.put(skill.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
        }
        changeSkillsLevel(newList);
        newList.clear();
        chrSkill.clear();
    }

    /*
     * 幻影复制技能
     */
    public Map<Integer, Byte> getSkillsWithPos() {
        Map<Integer, SkillEntry> chrskills = new HashMap<>(getSkills());
        Map<Integer, Byte> skillsWithPos = new LinkedHashMap<>();
        for (Entry<Integer, SkillEntry> skill : chrskills.entrySet()) {
            if (skill.getValue().position >= 0 && skill.getValue().position <= 15) {
                skillsWithPos.put(skill.getKey(), skill.getValue().position);
            }
        }
        return skillsWithPos;
    }

    public int 幻影复制技能(int position) {
        int skillId = 0;
        Map<Integer, Byte> skillsWithPos = getSkillsWithPos();
        for (Entry<Integer, Byte> x : skillsWithPos.entrySet()) {
            if (x.getValue() == position) {
                skillId = x.getKey();
                break;
            }
        }
        return skillId;
    }

    /*
     * 获取幻影装备中的技能ID
     */
    public int 获取幻影装备技能(int skillid) {
        SkillEntry ret = skills.get(skillid);
        if (ret == null || ret.teachId == 0) {
            return 0;
        }
        return skills.get(ret.teachId) != null ? ret.teachId : 0;
    }

    /*
     * 装备或者解除幻影装备的技能
     */
    public void 修改幻影装备技能(int skillId, int teachId) {
        SkillEntry ret = skills.get(skillId);
        if (ret != null) {
            Skill theskill = SkillFactory.getSkill(ret.teachId);
            if (theskill != null && theskill.isBuffSkill()) {
                cancelEffect(theskill.getEffect(1), false, -1);
            }
            ret.teachId = teachId;
            changed_skills = true;
            client.announce(MaplePacketCreator.修改幻影装备技能(skillId, teachId));
        }
    }

    public void 幻影删除技能(int skillId) {
        SkillEntry ret = skills.get(skillId);
        if (ret != null) {
            skills.remove(skillId);
            client.announce(MaplePacketCreator.幻影删除技能(ret.position));
            changed_skills = true;
        }
    }

    public void 幻影技能复制(int skillBook, int skillId, int level) {
        int skillLevel = 0;
        if (skillBook == 1) {
            skillLevel = getSkillLevel(幻影.幻影印技天赋Ⅰ);
        } else if (skillBook == 2) {
            skillLevel = getSkillLevel(幻影.幻影印技天赋Ⅱ);
        } else if (skillBook == 3) {
            skillLevel = getSkillLevel(幻影.幻影印技天赋Ⅲ);
        } else if (skillBook == 4) {
            skillLevel = getSkillLevel(幻影.幻影印技天赋Ⅳ);
        } else if (skillBook == 5) {
            skillLevel = getSkillLevel(幻影.幻影印技天赋H);
        }
        Skill theskill = SkillFactory.getSkill(skillId); //获取复制技能的信息
        if (level > skillLevel) { //检测复制技能等级是否大于当前封印天赋的等级
            skillLevel = level; //如果大于就设置这个为封印天赋的等级
        }
        if (skillLevel > theskill.getMaxLevel()) { //检测技能的等级大于复制技能的最大等级
            skillLevel = theskill.getMaxLevel();
        }
        SkillEntry ret = skills.get(skillId);
        if (ret != null) { //如果技能列表中有这个复制的技能
            ret.skillevel = skillLevel; //直接修改这个技能的等级
            changed_skills = true;
            client.announce(MaplePacketCreator.幻影复制技能(ret.position, skillId, skillLevel));
        } else if (skillBook == 1) {
            for (int i = 0; i < 4; i++) {
                if (幻影复制技能(i) == 0) {
                    skills.put(skillId, new SkillEntry(skillLevel, (byte) theskill.getMasterLevel(), -1, 0, (byte) i));
                    changed_skills = true;
                    client.announce(MaplePacketCreator.幻影复制技能(i, skillId, skillLevel));
                    break;
                }
            }
        } else if (skillBook == 2) {
            for (int i = 4; i < 8; i++) {
                if (幻影复制技能(i) == 0) {
                    skills.put(skillId, new SkillEntry(skillLevel, (byte) theskill.getMasterLevel(), -1, 0, (byte) i));
                    changed_skills = true;
                    client.announce(MaplePacketCreator.幻影复制技能(i, skillId, skillLevel));
                    break;
                }
            }
        } else if (skillBook == 3) {
            for (int i = 8; i < 11; i++) {
                if (幻影复制技能(i) == 0) {
                    skills.put(skillId, new SkillEntry(skillLevel, (byte) theskill.getMasterLevel(), -1, 0, (byte) i));
                    changed_skills = true;
                    client.announce(MaplePacketCreator.幻影复制技能(i, skillId, skillLevel));
                    break;
                }
            }
        } else if (skillBook == 4) {
            for (int i = 11; i < 13; i++) {
                if (幻影复制技能(i) == 0) {
                    skills.put(skillId, new SkillEntry(skillLevel, (byte) theskill.getMasterLevel(), -1, 0, (byte) i));
                    changed_skills = true;
                    client.announce(MaplePacketCreator.幻影复制技能(i, skillId, skillLevel));
                    break;
                }
            }
        } else if (skillBook == 5) {
            for (int i = 13; i < 15; i++) {
                if (幻影复制技能(i) == 0) {
                    skills.put(skillId, new SkillEntry(skillLevel, (byte) theskill.getMasterLevel(), -1, 0, (byte) i));
                    changed_skills = true;
                    client.announce(MaplePacketCreator.幻影复制技能(i, skillId, skillLevel));
                    break;
                }
            }
        }
    }

    /*
     * 角色卡系统
     */
    public MapleCharacterCards getCharacterCard() {
        return characterCard;
    }

    /*
     * 荣誉系统
     */
    public InnerSkillEntry[] getInnerSkills() {
        return innerSkills;
    }

    public int getInnerSkillSize() {
        int ret = 0;
        for (int i = 0; i < 3; i++) {
            if (innerSkills[i] != null) {
                ret++;
            }
        }
        return ret;
    }

    public int getInnerSkillIdByPos(int position) {
        if (innerSkills[position] != null) {
            return innerSkills[position].getSkillId();
        }
        return 0;
    }

    public int getHonor() {
        return honor;
    }

    public void setHonor(int exp) {
        this.honor = exp;
    }

    public void gainHonor(int amount) {
        if (amount > 0) {
            dropMessage(-9, "声望" + amount + "已获得。");
        }
        honor += amount;
        client.announce(MaplePacketCreator.updateInnerStats(this));
    }

    /*
     * 检测角色是否有内在技能
     * 如果没有自动给予
     */
    public void checkInnerSkill() {
        if (level >= 30 && innerSkills[0] == null) {
            changeInnerSkill(new InnerSkillEntry(70000000, 1, (byte) 1, (byte) 0));
        }
        if (level >= 50 && innerSkills[1] == null) {
            changeInnerSkill(new InnerSkillEntry(70000001, 3, (byte) 2, (byte) 0));
        }
        if (level >= 70 && innerSkills[2] == null) {
            changeInnerSkill(new InnerSkillEntry(70000002, 5, (byte) 3, (byte) 0));
        }
    }

    /*
     * 改变内在技能
     */
    public void changeInnerSkill(InnerSkillEntry data) {
        changeInnerSkill(data.getSkillId(), data.getSkillLevel(), data.getPosition(), data.getRank());
    }

    public void changeInnerSkill(int skillId, int skillevel, byte position, byte rank) {
        Skill skill = SkillFactory.getSkill(skillId);
        if (skill == null || !skill.isInnerSkill() || skillevel <= 0 || position > 3 || position < 1) {
            return;
        }
        if (skillevel > skill.getMaxLevel()) {
            skillevel = skill.getMaxLevel();
        }
        InnerSkillEntry InnerSkill = new InnerSkillEntry(skillId, skillevel, position, rank);
        innerSkills[position - 1] = InnerSkill; //这个地方用的数组 数组是从 0 开始计算 所以减去1
        client.announce(MaplePacketCreator.updateInnerSkill(skillId, skillevel, position, rank));
        changed_innerSkills = true;
    }

    public void checkHyperAP() {
        if (level >= 140) {
            Skill skil;
            Map<Integer, SkillEntry> list = new HashMap<>();
            for (int i = 80000400; i <= 80000417; i++) {
                skil = SkillFactory.getSkill(i);
                if (skil != null && getSkillLevel(skil) <= 0) {
                    list.put(i, new SkillEntry((byte) 0, (byte) skil.getMaxLevel(), -1));
                }
            }
            if (!list.isEmpty()) {
                changeSkillsLevel(list);
            }
        }
    }

    /*
     * 侠盗本能击杀点数
     */
    public void handleKillSpreeGain() {
        Skill skill = SkillFactory.getSkill(侠盗.侠盗本能);
        int skillLevel = getSkillLevel(skill);
        int killSpree = getBuffedIntValue(MapleBuffStat.击杀点数);
        if (skillLevel <= 0 || killSpree >= 5) {
            return;
        }
        if (Randomizer.nextInt(100) <= 20) {
            skill.getEffect(skillLevel).applyTo(this, true);
        }
    }

    /*
     * 奇袭者元素雷电被动BUFF设置
     */
    public void handle元素雷电(int skillId) {
        //这几个技能是不加雷电次数的
        if (skillId == 奇袭者.疾风 || skillId == 奇袭者.毁灭 || skillId == 奇袭者.台风) {
            return;
        }
        Skill skill = SkillFactory.getSkill(奇袭者.元素_闪电);
        int skillLevel = getSkillLevel(skill);
        if (skillLevel <= 0 || getBuffedValue(MapleBuffStat.元素属性) == null) {
            return;
        }
        if (Randomizer.nextInt(100) <= getStat().raidenPorp) {
            skill.getEffect(skillLevel).applyTo(this, true);
        }
    }

    /*
     * 圣骑士元素冲击处理
     */
    public void handle元素冲击(int skillId) {
        //检测角色是否有这个技能元素冲击
        Skill skill_2 = SkillFactory.getSkill(圣骑士.元素冲击);
        Skill skill_4 = SkillFactory.getSkill(圣骑士.万佛归一破);
        MapleStatEffect effect;
        if (getSkillLevel(skill_4) > 0) {
            effect = skill_4.getEffect(getTotalSkillLevel(skill_4));
        } else if (getSkillLevel(skill_2) > 0) {
            effect = skill_2.getEffect(getTotalSkillLevel(skill_2));
        } else {
            return;
        }
        //检测当前攻击技能的元素属性
        Element newElement = null;
        switch (skillId) {
            case 圣骑士.火焰冲击:
                newElement = Element.火;
                break;
            case 圣骑士.寒冰冲击:
                newElement = Element.冰;
                break;
            case 圣骑士.雷鸣冲击:
                newElement = Element.雷;
                break;
            case 圣骑士.神圣冲击:
                newElement = Element.神圣;
                break;
        }
        if (newElement == null) { //如果新的攻击属性为空就返回
            return;
        }
        if (elements == null) { //如果上1个属性攻击为空就返回
            elements = newElement;
            return;
        }
        boolean change = elements != newElement; //查看属性是否发生改变
        if (change) {
            elements = newElement;
            MapleBuffStat buffStat = MapleBuffStat.元素冲击;
            int buffSourceId = getBuffSource(buffStat);
            if (getBuffedValue(buffStat) == null || buffSourceId != effect.getSourceid()) {
                effect.applyTo(this, true); //注册第1次的BUFF效果
                return;
            }
            int count = Math.min(getBuffedIntValue(buffStat) / 5, 5);
            if (count < 5) {
                count++;
            }
            if (isShowPacket()) {
                dropSpouseMessage(0x0A, "当前元素冲击数: " + count);
            }
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(buffStat, count * 5);
            setBuffedValue(buffStat, count * 5);
            int duration = effect.getDuration();
            client.announce(BuffPacket.giveBuff(effect.getSourceid(), duration, stat, effect, this));
        }
    }

    /*
     * 神之子攻击BUFF
     */
    public void handle提速时刻() {
        Skill skill = SkillFactory.getSkill(isZeroSecondLook() ? 神之子.提速时刻_战斗 : 神之子.提速时刻_侦查);
        int skillLevel = 1;
        skill.getEffect(skillLevel).applyTo(this, true);
    }

    public void handleHeartMineUnity() {
        MapleStatEffect effect;
        Skill skill = SkillFactory.getSkill(奇袭者.心雷合一);
        if (getSkillLevel(奇袭者.心雷合一) > 0 && getBuffedIntValue(MapleBuffStat.心雷合一) > 0 && (effect = skill.getEffect(getSkillLevel(奇袭者.心雷合一))) != null) {
            int n2 = effect.getX();
            List<MapleMapObject> list = getMap().getMonstersInRange(getTruePosition(), 100000.0);
            ArrayList<Integer> arrayList = new ArrayList<>();
            list.forEach(y2 -> {
                        MapleMonster monster = (MapleMonster) y2;
                        if (!monster.getStats().isFriendly() && !monster.isFake() && arrayList.size() < n2) {
                            arrayList.add(y2.getObjectId());
                        }
                    }
            );
            MapleForce force = new MapleForce(getId(), 奇袭者.心雷合一_1, 0, MapleForceType.心雷合一, (byte) 1, arrayList, (byte) 1, 1, getMap());
            send_other(ForcePacket.showForce(force), true);
        }
    }

    /*
     * 获取角色勋章的名字
     */
    public String getMedalText() {
        String medal = "";
        Item medalItem = getInventory(MapleInventoryType.EQUIPPED).getItem((byte) -26);
        if (medalItem != null) {
            medal = "<" + MapleItemInformationProvider.getInstance().getName(medalItem.getItemId()) + "> ";
        }
        return medal;
    }

    /*
     * 新的角色泡点，按帐号计算
     */
    public int getGamePoints() {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int gamePoints = 0;
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts_info WHERE accId = ? AND worldId = ?");
            ps.setInt(1, getAccountID());
            ps.setInt(2, getWorld());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                gamePoints = rs.getInt("gamePoints");
                Timestamp updateTime = rs.getTimestamp("updateTime");
                Calendar sqlcal = Calendar.getInstance();
                if (updateTime != null) {
                    sqlcal.setTimeInMillis(updateTime.getTime());
                }
                if (sqlcal.get(Calendar.DAY_OF_MONTH) + 1 <= Calendar.getInstance().get(Calendar.DAY_OF_MONTH) || sqlcal.get(Calendar.MONTH) + 1 <= Calendar.getInstance().get(Calendar.MONTH) || sqlcal.get(Calendar.YEAR) + 1 <= Calendar.getInstance().get(Calendar.YEAR)) {
                    gamePoints = 0;
                    PreparedStatement psu = con.prepareStatement("UPDATE accounts_info SET gamePoints = 0, updateTime = CURRENT_TIMESTAMP() WHERE accId = ? AND worldId = ?");
                    psu.setInt(1, getAccountID());
                    psu.setInt(2, getWorld());
                    psu.executeUpdate();
                    psu.close();
                }
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO accounts_info (accId, worldId, gamePoints) VALUES (?, ?, ?)");
                psu.setInt(1, getAccountID());
                psu.setInt(2, getWorld());
                psu.setInt(3, 0);
                psu.executeUpdate();
                psu.close();
            }
            rs.close();
            ps.close();
            return gamePoints;
        } catch (Exception Ex) {
            log.error("获取角色帐号的在线时间点出现错误 - 数据库查询失败", Ex);
            return -1;
        }
    }

    public void gainGamePoints(int amount) {
        int gamePoints = getGamePoints() + amount;
        updateGamePoints(gamePoints);
    }

    public void resetGamePoints() {
        updateGamePoints(0);
    }

    public void updateGamePoints(int amount) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE accounts_info SET gamePoints = ?, updateTime = CURRENT_TIMESTAMP() WHERE accId = ? AND worldId = ?")) {
                ps.setInt(1, amount);
                ps.setInt(2, getAccountID());
                ps.setInt(3, getWorld());
                ps.executeUpdate();
            }
        } catch (Exception Ex) {
            log.error("更新角色帐号的在线时间出现错误 - 数据库更新失败.", Ex);
        }
    }

    /*
     * 新增变量
     */
    public int getMaxLevelForSever() {
        return ServerConfig.CHANNEL_PLAYER_MAXLEVEL;
    }

    public int getMaxHpForSever() {
        return ServerConfig.CHANNEL_PLAYER_MAXHP;
    }

    public int getMaxMpForSever() {
        return ServerConfig.CHANNEL_PLAYER_MAXMP;
    }

    /**
     * 获取某个技能的最大伤害上限
     *
     * @param skillId
     * @return
     */
    public long getMaxDamageOver(int skillId) {
        long maxDamage = 10000000000L; //默认的攻击上限
        long skillMaxDamage; //技能的攻击最大上限
        int limitBreak = getStat().getLimitBreak(this); //武器突破极限的伤害上限
        int incMaxDamage = getStat().incMaxDamage; //潜能增加的伤害上限
        Skill skill;
        if (skillId <= 0 || (skill = SkillFactory.getSkill(skillId)) == null)
            return (limitBreak > maxDamage ? limitBreak : maxDamage) + incMaxDamage;
        if (skillId == 新手.升级特效)
            return maxDamage;
        skillMaxDamage = skill.getMaxDamageOver();
        if (skillId == 侠盗.暗杀 && getTotalSkillLevel(侠盗.暗杀_最大值提高) > 0) {
            skillMaxDamage = 60000000;
        } else if (skillId == 夜光.绝对死亡) {
            skillMaxDamage = maxDamage;
        }
        return (limitBreak > skillMaxDamage ? limitBreak : skillMaxDamage) + incMaxDamage;
    }

    /*
     * 测试倾向系统的经验
     */
    public void addTraitExp(int exp) {
        traits.get(MapleTraitType.craft).addExp(exp, this);
    }

    public void setTraitExp(int exp) {
        traits.get(MapleTraitType.craft).addExp(exp, this);
    }

    /*
     * 好感度系统
     */
    public int getLove() {
        return love;
    }

    public void setLove(int love) {
        this.love = love;
    }

    public void addLove(int loveChange) {
        this.love += loveChange;
        MessengerRankingWorker.getInstance().updateRankFromPlayer(this);
    }

    public long getLastLoveTime() {
        return lastlovetime;
    }

    public Map<Integer, Long> getLoveCharacters() {
        return lastdayloveids;
    }

    /*
     * 0 = 成功
     * 1 = 未知错误
     * 2 = 今天无法增加
     */
    public int canGiveLove(MapleCharacter from) {
        if (from == null || lastdayloveids == null) { //如果要加的对象为空 或者好感度列表为空
            return 1;

        } else if (lastdayloveids.containsKey(from.getId())) { //如果给别人加好感度的列表有有这个玩家
            long lastTime = lastdayloveids.get(from.getId()); //获取最后给这个玩家加好感度的时间
            if (lastTime >= System.currentTimeMillis() - 60 * 60 * 24 * 1000) {
                return 2;
            } else {
                return 0;
            }
        }
        return 0;
    }

    public void hasGiveLove(MapleCharacter to) {
        lastlovetime = System.currentTimeMillis();
        if (lastdayloveids.containsKey(to.getId())) {
            lastdayloveids.remove(to.getId());
        }
        lastdayloveids.put(to.getId(), System.currentTimeMillis());
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO lovelog (characterid, characterid_to) VALUES (?, ?)");
            ps.setInt(1, getId());
            ps.setInt(2, to.getId());
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            System.err.println("ERROR writing lovelog for char " + getName() + " to " + to.getName() + e);
        }
    }

    /*
     * 角色升级需要的经验
     */
    public long getExpNeededForLevel() {
        return GameConstants.getExpNeededForLevel(level);
    }

    /*
     * 角色的能力点
     */
    public int getPlayerStats() {
        return getHpApUsed() + stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp();
    }

    public int getMaxStats(boolean hpap) {
        //默认的 29 点
        int total = 29;
        //计算专职赠送
        if (!JobConstants.is新手职业(job)) { //非新手职业
            int jobSp = 0;
            if (JobConstants.isSeparatedSpJob(job)) { //是特殊职业
                jobSp = JobConstants.getSkillBookByJob(job) * 5;
            } else {
                jobSp += hpap ? 15 : 0; //其他普通职业的SP
            }
            total += jobSp;
        }
        //骑士团的计算
        if (JobConstants.is炎术士(job) || JobConstants.is夜行者(job)) {
            if (level <= 70) {
                total += getLevel() * 6;
            } else {
                total += 70 * 6;
                total += (level - 70) * 5;
            }
        } else {
            total += level * 5; //普通职业的计算
        }
        //是否计算投资到hp的点数 如果计算就要加上 不计算就要减去
        if (hpap) {
            total += getHpApUsed();
        } else {
            total -= getHpApUsed();
        }
        return total;
    }

    public boolean checkMaxStat() {
        return getGMLevel() <= 0 && getLevel() >= 10 && getPlayerStats() > getMaxStats(true) + 15;
    }

    public void resetStats(int str, int dex, int int_, int luk) {
        resetStats(str, dex, int_, luk, false);
    }

    public void resetStats(int str, int dex, int int_, int luk, boolean resetAll) {
        List<Pair<MapleStat, Long>> stat = new ArrayList<>(5);
        int total = stats.getStr() + stats.getDex() + stats.getLuk() + stats.getInt() + getRemainingAp(); //这个是直接重置能力点 不需要计算投资到hp的点数
        if (resetAll) {
            total = getMaxStats(false); //这个地方要减去投资到hp的能力点数
        }
        total -= str;
        stats.str = (short) str;
        total -= dex;
        stats.dex = (short) dex;
        total -= int_;
        stats.int_ = (short) int_;
        total -= luk;
        stats.luk = (short) luk;

        setRemainingAp((short) total);
        stats.recalcLocalStats(this);

        stat.add(new Pair<>(MapleStat.力量, (long) str));
        stat.add(new Pair<>(MapleStat.敏捷, (long) dex));
        stat.add(new Pair<>(MapleStat.智力, (long) int_));
        stat.add(new Pair<>(MapleStat.运气, (long) luk));
        stat.add(new Pair<>(MapleStat.AVAILABLEAP, (long) total));

        client.announce(MaplePacketCreator.updatePlayerStats(stat, false, this));
    }

    /*
     * 对角色进行SP技能初始化
     */
    public void spReset() {
        int skillLevel;
        Map<Integer, SkillEntry> oldList = new HashMap<>(getSkills());
        Map<Integer, SkillEntry> newList = new HashMap<>();
        for (Entry<Integer, SkillEntry> toRemove : oldList.entrySet()) {
            Skill skill = SkillFactory.getSkill(toRemove.getKey());
            if (!skill.isBeginnerSkill() && !skill.isSpecialSkill() && !skill.isHyperSkill()) {
                skillLevel = getSkillLevel(toRemove.getKey());
                if (skillLevel > 0) {
                    if (skill.canBeLearnedBy(getJob())) {
                        newList.put(toRemove.getKey(), new SkillEntry((byte) 0, toRemove.getValue().masterlevel, toRemove.getValue().expiration));
                    } else {
                        newList.put(toRemove.getKey(), new SkillEntry((byte) 0, (byte) 0, -1));
                    }
                }
            }
        }
        if (!newList.isEmpty()) {
            changeSkillsLevel(newList);
        }
        oldList.clear();
        newList.clear();
        int[] spToGive;
        if (JobConstants.is暗影双刀(getJob())) {
            spToGive = new int[6];
            if (getLevel() > 100) {
                spToGive[5] = (getLevel() - 100) * 3 + 4;
                spToGive[4] = 32 * 3 + 4;
                spToGive[3] = 17 * 3 + 4;
                spToGive[2] = 20 * 3 + 5;
                spToGive[1] = 10 * 3 + 5;
                spToGive[0] = 10 * 3 + 5;
            } else if (getLevel() > 70 && getLevel() <= 100) {
                spToGive[4] = (getLevel() - 70) * 3 + 4;
                spToGive[3] = 15 * 3 + 4;
                spToGive[2] = 15 * 3 + 4;
                spToGive[1] = 10 * 3 + 4;
                spToGive[0] = 10 * 3 + 5;
            } else if (getLevel() > 55 && getLevel() <= 70) {
                spToGive[3] = (getLevel() - 55) * 3 + 4;
                spToGive[2] = 15 * 3 + 4;
                spToGive[1] = 10 * 3 + 4;
                spToGive[0] = 10 * 3 + 5;
            } else if (getLevel() > 30 && getLevel() <= 55) {
                spToGive[2] = (getLevel() - 30) * 3 + 4;
                spToGive[1] = 10 * 3 + 4;
                spToGive[0] = 10 * 3 + 5;
            } else if (getLevel() > 20 && getLevel() <= 30) {
                spToGive[1] = (getLevel() - 20) * 3 + 4;
                spToGive[0] = 10 * 3 + 5;
            } else if (getLevel() > 10 && getLevel() <= 20) {
                spToGive[0] = (getLevel() - 10) * 3 + 5;
            }
        } else if (JobConstants.is神之子(getJob())) {
            spToGive = new int[2];
            if (getLevel() >= 100) {
                spToGive[0] = (getLevel() - 100) * 3 + 8;
                spToGive[1] = (getLevel() - 100) * 3 + 8;
            }
        } else if (JobConstants.isSeparatedSpJob(getJob())) {
            spToGive = new int[4];
            boolean isMagicJob = getJob() >= 200 && getJob() <= 232; //法师职业是8级转职
            if (getLevel() > 100) {
                spToGive[3] = (getLevel() - 100) * 3 + 4;
                spToGive[2] = 40 * 3 + 4;
                spToGive[1] = 30 * 3 + 5;
                spToGive[0] = 20 * 3 + 5 + (isMagicJob ? 6 : 0);
            } else if (getLevel() > 60 && getLevel() <= 100) {
                spToGive[2] = (getLevel() - 60) * 3 + 4;
                spToGive[1] = 30 * 3 + 5;
                spToGive[0] = 20 * 3 + 5 + (isMagicJob ? 6 : 0);
            } else if (getLevel() > 30 && getLevel() <= 60) {
                spToGive[1] = (getLevel() - 30) * 3 + 5;
                spToGive[0] = 20 * 3 + 5 + (isMagicJob ? 6 : 0);
            } else if (getLevel() > 10 && getLevel() <= 30) {
                spToGive[0] = (getLevel() - 10) * 3 + 5;
            }
        } else {
            spToGive = new int[1];
            spToGive[0] += (getJob() % 100 != 0 && getJob() % 100 != 1) ? ((getJob() % 10) + 3) : 0;
            if (getJob() % 10 >= 2) {
                spToGive[0] += 3;
            }
            spToGive[0] += (getLevel() - 10) * 3;
        }
        for (int i = 0; i < spToGive.length; i++) {
            setRemainingSp(spToGive[i], i);
        }
        updateSingleStat(MapleStat.AVAILABLESP, 0);
        client.announce(MaplePacketCreator.enableActions());
    }

    /*
     * 新增变量
     */
    public int getPlayerPoints() {
        return playerPoints;
    }

    public void setPlayerPoints(int gain) {
        playerPoints = gain;
    }

    public void gainPlayerPoints(int gain) {
        if (playerPoints + gain < 0) {
            return;
        }
        playerPoints += gain;
    }

    public int getPlayerEnergy() {
        return playerEnergy;
    }

    public void setPlayerEnergy(int gain) {
        playerEnergy = gain;
    }

    public void gainPlayerEnergy(int gain) {
        if (playerEnergy + gain < 0) {
            return;
        }
        playerEnergy += gain;
    }

    /*
     * Pvp变量
     */
    public MaplePvpStats getPvpStats() {
        return pvpStats;
    }

    public int getPvpKills() {
        return pvpKills;
    }

    public void gainPvpKill() {
        this.pvpKills += 1;
        this.pvpVictory += 1;
        if (pvpVictory == 5) {
            map.broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[Pvp] 玩家 " + getName() + " 已经达到 5 连斩。"));
        } else if (pvpVictory == 10) {
            client.getChannelServer().broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[Pvp] 玩家 " + getName() + " 已经达到 10 连斩。"));
        } else if (pvpVictory >= 20) {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[Pvp] 玩家 " + getName() + " 已经达到 " + pvpVictory + " 连斩。他(她)在频道 " + client.getChannel() + " 地图 " + map.getMapName() + " 中喊道谁能赐我一死."));
        } else {
            dropMessage(6, "当前: " + pvpVictory + " 连斩.");
        }
    }

    public int getPvpDeaths() {
        return pvpDeaths;
    }

    public void gainPvpDeath() {
        this.pvpDeaths += 1;
        this.pvpVictory = 0;
    }

    public int getPvpVictory() {
        return pvpVictory;
    }

    /*
     * 提示信息
     */
    public void dropTopMsg(String message) {
        client.announce(UIPacket.getTopMsg(message));
    }

    public void dropMidMsg(String message) {
        client.announce(UIPacket.clearMidMsg());
        client.announce(UIPacket.getMidMsg(message, true, 0));
    }

    public void clearMidMsg() {
        client.announce(UIPacket.clearMidMsg());
    }

    /*
     * 特殊的顶部公告
     */
    public void dropSpecialTopMsg(String message, int fontsize, int color) {
        dropSpecialTopMsg(message, 0x00, fontsize, color);
    }

    public void dropSpecialTopMsg(String message, int unk, int fontsize, int color) {
        if (fontsize < 10) {
            fontsize = 10;
        }
        client.announce(UIPacket.getSpecialTopMsg(message, unk, fontsize, color));
    }

    /*
     * 新增函数
     * 帐号下的角色统计计算每日活动次数
     */
    public int getEventCount(String eventId) {
        return getEventCount(eventId, 0);
    }

    public int getEventCount(String eventId, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int count = 0;
            PreparedStatement ps = con.prepareStatement("SELECT * FROM accounts_event WHERE accId = ? AND eventId = ?");
            ps.setInt(1, getAccountID());
            ps.setString(2, eventId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                /*
                 * 年：calendar.get(Calendar.YEAR)
                 * 月：calendar.get(Calendar.MONTH)+1
                 * 日：calendar.get(Calendar.DAY_OF_MONTH)
                 * 星期：calendar.get(Calendar.DAY_OF_WEEK)-1
                 */
                count = rs.getInt("count");
                Timestamp updateTime = rs.getTimestamp("updateTime");
                if (type == 0) {
                    Calendar sqlcal = Calendar.getInstance();
                    if (updateTime != null) {
                        sqlcal.setTimeInMillis(updateTime.getTime());
                    }
                    if (sqlcal.get(Calendar.DAY_OF_MONTH) + 1 <= Calendar.getInstance().get(Calendar.DAY_OF_MONTH) || sqlcal.get(Calendar.MONTH) + 1 <= Calendar.getInstance().get(Calendar.MONTH) || sqlcal.get(Calendar.YEAR) + 1 <= Calendar.getInstance().get(Calendar.YEAR)) {
                        count = 0;
                        PreparedStatement psu = con.prepareStatement("UPDATE accounts_event SET count = 0, updateTime = CURRENT_TIMESTAMP() WHERE accId = ? AND eventId = ?");
                        psu.setInt(1, getAccountID());
                        psu.setString(2, eventId);
                        psu.executeUpdate();
                        psu.close();
                    }
                }
            } else {
                PreparedStatement psu = con.prepareStatement("INSERT INTO accounts_event (accId, eventId, count, type) VALUES (?, ?, ?, ?)");
                psu.setInt(1, getAccountID());
                psu.setString(2, eventId);
                psu.setInt(3, 0);
                psu.setInt(4, type);
                psu.executeUpdate();
                psu.close();
            }
            rs.close();
            ps.close();
            return count;
        } catch (Exception Ex) {
            log.error("获取 EventCount 次数.", Ex);
            return -1;
        }
    }

    /*
     * 增加帐号下的角色统计计算每日活动次数
     */
    public void setEventCount(String eventId) {
        setEventCount(eventId, 0);
    }

    public void setEventCount(String eventId, int type) {
        setEventCount(eventId, type, 1);
    }

    public void setEventCount(String eventId, int type, int count) {
        int eventCount = getEventCount(eventId, type);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts_event SET count = ?, type = ?, updateTime = CURRENT_TIMESTAMP() WHERE accId = ? AND eventId = ?");
            ps.setInt(1, eventCount + count);
            ps.setInt(2, type);
            ps.setInt(3, getAccountID());
            ps.setString(4, eventId);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
            log.error("增加 EventCount 次数失败.", Ex);
        }
    }

    /*
     * 重置帐号下的角色统计计算每日活动次数
     */
    public void resetEventCount(String eventId) {
        resetEventCount(eventId, 0);
    }

    public void resetEventCount(String eventId, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE accounts_event SET count = 0, type = ?, updateTime = CURRENT_TIMESTAMP() WHERE accId = ? AND eventId = ?");
            ps.setInt(1, type);
            ps.setInt(2, getAccountID());
            ps.setString(3, eventId);
            ps.executeUpdate();
            ps.close();
        } catch (Exception Ex) {
            log.error("重置 EventCount 次数失败.", Ex);
        }
    }

    /*
     * 检测 林子灵技能
     */
    public void checkBeastTamerSkill() {
        // 11212 = 林之灵4转 这个职业默认是4转角色 如果不是返回 不进行下面的操作
        if (job != 11212 || level < 10) {
            return;
        }
        Skill skil;
        Map<Integer, SkillEntry> list = new HashMap<>();
        //检测新手技能 , 林之灵.猛鹰模式, 林之灵.猫咪模式 暂时不开放后面2个模式
        int[] skillIds = {林之灵.模式解除, 林之灵.巨熊模式, 林之灵.雪豹模式, 林之灵.守护者的身手, 林之灵.守护模式变更, 林之灵.精灵集中, 林之灵.猛鹰模式, 林之灵.猫咪模式};
        for (int i : skillIds) {
            skil = SkillFactory.getSkill(i);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(i, new SkillEntry((byte) 1, (byte) 1, -1));
            }
        }
        //驯兽魔法棒练习 30级开始自动学习 技能最大等级10    10级技能增加1级
        skil = SkillFactory.getSkill(林之灵.驯兽魔法棒练习);
        if (skil != null && level >= 30) {
            int oldskilllevel = getSkillLevel(skil);
            int newskilllevel = level / 10 - 2;
            if (newskilllevel > skil.getMaxLevel()) {
                newskilllevel = skil.getMaxLevel();
            }
            if (newskilllevel > 0 && newskilllevel > oldskilllevel && oldskilllevel < skil.getMaxLevel()) {
                list.put(林之灵.驯兽魔法棒练习, new SkillEntry((byte) newskilllevel, (byte) skil.getMaxLevel(), -1));
            }
        }
        //冒险岛守护勇士 120级学习 技能最大等级30
        if (level >= 120) {
            skil = SkillFactory.getSkill(林之灵.冒险岛守护勇士);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(林之灵.冒险岛守护勇士, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), -1));
            }
        }
        //林之灵之意志 150级学习 技能最大等级5
        if (level >= 150) {
            skil = SkillFactory.getSkill(林之灵.林之灵之意志);
            if (skil != null && getSkillLevel(skil) <= 0) {
                list.put(林之灵.林之灵之意志, new SkillEntry((byte) skil.getMaxLevel(), (byte) skil.getMaxLevel(), -1));
            }
        }
        //改变技能等级
        if (!list.isEmpty()) {
            changeSkillsLevel(list);
        }
    }

    /*
     * 获取林之灵当前模式投入的技能点数
     */
    public int getBeastTamerSkillLevels(int skillId) {
        int ret = 0;
        int mod = skillId / 10000;
        if (mod == 11200 || mod == 11210 || mod == 11211 || mod == 11212) {
            Map<Integer, SkillEntry> chrSkills = new HashMap<>(getSkills());
            for (Entry<Integer, SkillEntry> list : chrSkills.entrySet()) {
                Skill skill = SkillFactory.getSkill(list.getKey());
                if (list.getKey() / 10000 == mod && !skill.isLinkedAttackSkill()) {
                    ret += getSkillLevel(list.getKey());
                }
            }
        }
        return ret;
    }

    /*
     * 林之灵之修养 这个技能的等级是任务显示
     * 59340 技能的等级任务
     * 59341 技能当前的经验
     */
    public void gainBeastTamerSkillExp(int amount) {
        if (job != 11212 || level < 60 || amount < 0) {
            return;
        }
        Skill skil = SkillFactory.getSkill(林之灵.林之灵之修养);
        if (skil == null) {
            return;
        }
        //技能的等级任务
        MapleQuestStatus levelStat = getQuestNAdd(MapleQuest.getInstance(59340));
        if (levelStat.getCustomData() == null) {
            levelStat.setCustomData("1"); //默认为1级
        }
        if (levelStat.getStatus() != 1) {
            levelStat.setStatus((byte) 1);
        }
        //技能当前的经验
        MapleQuestStatus expStat = getQuestNAdd(MapleQuest.getInstance(59341));
        if (expStat.getCustomData() == null) {
            expStat.setCustomData("0"); //默认的经验
        }
        if (expStat.getStatus() != 1) {
            expStat.setStatus((byte) 1);
        }
        int skillLevel = Integer.parseInt(levelStat.getCustomData());
        //角色以前的技能等级 修复角色技能的等级设置
        if (getSkillLevel(skil) <= skillLevel) {
            Map<Integer, SkillEntry> sDate = new HashMap<>();
            sDate.put(skil.getId(), new SkillEntry((byte) skillLevel, (byte) skil.getMaxLevel(), -1));
            changeSkillLevel_Skip(sDate, false);
            updateQuest(levelStat, true);
            updateQuest(expStat, true);
            changed_skills = true;
            return;
        }
        int skillExp = Integer.parseInt(expStat.getCustomData());
        int needed = skil.getBonusExpInfo(skillLevel);
        int newExp = skillExp + amount;
        if (newExp >= needed) {
            if (skillLevel < skil.getMaxLevel()) {
                int newLevel = skillLevel + 1;
                if (newLevel > skil.getMaxLevel()) {
                    newLevel = skil.getMaxLevel();
                }
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                sDate.put(skil.getId(), new SkillEntry((byte) newLevel, (byte) skil.getMaxLevel(), -1));
                changeSkillLevel_Skip(sDate, false);
                levelStat.setCustomData(String.valueOf(newLevel));
                expStat.setCustomData("0");
                changed_skills = true;
            } else { //角色技能满级后的处理
                levelStat.setCustomData("30");
                expStat.setCustomData("10000");
            }
        } else {
            expStat.setCustomData(String.valueOf(newExp));
        }
        updateQuest(levelStat, true);
        updateQuest(expStat, true);
    }

    /*
     * 修复链接技能的等级
     */
    public void fixTeachSkillLevel() {
        int skillId = JobConstants.getTecahSkillID(getJob());
        //技能ID<=0 返回
        if (skillId <= 0) {
            return;
        }
        Skill skill = SkillFactory.getSkill(skillId);
        //如果技能为空 或者技能的最大等级小于等于1就返回不进行下面的操作
        if (skill == null || skill.getMaxLevel() <= 1) {
            return;
        }
        //下面开始检测操作
        if (skill.canBeLearnedBy(getJob())) {
            //未修改前的技能等级
            int oldlevel = getSkillLevel(skill);
            //新的技能等级 好像盛大是判断职业的等级来判断技能的等级
            int newlevel = level >= 210 ? 3 : level >= 120 ? 2 : 1;
            //当前技能的最大等级
            int maxlevel = skill.getMaxLevel();
            //检测新的等级是否大于技能最大等级
            if (newlevel > maxlevel) {
                newlevel = maxlevel;
            }
            //检测新的技能等级是否不等于老的技能等级
            if (newlevel != oldlevel) {
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                SkillEntry ret = getSkillEntry(skillId);
                if (ret != null) {
                    ret.skillevel = newlevel;
                    ret.masterlevel = (byte) skill.getMaxLevel();
                    sDate.put(skillId, ret);
                } else {
                    sDate.put(skillId, new SkillEntry((byte) newlevel, (byte) skill.getMaxLevel(), -1));
                }
                changeSkillsLevel(sDate);
            }
        }
    }

    public boolean isSamePartyId(int partyId) {
        return partyId > 0 && party != null && party.getPartyId() == partyId;
    }

    /*
     * 角色1次对怪物的最大伤害 totDamageToMob
     */
    public long getTotDamageToMob() {
        return totDamageToMob;
    }

    public void setTotDamageToMob(long totDamageToMob) {
        this.totDamageToMob = totDamageToMob;
    }

    /*
         * 使用符文的时间间隔
         */
    public void prepareFuWenTime(long time) {
        lastFuWenTime = System.currentTimeMillis() + time;
    }

    public int getLastFuWenTime() {
        if (lastFuWenTime <= 0) {
            lastFuWenTime = System.currentTimeMillis();
        }
        long time = lastFuWenTime - System.currentTimeMillis();
        if (time <= 0) {
            return 0;
        }
        return (int) time;
    }

    /*
     * 获取角色当前穿戴的技能皮肤信息
     */
    public Map<Integer, Integer> getSkillSkin() {
        Map<Integer, Integer> ret = new LinkedHashMap<>(); //封包发送需要的信息 [技能ID] [皮肤ID]
        List<Integer> theList = getInventory(MapleInventoryType.EQUIPPED).listSkillSkinIds();  //装备中的技能皮肤ID集合
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (Integer i : theList) {
            int skillId = ii.getSkillSkinFormSkillId(i);
            Skill skill = SkillFactory.getSkill(skillId);
            if (skill != null) { //&& skill.canBeLearnedBy(getJob())
                ret.put(skillId, i); //加入集合 [技能ID] [皮肤ID]
            }
        }
        return ret;
    }

    /*
     * 使用终极魔方对道具进行潜能
     */
    public ModifyItemPotential getItemPotential() {
        return ItemPotential;
    }

    public void setItemPotential(ModifyItemPotential potential) {
        this.ItemPotential = potential;
    }

    public void resetItemPotential() {
        this.ItemPotential = null;
    }

    /*
     * 从地图的1个位置移动到另外1个位置
     */
    public void instantMapWarp(int portalId) {
        if (map == null) {
            return;
        }
        MaplePortal portal = map.getPortal(portalId);
        if (portal == null) {
            portal = map.getPortal(0);
        }
        Point portalPos = new Point(portal.getPosition());
        client.announce(MaplePacketCreator.instantMapWarp(getId(), portalPos));
        checkFollow();
        map.movePlayer(this, portalPos);
    }

    /*
     * 移动到地图的另外1个坐标
     */
    public void instantMapWarp(Point portalPos) {
        if (map == null || portalPos == null) {
            return;
        }
        client.announce(MaplePacketCreator.instantMapWarp(getId(), portalPos));
        checkFollow();
        map.movePlayer(this, portalPos);
    }

    public final int[] StringtoInt(final String str) {
        int ret[] = new int[100]; //最大支持100个前置条件参数
        StringTokenizer toKenizer = new StringTokenizer(str, ",");
        String[] strx = new String[toKenizer.countTokens()];
        for (int i = 0; i < toKenizer.countTokens(); i++) {
            strx[i] = toKenizer.nextToken();
            ret[i] = Integer.valueOf(strx[i]);
        }
        return ret;
    }

    //高级任务系统 - 检查基础条件是否符合所有任务前置条件
    public final boolean MissionCanMake(final int missionid) {
        boolean ret = true;
        for (int i = 1; i < 5; i++) {
            if (!MissionCanMake(missionid, i)) { //检查每一个任务条件是否满足
                ret = false;
            }
        }
        return ret;
    }

    //高级任务系统 - 检查基础条件是否符合指定任务前置条件
    public final boolean MissionCanMake(final int missionid, final int checktype) {
        //checktype
        //1 检查等级范围
        //2 检查职业
        //3 检查物品
        //4 检查前置任务
        boolean ret = false;
        int minlevel = -1, maxlevel = -1; //默认不限制接任务的等级范围
        String joblist = "all", itemlist = "none", prelist = "none"; //默认所有职业可以接，默认不需要任何前置物品和任务
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT minlevel,maxlevel,joblist,itemlist,prelist FROM missionlist WHERE missionid = ?")) {
                ps.setInt(1, missionid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        minlevel = rs.getInt("minlevel");
                        maxlevel = rs.getInt("maxlevel");
                        joblist = rs.getString("joblist");
                        itemlist = rs.getString("itemlist");
                        prelist = rs.getString("prelist");
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionCanMake:", ex);
        }
        //判断检查条件是否吻合
        switch (checktype) {
            case 1: //判断级别是否符合要求
                if (minlevel > -1 && maxlevel > -1) { //双范围检查
                    if (this.getLevel() >= minlevel && this.getLevel() <= maxlevel) {
                        ret = true;
                    }
                } else if (minlevel > -1 && maxlevel == -1) { //只有最小限制
                    if (this.getLevel() >= minlevel) {
                        ret = true;
                    }
                } else if (minlevel == -1 && maxlevel > -1) { //只有最大限制
                    if (this.getLevel() <= maxlevel) {
                        ret = true;
                    }
                } else if (minlevel == -1 && maxlevel == -1) { //如果是默认值-1，表示任何等级都可以接
                    ret = true;
                }
                break;
            case 2: //检查职业是否符合要求
                if (joblist.equals("all")) { //所有职业多可以接
                    ret = true;
                } else {
                    for (int i : StringtoInt(joblist)) {
                        if (this.getJob() == i) { //只要自己的职业ID在这个清单里，就是符合要求，立即跳出检查
                            ret = true;
                            break;
                        }
                    }
                }
                break;
            case 3: //检查前置物品是否有
                if (itemlist.equals("none")) { //没有前置物品要求
                    ret = true;
                } else {
                    for (int i : StringtoInt(itemlist)) {
                        if (!this.haveItem(i)) { //如果没有清单里要求的物品，立即跳出检查
                            ret = false;
                            break;
                        }
                    }
                }
                break;
            case 4: //检查前置任务是否有完成
                if (prelist.equals("none")) { //前置任务是否完成
                    ret = true;
                } else {
                    for (int i : StringtoInt(prelist)) {
                        if (!MissionStatus(this.getId(), i, 0, 1)) { //如果要求的前置任务没完成或从来没接过，立即跳出检查
                            ret = false;
                            break;
                        }
                    }
                }
                break;
        }
        return ret;
    }

    //高级任务函数 - 得到任务的等级数据
    public final int MissionGetIntData(final int missionid, final int checktype) {
        //checktype
        //1 最小等级
        //2 最大等级
        int ret = -1;
        int minlevel = -1, maxlevel = -1;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT minlevel,maxlevel FROM missionlist WHERE missionid = ?")) {
                ps.setInt(1, missionid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        minlevel = rs.getInt("minlevel");
                        maxlevel = rs.getInt("maxlevel");
                    }
                }
            }
            //判断检查条件是否吻合
            switch (checktype) {
                case 1:
                    ret = minlevel;
                    break;
                case 2:
                    ret = maxlevel;
                    break;
            }
        } catch (final SQLException ex) {
            log.error("Error MissionGetIntData:", ex);
        }
        return ret;
    }

    //高级任务函数 - 得到任务的的字符串型数据
    public final String MissionGetStrData(final int missionid, final int checktype) {
        //checktype
        //1 任务名称
        //2 职业列表
        //3 物品列表
        //4 前置任务列表
        String ret = "";
        String missionname = "", joblist = "all", itemlist = "none", prelist = "none"; //默认所有职业可以接，默认不需要任何前置物品和任务
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT missionname,joblist,itemlist,prelist FROM missionlist WHERE missionid = ?")) {
                ps.setInt(1, missionid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        missionname = rs.getString("missionname");
                        joblist = rs.getString("joblist");
                        itemlist = rs.getString("itemlist");
                        prelist = rs.getString("prelist");
                    }
                }
            }
            //判断检查条件是否吻合
            switch (checktype) {
                case 1:
                    ret = missionname;
                    break;
                case 2:
                    ret = joblist;
                    break;
                case 3:
                    ret = itemlist;
                    break;
                case 4:
                    ret = prelist;
                    break;
            }
        } catch (SQLException ex) {
            log.error("Error MissionCanMake:", ex);
        }
        return ret;
    }

    //高级任务函数 - 直接输出需要的职业列表串
    public final String MissionGetJoblist(final String joblist) {
        StringBuilder ret = new StringBuilder();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            for (int i : StringtoInt(joblist)) {
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM joblist WHERE id = ?")) {
                    ps.setInt(1, i);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            ret.append(",").append(rs.getString("jobname"));
                        }
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionGetJoblist:", ex);
        }
        return ret.toString();
    }

    //高级任务系统 - 任务创建
    public final void MissionMake(final int charid, final int missionid, final int repeat, final long repeattime, final int lockmap, final int mobid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("INSERT INTO missionstatus VALUES (DEFAULT, ?, ?, ?, ?, ?, 0, DEFAULT, 0, 0, ?, 0, 0)")) {
                ps.setInt(1, missionid);
                ps.setInt(2, charid);
                ps.setInt(3, repeat);
                ps.setLong(4, repeattime);
                ps.setInt(5, lockmap);
                ps.setInt(6, mobid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMake:", ex);
        }
    }

    //高级任务系统 - 重新做同一个任务
    public final void MissionReMake(final int charid, final int missionid, final int repeat, final long repeattime, final int lockmap) {
        int finish = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET `repeat` = ?, repeattime = ?, lockmap = ?, finish = ?, minnum = 0 WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, repeat);
                ps.setLong(2, repeattime);
                ps.setInt(3, lockmap);
                ps.setInt(4, finish);
                ps.setInt(5, missionid);
                ps.setInt(6, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionFinish:", ex);
        }
    }

    //高级任务系统 - 任务完成
    public final void MissionFinish(final int charid, final int missionid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET finish = 1, lastdate = CURRENT_TIMESTAMP(), times = times+1, lockmap = 0 WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, missionid);
                ps.setInt(2, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            //log.error("Error MissionFinish:", ex);
        }
    }

    // 高级任务系统 - 获得任务完成次数
    public final int MissionGetFinish(final int charid, final int missionid) {
        int ret = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT times FROM missionstatus WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, missionid);
                ps.setInt(2, charid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ret = rs.getInt(1);
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionFinish:", ex);
        }
        return ret;
    }

    //高级任务系统 - 放弃任务
    public final void MissionDelete(final int charid, final int missionid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM missionstatus WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, missionid);
                ps.setInt(2, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionDelete:", ex);
        }
    }

    //高级任务系统 - 增加指定任务的打怪数量
    public final void MissionSetMinNum(final int charid, final int missionid, final int num) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET `minnum` = ? WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, num);
                ps.setInt(2, missionid);
                ps.setInt(3, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionAddNum:", ex);
        }
    }

    //高级任务系统 - 增加指定任务的打怪数量
    public final void MissionAddMinNum(final int charid, final int missionid, final int num) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET `minnum` = `minnum` + ? WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, num);
                ps.setInt(2, missionid);
                ps.setInt(3, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionAddNum:", ex);
        }
    }

    // 高级任务系统 - 获取任务已经完成的怪物数量
    public final int MissionGetMinNum(final int charid, final int missionid, final int mobid) {
        int ret = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            String sql;
            if (mobid == 0) {
                sql = "SELECT minnum FROM missionstatus WHERE charid = ? AND missionid = ?";
            } else {
                sql = "SELECT minnum FROM missionstatus WHERE charid = ? AND missionid = ? AND mobid = ?";
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (mobid == 0) {
                    ps.setInt(1, charid);
                    ps.setInt(2, missionid);
                } else {
                    ps.setInt(1, charid);
                    ps.setInt(2, missionid);
                    ps.setInt(3, mobid);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret = rs.getInt("minnum");
                        break;
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMob:", ex);
        }
        return ret;
    }

    // 高级任务系统 - 获取任务需要消灭的怪物数量
    public final int MissionGetMaxNum(final int charid, final int missionid, final int mobid) {
        int ret = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            String sql;
            if (mobid == 0) {
                sql = "SELECT maxnum FROM missionstatus WHERE charid = ? AND missionid = ?";
            } else {
                sql = "SELECT maxnum FROM missionstatus WHERE charid = ? AND missionid = ? AND mobid = ?";
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (mobid == 0) {
                    ps.setInt(1, charid);
                    ps.setInt(2, missionid);
                } else {
                    ps.setInt(1, charid);
                    ps.setInt(2, missionid);
                    ps.setInt(3, mobid);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret = rs.getInt("maxnum");
                        break;
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMob:", ex);
        }
        return ret;
    }

    // 高级任务系统 - 获取任务需要消灭的怪物ID
    public final int MissionGetMobId(final int charid, final int missionid) {
        int ret = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT mobid FROM missionstatus WHERE charid = ? AND missionid = ?")) {
                ps.setInt(1, charid);
                ps.setInt(2, missionid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret = rs.getInt("mobid");
                        break;
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMob:", ex);
        }
        return ret;
    }

    //高级任务系统 - 增加指定任务的打怪数量
    public final void MissionSetMobId(final int charid, final int missionid, final int mobid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET `mobid` = ? WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, mobid);
                ps.setInt(2, missionid);
                ps.setInt(3, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionAddNum:", ex);
        }
    }

    //高级任务系统 - 指定任务的需要最大打怪数量
    public final void MissionMaxNum(final int missionid, final int maxnum) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE missionstatus SET `maxnum` = ? WHERE missionid = ? AND charid = ?")) {
                ps.setInt(1, maxnum);
                ps.setInt(2, missionid);
                ps.setInt(3, this.getId());
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMaxNum:", ex);
        }
    }

    //高级任务系统 - 获取repeattime
    public final long MissionGetRepeattime(final int charid, final int missionid) {
        long ret = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT repeattime FROM missionstatus WHERE charid = ? AND missionid = ?")) {
                ps.setInt(1, charid);
                ps.setInt(2, missionid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ret = rs.getLong("repeattime");
                        break;
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionMob:", ex);
        }
        return ret;
    }

    //高级任务系统 - 放弃所有未完成任务
    public final void MissionDeleteNotFinish(final int charid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM missionstatus WHERE finish = 0 AND charid = ?")) {

                ps.setInt(1, charid);
                ps.executeUpdate();
            }
        } catch (final SQLException ex) {
            log.error("Error MissionDeleteNotFinish:", ex);
        }
    }

    /* 高级任务系统 - 获得任务是否可以做
     * checktype
     * 0 检查此任务是否被完成了
     * 1 检查此任务是否允许重复做
     * 2 检查此任务重复做的时间间隔是否到
     * 3 检查此任务是否到达最大的任务次数
     * 4 检查是否接过此任务，即是否第一次做这个任务
     * 5 检查是否接了锁地图传送的任务
     */
    public final boolean MissionStatus(final int charid, final int missionid, final int maxtimes, final int checktype) {
        boolean ret = false; //默认是可以做
        int MissionMake = 0; //默认是没有接过此任务
        long now = 0;
        long t = 0;
        Timestamp lastdate;
        int repeat = 0;
        int repeattime = 0;
        int finish = 0;
        int times = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            String sql;
            if (checktype == 5) {
                sql = "SELECT * FROM missionstatus WHERE lockmap = 1 AND charid = ?";
            } else {
                sql = "SELECT * FROM missionstatus WHERE missionid = ? AND charid = ?";
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                if (checktype == 5) {
                    ps.setInt(1, charid);
                } else {
                    ps.setInt(1, missionid);
                    ps.setInt(2, charid);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        lastdate = rs.getTimestamp("lastdate");
                        repeat = rs.getInt("repeat");
                        repeattime = rs.getInt("repeattime");
                        finish = rs.getInt("finish");
                        times = rs.getInt("times");
                        t = lastdate.getTime();
                        now = System.currentTimeMillis();
                        MissionMake = 1; //标明这个任务已经接过了
                    }
                }
            }
        } catch (final SQLException ex) {
            log.error("Error MissionStatus:", ex);
        }
        //判断检查状态类型
        switch (checktype) {
            case 0:
                if (finish == 1) {
                    ret = true;
                }
                break;
            case 1:
                if (repeat == 1) {
                    ret = true;
                }
                break;
            case 2:
                if (now - t > repeattime) { // 判断如果有没有到指定的重复做任务间隔时间
                    //已经到了间隔时间
                    ret = true;
                }
                break;
            case 3:
                if (times >= maxtimes) {
                    //任务到达最大次数
                    ret = true;
                }
                break;
            case 4:
                if (MissionMake == 1) {
                    //此任务已经接过了
                    ret = true;
                }
                break;
            case 5:
                if (MissionMake == 1) {
                    //已经接了锁地图的任务
                    ret = true;
                }
        }
        return ret;
    }

    public void gainItem(int code, int amount, String log) {
        MapleInventoryManipulator.addById(client, code, (short) amount, log);
    }

    public void fixOnlineTime() {
        int day = getIntRecord(GameConstants.CHECK_DAY);
        int enter = getIntNoRecord(GameConstants.ENTER_CASH_SHOP);
        if (enter > 0 && getDay() != day) {
            setTodayOnlineTime(0);
            initTodayOnlineTime();
        }
        updateTodayDate();
        updataEnterShop(false);
    }

    public int getDay() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.DATE);
    }

    public void updataEnterShop(boolean enter) {
        getQuestNAdd(MapleQuest.getInstance(GameConstants.ENTER_CASH_SHOP)).setCustomData(enter ? String.valueOf(1) : String.valueOf(0));
    }

    public Calendar getDaybyDay(int n2) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + n2);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.AM_PM, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public void setOnlineTime() {
        if (getLogintime() > 0) {
            long l2 = getDaybyDay(1).getTimeInMillis() - getLogintime();
            if (onlineTime < 0) {
                resetEventCount("OnlineTime", 0);
            }
            setEventCount("OnlineTime", 0, (int) (System.currentTimeMillis() - (l2 >= 86400000 ? getDaybyDay(1).getTimeInMillis() - 86400000 : getLogintime())));
            setLogintime(System.currentTimeMillis());
        }
    }

    public void initOnlineTime() {
        onlineTime = getEventCount("OnlineTime", 0);
    }

    public int getOnlineTime() {
        long l2;
        long l3 = getDaybyDay(1).getTimeInMillis() - getLogintime();
        if (onlineTime < 0) {
            resetEventCount("OnlineTime", 0);
            initOnlineTime();
        }
        if (l3 >= 86400000) {
            l2 = getDaybyDay(1).getTimeInMillis() - 86400000;
            setLogintime(getDaybyDay(0).getTimeInMillis());
        } else {
            l2 = getLogintime() - onlineTime;
        }
        return (int) (System.currentTimeMillis() - l2);
    }

    public void setOnlineTime(long onlineTime) {
        this.onlineTime = onlineTime;
    }

    public long getLTime() {
        return ltime;
    }

    public void setLTime() {
        this.ltime = System.currentTimeMillis();
    }

    public void updateTodayDate() {
        getQuestNAdd(MapleQuest.getInstance(GameConstants.CHECK_DAY)).setCustomData(String.valueOf(getDay()));
    }

    public int getTodayOnlineTime() {
        return todayonlinetime + (int) ((System.currentTimeMillis() - todayonlinetimestamp.getTime()) / 60000);
    }

    public void setTodayOnlineTime(int time) {
        todayonlinetime = time;
    }

    public int getTotalOnlineTime() {
        return totalonlinetime + (int) ((System.currentTimeMillis() - todayonlinetimestamp.getTime()) / 60000);
    }

    public void initTodayOnlineTime() {
        todayonlinetimestamp = new Timestamp(System.currentTimeMillis());
    }

    public void finishActivity(final int questid) {
        MapleActivity.finish(this, questid);
    }

    public final void openNpc(final int id) {
        openNpc(id, null);
    }

    public final void openNpc(final int id, final String mode) {
        getClient().removeClickedNPC();
        NPCScriptManager.getInstance().dispose(getClient());
        NPCScriptManager.getInstance().start(getClient(), id, mode);
    }

    public void updateVisitorKills(int n2, int n3) {
        client.announce(MaplePacketCreator.updateVisitorKills(n2, n3));
    }

    /*
     * 检测称号等级
     */
    public void checkNameLevel() {
        if (!questinfo.containsKey(18127)) {
            updateInfoQuest(18127, "ago=" + this.getVip() + ";step=0;gD=14/08/08;FP=25;adm=1", false);
        }
    }

    public void AutoCelebrityCrit() {
        final int skilllevel = getSkillLevel(侠盗.名流爆击);
        if (skilllevel <= 0 || celebrityCrit != null) {
            return;
        }
        celebrityCrit = BuffTimer.getInstance().register(() -> {
            Skill crit = SkillFactory.getSkill(侠盗.名流爆击);
            MapleStatEffect effect = crit.getEffect(skilllevel);
            Map<MapleBuffStat, Integer> stat = Collections.singletonMap(MapleBuffStat.暴击蓄能, skilllevel * 2);
            client.announce(BuffPacket.giveBuff(crit.getId(), 0, stat, effect, client.getPlayer()));
//                SkillFactory.getSkill(刀飞.名流爆击).getEffect(skilllevel).applyTo(client.getPlayer());
        }, 1000 * 10);
    }

    public void cannelAutoCelebrityCrit() {
        if (celebrityCrit != null) {
            celebrityCrit.cancel(true);
            celebrityCrit = null;
        }
    }

    public void switchLuckyMoney(boolean on) {
        if (on && luckymoney) {
            return;
        }
        luckymoney = on;
        client.announce(BuffPacket.switchLuckyMoney(on));
    }

    // 更改伤害皮肤
    public void changeDamageSkin(int id) {
        MapleQuest q = MapleQuest.getInstance(7291);
        if (q == null) {
            return;
        }
        MapleQuestStatus status = getQuestNAdd(q);
        status.setStatus((byte) 1);
        status.setCustomData(String.valueOf(id));
        updateQuest(status, true);
        map.broadcastMessage(InventoryPacket.showDamageSkin(getId(), id)); //发送给其他玩家显示角色更换技能皮肤
        dropMessage(-9, "伤害皮肤已更改。");
    }

    /*
     * 获取伤害皮肤的数值
     */
    public int getDamageSkin() {
        if (JobConstants.is神之子(job)) {
            return 0;
        }
        MapleQuestStatus stat = getQuestNAdd(MapleQuest.getInstance(7291));
        stat.setStatus((byte) 1); //设置任务为进行中
        if (stat.getCustomData() == null) {
            stat.setCustomData("0");
        }
        return Integer.parseInt(stat.getCustomData());
    }

    public final void insertRanking(String rankingname, int value) {
        RankingTop.getInstance().insertRanking(this, rankingname, value);
    }

    public void getNpcNotice(int npcid, String text, int time) {
        getClient().announce(UIPacket.getNpcNotice(npcid, text, time * 1000));
    }

    public synchronized Channel getChatSession() {
        return chatSession;
    }

    public void setChatSession(Channel session) {
        chatSession = session;
    }

    public int getWeaponPoint() {
        return weaponPoint;
    }

    public void setWeaponPoint(int wp) {
        this.weaponPoint = wp;
        client.announce(MaplePacketCreator.showGainWeaponPoint(wp));
        client.announce(MaplePacketCreator.updateWeaponPoint(getWeaponPoint()));
    }

    public void gainWeaponPoint(int wp) {
        this.weaponPoint += wp;
        if (wp > 0) {
            client.announce(MaplePacketCreator.showGainWeaponPoint(wp));
        }
        client.announce(MaplePacketCreator.updateWeaponPoint(getWeaponPoint()));
    }

    public Map<String, Integer> getCredits() {
        return credit;
    }

    public void setCredit(String name, int value) {
        credit.put(name, value);
    }

    public void gainCredit(String name, int value) {
        credit.put(name, getCredit(name) + value);
    }

    public int getCredit(String name) {
        if (credit.containsKey(name)) {
            return credit.get(name);
        }
        return 0;
    }

    public int getJianQi() {
        return specialStats.getJianQi();
    }

    public void setJianQi(int jianqi) {
        specialStats.setJianQi(jianqi);
        getClient().announce(MaplePacketCreator.updateJianQi(specialStats.getJianQi()));
    }

    public boolean checkSoulWeapon() {
        Equip weapon = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        return weapon != null && weapon.getSoulEnchanter() != 0;
    }

    public boolean isSoulWeapon(Equip equip) {
        return equip != null && equip.getSoulEnchanter() != 0;
    }

    public void equipSoulWeapon(Equip equip) {
        changeSkillLevel(new Skill(getEquippedSoulSkill()), (byte) -1, (byte) 0);
        changeSkillLevel(new Skill(equip.getSoulSkill()), (byte) 1, (byte) 1);
        setSoulCount((short) 0);
        getClient().announce(BuffPacket.giveSoulGauge(getSoulCount(), equip.getSoulSkill()));
    }

    public void unequipSoulWeapon(Equip equip) {
        changeSkillLevel(new Skill(equip.getSoulSkill()), (byte) -1, (byte) 0);
        setSoulCount((short) 0);

        getClient().announce(BuffPacket.cancelSoulGauge());
        getMap().broadcastMessage(BuffPacket.cancelForeignSoulEffect(getId()));
    }

    public void writeSoulPacket() {
        int soulSkill = getEquippedSoulSkill();
        getClient().announce(BuffPacket.giveSoulGauge(addgetSoulCount(), soulSkill));
    }

    public void checkSoulState(boolean useskill) {
        int skillid = getEquippedSoulSkill();
        Skill skill = SkillFactory.getSkill(skillid);
        if (skill != null) {
            MapleStatEffect effect = skill.getEffect(getSkillLevel(skillid));
            long cooldown = getCooldownLimit(skillid);
            if (useskill) {
                if (getSoulCount() >= effect.getSoulMpCon()) {
                    List<ModifyInventory> mods = new ArrayList<>();
                    setSoulCount((short) (getSoulCount() - effect.getSoulMpCon()));
                    getClient().announce(BuffPacket.giveSoulEffect(0));
                    getMap().broadcastMessage(BuffPacket.cancelForeignSoulEffect(getId()));
                    client.announce(InventoryPacket.modifyInventory(true, mods, this));
                }
            } else if (getSoulCount() >= effect.getSoulMpCon()) {
                if (cooldown <= 0) {
                    getClient().announce(BuffPacket.giveSoulEffect(skillid));
                    getMap().broadcastMessage(this, BuffPacket.giveForeignSoulEffect(getId(), skillid), false);
                }
            }
        }
    }

    public short getSoulCount() {
        return soulcount;
    }

    public void setSoulCount(short soulcount) {
        this.soulcount = soulcount > 1000 ? 1000 : soulcount;
    }

    public void addSoulCount() {
        if (soulcount < 1000) {
            this.soulcount++;
        }
    }

    public short addgetSoulCount() {
        addSoulCount();
        return getSoulCount();
    }

    public int getEquippedSoulSkill() {
        Equip weapon = (Equip) getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        if (weapon == null) {
            return 0;
        }
        return weapon.getSoulSkill();
    }

    public int getSoulSkillMpCon() {
        int skillid = getEquippedSoulSkill();
        Skill skill = SkillFactory.getSkill(skillid);
        MapleStatEffect effect;
        if (skill != null && (effect = skill.getEffect(getSkillLevel(skillid))) != null) {
            return effect.getSoulMpCon();
        }
        return 0;
    }

    public void iNeedSystemProcess() {
        setLastCheckProcess(System.currentTimeMillis());
        this.getClient().announce(MaplePacketCreator.SystemProcess());
    }

    public long getLastCheckProcess() {
        return lastCheckProcess;
    }

    public void setLastCheckProcess(long lastCheckProcess) {
        this.lastCheckProcess = lastCheckProcess;
    }

    public List<MapleProcess> getProcess() {
        return Process;
    }

    public void send(final byte[] array) {
        getClient().announce(array);
    }

    public void send_other(final byte[] array, final boolean b) {
        getMap().broadcastMessage(this, array, b);
    }

    public List<Integer> getEffectSwitch() {
        return effectSwitch;
    }

    public void updateEffectSwitch(int pos) {
        for (Integer poss : effectSwitch) {
            if (poss == pos) {
                effectSwitch.remove(poss);
                return;
            }
        }
        effectSwitch.add(pos);
    }

    public void gainPP(int pp) {
        specialStats.gainPP(pp);
        client.announce(BuffPacket.showPP(specialStats.getPP(), job));
    }

    public int getMobKills() {
        return mobKills;
    }

    public void setMobKills(int mobKills) {
        this.mobKills = mobKills;
    }

    public long getLastMobKillTime() {
        return lastMobKillTime;
    }

    public void setLastMobKillTime(long lastMobKillTime) {
        this.lastMobKillTime = lastMobKillTime;
    }

    public void handle影子分裂(int oid, Point point) {
        Skill BuffSkill = SkillFactory.getSkill(夜行者.影子蝙蝠);
        Skill skill_1 = SkillFactory.getSkill(夜行者.影子蝙蝠);
        Skill skill_2 = SkillFactory.getSkill(夜行者.蝙蝠掌控);
        Skill skill_4 = SkillFactory.getSkill(夜行者.蝙蝠掌控Ⅲ);
        int skillLevel_1 = getSkillLevel(BuffSkill);
        int skillLevel_2 = getSkillLevel(skill_2);
        int skillLevel_4 = getSkillLevel(skill_4);
        if (skillLevel_1 < 0) {
            return;
        }
        MapleStatEffect effect = BuffSkill.getEffect(skillLevel_1);
        int prop = effect.getProp();

        if (skillLevel_2 > 0) {
            prop += skill_2.getEffect(skillLevel_2).getProp();
        }

        if (skillLevel_4 > 0) {
            prop += skill_2.getEffect(skillLevel_4).getProp();
        }

        if (Randomizer.nextInt(100) > prop) {
            specialStats.gainForceCounter();
            getMap().broadcastMessage(MaplePacketCreator.Show影子蝙蝠锁定(getId(), 夜行者.影子蝙蝠_攻击, oid, 1, point, specialStats.getForceCounter()), getTruePosition());
            specialStats.gainForceCounter();
        }
    }

    public MapleSigninStatus getSigninStatus() {
        return siginStatus;
    }

    /**
     * 初始化每日签到状态
     */
    public void initSigninStatus() {
        if (level >= MapleSignin.MINLEVEL) {
            siginStatus = new MapleSigninStatus(getInfoQuest(GameConstants.每日签到系统_签到记录));
            updateInfoQuest(GameConstants.每日签到系统_当前时间, MapleSignin.getCurrentTime(), false);
        }
    }

    public int getPQLog(String pqName) {
        return this.getPQLog(pqName, 0);
    }

    public int getPQLog(String pqName, int times) {
        return this.getPQLog(pqName, times, 1);
    }

    public int getDaysPQLog(String pqName, int times) {
        return this.getDaysPQLog(pqName, 0, times);
    }

    public int getDaysPQLog(String pqName, int times, int day) {
        return this.getPQLog(pqName, times, day);
    }

    public int getPQLog(String pqName, int times, int day) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            int n4 = 0;
            try (PreparedStatement ps = con.prepareStatement("SELECT `count`,`time` FROM pqlog WHERE characterid = ? AND pqname = ?")) {
                ps.setInt(1, this.id);
                ps.setString(2, pqName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        n4 = rs.getInt("count");
                        Timestamp timestamp = rs.getTimestamp("time");
                        rs.close();
                        ps.close();
                        if (times == 0) {
                            int n5;
                            Calendar calendar = Calendar.getInstance();
                            Calendar calendar2 = Calendar.getInstance();
                            if (timestamp != null) {
                                calendar2.setTimeInMillis(timestamp.getTime());
                                calendar2.add(6, day);
                            }
                            if (calendar.get(1) - calendar2.get(1) > 1) {
                                n5 = 0;
                            } else if (calendar.get(1) - calendar2.get(1) >= 0) {
                                if (calendar.get(1) - calendar2.get(1) > 0) {
                                    calendar2.add(1, 1);
                                }
                                n5 = calendar.get(6) - calendar2.get(6);
                            } else {
                                n5 = -1;
                            }
                            if (n5 >= 0) {
                                n4 = 0;
                                try (PreparedStatement psi = con.prepareStatement("UPDATE pqlog SET count = 0, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?")) {
                                    psi.setInt(1, this.id);
                                    psi.setString(2, pqName);
                                    psi.executeUpdate();
                                }
                            }
                        }
                    } else {
                        try (PreparedStatement pss = con.prepareStatement("INSERT INTO pqlog (characterid, pqname, count, type) VALUES (?, ?, ?, ?)")) {
                            pss.setInt(1, this.id);
                            pss.setString(2, pqName);
                            pss.setInt(3, 0);
                            pss.setInt(4, times);
                            pss.executeUpdate();
                        }
                    }
                }
            }
            return n4;
        } catch (SQLException sQLException) {
            System.err.println("Error while get pqlog: " + sQLException);
            return -1;
        }
    }

    public void setPQLog(String pqname) {
        this.setPQLog(pqname, 0);
    }

    public void setPQLog(String pqname, int type) {
        this.setPQLog(pqname, type, 1);
    }

    public void setPQLog(String pqname, int type, int count) {
        int times = this.getPQLog(pqname, type);
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE pqlog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?")) {
                ps.setInt(1, times + count);
                ps.setInt(2, type);
                ps.setInt(3, this.id);
                ps.setString(4, pqname);
                ps.executeUpdate();
            }
        } catch (SQLException sQLException) {
            System.err.println("Error while set pqlog: " + sQLException);
        }
    }

    public void resetPQLog(String pqname) {
        this.resetPQLog(pqname, 0);
    }

    public void resetPQLog(String pqname, int type) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE pqlog SET count = ?, type = ?, time = CURRENT_TIMESTAMP() WHERE characterid = ? AND pqname = ?")) {
                ps.setInt(1, 0);
                ps.setInt(2, type);
                ps.setInt(3, this.id);
                ps.setString(4, pqname);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error while reset pqlog: " + e);
        }
    }

    public long getPQPoint() {
        return Long.valueOf(this.getOneInfo(7907, "point") != null ? this.getOneInfo(7907, "point") : "0");
    }

    public void gainPQPoint(long point) {
        this.updateOneInfo(7907, "point", String.valueOf(this.getPQPoint() + point));
    }

    public void doneSailQuestion() {
        for (int i2 = 0; i2 <= 4; ++i2) {
            MapleQuest.getInstance(17003 + i2).complete(this, 0, null);
            MapleQuest.getInstance(17003 + i2).complete(this, 0);
        }
        this.updateInfoQuest(17008, "T=0;L=1;E=0");
    }

    public long getLogintime() {
        return logintime;
    }

    public void setLogintime(long logintime) {
        this.logintime = logintime;
    }

    public long getQuestDiffTime() {
        return (System.currentTimeMillis() - this.logintime) / 1000 / 60;
    }

    public boolean isAttclimit() {
        return this.attclimit;
    }

    public void setAttclimit(boolean attclimit) {
        this.attclimit = attclimit;
    }

    public WeakReference<MapleReactor> getReactor() {
        return this.reactor;
    }

    public void setReactor(MapleReactor reactor) {
        this.reactor = new WeakReference<>(reactor);
    }

    public boolean isMainWeapon(int jobid, int weapon) {
        Item item = this.getInventory(MapleInventoryType.EQUIPPED).getItem((short) -11);
        MapleWeaponType type = ItemConstants.getWeaponType(weapon != 0 ? weapon : (item != null ? item.getItemId() : 0));
        switch (type) {
            case 没有武器: {
                return false;
            }
            case 单手剑: {
                return JobConstants.is英雄(jobid) || JobConstants.is圣骑士(jobid) || JobConstants.is米哈尔(jobid);
            }
            case 双手剑: {
                return JobConstants.is英雄(jobid) || JobConstants.is圣骑士(jobid) || JobConstants.is狂龙战士(jobid);
            }
            case 单手斧: {
                return JobConstants.is圣骑士(jobid) || JobConstants.is恶魔猎手(jobid);
            }
            case 双手斧: {
                return JobConstants.is圣骑士(jobid);
            }
            case 单手钝器: {
                return JobConstants.is英雄(jobid) || JobConstants.is恶魔猎手(jobid);
            }
            case 双手钝器: {
                return JobConstants.is英雄(jobid);
            }
            case 矛: {
                return JobConstants.is黑骑士(jobid) || JobConstants.is战神(jobid);
            }
            case 枪: {
                return JobConstants.is黑骑士(jobid);
            }
            case 弓: {
                return JobConstants.is风灵使者(jobid) || JobConstants.is神射手(jobid);
            }
            case 弩: {
                return JobConstants.is箭神(jobid) || JobConstants.is豹弩游侠(jobid);
            }
            case 双弩枪: {
                return JobConstants.is双弩精灵(jobid);
            }
            case 短杖:
            case 长杖: {
                return JobConstants.is魔法师(jobid) || JobConstants.is唤灵斗师(jobid);
            }
            case 大剑:
            case 太刀: {
                return JobConstants.is神之子(jobid);
            }
            case 手持火炮: {
                return JobConstants.is火炮手(jobid);
            }
            case 双头杖: {
                return JobConstants.is夜光(jobid);
            }
            case 灵魂手铳: {
                return JobConstants.is爆莉萌天使(jobid);
            }
            case 亡命剑: {
                return JobConstants.is恶魔复仇者(jobid);
            }
            case 能量剑: {
                return JobConstants.is尖兵(jobid);
            }
            case 驯兽魔法棒: {
                return JobConstants.is林之灵(jobid);
            }
            case 短刀:
            case 双刀副手: {
                return JobConstants.is双刀(jobid) || JobConstants.is侠盗(jobid);
            }
            case 特殊副手:
            case 手杖: {
                return JobConstants.is幻影(jobid);
            }
            case 拳套: {
                return JobConstants.is隐士(jobid) || JobConstants.is夜行者(jobid);
            }
            case 指节: {
                return JobConstants.is冲锋队长(jobid);
            }
            case 短枪: {
                return JobConstants.is机械师(jobid) || JobConstants.is船长(jobid) || JobConstants.is龙的传人(jobid);
            }
            case 武士刀: {
                return JobConstants.is剑豪(jobid);
            }
            case 折扇: {
                return JobConstants.is阴阳师(jobid);
            }
            case 左轮: {
                return JobConstants.is爆破手(jobid);
            }
        }
        return false;
    }

    public void sendEnableActions() {
        getClient().announce(MaplePacketCreator.enableActions());
    }

    public List<Integer> getDamSkinList() {
        return damSkinList;
    }

    public void initAllInfo() {
        if (this.getInfoQuest(1).isEmpty()) {
            this.updateInfoQuest(1, "accept=1;date=15/06/25", false);
        }
        if (this.getInfoQuest(7).isEmpty()) {
            this.updateInfoQuest(7, "count=0;day=0;date=0;nowMonth=0", false);
        }
        this.initDamageSkinList();
    }

    public void initDamageSkinList() {
        final String keyValue = this.getKeyValue("DAMAGE_SKIN");
        final String keyValue2 = this.getKeyValue("DAMAGE_SKIN_SLOT");
        if (keyValue != null && !keyValue.isEmpty()) {
            final String[] split = keyValue.split(",");
            for (String aSplit : split) {
                if (!aSplit.isEmpty()) {
                    final int intValue = Integer.valueOf(aSplit);
                    if (!this.getDamSkinList().contains(intValue)) {
                        this.getDamSkinList().add(intValue);
                    }
                }
            }
        }
        if (keyValue2 == null || keyValue2.isEmpty()) {
            this.setKeyValue("DAMAGE_SKIN_SLOT", "1");
        }
    }

    /*
     * 充值函数 1 = 当前充值金额 2 = 已经消费金额 3 = 总计消费金额 4 = 充值奖励
     */
    public int getHyPay(int type) {
        int pay = 0;
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT * FROM hypay WHERE accname = ?")) {
                ps.setString(1, getClient().getAccountName());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (type == 1) { //当前充值记录
                            pay = rs.getInt("pay");
                        } else if (type == 2) { //已消费记录
                            pay = rs.getInt("payUsed");
                        } else if (type == 3) { //当前消费总额
                            pay = rs.getInt("pay") + rs.getInt("payUsed");
                        } else if (type == 4) { //充值奖励
                            pay = rs.getInt("payReward");
                        } else {
                            pay = 0;
                        }
                    } else {
                        try (PreparedStatement psu = con.prepareStatement("INSERT INTO hypay (accname, pay, payUsed, payReward) VALUES (?, ?, ?, ?)")) {
                            psu.setString(1, getClient().getAccountName());
                            psu.setInt(2, 0); //当前充值金额
                            psu.setInt(3, 0); //已经消费金额
                            psu.setInt(4, 0); //消费奖励
                            psu.executeUpdate();
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("获取充值信息发生错误", e);
        }
        return pay;
    }

    public int addHyPay(int hypay) {
        int pay = getHyPay(1);
        int payUsed = getHyPay(2);
        int payReward = getHyPay(4);
        if (hypay > pay) {
            return -1;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE hypay SET pay = ? ,payUsed = ? ,payReward = ? WHERE accname = ?")) {
                ps.setInt(1, pay - hypay); //当前充值金额
                ps.setInt(2, payUsed + hypay); //已经消费金额
                ps.setInt(3, payReward + hypay); //消费奖励
                ps.setString(4, getClient().getAccountName());
                ps.executeUpdate();
                return 1;
            }
        } catch (SQLException e) {
            log.error("加减充值信息发生错误", e);
            return -1;
        }
    }

    public int delPayReward(int pay) {
        int payReward = getHyPay(4);
        if (pay <= 0) {
            return -1;
        }
        if (pay > payReward) {
            return -1;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("UPDATE hypay SET payReward = ? WHERE accname = ?")) {
                ps.setInt(1, payReward - pay); //消费奖励
                ps.setString(2, getClient().getAccountName());
                ps.executeUpdate();
                return 1;
            }
        } catch (SQLException ex) {
            log.error("加减消费奖励信息发生错误", ex);
            return -1;
        }
    }

    public int get免费超时空券() {
        return 7 - this.transWarpTicket;
    }

    public void initTransWarpTicket() {
        this.transWarpTicket = this.getDaysPQLog("免费超时空券", 7);
    }

    public int get超时空券() {
        String customData = this.getQuestNAdd(MapleQuest.getInstance(99997)).getCustomData();
        if (customData == null) {
            customData = "0";
        }
        return Integer.parseInt(customData);
    }

    public void set超时空券(final int n) {
        this.getQuestNAdd(MapleQuest.getInstance(99997)).setCustomData(String.valueOf(n));
    }

    public void gainVCraftCore(int quantity) {
        String data = getOneInfo(1477, "count");
        if (data != null) {
            int count = Integer.valueOf(data);
            updateOneInfo(1477, "count", String.valueOf(count + quantity));
        } else {
            updateOneInfo(1477, "count", String.valueOf(quantity));
        }
    }

    public void check5thJobQuest() {
        if (getQuestStatus(1465) == 1 && ltime > 10000000) {
            if (getQuestStatus(1474) == 1) {
                if (System.currentTimeMillis() - ltime >= 3600000) {
                    setLTime();
                    updateInfoQuest(1470, "on=1;remain=0;exp=500000000");
                    forceCompleteQuest(1474);
                }
            } else if (getQuestStatus(1475) == 1) {
                if (System.currentTimeMillis() - ltime >= 3600000) {
                    setLTime();
                    updateInfoQuest(1471, "on=1;remain=0;exp=500000000");
                    forceCompleteQuest(1475);
                }
            } else if (getQuestStatus(1476) == 1 && System.currentTimeMillis() - ltime >= 3600000) {
                setLTime();
                updateInfoQuest(1472, "on=1;remain=0;exp=500000000");
                forceCompleteQuest(1476);
            }
        }
    }

    public Map<Integer, VCoreSkillEntry> getVCoreSkill() {
        return Collections.unmodifiableMap(vCoreSkills);
    }

    public void setVCoreSkills(Map<Integer, VCoreSkillEntry> vCoreSkills) {
        this.vCoreSkills = vCoreSkills;
    }

    public void addVCoreSkill(VCoreSkillEntry ah2) {
        this.vCoreSkills.put(this.vCoreSkillIndex.getAndIncrement(), ah2);
        this.changed_vcores = true;
    }

    public void setVCoreSkillSlot(int coreid, int slot) {
        if (this.vCoreSkills.get(coreid) != null) {
            this.vCoreSkills.get(coreid).setSlot(slot);
            this.changed_vcores = true;
        }
    }

    public void removeVCoreSkill(int coreid) {
        if (this.vCoreSkills.get(coreid) != null) {
            this.vCoreSkills.get(coreid).setSlot(0);
            this.changed_vcores = true;
        }
    }

    public int getVCoreSkillLevel(int skillid) {
        int level = 0;
        for (VCoreSkillEntry vse : vCoreSkills.values()) {
            for (int i2 = 1; i2 <= 3; ++i2) {
                if (vse.getSlot() != 2 || vse.getSkill(i2) != skillid) continue;
                if (vse.getType() == 0 || vse.getType() == 2) {
                    return vse.getLevel();
                }
                if (vse.getType() != 1) continue;
                level += vse.getLevel();
            }
        }
        return level;
    }

    public void sendGiantBossMapChange(String string, String string2) {
        this.send_other(MaplePacketCreator.sendGhostPoint(string, string2), true);
    }

    public void showPortal(String string, String string2) {
        this.send_other(MaplePacketCreator.ShowPortal(string, Integer.valueOf(string2)), true);
    }

    public void gaintBossMap() {
        Map<String, String> hashMap = new HashMap<>();
        EventInstanceManager eim = this.getEventInstance();
        for (Entry<String, String> entry : eim.getInfoStats().entrySet()) {
            hashMap.put(entry.getKey(), eim.getProperty(entry.getKey()).equals("1") && !String.valueOf(this.getMapId()).equals(entry.getKey()) ? "0" : eim.getProperty(entry.getKey()));
        }
        this.sendGiantBossMap(hashMap);
    }

    public void sendGiantBossMap(Map<String, String> map) {
        client.announce(MaplePacketCreator.SendGiantBossMap(map));
    }

    public long getRuneNextActionTime() {
        long time = 0;
        if (getKeyValue("RUNE_NEXT_ACTION_TIME") != null) {
            time = Long.valueOf(getKeyValue("RUNE_NEXT_ACTION_TIME"));
        }
        return time;
    }

    public void setRuneNextActionTime(long time) {
        setKeyValue("RUNE_NEXT_ACTION_TIME", String.valueOf(System.currentTimeMillis() + time));
    }

    public void sendDathCount() {
        if (reviveCount >= 0) {
            client.announce(MaplePacketCreator.IndividualDeathCountInfo(reviveCount));
        }
    }

    public int getEnchant() {
        return enchant;
    }

    public void setEnchant(int enchant) {
        this.enchant = enchant;
    }

    public void handle无敌(MapleStatEffect effect, int bufflength) {
        send(BuffPacket.giveGodBuff(effect.getSourceid(), bufflength));
        Timer.BuffTimer.getInstance().register(() -> send(BuffPacket.cancelBuff(Collections.singletonList(MapleBuffStat.无敌状态), this)), bufflength);
    }

    public void updateBuffEffect(MapleStatEffect effect, Map<MapleBuffStat, Integer> map) {
        long time = System.currentTimeMillis() - getBuffStatValueHolder(map.keySet().iterator().next()).startTime;
        send(BuffPacket.giveBuff(effect.getSourceid(), effect.getDuration() - (int) time, map, effect, this));
    }

    public long getLastUseVSkillTime() {
        return lastUseVSkillTime;
    }

    public void setLastUseVSkillTime(long lastUseVSkillTime) {
        this.lastUseVSkillTime = lastUseVSkillTime;
    }

    public void hadnleBlasterClip(int n2) {
        if (JobConstants.is爆破手(job)) {
            int n3 = Math.max(specialStats.getBullet() + n2, 0);
            specialStats.setBullet(Math.min(n3, specialStats.getBullet()));
        }
    }

    public void hadnleCylinder(int n2) {
        if (JobConstants.is爆破手(job)) {
            int n3 = Math.max(getCylinder() + n2, 0);
            specialStats.setCylinder(Math.min(n3, specialStats.getBullet()));
        }
    }

    public int getCylinder() {
        return JobConstants.is爆破手(job) ? specialStats.getCylinder() : 0;
    }

    public int getBullet() {
        return JobConstants.is爆破手(job) ? specialStats.getBullet() : 0;
    }

    public void hadnleBlasterSkill(int skillid) {
        if (JobConstants.is爆破手(job)) {
            switch (skillid) {
                case 爆破手.狂暴打击:
                case 爆破手.双重爆炸:
                case 爆破手.旋转弹: {
                    this.send(MaplePacketCreator.SkillFeed());
                    this.send(MaplePacketCreator.RegisterExtraSkill(this, skillid, 爆破手.忍耐之盾));
                }
            }
        }
    }

    public void hadnleChargeBlaster() {
        MapleStatEffect effect = SkillFactory.getSkill(爆破手.装填弹药).getEffect(getSkillLevel(爆破手.装填弹药));
        if (JobConstants.is爆破手(job) && getBullet() <= 0 && effect != null) {
            effect.applyTo(this);
        }
    }

    public int getHurtHP() {
        return specialStats.getHurtHP();
    }

    public void hadnle忍耐之盾(int n2) {
        if (JobConstants.is爆破手(job) && getStatForBuff(MapleBuffStat.忍耐之盾) == null) {
            specialStats.setHurtHP(n2);
            MapleStatEffect effect = SkillFactory.getSkill(爆破手.忍耐之盾).getEffect(getTotalSkillLevel(爆破手.忍耐之盾));
            if (this.isAlive() && effect != null) {
                effect.applyTo(this, true);
            }
        }
    }

    public void updateShinasBless() {
        int[] arrn = new int[]{80000066, 80000067, 80000068, 80000069, 80000070};
        int n2 = 0;
        Optional<Skill> skill = Optional.ofNullable(SkillFactory.getSkill(80000055));
        for (int n3 : arrn) {
            Skill an3 = SkillFactory.getSkill(n3);
            if (an3 != null && this.getSkillLevel(an3) > 0) {
                n2 += this.getSkillLevel(an3);
            }
        }
        int finalN = n2;
        skill.ifPresent(skill1 -> changeSkillData(skill1, finalN == 0 ? -1 : finalN, (byte) skill1.getMasterLevel(), -1));

    }

    public void doAnimabuff() {
        if (JobConstants.is唤灵斗师(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.战法灵气);
            if (effect != null) {
                switch (effect.getSourceid()) {
                    case 32001016:
                    case 32101009:
                    case 32111012:
                    case 32121017: {
                        if (this.getParty() != null) {
                            effect.applyTo(this, false);
                        }
                        break;
                    }
                    case 32121018: {
                        effect.applyTo(this, true);
                    }
                }
                addMP(-effect.getMpCon());
                if (getStat().getMp() < effect.getMpCon()) {
                    cancelEffectFromBuffStat(MapleBuffStat.战法灵气);
                }
            }
        } else if (JobConstants.is神之子(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.神圣迅捷);
            if (effect != null && getParty() != null) {
                effect.applyTo(this, false);
            }
            if ((effect = getStatForBuff(MapleBuffStat.圣洁之力)) != null && getParty() != null) {
                effect.applyTo(this, false);
            }
        } else if (JobConstants.is火毒(getJob())) {
            int n2 = this.getTotalSkillLevel(2100009);
            if (n2 > 0) {
                Skill skill = SkillFactory.getSkill(2100009);
                MapleStatEffect effect = skill.getEffect(n2);
                if (effect.a(this, MonsterStatus.MOB_STAT_Poison) > 0) {
                    effect.applyTo(this);
                } else {
                    cancelEffectFromBuffStat(MapleBuffStat.元素爆破);
                }
            }
        } else if (JobConstants.is米哈尔(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.灵魂链接);
            MapleBuffStatValueHolder valueHolder = getBuffStatValueHolder(MapleBuffStat.灵魂链接);
            if (effect != null && getParty() != null && valueHolder != null && valueHolder.fromChrId == getId()) {
                addMP(-effect.getMpCon());
                effect.applyTo(this, false);
                if (getStat().getMp() < effect.getMpCon()) {
                    cancelEffectFromBuffStat(MapleBuffStat.灵魂链接);
                }
            }
        } else if (JobConstants.is剑豪(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.拔刀术加成);
            MapleBuffStatValueHolder valueHolder = getBuffStatValueHolder(MapleBuffStat.拔刀术加成);
            if (effect != null && valueHolder != null && valueHolder.fromChrId == getId()) {
                addMP(-3);
                if (getStat().getMp() < 3) {
                    cancelEffectFromBuffStat(MapleBuffStat.拔刀术加成);
                }
            }
        } else if (JobConstants.is主教(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.祈祷);
            if (effect != null && this.getParty() != null) {
                effect.h(this, false);
            }
        } else if (JobConstants.is恶魔复仇者(getJob())) {
            MapleStatEffect effect = getStatForBuff(MapleBuffStat.恶魔狂怒);
            if (effect != null && getStat().getHp() - effect.getY() > effect.getY() && getStat().getHp() - effect.getY() > getStat().getCurrentMaxHp() * effect.getQ2() / 100) {
                addHPMP(-effect.getY(), 0);
                send(MaplePacketCreator.userBonusAttackRequest(恶魔复仇者.恶魔狂怒_2, 0, Collections.emptyList()));
            }
        }
    }

    public int getBuffValue() {
        return buffValue;
    }

    public void setBuffValue(int buffValue) {
        this.buffValue = buffValue;
    }

    public long getLastComboTime() {
        return lastComboTime;
    }

    public void setLastComboTime(long lastComboTime) {
        this.lastComboTime = lastComboTime;
    }

    public HiredFisher getHiredFisher() {
        return hiredFisher;
    }

    public void setHiredFisher(HiredFisher hiredFisher) {
        this.hiredFisher = hiredFisher;
    }

    public enum FameStatus {

        OK,
        NOT_TODAY,
        NOT_THIS_MONTH
    }

    public class PlayerObservable extends Observable {

        final MapleCharacter player;

        public PlayerObservable(MapleCharacter player) {
            this.player = player;
        }

        public MapleCharacter getPlayer() {
            return player;
        }

        public void update() {
            setChanged();
            notifyObservers(player);
        }
    }
}
