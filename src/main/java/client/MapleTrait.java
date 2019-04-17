package client;

import configs.ServerConfig;
import constants.GameConstants;
import tools.MaplePacketCreator;

public class MapleTrait {

    private MapleTraitType type;
    private int totalExp = 0, localTotalExp = 0;
    private short exp = 0;
    private byte level = 0;

    public MapleTrait() {

    }

    public MapleTrait(MapleTraitType traitTyp) {
        this.type = traitTyp;
    }

    public void setExp(int exp, MapleCharacter chr) {
        if (exp >= 0) {
            this.totalExp = exp;
            this.localTotalExp = exp;
            chr.updateSingleStat(type.getStat(), totalExp);
            chr.send(MaplePacketCreator.showTraitGain(type, exp));
            recalcLevel();
        }
    }

    public void addExp(int exp) {
        this.totalExp += exp;
        this.localTotalExp += exp;
        if (exp != 0) {
            recalcLevel();
        }
    }

    public void addExp(int exp, MapleCharacter chr) {
        addTrueExp(exp * ServerConfig.CHANNEL_RATE_TRAIT, chr);
    }

    public void addTrueExp(int exp, MapleCharacter chr) {
        if (exp != 0) {
            this.totalExp += exp;
            this.localTotalExp += exp;
            chr.updateSingleStat(type.getStat(), totalExp);
            chr.send(MaplePacketCreator.showTraitGain(type, exp));
            recalcLevel();
        }
    }

    public boolean recalcLevel() {
        if (totalExp < 0) {
            totalExp = 0;
            localTotalExp = 0;
            level = 0;
            exp = 0;
            return false;
        }
        int oldLevel = level;
        for (byte i = 0; i < 100; i++) {
            if (GameConstants.getTraitExpNeededForLevel(i) > localTotalExp) {
                exp = (short) (GameConstants.getTraitExpNeededForLevel(i) - localTotalExp);
                level = (byte) (i - 1);
                return level > oldLevel;
            }
        }
        exp = 0;
        level = 100;
        totalExp = GameConstants.getTraitExpNeededForLevel(level);
        localTotalExp = totalExp;
        return level > oldLevel;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int newLevel, MapleCharacter chr) {
        if (newLevel <= level) {
            return;
        }
        totalExp = GameConstants.getTraitExpNeededForLevel(newLevel);
        localTotalExp = totalExp;
        recalcLevel();
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.totalExp = exp;
        this.localTotalExp = exp;
        recalcLevel();
    }

    public int getTotalExp() {
        return totalExp;
    }

    public int getLocalTotalExp() {
        return localTotalExp;
    }

    public void addLocalExp(int exp) {
        this.localTotalExp += exp;
    }

    public void clearLocalExp() {
        this.localTotalExp = totalExp;
    }

    public MapleTraitType getType() {
        return type;
    }
}
