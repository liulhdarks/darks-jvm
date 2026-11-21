package java.time.format;

import java.time.DateTimeException;

/** Thrown when text cannot be parsed by a DateTimeFormatter. */
public class DateTimeParseException extends DateTimeException
{
    private final String parsedString;
    private final int errorIndex;

    public DateTimeParseException(String message, CharSequence parsedData, int errorIndex)
    {
        super(message);
        this.parsedString = parsedData == null ? null : parsedData.toString();
        this.errorIndex = errorIndex;
    }

    public DateTimeParseException(String message, CharSequence parsedData,
                                  int errorIndex, Throwable cause)
    {
        super(message);
        this.parsedString = parsedData == null ? null : parsedData.toString();
        this.errorIndex = errorIndex;
    }

    public String getParsedString()
    {
        return parsedString;
    }

    public int getErrorIndex()
    {
        return errorIndex;
    }
}
