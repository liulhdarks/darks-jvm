// java.lang.Thread basics: start/join, naming, liveness, currentThread and
// ThreadLocal isolation.
public class ThreadBasics {
    static final ThreadLocal<String> TL = new ThreadLocal<String>() {
        protected String initialValue() { return "none"; }
    };

    public static void main(String[] a) throws Exception {
        if (!"main".equals(Thread.currentThread().getName())) {
            throw new RuntimeException("main thread name is " + Thread.currentThread().getName());
        }
        TL.set("main-value");
        final StringBuilder childSaw = new StringBuilder();
        Thread t = new Thread(new Runnable() {
            public void run() {
                childSaw.append(TL.get());   // must be the initial value
                TL.set("child-value");
                Thread.yield();
            }
        }, "worker-1");
        t.setName("worker-1");
        t.start();
        if (!t.isAlive()) {
            throw new RuntimeException("thread not alive after start");
        }
        t.join();
        if (t.isAlive()) {
            throw new RuntimeException("thread still alive after join");
        }
        if (!"none".equals(childSaw.toString())) {
            throw new RuntimeException("ThreadLocal leaked: " + childSaw);
        }
        if (!"main-value".equals(TL.get())) {
            throw new RuntimeException("main ThreadLocal changed");
        }
        if (!"worker-1".equals(t.getName())) {
            throw new RuntimeException("name mismatch: " + t.getName());
        }
        System.out.println("thread-basics ok name=" + t.getName() + " childTls=" + childSaw);
    }
}
