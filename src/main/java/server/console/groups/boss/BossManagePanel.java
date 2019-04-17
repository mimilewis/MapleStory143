package server.console.groups.boss;

import com.alee.extended.painter.TitledBorderPainter;
import com.alee.extended.panel.GroupPanel;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.list.WebListModel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebDialog;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebTextField;
import scripting.event.BossEventManager;

import javax.swing.*;
import java.awt.*;

public class BossManagePanel extends WebDialog {

    private final WebList bosslist = new WebList(new WebListModel<String>());
    private final WebCheckBox isOpen;
    private final WebCheckBox isUseCustomScript;
    private final WebTextField maxPlayerCount = new WebTextField(10);
    private final WebTextField maxCount = new WebTextField(10);
    private final WebTextField maxTime = new WebTextField(10);

    {
        isUseCustomScript = new WebCheckBox("使用默认脚本");
        isOpen = new WebCheckBox("开放挑战");
        isUseCustomScript.setMargin(3, 0, 3, 0);
        isOpen.setMargin(3, 0, 3, 0);
        isUseCustomScript.addActionListener(e -> setSettingEnable(isUseCustomScript.isSelected(), false));

    }

    public BossManagePanel(Frame owner) {
        super(owner, "事件脚本管理");
        setPreferredSize(new Dimension(600, 400));
        add(new WebPanel(new BorderLayout(5, 5)) {
            {
                setMargin(3);
                for (String s : BossEventManager.getInstance().getAllBossName()) {
                    bosslist.getWebModel().add(s);
                }
                bosslist.setPreferredWidth(150);
                bosslist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                bosslist.addListSelectionListener(e -> {
                    BossEventManager.BossEventEntry eventEntry = BossEventManager.getInstance().getBossEventEntry((String) bosslist.getSelectedValue());
                    isOpen.setSelected(eventEntry.isOpen());
                    isUseCustomScript.setSelected(eventEntry.isUseDefaultScript());
                    maxPlayerCount.setText(String.valueOf(eventEntry.getMaxPlayerCount()));
                    maxCount.setText(String.valueOf(eventEntry.getMaxCount()));
                    maxTime.setText(String.valueOf(eventEntry.getMaxTime()));
                    setSettingEnable(eventEntry.isUseDefaultScript(), false);
                });
                add(new WebPanel() {
                    {
                        final WebCheckBox checkBox = new WebCheckBox("使用默认脚本(关闭后可使用自定义脚本)", true);
                        checkBox.setMargin(5);
                        checkBox.addChangeListener(e -> setSettingEnable(checkBox.isSelected(), true));
                        add(checkBox, BorderLayout.NORTH);
                        add(new WebScrollPane(bosslist));
                    }
                }, BorderLayout.WEST);
                add(new WebPanel(new BorderLayout(5, 5)) {
                    {
                        setPainter(new TitledBorderPainter("参数设置", SwingConstants.CENTER));
                        setMargin(5, 20, 5, 20);
                        final GroupPanel groupPanel1 = new GroupPanel(new WebLabel("最大进入人数："), maxPlayerCount);
                        final GroupPanel groupPanel2 = new GroupPanel(new WebLabel("每天进入次数："), maxCount);
                        final GroupPanel groupPanel3 = new GroupPanel(new WebLabel("最大挑战时间："), maxTime);
                        add(new GroupPanel(false, isUseCustomScript, isOpen, new WebSeparator().setMargin(5, 0, 5, 0), groupPanel1, groupPanel2, groupPanel3));
                    }
                });
            }
        });
        bosslist.setSelectedIndex(0);
    }

    public void setSettingEnable(boolean isUse, boolean all) {
        maxPlayerCount.setEnabled(isUse);
        maxCount.setEnabled(isUse);
        maxTime.setEnabled(isUse);
        if (all) {
            bosslist.setEnabled(isUse);
            isOpen.setEnabled(isUse);
            isUseCustomScript.setEnabled(isUse);
        }
    }
}
