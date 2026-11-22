package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * A thread pool with a bounded or unbounded work queue.
 *
 * Workers are ordinary {@link Thread}s (coroutines in darks-jvm).  Idle
 * workers block in the work queue's take/poll, which the scheduler suspends
 * without occupying a carrier, so a large pool is cheap.
 *
 * Simplifications: there is no per-worker lock (an "idle" flag is used when
 * deciding which workers shutdown may interrupt) and no core-thread
 * prestart-on-demand beyond the execute path.
 */
public class ThreadPoolExecutor extends AbstractExecutorService
{
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int STOP = 2;

    private static final RejectedExecutionHandler DEFAULT_HANDLER =
        new AbortPolicy();

    public static class AbortPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            throw new RejectedExecutionException("task rejected from " + executor);
        }
    }

    public static class CallerRunsPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            if (!executor.isShutdown())
            {
                task.run();
            }
        }
    }

    public static class DiscardPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
        }
    }

    public static class DiscardOldestPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            if (!executor.isShutdown())
            {
                executor.getQueue().poll();
                executor.execute(task);
            }
        }
    }


    /*
     * Idle workers are woken by placing a signal in the work queue instead of
     * interrupting them.  darks-jvm's scheduler cannot safely interrupt a
     * coroutine that was already handed to a monitor's entry queue (a repeated
     * interrupt registers it twice and the monitor is never released), and a
     * shutdown that only sets the state would leave blocked takers asleep.
     */
    private static final Runnable SHUTDOWN_SIGNAL = new Runnable()
    {
        public void run()
        {
        }
    };

    private final Object mainLock = new Object();
    private final BlockingQueue<Runnable> workQueue;
    private final ThreadFactory threadFactory;
    private RejectedExecutionHandler handler;
    private final ArrayList<Worker> workers = new ArrayList<Worker>();

    private int corePoolSize;
    private int maximumPoolSize;
    private long keepAliveMillis;
    private boolean allowCoreThreadTimeOut;
    private int runState = RUNNING;
    private int workerCount;
    private long completedTasks;
    private long largestPoolSize;

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                              long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue)
    {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), DEFAULT_HANDLER);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                              long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory)
    {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, DEFAULT_HANDLER);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                              long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler)
    {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    public ThreadPoolExecutor(int corePoolSize, int maximumPoolSize,
                              long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler)
    {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
        {
            throw new IllegalArgumentException("invalid pool size");
        }
        if (keepAliveTime < 0)
        {
            throw new IllegalArgumentException("keepAliveTime < 0");
        }
        if (workQueue == null || threadFactory == null || handler == null)
        {
            throw new NullPointerException("queue, factory and handler are required");
        }
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveMillis = unit.toMillis(keepAliveTime);
        this.workQueue = workQueue;
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    private final class Worker implements Runnable
    {
        final Thread thread;
        Runnable firstTask;
        boolean idle;

        Worker(Runnable firstTask)
        {
            this.firstTask = firstTask;
            this.thread = threadFactory.newThread(this);
        }

        public void run()
        {
            runWorker(this);
        }
    }

    private void runWorker(Worker worker)
    {
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        try
        {
            while (true)
            {
                if (task == null)
                {
                    synchronized (mainLock)
                    {
                        worker.idle = true;
                    }
                    task = getTask();
                    if (task == null || task == SHUTDOWN_SIGNAL)
                    {
                        break;
                    }
                    synchronized (mainLock)
                    {
                        worker.idle = false;
                    }
                }
                try
                {
                    task.run();
                }
                catch (Throwable failure)
                {
                    // A task failure must not kill the worker.
                }
                finally
                {
                    task = null;
                    synchronized (mainLock)
                    {
                        completedTasks++;
                    }
                }
            }
        }
        finally
        {
            workerDone(worker);
        }
    }

    private Runnable getTask()
    {
        while (true)
        {
            boolean timed;
            boolean stopping;
            synchronized (mainLock)
            {
                if (runState >= STOP)
                {
                    return null;
                }
                stopping = runState == SHUTDOWN;
                timed = allowCoreThreadTimeOut || workerCount > corePoolSize;
            }
            if (stopping)
            {
                // A shutting-down pool runs the tasks that are already queued
                // and then lets the worker exit.  Do not block here: the queue
                // may hold nothing but shutdown signals, and exiting on an
                // empty poll keeps the worker from waiting for a wakeup that
                // another worker already consumed.
                Runnable queued = workQueue.poll();
                if (queued == null || queued == SHUTDOWN_SIGNAL)
                {
                    return null;
                }
                return queued;
            }
            try
            {
                Runnable task;
                if (timed && keepAliveMillis > 0)
                {
                    task = workQueue.poll(keepAliveMillis, TimeUnit.MILLISECONDS);
                }
                else if (timed)
                {
                    task = workQueue.poll();
                }
                else
                {
                    task = workQueue.take();
                }
                if (task != null)
                {
                    return task;
                }
                if (timed)
                {
                    synchronized (mainLock)
                    {
                        if (runState >= STOP
                            || workerCount > corePoolSize
                            || allowCoreThreadTimeOut)
                        {
                            return null;
                        }
                    }
                }
            }
            catch (InterruptedException interrupted)
            {
                // Re-check the shutdown state and retry.
            }
        }
    }

    private boolean addWorker(Runnable firstTask, boolean core)
    {
        synchronized (mainLock)
        {
            if (runState >= STOP)
            {
                return false;
            }
            if (runState == SHUTDOWN && (firstTask != null || workQueue.isEmpty()))
            {
                return false;
            }
            int limit = core ? corePoolSize : maximumPoolSize;
            if (workerCount >= limit)
            {
                return false;
            }
            Worker worker = new Worker(firstTask);
            workers.add(worker);
            workerCount++;
            if (workerCount > largestPoolSize)
            {
                largestPoolSize = workerCount;
            }
            worker.thread.start();
            return true;
        }
    }

    public void execute(Runnable command)
    {
        if (command == null)
        {
            throw new NullPointerException("command");
        }
        boolean submitToCore = false;
        boolean reject = false;
        synchronized (mainLock)
        {
            if (runState != RUNNING)
            {
                reject = true;
            }
            else if (workerCount < corePoolSize)
            {
                submitToCore = true;
            }
        }
        if (reject)
        {
            handler.rejectedExecution(command, this);
            return;
        }
        if (submitToCore && addWorker(command, true))
        {
            return;
        }
        if (workQueue.offer(command))
        {
            boolean removeFromQueue = false;
            boolean startWorker = false;
            synchronized (mainLock)
            {
                if (runState != RUNNING && workQueue.remove(command))
                {
                    removeFromQueue = true;
                }
                else if (runState == RUNNING && workerCount == 0)
                {
                    startWorker = true;
                }
            }
            if (startWorker)
            {
                addWorker(null, false);
            }
            if (removeFromQueue)
            {
                handler.rejectedExecution(command, this);
            }
            return;
        }
        if (addWorker(command, false))
        {
            return;
        }
        handler.rejectedExecution(command, this);
    }

    private void workerDone(Worker worker)
    {
        synchronized (mainLock)
        {
            workers.remove(worker);
            if (workerCount > 0)
            {
                workerCount--;
            }
            mainLock.notifyAll();
        }
    }

    /*
     * Wake workers blocked in the work queue.  One signal per worker is
     * queued: a worker only consumes one before exiting, so every blocked
     * worker is guaranteed to find one even if it raced with the shutdown
     * state change.  Surplus signals are simply left unread by a terminated
     * pool.
     */
    private void signalWorkers()
    {
        int count;
        synchronized (mainLock)
        {
            count = workerCount;
        }
        for (int i = 0; i < count; i++)
        {
            workQueue.offer(SHUTDOWN_SIGNAL);
        }
    }

    public void shutdown()
    {
        boolean changed;
        synchronized (mainLock)
        {
            changed = runState == RUNNING;
            if (changed)
            {
                runState = SHUTDOWN;
            }
        }
        if (changed)
        {
            signalWorkers();
        }
    }

    public List<Runnable> shutdownNow()
    {
        ArrayList<Runnable> pending = new ArrayList<Runnable>();
        ArrayList<Thread> running = new ArrayList<Thread>();
        Thread self = Thread.currentThread();
        synchronized (mainLock)
        {
            runState = STOP;
            for (int i = 0; i < workers.size(); i++)
            {
                Worker worker = workers.get(i);
                if (!worker.idle && worker.thread != self)
                {
                    running.add(worker.thread);
                }
            }
        }
        workQueue.drainTo(pending);
        for (int i = pending.size() - 1; i >= 0; i--)
        {
            if (pending.get(i) == SHUTDOWN_SIGNAL)
            {
                pending.remove(i);
            }
        }
        signalWorkers();
        for (int i = 0; i < running.size(); i++)
        {
            running.get(i).interrupt();
        }
        return pending;
    }

    public boolean isShutdown()
    {
        synchronized (mainLock)
        {
            return runState != RUNNING;
        }
    }

    public boolean isTerminated()
    {
        synchronized (mainLock)
        {
            return runState != RUNNING && workerCount == 0;
        }
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException
    {
        long millis = unit.toMillis(timeout);
        long deadline = System.currentTimeMillis() + millis;
        synchronized (mainLock)
        {
            while (workerCount > 0)
            {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0)
                {
                    return false;
                }
                mainLock.wait(remaining);
            }
            return true;
        }
    }

    public BlockingQueue<Runnable> getQueue()
    {
        return workQueue;
    }

    public int getPoolSize()
    {
        synchronized (mainLock)
        {
            return workerCount;
        }
    }

    public int getActiveCount()
    {
        synchronized (mainLock)
        {
            int active = 0;
            for (int i = 0; i < workers.size(); i++)
            {
                if (!workers.get(i).idle)
                {
                    active++;
                }
            }
            return active;
        }
    }

    public int getCorePoolSize()
    {
        synchronized (mainLock)
        {
            return corePoolSize;
        }
    }

    public int getMaximumPoolSize()
    {
        synchronized (mainLock)
        {
            return maximumPoolSize;
        }
    }

    public long getCompletedTaskCount()
    {
        synchronized (mainLock)
        {
            return completedTasks;
        }
    }

    public long getLargestPoolSize()
    {
        synchronized (mainLock)
        {
            return largestPoolSize;
        }
    }

    public long getKeepAliveTime(TimeUnit unit)
    {
        return unit.convert(keepAliveMillis, TimeUnit.MILLISECONDS);
    }

    public void setKeepAliveTime(long time, TimeUnit unit)
    {
        if (time < 0L)
        {
            throw new IllegalArgumentException("time < 0");
        }
        synchronized (mainLock)
        {
            keepAliveMillis = unit.toMillis(time);
        }
    }

    public ThreadFactory getThreadFactory()
    {
        return threadFactory;
    }

    public RejectedExecutionHandler getRejectedExecutionHandler()
    {
        return handler;
    }

    public void setRejectedExecutionHandler(RejectedExecutionHandler handler)
    {
        if (handler == null)
        {
            throw new NullPointerException("handler");
        }
        synchronized (mainLock)
        {
            this.handler = handler;
        }
    }

    public void setCorePoolSize(int size)
    {
        if (size < 0)
        {
            throw new IllegalArgumentException("size < 0");
        }
        synchronized (mainLock)
        {
            if (size > maximumPoolSize)
            {
                throw new IllegalArgumentException("core > maximum");
            }
            corePoolSize = size;
        }
    }

    public void setMaximumPoolSize(int size)
    {
        if (size <= 0 || size < corePoolSize)
        {
            throw new IllegalArgumentException("invalid maximum");
        }
        synchronized (mainLock)
        {
            maximumPoolSize = size;
        }
    }

    public void allowCoreThreadTimeOut(boolean value)
    {
        synchronized (mainLock)
        {
            allowCoreThreadTimeOut = value;
            if (value && keepAliveMillis <= 0)
            {
                keepAliveMillis = 1;
            }
        }
    }

    public boolean allowsCoreThreadTimeOut()
    {
        synchronized (mainLock)
        {
            return allowCoreThreadTimeOut;
        }
    }

    public boolean prestartCoreThread()
    {
        return addWorker(null, true);
    }

    public int prestartAllCoreThreads()
    {
        int started = 0;
        while (addWorker(null, true))
        {
            started++;
        }
        return started;
    }

    public void purge()
    {
    }

    public String toString()
    {
        synchronized (mainLock)
        {
            int active = 0;
            for (int i = 0; i < workers.size(); i++)
            {
                if (!workers.get(i).idle)
                {
                    active++;
                }
            }
            return "ThreadPoolExecutor[pool=" + workerCount
                + ", active=" + active
                + ", queued=" + workQueue.size()
                + ", completed=" + completedTasks + "]";
        }
    }
}
