package java.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

public class DriverManager
{
    private static final ArrayList<Driver> DRIVERS = new ArrayList<Driver>();

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
}
