package java.util.logging;

public class ConsoleHandler extends Handler
{
    public ConsoleHandler()
    {
        setLevel(Level.INFO);
        setFormatter(new SimpleFormatter());
    }

    public void publish(LogRecord record)
    {
        if (!isLoggable(record))
        {
            return;
        }
        String text = getFormatter().format(record);
        System.err.print(text);
    }

    public void flush()
    {
        System.err.flush();
    }

    public void close()
    {
    }
}
