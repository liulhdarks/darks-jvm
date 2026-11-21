package java.util.concurrent;

/**
 * A cancellable asynchronous computation.
 *
 * The implementation is intentionally simple: a task owns one monitor and
 * waiters block on it.  darks-jvm's Object.wait/notify are coroutine-aware, so
 * a blocked get() suspends the virtual thread instead of pinning an OS
 * thread.
 */
public class FutureTask<V> implements Future<V>, Runnable
{
    private static final int NEW = 0;
    private static final int RUNNING = 1;
    private static final int NORMAL = 2;
    private static final int EXCEPTIONAL = 3;
    private static final int CANCELLED = 4;

    private Callable<V> callable;
    private V result;
    private Throwable failure;
    private int state;
    private Thread runner;

    public FutureTask(Callable<V> callable)
    {
        if (callable == null)
        {
            throw new NullPointerException("callable");
        }
        this.callable = callable;
        this.state = NEW;
    }

    public FutureTask(Runnable runnable, V result)
    {
        if (runnable == null)
        {
            throw new NullPointerException("runnable");
        }
        this.callable = new RunnableAdapter<V>(runnable, result);
        this.state = NEW;
    }

    public boolean isCancelled()
    {
        synchronized (this)
        {
            return state == CANCELLED;
        }
    }

    public boolean isDone()
    {
        synchronized (this)
        {
            return state >= NORMAL;
        }
    }

    public boolean cancel(boolean mayInterruptIfRunning)
    {
        Thread target;
        synchronized (this)
        {
            if (state >= NORMAL)
            {
                return false;
            }
            state = CANCELLED;
            target = runner;
            notifyAll();
        }
        if (mayInterruptIfRunning && target != null)
        {
            target.interrupt();
        }
        return true;
    }

    public V get() throws InterruptedException, ExecutionException
    {
        synchronized (this)
        {
            while (state < NORMAL)
            {
                wait();
            }
            return report();
        }
    }

    public V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        synchronized (this)
        {
            while (state < NORMAL)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    throw new TimeoutException("task did not complete in time");
                }
                wait(remaining);
            }
            return report();
        }
    }

    public void run()
    {
        synchronized (this)
        {
            if (state != NEW)
            {
                return;
            }
            state = RUNNING;
            runner = Thread.currentThread();
        }

        V value = null;
        Throwable error = null;
        try
        {
            value = callable.call();
        }
        catch (Throwable failure)
        {
            error = failure;
        }

        synchronized (this)
        {
            runner = null;
            callable = null;
            if (state == RUNNING)
            {
                if (error != null)
                {
                    failure = error;
                    state = EXCEPTIONAL;
                }
                else
                {
                    result = value;
                    state = NORMAL;
                }
            }
            notifyAll();
        }
    }

    private V report() throws ExecutionException
    {
        if (state == CANCELLED)
        {
            throw new CancellationException("task was cancelled");
        }
        if (state == EXCEPTIONAL)
        {
            throw new ExecutionException(failure);
        }
        return result;
    }

    public String toString()
    {
        synchronized (this)
        {
            return "FutureTask[state=" + state + "]";
        }
    }

    private static class RunnableAdapter<V> implements Callable<V>
    {
        private final Runnable task;
        private final V value;

        RunnableAdapter(Runnable task, V value)
        {
            this.task = task;
            this.value = value;
        }

        public V call()
        {
            task.run();
            return value;
        }
    }
}
