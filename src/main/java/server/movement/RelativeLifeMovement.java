package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class RelativeLifeMovement extends AbstractLifeMovement {

    public RelativeLifeMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writeShort(getPosition().x);
        lew.writeShort(getPosition().y);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
