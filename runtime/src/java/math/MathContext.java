package java.math;

public final class MathContext implements java.io.Serializable
{
    public static final MathContext UNLIMITED = new MathContext(0, RoundingMode.HALF_UP);
    public static final MathContext DECIMAL32 = new MathContext(7, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL64 = new MathContext(16, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL128 = new MathContext(34, RoundingMode.HALF_EVEN);

    private final int precision;
    private final RoundingMode roundingMode;

    public MathContext(int precision)
    {
        this(precision, RoundingMode.HALF_UP);
    }

    public MathContext(int precision, RoundingMode roundingMode)
    {
        if (precision < 0)
        {
            throw new IllegalArgumentException("Digits < 0");
        }
        if (roundingMode == null)
        {
            throw new NullPointerException("null RoundingMode");
        }
        this.precision = precision;
        this.roundingMode = roundingMode;
    }

    public MathContext(String value)
    {
        if (value == null)
        {
            throw new NullPointerException("null String");
        }
        this.precision = 0;
        this.roundingMode = RoundingMode.HALF_UP;
    }

    public int getPrecision()
    {
        return precision;
    }

    public RoundingMode getRoundingMode()
    {
        return roundingMode;
    }

    public boolean equals(Object other)
    {
        if (!(other instanceof MathContext))
        {
            return false;
        }
        MathContext context = (MathContext)other;
        return precision == context.precision && roundingMode == context.roundingMode;
    }

    public int hashCode()
    {
        return precision + roundingMode.hashCode() * 59;
    }

    public String toString()
    {
        return "precision=" + precision + " roundingMode=" + roundingMode;
    }
}
