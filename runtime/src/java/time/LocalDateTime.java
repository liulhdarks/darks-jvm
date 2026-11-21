package java.time;

/** A date-time without a time-zone in the ISO-8601 calendar system. */
public final class LocalDateTime implements java.time.temporal.Temporal,
        java.time.temporal.TemporalAdjuster, Comparable<LocalDateTime>
{
    public static final LocalDateTime MIN = LocalDateTime.of(LocalDate.MIN, LocalTime.MIN);
    public static final LocalDateTime MAX = LocalDateTime.of(LocalDate.MAX, LocalTime.MAX);

    private final LocalDate date;
    private final LocalTime time;

    private LocalDateTime(LocalDate date, LocalTime time)
    {
        this.date = date;
        this.time = time;
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth,
                                   int hour, int minute)
    {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth),
                                 LocalTime.of(hour, minute));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth,
                                   int hour, int minute, int second)
    {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth),
                                 LocalTime.of(hour, minute, second));
    }

    public static LocalDateTime of(int year, int month, int dayOfMonth,
                                   int hour, int minute, int second, int nanoOfSecond)
    {
        return new LocalDateTime(LocalDate.of(year, month, dayOfMonth),
                                 LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public static LocalDateTime of(LocalDate date, LocalTime time)
    {
        if (date == null || time == null)
        {
            throw new NullPointerException();
        }
        return new LocalDateTime(date, time);
    }

    public static LocalDateTime ofInstant(Instant instant, ZoneId zone)
    {
        return ofEpochSecond(instant.getEpochSecond(), instant.getNano(), zone);
    }

    public static LocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond, ZoneId zone)
    {
        int offset = zone.getOffsetSeconds(epochSecond * 1000L);
        long localSecond = epochSecond + offset;
        long days = Math.floorDiv(localSecond, 86400L);
        int secondOfDay = (int)Math.floorMod(localSecond, 86400L);
        int hour = secondOfDay / 3600;
        int minute = (secondOfDay % 3600) / 60;
        int second = secondOfDay % 60;
        return new LocalDateTime(LocalDate.ofEpochDay(days),
                                 LocalTime.of(hour, minute, second, nanoOfSecond));
    }

    public static LocalDateTime ofEpochSecond(long epochSecond, int nanoOfSecond, ZoneOffset offset)
    {
        long localSecond = epochSecond + offset.getTotalSeconds();
        long days = Math.floorDiv(localSecond, 86400L);
        int secondOfDay = (int)Math.floorMod(localSecond, 86400L);
        return new LocalDateTime(LocalDate.ofEpochDay(days),
                                 LocalTime.of(secondOfDay / 3600,
                                              (secondOfDay % 3600) / 60,
                                              secondOfDay % 60, nanoOfSecond));
    }

    public static LocalDateTime now()
    {
        Instant now = Instant.now();
        return ofEpochSecond(now.getEpochSecond(), now.getNano(), ZoneId.systemDefault());
    }

    public static LocalDateTime parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int t = s.indexOf('T');
        if (t < 0)
        {
            return LocalDate.parse(s).atStartOfDay();
        }
        return new LocalDateTime(LocalDate.parse(s.substring(0, t)),
                                 LocalTime.parse(s.substring(t + 1)));
    }

    public LocalDate toLocalDate()
    {
        return date;
    }

    public LocalTime toLocalTime()
    {
        return time;
    }

    public int getYear()
    {
        return date.getYear();
    }

    public int getMonthValue()
    {
        return date.getMonthValue();
    }

    public int getDayOfMonth()
    {
        return date.getDayOfMonth();
    }

    public int getHour()
    {
        return time.getHour();
    }

    public int getMinute()
    {
        return time.getMinute();
    }

    public int getSecond()
    {
        return time.getSecond();
    }

    public int getNano()
    {
        return time.getNano();
    }

    public long toEpochSecond(ZoneOffset offset)
    {
        return date.toEpochDay() * 86400L + time.toSecondOfDay() - offset.getTotalSeconds();
    }

    public LocalDateTime plusDays(long days)
    {
        return new LocalDateTime(date.plusDays(days), time);
    }

    public LocalDateTime plusWeeks(long weeks)
    {
        return plusDays(weeks * 7L);
    }

    public LocalDateTime plusMonths(long months)
    {
        return new LocalDateTime(date.plusMonths(months), time);
    }

    public LocalDateTime plusYears(long years)
    {
        return new LocalDateTime(date.plusYears(years), time);
    }

    public LocalDateTime plusHours(long hours)
    {
        return plusWithOverflow(hours * 3600L * 1000000000L);
    }

    public LocalDateTime plusMinutes(long minutes)
    {
        return plusWithOverflow(minutes * 60L * 1000000000L);
    }

    public LocalDateTime plusSeconds(long seconds)
    {
        return plusWithOverflow(seconds * 1000000000L);
    }

    public LocalDateTime plusNanos(long nanos)
    {
        return plusWithOverflow(nanos);
    }

    private LocalDateTime plusWithOverflow(long nanos)
    {
        long nod = time.toNanoOfDay() + nanos;
        long dayShift = Math.floorDiv(nod, 86400000000000L);
        long newNod = Math.floorMod(nod, 86400000000000L);
        return new LocalDateTime(date.plusDays(dayShift), LocalTime.ofNanoOfDay(newNod));
    }

    public LocalDateTime minusDays(long days)
    {
        return plusDays(-days);
    }

    public LocalDateTime minusMonths(long months)
    {
        return plusMonths(-months);
    }

    public LocalDateTime minusYears(long years)
    {
        return plusYears(-years);
    }

    public LocalDateTime minusHours(long hours)
    {
        return plusHours(-hours);
    }

    public LocalDateTime minusMinutes(long minutes)
    {
        return plusMinutes(-minutes);
    }

    public LocalDateTime minusSeconds(long seconds)
    {
        return plusSeconds(-seconds);
    }

    public LocalDateTime minusNanos(long nanos)
    {
        return plusNanos(-nanos);
    }

    public ZonedDateTime atZone(ZoneId zone)
    {
        return ZonedDateTime.of(this, zone);
    }

    public int compareTo(LocalDateTime other)
    {
        int cmp = date.compareTo(other.date);
        if (cmp != 0)
        {
            return cmp;
        }
        return time.compareTo(other.time);
    }

    public boolean isBefore(LocalDateTime other)
    {
        return compareTo(other) < 0;
    }

    public boolean isAfter(LocalDateTime other)
    {
        return compareTo(other) > 0;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof LocalDateTime))
        {
            return false;
        }
        LocalDateTime o = (LocalDateTime)other;
        return date.equals(o.date) && time.equals(o.time);
    }

    public int hashCode()
    {
        return date.hashCode() * 31 + time.hashCode();
    }

    public String format(java.time.format.DateTimeFormatter formatter)
    {
        return formatter.format(this);
    }

    public String toString()
    {
        return date.toString() + "T" + time.toString();
    }
}
