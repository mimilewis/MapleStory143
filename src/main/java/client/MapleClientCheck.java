package client;

import handling.channel.ChannelServer;
import server.Timer;

public class MapleClientCheck {

    public static void start() {
        Timer.WorldTimer.getInstance().register(() -> {
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                for (MapleCharacter players : cserv.getPlayerStorage().getAllCharacters()) {
                    if (players.getLastCheckProcess() <= 0) {
                        players.iNeedSystemProcess();
                        continue;
                    }
                    if (System.currentTimeMillis() - players.getLastCheckProcess() >= 100 * 1000) { //距上次發送進程包經過 2 分鐘
                        players.iNeedSystemProcess();
                    }
                }
            }
        }, 120 * 1000);
    }
}
