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
    private static final OperatingSystemMXBean OS = new OperatingSystemBean();
    private static final MemoryMXBean MEMORY = new MemoryBean();
    private static final ClassLoadingMXBean CLASS_LOADING = new ClassLoadingBean();
    private static final CompilationMXBean COMPILATION = new CompilationBean();

    private static abstract class PlatformBean implements PlatformManagedObject
    {
        public javax.management.ObjectName getObjectName()
        {
            return null;
        }
    }

    private static final class RuntimeBean extends PlatformBean
        implements RuntimeMXBean
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

        public boolean isBootClassPathSupported()
        {
            return true;
        }
    }

    private static final class ThreadsBean extends PlatformBean
        implements ThreadMXBean
    {
        public int getThreadCount()
        {
            return 1;
        }

        public int getDaemonThreadCount()
        {
            return 0;
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
            return getThreadInfo(id, 0);
        }

        public ThreadInfo getThreadInfo(long id, int maxDepth)
        {
            if (id != Thread.currentThread().getId())
            {
                return null;
            }
            return new ThreadInfo(id, Thread.currentThread().getName(),
                                  Thread.currentThread().getState());
        }

        private ThreadInfo[] threadInfos(long[] ids)
        {
            if (ids == null)
            {
                return null;
            }
            ThreadInfo[] result = new ThreadInfo[ids.length];
            for (int i = 0; i < ids.length; i++)
            {
                result[i] = getThreadInfo(ids[i]);
            }
            return result;
        }

        public ThreadInfo[] getThreadInfo(long[] ids)
        {
            return threadInfos(ids);
        }

        public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth)
        {
            return threadInfos(ids);
        }

        public ThreadInfo[] getThreadInfo(long[] ids, boolean lockedMonitors,
                                          boolean lockedSynchronizers)
        {
            return threadInfos(ids);
        }

        public long[] findDeadlockedThreads()
        {
            return null;
        }

        public boolean isThreadContentionMonitoringSupported()
        {
            return false;
        }

        public boolean isThreadContentionMonitoringEnabled()
        {
            return false;
        }

        public boolean isThreadCpuTimeSupported()
        {
            return false;
        }

        public boolean isCurrentThreadCpuTimeSupported()
        {
            return false;
        }

        public boolean isThreadCpuTimeEnabled()
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

    private static final class OperatingSystemBean extends PlatformBean
        implements OperatingSystemMXBean
    {
        public String getName()
        {
            return System.getProperty("os.name", "linux");
        }

        public String getArch()
        {
            return System.getProperty("os.arch", "unknown");
        }

        public String getVersion()
        {
            return System.getProperty("os.version", "");
        }

        public int getAvailableProcessors()
        {
            return Runtime.getRuntime().availableProcessors();
        }

        public double getSystemLoadAverage()
        {
            return -1.0;
        }
    }

    private static final class MemoryBean extends PlatformBean
        implements MemoryMXBean
    {
        public MemoryUsage getHeapMemoryUsage()
        {
            Runtime runtime = Runtime.getRuntime();
            long total = runtime.totalMemory();
            long free = runtime.freeMemory();
            return new MemoryUsage(-1L, total - free, total, runtime.maxMemory());
        }

        public MemoryUsage getNonHeapMemoryUsage()
        {
            return new MemoryUsage(-1L, 0L, 0L, -1L);
        }

        public int getObjectPendingFinalizationCount()
        {
            return 0;
        }

        public boolean isVerbose()
        {
            return false;
        }

        public void setVerbose(boolean value)
        {
        }

        public void gc()
        {
            System.gc();
        }
    }

    private static final class ClassLoadingBean extends PlatformBean
        implements ClassLoadingMXBean
    {
        public long getTotalLoadedClassCount()
        {
            return 0L;
        }

        public int getLoadedClassCount()
        {
            return 0;
        }

        public long getUnloadedClassCount()
        {
            return 0L;
        }

        public boolean isVerbose()
        {
            return false;
        }

        public void setVerbose(boolean value)
        {
        }
    }

    private static final class CompilationBean extends PlatformBean
        implements CompilationMXBean
    {
        public String getName()
        {
            return "darks-jvm";
        }

        public boolean isCompilationTimeMonitoringSupported()
        {
            return false;
        }

        public long getTotalCompilationTime()
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

    public static OperatingSystemMXBean getOperatingSystemMXBean()
    {
        return OS;
    }

    public static MemoryMXBean getMemoryMXBean()
    {
        return MEMORY;
    }

    public static ClassLoadingMXBean getClassLoadingMXBean()
    {
        return CLASS_LOADING;
    }

    public static CompilationMXBean getCompilationMXBean()
    {
        return COMPILATION;
    }

    public static List<MemoryPoolMXBean> getMemoryPoolMXBeans()
    {
        return new ArrayList<MemoryPoolMXBean>();
    }

    public static List<GarbageCollectorMXBean> getGarbageCollectorMXBeans()
    {
        return new ArrayList<GarbageCollectorMXBean>();
    }

    public static List<MemoryManagerMXBean> getMemoryManagerMXBeans()
    {
        return new ArrayList<MemoryManagerMXBean>();
    }

    public static <T extends PlatformManagedObject> T getPlatformMXBean(
        Class<T> mxbeanInterface)
    {
        if (mxbeanInterface == null)
        {
            throw new IllegalArgumentException("null mxbean interface");
        }
        if (RuntimeMXBean.class.equals(mxbeanInterface))
        {
            return (T)RUNTIME;
        }
        if (ThreadMXBean.class.equals(mxbeanInterface))
        {
            return (T)THREADS;
        }
        if (OperatingSystemMXBean.class.equals(mxbeanInterface))
        {
            return (T)OS;
        }
        if (MemoryMXBean.class.equals(mxbeanInterface))
        {
            return (T)MEMORY;
        }
        if (ClassLoadingMXBean.class.equals(mxbeanInterface))
        {
            return (T)CLASS_LOADING;
        }
        if (CompilationMXBean.class.equals(mxbeanInterface))
        {
            return (T)COMPILATION;
        }
        return null;
    }

    public static <T extends PlatformManagedObject> List<T> getPlatformMXBeans(
        Class<T> mxbeanInterface)
    {
        List<T> beans = new ArrayList<T>();
        T bean = getPlatformMXBean(mxbeanInterface);
        if (bean != null)
        {
            beans.add(bean);
        }
        return beans;
    }
}
