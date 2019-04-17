package client;

import org.junit.Test;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MapleBuffStatTest {

    @Test
    public void name() throws Exception {
        int value = 75;

        int mask = 1 << 31 - value % 32;
        int pos = (int) Math.floor(value / 32) + 1;


        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeInt(mask);
        System.out.println(Integer.toHexString(mask));
        System.out.println(pos);
        System.out.println(mplew.toString());

    }

    @Test
    public void maskToValue() throws Exception {
//        int mask = 0x0;
//        int pos = 1;
//        int value = pos * 32;
//        while ()
    }
}