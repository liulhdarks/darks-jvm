package java.security;

/**
 * Wraps the checked exception thrown by a {@link PrivilegedExceptionAction}.
 */
public class PrivilegedActionException extends Exception
{
    private final Exception exception;

    public PrivilegedActionException(Exception exception)
    {
        super();
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }

    public Throwable getCause()
    {
        return exception;
    }

    public String toString()
    {
        return "java.security.PrivilegedActionException: " + exception;
    }
}
