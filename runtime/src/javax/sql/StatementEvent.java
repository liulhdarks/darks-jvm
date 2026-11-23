package javax.sql;

import java.sql.PreparedStatement;
import java.util.EventObject;

public class StatementEvent extends EventObject
{
    private final PreparedStatement statement;

    public StatementEvent(PooledConnection connection, PreparedStatement statement)
    {
        super(connection);
        this.statement = statement;
    }

    public PreparedStatement getStatement()
    {
        return statement;
    }
}
