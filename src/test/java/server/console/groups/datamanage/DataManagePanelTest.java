package server.console.groups.datamanage;

import com.alee.laf.WebLookAndFeel;
import org.junit.Before;
import org.junit.Test;

import javax.swing.plaf.FontUIResource;

public class DataManagePanelTest {

    @Test
    public static void main(String[] arges) {

        final FontUIResource fontUIResource = new FontUIResource("微软雅黑", 0, 12);
        WebLookAndFeel.globalAlertFont = fontUIResource;
        WebLookAndFeel.globalMenuFont = fontUIResource;
        WebLookAndFeel.globalAcceleratorFont = fontUIResource;
        WebLookAndFeel.globalTextFont = fontUIResource;
        WebLookAndFeel.globalTitleFont = fontUIResource;
        WebLookAndFeel.globalControlFont = fontUIResource;
        WebLookAndFeel.globalTooltipFont = new FontUIResource("新宋体", 0, 32);
        WebLookAndFeel.textPaneFont = fontUIResource;
        WebLookAndFeel.install();
        DataManagePanel dataManagePanel = DataManagePanel.getInstance();
        dataManagePanel.pack();
        dataManagePanel.setLocationRelativeTo(null);
        dataManagePanel.setVisible(true);
    }

    @Before
    public void setUp() throws Exception {

        WebLookAndFeel.install();

    }
}