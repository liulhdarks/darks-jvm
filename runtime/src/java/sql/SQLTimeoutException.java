package java.sql;

public class SQLTimeoutException extends SQLTransientException
{
    public SQLTimeoutException()
    {
        super();
    }

    public SQLTimeoutException(String reason)
    {
        super(reason);
    }

    public SQLTimeoutException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLTimeoutException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLTimeoutException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLTimeoutException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLTimeoutException(Throwable cause)
    {
        super(cause);
    }
}
