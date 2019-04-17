package server.console.groups.datamanage;

import client.MapleCharacter;
import com.alee.extended.button.WebSwitch;
import com.alee.extended.layout.TableLayout;
import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.text.WebTextField;
import constants.JobConstants;
import handling.world.WorldFindService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.MapleItemInformationProvider;
import server.console.Start;
import server.maps.MapleMap;
import server.maps.MapleMapFactory;
import tools.DateUtil;
import tools.Pair;
import tools.StringUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.util.*;

public class PlayerPane extends TabbedPane {

    private static final Logger log = LogManager.getLogger();
    private static PlayerPane instance;
    private final Map<Integer, Vector<String>> playerData = new TreeMap<>();
    private final Map<Integer, Integer> ids = new TreeMap<>();

    PlayerPane(WebFrame owner) {
        super(owner);
    }

    public static PlayerPane getInstance(WebFrame owner) {
        if (instance == null) {
            instance = new PlayerPane(owner);
        }
        return instance;
    }

    @Override
    void init() {
        idTableName.add("帐号属性");
        idTableName.add("值");

        dataTableName.add("角色ID");
        dataTableName.add("帐号");
        dataTableName.add("角色名");
        dataTableName.add("等级");
        dataTableName.add("职业");
        dataTableName.add("所在频道");
        dataTableName.add("所在地图");

        allDataTableName.add(new Pair<>(true, "角色ID"));
        allDataTableName.add(new Pair<>(true, "帐号"));
        allDataTableName.add(new Pair<>(true, "角色名"));
        allDataTableName.add(new Pair<>(true, "频道"));
        allDataTableName.add(new Pair<>(true, "地图"));
        allDataTableName.add(new Pair<>(true, "等级"));
        allDataTableName.add(new Pair<>(true, "职业"));
        allDataTableName.add(new Pair<>(true, "金币"));
        allDataTableName.add(new Pair<>(true, "RMB(元宝)"));
        allDataTableName.add(new Pair<>(true, "点券"));
        allDataTableName.add(new Pair<>(true, "抵用券"));
    }

    public synchronized void registerIDs(int playerid, MapleCharacter.PlayerObservable playerObservable) {
        ids.put(playerid, dataTable.getRowCount());
        ((DefaultTableModel) dataTable.getModel()).addRow(new Vector());
        playerObservable.addObserver(new TableDataObserver());
        playerObservable.update();
    }

    public synchronized void removeIDs(int plyaerid, MapleCharacter.PlayerObservable playerObservable) {
        try {
            if (ids.containsKey(plyaerid)) {
                int index = ids.remove(plyaerid);
                ((DefaultTableModel) dataTable.getModel()).removeRow(index);
                playerObservable.deleteObservers();
                for (Map.Entry<Integer, Integer> entry : ids.entrySet()) {
                    if (entry.getValue() > index) {
                        entry.setValue(entry.getValue() - 1);
                    }
                }
            }
        } catch (Exception e) {
            log.error("移除ID失败", e);
        }
    }

    @Override
    String getTitle() {
        return "玩家角色";
    }

    @Override
    protected DefaultTableModel getIDTableModel() {
        try {
            if (dataTable.getSelectedRow() == -1) {
                return super.getIDTableModel();
            }
            int playerid = Integer.parseInt(String.valueOf(dataTable.getValueAt(dataTable.getSelectedRow(), 0)));
            if (!playerData.containsKey(playerid)) {
                return super.getIDTableModel();
            }
            MapleCharacter player = WorldFindService.getInstance().findCharacterById(playerid);
            if (player == null || player.getClient() == null) {
                return super.getIDTableModel();
            }
            Map<String, String> accInfoMap = player.getClient().getAccInfoFromDB();
            Map<String, String> dataMap = new LinkedHashMap<>();
            dataMap.put("帐号ID", String.valueOf(player.getAccountID()));
            dataMap.put("帐号名", player.getClient().getAccountName());
            dataMap.put("密码", accInfoMap.get("password"));
            dataMap.put("安全码", accInfoMap.get("safecode"));
            dataMap.put("交易码", accInfoMap.get("tradecode"));
            dataMap.put("创建时间", accInfoMap.get("createdat"));
            dataMap.put("上次登陆时间", accInfoMap.get("lastlogin"));
            dataMap.put("上次登陆IP", accInfoMap.get("sessionIP"));
            dataMap.put("生日", accInfoMap.get("birthday"));
            dataMap.put("QQ", accInfoMap.get("qq"));
            dataMap.put("邮箱", accInfoMap.get("email"));


            Vector<Vector<String>> datas = new Vector<>();
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                Vector<String> data = new Vector<>();
                data.add(entry.getKey());
                data.add(entry.getValue());
                datas.add(data);
            }
            return new DefaultTableModel(datas, idTableName);
        } catch (Exception e) {
            log.error("", e);
            return new DefaultTableModel(null, idTableName);
        }
    }

    @Override
    protected DefaultTableModel getDataTableModel() {
        try {
            dataTableName.clear();
            Vector<Vector<String>> datas = new Vector<>();

            for (Pair<Boolean, String> pair : allDataTableName) {
                if (pair.getLeft()) {
                    dataTableName.add(pair.getRight());
                }
            }

            int i = 0;
            for (Vector<String> vector : playerData.values()) {
                Vector<String> data = new Vector<>();

                for (String aVector : vector) {
                    if (allDataTableName.get(i++).getLeft()) {
                        data.add(aVector);
                    }
                }
                datas.add(data);
            }

            return new DefaultTableModel(datas, dataTableName);
        } catch (Exception e) {
            log.error("", e);
            return new DefaultTableModel(null, dataTableName);
        }
    }

    @Override
    protected boolean showPopmenu() {
        return true;
    }

    @Override
    protected boolean showOperation() {
        return false;
    }

    @Override
    protected boolean showEditColumn() {
        return true;
    }

    @Override
    protected void initColumnSizes() {
        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            TableColumn model = dataTable.getColumnModel().getColumn(i);
            int width;
            switch (i) {
                case 0:
                    width = 60;
                    break;
                case 3:
                case 5:
                    width = 50;
                    break;
                case 4:
                    width = 200;
                    break;
                default:
                    width = 100;
            }
            model.setPreferredWidth(width);
        }
    }

    @Override
    protected WebPopupMenu getTablePopupMenu(boolean isIDTable) {
        WebPopupMenu popupMenu = new WebPopupMenu();

        if (isIDTable) {

        } else {
            popupMenu.add(new PlayerWebMenuItem(new String[]{"踢下线"}, (player, o) -> player.getClient().disconnect(true, false)));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"封号", "封号理由", "是否封IP&MAC", "是否永久封号"}, (player, o) -> player.ban(o[0].toString(), Boolean.valueOf(o[1].toString()), false, Boolean.valueOf(o[2].toString()))));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"传送", "地图ID"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                MapleMap map = player.getClient().getChannelServer().getMapFactory().getMap(Integer.valueOf((String) o[0]));
                if (map == null) {
                    WebOptionPane.showMessageDialog(instance, "输入的地图不存在");
                    return;
                }
                player.changeMap(map);
                player.dropMessage(1, "您被管理员传送到这里.");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予金币", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                long meso = Long.valueOf((String) o[0]);
                player.gainMeso(meso, true);
                player.dropMessage(1, "您已获得管理员发放的" + meso + "金币");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予点券", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                int cash = Integer.valueOf((String) o[0]);
                player.modifyCSPoints(1, cash, true);
                player.dropMessage(1, "您已获得管理员发放的 " + cash + " 点券");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予抵用券", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                int cash = Integer.valueOf((String) o[0]);
                player.modifyCSPoints(2, cash, true);
                player.dropMessage(1, "您已获得管理员发放的 " + cash + " 抵用券");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予元宝(RMB)", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                int rmb = Integer.valueOf((String) o[0]);
                player.gainRMB(rmb);
                player.dropMessage(1, "您已获得管理员发放的 " + rmb + " 元宝");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予元宝(HyPay)", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                int rmb = Integer.valueOf((String) o[0]);
                player.addHyPay(-rmb);
                player.dropMessage(1, "您已获得管理员发放的 " + rmb + " 元宝");
            }));
            popupMenu.add(new PlayerWebMenuItem(new String[]{"给予道具", "道具ID", "数量"}, (player, o) -> {
                for (Object o1 : o) {
                    if (!StringUtil.isNumber(o1.toString())) {
                        WebOptionPane.showMessageDialog(instance, "输入的不是有效的数字");
                        return;
                    }
                }
                int itemID = Integer.valueOf((String) o[0]);
                short quantity = Short.valueOf((String) o[1]);
                player.gainItem(itemID, quantity, "管理员在控制台赠送 时间:" + DateUtil.getNowTime());
                player.dropMessage(1, "您已获得管理员发放的 \r\n" + MapleItemInformationProvider.getInstance().getName(itemID) + " " + quantity + "个");
            }));
        }
        return popupMenu;
    }

    @FunctionalInterface
    interface PlayerAction {
        void run(MapleCharacter player, Object[] objects);
    }

    class PlayerWebMenuItem extends WebMenuItem {

        private final Map<String, Object> values = new LinkedHashMap<>();
        private final PlayerAction playerAction;

        public PlayerWebMenuItem(String[] text, PlayerAction function) {
            super(text[0]);
            playerAction = function;
            addActionListener(e -> {
                if (dataTable.getSelectedRowCount() <= 0) {
                    WebOptionPane.showMessageDialog(instance, "没有选择玩家,无法操作.", "警告", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (text.length > 1) {
                    for (int i = 1; i < text.length; i++) {
                        String labelName = text[i];
                        if (labelName.contains("数量") || labelName.contains("ID")) {
                            this.values.put(labelName, 0);
                        } else if (labelName.contains("是否")) {
                            this.values.put(labelName, false);
                        } else {
                            this.values.put(labelName, "");
                        }
                    }
                    showInputDialog();
                } else {
                    apply();
                }
            });
        }

        private void showInputDialog() {
            WebDialog dialog = new WebDialog(owner, "设置参数", true);
            dialog.setResizable(false);
            GroupPanel groupPanel = new GroupPanel(5, false);
            double[] doubles = new double[values.size() + 1];
            Arrays.fill(doubles, 0, doubles.length, TableLayout.PREFERRED);
            groupPanel.setLayout(new TableLayout(new double[]{TableLayout.PREFERRED, TableLayout.FILL}, doubles, 5, 5));
            int i = 0;
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                groupPanel.add(new WebLabel(entry.getKey(), WebLabel.TRAILING), "0," + i);

                if (entry.getValue() instanceof Boolean) {
                    final WebSwitch ws2 = new WebSwitch(Boolean.valueOf(entry.getValue().toString()));
                    ws2.setRound(11);
                    ws2.setLeftComponent(createSwitchIcon(Start.loadIcon("ok.png"), 4, 0));
                    ws2.setRightComponent(createSwitchIcon(Start.loadIcon("off.png"), 0, 4));
                    ws2.addActionListener(e -> entry.setValue(ws2.isSelected()));
                    groupPanel.add(new GroupPanel(ws2), "1," + i);
                } else {
                    groupPanel.add(new WebTextField(15) {
                        {
                            getDocument().addDocumentListener(new DocumentListener() {
                                @Override
                                public void insertUpdate(DocumentEvent e) {
                                    values.put(entry.getKey(), getText());
                                }

                                @Override
                                public void removeUpdate(DocumentEvent e) {
                                    values.put(entry.getKey(), getText());
                                }

                                @Override
                                public void changedUpdate(DocumentEvent e) {
                                    values.put(entry.getKey(), getText());
                                }
                            });
                            addActionListener(e -> {
                                apply();
                                dialog.dispose();
                            });
                        }
                    }, "1," + i);
                }

                i++;
            }
            groupPanel.add(new CenterPanel(new GroupPanel(5, new WebButton("确定") {
                {
                    addActionListener(e -> {
                        apply();
                        dialog.dispose();
                    });
                }
            }, new WebButton("取消") {
                {
                    addActionListener(e -> dialog.dispose());
                }
            })), "0," + values.size() + ",1," + values.size());
            groupPanel.setMargin(10, 20, 10, 20);
            dialog.add(new CenterPanel(groupPanel));
            dialog.pack();
            dialog.setLocationRelativeTo(instance);
            dialog.setVisible(true);
        }

        private void apply() {
            List<Integer> selectedIDs = new ArrayList<>();
            for (int i : dataTable.getSelectedRows()) {
                selectedIDs.add(Integer.valueOf(dataTable.getModel().getValueAt(i, 0).toString()));
            }
            selectedIDs.parallelStream().forEach(playerID -> {
                MapleCharacter player = WorldFindService.getInstance().findCharacterById(playerID);
                if (player != null) {
                    playerAction.run(player, values.values().toArray());
                }
            });
        }
    }

    class TableDataObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            try {
                MapleCharacter player = (MapleCharacter) arg;
                int row = ids.get(player.getId());
                Vector<String> alldata = new Vector<>(), viewdata = new Vector<>();

                alldata.add(String.valueOf(player.getId()));
                alldata.add(player.getClient().getAccountName());
                alldata.add(player.getName());
                alldata.add(String.valueOf(player.getClient().getChannel()));
                alldata.add(player.getMapId() + "(" + MapleMapFactory.getMapName(player.getMapId()) + ")");
                alldata.add(String.valueOf(player.getLevel()));
                alldata.add(player.getJob() + "(" + JobConstants.getJobNameById(player.getJob()) + ")");
                alldata.add(String.valueOf(player.getMeso()));
                alldata.add(String.valueOf(player.getRMB()));
                alldata.add(String.valueOf(player.getCSPoints(1)));
                alldata.add(String.valueOf(player.getCSPoints(2)));

                playerData.put(player.getId(), alldata);


                for (int i = 0; i < allDataTableName.size(); i++) {
                    Pair<Boolean, String> pair = allDataTableName.get(i);
                    if (pair.getLeft()) {
                        viewdata.add(alldata.get(i));
                    }
                }

                for (int i = 0; i < viewdata.size(); i++) {
                    dataTable.setValueAt(viewdata.get(i), row, i);
                }
            } catch (Exception e) {
                log.error("更新角色数据表失败", e);
            }

        }
    }
}
