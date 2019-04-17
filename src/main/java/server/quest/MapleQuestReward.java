package server.quest;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.ItemConstants;
import tools.Pair;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MapleQuestReward implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final MapleQuestRewardType type;
    private final List<Pair<Integer, Integer>> dataStore = new ArrayList<>();
    private int intStore;

    /**
     * 任务的必要条件
     *
     * @param type
     * @param rse
     * @throws SQLException
     */
    public MapleQuestReward(MapleQuestRewardType type, ResultSet rse) throws SQLException {
        this.type = type;
        if (type == MapleQuestRewardType.item) {
            this.dataStore.add(new Pair<>(rse.getInt("itemId"), rse.getInt("num")));
        } else {
            System.out.println("暂时不支持的奖励类型：" + type.toString());
        }
    }

    /**
     * @param chr
     * @return
     */
    public boolean getRewardToChr(MapleCharacter chr) {
        switch (type) {
            case item:
                for (Pair a : this.dataStore) {
                    int itemId = ((Integer) a.getLeft());
                    short quantity = 0;
                    MapleInventoryType iType = ItemConstants.getInventoryType(itemId);
                    for (Item item : chr.getInventory(iType).listById(itemId)) {
                        quantity = (short) (quantity + item.getQuantity());
                    }
                    int count = ((Integer) a.getRight());
                    if ((quantity < count) || ((count <= 0) && (quantity > 0))) {
                        return false;
                    }
                    chr.gainItem(itemId, count, "任务获得道具！");
                }
                return true;
        }
        return true;
    }

    public boolean check(MapleCharacter chr, Integer npcid) {
        switch (type) {
            case job:
                for (Pair a : this.dataStore) {
                    if ((((Integer) a.getRight()) == chr.getJob()) || (chr.isGM())) {
                        return true;
                    }
                }
                return false;
            case skill:
                for (Pair a : this.dataStore) {
                    boolean acquire = ((Integer) a.getRight()) > 0;
                    int skill = ((Integer) a.getLeft());
                    Skill skil = SkillFactory.getSkill(skill);
                    if (acquire) {
                        if (chr.getSkillLevel(skil) == 0) {
                            return false;
                        }
                    } else if ((chr.getSkillLevel(skil) > 0) || (chr.getMasterLevel(skil) > 0)) {
                        return false;
                    }
                }
                return true;
            case quest:
                for (Pair a : this.dataStore) {
                    MapleQuestStatus q = chr.getQuest(MapleQuest.getInstance((Integer) a.getLeft()));
                    int state = ((Integer) a.getRight());
                    if (state != 0) {
                        if ((q == null) && (state == 0)) {
                            continue;
                        }
                        if ((q == null) || (q.getStatus() != state)) {
                            return false;
                        }
                    }
                }
                return true;
            case item:
                for (Pair a : this.dataStore) {
                    int itemId = ((Integer) a.getLeft());
                    short quantity = 0;
                    MapleInventoryType iType = ItemConstants.getInventoryType(itemId);
                    for (Item item : chr.getInventory(iType).listById(itemId)) {
                        quantity = (short) (quantity + item.getQuantity());
                    }
                    int count = ((Integer) a.getRight());
                    if ((quantity < count) || ((count <= 0) && (quantity > 0))) {
                        return false;
                    }
                }
                return true;
            case lvmin:
                return chr.getLevel() >= this.intStore;
            case lvmax:
                return chr.getLevel() <= this.intStore;
            case mob:
                // @TODO 给召唤几只怪物吧
                return true;
            case npc:
                return (npcid == null) || (npcid == this.intStore);
            case fieldEnter:
                return this.intStore <= 0 || this.intStore == chr.getMapId();
            case pop:
                return chr.getFame() >= this.intStore;
            case questComplete:
                return chr.getNumQuest() >= this.intStore;
            case partyQuest_S:
                int[] partyQuests = {1200, 1201, 1202, 1203, 1204, 1205, 1206, 1300, 1301, 1302};
                int sRankings = 0;
                for (int i : partyQuests) {
                    String rank = chr.getOneInfo(i, "rank");
                    if ((rank != null) && (rank.equals("S"))) {
                        sRankings++;
                    }
                }
                return sRankings >= 5;
            case subJobFlags:
                return chr.getSubcategory() == this.intStore / 2;
            case craftMin:
            case willMin:
            case charismaMin:
            case insightMin:
            case charmMin:
            case senseMin:
//            case interval:
//            case startscript: 
        }
        return true;
    }

    public MapleQuestRewardType getType() {
        return this.type;
    }

    @Override
    public String toString() {
        return this.type.toString();
    }

    public List<Pair<Integer, Integer>> getDataStore() {
        return this.dataStore;
    }
}
