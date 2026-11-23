package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.QName;

public interface StartElement extends XMLEvent
{
    QName getName();

    Iterator getAttributes();

    Iterator getNamespaces();

    Namespace getNamespaceContext();

    String getNamespaceURI(String prefix);

    Attribute getAttributeByName(QName name);
}
