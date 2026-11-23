package java.sql;

import java.time.Instant;
import java.time.LocalTime;

public class Time extends java.util.Date
{
    public Time(long time)
    {
        super(time);
    }

    public Time(int hour, int minute, int second)
    {
        super((hour * 3600L + minute * 60L + second) * 1000L);
    }

    public static Time valueOf(String text)
    {
        return new Time(toMillis(LocalTime.parse(text)));
    }

    public static Time valueOf(LocalTime time)
    {
        return new Time(toMillis(time));
    }

    public static Time from(Instant instant)
    {
        return new Time(instant.toEpochMilli());
    }

    private static long toMillis(LocalTime time)
    {
        return time.toNanoOfDay() / 1000000L;
    }

    public LocalTime toLocalTime()
    {
        long value = getTime();
        long dayMillis = 86400000L;
        long millis = value % dayMillis;
        if (millis < 0)
        {
            millis += dayMillis;
        }
        return LocalTime.ofNanoOfDay(millis * 1000000L);
    }

    public String toString()
    {
        return toLocalTime().toString();
    }
}
