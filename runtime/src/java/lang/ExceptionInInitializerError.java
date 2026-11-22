package java.lang;

public class ExceptionInInitializerError extends LinkageError
{
    public ExceptionInInitializerError()
    {
        super();
    }

    public ExceptionInInitializerError(Throwable thrown)
    {
        super((String)null);
        initCause(thrown);
    }

    public ExceptionInInitializerError(String message)
    {
        super(message);
    }

    public Throwable getException()
    {
        return getCause();
    }
}
