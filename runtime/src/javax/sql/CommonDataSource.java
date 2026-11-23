package javax.sql;

public interface CommonDataSource
{
    java.io.PrintWriter getLogWriter();
    int getLoginTimeout();
    void setLogWriter(java.io.PrintWriter writer);
    void setLoginTimeout(int seconds);
}
