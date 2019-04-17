package client;

import server.MapleInventoryManipulator;
import tools.MaplePacketCreator;
import tools.Randomizer;

public class RockPaperScissors {

    private int round = 0;
    private boolean ableAnswer = true;
    private boolean win = false;

    public RockPaperScissors(MapleClient c, byte mode) {
        c.announce(MaplePacketCreator.getRPSMode((byte) (0x09 + mode), -1, -1, -1));
        if (mode == 0) {
            c.getPlayer().gainMeso(-1000, true, true);
        }
    }

    public boolean answer(MapleClient c, int answer) {
        if (ableAnswer && !win && answer >= 0 && answer <= 2) {
            int response = Randomizer.nextInt(3);
            if (response == answer) {
                c.announce(MaplePacketCreator.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) round));
                //dont do anything. they can still answer once a draw
            } else if ((answer == 0 && response == 2) || (answer == 1 && response == 0) || (answer == 2 && response == 1)) { //they win
                c.announce(MaplePacketCreator.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) (round + 1)));
                ableAnswer = false;
                win = true;
            } else { //they lose
                c.announce(MaplePacketCreator.getRPSMode((byte) 0x0B, -1, (byte) response, (byte) -1));
                ableAnswer = false;
            }
            return true;
        }
        reward(c);
        return false;
    }

    public boolean timeOut(MapleClient c) {
        if (ableAnswer && !win) {
            ableAnswer = false;
            c.announce(MaplePacketCreator.getRPSMode((byte) 0x0A, -1, -1, -1));
            return true;
        }
        reward(c);
        return false;
    }

    public boolean nextRound(MapleClient c) {
        if (win) {
            round++;
            if (round < 10) {
                win = false;
                ableAnswer = true;
                c.announce(MaplePacketCreator.getRPSMode((byte) 0x0C, -1, -1, -1));
                return true;
            }
        }
        reward(c);
        return false;
    }

    public void reward(MapleClient c) {
        if (win) {
            MapleInventoryManipulator.addById(c, 4031332 + round, (short) 1, "", null, 0, "Obtained from rock, paper, scissors");
        } else if (round == 0) {
            c.getPlayer().gainMeso(500, true, true);
        }
        c.getPlayer().setRPS(null);
    }

    public void dispose(MapleClient c) {
        reward(c);
        c.announce(MaplePacketCreator.getRPSMode((byte) 0x0D, -1, -1, -1));
    }
}
