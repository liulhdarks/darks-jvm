package java.lang;

import jdk.internal.math.DoubleConsts;
import jdk.internal.math.FloatConsts;

public final class Math
{
    public static final double PI = 3.141592653589793;
    public static final double E = 2.718281828459045;

    public static native double sqrt(double value);
    public static native double pow(double base, double exponent);
    public static native double floor(double value);
    public static native double ceil(double value);

    public static int abs(int value)
    {
        return value < 0 ? -value : value;
    }

    public static long abs(long value)
    {
        return value < 0 ? -value : value;
    }

    public static double abs(double value)
    {
        return value < 0 ? -value : value;
    }

    public static int max(int a, int b)
    {
        return a > b ? a : b;
    }

    public static long max(long a, long b)
    {
        return a > b ? a : b;
    }

    public static double max(double a, double b)
    {
        if (a != a) return a;
        if ((a == 0.0d) && (b == 0.0d) &&
            (Double.doubleToRawLongBits(a) == 0x8000000000000000L)) return b;
        return (a >= b) ? a : b;
    }

    public static int min(int a, int b)
    {
        return a < b ? a : b;
    }

    public static long min(long a, long b)
    {
        return a < b ? a : b;
    }

    public static double min(double a, double b)
    {
        if (a != a) return a;
        if ((a == 0.0d) && (b == 0.0d) &&
            (Double.doubleToRawLongBits(b) == 0x8000000000000000L)) return b;
        return (a <= b) ? a : b;
    }

    public static long round(double value)
    {
        if (value < 0)
        {
            return (long)(value - 0.5);
        }
        return (long)(value + 0.5);
    }

    public static int round(float value)
    {
        return (int)round((double)value);
    }

    public static int signum(int value)
    {
        return value < 0 ? -1 : (value == 0 ? 0 : 1);
    }

    public static long signum(long value)
    {
        return value < 0 ? -1L : (value == 0 ? 0L : 1L);
    }

    public static double signum(double value)
    {
        if (value < 0)
        {
            return -1.0;
        }
        if (value > 0)
        {
            return 1.0;
        }
        return value;
    }

    public static int addExact(int a, int b)
    {
        int result = a + b;
        if (((a ^ result) & (b ^ result)) < 0)
        {
            throw new ArithmeticException("integer overflow");
        }
        return result;
    }

    public static long addExact(long a, long b)
    {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0)
        {
            throw new ArithmeticException("long overflow");
        }
        return result;
    }

    public static int multiplyExact(int a, int b)
    {
        long result = (long) a * (long) b;
        if ((int) result != result)
        {
            throw new ArithmeticException("integer overflow");
        }
        return (int) result;
    }

    public static long multiplyExact(long a, long b)
    {
        if (a == Long.MIN_VALUE && b == -1)
        {
            throw new ArithmeticException("long overflow");
        }
        long result = a * b;
        if (a != 0 && (result / a != b))
        {
            throw new ArithmeticException("long overflow");
        }
        return result;
    }

    public static int toIntExact(long value)
    {
        if (value < -2147483648L || value > 2147483647L)
        {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }

    public static int floorDiv(int a, int b)
    {
        int q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0)))
        {
            q = q - 1;
        }
        return q;
    }

    public static long floorDiv(long a, long b)
    {
        long q = a / b;
        if ((a % b != 0) && ((a < 0) != (b < 0)))
        {
            q = q - 1;
        }
        return q;
    }

    public static int floorMod(int a, int b)
    {
        return a - floorDiv(a, b) * b;
    }

    public static long floorMod(long a, long b)
    {
        return a - floorDiv(a, b) * b;
    }

    public static double hypot(double a, double b)
    {
        return sqrt(a * a + b * b);
    }

    public static double random()
    {
        long n = System.nanoTime() & 16777215L;
        return (double)n / 16777216.0;
    }
    // Interim helpers required by the JDK 11 Double/Float port; the full
    // JDK 11 Math implementation replaces this class in the next change.

    public static float max(float a, float b) {
        if (a != a) return a;
        if ((a == 0.0f) && (b == 0.0f) &&
            (Float.floatToRawIntBits(a) == 0x80000000)) return b;
        return (a >= b) ? a : b;
    }

    public static float min(float a, float b) {
        if (a != a) return a;
        if ((a == 0.0f) && (b == 0.0f) &&
            (Float.floatToRawIntBits(b) == 0x80000000)) return b;
        return (a <= b) ? a : b;
    }

    public static double copySign(double magnitude, double sign) {
        return Double.longBitsToDouble((Double.doubleToRawLongBits(sign) &
                                        (DoubleConsts.SIGN_BIT_MASK)) |
                                       (Double.doubleToRawLongBits(magnitude) &
                                        (DoubleConsts.EXP_BIT_MASK |
                                         DoubleConsts.SIGNIF_BIT_MASK)));
    }

    public static float copySign(float magnitude, float sign) {
        return Float.intBitsToFloat((Float.floatToRawIntBits(sign) &
                                     (FloatConsts.SIGN_BIT_MASK)) |
                                    (Float.floatToRawIntBits(magnitude) &
                                     (FloatConsts.EXP_BIT_MASK |
                                      FloatConsts.SIGNIF_BIT_MASK)));
    }

    public static int getExponent(float f) {
        /*
         * Bitwise convert f to integer, mask out exponent bits, shift
         * to the right and then subtract out float's bias adjust to
         * get true exponent value
         */
        return ((Float.floatToRawIntBits(f) & FloatConsts.EXP_BIT_MASK) >>
                (FloatConsts.SIGNIFICAND_WIDTH - 1)) - FloatConsts.EXP_BIAS;
    }

    public static int getExponent(double d) {
        /*
         * Bitwise convert d to long, mask out exponent bits, shift
         * to the right and then subtract out double's bias adjust to
         * get true exponent value.
         */
        return (int)(((Double.doubleToRawLongBits(d) & DoubleConsts.EXP_BIT_MASK) >>
                      (DoubleConsts.SIGNIFICAND_WIDTH - 1)) - DoubleConsts.EXP_BIAS);
    }

    public static double scalb(double d, int scaleFactor) {
        /*
         * This method does not need to be declared strictfp to
         * compute the same correct result on all platforms.  When
         * scaling up, it does not matter what order the
         * multiply-store operations are done; the result will be
         * finite or overflow regardless of the operation ordering.
         * However, to get the correct result when scaling down, a
         * particular ordering must be used.
         *
         * When scaling down, the multiply-store operations are
         * sequenced so that it is not possible for two consecutive
         * multiply-stores to return subnormal results.  If one
         * multiply-store result is subnormal, the next multiply will
         * round it away to zero.  This is done by first multiplying
         * by 2 ^ (scaleFactor % n) and then multiplying several
         * times by 2^n as needed where n is the exponent of number
         * that is a covenient power of two.  In this way, at most one
         * real rounding error occurs.  If the double value set is
         * being used exclusively, the rounding will occur on a
         * multiply.  If the double-extended-exponent value set is
         * being used, the products will (perhaps) be exact but the
         * stores to d are guaranteed to round to the double value
         * set.
         *
         * It is _not_ a valid implementation to first multiply d by
         * 2^MIN_EXPONENT and then by 2 ^ (scaleFactor %
         * MIN_EXPONENT) since even in a strictfp program double
         * rounding on underflow could occur; e.g. if the scaleFactor
         * argument was (MIN_EXPONENT - n) and the exponent of d was a
         * little less than -(MIN_EXPONENT - n), meaning the final
         * result would be subnormal.
         *
         * Since exact reproducibility of this method can be achieved
         * without any undue performance burden, there is no
         * compelling reason to allow double rounding on underflow in
         * scalb.
         */

        // magnitude of a power of two so large that scaling a finite
        // nonzero value by it would be guaranteed to over or
        // underflow; due to rounding, scaling down takes an
        // additional power of two which is reflected here
        final int MAX_SCALE = Double.MAX_EXPONENT + -Double.MIN_EXPONENT +
                              DoubleConsts.SIGNIFICAND_WIDTH + 1;
        int exp_adjust = 0;
        int scale_increment = 0;
        double exp_delta = Double.NaN;

        // Make sure scaling factor is in a reasonable range

        if(scaleFactor < 0) {
            scaleFactor = Math.max(scaleFactor, -MAX_SCALE);
            scale_increment = -512;
            exp_delta = twoToTheDoubleScaleDown;
        }
        else {
            scaleFactor = Math.min(scaleFactor, MAX_SCALE);
            scale_increment = 512;
            exp_delta = twoToTheDoubleScaleUp;
        }

        // Calculate (scaleFactor % +/-512), 512 = 2^9, using
        // technique from "Hacker's Delight" section 10-2.
        int t = (scaleFactor >> 9-1) >>> 32 - 9;
        exp_adjust = ((scaleFactor + t) & (512 -1)) - t;

        d *= powerOfTwoD(exp_adjust);
        scaleFactor -= exp_adjust;

        while(scaleFactor != 0) {
            d *= exp_delta;
            scaleFactor -= scale_increment;
        }
        return d;
    }

    public static float scalb(float f, int scaleFactor) {
        // magnitude of a power of two so large that scaling a finite
        // nonzero value by it would be guaranteed to over or
        // underflow; due to rounding, scaling down takes an
        // additional power of two which is reflected here
        final int MAX_SCALE = Float.MAX_EXPONENT + -Float.MIN_EXPONENT +
                              FloatConsts.SIGNIFICAND_WIDTH + 1;

        // Make sure scaling factor is in a reasonable range
        scaleFactor = Math.max(Math.min(scaleFactor, MAX_SCALE), -MAX_SCALE);

        /*
         * Since + MAX_SCALE for float fits well within the double
         * exponent range and + float -> double conversion is exact
         * the multiplication below will be exact. Therefore, the
         * rounding that occurs when the double product is cast to
         * float will be the correctly rounded float result.  Since
         * all operations other than the final multiply will be exact,
         * it is not necessary to declare this method strictfp.
         */
        return (float)((double)f*powerOfTwoD(scaleFactor));
    }

    // Constants used in scalb
    static double twoToTheDoubleScaleUp = powerOfTwoD(512);
    static double twoToTheDoubleScaleDown = powerOfTwoD(-512);

    static double powerOfTwoD(int n) {
        assert(n >= Double.MIN_EXPONENT && n <= Double.MAX_EXPONENT);
        return Double.longBitsToDouble((((long)n + (long)DoubleConsts.EXP_BIAS) <<
                                        (DoubleConsts.SIGNIFICAND_WIDTH-1))
                                       & DoubleConsts.EXP_BIT_MASK);
    }

    static float powerOfTwoF(int n) {
        assert(n >= Float.MIN_EXPONENT && n <= Float.MAX_EXPONENT);
        return Float.intBitsToFloat(((n + FloatConsts.EXP_BIAS) <<
                                     (FloatConsts.SIGNIFICAND_WIDTH-1))
                                    & FloatConsts.EXP_BIT_MASK);
    }

}
