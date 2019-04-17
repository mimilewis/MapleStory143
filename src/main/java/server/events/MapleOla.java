package server.events;

import client.MapleCharacter;
import tools.Randomizer;

public class MapleOla extends MapleSurvival { //survival/ola so similar.

    private int[] stages = new int[3];
    //stg1 = ch00-ch04 = 5 ports
    //stg2 = ch00-ch07 = 8 ports
    //stg3 = ch00-ch15 = 16 ports

    public MapleOla(int channel, MapleEventType type) {
        super(channel, type);
    }

    @Override
    public void finished(MapleCharacter chr) {
        givePrize(chr);
        chr.finishAchievement(21);
    }

    @Override
    public void reset() {
        super.reset();
        stages = new int[]{0, 0, 0};
    }

    @Override
    public void unreset() {
        super.unreset();
        stages = new int[]{Randomizer.nextInt(5), Randomizer.nextInt(8), Randomizer.nextInt(15)};
        if (stages[0] == 2) {
            stages[0] = 3; //hack check; 2nd portal cant be access
        }
    }

    public boolean isCharCorrect(String portalName, int mapid) {
        int st = stages[(mapid % 10) - 1];
        return portalName.equals("ch" + (st < 10 ? "0" : "") + st);
    }
}
