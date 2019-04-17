package server.maps;

public class ReactorDropEntry {

    public final int itemId;
    public final int chance;
    public final int questid;
    public int assignedRangeStart, assignedRangeLength;

    public ReactorDropEntry(int itemId, int chance, int questid) {
        this.itemId = itemId;
        this.chance = chance;
        this.questid = questid;
    }
}
