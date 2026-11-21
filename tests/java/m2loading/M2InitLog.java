public class M2InitLog
{
    private static int inits;

    public static void record(String name)
    {
        inits++;
        System.out.println("M2-CLINIT:" + name);
    }

    public static int inits()
    {
        return inits;
    }

    public static void reset()
    {
        inits = 0;
    }
}
