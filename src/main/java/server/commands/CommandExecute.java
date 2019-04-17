package server.commands;

import client.MapleClient;

/**
 * Interface for the executable part of a {@link CommandObject}.
 *
 * @author Emilyx3
 */
public abstract class CommandExecute {

    /**
     * The method executed when this command is used.
     *
     * @param c        the client executing this command
     * @param splitted the command and any arguments attached
     * @return 1 if you want to log the command, 0 if not. TODO: USE {@link #ReturnValue}
     */
    public abstract int execute(MapleClient c, String[] splitted);
    //1 = Success
    //0 = Something Went Wrong

    public CommandType getType() {
        return CommandType.NORMAL;
    }

    enum ReturnValue {

        DONT_LOG,
        LOG
    }

    public static abstract class TradeExecute extends CommandExecute {

        @Override
        public CommandType getType() {
            return CommandType.TRADE;
        }
    }

    public static abstract class PokemonExecute extends CommandExecute {

        @Override
        public CommandType getType() {
            return CommandType.POKEMON;
        }
    }
}
