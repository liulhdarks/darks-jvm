package javax.xml.stream;

public interface XMLStreamReader extends XMLStreamConstants
{
    int next() throws XMLStreamException;

    void close() throws XMLStreamException;

    boolean hasNext() throws XMLStreamException;

    int getEventType();

    String getLocalName();

    String getNamespaceURI();

    String getPrefix();

    String getText();

    int getAttributeCount();

    String getAttributeLocalName(int index);

    String getAttributeValue(int index);

    boolean isStartElement();

    boolean isEndElement();

    boolean isCharacters();

    boolean isWhiteSpace();

    boolean hasName();

    Location getLocation();
}
