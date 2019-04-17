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
public class DeleteCharHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        if (!c.isGm()) {
            return;
        }
        String Secondpw_Client = slea.readMapleAsciiString();
        int charId = slea.readInt();
        if (!c.login_Auth(charId) || !c.isLoggedIn()) {
            c.getSession().close();
            return; // Attempting to delete other character
        }
        byte state;
        if (c.getSecondPassword() != null) { // On the server, there's a second password
            if (Secondpw_Client == null) { // Client's hacking
                c.getSession().close();
                return;
            }
        }
        state = (byte) c.deleteCharacter(charId);
        c.announce(LoginPacket.deleteCharResponse(charId, state));
    }
}
