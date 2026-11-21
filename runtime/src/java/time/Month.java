package java.time;

public enum Month
{
    JANUARY,
    FEBRUARY,
    MARCH,
    APRIL,
    MAY,
    JUNE,
    JULY,
    AUGUST,
    SEPTEMBER,
    OCTOBER,
    NOVEMBER,
    DECEMBER;

    public int getValue()
    {
        return ordinal() + 1;
    }

    public static Month of(int month)
    {
        if (month < 1 || month > 12)
        {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return values()[month - 1];
    }

    public int length(boolean leapYear)
    {
        int value = getValue();
        if (value == 2)
        {
            return leapYear ? 29 : 28;
        }
        if (value == 4 || value == 6 || value == 9 || value == 11)
        {
            return 30;
        }
        return 31;
    }

    public int minLength()
    {
        return length(false);
    }

    public int maxLength()
    {
        return length(true);
    }
}
