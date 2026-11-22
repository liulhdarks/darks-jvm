package javax.management;

public class MBeanException extends JMException
{
    private static final long serialVersionUID = 1L;

    private final Exception exception;

    public MBeanException(Exception e)
    {
        super(e == null ? null : e.toString());
        this.exception = e;
    }

    public MBeanException(Exception e, String message)
    {
        super(message);
        this.exception = e;
    }

    public Exception getTargetException()
    {
        return exception;
    }

    public Throwable getCause()
    {
        return exception;
    }
}
