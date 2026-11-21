package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * A thread pool that can delay and periodically repeat tasks.
 *
 * One timer thread tracks the earliest deadline in a sorted task list and
 * dispatches due tasks to the underlying worker pool.  Periodic tasks are
 * re-inserted after their previous run finishes, so a slow periodic task
 * never overlaps itself.
 *
 * Simplifications: pending delayed tasks are discarded on shutdown (their
 * futures are cancelled), and shutdownNow returns only the worker queue.
 */
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor
    implements ScheduledExecutorService
{
    private static final class ScheduledTask<V> implements ScheduledFuture<V>, Runnable
    {
        private final ScheduledThreadPoolExecutor owner;
        private final Runnable runnable;
        private final Callable<V> callable;
        private long sequence;
        private final boolean periodic;
        private final boolean fixedRate;
        private long periodMillis;
        private long time;

        private boolean cancelled;
        private boolean done;
        private V result;
        private Throwable failure;
        private Thread runner;

        ScheduledTask(ScheduledThreadPoolExecutor owner, Runnable runnable,
                      Callable<V> callable, long time, long periodMillis,
                      boolean periodic, boolean fixedRate, long sequence)
        {
            this.owner = owner;
            this.runnable = runnable;
            this.callable = callable;
            this.time = time;
            this.periodMillis = periodMillis;
            this.periodic = periodic;
            this.fixedRate = fixedRate;
            this.sequence = sequence;
        }

        void computeNextTime(long now)
        {
            if (fixedRate)
            {
                time = time + periodMillis;
                if (time < now)
                {
                    time = now + periodMillis;
                }
            }
            else
            {
                time = now + periodMillis;
            }
        }

        boolean isPeriodic()
        {
            return periodic;
        }

        void fail(Throwable error)
        {
            synchronized (this)
            {
                failure = error;
                done = true;
                notifyAll();
            }
        }

        public void run()
        {
            synchronized (this)
            {
                if (cancelled || done)
                {
                    return;
                }
                runner = Thread.currentThread();
            }
            V value = null;
            Throwable error = null;
            try
            {
                if (callable != null)
                {
                    value = callable.call();
                }
                else if (runnable != null)
                {
                    runnable.run();
                }
            }
            catch (Throwable failure)
            {
                error = failure;
            }
            boolean reschedule;
            synchronized (this)
            {
                runner = null;
                if (error != null)
                {
                    failure = error;
                    done = true;
                    cancelled = periodic && !cancelled;
                    notifyAll();
                    reschedule = false;
                }
                else if (periodic)
                {
                    result = value;
                    reschedule = !cancelled;
                }
                else
                {
                    result = value;
                    done = true;
                    notifyAll();
                    reschedule = false;
                }
            }
            if (reschedule)
            {
                owner.rescheduleTask(this);
            }
        }

        public boolean cancel(boolean mayInterruptIfRunning)
        {
            Thread target;
            synchronized (this)
            {
                if (done || cancelled)
                {
                    return false;
                }
                cancelled = true;
                target = runner;
                notifyAll();
            }
            owner.removeTask(this);
            if (mayInterruptIfRunning && target != null)
            {
                target.interrupt();
            }
            return true;
        }

        public boolean isCancelled()
        {
            synchronized (this)
            {
                return cancelled;
            }
        }

        public boolean isDone()
        {
            synchronized (this)
            {
                return done || cancelled;
            }
        }

        public V get() throws InterruptedException, ExecutionException
        {
            synchronized (this)
            {
                while (!done && !cancelled)
                {
                    wait();
                }
                return report();
            }
        }

        public V get(long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException
        {
            long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
            synchronized (this)
            {
                while (!done && !cancelled)
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

        private V report() throws ExecutionException
        {
            if (cancelled)
            {
                throw new CancellationException("task was cancelled");
            }
            if (failure != null)
            {
                throw new ExecutionException(failure);
            }
            return result;
        }

        public long getDelay(TimeUnit unit)
        {
            long remaining = time - System.currentTimeMillis();
            if (remaining < 0)
            {
                remaining = 0;
            }
            return unit.convert(remaining, TimeUnit.MILLISECONDS);
        }

        public int compareTo(Delayed other)
        {
            if (other == this)
            {
                return 0;
            }
            ScheduledTask<?> task = (ScheduledTask<?>)other;
            if (time != task.time)
            {
                return time < task.time ? -1 : 1;
            }
            if (sequence == task.sequence)
            {
                return 0;
            }
            return sequence < task.sequence ? -1 : 1;
        }

        String describe()
        {
            return "ScheduledTask[time=" + time + ", periodic=" + periodic
                + ", cancelled=" + cancelled + "]";
        }
    }

    private final Object tasksLock = new Object();
    private final ArrayList<ScheduledTask<?>> tasks = new ArrayList<ScheduledTask<?>>();
    private Thread timerThread;
    private boolean stopped;
    private long taskSequence;

    private final Runnable timerLoop = new Runnable()
    {
        public void run()
        {
            timerMain();
        }
    };

    public ScheduledThreadPoolExecutor(int corePoolSize)
    {
        this(corePoolSize, Executors.defaultThreadFactory());
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory)
    {
        super(corePoolSize, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(), threadFactory,
              new RejectedExecutionHandler.AbortPolicy());
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler)
    {
        super(corePoolSize, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(), handler);
    }

    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                       RejectedExecutionHandler handler)
    {
        super(corePoolSize, Integer.MAX_VALUE, 0L, TimeUnit.MILLISECONDS,
              new LinkedBlockingQueue<Runnable>(), threadFactory, handler);
    }

    public void execute(Runnable command)
    {
        schedule(command, 0L, TimeUnit.NANOSECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        long time = System.currentTimeMillis() + unit.toMillis(delay);
        return addTask(new ScheduledTask<Object>(this, command, null, time, 0L,
                                                 false, false, 0L));
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit)
    {
        if (callable == null)
        {
            throw new NullPointerException("callable");
        }
        long time = System.currentTimeMillis() + unit.toMillis(delay);
        return addTask(new ScheduledTask<V>(this, null, callable, time, 0L,
                                            false, false, 0L));
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay,
                                                  long period, TimeUnit unit)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        long periodMillis = unit.toMillis(period);
        if (periodMillis <= 0)
        {
            throw new IllegalArgumentException("period <= 0");
        }
        long time = System.currentTimeMillis() + unit.toMillis(initialDelay);
        return addTask(new ScheduledTask<Object>(this, command, null, time, periodMillis,
                                                 true, true, 0L));
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
                                                     long delay, TimeUnit unit)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        long periodMillis = unit.toMillis(delay);
        if (periodMillis <= 0)
        {
            throw new IllegalArgumentException("delay <= 0");
        }
        long time = System.currentTimeMillis() + unit.toMillis(initialDelay);
        return addTask(new ScheduledTask<Object>(this, command, null, time, periodMillis,
                                                 true, false, 0L));
    }

    private <V> ScheduledTask<V> addTask(ScheduledTask<V> task)
    {
        synchronized (tasksLock)
        {
            if (stopped || isShutdown())
            {
                throw new RejectedExecutionException("executor shut down");
            }
            task.sequence = taskSequence++;
            insertSorted(task);
            tasksLock.notifyAll();
        }
        startTimer();
        return task;
    }

    private void insertSorted(ScheduledTask<?> task)
    {
        int index = tasks.size();
        while (index > 0 && tasks.get(index - 1).compareTo(task) > 0)
        {
            index--;
        }
        tasks.add(index, task);
    }

    private void startTimer()
    {
        synchronized (tasksLock)
        {
            if (timerThread == null && !stopped)
            {
                timerThread = getThreadFactory().newThread(timerLoop);
                timerThread.start();
            }
        }
    }

    private void timerMain()
    {
        while (true)
        {
            ScheduledTask<?> task;
            synchronized (tasksLock)
            {
                while (true)
                {
                    if (stopped)
                    {
                        return;
                    }
                    if (tasks.isEmpty())
                    {
                        try
                        {
                            tasksLock.wait();
                        }
                        catch (InterruptedException interrupted)
                        {
                            if (stopped)
                            {
                                return;
                            }
                        }
                        continue;
                    }
                    task = tasks.get(0);
                    if (task.isCancelled())
                    {
                        tasks.remove(0);
                        continue;
                    }
                    long delay = task.time - System.currentTimeMillis();
                    if (delay <= 0)
                    {
                        tasks.remove(0);
                        break;
                    }
                    try
                    {
                        tasksLock.wait(delay);
                    }
                    catch (InterruptedException interrupted)
                    {
                        if (stopped)
                        {
                            return;
                        }
                    }
                }
            }
            try
            {
                super.execute(task);
            }
            catch (RejectedExecutionException rejected)
            {
                task.fail(rejected);
            }
        }
    }

    void rescheduleTask(ScheduledTask<?> task)
    {
        synchronized (tasksLock)
        {
            if (stopped || task.isCancelled())
            {
                return;
            }
            task.computeNextTime(System.currentTimeMillis());
            insertSorted(task);
            tasksLock.notifyAll();
        }
    }

    void removeTask(ScheduledTask<?> task)
    {
        synchronized (tasksLock)
        {
            tasks.remove(task);
            tasksLock.notifyAll();
        }
    }

    private void stopTimer()
    {
        ArrayList<ScheduledTask<?>> pending;
        synchronized (tasksLock)
        {
            stopped = true;
            pending = new ArrayList<ScheduledTask<?>>(tasks);
            tasks.clear();
            if (timerThread != null)
            {
                timerThread.interrupt();
            }
            tasksLock.notifyAll();
        }
        for (int i = 0; i < pending.size(); i++)
        {
            pending.get(i).cancel(false);
        }
    }

    public void shutdown()
    {
        stopTimer();
        super.shutdown();
    }

    public List<Runnable> shutdownNow()
    {
        stopTimer();
        return super.shutdownNow();
    }

    /*
     * darks-jvm cannot resolve an interface method through a sub-interface when
     * the implementation is inherited from a superclass, so the inherited
     * ExecutorService methods are redeclared here and forwarded explicitly.
     */
    public boolean isShutdown()
    {
        return super.isShutdown();
    }

    public boolean isTerminated()
    {
        return super.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
        return super.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task)
    {
        return super.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result)
    {
        return super.submit(task, result);
    }

    public Future<?> submit(Runnable task)
    {
        return super.submit(task);
    }

    public void close()
    {
        shutdown();
    }

    public void setRemoveOnCancelPolicy(boolean value)
    {
    }

    public boolean getRemoveOnCancelPolicy()
    {
        return true;
    }

    public String toString()
    {
        return "ScheduledThreadPoolExecutor[" + super.toString() + "]";
    }
}
