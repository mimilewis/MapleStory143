package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public interface LifeMovementFragment {

    void serialize(MaplePacketLittleEndianWriter lew);

    Point getPosition();
}
