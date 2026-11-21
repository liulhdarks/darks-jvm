package java.time;

public final class MonthDay
{
    private final int month;
    private final int day;

    private MonthDay(int month, int day)
    {
        this.month = month;
        this.day = day;
    }

    public static MonthDay now()
    {
        LocalDate date = LocalDate.now();
        return of(date.getMonthValue(), date.getDayOfMonth());
    }

    public static MonthDay of(int month, int day)
    {
        return of(Month.of(month), day);
    }

    public static MonthDay of(Month month, int day)
    {
        if (day < 1 || day > month.length(false))
        {
            throw new DateTimeException("Invalid day: " + day);
        }
        return new MonthDay(month.getValue(), day);
    }

    public static MonthDay parse(CharSequence text)
    {
        String value = text.toString();
        if (value.startsWith("--"))
        {
            value = value.substring(2);
        }
        int dash = value.indexOf('-');
        return of(Integer.parseInt(value.substring(0, dash)),
            Integer.parseInt(value.substring(dash + 1)));
    }

    public int getMonthValue()
    {
        return month;
    }

    public Month getMonth()
    {
        return Month.of(month);
    }

    public int getDayOfMonth()
    {
        return day;
    }

    public boolean isValidYear(int year)
    {
        return !(month == 2 && day == 29 && !Year.isLeap(year));
    }

    public LocalDate atYear(int year)
    {
        return LocalDate.of(year, month, day);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof MonthDay))
        {
            return false;
        }
        MonthDay value = (MonthDay)other;
        return month == value.month && day == value.day;
    }

    public int hashCode()
    {
        return month * 32 + day;
    }

    public String toString()
    {
        return "--" + (month < 10 ? "0" : "") + month + (day < 10 ? "-0" : "-") + day;
    }
}
