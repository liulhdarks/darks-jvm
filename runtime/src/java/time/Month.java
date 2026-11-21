package java.time;

/** ISO month-of-year. */
public enum Month
{
    JANUARY(1, 31),
    FEBRUARY(2, 28),
    MARCH(3, 31),
    APRIL(4, 30),
    MAY(5, 31),
    JUNE(6, 30),
    JULY(7, 31),
    AUGUST(8, 31),
    SEPTEMBER(9, 30),
    OCTOBER(10, 31),
    NOVEMBER(11, 30),
    DECEMBER(12, 31);

    private final int value;
    private final int standardLength;

    Month(int value, int standardLength)
    {
        this.value = value;
        this.standardLength = standardLength;
    }

    public static Month of(int month)
    {
        if (month < 1 || month > 12)
        {
            throw new DateTimeException("Invalid value for MonthOfYear: " + month);
        }
        return values()[month - 1];
    }

    public int getValue()
    {
        return value;
    }

    public int length(boolean leapYear)
    {
        return this == FEBRUARY && leapYear ? 29 : standardLength;
    }

    public int minLength()
    {
        return this == FEBRUARY ? 28 : standardLength;
    }

    public int maxLength()
    {
        return this == FEBRUARY ? 29 : standardLength;
    }

    public Month plus(long months)
    {
        return values()[(int)(((value - 1) + months % 12 + 12) % 12)];
    }

    public Month minus(long months)
    {
        return plus(-months);
    }

    public String toString()
    {
        String name = name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
