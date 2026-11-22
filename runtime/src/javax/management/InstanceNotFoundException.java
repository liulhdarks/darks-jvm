package javax.management;

public class InstanceNotFoundException extends OperationsException
{
    public InstanceNotFoundException()
    {
    }

    public InstanceNotFoundException(String message)
    {
        super(message);
    }
}
