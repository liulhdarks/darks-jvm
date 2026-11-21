package java.time;

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

    public static Period ofWeeks(int weeks)
    {
        return new Period(0, 0, weeks * 7);
    }

    public static Period parse(CharSequence text)
    {
        String value = text.toString();
        if ("P".equals(value) || "PT0S".equals(value))
        {
            return ZERO;
        }
        int years = 0;
        int months = 0;
        int days = 0;
        int index = 0;
        if (index < value.length() && value.charAt(index) == 'P')
        {
            index++;
        }
        while (index < value.length())
        {
            int start = index;
            while (index < value.length() && value.charAt(index) >= '0' && value.charAt(index) <= '9')
            {
                index++;
            }
            int number = Integer.parseInt(value.substring(start, index));
            if (index < value.length())
            {
                char unit = value.charAt(index++);
                if (unit == 'Y')
                {
                    years = number;
                }
                else if (unit == 'M')
                {
                    months = number;
                }
                else if (unit == 'W')
                {
                    days = number * 7;
                }
                else if (unit == 'D')
                {
                    days = number;
                }
                else if (unit == 'T')
                {
                    break;
                }
            }
        }
        return new Period(years, months, days);
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

    public Period plusYears(long amount)
    {
        return new Period((int)(years + amount), months, days);
    }

    public Period plusMonths(long amount)
    {
        return new Period(years, (int)(months + amount), days);
    }

    public Period plusDays(long amount)
    {
        return new Period(years, months, (int)(days + amount));
    }

    public Period normalized()
    {
        return this;
    }

    public boolean isZero()
    {
        return years == 0 && months == 0 && days == 0;
    }

    public boolean isNegative()
    {
        return years < 0 || months < 0 || days < 0;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof Period))
        {
            return false;
        }
        Period value = (Period)other;
        return years == value.years && months == value.months && days == value.days;
    }

    public int hashCode()
    {
        return years * 31 * 31 + months * 31 + days;
    }

    public String toString()
    {
        if (isZero())
        {
            return "P0D";
        }
        StringBuilder sb = new StringBuilder("P");
        if (years != 0)
        {
            sb.append(years).append('Y');
        }
        if (months != 0)
        {
            sb.append(months).append('M');
        }
        if (days != 0)
        {
            sb.append(days).append('D');
        }
        return sb.toString();
    }
}
