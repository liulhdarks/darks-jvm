// Interrupting a sleeping virtual thread must raise InterruptedException and
// leave the thread terminated.
public class VirtualThreadInterruptTest {
    static volatile boolean interrupted;

    public static void main(String[] args) throws Exception {
        Thread thread = Thread.ofVirtual().name("sleeper").start(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException expected) {
                    interrupted = true;
                }
            }
        });
        if (!thread.isVirtual()) {
            throw new RuntimeException("isVirtual() returned false");
        }
        Thread.sleep(50);
        thread.interrupt();
        thread.join(5000);
        if (thread.isAlive()) {
            throw new RuntimeException("virtual thread did not terminate");
        }
        if (!interrupted) {
            throw new RuntimeException("InterruptedException was not delivered");
        }
        System.out.println("virtual-thread-interrupt ok virtual="
                + thread.isVirtual() + " interrupted=" + interrupted);
    }
}
