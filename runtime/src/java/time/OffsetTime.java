package java.time;

import java.time.temporal.TemporalAccessor;

public final class OffsetTime implements TemporalAccessor, Comparable<OffsetTime>
{
    private final LocalTime time;
    private final ZoneOffset offset;

    private OffsetTime(LocalTime time, ZoneOffset offset)
    {
        this.time = time;
        this.offset = offset;
    }

    public static OffsetTime now()
    {
        return of(LocalTime.now(), ZoneOffset.UTC);
    }

    public static OffsetTime of(LocalTime time, ZoneOffset offset)
    {
        if (time == null || offset == null)
        {
            throw new NullPointerException();
        }
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nano, ZoneOffset offset)
    {
        return of(LocalTime.of(hour, minute, second, nano), offset);
    }

    public static OffsetTime parse(CharSequence text)
    {
        String value = text.toString();
        if (value.endsWith("Z"))
        {
            return of(LocalTime.parse(value.substring(0, value.length() - 1)), ZoneOffset.UTC);
        }
        int sign = value.lastIndexOf('+');
        if (sign < 0)
        {
            sign = value.lastIndexOf('-');
        }
        if (sign > 0)
        {
            return of(LocalTime.parse(value.substring(0, sign)), ZoneOffset.of(value.substring(sign)));
        }
        return of(LocalTime.parse(value), ZoneOffset.UTC);
    }

    public LocalTime toLocalTime()
    {
        return time;
    }

    public ZoneOffset getOffset()
    {
        return offset;
    }

    public int getHour()
    {
        return time.getHour();
    }

    public int getMinute()
    {
        return time.getMinute();
    }

    public int getSecond()
    {
        return time.getSecond();
    }

    public int compareTo(OffsetTime other)
    {
        return time.compareTo(other.time);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof OffsetTime))
        {
            return false;
        }
        OffsetTime value = (OffsetTime)other;
        return time.equals(value.time) && offset.equals(value.offset);
    }

    public int hashCode()
    {
        return time.hashCode() ^ offset.hashCode();
    }

    public String toString()
    {
        return time.toString() + offset.toString();
    }
}
