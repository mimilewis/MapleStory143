package server.life;

public class MonsterDropEntry {

    public int itemId, chance, minimum, maximum, questid;

    public MonsterDropEntry() {

    }

    public MonsterDropEntry(int itemId, int chance, int Minimum, int Maximum, int questid) {
        this.itemId = itemId;
        this.chance = chance;
        this.questid = questid;
        this.minimum = Minimum;
        this.maximum = Maximum;
    }
}
