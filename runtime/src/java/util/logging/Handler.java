package java.util.logging;

public abstract class Handler
{
    private Level level = Level.ALL;
    private Formatter formatter;

    public abstract void publish(LogRecord record);

    public abstract void flush();

    public abstract void close();

    public boolean isLoggable(LogRecord record)
    {
        return record != null && record.getLevel().intValue() >= level.intValue();
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        if (level == null)
        {
            throw new NullPointerException("level");
        }
        this.level = level;
    }

    public Formatter getFormatter()
    {
        return formatter;
    }

    public void setFormatter(Formatter formatter)
    {
        this.formatter = formatter;
    }
}
