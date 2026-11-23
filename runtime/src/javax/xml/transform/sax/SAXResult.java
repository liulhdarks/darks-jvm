package javax.xml.transform.sax;

import org.xml.sax.ContentHandler;

import javax.xml.transform.Result;

public class SAXResult implements Result
{
    public static final String FEATURE = "http://javax.xml.transform.sax.SAXResult/feature";

    private ContentHandler handler;
    private String systemId;

    public SAXResult()
    {
    }

    public SAXResult(ContentHandler handler)
    {
        this.handler = handler;
    }

    public void setHandler(ContentHandler handler)
    {
        this.handler = handler;
    }

    public ContentHandler getHandler()
    {
        return handler;
    }

    public void setSystemId(String systemId)
    {
        this.systemId = systemId;
    }

    public String getSystemId()
    {
        return systemId;
    }
}
