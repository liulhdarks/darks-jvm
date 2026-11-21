package java.time;

/** Minimal ISO year value object. */
public final class Year implements Comparable<Year>
{
    public static final int MIN_VALUE = -999999999;
    public static final int MAX_VALUE = 999999999;

    private final int year;

    private Year(int year)
    {
        this.year = year;
    }

    public static Year of(int year)
    {
        if (year < MIN_VALUE || year > MAX_VALUE)
        {
            throw new java.time.DateTimeException("Invalid year: " + year);
        }
        return new Year(year);
    }

    public static Year now()
    {
        return of(LocalDate.now().getYear());
    }

    public static Year parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        if (s.length() == 0)
        {
            throw new java.time.format.DateTimeParseException("Empty year", s, 0);
        }
        int i = 0;
        boolean negative = false;
        if (s.charAt(0) == '-')
        {
            negative = true;
            i = 1;
        }
        else if (s.charAt(0) == '+')
        {
            i = 1;
        }
        if (i >= s.length() || s.length() - i < 4)
        {
            throw new java.time.format.DateTimeParseException("Invalid year", s, i);
        }
        int value = 0;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (c < '0' || c > '9')
            {
                throw new java.time.format.DateTimeParseException("Invalid year", s, i);
            }
            value = value * 10 + (c - '0');
            i = i + 1;
        }
        return of(negative ? -value : value);
    }

    public int getValue()
    {
        return year;
    }

    public boolean isLeap()
    {
        return LocalDate.of(year, 2, 1).lengthOfMonth() == 29;
    }

    public int length()
    {
        return isLeap() ? 366 : 365;
    }

    public Year plusYears(long years)
    {
        return of((int)(year + years));
    }

    public Year minusYears(long years)
    {
        return plusYears(-years);
    }

    public int compareTo(Year other)
    {
        return year < other.year ? -1 : (year == other.year ? 0 : 1);
    }

    public boolean equals(Object other)
    {
        return other instanceof Year && ((Year)other).year == year;
    }

    public int hashCode()
    {
        return year;
    }

    public String toString()
    {
        return Integer.toString(year);
    }
}
