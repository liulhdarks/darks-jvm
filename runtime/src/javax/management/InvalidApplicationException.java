package javax.management;

public class InvalidApplicationException extends Exception
{
    private final Object val;

    public InvalidApplicationException(Object val)
    {
        super(String.valueOf(val));
        this.val = val;
    }
}
