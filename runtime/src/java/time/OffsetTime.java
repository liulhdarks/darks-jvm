package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/** A time with a fixed offset from UTC. */
public final class OffsetTime implements Temporal, Comparable<OffsetTime>
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
        return ofInstant(Instant.now(), ZoneId.systemDefault());
    }

    public static OffsetTime of(LocalTime time, ZoneOffset offset)
    {
        if (time == null || offset == null)
        {
            throw new NullPointerException();
        }
        return new OffsetTime(time, offset);
    }

    public static OffsetTime of(int hour, int minute, int second, int nano,
                                ZoneOffset offset)
    {
        return of(LocalTime.of(hour, minute, second, nano), offset);
    }

    public static OffsetTime ofInstant(Instant instant, ZoneId zone)
    {
        ZonedDateTime zoned = ZonedDateTime.ofInstant(instant, zone);
        return of(zoned.toLocalTime(), zoned.getOffset());
    }

    public static OffsetTime parse(CharSequence text, DateTimeFormatter formatter)
    {
        if (formatter == null)
        {
            throw new NullPointerException("formatter");
        }
        java.time.temporal.TemporalAccessor parsed = formatter.parse(text);
        if (parsed instanceof OffsetTime)
        {
            return (OffsetTime)parsed;
        }
        if (parsed instanceof ZonedDateTime)
        {
            ZonedDateTime zoned = (ZonedDateTime)parsed;
            return of(zoned.toLocalTime(), zoned.getOffset());
        }
        if (parsed instanceof LocalTime)
        {
            return of((LocalTime)parsed, ZoneOffset.UTC);
        }
        throw new java.time.format.DateTimeParseException(
            "Text cannot be parsed to an OffsetTime", text.toString(), 0);
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

    public int getNano()
    {
        return time.getNano();
    }

    public int compareTo(OffsetTime other)
    {
        long mine = time.toNanoOfDay() - offset.getTotalSeconds() * 1000000000L;
        long theirs = other.time.toNanoOfDay() - other.offset.getTotalSeconds() * 1000000000L;
        if (mine < theirs)
        {
            return -1;
        }
        return mine == theirs ? 0 : 1;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof OffsetTime))
        {
            return false;
        }
        OffsetTime o = (OffsetTime)other;
        return time.equals(o.time) && offset.equals(o.offset);
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
