package javax.xml.transform.stream;

import javax.xml.transform.Result;

public class StreamResult implements Result
{
    private java.io.OutputStream outputStream;
    private java.io.Writer writer;
    private String systemId;

    public StreamResult()
    {
    }

    public StreamResult(java.io.OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public StreamResult(java.io.Writer writer)
    {
        this.writer = writer;
    }

    public StreamResult(java.io.File file)
    {
        this.systemId = file.toURI().toString();
    }

    public StreamResult(String systemId)
    {
        this.systemId = systemId;
    }

    public java.io.OutputStream getOutputStream()
    {
        return outputStream;
    }

    public void setOutputStream(java.io.OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public java.io.Writer getWriter()
    {
        return writer;
    }

    public void setWriter(java.io.Writer writer)
    {
        this.writer = writer;
    }

    public void setSystemId(String systemId)
    {
        this.systemId = systemId;
    }

    public String getSystemId()
    {
        return systemId;
    }

    public boolean isEmpty()
    {
        return outputStream == null && writer == null && systemId == null;
    }
}
