package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import scripting.reactor.ReactorScriptManager;
import server.Timer.MapTimer;
import tools.MaplePacketCreator;
import tools.Pair;

import java.awt.*;

public class MapleReactor extends MapleMapObject {

    private int rid;
    private MapleReactorStats stats;
    private byte state = 0, facingDirection = 0;
    private int delay = -1;
    private MapleMap map;
    private String name = "";
    private boolean timerActive = false, alive = true, custom = false, pqAction = false;
    private Point srcPos = new Point();

    public MapleReactor(MapleReactorStats stats, int rid) {
        this.stats = stats;
        this.rid = rid;
    }

    public Point getSrcPos() {
        return srcPos;
    }

    public void setSrcPos(Point srcPos) {
        this.srcPos = srcPos;
    }

    public int getRid() {
        return rid;
    }

    public void setRid(int rid) {
        this.rid = rid;
    }

    public MapleReactorStats getStats() {
        return stats;
    }

    public void setStats(MapleReactorStats stats) {
        this.stats = stats;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean c) {
        this.custom = c;
    }

    public byte getFacingDirection() {
        return facingDirection;
    }

    public void setFacingDirection(byte facingDirection) {
        this.facingDirection = facingDirection;
    }

    public boolean isTimerActive() {
        return timerActive;
    }

    public void setTimerActive(boolean active) {
        this.timerActive = active;
    }

    public int getReactorId() {
        return rid;
    }

    public byte getState() {
        return state;
    }

    public void setState(byte state) {
        this.state = state;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.REACTOR;
    }

    public int getReactorType() {
        return stats.getType(state);
    }

    public byte getTouch() {
        return stats.canTouch(state);
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public Pair<Integer, Integer> getReactItem() {
        return stats.getReactItem(state);
    }

    public boolean isPqAction() {
        return pqAction;
    }

    public void setPqAction(boolean pqAction) {
        this.pqAction = pqAction;
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.destroyReactor(this));
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(MaplePacketCreator.spawnReactor(this));
    }

    public void forceStartReactor(MapleClient c) {
        ReactorScriptManager.getInstance().act(c, this);
    }

    public void forceHitReactor(byte newState) {
        forceHitReactor(newState, 0, 0, 0);
    }

    public void forceHitReactor(byte newState, int n2, int cid, int n4) {
        setState(newState);
        setTimerActive(false);
        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (short) 0, n2, cid, n4));
    }

    //hitReactor command for item-triggered reactors
    public void hitReactor(MapleClient c) {
        hitReactor(0, (short) 0, c);
    }

    public void forceTrigger() {
        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (short) 0));
    }

    public void delayedDestroyReactor(long delay) {
        MapTimer.getInstance().schedule(() -> map.destroyReactor(getObjectId()), delay);
    }

    public void hitReactor(int charPos, short stance, MapleClient c) {
        if (stats.getType(state) < 999 && stats.getType(state) != -1) {
            //type 2 = only hit from right (kerning swamp plants), 00 is air left 02 is ground left
            byte oldState = state;
            if (!(stats.getType(state) == 2 && (charPos == 0 || charPos == 2))) { // next state
                state = stats.getNextState(state);

                if (stats.getNextState(state) == -1 || stats.getType(state) == 999) { //end of reactor
                    if ((stats.getType(state) < 100 || stats.getType(state) == 999) && delay > 0) { //reactor broken
                        map.destroyReactor(getObjectId());
                    } else { //item-triggered on step
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(this, stance));
                    }
                    //if (rid > 200011) {
                    ReactorScriptManager.getInstance().act(c, this);
                    //}
                } else { //reactor not broken yet
                    if (rid == 9239001 && !this.isPqAction()) {
                        this.setSrcPos(this.getTruePosition());
                        this.setPqAction(true);
                        this.forceHitReactor((byte) 1, 2, c.getPlayer().getId(), 0);
                    } else {
                        if (this.isPqAction()) {
                            this.setPqAction(false);
                            c.getPlayer().setReactor(null);
                            ReactorScriptManager.getInstance().act(c, this);
                        }
                    }
                    boolean done = false;
                    map.broadcastMessage(MaplePacketCreator.triggerReactor(this, (int) stance));
                    if (state == stats.getNextState(state) || rid == 2618000 || rid == 2309000) { //current state = next state, looping reactor
                        if (rid > 200011) {
                            ReactorScriptManager.getInstance().act(c, this);
                        }
                        done = true;
                    }
                    if (stats.getTimeOut(state) > 0) {
                        if (!done && rid > 200011) {
                            ReactorScriptManager.getInstance().act(c, this);
                        }
                        scheduleSetState(c.getPlayer(), state, oldState, stats.getTimeOut(state));
                    }
                }
            }
        }
    }

    public Rectangle getArea() {
        int height = stats.getBR().y - stats.getTL().y;
        int width = stats.getBR().x - stats.getTL().x;
        int origX = getTruePosition().x + stats.getTL().x;
        int origY = getTruePosition().y + stats.getTL().y;
        return new Rectangle(origX, origY, width, height);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "反应堆 工作ID:" + getObjectId() + " ReactorID: " + rid + " 坐标: " + getPosition().x + "/" + getPosition().y + " 状态: " + state + " 类型: " + stats.getType(state);
    }

    public void delayedHitReactor(final MapleClient c, final long delay) {
        MapTimer.getInstance().schedule(() -> hitReactor(c), delay);
    }

    public void scheduleSetState(final MapleCharacter chr, final byte oldState, final byte newState, final long delay) {
        MapTimer.getInstance().schedule(() -> {
            if (MapleReactor.this.state == oldState) {
                forceHitReactor(newState);
            }
        }, delay);
    }
}
