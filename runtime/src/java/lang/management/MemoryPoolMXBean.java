package java.lang.management;

public interface MemoryPoolMXBean extends PlatformManagedObject
{
    String getName();

    MemoryType getType();

    MemoryUsage getUsage();

    MemoryUsage getPeakUsage();

    MemoryUsage getCollectionUsage();

    boolean isValid();

    String[] getMemoryManagerNames();
}
