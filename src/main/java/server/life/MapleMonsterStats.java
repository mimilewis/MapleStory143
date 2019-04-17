package server.life;

import constants.GameConstants;
import server.life.MapleLifeFactory.loseItem;
import tools.Pair;
import tools.Triple;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MapleMonsterStats {

    private final int id;
    private final EnumMap<Element, ElementalEffectiveness> resistance = new EnumMap<>(Element.class);
    private final Map<String, Integer> animationTimes = new HashMap<>();
    private final List<Triple<Integer, Integer, Integer>> skills = new ArrayList<>();
    private final List<MobAttackInfo> mai = new ArrayList<>();
    private byte cp, selfDestruction_action, tagColor, tagBgColor, rareItemDropLevel, HPDisplayType, summonType, category;
    private short level, charismaEXP;
    private long hp;
    private int exp;
    private int mp;
    private int removeAfter;
    private int buffToGive;
    private int fixedDamage;
    private int selfDestruction_hp;
    private int dropItemPeriod;
    private int point;
    private int eva;
    private int acc;
    private int PhysicalAttack;
    private int MagicAttack;
    private int speed;
    private int partyBonusR;
    private int pushed;
    private int link;
    private int weaponPoint;
    private int PDRate;
    private int MDRate;
    private int smartPhase;
    private boolean boss, undead, ffaLoot, firstAttack, isExplosiveReward, mobile, fly, onlyNormalAttack, friendly, noDoom, invincible, partyBonusMob, changeable, escort, removeOnMiss, skeleton;
    private String name, mobType, hitParts;
    private List<Integer> revives = new ArrayList<>();
    private List<Pair<Point, Point>> mobZone = new ArrayList<>();
    private Pair<Integer, Integer> cool = null;
    private List<loseItem> loseItem = null;
    private BanishInfo banish;
    private TransMobs transMobs;

    public MapleMonsterStats(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public long getHp() {
        return hp;
    }

    public void setHp(long hp) {
        this.hp = hp;
    }

    public int getMp() {
        return mp;
    }

    public void setMp(int mp) {
        this.mp = mp;
    }

    public short getLevel() {
        return level;
    }

    public void setLevel(short level) {
        this.level = level;
    }

    public int getWeaponPoint() {
        return weaponPoint;
    }

    public void setWeaponPoint(int wp) {
        this.weaponPoint = wp;
    }

    public short getCharismaEXP() {
        return charismaEXP;
    }

    public void setCharismaEXP(short leve) {
        this.charismaEXP = leve;
    }

    public byte getSelfD() {
        return selfDestruction_action;
    }

    public void setSelfD(byte selfDestruction_action) {
        this.selfDestruction_action = selfDestruction_action;
    }

    public void setSelfDHP(int selfDestruction_hp) {
        this.selfDestruction_hp = selfDestruction_hp;
    }

    public int getSelfDHp() {
        return selfDestruction_hp;
    }

    public int getFixedDamage() {
        return fixedDamage;
    }

    public void setFixedDamage(int damage) {
        this.fixedDamage = damage;
    }

    public int getPushed() {
        return pushed;
    }

    public void setPushed(int damage) {
        this.pushed = damage;
    }

    public int getPhysicalAttack() {
        return PhysicalAttack;
    }

    public void setPhysicalAttack(int PhysicalAttack) {
        this.PhysicalAttack = PhysicalAttack;
    }

    public int getMagicAttack() {
        return MagicAttack;
    }

    public void setMagicAttack(int MagicAttack) {
        this.MagicAttack = MagicAttack;
    }

    public int getEva() {
        return eva;
    }

    public void setEva(int eva) {
        this.eva = eva;
    }

    public int getAcc() {
        return acc;
    }

    public void setAcc(int acc) {
        this.acc = acc;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getPartyBonusRate() {
        return partyBonusR;
    }

    public void setPartyBonusRate(int speed) {
        this.partyBonusR = speed;
    }

    public void setOnlyNormalAttack(boolean onlyNormalAttack) {
        this.onlyNormalAttack = onlyNormalAttack;
    }

    public boolean getOnlyNoramlAttack() {
        return onlyNormalAttack;
    }

    public BanishInfo getBanishInfo() {
        return banish;
    }

    public void setBanishInfo(BanishInfo banish) {
        this.banish = banish;
    }

    public int getRemoveAfter() {
        return removeAfter;
    }

    public void setRemoveAfter(int removeAfter) {
        this.removeAfter = removeAfter;
    }

    public byte getrareItemDropLevel() {
        return rareItemDropLevel;
    }

    public void setrareItemDropLevel(byte rareItemDropLevel) {
        this.rareItemDropLevel = rareItemDropLevel;
    }

    public boolean isBoss() {
        return boss;
    }

    public void setBoss(boolean boss) {
        this.boss = boss;
    }

    public boolean isFfaLoot() {
        return ffaLoot;
    }

    public void setFfaLoot(boolean ffaLoot) {
        this.ffaLoot = ffaLoot;
    }

    public boolean isEscort() {
        return escort;
    }

    public void setEscort(boolean ffaL) {
        this.escort = ffaL;
    }

    public boolean isExplosiveReward() {
        return isExplosiveReward;
    }

    public void setExplosiveReward(boolean isExplosiveReward) {
        this.isExplosiveReward = isExplosiveReward;
    }

    public void setAnimationTime(String name, int delay) {
        animationTimes.put(name, delay);
    }

    public int getAnimationTime(String name) {
        Integer ret = animationTimes.get(name);
        if (ret == null) {
            return 500;
        }
        return ret;
    }

    public boolean isMobile() {
        return animationTimes.containsKey("move") || animationTimes.containsKey("fly");
    }

    public boolean isFly() {
        return fly;
    }

    public void setFly(boolean fly) {
        this.fly = fly;
    }

    public List<Integer> getRevives() {
        return revives;
    }

    public void setRevives(List<Integer> revives) {
        this.revives = revives;
    }

    public boolean getUndead() {
        return undead;
    }

    public void setUndead(boolean undead) {
        this.undead = undead;
    }

    public byte getSummonType() {
        return summonType;
    }

    public void setSummonType(byte selfDestruction) {
        this.summonType = selfDestruction;
    }

    public byte getCategory() {
        return category;
    }

    public void setCategory(byte selfDestruction) {
        this.category = selfDestruction;
    }

    public int getPDRate() {
        return PDRate;
    }

    public void setPDRate(int selfDestruction) {
        this.PDRate = selfDestruction;
    }

    public int getMDRate() {
        return MDRate;
    }

    public void setMDRate(int selfDestruction) {
        this.MDRate = selfDestruction;
    }

    public EnumMap<Element, ElementalEffectiveness> getElements() {
        return resistance;
    }

    public void setEffectiveness(Element e, ElementalEffectiveness ee) {
        resistance.put(e, ee);
    }

    public void removeEffectiveness(Element e) {
        resistance.remove(e);
    }

    public ElementalEffectiveness getEffectiveness(Element e) {
        ElementalEffectiveness elementalEffectiveness = resistance.get(e);
        if (elementalEffectiveness == null) {
            return ElementalEffectiveness.正常;
        } else {
            return elementalEffectiveness;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return mobType;
    }

    public void setType(String mobt) {
        this.mobType = mobt;
    }

    public String getHitParts() {
        return hitParts;
    }

    public void setHitParts(String hitParts) {
        this.hitParts = hitParts;
    }

    public byte getTagColor() {
        return tagColor;
    }

    public void setTagColor(int tagColor) {
        this.tagColor = (byte) tagColor;
    }

    public byte getTagBgColor() {
        return tagBgColor;
    }

    public void setTagBgColor(int tagBgColor) {
        this.tagBgColor = (byte) tagBgColor;
    }

    public List<Triple<Integer, Integer, Integer>> getSkills() {
        return Collections.unmodifiableList(this.skills);
    }

    public void setSkills(List<Triple<Integer, Integer, Integer>> skill_) {
        for (Triple<Integer, Integer, Integer> skill : skill_) {
            skills.add(skill);
        }
    }

    public byte getNoSkills() {
        return (byte) skills.size();
    }

    public boolean hasSkill(int skillId, int level) {
        for (Triple<Integer, Integer, Integer> skill : skills) {
            if (skill.getLeft() == skillId && skill.getMid() == level) {
                return true;
            }
        }
        return false;
    }

    public boolean isFirstAttack() {
        return firstAttack;
    }

    public void setFirstAttack(boolean firstAttack) {
        this.firstAttack = firstAttack;
    }

    public byte getCP() {
        return cp;
    }

    public void setCP(byte cp) {
        this.cp = cp;
    }

    public int getPoint() {
        return point;
    }

    public void setPoint(int cp) {
        this.point = cp;
    }

    public boolean isFriendly() {
        return friendly;
    }

    public void setFriendly(boolean friendly) {
        this.friendly = friendly;
    }

    public boolean isInvincible() {
        return invincible;
    }

    public void setInvincible(boolean invin) {
        this.invincible = invin;
    }

    public int getSmartPhase() {
        return smartPhase;
    }

    public void setSmartPhase(int smartPhase) {
        this.smartPhase = smartPhase;
    }

    public void setChange(boolean invin) {
        this.changeable = invin;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public boolean isPartyBonus() {
        return partyBonusMob;
    }

    public void setPartyBonus(boolean invin) {
        this.partyBonusMob = invin;
    }

    public boolean isNoDoom() {
        return noDoom;
    }

    public void setNoDoom(boolean doom) {
        this.noDoom = doom;
    }

    public int getBuffToGive() {
        return buffToGive;
    }

    public void setBuffToGive(int buff) {
        this.buffToGive = buff;
    }

    public int getLink() {
        return link;
    }

    public void setLink(int link) {
        this.link = link;
    }

    public byte getHPDisplayType() {
        return HPDisplayType;
    }

    public void setHPDisplayType(byte HPDisplayType) {
        this.HPDisplayType = HPDisplayType;
    }

    public int getDropItemPeriod() {
        return dropItemPeriod;
    }

    public void setDropItemPeriod(int d) {
        this.dropItemPeriod = d;
    }

    public void setRemoveOnMiss(boolean removeOnMiss) {
        this.removeOnMiss = removeOnMiss;
    }

    public boolean removeOnMiss() {
        return removeOnMiss;
    }

    public Pair<Integer, Integer> getCool() {
        return cool;
    }

    public void setCool(Pair<Integer, Integer> cool) {
        this.cool = cool;
    }

    public List<Pair<Point, Point>> getMobZone() {
        return mobZone;
    }

    public void setMobZone(List<Pair<Point, Point>> mobZone) {
        this.mobZone = mobZone;
    }

    public boolean isSkeleton() {
        return skeleton;
    }

    public void setSkeleton(boolean skeleton) {
        this.skeleton = skeleton;
    }

    public List<loseItem> loseItem() {
        return loseItem;
    }

    public void addLoseItem(loseItem li) {
        if (loseItem == null) {
            loseItem = new LinkedList<>();
        }
        loseItem.add(li);
    }

    public void addMobAttack(MobAttackInfo ma) {
        this.mai.add(ma);
    }

    public MobAttackInfo getMobAttack(int attack) {
        if (attack >= this.mai.size() || attack < 0) {
            return null;
        }
        return this.mai.get(attack);
    }

    public List<MobAttackInfo> getMobAttacks() {
        return this.mai;
    }

    public int dropsMesoCount() {
        if (getRemoveAfter() != 0 || isInvincible() || getOnlyNoramlAttack() || getDropItemPeriod() > 0 || getCP() > 0 || getPoint() > 0 || getFixedDamage() > 0 || getSelfD() != -1 || getPDRate() <= 0 || getMDRate() <= 0) {
            return 0;
        }
        int mobId = getId() / 100000;
        if (GameConstants.getPartyPlayHP(getId()) > 0 || mobId == 97 || mobId == 95 || mobId == 93 || mobId == 91 || mobId == 90) {
            return 0;
        }
        if (isExplosiveReward()) {
            return 7;
        }
        if (isBoss()) {
            return 2;
        }
        return 1;
    }

    public TransMobs getTransMobs() {
        return transMobs;
    }

    public void setTransMobs(TransMobs transMobs) {
        this.transMobs = transMobs;
    }

    public static class TransMobs {
        private List<Integer> mobids;
        private List<Pair<Integer, Integer>> skills;
        private int time;
        private int cooltime;
        private int hpTriggerOn;
        private int hpTriggerOff;
        private int withMob = 0;

        public TransMobs(List<Integer> mobids, List<Pair<Integer, Integer>> skills, int time, int cooltime, int hpTriggerOn, int hpTriggerOff, int withMob) {
            this.mobids = mobids;
            this.skills = skills;
            this.time = time;
            this.cooltime = cooltime;
            this.hpTriggerOn = hpTriggerOn;
            this.hpTriggerOff = hpTriggerOff;
            this.withMob = withMob;
        }

        public List<Integer> getMobids() {
            return mobids;
        }

        public List<Pair<Integer, Integer>> getSkills() {
            return skills;
        }

        public int getTime() {
            return time;
        }

        public int getCooltime() {
            return cooltime;
        }

        public int getHpTriggerOn() {
            return hpTriggerOn;
        }

        public int getHpTriggerOff() {
            return hpTriggerOff;
        }

        public int getWithMob() {
            return withMob;
        }
    }
}
