package java.sql;

public interface NClob extends java.sql.Clob
{
    void free();
    java.io.InputStream getAsciiStream();
    java.io.Reader getCharacterStream();
    long length();
    java.io.OutputStream setAsciiStream(long a0);
    int setString(long a0, java.lang.String a1);
}
