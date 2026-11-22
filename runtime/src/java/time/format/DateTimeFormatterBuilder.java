package java.time.format;

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Locale;

/**
 * Builds a {@link DateTimeFormatter} from literal and numeric components.
 * The runtime formatter is pattern based, so the builder accumulates an
 * equivalent pattern string and hands it to {@link DateTimeFormatter#ofPattern}.
 */
public final class DateTimeFormatterBuilder
{
    private final StringBuilder pattern = new StringBuilder();

    public DateTimeFormatterBuilder()
    {
    }

    public DateTimeFormatterBuilder appendPattern(String pattern)
    {
        if (pattern == null)
        {
            throw new NullPointerException("pattern");
        }
        this.pattern.append(pattern);
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(char literal)
    {
        appendQuoted(String.valueOf(literal));
        return this;
    }

    public DateTimeFormatterBuilder appendLiteral(String literal)
    {
        if (literal == null)
        {
            throw new NullPointerException("literal");
        }
        appendQuoted(literal);
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int width)
    {
        if (width < 1)
        {
            throw new IllegalArgumentException("width must be positive");
        }
        appendLetters(letterFor(field), width);
        return this;
    }

    public DateTimeFormatterBuilder appendValue(TemporalField field, int minWidth,
                                               int maxWidth, SignStyle signStyle)
    {
        if (minWidth < 1 || maxWidth < minWidth)
        {
            throw new IllegalArgumentException("Invalid width");
        }
        int width = minWidth;
        if (field == ChronoField.YEAR && width < 4)
        {
            width = 4;
        }
        appendLetters(letterFor(field), width);
        return this;
    }

    public DateTimeFormatterBuilder parseCaseInsensitive()
    {
        return this;
    }

    public DateTimeFormatterBuilder parseCaseSensitive()
    {
        return this;
    }

    public DateTimeFormatter toFormatter()
    {
        return DateTimeFormatter.ofPattern(pattern.toString());
    }

    public DateTimeFormatter toFormatter(Locale locale)
    {
        return DateTimeFormatter.ofPattern(pattern.toString(),
            locale == null ? Locale.getDefault() : locale);
    }

    private void appendQuoted(String literal)
    {
        boolean needsQuotes = literal.length() == 0 || literal.indexOf('\'') >= 0;
        for (int i = 0; i < literal.length() && !needsQuotes; i++)
        {
            char c = literal.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                needsQuotes = true;
            }
        }
        if (!needsQuotes)
        {
            pattern.append(literal);
            return;
        }
        pattern.append('\'');
        for (int i = 0; i < literal.length(); i++)
        {
            char c = literal.charAt(i);
            if (c == '\'')
            {
                pattern.append("''");
            }
            else
            {
                pattern.append(c);
            }
        }
        pattern.append('\'');
    }

    private void appendLetters(char letter, int count)
    {
        for (int i = 0; i < count; i++)
        {
            pattern.append(letter);
        }
    }

    private static char letterFor(TemporalField field)
    {
        if (field == ChronoField.YEAR)
        {
            return 'u';
        }
        if (field == ChronoField.YEAR_OF_ERA)
        {
            return 'y';
        }
        if (field == ChronoField.MONTH_OF_YEAR)
        {
            return 'M';
        }
        if (field == ChronoField.DAY_OF_MONTH)
        {
            return 'd';
        }
        if (field == ChronoField.DAY_OF_YEAR)
        {
            return 'D';
        }
        if (field == ChronoField.HOUR_OF_DAY)
        {
            return 'H';
        }
        if (field == ChronoField.MINUTE_OF_HOUR)
        {
            return 'm';
        }
        if (field == ChronoField.SECOND_OF_MINUTE)
        {
            return 's';
        }
        if (field == ChronoField.NANO_OF_SECOND)
        {
            return 'n';
        }
        if (field == ChronoField.MILLI_OF_SECOND)
        {
            return 'S';
        }
        if (field == ChronoField.OFFSET_SECONDS)
        {
            return 'X';
        }
        if (field == ChronoField.AMPM_OF_DAY)
        {
            return 'a';
        }
        if (field == ChronoField.DAY_OF_WEEK)
        {
            return 'E';
        }
        if (field == ChronoField.ERA)
        {
            return 'G';
        }
        throw new IllegalArgumentException("Unsupported field: " + field);
    }
}
