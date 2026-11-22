package javax.management;

public class InstanceAlreadyExistsException extends OperationsException
{
    public InstanceAlreadyExistsException()
    {
    }

    public InstanceAlreadyExistsException(String message)
    {
        super(message);
    }
}
