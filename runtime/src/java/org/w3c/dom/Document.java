package org.w3c.dom;

public interface Document extends Node
{
    Element getDocumentElement();

    Element createElement(String tagName) throws DOMException;

    Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException;

    DocumentFragment createDocumentFragment();

    Text createTextNode(String data);

    Comment createComment(String data);

    CDATASection createCDATASection(String data) throws DOMException;

    ProcessingInstruction createProcessingInstruction(String target, String data)
        throws DOMException;

    Attr createAttribute(String name) throws DOMException;

    Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException;

    NodeList getElementsByTagName(String name);

    NodeList getElementsByTagNameNS(String namespaceURI, String localName);

    Node importNode(Node importedNode, boolean deep) throws DOMException;

    Element getElementById(String elementId);

    String getInputEncoding();

    String getXmlEncoding();

    boolean getXmlStandalone();

    void setXmlStandalone(boolean xmlStandalone) throws DOMException;

    String getXmlVersion();

    void setXmlVersion(String xmlVersion) throws DOMException;

    boolean getStrictErrorChecking();

    void setStrictErrorChecking(boolean strictErrorChecking);

    String getDocumentURI();

    void setDocumentURI(String documentURI);

    Node adoptNode(Node source) throws DOMException;

    void normalizeDocument();
}
