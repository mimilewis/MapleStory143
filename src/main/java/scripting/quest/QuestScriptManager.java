/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.quest;

import client.MapleClient;
import configs.ServerConfig;
import constants.ScriptType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractScriptManager;
import server.quest.MapleQuest;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author PlayDK
 */
public class QuestScriptManager extends AbstractScriptManager {

    private static final Logger log = LogManager.getLogger(QuestScriptManager.class.getName());
    private static final QuestScriptManager instance = new QuestScriptManager();
    private final Map<MapleClient, QuestActionManager> qms = new WeakHashMap<>();
    //没脚本的任务信息 以免重复写入日志
    private final List<Integer> startQuestIds = new LinkedList<>();
    private final List<Integer> endQuestIds = new LinkedList<>();

    public synchronized static QuestScriptManager getInstance() {
        return instance;
    }

    public void startQuest(MapleClient c, int npcId, int questId) {
        if (c.getPlayer().isInJailMap()) {
            c.getPlayer().dropMessage(1, "在这个地方无法进行任务操作.");
            return;
        }
        if (!MapleQuest.getInstance(questId).canStart(c.getPlayer(), npcId)) {
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(6, "startQuest - 不能开始这个任务 NPC：" + npcId + " Quest：" + questId);
            }
            return;
        }
        try {
            if (qms.containsKey(c)) {
                dispose(c);
                return;
            }
            Invocable iv = getInvocable("quest/" + questId + ".js", c, true);
            if (iv == null) {
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(5, "开始任务脚本不存在 NPC：" + npcId + " Quest：" + questId);
                }
                if (!startQuestIds.contains(questId)) {
                    log.info("开始任务脚本不存在 NPC：" + npcId + " Quest：" + questId);
                }
                dispose(c);
                return;
            }
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "开始脚本任务 NPC：" + npcId + " Quest：" + questId);
            }
            ScriptEngine scriptengine = (ScriptEngine) iv;
            QuestActionManager qm = new QuestActionManager(c, npcId, questId, true, ScriptType.QUEST_START, iv);
            qms.put(c, qm);
            scriptengine.put("qm", qm);
            c.getPlayer().setConversation(1);
            c.setClickedNPC();
            iv.invokeFunction("start", (byte) 1, (byte) 0, 0);
        } catch (Exception e) {
            log.error("执行任务脚本失败 任务ID: (" + questId + ")..NPCID: " + npcId + ".", e);
            dispose(c);
            notice(c, questId);
        }
    }

    public void startAction(MapleClient c, byte mode, byte type, int selection) {
        QuestActionManager qm = qms.get(c);
        if (qm == null) {
            return;
        }
        try {
            if (qm.pendingDisposal) {
                dispose(c);
            } else {
                c.setClickedNPC();
                qm.getIv().invokeFunction("start", mode, type, selection);
            }
        } catch (Exception e) {
            int npcId = qm.getNpc();
            int questId = qm.getQuest();
            log.error("执行任务脚本失败 任务ID: (" + questId + ")..NPCID: " + npcId + ".", e);
            dispose(c);
            notice(c, questId);
        }
    }

    public void endQuest(MapleClient c, int npcId, int questId, boolean customEnd) {
        if (c.getPlayer().isInJailMap()) {
            c.getPlayer().dropMessage(1, "在这个地方无法进行任务操作.");
            return;
        }
        if (!customEnd && !MapleQuest.getInstance(questId).canComplete(c.getPlayer(), null)) {
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(6, "不能完成这个任务 NPC：" + npcId + " Quest：" + questId);
            }
            return;
        }
        try {
            if (!qms.containsKey(c) && c.canClickNPC()) {
                Invocable iv = getInvocable("quest/" + questId + ".js", c, true);
                if (iv == null) {
                    if (c.getPlayer().isAdmin()) {
                        c.getPlayer().dropMessage(5, "完成任务脚本不存在 NPC：" + npcId + " Quest：" + questId);
                    }
                    dispose(c);
                    if (!endQuestIds.contains(questId)) {
                        endQuestIds.add(questId);
                        MapleQuest.getInstance(questId).forceComplete(c.getPlayer(), npcId);
                        log.info("完成任务脚本不存在 NPC：" + npcId + " Quest：" + questId);
                    }
                    return;
                }
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(5, "完成脚本任务 NPC：" + npcId + " Quest：" + questId);
                }
                ScriptEngine scriptengine = (ScriptEngine) iv;
                QuestActionManager qm = new QuestActionManager(c, npcId, questId, false, ScriptType.QUEST_END, iv);
                qms.put(c, qm);
                scriptengine.put("qm", qm);
                c.getPlayer().setConversation(1);
                c.setClickedNPC();
                iv.invokeFunction("end", (byte) 1, (byte) 0, 0);
            } else {
                dispose(c);
            }
        } catch (Exception e) {
            log.error("执行任务脚本失败 任务ID: (" + questId + ")..NPCID: " + npcId + ".", e);
            dispose(c);
            notice(c, questId);
        }
    }

    public void endAction(MapleClient c, byte mode, byte type, int selection) {
        QuestActionManager qm = qms.get(c);
        if (qm == null) {
            return;
        }
        try {
            if (qm.pendingDisposal) {
                dispose(c);
            } else {
                c.setClickedNPC();
                qm.getIv().invokeFunction("end", mode, type, selection);
            }
        } catch (Exception e) {
            int npcId = qm.getNpc();
            int questId = qm.getQuest();
            log.error("完成任务脚本失败 任务ID: (" + questId + ")..NPCID: " + npcId + ".", e);
            dispose(c);
            notice(c, questId);
        }
    }

    public void dispose(MapleClient c) {
        QuestActionManager qm = qms.get(c);
        if (qm != null) {
            qms.remove(c);
            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/quest/" + qm.getQuest() + ".js");
        }
        if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
            c.getPlayer().setConversation(0);
        }
    }

    public void dispose(QuestActionManager qm, MapleClient c) {
        if (qm != null) {
            qms.remove(c);
            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/quest/" + qm.getQuest() + ".js");
        }
        if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
            c.getPlayer().setConversation(0);
        }
    }

    public QuestActionManager getQM(MapleClient c) {
        return qms.get(c);
    }

    private void notice(MapleClient c, int questId) {
        c.getPlayer().dropMessage(1, "这个任务脚本是错误的，请联系管理员修复它.任务ID: " + questId);
    }
}
