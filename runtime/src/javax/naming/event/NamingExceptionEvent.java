package javax.naming.event;

import javax.naming.NamingException;

public class NamingExceptionEvent extends java.util.EventObject
{
    private static final long serialVersionUID = -4873015028103947168L;

    private final NamingException exception;

    public NamingExceptionEvent(Object source, NamingException exception)
    {
        super(source);
        this.exception = exception;
    }

    public NamingException getException()
    {
        return exception;
    }

    public Object getEventContext()
    {
        return getSource();
    }
}
