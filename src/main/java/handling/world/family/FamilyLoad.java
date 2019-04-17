package handling.world.family;

import handling.world.WorldFamilyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class FamilyLoad {

    public static final int NumSavingThreads = 8;
    private static final TimingThread[] Threads = new TimingThread[NumSavingThreads];
    private static final Logger log = LogManager.getLogger(FamilyLoad.class);
    private static final AtomicInteger Distribute = new AtomicInteger(0);

    static {
        for (int i = 0; i < Threads.length; i++) {
            Threads[i] = new TimingThread(new FamilyLoadRunnable());
        }
    }

    public static void QueueFamilyForLoad(int hm) {
        int Current = Distribute.getAndIncrement() % NumSavingThreads;
        Threads[Current].getRunnable().Queue(hm);
    }

    public static void Execute(Object ToNotify) {
        for (TimingThread Thread1 : Threads) {
            Thread1.getRunnable().SetToNotify(ToNotify);
        }
        for (TimingThread Thread : Threads) {
            Thread.start();
        }
    }

    private static class FamilyLoadRunnable implements Runnable {

        private final ArrayBlockingQueue<Integer> Queue = new ArrayBlockingQueue<>(1000);
        private Object ToNotify;

        @Override
        public void run() {
            try {
                while (!Queue.isEmpty()) {
                    WorldFamilyService.getInstance().addLoadedFamily(new MapleFamily(Queue.take()));
                }
                synchronized (ToNotify) {
                    ToNotify.notify();
                }
            } catch (InterruptedException ex) {
                log.error("[FamilyLoad] 加载学院信息出错." + ex);
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

        private final FamilyLoadRunnable ext;

        public TimingThread(FamilyLoadRunnable r) {
            super(r);
            ext = r;
        }

        public FamilyLoadRunnable getRunnable() {
            return ext;
        }
    }
}
