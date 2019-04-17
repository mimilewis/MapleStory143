/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.inventory;

/**
 * @author PlayDK
 */
public enum ImpFlag {

    REMOVED(0x1),
    SUMMONED(0x2),
    TYPE(0x4),
    STATE(0x8),
    FULLNESS(0x10),
    CLOSENESS(0x20),
    CLOSENESS_LEFT(0x40),
    MINUTES_LEFT(0x80),
    LEVEL(0x100),
    FULLNESS_2(0x200),
    UPDATE_TIME(0x400),
    CREATE_TIME(0x800),
    AWAKE_TIME(0x1000),
    SLEEP_TIME(0x2000),
    MAX_CLOSENESS(0x4000),
    MAX_DELAY(0x8000),
    MAX_FULLNESS(0x10000),
    MAX_ALIVE(0x20000),
    MAX_MINUTES(0x40000);
    private final int i;

    ImpFlag(int i) {
        this.i = i;
    }

    public int getValue() {
        return i;
    }

    public boolean check(int flag) {
        return (flag & i) == i;
    }
}
