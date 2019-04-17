/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.cashshop.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.inventory.Item;
import constants.ItemConstants;
import server.MapleInventoryManipulator;
import server.cashshop.CashItemFactory;
import server.cashshop.CashItemInfo;
import tools.DateUtil;
import tools.Triple;
import tools.data.input.LittleEndianAccessor;
import tools.packet.MTSCSPacket;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PlayDK
 */
public class CouponCodeHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        String toPlayer = slea.readMapleAsciiString();
        if (toPlayer.length() > 0) {
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x16));
            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
            return;
        }
        String code = slea.readMapleAsciiString();
        if (code.length() <= 0) {
            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
            return;
        }
        Triple<Boolean, Integer, Integer> info = null;
        try {
            info = MapleCharacterUtil.getNXCodeInfo(code);
        } catch (SQLException e) {
            System.out.print("错误 getNXCodeInfo" + e);
        }
        if (info != null && info.left) {
            int type = info.mid, item = info.right;
            try {
                MapleCharacterUtil.setNXCodeUsed(chr.getName(), code);
            } catch (SQLException e) {
                System.out.print("错误 setNXCodeUsed" + e);
                c.announce(MTSCSPacket.INSTANCE.商城错误提示(0));
                return;
            }
            /*
             * Explanation of type! Basically, this makes coupon codes do different things!
             *
             * Type 1: 点卷,
             * Type 2: 抵用卷
             * Type 3: 物品道具.. use SN
             * Type 4: 金币
             */
            Map<Integer, Item> itemz = new HashMap<>();
            int maplePoints = 0, mesos = 0;
            switch (type) {
                case 1: //抵用卷
                case 2:
                    c.getPlayer().modifyCSPoints(type, item, false);
                    maplePoints = item;
                    break;
                case 3:
                    CashItemInfo itez = CashItemFactory.getInstance().getItem(item);
                    if (itez == null) {
                        c.announce(MTSCSPacket.INSTANCE.商城错误提示(0));
                        return;
                    }
                    byte slot = MapleInventoryManipulator.addId(c, itez.getItemId(), (short) 1, "", "商城道具卡兑换 时间: " + DateUtil.getCurrentDate());
                    if (slot <= -1) {
                        c.announce(MTSCSPacket.INSTANCE.商城错误提示(0));
                        return;
                    } else {
                        itemz.put(item, chr.getInventory(ItemConstants.getInventoryType(item)).getItem(slot));
                    }
                    break;
                case 4: //金币
                    chr.gainMeso(item, false);
                    mesos = item;
                    break;
            }
            c.announce(MTSCSPacket.INSTANCE.showCouponRedeemedItem(itemz, mesos, maplePoints, c));
            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
        } else {
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(info == null ? 0x0E : 0x10));
        }
    }
}
