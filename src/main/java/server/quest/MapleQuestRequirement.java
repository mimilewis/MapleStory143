package server.quest;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleTraitType;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.ItemConstants;
import tools.Pair;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class MapleQuestRequirement implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final MapleQuest quest;
    private final MapleQuestRequirementType type;
    private int intStore;
    private String stringStore;
    private List<Pair<Integer, Integer>> dataStore;

    /**
     * Creates a new instance of MapleQuestRequirement
     */
    public MapleQuestRequirement(MapleQuest quest, MapleQuestRequirementType type, ResultSet rse) throws SQLException {
        this.type = type;
        this.quest = quest;

        switch (type) {
            case pet:
            case mbcard:
            case mob:
            case item:
            case quest:
            case skill:
            case job: {
                dataStore = new LinkedList<>();
                String[] first = rse.getString("intStoresFirst").split(", ");
                String[] second = rse.getString("intStoresSecond").split(", ");
                if (first.length <= 0 && rse.getString("intStoresFirst").length() > 0) {
                    dataStore.add(new Pair<>(Integer.parseInt(rse.getString("intStoresFirst")), Integer.parseInt(rse.getString("intStoresSecond"))));
                }
                for (int i = 0; i < first.length; i++) {
                    if (first[i].length() > 0 && second[i].length() > 0) {
                        dataStore.add(new Pair<>(Integer.parseInt(first[i]), Integer.parseInt(second[i])));
                    }
                }
                break;
            }
            case partyQuest_S:
            case dayByDay:
            case normalAutoStart:
            case subJobFlags:
            case fieldEnter:
            case pettamenessmin:
            case npc:
            case questComplete:
            case pop:
            case interval:
            case mbmin:
            case lvmax:
            case lvmin: {
                intStore = Integer.parseInt(rse.getString("stringStore"));
                break;
            }
            case end: {
                stringStore = rse.getString("stringStore");
                break;
            }
        }
    }

    public boolean check(MapleCharacter chr, Integer npcid) {
        switch (type) {
            case job:
                for (Pair<Integer, Integer> a : dataStore) {
                    if (a.getRight() == chr.getJob() || chr.isGM()) {
                        return true;
                    }
                }
                return false;
            case skill: {
                for (Pair<Integer, Integer> a : dataStore) {
                    boolean acquire = a.getRight() > 0;
                    int skill = a.getLeft();
                    Skill skil = SkillFactory.getSkill(skill);
                    if (acquire) {
                        if (skil.isFourthJob()) {
                            if (chr.getMasterLevel(skil) == 0) {
                                return false;
                            }
                        } else if (skil.isGuildSkill()) {
                            return !(chr.getGuild() == null || chr.getGuild().getLevel() < 1 || !chr.getGuild().hasSkill(skill));
                        } else {
                            if (chr.getSkillLevel(skil) == 0) {
                                return false;
                            }
                        }
                    } else {
                        if (chr.getSkillLevel(skil) > 0 || chr.getMasterLevel(skil) > 0) {
                            return false;
                        }
                    }
                }
                return true;
            }
            case quest:
                for (Pair<Integer, Integer> a : dataStore) {
                    MapleQuestStatus q = chr.getQuest(MapleQuest.getInstance(a.getLeft()));
                    int state = a.getRight();
                    if (state != 0) {
                        if (q == null && state == 0) {
                            continue;
                        }
                        if (q == null || q.getStatus() != state) {
                            return false;
                        }
                    }
                }
                return true;
            case item:
                MapleInventoryType iType;
                int itemId;
                short quantity;

                for (Pair<Integer, Integer> a : dataStore) {
                    itemId = a.getLeft();
                    quantity = 0;
                    iType = ItemConstants.getInventoryType(itemId);
                    for (Item item : chr.getInventory(iType).listById(itemId)) {
                        quantity += item.getQuantity();
                    }
                    int count = a.getRight();
                    if (quantity < count || (count <= 0 && quantity > 0)) {
                        return false;
                    }
                }
                return true;
            case lvmin:
                return chr.getLevel() >= intStore;
            case lvmax:
                return chr.getLevel() <= intStore;
            case end:
                String timeStr = stringStore;
                if (timeStr == null || timeStr.length() <= 0) {
                    return true;
                }
                Calendar cal = Calendar.getInstance();
                cal.set(Integer.parseInt(timeStr.substring(0, 4)), Integer.parseInt(timeStr.substring(4, 6)), Integer.parseInt(timeStr.substring(6, 8)), Integer.parseInt(timeStr.substring(8, 10)), 0);
                return cal.getTimeInMillis() >= System.currentTimeMillis();
            case mob:
                for (Pair<Integer, Integer> a : dataStore) {
                    int mobId = a.getLeft();
                    int killReq = a.getRight();
                    if (chr.getQuest(quest).getMobKills(mobId) < killReq) {
                        return false;
                    }
                }
                return true;
            case npc:
                return npcid == null || npcid == intStore;
            case fieldEnter:
                return intStore <= 0 || intStore == chr.getMapId();
            case mbmin:
                return chr.getMonsterBook().getSeen() >= intStore;
            case mbcard:
                for (Pair<Integer, Integer> a : dataStore) {
                    int cardId = a.getLeft();
                    int killReq = a.getRight();
                    if (chr.getMonsterBook().getLevelByCard(cardId) < killReq) {
                        return false;
                    }
                }
                return true;
            case pop:
                return chr.getFame() >= intStore;
            case questComplete:
                return chr.getNumQuest() >= intStore;
            case interval:
                return chr.getQuest(quest).getStatus() != 2 || chr.getQuest(quest).getCompletionTime() <= System.currentTimeMillis() - intStore * 60 * 1000L;
            case pet:
                for (Pair<Integer, Integer> a : dataStore) {
                    if (chr.getPetByItemId(a.getRight()) != -1) {
                        return true;
                    }
                }
                return false;
            case pettamenessmin:
                MaplePet[] pet = chr.getSpawnPets();
                for (int i = 0; i < 3; i++) {
                    if (pet[i] != null && pet[i].getSummoned() && pet[i].getCloseness() >= intStore) {
                        return true;
                    }
                }
                return false;
            case partyQuest_S:
                int[] partyQuests = new int[]{1200, 1201, 1202, 1203, 1204, 1205, 1206, 1300, 1301, 1302};
                int sRankings = 0;
                for (int i : partyQuests) {
                    String rank = chr.getOneInfo(i, "rank");
                    if (rank != null && rank.equals("S")) {
                        sRankings++;
                    }
                }
                return sRankings >= 5;
            case subJobFlags: // 1 for non-DB, 2 for DB...
                return chr.getSubcategory() == (intStore / 2);
            case craftMin:
            case willMin:
            case charismaMin:
            case insightMin:
            case charmMin:
            case senseMin:
                return chr.getTrait(MapleTraitType.getByQuestName(type.name())).getLevel() >= intStore;
            default:
                return true;
        }
    }

    public MapleQuestRequirementType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }

    public List<Pair<Integer, Integer>> getDataStore() {
        return dataStore;
    }
}
