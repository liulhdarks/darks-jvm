package java.util.concurrent;

/**
 * Provides default implementations of the task submission methods.
 */
public abstract class AbstractExecutorService implements ExecutorService
{
    public <T> Future<T> submit(Callable<T> task)
    {
        if (task == null)
        {
            throw new NullPointerException("task");
        }
        FutureTask<T> future = new FutureTask<T>(task);
        execute(future);
        return future;
    }

    public <T> Future<T> submit(Runnable task, T result)
    {
        if (task == null)
        {
            throw new NullPointerException("task");
        }
        FutureTask<T> future = new FutureTask<T>(task, result);
        execute(future);
        return future;
    }

    public Future<?> submit(Runnable task)
    {
        return submit(task, null);
    }
}
