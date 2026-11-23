package org.xml.sax;

public interface DTDHandler
{
    void notationDecl(String name, String publicId, String systemId)
        throws SAXException;

    void unparsedEntityDecl(String name, String publicId, String systemId,
        String notationName) throws SAXException;
}
