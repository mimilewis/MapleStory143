package handling.login;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Triple;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginInformationProvider {

    private static LoginInformationProvider instance;
    protected final List<String> ForbiddenName = new ArrayList<>(); //禁止取名
    protected final List<String> Curse = new ArrayList<>(); //聊天禁止出现的字符
    protected final List<Integer> makeCharInfoItemIds = new ArrayList<>(); //所有新建角色出现的装备的集合
    //性别, val, 职业类型
    protected final Map<Triple<Integer, Integer, Integer>, List<Integer>> makeCharInfo = new HashMap<>();
    /*
     * 0 = 脸型
     * 1 = 发型
     * 2 = 上衣
     * 3 = 裤/裙
     * 4 = 鞋子
     * 5 = 武器
     * 6 = 盾牌
     */

    protected LoginInformationProvider() {
        String WZpath = System.getProperty("wzpath");
        MapleDataProvider prov = MapleDataProviderFactory.getDataProvider(new File(WZpath + "/Etc.wz"));
        MapleData nameData = prov.getData("ForbiddenName.img"); //禁止玩家取的名字
        for (MapleData data : nameData.getChildren()) {
            ForbiddenName.add(MapleDataTool.getString(data));
        }
        ForbiddenName.add("落叶无痕");
        ForbiddenName.add("HiredMerch");
        nameData = prov.getData("Curse.img"); //聊天禁止出现的字符
        for (MapleData data : nameData.getChildren()) {
            Curse.add(MapleDataTool.getString(data).split(",")[0]);
            ForbiddenName.add(MapleDataTool.getString(data).split(",")[0]);
        }
        MapleData infoData = prov.getData("MakeCharInfo.img"); //新建角色WZ中默认的装备
        //System.out.println("infoData - " + infoData.getName());
        for (MapleData dat : infoData) {
            if (dat.getName().endsWith("Male") || dat.getName().endsWith("Female") || dat.getName().endsWith("Adventurer") || dat.getName().equals("10112_Dummy")) {
                continue;
            }
            int type;
            if (dat.getName().equals("000_1")) {
                type = JobType.getById(1).type;
            } else if (dat.getName().equals("3001_Dummy")) {
                type = JobType.getById(6).type;
            } else {
                type = JobType.getById(Integer.parseInt(dat.getName())).type;
            }
            //System.out.println("dat - " + dat.getName());
            for (MapleData d : dat) {
                int gender;
                if (d.getName().equals("male") || d.getName().startsWith("male")) {
                    gender = 0;
                } else if (d.getName().equals("female") || d.getName().startsWith("female")) {
                    gender = 1;
                } else {
                    continue;
                }
                for (MapleData da : d) {
                    //System.out.println("da - " + da.getName());
                    Triple<Integer, Integer, Integer> key = new Triple<>(gender, Integer.parseInt(da.getName()), type);
                    List<Integer> our = makeCharInfo.computeIfAbsent(key, k -> new ArrayList<>());
                    for (MapleData dd : da) {
                        if (!dd.getName().equals("name")) {
                            our.add(MapleDataTool.getInt(dd, -1));
                            //System.out.println("dd - " + dd.getName() + " - " + MapleDataTool.getInt(dd, -1) + " our - " + our);
                        }
                    }
                }
            }
        }
        MapleData uA = infoData.getChildByPath("UltimateAdventurer");
        for (MapleData dat : uA) {
            final Triple<Integer, Integer, Integer> key = new Triple<>(-1, Integer.parseInt(dat.getName()), JobType.终极冒险家.type);
            List<Integer> our = makeCharInfo.computeIfAbsent(key, k -> new ArrayList<>());
            for (MapleData d : dat) {
                our.add(MapleDataTool.getInt(d, -1));
                //System.out.println("d - " + d.getName() + " - " + MapleDataTool.getInt(d, -1) + " our - " + our);
            }
        }
        /////////////////////////////////////////////////////////////////////////////////////
        for (MapleData data : infoData) {
            if (data.getName().equalsIgnoreCase("UltimateAdventurer")) {
                continue;
            }
            if (data.getName().endsWith("Male") || data.getName().endsWith("Female")) {
                for (MapleData dat : data) {
                    for (MapleData da : dat) {
                        int itemId = MapleDataTool.getInt(da, -1);
                        if (itemId > 1000000 && !makeCharInfoItemIds.contains(itemId)) {
                            makeCharInfoItemIds.add(itemId);
                        }
                    }
                }
            } else {
                for (MapleData dat : data) {
                    if (dat.getName().startsWith("male") || dat.getName().startsWith("female")) {
                        for (MapleData da : dat) {
                            for (MapleData dd : da) {
                                if (!dd.getName().equals("name")) {
                                    int itemId = MapleDataTool.getInt(dd, -1);
                                    if (itemId > 1000000 && !makeCharInfoItemIds.contains(itemId)) {
                                        makeCharInfoItemIds.add(itemId);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static LoginInformationProvider getInstance() {
        if (instance == null) {
            instance = new LoginInformationProvider();
        }
        return instance;
    }

    /*
     * 是否是禁止取的名字
     */
    public boolean isForbiddenName(String in) {
        for (String name : ForbiddenName) {
            if (in.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /*
     * 是否是禁止聊天出现的字符
     */
    public boolean isCurseMsg(String in) {
        for (String name : Curse) {
            if (in.toLowerCase().contains(name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public boolean isEligibleItem(int gender, int val, int job, int item) {
        if (item < 0) {
            return false;
        }
        Triple<Integer, Integer, Integer> key = new Triple<>(gender, val, job);
        List<Integer> our = makeCharInfo.get(key);
        return our != null && our.contains(item);
    }

    /*
     * 是否合法的未修改WZ新建角色出现的装备
     */
    public boolean isEligibleItem(int itemId) {
        return itemId >= 0 && (itemId == 0 || makeCharInfoItemIds.contains(itemId));
    }
}
