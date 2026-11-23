package javax.sql;

public interface DataSource extends javax.sql.CommonDataSource, java.sql.Wrapper
{
    java.sql.Connection getConnection();
    java.sql.Connection getConnection(java.lang.String a0, java.lang.String a1);
    java.io.PrintWriter getLogWriter();
    int getLoginTimeout();
    boolean isWrapperFor(java.lang.Class a0);
    void setLogWriter(java.io.PrintWriter a0);
    void setLoginTimeout(int a0);
    java.lang.Object unwrap(java.lang.Class a0);
}
