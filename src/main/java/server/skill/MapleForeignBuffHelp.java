/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.skill;

import client.MapleBuffStat;
import server.MapleStatEffect;

public class MapleForeignBuffHelp {

    public static MapleForeignBuffSkill getForeignBuffSkill(MapleStatEffect effect) {
        MapleForeignBuffSkill skill = new MapleForeignBuffSkill(effect);
        addForeignBuffStat(skill);
        return skill;
    }

    private static void addForeignBuffStat(MapleForeignBuffSkill skill) {
        switch (skill.getEffect().getSourceid()) {
            case 2003516:
                skill.getStats().add(new MapleForeignBuffByteStat(MapleBuffStat.移动速度));
                skill.getStats().add(new MapleForeignBuffShortStat(MapleBuffStat.巨人药水));
                break;
            case 61120008:
                skill.getStats().add(new MapleForeignBuffByteStat(MapleBuffStat.移动速度));
                skill.getStats().add(new MapleForeignBuffShortStat(MapleBuffStat.变身效果));
                break;
        }
        if (skill.getEffect().isMonsterBuff()) {
            skill.getStats().add(new MapleForeignBuffNoStat(MapleBuffStat.骑兽技能));
        }
    }
}
