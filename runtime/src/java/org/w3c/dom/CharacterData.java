package org.w3c.dom;

public interface CharacterData extends Node
{
    String getData() throws DOMException;

    void setData(String data) throws DOMException;

    int getLength();

    String substringData(int offset, int count) throws DOMException;

    void appendData(String arg) throws DOMException;

    void insertData(int offset, String arg) throws DOMException;

    void deleteData(int offset, int count) throws DOMException;

    void replaceData(int offset, int count, String arg) throws DOMException;
}
