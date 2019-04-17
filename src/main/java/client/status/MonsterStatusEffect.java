package client.status;

import client.MapleCharacter;
import server.life.MapleMonster;
import server.life.MobSkill;

import java.lang.ref.WeakReference;

public class MonsterStatusEffect {

    private final int skill;
    private final MobSkill mobskill;
    private final boolean monsterSkill;
    private MonsterStatus stati;
    private WeakReference<MapleCharacter> weakChr = null;
    private Integer x;
    private int poisonSchedule = 0;
    private int moboid = 0;
    private boolean reflect = false;
    private long cancelTime = 0, dotTime = 0;
    private int DOTStack = 0;//叠加
    private long startTime = System.currentTimeMillis();
    private boolean firstUse = true;
    private int angle = 0;
    private int count = 1;

    public MonsterStatusEffect(MonsterStatus stat, Integer x, int skillId, MobSkill mobskill, boolean monsterSkill) {
        this.stati = stat;
        this.x = x;
        this.skill = skillId;
        this.mobskill = mobskill;
        this.monsterSkill = monsterSkill;
    }

    public MonsterStatusEffect(MonsterStatus stat, Integer x, int skillId, MobSkill mobskill, boolean monsterSkill, int stack) {
        this.stati = stat;
        this.x = x;
        this.skill = skillId;
        this.mobskill = mobskill;
        this.monsterSkill = monsterSkill;
        DOTStack = stack;
    }

    public MonsterStatusEffect(MonsterStatus stat, Integer x, int skillId, MobSkill mobskill, boolean monsterSkill, boolean reflect) {
        this.stati = stat;
        this.x = x;
        this.skill = skillId;
        this.mobskill = mobskill;
        this.monsterSkill = monsterSkill;
        this.reflect = reflect;
        DOTStack = 0;
    }

    public MonsterStatusEffect(MonsterStatus stat, Integer x, int skillId, MobSkill mobskill, boolean monsterSkill, boolean reflect, int Stack) {
        this.stati = stat;
        this.x = x;
        this.skill = skillId;
        this.mobskill = mobskill;
        this.monsterSkill = monsterSkill;
        this.reflect = reflect;
        DOTStack = Stack;
    }

    public MonsterStatus getStati() {
        return stati;
    }

    public Integer getX() {
        return x * count;
    }

    public void setValue(MonsterStatus status, Integer newVal) {
        stati = status;
        x = newVal;
    }

    public int getSkill() {
        return skill;
    }

    public MobSkill getMobSkill() {
        return mobskill;
    }

    public boolean isMonsterSkill() {
        return monsterSkill;
    }

    /*
     * 获取怪物取消BUFF的时间
     */
    public long getCancelTask() {
        return this.cancelTime;
    }

    /*
     * 设置取消怪物BUFF的时间
     */
    public void setCancelTask(long cancelTask) {
        this.cancelTime = System.currentTimeMillis() + cancelTask;
    }

    public long getDotTime() {
        return this.dotTime;
    }

    public void setDotTime(long duration) {
        this.dotTime = duration;
    }

    public void setPoisonSchedule(int poisonSchedule, MapleCharacter chrr) {
        this.poisonSchedule = poisonSchedule;
        this.weakChr = new WeakReference<>(chrr);
    }

    public int getPoisonSchedule() {
        return this.poisonSchedule;
    }

    public boolean shouldCancel(long now) {
        return (cancelTime > 0 && cancelTime <= now);
    }

    public void cancelTask() {
        cancelTime = 0;
    }

    public boolean isReflect() {
        return reflect;
    }

    public int getFromID() {
        return weakChr == null || weakChr.get() == null ? 0 : weakChr.get().getId();
    }

    public void cancelPoisonSchedule(MapleMonster mm) {
        mm.doPoison(this, weakChr);
        this.poisonSchedule = 0;
        this.weakChr = null;
    }

    public int GetStack() {
        return this.DOTStack;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public boolean isFirstUse() {
        return firstUse;
    }

    public void setFirstUse(boolean firstUse) {
        this.firstUse = firstUse;
    }

    public int getAngle() {
        return angle;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count, boolean gain) {
        this.count = gain ? count : (this.count += count);
    }

    public int getMoboid() {
        return moboid;
    }

    public void setMoboid(int moboid) {
        this.moboid = moboid;
    }
}
