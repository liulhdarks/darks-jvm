package java.util.concurrent;

/**
 * Time unit constants used by the concurrency APIs.  darks-jvm schedules in
 * milliseconds, so the finer units are rounded up by {@link #sleep(long)}.
 */
public enum TimeUnit
{
    NANOSECONDS(1L),
    MICROSECONDS(1000L),
    MILLISECONDS(1000000L),
    SECONDS(1000000000L),
    MINUTES(60000000000L),
    HOURS(3600000000000L),
    DAYS(86400000000000L);

    private final long nanos;

    TimeUnit(long nanos)
    {
        this.nanos = nanos;
    }

    public long toNanos(long duration)
    {
        return duration * nanos;
    }

    public long toMillis(long duration)
    {
        if (nanos >= 1000000L)
        {
            return duration * (nanos / 1000000L);
        }
        return duration / (1000000L / nanos);
    }

    public long toSeconds(long duration)
    {
        if (nanos >= 1000000000L)
        {
            return duration * (nanos / 1000000000L);
        }
        return duration / (1000000000L / nanos);
    }

    public long convert(long duration, TimeUnit unit)
    {
        if (unit == this)
        {
            return duration;
        }
        return (duration * unit.nanos) / nanos;
    }

    public void sleep(long timeout) throws InterruptedException
    {
        long millis = toMillis(timeout);
        if (millis <= 0 && timeout > 0)
        {
            millis = 1;
        }
        Thread.sleep(millis);
    }
}
