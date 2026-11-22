package javax.management;

public class RuntimeOperationsException extends JMRuntimeException
{
    private static final long serialVersionUID = 1L;

    private final RuntimeException exception;

    public RuntimeOperationsException(RuntimeException e)
    {
        super(e == null ? null : e.toString());
        this.exception = e;
    }

    public RuntimeOperationsException(RuntimeException e, String message)
    {
        super(message);
        this.exception = e;
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
