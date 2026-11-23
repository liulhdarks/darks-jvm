package javax.sql;

import java.sql.SQLException;
import java.util.EventObject;

public class ConnectionEvent extends EventObject
{
    private final SQLException exception;

    public ConnectionEvent(PooledConnection connection)
    {
        super(connection);
        this.exception = null;
    }

    public ConnectionEvent(PooledConnection connection, SQLException exception)
    {
        super(connection);
        this.exception = exception;
    }

    public SQLException getSQLException()
    {
        return exception;
    }
}
