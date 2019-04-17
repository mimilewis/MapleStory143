package server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*
 * 套装设置
 */
public class StructSetItem {

    //[激活属性的数量] [激活后的套装加成属性]
    public final Map<Integer, StructSetItemStat> setItemStat = new LinkedHashMap<>();
    public final List<Integer> itemIDs = new ArrayList<>();
    public int setItemID; //套装ID
    public byte completeCount; //套装总数
    public String setItemName; //套装名称

    public Map<Integer, StructSetItemStat> getSetItemStats() {
        return new LinkedHashMap<>(setItemStat);
    }
}
