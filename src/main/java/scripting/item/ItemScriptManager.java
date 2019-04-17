/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.item;

import client.MapleClient;
import client.inventory.Item;
import configs.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.AbstractScriptManager;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * @author PlayDK
 */
public class ItemScriptManager extends AbstractScriptManager {

    private static final Logger log = LogManager.getLogger();

    private static final ItemScriptManager instance = new ItemScriptManager();
    private final Map<MapleClient, ItemActionManager> ims = new WeakHashMap<>();

    public synchronized static ItemScriptManager getInstance() {
        return instance;
    }

    public void start(MapleClient c, int npc, Item item) {
        try {
            if (ims.containsKey(c)) {
                dispose(c);
                return;
            }
            if (c.getPlayer().isAdmin()) {
                c.getPlayer().dropMessage(5, "开始道具脚本 NPC：" + npc + " ItemId：" + item.getItemId());
            }
            Invocable iv = getInvocable("item/" + item.getItemId() + ".js", c, true);
            ScriptEngine scriptengine = (ScriptEngine) iv;
            ItemActionManager im = new ItemActionManager(c, npc, item, iv);
            if (iv == null) {
                c.getPlayer().dropMessage(5, "找不到道具脚本: " + npc + " 道具ID: " + item.getItemId() + " 当前所在地图: " + c.getPlayer().getMapId());
                dispose(c);
                return;
            }
            ims.put(c, im);
            scriptengine.put("im", im);
            scriptengine.put("it", im);
            c.getPlayer().setConversation(1);
            c.setClickedNPC();
            try {
                iv.invokeFunction("start");
            } catch (NoSuchMethodException nsme) {
                iv.invokeFunction("action", (byte) 1, (byte) 0, 0);
            }
        } catch (Exception e) {
            log.error("执行道具脚本失败 道具ID: (" + item.getItemId() + ")..NPCID: " + npc + ". 错误信息: ", e);
            dispose(c);
            notice(c, item.getItemId());
        }
    }

    public void action(MapleClient c, byte mode, byte type, int selection) {
        if (mode != -1) {
            ItemActionManager im = ims.get(c);
            if (im == null) {
                return;
            }
            try {
                if (im.pendingDisposal) {
                    dispose(c);
                } else {
                    c.setClickedNPC();
                    im.getIv().invokeFunction("action", mode, type, selection);
                }
            } catch (Exception e) {
                int npcId = im.getNpc();
                int itemId = im.getItemId();
                log.error("执行NPC脚本出错 NPC ID : " + npcId + " 道具ID: " + itemId + " 错误信息: ", e);
                dispose(c);
                notice(c, itemId);
            }
        }
    }

    public void dispose(MapleClient c) {
        ItemActionManager im = ims.get(c);
        if (im != null) {
            ims.remove(c);
            c.removeScriptEngine(ServerConfig.WORLD_SCRIPTSPATH + "/item/" + im.getItemId() + ".js");
        }
        if (c.getPlayer() != null && c.getPlayer().getConversation() == 1) {
            c.getPlayer().setConversation(0);
        }
    }

    public ItemActionManager getIM(MapleClient c) {
        return ims.get(c);
    }

    private void notice(MapleClient c, int itemId) {
        c.getPlayer().dropMessage(1, "这个道具脚本是错误的，请联系管理员修复它.道具ID: " + itemId);
    }
}
