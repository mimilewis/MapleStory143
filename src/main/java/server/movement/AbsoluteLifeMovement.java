package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class AbsoluteLifeMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond, offset;
    private int newfh;

    public AbsoluteLifeMovement(int type, Point position, int duration, int newstate) {
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

    public int getNewFH() {
        return newfh;
    }

    public void setNewFH(short fh) {
        this.newfh = fh;
    }

    public void defaulted() {
        newfh = 0;
        pixelsPerSecond = new Point(0, 0);
        offset = new Point(0, 0);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writePos(getPosition());
        lew.writePos(pixelsPerSecond);
        lew.writeShort(newfh);
        lew.writePos(offset);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
