package java.lang.management;

public class MemoryUsage
{
    private final long init;
    private final long used;
    private final long committed;
    private final long max;

    public MemoryUsage(long init, long used, long committed, long max)
    {
        this.init = init;
        this.used = used;
        this.committed = committed;
        this.max = max;
    }

    public long getInit()
    {
        return init;
    }

    public long getUsed()
    {
        return used;
    }

    public long getCommitted()
    {
        return committed;
    }

    public long getMax()
    {
        return max;
    }

    public String toString()
    {
        return "init = " + init + "(" + (init / 1024L / 1024L) + "K)"
            + " used = " + used + "(" + (used / 1024L / 1024L) + "K)"
            + " committed = " + committed + "(" + (committed / 1024L / 1024L) + "K)"
            + " max = " + max + "(" + (max / 1024L / 1024L) + "K)";
    }
}
