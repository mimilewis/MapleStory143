/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import client.MapleEnumClass;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class SetGenderHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        byte gender = slea.readByte(); //?
        String username = slea.readMapleAsciiString();
        if (c.getAccountName().equals(username)) {
            c.setGender(gender);
            c.announce(LoginPacket.genderChanged(c));
            c.announce(LoginPacket.getLoginFailed(MapleEnumClass.AuthReply.GAME_PROTOCOL_INFO));
        } else {
            c.getSession().close();
        }
    }
}
