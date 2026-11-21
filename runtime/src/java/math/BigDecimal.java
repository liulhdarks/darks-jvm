package java.math;

/**
 * Arbitrary precision decimal: an unscaled BigInteger plus a scale.  The
 * common arithmetic, comparison, rounding and text conversions are provided.
 */
public class BigDecimal extends Number implements Comparable<BigDecimal>
{
    public static final BigDecimal ZERO = new BigDecimal(BigInteger.ZERO, 0);
    public static final BigDecimal ONE = new BigDecimal(BigInteger.ONE, 0);
    public static final BigDecimal TEN = new BigDecimal(BigInteger.TEN, 0);

    private final BigInteger intVal;
    private final int scale;

    public BigDecimal(String value)
    {
        if (value == null)
        {
            throw new NullPointerException("value");
        }
        String s = value;
        int i = 0;
        int sign = 1;
        if (s.length() > 0 && (s.charAt(0) == '-' || s.charAt(0) == '+'))
        {
            sign = s.charAt(0) == '-' ? -1 : 1;
            i = 1;
        }
        StringBuilder digits = new StringBuilder();
        int digitsAfterPoint = 0;
        boolean point = false;
        while (i < s.length())
        {
            char c = s.charAt(i);
            if (c == '.')
            {
                if (point)
                {
                    throw new NumberFormatException("Invalid BigDecimal: " + value);
                }
                point = true;
                i = i + 1;
            }
            else if (c >= '0' && c <= '9')
            {
                digits.append(c);
                if (point)
                {
                    digitsAfterPoint = digitsAfterPoint + 1;
                }
                i = i + 1;
            }
            else
            {
                break;
            }
        }
        int exponent = 0;
        if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E'))
        {
            i = i + 1;
            int expSign = 1;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-'))
            {
                expSign = s.charAt(i) == '-' ? -1 : 1;
                i = i + 1;
            }
            if (i >= s.length())
            {
                throw new NumberFormatException("Invalid BigDecimal: " + value);
            }
            while (i < s.length())
            {
                char c = s.charAt(i);
                if (c < '0' || c > '9')
                {
                    throw new NumberFormatException("Invalid BigDecimal: " + value);
                }
                exponent = exponent * 10 + (c - '0');
                i = i + 1;
            }
            exponent = exponent * expSign;
        }
        if (i != s.length() || digits.length() == 0)
        {
            throw new NumberFormatException("Invalid BigDecimal: " + value);
        }
        BigInteger unscaled = new BigInteger(digits.toString());
        if (sign < 0)
        {
            unscaled = unscaled.negate();
        }
        this.intVal = unscaled;
        this.scale = digitsAfterPoint - exponent;
    }

    public BigDecimal(BigInteger unscaledValue)
    {
        this(unscaledValue, 0);
    }

    public BigDecimal(BigInteger unscaledValue, int scale)
    {
        if (unscaledValue == null)
        {
            throw new NullPointerException("unscaledValue");
        }
        this.intVal = unscaledValue;
        this.scale = scale;
    }

    public BigDecimal(int value)
    {
        this(BigInteger.valueOf(value), 0);
    }

    public BigDecimal(long value)
    {
        this(BigInteger.valueOf(value), 0);
    }

    public BigDecimal(double value)
    {
        this(Double.toString(value));
    }

    public static BigDecimal valueOf(long value)
    {
        return new BigDecimal(BigInteger.valueOf(value), 0);
    }

    public static BigDecimal valueOf(long unscaledValue, int scale)
    {
        return new BigDecimal(BigInteger.valueOf(unscaledValue), scale);
    }

    public static BigDecimal valueOf(double value)
    {
        return new BigDecimal(Double.toString(value));
    }

    public BigInteger unscaledValue()
    {
        return intVal;
    }

    public int scale()
    {
        return scale;
    }

    public int signum()
    {
        return intVal.signum();
    }

    public int precision()
    {
        return intVal.abs().toString().length();
    }

    public BigDecimal abs()
    {
        return signum() < 0 ? negate() : this;
    }

    public BigDecimal negate()
    {
        return new BigDecimal(intVal.negate(), scale);
    }

    public BigDecimal plus()
    {
        return this;
    }

    public BigDecimal add(BigDecimal other)
    {
        if (scale == other.scale)
        {
            return new BigDecimal(intVal.add(other.intVal), scale);
        }
        int target = scale > other.scale ? scale : other.scale;
        BigInteger a = scaleBy(intVal, target - scale);
        BigInteger b = scaleBy(other.intVal, target - other.scale);
        return new BigDecimal(a.add(b), target);
    }

    public BigDecimal subtract(BigDecimal other)
    {
        return add(other.negate());
    }

    public BigDecimal multiply(BigDecimal other)
    {
        return new BigDecimal(intVal.multiply(other.intVal), scale + other.scale);
    }

    private static BigInteger scaleBy(BigInteger value, int power)
    {
        if (power == 0)
        {
            return value;
        }
        if (power > 0)
        {
            return value.multiply(BigInteger.TEN.pow(power));
        }
        return value.divide(BigInteger.TEN.pow(-power));
    }

    public BigDecimal divide(BigDecimal divisor)
    {
        if (divisor.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        int preferred = scale - divisor.scale;
        int target = preferred;
        int limit = preferred + 1000;
        while (target <= limit)
        {
            try
            {
                return divide(divisor, target, RoundingMode.UNNECESSARY);
            }
            catch (ArithmeticException notExact)
            {
                target = target + 1;
            }
        }
        throw new ArithmeticException("Non-terminating decimal expansion");
    }

    public BigDecimal divide(BigDecimal divisor, int scale, RoundingMode roundingMode)
    {
        if (divisor.signum() == 0)
        {
            throw new ArithmeticException("Division by zero");
        }
        int shift = divisor.scale - this.scale + scale;
        BigInteger numerator;
        BigInteger denominator;
        if (shift >= 0)
        {
            numerator = scaleBy(this.intVal, shift);
            denominator = divisor.intVal;
        }
        else
        {
            numerator = this.intVal;
            denominator = scaleBy(divisor.intVal, -shift);
        }
        BigInteger quotient = numerator.divide(denominator);
        BigInteger remainder = numerator.remainder(denominator);
        if (remainder.signum() != 0)
        {
            boolean negative = (numerator.signum() * denominator.signum()) < 0;
            boolean away = false;
            BigInteger twice = remainder.abs().multiply(BigInteger.valueOf(2));
            BigInteger absDivisor = denominator.abs();
            int half = twice.compareTo(absDivisor);
            if (roundingMode == RoundingMode.UP)
            {
                away = true;
            }
            else if (roundingMode == RoundingMode.DOWN)
            {
                away = false;
            }
            else if (roundingMode == RoundingMode.CEILING)
            {
                away = !negative;
            }
            else if (roundingMode == RoundingMode.FLOOR)
            {
                away = negative;
            }
            else if (roundingMode == RoundingMode.HALF_UP)
            {
                away = half >= 0;
            }
            else if (roundingMode == RoundingMode.HALF_DOWN)
            {
                away = half > 0;
            }
            else if (roundingMode == RoundingMode.HALF_EVEN)
            {
                away = half > 0 || (half == 0 && quotient.isOdd());
            }
            else
            {
                throw new ArithmeticException("Rounding necessary");
            }
            if (away)
            {
                quotient = negative ? quotient.subtract(BigInteger.ONE)
                                    : quotient.add(BigInteger.ONE);
            }
        }
        return new BigDecimal(quotient, scale);
    }

    public BigDecimal divide(BigDecimal divisor, RoundingMode roundingMode)
    {
        return divide(divisor, scale - divisor.scale, roundingMode);
    }

    public BigDecimal setScale(int newScale)
    {
        return setScale(newScale, RoundingMode.UNNECESSARY);
    }

    public BigDecimal setScale(int newScale, RoundingMode roundingMode)
    {
        if (newScale == scale)
        {
            return this;
        }
        if (newScale > scale)
        {
            return new BigDecimal(scaleBy(intVal, newScale - scale), newScale);
        }
        return divide(ONE, newScale, roundingMode);
    }

    public BigDecimal movePointLeft(int n)
    {
        return new BigDecimal(intVal, scale + n);
    }

    public BigDecimal movePointRight(int n)
    {
        return new BigDecimal(intVal, scale - n);
    }

    public BigDecimal stripTrailingZeros()
    {
        if (intVal.signum() == 0)
        {
            return new BigDecimal(BigInteger.ZERO, 0);
        }
        BigInteger value = intVal;
        int newScale = scale;
        while (newScale > 0 && value.mod(BigInteger.TEN).signum() == 0)
        {
            value = value.divide(BigInteger.TEN);
            newScale = newScale - 1;
        }
        return new BigDecimal(value, newScale);
    }

    public BigDecimal max(BigDecimal other)
    {
        return compareTo(other) >= 0 ? this : other;
    }

    public BigDecimal min(BigDecimal other)
    {
        return compareTo(other) <= 0 ? this : other;
    }

    public int compareTo(BigDecimal other)
    {
        if (scale == other.scale)
        {
            return intVal.compareTo(other.intVal);
        }
        int target = scale > other.scale ? scale : other.scale;
        BigInteger a = scaleBy(intVal, target - scale);
        BigInteger b = scaleBy(other.intVal, target - other.scale);
        return a.compareTo(b);
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof BigDecimal))
        {
            return false;
        }
        BigDecimal o = (BigDecimal)other;
        return scale == o.scale && intVal.equals(o.intVal);
    }

    public int hashCode()
    {
        return intVal.hashCode() * 31 + scale;
    }

    public int intValue()
    {
        return toBigInteger().intValue();
    }

    public long longValue()
    {
        return toBigInteger().longValue();
    }

    public float floatValue()
    {
        return (float)doubleValue();
    }

    public double doubleValue()
    {
        return intVal.doubleValue() * Math.pow(10.0, (double)(-scale));
    }

    private BigInteger toBigInteger()
    {
        if (scale == 0)
        {
            return intVal;
        }
        if (scale > 0)
        {
            return intVal.divide(BigInteger.TEN.pow(scale));
        }
        return intVal.multiply(BigInteger.TEN.pow(-scale));
    }

    public String toPlainString()
    {
        String digits = intVal.abs().toString();
        StringBuilder out = new StringBuilder();
        if (intVal.signum() < 0)
        {
            out.append('-');
        }
        if (scale <= 0)
        {
            out.append(digits);
            int i = 0;
            while (i < -scale)
            {
                out.append('0');
                i = i + 1;
            }
            return out.toString();
        }
        if (digits.length() > scale)
        {
            out.append(digits.substring(0, digits.length() - scale));
            out.append('.');
            out.append(digits.substring(digits.length() - scale));
        }
        else
        {
            out.append('0');
            out.append('.');
            int zeros = scale - digits.length();
            int i = 0;
            while (i < zeros)
            {
                out.append('0');
                i = i + 1;
            }
            out.append(digits);
        }
        return out.toString();
    }

    public String toString()
    {
        String digits = intVal.abs().toString();
        if (intVal.signum() == 0)
        {
            if (scale == 0)
            {
                return "0";
            }
            if (scale > 0 && scale <= 6)
            {
                StringBuilder zero = new StringBuilder("0.");
                int i = 0;
                while (i < scale)
                {
                    zero.append('0');
                    i = i + 1;
                }
                return zero.toString();
            }
            if (scale > 0)
            {
                return "0E-" + scale;
            }
            return "0E+" + (-scale);
        }
        int adjusted = digits.length() - 1 - scale;
        StringBuilder out = new StringBuilder();
        if (intVal.signum() < 0)
        {
            out.append('-');
        }
        if (scale >= 0 && adjusted >= -6)
        {
            if (scale == 0)
            {
                out.append(digits);
            }
            else if (digits.length() > scale)
            {
                out.append(digits.substring(0, digits.length() - scale));
                out.append('.');
                out.append(digits.substring(digits.length() - scale));
            }
            else
            {
                out.append("0.");
                int zeros = scale - digits.length();
                int i = 0;
                while (i < zeros)
                {
                    out.append('0');
                    i = i + 1;
                }
                out.append(digits);
            }
            return out.toString();
        }
        out.append(digits.charAt(0));
        if (digits.length() > 1)
        {
            out.append('.');
            out.append(digits.substring(1));
        }
        out.append('E');
        if (adjusted >= 0)
        {
            out.append('+');
        }
        out.append(Integer.toString(adjusted));
        return out.toString();
    }
}
