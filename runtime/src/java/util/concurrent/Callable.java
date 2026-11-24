package java.util.concurrent;

/**
 * A task that returns a result and may throw an exception.
 */
public interface Callable<V>
{
    V call() throws Exception;
}
