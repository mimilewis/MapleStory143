/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package scripting.item;

import client.MapleClient;
import client.inventory.Item;
import client.skills.Skill;
import client.skills.SkillFactory;
import constants.ItemConstants;
import constants.JobConstants;
import constants.ScriptType;
import scripting.npc.NPCConversationManager;
import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;

import javax.script.Invocable;

/**
 * @author PlayDK
 */
public class ItemActionManager extends NPCConversationManager {

    private final Item item;

    /**
     * @param c
     * @param npc
     * @param item
     * @param iv
     */
    public ItemActionManager(MapleClient c, int npc, Item item, Invocable iv) {
        super(c, npc, String.valueOf(item.getItemId()), ScriptType.ITEM, iv);
        this.item = item;
    }

    @Override
    public void dispose() {
        dispose(-1);
    }

    public void dispose(int remove) {
        if (remove == 0) {
            this.usedAll();
        } else if (remove > 0) {
            this.used(remove);
        }
        ItemScriptManager.getInstance().dispose(getClient());
    }

    /**
     * @return
     */
    public Item getItem() {
        return item;
    }

    /**
     * @return
     */
    public int getItemId() {
        return item.getItemId();
    }

    public short getPosition() {
        return item.getPosition();
    }

    public boolean used() {
        return used(1);
    }

    public boolean used(int q) {
        return MapleInventoryManipulator.removeFromSlot(getClient(), ItemConstants.getInventoryType(getItemId()), getPosition(), (short) q, true, false);
    }

    /**
     * 删除一个道具
     */
    public void remove() {
        remove(1);
    }

    /**
     * 删除指定数量的道具
     *
     * @param quantity 数量
     */
    public void remove(int quantity) {
        used(quantity);
    }

    public boolean usedAll() {
        return MapleInventoryManipulator.removeFromSlot(getClient(), ItemConstants.getInventoryType(getItemId()), getPosition(), item.getQuantity(), true, false);
    }

    public String getSkillMenu(int skillMaster) {
        StringBuilder sb = new StringBuilder();

        getPlayer().getSkills().forEach((key, value) -> {
            Skill skill = SkillFactory.getSkill(key);
            if (JobConstants.getSkillBookByJob(key / 10000) > 2 && skill.getMaxLevel() > 10 && value.masterlevel < skill.getMaxLevel()) {
                if (skillMaster > 20) {
                    if (value.masterlevel < 30 && value.masterlevel >= 20 && skill.getMaxLevel() > 20) {
                        sb.append("\r\n#L").append(key).append("# #s").append(key).append("# #fn黑体##fs14##e#q").append(key).append("##n#fs##fn##l");
                    }
                } else if (value.masterlevel < 20) {
                    sb.append("\r\n#L").append(key).append("# #s").append(key).append("# #fn黑体##fs14##e#q").append(key).append("##n#fs##fn##l");
                }
            }
        });
        return sb.toString();
    }

    public boolean canUseSkillBook(int skillId, int masterLevel) {
        if (masterLevel > 0) {
            Skill skill = SkillFactory.getSkill(skillId);
            if (getPlayer().getSkillLevel(skill) >= skill.getMaxLevel()) {
                return false;
            }
            int skillLevel = getPlayer().getSkillLevel(skill);
            if (skillLevel >= 5 && masterLevel == 20 || skillLevel >= 15 && masterLevel == 30) {
                return true;
            }
        }
        return false;
    }

    public void useSkillBook(int skillId, int masterLevel) {
        Skill skill = SkillFactory.getSkill(skillId);
        masterLevel = masterLevel > skill.getMaxLevel() ? skill.getMaxLevel() : masterLevel;
        getPlayer().changeSingleSkillLevel(skill, getPlayer().getSkillLevel(skill), (byte) masterLevel);
        getPlayer().getMap().broadcastMessage(MaplePacketCreator.useSkillBook(getPlayer(), 0, 0, true, true));
        enableActions();
    }

    public void useFailed() {
        getMap().broadcastMessage(MaplePacketCreator.useSkillBook(getPlayer(), 0, 0, true, false));
        enableActions();
    }
}
