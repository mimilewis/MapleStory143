package client.status;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class NewMonsterStatusEffect {

    private int skillid, localDuration;
    private Integer x;
    private long starttime;

    public boolean shouldCancel(long now) {
        return starttime + localDuration * 1000 <= now;
    }

}
