package client;

import server.MapleStatEffect;

import java.util.concurrent.ScheduledFuture;

public class MapleBuffStatValueHolder {

    public MapleStatEffect effect;
    public long startTime;
    public int localDuration;
    public int fromChrId;
    public int value;
    public ScheduledFuture<?> schedule;

    public MapleBuffStatValueHolder() {
    }

    public MapleBuffStatValueHolder(MapleStatEffect effect, long startTime, ScheduledFuture<?> schedule, int value, int localDuration, int fromChrId) {
        super();
        this.effect = effect;
        this.startTime = startTime;
        this.schedule = schedule;
        this.value = value;
        this.localDuration = localDuration;
        this.fromChrId = fromChrId;
    }
}
