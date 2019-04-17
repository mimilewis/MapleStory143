package server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tools.Randomizer;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Timer {

    private static final AtomicInteger threadNumber = new AtomicInteger(1);
    protected String name;
    private ScheduledThreadPoolExecutor ses;

    public void start() {
        if (ses != null && !ses.isShutdown() && !ses.isTerminated()) {
            return;
        }
        ses = new ScheduledThreadPoolExecutor(5, new RejectedThreadFactory());
        ses.setKeepAliveTime(10, TimeUnit.MINUTES);
        ses.allowCoreThreadTimeOut(true);
        ses.setMaximumPoolSize(8);
        ses.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    public ScheduledThreadPoolExecutor getSES() {
        return ses;
    }

    public void stop() {
        if (ses != null) {
            ses.shutdown();
        }
    }

    /*
     * 周期执行
     */
    public ScheduledFuture<?> register(Runnable command, long period, long initialDelay) {
        if (ses == null) {
            return null;
        }
        /* *
         * public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit)
         * 从接口 ScheduledExecutorService 复制的描述创建并执行一个在给定初始延迟后首次启用的定期操作，后续操作具有给定的周期；
         * 也就是将在 initialDelay 后开始执行，然后在 initialDelay+period 后执行，接着在 initialDelay + 2 * period 后执行，依此类推。
         * 如果任务的任何一个执行遇到异常，则后续执行都会被取消。否则，只能通过执行程序的取消或终止方法来终止该任务。
         * 如果此任务的任何一个执行要花费比其周期更长的时间，则将推迟后续执行，但不会同时执行。
         * 指定者：
         * 接口 ScheduledExecutorService 中的 scheduleAtFixedRate
         * 参数：
         * command - 要执行的任务
         * initialDelay - 首次执行的延迟时间
         * period - 连续执行之间的周期
         * unit - initialDelay 和 period 参数的时间单位
         * 返回：
         * 表示挂起任务完成的 ScheduledFuture，并且其 get() 方法在取消后将抛出异常
         */
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(command), initialDelay, period, TimeUnit.MILLISECONDS);
    }

    /*
     * 周期执行
     */
    public ScheduledFuture<?> register(Runnable command, long period) {
        if (ses == null) {
            return null;
        }
        return ses.scheduleAtFixedRate(new LoggingSaveRunnable(command), 0, period, TimeUnit.MILLISECONDS);
    }

    /*
     * 执行1次
     */
    public ScheduledFuture<?> schedule(Runnable command, long delay) {
        if (ses == null) {
            return null;
        }
        /* *
         * public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
         * 从接口 ScheduledExecutorService 复制的描述创建并执行在给定延迟后启用的一次性操作。
         * 指定者：
         * 接口 ScheduledExecutorService 中的 schedule
         * 参数：
         * command - 要执行的任务
         * delay - 从现在开始延迟执行的时间
         * unit - 延迟参数的时间单位
         * 返回：
         * 表示挂起任务完成的 ScheduledFuture，并且其 get() 方法在完成后将返回 null
         */
        return ses.schedule(new LoggingSaveRunnable(command), delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> scheduleAtTimestamp(Runnable command, long timestamp) {
        return schedule(command, timestamp - System.currentTimeMillis());
    }

    public static class WorldTimer extends Timer {

        private static final WorldTimer instance = new WorldTimer();

        private WorldTimer() {
            name = "Worldtimer";
        }

        public static WorldTimer getInstance() {
            return instance;
        }
    }

    public static class MapTimer extends Timer {

        private static final MapTimer instance = new MapTimer();

        private MapTimer() {
            name = "Maptimer";
        }

        public static MapTimer getInstance() {
            return instance;
        }
    }

    public static class BuffTimer extends Timer {

        private static final BuffTimer instance = new BuffTimer();

        private BuffTimer() {
            name = "Bufftimer";
        }

        public static BuffTimer getInstance() {
            return instance;
        }
    }

    public static class EventTimer extends Timer {

        private static final EventTimer instance = new EventTimer();

        private EventTimer() {
            name = "Eventtimer";
        }

        public static EventTimer getInstance() {
            return instance;
        }
    }

    public static class CloneTimer extends Timer {

        private static final CloneTimer instance = new CloneTimer();

        private CloneTimer() {
            name = "Clonetimer";
        }

        public static CloneTimer getInstance() {
            return instance;
        }
    }

    public static class EtcTimer extends Timer {

        private static final EtcTimer instance = new EtcTimer();

        private EtcTimer() {
            name = "Etctimer";
        }

        public static EtcTimer getInstance() {
            return instance;
        }
    }

    public static class CheatTimer extends Timer {

        private static final CheatTimer instance = new CheatTimer();

        private CheatTimer() {
            name = "Cheattimer";
        }

        public static CheatTimer getInstance() {
            return instance;
        }
    }

    public static class PingTimer extends Timer {

        private static final PingTimer instance = new PingTimer();

        private PingTimer() {
            name = "Pingtimer";
        }

        public static PingTimer getInstance() {
            return instance;
        }
    }

    public static class GuiTimer extends Timer {

        private static final GuiTimer instance = new GuiTimer();

        private GuiTimer() {
            name = "GuiTimer";
        }

        public static GuiTimer getInstance() {
            return instance;
        }
    }

    public static class PlayerTimer extends Timer {

        private static final PlayerTimer instance = new PlayerTimer();

        private PlayerTimer() {
            name = "PlayerTimer";
        }

        public static PlayerTimer getInstance() {
            return instance;
        }
    }

    private static class LoggingSaveRunnable implements Runnable {

        private static final Logger logger = LogManager.getLogger(LoggingSaveRunnable.class.getName());
        final Runnable command;

        public LoggingSaveRunnable(Runnable r) {
            this.command = r;
        }

        @Override
        public void run() {
            try {
                command.run();
            } catch (Throwable t) {
                logger.error("", t); //写出执行定时任务的错误信息
            }
        }
    }

    private class RejectedThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber2 = new AtomicInteger(1);
        private final String tname;

        public RejectedThreadFactory() {
            tname = name + Randomizer.nextInt();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName(tname + "-W-" + threadNumber.getAndIncrement() + "-" + threadNumber2.getAndIncrement());
            return t;
        }
    }
}