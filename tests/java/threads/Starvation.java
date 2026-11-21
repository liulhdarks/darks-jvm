/**
 * Ready-queue starvation: with one worker, more CPU-bound threads than the
 * queue can dispatch promptly leaves runnable coroutines waiting far longer
 * than the (deliberately tiny) starvation threshold.
 *
 * Runs with -Ddarks.diag.starvation=1 -Ddarks.diag.starvation.ms=<small> and
 * must report starvation and exit with code 6.
 */
public final class Starvation
{
    static final int THREADS = 12;
    static final int ITERATIONS = 1500000;
    static volatile long sink;

    static final class Burner extends Thread
    {
        private final int seed;

        Burner(int id)
        {
            super("burner-" + id);
            this.seed = id;
        }

        public void run()
        {
            long acc = seed;
            for (int i = 0; i < ITERATIONS; i++)
            {
                acc = acc * 1103515245L + 12345L;
            }
            if (acc == 42)
            {
                sink = acc;
            }
        }
    }

    public static void main(String[] args) throws Exception
    {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++)
        {
            threads[i] = new Burner(i);
        }
        for (int i = 0; i < THREADS; i++)
        {
            threads[i].start();
        }
        for (int i = 0; i < THREADS; i++)
        {
            threads[i].join();
        }
        System.out.println("starvation: no starvation detected");
    }
}
