package org.xml.sax;

import java.io.InputStream;
import java.io.Reader;

// Minimal SAX InputSource.  It only carries the fields Spring's
// InputSourceEditor and property editors touch; no parser is provided.
public class InputSource
{
    private String publicId;
    private String systemId;
    private InputStream byteStream;
    private String encoding;
    private Reader characterStream;

    public InputSource()
    {
    }

    public InputSource(String systemId)
    {
        setSystemId(systemId);
    }

    public InputSource(InputStream byteStream)
    {
        setByteStream(byteStream);
    }

    public InputSource(Reader characterStream)
    {
        setCharacterStream(characterStream);
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

    public void setByteStream(InputStream byteStream)
    {
        this.byteStream = byteStream;
    }

    public InputStream getByteStream()
    {
        return byteStream;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setCharacterStream(Reader characterStream)
    {
        this.characterStream = characterStream;
    }

    public Reader getCharacterStream()
    {
        return characterStream;
    }

    public String toString()
    {
        return "org.xml.sax.InputSource[systemId=" + systemId + "]";
    }
}
