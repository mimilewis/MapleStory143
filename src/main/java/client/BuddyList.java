package client;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import database.DatabaseConnection;
import tools.packet.BuddyListPacket;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuddyList implements Serializable {

    private static final long serialVersionUID = 1413738569L;
    private final Map<Integer, BuddylistEntry> buddies = new LinkedHashMap<>();
    private byte capacity;
    private boolean changed = false;

    @JsonCreator
    public BuddyList(@JsonProperty("capacity") byte capacity) {
        this.capacity = capacity;
    }

    public boolean contains(int characterId) {
        return buddies.containsKey(characterId);
    }

    public boolean containsVisible(int characterId) {
        BuddylistEntry ble = buddies.get(characterId);
        return ble != null && ble.isVisible();
    }

    public byte getCapacity() {
        return capacity;
    }

    public void setCapacity(byte capacity) {
        this.capacity = capacity;
    }

    public BuddylistEntry get(int characterId) {
        return buddies.get(characterId);
    }

    public BuddylistEntry get(String characterName) {
        String lowerCaseName = characterName.toLowerCase();
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.getName().toLowerCase().equals(lowerCaseName)) {
                return ble;
            }
        }
        return null;
    }

    public void put(BuddylistEntry entry) {
        buddies.put(entry.getCharacterId(), entry);
        changed = true;
    }

    public void remove(int characterId) {
        buddies.remove(characterId);
        changed = true;
    }

    public Collection<BuddylistEntry> getBuddies() {
        return buddies.values();
    }

    public boolean isFull() {
        return buddies.size() >= capacity;
    }

    public int[] getBuddyIds() {
        int buddyIds[] = new int[buddies.size()];
        int i = 0;
        for (BuddylistEntry ble : buddies.values()) {
            if (ble.isVisible()) {
                buddyIds[i++] = ble.getCharacterId();
            }
        }
        return buddyIds;
    }

    public void loadFromTransfer(List<CharacterNameAndId> data) {
        for (CharacterNameAndId qs : data) {
            put(new BuddylistEntry(qs.getName(), qs.getId(), qs.getGroup(), -1, qs.isVisible()));
        }
    }

    public void loadFromDb(int characterId) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT b.buddyid, b.pending, c.name AS buddyname, b.groupname FROM buddies AS b, characters AS c WHERE c.id = b.buddyid AND b.characterid = ?")) {
                ps.setInt(1, characterId);
                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        put(new BuddylistEntry(rs.getString("buddyname"), rs.getInt("buddyid"), rs.getString("groupname"), -1, rs.getInt("pending") != 1));
                    }
                }
            }
        }
    }

    public void addBuddyRequest(MapleClient c, int cidFrom, String nameFrom, int channelFrom, int levelFrom, int jobFrom) {
        put(new BuddylistEntry(nameFrom, cidFrom, "未指定群组", channelFrom, false));
        c.announce(BuddyListPacket.requestBuddylistAdd(cidFrom, nameFrom, channelFrom, levelFrom, jobFrom));
    }

    public void setChanged(boolean v) {
        this.changed = v;
    }

    public boolean changed() {
        return changed;
    }

    public enum BuddyOperation {

        添加好友, 删除好友
    }

    public enum BuddyAddResult {

        好友列表已满, 已经是好友关系, 添加好友成功
    }
}
