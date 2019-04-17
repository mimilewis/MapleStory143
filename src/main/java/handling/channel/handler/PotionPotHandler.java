/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePotionPot;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStatEffect;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;
import tools.packet.InventoryPacket;

/**
 * @author PlayDK
 */
public class PotionPotHandler {

    /*
     * 使用药剂罐
     */
    public static void PotionPotUse(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPotionPot() == null || !chr.isAlive()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            c.announce(InventoryPacket.showPotionPotMsg(0x00, 0x08)); //0x08 被奇怪的气息所围绕，暂时无法使用道具。
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        //[AC 00] [14 0E 3A 01] [04 00] [60 CE 58 00]
        slea.skip(4);
        short slot = slea.readShort(); //药剂罐在背包的位置
        int itemId = slea.readInt(); //药剂罐道具ID
        Item item = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (item == null || item.getItemId() != itemId || itemId / 10000 != 582) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MaplePotionPot potionPot = chr.getPotionPot();
        int potHp = potionPot.getHp();
        int potMp = potionPot.getMp();
        if (potHp == 0 && potMp == 0) {
            c.announce(InventoryPacket.updataPotionPot(potionPot));
            c.announce(InventoryPacket.showPotionPotMsg(0x00, 0x06)); //0x06 这个药剂罐是空的，请再次填充。
            return;
        }
        boolean usePot = false; //是否使用了药剂罐
        int healHp = chr.getStat().getHealHp();
        int healMp = chr.getStat().getHealMp(chr.getJob());
        int usePotHp = potHp >= healHp ? healHp : potHp;
        int usePotMp = potMp >= healMp ? healMp : potMp;
        if (usePotHp > 0) {
            chr.addHP(usePotHp);
            potionPot.setHp(potHp - usePotHp);
            usePot = true;
        }
        if (usePotMp > 0) {
            chr.addMP(usePotMp);
            potionPot.setMp(potMp - usePotMp);
            usePot = true;
        }
        if (usePot && chr.getMap().getConsumeItemCoolTime() > 0) {
            chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
        }
        c.announce(InventoryPacket.updataPotionPot(potionPot));
        c.announce(InventoryPacket.showPotionPotMsg(usePot ? 0x01 : 0x00, 0x00));
    }

    /*
     * 往药剂罐中加入药水
     */
    public static void PotionPotAdd(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPotionPot() == null || !chr.isAlive()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        //[AD 00] [A6 34 72 01] [02 00] [88 84 1E 00] [17 00 00 00]   血  23个
        //[AD 00] [26 78 72 01] [03 00] [8A 84 1E 00] [21 00 00 00]   蓝  33个
        slea.skip(4);
        short slot = slea.readShort(); //药水在背包的位置
        int itemId = slea.readInt(); //药水道具ID
        short quantity = (short) slea.readInt(); //冲入的数量
        Item toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || toUse.getQuantity() < quantity) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MaplePotionPot potionPot = chr.getPotionPot();
        MapleStatEffect itemEffect = MapleItemInformationProvider.getInstance().getItemEffect(itemId);
        if (itemEffect != null) {
            //当前药水1个能冲入的hp
            int addHp = itemEffect.getHp();
            if (itemEffect.getHpR() > 0) {
                int hp = chr.getStat().getCurrentMaxHp(); //角色当前的最大Hp
                if (hp > 100000) {
                    hp = (int) (itemEffect.getHpR() * 100000) - 1;
                } else {
                    hp = (int) (itemEffect.getHpR() * hp);
                }
                addHp += hp;
            }
            addHp = (int) (addHp * 1.2);
            //当前药水1个能冲入的Mp
            int addMp = itemEffect.getMp();
            if (itemEffect.getMpR() > 0) {
                int mp = chr.getStat().getCurrentMaxMp(chr.getJob()); //角色当前的最大Mp
                if (mp > 100000) {
                    mp = (int) (itemEffect.getMpR() * 100000) - 1;
                } else {
                    mp = (int) (itemEffect.getMpR() * mp);
                }
                addMp += mp;
            }
            addMp = (int) (addMp * 1.2);
            if (addHp <= 0 && addMp <= 0) {
                c.announce(InventoryPacket.updataPotionPot(potionPot));
                c.announce(InventoryPacket.showPotionPotMsg(0x00, 0x00)); //0x00 没有提示。
                return;
            }
            if (potionPot.isFull(addHp, addMp)) {
                c.announce(InventoryPacket.updataPotionPot(potionPot));
                c.announce(InventoryPacket.showPotionPotMsg(0x00, 0x02)); //0x02 这个药剂罐已经满了。
                return;
            }
            //开始检测是否满了
            boolean isFull = false; //药剂罐是否已经满了 或者道具已经使用完成
            short useQuantity = 0;
            while (!isFull) {
                potionPot.addHp(addHp > 0 ? addHp : 0);
                potionPot.addMp(addMp > 0 ? addMp : 0);
                useQuantity++;
                //如果药剂罐已经满了或者使用的道具数量等于封包道具数量
                isFull = potionPot.isFull(addHp, addMp) || useQuantity == quantity;
            }
            if (useQuantity > 0) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, useQuantity, false);
            }
            c.announce(InventoryPacket.updataPotionPot(potionPot));
            c.announce(InventoryPacket.showPotionPotMsg(0x02));
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    /*
     * 药剂罐模式设置 也就是是否自动补充药水
     */
    public static void PotionPotMode(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPotionPot() == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    /*
     * 增加药剂罐的容量上限
     */
    public static void PotionPotIncr(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPotionPot() == null) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        //[AF 00] [BC 5D BB 01] [00] [48 D2 58 00] [03 00]
        slea.skip(4);
        slea.skip(1); //[00] 未知
        int itemId = slea.readInt(); //扩充道具的ID 5821000
        short slot = slea.readShort(); //扩充道具在背包的位置
        Item toUse = chr.getInventory(MapleInventoryType.CASH).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId || itemId != 5821000) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MaplePotionPot potionPot = chr.getPotionPot();
        boolean useItem = potionPot.addMaxValue();
        if (useItem) {
            MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.CASH, slot, (short) 1, false);
        }
        c.announce(InventoryPacket.updataPotionPot(potionPot));
        c.announce(InventoryPacket.showPotionPotMsg(useItem ? 0x03 : 0x00, useItem ? 0x00 : 0x03));
    }
}
