package java.util.concurrent;

public class ThreadLocalRandom extends java.util.Random
{
    private static final ThreadLocalRandom INSTANCE = new ThreadLocalRandom();

    private ThreadLocalRandom()
    {
        super();
    }

    public static ThreadLocalRandom current()
    {
        return INSTANCE;
    }

    public int nextInt(int origin, int bound)
    {
        if (origin >= bound)
        {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + nextInt(bound - origin);
    }

    public long nextLong(long bound)
    {
        if (bound <= 0)
        {
            throw new IllegalArgumentException("bound must be positive");
        }
        return (nextLong() & Long.MAX_VALUE) % bound;
    }

    public long nextLong(long origin, long bound)
    {
        if (origin >= bound)
        {
            throw new IllegalArgumentException("bound must be greater than origin");
        }
        return origin + nextLong(bound - origin);
    }

    public double nextDouble(double bound)
    {
        return nextDouble() * bound;
    }

    public double nextDouble(double origin, double bound)
    {
        return origin + nextDouble() * (bound - origin);
    }
}
