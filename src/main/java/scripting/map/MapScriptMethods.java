/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.map;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.skills.SkillEntry;
import client.skills.SkillFactory;
import constants.GameConstants;
import constants.skills.双弩;
import constants.skills.恶魔猎手;
import constants.skills.战神;
import lombok.extern.log4j.Log4j2;
import scripting.AbstractPlayerInteraction;
import scripting.event.EventManager;
import scripting.npc.NPCScriptManager;
import server.MapleItemInformationProvider;
import server.Timer;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.OverrideMonsterStats;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import server.maps.MapleNodes;
import server.maps.events.Event_PyramidSubway;
import server.quest.MapleQuest;
import server.quest.MedalQuest;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.packet.EffectPacket;
import tools.packet.UIPacket;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
@Log4j2
public class MapScriptMethods extends AbstractPlayerInteraction {

    private static final Point witchTowerPos = new Point(-60, 184);
    private static final String[] mulungEffects = {
            "胆子真够大的！别把鲁莽和勇敢混为一谈！",
            "想挑战武陵道场…还真有勇气！",
            "我等你！还有勇气的话，欢迎再来挑战！",
            "挑战武陵道场的家伙，我一定会让他(她)后悔！！",
            "想被称呼为失败者吗？欢迎来挑战！"};

    public MapScriptMethods(MapleClient c) {
        super(c);
    }

    public static void startDirectionInfo(MapleCharacter chr, boolean start) {
        final MapleClient c = chr.getClient();
        MapleNodes.DirectionInfo di = chr.getMap().getDirectionInfo(start ? 0 : chr.getDirection());
        if (di != null && di.eventQ.size() > 0) {
            if (start) {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.getDirectionInfo(3, 4));
            } else {
                for (String s : di.eventQ) {
                    switch (directionInfo.fromString(s)) {
                        case merTutorDrecotion01: //direction info: 1 is probably the time
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/0", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion02:
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/1", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion03:
                            c.announce(UIPacket.getDirectionInfo(3, 2));
                            c.announce(UIPacket.getDirectionStatus(true));
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/2", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion04:
                            c.announce(UIPacket.getDirectionInfo(3, 2));
                            c.announce(UIPacket.getDirectionStatus(true));
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/3", 2000, 0, -100, 1));
                            break;
                        case merTutorDrecotion05:
                            c.announce(UIPacket.getDirectionInfo(3, 2));
                            c.announce(UIPacket.getDirectionStatus(true));
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/4", 2000, 0, -100, 1));
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(3, 2));
                                c.announce(UIPacket.getDirectionStatus(true));
                                c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/5", 2000, 0, -100, 1));
                            }, 2000);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.IntroEnableUI(0));
                                c.announce(MaplePacketCreator.enableActions());
                            }, 4000);
                            break;
                        case merTutorDrecotion12:
                            c.announce(UIPacket.getDirectionInfo(3, 2));
                            c.announce(UIPacket.getDirectionStatus(true));
                            c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/8", 2000, 0, -100, 1));
                            c.announce(UIPacket.IntroEnableUI(0));
                            break;
                        case merTutorDrecotion21:
                            c.announce(UIPacket.getDirectionInfo(3, 1));
                            c.announce(UIPacket.getDirectionStatus(true));
                            MapleMap mapto = c.getChannelServer().getMapFactory().getMap(910150005);
                            c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                            break;
                        case ds_tuto_0_2:
                            c.announce(MaplePacketCreator.showEffect("demonSlayer/text1"));
                            break;
                        case ds_tuto_0_1:
                            c.announce(UIPacket.getDirectionInfo(3, 2));
                            break;
                        case ds_tuto_0_3:
                            c.announce(MaplePacketCreator.showEffect("demonSlayer/text2"));
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(1, 4000));
                                c.announce(MaplePacketCreator.showEffect("demonSlayer/text3"));
                            }, 2000);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(1, 500));
                                c.announce(MaplePacketCreator.showEffect("demonSlayer/text4"));
                            }, 6000);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(1, 4000));
                                c.announce(MaplePacketCreator.showEffect("demonSlayer/text5"));
                            }, 6500);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(1, 500));
                                c.announce(MaplePacketCreator.showEffect("demonSlayer/text6"));
                            }, 10500);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(1, 4000));
                                c.announce(MaplePacketCreator.showEffect("demonSlayer/text7"));
                            }, 11000);
                            Timer.EventTimer.getInstance().schedule(() -> {
                                c.announce(UIPacket.getDirectionInfo(4, 2159307));
                                NPCScriptManager.getInstance().dispose(c);
                                NPCScriptManager.getInstance().start(c, 2159307);
                            }, 15000);
                            break;
                    }
                }
            }
            c.announce(UIPacket.getDirectionInfo(1, 2000));
            chr.setDirection(chr.getDirection() + 1);
            if (chr.getMap().getDirectionInfo(chr.getDirection()) == null) {
                chr.setDirection(-1);
            }
        } else if (start) {
            switch (chr.getMapId()) {
                //hack
                case 931050300:
                    while (chr.getLevel() < 10) {
                        chr.levelUp();
                    }
                    MapleMap mapto = c.getChannelServer().getMapFactory().getMap(931050000);
                    chr.changeMap(mapto, mapto.getPortal(0));
                    break;
            }
        }
    }

    public static void startScript_FirstUser(MapleClient c, String scriptName) {
        if (c.getPlayer() == null) {
            return;
        } //o_O
        switch (onFirstUserEnter.fromString(scriptName)) {
            case dojang_Eff: {
                int temp = (c.getPlayer().getMapId() - 925000000) / 100;
                int stage = temp - ((temp / 100) * 100);
                sendDojoClock(c, getTiming(stage) * 60);
                sendDojoStart(c, stage - getDojoStageDec(stage));
                break;
            }
            case PinkBeen_before: {
                handlePinkBeanStart(c);
                break;
            }
            case onRewordMap: {
                reloadWitchTower(c);
                break;
            }
            //5120019 = orbis(start_itemTake - onUser)
            case moonrabbit_mapEnter: {
                c.getPlayer().getMap().startMapEffect("Gather the Primrose Seeds around the moon and protect the Moon Bunny!", 5120016);
                break;
            }
            case StageMsg_goddess: {
                switch (c.getPlayer().getMapId()) {
                    case 920010000:
                        c.getPlayer().getMap().startMapEffect("Please save me by collecting Cloud Pieces!", 5120019);
                        break;
                    case 920010100:
                        c.getPlayer().getMap().startMapEffect("Bring all the pieces here to save Minerva!", 5120019);
                        break;
                    case 920010200:
                        c.getPlayer().getMap().startMapEffect("Destroy the monsters and gather Statue Pieces!", 5120019);
                        break;
                    case 920010300:
                        c.getPlayer().getMap().startMapEffect("Destroy the monsters in each room and gather Statue Pieces!", 5120019);
                        break;
                    case 920010400:
                        c.getPlayer().getMap().startMapEffect("Play the correct LP of the day!", 5120019);
                        break;
                    case 920010500:
                        c.getPlayer().getMap().startMapEffect("Find the correct combination!", 5120019);
                        break;
                    case 920010600:
                        c.getPlayer().getMap().startMapEffect("Destroy the monsters and gather Statue Pieces!", 5120019);
                        break;
                    case 920010700:
                        c.getPlayer().getMap().startMapEffect("Get the right combination once you get to the top!", 5120019);
                        break;
                    case 920010800:
                        c.getPlayer().getMap().startMapEffect("Summon and defeat Papa Pixie!", 5120019);
                        break;
                }
                break;
            }
            case StageMsg_crack: {
                switch (c.getPlayer().getMapId()) {
                    case 922010100:
                        c.getPlayer().getMap().startMapEffect("Defeat all the Ratz!", 5120018);
                        break;
                    case 922010200:
                        c.getPlayer().getMap().startSimpleMapEffect("Collect all the passes!", 5120018);
                        break;
                    case 922010300:
                        c.getPlayer().getMap().startMapEffect("Destroy the monsters!", 5120018);
                        break;
                    case 922010400:
                        c.getPlayer().getMap().startMapEffect("Destroy the monsters in each room!", 5120018);
                        break;
                    case 922010500:
                        c.getPlayer().getMap().startMapEffect("Collect passes from each room!", 5120018);
                        break;
                    case 922010600:
                        c.getPlayer().getMap().startMapEffect("Get to the top!", 5120018);
                        break;
                    case 922010700:
                        c.getPlayer().getMap().startMapEffect("Destroy the Rombots!", 5120018);
                        break;
                    case 922010800:
                        c.getPlayer().getMap().startSimpleMapEffect("Get the right combination!", 5120018);
                        break;
                    case 922010900:
                        c.getPlayer().getMap().startMapEffect("Defeat Alishar!", 5120018);
                        break;
                }
                break;
            }
            case StageMsg_together: {
                switch (c.getPlayer().getMapId()) {
                    case 103000800:
                        c.getPlayer().getMap().startMapEffect("Solve the question and gather the amount of passes!", 5120017);
                        break;
                    case 103000801:
                        c.getPlayer().getMap().startMapEffect("Get on the ropes and unveil the correct combination!", 5120017);
                        break;
                    case 103000802:
                        c.getPlayer().getMap().startMapEffect("Get on the platforms and unveil the correct combination!", 5120017);
                        break;
                    case 103000803:
                        c.getPlayer().getMap().startMapEffect("Get on the barrels and unveil the correct combination!", 5120017);
                        break;
                    case 103000804:
                        c.getPlayer().getMap().startMapEffect("Defeat King Slime and his minions!", 5120017);
                        break;
                }
                break;
            }
            case StageMsg_romio: {
                switch (c.getPlayer().getMapId()) {
                    case 926100000:
                        c.getPlayer().getMap().startMapEffect("Please find the hidden door by investigating the Lab!", 5120021);
                        break;
                    case 926100001:
                        c.getPlayer().getMap().startMapEffect("Find  your way through this darkness!", 5120021);
                        break;
                    case 926100100:
                        c.getPlayer().getMap().startMapEffect("Fill the beakers to power the energy!", 5120021);
                        break;
                    case 926100200:
                        c.getPlayer().getMap().startMapEffect("Get the files for the experiment through each door!", 5120021);
                        break;
                    case 926100203:
                        c.getPlayer().getMap().startMapEffect("Please defeat all the monsters!", 5120021);
                        break;
                    case 926100300:
                        c.getPlayer().getMap().startMapEffect("Find your way through the Lab!", 5120021);
                        break;
                    case 926100401:
                        c.getPlayer().getMap().startMapEffect("Please, protect my love!", 5120021);
                        break;
                }
                break;
            }
            case StageMsg_juliet: {
                switch (c.getPlayer().getMapId()) {
                    case 926110000:
                        c.getPlayer().getMap().startMapEffect("Please find the hidden door by investigating the Lab!", 5120022);
                        break;
                    case 926110001:
                        c.getPlayer().getMap().startMapEffect("Find  your way through this darkness!", 5120022);
                        break;
                    case 926110100:
                        c.getPlayer().getMap().startMapEffect("Fill the beakers to power the energy!", 5120022);
                        break;
                    case 926110200:
                        c.getPlayer().getMap().startMapEffect("Get the files for the experiment through each door!", 5120022);
                        break;
                    case 926110203:
                        c.getPlayer().getMap().startMapEffect("Please defeat all the monsters!", 5120022);
                        break;
                    case 926110300:
                        c.getPlayer().getMap().startMapEffect("Find your way through the Lab!", 5120022);
                        break;
                    case 926110401:
                        c.getPlayer().getMap().startMapEffect("Please, protect my love!", 5120022);
                        break;
                }
                break;
            }
            case party6weatherMsg: {
                switch (c.getPlayer().getMapId()) {
                    case 930000000:
                        c.getPlayer().getMap().startMapEffect("Step in the portal to be transformed.", 5120023);
                        break;
                    case 930000100:
                        c.getPlayer().getMap().startMapEffect("Defeat the poisoned monsters!", 5120023);
                        break;
                    case 930000200:
                        c.getPlayer().getMap().startMapEffect("Eliminate the spore that blocks the way by purifying the poison!", 5120023);
                        break;
                    case 930000300:
                        c.getPlayer().getMap().startMapEffect("Uh oh! The forest is too confusing! Find me, quick!", 5120023);
                        break;
                    case 930000400:
                        c.getPlayer().getMap().startMapEffect("Purify the monsters by getting Purification Marbles from me!", 5120023);
                        break;
                    case 930000500:
                        c.getPlayer().getMap().startMapEffect("Find the Purple Magic Stone!", 5120023);
                        break;
                    case 930000600:
                        c.getPlayer().getMap().startMapEffect("Place the Magic Stone on the altar!", 5120023);
                        break;
                }
                break;
            }
            case prisonBreak_mapEnter: {
                break;
            }
            case StageMsg_davy: {
                switch (c.getPlayer().getMapId()) {
                    case 925100000:
                        c.getPlayer().getMap().startMapEffect("Defeat the monsters outside of the ship to advance!", 5120020);
                        break;
                    case 925100100:
                        c.getPlayer().getMap().startMapEffect("We must prove ourselves! Get me Pirate Medals!", 5120020);
                        break;
                    case 925100200:
                        c.getPlayer().getMap().startMapEffect("Defeat the guards here to pass!", 5120020);
                        break;
                    case 925100300:
                        c.getPlayer().getMap().startMapEffect("Eliminate the guards here to pass!", 5120020);
                        break;
                    case 925100400:
                        c.getPlayer().getMap().startMapEffect("Lock the doors! Seal the root of the Ship's power!", 5120020);
                        break;
                    case 925100500:
                        c.getPlayer().getMap().startMapEffect("Destroy the Lord Pirate!", 5120020);
                        break;
                }
                final EventManager em = c.getChannelServer().getEventSM().getEventManager("Pirate");
                if (c.getPlayer().getMapId() == 925100500 && em != null && em.getProperty("stage5") != null) {
                    int mobId = Randomizer.nextBoolean() ? 9300107 : 9300119; //lord pirate
                    final int st = Integer.parseInt(em.getProperty("stage5"));
                    switch (st) {
                        case 1:
                            mobId = Randomizer.nextBoolean() ? 9300119 : 9300105; //angry
                            break;
                        case 2:
                            mobId = Randomizer.nextBoolean() ? 9300106 : 9300105; //enraged
                            break;
                    }
                    MapleMonster shammos = MapleLifeFactory.getMonster(mobId);
                    if (c.getPlayer().getEventInstance() != null) {
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                    }
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(shammos, new Point(411, 236));
                }
                break;
            }
            case astaroth_summon: {
                c.getPlayer().getMap().resetFully();
                break;
            }
            case boss_Ravana_mirror:
            case boss_Ravana: { //event handles this so nothing for now until i find out something to do with it
                c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.serverNotice(5, "拉瓦特出现了。"));
                break;
            }
            case killing_BonusSetting: { //spawns monsters according to mapid
                //910320010-910320029 = Train 999 bubblings.
                //926010010-926010029 = 30 Yetis
                //926010030-926010049 = 35 Yetis
                //926010050-926010069 = 40 Yetis
                //926010070-926010089 - 50 Yetis (specialized? immortality)
                //TODO also find positions to spawn these at
                c.getPlayer().getMap().resetFully();
                c.announce(MaplePacketCreator.showEffect("killing/bonus/bonus"));
                c.announce(MaplePacketCreator.showEffect("killing/bonus/stage"));
                Point pos1 = null, pos2 = null, pos3 = null;
                int spawnPer = 0;
                int mobId = 0;
                //9700019, 9700029
                //9700021 = one thats invincible
                if (c.getPlayer().getMapId() >= 910320010 && c.getPlayer().getMapId() <= 910320029) {
                    pos1 = new Point(121, 218);
                    pos2 = new Point(396, 43);
                    pos3 = new Point(-63, 43);
                    mobId = 9700020;
                    spawnPer = 10;
                } else if (c.getPlayer().getMapId() >= 926010010 && c.getPlayer().getMapId() <= 926010029) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 10;
                } else if (c.getPlayer().getMapId() >= 926010030 && c.getPlayer().getMapId() <= 926010049) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 15;
                } else if (c.getPlayer().getMapId() >= 926010050 && c.getPlayer().getMapId() <= 926010069) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700019;
                    spawnPer = 20;
                } else if (c.getPlayer().getMapId() >= 926010070 && c.getPlayer().getMapId() <= 926010089) {
                    pos1 = new Point(0, 88);
                    pos2 = new Point(-326, -115);
                    pos3 = new Point(361, -115);
                    mobId = 9700029;
                    spawnPer = 20;
                } else {
                    break;
                }
                for (int i = 0; i < spawnPer; i++) {
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos1));
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos2));
                    c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mobId), new Point(pos3));
                }

                c.getPlayer().startMapTimeLimitTask(120, c.getPlayer().getMap().getReturnMap());
                break;
            }
            case mPark_summonBoss: {
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getEventInstance().getProperty("boss") != null && c.getPlayer().getEventInstance().getProperty("boss").equals("0")) {
                    for (int i = 9800119; i < 9800125; i++) {
                        final MapleMonster boss = MapleLifeFactory.getMonster(i);
                        c.getPlayer().getEventInstance().registerMonster(boss);
                        c.getPlayer().getMap().spawnMonsterOnGroundBelow(boss, new Point(c.getPlayer().getMap().getPortal(2).getPosition()));
                    }
                }
                break;
            }
            case shammos_Fenter: {
                if (c.getPlayer().getMapId() >= 921120005 && c.getPlayer().getMapId() < 921120500) {
                    MapleMonster shammos = MapleLifeFactory.getMonster(9300275);
                    if (c.getPlayer().getEventInstance() != null) {
                        int averageLevel = 0, size = 0;
                        for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                            averageLevel += pl.getLevel();
                            size++;
                        }
                        if (size <= 0) {
                            return;
                        }
                        averageLevel /= size;
                        shammos.changeLevel(averageLevel);
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                        if (c.getPlayer().getEventInstance().getProperty("HP") == null) {
                            c.getPlayer().getEventInstance().setProperty("HP", averageLevel + "000");
                        }
                        shammos.setHp(Long.parseLong(c.getPlayer().getEventInstance().getProperty("HP")));
                    }
                    c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(c.getPlayer().getMap().getPortal(0).getPosition()), 12);
                    shammos.switchController(c.getPlayer(), false);

                    /*
                     * } else if (c.getPlayer().getMapId() == (GameConstants.GMS ? 921120300 : 921120500) && c.getPlayer().getMap().getAllMonstersThreadsafe().size() == 0) {
                     * final MapleMonster shammos = MapleLifeFactory.getMonster(9300281);
                     * if (c.getPlayer().getEventInstance() != null) {
                     * int averageLevel = 0, size = 0;
                     * for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                     * averageLevel += pl.getLevel();
                     * size++;
                     * }
                     * if (size <= 0) {
                     * return;
                     * }
                     * averageLevel /= size;
                     * shammos.changeLevel(Math.max(120, Math.min(200, averageLevel)));
                     * }
                     * c.getPlayer().getMap().spawnMonsterOnGroundBelow(shammos, new Point(350, 170));
                     */
                }
                break;
            }
            //5120038 =  dr bing. 5120039 = visitor lady. 5120041 = unknown dr bing.
            case iceman_FEnter: {
                if (c.getPlayer().getMapId() >= 932000100 && c.getPlayer().getMapId() < 932000300) {
                    final MapleMonster shammos = MapleLifeFactory.getMonster(9300438);
                    if (c.getPlayer().getEventInstance() != null) {
                        int averageLevel = 0, size = 0;
                        for (MapleCharacter pl : c.getPlayer().getEventInstance().getPlayers()) {
                            averageLevel += pl.getLevel();
                            size++;
                        }
                        if (size <= 0) {
                            return;
                        }
                        averageLevel /= size;
                        shammos.changeLevel(averageLevel);
                        c.getPlayer().getEventInstance().registerMonster(shammos);
                        if (c.getPlayer().getEventInstance().getProperty("HP") == null) {
                            c.getPlayer().getEventInstance().setProperty("HP", averageLevel + "000");
                        }
                        shammos.setHp(Long.parseLong(c.getPlayer().getEventInstance().getProperty("HP")));
                    }
                    c.getPlayer().getMap().spawnMonsterWithEffectBelow(shammos, new Point(c.getPlayer().getMap().getPortal(0).getPosition()), 12);
                    shammos.switchController(c.getPlayer(), false);
                }
                break;
            }
            case PRaid_D_Fenter: {
                switch (c.getPlayer().getMapId() % 10) {
                    case 0:
                        c.getPlayer().getMap().startMapEffect("Eliminate all the monsters!", 5120033);
                        break;
                    case 1:
                        c.getPlayer().getMap().startMapEffect("Break the boxes and eliminate the monsters!", 5120033);
                        break;
                    case 2:
                        c.getPlayer().getMap().startMapEffect("Eliminate the Officer!", 5120033);
                        break;
                    case 3:
                        c.getPlayer().getMap().startMapEffect("Eliminate all the monsters!", 5120033);
                        break;
                    case 4:
                        c.getPlayer().getMap().startMapEffect("Find the way to the other side!", 5120033);
                        break;
                }
                break;
            }
            case PRaid_B_Fenter: {
                c.getPlayer().getMap().startMapEffect("Defeat the Ghost Ship Captain!", 5120033);
                break;
            }
            case summon_pepeking: {
                c.getPlayer().getMap().resetFully();
                final int rand = Randomizer.nextInt(10);
                int mob_ToSpawn = 100100;
                if (rand >= 4) { //60%
                    mob_ToSpawn = 3300007;
                } else if (rand >= 1) {
                    mob_ToSpawn = 3300006;
                } else {
                    mob_ToSpawn = 3300005;
                }
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(mob_ToSpawn), c.getPlayer().getPosition());
                break;
            }
            case Xerxes_summon: {
                c.getPlayer().getMap().resetFully();
                c.getPlayer().getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(6160003), c.getPlayer().getPosition());
                break;
            }
            case shammos_FStart:
                c.getPlayer().getMap().startMapEffect("Defeat the monsters!", 5120035);
                break;
            case kenta_mapEnter:
                switch ((c.getPlayer().getMapId() / 100) % 10) {
                    case 1:
                        c.getPlayer().getMap().startMapEffect("Eliminate all the monsters!", 5120052);
                        break;
                    case 2:
                        c.getPlayer().getMap().startMapEffect("Get me 20 Air Bubbles for me to survive!", 5120052);
                        break;
                    case 3:
                        c.getPlayer().getMap().startMapEffect("Help! Make sure I live for three minutes!", 5120052);
                        break;
                    case 4:
                        c.getPlayer().getMap().startMapEffect("Eliminate the two Pianus!", 5120052);
                        break;
                } //TODOO find out which one it really is, lol
                break;
            case cygnus_Summon: {
                c.getPlayer().getMap().startMapEffect("好久没看到有人来了，但是也没看到有人可以回去。", 5120043);
                break;
            }
            case iceman_Boss: {
                c.getPlayer().getMap().startMapEffect("You will perish!", 5120050);
                break;
            }
            case Visitor_Cube_poison: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the monsters!", 5120039);
                break;
            }
            case Visitor_Cube_Hunting_Enter_First: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the Visitors!", 5120039);
                break;
            }
            case VisitorCubePhase00_Start: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the flying monsters!", 5120039);
                break;
            }
            case visitorCube_addmobEnter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all the monsters by moving around the map!", 5120039);
                break;
            }
            case Visitor_Cube_PickAnswer_Enter_First_1: {
                c.getPlayer().getMap().startMapEffect("One of the aliens must have a clue to the way out.", 5120039);
                break;
            }
            case visitorCube_medicroom_Enter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all of the Unjust Visitors!", 5120039);
                break;
            }
            case visitorCube_iceyunna_Enter: {
                c.getPlayer().getMap().startMapEffect("Eliminate all of the Speedy Visitors!", 5120039);
                break;
            }
            case Visitor_Cube_AreaCheck_Enter_First: {
                c.getPlayer().getMap().startMapEffect("The switch at the top of the room requires a heavy weight.", 5120039);
                break;
            }
            case visitorCube_boomboom_Enter: {
                c.getPlayer().getMap().startMapEffect("The enemy is powerful! Watch out!", 5120039);
                break;
            }
            case visitorCube_boomboom2_Enter: {
                c.getPlayer().getMap().startMapEffect("This Visitor is strong! Be careful!", 5120039);
                break;
            }
            case CubeBossbang_Enter: {
                c.getPlayer().getMap().startMapEffect("This is it! Give it your best shot!", 5120039);
                break;
            }
            case MalayBoss_Int:
            case storymap_scenario:
            case VanLeon_Before:
            case dojang_Msg:
            case balog_summon:
            case easy_balog_summon: { //we dont want to reset
                break;
            }
            case metro_firstSetting:
            case killing_MapSetting:
            case Sky_TrapFEnter:
            case balog_bonusSetting: { //not needed
                c.getPlayer().getMap().resetFully();
                break;
            }
            default: {
                log.error("地图触发: 未找到 " + scriptName + ", 类型: onFirstUserEnter - 地图ID " + c.getPlayer().getMapId());
                NPCScriptManager.getInstance().onFirstUserEnter(c, scriptName);
                break;
            }
        }
    }

    private static void sendDojoClock(MapleClient c, int time) {
        c.announce(MaplePacketCreator.getClock(time));
    }

    private static void sendDojoStart(MapleClient c, int stage) {
        c.announce(MaplePacketCreator.environmentChange("Dojang/start", 0x05));
        c.announce(MaplePacketCreator.environmentChange("dojang/start/stage", 0x0C));
        c.announce(MaplePacketCreator.environmentChange("dojang/start/number/" + stage, 0x0C));
        if (stage == 1) {
            c.getPlayer().getMap().startMapEffect("别忘了限制时间是10分钟，只要在时间内打倒怪物，进入下一层就行！", 5120024);
        }
        //c.announce(MaplePacketCreator.trembleEffect(0, 1));
    }

    private static void handlePinkBeanStart(MapleClient c) {
        MapleMap map = c.getPlayer().getMap();
        map.resetFully();
        if (!map.containsNPC(2141000)) {
            map.spawnNpc(2141000, new Point(-190, -42));
        }
    }

    private static int getTiming(int ids) {
        if (ids <= 5) {
            return 5;
        } else if (ids >= 7 && ids <= 11) {
            return 6;
        } else if (ids >= 13 && ids <= 17) {
            return 7;
        } else if (ids >= 19 && ids <= 23) {
            return 8;
        } else if (ids >= 25 && ids <= 29) {
            return 9;
        } else if (ids >= 31 && ids <= 35) {
            return 10;
        } else if (ids >= 37 && ids <= 38) {
            return 15;
        }
        return 0;
    }

    private static int getDojoStageDec(int ids) {
        if (ids <= 5) {
            return 0;
        } else if (ids >= 7 && ids <= 11) {
            return 1;
        } else if (ids >= 13 && ids <= 17) {
            return 2;
        } else if (ids >= 19 && ids <= 23) {
            return 3;
        } else if (ids >= 25 && ids <= 29) {
            return 4;
        } else if (ids >= 31 && ids <= 35) {
            return 5;
        } else if (ids >= 37 && ids <= 38) {
            return 6;
        }
        return 0;
    }

    public static void startScript_User(final MapleClient c, String scriptName) {
        if (c.getPlayer() == null) {
            return;
        } //o_O
        String data = "";
        switch (onUserEnter.fromString(scriptName)) {
            case cannon_tuto_direction: {
                showIntro(c, "Effect/Direction4.img/cannonshooter/Scene00");
                showIntro(c, "Effect/Direction4.img/cannonshooter/out00");
                break;
            }
            case cannon_tuto_direction1: {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.IntroLock(true));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/0", 5000, 0, 0, 1));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/1", 5000, 0, 0, 1));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction4.img/effect/cannonshooter/balloon/2", 5000, 0, 0, 1));
                c.announce(EffectPacket.ShowWZEffect("Effect/Direction4.img/cannonshooter/face04"));
                c.announce(EffectPacket.ShowWZEffect("Effect/Direction4.img/cannonshooter/out01"));
                c.announce(UIPacket.getDirectionInfo(1, 5000));
                break;
            }
            case cannon_tuto_direction2: {
                showIntro(c, "Effect/Direction4.img/cannonshooter/Scene01");
                showIntro(c, "Effect/Direction4.img/cannonshooter/out02");
                break;
            }
            case cygnusTest: {
                showIntro(c, "Effect/Direction.img/cygnus/Scene" + (c.getPlayer().getMapId() == 913040006 ? 9 : (c.getPlayer().getMapId() - 913040000)));
                break;
            }
            case cygnusJobTutorial: {
                showIntro(c, "Effect/Direction.img/cygnusJobTutorial/Scene" + (c.getPlayer().getMapId() - 913040100));
                break;
            }
            case shammos_Enter: { //nothing to go on inside the map
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getMapId() == 921120500) {
                    NPCScriptManager.getInstance().dispose(c); //only boss map.
                    c.removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 2022006);
                }
                break;
            }
            case iceman_Enter: { //nothing to go on inside the map
                if (c.getPlayer().getEventInstance() != null && c.getPlayer().getMapId() == 932000300) {
                    NPCScriptManager.getInstance().dispose(c); //only boss map.
                    c.removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 2159020);
                }
                break;
            }
            case start_itemTake: { //nothing to go on inside the map
                final EventManager em = c.getChannelServer().getEventSM().getEventManager("OrbisPQ");
                if (em != null && em.getProperty("pre").equals("0")) {
                    NPCScriptManager.getInstance().dispose(c);
                    c.removeClickedNPC();
                    NPCScriptManager.getInstance().start(c, 2013001);
                }
                break;
            }
            case PRaid_W_Enter: {
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_expPenalty", "0"));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_ElapssedTimeAtField", "0"));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_Point", "-1"));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_Bonus", "-1"));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_Total", "-1"));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_Team", ""));
                c.announce(MaplePacketCreator.sendPyramidEnergy("PRaid_IsRevive", "0"));
                c.getPlayer().writePoint("PRaid_Point", "-1");
                c.getPlayer().writeStatus("Red_Stage", "1");
                c.getPlayer().writeStatus("Blue_Stage", "1");
                c.getPlayer().writeStatus("redTeamDamage", "0");
                c.getPlayer().writeStatus("blueTeamDamage", "0");
                break;
            }
            case jail: {
                if (!c.getPlayer().isIntern()) {
                    c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_TIME)).setCustomData(String.valueOf(System.currentTimeMillis()));
                    final MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST));
                    if (stat.getCustomData() != null) {
                        final int seconds = Integer.parseInt(stat.getCustomData());
                        if (seconds > 0) {
                            c.getPlayer().startMapTimeLimitTask(seconds, c.getChannelServer().getMapFactory().getMap(100000000));
                        }
                    }
                }
                break;
            }
            case TD_neo_BossEnter:
            case findvioleta: {
                c.getPlayer().getMap().resetFully();
                break;
            }
            case StageMsg_crack:
                if (c.getPlayer().getMapId() == 922010400) { //2nd stage
                    MapleMapFactory mf = c.getChannelServer().getMapFactory();
                    int q = 0;
                    for (int i = 0; i < 5; i++) {
                        q += mf.getMap(922010401 + i).getAllMonstersThreadsafe().size();
                    }
                    if (q > 0) {
                        c.getPlayer().dropMessage(-1, "There are still " + q + " monsters remaining.");
                    }
                } else if (c.getPlayer().getMapId() >= 922010401 && c.getPlayer().getMapId() <= 922010405) {
                    if (c.getPlayer().getMap().getAllMonstersThreadsafe().size() > 0) {
                        c.getPlayer().dropMessage(-1, "There are still some monsters remaining in this map.");
                    } else {
                        c.getPlayer().dropMessage(-1, "There are no monsters remaining in this map.");
                    }
                }
                break;
            case q31102e:
                if (c.getPlayer().getQuestStatus(31102) == 1) {
                    MapleQuest.getInstance(31102).forceComplete(c.getPlayer(), 2140000);
                }
                break;
            case q31103s:
                if (c.getPlayer().getQuestStatus(31103) == 0) {
                    MapleQuest.getInstance(31103).forceComplete(c.getPlayer(), 2142003);
                }
                break;
            case Resi_tutor20:
                c.announce(UIPacket.showMapEffect("resistance/tutorialGuide"));
                break;
            case Resi_tutor30:
                c.announce(EffectPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/resistanceTutorial/userTalk"));
                break;
            case Resi_tutor40:
                NPCScriptManager.getInstance().dispose(c);
                c.removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 2159012);
                break;
            case Resi_tutor50:
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                c.announce(MaplePacketCreator.enableActions());
                NPCScriptManager.getInstance().dispose(c);
                c.removeClickedNPC();
                NPCScriptManager.getInstance().start(c, 2159006);
                break;
            case Resi_tutor70:
                showIntro(c, "Effect/Direction4.img/Resistance/TalkJ");
                break;
            case prisonBreak_1stageEnter:
            case shammos_Start:
            case moonrabbit_takeawayitem:
            case TCMobrevive:
            case cygnus_ExpeditionEnter:
            case knights_Summon:
            case VanLeon_ExpeditionEnter:
            case Resi_tutor10:
            case Resi_tutor60:
            case Resi_tutor50_1:
            case sealGarden:
            case in_secretroom:
            case TD_MC_gasi2:
            case TD_MC_keycheck:
            case pepeking_effect:
            case userInBattleSquare:
            case summonSchiller:
            case VisitorleaveDirectionMode:
            case visitorPT_Enter:
            case VisitorCubePhase00_Enter:
            case visitor_ReviveMap:
            case PRaid_D_Enter:
            case PRaid_B_Enter:
            case PRaid_WinEnter: //handled by event
            case PRaid_FailEnter: //also
            case PRaid_Revive: //likely to subtract points or remove a life, but idc rly
            case metro_firstSetting:
            case blackSDI:
            case summonIceWall:
            case onSDI:
            case enterBlackfrog:
            case Sky_Quest: //forest that disappeared 240030102
            case dollCave00:
            case dollCave01:
            case dollCave02:
            case shammos_Base:
            case shammos_Result:
            case Sky_BossEnter:
            case Sky_GateMapEnter:
            case balog_dateSet:
            case balog_buff:
            case outCase:
            case Sky_StageEnter:
            case dojang_QcheckSet:
            case evanTogether:
            case merStandAlone:
            case EntereurelTW:
            case aranTutorAlone:
            case evanAlone: { //no idea
                c.announce(MaplePacketCreator.enableActions());
                break;
            }
            case merOutStandAlone: {
                if (c.getPlayer().getQuestStatus(24001) == 1) {
                    MapleQuest.getInstance(24001).forceComplete(c.getPlayer(), 0);
                    c.getPlayer().dropMessage(5, "Quest complete.");
                }
                break;
            }
            case merTutorSleep00: {
                showIntro(c, "Effect/Direction5.img/mersedesTutorial/Scene0");
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                sDate.put(20021181, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(20021166, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(双弩.精灵恢复, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(双弩.精灵的祝福, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(双弩.优雅移动, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(双弩.王者资格, new SkillEntry((byte) 1, (byte) 1, -1));
                c.getPlayer().changeSkillsLevel(sDate);
                break;
            }
            case merTutorSleep01: {
                while (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().levelUp();
                }
                c.getPlayer().changeJob(2300);
                showIntro(c, "Effect/Direction5.img/mersedesTutorial/Scene1");
                break;
            }
            case merTutorSleep02: {
                c.announce(UIPacket.IntroEnableUI(0));
                break;
            }
            case merTutorDrecotion00: {
                c.announce(UIPacket.playMovie("Mercedes.avi", true));
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                sDate.put(20021181, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(20021166, new SkillEntry((byte) 1, (byte) 1, -1));
                c.getPlayer().changeSkillsLevel(sDate);
                break;
            }
            case merTutorDrecotion10: {
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/6", 2000, 0, -100, 1));
                c.announce(UIPacket.getDirectionInfo(1, 2000));
                c.getPlayer().setDirection(0);
                break;
            }
            case merTutorDrecotion20: {
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/mercedesInIce/merBalloon/9", 2000, 0, -100, 1));
                c.announce(UIPacket.getDirectionInfo(1, 2000));
                c.getPlayer().setDirection(0);
                break;
            }
            case ds_tuto_ani: {
                c.announce(UIPacket.playMovie("DemonSlayer1.avi", true));
                break;
            }
            case Resi_tutor80:
            case startEreb:
            case mirrorCave:
            case babyPigMap:
            case evanleaveD: {
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                c.announce(MaplePacketCreator.enableActions());
                break;
            }
            case dojang_Msg: {
                if (c.getPlayer().getMap().getId() == 925020000) {
                    c.getPlayer().getMap().startMapEffect(mulungEffects[Randomizer.nextInt(mulungEffects.length)], 5120024);
                } else {
                    c.getPlayer().getMap().startMapEffect("哈哈！让我看看你，我不会让你离开的。除非你先打败我！", 5120024);
                }
                break;
            }
            case dojang_1st: {
                c.getPlayer().writeMulungEnergy();
                break;
            }
            case undomorphdarco:
            case reundodraco: {
                c.getPlayer().cancelEffect(MapleItemInformationProvider.getInstance().getItemEffect(2210016), false, -1);
                break;
            }
            case goAdventure: {
                showIntro(c, "Effect/Direction3.img/goAdventure/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case crash_Dragon:
                showIntro(c, "Effect/Direction4.img/crash/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case getDragonEgg:
                showIntro(c, "Effect/Direction4.img/getDragonEgg/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case meetWithDragon:
                showIntro(c, "Effect/Direction4.img/meetWithDragon/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case PromiseDragon:
                showIntro(c, "Effect/Direction4.img/PromiseDragon/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            case evanPromotion:
                switch (c.getPlayer().getMapId()) {
                    case 900090000:
                        data = "Effect/Direction4.img/promotion/Scene0" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 900090001:
                        data = "Effect/Direction4.img/promotion/Scene1";
                        break;
                    case 900090002:
                        data = "Effect/Direction4.img/promotion/Scene2" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 900090003:
                        data = "Effect/Direction4.img/promotion/Scene3";
                        break;
                    case 900090004:
                        c.announce(UIPacket.IntroDisableUI(false));
                        c.announce(UIPacket.IntroLock(false));
                        c.announce(MaplePacketCreator.enableActions());
                        final MapleMap mapto = c.getChannelServer().getMapFactory().getMap(900010000);
                        c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                        return;
                }
                showIntro(c, data);
                break;
            case mPark_stageEff:
                c.getPlayer().dropMessage(-1, "必须消灭掉地图上的所有怪物，才能移动到下一关。");
                switch ((c.getPlayer().getMapId() % 1000) / 100) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        c.announce(UIPacket.showMapEffect("monsterPark/stageEff/stage"));
                        c.announce(UIPacket.showMapEffect("monsterPark/stageEff/number/" + (((c.getPlayer().getMapId() % 1000) / 100) + 1)));
                        break;
                    case 4:
                        if (c.getPlayer().getMapId() / 1000000 == 952) {
                            c.announce(UIPacket.showMapEffect("monsterPark/stageEff/final"));
                        } else {
                            c.announce(UIPacket.showMapEffect("monsterPark/stageEff/stage"));
                            c.announce(UIPacket.showMapEffect("monsterPark/stageEff/number/5"));
                        }
                        break;
                    case 5:
                        c.announce(UIPacket.showMapEffect("monsterPark/stageEff/final"));
                        break;
                }
                break;
            case aswan_stageEff: {
                c.getPlayer().dropMessage(-1, "必须消灭掉地图上的所有怪物，才能移动到下一关。");
                switch ((c.getPlayer().getMapId() % 1000) / 100) {
                    case 1:
                    case 2:
                        c.announce(UIPacket.showMapEffect("aswan/stageEff/stage"));
                        c.announce(UIPacket.showMapEffect("aswan/stageEff/number/" + ((c.getPlayer().getMapId() % 1000) / 100)));
                        break;
                    case 3:
                        c.announce(UIPacket.showMapEffect("aswan/stageEff/final"));
                        break;
                }
                break;
            }
            case TD_MC_title: {
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                c.announce(MaplePacketCreator.enableActions());
                c.announce(UIPacket.showMapEffect("temaD/enter/mushCatle"));
                break;
            }
            case TD_NC_title: {
                switch ((c.getPlayer().getMapId() / 100) % 10) {
                    case 0:
                        c.announce(UIPacket.showMapEffect("temaD/enter/teraForest"));
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        c.announce(UIPacket.showMapEffect("temaD/enter/neoCity" + ((c.getPlayer().getMapId() / 100) % 10)));
                        break;
                }
                break;
            }
            case explorationPoint: {
                explorationPoint(c);
                break;
            }
            case go10000:
            case go1020000:
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                c.announce(MaplePacketCreator.enableActions());
            case go20000:
            case go30000:
            case go40000:
            case go50000:
            case go1000000:
            case go2000000:
            case go1010000:
            case go1010100:
            case go1010200:
            case go1010300:
            case go1010400: {
                c.announce(UIPacket.MapNameDisplay(c.getPlayer().getMapId()));
                break;
            }
            case TD_LC_title:
                c.announce(MaplePacketCreator.showEffect("temaD/enter/lionCastle"));
                break;
            case ds_tuto_ill0: {
                c.announce(UIPacket.getDirectionInfo(1, 6300));
                showIntro(c, "Effect/Direction6.img/DemonTutorial/SceneLogo");
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.IntroDisableUI(false));
                    c.announce(UIPacket.IntroLock(false));
                    c.announce(MaplePacketCreator.enableActions());
                    MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000000);
                    c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                }, 6300); //wtf
                break;
            }
            case ds_tuto_home_before: {
                c.announce(UIPacket.getDirectionInfo(3, 1));
                c.announce(UIPacket.getDirectionInfo(1, 30));
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.getDirectionInfo(3, 0));
                c.announce(UIPacket.getDirectionInfo(1, 90));

                c.announce(MaplePacketCreator.showEffect("demonSlayer/text11"));
                c.announce(UIPacket.getDirectionInfo(1, 4000));
                Timer.EventTimer.getInstance().schedule(() -> showIntro(c, "Effect/Direction6.img/DemonTutorial/Scene2"), 1000);
                break;
            }
            case ds_tuto_1_0: {
                c.announce(UIPacket.getDirectionInfo(3, 1));
                c.announce(UIPacket.getDirectionInfo(1, 30));
                c.announce(UIPacket.getDirectionStatus(true));

                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.getDirectionInfo(3, 0));
                    c.announce(UIPacket.getDirectionInfo(4, 2159310));
                    NPCScriptManager.getInstance().start(c, 2159310);
                }, 1000);
                break;
            }
            case ds_tuto_4_0: {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.getDirectionInfo(3, 0));
                c.announce(UIPacket.getDirectionInfo(4, 2159344));
                NPCScriptManager.getInstance().start(c, 2159344);
                break;
            }
            case cannon_tuto_01: {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionStatus(true));
                c.getPlayer().changeSingleSkillLevel(SkillFactory.getSkill(110), (byte) 1, (byte) 1); //海盗祝福 - [种族特性技能]强化火炮手特有的坚韧，永久提高各种属性。
                c.announce(UIPacket.getDirectionInfo(3, 0));
                c.announce(UIPacket.getDirectionInfo(4, 1096000));
                NPCScriptManager.getInstance().dispose(c);
                NPCScriptManager.getInstance().start(c, 1096000);
                break;
            }
            case ds_tuto_5_0: {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.getDirectionInfo(3, 0));
                c.announce(UIPacket.getDirectionInfo(4, 2159314));
                NPCScriptManager.getInstance().dispose(c);
                NPCScriptManager.getInstance().start(c, 2159314);
                break;
            }
            case ds_tuto_3_0: {
                c.announce(UIPacket.getDirectionInfo(3, 1));
                c.announce(UIPacket.getDirectionInfo(1, 30));
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(MaplePacketCreator.showEffect("demonSlayer/text12"));

                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.getDirectionInfo(3, 0));
                    c.announce(UIPacket.getDirectionInfo(4, 2159311));
                    NPCScriptManager.getInstance().dispose(c);
                    NPCScriptManager.getInstance().start(c, 2159311);
                }, 1000);
                break;
            }
            case ds_tuto_3_1: {
                c.announce(UIPacket.IntroDisableUI(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionStatus(true));
                if (!c.getPlayer().getMap().containsNPC(2159340)) {
                    c.getPlayer().getMap().spawnNpc(2159340, new Point(175, 0));
                    c.getPlayer().getMap().spawnNpc(2159341, new Point(300, 0));
                    c.getPlayer().getMap().spawnNpc(2159342, new Point(600, 0));
                }
                c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/tuto/balloonMsg2/0", 2000, 0, -100, 1));
                c.announce(UIPacket.getDirectionInfo("Effect/Direction5.img/effect/tuto/balloonMsg1/3", 2000, 0, -100, 1));
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.getDirectionInfo(3, 0));
                    c.announce(UIPacket.getDirectionInfo(4, 2159340));
                    NPCScriptManager.getInstance().dispose(c);
                    NPCScriptManager.getInstance().start(c, 2159340);
                }, 1000);
                break;
            }
            case ds_tuto_2_before: {
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.getDirectionInfo(3, 1));
                c.announce(UIPacket.getDirectionInfo(1, 30));
                c.announce(UIPacket.getDirectionStatus(true));
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.getDirectionInfo(3, 0));
                    c.announce(MaplePacketCreator.showEffect("demonSlayer/text13"));
                    c.announce(UIPacket.getDirectionInfo(1, 500));
                }, 1000);
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(MaplePacketCreator.showEffect("demonSlayer/text14"));
                    c.announce(UIPacket.getDirectionInfo(1, 4000));
                }, 1500);
                Timer.EventTimer.getInstance().schedule(() -> {
                    MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000020);
                    c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                    c.announce(UIPacket.IntroEnableUI(0));
                    MapleQuest.getInstance(23204).forceStart(c.getPlayer(), 0, null);
                    MapleQuest.getInstance(23205).forceComplete(c.getPlayer(), 0);
                    Map<Integer, SkillEntry> sDate = new HashMap<>();
                    sDate.put(30011170, new SkillEntry((byte) 1, (byte) 1, -1));
                    sDate.put(30011169, new SkillEntry((byte) 1, (byte) 1, -1));
                    sDate.put(30011168, new SkillEntry((byte) 1, (byte) 1, -1));
                    sDate.put(30011167, new SkillEntry((byte) 1, (byte) 1, -1));
                    sDate.put(30010166, new SkillEntry((byte) 1, (byte) 1, -1));
                    c.getPlayer().changeSkillsLevel(sDate);
                }, 5500);
                break;
            }
            case ds_tuto_1_before: {
                c.announce(UIPacket.getDirectionInfo(3, 1));
                c.announce(UIPacket.getDirectionInfo(1, 30));
                c.announce(UIPacket.getDirectionStatus(true));
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(UIPacket.getDirectionInfo(3, 0));
                    c.announce(MaplePacketCreator.showEffect("demonSlayer/text8"));
                    c.announce(UIPacket.getDirectionInfo(1, 500));
                }, 1000);
                Timer.EventTimer.getInstance().schedule(() -> {
                    c.announce(MaplePacketCreator.showEffect("demonSlayer/text9"));
                    c.announce(UIPacket.getDirectionInfo(1, 3000));
                }, 1500);
                Timer.EventTimer.getInstance().schedule(() -> {
                    MapleMap mapto = c.getChannelServer().getMapFactory().getMap(927000010);
                    c.getPlayer().changeMap(mapto, mapto.getPortal(0));
                }, 4500);
                break;
            }
            case ds_tuto_0_0: {
                c.announce(UIPacket.getDirectionStatus(true));
                c.announce(UIPacket.IntroEnableUI(1));
                c.announce(UIPacket.IntroDisableUI(true));
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                sDate.put(恶魔猎手.恶魔之翼, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(恶魔猎手.恶魔跳跃, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(恶魔猎手.死亡诅咒, new SkillEntry((byte) 1, (byte) 1, -1));
                sDate.put(恶魔猎手.恶魔之血, new SkillEntry((byte) 1, (byte) 1, -1));
                c.getPlayer().changeSkillsLevel(sDate);
                c.announce(UIPacket.getDirectionInfo(3, 0));
                c.announce(MaplePacketCreator.showEffect("demonSlayer/back"));
                c.announce(MaplePacketCreator.showEffect("demonSlayer/text0"));
                c.announce(UIPacket.getDirectionInfo(1, 500));
                c.getPlayer().setDirection(0);
                if (!c.getPlayer().getMap().containsNPC(2159307)) {
                    c.getPlayer().getMap().spawnNpc(2159307, new Point(1305, 50));
                }
                break;
            }
            case ds_tuto_2_prep: {
                if (!c.getPlayer().getMap().containsNPC(2159309)) {
                    c.getPlayer().getMap().spawnNpc(2159309, new Point(550, 50));
                }
                break;
            }
            case goArcher: {
                showIntro(c, "Effect/Direction3.img/archer/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goPirate: {
                showIntro(c, "Effect/Direction3.img/pirate/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goRogue: {
                showIntro(c, "Effect/Direction3.img/rogue/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goMagician: {
                showIntro(c, "Effect/Direction3.img/magician/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goSwordman: {
                showIntro(c, "Effect/Direction3.img/swordman/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case goLith: {
                showIntro(c, "Effect/Direction3.img/goLith/Scene" + (c.getPlayer().getGender() == 0 ? "0" : "1"));
                break;
            }
            case TD_MC_Openning: {
                showIntro(c, "Effect/Direction2.img/open");
                break;
            }
            case TD_MC_gasi: {
                showIntro(c, "Effect/Direction2.img/gasi");
                break;
            }
            case aranDirection: {
                switch (c.getPlayer().getMapId()) {
                    case 914090010:
                        data = "Effect/Direction1.img/aranTutorial/Scene0";
                        break;
                    case 914090011:
                        data = "Effect/Direction1.img/aranTutorial/Scene1" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090012:
                        data = "Effect/Direction1.img/aranTutorial/Scene2" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090013:
                        data = "Effect/Direction1.img/aranTutorial/Scene3";
                        break;
                    case 914090100:
                        data = "Effect/Direction1.img/aranTutorial/HandedPoleArm" + (c.getPlayer().getGender() == 0 ? "0" : "1");
                        break;
                    case 914090200:
                        data = "Effect/Direction1.img/aranTutorial/Maha";
                        break;
                }
                showIntro(c, data);
                break;
            }
            case iceCave: {
                Map<Integer, SkillEntry> sDate = new HashMap<>();
                sDate.put(战神.教程技能1, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(战神.教程技能2, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(战神.教程技能3, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(战神.教程技能4, new SkillEntry((byte) -1, (byte) 0, -1));
                sDate.put(战神.教程技能5, new SkillEntry((byte) -1, (byte) 0, -1));
                c.getPlayer().changeSkillsLevel(sDate);
                c.announce(EffectPacket.ShowWZEffect("Effect/Direction1.img/aranTutorial/ClickLirin"));
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                c.announce(MaplePacketCreator.enableActions());
                break;
            }
            case rienArrow: {
                if (c.getPlayer().getInfoQuest(21019).equals("miss=o;helper=clear")) {
                    c.getPlayer().updateInfoQuest(21019, "miss=o;arr=o;helper=clear");
                    c.announce(EffectPacket.AranTutInstructionalBalloon("Effect/OnUserEff.img/guideEffect/aranTutorial/tutorialArrow3"));
                }
                break;
            }
            case rien: {
                if (c.getPlayer().getQuestStatus(21101) == 2 && c.getPlayer().getInfoQuest(21019).equals("miss=o;arr=o;helper=clear")) {
                    c.getPlayer().updateInfoQuest(21019, "miss=o;arr=o;ck=1;helper=clear");
                }
                c.announce(UIPacket.IntroDisableUI(false));
                c.announce(UIPacket.IntroLock(false));
                break;
            }
            case check_count: {
                if (c.getPlayer().getMapId() == 950101010 && (!c.getPlayer().haveItem(4001433, 20) || c.getPlayer().getLevel() < 50)) { //ravana Map
                    MapleMap mapp = c.getChannelServer().getMapFactory().getMap(950101100); //exit Map
                    c.getPlayer().changeMap(mapp, mapp.getPortal(0));
                }
                break;
            }
            case Massacre_first: { //sends a whole bunch of shit.
                if (c.getPlayer().getPyramidSubway() == null) {
                    c.getPlayer().setPyramidSubway(new Event_PyramidSubway(c.getPlayer()));
                }
                break;
            }
            case Massacre_result: { //clear, give exp, etc.
                c.announce(MaplePacketCreator.showEffect("killing/fail"));
                break;
            }
            default: {
                log.info("地图触发: 未找到 " + scriptName + ", 类型: onUserEnter - 地图ID " + c.getPlayer().getMapId());
                NPCScriptManager.getInstance().onUserEnter(c, scriptName);
                break;
            }
        }
    }

    public static void explorationPoint(MapleClient c) {
        if (c.getPlayer().getMapId() == 104000000) {
            c.announce(UIPacket.IntroDisableUI(false));
            c.announce(UIPacket.IntroLock(false));
            c.announce(MaplePacketCreator.enableActions());
            c.announce(UIPacket.MapNameDisplay(c.getPlayer().getMapId()));
        }
        //c.getPlayer().dropMessage(-11, "当前地图信息: ID " + c.getPlayer().getMapId() + " 名字 " + c.getPlayer().getMap().getMapName());
        MedalQuest m = null;
        for (MedalQuest mq : MedalQuest.values()) {
            for (int i : mq.maps) {
                if (c.getPlayer().getMapId() == i) {
                    m = mq;
                    break;
                }
            }
        }
        if (m != null && c.getPlayer().getLevel() >= m.level && c.getPlayer().getQuestStatus(m.questid) != 2) {
            if (c.getPlayer().getQuestStatus(m.lquestid) != 1) {
                MapleQuest.getInstance(m.lquestid).forceStart(c.getPlayer(), 0, "0");
            }
            if (c.getPlayer().getQuestStatus(m.questid) != 1) {
                MapleQuest.getInstance(m.questid).forceStart(c.getPlayer(), 0, null);
                StringBuilder sb = new StringBuilder("enter=");
                for (int map : m.maps) {
                    sb.append("0");
                }
                c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, "0");
            }
            String quest = c.getPlayer().getInfoQuest(m.questid - 2005);
            if (quest.length() != m.maps.length + 6) { //enter= is 6
                StringBuilder sb = new StringBuilder("enter=");
                for (int map : m.maps) {
                    sb.append("0");
                }
                quest = sb.toString();
                c.getPlayer().updateInfoQuest(m.questid - 2005, quest);
            }
            MapleQuestStatus stat = c.getPlayer().getQuestNAdd(MapleQuest.getInstance(m.questid - 1995));
            if (stat.getCustomData() == null) { //just a check.
                stat.setCustomData("0");
            }
            int number = Integer.parseInt(stat.getCustomData());
            StringBuilder sb = new StringBuilder("enter=");
            boolean changedd = false;
            for (int i = 0; i < m.maps.length; i++) {
                boolean changed = false;
                if (c.getPlayer().getMapId() == m.maps[i]) {
                    if (quest.substring(i + 6, i + 7).equals("0")) {
                        sb.append("1");
                        changed = true;
                        changedd = true;
                    }
                }
                if (!changed) {
                    sb.append(quest.substring(i + 6, i + 7));
                }
            }
            if (changedd) {
                number++;
                c.getPlayer().updateInfoQuest(m.questid - 2005, sb.toString());
                MapleQuest.getInstance(m.questid - 1995).forceStart(c.getPlayer(), 0, String.valueOf(number));
                c.getPlayer().dropMessage(-1, "探险了 " + number + "/" + m.maps.length + " 个地区");
                c.getPlayer().dropMessage(-1, "正在挑战称号 - " + String.valueOf(m) + "");
                c.announce(MaplePacketCreator.showQuestMsg("正在挑战称号 - " + String.valueOf(m) + "。" + number + "/" + m.maps.length + " 完成"));
            }
        }
    }

    private static void reloadWitchTower(MapleClient c) {
        MapleMap map = c.getPlayer().getMap();
        map.killAllMonsters(false);
        int level = c.getPlayer().getLevel();
        int mob;
        if (level <= 10) {
            mob = 9300367; //魔女的玩具熊
        } else if (level <= 20) {
            mob = 9300368; //魔女的玩具熊
        } else if (level <= 30) {
            mob = 9300369; //魔女的玩具熊
        } else if (level <= 40) {
            mob = 9300370; //魔女的玩具熊
        } else if (level <= 50) {
            mob = 9300371; //魔女的玩具熊
        } else if (level <= 60) {
            mob = 9300372; //魔女的玩具熊
        } else if (level <= 70) {
            mob = 9300373; //魔女的玩具熊
        } else if (level <= 80) {
            mob = 9300374; //魔女的玩具熊
        } else if (level <= 90) {
            mob = 9300375; //魔女的玩具熊
        } else if (level <= 100) {
            mob = 9300376; //魔女的玩具熊
        } else {
            mob = 9300377; //魔女的玩具熊
        }
        MapleMonster theMob = MapleLifeFactory.getMonster(mob);
        OverrideMonsterStats oms = new OverrideMonsterStats();
        oms.setOMp(theMob.getMobMaxMp());
        oms.setOExp(theMob.getMobExp());
        oms.setOHp((long) Math.ceil(theMob.getMobMaxHp() * (level / 5.0))); //10k to 4m
        theMob.setOverrideStats(oms);
        map.spawnMonsterOnGroundBelow(theMob, witchTowerPos);
    }

    private static void showIntro(MapleClient c, String data) {
        c.announce(UIPacket.IntroDisableUI(true));
        c.announce(UIPacket.IntroLock(true));
        c.announce(EffectPacket.ShowWZEffect(data));
    }

    public void reloadWitchTower() {
        reloadWitchTower(getClient());
    }

    public void startMapEffect(MapleClient c, String data, int itemId) {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (!ii.itemExists(itemId)) {
            getPlayer().dropMessage(5, "地图效果触发 道具: " + itemId + " 不存在.");
            return;
        } else if (!ii.isFloatCashItem(itemId)) {
            getPlayer().dropMessage(5, "地图效果触发 道具: " + itemId + " 不具有漂浮公告的效果.");
            return;
        }
        getPlayer().getMap().startMapEffect(data, itemId);
    }

    public void sendMapNameDisplay(boolean enabled) {
        if (enabled) {
            getClient().announce(UIPacket.IntroDisableUI(false));
            getClient().announce(UIPacket.IntroLock(false));
        }
        getClient().announce(UIPacket.MapNameDisplay(getPlayer().getMapId()));
    }

    public void handlePinkBeanStart() {
        MapleMap map = getPlayer().getMap();
        map.resetFully();
        if (!map.containsNPC(2141000)) {
            map.spawnNpc(2141000, new Point(-190, -42));
        }
    }

    private enum onFirstUserEnter {

        dojang_Eff,
        dojang_Msg,
        PinkBeen_before,
        onRewordMap,
        StageMsg_together,
        StageMsg_crack,
        StageMsg_davy,
        StageMsg_goddess,
        party6weatherMsg,
        StageMsg_juliet,
        StageMsg_romio,
        moonrabbit_mapEnter,
        astaroth_summon,
        boss_Ravana,
        boss_Ravana_mirror,
        killing_BonusSetting,
        killing_MapSetting,
        metro_firstSetting,
        balog_bonusSetting,
        balog_summon,
        easy_balog_summon,
        Sky_TrapFEnter,
        shammos_Fenter,
        PRaid_D_Fenter,
        PRaid_B_Fenter,
        summon_pepeking,
        Xerxes_summon,
        VanLeon_Before,
        cygnus_Summon,
        storymap_scenario,
        shammos_FStart,
        kenta_mapEnter,
        iceman_FEnter,
        iceman_Boss,
        prisonBreak_mapEnter,
        Visitor_Cube_poison,
        Visitor_Cube_Hunting_Enter_First,
        VisitorCubePhase00_Start,
        visitorCube_addmobEnter,
        Visitor_Cube_PickAnswer_Enter_First_1,
        visitorCube_medicroom_Enter,
        visitorCube_iceyunna_Enter,
        Visitor_Cube_AreaCheck_Enter_First,
        visitorCube_boomboom_Enter,
        visitorCube_boomboom2_Enter,
        CubeBossbang_Enter,
        MalayBoss_Int,
        mPark_summonBoss,
        NULL;

        private static onFirstUserEnter fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    }

    private enum onUserEnter {

        babyPigMap,
        crash_Dragon,
        evanleaveD,
        getDragonEgg,
        meetWithDragon,
        go1010100,
        go1010200,
        go1010300,
        go1010400,
        evanPromotion,
        PromiseDragon,
        evanTogether,
        incubation_dragon,
        TD_MC_Openning,
        TD_MC_gasi,
        TD_MC_title,
        cygnusJobTutorial,
        cygnusTest,
        startEreb,
        dojang_Msg,
        dojang_1st,
        reundodraco,
        undomorphdarco,
        explorationPoint,
        goAdventure,
        go10000,
        go20000,
        go30000,
        go40000,
        go50000,
        go1000000,
        go1010000,
        go1020000,
        go2000000,
        goArcher,
        goPirate,
        goRogue,
        goMagician,
        goSwordman,
        goLith,
        iceCave,
        mirrorCave,
        aranDirection,
        rienArrow,
        rien,
        check_count,
        Massacre_first,
        Massacre_result,
        aranTutorAlone,
        evanAlone,
        dojang_QcheckSet,
        Sky_StageEnter,
        outCase,
        balog_buff,
        balog_dateSet,
        Sky_BossEnter,
        Sky_GateMapEnter,
        shammos_Enter,
        shammos_Result,
        shammos_Base,
        dollCave00,
        dollCave01,
        dollCave02,
        Sky_Quest,
        enterBlackfrog,
        onSDI,
        blackSDI,
        summonIceWall,
        metro_firstSetting,
        start_itemTake,
        findvioleta,
        pepeking_effect,
        TD_MC_keycheck,
        TD_MC_gasi2,
        in_secretroom,
        sealGarden,
        TD_NC_title,
        TD_neo_BossEnter,
        PRaid_D_Enter,
        PRaid_B_Enter,
        PRaid_Revive,
        PRaid_W_Enter,
        PRaid_WinEnter,
        PRaid_FailEnter,
        Resi_tutor10,
        Resi_tutor20,
        Resi_tutor30,
        Resi_tutor40,
        Resi_tutor50,
        Resi_tutor60,
        Resi_tutor70,
        Resi_tutor80,
        Resi_tutor50_1,
        summonSchiller,
        q31102e,
        q31103s,
        jail,
        VanLeon_ExpeditionEnter,
        cygnus_ExpeditionEnter,
        knights_Summon,
        TCMobrevive,
        mPark_stageEff,
        aswan_stageEff,
        moonrabbit_takeawayitem,
        StageMsg_crack,
        shammos_Start,
        iceman_Enter,
        prisonBreak_1stageEnter,
        VisitorleaveDirectionMode,
        visitorPT_Enter,
        VisitorCubePhase00_Enter,
        visitor_ReviveMap,
        cannon_tuto_01,
        cannon_tuto_direction,
        cannon_tuto_direction1,
        cannon_tuto_direction2,
        userInBattleSquare,
        merTutorDrecotion00,
        merTutorDrecotion10,
        merTutorDrecotion20,
        merStandAlone,
        merOutStandAlone,
        merTutorSleep00,
        merTutorSleep01,
        merTutorSleep02,
        EntereurelTW,
        ds_tuto_ill0,
        ds_tuto_0_0,
        ds_tuto_1_0,
        ds_tuto_3_0,
        ds_tuto_3_1,
        ds_tuto_4_0,
        ds_tuto_5_0,
        ds_tuto_2_prep,
        ds_tuto_1_before,
        ds_tuto_2_before,
        ds_tuto_home_before,
        ds_tuto_ani,
        TD_LC_title, //V.101新增
        NULL;

        private static onUserEnter fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    }

    private enum directionInfo {

        merTutorDrecotion01,
        merTutorDrecotion02,
        merTutorDrecotion03,
        merTutorDrecotion04,
        merTutorDrecotion05,
        merTutorDrecotion12,
        merTutorDrecotion21,
        ds_tuto_0_1,
        ds_tuto_0_2,
        ds_tuto_0_3,
        NULL;

        private static directionInfo fromString(String Str) {
            try {
                return valueOf(Str);
            } catch (IllegalArgumentException ex) {
                return NULL;
            }
        }
    }
}
