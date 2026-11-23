package org.xml.sax;

public interface Attributes
{
    int getLength();

    String getURI(int index);

    String getLocalName(int index);

    String getQName(int index);

    String getType(int index);

    String getValue(int index);

    int getIndex(String qName);

    int getIndex(String uri, String localName);

    String getType(String qName);

    String getType(String uri, String localName);

    String getValue(String qName);

    String getValue(String uri, String localName);
}
