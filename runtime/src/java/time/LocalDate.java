package java.time;

/** A date without a time-zone in the ISO-8601 calendar system. */
public final class LocalDate implements java.time.temporal.Temporal,
        java.time.temporal.TemporalAdjuster, Comparable<LocalDate>
{
    public static final LocalDate MIN = new LocalDate(-999999999, 1, 1);
    public static final LocalDate MAX = new LocalDate(999999999, 12, 31);
    public static final LocalDate EPOCH = new LocalDate(1970, 1, 1);

    private final int year;
    private final int month;
    private final int day;

    private LocalDate(int year, int month, int day)
    {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public static LocalDate of(int year, int month, int dayOfMonth)
    {
        if (month < 1 || month > 12)
        {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        int length = Civil.lengthOfMonth(year, month);
        if (dayOfMonth < 1 || dayOfMonth > length)
        {
            throw new DateTimeException("Invalid value for DayOfMonth: " + dayOfMonth);
        }
        if (year < -999999999 || year > 999999999)
        {
            throw new DateTimeException("Invalid value for Year: " + year);
        }
        return new LocalDate(year, month, dayOfMonth);
    }

    public static LocalDate ofYearDay(int year, int dayOfYear)
    {
        int length = Civil.isLeap(year) ? 366 : 365;
        if (dayOfYear < 1 || dayOfYear > length)
        {
            throw new DateTimeException("Invalid value for DayOfYear: " + dayOfYear);
        }
        int month = 1;
        int day = dayOfYear;
        while (day > Civil.lengthOfMonth(year, month))
        {
            day -= Civil.lengthOfMonth(year, month);
            month++;
        }
        return of(year, month, day);
    }

    public static LocalDate ofEpochDay(long epochDay)
    {
        int[] ymd = Civil.civilFromDays(epochDay);
        return new LocalDate(ymd[0], ymd[1], ymd[2]);
    }

    public static LocalDate now()
    {
        return LocalDateTime.now().toLocalDate();
    }

    public static LocalDate from(java.time.temporal.TemporalAccessor temporal)
    {
        if (temporal == null)
        {
            throw new NullPointerException("temporal");
        }
        if (temporal instanceof LocalDate)
        {
            return (LocalDate)temporal;
        }
        if (temporal instanceof LocalDateTime)
        {
            return ((LocalDateTime)temporal).toLocalDate();
        }
        if (temporal instanceof ZonedDateTime)
        {
            return ((ZonedDateTime)temporal).toLocalDate();
        }
        if (temporal instanceof OffsetDateTime)
        {
            return ((OffsetDateTime)temporal).toLocalDateTime().toLocalDate();
        }
        throw new DateTimeException("Unable to obtain LocalDate from TemporalAccessor");
    }

    public static LocalDate parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int first = s.indexOf('-');
        if (first < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid date: " + s, s, 0);
        }
        int second = s.indexOf('-', first + 1);
        if (second < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid date: " + s, s, 0);
        }
        int year = parseInt(s, 0, first, s);
        int month = parseInt(s, first + 1, second, s);
        int day = parseInt(s, second + 1, s.length(), s);
        return of(year, month, day);
    }

    private static int parseInt(String s, int from, int to, String original)
    {
        if (from >= to)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid date: " + original, original, from);
        }
        boolean negative = false;
        if (s.charAt(from) == '-')
        {
            negative = true;
            from = from + 1;
        }
        else if (s.charAt(from) == '+')
        {
            from = from + 1;
        }
        int value = 0;
        int i = from;
        while (i < to)
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid date: " + original, original, i);
            }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return negative ? -value : value;
    }

    public int getYear()
    {
        return year;
    }

    public int getMonthValue()
    {
        return month;
    }

    public int getDayOfMonth()
    {
        return day;
    }

    public int getDayOfYear()
    {
        int total = 0;
        int m = 1;
        while (m < month)
        {
            total = total + Civil.lengthOfMonth(year, m);
            m = m + 1;
        }
        return total + day;
    }

    public int lengthOfMonth()
    {
        return Civil.lengthOfMonth(year, month);
    }

    public int lengthOfYear()
    {
        return isLeapYear() ? 366 : 365;
    }

    public boolean isLeapYear()
    {
        return Civil.isLeap(year);
    }

    public static boolean isLeapYear(long year)
    {
        return Civil.isLeap(year);
    }

    public long toEpochDay()
    {
        return Civil.daysFromCivil(year, month, day);
    }

    public LocalDate plusDays(long daysToAdd)
    {
        if (daysToAdd == 0)
        {
            return this;
        }
        return ofEpochDay(toEpochDay() + daysToAdd);
    }

    public LocalDate minusDays(long daysToSubtract)
    {
        return plusDays(-daysToSubtract);
    }

    public LocalDate plusWeeks(long weeksToAdd)
    {
        return plusDays(weeksToAdd * 7L);
    }

    public LocalDate minusWeeks(long weeksToSubtract)
    {
        return plusDays(-(weeksToSubtract * 7L));
    }

    public LocalDate plusMonths(long monthsToAdd)
    {
        if (monthsToAdd == 0)
        {
            return this;
        }
        long monthCount = (long)(year - 1) * 12L + (long)(month - 1) + monthsToAdd;
        long newYear = Math.floorDiv(monthCount, 12L) + 1L;
        int newMonth = (int)Math.floorMod(monthCount, 12L) + 1;
        int newDay = day;
        int length = Civil.lengthOfMonth((int)newYear, newMonth);
        if (newDay > length)
        {
            newDay = length;
        }
        return of((int)newYear, newMonth, newDay);
    }

    public LocalDate minusMonths(long monthsToSubtract)
    {
        return plusMonths(-monthsToSubtract);
    }

    public LocalDate plusYears(long yearsToAdd)
    {
        if (yearsToAdd == 0)
        {
            return this;
        }
        int newYear = (int)((long)year + yearsToAdd);
        int newDay = day;
        int length = Civil.lengthOfMonth(newYear, month);
        if (newDay > length)
        {
            newDay = length;
        }
        return of(newYear, month, newDay);
    }

    public LocalDate minusYears(long yearsToSubtract)
    {
        return plusYears(-yearsToSubtract);
    }

    public LocalDate withYear(int newYear)
    {
        return of(newYear, month, day);
    }

    public LocalDate withMonth(int newMonth)
    {
        int newDay = day;
        int length = Civil.lengthOfMonth(year, newMonth);
        if (newDay > length)
        {
            newDay = length;
        }
        return of(year, newMonth, newDay);
    }

    public LocalDate withDayOfMonth(int newDay)
    {
        return of(year, month, newDay);
    }

    public LocalDateTime atTime(LocalTime time)
    {
        return LocalDateTime.of(this, time);
    }

    public LocalDateTime atTime(int hour, int minute)
    {
        return LocalDateTime.of(this, LocalTime.of(hour, minute));
    }

    public LocalDateTime atTime(int hour, int minute, int second)
    {
        return LocalDateTime.of(this, LocalTime.of(hour, minute, second));
    }

    public LocalDateTime atStartOfDay()
    {
        return LocalDateTime.of(this, LocalTime.MIDNIGHT);
    }

    public int compareTo(LocalDate other)
    {
        if (year != other.year)
        {
            return year < other.year ? -1 : 1;
        }
        if (month != other.month)
        {
            return month < other.month ? -1 : 1;
        }
        if (day == other.day)
        {
            return 0;
        }
        return day < other.day ? -1 : 1;
    }

    public boolean isBefore(LocalDate other)
    {
        return compareTo(other) < 0;
    }

    public boolean isAfter(LocalDate other)
    {
        return compareTo(other) > 0;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof LocalDate))
        {
            return false;
        }
        LocalDate o = (LocalDate)other;
        return year == o.year && month == o.month && day == o.day;
    }

    public int hashCode()
    {
        return year * 31 * 31 + month * 31 + day;
    }

    public String format(java.time.format.DateTimeFormatter formatter)
    {
        return formatter.format(this);
    }

    public String toString()
    {
        StringBuilder out = new StringBuilder();
        appendYear(out, year);
        out.append('-');
        LocalTime.appendTwo(out, month);
        out.append('-');
        LocalTime.appendTwo(out, day);
        return out.toString();
    }

    static void appendYear(StringBuilder out, int value)
    {
        if (value < 0)
        {
            out.append('-');
            value = -value;
        }
        if (value < 1000)
        {
            out.append('0');
        }
        if (value < 100)
        {
            out.append('0');
        }
        if (value < 10)
        {
            out.append('0');
        }
        out.append(Integer.toString(value));
    }
}
