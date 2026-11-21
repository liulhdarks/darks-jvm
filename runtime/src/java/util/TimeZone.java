package java.util;

/**
 * Time zone with a fixed raw offset from UTC.  The default zone is GMT.
 */
public class TimeZone implements java.io.Serializable, Cloneable
{
    public static final int SHORT = 0;
    public static final int LONG = 1;

    private static final TimeZone GMT = new TimeZone("GMT", 0);
    private static TimeZone defaultZone = GMT;

    private String id;
    private int rawOffset;

    public TimeZone()
    {
        this.id = "GMT";
    }

    private TimeZone(String id, int rawOffset)
    {
        this.id = id;
        this.rawOffset = rawOffset;
    }

    public static synchronized TimeZone getDefault()
    {
        return defaultZone;
    }

    public static synchronized void setDefault(TimeZone zone)
    {
        defaultZone = zone == null ? GMT : zone;
    }

    public static String[] getAvailableIDs()
    {
        return new String[] {"GMT", "UTC"};
    }

    public static String[] getAvailableIDs(int rawOffset)
    {
        return new String[] {"GMT"};
    }

    public static TimeZone getTimeZone(String id)
    {
        if (id == null)
        {
            return GMT;
        }
        String value = id.trim();
        if (value.length() == 0 || value.equals("GMT") || value.equals("UTC")
                || value.equals("Z") || value.equals("UT"))
        {
            return new TimeZone("GMT", 0);
        }
        if (value.startsWith("GMT") || value.startsWith("UTC"))
        {
            String rest = value.substring(3);
            if (rest.length() > 0)
            {
                try
                {
                    int minutes = parseOffset(rest);
                    return new TimeZone(value, minutes * 60000);
                }
                catch (RuntimeException ex)
                {
                    return new TimeZone("GMT", 0);
                }
            }
        }
        return new TimeZone(value, 0);
    }

    private static int parseOffset(String value)
    {
        int sign = 1;
        int index = 0;
        if (value.charAt(0) == '+')
        {
            index = 1;
        }
        else if (value.charAt(0) == '-')
        {
            sign = -1;
            index = 1;
        }
        String rest = value.substring(index);
        int hours;
        int minutes = 0;
        int colon = rest.indexOf(':');
        if (colon >= 0)
        {
            hours = Integer.parseInt(rest.substring(0, colon));
            minutes = Integer.parseInt(rest.substring(colon + 1));
        }
        else if (rest.length() > 2)
        {
            hours = Integer.parseInt(rest.substring(0, rest.length() - 2));
            minutes = Integer.parseInt(rest.substring(rest.length() - 2));
        }
        else
        {
            hours = Integer.parseInt(rest);
        }
        return sign * (hours * 60 + minutes);
    }

    public String getID()
    {
        return id;
    }

    public void setID(String id)
    {
        if (id == null)
        {
            throw new NullPointerException("id");
        }
        this.id = id;
    }

    public int getRawOffset()
    {
        return rawOffset;
    }

    public void setRawOffset(int offsetMillis)
    {
        this.rawOffset = offsetMillis;
    }

    public int getOffset(long date)
    {
        return rawOffset;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
                        int milliseconds)
    {
        return rawOffset;
    }

    public int getDSTSavings()
    {
        return 0;
    }

    public boolean inDaylightTime(Date date)
    {
        return false;
    }

    public boolean useDaylightTime()
    {
        return false;
    }

    public boolean observesDaylightTime()
    {
        return false;
    }

    public boolean hasSameRules(TimeZone other)
    {
        return other != null && other.rawOffset == rawOffset;
    }

    public String getDisplayName()
    {
        return id;
    }

    public String getDisplayName(Locale locale)
    {
        return id;
    }

    public String getDisplayName(boolean daylight, int style)
    {
        return id;
    }

    public String getDisplayName(boolean daylight, int style, Locale locale)
    {
        return id;
    }

    public Object clone()
    {
        return new TimeZone(id, rawOffset);
    }

    public boolean equals(Object other)
    {
        return other instanceof TimeZone && ((TimeZone)other).id.equals(id);
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toString()
    {
        return getClass().getName() + "[id=" + id + ",offset=" + rawOffset + "]";
    }
}
