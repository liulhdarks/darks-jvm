package javax.xml.stream;

public interface XMLEventWriter
{
    void add(Object event) throws XMLStreamException;

    void flush() throws XMLStreamException;

    void close() throws XMLStreamException;
}
