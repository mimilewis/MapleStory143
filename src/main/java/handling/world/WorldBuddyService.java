/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.BuddyList;
import client.BuddyList.BuddyAddResult;
import client.BuddyList.BuddyOperation;
import client.BuddylistEntry;
import client.MapleCharacter;
import handling.channel.ChannelServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.MaplePacketCreator;
import tools.packet.BuddyListPacket;
import tools.packet.ChatPacket;

/**
 * @author PlayDK
 */
public class WorldBuddyService {

    private static final Logger log = LogManager.getLogger(WorldBuddyService.class.getName());

    private WorldBuddyService() {
        log.info("正在启动[WorldBuddyService]");
    }

    public static WorldBuddyService getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 好友聊天
     *
     * @param recipientCharacterIds 接收这条好友消息的所有角色ID <b>注意:实际上这个参数并未使用</b>
     * @param chrIdFrom             发送这条好友消息的角色ID
     * @param chatText              消息具体内容
     */
    public void buddyChat(int[] recipientCharacterIds, int chrIdFrom, String chatText) {

        //获取发送这条好友消息的角色
        MapleCharacter chrFrom = WorldFindService.getInstance().findCharacterById(chrIdFrom);
        if (chrFrom == null) {
            return;
        }
        if (chrFrom.getChatSession() == null) {
            chrFrom.dropMessage(6, "聊天服务器已断开，请重新登录。");
            return;
        }
        //先在发送这条好友消息的角色的聊天框中展示这条消息
        chrFrom.getChatSession().writeAndFlush(ChatPacket.buddyChat(chrFrom.getAccountID(), chrIdFrom, chatText));

        //然后获取这个角色的好友列表
        int buddyids[] = chrFrom.getBuddylist().getBuddyIds();

        //向这个角色的所有好友发送这条消息
        for (int chrid : buddyids) {
            int ch = WorldFindService.getInstance().findChannel(chrid);
            if (ch > 0) {
                MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(chrid);
                if (chr != null && chr.getBuddylist().containsVisible(chrIdFrom)) {
//                    chr.send(MaplePacketCreator.multiChat(nameFrom, chatText, 0));
                    chr.getChatSession().writeAndFlush(ChatPacket.buddyChat(chrFrom.getAccountID(), chrIdFrom, chatText));
                    if (chr.getClient().isMonitored()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + chrFrom.getName() + " said to " + chr.getName() + " (好友): " + chatText));
                    }
                }
            }
        }
    }

    private void updateBuddies(int characterId, int channel, int[] buddies, boolean offline) {
        for (int buddy : buddies) {
            int ch = WorldFindService.getInstance().findChannel(buddy);
            if (ch > 0) {
                MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(buddy);
                if (chr != null) {
                    BuddylistEntry ble = chr.getBuddylist().get(characterId);
                    if (ble != null && ble.isVisible()) {
                        int mcChannel;
                        if (offline) {
                            ble.setChannel(-1);
                            mcChannel = -1;
                        } else {
                            ble.setChannel(channel);
                            mcChannel = channel - 1;
                        }
                        chr.send(BuddyListPacket.updateBuddyChannel(ble.getCharacterId(), mcChannel, ble.getName()));
                    }
                }
            }
        }
    }

    public void buddyChanged(int chrId, int chrIdFrom, String name, int channel, BuddyOperation operation, String group) {
        int ch = WorldFindService.getInstance().findChannel(chrId);
        if (ch > 0) {
            MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(chrId);
            if (addChar != null) {
                BuddyList buddylist = addChar.getBuddylist();
                switch (operation) {
                    case 添加好友:
                        if (buddylist.contains(chrIdFrom)) {
                            buddylist.put(new BuddylistEntry(name, chrIdFrom, group, channel, true));
                            addChar.getClient().announce(BuddyListPacket.updateBuddylist(buddylist.getBuddies(), 0x17));
                        }
                        break;
                    case 删除好友:
                        if (buddylist.contains(chrIdFrom)) {
                            buddylist.remove(chrIdFrom);
                            addChar.getClient().announce(BuddyListPacket.updateBuddylist(buddylist.getBuddies(), 0x17));
                        }
                        break;
                }
            }
        }
    }

    public BuddyAddResult requestBuddyAdd(String addName, int channelFrom, int chrIdFrom, String nameFrom, int levelFrom, int jobFrom) {
        int ch = WorldFindService.getInstance().findChannel(chrIdFrom);
        if (ch > 0) {
            MapleCharacter addChar = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(addName);
            if (addChar != null) {
                BuddyList buddylist = addChar.getBuddylist();
                if (buddylist.isFull()) {
                    return BuddyAddResult.好友列表已满;
                }
                if (!buddylist.contains(chrIdFrom)) {
                    buddylist.addBuddyRequest(addChar.getClient(), chrIdFrom, nameFrom, channelFrom, levelFrom, jobFrom);
                } else {
                    if (buddylist.containsVisible(chrIdFrom)) {
                        return BuddyAddResult.已经是好友关系;
                    }
                }
            }
        }
        return BuddyAddResult.添加好友成功;
    }

    public void loggedOn(String name, int chrId, int channel, int[] buddies) {
        updateBuddies(chrId, channel, buddies, false);
    }

    public void loggedOff(String name, int chrId, int channel, int[] buddies) {
        updateBuddies(chrId, channel, buddies, true);
    }

    private static class SingletonHolder {

        protected static final WorldBuddyService instance = new WorldBuddyService();
    }
}
