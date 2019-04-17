/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import constants.GameConstants;
import handling.world.WorldBroadcastService;
import handling.world.WorldFindService;
import scripting.lieDetector.LieDetectorScript;
import server.Timer.EtcTimer;
import server.maps.MapleMap;
import server.quest.MapleQuest;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 * @author AuroX
 */
public class MapleLieDetector {

    public final int chrid;
    public byte type; // 0 = Normal, 1 = Admin Macro (Manager Skill) 
    public int attempt;
    public String tester, answer;
    public boolean inProgress, passed;

    @JsonCreator
    public MapleLieDetector(@JsonProperty("chrid") final int chrid) {
        this.chrid = chrid;
        reset();
    }

    public final boolean startLieDetector(final String tester, final boolean isItem, final boolean anotherAttempt) {
        if (!anotherAttempt && ((isPassed() && isItem) || inProgress() || attempt == 3/*
                 * || answer != null || tester != null
                 */)) {
            return false;
        }
        final Pair<String, String> captcha = LieDetectorScript.getImageBytes();
        if (captcha == null) {
            return false;
        }
        final byte[] image = HexTool.getByteArrayFromHexString(captcha.getLeft());
        this.answer = captcha.getRight();
        this.tester = tester;
        this.inProgress = true;
        this.type = (byte) (isItem ? 0 : 1);
        this.attempt++;

        final MapleCharacter chr = WorldFindService.getInstance().findCharacterById(chrid);
        if (chr == null) {
            return false;
        }
        chr.send(MaplePacketCreator.sendLieDetector(image, attempt));
        EtcTimer.getInstance().schedule(() -> {
            if (!isPassed()) {
                if (attempt >= 3) {
                    MapleCharacter search_chr = chr.getMap().getCharacterByName(tester);
                    if (search_chr != null && search_chr.getId() != chr.getId()) {
                        search_chr.dropMessage(5, chr.getName() + " 没用通过测谎仪的检测，恭喜你获得7000的金币.");
                        search_chr.gainMeso(7000, true);
                    }
                    end();
                    chr.send(MaplePacketCreator.LieDetectorResponse((byte) 0x0A, (byte) 4));
                    MapleMap map = chr.getClient().getChannelServer().getMapFactory().getMap(GameConstants.JAIL);
                    chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.JAIL_QUEST)).setCustomData(String.valueOf(30 * 60));
                    chr.changeMap(map, map.getPortal(0));
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + chr.getName() + " (等级 " + chr.getLevel() + ") 未通过测谎仪检测，系统将其监禁30分钟！"));
                } else { // can have another attempt
                    startLieDetector(tester, isItem, true);
                }
            }
        }, 60 * 1000); // 60 秒
        return true;
    }

    public final int getAttempt() {
        return attempt;
    }

    public final byte getLastType() {
        return type;
    }

    public final String getTester() {
        return tester;
    }

    public final String getAnswer() {
        return answer;
    }

    public final boolean inProgress() {
        return inProgress;
    }

    public final boolean isPassed() {
        return passed;
    }

    public final void end() {
        this.inProgress = false;
        this.passed = true;
        this.attempt = 0;
    }

    public final void reset() { // called when change map, cc, reenter cs, or login 
        this.tester = "";
        this.answer = "";
        this.attempt = 0;
        this.inProgress = false;
        this.passed = false;
    }
}
