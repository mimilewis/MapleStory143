package handling.world.party;

import handling.world.WorldPartyService;

import java.util.ArrayList;
import java.util.List;

public class MapleExpedition {

    private final List<Integer> parties;
    private final ExpeditionType type;
    private final int id;
    private int leaderId;

    public MapleExpedition(ExpeditionType etype, int leaderId, int id) {
        this.type = etype;
        this.id = id;
        this.leaderId = leaderId;
        this.parties = new ArrayList<>(etype.maxParty);
    }

    public ExpeditionType getType() {
        return type;
    }

    public int getLeader() {
        return leaderId;
    }

    public void setLeader(int newLead) {
        this.leaderId = newLead;
    }

    public List<Integer> getParties() {
        return parties;
    }

    public int getId() {
        return id;
    }

    public int getAllMembers() {
        int ret = 0;
        for (int i = 0; i < parties.size(); i++) {
            MapleParty pp = WorldPartyService.getInstance().getParty(parties.get(i));
            if (pp == null) {
                parties.remove(i);
            } else {
                ret += pp.getMembers().size();
            }
        }
        return ret;
    }

    public int getFreeParty() {
        for (int i = 0; i < parties.size(); i++) {
            MapleParty party = WorldPartyService.getInstance().getParty(parties.get(i));
            if (party == null) {
                parties.remove(i);
            } else if (party.getMembers().size() < 6) {
                return party.getPartyId();
            }
        }
        if (parties.size() < type.maxParty) {
            return 0;
        }
        return -1;
    }

    public int getIndex(int partyId) {
        for (int i = 0; i < parties.size(); i++) {
            if (parties.get(i) == partyId) {
                return i;
            }
        }
        return -1;
    }
}