// Timed join returns while the target is still alive, then the target can be
// interrupted and joined normally.
public class JoinTimeoutTest {
    public static void main(String[] a) throws Exception {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(10000); } catch (InterruptedException e) { }
            }
        });
        t.start();
        long t0 = System.currentTimeMillis();
        t.join(100);
        long dt = System.currentTimeMillis() - t0;
        if (!t.isAlive()) {
            throw new RuntimeException("target finished before timeout");
        }
        if (dt < 50 || dt > 3000) {
            throw new RuntimeException("timed join waited " + dt + " ms");
        }
        t.interrupt();
        t.join();
        System.out.println("join-timeout ok elapsed=" + dt + " aliveAfterTimeout=true aliveNow=" + t.isAlive());
        if (t.isAlive()) {
            throw new RuntimeException("target still alive after final join");
        }
    }
}
