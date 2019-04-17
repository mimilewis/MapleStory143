package tools.packet;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import handling.opcode.InteractionOpcode;
import handling.opcode.SendPacketOpcode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MerchItemPackage;
import server.shops.*;
import server.shops.AbstractPlayerStore.BoughtItem;
import server.shops.AbstractPlayerStore.VisitorInfo;
import tools.Pair;
import tools.data.output.MaplePacketLittleEndianWriter;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class PlayerShopPacket {
    private static final Logger log = LogManager.getLogger();

    public static byte[] sendTitleBox(int message) {
        return sendTitleBox(message, 0, 0);
    }

    public static byte[] sendTitleBox(int message, int mapId, int ch) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SEND_TITLE_BOX.getValue());
        mplew.write(message);
        switch (message) {
            case 0x07: //双击雇佣商店卡 弹出输入雇佣商店的名字窗口
            case 0x09: //请向自由市场入口处的弗兰德里领取物品后，重新再试。
            case 0x0F: //请通过弗兰德里领取物品。
                break;
            case 0x08: //提示雇佣商店开设在什么地方
            case 0x10: //x频道开设有商店。您想要移动到该频道吗？ 这个时候 mapId = 0
                mplew.writeInt(mapId);
                mplew.write(ch);
                break;
        }

        return mplew.getPacket();
    }

    public static byte[] sendPlayerBox(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.UPDATE_CHAR_BOX.getValue());
        mplew.writeInt(chr.getId());
        PacketHelper.addAnnounceBox(mplew, chr);

        return mplew.getPacket();
    }

    public static byte[] getHiredMerch(MapleCharacter chr, HiredMerchant merch, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.房间.getValue());
        mplew.write(0x06); //V.109修改 这个是商店的类型
        mplew.write(merch.getMaxSize()); //以前是0x04 V.100修改
        mplew.writeShort(merch.getVisitorSlot(chr));
        mplew.writeInt(merch.getItemId());
        mplew.writeMapleAsciiString("雇佣商人");
        for (Pair<Byte, MapleCharacter> storechr : merch.getVisitors()) {
            mplew.write(storechr.left);
            PacketHelper.addCharLook(mplew, storechr.right, true, storechr.right.isZeroSecondLook());
            mplew.writeMapleAsciiString(storechr.right.getName());
            mplew.writeShort(storechr.right.getJob());
        }
        mplew.write(-1);
        mplew.writeShort(merch.isOwner(chr) ? merch.getMessages().size() : 0);
        if (merch.isOwner(chr)) {
            for (int i = 0; i < merch.getMessages().size(); i++) {
                mplew.writeMapleAsciiString(merch.getMessages().get(i).getLeft());
                mplew.write(merch.getMessages().get(i).getRight());
            }
        }
        mplew.writeMapleAsciiString(merch.getOwnerName());
        if (merch.isOwner(chr)) {
            /*
             * 74 5B 02 00 - 23:57
             * 8B C5 03 00 - 23:55
             */
            mplew.writeInt(merch.getTimeLeft(firstTime));
            mplew.write(firstTime ? 1 : 0);
            mplew.write(merch.getBoughtItems().size());
            for (BoughtItem SoldItem : merch.getBoughtItems()) {
                mplew.writeInt(SoldItem.id);
                mplew.writeShort(SoldItem.quantity); // number of purchased
                mplew.writeLong(SoldItem.totalPrice); // total price
                mplew.writeMapleAsciiString(SoldItem.buyer); // name of the buyer
            }
            mplew.writeLong(merch.getMeso());
        }
        mplew.writeInt(merch.getObjectId()); //V.106新增
        mplew.writeMapleAsciiString(merch.getDescription());
        mplew.write(0x10);
        mplew.writeLong(merch.getMeso());
        mplew.write(merch.getItems().size());
        for (MaplePlayerShopItem item : merch.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeLong(item.price);
            PacketHelper.addItemInfo(mplew, item.item);
        }
        mplew.writeShort(0); //求购的道具信息

        return mplew.getPacket();
    }

    public static byte[] getPlayerStore(MapleCharacter chr, boolean firstTime) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        IMaplePlayerShop ips = chr.getPlayerShop();
        mplew.write(InteractionOpcode.房间.getValue());
        switch (ips.getShopType()) {
            case 2:
                mplew.write(5);
                mplew.write(7);
                break;
            case 3:
                mplew.write(2);
                mplew.write(2);
                break;
            case 4:
                mplew.write(1);
                mplew.write(2);
                break;
        }
        mplew.writeShort(ips.getVisitorSlot(chr));
        PacketHelper.addCharLook(mplew, ((MaplePlayerShop) ips).getMCOwner(), false, ((MaplePlayerShop) ips).getMCOwner().isZeroSecondLook());
        mplew.writeMapleAsciiString(ips.getOwnerName());
        mplew.writeShort(((MaplePlayerShop) ips).getMCOwner().getJob());
        for (Pair<Byte, MapleCharacter> storechr : ips.getVisitors()) {
            mplew.write(storechr.left);
            PacketHelper.addCharLook(mplew, storechr.right, false, storechr.right.isZeroSecondLook());
            mplew.writeMapleAsciiString(storechr.right.getName());
            mplew.writeShort(storechr.right.getJob());
        }
        mplew.write(0xFF);
        mplew.writeInt(0);
        mplew.writeMapleAsciiString(ips.getDescription());
        mplew.write(24);
        mplew.write(ips.getItems().size());
        for (MaplePlayerShopItem item : ips.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeLong(item.price);
            PacketHelper.addItemInfo(mplew, item.item);
        }
        mplew.writeShort(0);
        return mplew.getPacket();
    }

    /*
     * 互动类聊天通用！
     */
    public static byte[] playerInterChat(String message, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.聊天.getValue());
        mplew.write(InteractionOpcode.聊天事件.getValue());
        mplew.write(slot);
        mplew.writeMapleAsciiString(message);

        return mplew.getPacket();
    }

    /**
     * 提示错误信息
     */
    public static byte[] shopErrorMessage(int error, int type) {
        return shopErrorMessage(false, error, type);
    }

    public static byte[] shopErrorMessage(boolean room, int error, int type) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(room ? InteractionOpcode.房间.getValue() : InteractionOpcode.退出.getValue());
        mplew.write(type);
        mplew.write(error);

        return mplew.getPacket();
    }

    /*
     * 召唤雇佣商店
     */

    public static byte[] spawnHiredMerchant(AbstractPlayerStore hm) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SPAWN_HIRED_MERCHANT.getValue());
        mplew.writeInt(hm.getOwnerId());
        mplew.writeInt(hm.getItemId());
        mplew.writePos(hm.getTruePosition());
        mplew.writeShort(hm instanceof HiredFisher ? ((HiredFisher) hm).getFh() : 0);
        mplew.writeMapleAsciiString(hm.getOwnerName());
        PacketHelper.addInteraction(mplew, hm);

        return mplew.getPacket();
    }

    /*
     * 取消雇佣商店
     */
    public static byte[] destroyHiredMerchant(int id) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.DESTROY_HIRED_MERCHANT.getValue());
        mplew.writeInt(id);

        return mplew.getPacket();
    }

    /*
     * 更新商店道具
     */
    public static byte[] shopItemUpdate(IMaplePlayerShop shop) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.雇佣商店_更新信息.getValue());
        if (shop.getShopType() == 1) {
            mplew.writeLong(shop.getMeso()); //购买的时候显示价格
        }
        mplew.write(shop.getItems().size());
        for (MaplePlayerShopItem item : shop.getItems()) {
            mplew.writeShort(item.bundles);
            mplew.writeShort(item.item.getQuantity());
            mplew.writeLong(item.price);
            PacketHelper.addItemInfo(mplew, item.item);
        }
        mplew.writeShort(0); //求购的道具信息更新

        return mplew.getPacket();
    }

    /*
     * 玩家同意交易后添加角色
     */
    public static byte[] playerInterVisitorAdd(MapleCharacter chr, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.访问.getValue());
        mplew.write(slot);
        PacketHelper.addCharLook(mplew, chr, false, chr.isZeroSecondLook());
        mplew.writeMapleAsciiString(chr.getName());
        mplew.writeShort(chr.getJob());

        return mplew.getPacket();
    }

    /**
     * 玩家互动退出
     */
    public static byte[] playerInterVisitorLeave(byte slot) {
        log.trace("调用");
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.退出.getValue());
        mplew.write(slot);

        return mplew.getPacket();
    }

    /**
     * 雇佣商店错误提示
     */
    public static byte[] Merchant_Buy_Error(byte message) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        /*
         * V.106 0x2C
         * V.110 0x2D
         */
        mplew.write(InteractionOpcode.雇佣商店_错误提示.getValue());
        /*
         * 0x01 物品不够。
         * 0x02 金币不足
         * 0x03 该商品的价格太贵。你买不起
         * 0x04 超过对方的最大金额 无法交易.
         * 0x05 请确认是不是你的背包空间不够。
         * 0x06 这种道具不能拿两个以上。
         * 0x07 性别不符，无法购买。
         * 0x08 未满7岁的人无法购买该物品。
         * 0x09 由于存在有效时间结束而消失的物品，购买已取消。
         * 0x0A 正在验证中，现金道具的购买已取消。
         * 0x0B 该角色不能执行该操作。
         * 0x0C 游戏文件损坏，无法交易物品。请重新安装游戏后，在重新尝试。
         * 0x0D 发生未知错误，不能交易。
         */
        mplew.write(message);

        return mplew.getPacket();
    }

    /*
     * 更新雇佣商店
     * True - 更新雇佣商店信息
     * Flase - 更改雇佣商店名字
     */
    public static byte[] updateHiredMerchant(AbstractPlayerStore shop) {
        return updateHiredMerchant(shop, true);
    }

    public static byte[] updateHiredMerchant(AbstractPlayerStore shop, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(update ? SendPacketOpcode.UPDATE_HIRED_MERCHANT.getValue() : SendPacketOpcode.CHANGE_HIRED_MERCHANT_NAME.getValue());
        mplew.writeInt(shop.getOwnerId());
        PacketHelper.addInteraction(mplew, shop);

        return mplew.getPacket();
    }

    /*
     * 雇佣商店关闭完成
     */
    public static byte[] hiredMerchantOwnerLeave() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.雇佣商店_关闭完成.getValue());
        mplew.write(0);

        return mplew.getPacket();
    }

    /*
     * 弗兰德里操作信息
     */
    public static byte[] merchItem_Message(byte op) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        /*
         * 已领取所有道具
         * 0x24 已领取所有道具与金币
         * 0x25 因商店仓库内的金额过多，未能领取金币与道具
         * 0x26 因道具有数量的限制，未能领取金币与道具
         * 0x27 因手续费不足，未能领取金币与道具
         * 0x28 因背包位不足，未能领取进步与道具
         */
        mplew.writeShort(SendPacketOpcode.MERCH_ITEM_MSG.getValue());
        mplew.write(op);

        return mplew.getPacket();
    }

    public static byte[] merchItemStore(byte op) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MERCH_ITEM_STORE.getValue());
        /*
         * 发送打开雇佣商店的封包
         */
        mplew.write(op);
        switch (op) {
            case 0x2B: //已经过去x天，因此需要收取全部金币的100% x金币作为手续费。确定要取款吗？
                mplew.writeInt(0); //过去了多少天数
                mplew.writeLong(0); //取回需要金币多少
                break;
            default:
                mplew.write(0);
                break;
        }
        return mplew.getPacket();
    }

    /*
     * 提示雇佣商店开设在什么地方
     */
    public static byte[] merchItemStore(int mapId, int ch) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MERCH_ITEM_STORE.getValue());
        /*
         * V.110 0x28
         * V.115 0x2B
         */
        mplew.write(InteractionOpcode.SHOW_MERCH_ITEM_STOR.getValue());
        mplew.writeInt(9030000); //对话的NpcId
        mplew.writeInt(mapId);
        mplew.write(mapId != 999999999 ? ch : 0);

        return mplew.getPacket();
    }

    public static byte[] merchItemStore_ItemData(MerchItemPackage pack) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.MERCH_ITEM_STORE.getValue());
        /*
         * V.103 0x26
         * V.115 0x29
         */
        mplew.write(InteractionOpcode.MERCH_ITEM_STOR.getValue());
        mplew.writeInt(9030000); // Fredrick
        mplew.writeInt(32272); // always the same..?
        mplew.writeZeroBytes(5);
        mplew.writeLong(pack.getMesos());
        mplew.write(0);
        mplew.write(pack.getItems().size());
        for (Item item : pack.getItems()) {
            PacketHelper.addItemInfo(mplew, item);
        }
        mplew.writeZeroBytes(3);

        return mplew.getPacket();
    }

    public static byte[] openMiniGameBox(MapleClient c, MapleMiniGame minigame) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.房间.getValue());
        mplew.write(minigame.getGameType());
        mplew.write(minigame.getMaxSize());
        mplew.writeShort(minigame.getVisitorSlot(c.getPlayer()));
        PacketHelper.addCharLook(mplew, minigame.getMCOwner(), false, false);
        mplew.writeMapleAsciiString(minigame.getOwnerName());
        mplew.writeShort(minigame.getMCOwner().getJob());
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            mplew.write(visitorz.getLeft());
            PacketHelper.addCharLook(mplew, visitorz.getRight(), false, false);
            mplew.writeMapleAsciiString(visitorz.getRight().getName());
            mplew.writeShort(visitorz.getRight().getJob());
        }
        mplew.write(-1);
        mplew.write(0);
        addGameInfo(mplew, minigame.getMCOwner(), minigame);
        for (Pair<Byte, MapleCharacter> visitorz : minigame.getVisitors()) {
            mplew.write(visitorz.getLeft());
            addGameInfo(mplew, visitorz.getRight(), minigame);
        }
        mplew.write(-1);
        mplew.writeMapleAsciiString(minigame.getDescription());
        mplew.writeShort(minigame.getPieceType());
        return mplew.getPacket();
    }

    /*
     * 游戏准备就绪
     */
    public static byte[] getMiniGameReady(boolean ready) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write((ready ? InteractionOpcode.准备开始.getValue() : InteractionOpcode.准备就绪.getValue())); //0x38 : 0x39
        return mplew.getPacket();
    }

    public static byte[] getMiniGameExitAfter(boolean ready) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(ready ? InteractionOpcode.退出游戏.getValue() : InteractionOpcode.取消退出.getValue());//0x36 : 0x37
        return mplew.getPacket();
    }

    public static byte[] getMiniGameStart(int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.开始游戏.getValue());//3B
        mplew.write(loser == 1 ? 0 : 1);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameSkip(int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.SKIP.getValue());//0x3D
        //owner = 1 visitor = 0?
        mplew.write(slot);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameRequestTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.请求平局.getValue());//0x30
        return mplew.getPacket();
    }

    public static byte[] getMiniGameDenyTie() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.应答平局.getValue());//0x31
        return mplew.getPacket();
    }

    public static byte[] getMiniGameFull() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.writeShort(InteractionOpcode.房间.getValue());
        mplew.write(2);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameMoveOmok(int move1, int move2, int move3) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.移动棋子.getValue());//0x3E
        mplew.writeInt(move1);
        mplew.writeInt(move2);
        mplew.write(move3);
        return mplew.getPacket();
    }

    public static byte[] getMiniGameNewVisitor(MapleCharacter c, int slot, MapleMiniGame game) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.访问.getValue());//4
        mplew.write(slot);
        PacketHelper.addCharLook(mplew, c, false, false);
        mplew.writeMapleAsciiString(c.getName());
        mplew.writeShort(c.getJob());
        addGameInfo(mplew, c, game);
        return mplew.getPacket();
    }

    public static void addGameInfo(MaplePacketLittleEndianWriter mplew, MapleCharacter chr, MapleMiniGame game) {
        mplew.writeInt(game.getGameType()); // start of visitor; unknown
        mplew.writeInt(game.getWins(chr));
        mplew.writeInt(game.getTies(chr));
        mplew.writeInt(game.getLosses(chr));
        mplew.writeInt(game.getScore(chr)); // points
    }

    public static byte[] getMiniGameClose(byte number) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.退出.getValue());//0xA
        mplew.write(1);
        mplew.write(number);
        return mplew.getPacket();
    }

    public static byte[] getMatchCardStart(MapleMiniGame game, int loser) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.开始游戏.getValue());//0x3B
        mplew.write(loser == 1 ? 0 : 1);
        int times = game.getPieceType() == 1 ? 20 : (game.getPieceType() == 2 ? 30 : 12);
        mplew.write(times);
        for (int i = 1; i <= times; i++) {
            mplew.writeInt(game.getCardId(i));
        }
        return mplew.getPacket();
    }

    public static byte[] getMatchCardSelect(int turn, int slot, int firstslot, int type) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.选择卡片.getValue());//0x42
        mplew.write(turn);
        mplew.write(slot);
        if (turn == 0) {
            mplew.write(firstslot);
            mplew.write(type);
        }
        return mplew.getPacket();
    }

    public static byte[] getMiniGameResult(MapleMiniGame game, int type, int x) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.GAME_RESULT.getValue());//0x3C
        mplew.write(type); //lose = 0, tie = 1, win = 2
        game.setPoints(x, type);
        if (type != 0) {
            game.setPoints(x == 1 ? 0 : 1, type == 2 ? 0 : 1);
        }
        if (type != 1) {
            if (type == 0) {
                mplew.write(x == 1 ? 0 : 1); //who did it?
            } else {
                mplew.write(x);
            }
        }
        addGameInfo(mplew, game.getMCOwner(), game);
        for (Pair<Byte, MapleCharacter> visitorz : game.getVisitors()) {
            addGameInfo(mplew, visitorz.right, game);
        }

        return mplew.getPacket();
    }

    /*
     * 雇佣商店访问者名单
     */
    public static byte[] MerchantVisitorView(Map<String, VisitorInfo> visitor) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.雇佣商店_查看访问名单.getValue()); //V.110修改 以前是 0x24
        mplew.writeShort(visitor.size());
        for (Entry<String, VisitorInfo> ret : visitor.entrySet()) {
            mplew.writeMapleAsciiString(ret.getKey());
            mplew.writeInt(ret.getValue().getInTime()); //访问时间是几分钟前
        }

        return mplew.getPacket();
    }

    /*
     * 雇佣商店黑名单
     */
    public static byte[] MerchantBlackListView(List<String> blackList) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.雇佣商店_查看黑名单.getValue()); //V.110修改 以前是 0x25
        mplew.writeShort(blackList.size());
        for (String visit : blackList) {
            mplew.writeMapleAsciiString(visit);
        }
        return mplew.getPacket();
    }

    public static byte[] FishNotice() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.FISH_NOTICE.getValue());
        mplew.write(75);
        mplew.write(24);

        return mplew.getPacket();
    }

    public static byte[] FishExit() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.PLAYER_INTERACTION.getValue());
        mplew.write(InteractionOpcode.退出.getValue());
        mplew.write(0);
        mplew.write(17);

        return mplew.getPacket();
    }
}
