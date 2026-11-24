package java.util.concurrent;

import java.util.List;

/**
 * An {@link Executor} that provides methods to manage termination and to
 * track the progress of asynchronous tasks.
 *
 * This is the minimal subset used by the darks-jvm runtime and its tests.
 */
public interface ExecutorService extends Executor, AutoCloseable
{
    void shutdown();

    List<Runnable> shutdownNow();

    boolean isShutdown();

    boolean isTerminated();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    <T> Future<T> submit(Callable<T> task);

    <T> Future<T> submit(Runnable task, T result);

    Future<?> submit(Runnable task);

    default void close()
    {
        shutdown();
    }
}
