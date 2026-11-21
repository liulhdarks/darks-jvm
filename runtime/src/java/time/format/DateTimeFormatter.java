package java.time.format;

import java.time.Civil;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

/**
 * Pattern based formatter/parser for the reduced java.time API.
 *
 * Supported pattern letters: G y u M L d D H h K k m s S n a E z Z X x and
 * quoted literals.  Parsing understands the same letters; unknown letters are
 * rejected when the pattern is created.
 */
public final class DateTimeFormatter
{
    public static final DateTimeFormatter ISO_LOCAL_DATE =
        new DateTimeFormatter("yyyy-MM-dd", true, false);
    public static final DateTimeFormatter ISO_LOCAL_TIME =
        new DateTimeFormatter("HH:mm:ss", true, false);
    public static final DateTimeFormatter ISO_LOCAL_DATE_TIME =
        new DateTimeFormatter("yyyy-MM-dd'T'HH:mm:ss", true, false);
    public static final DateTimeFormatter ISO_INSTANT =
        new DateTimeFormatter("", false, true);
    public static final DateTimeFormatter ISO_ZONED_DATE_TIME =
        new DateTimeFormatter("yyyy-MM-dd'T'HH:mm:ssXXX", true, false);
    public static final DateTimeFormatter BASIC_ISO_DATE =
        new DateTimeFormatter("yyyyMMdd", true, false);
    public static final DateTimeFormatter ISO_DATE =
        new DateTimeFormatter("yyyy-MM-dd", true, false);
    public static final DateTimeFormatter ISO_TIME =
        new DateTimeFormatter("HH:mm:ss", true, false);
    public static final DateTimeFormatter ISO_DATE_TIME =
        new DateTimeFormatter("yyyy-MM-dd'T'HH:mm:ss", true, false);
    public static final DateTimeFormatter ISO_OFFSET_DATE_TIME =
        new DateTimeFormatter("yyyy-MM-dd'T'HH:mm:ssXXX", true, false);
    public static final DateTimeFormatter ISO_OFFSET_TIME =
        new DateTimeFormatter("HH:mm:ssXXX", true, false);
    public static final DateTimeFormatter RFC_1123_DATE_TIME =
        new DateTimeFormatter("EEE, dd MMM yyyy HH:mm:ss zzz", true, false);

    private static final String[] MONTHS_SHORT = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    private static final String[] MONTHS_LONG = {
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    };
    private static final String[] DAYS_SHORT = {
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
    };
    private static final String[] DAYS_LONG = {
        "Monday", "Tuesday", "Wednesday", "Thursday", "Friday",
        "Saturday", "Sunday"
    };

    private final String pattern;
    private final boolean patternBased;
    private final boolean instantOnly;

    private DateTimeFormatter(String pattern, boolean patternBased, boolean instantOnly)
    {
        this.pattern = pattern;
        this.patternBased = patternBased;
        this.instantOnly = instantOnly;
    }

    public static DateTimeFormatter ofLocalizedDate(FormatStyle dateStyle)
    {
        if (dateStyle == null)
        {
            throw new NullPointerException("dateStyle");
        }
        return ISO_LOCAL_DATE;
    }

    public static DateTimeFormatter ofLocalizedTime(FormatStyle timeStyle)
    {
        if (timeStyle == null)
        {
            throw new NullPointerException("timeStyle");
        }
        return ISO_LOCAL_TIME;
    }

    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateTimeStyle)
    {
        if (dateTimeStyle == null)
        {
            throw new NullPointerException("dateTimeStyle");
        }
        return ISO_LOCAL_DATE_TIME;
    }

    public static DateTimeFormatter ofLocalizedDateTime(FormatStyle dateStyle, FormatStyle timeStyle)
    {
        if (dateStyle == null || timeStyle == null)
        {
            throw new NullPointerException("style");
        }
        return ISO_LOCAL_DATE_TIME;
    }

    public static DateTimeFormatter ofPattern(String pattern)
    {
        if (pattern == null)
        {
            throw new NullPointerException("pattern");
        }
        validate(pattern);
        return new DateTimeFormatter(pattern, true, false);
    }

    public static DateTimeFormatter ofPattern(String pattern, Locale locale)
    {
        return ofPattern(pattern);
    }

    public DateTimeFormatter withZone(ZoneId zone)
    {
        return this;
    }

    public DateTimeFormatter withChronology(java.time.chrono.Chronology chronology)
    {
        return this;
    }

    public DateTimeFormatter withResolverStyle(ResolverStyle resolverStyle)
    {
        if (resolverStyle == null)
        {
            throw new NullPointerException("resolverStyle");
        }
        return this;
    }

    public DateTimeFormatter withLocale(Locale locale)
    {
        return this;
    }

    public String toString()
    {
        if (!patternBased)
        {
            return "ISO";
        }
        return pattern;
    }

    private static void validate(String pattern)
    {
        int i = 0;
        while (i < pattern.length())
        {
            char c = pattern.charAt(i);
            if (c == '\'')
            {
                i = i + 1;
                if (i < pattern.length() && pattern.charAt(i) == '\'')
                {
                    i = i + 1;
                    continue;
                }
                while (i < pattern.length() && pattern.charAt(i) != '\'')
                {
                    i = i + 1;
                }
                if (i >= pattern.length())
                {
                    throw new IllegalArgumentException("Unterminated quote in pattern");
                }
                i = i + 1;
                continue;
            }
            if (isLetter(c))
            {
                if (!isSupported(c))
                {
                    throw new IllegalArgumentException(
                        "Unsupported pattern letter: " + c);
                }
                while (i < pattern.length() && pattern.charAt(i) == c)
                {
                    i = i + 1;
                }
            }
            else
            {
                i = i + 1;
            }
        }
    }

    private static boolean isLetter(char c)
    {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isSupported(char c)
    {
        return "GyuMLdDHhKkmsSnaEzZXx".indexOf(c) >= 0;
    }

    // ------------------------------------------------------------------
    // Formatting
    // ------------------------------------------------------------------
    public String format(TemporalAccessor value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        if (instantOnly)
        {
            if (!(value instanceof Instant))
            {
                throw new IllegalArgumentException("ISO_INSTANT requires an Instant");
            }
            return value.toString();
        }
        LocalDate date;
        LocalTime time;
        int offsetSeconds = 0;
        boolean hasOffset = false;
        String zoneId = null;
        if (value instanceof ZonedDateTime)
        {
            ZonedDateTime zdt = (ZonedDateTime)value;
            date = zdt.toLocalDate();
            time = zdt.toLocalTime();
            offsetSeconds = zdt.getOffset().getTotalSeconds();
            hasOffset = true;
            zoneId = zdt.getZone().getId();
        }
        else if (value instanceof LocalDateTime)
        {
            LocalDateTime ldt = (LocalDateTime)value;
            date = ldt.toLocalDate();
            time = ldt.toLocalTime();
        }
        else if (value instanceof LocalDate)
        {
            date = (LocalDate)value;
            time = LocalTime.MIDNIGHT;
        }
        else if (value instanceof LocalTime)
        {
            date = LocalDate.of(1970, 1, 1);
            time = (LocalTime)value;
        }
        else if (value instanceof Instant)
        {
            ZonedDateTime zdt = ZonedDateTime.ofInstant((Instant)value, ZoneOffset.UTC);
            date = zdt.toLocalDate();
            time = zdt.toLocalTime();
            offsetSeconds = 0;
            hasOffset = true;
            zoneId = "UTC";
        }
        else
        {
            throw new IllegalArgumentException("Unsupported temporal type: " +
                value.getClass().getName());
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < pattern.length())
        {
            char c = pattern.charAt(i);
            if (c == '\'')
            {
                i = i + 1;
                if (i < pattern.length() && pattern.charAt(i) == '\'')
                {
                    out.append('\'');
                    i = i + 1;
                    continue;
                }
                while (i < pattern.length() && pattern.charAt(i) != '\'')
                {
                    out.append(pattern.charAt(i));
                    i = i + 1;
                }
                i = i + 1;
                continue;
            }
            if (!isLetter(c))
            {
                out.append(c);
                i = i + 1;
                continue;
            }
            int start = i;
            while (i < pattern.length() && pattern.charAt(i) == c)
            {
                i = i + 1;
            }
            int count = i - start;
            appendField(out, c, count, date, time, offsetSeconds, hasOffset, zoneId);
        }
        return out.toString();
    }

    private static void appendField(StringBuilder out, char c, int count,
                                    LocalDate date, LocalTime time,
                                    int offsetSeconds, boolean hasOffset,
                                    String zoneId)
    {
        switch (c)
        {
        case 'G':
            out.append("AD");
            return;
        case 'y':
        case 'u':
            appendYear(out, date.getYear(), count);
            return;
        case 'M':
        case 'L':
            if (count <= 2)
            {
                pad(out, date.getMonthValue(), count);
            }
            else if (count == 3)
            {
                out.append(MONTHS_SHORT[date.getMonthValue() - 1]);
            }
            else
            {
                out.append(MONTHS_LONG[date.getMonthValue() - 1]);
            }
            return;
        case 'd':
            pad(out, date.getDayOfMonth(), count);
            return;
        case 'D':
            pad(out, date.getDayOfYear(), count < 3 ? 3 : count);
            return;
        case 'H':
            pad(out, time.getHour(), count);
            return;
        case 'h':
        {
            int h = time.getHour() % 12;
            if (h == 0)
            {
                h = 12;
            }
            pad(out, h, count);
            return;
        }
        case 'K':
        case 'k':
        {
            int h = time.getHour() % 12;
            if (c == 'k' && h == 0)
            {
                h = 12;
            }
            pad(out, h, count);
            return;
        }
        case 'm':
            pad(out, time.getMinute(), count);
            return;
        case 's':
            pad(out, time.getSecond(), count);
            return;
        case 'S':
            appendFraction(out, time.getNano(), count);
            return;
        case 'n':
            pad(out, time.getNano(), count < 9 ? 9 : count);
            return;
        case 'a':
            out.append(time.getHour() < 12 ? "AM" : "PM");
            return;
        case 'E':
        {
            int dow = Civil.dayOfWeek(date.toEpochDay());
            out.append(count >= 4 ? DAYS_LONG[dow - 1] : DAYS_SHORT[dow - 1]);
            return;
        }
        case 'z':
            out.append(zoneId == null ? (hasOffset ? offsetText(offsetSeconds, 3) : "UTC")
                                      : zoneId);
            return;
        case 'Z':
            out.append(offsetText(offsetSeconds, count >= 5 ? 5 : 0));
            return;
        case 'X':
            out.append(offsetText(offsetSeconds, count >= 3 ? 3 : count));
            return;
        case 'x':
            out.append(xOffsetText(offsetSeconds, count));
            return;
        default:
            return;
        }
    }

    private static void appendYear(StringBuilder out, int year, int count)
    {
        if (count == 2)
        {
            int v = year % 100;
            if (v < 0)
            {
                v = -v;
            }
            pad(out, v, 2);
            return;
        }
        if (year < 0)
        {
            out.append('-');
        }
        int digits = count < 4 ? 4 : count;
        pad(out, year < 0 ? -year : year, digits);
    }

    private static void appendFraction(StringBuilder out, int nanos, int count)
    {
        String fraction = Integer.toString(nanos);
        while (fraction.length() < 9)
        {
            fraction = "0" + fraction;
        }
        if (count < 9)
        {
            fraction = fraction.substring(0, count);
        }
        else if (count > 9)
        {
            while (fraction.length() < count)
            {
                fraction = fraction + "0";
            }
        }
        out.append(fraction);
    }

    private static void pad(StringBuilder out, int value, int width)
    {
        if (value < 0)
        {
            out.append('-');
            value = -value;
        }
        String text = Integer.toString(value);
        while (text.length() < width)
        {
            text = "0" + text;
        }
        out.append(text);
    }

    static String offsetText(int totalSeconds, int style)
    {
        if (totalSeconds == 0 && (style == 1 || style == 2 || style == 3))
        {
            return style == 3 ? "Z" : "Z";
        }
        String base = ZoneOffset.formatOffset(totalSeconds);
        if (style == 3)
        {
            return base;
        }
        if (style == 5)
        {
            return base.equals("Z") ? "Z" : base;
        }
        String body = base.equals("Z") ? "+00:00" : base;
        return body.substring(0, 3) + body.substring(4);
    }

    static String xOffsetText(int totalSeconds, int count)
    {
        if (totalSeconds == 0)
        {
            return count >= 3 ? "+00:00" : "+00";
        }
        String text = ZoneOffset.formatOffset(totalSeconds);
        if (count >= 3)
        {
            return text;
        }
        return text.substring(0, 3);
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------
    public TemporalAccessor parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        if (instantOnly)
        {
            return Instant.parse(s);
        }
        int year = Integer.MIN_VALUE;
        int month = Integer.MIN_VALUE;
        int day = Integer.MIN_VALUE;
        int hour = Integer.MIN_VALUE;
        int minute = Integer.MIN_VALUE;
        int second = 0;
        int nano = 0;
        int hour12 = Integer.MIN_VALUE;
        boolean pm = false;
        boolean hasAmPm = false;
        int offsetSeconds = 0;
        boolean hasOffset = false;
        String zoneId = null;
        int i = 0;
        int p = 0;
        while (p < pattern.length())
        {
            char c = pattern.charAt(p);
            if (c == '\'')
            {
                p = p + 1;
                if (p < pattern.length() && pattern.charAt(p) == '\'')
                {
                    if (i >= s.length() || s.charAt(i) != '\'')
                    {
                        throw parseError(s, i, "Expected ''");
                    }
                    i = i + 1;
                    p = p + 1;
                    continue;
                }
                while (p < pattern.length() && pattern.charAt(p) != '\'')
                {
                    if (i >= s.length() || s.charAt(i) != pattern.charAt(p))
                    {
                        throw parseError(s, i, "Literal mismatch");
                    }
                    i = i + 1;
                    p = p + 1;
                }
                p = p + 1;
                continue;
            }
            if (!isLetter(c))
            {
                if (i >= s.length() || s.charAt(i) != c)
                {
                    throw parseError(s, i, "Expected '" + c + "'");
                }
                i = i + 1;
                p = p + 1;
                continue;
            }
            int start = p;
            while (p < pattern.length() && pattern.charAt(p) == c)
            {
                p = p + 1;
            }
            int count = p - start;
            if (c == 'y' || c == 'u')
            {
                int[] r = readNumber(s, i, count == 2 ? 2 : (count < 4 ? 4 : count),
                                     count >= 4 ? 10 : 4);
                year = r[0];
                if (count == 2)
                {
                    year = year + 2000;
                }
                i = r[1];
            }
            else if (c == 'M' || c == 'L')
            {
                if (count <= 2)
                {
                    int[] r = readNumber(s, i, 1, 2);
                    month = r[0];
                    i = r[1];
                }
                else
                {
                    int found = -1;
                    int k = 0;
                    String[] table = count == 3 ? MONTHS_SHORT : MONTHS_LONG;
                    while (k < 12)
                    {
                        if (startsWithIgnoreCase(s, i, table[k]))
                        {
                            found = k + 1;
                            i = i + table[k].length();
                            break;
                        }
                        k = k + 1;
                    }
                    if (found < 0)
                    {
                        throw parseError(s, i, "Invalid month name");
                    }
                    month = found;
                }
            }
            else if (c == 'd')
            {
                int[] r = readNumber(s, i, 1, 2);
                day = r[0];
                i = r[1];
            }
            else if (c == 'D')
            {
                int[] r = readNumber(s, i, 1, 3);
                i = r[1];
            }
            else if (c == 'H')
            {
                int[] r = readNumber(s, i, 1, 2);
                hour = r[0];
                i = r[1];
            }
            else if (c == 'h')
            {
                int[] r = readNumber(s, i, 1, 2);
                hour12 = r[0];
                i = r[1];
            }
            else if (c == 'K' || c == 'k')
            {
                int[] r = readNumber(s, i, 1, 2);
                hour12 = r[0];
                if (c == 'k' && hour12 == 12)
                {
                    hour12 = 0;
                }
                i = r[1];
            }
            else if (c == 'm')
            {
                int[] r = readNumber(s, i, 1, 2);
                minute = r[0];
                i = r[1];
            }
            else if (c == 's')
            {
                int[] r = readNumber(s, i, 1, 2);
                second = r[0];
                i = r[1];
            }
            else if (c == 'S')
            {
                int[] r = readNumber(s, i, count, count);
                int value = r[0];
                int scale = count;
                while (scale < 9)
                {
                    value = value * 10;
                    scale = scale + 1;
                }
                nano = value;
                i = r[1];
            }
            else if (c == 'n')
            {
                int[] r = readNumber(s, i, 9, 9);
                nano = r[0];
                i = r[1];
            }
            else if (c == 'a')
            {
                if (startsWithIgnoreCase(s, i, "AM"))
                {
                    hasAmPm = true;
                    pm = false;
                    i = i + 2;
                }
                else if (startsWithIgnoreCase(s, i, "PM"))
                {
                    hasAmPm = true;
                    pm = true;
                    i = i + 2;
                }
                else
                {
                    throw parseError(s, i, "Invalid AM/PM");
                }
            }
            else if (c == 'E' || c == 'G')
            {
                while (i < s.length() && isLetter(s.charAt(i)))
                {
                    i = i + 1;
                }
            }
            else if (c == 'z')
            {
                int begin = i;
                while (i < s.length() && s.charAt(i) != ' ' && s.charAt(i) != ',' &&
                       s.charAt(i) != ']')
                {
                    i = i + 1;
                }
                zoneId = s.substring(begin, i);
                if (zoneId.equals("UTC") || zoneId.equals("GMT"))
                {
                    hasOffset = true;
                    offsetSeconds = 0;
                }
            }
            else if (c == 'Z' || c == 'X' || c == 'x')
            {
                int begin = i;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-'))
                {
                    i = i + 1;
                    while (i < s.length() &&
                           ((s.charAt(i) >= '0' && s.charAt(i) <= '9') || s.charAt(i) == ':'))
                    {
                        i = i + 1;
                    }
                }
                else if (i < s.length() && (s.charAt(i) == 'Z' || s.charAt(i) == 'z'))
                {
                    i = i + 1;
                }
                String offsetText = s.substring(begin, i);
                if (offsetText.length() == 0)
                {
                    throw parseError(s, i, "Invalid offset");
                }
                ZoneOffset offset = ZoneOffset.of(offsetText);
                offsetSeconds = offset.getTotalSeconds();
                hasOffset = true;
            }
        }
        if (i != s.length())
        {
            throw parseError(s, i, "Unparsed text");
        }
        if (hasAmPm)
        {
            int h = hour12;
            if (h < 1 || h > 12)
            {
                throw parseError(s, 0, "Invalid hour");
            }
            if (pm && h != 12)
            {
                h = h + 12;
            }
            else if (!pm && h == 12)
            {
                h = 0;
            }
            hour = h;
        }
        else if (hour12 != Integer.MIN_VALUE)
        {
            hour = hour12;
        }
        LocalDate date = null;
        if (year != Integer.MIN_VALUE || month != Integer.MIN_VALUE ||
            day != Integer.MIN_VALUE)
        {
            if (month == Integer.MIN_VALUE || day == Integer.MIN_VALUE)
            {
                throw parseError(s, 0, "Incomplete date");
            }
            if (year == Integer.MIN_VALUE)
            {
                year = 1970;
            }
            date = LocalDate.of(year, month, day);
        }
        LocalTime time = null;
        if (hour != Integer.MIN_VALUE || minute != Integer.MIN_VALUE)
        {
            if (minute == Integer.MIN_VALUE)
            {
                minute = 0;
            }
            if (hour == Integer.MIN_VALUE)
            {
                hour = 0;
            }
            time = LocalTime.of(hour, minute, second, nano);
        }
        if (date != null && time != null)
        {
            LocalDateTime ldt = LocalDateTime.of(date, time);
            if (hasOffset)
            {
                return ZonedDateTime.of(ldt, ZoneOffset.ofTotalSeconds(offsetSeconds));
            }
            if (zoneId != null)
            {
                return ZonedDateTime.of(ldt, ZoneId.of(zoneId));
            }
            return ldt;
        }
        if (date != null)
        {
            return date;
        }
        if (time != null)
        {
            return time;
        }
        throw parseError(s, 0, "No fields parsed");
    }

    private static DateTimeParseException parseError(String text, int index, String message)
    {
        return new DateTimeParseException(message + " at index " + index, text, index);
    }

    private static boolean startsWithIgnoreCase(String text, int offset, String value)
    {
        if (offset + value.length() > text.length())
        {
            return false;
        }
        int i = 0;
        while (i < value.length())
        {
            char a = text.charAt(offset + i);
            char b = value.charAt(i);
            if (a >= 'a' && a <= 'z')
            {
                a = (char)(a - 32);
            }
            if (b >= 'a' && b <= 'z')
            {
                b = (char)(b - 32);
            }
            if (a != b)
            {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    /** Reads between min and max digits; returns {value, nextIndex}. */
    private static int[] readNumber(String s, int index, int min, int max)
    {
        int value = 0;
        int count = 0;
        int i = index;
        while (i < s.length() && count < max)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                break;
            }
            value = value * 10 + (c - '0');
            count = count + 1;
            i = i + 1;
        }
        if (count < min)
        {
            throw parseError(s, index, "Expected at least " + min + " digits");
        }
        return new int[] {value, i};
    }
}
