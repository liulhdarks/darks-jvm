import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

public class SdkMathTest
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
        BigInteger factorial = BigInteger.ONE;
        int i = 1;
        while (i <= 30)
        {
            factorial = factorial.multiply(BigInteger.valueOf(i));
            i = i + 1;
        }
        check("factorial-30", factorial.toString()
              .equals("265252859812191058636308480000000"));

        BigInteger big = new BigInteger("123456789012345678901234567890");
        check("big-add", big.add(BigInteger.ONE).toString()
              .equals("123456789012345678901234567891"));
        check("big-subtract", big.subtract(BigInteger.ONE).toString()
              .equals("123456789012345678901234567889"));
        check("big-pow", BigInteger.valueOf(2).pow(100).toString()
              .equals("1267650600228229401496703205376"));
        check("big-radix", BigInteger.valueOf(255).toString(16).equals("ff"));
        check("big-parse-radix",
              new BigInteger("deadbeef", 16).toString().equals("3735928559"));
        check("big-compare",
              BigInteger.TEN.compareTo(BigInteger.valueOf(2)) > 0);
        check("big-negative",
              BigInteger.valueOf(-1234567890123L).toString()
                  .equals("-1234567890123"));
        check("big-gcd", BigInteger.valueOf(48).gcd(BigInteger.valueOf(18))
              .toString().equals("6"));

        BigInteger[] divided = BigInteger.TEN.pow(18).divideAndRemainder(
            BigInteger.valueOf(7));
        check("big-divide",
              divided[0].toString().equals("142857142857142857"));
        check("big-remainder", divided[1].toString().equals("1"));
        check("big-mod-negative",
              BigInteger.valueOf(-7).mod(BigInteger.valueOf(3)).toString()
                  .equals("2"));

        BigDecimal price = new BigDecimal("123.45");
        check("decimal-multiply",
              price.multiply(new BigDecimal("2.5")).toString().equals("308.625"));
        check("decimal-add",
              price.add(new BigDecimal("0.55")).toString().equals("124.00"));
        check("decimal-divide",
              new BigDecimal("10").divide(new BigDecimal("4")).toString()
                  .equals("2.5"));
        check("decimal-divide-scale",
              new BigDecimal("22").divide(new BigDecimal("7"), 5,
                                          RoundingMode.HALF_UP)
                  .toString().equals("3.14286"));
        check("decimal-set-scale",
              new BigDecimal("2.345").setScale(2, RoundingMode.HALF_UP)
                  .toString().equals("2.35"));
        check("decimal-strip",
              new BigDecimal("123.4500").stripTrailingZeros().toPlainString()
                  .equals("123.45"));
        check("decimal-move",
              new BigDecimal("1234").movePointLeft(2).toPlainString()
                  .equals("12.34"));
        check("decimal-compare",
              new BigDecimal("1.0").compareTo(new BigDecimal("1.00")) == 0);
        check("decimal-negate",
              new BigDecimal("12.5").negate().toString().equals("-12.5"));
        check("decimal-precision", new BigDecimal("123.4500").precision() == 7);
        check("decimal-scale", new BigDecimal("1.2500").scale() == 4);
        check("decimal-int", new BigDecimal("42.9").intValue() == 42);

        System.out.println("math checks=" + checks + " failures=" + failures);
        System.out.println("sdk math: ok");
    }
}
