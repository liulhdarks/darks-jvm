package java.math;

/**
 * Arbitrary precision integer implemented with base-10000 limbs.  All the
 * usual sign/magnitude arithmetic (add, subtract, multiply, divide, pow, gcd)
 * is provided; bit oriented operations are not.
 */
public class BigInteger extends Number implements Comparable<BigInteger>
{
    public static final BigInteger ZERO = new BigInteger(0, new int[0]);
    public static final BigInteger ONE = new BigInteger(1, new int[] {1});
    public static final BigInteger TEN = new BigInteger(1, new int[] {10});

    private static final int BASE = 10000;
    private static final int BASE_DIGITS = 4;

    private final int signum;
    private final int[] mag; // little-endian, no trailing zero limbs

    private BigInteger(int signum, int[] magnitude)
    {
        this.signum = signum;
        this.mag = normalize(magnitude);
    }

    private static int[] normalize(int[] value)
    {
        int length = value.length;
        while (length > 0 && value[length - 1] == 0)
        {
            length = length - 1;
        }
        if (length == value.length)
        {
            return value;
        }
        int[] result = new int[length];
        int i = 0;
        while (i < length)
        {
            result[i] = value[i];
            i = i + 1;
        }
        return result;
    }

    private static BigInteger create(int sign, int[] magnitude)
    {
        int[] normalized = normalize(magnitude);
        if (normalized.length == 0)
        {
            return ZERO;
        }
        return new BigInteger(sign, normalized);
    }

    public BigInteger(String value)
    {
        this(value, 10);
    }

    public BigInteger(String value, int radix)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        if (radix < 2 || radix > 36)
        {
            throw new NumberFormatException("radix out of range");
        }
        String s = value;
        int i = 0;
        int sign = 1;
        if (s.length() > 0 && (s.charAt(0) == '-' || s.charAt(0) == '+'))
        {
            sign = s.charAt(0) == '-' ? -1 : 1;
            i = 1;
        }
        if (i >= s.length())
        {
            throw new NumberFormatException("Zero length BigInteger");
        }
        int[] magnitude = new int[0];
        while (i < s.length())
        {
            int digit = Character.digit(s.charAt(i), radix);
            if (digit < 0)
            {
                throw new NumberFormatException("For input string: \"" + value + "\"");
            }
            magnitude = mulAdd(magnitude, radix, digit);
            i = i + 1;
        }
        int[] normalized = normalize(magnitude);
        this.mag = normalized;
        this.signum = normalized.length == 0 ? 0 : sign;
    }

    public static BigInteger valueOf(long value)
    {
        if (value == 0)
        {
            return ZERO;
        }
        int sign = value < 0 ? -1 : 1;
        long abs = value < 0 ? -value : value;
        if (value == Long.MIN_VALUE)
        {
            abs = Long.MAX_VALUE;
        }
        int[] limbs = new int[5];
        int count = 0;
        while (abs > 0)
        {
            limbs[count] = (int)(abs % (long)BASE);
            abs = abs / (long)BASE;
            count = count + 1;
        }
        if (value == Long.MIN_VALUE)
        {
            // 9223372036854775808 = last magnitude limb + 1
            int carry = 1;
            int i = 0;
            while (carry != 0 && i < count)
            {
                int sum = limbs[i] + carry;
                limbs[i] = sum % BASE;
                carry = sum / BASE;
                i = i + 1;
            }
            if (carry != 0)
            {
                limbs[count] = carry;
                count = count + 1;
            }
        }
        int[] magnitude = new int[count];
        int i = 0;
        while (i < count)
        {
            magnitude[i] = limbs[i];
            i = i + 1;
        }
        return new BigInteger(sign, magnitude);
    }

    private static int[] mulAdd(int[] magnitude, int multiplier, int addend)
    {
        int[] result = new int[magnitude.length + 2];
        long carry = addend;
        int i = 0;
        while (i < magnitude.length)
        {
            long value = (long)magnitude[i] * (long)multiplier + carry;
            result[i] = (int)(value % (long)BASE);
            carry = value / (long)BASE;
            i = i + 1;
        }
        int count = magnitude.length;
        while (carry != 0)
        {
            result[count] = (int)(carry % (long)BASE);
            carry = carry / (long)BASE;
            count = count + 1;
        }
        return normalize(trim(result, count));
    }

    private static int[] trim(int[] value, int length)
    {
        int[] result = new int[length];
        int i = 0;
        while (i < length)
        {
            result[i] = value[i];
            i = i + 1;
        }
        return result;
    }

    public int signum()
    {
        return signum;
    }

    public BigInteger abs()
    {
        return signum >= 0 ? this : new BigInteger(1, mag);
    }

    public BigInteger negate()
    {
        return signum == 0 ? this : new BigInteger(-signum, mag);
    }

    public int compareTo(BigInteger other)
    {
        if (signum != other.signum)
        {
            return signum < other.signum ? -1 : 1;
        }
        int cmp = compareMagnitude(mag, other.mag);
        return signum >= 0 ? cmp : -cmp;
    }

    private static int compareMagnitude(int[] a, int[] b)
    {
        if (a.length != b.length)
        {
            return a.length < b.length ? -1 : 1;
        }
        int i = a.length - 1;
        while (i >= 0)
        {
            if (a[i] != b[i])
            {
                return a[i] < b[i] ? -1 : 1;
            }
            i = i - 1;
        }
        return 0;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof BigInteger))
        {
            return false;
        }
        BigInteger o = (BigInteger)other;
        if (signum != o.signum)
        {
            return false;
        }
        return compareMagnitude(mag, o.mag) == 0;
    }

    public int hashCode()
    {
        int hash = signum;
        int i = 0;
        while (i < mag.length)
        {
            hash = hash * 31 + mag[i];
            i = i + 1;
        }
        return hash;
    }

    public BigInteger add(BigInteger other)
    {
        if (other.signum == 0)
        {
            return this;
        }
        if (signum == 0)
        {
            return other;
        }
        if (signum == other.signum)
        {
            return new BigInteger(signum, addMagnitude(mag, other.mag));
        }
        int cmp = compareMagnitude(mag, other.mag);
        if (cmp == 0)
        {
            return ZERO;
        }
        if (cmp > 0)
        {
            return new BigInteger(signum, subtractMagnitude(mag, other.mag));
        }
        return new BigInteger(other.signum, subtractMagnitude(other.mag, mag));
    }

    public BigInteger subtract(BigInteger other)
    {
        return add(other.negate());
    }

    private static int[] addMagnitude(int[] a, int[] b)
    {
        int length = (a.length > b.length ? a.length : b.length) + 1;
        int[] result = new int[length];
        int carry = 0;
        int i = 0;
        while (i < length)
        {
            int sum = carry;
            if (i < a.length)
            {
                sum = sum + a[i];
            }
            if (i < b.length)
            {
                sum = sum + b[i];
            }
            if (sum >= BASE)
            {
                result[i] = sum - BASE;
                carry = 1;
            }
            else
            {
                result[i] = sum;
                carry = 0;
            }
            i = i + 1;
        }
        return normalize(result);
    }

    private static int[] subtractMagnitude(int[] a, int[] b)
    {
        int[] result = new int[a.length];
        int borrow = 0;
        int i = 0;
        while (i < a.length)
        {
            int diff = a[i] - borrow - (i < b.length ? b[i] : 0);
            if (diff < 0)
            {
                diff = diff + BASE;
                borrow = 1;
            }
            else
            {
                borrow = 0;
            }
            result[i] = diff;
            i = i + 1;
        }
        return normalize(result);
    }

    public BigInteger multiply(BigInteger other)
    {
        if (signum == 0 || other.signum == 0)
        {
            return ZERO;
        }
        int[] a = mag;
        int[] b = other.mag;
        int[] result = new int[a.length + b.length + 1];
        int i = 0;
        while (i < a.length)
        {
            long carry = 0;
            int j = 0;
            while (j < b.length)
            {
                long value = (long)result[i + j] + (long)a[i] * (long)b[j] + carry;
                result[i + j] = (int)(value % (long)BASE);
                carry = value / (long)BASE;
                j = j + 1;
            }
            int k = i + b.length;
            while (carry != 0)
            {
                long value = (long)result[k] + carry;
                result[k] = (int)(value % (long)BASE);
                carry = value / (long)BASE;
                k = k + 1;
            }
            i = i + 1;
        }
        return new BigInteger(signum * other.signum, normalize(result));
    }

    public BigInteger divide(BigInteger divisor)
    {
        return divideAndRemainder(divisor)[0];
    }

    public BigInteger remainder(BigInteger divisor)
    {
        return divideAndRemainder(divisor)[1];
    }

    public BigInteger mod(BigInteger divisor)
    {
        BigInteger r = remainder(divisor);
        if (r.signum < 0)
        {
            r = r.add(divisor.abs());
        }
        return r;
    }

    public BigInteger[] divideAndRemainder(BigInteger divisor)
    {
        if (divisor.signum == 0)
        {
            throw new ArithmeticException("BigInteger divide by zero");
        }
        if (signum == 0)
        {
            return new BigInteger[] {ZERO, ZERO};
        }
        int sign = signum * divisor.signum;
        int[] quotient;
        int[] remainder;
        if (divisor.mag.length == 1)
        {
            int[] q = new int[mag.length];
            int rem = divideSmall(mag, divisor.mag[0], q);
            quotient = normalize(q);
            remainder = rem == 0 ? new int[0] : new int[] {rem};
        }
        else
        {
            int[][] qr = divideMagnitude(mag, divisor.mag);
            quotient = qr[0];
            remainder = qr[1];
        }
        BigInteger q = create(sign, quotient);
        BigInteger r = create(signum, remainder);
        return new BigInteger[] {q, r};
    }

    private static int divideSmall(int[] value, int divisor, int[] quotient)
    {
        long remainder = 0;
        int i = value.length - 1;
        while (i >= 0)
        {
            long current = remainder * (long)BASE + (long)value[i];
            quotient[i] = (int)(current / (long)divisor);
            remainder = current % (long)divisor;
            i = i - 1;
        }
        return (int)remainder;
    }

    /** Knuth algorithm D on base-BASE little-endian magnitudes. */
    private static int[][] divideMagnitude(int[] uIn, int[] vIn)
    {
        int n = vIn.length;
        int m = uIn.length - n;
        int d = BASE / (vIn[n - 1] + 1);
        int[] v = d == 1 ? vIn : multiplySmall(vIn, d);
        int[] u = multiplySmall(uIn, d);
        int[] uExt = new int[u.length + 1];
        int i = 0;
        while (i < u.length)
        {
            uExt[i] = u[i];
            i = i + 1;
        }
        uExt[u.length] = 0;
        int[] q = new int[m + 1];
        int j = m;
        while (j >= 0)
        {
            long numerator = (long)uExt[j + n] * (long)BASE + (long)uExt[j + n - 1];
            long qhat = numerator / (long)v[n - 1];
            long rhat = numerator % (long)v[n - 1];
            while (qhat >= (long)BASE ||
                   qhat * (long)v[n - 2] > (long)BASE * rhat + (long)uExt[j + n - 2])
            {
                qhat = qhat - 1;
                rhat = rhat + (long)v[n - 1];
                if (rhat >= (long)BASE)
                {
                    break;
                }
            }
            long borrow = 0;
            long carry = 0;
            int k = 0;
            while (k < n)
            {
                long product = qhat * (long)v[k] + carry;
                carry = product / (long)BASE;
                long sub = (long)uExt[j + k] - (product % (long)BASE) - borrow;
                if (sub < 0)
                {
                    sub = sub + BASE;
                    borrow = 1;
                }
                else
                {
                    borrow = 0;
                }
                uExt[j + k] = (int)sub;
                k = k + 1;
            }
            long top = (long)uExt[j + n] - carry - borrow;
            if (top < 0)
            {
                // qhat was one too large: add the divisor back.
                qhat = qhat - 1;
                long c = 0;
                int t = 0;
                while (t < n)
                {
                    long sum = (long)uExt[j + t] + (long)v[t] + c;
                    uExt[j + t] = (int)(sum % BASE);
                    c = sum / BASE;
                    t = t + 1;
                }
                uExt[j + n] = (int)((long)uExt[j + n] + c);
            }
            else
            {
                uExt[j + n] = (int)top;
            }
            q[j] = (int)qhat;
            j = j - 1;
        }
        int[] remainder = new int[n];
        i = 0;
        while (i < n)
        {
            remainder[i] = uExt[i];
            i = i + 1;
        }
        if (d != 1)
        {
            int[] q2 = new int[n];
            int rem = divideSmall(remainder, d, q2);
            remainder = normalize(q2);
            if (rem != 0)
            {
                // Cannot happen: the normalization divisor divides exactly.
                remainder = normalize(append(remainder, rem));
            }
        }
        return new int[][] {normalize(q), normalize(remainder)};
    }

    private static int[] multiplySmall(int[] value, int factor)
    {
        int[] result = new int[value.length + 1];
        long carry = 0;
        int i = 0;
        while (i < value.length)
        {
            long current = (long)value[i] * (long)factor + carry;
            result[i] = (int)(current % (long)BASE);
            carry = current / (long)BASE;
            i = i + 1;
        }
        result[value.length] = (int)carry;
        return normalize(result);
    }

    private static int[] append(int[] value, int extra)
    {
        int[] result = new int[value.length + 1];
        int i = 0;
        while (i < value.length)
        {
            result[i] = value[i];
            i = i + 1;
        }
        result[value.length] = extra;
        return result;
    }

    public BigInteger pow(int exponent)
    {
        if (exponent < 0)
        {
            throw new ArithmeticException("Negative exponent");
        }
        if (exponent == 0)
        {
            return ONE;
        }
        BigInteger result = ONE;
        BigInteger base = this;
        int e = exponent;
        while (e > 0)
        {
            if ((e & 1) != 0)
            {
                result = result.multiply(base);
            }
            e = e >>> 1;
            if (e > 0)
            {
                base = base.multiply(base);
            }
        }
        return result;
    }

    public BigInteger gcd(BigInteger other)
    {
        BigInteger a = abs();
        BigInteger b = other.abs();
        while (b.signum != 0)
        {
            BigInteger temp = a.remainder(b);
            a = b;
            b = temp;
        }
        return a;
    }

    public boolean isOdd()
    {
        return mag.length > 0 && (mag[0] & 1) != 0;
    }

    public int intValue()
    {
        long value = longValue();
        return (int)value;
    }

    public long longValue()
    {
        long value = 0;
        int i = mag.length - 1;
        while (i >= 0)
        {
            value = value * (long)BASE + (long)mag[i];
            i = i - 1;
        }
        return signum < 0 ? -value : value;
    }

    public float floatValue()
    {
        return (float)doubleValue();
    }

    public double doubleValue()
    {
        double value = 0.0;
        int i = mag.length - 1;
        while (i >= 0)
        {
            value = value * (double)BASE + (double)mag[i];
            i = i - 1;
        }
        return signum < 0 ? -value : value;
    }

    public String toString()
    {
        return toString(10);
    }

    public String toString(int radix)
    {
        if (radix < 2 || radix > 36)
        {
            throw new NumberFormatException("radix out of range");
        }
        if (signum == 0)
        {
            return "0";
        }
        if (radix == 10)
        {
            StringBuilder out = new StringBuilder();
            if (signum < 0)
            {
                out.append('-');
            }
            int top = mag[mag.length - 1];
            out.append(Integer.toString(top));
            int i = mag.length - 2;
            while (i >= 0)
            {
                String part = Integer.toString(mag[i]);
                int pad = BASE_DIGITS - part.length();
                while (pad > 0)
                {
                    out.append('0');
                    pad = pad - 1;
                }
                out.append(part);
                i = i - 1;
            }
            return out.toString();
        }
        // Repeated division for other radixes.
        StringBuilder out = new StringBuilder();
        BigInteger value = abs();
        BigInteger base = valueOf(radix);
        while (value.signum != 0)
        {
            BigInteger[] qr = value.divideAndRemainder(base);
            int digit = qr[1].intValue();
            out.append(Character.forDigit(digit, radix));
            value = qr[0];
        }
        if (signum < 0)
        {
            out.append('-');
        }
        return out.reverse().toString();
    }
}
