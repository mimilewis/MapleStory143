package client.anticheat;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ReportType {

    Hacking(0, "hack"),
    Botting(1, "bot"),
    Scamming(2, "scam"),
    FakeGM(3, "fake"),
    //Harassment(4, "harass"),
    Advertising(5, "ad");
    public final byte i;
    public final String theId;

    @JsonCreator
    ReportType(@JsonProperty("i") int i, @JsonProperty("theId") String theId) {
        this.i = (byte) i;
        this.theId = theId;
    }

    public static ReportType getById(int z) {
        for (ReportType t : ReportType.values()) {
            if (t.i == z) {
                return t;
            }
        }
        return null;
    }

    public static ReportType getByString(String z) {
        for (ReportType t : ReportType.values()) {
            if (z.contains(t.theId)) {
                return t;
            }
        }
        return null;
    }
}
