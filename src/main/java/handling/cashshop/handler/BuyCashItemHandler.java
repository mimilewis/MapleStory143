/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.cashshop.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MapleQuestStatus;
import client.inventory.*;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.AutobanManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.cashshop.CashItemFactory;
import server.cashshop.CashItemInfo;
import server.cashshop.CashShop;
import server.quest.MapleQuest;
import tools.*;
import tools.data.input.LittleEndianAccessor;
import tools.packet.MTSCSPacket;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author PlayDK
 */
public class BuyCashItemHandler {

    private static final Logger log = LogManager.getLogger(BuyCashItemHandler.class.getName());

    public static void BuyCashItem(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) throws SQLException {
        int action = slea.readByte() & 0xFF;
        CashShop cs = chr.getCashInventory();
        CashItemFactory cashinfo = CashItemFactory.getInstance();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        switch (action) {
            case 0x03: { // 购买道具    T071 OK
                //[75 01] [03] [00] [B0 B3 32 01] [00]
                int toCharge = slea.readByte() + 1; //是点卷还是抵用卷
                slea.skip(2); //未知 [00]
                int snCS = slea.readInt();
                //后面还有 [01 00 00 00 00 00 00 00]
                // 是否允许抵用券购买
                if (toCharge == 2 && !ServerConfig.CHANNEL_ENABLEPOINTSBUY) {
                    chr.dropMessage(1, "此商品无法使用抵用券购买，请选择其他商品吧。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                CashItemInfo cItem = cashinfo.getItem(snCS);
                if (chr.isShowPacket()) {
                    log.info("商城 => 购买 - 物品 " + snCS + " 是否为空 " + (cItem == null));
                }
                if (cItem != null) {
                    if (snCS == 92000046) {
                        AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买道具.");
                        return;
                    }
                    if (cItem.getItemId() / 1000 == 5533 && !cashinfo.hasRandomItem(cItem.getItemId())) {
                        chr.dropMessage(1, "该道具暂时无法购买，因为找不到对应的箱子信息.");
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        return;
                    }
//                    if (cashinfo.isBlockedCashItemId(cItem.getItem()) || cashinfo.isBlockCashSnId(snCS)) {
//                        chr.dropMessage(1, "该商品禁止购买,请选择其他商品吧.");
//                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//                        return;
//                    }
                    if (chr.getCSPoints(toCharge) < cItem.getPrice()) {
                        chr.dropMessage(1, "点卷余额不足");
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        return;
                    } else if (!cItem.genderEquals(chr.getGender())) { //判断性别是否符合
                        chr.dropMessage(1, "请确认角色名是否错误");
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        return;
                    } else if (cs.getItemsSize() >= 100) { //判断商城道具栏是否有空位
                        chr.dropMessage(1, "保管箱已满");
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        return;
                    }
                    if (cItem.getPrice() <= 0) {
                        AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买道具.");
                        return;
                    }
                    if (chr.isShowPacket()) {
                        log.info("商城 => 购买 - 点卷类型 " + toCharge + " 减少 " + cItem.getPrice());
                    }
                    Item item = cs.toItem(cItem);
                    if (item != null && item.getUniqueId() > 0 && item.getItemId() == cItem.getItemId() && item.getQuantity() == cItem.getCount()) {
                        if (ii.isCash(item.getItemId())) { //检测是否为商城道具
                            chr.modifyCSPoints(toCharge, -cItem.getPrice(), false); //扣掉点卷或者抵用卷
                            cs.addToInventory(item);
                            if (toCharge == 1) {
                                item.setFlag((short) ItemFlag.可以交换1次.getValue()); //设置道具可以交易一次
                            }
                            c.announce(MTSCSPacket.INSTANCE.购买商城道具(item, cItem.getSN(), c.getAccID()));
                            addCashshopLog(chr, cItem.getSN(), cItem.getItemId(), toCharge, cItem.getPrice(), cItem.getCount(), chr.getName() + " 购买道具: " + ii.getName(cItem.getItemId()));
                            if (item.getItemId() == 5820000) { //如果购买的是药剂罐
                                if (chr.getPotionPot() == null) {
                                    MaplePotionPot pot = MaplePotionPot.createPotionPot(chr.getId(), item.getItemId(), item.getExpiration());
                                    if (pot == null) {
                                        chr.dropMessage(1, "创建1个新的药剂罐出现错误.");
                                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                                        return;
                                    }
                                    chr.setPotionPot(pot);
                                }
                                c.announce(MTSCSPacket.INSTANCE.updataPotionPot(chr.getPotionPot()));
                            }
                        } else {
                            log.info("[作弊] " + chr.getName() + " 商城非法购买道具.道具: " + item.getItemId() + " - " + ii.getName(item.getItemId()));
                            AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买道具.");
                        }
                    } else {
                        chr.dropMessage(1, "道具不存在或出现异常，请联系管理员");
                    }
                } else {
                    chr.dropMessage(1, "不存在的道具，请联系管理员");
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x04: { //点击角色信息后选择购物车送礼
                chr.dropMessage(1, "暂不支持，直接选了点送礼吧！");
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x05: { // 加入购物车    T071 OK
                //[75 01] [05] [6A A0 98 00] 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                chr.clearWishlist();
                if (slea.available() < 40) {
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                int[] wishlist = new int[12];
                for (int i = 0; i < 12; i++) {
                    wishlist[i] = slea.readInt();
                }
                chr.setWishlist(wishlist);
                c.announce(MTSCSPacket.INSTANCE.商城购物车(chr, true));
                break;
            }
            case 0x06: { // 增加道具栏    T071 OK
                /*
                 * [75 01] [06] [00] [00] [01] 点卷 增加装备栏 4个位置 需要600的点卷
                 * [75 01] [06] [01] [00] [01] 抵用卷 增加装备栏 4个位置 需要600的抵用卷
                 * [75 01] [06] [01] [00] [02] 抵用卷 增加消耗栏 4个位置 需要600的抵用卷
                 * [75 01] [06] [01] [01] [D2 FD FD 02] 抵用卷 增加装备栏 8个位置 需要1100的抵用卷
                 */
                int toCharge = slea.readByte() + 1;
                boolean coupon = slea.readByte() > 0;
                if (coupon) {
                    int snCS = slea.readInt();
                    CashItemInfo cItem = cashinfo.getItem(snCS);
                    if (cItem == null) {
                        chr.dropMessage(1, "未知错误");
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        break;
                    }
                    int types = (cItem.getItemId() - 9110000) / 1000;
                    MapleInventoryType type = MapleInventoryType.getByType((byte) types);
                    if (chr.isShowPacket()) {
                        System.out.println("增加道具栏  snCS " + snCS + " 扩充: " + types);
                    }
                    if (chr.getCSPoints(toCharge) >= 1100 && chr.getInventory(type).getSlotLimit() < 121) {
                        chr.modifyCSPoints(toCharge, -1100, false);
                        chr.getInventory(type).addSlot((byte) 8);
                        //chr.dropMessage(1, "扩充成功，当前栏位: " + chr.getInventory(type).getSlotLimit() + " 个。");
                        c.announce(MTSCSPacket.INSTANCE.扩充道具栏(type.getType(), chr.getInventory(type).getSlotLimit()));
                    } else {
                        chr.dropMessage(1, "扩充失败，点卷余额不足或者栏位已超过上限。");
                    }
                } else {
                    MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                    if (chr.getCSPoints(toCharge) >= 600 && chr.getInventory(type).getSlotLimit() < 125) {
                        chr.modifyCSPoints(toCharge, -600, false);
                        chr.getInventory(type).addSlot((byte) 4);
                        //chr.dropMessage(1, "扩充成功，当前栏位: " + chr.getInventory(type).getSlotLimit() + " 个。");
                        c.announce(MTSCSPacket.INSTANCE.扩充道具栏(type.getType(), chr.getInventory(type).getSlotLimit()));
                    } else {
                        chr.dropMessage(1, "扩充失败，点卷余额不足或者栏位已超过上限。");
                    }
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x07: { // 增加仓库容量    T071 OK
                /*
                 * [75 01] [07] [00] [00] 点卷 4个位置 需要600的点卷
                 * [75 01] [07] [01] [00] 抵用卷 4个位置 需要600的抵用卷
                 * [75 01] [07] [01] [01] D1 FD FD 02 抵用卷 购买商城物品扩充8个位置 需要1100的抵用卷
                 */
                int toCharge = slea.readByte() + 1;
                int coupon = slea.readByte() > 0 ? 2 : 1;
                if (chr.getCSPoints(toCharge) >= (coupon == 2 ? 1100 : 600) && chr.getStorage().getSlots() < (129 - (4 * coupon))) {
                    chr.modifyCSPoints(toCharge, (coupon == 2 ? -1100 : -600), false);
                    chr.getStorage().increaseSlots((byte) (4 * coupon));
                    chr.getStorage().saveToDB();
                    //chr.dropMessage(1, "仓库扩充成功，当前栏位: " + chr.getStorage().getSlots() + " 个。");
                    c.announce(MTSCSPacket.INSTANCE.扩充仓库(chr.getStorage().getSlots()));
                } else {
                    chr.dropMessage(1, "仓库扩充失败，点卷余额不足或者栏位已超过上限 96 个位置。");
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x08: { // 角色增加会员卡    T071 OK
                /*
                 * [90 02] [08] [00] [A9 45 C7 07 00 00 00 00] 需要3000抵用卷
                 */
                int toCharge = slea.readByte() + 1;
                int snCS = slea.readInt();
                CashItemInfo item = cashinfo.getItem(snCS, false);
                int slots = c.getAccCharSlots();
                if (item == null || item.getItemId() != 5430000) {
                    String msg = "角色栏扩充失败，找不到指定的道具信息或者道具ID不正确。";
                    if (chr.isAdmin()) {
                        msg = item == null ? "角色栏扩充失败:\r\n找不到道具的信息或者道具没有出售\r\n当前道具的SNid: " + snCS : "角色栏扩充失败:\r\n道具ID是否正确: " + (item.getItemId() == 5430000) + " 当前ID：" + item.getItemId();
                    }
                    chr.dropMessage(1, msg);
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                if (chr.getCSPoints(toCharge) < item.getPrice() || slots >= 36) {
                    chr.dropMessage(1, "角色栏扩充失败，点卷余额不足或者栏位已超过上限。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                if (c.gainAccCharSlot()) {
                    chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                    chr.dropMessage(1, "角色栏扩充成功，当前栏位: " + (slots + 1));
                } else {
                    chr.dropMessage(1, "角色栏扩充失败。");
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x09: { // 购买角色卡
                /*
                 * [75 01] [09] [01] [0A FE FD 02] 角色卡 50级 需要1900抵用卷
                 * [75 01] [09] [01] [0B FE FD 02] 角色卡100级 需要9900抵用卷
                 */
//                int toCharge = slea.readByte() + 1;
//                int sn = slea.readInt();
//                CashItemInfo item = cashinfo.getItem(sn);
                chr.dropMessage(1, "暂时不支持。");
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x0A: {  // 项链扩充    V.111.1修改 以前0x0A
                /*
                 * [7F 01] [0A] [01] [FA FD FD 02] 项链扩充7天 500抵用卷
                 * [7F 01] [0A] [00] [F9 FD FD 02] 项链扩充30天 1500点卷
                 * [7F 01] [0A] [01] [F9 FD FD 02] 项链扩充30天 1500抵用卷
                 */
                int toCharge = slea.readByte() + 1;
                int sn = slea.readInt();
                CashItemInfo item = cashinfo.getItem(sn);
                if (item == null || chr.getCSPoints(toCharge) < item.getPrice() * 10 || item.getItemId() / 10000 != 555) {
                    chr.dropMessage(1, "项链扩充失败，点卷余额不足或者出现其他错误。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                MapleQuestStatus marr = chr.getQuestNoAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT));
                if (marr != null && marr.getCustomData() != null && Long.parseLong(marr.getCustomData()) >= System.currentTimeMillis()) {
                    chr.dropMessage(1, "项链扩充失败，您已经进行过项链扩充。");
                } else {
                    long days = 0;
                    if (item.getItemId() == 5550000) { //项链扩充（30天权）
                        days = 30;
                    } else if (item.getItemId() == 5550001) {  //项链扩充（7天权）
                        days = 7;
                    }
                    String customData = String.valueOf(System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000));
                    chr.getQuestNAdd(MapleQuest.getInstance(GameConstants.PENDANT_SLOT)).setCustomData(customData);
                    chr.modifyCSPoints(toCharge, -item.getPrice() * 10, false);
                    chr.dropMessage(1, "项链扩充成功，本次扩充花费:\r\n" + (toCharge == 1 ? "点卷" : "抵用卷") + item.getPrice() * 10 + " 点，持续时间为: " + days + " 天。");
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x0D: { //删除商城道具
                int uniqueId = (int) slea.readLong();
                Item item = cs.findByCashId(uniqueId);
                if (item == null) {
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    if (chr.isShowPacket()) {
                        System.out.println("删除商城道具 - 道具为空 删除失败");
                    }
                    return;
                }
                cs.removeFromInventory(item);
                c.announce(MTSCSPacket.INSTANCE.商城删除道具(uniqueId));
                break;
            }
            case 0x0E: { // 商城 => 背包    V.111.1修改 以前0x0E
                Item item = cs.findByCashId((int) slea.readLong());
                if (chr.isShowPacket()) {
                    log.info("商城 => 背包 - 道具是否为空 " + (item == null));
                }
                if (item == null) {
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                if (chr.getInventory(ItemConstants.getInventoryType(item.getItemId())).addItem(item) != -1) {
                    cs.removeFromInventory(item);
                    c.announce(MTSCSPacket.INSTANCE.moveItemToInvFormCs(item));
                    if (chr.isShowPacket()) {
                        log.info("商城 => 背包 - 移动成功");
                    }
                }
                break;
            }
            case 0x0F: { // 背包 => 商城    V.111.1修改 以前0x0F
                int cashId = (int) slea.readLong();
                MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                Item item = chr.getInventory(type).findByUniqueId(cashId);
                if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && chr.getCashInventory().getItemsSize() < 100) {
                    Item item_ = item.copy();
                    int sn = cashinfo.getSnFromId(item_.getItemId());
                    chr.getInventory(type).removeItem(item.getPosition(), item.getQuantity(), false);
                    item_.setPosition((byte) 0);
                    chr.getCashInventory().addToInventory(item_);
                    c.announce(MTSCSPacket.INSTANCE.moveItemToCsFromInv(item_, c.getAccID(), sn));
                } else {
                    chr.dropMessage(1, "移动失败。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                }
                break;
            }
            case 0x1F: { // 换购 V.111.1修改 以前为0x1F
                /*
                 * 7F 01
                 * 1D
                 * 01 00 20
                 * 46 00 00 00 00 00 00 00 - uniqueid
                 */
                slea.readMapleAsciiString(); //[01 00 20]
                int toCharge = 2; //是点卷还是抵用卷
                int uniqueId = (int) slea.readLong();
                Item item = cs.findByCashId(uniqueId);
                if (item == null) {
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                int snCS = cashinfo.getSnFromId(item.getItemId());
                CashItemInfo cItem = cashinfo.getItem(snCS, false);
                if (cItem == null || cashinfo.isBlockRefundableItemId(item.getItemId())) {
                    if (chr.isAdmin()) {
                        if (cItem == null) {
                            chr.dropMessage(1, "换购失败:\r\n道具是否为空: " + (cItem == null));
                        } else {
                            chr.dropMessage(1, "换购失败:\r\n道具禁止回购: " + cashinfo.isBlockRefundableItemId(item.getItemId()));
                        }
                    } else {
                        chr.dropMessage(1, "换购失败，当前道具不支持换购。");
                    }
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                if (!ii.isCash(cItem.getItemId())) {
                    AutobanManager.getInstance().autoban(chr.getClient(), "商城非法换购道具.");
                    return;
                }
                int Money = cItem.getPrice() / 10 * 3; //获得的抵用券价格是原价*0.3
                cs.removeFromInventory(item);
                chr.modifyCSPoints(toCharge, Money, false);
                c.announce(MTSCSPacket.INSTANCE.商城换购道具(uniqueId, Money));
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x22: //V.111.1修改 以前 [0x23 结婚戒指] [0x29 友谊戒指]
            case 0x28: {
                slea.readMapleAsciiString(); //[01 00 20]
                slea.skip(1); //[00] V.114.1新增
                int toCharge = 1;
                int snCS = slea.readInt();
                CashItemInfo item = cashinfo.getItem(snCS);
                slea.skip(4); //[00 00 00 00] V.114.1新增
                String partnerName = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                if (item == null || !ItemConstants.isEffectRing(item.getItemId()) || chr.getCSPoints(toCharge) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x00));
                    return;
                } else if (!item.genderEquals(chr.getGender())) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x07));
                    return;
                } else if (chr.getCashInventory().getItemsSize() >= 100) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x18));
                    return;
                } else if (!ii.isCash(item.getItemId())) { //非商城道具
                    AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买戒指道具.");
                    return;
                }
//                if (cashinfo.isBlockedCashItemId(item.getItem()) || cashinfo.isBlockCashSnId(snCS)) {
//                    chr.dropMessage(1, "该道具禁止购买.");
//                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//                    return;
//                }
                Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, chr.getWorld()); //[角色ID 帐号ID 性别]
                if (info == null || info.getLeft() <= 0) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x07));
                } else if (info.getMid() == c.getAccID() || info.getLeft() == chr.getId()) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x06));
                } else {
                    if (info.getRight() == chr.getGender() && action == 0x23) {
                        c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x1A));
                        return;
                    }
                    int err = MapleRing.createRing(item.getItemId(), chr, partnerName, msg, info.getLeft(), item.getSN());
                    if (err != 1) {
                        c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x01));
                        return;
                    }
                    chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                    c.announce(MTSCSPacket.INSTANCE.商城送礼(item.getItemId(), item.getCount(), partnerName));
                    addCashshopLog(chr, item.getSN(), item.getItemId(), toCharge, item.getPrice(), item.getCount(), chr.getName() + " 购买戒指: " + ii.getName(item.getItemId()) + " 送给 " + partnerName);
                    chr.sendNote(partnerName, partnerName + " 您已收到" + chr.getName() + "送给您的礼物，请进入现金商城查看！");
                    int chz = WorldFindService.getInstance().findChannel(partnerName);
                    if (chz > 0) {
                        MapleCharacter receiver = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterByName(partnerName);
                        if (receiver != null) {
                            receiver.showNote();
                        }
                    }
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr)); //刷新下免得卡住
                break;
            }
            case 0x23: { // 购买礼包   V.111.1修改 以前 0x24
                //[75 01] [22] [00] [65 1E 2C 04]
                int toCharge = slea.readByte() + 1; //是点卷还是抵用卷
                int snCsId = slea.readInt(); //礼包的SNid
                slea.readInt(); //礼包道具的数量  int count = 
                if (snCsId == 10200551 || snCsId == 10200552 || snCsId == 10200553) {
                    chr.dropMessage(1, "当前服务器未开放购买商城活动栏里面的道具.");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
//                if (cashinfo.isBlockCashSnId(snCsId)) {
//                    chr.dropMessage(1, "该礼包禁止购买.");
//                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//                    return;
//                }
                CashItemInfo item = cashinfo.getItem(snCsId, false);
                List<Integer> packageIds = null;
                if (item != null) {
                    packageIds = cashinfo.getPackageItems(item.getItemId());
                }
                if (item == null || packageIds == null) {
                    String msg = "未知错误";
                    if (chr.isAdmin()) {
                        if (item == null) {
                            msg += "\r\n\r\n 礼包道具信息为空";
                        }
                        if (packageIds == null) {
                            msg += "\r\n\r\n 礼包道具里面的物品道具为空";
                        }
                    }
                    chr.dropMessage(1, msg);
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (chr.getCSPoints(toCharge) < item.getPrice()) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x03));
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (!item.genderEquals(c.getPlayer().getGender())) {
                    chr.dropMessage(1, "性别不符合");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - packageIds.size())) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x18));
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                if (item.getPrice() <= 0) {
                    AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买礼包道具.");
                    return;
                }
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
                Map<Integer, Item> packageItems = new HashMap<>(); //定义最终发送的礼包道具信息
                for (int i : packageIds) {
                    CashItemInfo cii = cashinfo.getSimpleItem(i);
                    if (cii == null) {
                        continue;
                    }
                    Item itemz = chr.getCashInventory().toItem(cii);
                    if (itemz == null || itemz.getUniqueId() <= 0) {
                        continue;
                    }
//                    if (cashinfo.isBlockedCashItemId(item.getItem())) {
//                        continue;
//                    }
                    if (!ii.isCash(itemz.getItemId())) {
                        log.info("[作弊] " + chr.getName() + " 商城非法购买礼包道具.道具: " + itemz.getItemId() + " - " + ii.getName(itemz.getItemId()));
                        AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买礼包道具.");
                        continue;
                    }
                    packageItems.put(i, itemz);
                    chr.getCashInventory().addToInventory(itemz);
                    addCashshopLog(chr, snCsId, itemz.getItemId(), toCharge, item.getPrice(), itemz.getQuantity(), chr.getName() + " 购买礼包: " + ii.getName(itemz.getItemId()) + " - " + i);
                }
                c.announce(MTSCSPacket.INSTANCE.商城购买礼包(packageItems, c.getAccID()));
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x24: { //商城送礼包   V.111.1修改 以前0x25
                slea.readMapleAsciiString(); //[01 00 20]
                int snCsId = slea.readInt(); //礼包的SNID
                CashItemInfo item = cashinfo.getItem(snCsId);
                String partnerName = slea.readMapleAsciiString();
                String msg = slea.readMapleAsciiString();
                if (item == null || chr.getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x03));
                    return;
                }
//                if (cashinfo.isBlockCashSnId(snCsId)) {
//                    chr.dropMessage(1, "该礼包禁止购买.");
//                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//                    return;
//                }
                Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, chr.getWorld());
                if (info == null || info.getLeft() <= 0) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x07)); //请确认角色名是否错误。
                } else if (info.getLeft() == chr.getId() || info.getMid() == c.getAccID()) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x06)); //无法向本人的账号赠送礼物。请用该角色登陆，然后购买。
                } else if (!item.genderEquals(info.getRight())) {
                    c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x08)); //此道具对性别有限制。请确认接收人的性别。
                } else {
                    if (item.getPrice() <= 0) {
                        AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买礼包道具.");
                        return;
                    }
                    chr.getCashInventory().gift(info.getLeft(), chr.getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
                    chr.modifyCSPoints(1, -item.getPrice(), false);
                    //chr.dropMessage(1, "您成功的将礼包送给[" + partnerName + "]花费点卷" + item.getUnitPrice() + "点.");
                    c.announce(MTSCSPacket.INSTANCE.商城送礼包(item.getItemId(), item.getCount(), partnerName));
                    chr.sendNote(partnerName, partnerName + " 您已收到" + chr.getName() + "送给您的礼物，请进入现金商城查看！");
                    addCashshopLog(chr, item.getSN(), item.getItemId(), 1, item.getPrice(), item.getCount(), chr.getName() + " 赠送礼包给 " + partnerName);
                    int chz = WorldFindService.getInstance().findChannel(partnerName);
                    if (chz > 0) {
                        MapleCharacter receiver = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterByName(partnerName);
                        if (receiver != null) {
                            receiver.showNote();
                        }
                    }
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr)); //刷新下免得卡住
                break;
            }
            case 0x25: { // 购买任务道具    V.111.1修改 以前0x26
                CashItemInfo item = cashinfo.getItem(slea.readInt());
                if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getItemId())) {
                    chr.dropMessage(1, "该道具不是任务物品");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (chr.getMeso() < item.getPrice() || item.getPrice() <= 0) {
                    chr.dropMessage(1, "金币不足");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (chr.getItemQuantity(item.getItemId()) > 0) {
                    chr.dropMessage(1, "你已经有这个道具\r\n不能购买.");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                } else if (chr.getInventory(ItemConstants.getInventoryType(item.getItemId())).getNextFreeSlot() < 0) {
                    chr.dropMessage(1, "背包空间不足");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
//                if (cashinfo.isBlockedCashItemId(item.getItem())) {
//                    chr.dropMessage(1, CashShopServer.getCashBlockedMsg(item.getItem()));
//                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//                    return;
//                }
                if (item.getItemId() == 4031063 || item.getItemId() == 4031191 || item.getItemId() == 4031192) {
                    byte pos = MapleInventoryManipulator.addId(c, item.getItemId(), (short) item.getCount(), null, "商城: 任务物品" + " 在 " + DateUtil.getCurrentDate());
                    if (pos < 0) {
                        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                        return;
                    }
                    chr.gainMeso(-item.getPrice(), false);
                    c.announce(MTSCSPacket.INSTANCE.updataMeso(chr));
                    c.announce(MTSCSPacket.INSTANCE.商城购买任务道具(item.getPrice(), (short) item.getCount(), pos, item.getItemId()));
                } else {
                    AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买任务道具.");
                }
                break;
            }
            case 0x32: { //V.111.1 以前为0x31
                slea.readByte(); //00 未知
                int snCS = slea.readInt();
                slea.readInt(); //01 00 00 00 数量?
                if (snCS == 50200031 && chr.getCSPoints(1) >= 500) {
                    chr.modifyCSPoints(1, -500, false);
                    chr.modifyCSPoints(2, 500, false);
                    chr.dropMessage(1, "兑换抵用卷成功");
                } else if (snCS == 50200032 && chr.getCSPoints(1) >= 1000) {
                    chr.modifyCSPoints(1, -1000, false);
                    chr.modifyCSPoints(2, 1000, false);
                    chr.dropMessage(1, "兑换抵用卷成功");
                } else if (snCS == 50200033 && chr.getCSPoints(1) >= 5000) {
                    chr.modifyCSPoints(1, -5000, false);
                    chr.modifyCSPoints(2, 5000, false);
                    chr.dropMessage(1, "兑换抵用卷成功");
                } else {
                    chr.dropMessage(1, "没有找到这个道具的信息。");
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x31: { // 这个很重要 不发送这个包无法购买商城道具 V.111.1修改 以前是0x33
                c.announce(MTSCSPacket.INSTANCE.redeemResponse());
                break;
            }
            case 0x40: { //在商城中打开箱子 T071修改 以前是0x3E
                // [B7 01] [3E] [23 00 00 00 00 00 00 00]
                long uniqueId = slea.readLong();
                Item boxItem = cs.findByCashId((int) uniqueId);
                if (boxItem == null || !cashinfo.hasRandomItem(boxItem.getItemId())) {
                    chr.dropMessage(1, "打开箱子失败，服务器找不到对应的道具信息。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                List<Pair<Integer, Integer>> boxItemSNs = cashinfo.getRandomItem(boxItem.getItemId());
                if (boxItemSNs.isEmpty()) {
                    chr.dropMessage(1, "打开箱子失败，服务器找不到对应的道具信息。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                Pair<Integer, Integer> snCS = boxItemSNs.get(Randomizer.nextInt(boxItemSNs.size()));
                CashItemInfo cItem = cashinfo.getItem(snCS.getLeft(), false);
                if (cItem != null) {
                    Item item = cs.toItem(cItem);
                    if (item != null && item.getUniqueId() > 0 && item.getItemId() == cItem.getItemId() && item.getQuantity() == cItem.getCount()) {
                        if (chr.getInventory(ItemConstants.getInventoryType(item.getItemId())).addItem(item) != -1) {
                            cs.removeFromInventory(boxItem);
                            item.setFlag((short) ItemFlag.可以交换1次.getValue());
                            c.announce(MTSCSPacket.INSTANCE.商城打开箱子(item, uniqueId));
                        } else {
                            chr.dropMessage(1, "打开箱子失败，请确认背包是否有足够的空间。");
                        }
                    }
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0x52: { //抵用券兑换道具
                slea.skip(1);
                int snID = slea.readInt();
                int count = slea.readInt();
                CashItemInfo cItem = CashItemFactory.getInstance().getItem(snID, false);
                if (cItem == null || count < 1) {
                    chr.dropMessage(1, "暂时不能购买该道具。");
                    c.announce((MTSCSPacket.INSTANCE.updateCouponsInfo(chr)));
                    return;
                }
                int price = cItem.getPrice() * count;
                if (chr.getCSPoints(1) < price) {
                    chr.dropMessage(1, "点券不足无法购买。");
                    c.announce((MTSCSPacket.INSTANCE.updateCouponsInfo(chr)));
                    return;
                } else {
                    chr.modifyCSPoints(1, -price);
                    chr.modifyCSPoints(2, price);
                    c.announce(MTSCSPacket.INSTANCE.抵用券兑换道具());
                }
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            case 0xA2: {
                final byte toCharge = (byte) (slea.readByte() + 1);
                final CashItemInfo cItem = CashItemFactory.getInstance().getItem(slea.readInt());
                if (cItem == null || chr.getCSPoints(toCharge) < cItem.getPrice() || cItem.getItemId() / 10000 != 504) {
                    chr.dropMessage(1, "点券余额不足或者出现其他错误,可能被禁止购买。");
                    c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                    return;
                }
                chr.modifyCSPoints(toCharge, -cItem.getPrice(), false);
                chr.set超时空券(chr.get超时空券() + cItem.getCount());
                chr.dropMessage(1, "购买超时空成功\r\n消耗 " + cItem.getPrice() + (toCharge == 1 ? " 点券" : " 抵用卷") + "\r\n目前超时空券数量为" + chr.get超时空券() + "。");
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                break;
            }
            default:
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                System.out.println("商城操作未知的操作类型: 0x" + StringUtil.getLeftPaddedStr(Integer.toHexString(action).toUpperCase(), '0', 2) + " " + slea.toString());
                break;
        }
    }

    public static void 商城送礼(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        slea.readMapleAsciiString(); //[01 00 20]
        int snCS = slea.readInt();
        CashItemFactory cashinfo = CashItemFactory.getInstance();
        CashItemInfo item = cashinfo.getItem(snCS);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        String partnerName = slea.readMapleAsciiString();
        String msg = slea.readMapleAsciiString();
        if (snCS == 92000046) {
            AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买道具.");
            return;
        }
//        if (cashinfo.isBlockedCashItemId(item.getItem()) || cashinfo.isBlockCashSnId(snCS)) {
//            chr.dropMessage(1, "该道具禁止购买.");
//            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
//            return;
//        }
        //System.out.println("商城 => 送礼 - 送给: " + partnerName + " 信息: " + msg);
        if (item == null || chr.getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1) { //dont want packet editors gifting random stuff =P
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x03));
            //System.out.println("商城送礼: 错误 - 1");
            return;
        }
        Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, chr.getWorld());
        if (info == null || info.getLeft() <= 0) {
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x07)); //请确认角色名是否错误。
            //System.out.println("商城送礼: 错误 - 2");
        } else if (info.getLeft() == chr.getId() || info.getMid() == c.getAccID()) {
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x06)); //无法向本人的账号赠送礼物。请用该角色登陆，然后购买。
            //System.out.println("商城送礼: 错误 - 3");
        } else if (!item.genderEquals(info.getRight())) {
            c.announce(MTSCSPacket.INSTANCE.商城错误提示(0x08)); //此道具对性别有限制。请确认接收人的性别。
            //System.out.println("商城送礼: 错误 - 4");
        } else {
            if (!ii.isCash(item.getItemId())) {
                log.info("[作弊] " + chr.getName() + " 商城非法购买礼物道具.道具: " + item.getItemId() + " - " + ii.getName(item.getItemId()));
                chr.dropMessage(1, "购买商城礼物道具出现错误.");
                c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
                //AutobanManager.getInstance().autoban(chr.getClient(), "商城非法购买道具.");
                return;
            }
            if (item.getPrice() <= 0) {
                AutobanManager.getInstance().autoban(chr.getClient(), "商城非法赠送礼包道具.");
                return;
            }
            //System.out.println("商城送礼: OK");
            chr.getCashInventory().gift(info.getLeft(), chr.getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
            chr.modifyCSPoints(1, -item.getPrice(), false);
            c.announce(MTSCSPacket.INSTANCE.商城送礼(item.getItemId(), item.getCount(), partnerName));
            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
            addCashshopLog(chr, item.getSN(), item.getItemId(), 1, item.getPrice(), item.getCount(), chr.getName() + " 购买道具: " + ii.getName(item.getItemId()) + " 送给 " + partnerName);
            chr.sendNote(partnerName, partnerName + " 您已收到" + chr.getName() + "送给您的礼物，请进入现金商城查看！");
            int chz = WorldFindService.getInstance().findChannel(partnerName);
            if (chz > 0) {
                MapleCharacter receiver = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterByName(partnerName);
                if (receiver != null) {
                    receiver.showNote();
                }
            }
        }
    }

    /*
     * 打开闪耀的随机箱
     */
    public static void openAvatarRandomBox(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null) {
            return;
        }
        CashShop cs = chr.getCashInventory();
        Item item = cs.findByCashId((int) slea.readLong());
        if (item == null || item.getItemId() != 5222036) {
            c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
            return;
        }
        chr.dropMessage(1, "当前游戏不支持此功能");
        c.announce(MTSCSPacket.INSTANCE.updateCouponsInfo(chr));
    }

    private static void addCashshopLog(MapleCharacter chr, int SN, int itemId, int type, int price, int count, String itemLog) {
        if (chr == null) {
            return;
        }
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO cashshop_log (accId, chrId, name, SN, itemId, type, price, count, cash, points, itemlog) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, chr.getAccountID());
            ps.setInt(2, chr.getId());
            ps.setString(3, chr.getName());
            ps.setInt(4, SN);
            ps.setInt(5, itemId);
            ps.setInt(6, type);
            ps.setInt(7, price);
            ps.setInt(8, count);
            ps.setInt(9, chr.getCSPoints(1));
            ps.setInt(10, chr.getCSPoints(2));
            ps.setString(11, itemLog);
            ps.execute();
            ps.close();
        } catch (SQLException e) {
            log.error("玩家: " + chr.getName() + " ID: " + chr.getId() + " 购买商城道具保存日志出错.", e);
        }
    }
}
