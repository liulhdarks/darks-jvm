package java.time;

/** Thrown when a date/time string cannot be parsed by the ISO parsers. */
public class DateTimeException extends RuntimeException
{
    public DateTimeException()
    {
        super();
    }

    public DateTimeException(String message)
    {
        super(message);
    }
}
