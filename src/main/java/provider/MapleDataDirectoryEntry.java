package provider;

import java.util.*;

/**
 * @author Matze
 */
public class MapleDataDirectoryEntry extends MapleDataEntry {

    private final List<MapleDataDirectoryEntry> subdirs = new ArrayList<>();
    private final List<MapleDataFileEntry> files = new ArrayList<>();
    private final Map<String, MapleDataEntry> entries = new HashMap<>();

    public MapleDataDirectoryEntry(String name, int size, int checksum, MapleDataEntity parent) {
        super(name, size, checksum, parent);
    }

    public MapleDataDirectoryEntry() {
        super(null, 0, 0, null);
    }

    public void addDirectory(MapleDataDirectoryEntry dir) {
        subdirs.add(dir);
        entries.put(dir.getName(), dir);
    }

    public void addFile(MapleDataFileEntry fileEntry) {
        files.add(fileEntry);
        entries.put(fileEntry.getName(), fileEntry);
    }

    public List<MapleDataDirectoryEntry> getSubdirectories() {
        return Collections.unmodifiableList(subdirs);
    }

    public List<MapleDataFileEntry> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public MapleDataEntry getEntry(String name) {
        return entries.get(name);
    }
}
