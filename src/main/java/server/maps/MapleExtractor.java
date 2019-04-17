package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import tools.MaplePacketCreator;

public class MapleExtractor extends MapleMapObject {

    public final int owner;
    public final int timeLeft;
    public final int itemId;
    public final int fee;
    public final long startTime;
    public final String ownerName;

    public MapleExtractor(MapleCharacter owner, int itemId, int fee, int timeLeft) {
        super();
        this.owner = owner.getId();
        this.itemId = itemId;
        this.fee = fee;
        this.ownerName = owner.getName();
        this.startTime = System.currentTimeMillis();
        this.timeLeft = timeLeft;
        setPosition(owner.getPosition());
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(MaplePacketCreator.makeExtractor(owner, ownerName, getTruePosition(), getTimeLeft(), itemId, fee));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removeExtractor(this.owner));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.EXTRACTOR;
    }
}
