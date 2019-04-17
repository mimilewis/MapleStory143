package tools;

import java.awt.*;
import java.util.List;

public class AttackPair {

    public final int objectid;
    public final List<Pair<Long, Boolean>> attack;
    public int ksPsychicObjectId = -1;
    public Point point;

    public AttackPair(int objectid, List<Pair<Long, Boolean>> attack) {
        this.objectid = objectid;
        this.attack = attack;
    }

    public AttackPair(int objectid, int ksPsychicObjectId, List<Pair<Long, Boolean>> attack) {
        this.objectid = objectid;
        this.ksPsychicObjectId = ksPsychicObjectId;
        this.attack = attack;
    }

    public AttackPair(int objectid, Point point, List<Pair<Long, Boolean>> attack) {
        this.objectid = objectid;
        this.point = point;
        this.attack = attack;
    }
}
