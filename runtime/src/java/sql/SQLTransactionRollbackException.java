package java.sql;

public class SQLTransactionRollbackException extends SQLTransientException
{
    public SQLTransactionRollbackException()
    {
        super();
    }

    public SQLTransactionRollbackException(String reason)
    {
        super(reason);
    }

    public SQLTransactionRollbackException(String reason, String SQLState)
    {
        super(reason, SQLState);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, int vendorCode)
    {
        super(reason, SQLState, vendorCode);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, int vendorCode, Throwable cause)
    {
        super(reason, SQLState, vendorCode, cause);
    }

    public SQLTransactionRollbackException(String reason, String SQLState, Throwable cause)
    {
        super(reason, SQLState, cause);
    }

    public SQLTransactionRollbackException(String reason, Throwable cause)
    {
        super(reason, cause);
    }

    public SQLTransactionRollbackException(Throwable cause)
    {
        super(cause);
    }
}
