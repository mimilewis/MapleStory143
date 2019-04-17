package server.console.groups.database;

import com.alee.extended.filechooser.WebDirectoryChooser;
import com.alee.extended.painter.TitledBorderPainter;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.window.WebProgressDialog;
import com.alee.laf.button.WebButton;
import com.alee.laf.label.WebLabel;
import com.alee.laf.list.WebList;
import com.alee.laf.list.WebListModel;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebTextField;
import com.alee.utils.ThreadUtils;
import com.alee.utils.swing.DialogOptions;
import configs.Config;
import configs.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.Timer;
import server.console.Start;
import tools.DateUtil;
import tools.DeleteUserData;
import tools.Randomizer;
import tools.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.concurrent.ScheduledFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class DataBaseManagePanel extends WebFrame {

    private static final Logger log = LogManager.getLogger();
    public static DataBaseManagePanel instance;
    private final WebTextField setupPath = new WebTextField(20), backupPath = new WebTextField((20));
    private final WebList backupList = new WebList(new WebListModel<>());
    private transient ScheduledFuture<?> autoback;

    DataBaseManagePanel() {
        super("数据库管理");
        setIconImage(Start.getMainIcon().getImage());

        setupPath.setText(ServerConfig.DB_SETUPPATH);
        backupPath.setText(ServerConfig.DB_BACKUPPATH);
        setupPath.setEditable(false);
        backupPath.setEditable(false);
        updateSQLList();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        add(new WebPanel(new BorderLayout(5, 5)) {
            {
                setPreferredSize(400, 500);
                setResizable(false);
                setMargin(2, 5, 2, 5);

                // 设置mysql安装路径、备份路径
                add(new WebPanel() {
                    {
                        setMargin(5);
                        setPainter(new TitledBorderPainter("MySQL设置"));
                        final WebTextField time = new WebTextField(String.valueOf(ServerConfig.DB_AUTOBACKUPTIME), 5);

                        add(new GroupPanel(false,
                                new GroupPanel(
                                        new WebLabel("MySQL安装路径："),
                                        setupPath,
                                        new WebButton("...") {
                                            {
                                                addActionListener(new ActionListener() {
                                                    private WebDirectoryChooser directoryChooser = null;

                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        if (directoryChooser == null) {
                                                            directoryChooser = new WebDirectoryChooser(instance);
                                                        }
                                                        directoryChooser.setVisible(true);
                                                        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
                                                            final File file = directoryChooser.getSelectedDirectory();
                                                            ServerConfig.DB_SETUPPATH = file.getAbsolutePath();
                                                            Config.setProperty("db.setuppath", ServerConfig.DB_SETUPPATH.replace("\\", "\\\\"));
                                                            setupPath.setText(ServerConfig.DB_SETUPPATH);
                                                        }
                                                    }
                                                });
                                            }
                                        }),
                                new GroupPanel(
                                        new WebLabel("MySQL备份路径："),
                                        backupPath,
                                        new WebButton("...") {
                                            {
                                                addActionListener(new ActionListener() {
                                                    private WebDirectoryChooser directoryChooser = null;

                                                    @Override
                                                    public void actionPerformed(ActionEvent e) {
                                                        if (directoryChooser == null) {
                                                            directoryChooser = new WebDirectoryChooser(instance);
                                                        }
                                                        directoryChooser.setVisible(true);
                                                        if (directoryChooser.getResult() == DialogOptions.OK_OPTION) {
                                                            final File file = directoryChooser.getSelectedDirectory();
                                                            ServerConfig.DB_BACKUPPATH = file.getAbsolutePath();
                                                            Config.setProperty("db.backuppath", ServerConfig.DB_BACKUPPATH.replace("\\", "\\\\"));
                                                            backupPath.setText(ServerConfig.DB_BACKUPPATH);
                                                        }
                                                    }
                                                });
                                            }
                                        }),
                                new GroupPanel(new WebLabel("MySQL自动备份间隔时间（分钟）："), time, new WebButton("刷新") {
                                    {
                                        addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                ServerConfig.DB_AUTOBACKUPTIME = Integer.parseInt(time.getText());
                                                Config.setProperty("db.autobackuptime", time.getText());
                                                autoBackup();
                                                WebOptionPane.showMessageDialog(instance, "刷新成功");
                                            }
                                        });
                                    }
                                })));
                    }
                }, BorderLayout.NORTH);

                // 备份历史列表
                final String[] data = {"2016年9月3日0:37:25", "2016年9月3日0:37:32", "2016年9月3日0:37:35"};
                add(new WebPanel() {
                    {
                        setPainter(new TitledBorderPainter("历史备份列表"));

                        backupList.setEditable(false);
                        add(new WebScrollPane(new WebPanel(backupList)));

                        // 操作按钮
                        add(new GroupPanel(false,
                                new WebButton("新建备份") {
                                    {
                                        addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                if (WebOptionPane.showConfirmDialog(instance, "备份数据库大概需要几分钟的时间，全程在后台运行，期间不会影响您的其他操作，完成后将会自动通知您，是否继续？", "数据库备份", WebOptionPane.YES_NO_OPTION) == WebOptionPane.NO_OPTION) {
                                                    return;
                                                }
                                                final WebButton webButton = ((WebButton) e.getSource());
                                                final String oldtext = webButton.getText();
                                                webButton.setText("正在备份");
                                                webButton.setEnabled(false);
                                                new SwingWorker() {
                                                    @Override
                                                    protected Object doInBackground() throws Exception {
                                                        new BackupDB().run();
                                                        webButton.setText(oldtext);
                                                        webButton.setEnabled(true);
                                                        WebOptionPane.showMessageDialog(instance, "数据库备份完成！");
                                                        return null;
                                                    }
                                                }.execute();
                                            }
                                        });
                                    }
                                },
                                new WebButton("还原备份") {
                                    {
                                        addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                if (backupList.getSelectedIndex() == -1) {
                                                    WebOptionPane.showMessageDialog(instance, "请先在左侧列表选择还原点");
                                                    return;
                                                }
                                                final WebButton webButton = ((WebButton) e.getSource());
                                                final String path = (String) backupList.getSelectedValue();
                                                if (WebOptionPane.showConfirmDialog(instance, "确定还原到 " + path + " " + " 时的备份?", "还原备份", WebOptionPane.YES_NO_OPTION) == WebOptionPane.NO_OPTION) {
                                                    return;
                                                }
                                                new SwingWorker() {
                                                    @Override
                                                    protected Object doInBackground() throws Exception {
                                                        String oldtext = webButton.getText();
                                                        webButton.setText("正在还原");
                                                        webButton.setEnabled(false);
                                                        recoverDB(path);
                                                        webButton.setText(oldtext);
                                                        webButton.setEnabled(true);
                                                        return null;
                                                    }
                                                }.execute();
                                            }
                                        });
                                    }
                                },
                                new WebButton("删除备份") {
                                    {
                                        addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                int[] indexs = backupList.getSelectedIndices();
                                                if (indexs.length == 0) {
                                                    WebOptionPane.showMessageDialog(instance, "请先在左侧列表选择要删除的备份");
                                                    return;
                                                }
                                                if (WebOptionPane.showConfirmDialog(instance, "确定要删除已选的 " + indexs.length + " 个备份吗？", "删除备份", WebOptionPane.OK_CANCEL_OPTION) == WebOptionPane.OK_OPTION) {
                                                    int delete = 0;
                                                    for (int index : indexs) {
                                                        File file = new File(ServerConfig.DB_BACKUPPATH + "/" + backupList.getWebModel().get(index - delete) + ".sql.gz");
                                                        if (file.exists() && file.isFile()) {
                                                            file.delete();
                                                        } else {
                                                            WebOptionPane.showMessageDialog(instance, "删除失败，文件不存在或已删除 " + file.exists() + " " + file.isFile());
                                                        }
                                                        backupList.getWebModel().remove(index - delete);
                                                        delete++;
                                                    }
                                                }
                                            }
                                        });
                                    }
                                },
                                new WebSeparator().setMargin(5),
                                new WebButton("一键删档") {
                                    {
                                        addActionListener(new ActionListener() {
                                            @Override
                                            public void actionPerformed(ActionEvent e) {
                                                new SwingWorker() {
                                                    @Override
                                                    protected Object doInBackground() throws Exception {
                                                        int verifcode;
                                                        final String defText = "请输入验证码";
                                                        Object obj = "";
                                                        while (obj != null && !defText.equals(obj)) {
                                                            verifcode = Randomizer.rand(1000, 9999);
                                                            obj = WebOptionPane.showInputDialog(instance, "此操作不可逆，操作前尽量备份数据库，以免悔恨终生。\r\n如果已经过慎重考虑，那么请输入验证码：" + verifcode, "清空玩家数据", JOptionPane.WARNING_MESSAGE, null, null, defText);
                                                            if (obj instanceof String && StringUtil.isNumber(String.valueOf(obj))) {
                                                                int resultcode = Integer.valueOf(String.valueOf(obj));
                                                                if (resultcode != verifcode) {
                                                                    WebOptionPane.showMessageDialog(instance, "验证码错误，请重新输入");
                                                                } else {
                                                                    // 清空数据
                                                                    Thread thread = new Thread(DeleteUserData::run);
                                                                    thread.start();
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        return null;
                                                    }
                                                }.execute();
                                            }
                                        });
                                    }
                                }
                        ), BorderLayout.EAST);
                    }
                }, BorderLayout.CENTER);
            }
        });
    }

    public static DataBaseManagePanel getInstance() {
        if (instance == null) {
            instance = new DataBaseManagePanel();
        }
        return instance;
    }

    private void updateSQLList() {
        File file = new File(ServerConfig.DB_BACKUPPATH);
        if (!file.exists()) {
            return;
        }
        WebListModel webModel = backupList.getWebModel();
        webModel.clear();
        for (String s : file.list()) {
            if (s.endsWith(".sql.gz")) {
                webModel.add(s.substring(0, s.indexOf(".")));
            }
        }
    }

    public void recoverDB(String path) {
        String command = "\"" + ServerConfig.DB_SETUPPATH + "\\bin\\mysql.exe\" -u" + ServerConfig.DB_USER + " -p" + ServerConfig.DB_PASSWORD + " --default-character-set=utf8";
        WebProgressDialog webProgressDialog = new WebProgressDialog("数据库还原备份");
        webProgressDialog.setPreferredProgressWidth(300);
        webProgressDialog.setVisible(true);
        try {
            Process process = Runtime.getRuntime().exec(command);
            try (OutputStream outputStream = process.getOutputStream()) {
                try (FileInputStream fileInputStream = new FileInputStream(ServerConfig.DB_BACKUPPATH + "/" + path + ".sql.gz")) {
                    final int max = fileInputStream.available();
                    webProgressDialog.setText("正在还原备份到: " + path + "...");
                    try (GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream)) {
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = gzipInputStream.read(buf)) > 0) {
                            outputStream.write(buf, 0, len);
                            webProgressDialog.setProgress((int) (100 - ((double) fileInputStream.available() / max) * 100));
                        }
                    }
                }
            } catch (IOException e) {
                webProgressDialog.setVisible(false);
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    String buff;
                    while ((buff = bufferedReader.readLine()) != null) {
                        stringBuilder.append(buff).append("\r\n");
                    }
                    log.error(stringBuilder);
                    if (stringBuilder.indexOf("using password: YES") != -1) {
                        WebOptionPane.showMessageDialog(instance, "数据库连接失败，请在配置参数里确认数据库IP、端口、账号、密码、库名 是否填写正确。");
                        instance.setVisible(false);
                        Start.getInstance().showConfigPanel();
                    }
                }
                log.error(e);
            }
            webProgressDialog.setProgressText("备份恢复完成...");
            ThreadUtils.sleepSafely(1000);
            webProgressDialog.setVisible(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void autoBackup() {
        if (autoback != null) {
            autoback.cancel(false);
        }
        autoback = Timer.GuiTimer.getInstance().register(new BackupDB(), ServerConfig.DB_AUTOBACKUPTIME * 60 * 1000, ServerConfig.DB_AUTOBACKUPTIME * 60 * 1000);
    }

    public class BackupDB implements Runnable {
        @Override
        public void run() {
            String command = "\"" + ServerConfig.DB_SETUPPATH + "\\bin\\mysqldump.exe\" --no-defaults -u" + ServerConfig.DB_USER + " -p" + ServerConfig.DB_PASSWORD + " --default-character-set=utf8 --database \"" + ServerConfig.DB_NAME + "\"";
            try {
                Process process = Runtime.getRuntime().exec(command);
                File file = new File(ServerConfig.DB_BACKUPPATH + "/" + DateUtil.getNowTime() + ".sql.gz");
                file.getParentFile().mkdirs();
                try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                    try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
                        try (InputStream inputStream = process.getInputStream()) {
                            byte[] buf = new byte[1024];
                            int len;
                            while ((len = inputStream.read(buf)) != -1) {
                                gzipOutputStream.write(buf, 0, len);
                            }
                            gzipOutputStream.finish();
                            gzipOutputStream.flush();
                            updateSQLList();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
