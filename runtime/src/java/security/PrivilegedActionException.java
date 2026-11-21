package java.security;

public class PrivilegedActionException extends Exception
{
    private Exception exception;

    public PrivilegedActionException(Exception exception)
    {
        super((String)null);
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }
}
