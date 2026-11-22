package javax.management;

public class BadStringOperationException extends Exception
{
    private final String operation;

    public BadStringOperationException(String message)
    {
        super(message);
        this.operation = message;
    }

    public String toString()
    {
        return "BadStringOperationException: " + operation;
    }
}
