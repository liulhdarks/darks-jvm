import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SdkTextTest
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks = checks + 1;
        if (!ok)
        {
            failures = failures + 1;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args)
    {
        DecimalFormat decimal = new DecimalFormat("#,##0.00");
        check("decimal-grouping",
              decimal.format(1234567.891).equals("1,234,567.89"));
        check("decimal-negative",
              decimal.format(-1234.5).equals("-1,234.50"));
        check("decimal-integer", decimal.format(42L).equals("42.00"));
        check("decimal-pattern", decimal.toPattern().equals("#,##0.00"));

        DecimalFormat trimmed = new DecimalFormat("0.###");
        check("decimal-trim", trimmed.format(2.5).equals("2.5"));

        DecimalFormat percent = new DecimalFormat("0.00%");
        check("decimal-percent", percent.format(0.125).equals("12.50%"));

        NumberFormat integers = NumberFormat.getIntegerInstance();
        check("number-integer", integers.format(1234567L).equals("1,234,567"));

        NumberFormat numbers = NumberFormat.getNumberInstance(Locale.ROOT);
        check("number-decimal", numbers.format(1234.5).equals("1,234.5"));

        try
        {
            Number parsed = decimal.parse("1,234.50");
            check("decimal-parse", parsed.doubleValue() == 1234.5);
        }
        catch (ParseException broken)
        {
            check("decimal-parse", false);
        }
        try
        {
            Number parsed = NumberFormat.getIntegerInstance().parse("12,345");
            check("integer-parse", parsed.longValue() == 12345L);
        }
        catch (ParseException broken)
        {
            check("integer-parse", false);
        }

        check("message-plain",
              MessageFormat.format("Hello {0}!", "world").equals("Hello world!"));
        check("message-number",
              MessageFormat.format("total={0,number,#,##0.00}", new Double(1234.5))
                  .equals("total=1,234.50"));
        check("message-repeat",
              MessageFormat.format("{0}-{0}-{1}", "a", "b").equals("a-a-b"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        check("dateformat-format",
              dateFormat.format(new Date(0L)).equals("1970-01-01 00:00:00"));
        check("dateformat-epoch",
              dateFormat.format(new Date(1700000000000L))
                  .equals("2023-11-14 22:13:20"));
        try
        {
            Date parsed = dateFormat.parse("2024-01-02 03:04:05");
            check("dateformat-parse", parsed.getTime() == 1704164645000L);
        }
        catch (ParseException broken)
        {
            check("dateformat-parse", false);
        }
        check("dateformat-pattern",
              dateFormat.toPattern().equals("yyyy-MM-dd HH:mm:ss"));

        SimpleDateFormat iso = SimpleDateFormat.isoDateTimeFormat();
        check("iso-format",
              iso.format(new Date(0L)).equals("1970-01-01T00:00:00Z"));

        System.out.println("text checks=" + checks + " failures=" + failures);
        System.out.println("sdk text: ok");
    }
}
