package javax.xml.stream.events;

import javax.xml.namespace.QName;

public interface Attribute extends XMLEvent
{
    QName getName();

    String getValue();

    String getDTDType();

    boolean isSpecified();
}
