package server;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StructFamiliar {

    private int id, skillID, effectAfter, mobID, monsterCardID;
    private byte grade;
}
