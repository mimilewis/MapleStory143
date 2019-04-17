package server.commands;

import client.MapleCharacter;
import client.MapleClient;

/**
 * Represents a command given by a user
 *
 * @author Emilyx3
 */
public class CommandObject {

    /**
     * what {@link MapleCharacter#gm} level is required to use this command
     */
    private final int gmLevelReq;
    /**
     * what gets done when this command is used
     */
    private final CommandExecute exe;

    public CommandObject(CommandExecute c, int gmLevel) {
        exe = c;
        gmLevelReq = gmLevel;
    }

    /**
     * Call this to apply this command to the specified {@link MapleClient} with
     * the specified arguments.
     *
     * @param c        the MapleClient to apply this to
     * @param splitted the arguments
     * @return See {@link CommandExecute#execute}
     */
    public int execute(MapleClient c, String[] splitted) {
        return exe.execute(c, splitted);
    }

    public CommandType getType() {
        return exe.getType();
    }

    /**
     * Returns the GMLevel needed to use this command.
     *
     * @return the required GM Level
     */
    public int getReqGMLevel() {
        return gmLevelReq;
    }
}
