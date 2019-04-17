/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import constants.ServerConstants;
import handling.login.LoginServer;
import tools.Triple;
import tools.packet.LoginPacket;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ServerlistRequestHandler {

    public static void handlePacket(MapleClient c, boolean packet) {
        //c.announce(LoginPacket.getLoginWelcome());
        List<Triple<String, Integer, Boolean>> backgrounds = new LinkedList<>();
        backgrounds.addAll(Arrays.asList(ServerConstants.backgrounds));
        c.announce(LoginPacket.getServerList(0, LoginServer.getLoad()));
        c.announce(LoginPacket.getEndOfServerList());
        c.announce(LoginPacket.enableRecommended(backgrounds));
        //c.announce(LoginPacket.sendRecommended(0, LoginServer.getEventMessage()));
    }
}
