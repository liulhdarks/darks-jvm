package java.time;

public final class Year implements Comparable<Year>
{
    private final int year;

    private Year(int year)
    {
        this.year = year;
    }

    public static Year now()
    {
        return of(LocalDate.now().getYear());
    }

    public static Year of(int year)
    {
        return new Year(year);
    }

    public static Year parse(CharSequence text)
    {
        return new Year(Integer.parseInt(text.toString()));
    }

    public static boolean isLeap(long year)
    {
        return (year % 4 == 0) && (year % 100 != 0 || year % 400 == 0);
    }

    public int getValue()
    {
        return year;
    }

    public boolean isLeap()
    {
        return isLeap(year);
    }

    public int length()
    {
        return isLeap() ? 366 : 365;
    }

    public Year plusYears(long years)
    {
        return new Year((int)(year + years));
    }

    public Year minusYears(long years)
    {
        return plusYears(-years);
    }

    public LocalDate atDay(int dayOfYear)
    {
        return LocalDate.ofYearDay(year, dayOfYear);
    }

    public YearMonth atMonth(Month month)
    {
        return YearMonth.of(year, month.getValue());
    }

    public YearMonth atMonth(int month)
    {
        return YearMonth.of(year, month);
    }

    public int compareTo(Year other)
    {
        return year - other.year;
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
        return String.valueOf(year);
    }
}
