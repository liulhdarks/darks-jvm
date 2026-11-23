package java.lang;

public class InternalError extends VirtualMachineError
{
    private static final long serialVersionUID = -9062593416125562365L;

    public InternalError()
    {
        super();
    }

    public InternalError(String message)
    {
        super(message);
    }

    public InternalError(String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }

    public InternalError(Throwable cause)
    {
        super(cause == null ? null : cause.toString());
        initCause(cause);
    }
}
