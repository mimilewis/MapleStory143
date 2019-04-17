package server.quest;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleStat;
import client.MapleTraitType;
import client.inventory.InventoryException;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillEntry;
import client.skills.SkillFactory;
import constants.GameConstants;
import constants.ItemConstants;
import constants.JobConstants;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.RandomRewards;
import tools.*;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapleQuestAction implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final MapleQuestActionType type;
    private final MapleQuest quest;
    private final List<Integer> applicableJobs = new ArrayList<>();
    private int intStore = 0;
    private List<QuestItem> items = null;
    private List<Triple<Integer, Integer, Integer>> skill = null;
    private List<Pair<Integer, Integer>> state = null;

    /**
     * Creates a new instance of MapleQuestAction
     */
    public MapleQuestAction(MapleQuestActionType type, ResultSet rse, MapleQuest quest, PreparedStatement pss, PreparedStatement psq, PreparedStatement psi) throws SQLException {
        this.type = type;
        this.quest = quest;

        this.intStore = rse.getInt("intStore");
        String[] jobs = rse.getString("applicableJobs").split(", ");
        if (jobs.length <= 0 && rse.getString("applicableJobs").length() > 0) {
            applicableJobs.add(Integer.parseInt(rse.getString("applicableJobs")));
        }
        for (String j : jobs) {
            if (j.length() > 0) {
                applicableJobs.add(Integer.parseInt(j));
            }
        }
        ResultSet rs;
        switch (type) {
            case item:
                items = new ArrayList<>();
                psi.setInt(1, rse.getInt("uniqueid"));
                rs = psi.executeQuery();
                while (rs.next()) {
                    items.add(new QuestItem(rs.getInt("itemid"), rs.getInt("count"), rs.getInt("period"), rs.getInt("gender"), rs.getInt("job"), rs.getInt("jobEx"), rs.getInt("prop")));
                }
                rs.close();
                break;
            case quest:
                state = new ArrayList<>();
                psq.setInt(1, rse.getInt("uniqueid"));
                rs = psq.executeQuery();
                while (rs.next()) {
                    state.add(new Pair<>(rs.getInt("quest"), rs.getInt("state")));
                }
                rs.close();
                break;
            case skill:
                skill = new ArrayList<>();
                pss.setInt(1, rse.getInt("uniqueid"));
                rs = pss.executeQuery();
                while (rs.next()) {
                    skill.add(new Triple<>(rs.getInt("skillid"), rs.getInt("skillLevel"), rs.getInt("masterLevel")));
                }
                rs.close();
                break;
        }
    }

    private static boolean canGetItem(QuestItem item, MapleCharacter chr) {
        if (item.gender != 2 && item.gender >= 0 && item.gender != chr.getGender()) {
            return false;
        }
        if (item.job > 0) {
            List<Integer> code = getJobBy5ByteEncoding(item.job);
            boolean jobFound = false;
            for (int codec : code) {
                if (codec / 100 == chr.getJob() / 100) {
                    jobFound = true;
                    break;
                }
            }
            if (!jobFound && item.jobEx > 0) {
                List<Integer> codeEx = getJobBySimpleEncoding(item.jobEx);
                for (int codec : codeEx) {
                    if ((codec / 100 % 10) == (chr.getJob() / 100 % 10)) {
                        jobFound = true;
                        break;
                    }
                }
            }
            return jobFound;
        }
        return true;
    }

    private static List<Integer> getJobBy5ByteEncoding(int encoded) {
        List<Integer> ret = new ArrayList<>();
        if ((encoded & 0x1) != 0) {
            ret.add(0);
        }
        if ((encoded & 0x2) != 0) {
            ret.add(100);
        }
        if ((encoded & 0x4) != 0) {
            ret.add(200);
        }
        if ((encoded & 0x8) != 0) {
            ret.add(300);
        }
        if ((encoded & 0x10) != 0) {
            ret.add(400);
        }
        if ((encoded & 0x20) != 0) {
            ret.add(500);
        }
        if ((encoded & 0x400) != 0) {
            ret.add(1000);
        }
        if ((encoded & 0x800) != 0) {
            ret.add(1100);
        }
        if ((encoded & 0x1000) != 0) {
            ret.add(1200);
        }
        if ((encoded & 0x2000) != 0) {
            ret.add(1300);
        }
        if ((encoded & 0x4000) != 0) {
            ret.add(1400);
        }
        if ((encoded & 0x8000) != 0) {
            ret.add(1500);
        }
        if ((encoded & 0x20000) != 0) {
            ret.add(2001); //im not sure of this one
            ret.add(2200);
        }
        if ((encoded & 0x100000) != 0) {
            ret.add(2000);
            ret.add(2001); //?
        }
        if ((encoded & 0x200000) != 0) {
            ret.add(2100);
        }
        if ((encoded & 0x400000) != 0) {
            ret.add(2001); //?
            ret.add(2200);
        }
        if ((encoded & 0x40000000) != 0) { //i haven't seen any higher than this o.o
            ret.add(3000);
            ret.add(3200);
            ret.add(3300);
            ret.add(3500);
        }
        return ret;
    }

    private static List<Integer> getJobBySimpleEncoding(int encoded) {
        List<Integer> ret = new ArrayList<>();
        if ((encoded & 0x1) != 0) {
            ret.add(200);
        }
        if ((encoded & 0x2) != 0) {
            ret.add(300);
        }
        if ((encoded & 0x4) != 0) {
            ret.add(400);
        }
        if ((encoded & 0x8) != 0) {
            ret.add(500);
        }
        return ret;
    }

    public boolean RestoreLostItem(MapleCharacter chr, int itemid) {
        if (type == MapleQuestActionType.item) {
            for (QuestItem item : items) {
                if (item.itemid == itemid) {
                    if (!chr.haveItem(item.itemid, item.count, true, false)) {
                        MapleInventoryManipulator.addById(chr.getClient(), item.itemid, (short) item.count, "Obtained from quest (Restored) " + quest.getId() + " on " + DateUtil.getCurrentDate());
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * 开始任务
     */
    public void runStart(MapleCharacter chr, Integer extSelection) {
        MapleQuestStatus status;
        switch (type) {
            case exp:
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                chr.gainExp(intStore * GameConstants.getExpRate_Quest(chr.getLevel()) * (chr.getStat().questBonus) * ((chr.getTrait(MapleTraitType.sense).getLevel() * 3 / 10) + 100) / 100, true, true, true);
                break;
            case item:
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<>();
                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, chr)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                for (QuestItem item : items) {
                    if (!canGetItem(item, chr)) {
                        continue;
                    }
                    int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    short count = (short) item.count;
                    if (count < 0) { // 删除任务道具
                        try {
                            MapleInventoryManipulator.removeById(chr.getClient(), ItemConstants.getInventoryType(id), id, (count * -1), true, false);
                        } catch (InventoryException ie) {
                            // it's better to catch this here so we'll atleast try to remove the other items
                            System.err.println("[h4x] Completing a quest without meeting the requirements" + ie);
                        }
                        chr.send(MaplePacketCreator.getShowItemGain(id, count, true));
                    } else { // 给任务奖励道具
                        int period = item.period / 1440; //转换成天
                        String name = MapleItemInformationProvider.getInstance().getName(id);
                        if (id / 10000 == 114 && name != null && name.length() > 0) { //如果是勋章道具
                            String msg = "恭喜您获得勋章 <" + name + ">";
                            chr.dropMessage(-1, msg);
                            chr.dropMessage(5, msg);
                        }
                        MapleInventoryManipulator.addById(chr.getClient(), id, count, "", null, period, "任务获得 " + quest.getId() + " 时间: " + DateUtil.getCurrentDate());
                        chr.send(MaplePacketCreator.getShowItemGain(id, count, true));
                    }
                }
                break;
            case nextQuest:
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                chr.send(MaplePacketCreator.updateQuestInfo(quest.getId(), status.getNpc(), intStore, false));
                break;
            case money:
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                chr.gainMeso(intStore, true, true);
                break;
            case quest:
                for (Pair<Integer, Integer> q : state) {
                    chr.updateQuest(new MapleQuestStatus(MapleQuest.getInstance(q.left), q.right));
                }
                break;
            case skill:
                Map<Integer, SkillEntry> list = new HashMap<>();
                for (Triple<Integer, Integer, Integer> skills : skill) {
                    int skillid = skills.left;
                    int skillLevel = skills.mid;
                    int masterLevel = skills.right;
                    Skill skillObject = SkillFactory.getSkill(skillid);
                    boolean found = false;
                    for (int applicableJob : applicableJobs) {
                        if (chr.getJob() == applicableJob) {
                            found = true;
                            break;
                        }
                    }
                    if (skillObject.isBeginnerSkill() || found) {
                        list.put(skillid, new SkillEntry((byte) Math.max(skillLevel, chr.getSkillLevel(skillObject)), (byte) Math.max(masterLevel, chr.getMasterLevel(skillObject)), SkillFactory.getDefaultSExpiry(skillObject)));
                    }
                }
                chr.changeSkillsLevel(list);
                break;
            case pop:
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                int fameGain = intStore;
                chr.addFame(fameGain);
                chr.updateSingleStat(MapleStat.人气, chr.getFame());
                chr.send(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            case buffItemID:
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                int tobuff = intStore;
                if (tobuff <= 0) {
                    break;
                }
                MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(chr);
                break;
            case infoNumber: {
//		System.out.println("quest : "+intStore+"");
//		MapleQuest.getInstance(intStore).forceComplete(c, 0);
                break;
            }
            case sp: {
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                int sp_val = intStore;
                if (applicableJobs.size() > 0) {
                    int finalJob = 0;
                    for (int job_val : applicableJobs) {
                        if (chr.getJob() >= job_val && job_val > finalJob) {
                            finalJob = job_val;
                        }
                    }
                    if (finalJob == 0) {
                        chr.gainSP(sp_val);
                    } else {
                        chr.gainSP(sp_val, JobConstants.getSkillBookByJob(finalJob));
                    }
                } else {
                    chr.gainSP(sp_val);
                }
                break;
            }
            case charmEXP:
            case charismaEXP:
            case craftEXP:
            case insightEXP:
            case senseEXP:
            case willEXP: {
                status = chr.getQuest(quest);
                if (status.getForfeited() > 0) {
                    break;
                }
                chr.getTrait(MapleTraitType.getByQuestName(type.name())).addExp(intStore, chr);
                break;
            }
            default:
                break;
        }
    }

    /*
     * 检查任务是否可以完成
     */
    public boolean checkEnd(MapleCharacter chr, Integer extSelection) {
        switch (type) {
            case item: {
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<>();

                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, chr)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;

                for (QuestItem item : items) {
                    if (!canGetItem(item, chr)) {
                        continue;
                    }
                    int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    short count = (short) item.count;
                    if (count < 0) { // 删除任务道具检测
                        if (!chr.haveItem(id, count, false, true)) {
                            chr.dropMessage(1, "您的任务道具不够，还不能完成任务.");
                            return false;
                        }
                    } else { // 给角色任务奖励检测
                        if (MapleItemInformationProvider.getInstance().isPickupRestricted(id) && chr.haveItem(id, 1, true, false)) {
                            chr.dropMessage(1, "You have this item already: " + MapleItemInformationProvider.getInstance().getName(id));
                            return false;
                        }
                        switch (ItemConstants.getInventoryType(id)) {
                            case EQUIP:
                                eq++;
                                break;
                            case USE:
                                use++;
                                break;
                            case SETUP:
                                setup++;
                                break;
                            case ETC:
                                etc++;
                                break;
                            case CASH:
                                cash++;
                                break;
                        }
                    }
                }
                if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq) {
                    chr.dropMessage(1, "装备栏空间不足.");
                    return false;
                } else if (chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use) {
                    chr.dropMessage(1, "消耗栏空间不足.");
                    return false;
                } else if (chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup) {
                    chr.dropMessage(1, "设置栏空间不足.");
                    return false;
                } else if (chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc) {
                    chr.dropMessage(1, "其他栏空间不足.");
                    return false;
                } else if (chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
                    chr.dropMessage(1, "特殊栏空间不足.");
                    return false;
                }
                return true;
            }
            case money: {
                int meso = intStore;
                if (chr.getMeso() + meso < 0) { // Giving, overflow
                    chr.dropMessage(1, "携带金币数量已达限制.");
                    return false;
                } else if (meso < 0 && chr.getMeso() < Math.abs(meso)) { //remove meso
                    chr.dropMessage(1, "金币不足.");
                    return false;
                }
                return true;
            }
        }
        return true;
    }

    /*
     * 结束任务
     */
    public void runEnd(MapleCharacter chr, Integer extSelection) {
        switch (type) {
            case exp: {
                chr.gainExp(intStore * GameConstants.getExpRate_Quest(chr.getLevel()) * (chr.getStat().questBonus) * ((chr.getTrait(MapleTraitType.sense).getLevel() * 3 / 10) + 100) / 100, true, true, true);
                break;
            }
            case item: {
                // first check for randomness in item selection
                Map<Integer, Integer> props = new HashMap<>();
                for (QuestItem item : items) {
                    if (item.prop > 0 && canGetItem(item, chr)) {
                        for (int i = 0; i < item.prop; i++) {
                            props.put(props.size(), item.itemid);
                        }
                    }
                }
                int selection = 0;
                int extNum = 0;
                if (props.size() > 0) {
                    selection = props.get(Randomizer.nextInt(props.size()));
                }
                for (QuestItem item : items) {
                    if (!canGetItem(item, chr)) {
                        continue;
                    }
                    int id = item.itemid;
                    if (item.prop != -2) {
                        if (item.prop == -1) {
                            if (extSelection != null && extSelection != extNum++) {
                                continue;
                            }
                        } else if (id != selection) {
                            continue;
                        }
                    }
                    short count = (short) item.count;
                    if (count < 0) { // 删除任务道具
                        MapleInventoryManipulator.removeById(chr.getClient(), ItemConstants.getInventoryType(id), id, (count * -1), true, false);
                        chr.send(MaplePacketCreator.getShowItemGain(id, count, true));
                    } else { // 给任务奖励道具
                        int period = item.period / 1440; //转换成天数
                        String name = MapleItemInformationProvider.getInstance().getName(id);
                        if (id / 10000 == 114 && name != null && name.length() > 0) { //如果是勋章道具奖励
                            String msg = "你获得了勋章 <" + name + ">";
                            chr.dropMessage(-1, msg);
                            chr.dropMessage(5, msg);
                        }
                        MapleInventoryManipulator.addById(chr.getClient(), id, count, "", null, period, "任务获得 " + quest.getId() + " 时间: " + DateUtil.getCurrentDate());
                        chr.send(MaplePacketCreator.getShowItemGain(id, count, true));
                    }
                }
                break;
            }
            case nextQuest: {
                chr.send(MaplePacketCreator.updateQuestInfo(quest.getId(), chr.getQuest(quest).getNpc(), intStore, false));
                break;
            }
            case money: {
                chr.gainMeso(intStore, true, true);
                break;
            }
            case quest: {
                for (Pair<Integer, Integer> q : state) {
                    chr.updateQuest(new MapleQuestStatus(MapleQuest.getInstance(q.left), q.right));
                }
                break;
            }
            case skill:
                Map<Integer, SkillEntry> list = new HashMap<>();
                for (Triple<Integer, Integer, Integer> skills : skill) {
                    int skillid = skills.left;
                    int skillLevel = skills.mid;
                    int masterLevel = skills.right;
                    Skill skillObject = SkillFactory.getSkill(skillid);
                    boolean found = false;
                    for (int applicableJob : applicableJobs) {
                        if (chr.getJob() == applicableJob) {
                            found = true;
                            break;
                        }
                    }
                    if (skillObject.isBeginnerSkill() || found) {
                        list.put(skillid, new SkillEntry((byte) Math.max(skillLevel, chr.getSkillLevel(skillObject)), (byte) Math.max(masterLevel, chr.getMasterLevel(skillObject)), SkillFactory.getDefaultSExpiry(skillObject)));
                    }
                }
                chr.changeSkillsLevel(list);
                break;
            case pop: {
                int fameGain = intStore;
                chr.addFame(fameGain);
                chr.updateSingleStat(MapleStat.人气, chr.getFame());
                chr.send(MaplePacketCreator.getShowFameGain(fameGain));
                break;
            }
            case buffItemID: {
                int tobuff = intStore;
                if (tobuff <= 0) {
                    break;
                }
                MapleItemInformationProvider.getInstance().getItemEffect(tobuff).applyTo(chr);
                break;
            }
            case infoNumber: {
//		System.out.println("quest : "+intStore+"");
//		MapleQuest.getInstance(intStore).forceComplete(c, 0);
                break;
            }
            case sp: {
                int sp_val = intStore;
                if (applicableJobs.size() > 0) {
                    int finalJob = 0;
                    for (int job_val : applicableJobs) {
                        if (chr.getJob() >= job_val && job_val > finalJob) {
                            finalJob = job_val;
                        }
                    }
                    if (finalJob == 0) {
                        chr.gainSP(sp_val);
                    } else {
                        chr.gainSP(sp_val, JobConstants.getSkillBookByJob(finalJob));
                    }
                } else {
                    chr.gainSP(sp_val);
                }
                break;
            }
            case charmEXP:
            case charismaEXP:
            case craftEXP:
            case insightEXP:
            case senseEXP:
            case willEXP: {
                chr.getTrait(MapleTraitType.getByQuestName(type.name())).addExp(intStore, chr);
                break;
            }
            default:
                break;
        }
    }

    public MapleQuestActionType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public List<Triple<Integer, Integer, Integer>> getSkills() {
        return skill;
    }

    public List<QuestItem> getItems() {
        return items;
    }

    public static class QuestItem {

        public final int itemid;
        public final int count;
        public final int period;
        public final int gender;
        public final int job;
        public final int jobEx;
        public final int prop;

        public QuestItem(int itemid, int count, int period, int gender, int job, int jobEx, int prop) {
            if (RandomRewards.getTenPercent().contains(itemid)) {
                count += Randomizer.nextInt(3);
            }
            this.itemid = itemid;
            this.count = count;
            this.period = period;
            this.gender = gender;
            this.job = job;
            this.jobEx = jobEx;
            this.prop = prop;
        }
    }
}
