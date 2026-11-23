package org.w3c.dom;

public interface ProcessingInstruction extends Node
{
    String getTarget();

    String getData();

    void setData(String data) throws DOMException;
}
