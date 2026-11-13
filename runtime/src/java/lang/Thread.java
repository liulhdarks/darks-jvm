package java.lang;

/**
 * Java-level Thread API on top of the coroutine scheduler.
 *
 * Every Thread is a user-space coroutine (M:N scheduled).  The methods below
 * keep the standard Java signatures; primitives that need scheduler support
 * (start0, sleep0, join0, interrupt0, currentThread0) are native.
 *
 * Not provided: ThreadGroup, thread priorities (stored only), Thread.State,
 * thread stacks/traces, timed join nanoseconds precision.
 */
public class Thread implements Runnable
{
    private String name;
    private Runnable target;
    private boolean daemon;
    private boolean alive;
    private boolean started;
    private long tid;
    private int priority;
    // ThreadLocal storage: slots indexed by ThreadLocal.key.
    Object[] tls;

    private static long threadSeq;

    private static synchronized long nextThreadId()
    {
        threadSeq = threadSeq + 1;
        return threadSeq;
    }

    private void init(Runnable target, String name)
    {
        this.target = target;
        this.name = name;
        this.daemon = false;
        this.alive = false;
        this.started = false;
        this.tid = nextThreadId();
        this.priority = 5;
    }

    public Thread()
    {
        init(null, null);
    }

    public Thread(Runnable target)
    {
        init(target, null);
    }

    public Thread(String name)
    {
        init(null, name);
    }

    public Thread(Runnable target, String name)
    {
        init(target, name);
    }

    public synchronized void start()
    {
        if (started)
        {
            throw new IllegalStateException("Thread already started");
        }
        started = true;
        start0();
    }

    private native void start0();

    public void run()
    {
        if (target != null)
        {
            target.run();
        }
    }

    public static void sleep(long millis) throws InterruptedException
    {
        sleep0(millis);
    }

    public static void sleep(long millis, int nanos) throws InterruptedException
    {
        // Nanoseconds are accepted but the scheduler has millisecond
        // granularity; a positive sub-millisecond request waits one tick.
        sleep0(millis == 0 && nanos > 0 ? 1 : millis);
    }

    private static native void sleep0(long millis) throws InterruptedException;

    public static void yield()
    {
        yield0();
    }

    private static native void yield0();

    public final void join() throws InterruptedException
    {
        join0(0);
    }

    public final void join(long millis) throws InterruptedException
    {
        join0(millis);
    }

    public final void join(long millis, int nanos) throws InterruptedException
    {
        join0(millis == 0 && nanos > 0 ? 1 : millis);
    }

    private native void join0(long millis) throws InterruptedException;

    public void interrupt()
    {
        interrupt0();
    }

    private native void interrupt0();

    public boolean isInterrupted()
    {
        return isInterrupted0(false);
    }

    private native boolean isInterrupted0(boolean clear);

    public static boolean interrupted()
    {
        return interrupted0();
    }

    private static native boolean interrupted0();

    public boolean isAlive()
    {
        return alive;
    }

    public final boolean isDaemon()
    {
        return daemon;
    }

    public final void setDaemon(boolean on)
    {
        if (started)
        {
            throw new IllegalStateException("Thread already started");
        }
        daemon = on;
    }

    public final String getName()
    {
        return name != null ? name : ("Thread-" + tid);
    }

    public final void setName(String newName)
    {
        if (newName == null)
        {
            throw new NullPointerException("name");
        }
        name = newName;
    }

    public final long getId()
    {
        return tid;
    }

    public final int getPriority()
    {
        return priority;
    }

    public final void setPriority(int newPriority)
    {
        if (newPriority < 1 || newPriority > 10)
        {
            throw new IllegalArgumentException("priority out of range");
        }
        priority = newPriority;
    }

    public static Thread currentThread()
    {
        return currentThread0();
    }

    private static native Thread currentThread0();

    public String toString()
    {
        return "Thread[" + getName() + "]";
    }
}
