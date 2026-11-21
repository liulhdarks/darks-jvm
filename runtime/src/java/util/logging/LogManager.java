package java.util.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

public class LogManager
{
    private static final LogManager INSTANCE = new LogManager();

    private final Properties properties = new Properties();

    public static LogManager getLogManager()
    {
        return INSTANCE;
    }

    public void reset()
    {
    }

    public void readConfiguration() throws IOException
    {
    }

    public void readConfiguration(InputStream input) throws IOException
    {
        if (input != null)
        {
            properties.load(input);
        }
    }

    public String getProperty(String name)
    {
        return properties.getProperty(name);
    }

    public Enumeration<String> getLoggerNames()
    {
        List<String> names = new ArrayList<String>();
        return Collections.enumeration(names);
    }

    public Logger getLogger(String name)
    {
        return Logger.getLogger(name);
    }
}
