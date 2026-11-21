package java.util;

/**
 * Calendar with millisecond precision.  Instances use UTC for field
 * arithmetic; time zone support is limited to the default offset.
 */
public abstract class Calendar implements java.io.Serializable, Cloneable, Comparable<Calendar>
{
    public static final int ERA = 0;
    public static final int YEAR = 1;
    public static final int MONTH = 2;
    public static final int WEEK_OF_YEAR = 3;
    public static final int WEEK_OF_MONTH = 4;
    public static final int DATE = 5;
    public static final int DAY_OF_MONTH = 5;
    public static final int DAY_OF_YEAR = 6;
    public static final int DAY_OF_WEEK = 7;
    public static final int DAY_OF_WEEK_IN_MONTH = 8;
    public static final int AM_PM = 9;
    public static final int HOUR = 10;
    public static final int HOUR_OF_DAY = 11;
    public static final int MINUTE = 12;
    public static final int SECOND = 13;
    public static final int MILLISECOND = 14;
    public static final int ZONE_OFFSET = 15;
    public static final int DST_OFFSET = 16;
    public static final int FIELD_COUNT = 17;

    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    public static final int JANUARY = 0;
    public static final int FEBRUARY = 1;
    public static final int MARCH = 2;
    public static final int APRIL = 3;
    public static final int MAY = 4;
    public static final int JUNE = 5;
    public static final int JULY = 6;
    public static final int AUGUST = 7;
    public static final int SEPTEMBER = 8;
    public static final int OCTOBER = 9;
    public static final int NOVEMBER = 10;
    public static final int DECEMBER = 11;
    public static final int UNDECIMBER = 12;

    public static final int AM = 0;
    public static final int PM = 1;

    private long time;
    private final int[] fields = new int[FIELD_COUNT];
    private final boolean[] isSet = new boolean[FIELD_COUNT];
    private boolean lenient = true;
    private int firstDayOfWeek = SUNDAY;
    private int minimalDaysInFirstWeek = 1;

    protected Calendar()
    {
        this.time = System.currentTimeMillis();
    }

    protected Calendar(int year, int month, int dayOfMonth)
    {
        set(year, month, dayOfMonth);
    }

    protected Calendar(int year, int month, int dayOfMonth, int hourOfDay, int minute)
    {
        set(year, month, dayOfMonth, hourOfDay, minute);
    }

    protected Calendar(int year, int month, int dayOfMonth, int hourOfDay,
                       int minute, int second)
    {
        set(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    public static Calendar getInstance()
    {
        return new GregorianCalendar();
    }

    public static Calendar getInstance(Locale locale)
    {
        return new GregorianCalendar();
    }

    public static Calendar getInstance(java.util.TimeZone zone)
    {
        return new GregorianCalendar();
    }

    public long getTimeInMillis()
    {
        applyFields();
        return time;
    }

    public void setTimeInMillis(long millis)
    {
        this.time = millis;
        clearFields();
    }

    public Date getTime()
    {
        return new Date(getTimeInMillis());
    }

    public void setTime(Date date)
    {
        setTimeInMillis(date == null ? System.currentTimeMillis() : date.getTime());
    }

    public int get(int field)
    {
        String name = fieldName(field);
        if (isSet[field])
        {
            return fields[field];
        }
        LocalParts parts = localParts(getTimeInMillis());
        switch (field)
        {
        case ERA:
            return 1;
        case YEAR:
            return parts.year;
        case MONTH:
            return parts.month - 1;
        case WEEK_OF_YEAR:
            return (parts.dayOfYear - 1) / 7 + 1;
        case WEEK_OF_MONTH:
            return (parts.dayOfMonth - 1) / 7 + 1;
        case DATE:
            return parts.dayOfMonth;
        case DAY_OF_YEAR:
            return parts.dayOfYear;
        case DAY_OF_WEEK:
            return (parts.dayOfWeek - 1) % 7 + 1;
        case DAY_OF_WEEK_IN_MONTH:
            return (parts.dayOfMonth - 1) / 7 + 1;
        case AM_PM:
            return parts.hour < 12 ? AM : PM;
        case HOUR:
            return parts.hour % 12;
        case HOUR_OF_DAY:
            return parts.hour;
        case MINUTE:
            return parts.minute;
        case SECOND:
            return parts.second;
        case MILLISECOND:
            return parts.milli;
        case ZONE_OFFSET:
        case DST_OFFSET:
            return 0;
        default:
            throw new IllegalArgumentException("Unknown field: " + name);
        }
    }

    public void set(int field, int value)
    {
        if (field < 0 || field >= FIELD_COUNT)
        {
            throw new IllegalArgumentException("Unknown field: " + field);
        }
        fields[field] = value;
        isSet[field] = true;
        if (field == DAY_OF_MONTH)
        {
            fields[DATE] = value;
            isSet[DATE] = true;
        }
    }

    public void set(int year, int month, int dayOfMonth)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DATE, dayOfMonth);
    }

    public void set(int year, int month, int dayOfMonth, int hourOfDay, int minute)
    {
        set(year, month, dayOfMonth);
        set(HOUR_OF_DAY, hourOfDay);
        set(MINUTE, minute);
    }

    public void set(int year, int month, int dayOfMonth,
                    int hourOfDay, int minute, int second)
    {
        set(year, month, dayOfMonth, hourOfDay, minute);
        set(SECOND, second);
    }

    public final void clear()
    {
        clearFields();
    }

    public final void clear(int field)
    {
        if (field >= 0 && field < FIELD_COUNT)
        {
            isSet[field] = false;
            fields[field] = 0;
        }
    }

    public final boolean isSet(int field)
    {
        return field >= 0 && field < FIELD_COUNT && isSet[field];
    }

    private void clearFields()
    {
        for (int i = 0; i < FIELD_COUNT; i++)
        {
            isSet[i] = false;
            fields[i] = 0;
        }
    }

    private void applyFields()
    {
        if (!isSet[YEAR] && !isSet[MONTH] && !isSet[DATE] && !isSet[HOUR_OF_DAY]
                && !isSet[MINUTE] && !isSet[SECOND])
        {
            return;
        }
        LocalParts current = localParts(time);
        int year = isSet[YEAR] ? fields[YEAR] : current.year;
        int month = isSet[MONTH] ? fields[MONTH] + 1 : current.month;
        int day = isSet[DATE] ? fields[DATE] : current.dayOfMonth;
        int hour = isSet[HOUR_OF_DAY] ? fields[HOUR_OF_DAY] : current.hour;
        if (isSet[HOUR] && !isSet[HOUR_OF_DAY])
        {
            hour = fields[HOUR] + (isSet[AM_PM] && fields[AM_PM] == PM ? 12 : 0);
        }
        int minute = isSet[MINUTE] ? fields[MINUTE] : current.minute;
        int second = isSet[SECOND] ? fields[SECOND] : current.second;
        int milli = isSet[MILLISECOND] ? fields[MILLISECOND] : current.milli;
        long epochDay = java.time.Civil.daysFromCivil(year, month, day);
        long localSeconds = epochDay * 86400L + hour * 3600L + minute * 60L + second;
        time = localSeconds * 1000L + milli;
        clearFields();
    }

    public void add(int field, int amount)
    {
        applyFields();
        if (field == YEAR)
        {
            set(YEAR, localParts(time).year + amount);
            applyFields();
        }
        else if (field == MONTH)
        {
            LocalParts p = localParts(time);
            int total = p.year * 12 + (p.month - 1) + amount;
            int year = Math.floorDiv(total, 12);
            int month = Math.floorMod(total, 12) + 1;
            int day = Math.min(p.dayOfMonth, java.time.Civil.lengthOfMonth(year, month));
            set(year, month - 1, day, p.hour, p.minute, p.second);
            applyFields();
        }
        else if (field == DATE || field == DAY_OF_MONTH)
        {
            setTimeInMillis(time + amount * 86400000L);
        }
        else if (field == HOUR || field == HOUR_OF_DAY)
        {
            setTimeInMillis(time + amount * 3600000L);
        }
        else if (field == MINUTE)
        {
            setTimeInMillis(time + amount * 60000L);
        }
        else if (field == SECOND)
        {
            setTimeInMillis(time + amount * 1000L);
        }
        else if (field == MILLISECOND)
        {
            setTimeInMillis(time + amount);
        }
        else
        {
            throw new IllegalArgumentException("Unsupported field: " + field);
        }
    }

    public void roll(int field, int amount)
    {
        add(field, amount);
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public void setLenient(boolean lenient)
    {
        this.lenient = lenient;
    }

    public int getFirstDayOfWeek()
    {
        return firstDayOfWeek;
    }

    public void setFirstDayOfWeek(int value)
    {
        this.firstDayOfWeek = value;
    }

    public int getMinimalDaysInFirstWeek()
    {
        return minimalDaysInFirstWeek;
    }

    public void setMinimalDaysInFirstWeek(int value)
    {
        this.minimalDaysInFirstWeek = value;
    }

    public int compareTo(Calendar other)
    {
        long a = getTimeInMillis();
        long b = other.getTimeInMillis();
        return a < b ? -1 : (a == b ? 0 : 1);
    }

    public boolean equals(Object other)
    {
        return other instanceof Calendar
            && ((Calendar)other).getTimeInMillis() == getTimeInMillis();
    }

    public int hashCode()
    {
        long millis = getTimeInMillis();
        return (int)(millis ^ (millis >>> 32));
    }

    public Object clone()
    {
        Calendar copy = new GregorianCalendar();
        copy.setTimeInMillis(getTimeInMillis());
        return copy;
    }

    public String getDisplayName(int field, int style, Locale locale)
    {
        return Integer.toString(get(field));
    }

    private static String fieldName(int field)
    {
        switch (field)
        {
        case ERA: return "ERA";
        case YEAR: return "YEAR";
        case MONTH: return "MONTH";
        case DATE: return "DATE";
        case DAY_OF_YEAR: return "DAY_OF_YEAR";
        case DAY_OF_WEEK: return "DAY_OF_WEEK";
        case HOUR: return "HOUR";
        case HOUR_OF_DAY: return "HOUR_OF_DAY";
        case MINUTE: return "MINUTE";
        case SECOND: return "SECOND";
        case MILLISECOND: return "MILLISECOND";
        default: return Integer.toString(field);
        }
    }

    static final class LocalParts
    {
        int year;
        int month;
        int dayOfMonth;
        int dayOfYear;
        int dayOfWeek;
        int hour;
        int minute;
        int second;
        int milli;
    }

    static LocalParts localParts(long millis)
    {
        long epochSecond = Math.floorDiv(millis, 1000L);
        int milli = (int)Math.floorMod(millis, 1000L);
        int[] ymd = java.time.Civil.civilFromDays(Math.floorDiv(epochSecond, 86400L));
        int secondOfDay = (int)Math.floorMod(epochSecond, 86400L);
        LocalParts parts = new LocalParts();
        parts.year = ymd[0];
        parts.month = ymd[1];
        parts.dayOfMonth = ymd[2];
        parts.dayOfYear = (int)(java.time.Civil.daysFromCivil(ymd[0], ymd[1], 1)
            - java.time.Civil.daysFromCivil(ymd[0], 1, 1)) + ymd[2] - 1 + 1;
        parts.dayOfWeek = java.time.Civil.dayOfWeek(Math.floorDiv(epochSecond, 86400L));
        parts.hour = secondOfDay / 3600;
        parts.minute = (secondOfDay % 3600) / 60;
        parts.second = secondOfDay % 60;
        parts.milli = milli;
        return parts;
    }
}
