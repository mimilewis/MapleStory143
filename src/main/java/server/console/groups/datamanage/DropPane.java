package server.console.groups.datamanage;

import com.alee.laf.rootpane.WebFrame;
import com.fasterxml.jackson.core.JsonProcessingException;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.life.MonsterGlobalDropEntry;
import tools.Pair;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.stream.Collectors;

public class DropPane extends TabbedPane {

    private final Map<Integer, java.util.List<MonsterDropEntry>> allDrop = new TreeMap<>();
    private final Map<String, Pair<Boolean, DefaultTableModel>> modelMap = new LinkedHashMap<>();
    private final Map<String, List<Pair<DataMamageMode, Vector<String>>>> changelist = new LinkedHashMap<>();

    DropPane(WebFrame owner) {
        super(owner);
    }

    @Override
    void init() {
        idTableName.add("怪物ID");
        idTableName.add("怪物名称");

        dataTableName.add("物品ID");
        dataTableName.add("物品名称");
        dataTableName.add("最小掉落数量");
        dataTableName.add("最大掉落数量");
        dataTableName.add("关联任务ID");
        dataTableName.add("爆率(百分比)");

        MapleMonsterInformationProvider.getInstance().getAllDrop().entrySet()
                .parallelStream()
                .forEach(i -> allDrop.put(i.getKey(), i.getValue().parallelStream().filter(m -> m.itemId != 0).collect(Collectors.toList())));

        Vector<Vector<String>> alldata = new Vector<>(), normaldata = new Vector<>(), bossdata = new Vector<>(), globaldata = new Vector<>();

        Vector<String> data;
        for (Integer mobid : allDrop.keySet()) {
            data = new Vector<>();
            data.add(mobid.toString());
            data.add(MapleLifeFactory.getMonsterName(mobid));
            if (MapleLifeFactory.isBoss(mobid)) {
                bossdata.add(data);
            } else {
                normaldata.add(data);
            }
            alldata.add(data);
        }

        // 全局爆率
        for (MonsterGlobalDropEntry entry : MapleMonsterInformationProvider.getInstance().getGlobalDrop()) {
            data = new Vector<>();
            data.add(String.valueOf(entry.itemId));
            data.add(MapleItemInformationProvider.getInstance().getName(entry.itemId));
            data.add(String.valueOf(entry.Minimum));
            data.add(String.valueOf(entry.Maximum));
            data.add(String.valueOf(entry.questid));
            data.add((entry.chance == 0 ? "0" : String.valueOf((double) entry.chance / 10000)) + "%");
            globaldata.add(data);
        }

        modelMap.put("全部怪物", new Pair<>(true, new DefaultTableModel(alldata, idTableName)));
        modelMap.put("普通怪物", new Pair<>(true, new DefaultTableModel(normaldata, idTableName)));
        modelMap.put("BOSS", new Pair<>(true, new DefaultTableModel(bossdata, idTableName)));
        modelMap.put("全局爆率", new Pair<>(false, new DefaultTableModel(globaldata, dataTableName)));
    }

    @Override
    String getTitle() {
        return "怪物爆率";
    }

    @Override
    protected DefaultTableModel getIDTableModel() {
        return modelMap.get("全部怪物").getRight();
    }

    @Override
    protected Map<String, Pair<Boolean, DefaultTableModel>> getMultiIDTableModel() {
        return modelMap;
    }

    @Override
    protected DefaultTableModel getDataTableModel() {
        Vector<Vector<String>> datas = new Vector<>();
        if (idTable.getSelectedRow() > -1) {
            String id = (String) idTable.getValueAt(idTable.getSelectedRow(), 0);
            if (allDrop.containsKey(Integer.valueOf(id))) {
                for (MonsterDropEntry entry : allDrop.get(Integer.valueOf(id))) {
                    Vector<String> data = new Vector<>();
                    data.add(String.valueOf(entry.itemId));
                    data.add(entry.itemId == 0 ? "金币" : MapleItemInformationProvider.getInstance().getName(entry.itemId));
                    data.add(String.valueOf(entry.minimum));
                    data.add(String.valueOf(entry.maximum));
                    data.add(String.valueOf(entry.questid));
                    data.add((entry.chance == 0 ? "0" : String.valueOf((double) entry.chance / 10000)) + "%");
                    datas.add(data);
                }
            }
        }
        return new DefaultTableModel(datas, dataTableName);
    }

    @Override
    protected Vector<String> getDefaultDataVector() {
        Vector<String> ret = new Vector<>();
        ret.add("");
        ret.add("");
        ret.add("1");
        ret.add("1");
        ret.add("0");
        ret.add("0%");
        return ret;
    }

    @Override
    protected String getIDName(String id) {
        try {
            return MapleLifeFactory.getMonsterName(Integer.valueOf(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected String getDataName(String id) {
        try {
            return MapleItemInformationProvider.getInstance().getName(Integer.valueOf(id));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void addToChangeList(DataMamageMode mode, String id, Vector<String> row) {
        List<Pair<DataMamageMode, Vector<String>>> oldrow;
        if (changelist.containsKey(id)) {
            oldrow = changelist.get(id);
        } else {
            oldrow = new LinkedList<>();
        }
        oldrow.add(new Pair<>(mode, row));
        changelist.put(id, oldrow);
        button_apply.setEnabled(true);
    }

    @Override
    protected void applyChange(boolean change) {
        if (!changelist.isEmpty()) {
            String mobid;
            DataMamageMode mode;
            try {
                for (Map.Entry<String, List<Pair<DataMamageMode, Vector<String>>>> entry : changelist.entrySet()) {
                    mobid = entry.getKey();
                    for (Pair<DataMamageMode, Vector<String>> pair : entry.getValue()) {
                        mode = pair.getLeft();
                        final Vector<String> row = pair.getRight();
                        List<MonsterDropEntry> monsterDropEntries = null;
                        switch (mode) {
                            case ID_编辑:
                            case ID_删除:
                                if (!allDrop.containsKey(Integer.valueOf(mobid))) {
                                    monsterDropEntries = Collections.emptyList();
                                    allDrop.put(Integer.valueOf(mobid), monsterDropEntries);
                                } else {
                                    allDrop.remove(Integer.valueOf(mobid));
                                }
                                break;
                            case DATA_编辑:
                                if (row.size() != dataTableName.size()) {
                                    throw new RuntimeException("Drop row index error");
                                }
                                MonsterDropEntry monsterDropEntry = new MonsterDropEntry(
                                        Integer.valueOf(row.get(0)),
                                        (int) (Double.valueOf(row.get(5).substring(0, row.get(5).length() - 1)) * 10000),
                                        Integer.valueOf(row.get(2)),
                                        Integer.valueOf(row.get(3)),
                                        Integer.valueOf(row.get(4)));
                                if (allDrop.containsKey(Integer.valueOf(mobid))) {
                                    monsterDropEntries = allDrop.get(Integer.valueOf(mobid));
                                    monsterDropEntries.removeIf(monsterDropEntry1 -> monsterDropEntry1.itemId == monsterDropEntry.itemId);
                                    monsterDropEntries.add(monsterDropEntry);
                                } else {
                                    monsterDropEntries = Collections.singletonList(monsterDropEntry);
                                    allDrop.put(Integer.valueOf(mobid), monsterDropEntries);
                                }
                                break;
                            case DATA_删除:
                                if (allDrop.containsKey(Integer.valueOf(mobid))) {
                                    monsterDropEntries = allDrop.get(Integer.valueOf(mobid));
                                    monsterDropEntries.removeIf(monsterDropEntry1 ->
                                            monsterDropEntry1.itemId == Integer.valueOf(row.get(0))
                                                    && monsterDropEntry1.minimum == Integer.valueOf(row.get(2))
                                                    && monsterDropEntry1.maximum == Integer.valueOf(row.get(3))
                                                    && monsterDropEntry1.questid == Integer.valueOf(row.get(4))
                                                    && monsterDropEntry1.chance == Double.valueOf(row.get(5).substring(0, row.get(5).length() - 1)) * 10000);
                                }
                                break;
                        }
                        if (monsterDropEntries == null) {
                            MapleMonsterInformationProvider.getInstance().removeDropData(Integer.parseInt(mobid));
                        } else {
                            MapleMonsterInformationProvider.getInstance().setDropData(mobid, monsterDropEntries);
                        }
                    }

                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } finally {
                changelist.clear();
            }
        }
    }
}
