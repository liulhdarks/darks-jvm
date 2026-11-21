package java.lang.management;

public interface RuntimeMXBean
{
    String getName();

    String getVmName();

    String getVmVendor();

    String getVmVersion();

    String getSpecName();

    String getSpecVendor();

    String getSpecVersion();

    long getStartTime();

    long getUptime();

    java.util.List<String> getInputArguments();

    java.util.Map<String, String> getSystemProperties();

    String getClassPath();

    String getLibraryPath();

    String getBootClassPath();

    boolean isBootClassPathSupported();

    String getManagementSpecVersion();
}
