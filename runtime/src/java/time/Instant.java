package java.time;

/**
 * A point on the time-line measured in seconds and nanoseconds from the
 * epoch.  Formatted output always uses UTC and a trailing 'Z'.
 */
public final class Instant implements java.time.temporal.Temporal,
        java.time.temporal.TemporalAdjuster, Comparable<Instant>
{
    public static final Instant EPOCH = new Instant(0L, 0);
    public static final Instant MIN = new Instant(-31557014167219200L, 0);
    public static final Instant MAX = new Instant(31556889864403199L, 999999999);

    private final long seconds;
    private final int nanos;

    private Instant(long seconds, int nanos)
    {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    private static Instant create(long seconds, long nanoAdjustment)
    {
        long sec = seconds + Math.floorDiv(nanoAdjustment, 1000000000L);
        int nano = (int)Math.floorMod(nanoAdjustment, 1000000000L);
        return new Instant(sec, nano);
    }

    public static Instant now()
    {
        return ofEpochMilli(System.currentTimeMillis());
    }

    public static Instant ofEpochSecond(long epochSecond)
    {
        return new Instant(epochSecond, 0);
    }

    public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment)
    {
        return create(epochSecond, nanoAdjustment);
    }

    public static Instant ofEpochMilli(long epochMilli)
    {
        long sec = Math.floorDiv(epochMilli, 1000L);
        int nano = (int)(Math.floorMod(epochMilli, 1000L) * 1000000L);
        return new Instant(sec, nano);
    }

    public static Instant from(java.util.Date date)
    {
        return ofEpochMilli(date.getTime());
    }

    public static Instant parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int t = s.indexOf('T');
        if (t < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid instant: " + s, s, 0);
        }
        String datePart = s.substring(0, t);
        String rest = s.substring(t + 1);
        LocalDate date = LocalDate.parse(datePart);
        char last = rest.charAt(rest.length() - 1);
        int offsetSeconds = 0;
        String timePart;
        if (last == 'Z' || last == 'z')
        {
            timePart = rest.substring(0, rest.length() - 1);
        }
        else
        {
            int sign = -1;
            int plus = rest.lastIndexOf('+');
            int minus = rest.lastIndexOf('-');
            int pos = plus > minus ? plus : minus;
            if (pos < 0)
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid instant (missing offset): " + s, s, 0);
            }
            ZoneOffset offset = ZoneOffset.of(rest.substring(pos));
            offsetSeconds = offset.getTotalSeconds();
            timePart = rest.substring(0, pos);
        }
        long[] time = LocalTime.parseTimeFields(timePart);
        int hour = (int)time[0];
        int minute = (int)time[1];
        int second = (int)time[2];
        int nano = (int)time[3];
        long days = date.toEpochDay();
        long total = days * 86400L + hour * 3600L + minute * 60L + second - offsetSeconds;
        return new Instant(total, nano);
    }

    public long getEpochSecond()
    {
        return seconds;
    }

    public int getNano()
    {
        return nanos;
    }

    public long toEpochMilli()
    {
        return seconds * 1000L + nanos / 1000000;
    }

    public Instant plus(Duration duration)
    {
        return create(seconds + duration.getSeconds(), nanos + (long)duration.getNano());
    }

    public Instant minus(Duration duration)
    {
        return create(seconds - duration.getSeconds(), nanos - (long)duration.getNano());
    }

    public Instant plusMillis(long millis)
    {
        return create(seconds + Math.floorDiv(millis, 1000L),
                      nanos + Math.floorMod(millis, 1000L) * 1000000L);
    }

    public Instant plusSeconds(long secondsToAdd)
    {
        return create(seconds + secondsToAdd, nanos);
    }

    public Instant plusNanos(long nanosToAdd)
    {
        return create(seconds, nanos + nanosToAdd);
    }

    public Instant minusMillis(long millis)
    {
        return plusMillis(-millis);
    }

    public Instant minusSeconds(long secondsToSubtract)
    {
        return create(seconds - secondsToSubtract, nanos);
    }

    public Instant minusNanos(long nanosToSubtract)
    {
        return create(seconds, nanos - nanosToSubtract);
    }

    public boolean isBefore(Instant other)
    {
        return compareTo(other) < 0;
    }

    public boolean isAfter(Instant other)
    {
        return compareTo(other) > 0;
    }

    public int compareTo(Instant other)
    {
        if (seconds != other.seconds)
        {
            return seconds < other.seconds ? -1 : 1;
        }
        if (nanos == other.nanos)
        {
            return 0;
        }
        return nanos < other.nanos ? -1 : 1;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Instant))
        {
            return false;
        }
        Instant o = (Instant)other;
        return seconds == o.seconds && nanos == o.nanos;
    }

    public int hashCode()
    {
        return (int)(seconds ^ (seconds >>> 32)) * 31 + nanos * 51;
    }

    public String toString()
    {
        LocalDateTime ldt = LocalDateTime.ofEpochSecond(seconds, 0, ZoneOffset.UTC);
        String base = ldt.toString();
        if (nanos == 0)
        {
            return base + "Z";
        }
        StringBuilder out = new StringBuilder();
        out.append(base);
        out.append('.');
        String fraction = Integer.toString(nanos);
        int pad = 9 - fraction.length();
        while (pad > 0)
        {
            out.append('0');
            pad = pad - 1;
        }
        out.append(fraction);
        out.append('Z');
        return out.toString();
    }
}
