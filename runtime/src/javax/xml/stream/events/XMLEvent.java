package javax.xml.stream.events;

import javax.xml.stream.XMLStreamConstants;

public interface XMLEvent extends XMLStreamConstants
{
    int getEventType();

    boolean isStartElement();

    boolean isEndElement();

    boolean isCharacters();

    boolean isStartDocument();

    boolean isEndDocument();
}
