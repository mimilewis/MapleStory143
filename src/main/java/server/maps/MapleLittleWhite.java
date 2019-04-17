/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import server.movement.LifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.StaticLifeMovement;
import tools.packet.SummonPacket;

import java.awt.*;
import java.util.List;

public class MapleLittleWhite extends AnimatedMapleMapObject {

    private final int owner, jobid, skillid, fh;
    private boolean stats, show;
    private Point pos = new Point(0, 0);

    public MapleLittleWhite(MapleCharacter owner) {
        this.owner = owner.getId();
        this.jobid = owner.getJob();
        this.skillid = 40020109;
        this.show = true;
        this.fh = owner.getFH();
        this.stats = false;
        setPosition(owner.getTruePosition());
        setStance(this.fh);
    }

    public int getOwner() {
        return this.owner;
    }

    public int getJobId() {
        return this.jobid;
    }

    public int getSkillId() {
        return this.skillid;
    }

    public void sendStats() {
        this.stats = (!this.stats);
    }

    public boolean getStats() {
        return this.stats;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.SUMMON;
    }

    public final Point getPos() {
        return this.pos;
    }

    public final void setPos(Point pos) {
        this.pos = pos;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(SummonPacket.spawnLittleWhite(this));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(SummonPacket.showLittleWhite(this));
    }

    public final void updatePosition(List<LifeMovementFragment> movement) {
        for (LifeMovementFragment move : movement) {
            if ((move instanceof LifeMovement)) {
                if ((move instanceof StaticLifeMovement)) {
                    setPos(move.getPosition());
                }
                setStance(((LifeMovement) move).getNewstate());
            }
        }
    }
}
