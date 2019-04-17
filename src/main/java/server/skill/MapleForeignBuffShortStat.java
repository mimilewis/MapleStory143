/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MapleForeignBuffShortStat extends MapleForeignBuffStat {

    public MapleForeignBuffShortStat(MapleBuffStat stat) {
        super(stat);
    }

    @Override
    public void writePacket(MaplePacketLittleEndianWriter mplew, int value) {
        mplew.writeShort(value);
    }
}
