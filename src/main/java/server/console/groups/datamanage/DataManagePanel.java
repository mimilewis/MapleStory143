package server.console.groups.datamanage;

import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.tabbedpane.TabbedPaneStyle;
import com.alee.laf.tabbedpane.WebTabbedPane;
import server.console.Start;

import java.awt.*;
import java.util.ArrayList;

public class DataManagePanel extends WebFrame {

    private static DataManagePanel instance = null;
    private final java.util.List<TabbedPane> tabbedPanes = new ArrayList<>();

    public DataManagePanel() {
        setPreferredSize(new Dimension(1366, 800));
        setTitle("数据管理界面");
        setIconImage(Start.getMainIcon().getImage());

        final WebTabbedPane tabbedPane = new WebTabbedPane() {
            @Override
            public Dimension getPreferredSize() {
                final Dimension ps = super.getPreferredSize();
                ps.width = 150;
                return ps;
            }
        };
        tabbedPane.setTabbedPaneStyle(TabbedPaneStyle.attached);
        tabbedPanes.add(PlayerPane.getInstance(this));
        tabbedPanes.add(new DropPane(this));
        tabbedPanes.add(new CashShopPane(this));
        tabbedPanes.add(new NPCShopPane(this));
        tabbedPanes.add(new GuildPane(this));
        tabbedPanes.add(new FishingPane(this));
        final WebPanel webPanel = new WebPanel(false, tabbedPane);
        webPanel.setPaintFocus(true);
        add(tabbedPane);
        settupTabbedPane(tabbedPane);
    }

    public static DataManagePanel getInstance() {
        if (instance == null) {
            instance = new DataManagePanel();
        }
        return instance;
    }

    private void settupTabbedPane(WebTabbedPane webTabbedPane) {
        for (TabbedPane tabbedPane : tabbedPanes) {
            webTabbedPane.addTab(tabbedPane.getTitle(), tabbedPane.getPreview());
        }
//        webTabbedPane.addTab("角色管理", new WebLabel("角色管理"));
//        webTabbedPane.addTab("爆率管理", new WebLabel("爆率管理"));
//        webTabbedPane.addTab("NPC商店管理", new WebLabel("NPC商店管理"));
//        webTabbedPane.addTab("商城管理", new WebLabel("商城管理"));
//        webTabbedPane.addTab("家族管理", new WebLabel("家族管理"));
//        webTabbedPane.addTab("钓鱼管理", new WebLabel("钓鱼管理"));
    }
}
