package server.console.groups.setting;

import com.alee.extended.list.CheckBoxCellData;
import com.alee.extended.list.CheckBoxListModel;
import com.alee.extended.list.WebCheckBoxList;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;
import com.alee.laf.button.WebButton;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextField;
import configs.ServerConfig;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;

@Log4j2
public class EventConfigGroup extends AbstractConfigGroup {
    private final CheckBoxListModel listModel = new CheckBoxListModel();

    EventConfigGroup(ConfigPanel owner) {
        super(owner, "事件脚本");
    }

    @Override
    public Component getPreview() {
        updateData();

        final WebCheckBoxList webCheckBoxList = new WebCheckBoxList(listModel);
        webCheckBoxList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {

            }

            @Override
            public void intervalRemoved(ListDataEvent e) {

            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                String ret = "";
                for (CheckBoxCellData checkBoxCellData : listModel.getElements()) {
                    if (checkBoxCellData.isSelected()) {
                        ret += checkBoxCellData.getUserObject() + ",";
                    }
                }
                if (!ret.isEmpty()) {
                    ret = ret.substring(0, ret.length() - 1);
                }
                owner.getChangeSettingQueue().put("channel.server.events", ret);
                if (!owner.getWebButtonApply().isEnabled()) {
                    owner.getWebButtonApply().setEnabled(true);
                }
            }
        });

        webCheckBoxList.setComponentPopupMenu(new WebPopupMenu() {
            {
                add(new WebMenuItem("全选") {
                    {
                        addActionListener(e -> {
                            for (int i = 0; i < listModel.size(); i++) {
                                webCheckBoxList.setCheckBoxSelected(i, true);
                            }
                        });
                    }
                });

                add(new WebMenuItem("反选") {
                    {
                        addActionListener(e -> {
                            try {
                                for (int i = 0; i < listModel.size(); i++) {
                                    webCheckBoxList.setCheckBoxSelected(i, !webCheckBoxList.isCheckBoxSelected(i));
                                }
                            } catch (Exception e1) {
                                log.error("反选错误", e1);
                            }
                        });
                    }
                });

                add(new WebMenuItem("取消全选") {
                    {
                        addActionListener(e -> {
                            for (int i = 0; i < listModel.size(); i++) {
                                webCheckBoxList.setCheckBoxSelected(i, false);
                            }
                        });
                    }
                });
            }
        });

        final WebTextField search = new WebTextField(10);
        search.setInputPrompt("按脚本文件名搜索，不区分大小写，输入完成后按回车键直接转到结果");
        search.setInputPromptFont(search.getFont().deriveFont(Font.ITALIC));
        search.setMaximumWidth(10);
        search.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    boolean find = false;
                    for (CheckBoxCellData checkBoxCellData : listModel.getElements()) {
                        if (search.getText().equalsIgnoreCase(String.valueOf(checkBoxCellData.getUserObject()))) {
                            webCheckBoxList.setSelectedIndex(listModel.indexOf(checkBoxCellData));
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        WebOptionPane.showMessageDialog(owner, "未找到脚本，请确认" + ServerConfig.WORLD_SCRIPTSPATH + "/event目录中存在该脚本", "查找脚本", WebOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        final TitleWebPanel titleWebPanel1 = new TitleWebPanel("事件脚本管理");
        titleWebPanel1.add(new GroupPanel(GroupingType.fillLast, 5, false,
                new GroupPanel(
                        GroupingType.fillFirst,
                        search,
                        new WebButton("刷新脚本列表", loadIcon("switch.png")) { // 刷新按钮
                            {
                                addActionListener(e -> updateData());
                            }
                        }),
                new WebScrollPane(webCheckBoxList)
        ).setMargin(5));
        return titleWebPanel1;
    }

    /**
     * 更新事件脚本列表
     */
    private void updateData() {
        listModel.clear();
        java.util.List<String> events = Arrays.asList(ServerConfig.CHANNEL_EVENTS.split(","));
        File eventfiles = new File(ServerConfig.WORLD_SCRIPTSPATH + "/event");
        for (String s : eventfiles.list()) {
            String event = s.substring(0, s.indexOf("."));
            listModel.addCheckBoxElement(event, events.contains(event));
        }
    }
}
