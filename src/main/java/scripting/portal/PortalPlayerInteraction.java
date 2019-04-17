package scripting.portal;

import client.MapleClient;
import scripting.AbstractPlayerInteraction;
import server.MaplePortal;

public class PortalPlayerInteraction extends AbstractPlayerInteraction {

    private final MaplePortal portal;

    /**
     * @param c
     * @param portal
     */
    public PortalPlayerInteraction(MapleClient c, MaplePortal portal) {
        super(c, portal.getId(), String.valueOf(c.getPlayer().getMapId()));
        this.portal = portal;
    }

    /**
     * @return
     */
    public MaplePortal getPortal() {
        return portal;
    }

    public String getPortalName() {
        return portal.getName();
    }


    public void inFreeMarket() {
        if (getMapId() != 910000000) {
            if (getPlayer().getLevel() > 10) {
                saveLocation("FREE_MARKET");
                playPortalSE();
                warp(910000000, "st00");
            } else {
                playerMessage(5, "你必须10级以上才能进入自由市场。");
            }
        }
    }


    public void inArdentmill() {
        if (getMapId() != 910001000) {
            if (getPlayer().getLevel() >= 10) {
                saveLocation("ARDENTMILL");
                playPortalSE();
                warp(910001000, "st00");
            } else {
                playerMessage(5, "你必须10级以上才能进入匠人街。");
            }
        }
    }

    // summon one monster on reactor location

    /**
     * @param id
     */
    @Override
    public void spawnMonster(int id) {
        spawnMonster(id, 1, portal.getPosition());
    }

    // summon monsters on reactor location

    /**
     * @param id
     * @param qty
     */
    @Override
    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, portal.getPosition());
    }
}
