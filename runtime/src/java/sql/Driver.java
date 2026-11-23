package java.sql;

public interface Driver
{
    boolean acceptsURL(java.lang.String a0) throws java.sql.SQLException;
    java.sql.Connection connect(java.lang.String a0, java.util.Properties a1) throws java.sql.SQLException;
    java.util.logging.Logger getParentLogger() throws java.sql.SQLFeatureNotSupportedException;
}
