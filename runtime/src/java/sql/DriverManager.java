package java.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

public class DriverManager
{
    private static final ArrayList<Driver> DRIVERS = new ArrayList<Driver>();
    private static int loginTimeout;
    private static java.io.PrintWriter logWriter;

    private DriverManager()
    {
    }

    public static synchronized void registerDriver(Driver driver)
    {
        if (driver == null)
        {
            throw new NullPointerException("driver");
        }
        DRIVERS.add(driver);
    }

    public static synchronized void deregisterDriver(Driver driver)
    {
        if (driver != null)
        {
            DRIVERS.remove(driver);
        }
    }

    public static synchronized Enumeration<Driver> getDrivers()
    {
        return Collections.enumeration(new ArrayList<Driver>(DRIVERS));
    }

    public static Driver getDriver(String url) throws SQLException
    {
        if (url == null)
        {
            return null;
        }
        Driver[] drivers;
        synchronized (DriverManager.class)
        {
            drivers = DRIVERS.toArray(new Driver[DRIVERS.size()]);
        }
        for (int i = 0; i < drivers.length; i++)
        {
            try
            {
                if (drivers[i].acceptsURL(url))
                {
                    return drivers[i];
                }
            }
            catch (RuntimeException ignored)
            {
            }
        }
        return null;
    }

    public static Connection getConnection(String url) throws SQLException
    {
        return getConnection(url, new Properties());
    }

    public static Connection getConnection(String url, String user, String password) throws SQLException
    {
        Properties info = new Properties();
        if (user != null)
        {
            info.setProperty("user", user);
        }
        if (password != null)
        {
            info.setProperty("password", password);
        }
        return getConnection(url, info);
    }

    public static Connection getConnection(String url, Properties info) throws SQLException
    {
        if (url == null)
        {
            throw new SQLException("The url cannot be null");
        }
        Driver[] drivers;
        synchronized (DriverManager.class)
        {
            drivers = DRIVERS.toArray(new Driver[DRIVERS.size()]);
        }
        SQLException last = null;
        for (int i = 0; i < drivers.length; i++)
        {
            try
            {
                Connection connection = drivers[i].connect(url, info);
                if (connection != null)
                {
                    return connection;
                }
            }
            catch (SQLException e)
            {
                last = e;
            }
        }
        if (last != null)
        {
            throw last;
        }
        throw new SQLException("No suitable driver found for " + url);
    }

    public static int getLoginTimeout()
    {
        return loginTimeout;
    }

    public static void setLoginTimeout(int seconds)
    {
        loginTimeout = seconds;
    }

    public static java.io.PrintWriter getLogWriter()
    {
        return logWriter;
    }

    public static void setLogWriter(java.io.PrintWriter out)
    {
        logWriter = out;
    }

    public static void println(String message)
    {
        java.io.PrintWriter writer = logWriter;
        if (writer != null)
        {
            writer.println(message);
        }
    }
}
