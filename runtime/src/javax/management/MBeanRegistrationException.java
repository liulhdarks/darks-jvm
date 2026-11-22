package javax.management;

public class MBeanRegistrationException extends MBeanException
{
    public MBeanRegistrationException(Exception exception)
    {
        super(exception);
    }

    public MBeanRegistrationException(Exception exception, String message)
    {
        super(exception, message);
    }
}
