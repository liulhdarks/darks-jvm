public class M3SemBase
{
    static int base;
    static
    {
        base = 1;
        boom();
    }

    static void boom()
    {
        throw new RuntimeException("base-boom");
    }
}
