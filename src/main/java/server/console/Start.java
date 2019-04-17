package server.console;

import client.inventory.MapleInventoryIdentifier;
import com.alee.extended.label.WebHotkeyLabel;
import com.alee.extended.painter.TitledBorderPainter;
import com.alee.extended.panel.GroupPanel;
import com.alee.extended.progress.WebProgressOverlay;
import com.alee.extended.statusbar.WebMemoryBar;
import com.alee.extended.statusbar.WebStatusBar;
import com.alee.global.StyleConstants;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.laf.combobox.WebComboBox;
import com.alee.laf.label.WebLabel;
import com.alee.laf.menu.WebMenuItem;
import com.alee.laf.menu.WebPopupMenu;
import com.alee.laf.optionpane.WebOptionPane;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.progressbar.WebProgressBar;
import com.alee.laf.rootpane.WebFrame;
import com.alee.laf.scroll.WebScrollBar;
import com.alee.laf.scroll.WebScrollPane;
import com.alee.laf.separator.WebSeparator;
import com.alee.laf.text.WebTextField;
import com.alee.laf.text.WebTextPane;
import com.alee.utils.ThreadUtils;
import configs.Config;
import configs.OpcodeConfig;
import configs.ServerConfig;
import constants.ItemConstants;
import database.DatabaseConnection;
import handling.Auction.AuctionServer;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleDojoRanking;
import handling.channel.MapleGuildRanking;
import handling.channel.PlayerStorage;
import handling.chat.ChatServer;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.WorldBroadcastService;
import handling.world.WorldRespawnService;
import handling.world.family.MapleFamily;
import handling.world.guild.MapleGuild;
import handling.world.messenger.MessengerRankingWorker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.*;
import server.Timer;
import server.carnival.MapleCarnivalFactory;
import server.console.groups.boss.BossManagePanel;
import server.console.groups.database.DataBaseManagePanel;
import server.console.groups.datamanage.DataManagePanel;
import server.console.groups.setting.ConfigPanel;
import server.life.PlayerNPC;
import server.quest.MapleQuest;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;


public class Start extends WebFrame {


    private static final Logger log = LogManager.getLogger(Start.class.getName());
    public static boolean startFinish = false;
    private static Start instance = null;
    private final StartFrame progress;
    private final Thread start_thread;
    private final String server_version;
    private WebTextPane textPane;
    private long starttime = 0;
    private ScheduledFuture<?> shutdownServer, startRunTime;
    private WebHotkeyLabel[] labels;
    private boolean autoScroll = true;
    private WebHotkeyLabel runningTimelabel;
    private DatabaseConnection.DataBaseStatus dataBaseStatus;

    public Start() {
        start_thread = new Thread(new StartThread());

        // 创建主面板
        final WebPanel contentPane = new WebPanel();

        contentPane.setPreferredSize(1000, 600);
        setMinimumSize(contentPane.getPreferredSize());

        Timer.GuiTimer.getInstance().start();

        ProgressBarObservable progressBarObservable = new ProgressBarObservable();
        ProgressBarObserver progressBarObserver = new ProgressBarObserver(progressBarObservable);

        progress = createProgressDialog();
        progress.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                System.exit(0);
            }
        });

        progress.setIconImage(getMainIcon().getImage());
        progress.setTitle("服务端正在启动...");
        setIconImage(getMainIcon().getImage());
        setLayout(new BorderLayout());

        Properties properties = new Properties();
        try {
            properties.load(Start.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        server_version = properties.getProperty("version");

        progressBarObservable.setProgress(new Pair<>("初始化配置...", 0));
        configs.Config.load();
        progressBarObservable.setProgress(new Pair<>("检查网络状态...", 10));

        progressBarObservable.setProgress(new Pair<>("初始化数据库配置...", 30));
        dataBaseStatus = DatabaseConnection.getInstance().TestConnection();
        InitializeServer.initializeRedis(false, progressBarObservable);

        ThreadUtils.sleepSafely(1000);
        progress.setVisible(false);

        contentPane.add(createMainPane(), BorderLayout.CENTER);
        contentPane.add(createStatusBar(), BorderLayout.SOUTH);

        add(contentPane);

        progressBarObserver.deleteObserver(progressBarObservable);
        progressBarObservable.deleteObservers();

        setTitle("彩虹冒险岛服务端  当前游戏版本: v." + ServerConfig.LOGIN_MAPLE_VERSION + "." + ServerConfig.LOGIN_MAPLE_PATCH + " 服务端版本: " + server_version);

        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = WebOptionPane.showConfirmDialog(instance, "确定要退出？", "警告", WebOptionPane.YES_NO_OPTION);
                if (result == WebOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        SwingUtilities.invokeLater(DataManagePanel::getInstance);

        System.setOut(new PrintStream(new NewOutputStram((byte) 0)));
        System.setErr(new PrintStream(new NewOutputStram((byte) 1)));
    }

    public static Start getInstance() {
        if (instance == null) {
            instance = new Start();
        }
        return instance;
    }

    public static ImageIcon loadIcon(final String path) {
        return new ImageIcon(Start.class.getResource("/icon/" + path));
    }

    public static ImageIcon getMainIcon() {
        return loadIcon("1002140.png");
    }

    private static void checkSingleInstance() {
        try {
            new ServerSocket(26351);
        } catch (IOException ex) {
            if (ex.getMessage().contains("Address already in use: JVM_Bind")) {
                WebOptionPane.showMessageDialog(instance, "同一台电脑只能运行一个服务端，若因服务端未正常关闭，请在任务管理器内结束javaw.exe进程", "错误", WebOptionPane.ERROR_MESSAGE);
                System.out.println();
            }
            System.exit(0);
        }
    }

    private static void run() {
        Start.getInstance().display();
        Start.getInstance().testDatabaseConnection();
    }

    public static void main(String[] args) {
        checkSingleInstance();
        final FontUIResource fontUIResource = new FontUIResource("微软雅黑", 0, 12);
        WebLookAndFeel.globalControlFont = fontUIResource;
        WebLookAndFeel.globalTooltipFont = fontUIResource;
        WebLookAndFeel.globalAlertFont = fontUIResource;
        WebLookAndFeel.globalMenuFont = fontUIResource;
        WebLookAndFeel.globalAcceleratorFont = fontUIResource;
        WebLookAndFeel.globalTitleFont = fontUIResource;
        WebLookAndFeel.globalTextFont = fontUIResource;
        WebLookAndFeel.toolTipFont = fontUIResource;
        WebLookAndFeel.textPaneFont = fontUIResource;
        WebLookAndFeel.install();

        run();
    }

    public static void showMessage(String error, String title, int type) {
        WebOptionPane.showMessageDialog(null, error, title, type);
    }

    public String getServer_version() {
        return server_version;
    }

    public void setDataBaseStatus(DatabaseConnection.DataBaseStatus dataBaseStatus) {
        this.dataBaseStatus = dataBaseStatus;
    }

    private boolean testDatabaseConnection() {
        if (!dataBaseStatus.equals(DatabaseConnection.DataBaseStatus.连接成功)) {
            if (WebOptionPane.showConfirmDialog(instance, "数据库连接失败，将转到配置页面，请务必通过测试连接，否则服务端无法启动", "", WebOptionPane.OK_CANCEL_OPTION) == WebOptionPane.OK_OPTION) {
                showConfigPanel();
            }
            return false;
        }
        return true;
    }

    private void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }

    private StartFrame createProgressDialog() {
        final StartFrame progress = new StartFrame();
        progress.setUndecorated(true);
        progress.pack();
        progress.setResizable(false);
        progress.setLocationRelativeTo(null);
        progress.setVisible(true);

        return progress;
    }

    private Component createMainPane() {
        final WebPanel contentPane = new WebPanel();

        contentPane.setLayout(new BorderLayout());

        // 创建运行日志
        final WebPanel runningLogPane = new WebPanel(new BorderLayout());
        runningLogPane.setPainter(new TitledBorderPainter("运行日志")).setMargin(2);
        runningLogPane.setPreferredSize(660, 300);
        textPane = new WebTextPane();
        textPane.setEditable(false);
        textPane.setComponentPopupMenu(new WebPopupMenu() {
            {
                add(new WebMenuItem("清屏") {
                    {
                        addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                textPane.clear();
                            }
                        });
                    }
                });
            }
        });
        final WebScrollPane textPaneScroll = new WebScrollPane(textPane);
        textPaneScroll.createVerticalScrollBar();

        // 实现滚动条到达底部后自动滚动，否则不自动滚动
        textPaneScroll.addMouseWheelListener(e -> {
            WebScrollBar scrollBar = textPaneScroll.getWebVerticalScrollBar();
            autoScroll = e.getWheelRotation() != -1 && scrollBar.getMaximum() - scrollBar.getValue() <= scrollBar.getHeight();
        });

        runningLogPane.add(textPaneScroll);

        // 快捷菜单
        final WebPanel menuPane = new WebPanel(new BorderLayout(5, 5));
        menuPane.setUndecorated(false);
        menuPane.setRound(StyleConstants.largeRound);
        menuPane.setMargin(5);
        menuPane.setShadeWidth(5);

        final WebButton serverConfig = new WebButton("配置参数", e -> new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                showConfigPanel();
                return null;
            }
        }.execute());
        serverConfig.setMargin(5, 10, 5, 10);
        serverConfig.setRound(15);

        final WebButton dataManage = new WebButton("数据管理", e -> {
            DataManagePanel dataManagePanel = DataManagePanel.getInstance();
            dataManagePanel.pack();
            dataManagePanel.setLocationRelativeTo(null);
            dataManagePanel.setVisible(true);
        });
        dataManage.setMargin(5, 10, 5, 10);

//        final WebButton bossManage = new WebButton("BOSS管理", e -> showBossPanel());
//        bossManage.setMargin(5, 10, 5, 10);

        final WebButton delUserDataManage = new WebButton("数据库管理", e -> new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                ((WebButton) e.getSource()).setEnabled(false);
                DataBaseManagePanel dataBaseManagePanel = DataBaseManagePanel.getInstance();
                dataBaseManagePanel.pack();
                dataBaseManagePanel.setLocationRelativeTo(instance);
                dataBaseManagePanel.setVisible(true);
                ((WebButton) e.getSource()).setEnabled(true);
                return null;
            }
        }.execute());
        delUserDataManage.setMargin(5, 10, 5, 10);

        final ImageIcon start = loadIcon("start.png");
        final ImageIcon stop = loadIcon("stop.png");
        final WebButton startServer = new WebButton("启动服务端", start);
        final WebProgressOverlay progressOverlay = new WebProgressOverlay();
        progressOverlay.setConsumeEvents(false);
        startServer.setMargin(5, 10, 5, 10);
        startServer.setRound(15);
        progressOverlay.setComponent(startServer);
        progressOverlay.setOpaque(false);
        startServer.addActionListener(e -> {
            if (!testDatabaseConnection()) {
                return;
            }
            boolean showLoad = !progressOverlay.isShowLoad();
            if (showLoad) {
                startRunTime();
                start_thread.start();
            } else {
                final String input = WebOptionPane.showInputDialog(instance, "关闭倒计时(分钟)：", 0);
                if (input == null) {
                    return;
                }
                startServer.setEnabled(false);
                final int time = Integer.valueOf(StringUtil.isNumber(input) ? input : "0");
                final ShutdownServer si = ShutdownServer.getInstance();
                if (si == null) {
                    WebOptionPane.showMessageDialog(instance, "停止服务端发生错误，服务端似乎没有启动？\r\n\r\n请关闭服务端，确保进程内的java.exe和javaw.exe进程完全关闭，再启动服务端试试吧~", "错误", JOptionPane.ERROR_MESSAGE);
                    System.exit(0);
                }
                si.setTime(time);
                Thread stop_thread = new Thread(() -> shutdownServer = Timer.GuiTimer.getInstance().register(() -> {
                    ShutdownServer.getInstance().shutdown();
                    if (si.getTime() > 0) {
                        System.out.println("距离服务端完全关闭还剩 " + si.getTime() + " 分钟，已通知玩家，请耐心等待...");
                    } else {
                        shutdownServer.cancel(false);
                        startRunTime.cancel(false);
                    }
                    si.setTime(si.getTime() - 1);
                }, 60000));
                stop_thread.start();
                try {
                    stop_thread.join();
                } catch (InterruptedException e1) {
                    log.error("停止服务端失败", e);
                }
//                    start_thread.interrupt();
//                    try {
//                        start_thread.join();
//                    } catch (InterruptedException e1) {
//                        e1.printStackTrace();
//                    }
            }

            progressOverlay.setShowLoad(showLoad);
            startServer.setText(showLoad ? "停止服务端" : "启动服务端");
            startServer.setIcon(showLoad ? stop : start);
        });

        menuPane.add(new GroupPanel(false,
                        serverConfig,
                        new WebSeparator(false, true).setMargin(4, 0, 4, 0),
                        dataManage,
//                        bossManage,
                        delUserDataManage,
                        new WebSeparator(false, true).setMargin(4, 0, 0, 0)),
                BorderLayout.NORTH);
        menuPane.add(new GroupPanel(false, new WebSeparator(false, true).setMargin(4, 0, 4, 0), progressOverlay), BorderLayout.SOUTH);


        contentPane.add(runningLogPane, BorderLayout.CENTER);
        contentPane.add(menuPane, BorderLayout.EAST);

        // 设置默认焦点
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                startServer.requestFocus();
            }
        });

        return contentPane;
    }

    public synchronized ConfigPanel showConfigPanel() {
        ConfigPanel serverConfigFrame = new ConfigPanel(instance);
        serverConfigFrame.pack();
        serverConfigFrame.setLocationRelativeTo(instance);
        serverConfigFrame.setVisible(true);
        return serverConfigFrame;
    }

    private void showBossPanel() {
        new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                BossManagePanel bossManagePanel = new BossManagePanel(instance);
                bossManagePanel.pack();
                bossManagePanel.setLocationRelativeTo(instance);
                bossManagePanel.setVisible(true);
                return null;
            }
        }.execute();
    }


    private Component createOnlineStatus() {
        final GroupPanel groupPanel = new GroupPanel(5);
        groupPanel.setPainter(new TitledBorderPainter("在线人数"));
        groupPanel.setMargin(5);
        labels = new WebHotkeyLabel[ServerConfig.CHANNEL_PORTS + 1];
        for (int i = 0; i <= ServerConfig.CHANNEL_PORTS; i++) {
            final WebHotkeyLabel label = new WebHotkeyLabel("频道" + (i + 1) + " : 0");
            labels[i] = label;
            groupPanel.add(label);
        }
        return groupPanel;
    }

    public void setupOnlineStatus(final int channel) {
        final ChannelServer channelServer = ChannelServer.getInstance(channel);
        if (channelServer == null) {
            return;
        }
        final PlayerStorage.PlayerObservable playerObservable = channelServer.getPlayerStorage().getPlayerObservable();
        Observer observer = (o, arg) -> labels[channel - 1].setText("频道" + channel + " : " + playerObservable.getCount());
        playerObservable.addObserver(observer);
    }

    private Component createBroadCastMsg() {
        final WebPanel contentPanel = new WebPanel(new BorderLayout(5, 5));
        contentPanel.setPainter(new TitledBorderPainter("系统公告"));
        contentPanel.setMargin(5);

        String[] items = {"顶部黄色公告", "信息提示框", "蓝色公告", "红色公告", "白色公告"};

        final WebComboBox comboBox = new WebComboBox(items);
        contentPanel.add(comboBox, BorderLayout.WEST);

        final WebTextField textField = new WebTextField(ServerConfig.LOGIN_SERVERMESSAGE);
        textField.setInputPrompt("点击此处输入您要发布的消息内容...");
        textField.setHideInputPromptOnFocus(false);
        contentPanel.add(textField, BorderLayout.CENTER);

        comboBox.addItemListener(e -> {
            if (e.getItem().equals("顶部黄色公告")) {
                textField.setText(ServerConfig.LOGIN_SERVERMESSAGE);
            } else {
                textField.setText("");
            }
        });

        final WebButton send = new WebButton("发送消息", e -> new SwingWorker() {
            @Override
            protected Object doInBackground() throws Exception {
                if (!startFinish) {
                    WebOptionPane.showMessageDialog(instance, "服务端暂未启动，无法使用该功能");
                    return null;
                }
                String msg = textField.getText();
                byte[] packet = new byte[0];
                switch (comboBox.getSelectedIndex()) {
                    case 0:
                        ServerConfig.LOGIN_SERVERMESSAGE = msg;
                        Config.setProperty("login.server.message", msg);
                        packet = MaplePacketCreator.serverMessage(msg);
                        break;
                    case 1:
                        packet = MaplePacketCreator.serverNotice(1, msg);
                        break;
                    case 2:
                        packet = MaplePacketCreator.serverNotice(6, msg);
                        break;
                    case 3:
                        packet = MaplePacketCreator.serverNotice(5, msg);
                        break;
                    case 4:
                        packet = MaplePacketCreator.spouseMessage(0x0A, msg);
                        break;
                }
                WorldBroadcastService.getInstance().broadcastMessage(packet);
                WebOptionPane.showMessageDialog(instance, "发送完成");
                return null;
            }
        }.execute());
        contentPanel.add(send, BorderLayout.EAST);

        return contentPanel;
    }

    private Component createStatusBar() {
        final WebPanel contentPane = new WebPanel(new BorderLayout(5, 5));
        final WebStatusBar statusBar = new WebStatusBar();



        runningTimelabel = new WebHotkeyLabel("运行时长: 00天00:00:00");
        statusBar.addToEnd(runningTimelabel);
        statusBar.addSeparatorToEnd();


        WebMemoryBar memoryBar = new WebMemoryBar();
        memoryBar.setShowMaximumMemory(false);
        memoryBar.setPreferredWidth(memoryBar.getPreferredSize().width + 20);
        statusBar.addToEnd(memoryBar);

        contentPane.add(createBroadCastMsg(), BorderLayout.NORTH);
        contentPane.add(createOnlineStatus(), BorderLayout.CENTER);
        contentPane.add(statusBar, BorderLayout.SOUTH);

        return contentPane;
    }

    private void startRunTime() {
        starttime = System.currentTimeMillis();
        startRunTime = Timer.GuiTimer.getInstance().register(new Runnable() {
            @Override
            public void run() {
                runningTimelabel.setText(formatDuring(System.currentTimeMillis() - starttime));
            }

            private String formatDuring(long mss) {
                long days = mss / (1000 * 60 * 60 * 24);
                long hours = (mss % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
                long minutes = (mss % (1000 * 60 * 60)) / (1000 * 60);
                long seconds = (mss % (1000 * 60)) / 1000;
                return "运行时长: " + (days / 10 == 0 ? "0" : "") + days + "天" + (hours / 10 == 0 ? "0" : "") + hours + ":" + (minutes / 10 == 0 ? "0" : "") + minutes + ":"
                        + (seconds / 10 == 0 ? "0" : "") + seconds;
            }
        }, 1000);
    }

    private static class StartThread implements Runnable {

        @Override
        public void run() {
            try {
                System.out.println("准备启动服务端...");
                if (!InitializeServer.Initial()) {
                    System.out.println("服务端初始化失败。");
                    return;
                }

                System.out.println("正在启动 - 时钟管理器");
                Timer.WorldTimer.getInstance().start();
                Timer.EtcTimer.getInstance().start();
                Timer.MapTimer.getInstance().start();
                Timer.CloneTimer.getInstance().start();
                Timer.EventTimer.getInstance().start();
                Timer.BuffTimer.getInstance().start();
                Timer.PingTimer.getInstance().start();
                Timer.PlayerTimer.getInstance().start();

                System.out.println("正在启动 - 好友、组队、家族、联盟、角色管理");
                World.init();
                MapleGuildRanking.getInstance().load();
                MapleGuild.loadAll();
                MapleFamily.loadAll();
                MapleQuest.initQuests();

                System.out.println("正在加载 - 道具信息");
                MapleItemInformationProvider.getInstance().runEtc();

//                System.out.println("正在加载 - 技能信息");
//                SkillFactory.loadAllSkills();

                System.out.println("正在加载 - 初始角色信息");
                LoginInformationProvider.getInstance();

                System.out.println("正在加载 - 随机奖励");
                RandomRewards.load();

                System.out.println("正在加载 - 角色卡系统");
                CharacterCardFactory.getInstance().initialize();

                System.out.println("正在加载 - 副本竞速排行榜");
                SpeedRunner.loadSpeedRuns();

                System.out.println("正在加载 - 拍卖行系统");
                MTSStorage.load();

                LoginServer.run_startup_configurations();
                ChannelServer.startChannel_Main();
                CashShopServer.run_startup_configurations();
                AuctionServer.run_startup_configurations();
                ChatServer.run_startup_configurations();

                System.out.println("正在加载 - 其他信息");
                Timer.CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
                WorldRespawnService.getInstance();
                ShutdownServer.registerMBean();
                LoginServer.setOn();
                PredictCardFactory.getInstance().initialize();
                MapleInventoryIdentifier.getInstance();
                PlayerNPC.loadAll();
                MapleDojoRanking.getInstance().load(false);
                RankingWorker.start();
                PlayMSEvent.start();
                MessengerRankingWorker.getInstance();
                MapleSignin.getInstance().load();
//                server.Start.checkCopyItemFromSql();
//                clearOnlineTime();
                RankingTop.getInstance();
                DataBaseManagePanel.getInstance().autoBackup();
                OpcodeConfig.load();
                MapleCarnivalFactory.getInstance();
                ItemConstants.TapJoyReward.init();
                Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
//                ServerNotice.start();
                System.err.println("服务端启动完成！");
                log.info("服务端启动完成！");
                startFinish = true;
            } catch (Exception e) {
                System.err.println("服务端启动失败！");
                log.fatal("服务端启动失败", e);
            }
        }
    }

    static class NewOutputStram extends OutputStream {

        private final byte type;

        public NewOutputStram(byte type) {
            this.type = type;
        }

        @Override
        public void write(int b) throws IOException {

        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
//            super.write(b, off, len);
            final SimpleAttributeSet set = new SimpleAttributeSet();
            switch (type) {
                case 0:
                    javax.swing.text.StyleConstants.setForeground(set, Color.BLACK);
                    break;
                case 1:
                    javax.swing.text.StyleConstants.setForeground(set, Color.RED);
                    break;
                case 2:
                    javax.swing.text.StyleConstants.setForeground(set, Color.BLUE);
                    break;
            }

            try {
                WebTextPane textPane = Start.getInstance().textPane;

                textPane.getDocument().insertString(textPane.getDocument().getLength(), new String(b, off, len), set);
                if (Start.getInstance().autoScroll) {
                    textPane.setCaretPosition(textPane.getDocument().getLength());
                }
            } catch (BadLocationException e) {
                log.fatal("控制台输出失败", e);
            }
        }
    }

    private static class Shutdown implements Runnable {

        @Override
        public void run() {
            ShutdownServer.getInstance().run();
        }
    }

    public class StartFrame extends WebFrame {

        private final WebLabel titleText;
        private final WebProgressBar progressBar;
        private final ImageIcon background;

        {
            background = loadIcon("LOGO.png");
        }

        StartFrame() {
            super();

            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setPreferredSize(new Dimension(background.getIconWidth(), background.getIconHeight()));

            BackgroundPanel backgroundPanel = new BackgroundPanel();

            backgroundPanel.setLayout(new BorderLayout());

            titleText = new WebLabel("正在启动……", SwingConstants.CENTER).setMargin(0, 0, 3, 0);
            titleText.setForeground(Color.WHITE);
            titleText.setFont(new FontUIResource("微软雅黑", 0, 12));
            progressBar = new WebProgressBar(0, 100);
            progressBar.setPreferredHeight(5);
            progressBar.setStringPainted(false);
            progressBar.setRound(0);
            progressBar.setValue(0);
            progressBar.setShadeWidth(0);
            progressBar.setBorderPainted(false);
            progressBar.setProgressBottomColor(Color.CYAN);
            progressBar.setProgressTopColor(Color.BLACK);

            backgroundPanel.add(new GroupPanel(false, titleText, progressBar), BorderLayout.SOUTH);

            add(backgroundPanel);
        }

        public void setText(String text) {
            this.titleText.setText(text);
        }

        public void setProgress(int value) {
            this.progressBar.setValue(value);
        }

        private class BackgroundPanel extends WebPanel {
            @Override
            public void paintComponent(Graphics g) {
                g.drawImage(background.getImage(), 0, 0, null);
            }
        }
    }

    public class ProgressBarObservable extends Observable {
        private String text;
        private int progress;

        public int getProgress() {
            return progress;
        }

        public void setProgress(Pair<String, Integer> value) {
            this.text = value.getLeft();
            this.progress = value.getRight();
            setChanged();
            notifyObservers(value);
        }
    }

    private class ProgressBarObserver implements Observer {
        ProgressBarObserver(ProgressBarObservable progressBarObservable) {
            progressBarObservable.addObserver(this);
        }

        public void deleteObserver(ProgressBarObservable progressBarObservable) {
            progressBarObservable.deleteObserver(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg instanceof Pair) {
                Pair pair = (Pair) arg;
                progress.setText((String) pair.getLeft());
                progress.setProgress((Integer) pair.getRight());
            }
        }
    }
}
