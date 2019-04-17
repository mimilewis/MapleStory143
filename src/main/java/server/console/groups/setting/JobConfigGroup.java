package server.console.groups.setting;

import client.skills.SkillFactory;
import com.alee.extended.list.CheckBoxCellData;
import com.alee.extended.list.CheckBoxListModel;
import com.alee.extended.list.WebCheckBoxList;
import com.alee.extended.panel.CenterPanel;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.panel.GroupingType;
import com.alee.extended.panel.WebButtonGroup;
import com.alee.laf.button.WebButton;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextField;
import configs.ServerConfig;
import constants.ServerConstants;
import constants.skills.*;
import provider.MapleOverrideData;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class JobConfigGroup extends AbstractConfigGroup {

    public final Class[] classes = {
            战士.class,
            英雄.class,
            圣骑士.class,
            黑骑士.class,
            魔法师.class,
            火毒.class,
            冰雷.class,
            主教.class,
            弓箭手.class,
            神射手.class,
            箭神.class,
            飞侠.class,
            隐士.class,
            侠盗.class,
            海盗.class,
            冲锋队长.class,
            船长.class,
            神炮王.class,
            龙的传人.class,
            初心者.class,
            魂骑士.class,
            炎术士.class,
            风灵使者.class,
            夜行者.class,
            奇袭者.class,
            预备兵.class,
            唤灵斗师.class,
            豹弩游侠.class,
            机械师.class,
            爆破手.class,
            恶魔猎手.class,
            恶魔复仇者.class,
            尖兵.class,
            战神.class,
            龙神.class,
            夜光.class,
            双弩.class,
            双刀.class,
            幻影.class,
            狂龙战士.class,
            林之灵.class,
            爆莉萌天使.class,
            隐月.class,
            神之子.class,
            米哈尔.class,
            剑豪.class,
            阴阳师.class,
            品克缤.class,
    };

    private final Map<String, Map<Integer, String>> skills = new LinkedHashMap<>();
    private final List<String> blockSkills = new ArrayList<>(Arrays.asList(ServerConfig.WORLD_BLOCKSKILLS.split(",")));
    private SkillDataPanel skillDataPanel = new SkillDataPanel();
    private Map<String, WebTextField> modifiedMaps = new HashMap<>();
    private int skillid = 0;

    {
        for (Class aClass : classes) {
            Map<Integer, String> skillss = new LinkedHashMap<>();
            for (Field field : aClass.getDeclaredFields()) {
                try {
                    skillss.put(field.getInt(field.getName()), field.getName());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            String jobname = aClass.getName();
            skills.put(jobname.split("\\.")[2], skillss);
        }
    }

    JobConfigGroup(ConfigPanel owner) {
        super(owner, "职业&技能管理");
    }

    @Override
    public Component getPreview() {
        TitleWebPanel titleWebPanel1 = new TitleWebPanel("职业&技能设置");

        WebList jobList = new WebList(skills.keySet().toArray());
        CheckBoxListModel skillListMode = new CheckBoxListModel();
        WebCheckBoxList skillList = new WebCheckBoxList(skillListMode);
        WebPanel skillData = new WebPanel();
        jobList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        skillList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        jobList.setPreferredWidth(100);
        skillData.setPreferredWidth(300);
        skillListMode.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {

            }

            @Override
            public void intervalRemoved(ListDataEvent e) {

            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                String ret = "";
                for (CheckBoxCellData checkBoxCellData : skillListMode.getElements()) {
                    int skillid = 0;
                    for (Map.Entry<Integer, String> entry : skills.get(jobList.getSelectedValue()).entrySet()) {
                        if (entry.getValue().equals(checkBoxCellData.getUserObject())) {
                            skillid = entry.getKey();
                            break;
                        }
                    }
                    if (checkBoxCellData.isSelected() && !blockSkills.contains(String.valueOf(skillid))) {
                        blockSkills.add(String.valueOf(skillid));
                    } else if (!checkBoxCellData.isSelected() && blockSkills.contains(String.valueOf(skillid))) {
                        blockSkills.remove(String.valueOf(skillid));
                    }
                }

                for (String integer : blockSkills) {
                    if (!integer.isEmpty()) {
                        ret += integer + ",";
                    }
                }
                if (!ret.isEmpty()) {
                    ret = ret.substring(0, ret.length() - 1);
                }

                owner.getChangeSettingQueue().put("world.blockskills", ret);
                if (!owner.getWebButtonApply().isEnabled()) {
                    owner.getWebButtonApply().setEnabled(true);
                }
            }
        });

        skillList.setComponentPopupMenu(new WebPopupMenu() {
            {
                add(new WebMenuItem("全选") {
                    {
                        addActionListener(e -> {
                            for (int i = 0; i < skillListMode.size(); i++) {
                                skillList.setCheckBoxSelected(i, true);
                            }
                        });
                    }
                });

                add(new WebMenuItem("反选") {
                    {
                        addActionListener(e -> {
                            for (int i = 0; i < skillListMode.size(); i++) {
                                skillList.setCheckBoxSelected(i, !skillList.isCheckBoxSelected(i));
                            }
                        });
                    }
                });

                add(new WebMenuItem("取消全选") {
                    {
                        addActionListener(e -> {
                            for (int i = 0; i < skillListMode.size(); i++) {
                                skillList.setCheckBoxSelected(i, false);
                            }
                        });
                    }
                });
            }
        });

        skillList.addListSelectionListener(e -> {
            CheckBoxCellData selectedValue = (CheckBoxCellData) ((WebCheckBoxList) e.getSource()).getSelectedValue();
            if (selectedValue != null) {
                skillid = getSkillID(selectedValue.getUserObject().toString());
                skillDataPanel.removeAll();
                skillDataPanel.display(skillid);
//            WebOptionPane.showMessageDialog(owner, getSkillID(((CheckBoxCellData) ((WebCheckBoxList) e.getSource()).getSelectedValue()).getUserObject().toString()));
            }
        });


        jobList.addListSelectionListener(e -> {
            skillListMode.clear();
            skills.get(jobList.getSelectedValue()).forEach((integer, s) -> skillListMode.addCheckBoxElement(s, isBlockSkill(integer)));
        });

//        skillData.add(new WebLabel(SkillFactory.getH(4341052), JLabel.LEFT));
        skillData.add(skillDataPanel);
        skillData.add(new CenterPanel(new WebButtonGroup(new WebButton("应用") {
            {
                setPreferredWidth(100);
                addActionListener(e -> {
                    // 技能ID为0时禁止执行后续的应用操作
                    if (skillid == 0) {
                        return;
                    }
                    HashMap<String, String> values = new HashMap<>();
                    modifiedMaps.forEach((key, value) -> values.put(key, value.getText()));
                    if (values.isEmpty()) {
                        WebOptionPane.showMessageDialog(owner, "没有发生变动");
                        return;
                    }
                    MapleOverrideData.getInstance().getOverridedata().put(skillid, values);
                    SkillFactory.reloadSkills(skillid);
                    MapleOverrideData.getInstance().save();
                    WebOptionPane.showMessageDialog(owner, "应用完成");
                });
            }
        }, new WebButton("重置") {
            {
                setPreferredWidth(100);
                addActionListener(e -> {
                    // 技能ID为0时禁止执行后续的应用操作
                    if (skillid == 0) {
                        return;
                    }
                    modifiedMaps.forEach((s, webTextField) -> webTextField.setText(SkillFactory.getSkillDefaultData(skillid, s)));
                    SkillFactory.reloadSkills(skillid);
                    MapleOverrideData.getInstance().getOverridedata().remove(skillid);
                    MapleOverrideData.getInstance().save();
                    WebOptionPane.showMessageDialog(owner, "重置完成");
                });
            }
        })), BorderLayout.SOUTH);
        WebPanel panel = new WebPanel() {
            {
                setMargin(0, 15, 0, 15);
                add(getOpenJobList(), BorderLayout.NORTH);
                add(new WebScrollPane(jobList), BorderLayout.WEST);
                add(new WebScrollPane(skillList));
                add(new WebScrollPane(skillData) {
                    {
                        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                    }
                }, BorderLayout.EAST);
                add(new WebLabel("<html><p>注意:</p><p>* 职业打勾为启用该职业创建</p><p>* 技能打勾即为禁用该技能</p></html>").setMargin(10, 0, 10, 0), BorderLayout.SOUTH);
            }
        };

        return new GroupPanel(GroupingType.fillLast, 5, false, titleWebPanel1, panel);
    }

    public boolean isBlockSkill(int skillid) {
        return blockSkills.contains(String.valueOf(skillid));
    }

    public Component getOpenJobList() {
        WebPanel openJobList = new WebPanel(new GridLayout(5, 5, 0, 5));
        for (String s : ServerConstants.JOB_NAMELIST) {
            openJobList.add(new WebCheckBox(s, ServerConstants.isOpenJob(s)) {
                {
                    setPreferredWidth(100);
                    addActionListener(e -> {
                        if (((WebCheckBox) e.getSource()).isSelected()) {
                            openJob(s);
                        } else {
                            closeJob(s);
                        }
                    });
                }
            });
        }
        openJobList.setMargin(0, 0, 10, 0);
        return openJobList;
    }

    private void closeJob(String jobname) {
        if (!ServerConfig.WORLD_CLOSEJOBS.contains(jobname)) {
            ServerConfig.WORLD_CLOSEJOBS += jobname + ",";
        }
        saveChange();
    }

    private void openJob(String jobname) {
        if (ServerConfig.WORLD_CLOSEJOBS.contains(jobname)) {
            ServerConfig.WORLD_CLOSEJOBS = ServerConfig.WORLD_CLOSEJOBS.replace(jobname + ",", "");
        }
        saveChange();
    }

    private void saveChange() {
        owner.getChangeSettingQueue().put("world.closejobs", ServerConfig.WORLD_CLOSEJOBS);
        if (!owner.getWebButtonApply().isEnabled()) {
            owner.getWebButtonApply().setEnabled(true);
        }
    }

    private int getSkillID(String name) {
        for (Map.Entry<String, Map<Integer, String>> entry : skills.entrySet()) {
            for (Map.Entry<Integer, String> stringEntry : entry.getValue().entrySet()) {
                if (stringEntry.getValue().equals(name)) {
                    return stringEntry.getKey();
                }
            }
        }
        return 0;
    }

    public class SkillDataPanel extends WebPanel {

        private SkillDataPanel() {
            super();
        }

        public void display(int skillid) {
            String h = SkillFactory.getH(skillid) + " ";
            add(new DescLabel(h), BorderLayout.NORTH);
            List<String> property = new ArrayList<>();
            boolean find = false;
            StringBuilder temp = new StringBuilder();
            for (char c : h.toCharArray()) {
                if (c == '#') {
                    find = true;
                    continue;
                }
                if (find) {
                    if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z') && (c < '0' || c > '9')) {
                        find = false;
                        if (temp.length() != 0) {
                            property.add(temp.toString());
                            temp = new StringBuilder();
                        }
                    } else {
                        temp.append(c);
                    }
                }
            }
            GroupPanel panel = new GroupPanel(false);
            panel.setMargin(10);
            property = property.stream().filter(this::canChange).collect(Collectors.toList());
            modifiedMaps.clear();
            for (String s : property) {
                WebLabel label = new WebLabel(s, SwingConstants.RIGHT) {
                    {
                        setMargin(0, 0, 0, 5);
                        setPreferredWidth(120);
                    }
                };
                WebTextField field = new WebTextField(getNewValue(skillid, s), 10);
                panel.add(new GroupPanel(label, field));
                modifiedMaps.put(s, field);
            }
            add(panel, BorderLayout.CENTER);
            updateUI();
        }

        private String getNewValue(int skillid, String p) {
            String ret;
            Map<Integer, Map<String, String>> overridedata = MapleOverrideData.getInstance().getOverridedata();
            if (overridedata.containsKey(skillid) && overridedata.get(skillid).containsKey(p)) {
                ret = overridedata.get(skillid).get(p);
            } else {
                ret = SkillFactory.getSkillDefaultData(skillid, p);
            }
            return ret == null ? "" : ret;
        }

        private boolean canChange(String name) {
            switch (name) {
                case "c":
                case "dotTime":
                case "dotInterval":
                case "dot":
                case "attackCount":
                case "damage":
                    return false;
            }
            return true;
        }
    }

    public class DescLabel extends JTextArea {
        public DescLabel(String text) {
            super(text, 2, 10);
            setBackground(null);
            setEditable(false);
            setBorder(null);
            setLineWrap(true);
            setWrapStyleWord(false);
            setFocusable(false);
            setMargin(new Insets(5, 5, 5, 15));
        }
    }

}
