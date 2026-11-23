package java.io;

public class NotSerializableException extends ObjectStreamException
{
    private final String className;

    public NotSerializableException()
    {
        super();
        this.className = null;
    }

    public NotSerializableException(String className)
    {
        super(className);
        this.className = className;
    }

    public String getMessage()
    {
        return className;
    }
}
