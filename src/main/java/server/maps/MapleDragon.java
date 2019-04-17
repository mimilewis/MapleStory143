package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import tools.packet.SummonPacket;

public class MapleDragon extends AnimatedMapleMapObject {

    private int owner, jobid;

    public MapleDragon(MapleCharacter owner) {
        super();
        this.owner = owner.getId();
        this.jobid = owner.getJob();
        if (jobid < 2200 || jobid > 2218) {
            throw new RuntimeException("试图生成1个龙龙的信息，但角色不是龙神职业.");
        }
        setPosition(owner.getTruePosition());
        setStance(4);
    }

    public void setJobid(int jobid) {
        this.jobid = jobid;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(SummonPacket.spawnDragon(this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(SummonPacket.removeDragon(this.owner));
    }

    public int getOwner() {
        return this.owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
    }

    public int getJobId() {
        return this.jobid;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }
}
