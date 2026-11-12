// A blocked coroutine must not consume an OS thread: 1000 sleepers taking
// about 100 ms in total, not 100 s.
public class SleepConcurrency {
    public static void main(String[] a) throws Exception {
        int N = 1000;
        Thread[] ts = new Thread[N];
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            ts[i] = new Thread(new Runnable() {
                public void run() {
                    try { Thread.sleep(100); } catch (InterruptedException e) { }
                }
            });
            ts[i].start();
        }
        for (int i = 0; i < N; i++) ts[i].join();
        long dt = System.currentTimeMillis() - t0;
        if (dt > 3000) {
            throw new RuntimeException("sleepers were serialized: " + dt + " ms");
        }
        System.out.println("sleep-concurrency ok n=" + N + " elapsed_ms=" + dt);
    }
}
