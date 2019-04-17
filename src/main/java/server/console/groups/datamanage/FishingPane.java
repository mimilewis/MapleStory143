package server.console.groups.datamanage;

import com.alee.laf.rootpane.WebFrame;
import server.MapleItemInformationProvider;
import server.life.MapleMonsterInformationProvider;
import server.maps.MapleMapFactory;
import server.reward.RewardDropEntry;

import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Map;
import java.util.Vector;

class FishingPane extends TabbedPane {

    private final Map<Integer, Map<Integer, List<RewardDropEntry>>> fishDrops = MapleMonsterInformationProvider.getInstance().getFishDrop();
    private DefaultTableModel defaultTableModel_ID;

    FishingPane(WebFrame owner) {
        super(owner);
    }

    @Override
    void init() {
        idTableName.add("地图ID");
        idTableName.add("地图名称");

        dataTableName.add("鱼饵");
        dataTableName.add("道具ID");
        dataTableName.add("道具名称");
        dataTableName.add("数量");
        dataTableName.add("爆率(百分比)");
        dataTableName.add("消息类型");
        dataTableName.add("限时");
        dataTableName.add("潜能状态");


        Vector<Vector<String>> datas = new Vector<>();
        for (Integer integer : fishDrops.keySet()) {
            Vector<String> data = new Vector<>();
            data.add(String.valueOf(integer));
            data.add(getIDName(String.valueOf(integer)));
            datas.add(data);
        }
        defaultTableModel_ID = new DefaultTableModel(datas, idTableName);
    }

    @Override
    String getTitle() {
        return "钓鱼";
    }

    @Override
    protected DefaultTableModel getIDTableModel() {
        return defaultTableModel_ID;
    }

    @Override
    protected DefaultTableModel getDataTableModel() {
        Vector<Vector<String>> datas = new Vector<>();
        if (idTable.getSelectedRow() > -1) {
            String id = (String) idTable.getValueAt(idTable.getSelectedRow(), 0);
            if (fishDrops.containsKey(Integer.valueOf(id))) {
                for (Map.Entry<Integer, List<RewardDropEntry>> listEntry : fishDrops.get(Integer.valueOf(id)).entrySet()) {
                    for (RewardDropEntry rewardDropEntry : listEntry.getValue()) {
                        Vector<String> data = new Vector<>();
                        data.add(listEntry.getKey().toString());
                        data.add(String.valueOf(rewardDropEntry.itemId));
                        data.add(getDataName(String.valueOf(rewardDropEntry.itemId)));
                        data.add(String.valueOf(rewardDropEntry.quantity));
                        data.add(String.valueOf((double) rewardDropEntry.chance / 10) + "%"); // String.valueOf((double) entry.chance / 10000) + "%"
                        data.add(String.valueOf(rewardDropEntry.msgType));
                        data.add(String.valueOf(rewardDropEntry.period));
                        data.add(String.valueOf(rewardDropEntry.state));
                        datas.add(data);
                    }
                }
            }

        }
        return new DefaultTableModel(datas, dataTableName);
    }

    @Override
    protected String getIDName(String id) {
        return MapleMapFactory.getMapName(Integer.valueOf(id));
    }

    @Override
    protected String getDataName(String id) {
        return id.equals("0") ? "金币或经验" : MapleItemInformationProvider.getInstance().getName(Integer.valueOf(id));
    }

    @Override
    protected Vector<String> getDefaultDataVector() {
        Vector<String> vector = new Vector<>();
        vector.add("2300000");
        vector.add("0");
        vector.add("");
        vector.add("1");
        vector.add("0%");
        vector.add("0");
        vector.add("0");
        vector.add("0");
        return vector;
    }

    @Override
    protected boolean hasNameDataTable(int i) {
        return i == 1;
    }
}
