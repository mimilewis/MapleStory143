package tools;

import client.MapleClient;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.text.WebTextArea;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

public class DebugUITest extends WebFrame {
    private MapleClient c;

    public DebugUITest() {
        super("DebugUITest");

        setPreferredSize(new Dimension(500, 400));

        final WebTextArea packetTextArea = new WebTextArea();
        final WebLabel statusLabel = new WebLabel();
        final Vector<String> historylist = new Vector<>();
        packetTextArea.setLineWrap(true);

        add(new WebPanel(new BorderLayout(5, 5)) {
            {
                add(new WebComboBox(historylist) {
                    {
                        historylist.add("1");
                        historylist.add("2");
                        historylist.add("3");
                    }
                }, BorderLayout.NORTH);
                add(new WebScrollPane(packetTextArea), BorderLayout.CENTER);
                add(new WebPanel() {
                    {
                        add(statusLabel, BorderLayout.WEST);
                        add(new WebButton("测试封包") {
                            {
                                addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {

                                        historylist.insertElementAt(packetTextArea.getText(), 0);
                                        byte[] data = HexTool.getByteArrayFromHexString(packetTextArea.getText());
                                        packetTextArea.setText(null);
                                        statusLabel.setText("发送成功，发送的封包长度: " + data.length);
                                    }
                                });
                            }
                        }, BorderLayout.EAST);
                    }
                }, BorderLayout.SOUTH);
            }
        });

        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setLocationRelativeTo(null);
        setVisible(true);

    }

    @Test
    public static void main(String[] arge) {
        new DebugUITest();
    }

    @Before
    public void setUp() throws Exception {

        FontUIResource fontUIResource = new FontUIResource("微软雅黑", 0, 12);
        WebLookAndFeel.globalTextFont = new FontUIResource("Consolas", 0, 12);
        WebLookAndFeel.globalControlFont = fontUIResource;
        WebLookAndFeel.globalTitleFont = fontUIResource;
        UIManager.setLookAndFeel(new WebLookAndFeel());

    }

    /**
     * 获取连接
     */
    public MapleClient getC() {
        return c;
    }

    /**
     * 设置连接和窗口的标题
     */
    public void setC(MapleClient c) {
        this.c = c;
        if (c.getPlayer() != null) {
            setTitle("玩家: " + c.getPlayer().getName() + " - 封包测试");
        }
    }
}
