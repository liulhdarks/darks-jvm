package java.time;

/**
 * Minimal ISO-8601 period (years, months, days).
 */
public final class Period
{
    public static final Period ZERO = new Period(0, 0, 0);

    private final int years;
    private final int months;
    private final int days;

    private Period(int years, int months, int days)
    {
        this.years = years;
        this.months = months;
        this.days = days;
    }

    public static Period of(int years, int months, int days)
    {
        return new Period(years, months, days);
    }

    public static Period ofYears(int years)
    {
        return new Period(years, 0, 0);
    }

    public static Period ofMonths(int months)
    {
        return new Period(0, months, 0);
    }

    public static Period ofDays(int days)
    {
        return new Period(0, 0, days);
    }

    public static Period parse(CharSequence text)
    {
        if (text == null)
        {
            throw new NullPointerException("text");
        }
        String s = text.toString();
        if (s.length() == 0 || (s.charAt(0) != 'P' && s.charAt(0) != '-' &&
                                !(s.length() > 1 && s.charAt(1) == 'P')))
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid period", s, 0);
        }
        int i = 0;
        int sign = 1;
        if (s.charAt(0) == '-')
        {
            sign = -1;
            i = 1;
        }
        if (i >= s.length() || s.charAt(i) != 'P')
        {
            throw new java.time.format.DateTimeParseException(
                "Invalid period", s, i);
        }
        i = i + 1;
        int years = 0;
        int months = 0;
        int days = 0;
        int negative = 1;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (c == '-')
            {
                negative = -1;
                i = i + 1;
                continue;
            }
            if (c == '+')
            {
                negative = 1;
                i = i + 1;
                continue;
            }
            int start = i;
            while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9')
            {
                i = i + 1;
            }
            if (start == i || i >= s.length())
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid period", s, start);
            }
            int value = Integer.parseInt(s.substring(start, i)) * negative;
            negative = 1;
            char unit = s.charAt(i);
            i = i + 1;
            if (unit == 'Y')
            {
                years = value;
            }
            else if (unit == 'M')
            {
                months = value;
            }
            else if (unit == 'W')
            {
                days = value * 7;
            }
            else if (unit == 'D')
            {
                days = value;
            }
            else
            {
                throw new java.time.format.DateTimeParseException(
                    "Invalid period unit", s, i - 1);
            }
        }
        return new Period(sign * years, sign * months, sign * days);
    }

    public int getYears()
    {
        return years;
    }

    public int getMonths()
    {
        return months;
    }

    public int getDays()
    {
        return days;
    }

    public boolean isZero()
    {
        return years == 0 && months == 0 && days == 0;
    }

    public boolean isNegative()
    {
        return years < 0 || months < 0 || days < 0;
    }

    public Period plus(Period other)
    {
        return new Period(years + other.years, months + other.months,
            days + other.days);
    }

    public Period minus(Period other)
    {
        return new Period(years - other.years, months - other.months,
            days - other.days);
    }

    public Period normalized()
    {
        long totalMonths = years * 12L + months;
        long newYears = totalMonths / 12;
        long newMonths = totalMonths % 12;
        if (newYears == years && newMonths == months)
        {
            return this;
        }
        if (newYears > Integer.MAX_VALUE || newYears < Integer.MIN_VALUE ||
            newMonths > Integer.MAX_VALUE || newMonths < Integer.MIN_VALUE)
        {
            throw new ArithmeticException("integer overflow");
        }
        return new Period((int)newYears, (int)newMonths, days);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Period))
        {
            return false;
        }
        Period o = (Period)other;
        return years == o.years && months == o.months && days == o.days;
    }

    public int hashCode()
    {
        return (years * 31 + months) * 31 + days;
    }

    public String toString()
    {
        if (isZero())
        {
            return "P0D";
        }
        StringBuilder out = new StringBuilder("P");
        if (years != 0)
        {
            out.append(years).append('Y');
        }
        if (months != 0)
        {
            out.append(months).append('M');
        }
        if (days != 0)
        {
            out.append(days).append('D');
        }
        return out.toString();
    }
}
