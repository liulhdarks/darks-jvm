package javax.xml.stream;

public class XMLOutputFactory
{
    protected XMLOutputFactory()
    {
    }

    public static XMLOutputFactory newInstance()
    {
        return new XMLOutputFactory();
    }

    public XMLStreamWriter createXMLStreamWriter(java.io.Writer writer)
        throws XMLStreamException
    {
        throw new XMLStreamException("StAX writing is not supported");
    }

    public XMLEventWriter createXMLEventWriter(java.io.Writer writer)
        throws XMLStreamException
    {
        throw new XMLStreamException("StAX writing is not supported");
    }
}
