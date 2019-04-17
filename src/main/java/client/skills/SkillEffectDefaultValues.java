package client.skills;

import java.util.HashMap;
import java.util.Map;

public class SkillEffectDefaultValues {

    private final static Map<String, Integer> defvalues = new HashMap<>();

    static {
        defvalues.put("time", -1);
        defvalues.put("damage", 100);
        defvalues.put("attackCount", 1);
        defvalues.put("bulletCount", 1);
        defvalues.put("moveTo", -1);
        defvalues.put("prop", 100);
        defvalues.put("mobCount", 1);
        defvalues.put("slotCount", 0);
        defvalues.put("type", 0);
        defvalues.put("onActive", -1);
    }

    public static int getDef(String key) {
        if (defvalues.containsKey(key)) {
            return defvalues.get(key);
        }
        return 0;
    }
}
