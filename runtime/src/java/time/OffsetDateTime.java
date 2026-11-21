package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class OffsetDateTime implements TemporalAccessor, Comparable<OffsetDateTime>
{
    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset)
    {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public static OffsetDateTime now()
    {
        return of(LocalDateTime.now(), ZoneOffset.UTC);
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset)
    {
        if (dateTime == null || offset == null)
        {
            throw new NullPointerException();
        }
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(int year, int month, int day, int hour,
                                    int minute, int second, int nano, ZoneOffset offset)
    {
        return of(LocalDateTime.of(year, month, day, hour, minute, second, nano), offset);
    }

    public static OffsetDateTime parse(CharSequence text)
    {
        String value = text.toString();
        if (value.endsWith("Z"))
        {
            return of(LocalDateTime.parse(value.substring(0, value.length() - 1)),
                ZoneOffset.UTC);
        }
        int sign = value.lastIndexOf('+');
        if (sign < 0)
        {
            sign = value.lastIndexOf('-');
        }
        if (sign > 10)
        {
            return of(LocalDateTime.parse(value.substring(0, sign)),
                ZoneOffset.of(value.substring(sign)));
        }
        return of(LocalDateTime.parse(value), ZoneOffset.UTC);
    }

    public LocalDateTime toLocalDateTime()
    {
        return dateTime;
    }

    public ZoneOffset getOffset()
    {
        return offset;
    }

    public int getYear()
    {
        return dateTime.getYear();
    }

    public int getMonthValue()
    {
        return dateTime.getMonthValue();
    }

    public int getDayOfMonth()
    {
        return dateTime.getDayOfMonth();
    }

    public int getHour()
    {
        return dateTime.getHour();
    }

    public int getMinute()
    {
        return dateTime.getMinute();
    }

    public int getSecond()
    {
        return dateTime.getSecond();
    }

    public int compareTo(OffsetDateTime other)
    {
        return dateTime.compareTo(other.dateTime);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof OffsetDateTime))
        {
            return false;
        }
        OffsetDateTime value = (OffsetDateTime)other;
        return dateTime.equals(value.dateTime) && offset.equals(value.offset);
    }

    public int hashCode()
    {
        return dateTime.hashCode() ^ offset.hashCode();
    }

    public String toString()
    {
        return dateTime.toString() + offset.toString();
    }
}
