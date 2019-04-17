/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.PlayerStorage;
import handling.world.guild.MapleBBSThread;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.Pair;
import tools.packet.GuildPacket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author PlayDK
 */
public class WorldGuildService {

    private static final Logger log = LogManager.getLogger(WorldGuildService.class.getName());
    private final Map<Integer, MapleGuild> guildList;
    private final ReentrantReadWriteLock lock;

    private WorldGuildService() {
        log.info("正在启动[WorldGuildService]");
        lock = new ReentrantReadWriteLock();
        guildList = new LinkedHashMap<>();
    }

    public static WorldGuildService getInstance() {
        return SingletonHolder.instance;
    }

    public void addLoadedGuild(MapleGuild guild) {
        if (guild.isProper()) {
            guildList.put(guild.getId(), guild);
        }
    }

    /*
     * 创建1个新的家族
     */
    public int createGuild(int leaderId, String name) {
        return MapleGuild.createGuild(leaderId, name);
    }

    public MapleGuild getGuild(int guildId) {
        MapleGuild ret = null;
        lock.readLock().lock();
        try {
            ret = guildList.get(guildId);
        } finally {
            lock.readLock().unlock();
        }
        if (ret == null) {
            lock.writeLock().lock();
            try {
                ret = new MapleGuild(guildId);
                if (ret == null || ret.getId() <= 0 || !ret.isProper()) { //failed to load
                    return null;
                }
                guildList.put(guildId, ret);
            } finally {
                lock.writeLock().unlock();
            }
        }
        return ret; //Guild doesn't exist?
    }

    public MapleGuild getGuildByName(String guildName) {
        lock.readLock().lock();
        try {
            for (MapleGuild guild : guildList.values()) {
                if (guild.getName().equalsIgnoreCase(guildName)) {
                    return guild;
                }
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public MapleGuild getGuild(MapleCharacter chr) {
        return getGuild(chr.getGuildId());
    }

    /*
     * 更新家族成员在线信息
     */
    public void setGuildMemberOnline(MapleGuildCharacter guildMember, boolean isOnline, int channel) {
        MapleGuild guild = getGuild(guildMember.getGuildId());
        if (guild != null) {
            guild.setOnline(guildMember.getId(), isOnline, channel);
        }
    }

    /*
     * 给家族所有成员发送封包信息
     */
    public void guildPacket(int guildId, byte[] message) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.broadcast(message);
        }
    }

    /*
     * 添加新的家族成员
     */
    public int addGuildMember(MapleGuildCharacter guildMember) {
        MapleGuild guild = getGuild(guildMember.getGuildId());
        if (guild != null) {
            return guild.addGuildMember(guildMember);
        }
        return 0;
    }

    public int addGuildMember(int guildId, int chrId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.addGuildMember(chrId);
        }
        return 0;
    }

    /*
     * 添加新的家族申请列表
     */
    public int addGuildApplyMember(MapleGuildCharacter guildMember) {
        MapleGuild guild = getGuild(guildMember.getGuildId());
        if (guild != null) {
            return guild.addGuildApplyMember(guildMember);
        }
        return 0;
    }

    /*
     * 拒绝角色的家族申请
     */
    public int denyGuildApplyMember(int guildId, int chrId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            //删除家族申请信息
            guild.denyGuildApplyMember(chrId);
            int ch = WorldFindService.getInstance().findChannel(chrId);
            if (ch < 0) {
                return 0;
            }
            MapleCharacter player = getStorage(ch).getCharacterById(chrId);
            if (player == null) {
                return 0;
            }
            player.getClient().announce(GuildPacket.DenyGuildApply(chrId));
            return 1;
        }
        return 0;
    }

    /*
     * 玩家自己离开家族
     */
    public void leaveGuild(MapleGuildCharacter guildMember) {
        MapleGuild guild = getGuild(guildMember.getGuildId());
        if (guild != null) {
            guild.leaveGuild(guildMember);
        }
    }

    /**
     * 家族聊天
     *
     * @param accId   发送者账号ID
     * @param guildId 发送者所属家族ID
     * @param chrId   发送角色ID
     * @param msg     具体消息内容
     */
    public void guildChat(int accId, int guildId, int chrId, String msg) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.guildChat(accId, guildId, chrId, msg);
        }
    }

    /*
     * 家族职位称号变更
     */
    public void changeRank(int guildId, int chrId, int newRank) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.changeRank(chrId, newRank);
        }
    }

    /*
     * 驱逐成员
     */
    public void expelMember(MapleGuildCharacter initiator, String name, int chrId) {
        MapleGuild guild = getGuild(initiator.getGuildId());
        if (guild != null) {
            guild.expelMember(initiator, name, chrId);
        }
    }

    /*
     * 修改家族公告
     */
    public void setGuildNotice(int guildId, String notice) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.setGuildNotice(notice);
        }
    }

    /*
     * 修改家族族长
     */
    public void setGuildLeader(int guildId, int chrId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.changeGuildLeader(chrId);
        }
    }

    /*
     * 获取家族指定技能的等级
     */
    public int getSkillLevel(int guildId, int skillId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.getSkillLevel(skillId);
        }
        return 0;
    }

    /*
     * 购买家族技能
     */
    public boolean purchaseSkill(int guildId, int skillId, String name, int chrId) {
        MapleGuild guild = getGuild(guildId);
        return guild != null && guild.purchaseSkill(skillId, name, chrId);
    }

    /*
     * 激活家族技能
     */
    public boolean activateSkill(int guildId, int skillId, String name) {
        MapleGuild guild = getGuild(guildId);
        return guild != null && guild.activateSkill(skillId, name);
    }

    /*
     * 更新家族成员 升级 或者 改变职业
     */
    public void memberLevelJobUpdate(MapleGuildCharacter guildMember) {
        MapleGuild guild = getGuild(guildMember.getGuildId());
        if (guild != null) {
            guild.memberLevelJobUpdate(guildMember);
        }
    }

    /*
     * 家族成员职位变更
     */
    public void changeRankTitle(int guildId, String[] ranks) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.changeRankTitle(ranks);
        }
    }

    /*
     * 家族头像变更
     */
    public void setGuildEmblem(int guildId, short bg, byte bgcolor, short logo, byte logocolor) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.setGuildEmblem(bg, bgcolor, logo, logocolor);
        }
    }

    /*
     * 解散家族
     */
    public void disbandGuild(int guildId) {
        MapleGuild guild = getGuild(guildId);
        lock.writeLock().lock();
        try {
            if (guild != null) {
                guild.disbandGuild();
                guildList.remove(guildId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /*
     * 删除家族成员
     */
    public void deleteGuildCharacter(int guildId, int charId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            MapleGuildCharacter mc = guild.getMGC(charId);
            if (mc != null) {
                if (mc.getGuildRank() > 1) { //not leader
                    guild.leaveGuild(mc);
                } else {
                    guild.disbandGuild();
                }
            }
        }
    }

    /*
     * 增加家族成员上限数量
     */
    public boolean increaseGuildCapacity(int guildId, boolean b) {
        MapleGuild guild = getGuild(guildId);
        return guild != null && guild.increaseCapacity(b);
    }

    /*
     * 增加家族的贡献度
     */
    public void gainGP(int guildId, int amount) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.gainGP(amount);
        }
    }

    public void gainGP(int guildId, int amount, int chrId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.gainGP(amount, false, chrId);
        }
    }

    public int getGP(int guildId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.getGP();
        }
        return 0;
    }

    public int getInvitedId(int guildId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.getInvitedId();
        }
        return 0;
    }

    public void setInvitedId(int guildId, int inviteId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.setInvitedId(inviteId);
        }
    }

    public int getGuildLeader(int guildName) {
        MapleGuild guild = getGuild(guildName);
        if (guild != null) {
            return guild.getLeaderId();
        }
        return 0;
    }

    public int getGuildLeader(String guildName) {
        MapleGuild guild = getGuildByName(guildName);
        if (guild != null) {
            return guild.getLeaderId();
        }
        return 0;
    }

    public void save() {
        System.out.println("正在保存家族数据...");
        lock.writeLock().lock();
        try {
            for (MapleGuild guild : guildList.values()) {
                guild.writeToDB(false);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /*
     * 获取家族BSS的信息
     */
    public List<MapleBBSThread> getBBS(int guildId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.getBBS();
        }
        return null;
    }

    /*
     * 添加1个家族BBS信息
     */
    public int addBBSThread(int guildId, String title, String text, int icon, boolean bNotice, int posterId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            return guild.addBBSThread(title, text, icon, bNotice, posterId);
        }
        return -1;
    }

    /*
     * 编辑家族BSS信息
     */
    public void editBBSThread(int guildId, int localthreadId, String title, String text, int icon, int posterId, int guildRank) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.editBBSThread(localthreadId, title, text, icon, posterId, guildRank);
        }
    }

    /*
     * 删除家族BBS信息
     */
    public void deleteBBSThread(int guildId, int localthreadId, int posterId, int guildRank) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.deleteBBSThread(localthreadId, posterId, guildRank);
        }
    }

    /*
     * 添加家族BBS信息的回复 也就是留言
     */
    public void addBBSReply(int guildId, int localthreadId, String text, int posterId) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.addBBSReply(localthreadId, text, posterId);
        }
    }

    /*
     * 删除家族BBS信息的回复
     */
    public void deleteBBSReply(int guildId, int localthreadId, int replyId, int posterId, int guildRank) {
        MapleGuild guild = getGuild(guildId);
        if (guild != null) {
            guild.deleteBBSReply(localthreadId, replyId, posterId, guildRank);
        }
    }

    /*
     * 修改家族图标
     */
    public void changeEmblem(int guildId, int affectedPlayers, MapleGuild guild) {
        WorldBroadcastService.getInstance().sendGuildPacket(affectedPlayers, GuildPacket.guildEmblemChange(guildId, (short) guild.getLogoBG(), (byte) guild.getLogoBGColor(), (short) guild.getLogo(), (byte) guild.getLogoColor()), -1, guildId, false);
        setGuildAndRank(affectedPlayers, -1, -1, -1, -1);
    }

    public void setGuildAndRank(int chrId, int guildId, int rank, int contribution, int alliancerank) {
        int ch = WorldFindService.getInstance().findChannel(chrId);
        if (ch == -1) {
            return;
        }
        MapleCharacter player = getStorage(ch).getCharacterById(chrId);
        if (player == null) {
            return;
        }
        boolean isDifferentGuild;
        if (guildId == -1 && rank == -1) { //just need a respawn
            isDifferentGuild = true;
        } else {
            isDifferentGuild = guildId != player.getGuildId();
            player.setGuildId(guildId);
            player.setGuildRank((byte) rank);
            player.setGuildContribution(contribution);
            player.setAllianceRank((byte) alliancerank);
            player.saveGuildStatus();
        }
        if (isDifferentGuild && ch > 0) {
            player.getMap().broadcastMessage(player, GuildPacket.loadGuildName(player), false);
            player.getMap().broadcastMessage(player, GuildPacket.loadGuildIcon(player), false);
        }
    }

    public PlayerStorage getStorage(int channel) {
        if (channel == -20) {
            return AuctionServer.getPlayerStorage();
        } else if (channel == -10) {
            return CashShopServer.getPlayerStorage();
        }
        return ChannelServer.getInstance(channel).getPlayerStorage();
    }

    public List<Pair<Integer, MapleGuild>> getGuildList() {
        List<Pair<Integer, MapleGuild>> gui = new ArrayList<>();
        for (Entry<Integer, MapleGuild> g : guildList.entrySet()) {
            gui.add(new Pair<>(g.getKey(), g.getValue()));
        }
        return gui;
    }

    private static class SingletonHolder {

        protected static final WorldGuildService instance = new WorldGuildService();
    }
}
