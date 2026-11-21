package java.util.logging;

public class LogRecord
{
    private Level level;
    private String loggerName;
    private String message;
    private long millis;
    private Throwable thrown;
    private Object[] parameters;
    private String resourceBundleName;
    private java.util.ResourceBundle resourceBundle;
    private long sequenceNumber;
    private String sourceClassName;
    private String sourceMethodName;
    private int threadID;

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

    public String getResourceBundleName()
    {
        return resourceBundleName;
    }

    public void setResourceBundleName(String resourceBundleName)
    {
        this.resourceBundleName = resourceBundleName;
    }

    public java.util.ResourceBundle getResourceBundle()
    {
        return resourceBundle;
    }

    public void setResourceBundle(java.util.ResourceBundle resourceBundle)
    {
        this.resourceBundle = resourceBundle;
    }

    public long getSequenceNumber()
    {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber)
    {
        this.sequenceNumber = sequenceNumber;
    }

    public String getSourceClassName()
    {
        return sourceClassName;
    }

    public void setSourceClassName(String sourceClassName)
    {
        this.sourceClassName = sourceClassName;
    }

    public String getSourceMethodName()
    {
        return sourceMethodName;
    }

    public void setSourceMethodName(String sourceMethodName)
    {
        this.sourceMethodName = sourceMethodName;
    }

    public int getThreadID()
    {
        return threadID;
    }

    public void setThreadID(int threadID)
    {
        this.threadID = threadID;
    }
}
