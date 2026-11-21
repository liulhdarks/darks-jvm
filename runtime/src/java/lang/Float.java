package java.lang;

public final class Float extends Number implements Comparable<Float>
{
    public static final Class TYPE = Class.getPrimitiveClass("float");

    public static final float MIN_VALUE = 1.4E-45f;
    public static final float MAX_VALUE = 3.4028235E38f;

    private final float value;

    public Float(float value)
    {
        this.value = value;
    }

    public static Float valueOf(String s)
    {
        return new Float(parseFloat(s));
    }

    public static Float valueOf(float value)
    {
        return new Float(value);
    }

    public float floatValue()
    {
        return value;
    }

    public double doubleValue()
    {
        return (double)value;
    }

    public int intValue()
    {
        return (int)value;
    }

    public long longValue()
    {
        return (long)value;
    }


    public static native int floatToRawIntBits(float value);

    public static native float intBitsToFloat(int bits);

    public static int floatToIntBits(float value)
    {
        if (isNaN(value))
        {
            return 0x7fc00000;
        }
        return floatToRawIntBits(value);
    }

    public static boolean equals(float a, float b)
    {
        return (a == b) && (a != 0.0f || floatToIntBits(a) == floatToIntBits(b));
    }

    public static int hashCode(float value)
    {
        return floatToIntBits(value);
    }

    public static int compare(float a, float b)
    {
        if (a < b)
        {
            return -1;
        }
        if (a > b)
        {
            return 1;
        }
        int bitsA = floatToIntBits(a);
        int bitsB = floatToIntBits(b);
        return bitsA == bitsB ? 0 : (bitsA < bitsB ? -1 : 1);
    }

    public static float parseFloat(String s)
    {
        return (float)Double.parseDouble(s);
    }
    public static boolean isNaN(float v)
    {
        return v != v;
    }

    public static boolean isInfinite(float v)
    {
        return v > MAX_VALUE || v < -MAX_VALUE;
    }

    public static boolean isFinite(float v)
    {
        return !isNaN(v) && !isInfinite(v);
    }

    public boolean equals(Object other)
    {
        return (other instanceof Float) && ((Float)other).value == value;
    }

    public int hashCode()
    {
        long bits = (long)value;
        return (int)(bits ^ (bits >>> 32));
    }

    public int compareTo(Float other)
    {
        if (value < other.value)
        {
            return -1;
        }
        return value == other.value ? 0 : 1;
    }

    public static String toString(float v)
    {
        return Double.toString((double)v);
    }

    public String toString()
    {
        return toString(value);
    }
}
