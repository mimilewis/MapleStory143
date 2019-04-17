package scripting.event;

import com.fasterxml.jackson.core.type.TypeReference;
import server.console.groups.boss.BossManagePanel;
import tools.JsonUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BossEventManager {

    private static final String filepath = "./config/boss.json";
    public static boolean ISOPEN = false;
    private static BossEventManager instance;
    private final Map<String, BossEventEntry> bossEntrys = new LinkedHashMap<>();

    public BossEventManager() {
        try {
            if (ISOPEN) {
                load();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BossEventManager getInstance() {
        if (instance == null)
            instance = new BossEventManager();
        return instance;
    }

    public Set<String> getAllBossName() {
        return bossEntrys.keySet();
    }

    public Set<String> getAllBossEventName() {
        Set<String> ret = new LinkedHashSet<>();
        for (BossEventEntry bossEventEntry : bossEntrys.values()) {
            ret.add(bossEventEntry.getEventname());
        }
        return ret;
    }

    public boolean canUseDefaultScript(String name) {
        for (BossEventEntry bossEventEntry : bossEntrys.values()) {
            if (name.equalsIgnoreCase(bossEventEntry.getEventname()) && bossEventEntry.isUseDefaultScript()) {
                return true;
            }
        }
        return false;
    }

    public BossEventEntry getBossEventEntry(String name) {
        return bossEntrys.get(name);
    }

    public void load() throws IOException {
        Map<String, Map<String, String>> bosslist_data;
        File file = new File(filepath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                System.out.println("打开BOSS配置文件失败。");
                return;
            }
            bosslist_data = JsonUtil.getMapperInstance().readValue(BossManagePanel.class.getResourceAsStream("bossDefault.json"), new TypeReference<Map<String, Map<String, String>>>() {
            });
        } else {
            bosslist_data = JsonUtil.getMapperInstance().readValue(file, new TypeReference<Map<String, Map<String, String>>>() {
            });
        }
        if (!bosslist_data.isEmpty()) {
            for (Map.Entry<String, Map<String, String>> entry : bosslist_data.entrySet()) {
                Map<String, String> values = entry.getValue();
                BossEventEntry bossEventEntry = new BossEventEntry(
                        values.get("name"),
                        Boolean.valueOf(values.get("isOpen")),
                        Boolean.valueOf(values.get("isUseDefaultScript")),
                        Integer.valueOf(values.get("maxPlayerCount")),
                        Integer.valueOf(values.get("maxCount")),
                        Integer.valueOf(values.get("maxTime")));
                bossEntrys.put(entry.getKey(), bossEventEntry);
            }
        }
    }

    public class BossEventEntry {
        private final String eventname;
        private final boolean isOpen;
        private final boolean isUseDefaultScript;
        private final int maxPlayerCount;
        private final int maxCount;
        private final int maxTime;

        public BossEventEntry(String eventname, boolean isOpen, boolean isUseCustomScript, int maxPlayerCount, int maxCount, int maxTime) {

            this.eventname = eventname;
            this.isOpen = isOpen;
            this.isUseDefaultScript = isUseCustomScript;
            this.maxPlayerCount = maxPlayerCount;
            this.maxCount = maxCount;
            this.maxTime = maxTime;
        }

        public String getEventname() {
            return eventname;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public boolean isUseDefaultScript() {
            return isUseDefaultScript;
        }

        public int getMaxPlayerCount() {
            return maxPlayerCount;
        }

        public int getMaxCount() {
            return maxCount;
        }

        public int getMaxTime() {
            return maxTime;
        }
    }
}
