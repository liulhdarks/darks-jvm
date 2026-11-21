package java.time;

/** A time of day without a time-zone, second precision plus nanoseconds. */
public final class LocalTime implements java.time.temporal.Temporal, Comparable<LocalTime>
{
    public static final LocalTime MIN = new LocalTime(0, 0, 0, 0);
    public static final LocalTime MAX = new LocalTime(23, 59, 59, 999999999);
    public static final LocalTime MIDNIGHT = MIN;
    public static final LocalTime NOON = new LocalTime(12, 0, 0, 0);

    private final int hour;
    private final int minute;
    private final int second;
    private final int nano;

    private LocalTime(int hour, int minute, int second, int nano)
    {
        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.nano = nano;
    }

    public static LocalTime of(int hour, int minute)
    {
        return of(hour, minute, 0, 0);
    }

    public static LocalTime of(int hour, int minute, int second)
    {
        return of(hour, minute, second, 0);
    }

    public static LocalTime of(int hour, int minute, int second, int nanoOfSecond)
    {
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 ||
            second < 0 || second > 59 || nanoOfSecond < 0 || nanoOfSecond > 999999999)
        {
            throw new DateTimeException("Invalid time");
        }
        return new LocalTime(hour, minute, second, nanoOfSecond);
    }

    public static LocalTime ofSecondOfDay(long secondOfDay)
    {
        if (secondOfDay < 0 || secondOfDay >= 86400)
        {
            throw new DateTimeException("Invalid value for second of day");
        }
        int h = (int)(secondOfDay / 3600L);
        int m = (int)((secondOfDay % 3600L) / 60L);
        int s = (int)(secondOfDay % 60L);
        return new LocalTime(h, m, s, 0);
    }

    public static LocalTime ofNanoOfDay(long nanoOfDay)
    {
        if (nanoOfDay < 0 || nanoOfDay >= 86400000000000L)
        {
            throw new DateTimeException("Invalid value for nano of day");
        }
        int h = (int)(nanoOfDay / 3600000000000L);
        int m = (int)((nanoOfDay / 60000000000L) % 60L);
        int s = (int)((nanoOfDay / 1000000000L) % 60L);
        int n = (int)(nanoOfDay % 1000000000L);
        return new LocalTime(h, m, s, n);
    }

    public static LocalTime now()
    {
        return LocalDateTime.now().toLocalTime();
    }

    public static LocalTime parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        long[] fields = parseTimeFields(text.toString());
        return new LocalTime((int)fields[0], (int)fields[1], (int)fields[2], (int)fields[3]);
    }

    /** Returns {hour, minute, second, nano}; accepts HH:mm[:ss[.fff...]]. */
    static long[] parseTimeFields(String s)
    {
        int first = s.indexOf(':');
        if (first < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid time: " + s, s, 0);
        }
        int hour = parseInt(s, 0, first, s);
        int secondColon = s.indexOf(':', first + 1);
        int minute;
        int second = 0;
        int nano = 0;
        int end;
        if (secondColon < 0)
        {
            minute = parseInt(s, first + 1, s.length(), s);
            end = s.length();
        }
        else
        {
            minute = parseInt(s, first + 1, secondColon, s);
            int dot = s.indexOf('.', secondColon + 1);
            int secEnd = dot < 0 ? s.length() : dot;
            second = parseInt(s, secondColon + 1, secEnd, s);
            if (dot >= 0)
            {
                int digits = s.length() - dot - 1;
                if (digits <= 0 || digits > 9)
                {
                    throw new java.time.format.DateTimeParseException(
                        "Invalid fraction: " + s, s, dot);
                }
                int value = parseInt(s, dot + 1, s.length(), s);
                int scale = digits;
                while (scale < 9)
                {
                    value = value * 10;
                    scale = scale + 1;
                }
                nano = value;
            }
            end = s.length();
        }
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59 ||
            second < 0 || second > 59)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid time: " + s, s, 0);
        }
        return new long[] {hour, minute, second, nano};
    }

    private static int parseInt(String s, int from, int to, String original)
    {
        if (from >= to)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid number in: " + original, original, from);
        }
        int value = 0;
        int i = from;
        while (i < to)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid number in: " + original, original, i);
            }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return value;
    }

    public int getHour()
    {
        return hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public int getSecond()
    {
        return second;
    }

    public int getNano()
    {
        return nano;
    }

    public int toSecondOfDay()
    {
        return hour * 3600 + minute * 60 + second;
    }

    public long toNanoOfDay()
    {
        return ((long)hour * 3600L + (long)minute * 60L + second) * 1000000000L + nano;
    }

    public LocalTime plusHours(long hoursToAdd)
    {
        return plusNanos(((hoursToAdd % 24L) * 3600L) * 1000000000L);
    }

    public LocalTime plusMinutes(long minutesToAdd)
    {
        return plusNanos(((minutesToAdd % 1440L) * 60L) * 1000000000L);
    }

    public LocalTime plusSeconds(long secondsToAdd)
    {
        return plusNanos((secondsToAdd % 86400L) * 1000000000L);
    }

    public LocalTime plusNanos(long nanosToAdd)
    {
        if (nanosToAdd == 0)
        {
            return this;
        }
        long nod = toNanoOfDay() + nanosToAdd;
        nod = Math.floorMod(nod, 86400000000000L);
        return ofNanoOfDay(nod);
    }

    public LocalTime minusHours(long hoursToSubtract)
    {
        return plusHours(-(hoursToSubtract % 24L));
    }

    public LocalTime minusMinutes(long minutesToSubtract)
    {
        return plusMinutes(-(minutesToSubtract % 1440L));
    }

    public LocalTime minusSeconds(long secondsToSubtract)
    {
        return plusSeconds(-(secondsToSubtract % 86400L));
    }

    public LocalTime minusNanos(long nanosToSubtract)
    {
        return plusNanos(-nanosToSubtract);
    }

    public LocalTime withHour(int newHour)
    {
        return of(newHour, minute, second, nano);
    }

    public LocalTime withMinute(int newMinute)
    {
        return of(hour, newMinute, second, nano);
    }

    public LocalTime withSecond(int newSecond)
    {
        return of(hour, minute, newSecond, nano);
    }

    public LocalTime withNano(int newNano)
    {
        return of(hour, minute, second, newNano);
    }

    public int compareTo(LocalTime other)
    {
        if (hour != other.hour)
        {
            return hour < other.hour ? -1 : 1;
        }
        if (minute != other.minute)
        {
            return minute < other.minute ? -1 : 1;
        }
        if (second != other.second)
        {
            return second < other.second ? -1 : 1;
        }
        if (nano == other.nano)
        {
            return 0;
        }
        return nano < other.nano ? -1 : 1;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof LocalTime))
        {
            return false;
        }
        LocalTime o = (LocalTime)other;
        return hour == o.hour && minute == o.minute && second == o.second && nano == o.nano;
    }

    public int hashCode()
    {
        return hour * 3600 + minute * 60 + second + nano * 7;
    }

    public String format(java.time.format.DateTimeFormatter formatter)
    {
        return formatter.format(this);
    }

    public String toString()
    {
        StringBuilder out = new StringBuilder();
        appendTwo(out, hour);
        out.append(':');
        appendTwo(out, minute);
        if (second != 0 || nano != 0)
        {
            out.append(':');
            appendTwo(out, second);
            if (nano != 0)
            {
                out.append('.');
                String fraction = Integer.toString(nano);
                while (fraction.length() < 9)
                {
                    fraction = "0" + fraction;
                }
                while (fraction.length() > 1 &&
                       fraction.charAt(fraction.length() - 1) == '0')
                {
                    fraction = fraction.substring(0, fraction.length() - 1);
                }
                out.append(fraction);
            }
        }
        return out.toString();
    }

    static void appendTwo(StringBuilder out, int value)
    {
        if (value < 10)
        {
            out.append('0');
        }
        out.append(Integer.toString(value));
    }
}
