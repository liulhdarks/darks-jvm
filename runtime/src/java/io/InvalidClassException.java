package java.io;

public class InvalidClassException extends ObjectStreamException
{
    private final String className;

    public InvalidClassException(String reason)
    {
        super(reason);
        this.className = null;
    }

    public InvalidClassException(String className, String reason)
    {
        super(reason);
        this.className = className;
    }

    public String getMessage()
    {
        if (className == null)
        {
            return super.getMessage();
        }
        return className + "; " + super.getMessage();
    }
}
