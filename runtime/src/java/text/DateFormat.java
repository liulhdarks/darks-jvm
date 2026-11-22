package java.text;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Base class for date/time formats.  The pattern implementation lives in
 *  SimpleDateFormat. */
public class DateFormat
{
    public static final int FULL = 0;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    public static final int DEFAULT = MEDIUM;

    private TimeZone timeZone = TimeZone.getTimeZone("UTC");

    public DateFormat()
    {
    }

    public void setTimeZone(TimeZone zone)
    {
        if (zone != null)
        {
            timeZone = zone;
        }
    }

    public TimeZone getTimeZone()
    {
        return timeZone;
    }

    public void setCalendar(Calendar calendar)
    {
    }

    public Calendar getCalendar()
    {
        return null;
    }

    public static DateFormat getInstance()
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static DateFormat getDateInstance()
    {
        return getDateInstance(DEFAULT);
    }

    public static DateFormat getDateInstance(int style)
    {
        return new SimpleDateFormat(stylePattern(style));
    }

    public static DateFormat getTimeInstance()
    {
        return getTimeInstance(DEFAULT);
    }

    public static DateFormat getTimeInstance(int style)
    {
        return new SimpleDateFormat(style == SHORT ? "HH:mm" : "HH:mm:ss");
    }

    public static DateFormat getDateTimeInstance()
    {
        return getDateTimeInstance(DEFAULT, DEFAULT);
    }

    public static DateFormat getDateTimeInstance(int dateStyle, int timeStyle)
    {
        return new SimpleDateFormat(stylePattern(dateStyle) + " " +
                                   (timeStyle == SHORT ? "HH:mm" : "HH:mm:ss"));
    }

    public static DateFormat getInstance(Locale locale)
    {
        return getInstance();
    }

    private static String stylePattern(int style)
    {
        if (style == SHORT)
        {
            return "yy-MM-dd";
        }
        if (style == LONG)
        {
            return "yyyy-MM-dd";
        }
        if (style == FULL)
        {
            return "yyyy-MM-dd";
        }
        return "yyyy-MM-dd";
    }

    public String format(Date date)
    {
        return date.toString();
    }

    public String format(Object value)
    {
        if (value instanceof Date)
        {
            return format((Date)value);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    public Date parse(String text) throws ParseException
    {
        throw new ParseException("Parsing is not supported by this DateFormat", 0);
    }

    public void setLenient(boolean lenient)
    {
    }

    public boolean isLenient()
    {
        return true;
    }
}
