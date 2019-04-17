/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.messenger.MessengerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.packet.MessengerPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author PlayDK
 */
public class WorldMessengerService {

    private static final Logger log = LogManager.getLogger(WorldMessengerService.class.getName());
    private final Map<Integer, MapleMessenger> messengers;
    private final AtomicInteger runningMessengerId;

    private WorldMessengerService() {
        log.info("正在启动[WorldMessengerService]");
        runningMessengerId = new AtomicInteger(1);
        messengers = new HashMap<>();
    }

    public static WorldMessengerService getInstance() {
        return SingletonHolder.instance;
    }

    public MapleMessenger createMessenger(MapleMessengerCharacter chrfor, MessengerType type, boolean gm) {
        int messengerid = runningMessengerId.getAndIncrement();
        MapleMessenger messenger = new MapleMessenger(messengerid, chrfor, type, gm);
        messengers.put(messenger.getId(), messenger);
        return messenger;
    }

    /*
     * 玩家邀请另外1个玩家
     * 被邀请的玩家拒绝聊天邀请
     */
    public void declineChat(String target, String nameFrom) {
        MapleCharacter player = WorldFindService.getInstance().findCharacterByName(target);
        if (player != null) {
            if (player.getMessenger() != null) {
                player.getClient().announce(MessengerPacket.messengerNote(nameFrom, 0x05, 0x00));
            }
        }
    }

    /*
     * 通过聊天招待的工作 ID  来获取聊天招待的信息
     */
    public MapleMessenger getMessenger(int messengerId) {
        return messengers.get(messengerId);
    }

    /*
     * 通过聊天招待的类型获取是否有空闲的聊天招待信息
     * 聊天室不是GM创建
     */
    public MapleMessenger getRandomMessenger(MessengerType type) {
        for (Entry<Integer, MapleMessenger> ms : messengers.entrySet()) {
            MapleMessenger messenger = ms.getValue();
            if (messenger != null && messenger.getType() == type && messenger.getLowestPosition() != -1 && !messenger.isHide()) {
                return messenger;
            }
        }
        return null;
    }

    /*
     * 通过聊天招待的类型获取是否有空闲的聊天招待信息
     * 聊天室是GM创建
     */
    public MapleMessenger getRandomHideMessenger(MessengerType type) {
        for (Entry<Integer, MapleMessenger> ms : messengers.entrySet()) {
            MapleMessenger messenger = ms.getValue();
            if (messenger != null && messenger.getType() == type && messenger.getLowestPosition() != -1 && messenger.isHide()) {
                return messenger;
            }
        }
        return null;
    }

    /*
     * 玩家离开聊天招待
     */
    public void leaveMessenger(int messengerId, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(target.getName());
        messenger.removeMember(target);
        for (MapleMessengerCharacter mmc : messenger.getMembers()) {
            if (mmc != null) {
                MapleCharacter player = WorldFindService.getInstance().findCharacterById(mmc.getId());
                if (player != null) {
                    player.getClient().announce(MessengerPacket.removeMessengerPlayer(position));
                }
            }
        }
    }

    public void silentLeaveMessenger(int messengerId, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentRemoveMember(target);
    }

    public void silentJoinMessenger(int messengerId, MapleMessengerCharacter target) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.silentAddMember(target);
    }

    /*
     * 更新聊天招待信息
     */
    public void updateMessenger(int messengerId, String nameFrom, int fromChannel) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        int position = messenger.getPositionByName(nameFrom);
        for (MapleMessengerCharacter mmc : messenger.getMembers()) {
            if (mmc != null && !mmc.getName().equals(nameFrom)) {
                MapleCharacter player = WorldFindService.getInstance().findCharacterByName(mmc.getName());
                if (player != null) {
                    MapleCharacter fromplayer = ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(nameFrom);
                    if (fromplayer != null) {
                        player.getClient().announce(MessengerPacket.updateMessengerPlayer(nameFrom, fromplayer, position, fromChannel - 1));
                    }
                }
            }
        }
    }

    /*
     * 玩家加入聊天招待
     */
    public void joinMessenger(int messengerId, MapleMessengerCharacter target, String from, int fromChannel) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        messenger.addMember(target);
        int position = messenger.getPositionByName(target.getName());
        for (MapleMessengerCharacter mmc : messenger.getMembers()) {
            if (mmc != null) {
                int mposition = messenger.getPositionByName(mmc.getName());
                MapleCharacter player = WorldFindService.getInstance().findCharacterByName(mmc.getName());
                if (player != null) {
                    if (!mmc.getName().equals(from)) {
                        MapleCharacter fromplayer = ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(from);
                        if (fromplayer != null) {
                            player.getClient().announce(MessengerPacket.addMessengerPlayer(from, fromplayer, position, fromChannel - 1));
                            fromplayer.getClient().announce(MessengerPacket.addMessengerPlayer(player.getName(), player, mposition, mmc.getChannel() - 1));
                        }
                    } else {
                        player.getClient().announce(MessengerPacket.joinMessenger(mposition));
                    }
                }
            }
        }
    }

    /*
     * 在聊天招待中说话
     */
    public void messengerChat(int messengerId, String chatText, String namefrom, String postxt) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        for (MapleMessengerCharacter mmc : messenger.getMembers()) {
            if (mmc != null && !mmc.getName().equals(namefrom)) {
                MapleCharacter player = WorldFindService.getInstance().findCharacterByName(mmc.getName());
                if (player != null) {
                    player.getClient().announce(MessengerPacket.messengerChat(chatText, postxt));
                }
            }
        }
    }

    /*
     * 在聊天招待中私聊
     */
    public void messengerWhisper(int messengerId, String chatText, String namefrom, int position) {
        MapleMessenger messenger = getMessenger(messengerId);
        if (messenger == null) {
            throw new IllegalArgumentException("No messenger with the specified messengerid exists");
        }
        MapleMessengerCharacter mmc = messenger.getMemberByPos(position);
        if (mmc != null && !mmc.getName().equals(namefrom)) {
            MapleCharacter player = WorldFindService.getInstance().findCharacterByName(mmc.getName());
            if (player != null) {
                player.getClient().announce(MessengerPacket.messengerWhisper(namefrom, chatText));
            }
        }
    }

    /*
     * 聊天招待邀请
     */
    public void messengerInvite(String sender, int messengerId, String target, int fromChannel, boolean gm) {
        MapleCharacter fromplayer = ChannelServer.getInstance(fromChannel).getPlayerStorage().getCharacterByName(sender);
        MapleCharacter targetplayer = WorldFindService.getInstance().findCharacterByName(target);
        if (targetplayer != null && fromplayer != null) {
            if (!targetplayer.isIntern() || gm) {
                targetplayer.getClient().announce(MessengerPacket.messengerInvite(sender, messengerId, fromChannel - 1));
                fromplayer.getClient().announce(MessengerPacket.messengerNote(target, 0x04, 0x01));
            } else {
                fromplayer.getClient().announce(MessengerPacket.messengerNote(target, 0x04, 0x00));
            }
        }
    }

    private static class SingletonHolder {

        protected static final WorldMessengerService instance = new WorldMessengerService();
    }
}
