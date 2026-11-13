package java.util;

// java.util.Random-compatible 48-bit linear congruential generator.  A fixed
// seed therefore produces the same sequence as the JDK implementation.
public class Random
{
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    private long seed;

    public Random()
    {
        this(System.nanoTime());
    }

    public Random(long seed)
    {
        setSeed(seed);
    }

    public void setSeed(long seed)
    {
        this.seed = (seed ^ MULTIPLIER) & MASK;
    }

    protected int next(int bits)
    {
        seed = (seed * MULTIPLIER + ADDEND) & MASK;
        return (int)(seed >>> (48 - bits));
    }

    public int nextInt()
    {
        return next(32);
    }

    public int nextInt(int bound)
    {
        if (bound <= 0)
        {
            throw new IllegalArgumentException("bound must be positive");
        }
        if ((bound & -bound) == bound)
        {
            return (int)(((long)bound * (long)next(31)) >> 31);
        }
        int bits;
        int value;
        do
        {
            bits = next(31);
            value = bits % bound;
        }
        while (bits - value + (bound - 1) < 0);
        return value;
    }

    public long nextLong()
    {
        long high = (long)next(32) << 32;
        long low = (long)next(32) & 0xFFFFFFFFL;
        return high + low;
    }

    public boolean nextBoolean()
    {
        return next(1) != 0;
    }

    public double nextDouble()
    {
        long high = (long)next(26) << 27;
        long low = (long)next(27);
        return (double)(high + low) / (double)(1L << 53);
    }

    public float nextFloat()
    {
        return (float)next(24) / (float)(1 << 24);
    }
}
