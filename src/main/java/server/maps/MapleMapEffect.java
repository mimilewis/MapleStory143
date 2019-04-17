package server.maps;

import client.MapleClient;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;

public class MapleMapEffect {

    private String msg = "";
    private int itemId = 0, effectType = -1;
    private boolean active = true;
    private boolean jukebox = false;

    public MapleMapEffect(String msg, int itemId) {
        this.msg = msg;
        this.itemId = itemId;
        this.effectType = -1;
    }

    public MapleMapEffect(String msg, int itemId, int effectType) {
        this.msg = msg;
        this.itemId = itemId;
        this.effectType = effectType;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isJukebox() {
        return this.jukebox;
    }

    public void setJukebox(boolean actie) {
        this.jukebox = actie;
    }

    public byte[] makeDestroyData() {
        return jukebox ? MTSCSPacket.INSTANCE.playCashSong(0, "") : MaplePacketCreator.removeMapEffect();
    }

    public byte[] makeStartData() {
        return jukebox ? MTSCSPacket.INSTANCE.playCashSong(itemId, msg) : MaplePacketCreator.startMapEffect(msg, itemId, effectType, active);
    }

    public void sendStartData(MapleClient c) {
        c.announce(makeStartData());
    }
}
