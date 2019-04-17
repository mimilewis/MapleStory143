package client.skills;

import tools.CaltechEval;

import java.util.HashMap;
import java.util.Map;

public class SkillStats {

    private final int level;
    private final Map<String, Integer> stats = new HashMap<>();

    public SkillStats(int skilllevel) {
        this.level = skilllevel;
    }

    public void setStats(String name, int value) {
        if (stats.containsKey(name)) {
            stats.remove(name);
        }
        stats.put(name, value);
    }

    public void setStats(String name, String fomular) {
        if (stats.containsKey(name)) {
            stats.remove(name);
        }
        if (fomular != null) {
            int result = (int) (new CaltechEval(fomular.replace("x", level + "")).evaluate());
            stats.put(name, result);
        }
    }

    public int getStats(String key) {
        if (stats.containsKey(key)) {
            return stats.get(key);
        }
        return SkillEffectDefaultValues.getDef(key);
    }
}
