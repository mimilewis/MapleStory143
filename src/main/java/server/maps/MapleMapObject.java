package server.maps;

import client.MapleClient;
import constants.GameConstants;

import java.awt.*;

/**
 * 冒险岛地图上所有对象(例如:玩家、怪物、召唤兽、NPC等)的抽象基类.
 *
 * @author dongjak
 */
public abstract class MapleMapObject {

    private final Point position = new Point();
    private int objectId;

    public Point getPosition() {
        return new Point(position);
    }

    public void setPosition(Point position) {
        this.position.x = position.x;
        this.position.y = position.y;
    }

    public Point getTruePosition() {
        return position;
    }

    public int getObjectId() {
        return objectId;
    }

    public void setObjectId(int id) {
        this.objectId = id;
    }

    public int getRange() {
        return GameConstants.maxViewRangeSq();
    }

    public abstract MapleMapObjectType getType();

    public abstract void sendSpawnData(MapleClient client);

    public abstract void sendDestroyData(MapleClient client);
}
