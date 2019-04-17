package server;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import constants.GameConstants;
import constants.ItemConstants;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.commands.CommandProcessor;
import server.commands.CommandType;
import tools.MaplePacketCreator;
import tools.packet.PlayerShopPacket;
import tools.packet.TradePacket;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class MapleTrade {

    private static final Logger log = LogManager.getLogger(MapleTrade.class);
    private final List<Item> items = new LinkedList<>(); //保存交易中的道具
    private final WeakReference<MapleCharacter> chr;
    private final byte tradingslot;
    private MapleTrade partner = null;
    private List<Item> exchangeItems;
    private int meso = 0, exchangeMeso = 0;
    private boolean locked = false, inTrade = false;

    public MapleTrade(byte tradingslot, MapleCharacter chr) {
        this.tradingslot = tradingslot;
        this.chr = new WeakReference<>(chr);
    }

    /*
     * 交易确定
     * local 玩家自己的交易
     * partner 对方的交易
     */
    public static void completeTrade(MapleCharacter player) {
        MapleTrade local = player.getTrade();
        MapleTrade partner = local.getPartner();
        if (partner == null || local.locked) {
            return;
        }
        local.locked = true; // 确定这个交易
        partner.getChr().getClient().announce(TradePacket.getTradeConfirmation());
        partner.exchangeItems = new LinkedList<>(local.items); // 复制交易中的道具信息
        partner.exchangeMeso = local.meso; // 复制交易中的金币
        if (partner.isLocked()) { // 玩家已经点了确定交易
            int lz = local.check(), lz2 = partner.check(); //检测交易中双方的背包空间信息和道具是否能交易
            if (lz == 0 && lz2 == 0) { //双方都通过检测
                log.info("[交易] -------------------------------------------------------------------------- ");
                local.CompleteTrade();
                partner.CompleteTrade();
                log.info("[交易] " + local.getChr().getName() + " 和 " + partner.getChr().getName() + " 交易完成。");
            } else {
                // 注意 : 如果双方都确定交易后 交易对象的背包是满的 交易就自动取消.
                partner.cancel(partner.getChr().getClient(), partner.getChr(), lz == 0 ? lz2 : lz);
                local.cancel(player.getClient(), player, lz == 0 ? lz2 : lz);
            }
            partner.getChr().setTrade(null);
            player.setTrade(null);
        }
    }

    /*
     * 交易取消
     */
    public static void cancelTrade(MapleTrade Localtrade, MapleClient c, MapleCharacter player) {
        Localtrade.cancel(c, player);
        MapleTrade partner = Localtrade.getPartner();
        if (partner != null && partner.getChr() != null) {
            partner.cancel(partner.getChr().getClient(), partner.getChr());
            partner.getChr().setTrade(null);
        }
        player.setTrade(null);
    }

    /*
     * 开始交易
     * 也就是创建1个交易
     */
    public static void startTrade(MapleCharacter player) {
        if (player.getTrade() == null) {
            player.setTrade(new MapleTrade((byte) 0, player));
            player.getClient().announce(TradePacket.getTradeStart(player.getClient(), player.getTrade(), (byte) 0));
        } else {
            player.getClient().announce(MaplePacketCreator.serverNotice(5, "不能同时做多件事情。"));
        }
    }

    /*
     * 交易邀请
     */
    public static void inviteTrade(MapleCharacter player, MapleCharacter target) {
        if (player == null || player.getTrade() == null) {
            return;
        }
        if (target != null && target.getTrade() == null) {
            target.setTrade(new MapleTrade((byte) 1, target));
            target.getTrade().setPartner(player.getTrade());
            player.getTrade().setPartner(target.getTrade());
            target.getClient().announce(TradePacket.getTradeInvite(player));
        } else {
            player.getClient().announce(MaplePacketCreator.serverNotice(5, "对方正在和其他玩家进行交易中。"));
            cancelTrade(player.getTrade(), player.getClient(), player);
        }
    }

    /*
     * 访问交易 进入开始交易状态
     */
    public static void visitTrade(MapleCharacter player, MapleCharacter target) {
        if (target != null && player.getTrade() != null && player.getTrade().getPartner() == target.getTrade() && target.getTrade() != null && target.getTrade().getPartner() == player.getTrade()) {
            player.getTrade().inTrade = true;
            target.getClient().announce(PlayerShopPacket.playerInterVisitorAdd(player, 1));
            player.getClient().announce(TradePacket.getTradeStart(player.getClient(), player.getTrade(), (byte) 1));
            player.dropMessage(-2, "系统提示 : 交易时请仔细查看交易的道具信息");
            target.dropMessage(-2, "系统提示 : 交易时请仔细查看交易的道具信息");
        } else {
            player.getClient().announce(MaplePacketCreator.serverNotice(5, "对方已经取消了交易。"));
        }
    }

    /*
     * 拒绝交易邀请
     */
    public static void declineTrade(MapleCharacter player) {
        MapleTrade trade = player.getTrade();
        if (trade != null) {
            if (trade.getPartner() != null) {
                MapleCharacter other = trade.getPartner().getChr();
                if (other != null && other.getTrade() != null) {
                    other.getTrade().cancel(other.getClient(), other);
                    other.setTrade(null);
                    other.dropMessage(5, player.getName() + " 拒绝了你的交易邀请。");
                }
            }
            trade.cancel(player.getClient(), player);
            player.setTrade(null);
        }
    }

    /*
     * 交易完成
     */
    public void CompleteTrade() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (exchangeItems != null) { // just to be on the safe side...
            List<Item> itemz = new LinkedList<>(exchangeItems);
            for (Item item : itemz) {
                short flag = item.getFlag();
                if (ItemFlag.可以交换1次.check(flag)) {
                    item.setFlag((short) (flag - ItemFlag.可以交换1次.getValue()));
                } else if (ItemFlag.宿命剪刀.check(flag)) {
                    item.setFlag((short) (flag - ItemFlag.宿命剪刀.getValue()));
                }
                MapleInventoryManipulator.addFromDrop(chr.get().getClient(), item, false);
                log.info("[交易] " + chr.get().getName() + " 交易获得道具: " + item.getItemId() + " x " + item.getQuantity() + " - " + ii.getName(item.getItemId()));
            }
            exchangeItems.clear();
        }
        if (exchangeMeso > 0) {
            chr.get().gainMeso(exchangeMeso - GameConstants.getTaxAmount(exchangeMeso), false, false);
            log.info("[交易] " + chr.get().getName() + " 交易获得金币: " + exchangeMeso);
        }
        exchangeMeso = 0;
        chr.get().getClient().announce(TradePacket.TradeMessage(tradingslot, (byte) 0x07));
    }

    /*
     * 交易取消
     * 直接取消交易 也就是退出交易
     */
    public void cancel(MapleClient c, MapleCharacter chr) {
        cancel(c, chr, 0);
    }

    /*
     * 交易取消
     * 0x02 对方中止交易。
     * 0x08 交易成功了。请再确认交易的结果。
     * 0x09 交易失败了。
     * 0x0A 因部分道具有数量限制只能拥有一个交易失败了。
     * 0x0D 双方在不同的地图不能交易。
     * 0x0E 游戏文件损坏，无法交易物品。请重新安装游戏后，再重新尝试。
     */
    public void cancel(MapleClient c, MapleCharacter chr, int message) {
        if (items != null) { //为了安全起见 检测下交易中是否有道具
            List<Item> itemz = new LinkedList<>(items);
            for (Item item : itemz) {
                MapleInventoryManipulator.addFromDrop(c, item, false); //将道具归还给玩家
            }
            items.clear();
        }
        if (meso > 0) {
            chr.gainMeso(meso, false, false);
        }
        meso = 0;
        c.announce(TradePacket.getTradeCancel(tradingslot, message));
    }

    /*
     * 检测交易是否确认
     */
    public boolean isLocked() {
        return locked;
    }

    /*
     * 添加金币
     */
    public void setMeso(int meso) {
        if (locked || partner == null || meso <= 0 || this.meso + meso <= 0) {
            return;
        }
        if (chr.get().getMeso() >= meso) {
            chr.get().gainMeso(-meso, false, false);
            this.meso += meso;
            chr.get().getClient().announce(TradePacket.getTradeMesoSet((byte) 0, this.meso));
            if (partner != null) {
                partner.getChr().getClient().announce(TradePacket.getTradeMesoSet((byte) 1, this.meso));
            }
        }
    }

    /*
     * 添加道具
     */
    public void addItem(Item item) {
        if (locked || partner == null) {
            return;
        }
        items.add(item);
        chr.get().getClient().announce(TradePacket.getTradeItemAdd((byte) 0, item));
        if (partner != null) {
            partner.getChr().getClient().announce(TradePacket.getTradeItemAdd((byte) 1, item));
        }
    }

    /*
     * 交易聊天
     */
    public void chat(String message) {
        if (!CommandProcessor.processCommand(chr.get().getClient(), message, CommandType.TRADE)) {
            chr.get().dropMessage(-2, chr.get().getName() + " : " + message);
            if (partner != null) {
                partner.getChr().getClient().announce(PlayerShopPacket.playerInterChat(chr.get().getName() + " : " + message, 1));
            }
        }
        if (chr.get().getClient().isMonitored()) { //Broadcast info even if it was a command.
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, chr.get().getName() + " 在交易中对 " + partner.getChr().getName() + " 说: " + message));
        } else if (partner != null && partner.getChr() != null && partner.getChr().getClient().isMonitored()) {
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, chr.get().getName() + " 在交易中对 " + partner.getChr().getName() + " 说: " + message));
        }
    }

    public void chatAuto(String message) {
        chr.get().dropMessage(-2, message);
        if (partner != null) {
            partner.getChr().getClient().announce(PlayerShopPacket.playerInterChat(message, 1));
        }
        if (chr.get().getClient().isMonitored()) { //Broadcast info even if it was a command.
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, chr.get().getName() + " said in trade [Automated] with " + partner.getChr().getName() + " 说: " + message));
        } else if (partner != null && partner.getChr() != null && partner.getChr().getClient().isMonitored()) {
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, chr.get().getName() + " said in trade [Automated] with " + partner.getChr().getName() + " 说: " + message));
        }
    }

    public MapleTrade getPartner() {
        return partner;
    }

    public void setPartner(MapleTrade partner) {
        if (locked) {
            return;
        }
        this.partner = partner;
    }

    public MapleCharacter getChr() {
        return chr.get();
    }

    public int getNextTargetSlot() {
        if (items.size() >= 9) {
            return -1;
        }
        int ret = 1; //first slot
        for (Item item : items) {
            if (item.getPosition() == ret) {
                ret++;
            }
        }
        return ret;
    }

    public boolean inTrade() {
        return inTrade;
    }

    /*
     * 添加道具
     */
    public boolean setItems(MapleClient c, Item item, byte targetSlot, int quantity) {
        int target = getNextTargetSlot();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (partner == null || target == -1 || ItemConstants.isPet(item.getItemId()) || isLocked() || (ItemConstants.getInventoryType(item.getItemId()) == MapleInventoryType.EQUIP && quantity != 1)) {
            return false;
        }
        short flag = item.getFlag();
        if (ItemFlag.不可交易.check(flag) || ItemFlag.封印.check(flag)) {
            c.announce(MaplePacketCreator.enableActions());
            return false;
        }
        if (ii.isDropRestricted(item.getItemId()) || ii.isAccountShared(item.getItemId())) {
            if (!(ItemFlag.可以交换1次.check(flag) || ItemFlag.宿命剪刀.check(flag))) {
                c.announce(MaplePacketCreator.enableActions());
                return false;
            }
        }
        Item tradeItem = item.copy();
        if (ItemConstants.is飞镖道具(item.getItemId()) || ItemConstants.is子弹道具(item.getItemId())) {
            tradeItem.setQuantity(item.getQuantity());
            MapleInventoryManipulator.removeFromSlot(c, ItemConstants.getInventoryType(item.getItemId()), item.getPosition(), item.getQuantity(), true);
        } else {
            tradeItem.setQuantity((short) quantity);
            MapleInventoryManipulator.removeFromSlot(c, ItemConstants.getInventoryType(item.getItemId()), item.getPosition(), (short) quantity, true);
        }
        if (targetSlot < 0) {
            targetSlot = (byte) target;
        } else {
            for (Item itemz : items) {
                if (itemz.getPosition() == targetSlot) {
                    targetSlot = (byte) target;
                    break;
                }
            }
        }
        tradeItem.setPosition(targetSlot);
        addItem(tradeItem);
        return true;
    }

    /*
     * 交易中双方确认后检测背包空间
     * 0 = 可以完成交易
     * 1 = 背包空间不足
     * 2 = 交易的道具中有捡取限制的道具
     */
    private int check() {
        if (chr.get().getMeso() + exchangeMeso < 0) {
            return 1;
        }
        if (exchangeItems != null) {
            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
            byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
            for (Item item : exchangeItems) {
                switch (ItemConstants.getInventoryType(item.getItemId())) {
                    case EQUIP:
                        eq++;
                        break;
                    case USE:
                        use++;
                        break;
                    case SETUP:
                        setup++;
                        break;
                    case ETC:
                        etc++;
                        break;
                    case CASH:
                        cash++;
                        break;
                }
                if (ii.isPickupRestricted(item.getItemId()) && chr.get().haveItem(item.getItemId(), 1, true, true)) {
                    return 2;
                }
            }
            if (chr.get().getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.get().getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.get().getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.get().getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.get().getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
                return 1;
            }
        }
        return 0;
    }
}
