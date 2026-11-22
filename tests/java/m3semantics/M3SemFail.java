public class M3SemFail
{
    static int value;
    static
    {
        M3SemLog.runs++;
        value = 42;
        boom();
    }

    static void boom()
    {
        throw new IllegalStateException("boom");
    }

    public static void touch()
    {
    }
}
