/**
 * Thread.join() cycle: each worker joins the other.
 *
 * The join edges must be enough for the watchdog to report the cycle; the main
 * thread joins one of the workers without a timeout, so the whole process is
 * parked.  -Ddarks.diag.deadlock=1 must report it and exit with code 5.
 */
public final class JoinCycle
{
    static volatile boolean go = false;
    static Thread t1;
    static Thread t2;

    static final class Joiner extends Thread
    {
        private final boolean first;

        Joiner(String name, boolean first)
        {
            super(name);
            this.first = first;
        }

        public void run()
        {
            Thread other = first ? t2 : t1;
            // Both workers must be registered before either can block in join;
            // otherwise the join would observe "not started" and return.
            while (!go)
            {
                Thread.yield();
            }
            try
            {
                other.join();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
            System.out.println("join-cycle: no deadlock detected");
        }
    }

    public static void main(String[] args) throws Exception
    {
        t1 = new Joiner("join-a", true);
        t2 = new Joiner("join-b", false);
        t1.start();
        t2.start();
        go = true;
        t1.join();
        System.out.println("join-cycle: no deadlock detected");
    }
}
