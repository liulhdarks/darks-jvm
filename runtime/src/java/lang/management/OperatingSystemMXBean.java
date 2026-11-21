package java.lang.management;

public interface OperatingSystemMXBean
{
    String getName();

    String getArch();

    String getVersion();

    int getAvailableProcessors();

    double getSystemLoadAverage();
}
