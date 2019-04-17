package handling.world;

import client.MapleBuffStat;
import server.MapleStatEffect;

import java.io.Serializable;
import java.util.Map;

/*
 * 角色BUFF的具体信息
 */
public class PlayerBuffValueHolder implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public final long startTime;
    public final MapleStatEffect effect;
    public final Map<MapleBuffStat, Integer> statup;
    public int localDuration, fromChrId;

    public PlayerBuffValueHolder(long startTime, MapleStatEffect effect, Map<MapleBuffStat, Integer> statup, int localDuration, int fromChrId) {
        this.startTime = startTime;
        this.effect = effect;
        this.statup = statup;
        this.localDuration = localDuration;
        this.fromChrId = fromChrId;
    }

    public PlayerBuffValueHolder(long startTime, MapleStatEffect effect, Map<MapleBuffStat, Integer> statup) {
        this.startTime = startTime;
        this.effect = effect;
        this.statup = statup;
    }
}
