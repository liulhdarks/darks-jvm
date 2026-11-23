package org.xml.sax;

public interface AttributeList
{
    int getLength();

    String getName(int i);

    String getType(int i);

    String getValue(int i);

    String getType(String name);

    String getValue(String name);
}
