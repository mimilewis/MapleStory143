/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import handling.channel.MapleGuildRanking;
import handling.opcode.GuildOpcode;
import handling.opcode.SendPacketOpcode;
import handling.world.WorldGuildService;
import handling.world.guild.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.Collection;
import java.util.List;

/**
 * @author PlayDK
 */
public class GuildPacket {

    private static final Logger log = LogManager.getLogger(GuildPacket.class);

    /*
     * 家族邀请玩家 V.117.1 OK
     */
    public static byte[] guildInvite(int guildId, String charName, int levelFrom, int jobFrom) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildReq_InviteGuild.getValue()); //V.117.1修改 以前 0x05
        mplew.writeInt(guildId);
        mplew.writeMapleAsciiString(charName);
        mplew.writeInt(levelFrom);
        mplew.writeInt(jobFrom);
        mplew.writeInt(0); //V.104新增 貌似是把职业的 Int 该为 Long ?

        return mplew.getPacket();
    }

    /*
     * 进入游戏试显示家族信息
     * V.1171.1 OK
     */
    public static byte[] showGuildInfo(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.writeShort(GuildOpcode.GuildRes_LoadGuild_Done.getValue());
        //检测是否有家族
        if (chr == null || chr.getMGC() == null) {
            mplew.write(0);
            return mplew.getPacket();
        }
        MapleGuild guild = WorldGuildService.getInstance().getGuild(chr.getGuildId());
        if (guild == null) {
            mplew.write(0);
            return mplew.getPacket();
        }
        mplew.write(1);
        //家族信息
        addGuildInfo(mplew, guild);
        //家族升级需要的声望点数
        addGuildExpInfo(mplew, guild);

        return mplew.getPacket();
    }

    /*
     * 显示别人的家族信息
     * V.1171.1 OK
     */
    public static byte[] showPlayerGuildInfo(MapleGuild guild) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_FindGuild_Done.getValue());
        mplew.writeInt(guild.getId());
        addGuildInfo(mplew, guild);

        return mplew.getPacket();
    }

    private static void addGuildInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
        mplew.writeInt(guild.getId());
        mplew.writeMapleAsciiString(guild.getName());
        //家族称号信息
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(guild.getRankTitle(i));
        }
        //家族成员信息和等待加入家族的角色信息
        guild.addMemberData(mplew);
        //家族最大成员上限
        mplew.writeInt(guild.getCapacity());
        //家族图标信息
        mplew.writeShort(guild.getLogoBG());
        mplew.write(guild.getLogoBGColor());
        mplew.writeShort(guild.getLogo());
        mplew.write(guild.getLogoColor());
        //家族公告  V.117.1 好像没有了改为空的【00 00】
        mplew.writeMapleAsciiString(guild.getNotice());
        //家族声望 好像要写2次
        mplew.writeInt(guild.getGP()); //written twice, aftershock?
        mplew.writeInt(guild.getGP());
        //家族是否有联盟 如果有写联盟ID
        mplew.writeInt(guild.getAllianceId() > 0 ? guild.getAllianceId() : 0);
        //家族等级
        mplew.write(guild.getLevel());
        mplew.writeShort(0); //probably guild rank or somethin related, appears to be 0
        mplew.writeInt(0); //未知 V.117.1新增
        //家族技能信息
        mplew.writeShort(guild.getSkills().size()); //AFTERSHOCK: uncomment
        for (MapleGuildSkill i : guild.getSkills()) {
            mplew.writeInt(i.skillID);
            mplew.writeShort(i.level);
            mplew.writeLong(PacketHelper.getTime(i.timestamp));
            mplew.writeMapleAsciiString(i.purchaser);
            mplew.writeMapleAsciiString(i.activator);
        }
        mplew.write(0); //V.117.1新增 未知
    }

    private static void addGuildExpInfo(MaplePacketLittleEndianWriter mplew, MapleGuild guild) {
        int[] guildExp = guild.getGuildExp();
        mplew.writeInt(guildExp.length);
        for (int aGuildExp : guildExp) {
            mplew.writeInt(aGuildExp);
        }
    }

    /*
     * 创建1个家族
     */
    public static byte[] createGuild(MapleGuild guild) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_CreateNewGuild_Done.getValue());
        addGuildInfo(mplew, guild);

        return mplew.getPacket();
    }

    /*
     * 家族技能
     */
    public static byte[] guildSkillPurchased(int guildId, int skillId, int level, long expiration, String purchase, String activate) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetSkill_Done.getValue());
        mplew.writeInt(guildId);
        mplew.writeInt(skillId);
        mplew.writeShort(level);
        mplew.writeLong(PacketHelper.getTime(expiration));
        mplew.writeMapleAsciiString(purchase);
        mplew.writeMapleAsciiString(activate);

        return mplew.getPacket();
    }

    /*
     * 0x03 弹出输出创建家族名字的对话
     * 0x38 等级太低不能创建家族
     * 0x3D 已经有家族了
     * 0x3E 家族人数已满
     * 0x3F 当前频道找不到该角色
     * 0x61 无法创建公会标志。创建条件: 公会等级2以上 拥有GP 150000
     */
    public static byte[] genericGuildMessage(byte code) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(code);

        return mplew.getPacket();
    }

    /*
     * 新成员加入家族或者玩家申请加入家族
     * V.117.1 OK
     */
    public static byte[] newGuildMember(MapleGuildCharacter mgc, boolean isApply) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(isApply ? GuildOpcode.GuildRes_JoinRequest_Done.getValue() : GuildOpcode.GuildRes_JoinGuild_Done.getValue()); //V.117.1修改 以前0x2D   +0x0F
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeAsciiString(mgc.getName(), 13);
        mplew.writeInt(mgc.getJobId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getGuildRank()); //should be always 5 but whatevs
        mplew.writeInt(mgc.isOnline() ? 1 : 0); //should always be 1 too
        mplew.writeInt(mgc.getAllianceRank()); //? could be guild signature, but doesn't seem to matter
        mplew.writeInt(mgc.getGuildContribution()); //should always 3
        mplew.writeInt(0); //未知 V.117.1 新增
        mplew.writeInt(0); //未知 V.117.1 新增
        mplew.writeLong(PacketHelper.getTime(-2)); //00 40 E0 FD 3B 37 4F 01

        return mplew.getPacket();
    }

    /*
     * 拒绝角色的家族申请
     */
    public static byte[] DenyGuildApply(int chrId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_JoinCancelRequest_Done.getValue());
        mplew.writeInt(chrId);

        return mplew.getPacket();
    }

    /*
     * 家族成员离开或者驱逐
     * V.117.1 OK
     */
    public static byte[] memberLeft(MapleGuildCharacter mgc, boolean isExpelled) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(isExpelled ? GuildOpcode.GuildRes_KickGuild_Done.getValue() : GuildOpcode.GuildRes_WithdrawGuild_Done.getValue()); //V.117.1修改 以前驱逐 0x35 自己退出0x32 +0x16
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeMapleAsciiString(mgc.getName());

        return mplew.getPacket();
    }

    /*
     * 修改家族公告
     * V.117.1删除
     */
    public static byte[] guildNotice(int guildId, String notice) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetNotice_Done.getValue());
        mplew.writeInt(guildId);
        mplew.writeMapleAsciiString(notice);

        return mplew.getPacket();
    }

    /*
     * 解散家族
     * V.117.1 OK
     */
    public static byte[] guildDisband(int guildId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_RemoveGuild_Done.getValue()); //V.117.1修改 以前0x38
        mplew.writeInt(guildId);
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 玩家拒绝家族邀请
     * V.117.1 OK
     */
    public static byte[] denyGuildInvitation(String charname) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_InviteGuild_Rejected.getValue()); //V.117.1修改 以前0x3D   +0x17
        mplew.writeMapleAsciiString(charname);

        return mplew.getPacket();
    }

    /*
     * 家族成员上限改变
     * V.117.1 OK
     */
    public static byte[] guildCapacityChange(int guildId, int capacity) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_IncMaxMemberNum_Done.getValue()); //V.117.1修改 以前0x40
        mplew.writeInt(guildId);
        mplew.write(capacity);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 家族成员等级提升或者职业变更
     * V.117.1 OK
     */
    public static byte[] guildMemberLevelJobUpdate(MapleGuildCharacter mgc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_ChangeLevelOrJob.getValue()); //V.117.1修改 以前0x42   +0x17
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());

        return mplew.getPacket();
    }

    /*
     * 家族成员上线
     * V.117.1 OK
     */
    public static byte[] guildMemberOnline(int guildId, int chrId, boolean isOnline) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_NotifyLoginOrLogout.getValue()); //V.117.1修改 以前0x43   +0x17
        mplew.writeInt(guildId);
        mplew.writeInt(chrId);
        mplew.write(isOnline ? 1 : 0);
        mplew.write(1);

        return mplew.getPacket();
    }

    /*
     * 家族称号修改
     * V.117.1 OK
     */
    public static byte[] rankTitleChange(int guildId, String[] ranks) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetGradeName_Done.getValue()); //V.117.1修改 以前0x44   +0x17
        mplew.writeInt(guildId);
        for (String r : ranks) {
            mplew.writeMapleAsciiString(r);
        }
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    /*
     * 家族地位变更
     * V.117.1 OK
     */
    public static byte[] changeRank(MapleGuildCharacter mgc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetMemberGrade_Done.getValue());  //V.117.1修改 以前0x46  +0x17
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.write(mgc.getGuildRank());

        return mplew.getPacket();
    }

    /*
     * 更新家族玩家的贡献信息
     * V.117.1 OK
     */
    public static byte[] updatePlayerContribution(int guildId, int chrId, int contribution) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetMemberCommitment_Done.getValue()); //V.117.1 修改 以前 0x48 +0x17
        mplew.writeInt(guildId);
        mplew.writeInt(chrId);
        mplew.writeInt(contribution); //当前的贡献度
        mplew.writeInt(contribution); //获得的贡献度
        mplew.writeInt(0); //当前的IGP
        mplew.writeLong(PacketHelper.getTime(System.currentTimeMillis())); //当前的时间

        return mplew.getPacket();
    }

    /*
     * 家族图标变更
     * V.117.1 OK
     */
    public static byte[] guildEmblemChange(int guildId, short bg, byte bgcolor, short logo, byte logocolor) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_SetMark_Done.getValue()); //V.117.1修改 以前0x49
        mplew.writeInt(guildId);
        mplew.writeShort(bg);
        mplew.write(bgcolor);
        mplew.writeShort(logo);
        mplew.write(logocolor);

        return mplew.getPacket();
    }

    /*
     * 更新家族的总共享和总GP
     */
    public static byte[] updateGuildInfo(int guildId, int totalContribution, int guildlevel) {
        return updateGuildInfo(guildId, totalContribution, guildlevel, 0);
    }

    public static byte[] updateGuildInfo(int guildId, int totalContribution, int guildlevel, int totalGP) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_IncPoint_Done.getValue()); //V.117.1 修改 以前0x4F  +0x17
        mplew.writeInt(guildId);
        mplew.writeInt(totalContribution); //当前家族的总贡献度
        mplew.writeInt(guildlevel); //家族的等级
        mplew.writeInt(totalGP); //当前的IGP

        return mplew.getPacket();
    }

    /*
     * 荣耀之石
     * V.117.1 OK
     */
    public static byte[] showGuildRanks(int npcid, List<MapleGuildRanking.GuildRankingInfo> all, boolean show) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_ShowGuildRanking.getValue()); //V.117.1修改 以前0x50   +0x17
        mplew.writeInt(npcid);
        mplew.writeInt(show ? all.size() : 0);
        if (show) {
            for (MapleGuildRanking.GuildRankingInfo info : all) {
                mplew.writeMapleAsciiString(info.getName());
                mplew.writeInt(info.getGP());
                mplew.writeInt(info.getLogo());
                mplew.writeInt(info.getLogoColor());
                mplew.writeInt(info.getLogoBg());
                mplew.writeInt(info.getLogoBgColor());
            }
        }

        return mplew.getPacket();
    }

    /*
     * 改变家族族长
     * V.117.1 OK
     */
    public static byte[] guildLeaderChanged(int guildId, int oldLeader, int newLeader, int allianceId) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_ChangeMaster_Done.getValue()); //V.117.1修改 以前0x59
        mplew.writeInt(guildId);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);
        mplew.write(allianceId > 0 ? 1 : 0);
        if (allianceId > 0) {
            mplew.writeInt(allianceId);
        }

        return mplew.getPacket();
    }

    /*
     * 显示初心者技能信息
     * V.117.1 OK
     */
    public static byte[] showGuildBeginnerSkill() {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(GuildOpcode.GuildRes_BattleSkillOpen.getValue());
        mplew.writeShort(0); //当前拥有的技能点
        mplew.writeShort(0); //当前使用的技能点

        return mplew.getPacket();
    }

    /*
     * 家族联盟
     */
    public static byte[] removeGuildFromAlliance(MapleGuildAlliance alliance, MapleGuild expelledGuild, boolean expelled) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x10);
        addAllianceInfo(mplew, alliance);
        addGuildInfo(mplew, expelledGuild);
        mplew.write(expelled ? 1 : 0); //1 = expelled, 0 = left

        return mplew.getPacket();
    }

    /*
     * 家族联盟
     */
    public static byte[] changeAlliance(MapleGuildAlliance alliance, boolean in) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x01);
        mplew.write(in ? 1 : 0);
        mplew.writeInt(in ? alliance.getId() : 0);
        int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < noGuilds; i++) {
            g[i] = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return MaplePacketCreator.enableActions();
            }
        }
        mplew.write(noGuilds);
        for (int i = 0; i < noGuilds; i++) {
            mplew.writeInt(g[i].getId());
            //must be world
            Collection<MapleGuildCharacter> members = g[i].getMembers();
            mplew.writeInt(members.size());
            for (MapleGuildCharacter mgc : members) {
                mplew.writeInt(mgc.getId());
                mplew.write(in ? mgc.getAllianceRank() : 0);
            }
        }

        return mplew.getPacket();
    }

    /*
     * 家族联盟族长变更
     */
    public static byte[] changeAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x02);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);

        return mplew.getPacket();
    }

    /*
     * 改变家族联盟族长
     */
    public static byte[] updateAllianceLeader(int allianceid, int newLeader, int oldLeader) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x19);
        mplew.writeInt(allianceid);
        mplew.writeInt(oldLeader);
        mplew.writeInt(newLeader);

        return mplew.getPacket();
    }

    /*
     * 家族联盟邀请
     */
    public static byte[] sendAllianceInvite(String allianceName, MapleCharacter inviter) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x03);
        mplew.writeInt(inviter.getGuildId());
        mplew.writeMapleAsciiString(inviter.getName());
        mplew.writeMapleAsciiString(allianceName);

        return mplew.getPacket();
    }

    public static byte[] changeGuildInAlliance(MapleGuildAlliance alliance, MapleGuild guild, boolean add) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x04);
        mplew.writeInt(add ? alliance.getId() : 0);
        mplew.writeInt(guild.getId());
        Collection<MapleGuildCharacter> members = guild.getMembers();
        mplew.writeInt(members.size());
        for (MapleGuildCharacter mgc : members) {
            mplew.writeInt(mgc.getId());
            mplew.write(add ? mgc.getAllianceRank() : 0);
        }

        return mplew.getPacket();
    }

    public static byte[] changeAllianceRank(int allianceid, MapleGuildCharacter player) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x05);
        mplew.writeInt(allianceid);
        mplew.writeInt(player.getId());
        mplew.writeInt(player.getAllianceRank());

        return mplew.getPacket();
    }

    public static byte[] createGuildAlliance(MapleGuildAlliance alliance) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0F);
        addAllianceInfo(mplew, alliance);
        int noGuilds = alliance.getNoGuilds();
        MapleGuild[] g = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            g[i] = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
            if (g[i] == null) {
                return MaplePacketCreator.enableActions();
            }
        }
        for (MapleGuild gg : g) {
            addGuildInfo(mplew, gg);
        }

        return mplew.getPacket();
    }

    public static byte[] getAllianceInfo(MapleGuildAlliance alliance) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0C);
        mplew.write(alliance == null ? 0 : 1); //in an alliance
        if (alliance != null) {
            addAllianceInfo(mplew, alliance);
        }

        return mplew.getPacket();
    }

    public static byte[] getAllianceUpdate(MapleGuildAlliance alliance) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x17);
        addAllianceInfo(mplew, alliance);

        return mplew.getPacket();
    }

    public static byte[] getGuildAlliance(MapleGuildAlliance alliance) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0D);
        if (alliance == null) {
            mplew.writeInt(0);
            return mplew.getPacket();
        }
        int noGuilds = alliance.getNoGuilds();
        MapleGuild[] guildlist = new MapleGuild[noGuilds];
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            guildlist[i] = WorldGuildService.getInstance().getGuild(alliance.getGuildId(i));
            if (guildlist[i] == null) {
                return MaplePacketCreator.enableActions();
            }
        }
        mplew.writeInt(noGuilds);
        for (MapleGuild guild : guildlist) {
            addGuildInfo(mplew, guild);
        }

        return mplew.getPacket();
    }

    public static byte[] addGuildToAlliance(MapleGuildAlliance alliance, MapleGuild newGuild) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x12);
        addAllianceInfo(mplew, alliance);
        mplew.writeInt(newGuild.getId()); //???
        addGuildInfo(mplew, newGuild);
        mplew.write(0); //???

        return mplew.getPacket();
    }

    private static void addAllianceInfo(MaplePacketLittleEndianWriter mplew, MapleGuildAlliance alliance) {
        mplew.writeInt(alliance.getId());
        mplew.writeMapleAsciiString(alliance.getName());
        for (int i = 1; i <= 5; i++) {
            mplew.writeMapleAsciiString(alliance.getRank(i));
        }
        mplew.write(alliance.getNoGuilds());
        for (int i = 0; i < alliance.getNoGuilds(); i++) {
            mplew.writeInt(alliance.getGuildId(i));
        }
        mplew.writeInt(alliance.getCapacity());
        mplew.writeMapleAsciiString(alliance.getNotice());
    }

    public static byte[] allianceMemberOnline(int alliance, int gid, int id, boolean online) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x0E);
        mplew.writeInt(alliance);
        mplew.writeInt(gid);
        mplew.writeInt(id);
        mplew.write(online ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] updateAlliance(MapleGuildCharacter mgc, int allianceid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x18);
        mplew.writeInt(allianceid);
        mplew.writeInt(mgc.getGuildId());
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getLevel());
        mplew.writeInt(mgc.getJobId());

        return mplew.getPacket();
    }

    public static byte[] updateAllianceRank(int allianceid, MapleGuildCharacter mgc) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1B);
        mplew.writeInt(allianceid);
        mplew.writeInt(mgc.getId());
        mplew.writeInt(mgc.getAllianceRank());

        return mplew.getPacket();
    }

    /*
     * 解散家族联盟
     */
    public static byte[] disbandAlliance(int alliance) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ALLIANCE_OPERATION.getValue());
        mplew.write(0x1D);
        mplew.writeInt(alliance);

        return mplew.getPacket();
    }

    /*
     * 家族BBS公告
     */
    public static byte[] BBSThreadList(List<MapleBBSThread> bbs, int start) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 打开没有BBS信息
         * [2C 01] [06] [00] [00 00 00 00]
         */
        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x06);

        if (bbs == null) {
            mplew.write(0); //是否有公告
            mplew.writeInt(0); //当前的条数
            return mplew.getPacket();
        }
        /*
         * 开始加载
         */
        MapleBBSThread notice = null;
        for (MapleBBSThread b : bbs) {
            if (b.isNotice()) { //notice
                notice = b;
                bbs.remove(b);
                break;
            }
        }
        mplew.write(notice == null ? 0 : 1);
        if (notice != null) { //has a notice
            addThread(mplew, notice);
        }
        int threadCount = bbs.size();
        if (threadCount < start) { //seek to the thread before where we start
            //uh, we're trying to start at a place past possible
            start = 0;
        }
        //each page has 10 threads, start = page # in packet but not here
        mplew.writeInt(threadCount);
        int pages = Math.min(10, threadCount - start);
        mplew.writeInt(pages);

        for (int i = 0; i < pages; i++) {
            addThread(mplew, bbs.get(start + i)); //because 0 = notice
        }

        return mplew.getPacket();
    }

    private static void addThread(MaplePacketLittleEndianWriter mplew, MapleBBSThread thread) {
        mplew.writeInt(thread.localthreadID);
        mplew.writeInt(thread.ownerID);
        mplew.writeMapleAsciiString(thread.name);
        mplew.writeLong(PacketHelper.getTime(thread.timestamp));
        mplew.writeInt(thread.icon);
        mplew.writeInt(thread.getReplyCount());
    }

    public static byte[] showThread(MapleBBSThread thread) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.BBS_OPERATION.getValue());
        mplew.write(0x07);

        mplew.writeInt(thread.localthreadID);
        mplew.writeInt(thread.ownerID);
        mplew.writeLong(PacketHelper.getTime(thread.timestamp));
        mplew.writeMapleAsciiString(thread.name);
        mplew.writeMapleAsciiString(thread.text);
        mplew.writeInt(thread.icon);
        mplew.writeInt(thread.getReplyCount());
        for (MapleBBSReply reply : thread.replies.values()) {
            mplew.writeInt(reply.replyid);
            mplew.writeInt(reply.ownerID);
            mplew.writeLong(PacketHelper.getTime(reply.timestamp));
            mplew.writeMapleAsciiString(reply.content);
        }

        return mplew.getPacket();
    }

    public static byte[] loadGuildName(MapleCharacter chr) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOAD_GUILD_NAME.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeShort(0);
        } else {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(chr.getGuildId());
            if (guild != null) {
                mplew.writeMapleAsciiString(guild.getName());
            } else {
                mplew.writeShort(0);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] loadGuildIcon(MapleCharacter chr) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.LOAD_GUILD_ICON.getValue());
        mplew.writeInt(chr.getId());
        if (chr.getGuildId() <= 0) {
            mplew.writeZeroBytes(6);
        } else {
            MapleGuild guild = WorldGuildService.getInstance().getGuild(chr.getGuildId());
            if (guild != null) {
                mplew.writeShort(guild.getLogoBG());
                mplew.write(guild.getLogoBGColor());
                mplew.writeShort(guild.getLogo());
                mplew.write(guild.getLogoColor());
            } else {
                mplew.writeZeroBytes(6);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] guildSearch_Results(List<Pair<Integer, MapleGuild>> guild, String Name, int Mean) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.GUILD_SEARCH_RESULTS.getValue());
        mplew.writeInt(guild.size());
        for (Pair<Integer, MapleGuild> Guild : guild) {
            mplew.writeInt(Guild.getLeft());
            mplew.writeInt(Guild.getRight().getLevel());//等级
            mplew.writeMapleAsciiString(Guild.right.getName());//家族名
            mplew.writeMapleAsciiString(Name);//族长名
            mplew.writeInt(Guild.getRight().getCapacity());//家族人数
            mplew.writeInt(Mean);
        }
        return mplew.getPacket();
    }

    //查看家族信息
    public static byte[] viewGuildInfo(final MapleGuild g) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();


        /*
          0x2E 当前登陆的世界为预合拼世界。暂时无法创建家族
          0x31 查看家族信息
          0x33 该名字已存在!
          0x36 访问中出现异常。请重新开始

         */
        mplew.writeShort(SendPacketOpcode.GUILD_OPERATION.getValue());
        mplew.write(0x31);

        mplew.writeInt(g.getId());
        addGuildInfo(mplew, g);

        return mplew.getPacket();
    }
}
