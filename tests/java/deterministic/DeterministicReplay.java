// Exercises every recorded decision kind: Random draws, HashMap iteration
// order, a virtual-clock sleep, monitor hand-offs and thread scheduling.  The
// CTest driver records one run and then replays it, requiring an identical
// trace and event hash.
public class DeterministicReplay
{
    private static final StringBuilder log = new StringBuilder();

    public static void main(String[] args) throws Exception
    {
        java.util.Random rnd = new java.util.Random(4242L);
        java.util.Random seeded = new java.util.Random();
        java.util.HashMap map = new java.util.HashMap();
        for (int i = 0; i < 8; i++)
        {
            map.put("k" + i, Integer.valueOf(i * 7));
        }
        StringBuilder keys = new StringBuilder();
        java.util.Iterator it = map.keySet().iterator();
        while (it.hasNext())
        {
            keys.append(it.next());
        }

        Thread sleeper = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(250L);
                }
                catch (InterruptedException e)
                {
                }
                synchronized (log)
                {
                    log.append('S');
                }
            }
        });
        Thread worker = new Thread(new Runnable()
        {
            public void run()
            {
                for (int i = 0; i < 3; i++)
                {
                    synchronized (log)
                    {
                        log.append('W');
                    }
                    Thread.yield();
                }
            }
        });
        sleeper.start();
        worker.start();
        sleeper.join();
        worker.join();

        System.out.println("deterministic-replay rnd=" + rnd.nextInt(1000) + ","
                           + rnd.nextInt(1000) + " seeded=" + seeded.nextInt(1000));
        System.out.println("deterministic-replay keys=" + keys + " log=" + log
                           + " time=" + System.currentTimeMillis());
    }
}
