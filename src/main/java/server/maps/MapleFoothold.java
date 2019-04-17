package server.maps;

import java.awt.*;

public class MapleFoothold implements Comparable<MapleFoothold> {

    private final Point p1;
    private final Point p2;
    private final int id;
    private short next, prev;

    public MapleFoothold(Point p1, Point p2, int id) {
        this.p1 = p1;
        this.p2 = p2;
        this.id = id;
    }

    public boolean isWall() {
        return p1.x == p2.x;
    }

    public Point getPoint1() {
        return p1;
    }

    public Point getPoint2() {
        return p2;
    }

    public int getX1() {
        return p1.x;
    }

    public int getX2() {
        return p2.x;
    }

    public int getY1() {
        return p1.y;
    }

    public int getY2() {
        return p2.y;
    }

    @Override
    public int compareTo(MapleFoothold o) {
        if (p2.y < o.getY1()) {
            return -1;
        } else if (p1.y > o.getY2()) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapleFoothold)) {
            return false;
        }
        MapleFoothold oth = (MapleFoothold) o;
        return oth.getY1() == p1.y && oth.getY2() == p2.y && oth.getX1() == p1.x && oth.getX2() == p2.x && id == oth.getId();
    }

    public int getId() {
        return id;
    }

    public short getNext() {
        return next;
    }

    public void setNext(short next) {
        this.next = next;
    }

    public short getPrev() {
        return prev;
    }

    public void setPrev(short prev) {
        this.prev = prev;
    }
}
