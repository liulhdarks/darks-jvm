package java.lang;

import java.util.concurrent.ThreadFactory;

/**
 * Java-level Thread API on top of the coroutine scheduler.
 *
 * Every Thread is a user-space coroutine (M:N scheduled).  The methods below
 * keep the standard Java signatures; primitives that need scheduler support
 * (start0, startVirtual0, sleep0, join0, interrupt0, currentThread0) are
 * native.
 *
 * Virtual threads and platform threads currently share the same coroutine
 * scheduler.  The distinction is kept in the Java object and in the scheduler
 * so that isVirtual()/diagnostics behave like the JDK API, while platform
 * threads can be bound to a dedicated carrier later without changing callers.
 *
 * Not provided: thread stacks/traces, timed join nanoseconds precision,
 * inheritable thread locals.
 */
public class Thread implements Runnable
{
    public interface UncaughtExceptionHandler
    {
        void uncaughtException(Thread t, Throwable e);
    }

    public interface Builder
    {
        Builder name(String name);

        Builder name(String prefix, long start);

        Builder inheritInheritableThreadLocals(boolean inherit);

        Builder uncaughtExceptionHandler(UncaughtExceptionHandler eh);

        Thread unstarted(Runnable task);

        Thread start(Runnable task);

        ThreadFactory factory();

        interface OfVirtual extends Builder
        {
            OfVirtual name(String name);

            OfVirtual name(String prefix, long start);

            OfVirtual inheritInheritableThreadLocals(boolean inherit);

            OfVirtual uncaughtExceptionHandler(UncaughtExceptionHandler eh);
        }

        interface OfPlatform extends Builder
        {
            OfPlatform name(String name);

            OfPlatform name(String prefix, long start);

            OfPlatform inheritInheritableThreadLocals(boolean inherit);

            OfPlatform uncaughtExceptionHandler(UncaughtExceptionHandler eh);

            OfPlatform daemon(boolean on);

            OfPlatform priority(int newPriority);

            OfPlatform stackSize(long size);
        }
    }

    public enum State
    {
        NEW,
        RUNNABLE,
        BLOCKED,
        WAITING,
        TIMED_WAITING,
        TERMINATED
    }

    public static final int MIN_PRIORITY = 1;
    public static final int NORM_PRIORITY = 5;
    public static final int MAX_PRIORITY = 10;

    private String name;
    private ThreadGroup group;
    private Runnable target;
    private boolean daemon;
    private boolean alive;
    private boolean started;
    private long tid;
    private int priority;
    private boolean virtual;
    private ClassLoader contextClassLoader;
    private UncaughtExceptionHandler uncaughtExceptionHandler;
    private int threadLocalRandomProbe;
    private static UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    // ThreadLocal storage: slots indexed by ThreadLocal.key.
    Object[] tls;

    private static long threadSeq;

    private static synchronized long nextThreadId()
    {
        threadSeq = threadSeq + 1;
        return threadSeq;
    }

    private static final ThreadGroup MAIN_GROUP = new ThreadGroup("main");

    private void init(Runnable target, String name)
    {
        this.target = target;
        this.name = name;
        this.daemon = false;
        this.alive = false;
        this.started = false;
        this.tid = nextThreadId();
        this.priority = 5;
        this.virtual = false;
        Thread parent = currentThread();
        if (parent != null)
        {
            this.tls = ThreadLocal.inherit(parent.tls);
        }
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

    public Thread(ThreadGroup group, Runnable target)
    {
        init(target, null);
        this.group = group;
    }

    public Thread(ThreadGroup group, String name)
    {
        init(null, name);
        this.group = group;
    }

    public Thread(ThreadGroup group, Runnable target, String name)
    {
        init(target, name);
        this.group = group;
    }

    public Thread(ThreadGroup group, Runnable target, String name, long stackSize)
    {
        init(target, name);
        this.group = group;
    }

    public final ThreadGroup getThreadGroup()
    {
        return group != null ? group : MAIN_GROUP;
    }

    private Thread(Runnable target, String name, boolean virtual, boolean daemon,
                   int priority, UncaughtExceptionHandler handler,
                   ClassLoader contextClassLoader)
    {
        init(target, name);
        this.virtual = virtual;
        this.daemon = daemon;
        this.priority = priority;
        this.uncaughtExceptionHandler = handler;
        this.contextClassLoader = contextClassLoader;
    }

    public static Builder.OfVirtual ofVirtual()
    {
        return new VirtualBuilder();
    }

    public static Builder.OfPlatform ofPlatform()
    {
        return new PlatformBuilder();
    }

    public static Thread startVirtualThread(Runnable task)
    {
        return ofVirtual().start(task);
    }

    public synchronized void start()
    {
        if (started)
        {
            throw new IllegalStateException("Thread already started");
        }
        started = true;
        if (virtual)
        {
            startVirtual0();
        }
        else
        {
            start0();
        }
    }

    private native void start0();

    private native void startVirtual0();

    public void run()
    {
        if (target == null)
        {
            return;
        }
        try
        {
            target.run();
        }
        catch (Throwable failure)
        {
            UncaughtExceptionHandler handler = getUncaughtExceptionHandler();
            if (handler == null)
            {
                throw failure;
            }
            handler.uncaughtException(this, failure);
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

    public State getState()
    {
        // The scheduler tracks liveness only; a live coroutine may be runnable,
        // waiting or blocked, so report the conservative RUNNABLE state.
        if (!started)
        {
            return State.NEW;
        }
        if (!alive)
        {
            return State.TERMINATED;
        }
        return State.RUNNABLE;
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

    public long threadId()
    {
        return tid;
    }

    public final boolean isVirtual()
    {
        return virtual;
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

    public ClassLoader getContextClassLoader()
    {
        if (contextClassLoader != null)
        {
            return contextClassLoader;
        }
        return ClassLoader.getSystemClassLoader();
    }

    public void setContextClassLoader(ClassLoader cl)
    {
        contextClassLoader = cl;
    }

    public UncaughtExceptionHandler getUncaughtExceptionHandler()
    {
        if (uncaughtExceptionHandler != null)
        {
            return uncaughtExceptionHandler;
        }
        return defaultUncaughtExceptionHandler;
    }

    public void setUncaughtExceptionHandler(UncaughtExceptionHandler handler)
    {
        uncaughtExceptionHandler = handler;
    }

    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler()
    {
        return defaultUncaughtExceptionHandler;
    }

    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler handler)
    {
        defaultUncaughtExceptionHandler = handler;
    }

    public static Thread currentThread()
    {
        return currentThread0();
    }

    private static native Thread currentThread0();

    public String toString()
    {
        return (virtual ? "VirtualThread[" : "Thread[") + getName() + "]";
    }

    private static class VirtualBuilder implements Builder.OfVirtual
    {
        private String name;
        private UncaughtExceptionHandler handler;

        public OfVirtual name(String value)
        {
            name = value;
            return this;
        }

        public OfVirtual name(String prefix, long start)
        {
            name = prefix + start;
            return this;
        }

        public OfVirtual inheritInheritableThreadLocals(boolean inherit)
        {
            return this;
        }

        public OfVirtual uncaughtExceptionHandler(UncaughtExceptionHandler eh)
        {
            handler = eh;
            return this;
        }

        public Thread unstarted(Runnable task)
        {
            if (task == null)
            {
                throw new NullPointerException("task");
            }
            return new Thread(task, name, true, true, 5, handler, null);
        }

        public Thread start(Runnable task)
        {
            Thread thread = unstarted(task);
            thread.start();
            return thread;
        }

        public ThreadFactory factory()
        {
            final VirtualBuilder builder = this;
            return new ThreadFactory()
            {
                public Thread newThread(Runnable task)
                {
                    return builder.unstarted(task);
                }
            };
        }
    }

    private static class PlatformBuilder implements Builder.OfPlatform
    {
        private String name;
        private boolean daemon;
        private int priority = 5;
        private UncaughtExceptionHandler handler;

        public OfPlatform name(String value)
        {
            name = value;
            return this;
        }

        public OfPlatform name(String prefix, long start)
        {
            name = prefix + start;
            return this;
        }

        public OfPlatform inheritInheritableThreadLocals(boolean inherit)
        {
            return this;
        }

        public OfPlatform uncaughtExceptionHandler(UncaughtExceptionHandler eh)
        {
            handler = eh;
            return this;
        }

        public OfPlatform daemon(boolean on)
        {
            daemon = on;
            return this;
        }

        public OfPlatform priority(int newPriority)
        {
            if (newPriority < 1 || newPriority > 10)
            {
                throw new IllegalArgumentException("priority out of range");
            }
            priority = newPriority;
            return this;
        }

        public OfPlatform stackSize(long size)
        {
            return this;
        }

        public Thread unstarted(Runnable task)
        {
            if (task == null)
            {
                throw new NullPointerException("task");
            }
            return new Thread(task, name, false, daemon, priority, handler, null);
        }

        public Thread start(Runnable task)
        {
            Thread thread = unstarted(task);
            thread.start();
            return thread;
        }

        public ThreadFactory factory()
        {
            final PlatformBuilder builder = this;
            return new ThreadFactory()
            {
                public Thread newThread(Runnable task)
                {
                    return builder.unstarted(task);
                }
            };
        }
    }
}
