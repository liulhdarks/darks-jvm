package java.lang.reflect;

public class InvocationTargetException extends ReflectiveOperationException
{
    private Throwable target;

    public InvocationTargetException(Throwable target)
    {
        this.target = target;
    }

    public InvocationTargetException(Throwable target, String message)
    {
        super(message);
        this.target = target;
    }

    public Throwable getTargetException()
    {
        return target;
    }

    public Throwable getCause()
    {
        return target;
    }
}
