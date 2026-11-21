package java.time;

public final class YearMonth implements Comparable<YearMonth>
{
    private final int year;
    private final int month;

    private YearMonth(int year, int month)
    {
        this.year = year;
        this.month = month;
    }

    public static YearMonth now()
    {
        LocalDate date = LocalDate.now();
        return of(date.getYear(), date.getMonthValue());
    }

    public static YearMonth of(int year, Month month)
    {
        return of(year, month.getValue());
    }

    public static YearMonth of(int year, int month)
    {
        Month.of(month);
        return new YearMonth(year, month);
    }

    public static YearMonth parse(CharSequence text)
    {
        String value = text.toString();
        int dash = value.indexOf('-');
        return of(Integer.parseInt(value.substring(0, dash)),
            Integer.parseInt(value.substring(dash + 1)));
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

    public int lengthOfMonth()
    {
        return getMonth().length(Year.isLeap(year));
    }

    public int lengthOfYear()
    {
        return Year.isLeap(year) ? 366 : 365;
    }

    public LocalDate atDay(int dayOfMonth)
    {
        return LocalDate.of(year, month, dayOfMonth);
    }

    public LocalDate atEndOfMonth()
    {
        return LocalDate.of(year, month, lengthOfMonth());
    }

    public YearMonth plusMonths(long months)
    {
        long total = year * 12L + (month - 1) + months;
        int newYear = (int)Math.floorDiv(total, 12L);
        int newMonth = (int)Math.floorMod(total, 12L) + 1;
        return of(newYear, newMonth);
    }

    public int compareTo(YearMonth other)
    {
        if (year != other.year)
        {
            return year - other.year;
        }
        return month - other.month;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof YearMonth))
        {
            return false;
        }
        YearMonth value = (YearMonth)other;
        return year == value.year && month == value.month;
    }

    public int hashCode()
    {
        return year * 12 + month;
    }

    public String toString()
    {
        return (year < 0 ? "-" : "") + String.valueOf(Math.abs(year))
            + (month < 10 ? "-0" : "-") + month;
    }
}
