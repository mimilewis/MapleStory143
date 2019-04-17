package handling.world;

import client.MapleCoolDownValueHolder;
import client.MapleDiseaseValueHolder;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * 角色的BUFF状态信息
 */
public class PlayerBuffStorage implements Serializable {


    private static final long serialVersionUID = -5428338713000883808L;
    private static final Map<Integer, List<PlayerBuffValueHolder>> buffs = new ConcurrentHashMap<>();
    private static final Map<Integer, List<MapleCoolDownValueHolder>> coolDowns = new ConcurrentHashMap<>();
    private static final Map<Integer, List<MapleDiseaseValueHolder>> diseases = new ConcurrentHashMap<>();

    public static void addBuffsToStorage(int chrid, List<PlayerBuffValueHolder> toStore) {
        buffs.put(chrid, toStore);
    }

    public static void addCooldownsToStorage(int chrid, List<MapleCoolDownValueHolder> toStore) {
        coolDowns.put(chrid, toStore);
    }

    public static void addDiseaseToStorage(int chrid, List<MapleDiseaseValueHolder> toStore) {
        diseases.put(chrid, toStore);
    }

    public static List<PlayerBuffValueHolder> getBuffsFromStorage(int chrid) {
        return buffs.remove(chrid);
    }

    public static List<MapleCoolDownValueHolder> getCooldownsFromStorage(int chrid) {
        return coolDowns.remove(chrid);
    }

    public static List<MapleDiseaseValueHolder> getDiseaseFromStorage(int chrid) {
        return diseases.remove(chrid);
    }
}
