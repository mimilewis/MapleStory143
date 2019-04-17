/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.movement;

import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;

/**
 * @author admin
 */
public class StaticLifeMovement extends AbstractLifeMovement {

    private Point pixelsPerSecond;
    private int newfh;

    public StaticLifeMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    public Point getPixelsPerSecond() {
        return pixelsPerSecond;
    }

    public void setPixelsPerSecond(Point wobble) {
        this.pixelsPerSecond = wobble;
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
    }

    @Override
    public void serialize(MaplePacketLittleEndianWriter lew) {
        lew.write(getType());
        lew.writePos(getPosition());
        lew.writePos(pixelsPerSecond);
        lew.writeShort(newfh);
        lew.write(getNewstate());
        lew.writeShort(getDuration());
        lew.write(0);
    }
}
