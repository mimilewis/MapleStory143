package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class AranMovement extends AbstractLifeMovement {

    public AranMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
