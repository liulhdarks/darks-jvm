package org.xml.sax;

public class InputSource
{
    private String publicId;
    private String systemId;
    private java.io.InputStream byteStream;
    private java.io.Reader characterStream;
    private String encoding;

    public InputSource()
    {
    }

    public InputSource(String systemId)
    {
        this.systemId = systemId;
    }

    public InputSource(java.io.InputStream byteStream)
    {
        this.byteStream = byteStream;
    }

    public InputSource(java.io.Reader characterStream)
    {
        this.characterStream = characterStream;
    }

    public void setPublicId(String publicId)
    {
        this.publicId = publicId;
    }

    public String getPublicId()
    {
        return publicId;
    }

    public void setSystemId(String systemId)
    {
        this.systemId = systemId;
    }

    public String getSystemId()
    {
        return systemId;
    }

    public void setByteStream(java.io.InputStream byteStream)
    {
        this.byteStream = byteStream;
    }

    public java.io.InputStream getByteStream()
    {
        return byteStream;
    }

    public void setCharacterStream(java.io.Reader characterStream)
    {
        this.characterStream = characterStream;
    }

    public java.io.Reader getCharacterStream()
    {
        return characterStream;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public String toString()
    {
        return getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(this))
            + "[systemId=" + systemId + "]";
    }
}
