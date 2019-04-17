package client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MapleCoolDownValueHolder {

    public final int skillId;
    public final long startTime;
    public final long length;

    @JsonCreator
    public MapleCoolDownValueHolder(@JsonProperty("skillId") int skillId, @JsonProperty("startTime") long startTime, @JsonProperty("length") long length) {
        super();
        this.skillId = skillId;
        this.startTime = startTime;
        this.length = length;
    }
}
