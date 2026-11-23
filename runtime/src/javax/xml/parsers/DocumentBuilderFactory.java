package javax.xml.parsers;

public class DocumentBuilderFactory
{
    private boolean namespaceAware;
    private boolean validating;
    private String schemaLanguage;

    protected DocumentBuilderFactory()
    {
    }

    public static DocumentBuilderFactory newInstance()
        throws FactoryConfigurationError
    {
        return new DocumentBuilderFactory();
    }

    public static DocumentBuilderFactory newInstance(String factoryClassName,
        ClassLoader classLoader) throws FactoryConfigurationError
    {
        return newInstance();
    }

    public DocumentBuilder newDocumentBuilder() throws ParserConfigurationException
    {
        return new DocumentBuilder();
    }

    public void setNamespaceAware(boolean awareness)
    {
        namespaceAware = awareness;
    }

    public boolean isNamespaceAware()
    {
        return namespaceAware;
    }

    public void setValidating(boolean validating)
    {
        this.validating = validating;
    }

    public boolean isValidating()
    {
        return validating;
    }

    public void setFeature(String name, boolean value)
        throws ParserConfigurationException
    {
        throw new ParserConfigurationException("Unknown feature: " + name);
    }

    public boolean getFeature(String name)
        throws ParserConfigurationException
    {
        throw new ParserConfigurationException("Unknown feature: " + name);
    }

    public void setAttribute(String name, Object value)
        throws IllegalArgumentException
    {
        throw new IllegalArgumentException("Unknown attribute: " + name);
    }

    public Object getAttribute(String name) throws IllegalArgumentException
    {
        throw new IllegalArgumentException("Unknown attribute: " + name);
    }

    public boolean isXIncludeAware()
    {
        return false;
    }

    public void setXIncludeAware(boolean state)
    {
        if (state)
        {
            throw new UnsupportedOperationException("XInclude is not supported");
        }
    }
}
