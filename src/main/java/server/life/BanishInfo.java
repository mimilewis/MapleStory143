package server.life;

public class BanishInfo {

    private final int map;
    private final String portal;
    private final String msg;

    public BanishInfo(String msg, int map, String portal) {
        this.msg = msg;
        this.map = map;
        this.portal = portal;
    }

    public int getMap() {
        return map;
    }

    public String getPortal() {
        return portal;
    }

    public String getMsg() {
        return msg;
    }
}
