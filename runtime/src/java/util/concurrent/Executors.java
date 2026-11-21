package java.util.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory methods for executors.  The virtual-thread executor creates one
 * virtual thread per submitted task; tasks are tracked so shutdown and
 * awaitTermination work without a fixed worker pool.
 */
public class Executors
{
    private Executors()
    {
    }

    public static ExecutorService newVirtualThreadPerTaskExecutor()
    {
        return new VirtualThreadPerTaskExecutor();
    }

    public static ThreadFactory defaultThreadFactory()
    {
        return new ThreadFactory()
        {
            public Thread newThread(Runnable task)
            {
                return new Thread(task);
            }
        };
    }

    private static final class VirtualThreadPerTaskExecutor extends AbstractExecutorService
    {
        private int active;
        private boolean shutdown;
        // Live threads are kept in a slot array so completion is O(1); a slot
        // is cleared when its task finishes.  Finished slots are not compacted
        // (wrappers keep their slot index), so the array costs one reference
        // per submitted task over the executor's lifetime.
        private Thread[] threads = new Thread[16];
        private int threadCount;

        private void growThreads()
        {
            Thread[] grown = new Thread[threads.length * 2];
            for (int i = 0; i < threadCount; i++)
            {
                grown[i] = threads[i];
            }
            threads = grown;
        }

        public void execute(Runnable command)
        {
            if (command == null)
            {
                throw new NullPointerException("command");
            }
            Thread thread;
            int slot;
            synchronized (this)
            {
                if (shutdown)
                {
                    throw new RejectedExecutionException("executor is shut down");
                }
                active++;
                if (threadCount == threads.length)
                {
                    growThreads();
                }
                slot = threadCount;
                thread = Thread.ofVirtual().unstarted(new TaskWrapper(command, this, slot));
                threads[threadCount] = thread;
                threadCount++;
            }
            try
            {
                thread.start();
            }
            catch (RuntimeException failure)
            {
                finished(slot, thread);
                throw failure;
            }
        }

        void finished(int slot, Thread thread)
        {
            synchronized (this)
            {
                if (slot >= 0 && slot < threadCount && threads[slot] == thread)
                {
                    threads[slot] = null;
                }
                if (active > 0)
                {
                    active--;
                }
                notifyAll();
            }
        }

        public void shutdown()
        {
            synchronized (this)
            {
                shutdown = true;
                notifyAll();
            }
        }

        public List<Runnable> shutdownNow()
        {
            List<Thread> copy = new ArrayList<Thread>();
            synchronized (this)
            {
                shutdown = true;
                for (int i = 0; i < threadCount; i++)
                {
                    if (threads[i] != null)
                    {
                        copy.add(threads[i]);
                    }
                }
                notifyAll();
            }
            for (int i = 0; i < copy.size(); i++)
            {
                copy.get(i).interrupt();
            }
            return new ArrayList<Runnable>();
        }

        public synchronized boolean isShutdown()
        {
            return shutdown;
        }

        public synchronized boolean isTerminated()
        {
            return shutdown && active == 0;
        }

        public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException
        {
            long millis = unit.toMillis(timeout);
            long deadline = System.currentTimeMillis() + millis;
            synchronized (this)
            {
                while (active > 0)
                {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0)
                    {
                        return false;
                    }
                    wait(remaining);
                }
                return true;
            }
        }
    }

    private static final class TaskWrapper implements Runnable
    {
        private final Runnable task;
        private final VirtualThreadPerTaskExecutor owner;
        private final int slot;

        TaskWrapper(Runnable task, VirtualThreadPerTaskExecutor owner, int slot)
        {
            this.task = task;
            this.owner = owner;
            this.slot = slot;
        }

        public void run()
        {
            try
            {
                task.run();
            }
            finally
            {
                owner.finished(slot, Thread.currentThread());
            }
        }
    }
}
