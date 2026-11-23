package java.sql;

public class SQLSyntaxErrorException extends SQLNonTransientException
{
    public SQLSyntaxErrorException()
    {
        super();
    }

    public SQLSyntaxErrorException(String reason)
    {
        super(reason);
    }

    public SQLSyntaxErrorException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLSyntaxErrorException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLSyntaxErrorException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLSyntaxErrorException(Throwable cause)
    {
        super(cause);
    }
}
