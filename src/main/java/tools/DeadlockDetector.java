/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.management.*;

/**
 * @author PlayDK
 */
public class DeadlockDetector implements Runnable {

    private static final Logger log = LogManager.getLogger(DeadlockDetector.class);
    private static final String INDENT = "    ";
    private int checkInterval = 0;
    private StringBuilder sb = null;

    public DeadlockDetector(int checkInterval) {
        this.checkInterval = checkInterval * 1000;
    }

    @Override
    public void run() {
        boolean noDeadLocks = true;

        while (noDeadLocks) {
            try {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();
                long[] threadIds = bean.findDeadlockedThreads();

                if (threadIds != null) {
                    log.error("Deadlock detected!");
                    sb = new StringBuilder();
                    noDeadLocks = false;

                    ThreadInfo[] infos = bean.getThreadInfo(threadIds);
                    sb.append("\nTHREAD LOCK INFO: \n");
                    for (ThreadInfo threadInfo : infos) {
                        printThreadInfo(threadInfo);
                        LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
                        MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();

                        printLockInfo(lockInfos);
                        printMonitorInfo(threadInfo, monitorInfos);
                    }

                    sb.append("\nTHREAD DUMPS: \n");
                    for (ThreadInfo ti : bean.dumpAllThreads(true, true)) {
                        printThreadInfo(ti);
                    }
                    log.error(sb.toString());
                }
                Thread.sleep(checkInterval);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void printThreadInfo(ThreadInfo threadInfo) {
        printThread(threadInfo);
        sb.append(INDENT).append(threadInfo.toString()).append("\n");
        StackTraceElement[] stacktrace = threadInfo.getStackTrace();
        MonitorInfo[] monitors = threadInfo.getLockedMonitors();

        for (int i = 0; i < stacktrace.length; i++) {
            StackTraceElement ste = stacktrace[i];
            sb.append(INDENT).append("at ").append(ste.toString()).append("\n");
            for (MonitorInfo mi : monitors) {
                if (mi.getLockedStackDepth() == i) {
                    sb.append(INDENT).append("  - locked ").append(mi).append("\n");
                }
            }
        }
    }

    private void printThread(ThreadInfo ti) {
        sb.append("\nPrintThread\n");
        sb.append("\"").append(ti.getThreadName()).append("\"" + " Id=").append(ti.getThreadId()).append(" in ").append(ti.getThreadState()).append("\n");
        if (ti.getLockName() != null) {
            sb.append(" on lock=").append(ti.getLockName()).append("\n");
        }
        if (ti.isSuspended()) {
            sb.append(" (suspended)" + "\n");
        }
        if (ti.isInNative()) {
            sb.append(" (running in native)" + "\n");
        }
        if (ti.getLockOwnerName() != null) {
            sb.append(INDENT).append(" owned by ").append(ti.getLockOwnerName()).append(" Id=").append(ti.getLockOwnerId()).append("\n");
        }
    }

    private void printMonitorInfo(ThreadInfo threadInfo, MonitorInfo[] monitorInfos) {
        sb.append(INDENT).append("Locked monitors: count = ").append(monitorInfos.length).append("\n");
        for (MonitorInfo monitorInfo : monitorInfos) {
            sb.append(INDENT).append("  - ").append(monitorInfo).append(" locked at " + "\n");
            sb.append(INDENT).append("      ").append(monitorInfo.getLockedStackDepth()).append(" ").append(monitorInfo.getLockedStackFrame()).append("\n");
        }
    }

    private void printLockInfo(LockInfo[] lockInfos) {
        sb.append(INDENT).append("Locked synchronizers: count = ").append(lockInfos.length).append("\n");
        for (LockInfo lockInfo : lockInfos) {
            sb.append(INDENT).append("  - ").append(lockInfo).append("\n");
        }
    }
}