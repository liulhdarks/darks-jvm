package org.xml.sax;

public class SAXParseException extends SAXException
{
    private String publicId;
    private String systemId;
    private int lineNumber;
    private int columnNumber;

    public SAXParseException(String message, Locator locator)
    {
        this(message, locator.getPublicId(), locator.getSystemId(),
             locator.getLineNumber(), locator.getColumnNumber());
    }

    public SAXParseException(String message, Locator locator, Exception e)
    {
        this(message, locator.getPublicId(), locator.getSystemId(),
             locator.getLineNumber(), locator.getColumnNumber(), e);
    }

    public SAXParseException(String message, String publicId, String systemId,
                             int lineNumber, int columnNumber)
    {
        super(message);
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public SAXParseException(String message, String publicId, String systemId,
                             int lineNumber, int columnNumber, Exception e)
    {
        super(message, e);
        this.publicId = publicId;
        this.systemId = systemId;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public String getPublicId()
    {
        return publicId;
    }

    public String getSystemId()
    {
        return systemId;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public int getColumnNumber()
    {
        return columnNumber;
    }

    public String toString()
    {
        return getClass().getName() + ":" + getLineNumber() + ":" + getColumnNumber()
            + ": " + getMessage();
    }
}
