package java.time;

public enum DayOfWeek
{
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public int getValue()
    {
        return ordinal() + 1;
    }

    public static DayOfWeek of(int dayOfWeek)
    {
        if (dayOfWeek < 1 || dayOfWeek > 7)
        {
            throw new DateTimeException("Invalid value for DayOfWeek: " + dayOfWeek);
        }
        return values()[dayOfWeek - 1];
    }
}
