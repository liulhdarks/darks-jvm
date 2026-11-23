package java.sql;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class Timestamp extends java.util.Date
{
    private int nanos;

    public Timestamp(long time)
    {
        super(time - Math.floorMod(time, 1000L));
        nanos = (int)(Math.floorMod(time, 1000L) * 1000000L);
    }

    public Timestamp(int year, int month, int day, int hour, int minute, int second, int nano)
    {
        super(LocalDateTime.of(year, month, day, hour, minute, second, 0)
            .toEpochSecond(ZoneOffset.UTC) * 1000L);
        this.nanos = nano;
    }

    public void setTime(long time)
    {
        super.setTime(time - Math.floorMod(time, 1000L));
        nanos = (int)(Math.floorMod(time, 1000L) * 1000000L);
    }

    public long getTime()
    {
        return super.getTime() + nanos / 1000000;
    }

    public int getNanos()
    {
        return nanos;
    }

    public void setNanos(int nanos)
    {
        if (nanos < 0 || nanos > 999999999)
        {
            throw new IllegalArgumentException("nanos out of range");
        }
        this.nanos = nanos;
    }

    public int getYear()
    {
        return toLocalDateTime().getYear() - 1900;
    }

    public static Timestamp valueOf(String text)
    {
        return valueOf(parse(text));
    }

    public static Timestamp valueOf(LocalDateTime time)
    {
        long seconds = time.toEpochSecond(ZoneOffset.UTC);
        Timestamp timestamp = new Timestamp(seconds * 1000L);
        timestamp.setNanos(time.getNano());
        return timestamp;
    }

    public static Timestamp from(Instant instant)
    {
        Timestamp timestamp = new Timestamp(instant.toEpochMilli());
        timestamp.setNanos(instant.getNano());
        return timestamp;
    }

    public Instant toInstant()
    {
        return Instant.ofEpochSecond(Math.floorDiv(super.getTime(), 1000L), nanos);
    }

    public LocalDateTime toLocalDateTime()
    {
        long seconds = Math.floorDiv(super.getTime(), 1000L);
        return LocalDateTime.ofEpochSecond(seconds, nanos, ZoneOffset.UTC);
    }

    public String toString()
    {
        LocalDateTime time = toLocalDateTime();
        String text = time.toLocalDate().toString() + " " +
            pad(time.getHour()) + ":" + pad(time.getMinute()) + ":" + pad(time.getSecond());
        if (nanos > 0)
        {
            String fraction = "000000000" + nanos;
            fraction = fraction.substring(fraction.length() - 9);
            while (fraction.endsWith("0"))
            {
                fraction = fraction.substring(0, fraction.length() - 1);
            }
            text = text + "." + fraction;
        }
        return text;
    }

    private static String pad(int value)
    {
        return value < 10 ? "0" + value : String.valueOf(value);
    }

    private static LocalDateTime parse(String text)
    {
        String value = text.trim();
        int space = value.indexOf(' ');
        String datePart = value.substring(0, space);
        String timePart = value.substring(space + 1);
        int hour = Integer.parseInt(timePart.substring(0, 2));
        int minute = Integer.parseInt(timePart.substring(3, 5));
        int second = 0;
        int nano = 0;
        if (timePart.length() >= 8)
        {
            second = Integer.parseInt(timePart.substring(6, 8));
        }
        int dot = timePart.indexOf('.');
        if (dot >= 0)
        {
            String fraction = timePart.substring(dot + 1);
            while (fraction.length() < 9)
            {
                fraction = fraction + "0";
            }
            nano = Integer.parseInt(fraction.substring(0, 9));
        }
        return LocalDateTime.of(java.time.LocalDate.parse(datePart), java.time.LocalTime.of(hour, minute, second, nano));
    }
}
