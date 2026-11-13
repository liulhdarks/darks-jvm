// Regression: the main coroutine must be resumed when it is the next monitor
// entry owner at an Object.wait() handoff.
//
// A consumer owns the monitor and calls wait() while main is already queued in
// monitorenter.  WaitCurrent releases the monitor and hands it directly to the
// queued owner.  It used to leave main in CO_MONITOR (not CO_READY), so
// PumpMain never resumed it and the process hung.  DARKS_JVM_MP=1 is
// deterministic for this schedule.

public class MainMonitorHandoff {
    static final Object LOCK = new Object();
    static final Object DONE = new Object();
    static volatile boolean inside;
    static volatile boolean mainTried;
    static boolean finished;
    static int signals;

    public static void main(String[] args) throws Exception {
        Thread waiter = new Thread(new Runnable() {
            public void run() {
                synchronized (LOCK) {
                    inside = true;
                    while (!mainTried) {
                        Thread.yield();
                    }
                    while (!finished) {
                        try {
                            LOCK.wait();
                        } catch (InterruptedException e) {
                            throw new RuntimeException("interrupted while waiting");
                        }
                    }
                }
                synchronized (DONE) {
                    signals++;
                    DONE.notifyAll();
                }
            }
        }, "handoff-waiter");
        waiter.start();

        while (!inside) {
            Thread.yield();
        }
        mainTried = true;
        // The waiter observes mainTried and immediately parks in wait(); main
        // is queued on the monitor entry list at that moment.
        synchronized (LOCK) {
            finished = true;
            LOCK.notifyAll();
        }

        waiter.join(10000);
        if (waiter.isAlive()) {
            throw new RuntimeException("main was not resumed after monitor handoff");
        }
        synchronized (DONE) {
            if (signals != 1) {
                throw new RuntimeException("waiter did not finish, signals=" + signals);
            }
        }
        System.out.println("main-monitor-handoff ok signals=" + signals);
    }
}
