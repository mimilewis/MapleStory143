package server.life;

public class MonsterGlobalDropEntry {

    public byte dropType;
    public int itemId, chance, Minimum, Maximum, continent, questid;
    public boolean onlySelf = false;

    public MonsterGlobalDropEntry() {
    }

    public MonsterGlobalDropEntry(int itemId, int chance, int continent, byte dropType, int Minimum, int Maximum, int questid) {
        this.itemId = itemId;
        this.chance = chance;
        this.dropType = dropType;
        this.continent = continent;
        this.questid = questid;
        this.Minimum = Minimum;
        this.Maximum = Maximum;
    }

    public MonsterGlobalDropEntry(int itemId, int chance, int continent, byte dropType, int Minimum, int Maximum, int questid, boolean onlySelf) {
        this.itemId = itemId;
        this.chance = chance;
        this.dropType = dropType;
        this.continent = continent;
        this.questid = questid;
        this.Minimum = Minimum;
        this.Maximum = Maximum;
        this.onlySelf = onlySelf;
    }
}
