package java.util.jar;

import java.util.zip.ZipEntry;

public class JarEntry extends ZipEntry
{
    public JarEntry(String name)
    {
        super(name);
    }

    public JarEntry(ZipEntry entry)
    {
        super(entry);
    }

    public JarEntry(JarEntry entry)
    {
        this((ZipEntry)entry);
    }

    public Attributes getAttributes() throws java.io.IOException
    {
        return null;
    }

    public java.security.cert.Certificate[] getCertificates()
    {
        return null;
    }

    public java.security.CodeSigner[] getCodeSigners()
    {
        return null;
    }

    public String getRealName()
    {
        return getName();
    }
}
