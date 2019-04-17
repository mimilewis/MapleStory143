package server.squad;

import client.MapleCharacter;
import constants.JobConstants;
import handling.channel.ChannelServer;
import handling.world.WorldFindService;
import server.Timer.EtcTimer;
import server.maps.MapleMap;
import tools.MaplePacketCreator;
import tools.Pair;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;

public class MapleSquad {

    private final String leaderName;
    private final String toSay;
    private final Map<String, String> members = new LinkedHashMap<>(); //[角色名字] [职业类型]
    private final Map<String, String> bannedMembers = new LinkedHashMap<>();
    private final int ch;
    private final long startTime;
    private final int expiration;
    private final int beginMapId;
    private final MapleSquadType type;
    private WeakReference<MapleCharacter> leader;
    private byte status = 0;
    private ScheduledFuture<?> removal;

    /*
     * 新建远征队
     * 频道
     * 类型
     * 队长
     * 时间
     * 广告提示
     */
    public MapleSquad(int ch, String type, MapleCharacter leader, int expiration, String toSay) {
        this.leader = new WeakReference<>(leader);
        this.members.put(leader.getName(), JobConstants.getJobBasicNameById(leader.getJob()));
        this.leaderName = leader.getName();
        this.ch = ch; //远征队所在频道
        this.toSay = toSay; //广告提示
        this.type = MapleSquadType.valueOf(type.toLowerCase());
        this.status = 1; //远征队的状态
        this.beginMapId = leader.getMapId(); //开始远征的地图 也就是准备的地图
        leader.getMap().setSquad(this.type);
        if (this.type.queue.get(ch) == null) {
            this.type.queue.put(ch, new ArrayList<>());
            this.type.queuedPlayers.put(ch, new ArrayList<>());
        }
        this.startTime = System.currentTimeMillis();
        this.expiration = expiration;
    }

    public void copy() {
        while (type.queue.get(ch).size() > 0 && ChannelServer.getInstance(ch).getMapleSquad(type) == null) {
            int index = 0;
            long lowest = 0;
            for (int i = 0; i < type.queue.get(ch).size(); i++) {
                if (lowest == 0 || type.queue.get(ch).get(i).right < lowest) {
                    index = i;
                    lowest = type.queue.get(ch).get(i).right;
                }
            }
            String nextPlayerId = type.queue.get(ch).remove(index).left;
            int theirCh = WorldFindService.getInstance().findChannel(nextPlayerId);
            if (theirCh > 0) {
                MapleCharacter lead = ChannelServer.getInstance(theirCh).getPlayerStorage().getCharacterByName(nextPlayerId);
                if (lead != null && lead.getMapId() == beginMapId && lead.getClient().getChannel() == ch) {
                    MapleSquad squad = new MapleSquad(ch, type.name(), lead, expiration, toSay);
                    if (ChannelServer.getInstance(ch).addMapleSquad(squad, type.name())) {
                        getBeginMap().broadcastMessage(MaplePacketCreator.getClock(expiration / 1000));
                        getBeginMap().broadcastMessage(MaplePacketCreator.serverNotice(6, nextPlayerId + toSay));
                        type.queuedPlayers.get(ch).add(new Pair<>(nextPlayerId, "Success"));
                    } else {
                        squad.clear();
                        type.queuedPlayers.get(ch).add(new Pair<>(nextPlayerId, "Skipped"));
                    }
                    break;
                } else {
                    if (lead != null) {
                        lead.dropMessage(6, "Your squad has been skipped due to you not being in the right channel and map.");
                    }
                    getBeginMap().broadcastMessage(MaplePacketCreator.serverNotice(6, nextPlayerId + "'s squad has been skipped due to the player not being in the right channel and map."));
                    type.queuedPlayers.get(ch).add(new Pair<>(nextPlayerId, "Not in map"));
                }
            } else {
                getBeginMap().broadcastMessage(MaplePacketCreator.serverNotice(6, nextPlayerId + "'s squad has been skipped due to the player not being online."));
                type.queuedPlayers.get(ch).add(new Pair<>(nextPlayerId, "Not online"));
            }
        }
    }

    public MapleMap getBeginMap() {
        return ChannelServer.getInstance(ch).getMapFactory().getMap(beginMapId);
    }

    public void clear() {
        if (removal != null) {
            getBeginMap().broadcastMessage(MaplePacketCreator.stopClock());
            removal.cancel(false);
            removal = null;
        }
        members.clear();
        bannedMembers.clear();
        leader = null;
        ChannelServer.getInstance(ch).removeMapleSquad(type);
        this.status = 0;
    }

    public MapleCharacter getChar(String name) {
        return ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(name);
    }

    public long getTimeLeft() {
        return expiration - (System.currentTimeMillis() - startTime);
    }

    public void scheduleRemoval() {
        removal = EtcTimer.getInstance().schedule(() -> {
            if (status != 0 && leader != null && (getLeader() == null || status == 1)) { //leader itself = null means we're already cleared
                clear();
                copy();
            }
        }, expiration);
    }

    public String getLeaderName() {
        return leaderName;
    }

    /*
     * 等待中的成员信息
     */
    public List<Pair<String, Long>> getAllNextPlayer() {
        return type.queue.get(ch);
    }

    public boolean containsNextPlayer(MapleCharacter player) {
        for (Pair<String, Long> names : type.queue.get(ch)) {
            if (names.left.equalsIgnoreCase(player.getName())) {
                return true;
            }
        }
        return false;
    }

    public String getNextPlayer() {
        StringBuilder sb = new StringBuilder("\n远征人数 : ");
        sb.append("#b").append(type.queue.get(ch).size()).append(" #k ").append("远征队成员信息 : \n\r ");
        int i = 0;
        for (Pair<String, Long> chr : type.queue.get(ch)) {
            i++;
            sb.append(i).append(" : ").append(chr.left);
            sb.append(" \n\r ");
        }
        sb.append("Would you like to #ebe next#n in the queue, or #ebe removed#n from the queue if you are in it?");
        return sb.toString();
    }

    public void setNextPlayer(String chr) {
        Pair<String, Long> toRemove = null;
        for (Pair<String, Long> s : type.queue.get(ch)) {
            if (s.left.equals(chr)) {
                toRemove = s;
                break;
            }
        }
        if (toRemove != null) {
            type.queue.get(ch).remove(toRemove);
            return;
        }
        for (ArrayList<Pair<String, Long>> v : type.queue.values()) {
            for (Pair<String, Long> s : v) {
                if (s.left.equals(chr)) {
                    return;
                }
            }
        }
        type.queue.get(ch).add(new Pair<>(chr, System.currentTimeMillis()));
    }

    public MapleCharacter getLeader() {
        if (leader == null || leader.get() == null) {
            if (members.size() > 0 && getChar(leaderName) != null) {
                leader = new WeakReference<>(getChar(leaderName));
            } else {
                if (status != 0) {
                    clear();
                }
                return null;
            }
        }
        return leader.get();
    }

    public boolean containsMember(MapleCharacter member) {
        for (String mmbr : members.keySet()) {
            if (mmbr.equalsIgnoreCase(member.getName())) {
                return true;
            }
        }
        return false;
    }

    public List<String> getMembers() {
        return new LinkedList<>(members.keySet());
    }

    public List<String> getBannedMembers() {
        return new LinkedList<>(bannedMembers.keySet());
    }

    public int getSquadSize() {
        return members.size();
    }

    public boolean isBanned(MapleCharacter member) {
        return bannedMembers.containsKey(member.getName());
    }

    public int addMember(MapleCharacter member, boolean join) {
        if (getLeader() == null) {
            return -1;
        }
        String job = JobConstants.getJobBasicNameById(member.getJob());
        if (join) {
            if (!containsMember(member) && !containsNextPlayer(member)) {
                if (members.size() <= 30) {
                    members.put(member.getName(), job);
                    getLeader().dropMessage(5, member.getName() + " (" + job + ") 加入了远征队.");
                    return 1;
                }
                return 2;
            }
            return -1;
        } else {
            if (containsMember(member)) {
                members.remove(member.getName());
                getLeader().dropMessage(5, member.getName() + " (" + job + ") 离开了远征队.");
                return 1;
            }
            return -1;
        }
    }

    public void acceptMember(int pos) {
        if (pos < 0 || pos >= bannedMembers.size()) {
            return;
        }
        List<String> membersAsList = getBannedMembers();
        String toadd = membersAsList.get(pos);
        if (toadd != null && getChar(toadd) != null) {
            members.put(toadd, bannedMembers.get(toadd));
            bannedMembers.remove(toadd);
            getChar(toadd).dropMessage(5, getLeaderName() + " 将你列为远征队队员.");
        }
    }

    public void reAddMember(MapleCharacter chr) {
        removeMember(chr);
        members.put(chr.getName(), JobConstants.getJobBasicNameById(chr.getJob()));
    }

    public void removeMember(MapleCharacter chr) {
        if (members.containsKey(chr.getName())) {
            members.remove(chr.getName());
        }
    }

    public void removeMember(String chr) {
        if (members.containsKey(chr)) {
            members.remove(chr);
        }
    }

    public void banMember(int pos) {
        if (pos <= 0 || pos >= members.size()) { //may not ban leader
            return;
        }
        List<String> membersAsList = getMembers();
        String toban = membersAsList.get(pos);
        if (toban != null && getChar(toban) != null) {
            bannedMembers.put(toban, members.get(toban));
            members.remove(toban);
            getChar(toban).dropMessage(5, getLeaderName() + " 将你请出远征队，目前无法加入远征队.");
        }
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
        if (status == 2 && removal != null) {
            removal.cancel(false);
            removal = null;
        }
    }

    public int getBannedMemberSize() {
        return bannedMembers.size();
    }

    public String getSquadMemberString(byte type) {
        switch (type) {
            case 0: {
                StringBuilder sb = new StringBuilder("总共 : ");
                sb.append("#b").append(members.size()).append(" #k ").append("个远征队成员 : \n\r ");
                int i = 0;
                for (Entry<String, String> chr : members.entrySet()) {
                    i++;
                    sb.append(i).append(" : ").append(chr.getKey()).append(" (").append(chr.getValue()).append(") ");
                    if (i == 1) {
                        sb.append("(远征队队长)");
                    }
                    sb.append(" \n\r ");
                }
                while (i < 30) {
                    i++;
                    sb.append(i).append(" : ").append(" \n\r ");
                }
                return sb.toString();
            }
            case 1: {
                StringBuilder sb = new StringBuilder("总共 : ");
                sb.append("#b").append(members.size()).append(" #k ").append("个远征队成员 : \n\r ");
                int i = 0, selection = 0;
                for (Entry<String, String> chr : members.entrySet()) {
                    i++;
                    sb.append("#b#L").append(selection).append("#");
                    selection++;
                    sb.append(i).append(" : ").append(chr.getKey()).append(" (").append(chr.getValue()).append(") ");
                    if (i == 1) {
                        sb.append("(远征队队长)");
                    }
                    sb.append("#l").append(" \n\r ");
                }
                while (i < 30) {
                    i++;
                    sb.append(i).append(" : ").append(" \n\r ");
                }
                return sb.toString();
            }
            case 2: {
                StringBuilder sb = new StringBuilder("总共 : ");
                sb.append("#b").append(members.size()).append(" #k ").append("个远征队成员 : \n\r ");
                int i = 0, selection = 0;
                for (Entry<String, String> chr : bannedMembers.entrySet()) {
                    i++;
                    sb.append("#b#L").append(selection).append("#");
                    selection++;
                    sb.append(i).append(" : ").append(chr.getKey()).append(" (").append(chr.getValue()).append(") ");
                    sb.append("#l").append(" \n\r ");
                }
                while (i < 30) {
                    i++;
                    sb.append(i).append(" : ").append(" \n\r ");
                }
                return sb.toString();
            }
            case 3: { //CWKPQ
                StringBuilder sb = new StringBuilder("Jobs : ");
                Map<String, Integer> jobs = getJobs();
                for (Entry<String, Integer> chr : jobs.entrySet()) {
                    sb.append("\r\n").append(chr.getKey()).append(" : ").append(chr.getValue());
                }
                return sb.toString();
            }
        }
        return null;
    }

    public MapleSquadType getType() {
        return type;
    }

    public Map<String, Integer> getJobs() {
        Map<String, Integer> jobs = new LinkedHashMap<>();
        for (Entry<String, String> chr : members.entrySet()) {
            if (jobs.containsKey(chr.getValue())) {
                jobs.put(chr.getValue(), jobs.get(chr.getValue()) + 1);
            } else {
                jobs.put(chr.getValue(), 1);
            }
        }
        return jobs;
    }
}
