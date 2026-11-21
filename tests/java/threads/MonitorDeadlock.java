/**
 * Cross-locking monitor deadlock.
 *
 * Two threads acquire the shared monitors in opposite order and then sleep, so
 * both are guaranteed to hold one monitor before requesting the other.  With
 * -Ddarks.diag.deadlock=1 the watchdog must report the wait-for cycle within a
 * second and exit with code 5.
 */
public final class MonitorDeadlock
{
    static final Object A = new Object();
    static final Object B = new Object();

    static final class Worker extends Thread
    {
        private final Object first;
        private final Object second;

        Worker(String name, Object first, Object second)
        {
            super(name);
            this.first = first;
            this.second = second;
        }

        public void run()
        {
            synchronized (first)
            {
                sleepQuietly(60);
                synchronized (second)
                {
                    System.out.println("monitor-deadlock: no deadlock detected");
                }
            }
        }
    }

    static void sleepQuietly(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception
    {
        Thread t1 = new Worker("deadlock-a", A, B);
        Thread t2 = new Worker("deadlock-b", B, A);
        t1.start();
        t2.start();
        t1.join();
        System.out.println("monitor-deadlock: no deadlock detected");
    }
}
