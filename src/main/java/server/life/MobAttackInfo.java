package server.life;

import java.awt.*;

public class MobAttackInfo {

    public int PADamage, MADamage, attackAfter, range = 0;
    public Point lt = null, rb = null;
    public boolean magic = false, isElement = false;
    private boolean isDeadlyAttack;
    private int mpBurn, mpCon;
    private int diseaseSkill, diseaseLevel;

    public boolean isDeadlyAttack() {
        return isDeadlyAttack;
    }

    public void setDeadlyAttack(boolean isDeadlyAttack) {
        this.isDeadlyAttack = isDeadlyAttack;
    }

    public int getMpBurn() {
        return mpBurn;
    }

    public void setMpBurn(int mpBurn) {
        this.mpBurn = mpBurn;
    }

    public int getDiseaseSkill() {
        return diseaseSkill;
    }

    public void setDiseaseSkill(int diseaseSkill) {
        this.diseaseSkill = diseaseSkill;
    }

    public int getDiseaseLevel() {
        return diseaseLevel;
    }

    public void setDiseaseLevel(int diseaseLevel) {
        this.diseaseLevel = diseaseLevel;
    }

    public int getMpCon() {
        return mpCon;
    }

    public void setMpCon(int mpCon) {
        this.mpCon = mpCon;
    }

    public int getRange() {
        int maxX = Math.max(Math.abs(lt == null ? 0 : lt.x), Math.abs(rb == null ? 0 : rb.x));
        int maxY = Math.max(Math.abs(lt == null ? 0 : lt.y), Math.abs(rb == null ? 0 : rb.y));
        return Math.max((maxX * maxX) + (maxY * maxY), range);
    }
}
