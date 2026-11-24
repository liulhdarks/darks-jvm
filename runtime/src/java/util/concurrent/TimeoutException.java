package java.util.concurrent;

/**
 * Exception thrown when a blocking operation times out.
 */
public class TimeoutException extends Exception
{
    public TimeoutException()
    {
        super();
    }

    public TimeoutException(String message)
    {
        super(message);
    }
}
