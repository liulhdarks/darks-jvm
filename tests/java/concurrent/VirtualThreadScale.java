// 100k virtual threads, each sleeping and joining.  Verifies that the
// scheduler can hold a large number of suspended coroutines without one OS
// thread per Java thread.
public class VirtualThreadScale {
    public static void main(String[] args) throws Exception {
        int count = args.length > 0 ? Integer.parseInt(args[0]) : 100000;
        Runtime runtime = Runtime.getRuntime();
        long before = runtime.totalMemory() - runtime.freeMemory();
        long start = System.currentTimeMillis();

        Thread[] threads = new Thread[count];
        for (int i = 0; i < count; i++) {
            threads[i] = Thread.ofVirtual().start(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        throw new RuntimeException("unexpected interrupt");
                    }
                }
            });
        }
        for (int i = 0; i < count; i++) {
            threads[i].join();
        }

        long elapsed = System.currentTimeMillis() - start;
        long after = runtime.totalMemory() - runtime.freeMemory();
        boolean allVirtual = true;
        for (int i = 0; i < count; i++) {
            if (!threads[i].isVirtual()) {
                allVirtual = false;
            }
            if (threads[i].isAlive()) {
                throw new RuntimeException("thread " + i + " still alive");
            }
        }
        if (!allVirtual) {
            throw new RuntimeException("isVirtual() returned false");
        }
        System.out.println("virtual-thread-scale ok n=" + count
                + " virtual=" + allVirtual + " elapsed_ms=" + elapsed
                + " heap_delta_kb=" + ((after - before) / 1024));
    }
}
