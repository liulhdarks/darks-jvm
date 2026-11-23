package org.w3c.dom;

public interface Attr extends Node
{
    String getName();

    boolean getSpecified();

    String getValue();

    void setValue(String value) throws DOMException;

    Element getOwnerElement();

    boolean isId();
}
