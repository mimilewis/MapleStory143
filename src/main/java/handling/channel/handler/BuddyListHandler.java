package handling.channel.handler;

import client.*;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import com.alibaba.druid.pool.DruidPooledConnection;
import database.DatabaseConnection;
import handling.channel.ChannelServer;
import handling.opcode.BuddyOpcode;
import handling.world.WorldBuddyService;
import handling.world.WorldFindService;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;
import tools.packet.BuddyListPacket;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static client.BuddyList.BuddyOperation.删除好友;
import static client.BuddyList.BuddyOperation.添加好友;

public class BuddyListHandler {

    private static CharacterIdNameBuddyCapacity getCharacterIdAndNameFromDatabase(String name, String group) throws SQLException {
        try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT * FROM characters WHERE name LIKE ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            CharacterIdNameBuddyCapacity ret = null;
            if (rs.next()) {
                if (rs.getInt("gm") < 3) {
                    ret = new CharacterIdNameBuddyCapacity(rs.getInt("id"), rs.getString("name"), group, rs.getInt("buddyCapacity"), true);
                }
            }
            rs.close();
            ps.close();
            return ret;
        }
    }

    public static void BuddyOperation(LittleEndianAccessor slea, MapleClient c) {
        short mode = slea.readByte();
        BuddyOpcode opcode = BuddyOpcode.Companion.getByAction(mode);
        if (opcode == null) {
            System.err.println("未处理好友操作码：" + mode);
            return;
        }
        BuddyList buddylist = c.getPlayer().getBuddylist();
        switch (opcode) {
            case FriendReq_SetFriend: { // 添加好友
                String addName = slea.readMapleAsciiString();
                String groupName = slea.readMapleAsciiString();
//            String note = slea.readMapleAsciiString();
                slea.skip(slea.readShort());
                boolean linkaccount = slea.readByte() == 1;
                if (linkaccount) { // 暂时不开放账号综合好友
                    c.getPlayer().dropMessage(1, "暂时无法添加账号综合好友，\r\n请添加普通好友。");
                    c.announce(MaplePacketCreator.enableActions());
                    return;
                }
//            String namer = linkaccount ? (slea.available() == 2 ? addName : slea.readMapleAsciiString()) : "";

                // 添加前的判断，比如被邀请的玩家是否在线、角色名是否正确等
                BuddylistEntry ble = buddylist.get(addName);
                if (addName.getBytes().length > 13 || groupName.getBytes().length > 18) {
                    return;
                }
                if (addName.endsWith(c.getPlayer().getName())) {
                    c.announce(BuddyOpcode.FriendRes_SetFriend_CantSelf.getPacket());
                } else if (ble != null) {
                    c.announce(BuddyOpcode.FriendRes_SetFriend_AlreadySet.getPacket());
                } else if (buddylist.isFull()) {
                    c.announce(BuddyOpcode.FriendRes_SetFriend_FullMe.getPacket());
                } else {
                    try {
                        // 获取对方信息
                        CharacterIdNameBuddyCapacity charWithId = null;
                        int channel = WorldFindService.getInstance().findChannel(addName);
                        MapleCharacter otherChar;
                        if (channel > 0) {
                            otherChar = ChannelServer.getInstance(channel).getPlayerStorage().getCharacterByName(addName);
                            if (otherChar == null) {
                                charWithId = getCharacterIdAndNameFromDatabase(addName, groupName);
                            } else if (!otherChar.isIntern() || c.getPlayer().isIntern()) {
                                charWithId = new CharacterIdNameBuddyCapacity(otherChar.getId(), otherChar.getName(), groupName, otherChar.getBuddylist().getCapacity(), true);
                            }
                        } else {
                            charWithId = getCharacterIdAndNameFromDatabase(addName, groupName);
                        }
                        if (charWithId != null) {
                            BuddyAddResult buddyAddResult = null;
                            if (channel > 0) {
                                buddyAddResult = WorldBuddyService.getInstance().requestBuddyAdd(addName, c.getChannel(), c.getPlayer().getId(), c.getPlayer().getName(), c.getPlayer().getLevel(), c.getPlayer().getJob());
                            } else {
                                try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                                    PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS buddyCount FROM buddies WHERE characterid = ? AND pending = 0");
                                    ps.setInt(1, charWithId.getId());
                                    ResultSet rs = ps.executeQuery();
                                    if (!rs.next()) {
                                        ps.close();
                                        rs.close();
                                        throw new RuntimeException("Result set expected");
                                    } else {
                                        int count = rs.getInt("buddyCount");
                                        if (count >= charWithId.getBuddyCapacity()) {
                                            buddyAddResult = BuddyAddResult.好友列表已满;
                                        }
                                    }
                                    rs.close();
                                    ps.close();

                                    ps = con.prepareStatement("SELECT pending FROM buddies WHERE characterid = ? AND buddyid = ?");
                                    ps.setInt(1, charWithId.getId());
                                    ps.setInt(2, c.getPlayer().getId());
                                    rs = ps.executeQuery();
                                    if (rs.next()) {
                                        buddyAddResult = BuddyAddResult.已经是好友关系;
                                    }
                                    rs.close();
                                    ps.close();
                                }
                            }
                            if (buddyAddResult == BuddyAddResult.好友列表已满) {
                                c.announce(BuddyOpcode.FriendRes_SetFriend_FullOther.getPacket());
                            } else {
                                int displayChannel = -1;
                                int otherCid = charWithId.getId();
                                if (buddyAddResult == BuddyAddResult.已经是好友关系 && channel > 0) {
                                    displayChannel = channel;
                                    notifyRemoteChannel(c, channel, otherCid, groupName, 添加好友);
                                } else if (buddyAddResult != BuddyAddResult.已经是好友关系) {
                                    try (DruidPooledConnection con = DatabaseConnection.getInstance().getConnection()) {
                                        PreparedStatement ps = con.prepareStatement("INSERT INTO buddies (`characterid`, `buddyid`, `groupname`, `pending`) VALUES (?, ?, ?, 1)");
                                        ps.setInt(1, charWithId.getId());
                                        ps.setInt(2, c.getPlayer().getId());
                                        ps.setString(3, groupName);
                                        ps.executeUpdate();
                                        ps.close();
                                    }
                                }
                                buddylist.put(new BuddylistEntry(charWithId.getName(), otherCid, groupName, displayChannel, true));
                                c.announce(BuddyListPacket.updateBuddylist(buddylist.getBuddies(), BuddyOpcode.FriendRes_SendSingleFriendInfo.getValue()));
                                c.announce(BuddyListPacket.BuddyMess(BuddyOpcode.FriendRes_SetFriend_Done.getValue(), charWithId.getName()));
                            }
                        } else {
                            c.announce(BuddyOpcode.FriendRes_SetFriend_UnknownUser.getPacket());
                        }
                    } catch (SQLException e) {
                        System.err.println("SQL THROW" + e);
                    }
                }
                break;
            }
            case FriendReq_AcceptFriend: { // 接受普通好友邀请
                int otherCid = slea.readInt();
                BuddylistEntry ble = buddylist.get(otherCid);
                if (!buddylist.isFull() && ble != null && !ble.isVisible()) {
                    int channel = WorldFindService.getInstance().findChannel(otherCid);
                    buddylist.put(new BuddylistEntry(ble.getName(), otherCid, "未指定群组", channel, true));
                    c.announce(BuddyListPacket.updateBuddylist(buddylist.getBuddies(), BuddyOpcode.FriendRes_LoadAccountIDOfCharacterFriend_Done.getValue()));
                    notifyRemoteChannel(c, channel, otherCid, "未指定群组", 添加好友);
                } else {
                    c.announce(BuddyOpcode.FriendRes_SetFriend_FullMe.getPacket());
                }
                break;
            }
            case FriendReq_AcceptAccountFriend:  // 接受账号好友邀请
                break;
            case FriendReq_DeleteFriend: { // 删除普通好友
                int otherCid = slea.readInt();
                BuddylistEntry blz = buddylist.get(otherCid);
                if (blz != null && blz.isVisible()) {
                    notifyRemoteChannel(c, WorldFindService.getInstance().findChannel(otherCid), otherCid, blz.getGroup(), 删除好友);
                }
                buddylist.remove(otherCid);
                c.announce(BuddyListPacket.updateBuddylist(buddylist.getBuddies(), BuddyOpcode.FriendRes_LoadAccountIDOfCharacterFriend_Done.getValue()));
                break;
            }
            case FriendReq_DeleteAccountFriend:  // 删除账号好友
//        } else if (mode == 0x06) { //扩充好友数量 在好友目录中添加5人需要消耗5万金币。你要扩充好友目录吗？
//            int capacity = c.getPlayer().getBuddyCapacity();
//            if (capacity >= 100 || c.getPlayer().getMeso() < 50000) {
//                c.getPlayer().dropMessage(1, "金币不足，或已扩充达到上限。包括基本格数在内，好友目录中只能加入100个好友。您当前的好友数量为: " + capacity);
//            } else {
//                int newcapacity = capacity + 5;
//                c.getPlayer().gainMeso(-50000, true, true);
//                c.getPlayer().setBuddyCapacity((byte) newcapacity);
//            }
                break;
            case FriendReq_RefuseFriend:
            case FriendReq_RefuseAccountFriend: { // 账号好友拒绝
                int otherCid = slea.readInt();
                //拒绝信息
                BuddylistEntry ble = buddylist.get(otherCid);
                if (ble == null) {
                    c.announce(BuddyOpcode.FriendRes_SetFriend_Unknown.getPacket());
                    return;
                }
                c.announce(BuddyListPacket.NoBuddy(otherCid, BuddyOpcode.FriendRes_DeleteFriend_Done.getValue(), opcode == BuddyOpcode.FriendReq_RefuseAccountFriend));
                MapleCharacter addChar = ChannelServer.getInstance(ble.getChannel()).getPlayerStorage().getCharacterById(ble.getCharacterId());
                addChar.getClient().announce(BuddyListPacket.BuddyMess(BuddyOpcode.FriendRes_DeleteFriend_Unknown.getValue(), c.getPlayer().getName()));
                buddylist.remove(otherCid);
                break;
            }
            case FriendReq_ConvertAccountFriend: //帐号好友转换
                c.getPlayer().dropMessage(1, "暂时无法添加账号综合好友，\r\n请添加普通好友。");
                c.announce(MaplePacketCreator.enableActions());
                break;
            case FriendReq_ModifyFriend: {//好友备注
                slea.readByte();
                int otherCid = slea.readInt();
//            int type = slea.readInt();//43108841 存在?
//            String Namer = slea.readMapleAsciiString();
//            String note = slea.readMapleAsciiString();
                slea.skip(4);
                slea.skip(slea.readShort());
                slea.skip(slea.readShort());
                BuddylistEntry ble = buddylist.get(otherCid);
                if (ble != null) {
                    c.announce(BuddyListPacket.updateBuddyNamer(ble, BuddyOpcode.FriendRes_NotifyChange_FriendInfo.getValue()));
                }
                break;
            }
        }
    }

    private static void notifyRemoteChannel(MapleClient c, int remoteChannel, int otherCid, String group, BuddyOperation operation) {
        MapleCharacter player = c.getPlayer();
        if (remoteChannel > 0) {
            WorldBuddyService.getInstance().buddyChanged(otherCid, player.getId(), player.getName(), c.getChannel(), operation, group);
        }
    }

    private static final class CharacterIdNameBuddyCapacity extends CharacterNameAndId {

        private final int buddyCapacity;

        public CharacterIdNameBuddyCapacity(int id, String name, String group, int buddyCapacity, boolean visible) {
            super(id, name, group, visible);
            this.buddyCapacity = buddyCapacity;
        }

        public int getBuddyCapacity() {
            return buddyCapacity;
        }
    }
}
