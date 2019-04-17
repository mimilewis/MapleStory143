/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author PlayDK
 */
public class PlayerSpecialStats implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    private final AtomicInteger forceCounter = new AtomicInteger(); //恶魔相关
    private final HashMap<Integer, Integer> forceCounters = new HashMap<>();// 恶魔精气恢复
    private int cardStack; //幻影卡片
    private int morphCount; //狂龙变形值
    private long lastMorphLostTime; //狂龙变形值减少的时间
    private int powerCount; //尖兵电力也就是能量
    private long lastRecoveryPowerTime; //尖兵能量恢复时间检测
    private boolean energyfull; //拳手能量检测是否满能量
    private int darkLight;
    private int trackFlames;
    private int arrowsMode; //三彩箭矢 模式
    private int deathPactCount; // 死亡契约
    private int moonCycle;
    private int jianqi;
    private int pp; //超能力者MP
    private int aranCombo; //战神连击点数
    private transient int cylinder;
    private transient int bullet;
    private transient int hurtHP;

    public void resetSpecialStats() {
        forceCounter.set(0);
        this.forceCounters.clear();
        this.cardStack = 0;
        this.morphCount = 0;
        this.lastMorphLostTime = System.currentTimeMillis();
        this.powerCount = 0;
        this.lastRecoveryPowerTime = System.currentTimeMillis();
        this.energyfull = false;
        this.trackFlames = 1;
        this.deathPactCount = 0;
        this.moonCycle = 0;
        this.jianqi = 0;
        this.pp = 0;
        this.aranCombo = 0;
        this.cylinder = 0;
        this.bullet = 0;
    }

    /**
     * 恶魔相关
     */
    public int getForceCounter() {
        return forceCounter.get();
    }

    public void setForceCounter(int amount) {
        this.forceCounter.set(amount);
    }

    public void gainForceCounter() {
        this.forceCounter.incrementAndGet();
    }

    public void gainForceCounter(int amount) {
        this.forceCounter.addAndGet(amount);
    }

    public void addForceCounter(int df) {
        forceCounters.put(forceCounter.incrementAndGet(), df);
    }

    public int removeForceCounter(int oid) {
        if (forceCounters.containsKey(oid)) {
            return forceCounters.remove(oid);
        }
        return 0;
    }

    /**
     * 幻影卡片系统
     */
    public int getCardStack() {
        if (cardStack < 0) {
            cardStack = 0;
        }
        return cardStack;
    }

    public void setCardStack(int amount) {
        this.cardStack = amount;
    }

    public void gainCardStack() {
        this.cardStack++;
    }

    /*
     * 狂龙变形值
     */
    public int getMorphCount() {
        if (morphCount < 0) {
            morphCount = 0;
        }
        return morphCount;
    }

    public void setMorphCount(int amount) {
        this.morphCount = amount;
    }

    public void gainMorphCount() {
        this.morphCount++;
    }

    public void gainMorphCount(int amount) {
        this.morphCount += amount;
    }

    public long getLastMorphLostTime() {
        if (lastMorphLostTime <= 0) {
            lastMorphLostTime = System.currentTimeMillis();
        }
        return lastMorphLostTime;
    }

    public void prepareMorphLostTime() {
        this.lastMorphLostTime = System.currentTimeMillis();
    }

    /*
     * 尖兵能量
     */
    public int getPowerCount() {
        if (powerCount < 0) {
            powerCount = 0;
        }
        return powerCount;
    }

    public void setPowerCount(int amount) {
        this.powerCount = amount;
    }

    public int getDeathPactCount() {
        if (deathPactCount < 0) {
            deathPactCount = 0;
        }
        return deathPactCount;
    }

    public void setDeathPactCount(int amount) {
        this.deathPactCount = amount;
    }

    public void gainDeathPactCount(int count) {
        this.deathPactCount += count;
    }

    public long getLastRecoveryPowerTime() {
        if (lastRecoveryPowerTime <= 0) {
            lastRecoveryPowerTime = System.currentTimeMillis();
        }
        return lastRecoveryPowerTime;
    }

    public void prepareRecoveryPowerTime() {
        this.lastRecoveryPowerTime = System.currentTimeMillis();
    }

    /*
     * 拳手能量获得是否满
     */
    public boolean isEnergyFull() {
        return energyfull;
    }

    public void changeEnergyfull(boolean full) {
        this.energyfull = full;
    }

    /**
     * 处理夜光的 光明和黑暗
     */
    public int getDarkLight() {
        return darkLight;
    }

    public void setDarkLight(int darkLight) {
        this.darkLight = darkLight;
    }

    public void gainTrackFlmes() {
        this.trackFlames++;
    }

    public int getTrackFlmes() {
        return trackFlames;
    }

    public void setTrackFlmes(int amount) {
        this.trackFlames = amount;
    }

    /*
     * 获取当前 三彩箭矢 的模式
     */
    public int getArrowsMode() {
        return arrowsMode;
    }

    /*
     * 设置 三彩箭矢 的模式
     */
    public void setArrowsMode(int mode) {
        this.arrowsMode = mode;
    }

    public int getMoonCycle() {
        moonCycle++;
        if (moonCycle > 1) {
            moonCycle = 0;
        }
        return moonCycle;
    }

    public void gainJianQi(int mode) {
        this.jianqi = Math.min(1000, jianqi + (mode == 1 ? 5 : 2));
    }

    public int getJianQi() {
        return jianqi;
    }

    public void setJianQi(int jianqi) {
        this.jianqi = Math.min(1000, jianqi);
    }

    public void gainPP(int pp) {
        this.pp = Math.min(30, Math.max(0, this.pp + pp));
    }

    public int getPP() {
        return pp;
    }

    public void setPP(int pp) {
        this.pp = Math.min(30, pp);
    }

    public int getAranCombo() {
        return aranCombo;
    }

    public void setAranCombo(int aranCombo) {
        this.aranCombo = aranCombo;
    }

    public int getCylinder() {
        return cylinder;
    }

    public void setCylinder(int cylinder) {
        this.cylinder = cylinder;
    }

    public int getBullet() {
        return bullet;
    }

    public void setBullet(int bullet) {
        this.bullet = bullet;
    }

    public int getHurtHP() {
        return hurtHP;
    }

    public void setHurtHP(int hurtHP) {
        this.hurtHP = hurtHP;
    }
}
