package javax.xml.transform;

public class TransformerException extends Exception
{
    private Throwable containedException;
    private SourceLocator locator;

    public TransformerException(String message)
    {
        super(message);
    }

    public TransformerException(Throwable cause)
    {
        super(cause);
        containedException = cause;
    }

    public TransformerException(String message, Throwable cause)
    {
        super(message);
        containedException = cause;
    }

    public TransformerException(String message, SourceLocator locator)
    {
        super(message);
        this.locator = locator;
    }

    public TransformerException(String message, SourceLocator locator, Throwable cause)
    {
        super(message);
        this.locator = locator;
        containedException = cause;
    }

    public Throwable getException()
    {
        return containedException;
    }

    public Throwable getCause()
    {
        return containedException == null ? super.getCause() : containedException;
    }

    public synchronized Throwable initCause(Throwable cause)
    {
        containedException = cause;
        return this;
    }

    public SourceLocator getLocator()
    {
        return locator;
    }

    public void setLocator(SourceLocator locator)
    {
        this.locator = locator;
    }

    public String getMessageAndLocation()
    {
        String message = getMessage();
        if (locator == null)
        {
            return message;
        }
        String location = getLocationAsString();
        return message == null ? location : message + location;
    }

    public String getLocationAsString()
    {
        if (locator == null)
        {
            return "";
        }
        return "; SystemID=" + locator.getSystemId()
            + "; lineNumber=" + locator.getLineNumber()
            + "; columnNumber=" + locator.getColumnNumber();
    }
}
