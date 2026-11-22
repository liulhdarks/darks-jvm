package java.util.concurrent;

/**
 * Handler invoked when a task cannot be accepted by an executor.
 */
public interface RejectedExecutionHandler
{
    void rejectedExecution(Runnable task, ThreadPoolExecutor executor);
}
