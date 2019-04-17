package handling.world;

import client.*;
import client.anticheat.CheatTracker;
import client.anticheat.ReportType;
import client.inventory.*;
import client.skills.InnerSkillEntry;
import client.skills.SkillEntry;
import client.skills.SkillMacro;
import server.MapleStorage;
import server.cashshop.CashShop;
import server.shop.MapleShopItem;
import tools.Pair;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Timestamp;
import java.util.*;
import java.util.Map.Entry;

/**
 * 角色在换频道时的数据交换
 */

public class CharacterTransfer implements Externalizable {

    public final Map<MapleTraitType, Integer> traits = new EnumMap<>(MapleTraitType.class);
    public final List<Battler> boxed;
    public final List<CharacterNameAndId> buddies = new LinkedList<>();
    public final Map<Integer, MapleQuestStatus> Quest = new LinkedHashMap<>();
    public final Map<Integer, SkillEntry> skills = new LinkedHashMap<>();
    public final Map<String, Integer> credit;
    public final Map<Integer, Integer> mbook;
    public final Map<Byte, Integer> reports = new LinkedHashMap<>();
    public final Map<Integer, Pair<Byte, Integer>> keymap;
    public final List<Pair<Integer, Integer>> quickslot;
    public final Map<Integer, String> InfoQuest;
    public final Map<String, String> KeyValue;
    public final Map<Integer, VCoreSkillEntry> vcoreskills = new LinkedHashMap<>();
    public Map<Integer, Pair<Integer, SkillEntry>> sonOfLinedSkills = new LinkedHashMap<>();
    public int characterid, accountid, fame, pvpExp, pvpPoints,
            hair, face, mapid, guildid,
            partyid, messengerid, /*ACash, MaplePoints,*/
            mount_itemid, mount_exp, points, vpoints, marriageId, maxhp, maxmp, hp, mp, friendshiptoadd,
            familyid, seniorid, junior1, junior2, currentrep, totalrep, gachexp, guildContribution, totalWins, totalLosses, todayonlinetime, totalonlinetime, weaponPoint;
    public byte channel, accountGender, gender, gmLevel, guildrank, alliancerank, hairBaseColor, hairProbColor, hairMixedColor,
            fairyExp, buddysize, world, initialSpawnPoint, skinColor, mount_level, mount_Fatigue, subcategory;
    public long lastfametime, TranferTime, exp, meso;
    public String name, accountname, BlessOfFairy, BlessOfEmpress, chalkboard, tempIP;
    public short level, str, dex, int_, luk, remainingAp, hpApUsed, job, fatigue, soulcount;
    public MapleInventory[] inventorys;
    public SkillMacro[] skillmacro = new SkillMacro[5];
    public MapleStorage storage;
    public CashShop cs;
    public Battler[] battlers = new Battler[6];
    public CheatTracker anticheat;
    public MapleLieDetector antiMacro;
    public InnerSkillEntry[] innerSkills;
    public int[] savedlocation, wishlist, rocks, remainingSp, regrocks, hyperrocks, friendshippoints;
    public byte[] petStore;
    public MapleImp[] imps;
    public List<Integer> finishedAchievements = null, famedcharacters = null, battledaccs = null;
    public List<MapleShopItem> rebuy = null;
    public int decorate;
    public int beans;
    public int warning;
    public int vip;
    public Timestamp viptime;
    public int reborns, reborns1, reborns2, reborns3, apstorage;
    public int honor;
    public int love;
    public long lastLoveTime;
    public Map<Integer, Long> loveCharacters = null;
    public int playerPoints;
    public int playerEnergy;
    public MaplePvpStats pvpStats;
    public int pvpDeaths;
    public int pvpKills;
    public int pvpVictory;
    public int darkLight;
    public MaplePotionPot potionPot;
    public MapleCoreAura coreAura;
    public PlayerSpecialStats SpecialStats;
    public MapleSigninStatus signinStatus;
    public List<Integer> effectSwitch;
    public Map<Integer, MonsterFamiliar> familiars;
    public Map<Byte, List<Integer>> extendedSlots = null;
    public long ltime;

    public CharacterTransfer() {
        boxed = new ArrayList<>();
        finishedAchievements = new ArrayList<>();
        famedcharacters = new ArrayList<>();
        battledaccs = new ArrayList<>();
        extendedSlots = new HashMap<>();
        loveCharacters = new LinkedHashMap<>();
        rebuy = new ArrayList<>();
        KeyValue = new LinkedHashMap<>();
        InfoQuest = new LinkedHashMap<>();
        keymap = new LinkedHashMap<>();
        quickslot = new ArrayList<>();
        mbook = new LinkedHashMap<>();
        credit = new LinkedHashMap<>();
    }

    public CharacterTransfer(MapleCharacter chr) {
        this.characterid = chr.getId();
        this.accountid = chr.getAccountID();
        this.accountname = chr.getClient().getAccountName();
        this.channel = (byte) chr.getClient().getChannel();


        this.vpoints = chr.getVPoints();
        this.name = chr.getName();
        this.fame = chr.getFame();
        this.love = chr.getLove();
        this.accountGender = chr.getAccountGender();
        this.gender = chr.getGender();
        this.level = chr.getLevel();
        this.str = chr.getStat().getStr();
        this.dex = chr.getStat().getDex();
        this.int_ = chr.getStat().getInt();
        this.luk = chr.getStat().getLuk();
        this.hp = chr.getStat().getHp();
        this.mp = chr.getStat().getMp();
        this.maxhp = chr.getStat().getMaxHp();
        this.maxmp = chr.getStat().getMaxMp();
        this.exp = chr.getExp();
        this.hpApUsed = chr.getHpApUsed();
        this.remainingAp = chr.getRemainingAp();
        this.remainingSp = chr.getRemainingSps();
        this.meso = chr.getMeso();
        this.pvpExp = chr.getTotalBattleExp();
        this.pvpPoints = chr.getBattlePoints();
        this.skinColor = chr.getSkinColor();
        this.job = chr.getJob();
        this.hair = chr.getHair();
        this.hairBaseColor = chr.getHairBaseColor();
        this.hairMixedColor = chr.getHairMixedColor();
        this.hairProbColor = chr.getHairProbColor();
        this.face = chr.getFace();
        this.mapid = chr.getMapId();
        this.initialSpawnPoint = chr.getInitialSpawnpoint();
        this.marriageId = chr.getMarriageId();
        this.world = chr.getWorld();
        this.guildid = chr.getGuildId();
        this.guildrank = chr.getGuildRank();
        this.guildContribution = chr.getGuildContribution();
        this.alliancerank = chr.getAllianceRank();
        this.gmLevel = (byte) chr.getGMLevel();
        this.points = chr.getPoints();
        this.fairyExp = chr.getFairyExp();
        this.petStore = chr.getPetStores();
        this.subcategory = chr.getSubcategory();
        this.imps = chr.getImps();
        this.fatigue = chr.getFatigue();
        this.currentrep = chr.getCurrentRep();
        this.totalrep = chr.getTotalRep();
        this.familyid = chr.getFamilyId();
        this.totalWins = chr.getTotalWins();
        this.totalLosses = chr.getTotalLosses();
        this.seniorid = chr.getSeniorId();
        this.junior1 = chr.getJunior1();
        this.junior2 = chr.getJunior2();
        this.gachexp = chr.getGachExp();
        this.boxed = chr.getBoxed();
        this.anticheat = chr.getCheatTracker();
        this.antiMacro = chr.getAntiMacro();
        this.tempIP = chr.getClient().getTempIP();
        this.rebuy = chr.getRebuy();
        this.decorate = chr.getDecorate();
        this.beans = chr.getBeans();
        this.warning = chr.getWarning();
        this.reborns = chr.getReborns();
        this.reborns1 = chr.getReborns1();
        this.reborns2 = chr.getReborns2();
        this.reborns3 = chr.getReborns3();
        this.apstorage = chr.getAPS();
        this.honor = chr.getHonor();
        this.vip = chr.getVip();
        this.viptime = chr.getViptime();
        this.playerPoints = chr.getPlayerPoints();
        this.playerEnergy = chr.getPlayerEnergy();
        this.pvpDeaths = chr.getPvpDeaths();
        this.pvpKills = chr.getPvpKills();
        this.pvpVictory = chr.getPvpVictory();
        this.darkLight = chr.getDarkLight();
        this.friendshiptoadd = chr.getFriendShipToAdd();
        this.friendshippoints = chr.getFriendShipPoints();
        this.soulcount = chr.getSoulCount();

//        boolean uneq = false;
        for (int i = 0; i < this.petStore.length; i++) {
            MaplePet pet = chr.getSpawnPet(i);
            if (this.petStore[i] == 0) {
                this.petStore[i] = (byte) -1;
            }
            if (pet != null) {
//                uneq = true;
                this.petStore[i] = (byte) Math.max(this.petStore[i], pet.getInventoryPosition());
            }
        }
//        if (uneq) {
//            chr.unequipAllSpawnPets();
//        }
        for (MapleTraitType t : MapleTraitType.values()) {
            this.traits.put(t, chr.getTrait(t).getTotalExp());
        }
        for (BuddylistEntry qs : chr.getBuddylist().getBuddies()) {
            this.buddies.add(new CharacterNameAndId(qs.getCharacterId(), qs.getName(), qs.getGroup(), qs.isVisible()));
        }
        for (Entry<ReportType, Integer> ss : chr.getReports().entrySet()) {
            this.reports.put(ss.getKey().i, ss.getValue());
        }
        this.buddysize = chr.getBuddyCapacity();

        this.partyid = chr.getParty() == null ? -1 : chr.getParty().getPartyId();

        if (chr.getMessenger() != null) {
            this.messengerid = chr.getMessenger().getId();
        } else {
            this.messengerid = 0;
        }
        this.finishedAchievements = chr.getFinishedAchievements();
        this.KeyValue = chr.getKeyValue_Map();
        this.InfoQuest = chr.getInfoQuest_Map();
        for (Entry<Integer, MapleQuestStatus> qs : chr.getQuest_Map().entrySet()) {
            this.Quest.put(qs.getKey(), qs.getValue());
        }
        this.mbook = chr.getMonsterBook().getCards();
        this.inventorys = chr.getInventorys();
        chr.getSkills().forEach(skills::put);
        chr.getSonOfLinkedSkills().forEach(sonOfLinedSkills::put);
        chr.getVCoreSkill().forEach(vcoreskills::put);
        this.SpecialStats = chr.getSpecialStat();
        this.BlessOfFairy = chr.getBlessOfFairyOrigin();
        this.BlessOfEmpress = chr.getBlessOfEmpressOrigin();
        this.chalkboard = chr.getChalkboard();
        this.skillmacro = chr.getMacros();
        this.innerSkills = chr.getInnerSkills();
        this.keymap = chr.getKeyLayout().Layout();
        this.quickslot = chr.getQuickSlot().Layout();
        this.savedlocation = chr.getSavedLocations();
        this.wishlist = chr.getWishlist();
        this.rocks = chr.getRocks();
        this.regrocks = chr.getRegRocks();
        this.hyperrocks = chr.getHyperRocks();
        this.famedcharacters = chr.getFamedCharacters();
        this.battledaccs = chr.getBattledCharacters();
        this.lastfametime = chr.getLastFameTime();
        this.storage = chr.getStorage();
        this.pvpStats = chr.getPvpStats();
        this.potionPot = chr.getPotionPot();
        this.coreAura = chr.getCoreAura();
        this.cs = chr.getCashInventory();
        this.extendedSlots = chr.getAllExtendedSlots();
        MapleMount mount = chr.getMount();
        this.mount_itemid = mount.getItemId();
        this.mount_Fatigue = mount.getFatigue();
        this.mount_level = mount.getLevel();
        this.mount_exp = mount.getExp();
        this.battlers = chr.getBattlers();
        this.lastLoveTime = chr.getLastLoveTime();
        this.loveCharacters = chr.getLoveCharacters();
        TranferTime = System.currentTimeMillis();
        this.todayonlinetime = chr.getTodayOnlineTime();
        this.totalonlinetime = chr.getTotalOnlineTime();
        this.weaponPoint = chr.getWeaponPoint();
        this.credit = chr.getCredits();
        this.signinStatus = chr.getSigninStatus();
        this.effectSwitch = chr.getEffectSwitch();
        this.familiars = chr.getFamiliars();
        this.ltime = chr.getLTime();
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        TranferTime = System.currentTimeMillis();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
    }
}