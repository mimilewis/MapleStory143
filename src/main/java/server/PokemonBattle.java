package server;

import client.Battler;
import client.MapleCharacter;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.BattleConstants;
import constants.BattleConstants.*;
import constants.GameConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.event.EventScriptManager;
import server.Timer.EtcTimer;
import server.life.*;
import server.maps.MapleMap;
import server.movement.AbsoluteLifeMovement;
import server.movement.LifeMovementFragment;
import server.movement.RelativeLifeMovement;
import server.quest.MapleQuest;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.StringUtil;
import tools.packet.MobPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class PokemonBattle {

    private static final Logger log = LogManager.getLogger(PokemonBattle.class.getName());
    private static final String[] messages = new String[]{"The ball broke free?!", "It appeared to be caught!", "No! It was so close, too!", "Argh! Almost had it!"};
    private static final int WALK_TIME = 2000; //how fast monster movement should be
    private static final String wildBGM = "BgmTW/NightField";
    private static final String npcBGM = "BgmTW/YoTaipei";
    private static final String trainerBGM = "BgmCN/PvP";
    private final int mapid;
    private MapleMap map;
    private int[] characterIds = new int[2];
    private Battler[] battlers = new Battler[2];
    private int instanceid = -1;
    private Turn[] turn = new Turn[2];
    private String[] attacks = new String[2];
    private boolean disposed = false;
    private Battler[] switches = new Battler[2];
    private List<Integer> npcTeam;

    public PokemonBattle(MapleCharacter init, int monsterId, PokemonMap mapp) { //wild battle
        for (Battler b : init.getBattlers()) {
            if (b != null) {
                b.resetStats();
            }
        }
        characterIds[0] = init.getId();
        characterIds[1] = 0;
        instanceid = EventScriptManager.getNewInstanceMapId();
        map = init.getClient().getChannelServer().getMapFactory().CreateInstanceMap(init.getMapId(), false, false, false, instanceid);
        mapid = init.getMapId();
        battlers[0] = init.getBattler(0);
        MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(monsterId);
        if (mons == null) {
            dispose(init, true);
            throw new RuntimeException("MONSTER NOT EXIST " + monsterId);
        }
        battlers[1] = new Battler(mons);
    }

    public PokemonBattle(MapleCharacter init, List<Integer> monsterId, int npcId, int maxLevel) { //npc battle
        for (Battler b : init.getBattlers()) {
            if (b != null) {
                b.resetStats();
                if (maxLevel > 0 && b.getLevel() > maxLevel) {
                    b.setTempLevel(maxLevel);
                    b.resetHP();
                }
            }
        }
        characterIds[0] = init.getId();
        characterIds[1] = 0;
        this.instanceid = EventScriptManager.getNewInstanceMapId();
        this.map = init.getClient().getChannelServer().getMapFactory().CreateInstanceMap(init.getMapId(), false, false, false, instanceid);
        mapid = init.getMapId();
        battlers[0] = init.getBattler(0);
        npcTeam = monsterId;
        MapleMonsterStats mons = MapleLifeFactory.getMonsterStats(monsterId.get(0));
        if (mons == null) {
            dispose(init, true);
            throw new RuntimeException("MONSTER NOT EXIST " + monsterId.get(0));
        }
        battlers[1] = new Battler(mons);
        npcTeam.remove(0);
        this.map.spawnNpc(npcId, BattleConstants.getNPCPos(init.getMapId()));

    }

    public PokemonBattle(MapleCharacter init, MapleCharacter init2) { //trainer
        map = init.getMap();
        mapid = init.getMapId();
        if (init2.getMap() != map) {
            throw new RuntimeException("INIT MAPS WERE NOT EQUAL: " + init.getMapId() + " vs " + init2.getMapId());
        }
        for (Battler b : init.getBattlers()) {
            if (b != null) {
                b.resetStats();
            }
        }
        for (Battler b : init2.getBattlers()) {
            if (b != null) {
                b.resetStats();
            }
        }
        characterIds[0] = init.getId();
        characterIds[1] = init2.getId();
        battlers[0] = init.getBattler(0);
        battlers[1] = init2.getBattler(0);
    }

    public int getInstanceId() {
        return instanceid;
    }

    public void giveReward(MapleCharacter chr) {
        if (chr.getMapId() == 925020011) { //normal 新手教学 - 武陵道场修炼场
            int rr = Randomizer.nextInt(100);
            if (rr < 10) {
                int reward = RandomRewards.getPokemonReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    chr.dropMessage(-6, "You have gained a pokemon item.");
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 45) {
                int reward = RandomRewards.getDropReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 70) {
                int reward = RandomRewards.getFishingReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 80) {
                int reward = RandomRewards.getSilverBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 85) {
                int reward = RandomRewards.getGoldBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 90) {
                int reward = RandomRewards.getPeanutReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on NORMAL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else {
                chr.dropMessage(-6, "The trainer ran away, ashamed at their defeat, unable to give a reward.");
            }
        } else if (chr.getMapId() == 925020012) { //hard 新手教学 - 武陵道场修炼场
            int rr = Randomizer.nextInt(100);
            if (rr < 20) {
                int reward = RandomRewards.getPokemonReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    chr.dropMessage(-6, "You have gained a pokemon item.");
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 40) {
                int reward = RandomRewards.getDropReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 60) {
                int reward = RandomRewards.getFishingReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 75) {
                int reward = RandomRewards.getSilverBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 85) {
                int reward = RandomRewards.getGoldBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 95) {
                int reward = RandomRewards.getPeanutReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HARD on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else {
                chr.dropMessage(-6, "The trainer ran away, ashamed at their defeat, unable to give a reward.");
            }
        } else if (chr.getMapId() == 925020013) { //hell 新手教学 - 武陵道场修炼场
            int rr = Randomizer.nextInt(100);
            if (rr < 40) {
                int reward = RandomRewards.getPokemonReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    chr.dropMessage(-6, "You have gained an evolution item.");
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HELL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 50) {
                int reward = RandomRewards.getDropReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HELL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 70) {
                int reward = RandomRewards.getSilverBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HELL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else if (rr < 85) {
                int reward = RandomRewards.getGoldBoxReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HELL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            } else {
                int reward = RandomRewards.getPeanutReward();
                if (MapleInventoryManipulator.checkSpace(chr.getClient(), reward, 1, "")) {
                    MapleInventoryManipulator.addById(chr.getClient(), reward, (short) 1, "Pokemon prize on HELL on " + DateUtil.getCurrentDate());
                } else {
                    giveReward(chr); //do again until they get
                }
            }
        }
    }

    public void dispose() {
        if (map != null) {
            for (Battler battler : battlers) {
                if (battler != null && battler.getMonster() != null) {
                    map.killMonster(battler.getMonster());
                    battler.resetStats();
                }
            }
        }
        map = null;
        battlers = new Battler[2];
        turn = new Turn[2];
        attacks = new String[2];
        switches = new Battler[2];
        characterIds = new int[2];
        disposed = true;
        if (npcTeam != null) {
            npcTeam.clear();
            npcTeam = null;
        }
    }

    public void dispose(MapleCharacter chr, boolean dc) {
        boolean npc = npcTeam != null;
        dispose();
        chr.setBattle(null);
        for (Battler b : chr.getBattlers()) {
            if (b != null) {
                b.resetStats();
            }
        }
        if (instanceid >= 0) {
            if ((!dc || npc) && chr.getMapId() == mapid) {
                MapleMap mapz = chr.getClient().getChannelServer().getMapFactory().getMap(mapid);
                chr.changeMap(mapz, mapz.findClosestPortal(chr.getTruePosition()));
            }
            chr.getClient().getChannelServer().getMapFactory().removeInstanceMap(instanceid);
        } else if (chr.getMapId() == mapid && !dc) {
            chr.fakeRelog();
        }
    }

    public MapleMap getMap() {
        return map;
    }

    public Battler getBattler(int index) {
        return battlers[index];
    }

    public void updateHP(int i) {
        if (battlers[i] != null && battlers[i].getMonster() != null && characterIds[i == 0 ? 1 : 0] > 0) {
            MapleCharacter chrr = map.getCharacterById(characterIds[i == 0 ? 1 : 0]);
            if (chrr != null) {
                chrr.getClient().announce(MobPacket.showBossHP(9300184, battlers[i].getCurrentHP(), battlers[i].calcHP()));
            }
        }
    }

    public MapleMonster spawnMonster(Point pos, boolean facingLeft, boolean animation, Battler b) {
        MapleMonster mons = MapleLifeFactory.getMonster(b.getMonsterId());
        if (mons == null) {
            return mons;
        }
        mons.setFake(true);
        mons.setLinkCID(b.getCharacterId() <= 0 ? 1 : b.getCharacterId());
        if (facingLeft) {
            mons.setStance(4); //wild battle face right
        }
        map.spawnMonster_Pokemon(mons, pos, animation ? 15 : -2); //dojo "ball" animation, or newSpawn
        b.setMonster(mons);
        for (int i = 0; i < characterIds.length; i++) {
            if (characterIds[i] == b.getCharacterId()) {
                updateHP(i);
                break;
            }
        }
        return mons;
    }

    public void initiate(MapleCharacter initiator, PokemonMap mapp) { //wild battle
        spawnMonster(mapp.pos1, mapp.facingLeft, false, battlers[1]);
        initiator.dropMessage(-6, "Wild " + battlers[1].getName() + " has appeared! (Level " + battlers[1].getLevel() + ")");
        initiator.dropMessage(-3, "Go, " + battlers[0].getName() + "! (Level " + battlers[0].getLevel() + ")");
        spawnMonster(mapp.pos0, !mapp.facingLeft, true, battlers[0]);
        initiator.dropMessage(-6, "What will you do? @use @info @ball @run @battlehelp");
        turn[1] = makeRandomTurn();
        initiator.getClient().announce(MaplePacketCreator.musicChange(wildBGM));
        initiator.getMonsterBook().monsterSeen(initiator.getClient(), battlers[1].getMonsterId(), battlers[1].getOriginalName());
        firstTurn(battlers[0], battlers[1]);
    }

    public void initiate(MapleCharacter initiator, MapleNPC npc) { //wild battle
        Point ourPos = BattleConstants.getNPCPos(initiator.getMapId());
        spawnMonster(new Point(ourPos.x - 100, ourPos.y), false, true, battlers[1]);
        initiator.dropMessage(-6, "Trainer " + npc.getName() + " wants to battle!");
        initiator.dropMessage(-6, "Go, " + battlers[1].getName() + "! (Level " + battlers[1].getLevel() + ")");
        initiator.dropMessage(-3, "Go, " + battlers[0].getName() + "! (Level " + battlers[0].getLevel() + ")");
        spawnMonster(new Point(ourPos.x - 400, ourPos.y), true, true, battlers[0]);
        initiator.dropMessage(-6, "What will you do? @use @info @ball @run @battlehelp");
        turn[1] = makeRandomTurn();
        initiator.getClient().announce(MaplePacketCreator.musicChange(npcBGM));
        firstTurn(battlers[0], battlers[1]);
    }

    public void initiate() {
        MapleCharacter initiator = map.getCharacterById(battlers[0].getCharacterId());
        MapleCharacter initiator2 = map.getCharacterById(battlers[1].getCharacterId());
        initiator.dropMessage(-6, initiator2.getName() + " wants to battle!");
        initiator2.dropMessage(-6, initiator.getName() + " wants to battle!");
        map.broadcastMessage(MaplePacketCreator.getChatText(initiator.getId(), "Go, " + battlers[0].getName() + "! (Level " + battlers[0].getLevel() + ")", initiator.isGM(), 0));
        map.broadcastMessage(MaplePacketCreator.getChatText(initiator2.getId(), "Go, " + battlers[1].getName() + "! (Level " + battlers[1].getLevel() + ")", initiator2.isGM(), 0));
        spawnMonster(new Point(initiator.getTruePosition().x + (initiator.getTruePosition().x > initiator2.getTruePosition().x ? -100 : 100), initiator.getTruePosition().y), initiator.getTruePosition().x < initiator2.getTruePosition().x, true, battlers[0]);
        spawnMonster(new Point(initiator2.getTruePosition().x + (initiator2.getTruePosition().x > initiator.getTruePosition().x ? -100 : 100), initiator2.getTruePosition().y), initiator2.getTruePosition().x < initiator.getTruePosition().x, true, battlers[1]);
        initiator.dropMessage(-6, "What will you do? @use @info @ball @run @battlehelp");
        initiator2.dropMessage(-6, "What will you do? @use @info @ball @run @battlehelp");
        initiator.getClient().announce(MaplePacketCreator.musicChange(trainerBGM));
        initiator2.getClient().announce(MaplePacketCreator.musicChange(trainerBGM));
        firstTurn(battlers[0], battlers[1]);
        firstTurn(battlers[1], battlers[0]);
    }

    public void forfeit(MapleCharacter forfeiter, boolean dc) {
        if (!dc && turn[forfeiter.getId() == characterIds[0] ? 0 : 1] != null && turn[forfeiter.getId() == characterIds[0] ? 0 : 1] != Turn.TRUANT) {
            forfeiter.dropMessage(-6, "You've already selected an action.");
            return;
        }
        if (instanceid >= 0) { //wild
            if (!dc && battlers[0] != null && battlers[1] != null && battlers[1].getLevel() > battlers[0].getLevel() && battlers[1].getLevel() > 1 && Randomizer.nextInt(battlers[1].getLevel() - battlers[0].getLevel()) / 10 != 0 && battlers[0].getAbility() != PokemonAbility.RunAway) {
                forfeiter.dropMessage(-6, "Couldn't get away!");
                turn[1] = makeRandomTurn();
                makeTurn();
                return;
            }
            dispose(forfeiter, dc);
        } else {
            MapleMap theMap = map;
            if (theMap == null) {
                theMap = forfeiter.getMap();
                if (theMap == null) {
                    dispose(forfeiter, dc);
                    return;
                }
            }
            theMap.broadcastMessage(MaplePacketCreator.serverNotice(6, forfeiter.getName() + " has left the match."));
            if (!disposed) {
                dispose(forfeiter.getId() == characterIds[0] ? theMap.getCharacterById(characterIds[1]) : theMap.getCharacterById(characterIds[0]), dc);
            }
            dispose(forfeiter, dc);
        }
    }

    public boolean attack(MapleCharacter chr, String name) {
        if (battlers[0] == null || battlers[1] == null || (turn[chr.getId() == characterIds[0] ? 0 : 1] != null && (turn[chr.getId() == characterIds[0] ? 0 : 1] != Turn.TRUANT || attacks[chr.getId() == characterIds[0] ? 0 : 1] != null))) {
            return false;
        }
        attacks[chr.getId() == characterIds[0] ? 0 : 1] = name;
        if (turn[chr.getId() == characterIds[0] ? 0 : 1] == null) {
            turn[chr.getId() == characterIds[0] ? 0 : 1] = Turn.ATTACK;
        }
        if (turn[chr.getId() == characterIds[0] ? 1 : 0] != null) {
            makeTurn();
        }
        return true;
    }

    public boolean switchBattler(MapleCharacter chr, Battler b) {
        try {
            if (b.getCurrentHP() <= 0 || (turn[chr.getId() == characterIds[0] ? 0 : 1] != null)) {
                return false;
            }
            if (b == battlers[chr.getId() == characterIds[0] ? 0 : 1] || battlers[chr.getId() == characterIds[0] ? 1 : 0] == null) {
                return false;
            }
            if (battlers[chr.getId() == characterIds[0] ? 1 : 0] != null && battlers[chr.getId() == characterIds[0] ? 1 : 0].getAbility() == PokemonAbility.ShadowTag) {
                return false;
            }
            turn[chr.getId() == characterIds[0] ? 0 : 1] = Turn.SWITCH;
            switches[chr.getId() == characterIds[0] ? 0 : 1] = b;
            if (turn[chr.getId() == characterIds[0] ? 1 : 0] != null) {
                makeTurn();
            }
            return true;
        } catch (NullPointerException e) {
            log.error(e);
            return false;
        }
    }

    public boolean useBall(final MapleCharacter user, PokemonItem itemId) {
        if (battlers[1] == null || user == null) {
            return false;
        }
        boolean toBox = false;
        int highestLevel = 0;
        for (Battler b : user.getBattlers()) {
            if (b != null && b.getLevel() > highestLevel) {
                highestLevel = b.getLevel();
            }
            if (b != null && b.getMonsterId() == battlers[1].getMonsterId()) {
                toBox = true;
            }
        }
        if (turn[1] == Turn.DISABLED || (turn[0] != null && turn[0] != Turn.TRUANT) || battlers[0] == null || battlers[1] == null || (highestLevel + 5) < battlers[1].getLevel() || npcTeam != null || disposed) {
            return false;
        }
        if (turn[0] == Turn.TRUANT) {
            turn[0] = null;
        }
        user.dropMessage(-3, "Go, " + StringUtil.makeEnumHumanReadable(itemId.name()) + "!");
        catchEffect(battlers[1].getMonster());
        final String mName = battlers[1].getOriginalName();
        final long rand = Math.round(battlers[1].canCatch(itemId.catchChance));
        for (int i = 0; i < messages.length; i++) {
            if (Randomizer.nextInt(256) <= rand) {
                if (i != (messages.length - 1)) {
                    EtcTimer.getInstance().schedule(() -> {
                        if (!disposed) {
                            user.getClient().announce(MaplePacketCreator.playSound("Cokeplay/Fall"));
                            catchEffect(battlers[1].getMonster());
                        } else {
                            dispose();
                        }
                    }, 2000 * i + 2000);
                } else {
                    turn[1] = Turn.DISABLED;
                    final boolean toBo = toBox || user.countBattlers() >= 6;
                    EtcTimer.getInstance().schedule(() -> {
                        if (!disposed) {
                            map.killMonster(battlers[1].getMonster());
                            user.getClient().announce(MaplePacketCreator.playSound("Romio/discovery"));
                            user.dropMessage(-6, "Monster " + battlers[1].getName() + " has been successfully caught!");
                            caughtMonster(user, mName, toBo);
                        } else {
                            dispose();
                        }
                    }, 2000 * i + 2000);
                    return true;
                }
            } else {
                turn[1] = Turn.DISABLED;
                final String msgg = messages[i];
                EtcTimer.getInstance().schedule(() -> {
                    if (!disposed) {
                        user.dropMessage(-3, msgg);
                        turn[1] = makeRandomTurn();
                        makeTurn();
                    } else {
                        dispose();
                    }
                }, 2000 * i + 2500); //a bit more time
                return true;
            }
        }
        return true;
    }

    public void catchEffect(MapleMonster mons) {
        map.broadcastMessage(MobPacket.killMonster(mons.getObjectId(), 0));
        map.broadcastMessage(MobPacket.makeMonsterEffect(mons, 15));
    }

    public Turn makeRandomTurn() {
        if (battlers[1] == null || battlers[0] == null) {
            return Turn.DISABLED;
        } else if (npcTeam == null && battlers[1].getLevel() < battlers[0].getLevel() && battlers[1].getLevel() >= 1 && Randomizer.nextInt(battlers[0].getLevel() - battlers[1].getLevel()) > 10 && Randomizer.nextInt(50) >= 40) {
            return Turn.SWITCH;
        }
        return Turn.ATTACK;
    }

    public void firstTurn(Battler ours, Battler theirs) {
        if (ours.getAbility() == PokemonAbility.Forewarn) {
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, theirs.getName() + "'s type is " + theirs.getElementString() + "!"));
        } else if (ours.getAbility() == PokemonAbility.Frisk) {
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, theirs.getName() + "'s item is " + theirs.getItemString() + "!"));
        } else if (ours.getAbility() == PokemonAbility.Intimidate) {
            theirs.setMod(PokemonStat.ATK, theirs.decreaseMod(theirs.getMod(PokemonStat.ATK)));
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, ours.getName() + "'s Intimidate scared the opponent!"));
        }
    }

    public void caughtMonster(MapleCharacter user, String mName, boolean toBox) {
        if (toBox) {
            user.getBoxed().add(battlers[1]);
        } else {
            user.getBattlers()[user.countBattlers()] = battlers[1];
        }
        battlers[1].setCharacterId(user.getId());
        user.changedBattler();
        user.getMonsterBook().monsterCaught(user.getClient(), battlers[1].getMonsterId(), mName);
        dispose(user, false);
    }

    public void makeTurn() {
        final int theFirst;
        int[] order;
        if (turn[0] != Turn.SWITCH && (turn[1] == Turn.SWITCH || (battlers[1] != null && battlers[0] != null && ((battlers[1].getItem() != null && battlers[1].getItem() == HoldItem.Orange_Star && Randomizer.nextBoolean()) || battlers[1].getSpeed() > battlers[0].getSpeed()) && !(battlers[0].getItem() != null && battlers[0].getItem() == HoldItem.Orange_Star && Randomizer.nextBoolean())))) {
            order = new int[]{1, 0};
            theFirst = 1;
        } else {
            order = new int[]{0, 1};
            theFirst = 0;
        }
        int timeFirst = WALK_TIME / 8;
        for (final int i : order) {
            if (turn[i] != null && battlers[i == 1 ? 0 : 1] != null && turn[i] != Turn.DISABLED) {
                if (npcTeam == null && i == 1 && turn[i] == Turn.SWITCH && instanceid >= 0) { //wild battle running, special exception
                    MapleCharacter chr = map.getCharacterById(characterIds[0]);
                    chr.dropMessage(-6, "The wild monster fled.");
                    dispose(chr, false);
                    return;
                }
                if (battlers[i] != null) {
                    battlers[i].addMonsterId(battlers[i == 1 ? 0 : 1].getMonsterId());
                }
                EtcTimer.getInstance().schedule(() -> {
                    if (map == null || turn[i] == null || disposed || battlers[i == 1 ? 0 : 1] == null) {
                        return;
                    }
                    switch (turn[i]) {
                        case TRUANT:
                            if (battlers[i] != null) {
                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was loafing around!"));
                            }
                            break;
                        case SWITCH:
                            if (battlers[i == 1 ? 0 : 1].getMonster() == null) {
                                map.broadcastMessage(MaplePacketCreator.serverNotice(5, "The move could not be made due to an error."));
                                return;
                            }
                            boolean left = battlers[i == 1 ? 0 : 1].getMonster().isFacingLeft();
                            Point pos = new Point(battlers[i == 1 ? 0 : 1].getMonster().getTruePosition().x + (left ? -300 : 300), battlers[i == 1 ? 0 : 1].getMonster().getTruePosition().y - 20);
                            if (battlers[i] != null) {
                                if (battlers[i].getMonster() == null) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(5, "The move could not be made due to an error."));
                                    return;
                                }
                                map.broadcastMessage(MaplePacketCreator.getChatText(characterIds[i], "Return, " + battlers[i].getName() + "!", false, 0));
                                pos = battlers[i].getMonster().getPosition();
                                left = !battlers[i].getMonster().isFacingLeft();
                                map.killMonster(battlers[i].getMonster());
                                battlers[i].wipe();
                                if (battlers[i].getAbility() == PokemonAbility.NaturalCure) {
                                    battlers[i].wipeStatus();
                                } else if (battlers[i].getAbility() == PokemonAbility.Regenerator) {
                                    battlers[i].damage((int) -(battlers[i].calcHP() / 3), null, 0, true);
                                }
                            }
                            battlers[i] = switches[i];
                            if (battlers[i] == null) {
                                map.broadcastMessage(MaplePacketCreator.serverNotice(5, "The move could not be made due to an error."));
                                return;
                            }
                            if (i == 1 && npcTeam != null) {
                                map.broadcastMessage(MaplePacketCreator.serverNotice(5, "Go, " + battlers[i].getName() + "! (Level " + battlers[i].getLevel() + ")"));
                            } else {
                                map.broadcastMessage(MaplePacketCreator.getChatText(characterIds[i], "Go, " + battlers[i].getName() + "! (Level " + battlers[i].getLevel() + ")", false, 0));
                            }
                            spawnMonster(pos, left, true, battlers[i]);
                            firstTurn(battlers[i], battlers[i == 0 ? 1 : 0]);
                            break;
                        case ATTACK:
                            //AttackPower = (level + rand(level)) here
                            //element
                            if (battlers[i] == null || battlers[i].getMonster() == null) {
                                map.broadcastMessage(MaplePacketCreator.serverNotice(5, "The move could not be made due to an error."));
                                return;
                            }
                            if (attacks[i] != null) {
                                map.broadcastMessage(MaplePacketCreator.getChatText(characterIds[i], battlers[i].getName() + ", use " + attacks[i] + "!", false, 0));
                            }
                            battlers[i].decreaseStatusTurns();
                            boolean st = false;
                            if (battlers[i].getAbility() == PokemonAbility.ShedSkin && Randomizer.nextBoolean()) {
                                battlers[i].decreaseStatusTurns();
                            }
                            final MonsterStatus s;
                            if (battlers[i].getCurrentStatus() != null) {
                                if (battlers[i].getAbility() == PokemonAbility.Guts) {
                                    battlers[i].setMod(PokemonStat.ATK, battlers[i].decreaseMod(battlers[i].getMod(PokemonStat.ATK)));
                                } else if (battlers[i].getAbility() == PokemonAbility.MarvelScale) {
                                    battlers[i].setMod(PokemonStat.DEF, battlers[i].decreaseMod(battlers[i].getMod(PokemonStat.DEF)));
                                } else if (battlers[i].getAbility() == PokemonAbility.QuickFeet) {
                                    battlers[i].setMod(PokemonStat.SPEED, battlers[i].decreaseMod(battlers[i].getMod(PokemonStat.SPEED)));
                                }
                                s = battlers[i].getCurrentStatus().getStati();
                                switch (s) {
                                    case MOB_STAT_Stun:
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " is stunned and cannot move!"));
                                        st = true;
                                        break;
                                    case MOB_STAT_Freeze:
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " is frozen and cannot move!"));
                                        st = true;
                                        break;
                                    case MOB_STAT_Weakness:
                                        battlers[i].setMod(PokemonStat.SPATK, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPATK)));
                                        if (battlers[i].getAbility() == PokemonAbility.MagicGuard) {
                                            break;
                                        }
                                        battlers[i].damage((int) battlers[i].calcHP() / 16, map, battlers[i == 0 ? 1 : 0] == null ? 0 : battlers[i == 0 ? 1 : 0].getMonsterId(), true);
                                        updateHP(i);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 16) + " damage by imprint!"));
                                        break;
                                    case MOB_STAT_Blind:
                                        if (battlers[i].getAbility() == PokemonAbility.EarlyBird) {
                                            battlers[i].decreaseStatusTurns();
                                            break;
                                        }
                                        if (battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.BadDreams) {
                                            break;
                                        }
                                        if (battlers[i].getAbility() == PokemonAbility.MagicGuard) {
                                            break;
                                        }
                                        battlers[i].damage((int) battlers[i].calcHP() / 16, map, battlers[i == 0 ? 1 : 0] == null ? 0 : battlers[i == 0 ? 1 : 0].getMonsterId(), true);
                                        updateHP(i);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 16) + " damage by the opponent's Bad Dreams!"));
                                        break;
                                    case MOB_STAT_Poison:
                                        if (battlers[i].getAbility() == PokemonAbility.MagicGuard) {
                                            break;
                                        }
                                        battlers[i].damage((int) (battlers[i].getAbility() == PokemonAbility.PoisonHeal ? -(battlers[i].calcHP() / 16) : (battlers[i].calcHP() / 16)), map, battlers[i == 0 ? 1 : 0] == null ? 0 : battlers[i == 0 ? 1 : 0].getMonsterId(), true);
                                        updateHP(i);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 16) + " damage by poison!"));
                                        break;
                                    case MOB_STAT_Web:
                                        if (Randomizer.nextInt(4) == 0) {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was paralyzed and could not move!"));
                                            st = true;
                                        }
                                        //fallthrough is intended
                                    case MOB_STAT_Speed:
                                        battlers[i].setMod(PokemonStat.SPEED, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPEED)));
                                        break;
                                    case MOB_STAT_Darkness:
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " is angry and confused!"));
                                        if (Randomizer.nextBoolean() && battlers[i].getAbility() != PokemonAbility.MagicGuard) {
                                            battlers[i].damage((int) battlers[i].calcHP() / 16, map, battlers[i == 0 ? 1 : 0] == null ? 0 : battlers[i == 0 ? 1 : 0].getMonsterId(), true);
                                            updateHP(i);
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 16) + " damage in its rage!"));
                                            st = true;
                                        }
                                        battlers[i].setMod(PokemonStat.ATK, battlers[i].decreaseMod(battlers[i].getMod(PokemonStat.ATK)));
                                        battlers[i].setMod(PokemonStat.SPATK, battlers[i].decreaseMod(battlers[i].getMod(PokemonStat.SPATK)));
                                        battlers[i].setMod(PokemonStat.DEF, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.DEF)));
                                        battlers[i].setMod(PokemonStat.SPDEF, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPDEF)));
                                        break;
                                }
                            } else {
                                s = null;
                            }
                            if (st) {
                                break;
                            }
                            final boolean extraEffects = battlers[i].getAbility() != PokemonAbility.SheerForce && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.ShieldDust && s != MonsterStatus.MOB_STAT_MagicCrash;
                            int critChance = 20;
                            if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Green_Star) {
                                critChance /= 2;
                            }
                            if (battlers[i].getAbility() == PokemonAbility.SuperLuck || battlers[i].getAbility() == PokemonAbility.SereneGrace) {
                                critChance /= 2;
                            }
                            final Point startPos = battlers[i].getMonster().getPosition();
                            Point maskedPos = null;
                            final int origStance = battlers[i].getMonster().getStance();
                            final int atk = Randomizer.nextInt(battlers[i].getMonster().getStats().getMobAttacks().size() + 1);
                            final MobAttackInfo theAtk = (atk > 0 ? battlers[i].getMonster().getStats().getMobAttack(atk - 1) : null);
                            final boolean speshul = theAtk != null && (theAtk.magic || theAtk.MADamage > theAtk.PADamage);
                            final PokemonElement element = (speshul || Randomizer.nextInt(5) == 0) && s != MonsterStatus.MOB_STAT_Seal && battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Normalize && battlers[i].getElementSize() > 0 ? battlers[i].getElements()[(battlers[i].getElementSize() == 1 ? 0 : Randomizer.nextInt(battlers[i].getElementSize()))] : PokemonElement.None;
                            final boolean stab = element != PokemonElement.None,
                                    critical = Randomizer.nextInt(critChance) == 0 && extraEffects,
                                    flinch = Randomizer.nextInt(battlers[i].getAbility() == PokemonAbility.SereneGrace ? 25 : 50) == 0 && extraEffects;
                            PokemonStat decc = null,
                                    incc = null;
                            MonsterStatus statt = null;
                            final int levelDec = Randomizer.nextInt(10) == 0 ? 2 : 1,
                                    levelInc = Randomizer.nextInt(10) == 0 ? 2 : 1;
                            long base = battlers[i].getLevel() + 1; //dmg reduction like actual ms, but not dmg increase

                            int incDivisor = 1;
                            if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Herb_Pouch) {
                                incDivisor *= 2;
                            }
                            if (battlers[i].getAbility() == PokemonAbility.SereneGrace) {
                                incDivisor *= 2;
                            }
                            if (extraEffects && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.ClearBody && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.BigPecks && Randomizer.nextInt(20 / incDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                decc = (stab || speshul ? PokemonStat.SPDEF : PokemonStat.DEF);
                            } else if (extraEffects && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.HyperCutter && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.ClearBody && Randomizer.nextInt(20 / incDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                decc = (stab || speshul ? PokemonStat.SPATK : PokemonStat.ATK);
                            } else if (extraEffects && battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.ClearBody && Randomizer.nextInt(50 / incDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                decc = (stab || speshul ? PokemonStat.EVA : PokemonStat.SPEED);
                            }
                            int decDivisor = 1;
                            if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Ripped_Note) {
                                decDivisor *= 2;
                            }
                            if (battlers[i].getAbility() == PokemonAbility.SereneGrace) {
                                decDivisor *= 2;
                            }
                            if (extraEffects && Randomizer.nextInt(20 / decDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                incc = (stab || speshul ? PokemonStat.SPDEF : PokemonStat.DEF);
                            } else if (extraEffects && Randomizer.nextInt(20 / decDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                incc = (stab || speshul ? PokemonStat.SPATK : PokemonStat.ATK);
                            } else if (extraEffects && Randomizer.nextInt(50 / decDivisor) == 0) { //inflict stat change, decrease base
                                base = (base * 9) / 10;
                                incc = (stab || speshul ? PokemonStat.ACC : PokemonStat.SPEED);
                            }
                            double eez = element.getEffectiveness(battlers[i == 1 ? 0 : 1].getElements());
                            final double ee = (eez <= 0.0 && battlers[i].getAbility() == PokemonAbility.Scrappy ? 1.0 : eez);
                            int statDivisor = 1;
                            if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.King_Star) {
                                statDivisor *= 2;
                            }
                            if (battlers[i].getAbility() == PokemonAbility.SereneGrace) {
                                statDivisor *= 2;
                            }
                            if (extraEffects && Randomizer.nextInt(33 / statDivisor) == 0 && ee >= 1.0) {
                                switch (element) {
                                    case None:
                                    case Normal:
                                    case Immortal:
                                        statt = MonsterStatus.MOB_STAT_Stun;
                                        break;
                                    case Enchanted:
                                        statt = MonsterStatus.MOB_STAT_Weakness;
                                        break;
                                    case Reptile:
                                    case Mammal:
                                        statt = MonsterStatus.MOB_STAT_Seal;
                                        break;
                                    case Ice:
                                        statt = MonsterStatus.MOB_STAT_Freeze;
                                        break;
                                    case Lightning:
                                        statt = MonsterStatus.MOB_STAT_Web;
                                        break;
                                    case Fish:
                                        statt = MonsterStatus.MOB_STAT_Speed;
                                        break;
                                    case Plant:
                                    case Poison:
                                        statt = MonsterStatus.MOB_STAT_Poison;
                                        break;
                                    case Spirit:
                                        statt = MonsterStatus.MOB_STAT_Showdown;
                                        break;
                                    case Holy:
                                        statt = MonsterStatus.MOB_STAT_MagicCrash;
                                        break;
                                    case Dark:
                                        statt = MonsterStatus.MOB_STAT_Blind;
                                        break;
                                }
                            }
                            if (extraEffects && statt == null && theAtk != null && theAtk.getDiseaseSkill() > 0 && Randomizer.nextInt(10 / statDivisor) == 0) {
                                statt = MonsterStatus.getBySkill_Pokemon(theAtk.getDiseaseSkill());
                            }
                            if (statt == null && battlers[i].getAbility() == PokemonAbility.PoisonTouch && Randomizer.nextInt(5) == 0) {
                                statt = MonsterStatus.MOB_STAT_Poison;
                            }
                            final PokemonStat dec = decc;
                            final PokemonStat inc = incc;
                            final MonsterStatus stat = statt;
                            final long basedamagee;
                            if (ee > 0.0) {
                                double stabModifier = battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Medal ? 0.3 : 0.0;
                                if (battlers[i].getAbility() == PokemonAbility.Adaptability) {
                                    stabModifier += 0.3;
                                } else if (battlers[i].getAbility() == PokemonAbility.Blaze && battlers[i].getHPPercent() <= 33 && element == PokemonElement.Fire) {
                                    stabModifier += 0.5;
                                } else if (battlers[i].getAbility() == PokemonAbility.Torrent && battlers[i].getHPPercent() <= 33 && element == PokemonElement.Fish) {
                                    stabModifier += 0.5;
                                } else if (battlers[i].getAbility() == PokemonAbility.Overgrow && battlers[i].getHPPercent() <= 33 && element == PokemonElement.Plant) {
                                    stabModifier += 0.5;
                                }
                                if (battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.Filter) {
                                    stabModifier -= 0.5;
                                }
                                basedamagee = (long) Math.max(1, Math.ceil(((battlers[i].getAbility() == PokemonAbility.Analytic && theFirst != i ? 1.0 : 0.75) + (battlers[i].getLevel() / 200.0)) * (!element.special ? battlers[i].getATK(atk) : battlers[i].getSpATK(atk)) * (base / 5.0) * (100.0 - (!element.special ? battlers[i == 1 ? 0 : 1].getDEF() : battlers[i == 1 ? 0 : 1].getSpDEF())) / 100.0 + 2) * (stab ? (stabModifier + 3.0) : 2.0) * Math.round(ee * 2.0 * (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Other_World_Key && ee >= 2.0 ? 1.1 : 1.0)) * (35.0 + Randomizer.nextInt(15)) / 200.0);
                            } else {
                                basedamagee = 1;
                            }
                            final byte skillByte = (byte) (atk <= 0 ? -1 : (26 + (atk - 1) * 2 + battlers[i].getMonster().getFacingDirection()));
                            final List<LifeMovementFragment> moves = new ArrayList<>();
                            if (theAtk == null) { //simple walk to monster -> atk -> walk back
                                AbsoluteLifeMovement alm = new AbsoluteLifeMovement(0, battlers[i == 0 ? 1 : 0].getMonster().getPosition(), WALK_TIME, battlers[i].getMonster().getFacingDirection() + 2);
                                alm.defaulted();
                                moves.add(alm);
                                alm = new AbsoluteLifeMovement(0, startPos, WALK_TIME, (battlers[i].getMonster().getFacingDirection() == 1 ? 0 : 1) + 2);
                                alm.defaulted();
                                moves.add(alm);
                                alm = new AbsoluteLifeMovement(0, startPos, 0, origStance);
                                alm.defaulted();
                                moves.add(alm);
                            } else { //walk to required distance -> atk -> walk back if any
                                AbsoluteLifeMovement alm;
                                //if range is enough, then don't move at all :P
                                final boolean shouldAdd = (startPos.x < battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x && battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x - theAtk.getRange() > startPos.x) || (startPos.x > battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x && battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x + theAtk.getRange() < startPos.x);
                                if (shouldAdd) { //walk up to the required range of the attack
                                    List<LifeMovementFragment> moves2 = new ArrayList<>();
                                    maskedPos = new Point(battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x + (startPos.x < battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().x ? -theAtk.getRange() : theAtk.getRange()), battlers[i == 0 ? 1 : 0].getMonster().getTruePosition().y);
                                    alm = new AbsoluteLifeMovement(0, maskedPos, WALK_TIME, battlers[i].getMonster().getFacingDirection() + 2);
                                    alm.defaulted();
                                    moves2.add(alm); //separate for this movement only, as we must move THEN attack
                                    //map.broadcastMessage(MobPacket.moveMonster(false, (byte) -1, 0, 0, 0, battlers[i].getMonster().getObjectId(), startPos, moves2));
                                }
                                RelativeLifeMovement rlm = new RelativeLifeMovement(2, maskedPos == null ? battlers[i == 0 ? 1 : 0].getMonster().getPosition() : new Point(0, 0), 0, battlers[i].getMonster().getFacingDirection() + 2);
                                moves.add(rlm);
                                if (shouldAdd) {
                                    alm = new AbsoluteLifeMovement(0, maskedPos, Math.min(theAtk.attackAfter, WALK_TIME - WALK_TIME / 2), battlers[i].getMonster().getFacingDirection() + 2);
                                    alm.defaulted(); //stay at the point located for a certain time
                                    moves.add(alm);
                                    alm = new AbsoluteLifeMovement(0, startPos, WALK_TIME - Math.min(theAtk.attackAfter, WALK_TIME - WALK_TIME / 2), (battlers[i].getMonster().getFacingDirection() == 1 ? 0 : 1) + 2);
                                    alm.defaulted(); //then come back quickly
                                    moves.add(alm);
                                }
                                alm = new AbsoluteLifeMovement(0, startPos, 0, origStance);
                                alm.defaulted();
                                moves.add(alm);
                            }
                            final Point mPos = maskedPos;
                            if (mPos == null) {
                                //map.broadcastMessage(MobPacket.moveMonster(skillByte > 0, skillByte, 0, 0, 0, battlers[i].getMonster().getObjectId(), startPos, moves));
                                moves.clear();
                            }
                            EtcTimer.getInstance().schedule(() -> {
                                if (disposed || map == null || battlers[i == 1 ? 0 : 1] == null || battlers[i] == null) {
                                    return;
                                }
                                if (mPos != null) {
                                    // map.broadcastMessage(MobPacket.moveMonster(skillByte > 0, skillByte, 0, 0, 0, battlers[i].getMonster().getObjectId(), mPos, moves));
                                    moves.clear();
                                }
                                long basedamage = basedamagee;
                                if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.NoGuard && battlers[i].getAbility() != PokemonAbility.NoGuard && battlers[i].getACC() < (Randomizer.nextInt(1 + battlers[i == 1 ? 0 : 1].getEVA()) * (s == MonsterStatus.MOB_STAT_Blind ? 2 : 1))) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack missed!"));
                                    basedamage = 0;
                                } else {
                                    if (ee <= 0.0) {
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack didn't affect " + battlers[i == 0 ? 1 : 0].getName() + "..."));
                                    } else if (ee < 1.0) {
                                        if (battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.WonderGuard) {
                                            basedamage = 0;
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack was negated by Wonder Guard!"));
                                        } else if (battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Unnerve && battlers[i].getItem() == HoldItem.Dark_Chocolate) {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s item negated the effectiveness."));
                                            basedamage *= 2;
                                            battlers[i].setItem(0);
                                        } else if (battlers[i].getAbility() == PokemonAbility.TintedLens) {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s ability negated the effectiveness."));
                                            basedamage *= 2;
                                        } else {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack wasn't very effective."));
                                        }
                                    } else if (ee > 1.0) {
                                        if (battlers[i].getAbility() != PokemonAbility.Klutz && battlers[i].getAbility() != PokemonAbility.Unnerve && battlers[i == 1 ? 0 : 1].getItem() == HoldItem.White_Chocolate) {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s effectiveness was negated!"));
                                            basedamage /= 2;
                                            battlers[i == 1 ? 0 : 1].setItem(0);
                                        } else {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack was super effective!"));
                                        }
                                    } else {
                                        if (battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.WonderGuard) {
                                            basedamage = 0;
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack was negated by Wonder Guard!"));
                                        }
                                    }
                                }
                                if (atk == 0) { //defined as "contact"
                                    if (battlers[i].getCurrentStatus() == null && battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.EffectSpore && Randomizer.nextInt(100) < 50) {
                                        MonsterStatus stati = null;
                                        switch (Randomizer.nextInt(3)) {
                                            case 0:
                                                stati = MonsterStatus.MOB_STAT_Poison;
                                                break;
                                            case 1:
                                                stati = MonsterStatus.MOB_STAT_Web;
                                                break;
                                            case 2:
                                                stati = MonsterStatus.MOB_STAT_Blind;
                                                break;
                                        }
                                        final MonsterStatusEffect mse = new MonsterStatusEffect(stati, stati == MonsterStatus.MOB_STAT_Poison ? (int) (battlers[i].calcHP() / 16) : 1, MonsterStatus.genericSkill(stati), null, false, 0);
                                        battlers[i].setStatus(mse);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was inflicted with " + StringUtil.makeEnumHumanReadable(stati.name()) + " status!"));
                                    } else if (battlers[i].getCurrentStatus() == null && battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.PoisonPoint && Randomizer.nextInt(100) < 50) {
                                        final MonsterStatus stati = MonsterStatus.MOB_STAT_Poison;
                                        final MonsterStatusEffect mse = new MonsterStatusEffect(stati, (int) (battlers[i].calcHP() / 16), MonsterStatus.genericSkill(stati), null, false, 0);
                                        battlers[i].setStatus(mse);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was inflicted with " + StringUtil.makeEnumHumanReadable(stati.name()) + " status!"));
                                    } else if (battlers[i].getCurrentStatus() == null && battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.Static && Randomizer.nextInt(100) < 50) {
                                        final MonsterStatus stati = MonsterStatus.MOB_STAT_Web;
                                        final MonsterStatusEffect mse = new MonsterStatusEffect(stati, 1, MonsterStatus.genericSkill(stati), null, false, 0);
                                        battlers[i].setStatus(mse);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was inflicted with " + StringUtil.makeEnumHumanReadable(stati.name()) + " status!"));
                                    } else if (battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.IronBarbs && battlers[i].getAbility() != PokemonAbility.MagicGuard) {
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " got hurt " + (battlers[i].calcHP() / 8) + " damage from Rough Skin!"));
                                        battlers[i].damage((int) battlers[i].calcHP() / 8, map, 0, false);
                                        updateHP(i);
                                    }
                                }
                                if (basedamage > 1 && battlers[i].getAbility() == PokemonAbility.SheerForce && Randomizer.nextInt(3) == 0) {
                                    basedamage = (basedamage * 13 / 10);
                                }
                                if (basedamage > 1 && battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Mini_Dragon && battlers[i].getAbility() != PokemonAbility.MagicGuard) {
                                    basedamage = (basedamage * 13 / 10);
                                    battlers[i].damage((int) battlers[i].calcHP() / 8, map, battlers[i == 0 ? 1 : 0] == null ? 0 : battlers[i == 0 ? 1 : 0].getMonsterId(), true);
                                    updateHP(i);
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 8) + " damage from its item!"));
                                }
                                if (battlers[i == 1 ? 0 : 1].getAbility() != PokemonAbility.BattleArmor && basedamage > 1 && critical) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack was a critical hit!"));
                                    basedamage *= (battlers[i].getAbility() == PokemonAbility.Sniper ? 3 : 2);
                                    if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.AngerPoint) {
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s attack triggered the foe's Anger Point!"));
                                        battlers[i == 1 ? 0 : 1].setMod(PokemonStat.ATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.ATK)));
                                        battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPATK)));
                                    }
                                }
                                if (basedamage > 1 && inc != null) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s " + StringUtil.makeEnumHumanReadable(inc.name()) + " increased " + (levelInc == 1 ? "slightly" : "greatly") + "!"));
                                    for (int x = 0; x < levelInc; x++) {
                                        battlers[i].setMod(inc, battlers[i].increaseMod(battlers[i].getMod(inc)));
                                    }
                                }
                                if (basedamage > 1 && dec != null && battlers[i == 1 ? 0 : 1].getItem() != HoldItem.Kenta_Report) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s " + StringUtil.makeEnumHumanReadable(dec.name()) + " decreased " + (levelDec == 1 ? "slightly" : "greatly") + "!"));
                                    for (int x = 0; x < levelDec; x++) {
                                        battlers[i == 1 ? 0 : 1].setMod(dec, battlers[i == 1 ? 0 : 1].decreaseMod(battlers[i == 1 ? 0 : 1].getMod(dec)));
                                    }
                                    if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.Defiant) {
                                        for (int x = 0; x < 2; x++) {
                                            battlers[i == 1 ? 0 : 1].setMod(PokemonStat.ATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i].getMod(PokemonStat.ATK)));
                                            battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i].getMod(PokemonStat.SPATK)));
                                        }
                                    }
                                }
                                if (basedamage > 1 && flinch) {
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " absorbed " + (basedamage / 2) + " HP!"));
                                    battlers[i].damage((int) -(basedamage / 2), map, 0, false);
                                    updateHP(i);
                                } else if (basedamage > 1 && battlers[i].getItem() != null && battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() == HoldItem.Strange_Slush) {
                                    int percentHP = (int) Math.min(100, (basedamage * 100) / battlers[i == 0 ? 1 : 0].calcHP());
                                    if (percentHP > 0 && battlers[i].calcHP() > 800) {
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " absorbed " + (percentHP * battlers[i].calcHP() / 800) + " HP!"));
                                        battlers[i].damage((int) -(percentHP * battlers[i].calcHP() / 800), map, 0, false);
                                        updateHP(i);
                                    }
                                }
                                if (extraEffects && basedamage > 1 && stat != null && battlers[i == 1 ? 0 : 1].getCurrentStatus() == null && battlers[i].getAbility() != PokemonAbility.Klutz && battlers[i == 1 ? 0 : 1].getItem() != HoldItem.Pheremone) {
                                    if (battlers[i].getAbility() != PokemonAbility.Klutz && battlers[i].getAbility() != PokemonAbility.Unnerve && battlers[i == 1 ? 0 : 1].getItem() == HoldItem.Pineapple) {
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s item negated the status!"));
                                        battlers[i == 1 ? 0 : 1].setItem(0);
                                    } else {
                                        battlers[i == 1 ? 0 : 1].setStatus(new MonsterStatusEffect(stat, 1, MonsterStatus.genericSkill(stat), null, false, 0));
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + " was inflicted with " + StringUtil.makeEnumHumanReadable(stat.name()) + " status!"));
                                        if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.Synchronize && battlers[i].getCurrentStatus() == null && battlers[i == 0 ? 1 : 0].getAbility() != PokemonAbility.Klutz && battlers[i].getItem() != HoldItem.Pheremone) {
                                            battlers[i].setStatus(new MonsterStatusEffect(stat, stat == MonsterStatus.MOB_STAT_Poison ? (int) (battlers[i].calcHP() / 16) : 1, MonsterStatus.genericSkill(stat), null, false, 0));
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was inflicted with " + StringUtil.makeEnumHumanReadable(stat.name()) + " status!"));
                                        }
                                    }
                                }
                                if (basedamage != 0) {
                                    if (battlers[i == 1 ? 0 : 1] == null) {
                                        return;
                                    }
                                    if (basedamage > 1) {
                                        final byte skillByte2 = (byte) (14 + battlers[i == 1 ? 0 : 1].getMonster().getFacingDirection());
                                        AbsoluteLifeMovement alm2 = new AbsoluteLifeMovement(0, battlers[i == 1 ? 0 : 1].getMonster().getPosition(), 0, battlers[i == 1 ? 0 : 1].getMonster().getStance());
                                        alm2.defaulted();
                                        moves.add(alm2); //"hit" face
                                        // map.broadcastMessage(MobPacket.moveMonster(true, skillByte2, 0, 0, 0, battlers[i == 1 ? 0 : 1].getMonster().getObjectId(), battlers[i == 1 ? 0 : 1].getMonster().getTruePosition(), moves));
                                    }
                                    if (((battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.DrySkin || battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.WaterAbsorb) && element == PokemonElement.Fish) || (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.VoltAbsorb && element == PokemonElement.Lightning) || (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.SapSipper && element == PokemonElement.Plant)) {
                                        int percentHP = (int) Math.min(100, (basedamage * 100) / battlers[i == 0 ? 1 : 0].calcHP());
                                        if (percentHP > 0 && battlers[i == 0 ? 1 : 0].calcHP() > 800) {
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + " absorbed " + (percentHP * battlers[i == 1 ? 0 : 1].calcHP() / 800) + " HP!"));
                                            battlers[i == 0 ? 1 : 0].damage((int) -(percentHP * battlers[i == 1 ? 0 : 1].calcHP() / 800), map, 0, false);
                                            updateHP(i == 0 ? 1 : 0);
                                        }
                                    } else {
                                        if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.DrySkin && element == PokemonElement.Fire) {
                                            basedamage *= 2;
                                        } else if ((battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.ThickFat || battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.Heatproof) && element == PokemonElement.Fire) {
                                            basedamage /= 2;
                                        } else if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.ThickFat && element == PokemonElement.Ice) {
                                            basedamage /= 2;
                                        } else if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.Multiscale && battlers[i == 1 ? 0 : 1].getHPPercent() >= 100) {
                                            basedamage /= 2;
                                        } else if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.WeakArmor) {
                                            battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPEED, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPEED)));
                                            battlers[i == 1 ? 0 : 1].setMod(PokemonStat.DEF, battlers[i == 1 ? 0 : 1].decreaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.DEF)));
                                        } else if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.MotorDrive && element == PokemonElement.Lightning) {
                                            battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPEED, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPEED)));
                                        }
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + " received " + basedamage + " damage!"));
                                        battlers[i == 1 ? 0 : 1].damage((int) basedamage, map, battlers[i].getMonsterId(), false);
                                        updateHP(i == 1 ? 0 : 1);
                                    }
                                }
                                if (battlers[i == 1 ? 0 : 1].getCurrentHP() <= 0) {
                                    //exp distribution
                                    if (battlers[i] != null && battlers[i].getAbility() == PokemonAbility.Moxie) {
                                        battlers[i].setMod(PokemonStat.ATK, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.ATK)));
                                        battlers[i].setMod(PokemonStat.SPATK, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPATK)));
                                    }
                                    final MapleCharacter ch = map.getCharacterById(characterIds[i]);
                                    if (ch != null && instanceid >= 0) {
                                        for (Battler b : ch.getBattlers()) {
                                            if (b != null && b.getItem() == HoldItem.Maha_Charm) {
                                                battlers[i == 1 ? 0 : 1].addMonsterId(b.getMonsterId());
                                            }
                                        }
                                        for (int z : battlers[i == 1 ? 0 : 1].getDamaged()) {
                                            for (Battler b : ch.getBattlers()) {
                                                if (b != null && b.getMonsterId() == z) {
                                                    final int oLevel = b.getTrueLevel();
                                                    final String oName = b.getStats().getName();
                                                    final int xx = battlers[i == 1 ? 0 : 1].getExp(npcTeam != null, z);
                                                    b.gainExp(xx, ch);
                                                    ch.dropMessage(-6, b.getName() + " gained " + xx + " EXP.");
                                                    ch.changedBattler();
                                                    if (b.getTrueLevel() > oLevel) {
                                                        ch.dropMessage(-6, b.getName() + " leveled up to level " + b.getTrueLevel() + "!");
                                                        if (!b.getStats().getName().equals(oName)) {
                                                            ch.getClient().announce(MaplePacketCreator.playSound("5th_Maple/prize"));
                                                            ch.dropMessage(-6, b.getName() + " evolved from a " + oName + " to a " + b.getStats().getName() + "!!!");
                                                            if (b.getMonster() != null) { //respawn it
                                                                Point pos1 = b.getMonster().getPosition();
                                                                boolean left1 = !b.getMonster().isFacingLeft();
                                                                map.killMonster(b.getMonster());
                                                                spawnMonster(pos1, left1, true, b);
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    final MapleMonsterStats mons = battlers[i == 1 ? 0 : 1].getStats();
                                    map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + " fainted!"));
                                    if (battlers[i == 1 ? 0 : 1].getAbility() == PokemonAbility.Aftermath) {
                                        battlers[i].damage((int) battlers[i].calcHP() / 4, map, 0, true);
                                        updateHP(i);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + " was hurt " + (battlers[i].calcHP() / 4) + " damage by Aftermath!"));
                                    }
                                    map.killMonster(battlers[i == 1 ? 0 : 1].getMonster());
                                    if (battlers[i] != null) {
                                        battlers[i].removeMonsterId(battlers[i == 1 ? 0 : 1].getMonsterId());
                                    }
                                    battlers[i == 1 ? 0 : 1].wipe();
                                    battlers[i == 1 ? 0 : 1] = null;
                                    if (instanceid >= 0 && i == 0 && npcTeam == null) {
                                        final MapleCharacter chr = map.getCharacterById(characterIds[0]);
                                        chr.dropMessage(-6, "The wild monster fainted.");
                                        chr.send(MaplePacketCreator.playSound("Romio/discovery"));
                                        dispose(chr, false);
                                        return;
                                    }
                                    if (instanceid >= 0 && i == 0 && npcTeam != null) {
                                        if (npcTeam.size() > 0) {
                                            turn[i] = Turn.DISABLED;
                                            turn[i == 1 ? 0 : 1] = Turn.SWITCH;
                                            attacks = new String[2];
                                            switches = new Battler[2];
                                            switches[i == 1 ? 0 : 1] = new Battler(MapleLifeFactory.getMonsterStats(npcTeam.get(0)));
                                            npcTeam.remove(0);
                                        } else {
                                            final MapleCharacter chrr = map.getCharacterById(characterIds[i]);
                                            map.broadcastMessage(MaplePacketCreator.serverNotice(6, chrr.getName() + " won the match."));
                                            giveReward(chrr);
                                            forfeit(chrr, true);
                                        }
                                        return;
                                    }
                                    final MapleCharacter chrr = map.getCharacterById(characterIds[i == 0 ? 1 : 0]);
                                    boolean cont = false;
                                    for (Battler b : chrr.getBattlers()) {
                                        if (b != null && b.getCurrentHP() > 0) {
                                            cont = true;
                                            break;
                                        }
                                    }
                                    turn[i] = Turn.DISABLED;
                                    turn[i == 1 ? 0 : 1] = cont ? null : Turn.DISABLED;
                                    if (!cont) {
                                        playerWin(i);
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, chrr.getName() + " lost the match."));
                                        forfeit(chrr, true);
                                    }
                                } else {
                                    if (battlers[i].getAbility() != PokemonAbility.Klutz && battlers[i].getAbility() != PokemonAbility.Unnerve && battlers[i == 0 ? 1 : 0].getItem() != null && battlers[i == 0 ? 1 : 0].getHPPercent() < (battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.Gluttony ? 75 : 50)) {
                                        boolean usedItem = battlers[i == 0 ? 1 : 0].getAbility() == PokemonAbility.Unburden;
                                        switch (battlers[i == 0 ? 1 : 0].getItem()) {
                                            case Red_Candy:
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s attack increased from its item!"));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.ATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.ATK)));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPATK, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPATK)));
                                                battlers[i == 0 ? 1 : 0].setItem(0);
                                                break;
                                            case Blue_Candy:
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s defense increased from its item!"));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.DEF, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.DEF)));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPDEF, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPDEF)));
                                                battlers[i == 0 ? 1 : 0].setItem(0);
                                                break;
                                            case Green_Candy:
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s speed increased from its item!"));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.SPEED, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.SPEED)));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.EVA, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.EVA)));
                                                battlers[i == 1 ? 0 : 1].setMod(PokemonStat.ACC, battlers[i == 1 ? 0 : 1].increaseMod(battlers[i == 1 ? 0 : 1].getMod(PokemonStat.ACC)));
                                                battlers[i == 0 ? 1 : 0].setItem(0);
                                                break;
                                            case Strawberry:
                                                map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i == 1 ? 0 : 1].getName() + "'s HP healed from its item!"));
                                                battlers[i == 0 ? 1 : 0].damage((int) -(battlers[i].calcHP() / 10), map, 0, false);
                                                updateHP(i == 0 ? 1 : 0);
                                                battlers[i == 0 ? 1 : 0].setItem(0);
                                                break;
                                            default:
                                                usedItem = false;
                                                break;
                                        }
                                        if (usedItem) {
                                            battlers[i].setMod(PokemonStat.SPEED, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPEED)));
                                        }
                                    }
                                    if (battlers[i].getAbility() == PokemonAbility.SpeedBoost) {
                                        battlers[i].setMod(PokemonStat.SPEED, battlers[i].increaseMod(battlers[i].getMod(PokemonStat.SPEED)));
                                    } else if (battlers[i].getAbility() == PokemonAbility.Moody) {
                                        PokemonStat down = PokemonStat.getRandom(), up = PokemonStat.getRandom();
                                        for (int x = 0; x < 2; x++) {
                                            battlers[i].setMod(up, battlers[i].increaseMod(battlers[i].getMod(up)));
                                            battlers[i].setMod(down, battlers[i].decreaseMod(battlers[i].getMod(down)));
                                        }
                                        map.broadcastMessage(MaplePacketCreator.serverNotice(6, battlers[i].getName() + "'s Moody increased the " + StringUtil.makeEnumHumanReadable(up.name()) + " stat and decreased the " + StringUtil.makeEnumHumanReadable(down.name()) + " stat!"));
                                    }
                                }
                            }, WALK_TIME);

                            break;
                    }
                }, timeFirst);
                timeFirst += (WALK_TIME * 2) + (WALK_TIME / 8);
            }
        }
        if ((turn[0] == Turn.DISABLED || turn[1] == Turn.DISABLED || disposed) && turn[0] != Turn.SWITCH && turn[1] != Turn.SWITCH && npcTeam == null) {
            return;
        }
        EtcTimer.getInstance().schedule(() -> {
            if ((turn[0] == Turn.DISABLED || turn[1] == Turn.DISABLED || disposed) && turn[0] != Turn.SWITCH && (turn[1] != Turn.SWITCH || (npcTeam != null && battlers[1] == null))) {
                if (npcTeam != null && turn[1] == Turn.SWITCH) {
                    makeTurn();
                }
                return;
            }
            final boolean[] truant = {false, false};
            switches = new Battler[2];
            attacks = new String[2];
            if (!disposed) {
                for (int i = 0; i < characterIds.length; i++) {
                    updateHP(i);
                    if (battlers[i] != null && battlers[i].getAbility() == PokemonAbility.Truant && turn[i] == Turn.ATTACK) {
                        truant[i] = true;
                    }
                }
            }
            turn = new Turn[]{null, null};
            for (int i = 0; i < truant.length; i++) {
                if (truant[i]) {
                    turn[i] = Turn.TRUANT;
                }
            }
            if (!disposed && instanceid >= 0 && turn[1] != Turn.TRUANT) {
                turn[1] = makeRandomTurn();
            }
        }, timeFirst - (WALK_TIME / 8));
    }

    public boolean isTrainerBattle() {
        return npcTeam != null;
    }

    public void playerWin(int i) {
        final int[] averageLevel = new int[]{0, 0}, numBattlers = new int[]{0, 0};
        for (int x = 0; x < 2; x++) {
            MapleCharacter pro = map.getCharacterById(characterIds[x]);
            if (pro == null) {
                return;
            }
            for (Battler b : pro.getBattlers()) {
                if (b != null) {
                    averageLevel[x] += b.getLevel();
                    numBattlers[x]++;
                }
            }
            averageLevel[x] /= numBattlers[x];
        }
        final MapleCharacter winner = map.getCharacterById(characterIds[i]);
        final MapleCharacter loser = map.getCharacterById(characterIds[i == 0 ? 1 : 0]);
        if (Math.abs(averageLevel[0] - averageLevel[1]) > 20 || numBattlers[0] != numBattlers[1]) {
            winner.dropMessage(-6, "The battle did not count as a win due to the ease.");
        } else if (!winner.canBattle(loser)) {
            winner.dropMessage(-6, "The battle did not count as a win due to you battling the character in this month.");
        } else {
            winner.hasBattled(loser);
            winner.increaseTotalWins();
            loser.increaseTotalLosses();
            int theWins = winner.getIntNoRecord(GameConstants.POKEMON_WINS) + 1, theWins2 = Math.max(0, loser.getIntNoRecord(GameConstants.POKEMON_WINS) - 1);
            winner.getQuestNAdd(MapleQuest.getInstance(GameConstants.POKEMON_WINS)).setCustomData(String.valueOf(theWins));
            loser.getQuestNAdd(MapleQuest.getInstance(GameConstants.POKEMON_WINS)).setCustomData(String.valueOf(theWins2));
            winner.dropMessage(-6, "You have gained a win on your record! Total Wins: " + winner.getTotalWins() + ", Current Wins: " + theWins);
            loser.dropMessage(-6, "You have gained a loss on your record... Total Losses: " + loser.getTotalLosses() + ", Current Wins: " + theWins2);
        }
    }
}
