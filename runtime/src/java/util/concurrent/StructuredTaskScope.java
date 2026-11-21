package java.util.concurrent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * StructuredTaskScope (JEP 453 subset) on top of the coroutine scheduler.
 *
 * A scope is confined to the thread that creates it.  {@link #fork(Callable)}
 * starts a virtual thread as a child of the scope; {@link #join()} waits for
 * every child using the scheduler's regular Thread.join paths.  Closing the
 * scope interrupts unfinished children (through the scheduler's interrupt +
 * injected_exception mechanism), waits for them, and rejects any later fork.
 *
 * The scheduler records each child's parent scope ({@code Coroutine::
 * scope_parent}) and prunes the registry as children terminate, so a closed
 * scope can be checked for leaked subtasks.
 */
public class StructuredTaskScope<T> implements AutoCloseable
{
    /**
     * A task forked into a scope.
     */
    public interface Subtask<T> extends Supplier<T>
    {
        enum State
        {
            UNAVAILABLE,
            SUCCESS,
            FAILED
        }

        /** Waits for the result; throws IllegalStateException when unfinished or failed. */
        T get();

        /** The exception of a failed subtask, or null. */
        Throwable exception();

        State state();

        Callable<? extends T> task();

        Thread thread();
    }

    private static final ThreadFactory DEFAULT_FACTORY = Thread.ofVirtual().factory();
    private static long scopeSeq;

    private final String name;
    private final Thread owner;
    private final ThreadFactory factory;
    private final Object lock = new Object();

    private final ArrayList<Thread> threads = new ArrayList<Thread>();
    private final ArrayList<Throwable> failures = new ArrayList<Throwable>();

    private boolean closed;
    private boolean shutdown;
    private boolean joined;
    private boolean timedOut;
    private Throwable firstFailure;
    private Object firstResult;
    private boolean hasResult;

    // Set by the policy subclasses.
    private boolean shutdownOnFailure;
    private boolean shutdownOnSuccess;

    public StructuredTaskScope()
    {
        this(null, null);
    }

    public StructuredTaskScope(String name, ThreadFactory factory)
    {
        if (factory == null)
        {
            factory = DEFAULT_FACTORY;
        }
        synchronized (StructuredTaskScope.class)
        {
            scopeSeq = scopeSeq + 1;
            this.name = name != null ? name : ("scope-" + scopeSeq);
        }
        this.factory = factory;
        this.owner = Thread.currentThread();
    }

    /**
     * Starts a child task.  Only the scope owner may fork, and only before the
     * scope is shut down, joined or closed.
     */
    public <U extends T> Subtask<U> fork(Callable<? extends U> task)
    {
        if (task == null)
        {
            throw new NullPointerException("task");
        }
        if (Thread.currentThread() != owner)
        {
            throw new IllegalStateException("fork must be called from the scope owner");
        }
        synchronized (lock)
        {
            if (closed)
            {
                throw new IllegalStateException("scope is closed");
            }
            if (shutdown)
            {
                throw new IllegalStateException("scope is shut down");
            }
            if (joined)
            {
                throw new IllegalStateException("scope has already been joined");
            }
            SubtaskImpl<U> subtask = new SubtaskImpl<U>(task);
            Thread thread = factory.newThread(subtask);
            subtask.thread = thread;
            threads.add(thread);
            startScoped0(thread, this);
            return subtask;
        }
    }

    /** Waits until every subtask has finished or the scope has shut down. */
    public void join() throws InterruptedException
    {
        ensureOwnedAndOpen();
        synchronized (lock)
        {
            joined = true;
        }
        try
        {
            joinAll(0);
        }
        catch (InterruptedException e)
        {
            synchronized (lock)
            {
                shutdownLocked();
            }
            throw e;
        }
    }

    /** Waits until every subtask finishes or the deadline elapses. */
    public void joinUntil(Instant deadline) throws InterruptedException, TimeoutException
    {
        if (deadline == null)
        {
            throw new NullPointerException("deadline");
        }
        ensureOwnedAndOpen();
        long deadlineMillis = deadline.toEpochMilli();
        synchronized (lock)
        {
            joined = true;
            if (deadlineMillis <= System.currentTimeMillis())
            {
                timedOut = true;
                shutdownLocked();
                throw new TimeoutException("deadline elapsed before join");
            }
        }
        try
        {
            joinAll(deadlineMillis);
        }
        catch (InterruptedException e)
        {
            synchronized (lock)
            {
                shutdownLocked();
            }
            throw e;
        }
        if (hasLiveSubtasks())
        {
            synchronized (lock)
            {
                timedOut = true;
                shutdownLocked();
            }
            throw new TimeoutException("deadline elapsed");
        }
    }

    /** Interrupts unfinished subtasks and prevents further forks. */
    public void shutdown()
    {
        ensureOwnedAndOpen();
        synchronized (lock)
        {
            shutdownLocked();
        }
    }

    /**
     * Closes the scope: interrupts unfinished children, waits for them, and
     * verifies that no child coroutine outlived the scope.
     */
    public void close()
    {
        synchronized (lock)
        {
            if (closed)
            {
                return;
            }
            if (Thread.currentThread() != owner)
            {
                throw new IllegalStateException("close must be called from the scope owner");
            }
            closed = true;
            shutdownLocked();
        }

        long timeout = 30000L;
        String configured = System.getProperty("darks.scope.closeTimeout");
        if (configured != null)
        {
            try
            {
                timeout = Long.parseLong(configured);
            }
            catch (NumberFormatException ignored)
            {
                timeout = 30000L;
            }
        }
        long deadline = System.currentTimeMillis() + timeout;
        boolean interrupted = false;
        ArrayList<Thread> snapshot;
        synchronized (lock)
        {
            snapshot = new ArrayList<Thread>(threads);
        }
        for (int i = 0; i < snapshot.size(); i++)
        {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0)
            {
                break;
            }
            try
            {
                snapshot.get(i).join(remaining);
            }
            catch (InterruptedException e)
            {
                interrupted = true;
            }
        }

        int live = closeScope0(this);
        if (interrupted)
        {
            Thread.currentThread().interrupt();
        }
        if (live != 0)
        {
            throw new IllegalStateException(
                "scope " + name + " closed with " + live + " live subtasks");
        }
    }

    public boolean isShutdown()
    {
        synchronized (lock)
        {
            return shutdown;
        }
    }

    /** Number of children that have not finished yet (leak diagnostics). */
    public int liveSubtasks()
    {
        return liveSubtasks0(this);
    }

    /** Parent scope of the calling coroutine, or null outside any scope. */
    public static Object currentScope()
    {
        return currentScope0();
    }

    protected boolean isTimedOut()
    {
        synchronized (lock)
        {
            return timedOut;
        }
    }

    private void ensureOwnedAndOpen()
    {
        synchronized (lock)
        {
            if (closed)
            {
                throw new IllegalStateException("scope is closed");
            }
            if (Thread.currentThread() != owner)
            {
                throw new IllegalStateException("operation must be called from the scope owner");
            }
        }
    }

    // All callers hold lock.
    private void shutdownLocked()
    {
        if (shutdown)
        {
            return;
        }
        shutdown = true;
        shutdownScope0(this);
    }

    private void joinAll(long deadlineMillis) throws InterruptedException
    {
        ArrayList<Thread> snapshot;
        synchronized (lock)
        {
            snapshot = new ArrayList<Thread>(threads);
        }
        for (int i = 0; i < snapshot.size(); i++)
        {
            if (deadlineMillis > 0)
            {
                long remaining = deadlineMillis - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return;
                }
                snapshot.get(i).join(remaining);
            }
            else
            {
                snapshot.get(i).join();
            }
        }
    }

    private boolean hasLiveSubtasks()
    {
        return liveSubtasks0(this) != 0;
    }

    // All callers hold lock.
    private void taskSucceeded(SubtaskImpl<?> subtask, Object value)
    {
        if (subtask.state == 1)
        {
            return;
        }
        subtask.result = value;
        subtask.state = 1;
        if (shutdownOnSuccess && !hasResult)
        {
            hasResult = true;
            firstResult = value;
            shutdownLocked();
        }
    }

    // All callers hold lock.
    private void taskFailed(SubtaskImpl<?> subtask, Throwable failure)
    {
        if (subtask.state != 0)
        {
            return;
        }
        subtask.exception = failure;
        subtask.state = 2;
        failures.add(failure);
        if (firstFailure == null)
        {
            firstFailure = failure;
        }
        if (shutdownOnFailure)
        {
            shutdownLocked();
        }
    }

    /**
     * Throws an ExecutionException when any subtask failed.  Sibling failures
     * (for example the InterruptedException raised in tasks cancelled by an
     * earlier failure) are attached as suppressed exceptions.
     */
    public void throwIfFailed() throws ExecutionException
    {
        Throwable first;
        ArrayList<Throwable> all;
        synchronized (lock)
        {
            first = firstFailure;
            all = new ArrayList<Throwable>(failures);
        }
        if (first == null)
        {
            return;
        }
        ExecutionException failure = new ExecutionException(first);
        for (int i = 0; i < all.size(); i++)
        {
            Throwable next = all.get(i);
            if (next != null && next != first)
            {
                failure.addSuppressed(next);
            }
        }
        throw failure;
    }

    protected Object firstResultOrThrow() throws ExecutionException, TimeoutException
    {
        Throwable first;
        Object result;
        boolean present;
        boolean expired;
        synchronized (lock)
        {
            result = firstResult;
            present = hasResult;
            first = firstFailure;
            expired = timedOut;
        }
        if (present)
        {
            return result;
        }
        if (first != null)
        {
            throw new ExecutionException(first);
        }
        if (expired)
        {
            throw new TimeoutException("deadline elapsed");
        }
        throw new ExecutionException("no subtask completed successfully");
    }

    private final class SubtaskImpl<U> implements Subtask<U>, Runnable
    {
        private final Callable<? extends U> task;
        private Thread thread;
        private Object result;
        private Throwable exception;
        // 0 unavailable, 1 success, 2 failed (also cancelled).
        private int state;

        SubtaskImpl(Callable<? extends U> task)
        {
            this.task = task;
        }

        public void run()
        {
            try
            {
                U value = task.call();
                synchronized (lock)
                {
                    taskSucceeded(this, value);
                }
            }
            catch (Throwable failure)
            {
                synchronized (lock)
                {
                    taskFailed(this, failure);
                }
            }
        }

        public U get()
        {
            synchronized (lock)
            {
                if (state == 0)
                {
                    throw new IllegalStateException("subtask has not completed");
                }
                if (state == 2)
                {
                    throw new IllegalStateException("subtask failed: " + exception);
                }
                return (U) result;
            }
        }

        public Throwable exception()
        {
            synchronized (lock)
            {
                return exception;
            }
        }

        public State state()
        {
            synchronized (lock)
            {
                if (state == 1)
                {
                    return State.SUCCESS;
                }
                if (state == 2)
                {
                    return State.FAILED;
                }
                return State.UNAVAILABLE;
            }
        }

        public Callable<? extends U> task()
        {
            return task;
        }

        public Thread thread()
        {
            return thread;
        }
    }

    /**
     * Shuts the scope down as soon as one subtask fails; the first failure is
     * rethrown by {@link #throwIfFailed()}.
     */
    public static final class ShutdownOnFailure extends StructuredTaskScope<Object>
    {
        public ShutdownOnFailure()
        {
            super();
            setShutdownOnFailure(true);
        }

        public ShutdownOnFailure(String name, ThreadFactory factory)
        {
            super(name, factory);
            setShutdownOnFailure(true);
        }
    }

    /**
     * Shuts the scope down as soon as one subtask succeeds;
     * {@link #result()} returns that first result.
     */
    public static final class ShutdownOnSuccess<T> extends StructuredTaskScope<T>
    {
        public ShutdownOnSuccess()
        {
            super();
            setShutdownOnSuccess(true);
        }

        public ShutdownOnSuccess(String name, ThreadFactory factory)
        {
            super(name, factory);
            setShutdownOnSuccess(true);
        }

        public T result() throws ExecutionException, TimeoutException
        {
            return (T) firstResultOrThrow();
        }
    }

    protected void setShutdownOnFailure(boolean value)
    {
        shutdownOnFailure = value;
    }

    protected void setShutdownOnSuccess(boolean value)
    {
        shutdownOnSuccess = value;
    }
    private static native void startScoped0(Thread thread, StructuredTaskScope<?> scope);

    private static native void shutdownScope0(StructuredTaskScope<?> scope);

    private static native int liveSubtasks0(StructuredTaskScope<?> scope);

    private static native int closeScope0(StructuredTaskScope<?> scope);

    private static native Object currentScope0();
}
