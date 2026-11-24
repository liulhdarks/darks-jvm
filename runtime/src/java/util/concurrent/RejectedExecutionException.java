package java.util.concurrent;

/**
 * Exception thrown when a task submitted to an executor is rejected.
 */
public class RejectedExecutionException extends RuntimeException
{
    public RejectedExecutionException()
    {
        super();
    }

    public RejectedExecutionException(String message)
    {
        super(message);
    }
}
