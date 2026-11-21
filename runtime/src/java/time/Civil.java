package java.time;

/**
 * Proleptic Gregorian calendar helpers shared by the java.time classes.
 * Days are counted from 1970-01-01 (the Unix epoch).
 */
public final class Civil
{
    private Civil()
    {
    }

    public static boolean isLeap(long year)
    {
        return (year % 4 == 0) && ((year % 100 != 0) || (year % 400 == 0));
    }

    public static int lengthOfMonth(int year, int month)
    {
        switch (month)
        {
        case 2:
            return isLeap(year) ? 29 : 28;
        case 4:
        case 6:
        case 9:
        case 11:
            return 30;
        default:
            return 31;
        }
    }

    public static long daysFromCivil(int year, int month, int day)
    {
        long y = year;
        y = y - (month <= 2 ? 1 : 0);
        long era = (y >= 0 ? y : y - 399) / 400;
        long yoe = y - era * 400;
        long doy = (153 * (month + (month > 2 ? -3 : 9)) + 2) / 5 + day - 1;
        long doe = yoe * 365 + yoe / 4 - yoe / 100 + doy;
        return era * 146097 + doe - 719468;
    }

    /** Returns {year, month, day} for the given epoch day. */
    public static int[] civilFromDays(long epochDay)
    {
        long z = epochDay + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp + (mp < 10 ? 3 : -9);
        y = y + (m <= 2 ? 1 : 0);
        return new int[] {(int)y, (int)m, (int)d};
    }

    /** ISO-8601 day-of-week, Monday = 1 ... Sunday = 7. */
    public static int dayOfWeek(long epochDay)
    {
        // 1970-01-01 was a Thursday (4).
        int value = (int)Math.floorMod(epochDay + 3, 7L) + 1;
        return value;
    }
}
