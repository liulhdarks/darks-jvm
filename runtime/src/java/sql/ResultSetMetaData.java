package java.sql;

public interface ResultSetMetaData extends java.sql.Wrapper
{
    java.lang.String getCatalogName(int a0);
    java.lang.String getColumnClassName(int a0);
    int getColumnCount();
    int getColumnDisplaySize(int a0);
    java.lang.String getColumnLabel(int a0);
    java.lang.String getColumnName(int a0);
    int getColumnType(int a0);
    java.lang.String getColumnTypeName(int a0);
    int getPrecision(int a0);
    int getScale(int a0);
    java.lang.String getSchemaName(int a0);
    java.lang.String getTableName(int a0);
    boolean isAutoIncrement(int a0);
    boolean isCaseSensitive(int a0);
    boolean isCurrency(int a0);
    boolean isDefinitelyWritable(int a0);
    int isNullable(int a0);
    boolean isReadOnly(int a0);
    boolean isSearchable(int a0);
    boolean isSigned(int a0);
    boolean isWritable(int a0);
}
