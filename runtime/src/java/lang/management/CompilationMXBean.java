package java.lang.management;

public interface CompilationMXBean extends PlatformManagedObject
{
    String getName();

    boolean isCompilationTimeMonitoringSupported();

    long getTotalCompilationTime();
}
