package javax.management.modelmbean;

public class InvalidTargetObjectTypeException extends Exception
{
    private final Exception exception;

    public InvalidTargetObjectTypeException()
    {
        this.exception = null;
    }

    public InvalidTargetObjectTypeException(String message)
    {
        super(message);
        this.exception = null;
    }

    public InvalidTargetObjectTypeException(Exception exception, String message)
    {
        super(message);
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }
}
