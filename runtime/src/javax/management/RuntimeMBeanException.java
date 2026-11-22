package javax.management;

public class RuntimeMBeanException extends JMRuntimeException
{
    private final RuntimeException exception;

    public RuntimeMBeanException(RuntimeException exception)
    {
        this.exception = exception;
    }

    public RuntimeMBeanException(RuntimeException exception, String message)
    {
        super(message);
        this.exception = exception;
    }

    public RuntimeException getTargetException()
    {
        return exception;
    }

    public Throwable getCause()
    {
        return exception;
    }
}
