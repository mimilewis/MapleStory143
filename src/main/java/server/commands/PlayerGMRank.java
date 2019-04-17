/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.commands;

/**
 * @author PlayDK
 */
public enum PlayerGMRank {

    NORMAL('@', 0),
    DONATOR('#', 1),
    SUPERDONATOR('$', 2),
    INTERN('%', 3),
    GM('!', 4),
    SUPERGM('!', 5),
    ADMIN('!', 6);
    private final char commandPrefix;
    private final int level;

    PlayerGMRank(char ch, int level) {
        this.commandPrefix = ch;
        this.level = level;
    }

    public char getCommandPrefix() {
        return commandPrefix;
    }

    public int getLevel() {
        return level;
    }
}
