package server.commands;

import client.*;
import client.anticheat.CheatingOffense;
import client.inventory.*;
import client.skills.Skill;
import client.skills.SkillFactory;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.BattleConstants;
import constants.BattleConstants.PokedexEntry;
import constants.BattleConstants.PokemonNature;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.World;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.*;
import server.Timer;
import server.Timer.*;
import server.life.*;
import server.maps.*;
import server.quest.MapleQuest;
import server.squad.MapleSquad;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.StringUtil;
import tools.packet.MobPacket;
import tools.packet.NPCPacket;

import java.awt.*;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author Emilyx3
 */
public class SuperGMCommand {

    private static final Logger log = LogManager.getLogger(SuperGMCommand.class);

    /**
     * @return
     */
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }


    public static class GiveSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) {
                c.getPlayer().dropMessage(6, "用法: !giveskill <玩家名字> <技能ID> <技能等级> <技能最大等级> [最后一项可以随便填写]");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            Skill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
            byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
            byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);
            if (level > skill.getMaxLevel()) {
                level = (byte) skill.getMaxLevel();
            }
            if (masterlevel > skill.getMaxLevel()) {
                masterlevel = (byte) skill.getMaxLevel();
            }
            victim.changeSingleSkillLevel(skill, level, masterlevel);
            c.dropMessage("技能添加成功。");
            return 1;
        }
    }


    public static class UnlockInv extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            java.util.Map<Item, MapleInventoryType> eqs = new HashMap<>();
            boolean add = false;
            if (splitted.length < 2 || splitted[1].equals("all")) {
                for (MapleInventoryType type : MapleInventoryType.values()) {
                    for (Item item : c.getPlayer().getInventory(type)) {
                        if (ItemFlag.封印.check(item.getFlag())) {
                            item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                            add = true;
                            //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                        }
                        if (ItemFlag.不可交易.check(item.getFlag())) {
                            item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                            add = true;
                            //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                        }
                        if (add) {
                            eqs.put(item, type);
                        }
                        add = false;
                    }
                }
            } else if (splitted[1].equals("eqp")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIPPED).newList()) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("eq")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.EQUIP)) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.EQUIP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("u")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.USE)) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.USE);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("s")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.SETUP)) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.SETUP);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("e")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.ETC);
                    }
                    add = false;
                }
            } else if (splitted[1].equals("c")) {
                for (Item item : c.getPlayer().getInventory(MapleInventoryType.CASH)) {
                    if (ItemFlag.封印.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.封印.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (ItemFlag.不可交易.check(item.getFlag())) {
                        item.setFlag((byte) (item.getFlag() - ItemFlag.不可交易.getValue()));
                        add = true;
                        //c.announce(MaplePacketCreator.updateSpecialItemUse(item, type.getType()));
                    }
                    if (add) {
                        eqs.put(item, MapleInventoryType.CASH);
                    }
                    add = false;
                }
            } else {
                c.getPlayer().dropMessage(6, "[all/eqp/eq/u/s/e/c]");
            }

            for (Entry<Item, MapleInventoryType> eq : eqs.entrySet()) {
                c.getPlayer().forceUpdateItem(eq.getKey().copy());
            }
            return 1;
        }
    }


    public static class Drop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int itemId = Integer.parseInt(splitted[1]);
            short quantity = (short) CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (ItemConstants.isPet(itemId)) {
                c.getPlayer().dropMessage(5, "宠物道具请通过商城购买.");
            } else if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 不存在.");
            } else {
                Item toDrop;
                if (ItemConstants.getInventoryType(itemId) == MapleInventoryType.EQUIP) {
                    toDrop = ii.randomizeStats((Equip) ii.getEquipById(itemId));
                } else {
                    toDrop = new client.inventory.Item(itemId, (byte) 0, quantity, (byte) 0);
                }
                if (!c.getPlayer().isAdmin()) {
                    toDrop.setGMLog(c.getPlayer().getName() + " 使用 !drop");
                    toDrop.setOwner(c.getPlayer().getName());
                }
                c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                c.getPlayer().dropMessage(6, "使用命令成功，丢出道具: " + toDrop.getItemId() + " - " + ii.getName(toDrop.getItemId()) + " 数量: " + toDrop.getQuantity());
                log.info("[命令] 管理员 " + c.getPlayer().getName() + " 丢道具: " + toDrop.getItemId() + " 数量: " + toDrop.getQuantity() + " 名称: " + ii.getName(toDrop.getItemId()));
            }
            return 1;
        }
    }


    public static class Marry extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "用法: !Marry  <name> <itemid>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[2]);
            if (!ItemConstants.isEffectRing(itemId)) {
                c.getPlayer().dropMessage(6, "Invalid itemID.");
            } else {
                MapleCharacter fff = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (fff == null) {
                    c.getPlayer().dropMessage(6, "玩家必须在线.");
                } else {
                    int[] ringID = {MapleInventoryIdentifier.getInstance(), MapleInventoryIdentifier.getInstance()};
                    try {
                        MapleCharacter[] chrz = {fff, c.getPlayer()};
                        for (int i = 0; i < chrz.length; i++) {
                            Equip eq = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId, ringID[i]);
                            if (eq == null) {
                                c.getPlayer().dropMessage(6, "Invalid itemID.");
                                return 0;
                            }
                            MapleInventoryManipulator.addbyItem(chrz[i].getClient(), eq.copy());
                            chrz[i].dropMessage(6, "结婚成功 " + chrz[i == 0 ? 1 : 0].getName());
                        }
                        MapleRing.addToDB(itemId, c.getPlayer(), fff.getName(), fff.getId(), ringID);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
            return 1;
        }
    }


    public static class GivePoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "需要输入玩家的名字和数量.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "错误: 要操作的玩家必须是在同一线或者玩家不存在.");
            } else {
                chrs.setPoints(chrs.getPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getPoints() + " points, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }


    public static class GiveVPoint extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                c.getPlayer().dropMessage(6, "需要输入玩家的名字和数量.");
                return 0;
            }
            MapleCharacter chrs = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (chrs == null) {
                c.getPlayer().dropMessage(6, "错误: 要操作的玩家必须是在同一线或者玩家不存在.");
            } else {
                chrs.setVPoints(chrs.getVPoints() + Integer.parseInt(splitted[2]));
                c.getPlayer().dropMessage(6, splitted[1] + " has " + chrs.getVPoints() + " vpoints, after giving " + splitted[2] + ".");
            }
            return 1;
        }
    }


    public static class SpeakMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getPlayer().getMap().getCharactersThreadsafe()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }


    public static class SpeakChn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (MapleCharacter victim : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                if (victim.getId() != c.getPlayer().getId()) {
                    victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                }
            }
            return 1;
        }
    }


    public static class SpeakWorld extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter victim : cserv.getPlayerStorage().getAllCharacters()) {
                    if (victim.getId() != c.getPlayer().getId()) {
                        victim.getMap().broadcastMessage(MaplePacketCreator.getChatText(victim.getId(), StringUtil.joinStringFrom(splitted, 1), victim.isGM(), 0));
                    }
                }
            }
            return 1;
        }
    }


    public static class 监视玩家 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (target != null) {
                if (target.getClient().isMonitored()) {
                    target.getClient().setMonitored(false);
                    c.getPlayer().dropMessage(5, "停止对 " + target.getName() + " 的监视.");
                } else {
                    target.getClient().setMonitored(true);
                    c.getPlayer().dropMessage(5, "开始监视 " + target.getName() + " 的信息.");
                }
            } else {
                c.getPlayer().dropMessage(5, "当前频道没有找到该玩家.");
                return 0;
            }
            return 1;
        }
    }


    public static class ResetOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forfeit(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]));
            return 1;
        }
    }


    public static class FStartOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceStart(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]), splitted.length > 4 ? splitted[4] : null);
            return 1;
        }
    }


    public static class FCompleteOther extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[2])).forceComplete(c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }


    public static class Threads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            String filter = "";
            if (splitted.length > 1) {
                filter = splitted[1];
            }
            for (int i = 0; i < threads.length; i++) {
                String tstring = threads[i].toString();
                if (tstring.toLowerCase().contains(filter.toLowerCase())) {
                    c.getPlayer().dropMessage(6, i + ": " + tstring);
                }
            }
            return 1;
        }
    }


    public static class ShowTrace extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                throw new IllegalArgumentException();
            }
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            Thread t = threads[Integer.parseInt(splitted[1])];
            c.getPlayer().dropMessage(6, t.toString() + ":");
            for (StackTraceElement elem : t.getStackTrace()) {
                c.getPlayer().dropMessage(6, elem.toString());
            }
            return 1;
        }
    }


    public static class ToggleOffense extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                CheatingOffense co = CheatingOffense.valueOf(splitted[1]);
                co.setEnabled(!co.isEnabled());
            } catch (IllegalArgumentException iae) {
                c.getPlayer().dropMessage(6, "Offense " + splitted[1] + " not found");
            }
            return 1;
        }
    }


    public static class TMegaphone extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            World.toggleMegaphoneMuteState();
            c.getPlayer().dropMessage(6, "Megaphone state : " + (c.getChannelServer().getMegaphoneMuteState() ? "Enabled" : "Disabled"));
            return 1;
        }
    }


    public static class SReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleReactor reactor = new MapleReactor(MapleReactorFactory.getReactor(Integer.parseInt(splitted[1])), Integer.parseInt(splitted[1]));
            reactor.setDelay(-1);
            c.getPlayer().getMap().spawnReactorOnGroundBelow(reactor, new Point(c.getPlayer().getTruePosition().x, c.getPlayer().getTruePosition().y - 20));
            return 1;
        }
    }


    public static class ClearSquads extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            Collection<MapleSquad> squadz = new ArrayList<>(c.getChannelServer().getAllSquads().values());
            for (MapleSquad squads : squadz) {
                squads.clear();
            }
            c.getPlayer().dropMessage(6, "清理远征队伍信息完成.");
            return 1;
        }
    }


    public static class HitMonsterByOID extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            int targetId = Integer.parseInt(splitted[1]);
            int damage = Integer.parseInt(splitted[2]);
            MapleMonster monster = map.getMonsterByOid(targetId);
            if (monster != null) {
                map.broadcastMessage(MobPacket.damageMonster(targetId, damage));
                monster.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }


    public static class HitAll extends CommandExecute {

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
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                mob.damage(c.getPlayer(), damage, false);
            }
            return 1;
        }
    }


    public static class HitMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            int damage = Integer.parseInt(splitted[1]);
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[2])) {
                    map.broadcastMessage(MobPacket.damageMonster(mob.getObjectId(), damage));
                    mob.damage(c.getPlayer(), damage, false);
                }
            }
            return 1;
        }
    }


    public static class KillMonster extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            double range = Double.POSITIVE_INFINITY;
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.getId() == Integer.parseInt(splitted[1])) {
                    mob.damage(c.getPlayer(), mob.getHp(), false);
                }
            }
            return 1;
        }
    }


    public static class KillAllDrops extends CommandExecute {

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
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                map.killMonster(mob, c.getPlayer(), true, false, (byte) 1);
            }
            return 1;
        }
    }


    public static class KillAllExp extends CommandExecute {

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
                c.getPlayer().dropMessage(6, "输入的地图不存在.");
                return 0;
            }
            MapleMonster mob;
            for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Collections.singletonList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                mob.damage(c.getPlayer(), mob.getHp(), false);
            }
            return 1;
        }
    }


    public static class NPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId, c.getPlayer().getMapId());
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(c.getPlayer().getPosition().y);
                npc.setRx0(c.getPlayer().getPosition().x + 50);
                npc.setRx1(c.getPlayer().getPosition().x - 50);
                npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false).getId());
                npc.setCustom(true);
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc));
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 Npc-Id");
                return 0;
            }
            return 1;
        }
    }


    public static class MakeNpc extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !MakeNpc <NpcID>");
                return 0;
            }
            int npcId = Integer.parseInt(splitted[1]);
            MapleNPC npc = MapleLifeFactory.getNPC(npcId, c.getPlayer().getMapId());
            if (npc != null && !npc.getName().equals("MISSINGNO")) {
                int xpos = c.getPlayer().getPosition().x;
                int ypos = c.getPlayer().getPosition().y;
                int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false).getId();
                npc.setPosition(c.getPlayer().getPosition());
                npc.setCy(ypos);
                npc.setRx0(xpos + 50);
                npc.setRx1(xpos - 50);
                npc.setFh(fh);
                npc.setCustom(true);
                try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, npcId);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "n");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, c.getPlayer().getMapId());
                    ps.executeUpdate();
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "保存Npc信息到数据库中出现错误.");
                }
                c.getPlayer().getMap().addMapObject(npc);
                c.getPlayer().getMap().broadcastMessage(NPCPacket.spawnNPC(npc));
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 Npc-Id.");
                return 0;
            }
            return 1;
        }
    }


    public static class MakeMob extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !MakeMob <怪物ID> <刷新时间 默认按秒计算>");
                return 0;
            }
            int mobid = Integer.parseInt(splitted[1]);
            int mobTime = Integer.parseInt(splitted[2]);
            if (splitted[2] == null) {
                mobTime = 1;
            }
            MapleMonster mob = MapleLifeFactory.getMonster(mobid);
            if (mob != null) {
                int xpos = c.getPlayer().getPosition().x;
                int ypos = c.getPlayer().getPosition().y;
                int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition(), false).getId();
                mob.setPosition(c.getPlayer().getPosition());
                mob.setCy(ypos);
                mob.setRx0(xpos + 50);
                mob.setRx1(xpos - 50);
                mob.setFh(fh);
                try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO spawns ( idd, f, fh, cy, rx0, rx1, type, x, y, mid, mobtime ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    ps.setInt(1, mobid);
                    ps.setInt(2, 0);
                    ps.setInt(3, fh);
                    ps.setInt(4, ypos);
                    ps.setInt(5, xpos + 50);
                    ps.setInt(6, xpos - 50);
                    ps.setString(7, "m");
                    ps.setInt(8, xpos);
                    ps.setInt(9, ypos);
                    ps.setInt(10, c.getPlayer().getMapId());
                    ps.setInt(11, mobTime);
                    ps.executeUpdate();
                } catch (SQLException e) {
                    c.getPlayer().dropMessage(6, "保存Mob信息到数据库中出现错误.");
                }
                c.getPlayer().getMap().addMonsterSpawn(mob, mobTime, (byte) -1, null);
            } else {
                c.getPlayer().dropMessage(6, "你应该输入一个正确的 Mob-Id.");
                return 0;
            }
            return 1;
        }
    }


    public static class MakePNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleCharacter chhr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " is not online");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "Done");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return 1;
        }
    }


    public static class MakeOfflineP extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Making playerNPC...");
                MapleClient cs = new MapleClient(null, null, null); //new MockIOSession()
                MapleCharacter chhr = MapleCharacter.loadCharFromDB(MapleCharacterUtil.getIdByName(splitted[1]), cs, false);
                if (chhr == null) {
                    c.getPlayer().dropMessage(6, splitted[1] + " does not exist");
                    return 0;
                }
                PlayerNPC npc = new PlayerNPC(chhr, Integer.parseInt(splitted[2]), c.getPlayer().getMap(), c.getPlayer());
                npc.addToServer();
                c.getPlayer().dropMessage(6, "Done");
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return 1;
        }
    }


    public static class DestroyPNPC extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            try {
                c.getPlayer().dropMessage(6, "Destroying playerNPC...");
                MapleNPC npc = c.getPlayer().getMap().getNPCByOid(Integer.parseInt(splitted[1]));
                if (npc instanceof PlayerNPC) {
                    ((PlayerNPC) npc).destroy(null, true);
                    c.getPlayer().dropMessage(6, "Done");
                } else {
                    c.getPlayer().dropMessage(6, "!destroypnpc [objectid]");
                }
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "NPC failed... : " + e.getMessage());
            }
            return 1;
        }
    }


    public static class ServerMessage extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            String outputMessage = StringUtil.joinStringFrom(splitted, 1);
            ServerConfig.LOGIN_SERVERMESSAGE = outputMessage;
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.setServerMessage(outputMessage);
            }
            return 1;
        }
    }


    public static class Spawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int mid = Integer.parseInt(splitted[1]);
            int num = Math.min(CommandProcessorUtil.getOptionalIntArg(splitted, 2, 1), 50000);
            Integer level = CommandProcessorUtil.getNamedIntArg(splitted, 1, "lvl");
            Long hp = CommandProcessorUtil.getNamedLongArg(splitted, 1, "hp");
            Integer exp = CommandProcessorUtil.getNamedIntArg(splitted, 1, "exp");
            Double php = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "php");
            Double pexp = CommandProcessorUtil.getNamedDoubleArg(splitted, 1, "pexp");

            MapleMonster onemob;
            try {
                onemob = MapleLifeFactory.getMonster(mid);
            } catch (RuntimeException e) {
                c.getPlayer().dropMessage(5, "错误: " + e.getMessage());
                return 0;
            }
            if (onemob == null) {
                c.getPlayer().dropMessage(5, "输入的怪物不存在.");
                return 0;
            }
            long newhp = 0;
            int newexp = 0;
            if (hp != null) {
                newhp = hp;
            } else if (php != null) {
                newhp = (long) (onemob.getMobMaxHp() * (php / 100));
            } else {
                newhp = onemob.getMobMaxHp();
            }
            if (exp != null) {
                newexp = exp;
            } else if (pexp != null) {
                newexp = (int) (onemob.getMobExp() * (pexp / 100));
            } else {
                newexp = onemob.getMobExp();
            }
            if (newhp < 1) {
                newhp = 1;
            }

            OverrideMonsterStats overrideStats = new OverrideMonsterStats(newhp, onemob.getMobMaxMp(), newexp, false);

            for (int i = 0; i < num; i++) {
                MapleMonster mob = MapleLifeFactory.getMonster(mid);
                mob.setHp(newhp);
                if (level != null) {
                    mob.changeLevel(level, false);
                } else {
                    mob.setOverrideStats(overrideStats);
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(mob, c.getPlayer().getPosition());
            }
            return 1;
        }
    }


    public static class PS extends CommandExecute {


        protected static StringBuilder builder = new StringBuilder();

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (builder.length() > 1) {
                c.announce(MaplePacketCreator.getPacketFromHexString(builder.toString()));
                builder = new StringBuilder();
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }


    public static class APS extends PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                builder.append(StringUtil.joinStringFrom(splitted, 1));
                c.getPlayer().dropMessage(6, "String is now: " + builder.toString());
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }


    public static class CPS extends PS {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            builder = new StringBuilder();
            return 1;
        }
    }


    public static class P extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length > 1) {
                c.announce(MaplePacketCreator.getPacketFromHexString(StringUtil.joinStringFrom(splitted, 1)));
            } else {
                c.getPlayer().dropMessage(6, "请输入要测试的数据包信息!");
            }
            return 1;
        }
    }


    public static class Packet extends P {
    }


    public static class ReloadMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            int mapId = Integer.parseInt(splitted[1]);
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId) && cserv.getMapFactory().getMap(mapId).getCharactersSize() > 0) {
                    c.getPlayer().dropMessage(5, "There exists characters on channel " + cserv.getChannel());
                    return 0;
                }
            }
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                if (cserv.getMapFactory().isMapLoaded(mapId)) {
                    cserv.getMapFactory().removeMap(mapId);
                }
            }
            return 1;
        }
    }


    public static class Respawn extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().respawn(true);
            return 1;
        }
    }


    public abstract static class TestTimer extends CommandExecute {


        protected Timer toTest = null;

        @Override
        public int execute(final MapleClient c, String[] splitted) {
            final int sec = Integer.parseInt(splitted[1]);
            c.getPlayer().dropMessage(5, "Message will pop up in " + sec + " seconds.");
            c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: " + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
            final long oldMillis = System.currentTimeMillis();
            toTest.schedule(() -> {
                c.getPlayer().dropMessage(5, "Message has popped up in " + ((System.currentTimeMillis() - oldMillis) / 1000) + " seconds, expected was " + sec + " seconds");
                c.getPlayer().dropMessage(5, "Active: " + toTest.getSES().getActiveCount() + " Core: " + toTest.getSES().getCorePoolSize() + " Largest: " + toTest.getSES().getLargestPoolSize() + " Max: " + toTest.getSES().getMaximumPoolSize() + " Current: " + toTest.getSES().getPoolSize() + " Status: " + toTest.getSES().isShutdown() + toTest.getSES().isTerminated() + toTest.getSES().isTerminating());
            }, sec * 1000);
            return 1;
        }
    }


    public static class TestEventTimer extends TestTimer {


        public TestEventTimer() {
            toTest = EventTimer.getInstance();
        }
    }


    public static class TestCloneTimer extends TestTimer {


        public TestCloneTimer() {
            toTest = CloneTimer.getInstance();
        }
    }


    public static class TestEtcTimer extends TestTimer {


        public TestEtcTimer() {
            toTest = EtcTimer.getInstance();
        }
    }


    public static class TestMapTimer extends TestTimer {


        public TestMapTimer() {
            toTest = MapTimer.getInstance();
        }
    }


    public static class TestWorldTimer extends TestTimer {


        public TestWorldTimer() {
            toTest = WorldTimer.getInstance();
        }
    }


    public static class TestBuffTimer extends TestTimer {


        public TestBuffTimer() {
            toTest = BuffTimer.getInstance();
        }
    }


    public static class Crash extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            if (victim != null && c.getPlayer().getGMLevel() >= victim.getGMLevel()) {
                victim.getClient().announce(HexTool.getByteArrayFromHexString("1A 00")); //give_buff with no data :D
                return 1;
            } else {
                c.getPlayer().dropMessage(6, "The victim does not exist.");
                return 0;
            }
        }
    }


    public static class Rev extends CommandExecute {

        private static int revision = -1;

        /**
         * @return
         */
        public static int getRevision() {
            if (revision != -1) {
                return revision;
            } else {
                InputStream svninfo = AdminCommand.class.getResourceAsStream("/all-wcprops");
                if (svninfo == null) {
                    return revision;
                }
                Scanner sc = new Scanner(svninfo);
                while (sc.hasNext()) {
                    String[] s = sc.next().split("/");
                    if (s.length > 1 && s[1].equals("svn")) {
                        revision = Integer.parseInt(s[5]);
                        break;
                    }
                }
                sc.close();
            }
            return revision;
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (getRevision() != -1) {
                player.dropMessage(5, "This is revision " + revision + ".");
            } else {
                player.dropMessage(5, "Can't find revision T_T");
            }
            return 1;
        }
    }


    public static class FillBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int e : MapleItemInformationProvider.getInstance().getMonsterBook().keySet()) {
                c.getPlayer().getMonsterBook().getCards().put(e, 2);
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "完成.");
            return 1;
        }
    }


    public static class ListBook extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            List<Entry<Integer, Integer>> mbList = new ArrayList<>(MapleItemInformationProvider.getInstance().getMonsterBook().entrySet());
            mbList.sort(new BookComparator());
            int page = Integer.parseInt(splitted[1]);
            for (int e = (page * 8); e < Math.min(mbList.size(), (page + 1) * 8); e++) {
                c.getPlayer().dropMessage(6, e + ": " + mbList.get(e).getKey() + " - " + mbList.get(e).getValue());
            }
            return 0;
        }


        public static class BookComparator implements Comparator<Entry<Integer, Integer>>, Serializable {

            /**
             *
             */
            private static final long serialVersionUID = 4610445413085711114L;

            @Override
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return 1;
                } else if (o1.getValue() == o2.getValue()) {
                    return 0;
                } else {
                    return -1;
                }
            }
        }
    }


    public static class FillPokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (PokedexEntry e : BattleConstants.getAllPokedex()) {
                c.getPlayer().getMonsterBook().getCards().put(e.id, 2);
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }


    public static class SeePokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (PokedexEntry e : BattleConstants.getAllPokedex()) {
                c.getPlayer().getMonsterBook().getCards().put(e.id, 1);
            }
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }


    public static class ClearPokedex extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMonsterBook().getCards().clear();
            c.getPlayer().getMonsterBook().changed();
            c.getPlayer().dropMessage(5, "操作完成.");
            return 1;
        }
    }


    public static class GetPokemon extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (c.getPlayer().countBattlers() >= 6 || splitted.length < 2) {
                c.getPlayer().dropMessage(5, "You have 6 pokemons already, !getpokemon <mobID>");
                return 0;
            }
            MapleMonsterStats theMob = MapleLifeFactory.getMonsterStats(Integer.parseInt(splitted[1]));
            if (theMob == null) {
                c.getPlayer().dropMessage(5, "输入的怪物ID不存在.");
                return 0;
            }
            Battler theB = new Battler(theMob.getLevel(), 0, c.getPlayer().getId(), theMob.getId(), theMob.getName(), PokemonNature.randomNature(), 0, (byte) -1, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) 100, (byte) Randomizer.nextInt(2));
            if (theB.getFamily() == null) {
                c.getPlayer().dropMessage(5, "mob cannot be used in pokemon.");
                return 0;
            }
            c.getPlayer().getBattlers()[c.getPlayer().countBattlers()] = theB;
            c.getPlayer().getMonsterBook().monsterCaught(c, theMob.getId(), theMob.getName());
            c.getPlayer().changedBattler();
            c.getPlayer().dropMessage(6, "操作完成.");
            return 1;
        }
    }


    public static class ResetMap extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetFully();
            return 1;
        }
    }


    public static class ResetQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forfeit(c.getPlayer());
            return 1;
        }
    }


    public static class StartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).start(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }


    public static class CompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).complete(c.getPlayer(), Integer.parseInt(splitted[2]), Integer.parseInt(splitted[3]));
            return 1;
        }
    }


    public static class FStartQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceStart(c.getPlayer(), Integer.parseInt(splitted[2]), splitted.length >= 4 ? splitted[3] : null);
            return 1;
        }
    }


    public static class FCompleteQuest extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleQuest.getInstance(Integer.parseInt(splitted[1])).forceComplete(c.getPlayer(), Integer.parseInt(splitted[2]));
            return 1;
        }
    }


    public static class HReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).hitReactor(c);
            return 1;
        }
    }


    public static class FHReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().getReactorByOid(Integer.parseInt(splitted[1])).forceHitReactor(Byte.parseByte(splitted[2]));
            return 1;
        }
    }


    public static class DReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMap map = c.getPlayer().getMap();
            List<MapleMapObject> reactors = map.getMapObjectsInRange(c.getPlayer().getPosition(), Double.POSITIVE_INFINITY, Collections.singletonList(MapleMapObjectType.REACTOR));
            if (splitted[1].equals("all")) {
                for (MapleMapObject reactorL : reactors) {
                    MapleReactor reactor2l = (MapleReactor) reactorL;
                    c.getPlayer().getMap().destroyReactor(reactor2l.getObjectId());
                }
            } else {
                c.getPlayer().getMap().destroyReactor(Integer.parseInt(splitted[1]));
            }
            return 1;
        }
    }


    public static class SetReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().setReactorState(Byte.parseByte(splitted[1]));
            return 1;
        }
    }


    public static class ResetReactor extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().getMap().resetReactors();
            return 1;
        }
    }


    public static class SendAllNote extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length >= 1) {
                String text = StringUtil.joinStringFrom(splitted, 1);
                for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                    c.getPlayer().sendNote(mch.getName(), text);
                    if (mch != null) {
                        mch.showNote();
                    }
                }
            } else {
                c.getPlayer().dropMessage(6, "用法: !sendallnote <text>");
                return 0;
            }
            return 1;
        }
    }


    public static class BuffSkill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !BuffSkill <技能ID> <技能等级>");
                return 0;
            }
            SkillFactory.getSkill(Integer.parseInt(splitted[1])).getEffect(Integer.parseInt(splitted[2])).applyTo(c.getPlayer());
            return 1;
        }
    }


    public static class BuffItem extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !BuffItem <道具ID>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            }
            ii.getItemEffect(Integer.parseInt(splitted[1])).applyTo(c.getPlayer());
            return 1;
        }
    }


    public static class BuffItemEX extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 1) {
                c.getPlayer().dropMessage(6, "用法: !BuffItemEX <道具ID>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            }
            ii.getItemEffectEX(itemId).applyTo(c.getPlayer());
            return 1;
        }
    }


    public static class ItemSize extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "当前加载的物品数量: " + MapleItemInformationProvider.getInstance().getAllItemSize());
            return 1;
        }
    }


    public static class 漂浮公告 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, "用法: !漂浮公告 <道具ID> <公告信息>");
                return 0;
            }
            int itemId = Integer.parseInt(splitted[1]);
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            if (!ii.itemExists(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 这个道具不存在.");
                return 0;
            } else if (!ii.isFloatCashItem(itemId)) {
                c.getPlayer().dropMessage(5, itemId + " 不具有漂浮公告的效果.");
                return 0;
            }
            WorldBroadcastService.getInstance().startMapEffect(StringUtil.joinStringFrom(splitted, 2), itemId);
            return 1;
        }
    }


    public static class 刷新排行榜 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            RankingTop.getInstance().initAll();
            c.getPlayer().dropMessage(5, "排行榜刷新完成");
            return 1;
        }
    }

    public static class checkGames extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(6, splitted[0] + " <使用方法：进程名/MD5>");
                return 0;
            }
            int i = 0;
            for (ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    for (MapleProcess mp : chr.getProcess()) {
                        if (mp.getMD5().equalsIgnoreCase(splitted[1]) || mp.getName().toLowerCase().contains(splitted[1].toLowerCase())) {
                            c.getPlayer().dropMessage(5, "角色：" + chr.getName() + " 进程【\"" + mp.getPath() + "\" MD5：\"" + mp.getMD5() + "\"】");
                            i++;
                        }
                    }
                }
            }
            c.getPlayer().dropMessage(5, "进程检查完成，本次共检测到" + i + "个。");
            return 1;
        }
    }

    public static class 重载签到奖励 extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleSignin.getInstance().reload(true);
            c.dropMessage("签到奖励重载完成！");
            return 1;
        }
    }
}
