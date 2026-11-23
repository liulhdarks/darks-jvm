package javax.sql;

public interface XADataSource extends javax.sql.CommonDataSource
{
    javax.sql.XAConnection getXAConnection();
    javax.sql.XAConnection getXAConnection(String user, String password);
}
