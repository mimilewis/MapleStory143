package client;

import constants.GameConstants;
import server.life.MapleLifeFactory;
import server.quest.MapleQuest;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class MapleQuestStatus implements Serializable {

    private static final long serialVersionUID = 91795419934134L;
    private transient MapleQuest quest;
    private byte status;
    private Map<Integer, Integer> killedMobs = null;
    private int npc;
    private long completionTime;
    private int forfeited = 0;
    private String customData;

    public MapleQuestStatus() {

    }

    /**
     * Creates a new instance of MapleQuestStatus
     */
    public MapleQuestStatus(MapleQuest quest, int status) {
        this.quest = quest;
        this.setStatus((byte) status);
        this.completionTime = System.currentTimeMillis();
        if (status == 1) { // 开始任务
            if (!quest.getRelevantMobs().isEmpty()) {
                registerMobs();
            }
        }
    }

    public MapleQuestStatus(MapleQuest quest, byte status, int npc) {
        this.quest = quest;
        this.setStatus(status);
        this.setNpc(npc);
        this.completionTime = System.currentTimeMillis();
        if (status == 1) { // 开始任务
            if (!quest.getRelevantMobs().isEmpty()) {
                registerMobs();
            }
        }
    }

    public MapleQuest getQuest() {
        return quest;
    }

    public void setQuest(int qid) {
        this.quest = MapleQuest.getInstance(qid);
    }

    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    public int getNpc() {
        return npc;
    }

    public void setNpc(int npc) {
        this.npc = npc;
    }

    public boolean isCustom() {
        return GameConstants.isCustomQuest(quest.getId());
    }

    private void registerMobs() {
        killedMobs = new LinkedHashMap<>();
        for (int i : quest.getRelevantMobs().keySet()) {
            killedMobs.put(i, 0);
        }
    }

    private int maxMob(int mobid) {
        for (Entry<Integer, Integer> qs : quest.getRelevantMobs().entrySet()) {
            if (qs.getKey() == mobid) {
                return qs.getValue();
            }
        }
        return 0;
    }

    public boolean mobKilled(int id, int skillID) {
        if (quest != null && quest.getSkillID() > 0) {
            if (quest.getSkillID() != skillID) {
                return false;
            }
        }
        Integer mob = killedMobs.get(id);
        if (mob != null) {
            int mo = maxMob(id);
            if (mob >= mo) {
                return false; //nothing happened
            }
            killedMobs.put(id, Math.min(mob + 1, mo));
            return true;
        }
        for (Entry<Integer, Integer> mo : killedMobs.entrySet()) {
            if (MapleLifeFactory.exitsQuestCount(mo.getKey(), id)) {
                int mobb = maxMob(mo.getKey());
                if (mo.getValue() >= mobb) {
                    return false; //nothing
                }
                killedMobs.put(mo.getKey(), Math.min(mo.getValue() + 1, mobb));
                return true;
            }
        }
        return false;
    }

    public void setMobKills(int id, int count) {
        if (killedMobs == null) {
            registerMobs();
        }
        killedMobs.put(id, count);
    }

    public boolean hasMobKills() {
        return killedMobs != null && killedMobs.size() > 0;
    }

    public int getMobKills(int id) {
        Integer mob = killedMobs.get(id);
        if (mob == null) {
            return 0;
        }
        return mob;
    }

    public Map<Integer, Integer> getMobKills() {
        return killedMobs;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    public void setCompletionTime(long completionTime) {
        this.completionTime = completionTime;
    }

    public int getForfeited() {
        return forfeited;
    }

    public void setForfeited(int forfeited) {
        if (forfeited >= this.forfeited) {
            this.forfeited = forfeited;
        } else {
            throw new IllegalArgumentException("Can't set forfeits to something lower than before.");
        }
    }

    public String getCustomData() {
        return customData;
    }

    public void setCustomData(String customData) {
        this.customData = customData;
    }

    /*
     * 每日任务
     */
    public boolean isDailyQuest() {
        switch (quest.getId()) {
            case 11463: //[传说]挑战！怪物公园5次通关！
            case 11464: //[传说]挑战！怪物公园5次通关！
            case 11465: //[传说]时空石支援
            case 11468: //[传说]挑战！怪物公园5次通关！
                return true;
        }
        return false;
    }
}
