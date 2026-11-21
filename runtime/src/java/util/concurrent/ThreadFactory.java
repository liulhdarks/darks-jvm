package java.util.concurrent;

/**
 * A factory for creating threads.
 */
public interface ThreadFactory
{
    Thread newThread(Runnable r);
}
