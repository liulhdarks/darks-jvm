public class M3SemLazy
{
    static int value = 5;
    static
    {
        M3SemLog.lazyRuns++;
        boom();
    }

    static void boom()
    {
        throw new UnsupportedOperationException("lazy-boom");
    }
}
