package java.time.temporal;

public enum ChronoField implements TemporalField
{
    NANO_OF_SECOND(0, 999999999),
    NANO_OF_DAY(0, 86399999999999L),
    MICRO_OF_SECOND(0, 999999),
    MICRO_OF_DAY(0, 86399999999L),
    MILLI_OF_SECOND(0, 999),
    MILLI_OF_DAY(0, 86399999),
    SECOND_OF_MINUTE(0, 59),
    SECOND_OF_DAY(0, 86399),
    MINUTE_OF_HOUR(0, 59),
    MINUTE_OF_DAY(0, 1439),
    HOUR_OF_AMPM(0, 11),
    CLOCK_HOUR_OF_AMPM(1, 12),
    HOUR_OF_DAY(0, 23),
    CLOCK_HOUR_OF_DAY(1, 24),
    AMPM_OF_DAY(0, 1),
    DAY_OF_WEEK(1, 7),
    ALIGNED_DAY_OF_WEEK_IN_MONTH(1, 7),
    ALIGNED_DAY_OF_WEEK_IN_YEAR(1, 7),
    DAY_OF_MONTH(1, 31),
    DAY_OF_YEAR(1, 366),
    EPOCH_DAY(-365243219162L, 365241780471L),
    ALIGNED_WEEK_OF_MONTH(1, 5),
    ALIGNED_WEEK_OF_YEAR(1, 53),
    MONTH_OF_YEAR(1, 12),
    PROLEPTIC_MONTH(-11999999988L, 11999999999L),
    YEAR_OF_ERA(1, 999999999),
    YEAR(-999999999, 999999999),
    ERA(0, 1),
    INSTANT_SECONDS(Long.MIN_VALUE, Long.MAX_VALUE),
    OFFSET_SECONDS(-64800, 64800);

    private final long minimum;
    private final long maximum;

    ChronoField(long minimum, long maximum)
    {
        this.minimum = minimum;
        this.maximum = maximum;
    }

    public ValueRange range()
    {
        return ValueRange.of(minimum, maximum);
    }

    public boolean isDateBased()
    {
        return ordinal() >= DAY_OF_WEEK.ordinal() && ordinal() <= ERA.ordinal();
    }

    public boolean isTimeBased()
    {
        return ordinal() < DAY_OF_WEEK.ordinal();
    }

    public String toString()
    {
        return name();
    }
}
