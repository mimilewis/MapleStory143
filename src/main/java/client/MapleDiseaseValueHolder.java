package client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class MapleDiseaseValueHolder implements Serializable {

    private static final long serialVersionUID = 9179541993413738569L;
    public final long startTime;
    public final long length;
    public final MapleDisease disease;
    public int skillid;
    public int level;

    @JsonCreator
    public MapleDiseaseValueHolder(@JsonProperty("disease") MapleDisease disease, @JsonProperty("skillid") int skillid, @JsonProperty("level") int level, @JsonProperty("startTime") long startTime, @JsonProperty("length") long length) {
        this.disease = disease;
        this.skillid = skillid;
        this.level = level;
        this.startTime = startTime;
        this.length = length;
    }
}
