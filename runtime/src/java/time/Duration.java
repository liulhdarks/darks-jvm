package java.time;

/**
 * An amount of time measured in seconds and nanoseconds, both storing the
 * same sign.  Arithmetic is deliberately simple: values that overflow the
 * 64 bit nanosecond range are not detected.
 */
public final class Duration implements Comparable<Duration>
{
    public static final Duration ZERO = new Duration(0L, 0);

    private final long seconds;
    private final int nanos;

    private Duration(long seconds, int nanos)
    {
        this.seconds = seconds;
        this.nanos = nanos;
    }

    private static Duration create(long seconds, long nanoAdjustment)
    {
        long sec = seconds + Math.floorDiv(nanoAdjustment, 1000000000L);
        int nano = (int)Math.floorMod(nanoAdjustment, 1000000000L);
        return new Duration(sec, nano);
    }

    public static Duration ofDays(long days)
    {
        return create(days * 86400L, 0);
    }

    public static Duration ofHours(long hours)
    {
        return create(hours * 3600L, 0);
    }

    public static Duration ofMinutes(long minutes)
    {
        return create(minutes * 60L, 0);
    }

    public static Duration ofSeconds(long seconds)
    {
        return create(seconds, 0);
    }

    public static Duration ofSeconds(long seconds, long nanoAdjustment)
    {
        return create(seconds, nanoAdjustment);
    }

    public static Duration ofMillis(long millis)
    {
        return create(Math.floorDiv(millis, 1000L),
                      Math.floorMod(millis, 1000L) * 1000000L);
    }

    public static Duration ofNanos(long nanos)
    {
        return create(0L, nanos);
    }

    public static Duration of(long amount, java.time.temporal.TemporalUnit unit)
    {
        if (unit == null)
        {
            throw new NullPointerException("unit");
        }
        if (unit == java.time.temporal.ChronoUnit.NANOS)
        {
            return ofNanos(amount);
        }
        if (unit == java.time.temporal.ChronoUnit.MICROS)
        {
            return create(Math.floorDiv(amount, 1000000L),
                          Math.floorMod(amount, 1000000L) * 1000L);
        }
        if (unit == java.time.temporal.ChronoUnit.MILLIS)
        {
            return ofMillis(amount);
        }
        if (unit == java.time.temporal.ChronoUnit.SECONDS)
        {
            return ofSeconds(amount);
        }
        if (unit == java.time.temporal.ChronoUnit.MINUTES)
        {
            return ofMinutes(amount);
        }
        if (unit == java.time.temporal.ChronoUnit.HOURS)
        {
            return ofHours(amount);
        }
        if (unit == java.time.temporal.ChronoUnit.HALF_DAYS)
        {
            return ofHours(Math.multiplyExact(amount, 12L));
        }
        if (unit == java.time.temporal.ChronoUnit.DAYS)
        {
            return ofDays(amount);
        }
        // Units of a month or larger do not have a fixed length.
        throw new java.time.temporal.UnsupportedTemporalTypeException(
            "Unit must not be estimated: " + unit);
    }

    public static Duration between(Instant start, Instant end)
    {
        long sec = end.getEpochSecond() - start.getEpochSecond();
        return create(sec, (long)end.getNano() - (long)start.getNano());
    }

    public long getSeconds()
    {
        return seconds;
    }

    public int getNano()
    {
        return nanos;
    }

    public long toMillis()
    {
        return seconds * 1000L + nanos / 1000000;
    }

    public long toNanos()
    {
        return seconds * 1000000000L + nanos;
    }

    public long toSeconds()
    {
        return seconds;
    }

    public long toMinutes()
    {
        return seconds / 60L;
    }

    public long toHours()
    {
        return seconds / 3600L;
    }

    public long toDays()
    {
        return seconds / 86400L;
    }

    public Duration plus(Duration other)
    {
        return create(seconds + other.seconds, (long)nanos + other.nanos);
    }

    public Duration minus(Duration other)
    {
        return create(seconds - other.seconds, (long)nanos - other.nanos);
    }

    public Duration plusSeconds(long value)
    {
        return create(seconds + value, nanos);
    }

    public Duration plusMillis(long value)
    {
        return create(seconds + Math.floorDiv(value, 1000L),
                      nanos + Math.floorMod(value, 1000L) * 1000000L);
    }

    public Duration plusNanos(long value)
    {
        return create(seconds, (long)nanos + value);
    }

    public Duration minusSeconds(long value)
    {
        return create(seconds - value, nanos);
    }

    public Duration minusMillis(long value)
    {
        return plusMillis(-value);
    }

    public Duration multipliedBy(long multiplicand)
    {
        return create(seconds * multiplicand, (long)nanos * multiplicand);
    }

    public Duration dividedBy(long divisor)
    {
        if (divisor == 0)
        {
            throw new ArithmeticException("division by zero");
        }
        long totalNanos = seconds * 1000000000L + nanos;
        if (seconds > 9223372036L || seconds < -9223372036L)
        {
            // Fall back to the (seconds, nanos) split for large values.
            long sec = seconds / divisor;
            long rem = seconds % divisor;
            long extra = (rem * 1000000000L + nanos) / divisor;
            return create(sec + Math.floorDiv(extra, 1000000000L),
                          Math.floorMod(extra, 1000000000L));
        }
        return create(0L, totalNanos / divisor);
    }

    public Duration negated()
    {
        return new Duration(-seconds - (nanos == 0 ? 0 : 1),
                            nanos == 0 ? 0 : 1000000000 - nanos);
    }

    public boolean isZero()
    {
        return seconds == 0 && nanos == 0;
    }

    public boolean isNegative()
    {
        return seconds < 0;
    }

    public int compareTo(Duration other)
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
        if (!(other instanceof Duration))
        {
            return false;
        }
        Duration o = (Duration)other;
        return seconds == o.seconds && nanos == o.nanos;
    }

    public int hashCode()
    {
        return (int)(seconds ^ (seconds >>> 32)) * 31 + nanos;
    }

    public static Duration parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int i = 0;
        boolean negative = false;
        if (i < s.length() && (s.charAt(i) == '-' || s.charAt(i) == '+'))
        {
            negative = s.charAt(i) == '-';
            i = i + 1;
        }
        if (i + 2 > s.length() || s.charAt(i) != 'P' || s.charAt(i + 1) != 'T')
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid duration: " + s, s, i);
        }
        i = i + 2;
        long seconds = 0;
        long nanos = 0;
        boolean any = false;
        while (i < s.length())
        {
            int start = i;
            while (i < s.length() && (s.charAt(i) == '.' ||
                   (s.charAt(i) >= '0' && s.charAt(i) <= '9')))
            {
                i = i + 1;
            }
            if (i >= s.length() || start == i)
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid duration: " + s, s, start);
            }
            char unit = s.charAt(i);
            i = i + 1;
            double value = Double.parseDouble(s.substring(start, i - 1));
            any = true;
            if (unit == 'H')
            {
                seconds = seconds + (long)(value * 3600.0);
                nanos = nanos + (long)((value * 3600.0 - (double)(long)(value * 3600.0)) * 1000000000.0);
            }
            else if (unit == 'M')
            {
                seconds = seconds + (long)(value * 60.0);
                nanos = nanos + (long)((value * 60.0 - (double)(long)(value * 60.0)) * 1000000000.0);
            }
            else if (unit == 'S')
            {
                seconds = seconds + (long)value;
                nanos = nanos + (long)((value - (double)(long)value) * 1000000000.0);
            }
            else if (unit == 'D')
            {
                seconds = seconds + (long)(value * 86400.0);
            }
            else
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid duration unit: " + s, s, i - 1);
            }
        }
        if (!any)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid duration: " + s, s, 0);
        }
        Duration result = create(seconds, nanos);
        return negative ? result.negated() : result;
    }

    public String toString()
    {
        if (seconds == 0 && nanos == 0)
        {
            return "PT0S";
        }
        boolean negative = seconds < 0;
        long sec;
        int nano;
        if (negative)
        {
            if (nanos == 0)
            {
                sec = -seconds;
                nano = 0;
            }
            else
            {
                sec = -(seconds + 1);
                nano = 1000000000 - nanos;
            }
        }
        else
        {
            sec = seconds;
            nano = nanos;
        }
        long hours = sec / 3600L;
        long minutes = (sec % 3600L) / 60L;
        long secs = sec % 60L;
        StringBuilder out = new StringBuilder();
        if (negative)
        {
            out.append('-');
        }
        out.append("PT");
        if (hours != 0)
        {
            out.append(Long.toString(hours));
            out.append('H');
        }
        if (minutes != 0)
        {
            out.append(Long.toString(minutes));
            out.append('M');
        }
        if (secs != 0 || nano != 0 || (hours == 0 && minutes == 0))
        {
            out.append(Long.toString(secs));
            if (nano != 0)
            {
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
                out.append('.');
                out.append(fraction);
            }
            out.append('S');
        }
        return out.toString();
    }
}
