package java.time;

/**
 * A fixed offset from UTC.  Offsets are limited to +/-18:00.
 */
public class ZoneOffset extends ZoneId
{
    public static final ZoneOffset UTC = new ZoneOffset("Z", 0);
    public static final ZoneOffset MIN = new ZoneOffset("-18:00", -18 * 3600);
    public static final ZoneOffset MAX = new ZoneOffset("+18:00", 18 * 3600);

    private ZoneOffset(String id, int totalSeconds)
    {
        super(id, totalSeconds);
    }

    public static ZoneOffset of(String offsetId)
    {
        if (offsetId == null)
        {
            throw new NullPointerException("offsetId");
        }
        if (offsetId.equals("Z") || offsetId.equals("z") || offsetId.equals("UTC") ||
            offsetId.equals("GMT") || offsetId.equals("UT"))
        {
            return UTC;
        }
        if (offsetId.length() == 0)
        {
            throw new DateTimeException("Invalid offset: " + offsetId);
        }
        char signChar = offsetId.charAt(0);
        if (signChar != '+' && signChar != '-')
        {
            throw new DateTimeException("Invalid offset: " + offsetId);
        }
        int sign = signChar == '-' ? -1 : 1;
        String body = offsetId.substring(1);
        int hour;
        int minute = 0;
        int second = 0;
        int colon = body.indexOf(':');
        if (colon >= 0)
        {
            hour = Integer.parseInt(body.substring(0, colon));
            String rest = body.substring(colon + 1);
            int colon2 = rest.indexOf(':');
            if (colon2 >= 0)
            {
                minute = Integer.parseInt(rest.substring(0, colon2));
                second = Integer.parseInt(rest.substring(colon2 + 1));
            }
            else
            {
                minute = Integer.parseInt(rest);
            }
        }
        else if (body.length() <= 2)
        {
            hour = Integer.parseInt(body);
        }
        else
        {
            hour = Integer.parseInt(body.substring(0, 2));
            minute = Integer.parseInt(body.substring(2, 4));
        }
        if (hour < 0 || hour > 18 || minute < 0 || minute > 59 ||
            second < 0 || second > 59 || (hour == 18 && (minute != 0 || second != 0)))
        {
            throw new DateTimeException("Invalid offset: " + offsetId);
        }
        int total = sign * (hour * 3600 + minute * 60 + second);
        return new ZoneOffset(formatOffset(total), total);
    }

    public static ZoneOffset ofTotalSeconds(int totalSeconds)
    {
        if (totalSeconds < -18 * 3600 || totalSeconds > 18 * 3600)
        {
            throw new DateTimeException("Invalid offset seconds: " + totalSeconds);
        }
        if (totalSeconds == 0)
        {
            return UTC;
        }
        return new ZoneOffset(formatOffset(totalSeconds), totalSeconds);
    }

    public int getTotalSeconds()
    {
        return getOffsetSeconds(0L);
    }

    public static String formatOffset(int totalSeconds)
    {
        if (totalSeconds == 0)
        {
            return "Z";
        }
        StringBuilder out = new StringBuilder();
        if (totalSeconds < 0)
        {
            out.append('-');
            totalSeconds = -totalSeconds;
        }
        else
        {
            out.append('+');
        }
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        LocalTime.appendTwo(out, hours);
        out.append(':');
        LocalTime.appendTwo(out, minutes);
        if (seconds != 0)
        {
            out.append(':');
            LocalTime.appendTwo(out, seconds);
        }
        return out.toString();
    }

    public String toString()
    {
        return getId();
    }
}
