package javax.management;

public class RuntimeErrorException extends JMRuntimeException
{
    private static final long serialVersionUID = 1L;

    private final Error error;

    public RuntimeErrorException(Error e)
    {
        super(e == null ? null : e.toString());
        this.error = e;
    }

    public RuntimeErrorException(Error e, String message)
    {
        super(message);
        this.error = e;
    }

    public Error getTargetError()
    {
        return error;
    }

    public Throwable getCause()
    {
        return error;
    }
}
