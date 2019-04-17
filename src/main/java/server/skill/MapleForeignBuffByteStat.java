/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MapleForeignBuffByteStat extends MapleForeignBuffStat {

    public MapleForeignBuffByteStat(MapleBuffStat stat) {
        super(stat);
    }

    @Override
    public void writePacket(MaplePacketLittleEndianWriter mplew, int value) {
        mplew.write(value);
    }
}
