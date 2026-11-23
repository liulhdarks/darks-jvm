package javax.xml.stream.events;

public interface EntityDeclaration extends XMLEvent
{
    String getPublicId();

    String getSystemId();

    String getName();

    String getNotationName();

    String getReplacementText();

    String getBaseURI();
}
