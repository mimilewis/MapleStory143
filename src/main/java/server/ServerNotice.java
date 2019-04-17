package server;

import handling.world.WorldBroadcastService;
import tools.MaplePacketCreator;

public class ServerNotice {

    private static final String notice = "本模拟器仅用于技术研究，仅供单机测试，无任何商业行为。";

    public static void start() {
        Timer.EventTimer.getInstance().register(() -> {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverNotice(0x00, notice));
        }, 1000 * 60);


        Timer.EventTimer.getInstance().register(() -> {
            WorldBroadcastService.getInstance().broadcastMessage(MaplePacketCreator.serverMessage(notice));
        }, 1000 * 60);
    }
}
