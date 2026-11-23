package javax.xml.stream.events;

import java.util.Iterator;

import javax.xml.namespace.QName;

public interface EndElement extends XMLEvent
{
    QName getName();

    Iterator getAttributes();

    Iterator getNamespaces();
}
