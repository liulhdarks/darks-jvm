package java.util.concurrent;

/**
 * An object that executes submitted {@link Runnable} tasks.
 */
public interface Executor
{
    void execute(Runnable command);
}
