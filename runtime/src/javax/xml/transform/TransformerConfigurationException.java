package javax.xml.transform;

public class TransformerConfigurationException extends TransformerException
{
    public TransformerConfigurationException()
    {
        super("Transformer configuration error");
    }

    public TransformerConfigurationException(String message)
    {
        super(message);
    }

    public TransformerConfigurationException(Throwable cause)
    {
        super(cause);
    }

    public TransformerConfigurationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TransformerConfigurationException(String message, SourceLocator locator)
    {
        super(message, locator);
    }

    public TransformerConfigurationException(String message, SourceLocator locator,
        Throwable cause)
    {
        super(message, locator, cause);
    }
}
