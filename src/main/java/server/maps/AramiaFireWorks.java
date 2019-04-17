package server.maps;

import client.MapleCharacter;
import handling.world.WorldBroadcastService;
import server.MapleItemInformationProvider;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.awt.*;

public class AramiaFireWorks {

    public final static int KEG_ID = 4001128, SUN_ID = 4001246, DEC_ID = 4001473;
    public final static int MAX_KEGS = 2400, MAX_SUN = 3000, MAX_DEC = 3600;
    private static final int[] arrayMob = {9500168, 9500169, 9500170, 9500171, 9500173, 9500174, 9500175, 9500176, 9500170, 9500171, 9500172, 9500173, 9500174, 9500175, 9400569};
    private static final int[] arrayX = {2100, 2605, 1800, 2600, 3120, 2700, 2320, 2062, 2800, 3100, 2300, 2840, 2700, 2320, 1950};
    private static final int[] arrayY = {574, 364, 574, 316, 574, 574, 403, 364, 574, 574, 403, 574, 574, 403, 574};
    private static final int[] array_X = {720, 180, 630, 270, 360, 540, 450, 142, 142, 218, 772, 810, 848, 232, 308, 142};
    private static final int[] array_Y = {1234, 1234, 1174, 1234, 1174, 1174, 1174, 1260, 1234, 1234, 1234, 1234, 1234, 1114, 1114, 1140};
    private static final int flake_Y = 149;
    private short kegs = MAX_KEGS / 6;
    private short sunshines = MAX_SUN / 6; //start at 1/6 then go from that
    private short decorations = MAX_DEC / 6;

    public void giveKegs(MapleCharacter c, int kegs) {
        this.kegs += kegs;
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            broadcastEvent(c);
        }
    }

    private void broadcastServer(MapleCharacter c, int itemid) {
        WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(6, itemid, "<频道 " + c.getClient().getChannel() + "> " + c.getMap().getMapName() + " : The amount of {" + MapleItemInformationProvider.getInstance().getName(itemid) + "} has reached the limit!"));
    }

    public short getKegsPercentage() {
        return (short) ((kegs / MAX_KEGS) * 10000);
    }

    private void broadcastEvent(final MapleCharacter c) {
        broadcastServer(c, KEG_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(() -> startEvent(c.getClient().getChannelServer().getMapFactory().getMap(100000200)), 10000);
    }

    private void startEvent(final MapleMap map) {
        map.startMapEffect("Who's going crazy with the fireworks?", 5121010);

        EventTimer.getInstance().schedule(() -> spawnMonster(map), 5000);
    }

    private void spawnMonster(MapleMap map) {
        Point pos;
        for (int i = 0; i < arrayMob.length; i++) {
            pos = new Point(arrayX[i], arrayY[i]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(arrayMob[i]), pos);
        }
    }

    public void giveSuns(MapleCharacter c, int kegs) {
        this.sunshines += kegs;
        //have to broadcast a Reactor?
        MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(970010000);
        MapleReactor reactor = map.getReactorByName("mapleTree");
        for (int gogo = kegs + (MAX_SUN / 6); gogo > 0; gogo -= (MAX_SUN / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.sunshines >= (MAX_SUN / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.sunshines >= (MAX_SUN / 6)) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.sunshines >= MAX_SUN) {
            this.sunshines = 0;
            broadcastSun(c);
        }
    }

    public short getSunsPercentage() {
        return (short) ((sunshines / MAX_SUN) * 10000);
    }

    private void broadcastSun(final MapleCharacter c) {
        broadcastServer(c, SUN_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(() -> startSun(c.getClient().getChannelServer().getMapFactory().getMap(970010000)), 10000);
    }

    private void startSun(final MapleMap map) {
        map.startMapEffect("The tree is bursting with sunshine!", 5121010); //5120069
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(() -> spawnItem(map), 5000 + (i * 10000));
        }
    }

    private void spawnItem(MapleMap map) {
        Point pos;
        for (int i = 0; i < Randomizer.nextInt(5) + 10; i++) {
            pos = new Point(array_X[i], array_Y[i]);
            int itemId = 4001246; //温暖的阳光 - 为了让豆芽快速生长，把温暖的阳光放到豆芽上吧
            switch (Randomizer.nextInt(15)) {
                case 0:
                case 1:
                    itemId = 3010141; //蛋糕椅子 - 蛋糕师精心制作的蛋糕. 坐在上面每10秒钟恢复HP,MP各30
                    break;
                case 2:
                    itemId = 3010146; //周年庆水晶枫叶椅子 - 坐在上面时，每10秒恢复HP的冒险岛周年庆水晶枫叶椅子
                    break;
                case 3:
                case 4:
                    itemId = 3010025; //枫叶纪念凳 - 为纪念冒险岛5周年生日而特制的凳子。坐在枫叶树下会自然而然地回忆起最初的感动。每10秒钟可恢复HP 35,MP 10
                    break;
            }
            map.spawnAutoDrop(itemId, pos);
        }
    }

    public void giveDecs(MapleCharacter c, int kegs) {
        this.decorations += kegs;
        //have to broadcast a Reactor?
        MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(555000000);
        MapleReactor reactor = map.getReactorByName("XmasTree");
        for (int gogo = kegs + (MAX_DEC / 6); gogo > 0; gogo -= (MAX_DEC / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.decorations >= (MAX_DEC / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.decorations >= MAX_DEC / 6) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.decorations >= MAX_DEC) {
            this.decorations = 0;
            broadcastDec(c);
        }
    }

    public short getDecsPercentage() {
        return (short) ((decorations / MAX_DEC) * 10000);
    }

    private void broadcastDec(final MapleCharacter c) {
        broadcastServer(c, DEC_ID);
        EventTimer.getInstance().schedule(() -> startDec(c.getClient().getChannelServer().getMapFactory().getMap(555000000)), 10000); //no msg
    }

    private void startDec(final MapleMap map) {
        map.startMapEffect("The tree is bursting with snow!", 5120000);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(() -> spawnDec(map), 5000 + (i * 10000));
        }
    }

    private void spawnDec(MapleMap map) {
        Point pos;
        for (int i = 0; i < Randomizer.nextInt(10) + 40; i++) {
            pos = new Point(Randomizer.nextInt(800) - 400, flake_Y);
            map.spawnAutoDrop(Randomizer.nextInt(15) == 1 ? 4310012 : 4310011, pos);
        }
    }
}
