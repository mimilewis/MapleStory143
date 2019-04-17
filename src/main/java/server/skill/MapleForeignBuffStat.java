/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.io.Serializable;

public abstract class MapleForeignBuffStat implements Serializable {

    static final long serialVersionUID = 9179541993413798759L;
    private MapleBuffStat stat;

    private MapleForeignBuffStat() {
    }

    public MapleForeignBuffStat(MapleBuffStat stat) {
        this.stat = stat;
    }

    public abstract void writePacket(MaplePacketLittleEndianWriter mplew, int value);

    public MapleBuffStat getStat() {
        return stat;
    }
}
