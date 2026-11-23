package java.sql;

public class SQLWarning extends SQLException
{
    private SQLWarning nextWarning;

    public SQLWarning()
    {
        super();
    }

    public SQLWarning(String reason)
    {
        super(reason);
    }

    public SQLWarning(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLWarning(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLWarning(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLWarning(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLWarning(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLWarning(Throwable cause)
    {
        super(cause);
    }

    public SQLWarning getNextWarning()
    {
        return nextWarning;
    }

    public void setNextWarning(SQLWarning warning)
    {
        nextWarning = warning;
    }
}
