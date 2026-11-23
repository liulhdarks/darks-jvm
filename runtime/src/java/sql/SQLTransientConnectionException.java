package java.sql;

public class SQLTransientConnectionException extends SQLTransientException
{
    public SQLTransientConnectionException()
    {
        super();
    }

    public SQLTransientConnectionException(String reason)
    {
        super(reason);
    }

    public SQLTransientConnectionException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLTransientConnectionException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLTransientConnectionException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLTransientConnectionException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLTransientConnectionException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLTransientConnectionException(Throwable cause)
    {
        super(cause);
    }
}
