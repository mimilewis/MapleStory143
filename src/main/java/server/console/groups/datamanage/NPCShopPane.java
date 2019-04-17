package server.console.groups.datamanage;

import com.alee.laf.rootpane.WebFrame;
import server.MapleItemInformationProvider;
import server.life.MapleLifeFactory;
import server.shop.MapleShop;
import server.shop.MapleShopFactory;
import server.shop.MapleShopItem;
import tools.Pair;

import javax.swing.table.DefaultTableModel;
import java.util.*;

class NPCShopPane extends TabbedPane {

    final Map<Integer, MapleShop> shops = MapleShopFactory.getInstance().getAllShop();
    private final Map<String, List<Pair<DataMamageMode, Vector<String>>>> changelist = new LinkedHashMap<>();
    private DefaultTableModel defaultTableModel_ID;

    NPCShopPane(WebFrame owner) {
        super(owner);
    }

    @Override
    void init() {
        idTableName.add("商店ID");
        idTableName.add("NPCID");

        dataTableName.add("位置");
        dataTableName.add("道具ID");
        dataTableName.add("道具名称");
        dataTableName.add("游戏币价格");
        dataTableName.add("特殊货币ID");
        dataTableName.add("特殊货币数量");
        dataTableName.add("有效时间");
        dataTableName.add("潜能状态");
        dataTableName.add("分类");
        dataTableName.add("可购买的最小等级");

        Vector<Vector<String>> shopList = new Vector<>();
        for (Map.Entry<Integer, MapleShop> entry : shops.entrySet()) {
            Vector<String> data = new Vector<>();
            data.add(String.valueOf(entry.getKey()));
            data.add(String.valueOf(entry.getValue().getNpcId()));
            shopList.add(data);
        }
        defaultTableModel_ID = new DefaultTableModel(shopList, idTableName);
    }

    @Override
    String getTitle() {
        return "NPC商店";
    }


    @Override
    protected DefaultTableModel getIDTableModel() {
        return defaultTableModel_ID;
    }

    @Override
    protected Vector<String> getDefaultDataVector() {
        Vector<String> ret = new Vector<>();
        ret.add("");
        ret.add("");
        ret.add("");
        ret.add("0");
        ret.add("0");
        ret.add("0");
        ret.add("0");
        ret.add("0");
        ret.add("0");
        ret.add("0");
        return ret;
    }

    @Override
    protected DefaultTableModel getDataTableModel() {
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Vector<Vector<String>> datas = new Vector<>();
        if (idTable.getSelectedRow() > -1) {
            String id = (String) idTable.getValueAt(idTable.getSelectedRow(), 0);
            if (shops.containsKey(Integer.valueOf(id))) {
                for (MapleShopItem mapleShopItem : shops.get(Integer.valueOf(id)).getItems()) {
                    if (mapleShopItem.isRechargeableItem()) {
                        continue;
                    }
                    Vector<String> data = new Vector<>();
                    data.add(String.valueOf(mapleShopItem.getPosition()));
                    data.add(String.valueOf(mapleShopItem.getItemId()));
                    data.add(ii.getName(mapleShopItem.getItemId()));
                    data.add(String.valueOf(mapleShopItem.getPrice()));
                    data.add(String.valueOf(mapleShopItem.getReqItem()));
                    data.add(String.valueOf(mapleShopItem.getReqItemQ()));
                    data.add(String.valueOf(mapleShopItem.getPeriod()));
                    data.add(String.valueOf(mapleShopItem.getState()));
                    data.add(String.valueOf(mapleShopItem.getCategory()));
                    data.add(String.valueOf(mapleShopItem.getMinLevel()));
                    datas.add(data);
                }
            }

        }
        return new DefaultTableModel(datas, dataTableName);
    }

    @Override
    protected String getIDName(String id) {
        try {
            if (shops.containsKey(Integer.valueOf(id))) {
                int npcid = shops.get(Integer.valueOf(id)).getNpcId();
                return npcid + " - " + MapleLifeFactory.getNpcName(npcid);
            } else {
                return null;
            }
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
    protected boolean hasNameIDTable(int i) {
        return false;
    }

    @Override
    protected boolean hasNameDataTable(int i) {
        return i == 1;
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
//        if (!changelist.isEmpty()) {
//            String npcid;
//            DataMamageMode mode;
//            Vector<String> row;
//            try {
//                for (Map.Entry<String, List<Pair<DataMamageMode, Vector<String>>>> entry : changelist.entrySet()) {
//                    npcid = entry.getKey();
//                    for (Pair<DataMamageMode, Vector<String>> pair : entry.getValue()) {
//                        mode = pair.getLeft();
//                        row = pair.getRight();
//                        List<MapleShopItem> shopItems = null;
//                        switch (mode) {
//                            case ID_编辑:
//                            case ID_删除:
//                                if (!shops.containsKey(Integer.valueOf(npcid))) {
//                                    shops.put(Integer.valueOf(npcid), new MapleShop(Integer.valueOf(row.get(0)), Integer.valueOf(row.get(1))));
//                                } else {
//                                    shops.remove(Integer.valueOf(npcid));
//                                }
//                                break;
//                            case DATA_编辑:
//                                if (row.size() != dataTableName.size() - 1) {
//                                    throw new RuntimeException("Drop row index error");
//                                }
//                                MapleShopItem mapleShopItem = new MapleShopItem(
//                                        (short) 1,
//                                        Integer.valueOf(row.get(1)),
//                                        Integer.valueOf(row.get(3)),
//                                        Integer.valueOf(row.get(4)),
//                                        Integer.valueOf(row.get(5)),
//                                        Integer.valueOf(row.get(6)),
//                                        Integer.valueOf(row.get(7)),
//                                        Integer.valueOf(row.get(8)),
//                                        Integer.valueOf(row.get(9)),
//                                        Integer.valueOf(row.get(0)),
//                                        ItemConstants.is飞镖道具(Integer.valueOf(row.get(1))) || ItemConstants.is子弹道具(Integer.valueOf(row.get(1))));
//                                if (shops.containsKey(Integer.valueOf(npcid))) {
//                                    shops.get(Integer.valueOf(npcid)).addItem(mapleShopItem);
//                                } else {
//                                    monsterDropEntries = Collections.singletonList(monsterDropEntry);
//                                    allDrop.put(Integer.valueOf(mobid), monsterDropEntries);
//                                }
//                                break;
//                            case DATA_删除:
//                                if (allDrop.containsKey(Integer.valueOf(mobid))) {
//                                    monsterDropEntries = allDrop.get(Integer.valueOf(mobid));
//                                    Iterator<MonsterDropEntry> iterator = monsterDropEntries.iterator();
//                                    while (iterator.hasNext()) {
//                                        MonsterDropEntry next = iterator.next();
//                                        if (next.itemId == Integer.valueOf(row.get(0)) &&
//                                                next.minimum == Integer.valueOf(row.get(2)) &&
//                                                next.maximum == Integer.valueOf(row.get(3)) &&
//                                                next.questid == Integer.valueOf(row.get(4)) &&
//                                                next.chance == Double.valueOf(row.get(5).substring(0, row.get(5).length() - 1)) * 10000) {
//                                            iterator.remove();
//                                            break;
//                                        }
//                                    }
//                                }
//                                break;
//                        }
//                        if (monsterDropEntries == null) {
//                            MapleMonsterInformationProvider.getInstance().removeDropData(Integer.parseInt(mobid));
//                        } else {
//                            MapleMonsterInformationProvider.getInstance().setDropData(mobid, monsterDropEntries);
//                        }
//                    }
//
//                }
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//            } finally {
//                changelist.clear();
//            }
//        }
    }
}
