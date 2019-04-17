package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import configs.ServerConfig;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MerchItemPackage;
import server.shops.HiredMerchant;
import tools.DateUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianAccessor;
import tools.packet.PlayerShopPacket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HiredMerchantHandler {

    private static final Logger log = LogManager.getLogger(HiredMerchantHandler.class.getName());

    public static boolean UseHiredMerchant(MapleClient c, boolean packet) {
        MapleCharacter chr = c.getPlayer();
        if (c.getChannelServer().isShutdown()) {
            chr.dropMessage(1, "服务器即将关闭维护，暂时无法进行开店。");
            return false;
        }
        if (chr.getMap() != null && chr.getMap().allowPersonalShop()) {
            HiredMerchant merchant = World.getMerchant(chr.getAccountID(), chr.getId());
            if (merchant != null) {
                c.announce(PlayerShopPacket.sendTitleBox(0x08, merchant.getMapId(), merchant.getChannel() - 1));
            } else {
                //System.out.println("是否有道具: " + ItemLoader.雇佣道具.loadItems(false, chr.getId()).isEmpty() + " 是否有金币: " + chr.getMerchantMeso());
                if (loadItemFrom_Database(chr) == null) {
                    if (packet) {
                        c.announce(PlayerShopPacket.sendTitleBox(0x07));
                    }
                    return true;
                } else {
                    c.announce(PlayerShopPacket.sendTitleBox(0x09));
                }
            }
        }
        return false;
    }

    public static int getMerchMesos(MapleCharacter chr) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * from hiredmerch where characterid = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                rs.close();
                return 0;
            }
            int mesos = rs.getInt("Mesos");
            rs.close();
            ps.close();
            return mesos > 0 ? mesos : 0;
        } catch (SQLException se) {
            return 0;
        }
    }

    public static void MerchantItemStore(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        if (chr == null) {
            return;
        }
        if (c.getChannelServer().isShutdown()) {
            chr.dropMessage(1, "服务器即将关闭维护，暂时无法进行道具取回。");
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        byte operation = slea.readByte();
        switch (operation) {
            case 0x17: //打开NPC V.115修改 以前0x15
                HiredMerchant merchant = World.getMerchant(chr.getAccountID(), chr.getId());
                if (merchant != null) {
                    c.announce(PlayerShopPacket.merchItemStore(merchant.getMapId(), merchant.getChannel() - 1));
                    chr.setConversation(0);
                } else {
                    MerchItemPackage pack = loadItemFrom_Database(chr);
                    //System.out.println("加载弗洛兰德道具信息 pack " + pack);
                    if (pack == null) {
                        c.announce(PlayerShopPacket.merchItemStore(999999999, 0));
                        chr.setConversation(0);
                    } else {
                        c.announce(PlayerShopPacket.merchItemStore_ItemData(pack));
                    }
                }
                break;
            case 0x18: //不取回道具
                if (chr.getConversation() != 3) {
                    return;
                }
                c.announce(PlayerShopPacket.merchItemStore((byte) 0x2D));
                break;
            case 0x1F: //取回道具  V.131修改 以前0x1D
                if (chr.getConversation() != 3) {
                    return;
                }
                boolean merch = World.hasMerchant(chr.getAccountID(), chr.getId());
                if (merch) {
                    chr.dropMessage(1, "请关闭现有的商店.");
                    chr.setConversation(0);
                    return;
                }
                MerchItemPackage pack = loadItemFrom_Database(chr);
                if (pack == null) {
                    chr.dropMessage(1, "发生了未知错误.");
                    return;
                }
                int checkstatus = check(chr, pack);
                switch (checkstatus) {
                    case 1: //金币太多
                        c.announce(PlayerShopPacket.merchItem_Message((byte) 0x25));
                        return;
                    case 2: //背包栏位不足
                        c.announce(PlayerShopPacket.merchItem_Message((byte) 0x28));
                        return;
                    case 3: //道具有数量限制
                        c.announce(PlayerShopPacket.merchItem_Message((byte) 0x26));
                        return;
                }
                if (pack.getMesos() > 0 && chr.getMeso() + pack.getMesos() > ServerConfig.CHANNEL_PLAYER_MAXMESO) {
                    c.announce(PlayerShopPacket.merchItem_Message((byte) 0x25));
                    return;
                }
                if (deletePackage(chr.getId())) {
                    if (pack.getMesos() > 0) {
                        chr.gainMeso(pack.getMesos(), false);
                        log.info("[雇佣] " + chr.getName() + " 雇佣取回获得金币: " + pack.getMesos() + " 时间: " + DateUtil.getCurrentDate());
                        HiredMerchant.log.info(chr.getName() + " 雇佣取回获得金币: " + pack.getMesos());
                    }
                    for (Item item : pack.getItems()) {
                        MapleInventoryManipulator.addFromDrop(c, item, false);
                        HiredMerchant.log.info(chr.getName() + " 雇佣取回获得道具: " + item.getItemId() + " - " + MapleItemInformationProvider.getInstance().getName(item.getItemId()) + " 数量: " + item.getQuantity());
                    }
                    c.announce(PlayerShopPacket.merchItem_Message((byte) 0x24));
                } else {
                    chr.dropMessage(1, "发生了未知错误.");
                }
                break;
            case 0x21:  //退出 V.115修改 以前0x1D
                chr.setConversation(0);
                break;
            default:
                System.out.println("弗洛兰德：未知的操作类型 " + operation);
                break;
        }
    }

    public static void RemoteStore(LittleEndianAccessor slea, MapleClient c) {
        MapleCharacter chr = c.getPlayer();
        HiredMerchant merchant = World.getMerchant(chr.getAccountID(), chr.getId());
        if (merchant != null) {
            if (merchant.getChannel() == chr.getClient().getChannel()) {
                merchant.setOpen(false);
                merchant.removeAllVisitors((byte) 0x14, (byte) 0);
                chr.setPlayerShop(merchant);
                c.announce(PlayerShopPacket.getHiredMerch(chr, merchant, false));
            } else {
                c.announce(PlayerShopPacket.sendTitleBox(0x10, 0, merchant.getChannel() - 1));
            }
        } else {
            chr.dropMessage(1, "你没有开设商店");
        }
        c.announce(MaplePacketCreator.enableActions());
    }

    private static int check(MapleCharacter chr, MerchItemPackage pack) {
        if (chr.getMeso() + pack.getMesos() < 0) {
            log.info("[雇佣] " + chr.getName() + " 雇佣取回道具金币检测错误 时间: " + DateUtil.getCurrentDate());
            HiredMerchant.log.error(chr.getName() + " 雇佣取回道具金币检测错误");
            return 1;
        }
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (Item item : pack.getItems()) {
            MapleInventoryType invtype = ItemConstants.getInventoryType(item.getItemId());
            if (invtype == MapleInventoryType.EQUIP) {
                eq++;
            } else if (invtype == MapleInventoryType.USE) {
                use++;
            } else if (invtype == MapleInventoryType.SETUP) {
                setup++;
            } else if (invtype == MapleInventoryType.ETC) {
                etc++;
            } else if (invtype == MapleInventoryType.CASH) {
                cash++;
            }
            if (MapleItemInformationProvider.getInstance().isPickupRestricted(item.getItemId()) && chr.haveItem(item.getItemId(), 1)) {
                log.info("[雇佣] " + chr.getName() + " 雇佣取回道具是否可以捡取错误 时间: " + DateUtil.getCurrentDate());
                HiredMerchant.log.error(chr.getName() + " 雇佣取回道具是否可以捡取错误");
                return 3;
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
            log.info("[雇佣] " + chr.getName() + " 雇佣取回道具背包空间不够 时间: " + DateUtil.getCurrentDate());
            HiredMerchant.log.error(chr.getName() + " 雇佣取回道具背包空间不够");
            return 2;
        }
        return 0;
    }

    private static boolean deletePackage(int charId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE from hiredmerch where characterid = ?");
            ps.setInt(1, charId);
            ps.executeUpdate();
            ps.close();
            ItemLoader.雇佣道具.saveItems(con, null, charId);
            return true;
        } catch (SQLException e) {
            System.out.println("删除弗洛兰德道具信息出错" + e);
            return false;
        }
    }

    private static MerchItemPackage loadItemFrom_Database(MapleCharacter chr) {
        try {
            long mesos = chr.getMerchantMeso();
            Map<Long, Pair<Item, MapleInventoryType>> items = ItemLoader.雇佣道具.loadItems(false, chr.getId());
            if (mesos == 0 && items.isEmpty()) {
                //FileoutputUtil.hiredMerchLog(chr.getName(), "加载弗洛兰德道具信息 金币 " + mesos + " 是否有道具 " + items.size());
                return null;
            }
            MerchItemPackage pack = new MerchItemPackage();
            pack.setMesos(mesos);
            if (!items.isEmpty()) {
                List<Item> iters = new ArrayList<>();
                for (Pair<Item, MapleInventoryType> z : items.values()) {
                    iters.add(z.left);
                }
                pack.setItems(iters);
            }
            HiredMerchant.log.error(chr.getName() + " 弗洛兰德取回最后返回 金币: " + mesos + " 道具数量: " + items.size());
            return pack;
        } catch (SQLException e) {
            System.out.println("加载弗洛兰德道具信息出错" + e);
            return null;
        }
    }
}
