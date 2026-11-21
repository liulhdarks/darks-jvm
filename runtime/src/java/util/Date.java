package java.util;

/**
 * Minimal date value backed by milliseconds since the epoch.  The pattern
 * based formatting lives in java.text.SimpleDateFormat.
 */
public class Date implements Comparable<Date>
{
    private long millis;

    public Date()
    {
        millis = System.currentTimeMillis();
    }

    public Date(long millis)
    {
        this.millis = millis;
    }

    public long getTime()
    {
        return millis;
    }

    public void setTime(long millis)
    {
        this.millis = millis;
    }

    public boolean before(Date other)
    {
        return millis < other.millis;
    }

    public boolean after(Date other)
    {
        return millis > other.millis;
    }

    public int compareTo(Date other)
    {
        if (millis < other.millis)
        {
            return -1;
        }
        return millis == other.millis ? 0 : 1;
    }

    public boolean equals(Object other)
    {
        return (other instanceof Date) && ((Date)other).millis == millis;
    }

    public int hashCode()
    {
        return (int)(millis ^ (millis >>> 32));
    }

    // java.time bridge (Date.from(Instant) / toInstant()).
    public static Date from(java.time.Instant instant)
    {
        return new Date(instant.toEpochMilli());
    }

    public java.time.Instant toInstant()
    {
        return java.time.Instant.ofEpochMilli(millis);
    }

    public String toString()
    {
        return java.text.SimpleDateFormat.isoDateTimeFormat().format(this);
    }
}
