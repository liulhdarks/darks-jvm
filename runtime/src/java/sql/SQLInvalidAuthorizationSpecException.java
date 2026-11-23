package java.sql;

public class SQLInvalidAuthorizationSpecException extends SQLNonTransientException
{
    public SQLInvalidAuthorizationSpecException()
    {
        super();
    }

    public SQLInvalidAuthorizationSpecException(String reason)
    {
        super(reason);
    }

    public SQLInvalidAuthorizationSpecException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLInvalidAuthorizationSpecException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLInvalidAuthorizationSpecException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLInvalidAuthorizationSpecException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLInvalidAuthorizationSpecException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLInvalidAuthorizationSpecException(Throwable cause)
    {
        super(cause);
    }
}
