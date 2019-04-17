package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.inventory.Item;
import client.inventory.ItemFlag;
import client.inventory.ItemLoader;
import client.inventory.MapleInventoryType;
import com.alibaba.druid.pool.DruidPooledConnection;
import constants.GameConstants;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.world.WorldFindService;
import server.MapleDueyActions;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.LittleEndianAccessor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class DueyHandler {

    /*
     * 0x0C = 您的金币不够.
     * 0x0D = 这是错误的邀请.
     * 0x0E = 请重新确认接收人的姓名.
     * 0x0F = 不能发送给同一帐号的角色.
     * 0x10 = 接收人的快递栏已满.
     * 0x11 = 该角色无法接收快递.
     * 0x12 = 只能拥有一个的道具已经在该角色的快递栏里面.
     * 0x13 = 成功发送.
     * 0x14 = 出现不明错误.
     * 0x16 = 请查看是否有空间.
     * 0x17 = 因为只能拥有一个的道具无法找到金币和道具.
     * 0x18 = 领取或删除送货员的道具信息
     */
    public static void DueyOperation(LittleEndianAccessor slea, MapleClient c) {
        byte operation = slea.readByte();
        switch (operation) {
            case 0x01: { // 打开送货员窗口
//                String AS13Digit = slea.readMapleAsciiString();
                int conv = c.getPlayer().getConversation();
                if (conv == 2) { // Duey
                    c.announce(MaplePacketCreator.sendDuey((byte) 0x0A, loadItems(c.getPlayer())));
                }
                break;
            }
            case 0x03: { // 设置需要发送的道具信息
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                byte inventId = slea.readByte();
                short itemPos = slea.readShort();
                short amount = slea.readShort();
                int mesos = slea.readInt();
                String recipient = slea.readMapleAsciiString();
                boolean quickdelivery = slea.readByte() > 0;

                int finalcost = mesos + GameConstants.getTaxAmount(mesos) + (quickdelivery ? 0 : 5000);

                if (mesos >= 0 && mesos <= 100000000 && c.getPlayer().getMeso() >= finalcost) {
                    int accid = getAccIdFromName(recipient, true);
                    if (accid != -1) { //当前有这个角色ID的记录 也就是角色存在
                        if (accid != c.getAccID()) { //发送给其他角色的ID和发送者的角色ID不相同
                            boolean recipientOn = false;
                            int chz = WorldFindService.getInstance().findChannel(recipient);
                            if (chz > 0) {
                                MapleCharacter receiver = ChannelServer.getInstance(chz).getPlayerStorage().getCharacterByName(recipient);
                                if (receiver != null) {
                                    recipientOn = true;
                                }
                            }
                            if (inventId > 0) { //有发送装备的信息
                                MapleInventoryType inv = MapleInventoryType.getByType(inventId);
                                Item item = c.getPlayer().getInventory(inv).getItem((byte) itemPos);
                                if (item == null) {
                                    c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); //该角色无法接收快递.
                                    return;
                                }
                                short flag = item.getFlag();
                                if (ItemFlag.不可交易.check(flag) || ItemFlag.封印.check(flag)) {
                                    c.announce(MaplePacketCreator.enableActions());
                                    return;
                                }
                                if (c.getPlayer().getItemQuantity(item.getItemId(), false) >= amount) {
                                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                                    if (!ii.isDropRestricted(item.getItemId()) && !ii.isAccountShared(item.getItemId())) {
                                        if (addItemToDB(item, amount, mesos, c.getPlayer().getName(), getAccIdFromName(recipient, false), recipientOn)) {
                                            if (ItemConstants.is飞镖道具(item.getItemId()) || ItemConstants.is子弹道具(item.getItemId())) {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, item.getQuantity(), true);
                                            } else {
                                                MapleInventoryManipulator.removeFromSlot(c, inv, (byte) itemPos, amount, true, false);
                                            }
                                            c.getPlayer().gainMeso(-finalcost, false);
                                            c.announce(MaplePacketCreator.sendDuey((byte) 0x13, null)); // 成功发送.
                                        } else {
                                            c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); // 该角色无法接收快递.
                                        }
                                    } else {
                                        c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); // 该角色无法接收快递.
                                    }
                                } else {
                                    c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); // 该角色无法接收快递.
                                }
                            } else { //没有道具发送 只有金币
                                if (addMesoToDB(mesos, c.getPlayer().getName(), getAccIdFromName(recipient, false), recipientOn)) {
                                    c.getPlayer().gainMeso(-finalcost, false);
                                    c.announce(MaplePacketCreator.sendDuey((byte) 0x13, null)); // 成功发送.
                                } else {
                                    c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); // 该角色无法接收快递.
                                }
                            }
                        } else {
                            c.announce(MaplePacketCreator.sendDuey((byte) 0x0F, null)); // 不能发送给同一帐号的角色.
                        }
                    } else {
                        c.announce(MaplePacketCreator.sendDuey((byte) 0x0E, null)); // 请重新确认接收人的姓名.
                    }
                } else {
                    c.announce(MaplePacketCreator.sendDuey((byte) 0x0C, null)); // 您的金币不够.
                }
                break;
            }
            case 0x05: { // 接收未领取的道具
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                int packageid = slea.readInt();
                //System.out.println("Item attempted : " + packageid);
                MapleDueyActions dp = loadSingleItem(packageid, c.getPlayer().getId());
                if (dp == null) {
                    return;
                }
                if (dp.getItem() != null && !MapleInventoryManipulator.checkSpace(c, dp.getItem().getItemId(), dp.getItem().getQuantity(), dp.getItem().getOwner())) {
                    c.announce(MaplePacketCreator.sendDuey((byte) 0x16, null)); // 请查看是否有空间.
                    return;
                } else if (dp.getMesos() < 0 || (dp.getMesos() + c.getPlayer().getMeso()) < 0) {
                    c.announce(MaplePacketCreator.sendDuey((byte) 0x11, null)); // 该角色无法接收快递.
                    return;
                }
                removeItemFromDB(packageid, c.getPlayer().getId()); // Remove first
                if (dp.getItem() != null) {
                    MapleInventoryManipulator.addFromDrop(c, dp.getItem(), false);
                }
                if (dp.getMesos() != 0) {
                    c.getPlayer().gainMeso(dp.getMesos(), false);
                }
                c.announce(MaplePacketCreator.removeItemFromDuey(false, packageid));
                break;
            }
            case 0x06: { // 删除未领取的道具
                if (c.getPlayer().getConversation() != 2) {
                    return;
                }
                int packageid = slea.readInt();
                removeItemFromDB(packageid, c.getPlayer().getId());
                c.announce(MaplePacketCreator.removeItemFromDuey(true, packageid));
                break;
            }
            case 0x08: { // 关闭送货窗口
                c.getPlayer().setConversation(0);
                break;
            }
            default: {
                System.out.println("Unhandled Duey operation : " + slea.toString());
                break;
            }
        }
    }

    private static boolean addMesoToDB(int mesos, String sName, int recipientID, boolean isOn) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);
            ps.setInt(6, 3);

            ps.executeUpdate();
            ps.close();

            return true;
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        }
    }

    private static boolean addItemToDB(Item item, int quantity, int mesos, String sName, int recipientID, boolean isOn) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("INSERT INTO dueypackages (RecieverId, SenderName, Mesos, TimeStamp, Checked, Type) VALUES (?, ?, ?, ?, ?, ?)", DatabaseConnection.RETURN_GENERATED_KEYS);
            ps.setInt(1, recipientID);
            ps.setString(2, sName);
            ps.setInt(3, mesos);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, isOn ? 0 : 1);

            ps.setInt(6, item.getType());
            ps.executeUpdate();
            ps.close();
            if (item != null) {
                ItemLoader.送货道具.saveItems(con, Collections.singletonList(new Pair<>(item, ItemConstants.getInventoryType(item.getItemId()))), recipientID);
            }
            return true;
        } catch (SQLException se) {
            se.printStackTrace();
            return false;
        }
    }

    public static List<MapleDueyActions> loadItems(MapleCharacter chr) {
        List<MapleDueyActions> packages = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE RecieverId = ?");
            ps.setInt(1, chr.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(rs.getInt("packageid"));
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                packages.add(dueypack);
            }
            rs.close();
            ps.close();
            return packages;
        } catch (SQLException se) {
            se.printStackTrace();
            return null;
        }
    }

    public static MapleDueyActions loadSingleItem(int packageid, int charid) {
        List<MapleDueyActions> packages = new LinkedList<>();
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM dueypackages WHERE PackageId = ? AND RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                MapleDueyActions dueypack = getItemByPID(packageid);
                dueypack.setSender(rs.getString("SenderName"));
                dueypack.setMesos(rs.getInt("Mesos"));
                dueypack.setSentTime(rs.getLong("TimeStamp"));
                packages.add(dueypack);
                rs.close();
                ps.close();
                return dueypack;
            } else {
                rs.close();
                ps.close();
                return null;
            }
        } catch (SQLException se) {
            return null;
        }
    }

    public static void reciveMsg(MapleClient c, int recipientId) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("UPDATE dueypackages SET Checked = 0 WHERE RecieverId = ?");
            ps.setInt(1, recipientId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static void removeItemFromDB(int packageid, int charid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("DELETE FROM dueypackages WHERE PackageId = ? AND RecieverId = ?");
            ps.setInt(1, packageid);
            ps.setInt(2, charid);
            ps.executeUpdate();
            ps.close();
            ItemLoader.送货道具.saveItems(con, null, packageid);
        } catch (SQLException se) {
            se.printStackTrace();
        }
    }

    private static MapleDueyActions getItemByPID(int packageid) {
        try {
            Map<Long, Pair<Item, MapleInventoryType>> iter = ItemLoader.送货道具.loadItems(false, packageid);
            if (iter != null && iter.size() > 0) {
                for (Pair<Item, MapleInventoryType> i : iter.values()) {
                    return new MapleDueyActions(packageid, i.getLeft());
                }
            }
        } catch (Exception se) {
            se.printStackTrace();
        }
        return new MapleDueyActions(packageid);
    }

    private static int getAccIdFromName(String name, boolean accountid) {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                rs.close();
                ps.close();
                return -1;
            }
            int id_ = accountid ? rs.getInt("accountid") : rs.getInt("id");
            rs.close();
            ps.close();
            return id_;
        } catch (SQLException ignored) {
        }
        return -1;
    }
}
