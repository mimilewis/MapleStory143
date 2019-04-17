package handling.world.guild;

import java.io.Serializable;

public class MapleGuildSkill implements Serializable {

    public static final long serialVersionUID = 3565477792055301248L;
    public final int skillID;
    public String purchaser, activator;
    public long timestamp;
    public int level;

    public MapleGuildSkill(int skillID, int level, long timestamp, String purchaser, String activator) {
        this.timestamp = timestamp;
        this.skillID = skillID;
        this.level = level;
        this.purchaser = purchaser;
        this.activator = activator;
    }
}
