package java.io;

public class StreamCorruptedException extends ObjectStreamException
{
    public StreamCorruptedException()
    {
        super();
    }

    public StreamCorruptedException(String reason)
    {
        super(reason);
    }
}
