package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleQuestStatus;
import client.RockPaperScissors;
import client.inventory.Equip;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.MapleInventoryType;
import configs.ServerConfig;
import constants.GameConstants;
import constants.ItemConstants;
import constants.ServerConstants;
import handling.opcode.EffectOpcode;
import handling.opcode.NPCTalkOP;
import handling.opcode.SendPacketOpcode;
import handling.world.WorldBroadcastService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import scripting.item.ItemScriptManager;
import scripting.npc.NPCScriptManager;
import scripting.quest.QuestScriptManager;
import server.AutobanManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleStorage;
import server.life.MapleNPC;
import server.maps.MapleQuickMove;
import server.quest.MapleQuest;
import server.shop.MapleShop;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianAccessor;
import tools.data.output.MaplePacketLittleEndianWriter;
import tools.packet.EffectPacket;
import tools.packet.NPCPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NPCHandler {

    /**
     * Logger for this class.
     */
    private static final Logger log = LogManager.getLogger(NPCHandler.class);

    /*
     * NPC自己说话和移动效果
     */
    public static void NPCAnimation(LittleEndianAccessor slea, MapleClient c) {
//        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
//        int length = (int) slea.available();
//        byte[] bytes = slea.read(length);
//        mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
//        mplew.write(bytes);
//        c.announce(mplew.getPacket());


        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.NPC_ACTION.getValue());
        int length = (int) slea.available();
        int npcOid = slea.readInt();
        byte type1 = slea.readByte();
        byte type2 = slea.readByte();
        mplew.writeInt(npcOid);
        mplew.write(type1);
        mplew.write(type2);
        if (length == 10) {
            mplew.writeInt(slea.readInt());
        } else if (length > 6) {
            mplew.write(slea.read(length - 6));
        } else {
            return;
        }
        if (type2 == 1) {
            c.announce(NPCPacket.moveNpc(mplew));
            return;
        }
        c.announce(mplew.getPacket());
    }

    /*
     * NPC商店操作
     */
    public static void NPCShop(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte bmode = slea.readByte();
        if (chr == null) {
            return;
        }
        MapleShop shop = chr.getShop();
        if (shop == null) {
            return;
        }
        switch (bmode) {
            case 0: { //购买道具
                short position = slea.readShort(); //道具在商店的位置
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                shop.buy(c, itemId, quantity, position);
                break;
            }
            case 1: { //出售道具
                byte slot = (byte) slea.readShort();
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                shop.sell(c, ItemConstants.getInventoryType(itemId), slot, quantity);
                break;
            }
            case 2: { //冲飞镖和子弹数量
                byte slot = (byte) slea.readShort();
                shop.recharge(c, slot);
                break;
            }
            default:
                chr.setConversation(0);
                break;
        }
    }

    /*
     * NPC对话操作
     */
    public static void NPCTalk(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getBattle() != null || !c.canClickNPC()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleNPC npc = chr.getMap().getNPCByOid(slea.readInt());
        if (npc == null) {
            return;
        }
        if (chr.hasBlockedInventory()) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        chr.setCurrenttime(System.currentTimeMillis());
        if (chr.getCurrenttime() - chr.getLasttime() < chr.getDeadtime()) {
            if (chr.isGM()) {
                chr.dropMessage(5, "连接速度过快，请稍后再试。");
            }

            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        chr.setLasttime(System.currentTimeMillis());
//        c.getPlayer().updateTick(slea.readInt()); //暂时不检测点NPC的速度
        if (npc.hasShop()) {
            chr.setConversation(1);
            npc.sendShop(c);
        } else {
            NPCScriptManager.getInstance().start(c, npc.getId());
        }
    }

    /*
     * 任务操作
     */
    public static void QuestAction(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte action = slea.readByte();
        int quest = slea.readInt();
        //log.info("开始执行任务ID: " + quest + " 操作类型: " + action);
        if (chr == null) {
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleQuest q = MapleQuest.getInstance(quest);
        switch (action) {
            case 0: { // Restore lost item
                slea.readInt();
                int itemid = slea.readInt();
                q.RestoreLostItem(chr, itemid);
                break;
            }
            case 1: { // 开始任务
                int npc = slea.readInt();
                if (!q.hasStartScript()) {
                    //暂时关闭脚本任务 直接完成
                    q.start(chr, npc);
                    if (chr.isShowPacket()) {
                        chr.dropMessage(6, "开始系统任务 NPC: " + npc + " Quest：" + quest);
                    }
//                    switch (quest) {
//                        case 18583:
//                        case 18584:
//                        case 18585:
//                            q.forceStart(chr, npc, "");
//                            break;
//                        default:
//                            q.forceComplete(chr, npc);
//                            break;
//                    }
                }
                break;
            }
            case 2: { // 完成任务
                int npc = slea.readInt();
                slea.readInt();
                if (q.hasEndScript()) {
                    return;
                }
                if (slea.available() >= 4) {
                    q.complete(chr, npc, slea.readInt());
                } else {
                    q.complete(chr, npc);
                }
                if (chr.isShowPacket()) {
                    chr.dropMessage(6, "完成系统任务 NPC: " + npc + " Quest: " + quest);
                }
                // 6 = start quest
                // 7 = unknown error
                // 8 = equip is full
                // 9 = not enough mesos
                // 11 = due to the equipment currently being worn wtf o.o
                // 12 = you may not posess more than one of this item
                break;
            }
            case 3: { // 放弃任务
                if (GameConstants.canForfeit(q.getId())) {
                    q.forfeit(chr);
                    if (chr.isShowPacket()) {
                        chr.dropMessage(6, "放弃系统任务 Quest: " + quest);
                    }
                } else {
                    chr.dropMessage(1, "无法放弃这个任务.");
                }
                break;
            }
            case 4: { // 脚本任务开始
                int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                QuestScriptManager.getInstance().startQuest(c, npc, quest);
                if (chr.isAdmin() && ServerConstants.isShowPacket()) {
                    chr.dropMessage(6, "执行脚本任务 NPC：" + npc + " Quest: " + quest);
                }
                break;
            }
            case 5: { // 脚本任务结束
                int npc = slea.readInt();
                if (chr.hasBlockedInventory()) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                QuestScriptManager.getInstance().endQuest(c, npc, quest, false);
                if (chr.isShowPacket()) {
                    chr.dropMessage(6, "完成脚本任务 NPC：" + npc + " Quest: " + quest);
                }
                break;
            }
            case 6: {
                if (chr.isShowPacket()) {
                    chr.dropMessage(6, "完成脚本任务 Quest: " + quest);
                }
                chr.send(EffectPacket.showSpecialEffect(EffectOpcode.UserEffect_QuestComplete.getValue())); // 任务完成
                chr.getMap().broadcastMessage(chr, EffectPacket.showForeignEffect(chr.getId(), EffectOpcode.UserEffect_QuestComplete.getValue()), false);
            }
        }
    }

    /*
     * 仓库操作
     */
    public static void Storage(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        byte mode = slea.readByte();
        if (chr == null) {
            return;
        }
        if (chr.getAntiMacro().inProgress()) {
            chr.dropMessage(5, "被使用测谎仪时无法操作。");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        MapleStorage storage = chr.getStorage();
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        switch (mode) {
            case 4: { // 取出
                byte type = slea.readByte();
                byte slot = slea.readByte();
                slot = storage.getSlot(MapleInventoryType.getByType(type), slot);
                Item item = storage.getItem(slot); //获取道具在仓库的信息
                if (item != null) {
                    //检测是否是唯一道具
                    if (ii.isPickupRestricted(item.getItemId()) && chr.getItemQuantity(item.getItemId(), true) > 0) {
                        c.announce(NPCPacket.getStorageError((byte) 0x0C));
                        return;
                    }
                    //检测取回道具金币是否足够
                    int meso = storage.getNpcId() == 9030100 || storage.getNpcId() == 9031016 ? 1000 : 0;
                    if (chr.getMeso() < meso) {
                        c.announce(NPCPacket.getStorageError((byte) 0x0B)); //取回道具金币不足
                        return;
                    }
                    //检测角色背包是否有位置
                    if (MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                        item = storage.takeOut(slot); //从仓库中取出这个道具
                        short flag = item.getFlag();
                        if (ItemFlag.可以交换1次.check(flag)) {
                            item.setFlag((short) (flag - ItemFlag.可以交换1次.getValue()));
                        } else if (ItemFlag.宿命剪刀.check(flag)) {
                            item.setFlag((short) (flag - ItemFlag.宿命剪刀.getValue()));
                        } else if (ItemFlag.转存吊牌.check(flag)) {
                            item.setFlag((short) (flag - ItemFlag.转存吊牌.getValue()));
                        } else if (ItemFlag.宿命剪刀.check(flag)) {
                            item.setFlag((short) (flag - ItemFlag.宿命剪刀.getValue()));
                        }
                        MapleInventoryManipulator.addFromDrop(c, item, false); //给角色道具
                        if (meso > 0) { //扣取角色的金币
                            chr.gainMeso(-meso, false);
                        }
                        storage.sendTakenOut(c, ItemConstants.getInventoryType(item.getItemId()));
                    } else {
                        c.announce(NPCPacket.getStorageError((byte) 0x0A)); //发送背包是满的封包
                    }
                } else {
                    //AutobanManager.getInstance().autoban(c, "试图从仓库取出不存在的道具.");
                    log.info("[作弊] " + chr.getName() + " (等级 " + chr.getLevel() + ") 试图从仓库取出不存在的道具.");
                    WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] 玩家: " + chr.getName() + " (等级 " + chr.getLevel() + ") 试图从仓库取出不存在的道具."));
                    c.announce(MaplePacketCreator.enableActions());
                }
                break;
            }
            case 5: { // 存入
                byte slot = (byte) slea.readShort();
                int itemId = slea.readInt();
                short quantity = slea.readShort();
                //检测保存道具的数量是否小于 1 
                if (quantity < 1) {
                    AutobanManager.getInstance().autoban(c, "试图存入到仓库的道具数量: " + quantity + " 道具ID: " + itemId);
                    return;
                }
                //检测仓库的道具是否已满
                if (storage.isFull()) {
                    c.announce(NPCPacket.getStorageError((byte) 0x11));
                    return;
                }
                //检测角色背包当前道具是否有道具
                MapleInventoryType type = ItemConstants.getInventoryType(itemId);
                if (chr.getInventory(type).getItem(slot) == null) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                //检测金币是否足够
                int meso = storage.getNpcId() == 9030100 || storage.getNpcId() == 9031016 ? 500 : 100;
                if (chr.getMeso() < meso) {
                    c.announce(NPCPacket.getStorageError((byte) 0x10));
                    return;
                }
                //开始操作保存道具到仓库
                Item item = chr.getInventory(type).getItem(slot).copy();
                //检测道具是否为宠物道具
                if (ItemConstants.isPet(item.getItemId())) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                //检测道具是否为唯一道具 且角色仓库已经有这个道具
                if (ii.isPickupRestricted(item.getItemId()) && storage.findById(item.getItemId()) != null) {
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
                if (item.getItemId() == itemId && (item.getQuantity() >= quantity || ItemConstants.isRechargable(itemId))) {
                    //如果是飞镖子弹道具就设置保存的数量为道具当前的数量
                    if (ItemConstants.isRechargable(itemId)) {
                        quantity = item.getQuantity();
                    }
                    MapleInventoryManipulator.removeFromSlot(c, type, slot, quantity, false); //删除角色背包中的道具
                    chr.gainMeso(-meso, false, false); //收取保存到仓库的费用
                    item.setQuantity(quantity); //设置道具的数量
                    storage.store(item); //存入道具到仓库
                    storage.sendStored(c, ItemConstants.getInventoryType(itemId)); //发送当前仓库的道具封包
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "试图存入到仓库的道具: " + itemId + " 数量: " + quantity + " 当前玩家用道具: " + item.getItemId() + " 数量: " + item.getQuantity());
                }
                break;
            }
            case 6: { // 仓库物品排序
                storage.arrange();
                storage.update(c);
                break;
            }
            case 7: { // 金币
                long meso = slea.readLong();
                long storageMesos = storage.getMeso();
                long playerMesos = chr.getMeso();
                if ((meso > 0 && storageMesos >= meso) || (meso < 0 && playerMesos >= -meso)) {
                    if (meso < 0 && (storageMesos - meso) < 0) {
                        meso = (int) -(ServerConfig.CHANNEL_PLAYER_MAXMESO - storageMesos);
                        if ((-meso) > playerMesos) {
                            return;
                        }
                    } else if (meso > 0 && (playerMesos + meso) < 0) {
                        meso = (int) (ServerConfig.CHANNEL_PLAYER_MAXMESO - playerMesos);
                        if ((meso) > storageMesos) {
                            return;
                        }
                    }
                    if (meso > 0 && chr.getMeso() + meso > ServerConfig.CHANNEL_PLAYER_MAXMESO) {
                        chr.dropMessage(1, "玩家身上装备不能超过100E。");
                        c.announce(NPCPacket.getStorageError((byte) 0x17));
                        return;
                    }
                    storage.setMeso(storageMesos - meso);
                    chr.gainMeso(meso, false, false);
                } else {
                    AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to store or take out unavailable amount of mesos (" + meso + "/" + storage.getMeso() + "/" + c.getPlayer().getMeso() + ")");
                    return;
                }
                storage.sendMeso(c);
                break;
            }
            case 8: { // 退出仓库
                storage.close();
                chr.setConversation(0);
                break;
            }
            default:
                System.out.println("Unhandled Storage mode : " + mode);
                break;
        }
    }

    /*
     * 和NPC交谈也就是对话操作
     */
    public static void NPCMoreTalk(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter player = c.getPlayer();
        if (player == null) {
            return;
        }
        if (player.getConversation() != 1) {
            return;
        }
        byte lastMsg = slea.readByte(); // 00 last message type (2 = yesno, 0F = acceptdecline)
        NPCTalkOP op = NPCTalkOP.Companion.getNPCTalkOP(lastMsg);
        if (op == null) {
            c.getPlayer().dropMessage(1, "未知的NPC操作类型:" + lastMsg);
            return;
        }
        if (op == NPCTalkOP.HAIR) { //更换发型出现
            slea.skip(2); //[00 00]
        }
        byte action = slea.readByte(); // 00 = end chat/no/decline, 01 == next/yes/accept
        int selection = -1;
        switch (op) {
            case TEXT:
                if (action != 0) {
                    String returnText = slea.readMapleAsciiString();
                    if (player.isShowPacket()) {
                        //player.dropSpouseMessage(0x0A, "Npc对话 - lastMsg: " + lastMsg + " action: " + action + " Text: " + returnText);
                    }
                    if (c.getQM() != null) {
                        c.getQM().setGetText(returnText);
                        if (c.getQM().isStart()) {
                            QuestScriptManager.getInstance().startAction(c, action, lastMsg, -1);
                        } else {
                            QuestScriptManager.getInstance().endAction(c, action, lastMsg, -1);
                        }
                    } else if (c.getIM() != null) {
                        c.getIM().setGetText(returnText);
                        ItemScriptManager.getInstance().action(c, action, lastMsg, -1);
                    } else if (c.getCM() != null) {
                        c.getCM().setGetText(returnText);
                        NPCScriptManager.getInstance().action(c, action, lastMsg, -1);
                    }
                }
                return;
            case MIXED_HAIR:
                if (action == 0)
                    break;
                slea.skip(6);
                c.getPlayer().handleMixHairColor((byte) slea.readInt(), (byte) slea.readInt(), (byte) slea.readInt());
                return;
            default:
                if (slea.available() >= 4) {
                    selection = slea.readInt();
                } else if (slea.available() > 0) {
                    selection = slea.readByte();
                }
                if (player.isShowPacket()) {
                    //player.dropSpouseMessage(0x14, "Npc对话 - lastMsg: " + lastMsg + " action: " + action + " selection: " + selection);
                }
        }

        if (selection >= -1 && action != -1) {
            if (c.getQM() != null) {
                if (c.getQM().isStart()) {
                    QuestScriptManager.getInstance().startAction(c, action, lastMsg, selection);
                } else {
                    QuestScriptManager.getInstance().endAction(c, action, lastMsg, selection);
                }
            } else if (c.getIM() != null) {
                ItemScriptManager.getInstance().action(c, action, lastMsg, selection);
            } else if (c.getCM() != null) {
                NPCScriptManager.getInstance().action(c, action, lastMsg, selection);
            }
        } else {
            if (c.getQM() != null) {
                c.getQM().dispose();
            } else if (c.getIM() != null) {
                c.getIM().dispose();
            } else if (c.getCM() != null) {
                c.getCM().dispose();
            }
        }
    }

    /*
     * 全部装备持久修复
     */
    public static void repairAll(MapleClient c) {
        Equip eq;
        double rPercentage;
        int price = 0;
        Map<String, Integer> eqStats;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Map<Equip, Integer> eqs = new HashMap<>();
        MapleInventoryType[] types = {MapleInventoryType.EQUIP, MapleInventoryType.EQUIPPED};
        for (MapleInventoryType type : types) {
            for (Item item : c.getPlayer().getInventory(type).newList()) {
                if (item instanceof Equip) { //redundant
                    eq = (Equip) item;
                    if (eq.getDurability() >= 0) {
                        eqStats = ii.getItemBaseInfo(eq.getItemId());
                        if (eqStats.containsKey("durability") && eqStats.get("durability") > 0 && eq.getDurability() < eqStats.get("durability")) {
                            rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
                            eqs.put(eq, eqStats.get("durability"));
                            price += (int) Math.ceil(rPercentage * ii.getUnitPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0));
                        }
                    }
                }
            }
        }
        if (eqs.size() <= 0 || c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, true);
        Equip ez;
        for (Entry<Equip, Integer> eqqz : eqs.entrySet()) {
            ez = eqqz.getKey();
            ez.setDurability(eqqz.getValue());
            c.getPlayer().forceUpdateItem(ez.copy());
        }
    }

    /*
     * 当个装备持久修复
     */
    public static void repair(LittleEndianAccessor slea, MapleClient c) {
        if (slea.available() < 4) {
            return;
        }
        int position = slea.readInt(); //who knows why this is a int
        MapleInventoryType type = position < 0 ? MapleInventoryType.EQUIPPED : MapleInventoryType.EQUIP;
        Item item = c.getPlayer().getInventory(type).getItem((byte) position);
        if (item == null) {
            return;
        }
        Equip eq = (Equip) item;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Map<String, Integer> eqStats = ii.getItemBaseInfo(item.getItemId());
        if (eq.getDurability() < 0 || !eqStats.containsKey("durability") || eqStats.get("durability") <= 0 || eq.getDurability() >= eqStats.get("durability")) {
            return;
        }
        double rPercentage = (100.0 - Math.ceil((eq.getDurability() * 1000.0) / (eqStats.get("durability") * 10.0)));
        //drpq level 105 weapons - ~420k per %; 2k per durability point
        //explorer level 30 weapons - ~10 mesos per %
        int price = (int) Math.ceil(rPercentage * ii.getUnitPrice(eq.getItemId()) / (ii.getReqLevel(eq.getItemId()) < 70 ? 100.0 : 1.0)); // / 100 for level 30?
        //TODO: need more data on calculating off client
        if (c.getPlayer().getMeso() < price) {
            return;
        }
        c.getPlayer().gainMeso(-price, false);
        eq.setDurability(eqStats.get("durability"));
        c.getPlayer().forceUpdateItem(eq.copy());
    }

    /*
     * 更新任务信息
     */
    public static void UpdateQuest(LittleEndianAccessor slea, MapleClient c) {
        MapleQuest quest = MapleQuest.getInstance(slea.readShort());
        if (quest != null) {
            c.getPlayer().updateQuest(c.getPlayer().getQuest(quest), true);
        }
    }

    public static void UseItemQuest(LittleEndianAccessor slea, MapleClient c) {
        short slot = slea.readShort();
        int itemId = slea.readInt();
        Item item = c.getPlayer().getInventory(MapleInventoryType.ETC).getItem(slot);
        int qid = slea.readInt();
        MapleQuest quest = MapleQuest.getInstance(qid);
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, Map<Integer, Integer>> questItemInfo = null;
        boolean found = false;
        for (Item i : c.getPlayer().getInventory(MapleInventoryType.ETC)) {
            if (i.getItemId() / 10000 == 422) {
                questItemInfo = ii.questItemInfo(i.getItemId());
                if (questItemInfo != null && questItemInfo.getLeft() == qid && questItemInfo.getRight() != null && questItemInfo.getRight().containsKey(itemId)) {
                    found = true;
                    break; //i believe it's any order
                }
            }
        }
        if (quest != null && found && item != null && item.getQuantity() > 0 && item.getItemId() == itemId) {
            int newData = slea.readInt();
            MapleQuestStatus stats = c.getPlayer().getQuestNoAdd(quest);
            if (stats != null && stats.getStatus() == 1) {
                stats.setCustomData(String.valueOf(newData));
                c.getPlayer().updateQuest(stats, true);
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.ETC, slot, questItemInfo.getRight().get(item.getItemId()).shortValue(), false);
            }
        }
    }

    public static void RPSGame(LittleEndianAccessor slea, MapleClient c) {
        if (slea.available() == 0 || c.getPlayer() == null || c.getPlayer().getMap() == null || !c.getPlayer().getMap().containsNPC(9000019)) {
            if (c.getPlayer() != null && c.getPlayer().getRPS() != null) {
                c.getPlayer().getRPS().dispose(c);
            }
            return;
        }
        byte mode = slea.readByte();
        switch (mode) {
            case 0: //start game
            case 5: //retry
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().reward(c);
                }
                if (c.getPlayer().getMeso() >= 1000) {
                    c.getPlayer().setRPS(new RockPaperScissors(c, mode));
                } else {
                    c.announce(MaplePacketCreator.getRPSMode((byte) 0x08, -1, -1, -1));
                }
                break;
            case 1: //answer
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().answer(c, slea.readByte())) {
                    c.announce(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 2: //time over
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().timeOut(c)) {
                    c.announce(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 3: //continue
                if (c.getPlayer().getRPS() == null || !c.getPlayer().getRPS().nextRound(c)) {
                    c.announce(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
            case 4: //leave
                if (c.getPlayer().getRPS() != null) {
                    c.getPlayer().getRPS().dispose(c);
                } else {
                    c.announce(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
                }
                break;
        }

    }

    /*
     * 使用小地图下面的快速移动
     */
    public static void OpenQuickMoveNpc(LittleEndianAccessor slea, MapleClient c) {
        int npcid = slea.readInt();
        if (c.getPlayer().hasBlockedInventory() || c.getPlayer().isInBlockedMap() || c.getPlayer().getLevel() < 10) {
            c.getPlayer().dropMessage(-1, "您当前已经和1个NPC对话了. 如果不是请输入 @ea 命令进行解卡。");
            return;
        }
        for (MapleQuickMove pn : MapleQuickMove.values()) {
            if (pn.getNpcId() == npcid) {
                NPCScriptManager.getInstance().start(c, npcid);
                break;
            }
        }
    }
}
