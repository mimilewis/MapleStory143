package server.events;

import client.MapleCharacter;
import server.Timer.EventTimer;
import tools.MaplePacketCreator;

import java.util.concurrent.ScheduledFuture;

public class MapleSurvival extends MapleEvent {

    protected final long time = 360000; //reduce for less time
    protected long timeStarted = 0;
    protected ScheduledFuture<?> olaSchedule;

    public MapleSurvival(int channel, MapleEventType type) {
        super(channel, type);
    }

    @Override
    public void finished(MapleCharacter chr) {
        givePrize(chr);
        chr.finishAchievement(25);
    }

    @Override
    public void onMapLoad(MapleCharacter chr) {
        super.onMapLoad(chr);
        if (isTimerStarted()) {
            chr.send(MaplePacketCreator.getClock((int) (getTimeLeft() / 1000)));
        }
    }

    @Override
    public void startEvent() { // TODO: Messages
        unreset();
        super.reset(); //isRunning = true
        broadcast(MaplePacketCreator.getClock((int) (time / 1000)));
        this.timeStarted = System.currentTimeMillis();

        olaSchedule = EventTimer.getInstance().schedule(() -> {
            for (int i = 0; i < type.mapids.length; i++) {
                for (MapleCharacter chr : getMap(i).getCharactersThreadsafe()) {
                    warpBack(chr);
                }
                unreset();
            }
        }, this.time);
        broadcast(MaplePacketCreator.serverNotice(0, "The portal has now opened. Press the up arrow key at the portal to enter."));
        broadcast(MaplePacketCreator.serverNotice(0, "Fall down once, and never get back up again! Get to the top without falling down!"));
    }

    public boolean isTimerStarted() {
        return timeStarted > 0;
    }

    public long getTime() {
        return time;
    }

    public void resetSchedule() {
        this.timeStarted = 0;
        if (olaSchedule != null) {
            olaSchedule.cancel(false);
        }
        olaSchedule = null;
    }

    @Override
    public void reset() {
        super.reset();
        resetSchedule();
        getMap(0).getPortal("join00").setPortalState(false);
    }

    @Override
    public void unreset() {
        super.unreset();
        resetSchedule();
        getMap(0).getPortal("join00").setPortalState(true);
    }

    public long getTimeLeft() {
        return time - (System.currentTimeMillis() - timeStarted);
    }
}
