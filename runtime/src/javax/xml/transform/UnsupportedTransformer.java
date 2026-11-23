package javax.xml.transform;

class UnsupportedTransformer extends Transformer
{
    public void transform(Source xmlSource, Result outputTarget)
        throws TransformerException
    {
        throw new TransformerException("XSLT is not supported");
    }

    public void setParameter(String name, Object value)
    {
    }

    public Object getParameter(String name)
    {
        return null;
    }

    public void clearParameters()
    {
    }

    public void setOutputProperty(String name, String value)
        throws IllegalArgumentException
    {
    }

    public String getOutputProperty(String name) throws IllegalArgumentException
    {
        return null;
    }

    public void setOutputProperties(java.util.Properties properties)
    {
    }

    public java.util.Properties getOutputProperties()
    {
        return new java.util.Properties();
    }

    public void setURIResolver(URIResolver resolver)
    {
    }

    public URIResolver getURIResolver()
    {
        return null;
    }

    public void setErrorListener(ErrorListener listener)
        throws IllegalArgumentException
    {
    }

    public ErrorListener getErrorListener()
    {
        return null;
    }
}
