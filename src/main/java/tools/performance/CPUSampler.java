package tools.performance;

import java.io.IOException;
import java.io.Writer;
import java.lang.Thread.State;
import java.util.*;
import java.util.Map.Entry;

public class CPUSampler {

    private static final CPUSampler instance = new CPUSampler();
    private final List<String> included = new LinkedList<>();
    private final Map<StackTrace, Integer> recorded = new HashMap<>();
    private long interval = 5;
    private SamplerThread sampler = null;
    private int totalSamples = 0;

    public static CPUSampler getInstance() {
        return instance;
    }

    public void setInterval(long millis) {
        interval = millis;
    }

    public void addIncluded(String include) {
        for (String alreadyIncluded : included) {
            if (include.startsWith(alreadyIncluded)) {
                return;
            }
        }
        included.add(include);
    }

    public void reset() {
        recorded.clear();
        totalSamples = 0;
    }

    public void start() {
        if (sampler == null) {
            sampler = new SamplerThread();
            sampler.start();
        }
    }

    public void stop() {
        if (sampler != null) {
            sampler.stop();
            sampler = null;
        }
    }

    public SampledStacktraces getTopConsumers() {
        List<StacktraceWithCount> ret = new ArrayList<>();
        Set<Entry<StackTrace, Integer>> entrySet = recorded.entrySet();
        for (Entry<StackTrace, Integer> entry : entrySet) {
            ret.add(new StacktraceWithCount(entry.getValue(), entry.getKey()));
        }
        Collections.sort(ret);
        return new SampledStacktraces(ret, totalSamples);
    }

    public void save(Writer writer, int minInvocations, int topMethods) throws IOException {
        SampledStacktraces topConsumers = getTopConsumers();
        StringBuilder builder = new StringBuilder(); // build our summary :o
        builder.append("Top Methods:\r\n");
        for (int i = 0; i < topMethods && i < topConsumers.getTopConsumers().size(); i++) {
            builder.append(topConsumers.getTopConsumers().get(i).toString(topConsumers.getTotalInvocations(), 1));
        }
        builder.append("\r\nStack Traces:\r\n");
        writer.write(builder.toString());
        writer.write(topConsumers.toString(minInvocations));
        writer.flush();
    }

    private void consumeStackTraces(Map<Thread, StackTraceElement[]> traces) {
        for (Entry<Thread, StackTraceElement[]> trace : traces.entrySet()) {
            int relevant = findRelevantElement(trace.getValue());
            if (relevant != -1) {
                StackTrace st = new StackTrace(trace.getValue(), relevant, trace.getKey().getState());
                Integer i = recorded.get(st);
                totalSamples++;
                if (i == null) {
                    recorded.put(st, 1);
                } else {
                    recorded.put(st, i + 1);
                }
            }
        }
    }

    private int findRelevantElement(StackTraceElement[] trace) {
        if (trace.length == 0) {
            return -1;
        } else if (included.isEmpty()) {
            return 0;
        }
        int firstIncluded = -1;
        for (String myIncluded : included) {
            for (int i = 0; i < trace.length; i++) {
                StackTraceElement ste = trace[i];
                if (ste.getClassName().startsWith(myIncluded)) {
                    if (i < firstIncluded || firstIncluded == -1) {
                        firstIncluded = i;
                        break;
                    }
                }
            }
        }
        if (firstIncluded >= 0 && trace[firstIncluded].getClassName().equals("tools.performance.CPUSampler$SamplerThread")) { // don't sample us
            return -1;
        }
        return firstIncluded;
    }

    private static class StackTrace {

        private final StackTraceElement[] trace;
        private final State state;

        public StackTrace(StackTraceElement[] trace, int startAt, State state) {
            this.state = state;
            if (startAt == 0) {
                this.trace = trace;
            } else {
                this.trace = new StackTraceElement[trace.length - startAt];
                System.arraycopy(trace, startAt, this.trace, 0, this.trace.length);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof StackTrace)) {
                return false;
            }
            StackTrace other = (StackTrace) obj;
            if (other.trace.length != trace.length) {
                return false;
            }
            if (!(other.state == this.state)) {
                return false;
            }
            for (int i = 0; i < trace.length; i++) {
                if (!trace[i].equals(other.trace[i])) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            int ret = 13 * trace.length + state.hashCode();
            for (StackTraceElement ste : trace) {
                ret ^= ste.hashCode();
            }
            return ret;
        }

        public StackTraceElement[] getTrace() {
            return trace;
        }

        @Override
        public String toString() {
            return toString(-1);
        }

        public String toString(int traceLength) {
            StringBuilder ret = new StringBuilder("State: ");
            ret.append(state.name());
            if (traceLength > 1) {
                ret.append("\r\n");
            } else {
                ret.append(" ");
            }
            int i = 0;
            for (StackTraceElement ste : trace) {
                i++;
                if (i > traceLength) {
                    break;
                }
                ret.append(ste.getClassName());
                ret.append("#");
                ret.append(ste.getMethodName());
                ret.append(" (Line: ");
                ret.append(ste.getLineNumber());
                ret.append(")\r\n");
            }
            return ret.toString();
        }
    }

    public static class StacktraceWithCount implements Comparable<StacktraceWithCount> {

        private final int count;
        private final StackTrace trace;

        public StacktraceWithCount(int count, StackTrace trace) {
            super();
            this.count = count;
            this.trace = trace;
        }

        public int getCount() {
            return count;
        }

        public StackTraceElement[] getTrace() {
            return trace.getTrace();
        }

        @Override
        public int compareTo(StacktraceWithCount o) {
            return -Integer.valueOf(count).compareTo(o.count);
        }

        @Override
        public boolean equals(Object oth) {
            if (!(oth instanceof StacktraceWithCount)) {
                return false;
            }
            final StacktraceWithCount o = (StacktraceWithCount) oth;
            return count == o.count;
        }

        @Override
        public String toString() {
            return count + " Sampled Invocations\r\n" + trace.toString();
        }

        private double getPercentage(int total) {
            return Math.round((((double) count) / total) * 10000.0) / 100.0;
        }

        public String toString(int totalInvoations, int traceLength) {
            return count + "/" + totalInvoations + " Sampled Invocations (" + getPercentage(totalInvoations) + "%) " + trace.toString(traceLength);
        }
    }

    public static class SampledStacktraces {

        final List<StacktraceWithCount> topConsumers;
        final int totalInvocations;

        public SampledStacktraces(List<StacktraceWithCount> topConsumers, int totalInvocations) {
            super();
            this.topConsumers = topConsumers;
            this.totalInvocations = totalInvocations;
        }

        public List<StacktraceWithCount> getTopConsumers() {
            return topConsumers;
        }

        public int getTotalInvocations() {
            return totalInvocations;
        }

        @Override
        public String toString() {
            return toString(0);
        }

        public String toString(int minInvocation) {
            StringBuilder ret = new StringBuilder();
            for (StacktraceWithCount swc : topConsumers) {
                if (swc.getCount() >= minInvocation) {
                    ret.append(swc.toString(totalInvocations, Integer.MAX_VALUE));
                    ret.append("\r\n");
                }
            }
            return ret.toString();
        }
    }

    private class SamplerThread implements Runnable {

        private boolean running = false;
        private boolean shouldRun = false;
        private Thread rthread;

        public void start() {
            if (!running) {
                shouldRun = true;
                rthread = new Thread(this, "CPU Sampling Thread");
                rthread.start();
                running = true;
            }
        }

        public void stop() {
            this.shouldRun = false;
            rthread.interrupt();
            try {
                rthread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (shouldRun) {
                consumeStackTraces(Thread.getAllStackTraces());
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}
