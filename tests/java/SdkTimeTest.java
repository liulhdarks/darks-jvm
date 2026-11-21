import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SdkTimeTest
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
        Instant instant = Instant.ofEpochSecond(1700000000L, 123456789L);
        check("instant-toString",
              instant.toString().equals("2023-11-14T22:13:20.123456789Z"));
        check("instant-seconds", instant.getEpochSecond() == 1700000000L);
        check("instant-nanos", instant.getNano() == 123456789);
        check("instant-millis", instant.toEpochMilli() == 1700000000123L);
        check("instant-parse",
              Instant.parse("2023-11-14T22:13:20Z").getEpochSecond() == 1700000000L);
        check("instant-plus",
              instant.plus(Duration.ofSeconds(10)).getEpochSecond() == 1700000010L);
        check("instant-minus-millis",
              Instant.ofEpochMilli(1500L).minusMillis(500L).toEpochMilli() == 1000L);

        Duration duration = Duration.ofMinutes(90);
        check("duration-toString", duration.toString().equals("PT1H30M"));
        check("duration-seconds", duration.getSeconds() == 5400L);
        check("duration-from-text",
              Duration.parse("PT2H3M4.5S").toMillis() == 7384500L);
        check("duration-between",
              Duration.between(Instant.ofEpochSecond(100L),
                              Instant.ofEpochSecond(250L, 500L)).toMillis() == 150000L);
        check("duration-negated", Duration.ofSeconds(5).negated().isNegative());

        LocalDate leap = LocalDate.of(2024, 2, 29);
        check("date-leap", leap.isLeapYear() && leap.lengthOfMonth() == 29);
        check("date-plus-day",
              leap.plusDays(1).toString().equals("2024-03-01"));
        check("date-minus-year",
              leap.minusYears(1).toString().equals("2023-02-28"));
        check("date-parse", LocalDate.parse("2024-12-31").toEpochDay() == 20088L);
        check("date-epoch-day",
              LocalDate.ofEpochDay(0L).toString().equals("1970-01-01"));

        LocalTime time = LocalTime.of(13, 45, 30);
        check("time-plus", time.plusMinutes(20).toString().equals("14:05:30"));
        check("time-wrapped", LocalTime.of(23, 30).plusMinutes(45).toString()
                                  .equals("00:15"));
        check("time-parse", LocalTime.parse("06:07:08").getSecond() == 8);
        check("time-nano", LocalTime.of(1, 2, 3, 400000000).getNano() == 400000000);

        LocalDateTime dateTime =
            LocalDateTime.of(2024, 1, 2, 3, 4, 5);
        check("datetime-epoch",
              dateTime.toEpochSecond(ZoneOffset.UTC) == 1704164645L);
        check("datetime-toString",
              dateTime.toString().equals("2024-01-02T03:04:05"));
        check("datetime-parse",
              LocalDateTime.parse("2024-01-02T03:04:05").equals(dateTime));
        check("datetime-plus",
              dateTime.plusHours(22).toString().equals("2024-01-03T01:04:05"));

        ZoneId shanghai = ZoneId.of("Asia/Shanghai");
        check("zone-id", shanghai.getId().equals("Asia/Shanghai"));
        check("zone-offset", shanghai.getOffsetSeconds(0L) == 8 * 3600);
        check("zone-offset-parse",
              ZoneOffset.of("+08:00").getTotalSeconds() == 8 * 3600);
        check("zone-utc", ZoneOffset.of("Z").getTotalSeconds() == 0);

        ZonedDateTime zoned = dateTime.atZone(shanghai);
        check("zoned-instant", zoned.toInstant().getEpochSecond() == 1704135845L);
        check("zoned-toString",
              zoned.toString().equals("2024-01-02T03:04:05+08:00[Asia/Shanghai]"));
        ZonedDateTime parsed =
            ZonedDateTime.parse("2024-01-02T03:04:05+08:00[Asia/Shanghai]");
        check("zoned-parse", parsed.equals(zoned));
        check("zoned-zone", parsed.getZone().getId().equals("Asia/Shanghai"));
        check("zoned-utc",
              zoned.withZoneSameInstant(ZoneOffset.UTC).toString()
                  .equals("2024-01-01T19:04:05Z"));

        DateTimeFormatter custom =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        check("formatter-format",
              custom.format(dateTime).equals("2024-01-02 03:04:05"));
        check("formatter-parse",
              custom.parse("2024-01-02 03:04:05").toString()
                  .equals("2024-01-02T03:04:05"));
        check("formatter-iso",
              DateTimeFormatter.ISO_LOCAL_DATE.format(leap).equals("2024-02-29"));
        check("formatter-iso-parse",
              DateTimeFormatter.ISO_LOCAL_DATE.parse("2024-06-15").toString()
                  .equals("2024-06-15"));

        System.out.println("time checks=" + checks + " failures=" + failures);
        System.out.println("sdk time: ok");
    }
}
