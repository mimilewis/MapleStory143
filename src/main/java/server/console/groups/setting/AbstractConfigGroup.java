package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;
import com.alee.laf.checkbox.WebCheckBox;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebTextField;
import configs.Config;
import server.console.Start;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractConfigGroup implements ConfigGroup {

    private static final Map<String, ImageIcon> iconsCache = new HashMap<>();
    protected final ConfigPanel owner;
    protected final String titleText;

    AbstractConfigGroup(ConfigPanel owner, String titleText) {
        this.owner = owner;
        this.titleText = titleText;
    }

    @Override
    public String toString() {
        return titleText;
    }

    public ImageIcon loadIcon(final String path) {
        final String key = Start.class.getCanonicalName() + ":" + path;
        if (!iconsCache.containsKey(key)) {
            iconsCache.put(key, new ImageIcon(getClass().getResource("/icon/" + path)));
        }
        return iconsCache.get(key);
    }

    class TitleWebPanel extends WebPanel {

        private final String titleText;

        TitleWebPanel(String titleText) {
            this.titleText = titleText;
            setMargin(15, 15, 0, 15);
            final WebPanel north = new WebPanel(new BorderLayout());
            north.add(new WebLabel(titleText).setFontSizeAndStyle(14, false, false), BorderLayout.WEST);
            final WebSeparator separator = new WebSeparator(false, false);
            separator.setSeparatorUpperColor(separator.getSeparatorColor());
            separator.setMargin(9, 5, 0, 0);
            north.add(separator, BorderLayout.CENTER);
            add(north, BorderLayout.NORTH);
        }

        public String getTitleText() {
            return titleText;
        }
    }

    class ConfigComponent extends GroupPanel {

        ConfigComponent(final String labelName) {
            this(labelName, ComponentType.编辑框);
        }

        ConfigComponent(final String labelName, String settingname) {
            this(labelName, ComponentType.编辑框, settingname);
        }

        ConfigComponent(final String labelName, ComponentType componentType) {
            this(labelName, componentType, "");
        }

        ConfigComponent(final String labelName, ComponentType componentType, final String settingname) {
            this(labelName, componentType, settingname, "");
        }

        ConfigComponent(final String labelName, ComponentType componentType, final String settingname, final String defCombo) {
            switch (componentType) {
                case 编辑框: {
                    final WebLabel label = new WebLabel(labelName + ": ", SwingConstants.RIGHT);
                    label.setPreferredWidth(150);
                    add(label);

                    final WebTextField textField = new WebTextField(30).setPreferredWidth(200);
                    if (!settingname.isEmpty()) {
                        textField.setText(Config.getProperty(settingname, ""));
                    }
                    textField.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            owner.getWebButtonApply().setEnabled(true);
                            owner.getChangeSettingQueue().put(settingname, textField.getText().replace("\\", "/"));
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            owner.getWebButtonApply().setEnabled(true);
                            owner.getChangeSettingQueue().put(settingname, textField.getText().replace("\\", "/"));
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            System.out.println(textField.getText().replace("\\", "/"));
                        }
                    });
                    add(textField);
                    break;
                }
                case 复选框: {
                    final WebCheckBox checkBox = new WebCheckBox(labelName);
                    if (!settingname.isEmpty()) {
                        checkBox.setSelected(Boolean.valueOf(Config.getProperty(settingname, "false")));
                    } else {
                        checkBox.setEnabled(false);
                    }
                    checkBox.setMargin(3, 20, 0, 0);
                    checkBox.addActionListener(e -> {
                        owner.getWebButtonApply().setEnabled(true);
                        owner.getChangeSettingQueue().put(settingname, String.valueOf(checkBox.isSelected()));
                    });
                    add(checkBox);
                    break;
                }
                case 下拉菜单: {
                    final WebLabel label = new WebLabel(labelName + ": ", SwingConstants.RIGHT);
                    label.setPreferredWidth(150);
                    add(label);
                    final WebComboBox comboBox = new WebComboBox(defCombo.split(",")).setPreferredWidth(100);
                    if (!settingname.isEmpty()) {
                        comboBox.setSelectedIndex(Integer.valueOf(Config.getProperty(settingname, "0")));
                    }
                    comboBox.addActionListener(new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            owner.getWebButtonApply().setEnabled(true);
                            owner.getChangeSettingQueue().put(settingname, String.valueOf(comboBox.getSelectedIndex()));


                        }
                    });
                    add(comboBox);
                    break;
                }
            }
        }
    }
}
