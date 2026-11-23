package org.xml.sax;

import java.io.IOException;

public interface XMLReader
{
    boolean getFeature(String name) throws SAXNotRecognizedException, SAXNotSupportedException;

    void setFeature(String name, boolean value)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    Object getProperty(String name)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    void setProperty(String name, Object value)
        throws SAXNotRecognizedException, SAXNotSupportedException;

    void setEntityResolver(EntityResolver resolver);

    EntityResolver getEntityResolver();

    void setDTDHandler(DTDHandler handler);

    DTDHandler getDTDHandler();

    void setContentHandler(ContentHandler handler);

    ContentHandler getContentHandler();

    void setErrorHandler(ErrorHandler handler);

    ErrorHandler getErrorHandler();

    void parse(InputSource input) throws IOException, SAXException;

    void parse(String systemId) throws IOException, SAXException;
}
