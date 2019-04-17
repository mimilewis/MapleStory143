package server.quest;

import client.MapleCharacter;
import client.MapleQuestStatus;
import constants.JobConstants;
import handling.opcode.EffectOpcode;
import scripting.quest.QuestScriptManager;
import tools.Pair;
import tools.packet.EffectPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class MapleQuest implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private static final List<MapleQuest> BulbQuest = new LinkedList<>();
    private static final Map<Integer, MapleQuest> quests = new LinkedHashMap<>();
    protected final List<MapleQuestRequirement> startReqs = new LinkedList<>(); //开始任务需要的条件
    protected final List<MapleQuestRequirement> completeReqs = new LinkedList<>(); //完成任务需要的条件
    protected final List<MapleQuestAction> startActs = new LinkedList<>(); //开始任务的操作
    protected final List<MapleQuestAction> completeActs = new LinkedList<>(); //完成任务的操作
    protected final Map<String, List<Pair<String, Pair<String, Integer>>>> partyQuestInfo = new LinkedHashMap<>(); //组队任务 [rank, [more/less/equal, [property, value]]]
    protected final Map<Integer, Integer> relevantMobs = new LinkedHashMap<>(); //完成任务需要杀的怪物和数量
    protected final Map<Integer, Integer> questItems = new LinkedHashMap<>(); //完成任务需要的道具和数量
    protected int id; //任务ID
    protected String name = "";
    private boolean autoStart = false, autoPreComplete = false, repeatable = false, customend = false, blocked = false, autoAccept = false, autoComplete = false, scriptedStart = false, selfStart = false;
    private int viewMedalItem = 0, selectedSkillID = 0;

    public MapleQuest() {

    }

    protected MapleQuest(int id) {
        this.id = id;
    }

    private static MapleQuest loadQuest(ResultSet rs, PreparedStatement psr, PreparedStatement psa, PreparedStatement pss, PreparedStatement psq, PreparedStatement psi, PreparedStatement psp) throws SQLException {
        MapleQuest ret = new MapleQuest(rs.getInt("questid"));
        ret.name = rs.getString("name");
        ret.autoStart = rs.getInt("autoStart") > 0;
        ret.autoPreComplete = rs.getInt("autoPreComplete") > 0;
        ret.autoAccept = rs.getInt("autoAccept") > 0;
        ret.autoComplete = rs.getInt("autoComplete") > 0;
        ret.viewMedalItem = rs.getInt("viewMedalItem");
        ret.selectedSkillID = rs.getInt("selectedSkillID");
        ret.blocked = rs.getInt("blocked") > 0; //ult.explorer quests will dc as the item isn't there...

        ret.selfStart = rs.getInt("selfStart") > 0;

        psr.setInt(1, ret.id);
        ResultSet rse = psr.executeQuery();
        while (rse.next()) {
            MapleQuestRequirementType type = MapleQuestRequirementType.getByWZName(rse.getString("name"));
            MapleQuestRequirement req = new MapleQuestRequirement(ret, type, rse);
            if (type.equals(MapleQuestRequirementType.interval)) {
                ret.repeatable = true;
            } else if (type.equals(MapleQuestRequirementType.normalAutoStart)) {
                ret.repeatable = true;
                ret.autoStart = true;
            } else if (type.equals(MapleQuestRequirementType.startscript)) {
                ret.scriptedStart = true;
            } else if (type.equals(MapleQuestRequirementType.endscript)) {
                ret.customend = true;
            } else if (type.equals(MapleQuestRequirementType.mob)) {
                for (Pair<Integer, Integer> mob : req.getDataStore()) {
                    ret.relevantMobs.put(mob.left, mob.right);
                }
            } else if (type.equals(MapleQuestRequirementType.item)) {
                for (Pair<Integer, Integer> it : req.getDataStore()) {
                    ret.questItems.put(it.left, it.right);
                }
            }
            if (rse.getInt("type") == 0) {
                ret.startReqs.add(req);
            } else {
                ret.completeReqs.add(req);
            }
            if (ret.isSelfStart()) {
                BulbQuest.add(ret);
            }
        }
        rse.close();

        psa.setInt(1, ret.id);
        rse = psa.executeQuery();
        while (rse.next()) {
            MapleQuestActionType ty = MapleQuestActionType.getByWZName(rse.getString("name"));
            if (rse.getInt("type") == 0) { //pass it over so it will set ID + type once done
                if (ty == MapleQuestActionType.item && ret.id == 7103) { //帕普拉图斯任务
                    continue;
                }
                ret.startActs.add(new MapleQuestAction(ty, rse, ret, pss, psq, psi));
            } else {
                if (ty == MapleQuestActionType.item && ret.id == 7102) { //时间球任务
                    continue;
                }
                ret.completeActs.add(new MapleQuestAction(ty, rse, ret, pss, psq, psi));
            }
        }
        rse.close();

        psp.setInt(1, ret.id);
        rse = psp.executeQuery();
        while (rse.next()) {
            if (!ret.partyQuestInfo.containsKey(rse.getString("rank"))) {
                ret.partyQuestInfo.put(rse.getString("rank"), new ArrayList<>());
            }
            ret.partyQuestInfo.get(rse.getString("rank")).add(new Pair<>(rse.getString("mode"), new Pair<>(rse.getString("property"), rse.getInt("value"))));
        }
        rse.close();
        return ret;
    }

    public static void initQuests() {
//        try {
//            Connection con = DatabaseConnectionWZ.getConnection();
//            PreparedStatement ps = con.prepareStatement("SELECT * FROM wz_questdata");
//            PreparedStatement psr = con.prepareStatement("SELECT * FROM wz_questreqdata WHERE questid = ?");
//            PreparedStatement psa = con.prepareStatement("SELECT * FROM wz_questactdata WHERE questid = ?");
//            PreparedStatement pss = con.prepareStatement("SELECT * FROM wz_questactskilldata WHERE uniqueid = ?");
//            PreparedStatement psq = con.prepareStatement("SELECT * FROM wz_questactquestdata WHERE uniqueid = ?");
//            PreparedStatement psi = con.prepareStatement("SELECT * FROM wz_questactitemdata WHERE uniqueid = ?");
//            PreparedStatement psp = con.prepareStatement("SELECT * FROM wz_questpartydata WHERE questid = ?");
//            ResultSet rs = ps.executeQuery();
//            while (rs.next()) {
//                quests.put(rs.getInt("questid"), loadQuest(rs, psr, psa, pss, psq, psi, psp));
//            }
//            rs.close();
//            ps.close();
//            psr.close();
//            psa.close();
//            pss.close();
//            psq.close();
//            psi.close();
//            psp.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }


//        System.out.println("共加载 " + quests.size() + " 个任务信息.");
//        System.out.println("共加载 " + BulbQuest.size() + " 个灯泡任务,角色登录后会自动屏蔽的!");
    }

    public static MapleQuest getInstance(int id) {
        //by this time we have already initialized
        return quests.computeIfAbsent(id, MapleQuest::new);
    }

    public static Collection<MapleQuest> getAllInstances() {
        return quests.values();
    }

    /**
     * 灯泡任务列表
     *
     * @return
     */
    public static List<MapleQuest> GetBulbQuest() {
        return BulbQuest;
    }

    public List<Pair<String, Pair<String, Integer>>> getInfoByRank(String rank) {
        return partyQuestInfo.get(rank);
    }

    public boolean isPartyQuest() {
        return partyQuestInfo.size() > 0;
    }

    public int getSkillID() {
        return selectedSkillID;
    }

    public String getName() {
        return name;
    }

    public List<MapleQuestAction> getCompleteActs() {
        return completeActs;
    }

    public boolean canStart(MapleCharacter chr, Integer npcid) {
        if (chr.getQuest(this).getStatus() != 0 && !(chr.getQuest(this).getStatus() == 2 && repeatable)) {
            if (chr.isShowPacket()) {
                chr.dropMessage(6, "开始任务 canStart: " + (chr.getQuest(this).getStatus() != 0) + " - " + !(chr.getQuest(this).getStatus() == 2 && repeatable) + " repeatable: " + repeatable);
            }
            forceComplete(chr, npcid);
            return false;
        }
        if (blocked && !chr.isGM()) {
            if (chr.isShowPacket()) {
                chr.dropMessage(6, "开始任务 canStart - blocked " + blocked);
            }
            return false;
        }
        //if (autoAccept) {
        //    return true; //need script
        //}
        for (MapleQuestRequirement r : startReqs) {
            if (r.getType() == MapleQuestRequirementType.dayByDay && npcid != null) { //everyday. we don't want ok
                forceComplete(chr, npcid);
                return false;
            }
            if (!r.check(chr, npcid)) {
                if (chr.isShowPacket()) {
                    chr.dropMessage(6, "开始任务 canStart - check " + !r.check(chr, npcid));
                }
                return false;
            }
        }
        return true;
    }

    public boolean canComplete(MapleCharacter chr, Integer npcid) {
        if (chr.getQuest(this).getStatus() != 1) {
            return false;
        }
        if (blocked && !chr.isGM()) {
            return false;
        }
        if (autoComplete && npcid != null && viewMedalItem <= 0) {
            forceComplete(chr, npcid);
            return false; //skip script
        }
        for (MapleQuestRequirement r : completeReqs) {
            if (!r.check(chr, npcid)) {
                return false;
            }
        }
        return true;
    }

    public void RestoreLostItem(MapleCharacter chr, int itemid) {
        if (blocked && !chr.isGM()) {
            return;
        }
        for (MapleQuestAction a : startActs) {
            if (a.RestoreLostItem(chr, itemid)) {
                break;
            }
        }
    }

    /*
     * 开始任务
     */
    public void start(MapleCharacter chr, int npc) {
        if (chr.isShowPacket()) {
            chr.dropMessage(6, "开始任务 start: " + npc + " autoStart：" + autoStart + " checkNPCOnMap: " + checkNPCOnMap(chr, npc) + " canStart: " + canStart(chr, npc));
        }
        if ((autoStart || checkNPCOnMap(chr, npc)) && canStart(chr, npc)) {
            for (MapleQuestAction a : startActs) {
                if (!a.checkEnd(chr, null)) { //just in case
                    if (chr.isShowPacket()) {
                        chr.dropMessage(6, "开始任务 checkEnd 错误...");
                    }
                    return;
                }
            }
            for (MapleQuestAction a : startActs) {
                a.runStart(chr, null);
            }
            if (!customend) {
                forceStart(chr, npc, null);
            } else {
                QuestScriptManager.getInstance().endQuest(chr.getClient(), npc, getId(), true);
            }
        } else {
            forceComplete(chr, npc);
        }
    }

    public void complete(MapleCharacter chr, int npc) {
        complete(chr, npc, null);
    }

    public void complete(MapleCharacter chr, int npc, Integer selection) {
        if (chr.getMap() != null && (autoPreComplete || checkNPCOnMap(chr, npc)) && canComplete(chr, npc)) {
            for (MapleQuestAction a : completeActs) {
                if (!a.checkEnd(chr, selection)) {
                    return;
                }
            }
            forceComplete(chr, npc);
            for (MapleQuestAction a : completeActs) {
                a.runEnd(chr, selection);
            }
            chr.send(EffectPacket.showSpecialEffect(EffectOpcode.UserEffect_QuestComplete.getValue())); // 任务完成
            chr.getMap().broadcastMessage(chr, EffectPacket.showForeignEffect(chr.getId(), EffectOpcode.UserEffect_QuestComplete.getValue()), false);
        }
    }

    public void forfeit(MapleCharacter chr) {
        if (chr.getQuest(this).getStatus() != (byte) 1) {
            return;
        }
        MapleQuestStatus oldStatus = chr.getQuest(this);
        MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 0);
        newStatus.setForfeited(oldStatus.getForfeited() + 1);
        newStatus.setCompletionTime(oldStatus.getCompletionTime());
        chr.updateQuest(newStatus);
    }

    public void forceStart(MapleCharacter chr, int npc, String customData) {
        if (chr.isShowPacket()) {
            chr.dropSpouseMessage(0x14, "[Start] 开始任务 任务ID： " + getId() + " 任务Npc: " + npc);
        }
        MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 1, npc);
        newStatus.setForfeited(chr.getQuest(this).getForfeited());
        newStatus.setCompletionTime(chr.getQuest(this).getCompletionTime());
        newStatus.setCustomData(customData);
        chr.updateQuest(newStatus);
    }

    public void forceComplete(MapleCharacter chr, int npc) {
        if (chr.isShowPacket()) {
            chr.dropSpouseMessage(0x14, "[Complete] 完成任务 任务ID： " + getId() + " 任务Npc: " + npc);
        }
        MapleQuestStatus newStatus = new MapleQuestStatus(this, (byte) 2, npc);
        newStatus.setForfeited(chr.getQuest(this).getForfeited());
        chr.updateQuest(newStatus);
    }

    public int getId() {
        return id;
    }

    public Map<Integer, Integer> getRelevantMobs() {
        return relevantMobs;
    }

    private boolean checkNPCOnMap(MapleCharacter player, int npcId) {
        return (JobConstants.is龙神(player.getJob()) && npcId == 1013000) //米乐
                || (JobConstants.is恶魔猎手(player.getJob()) && npcId == 0)
                || (JobConstants.is双弩精灵(player.getJob()) && npcId == 0)
                || npcId == 2151009 //马斯特玛
                || npcId == 3000018 //爱丝卡达
                || npcId == 9010000 //冒险岛运营员
                || (npcId >= 2161000 && npcId <= 2161011) //狮子城任务
                || npcId == 9000040 //勋章老人
                || npcId == 9000066 //勋章老人
                || npcId == 2010010 //黛雅
                || npcId == 1032204 //奇怪的修行者
                || npcId == 0 //玩家头上的任务
                || npcId == 2182001 //玩家头上的任务
                || npcId == 9000279
                || npcId == 9000371
                || (player.getMap() != null && player.getMap().containsNPC(npcId));
    }

    public int getMedalItem() {
        return viewMedalItem;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public int getAmountofItems(int itemId) {
        return questItems.get(itemId) != null ? questItems.get(itemId) : 0;
    }

    public boolean hasStartScript() {
        return scriptedStart;
    }

    public boolean hasEndScript() {
        return customend;
    }

    /**
     * 判断是否为灯泡任务
     *
     * @return
     */
    public boolean isSelfStart() {
        return selfStart;
    }

}
