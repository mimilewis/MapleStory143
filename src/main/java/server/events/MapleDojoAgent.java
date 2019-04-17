/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.events;

import client.MapleCharacter;
import client.MapleTraitType;
import constants.GameConstants;
import handling.channel.ChannelServer;
import handling.world.party.MaplePartyCharacter;
import server.Timer.MapTimer;
import server.life.MapleLifeFactory;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.Randomizer;
import tools.packet.UIPacket;

import java.awt.*;

public class MapleDojoAgent {

    private final static int baseAgentMapId = 970030000; // 9500337 = mano
    private final static Point point1 = new Point(140, 0),
            point2 = new Point(-193, 0),
            point3 = new Point(355, 0);

    public static boolean warpStartAgent(final MapleCharacter c, final boolean party) {
        final int stage = 1;
        final int mapid = baseAgentMapId + (stage * 100);

        final ChannelServer ch = c.getClient().getChannelServer();
        for (int i = mapid; i < mapid + 15; i++) {
            final MapleMap map = ch.getMapFactory().getMap(i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));
                map.respawn(true);
                return true;
            }
        }
        return false;
    }

    public static boolean warpNextMap_Agent(final MapleCharacter c, final boolean fromResting) {
        final int currentmap = c.getMapId();
        final int thisStage = (currentmap - baseAgentMapId) / 100;

        MapleMap map = c.getMap();
        if (map.getSpawnedMonstersOnMap() > 0) {
            return false;
        }
        if (!fromResting) {
            clearMap(map, true);
            c.modifyCSPoints(1, 40, true);
        }
        final ChannelServer ch = c.getClient().getChannelServer();
        if (currentmap >= 970032700 && currentmap <= 970032800) {
            map = ch.getMapFactory().getMap(baseAgentMapId);
            c.changeMap(map, map.getPortal(0));
            return true;
        }
        final int nextmapid = baseAgentMapId + ((thisStage + 1) * 100);
        for (int i = nextmapid; i < nextmapid + 7; i++) {
            map = ch.getMapFactory().getMap(i);
            if (map.getCharactersSize() == 0) {
                clearMap(map, false);
                c.changeMap(map, map.getPortal(0));
                c.updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=15;stage=" + (thisStage - 1) + ";lBonus=0");
                c.updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=15;stage=" + (thisStage) + ";lBonus=0");
                c.updateInfoQuest(7214, "15");
                c.updateInfoQuest(7215, "stage=" + (thisStage + 1) + ";type=1;token=3");
                c.updateInfoQuest(7215, "stage=" + (thisStage + 1) + ";type=1;token=3");
                map.respawn(true);
                return true;
            }
        }
        return false;
    }

    public static boolean warpStartDojo(final MapleCharacter c, final boolean party, final MapleMap currentmap) {
        int stage = 1;
        if (party || stage <= -1 || stage > 38) {
            stage = 1;
        }
        int mapid = currentmap.getId();//925020000 + (stage * 100);
        boolean canenter = false;
        final ChannelServer ch = c.getClient().getChannelServer();
        for (int x = 0; x < 10; x++) { //15 maps each stage
            boolean canenterr = true;
            for (int i = 1; i <= 47; i++) { //only 40 stages, but 47 maps
                MapleMap map = ch.getMapFactory().getMap(925020000 + 100 * i + x);//normal mode
                if (map.getCharactersSize() > 0) {
                    canenterr = false;
                    break;
                } else {
                    clearMap(map, false);
                }
            }
            if (canenterr) {
                canenter = true;
                mapid += x;
                break;
            }
        }
        for (int x = 0; x < 10; x++) { //15 maps each stage // make this better.....
            boolean canenterr = true;
            for (int i = 1; i <= 47; i++) { //only 40 stages, but 47 maps
                MapleMap map = ch.getMapFactory().getMap(925030000 + 100 * i + x);//hard mode
                if (map.getCharactersSize() > 0) {
                    canenterr = false;
                    break;
                } else {
                    clearMap(map, false);
                }
            }
            if (canenterr) {
                canenter = true;
                mapid += x;
                break;
            }
        }
        final MapleMap map = ch.getMapFactory().getMap(mapid);
        final MapleMap mapidd = c.getMap();
        if (canenter) {
            if (party && c.getParty() != null) {
                for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                    MapleCharacter chr = mapidd.getCharacterById(mem.getId());
                    if (chr != null && chr.isAlive()) {
                        changeMap(chr, map);
                    }
                }
            } else {
                changeMap(c, map);
            }
            spawnMonster(map, stage);
        }
        return canenter;
    }

    private static void changeMap(MapleCharacter player, MapleMap map) {
        MapleQuest quest = MapleQuest.getInstance(7214);
        player.getQuestNAdd(quest).setCustomData("0");
        player.updateQuest(player.getQuestNAdd(quest));
        player.updateInfoQuest(7218, "1");
        player.updateOneInfo(7281, "sec", "0");
        player.updateOneInfo(7215, "sTime", String.valueOf(System.currentTimeMillis()));
        player.changeMap(map, map.getPortal(0));
    }

    public static void failed(final MapleCharacter c) {
        final MapleMap currentmap = c.getMap();
        final MapleMap deadMap = c.getClient().getChannelServer().getMapFactory().getMap(925020002);
        if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
            for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                if (chr != null) {
                    chr.changeMap(deadMap, deadMap.getPortal(0));
                }
            }
        }
    }

    // Resting rooms :
    // 925020600 ~ 925020609
    // 925021200 ~ 925021209
    // 925021800 ~ 925021809
    // 925022400 ~ 925022409
    // 925023000 ~ 925023009
    // 925023600 ~ 925023609
    // 925024200 ~ 925024209
    public static boolean warpNextMap(final MapleCharacter c, final boolean fromResting, final MapleMap currentmap) {
        try {
            final int temp = (currentmap.getId() - 925000000) / 100;
            final int thisStage = temp - ((temp / 100) * 100);
            final int points = getDojoPoints(thisStage);
            final ChannelServer ch = c.getClient().getChannelServer();
            final MapleMap deadMap = ch.getMapFactory().getMap(925020002);
            if (!c.isAlive()) { //shouldn't happen
                c.changeMap(deadMap, deadMap.getPortal(0));
                return true;
            }
            final MapleMap map = ch.getMapFactory().getMap(currentmap.getId() + 100);
            if (!fromResting && map != null) {
                clearMap(currentmap, true);
                if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            final int point = (points * 3);
                            c.getTrait(MapleTraitType.will).addExp(points, c);
                            chr.modifyCSPoints(1, point * 4, true);
                            final int dojo = chr.getIntRecord(GameConstants.DOJO) + point;
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                            //chr.getClient().getSession().write(WvsContext.Mulung_Pts(point, dojo));
                        }
                    }
                } else {
                    final int point = (points * 4);
                    c.getTrait(MapleTraitType.will).addExp(points, c);
                    c.modifyCSPoints(1, point * 4, true);
                    final int dojo = c.getIntRecord(GameConstants.DOJO) + point;
                    c.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                    //c.getClient().getSession().write(WvsContext.Mulung_Pts(point, dojo));
                }

            }
            if (currentmap.getId() >= 925023800 && currentmap.getId() <= 925023814) {
                final MapleMap lastMap = ch.getMapFactory().getMap(925020003);

                if (c.getParty() != null && c.getParty().getMembers().size() > 1) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            if (!chr.isAlive()) {
                                chr.addHP(50);
                            }
                            chr.changeMap(lastMap, lastMap.getPortal(1));
                            final int point = (points * 3);
                            c.getTrait(MapleTraitType.will).addExp(points, c);
                            final int dojo = chr.getIntRecord(GameConstants.DOJO) + point;
                            chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                            //chr.getClient().getSession().write(WvsContext.Mulung_Pts(point, dojo));
                            chr.modifyCSPoints(1, 5000, true);
                        }
                    }
                } else {
                    c.changeMap(lastMap, lastMap.getPortal(1));
                    final int point = (points * 4);
                    c.getTrait(MapleTraitType.will).addExp(points, c);
                    final int dojo = c.getIntRecord(GameConstants.DOJO) + point;
                    c.getQuestNAdd(MapleQuest.getInstance(GameConstants.DOJO)).setCustomData(String.valueOf(dojo));
                    //c.getClient().getSession().write(WvsContext.Mulung_Pts(point, dojo));
                    c.modifyCSPoints(1, currentmap.getCharactersSize() > 1 ? 5000 : 7500, true);
                }
                return true;
            }
            if (currentmap.getId() == 925020001) {//925030100 925020001
//                final MapleMap mappzz = ch.getMapFactory().getMap(925030100);//rooftop
//                c.changeMap(mappzz, mappzz.getPortal(0));
            } else if (currentmap.getId() == 925021100) {//End Normal
                final MapleMap mappzz = ch.getMapFactory().getMap(925020003);//rooftop
                c.changeMap(mappzz, mappzz.getPortal(1));
            } else if (currentmap.getId() == 925032300) {//End Hard
                final MapleMap mappzz = ch.getMapFactory().getMap(925020003);//rooftop
                c.changeMap(mappzz, mappzz.getPortal(1));
                c.getClient().getSession().write(UIPacket.sendOpenWindow(72));// Dojo ui TODO add points?
            } else {

                //final int nextmapid = 925020000 + ((thisStage + 1) * 100);
                if (map != null && map.getCharactersSize() == 0) {
                    clearMap(map, false);
                    updateQuestInfoAndChangeMap(c, currentmap, map, thisStage);
//                    for (int i = 0; i < 2; i++) {
//                        c.updateInfoQuest(7215, "stage=1;type=1;token=3");
//                    }
//                    c.updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=15;stage=" + (thisStage - 1) + ";lBonus=0");
//                    c.updateInfoQuest(7281, "item=0;chk=0;cNum=0;sec=15;stage=" + (thisStage) + ";lBonus=0");
//                    c.updateInfoQuest(7214, "15");
//                    c.updateInfoQuest(7215, "stage=" + (thisStage + 1) + ";type=1;token=3");
//                    c.updateInfoQuest(7215, "stage=" + (thisStage + 1) + ";type=1;token=3");
                    spawnMonster(map, thisStage + 1);
                    return true;
                } else if (map != null) { //wtf, find a new map
                    int basemap = currentmap.getId() / 100 * 100 + 100;
                    for (int x = 0; x < 10; x++) {
                        MapleMap mapz = ch.getMapFactory().getMap(basemap + x);
                        if (mapz.getCharactersSize() == 0) {
                            clearMap(mapz, false);
                            updateQuestInfoAndChangeMap(c, currentmap, mapz, thisStage);
//                            for (int i = 0; i < 2; i++) {
//                                c.updateInfoQuest(7215, "stage=1;type=1;token=3");
//                            }
                            spawnMonster(mapz, thisStage + 1);
                            return true;
                        }
                    }
                }
                final MapleMap mappz = ch.getMapFactory().getMap(925020003);
                int diff = (currentmap.getId() % 1000000 / 10000 - 1) / 2;
                String mode = diff == 0 ? "普通模式" : (diff == 1 ? "困难模式" : "排行榜模式");
                int exp = 306951 * (1 + diff);
                if (c.getParty() != null) {
                    for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                        MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                        if (chr != null) {
                            chr.dropMessage(5, "你在" + mode + "下通关了武陵道场。获得了" + exp + "点经验值。");
                            chr.changeMap(mappz, mappz.getPortal(0));
                        }
                    }
                } else {
                    c.gainExp(exp, true, true, true);
                    if (c.getEventCount("Dojo_VALUE") < 5) {
                        c.setEventCount("Dojo_VALUE");
                        c.setEventCount("ACTIVE_VALUE");
                    }
                    c.dropMessage(5, "你在" + mode + "下通关了武陵道场。获得了" + exp + "点经验值。");
                    c.changeMap(mappz, mappz.getPortal(0));
                }
            }
        } catch (Exception rm) {
            rm.printStackTrace();
        }

        return false;
    }

    private static void updateQuestInfoAndChangeMap(MapleCharacter c, MapleMap currentmap, MapleMap mapz, int thisStage) {
        if (c.getParty() != null) {
            for (MaplePartyCharacter mem : c.getParty().getMembers()) {
                MapleCharacter chr = currentmap.getCharacterById(mem.getId());
                if (chr != null) {
                    if (!chr.isAlive()) {
                        chr.addHP(50);
                    }
                    if (thisStage % 6 == 0) {
                        chr.updateOneInfo(7215, "sTime", String.valueOf(System.currentTimeMillis()));
                    }
                    chr.changeMap(mapz, mapz.getPortal(0));
                }
            }
        } else {
            if (thisStage % 6 == 0) {
                c.updateOneInfo(7215, "sTime", String.valueOf(System.currentTimeMillis()));
            }
            c.changeMap(mapz, mapz.getPortal(0));
        }
    }

    private static void clearMap(final MapleMap map, final boolean check) {
        if (check) {
            if (map.getCharactersSize() != 0) {
                return;
            }
        }
        map.resetFully();
    }

    private static int getDojoPoints(final int stage) {

        switch (stage) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return 1;
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
                return 2;
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
                return 3;
            case 19:
            case 20:
            case 21:
            case 22:
            case 23:
                return 4;
            case 25:
            case 26:
            case 27:
            case 28:
            case 29:
                return 5;
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
                return 6;
            case 37:
            case 38:
                return 7;
            default:
                return 0;
        }
    }

    private static void spawnMonster(final MapleMap map, final int stage) {
        final int mobid;
        /*mobid = 9300183 + stage;
         if (mobid < 9300184 || mobid > 9300215) {
         return;
         }*/
////        mobid = 9305100 + stage - 1;
////        if (mobid < 9305100 || mobid > 9305139) {
////            return;
////        }
        //For normal stages
        final int[] mob1 = {9300209, 9300184, 9300191, 9300185};//bluedad, mano, mushmom, sumpy
        int stage1 = mob1[Randomizer.nextInt(mob1.length)];
        final int[] mob2 = {9300196, 9300187, 9300194, 9300190};//zombiemom, kingslime, dyle, kingclang
        int stage2 = mob2[Randomizer.nextInt(mob2.length)];
        final int[] mob3 = {9300189, 9300204};//faust, eliza
        int stage3 = mob3[Randomizer.nextInt(mob3.length)];
        final int[] mob4 = {9300203, 9300199};//jrbalrog, ninetail
        int stage4 = mob4[Randomizer.nextInt(mob4.length)];
        final int[] mob5 = {9300186, 9300197, 9300193};//deo, zeno, timer
        int stage5 = mob5[Randomizer.nextInt(mob5.length)];
        final int[] mob6 = {9300207, 9300200, 9300195};//snakbar, taerone, papapixie
        int stage6 = mob6[Randomizer.nextInt(mob6.length)];
        final int[] mob7 = {9300192, 9300198};//alishar, lordpirate
        int stage7 = mob7[Randomizer.nextInt(mob7.length)];
        final int[] mob8 = {9300205};//Frankenroid
        int stage8 = mob8[Randomizer.nextInt(mob8.length)];
        final int[] mob9 = {9300206};//chimera
        int stage9 = mob9[Randomizer.nextInt(mob9.length)];
        final int[] mob10 = {9300215};//mugong
        int stage10 = mob10[Randomizer.nextInt(mob10.length)];

        switch (stage) {
            case 1:
                mobid = stage1;
                break;
            case 2:
                mobid = stage2;
                break;
            case 3:
                mobid = stage3;
                break;
            case 4:
                mobid = stage4;
                break;
            case 5:
                mobid = stage5;
                break;
            case 7:
                mobid = stage6;
                break;
            case 8:
                mobid = stage7;
                break;
            case 9:
                mobid = stage8;
                break;
            case 10:
                mobid = stage9;
                break;
            case 11:
                if (map.getId() == 925021100) {//last stage normal
                    mobid = stage10;
                } else {
                    mobid = 9300201;//poison golem
                }
                break;
            case 13:
                mobid = 9300188; // Giant Centipede
                break;
            case 14:
                mobid = 9300211; // Manon
                break;
            case 15:
                mobid = 9300208; // Snowman
                break;
            case 16:
                mobid = 9300214; // Papulatus
                break;
            case 17:
                mobid = 9305134; // Ani
                break;
            case 19:
                mobid = 9300213; // Leviathan
                break;
            case 20:
                mobid = 9305136; // Dodo
                break;
            case 21:
                mobid = 9305137; // Lilynouch
                break;
            case 22:
                mobid = 9305138; // Lyka
                break;
            case 23:
                mobid = 9305139; // Mu Gong
                break;
            default:
                return;
        }
//        switch (stage) {
//         case 1:
//         mobid = 9300184; // Mano
//         break;
//         case 2:
//         mobid = 9300185; // Stumpy
//         break;
//         case 3:
//         mobid = 9300186; // Dewu
//         break;
//         case 4:
//         mobid = 9300187; // King Slime
//         break;
//         case 5:
//         mobid = 9300188; // Giant Centipede
//         break;
//         case 7:
//         mobid = 9300189; // Faust
//         break;
//         case 8:
//         mobid = 9300190; // King Clang
//         break;
//         case 9:
//         mobid = 9300191; // Mushmom
//         break;
//         case 10:
//         mobid = 9300192; // Alishar
//         break;
//         case 11:
//         mobid = 9300193; // Timer
//         break;
//         case 13:
//         mobid = 9300194; // Dale
//         break;
//         case 14:
//         mobid = 9300195; // Papa Pixie
//         break;
//         case 15:
//         mobid = 9300196; // Zombie Mushmom
//         break;
//         case 16:
//         mobid = 9300197; // Jeno
//         break;
//         case 17:
//         mobid = 9300198; // Lord Pirate
//         break;
//         case 19:
//         mobid = 9300199; // Old Fox
//         break;
//         case 20:
//         mobid = 9300200; // Tae Roon
//         break;
//         case 21:
//         mobid = 9300201; // Poison Golem
//         break;
//         case 22:
//         mobid = 9300202; // Ghost Priest
//         break;
//         case 23:
//         mobid = 9300203; // Jr. Balrog
//         break;
//         case 25:
//         mobid = 9300204; // Eliza
//         break;
//         case 26:
//         mobid = 9300205; // Frankenroid
//         break;
//         case 27:
//         mobid = 9300206; // Chimera
//         break;
//         case 28:
//         mobid = 9300207; // Snack Bar
//         break;
//         case 29:
//         mobid = 9300208; // Snowman
//         break;
//         case 31:
//         mobid = 9300209; // Blue Mushmom
//         break;
//         case 32:
//         mobid = 9300210; // Crimson Balrog
//         break;
//         case 33:
//         mobid = 9300211; // Manon
//         break;
//         case 34:
//         mobid = 9300212; // Griffey
//         break;
//         case 35:
//         mobid = 9300213; // Leviathan
//         break;
//         case 37:
//         mobid = 9300214; // Papulatus
//         break;
//         case 38:
//         mobid = 9300215; // Mu gong
//         break;
//         default:
//         return;
//         }
        if (mobid != 0) {
            final int rand = Randomizer.nextInt(3);

            MapTimer.getInstance().schedule(() -> map.spawnMonsterWithEffect(MapleLifeFactory.getMonster(mobid), 15, rand == 0 ? point1 : rand == 1 ? point2 : point3), 3000);
        }
    }
}
