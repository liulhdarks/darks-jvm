// A virtual thread that calls a native method while holding a monitor is
// "pinned": the carrier cannot be released.  The VM warns once per thread.
// The same program also checks that Thread.ofPlatform() reports isVirtual()
// false and still runs.
public class PinningTest {
    public static void main(String[] args) throws Exception {
        final Object lock = new Object();
        Thread pinned = Thread.ofVirtual().name("pinned").start(new Runnable() {
            public void run() {
                synchronized (lock) {
                    int hash = System.identityHashCode(lock);
                    if (hash == 0) {
                        throw new RuntimeException("unexpected identity hash");
                    }
                }
            }
        });
        pinned.join();

        Thread platform = Thread.ofPlatform().name("platform").start(new Runnable() {
            public void run() {
            }
        });
        platform.join();

        if (!pinned.isVirtual()) {
            throw new RuntimeException("virtual thread reported as platform");
        }
        if (platform.isVirtual()) {
            throw new RuntimeException("platform thread reported as virtual");
        }
        System.out.println("pinning ok virtual=" + pinned.isVirtual()
                + " platform=" + platform.isVirtual());
    }
}
