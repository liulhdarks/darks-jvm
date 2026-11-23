package javax.xml.stream.events;

public interface ProcessingInstruction extends XMLEvent
{
    String getTarget();

    String getData();
}
