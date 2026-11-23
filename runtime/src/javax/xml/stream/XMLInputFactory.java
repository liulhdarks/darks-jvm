package javax.xml.stream;

public class XMLInputFactory
{
    public static final String IS_NAMESPACE_AWARE = "javax.xml.stream.isNamespaceAware";
    public static final String IS_VALIDATING = "javax.xml.stream.isValidating";
    public static final String IS_COALESCING = "javax.xml.stream.isCoalescing";
    public static final String SUPPORT_DTD = "javax.xml.stream.supportDTD";
    public static final String IS_REPLACING_ENTITY_REFERENCES =
        "javax.xml.stream.isReplacingEntityReferences";
    public static final String IS_SUPPORTING_EXTERNAL_ENTITIES =
        "javax.xml.stream.isSupportingExternalEntities";
    public static final String REPORTER = "javax.xml.stream.reporter";
    public static final String RESOLVER = "javax.xml.stream.resolver";
    public static final String ALLOCATOR = "javax.xml.stream.allocator";

    protected XMLInputFactory()
    {
    }

    public static XMLInputFactory newInstance()
    {
        return new XMLInputFactory();
    }

    public XMLStreamReader createXMLStreamReader(java.io.InputStream stream)
        throws XMLStreamException
    {
        throw new XMLStreamException("StAX parsing is not supported");
    }

    public XMLEventReader createXMLEventReader(java.io.InputStream stream)
        throws XMLStreamException
    {
        throw new XMLStreamException("StAX parsing is not supported");
    }

    public void setXMLResolver(XMLResolver resolver)
    {
    }

    public void setProperty(String name, Object value) throws IllegalArgumentException
    {
        throw new IllegalArgumentException("Unsupported property: " + name);
    }

    public Object getProperty(String name) throws IllegalArgumentException
    {
        throw new IllegalArgumentException("Unsupported property: " + name);
    }
}
