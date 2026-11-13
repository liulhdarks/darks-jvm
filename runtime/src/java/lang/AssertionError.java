package java.lang;

public class AssertionError extends Error
{
    public AssertionError()
    {
        super();
    }

    private static String messageOf(Object detailMessage)
    {
        return detailMessage == null ? null : String.valueOf(detailMessage);
    }

    public AssertionError(Object detailMessage)
    {
        super(messageOf(detailMessage));
    }

    public AssertionError(String detailMessage)
    {
        super(detailMessage);
    }

    public AssertionError(boolean detailMessage)
    {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(char detailMessage)
    {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(int detailMessage)
    {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(long detailMessage)
    {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(float detailMessage)
    {
        super(String.valueOf(detailMessage));
    }

    public AssertionError(double detailMessage)
    {
        super(String.valueOf(detailMessage));
    }
}
