package javax.xml.transform.stax;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;

import javax.xml.transform.Result;

public class StAXResult implements Result
{
    public static final String XMLSTREAMWRITER = "javax.xml.transform.stax.StAXResult/XMLStreamWriter";
    public static final String XMLEVENTWRITER = "javax.xml.transform.stax.StAXResult/XMLEventWriter";

    private XMLStreamWriter streamWriter;
    private XMLEventWriter eventWriter;

    public StAXResult(XMLStreamWriter writer)
    {
        this.streamWriter = writer;
    }

    public StAXResult(XMLEventWriter writer)
    {
        this.eventWriter = writer;
    }

    public XMLStreamWriter getXMLStreamWriter()
    {
        return streamWriter;
    }

    public XMLEventWriter getXMLEventWriter()
    {
        return eventWriter;
    }

    public void setSystemId(String systemId)
    {
        throw new UnsupportedOperationException("StAXResult has no system ID");
    }

    public String getSystemId()
    {
        return null;
    }
}
