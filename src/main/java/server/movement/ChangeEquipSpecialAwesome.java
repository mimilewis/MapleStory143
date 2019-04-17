package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

public class ChangeEquipSpecialAwesome implements LifeMovementFragment {

    private final int type;
    private final int wui;

    public ChangeEquipSpecialAwesome(int type, int wui) {
        this.type = type;
        this.wui = wui;
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(type);
        lew.write(wui);
    }

    @Override
    public Point getPosition() {
        return new Point(0, 0);
    }
}
