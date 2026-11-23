package javax.sql;

public interface ConnectionPoolDataSource extends javax.sql.CommonDataSource
{
    java.io.PrintWriter getLogWriter();
    javax.sql.PooledConnection getPooledConnection();
}
