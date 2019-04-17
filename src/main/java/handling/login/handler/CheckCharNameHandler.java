/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleCharacterUtil;
import client.MapleClient;
import handling.login.LoginInformationProvider;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class CheckCharNameHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        String name = slea.readMapleAsciiString();
        c.announce(LoginPacket.charNameResponse(name, !(MapleCharacterUtil.canCreateChar(name, c.isGm()) && (!LoginInformationProvider.getInstance().isForbiddenName(name) || c.isGm()))));
    }
}
