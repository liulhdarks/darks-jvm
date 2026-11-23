package java.sql;

public class SQLRecoverableException extends SQLException
{
    public SQLRecoverableException()
    {
        super();
    }

    public SQLRecoverableException(String reason)
    {
        super(reason);
    }

    public SQLRecoverableException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLRecoverableException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLRecoverableException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLRecoverableException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLRecoverableException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLRecoverableException(Throwable cause)
    {
        super(cause);
    }
}
