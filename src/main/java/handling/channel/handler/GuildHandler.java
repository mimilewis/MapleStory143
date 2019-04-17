package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.skills.Skill;
import client.skills.SkillFactory;
import configs.ServerConfig;
import handling.world.WorldAllianceService;
import handling.world.WorldGuildService;
import handling.world.guild.MapleGuild;
import handling.world.guild.MapleGuildCharacter;
import handling.world.guild.MapleGuildResponse;
import server.MapleStatEffect;
import tools.Pair;
import tools.StringUtil;
import tools.data.input.LittleEndianAccessor;
import tools.packet.GuildPacket;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GuildHandler {

    private static final Map<String, Pair<Integer, Long>> invited = new HashMap<>(); //[角色名字] [[家族ID] [邀请的时间]]
    private static final List<Integer> ApplyIDs = new ArrayList<>(); //在申请中的角色ID
    private static final ReentrantReadWriteLock applyIDsLock = new ReentrantReadWriteLock();
    private static long nextPruneTime = System.currentTimeMillis() + 60 * 1000;

    public static void addApplyIDs(int id) {
        applyIDsLock.readLock().lock();
        try {
            ApplyIDs.add(id);
        } finally {
            applyIDsLock.readLock().unlock();
        }
    }

    public static void removeApplyIDs(int id) {
        applyIDsLock.readLock().lock();
        try {
            if (ApplyIDs.contains(id)) {
                ApplyIDs.remove(Integer.valueOf(id));
            }
        } finally {
            applyIDsLock.readLock().unlock();
        }
    }

    /*
     * 拒绝家族邀请
     */
    public static void DenyGuildRequest(String from, MapleClient c) {
        MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterByName(from);
        if (cfrom != null && invited.remove(c.getPlayer().getName().toLowerCase()) != null) {
            cfrom.getClient().announce(GuildPacket.denyGuildInvitation(c.getPlayer().getName()));
        }
    }

    /*
     * 玩家自己申请加入家族
     * 如果家族没有同意或者拒绝你的申请
     * 申请后无法向其他家族进行申请 持续时间48小时
     */
    public static void GuildApply(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() > 0) {
            c.getPlayer().dropMessage(1, "您已经有家族了，无需重复申请.");
            return;
        }
        if (ApplyIDs.contains(c.getPlayer().getId())) {
            c.getPlayer().dropMessage(1, "您已经在家族申请列表中，暂时无法进行此操作.");
            c.announce(MapleGuildResponse.无法申请公会.getPacket());
            removeApplyIDs(c.getPlayer().getId());
            return;
        }
        int guildId = slea.readInt(); //家族ID
        MapleGuildCharacter guildMember = new MapleGuildCharacter(c.getPlayer());
        guildMember.setGuildId(guildId);
        int ret = WorldGuildService.getInstance().addGuildApplyMember(guildMember);
        if (ret == 1) {
            addApplyIDs(c.getPlayer().getId());
            c.getPlayer().dropMessage(1, "您成功申请加入家族，请等待族长同意.");
        } else {
            c.getPlayer().dropMessage(1, "申请加入家族出现错误，请稍后再试.");
        }
    }

    /*
     * 接受家族申请
     * [29 01] [01] [37 75 00 00]
     * 应该是同时 接受 多少角色的家族申请
     */
    public static void AcceptGuildApply(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) { //1 == 族长, 2 == 副族长
            return;
        }
        int guildId = c.getPlayer().getGuildId();
        byte amount = slea.readByte();
        int fromId;
        MapleCharacter from;
        for (int i = 0; i < amount; i++) {
            fromId = slea.readInt(); //角色ID
            from = c.getChannelServer().getPlayerStorage().getCharacterById(fromId);
            //暂时只能处理在线的角色申请的信息
            if (from != null && from.getGuildId() <= 0) {
                from.setGuildId(guildId);
                from.setGuildRank((byte) 5);
                int ret = WorldGuildService.getInstance().addGuildMember(from.getMGC());
                if (ret == 0) {
                    from.setGuildId(0);
                    continue;
                }
                from.getClient().announce(GuildPacket.showGuildInfo(from));
                MapleGuild gs = WorldGuildService.getInstance().getGuild(guildId);
                for (byte[] pack : WorldAllianceService.getInstance().getAllianceInfo(gs.getAllianceId(), true)) {
                    if (pack != null) {
                        from.getClient().announce(pack);
                    }
                }
                from.saveGuildStatus();
                respawnPlayer(from);
            }
            if (ApplyIDs.contains(fromId)) {
                removeApplyIDs(fromId);
                break;
            }
        }
    }

    /*
     * 拒绝家族申请
     * [2A 01] [01] [37 75 00 00]
     * 应该是同时 拒绝 多少角色的家族申请
     */
    public static void DenyGuildApply(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() <= 0 || c.getPlayer().getGuildRank() > 2) { //1 == 族长, 2 == 副族长
            return;
        }
        int guildId = c.getPlayer().getGuildId();
        byte amount = slea.readByte();
        int fromId;
        for (int i = 0; i < amount; i++) {
            fromId = slea.readInt(); //角色ID
            WorldGuildService.getInstance().denyGuildApplyMember(guildId, fromId);
            if (ApplyIDs.contains(fromId)) {
                removeApplyIDs(fromId);
            }
        }
    }

    /*
     * 家族操作
     */
    public static void Guild(LittleEndianAccessor slea, MapleClient c) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= nextPruneTime) {
            Iterator<Entry<String, Pair<Integer, Long>>> itr = invited.entrySet().iterator();
            Entry<String, Pair<Integer, Long>> inv;
            while (itr.hasNext()) {
                inv = itr.next();
                if (currentTime >= inv.getValue().right) {
                    itr.remove();
                }
            }
            nextPruneTime += 5 * 60 * 1000;
        }
        MapleCharacter chr = c.getPlayer();
        byte mode = slea.readByte();
        switch (mode) {
            case 0x00: //别人加入家族
                c.announce(GuildPacket.showGuildInfo(chr));
                break;
            case 0x01: //看其他玩家的家族信息
                int fromId = slea.readInt(); //角色ID有时是家族ID
                MapleGuild guild;
                //先查找角色信息
                MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterById(fromId);
                if (target == null) {
                    //如果角色为空就找家族ID
                    guild = WorldGuildService.getInstance().getGuild(fromId);
                    if (guild == null) {
                        chr.dropMessage(1, "找不到玩家或家族的信息.");
                        return;
                    }
                    c.announce(GuildPacket.showPlayerGuildInfo(guild));
                    return;
                }
                //角色信息不为空 判断是否有家族
                if (target.getGuildId() <= 0) {
                    chr.dropMessage(1, "玩家[" + target.getName() + "]没有家族.");
                    return;
                }
                //获得家族的信息
                guild = WorldGuildService.getInstance().getGuild(target.getGuildId());
                if (guild == null) {
                    chr.dropMessage(1, "玩家[" + target.getName() + "]还没有家族.");
                    return;
                }
                c.announce(GuildPacket.showPlayerGuildInfo(guild));
                break;
            case 0x02://查看家族信息
                MapleGuild tmpguild = WorldGuildService.getInstance().getGuild(slea.readInt());
                if (tmpguild != null) {
                    c.announce(GuildPacket.viewGuildInfo(tmpguild));
                }
                break;
            case 0x04: // 创建家族  V.117.1修改
                int cost = ServerConfig.CHANNEL_CREATEGUILDCOST;
                if (chr.getGuildId() > 0 || chr.getMapId() != 200000301) {
                    chr.dropMessage(1, "不能创建家族\r\n已经有家族或没在家族中心");
                    return;
                } else if (chr.getMeso() < cost) {
                    chr.dropMessage(1, "你没有足够的金币创建一个家族。当前创建家族需要: " + cost + " 的金币.");
                    return;
                }
                String guildName = slea.readMapleAsciiString();
                if (!isGuildNameAcceptable(guildName)) {
                    chr.dropMessage(1, "你不能使用这个名字。");
                    return;
                }
                int guildId = WorldGuildService.getInstance().createGuild(chr.getId(), guildName);
                if (guildId == 0) {
                    chr.dropMessage(1, "创建家族出错\r\n请重试一次.");
                    return;
                }
                chr.gainMeso(-cost, true, true);
                chr.setGuildId(guildId);
                chr.setGuildRank((byte) 1);
                chr.saveGuildStatus();
                chr.finishAchievement(35);
                WorldGuildService.getInstance().setGuildMemberOnline(chr.getMGC(), true, c.getChannel());
                c.announce(GuildPacket.showGuildInfo(chr));
                WorldGuildService.getInstance().gainGP(chr.getGuildId(), 500, chr.getId());
                chr.dropMessage(1, "恭喜你成功创建家族.");
                respawnPlayer(chr);
                break;
            case 0x07: // 家族邀请  V.117.1修改
                if (chr.getGuildId() <= 0 || chr.getGuildRank() > 2) { //1 == 族长, 2 == 副族长
                    return;
                }
                String name = slea.readMapleAsciiString().toLowerCase();
                if (invited.containsKey(name)) {
                    chr.dropMessage(5, "玩家 " + name + " 已经在邀请的列表，请稍后在试。");
                    return;
                }
                MapleGuildResponse mgr = MapleGuild.sendInvite(c, name);
                if (mgr != null) {
                    c.announce(mgr.getPacket());
                } else {
                    invited.put(name, new Pair<>(chr.getGuildId(), currentTime + (60 * 1000)));
                }
                break;
            case 0x36: // 接受家族邀请
                if (chr.getGuildId() > 0) {
                    return;
                }
                guildId = slea.readInt();
                fromId = slea.readInt();
                if (fromId != chr.getId()) {
                    return;
                }
                name = chr.getName().toLowerCase();
                Pair<Integer, Long> gid = invited.remove(name);
                if (gid != null && guildId == gid.left) {
                    chr.setGuildId(guildId);
                    chr.setGuildRank((byte) 5);
                    int ret = WorldGuildService.getInstance().addGuildMember(chr.getMGC());
                    if (ret == 0) {
                        chr.dropMessage(1, "尝试加入的家族成员数已到达最高限制。");
                        chr.setGuildId(0);
                        return;
                    }
                    c.announce(GuildPacket.showGuildInfo(chr));
                    MapleGuild gs = WorldGuildService.getInstance().getGuild(guildId);
                    for (byte[] pack : WorldAllianceService.getInstance().getAllianceInfo(gs.getAllianceId(), true)) {
                        if (pack != null) {
                            c.announce(pack);
                        }
                    }
                    chr.saveGuildStatus();
                    respawnPlayer(c.getPlayer());
                }
                break;
            case 0x0B: // 离开家族  V.117.1修改
                fromId = slea.readInt();
                name = slea.readMapleAsciiString();
                if (fromId != chr.getId() || !name.equals(chr.getName()) || chr.getGuildId() <= 0) {
                    return;
                }
                WorldGuildService.getInstance().leaveGuild(chr.getMGC());
                c.announce(GuildPacket.showGuildInfo(null));
                break;
            case 0x0C: // 家族驱除玩家  V.117.1修改
                fromId = slea.readInt();
                name = slea.readMapleAsciiString();
                if (chr.getGuildRank() > 2 || chr.getGuildId() <= 0) {
                    return;
                }
                WorldGuildService.getInstance().expelMember(chr.getMGC(), name, fromId);
                break;
            case 0x12: // 家族等级职称修改  V.117.1修改
                if (chr.getGuildId() <= 0 || chr.getGuildRank() != 1) {
                    return;
                }
                String ranks[] = new String[5];
                for (int i = 0; i < 5; i++) {
                    ranks[i] = slea.readMapleAsciiString();
                }
                WorldGuildService.getInstance().changeRankTitle(chr.getGuildId(), ranks);
                break;
            case 0x13: // 职位变化  V.117.1修改
                fromId = slea.readInt();
                byte newRank = slea.readByte();
                if ((newRank <= 1 || newRank > 5) || chr.getGuildRank() > 2 || (newRank <= 2 && chr.getGuildRank() != 1) || chr.getGuildId() <= 0) {
                    return;
                }
                WorldGuildService.getInstance().changeRank(chr.getGuildId(), fromId, newRank);
                break;
            case 0x14: // 家族徽章修改  V.117.1修改
                if (chr.getGuildId() <= 0 || chr.getGuildRank() != 1) {
                    return;
                }
                if (chr.getMeso() < 1500000) {
                    chr.dropMessage(1, "金币不足 1500000。");
                    return;
                }
                short bg = slea.readShort();
                byte bgcolor = slea.readByte();
                short logo = slea.readShort();
                byte logocolor = slea.readByte();
                WorldGuildService.getInstance().setGuildEmblem(chr.getGuildId(), bg, bgcolor, logo, logocolor);
                chr.gainMeso(-1500000, true, true);
                respawnPlayer(c.getPlayer());
                break;
            case 0x31: // 家族公告修改
                String notice = slea.readMapleAsciiString();
                if (notice.length() > 100 || chr.getGuildId() <= 0 || chr.getGuildRank() > 2) {
                    return;
                }
                WorldGuildService.getInstance().setGuildNotice(chr.getGuildId(), notice);
                break;
            case 0x23: // 升级家族技能
                int skillId = slea.readInt();
                byte level = slea.readByte();
                if (skillId > 0) {
                    chr.dropMessage(1, "当前暂不支持家族技能升级.");
                    return;
                }
                Skill skill = SkillFactory.getSkill(skillId);
                if (chr.getGuildId() <= 0 || skill == null || skill.getId() < 91000000) {
                    return;
                }
                //检测新的技能等级
                int newLevel = WorldGuildService.getInstance().getSkillLevel(chr.getGuildId(), skill.getId()) + level;
                if (newLevel > skill.getMaxLevel()) {
                    return;
                }
                MapleStatEffect skillid = skill.getEffect(newLevel);
                if (skillid.getReqGuildLevel() <= 0 || chr.getMeso() < skillid.getPrice()) {
                    return;
                }
                if (WorldGuildService.getInstance().purchaseSkill(chr.getGuildId(), skillid.getSourceid(), chr.getName(), chr.getId())) {
                    chr.gainMeso(-skillid.getPrice(), true);
                }
                break;
            case 0x3E: // 激活使用家族技能
                skill = SkillFactory.getSkill(slea.readInt());
                if (c.getPlayer().getGuildId() <= 0 || skill == null) {
                    return;
                }
                newLevel = WorldGuildService.getInstance().getSkillLevel(chr.getGuildId(), skill.getId());
                if (newLevel <= 0) {
                    return;
                }
                MapleStatEffect skillii = skill.getEffect(newLevel);
                if (skillii.getReqGuildLevel() < 0 || chr.getMeso() < skillii.getExtendPrice()) {
                    return;
                }
                if (WorldGuildService.getInstance().activateSkill(chr.getGuildId(), skillii.getSourceid(), chr.getName())) {
                    chr.gainMeso(-skillii.getExtendPrice(), true);
                }
                break;
            case 0x28: //改变家族族长   V.117.1修改
                fromId = slea.readInt();
                if (chr.getGuildId() <= 0 || chr.getGuildRank() > 1) {
                    return;
                }
                WorldGuildService.getInstance().setGuildLeader(chr.getGuildId(), fromId);
                break;
            case 0x2C: //显示初心者技能信息
                if (chr.getGuildId() <= 0) {
                    return;
                }
                c.announce(GuildPacket.showGuildBeginnerSkill());
                break;
            case 0x2D: {//家族搜索
                int type = slea.readByte();
                switch (type) {
                    case 0: {//搜索家族名:
                        int subtype = slea.readByte();
                        slea.skip(2);
                        List<Pair<Integer, MapleGuild>> gui = WorldGuildService.getInstance().getGuildList();
                        List<Pair<Integer, MapleGuild>> Gui = new ArrayList<>();
                        String resguildName = slea.readMapleAsciiString().toLowerCase();
                        String Patriarch = "找不到家族";
                        int Mean = 0;
                        for (Pair<Integer, MapleGuild> g : gui) {
                            MapleCharacter leaderObj = g.getRight().getLeader(c);
                            String gname = g.getRight().getName().toLowerCase();
                            if ((subtype == 1 || subtype == 2) && gname.contains(resguildName) || (subtype == 1 && gname.contains(resguildName)) || (subtype == 2 && leaderObj != null && leaderObj.getName().toLowerCase().contains(resguildName))) {
                                for (MapleGuildCharacter gchr : g.getRight().getMembers()) {
                                    if (gchr.getGuildRank() == 1) {
                                        Patriarch = gchr.getName();
                                    }
                                }
                                Mean = g.getRight().getMembers().size() / g.getRight().getCapacity();
                                Gui.add(g);
                            }
                        }
                        c.announce(GuildPacket.guildSearch_Results(Gui, Patriarch, Mean)); //搜索家族
                        break;
                    }
                    case 1: {//按照搜索条件来搜索家族
                        int minlevel = slea.readByteAsInt();//搜索最小家族等级
                        int maxlevel = slea.readByteAsInt();//搜索最大家族等级
                        int minScale = slea.readByteAsInt();//搜索最小家族规模
                        int maxScale = slea.readByteAsInt();//搜索最大家族规模
                        int minMemberLevel = slea.readByteAsInt();//搜索最小家族成员等级
                        int maxMemberLevel = slea.readByteAsInt();//搜索最大家族成员等级

                        List<Pair<Integer, MapleGuild>> gui = WorldGuildService.getInstance().getGuildList();
                        List<Pair<Integer, MapleGuild>> Gui = new ArrayList<>();
                        String Patriarch = "找不到族长";
                        int Mean = 0;
                        for (Pair<Integer, MapleGuild> g : gui) {
                            if (g.getRight().getLevel() >= minlevel && g.getRight().getLevel() <= maxlevel) {
                                if (g.getRight().getCapacity() >= minScale && g.getRight().getCapacity() <= maxScale) {
                                    boolean isOout = false;
                                    for (MapleGuildCharacter gchr : g.getRight().getMembers()) {
                                        if (gchr.getLevel() >= minMemberLevel && gchr.getLevel() <= maxMemberLevel) {
                                            isOout = true;
                                        }
                                        if (isOout) {
                                            if (gchr.getGuildRank() == 1) {
                                                Patriarch = gchr.getName();
                                            }
                                        }
                                    }
                                    if (isOout) {
                                        Mean = g.getRight().getMembers().size() / g.getRight().getCapacity();
                                        Gui.add(g);
                                    }
                                }
                            }
                        }
                        c.announce(GuildPacket.guildSearch_Results(Gui, Patriarch, Mean)); //搜索家族
                        break;
                    }
                }
                break;
            }
            default:
                System.out.println("未知家族操作类型: ( 0x" + StringUtil.getLeftPaddedStr(Integer.toHexString(mode).toUpperCase(), '0', 2) + " )" + slea.toString());
                break;
        }
    }

    private static boolean isGuildNameAcceptable(String name) {
        return !(name.getBytes().length < 3 || name.getBytes().length > 12);
    }

    private static void respawnPlayer(MapleCharacter chr) {
        if (chr.getMap() == null) {
            return;
        }
        chr.getMap().broadcastMessage(GuildPacket.loadGuildName(chr));
        chr.getMap().broadcastMessage(GuildPacket.loadGuildIcon(chr));
    }
}
