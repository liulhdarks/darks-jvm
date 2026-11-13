public class LongArrayInit
{
    static long[] staticLongs = new long[3];
    static double[] staticDoubles = new double[2];
    long[] instanceLongs = new long[2];

    static
    {
        staticLongs[1] = 1234567890123L;
        staticDoubles[0] = 2.5;
    }

    LongArrayInit()
    {
        instanceLongs[0] = 77L;
    }

    public static void main(String[] args)
    {
        LongArrayInit value = new LongArrayInit();
        int checks = 0;
        int failures = 0;
        checks++;
        if (staticLongs[1] != 1234567890123L) failures++;
        checks++;
        if (staticDoubles[0] != 2.5) failures++;
        checks++;
        if (value.instanceLongs[0] != 77L) failures++;
        System.out.println("long-array checks=" + checks + " failures=" + failures);
        if (failures != 0)
        {
            System.exit(1);
        }
    }
}
