package handling.channel.handler;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import configs.ServerConfig;
import handling.channel.ChannelServer;
import handling.chat.ChatServer;
import handling.world.*;
import handling.world.messenger.MapleMessenger;
import handling.world.messenger.MapleMessengerCharacter;
import handling.world.messenger.MessengerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.commands.*;
import tools.MaplePacketCreator;
import tools.StringUtil;
import tools.data.input.LittleEndianAccessor;
import tools.packet.ChatPacket;
import tools.packet.MessengerPacket;
import tools.packet.WhisperPacket;

/**
 * 所有的聊天消息处理器
 *
 * @author dongjak
 */
public class ChatHandler {

    private static final Logger log = LogManager.getLogger(ChatHandler.class);

    /**
     * 与当前地图中的所有玩家聊天.如果消息以特定前缀开头,则表示运行一个GM命令.
     *
     * @param text 消息文本
     * @param unk
     * @param c    客户端
     * @param chr  角色
     * @see PlayerGMRank
     * @see GMCommand
     * @see AdminCommand
     */
    public static void GeneralChat(String text, byte unk, MapleClient c, MapleCharacter chr) {
        if (text.length() > 0 && chr != null && chr.getMap() != null && !CommandProcessor.processCommand(c, text, chr.getBattle() == null ? CommandType.NORMAL : CommandType.POKEMON)) {
            if (!chr.isIntern() && text.length() >= 80) {
                return;
            }
            log.info("[信息] " + chr.getName() + " : " + text);
            if (chr.getCanTalk() || chr.isStaff()) {
                //Note: This patch is needed to prevent chat packet from being broadcast to people who might be packet sniffing.
                if (chr.isHidden()) {
                    if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                        chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.getChatText(chr.getId(), text, false, (byte) 1), true);
                        if (unk == 0) {
                            chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.serverNotice(2, chr.getName() + " : " + text), true);
                        }
                    } else {
                        chr.getMap().broadcastGMMessage(chr, MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), true);
                    }
                } else {
                    chr.getCheatTracker().checkMsg();
                    if (chr.isIntern() && !chr.isSuperGM() && unk == 0) {
                        chr.getMap().broadcastMessage(MaplePacketCreator.getChatText(chr.getId(), text, false, (byte) 1), c.getPlayer().getTruePosition());
                        if (unk == 0) {
                            chr.getMap().broadcastMessage(MaplePacketCreator.serverNotice(2, chr.getName() + " : " + text), c.getPlayer().getTruePosition());
                        }
                    } else {
                        if (chr.haveItem(2430865)) {
                            int type = 0x13;
                            chr.getMap().broadcastMessage(MaplePacketCreator.spouseMessage(type, chr.getName() + "：" + text), c.getPlayer().getTruePosition());
                            chr.getMap().broadcastMessage(MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), 1), c.getPlayer().getTruePosition());
                        } else {
                            chr.getMap().broadcastMessage(MaplePacketCreator.getChatText(chr.getId(), text, c.getPlayer().isSuperGM(), unk), c.getPlayer().getTruePosition());
                        }
                    }
                }
                if (text.equalsIgnoreCase("我喜欢" + c.getChannelServer().getServerName())) {
                    chr.finishAchievement(11);
                }
                if (chr.getMap().getId() == 910000000 && ServerConfig.CHANNEL_CHALKBOARD) {
                    chr.setMarketChalkboard(chr.getName() + " : " + text);
                }
            } else {
                c.announce(MaplePacketCreator.serverNotice(6, "You have been muted and are therefore unable to talk."));
            }
        }
    }

    public static void Others(LittleEndianAccessor slea, MapleClient c, MapleCharacter chr) {
        int type = slea.readByte();
        byte numRecipients = slea.readByte();
        if (numRecipients <= 0) {
            return;
        }
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        String chattext = slea.readMapleAsciiString();
        if (chr == null || !chr.getCanTalk()) {
            c.announce(MaplePacketCreator.serverNotice(6, "You have been muted and are therefore unable to talk."));
            return;
        }
        log.info("[信息] " + chr.getName() + " : " + chattext);
        if (c.isMonitored()) {
            String chattype = "未知";
            switch (type) {
                case 0:
                    chattype = "好友";
                    break;
                case 1:
                    chattype = "组队";
                    break;
                case 2:
                    chattype = "家族";
                    break;
                case 3:
                    chattype = "联盟";
                    break;
                case 4:
                    chattype = "远征";
                    break;
            }
            WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + MapleCharacterUtil.makeMapleReadable(chr.getName()) + " 在 (" + chattype + ") 中说: " + chattext));
        }
        if (chattext.length() <= 0 || CommandProcessor.processCommand(c, chattext, chr.getBattle() == null ? CommandType.NORMAL : CommandType.POKEMON)) {
            return;
        }
        chr.getCheatTracker().checkMsg();
        switch (type) {
            case 0:
//                WorldBuddyService.getInstance().buddyChat(recipients, chr.getId(), chr.getName(), chattext);
                break;
            case 1:
                if (chr.getParty() == null) {
                    break;
                }
                WorldPartyService.getInstance().partyChat(chr.getParty().getPartyId(), chattext, chr.getName());
                break;
            case 2:
                if (chr.getGuildId() <= 0) {
                    break;
                }
//                WorldGuildService.getInstance().guildChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
            case 3:
                if (chr.getGuildId() <= 0) {
                    break;
                }
                WorldAllianceService.getInstance().allianceChat(chr.getGuildId(), chr.getName(), chr.getId(), chattext);
                break;
            case 4:
                if (chr.getParty() == null || chr.getParty().getExpeditionId() <= 0) {
                    break;
                }
                WorldPartyService.getInstance().expedChat(chr.getParty().getExpeditionId(), chattext, chr.getName());
                break;
        }
    }

    public static void Messenger(LittleEndianAccessor slea, MapleClient c) {
        String input;
        MapleMessenger messenger = c.getPlayer().getMessenger();
        WorldMessengerService messengerService = WorldMessengerService.getInstance();
        int action = slea.readByte();
        switch (action) {
            case 0x00: // 打开
                if (messenger != null) { //如果玩家有聊天招待 就退出这个聊天招待 然后进行下面的操作
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    messengerService.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                int mode = slea.readByte(); //现在多了模式
                int maxMembers = slea.readByte(); //聊天招待的人数
                int messengerId = slea.readInt(); //聊天招待的工作ID
                //System.out.println("聊天招待操作 → 打开 模式: " + mode + " 最大人数: " + maxMembers + " messengerId: " + messengerId);
                if (messengerId == 0) { //创建
                    MapleMessengerCharacter messengerPlayer = new MapleMessengerCharacter(c.getPlayer());
                    MessengerType type = MessengerType.getMessengerType(maxMembers, mode != 0x00);
                    if (type == null) {
                        System.out.println("聊天招待操作 → 打开 模式为空");
                        return;
                    }
                    if (mode == 0x00) { //好友聊天 直接创建
                        c.getPlayer().setMessenger(messengerService.createMessenger(messengerPlayer, type, c.getPlayer().isIntern()));
                    } else if (mode == 0x01) { //随机聊天
                        messenger = c.getPlayer().isIntern() ? messengerService.getRandomHideMessenger(type) : messengerService.getRandomMessenger(type);
                        if (messenger != null) { //如果随机的聊天招待不为空就加入这个聊天招待
                            int position = messenger.getLowestPosition();
                            if (position != -1) {
                                c.getPlayer().setMessenger(messenger);
                                messengerService.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getChannel());
                            }
                        } else { //如果找不到就创建这个随机聊天招待
                            c.getPlayer().setMessenger(messengerService.createMessenger(messengerPlayer, type, c.getPlayer().isIntern()));
                            c.announce(MessengerPacket.joinMessenger(0xFF)); //发送提示 等待其他玩家加入..
                        }
                    }
                } else { // 接受别人的聊天邀请加入其他的聊天
                    messenger = messengerService.getMessenger(messengerId);
                    if (messenger != null) {
                        int position = messenger.getLowestPosition();
                        if (position != -1) {
                            c.getPlayer().setMessenger(messenger);
                            messengerService.joinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()), c.getPlayer().getName(), c.getChannel());
                        }
                    }
                }
                break;
            case 0x02: // 退出
                if (messenger != null) {
                    MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(c.getPlayer());
                    messengerService.leaveMessenger(messenger.getId(), messengerplayer);
                    c.getPlayer().setMessenger(null);
                }
                break;
            case 0x03: // 邀请
                if (messenger != null) {
                    int position = messenger.getLowestPosition();
                    if (position == -1) {
                        System.out.println("聊天招待操作 → 邀请错误 没有空闲的位置");
                        return;
                    }
                    input = slea.readMapleAsciiString();
                    MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(input);
                    if (target != null) { //在当前频道中找到该玩家
                        if (!target.isIntern() || c.getPlayer().isIntern()) {
                            c.announce(MessengerPacket.messengerNote(input, 0x04, 0x01));
                            target.getClient().announce(MessengerPacket.messengerInvite(c.getPlayer().getName(), messenger.getId(), c.getChannel() - 1));
                        } else {
                            c.announce(MessengerPacket.messengerNote(input, 0x04, 0x01));
                        }
                    } else {
                        if (World.isConnected(input)) { //在其他频道中找到该玩家
                            messengerService.messengerInvite(c.getPlayer().getName(), messenger.getId(), input, c.getChannel(), c.getPlayer().isIntern());
                        } else { //找不到这个玩家的信息
                            c.announce(MessengerPacket.messengerNote(input, 0x04, 0x00));
                        }
                    }
                }
                break;
            case 0x05: // 拒绝别人的邀请
                String targeted = slea.readMapleAsciiString();
                MapleCharacter target = c.getChannelServer().getPlayerStorage().getCharacterByName(targeted);
                if (target != null) { // 如果在当前频道能找到邀请这的角色信息
                    if (target.getMessenger() != null) {
                        target.getClient().announce(MessengerPacket.messengerNote(c.getPlayer().getName(), 0x05, 0x00));
                    }
                } else { // 如果在其他频道
                    if (!c.getPlayer().isIntern()) {
                        messengerService.declineChat(targeted, c.getPlayer().getName());
                    }
                }
                break;
            case 0x06: // 聊天
                if (messenger != null) {
                    String chattext = slea.readMapleAsciiString();
                    String position = null;
                    if (slea.available() > 0) {
                        position = slea.readMapleAsciiString();
                    }
                    messengerService.messengerChat(messenger.getId(), chattext, c.getPlayer().getName(), position);
                    if (messenger.isMonitored() && chattext.length() > c.getPlayer().getName().length() + 3) { //name : NOT name0 or name1
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, "[GM消息] " + MapleCharacterUtil.makeMapleReadable(c.getPlayer().getName()) + "(Messenger: " + messenger.getMemberNamesDEBUG() + ") said: " + chattext));
                    }
                }
                break;
            case 0x09: //对别人增加好感度
                if (messenger != null) {
                    String name = slea.readMapleAsciiString();
                    if (!messenger.getType().random) {
                        System.out.println("聊天招待操作 → 对别人增加好感度错误 聊天室的类型不是随机聊天 : " + !messenger.getType().random);
                        return;
                    }
                    MapleCharacter targetPlayer = WorldFindService.getInstance().findCharacterByName(name);
                    if (targetPlayer != null && targetPlayer.getId() != c.getPlayer().getId() && targetPlayer.getMessenger() != null && targetPlayer.getMessenger().getId() == messenger.getId()) {
                        switch (c.getPlayer().canGiveLove(targetPlayer)) {
                            case 0x00:
                                if (Math.abs(targetPlayer.getLove() + 1) <= 99999) {
                                    targetPlayer.addLove(1);
                                    targetPlayer.getClient().announce(MessengerPacket.updateLove(targetPlayer.getLove()));
                                }
                                c.getPlayer().hasGiveLove(targetPlayer);
                                c.announce(MessengerPacket.giveLoveResponse(0x00, c.getPlayer().getName(), targetPlayer.getName()));
                                targetPlayer.getClient().announce(MessengerPacket.giveLoveResponse(0x00, c.getPlayer().getName(), targetPlayer.getName()));
                                break;
                            case 0x01:
                                c.announce(MessengerPacket.giveLoveResponse(0x01, c.getPlayer().getName(), targetPlayer.getName()));
                                break;
                            case 0x02:
                                c.announce(MessengerPacket.giveLoveResponse(0x02, c.getPlayer().getName(), targetPlayer.getName()));
                                break;
                        }
                    }
                }
                //System.out.println("聊天招待操作 → 对别人增加好感度.");
                break;
            case 0x0B: //查看对方的信息 也就是别人的好感度多少什么的
                if (messenger != null) {
                    String name = slea.readMapleAsciiString();
                    MapleCharacter player = WorldFindService.getInstance().findCharacterByName(name);
                    if (player != null) {
                        if (player.getMessenger() != null && player.getMessenger().getId() == messenger.getId()) {
                            c.announce(MessengerPacket.messengerPlayerInfo(player));
                        }
                    } else {
                        c.announce(MessengerPacket.messengerNote(name, 0x04, 0x00));
                    }
                }
                //System.out.println("聊天招待操作 → 查看对方的信息.");
                break;
            case 0x0E: //在聊天招待中对别人说悄悄话
                if (messenger != null) {
                    String namefrom = slea.readMapleAsciiString(); //我的角色名字
                    String chattext = slea.readMapleAsciiString(); //对方的角色名字加我说的话
                    int position = slea.readByte(); //对方在聊天招待中的位置
                    messengerService.messengerWhisper(messenger.getId(), chattext, namefrom, position);
                }
                //System.out.println("聊天招待操作 → 在聊天招待中对别人说悄悄话.");
                break;
            case 0x0F: //机器人说话?也就里面的NPC自己说话
                //System.out.println("聊天招待操作 → 机器人说话?也就里面的NPC自己说话.");
                break;
            default:
                System.out.println("聊天招待操作( 0x" + StringUtil.getLeftPaddedStr(Integer.toHexString(action).toUpperCase(), '0', 2) + " ) 未知.");
                break;
        }
    }

    public static void Whisper_Find(LittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        slea.readInt(); //ticks
        switch (mode) {
            case 0x44: //buddy
            case 0x05: { // Find
                String recipient = slea.readMapleAsciiString();
                MapleCharacter player = c.getChannelServer().getPlayerStorage().getCharacterByName(recipient);
                if (player != null) {
                    if (!player.isIntern() || c.getPlayer().isIntern() && player.isIntern()) {
                        c.announce(WhisperPacket.getFindReplyWithMap(player.getName(), player.getMap().getId(), mode == 0x44));
                    } else {
                        c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 0));
                    }
                } else { // Not found
                    int ch = WorldFindService.getInstance().findChannel(recipient);
                    if (ch > 0) {
                        player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(recipient);
                        if (player == null) {
                            break;
                        }
                        if (!player.isIntern() || (c.getPlayer().isIntern() && player.isIntern())) {
                            c.announce(WhisperPacket.getFindReply(recipient, (byte) ch, mode == 0x44));
                        } else {
                            c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 0));
                        }
                        return;
                    }
                    if (ch == -10) {
                        c.announce(WhisperPacket.getFindReplyWithCS(recipient, mode == 0x44));
                    } else if (ch == -20) {
                        c.getPlayer().dropMessage(5, "'" + recipient + "' is at the MTS."); //idfc
                    } else {
                        c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 0));
                    }
                }
                break;
            }
            case 0x06: { // Whisper
                if (c.getPlayer() == null || c.getPlayer().getMap() == null) {
                    return;
                }
                if (!c.getPlayer().getCanTalk()) {
                    c.announce(MaplePacketCreator.serverNotice(6, "You have been muted and are therefore unable to talk."));
                    return;
                }
                c.getPlayer().getCheatTracker().checkMsg();
                String recipient = slea.readMapleAsciiString();
                String text = slea.readMapleAsciiString();
                int ch = WorldFindService.getInstance().findChannel(recipient);
                if (ch > 0) {
                    MapleCharacter player = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(recipient);
                    if (player == null) {
                        break;
                    }
                    player.getClient().announce(WhisperPacket.getWhisper(c.getPlayer().getName(), c.getChannel(), text));
                    if (!c.getPlayer().isIntern() && player.isIntern()) {
                        c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 0));
                    } else {
                        c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 1));
                    }
                    if (c.isMonitored()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + " whispered " + recipient + " : " + text));
                    } else if (player.getClient().isMonitored()) {
                        WorldBroadcastService.getInstance().broadcastGMMessage(MaplePacketCreator.serverNotice(6, c.getPlayer().getName() + " whispered " + recipient + " : " + text));
                    }
                } else {
                    c.announce(WhisperPacket.getWhisperReply(recipient, (byte) 0));
                }
            }
            break;
        }
    }

    public static void ShowLoveRank(LittleEndianAccessor slea, MapleClient c) {
        byte mode = slea.readByte();
        switch (mode) {
            case 0x07: //显示冒险岛明星
                c.announce(MessengerPacket.showLoveRank(0x07));
                break;
            case 0x08: //显示每周的冒险岛明星
                c.announce(MessengerPacket.showLoveRank(0x08));
                break;
        }
    }

    public static void EnterChatServer(LittleEndianAccessor slea, MapleClient c) {
        // 01 00 00 00 01 00 00 00 D5 97 7D 2E 4D 01 00 00 01 00 00 00 08 00 BD C5 B1 BE B2 DF BB AE 00 00 00 00 79 00 00 00 DE 00 00 00
        if (ChatServer.isShutdown()) {
            return;
        }
//        int accid = slea.readInt();
        slea.skip(17);
        int chrid = slea.readInt();
//        String chrname = slea.readMapleAsciiString();
        slea.skip(slea.readShort());

//        PlayerStorage client = ChatServer.getPlayerStorage();
        MapleCharacter chr = WorldFindService.getInstance().findCharacterById(chrid);
        if (chr == null) {
            return;
        }
        chr.setChatSession(c.getSession());
        c.announce(ChatPacket.getChatLoginAUTH());
        c.announce(ChatPacket.getChatLoginResult(chr.getClient()));
    }

    /**
     * 处理好友聊天
     *
     * @param slea
     * @param c
     */
    public static void BuddyChat(LittleEndianAccessor slea, MapleClient c) {
        int playerid = slea.readInt();
        String chattext = slea.readMapleAsciiString();
        int numRecipients = slea.readInt();
        int recipients[] = new int[numRecipients];

        for (byte i = 0; i < numRecipients; i++) {
            recipients[i] = slea.readInt();
        }
        WorldBuddyService.getInstance().buddyChat(recipients, playerid, chattext);
    }

    /**
     * 处理家族聊天
     *
     * @param slea
     * @param c
     */
    public static void GuildChat(LittleEndianAccessor slea, MapleClient c) {
        int playerid = slea.readInt();
        int guildid = slea.readInt();
        String chattext = slea.readMapleAsciiString();

        MapleCharacter chr = WorldFindService.getInstance().findCharacterById(playerid);
        if (chr == null) {
            return;
        }
        WorldGuildService.getInstance().guildChat(chr.getAccountID(), guildid, playerid, chattext);
    }
}
