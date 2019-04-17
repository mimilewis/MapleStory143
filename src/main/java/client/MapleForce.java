package client;

import server.maps.MapleMap;
import tools.Pair;

import java.util.ArrayList;
import java.util.List;

public class MapleForce {

    private final int ownerID;
    private final int skillID;
    private final int fromMobOID;
    private final int attackCount;
    private final MapleForceType type;
    private final byte subType;
    private final byte value;
    private final List<Integer> toMobOID;
    private final List<Pair<Integer, Byte>> forceOID = new ArrayList<>();

    public MapleForce(int ownerID, int skillID, int fromMobOID, MapleForceType type, byte subType, List<Integer> toMobOID, byte value, int attackCount, MapleMap map) {
        this.ownerID = ownerID;
        this.skillID = skillID;
        this.fromMobOID = fromMobOID;
        this.type = type;
        this.subType = subType;
        this.toMobOID = toMobOID;
        this.value = value;
        this.attackCount = attackCount;
        initForces(map);
    }

    private void initForces(MapleMap map) {
        for (int i = 0; i < attackCount; i++) {
            forceOID.add(new Pair<>(map.getSpawnedForcesOnMap(), value));
        }
    }


    public int getOwnerID() {
        return ownerID;
    }

    public int getSkillID() {
        return skillID;
    }

    public int getFromMobOID() {
        return fromMobOID;
    }

    public MapleForceType getType() {
        return type;
    }

    public byte getSubType() {
        return subType;
    }

    public List<Integer> getToMobOID() {
        return toMobOID;
    }

    public List<Pair<Integer, Byte>> getForceOID() {
        return forceOID;
    }

    public byte getValue() {
        return value;
    }

    public int getAttackCount() {
        return attackCount;
    }
}
