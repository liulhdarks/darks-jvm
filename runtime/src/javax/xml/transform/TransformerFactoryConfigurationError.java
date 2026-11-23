package javax.xml.transform;

public class TransformerFactoryConfigurationError extends Error
{
    private Exception exception;

    public TransformerFactoryConfigurationError()
    {
        super();
    }

    public TransformerFactoryConfigurationError(String message)
    {
        super(message);
    }

    public TransformerFactoryConfigurationError(Exception exception)
    {
        super(exception == null ? null : exception.getMessage());
        this.exception = exception;
    }

    public TransformerFactoryConfigurationError(Exception exception, String message)
    {
        super(message);
        this.exception = exception;
    }

    public Exception getException()
    {
        return exception;
    }
}
