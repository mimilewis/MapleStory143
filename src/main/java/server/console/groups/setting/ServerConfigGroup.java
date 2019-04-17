package server.console.groups.setting;

import com.alee.extended.panel.GroupPanel;
import com.alee.laf.scroll.WebScrollPane;
import constants.ServerConstants;

import java.awt.*;

public class ServerConfigGroup extends AbstractConfigGroup {


    public ServerConfigGroup(ConfigPanel owner) {
        super(owner, "服务端配置");
    }

    @Override
    public Component getPreview() {

        TitleWebPanel titleWebPanel1 = new TitleWebPanel("基本设置");
        titleWebPanel1.add(new GroupPanel(false,
                new ConfigComponent("游戏名称", "login.server.name"),
                new ConfigComponent("频道数量", ComponentType.下拉菜单, "channel.server.ports", "1,2,3,4,5,6,7,8,9,10"),
                new ConfigComponent("游戏服务器名称", ComponentType.下拉菜单, "login.server.flag", getAllServerName()),
                new ConfigComponent("游戏服务器状态", ComponentType.下拉菜单, "login.server.status", getAllServerStatus())
        ).setMargin(5));

        TitleWebPanel titleWebPanel2 = new TitleWebPanel("数据库连接设置");
        titleWebPanel2.add(new GroupPanel(false,
                new ConfigComponent("IP", "db.ip"),
                new ConfigComponent("端口", "db.port"),
                new ConfigComponent("数据库名称", "db.name"),
                new ConfigComponent("账号", "db.user"),
                new ConfigComponent("密码", "db.password")
//                new GroupPanel(new WebButton("测试连接") {
//                    {
//                        setPreferredWidth(100);
//                        addActionListener(new ActionListener() {
//                            @Override
//                            public void actionPerformed(ActionEvent e) {
//                                new SwingWorker() {
//                                    @Override
//                                    protected Object doInBackground() throws Exception {
//                                        setEnabled(false);
//                                        String oldtext = getText();
//                                        setText("正在测试...");
//                                        owner.applyChange();
//                                        DatabaseConnection.DataBaseStatus status = DatabaseConnection.TestConnection();
//                                        WebOptionPane.showMessageDialog(null, status.name());
//                                        Start.getInstance().setDataBaseStatus(status);
//                                        setText(oldtext);
//                                        setEnabled(true);
//                                        return null;
//                                    }
//                                }.execute();
//                            }
//                        });
//                    }
//                }).setMargin(0, 150, 0, 0)
        ).setMargin(5));

        TitleWebPanel titleWebPanel3 = new TitleWebPanel("其他设置");
        titleWebPanel3.add(new GroupPanel(false,
                new ConfigComponent("脚本路径", "world.server.scriptspath")
        ).setMargin(5));

        WebScrollPane webScrollPane = new WebScrollPane(new GroupPanel(5, false, titleWebPanel1, titleWebPanel2, titleWebPanel3), false);
        webScrollPane.createHorizontalScrollBar();
        webScrollPane.getViewport().setOpaque(false);
        return webScrollPane;
    }

    private String getAllServerName() {
        StringBuilder sb = new StringBuilder();
        for (ServerConstants.MapleServerName serverName : ServerConstants.MapleServerName.values()) {
            sb.append(serverName.name()).append(",");
        }
        return sb.toString();
    }

    private String getAllServerStatus() {
        StringBuilder sb = new StringBuilder();
        for (ServerConstants.MapleServerStatus status : ServerConstants.MapleServerStatus.values()) {
            sb.append(status.name()).append(",");
        }
        return sb.toString();
    }


}
