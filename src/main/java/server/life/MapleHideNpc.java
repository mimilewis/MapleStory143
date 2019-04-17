package server.life;

public enum MapleHideNpc {

    A(9000069),
    B(9000133),
    C(9310111);
    final int npcId;

    MapleHideNpc(int npcId) {
        this.npcId = npcId;
    }

    public final int getNpcId() {
        return npcId;
    }
}