package javax.xml.transform;

public interface URIResolver
{
    Source resolve(String href, String base) throws TransformerException;
}
