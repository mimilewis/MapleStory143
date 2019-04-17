package client;

public class MaplePartTimeJob {

    private final int cid;
    private byte job = 0;
    private long time = 0;
    private int reward = 0;

    public MaplePartTimeJob(int cid) {
        this.cid = cid;
    }

    public int getCharacterId() {
        return cid;
    }

    public byte getJob() {
        return job;
    }

    public void setJob(byte job) {
        this.job = job;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getReward() {
        return reward;
    }

    public void setReward(int reward) {
        this.reward = reward;
    }
}
