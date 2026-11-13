// Timed Object.wait(): wait(long) and wait(long,int) return after the timeout,
// notify() wakes a waiter early, and wait/notify keep the monitor invariants.
public class TimedWait {
    static final Object LOCK = new Object();
    static volatile boolean notified;

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        synchronized (LOCK) {
            LOCK.wait(50);
        }
        long elapsed = System.currentTimeMillis() - t0;
        if (elapsed < 30 || elapsed > 3000) {
            throw new RuntimeException("wait(50) slept " + elapsed + " ms");
        }

        t0 = System.currentTimeMillis();
        synchronized (LOCK) {
            LOCK.wait(20, 123456);
        }
        elapsed = System.currentTimeMillis() - t0;
        if (elapsed < 10 || elapsed > 3000) {
            throw new RuntimeException("wait(20,123456) slept " + elapsed + " ms");
        }

        // wait(0, positive nanos) must be timed, not indefinite.
        t0 = System.currentTimeMillis();
        synchronized (LOCK) {
            LOCK.wait(0, 1);
        }
        elapsed = System.currentTimeMillis() - t0;
        if (elapsed > 3000) {
            throw new RuntimeException("wait(0,1) treated as indefinite");
        }

        Thread w = new Thread(new Runnable() {
            public void run() {
                synchronized (LOCK) {
                    while (!notified) {
                        try {
                            LOCK.wait(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException("interrupted while waiting");
                        }
                    }
                }
            }
        });
        w.start();
        Thread.yield();
        synchronized (LOCK) {
            notified = true;
            LOCK.notify();
        }
        w.join(10000);
        if (w.isAlive()) {
            throw new RuntimeException("notify did not wake the waiter");
        }
        System.out.println("timed-wait ok elapsed=" + elapsed + " notified=" + notified);
    }
}
