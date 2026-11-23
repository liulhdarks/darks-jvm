package java.sql;

public interface Clob extends java.sql.Wrapper
{
    void free();
    java.io.InputStream getAsciiStream();
    java.io.Reader getCharacterStream();
    java.lang.String getSubString(long a0, int a1);
    long length();
    java.io.OutputStream setAsciiStream(long a0);
    java.io.Writer setCharacterStream(long a0);
    int setString(long a0, java.lang.String a1);
}
