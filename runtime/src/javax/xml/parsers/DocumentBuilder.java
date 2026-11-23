package javax.xml.parsers;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentBuilder
{
    protected DocumentBuilder()
    {
    }

    public Document parse(InputSource input) throws SAXException, java.io.IOException
    {
        throw new UnsupportedOperationException("DOM parsing is not supported");
    }

    public Document parse(java.io.InputStream input) throws SAXException, java.io.IOException
    {
        throw new UnsupportedOperationException("DOM parsing is not supported");
    }

    public Document parse(String uri) throws SAXException, java.io.IOException
    {
        throw new UnsupportedOperationException("DOM parsing is not supported");
    }

    public Document newDocument()
    {
        throw new UnsupportedOperationException("DOM is not supported");
    }

    public void setEntityResolver(org.xml.sax.EntityResolver resolver)
    {
    }

    public void setErrorHandler(org.xml.sax.ErrorHandler handler)
    {
    }
}
