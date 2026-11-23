package javax.xml.stream.events;

public interface Characters extends XMLEvent
{
    String getData();

    boolean isWhiteSpace();

    boolean isCData();

    boolean isIgnorableWhiteSpace();
}
