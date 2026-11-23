package java.lang.management;

public interface ThreadMXBean
{
    int getThreadCount();

    int getDaemonThreadCount();

    int getPeakThreadCount();

    long getTotalStartedThreadCount();

    long[] getAllThreadIds();

    ThreadInfo getThreadInfo(long id);

    ThreadInfo getThreadInfo(long id, int maxDepth);

    ThreadInfo[] getThreadInfo(long[] ids);

    ThreadInfo[] getThreadInfo(long[] ids, int maxDepth);

    ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
                               boolean lockedSynchronizers);

    long[] findDeadlockedThreads();

    boolean isThreadContentionMonitoringSupported();

    boolean isThreadContentionMonitoringEnabled();

    boolean isThreadCpuTimeSupported();

    boolean isCurrentThreadCpuTimeSupported();

    boolean isThreadCpuTimeEnabled();

    long getCurrentThreadCpuTime();

    long getThreadCpuTime(long id);
}
