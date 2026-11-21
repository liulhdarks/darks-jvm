package java.util.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Minimal hierarchical logger.  The root logger owns one ConsoleHandler at
// INFO, matching the default java.util.logging setup closely enough for
// libraries that only rely on getLogger/log/isLoggable.
public class Logger
{
    public static final String GLOBAL_LOGGER_NAME = "global";

    private static final Map<String, Logger> loggers = new HashMap<String, Logger>();
    private static final Logger ROOT = createRoot();
    public static final Logger global = getLogger(GLOBAL_LOGGER_NAME);

    private final String name;
    private final List<Handler> handlers = new ArrayList<Handler>();
    private Level level;
    private Logger parent;
    private boolean useParentHandlers = true;

    private Logger(String name, Logger parent)
    {
        this.name = name;
        this.parent = parent;
    }

    private static Logger createRoot()
    {
        Logger root = new Logger("", null);
        root.setLevel(Level.INFO);
        root.addHandler(new ConsoleHandler());
        return root;
    }

    public static Logger getLogger(String name)
    {
        if (name == null)
        {
            throw new NullPointerException("name");
        }
        if (name.length() == 0)
        {
            return ROOT;
        }
        synchronized (loggers)
        {
            Logger existing = loggers.get(name);
            if (existing != null)
            {
                return existing;
            }
            int dot = name.lastIndexOf('.');
            Logger parent = dot < 0 ? ROOT : getLogger(name.substring(0, dot));
            Logger created = new Logger(name, parent);
            loggers.put(name, created);
            return created;
        }
    }

    public static Logger getLogger(String name, String resourceBundleName)
    {
        return getLogger(name);
    }

    public static Logger getAnonymousLogger()
    {
        return new Logger("", ROOT);
    }

    public static Logger getGlobal()
    {
        return global;
    }

    public String getName()
    {
        return name;
    }

    public String getResourceBundleName()
    {
        return null;
    }

    public java.util.ResourceBundle getResourceBundle()
    {
        return null;
    }

    public Logger getParent()
    {
        return parent;
    }

    public void setParent(Logger parent)
    {
        this.parent = parent;
    }

    public Level getLevel()
    {
        return level;
    }

    public void setLevel(Level level)
    {
        this.level = level;
    }

    public void addHandler(Handler handler)
    {
        if (handler == null)
        {
            throw new NullPointerException("handler");
        }
        handlers.add(handler);
    }

    public void removeHandler(Handler handler)
    {
        handlers.remove(handler);
    }

    public Handler[] getHandlers()
    {
        Handler[] result = new Handler[handlers.size()];
        for (int i = 0; i < handlers.size(); i++)
        {
            result[i] = handlers.get(i);
        }
        return result;
    }

    public void setUseParentHandlers(boolean useParentHandlers)
    {
        this.useParentHandlers = useParentHandlers;
    }

    public boolean getUseParentHandlers()
    {
        return useParentHandlers;
    }

    private Level effectiveLevel()
    {
        Logger current = this;
        while (current != null)
        {
            if (current.level != null)
            {
                return current.level;
            }
            current = current.parent;
        }
        return Level.INFO;
    }

    public boolean isLoggable(Level level)
    {
        if (level == null)
        {
            throw new NullPointerException("level");
        }
        return level.intValue() >= effectiveLevel().intValue();
    }

    public void log(Level level, String message)
    {
        if (!isLoggable(level))
        {
            return;
        }
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(name);
        publish(record);
    }

    public void log(Level level, String message, Object parameter)
    {
        if (!isLoggable(level))
        {
            return;
        }
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(name);
        record.setParameters(new Object[] { parameter });
        publish(record);
    }

    public void log(Level level, String message, Object[] parameters)
    {
        if (!isLoggable(level))
        {
            return;
        }
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(name);
        record.setParameters(parameters);
        publish(record);
    }

    public void log(Level level, String message, Throwable thrown)
    {
        if (!isLoggable(level))
        {
            return;
        }
        LogRecord record = new LogRecord(level, message);
        record.setLoggerName(name);
        record.setThrown(thrown);
        publish(record);
    }

    public void log(LogRecord record)
    {
        if (record == null)
        {
            throw new NullPointerException("record");
        }
        if (!isLoggable(record.getLevel()))
        {
            return;
        }
        if (record.getLoggerName() == null)
        {
            record.setLoggerName(name);
        }
        publish(record);
    }

    private void publish(LogRecord record)
    {
        for (int i = 0; i < handlers.size(); i++)
        {
            handlers.get(i).publish(record);
        }
        if (useParentHandlers && parent != null)
        {
            parent.publish(record);
        }
    }

    public void severe(String message)
    {
        log(Level.SEVERE, message);
    }

    public void warning(String message)
    {
        log(Level.WARNING, message);
    }

    public void info(String message)
    {
        log(Level.INFO, message);
    }

    public void config(String message)
    {
        log(Level.CONFIG, message);
    }

    public void fine(String message)
    {
        log(Level.FINE, message);
    }

    public void finer(String message)
    {
        log(Level.FINER, message);
    }

    public void finest(String message)
    {
        log(Level.FINEST, message);
    }

    public String toString()
    {
        return "Logger[" + name + "]";
    }
}
