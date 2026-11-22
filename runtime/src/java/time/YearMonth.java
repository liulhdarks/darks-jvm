package java.time;

/** Minimal year-month value object. */
public final class YearMonth implements java.time.temporal.TemporalAccessor,
        Comparable<YearMonth>
{
    private final int year;
    private final int month;

    private YearMonth(int year, int month)
    {
        this.year = year;
        this.month = month;
    }

    public static YearMonth of(int year, int month)
    {
        if (month < 1 || month > 12)
        {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return new YearMonth(year, month);
    }

    public static YearMonth of(int year, Month month)
    {
        return of(year, month.getValue());
    }

    public static YearMonth now()
    {
        LocalDate today = LocalDate.now();
        return of(today.getYear(), today.getMonthValue());
    }

    public static YearMonth parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        int dash = s.indexOf('-', 1);
        if (dash < 0)
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid year-month", s, 0);
        }
        try
        {
            return of(Year.parse(s.substring(0, dash)).getValue(),
                Integer.parseInt(s.substring(dash + 1)));
        }
        catch (RuntimeException ex)
        {
            if (ex instanceof java.time.format.DateTimeParseException ||
                ex instanceof DateTimeException ||
                ex instanceof NumberFormatException)
            {
                if (ex instanceof java.time.format.DateTimeParseException)
                {
                    throw ex;
                }
                throw new java.time.format.DateTimeParseException(
                    "Invalid year-month", s, 0);
            }
            throw ex;
        }
    }

    public static YearMonth parse(CharSequence text,
            java.time.format.DateTimeFormatter formatter)
    {
        if (formatter == null)
        {
            throw new NullPointerException("formatter");
        }
        Object parsed = formatter.parse(text);
        if (parsed instanceof YearMonth)
        {
            return (YearMonth) parsed;
        }
        if (parsed instanceof LocalDate)
        {
            LocalDate date = (LocalDate) parsed;
            return of(date.getYear(), date.getMonthValue());
        }
        throw new java.time.format.DateTimeParseException(
            "Invalid year-month", text == null ? "null" : text.toString(), 0);
    }

    public int getYear()
    {
        return year;
    }

    public int getMonthValue()
    {
        return month;
    }

    public Month getMonth()
    {
        return Month.of(month);
    }

    public boolean isLeapYear()
    {
        return java.time.LocalDate.of(year, 2, 1).lengthOfMonth() == 29;
    }

    public int lengthOfMonth()
    {
        return java.time.LocalDate.of(year, month, 1).lengthOfMonth();
    }

    public int lengthOfYear()
    {
        return isLeapYear() ? 366 : 365;
    }

    public YearMonth plusMonths(long months)
    {
        long total = year * 12L + (month - 1) + months;
        long newYear = total / 12;
        long newMonth = total % 12;
        if (newMonth < 0)
        {
            newMonth = newMonth + 12;
            newYear = newYear - 1;
        }
        return of((int)newYear, (int)newMonth + 1);
    }

    public YearMonth minusMonths(long months)
    {
        return plusMonths(-months);
    }

    public YearMonth plusYears(long years)
    {
        return of((int)(year + years), month);
    }

    public YearMonth minusYears(long years)
    {
        return plusYears(-years);
    }

    public LocalDate atDay(int dayOfMonth)
    {
        return LocalDate.of(year, month, dayOfMonth);
    }

    public LocalDate atEndOfMonth()
    {
        return LocalDate.of(year, month, lengthOfMonth());
    }

    public int compareTo(YearMonth other)
    {
        if (year != other.year)
        {
            return year < other.year ? -1 : 1;
        }
        return month < other.month ? -1 : (month == other.month ? 0 : 1);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof YearMonth))
        {
            return false;
        }
        YearMonth o = (YearMonth)other;
        return year == o.year && month == o.month;
    }

    public int hashCode()
    {
        return year * 12 + (month - 1);
    }

    public String toString()
    {
        String m = Integer.toString(month);
        return year + (m.length() < 2 ? "-0" + m : "-" + m);
    }
}
