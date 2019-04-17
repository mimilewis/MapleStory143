/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.reward;

/**
 * @author admin
 */
public class RewardDropEntry {

    public int itemId, chance, quantity, msgType, period, state;

    public RewardDropEntry() {
    }

    public RewardDropEntry(int itemId, int chance, int quantity, int msgType, int period, int state) {
        this.itemId = itemId;
        this.chance = chance;
        this.quantity = quantity;
        this.msgType = msgType;
        this.period = period;
        this.state = state;
    }
}
