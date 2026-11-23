package javax.sql;

public interface ConnectionEventListener
{
    void connectionClosed(javax.sql.ConnectionEvent a0);
    void connectionErrorOccurred(javax.sql.ConnectionEvent a0);
}
