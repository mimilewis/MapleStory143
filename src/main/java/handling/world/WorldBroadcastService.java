/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.world;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * @author PlayDK
 */
public class WorldBroadcastService {

    private static final Logger log = LogManager.getLogger(WorldBroadcastService.class.getName());

    private WorldBroadcastService() {
        log.info("正在启动[WorldBroadcastService]");
    }

    public static WorldBroadcastService getInstance() {
        return SingletonHolder.instance;
    }

    public void broadcastSmega(byte[] message) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.broadcastSmega(message);
        }
    }

    public void broadcastGMMessage(byte[] message) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.broadcastGMMessage(message);
        }
    }

    public void broadcastMessage(byte[] message) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.broadcastMessage(message);
        }
    }

    public void sendPacket(List<Integer> targetIds, byte[] packet, int exception) {
        MapleCharacter chr;
        for (int i : targetIds) {
            if (i == exception) {
                continue;
            }
            int ch = WorldFindService.getInstance().findChannel(i);
            if (ch < 0) {
                continue;
            }
            chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(i);
            if (chr != null) {
                chr.send(packet);
            }
        }
    }

    public void sendPacket(int targetId, byte[] packet) {
        int ch = WorldFindService.getInstance().findChannel(targetId);
        if (ch < 0) {
            return;
        }
        final MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetId);
        if (chr != null) {
            chr.send(packet);
        }
    }

    public void sendGuildPacket(int targetIds, byte[] packet, int exception, int guildid, boolean chat) {
        if (targetIds == exception && !chat) {
            return;
        }
        int ch = WorldFindService.getInstance().findChannel(targetIds);
        if (ch < 0) {
            return;
        }
        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
        if (chr != null && chr.getGuildId() == guildid) {
            if (chat) {
                if (chr.getChatSession() != null) {
                    chr.getChatSession().writeAndFlush(packet);
                }
            } else {
                chr.send(packet);
            }
        }
    }

    public void sendFamilyPacket(int targetIds, byte[] packet, int exception, int guildid) {
        if (targetIds == exception) {
            return;
        }
        int ch = WorldFindService.getInstance().findChannel(targetIds);
        if (ch < 0) {
            return;
        }
        MapleCharacter chr = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterById(targetIds);
        if (chr != null && chr.getFamilyId() == guildid) {
            chr.send(packet);
        }
    }

    /**
     * 地图漂浮公告
     */
    public void startMapEffect(String msg, int itemId) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.startMapEffect(msg, itemId);
        }
    }

    public void startMapEffect(String msg, int itemId, int time) {
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.startMapEffect(msg, itemId, time);
        }
    }

    private static class SingletonHolder {

        protected static final WorldBroadcastService instance = new WorldBroadcastService();
    }
}
