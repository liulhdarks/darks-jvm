package java.util.logging;

public abstract class Formatter
{
    public abstract String format(LogRecord record);

    public String getHead(Handler handler)
    {
        return "";
    }

    public String getTail(Handler handler)
    {
        return "";
    }
}
