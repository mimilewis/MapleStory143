package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class BounceMovement extends AbstractLifeMovement {

    private int unk;
    private int fh;

    public BounceMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public int getUnk() {
        return unk;
    }

    public void setUnk(int unk) {
        this.unk = unk;
    }

    public int getFH() {
        return fh;
    }

    public void setFH(int fh) {
        this.fh = fh;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writePos(getPosition());
        lew.writeShort(getUnk());
        lew.writeShort(getFH());
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}
