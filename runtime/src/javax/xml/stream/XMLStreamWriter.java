package javax.xml.stream;

public interface XMLStreamWriter
{
    void writeStartElement(String localName) throws XMLStreamException;

    void writeStartElement(String namespaceURI, String localName) throws XMLStreamException;

    void writeEndElement() throws XMLStreamException;

    void writeEndDocument() throws XMLStreamException;

    void writeCharacters(String text) throws XMLStreamException;

    void writeAttribute(String localName, String value) throws XMLStreamException;

    void flush() throws XMLStreamException;

    void close() throws XMLStreamException;
}
