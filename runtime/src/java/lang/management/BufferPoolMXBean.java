package java.lang.management;

public interface BufferPoolMXBean extends PlatformManagedObject
{
    String getName();

    long getCount();

    long getTotalCapacity();

    long getMemoryUsed();
}
