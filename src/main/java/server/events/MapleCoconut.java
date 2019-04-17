package server.events;

import client.MapleCharacter;
import server.Timer.EventTimer;
import tools.MaplePacketCreator;

import java.util.LinkedList;
import java.util.List;

public class MapleCoconut extends MapleEvent {

    private final List<MapleCoconuts> coconuts = new LinkedList<>();
    private final int[] coconutscore = new int[2];
    private int countBombing = 0;
    private int countFalling = 0;
    private int countStopped = 0;

    public MapleCoconut(int channel, MapleEventType type) {
        super(channel, type);
    }

    @Override
    public void finished(MapleCharacter chr) { //do nothing.
    }

    @Override
    public void reset() {
        super.reset();
        resetCoconutScore();
    }

    @Override
    public void unreset() {
        super.unreset();
        resetCoconutScore();
        setHittable(false);
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        super.onMapLoad(chr);
        chr.send(MaplePacketCreator.coconutScore(getCoconutScore()));
    }

    public MapleCoconuts getCoconut(int id) {
        if (id >= coconuts.size()) {
            return null;
        }
        return coconuts.get(id);
    }

    public List<MapleCoconuts> getAllCoconuts() {
        return coconuts;
    }

    public void setHittable(boolean hittable) {
        for (MapleCoconuts nut : coconuts) {
            nut.setHittable(hittable);
        }
    }

    public int getBombings() {
        return countBombing;
    }

    public void bombCoconut() {
        countBombing--;
    }

    public int getFalling() {
        return countFalling;
    }

    public void fallCoconut() {
        countFalling--;
    }

    public int getStopped() {
        return countStopped;
    }

    public void stopCoconut() {
        countStopped--;
    }

    public int[] getCoconutScore() { // coconut event
        return coconutscore;
    }

    public int getMapleScore() { // Team Maple, coconut event
        return coconutscore[0];
    }

    public int getStoryScore() { // Team Story, coconut event
        return coconutscore[1];
    }

    public void addMapleScore() { // Team Maple, coconut event
        coconutscore[0]++;
    }

    public void addStoryScore() { // Team Story, coconut event
        coconutscore[1]++;
    }

    public void resetCoconutScore() {
        coconutscore[0] = 0;
        coconutscore[1] = 0;
        countBombing = 80;
        countFalling = 401;
        countStopped = 20;
        coconuts.clear();
        for (int i = 0; i < 506; i++) {
            coconuts.add(new MapleCoconuts());
        }
    }

    @Override
    public void startEvent() {
        reset();
        setHittable(true);
        getMap(0).broadcastMessage(MaplePacketCreator.serverNotice(5, "The event has started!!"));
        getMap(0).broadcastMessage(MaplePacketCreator.hitCoconut(true, 0, 0));
        getMap(0).broadcastMessage(MaplePacketCreator.getClock(300));

        EventTimer.getInstance().schedule(() -> {
            if (getMapleScore() == getStoryScore()) {
                bonusTime();
            } else {
                for (MapleCharacter chr : getMap(0).getCharactersThreadsafe()) {
                    if (chr.getTeam() == (getMapleScore() > getStoryScore() ? 0 : 1)) {
                        chr.send(MaplePacketCreator.showEffect("event/coconut/victory"));
                        chr.send(MaplePacketCreator.playSound("Coconut/Victory"));
                    } else {
                        chr.send(MaplePacketCreator.showEffect("event/coconut/lose"));
                        chr.send(MaplePacketCreator.playSound("Coconut/Failed"));
                    }
                }
                warpOut();
            }
        }, 300000);
    }

    public void bonusTime() {
        getMap(0).broadcastMessage(MaplePacketCreator.getClock(60));
        EventTimer.getInstance().schedule(() -> {
            if (getMapleScore() == getStoryScore()) {
                for (MapleCharacter chr : getMap(0).getCharactersThreadsafe()) {
                    chr.send(MaplePacketCreator.showEffect("event/coconut/lose"));
                    chr.send(MaplePacketCreator.playSound("Coconut/Failed"));
                }
                warpOut();
            } else {
                for (MapleCharacter chr : getMap(0).getCharactersThreadsafe()) {
                    if (chr.getTeam() == (getMapleScore() > getStoryScore() ? 0 : 1)) {
                        chr.send(MaplePacketCreator.showEffect("event/coconut/victory"));
                        chr.send(MaplePacketCreator.playSound("Coconut/Victory"));
                    } else {
                        chr.send(MaplePacketCreator.showEffect("event/coconut/lose"));
                        chr.send(MaplePacketCreator.playSound("Coconut/Failed"));
                    }
                }
                warpOut();
            }
        }, 60000);

    }

    public void warpOut() {
        setHittable(false);
        EventTimer.getInstance().schedule(() -> {
            for (MapleCharacter chr : getMap(0).getCharactersThreadsafe()) {
                if ((getMapleScore() > getStoryScore() && chr.getTeam() == 0) || (getStoryScore() > getMapleScore() && chr.getTeam() == 1)) {
                    givePrize(chr);
                }
                warpBack(chr);
            }
            unreset();
        }, 10000);
    }

    public static class MapleCoconuts {

        private int hits = 0;
        private boolean hittable = false;
        private boolean stopped = false;
        private long hittime = System.currentTimeMillis();

        public void hit() {
            this.hittime = System.currentTimeMillis() + 1000; // test
            hits++;
        }

        public int getHits() {
            return hits;
        }

        public void resetHits() {
            hits = 0;
        }

        public boolean isHittable() {
            return hittable;
        }

        public void setHittable(boolean hittable) {
            this.hittable = hittable;
        }

        public boolean isStopped() {
            return stopped;
        }

        public void setStopped(boolean stopped) {
            this.stopped = stopped;
        }

        public long getHitTime() {
            return hittime;
        }
    }
}
