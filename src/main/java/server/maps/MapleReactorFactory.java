package server.maps;

import provider.MapleData;
import provider.MapleDataProvider;
import provider.MapleDataProviderFactory;
import provider.MapleDataTool;
import tools.Pair;
import tools.StringUtil;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MapleReactorFactory {

    private static final MapleDataProvider data = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("wzpath") + "/Reactor.wz"));
    private static final Map<Integer, MapleReactorStats> reactorStats = new HashMap<>();

    public static MapleReactorStats getReactor(int rid) {
        MapleReactorStats stats = reactorStats.get(rid);
        if (stats == null) {
            int infoId = rid;
            MapleData reactorData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
            MapleData link = reactorData.getChildByPath("info/link");
            if (link != null) {
                infoId = MapleDataTool.getIntConvert("info/link", reactorData);
                stats = reactorStats.get(infoId);
            }
            if (stats == null) {
                stats = new MapleReactorStats();
                reactorData = data.getData(StringUtil.getLeftPaddedStr(Integer.toString(infoId) + ".img", '0', 11));
                if (reactorData == null) {
                    return stats;
                }
                /*
                 * int i = 0; switch(rid) { case 2002011: case 2002012: case
                 * 2002013: case 2618003: case 2618004: case 2618005: case
                 * 2618006: case 2618007: case 2002018: case 9101000: case
                 * 2512001: case 6109016: case 6109017: case 6109018: case
                 * 6109019: case 6109020: i = 1; break; case 2006000: i = 20;
                 * break; } for (int x = 0; x < i; x++) { stats.addState((byte)
                 * x, 0, null, (byte) (x+1), 0); //test
                 * }
                 */
                boolean canTouch = MapleDataTool.getInt("info/activateByTouch", reactorData, 0) > 0;
                boolean areaSet = false;
                boolean foundState = false;
                for (byte i = 0; true; i++) {
                    MapleData reactorD = reactorData.getChildByPath(String.valueOf(i));
                    if (reactorD == null) {
                        break;
                    }
                    MapleData reactorInfoData_ = reactorD.getChildByPath("event");
                    if (reactorInfoData_ != null && reactorInfoData_.getChildByPath("0") != null) {
                        MapleData reactorInfoData = reactorInfoData_.getChildByPath("0");
                        Pair<Integer, Integer> reactItem = null;
                        int type = MapleDataTool.getIntConvert("type", reactorInfoData);
                        if (type == 100) { //reactor waits for item
                            reactItem = new Pair<>(MapleDataTool.getIntConvert("0", reactorInfoData), MapleDataTool.getIntConvert("1", reactorInfoData, 1));
                            if (!areaSet) { //only set area of effect for item-triggered reactors once
                                stats.setTL(MapleDataTool.getPoint("lt", reactorInfoData));
                                stats.setBR(MapleDataTool.getPoint("rb", reactorInfoData));
                                areaSet = true;
                            }
                        }
                        foundState = true;
                        stats.addState(i, type, reactItem, (byte) MapleDataTool.getIntConvert("state", reactorInfoData), MapleDataTool.getIntConvert("timeOut", reactorInfoData_, -1), (byte) (canTouch ? 2 : (MapleDataTool.getIntConvert("2", reactorInfoData, 0) > 0 || reactorInfoData.getChildByPath("clickArea") != null || type == 9 ? 1 : 0)));
                    } else {
                        stats.addState(i, 999, null, (byte) (foundState ? -1 : (i + 1)), 0, (byte) 0);
                    }
                }
                reactorStats.put(infoId, stats);
                if (rid != infoId) {
                    reactorStats.put(rid, stats);
                }
            } else { // stats exist at infoId but not rid; add to map
                reactorStats.put(rid, stats);
            }
        }
        return stats;
    }
}
