package java.time;

/**
 * A time-zone identifier.  Fixed offsets ("Z", "+08:00") and a small table of
 * IANA region ids are supported.  Region zones use their standard offset all
 * year: daylight saving transitions are not modelled.
 */
public class ZoneId implements Comparable<ZoneId>
{
    private static final String[] REGION_IDS = {
        "UTC", "GMT", "UT",
        "Asia/Shanghai", "Asia/Tokyo", "Asia/Seoul", "Asia/Singapore",
        "Asia/Hong_Kong", "Asia/Kolkata", "Asia/Dubai", "Asia/Bangkok",
        "Asia/Taipei", "Asia/Jerusalem",
        "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Rome",
        "Europe/Madrid", "Europe/Amsterdam", "Europe/Moscow",
        "Europe/Istanbul",
        "America/New_York", "America/Chicago", "America/Denver",
        "America/Los_Angeles", "America/Toronto", "America/Sao_Paulo",
        "America/Mexico_City",
        "Australia/Sydney", "Australia/Melbourne", "Pacific/Auckland",
        "Africa/Cairo", "Africa/Johannesburg"
    };

    private static final int[] REGION_OFFSETS = {
        0, 0, 0,
        8 * 3600, 9 * 3600, 9 * 3600, 8 * 3600,
        8 * 3600, 19800, 4 * 3600, 7 * 3600,
        8 * 3600, 2 * 3600,
        0, 1 * 3600, 1 * 3600, 1 * 3600,
        1 * 3600, 1 * 3600, 3 * 3600,
        3 * 3600,
        -5 * 3600, -6 * 3600, -7 * 3600,
        -8 * 3600, -5 * 3600, -3 * 3600,
        -6 * 3600,
        10 * 3600, 10 * 3600, 12 * 3600,
        2 * 3600, 2 * 3600
    };

    private final String id;
    private final int offsetSeconds;

    protected ZoneId(String id, int offsetSeconds)
    {
        this.id = id;
        this.offsetSeconds = offsetSeconds;
    }

    public static ZoneId systemDefault()
    {
        String tz = System.getenv("TZ");
        if (tz != null && tz.length() > 0)
        {
            try
            {
                return of(tz);
            }
            catch (RuntimeException ignored)
            {
            }
        }
        return ZoneOffset.UTC;
    }

    public static ZoneId of(String zoneId)
    {
        if (zoneId == null)
        {
            throw new NullPointerException("zoneId");
        }
        if (zoneId.equals("Z") || zoneId.equals("z"))
        {
            return ZoneOffset.UTC;
        }
        if (zoneId.length() > 0 &&
            (zoneId.charAt(0) == '+' || zoneId.charAt(0) == '-'))
        {
            return ZoneOffset.of(zoneId);
        }
        int i = 0;
        while (i < REGION_IDS.length)
        {
            if (REGION_IDS[i].equals(zoneId))
            {
                return new ZoneId(zoneId, REGION_OFFSETS[i]);
            }
            i = i + 1;
        }
        if (zoneId.startsWith("UTC+") || zoneId.startsWith("UTC-") ||
            zoneId.startsWith("GMT+") || zoneId.startsWith("GMT-"))
        {
            return ZoneOffset.of(zoneId.substring(3));
        }
        throw new DateTimeException("Unknown time-zone ID: " + zoneId);
    }

    public static ZoneId ofOffset(String prefix, ZoneOffset offset)
    {
        if (prefix == null || prefix.length() == 0)
        {
            return offset;
        }
        return new ZoneId(prefix + offset.getId(), offset.getTotalSeconds());
    }

    public String getId()
    {
        return id;
    }

    public int getOffsetSeconds(long epochMilli)
    {
        // Offsets are fixed in this runtime.  The arithmetic keeps the method
        // out of the VM's trivial-getter inliner, which does not account for
        // arguments on the caller's operand stack.
        return offsetSeconds + (int)(epochMilli - epochMilli);
    }

    public ZoneId normalized()
    {
        if (this instanceof ZoneOffset)
        {
            return this;
        }
        return ZoneOffset.ofTotalSeconds(offsetSeconds);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof ZoneId))
        {
            return false;
        }
        return id.equals(((ZoneId)other).id);
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public int compareTo(ZoneId other)
    {
        return id.compareTo(other.id);
    }

    public String toString()
    {
        return id;
    }
}
