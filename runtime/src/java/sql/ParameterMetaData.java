package java.sql;

public interface ParameterMetaData extends java.sql.Wrapper
{
    java.lang.String getParameterClassName(int a0);
    int getParameterCount();
    int getParameterMode(int a0);
    int getParameterType(int a0);
    java.lang.String getParameterTypeName(int a0);
    int getPrecision(int a0);
    int getScale(int a0);
}
