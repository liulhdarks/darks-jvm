// 10k coroutines: every Thread is a user-space coroutine with its own stack.
public class CoroutineScale {
    static class Counter implements Runnable {
        int n;
        public void run() { for (int i = 0; i < 100; i++) n++; }
    }

    public static void main(String[] a) throws Exception {
        int N = a.length > 0 ? Integer.parseInt(a[0]) : 10000;
        long t0 = System.currentTimeMillis();
        Counter[] cs = new Counter[N];
        Thread[] ts = new Thread[N];
        for (int i = 0; i < N; i++) {
            cs[i] = new Counter();
            ts[i] = new Thread(cs[i]);
            ts[i].start();
        }
        long sum = 0;
        for (int i = 0; i < N; i++) ts[i].join();
        for (int i = 0; i < N; i++) sum += cs[i].n;
        long expected = (long) N * 100;
        if (sum != expected) {
            throw new RuntimeException("sum=" + sum + " expected=" + expected);
        }
        long dt = System.currentTimeMillis() - t0;
        System.out.println("coroutines ok n=" + N + " sum=" + sum + " elapsed_ms=" + dt);
    }
}
