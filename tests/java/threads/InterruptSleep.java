// Interrupting a sleeping coroutine raises InterruptedException.
public class InterruptSleep {
    static volatile boolean caught;

    public static void main(String[] a) throws Exception {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000);
                    caught = false;
                } catch (InterruptedException e) {
                    caught = true;
                }
            }
        });
        t.start();
        Thread.sleep(100);
        long t0 = System.currentTimeMillis();
        t.interrupt();
        t.join();
        long dt = System.currentTimeMillis() - t0;
        if (!caught) {
            throw new RuntimeException("InterruptedException was not thrown");
        }
        if (dt > 3000) {
            throw new RuntimeException("interrupt did not wake the sleeper: " + dt + " ms");
        }
        System.out.println("interrupt ok caught=" + caught + " wake_ms=" + dt);
    }
}
