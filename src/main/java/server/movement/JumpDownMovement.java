package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class JumpDownMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond;
    private Point offset;
    private int unk;
    private int fh;

    public JumpDownMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public Point getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
    }

    public Point getOffset() {
        return offset;
    }

    public void setOffset(Point wobble) {
        this.offset = wobble;
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
        lew.writePos(pixelsPerSecond);
        lew.writeShort(unk);
        lew.writeShort(fh);
        lew.writePos(offset);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
