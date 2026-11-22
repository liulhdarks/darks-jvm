package javax.management;

public class MalformedObjectNameException extends OperationsException
{
    public MalformedObjectNameException()
    {
    }

    public MalformedObjectNameException(String message)
    {
        super(message);
    }
}
