// Bounded producer/consumer using synchronized, wait and notifyAll.
public class ProducerConsumer {
    static final int CAP = 8;
    static final int ITEMS = 2000;
    static final int[] buf = new int[CAP];
    static final Object lock = new Object();
    static int count, head, tail;
    static long sumProduced, sumConsumed;

    public static void main(String[] a) throws Exception {
        int NP = 2, NC = 2;
        Thread[] ps = new Thread[NP];
        Thread[] cs = new Thread[NC];
        for (int i = 0; i < NP; i++) {
            final int base = i * ITEMS;
            ps[i] = new Thread(new Runnable() {
                public void run() {
                    for (int k = 0; k < ITEMS; k++) {
                        synchronized (lock) {
                            while (count == CAP) {
                                try { lock.wait(); } catch (InterruptedException e) { }
                            }
                            int v = base + k;
                            buf[tail] = v; tail = (tail + 1) % CAP; count++;
                            sumProduced += v;
                            lock.notifyAll();
                        }
                    }
                }
            });
            ps[i].start();
        }
        for (int i = 0; i < NC; i++) {
            cs[i] = new Thread(new Runnable() {
                public void run() {
                    for (int k = 0; k < ITEMS; k++) {
                        synchronized (lock) {
                            while (count == 0) {
                                try { lock.wait(); } catch (InterruptedException e) { }
                            }
                            int v = buf[head]; head = (head + 1) % CAP; count--;
                            sumConsumed += v;
                            lock.notifyAll();
                        }
                    }
                }
            });
            cs[i].start();
        }
        for (int i = 0; i < NP; i++) ps[i].join();
        for (int i = 0; i < NC; i++) cs[i].join();
        if (count != 0 || sumProduced != sumConsumed) {
            throw new RuntimeException("produced=" + sumProduced + " consumed=" + sumConsumed
                + " left=" + count);
        }
        System.out.println("producer-consumer ok produced=" + sumProduced
            + " consumed=" + sumConsumed + " left=" + count);
    }
}
