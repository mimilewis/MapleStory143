package handling.world.party;

import client.MapleCharacter;
import server.MapleStatEffect;

import java.io.Serializable;
import java.util.*;

/*
 * 组队
 */
public class MapleParty implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final List<MaplePartyCharacter> members = new LinkedList<>();
    private MaplePartyCharacter leader;
    private int partyId, expeditionLink = -1;
    private boolean disbanded = false;
    private String name = ""; //队伍的名字
    private boolean hidden = false; //是否隐藏队伍信息
    private Map<Integer, Map<Integer, List<Integer>>> partybuffs = new HashMap<>();

    public MapleParty() {
    }

    public MapleParty(int partyId, MaplePartyCharacter chrfor, String partyName, boolean isHidden) {
        this.leader = chrfor;
        this.members.add(this.leader);
        this.partyId = partyId;
        this.name = partyName;
        this.hidden = isHidden;
    }

    public MapleParty(int partyId, MaplePartyCharacter chrfor, int expeditionLink) {
        this.leader = chrfor;
        this.members.add(this.leader);
        this.partyId = partyId;
        this.expeditionLink = expeditionLink;
        this.name = "";
        this.hidden = false;
    }

    /*
     * 是否在队伍中有这个角色
     */
    public boolean containsMembers(MaplePartyCharacter member) {
        return members.contains(member);
    }

    /*
     * 添加队伍成员
     */
    public void addMember(MaplePartyCharacter member) {
        members.add(member);
    }

    /*
     * 移除队伍成员
     */
    public void removeMember(MaplePartyCharacter member) {
        members.remove(member);
    }

    /*
     * 更新队伍成员
     */
    public void updateMember(MaplePartyCharacter member) {
        for (int i = 0; i < members.size(); i++) {
            MaplePartyCharacter chr = members.get(i);
            if (chr.equals(member)) {
                members.set(i, member);
            }
        }
    }

    /*
     * 通过角色ID获取1个队伍成员的信息
     */
    public MaplePartyCharacter getMemberById(int chrId) {
        for (MaplePartyCharacter chr : members) {
            if (chr.getId() == chrId) {
                return chr;
            }
        }
        return null;
    }

    /*
     * 通过位置获取队伍成员信息
     */
    public MaplePartyCharacter getMemberByIndex(int index) {
        return members.get(index);
    }

    /*
     * 队伍成员全部信息
     */
    public Collection<MaplePartyCharacter> getMembers() {
        return new LinkedList<>(members);
    }

    /*
     * 队伍的ID
     */
    public int getPartyId() {
        return partyId;
    }

    /*
     * 设置队伍的ID
     */
    public void setPartyId(int id) {
        this.partyId = id;
    }

    /*
     * 获取队长信息
     */
    public MaplePartyCharacter getLeader() {
        return leader;
    }

    /*
     * 设置新的队长
     */
    public void setLeader(MaplePartyCharacter nLeader) {
        leader = nLeader;
    }

    /*
     * 队伍在远征中的队伍ID
     */
    public int getExpeditionId() {
        return expeditionLink;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + partyId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MapleParty other = (MapleParty) obj;
        return partyId == other.partyId;
    }

    /*
     * 队伍是否解散
     */
    public boolean isDisbanded() {
        return disbanded;
    }

    /*
     * 解散队伍
     */
    public void disband() {
        this.disbanded = true;
    }

    /*
     * 队伍的名字
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /*
     * 队伍是否隐藏
     */
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public int getAverageLevel() {
        int n2 = 0;
        for (MaplePartyCharacter d2 : members) {
            n2 += d2.getLevel();
        }
        return n2 / members.size();
    }

    public void givePartyBuff(int n2, int n3, int n4) {
        if (partybuffs.containsKey(n2)) {
            if (partybuffs.get(n2).containsKey(n3)) {
                if (!partybuffs.get(n2).keySet().isEmpty()) {
                    for (Integer integer : partybuffs.get(n2).keySet()) {
                        if (partybuffs.get(n2).get(integer).contains(n4)) {
                            partybuffs.get(n2).get(integer).remove(partybuffs.get(n2).get(integer).indexOf(n4));
                        }
                        if (partybuffs.get(n2).get(integer).isEmpty()) {
                            partybuffs.get(n2).remove(integer);
                        }
                    }
                }
                if (!(partybuffs == null || partybuffs.get(n2).isEmpty() || partybuffs.get(n2).get(n3).isEmpty() || partybuffs.get(n2).get(n3).contains(n4))) {
                    partybuffs.get(n2).get(n3).add(n4);
                }
            } else {
                ArrayList<Integer> integers = new ArrayList<>();
                integers.add(n4);
                partybuffs.get(n2).put(n3, integers);
            }
        } else {
            HashMap<Integer, List<Integer>> listHashMap = new HashMap<>();
            ArrayList<Integer> integers = new ArrayList<>();
            integers.add(n4);
            listHashMap.put(n3, integers);
            partybuffs.put(n2, listHashMap);
        }
    }

    public int getPartyBuffs(int n2) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        for (Map<Integer, List<Integer>> integerListMap : partybuffs.values()) {
            if (integerListMap.containsKey(n2)) {
                for (List<Integer> integers : integerListMap.values()) {
                    for (Integer integer : integers) {
                        if (!arrayList.contains(integer)) {
                            arrayList.add(integer);
                        }
                    }
                }
            }
        }
        return arrayList.size();
    }

    public int cancelPartyBuff(int n2, int n3) {
        if (partybuffs.containsKey(n2)) {
            if (partybuffs.get(n2).isEmpty()) {
                partybuffs.remove(n2);
            } else {
                for (Integer n4 : (partybuffs.get(n2)).keySet()) {
                    if (partybuffs.get(n2).get(n4).isEmpty()) {
                        partybuffs.get(n2).remove(n4);
                        continue;
                    }
                    if (partybuffs.get(n2).get(n4).contains(n3)) {
                        partybuffs.get(n2).get(n4).remove(partybuffs.get(n2).get(n4).indexOf(n3));
                        return n4;
                    }
                }
            }
        }
        return -1;
    }

    public void cancelAllPartyBuffsByChr(int n2) {
        if (partybuffs.isEmpty()) {
            return;
        }
        try {
            for (Integer n3 : partybuffs.keySet()) {
                if (partybuffs.get(n3).isEmpty()) {
                    partybuffs.remove(n3);
                    continue;
                }
                for (Integer n4 : partybuffs.get(n3).keySet()) {
                    MapleCharacter p2;
                    if (partybuffs.get(n3).get(n4).isEmpty() || n4 == n2) {
                        partybuffs.get(n3).remove(n4);
                        p2 = MapleCharacter.getOnlineCharacterById(n4);
                        if (n4 == n2 && p2 != null) {
                            MapleStatEffect.apply祈祷众生(p2);
                        }
                        continue;
                    }
                    if (partybuffs.get(n3).get(n4).contains(n2)) {
                        partybuffs.get(n3).get(n4).remove(partybuffs.get(n3).get(n4).indexOf(n2));
                        p2 = MapleCharacter.getOnlineCharacterById(n4);
                        if (p2 != null) {
                            MapleStatEffect.apply祈祷众生(p2);
                        }
                    }
                }
            }
        } catch (Exception exception) {
            // empty catch block
        }
    }
}
