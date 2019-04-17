package server.console.groups.setting;

import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;
import com.alee.laf.button.WebButton;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.table.WebTable;
import com.alee.laf.text.WebTextField;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import configs.Config;
import configs.ServerConfig;
import handling.channel.ChannelServer;
import lombok.extern.log4j.Log4j2;
import server.life.MapleLifeFactory;
import server.life.MapleNPC;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.JsonUtil;
import tools.StringUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

@Log4j2
public class NpcConfigGroup extends AbstractConfigGroup {

    private static final Map<Integer, List<Integer>> data = MapleMapFactory.getAllLinkNpc();
    private final NpcTableMode tableModel = new NpcTableMode();
    private final WebTable webTable = new WebTable(tableModel);
    private WebTextField searchText;
    private WebButton searchButton;
    private Map<Integer, List<Integer>> changelist;

    NpcConfigGroup(ConfigPanel owner) {
        super(owner, "NPC相关");

        if (!ServerConfig.WORLD_HIDENPCS.isEmpty()) {
            try {
                changelist = JsonUtil.getMapperInstance().readValue(ServerConfig.WORLD_HIDENPCS, new TypeReference<Map<Integer, List<Integer>>>() {
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            changelist = new HashMap<>();
        }
    }

    @Override
    public Component getPreview() {

        webTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        webTable.setGridColor(Color.LIGHT_GRAY);
        webTable.getColumnModel().getColumn(2).setMaxWidth(50);
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 2) {
                int mapid = Integer.parseInt(tableModel.getValueAt(e.getFirstRow(), 0).toString().split("\\(")[0]);
                int npcid = Integer.parseInt(tableModel.getValueAt(e.getFirstRow(), 1).toString().split("\\(")[0]);
                changeSetting(mapid, npcid, Boolean.valueOf(tableModel.getValueAt(e.getFirstRow(), e.getColumn()).toString()));
            }

        });


        TitleWebPanel titleWebPanel1 = new TitleWebPanel("设置隐藏NPC");

        WebPanel panel = new WebPanel() {
            {
                //添加顶部搜索框
                add(new CenterPanel(new GroupPanel(5,
                        searchText = new WebTextField(30) {
                            {
                                setInputPrompt("输入地图ID或NPCID进行搜索");
                                addActionListener(e -> searchButton.doClick());
                            }
                        },
                        searchButton = new WebButton("搜索") {
                            {
                                addActionListener(e -> {
                                    String text = searchText.getText();
                                    if (text.isEmpty() || !StringUtil.isNumber(text)) {
                                        WebOptionPane.showMessageDialog(null, "输入的内容非法,无法搜索,请检查代码拼写");
                                        return;
                                    }
                                    tableModel.setDataVector(getSearchResult(text));
                                });
                            }
                        }
                )).setMargin(0, 0, 10, 0), BorderLayout.NORTH);
                //添加表格
                add(new WebScrollPane(webTable));
            }

            private Vector<Vector<Object>> getSearchResult(String id) {
                Vector<Vector<Object>> ret = new Vector<>();

                if (data.containsKey(Integer.valueOf(id))) {
                    data.get(Integer.valueOf(id)).forEach(integer -> ret.add(getVectorData(Integer.valueOf(id), integer)));
                } else {
                    data.entrySet().parallelStream()
                            .filter(integerListEntry -> integerListEntry.getValue().contains(Integer.valueOf(id)))
                            .forEach(integerListEntry -> integerListEntry.getValue().parallelStream()
                                    .filter(integer -> integer.equals(Integer.valueOf(id)))
                                    .forEach(integer -> {
                                        ret.add(getVectorData(integerListEntry.getKey(), integer));
                                    }));
                }

                return ret;
            }

            private Vector<Object> getVectorData(int mapid, int npcid) {
                Vector<Object> vector = new Vector<>();
                vector.add(mapid + "(" + MapleMapFactory.getMapName(mapid) + ")");
                vector.add(npcid + "(" + MapleLifeFactory.getNpcName(npcid) + ")");
                vector.add(isHide(mapid, npcid));
                return vector;
            }

            private boolean isHide(int mapid, int npcid) {
                return changelist.containsKey(mapid) && changelist.get(mapid).contains(npcid);
            }
        };

        return new GroupPanel(GroupingType.fillLast, 5, false, titleWebPanel1, panel);
    }

    private void changeSetting(int mapid, int npcid, boolean isSelected) {

        List<Integer> integers = changelist.computeIfAbsent(mapid, integer -> new ArrayList<>());
        if (!isSelected) {
            integers.remove(Integer.valueOf(npcid));
        } else {
            integers.add(npcid);
        }

        if (integers.isEmpty()) {
            changelist.remove(mapid);
        }


        try {
            ServerConfig.WORLD_HIDENPCS = JsonUtil.getMapperInstance().writeValueAsString(changelist);
            Config.setProperty("world.hidenpcs", ServerConfig.WORLD_HIDENPCS);
        } catch (JsonProcessingException e) {
            log.error("写入配置项失败: world.hidenpcs", e);
        }

        if (!isSelected) {
            ChannelServer.getAllInstances().parallelStream().forEach(c -> {
                MapleMap map = c.getMapFactory().getMap(mapid);
                if (map == null) {
                    return;
                }
                MapleNPC npc = map.getNPCById(npcid);
                if (npc == null) {
                    return;
                }
                c.getPlayerStorage().getAllCharacters().parallelStream().forEach(p -> npc.sendDestroyData(p.getClient()));
                c.getPlayerStorage().getAllCharacters().parallelStream().forEach(p -> npc.sendSpawnData(p.getClient()));

            });
        } else {

            ChannelServer.getAllInstances().parallelStream().forEach(c -> {

                MapleMap map = c.getMapFactory().getMap(mapid);
                if (map == null) {
                    return;
                }
                MapleNPC npc = map.getNPCById(npcid);
                if (npc == null) {
                    return;
                }
                c.getMapFactory().getMap(mapid).hideNpc(npcid);
            });
        }
    }

    public class NpcTableMode extends AbstractTableModel {

        public final Object[] longValues = {"地图ID(名称)", "NPCID(名称)", Boolean.TRUE};
        private final String[] columnNames = {"地图ID(名称)", "NPCID(名称)", "隐藏"};
        private Vector data = new Vector();

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        @Override
        public Object getValueAt(int row, int col) {
            Vector rowVector = (Vector) data.elementAt(row);
            return rowVector.elementAt(col);
        }

        @Override
        public Class getColumnClass(int c) {
            return longValues[c].getClass();
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col >= 2;
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            Vector rowVector = (Vector) data.elementAt(row);
            rowVector.setElementAt(value, col);
            fireTableCellUpdated(row, col);
        }

        public void setDataVector(Vector data) {
            this.data = nonNullVector(data);
            justifyRows(0, getRowCount());
            fireTableDataChanged();
        }

        private Vector nonNullVector(Vector v) {
            return (v != null) ? v : new Vector();
        }

        private void justifyRows(int from, int to) {
            // Sometimes the DefaultTableModel is subclassed
            // instead of the AbstractTableModel by mistake.
            // Set the number of rows for the case when getRowCount
            // is overridden.
            data.setSize(getRowCount());

            for (int i = from; i < to; i++) {
                if (data.elementAt(i) == null) {
                    data.setElementAt(new Vector(), i);
                }
                ((Vector) data.elementAt(i)).setSize(getColumnCount());
            }
        }
    }
}
