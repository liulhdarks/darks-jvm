package java.time;

import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

/** A date-time with a fixed offset from UTC. */
public final class OffsetDateTime implements Temporal, Comparable<OffsetDateTime>
{
    private final LocalDateTime dateTime;
    private final ZoneOffset offset;

    private OffsetDateTime(LocalDateTime dateTime, ZoneOffset offset)
    {
        this.dateTime = dateTime;
        this.offset = offset;
    }

    public static OffsetDateTime now()
    {
        return ofInstant(Instant.now(), ZoneId.systemDefault());
    }

    public static OffsetDateTime of(LocalDateTime dateTime, ZoneOffset offset)
    {
        if (dateTime == null || offset == null)
        {
            throw new NullPointerException();
        }
        return new OffsetDateTime(dateTime, offset);
    }

    public static OffsetDateTime of(int year, int month, int day, int hour,
                                    int minute, int second, int nano,
                                    ZoneOffset offset)
    {
        return of(LocalDateTime.of(year, month, day, hour, minute, second, nano), offset);
    }

    public static OffsetDateTime ofInstant(Instant instant, ZoneId zone)
    {
        ZonedDateTime zoned = ZonedDateTime.ofInstant(instant, zone);
        return of(zoned.toLocalDateTime(), zoned.getOffset());
    }

    public static OffsetDateTime parse(CharSequence text)
    {
        return parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public static OffsetDateTime parse(CharSequence text, DateTimeFormatter formatter)
    {
        if (formatter == null)
        {
            throw new NullPointerException("formatter");
        }
        java.time.temporal.TemporalAccessor parsed = formatter.parse(text);
        if (parsed instanceof OffsetDateTime)
        {
            return (OffsetDateTime)parsed;
        }
        if (parsed instanceof ZonedDateTime)
        {
            ZonedDateTime zoned = (ZonedDateTime)parsed;
            return of(zoned.toLocalDateTime(), zoned.getOffset());
        }
        throw new java.time.format.DateTimeParseException(
            "Text cannot be parsed to an OffsetDateTime", text.toString(), 0);
    }

    public LocalDateTime toLocalDateTime()
    {
        return dateTime;
    }

    public LocalDate toLocalDate()
    {
        return dateTime.toLocalDate();
    }

    public LocalTime toLocalTime()
    {
        return dateTime.toLocalTime();
    }

    public ZoneOffset getOffset()
    {
        return offset;
    }

    public Instant toInstant()
    {
        return toZonedDateTime().toInstant();
    }

    public ZonedDateTime toZonedDateTime()
    {
        return ZonedDateTime.of(dateTime, offset);
    }

    public long toEpochSecond()
    {
        return toInstant().getEpochSecond();
    }

    public int getYear()
    {
        return dateTime.getYear();
    }

    public int getMonthValue()
    {
        return dateTime.getMonthValue();
    }

    public int getDayOfMonth()
    {
        return dateTime.getDayOfMonth();
    }

    public int getHour()
    {
        return dateTime.getHour();
    }

    public int getMinute()
    {
        return dateTime.getMinute();
    }

    public int getSecond()
    {
        return dateTime.getSecond();
    }

    public int getNano()
    {
        return dateTime.getNano();
    }

    public OffsetDateTime withOffsetSameInstant(ZoneOffset newOffset)
    {
        if (newOffset.equals(offset))
        {
            return this;
        }
        return ofInstant(toInstant(), newOffset);
    }

    public OffsetDateTime plusDays(long days)
    {
        return of(dateTime.plusDays(days), offset);
    }

    public OffsetDateTime plusHours(long hours)
    {
        return of(dateTime.plusHours(hours), offset);
    }

    public int compareTo(OffsetDateTime other)
    {
        int cmp = toInstant().compareTo(other.toInstant());
        if (cmp != 0)
        {
            return cmp;
        }
        return dateTime.compareTo(other.dateTime);
    }

    public boolean isBefore(OffsetDateTime other)
    {
        return toInstant().isBefore(other.toInstant());
    }

    public boolean isAfter(OffsetDateTime other)
    {
        return toInstant().isAfter(other.toInstant());
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof OffsetDateTime))
        {
            return false;
        }
        OffsetDateTime o = (OffsetDateTime)other;
        return dateTime.equals(o.dateTime) && offset.equals(o.offset);
    }

    public int hashCode()
    {
        return dateTime.hashCode() ^ offset.hashCode();
    }

    public String toString()
    {
        return dateTime.toString() + offset.toString();
    }
}
