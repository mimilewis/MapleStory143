package handling.login.handler;

import client.MapleClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.data.input.LittleEndianAccessor;

public class ClientErrorLogHandler {

    private static final Logger log = LogManager.getLogger(ClientErrorLogHandler.class.getName());

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        log.error((c != null ? c.getPlayer() != null ? "玩家名：" + c.getPlayer().getName() : "账号名：" + c.getAccountName() : "") + slea.readMapleAsciiString());
    }
}
