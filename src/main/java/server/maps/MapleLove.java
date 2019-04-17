/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import tools.MaplePacketCreator;

import java.awt.*;

/**
 * @author PlayDK
 */
public class MapleLove extends MapleMapObject {

    private final Point pos;
    private final MapleCharacter owner;
    private final String text;
    private final int ft;
    private final int itemid;

    public MapleLove(MapleCharacter owner, Point pos, int ft, String text, int itemid) {
        this.owner = owner;
        this.pos = pos;
        this.text = text;
        this.ft = ft;
        this.itemid = itemid;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.LOVE;
    }

    @Override
    public Point getPosition() {
        return pos.getLocation();
    }

    @Override
    public void setPosition(Point position) {
        throw new UnsupportedOperationException();
    }

    public MapleCharacter getOwner() {
        return owner;
    }

    public int getItemId() {
        return itemid;
    }

    @Override
    public void sendSpawnData(MapleClient c) {
        c.announce(MaplePacketCreator.spawnLove(getObjectId(), itemid, owner.getName(), text, pos, ft));
    }

    @Override
    public void sendDestroyData(MapleClient c) {
        c.announce(MaplePacketCreator.removeLove(getObjectId(), itemid));
    }
}
