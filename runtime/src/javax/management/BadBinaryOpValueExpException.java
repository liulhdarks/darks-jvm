package javax.management;

public class BadBinaryOpValueExpException extends Exception
{
    private final Object exp;

    public BadBinaryOpValueExpException(Object exp)
    {
        super(String.valueOf(exp));
        this.exp = exp;
    }

    public String toString()
    {
        return "BadBinaryOpValueExpException: " + exp;
    }
}
