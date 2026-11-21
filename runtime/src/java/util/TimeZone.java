package java.util;

/**
 * Minimal time-zone value object.  Regions use their standard offset all
 * year, mirroring the reduced java.time implementation.
 */
public class TimeZone implements java.io.Serializable
{
    public static final int SHORT = 0;
    public static final int LONG = 1;

    private String id;
    private int rawOffset;

    public TimeZone()
    {
        this("UTC", 0);
    }

    protected TimeZone(String id, int rawOffset)
    {
        this.id = id;
        this.rawOffset = rawOffset;
    }

    public static TimeZone getDefault()
    {
        return getTimeZone(java.time.ZoneId.systemDefault());
    }

    public static TimeZone getTimeZone(String id)
    {
        if (id == null)
        {
            throw new NullPointerException("id");
        }
        java.time.ZoneId zone = java.time.ZoneId.of(id);
        return new TimeZone(zone.getId(), zone.getOffsetSeconds(0));
    }

    public static TimeZone getTimeZone(java.time.ZoneId zone)
    {
        if (zone == null)
        {
            throw new NullPointerException("zone");
        }
        return new TimeZone(zone.getId(), zone.getOffsetSeconds(0));
    }

    public java.time.ZoneId toZoneId()
    {
        return java.time.ZoneId.of(id);
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
        this.rawOffset = java.time.ZoneId.of(id).getOffsetSeconds(0);
    }

    public int getOffset(long date)
    {
        return rawOffset;
    }

    public int getRawOffset()
    {
        return rawOffset;
    }

    public void setRawOffset(int offsetMillis)
    {
        this.rawOffset = offsetMillis;
    }

    public boolean inDaylightTime(Date date)
    {
        return false;
    }

    public boolean useDaylightTime()
    {
        return false;
    }

    public String getDisplayName()
    {
        return getDisplayName(false, LONG);
    }

    public String getDisplayName(boolean daylight, int style)
    {
        return id;
    }

    public Object clone()
    {
        return new TimeZone(id, rawOffset);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof TimeZone))
        {
            return false;
        }
        TimeZone o = (TimeZone)other;
        return id.equals(o.id) && rawOffset == o.rawOffset;
    }

    public int hashCode()
    {
        return id.hashCode() ^ rawOffset;
    }

    public String toString()
    {
        return id;
    }
}
