package server.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleStat;
import client.anticheat.ReportType;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.GameConstants;
import constants.skills.新手;
import constants.skills.管理员;
import handling.channel.ChannelServer;
import handling.world.CheaterData;
import handling.world.World;
import handling.world.WorldBroadcastService;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import scripting.event.EventInstanceManager;
import scripting.event.EventManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.life.MapleMonster;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleReactor;
import server.quest.MapleQuest;
import server.squad.MapleSquad;
import server.squad.MapleSquadType;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.packet.EffectPacket;

import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author Emilyx3
 */
public class InternCommand {

    private static final Logger log = LogManager.getLogger(InternCommand.class);

    /**
     * @return
     */
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.INTERN;
    }


    public static class 隐身模式 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Skill skill = SkillFactory.getSkill(管理员.隐藏术);
            Skill skill2 = SkillFactory.getSkill(新手.金刚霸体);
            if (c.getPlayer().isHidden()) {
                c.getPlayer().cancelEffect(skill.getEffect(1), false, -1);
                c.getPlayer().cancelEffect(skill2.getEffect(1), false, -1);
                c.getPlayer().dropMessage(6, "隐身模式已关闭.");
            } else {
                skill.getEffect(1).applyTo(c.getPlayer());
                skill2.getEffect(1).applyTo(c.getPlayer());
                c.getPlayer().dropMessage(6, "隐身模式已开启.");
            }
            return 0;
        }
    }


    public static class LowHP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().updateSingleStat(MapleStat.HP, 1);
            c.getPlayer().updateSingleStat(MapleStat.MP, 1);
            c.getPlayer().getStat().setHp((short) 1, c.getPlayer());
            c.getPlayer().getStat().setMp((short) 1, c.getPlayer());
            return 0;
        }
    }


    public static class Heal extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            return 0;
        }
    }


    public static class HealHere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            for (MapleCharacter mch : player.getMap().getCharacters()) {
                if (mch != null) {
                    mch.getStat().setHp(mch.getStat().getMaxHp(), mch);
                    mch.updateSingleStat(MapleStat.HP, mch.getStat().getMaxHp());
                    mch.getStat().setMp(mch.getStat().getMaxMp(), mch);
                    mch.updateSingleStat(MapleStat.MP, mch.getStat().getMaxMp());
                    mch.dispelDebuffs();
                }
            }
            return 1;
        }
    }


    public static class TempB extends TempBan {
    }


    public static class TempBan extends CommandExecute {


        private final String[] types = {"HACK", "BOT", "AD", "HARASS", "CURSE", "SCAM", "MISCONDUCT", "SELL", "ICASH", "TEMP", "GM", "IPROGRAM", "MEGAPHONE"};
        protected boolean ipBan = false;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(6, "Tempban [玩家名字] [理由] [多少天]");
                StringBuilder s = new StringBuilder("Tempban reasons: ");
                for (int i = 0; i < types.length; i++) {
                    s.append(i + 1).append(" - ").append(types[i]).append(", ");
                }
                c.getPlayer().dropMessage(6, s.toString());
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int reason = Integer.parseInt(splitted[2]);
            int numDay = Integer.parseInt(splitted[3]);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, numDay);
            DateFormat df = DateFormat.getInstance();

            if (victim == null || reason < 0 || reason >= types.length) {
                c.getPlayer().dropMessage(6, "Unable to find character or reason was not valid, type tempban to see reasons");
                return 0;
            }
            victim.tempban("Temp banned by " + c.getPlayer().getName() + " for " + types[reason] + " reason", cal, reason, ipBan);
            c.getPlayer().dropMessage(6, "The character " + splitted[1] + " has been successfully tempbanned till " + df.format(cal.getTime()));
            return 1;
        }
    }


    public static class B extends Ban {
    }


    public static class Ban extends CommandExecute {

        protected boolean hellban = false,


        ipBan = false;

        private String getCommand() {
            if (hellban) {
                return "HellBan";
            } else {
                return "Ban";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(5, "[用法] !" + getCommand() + " <IGN> <Reason>");
                return 0;
            }
            StringBuilder sb = new StringBuilder();
            if (hellban) {
                sb.append("Banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
            } else {
                sb.append(c.getPlayer().getName()).append(" banned ").append(splitted[1]).append(": ").append(StringUtil.joinStringFrom(splitted, 2));
            }
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (c.getPlayer().getGMLevel() > target.getGMLevel() || c.getPlayer().isAdmin()) {
                    sb.append(" (IP: ").append(target.getClient().getSessionIPAddress()).append(")");
                    if (target.ban(sb.toString(), hellban || ipBan, false, hellban)) {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] 已成功将玩家 " + splitted[1] + " 进行封号处理.");
                        return 1;
                    } else {
                        c.getPlayer().dropMessage(6, "[" + getCommand() + "] 封号失败.");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 对方的管理权限比你高无法对其进行封号...");
                    return 1;
                }
            } else {
                if (MapleCharacter.ban(splitted[1], sb.toString(), false, c.getPlayer().getGMLevel(), hellban)) { //c.getPlayer().isAdmin() ? 250 : 删除此项 以免封停玩家的IP错误
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 已成功将玩家 " + splitted[1] + " 进行离线封号.");
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "[" + getCommand() + "] 离线封号失败 " + splitted[1]);
                    return 0;
                }
            }
        }
    }


    public static class CC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(Integer.parseInt(splitted[1]));
            return 1;
        }
    }


    public static class CCPlayer extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().changeChannel(WorldFindService.getInstance().findChannel(splitted[1]));
            return 1;
        }
    }


    public static class DC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                victim.getClient().disconnect(true, false);
                victim.getClient().getSession().close();
                c.getPlayer().dropMessage(6, "已经成功断开 " + victim.getName() + " 的连接.");
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "使用的对象不存在或者角色名字错误或者对方的GM权限比你高.");
                return 0;
            }
        }
    }


    public static class Kill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !kill <list player names>");
                return 0;
            }
            MapleCharacter victim = null;
            for (int i = 1; i < splitted.length; i++) {
                try {
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "没有找到名字为: " + splitted[i] + " 的玩家.");
                }
                if (player.allowedToTarget(victim) && player.getGMLevel() >= victim.getGMLevel()) {
                    victim.updateSingleStat(MapleStat.HP, 0);
                    victim.updateSingleStat(MapleStat.MP, 0);
                    victim.getStat().setHp((short) 0, victim);
                    victim.getStat().setMp((short) 0, victim);
                    c.announce(EffectPacket.playerDeadConfirm(1, 0));
                }
            }
            return 1;
        }
    }


    public static class WhereAmI extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "当前地图ID: " + c.getPlayer().getMap().getId());
            return 1;
        }
    }


    public static class ClearInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Pair<Short, Short>, MapleInventoryType> eqs = new HashMap<>();
            switch (splitted[1]) {
                case "all":
                    for (MapleInventoryType type : MapleInventoryType.values()) {
                        for (Item item : c.getPlayer().getInventory(type)) {
                            eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), type);
                        }
                    }
                    break;
                case "eqp":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIPPED);
                    }
                    break;
                case "eq":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.EQUIP);
                    }
                    break;
                case "u":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.USE);
                    }
                    break;
                case "s":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.SETUP);
                    }
                    break;
                case "e":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.ETC);
                    }
                    break;
                case "c":
                    for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                        eqs.put(new Pair<>(item.getPosition(), item.getQuantity()), MapleInventoryType.CASH);
                    }
                    break;
                default:
                    c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
                    break;
            }
            for (Entry<Pair<Short, Short>, MapleInventoryType> eq : eqs.entrySet()) {
                MapleInventoryManipulator.removeFromSlot(c, eq.getValue(), eq.getKey().left, eq.getKey().right, false, false);
            }
            return 1;
        }
    }


    public static class 在线 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "频道在线 " + c.getChannel() + ":");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }


    public static class 频道在线 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "频道在线: " + Integer.parseInt(splitted[1]) + ":");
            c.getPlayer().dropMessage(6, ChannelServer.getInstance(Integer.parseInt(splitted[1])).getPlayerStorage().getOnlinePlayers(true));
            return 1;
        }
    }


    public static class ItemCheck extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                c.getPlayer().dropMessage(6, "用法: !itemcheck <玩家名字> <道具ID>");
                return 0;
            } else {
                int item = Integer.parseInt(splitted[2]);
                MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                int itemamount = chr.getItemQuantity(item, true);
                if (itemamount > 0) {
                    c.getPlayer().dropMessage(6, chr.getName() + " 拥有 " + itemamount + " (" + item + ").");
                } else {
                    c.getPlayer().dropMessage(6, chr.getName() + " 没有ID为 (" + item + ") 的道具.");
                }
            }
            return 1;
        }
    }


    public static class Song extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitted[1]));
            return 1;
        }
    }


    public static class CheckPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "请输入玩家的名字.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "当前频道么有找到这个玩家.");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " 拥有 " + chrs.getPoints() + " 点.");
            }
            return 1;
        }
    }


    public static class CheckVPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "请输入玩家的名字.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "当前频道么有找到这个玩家");
            } else {
                c.getPlayer().dropMessage(6, chrs.getName() + " 拥有 " + chrs.getVPoints() + " 点.");
            }
            return 1;
        }
    }


    public static class PermWeather extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getMap().getPermanentWeather() > 0) {
                c.getPlayer().getMap().setPermanentWeather(0);
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.removeMapEffect());
                c.getPlayer().dropMessage(5, "当前地图的效果已禁止.");
            } else {
                int weather = CommandProcessorUtil.getOptionalIntArg(splitted, 1, 5120000);
                if (!MapleItemInformationProvider.getInstance().itemExists(weather) || weather / 10000 != 512) {
                    c.getPlayer().dropMessage(5, "请输入ID.");
                } else {
                    c.getPlayer().getMap().setPermanentWeather(weather);
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.startMapEffect("", weather, false));
                    c.getPlayer().dropMessage(5, "当前地图的效果已开启.");
                }
            }
            return 1;
        }
    }


    public static class 角色信息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            StringBuilder builder = new StringBuilder();
            MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (other == null) {
                builder.append("输入的角色不存在...");
                c.getPlayer().dropMessage(6, builder.toString());
                return 0;
            }
            if (other.getClient().getLastPing() <= 0) {
                other.getClient().sendPing();
            }
            builder.append(MapleClient.getLogMessage(other, ""));
            builder.append(" 坐标 ").append(other.getPosition().x);
            builder.append(" /").append(other.getPosition().y);

            builder.append(" || 血 : ");
            builder.append(other.getStat().getHp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxHp());

            builder.append(" || 蓝 : ");
            builder.append(other.getStat().getMp());
            builder.append(" /");
            builder.append(other.getStat().getCurrentMaxMp(other.getJob()));

            builder.append(" || 物理攻击力 : ");
            builder.append(other.getStat().getTotalWatk());
            builder.append(" || 魔法攻击力 : ");
            builder.append(other.getStat().getTotalMagic());
            builder.append(" || 最大攻击 : ");
            builder.append(other.getStat().getCurrentMaxBaseDamage());
            builder.append(" || 伤害% : ");
            builder.append(other.getStat().getDamageRate());
            builder.append(" || BOSS伤害% : ");
            builder.append(other.getStat().getBossDamageRate());
            builder.append(" || 爆击几率 : ");
            builder.append(other.getStat().passive_sharpeye_rate());
            builder.append(" || 暴击伤害 : ");
            builder.append(other.getStat().passive_sharpeye_percent());

            builder.append(" || 力量 : ");
            builder.append(other.getStat().getStr());
            builder.append(" || 敏捷 : ");
            builder.append(other.getStat().getDex());
            builder.append(" || 智力 : ");
            builder.append(other.getStat().getInt());
            builder.append(" || 运气 : ");
            builder.append(other.getStat().getLuk());

            builder.append(" || 全部力量 : ");
            builder.append(other.getStat().getTotalStr());
            builder.append(" || 全部敏捷 : ");
            builder.append(other.getStat().getTotalDex());
            builder.append(" || 全部智力 : ");
            builder.append(other.getStat().getTotalInt());
            builder.append(" || 全部运气 : ");
            builder.append(other.getStat().getTotalLuk());

            builder.append(" || 经验 : ");
            builder.append(other.getExp());
            builder.append(" || 金币 : ");
            builder.append(other.getMeso());

            builder.append(" || 是否组队 : ");
            builder.append(other.getParty() == null ? -1 : other.getParty().getPartyId());

            builder.append(" || 是否交易: ");
            builder.append(other.getTrade() != null);
            builder.append(" || Latency: ");
            builder.append(other.getClient().getLatency());
            builder.append(" || PING: ");
            builder.append(other.getClient().getLastPing());
            builder.append(" || PONG: ");
            builder.append(other.getClient().getLastPong());

            c.getPlayer().dropMessage(6, builder.toString());
            return 1;
        }
    }


    public static class Reports extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getReports();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return 1;
        }
    }


    public static class ClearReport extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                StringBuilder ret = new StringBuilder("用法 !report [ign] [all/");
                for (ReportType type : ReportType.values()) {
                    ret.append(type.theId).append('/');
                }
                ret.setLength(ret.length() - 1);
                c.getPlayer().dropMessage(6, ret.append(']').toString());
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim == null) {
                c.getPlayer().dropMessage(5, "输入的角色不存在...");
                return 0;
            }
            ReportType type = ReportType.getByString(splitted[2]);
            if (type != null) {
                victim.clearReports(type);
            } else {
                victim.clearReports();
            }
            c.getPlayer().dropMessage(5, "完成.");
            return 1;
        }
    }


    public static class 检测作弊 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<CheaterData> cheaters = World.getCheaters();
            for (int x = cheaters.size() - 1; x >= 0; x--) {
                CheaterData cheater = cheaters.get(x);
                c.getPlayer().dropMessage(6, cheater.getInfo());
            }
            return 1;
        }
    }


    public static class 在线人数 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Map<Integer, Integer> connected = World.getConnected();
            StringBuilder conStr = new StringBuilder("在线人数: ");
            boolean first = true;
            for (int i : connected.keySet()) {
                if (!first) {
                    conStr.append(", ");
                } else {
                    first = false;
                }
                if (i == 0) {
                    conStr.append("总计: ");
                    conStr.append(connected.get(i));
                } else if (i == -10) {
                    conStr.append("商城: ");
                    conStr.append(connected.get(i));
                } else {
                    conStr.append("频道");
                    conStr.append(i);
                    conStr.append(": ");
                    conStr.append(connected.get(i));
                }
            }
            c.getPlayer().dropMessage(6, conStr.toString());
            return 1;
        }
    }


    public static class NearestPortal extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition());
            c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
            return 1;
        }
    }


    public static class SpawnDebug extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
            return 1;
        }
    }


    public static class FakeRelog extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().fakeRelog();
            return 1;
        }
    }


    public static class RemoveDrops extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "已成功将当前地图的 " + c.getPlayer().getMap().getNumItems() + " 个道具清除.");
            c.getPlayer().getMap().removeDrops();
            return 1;
        }
    }


    public static class ListSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (Entry<MapleSquadType, MapleSquad> squads : c.getChannelServer().getAllSquads().entrySet()) {
                c.getPlayer().dropMessage(5, "类型: " + squads.getKey().name() + ", 队长: " + squads.getValue().getLeader().getName() + ", 状态: " + squads.getValue().getStatus() + ", 注册成员数量: " + squads.getValue().getSquadSize() + ", 禁止成员数量: " + squads.getValue().getBannedMemberSize());
            }
            return 0;
        }
    }


    public static class ListInstances extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            EventManager em = c.getChannelServer().getEventSM().getEventManager(StringUtil.joinStringFrom(splitted, 1));
            if (em == null || em.getInstances().size() <= 0) {
                c.getPlayer().dropMessage(5, "没有找到.");
            } else {
                for (EventInstanceManager eim : em.getInstances()) {
                    c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + em.getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + em.getProperties().toString());
                }
            }
            return 0;
        }
    }


    public static class Uptime extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "Server has been up for " + StringUtil.getReadableMillis(ChannelServer.serverStartTime, System.currentTimeMillis()));
            return 1;
        }
    }


    public static class EventInstance extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getEventInstance() == null) {
                c.getPlayer().dropMessage(5, "没有找到.");
            } else {
                EventInstanceManager eim = c.getPlayer().getEventInstance();
                c.getPlayer().dropMessage(5, "Event " + eim.getName() + ", charSize: " + eim.getPlayers().size() + ", dcedSize: " + eim.getDisconnected().size() + ", mobSize: " + eim.getMobs().size() + ", eventManager: " + eim.getEventManager().getName() + ", timeLeft: " + eim.getTimeLeft() + ", iprops: " + eim.getProperties().toString() + ", eprops: " + eim.getEventManager().getProperties().toString());
            }
            return 1;
        }
    }


    public static class GoTo extends CommandExecute {

        private static final HashMap<String, Integer> gotomaps = new HashMap<>();

        static {
            gotomaps.put("gmmap", 180000000);
            gotomaps.put("southperry", 2000000);
            gotomaps.put("amherst", 1010000);
            gotomaps.put("henesys", 100000000);
            gotomaps.put("ellinia", 101000000);
            gotomaps.put("perion", 102000000);
            gotomaps.put("kerning", 103000000);
            gotomaps.put("harbor", 104000000);
            gotomaps.put("sleepywood", 105000000);
            gotomaps.put("florina", 120000300);
            gotomaps.put("orbis", 200000000);
            gotomaps.put("happyville", 209000000);
            gotomaps.put("elnath", 211000000);
            gotomaps.put("ludibrium", 220000000);
            gotomaps.put("aquaroad", 230000000);
            gotomaps.put("leafre", 240000000);
            gotomaps.put("mulung", 250000000);
            gotomaps.put("herbtown", 251000000);
            gotomaps.put("omegasector", 221000000);
            gotomaps.put("koreanfolktown", 222000000);
            gotomaps.put("newleafcity", 600000000);
            gotomaps.put("sharenian", 990000000);
            gotomaps.put("pianus", 230040420);
            gotomaps.put("horntail", 240060200);
            gotomaps.put("chorntail", 240060201);
            gotomaps.put("griffey", 240020101);
            gotomaps.put("manon", 240020401);
            gotomaps.put("zakum", 280030100);
            gotomaps.put("czakum", 280030001);
            gotomaps.put("papulatus", 220080001);
            gotomaps.put("showatown", 801000000);
            gotomaps.put("zipangu", 800000000);
            gotomaps.put("ariant", 260000100);
            gotomaps.put("nautilus", 120000000);
            gotomaps.put("boatquay", 541000000);
            gotomaps.put("malaysia", 550000000);
            gotomaps.put("erev", 130000000);
            gotomaps.put("ellin", 300000000);
            gotomaps.put("kampung", 551000000);
            gotomaps.put("singapore", 540000000);
            gotomaps.put("amoria", 680000000);
            gotomaps.put("timetemple", 270000000);
            gotomaps.put("pinkbean", 270050100);
            gotomaps.put("fm", 910000000);
            gotomaps.put("freemarket", 910000000);
            gotomaps.put("oxquiz", 109020001);
            gotomaps.put("ola", 109030101);
            gotomaps.put("fitness", 109040000);
            gotomaps.put("snowball", 109060000);
            gotomaps.put("golden", 950100000);
            gotomaps.put("phantom", 610010000);
            gotomaps.put("cwk", 610030000);
            gotomaps.put("rien", 140000000);
            gotomaps.put("edel", 310000000);
            gotomaps.put("ardent", 910001000);
            gotomaps.put("craft", 910001000);
            gotomaps.put("pvp", 960000000);
            gotomaps.put("future", 271000000);
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !goto <mapname>");
            } else {
                if (gotomaps.containsKey(splitted[1])) {
                    MapleMap target = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "输入的地图不存在.");
                        return 0;
                    }
                    MaplePortal targetPortal = target.getPortal(0);
                    c.getPlayer().changeMap(target, targetPortal);
                } else {
                    if (splitted[1].equals("locations")) {
                        c.getPlayer().dropMessage(6, "Use !goto <location>. Locations are as follows:");
                        StringBuilder sb = new StringBuilder();
                        for (String s : gotomaps.keySet()) {
                            sb.append(s).append(", ");
                        }
                        c.getPlayer().dropMessage(6, sb.substring(0, sb.length() - 2));
                    } else {
                        c.getPlayer().dropMessage(6, "Invalid command syntax - Use !goto <location>. For a list of locations, use !goto locations.");
                    }
                }
            }
            return 1;
        }
    }


    public static class MonsterDebug extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;

            if (splitted.length > 1) {
                //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "输入的地图ID无效.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                c.getPlayer().dropMessage(6, "怪物: " + mob.toString());
            }
            return 1;
        }
    }


    public static class 查看NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllNPCsThreadsafe()) {
                MapleNPC reactor2l = (MapleNPC) reactor1l;
                c.getPlayer().dropMessage(5, "NPC信息: 工作ID: " + reactor2l.getObjectId() + " npcID: " + reactor2l.getId() + " 位置: " + reactor2l.getPosition().toString() + " 名字: " + reactor2l.getName());
            }
            return 0;
        }
    }


    public static class 查看反应堆 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleMapObject reactor1l : c.getPlayer().getMap().getAllReactorsThreadsafe()) {
                MapleReactor reactor2l = (MapleReactor) reactor1l;
                c.getPlayer().dropMessage(5, "反应堆信息: 工作ID: " + reactor2l.getObjectId() + " reactorID: " + reactor2l.getReactorId() + " 位置: " + reactor2l.getPosition().toString() + " 状态: " + reactor2l.getState() + " 名字: " + reactor2l.getName());
            }
            return 0;
        }
    }


    public static class 查看传送门 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MaplePortal portal : c.getPlayer().getMap().getPortals()) {
                c.getPlayer().dropMessage(5, "传送门信息: ID: " + portal.getId() + " script: " + portal.getScriptName() + " name: " + portal.getName() + " pos: " + portal.getPosition().x + "," + portal.getPosition().y + " target: " + portal.getTargetMapId() + " / " + portal.getTarget());
            }
            return 0;
        }
    }


    public static class MyNPCPos extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Point pos = c.getPlayer().getPosition();
            c.getPlayer().dropMessage(6, "X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50) + " | FH: " + c.getPlayer().getFH());
            return 1;
        }
    }


    public static class Clock extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getClock(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 60)));
            return 1;
        }
    }


    public static class WarpHere extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null) {
                if (c.getPlayer().inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "请稍后在试.");
                    return 0;
                }
                victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
            } else {
                int ch = WorldFindService.getInstance().findChannel(splitted[1]);
                if (ch < 0) {
                    c.getPlayer().dropMessage(5, "没有找到玩家[" + splitted[1] + "],请确认玩家是否在线或者输入的角色名字是否正确.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                if (victim == null || victim.inPVP() || (!c.getPlayer().isGM() && (victim.isInBlockedMap() || victim.isGM()))) {
                    c.getPlayer().dropMessage(5, "请稍后在试.");
                    return 0;
                }
                c.getPlayer().dropMessage(5, "Victim is cross changing channel.");
                victim.dropMessage(5, "Cross changing channel.");
                if (victim.getMapId() != c.getPlayer().getMapId()) {
                    MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
                    victim.changeMap(mapp, mapp.findClosestPortal(c.getPlayer().getTruePosition()));
                }
                victim.changeChannel(c.getChannel());
            }
            return 1;
        }
    }


    public static class WarpHereAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            boolean all = splitted.length > 1;
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : cs.getPlayerStorage().getAllCharacters()) {
                    if (chr == null || chr.isGM() || (chr.isInBlockedMap() && !all)) {
                        continue;
                    }
                    chr.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestPortal(c.getPlayer().getTruePosition()));
                    chr.dropMessage(5, "您已被GM传送于此。");
                    if (chr.getClient().getChannel() != c.getChannel()) {
                        chr.changeChannel(c.getChannel());
                    }
                }
            }
            return 1;
        }
    }


    public static class WarpId extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !WarpId [角色ID]");
                return 0;
            }
            int victimId = Integer.parseInt(splitted[1]);
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterById(victimId);
            if (victim != null) {
                c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
            } else {
                int ch = WorldFindService.getInstance().findChannel(victimId);
                if (ch <= 0) {
                    c.getPlayer().dropMessage(6, "找不到该角色ID为: " + victimId + " 的信息.");
                    return 0;
                }
                victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(victimId);
                if (victim != null) {
                    c.getPlayer().dropMessage(6, "正在切换频道，请等待...");
                    if (victim.getMapId() != c.getPlayer().getMapId()) {
                        MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                        c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                    }
                    c.getPlayer().changeChannel(ch);
                }
            }
            return 1;
        }
    }


    public static class Warp extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel() && !victim.inPVP() && !c.getPlayer().inPVP()) {
                if (splitted.length == 2) {
                    c.getPlayer().changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getTruePosition()));
                } else {
                    MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(Integer.parseInt(splitted[2]));
                    if (target == null) {
                        c.getPlayer().dropMessage(6, "输入的地图不存在.");
                        return 0;
                    }
                    MaplePortal targetPortal = null;
                    if (splitted.length > 3) {
                        try {
                            targetPortal = target.getPortal(Integer.parseInt(splitted[3]));
                        } catch (IndexOutOfBoundsException e) {
                            // noop, assume the gm didn't know how many portals there are
                            c.getPlayer().dropMessage(5, "Invalid portal selected.");
                        } catch (NumberFormatException a) {
                            // noop, assume that the gm is drunk
                        }
                    }
                    if (targetPortal == null) {
                        targetPortal = target.getPortal(0);
                    }
                    victim.changeMap(target, targetPortal);
                }
            } else {
                try {
                    victim = c.getPlayer();
                    int ch = WorldFindService.getInstance().findChannel(splitted[1]);
                    if (ch < 0) {
                        MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                        if (target == null) {
                            c.getPlayer().dropMessage(6, "输入的地图不存在.");
                            return 0;
                        }
                        MaplePortal targetPortal = null;
                        if (splitted.length > 2) {
                            try {
                                targetPortal = target.getPortal(Integer.parseInt(splitted[2]));
                            } catch (IndexOutOfBoundsException e) {
                                // noop, assume the gm didn't know how many portals there are
                                c.getPlayer().dropMessage(5, "Invalid portal selected.");
                            } catch (NumberFormatException a) {
                                // noop, assume that the gm is drunk
                            }
                        }
                        if (targetPortal == null) {
                            targetPortal = target.getPortal(0);
                        }
                        c.getPlayer().changeMap(target, targetPortal);
                    } else {
                        victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().dropMessage(6, "正在切换频道，请等待...");
                        if (victim.getMapId() != c.getPlayer().getMapId()) {
                            MapleMap mapp = c.getChannelServer().getMapFactory().getMap(victim.getMapId());
                            c.getPlayer().changeMap(mapp, mapp.findClosestPortal(victim.getTruePosition()));
                        }
                        c.getPlayer().changeChannel(ch);
                    }
                } catch (Exception e) {
                    c.getPlayer().dropMessage(6, "出现错误: " + e.getMessage());
                    return 0;
                }
            }
            return 1;
        }
    }


    public static class 监禁 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "用法: !监禁 [玩家名字] [多少分钟, 0 = forever]");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            int minutes = Math.max(0, Integer.parseInt(splitted[2]));
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(GameConstants.JAIL);
                victim.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(minutes * 60));
                victim.changeMap(target, target.getPortal(0));
                victim.gainWarning(true);
            } else {
                c.getPlayer().dropMessage(6, "请确保要监禁的玩家处于在线状态.");
                return 0;
            }
            return 1;
        }
    }


    public static class ListAllSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (Entry<MapleSquadType, MapleSquad> squads : cserv.getAllSquads().entrySet()) {
                    c.getPlayer().dropMessage(5, "[频道 " + cserv.getChannel() + "] 类型: " + squads.getKey().name() + ", 队长: " + squads.getValue().getLeader().getName() + ", 状态: " + squads.getValue().getStatus() + ", 成员总数: " + squads.getValue().getSquadSize() + ", 拒绝成员总数: " + squads.getValue().getBannedMemberSize());
                }
            }
            return 1;
        }
    }


    public static class Say extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                StringBuilder sb = new StringBuilder();
                sb.append("[");
                if (!c.getPlayer().isGM()) {
                    sb.append("管理员 ");
                }
                sb.append(c.getPlayer().getName());
                sb.append("] ");
                sb.append(StringUtil.joinStringFrom(splitted, 1));
                WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(c.getPlayer().isGM() ? 6 : 5, sb.toString()));
            } else {
                c.getPlayer().dropMessage(6, "用法: say <message>");
                return 0;
            }
            return 1;
        }
    }


    public static class Letter extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "用法: !letter <color (green/red)> <word>");
                return 0;
            }
            int start, nstart;
            if (splitted[1].equalsIgnoreCase("green")) {
                start = 3991026;
                nstart = 3990019;
            } else if (splitted[1].equalsIgnoreCase("red")) {
                start = 3991000;
                nstart = 3990009;
            } else {
                c.getPlayer().dropMessage(6, "未知的颜色!");
                return 0;
            }
            String splitString = StringUtil.joinStringFrom(splitted, 2);
            List<Integer> chars = new ArrayList<>();
            splitString = splitString.toUpperCase();
            // System.out.println(splitString);
            for (int i = 0; i < splitString.length(); i++) {
                char chr = splitString.charAt(i);
                if (chr == ' ') {
                    chars.add(-1);
                } else if ((chr) >= 'A' && (chr) <= 'Z') {
                    chars.add((int) (chr));
                } else if ((chr) >= '0' && (chr) <= ('9')) {
                    chars.add((chr) + 200);
                }
            }
            final int w = 32;
            int dStart = c.getPlayer().getPosition().x - (splitString.length() / 2 * w);
            for (Integer i : chars) {
                if (i == -1) {
                    dStart += w;
                } else if (i < 200) {
                    int val = start + i - ('A');
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                } else if (i >= 200 && i <= 300) {
                    int val = nstart + i - ('0') - 200;
                    client.inventory.Item item = new client.inventory.Item(val, (byte) 0, (short) 1);
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), item, new Point(dStart, c.getPlayer().getPosition().y), false, false);
                    dStart += w;
                }
            }
            return 1;
        }
    }


    public static class Find extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length == 1) {
                c.getPlayer().dropMessage(6, splitted[0] + ": <NPC> <MOB> <ITEM> <MAP> <SKILL> <QUEST>");
            } else if (splitted.length == 2) {
                c.getPlayer().dropMessage(6, "Provide something to search.");
            } else {
                String type = splitted[1];
                String search = StringUtil.joinStringFrom(splitted, 2);
                MapleData data = null;
                MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/" + "String.wz"));
                c.getPlayer().dropMessage(6, "<<Type: " + type + " | Search: " + search + ">>");

                if (type.equalsIgnoreCase("NPC")) {
                    List<String> retNpcs = new ArrayList<>();
                    data = dataProvider.getData("Npc.img");
                    List<Pair<Integer, String>> npcPairList = new LinkedList<>();
                    for (MapleData npcIdData : data.getChildren()) {
                        npcPairList.add(new Pair<>(Integer.parseInt(npcIdData.getName()), MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> npcPair : npcPairList) {
                        if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight());
                        }
                    }
                    if (retNpcs != null && retNpcs.size() > 0) {
                        for (String singleRetNpc : retNpcs) {
                            c.getPlayer().dropMessage(6, singleRetNpc);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No NPC's Found");
                    }

                } else if (type.equalsIgnoreCase("MAP")) {
                    List<String> retMaps = new ArrayList<>();
                    data = dataProvider.getData("Map.img");
                    List<Pair<Integer, String>> mapPairList = new LinkedList<>();
                    for (MapleData mapAreaData : data.getChildren()) {
                        for (MapleData mapIdData : mapAreaData.getChildren()) {
                            mapPairList.add(new Pair<>(Integer.parseInt(mapIdData.getName()), MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME")));
                        }
                    }
                    for (Pair<Integer, String> mapPair : mapPairList) {
                        if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight());
                        }
                    }
                    if (retMaps != null && retMaps.size() > 0) {
                        for (String singleRetMap : retMaps) {
                            c.getPlayer().dropMessage(6, singleRetMap);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No Maps Found");
                    }
                } else if (type.equalsIgnoreCase("MOB")) {
                    List<String> retMobs = new ArrayList<>();
                    data = dataProvider.getData("Mob.img");
                    List<Pair<Integer, String>> mobPairList = new LinkedList<>();
                    for (MapleData mobIdData : data.getChildren()) {
                        mobPairList.add(new Pair<>(Integer.parseInt(mobIdData.getName()), MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME")));
                    }
                    for (Pair<Integer, String> mobPair : mobPairList) {
                        if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                            retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight());
                        }
                    }
                    if (retMobs != null && retMobs.size() > 0) {
                        for (String singleRetMob : retMobs) {
                            c.getPlayer().dropMessage(6, singleRetMob);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No Mobs Found");
                    }

                } else if (type.equalsIgnoreCase("ITEM")) {
                    List<String> retItems = new ArrayList<>();
                    for (Entry<String, String> entry : MapleItemInformationProvider.getInstance().getAllItemNames().entrySet()) {
                        if (entry.getValue().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(entry.getKey() + " - " + entry.getValue());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No Items Found");
                    }
                } else if (type.equalsIgnoreCase("QUEST")) {
                    List<String> retItems = new ArrayList<>();
                    for (MapleQuest itemPair : MapleQuest.getAllInstances()) {
                        if (itemPair.getName().length() > 0 && itemPair.getName().toLowerCase().contains(search.toLowerCase())) {
                            retItems.add(itemPair.getId() + " - " + itemPair.getName());
                        }
                    }
                    if (retItems != null && retItems.size() > 0) {
                        for (String singleRetItem : retItems) {
                            c.getPlayer().dropMessage(6, singleRetItem);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No Quests Found");
                    }
                } else if (type.equalsIgnoreCase("SKILL")) {
                    List<String> retSkills = new ArrayList<>();
                    for (Entry<Integer, String> skil : SkillFactory.getAllSkills().entrySet()) {
                        if (skil.getValue() != null && skil.getValue().toLowerCase().contains(search.toLowerCase())) {
                            retSkills.add(skil.getKey() + " - " + skil.getValue());
                        }
                    }
                    if (!retSkills.isEmpty() && retSkills.size() > 0) {
                        for (String singleRetSkill : retSkills) {
                            c.getPlayer().dropMessage(6, singleRetSkill);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "No skills Found");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "Sorry, that search call is unavailable");
                }
            }
            return 0;
        }
    }


    public static class ID extends Find {
    }


    public static class LookUp extends Find {
    }


    public static class Search extends Find {
    }


    public static class WhosFirst extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            //probably bad way to do it
            final long currentTime = System.currentTimeMillis();
            List<Pair<String, Long>> players = new ArrayList<>();
            for (MapleCharacter chr : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (!chr.isIntern()) {
                    players.add(new Pair<>(MapleCharacterUtil.makeMapleReadable(chr.getName()) + (currentTime - chr.getCheatTracker().getLastAttack() > 600000 ? " (AFK)" : ""), chr.getChangeTime()));
                }
            }
            players.sort(new WhoComparator());
            StringBuilder sb = new StringBuilder("List of people in this map in order, counting AFK (10 minutes):  ");
            for (Pair<String, Long> z : players) {
                sb.append(z.left).append(", ");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }


        public static class WhoComparator implements Comparator<Pair<String, Long>>, Serializable {

            /**
             *
             */
            private static final long serialVersionUID = -595003339775165157L;

            @Override
            public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                if (o1.right > o2.right) {
                    return 1;
                } else if (o1.right == o2.right) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }


    public static class WhosLast extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("用法: !whoslast [type] where type can be:  ");
                for (MapleSquadType t : MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquadType t = MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("用法: !whoslast [type] where type can be:  ");
                for (MapleSquadType z : MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queuedPlayers.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(6, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(6, "Queued players: " + t.queuedPlayers.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            for (Pair<String, String> z : t.queuedPlayers.get(c.getChannel())) {
                sb.append(z.left).append('(').append(z.right).append(')').append(", ");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }


    public static class WhosNext extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                StringBuilder sb = new StringBuilder("用法: !whosnext [type] where type can be:  ");
                for (MapleSquadType t : MapleSquadType.values()) {
                    sb.append(t.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            final MapleSquadType t = MapleSquadType.valueOf(splitted[1].toLowerCase());
            if (t == null) {
                StringBuilder sb = new StringBuilder("用法: !whosnext [type] where type can be:  ");
                for (MapleSquadType z : MapleSquadType.values()) {
                    sb.append(z.name()).append(", ");
                }
                c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
                return 0;
            }
            if (t.queue.get(c.getChannel()) == null) {
                c.getPlayer().dropMessage(6, "The queue has not been initialized in this channel yet.");
                return 0;
            }
            c.getPlayer().dropMessage(6, "Queued players: " + t.queue.get(c.getChannel()).size());
            StringBuilder sb = new StringBuilder("List of participants:  ");
            final long now = System.currentTimeMillis();
            for (Pair<String, Long> z : t.queue.get(c.getChannel())) {
                sb.append(z.left).append('(').append(StringUtil.getReadableMillis(z.right, now)).append(" ago),");
            }
            c.getPlayer().dropMessage(6, sb.toString().substring(0, sb.length() - 2));
            return 0;
        }
    }


    public static class WarpMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                MapleMap target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[1]));
                if (target == null) {
                    c.getPlayer().dropMessage(6, "输入的地图不存在.");
                    return 0;
                }
                MapleMap from = c.getPlayer().getMap();
                for (MapleCharacter chr : from.getCharactersThreadsafe()) {
                    chr.changeMap(target, target.getPortal(0));
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(5, "错误: " + e.getMessage());
                return 0; //assume drunk GM
            }
            return 1;
        }
    }


    public static class KillAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            if (splitted.length > 1) {
                int irange = Integer.parseInt(splitted[1]);
                if (splitted.length <= 2) {
                    range = irange * irange;
                } else {
                    map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                }
            }
            if (map == null) {
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (!mob.getStats().isBoss() || mob.getStats().isPartyBonus() || c.getPlayer().isGM()) {
                    map.killMonster(mob, c.getPlayer(), false, false, (byte) 1);
                }
            }
            return 1;
        }
    }


    public static class 查看封号 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[用法] !查看封号 <角色名字>");
                return 0;
            }
            String msg = MapleClient.getAccInfoByName(splitted[1], c.getPlayer().isAdmin());
            if (msg != null) {
                c.getPlayer().dropMessage(6, msg);
            } else {
                c.getPlayer().dropMessage(6, "输入的角色名字错误，无法找到信息.");
            }
            return 1;
        }
    }


    public static class 查看账号 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[用法] !查看账号 <玩家账号>");
                return 0;
            }
            String msg = MapleClient.getAccInfo(splitted[1], c.getPlayer().isAdmin());
            if (msg != null) {
                c.getPlayer().dropMessage(6, msg);
            } else {
                c.getPlayer().dropMessage(6, "输入的账号错误，无法找到信息.");
            }
            return 1;
        }
    }


    public static class 测谎仪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "!测谎仪 <角色名字>");
                return 0;
            }
            MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chr != null && c.getPlayer().getGMLevel() >= chr.getGMLevel()) {
                if (chr.getMapId() == GameConstants.JAIL) {
                    c.getPlayer().dropMessage(6, "玩家 " + chr.getName() + " 在监狱地图无法对其使用.");
                } else if (!chr.getAntiMacro().startLieDetector(chr.getName(), false, false)) {
                    c.getPlayer().dropMessage(6, "对玩家 " + chr.getName() + " 使用测谎仪失败.");
                } else {
                    c.getPlayer().dropMessage(6, "已成功对玩家 " + chr.getName() + " 使用测谎仪.");
                }
            } else {
                c.getPlayer().dropMessage(6, "请确保要测谎的玩家处于在线状态.");
                return 0;
            }
            return 1;
        }
    }

    /*
     * 解除封号
     */


    public static class HellB extends HellBan {
    }


    public static class HellBan extends Ban {


        public HellBan() {
            hellban = true;
        }
    }


    public static class UnHellB extends UnHellBan {
    }


    public static class UnHellBan extends UnBan {


        public UnHellBan() {
            hellban = true;
        }
    }


    public static class UnB extends UnBan {
    }


    public static class UnBan extends CommandExecute {


        protected boolean hellban = false;

        private String getCommand() {
            if (hellban) {
                return "UnHellBan";
            } else {
                return "UnBan";
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[用法] !" + getCommand() + " <IGN>");
                return 0;
            }
            byte ret;
            if (hellban) {
                ret = MapleClient.unHellban(splitted[1]);
            } else {
                ret = MapleClient.unban(splitted[1]);
            }
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 数据库查询出错.");
                return 0;
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 角色 " + splitted[1] + " 不存在.");
                return 0;
            } else {
                c.getPlayer().dropMessage(6, "[" + getCommand() + "] 已经成功将玩家 " + splitted[1] + " 解除封停!");
                log.info("[命令] 管理员 " + c.getPlayer().getName() + " 将玩家 " + splitted[1] + " 解除封号.");
            }
            byte ret_ = MapleClient.unbanIPMacs(splitted[1]);
            if (ret_ == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] 数据库查询出错.");
            } else if (ret_ == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] 输入的角色不存在.");
            } else if (ret_ == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret_ == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret_ == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            return ret_ > 0 ? 1 : 0;
        }
    }


    public static class UnbanIP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "[用法] !unbanip <IGN>");
                return 0;
            }
            byte ret = MapleClient.unbanIPMacs(splitted[1]);
            if (ret == -2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] 数据库查询出错.");
            } else if (ret == -1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] 输入的角色不存在.");
            } else if (ret == 0) {
                c.getPlayer().dropMessage(6, "[UnbanIP] No IP or Mac with that character exists!");
            } else if (ret == 1) {
                c.getPlayer().dropMessage(6, "[UnbanIP] IP/Mac -- one of them was found and unbanned.");
            } else if (ret == 2) {
                c.getPlayer().dropMessage(6, "[UnbanIP] Both IP and Macs were unbanned.");
            }
            if (ret > 0) {
                return 1;
            }
            return 0;
        }
    }
}
