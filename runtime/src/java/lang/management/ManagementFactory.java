package java.lang.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ManagementFactory
{
    private static final long START_TIME = System.currentTimeMillis();

    private ManagementFactory()
    {
    }

    public static RuntimeMXBean getRuntimeMXBean()
    {
        return RuntimeBean.INSTANCE;
    }

    public static OperatingSystemMXBean getOperatingSystemMXBean()
    {
        return OperatingSystemBean.INSTANCE;
    }

    private static final class RuntimeBean implements RuntimeMXBean
    {
        static final RuntimeBean INSTANCE = new RuntimeBean();

        public String getName()
        {
            String host = "localhost";
            try
            {
                host = java.net.InetAddress.getLocalHost().getHostName();
            }
            catch (Throwable ignored)
            {
            }
            return host + "@darks-jvm";
        }

        public String getVmName()
        {
            return "darks-jvm";
        }

        public String getVmVendor()
        {
            return "darks-jvm";
        }

        public String getVmVersion()
        {
            return System.getProperty("java.vm.version");
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
            return START_TIME;
        }

        public long getUptime()
        {
            return System.currentTimeMillis() - START_TIME;
        }

        public List<String> getInputArguments()
        {
            return new ArrayList<String>();
        }

        public Map<String, String> getSystemProperties()
        {
            Map<String, String> result = new HashMap<String, String>();
            Properties properties = System.getProperties();
            java.util.Iterator<Object> iterator = properties.keySet().iterator();
            while (iterator.hasNext())
            {
                Object key = iterator.next();
                if (key instanceof String)
                {
                    Object value = properties.get(key);
                    result.put((String)key, value == null ? null : value.toString());
                }
            }
            return result;
        }

        public String getClassPath()
        {
            String value = System.getProperty("java.class.path");
            return value == null ? "" : value;
        }

        public String getLibraryPath()
        {
            String value = System.getProperty("java.library.path");
            return value == null ? "" : value;
        }

        public String getBootClassPath()
        {
            throw new UnsupportedOperationException("boot class path is not supported");
        }

        public boolean isBootClassPathSupported()
        {
            return false;
        }

        public String getManagementSpecVersion()
        {
            return "1.4";
        }
    }

    private static final class OperatingSystemBean implements OperatingSystemMXBean
    {
        static final OperatingSystemBean INSTANCE = new OperatingSystemBean();

        public String getName()
        {
            String value = System.getProperty("os.name");
            return value == null ? "unknown" : value;
        }

        public String getArch()
        {
            String value = System.getProperty("os.arch");
            return value == null ? "unknown" : value;
        }

        public String getVersion()
        {
            String value = System.getProperty("os.version");
            return value == null ? "" : value;
        }

        public int getAvailableProcessors()
        {
            return Runtime.getRuntime().availableProcessors();
        }

        public double getSystemLoadAverage()
        {
            return -1.0d;
        }
    }
}
