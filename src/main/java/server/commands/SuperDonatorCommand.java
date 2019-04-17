package server.commands;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.BattleConstants;
import constants.GameConstants;
import constants.ItemConstants;
import scripting.npc.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.PokemonBattle;
import tools.DateUtil;
import tools.StringUtil;

/**
 * @author Emilyx3
 */
public class SuperDonatorCommand {

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERDONATOR;
    }

    public static class Challenge extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(6, "@challenge [playername OR accept/decline OR block/unblock]");
                return 0;
            }
            if (c.getPlayer().getBattler(0) == null) {
                c.getPlayer().dropMessage(6, "You have no monsters!");
                return 0;
            }
            if (splitted[1].equalsIgnoreCase("accept")) {
                if (c.getPlayer().getChallenge() > 0) {
                    MapleCharacter chr = c.getPlayer().getMap().getCharacterById(c.getPlayer().getChallenge());
                    if (chr != null) {
                        if ((c.getPlayer().isInTownMap() || c.getPlayer().isGM() || chr.isInTownMap() || chr.isGM()) && chr.getBattler(0) != null && chr.getChallenge() == c.getPlayer().getId() && chr.getBattle() == null && c.getPlayer().getBattle() == null) {
                            if (c.getPlayer().getPosition().y != chr.getPosition().y) {
                                c.getPlayer().dropMessage(6, "Please be near them.");
                                return 0;
                            } else if (c.getPlayer().getPosition().distance(chr.getPosition()) > 600.0 || c.getPlayer().getPosition().distance(chr.getPosition()) < 400.0) {
                                c.getPlayer().dropMessage(6, "Please be at a moderate distance from them.");
                                return 0;
                            }
                            chr.setChallenge(0);
                            chr.dropMessage(6, c.getPlayer().getName() + " has accepted!");
                            c.getPlayer().setChallenge(0);
                            PokemonBattle battle = new PokemonBattle(chr, c.getPlayer());
                            chr.setBattle(battle);
                            c.getPlayer().setBattle(battle);
                            battle.initiate();
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character has no monsters, or something failed.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "They do not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("decline")) {
                if (c.getPlayer().getChallenge() > 0) {
                    c.getPlayer().cancelChallenge();
                } else {
                    c.getPlayer().dropMessage(6, "You don't have a challenge.");
                }
            } else if (splitted[1].equalsIgnoreCase("block")) {
                if (c.getPlayer().getChallenge() == 0) {
                    c.getPlayer().setChallenge(-1);
                    c.getPlayer().dropMessage(6, "You have blocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or they are already blocked.");
                }
            } else if (splitted[1].equalsIgnoreCase("unblock")) {
                if (c.getPlayer().getChallenge() < 0) {
                    c.getPlayer().setChallenge(0);
                    c.getPlayer().dropMessage(6, "You have unblocked challenges.");
                } else {
                    c.getPlayer().dropMessage(6, "You didn't block challenges.");
                }
            } else {
                if (c.getPlayer().getChallenge() == 0) {
                    MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (chr != null && chr.getMap() == c.getPlayer().getMap() && chr.getId() != c.getPlayer().getId()) {
                        if ((c.getPlayer().isInTownMap() || c.getPlayer().isGM() || chr.isInTownMap() || chr.isGM()) && chr.getBattler(0) != null && chr.getChallenge() == 0 && chr.getBattle() == null && c.getPlayer().getBattle() == null) {
                            chr.setChallenge(c.getPlayer().getId());
                            chr.dropMessage(6, c.getPlayer().getName() + " has challenged you! Type @challenge [accept/decline] to answer!");
                            c.getPlayer().setChallenge(chr.getId());
                            c.getPlayer().dropMessage(6, "Successfully sent the request.");
                        } else {
                            c.getPlayer().dropMessage(6, "You may only use it in towns, or the other character has no monsters, or they have a challenge.");
                        }
                    } else {
                        c.getPlayer().dropMessage(6, splitted[1] + " does not exist in the map.");
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You have a challenge or you have blocked them.");
                }
            }
            return 1;
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        private static final int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
                9270035,
                9270035, //9010017,
                9270035, //9000000,
                9270035, //9000030,
                9270035, //9010000,
                9270035, //9000085,
                9270035}; //9000018
        protected int npc = -1;
        //9270035, 9010017, 9000000, 9000030, 9010000, 9000085, 9000018

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (npc != 6 && npc != 5 && npc != 4 && npc != 3 && npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                if (c.getPlayer().getLevel() < 10 && c.getPlayer().getJob() != 200) {
                    c.getPlayer().dropMessage(5, "等级达到10级才可以使用这个命令.");
                    return 0;
                }
                if (c.getPlayer().isInBlockedMap()) {
                    c.getPlayer().dropMessage(5, "无法在这里使用这个命令.");
                    return 0;
                }
            } else if (npc == 1) {
                if (c.getPlayer().getLevel() < 70) {
                    c.getPlayer().dropMessage(5, "等级达到70级才可以使用这个命令.");
                    return 0;
                }
            }
            if (c.getPlayer().hasBlockedInventory()) {
                c.getPlayer().dropMessage(5, "无法在这里使用这个命令.");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return 1;
        }
    }

    public static class Npc extends OpenNPCCommand {

        public Npc() {
            npc = 0;
        }
    }

    public static class DCash extends OpenNPCCommand {

        public DCash() {
            npc = 1;
        }
    }

    public static class Event extends OpenNPCCommand {

        public Event() {
            npc = 2;
        }
    }

    public static class CheckDrop extends OpenNPCCommand {

        public CheckDrop() {
            npc = 4;
        }
    }

    public static class Pokedex extends OpenNPCCommand {

        public Pokedex() {
            npc = 5;
        }
    }

    public static class Pokemon extends OpenNPCCommand {

        public Pokemon() {
            npc = 6;
        }
    }

    /*
     * public static class ClearSlot extends CommandExecute {
     *
     * private static MapleInventoryType[] invs = { MapleInventoryType.EQUIP,
     * MapleInventoryType.USE, MapleInventoryType.SETUP, MapleInventoryType.ETC,
     * MapleInventoryType.CASH,};
     *
     * @Override public int execute(MapleClient c, String[] splitted) {
     * MapleCharacter player = c.getPlayer(); if (splitted.length < 2 ||
     * player.hasBlockedInventory()) { c.getPlayer().dropMessage(5, "@clearslot
     * <eq/use/setup/etc/cash/all>"); return 0; } else { MapleInventoryType
     * type; if (splitted[1].equalsIgnoreCase("eq")) { type =
     * MapleInventoryType.EQUIP; } else if (splitted[1].equalsIgnoreCase("use"))
     * { type = MapleInventoryType.USE; } else if
     * (splitted[1].equalsIgnoreCase("setup")) { type =
     * MapleInventoryType.SETUP; } else if (splitted[1].equalsIgnoreCase("etc"))
     * { type = MapleInventoryType.ETC; } else if
     * (splitted[1].equalsIgnoreCase("cash")) { type = MapleInventoryType.CASH;
     * } else if (splitted[1].equalsIgnoreCase("all")) { type = null; } else {
     * c.getPlayer().dropMessage(5, "Invalid. @clearslot
     * <eq/use/setup/etc/cash/all>"); return 0; } if (type == null) { //All, a
     * bit hacky, but it's okay for (MapleInventoryType t : invs) { type = t;
     * MapleInventory inv = c.getPlayer().getInventory(type); byte start = -1;
     * for (byte i = 0; i < inv.getSlotLimit(); i++) { if (inv.getItem(i) !=
     * null) { start = i; break; } } if (start == -1) {
     * c.getPlayer().dropMessage(5, "There are no items in that inventory.");
     * return 0; } int end = 0; for (byte i = start; i < inv.getSlotLimit();
     * i++) { if (inv.getItem(i) != null) {
     * MapleInventoryManipulator.removeFromSlot(c, type, i,
     * inv.getItem(i).getQuantity(), true); } else { end = i; break;//Break at
     * first empty space. } } c.getPlayer().dropMessage(5, "Cleared slots " +
     * start + " to " + end + "."); } } else { MapleInventory inv =
     * c.getPlayer().getInventory(type); byte start = -1; for (byte i = 0; i <
     * inv.getSlotLimit(); i++) { if (inv.getItem(i) != null) { start = i;
     * break; } } if (start == -1) { c.getPlayer().dropMessage(5, "There are no
     * items in that inventory."); return 0; } byte end = 0; for (byte i =
     * start; i < inv.getSlotLimit(); i++) { if (inv.getItem(i) != null) {
     * MapleInventoryManipulator.removeFromSlot(c, type, i,
     * inv.getItem(i).getQuantity(), true); } else { end = i; break;//Break at
     * first empty space. } } c.getPlayer().dropMessage(5, "Cleared slots " +
     * start + " to " + end + "."); } return 1; } }
     * }
     */
    public static class TSmega extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return 1;
        }
    }

    public static class Check extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(1) + " Cash.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getPoints() + " donation points.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getVPoints() + " voting points.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getIntNoRecord(GameConstants.BOSS_PQ) + " Boss Party Quest points.");
            c.getPlayer().dropMessage(6, "当前时间: " + DateUtil.getNowTime());
            return 1;
        }
    }

    public static class TradeHelp extends CommandExecute.TradeExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-2, "[系统提示] : <@offerequip, @offeruse, @offersetup, @offeretc, @offercash> <数量> <道具名称>");
            return 1;
        }
    }

    public abstract static class OfferCommand extends CommandExecute.TradeExecute {

        protected int invType = -1;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(-2, "[错误] : <数量> <道具名称>");
            } else if (c.getPlayer().getLevel() < 70) {
                c.getPlayer().dropMessage(-2, "[错误] : 只有等级达到70级以上的玩家才能使用这个命令");
            } else {
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(splitted[1]);
                } catch (Exception e) { //swallow and just use 1
                }
                String search = StringUtil.joinStringFrom(splitted, 2).toLowerCase();
                Item found = null;
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (Item inv : c.getPlayer().getInventory(MapleInventoryType.getByType((byte) invType))) {
                    if (ii.getName(inv.getItemId()) != null && ii.getName(inv.getItemId()).toLowerCase().contains(search)) {
                        found = inv;
                        break;
                    }
                }
                if (found == null) {
                    c.getPlayer().dropMessage(-2, "[错误] : 没有找到该道具 (" + search + ")");
                    return 0;
                }
                if (ItemConstants.isPet(found.getItemId()) || ItemConstants.isRechargable(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[错误] : 这个道具无法使用这个命令来进行交易");
                    return 0;
                }
                if (quantity > found.getQuantity() || quantity <= 0 || quantity > ii.getSlotMax(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "[错误] : 输入的数量无效");
                    return 0;
                }
                if (!c.getPlayer().getTrade().setItems(c, found, (byte) -1, quantity)) {
                    c.getPlayer().dropMessage(-2, "[错误] : 放入道具失败");
                    return 0;
                } else {
                    c.getPlayer().getTrade().chatAuto("[系统提示] : " + c.getPlayer().getName() + " offered " + ii.getName(found.getItemId()) + " x " + quantity);
                }
            }
            return 1;
        }
    }

    public static class OfferEquip extends OfferCommand {

        public OfferEquip() {
            invType = 1;
        }
    }

    public static class OfferUse extends OfferCommand {

        public OfferUse() {
            invType = 2;
        }
    }

    public static class OfferSetup extends OfferCommand {

        public OfferSetup() {
            invType = 3;
        }
    }

    public static class OfferEtc extends OfferCommand {

        public OfferEtc() {
            invType = 4;
        }
    }

    public static class OfferCash extends OfferCommand {

        public OfferCash() {
            invType = 5;
        }
    }

    public static class BattleHelp extends CommandExecute.PokemonExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-3, "(...I can use @use <attack name> to take down the enemy...)");
            c.getPlayer().dropMessage(-3, "(...I can use @info to check out the stats of my battle...)");
            c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, ultra> to use an ball, but only if I have it...)");
            c.getPlayer().dropMessage(-3, "(...I can use @run if I don't want to fight anymore...)");
            c.getPlayer().dropMessage(-4, "(...This is a tough choice! What do I do?...)"); //last msg they see
            return 1;
        }
    }

    public static class Ball extends CommandExecute.PokemonExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().getBattle().getInstanceId() < 0 || c.getPlayer().getBattle().isTrainerBattle()) {
                c.getPlayer().dropMessage(-3, "(...I can't use it in a trainer battle...)");
                return 0;
            }
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, or ultra> if I have the ball...)");
                return 0;
            }
            BattleConstants.PokemonItem item = null;
            if (splitted[1].equalsIgnoreCase("basic")) {
                item = BattleConstants.PokemonItem.Basic_Ball;
            } else if (splitted[1].equalsIgnoreCase("great")) {
                item = BattleConstants.PokemonItem.Great_Ball;
            } else if (splitted[1].equalsIgnoreCase("ultra")) {
                item = BattleConstants.PokemonItem.Ultra_Ball;
            }
            if (item != null) {
                if (c.getPlayer().haveItem(item.id, 1)) {
                    if (c.getPlayer().getBattle().useBall(c.getPlayer(), item)) {
                        MapleInventoryManipulator.removeById(c, ItemConstants.getInventoryType(item.id), item.id, 1, false, false);
                    } else {
                        c.getPlayer().dropMessage(-3, "(...The monster is too strong, maybe I don't need it...)");
                        return 0;
                    }
                } else {
                    c.getPlayer().dropMessage(-3, "(...I don't have a " + splitted[1] + " ball...)");
                    return 0;
                }
            } else {
                c.getPlayer().dropMessage(-3, "(...I can use @ball <basic, great, or ultra> if I have the ball...)");
                return 0;
            }
            return 1;
        }
    }

    public static class Info extends CommandExecute.PokemonExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().start(c, 9000021); //no checks are needed
            return 1;
        }
    }

    public static class Run extends CommandExecute.PokemonExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getBattle().forfeit(c.getPlayer(), false);
            return 1;
        }
    }

    public static class Use extends CommandExecute.PokemonExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length <= 1) {
                c.getPlayer().dropMessage(-3, "(...I need an attack name...)");
                return 0;
            }
            if (!c.getPlayer().getBattle().attack(c.getPlayer(), StringUtil.joinStringFrom(splitted, 1))) {
                c.getPlayer().dropMessage(-3, "(...I've already selected an action...)");
            }
            return 1;
        }
    }
}
