package scripting.npc;

import client.MapleClient;
import configs.ServerConfig;
import constants.ScriptType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractScriptManager;
import tools.StringUtil;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.WeakHashMap;

public class NPCScriptManager extends AbstractScriptManager {

    private static final Logger log = LogManager.getLogger(NPCScriptManager.class.getName());
    private static final NPCScriptManager instance = new NPCScriptManager();
    private final Map<MapleClient, NPCConversationManager> cms = new WeakHashMap<>();

    public synchronized static NPCScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npcId) {
        start(c, npcId, null);
    }

    public void start(MapleClient c, int npcId, String npcMode) {
        try {
            if (cms.containsKey(c)) {
                dispose(c);
                return;
            }
            Invocable iv;
            if (npcMode == null || npcMode.isEmpty()) {
                iv = getInvocable("npc/" + npcId + ".js", c, true);
            } else if (StringUtil.isNumber(npcMode)) {
                iv = getInvocable("npc/" + npcId + "_" + npcMode + ".js", c, true);
            } else {
                iv = getInvocable("extend/" + npcMode + ".js", c, true);
            }
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "开始NPC对话 NPC：" + npcId + " 模式：" + npcMode);
            }
            ScriptEngine scriptengine = (ScriptEngine) iv;
            NPCConversationManager cm = new NPCConversationManager(c, npcId, npcMode, ScriptType.NPC, iv);
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv == null) {
                    c.getPlayer().dropMessage(5, "找不到NPC脚本: " + npcId + " 模式: " + npcMode + " 当前所在地图: " + c.getPlayer().getMapId());
                }
                dispose(c);
                return;
            }
            cms.put(c, cm);
            scriptengine.put("cm", cm);
            c.getPlayer().setConversation(1);
            c.setClickedNPC();
            try {
                iv.invokeFunction("start");
            } catch (NoSuchMethodException nsme) {
                iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
            }
        } catch (Exception e) {
            log.error("执行NPC脚本出错 NPC ID : " + npcId + " 模式: " + npcMode + ".\r\n错误信息: ", e);
            dispose(c);
            notice(c, npcId, npcMode);
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        if (mode != -1) {
            NPCConversationManager cm = cms.get(c);
            if (cm == null) {
                return;
            }
            try {
                if (cm.pendingDisposal) {
                    dispose(c);
                } else {
                    c.setClickedNPC();
                    cm.getIv().invokeFunction("action", mode, type, selection);
                }
            } catch (Exception e) {
                int npcId = cm.getNpc();
                String npcMode = cm.getNpcMode();
                log.error("执行NPC脚本出错 NPC ID : " + npcId + " 模式: " + npcMode + ". \r\n错误信息: ", e);
                dispose(c);
                notice(c, npcId, npcMode);
            }
        }
    }

    public void dispose(MapleClient c) {
        NPCConversationManager npccm = cms.get(c);
        StringBuilder stringBuilder = new StringBuilder();
        if (npccm != null) {
            cms.remove(c);

            if (null != npccm.getType()) {
                switch (npccm.getType()) {
                    case NPC: {
                        if (npccm.getScript() == null) {
                            stringBuilder.append(ServerConfig.WORLD_SCRIPTSPATH).append("/npc/").append(npccm.getNpc()).append(".js");
                            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/npc/" + npccm.getNpc() + ".js");
                        } else if (StringUtil.isNumber(npccm.getScript())) {
                            stringBuilder.append(ServerConfig.WORLD_SCRIPTSPATH).append("/npc/").append(npccm.getNpc()).append("_").append(npccm.getScript()).append(".js");
                            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/npc/" + npccm.getNpc() + "_" + npccm.getScript() + ".js");
                        } else {
                            stringBuilder.append(ServerConfig.WORLD_SCRIPTSPATH).append("/extend/").append(npccm.getScript()).append(".js");
                            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/extend/" + npccm.getScript() + ".js");
                        }
                        break;
                    }
                    case ON_USER_ENTER: {
                        stringBuilder.append(ServerConfig.WORLD_SCRIPTSPATH).append("/map/onUserEnter/").append(npccm.getScript()).append(".js");
                        break;
                    }
                    case ON_FIRST_USER_ENTER: {
                        stringBuilder.append(ServerConfig.WORLD_SCRIPTSPATH).append("/map/onFirstUserEnter/").append(npccm.getScript()).append(".js");
                        break;
                    }
                }
            }
            c.removeScriptEngine(stringBuilder.toString());
        }
        if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
            c.getPlayer().setConversation(0);
        }
    }

    public NPCConversationManager getCM(MapleClient c) {
        return cms.get(c);
    }

    private void notice(MapleClient c, int npcId, String npcMode) {
        c.getPlayer().dropMessage(1, "这个NPC脚本是错误的，请联系管理员修复它.NPCID: " + npcId + (npcMode != null && !npcMode.isEmpty() ? " 模式:" + npcMode : ""));
    }

    public final void onUserEnter(MapleClient c, String scriptname) {
        try {
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "[地图脚本] 执行onUserEnter脚本：" + scriptname + " 地图：" + c.getPlayer().getMap().getMapName());
            }
            Invocable iv = getInvocable("map/onUserEnter/" + scriptname + ".js", c, true);
            ScriptEngine scriptEngine = (ScriptEngine) iv;
            NPCConversationManager ms = new NPCConversationManager(c, 0, scriptname, ScriptType.ON_USER_ENTER, iv);
            if (cms.containsValue(ms)) {
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(5, "无法执行脚本:已有脚本执行 - " + cms.containsKey(c));
                }
                this.dispose(c);
                return;
            }
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv == null) {
                    // empty if block
                }
                this.dispose(c);
                return;
            }
            cms.put(c, ms);
            scriptEngine.put("ms", ms);
            c.getPlayer().setConversation(1);
            c.setClickedNPC();
            try {
                iv.invokeFunction("start");
            } catch (NoSuchMethodException nsme) {
                iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
            }
        } catch (NoSuchMethodException | ScriptException exception) {
            log.error("执行地图onUserEnter脚本出錯 : " + scriptname + ".\r\n错误信息：", exception);
            this.dispose(c);
        }
    }


    public final void onFirstUserEnter(MapleClient c, String scriptname) {
        try {
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "[地图脚本] 执行onFirstUserEnter脚本：" + scriptname + " 地图：" + c.getPlayer().getMap().getMapName());
            }
            if (cms.containsKey(c)) {
                if (c.getPlayer().isAdmin()) {
                    c.getPlayer().dropMessage(5, "无法执行脚本:已有脚本执行 - " + cms.containsKey(c));
                }
                this.dispose(c);
                return;
            }
            Invocable iv = getInvocable("map/onFirstUserEnter/" + scriptname + ".js", c, true);
            ScriptEngine scriptEngine = (ScriptEngine) iv;
            NPCConversationManager ms = new NPCConversationManager(c, 0, scriptname, ScriptType.ON_FIRST_USER_ENTER, iv);
            if (iv == null || NPCScriptManager.getInstance() == null) {
                if (iv != null || c.getPlayer().isAdmin()) {
                    // empty if block
                }
                this.dispose(c);
                return;
            }
            cms.put(c, ms);
            scriptEngine.put("ms", ms);
            c.getPlayer().setConversation(1);
            c.setClickedNPC();
            try {
                iv.invokeFunction("start");
            } catch (NoSuchMethodException nsme) {
                iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
            }
        } catch (NoSuchMethodException | ScriptException exception) {
            log.error("onFirstUserEnter : " + scriptname + ".\r\n错误信息：", exception);
            this.dispose(c);
        }
    }
}