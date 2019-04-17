package server.console.groups.datamanage;

import com.alee.extended.button.WebSwitch;
import com.alee.extended.layout.TableLayout;
import com.alee.extended.painter.TitledBorderPainter;
import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;
import com.alee.extended.panel.WebButtonGroup;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.radiobutton.WebRadioButton;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.table.WebTable;
import com.alee.laf.text.WebTextField;
import com.alee.managers.hotkey.Hotkey;
import com.alee.managers.hotkey.HotkeyManager;
import com.alee.utils.SwingUtils;
import com.alee.utils.swing.UnselectableButtonGroup;
import server.console.Start;
import tools.Pair;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public abstract class TabbedPane extends WebPanel {

    protected final WebFrame owner;
    protected final WebTable idTable = new WebTable();
    protected final WebTable dataTable = new WebTable();
    final Vector<String> idTableName = new Vector<>();
    final Vector<String> dataTableName = new Vector<>();
    final List<Pair<Boolean, String>> allDataTableName = new ArrayList<>();
    protected WebButton button_ok, button_cancel, button_apply;


    TabbedPane(WebFrame owner) {
        this.owner = owner;
    }

    abstract void init();

    abstract String getTitle();

    protected DefaultTableModel getIDTableModel() {
        return new DefaultTableModel(null, idTableName);
    }

    protected Map<String, Pair<Boolean, DefaultTableModel>> getMultiIDTableModel() {
        return null;
    }

    protected DefaultTableModel getDataTableModel() {
        return new DefaultTableModel(null, dataTableName);
    }

    public List<Pair<Boolean, String>> getAllDataTableName() {
        return allDataTableName;
    }

    public Component getPreview() {
        init();
        ((BorderLayout) getLayout()).setHgap(0);
        ((BorderLayout) getLayout()).setVgap(0);
        add(getLeftComponent(), BorderLayout.WEST);
        add(getCenterComponent());
        add(getSouthComponent(), BorderLayout.SOUTH);
        return this;
    }

    /**
     * 创建左侧组件
     *
     * @return
     */
    protected Component getLeftComponent() {
        WebPanel webPanel = new WebPanel(new BorderLayout(5, 5));
        WebScrollPane scrollPane = new WebScrollPane(idTable);
        idTable.setEditable(false);
        idTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        idTable.setGridColor(Color.LIGHT_GRAY);
        if (showPopmenu()) {
            final WebPopupMenu popupMenu = getTablePopupMenu(true);
            idTable.setComponentPopupMenu(popupMenu);
            scrollPane.setComponentPopupMenu(popupMenu);
        }
        idTable.addMouseListener(new TableMouseAdapter(true));
        if (!(this instanceof PlayerPane)) {
            idTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || idTable.getRowCount() == 0 || idTable.getSelectedRow() == -1) {
                    return;
                }
                dataTable.clearSelection();
                dataTable.setModel(getDataTableModel());
            });
        }

        GroupPanel typeGroup = null;
        if (getMultiIDTableModel() != null) {
            Map<String, Pair<Boolean, DefaultTableModel>> modelMap = getMultiIDTableModel();
            for (Pair<Boolean, DefaultTableModel> model : modelMap.values()) {
                if (model.getLeft()) {
                    idTable.setModel(model.getRight());
                    break;
                }
            }
            if (modelMap.size() > 1) {
                typeGroup = new GroupPanel(5, false);
                typeGroup.setMargin(0, 5, 5, 5);
                List<AbstractButton> radioButtons = new ArrayList<>();
                for (final Map.Entry<String, Pair<Boolean, DefaultTableModel>> entry : modelMap.entrySet()) {
                    WebRadioButton radioButton = new WebRadioButton(entry.getKey());
                    radioButton.addActionListener(e -> {
                        if (entry.getValue().getLeft()) {
                            idTable.setModel(entry.getValue().getRight());
                            dataTable.setModel(new DefaultTableModel(null, dataTableName));
                        } else {
                            idTable.setModel(new DefaultTableModel(null, idTableName));
                            dataTable.setModel(entry.getValue().getRight());
                        }
                    });
                    radioButtons.add(radioButton);
                }
                UnselectableButtonGroup unselectableButtonGroup = new UnselectableButtonGroup(radioButtons);
                unselectableButtonGroup.setUnselectable(false);
                radioButtons.get(0).setSelected(true);
                typeGroup.add(radioButtons);
                typeGroup.setPainter(new TitledBorderPainter("类型"));
            }
        } else {
            idTable.setModel(getIDTableModel());
        }
        webPanel.setPreferredWidth(200);
        webPanel.add(typeGroup == null ? getSearchGroup() : new GroupPanel(false, getSearchGroup(), typeGroup), BorderLayout.NORTH);
        webPanel.add(scrollPane);
        webPanel.setMargin(5, 5, 5, 0);
        return webPanel;
    }

    protected Component getSearchGroup() {
        WebTextField searchTextField = new WebTextField() {
            {
                setMargin(0, 4, 0, 4);
                setRound(10);
                setInputPrompt("输入搜索内容");
                setInputPromptFont(getFont().deriveFont(Font.ITALIC));
            }
        };

        WebButton searchButton = new WebButton(Start.loadIcon("search.png")) {
            {
                setRolloverDecoratedOnly(true);
                setDrawFocus(false);
                addActionListener(e -> doSearchAction(searchTextField.getText()));
            }
        };

        searchTextField.addActionListener(e -> searchButton.doClick());

        GroupPanel groupPanel1 = new GroupPanel(GroupingType.fillFirst, searchTextField, searchButton),
                groupPanel2 = new GroupPanel(10, new WebCheckBox("显示搜索结果", isSelectedSearchResult()) {
                    {
                        setEnabled(!isSelectedSearchResult());
                    }
                }, new WebCheckBox("模糊搜索", isSelectedSearchFuzzy()) {
                    {
                        setEnabled(isSelectedSearchFuzzy());
                    }
                }),
                mainGroupPanel = new GroupPanel(false, groupPanel1, groupPanel2);

        mainGroupPanel.setPainter(new TitledBorderPainter("搜索"));
        mainGroupPanel.setMargin(0, 5, 5, 5);
        groupPanel2.setMargin(5, 2, 0, 0);
        return mainGroupPanel;
    }

    protected void doSearchAction(String result) {
        // ignore
    }

    protected Component getCenterComponent() {
        WebPanel webPanel = new WebPanel();

        GroupPanel operations = new GroupPanel();

        if (showOperation()) {
            for (Action action : getActions(false)) {
                operations.add(new WebButton(action) {
                    {
                        setRolloverDecoratedOnly(true);
                        setDrawFocus(false);
                    }
                });
            }
        }

        final WebPanel operationPane = new WebPanel();
        operationPane.add(operations, BorderLayout.WEST);
        if (showEditColumn()) {
            operationPane.add(new WebButton("设置列", Start.loadIcon("table.png")) {
                {
                    setRolloverDecoratedOnly(true);
                    setDrawFocus(false);
                    addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            WebDialog dialog = new WebDialog(owner, "设置显示列名", true);
                            dialog.setResizable(false);
                            GroupPanel groupPanel = new GroupPanel(5, false);
                            double[] doubles = new double[getAllDataTableName().size() + 1];
                            Arrays.fill(doubles, 0, doubles.length, TableLayout.PREFERRED);
                            groupPanel.setLayout(new TableLayout(new double[]{TableLayout.PREFERRED, TableLayout.FILL}, doubles, 5, 5));
                            for (int i = 0; i < getAllDataTableName().size(); i++) {
                                final Pair<Boolean, String> pair = getAllDataTableName().get(i);
                                final WebSwitch ws2 = new WebSwitch(pair.getLeft());
                                ws2.setRound(11);
                                ws2.setLeftComponent(createSwitchIcon(Start.loadIcon("ok.png"), 4, 0));
                                ws2.setRightComponent(createSwitchIcon(Start.loadIcon("off.png"), 0, 4));
                                ws2.addActionListener(new AbstractAction() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        pair.left = ws2.isSelected();
                                        dataTable.setModel(getDataTableModel());
                                        initColumnSizes();
                                    }
                                });
                                groupPanel.add(new WebLabel(pair.getRight(), WebLabel.TRAILING), "0," + i);
                                groupPanel.add(ws2, "1," + i);
                            }
                            groupPanel.setMargin(10, 20, 10, 20);
                            dialog.add(new CenterPanel(groupPanel));
                            dialog.pack();
                            dialog.setLocationRelativeTo(TabbedPane.this);
                            dialog.setVisible(true);
                        }
                    });
                }

            }, BorderLayout.EAST);
        }

        webPanel.add(operationPane, BorderLayout.NORTH);

        dataTable.setModel(getDataTableModel());
        WebScrollPane scrollPane = new WebScrollPane(dataTable);
        initColumnSizes();
        dataTable.setEditable(false);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        dataTable.setGridColor(Color.LIGHT_GRAY);
        dataTable.addMouseListener(new TableMouseAdapter(false));
        if (showPopmenu()) {
            final WebPopupMenu popupMenu = getTablePopupMenu(false);
            dataTable.setComponentPopupMenu(popupMenu);
            scrollPane.setComponentPopupMenu(popupMenu);
        }
        if (this instanceof PlayerPane) {
            dataTable.getSelectionModel().addListSelectionListener(e -> {
                if (e.getValueIsAdjusting() || dataTable.getRowCount() == 0 || dataTable.getSelectedRow() == -1) {
                    return;
                }
                idTable.clearSelection();
                idTable.setModel(getIDTableModel());
            });
        }
        webPanel.add(scrollPane);
        webPanel.setMargin(5);
        return webPanel;
    }

    protected void initColumnSizes() {
    }

    protected Component getSouthComponent() {
        button_ok = new WebButton("确定") {
            {
                addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        applyChange(true);
                        button_apply.setEnabled(false);
                        DataManagePanel.getInstance().dispose();
                    }
                });
            }
        };
        button_cancel = new WebButton("取消") {
            {
                addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        applyChange(false);
                        button_apply.setEnabled(false);
                        DataManagePanel.getInstance().dispose();
                    }
                });
            }
        };
        button_apply = new WebButton("应用") {
            {
                addActionListener(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        applyChange(true);
                        button_apply.setEnabled(false);
                    }
                });
            }
        };

        button_apply.setEnabled(false);
        button_ok.setPreferredWidth(100);
        SwingUtils.equalizeComponentsSize(button_ok, button_cancel, button_apply);
        return new WebPanel(new BorderLayout(5, 5)) {
            {
                add(new WebSeparator(true), BorderLayout.NORTH);
                add(new WebButtonGroup(true, button_ok, button_cancel, button_apply).setMargin(0, 0, 5, 5), BorderLayout.EAST);
            }
        };
    }

    protected WebPopupMenu getTablePopupMenu(boolean isIDTable) {
        WebPopupMenu popupMenu = new WebPopupMenu();
        for (Action action : getActions(isIDTable)) {
            if (isIDTable && action.getValue(Action.NAME).equals("编辑")) {
                continue;
            }
            popupMenu.add(new WebMenuItem(action));
        }
        return popupMenu;
    }

    protected java.util.List<Action> getActions(boolean isIDTable) {
        List<Action> ret = new ArrayList<>();
        ret.add(getDefaultActionListener("新增", Start.loadIcon("add.png"), isIDTable));
        ret.add(getDefaultActionListener("删除", Start.loadIcon("off.png"), isIDTable));
        ret.add(getDefaultActionListener("编辑", Start.loadIcon("edit.png"), isIDTable));
        return ret;
    }

    private Action getDefaultActionListener(final String name, final ImageIcon icon, final boolean isIDTable) {
        return new AbstractAction(name, icon) {
            @Override
            public void actionPerformed(ActionEvent e) {
                WebTable table = isIDTable ? idTable : dataTable;
                switch (e.getActionCommand()) {
                    case "新增":
                        editAction(table, true, isIDTable);
                        break;
                    case "编辑":
                        editAction(table, false, isIDTable);
                        break;
                    case "删除":
                        removeAction(table, isIDTable);
                        break;
                }
            }
        };
    }

    private void editAction(final WebTable table, final boolean add, final boolean isIDTable) {
//        WebOptionPane.showMessageDialog(this, "该功能正在调整中,目前仅支持查询.", "警告", 0);

        if (!isIDTable && idTable.getSelectedRow() == -1) {
            WebOptionPane.showMessageDialog(this, "请先在左侧选择要添加分类名称", "警告", 0);
            return;
        }
        final String addtext = add ? "添加" : "修改";
        final WebDialog dialog = new WebDialog(owner, addtext + "数据", true);
        dialog.setResizable(false);
        GroupPanel mainPanel = new GroupPanel(false);
        double[] doubles = new double[table.getColumnCount() + 1];
        Arrays.fill(doubles, 0, doubles.length, TableLayout.PREFERRED);
        mainPanel.setLayout(new TableLayout(new double[]{TableLayout.PREFERRED, TableLayout.FILL}, doubles, 5, 5));
        final List<WebTextField> textFields = new ArrayList<>();
        for (int i = 0; i < table.getColumnCount(); i++) {
            final String columnName = table.getColumnName(i);
            if (columnName.endsWith("名称")) {
                continue;
            }
            mainPanel.add(new WebLabel(columnName, WebLabel.TRAILING), "0," + i);
            final int finalI = i;
            textFields.add(new WebTextField(add ? (isIDTable ? "" : getDefaultDataVector().get(i)) : String.valueOf(table.getValueAt(table.getSelectedRow(), finalI)), 10) {
                {
                    setInputPrompt(getReplaceText(columnName));
                    addFocusListener(new FocusAdapter() {
                        @Override
                        public void focusGained(FocusEvent e) {
                            selectAll();
                        }
                    });
                }
            });
            mainPanel.add(textFields.get(textFields.size() - 1), "1," + i);
        }
        final DefaultTableModel tableModel = (DefaultTableModel) table.getModel();

        WebButton button_ok = new WebButton(addtext) {
            {
                addActionListener(e -> {

                    String id = null;

                    for (int i = 0; i < textFields.size(); i++) {
                        if (hasName(i)) {
                            String text = textFields.get(i).getText();
                            if (getNameByID(text) == null) {
                                WebOptionPane.showMessageDialog(dialog, "输入的ID不存在，无法" + addtext);
                                return;
                            } else {
                                id = isIDTable ? text : getSelectedId();
                            }
                            break;
                        }
                    }

                    Vector<String> data = new Vector<>();
                    int j = 0;
                    for (int i = 0; i < textFields.size(); i++) {
                        String prompt = textFields.get(i).getInputPrompt();
                        String text = textFields.get(i).getText();
                        if (!text.endsWith(prompt)) {
                            text += prompt;
                        }
                        data.add(text);
                        if (!add) {
                            tableModel.setValueAt(text, table.getSelectedRow(), i + j);
                        }
                        if (hasName(i)) {
                            j++;
                            String name = getNameByID(text);
                            data.add(name);
                            if (!add) {
                                tableModel.setValueAt(name, table.getSelectedRow(), i + j);
                            }
                        }
                    }
                    if (add) {
                        tableModel.addRow(data);
                        table.setSelectedRow(table.getRowCount() - 1);
                    }
                    addToChangeList(isIDTable ? DataMamageMode.ID_编辑 : DataMamageMode.DATA_编辑, id, data);
                    dialog.dispose();
                });
            }

            private boolean hasName(int index) {
                return isIDTable && hasNameIDTable(index) || !isIDTable && hasNameDataTable(index);
            }

            private String getNameByID(String name) {
                return isIDTable ? getIDName(name) : getDataName(name);
            }
        };
        WebButton button_cancel = new WebButton("取消") {
            {
                addActionListener(e -> dialog.dispose());
            }
        };
        mainPanel.add(new CenterPanel(new GroupPanel(10, button_ok, button_cancel)), "0," + String.valueOf(doubles.length - 1) + ",1," + String.valueOf(doubles.length - 1));
        mainPanel.setMargin(10, 20, 10, 20);
        dialog.add(mainPanel);
        HotkeyManager.registerHotkey(dialog, button_ok, Hotkey.ENTER);
        HotkeyManager.registerHotkey(dialog, button_cancel, Hotkey.ESCAPE);
        dialog.pack();
        dialog.setLocationRelativeTo(TabbedPane.this);
        dialog.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void removeAction(WebTable table, boolean isIDTable) {
//        WebOptionPane.showMessageDialog(this, "该功能正在调整中,目前仅支持查询.", "警告", 0);
        int rowcount = table.getSelectedRowCount();
        if (WebOptionPane.showConfirmDialog(this, "确定要删除 " + rowcount + " 条记录吗？", "警告", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            int[] rows = table.getSelectedRows();
            int i = 0;
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            for (int row : rows) {
                Vector<String> vector = (Vector<String>) model.getDataVector().get(row - i);
                addToChangeList(isIDTable ? DataMamageMode.ID_删除 : DataMamageMode.DATA_删除, getSelectedId(), vector);
//                if (mobidmodel.getRowCount() > 0) {
//                    addChangeValue(getSelectedId(), vector, new ChangeEntry(ChangeType.DELETE));
//                } else {
//                    addChangeValue("-1", vector, new ChangeEntry(ChangeType.DELETE));
//                }
                model.removeRow(row - i);
                i++;
            }
        }
    }

    private String getSelectedId() {
        return String.valueOf(idTable.getValueAt(idTable.getSelectedRow(), 0));
    }

    protected void addToChangeList(DataMamageMode mode, String id, Vector<String> row) {
    }

    protected void applyChange(boolean change) {
    }

    protected boolean showOperation() {
        return true;
    }

    protected boolean showPopmenu() {
        return true;
    }

    protected boolean showEditColumn() {
        return false;
    }

    /**
     * 默认勾选"显示搜索结果"
     */
    protected boolean isSelectedSearchResult() {
        return false;
    }

    /**
     * 默认勾选"模糊搜索"
     */
    protected boolean isSelectedSearchFuzzy() {
        return false;
    }

    protected boolean hasNameIDTable(int i) {
        return i == 0;
    }

    protected boolean hasNameDataTable(int i) {
        return i == 0;
    }

    private String getReplaceText(String text) {
        if (text.contains("百分比")) {
            return "%";
        }
        return "";
    }

    protected WebLabel createSwitchIcon(ImageIcon icon, final int left, final int right) {
        final WebLabel rightComponent = new WebLabel(icon, WebLabel.CENTER);
        rightComponent.setMargin(2, left, 2, right);
        return rightComponent;
    }

    protected Vector<String> getDefaultDataVector() {
        return new Vector<>();
    }

    protected String getIDName(String id) {
        return null;
    }

    protected String getDataName(String id) {
        return null;
    }

    private class TableMouseAdapter extends MouseAdapter {

        private final boolean isIDTable;

        public TableMouseAdapter(boolean isIDTable) {
            this.isIDTable = isIDTable;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // 如果是角色管理面板，则直接跳出，不允许编辑
            if (!showPopmenu()) {
                return;
            }

            if (!isIDTable && e.getClickCount() == 2) {
                editAction((WebTable) e.getSource(), false, isIDTable);
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                WebTable table = ((WebTable) e.getComponent());
                int row = table.rowAtPoint(e.getPoint());
                if (!table.isRowSelected(row)) {
                    table.setSelectedRow(row);
                }
            }
        }
    }
}
