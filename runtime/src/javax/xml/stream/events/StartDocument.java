package javax.xml.stream.events;

public interface StartDocument extends XMLEvent
{
    String getSystemId();

    String getCharacterEncodingScheme();

    boolean isStandalone();

    boolean standaloneSet();

    String getVersion();

    String getEncoding();
}
