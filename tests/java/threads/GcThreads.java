// Allocation from several coroutines while the collector stops the world.
public class GcThreads {
    static volatile long sink;

    public static void main(String[] a) throws Exception {
        int N = a.length > 0 ? Integer.parseInt(a[0]) : 4;
        final int ITERS = 1000;
        Thread[] ts = new Thread[N];
        for (int i = 0; i < N; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    long local = 0;
                    for (int k = 0; k < ITERS; k++) {
                        Object[] x = new Object[128];
                        x[0] = new byte[256];
                        x[1] = new int[64];
                        local += x.length;
                        if ((k & 31) == 0) Thread.yield();
                    }
                    sink += local;
                }
            });
            ts[i].start();
        }
        for (int i = 0; i < N; i++) ts[i].join();
        long expected = (long) N * ITERS * 128;
        if (sink != expected) {
            throw new RuntimeException("sink=" + sink + " expected=" + expected);
        }
        System.out.println("gc-threads ok sink=" + sink + " expected=" + expected);
    }
}
