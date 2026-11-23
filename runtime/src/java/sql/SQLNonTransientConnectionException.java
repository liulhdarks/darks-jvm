package java.sql;

public class SQLNonTransientConnectionException extends SQLNonTransientException
{
    public SQLNonTransientConnectionException()
    {
        super();
    }

    public SQLNonTransientConnectionException(String reason)
    {
        super(reason);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLNonTransientConnectionException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLNonTransientConnectionException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLNonTransientConnectionException(Throwable cause)
    {
        super(cause);
    }
}
