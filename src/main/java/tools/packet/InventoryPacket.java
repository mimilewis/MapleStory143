/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools.packet;

import client.MapleCharacter;
import client.inventory.*;
import constants.ItemConstants;
import constants.ServerConstants;
import handling.channel.handler.EnchantHandler;
import handling.channel.handler.ItemScrollHandler;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.StructItemOption;
import server.maps.MapleMapItem;
import tools.DateUtil;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static tools.packet.PacketHelper.addItemInfo;

/**
 * @author PlayDK
 */
public class InventoryPacket {

    private static final Logger log = LogManager.getLogger(InventoryPacket.class);

    public static byte[] updateInventorySlotLimit(byte invType, byte newSlots) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_INVENTORY_SLOT.getValue());
        mplew.write(invType);
        mplew.write(newSlots);

        return mplew.getPacket();
    }

    public static byte[] updatePet(MaplePet pet, Item item, boolean summoned) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.write(0);
        mplew.write(2);
        mplew.write(0);
        mplew.write(3);
        mplew.write(5);
        mplew.writeShort(pet.getInventoryPosition());
        mplew.write(0);
        mplew.write(0);
        mplew.write(5);
        mplew.writeShort(pet.getInventoryPosition());
        mplew.write(3);
        mplew.writeInt(pet.getPetItemId());
        mplew.write(1);
        mplew.writeLong(pet.getUniqueId());
        PacketHelper.addPetItemInfo(mplew, item, pet, summoned);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] petAddSkill(MapleCharacter player, MaplePet pet) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PET_ADD_SKILL.getValue());
        mplew.writeInt(player.getId());
        mplew.write(1);
        mplew.writeInt(pet.getAddSkill());
        mplew.writeInt(0);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] modifyInventory() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.writeZeroBytes(3);

        return mplew.getPacket();
    }

    public static byte[] modifyInventory(boolean updateTick, List<ModifyInventory> mods) {
        return modifyInventory(updateTick, mods, null);
    }

    public static byte[] modifyInventory(boolean updateTick, List<ModifyInventory> mods, MapleCharacter chr) {
        return modifyInventory(updateTick, mods, chr, false);
    }

    /*
     * 0 = 获得道具
     * 1 = 更新道具数量
     * 2 = 移动道具
     * 3 = 删除道具
     * 4 = 刷新装备经验
     * 5 = 移动道具小背包到背包
     * 6 = 小背包更新道具
     * 7 = 小背包删除道具
     * 8 = 移动位置小背包里面的道具
     * 9 = 小背包获得道具
     */
    public static byte[] modifyInventory(boolean updateTick, List<ModifyInventory> mods, MapleCharacter chr, boolean active) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MODIFY_INVENTORY_ITEM.getValue());
        mplew.writeBool(updateTick);
        mplew.writeShort(mods.size()); //更新的次数
        int addMovement = -1;
        for (ModifyInventory mod : mods) {
            mplew.write(mod.getMode());
            mplew.write(mod.getInventoryType());
            boolean oldpos = mod.getMode() == 2 || mod.getMode() == 8 || (mod.getMode() == 5 && !mod.switchSrcDst());
            mplew.writeShort(oldpos ? mod.getOldPosition() : mod.getPosition());
            switch (mod.getMode()) {
                case 0:  //获得道具
                    addItemInfo(mplew, mod.getItem(), chr, active);
                    break;
                case 1:  //更新道具数量
                    mplew.writeShort(mod.getQuantity());
                    break;
                case 2:  //移动道具                  
                    mplew.writeShort(mod.getPosition());
                    if (mod.getPosition() < 0 || mod.getOldPosition() < 0) {
                        addMovement = mod.getOldPosition() < 0 ? 1 : 2;
                    }
                    break;
                case 3:  //删除道具
                    if (mod.getPosition() < 0) {
                        addMovement = 2;
                    }
                    break;
                case 4:  // 刷新经验值
                    mplew.writeLong(((Equip) mod.getItem()).getSealedExp());
                    break;
                case 5: //移动道具小背包到背包
                    mplew.writeShort(!mod.switchSrcDst() ? mod.getPosition() : mod.getOldPosition());
                    if (mod.getIndicator() != -1) {
                        mplew.writeShort(mod.getIndicator());
                    }
                    break;
                case 6: //小背包更新道具
                    mplew.writeShort(mod.getQuantity());
                    break;
                case 7: //小背包删除道具
                    //这个地方无需处理
                    break;
                case 8: //移动位置小背包里面的道具
                    mplew.writeShort(mod.getPosition());
                    break;
                case 9: //小背包获得道具
                    addItemInfo(mplew, mod.getItem());
                    break;
            }
            mplew.write(0);
            mod.clear();
        }
        if (addMovement > -1) {
            mplew.write(addMovement);
        }

        return mplew.getPacket();
    }

    public static byte[] getInventoryFull() {
        return modifyInventory(true, Collections.emptyList());
    }

    public static byte[] getInventoryStatus() {
        return modifyInventory(false, Collections.emptyList());
    }

    public static byte[] getShowInventoryFull() {
        return getShowInventoryStatus(0xFF);
    }

    public static byte[] showItemUnavailable() {
        return getShowInventoryStatus(0xFE);
    }

    public static byte[] getShowInventoryStatus(int mode) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_STATUS_INFO.getValue());
        mplew.write(ServerConstants.MapleStatusInfo.获得道具.getType());
        mplew.write(mode);

        return mplew.getPacket();
    }

    public static byte[] showScrollTip(boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_TIP.getValue());
        mplew.writeInt(success ? 1 : 0);

        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chrId, int scroll, int toScroll) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeShort(1);
        mplew.writeInt(scroll);
        mplew.writeInt(toScroll);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getScrollEffect(int chrId, Equip.ScrollResult scrollSuccess, boolean legendarySpirit, boolean whiteScroll, int scroll, int toScroll) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        //[DB 00] [46 57 01 00] [00] [00] [00] [00] 没有成功
        mplew.writeShort(SendPacketOpcode.SHOW_SCROLL_EFFECT.getValue());
        mplew.writeInt(chrId);
        switch (scrollSuccess) {
            case 失败:
                mplew.write(0x00);
                break;
            case 成功:
                mplew.write(0x01);
                break;
            case 消失:
                mplew.write(0x02);
                break;
            default:
                throw new IllegalArgumentException("effect in illegal range");
        }
        mplew.write(legendarySpirit ? 0 : 0); //V.110修改 好像都是0
        mplew.writeInt(scroll);
        mplew.writeInt(toScroll);
        mplew.write(whiteScroll ? 1 : 0);

        return mplew.getPacket();
    }

    /*
     * 使用魔方
     */
    public static byte[] getPotentialEffect(int chrId, int itemid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNIFYING_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(1);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    /*
     * 使用放大镜
     */
    public static byte[] showMagnifyingEffect(int chrId, short pos, boolean isPotAdd) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_MAGNIFYING_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.writeShort(pos);
        mplew.write(isPotAdd ? 1 : 0); //T071新增 是否扩展潜能

        return mplew.getPacket();
    }

    public static byte[] showPotentialReset(boolean fireworks, int chrId, boolean success, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(fireworks ? SendPacketOpcode.SHOW_FIREWORKS_EFFECT.getValue() : SendPacketOpcode.SHOW_POTENTIAL_FINALPANEL.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemid); // fireworks, Item/Cash/0506.img/%08d/effect/default

        return mplew.getPacket();
    }

    public static byte[] showBlackCubeResults() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MEMORIAL_CUBE_MODIFIED.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 重置扩展潜能效果
     */
    public static byte[] 潜能变化效果(int chrId, boolean success, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ADDITIONAL_RESET.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    /*
     * 增加扩展潜能效果
     */
    public static byte[] 潜能扩展效果(int chrId, boolean success, int itemid, boolean 是否破坏) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ADDITIONAL_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemid);
        mplew.write(是否破坏 ? 1 : 0); //道具是否破坏

        return mplew.getPacket();
    }

    /*
     * 镶嵌星岩效果
     */
    public static byte[] showNebuliteEffect(int chrId, boolean success, String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_NEBULITE_EFFECT.getValue());
        mplew.writeInt(chrId);
        mplew.write(success ? 1 : 0);
        mplew.writeMapleAsciiString(msg);

        return mplew.getPacket();
    }

    /*
     * 道具合成提示
     */
    public static byte[] showSynthesizingMsg(int itemId, int giveItemId, boolean success) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SYNTHESIZING_MSG.getValue());
        mplew.write(success ? 1 : 0);
        mplew.writeInt(itemId);
        mplew.writeInt(giveItemId);

        return mplew.getPacket();
    }

    public static byte[] dropItemFromMapObject(MapleMapItem drop, Point dropfrom, Point dropto, byte mod) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DROP_ITEM_FROM_MAPOBJECT.getValue());
        mplew.write(0);
        mplew.write(mod); // 1 animation, 2 no animation, 3 spawn disappearing item [Fade], 4 spawn disappearing item
        mplew.writeInt(drop.getObjectId()); // item owner id
        mplew.write(drop.getMeso() > 0 ? 1 : 0); // 1 金币, 0 物品, 2 and above all item meso bag,
        mplew.writeZeroBytes(12);
        mplew.writeInt(drop.getItemId()); // drop object ID
        mplew.writeInt(drop.getOwner()); // owner charid
        mplew.write(drop.getDropType()); // 0 = timeout for non-owner, 1 = timeout for non-owner's party, 2 = FFA, 3 = explosive/FFA
        mplew.writePos(dropto);
        mplew.writeInt(0);
        mplew.write(0);
        mplew.writeInt(0);
        mplew.write(0);
        if (mod != 2) {
            mplew.writePos(dropfrom);
            mplew.writeInt(0); //FH ?
        }
        mplew.write(0);

        if (drop.getMeso() == 0) {
            PacketHelper.addExpirationTime(mplew, drop.getItem().getExpiration());
        }
        mplew.writeInt(drop.isPlayerDrop() ? 0 : 1); // 玩家丢弃是 0 怪物掉落是 1
        mplew.writeZeroBytes(6); //未知 V.116.1修改 以前2位 好像玩家丢弃和掉落是6个0 如果是技能移动是 [00 02 00 00 00 00]
        mplew.write(drop.getState()); // 1蓝色光效B 2紫色光效A 3黄色光效S 4绿色光效SS
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] explodeDrop(int oid) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(4); // 4 = Explode
        mplew.writeInt(oid);
        mplew.writeShort(655);

        return mplew.getPacket();
    }

    public static byte[] removeItemFromMap(int oid, int animation, int chrId) {
        return removeItemFromMap(oid, animation, chrId, 0);
    }

    public static byte[] removeItemFromMap(int oid, int animation, int chrId, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.REMOVE_ITEM_FROM_MAP.getValue());
        mplew.write(animation); // 0 = Expire, 1 = without animation, 2 = pickup, 4 = explode, 5 = pet pickup
        mplew.writeInt(oid);
        if (animation >= 2) {
            mplew.writeInt(chrId);
            if (animation == 5) { // allow pet pickup?
                mplew.writeInt(slot);
            }
        }

        return mplew.getPacket();
    }

    /*
     * 药剂罐使用返回的提示
     * 0 = 使用失败
     * 1 = 使用成功
     */
    public static byte[] showPotionPotMsg(int reason) {
        return showPotionPotMsg(reason, 0x00);
    }

    public static byte[] showPotionPotMsg(int reason, int msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.POTION_POT_MSG.getValue());
        mplew.write(reason);
        if (reason == 0) {
            /*
             * 0x00 没有提示
             * 0x01 没有物品
             * 0x02 这个药剂罐已经满了。
             * 0x03 你的药剂罐容量已达最大值。
             * 0x04 药剂魔瓶不能用在生锈的药剂罐上。请用除锈剂为你的药剂罐除锈。
             * 0x05 你的药剂罐还没有生锈。
             * 0x06 这个药剂罐是空的，请再次填充。
             * 0x08 被奇怪的气息所围绕，暂时无法使用道具。
             */
            mplew.write(msg);
        }

        return mplew.getPacket();
    }

    /*
     * 更新药剂罐信息
     */
    public static byte[] updataPotionPot(MaplePotionPot potionPot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.POTION_POT_UPDATE.getValue());
        PacketHelper.addPotionPotInfo(mplew, potionPot);

        return mplew.getPacket();
    }

    /*
     * 更新宝盒信息
     */
    public static byte[] updataCoreAura(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_CORE_AURA.getValue());
        mplew.write(0x01);
        mplew.writeZeroBytes(6);
        mplew.write(0x04);
        mplew.writeZeroBytes(21);
        mplew.writeInt(8951284); //F4 95 88 00
        mplew.writeLong(0x01); //好像更新2次 第1次这个地方为 0x01 第2次为 0x02
        PacketHelper.addCoreAura(mplew, chr);

        return mplew.getPacket();
    }

    /*
     * 显示角色当前装备的技能皮肤信息
     */
    public static byte[] showSkillSkin(Map<Integer, Integer> skillskinlist) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_SKILL_SKIN.getValue());
        mplew.writeInt(0x02);
        mplew.writeInt(skillskinlist.size()); //当前全部装备的中的技能皮肤
        //循环发送信息[技能ID] [皮肤ID]
        for (Map.Entry<Integer, Integer> skillskin : skillskinlist.entrySet()) {
            mplew.writeInt(skillskin.getKey());
            mplew.writeInt(skillskin.getValue());
        }

        return mplew.getPacket();
    }

    /**
     * 其他玩家更换伤害皮肤效果
     */
    public static byte[] showDamageSkin(int chrId, int skinId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_DAMAGE_SKIN.getValue());
        mplew.writeInt(chrId); //玩家ID
        mplew.writeInt(skinId); //更换的伤害皮肤ID

        return mplew.getPacket();
    }

    /**
     * @param chrId
     * @param skinId
     * @return
     */
    public static final byte[] showDamageSkin_Premium(int chrId, int skinId) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_DAMAGE_SKIN_PREMIUM.getValue());
        mplew.writeInt(chrId);
        mplew.writeInt(skinId);

        return mplew.getPacket();
    }

    public static byte[] enchantingOperation(int n2, int n3, Item toScroll, Item scrolled, List<EnchantScrollEntry> scrollEntries) {
        return enchantingOperation(n2, n3, toScroll, scrolled, scrollEntries, "");
    }

    public static byte[] enchantingOperation(int mode, int success, Item toScroll, Item scrolled, List<EnchantScrollEntry> scrollEntries, String string) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.ENCHANTING_OPERATION.getValue());
        mplew.write(mode);
        switch (mode) {
            case 50: {
                mplew.write(0);
                mplew.write(scrollEntries.size());
                for (EnchantScrollEntry scrollEntry : scrollEntries) {
                    mplew.writeInt(scrollEntry.getViewType());
                    mplew.writeMapleAsciiString(scrollEntry.getName());
                    mplew.writeInt(scrollEntry.getViewType() == 4 ? 1 : (scrollEntry.getViewType() == 5 ? 2 : 0));
                    mplew.writeInt(scrollEntry.getViewType() >= 4 ? 1 : 0);
                    mplew.writeInt(scrollEntry.getMask());
                    if (scrollEntry.getMask() > 0) {
                        for (int n4 : scrollEntry.getValues()) {
                            mplew.writeInt(n4);
                        }
                    }
                    mplew.writeInt(scrollEntry.getNeed() * ItemScrollHandler.getNeedRate(((Equip) scrolled).getReqLevel()));
                    mplew.write(scrollEntry.getSucess() == 100 ? 1 : 0);
                }
                break;
            }
            case 51: {
                mplew.write(0);
                break;
            }
            case 52: {
                Equip equip = (Equip) toScroll;
                byte by2 = equip.getEnhance();
                boolean bl2 = MapleItemInformationProvider.getInstance().isSuperiorEquip(equip.getItemId());
                boolean bl3 = equip.getStartCurse();
                int n5 = equip.getNeedStar() * 10;
                int n6 = bl3 ? (1000 - n5) * (bl2 ? by2 - 4 : by2 - 14) / 18 : 0;
                int n7 = 2 * (by2 * by2 + 2);
                mplew.writeBool(equip.getStartDown());
                mplew.writeLong(n7);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeInt(n5);
                mplew.writeInt(n6);
                mplew.writeInt(0);
                mplew.writeInt(0);
                mplew.writeBool(equip.getBonusTime());
                writeMaskEnchantScroll(mplew, toScroll);
                break;
            }
            case 53: {
                mplew.write(0);
                mplew.writeInt(DateUtil.getTime());
                break;
            }
            case 100: {
                mplew.write(0);
                mplew.writeInt(success);
                mplew.writeMapleAsciiString(string);
                PacketHelper.addItemInfo(mplew, toScroll);
                if (success == 2) {
                    mplew.writeShort(0);
                    break;
                }
                PacketHelper.addItemInfo(mplew, scrolled);
                break;
            }
            case 101: {
                mplew.write(success);
                mplew.writeInt(0);
                PacketHelper.addItemInfo(mplew, toScroll);
                PacketHelper.addItemInfo(mplew, scrolled);
                break;
            }
            case 102: {
                mplew.writeInt(success);
                break;
            }
            case 103: {
                PacketHelper.addItemInfo(mplew, toScroll);
                PacketHelper.addItemInfo(mplew, scrolled);
                break;
            }
            case 104: {
                mplew.write(0);
                break;
            }
            case 105: {
                PacketHelper.addItemInfo(mplew, toScroll);
                PacketHelper.addItemInfo(mplew, scrolled);
            }
        }
        return mplew.getPacket();
    }

    public static void writeMaskEnchantScroll(MaplePacketLittleEndianWriter mplew, Item item) {
        Map<EnchantScrollFlag, Integer> scrollList = EnchantHandler.Companion.getEnchantScrollList(item, false);
        int mask = 0;
        for (EnchantScrollFlag flag : scrollList.keySet()) {
            if (scrollList.containsKey(flag) && scrollList.get(flag) > 0) {
                mask |= flag.getValue();
            }
        }
        mplew.writeInt(mask);
        if (mask != 0) {
            for (EnchantScrollFlag flag : EnchantScrollFlag.values()) {
                if (scrollList.containsKey(flag) && scrollList.get(flag) > 0) {
                    mplew.writeInt(scrollList.get(flag));
                }
            }
        }
    }

    public static byte[] getZeroWeaponInfo(int weaponlevel, int level, int weapon1, int weapon2) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ZERO_WEAPON_INFO.getValue());
        mplew.writeShort(0);
        mplew.writeInt(weaponlevel);
        mplew.writeInt(level);
        mplew.writeInt(weapon1);
        mplew.writeInt(weapon2);
        mplew.write(0);

        return mplew.getPacket();
    }

    public static byte[] getZeroWeaponChangePotential(int meso, int wp) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHANGE_POTENTIAL_MESO.getValue());
        mplew.writeInt(1);
        mplew.writeInt(meso);
        mplew.writeInt(wp);
        mplew.writeShort(1);

        return mplew.getPacket();
    }

    public static byte[] showZeroWeaponChangePotentialResult(boolean succ) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_CHANGE_POTENTIAL_RESULT.getValue());
        mplew.write(1);
        mplew.writeBool(succ);

        return mplew.getPacket();
    }

    public static byte[] showHyunPotentialResult(boolean result, int size, List<StructItemOption> potids) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_POTENTIAL_RESULT.getValue());
        mplew.writeShort(result ? 1 : 0);
        mplew.writeInt(0);
        if (!result) {
            mplew.writeInt(size);
            mplew.writeInt(potids.size());
            potids.forEach(integer -> mplew.writeInt(integer.opID));
        }

        return mplew.getPacket();
    }

    public static byte[] showCubeResetResult(final int n, final Item toScroll, final int n2, final int n3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort((n2 == 5062090) ? SendPacketOpcode.MEMORIAL_CUBE_RESULT.getValue() : ((n2 == 5062503) ? SendPacketOpcode.WHITEADDITIONAL_CUBE_RESULT.getValue() : SendPacketOpcode.BLACKY_CUBE_EFFECT.getValue()));
        mplew.writeLong((toScroll.getInventoryId() <= 0L) ? toScroll.getEquipOnlyId() : toScroll.getInventoryId());
        mplew.write(1);
        addItemInfo(mplew, toScroll);
        mplew.writeInt(n2);
        mplew.writeInt(n);
        mplew.writeInt(n3);

        return mplew.getPacket();
    }


    public static byte[] showCubeResult(final int chrid, final int cubeid, final int position, final Item equip) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        short opcode = 0;
        switch (cubeid) {
            case 5062022: {
                opcode = SendPacketOpcode.SHOW_SHININGMIRROR_CUBE_RESULT.getValue();
                break;
            }
            case 5062009: {
                opcode = SendPacketOpcode.SHOW_REDCUBE_RESULT.getValue();
                break;
            }
            case 5062500:
            case 5062501:
            case 5062502: {
                opcode = SendPacketOpcode.SHOW_ADDITIONALCUBE_RESULT.getValue();
                break;
            }
            default: {
                opcode = SendPacketOpcode.SHOW_INGAMECUBE_RESULT.getValue();
                break;
            }
        }
        mplew.writeShort(opcode);
        mplew.writeInt(chrid);
        mplew.write(1);
        mplew.writeInt(cubeid);
        mplew.writeInt(position);
        addItemInfo(mplew, equip);

        return mplew.getPacket();
    }

    public static byte[] showTapJoyInfo(final int slot, final int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        int i1 = itemid % 10 - 1;
        mplew.writeShort(SendPacketOpcode.TAP_JOY_INFO.getValue());
        mplew.write(5); // size
        mplew.writeInt(i1);
        mplew.writeInt(slot);
        mplew.writeInt(itemid);
        mplew.write(0);
        mplew.writeInt(350 * (i1 / 2 + 2));
        final int size = ItemConstants.TapJoyReward.getStages().size() / 2;
        mplew.writeInt(5840000 + i1);
        mplew.writeInt(size);
        for (int i = 0; i < size; ++i) {
            mplew.writeInt(i);
            mplew.writeInt(ItemConstants.TapJoyReward.getItemIdAndSN(i * 2).getLeft());
            mplew.writeInt(ItemConstants.TapJoyReward.getItemIdAndSN(i * 2).getRight());
            mplew.writeInt(ItemConstants.TapJoyReward.getItemIdAndSN(i * 2 + 1).getLeft());
            mplew.writeInt(ItemConstants.TapJoyReward.getItemIdAndSN(i * 2 + 1).getLeft());
            mplew.writeInt(100);
            mplew.writeInt(350 * (i / 2 + 2));
            mplew.writeInt(4009441 + i);
            mplew.writeInt(5840000 + i);
        }

        return mplew.getPacket();
    }

    public static byte[] showTapJoy(final int reward) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TAP_JOY.getValue());
        mplew.writeInt(reward);

        return mplew.getPacket();
    }

    public static byte[] showTapJoyDone(final int mode, final int itemid, final int intValue3, final int gainslot, final int intValue) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TAP_JOY_DONE.getValue());
        mplew.write(mode);
        mplew.writeInt(itemid);
        mplew.writeInt(intValue3);
        mplew.writeInt(4);
        mplew.writeInt(gainslot);
        mplew.writeInt(0);
        mplew.writeInt(intValue);

        return mplew.getPacket();
    }

    public static byte[] showTapJoyNextStage(final MapleCharacter player, final int n, final int n2, final int n3, final int n4) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TAP_JOY_NEXT_STAGE.getValue());
        mplew.write(n);
        mplew.writeInt(n3);
        mplew.writeInt(5);
        mplew.writeInt(n2);
        mplew.writeInt(n3);
        mplew.write(n4);
        mplew.writeInt(player.getCSPoints(n4));

        return mplew.getPacket();
    }
}
