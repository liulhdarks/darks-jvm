package java.sql;

public class SQLException extends Exception
{
    private String SQLState;
    private int vendorCode;
    private SQLException next;

    public SQLException()
    {
        super();
    }

    public SQLException(String reason)
    {
        super(reason);
    }

    public SQLException(String reason, String SQLState)
    {
        super(reason);
        this.SQLState = SQLState;
    }

    public SQLException(String reason, String SQLState, int vendorCode)
    {
        super(reason);
        this.SQLState = SQLState;
        this.vendorCode = vendorCode;
    }

    public SQLException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, cause);
        this.SQLState = SQLState;
        this.vendorCode = vendorCode;
    }

    public SQLException(String reason, String SQLState, Throwable cause)
    {
        super(reason, cause);
        this.SQLState = SQLState;
    }

    public SQLException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLException(Throwable cause)
    {
        super(cause);
    }

    public String getSQLState()
    {
        return SQLState;
    }

    public int getErrorCode()
    {
        return vendorCode;
    }

    public SQLException getNextException()
    {
        return next;
    }

    public void setNextException(SQLException exception)
    {
        if (next == null)
        {
            next = exception;
            return;
        }
        SQLException current = next;
        while (current.next != null)
        {
            current = current.next;
        }
        current.next = exception;
    }
}
