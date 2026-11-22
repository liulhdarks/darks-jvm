package javax.management;

public class ServiceNotFoundException extends OperationsException
{
    public ServiceNotFoundException()
    {
    }

    public ServiceNotFoundException(String message)
    {
        super(message);
    }
}
