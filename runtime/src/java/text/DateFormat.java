package java.text;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Base class for date/time formats.  The pattern implementation lives in
 * SimpleDateFormat.
 *
 * Unlike the JDK, this class is not abstract and does not extend
 * java.text.Format: the AttributedCharacterIterator machinery that Format is
 * built on is not available in this runtime.  The JDK public API surface
 * (field constants, field-position and ParsePosition overloads, locale
 * variants) is provided here so that code compiled against the JDK links;
 * where a locale or field-position based behaviour would need JDK locale data
 * the implementation is a documented simplification.
 */
public class DateFormat
{
    public static final int FULL = 0;
    public static final int LONG = 1;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    public static final int DEFAULT = MEDIUM;

    // java.text.DateFormat field constants (JDK 1.1 API).
    public static final int ERA_FIELD = 0;
    public static final int YEAR_FIELD = 1;
    public static final int MONTH_FIELD = 2;
    public static final int DATE_FIELD = 3;
    public static final int HOUR_OF_DAY1_FIELD = 4;
    public static final int HOUR_OF_DAY0_FIELD = 5;
    public static final int MINUTE_FIELD = 6;
    public static final int SECOND_FIELD = 7;
    public static final int MILLISECOND_FIELD = 8;
    public static final int DAY_OF_WEEK_FIELD = 9;
    public static final int DAY_OF_YEAR_FIELD = 10;
    public static final int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
    public static final int WEEK_OF_YEAR_FIELD = 12;
    public static final int WEEK_OF_MONTH_FIELD = 13;
    public static final int AM_PM_FIELD = 14;
    public static final int HOUR1_FIELD = 15;
    public static final int HOUR0_FIELD = 16;
    public static final int TIMEZONE_FIELD = 17;

    private TimeZone timeZone = TimeZone.getTimeZone("UTC");
    protected Calendar calendar;
    protected NumberFormat numberFormat;

    public DateFormat()
    {
        numberFormat = NumberFormat.getNumberInstance();
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
        this.calendar = calendar;
    }

    public Calendar getCalendar()
    {
        return calendar;
    }

    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    public void setNumberFormat(NumberFormat newNumberFormat)
    {
        if (newNumberFormat == null)
        {
            throw new NullPointerException();
        }
        numberFormat = newNumberFormat;
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

    public static DateFormat getDateInstance(int style, Locale aLocale)
    {
        // Locale-specific patterns need JDK locale data; the default patterns
        // are used instead.
        return getDateInstance(style);
    }

    public static DateFormat getTimeInstance()
    {
        return getTimeInstance(DEFAULT);
    }

    public static DateFormat getTimeInstance(int style)
    {
        return new SimpleDateFormat(style == SHORT ? "HH:mm" : "HH:mm:ss");
    }

    public static DateFormat getTimeInstance(int style, Locale aLocale)
    {
        return getTimeInstance(style);
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

    public static DateFormat getDateTimeInstance(int dateStyle, int timeStyle,
        Locale aLocale)
    {
        return getDateTimeInstance(dateStyle, timeStyle);
    }

    public static DateFormat getInstance(Locale locale)
    {
        return getInstance();
    }

    public static Locale[] getAvailableLocales()
    {
        // Only the default locale's patterns are implemented.
        return new Locale[] { Locale.getDefault() };
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

    /**
     * Appends the formatted date.  Field position information is not tracked
     * by this implementation; {@code fieldPosition} is left untouched.
     */
    public StringBuffer format(Date date, StringBuffer toAppendTo,
        FieldPosition fieldPosition)
    {
        return toAppendTo.append(format(date));
    }

    public final StringBuffer format(Object obj, StringBuffer toAppendTo,
        FieldPosition fieldPosition)
    {
        if (obj instanceof Date)
        {
            return format((Date)obj, toAppendTo, fieldPosition);
        }
        throw new IllegalArgumentException(
            "Cannot format given Object as a Date");
    }

    public String format(Object value)
    {
        if (value instanceof Date)
        {
            return format((Date)value);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Date");
    }

    /**
     * Parses a date starting at {@code pos.getIndex()}.  On success the index
     * is advanced to the end of the parsed text.  On failure {@code null} is
     * returned and the error index is set, as in the JDK; the throwing
     * {@link #parse(String)} variant is kept because the concrete
     * implementation here has no recourse to JDK parser internals.
     */
    public Date parse(String source, ParsePosition pos)
    {
        if (source == null || pos == null)
        {
            throw new NullPointerException();
        }
        pos.setErrorIndex(pos.getIndex());
        return null;
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
