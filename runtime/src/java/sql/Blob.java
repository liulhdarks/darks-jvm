package java.sql;

public interface Blob extends java.sql.Wrapper
{
    void free();
    java.io.InputStream getBinaryStream();
    byte[] getBytes(long a0, int a1);
    long length();
    java.io.OutputStream setBinaryStream(long a0);
    int setBytes(long a0, byte[] a1);
}
