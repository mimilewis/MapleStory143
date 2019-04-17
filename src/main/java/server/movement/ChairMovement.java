package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class ChairMovement extends AbstractLifeMovement {

    private int newfh;

    public ChairMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public int getNewFH() {
        return newfh;
    }

    public void setNewFH(int fh) {
        this.newfh = fh;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.writeShort(newfh);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
