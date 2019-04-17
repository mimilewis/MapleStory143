/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.quest;

import client.MapleClient;
import constants.ScriptType;
import handling.opcode.EffectOpcode;
import scripting.npc.NPCConversationManager;
import server.quest.MapleQuest;
import tools.packet.EffectPacket;

import javax.script.Invocable;
import java.awt.*;

/**
 * @author PlayDK
 */
public class QuestActionManager extends NPCConversationManager {

    private final int quest;
    private final boolean start;
    private final ScriptType type;

    public QuestActionManager(MapleClient c, int npc, int quest, boolean start, ScriptType type, Invocable iv) {
        super(c, npc, String.valueOf(quest), type, iv);
        this.quest = quest;
        this.start = start;
        this.type = type;
    }

    public int getQuest() {
        return quest;
    }

    public boolean isStart() {
        return start;
    }

    @Override
    public void dispose() {
        QuestScriptManager.getInstance().dispose(this, getClient());
    }

    public void forceStartQuest() {
        MapleQuest.getInstance(quest).forceStart(getPlayer(), getNpc(), null);
    }

    public void forceStartQuest(String customData) {
        MapleQuest.getInstance(quest).forceStart(getPlayer(), getNpc(), customData);
    }

    public void forceCompleteQuest() {
        MapleQuest.getInstance(quest).forceComplete(getPlayer(), getNpc());
    }

    public String getQuestCustomData() {
        return getPlayer().getQuestNAdd(MapleQuest.getInstance(quest)).getCustomData();
    }

    public void setQuestCustomData(String customData) {
        getPlayer().getQuestNAdd(MapleQuest.getInstance(quest)).setCustomData(customData);
    }

    public void showCompleteQuestEffect() {
        getPlayer().getClient().announce(EffectPacket.showSpecialEffect(EffectOpcode.UserEffect_QuestComplete.getValue())); // 任务完成
        getPlayer().getMap().broadcastMessage(getPlayer(), EffectPacket.showForeignEffect(getPlayer().getId(), EffectOpcode.UserEffect_QuestComplete.getValue()), false);
    }

    public final void spawnNpcForPlayer(int npcId, int x, int y) {
        getMap().spawnNpcForPlayer(getClient(), npcId, new Point(x, y));
    }
}
