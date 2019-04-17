/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import configs.ServerConfig;
import handling.ServerType;
import tools.data.input.LittleEndianAccessor;

/**
 * @author PlayDK
 */
public class MapLoginHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c, ServerType type) {
        //[14 00] [04] [63 00] 01 00
        byte mapleType = slea.readByte();
        short mapleVersion = slea.readShort();
        String maplePatch = String.valueOf(slea.readShort());
        if (mapleType != ServerConfig.LOGIN_MAPLE_TYPE || mapleVersion != ServerConfig.LOGIN_MAPLE_VERSION || !maplePatch.equals(ServerConfig.LOGIN_MAPLE_PATCH)) {
            c.getSession().close();
        }
    }
}
