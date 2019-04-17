package server.life;

import client.MapleClient;
import constants.GameConstants;
import server.maps.MapleMapObjectType;
import server.shop.MapleShopFactory;
import tools.packet.NPCPacket;

public class MapleNPC extends AbstractLoadedMapleLife {

    private final int mapid;
    private String name = "MISSINGNO";
    private boolean custom = false;
    private int ownerid = 0;

    public MapleNPC(int id, String name, int mapid) {
        super(id);
        this.name = name;
        this.mapid = mapid;
    }

    public boolean hasShop() {
        return MapleShopFactory.getInstance().getShopForNPC(getId()) != null;
    }

    public void sendShop(MapleClient c) {
        MapleShopFactory.getInstance().getShopForNPC(getId()).sendShop(c);
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        if (getId() < 9901000 && getId() != 9000069 && getId() != 9000133) {
            client.announce(NPCPacket.spawnNPC(this));
            client.announce(NPCPacket.spawnNPCRequestController(this, true));
        }
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(NPCPacket.removeNPC(getObjectId()));
        if (!isHidden() && !GameConstants.isHideNpc(client.getPlayer().getMapId(), getId())) {
            client.announce(NPCPacket.removeNPCController(getObjectId(), false));
        }
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.NPC;
    }

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public boolean isCustom() {
        return custom;
    }

    public void setCustom(boolean custom) {
        this.custom = custom;
    }

    public int getMapid() {
        return mapid;
    }

    public int getOwnerid() {
        return ownerid;
    }

    public void setOwnerid(int ownerid) {
        this.ownerid = ownerid;
    }
}
