package java.sql;

import java.time.Instant;
import java.time.LocalDate;

public class Date extends java.util.Date
{
    public Date(long date)
    {
        super(date);
    }

    public Date(int year, int month, int day)
    {
        super(LocalDate.of(year, month, day).toEpochDay() * 86400000L);
    }

    public static Date valueOf(String text)
    {
        return new Date(LocalDate.parse(text).toEpochDay() * 86400000L);
    }

    public static Date valueOf(LocalDate date)
    {
        return new Date(date.toEpochDay() * 86400000L);
    }

    public static Date from(Instant instant)
    {
        return new Date(instant.toEpochMilli());
    }

    public LocalDate toLocalDate()
    {
        return LocalDate.ofEpochDay(Math.floorDiv(getTime(), 86400000L));
    }

    public String toString()
    {
        return toLocalDate().toString();
    }
}
