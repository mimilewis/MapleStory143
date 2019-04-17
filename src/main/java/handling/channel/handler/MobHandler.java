package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.MapleInventoryType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleInventoryManipulator;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;
import server.life.MobSkill;
import server.life.MobSkillFactory;
import server.maps.MapleMap;
import server.maps.MapleMapObjectType;
import server.movement.LifeMovementFragment;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.Triple;
import tools.data.input.LittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.MobPacket;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MobHandler {

    private static final Logger log = LogManager.getLogger(MobHandler.class);
    /*
     * 怪物移动
     */

    public static void MoveMonster(SeekableLittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null) {
            return;
        }
        int objectid = slea.readInt();
        MapleMonster monster = chr.getMap().getMonsterByOid(objectid);
        if (monster == null || monster.getType() != MapleMapObjectType.MONSTER) {
            return;
        }
        if (monster.getLinkCID() > 0) {
            return;
        }
        List<LifeMovementFragment> res;

//        if (chr.isShowPacket()) {
//            String dropString = slea.toString(true).substring(0, 60);
//            chr.dropDebugMessage(dropString);
//        }

//        slea.skip(1); //[00]
        short moveid = slea.readShort();
        boolean useSkill = (slea.readByte() & 0xFF) > 0;
        byte mode = slea.readByte();
        int skillId = slea.readByte() & 0xFF; //是否使用技能
        int skillLevel = slea.readByte(); //技能ID
        short effectAfter = slea.readShort(); //使用技能的延迟时间

        int realskill = 0;
        int level = 0;
        
        /*
         * 8850003 - 伊卡尔特
         * 以上怪物移动会出现 aa > 0
         */
        List<Pair<Short, Short>> list = new ArrayList<>();
        byte aa = slea.readByte();
        if (aa > 0) {
            for (int i = 0; i < aa; i++) {
                list.add(new Pair<>(slea.readShort(), slea.readShort()));
            }
        }
        /*
         * 8300007 - 御龙魔 - gg = 7
         * 8840000 - 班·雷昂 - gg = 5
         * 8860000 - 阿卡伊勒 - gg = 5
         * 以上怪物移动会出现 gg > 0
         */
        List<Short> list1 = new ArrayList<>();
        byte gg = slea.readByte();
        if (gg > 0) {
            for (int i = 0; i < gg; i++) {
                list1.add(slea.readShort());
            }
        }
        slea.skip(1); //[00]
        boolean skipped = slea.readInt() != 0 && gg > 0;
        slea.skip(8); //[CC DD FF 00 CC DD FF 00]
        slea.skip(5); //[FC 2A CC 63 01]
        slea.skip(4); //怪物的x y坐标
        short start_x = slea.readShort();
        short start_y = slea.readShort();
        Point startPos = new Point(start_x, start_y);
        slea.skip(4);
        if (monster.getId() == 9300281 && skipped) { //莱格斯
            if (slea.readByte() > 10) { //estimate
                slea.skip(8);
            } else {
                slea.seek(slea.getPosition() - 1);
            }
        }
        String packet = slea.toString(true);
        try {
            res = MovementParse.parseMovement(slea, 2);
        } catch (ArrayIndexOutOfBoundsException e) {
            MovementParse.log.error("怪物ID " + monster.getId() + ", AIOBE Type2:\r\n" + packet, e);
            return;
        }
        if (res != null && res.size() > 0) {
            MapleMap map = chr.getMap();
            if (slea.available() != 29) {
                MovementParse.log.error("slea.available != 29 (movement parsing error)\r\n怪物ID: " + monster.getId() + "\r\n" + packet);
                return;
            }

            if (useSkill || (monster.getStats().isSkeleton() && Randomizer.nextInt(10) < 7)) {
                boolean used = false;
                byte size = monster.getNoSkills();
                if (size > 0) {
                    int random = Randomizer.nextInt(size);
                    Triple<Integer, Integer, Integer> skillToUse = monster.getSkills().get(random);
                    realskill = skillToUse.getLeft();
                    level = skillToUse.getMid();
                    MobSkill toUse = MobSkillFactory.getMobSkill(realskill, level);
                    if (toUse != null && !toUse.checkCurrentBuff(chr, monster)) {
                        final long currentTimeMillis = System.currentTimeMillis();
                        if (currentTimeMillis - monster.getLastSkillUsed(realskill) > toUse.getCoolTime() && !toUse.onlyOnce()) {
                            int reqHp = (int) ((float) monster.getHp() / (float) monster.getMobMaxHp() * 100.0F);
                            if (toUse.getMpCon() <= monster.getMp() && reqHp <= toUse.getHP()) {
                                used = true;
                                monster.setLastSkillUsed(realskill, currentTimeMillis, toUse.getCoolTime());
                            }
                        }
                    }
                }
                if (used && monster.getStats().isSkeleton()) {
                    skillId = realskill;
                    skillLevel = level;
                    realskill = 0;
                    level = 0;
                }
                if (!used) {
                    realskill = 0;
                    level = 0;
                }

                if (chr.isShowPacket()) {
                    if (skillLevel > 0) {
                        chr.dropDebugMessage(1, "[怪物技能] 怪物:" + MapleLifeFactory.getMonsterName(monster.getId()) + "(" + monster.getId() + ") 技能ID:" + skillLevel + " action:" + skillId + " useSkill:" + useSkill + " after:" + effectAfter);
                    }
                }
            }
            MovementParse.updatePosition(res, monster, -1);
            Point endPos = monster.getTruePosition();
            map.broadcastMessage(c.getPlayer(), MobPacket.moveMonster(useSkill, mode, skillId, skillLevel, effectAfter, objectid, startPos, res, list, list1), endPos);
            map.moveMonster(monster, monster.getPosition());

            if (skillId > 0 && skillLevel > 0 && monster.getNoSkills() > 0) {
                for (Triple<Integer, Integer, Integer> pair : monster.getSkills()) {
                    if (pair.getLeft() == skillId && pair.getMid() == skillLevel) {
                        MobSkill skillData = MobSkillFactory.getMobSkill(skillId, skillLevel);
                        if (skillData != null) {
                            skillData.applyEffect(c.getPlayer(), monster, pair.getRight(), true);
                            break;
                        }
                        break;
                    }
                }
            }
            if (monster.checkAggro(mode == -1 && !useSkill && startPos.distanceSq(endPos) <= 2.0) || monster.checkTrans(chr) || monster.getController() != chr) {
                return;
            }
            if (!c.getPlayer().isAlive() || (skillId == -1 && monster.isControllerKnowsAboutAggro() && !monster.isFirstAttack())) {
                monster.setControllerHasAggro(false);
                monster.setControllerKnowsAboutAggro(false);
            }
            c.announce(MobPacket.moveMonsterResponse(objectid, moveid, monster.getMp(), monster.isControllerHasAggro() | monster.isControllerKnowsAboutAggro(), realskill, level));
        }
    }

    /*
     * 怪物攻击怪物
     * 月妙任务出现
     */
    public static void FriendlyDamage(LittleEndianAccessor slea, MapleCharacter chr) {
        MapleMap map = chr.getMap();
        if (map == null) {
            return;
        }
        MapleMonster mobfrom = map.getMonsterByOid(slea.readInt());
        slea.skip(4); // 角色ID
        MapleMonster mobto = map.getMonsterByOid(slea.readInt());

        if (mobfrom != null && mobto != null && mobto.getStats().isFriendly()) {
            int damage = (mobto.getStats().getLevel() * Randomizer.nextInt(mobto.getStats().getLevel())) / 2; // Temp for now until I figure out something more effective
            mobto.damage(chr, damage, true);
            checkShammos(chr, mobto, map);
        }
    }

    public static void checkShammos(MapleCharacter chr, MapleMonster mobto, MapleMap map) {
        if (!mobto.isAlive() && mobto.getStats().isEscort()) { //shammos
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) { //check for 2022698
                if (chrz.getParty() != null && chrz.getParty().getLeader().getId() == chrz.getId()) {
                    //leader
                    if (chrz.haveItem(2022698)) { //万年冰河水
                        MapleInventoryManipulator.removeById(chrz.getClient(), MapleInventoryType.USE, 2022698, 1, false, true);
                        mobto.heal((int) mobto.getMobMaxHp(), mobto.getMobMaxMp(), true);
                        return;
                    }
                    break;
                }
            }
            map.broadcastMessage(MaplePacketCreator.serverNotice(6, "Your party has failed to protect the monster."));
            MapleMap mapp = chr.getMap().getForcedReturnMap();
            for (MapleCharacter chrz : map.getCharactersThreadsafe()) {
                chrz.changeMap(mapp, mapp.getPortal(0));
            }
        } else if (mobto.getStats().isEscort() && mobto.getEventInstance() != null) {
            mobto.getEventInstance().setProperty("HP", String.valueOf(mobto.getHp()));
        }
    }

    /*
     * 怪物自爆
     * 8500003 - 小黑水雷
     * 8500003 - 小黑水雷
     */
    public static void MonsterBomb(int oid, MapleCharacter chr) {
        MapleMonster monster = chr.getMap().getMonsterByOid(oid);
        if (monster == null || !chr.isAlive() || chr.isHidden() || monster.getLinkCID() > 0) {
            return;
        }
        byte selfd = monster.getStats().getSelfD();
        if (selfd != -1) {
            monster.setHp(0);
            chr.getMap().killMonster(monster, chr, false, false, selfd);
        }
    }

    /*
     * 怪物仇恨
     */
    public static void AutoAggro(int monsteroid, MapleCharacter chr) {
//        log.trace("调用");
        if (chr == null || chr.getMap() == null || chr.isHidden()) { //no evidence :)
            return;
        }
        MapleMonster monster = chr.getMap().getMonsterByOid(monsteroid);
        if (monster != null && monster.getLinkCID() <= 0) {
            if (monster.getController() != null) {
                if (chr.getMap().getCharacterById(monster.getController().getId()) == null) {
                    monster.switchController(chr, true);
                } else {
                    monster.switchController(monster.getController(), true);
                }
            } else {
                monster.switchController(chr, true);
            }
        }
//        chr.send(MobPacket.unknown(monsteroid));
    }

    public static void MonsterSpecialSkill(LittleEndianAccessor slea, MapleCharacter chr) {
        if (chr == null) {
            return;
        }

        int moboid = slea.readInt();
        int unk_1 = slea.readInt();
        int unk_2 = slea.readInt();
        int unk_3 = slea.readInt();
        int unk_4 = slea.readInt();

//        chr.send(MobPacket.showMonsterSpecialSkill(moboid));
    }
}
