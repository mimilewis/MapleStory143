/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.shops;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Emy
 */
public class HiredMerchantSave {

    public static final int NumSavingThreads = 5;
    private static final TimingThread[] Threads = new TimingThread[NumSavingThreads];
    private static final AtomicInteger Distribute = new AtomicInteger(0);

    static {
        for (int i = 0; i < Threads.length; i++) {
            Threads[i] = new TimingThread(new HiredMerchantSaveRunnable());
        }
    }

    public static void QueueShopForSave(HiredMerchant hm) {
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

    private static class HiredMerchantSaveRunnable implements Runnable {

        private static final AtomicInteger RunningThreadID = new AtomicInteger(0);
        private final int ThreadID = RunningThreadID.incrementAndGet();
        private final ArrayBlockingQueue<HiredMerchant> Queue = new ArrayBlockingQueue<>(500); //500 Start Capacity (Should be plenty)
        private long TimeTaken = 0;
        private int ShopsSaved = 0;
        private Object ToNotify;

        @Override
        public void run() {
            try {
                while (!Queue.isEmpty()) {
                    HiredMerchant next = Queue.take();
                    long Start = System.currentTimeMillis();
                    if (next.getMCOwner() != null && next.getMCOwner().getPlayerShop() == next) {
                        next.getMCOwner().setPlayerShop(null);
                    }
                    next.closeShop(true, false);
                    TimeTaken += (System.currentTimeMillis() - Start);
                    ShopsSaved++;
                }
                System.out.println("[保存雇佣商店数据 线程 " + ThreadID + "] 共保存: " + ShopsSaved + " | 耗时: " + TimeTaken + " 毫秒.");
                synchronized (ToNotify) {
                    ToNotify.notify();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(HiredMerchantSave.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void Queue(HiredMerchant hm) {
            Queue.add(hm);
        }

        private void SetToNotify(Object o) {
            if (ToNotify == null) {
                ToNotify = o;
            }
        }
    }

    private static class TimingThread extends Thread {

        private final HiredMerchantSaveRunnable ext;

        public TimingThread(HiredMerchantSaveRunnable r) {
            super(r);
            ext = r;
        }

        public HiredMerchantSaveRunnable getRunnable() {
            return ext;
        }
    }
}
