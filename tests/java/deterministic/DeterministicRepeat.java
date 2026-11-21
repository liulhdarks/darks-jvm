// Runs a fixed concurrent workload whose interleaving must be identical on
// every run under the same -Ddarks.deterministic seed.  The CTest driver
// launches this program 100 times and compares the trace and event hash.
public class DeterministicRepeat
{
    private static final StringBuilder trace = new StringBuilder();

    public static void main(String[] args) throws Exception
    {
        final int workers = 4;
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
                        synchronized (trace)
                        {
                            trace.append((char)('A' + id));
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
        String text = trace.toString();
        System.out.println("deterministic-repeat enabled=" + darks.Deterministic.enabled()
                           + " trace=" + text + " digest=" + text.hashCode());
        System.out.println("deterministic-repeat events=" + darks.Deterministic.eventCount()
                           + " hash=" + darks.Deterministic.eventHash());
    }
}
