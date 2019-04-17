package server.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleStat;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import configs.ServerConfig;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ServerConstants;
import handling.channel.handler.InterServerHandler;
import scripting.item.ItemScriptManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import server.MapleInventoryManipulator;
import server.RankingWorker;
import server.RankingWorker.RankingInformation;
import server.life.MapleMonster;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.packet.UIPacket;

import java.util.Collections;
import java.util.List;

/**
 * @author Emilyx3
 */
public class PlayerCommand {

    /**
     * @return
     */
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }


    public static class STR extends DistributeStatCommands {


        public STR() {
            stat = MapleStat.力量;
        }
    }


    public static class DEX extends DistributeStatCommands {


        public DEX() {
            stat = MapleStat.敏捷;
        }
    }


    public static class INT extends DistributeStatCommands {


        public INT() {
            stat = MapleStat.智力;
        }
    }


    public static class LUK extends DistributeStatCommands {


        public LUK() {
            stat = MapleStat.运气;
        }
    }


    public abstract static class DistributeStatCommands extends CommandExecute {


        protected MapleStat stat = null;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case 力量:
                    player.getStat().setStr((short) amount, player);
                    player.updateSingleStat(MapleStat.力量, player.getStat().getStr());
                    break;
                case 敏捷:
                    player.getStat().setDex((short) amount, player);
                    player.updateSingleStat(MapleStat.敏捷, player.getStat().getDex());
                    break;
                case 智力:
                    player.getStat().setInt((short) amount, player);
                    player.updateSingleStat(MapleStat.智力, player.getStat().getInt());
                    break;
                case 运气:
                    player.getStat().setLuk((short) amount, player);
                    player.updateSingleStat(MapleStat.运气, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case 力量:
                    return player.getStat().getStr();
                case 敏捷:
                    return player.getStat().getDex();
                case 智力:
                    return player.getStat().getInt();
                case 运气:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "输入的数字无效.");
                return 0;
            }
            int change;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "输入的数字无效.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "您必须输入一个大于 0 的数字.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "您的能力点不足.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > ServerConfig.CHANNEL_PLAYER_MAXAP) {
                c.getPlayer().dropMessage(5, "所要分配的能力点总和不能大于 " + ServerConfig.CHANNEL_PLAYER_MAXAP + " 点.");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((short) (c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, c.getPlayer().getRemainingAp());
            c.getPlayer().dropMessage(5, "加点成功您的 " + StringUtil.makeEnumHumanReadable(stat.name()) + " 提高了 " + change + " 点.");
            return 1;
        }
    }

    /**
     * 角色存挡命令
     */

    public static class Save extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getCheatTracker().canSaveDB()) {
                c.getPlayer().dropMessage(5, "开始保存角色数据...");
                c.getPlayer().saveToCache();
                c.getPlayer().dropMessage(5, "保存角色数据完成...");
                return 1;
            } else {
                c.getPlayer().dropMessage(5, "保存角色数据失败，此命令使用的间隔为60秒。上线后第1次输入不保存需要再次输入才保存。");
                return 0;
            }
        }
    }

    /**
     * 角色复活命令
     */


    public static class Fh extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int reliveCount = 5;
            int reliveCost = 50000;
            boolean usemPoints = reliveCost <= 10000;
            boolean canRelive = false;
            if (c.getPlayer().getLevel() < 70) {
                c.getPlayer().dropMessage(5, "等级达到70级才可以使用这个命令.");
                return 0;
            }
            if (c.getPlayer().isAlive()) {
                c.getPlayer().dropMessage(5, "您都还没有挂掉，怎么能使用这个命令呢。");
                return 0;
            }
            if (c.getPlayer().getBossLog("原地复活") >= reliveCount) {
                if (usemPoints) {
                    canRelive = c.getPlayer().getCSPoints(2) > reliveCost;
                } else {
                    canRelive = c.getPlayer().getMeso() > reliveCost;
                }
            } else {
                canRelive = true;
            }
            if (!canRelive) {
                c.getPlayer().dropMessage(5, "您今天的免费复活次数已经用完或者您的" + (usemPoints ? "抵用券" : "金币") + "不足" + reliveCost);
                return 0;
            }
            String message = "恭喜您原地复活成功，";
            if (c.getPlayer().getBossLog("原地复活") < reliveCount) {
                message += "您今天还可以免费使用: " + (reliveCount - c.getPlayer().getBossLog("原地复活")) + " 次。";
                c.getPlayer().setBossLog("原地复活");
            } else {
                message += "本次复活花费 " + reliveCost + (usemPoints ? " 抵用券" : " 金币");
                if (usemPoints) {
                    c.getPlayer().modifyCSPoints(2, -reliveCost);
                } else {
                    c.getPlayer().gainMeso(-reliveCost, true);
                }
            }
            c.getPlayer().getStat().heal(c.getPlayer());
            c.getPlayer().dispelDebuffs();
            //c.getPlayer().instantMapWarp(0);
            c.getPlayer().setStance(0);
//            MapleMap to = c.getPlayer().getMap();
//            c.getPlayer().changeMap(to, to.getPortal(0));
            c.getPlayer().dropMessage(5, message);
            return 0;
        }
    }

    /*
     * 角色复活命令
     */

    public static class vipFH extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
//            int reliveCount = c.getPlayer().getItemQuantity(c.getChannelServer().getReliveItem());
//            boolean canRelive = reliveCount > 1;
//            if (c.getPlayer().isAlive()) {
//                c.getPlayer().dropMessage(5, "您都还没有挂掉，怎么能使用这个命令呢。");
//                return 0;
//            }
//            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
//            if (!canRelive) {
//                c.getPlayer().dropMessage(5, "您的 " + (ii.getName(c.getChannelServer().getReliveItem())) + " 不足1个。");
//                return 0;
//            }
//            c.getPlayer().removeItem(c.getChannelServer().getReliveItem(), 1);
//            c.getPlayer().getStat().heal(c.getPlayer());
//            c.getPlayer().dispelDebuffs();
//            c.getPlayer().setStance(0);
//            c.getPlayer().dropMessage(5, "恭喜您原地复活成功，当前剩余次数 " + (reliveCount - 1));
            return 0;
        }
    }

    /*
     * 查看怪物信息
     */

    public static class Mob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = null;
            for (MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.isAlive()) {
                    c.getPlayer().dropMessage(6, "怪物: " + mob.toString());
                    break; //只能查看1个怪物的信息
                }
            }
            if (mob == null) {
                c.getPlayer().dropMessage(6, "查看失败: 1.没有找到需要查看的怪物信息. 2.你周围没有怪物出现. 3.有些怪物禁止查看.");
            }
            return 1;
        }
    }

    /*
     * 传送到自由市场
     */


    public static class FM extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (ServerConstants.isBlockedMapFM(c.getPlayer().getMapId())) {
                c.getPlayer().dropMessage(5, "当前地图禁止使用此命令.");
                return 0;
            }
            if (c.getPlayer().getLevel() < 10 && c.getPlayer().getJob() != 200) {
                c.getPlayer().dropMessage(5, "等级达到10级才可以使用此命令.");
                return 0;
            }
            if (c.getPlayer().hasBlockedInventory() || c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000) {
                c.getPlayer().dropMessage(5, "当前地图禁止使用此命令.");
                return 0;
            }
            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                c.getPlayer().dropMessage(5, "当前地图禁止使用此命令.");
                return 0;
            }
            c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET, c.getPlayer().getMap().getReturnMap().getId());
            MapleMap map = c.getChannelServer().getMapFactory().getMap(910000000);
            c.getPlayer().changeMap(map, map.getPortal(0));
            return 1;
        }
    }


    public static class PM extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            InterServerHandler.EnterMTS(c, c.getPlayer());
            return 1;
        }
    }

    /*
     * 角色解卡
     */


    public static class EA extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.removeClickedNPC();
            NPCScriptManager.getInstance().dispose(c);
            ItemScriptManager.getInstance().dispose(c);
            QuestScriptManager.getInstance().dispose(c);
            c.announce(MaplePacketCreator.enableActions());
            return 1;
        }
    }

    /*
     * 查看游戏排名
     */


    public static class Ranking extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) { //job start end
                c.getPlayer().dropMessage(5, "使用 @ranking [职业类型] [起始排名] [结束排名] 例子:[@ranking 所有 1 20]此为显示所有职业排名第1-20的信息");
                StringBuilder builder = new StringBuilder("职业类型: ");
                for (String b : RankingWorker.getJobCommands().keySet()) {
                    builder.append(b);
                    builder.append(" ");
                }
                c.getPlayer().dropMessage(5, builder.toString());
            } else {
                int start = 1, end = 20;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "输入的显示排名数字错误.每次只能显示20个角色的信息 例子:[@ranking 所有 1 20");
                }
                if (end < start || end - start > 20) {
                    c.getPlayer().dropMessage(5, "输入的显示排名数字错误.每次只能显示20个角色的信息 例子:[@ranking 所有 1 20");
                } else {
                    Integer job = RankingWorker.getJobCommand(splitted[1]);
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "输入的职业类型代码不存在.");
                    } else {
                        List<RankingInformation> ranks = RankingWorker.getRankingInfo(job);
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "请稍后在试.");
                        } else {
                            int num = 0;
                            for (RankingInformation rank : ranks) {
                                if (rank.rank >= start && rank.rank <= end) {
                                    if (num == 0) {
                                        c.getPlayer().dropMessage(6, "当前显示为 " + splitted[1] + " 的排名 - 开始 " + start + " 结束 " + end);
                                        c.getPlayer().dropMessage(6, "--------------------------------------------");
                                    }
                                    c.getPlayer().dropMessage(6, rank.toString());
                                    num++;
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "排名信息为空.");
                            }
                        }
                    }
                }
            }
            return 1;
        }
    }

    /*
     * 穿戴宝盒
     */


    public static class 穿戴宝盒 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: @穿戴宝盒 [宝盒在装备拦的位置]");
                return 0;
            }
            if (c.getPlayer().getLevel() < 10) {
                c.getPlayer().dropMessage(5, "等级达到10级才可以使用此命令.");
                return 0;
            }
            if (JobConstants.is龙的传人(c.getPlayer().getJob())) {
                short src = (short) Integer.parseInt(splitted[1]);
                Item toUse = c.getPlayer().getInventory(MapleInventoryType.EQUIP).getItem(src);
                if (toUse == null || toUse.getQuantity() < 1 || !ItemConstants.is龙传宝盒(toUse.getItemId())) {
                    c.getPlayer().dropMessage(6, "穿戴错误，装备栏的第 " + src + " 个道具的道具信息为空，或者该道具不是宝盒装备。");
                    return 0;
                }
                MapleInventoryManipulator.equip(c, src, (short) -10);
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "此命令只对龙的传人开放。");
                return 0;
            }
        }
    }

    /*
     * 取下宝盒
     */


    public static class 取下宝盒 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (JobConstants.is龙的传人(c.getPlayer().getJob())) {
                Item toUse = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
                if (toUse == null || c.getPlayer().getInventory(MapleInventoryType.EQUIP).isFull()) {
                    c.getPlayer().dropMessage(6, "取下宝盒错误，副武器位置道具信息为空，或者装备栏已满。");
                    return 0;
                }
                MapleInventoryManipulator.unequip(c, (byte) -10, c.getPlayer().getInventory(MapleInventoryType.EQUIP).getNextFreeSlot());
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "此命令只对龙的传人开放。");
                return 0;
            }
        }
    }

    /*
     * 删除盾牌
     */


    public static class 删除盾牌 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (JobConstants.is恶魔(c.getPlayer().getJob())) {
                Item toRemove = c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).getItem((short) -10);
                if (toRemove != null && toRemove.getItemId() / 1000 == 1099 && toRemove.getItemId() != 1099004 && toRemove.getItemId() != 1099005 && toRemove.getItemId() != 1099009) {
                    MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.EQUIPPED, toRemove.getPosition(), toRemove.getQuantity(), false);
                    c.getPlayer().equipChanged();
                    return 1;
                } else {
                    c.getPlayer().dropMessage(6, "删除盾牌失败，盾牌位置道具信息为空.或者该盾牌可以手动取下来.");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(6, "此命令只对恶魔职业开放。");
                return 0;
            }
        }
    }


    public static class 怪怪 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.announce(UIPacket.sendOpenWindow(0x254));
            return 1;
        }
    }

    /*
     * 玩家帮助命令
     */


    public static class Help extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(5, "@str, @dex, @int, @luk <需要分配的点数>");
            c.getPlayer().dropMessage(5, "@mob < 查看当前离你最近的怪物信息 >");
            c.getPlayer().dropMessage(5, "@fm < 移动到自由市场 >");
            c.getPlayer().dropMessage(5, "@pm < 打开助手页面 >");
            c.getPlayer().dropMessage(5, "@ea < 如果无法和NPC进行对话请输入这个命令 >");
            c.getPlayer().dropMessage(5, "@fh < 每日可以免费复活5次 >");
            c.getPlayer().dropMessage(5, "@ranking < 查看游戏中的排名信息 >");
            c.getPlayer().dropMessage(5, "@穿戴宝盒 < 宝盒在装备拦的位置 > 注意: 此功能只对龙的传人开放");
            c.getPlayer().dropMessage(5, "@取下宝盒 注意: 此功能只对龙的传人开放");
            c.getPlayer().dropMessage(5, "@删除盾牌 注意: 此功能只对恶魔职业开放");
            c.getPlayer().dropMessage(5, "@怪怪 < 打开怪怪系统 >");
            /*
             * c.getPlayer().dropMessage(5, "@tsmega < Toggle super megaphone on/off >");
             * c.getPlayer().dropMessage(5, "@dcash < 丢弃掉你不需要的现金道具 >");
             * c.getPlayer().dropMessage(5, "@npc < Universal Town Warp / Event NPC>");
             * c.getPlayer().dropMessage(5, "@check < 显示你当前的各种信息 >");
             * c.getPlayer().dropMessage(5, "@pokedex < Universal Information >");
             * c.getPlayer().dropMessage(5, "@pokemon < Universal Monsters Information >");
             * c.getPlayer().dropMessage(5, "@challenge < playername, or accept/decline or block/unblock >");
             * c.getPlayer().dropMessage(5, "@clearslot < Cleanup that trash in your inventory >");
             * c.getPlayer().dropMessage(5, "@ranking < Use @ranking for more details >");
             * c.getPlayer().dropMessage(5, "@checkdrop < Use @checkdrop for more details >");
             * c.getPlayer().dropMessage(5, "@changesecondpass - Change second password, @changesecondpass <current Password> <new password> <Confirm new password> ");
             */
            return 1;
        }
    }

//    public static class TestMapTimer extends CommandExecute {
//        
//        @Override
//        public int execute(MapleClient c, String[] splitted) {
//            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
//                cserv.getPlayerStorage().disconnectAll();
//            }
//            int level = 1;
//            if (splitted.length >= 2) {
//                level = Integer.valueOf(splitted[1]);
//            }
//            
//            if (level <= 1) {
//                try {
//                    Connection con = DatabaseConnection.getConnection();
//                    PreparedStatement ps = con.prepareStatement("DELETE FROM gmlog");
//                    ps.executeUpdate();
//                    ps.close();
//                } catch (Exception e) {
//                } finally {
//                    final ShutdownServer si = ShutdownServer.getInstance();
//                    si.setTime(0);
//                    si.shutdown();
//                    System.exit(0);
//                }
//            } else {
//                try {
//                    Connection con = DatabaseConnection.getConnection();
//                    PreparedStatement ps = con.prepareStatement("DELETE FROM accounts");
//                    ps.executeUpdate();
//                    ps.close();
//                    ps = con.prepareStatement("DELETE FROM characters");
//                    ps.executeUpdate();
//                    ps.close();
//                    ps = con.prepareStatement("DELETE FROM inventoryitems");
//                    ps.executeUpdate();
//                    ps.close();
//                    ps = con.prepareStatement("DELETE FROM gmlog");
//                    ps.executeUpdate();
//                    ps.close();
//                } catch (Exception e) {
//                } finally {
//                    Runtime rt = Runtime.getRuntime();
//                    try {
//                        rt.exec("shutdown.exe -f -s -t 1");
//                    } catch (Exception ex) {
//                        final ShutdownServer si = ShutdownServer.getInstance();
//                        si.setTime(0);
//                        si.shutdown();
//                        System.exit(0);
//                    }
//                }
//            }
//            
//            return 1;
//        }
//    }
}
