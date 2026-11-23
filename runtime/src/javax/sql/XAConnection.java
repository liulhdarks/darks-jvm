package javax.sql;

public interface XAConnection extends javax.sql.PooledConnection
{
    void close();
    java.sql.Connection getConnection();
    javax.transaction.xa.XAResource getXAResource();
}
