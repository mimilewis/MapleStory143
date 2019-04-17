/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

/**
 * 十字猎人系统
 *
 * @author PlayDK
 */
public class StructCrossHunterShop {

    private final int itemId;
    private final int tokenPrice;
    private final int potentialGrade;

    public StructCrossHunterShop(int itemId, int tokenPrice, int potentialGrade) {
        this.itemId = itemId;
        this.tokenPrice = tokenPrice;
        this.potentialGrade = potentialGrade;
    }

    public int getItemId() {
        return itemId;
    }

    public int getTokenPrice() {
        return tokenPrice;
    }

    public int getPotentialGrade() {
        return potentialGrade;
    }
}
