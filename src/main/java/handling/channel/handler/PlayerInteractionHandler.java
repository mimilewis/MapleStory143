package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import configs.ServerConfig;
import constants.ItemConstants;
import constants.ServerConstants;
import handling.opcode.InteractionOpcode;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleTrade;
import server.maps.FieldLimitType;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.shops.*;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.data.input.LittleEndianAccessor;
import tools.packet.PlayerShopPacket;

import java.util.Arrays;

public class PlayerInteractionHandler {

    private static final Logger log = LogManager.getLogger(PlayerInteractionHandler.class);

    public static void PlayerInteraction(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte mode = slea.readByte();
        InteractionOpcode action = InteractionOpcode.getByAction(mode);
        if (chr == null || action == null || c.getChannelServer().isShutdown()) {
            if (ServerConstants.isShowPacket()) {
                System.out.println("玩家互动未知的操作类型: 0x" + StringUtil.getLeftPaddedStr(Integer.toHexString(mode).toUpperCase(), '0', 2) + " " + slea.toString());
            }
            c.sendEnableActions();
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.sendEnableActions();
            return;
        }
        chr.setScrolledPosition((short) 0);
        if (chr.isAdmin()) {
            chr.dropMessage(5, "玩家互动操作类型: " + action);
        }
        switch (action) {
            case 创建: { //创建
                byte createType = slea.readByte();
                if (createType == 4) { //交易
                    if (ServerConfig.WORLD_BANTRADE) {
                        chr.dropMessage(1, "管理员禁用了交易功能.");
                        c.sendEnableActions();
                        return;
                    }
                    MapleTrade.startTrade(chr);
                } else if (createType == 1 || createType == 2 || createType == 5 || createType == 6 || createType == 0x4B) { // shop
                    if (!chr.getMap().getMapObjectsInRange(chr.getTruePosition(), 20000, Arrays.asList(MapleMapObjectType.SHOP, MapleMapObjectType.HIRED_MERCHANT)).isEmpty() || !chr.getMap().getPortalsInRange(chr.getTruePosition(), 20000).isEmpty()) {
                        chr.dropMessage(1, "无法在这个地方使用.");
                        c.sendEnableActions();
                        return;
                    } else if (createType == 1 || createType == 2) {
                        if (FieldLimitType.Minigames.check(chr.getMap().getFieldLimit()) || chr.getMap().allowPersonalShop()) {
                            chr.dropMessage(1, "无法在这个地方使用.");
                            c.sendEnableActions();
                            return;
                        }
                    }
                    String desc = slea.readMapleAsciiString();
                    String pass = "";
                    if (slea.readByte() > 0) {
                        pass = slea.readMapleAsciiString();
                    }
                    if (createType == 1 || createType == 2) {
                        slea.readShort(); //item pos
                        int piece = slea.readByte();
                        int itemId = createType == 1 ? (4080000 + piece) : 4080100;
                        MapleMiniGame game = new MapleMiniGame(chr, itemId, desc, pass, createType); //itemid
                        game.setPieceType(piece);
                        chr.setPlayerShop(game);
                        game.setAvailable(true);
                        game.setOpen(true);
                        game.send(c);
                        chr.getMap().addMapObject(game);
                        game.update();
                    } else if (chr.getMap().allowPersonalShop() || createType == 0x4B && chr.getMap().allowFishing()) {
                        Item shop = c.getPlayer().getInventory(MapleInventoryType.CASH).getItem((byte) slea.readShort());
                        if (shop == null || shop.getQuantity() <= 0 || shop.getItemId() != slea.readInt()) {
                            return;
                        }
                        if (createType == 5) { //玩家自己贩卖道具
                            MaplePlayerShop mps = new MaplePlayerShop(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(mps);
                            chr.getMap().addMapObject(mps);
                            c.announce(PlayerShopPacket.getPlayerStore(chr, true));
                        } else if (HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) { //雇佣商店
                            //剩余6个封包 前2位是 道具在背包的位置 后面四位是道具ID
                            HiredMerchant merch = new HiredMerchant(chr, shop.getItemId(), desc);
                            chr.setPlayerShop(merch);
                            chr.getMap().addMapObject(merch);
                            c.announce(PlayerShopPacket.getHiredMerch(chr, merch, true));
                        } else if (createType == 75 && HiredFisherHandler.INSTANCE.check(c, false)) {
                            c.announce(PlayerShopPacket.FishNotice());
                            int time = shop.getItemId() >= 5601000 && shop.getItemId() <= 5601002 ? (shop.getItemId() - 5601000 + 1) * 6 : 0;
                            HiredFisher fisher = new HiredFisher(chr, shop.getItemId(), time);
                            fisher.setId(c.getChannelServer().addFisher(fisher));
                            chr.setHiredFisher(fisher);
                            if (fisher.isHiredFisherItemId()) {
                                chr.removeItem(shop.getItemId(), 1);
                            }
                            chr.getMap().addMapObject(fisher);
                            c.announce(PlayerShopPacket.spawnHiredMerchant(fisher));
                            c.announce(PlayerShopPacket.FishExit());
                            c.announce(PlayerShopPacket.updateHiredMerchant(fisher));
                            break;
                        }
                    }
                }
                break;
            }
            case 交易邀请: {
                if (chr.getMap() == null) {
                    return;
                }
                MapleCharacter chrr = chr.getMap().getCharacterById(slea.readInt());
                if (chrr == null || c.getChannelServer().isShutdown() || chrr.hasBlockedInventory()) {
                    c.sendEnableActions();
                    return;
                }
                MapleTrade.inviteTrade(chr, chrr);
                break;
            }
            case 拒绝邀请: {
                MapleTrade.declineTrade(chr);
                break;
            }
            case 访问: { //访问
                if (c.getChannelServer().isShutdown()) {
                    c.sendEnableActions();
                    return;
                }
                if (chr.getTrade() != null && chr.getTrade().getPartner() != null && !chr.getTrade().inTrade()) {
                    MapleTrade.visitTrade(chr, chr.getTrade().getPartner().getChr());
                } else if (chr.getMap() != null && chr.getTrade() == null) {
                    int obid = slea.readInt();
                    MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                    if (ob == null) {
                        ob = chr.getMap().getMapObject(obid, MapleMapObjectType.SHOP);
                    }
                    if (ob instanceof IMaplePlayerShop && chr.getPlayerShop() == null) {
                        IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                        if (ob instanceof HiredMerchant) {
                            HiredMerchant merchant = (HiredMerchant) ips;
                            if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                                merchant.setOpen(false);
                                merchant.removeAllVisitors(0x14, (byte) 1);
                                chr.setPlayerShop(ips);
                                c.announce(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                            } else {
                                if (!merchant.isOpen() || !merchant.isAvailable()) {
                                    chr.dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                                } else {
                                    if (ips.getFreeSlot() == -1) {
                                        chr.dropMessage(1, "店铺已达到最大人数\r\n请稍后再度光临！");
                                    } else if (merchant.isInBlackList(chr.getName())) {
                                        chr.dropMessage(1, "你被禁止进入该店铺");
                                    } else {
                                        chr.setPlayerShop(ips);
                                        merchant.addVisitor(chr);
                                        c.announce(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                                    }
                                }
                            }
                        } else {
                            if (ips instanceof MaplePlayerShop && ((MaplePlayerShop) ips).isBanned(chr.getName())) {
                                c.announce(PlayerShopPacket.shopErrorMessage(true, 0x11, 0));
                            } else {
                                if (ips.getFreeSlot() < 0 || ips.getVisitorSlot(chr) > -1 || !ips.isOpen() || !ips.isAvailable()) {
                                    c.announce(PlayerShopPacket.getMiniGameFull());
                                } else {
                                    if (slea.available() > 0 && slea.readByte() > 0) { //a password has been entered
                                        String pass = slea.readMapleAsciiString();
                                        if (!pass.equals(ips.getPassword())) {
                                            c.getPlayer().dropMessage(1, "你输入的密码不正确.");
                                            return;
                                        }
                                    } else if (ips.getPassword().length() > 0) {
                                        c.getPlayer().dropMessage(1, "你输入的密码不正确.");
                                        return;
                                    }
                                    chr.setPlayerShop(ips);
                                    ips.addVisitor(chr);
                                    if (ips instanceof MapleMiniGame) {
                                        ((MapleMiniGame) ips).send(c);
                                    } else {
                                        c.announce(PlayerShopPacket.getPlayerStore(chr, false));
                                    }
                                }
                            }
                        }
                    }
                }
                break;
            }
            case 聊天: {
                chr.updateTick(slea.readInt());
                String message = slea.readMapleAsciiString();
                if (chr.getTrade() != null) {
                    chr.getTrade().chat(message);
                } else if (chr.getPlayerShop() != null) {
                    IMaplePlayerShop ips = chr.getPlayerShop();
                    ips.broadcastToVisitors(PlayerShopPacket.playerInterChat(chr.getName() + " : " + message, ips.getVisitorSlot(chr)));
                    if (ips.getShopType() == IMaplePlayerShop.HIRED_MERCHANT) {
                        ips.getMessages().add(new Pair<>(chr.getName() + " : " + message, ips.getVisitorSlot(chr)));
                    }
                    if (chr.getClient().isMonitored()) { //Broadcast info even if it was a command.
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, chr.getName() + " said in " + ips.getOwnerName() + " shop : " + message));
                    }
                }
                break;
            }
            case 退出: {
                if (chr.getTrade() != null) {
                    MapleTrade.cancelTrade(chr.getTrade(), chr.getClient(), chr);
                } else {
                    IMaplePlayerShop ips = chr.getPlayerShop();
                    if (ips == null) { //should be null anyway for owners of hired merchants (maintenance_off)
                        return;
                    }
                    if (ips.isOwner(chr) && ips.getShopType() != 1) {
                        ips.closeShop(false, ips.isAvailable()); //how to return the items?
                    } else {
                        ips.removeVisitor(chr);
                    }
                    chr.setPlayerShop(null);
                }
                break;
            }
            case 打开: {
                // c.getPlayer().haveItem(mode, 1, false, true)
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop.isOwner(chr) && shop.getShopType() < 3 && !shop.isAvailable()) {
                    if (chr.getMap().allowPersonalShop()) {
                        if (c.getChannelServer().isShutdown()) {
                            chr.dropMessage(1, "服务器即将关闭维护，暂时无法进行此操作。.");
                            c.sendEnableActions();
                            shop.closeShop(shop.getShopType() == IMaplePlayerShop.HIRED_MERCHANT, false);
                            return;
                        }
                        if (shop.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && HiredMerchantHandler.UseHiredMerchant(chr.getClient(), false)) {
                            HiredMerchant merchant = (HiredMerchant) shop;
                            merchant.setStoreid(c.getChannelServer().addMerchant(merchant));
                            merchant.setOpen(true);
                            merchant.setAvailable(true);
                            shop.saveItems();
                            chr.getMap().broadcastMessage(PlayerShopPacket.spawnHiredMerchant(merchant));
                            chr.setPlayerShop(null);
                        } else if (shop.getShopType() == 2) {
                            shop.setOpen(true);
                            shop.setAvailable(true);
                            shop.update();
                        }
                    } else {
                        chr.getClient().disconnect(true, false);
                        c.getSession().close();
                    }
                }
                break;
            }
            case 设置物品:
            case 设置物品_001:
            case 设置物品_002:
            case 设置物品_003: {
//                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                MapleInventoryType ivType = MapleInventoryType.getByType(slea.readByte());
                Item item = chr.getInventory(ivType).getItem((byte) slea.readShort());
                short quantity = slea.readShort();
                byte targetSlot = slea.readByte();
                if (chr.getTrade() != null && item != null) {
                    /*
                     * 设置国庆纪念币无法进行交易
                     */
                    boolean canTrade = true;
                    if (item.getItemId() == 4000463 && !canTrade) {
                        chr.dropMessage(1, "该道具无法进行交易.");
                        c.sendEnableActions();
                    } else if ((quantity <= item.getQuantity() && quantity >= 0) || ItemConstants.is飞镖道具(item.getItemId()) || ItemConstants.is子弹道具(item.getItemId())) {
                        chr.getTrade().setItems(c, item, targetSlot, quantity);
                    }
                }
                break;
            }
            case 设置金币:
            case 设置金币_005:
            case 设置金币_006:
            case 设置金币_007: {
                MapleTrade trade = chr.getTrade();
                if (trade != null) {
                    trade.setMeso(slea.readInt());
                }
                break;
            }
            case 确认交易:
            case 确认交易_009:
            case 确认交易_00A:
            case 确认交易_00B: {
                if (chr.getTrade() != null) {
                    MapleTrade.completeTrade(chr);
                    break;
                }
                break;
            }
            case 玩家商店_添加道具:
            case 玩家商店_添加道具003E:
            case 玩家商店_添加道具003F:
            case 玩家商店_添加道具0040:
            case 添加物品: //0x1F
            case 添加物品_0020:
            case 添加物品_0021:
            case 添加物品_0022: {
                /*
                 * D4 00
                 * 15
                 * 02 - type
                 * 02 00 - 位置
                 * 01 00
                 * 01 00
                 * 10 27 00 00 - 贩卖的价格
                 */
                MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
                byte slot = (byte) slea.readShort();
                short bundles = slea.readShort(); // How many in a bundle
                short perBundle = slea.readShort(); // Price per bundle
                long price = slea.readLong();
                if (price <= 0 || bundles <= 0 || perBundle <= 0) {
                    chr.dropMessage(1, "添加物品出现错误(1)");
                    c.sendEnableActions();
                    return;
                }
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame) {
                    return;
                }
                Item ivItem = chr.getInventory(type).getItem(slot);
                MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                if (ivItem != null) {
                    long check = bundles * perBundle;
                    if (check > 32767 || check <= 0) { //This is the better way to check.
                        return;
                    }
                    short bundles_perbundle = (short) (bundles * perBundle);
                    if (ivItem.getQuantity() >= bundles_perbundle) {
                        short flag = ivItem.getFlag();
                        if (ItemFlag.不可交易.check(flag) || ItemFlag.封印.check(flag)) {
                            c.sendEnableActions();
                            return;
                        }
                        if (ii.isDropRestricted(ivItem.getItemId()) || ii.isAccountShared(ivItem.getItemId())) {
                            if (!(ItemFlag.可以交换1次.check(flag) || ItemFlag.宿命剪刀.check(flag))) {
                                c.sendEnableActions();
                                return;
                            }
                        }
                        /*
                         * 设置国庆纪念币无法进行贩卖
                         */
                        if (ivItem.getItemId() == 4000463) {
                            chr.dropMessage(1, "该道具无法进行贩卖.");
                            c.sendEnableActions();
                            return;
                        }
                        if (bundles_perbundle >= 50 && ivItem.getItemId() == 2340000) {
                            c.setMonitored(true); //hack check
                        }
                        if (ItemConstants.getLowestPrice(ivItem.getItemId()) > price) {
                            c.getPlayer().dropMessage(1, "The lowest you can sell this for is " + ItemConstants.getLowestPrice(ivItem.getItemId()));
                            c.sendEnableActions();
                            return;
                        }
                        if (ItemConstants.is飞镖道具(ivItem.getItemId()) || ItemConstants.is子弹道具(ivItem.getItemId())) {
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, ivItem.getQuantity(), true);
                            Item sellItem = ivItem.copy();
                            shop.addItem(new MaplePlayerShopItem(sellItem, (short) 1, price));
                        } else {
                            MapleInventoryManipulator.removeFromSlot(c, type, slot, bundles_perbundle, true);
                            Item sellItem = ivItem.copy();
                            sellItem.setQuantity(perBundle);
                            shop.addItem(new MaplePlayerShopItem(sellItem, bundles, price));
                        }
                        c.announce(PlayerShopPacket.shopItemUpdate(shop));
                    } else {
                        chr.dropMessage(1, "添加物品的数量错误。如果是飞镖，子弹之类请充了后在进行贩卖。");
                    }
                }
                break;
            }
            case 玩家商店_购买道具:
            case 玩家商店_购买道具0042:
            case 玩家商店_购买道具0043:
            case 玩家商店_购买道具0044:
            case BUY_ITEM_STORE:
            case 雇佣商店_购买道具: //0x23
            case 雇佣商店_购买道具0024:
            case 雇佣商店_购买道具0025:
            case 雇佣商店_购买道具0026: {
                //[CD 00] [16] [00] [01 00] [41 62 E3 B1]
                int item = slea.readByte();
                short quantity = slea.readShort(); //数量
                //slea.skip(4);
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop == null || shop.isOwner(chr) || shop instanceof MapleMiniGame || item >= shop.getItems().size()) {
                    c.announce(PlayerShopPacket.Merchant_Buy_Error((byte) 0x0D));
                    c.sendEnableActions();
                    return;
                }
                MaplePlayerShopItem tobuy = shop.getItems().get(item);
                if (tobuy == null) {
                    c.announce(PlayerShopPacket.Merchant_Buy_Error((byte) 0x0A));
                    c.sendEnableActions();
                    return;
                }
                long check = tobuy.bundles * quantity;
                long check2 = tobuy.price * quantity; //价格
                long check3 = tobuy.item.getQuantity() * quantity; //数量
                if (check <= 0 || check2 > ServerConfig.CHANNEL_PLAYER_MAXMESO || check2 <= 0 || check3 > 32767 || check3 < 0) { //This is the better way to check.
                    c.announce(PlayerShopPacket.Merchant_Buy_Error((byte) 0x0D));
                    c.sendEnableActions();
                    return;
                }
                if (chr.getMeso() - (check2) < 0) {
                    c.announce(PlayerShopPacket.Merchant_Buy_Error((byte) 0x02));
                    c.sendEnableActions();
                    return;
                }
                if (tobuy.bundles < quantity || (tobuy.bundles % quantity != 0 && ItemConstants.isEquip(tobuy.item.getItemId())) // Buying
                        || chr.getMeso() - (check2) > ServerConfig.CHANNEL_PLAYER_MAXMESO || shop.getMeso() + (check2) < 0 || shop.getMeso() + (check2) > ServerConfig.CHANNEL_PLAYER_MAXMESO) {
                    c.announce(PlayerShopPacket.Merchant_Buy_Error((byte) 0x04));
                    c.sendEnableActions();
                    return;
                }
                if (quantity >= 50 && tobuy.item.getItemId() == 2340000) {
                    c.setMonitored(true); //hack check
                }
                shop.buy(c, item, quantity);
                shop.broadcastToVisitors(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case 雇佣商店_求购道具: {
                /*
                 * [D4 00] [17]
                 * F5 95 1F 00 - 求购道具的ID
                 * 01 00 -
                 * 01 00
                 * E8 03 00 00 - 求购道具的价格
                 */
                chr.dropMessage(1, "当前服务器暂不支持求购道具.");
                break;
            }
            case 雇佣商店_维护: {
                /*
                 * [CD 00] [14] [09] [05 01 00 20] [21 A1 07 00] 00
                 */
                slea.skip(1); //未知
                byte type = slea.readByte(); //模式
                slea.skip(3); //未知 01 00 20
                int obid = slea.readInt();
                if (type == 6) {
                    MapleMapObject ob = chr.getMap().getMapObject(obid, MapleMapObjectType.HIRED_MERCHANT);
                    if (ob instanceof IMaplePlayerShop && chr.getPlayerShop() == null) {
                        IMaplePlayerShop ips = (IMaplePlayerShop) ob;
                        if (ob instanceof HiredMerchant) {
                            HiredMerchant merchant = (HiredMerchant) ips;
                            if (merchant.isOwner(chr) && merchant.isOpen() && merchant.isAvailable()) {
                                merchant.setOpen(false);
                                merchant.removeAllVisitors(0x14, (byte) 1);
                                chr.setPlayerShop(ips);
                                c.announce(PlayerShopPacket.getHiredMerch(chr, merchant, false));
                            } else {
                                if (!merchant.isOpen() || !merchant.isAvailable()) {
                                    chr.dropMessage(1, "主人正在整理商店物品\r\n请稍后再度光临！");
                                } else if (ips.getFreeSlot() == -1) {
                                    chr.dropMessage(1, "店铺已达到最大人数\r\n请稍后再度光临！");
                                } else if (merchant.isInBlackList(chr.getName())) {
                                    chr.dropMessage(1, "你被禁止进入该店铺");
                                }
                            }
                        }
                    }
                } else {
                    c.sendEnableActions();
                }
                break;
            }
            case 移除物品: {
                slea.skip(1);
                int slot = slea.readShort();
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (chr.isAdmin()) {
                    chr.dropMessage(5, "移除商店道具: 道具数量 " + shop.getItems().size() + " slot " + slot);
                }
                if (shop == null || !shop.isOwner(chr) || shop instanceof MapleMiniGame || shop.getItems().size() <= 0 || shop.getItems().size() <= slot || slot < 0) {
                    return;
                }
                MaplePlayerShopItem item = shop.getItems().get(slot);
                if (item != null) {
                    if (item.bundles > 0) {
                        Item item_get = item.item.copy();
                        long check = item.bundles * item.item.getQuantity();
                        if (check < 0 || check > 32767) {
                            if (chr.isAdmin()) {
                                chr.dropMessage(5, "移除商店道具出错: check " + check);
                            }
                            return;
                        }
                        item_get.setQuantity((short) check);
                        if (item_get.getQuantity() >= 50 && item.item.getItemId() == 2340000) {
                            c.setMonitored(true); //hack check
                        }
                        if (MapleInventoryManipulator.checkSpace(c, item_get.getItemId(), item_get.getQuantity(), item_get.getOwner())) {
                            MapleInventoryManipulator.addFromDrop(c, item_get, false);
                            item.bundles = 0;
                            shop.removeFromSlot(slot);
                        }
                    }
                }
                c.announce(PlayerShopPacket.shopItemUpdate(shop));
                break;
            }
            case 雇佣商店_开启: //开启雇佣商店
            case 雇佣商店_维护开启: { //维护雇佣商店后在开启
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop instanceof HiredMerchant && shop.isOwner(chr) && shop.isAvailable()) {
                    shop.setOpen(true);
                    shop.saveItems();
                    shop.getMessages().clear();
                    shop.removeAllVisitors(-1, -1);
                    chr.setPlayerShop(null);
                }
                c.sendEnableActions();
                break;
            }
            case 雇佣商店_整理: {
                IMaplePlayerShop imps = chr.getPlayerShop();
                if (imps != null && imps.isOwner(chr) && !(imps instanceof MapleMiniGame)) {
                    for (int i = 0; i < imps.getItems().size(); i++) {
                        if (imps.getItems().get(i).bundles == 0) {
                            imps.getItems().remove(i);
                        }
                    }
                    if (chr.getMeso() + imps.getMeso() > 0) {
                        chr.gainMeso(imps.getMeso(), false);
                        HiredMerchant.log.info(chr.getName() + " 雇佣整理获得金币: " + imps.getMeso() + " 时间: " + DateUtil.getCurrentDate());
                        imps.setMeso(0);
                    }
                    c.announce(PlayerShopPacket.shopItemUpdate(imps));
                }
                break;
            }
            case 雇佣商店_关闭: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    c.announce(PlayerShopPacket.hiredMerchantOwnerLeave());
                    merchant.removeAllVisitors(-1, -1);
                    chr.setPlayerShop(null);
                    merchant.closeShop(true, true);
                } else {
                    chr.dropMessage(1, "关闭商店出现未知错误.");
                    c.sendEnableActions();
                }
                break;
            }
            case 管理员修改雇佣商店名称: { // Changing store name, only Admin
                // slea.readInt(); 要修改的雇佣商店的角色所有者ID
                chr.dropMessage(1, "暂不支持管理员修改雇佣商店的名字.");
                c.sendEnableActions();
                break;
            }
            case 雇佣商店_查看访问名单: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendVisitor(c);
                }
                break;
            }
            case 雇佣商店_查看黑名单: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).sendBlackList(c);
                }
                break;
            }
            case 雇佣商店_添加黑名单: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).addBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case 雇佣商店_移除黑名单: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    ((HiredMerchant) merchant).removeBlackList(slea.readMapleAsciiString());
                }
                break;
            }
            case 雇佣商店_修改商店名称: {
                IMaplePlayerShop merchant = chr.getPlayerShop();
                if (merchant != null && merchant.getShopType() == IMaplePlayerShop.HIRED_MERCHANT && merchant.isOwner(chr)) {
                    String desc = slea.readMapleAsciiString();
                    if (((HiredMerchant) merchant).canChangeName()) {
                        merchant.setDescription(desc);
                    } else {
                        c.announce(MaplePacketCreator.craftMessage("还不能变更名称，还需要等待" + ((HiredMerchant) merchant).getChangeNameTimeLeft() + "秒。"));
                    }
                }
                break;
            }
            case 玩家商店_移除玩家: {
                IMaplePlayerShop shop = chr.getPlayerShop();
                if (shop != null && shop.getShopType() == IMaplePlayerShop.PLAYER_SHOP) {
                    slea.skip(1);
                    String name = slea.readMapleAsciiString();
                    ((MaplePlayerShop) shop).banPlayer(name);
                }
                break;
            }
            case GIVE_UP: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 0, game.getVisitorSlot(chr)));
                    game.nextLoser();
                    game.setOpen(true);
                    game.update();
                    game.checkExitAfterGame();
                }
                break;
            }
            case 踢出玩家: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    if (!ips.isOpen()) {
                        break;
                    }
                    ips.removeAllVisitors(3, 1);
                }
                break;
            }
            case 准备就绪:
            case 准备开始: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (!game.isOwner(chr) && game.isOpen()) {
                        game.setReady(game.getVisitorSlot(chr));
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameReady(game.isReady(game.getVisitorSlot(chr))));
                    }
                }
                break;
            }
            case 开始游戏: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOwner(chr) && game.isOpen()) {
                        for (int i = 1; i < ips.getSize(); i++) {
                            if (!game.isReady(i)) {
                                return;
                            }
                        }
                        game.setGameType();
                        game.shuffleList();
                        if (game.getGameType() == 1) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameStart(game.getLoser()));
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardStart(game, game.getLoser()));
                        }
                        game.setOpen(false);
                        game.update();
                    }
                }
                break;
            }
            case 请求平局: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.isOwner(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMiniGameRequestTie(), false);
                    } else {
                        game.getMCOwner().getClient().announce(PlayerShopPacket.getMiniGameRequestTie());
                    }
                    game.setRequestedTie(game.getVisitorSlot(chr));
                }
                break;
            }
            case 应答平局: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getRequestedTie() > -1 && game.getRequestedTie() != game.getVisitorSlot(chr)) {
                        if (slea.readByte() > 0) {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameResult(game, 1, game.getRequestedTie()));
                            game.nextLoser();
                            game.setOpen(true);
                            game.update();
                            game.checkExitAfterGame();
                        } else {
                            game.broadcastToVisitors(PlayerShopPacket.getMiniGameDenyTie());
                        }
                        game.setRequestedTie(-1);
                    }
                }
                break;
            }
            case SKIP: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != ips.getVisitorSlot(chr)) {
                        ips.broadcastToVisitors(PlayerShopPacket.playerInterChat("Turn could not be skipped by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + ips.getVisitorSlot(chr), ips.getVisitorSlot(chr)));
                        return;
                    }
                    ips.broadcastToVisitors(PlayerShopPacket.getMiniGameSkip(ips.getVisitorSlot(chr)));
                    game.nextLoser();
                }
                break;
            }
            case 移动棋子: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != game.getVisitorSlot(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.playerInterChat("Omok could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                        return;
                    }
                    game.setPiece(slea.readInt(), slea.readInt(), slea.readByte(), chr);
                }
                break;
            }
            case 选择卡片: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    if (game.getLoser() != game.getVisitorSlot(chr)) {
                        game.broadcastToVisitors(PlayerShopPacket.playerInterChat("Card could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr), game.getVisitorSlot(chr)));
                        return;
                    }
                    if (slea.readByte() != game.getTurn()) {
                        game.broadcastToVisitors(PlayerShopPacket.playerInterChat("Omok could not be placed by " + chr.getName() + ". Loser: " + game.getLoser() + " Visitor: " + game.getVisitorSlot(chr) + " Turn: " + game.getTurn(), game.getVisitorSlot(chr)));
                        return;
                    }
                    int slot = slea.readByte();
                    int turn = game.getTurn();
                    int fs = game.getFirstSlot();
                    if (turn == 1) {
                        game.setFirstSlot(slot);
                        if (game.isOwner(chr)) {
                            game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn), false);
                        } else {
                            game.getMCOwner().getClient().announce(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, turn));
                        }
                        game.setTurn(0); //2nd turn nao
                        return;
                    } else if (fs > 0 && game.getCardId(fs + 1) == game.getCardId(slot + 1)) {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 2 : 3));
                        game.setPoints(game.getVisitorSlot(chr)); //correct.. so still same loser. diff turn tho
                    } else {
                        game.broadcastToVisitors(PlayerShopPacket.getMatchCardSelect(turn, slot, fs, game.isOwner(chr) ? 0 : 1));
                        game.nextLoser();//wrong haha
                    }
                    game.setTurn(1);
                    game.setFirstSlot(0);
                }
                break;
            }
            case 退出游戏:
            case 取消退出: {
                IMaplePlayerShop ips = chr.getPlayerShop();
                if (ips != null && ips instanceof MapleMiniGame) {
                    MapleMiniGame game = (MapleMiniGame) ips;
                    if (game.isOpen()) {
                        break;
                    }
                    game.setExitAfter(chr);
                    game.broadcastToVisitors(PlayerShopPacket.getMiniGameExitAfter(game.isExitAfter(chr)));
                }
                break;
            }
            default: {
                if (ServerConstants.isShowPacket()) {
                    log.warn("玩家互动未知的操作类型: 0x" + StringUtil.getLeftPaddedStr(Integer.toHexString(mode).toUpperCase(), '0', 2) + " " + slea.toString());
                }
                c.sendEnableActions();
                break;
            }
        }
    }
}
