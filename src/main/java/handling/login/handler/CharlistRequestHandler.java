/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleCharacter;
import client.MapleClient;
import client.MapleEnumClass;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import tools.data.input.LittleEndianAccessor;
import tools.packet.LoginPacket;

import java.util.List;

/**
 * @author PlayDK
 */
public class CharlistRequestHandler {

    public static void handlePacket(LittleEndianAccessor slea, MapleClient c) {
        if (!c.isLoggedIn()) {
            c.getSession().close();
            return;
        }
        slea.readByte();
        int server = 0; //V.106修改
        boolean useKey = slea.readByte() == 1;
        String key = useKey ? slea.readMapleAsciiString() : "";
        int channel = useKey ? LoginServer.getLoginAuthKey(key, true).getRight() : slea.readByte() + 1;

        if (!World.isChannelAvailable(channel)) { //TODOO: MULTI WORLDS
            c.announce(LoginPacket.getLoginFailed(MapleEnumClass.AuthReply.GAME_CONNECTION_BUSY)); //cannot process so many
            return;
        }

        System.out.println("客户地址: " + c.getSessionIPAddress() + " 连接到世界服务器: " + server + " 频道: " + channel);

        c.setChannel(channel);
        List<MapleCharacter> chars = c.loadCharacters(server);
        if (chars != null && ChannelServer.getInstance(channel) != null) {
            c.setWorld(server);
            c.setChannel(channel);
            //c.announce(LoginPacket.EventCheck());
            c.announce(LoginPacket.getCharList(c.getSecondPassword(), chars, c.getAccCharSlots()));
            //c.announce(LoginPacket.getChannelSelected());
        } else {
            c.getSession().close();
        }
    }
}
