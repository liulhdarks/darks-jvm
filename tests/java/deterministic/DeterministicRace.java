// A deliberately racy workload: several workers append to a shared buffer and
// yield between steps.  Under a fixed deterministic seed the interleaving is
// reproducible; different seeds explore different interleavings.
public class DeterministicRace
{
    private static final StringBuilder order = new StringBuilder();

    public static void main(String[] args) throws Exception
    {
        final int workers = 3;
        final int rounds = 6;
        Thread[] threads = new Thread[workers];
        for (int i = 0; i < workers; i++)
        {
            final int id = i;
            threads[i] = new Thread(new Runnable()
            {
                public void run()
                {
                    for (int r = 0; r < rounds; r++)
                    {
                        synchronized (order)
                        {
                            order.append((char)('A' + id));
                        }
                        Thread.yield();
                    }
                }
            });
        }
        for (int i = 0; i < workers; i++)
        {
            threads[i].start();
        }
        for (int i = 0; i < workers; i++)
        {
            threads[i].join();
        }
        System.out.println("deterministic-race interleaving=" + order);
        System.out.println("deterministic-race events=" + darks.Deterministic.eventCount()
                           + " hash=" + darks.Deterministic.eventHash());
    }
}
