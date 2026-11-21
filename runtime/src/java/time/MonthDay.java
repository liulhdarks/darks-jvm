package java.time;

/** Minimal month-day value object (no year). */
public final class MonthDay implements Comparable<MonthDay>
{
    private final int month;
    private final int day;

    private MonthDay(int month, int day)
    {
        this.month = month;
        this.day = day;
    }

    public static MonthDay of(int month, int day)
    {
        if (month < 1 || month > 12)
        {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        LocalDate check = LocalDate.of(2000, month, day);
        if (check.getDayOfMonth() != day)
        {
            throw new DateTimeException("Invalid date");
        }
        return new MonthDay(month, day);
    }

    public static MonthDay of(Month month, int day)
    {
        return of(month.getValue(), day);
    }

    public static MonthDay now()
    {
        LocalDate today = LocalDate.now();
        return of(today.getMonthValue(), today.getDayOfMonth());
    }

    public static MonthDay parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        if (s.length() < 5 || s.charAt(0) != '-' || s.charAt(3) != '-')
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid month-day", s, 0);
        }
        try
        {
            return of(Integer.parseInt(s.substring(1, 3)),
                Integer.parseInt(s.substring(4)));
        }
        catch (RuntimeException ex)
        {
            if (ex instanceof java.time.format.DateTimeParseException ||
                ex instanceof DateTimeException || ex instanceof NumberFormatException)
            {
                if (ex instanceof java.time.format.DateTimeParseException)
                {
                    throw ex;
                }
                throw new java.time.format.DateTimeParseException(
                    "Invalid month-day", s, 0);
            }
            throw ex;
        }
    }

    public int getMonthValue()
    {
        return month;
    }

    public Month getMonth()
    {
        return Month.of(month);
    }

    public int getDayOfMonth()
    {
        return day;
    }

    public boolean isValidYear(int year)
    {
        return !(month == 2 && day == 29) || LocalDate.of(year, 2, 1).lengthOfMonth() == 29;
    }

    public MonthDay withMonth(int newMonth)
    {
        return of(newMonth, day);
    }

    public MonthDay withDayOfMonth(int newDay)
    {
        return of(month, newDay);
    }

    public LocalDate atYear(int year)
    {
        return LocalDate.of(year, month, day);
    }

    public int compareTo(MonthDay other)
    {
        if (month != other.month)
        {
            return month < other.month ? -1 : 1;
        }
        return day < other.day ? -1 : (day == other.day ? 0 : 1);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof MonthDay))
        {
            return false;
        }
        MonthDay o = (MonthDay)other;
        return month == o.month && day == o.day;
    }

    public int hashCode()
    {
        return month * 32 + day;
    }

    public String toString()
    {
        String m = Integer.toString(month);
        String d = Integer.toString(day);
        return "--" + (m.length() < 2 ? "0" + m : m) + "-" +
            (d.length() < 2 ? "0" + d : d);
    }
}
