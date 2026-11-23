package javax.xml.transform.stream;

import javax.xml.transform.Source;

public class StreamSource implements Source
{
    private java.io.InputStream inputStream;
    private java.io.Reader reader;
    private String systemId;
    private String publicId;

    public StreamSource()
    {
    }

    public StreamSource(java.io.InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public StreamSource(java.io.InputStream inputStream, String systemId)
    {
        this.inputStream = inputStream;
        this.systemId = systemId;
    }

    public StreamSource(java.io.Reader reader)
    {
        this.reader = reader;
    }

    public StreamSource(java.io.Reader reader, String systemId)
    {
        this.reader = reader;
        this.systemId = systemId;
    }

    public StreamSource(java.io.File file)
    {
        this.systemId = file.toURI().toString();
    }

    public StreamSource(String systemId)
    {
        this.systemId = systemId;
    }

    public java.io.InputStream getInputStream()
    {
        return inputStream;
    }

    public void setInputStream(java.io.InputStream inputStream)
    {
        this.inputStream = inputStream;
    }

    public java.io.Reader getReader()
    {
        return reader;
    }

    public void setReader(java.io.Reader reader)
    {
        this.reader = reader;
    }

    public void setSystemId(String systemId)
    {
        this.systemId = systemId;
    }

    public String getSystemId()
    {
        return systemId;
    }

    public String getPublicId()
    {
        return publicId;
    }

    public void setPublicId(String publicId)
    {
        this.publicId = publicId;
    }

    public boolean isEmpty()
    {
        return inputStream == null && reader == null && systemId == null;
    }
}
