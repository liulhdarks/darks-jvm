package java.lang.management;

public interface MemoryManagerMXBean extends PlatformManagedObject
{
    String getName();

    String[] getMemoryPoolNames();

    boolean isValid();
}
