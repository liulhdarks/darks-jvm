package javax.xml.stream;

import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.DTD;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.EntityDeclaration;
import javax.xml.stream.events.EntityReference;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.ProcessingInstruction;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.StartElement;

public class XMLEventFactory
{
    protected XMLEventFactory()
    {
    }

    public static XMLEventFactory newInstance()
    {
        return new XMLEventFactory();
    }

    public static XMLEventFactory newFactory()
    {
        return new XMLEventFactory();
    }

    public void setLocation(Location location)
    {
    }

    private static UnsupportedOperationException unsupported()
    {
        return new UnsupportedOperationException("StAX event creation is not supported");
    }

    public Attribute createAttribute(String prefix, String namespaceURI,
        String localName, String value)
    {
        throw unsupported();
    }

    public Attribute createAttribute(QName name, String value)
    {
        throw unsupported();
    }

    public Namespace createNamespace(String prefix, String namespaceUri)
    {
        throw unsupported();
    }

    public Namespace createNamespace(String namespaceUri)
    {
        throw unsupported();
    }

    public StartElement createStartElement(String prefix, String namespaceUri,
        String localName)
    {
        throw unsupported();
    }

    public StartElement createStartElement(String prefix, String namespaceUri,
        String localName, Iterator attributes, Iterator namespaces)
    {
        throw unsupported();
    }

    public EndElement createEndElement(String prefix, String namespaceUri,
        String localName)
    {
        throw unsupported();
    }

    public EndElement createEndElement(String prefix, String namespaceUri,
        String localName, Iterator namespaces)
    {
        throw unsupported();
    }

    public javax.xml.stream.events.Characters createCharacters(String content)
    {
        throw unsupported();
    }

    public javax.xml.stream.events.Characters createCData(String content)
    {
        throw unsupported();
    }

    public javax.xml.stream.events.Characters createIgnorableSpace(String content)
    {
        throw unsupported();
    }

    public javax.xml.stream.events.Characters createSpace(String content)
    {
        throw unsupported();
    }

    public StartDocument createStartDocument()
    {
        throw unsupported();
    }

    public StartDocument createStartDocument(String encoding)
    {
        throw unsupported();
    }

    public StartDocument createStartDocument(String encoding, String version)
    {
        throw unsupported();
    }

    public StartDocument createStartDocument(String encoding, String version,
        boolean standalone)
    {
        throw unsupported();
    }

    public EndDocument createEndDocument()
    {
        throw unsupported();
    }

    public EntityReference createEntityReference(String name,
        EntityDeclaration declaration)
    {
        throw unsupported();
    }

    public Comment createComment(String text)
    {
        throw unsupported();
    }

    public ProcessingInstruction createProcessingInstruction(String target,
        String data)
    {
        throw unsupported();
    }

    public DTD createDTD(String dtd)
    {
        throw unsupported();
    }
}
