package java.lang;

/**
 * Minimal Runtime implementation.
 *
 * The JVM exposes a single process-wide instance, matching the Java API.  The
 * memory figures are process statistics from the operating system rather than
 * a managed-heap budget, which is enough for code that merely inspects them.
 */
public class Runtime
{
    private static final Runtime current = new Runtime();

    private Runtime()
    {
    }

    public static Runtime getRuntime()
    {
        return current;
    }

    public native int availableProcessors();

    public native long freeMemory();

    public native long totalMemory();

    public native long maxMemory();

    public native void gc();

    public void runFinalization()
    {
        // No finalization support.
    }

    public void exit(int status)
    {
        System.exit(status);
    }

    public void halt(int status)
    {
        System.exit(status);
    }

    private final java.util.ArrayList<Thread> shutdownHooks =
        new java.util.ArrayList<Thread>();

    public void addShutdownHook(Thread hook)
    {
        if (hook == null)
        {
            throw new NullPointerException("hook");
        }
        if (shutdownHooks.contains(hook))
        {
            throw new IllegalArgumentException("Hook already registered");
        }
        shutdownHooks.add(hook);
    }

    public boolean removeShutdownHook(Thread hook)
    {
        if (hook == null)
        {
            throw new NullPointerException("hook");
        }
        return shutdownHooks.remove(hook);
    }
}
