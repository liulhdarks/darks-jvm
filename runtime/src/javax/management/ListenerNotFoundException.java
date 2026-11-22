package javax.management;

public class ListenerNotFoundException extends OperationsException
{
    public ListenerNotFoundException()
    {
    }

    public ListenerNotFoundException(String message)
    {
        super(message);
    }
}
