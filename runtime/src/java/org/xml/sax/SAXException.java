package org.xml.sax;

public class SAXException extends Exception
{
    private Exception exception;

    public SAXException()
    {
        super();
    }

    public SAXException(String message)
    {
        super(message);
    }

    public SAXException(Exception e)
    {
        super(e);
        this.exception = e;
    }

    public SAXException(String message, Exception e)
    {
        super(message, e);
        this.exception = e;
    }

    public String getMessage()
    {
        String message = super.getMessage();
        if (message == null && exception != null)
        {
            return exception.getMessage();
        }
        return message;
    }

    public Exception getException()
    {
        return exception;
    }

    public Throwable getCause()
    {
        return exception != null ? exception : super.getCause();
    }

    public String toString()
    {
        String message = getMessage();
        return message == null ? getClass().getName() : getClass().getName() + ": " + message;
    }
}
