package java.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Minimal Gregorian calendar view over an epoch-millisecond instant.
 */
public abstract class Calendar implements java.io.Serializable
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

    public static final int AM = 0;
    public static final int PM = 1;

    protected long millis;
    protected boolean lenient = true;
    protected TimeZone zone;

    protected Calendar()
    {
        this(TimeZone.getDefault());
    }

    protected Calendar(TimeZone zone)
    {
        this.zone = zone == null ? TimeZone.getDefault() : zone;
        this.millis = System.currentTimeMillis();
    }

    public static Calendar getInstance()
    {
        return new GregorianCalendar();
    }

    public static Calendar getInstance(TimeZone zone)
    {
        return new GregorianCalendar(zone);
    }

    public static Calendar getInstance(Locale locale)
    {
        return new GregorianCalendar();
    }

    private ZonedDateTime zoned()
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), zone.toZoneId());
    }

    private void update(LocalDateTime value)
    {
        ZoneId id = zone.toZoneId();
        millis = ZonedDateTime.of(value, id).toInstant().toEpochMilli();
    }

    public long getTimeInMillis()
    {
        return millis;
    }

    public void setTimeInMillis(long millis)
    {
        this.millis = millis;
    }

    public Date getTime()
    {
        return new Date(millis);
    }

    public void setTime(Date date)
    {
        if (date == null)
        {
            throw new NullPointerException("date");
        }
        this.millis = date.getTime();
    }

    public TimeZone getTimeZone()
    {
        return zone;
    }

    public void setTimeZone(TimeZone zone)
    {
        if (zone == null)
        {
            throw new NullPointerException("zone");
        }
        this.zone = zone;
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
        return SUNDAY;
    }

    public void setFirstDayOfWeek(int value)
    {
    }

    public int getMinimalDaysInFirstWeek()
    {
        return 1;
    }

    public void setMinimalDaysInFirstWeek(int value)
    {
    }

    public int get(int field)
    {
        ZonedDateTime value = zoned();
        LocalDateTime local = value.toLocalDateTime();
        switch (field)
        {
        case ERA:
            return 1;
        case YEAR:
            return local.getYear();
        case MONTH:
            return local.getMonthValue() - 1;
        case DAY_OF_MONTH:
            return local.getDayOfMonth();
        case DAY_OF_YEAR:
            return local.toLocalDate().getDayOfYear();
        case DAY_OF_WEEK:
        {
            int monday = java.time.Civil.dayOfWeek(local.toLocalDate().toEpochDay());
            return monday % 7 + 1;
        }
        case AM_PM:
            return local.getHour() < 12 ? AM : PM;
        case HOUR:
            return local.getHour() % 12;
        case HOUR_OF_DAY:
            return local.getHour();
        case MINUTE:
            return local.getMinute();
        case SECOND:
            return local.getSecond();
        case MILLISECOND:
            return local.getNano() / 1000000;
        case ZONE_OFFSET:
            return zone.getRawOffset();
        case DST_OFFSET:
            return 0;
        default:
            throw new IllegalArgumentException("Unsupported calendar field: " + field);
        }
    }

    public void set(int field, int value)
    {
        LocalDateTime local = zoned().toLocalDateTime();
        switch (field)
        {
        case YEAR:
            local = LocalDateTime.of(local.toLocalDate().withYear(value),
                local.toLocalTime());
            break;
        case MONTH:
            local = LocalDateTime.of(local.toLocalDate().withMonth(value + 1),
                local.toLocalTime());
            break;
        case DAY_OF_MONTH:
            local = LocalDateTime.of(local.toLocalDate().withDayOfMonth(value),
                local.toLocalTime());
            break;
        case HOUR_OF_DAY:
            local = LocalDateTime.of(local.toLocalDate(),
                local.toLocalTime().withHour(value));
            break;
        case HOUR:
        {
            int hour = value % 12 + (local.getHour() >= 12 ? 12 : 0);
            local = LocalDateTime.of(local.toLocalDate(),
                local.toLocalTime().withHour(hour));
            break;
        }
        case MINUTE:
            local = LocalDateTime.of(local.toLocalDate(),
                local.toLocalTime().withMinute(value));
            break;
        case SECOND:
            local = LocalDateTime.of(local.toLocalDate(),
                local.toLocalTime().withSecond(value));
            break;
        case MILLISECOND:
            local = LocalDateTime.of(local.toLocalDate(),
                local.toLocalTime().withNano(value * 1000000));
            break;
        default:
            throw new IllegalArgumentException("Unsupported calendar field: " + field);
        }
        update(local);
    }

    public final void set(int year, int month, int day)
    {
        set(YEAR, year);
        set(MONTH, month);
        set(DAY_OF_MONTH, day);
    }

    public final void set(int year, int month, int day, int hour, int minute)
    {
        set(year, month, day);
        set(HOUR_OF_DAY, hour);
        set(MINUTE, minute);
    }

    public final void set(int year, int month, int day, int hour, int minute,
                          int second)
    {
        set(year, month, day, hour, minute);
        set(SECOND, second);
    }

    public void add(int field, int amount)
    {
        if (amount == 0)
        {
            return;
        }
        LocalDateTime local = zoned().toLocalDateTime();
        switch (field)
        {
        case YEAR:
            update(LocalDateTime.of(local.toLocalDate().plusYears(amount),
                local.toLocalTime()));
            return;
        case MONTH:
            update(LocalDateTime.of(local.toLocalDate().plusMonths(amount),
                local.toLocalTime()));
            return;
        case DAY_OF_MONTH:
        case DAY_OF_YEAR:
            update(local.plusDays(amount));
            return;
        case HOUR_OF_DAY:
        case HOUR:
            update(local.plusHours(amount));
            return;
        case MINUTE:
            update(local.plusMinutes(amount));
            return;
        case SECOND:
            update(local.plusSeconds(amount));
            return;
        case MILLISECOND:
            millis = millis + amount;
            return;
        default:
            throw new IllegalArgumentException("Unsupported calendar field: " + field);
        }
    }

    public boolean before(Object when)
    {
        return when instanceof Calendar &&
            millis < ((Calendar)when).getTimeInMillis();
    }

    public boolean after(Object when)
    {
        return when instanceof Calendar &&
            millis > ((Calendar)when).getTimeInMillis();
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Calendar))
        {
            return false;
        }
        Calendar o = (Calendar)other;
        return millis == o.millis && zone.equals(o.zone);
    }

    public int hashCode()
    {
        return (int)(millis ^ (millis >>> 32)) ^ zone.hashCode();
    }

    public String toString()
    {
        return getClass().getName() + "[time=" + millis + ",zone=" + zone.getID() + "]";
    }
}
