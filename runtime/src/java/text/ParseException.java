package java.text;

/** Signals a failure while parsing formatted text. */
public class ParseException extends Exception
{
    private final int errorOffset;

    public ParseException(String message, int errorOffset)
    {
        super(message);
        this.errorOffset = errorOffset;
    }

    public int getErrorOffset()
    {
        return errorOffset;
    }
}
