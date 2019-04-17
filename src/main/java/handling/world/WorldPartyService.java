/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.party.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.PartyPacket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author admin
 */
public class WorldPartyService {

    private static final Logger log = LogManager.getLogger(WorldPartyService.class.getName());
    private final Map<Integer, MapleParty> partyList;
    private final Map<Integer, MapleExpedition> expedsList;
    private final Map<PartySearchType, List<PartySearch>> searcheList;
    private final AtomicInteger runningPartyId;
    private final AtomicInteger runningExpedId;
    private final ReentrantReadWriteLock lock;

    private WorldPartyService() {
        log.info("正在启动[WorldPartyService]");
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE characters SET party = -1, fatigue = 0");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            log.error("更新角色组队为-1失败...", e);
        }
        lock = new ReentrantReadWriteLock();
        runningPartyId = new AtomicInteger(1);
        runningExpedId = new AtomicInteger(1);
        partyList = new HashMap<>();
        expedsList = new HashMap<>();
        searcheList = new EnumMap<>(PartySearchType.class);
        for (PartySearchType pst : PartySearchType.values()) {
            searcheList.put(pst, new ArrayList<>()); //according to client, max 10, even though theres page numbers ?!
        }
    }

    public static WorldPartyService getInstance() {
        return SingletonHolder.instance;
    }

    /*
     * 组队聊天
     */
    public void partyChat(int partyId, String chatText, String nameFrom) {
        partyChat(partyId, chatText, nameFrom, 1);
    }

    /*
     * 远征聊天
     */
    public void expedChat(int expedId, String chatText, String nameFrom) {
        MapleExpedition expedition = getExped(expedId);
        if (expedition == null) {
            return;
        }
        for (int i : expedition.getParties()) {
            partyChat(i, chatText, nameFrom, 4);
        }
    }

    /*
     * 发送远征队封包
     */
    public void sendExpedPacket(int expedId, byte[] packet, MaplePartyCharacter exception) {
        MapleExpedition expedition = getExped(expedId);
        if (expedition == null) {
            return;
        }
        for (int i : expedition.getParties()) {
            sendPartyPacket(i, packet, exception);
        }
    }

    /*
     * 发送组队封包
     */
    public void sendPartyPacket(int partyId, byte[] packet, MaplePartyCharacter exception) {
        MapleParty party = getParty(partyId);
        if (party == null) {
            return;
        }
        for (MaplePartyCharacter partychar : party.getMembers()) {
            int ch = WorldFindService.getInstance().findChannel(partychar.getName());
            if (ch > 0 && (exception == null || partychar.getId() != exception.getId())) {
                MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (player != null) {
                    player.getClient().announce(packet);
                }
            }
        }
    }

    /*
     * 组队聊天
     */
    public void partyChat(int partyId, String chatText, String nameFrom, int mode) {
        MapleParty party = getParty(partyId);
        if (party == null) {
            return;
        }
        for (MaplePartyCharacter partychar : party.getMembers()) {
            int ch = WorldFindService.getInstance().findChannel(partychar.getName());
            if (ch > 0) {
                MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (player != null && !player.getName().equalsIgnoreCase(nameFrom)) {
                    player.getClient().announce(MaplePacketCreator.multiChat(nameFrom, chatText, mode));
                    if (player.getClient().isMonitored()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + nameFrom + " said to " + player.getName() + " (组队): " + chatText));
                    }
                }
            }
        }
    }

    /*
     * 发送组队队伍信息
     */
    public void partyMessage(int partyId, String chatText) {
        MapleParty party = getParty(partyId);
        if (party == null) {
            return;
        }
        for (MaplePartyCharacter partychar : party.getMembers()) {
            int ch = WorldFindService.getInstance().findChannel(partychar.getName());
            if (ch > 0) {
                MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(partychar.getName());
                if (player != null) {
                    player.dropMessage(5, chatText);
                }
            }
        }
    }

    /*
     * 发送远征队伍信息
     */
    public void expedMessage(int expedId, String chatText) {
        MapleExpedition expedition = getExped(expedId);
        if (expedition == null) {
            return;
        }
        for (int i : expedition.getParties()) {
            partyMessage(i, chatText);
        }
    }

    /*
     * 更新队伍信息设置信息
     */
    public void updatePartySetup(int partyId, PartyOperation operation, String partyName, boolean isHidden) {
        MapleParty party = getParty(partyId);
        if (party == null) {
            System.out.println("no party with the specified partyid exists.");
            return;
        }
        party.setName(partyName);
        party.setHidden(isHidden);
        if (party.getMembers().size() <= 0) { //当队伍中没有玩家 就解散这个队伍
            disbandParty(partyId);
        }
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar == null) {
                continue;
            }
            int ch = WorldFindService.getInstance().findChannel(partychar.getName());
            if (ch > 0) {
                MapleCharacter chr = getStorage(ch).getCharacterByName(partychar.getName());
                if (chr != null) {
                    chr.setParty(party);
                    chr.send(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, null));
                }
            }
        }
    }

    /*
     * 更新队伍信息
     */
    public void updateParty(int partyId, PartyOperation operation, MaplePartyCharacter target) {
        MapleParty party = getParty(partyId);
        if (party == null) {
            System.out.println("no party with the specified partyid exists.");
            return;
        }
        int oldExped = party.getExpeditionId(); //远征队ID
        int oldIndex = -1; //远征队小组ID
        if (oldExped > 0) {
            MapleExpedition exped = getExped(oldExped);
            if (exped != null) {
                oldIndex = exped.getIndex(partyId);
            }
        }
        switch (operation) {
            case 加入队伍:
                party.addMember(target);
                if (party.getMembers().size() >= 6) {
                    PartySearch toRemove = getSearchByParty(partyId);
                    if (toRemove != null) {
                        removeSearch(toRemove, "队伍人数已满，组队广告已被删除。");
                    } else if (party.getExpeditionId() > 0) {
                        MapleExpedition exped = getExped(party.getExpeditionId());
                        if (exped != null && exped.getAllMembers() >= exped.getType().maxMembers) {
                            toRemove = getSearchByExped(exped.getId());
                            if (toRemove != null) {
                                removeSearch(toRemove, "队伍人数已满，组队广告已被删除。");
                            }
                        }
                    }
                }
                break;
            case 驱逐成员:
            case 离开队伍:
                party.removeMember(target);
                break;
            case 解散队伍:
                disbandParty(partyId);
                break;
            case 更新队伍:
            case LOG_ONOFF:
                party.updateMember(target);
                break;
            case 改变队长:
            case CHANGE_LEADER_DC:
                party.setLeader(target);
                break;
            default:
                throw new RuntimeException("Unhandeled updateParty operation " + operation.name());
        }
        if (operation == PartyOperation.离开队伍 || operation == PartyOperation.驱逐成员) {
            int chz = WorldFindService.getInstance().findChannel(target.getName());
            if (chz > 0) {
                MapleCharacter player = getStorage(chz).getCharacterByName(target.getName());
                if (player != null) {
                    player.setParty(null);
                    if (oldExped > 0) {
                        player.getClient().announce(PartyPacket.expeditionMessage(false));
                    }
                    player.getClient().announce(PartyPacket.updateParty(player.getClient().getChannel(), party, operation, target));
                }
            }
            if (target.getId() == party.getLeader().getId() && party.getMembers().size() > 0) { //pass on lead
                MaplePartyCharacter lchr = null;
                for (MaplePartyCharacter pchr : party.getMembers()) {
                    if (pchr != null && (lchr == null || lchr.getLevel() < pchr.getLevel())) {
                        lchr = pchr;
                    }
                }
                if (lchr != null) {
                    updateParty(partyId, PartyOperation.CHANGE_LEADER_DC, lchr);
                }
            }
        }
        if (party.getMembers().size() <= 0) { //当队伍中没有玩家 就解散这个队伍
            disbandParty(partyId);
        }
        for (MaplePartyCharacter partychar : party.getMembers()) {
            if (partychar == null) {
                continue;
            }
            int ch = WorldFindService.getInstance().findChannel(partychar.getName());
            if (ch > 0) {
                MapleCharacter chr = getStorage(ch).getCharacterByName(partychar.getName());
                if (chr != null) {
                    if (operation == PartyOperation.解散队伍) { //解散远征队伍
                        chr.setParty(null);
                        if (oldExped > 0) {
                            chr.send(PartyPacket.expeditionMessage(true));
                        }
                    } else {
                        chr.setParty(party);
                    }
                    chr.send(PartyPacket.updateParty(chr.getClient().getChannel(), party, operation, target));
                }
            }
        }
        if (oldExped > 0) {
            sendExpedPacket(oldExped, PartyPacket.expeditionUpdate(oldIndex, party), operation == PartyOperation.LOG_ONOFF || operation == PartyOperation.更新队伍 ? target : null);
        }
    }

    /*
     * 创建队伍
     */
    public MapleParty createParty(MaplePartyCharacter chrfor) {
        return createParty(chrfor, "快去组队游戏吧，GoGo", false);
    }

    public MapleParty createParty(MaplePartyCharacter chrfor, String partyName, boolean isHidden) {
        MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, partyName, isHidden);
        partyList.put(party.getPartyId(), party);
        return party;
    }

    /*
     * 创建远征队伍
     */
    public MapleParty createParty(MaplePartyCharacter chrfor, int expedId) {
        ExpeditionType ex = ExpeditionType.getById(expedId);
        MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, ex != null ? runningExpedId.getAndIncrement() : -1);
        partyList.put(party.getPartyId(), party);
        if (ex != null) {
            MapleExpedition expedition = new MapleExpedition(ex, chrfor.getId(), party.getExpeditionId());
            expedition.getParties().add(party.getPartyId());
            expedsList.put(party.getExpeditionId(), expedition);
        }
        return party;
    }

    public MapleParty createPartyAndAdd(MaplePartyCharacter chrfor, int expedId) {
        MapleExpedition expedition = getExped(expedId);
        if (expedition == null) {
            return null;
        }
        MapleParty party = new MapleParty(runningPartyId.getAndIncrement(), chrfor, expedId);
        partyList.put(party.getPartyId(), party);
        expedition.getParties().add(party.getPartyId());
        return party;
    }

    /*
     * 通过组队ID获取队伍信息
     */
    public MapleParty getParty(int partyId) {
        return partyList.get(partyId);
    }

    /*
     * 通过队长ID来获取队伍信息
     */
    public MapleParty getPartyByLeaderId(int leaderId) {
        lock.readLock().lock();
        try {
            Iterator<MapleParty> itr = partyList.values().iterator();
            MapleParty party;
            while (itr.hasNext()) {
                party = itr.next();
                if (party != null && party.getLeader().getId() == leaderId) {
                    return party;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public MapleExpedition getExped(int partyId) {
        return expedsList.get(partyId);
    }

    /*
     * 解散远征队伍
     */
    public MapleExpedition disbandExped(int partyId) {
        PartySearch toRemove = getSearchByExped(partyId);
        if (toRemove != null) {
            removeSearch(toRemove, "远征队解散，组队广告已被删除。");
        }
        MapleExpedition ret = expedsList.remove(partyId);
        if (ret != null) {
            for (int p : ret.getParties()) {
                MapleParty pp = getParty(p);
                if (pp != null) {
                    updateParty(p, PartyOperation.解散队伍, pp.getLeader());
                }
            }
        }
        return ret;
    }

    /*
     * 解散队伍
     */
    public MapleParty disbandParty(int partyId) {
        PartySearch toRemove = getSearchByParty(partyId);
        if (toRemove != null) {
            removeSearch(toRemove, "组队解散，组队广告已被删除。");
        }
        MapleParty ret = partyList.remove(partyId);
        if (ret == null) {
            return null;
        }
        if (ret.getExpeditionId() > 0) {
            MapleExpedition expedition = getExped(ret.getExpeditionId());
            if (expedition != null) {
                int index = expedition.getIndex(partyId);
                if (index >= 0) {
                    expedition.getParties().remove(index);
                    sendExpedPacket(expedition.getId(), PartyPacket.expeditionUpdate(index, null), null);
                }
            }
        }
        ret.disband();
        return ret;
    }

    /*
     * 组队广告
     */
    public List<PartySearch> searchParty(PartySearchType pst) {
        return searcheList.get(pst);
    }

    /*
     * 删除组队广告
     */
    public void removeSearch(PartySearch ps, String text) {
        List<PartySearch> ss = searcheList.get(ps.getType());
        if (ss.contains(ps)) {
            ss.remove(ps);
            ps.cancelRemoval();
            if (ps.getType().exped) {
                expedMessage(ps.getId(), text);
                sendExpedPacket(ps.getId(), PartyPacket.removePartySearch(ps), null);
            } else {
                partyMessage(ps.getId(), text);
                sendPartyPacket(ps.getId(), PartyPacket.removePartySearch(ps), null);
            }
        }
    }

    /*
     * 添加组队广告
     */
    public void addSearch(PartySearch ps) {
        searcheList.get(ps.getType()).add(ps);
    }

    /*
     * 通过队伍信息来获取组队广告
     */
    public PartySearch getSearch(MapleParty party) {
        for (List<PartySearch> ps : searcheList.values()) {
            for (PartySearch p : ps) {
                if ((p.getId() == party.getPartyId() && !p.getType().exped) || (p.getId() == party.getExpeditionId() && p.getType().exped)) {
                    return p;
                }
            }
        }
        return null;
    }

    /*
     * 通过队伍ID来获取组队广告
     */
    public PartySearch getSearchByParty(int partyId) {
        for (List<PartySearch> ps : searcheList.values()) {
            for (PartySearch p : ps) {
                if (p.getId() == partyId && !p.getType().exped) {
                    return p;
                }
            }
        }
        return null;
    }

    /*
     * 通过远征ID来获取组队广告
     */
    public PartySearch getSearchByExped(int partyId) {
        for (List<PartySearch> ps : searcheList.values()) {
            for (PartySearch p : ps) {
                if (p.getId() == partyId && p.getType().exped) {
                    return p;
                }
            }
        }
        return null;
    }

    /*
     * 检测组队是否已经有组队广告
     */
    public boolean partyListed(MapleParty party) {
        return getSearchByParty(party.getPartyId()) != null;
    }

    public PlayerStorage getStorage(int channel) {
        if (channel == -20) {
            return AuctionServer.getPlayerStorage();
        } else if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    private static class SingletonHolder {

        protected static final WorldPartyService instance = new WorldPartyService();
    }
}
