package server.maps;

import client.MapleCharacter;
import client.MapleClient;
import client.skills.Skill;
import client.skills.SkillFactory;
import server.MapleStatEffect;
import tools.MaplePacketCreator;

public class MapleRune extends MapleMapObject {

    private final int type;
    private MapleMap map;

    public MapleRune(int type) {
        this.type = type;
    }

    public MapleMap getMap() {
        return map;
    }

    public void setMap(MapleMap map) {
        this.map = map;
    }

    public int getRuneType() {
        return type;
    }

    public final void applyToPlayer(MapleCharacter player) {
        int n2 = 0;
        boolean bl2 = false;
        switch (getRuneType()) {
            case 0: {
                n2 = 80001427;
                break;
            }
            case 1: {
                n2 = 80001428;
                break;
            }
            case 2: {
                n2 = 80001432;
                bl2 = true;
                break;
            }
            case 3: {
                n2 = 80001752;
                SkillFactory.getSkill(80001762).getEffect(1).applyTo(player);
                bl2 = true;
                break;
            }
            case 4: {
                n2 = 80001753;
                SkillFactory.getSkill(80001757).getEffect(1).applyTo(player);
                bl2 = true;
                break;
            }
            case 5: {
                n2 = 80001754;
                break;
            }
            case 6: {
                n2 = 80001755;
                break;
            }
            case 7: {
                n2 = 80001877;
                break;
            }
            case 8: {
                n2 = 80001878;
                break;
            }
            case 9: {
                bl2 = true;
                SkillFactory.getSkill(80001876).getEffect(1).applyTo(player);
                n2 = 80001879;
            }
        }
        if (bl2) {
            player.send(MaplePacketCreator.showRuneEffect(this.getRuneType()));
        }
        Skill skill = SkillFactory.getSkill(n2);
        MapleStatEffect effect = skill.getEffect(1);
        effect.applyTo(player);
        this.getMap().setRuneTime();
        this.remove();
    }

    @Override
    public void sendSpawnData(MapleClient client) {
        client.announce(MaplePacketCreator.spawnRune(this, false));
    }

    @Override
    public void sendDestroyData(MapleClient client) {
        client.announce(MaplePacketCreator.removeRune(this));
    }

    @Override
    public MapleMapObjectType getType() {
        return MapleMapObjectType.RUNE;
    }

    public void remove() {
        getMap().removeRune(this);
        map = null;
    }
}
