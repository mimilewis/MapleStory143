/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MapleForeignBuffFixedShort extends MapleForeignBuffStat {

    private final short value;

    public MapleForeignBuffFixedShort(MapleBuffStat stat, short _value) {
        super(stat);
        this.value = _value;
    }

    @Override
    public void writePacket(MaplePacketLittleEndianWriter mplew, int value) {
        mplew.writeShort(this.value);
    }
}
