package org.xml.sax;

import java.io.IOException;

public interface EntityResolver
{
    InputSource resolveEntity(String publicId, String systemId)
        throws SAXException, IOException;
}
