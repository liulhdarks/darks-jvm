// Synchronized block and synchronized method under high contention.
public class SyncContention {
    static int blockCounter;
    static int methodCounter;
    static final Object lock = new Object();

    static synchronized void bumpMethod() { methodCounter++; }

    public static void main(String[] a) throws Exception {
        int NT = 4;
        final int ITERS = 25000;
        Thread[] ts = new Thread[NT];
        for (int i = 0; i < NT; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    for (int k = 0; k < ITERS; k++) {
                        synchronized (lock) { blockCounter++; }
                        bumpMethod();
                    }
                }
            });
            ts[i].start();
        }
        for (int i = 0; i < NT; i++) ts[i].join();
        int expected = NT * ITERS;
        if (blockCounter != expected || methodCounter != expected) {
            throw new RuntimeException("block=" + blockCounter + " method=" + methodCounter
                + " expected=" + expected);
        }
        System.out.println("sync ok block=" + blockCounter + " method=" + methodCounter
            + " expected=" + expected);
    }
}
