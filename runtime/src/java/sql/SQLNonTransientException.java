package java.sql;

public class SQLNonTransientException extends SQLException
{
    public SQLNonTransientException()
    {
        super();
    }

    public SQLNonTransientException(String reason)
    {
        super(reason);
    }

    public SQLNonTransientException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLNonTransientException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLNonTransientException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLNonTransientException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLNonTransientException(Throwable cause)
    {
        super(cause);
    }
}
