package javax.xml.stream;

import javax.xml.stream.events.XMLEvent;

public interface XMLEventReader
{
    boolean hasNext();

    XMLEvent nextEvent() throws XMLStreamException;

    Object next() throws XMLStreamException;

    void close() throws XMLStreamException;

    Object getProperty(String name) throws IllegalArgumentException;
}
