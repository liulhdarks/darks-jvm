// The virtual clock must make a one-hour sleep return immediately while
// System.nanoTime()/currentTimeMillis() report the full hour.
public class VirtualClock
{
    public static void main(String[] args) throws Exception
    {
        long nano0 = System.nanoTime();
        long millis0 = System.currentTimeMillis();
        Thread.sleep(3600000L);
        long nano1 = System.nanoTime();
        long millis1 = System.currentTimeMillis();
        long elapsedMs = (nano1 - nano0) / 1000000L;
        long wallDelta = millis1 - millis0;
        System.out.println("virtual-clock elapsed_ms=" + elapsedMs
                           + " wall_delta_ms=" + wallDelta);
        if (elapsedMs < 3600000L)
        {
            throw new RuntimeException("virtual clock did not advance: " + elapsedMs);
        }
        if (wallDelta < 3600000L)
        {
            throw new RuntimeException("currentTimeMillis did not advance: " + wallDelta);
        }
        System.out.println("virtual-clock ok");
    }
}
