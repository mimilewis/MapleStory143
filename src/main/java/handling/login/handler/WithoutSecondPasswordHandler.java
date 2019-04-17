/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class WithoutSecondPasswordHandler {

    private static boolean loginFailCount(MapleClient c) {
        c.loginAttempt++;
        return c.loginAttempt > 5;
    }

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c, boolean haspic, boolean view) {
        slea.readByte(); //0 = no 2nd pw, 1 = set 2nd pw
        int charId = slea.readInt();
        if (view) {
            c.setChannel(1);
            c.setWorld(slea.readInt());
        }
        String currentpw = c.getSecondPassword();
        if (!c.isLoggedIn() || loginFailCount(c) || (currentpw != null && (!currentpw.equals("") || haspic)) || !c.login_Auth(charId) || ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) { // TODOO: MULTI WORLDS
            c.getSession().close();
            return;
        }
        if (slea.available() != 0) {
            String setpassword = slea.readMapleAsciiString();
            if (setpassword.length() >= 6 && setpassword.length() <= 16) {
                c.setSecondPassword(setpassword);
                c.updateSecondPassword();
            } else {
                c.announce(LoginPacket.secondPwError((byte) 0x14));
                return;
            }
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        String s = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, s.substring(s.indexOf('/') + 1, s.length()), c.getTempIP(), c.getChannel(), c.getMac());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, s);
        c.announce(MaplePacketCreator.getServerIP(c, Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1]), charId));
    }
}
