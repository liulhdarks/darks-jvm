package java.time.chrono;

/**
 * Minimal chronology-aware view of a zoned date-time.  Jackson's
 * ZonedDateTimeSerializer binds {@code ChronoZonedDateTime::toEpochSecond}
 * through invokedynamic, so the interface and the method must exist and be
 * implemented by {@link java.time.ZonedDateTime}.
 */
public interface ChronoZonedDateTime
{
    long toEpochSecond();
}
