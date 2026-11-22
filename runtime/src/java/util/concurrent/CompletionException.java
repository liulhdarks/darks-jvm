package java.util.concurrent;

public class CompletionException extends RuntimeException
{
    public CompletionException()
    {
    }

    public CompletionException(String message)
    {
        super(message);
    }

    public CompletionException(String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }

    public CompletionException(Throwable cause)
    {
        super(cause);
    }
}
