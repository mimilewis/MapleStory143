package server.maps.events;

import client.MapleCharacter;
import client.MapleQuestStatus;
import client.MapleTraitType;
import handling.channel.ChannelServer;
import handling.world.WorldPartyService;
import handling.world.party.MaplePartyCharacter;
import handling.world.party.PartySearch;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Randomizer;

import java.util.concurrent.ScheduledFuture;

public class Event_PyramidSubway {

    private final int type;
    private int kill = 0;
    private int cool = 0;
    private int miss = 0;
    private int skill = 0;
    private int energybar = 100;
    private int bar = 0;
    private boolean broaded = false;
    private ScheduledFuture<?> energyBarDecrease, timerSchedule;
    //type: -1 = subway, 0-3 = difficulty of nett's pyramid.

    public Event_PyramidSubway(final MapleCharacter c) {
        final int mapid = c.getMapId();
        if (mapid / 10000 == 91032) {
            type = -1;
        } else {
            type = mapid % 10000 / 1000;
        }
        if (c.getParty() == null || c.getParty().getLeader().getId() == c.getId()) {
            if (c.getParty() != null && c.getParty().getLeader().getId() == c.getId()) {
                PartySearch ps = WorldPartyService.getInstance().getSearch(c.getParty());
                if (ps != null) {
                    WorldPartyService.getInstance().removeSearch(ps, "The Party Listing has been removed because the Party Quest started.");
                }
            }
            commenceTimerNextMap(c, 1);
            energyBarDecrease = MapTimer.getInstance().register(() -> {
                energybar -= (c.getParty() != null && c.getParty().getMembers().size() > 1 ? 5 : 2);
                if (broaded) {
                    //broadcastUpdate(c);
                    c.getMap().respawn(true);
                } else {
                    broaded = true;
                }
                if (energybar <= 0) { //why
                    fail(c);
                }
            }, 1000);
        }
    }

    public static boolean warpStartSubway(final MapleCharacter c) {
        final int mapid = 910320100;

        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = 0; i < 5; i++) {
            final MapleMap map = ch.getMapFactory().getMap(mapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                changeMap(c, map, 25, 30);
                return true;
            }
        }
        return false;
    }

    public static boolean warpBonusSubway(final MapleCharacter c) {
        final int mapid = 910320010;

        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = 0; i < 20; i++) {
            final MapleMap map = ch.getMapFactory().getMap(mapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));//solo
                return true;
            }
        }
        return false;
    }

    public static boolean warpNextMap_Subway(final MapleCharacter c) {
        final int currentmap = c.getMapId();
        final int thisStage = (currentmap - 910320100) / 100;

        MapleMap map = c.getMap();
        clearMap(map, true);
        final ChannelServer ch = c.getClient().getChannelServer();
        if (thisStage >= 2) {
            map = ch.getMapFactory().getMap(910330001);
            changeMap(c, map, 1, 200, 1);
            return true;
        }
        final int nextmapid = 910320100 + ((thisStage + 1) * 100);
        for (int i = 0; i < 5; i++) {
            map = ch.getMapFactory().getMap(nextmapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                changeMap(c, map, 1, 200, 1); //any level because they could level
                return true;
            }
        }
        return false;
    }

    public static boolean warpStartPyramid(final MapleCharacter c, final int difficulty) {
        final int mapid = 926010100 + (difficulty * 1000);
        int minLevel = 40, maxLevel = 60;
        switch (difficulty) {
            case 1:
                minLevel = 45;
                break;
            case 2:
                minLevel = 50;
                break;
            case 3:
                minLevel = 61;
                maxLevel = 200;
                break;
        }
        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = 0; i < 5; i++) {
            final MapleMap map = ch.getMapFactory().getMap(mapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                changeMap(c, map, minLevel, maxLevel);
                return true;
            }
        }
        return false;
    }

    public static boolean warpBonusPyramid(final MapleCharacter c, final int difficulty) {
        final int mapid = 926010010 + (difficulty * 20);

        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = 0; i < 20; i++) {
            final MapleMap map = ch.getMapFactory().getMap(mapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));//solo
                return true;
            }
        }
        return false;
    }

    public static boolean warpNextMap_Pyramid(final MapleCharacter c, final int difficulty) {
        final int currentmap = c.getMapId();
        final int thisStage = (currentmap - (926010100 + (difficulty * 1000))) / 100;

        MapleMap map = c.getMap();
        clearMap(map, true);
        final ChannelServer ch = c.getClient().getChannelServer();
        if (thisStage >= 4) {
            map = ch.getMapFactory().getMap(926020001 + difficulty);
            changeMap(c, map, 1, 200, 1);
            return true;
        }
        final int nextmapid = 926010100 + ((thisStage + 1) * 100) + (difficulty * 1000);
        for (int i = 0; i < 5; i++) {
            map = ch.getMapFactory().getMap(nextmapid + i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                changeMap(c, map, 1, 200, 1); //any level because they could level
                return true;
            }
        }
        return false;
    }

    private static void changeMap(MapleCharacter c, MapleMap map, int minLevel, int maxLevel) {
        changeMap(c, map, minLevel, maxLevel, 0);
    }

    private static void changeMap(MapleCharacter c, MapleMap map, int minLevel, int maxLevel, int clear) {
        MapleMap oldMap = c.getMap();
        if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
            for (MaplePartyCharacter mpc : c.getParty().getMembers()) {
                MapleCharacter chr = oldMap.getCharacterById(mpc.getId());
                if (chr != null && chr.getId() != c.getId() && chr.getLevel() >= minLevel && chr.getLevel() <= maxLevel) {
                    if (clear == 1) {
                        chr.send(MaplePacketCreator.showEffect("killing/clear"));
                    } else if (clear == 2) {
                        chr.send(MaplePacketCreator.showEffect("killing/fail"));
                    }
                    chr.changeMap(map, map.getPortal(0));
                }
            }
        }
        if (clear == 1) {
            c.getClient().announce(MaplePacketCreator.showEffect("killing/clear"));
        } else if (clear == 2) {
            c.getClient().announce(MaplePacketCreator.showEffect("killing/fail"));
        }
        c.changeMap(map, map.getPortal(0));
    }

    private static void clearMap(MapleMap map, boolean check) {
        if (check && map.getCharactersSize() > 0) {
            return;
        }
        map.resetFully(false);
    }

    public final void fullUpdate(final MapleCharacter c, final int stage) {
        broadcastEnergy(c, "massacre_party", c.getParty() == null ? 0 : c.getParty().getMembers().size()); //huh
        broadcastEnergy(c, "massacre_miss", miss);
        broadcastEnergy(c, "massacre_cool", cool);
        broadcastEnergy(c, "massacre_skill", skill);
        broadcastEnergy(c, "massacre_laststage", stage - 1);
        broadcastEnergy(c, "massacre_hit", kill);
        broadcastUpdate(c);
    }

    public final void commenceTimerNextMap(final MapleCharacter c, final int stage) {
        if (timerSchedule != null) {
            timerSchedule.cancel(false);
            timerSchedule = null;
        }
        final MapleMap ourMap = c.getMap();
        final int time = (type == -1 ? 180 : (stage == 1 ? 240 : 300)) - 1;
        energybar = 100;
        bar = 0;
        if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
            for (MaplePartyCharacter mpc : c.getParty().getMembers()) {
                final MapleCharacter chr = ourMap.getCharacterById(mpc.getId());
                if (chr != null) {
                    chr.send(MaplePacketCreator.getClock(time));
                    chr.send(MaplePacketCreator.showEffect("killing/first/number/" + stage));
                    chr.send(MaplePacketCreator.showEffect("killing/first/stage"));
                    chr.send(MaplePacketCreator.showEffect("killing/first/start"));
                    fullUpdate(chr, stage);
                }
            }
        } else {
            c.getClient().announce(MaplePacketCreator.getClock(time));
            c.getClient().announce(MaplePacketCreator.showEffect("killing/first/number/" + stage));
            c.getClient().announce(MaplePacketCreator.showEffect("killing/first/stage"));
            c.getClient().announce(MaplePacketCreator.showEffect("killing/first/start"));
            fullUpdate(c, stage);
        }
        if (type != -1 && (stage == 4 || stage == 5)) { //yetis. temporary
            for (int i = 0; i < (stage == 4 ? 1 : 2); i++) {
                //9700023 = 90 secs removeAfter -> real yeti
                //real yeti -> 9700022 when killed -> 15 secs removeAfter -> real yeti
                ourMap.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9700023), c.getPosition());
            }
        }
        timerSchedule = MapTimer.getInstance().schedule(() -> {
            boolean ret = false;
            if (type == -1) {
                ret = warpNextMap_Subway(c);
            } else {
                ret = warpNextMap_Pyramid(c, type);
            }
            if (!ret) {
                fail(c);
            }
        }, time * 1000L);
    }

    public final void onKill(final MapleCharacter c) {
        kill++;
        bar++;
        if (Randomizer.nextInt(100) < 5) { //monster properties coolDamage and coolDamageProb determine this, will code later
            cool++;
            bar++;
            broadcastEnergy(c, "massacre_cool", cool);
        }
        energybar++;
        if (energybar > 100) {
            energybar = 100; //rofl
        }
        if (type != -1) {
            for (int i = 5; i >= 1; i--) {
                if ((kill + cool) % (i * 100) == 0 && Randomizer.nextInt(100) < 20) {
                    broadcastEffect(c, "killing/yeti" + (i - 1));
                    break;
                }
            }
            //i dont want to give buffs as they could smuggle it
            if ((kill + cool) % 500 == 0) {
                skill++;
                broadcastEnergy(c, "massacre_skill", skill);
            }
        }

        broadcastUpdate(c);
        broadcastEnergy(c, "massacre_hit", kill);
    }

    public final void onMiss(final MapleCharacter c) {
        miss++;
        energybar -= 5;
        broadcastUpdate(c);
        broadcastEnergy(c, "massacre_miss", miss);
    }

    public final boolean onSkillUse(final MapleCharacter c) {
        if (skill > 0 && type != -1) {
            skill--;
            broadcastEnergy(c, "massacre_skill", skill);
            return true;
        }
        return false;
    }

    public final void onChangeMap(final MapleCharacter c, final int newmapid) {
        if ((newmapid == 910330001 && type == -1) || (newmapid == 926020001 + type && type != -1)) {
            succeed(c);
        } else {
            if (type == -1 && (newmapid < 910320100 || newmapid > 910320304)) {
                dispose(c);
            } else if (type != -1 && (newmapid < 926010100 || newmapid > 926013504)) {
                dispose(c);
            } else if (c.getParty() == null || c.getParty().getLeader().getId() == c.getId()) {
                energybar = 100;
                commenceTimerNextMap(c, newmapid % 1000 / 100);
            }
        }
    }

    public final void succeed(final MapleCharacter c) {
        final MapleQuestStatus record = c.getQuestNAdd(MapleQuest.getInstance(type == -1 ? 7662 : 7760));
        String data = record.getCustomData();
        if (data == null) {
            record.setCustomData("0");
            data = record.getCustomData();
        }
        final int mons = Integer.parseInt(data);
        final int tk = kill + cool;
        record.setCustomData(String.valueOf(mons + tk));
        byte rank = 4;
        if (type == -1) {
            if (tk >= 2000) {
                rank = 0;
            } else if (tk >= 1500 && tk <= 1999) {
                rank = 1;
            } else if (tk >= 1000 && tk <= 1499) {
                rank = 2;
            } else if (tk >= 500 && tk <= 999) {
                rank = 3;
            }
        } else {
            if (tk >= 3000) {
                rank = 0;
            } else if (tk >= 2000 && tk <= 2999) {
                rank = 1;
            } else if (tk >= 1500 && tk <= 1999) {
                rank = 2;
            } else if (tk >= 500 && tk <= 1499) {
                rank = 3;
            }
        }

        int pt = 0;
        switch (type) {
            case 0:
                switch (rank) {
                    case 0:
                        pt = 30500;
                        break;
                    case 1:
                        pt = 25000;
                        break;
                    case 2:
                        pt = 21750;
                        break;
                    case 3:
                        pt = 12000;
                        break;
                }
                break;
            case 1:
                switch (rank) {
                    case 0:
                        pt = 36000;
                        break;
                    case 1:
                        pt = 30000;
                        break;
                    case 2:
                        pt = 25750;
                        break;
                    case 3:
                        pt = 13000;
                        break;
                }
                break;
            case 2:
                switch (rank) {
                    case 0:
                        pt = 36500;
                        break;
                    case 1:
                        pt = 35000;
                        break;
                    case 2:
                        pt = 27250;
                        break;
                    case 3:
                        pt = 14000;
                        break;
                }
                break;
            case 3:
                switch (rank) {
                    case 0:
                        pt = 47000;
                        break;
                    case 1:
                        pt = 40000;
                        break;
                    case 2:
                        pt = 29500;
                        break;
                    case 3:
                        pt = 18000;
                        break;
                }
                break;
            default:
                switch (rank) {
                    case 0:
                        pt = 12000;
                        break;
                    case 1:
                        pt = 9000;
                        break;
                    case 2:
                        pt = 5750;
                        break;
                    case 3:
                        pt = 3000;
                        break;
                }
                break;
        }
        int exp = 0;
        if (rank < 4) {
            exp = ((kill + (cool * 5)) + pt) * c.getClient().getChannelServer().getExpRate();
            c.gainExp(exp, true, false, false);
        }
        c.getTrait(MapleTraitType.will).addExp((type + 2) * 8, c);
        c.getClient().announce(MaplePacketCreator.showEffect("killing/clear"));
        c.getClient().announce(MaplePacketCreator.sendPyramidResult(rank, exp));
        dispose(c);
    }

    public final void fail(final MapleCharacter c) {
        final MapleMap map;
        if (type == -1) {
            map = c.getClient().getChannelServer().getMapFactory().getMap(910320001);
        } else {
            map = c.getClient().getChannelServer().getMapFactory().getMap(926010001 + type);
        }
        changeMap(c, map, 1, 200, 2);
        dispose(c);
    }

    public final void dispose(final MapleCharacter c) {
        final boolean lead = energyBarDecrease != null && timerSchedule != null;
        if (energyBarDecrease != null) {
            energyBarDecrease.cancel(false);
            energyBarDecrease = null;
        }
        if (timerSchedule != null) {
            timerSchedule.cancel(false);
            timerSchedule = null;
        }
        if (c.getParty() != null && lead && c.getParty().getMembers().size() > 1) {
            fail(c);
            return;
        }
        c.setPyramidSubway(null);
    }

    public final void broadcastUpdate(final MapleCharacter c) {
        c.getClient().announce(MaplePacketCreator.sendPyramidUpdate(bar));
    }

    public final void broadcastEffect(final MapleCharacter c, final String effect) {
        c.getClient().announce(MaplePacketCreator.showEffect(effect));
    }

    public final void broadcastEnergy(final MapleCharacter c, final String type, final int amount) {
        c.getClient().announce(MaplePacketCreator.sendPyramidEnergy(type, String.valueOf(amount)));
    }
}
