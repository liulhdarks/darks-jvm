package java.lang.management;

public interface ThreadMXBean
{
    int getThreadCount();

    int getPeakThreadCount();

    long getTotalStartedThreadCount();

    long[] getAllThreadIds();

    ThreadInfo getThreadInfo(long id);

    ThreadInfo getThreadInfo(long id, int maxDepth);

    boolean isThreadCpuTimeSupported();

    boolean isCurrentThreadCpuTimeSupported();

    long getCurrentThreadCpuTime();

    long getThreadCpuTime(long id);
}
