package org.w3c.dom;

public interface Text extends CharacterData
{
    Text splitText(int offset) throws DOMException;

    boolean isElementContentWhitespace();

    String getWholeText();

    Text replaceWholeText(String content) throws DOMException;
}
