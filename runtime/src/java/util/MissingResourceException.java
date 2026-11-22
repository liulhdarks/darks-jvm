package java.util;

public class MissingResourceException extends RuntimeException
{
    private final String className;
    private final String key;

    public MissingResourceException(String message, String className, String key)
    {
        super(message);
        this.className = className;
        this.key = key;
    }

    public String getClassName()
    {
        return className;
    }

    public String getKey()
    {
        return key;
    }
}
