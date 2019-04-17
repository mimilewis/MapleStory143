package server.life;

import client.MapleCharacter;
import client.MapleDisease;
import client.status.MonsterStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import constants.GameConstants;
import constants.ServerConstants;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.MapleMist;
import tools.MaplePacketCreator;
import tools.Randomizer;
import tools.packet.MobPacket;
import tools.packet.UIPacket;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MobSkill {

    private final int skillId;
    private final int skillLevel;
    private int mpCon;
    private int spawnEffect;
    private int hp;
    private int x;
    private int y;
    private long duration, cooltime;
    private float prop;
    //private short effect_delay;
    private short limit;
    private List<Integer> toSummon = new ArrayList<>();
    private Point lt, rb;
    private boolean summonOnce;
    private int areaSequenceDelay;
    private int skillAfter;
    private int force, forcex;

    @JsonCreator
    public MobSkill(@JsonProperty("skillId") int skillId, @JsonProperty("level") int level) {
        this.skillId = skillId;
        this.skillLevel = level;
    }

    public void setOnce(boolean o) {
        this.summonOnce = o;
    }

    public boolean onlyOnce() {
        return summonOnce;
    }

    public void addSummons(List<Integer> toSummon) {
        this.toSummon = toSummon;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public void setProp(float prop) {
        this.prop = prop;
    }

    public void setLtRb(Point lt, Point rb) {
        this.lt = lt;
        this.rb = rb;
    }

    public boolean checkCurrentBuff(MapleCharacter player, MapleMonster monster) {
        boolean stop = false;
        switch (skillId) {
            case 100:
            case 110:
            case 150:
                stop = monster.isBuffed(MonsterStatus.MOB_STAT_PowerUp);
                break;
            case 101:
            case 111:
            case 151:
                stop = monster.isBuffed(MonsterStatus.MOB_STAT_MagicUp);
                break;
            case 102:
            case 112:
            case 152:
                stop = monster.isBuffed(MonsterStatus.MOB_STAT_PGuardUp);
                break;
            case 103:
            case 113:
            case 153:
                stop = monster.isBuffed(MonsterStatus.MOB_STAT_MGuardUp);
                break;
            //154-157, don't stop it
            case 140:
            case 141:
            case 142:
            case 143:
            case 144:
            case 145:
                stop = monster.isBuffed(MonsterStatus.MOB_STAT_HardSkin)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_MImmune)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_PCounter)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_PImmune)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_MCounter)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_Dazzle)
                        || monster.isBuffed(MonsterStatus.MOB_STAT_SealSkill);
                break;
            case 200:
                stop = player.getMap().getNumMonsters() >= limit;
                break;
        }
        stop |= monster.isBuffed(MonsterStatus.MOB_STAT_MagicCrash);
        return stop;
    }

    /*
     * 怪物BUFF解释
     * 100 = 物理攻击提高
     * 101 = 魔法攻击提高
     * 102 = 物理防御提高
     * 103 = 魔法防御提高
     * 104 = 致命攻击 难道就是血蓝为1？
     * 105 = 消费
     * 110 = 周边物理攻击提高
     * 111 = 周边魔法攻击提高
     * 112 = 周边物理防御提高
     * 113 = 周边魔法防御提高
     * 114 = HP恢复
     * 115 = 自己及周围移动速度变化
     * 120 = 封印
     * 121 = 黑暗
     * 122 = 虚弱
     * 123 = 晕眩
     * 124 = 诅咒
     * 125 = 中毒
     * 126 = 慢动作
     * 127 = 魔法无效
     * 128 = 诱惑
     * 129 = 逐出
     * 131 = 区域中毒
     * 133 = 不死化
     * 134 = 药水停止
     * 135 = 从不停止
     * 136 = 致盲
     * 137 = 中毒
     * 138 = 潜在能力无效
     * 140 = 物理防御
     * 141 = 魔法防御
     * 142 = 皮肤硬化
     * 143 = 物理反击免疫
     * 144 = 魔法反击免疫
     * 145 = 物理魔法反击免疫
     * 150 = PAD修改
     * 151 = MAD修改
     * 152 = PDD修改
     * 153 = MDD修改
     * 154 = ACC修改
     * 155 = EVA修改
     * 156 = Speed修改
     * 170 = 传送
     * 200 = 召唤
     */
    public void applyEffect(MapleCharacter player, MapleMonster monster, int effectAfter, boolean skill) {
        MapleDisease disease = MapleDisease.getBySkill(skillId);
        Map<MonsterStatus, Integer> stats = new EnumMap<>(MonsterStatus.class);
        List<Integer> reflection = new LinkedList<>();

        switch (skillId) {
            case 100: //物理攻击提高
            case 110: //周边物理攻击提高
            case 150: //PAD修改
                stats.put(MonsterStatus.MOB_STAT_PowerUp, x);
                break;
            case 101: //魔法攻击提高
            case 111: //周边魔法攻击提高
            case 151: //MAD修改
                stats.put(MonsterStatus.MOB_STAT_MagicUp, x);
                break;
            case 102: //物理防御提高
            case 112: //周边物理防御提高
            case 152: //PDD修改
                stats.put(MonsterStatus.MOB_STAT_PGuardUp, x);
                break;
            case 103: //魔法防御提高
            case 113: //周边魔法防御提高
            case 153: //MDD修改
                stats.put(MonsterStatus.MOB_STAT_MGuardUp, x);
                break;
            case 154: //ACC修改
                stats.put(MonsterStatus.MOB_STAT_ACC, x);
                break;
            case 155: //EVA修改
                stats.put(MonsterStatus.MOB_STAT_EVA, x);
                break;
            case 115: //自己及周围移动速度变化
            case 156: //Speed修改
                stats.put(MonsterStatus.MOB_STAT_Speed, x);
                break;
            case 157: //封印
                stats.put(MonsterStatus.MOB_STAT_Seal, x);
                break;
            case 236: {
                stats.put(MonsterStatus.MOB_STAT_HangOver, x);
                break;
            }
            case 188: {
                stats.put(MonsterStatus.MOB_STAT_Dazzle, x);
                break;
            }
            case 209: {
                break;
            }
            case 114: //HP恢复
                if (lt != null && rb != null && skill && monster != null) {
                    List<MapleMapObject> objects = getObjectsInRange(monster, MapleMapObjectType.MONSTER);
                    int hps = (getX() / 1000) * (int) (950 + 1050 * Math.random());
                    for (MapleMapObject mons : objects) {
                        ((MapleMonster) mons).heal(hps, getY(), true);
                    }
                } else if (monster != null) {
                    monster.heal(getX(), getY(), true);
                }
                break;
            case 105: //恢复 消费?
                if (lt != null && rb != null && skill && monster != null) {
                    List<MapleMapObject> objects = getObjectsInRange(monster, MapleMapObjectType.MONSTER);
                    for (MapleMapObject mons : objects) {
                        if (mons.getObjectId() != monster.getObjectId()) {
                            player.getMap().killMonster((MapleMonster) mons, player, true, false, (byte) 1, 0);
                            monster.heal(getX(), getY(), true);
                            break;
                        }
                    }
                } else if (monster != null) {
                    monster.heal(getX(), getY(), true);
                }
                break;
            case 127: //驱散玩家BUFF 魔法无效？
                if (lt != null && rb != null && skill && monster != null && player != null) {
                    for (MapleCharacter character : getPlayersInRange(monster, player)) {
                        character.dispel();
                    }
                } else if (player != null) {
                    player.dispel();
                }
                break;
            case 129: // 逐出?控制玩家?Banish
                if (monster != null && monster.getMap().getSquadByMap() == null) { //not pb/vonleon map
                    if (monster.getEventInstance() != null && monster.getEventInstance().getName().contains("BossQuest")) {
                        break;
                    }
                    BanishInfo info = monster.getStats().getBanishInfo();
                    if (info != null) {
                        if (lt != null && rb != null && skill && player != null) {
                            for (MapleCharacter chr : getPlayersInRange(monster, player)) {
                                if (!chr.hasBlockedInventory()) {
                                    chr.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
                                }
                            }
                        } else if (player != null && !player.hasBlockedInventory()) {
                            player.changeMapBanish(info.getMap(), info.getPortal(), info.getMsg());
                        }
                    }
                }
                break;
            case 131: // 区域中毒 乌贼怪 扎昆 驮狼雪人
            case 180:
            case 186:
            case 191:
                if (monster != null) {
                    if (skillId == 191) {
                        player.getMap().broadcastMessage(UIPacket.getTopMsg("时间裂缝中发生了\"龟裂\"。"));
                    }
                    MapleMist mist = new MapleMist(calculateBoundingBox(monster.getTruePosition(), monster.isFacingLeft()), monster, this, monster.getPosition());
                    mist.setSkillDelay(skillId == 186 ? 8 : 0);
                    mist.setMistType(skillId == 186 ? 1 : 0);
                    monster.getMap().spawnMist(mist, (int) getDuration(), false);
                }
                break;
            case 140:
                stats.put(MonsterStatus.MOB_STAT_PImmune, x);
                break;
            case 141:
                stats.put(MonsterStatus.MOB_STAT_MImmune, x);
                break;
            case 142:
                stats.put(MonsterStatus.MOB_STAT_HardSkin, Integer.valueOf(x));
                break;
            case 143:
                stats.put(MonsterStatus.MOB_STAT_PCounter, x);
                stats.put(MonsterStatus.MOB_STAT_PImmune, x);
                stats.put(MonsterStatus.MOB_STAT_MGuardUp, x);
                reflection.add(x);
                if (monster != null) {
                    monster.getMap().startMapEffect("请停止攻击，" + monster.getStats().getName() + "开启了反射物攻状态！", 5120116, (int) this.getDuration() / 1000);
                    monster.getMap().broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[系统提示] 注意 " + monster.getStats().getName() + " 即将开启反射物攻状态。" + this.getDuration()));
                }
                break;
            case 144:
                stats.put(MonsterStatus.MOB_STAT_MCounter, x);
                stats.put(MonsterStatus.MOB_STAT_MImmune, x);
                reflection.add(x);
                if (monster != null) {
                    monster.getMap().startMapEffect("请停止攻击，" + monster.getStats().getName() + "开启了反射魔攻状态！", 5120116, (int) this.getDuration() / 1000);
                    monster.getMap().broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[系统提示] 注意 " + monster.getStats().getName() + " 即将开启反射魔攻状态。" + this.getDuration()));
                }
                break;
            case 145:
                stats.put(MonsterStatus.MOB_STAT_PCounter, x);
                stats.put(MonsterStatus.MOB_STAT_PImmune, x);
                stats.put(MonsterStatus.MOB_STAT_MCounter, x);
                stats.put(MonsterStatus.MOB_STAT_MImmune, x);
                reflection.add(x);
                reflection.add(x);
                if (monster != null) {
                    monster.getMap().startMapEffect("请停止攻击，" + monster.getStats().getName() + "开启了反射物攻和魔攻状态！", 5120116, (int) this.getDuration() / 1000);
                    monster.getMap().broadcastMessage(MaplePacketCreator.spouseMessage(0x0A, "[系统提示] 注意 " + monster.getStats().getName() + " 即将开启反射物攻和魔攻状态。" + this.getDuration()));
                }
                break;
            case 184: {
                x = (player.getPosition().x << 16) + player.getPosition().y;
                break;
            }
            case 200: //召唤怪物
                if (monster == null) {
                    return;
                }
                for (Integer mobId : getSummons()) {
                    MapleMonster toSpawn;
                    try {
                        toSpawn = MapleLifeFactory.getMonster(GameConstants.getCustomSpawnID(monster.getId(), mobId));
                    } catch (RuntimeException e) { //monster doesn't exist
                        continue;
                    }
                    if (toSpawn == null) {
                        continue;
                    }
                    toSpawn.setPosition(monster.getTruePosition());
                    int ypos = (int) monster.getTruePosition().getY(), xpos = (int) monster.getTruePosition().getX();
                    switch (mobId) {
                        case 8500003: //小黑水雷 Pap bomb high
                            toSpawn.setFh((int) Math.ceil(Math.random() * 19.0));
                            ypos = -590;
                            break;
                        case 8500004: //大黑水雷 Pap bomb
                            //Spawn between -500 and 500 from the monsters X position
                            xpos = (int) (monster.getTruePosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
                            ypos = (int) monster.getTruePosition().getY();
                            break;
                        case 8510100: //嗜血单眼怪 Pianus bomb
                            if (Math.ceil(Math.random() * 5) == 1) {
                                ypos = 78;
                                xpos = (int) (0 + Math.ceil(Math.random() * 5)) + ((Math.ceil(Math.random() * 2) == 1) ? 180 : 0);
                            } else {
                                xpos = (int) (monster.getTruePosition().getX() + Math.ceil(Math.random() * 1000.0) - 500);
                            }
                            break;
                        case 8820007: //比恩宝宝 mini bean
                        case 8820107: //混沌比恩宝宝
                            continue;
                    }
                    // Get spawn coordinates (This fixes monster lock)
                    // TODO get map left and right wall.
                    switch (monster.getMap().getId()) {
                        case 220080001: //玩具城 - 时间塔的本源 Pap map
                            if (xpos < -890) {
                                xpos = (int) (-890 + Math.ceil(Math.random() * 150));
                            } else if (xpos > 230) {
                                xpos = (int) (230 - Math.ceil(Math.random() * 150));
                            }
                            break;
                        case 230040420: //水下世界 - 皮亚奴斯洞穴 Pianus map
                            if (xpos < -239) {
                                xpos = (int) (-239 + Math.ceil(Math.random() * 150));
                            } else if (xpos > 371) {
                                xpos = (int) (371 - Math.ceil(Math.random() * 150));
                            }
                            break;
                    }
                    monster.getMap().spawnMonsterWithEffect(toSpawn, getSpawnEffect(), monster.getMap().calcPointBelow(new Point(xpos, ypos - 1)));
                }
                break;
            case 201: {
                if (monster == null) {
                    return;
                }
                int n6 = 0;
                boolean b3 = false;
                int n7 = 0;
                for (final Integer toSummon : this.getSummons()) {
                    MapleMonster monster2;
                    try {
                        monster2 = MapleLifeFactory.getMonster(GameConstants.getCustomSpawnID(monster.getId(), toSummon));
                    } catch (RuntimeException ex2) {
                        continue;
                    }
                    boolean b4 = true;
                    int n9 = (int) monster.getTruePosition().getY();
                    int n10 = (int) monster.getTruePosition().getX();
                    int n11 = 0;
                    int n12 = 35;
                    switch (toSummon) {
                        case 8900000:
                        case 8900001:
                        case 8900002: {
                            n11 = 1;
                            n12 = 100;
                            b3 = true;
                            if (b4 & n7 == 0) {
                                monster2.handleDeadBound(2);
                                n7 = 1;
                                break;
                            }
                            break;
                        }
                        case 8920000:
                        case 8920001:
                        case 8920002:
                        case 8920003:
                        case 8920100:
                        case 8920101:
                        case 8920102:
                        case 8920103: {
                            n11 = 1;
                            b3 = true;
                            b4 = (System.currentTimeMillis() - monster.getChangeTime() > 30000L);
                            break;
                        }
                        case 8900100:
                        case 8900101:
                        case 8900102: {
                            b3 = true;
                            n11 = 1;
                            if (n7 == 0) {
                                monster2.handleDeadBound(2);
                                n7 = 1;
                                break;
                            }
                            break;
                        }
                        case 8950007:
                        case 8950107: {
                            n10 = -404;
                            n9 = -400;
                            monster2.setStance(2);
                            n6 = 1;
                            break;
                        }
                        case 8950003:
                        case 8950103: {
                            n10 = 423;
                            n9 = -400;
                            break;
                        }
                        case 8950004:
                        case 8950104: {
                            n10 = 505;
                            n9 = -230;
                            n6 = 1;
                            break;
                        }
                        case 8950005:
                        case 8950105: {
                            n10 = -514;
                            n9 = -230;
                            monster2.setStance(2);
                            n6 = 1;
                            break;
                        }
                        case 8920004:
                        case 8920005:
                        case 8920104:
                        case 8920105: {
                            if (n10 < -239) {
                                n10 = (int) (-239.0 + Math.ceil(Math.random() * 150.0));
                            } else if (n10 > 371) {
                                n10 = (int) (371.0 - Math.ceil(Math.random() * 150.0));
                            }
                            n6 = 1;
                            break;
                        }
                    }
                    if (monster2 != null) {
                        monster2.setPosition(monster.getTruePosition());
                        if (toSummon != monster.getId() && n6 == 0 && Randomizer.nextInt(100) < n12 && n11 > 0 && b4) {
                            monster2.setChangeTime(System.currentTimeMillis());
                            if (monster.getChangedStats() != null) {
                                monster2.setOverrideStats(monster.getChangedStats());
                            }
                            monster2.setStance(monster.getStance());
                            monster2.setFh(monster.getFh());
                            monster2.setHp(monster.getHp());
                            if (b3) {
                                player.getEventInstance().registerMonster(monster2);
                            }
                            n6 = 1;
                        }
                        if (n6 == 0) {
                            continue;
                        }
                        player.getMap().spawnMonsterWithEffect(monster2, getSpawnEffect(), new Point(n10, n9 - 1));
                    }
                }
                if (n6 != 0 && b3) {
                    player.getMap().removeMonster(monster);
                    player.getEventInstance().unregisterMonster(monster);
                    break;
                }
                break;
            }
            case 223: {
                skill = false;
                final int n13 = 1;
                if (monster.getStati().get(MonsterStatus.MOB_STAT_Laser) != null && monster.getStati().get(MonsterStatus.MOB_STAT_Laser).getX() != 0) {
                    return;
                }
                stats.put(MonsterStatus.MOB_STAT_Laser, n13);
                break;
            }
            case 211:
            case 217:
            case 227: {
                player.send(MobPacket.showMobSkillDelay(monster.getObjectId(), this, effectAfter, new ArrayList()));
                break;
            }
            case 226: {
                monster.isFacingLeft();
                final ArrayList<Rectangle> list2 = new ArrayList<>();
                for (int i = 0; i < 3; ++i) {
                    final int av = Randomizer.rand(170, 250);
                    list2.add(calculateBoundingBox(new Point(monster.getTruePosition().x + (monster.isFacingLeft() ? (-av) : av) * (2 * i - 1), monster.getTruePosition().y), monster.isFacingLeft()));
                }
                player.getMap().broadcastMessage(MobPacket.showMobSkillDelay(monster.getObjectId(), this, effectAfter, list2));
                break;
            }
            case 230: {
                ArrayList<Rectangle> rectangles = new ArrayList<>();
                final int abs = Math.abs(130);
                for (int j = 0; j < 10; ++j) {
                    final int av2 = Randomizer.rand(abs - 10, abs - 10);
                    rectangles.add(calculateBoundingBox(new Point((j % 2 == 0) ? (-654 + av2 * (j / 2)) : (651 - av2 * (j / 2)), monster.getPosition().y), monster.isFacingLeft()));
                }
                Collections.shuffle(rectangles);
                monster.setRectangles(rectangles);
                player.getMap().broadcastMessage(MobPacket.showMobSkillDelay(monster.getObjectId(), this, skillAfter, rectangles));
                break;
            }
            case 228: {
                if (monster.getStati() == null || monster.getStati().get(MonsterStatus.MOB_STAT_Laser) == null) {
                    break;
                }
                final int intValue = monster.getStati().get(MonsterStatus.MOB_STAT_Laser).getX();
                if (intValue != 0) {
                    final long currentTimeMillis = System.currentTimeMillis();
                    final double second = (currentTimeMillis - monster.getStati().get(MonsterStatus.MOB_STAT_Laser).getStartTime()) / 1000.0;
                    final boolean firstUse = monster.getStati().get(MonsterStatus.MOB_STAT_Laser).isFirstUse();
                    final int n15 = (int) (second * 9.725508365508366 % 360.0);
                    final int en = monster.getStati().get(MonsterStatus.MOB_STAT_Laser).getAngle();
                    monster.getStati().get(MonsterStatus.MOB_STAT_Laser).setStartTime(currentTimeMillis);
                    monster.getStati().get(MonsterStatus.MOB_STAT_Laser).setAngle(en + (firstUse ? (-n15) : n15));
                    monster.getStati().get(MonsterStatus.MOB_STAT_Laser).setFirstUse(!firstUse);
                    player.send_other(MobPacket.controlLaser(monster.getObjectId(), monster.getStati().get(MonsterStatus.MOB_STAT_Laser).getAngle(), intValue, monster.getStati().get(MonsterStatus.MOB_STAT_Laser).isFirstUse()), true);
                    break;
                }
                break;
            }
            case 170: {
                if (skillLevel == 42) {
                    final Map<Integer, Point> hashMap = new HashMap<>();
                    for (int k = 0; k < 3; ++k) {
                        hashMap.put(k, new Point(monster.getPosition().x + k * (monster.isFacingLeft() ? -250 : 250), monster.getPosition().y));
                    }
                    player.getMap().broadcastMessage(MobPacket.a(monster.getObjectId(), 1, hashMap, monster.isFacingLeft()));
                    break;
                }
                if (skillLevel > 44 && skillLevel <= 47) {
                    final Point point = new Point(monster.getPosition().x + (monster.isFacingLeft() ? -600 : 600), monster.getPosition().y);
                    player.getMap().broadcastMessage(MobPacket.teleportMonster(monster.getObjectId(), false, 10, point, 0));
                    player.getMap().broadcastMessage(MobPacket.a(monster.getObjectId(), skillLevel, 1, 1, point, monster.isFacingLeft()));
                    break;
                }
                if (skillLevel == 44) {
                    player.getMap().broadcastMessage(MobPacket.teleportMonster(monster.getObjectId(), false, 4, null, player.getMap().getFootholds().getAllRelevants().get(Randomizer.nextInt(player.getMap().getFootholds().getAllRelevants().size())).getId()));
                    break;
                }
                break;
            }
            case 176:
            case 214: {
                player.getMap().broadcastMessage(MobPacket.showMobSkillDelay(monster.getObjectId(), this, effectAfter + 100, new ArrayList()));
                if (monster.getId() / 100 == 88000 || monster.getId() / 100 == 88001) {
                    player.getMap().broadcastMessage(MobPacket.showMonsterSpecialSkill(monster.getObjectId(), (skillLevel == 24) ? 2 : 0));
                }
                if (monster.getId() == 8881000) {
                    player.getMap().broadcastMessage(MobPacket.cancelMonsterAction(monster, 7));
                    break;
                }
                break;
            }
            default:
                if (disease == null && ServerConstants.isShowPacket()) {
                    player.dropMessage(5, "未处理的怪物技能 skillid : " + skillId);
                }
                break;
        }
        if (stats.size() > 0 && monster != null) {
            if (lt != null && rb != null && skill) {
                for (MapleMapObject mons : getObjectsInRange(monster, MapleMapObjectType.MONSTER)) {
                    ((MapleMonster) mons).applyMonsterBuff(stats, getSkillId(), getDuration(), this, reflection);
                }
            } else {
                monster.applyMonsterBuff(stats, getSkillId(), getDuration(), this, reflection);
            }
        }
        if (disease != null && player != null) {
            if (lt != null && rb != null && skill && monster != null) {
                for (MapleCharacter chr : getPlayersInRange(monster, player)) {
                    chr.giveDebuff(disease, this);
                }
            } else {
                player.giveDebuff(disease, this);
            }
        }
        if (monster != null) {
            monster.setMp(monster.getMp() - getMpCon());
        }
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getMpCon() {
        return mpCon;
    }

    public void setMpCon(int mpCon) {
        this.mpCon = mpCon;
    }

    public List<Integer> getSummons() {
        return Collections.unmodifiableList(toSummon);
    }

    /*
     * public short getEffectDelay() {
     * return effect_delay;
     * }
     */
    public int getSpawnEffect() {
        return spawnEffect;
    }

    /*
     * public void setEffectDelay(short effect_delay) {
     * this.effect_delay = effect_delay;
     * }
     */
    public void setSpawnEffect(int spawnEffect) {
        this.spawnEffect = spawnEffect;
    }

    public int getHP() {
        return hp;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getCoolTime() {
        return cooltime;
    }

    public void setCoolTime(long cooltime) {
        this.cooltime = cooltime;
    }

    public Point getLt() {
        return lt;
    }

    public Point getRb() {
        return rb;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(short limit) {
        this.limit = limit;
    }

    public boolean makeChanceResult() {
        return prop >= 1.0 || Math.random() < prop;
    }

    public int getAreaSequenceDelay() {
        return areaSequenceDelay;
    }

    public void setAreaSequenceDelay(int areaSequenceDelay) {
        this.areaSequenceDelay = areaSequenceDelay;
    }

    public int getSkillAfter() {
        return skillAfter;
    }

    public void setSkillAfter(int skillAfter) {
        this.skillAfter = skillAfter;
    }

    public int getForce() {
        return force;
    }

    public void setForce(int force) {
        this.force = force;
    }

    public int getForcex() {
        return forcex;
    }

    public void setForcex(int forcex) {
        this.forcex = forcex;
    }

    public Rectangle calculateBoundingBox(Point posFrom, boolean facingLeft) {
        Point mylt, myrb;
        if (facingLeft) {
            mylt = new Point(lt.x + posFrom.x, lt.y + posFrom.y);
            myrb = new Point(rb.x + posFrom.x, rb.y + posFrom.y);
        } else {
            myrb = new Point(lt.x * -1 + posFrom.x, rb.y + posFrom.y);
            mylt = new Point(rb.x * -1 + posFrom.x, lt.y + posFrom.y);
        }
        return new Rectangle(mylt.x, mylt.y, myrb.x - mylt.x, myrb.y - mylt.y);
    }

    private List<MapleCharacter> getPlayersInRange(MapleMonster monster, MapleCharacter player) {
        Rectangle bounds = calculateBoundingBox(monster.getTruePosition(), monster.isFacingLeft());
        List<MapleCharacter> players = new ArrayList<>();
        players.add(player);
        return monster.getMap().getPlayersInRectAndInList(bounds, players);
    }

    private List<MapleMapObject> getObjectsInRange(MapleMonster monster, MapleMapObjectType objectType) {
        Rectangle bounds = calculateBoundingBox(monster.getTruePosition(), monster.isFacingLeft());
        List<MapleMapObjectType> objectTypes = new ArrayList<>();
        objectTypes.add(objectType);
        return monster.getMap().getMapObjectsInRect(bounds, objectTypes);
    }
}
