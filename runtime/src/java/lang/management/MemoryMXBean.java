package java.lang.management;

public interface MemoryMXBean extends PlatformManagedObject
{
    MemoryUsage getHeapMemoryUsage();

    MemoryUsage getNonHeapMemoryUsage();

    int getObjectPendingFinalizationCount();

    boolean isVerbose();

    void setVerbose(boolean value);

    void gc();
}
