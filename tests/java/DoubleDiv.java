// Regression test for the ddiv bytecode bug (present since master): the VM
// must evaluate "value1 / value2" as value1 divided by value2, not value2
// divided by value1.
public class DoubleDiv
{
    static int checks = 0;
    static int failures = 0;

    static void check(String name, boolean ok)
    {
        checks++;
        if (!ok)
        {
            failures++;
            System.out.println("FAIL " + name);
        }
    }

    static double div(double a, double b)
    {
        return a / b;
    }

    public static void main(String[] args)
    {
        double five = 5.0;
        double ten = 10.0;

        check("lit", 5.0 / 10.0 == 0.5);
        check("var-var", five / ten == 0.5);
        check("var-const", five / 10.0 == 0.5);
        check("const-var", 5.0 / ten == 0.5);
        check("param", div(five, ten) == 0.5);
        check("array", five / new double[]{10.0}[0] == 0.5);
        check("field", Holder.ten == 10.0 && five / Holder.ten == 0.5);
        check("chain", (five / ten) * ten == 5.0);
        check("recip", (1.0 / five) * 10.0 == 2.0);
        check("thirds", five / 15.0 == 1.0 / 3.0);
        check("negative", (-five) / ten == -0.5);
        check("two-negatives", (-five) / (-ten) == 0.5);
        check("zero-num", 0.0 / five == 0.0);
        check("neg-zero-num", (-0.0) / five == 0.0 && Double.doubleToRawLongBits((-0.0) / five) == Double.doubleToRawLongBits(-0.0));
        check("zero-den", five / 0.0 == Double.POSITIVE_INFINITY);
        check("neg-zero-den", five / (-0.0) == Double.NEGATIVE_INFINITY);
        check("nan", Double.isNaN(0.0 / 0.0));

        double sum = 0.0;
        for (int i = 1; i <= 1000; i++)
        {
            sum += 1.0 / (double)i;
        }
        check("harmonic", Math.abs(sum - 7.485470860550345) < 1e-12);

        float f = 5.0f;
        float g = 10.0f;
        check("fdiv", f / g == 0.5f);
        check("fdiv-const", f / 10.0f == 0.5f);
        check("fdiv-param", divF(f, g) == 0.5f);

        check("random-range", isUnitRandom());
        check("parse-double", Double.parseDouble("3.25") == 3.25);
        check("parse-neg", Double.parseDouble("-0.125") == -0.125);
        check("value-of", Double.valueOf("2.5").doubleValue() == 2.5);
        check("pow", Math.pow(2.0, 10.0) == 1024.0);
        check("pow-frac", Math.pow(9.0, 0.5) == 3.0);
        check("hypot", Math.hypot(3.0, 4.0) == 5.0);
        check("bigdecimal-dv", java.math.BigDecimal.valueOf(10.0).doubleValue() == 10.0);
        check("string-format-div", String.valueOf(1.0 / 8.0).equals("0.125"));
        check("double-tostring", Double.toString(0.75).equals("0.75"));

        System.out.println("double-div checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }

    static float divF(float a, float b)
    {
        return a / b;
    }

    static boolean isUnitRandom()
    {
        for (int i = 0; i < 16; i++)
        {
            double r = Math.random();
            if (r < 0.0 || r >= 1.0)
            {
                return false;
            }
        }
        return true;
    }

    static class Holder
    {
        static double ten = 10.0;
    }
}
