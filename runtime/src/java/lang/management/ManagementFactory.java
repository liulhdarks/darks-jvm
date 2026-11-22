package java.lang.management;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ManagementFactory
{
    private static MBeanServer platformServer;
    private static final RuntimeMXBean RUNTIME = new RuntimeBean();
    private static final ThreadMXBean THREADS = new ThreadsBean();

    private static final class RuntimeBean implements RuntimeMXBean
    {
        private final long startTime = System.currentTimeMillis();

        public String getName()
        {
            return "darks-jvm@" + getPid();
        }

        public String getVmName()
        {
            return "darks-jvm";
        }

        public String getVmVendor()
        {
            return "darks";
        }

        public String getVmVersion()
        {
            return System.getProperty("java.version", "1.8.0");
        }

        public String getSpecName()
        {
            return "Java Virtual Machine Specification";
        }

        public String getSpecVendor()
        {
            return "Oracle Corporation";
        }

        public String getSpecVersion()
        {
            return "1.8";
        }

        public long getStartTime()
        {
            return startTime;
        }

        public long getUptime()
        {
            return System.currentTimeMillis() - startTime;
        }

        public long getPid()
        {
            return 1L;
        }

        public List<String> getInputArguments()
        {
            return new ArrayList<String>();
        }

        public Map<String, String> getSystemProperties()
        {
            return new HashMap<String, String>();
        }

        public String getClassPath()
        {
            return System.getProperty("java.class.path", "");
        }

        public String getBootClassPath()
        {
            return "";
        }

        public String getLibraryPath()
        {
            return System.getProperty("java.library.path", "");
        }

        public String getManagementSpecVersion()
        {
            return "1.2";
        }
    }

    private static final class ThreadsBean implements ThreadMXBean
    {
        public int getThreadCount()
        {
            return 1;
        }

        public int getPeakThreadCount()
        {
            return 1;
        }

        public long getTotalStartedThreadCount()
        {
            return 1L;
        }

        public long[] getAllThreadIds()
        {
            return new long[] { Thread.currentThread().getId() };
        }

        public ThreadInfo getThreadInfo(long id)
        {
            return new ThreadInfo(id, Thread.currentThread().getName(),
                                  Thread.currentThread().getState());
        }

        public ThreadInfo getThreadInfo(long id, int maxDepth)
        {
            return getThreadInfo(id);
        }

        public boolean isThreadCpuTimeSupported()
        {
            return false;
        }

        public boolean isCurrentThreadCpuTimeSupported()
        {
            return false;
        }

        public long getCurrentThreadCpuTime()
        {
            return -1L;
        }

        public long getThreadCpuTime(long id)
        {
            return -1L;
        }
    }

    private ManagementFactory()
    {
    }

    public static synchronized MBeanServer getPlatformMBeanServer()
    {
        if (platformServer == null)
        {
            platformServer = MBeanServerFactory.createMBeanServer("DefaultDomain");
        }
        return platformServer;
    }

    public static RuntimeMXBean getRuntimeMXBean()
    {
        return RUNTIME;
    }

    public static ThreadMXBean getThreadMXBean()
    {
        return THREADS;
    }
}
