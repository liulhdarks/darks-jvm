package java.util;

/**
 * Gregorian calendar using the reduced java.time implementation.
 */
public class GregorianCalendar extends Calendar
{
    public static final int BC = 0;
    public static final int AD = 1;

    public GregorianCalendar()
    {
        super();
    }

    public GregorianCalendar(TimeZone zone)
    {
        super(zone);
    }

    public GregorianCalendar(Locale locale)
    {
        super();
    }

    public GregorianCalendar(TimeZone zone, Locale locale)
    {
        super(zone);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth)
    {
        super();
        set(year, month, dayOfMonth);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay,
                             int minute)
    {
        super();
        set(year, month, dayOfMonth, hourOfDay, minute);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth, int hourOfDay,
                             int minute, int second)
    {
        super();
        set(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    public static GregorianCalendar from(java.time.ZonedDateTime value)
    {
        GregorianCalendar calendar =
            new GregorianCalendar(TimeZone.getTimeZone(value.getZone().getId()));
        calendar.setTimeInMillis(value.toInstant().toEpochMilli());
        return calendar;
    }

    public java.time.ZonedDateTime toZonedDateTime()
    {
        return java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(getTimeInMillis()),
            getTimeZone().toZoneId());
    }

    public void setGregorianChange(Date date)
    {
    }

    public Date getGregorianChange()
    {
        return new Date(Long.MIN_VALUE);
    }

    public boolean isLeapYear(int year)
    {
        return java.time.Civil.isLeap(year);
    }

    public String getCalendarType()
    {
        return "gregory";
    }

    public Object clone()
    {
        GregorianCalendar copy = new GregorianCalendar(getTimeZone());
        copy.setTimeInMillis(getTimeInMillis());
        copy.setLenient(isLenient());
        return copy;
    }
}
