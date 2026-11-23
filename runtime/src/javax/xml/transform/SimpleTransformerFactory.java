package javax.xml.transform;

class SimpleTransformerFactory extends TransformerFactory
{
    private URIResolver resolver;
    private ErrorListener listener;

    public Transformer newTransformer() throws TransformerConfigurationException
    {
        return new UnsupportedTransformer();
    }

    public Transformer newTransformer(Source source)
        throws TransformerConfigurationException
    {
        return new UnsupportedTransformer();
    }

    public Templates newTemplates(Source source)
        throws TransformerConfigurationException
    {
        throw new TransformerConfigurationException("XSLT is not supported");
    }

    public Source getAssociatedStylesheet(Source source, String media,
        String title, String charset) throws TransformerConfigurationException
    {
        throw new TransformerConfigurationException("XSLT is not supported");
    }

    public void setURIResolver(URIResolver resolver)
    {
        this.resolver = resolver;
    }

    public URIResolver getURIResolver()
    {
        return resolver;
    }

    public void setFeature(String name, boolean value)
        throws TransformerConfigurationException
    {
        throw new TransformerConfigurationException(
            "Unknown feature: " + name);
    }

    public boolean getFeature(String name)
    {
        throw new IllegalArgumentException("Unknown feature: " + name);
    }

    public void setAttribute(String name, Object value)
    {
        throw new IllegalArgumentException("Unknown attribute: " + name);
    }

    public Object getAttribute(String name)
    {
        throw new IllegalArgumentException("Unknown attribute: " + name);
    }

    public void setErrorListener(ErrorListener listener) throws IllegalArgumentException
    {
        this.listener = listener;
    }

    public ErrorListener getErrorListener()
    {
        return listener;
    }
}
