package java.util;

/** Gregorian calendar implementation. */
public class GregorianCalendar extends Calendar
{
    public static final int BC = 0;
    public static final int AD = 1;

    private long gregorianChange = -12219292800000L;

    public GregorianCalendar()
    {
        super();
    }

    public GregorianCalendar(TimeZone zone)
    {
        super();
    }

    public GregorianCalendar(Locale locale)
    {
        super();
    }

    public GregorianCalendar(TimeZone zone, Locale locale)
    {
        super();
    }

    public GregorianCalendar(int year, int month, int dayOfMonth)
    {
        super(year, month, dayOfMonth);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth,
                             int hourOfDay, int minute)
    {
        super(year, month, dayOfMonth, hourOfDay, minute);
    }

    public GregorianCalendar(int year, int month, int dayOfMonth,
                             int hourOfDay, int minute, int second)
    {
        super(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    public void setGregorianChange(Date date)
    {
        gregorianChange = date.getTime();
    }

    public Date getGregorianChange()
    {
        return new Date(gregorianChange);
    }

    public boolean isLeapYear(int year)
    {
        return java.time.Civil.isLeap(year);
    }

    public int getActualMaximum(int field)
    {
        if (field == DATE)
        {
            return java.time.Civil.lengthOfMonth(get(YEAR), get(MONTH) + 1);
        }
        if (field == DAY_OF_YEAR)
        {
            return isLeapYear(get(YEAR)) ? 366 : 365;
        }
        if (field == MONTH)
        {
            return DECEMBER;
        }
        if (field == DAY_OF_WEEK)
        {
            return SATURDAY;
        }
        return get(field);
    }

    public int getActualMinimum(int field)
    {
        return get(field);
    }

    public String toString()
    {
        return "java.util.GregorianCalendar[time=" + getTimeInMillis() + "]";
    }
}
