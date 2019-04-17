package server.commands;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.skills.SkillFactory;
import configs.Config;
import configs.OpcodeConfig;
import configs.ServerConfig;
import constants.ServerConstants;
import handling.MapleServerHandler;
import handling.channel.ChannelServer;
import handling.opcode.CashShopOpcode;
import handling.opcode.RecvPacketOpcode;
import handling.opcode.SendPacketOpcode;
import handling.world.WorldBroadcastService;
import handling.world.WorldFindService;
import scripting.npc.NPCScriptManager;
import scripting.portal.PortalScriptManager;
import scripting.reactor.ReactorScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MaplePortal;
import server.ShutdownServer;
import server.Timer.*;
import server.life.MapleMonsterInformationProvider;
import server.shop.MapleShopFactory;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.UIPacket;
import tools.performance.CPUSampler;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Emilyx3
 */
public class AdminCommand {

    /**
     * @return
     */
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }


    public static class StripEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ChannelServer cs = c.getChannelServer();
            for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters()) {
                if (mchr.isGM()) {
                    continue;
                }
                MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                MapleInventory equip = mchr.getInventory(MapleInventoryType.EQUIP);
                List<Short> ids = new ArrayList<>();
                for (Item item : equipped.newList()) {
                    ids.add(item.getPosition());
                }
                for (short id : ids) {
                    MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot());
                }
            }
            return 1;
        }
    }


    public static class MesoEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !MesoEveryone [金币数量]");
                return 0;
            }
            int meso;
            try {
                meso = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "输入的金币数量无效.");
                return 0;
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainMeso(meso, true);
                }
            }
            return 1;
        }
    }


    public static class 测试距离 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !测试距离 x坐标 y坐标  说明: 此命令可以测试输入的坐标信息和角色当前坐标的距离");
                return 0;
            }
            int x = Integer.parseInt(splitted[1]);
            int y = Integer.parseInt(splitted[2]);
            Point test = new Point(x, y);
            c.getPlayer().dropMessage(6, "当前距离: " + c.getPlayer().getTruePosition().distanceSq(test));
            return 1;
        }
    }


    public static class CashEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !CashEveryone [点卷类型1-2] [点卷数量]");
                return 0;
            }
            int type = Integer.parseInt(splitted[1]);
            int quantity = Integer.parseInt(splitted[2]);
            if (type <= 0 || type > 2) {
                type = 2;
            }
            int ret = 0;
            StringBuilder sb = new StringBuilder();
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.modifyCSPoints(type, quantity, false);
                    mch.dropMessage(-11, "[系统提示] 恭喜您获得管理员赠送给您的" + (type == 1 ? "点券 " : " 抵用券 ") + quantity + " 点.");
                    ret++;
                    sb.append(MapleCharacterUtil.makeMapleReadable(mch.getName()));
                    sb.append(", ");
                }
            }
            c.getPlayer().dropMessage(6, "以下是获得" + (type == 1 ? "点券 " : " 抵用券 ") + "的玩家名单:");
            c.getPlayer().dropMessage(6, sb.toString());
            c.getPlayer().dropMessage(6, "命令使用成功，当前共有: " + ret + " 个玩家获得: " + quantity + " 点的" + (type == 1 ? "点券 " : " 抵用券 ") + " 总计: " + (ret * quantity));
            return 1;
        }
    }


    public static class PayEveryone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !PayEveryone [数量]");
                return 0;
            }
            int hb;
            try {
                hb = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "输入的金币数量无效.");
                return 0;
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters()) {
                    mch.gainRMB(hb);
                    mch.dropMessage(-11, "[系统提示] 恭喜您获得管理员赠送给您的冒险币" + hb + "个。");
                }
            }
            return 1;
        }
    }


    public static class ExpRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setExpRate(rate);
                    }
                } else {
                    c.getChannelServer().setExpRate(rate);
                }
                c.getPlayer().dropMessage(6, "经验倍率已经修改为: " + rate + "倍.");
            } else {
                c.getPlayer().dropMessage(6, "用法: !exprate <number> [all]");
            }
            return 1;
        }
    }


    public static class MesoRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setMesoRate(rate);
                    }
                } else {
                    c.getChannelServer().setMesoRate(rate);
                }
                c.getPlayer().dropMessage(6, "金币爆率已经修改为: " + rate + "倍.");
            } else {
                c.getPlayer().dropMessage(6, "用法: !mesorate <number> [all]");
            }
            return 1;
        }
    }


    public static class DropRate extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                int rate = Integer.parseInt(splitted[1]);
                if (splitted.length > 2 && splitted[2].equalsIgnoreCase("all")) {
                    for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                        cserv.setDropRate(rate);
                    }
                } else {
                    c.getChannelServer().setDropRate(rate);
                }
                c.getPlayer().dropMessage(6, "怪物爆率已经修改为: " + rate + "倍.");
            } else {
                c.getPlayer().dropMessage(6, "用法: !droprate <number> [all]");
            }
            return 1;
        }
    }


    public static class 双倍经验 extends CommandExecute {

        private int change = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            change = Integer.parseInt(splitted[1]);
            if (change == 1 || change == 2) {
                c.getPlayer().dropMessage(6, "以前 - 经验: " + c.getChannelServer().getExpRate() + " 金币: " + c.getChannelServer().getMesoRate() + " 爆率: " + c.getChannelServer().getDropRate());
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.setDoubleExp(change);
                }
                c.getPlayer().dropMessage(6, "现在 - 经验: " + c.getChannelServer().getExpRate() + " 金币: " + c.getChannelServer().getMesoRate() + " 爆率: " + c.getChannelServer().getDropRate());
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "输入的数字无效，1为关闭活动经验，2为开启活动经验。当前输入为: " + change);
                return 0;
            }
        }
    }


    public static class 经验信息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "当前游戏设置信息:");
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                String rateStr = "频道 " + cserv.getChannel() +
                        " 经验: " +
                        cserv.getExpRate() +
                        " 金币: " +
                        cserv.getMesoRate() +
                        " 爆率: " +
                        cserv.getDropRate() +
                        " 活动: " +
                        cserv.getDoubleExp();
                c.getPlayer().dropMessage(5, rateStr);
            }
            return 1;
        }
    }


    public static class DCAll extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int range = -1;
            switch (splitted[1]) {
                case "m":
                    range = 0;
                    break;
                case "c":
                    range = 1;
                    break;
                case "w":
                    range = 2;
                    break;
            }
            if (range == -1) {
                range = 1;
            }
            if (range == 0) {
                c.getPlayer().getMap().disconnectAll();
                c.getPlayer().dropMessage(5, "已成功断开当前地图所有玩家的连接.");
            } else if (range == 1) {
                c.getChannelServer().getPlayerStorage().disconnectAll(true);
                c.getPlayer().dropMessage(5, "已成功断开当前频道所有玩家的连接.");
            } else if (range == 2) {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    cserv.getPlayerStorage().disconnectAll(true);
                }
                c.getPlayer().dropMessage(5, "已成功断开当前游戏所有玩家的连接.");
            }
            return 1;
        }
    }


    public static class 查看爆率 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9010000, "1");
            return 1;
        }
    }


    public static class Shutdown extends CommandExecute {


        protected static Thread t = null;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "游戏即将关闭...");
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            } else {
                c.getPlayer().dropMessage(6, "已经使用过一次这个命令，暂时无法使用.");
            }
            return 1;
        }
    }


    public static class ShutdownTime extends Shutdown {

        private static ScheduledFuture<?> ts = null;
        private int minutesLeft = 0;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            minutesLeft = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(6, "游戏将在 " + minutesLeft + " 分钟之后关闭...");
            if (ts == null && (t == null || !t.isAlive())) {
                t = new Thread(ShutdownServer.getInstance());
                ts = EventTimer.getInstance().register(() -> {
                    if (minutesLeft == 0) {
                        ShutdownServer.getInstance().shutdown();
                        t.start();
                        ts.cancel(false);
                        return;
                    }
                    WorldBroadcastService.getInstance().broadcastMessage(UIPacket.clearMidMsg());
                    WorldBroadcastService.getInstance().broadcastMessage(UIPacket.getMidMsg("游戏将于 " + minutesLeft + " 分钟之后关闭维护.请玩家安全下线.", true, 0));
                    WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(0, " 游戏将于 " + minutesLeft + " 分钟之后关闭维护.请玩家安全下线."));
                    minutesLeft--;
                }, 60000);
            } else {
                c.getPlayer().dropMessage(6, "已经使用过一次这个命令，暂时无法使用.");
            }
            return 1;
        }
    }


    public static class StartProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            sampler.addIncluded("client");
            sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
            sampler.addIncluded("database");
            sampler.addIncluded("handling");
            sampler.addIncluded("provider");
            sampler.addIncluded("scripting");
            sampler.addIncluded("server");
            sampler.addIncluded("tools");
            sampler.start();
            c.getPlayer().dropMessage(6, "已经开启服务端性能监测.");
            return 1;
        }
    }


    public static class StopProfiling extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            CPUSampler sampler = CPUSampler.getInstance();
            try {
                String filename = "odinprofile.txt";
                if (splitted.length > 1) {
                    filename = splitted[1];
                }
                File file = new File(filename);
                if (file.exists()) {
                    c.getPlayer().dropMessage(6, "输入的文件名字已经存在，请重新输入1个新的文件名。");
                    return 0;
                }
                sampler.stop();
                FileWriter fw = new FileWriter(file);
                sampler.save(fw, 1, 10);
                fw.close();
            } catch (IOException e) {
                System.err.println("保存文件出错." + e);
            }
            sampler.reset();
            c.getPlayer().dropMessage(6, "已经停止服务端性能监测.");
            return 1;
        }
    }


    public static class Subcategory extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSubcategory(Byte.parseByte(splitted[1]));
            return 1;
        }
    }


    public static class 刷金币 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().gainMeso(ServerConfig.CHANNEL_PLAYER_MAXMESO - c.getPlayer().getMeso(), true);
            return 1;
        }
    }


    public static class 刷点卷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }


    public static class 刷抵用卷 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
            return 1;
        }
    }


    public static class GainP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().setPoints(c.getPlayer().getPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }


    public static class GainVP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().setVPoints(c.getPlayer().getVPoints() + Integer.parseInt(splitted[1]));
            return 1;
        }
    }


    public static class 重载包头 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (ServerConstants.isShowPacket()) {
                OpcodeConfig.load();
            }
            RecvPacketOpcode.reloadValues();
            SendPacketOpcode.reloadValues();
            CashShopOpcode.reloadValues();
            c.getPlayer().dropMessage(5, "重新获取包头完成.");
            return 1;
        }
    }


    public static class 重载爆率 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            ReactorScriptManager.getInstance().clearDrops();
            c.getPlayer().dropMessage(5, "重新加载爆率完成.");
            return 1;
        }
    }


    public static class 重载传送 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            PortalScriptManager.getInstance().clearScripts();
            c.getPlayer().dropMessage(5, "重新加载传送点脚本完成.");
            return 1;
        }
    }


    public static class 重载商店 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleShopFactory.getInstance().clear();
            c.getPlayer().dropMessage(5, "重新加载商店贩卖道具完成.");
            return 1;
        }
    }


    public static class 重载活动 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
//            ServerProperties.loadProperties("world.properties");
//            for (ChannelServer instance : ChannelServer.getAllInstances()) {
//                instance.reloadEvents();
//            }
//            c.getPlayer().dropMessage(5, "重新加载活动脚本完成.");
            c.getPlayer().dropMessage(5, "该命令已被禁止.");
            return 1;
        }
    }


    public static class resetEvent extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Config.load();
            for (ChannelServer instance : ChannelServer.getAllInstances()) {
                instance.reloadEvents();
            }
            c.getPlayer().dropMessage(5, "重新加载活动脚本完成.");
            return 1;
        }
    }


    public static class 封包测试 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.StartWindow();
            return 1;
        }
    }


    public static class 重载复制 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            SkillFactory.loadMemorySkills();
            c.getPlayer().dropMessage(5, "加载复制技能完成...");
            return 1;
        }
    }

    public static class 重载封包禁止 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            OpcodeConfig.load();
            return 1;
        }
    }


    public static class 在线玩家 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int total = 0;
            c.getPlayer().dropMessage(6, "---------------------------------------------------------------------------------------");
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                int curConnected = cserv.getConnectedClients();
                c.getPlayer().dropMessage(6, "频道: " + cserv.getChannel() + " 在线人数: " + curConnected);
                total += curConnected;
                for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                    if (chr != null) {
                        StringBuilder ret = new StringBuilder();
                        ret.append(StringUtil.getRightPaddedStr(chr.getName(), ' ', 13));
                        ret.append(" ID: ");
                        ret.append(chr.getId());
                        ret.append(" 等级: ");
                        ret.append(StringUtil.getRightPaddedStr(String.valueOf(chr.getLevel()), ' ', 3));
                        if (chr.getMap() != null) {
                            ret.append(" 地图: ");
                            ret.append(chr.getMapId());
                            ret.append(" - ");
                            ret.append(chr.getMap().getMapName());
                        }
                        c.getPlayer().dropMessage(6, ret.toString());
                    }
                }
            }
            c.getPlayer().dropMessage(6, "当前服务器总计在线: " + total);
            c.getPlayer().dropMessage(6, "---------------------------------------------------------------------------------------");
            return 1;
        }
    }

    /*
     * 测试倾向系统用
     */


    public static class 增加经验 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().addTraitExp(Integer.parseInt(splitted[1]));
            return 1;
        }
    }


    public static class 设置经验 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入数量.");
                return 0;
            }
            c.getPlayer().setTraitExp(Integer.parseInt(splitted[1]));
            return 1;
        }
    }


    public static class 检测复制 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<String> msgs = new ArrayList<>();
            Map<Integer, CopyItemInfo> checkItems = new LinkedHashMap<>();
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter player : cserv.getPlayerStorage().getAllCharacters()) {
                    if (player != null && player.getMap() != null) {
                        //检测背包装备
                        check(player.getInventory(MapleInventoryType.EQUIP), player, checkItems, msgs);
                        //检测身上的装备
                        check(player.getInventory(MapleInventoryType.EQUIPPED), player, checkItems, msgs);
                    }
                }
            }
            checkItems.clear();
            if (msgs.size() > 0) {
                c.getPlayer().dropMessage(5, "检测完成，共有: " + msgs.size() + " 个复制信息");
//                FileoutputUtil.log("装备复制.txt", "检测完成，共有: " + msgs.size() + " 个复制信息", true);
                for (String s : msgs) {
                    c.getPlayer().dropMessage(5, s);
//                    FileoutputUtil.log("装备复制.txt", s, true);
                }
                c.getPlayer().dropMessage(5, "以上信息为拥有复制道具的玩家.");
            } else {
                c.getPlayer().dropMessage(5, "未检测到游戏中的角色有复制的道具信息.");
            }
            return 1;
        }

        public void check(MapleInventory equip, MapleCharacter player, Map<Integer, CopyItemInfo> checkItems, List<String> msgs) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            for (Item item : equip.list()) {
                if (item.getEquipOnlyId() > 0) {
                    CopyItemInfo ret = new CopyItemInfo(item.getItemId(), player.getId(), player.getName());
                    if (checkItems.containsKey(item.getEquipOnlyId())) {
                        ret = checkItems.get(item.getEquipOnlyId());
                        if (ret.itemId == item.getItemId()) {
                            if (ret.isFirst()) {
                                ret.setFirst(false);
                                msgs.add("角色: " + StringUtil.getRightPaddedStr(ret.name, ' ', 13) + " 角色ID: " + StringUtil.getRightPaddedStr(String.valueOf(ret.chrId), ' ', 6) + " 道具: " + ret.itemId + " - " + ii.getName(ret.itemId) + " 唯一ID: " + item.getEquipOnlyId());
                            } else {
                                msgs.add("角色: " + StringUtil.getRightPaddedStr(player.getName(), ' ', 13) + " 角色ID: " + StringUtil.getRightPaddedStr(String.valueOf(player.getId()), ' ', 6) + " 道具: " + item.getItemId() + " - " + ii.getName(item.getItemId()) + " 唯一ID: " + item.getEquipOnlyId());
                            }
                        }
                    } else {
                        checkItems.put(item.getEquipOnlyId(), ret);
                    }
                }
            }
        }
    }


    public static class 查看进程 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "正在查看自己的进程信息.");
                c.announce(MaplePacketCreator.SystemProcess());
                return 0;
            }
            String name = splitted[1];
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "当前频道没有找到玩家[" + name + "]的信息.");
            } else {
                c.getPlayer().dropMessage(6, "正在查看玩家[" + name + "]的进程信息.");
                chrs.getClient().announce(MaplePacketCreator.SystemProcess());
            }
            return 1;
        }
    }


    public static class 重载配置 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Config.load();
            c.getPlayer().dropMessage(6, "重新加载配置文件完成.");
            return 1;
        }
    }


    public static class 异常信息 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            ServerConstants.setShowGMMessage(!ServerConstants.isShowGMMessage());
            c.getPlayer().dropMessage(6, "显示异常信息功能：" + (ServerConstants.isShowGMMessage() ? "已开启" : "已关闭"));
            return 1;
        }
    }


    public static class 重载商城封包 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
//            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
//                cserv.loadShopPack();
//            }
            c.getPlayer().dropMessage(6, "重新加载商城封包完成.");
            return 1;
        }
    }


    public static class 管理登陆模式 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int change = Integer.parseInt(splitted[1]);
            if (change == 1 || change == 2) {
                c.getPlayer().dropMessage(6, "以前 - 是否管理登陆: " + ServerConfig.WORLD_ONLYADMIN);
                ServerConfig.WORLD_ONLYADMIN = (change == 2);
                c.getPlayer().dropMessage(6, "现在 - 是否管理登陆: " + ServerConfig.WORLD_ONLYADMIN);
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "输入的数字无效，1为关闭管理登陆模式，2为开启管理登陆模式。当前输入为: " + change);
                return 0;
            }
        }
    }


    public static class 重载特殊爆率 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonsterInformationProvider.getInstance().clearDrops();
            c.getPlayer().dropMessage(6, "重新加载特殊爆率完成.");
            return 1;
        }
    }


    public static class 查看装备 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "!查看角色装备 [角色ID] [装备位置] ");
                return 0;
            }
            if (splitted[1].matches("/^([0-9])*$") || splitted[2].matches("/^([0-9])*$")) {
                c.getPlayer().dropMessage(6, "输入错误，角色ID和装备位置必须为数字！");
                return 0;
            }
            int victimId = Integer.parseInt(splitted[1]);
            int position = Integer.parseInt(splitted[2]);
            int ch = WorldFindService.getInstance().findChannel(victimId);
            if (ch <= 0) {
                c.getPlayer().dropMessage(6, "找不到该角色ID为: " + victimId + " 的信息.");
                return 0;
            }
            MapleCharacter victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(victimId);
            if (victim != null) {
                byte pos = (byte) (position * (position > 0 ? -1 : 1));
                Item item = victim.getInventory(MapleInventoryType.EQUIPPED).getItem(pos);
                if (item == null) {
                    c.getPlayer().dropMessage(6, "[查看装备] 玩家 : " + victim.getName() + " ID: " + victimId + " 身上装备位置为 " + position + " 空或者输入位置错误");
                    return 0;
                }
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                c.announce(MaplePacketCreator.getGachaponMega("[查看装备]", " : 玩家 " + victim.getName() + " 身上装备位置为 " + position + " 的装备{" + ii.getName(item.getItemId()) + "}信息..", item, 3, c.getChannel()));
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "找不到该角色ID为: " + victimId + " 的信息.");
            }
            return 0;
        }
    }


    public static class 坐标传送 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "用法: !坐标传送 [要传送的坐标点] 或者 !坐标传送 [X坐标] [Y坐标] ");
                return 0;
            }
            if (splitted.length == 2) {
                int portalId;
                try {
                    portalId = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    c.getPlayer().dropMessage(6, "输入的传送点ID无效.");
                    return 0;
                }
                int maxPortalId = c.getPlayer().getMap().getPortals().size();
                MaplePortal portal = c.getPlayer().getMap().getPortal(portalId);
                if (portal == null || portalId > maxPortalId) {
                    c.getPlayer().dropMessage(6, "输入的传送点ID[" + portalId + "]无效或者当前地图没这个传送点,当前地图最大传送点为 ：" + maxPortalId);
                    return 0;
                }
                c.getPlayer().instantMapWarp(portal.getPosition());
            } else {
                int posX;
                int posY;
                try {
                    posX = Integer.parseInt(splitted[1]);
                } catch (NumberFormatException nfe) {
                    c.getPlayer().dropMessage(6, "输入的X坐标无效.");
                    return 0;
                }
                try {
                    posY = Integer.parseInt(splitted[2]);
                } catch (NumberFormatException nfe) {
                    c.getPlayer().dropMessage(6, "输入的Y坐标无效.");
                    return 0;
                }
                Point portalPos = new Point(posX, posY);
                c.getPlayer().instantMapWarp(portalPos);
            }
            return 1;
        }
    }


    public static class 查看线程 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入查看线程的参数[1-7]");
                return 0;
            }
            int poolId;
            try {
                poolId = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "输入查看的线程ID错误.");
                return 0;
            }
            ScheduledThreadPoolExecutor scheduledPool;
            String name;
            switch (poolId) {
                case 1:
                    name = "World Timer";
                    scheduledPool = WorldTimer.getInstance().getSES();
                    break;
                case 2:
                    name = "Etc Timer";
                    scheduledPool = EtcTimer.getInstance().getSES();
                    break;
                case 3:
                    name = "Map Timer";
                    scheduledPool = MapTimer.getInstance().getSES();
                    break;
                case 4:
                    name = "Clone Timer";
                    scheduledPool = CloneTimer.getInstance().getSES();
                    break;
                case 5:
                    name = "Event Timer";
                    scheduledPool = EventTimer.getInstance().getSES();
                    break;
                case 6:
                    name = "Buff Timer";
                    scheduledPool = BuffTimer.getInstance().getSES();
                    break;
                case 7:
                    name = "Ping Timer";
                    scheduledPool = PingTimer.getInstance().getSES();
                    break;
                default:
                    return 0;
            }
            if (scheduledPool != null) {
                c.getPlayer().dropMessage(-11, "--------------------------------------------------------------------");
                c.getPlayer().dropMessage(-11, "查看的线程名称 : " + name + " 输入的参数: " + poolId);
                c.getPlayer().dropMessage(-11, "ActiveCount: ...... " + StringUtil.getRightPaddedStr(scheduledPool.getActiveCount(), ' ', 8) + " 注释: 主动执行任务的线程数");
                c.getPlayer().dropMessage(-11, "CorePoolSize: ..... " + StringUtil.getRightPaddedStr(scheduledPool.getCorePoolSize(), ' ', 8) + " 注释: 核心线程数");
                c.getPlayer().dropMessage(-11, "PoolSize: ......... " + StringUtil.getRightPaddedStr(scheduledPool.getPoolSize(), ' ', 8) + " 注释: 池中的当前启动的核心线程数");
                c.getPlayer().dropMessage(-11, "LargestPoolSize: .. " + StringUtil.getRightPaddedStr(scheduledPool.getLargestPoolSize(), ' ', 8) + " 注释: 曾经同时位于池中的最大核心线程数");
                c.getPlayer().dropMessage(-11, "MaximumPoolSize: .. " + StringUtil.getRightPaddedStr(scheduledPool.getMaximumPoolSize(), ' ', 8) + " 注释: 允许的最大核心线程数线程数");
                c.getPlayer().dropMessage(-11, "CompletedTaskCount: " + StringUtil.getRightPaddedStr(scheduledPool.getCompletedTaskCount(), ' ', 8) + " 注释: 已完成执行的线程任务总数");
                c.getPlayer().dropMessage(-11, "QueuedTaskCount: ..." + StringUtil.getRightPaddedStr(scheduledPool.getQueue().size(), ' ', 8) + " 注释: 使用的任务队列数量");
                c.getPlayer().dropMessage(-11, "TaskCount: ........ " + StringUtil.getRightPaddedStr(scheduledPool.getTaskCount(), ' ', 8) + " 注释: 计划执行的线程任务总数");
            }
            return 1;
        }
    }


    public static class 清理线程 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "请输入查看线程的参数[1-7]");
                return 0;
            }
            int poolId;
            try {
                poolId = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(6, "输入查看的线程ID错误.");
                return 0;
            }
            ScheduledThreadPoolExecutor scheduledPool;
            String name;
            switch (poolId) {
                case 1:
                    name = "World Timer";
                    scheduledPool = WorldTimer.getInstance().getSES();
                    break;
                case 2:
                    name = "Etc Timer";
                    scheduledPool = EtcTimer.getInstance().getSES();
                    break;
                case 3:
                    name = "Map Timer";
                    scheduledPool = MapTimer.getInstance().getSES();
                    break;
                case 4:
                    name = "Clone Timer";
                    scheduledPool = CloneTimer.getInstance().getSES();
                    break;
                case 5:
                    name = "Event Timer";
                    scheduledPool = EventTimer.getInstance().getSES();
                    break;
                case 6:
                    name = "Buff Timer";
                    scheduledPool = BuffTimer.getInstance().getSES();
                    break;
                case 7:
                    name = "Ping Timer";
                    scheduledPool = PingTimer.getInstance().getSES();
                    break;
                default:
                    return 0;
            }
            if (scheduledPool != null) {
                scheduledPool.purge();
                c.getPlayer().dropMessage(-11, "--------------------------------------------------------------------");
                c.getPlayer().dropMessage(-11, "清理的线程名称 : " + name + " 输入的参数: " + poolId);
                c.getPlayer().dropMessage(-11, "ActiveCount: ...... " + StringUtil.getRightPaddedStr(scheduledPool.getActiveCount(), ' ', 8) + " 注释: 主动执行任务的线程数");
                c.getPlayer().dropMessage(-11, "CorePoolSize: ..... " + StringUtil.getRightPaddedStr(scheduledPool.getCorePoolSize(), ' ', 8) + " 注释: 核心线程数");
                c.getPlayer().dropMessage(-11, "PoolSize: ......... " + StringUtil.getRightPaddedStr(scheduledPool.getPoolSize(), ' ', 8) + " 注释: 池中的当前启动的核心线程数");
                c.getPlayer().dropMessage(-11, "LargestPoolSize: .. " + StringUtil.getRightPaddedStr(scheduledPool.getLargestPoolSize(), ' ', 8) + " 注释: 曾经同时位于池中的最大核心线程数");
                c.getPlayer().dropMessage(-11, "MaximumPoolSize: .. " + StringUtil.getRightPaddedStr(scheduledPool.getMaximumPoolSize(), ' ', 8) + " 注释: 允许的最大核心线程数线程数");
                c.getPlayer().dropMessage(-11, "CompletedTaskCount: " + StringUtil.getRightPaddedStr(scheduledPool.getCompletedTaskCount(), ' ', 8) + " 注释: 已完成执行的线程任务总数");
                c.getPlayer().dropMessage(-11, "QueuedTaskCount: ..." + StringUtil.getRightPaddedStr(scheduledPool.getQueue().size(), ' ', 8) + " 注释: 使用的任务队列数量");
                c.getPlayer().dropMessage(-11, "TaskCount: ........ " + StringUtil.getRightPaddedStr(scheduledPool.getTaskCount(), ' ', 8) + " 注释: 计划执行的线程任务总数");
            }
            return 1;
        }
    }


    public static class 重置IP黑名单 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleServerHandler.BlockIPList.clear();
            c.getPlayer().dropMessage(6, "清除完毕.");
            return 1;
        }
    }


    public static class 重载技能 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2 || !StringUtil.isNumber(splitted[1])) {
                c.getPlayer().dropMessage(5, "格式: !重载技能 <技能ID>");
                return 0;
            }
            SkillFactory.reloadSkills(Integer.valueOf(splitted[1]));
            c.getPlayer().dropMessage(6, "重载完毕.");
            return 1;
        }
    }

    public static class 禁止技能 extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length != 2 || !StringUtil.isNumber(splitted[1])) {
                c.getPlayer().dropMessage(5, "格式: !禁止技能 <技能ID>");
                return 0;
            }
            int skillid = Integer.valueOf(splitted[1]);
            if (SkillFactory.getSkill(skillid) == null) {
                c.getPlayer().dropMessage(5, "不存在的技能,无法禁用");
                return 0;
            }
            if (!SkillFactory.addBlockedSkill(Integer.valueOf(splitted[1]))) {
                c.getPlayer().dropMessage(5, "禁用完毕.");
                return 0;
            } else {
                c.getPlayer().dropMessage(5, "已经禁用的技能ID");
            }
            return 1;
        }
    }
}
