package java.time;

/** A date-time with a time-zone. */
public final class ZonedDateTime implements java.time.temporal.Temporal,
        Comparable<ZonedDateTime>
{
    private final LocalDateTime dateTime;
    private final ZoneId zone;

    private ZonedDateTime(LocalDateTime dateTime, ZoneId zone)
    {
        this.dateTime = dateTime;
        this.zone = zone;
    }

    public static ZonedDateTime now()
    {
        return ofInstant(Instant.now(), ZoneId.systemDefault());
    }

    public static ZonedDateTime of(LocalDateTime dateTime, ZoneId zone)
    {
        if (dateTime == null || zone == null)
        {
            throw new NullPointerException();
        }
        return new ZonedDateTime(dateTime, zone);
    }

    public static ZonedDateTime of(int year, int month, int day, int hour,
                                   int minute, int second, int nano, ZoneId zone)
    {
        return of(LocalDateTime.of(year, month, day, hour, minute, second, nano), zone);
    }

    public static ZonedDateTime ofInstant(Instant instant, ZoneId zone)
    {
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, zone);
        return new ZonedDateTime(ldt, zone);
    }

    public static ZonedDateTime ofInstant(LocalDateTime dateTime, ZoneOffset offset,
                                          ZoneId zone)
    {
        return of(dateTime, zone);
    }

    public static ZonedDateTime ofStrict(LocalDateTime dateTime, ZoneOffset offset,
                                         ZoneId zone)
    {
        return of(dateTime, zone);
    }

    public static ZonedDateTime parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        String zonePart = null;
        int bracket = s.indexOf('[');
        if (bracket >= 0)
        {
            int close = s.indexOf(']', bracket + 1);
            if (close < 0)
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid zoned date-time: " + s, s, bracket);
            }
            zonePart = s.substring(bracket + 1, close);
            s = s.substring(0, bracket);
        }
        int t = s.indexOf('T');
        if (t < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid zoned date-time: " + s, s, 0);
        }
        int offsetPos = -1;
        if (s.charAt(s.length() - 1) == 'Z' || s.charAt(s.length() - 1) == 'z')
        {
            offsetPos = s.length() - 1;
        }
        else
        {
            int plus = s.lastIndexOf('+');
            int minus = s.lastIndexOf('-');
            int candidate = plus > minus ? plus : minus;
            if (candidate > t)
            {
                offsetPos = candidate;
            }
        }
        LocalDateTime ldt;
        ZoneOffset offset = null;
        if (offsetPos >= 0)
        {
            String offsetText = s.substring(offsetPos);
            if (offsetText.equals("Z") || offsetText.equals("z"))
            {
                offset = ZoneOffset.UTC;
            }
            else
            {
                offset = ZoneOffset.of(offsetText);
            }
            ldt = LocalDateTime.parse(s.substring(0, offsetPos));
        }
        else
        {
            ldt = LocalDateTime.parse(s);
        }
        if (zonePart != null)
        {
            ZoneId zone = ZoneId.of(zonePart);
            if (offset != null &&
                zone.getOffsetSeconds(ldt.toEpochSecond(offset) * 1000L) != offset.getTotalSeconds())
            {
                return ofInstant(Instant.ofEpochSecond(ldt.toEpochSecond(offset),
                                                       ldt.getNano()), zone);
            }
            return of(ldt, zone);
        }
        if (offset != null)
        {
            return of(ldt, offset);
        }
        return of(ldt, ZoneId.systemDefault());
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

    public ZoneId getZone()
    {
        return zone;
    }

    public ZoneOffset getOffset()
    {
        return ZoneOffset.ofTotalSeconds(zone.getOffsetSeconds(toInstant().toEpochMilli()));
    }

    public Instant toInstant()
    {
        long seconds = dateTime.toEpochSecond(ZoneOffset.UTC) -
                       zone.getOffsetSeconds(dateTime.toEpochSecond(ZoneOffset.UTC) * 1000L);
        return Instant.ofEpochSecond(seconds, dateTime.getNano());
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

    public ZonedDateTime withZoneSameInstant(ZoneId newZone)
    {
        return ofInstant(toInstant(), newZone);
    }

    public ZonedDateTime withZoneSameLocal(ZoneId newZone)
    {
        return of(dateTime, newZone);
    }

    public ZonedDateTime plusDays(long days)
    {
        return of(dateTime.plusDays(days), zone);
    }

    public ZonedDateTime plusMonths(long months)
    {
        return of(dateTime.plusMonths(months), zone);
    }

    public ZonedDateTime plusYears(long years)
    {
        return of(dateTime.plusYears(years), zone);
    }

    public ZonedDateTime plusHours(long hours)
    {
        return of(dateTime.plusHours(hours), zone);
    }

    public ZonedDateTime plusMinutes(long minutes)
    {
        return of(dateTime.plusMinutes(minutes), zone);
    }

    public ZonedDateTime plusSeconds(long seconds)
    {
        return of(dateTime.plusSeconds(seconds), zone);
    }

    public ZonedDateTime plusNanos(long nanos)
    {
        return of(dateTime.plusNanos(nanos), zone);
    }

    public ZonedDateTime minusDays(long days)
    {
        return plusDays(-days);
    }

    public ZonedDateTime minusMonths(long months)
    {
        return plusMonths(-months);
    }

    public ZonedDateTime minusYears(long years)
    {
        return plusYears(-years);
    }

    public ZonedDateTime minusHours(long hours)
    {
        return plusHours(-hours);
    }

    public ZonedDateTime minusMinutes(long minutes)
    {
        return plusMinutes(-minutes);
    }

    public ZonedDateTime minusSeconds(long seconds)
    {
        return plusSeconds(-seconds);
    }

    public int compareTo(ZonedDateTime other)
    {
        int cmp = toInstant().compareTo(other.toInstant());
        if (cmp != 0)
        {
            return cmp;
        }
        return dateTime.compareTo(other.dateTime);
    }

    public boolean isBefore(ZonedDateTime other)
    {
        return toInstant().isBefore(other.toInstant());
    }

    public boolean isAfter(ZonedDateTime other)
    {
        return toInstant().isAfter(other.toInstant());
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ZonedDateTime))
        {
            return false;
        }
        ZonedDateTime o = (ZonedDateTime)other;
        return dateTime.equals(o.dateTime) && zone.equals(o.zone);
    }

    public int hashCode()
    {
        return dateTime.hashCode() * 31 + zone.hashCode();
    }

    public String format(java.time.format.DateTimeFormatter formatter)
    {
        return formatter.format(this);
    }

    public String toString()
    {
        String local = dateTime.toString();
        if (zone instanceof ZoneOffset)
        {
            return local + zone.getId();
        }
        int offset = zone.getOffsetSeconds(toInstant().toEpochMilli());
        return local + ZoneOffset.formatOffset(offset) + "[" + zone.getId() + "]";
    }
}
