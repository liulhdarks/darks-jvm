public class M3SemError
{
    static int value = 1;
    static
    {
        boom();
    }

    static void boom()
    {
        throw new AssertionError((Object) "bad-init");
    }
}
