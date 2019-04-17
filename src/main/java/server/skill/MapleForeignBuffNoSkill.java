/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import tools.data.output.MaplePacketLittleEndianWriter;

public abstract class MapleForeignBuffNoSkill extends MapleForeignBuffNoStat {

    public MapleForeignBuffNoSkill(MapleBuffStat stat) {
        super(stat);
    }

    @Override
    public abstract void writePacket(MaplePacketLittleEndianWriter mplew, int value);
}
