package handling.channel.handler;

import client.MapleClient;
import handling.world.WorldGuildService;
import handling.world.guild.MapleBBSThread;
import tools.data.input.LittleEndianAccessor;
import tools.packet.GuildPacket;

import java.util.List;

public class BBSHandler {

    private static String correctLength(String in, int maxSize) {
        if (in.length() > maxSize) {
            return in.substring(0, maxSize);
        }
        return in;
    }

    public static void BBSOperation(LittleEndianAccessor slea, MapleClient c) {
        if (c.getPlayer().getGuildId() <= 0) {
            return; // expelled while viewing bbs or hax
        }
        int localthreadid = 0;
        byte action = slea.readByte();
        switch (action) {
            case 0: // start a new post
                boolean bEdit = slea.readByte() > 0; //是否编辑
                if (bEdit) {
                    localthreadid = slea.readInt();
                }
                boolean bNotice = slea.readByte() > 0; //是否为公告
                String title = correctLength(slea.readMapleAsciiString(), 25);
                String text = correctLength(slea.readMapleAsciiString(), 600);
                int icon = slea.readInt(); //是否有图标
                if (icon >= 0x64 && icon <= 0x6a) {
                    if (!c.getPlayer().haveItem(5290000 + icon - 0x64, 1, false, true)) {
                        return; // hax, using an nx icon that s/he doesn't have
                    }
                } else if (icon < 0 || icon > 2) {
                    return; // hax, using an invalid icon
                }
                if (!bEdit) {
                    newBBSThread(c, title, text, icon, bNotice);
                } else {
                    editBBSThread(c, title, text, icon, localthreadid);
                }
                break;
            case 1: // 删除发布的信息
                localthreadid = slea.readInt();
                deleteBBSThread(c, localthreadid);
                break;
            case 2: // 刷新BBS信息
                int start = slea.readInt();
                listBBSThreads(c, start * 10);
                break;
            case 3: // 浏览公告或留言
                localthreadid = slea.readInt();
                displayThread(c, localthreadid);
                break;
            case 4: // 发布留言
                localthreadid = slea.readInt();
                text = correctLength(slea.readMapleAsciiString(), 25);
                newBBSReply(c, localthreadid, text);
                break;
            case 5: // 删除留言
                localthreadid = slea.readInt();
                int replyid = slea.readInt();
                deleteBBSReply(c, localthreadid, replyid);
                break;
        }
    }

    private static void listBBSThreads(MapleClient c, int start) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        c.announce(GuildPacket.BBSThreadList(WorldGuildService.getInstance().getBBS(c.getPlayer().getGuildId()), start));
    }

    private static void newBBSReply(MapleClient c, int localthreadid, String text) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        WorldGuildService.getInstance().addBBSReply(c.getPlayer().getGuildId(), localthreadid, text, c.getPlayer().getId());
        displayThread(c, localthreadid);
    }

    private static void editBBSThread(MapleClient c, String title, String text, int icon, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return; // expelled while viewing?
        }
        WorldGuildService.getInstance().editBBSThread(c.getPlayer().getGuildId(), localthreadid, title, text, icon, c.getPlayer().getId(), c.getPlayer().getGuildRank());
        displayThread(c, localthreadid);
    }

    private static void newBBSThread(MapleClient c, String title, String text, int icon, boolean bNotice) {
        if (c.getPlayer().getGuildId() <= 0) {
            return; // expelled while viewing?
        }
        displayThread(c, WorldGuildService.getInstance().addBBSThread(c.getPlayer().getGuildId(), title, text, icon, bNotice, c.getPlayer().getId()));
        listBBSThreads(c, 0);
    }

    private static void deleteBBSThread(MapleClient c, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        WorldGuildService.getInstance().deleteBBSThread(c.getPlayer().getGuildId(), localthreadid, c.getPlayer().getId(), c.getPlayer().getGuildRank());
    }

    private static void deleteBBSReply(MapleClient c, int localthreadid, int replyid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }

        WorldGuildService.getInstance().deleteBBSReply(c.getPlayer().getGuildId(), localthreadid, replyid, c.getPlayer().getId(), c.getPlayer().getGuildRank());
        displayThread(c, localthreadid);
    }

    private static void displayThread(MapleClient c, int localthreadid) {
        if (c.getPlayer().getGuildId() <= 0) {
            return;
        }
        List<MapleBBSThread> bbsList = WorldGuildService.getInstance().getBBS(c.getPlayer().getGuildId());
        if (bbsList != null) {
            for (MapleBBSThread t : bbsList) {
                if (t != null && t.localthreadID == localthreadid) {
                    c.announce(GuildPacket.showThread(t));
                }
            }
        }
    }
}
