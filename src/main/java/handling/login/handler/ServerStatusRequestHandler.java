/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package handling.login.handler;

import client.MapleClient;
import handling.login.LoginServer;
import tools.packet.LoginPacket;

/**
 * @author PlayDK
 */
public class ServerStatusRequestHandler {

    public static void handlePacket(MapleClient c) {
        /*
         * 0 - 没有消息
         * 1 - 当前世界连接数量较多，这可能会导致登录游戏时有些困难。
         * 2 - 当前世界上的连接已到达最高限制。请选择别的服务器进行游戏或稍后再试。
         */
        int numPlayer = LoginServer.getUsersOn();
        int userLimit = LoginServer.getUserLimit();
        if (numPlayer >= userLimit) {
            c.announce(LoginPacket.getServerStatus(2));
        } else if (numPlayer >= userLimit * .8) {
            c.announce(LoginPacket.getServerStatus(1));
        } else {
            c.announce(LoginPacket.getServerStatus(0));
        }
    }
}
