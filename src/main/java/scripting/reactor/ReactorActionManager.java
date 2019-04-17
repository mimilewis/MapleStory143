package scripting.reactor;

import client.MapleClient;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import constants.ItemConstants;
import handling.channel.ChannelServer;
import scripting.AbstractPlayerInteraction;
import server.MapleItemInformationProvider;
import server.carnival.MapleCarnivalFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.maps.MapleReactor;
import server.maps.ReactorDropEntry;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ReactorActionManager extends AbstractPlayerInteraction {

    private static final int[] 碎片 = {4001513, 4001515, 4001521};
    private final MapleReactor reactor;

    /**
     * @param c
     * @param reactor
     */
    public ReactorActionManager(MapleClient c, MapleReactor reactor) {
        super(c, reactor.getReactorId(), String.valueOf(c.getPlayer().getMapId()));
        this.reactor = reactor;
    }


    public void dropItems() {
        dropItems(false, 0, 0, 0, 0, 0, false);
    }

    /**
     * @param minDrops
     * @param maxDrops
     */
    public void dropItems(int minDrops, int maxDrops) {
        dropItems(false, 0, 0, 0, minDrops, maxDrops, true);
    }

    /**
     * @param meso
     * @param mesoChance
     * @param minMeso
     * @param maxMeso
     */
    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso) {
        dropItems(meso, mesoChance, minMeso, maxMeso, 0, 0, false);
    }

    /**
     * @param meso
     * @param mesoChance
     * @param minMeso
     * @param maxMeso
     * @param minDrops
     * @param maxDrops
     * @param profession
     */
    public void dropItems(boolean meso, int mesoChance, int minMeso, int maxMeso, int minDrops, int maxDrops, boolean profession) {
        //System.err.println("反应堆爆物 - minDrops: " + minDrops + " maxDrops: " + maxDrops);
        List<ReactorDropEntry> chances = ReactorScriptManager.getInstance().getDrops(reactor.getReactorId());
        List<ReactorDropEntry> toDrop = new LinkedList<>();
        if (meso && Math.random() < (1 / (double) mesoChance)) {
            toDrop.add(new ReactorDropEntry(0, mesoChance, -1));
        }
        // narrow list down by chances
        // for (DropEntry d : chances){
        for (ReactorDropEntry d : chances) {
            if (Math.random() < (1 / (double) d.chance) && (d.questid <= 0 || getPlayer().getQuestStatus(d.questid) == 1)) {
                toDrop.add(d);
            }
        }
        //System.err.println("数据库随机爆率: " + toDrop.size());
        // if a minimum number of drops is required, add meso
        while (toDrop.size() < minDrops) {
            if (profession && !toDrop.isEmpty()) {
                for (int i = 0; i < toDrop.size(); i++) {
                    ReactorDropEntry fix = toDrop.get(i);
                    if (fix != null && toDrop.size() < minDrops) {
                        toDrop.add(fix);
                    }
                }
            } else {
                toDrop.add(new ReactorDropEntry(0, mesoChance, -1));
            }
        }
        if (profession && toDrop.size() > maxDrops) {
            toDrop = toDrop.subList(0, maxDrops);
        }
        Collections.shuffle(toDrop); //对道具随机排序
        Point dropPos = reactor.getPosition();
        dropPos.x -= (12 * toDrop.size());
        //System.err.println("最终掉落数量: " + toDrop.size());
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        for (ReactorDropEntry de : toDrop) {
            if (de.itemId == 0) {
                int range = maxMeso - minMeso;
                int mesoDrop = Randomizer.nextInt(range) + minMeso * ChannelServer.getInstance(getClient().getChannel()).getMesoRate();
                if (mesoDrop > 0) {
                    reactor.getMap().spawnMesoDrop(mesoDrop, dropPos, reactor, getPlayer(), false, (byte) 0);
                }
            } else {
                Item drop;
                if (ItemConstants.getInventoryType(de.itemId) != MapleInventoryType.EQUIP) {
                    drop = new Item(de.itemId, (byte) 0, (short) 1, (byte) 0);
                } else {
                    drop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                }
                drop.setGMLog("从箱子爆出 " + reactor.getReactorId() + " 在地图 " + getPlayer().getMapId());
                reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, dropPos, false, false);
            }
            dropPos.x += 25;
        }
    }

    /**
     * @param itemId
     */
    public void dropSingleItem(int itemId) {
        Item drop;
        if (ItemConstants.getInventoryType(itemId) != MapleInventoryType.EQUIP) {
            drop = new Item(itemId, (byte) 0, (short) 1, (byte) 0);
        } else {
            drop = MapleItemInformationProvider.getInstance().randomizeStats((Equip) MapleItemInformationProvider.getInstance().getEquipById(itemId));
        }
        drop.setGMLog("采矿(药)掉落 " + reactor.getReactorId() + " 在地图 " + getPlayer().getMapId());
        reactor.getMap().spawnItemDrop(reactor, getPlayer(), drop, reactor.getPosition(), false, false);
    }

    /**
     * @param npcId
     */
    @Override
    public void spawnNpc(int npcId) {
        spawnNpc(npcId, getPosition());
    }

    // returns slightly above the reactor's position for monster spawns

    /**
     * @return
     */
    public Point getPosition() {
        Point pos = reactor.getPosition();
        pos.y -= 10;
        return pos;
    }

    /**
     * @return
     */
    public MapleReactor getReactor() {
        return reactor;
    }


    public void spawnZakum() {
        spawnZakum(66000000L);
    }

    public void spawnZakum(long maxhp) {
        reactor.getMap().spawnZakum(getPosition().x, getPosition().y, maxhp);
    }


    public void spawnChaosZakum() {
        spawnChaosZakum(0);
    }

    public void spawnChaosZakum(long maxhp) {
        reactor.getMap().spawnChaosZakum(getPosition().x, getPosition().y, maxhp);
    }

    public void spawnSimpleZakum() {
        spawnSimpleZakum(0);
    }

    public void spawnSimpleZakum(long maxhp) {
        reactor.getMap().spawnSimpleZakum(getPosition().x, getPosition().y, maxhp);
    }

    /**
     * @param id
     */
    public void spawnFakeMonster(int id) {
        spawnFakeMonster(id, 1, getPosition());
    }

    // summon one monster, remote location

    /**
     * @param id
     * @param x
     * @param y
     */
    public void spawnFakeMonster(int id, int x, int y) {
        spawnFakeMonster(id, 1, new Point(x, y));
    }

    // multiple monsters, reactor location

    /**
     * @param id
     * @param qty
     */
    public void spawnFakeMonster(int id, int qty) {
        spawnFakeMonster(id, qty, getPosition());
    }

    // multiple monsters, remote location

    /**
     * @param id
     * @param qty
     * @param x
     * @param y
     */
    public void spawnFakeMonster(int id, int qty, int x, int y) {
        spawnFakeMonster(id, qty, new Point(x, y));
    }

    // handler for all spawnFakeMonster
    private void spawnFakeMonster(int id, int qty, Point pos) {
        for (int i = 0; i < qty; i++) {
            reactor.getMap().spawnFakeMonsterOnGroundBelow(MapleLifeFactory.getMonster(id), pos);
        }
    }


    public void killAll() {
        reactor.getMap().killAllMonsters(true);
    }

    /**
     * @param monsId
     */
    public void killMonster(int monsId) {
        reactor.getMap().killMonster(monsId);
    }

    // summon one monster on reactor location

    /**
     * @param id
     */
    @Override
    public void spawnMonster(int id) {
        spawnMonster(id, 1, getPosition());
    }

    // summon monsters on reactor location

    /**
     * @param id
     * @param qty
     */
    @Override
    public void spawnMonster(int id, int qty) {
        spawnMonster(id, qty, getPosition());
    }

    /**
     * @param succ
     */
    public void cancelHarvest(boolean succ) {
        getPlayer().setFatigue((byte) (getPlayer().getFatigue() + 1));
        getPlayer().getMap().broadcastMessage(getPlayer(), MaplePacketCreator.showHarvesting(getPlayer().getId(), 0), false);
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.harvestResult(getPlayer().getId(), succ));
    }


    public void doHarvest() {
        if (getPlayer().getFatigue() >= 200 || getPlayer().getStat().harvestingTool <= 0 || getReactor().getTruePosition().distanceSq(getPlayer().getTruePosition()) > 10000) {
            return;
        }
        int pID = getReactor().getReactorId() < 200000 ? 92000000 : 92010000;
        String pName = (getReactor().getReactorId() < 200000 ? "采药" : "采矿");
        int he = getPlayer().getProfessionLevel(pID);
        if (he <= 0) {
            return;
        }
        Item item = getInventory(1).getItem((short) getPlayer().getStat().harvestingTool);
        if (item == null || ((item.getItemId() / 10000)) != (getReactor().getReactorId() < 200000 ? 150 : 151)) {
            return;
        }
        int hm = getReactor().getReactorId() % 100;
        int successChance = 90 + ((he - hm) * 10);
        if (getReactor().getReactorId() % 100 == 10) {
            hm = 1;
            successChance = 100;
        } else if (getReactor().getReactorId() % 100 == 11) {
            hm = 10;
            successChance += 20;
        }
        getPlayer().getStat().checkEquipDurabilitys(getPlayer(), -1, true);
        int masteryIncrease = (hm - he) * 2 + 20;
        boolean succ = randInt(100) < successChance;
        if (!succ) {
            masteryIncrease /= 10;
            //dropSingleItem(getReactor().getReactorId() < 200000 ? 4022023 : 4011010); //掉落药草根和母矿碎片
        } else {
            int minDrops = 1;
            int maxDrops = getReactor().getReactorId() % 100 == 11 ? 6 : 2;
            if (item.getItemId() == 1512001) {
                minDrops += 2;
                maxDrops += 2;
            } else if (item.getItemId() == 1512004 || item.getItemId() == 1512005) {
                minDrops += 3;
                maxDrops += 3;
            } else if (item.getItemId() == 1512006) {
                minDrops += 5;
                maxDrops += 5;
            }
            dropItems(minDrops, maxDrops);
            if (getReactor().getReactorId() < 200000) {
                addTrait("sense", 5);
                if (Randomizer.nextInt(10) <= 1) {
                    dropSingleItem(2440000); //道具橘子宝宝
                }
                if (Randomizer.nextInt(100) == 0) {
                    dropSingleItem(4032933); //一朵玫瑰 任务道具 开启口袋栏
                }
                if (Randomizer.nextInt(10) <= 1) {
                    dropSingleItem(碎片[Randomizer.nextInt(碎片.length)]); //斑纹券碎片
                }
            } else {
                addTrait("insight", 5);
                if (Randomizer.nextInt(10) <= 1) {
                    dropSingleItem(2440001); //道具钻石宝宝
                }
                if (Randomizer.nextInt(10) <= 1) {
                    dropSingleItem(碎片[Randomizer.nextInt(碎片.length)]); //斑纹券碎片
                }
            }
        }
        cancelHarvest(succ);
        playerMessage(-5, pName + "的熟练度提高了。(+" + masteryIncrease + ")");
        if (getPlayer().addProfessionExp(pID, masteryIncrease)) {
            playerMessage(-5, pName + "的等级提升了。");
        }
    }

    public void delayedDestroyReactor(long time) {
        reactor.delayedDestroyReactor(time);
    }

    public void dispelAllMonsters(int num) {
        MapleCarnivalFactory.MCSkill guardian = MapleCarnivalFactory.getInstance().getGuardian(num);
        if (guardian != null) {
            for (MapleMonster monster : this.getMap().getAllMonstersThreadsafe()) {
                monster.dispelSkill(guardian.getSkill());
            }
        }
    }

    public void setSrcPos() {
        getPlayer().setReactor(null);
        reactor.setPosition(reactor.getSrcPos());
    }
}
