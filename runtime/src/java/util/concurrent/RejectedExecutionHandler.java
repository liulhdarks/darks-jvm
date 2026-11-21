package java.util.concurrent;

/**
 * Handler invoked when a task cannot be accepted by an executor.
 */
public interface RejectedExecutionHandler
{
    void rejectedExecution(Runnable task, ThreadPoolExecutor executor);

    class AbortPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            throw new RejectedExecutionException("task rejected from " + executor);
        }
    }

    class CallerRunsPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
            if (!executor.isShutdown())
            {
                task.run();
            }
        }
    }

    class DiscardPolicy implements RejectedExecutionHandler
    {
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor)
        {
        }
    }

    class DiscardOldestPolicy implements RejectedExecutionHandler
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
}
