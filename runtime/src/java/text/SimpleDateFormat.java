package java.text;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Locale;

/** Pattern based date format: a thin bridge over DateTimeFormatter in UTC. */
public class SimpleDateFormat extends DateFormat
{
    private String pattern;
    private DateTimeFormatter formatter;
    private boolean lenient = true;

    public SimpleDateFormat()
    {
        this("yyyy-MM-dd HH:mm:ss");
    }

    public SimpleDateFormat(String pattern)
    {
        applyPattern(pattern);
    }

    public SimpleDateFormat(String pattern, Locale locale)
    {
        applyPattern(pattern);
    }

    public void applyPattern(String pattern)
    {
        if (pattern == null)
        {
            throw new NullPointerException("pattern");
        }
        this.pattern = pattern;
        this.formatter = DateTimeFormatter.ofPattern(pattern);
    }

    public String toPattern()
    {
        return pattern;
    }

    public void setLenient(boolean value)
    {
        lenient = value;
    }

    public boolean isLenient()
    {
        return lenient;
    }

    public String format(Date date)
    {
        if (date == null)
        {
            throw new NullPointerException("date");
        }
        long millis = date.getTime() + getTimeZone().getOffset(date.getTime());
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(
            Math.floorDiv(millis, 1000L),
            (int)(Math.floorMod(millis, 1000L) * 1000000L),
            ZoneOffset.UTC);
        return formatter.format(ldt);
    }

    public Date parse(String text) throws ParseException
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        try
        {
            TemporalAccessor parsed = formatter.parse(text);
            long millis;
            if (parsed instanceof ZonedDateTime)
            {
                millis = ((ZonedDateTime)parsed).toInstant().toEpochMilli();
            }
            else if (parsed instanceof LocalDateTime)
            {
                millis = ((LocalDateTime)parsed).toEpochSecond(ZoneOffset.UTC) * 1000L;
            }
            else if (parsed instanceof LocalDate)
            {
                millis = ((LocalDate)parsed).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000L;
            }
            else if (parsed instanceof LocalTime)
            {
                millis = ((LocalTime)parsed).toSecondOfDay() * 1000L;
            }
            else
            {
                throw new ParseException("Unsupported parsed value: " + text, 0);
            }
            return new Date(millis);
        }
        catch (java.time.DateTimeException error)
        {
            throw new ParseException(error.getMessage(), 0);
        }
    }

    public Date parse(String text, ParsePosition pos)
    {
        if (text == null || pos == null)
        {
            throw new NullPointerException();
        }
        int start = pos.getIndex();
        try
        {
            Date value = parse(text.substring(start));
            pos.setIndex(text.length());
            return value;
        }
        catch (ParseException error)
        {
            pos.setErrorIndex(start);
            return null;
        }
    }

    public Object clone()
    {
        SimpleDateFormat copy = new SimpleDateFormat(pattern);
        copy.lenient = lenient;
        return copy;
    }

    public static SimpleDateFormat isoDateTimeFormat()
    {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }
}
