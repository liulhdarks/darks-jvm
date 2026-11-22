package javax.management;

public class BadAttributeValueExpException extends Exception
{
    private final Object value;

    public BadAttributeValueExpException(Object value)
    {
        super(String.valueOf(value));
        this.value = value;
    }

    public String toString()
    {
        return "BadAttributeValueExpException: " + value;
    }
}
