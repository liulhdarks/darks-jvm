package javax.sql;

public interface PooledConnection
{
    void addConnectionEventListener(javax.sql.ConnectionEventListener a0);
    void close();
    java.sql.Connection getConnection();
    void removeConnectionEventListener(javax.sql.ConnectionEventListener a0);
}
