/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package client;

public class ClientRedirector {

    private final String account;
    private final int world;
    private final int channel;
    private boolean logined;

    public ClientRedirector(String account, int world, int channel, boolean logined) {
        this.account = account;
        this.world = world;
        this.channel = channel;
        this.logined = logined;
    }

    public final String getAccount() {
        return account;
    }

    public final int getWorld() {
        return world;
    }

    public final int getChannel() {
        return channel;
    }

    public final boolean isLogined() {
        return logined;
    }

    public final void setLogined(boolean logined) {
        this.logined = logined;
    }
}
