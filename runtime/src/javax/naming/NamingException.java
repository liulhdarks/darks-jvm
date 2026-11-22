package javax.naming;

public class NamingException extends Exception
{
    private static final long serialVersionUID = -1299181962103167177L;

    protected Name resolvedName;
    protected Object resolvedObj;
    protected Name remainingName;

    public NamingException()
    {
        super();
    }

    public NamingException(String explanation)
    {
        super(explanation);
    }

    public NamingException(String explanation, Throwable cause)
    {
        super(explanation, cause);
    }

    public NamingException(Throwable cause)
    {
        super(cause);
    }

    public Name getResolvedName()
    {
        return resolvedName;
    }

    public void setResolvedName(Name name)
    {
        resolvedName = name;
    }

    public Name getRemainingName()
    {
        return remainingName;
    }

    public void setRemainingName(Name name)
    {
        remainingName = name;
    }

    public void appendRemainingName(Name name)
    {
        remainingName = name;
    }

    public void appendRemainingComponent(String name)
    {
    }

    public Object getResolvedObj()
    {
        return resolvedObj;
    }

    public void setResolvedObj(Object obj)
    {
        resolvedObj = obj;
    }

    public Throwable getRootCause()
    {
        Throwable cause = getCause();
        while (cause != null && cause.getCause() != null)
        {
            cause = cause.getCause();
        }
        return cause;
    }

    public String toString()
    {
        return toString(false);
    }

    public String toString(boolean detail)
    {
        String result = super.toString();
        if (detail && resolvedObj != null)
        {
            result += " [Root exception is " + resolvedObj + "]";
        }
        return result;
    }
}
