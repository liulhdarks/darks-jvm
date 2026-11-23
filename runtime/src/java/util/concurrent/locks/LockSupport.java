package java.util.concurrent.locks;

import java.util.HashMap;

/**
 * Permit-based park/unpark built on monitors. The VM has no futex-style
 * primitive exposed to Java, so a shared monitor plus a per-thread permit map
 * provides the JDK contract (unpark before park returns immediately).
 */
public class LockSupport
{
    private LockSupport()
    {
    }

    private static final Object LOCK = new Object();
    private static final HashMap<Thread, Boolean> PERMITS = new HashMap<Thread, Boolean>();
    private static final HashMap<Thread, Object> BLOCKERS = new HashMap<Thread, Object>();

    private static boolean takePermit(Thread thread)
    {
        return PERMITS.remove(thread) != null;
    }

    public static void park()
    {
        park(null);
    }

    public static void park(Object blocker)
    {
        Thread thread = Thread.currentThread();
        setBlocker(thread, blocker);
        synchronized (LOCK)
        {
            while (!takePermit(thread))
            {
                try
                {
                    LOCK.wait();
                }
                catch (InterruptedException e)
                {
                    thread.interrupt();
                    break;
                }
            }
        }
        setBlocker(thread, null);
    }

    public static void parkNanos(long nanos)
    {
        parkNanos(null, nanos);
    }

    public static void parkNanos(Object blocker, long nanos)
    {
        if (nanos <= 0L)
        {
            return;
        }
        Thread thread = Thread.currentThread();
        setBlocker(thread, blocker);
        long millis = nanos / 1000000L;
        int extraNanos = (int)(nanos % 1000000L);
        synchronized (LOCK)
        {
            if (!takePermit(thread))
            {
                try
                {
                    LOCK.wait(millis, extraNanos);
                }
                catch (InterruptedException e)
                {
                    thread.interrupt();
                }
                takePermit(thread);
            }
        }
        setBlocker(thread, null);
    }

    public static void parkUntil(long deadline)
    {
        parkUntil(null, deadline);
    }

    public static void parkUntil(Object blocker, long deadline)
    {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0L)
        {
            return;
        }
        parkNanos(blocker, remaining * 1000000L);
    }

    public static void unpark(Thread thread)
    {
        if (thread == null)
        {
            return;
        }
        synchronized (LOCK)
        {
            PERMITS.put(thread, Boolean.TRUE);
            LOCK.notifyAll();
        }
    }

    public static Object getBlocker(Thread thread)
    {
        if (thread == null)
        {
            throw new NullPointerException("thread");
        }
        synchronized (LOCK)
        {
            return BLOCKERS.get(thread);
        }
    }

    public static void setCurrentBlocker(Object blocker)
    {
        setBlocker(Thread.currentThread(), blocker);
    }

    static void setBlocker(Thread thread, Object blocker)
    {
        synchronized (LOCK)
        {
            if (blocker == null)
            {
                BLOCKERS.remove(thread);
            }
            else
            {
                BLOCKERS.put(thread, blocker);
            }
        }
    }
}
