package handling.world.guild;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class MapleBBSThread implements Serializable {

    public static final long serialVersionUID = 3565477792085301248L;
    public final Map<Integer, MapleBBSReply> replies = new HashMap<>();
    public String name, text;
    public long timestamp;
    public int localthreadID, guildID, ownerID, icon;

    public MapleBBSThread(int localthreadID, String name, String text, long timestamp, int guildID, int ownerID, int icon) {
        this.localthreadID = localthreadID;
        this.name = name;
        this.text = text;
        this.timestamp = timestamp;
        this.guildID = guildID;
        this.ownerID = ownerID;
        this.icon = icon;
    }

    public int getReplyCount() {
        return replies.size();
    }

    public boolean isNotice() {
        return localthreadID == 0;
    }

    public void setLocalthreadID(int id) {
        this.localthreadID = id;
    }

    public void setTitle(String name) {
        this.name = name;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setGuildId(int guildID) {
        this.guildID = guildID;
    }

    public void setOwnerID(int ownerID) {
        this.ownerID = ownerID;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public static class ThreadComparator implements Comparator<MapleBBSThread>, Serializable {


        private static final long serialVersionUID = -3135067861031600111L;

        @Override
        public int compare(MapleBBSThread o1, MapleBBSThread o2) {
            if (o1.localthreadID < o2.localthreadID) {
                return 1;
            } else if (o1.localthreadID == o2.localthreadID) {
                return 0;
            } else {
                return -1; //opposite here as oldest is last, newest is first
            }
        }
    }
}
