package java.util.logging;

public class LogRecord
{
    private Level level;
    private String loggerName;
    private String message;
    private long millis;
    private Throwable thrown;
    private Object[] parameters;

    public LogRecord(Level level, String message)
    {
        this.level = level;
        this.message = message;
        this.millis = System.currentTimeMillis();
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
    }

    public String getLoggerName()
    {
        return loggerName;
    }

    public void setLoggerName(String loggerName)
    {
        this.loggerName = loggerName;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public long getMillis()
    {
        return millis;
    }

    public void setMillis(long millis)
    {
        this.millis = millis;
    }

    public Throwable getThrown()
    {
        return thrown;
    }

    public void setThrown(Throwable thrown)
    {
        this.thrown = thrown;
    }

    public Object[] getParameters()
    {
        return parameters;
    }

    public void setParameters(Object[] parameters)
    {
        this.parameters = parameters;
    }
}
