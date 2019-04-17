package server.console.groups.setting;

import java.awt.*;

public interface ConfigGroup {

    String toString();

    Component getPreview();

    /**
     * 变量的控件类型
     */
    enum ComponentType {
        编辑框,
        复选框,
        下拉菜单,
    }
}
