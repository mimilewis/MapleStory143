package handling.world.guild;

import handling.world.WorldGuildService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class GuildLoad {

    public static final int NumSavingThreads = 6;
    private static final TimingThread[] Threads = new TimingThread[NumSavingThreads];
    private static final Logger log = LogManager.getLogger(GuildLoad.class);
    private static final AtomicInteger Distribute = new AtomicInteger(0);
    private static Map<Integer, Map<Integer, MapleBBSReply>> replies = null;

    static {
        for (int i = 0; i < Threads.length; i++) {
            Threads[i] = new TimingThread(new GuildLoadRunnable());
        }
    }

    public static void QueueGuildForLoad(int hm, Map<Integer, Map<Integer, MapleBBSReply>> replie) {
        int Current = Distribute.getAndIncrement() % NumSavingThreads;
        Threads[Current].getRunnable().Queue(hm);
        if (replies == null) {
            replies = replie;
        }
    }

    public static void Execute(Object ToNotify) {
        for (TimingThread Thread1 : Threads) {
            Thread1.getRunnable().SetToNotify(ToNotify);
        }
        for (TimingThread Thread : Threads) {
            Thread.start();
        }
    }

    private static class GuildLoadRunnable implements Runnable {

        private final ArrayBlockingQueue<Integer> Queue = new ArrayBlockingQueue<>(1000); //1000 Start Capacity (Should be plenty)
        private Object ToNotify;

        @Override
        public void run() {
            try {
                while (!Queue.isEmpty()) {
                    WorldGuildService.getInstance().addLoadedGuild(new MapleGuild(Queue.take(), replies));
                }
                synchronized (ToNotify) {
                    ToNotify.notify();
                }
            } catch (InterruptedException ex) {
                log.error("[GuildLoad] 加载家族信息出错." + ex);
            }
        }

        private void Queue(Integer hm) {
            Queue.add(hm);
        }

        private void SetToNotify(Object o) {
            if (ToNotify == null) {
                ToNotify = o;
            }
        }
    }

    private static class TimingThread extends Thread {

        private final GuildLoadRunnable ext;

        public TimingThread(GuildLoadRunnable r) {
            super(r);
            ext = r;
        }

        public GuildLoadRunnable getRunnable() {
            return ext;
        }
    }
}
