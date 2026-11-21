package java.util.logging;

import java.util.Properties;

// Minimal LogManager: exposes the singleton and logger factory used by
// libraries that inspect the JUL configuration.
public class LogManager
{
    private static final LogManager INSTANCE = new LogManager();

    private final Properties properties = new Properties();

    protected LogManager()
    {
    }

    public static LogManager getLogManager()
    {
        return INSTANCE;
    }

    public Logger getLogger(String name)
    {
        return Logger.getLogger(name);
    }

    public String getProperty(String name)
    {
        return properties.getProperty(name);
    }

    public void readConfiguration()
    {
    }

    public void reset()
    {
    }
}
