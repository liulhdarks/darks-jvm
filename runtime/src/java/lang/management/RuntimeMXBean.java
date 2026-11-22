package java.lang.management;

import java.util.List;
import java.util.Map;

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

    long getPid();

    List<String> getInputArguments();

    Map<String, String> getSystemProperties();

    String getClassPath();

    String getBootClassPath();

    String getLibraryPath();

    String getManagementSpecVersion();
}
