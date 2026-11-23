package java.sql;

public interface SQLXML extends java.sql.Wrapper
{
    void free();
    java.io.InputStream getBinaryStream();
    java.io.Reader getCharacterStream();
    javax.xml.transform.Source getSource(java.lang.Class a0);
    java.lang.String getString();
    java.io.OutputStream setBinaryStream();
    java.io.Writer setCharacterStream();
    javax.xml.transform.Result setResult(java.lang.Class a0);
    void setString(java.lang.String a0);
}
