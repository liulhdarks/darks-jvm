package java.io;

public class WriteAbortedException extends ObjectStreamException
{
    public final Exception detail;

    public WriteAbortedException(String message, Exception cause)
    {
        super(message);
        this.detail = cause;
    }

    public Throwable getCause()
    {
        return detail;
    }

    public String getMessage()
    {
        return detail == null ? super.getMessage()
            : super.getMessage() + "; " + detail.getMessage();
    }
}
