package java.util.concurrent;

/**
 * Exception thrown when retrieving the result of a cancelled task.
 */
public class CancellationException extends RuntimeException
{
    public CancellationException()
    {
        super();
    }

    public CancellationException(String message)
    {
        super(message);
    }
}
