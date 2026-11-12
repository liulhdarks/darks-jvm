public class Clock
{
    public static void main(String[] args)
    {
        long t = System.currentTimeMillis();
        if (t > 0)
        {
            System.out.println("clock: ok, native System.currentTimeMillis() returned a positive value");
        }
        else
        {
            System.out.println("clock: native System.currentTimeMillis() failed");
        }
    }
}
