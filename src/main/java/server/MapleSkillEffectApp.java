/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.skills.SkillFactory;
import tools.Pair;

import java.util.List;

public class MapleSkillEffectApp {

    /**
     * 应用BUFF状态。
     *
     * @param effect
     * @param applyfrom
     * @param applyto
     * @param primary
     * @param effectEventData
     */
    public static void applyBuffEffect(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, effectEventData effectEventData) {
        applyBuffEffect(effect, applyfrom, applyto, primary, effect.getSourceid(), effectEventData);
    }

    /**
     * @param effect
     * @param applyfrom
     * @param applyto
     * @param primary
     * @param sourceid
     * @param effectEventData
     */
    public static void applyBuffEffect(MapleStatEffect effect, final MapleCharacter applyfrom, final MapleCharacter applyto, boolean primary, int sourceid, effectEventData effectEventData) {
        int skilllevel = applyto.getSkillLevel(sourceid);
        if (skilllevel > 0) {
            effectEventData.duration = SkillFactory.getSkill(sourceid).getEffect(skilllevel).getDuration();
        }
    }

    public static class effectEventData {

        protected final List<Pair<MapleBuffStat, Integer>> statups;
        protected int duration;

        public effectEventData(List<Pair<MapleBuffStat, Integer>> statups) {
            this.statups = statups;
        }

        public effectEventData(List<Pair<MapleBuffStat, Integer>> statups, int duration) {
            this.statups = statups;
            this.duration = duration;
        }

        public List<Pair<MapleBuffStat, Integer>> getStatups() {
            return statups;
        }

        public int getDuration() {
            return duration;
        }
    }
}
