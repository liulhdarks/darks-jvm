package javax.xml.transform;

public abstract class Transformer
{
    protected Transformer()
    {
    }

    public abstract void transform(Source xmlSource, Result outputTarget)
        throws TransformerException;

    public abstract void setParameter(String name, Object value);

    public abstract Object getParameter(String name);

    public abstract void clearParameters();

    public abstract void setOutputProperty(String name, String value)
        throws IllegalArgumentException;

    public abstract String getOutputProperty(String name)
        throws IllegalArgumentException;

    public abstract void setOutputProperties(java.util.Properties properties);

    public abstract java.util.Properties getOutputProperties();

    public abstract void setURIResolver(URIResolver resolver);

    public abstract URIResolver getURIResolver();

    public abstract void setErrorListener(ErrorListener listener)
        throws IllegalArgumentException;

    public abstract ErrorListener getErrorListener();

    public void reset()
    {
        throw new UnsupportedOperationException("reset");
    }
}
