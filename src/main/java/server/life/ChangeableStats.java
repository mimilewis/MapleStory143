package server.life;

import constants.GameConstants;

public class ChangeableStats extends OverrideMonsterStats {

    public final int watk;
    public final int matk;
    public final int acc;
    public final int eva;
    public final int PDRate;
    public final int MDRate;
    public final int pushed;
    public final int speed;
    public final int level;

    public ChangeableStats(MapleMonsterStats stats, OverrideMonsterStats ostats) {
        hp = ostats.getHp();
        exp = ostats.getExp();
        mp = ostats.getMp();
        watk = stats.getPhysicalAttack();
        matk = stats.getMagicAttack();
        acc = stats.getAcc();
        eva = stats.getEva();
        PDRate = stats.getPDRate();
        MDRate = stats.getMDRate();
        pushed = stats.getPushed();
        speed = stats.getSpeed();
        level = stats.getLevel();
    }

    public ChangeableStats(MapleMonsterStats stats, int newLevel, boolean pqMob) {
        double mod = Math.max(newLevel / stats.getLevel(), 1);
//        double hpRatio = (double) stats.getHp() / (double) stats.getExp();
        double pqMod = (pqMob ? 2.5 : 1.0);
        hp = Math.round((!stats.isBoss() ? GameConstants.getMonsterHP(newLevel) : (stats.getHp() * mod)) * pqMod);
        exp = (int) Math.round(stats.getExp() * mod * pqMod * 0.8);
        mp = (int) Math.round(stats.getMp() * mod * pqMod);
        watk = (int) Math.round(stats.getPhysicalAttack() * mod);
        matk = (int) Math.round(stats.getMagicAttack() * mod);
        acc = Math.round(stats.getAcc() + Math.max(0, newLevel - stats.getLevel()) * 2);
        eva = Math.round(stats.getEva() + Math.max(0, newLevel - stats.getLevel()));
        PDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getPDRate() * mod));
        MDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getMDRate() * mod));
        pushed = (int) Math.round(stats.getPushed() * mod);
        speed = (int) Math.round(stats.getSpeed() * mod);
        level = newLevel > 250 ? 250 : newLevel;
    }

    public ChangeableStats(MapleMonsterStats stats, int newLevel, int multipler) {
        double base = Math.max(newLevel / stats.getLevel(), 1) * multipler;
        hp = Math.round(stats.getHp() * base * (!stats.isBoss() ? 1.0 : 3.0));
        mp = (int) Math.round(stats.getMp() * base);
        exp = (int) (stats.getExp() * base);
        watk = (int) Math.round(stats.getPhysicalAttack() * base);
        matk = (int) Math.round(stats.getMagicAttack() * base);
        acc = (int) Math.round(stats.getAcc() + Math.max(0, newLevel - stats.getLevel()) * base);
        eva = (int) Math.round(stats.getEva() + Math.max(0, newLevel - stats.getLevel()) * base);
        PDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getPDRate() * base));
        MDRate = Math.min(stats.isBoss() ? 30 : 20, (int) Math.round(stats.getMDRate() * base));
        pushed = (int) Math.round(stats.getPushed() * base);
        speed = (int) Math.round(stats.getSpeed() * base);
        level = newLevel > 250 ? 250 : newLevel;
    }
}
