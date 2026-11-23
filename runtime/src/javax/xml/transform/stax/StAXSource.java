package javax.xml.transform.stax;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;

import javax.xml.transform.Source;

public class StAXSource implements Source
{
    public static final String XMLSTREAMREADER = "javax.xml.transform.stax.StAXSource/XMLStreamReader";
    public static final String XMLEVENTREADER = "javax.xml.transform.stax.StAXSource/XMLEventReader";

    private XMLStreamReader streamReader;
    private XMLEventReader eventReader;

    public StAXSource(XMLStreamReader reader)
    {
        this.streamReader = reader;
    }

    public StAXSource(XMLEventReader reader)
    {
        this.eventReader = reader;
    }

    public XMLStreamReader getXMLStreamReader()
    {
        return streamReader;
    }

    public XMLEventReader getXMLEventReader()
    {
        return eventReader;
    }

    public void setSystemId(String systemId)
    {
        throw new UnsupportedOperationException("StAXSource has no system ID");
    }

    public String getSystemId()
    {
        return null;
    }
}
