package client;

import tools.Randomizer;
import tools.data.output.MaplePacketLittleEndianWriter;

public final class PlayerRandomStream {

    private transient long seed1, seed2, seed3;

    public PlayerRandomStream() {
        this.CRand32__Seed(Randomizer.nextLong(), Randomizer.nextLong(), Randomizer.nextLong());
    }

    public void CRand32__Seed(long s1, long s2, long s3) {
        seed1 = s1 | 0x100000;
        seed2 = s2 | 0x1000;
        seed3 = s3 | 0x10;
    }

    public long CRand32__Random() {
        long v8 = ((this.seed1 & 0xFFFFFFFE) << 12) ^ ((this.seed1 & 0x7FFC0 ^ (this.seed1 >> 13)) >> 6);
        long v9 = 16 * (this.seed2 & 0xFFFFFFF8) ^ (((this.seed2 >> 2) ^ this.seed2 & 0x3F800000) >> 23);
        long v10 = ((this.seed3 & 0xFFFFFFF0) << 17) ^ (((this.seed3 >> 3) ^ this.seed3 & 0x1FFFFF00) >> 8);
        return (v8 ^ v9 ^ v10) & 0xffffffffL; // to be confirmed, I am not experienced in converting signed > unsigned
    }

    public void connectData(MaplePacketLittleEndianWriter mplew) {
        long v5 = CRand32__Random();
        long s2 = CRand32__Random();
        long v6 = CRand32__Random();

        CRand32__Seed(v5, s2, v6);

        mplew.writeInt((int) v5);
        mplew.writeInt((int) s2);
        mplew.writeInt((int) v6);
    }
}
