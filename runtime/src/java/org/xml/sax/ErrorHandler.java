package org.xml.sax;

public interface ErrorHandler
{
    void warning(SAXParseException exception) throws SAXException;

    void error(SAXParseException exception) throws SAXException;

    void fatalError(SAXParseException exception) throws SAXException;
}
