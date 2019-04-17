/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class ShowCharCards {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        int accId = slea.readInt(); //帐号ID
        if (!c.isLoggedIn() || c.getAccID() != accId) {
            c.getSession().close();
            return;
        }
        c.announce(LoginPacket.showCharCards(c.getAccCardSlots()));
    }
}
