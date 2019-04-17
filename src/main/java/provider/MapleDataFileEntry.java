package provider;

public class MapleDataFileEntry extends MapleDataEntry {

    private int offset;

    public MapleDataFileEntry(String name, int size, int checksum, MapleDataEntity parent) {
        super(name, size, checksum, parent);
    }

    @Override
    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }
}
