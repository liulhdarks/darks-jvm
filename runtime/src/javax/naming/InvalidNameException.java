package javax.naming;

public class InvalidNameException extends NamingException
{
    private static final long serialVersionUID = -4202512507404713249L;

    public InvalidNameException()
    {
        super();
    }

    public InvalidNameException(String explanation)
    {
        super(explanation);
    }
}
