package javax.xml.parsers;

public class FactoryConfigurationError extends Error
{
    private Exception exception;

    public FactoryConfigurationError()
    {
        super();
    }

    public FactoryConfigurationError(String message)
    {
        super(message);
    }

    public FactoryConfigurationError(Exception exception)
    {
        super(exception == null ? null : exception.getMessage());
        this.exception = exception;
    }

    public FactoryConfigurationError(Exception exception, String message)
    {
        super(message);
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }
}
