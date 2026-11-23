package javax.xml.transform;

public interface ErrorListener
{
    void warning(TransformerException exception) throws TransformerException;

    void error(TransformerException exception) throws TransformerException;

    void fatalError(TransformerException exception) throws TransformerException;
}
