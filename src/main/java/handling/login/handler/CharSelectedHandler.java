/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import constants.JobConstants;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import tools.MaplePacketCreator;
import tools.data.input.LittleEndianAccessor;

/**
 * @author PlayDK
 */
public class CharSelectedHandler {

    private static boolean loginFailCount(MapleClient c) {
        c.loginAttempt++;
        return c.loginAttempt > 5;
    }

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        int charId = slea.readInt();
        if (!c.isLoggedIn() || loginFailCount(c) || !c.login_Auth(charId)) {
            c.announce(MaplePacketCreator.enableActions());
            return;
        }
        if (ChannelServer.getInstance(c.getChannel()) == null || c.getWorld() != 0) {
            c.getSession().close();
            return;
        }
        int job = c.getCharacterJob(charId);
        if (job > -1) {
            if (!ServerConstants.isOpenJob(JobConstants.getJobGroupName(job))) {
                c.dropMessage("该职业暂未开放,敬请期待!");
                c.announce(MaplePacketCreator.enableActions());
                return;
            }
        }
        if (c.getIdleTask() != null) {
            c.getIdleTask().cancel(true);
        }
        //c.announce(MaplePacketCreator.getWzCheck(LoginServer.getWzCheckPack()));
        String ip = c.getSessionIPAddress();
        LoginServer.putLoginAuth(charId, ip.substring(ip.indexOf('/') + 1, ip.length()), c.getTempIP(), c.getChannel(), c.getMac());
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, ip);
        c.announce(MaplePacketCreator.getServerIP(c, ChannelServer.getInstance(c.getChannel()).getPort(), charId));
    }
}
