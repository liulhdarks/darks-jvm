/**
 * Timed waits must never be mistaken for a deadlock: a coroutine blocked in
 * Object.wait(timeout) still owns a timer that will wake it, so the wait-for
 * graph is not closed even though the joining main thread is blocked forever.
 *
 * Runs with -Ddarks.diag.deadlock=1 and must finish normally (exit code 0).
 */
public final class TimedWaitNoFalsePositive
{
    static final Object LOCK = new Object();

    public static void main(String[] args) throws Exception
    {
        Thread waiter = new Thread(new Runnable()
        {
            public void run()
            {
                synchronized (LOCK)
                {
                    try
                    {
                        LOCK.wait(400);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }, "timed-waiter");
        waiter.start();
        waiter.join();
        System.out.println("timed-wait-no-false-positive ok");
    }
}
