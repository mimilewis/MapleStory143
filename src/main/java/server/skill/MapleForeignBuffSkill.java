/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import handling.Buffstat;
import server.MapleStatEffect;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 技能外观管理器
 */
public class MapleForeignBuffSkill implements Serializable {

    static final long serialVersionUID = 9179541993413739514L;
    private MapleStatEffect effect;
    private List<MapleForeignBuffStat> stats;

    private MapleForeignBuffSkill() {
    }

    public MapleForeignBuffSkill(MapleStatEffect effect) {
        this.effect = effect;
        this.stats = new ArrayList<>();
    }

    public MapleStatEffect getEffect() {
        return effect;
    }

    public List<MapleForeignBuffStat> getStats() {
        return stats;
    }

    public boolean hasStats() {
        synchronized (this) {
            for (MapleForeignBuffStat foreignBuffStat : stats) {
                if (!(foreignBuffStat instanceof MapleForeignBuffNoSkill)) {
                    return true;
                }
            }
            return false;
        }
    }

    public void applyStat(MaplePacketLittleEndianWriter mplew, MapleBuffStat stat, int value) {
        synchronized (this) {
            for (MapleForeignBuffStat buffStat : stats) {
                if (buffStat.getStat().equals(stat) && (buffStat instanceof MapleForeignBuffNoSkill || !(buffStat instanceof MapleForeignBuffNoStat))) {
                    buffStat.writePacket(mplew, value);
                    break;
                }
            }
        }
    }

    public boolean hasStat(Buffstat stat) {
        synchronized (this) {
            for (MapleForeignBuffStat buffStat : stats) {
                if (buffStat.getStat().equals(stat)) {
                    return true;
                }
            }
            return false;
        }
    }
}
