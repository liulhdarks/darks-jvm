package java.util.concurrent;

/**
 * Exception thrown when a task fails with an exception.
 */
public class ExecutionException extends Exception
{
    public ExecutionException()
    {
        super();
    }

    public ExecutionException(String message)
    {
        super(message);
    }

    public ExecutionException(String message, Throwable cause)
    {
        super(cause == null ? message : message + ": " + cause.toString());
    }

    public ExecutionException(Throwable cause)
    {
        super(cause == null ? null : cause.toString());
    }
}
