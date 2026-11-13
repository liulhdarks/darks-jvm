// Concurrency correctness check for the coroutine runtime.
//
//  * locked:   N threads increment one counter under synchronized; the result
//              must be exactly N*ITERS.
//  * unlocked: the same loop without a lock is allowed to lose updates.  Its
//              result proves the workers really overlap (it is reported and
//              must never exceed the exact value).  The test does not require
//              a loss on every platform: DARKS_JVM_MP=1 may serialise it.

public class CounterCorrectness {
    static final int THREADS = 4;
    static final int ITERS = 25000;
    static final int EXPECTED = THREADS * ITERS;
    static final long JOIN_TIMEOUT_MS = 30000;

    static final Object LOCK = new Object();
    static int lockedCounter;
    static int unlockedCounter;

    static class LockedWorker implements Runnable {
        public void run() {
            for (int i = 0; i < ITERS; i++) {
                synchronized (LOCK) {
                    lockedCounter++;
                }
            }
        }
    }

    static class UnlockedWorker implements Runnable {
        public void run() {
            for (int i = 0; i < ITERS; i++) {
                int value = unlockedCounter;
                if ((i & 7) == 0) {
                    Thread.yield();
                }
                unlockedCounter = value + 1;
            }
        }
    }

    static void runBatch(Runnable worker, String kind) throws Exception {
        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(worker, kind + "-" + i);
            threads[i].start();
        }
        for (int i = 0; i < THREADS; i++) {
            threads[i].join(JOIN_TIMEOUT_MS);
            if (threads[i].isAlive()) {
                throw new RuntimeException(kind + " " + i + " did not finish within "
                    + JOIN_TIMEOUT_MS + " ms");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("counter correctness: threads=" + THREADS + " iterations=" + ITERS);

        runBatch(new LockedWorker(), "locked");
        runBatch(new UnlockedWorker(), "unlocked");

        System.out.println("locked=" + lockedCounter + " expected=" + EXPECTED);
        System.out.println("unlocked=" + unlockedCounter + " expected=" + EXPECTED
            + " lost=" + (EXPECTED - unlockedCounter));

        if (lockedCounter != EXPECTED) {
            throw new AssertionError("synchronized counter lost updates: " + lockedCounter
                + " expected " + EXPECTED);
        }
        if (unlockedCounter < 0 || unlockedCounter > EXPECTED) {
            throw new AssertionError("unlocked counter out of range: " + unlockedCounter);
        }
        System.out.println("counter-check ok locked=" + lockedCounter
            + " expected=" + EXPECTED);
    }
}
